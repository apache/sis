/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.aggregate;

import java.util.Locale;
import java.util.List;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import java.time.Instant;
import java.time.Duration;
import org.opengis.util.GenericName;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.image.Colorizer;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.base.MemoryGridResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.privy.Numerics;


/**
 * Creates a grid coverage resource from an aggregation of an arbitrary number of other resources.
 * This class accepts heterogeneous resources (a <i>data lake</i>), organizes them in a tree
 * of resources as described in the next section, then performs different kinds of aggregation:
 *
 * <ul class="verbose">
 *   <li><b>Creation of a data cube from a collection of slices:</b>
 *     If a collection of {@link GridCoverageResource} instances represent the same phenomenon
 *     (for example Sea Surface Temperature) over the same geographic area but at different dates and times.
 *     {@code CoverageAggregator} can be used for building a single data cube with a time axis.</li>
 *   <li><b>Aggregation of bands:</b>
 *     Resources having different sample dimensions can be combined in a single resource.</li>
 * </ul>
 *
 * <h2>Generated resource tree</h2>
 * All source coverages should share the same CRS and have the same ranges (sample dimensions).
 * If this is not the case, then the source coverages will be grouped in different aggregates
 * with an uniform CRS and set of ranges in each sub-aggregates.
 * More specifically, {@code CoverageAggregator} organizes resources as below,
 * except that parent nodes having only one child are omitted:
 *
 * <pre class="text">
 *     Root aggregate
 *     ├─ All coverages with same sample dimensions #1
 *     │  └─ ...
 *     └─ All coverages with same sample dimensions #2
 *        ├─ Coverages with equivalent reference systems #1
 *        │  └─ ...
 *        └─ Coverages with equivalent reference systems #2
 *           ├─ Slices with compatible "grid to CRS" #1
 *           ├─ Slices with compatible "grid to CRS" #2
 *           └─ ...</pre>
 *
 * Where:
 *
 * <ul>
 *   <li><dfn>Equivalent reference systems</dfn> means two {@link org.opengis.referencing.crs.CoordinateReferenceSystem} instances
 *       for which {@link org.apache.sis.util.Utilities#equalsIgnoreMetadata(Object, Object)} returns {@code true}.</li>
 *   <li><dfn>Compatible grid to CRS</dfn> means two {@linkplain org.apache.sis.coverage.grid.GridGeometry#getGridToCRS grid to CRS}
 *       transforms which are identical (with small tolerance for rounding errors) except for the translation terms,
 *       with the additional condition that the translations, when expressed in units of grid cell indices,
 *       can differ only by integer amounts of cells.</li>
 *   <li><dfn>Slices</dfn> means source coverages declared to this aggregator by calls to {@code add(…)} methods,
 *       after they have been incorporated in a data cube by this aggregator.
 *       Above tree does not contain the individual slices, but data cubes containing all slices that can fit.</li>
 * </ul>
 *
 * <h2>Multi-threading and concurrency</h2>
 * All {@code add(…)} methods can be invoked concurrently from arbitrary threads.
 * It is okay to load {@link GridCoverageResource} instances in parallel threads
 * and add those resources to {@code CoverageAggregator} without synchronization.
 * However, the final {@link #build()} method is <em>not</em> thread-safe;
 * that method shall be invoked from a single thread after all sources have been added
 * and no more addition are in progress.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.3
 */
@SuppressWarnings("exports")    // There is no public method using `GroupBySample`.
public final class CoverageAggregator extends Group<GroupBySample> {
    /**
     * The listeners of the parent resource (typically a {@link DataStore}), or {@code null} if none.
     */
    private final StoreListeners listeners;

    /**
     * The aggregates which were the sources of components added during a call to {@link #addComponents(Aggregate)}.
     * This is used for reusing existing aggregates instead of {@link GroupAggregate} when the content is the same.
     */
    private final Map<Set<Resource>, Queue<Aggregate>> aggregates;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is {@code null} by default.
     *
     * @see #getMergeStrategy()
     */
    private volatile MergeStrategy strategy;

    /**
     * A sequential number of determining the order in which slices are rendered.
     */
    private int sequence;

    /**
     * Creates an initially empty aggregator with no listeners.
     *
     * @since 1.4
     */
    public CoverageAggregator() {
        this(null);
    }

    /**
     * Creates an initially empty aggregator with the given listeners.
     *
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     *        This is usually the listeners of the {@link org.apache.sis.storage.DataStore}.
     */
    public CoverageAggregator(final StoreListeners listeners) {
        this(listeners, new GridCoverageProcessor());
    }

    /**
     * Creates an initially empty aggregator with the given listeners and coverage processor.
     *
     * @param listeners  listeners of the parent resource, or {@code null} if none.
     * @param processor  the processor to use if an operation needs to be applied on coverages,
     *                   or {@code null} for a default processor.
     */
    CoverageAggregator(final StoreListeners listeners, final GridCoverageProcessor processor) {
        super(processor);
        this.listeners = listeners;
        aggregates = new HashMap<>();
    }

    /**
     * Creates a name for this group for use in metadata (not a persistent identifier).
     * This is used only if this aggregator find resources having different sample dimensions.
     * In such case, this name will be the default name of the root resource.
     *
     * @param  locale  the locale for the name to return, or {@code null} for the default.
     * @return a name which can be used as aggregation name, or {@code null} if none.
     */
    @Override
    final String createName(final Locale locale) {
        return (listeners != null) ? listeners.getSourceName() : null;
    }

    /**
     * Adds the given coverage. This method can be invoked from any thread.
     *
     * @param  coverage  coverage to add.
     *
     * @since 1.4
     */
    public void add(final GridCoverage coverage) {
        try {
            add(new MemoryGridResource(listeners, coverage, processor));
        } catch (DataStoreException e) {
            /*
             * `DataStoreException` are never thrown by `MemoryGridResource`.
             * The only case where we could get that exception with default
             * `add(GridCoverageResource)` is with non-invertible transform.
             */
            final Throwable cause = e.getCause();
            if (cause instanceof NoninvertibleTransformException) {
                throw new IllegalGridGeometryException(cause);
            } else {
                throw new BackingStoreException(e);
            }
        }
    }

    /**
     * Adds the given coverage augmented with the specified grid dimensions.
     * The {@code dimToAdd} argument contains typically vertical or temporal axes to add to a two-dimensional coverage.
     * All additional dimensions in {@code dimToAdd} must have a grid extent size of one cell.
     *
     * @param  coverage  coverage to add.
     * @param  dimToAdd  the dimensions to append. The grid extent size must be 1 cell in all dimensions.
     * @throws IllegalGridGeometryException if a dimension has more than one grid cell, or concatenation
     *         would result in duplicated {@linkplain GridExtent#getAxisType(int) grid axis types},
     *         or the compound CRS cannot be created.
     *
     * @see GridCoverageProcessor#appendDimensions(GridCoverage, GridGeometry)
     *
     * @since 1.5
     */
    public void add(GridCoverage coverage, GridGeometry dimToAdd) {
        add(processor.appendDimensions(coverage, dimToAdd));
    }

    /**
     * Adds the given coverage augmented with a single grid dimension.
     * The additional dimension is typically a vertical axis to add to a two-dimensional coverage.
     *
     * @param  coverage  coverage to add.
     * @param  lower     lower coordinate value of the slice, in units of the CRS.
     * @param  span      size of the slice, in units of the CRS.
     * @param  crs       one-dimensional coordinate reference system of the slice, or {@code null} if unknown.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     *
     * @see GridCoverageProcessor#appendDimension(GridCoverage, double, double, SingleCRS)
     *
     * @since 1.5
     */
    public void add(GridCoverage coverage, double lower, double span, SingleCRS crs) {
        add(processor.appendDimension(coverage, lower, span, crs));
    }

    /**
     * Adds the given coverage augmented with a single temporal grid dimension.
     * This method is provided for convenience, but should be used carefully.
     * Slice coordinates computed from calendars tend to produce slices at irregular intervals
     * or with heterogeneous spans, which result in coverages that cannot be aggregated by this
     * {@code CoverageAggregator} class.
     *
     * @param  source  the source on which to append a temporal dimension.
     * @param  lower   start time of the slice.
     * @param  span    duration of the slice.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     *
     * @see GridCoverageProcessor#appendDimension(GridCoverage, Instant, Duration)
     *
     * @since 1.5
     */
    public void add(GridCoverage coverage, Instant lower, Duration span) {
        add(processor.appendDimension(coverage, lower, span));
    }

    /**
     * Adds the given resource. This method can be invoked from any thread.
     * This method does <em>not</em> recursively decomposes an {@link Aggregate} into its component.
     * If such decomposition is desired, see {@link #addComponents(Aggregate)} instead.
     *
     * @param  resource  resource to add.
     * @throws DataStoreException if the resource cannot be used.
     */
    public void add(final GridCoverageResource resource) throws DataStoreException {
        final List<SampleDimension> ranges = resource.getSampleDimensions();
        GroupBySample bySample;
        final int order;
search: synchronized (members) {
            order = sequence++;
            for (int i = members.size(); --i >= 0;) {   // Most recently added group is more likely to be the desired one.
                bySample = members.get(i);
                if (bySample.accepts(ranges)) {
                    break search;
                }
            }
            bySample = new GroupBySample(this, ranges);
            members.add(bySample);
        }
        final GridSlice slice;
        try {
            slice = new GridSlice(order, resource, bySample, strategy);
        } catch (NoninvertibleTransformException e) {
            throw new DataStoreContentException(e);
        }
        assert bySample.contains(slice);
    }

    /**
     * Adds the given resource augmented with the specified grid dimensions.
     * The {@code dimToAdd} argument contains typically vertical or temporal axes to add to a two-dimensional resource.
     * All additional dimensions in {@code dimToAdd} must have a grid extent size of one cell.
     *
     * @param  resource  resource to add.
     * @param  dimToAdd  the dimensions to append. The grid extent size must be 1 cell in all dimensions.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     * @throws DataStoreException if the resource cannot be used.
     *
     * @since 1.5
     */
    public void add(GridCoverageResource resource, GridGeometry dimToAdd) throws DataStoreException {
        add(DimensionAppender.create(processor, resource, dimToAdd));
    }

    /**
     * Adds the given resource augmented with a single grid dimension.
     * The additional dimension is typically a vertical axis to add to a two-dimensional resource.
     *
     * @param  resource  resource to add.
     * @param  lower     lower coordinate value of the slice, in units of the CRS.
     * @param  span      size of the slice, in units of the CRS.
     * @param  crs       one-dimensional coordinate reference system of the slice, or {@code null} if unknown.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     * @throws DataStoreException if the resource cannot be used.
     *
     * @since 1.5
     */
    public void add(final GridCoverageResource resource, final double lower, final double span, final SingleCRS crs)
            throws DataStoreException
    {
        /*
         * This code currently duplicates `GridCoverageProcessor.appendDimension(..., double, double, CRS)`,
         * but a future version may use the state of this `CoverageAggregator`, for example making a better
         * effort to align the resources on the same "gridToCRS" transform.
         */
        final long index   = Numerics.roundAndClamp(lower / span);
        final var  indices = new long[] {index};
        final var  names   = new DimensionNameType[] {
            GridExtent.typeFromAxis(crs.getCoordinateSystem().getAxis(0)).orElse(null)
        };
        final var extent = new GridExtent(names, indices, indices, true);
        final MathTransform gridToCRS = MathTransforms.linear(span, Math.fma(index, -span, lower));
        add(resource, new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
    }

    /**
     * Adds the given resource augmented with a single temporal grid dimension.
     * This method is provided for convenience, but should be used carefully.
     * Slice coordinates computed from calendars tend to produce slices at irregular intervals
     * or with heterogeneous spans, which result in coverages that cannot be aggregated by this
     * {@code CoverageAggregator} class.
     *
     * @param  resource  resource to add.
     * @param  lower     start time of the slice.
     * @param  span      duration of the slice.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     * @throws DataStoreException if the resource cannot be used.
     *
     * @since 1.5
     */
    public void add(final GridCoverageResource resource, final Instant lower, final Duration span) throws DataStoreException {
        /*
         * This code currently duplicates `GridCoverageProcessor.appendDimension(..., double, double, CRS)`,
         * but a future version may use the state of this `CoverageAggregator`, for example making a better
         * effort to align the resources on the same "gridToCRS" transform.
         */
        final var crs = DefaultTemporalCRS.castOrCopy(CommonCRS.Temporal.TRUNCATED_JULIAN.crs());
        double scale  = crs.toValue(span);
        double offset = crs.toValue(lower);
        long   index  = Numerics.roundAndClamp(offset / scale);             // See comment in above method.
        offset = crs.toValue(lower.minus(span.multipliedBy(index)));
        final var extent = new GridExtent(DimensionNameType.TIME, index, index, true);
        final MathTransform gridToCRS = MathTransforms.linear(scale, offset);
        add(resource, new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
    }

    /**
     * Adds all components of the given aggregate. This method can be invoked from any thread.
     * It delegates to {@link #add(GridCoverageResource)} for each component in the aggregate
     * which is an instance of {@link GridCoverageResource}.
     * Components that are themselves instance of {@link Aggregate} are decomposed recursively.
     *
     * @param  resource  resource to add.
     * @throws DataStoreException if a component of the resource cannot be used.
     *
     * @todo Instead of ignoring non-coverage instances, we should put them in a separated aggregate.
     */
    public void addComponents(final Aggregate resource) throws DataStoreException {
        boolean hasDuplicated = false;
        final Set<Resource> components = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Resource component : resource.components()) {
            if (components.add(component)) {
                if (component instanceof GridCoverageResource) {
                    add((GridCoverageResource) component);
                } else if (component instanceof Aggregate) {
                    addComponents((Aggregate) component);
                }
            } else {
                hasDuplicated = true;       // Should never happen, but we are paranoiac.
            }
        }
        /*
         * Remember the aggregate that we just added. If after the user finished to add all components,
         * we discover that we still have the exact same set of components than the given aggregate,
         * then we will use `resource` instead of creating a `GroupAggregate` with the same content.
         */
        if (!(hasDuplicated || components.isEmpty())) {
            synchronized (aggregates) {
                aggregates.computeIfAbsent(components, (k) -> new ArrayDeque<>(1)).add(resource);
            }
        }
    }

    /**
     * If a user supplied aggregate exists for all the given components, returns that aggregate.
     * The returned aggregate is removed from the pool; aggregates are not returned twice.
     * This method is thread-safe.
     *
     * @param  components  the components for which to get user supplied aggregate.
     * @return user supplied aggregate if it exists. The returned aggregate is removed from the pool.
     */
    final Optional<Aggregate> existingAggregate(final Resource[] components) {
        final Set<Resource> key = Collections.newSetFromMap(new IdentityHashMap<>());
        if (Collections.addAll(key, components)) {
            final Queue<Aggregate> r;
            synchronized (aggregates) {
                r = aggregates.get(key);
            }
            if (r != null) {
                return Optional.ofNullable(r.poll());
            }
        }
        return Optional.empty();
    }

    /**
     * Adds all grid resources provided by the given stream. This method can be invoked from any thread.
     * It delegates to {@link #add(GridCoverageResource)} for each element in the stream.
     * {@link Aggregate} instances are added as-is (not decomposed in their components).
     *
     * @param  resources  resources to add.
     * @throws DataStoreException if a resource cannot be used.
     *
     * @see #add(GridCoverageResource)
     */
    public void addAll(final Stream<? extends GridCoverageResource> resources) throws DataStoreException {
        try {
            resources.forEach((resource) -> {
                try {
                    add(resource);
                } catch (DataStoreException e) {
                    throw new BackingStoreException(e);
                }
            });
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }

    /**
     * Adds a resource whose range is the aggregation of the ranges of a sequence of resources.
     * This method combines homogeneous grid coverage resources by "stacking" their sample dimensions (bands).
     * The grid geometry is typically the same for all resources, but some variations described below are allowed.
     * The number of sample dimensions in the aggregated coverage is the sum of the number of sample dimensions in
     * each individual resource.
     *
     * <p>This convenience method delegates to {@link #addRangeAggregate(GridCoverageResource[], int[][])}.
     * See that method for more information on restrictions.</p>
     *
     * @param  sources  resources whose bands shall be aggregated, in order.
     * @throws DataStoreException if an error occurred while fetching the grid geometry or sample dimensions from a resource.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     *
     * @see #getColorizer()
     * @see GridCoverageProcessor#aggregateRanges(GridCoverage...)
     *
     * @since 1.4
     */
    public void addRangeAggregate(final GridCoverageResource... sources) throws DataStoreException {
        addRangeAggregate(sources, null);
    }

    /**
     * Adds a resource whose range is the aggregation of the specified bands of a sequence of resources.
     * This method performs the same work as {@link #addRangeAggregate(GridCoverageResource...)},
     * but with the possibility to specify the sample dimensions to retain in each source coverage.
     * The {@code bandsPerSource} argument specifies the sample dimensions to keep, in order.
     * That array can be {@code null} for selecting all sample dimensions in all source coverages,
     * or may contain {@code null} elements for selecting all sample dimensions of the corresponding coverage.
     * An empty array element (i.e. zero sample dimension to select) discards the corresponding source coverage.
     *
     * <h4>Restrictions</h4>
     * <ul>
     *   <li>All resources shall use the same coordinate reference system (CRS).</li>
     *   <li>All resources shall have the same {@linkplain GridCoverageResource#getGridGeometry() domain}, except
     *       for the grid extent and the translation terms which can vary by integer numbers of grid cells.</li>
     *   <li>All grid extents shall intersect and the intersection area shall be non-empty.</li>
     *   <li>If coverage data are stored in {@link java.awt.image.RenderedImage} instances,
     *       then all images shall use the same data type.</li>
     * </ul>
     *
     * Some of those restrictions may be relaxed in future Apache SIS versions.
     *
     * @param  sources         resources whose bands shall be aggregated, in order.
     * @param  bandsPerSource  sample dimensions for each source. May be {@code null} or may contain {@code null} elements.
     * @throws DataStoreException if an error occurred while fetching the grid geometry or sample dimensions from a resource.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     *
     * @see #getColorizer()
     * @see GridCoverageProcessor#aggregateRanges(GridCoverage[], int[][])
     *
     * @since 1.4
     */
    public void addRangeAggregate(final GridCoverageResource[] sources, final int[][] bandsPerSource) throws DataStoreException {
        if (sources.length != 0) {
            add(BandAggregateGridResource.create(listeners, sources, bandsPerSource, processor));
        }
    }

    /**
     * Returns the colorization algorithm to apply on computed images.
     * This algorithm is used for all resources added by {@link #addRangeAggregate(GridCoverageResource[], int[][])},
     *
     * @return colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @since 1.4
     */
    public Colorizer getColorizer() {
        return processor.getColorizer();
    }

    /**
     * Sets the colorization algorithm to apply on computed images.
     * This algorithm applies to all resources added by {@link #addRangeAggregate(GridCoverageResource[], int[][])},
     * including resources already added before this method is invoked.
     * If this method is never invoked, the default value is {@code null}.
     *
     * @param  colorizer  colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @since 1.4
     */
    public void setColorizer(final Colorizer colorizer) {
        processor.setColorizer(colorizer);
    }

    /**
     * Returns the algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is the most recent value set by a call to {@link #setMergeStrategy(MergeStrategy)},
     * or {@code null} if no strategy has been specified. In the latter case,
     * {@link SubspaceNotSpecifiedException} will be thrown in situations of ambiguity.
     * An ambiguity happens at {@link GridCoverage#render(GridExtent)} invocation time
     * if more than one source coverage (slice) is found for a specified grid index.
     *
     * @return algorithm to apply for merging source coverages at the same grid index, or {@code null} if none.
     */
    public MergeStrategy getMergeStrategy() {
        return strategy;
    }

    /**
     * Sets the algorithm to apply when more than one grid coverage can be found at the same grid index.
     * The new strategy applies to the <em>next</em> coverages to be added;
     * previously added coverage may or may not be impacted by this change (see below).
     * For avoiding hard-to-predict behavior, this method should be invoked before to add the first coverage.
     *
     * <h4>Effect on previously added coverages</h4>
     * The merge strategy of previously added coverages is not modified by this method call, except
     * for coverages (slices) that become part of the same aggregated {@link GridCoverageResource}
     * (data cube) than a coverage added after this method call.
     * In such case, the strategy set by this call to {@code setMergeStrategy(…)} prevails.
     * Said otherwise, the merge strategy of a data cube is the strategy which was active
     * at the time of the most recently added slice for that data cube.
     *
     * @param  strategy  new algorithm to apply for merging source coverages at the same grid index,
     *                   or {@code null} if none.
     */
    public void setMergeStrategy(final MergeStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Builds a resource which is the aggregation or concatenation of all components added to this aggregator.
     * The returned resource will be an instance of {@link GridCoverageResource} if possible,
     * or an instance of {@link Aggregate} if some heterogeneity in grid geometries or sample dimensions
     * prevents the concatenation of all coverages in a single resource.
     *
     * <p>An identifier can optionally be specified for the resource.
     * This identifier will be used if this method creates an aggregated or concatenated resource,
     * but it will be ignored if this method returns directly one of the resource specified to the
     * {@code add(…)} methods.</p>
     *
     * <h4>Multi-threading</h4>
     * If the {@code add(…)} and {@code addAll(…)} methods were invoked in background threads,
     * then all additions must be finished before this method is invoked.
     *
     * @param  identifier  identifier to assign to the aggregated resource, or {@code null} if none.
     * @return the aggregation or concatenation of all components added to this aggregator.
     *
     * @since 1.4
     */
    public synchronized Resource build(final GenericName identifier) {
        final GroupAggregate aggregate = prepareAggregate(listeners);
        aggregate.fillWithChildAggregates(this, GroupBySample::createComponents);
        final Resource result = aggregate.simplify(this);
        if (result instanceof AggregatedResource) {
            ((AggregatedResource) result).identifier = identifier;
        }
        return result;
    }
}
