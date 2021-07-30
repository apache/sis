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
package org.apache.sis.internal.storage;

import java.util.List;
import java.util.Arrays;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.CannotEvaluateException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.ArraysExt;


/**
 * Base class of grid coverage resource storing data in tiles.
 * The word "tile" is used for simplicity but can be understood
 * as "chunk" in a <var>n</var>-dimensional generalization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class TiledGridResource extends AbstractGridResource {
    /**
     * A key in the {@link #rasters} cache of tiles.
     */
    static final class CacheKey {
        /** Index in a row-major array of tiles. */ private final int   indexInTileVector;
        /** Bands in strictly increasing order.  */ private final int[] selectedBands;
        /** Subsampling factors at read time.    */ private final int[] subsampling;
        /** Remainder of subsampling divisions.  */ private final int[] subsamplingOffsets;

        /** Creates a key with given arrays hold be reference (no copy). */
        CacheKey(final int indexInTileVector, final int[] selectedBands,
                 final int[] subsampling, final int[] subsamplingOffsets)
        {
            this.indexInTileVector  = indexInTileVector;
            this.selectedBands      = selectedBands;
            this.subsampling        = subsampling;
            this.subsamplingOffsets = subsamplingOffsets;
        }

        /** Returns a hash-code value for this key. */
        @Override public int hashCode() {
            return indexInTileVector
                    +   73 * Arrays.hashCode(selectedBands)
                    + 1063 * Arrays.hashCode(subsampling)
                    + 7919 * Arrays.hashCode(subsamplingOffsets);
        }

        /** Compares this key with the given object for equality. */
        @Override public boolean equals(final Object obj) {
            if (obj instanceof CacheKey) {
                final CacheKey other = (CacheKey) obj;
                return indexInTileVector == other.indexInTileVector
                        && Arrays.equals(selectedBands,      other.selectedBands)
                        && Arrays.equals(subsampling,        other.subsampling)
                        && Arrays.equals(subsamplingOffsets, other.subsamplingOffsets);
            }
            return false;
        }
    }

    /**
     * All tiles loaded by any {@link TiledGridCoverage} created from this resource.
     * Keys contains tile indices in a row-major array of tiles.
     * For each value, the {@link WritableRaster#getMinX()} and {@code minY} values
     * can be anything, depending which {@link TiledGridResource} was first to load the tile.
     *
     * @see TiledGridCoverage#rasters
     * @see TiledGridCoverage.AOI#getCachedTile()
     */
    private final WeakValueHashMap<CacheKey, WritableRaster> rasters;

    /**
     * Whether all tiles should be loaded at {@code read(…)} method call or deferred to a later time.
     * This field is initially {@code null} and is initialized to its default value only when needed.
     *
     * @see #getLoadingStrategy()
     * @see #setLoadingStrategy(RasterLoadingStrategy)
     */
    private RasterLoadingStrategy loadingStrategy;

    /**
     * Creates a new resource.
     *
     * @param  parent  listeners of the parent resource, or {@code null} if none.
     */
    protected TiledGridResource(final StoreListeners parent) {
        super(parent);
        rasters = new WeakValueHashMap<>(CacheKey.class);
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     * In current implementation, this is the {@link DataStore} that created this resource.
     * However this restriction may be relaxed in any future version.
     *
     * @return the synchronization lock.
     */
    protected abstract DataStore getSynchronizationLock();

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions.
     *
     * @return the size of tiles in this resource.
     */
    protected abstract int[] getTileSize();

    /**
     * Returns the Java2D sample model describing pixel type and layout for all bands.
     * The raster size is the {@linkplain #getTileSize() tile size} as stored in the resource.
     *
     * <h4>Multi-dimensional data cube</h4>
     * If this resource has more than 2 dimensions, then this model is for the two first ones (usually horizontal).
     * The images for all levels in additional dimensions shall use the same sample model.
     *
     * @return the sample model for tiles at full resolution with all their bands.
     * @throws DataStoreException if an error occurred during sample model construction.
     */
    protected abstract SampleModel getSampleModel() throws DataStoreException;

    /**
     * Returns the Java2D color model for rendering images, or {@code null} if none.
     * The color model shall be compatible with the sample model returned by {@link #getSampleModel()}.
     *
     * @return a color model compatible with {@link #getSampleModel()}, or {@code null} if none.
     * @throws DataStoreException if an error occurred during color model construction.
     */
    protected abstract ColorModel getColorModel() throws DataStoreException;

    /**
     * Returns the value to use for filling empty spaces in rasters,
     * or {@code null} if none, not different than zero or not valid for the target data type.
     * This value is used if a tile contains less pixels than expected.
     * The zero value is excluded because tiles are already initialized to zero by default.
     *
     * @return the value to use for filling empty spaces in rasters.
     * @throws DataStoreException if an error occurred while fetching filling information.
     */
    protected abstract Number getFillValue() throws DataStoreException;

    /**
     * Parameters that describe the resource subset to be accepted by the {@link TiledGridCoverage} constructor.
     * This is a temporary class used only for transferring information from {@link TiledGridResource}.
     * This class does not perform I/O operations.
     */
    public final class Subset {
        /**
         * The full size of the coverage in the enclosing {@link TiledGridResource}.
         */
        final GridExtent sourceExtent;

        /**
         * The area to read in unit of the full coverage (without subsampling).
         * This is the intersection between user-specified domain and enclosing
         * {@link TiledGridResource} domain, expanded to an integer number of tiles.
         */
        final GridExtent readExtent;

        /**
         * The sub-region extent, CRS and conversion from cell indices to CRS.
         * This is the domain of the grid coverage to create.
         */
        final GridGeometry domain;

        /**
         * Sample dimensions for each image band.
         * This is the range of the grid coverage to create.
         */
        final List<? extends SampleDimension> ranges;

        /**
         * Indices of {@link TiledGridResource} bands which have been retained for inclusion
         * in the {@link TiledGridCoverage} to construct, in strictly increasing order.
         * This is {@code null} if all bands shall be included.
         *
         * <p>If the user specified bands out of order, the change of band order is taken in
         * account by the {@link #modelForBandSubset}. This {@code selectedBands} array does
         * not show any change of order for making sequential readings easier.</p>
         */
        final int[] selectedBands;

        /**
         * Coordinate conversion from subsampled grid to the grid at full resolution.
         * This array contains the factors by which to divide {@link TiledGridResource}
         * cell coordinates in order to obtain {@link TiledGridCoverage} cell coordinates.
         */
        final int[] subsampling;

        /**
         * Remainder of the divisions of {@link TiledGridResource} cell coordinates by subsampling factors.
         */
        final int[] subsamplingOffsets;

        /**
         * Size of tiles (or chunks) in the resource, without clipping and subsampling.
         */
        final int[] tileSize;

        /**
         * The sample model for the bands to read (not the full set of bands in the resource).
         * The width is {@code tileSize[0]} and the height it {@code tileSize[1]},
         * i.e. subsampling is <strong>not</strong> applied.
         */
        final SampleModel modelForBandSubset;

        /**
         * The color model for the bands to read (not the full set of bands in the resource).
         */
        final ColorModel colorsForBandSubset;

        /**
         * Value to use for filling empty spaces in rasters, or {@code null} if none,
         * not different than zero or not valid for the target data type.
         */
        final Number fillValue;

        /**
         * Cache to use for tiles loaded by the {@link TiledGridCoverage}.
         * It is a reference to {@link TiledGridResource#rasters} if shareable.
         */
        final WeakValueHashMap<CacheKey, WritableRaster> cache;

        /**
         * Creates parameters for the given domain and range.
         *
         * @param  domain  the domain argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @param  range   the range argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @param  loadAllBands {@code true} if the reader will load all bands regardless the {@code range} subset,
         *         or {@code false} if only requested bands will be loaded and other bands will be skipped. See
         *         {@link AbstractGridResource.RangeArgument#select(SampleModel, boolean) RangeArgument.select(…)}
         *         for more information.
         *
         * @throws ArithmeticException if pixel indices exceed 64 bits integer capacity.
         * @throws DataStoreException if a call to {@link TiledGridResource} method failed.
         * @throws RasterFormatException if the sample model is not recognized.
         * @throws IllegalArgumentException if an error occurred in an operation
         *         such as creating the {@code SampleModel} subset for selected bands.
         */
        public Subset(GridGeometry domain, final int[] range, final boolean loadAllBands) throws DataStoreException {
            List<SampleDimension> bands        = getSampleDimensions();
            final RangeArgument   rangeIndices = validateRangeArgument(bands.size(), range);
            final GridGeometry    gridGeometry = getGridGeometry();
            sourceExtent = gridGeometry.getExtent();
            tileSize = getTileSize();
            boolean sharedCache = true;
            if (domain == null) {
                domain             = gridGeometry;
                readExtent         = sourceExtent;
                subsamplingOffsets = new int[gridGeometry.getDimension()];
                subsampling        = new int[subsamplingOffsets.length];
                Arrays.fill(subsampling, 1);
            } else {
                /*
                 * If an area of interest has been specified, we may need to expand it to an integer amount of tiles.
                 * But we do not need to do that if the image is untiled; it is okay to read only a sub-region of the
                 * single tile. We disable the "integer amount of tiles" restriction by setting the tile size to 1.
                 * Note that it is possible to disable this restriction in a single dimension, typically the X one
                 * when reading a TIFF image using strips instead of tiles.
                 */
                int tileWidth  = tileSize[0];
                int tileHeight = tileSize[1];
                if (tileWidth  >= sourceExtent.getSize(0)) {tileWidth  = 1; sharedCache = false;}
                if (tileHeight >= sourceExtent.getSize(1)) {tileHeight = 1; sharedCache = false;}
                final GridDerivation target = gridGeometry.derive().chunkSize(tileWidth, tileHeight)
                                              .rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
                domain             = target.build();
                readExtent         = target.getIntersection();
                subsampling        = target.getSubsampling();
                subsamplingOffsets = target.getSubsamplingOffsets();
            }
            /*
             * Get the bands selected by user in strictly increasing order of source band index.
             * If user has specified bands in a different order, that change of band order will
             * be handled by the `SampleModel`, not in `selectedBands` array.
             */
            int[] selectedBands = null;
            if (!rangeIndices.isIdentity()) {
                bands = Arrays.asList(rangeIndices.select(bands));
                selectedBands = new int[rangeIndices.getNumBands()];
                for (int i=0; i<selectedBands.length; i++) {
                    selectedBands[i] = rangeIndices.getSourceIndex(i);
                }
                assert ArraysExt.isSorted(selectedBands, true);
                if (rangeIndices.hasAllBands) {
                    assert ArraysExt.isRange(0, selectedBands);
                    selectedBands = null;
                }
            }
            this.domain              = domain;
            this.ranges              = bands;
            this.selectedBands       = selectedBands;
            this.modelForBandSubset  = rangeIndices.select(getSampleModel(), loadAllBands);
            this.colorsForBandSubset = rangeIndices.select(getColorModel());
            this.fillValue           = getFillValue();
            /*
             * All `TiledGridCoverage` instances can share the same cache if they read all tiles fully.
             * If they read only sub-regions or apply subsampling, then they will need their own cache.
             */
            cache = sharedCache ? rasters : new WeakValueHashMap<>(CacheKey.class);
        }

        /**
         * Returns {@code true} if this subset contains a subsampling in the given dimension.
         *
         * @param  dimension  the dimension to test.
         * @return {@code true} if there is a subsampling in the given dimension.
         */
        public boolean hasSubsampling(final int dimension) {
            return subsampling[dimension] != 1;
        }

        /**
         * Returns {@code true} if the user requested a subset of bands, or {@code false}
         * if all bands shall be read in order. For this method, a change of band order
         * is considered as a "subset".
         *
         * @return {@code true} if only a subset of bands will be read or if bands will be read out of order.
         */
        public boolean hasBandSubset() {
            return selectedBands != null;
        }
    }

    /**
     * If the loading strategy is to load all tiles at {@code read(…)} time, replaces the given coverage
     * by a coverage will all data in memory. This method should be invoked by subclasses at the end of
     * their {@link #read(GridGeometry, int...)} method implementation.
     *
     * @param  coverage  the {@link TiledGridCoverage} to potentially replace by a coverage with preloaded data.
     * @return a coverage with preloaded data, or the given coverage if preloading is not enabled.
     * @throws DataStoreException if an error occurred while preloading data.
     */
    protected final GridCoverage preload(final GridCoverage coverage) throws DataStoreException {
        assert Thread.holdsLock(getSynchronizationLock());
        // Note: `loadingStrategy` may still be null if unitialized.
        if (loadingStrategy != RasterLoadingStrategy.AT_RENDER_TIME) {
            /*
             * In theory the following condition is redundant with `supportImmediateLoading()`.
             * We apply it anyway in case the coverage geometry is not what was announced.
             * This condition is also necessary if `loadingStrategy` has not been initialized.
             */
            if (coverage.getGridGeometry().getDimension() == TiledGridCoverage.BIDIMENSIONAL) try {
                final RenderedImage image = coverage.render(null);
                return new GridCoverage2D(coverage.getGridGeometry(), coverage.getSampleDimensions(), image);
            } catch (RuntimeException e) {
                /*
                 * The `coverage.render(…)` implementation may have wrapped the checked `DataStoreException`
                 * because of API restriction. In that case we can unwrap the exception here since this API
                 * allows it. This is one of the reasons for preferring the `AT_READ_TIME` loading mode.
                 */
                Throwable cause = e.getCause();
                if (cause instanceof DataStoreException) {
                    throw (DataStoreException) cause;
                }
                /*
                 * The `CannotEvaluateException` wrapper is created by `TiledGridCoverage.render(…)`,
                 * in which case we avoid that level of indirection for making stack trace simpler.
                 * But if the exception is another kind, keep it.
                 */
                if (cause == null || !(e instanceof CannotEvaluateException)) {
                    cause = e;
                }
                throw new DataStoreException(e.getLocalizedMessage(), cause);
            }
        }
        return coverage;
    }

    /**
     * Whether this resource supports immediate loading of raster data.
     */
    private boolean supportImmediateLoading() {
        return getTileSize().length == TiledGridCoverage.BIDIMENSIONAL;
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     *
     * @return current raster data loading strategy for this resource.
     */
    @Override
    public final RasterLoadingStrategy getLoadingStrategy() {
        synchronized (getSynchronizationLock()) {
            if (loadingStrategy == null) {
                setLoadingStrategy(supportImmediateLoading());
            }
            return loadingStrategy;
        }
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     *
     * @param  strategy  the desired strategy for loading raster data.
     * @return {@code true} if the given strategy has been accepted, or {@code false}
     *         if this implementation replaced the given strategy by an alternative.
     */
    @Override
    public final boolean setLoadingStrategy(final RasterLoadingStrategy strategy) {
        synchronized (getSynchronizationLock()) {
            if (strategy != null) {
                setLoadingStrategy(strategy == RasterLoadingStrategy.AT_READ_TIME && supportImmediateLoading());
            }
            return super.setLoadingStrategy(strategy);
        }
    }

    /**
     * Sets the strategy for the given flag.
     *
     * @param  loadAtReadTime  whether all tiles should be read immediately
     *         at {@code read(…)} method call or deferred at a later time.
     */
    private void setLoadingStrategy(final boolean loadAtReadTime) {
        loadingStrategy = loadAtReadTime ? RasterLoadingStrategy.AT_READ_TIME
                                         : RasterLoadingStrategy.AT_RENDER_TIME;
    }
}
