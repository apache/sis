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
package org.apache.sis.internal.storage.aggregate;

import java.util.List;
import java.util.Arrays;
import java.util.Optional;
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
import org.apache.sis.internal.storage.MemoryGridResource;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.RangeArgument;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArraysExt;


/**
 * A grid coverage resource where a single dimension is the concatenation of many grid coverage resources.
 * All components must have the same "grid to CRS" transform.
 * Instances of {@code ConcatenatedGridResource} are created by {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class ConcatenatedGridResource extends AbstractGridCoverageResource implements AggregatedResource {
    /**
     * Name of this resource.
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
     * The slices of this resource, in the same order than {@link #coordinatesOfSlices}.
     * Each slice is not necessarily 1 cell tick; larger slices are accepted.
     */
    private final GridCoverageResource[] slices;

    /**
     * Whether loading of grid coverages should be deferred to rendering time.
     * This is a bit set packed as {@code long} values. A bit value of 1 means
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
     * @see RasterLoadingStrategy#AT_READ_TIME
     * @see RasterLoadingStrategy#AT_RENDER_TIME
     */
    private final long[] deferredLoading;

    /**
     * The object for identifying indices in the {@link #slices} array.
     */
    final GridSliceLocator locator;

    /**
     * The envelope of this aggregate, or {@code null} if not yet computed.
     * May also be {@code null} if no slice declare an envelope, or if the union can not be computed.
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
     *
     * @see #getResolutions()
     */
    private double[][] resolutions;

    /**
     * An optional resource to declare as the source of this aggregate in lineage metadata.
     * This is reset to {@code null} when no longer needed.
     */
    private Resource sourceMetadata;

    /**
     * Creates a new aggregated resource.
     *
     * @param  name       name of the grid coverage to create.
     * @param  listeners  listeners of the parent resource, or {@code null} if none.
     * @param  domain     value to be returned by {@link #getGridGeometry()}.
     * @param  ranges     value to be returned by {@link #getSampleDimensions()}.
     * @param  slices     the slices of this resource, in the same order than {@code coordinatesOfSlices}.
     */
    ConcatenatedGridResource(final String                 name,
                             final StoreListeners         listeners,
                             final GridGeometry           domain,
                             final List<SampleDimension>  ranges,
                             final GridCoverageResource[] slices,
                             final GridSliceLocator       locator)
    {
        super(listeners, false);
        this.name             = name;
        this.gridGeometry     = domain;
        this.sampleDimensions = ranges;
        this.slices           = slices;
        this.locator          = locator;
        this.deferredLoading  = new long[Numerics.ceilDiv(slices.length, Long.SIZE)];
        for (final SampleDimension sd : ranges) {
            if (sd.forConvertedValues(true) != sd) {
                isConverted = false;
                return;
            }
        }
        isConverted = true;
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
     * Specifies the resource to declare in lineage metadata as the source of this resource.
     * This information is used for metadata.
     */
    @Override
    public void setSourceMetadata(final Resource source) {
        sourceMetadata = source;
    }

    /**
     * Creates when first requested the metadata about this resource.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addTitle(name);
        builder.addDefaultMetadata(this, listeners);
        if (sourceMetadata != null) {
            builder.addSources(sourceMetadata);
            sourceMetadata = null;
        }
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
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * This method returns only the resolution that are declared by all coverages.
     *
     * @return preferred resolutions for read operations in this data store, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public synchronized List<double[]> getResolutions() throws DataStoreException {
        double[][] common = resolutions;
        if (common == null) {
            int count = 0;
            for (final GridCoverageResource slice : slices) {
                final double[][] sr = CollectionsExt.toArray(slice.getResolutions(), double[].class);
                if (sr != null) {                       // Should never be null, but we are paranoiac.
                    if (common == null) {
                        common = sr;
                        count = sr.length;
                    } else {
                        int retained = 0;
                        for (int i=0; i<count; i++) {
                            final double[] r = common[i];
                            for (int j=0; j<sr.length; j++) {
                                if (Arrays.equals(r, sr[j])) {
                                    common[retained++] = r;
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
            resolutions = common = ArraysExt.resize(common, count);
        }
        return UnmodifiableArrayList.wrap(common);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * This method returns the most conservative value of all slices.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        /*
         * If at least one bit of `deferredLoading` is set, then it means that
         * `setLoadingStrategy(…)` has been invoked with anything else than `AT_READ_TIME`.
         */
        int  bitx = 0;
        long mask = 1;
        RasterLoadingStrategy conservative = RasterLoadingStrategy.AT_GET_TILE_TIME;
        for (final GridCoverageResource slice : slices) {
            RasterLoadingStrategy s = slice.getLoadingStrategy();
            if (s == null || s.ordinal() == 0) {                    // Should never be null, but we are paranoiac.
                s = ((deferredLoading[bitx] & mask) != 0)
                        ? RasterLoadingStrategy.AT_RENDER_TIME
                        : RasterLoadingStrategy.AT_READ_TIME;
            }
            if (s.ordinal() < conservative.ordinal()) {
                conservative = s;
                if (s.ordinal() == 0) break;
            }
            if ((mask <<= 1) == 0) {
                mask=1;
                bitx++;
            }
        }
        return conservative;
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     * Slices are free to replace the given strategy by another one.
     *
     * @param  strategy  the desired strategy for loading raster data.
     * @return {@code true} if the given strategy has been accepted by at least one slice.
     * @throws DataStoreException if an error occurred while setting data store configuration.
     */
    @Override
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        final boolean deferred = (strategy.ordinal() != 0);
        Arrays.fill(deferredLoading, 0);
        boolean accepted = true;
        int  bitx = 0;
        long mask = 1;
        for (final GridCoverageResource slice : slices) {
            if (!slice.setLoadingStrategy(strategy)) {
                if (deferred) deferredLoading[bitx] |= mask;
                accepted = false;
            }
            if ((mask <<= 1) == 0) {
                mask=1;
                bitx++;
            }
        }
        return accepted;
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
         * Validate arguments.
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
        final int              count      = upper - lower;
        final GridGeometry[]   geometries = new GridGeometry[count];
        final GridCoverage[]   coverages  = new GridCoverage[count];
        GridCoverageResource[] resources  = null;                           // Created when first needed.
        int  bitx = lower >>> Numerics.LONG_SHIFT;
        long mask = 1L << lower;                        // No need for (lower & 63) because high bits are ignored.
        if (count <= 1) mask = 0;                       // Trick for forcing coverage loading.
        for (int i=0; i<count; i++) {
            final GridCoverageResource slice = slices[lower + i];
            if (slice instanceof MemoryGridResource) {
                final GridCoverage coverage = ((MemoryGridResource) slice).coverage;
                coverages [i] = coverage;
                geometries[i] = coverage.getGridGeometry();
            } else if ((deferredLoading[bitx] & mask) == 0) {
                final GridCoverage coverage = slice.read(domain, ranges);
                coverages [i] = coverage;
                geometries[i] = coverage.getGridGeometry();
            } else {
                if (resources == null) {
                    resources  = new GridCoverageResource[count];
                }
                resources [i] = slice;
                geometries[i] = slice.getGridGeometry();
            }
            if ((mask <<= 1) == 0) {
                mask=1;
                bitx++;
            }
        }
        /*
         * If it was not necessary to keep references to any resource, clear references to information
         * which were needed only for loading coverages from resources. Then create create the coverage.
         */
        if (count == 1) {
            return coverages[0];
        }
        if (resources == null) {
            domain = null;
            ranges = null;
        }
        final GridGeometry union = locator.union(gridGeometry, Arrays.asList(geometries), GridGeometry::getExtent);
        return new ConcatenatedGridCoverage(this, union, domain, coverages, resources, lower, ranges);
    }
}
