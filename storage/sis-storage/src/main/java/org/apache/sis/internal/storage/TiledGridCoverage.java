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

import java.util.Map;
import java.util.Locale;
import java.io.IOException;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import org.opengis.util.GenericName;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.internal.coverage.j2d.DeferredProperty;
import org.apache.sis.internal.coverage.j2d.TiledImage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.decrementExact;
import static java.lang.Math.toIntExact;
import static java.lang.Math.floorDiv;
import static org.apache.sis.internal.util.Numerics.ceilDiv;
import static org.apache.sis.internal.jdk9.JDK9.multiplyFull;


/**
 * Base class of grid coverage read from a resource where data are stored in tiles.
 * This grid coverage may represent only a subset of the coverage resource.
 * Tiles are read from the storage only when first needed.
 *
 * <h2>Cell Coordinates</h2>
 * When there is no subsampling, this coverage uses the same cell coordinates than the originating resource.
 * When there is a subsampling, cell coordinates in this coverage are divided by the subsampling factors.
 * Conversions are done by {@link #toFullResolution(long, int)}.
 *
 * <h2>Tile coordinate matrix</h2>
 * In each {@code TiledGridCoverage}, indices of tiles starts at (0, 0, …).
 * This class does not use the same tile indices than the coverage resource
 * in order to avoid integer overflow.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class TiledGridCoverage extends GridCoverage {
    /**
     * Number of dimensions in a rendered image.
     * Used for identifying codes where a two-dimensional slice is assumed.
     */
    protected static final int BIDIMENSIONAL = 2;

    /**
     * The dimensions of <var>x</var> and <var>y</var> axes.
     * Static constants for now, may become configurable fields in the future.
     */
    protected static final int X_DIMENSION = 0, Y_DIMENSION = 1;

    /**
     * The area to read in unit of the full coverage (without subsampling).
     * This is the intersection between user-specified domain and the source
     * {@link TiledGridResource} domain, expanded to an integer number of tiles.
     */
    private final GridExtent readExtent;

    /**
     * Whether to force the {@link #readExtent} tile intersection to the {@link #tileSize}.
     * This is relevant only for the last column of tile matrix, because those tiles may be truncated
     * if the image size is not a multiple of tile size. It is usually necessary to read those tiles
     * fully anyway because otherwise, the pixels read from the storage would not be aligned with the
     * pixels stored in the {@link Raster}. However there is a few exceptions where the read extent
     * should not be forced to the tile size:
     *
     * <ul>
     *   <li>If the image is untiled, then the {@link org.apache.sis.internal.storage.TiledGridResource.Subset}
     *       constructor assumes that only the requested region of the tile will be read.</li>
     *   <li>If the tile is truncated on the storage as well
     *       (note: this is rare. GeoTIFF for example always stores whole tiles).</li>
     * </ul>
     *
     * <p>In current version this is a flag for the <var>x</var> dimension only. In a future version
     * it could be flags for other dimensions as well (using bitmask) if it appears to be useful.</p>
     */
    private final boolean forceTileSize;

    /**
     * Size of all tiles in the domain of this {@code TiledGridCoverage}, without clipping and subsampling.
     * All coverages created from the same {@link TiledGridResource} have the same tile size values.
     * The length of this array is the number of dimensions in the source {@link GridExtent}.
     * This is often {@value #BIDIMENSIONAL} but can also be more.
     */
    private final int[] tileSize;

    /**
     * Values by which to multiply each tile coordinates for obtaining the index in the tile vector.
     * The length of this array is the same as {@link #tileSize}. All coverages created from the same
     * {@link TiledGridResource} have the same stride values.
     */
    private final int[] tileStrides;

    /**
     * Index of the first {@code TiledGridCoverage} tile in a row-major array of tiles.
     * This is the value to add to the index computed with {@link #tileStrides} before to access vector elements.
     */
    private final int indexOfFirstTile;

    /**
     * The Tile Matrix Coordinates (TMC) of the first tile.
     * This is the value to subtract from tile indices computed from pixel coordinates.
     *
     * @see #indexOfFirstTile
     */
    private final long[] tmcOfFirstTile;

    /**
     * Conversion from pixel coordinates in this (potentially subsampled) coverage
     * to pixel coordinates in the resource coverage at full resolution.
     * The conversion from (<var>x</var>, <var>y</var>) to (<var>x′</var>, <var>y′</var>) is as below,
     * where <var>s</var> are subsampling factors and <var>t</var> are subsampling offsets:
     *
     * <ul>
     *   <li><var>x′</var> = s₀⋅<var>x</var> + t₀</li>
     *   <li><var>y′</var> = s₁⋅<var>y</var> + t₁</li>
     * </ul>
     *
     * This transform maps {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CORNER pixel corners}.
     *
     * @see #getSubsampling(int)
     * @see #toFullResolution(long, int)
     */
    private final int[] subsampling, subsamplingOffsets;

    /**
     * Indices of {@link TiledGridResource} bands which have been retained for
     * inclusion in this {@code TiledGridCoverage}, in strictly increasing order.
     * An "included" band is stored in memory but not necessarily visible to the user,
     * because the {@link SampleModel} can be configured for ignoring some bands.
     * This array is {@code null} if all bands shall be included.
     *
     * <p>If the user specified bands out of order, the change of band order is taken in account
     * by the sample {@link #model}. This {@code includedBands} array does not apply any change
     * of order for making sequential readings easier.</p>
     */
    protected final int[] includedBands;

    /**
     * Cache of rasters read by this {@code TiledGridCoverage}. This cache may be shared with other coverages
     * created for the same {@link TiledGridResource} resource. For each value, the raster {@code minX} and
     * {@code minY} values can be anything, depending which {@code TiledGridCoverage} was first to load the tile.
     *
     * @see TiledGridResource#rasters
     * @see AOI#getCachedTile()
     * @see #createCacheKey(int)
     */
    private final WeakValueHashMap<TiledGridResource.CacheKey, Raster> rasters;

    /**
     * The sample model for all rasters. The width and height of this sample model are the two first elements
     * of {@link #tileSize} divided by subsampling and clipped to the domain. If user requested to read only
     * a subset of the bands, then this sample model is already the subset.
     */
    protected final SampleModel model;

    /**
     * The Java2D color model for images rendered from this coverage.
     */
    protected final ColorModel colors;

    /**
     * The value to use for filling empty spaces in rasters, or {@code null} if zero.
     */
    protected final Number fillValue;

    /**
     * Whether the reading of tiles is deferred to {@link RenderedImage#getTile(int, int)} time.
     */
    private final boolean deferredTileReading;

    /**
     * Creates a new tiled grid coverage. All parameters should have been validated before this call.
     *
     * @param  subset  description of the {@link TiledGridResource} subset to cover.
     * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
     */
    protected TiledGridCoverage(final TiledGridResource.Subset subset) {
        super(subset.domain, subset.ranges);
        final GridExtent extent = subset.domain.getExtent();
        final int dimension = subset.sourceExtent.getDimension();
        deferredTileReading = subset.deferredTileReading();
        readExtent          = subset.readExtent;
        subsampling         = subset.subsampling;
        subsamplingOffsets  = subset.subsamplingOffsets;
        includedBands       = subset.includedBands;
        rasters             = subset.cache;
        tileSize            = subset.tileSize;
        tmcOfFirstTile      = new long[dimension];
        tileStrides         = new int [dimension];
        final int[] subSize = new int [dimension];
        int  tileStride       = 1;
        long indexOfFirstTile = 0;
        for (int i=0; i<dimension; i++) {
            final int ts      = tileSize[i];
            tmcOfFirstTile[i] = floorDiv(readExtent.getLow(i), ts);
            tileStrides[i]    = tileStride;
            subSize[i]        = (int) Math.min(((ts-1) / subsampling[i]) + 1, extent.getSize(i));
            indexOfFirstTile  = addExact(indexOfFirstTile, multiplyExact(tmcOfFirstTile[i], tileStride));
            int tileCount     = toIntExact(ceilDiv(subset.sourceExtent.getSize(i), ts));
            tileStride        = multiplyExact(tileCount, tileStride);
        }
        this.indexOfFirstTile = toIntExact(indexOfFirstTile);
        /*
         * At this point, `tileStride` is the total number of tiles in source.
         * This value is not stored but its computation is still useful because
         * we want `ArithmeticException` to be thrown if the value is too high.
         */
        SampleModel model = subset.modelForBandSubset;
        if (model.getWidth() != subSize[X_DIMENSION] || model.getHeight() != subSize[Y_DIMENSION]) {
            model = model.createCompatibleSampleModel(subSize[X_DIMENSION], subSize[Y_DIMENSION]);
        }
        this.model     = model;
        this.colors    = subset.colorsForBandSubset;
        this.fillValue = subset.fillValue;
        forceTileSize  = subSize[X_DIMENSION] * subsampling[X_DIMENSION] == tileSize[X_DIMENSION];
    }

    /**
     * Returns a unique name that identifies this coverage.
     * The name shall be unique in the {@link TileMatrixSet}.
     *
     * @return an human-readable identification of this coverage.
     */
    protected abstract GenericName getIdentifier();

    /**
     * Returns the locale for error messages, or {@code null} for the default.
     *
     * @return the locale for warning or error messages, or {@code null} if unspecified.
     */
    protected Locale getLocale() {
        return null;
    }

    /**
     * Returns the size of all tiles in the domain of this {@code TiledGridCoverage}, without clipping and subsampling.
     *
     * @param  dimension  dimension for which to get tile size.
     * @return tile size in the given dimension, without clipping and subsampling.
     */
    protected final int getTileSize(final int dimension) {
        return tileSize[dimension];
    }

    /**
     * Returns the subsampling in the given dimension.
     *
     * @param  dimension  dimension for which to get subsampling.
     * @return subsampling as a value ≥ 1.
     */
    protected final int getSubsampling(final int dimension) {
        return subsampling[dimension];
    }

    /**
     * Converts a cell coordinate from this {@code TiledGridCoverage} coordinate space to full resolution.
     * This method removes the subsampling effect. Note that since this {@code TiledGridCoverage} uses the
     * same coordinate space than {@link TiledGridResource}, the converted coordinates should be valid in
     * the full resource as well.
     *
     * @param  coordinate  coordinate in this {@code TiledGridCoverage} domain.
     * @param  dimension   dimension of the coordinate.
     * @return coordinate in this {@code TiledGridCoverage} as if no subsampling was applied.
     * @throws ArithmeticException if the coordinate can not be represented as a long integer.
     */
    private long toFullResolution(final long coordinate, final int dimension) {
        return addExact(multiplyExact(coordinate, subsampling[dimension]), subsamplingOffsets[dimension]);
    }

    /**
     * Converts a cell coordinate from {@link TiledGridResource} space to {@code TiledGridCoverage} coordinate.
     * This is the converse of {@link #toFullResolution(long, int)}. Note that there is a possible accuracy lost.
     *
     * @param  coordinate  coordinate in the {@code TiledGridResource} domain.
     * @param  dimension   dimension of the coordinate.
     * @return coordinates in this subsampled {@code TiledGridCoverage} domain.
     * @throws ArithmeticException if the coordinate can not be represented as a long integer.
     */
    private long toSubsampledPixel(final long coordinate, final int dimension) {
        return floorDiv(subtractExact(coordinate, subsamplingOffsets[dimension]), subsampling[dimension]);
    }

    /**
     * Converts a cell coordinate from this {@code TiledGridCoverage} coordinate space
     * to the Tile Matrix Coordinate (TMC) of the tile which contains that cell.
     * The TMC is relative to the full {@link TiledGridResource},
     * i.e. without subtraction of {@link #tmcOfFirstTile}.
     *
     * @param  coordinate  coordinates in this {@code TiledGridCoverage} domain.
     * @param  dimension   dimension of the coordinate.
     * @return Tile Matrix Coordinate (TMC) of the tile which contains the specified cell.
     * @throws ArithmeticException if the coordinate can not be represented as an integer.
     */
    private long toTileMatrixCoordinate(final long coordinate, final int dimension) {
        return floorDiv(toFullResolution(coordinate, dimension), tileSize[dimension]);
    }

    /**
     * Returns the number of pixels in a single bank element. This is usually 1, except for
     * {@link MultiPixelPackedSampleModel} which packs many pixels in a single bank element.
     * This value is a power of 2 according {@code MultiPixelPackedSampleModel} specification.
     *
     * <div class="note"><b>Note:</b>
     * this is "pixels per element", not "samples per element". It makes a difference in the
     * {@link java.awt.image.SinglePixelPackedSampleModel} case, for which this method returns 1
     * (by contrast a "samples per element" would give a value greater than 1).
     * But this value can nevertheless be understood as a "samples per element" value
     * where only one band is considered at a time.</div>
     *
     * @return number of pixels in a single bank element. Usually 1.
     *
     * @see SampleModel#getSampleSize(int)
     * @see MultiPixelPackedSampleModel#getPixelBitStride()
     */
    protected final int getPixelsPerElement() {
        return getPixelsPerElement(model);
    }

    /**
     * Implementation of {@link #getPixelsPerElement()}.
     *
     * @param  model  the sample model from which to infer the number of pixels per bank element.
     * @return number of pixels in a single bank element. Usually 1.
     */
    static int getPixelsPerElement(final SampleModel model) {
        if (model instanceof MultiPixelPackedSampleModel) {
            /*
             * The following code performs the same computation than `MultiPixelPackedSampleModel`
             * constructor when computing its package-private field `pixelsPerDataElement`.
             * That constructor ensured that `sampleSize` is a divisor of `typeSize`.
             */
            final int typeSize   = DataBuffer.getDataTypeSize(model.getDataType());
            final int sampleSize = ((MultiPixelPackedSampleModel) model).getPixelBitStride();
            final int pixelsPerElement = typeSize / sampleSize;
            if (pixelsPerElement > 0) {                         // Paranoiac check.
                return pixelsPerElement;
            }
        }
        return 1;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     *
     * @param  sliceExtent  a subspace of this grid coverage extent, or {@code null} for the whole image.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        final GridExtent available = gridGeometry.getExtent();
        final int dimension = available.getDimension();
        if (sliceExtent == null) {
            sliceExtent = available;
        } else {
            final int sd = sliceExtent.getDimension();
            if (sd != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "sliceExtent", dimension, sd));
            }
        }
        final int[] selectedDimensions = sliceExtent.getSubspaceDimensions(BIDIMENSIONAL);
        if (selectedDimensions[1] != 1) {
            // TODO
            throw new UnsupportedOperationException("Non-horizontal slices not yet implemented.");
        }
        final RenderedImage image;
        try {
            final int[] tileLower = new int[dimension];         // Indices of first tile to read, inclusive.
            final int[] tileUpper = new int[dimension];         // Indices of last tile to read, exclusive.
            final int[] offsetAOI = new int[dimension];         // Pixel offset compared to Area Of Interest.
            final int[] imageSize = new int[dimension];         // Subsampled image size.
            for (int i=0; i<dimension; i++) {
                final long min    = available  .getLow (i);     // Lowest valid coordinate in subsampled image.
                final long max    = available  .getHigh(i);     // Highest valid coordinate, inclusive.
                final long aoiMin = sliceExtent.getLow (i);     // Requested coordinate in subsampled image.
                final long aoiMax = sliceExtent.getHigh(i);
                final long tileUp = incrementExact(toTileMatrixCoordinate(Math.min(aoiMax, max), i));
                final long tileLo =                toTileMatrixCoordinate(Math.max(aoiMin, min), i);
                if (tileUp <= tileLo) {
                    final String message = Errors.getResources(getLocale())
                            .getString(Errors.Keys.IllegalRange_2, aoiMin, aoiMax);
                    if (aoiMin > aoiMax) {
                        throw new IllegalArgumentException(message);
                    } else {
                        throw new DisjointExtentException(message);
                    }
                }
                // Lower and upper coordinates in subsampled image, rounded to integer number of tiles and clipped to available data.
                final long lower = /* inclusive */Math.max(toSubsampledPixel(/* inclusive */multiplyExact(tileLo, tileSize[i]),  i), min);
                final long upper = incrementExact(Math.min(toSubsampledPixel(decrementExact(multiplyExact(tileUp, tileSize[i])), i), max));
                imageSize[i] = toIntExact(subtractExact(upper, lower));
                offsetAOI[i] = toIntExact(subtractExact(lower, aoiMin));
                tileLower[i] = toIntExact(subtractExact(tileLo, tmcOfFirstTile[i]));
                tileUpper[i] = toIntExact(subtractExact(tileUp, tmcOfFirstTile[i]));
            }
            /*
             * Prepare an iterator over all tiles to read, together with the following properties:
             *    - Two-dimensional conversion from pixel coordinates to "real world" coordinates.
             */
            final AOI iterator = new AOI(tileLower, tileUpper, offsetAOI, dimension);
            final Map<String,Object> properties = DeferredProperty.forGridGeometry(gridGeometry, selectedDimensions);
            if (deferredTileReading) {
                image = new TiledDeferredImage(imageSize, tileLower, properties, iterator);
            } else {
                /*
                 * If the loading strategy is not `RasterLoadingStrategy.AT_GET_TILE_TIME`, get all tiles
                 * in the area of interest now. I/O operations, if needed, happen in `readTiles(…)` call.
                 */
                final Raster[] result = readTiles(iterator);
                image = new TiledImage(properties, colors,
                        imageSize[X_DIMENSION], imageSize[Y_DIMENSION],
                        tileLower[X_DIMENSION], tileLower[Y_DIMENSION], result);
            }
        } catch (Exception e) {     // Too many exception types for listing them all.
            throw new CannotEvaluateException(Resources.forLocale(getLocale()).getString(
                    Resources.Keys.CanNotRenderImage_1, getIdentifier().toFullyQualifiedName()), e);
        }
        return image;
    }

    /**
     * The Area Of Interest specified by user in a call to {@link #render(GridExtent)}.
     * This class is also an iterator over tiles in the region of interest.
     */
    protected final class AOI {
        /**
         * Total number of tiles in the AOI, from {@link #tileLower} inclusive to {@link #tileUpper} exclusive.
         * This is the length of the array to be returned by {@link #readTiles(AOI)}.
         */
        public final int tileCountInQuery;

        /**
         * Indices (relative to enclosing {@code TiledGridCoverage}) of the upper-left tile to read.
         * Tile indices starts at (0, 0, …), not at the indices of the corresponding tile in resource,
         * for avoiding integer overflow.
         */
        private final int[] tileLower;

        /**
         * Indices (relative to enclosing {@code TiledGridCoverage}) after the bottom-right tile to read.
         */
        private final int[] tileUpper;

        /**
         * Pixel coordinates to assign to the upper-left corner of the region to render, with subsampling applied.
         * This is the difference between the region requested by user and the region which will be rendered.
         */
        private final int[] offsetAOI;

        /**
         * Tile Matrix Coordinates (TMC) of current iterator position relative to enclosing {@code TiledGridCoverage}.
         * Initial position is a clone of {@link #tileLower}. This array is modified by calls to {@link #next()}.
         */
        private final int[] tmcInSubset;

        /**
         * Pixel coordinates of current iterator position relative to the Area Of Interest specified by user.
         * Those coordinates are in units of the full resolution image.
         * Initial position is {@link #offsetAOI} multiplied by {@link #subsampling}.
         * This array is modified by calls to {@link #next()}.
         */
        private final long[] tileOffsetFull;

        /**
         * Current iterator position as an index in the array of tiles to be returned by {@link #readTiles(AOI)}.
         * The initial position is zero. This field is incremented by calls to {@link #next()}.
         */
        private int indexInResultArray;

        /**
         * Current iterator position as an index in the vector of tiles in the {@link TiledGridResource}.
         * Tiles are assumed stored in a row-major fashion. This field is incremented by calls to {@link #next()}.
         */
        private int indexInTileVector;

        /**
         * Creates a new Area Of Interest for the given tile indices.
         *
         * @param  tileLower  indices (relative to enclosing {@code TiledGridCoverage}) of the upper-left tile to read.
         * @param  tileUpper  indices (relative to enclosing {@code TiledGridCoverage}) after the bottom-right tile to read.
         * @param  offsetAOI  pixel coordinates to assign to the upper-left corner of the subsampled region to render.
         * @param  dimension  number of dimension of the {@code TiledGridCoverage} grid extent.
         */
        AOI(final int[] tileLower, final int[] tileUpper, final int[] offsetAOI, final int dimension) {
            this.tileLower = tileLower;
            this.tileUpper = tileUpper;
            this.offsetAOI = offsetAOI;
            tileOffsetFull = new long[offsetAOI.length];
            /*
             * Initialize variables to values for the first tile to read. The loop does arguments validation and
             * converts the `tileLower` coordinates to index in the `tileOffsets` and `tileByteCounts` vectors.
             */
            indexInTileVector = indexOfFirstTile;
            int tileCountInQuery = 1;
            for (int i=0; i<dimension; i++) {
                final int lower   = tileLower[i];
                final int count   = subtractExact(tileUpper[i], lower);
                indexInTileVector = addExact(indexInTileVector, multiplyExact(tileStrides[i], lower));
                tileCountInQuery  = multiplyExact(tileCountInQuery, count);
                tileOffsetFull[i] = multiplyFull(offsetAOI[i], subsampling[i]);
                /*
                 * Following is the pixel coordinate after the last pixel in current dimension.
                 * This is not stored; the intent is to get a potential `ArithmeticException`
                 * now instead of in a call to `next()` during iteration. A negative value
                 * would mean that the AOI does not intersect the region requested by user.
                 */
                final int max = addExact(offsetAOI[i], multiplyExact(tileSize[i], count));
                assert max > Math.max(offsetAOI[i], 0) : max;
            }
            this.tileCountInQuery = tileCountInQuery;
            this.tmcInSubset      = tileLower.clone();
        }

        /**
         * Returns a new {@code AOI} instance over a sub-region of this Area Of Interest.
         * The region is specified by tile indices, with (0,0) being the first tile of the enclosing grid coverage.
         * The given region is intersected with the region of this {@code AOI}.
         * The {@code tileLower} and {@code tileUpper} array can have any length;
         * extra indices are ignored and missing indices are inherited from this AOI.
         * This method is independent to the iterator position of this {@code AOI}.
         *
         * @param  tileLower  indices (relative to enclosing {@code TiledGridCoverage}) of the upper-left tile to read.
         * @param  tileUpper  indices (relative to enclosing {@code TiledGridCoverage}) after the bottom-right tile to read.
         * @return a new {@code AOI} instance for the specified sub-region.
         */
        public AOI subset(final int[] tileLower, final int[] tileUpper) {
            final int[] offset = this.offsetAOI.clone();
            final int[] lower  = this.tileLower.clone();
            for (int i = Math.min(tileLower.length, lower.length); --i >= 0;) {
                final int base = lower[i];
                final int s = tileLower[i];
                if (s > base) {
                    lower[i] = s;
                    // Use of `ceilDiv(…)` is for consistency with `getTileOrigin(int)`.
                    offset[i] = addExact(offset[i], ceilDiv(multiplyExact(s - base, tileSize[i]), subsampling[i]));
                }
            }
            final int[] upper = this.tileUpper.clone();
            for (int i = Math.min(tileUpper.length, upper.length); --i >= 0;) {
                upper[i] = Math.max(lower[i], Math.min(upper[i], tileUpper[i]));
            }
            return new AOI(lower, upper, offset, offset.length);
        }

        /**
         * Returns the enclosing coverage.
         */
        final TiledGridCoverage getCoverage() {
            return TiledGridCoverage.this;
        }

        /**
         * Returns the current iterator position as an index in the array of tiles to be returned
         * by {@link #readTiles(AOI)}. The initial position is zero.
         * The position is incremented by 1 in each call to {@link #next()}.
         *
         * @return current iterator position in result array.
         */
        public final int getIndexInResultArray() {
            return indexInResultArray;
        }

        /**
         * Returns the cached tile for current iterator position.
         *
         * @return cached tile at current iterator position, or {@code null} if none.
         *
         * @see Snapshot#cache(Raster)
         */
        public Raster getCachedTile() {
            final Raster tile = rasters.get(createCacheKey(indexInTileVector));
            if (tile != null) {
                /*
                 * Found a tile, but the sample model may be different because band order may be different.
                 * In both cases, we need to make sure that the raster starts at the expected coordinates.
                 */
                final int x = getTileOrigin(X_DIMENSION);
                final int y = getTileOrigin(Y_DIMENSION);
                if (model.equals(tile.getSampleModel())) {
                    if (tile.getMinX() == x && tile.getMinY() == y) {
                        return tile;
                    }
                    return tile.createTranslatedChild(x, y);
                }
                /*
                 * If the sample model is not the same (e.g. different bands), it must at least have the same size.
                 * Having a sample model of different size would probably be a bug, but we check anyway for safety.
                 * Note that the tile size is not necessarily equals to the sample model size.
                 */
                final SampleModel sm = tile.getSampleModel();
                if (sm.getWidth() == model.getWidth() && sm.getHeight() == model.getHeight()) {
                    final int width  = tile.getWidth();     // May be smaller than sample model width.
                    final int height = tile.getHeight();    // Idem.
                    /*
                     * It is okay to have a different number of bands if the sample model is
                     * a view created by `SampleModel.createSubsetSampleModel(int[] bands)`.
                     * Bands can also be in a different order and still share the same buffer.
                     */
                    Raster r = Raster.createRaster(model, tile.getDataBuffer(), new Point(x, y));
                    if (r.getWidth() != width || r.getHeight() != height) {
                        r = r.createChild(x, y, width, height, x, y, null);
                    }
                    return r;
                }
            }
            return null;
        }

        /**
         * Returns the origin to assign to the tile at current iterator position.
         * Note that the subsampling should be a divisor of tile size,
         * otherwise a drift in pixel coordinates will appear.
         * There is two exceptions to this rule:
         *
         * <ul>
         *   <li>If image is untiled (i.e. there is only one tile),
         *       we allow to read a sub-region of the unique tile.</li>
         *   <li>If subsampling is larger than tile size.</li>
         * </ul>
         */
        final int getTileOrigin(final int dimension) {
            /*
             * We really need `ceilDiv(…)` below, not `floorDiv(…)`. It makes no difference in the usual
             * case where the subsampling is a divisor of the tile size (the numerator is initialized to
             * a multiple of the denominator, then incremented by another multiple of the denominator).
             * It makes no difference in the untiled case neither because the numerator is not incremented.
             * But if we are in the case where subsampling is larger than tile size, then we want rounding
             * to the next tile. The tile seems "too far", but it will either be discarded at a later step
             * (because of empty intersection with AOI) or compensated by the offset caused by subsampling.
             * At first the index values seem inconsistent, but after we discard the tiles where
             * `getRegionInsideTile(…)` returns `false` they become consistent.
             */
            return toIntExact(ceilDiv(tileOffsetFull[dimension], subsampling[dimension]));
        }

        /**
         * Moves the iterator position to next tile. This method should be invoked in a loop as below:
         *
         * {@preformat java
         *     do {
         *         // Process current tile.
         *     } while (domain.next());
         * }
         *
         * @return {@code true} on success, or {@code false} if the iteration is finished.
         */
        public boolean next() {
            if (++indexInResultArray >= tileCountInQuery) {
                return false;
            }
            /*
             * Iterates over all tiles in the region specified to this method by maintaining 4 indices:
             *
             *   - `indexInResultArray` is the index in the `rasters` array to be returned.
             *   - `indexInTileVector`  is the corresponding index in the `tileOffsets` vector.
             *   - `tmcInSubset[]`      contains the Tile Matrix Coordinates (TMC) relative to this `TiledGridCoverage`.
             *   - `tileOffsetFull[]`   contains the pixel coordinates relative to the user-specified AOI.
             *
             * We do not check for integer overflow in this method because if an overflow is possible,
             * then `ArithmeticException` should have occurred in `TiledGridCoverage` constructor.
             */
            for (int i=0; i<tmcInSubset.length; i++) {
                indexInTileVector += tileStrides[i];
                if (++tmcInSubset[i] < tileUpper[i]) {
                    tileOffsetFull[i] += tileSize[i];
                    break;
                }
                // Rewind to index for tileLower[i].
                indexInTileVector -= (tmcInSubset[i] - tileLower[i]) * tileStrides[i];
                tmcInSubset   [i]  = tileLower[i];
                tileOffsetFull[i]  = multiplyFull(offsetAOI[i], subsampling[i]);
            }
            return true;
        }
    }

    /**
     * Snapshot of a {@link AOI} iterator position. Those snapshots can be created during an iteration
     * for processing a tile later. For example a {@link #readTiles(AOI)} method implementation may want
     * to create a list of all tiles to load before to start the actual reading process in order to read
     * the tiles in some optimal order, or for combining multiple read operations in a single operation.
     */
    protected static class Snapshot {
        /**
         * The source coverage.
         */
        private final TiledGridCoverage coverage;

        /**
         * Tile Matrix Coordinates (TMC) relative to the enclosing {@link TiledGridCoverage}.
         * Tile (0,0) is the tile in the upper-left corner of this {@link TiledGridCoverage},
         * not necessarily the tile in the upper-left corner of the image in the resource.
         */
        private final int[] tmcInSubset;

        /**
         * Index of this tile in the array of tiles returned by {@link #readTiles(AOI)}.
         */
        public final int indexInResultArray;

        /**
         * Index of this tile in the {@link TiledGridResource}. In a GeoTIFF image, this is
         * the index of the tile in the {@code tileOffsets} and {@code tileByteCounts} vectors.
         * This index is also used as key in the {@link TiledGridCoverage#rasters} map.
         */
        public final int indexInTileVector;

        /**
         * Pixel coordinates of the upper-left corner of the tile.
         */
        public final int originX, originY;

        /**
         * Stores information about a tile to be loaded.
         *
         * @param iterator  the iterator for which to create a snapshot of its current position.
         */
        public Snapshot(final AOI iterator) {
            coverage           = iterator.getCoverage();
            tmcInSubset        = iterator.tmcInSubset.clone();
            indexInResultArray = iterator.indexInResultArray;
            indexInTileVector  = iterator.indexInTileVector;
            originX            = iterator.getTileOrigin(X_DIMENSION);
            originY            = iterator.getTileOrigin(Y_DIMENSION);
        }

        /**
         * Returns the coordinate of the pixel to read <em>inside</em> the tile, ignoring subsampling.
         * The tile upper-left corner is assumed (0,0). Consequently the lower coordinates are usually
         * (0,0) and the upper coordinates are usually the tile size, but those value may be different
         * if the enclosing {@link TiledGridCoverage} contains only one (potentially big) tile.
         * In that case, the reading process is more like untiled image reading.
         *
         * <p>The {@link TiledGridCoverage} subsampling is provided for convenience,
         * but is constant for all tiles regardless the subregion to read.
         * The same values can be obtained by {@link #getSubsampling(int)}.</p>
         *
         * @param  lower        a pre-allocated array where to store relative coordinates of the first pixel.
         * @param  upper        a pre-allocated array where to store relative coordinates after the last pixel.
         * @param  subsampling  a pre-allocated array where to store subsampling.
         * @param  dimension    number of elements to write in the {@code lower} and {@code upper} arrays.
         * @return {@code true} on success, or {@code false} if the tile is empty.
         */
        public boolean getRegionInsideTile(final long[] lower, final long[] upper, final int[] subsampling, int dimension) {
            System.arraycopy(coverage.subsampling, 0, subsampling, 0, dimension);
            while (--dimension >= 0) {
                final int  tileSize  = coverage.tileSize[dimension];
                final long tileIndex = addExact(coverage.tmcOfFirstTile[dimension], tmcInSubset[dimension]);
                final long tileBase  = multiplyExact(tileIndex, tileSize);
                /*
                 * The `offset` value is usually zero or negative because the tile to read should be inside the AOI,
                 * e.g. at the right of the AOI left border. It may be positive if the `TiledGridCoverage` contains
                 * only one (potentially big) tile, so the tile reading process become a reading of untiled data.
                 */
                long offset = subtractExact(coverage.readExtent.getLow(dimension), tileBase);
                long limit  = Math.min(addExact(offset, coverage.readExtent.getSize(dimension)), tileSize);
                if (offset < 0) {
                    /*
                     * Example: for `tileSize` = 10 pixels and `subsampling` = 3,
                     * the pixels to read are represented by black small squares below:
                     *
                     *  -10          0         10         20         30
                     *    ┼──────────╫──────────┼──────────┼──────────╫
                     *    │▪▫▫▪▫▫▪▫▫▪║▫▫▪▫▫▪▫▫▪▫│▫▪▫▫▪▫▫▪▫▫│▪▫▫▪▫▫▪▫▫▪║
                     *    ┼──────────╫──────────┼──────────┼──────────╫
                     *
                     * If reading the second tile, then `tileBase` = 10 and `offset` = -10.
                     * The first pixel to read in the second tile has a subsampling offset.
                     * We usually try to avoid this situation because it causes a variable
                     * number of white squares in tiles (4,3,3,4 in above example), except
                     * when there is only 1 tile to read in which case offset is tolerated.
                     */
                    final int s = coverage.subsampling[dimension];
                    offset %= s;
                    if (offset != 0) {
                        offset += s;
                    }
                }
                if (offset >= limit) {          // Test for intersection before we adjust the limit.
                    return false;
                }
                if (dimension == X_DIMENSION && coverage.forceTileSize) {
                    limit = tileSize;
                }
                lower[dimension] = offset;
                upper[dimension] = limit;
            }
            return true;
        }

        /**
         * Stores the given raster in the cache. If another raster existed previously in the cache,
         * the old raster will be reused if it has the same size and model, or discarded otherwise.
         * The latter case may happen if {@link AOI#getCachedTile()} determined that a cached raster
         * exists but can not be reused.
         *
         * @param  tile  the raster to cache.
         * @return the cached raster. Should be the given {@code raster} instance,
         *         but this method check for concurrent caching as a paranoiac check.
         *
         * @see AOI#getCachedTile()
         */
        public Raster cache(final Raster tile) {
            final TiledGridResource.CacheKey key = coverage.createCacheKey(indexInTileVector);
            Raster existing = coverage.rasters.put(key, tile);
            /*
             * If a tile already exists, verify if its layout is compatible with the given tile.
             * If yes, we assume that the two tiles have the same content. We do this check as a
             * safety but it should not happen if the caller synchronized the tile read actions.
             */
            if (existing != null
                    && existing.getSampleModel().equals(tile.getSampleModel())
                    && existing.getWidth()  == tile.getWidth()
                    && existing.getHeight() == tile.getHeight())
            {
                // Restore the existing tile in the cache, with its original position.
                if (coverage.rasters.replace(key, tile, existing)) {
                    final int x = tile.getMinX();
                    final int y = tile.getMinY();
                    if (existing.getMinX() != x || existing.getMinY() != y) {
                        existing = existing.createTranslatedChild(x, y);
                    }
                    return existing;
                }
            }
            return tile;
        }
    }

    /**
     * Creates the key to use for caching the tile at given index.
     */
    private TiledGridResource.CacheKey createCacheKey(final int indexInTileVector) {
        return new TiledGridResource.CacheKey(indexInTileVector, includedBands, subsampling, subsamplingOffsets);
    }

    /**
     * Returns all tiles in the given area of interest. Tile indices are relative to this {@code TiledGridCoverage}:
     * (0,0) is the tile in the upper-left corner of this {@code TiledGridCoverage} (not necessarily the upper-left
     * corner of the image in the {@link TiledGridResource}).
     *
     * The {@link Raster#getMinX()} and {@code getMinY()} coordinates of returned rasters
     * shall start at the given {@code iterator.offsetAOI} values.
     *
     * <p>This method must be thread-safe. It is implementer responsibility to ensure synchronization,
     * for example using {@link TiledGridResource#getSynchronizationLock()}.</p>
     *
     * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
     * @return tiles decoded from the {@link TiledGridResource}.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     * @throws RuntimeException if the Java2D image can not be created for another reason
     *         (too many exception types to list them all).
     */
    protected abstract Raster[] readTiles(AOI iterator) throws IOException, DataStoreException;
}
