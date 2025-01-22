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
import java.util.BitSet;
import java.util.Objects;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.base.MemoryGridResource;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.UnmodifiableArrayList;


/**
 * A grid coverage resource where a single dimension is the concatenation of many grid coverage resources.
 * All components must have the same "grid to CRS" transform, except for the translation term.
 * Instances of {@code ConcatenatedGridResource} are created by {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ConcatenatedGridResource extends AggregatedResource implements GridCoverageResource {
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
     * Whether loading of grid coverages should be deferred to rendering time.
     * A bit value of 1 means that the coverages at the corresponding index should
     * be loaded from the {@linkplain #slices} at same index only when first needed.
     *
     * <p>Whether a bit is set or not depends on three factors:</p>
     * <ul>
     *   <li>Whether deferred loading has been requested by a call to {@link #setLoadingStrategy(RasterLoadingStrategy)}.</li>
     *   <li>Whether the resource is a {@link MemoryGridResource} instance, in which case it is not useful to delay.</li>
     *   <li>Whether the slice at the corresponding index can handle deferred loading itself.
     *       In such case, we let the resource manages its own lazy loading.</li>
     * </ul>
     *
     * This {@code BitSet} shall be read-only. If changes are desired, a new array shall be created (copy-on-write).
     *
     * @see #isDeferred(int)
     * @see #getLoadingStrategy()
     * @see #setLoadingStrategy(RasterLoadingStrategy)
     * @see RasterLoadingStrategy#AT_READ_TIME
     * @see RasterLoadingStrategy#AT_RENDER_TIME
     */
    private BitSet deferredLoading;

    /**
     * All slices in this resource, together with methods for locating them for a given area of interest.
     */
    final GridSliceLocator locator;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is {@code null} if no merge should be attempted.
     */
    final MergeStrategy mergeStrategy;

    /**
     * The processor to use for creating in-memory grid coverages.
     * This is used for non-deferred read operations.
     */
    private final GridCoverageProcessor processor;

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
     * @param  ranges     value to be returned by {@link #getSampleDimensions()}.
     * @param  locator    all slices together with methods for locating them for a given area of interest.
     * @param  strategy   algorithm to apply when more than one grid coverage can be found at the same grid index.
     * @param  processor  the processor to use for creating in-memory grid coverages.
     */
    ConcatenatedGridResource(final String                 name,
                             final StoreListeners         listeners,
                             final List<SampleDimension>  ranges,
                             final GridSliceLocator       locator,
                             final MergeStrategy          strategy,
                             final GridCoverageProcessor  processor)
    {
        super(name, listeners, false);
        this.sampleDimensions = ranges;
        this.locator          = locator;
        this.mergeStrategy    = strategy;
        this.processor        = processor;
        this.deferredLoading  = new BitSet();
        for (final SampleDimension sd : ranges) {
            if (sd.forConvertedValues(true) != sd) {
                isConverted = false;
                return;
            }
        }
        isConverted = true;
    }

    /**
     * Creates a new resource with the same data as the given resource but with a different merge strategy.
     * The two resources will share the same cache of loaded coverages.
     *
     * @param  source    the resource to copy.
     * @param  strategy  the new merge strategy.
     */
    private ConcatenatedGridResource(final ConcatenatedGridResource source, final MergeStrategy strategy) {
        super(source);
        sampleDimensions = source.sampleDimensions;
        isConverted      = source.isConverted;
        deferredLoading  = source.deferredLoading;
        locator          = source.locator;
        processor        = source.processor;
        resolutions      = source.resolutions;
        mergeStrategy    = strategy;
    }

    /**
     * Returns a coverage with the same data as this coverage but a different merge strategy.
     * This is the implementation of {@link MergeStrategy#apply(Resource)} public method.
     */
    @Override
    final Resource apply(final MergeStrategy s) {
        synchronized (getSynchronizationLock()) {
            if (Objects.equals(mergeStrategy, s)) return this;
            return new ConcatenatedGridResource(this, s);
        }
    }

    /**
     * Returns the slices in this grid coverage resource.
     * Invoked by the parent class for computing the envelope.
     */
    @Override
    final List<Resource> components() {
        final GridSlice[] slices = locator.slices;
        final var resources = new GridCoverageResource[slices.length];
        for (int i=0; i<resources.length; i++) {
            resources[i] = slices[i].resource;
        }
        return UnmodifiableArrayList.wrap(resources);
    }

    /**
     * Creates when first requested the metadata about this resource.
     */
    @Override
    protected void createMetadata(final MetadataBuilder builder) throws DataStoreException {
        builder.addDefaultMetadata(this, listeners);
    }

    /**
     * Returns the grid geometry of this aggregated resource.
     */
    @Override
    public final GridGeometry getGridGeometry() {
        return locator.gridGeometry;
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
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this resource.
     * This method returns only the resolutions that are declared by all coverages.
     *
     * @return preferred resolutions for read operations in this resource, or an empty list if none.
     * @throws DataStoreException if an error occurred while reading definitions from an underlying resource.
     */
    @Override
    public List<double[]> getResolutions() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (resolutions == null) {
                final GridSlice[] slices = locator.slices;
                final var resources = new GridCoverageResource[slices.length];
                for (int i=0; i<resources.length; i++) {
                    resources[i] = slices[i].resource;
                }
                resolutions = commonResolutions(resources);
            }
            return UnmodifiableArrayList.wrap(resolutions);
        }
    }

    /**
     * Searches for a resolution which is common to all sources. It may be none.
     *
     * @param  sources  the sources to use for searching a common resolution.
     * @return the resolutions common to all given sources, or an empty array if none.
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
     * Whether the loading of raster data is performed immediately during {@code read(…)} method invocation.
     * By default, this is the most conservative loading strategy of all slices contained in this resource.
     * This value can be modified by a call to {@link #setLoadingStrategy(RasterLoadingStrategy)}.
     *
     * <h4>Implementation note</h4>
     * This value is determined every times that this method is invoked instead of being cached
     * in order to reflect any change that may occur in the resources from outside of this class.
     * It is also for avoiding {@link DataStoreException} to be thrown from the constructor.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     *
     * @see #setLoadingStrategy(RasterLoadingStrategy)
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        RasterLoadingStrategy conservative = RasterLoadingStrategy.AT_GET_TILE_TIME;
        synchronized (getSynchronizationLock()) {
            for (final GridSlice slice : locator.slices) {
                if (!ignore(slice.resource)) {
                    RasterLoadingStrategy s = slice.resource.getLoadingStrategy();
                    if (s.ordinal() < conservative.ordinal()) {
                        conservative = s;
                        if (s.ordinal() == 0) break;    // No value is more conservative, so continuing is useless.
                    }
                }
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
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        final boolean deferred = (strategy != RasterLoadingStrategy.AT_READ_TIME);
        final BitSet newValues = new BitSet();
        boolean accepted = true;
        synchronized (getSynchronizationLock()) {
            for (int i = locator.slices.length; --i >= 0;) {
                final GridCoverageResource slice = locator.slices[i].resource;
                if (!slice.setLoadingStrategy(strategy) && !ignore(slice)) {
                    if (deferred && slice.getLoadingStrategy() == RasterLoadingStrategy.AT_READ_TIME) {
                        newValues.set(i);
                    } else {
                        accepted = false;
                    }
                }
            }
            if (!deferredLoading.equals(newValues)) {
                deferredLoading = newValues;
            }
        }
        return accepted;
    }

    /**
     * Returns whether the given slice should be ignored in the determination of a loading strategy.
     * {@link MemoryGridResource} instances are ignored because they usually behave like "at get tile time",
     * even if their {@code setLoadingStrategy(…)} method rejects the value (because it cannot be guaranteed).
     * We also don't flag those instances as deferred for the same reason.
     */
    private static boolean ignore(final GridCoverageResource slice) {
        return slice instanceof MemoryGridResource;
    }

    /**
     * Returns {@code true} if the loading of the coverage at the given index is deferred.
     * This is true only for resources that do not support themselves deferred loading.
     * In such cases, {@link ConcatenatedGridCoverage} will use its own deferring mechanism.
     */
    private boolean isDeferred(final int sliceIndex) {
        return deferredLoading.get(sliceIndex);
    }

    /**
     * Returns the subsampling, or {@code null} if there is no factor different than 1.
     */
    private static long[] subsampling(final GridDerivation subgrid) {
        final long[] subsampling = subgrid.getSubsampling();
        for (long s : subsampling) {
            if (s != 1) {
                return subsampling;
            }
        }
        return null;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * <h4>Implementation note</h4>
     * A large part of the complexity of this method is in the handling of subsampling.
     * Where there is no subsampling, we can rely of all slices returning grid geometries
     * with the same resolution. But when a subsampling is specified, each slice is free to
     * choose whatever resolution best fit the data, which may result in heterogeneous resolution.
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
         * In addition of requesting a sub-region of this resource, the user may also be requesting a
         * different layer of a pyramid, which will appear as a subsampling.
         */
        if (ranges != null) {
            ranges = RangeArgument.validate(sampleDimensions.size(), ranges, listeners).getSelectedBands();
        }
        final GridGeometry gridGeometry = getGridGeometry();
        final GridExtent areaOfInterest;    // In units of `gridGeometry` cells.
        final long[] subsampling;           // Null if no subsampling is applied.
        final int[]  selection;             // Indexes of slices in `locator`.
        if (domain != null) {
            GridDerivation subgrid = gridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
            domain         = subgrid.build();
            subsampling    = subsampling(subgrid);
            areaOfInterest = subgrid.getIntersection();
            selection      = locator.find(areaOfInterest);
        } else {
            domain         = gridGeometry;
            subsampling    = null;
            areaOfInterest = null;
            selection      = ArraysExt.range(0, locator.slices.length);
        }
        /*
         * Shortcut for the common case where exactly one slice intersects the given domain.
         */
        switch (selection.length) {
            case 0: throw new DisjointExtentException(gridGeometry.getExtent(), domain.getExtent(), locator.searchDimension);
            case 1: {
                final int sliceIndex = selection[0];
                if (isDeferred(sliceIndex)) break;
                return locator.slices[sliceIndex].resource.read(domain, ranges);
            }
        }
        /*
         * At this point, we got a non-empty array of indexes of the slices to read. Create arrays with the selection,
         * without keeping reference to this concatenated resource, for allowing garbage-collection of other resources.
         */
        final boolean full = domain.equals(gridGeometry, ComparisonMode.IGNORE_METADATA);
        final var selected = new GridSlice[selection.length];
        GridCoverage[] coverages = null;
        long[] actualSubsampling = null;
        for (int i=0; i < selection.length; i++) {
            final int sliceIndex = selection[i];
            final GridSlice slice = locator.slices[sliceIndex];
            selected[i] = slice;
            /*
             * "Deferring" in this context means "user wants deferred loading, but the slice does not support that".
             * `ConcatenatedGridCoverage` can add its own deferring loading, but only if no subsampling is applied.
             * Because otherwise, we are not sure which resolution `GridCoverageResource.read(…)` will choose.
             */
            if (full && isDeferred(sliceIndex)) {
                continue;
            }
            if (coverages == null) {
                coverages = new GridCoverage[selection.length];
            }
            GridCoverage coverage = slice.resource.read(domain, ranges);
            coverages[i] = coverage;
            if (subsampling != null) {
                /*
                 * Keep the grid geometry of the coverage having the largest subsampling.
                 * It will be used as a base for building the grid geometry of the result.
                 * We use largest subsampling for avoiding false sense of fine resolution,
                 * and on the assumption that other coverages can be adjusted to that resolution.
                 */
                GridDerivation subgrid = gridGeometry.derive().subgrid(coverage.getGridGeometry());
                final long[] candidate = subsampling(subgrid);
                if (candidate != null) {
                    if (actualSubsampling == null) {
                        actualSubsampling = candidate;
                    } else {
                        for (int j=0; j < actualSubsampling.length; j++) {
                            actualSubsampling[j] = Math.max(actualSubsampling[j], candidate[j]);
                        }
                    }
                }
            }
        }
        /*
         * If a subsampling is applied, recompute the domain with the subsampling actually used.
         * It may be different than the subsampling requested.
         */
        if (!Arrays.equals(subsampling, actualSubsampling)) {
            GridDerivation subgrid = gridGeometry.derive().subgrid(areaOfInterest, actualSubsampling);
            domain = subgrid.build();
        }
        /*
         * For all coverages that have been read immediately, wrap them in a memory resource.
         * This look cannot be inlined inside above loop because we need to know the adjusted domain.
         * This loop has some tolerance regarding coverages with a resolution different than expected.
         * This is okay if `MemoryGridResource` applies the missing subsampling in its `read(…)` method.
         */
        if (coverages != null) try {
            for (int i=0; i < selected.length; i++) {
                final GridCoverage coverage = coverages[i];
                if (coverage != null) {
                    GridGeometry geometry = coverage.getGridGeometry();
                    GridExtent inGroup = domain.extentOf(geometry, GridSlice.CELL_ANCHOR, GridRoundingMode.NEAREST);
                    var resource = new MemoryGridResource(listeners, coverage, processor);
                    selected[i] = selected[i].resolve(resource, geometry.getExtent(), inGroup);
                }
            }
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        }
        GridSliceLocator subset = locator;
        if (!Arrays.equals(selected, subset.slices)) {
            subset = new GridSliceLocator(domain, selected, locator.searchDimension);
        }
        return new ConcatenatedGridCoverage(this, subset, ranges);
    }
}
