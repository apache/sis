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
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.internal.coverage.MultiSourceArgument;
import org.apache.sis.internal.coverage.RangeArgument;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * A resource whose range is the aggregation of the ranges of a sequence of resources.
 * This class combines homogeneous {@link GridCoverageResource}s by "stacking" their bands.
 * The grid geometry is typically the same for all resources, but some variations described below are allowed.
 * The number of sample dimensions in the aggregated coverage is the sum of the number of sample dimensions in
 * each individual resource, unless a subset of sample dimensions is specified.
 *
 * <h2>Restrictions</h2>
 * <ul>
 *   <li>All resources shall use the same coordinate reference system (CRS).</li>
 *   <li>All resources shall have the same {@linkplain GridCoverageResource#getGridGeometry() domain}, except
 *       for the grid extent and the translation terms which can vary by integer amounts of grid cells.</li>
 *   <li>All grid extents shall intersect and the intersection area shall be non-empty.</li>
 *   <li>If coverage data are stored in {@link java.awt.image.RenderedImage} instances,
 *       then all images shall use the same data type.</li>
 * </ul>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see GridCoverageProcessor#aggregateRanges(GridCoverage[], int[][])
 *
 * @since 1.4
 */
public class BandAggregateGridResource extends AbstractGridCoverageResource {
    /**
     * Identifier of this resource.
     *
     * @see #getIdentifier()
     */
    private final GenericName name;

    /**
     * The source grid coverage resources.
     */
    private final GridCoverageResource[] sources;

    /**
     * The grid geometry of this resources, computed from all sources.
     * All sources shall have this grid geometry except for grid extent and translation terms.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * The resolutions, or {@code null} if not yet computed.
     * Can be an empty array after computation.
     * Shall be read-only after computation.
     *
     * @see #getResolutions()
     */
    private double[][] resolutions;

    /**
     * The union or a subset of the union of all sample dimensions in this combined resource.
     * Should be an unmodifiable list.
     *
     * @see #getSampleDimensions()
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * The sample dimensions to use for each source coverage, in order.
     * The length of this array is always equal to {@link #sources} array length.
     * This array does not contain any {@code null} or empty element.
     */
    private final int[][] bandsPerSource;

    /**
     * The processor to use for creating grid coverages.
     */
    private final GridCoverageProcessor processor;

    /**
     * Creates a new aggregation of all sample dimensions of all specified grid coverage resources.
     * The new resource has no name and no parent, and use a default processor with default color model.
     *
     * @param  sources  resources whose bands shall be aggregated, in order. At least one resource must be provided.
     * @throws DataStoreException if an error occurred while fetching the grid geometry or sample dimensions from a resource.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     */
    public BandAggregateGridResource(final GridCoverageResource... sources) throws DataStoreException {
        this(null, null, sources, null, null);
    }

    /**
     * Creates a new range aggregation of grid coverage resources.
     * The {@linkplain #getSampleDimensions() list of sample dimensions} of the aggregated resource
     * will be the concatenation of the lists of all sources, or a subset of this concatenation.
     *
     * <p>The {@code bandsPerSource} argument specifies the bands to select in each resource.
     * That array can be {@code null} for selecting all bands in all resources,
     * or may contain {@code null} elements for selecting all bands of the corresponding resource.
     * An empty array element (i.e. zero band to select) discards the corresponding resource.
     * In the latter case, the discarded element in the {@code sources} array may be {@code null}.</p>
     *
     * <h4>Restrictions</h4>
     * All resources shall have compatible domain, defined as below:
     *
     * <ul>
     *   <li>Same CRS.</li>
     *   <li>Same <cite>grid to CRS</cite> transform except for translation terms.</li>
     *   <li>Translation terms that differ only by an integer amount of grid cells.</li>
     * </ul>
     *
     * The intersection of the domain of all resources shall be non-empty,
     * and all resources shall use the same data type in their rendered image.
     *
     * @param  parent          the parent resource, or {@code null} if none.
     * @param  name            name of the combined grid coverage resource, or {@code null} if none.
     * @param  sources         resources whose bands shall be aggregated, in order. At least one resource must be provided.
     * @param  bandsPerSource  sample dimensions for each source. May be {@code null} or may contain {@code null} elements.
     * @param  processor       the processor to use for creating grid coverages, or {@code null} for a default processor.
     * @throws DataStoreException if an error occurred while fetching the grid geometry or sample dimensions from a resource.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    public BandAggregateGridResource(final Resource parent, final GenericName name,
            final GridCoverageResource[] sources, final int[][] bandsPerSource,
            final GridCoverageProcessor processor) throws DataStoreException
    {
        super(parent);
        try {
            final var aggregate = new MultiSourceArgument<GridCoverageResource>(sources, bandsPerSource);
            aggregate.validate(BandAggregateGridResource::range);
            this.name             = name;
            this.sources          = aggregate.sources();
            this.gridGeometry     = aggregate.domain(BandAggregateGridResource::domain);
            this.sampleDimensions = List.copyOf(aggregate.ranges());
            this.bandsPerSource   = aggregate.bandsPerSource();
            this.processor        = (processor != null) ? processor : new GridCoverageProcessor();
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }

    /**
     * A getter method for grid geometry,
     * wrapping the checked exception for enabling use with lambda functions.
     */
    private static GridGeometry domain(final GridCoverageResource source) {
        try {
            return source.getGridGeometry();
        } catch (DataStoreException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * A getter method for sample dimensions,
     * wrapping the checked exception for enabling use with lambda functions.
     */
    private static List<SampleDimension> range(final GridCoverageResource source) {
        try {
            return source.getSampleDimensions();
        } catch (DataStoreException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns the resource identifier if available.
     * This is the name specified at construction time.
     *
     * @return an identifier for the band aggregation.
     * @throws DataStoreException if the identifier can not be obtained.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the valid extent of grid coordinates and their conversions to real world coordinates.
     * This grid geometry is inferred from the grid geometries of resources specified at construction time.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if the grid geometry can not be obtained.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * This is the union or a subset of the union of the ranges of all resources specified at construction time.
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if the sample dimensions can not be obtained.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sampleDimensions;
    }

    /**
     * Builds metadata the first time that {@code getMetadata()} is invoked.
     * This method provides the metadata described in the
     * {@linkplain AbstractGridCoverageResource#createMetadata() super-class method},
     * with the addition of lineage information for each source.
     *
     * @return the newly created metadata, or {@code null} if unknown.
     * @throws DataStoreException if an error occurred while reading metadata from a resource.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        MetadataBuilder builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        for (GridCoverageResource source : sources) {
            builder.addSource(source.getMetadata());
        }
        return builder.build();
    }

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this resource.
     * The default implementation returns only the resolutions that are declared by all sources.
     *
     * @return preferred resolutions for read operations in this resource, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from an underlying resource.
     */
    @Override
    public synchronized List<double[]> getResolutions() throws DataStoreException {
        if (resolutions == null) {
            resolutions = ConcatenatedGridResource.commonResolutions(sources);
        }
        return UnmodifiableArrayList.wrap(resolutions);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * The default implementation returns the most conservative value of all sources.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     */
    @Override
    public synchronized RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        RasterLoadingStrategy conservative = RasterLoadingStrategy.AT_GET_TILE_TIME;
        for (final GridCoverageResource source : sources) {
            RasterLoadingStrategy s = source.getLoadingStrategy();
            if (s.ordinal() < conservative.ordinal()) {
                conservative = s;
                if (s.ordinal() == 0) break;
            }
        }
        return conservative;
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     *
     * @param  strategy  the desired strategy for loading raster data.
     * @return {@code true} if the given strategy has been accepted by all sources.
     * @throws DataStoreException if an error occurred while setting data store configuration.
     */
    @Override
    public synchronized boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        boolean accepted = true;
        for (final GridCoverageResource source : sources) {
            accepted &= source.setLoadingStrategy(strategy);
        }
        return accepted;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws IllegalArgumentException if the given domain or ranges are invalid.
     * @throws DataStoreException if an error occurred while reading a grid coverage data.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        final var validator     = RangeArgument.validate(sampleDimensions.size(), ranges, listeners);
        final int numBands      = validator.getNumBands();      // Number of bands that are requested.
        final var coverages     = new GridCoverage[numBands];   // Grid coverages to aggregate. Duplicates are allowed.
        final var coverageBands = new int[numBands][];          // Indices of `coverages` bands to aggregate.
        final var bandsToLoad   = new int[numBands];            // Buffer for bands to load with a single resource.
        int numBandsToLoad = 0;                                 // Number of valid indices in `bandsToLoad`.
        int coverageCursor = 0;                                 // Index in `bandsPerSource[#][]` for current source band.
        int bandCursor     = 0;                                 // Index in `bandsPerSource[][#]` for current source band.
        int cursorBase     = 0;                                 // First valid value of `cursorIndex` for current source.
        int cursorIndex    = 0;                                 // Current band according numeration of this resource.
        int pendingIndex   = 0;                                 // Next value of `i` where to write in `coverages` array.
        for (int i=0; i <= numBands; i++) {
            /*
             * Iterate over the requested bands in increasing index order.
             * The `source` value is guaranteed to be always increasing.
             * This is not necessarily the same order than the one specified by the user.
             * User order is taken in account later, with the call to `getTargetIndex(…)`.
             */
            final int source = (i != numBands) ? validator.getSourceIndex(i) : sampleDimensions.size();
            bandCursor += source - cursorIndex;
            int   bandCursorMax;
            int[] bandsForCurrentSource;
            while (bandCursor >= (bandCursorMax = (bandsForCurrentSource = bandsPerSource[coverageCursor]).length)) {
                /*
                 * If we enter in this block, the current band specified by user is on a different
                 * resources than the one in previous iteration. We need to execute the pending
                 * read operation before to move to the next resource.
                 */
                if (numBandsToLoad != 0) {
                    final GridCoverage data = sources[coverageCursor].read(domain, Arrays.copyOf(bandsToLoad, numBandsToLoad));
                    numBandsToLoad = 0;
                    int b = 0;
                    do {
                        final int target = validator.getTargetIndex(pendingIndex);
                        coverageBands[target] = new int[] {b++};
                        coverages[target] = data;
                    } while (++pendingIndex != i);
                }
                bandCursor -= bandCursorMax;
                cursorBase += bandCursorMax;
                if (++coverageCursor >= bandsPerSource.length) break;
            }
            cursorIndex = source;
            bandsToLoad[numBandsToLoad++] = bandsForCurrentSource[cursorIndex - cursorBase];
        }
        return processor.aggregateRanges(coverages, coverageBands);
    }
}
