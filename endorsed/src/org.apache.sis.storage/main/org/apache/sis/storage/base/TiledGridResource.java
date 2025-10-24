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
package org.apache.sis.storage.base;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.lang.reflect.Array;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.internal.shared.RangeArgument;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.collection.WeakValueHashMap;
import static org.apache.sis.storage.base.TiledGridCoverage.X_DIMENSION;
import static org.apache.sis.storage.base.TiledGridCoverage.Y_DIMENSION;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * Base class of grid coverage resource storing data in tiles.
 * The word "tile" is used for simplicity but can be understood
 * as "chunk" in a <var>n</var>-dimensional generalization.
 * Subclasses need to implement the following methods:
 *
 * <ul>
 *   <li>{@link #getGridGeometry()}</li>
 *   <li>{@link #getSampleDimensions()}</li>
 *   <li>{@link #getTileSize()}</li>
 *   <li>{@link #getSampleModel(int[])} (optional but recommended)</li>
 *   <li>{@link #getColorModel(int[])} (optional but recommended)</li>
 *   <li>{@link #read(GridGeometry, int...)}</li>
 * </ul>
 *
 * The read method can be implemented simply as below:
 *
 * {@snippet lang="java" :
 *     @Override
 *     public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
 *         synchronized (getSynchronizationLock()) {
 *             var subset = new Subset(domain, ranges);
 *             var result = new MySubclassOfTiledGridCoverage(this, subset);
 *             return preload(result);
 *         }
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class TiledGridResource extends AbstractGridCoverageResource {
    /**
     * A key in the {@link #rasters} cache of tiles.
     * Each key shall be unique within its enclosing {@link TiledGridResource} instance.
     */
    static final class CacheKey {
        /** Index in a row-major array of tiles. */ private final int    indexInTileVector;
        /** Bands in strictly increasing order.  */ private final int[]  includedBands;
        /** Subsampling factors at read time.    */ private final long[] subsampling;
        /** Remainder of subsampling divisions.  */ private final long[] subsamplingOffsets;

        /** Creates a key with given arrays hold be reference (no copy). */
        CacheKey(final int indexInTileVector, final int[] includedBands,
                 final long[] subsampling, final long[] subsamplingOffsets)
        {
            this.indexInTileVector  = indexInTileVector;
            this.includedBands      = includedBands;
            this.subsampling        = subsampling;
            this.subsamplingOffsets = subsamplingOffsets;
        }

        /** Returns a hash-code value for this key. */
        @Override public int hashCode() {
            return indexInTileVector
                    +   73 * Arrays.hashCode(includedBands)
                    + 1063 * Arrays.hashCode(subsampling)
                    + 7919 * Arrays.hashCode(subsamplingOffsets);
        }

        /** Compares this key with the given object for equality. */
        @Override public boolean equals(final Object obj) {
            if (obj instanceof CacheKey) {
                final CacheKey other = (CacheKey) obj;
                return indexInTileVector == other.indexInTileVector
                        && Arrays.equals(includedBands,      other.includedBands)
                        && Arrays.equals(subsampling,        other.subsampling)
                        && Arrays.equals(subsamplingOffsets, other.subsamplingOffsets);
            }
            return false;
        }
    }

    /**
     * All tiles loaded by any {@link TiledGridCoverage} created from this resource.
     * Keys contains tile indices in a row-major array of tiles.
     * For each value, the {@link Raster#getMinX()} and {@code minY} values
     * can be anything, depending which {@link TiledGridResource} was first to load the tile.
     *
     * @see TiledGridCoverage#rasters
     * @see TiledGridCoverage.AOI#getCachedTile()
     */
    private final WeakValueHashMap<CacheKey, Raster> rasters = new WeakValueHashMap<>(CacheKey.class);

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
     * @param  parent  the parent resource, or {@code null} if none.
     */
    protected TiledGridResource(final Resource parent) {
        super(parent);
    }

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions,
     * which must be {@value TiledGridCoverage#BIDIMENSIONAL} or more.
     *
     * @return the size of tiles (in pixels) in this resource.
     * @throws DataStoreException if an error occurred while fetching the tile size.
     */
    protected abstract int[] getTileSize() throws DataStoreException;

    /**
     * Returns the tile size to use for a read operation using the given subsampling.
     * The default implementation returns the real tile size, as returned by {@link #getTileSize()}.
     * Subclasses may override if it is easy for them to read many tiles as if they were a single tile.
     * Such coalescing can be useful for avoiding that a read operation produces tiles that become too
     * small after the subsampling.
     *
     * <p>Note that {@link TiledGridCoverage} aligns its {@link GridExtent} on the boundaries of "real" tiles
     * (i.e. on tiles of the size returned by {@link #getTileSize()}), not on the boundaries of virtual tiles.
     * Therefore, subclasses should override this method only if they are prepared to read regions covering a
     * fraction of the virtual tiles. A simple and efficient strategy is to simply multiply {@code tileSize[i]}
     * by {@code subsampling[i]} for each dimension <var>i</var> so that each virtual tile contains only whole
     * real tiles.
     *
     * @param  subsampling  the subsampling which will be applied in a read operation.
     * @return the size of tiles (in pixels) in this resource.
     * @throws DataStoreException if an error occurred while fetching the tile size.
     */
    protected long[] getVirtualTileSize(long[] subsampling) throws DataStoreException {
        return ArraysExt.copyAsLongs(getTileSize());
    }

    /**
     * Returns the number of sample values in an indivisible element of a tile.
     * An element is a primitive type such as {@code byte}, {@code int} or {@code float}.
     * This value is usually 1 when each sample value is stored in a separated element.
     * However, in multi-pixels packed sample model (e.g. bilevel image with 8 pixels per byte),
     * it is difficult to start reading an image at <var>x</var> location other than a byte boundary.
     * By declaring an "atom" size of 8 sample values in dimension 0 (<var>x</var>), the {@link Subset}
     * constructor will ensure that the sub-region to read starts at a byte boundary when reading a bilevel image.
     *
     * <p>The default implementation returns the {@linkplain TiledGridCoverage#getPixelsPerElement()
     * number of pixels per data element} for dimension 0 and returns 1 for all other dimensions.</p>
     *
     * @param  dim  the dimension: 0 for <var>x</var>, 1 for <var>y</var>, <i>etc.</i>
     * @return indivisible number of sample values to read in the specified dimension. Must be ≥ 1.
     *         This is in units of sample values (may be bits, bytes, floats, <i>etc</i>).
     * @throws DataStoreException if an error occurred while fetching the sample model.
     */
    protected int getAtomSize(final int dim) throws DataStoreException {
        return (dim == 0) ? TiledGridCoverage.getPixelsPerElement(getSampleModel(null)) : 1;
    }

    /**
     * Returns {@code true} if the reader can load truncated tiles. Truncated tiles may happen in the
     * last row and last column of a tile matrix when the image size is not a multiple of the tile size.
     * Some file formats, such as GeoTIFF, unconditionally store full tiles, in which case this method
     * should return {@code false}. At the opposite, some implementations, such as <abbr>GDAL</abbr>,
     * accept only requests over the valid area, in which case this method should return {@code true}.
     *
     * <h4>Suggested value</h4>
     * The {@code suggested} argument is a value computed by the caller based on common usages.
     * The default implementation of {@link TiledGridCoverage} suggests {@code true}
     * if the read operation is inside a single big tile, or {@code false} otherwise.
     * The default implementation of this method returns {@code suggested} unchanged.
     *
     * <p>Note that even for subclasses that generally do not support the reading of truncated tiles,
     * it is often safe to return {@code true} for the last dimension (for example, <var>y</var> in a
     * two-dimensional image), because there is no data after that dimension.
     * Therefore, it is often safe to stop the reading process in that dimension.
     *
     * @param  dim        the dimension: 0 for <var>x</var>, 1 for <var>y</var>, <i>etc.</i>
     * @param  suggested  suggested response to return (see above heuristic rules).
     * @return whether the reader can load truncated tiles along the specified dimension.
     */
    protected boolean canReadTruncatedTiles(int dim, boolean suggested) {
        return suggested;
    }

    /**
     * Returns {@code true} if the reader can load only the requested bands and skip the other bands,
     * or {@code false} if the reader must load all bands. This value controls the amount of data to
     * be loaded by {@link #read(GridGeometry, int...)}:
     *
     * <ul class="verbose">
     *   <li>If {@code false}, then {@link TiledGridCoverage#model} will expect the same {@link DataBuffer}
     *       than the one expected by the {@linkplain #getSampleModel(int[]) sample model of this resource}.
     *       All bands will be loaded but the coverage sample model will ignore the bands that were not
     *       enumerated in the {@code range} argument. This strategy is convenient when skipping bands
     *       at reading time is difficult.</li>
     *   <li>If {@code true}, then {@link TiledGridCoverage#model} will have its band indices and bit masks
     *       "compressed" for making them consecutive. For example if the {@code range} argument specifies that
     *       the bands to read are {1, 3, 4, 6}, then after "compression" band indices become {0, 1, 2, 3}.
     *       This strategy is efficient when the reader is capable to skip bands at reading time.</li>
     * </ul>
     *
     * <p>The default implementation returns {@code true} if the sample model is a {@link ComponentSampleModel}
     * and {@code false} in all other cases, because skipping bands in a packed sample model is more difficult
     * to implement.</p>
     *
     * @return {@code true} if the reader can load only the requested bands and skip other bands, or
     *         {@code false} if the reader needs to load all bands regardless the {@code range} subset.
     * @throws DataStoreException if an error occurred while fetching the sample model.
     *
     * @see RangeArgument#select(SampleModel, boolean)
     */
    protected boolean canSeparateBands() throws DataStoreException {
        return getSampleModel(null) instanceof ComponentSampleModel;
    }

    /**
     * Returns the Java2D sample model describing pixel type and layout for the specified bands.
     * The raster size shall be the two first dimensions of the {@linkplain #getTileSize() tile size}.
     * This is the size of tiles as stored in the resource, i.e. ignoring sub-sampling.
     *
     * <p>If the {@code bands} argument is {@code null}, then this method <em>shall</em> return the sample model
     * for all bands and cannot return {@code null}. If the {@code bands} argument is non-null, then this method
     * <em>may</em> compute the sample model for the specified bands, or return {@code null} for instructing the
     * caller to derive the sample model from {@code getSampleModel(null)}.</p>
     *
     * <h4>Implementation note</h4>
     * The default implementation creates a sample model compatible with the {@linkplain #getColorModel(int[])
     * color model} if available, or otherwise defaults to a {@link BandedSampleModel} which stores samples as
     * floating point values. This is okay for prototyping, but it is recommended to override this method with
     * a simple model that better matches the data. A simple implementation can be as below:
     *
     * {@snippet lang="java" :
     *     private SampleModel sampleModel;
     *
     *     @Override
     *     protected SampleModel getSampleModel(int[] bands) throws DataStoreException {
     *         if (bands != null) {
     *             return null;
     *         }
     *         synchronized (getSynchronizationLock()) {
     *             if (sampleModel == null) {
     *                 sampleModel = ...;   // Compute and cache, because requested many times.
     *             }
     *             return sampleModel;
     *         }
     *     }
     *     }
     *
     * @param  bands  indices (not necessarily in increasing order) of desired bands, or {@code null} for all bands.
     * @return the sample model for tiles at full resolution with the specified bands.
     *         Shall be non-null if {@code bands} is null (i.e. the caller is requesting the main sample model).
     * @throws DataStoreException if an error occurred during sample model construction.
     */
    protected SampleModel getSampleModel(final int[] bands) throws DataStoreException {
        final int[] tileSize = getTileSize();
        final int width  = tileSize[X_DIMENSION];
        final int height = tileSize[Y_DIMENSION];
        final ColorModel colors = getColorModel(bands);
        if (colors != null) {
            return colors.createCompatibleSampleModel(width, height);
        }
        int numBands = (bands != null) ? bands.length : getSampleDimensions().size();
        return new BandedSampleModel(DataBuffer.TYPE_FLOAT, width, height, numBands);
    }

    /**
     * Returns the Java2D color model for rendering images, or {@code null} if none.
     * The color model shall be compatible with the sample model returned by {@link #getSampleModel(int[])}.
     *
     * <p>The default implementation returns a gray scale color model if there is only one band
     * to show and the range of the {@linkplain #getSampleDimensions() sample dimension} is known.
     * OTherwise, this method returns {@code null}.
     * This is okay for prototyping, but it is recommended to override.</p>
     *
     * @param  bands  indices (not necessarily in increasing order) of desired bands, or {@code null} for all bands.
     * @return a color model compatible with {@link #getSampleModel(int[])}, or {@code null} if none.
     * @throws DataStoreException if an error occurred during color model construction.
     */
    protected ColorModel getColorModel(final int[] bands) throws DataStoreException {
        final List<SampleDimension> sd = getSampleDimensions();
        if ((bands != null && bands.length == 1) || sd.size() == 1) {
            NumberRange<?> range = sd.get((bands != null) ? bands[0] : 0).getSampleRange().orElse(null);
            if (range != null) {
                final double min = range.getMinDouble();
                final double max = range.getMaxDouble();
                if (Double.isFinite(min) && Double.isFinite(max)) {
                    return ColorModelFactory.createGrayScale(DataBuffer.TYPE_FLOAT, 1, 0, min, max);
                }
            }
        }
        return null;
    }

    /**
     * Returns the values to use for filling empty spaces in rasters, with one value per band.
     * The returned array can be {@code null} if there is no fill value, or if the fill values
     * are not different than zero, or are not valid for the image data type.
     *
     * <p>Fill values are used when a tile contains less pixels than expected.
     * A null array is a shortcut for skipping the filling of new tiles,
     * because new tiles are already initialized with zero values by default.</p>
     *
     * <p>The default implementation returns an array of {@link DataType#fillValue()} except
     * for the {@linkplain IndexColorModel#getTransparentPixel() transparent pixel} if any.
     * If the array would contain only zero values, the default implementation returns null.</p>
     *
     * @param  bands  indices (not necessarily in increasing order) of desired bands, or {@code null} for all bands.
     * @return the value to use for filling empty spaces in each band, or {@code null} for defaulting to zero.
     * @throws DataStoreException if an error occurred while fetching filling information.
     */
    protected Number[] getFillValues(final int[] bands) throws DataStoreException {
        final SampleModel model = getSampleModel(bands);
        final var dataType = DataType.forBands(model);
        IndexColorModel icm = null;
check:  if (dataType.isInteger()) {
            final ColorModel colors = getColorModel(bands);
            if (colors instanceof IndexColorModel) {
                icm = (IndexColorModel) colors;
                if (icm.getTransparentPixel() > 0) {
                    break check;
                }
            }
            return null;
        }
        final Number fill = dataType.fillValue();
        final var fillValues = (Number[]) Array.newInstance(fill.getClass(), model.getNumBands());
        Arrays.fill(fillValues, fill);
        if (icm != null) {
            fillValues[ImageUtilities.getVisibleBand(icm)] = icm.getTransparentPixel();
        }
        return fillValues;
    }

    /**
     * Parameters that describe the resource subset to be accepted by the {@link TiledGridCoverage} constructor.
     * Instances of this class are temporary and used only for transferring information from {@link TiledGridResource}
     * to {@link TiledGridCoverage}. This class does not perform I/O operations.
     */
    public final class Subset {
        /**
         * The full size of the coverage in the enclosing {@link TiledGridResource}.
         * This is taken from {@link #getGridGeometry()} and does not take sub-sampling in account.
         */
        final GridExtent sourceExtent;

        /**
         * The area to read in unit of the full coverage (without subsampling).
         * This is the intersection between user-specified domain and enclosing
         * {@link TiledGridResource} domain, expanded to an integer number of chunks.
         * A chunk size is usually a tile size, but not necessarily as there is other
         * criteria to take in account such as "atom" size and subsampling.
         */
        final GridExtent readExtent;

        /**
         * The sub-region extent, CRS and conversion from cell indices to CRS.
         * This is the domain of the grid coverage to create.
         */
        final GridGeometry domain;

        /**
         * Sample dimensions for each image band. This is the range of the grid coverage to create.
         * If {@link #includedBands} is non-null, then the size of this list should be equal to
         * {@link #includedBands} array length. However, bands are not necessarily in the same order:
         * the order of bands in this {@code ranges} list is the order specified by user, while the
         * order of bands in {@link #includedBands} is always increasing index order for efficiency
         * reasons.
         */
        final List<? extends SampleDimension> ranges;

        /**
         * Indices of {@link TiledGridResource} bands which have been retained for inclusion
         * in the {@link TiledGridCoverage} to construct, in strictly increasing order.
         * An "included" band is stored in memory but not necessarily visible to the user,
         * because the {@link SampleModel} can be configured for ignoring some bands.
         * This array is {@code null} if all bands shall be included.
         *
         * <p>If the user specified bands out of order, the change of band order is taken in
         * account by the {@link #modelForBandSubset}. This {@code includedBands} array does
         * not apply any change of order for making sequential readings easier.</p>
         *
         * @see TiledGridCoverage#includedBands
         */
        final int[] includedBands;

        /**
         * Coordinate conversion from subsampled grid to the grid at full resolution.
         * This array contains the factors by which to divide {@link TiledGridResource}
         * cell coordinates in order to obtain {@link TiledGridCoverage} cell coordinates.
         */
        final long[] subsampling;

        /**
         * Remainder of the divisions of {@link TiledGridResource} cell coordinates by subsampling factors.
         */
        final long[] subsamplingOffsets;

        /**
         * Size of tiles (or chunks) in the resource, without sub-sampling.
         * May be a virtual tile size (i.e., tiles larger than the tiles in the file)
         * if the resource can easily coalesce many tiles in a single read operation.
         * Conversely, it may also be smaller than the real tile size if the subset
         * is effectively untiled (the requested region covers a single tile).
         *
         * @see #getVirtualTileSize(long[])
         * @see TiledGridCoverage#virtualTileSize
         */
        final long[] virtualTileSize;

        /**
         * The sample model for the bands to read (not the full set of bands in the resource).
         * The width is {@code tileSize[X_DIMENSION]} and the height it {@code tileSize[Y_DIMENSION]},
         * i.e. subsampling is <strong>not</strong> applied.
         */
        final SampleModel modelForBandSubset;

        /**
         * The color model for the bands to read (not the full set of bands in the resource).
         * May be {@code null} if the color model could not be created.
         */
        final ColorModel colorsForBandSubset;

        /**
         * Values to use for filling empty spaces in rasters, or {@code null} if none,
         * not different than zero or not valid for the target data type.
         */
        final Number[] fillValues;

        /**
         * Cache to use for tiles loaded by the {@link TiledGridCoverage}.
         * It is a reference to {@link TiledGridResource#rasters} if shareable.
         */
        final WeakValueHashMap<CacheKey, Raster> cache;

        /**
         * Creates parameters for the given domain and range.
         *
         * @param  domain  the domain argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @param  range   the range argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         *
         * @throws ArithmeticException if pixel indices exceed 64 bits integer capacity.
         * @throws DataStoreException if a call to {@link TiledGridResource} method failed.
         * @throws RasterFormatException if the sample model is not recognized.
         * @throws IllegalArgumentException if an error occurred in an operation
         *         such as creating the {@code SampleModel} subset for selected bands.
         */
        public Subset(GridGeometry domain, final int[] range) throws DataStoreException {
            // Validate argument first, before more expensive computations.
            List<SampleDimension> bands = getSampleDimensions();
            final RangeArgument rangeIndices = RangeArgument.validate(bands.size(), range, listeners);
            /*
             * Normally, the number of dimensions of `tileSize` should be equal to the number of dimensions
             * of the grid geometry (determined by its `GridExtent`). However, we are tolerant to situation
             * where the `TiledGridResource` is a two dimensional image associated to a 3-dimensional CRS.
             * This is not recommended, but can happen with GeoTIFF for example. What to do with the extra
             * dimension is unclear (the GeoTIFF specification itself said nothing), so we just ignore it.
             */
            final int[] tileSize = getTileSize();
            final int dimension = tileSize.length;          // May be shorter than the grid geometry dimension.
            GridGeometry gridGeometry = getGridGeometry();
            if ((domain == null || domain.getDimension() == dimension) && gridGeometry.getDimension() > dimension) {
                gridGeometry = gridGeometry.selectDimensions(ArraysExt.range(0, dimension));
            }
            sourceExtent = gridGeometry.getExtent();
            boolean sharedCache = true;
            if (domain == null) {
                domain             = gridGeometry;
                readExtent         = sourceExtent;
                subsamplingOffsets = new long[dimension];
                subsampling        = new long[dimension];
                Arrays.fill(subsampling, 1);
            } else {
                /*
                 * If an area of interest has been specified, we may need to expand it to an integer number of tiles.
                 * But we do not need to do that if the image is untiled; it is okay to read only a sub-region of the
                 * single tile. We disable the "integer number of tiles" restriction by setting the tile size to 1.
                 * Note that it is possible to disable this restriction in a single dimension, typically the X one
                 * when reading a TIFF image using strips instead of tiles.
                 */
                final var chunkSize = new int [dimension];
                final var maxSubsmp = new long[dimension];
                for (int i=0; i < dimension; i++) {
                    final int atomSize = getAtomSize(i);
                    int span = tileSize[i];
                    if (span >= sourceExtent.getSize(i)) {
                        span = atomSize;
                        sharedCache = false;
                    }
                    /*
                     * We put no restriction on the maximum subsampling if subsamplig can occur anywhere
                     * ("atome size" of 1) and disable subsampling otherwise for avoiding code complexity.
                     */
                    maxSubsmp[i] = (atomSize == 1) ? Long.MAX_VALUE : 1;
                    chunkSize[i] = (i == X_DIMENSION || i == Y_DIMENSION) ? span : 1;
                }
                /*
                 * Build the domain in units of subsampled pixels, and get the same extent (`readExtent`)
                 * without subsampling, i.e. in units of cells of the original grid resource.
                 */
                final GridDerivation target = gridGeometry.derive()
                            .pointsToInclude(PixelInCell.CELL_CENTER)       // For tight bounding box.
                            .chunkSize(chunkSize)
                            .maximumSubsampling(maxSubsmp)
                            .rounding(GridRoundingMode.ENCLOSING)
                            .subgrid(domain);
                /*
                 * Post-condition: we could have put `assert gridGeometry.contains(domain)` below,
                 * if it wasn't for the `chunkSize` argument which can make the result larger.
                 */
                domain     = target.build();
                readExtent = target.getIntersection();
                /*
                 * The grid extent may have more dimensions than the tile size because of cases such as GeoTIFF,
                 * which may declare a three-dimensional CRS despite the image being two-dimensional. We need to
                 * force an array length consistent with the length used in the above `(domain == null)` case,
                 * because those arrays as used in `TileGridCoverage.createCacheKey(…)`. Inconsistent lengths
                 * would cause the reader to not detect that a tile is available in the cache.
                 */
                subsampling        = ArraysExt.resize(target.getSubsampling(), dimension);
                subsamplingOffsets = ArraysExt.resize(target.getSubsamplingOffsets(), dimension);
            }
            /*
             * Virtual tile size is usually the same as the tile size encoded in the binary file.
             * The virtual size may be larger if the subclass override `getVirtualTileSize(…)`.
             * The loop below is where the virtual tile size may be made smaller.
             */
            virtualTileSize = getVirtualTileSize(subsampling);
            for (int i=0; i < virtualTileSize.length; i++) {
                virtualTileSize[i] = Math.min(sourceExtent.getSize(i), Math.max(tileSize[i], virtualTileSize[i]));
            }
            /*
             * Get the bands selected by user in strictly increasing order of source band index.
             * If user has specified bands in a different order, that change of band order will
             * be handled by the `SampleModel`, not by the `includedBands` array.
             */
            int[] requestedBands = null;          // Same as `includedBands` but in user-specified order.
            @SuppressWarnings("LocalVariableHidesMemberVariable") int[]       includedBands       = null;
            @SuppressWarnings("LocalVariableHidesMemberVariable") SampleModel modelForBandSubset  = null;
            @SuppressWarnings("LocalVariableHidesMemberVariable") ColorModel  colorsForBandSubset = null;
            boolean loadAllBands = rangeIndices.isIdentity();
            if (!loadAllBands) {
                bands = Arrays.asList(rangeIndices.select(bands));
                loadAllBands = !canSeparateBands();
                if (!loadAllBands) {
                    sharedCache = false;
                    if (!rangeIndices.hasAllBands) {
                        includedBands = new int[rangeIndices.getNumBands()];
                        for (int i=0; i<includedBands.length; i++) {
                            includedBands[i] = rangeIndices.getSourceIndex(i);
                        }
                        assert ArraysExt.isSorted(includedBands, true);
                    }
                    requestedBands = rangeIndices.getSelectedBands();
                    modelForBandSubset   = getSampleModel(requestedBands);
                    colorsForBandSubset  = getColorModel (requestedBands);
                }
            }
            if (modelForBandSubset == null) {
                modelForBandSubset = rangeIndices.select(getSampleModel(null), loadAllBands);
            }
            if (colorsForBandSubset == null) {
                colorsForBandSubset = rangeIndices.select(getColorModel(null));
            }
            this.domain              = domain;
            this.ranges              = bands;
            this.includedBands       = includedBands;
            this.modelForBandSubset  = Objects.requireNonNull(modelForBandSubset);
            this.colorsForBandSubset = colorsForBandSubset;
            this.fillValues          = getFillValues(requestedBands);
            /*
             * All `TiledGridCoverage` instances can share the same cache if they read all tiles fully.
             * If they read only sub-regions or apply subsampling, then they will need their own cache.
             */
            cache = sharedCache ? rasters : new WeakValueHashMap<>(CacheKey.class);
        }

        /**
         * Returns flags telling, for each dimension, whether the read region should be an integer number of tiles.
         * By default (when {@link #canReadTruncatedTiles(int, boolean)} is not overridden), the flags are set for
         * all dimensions except the ones where the region to read is smaller than the tile size. The latter case
         * happens when reading an effectively untiled coverage (when the requested region is inside a single tile).
         *
         * @param  subSize  tile size after subsampling.
         * @return a bitmask with the flag for the first dimension in the lowest bit.
         *
         * @see TiledGridCoverage#forceWholeTiles
         */
        final long forceWholeTiles(final int[] subSize) {
            long forceWholeTiles = 0;
            for (int i=0; i<subSize.length; i++) {
                if (!canReadTruncatedTiles(i, Math.multiplyExact(subsampling[i], subSize[i]) < virtualTileSize[i])) {
                    forceWholeTiles |= Numerics.bitmask(i);
                }
            }
            return forceWholeTiles;
        }

        /**
         * Returns {@code true} if reading data in this subset will read contiguous values on the <var>x</var> axis.
         * This method returns {@code true} if all following conditions are met:
         *
         * <ul>
         *   <li>All bands will be read (ignoring change of band order
         *       because this change is handled by the sample model).</li>
         *   <li>There is no subsampling on the <var>x</var> axis.</li>
         * </ul>
         *
         * Note that the first criterion can often be relaxed when the sample model is an instance
         * of {@link BandedSampleModel}. This method does not check the sample model type.
         *
         * @return whether the values to read on a row are contiguous.
         */
        public boolean isXContiguous() {
            return includedBands == null && subsampling[X_DIMENSION] == 1;
        }

        /**
         * Whether the reading of tiles is deferred until {@link RenderedImage#getTile(int, int)} is invoked.
         * This is true if the user explicitly {@linkplain #setLoadingStrategy requested such deferred loading
         * strategy} and this method considers that it is worth to do so.
         */
        final boolean deferredTileReading() {
            if (loadingStrategy != RasterLoadingStrategy.AT_GET_TILE_TIME) {
                return false;
            }
            for (int i = virtualTileSize.length; --i >= 0;) {
                if (subsampling[i] >= virtualTileSize[i]) {
                    return false;
                }
            }
            return true;
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
        if (loadingStrategy == null || loadingStrategy == RasterLoadingStrategy.AT_READ_TIME) {
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
     * Current implementation does not support immediate loading if the data cube has more than 2 dimensions.
     * Non-immediate loading allows users to specify two-dimensional slices.
     */
    private boolean supportImmediateLoading() throws DataStoreException {
        return getTileSize().length == TiledGridCoverage.BIDIMENSIONAL;
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     */
    @Override
    public final RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
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
     * @throws DataStoreException if an error occurred while setting data store configuration.
     */
    @Override
    public final boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (strategy == RasterLoadingStrategy.AT_GET_TILE_TIME) {
                loadingStrategy = strategy;
            } else if (strategy != null) {
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
