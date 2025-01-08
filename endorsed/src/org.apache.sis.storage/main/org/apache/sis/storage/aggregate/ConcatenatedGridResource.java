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

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.base.MemoryGridResource;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.pending.jdk.JDK18;


/**
 * A grid coverage resource where a single dimension is the concatenation of many grid coverage resources.
 * All components must have the same "grid to CRS" transform.
 * Instances of {@code ConcatenatedGridResource} are created by {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ConcatenatedGridResource extends AbstractGridCoverageResource implements AggregatedResource {
    /**
     * The identifier for this aggregate, or {@code null} if none.
     * This is optionally supplied by users for their own purposes.
     * There is no default value.
     */
    private GenericName identifier;

    /**
     * Name of this resource to use in metadata.
     */
    private String name;

    /**
     * The grid geometry of this aggregated resource.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * The ranges of sample values of this aggregated resource.
     * Shall be an unmodifiable list.
     *
     * @see #getSampleDimensions()
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * Whether all {@link SampleDimension} represent "real world" values.
     */
    final boolean isConverted;

    /**
     * The slices of this resource, in the same order as {@link GridSliceLocator#sliceLows}.
     * Each slice is not necessarily 1 cell tick; larger slices are accepted.
     * This array shall be read-only.
     */
    private final GridCoverageResource[] slices;

    /**
     * Whether loading of grid coverages should be deferred to rendering time.
     * This is a bit set packed as {@code int} values.  A bit value of 1 means
     * that the coverages at the corresponding index should be loaded from the
     * {@linkplain #slices} at same index only when first needed.
     *
     * <p>Whether a bit is set or not depends on two factor:</p>
     * <ul>
     *   <li>Whether deferred loading has been requested by a call to {@link #setLoadingStrategy(RasterLoadingStrategy)}.</li>
     *   <li>Whether the slice at the corresponding index can handle deferred loading itself.
     *       In such case, we let the resource manages its own lazy loading.</li>
     * </ul>
     *
     * This array shall be read-only. If changes are desired, a new array shall be created (copy-on-write).
     *
     * @see #isDeferred(int)
     * @see RasterLoadingStrategy#AT_READ_TIME
     * @see RasterLoadingStrategy#AT_RENDER_TIME
     */
    private int[] deferredLoading;

    /**
     * The object for identifying indices in the {@link #slices} array.
     */
    final GridSliceLocator locator;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is {@code null} if no merge should be attempted.
     */
    final MergeStrategy strategy;

    /**
     * The envelope of this aggregate, or {@code null} if not yet computed.
     * May also be {@code null} if no slice declare an envelope, or if the union cannot be computed.
     *
     * @see #getEnvelope()
     */
    private ImmutableEnvelope envelope;

    /**
     * Whether {@link #envelope} has been initialized.
     * The envelope may still be null if the initialization failed.
     */
    private boolean envelopeIsEvaluated;

    /**
     * The resolutions, or {@code null} if not yet computed. Can be an empty array after computation.
     * Shall be read-only after computation.
     *
     * @see #getResolutions()
     */
    private double[][] resolutions;

    /**
     * Creates a new aggregated resource.
     *
     * @param  name       name of the grid coverage to create.
     * @param  listeners  listeners of the parent resource, or {@code null} if none.
     * @param  domain     value to be returned by {@link #getGridGeometry()}.
     * @param  ranges     value to be returned by {@link #getSampleDimensions()}.
     * @param  slices     the slices of this resource, in the same order as {@link GridSliceLocator#sliceLows}.
     */
    ConcatenatedGridResource(final String                 name,
                             final StoreListeners         listeners,
                             final GridGeometry           domain,
                             final List<SampleDimension>  ranges,
                             final GridCoverageResource[] slices,
                             final GridSliceLocator       locator,
                             final MergeStrategy          strategy)
    {
        super(listeners, false);
        this.name             = name;
        this.gridGeometry     = domain;
        this.sampleDimensions = ranges;
        this.slices           = slices;
        this.locator          = locator;
        this.strategy         = strategy;
        this.deferredLoading  = new int[JDK18.ceilDiv(slices.length, Integer.SIZE)];
        for (final SampleDimension sd : ranges) {
            if (sd.forConvertedValues(true) != sd) {
                isConverted = false;
                return;
            }
        }
        isConverted = true;
    }

    /**
     * Creates a new resource with the same data as given resource but a different merge strategy.
     * The two resources will share the same cache of loaded coverages.
     *
     * @param  source    the resource to copy.
     * @param  strategy  the new merge strategy.
     */
    private ConcatenatedGridResource(final ConcatenatedGridResource source, final MergeStrategy strategy) {
        super(source.listeners, true);
        name                = source.name;
        gridGeometry        = source.gridGeometry;
        sampleDimensions    = source.sampleDimensions;
        isConverted         = source.isConverted;
        slices              = source.slices;
        deferredLoading     = source.deferredLoading;
        locator             = source.locator;
        envelope            = source.envelope;
        envelopeIsEvaluated = source.envelopeIsEvaluated;
        resolutions         = source.resolutions;
        this.strategy = strategy;
    }

    /**
     * Returns a coverage with the same data as this coverage but a different merge strategy.
     * This is the implementation of {@link MergeStrategy#apply(Resource)} public method.
     */
    @Override
    public final synchronized Resource apply(final MergeStrategy s) {
        if (Objects.equals(strategy, s)) return this;
        return new ConcatenatedGridResource(this, s);
    }

    /**
     * Modifies the name of the resource.
     * This information is used for metadata.
     */
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the identifier of this resource.
     */
    @Override
    public void setIdentifier(final GenericName identifier) {
        this.identifier = identifier;
    }


    /**
     * Returns the resource persistent identifier as specified by the
     * user in {@link CoverageAggregator}. There is no default value.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Creates when first requested the metadata about this resource.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addTitle(name);
        builder.addDefaultMetadata(this, listeners);
        return builder.build();
    }

    /**
     * Returns the grid geometry of this aggregated resource.
     */
    @Override
    public final GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values of this aggregated resource.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final List<SampleDimension> getSampleDimensions() {
        return sampleDimensions;
    }

    /**
     * Returns the spatiotemporal envelope of this resource.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public synchronized Optional<Envelope> getEnvelope() throws DataStoreException {
        if (!envelopeIsEvaluated) {
            try {
                envelope = GroupAggregate.unionOfComponents(slices);
            } catch (TransformException e) {
                listeners.warning(e);
            }
            envelopeIsEvaluated = true;
        }
        return Optional.ofNullable(envelope);
    }

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this resource.
     * This method returns only the resolutions that are declared by all coverages.
     *
     * @return preferred resolutions for read operations in this resource, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from an underlying resource.
     */
    @Override
    public synchronized List<double[]> getResolutions() throws DataStoreException {
        if (resolutions == null) {
            resolutions = commonResolutions(slices);
        }
        return UnmodifiableArrayList.wrap(resolutions);
    }

    /**
     * Computes resolutions common to all sources.
     *
     * @param  sources  the sources to use for computing the common resolution.
     * @return the resolutions common to all given sources.
     */
    static double[][] commonResolutions(final GridCoverageResource[] sources) throws DataStoreException {
        int count = 0;
        double[][] resolutions = null;
        for (final GridCoverageResource slice : sources) {
            final double[][] sr = slice.getResolutions().toArray(double[][]::new);
            if (sr != null) {                       // Should never be null, but we are paranoiac.
                if (resolutions == null) {
                    resolutions = sr;
                    count = sr.length;
                } else {
                    int retained = 0;
                    for (int i=0; i<count; i++) {
                        final double[] r = resolutions[i];
                        for (int j=0; j<sr.length; j++) {
                            if (Arrays.equals(r, sr[j])) {
                                resolutions[retained++] = r;
                                sr[j] = null;
                                break;
                            }
                        }
                    }
                    count = retained;
                    if (count == 0) break;
                }
            }
        }
        return ArraysExt.resize(resolutions, count);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * This method returns the most conservative value of all slices.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     */
    @Override
    public synchronized RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        /*
         * If at least one bit of `deferredLoading` is set, then it means that
         * `setLoadingStrategy(…)` has been invoked with anything else than `AT_READ_TIME`.
         */
        RasterLoadingStrategy conservative = RasterLoadingStrategy.AT_GET_TILE_TIME;
        for (int i=0; i < slices.length; i++) {
            final GridCoverageResource slice = slices[i];
            RasterLoadingStrategy s = slice.getLoadingStrategy();
            if (s == null || s.ordinal() == 0) {                    // Should never be null, but we are paranoiac.
                s = isDeferred(i) ? RasterLoadingStrategy.AT_RENDER_TIME
                                  : RasterLoadingStrategy.AT_READ_TIME;
            }
            if (s.ordinal() < conservative.ordinal()) {
                conservative = s;
                if (s.ordinal() == 0) break;
            }
        }
        return conservative;
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     * Slices are free to replace the given strategy by another one.
     *
     * @param  strategy  the desired strategy for loading raster data.
     * @return {@code true} if the given strategy has been accepted by all slices.
     * @throws DataStoreException if an error occurred while setting data store configuration.
     */
    @Override
    public synchronized boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        final boolean deferred = (strategy.ordinal() != 0);
        final int[] newValues = new int[deferredLoading.length];
        boolean accepted = true;
        for (int i=0; i < slices.length; i++) {
            final GridCoverageResource slice = slices[i];
            if (!slice.setLoadingStrategy(strategy)) {
                if (deferred) {
                    newValues[i >>> Numerics.INT_SHIFT] |= (1 << i);
                }
                accepted = false;
            }
        }
        if (!Arrays.equals(deferredLoading, newValues)) {
            deferredLoading = newValues;
        }
        return accepted;
    }

    /**
     * Returns {@code true} if the loading of the coverage at the given index is deferred.
     */
    private boolean isDeferred(final int i) {
        return (deferredLoading[i >>> Numerics.INT_SHIFT] & (1 << i)) != 0;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        /*
         * Validate arguments. The slices to read will be from `lower` inclusive to `upper` exclusive.
         */
        if (ranges != null) {
            ranges = RangeArgument.validate(sampleDimensions.size(), ranges, listeners).getSelectedBands();
        }
        int lower = 0, upper = slices.length;
        if (domain != null) {
            final GridDerivation subgrid = gridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
            domain = subgrid.build();
            final GridExtent sliceExtent = subgrid.getIntersection();
            upper = locator.getUpper(sliceExtent, lower, upper);
            lower = locator.getLower(sliceExtent, lower, upper);
        }
        /*
         * At this point we got the indices of the slices to read. The range should not be empty (upper > lower).
         * Create arrays with only the requested range, without keeping reference to this concatenated resource,
         * for allowing garbage-collection of resources outside that range.
         */
        final int            count      = upper - lower;
        final Object[]       coverages  = new Object[count];
        final GridGeometry[] geometries = new GridGeometry[count];
        int[] deferred = null;
        for (int i=0; i<count; i++) {
            final int j = lower + i;
            final GridCoverageResource slice = slices[j];
            if (slice instanceof MemoryGridResource) {
                final GridCoverage coverage = ((MemoryGridResource) slice).coverage;
                coverages [i] = coverage;
                geometries[i] = coverage.getGridGeometry();
            } else if (!isDeferred(j) || count <= 1) {
                final GridCoverage coverage = slice.read(domain, ranges);
                coverages [i] = coverage;
                geometries[i] = coverage.getGridGeometry();
            } else {
                coverages [i] = slice;
                geometries[i] = slice.getGridGeometry();
                if (deferred == null) {
                    deferred = new int[JDK18.ceilDiv(count, Integer.SIZE)];
                }
                deferred[i >>> Numerics.INT_SHIFT] |= (1 << i);
            }
        }
        /*
         * Following cast should never fail because the `count <= 1` check in above
         * loop ensured that we loaded the coverage.
         * Otherwise (if more than one slice), create a concatenation of all slices.
         */
        if (count == 1) {
            return (GridCoverage) coverages[0];
        }
        if (Arrays.equals(deferred, deferredLoading)) {
            deferred = deferredLoading;                     // Slight memory saving for a common case.
        }
        final GridGeometry union = locator.union(gridGeometry, Arrays.asList(geometries), GridGeometry::getExtent);
        return new ConcatenatedGridCoverage(this, union, coverages, lower, deferred, domain, ranges);
    }
}
