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
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.CollectionsExt;
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
     * The slices of this resource, in the same order than {@link #coordinatesOfSlices}.
     * Each slice is not necessarily 1 cell tick; larger slices are accepted.
     */
    private final GridCoverageResource[] slices;

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
        RasterLoadingStrategy common = null;
        for (final GridCoverageResource slice : slices) {
            final RasterLoadingStrategy sr = slice.getLoadingStrategy();
            if (sr != null) {       // Should never be null, but we are paranoiac.
                if (common == null || sr.ordinal() < common.ordinal()) {
                    common = sr;
                    if (common.ordinal() == 0) {
                        break;
                    }
                }
            }
        }
        return common;
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
        boolean accepted = false;
        for (final GridCoverageResource slice : slices) {
            accepted |= slice.setLoadingStrategy(strategy);
        }
        return accepted;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, final int... ranges) throws DataStoreException {
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
        final GridCoverage[] coverages = new GridCoverage[upper - lower];
        for (int i=0; i < coverages.length; i++) {
            final GridCoverageResource slice = slices[lower + i];
            if (slice instanceof MemoryGridResource) {
                coverages[i] = ((MemoryGridResource) slice).coverage;
            } else {
                coverages[i] = slice.read(domain, ranges);
            }
        }
        if (coverages.length == 1) {
            return coverages[0];
        }
        domain = locator.union(gridGeometry, Arrays.asList(coverages), (c) -> c.getGridGeometry().getExtent());
        return new ConcatenatedGridCoverage(this, domain, coverages, lower);
    }
}
