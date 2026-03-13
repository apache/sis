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
package org.apache.sis.storage.tiling;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collection;
import java.util.Spliterator;
import java.util.OptionalInt;
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
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
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
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.ListOfUnknownSize;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.iso.DefaultNameFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * Base class of grid coverage resources that store data in tiles.
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
 *   <li>{@link #read(Subset)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 * @since   1.7
 */
public abstract class TiledGridCoverageResource extends AbstractGridCoverageResource implements TiledResource {
    /**
     * Number of dimensions in a two-dimensional slice of data represented as a rendered image.
     * This constant can be used for making easier to identify codes where a two-dimensional slice is assumed.
     */
    protected static final int BIDIMENSIONAL = 2;

    /**
     * A key in the {@link #rasters} cache of tiles.
     * Each key shall be unique within its enclosing {@link TiledGridCoverageResource} instance.
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
     * can be anything, depending which {@link TiledGridCoverageResource} was first to load the tile.
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
     * The tile matrix sets, created when first requested.
     *
     * @see #getTileMatrixSets()
     */
    private Collection<TileMatrixSet> tileMatrixSets;

    /**
     * Zero-based index of the pyramid level of this grid coverage resource.
     * This is not used directly by this class, but this information is stored
     * for providing it to {@link TileReadEvent.Context#pyramidLevel}.
     *
     * @see TileReadEvent#getPyramidLevel()
     */
    private int pyramidLevel;

    /**
     * The dimension of the grid which is mapped to the <var>x</var> axis (column indexes) in rendered images.
     * This value is used, directly or indirectly, at {@link Subset} creation time. The default value is 0.
     */
    private int xDimension;

    /**
     * The dimension of the grid which is mapped to the <var>y</var> axis (row indexes) in rendered images.
     * This value is used, directly or indirectly, at {@link Subset} creation time. The default value is 1.
     */
    private int yDimension;

    /**
     * Creates a new resource.
     *
     * @param  parent  the parent resource, or {@code null} if none.
     */
    protected TiledGridCoverageResource(final Resource parent) {
        super(parent);
        yDimension = 1;
    }

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions,
     * which must be {@value #BIDIMENSIONAL} or more.
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
        final int width  = tileSize[xDimension];
        final int height = tileSize[yDimension];
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
     * Returns the preferred resolutions (in units of <abbr>CRS</abbr> axes) for read operations in this data store.
     * The list elements are ordered from finest (smallest numerical values) to coarsest (largest numerical values).
     *
     * <p>The default implementation uses information in the first element returned by {@link #getPyramids()}.
     * It is generally easier for subclasses to override {@link #getPyramids()} instead of this method.</p>
     *
     * <p>This returned list may defer the calculations of resolutions until first requested.
     * If a {@link DataStoreException} occurs during the invocation of a {@link List} method,
     * the exception will be wrapped in a {@link BackingStoreException}.</p>
     *
     * @return resolutions at all levels in the default pyramid.
     * @throws DataStoreException if an error occurred while fetching the resolution.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<double[]> getResolutions() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            final Pyramid pyramid = Containers.peekFirst(getPyramids());
            if (pyramid == null) {
                return super.getResolutions();
            }
            return new ListOfUnknownSize<double[]>() {
                /** Returns characteristics of this collection as a combination of {@code Spliterator} bits. */
                @Override protected int characteristics() {
                    return super.characteristics() | Spliterator.NONNULL;
                }

                /** Returns the {@link #size()} value if it is already known, or empty if the size is still unknown. */
                @Override protected OptionalInt sizeIfKnown() {
                    return pyramid.numberOfLevels();
                }

                /** Returns {@code true} if the given index is valid for this list. */
                @Override protected boolean isValidIndex(final int level) {
                    try {
                        return pyramid.forPyramidLevel(level) != null;
                    } catch (DataStoreException e) {
                        throw new BackingStoreException(e);
                    }
                }

                /** Returns the element at the specified index. */
                @Override public double[] get(final int level) {
                    try {
                        TiledGridCoverageResource c = pyramid.forPyramidLevel(level);
                        if (c != null) return c.getGridGeometry().getResolution(false);
                    } catch (DataStoreException e) {
                        throw new BackingStoreException(e);
                    }
                    throw new IndexOutOfBoundsException(level);
                }
            };
        }
    }

    /**
     * Returns the collection of all available tile matrix sets in this resource.
     * The returned collection typically contains exactly one instance,
     * which describes a pyramid in the same <abbr>CRS</abbr> as this Grid Coverage Resource.
     *
     * <p>The default implementation uses the information provided by {@link #getPyramids()}
     * for creating default {@link TileMatrixSet} instances.
     * It is generally easier for subclasses to override {@link #getPyramids()} instead of this method.</p>
     *
     * @return all available {@link TileMatrixSet} instances, or an empty collection if none.
     * @throws DataStoreException if an error occurred while fetching the tile matrix sets.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // The collection is unmodifiable.
    public Collection<? extends TileMatrixSet> getTileMatrixSets() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (tileMatrixSets == null) {
                final List<Pyramid> pyramids = getPyramids();
                final var sets = new TileMatrixSet[pyramids.size()];
                if (sets.length != 0) {     // For avoiding an index out of bounds in call to `get(0)`.
                    final GenericName scope = getIdentifier().orElseGet(
                                () -> pyramids.get(0).nameFactory().createLocalName(null, listeners.getSourceName()));
                    final var processor = new GridCoverageProcessor();
                    for (int i=0; i<sets.length; i++) {
                        sets[i] = new ImagePyramid(scope, pyramids.get(i), processor, listeners.getLocale());
                    }
                }
                tileMatrixSets = List.of(sets);
            }
            return tileMatrixSets;
        }
    }

    /**
     * Parameters that describe the resource subset to be accepted by the {@link TiledGridCoverage} constructor.
     * Instances of this class are temporary and used only for transferring information from {@link TiledGridCoverageResource}
     * to {@link TiledGridCoverage}. This class does not perform I/O operations.
     */
    public final class Subset {
        /**
         * The full size of the coverage in the enclosing {@link TiledGridCoverageResource}.
         * This is taken from {@link #getGridGeometry()} and does not take sub-sampling in account.
         */
        final GridExtent sourceExtent;

        /**
         * The area to read in unit of the full coverage (without subsampling).
         * This is the intersection between user-specified domain and enclosing
         * {@link TiledGridCoverageResource} domain, expanded to an integer number of chunks.
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
         * Indices of {@link TiledGridCoverageResource} bands which have been retained for inclusion
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
         * This array contains the factors by which to divide {@link TiledGridCoverageResource}
         * cell coordinates in order to obtain {@link TiledGridCoverage} cell coordinates.
         */
        final long[] subsampling;

        /**
         * Remainder of the divisions of {@link TiledGridCoverageResource} cell coordinates by subsampling factors.
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
         * The width is {@code tileSize[xDimension]} and the height it {@code tileSize[yDimension]},
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
         * It is a reference to {@link TiledGridCoverageResource#rasters} if shareable.
         */
        final WeakValueHashMap<CacheKey, Raster> cache;

        /**
         * The listeners of the resource at level 0.
         */
        StoreListeners listenersOfLevel0;

        /**
         * Creates parameters for the given domain and range.
         *
         * @param  domain  the domain argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         * @param  range   the range argument specified by user in a call to {@code GridCoverageResource.read(…)}.
         *
         * @throws ArithmeticException if pixel indices exceed 64 bits integer capacity.
         * @throws DataStoreException if a call to {@link TiledGridCoverageResource} method failed.
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
             * where the `TiledGridCoverageResource` is a two dimensional image associated to a 3-dimensional CRS.
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
                    chunkSize[i] = (i == xDimension() || i == yDimension()) ? span : 1;
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
            listenersOfLevel0 = listeners;
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
            return includedBands == null && subsampling[xDimension()] == 1;
        }

        /**
         * Returns dimension of the grid which is mapped to the <var>x</var> axis (column indexes) in rendered images.
         * This is usually 0.
         */
        final int xDimension() {
            return xDimension;
        }

        /**
         * Returns dimension of the grid which is mapped to the <var>y</var> axis (row indexes) in rendered images.
         * This is usually 1.
         */
        final int yDimension() {
            return yDimension;
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

        /**
         * Returns the zero-based index of the pyramid level of this grid coverage resource.
         *
         * @see TileReadEvent#getPyramidLevel()
         */
        final int pyramidLevel() {
            return pyramidLevel;
        }
    }

    /**
     * Creates a coverage which will read the specified subset from this resource when first requested.
     * This method is invoked by the default implementation of {@link #read(GridGeometry, int...)}.
     * This method creates a subclass of {@link TiledGridCoverage} which will read tiles later, when first requested.
     * The implementation of this method does not need to care about synchronization, immediate (rather than deferred)
     * loading of tiles, logging of loading time and handling of {@link RuntimeException}.
     * Those tasks should be handled by the caller.
     *
     * @param  subset  desired grid extent, resolution and sample dimensions to read.
     * @return the grid coverage for the specified domain, resolution and ranges.
     * @throws DataStoreException if the coverage cannot be created.
     * @throws RuntimeException if the coverage cannot be created for a reason not handled as a data store exception.
     *
     * @see TiledGridCoverage#TiledGridCoverage(Subset)
     */
    protected abstract TiledGridCoverage read(Subset subset) throws DataStoreException;

    /**
     * Loads a subset of the grid coverage represented by this resource.
     * While this method name suggests an immediate reading, the actual reading may be deferred.
     * This method performs the following steps:
     *
     * <ol>
     *   <li>Selects a {@code TiledGridCoverageResource} instance for the pyramid level
     *       considered the best fit for the resolution of the specified {@code domain}.
     *       The selected instance may be {@code this}.</li>
     *   <li>Invokes the {@link #read(Subset)} method on that selected instance inside a block
     *       synchronized on the {@linkplain #getSynchronizationLock() synchronization lock}.</li>
     *   <li>If the {@linkplain #getLoadingStrategy() current loading strategy} is
     *       {@link RasterLoadingStrategy#AT_READ_TIME}, forces the immediate reading of tiles
     *       and logs the time required for this operation.</li>
     * </ol>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        TiledGridCoverageResource bestFit;
        synchronized (getSynchronizationLock()) {
            /*
             * Select the pyramid which fits bet the request (taking in account, for example, the CRS),
             * then select the highest pyramid level (overview) with a resolution equal or better than
             * the requested resolution.
             */
            final Pyramid pyramid = choosePyramid(domain, ranges);
            if (pyramid == null || (bestFit = pyramid.forPyramidLevel(0)) == null) {
                return readAtThisPyramidLevel(domain, ranges, null);
            }
            int level = 0;
            final double[] request = bestFit.convertResolutionOf(domain);
            if (request != null) {
                TiledGridCoverageResource c;
                while ((c = pyramid.forPyramidLevel(level)) != null) {
                    final double[] resolution = c.getGridGeometry().getResolution(true);
                    if (!(request[xDimension] >= resolution[xDimension] &&  // Use `!` for catching NaN.
                          request[yDimension] >= resolution[yDimension])) break;
                    bestFit = c;
                    level++;
                }
            }
            if (bestFit == this) {
                return readAtThisPyramidLevel(domain, ranges, null);
            }
            bestFit.pyramidLevel = level;
            bestFit.xDimension = xDimension;
            bestFit.yDimension = yDimension;
            bestFit.loadingStrategy = loadingStrategy;
        }
        // Invoke outside the synchronization lock because the new lock may be different.
        return bestFit.readAtThisPyramidLevel(domain, ranges, listeners);
    }

    /**
     * Implementation of {@link #read(GridGeometry, int...)} on the selected pyramid level.
     * This method may be invoked on the same instance as {@code read(…)} or a different instance.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @param  listenersOfLevel0  listeners of the resource at level 0, can be {@code null} if that resource is {@code this}.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    private GridCoverage readAtThisPyramidLevel(final GridGeometry domain, final int[] ranges, final StoreListeners listenersOfLevel0)
            throws DataStoreException
    {
        final TiledGridCoverage coverage;
        final GridCoverage loaded;
        final boolean preload;
        final long startTime;
        synchronized (getSynchronizationLock()) {
            // Note: `loadingStrategy` may still be null if unitialized.
            preload = (loadingStrategy == null || loadingStrategy == RasterLoadingStrategy.AT_READ_TIME);
            startTime = preload ? System.nanoTime() : 0;
            try {
                final var subset = new Subset(domain, ranges);
                if (listenersOfLevel0 != null) {
                    subset.listenersOfLevel0 = listenersOfLevel0;
                }
                coverage = read(subset);
                /*
                 * In theory the following condition is redundant with `supportImmediateLoading()`.
                 * We apply it anyway in case the coverage geometry is not what was announced.
                 * This condition is also necessary if `loadingStrategy` has not been initialized.
                 */
                if (!preload || coverage.getGridGeometry().getDimension() != BIDIMENSIONAL) {
                    return coverage;
                }
                final RenderedImage image = coverage.render(null);
                loaded = new GridCoverage2D(coverage.getGridGeometry(), coverage.getSampleDimensions(), image);
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
                throw canNotRead(listeners.getSourceName(), domain, cause);
            }
        }
        logReadOperation(coverage.getContentPath(null), coverage.getGridGeometry(), startTime);
        return loaded;
    }

    /**
     * Returns a coverage which will read the tiles as late as possible.
     *
     * @return the coverage.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    final TiledGridCoverage readAtGetTileTime() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            final RasterLoadingStrategy old = loadingStrategy;
            try {
                loadingStrategy = RasterLoadingStrategy.AT_GET_TILE_TIME;
                return read(new Subset(null, null));
            } catch (RuntimeException e) {
                throw canNotRead(listeners.getSourceName(), null, e);
            } finally {
                loadingStrategy = old;
            }
        }
    }

    /**
     * Chooses the pyramid to use for reading the specified subset from this resource.
     * This method should return an element of the list returned by {@link #getPyramids()}.
     * The chosen pyramid should be a best match, but does not need to be an exact match.
     *
     * <p>The current implementation returns the first pyramid returned by {@link #getPyramids()}.
     * Future versions of Apache <abbr>SIS</abbr> may improve this algorithm for taking in account
     * at least the <abbr>CRS</abbr>.</p>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the pyramid to use, or {@code null} if no pyramid can satisfy the given request.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    protected Pyramid choosePyramid(final GridGeometry domain, final int[] ranges) throws DataStoreException {
        return Containers.peekFirst(getPyramids());     // See javadoc about possible change in future SIS version.
    }

    /**
     * Whether this resource supports immediate loading of raster data.
     * Current implementation does not support immediate loading if the data cube has more than 2 dimensions.
     * Non-immediate loading allows users to specify two-dimensional slices.
     */
    private boolean supportImmediateLoading() throws DataStoreException {
        return getTileSize().length == BIDIMENSIONAL;
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     *
     * @return current raster data loading strategy for this resource.
     * @throws DataStoreException if an error occurred while fetching data store configuration.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
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
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
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

    /**
     * Sets the mapping from grid dimensions to image axes.
     * This method specifies the dimensions of the slices obtained
     * when {@linkplain TiledGridCoverage#readTiles reading tiles}.
     * The values specified to this method are used, directly or indirectly, at {@link Subset} creation time.
     * Therefore, calls to this method have an effect on the next {@link TiledGridCoverage} instances to be read,
     * but not on the instances that are already read.
     *
     * <p>If this method is never invoked, then by default
     * the dimension 0 of the grid is mapped to the image <var>x</var> axis and
     * the dimension 1 of the grid is mapped to the image <var>y</var> axis.</p>
     *
     * @param  xDimension  dimension of the grid which is mapped to the <var>x</var> axis (column indexes) in rendered images.
     * @param  yDimension  dimension of the grid which is mapped to the <var>y</var> axis (row indexes) in rendered images.
     * @throws IllegalArgumentException if {@code xDimension} or {@code yDimension} is negative, or the two values are equal.
     * @throws DataStoreException if another error occurred while setting the mapping from grid dimensions to image axes.
     *
     * @see TiledGridCoverage#xDimension
     * @see TiledGridCoverage#yDimension
     * @see GridExtent#getSubspaceDimensions(int)
     */
    protected void setRasterSubspaceDimensions(final int xDimension, final int yDimension) throws DataStoreException {
        final int max = getGridGeometry().getDimension() - 1;
        ArgumentChecks.ensureBetween("xDimension", 0, max, xDimension);
        ArgumentChecks.ensureBetween("yDimension", 0, max, yDimension);
        if (xDimension == yDimension) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "yDimension", "xDimension"));
        }
        this.xDimension = xDimension;
        this.yDimension = yDimension;
    }

    /**
     * Returns information about the {@code TileMatrixSet} instances to create.
     * The first element in the returned list <em>shall</em> be the default pyramid
     * using the same Coordinate Reference System (<abbr>CRS</abbr>) as this Grid Coverage Resource.
     * Other elements, if any, can use any <abbr>CRS</abbr>.
     *
     * <p>This method is invoked by the default implementation of {@link #getTileMatrixSets()} when first needed.
     * By default, this method returns a list of only one element, which itself describes a pyramid of only one level.
     * This single level describes a {@link TileMatrix} at the resolution of this {@code TiledGridCoverageResource}.</p>
     *
     * @return information about the tile matrix sets to create.
     * @throws DataStoreException if an error occurred while fetching information about the pyramid.
     *
     * @see #getResolutions()
     * @see #getTileMatrixSets()
     */
    protected List<Pyramid> getPyramids() throws DataStoreException {
        if (!getGridGeometry().isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS | GridGeometry.RESOLUTION)) {
            return List.of();
        }
        return List.of(new Pyramid() {
            @Override public OptionalInt numberOfLevels() {return OptionalInt.of(1);}
            @Override public TiledGridCoverageResource forPyramidLevel(int level) {
                return (level == 0) ? TiledGridCoverageResource.this : null;
            }
        });
    }

    /**
     * Description of a {@code TileMatrixSet} implemented as an image pyramid.
     * This interface is used by the default implementation of {@link #getTileMatrixSets()}.
     * There is usually only one pyramid per {@link TiledGridCoverageResource} instance,
     * but many pyramids may exist, for example, if data are offered in different
     * Coordinate Reference System (<abbr>CRS</abbr>).
     *
     * <p>Each pyramid can have an arbitrary number of levels.
     * It is recommended to have one pyramid level for each {@linkplain #getResolutions() preferred resolutions}.
     * The pyramid levels must be sorted from finest resolution (at level 0) to coarsest resolution.</p>
     *
     * <p>The number of levels is unspecified because some data stores cannot provide this information in advance.
     * Instead, the {@link #forPyramidLevel(int)} method will be invoked with different argument values when each
     * level is first requested, until that method returns {@code null} for a level too high.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.7
     * @since   1.7
     */
    protected static interface Pyramid {
        /**
         * Returns an identifier for this pyramid. The default implementation returns <abbr>TMS</abbr>
         * as the abbreviation of "Tile Matrix Set". This is often sufficient in the common case where
         * there is only one Tile Matrix Set per Grid Coverage Resource.
         *
         * <p>This value is used for building the value of {@link TileMatrixSet#getIdentifier()}.</p>
         *
         * @return an identifier for this pyramid. Default is {@code "TMS"}.
         *
         * @see TileMatrixSet#getIdentifier()
         */
        default String identifier() {
            return "TMS";
        }

        /**
         * Returns an identifier for the given level of this pyramid. The returned identifier
         * will be local in the namespace of the pyramid {@linkplain #identifier() identifier}.
         *
         * @param  level  the pyramid level where 0 is the level with the finest resolution.
         * @return a local identifier for the specified level.
         */
        default String identifierOfLevel(int level) {
            return "L" + level;
        }

        /**
         * Returns the level in this pyramid for the given local identifier.
         * This method is the converse of {@link #identifierOfLevel(int)}.
         *
         * @param  identifier  the identifier for which to get the pyramid level.
         * @return pyramid level associated to the given identifier.
         * @throws IllegalArgumentException if the given identifier is not recognized by this pyramid.
         */
        default int levelOfIdentifier(final String identifier) {
            if (identifier.isEmpty() || identifier.charAt(0) != 'L') {
                throw new IllegalArgumentException(identifier);
            }
            // Note: `NumberFormatException` is a subtype of `IllegalArgumentException`.
            return Integer.parseInt(identifier.substring(1));
        }

        /**
         * Returns the number of pyramid levels if this information is known.
         * The returned value is empty if computing the number of levels is costly.
         * For iterations over pyramid levels, it is generally preferable to invoke
         * {@link #forPyramidLevel(int)} with increasing {@code level} values until
         * that method returns {@code null}.
         *
         * @return the number of pyramid levels if this information is known.
         */
        default OptionalInt numberOfLevels() {
            return OptionalInt.empty();
        }

        /**
         * Returns a resource for the same data as this resource but at a different resolution level.
         * The resource at index 0 shall be the resource with the finest resolution, and resources at
         * increasing index values shall be resources with increasingly coarser resolutions.
         * If the specified level is equal or greater than the number of levels in this pyramid,
         * then this method shall return {@code null}.
         *
         * <p>If this method returns a non-null instance <var>r</var>, then the following condition should hold:
         * {@code r.getGridGeometry().getResolution(false)} should be equal, ignoring NaN values and rounding
         * errors, to {@code getResolutions().get(level)}.</p>
         *
         * @param  level  the pyramid level where 0 is the level with the finest resolution.
         * @return a resource for data at the specified pyramid level, or {@code null} if the given level is too high.
         * @throws DataStoreException if an error occurred while creating the resource.
         *
         * @see #getResolutions()
         */
        TiledGridCoverageResource forPyramidLevel(int level) throws DataStoreException;

        /**
         * Returns the name factory to use for creating identifiers of tiles and tile matrices.
         * The default implementation returns {@link DefaultNameFactory#provider()}.
         * Subclasses can override for more control on the identifiers to create.
         *
         * @return the name factory to use for creating identifiers of tiles and tile matrices.
         */
        default NameFactory nameFactory() {
            return DefaultNameFactory.provider();
        }
    }

    /**
     * Returns the localized resources for error messages.
     */
    final Errors errors() {
        return Errors.forLocale(listeners.getLocale());
    }
}
