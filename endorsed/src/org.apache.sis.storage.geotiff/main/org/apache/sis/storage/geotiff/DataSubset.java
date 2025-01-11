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
package org.apache.sis.storage.geotiff;

import java.util.Arrays;
import java.util.Locale;
import java.nio.Buffer;
import java.io.Closeable;
import java.io.IOException;
import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.toIntExact;
import org.opengis.util.GenericName;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.io.stream.Region;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.image.privy.TilePlaceholder;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.storage.geotiff.reader.ReversedBitsChannel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Vector;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;


/**
 * Raster data obtained from a GeoTIFF file in the domain requested by user. The number of dimensions is 2
 * for standard TIFF files, but this class accepts higher number of dimensions if 3- or 4-dimensional data
 * are stored in a GeoTIFF file using some convention. This base class transfers uncompressed data.
 * Compressed data are handled by specialized subclasses.
 *
 * <h2>Cell Coordinates</h2>
 * When there is no subsampling, {@code DataSubset} uses the same cell coordinates as {@link DataCube}.
 * When there is a subsampling, cell coordinates in this subset are divided by the subsampling factors.
 * Conversion is done by {@link #coverageToResourceCoordinate(long, int)}.
 *
 * <h2>Tile Matrix Coordinates</h2>
 * In each {@code DataSubset}, indices of tiles starts at (0, 0, …). This class does not use
 * the same tile indices as {@link DataCube} in order to avoid integer overflow.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class DataSubset extends TiledGridCoverage implements Localized {
    /**
     * The resource which contain this {@code DataSubset}.
     * Used for fetching information like the input channel and where to report warnings.
     */
    final DataCube source;

    /**
     * For each tile, the byte offset of that tile as compressed and stored on disk.
     * Tile X index varies fastest, followed by tile Y index, then tile Z index if any.
     * The first tile included in this {@code DataSubset} is at index {@link #indexOfFirstTile}.
     *
     * @see ImageFileDirectory#tileOffsets
     * @see #indexOfFirstTile
     * @see #tileStrides
     */
    private final Vector tileOffsets;

    /**
     * For each tile, the number of (compressed) bytes in that tile.
     * Elements are in the same order as {@link #tileOffsets}.
     *
     * @see ImageFileDirectory#tileByteCounts
     * @see #indexOfFirstTile
     * @see #tileStrides
     */
    private final Vector tileByteCounts;

    /**
     * Total number of tiles in the image. This is used for computing the stride between
     * a band and the next band in {@link #tileOffsets} and {@link #tileByteCounts} vectors.
     */
    private final int numTiles;

    /**
     * Number of banks retained for the target image data buffer.
     * This is equal to the number of bands only for planar images, and 1 in all other cases.
     * If the user asked to read only a subset of the bands, then "number of bands" in above
     * sentence is the {@linkplain #includedBands subset} size.
     */
    protected final int numBanks;

    /**
     * Number of interleaved sample values in a pixel in the GeoTIFF file (ignoring band subset).
     * For planar images (banded sample model), this is equal to 1. For pixel interleaved image,
     * this is equal to the number of bands in the original image.
     *
     * <p>Note: a sample may be a fraction of byte. For example, in a bilevel image, a sample is a bit
     * and 8 samples are packed in each byte. Conversely a sample may also be 1, 2, 4 or 8 bytes.</p>
     *
     * @see java.awt.image.ComponentSampleModel#getPixelStride()
     */
    protected final int sourcePixelStride;

    /**
     * Number of interleaved sample values in a pixel of the image to load in memory.
     * This is similar to {@link #sourcePixelStride}, but taking in account the number
     * of bands requested by user at reading time.
     */
    protected final int targetPixelStride;

    /**
     * Provider of empty tiles, created only if needed. Empty tiles are tiles with a length of 0
     * declared in the TIFF header. This interpretation is a GDAL extension, not a TIFF standard.
     */
    private TilePlaceholder emptyTiles;

    /**
     * Creates a new data subset. All parameters should have been validated
     * by {@link ImageFileDirectory#validateMandatoryTags()} before this call.
     * This constructor should be invoked inside a synchronized block.
     *
     * @param  source  the resource which contain this {@code DataSubset}.
     * @param  subset  description of the {@code owner} subset to cover.
     * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
     */
    DataSubset(final DataCube source, final TiledGridResource.Subset subset) throws DataStoreException {
        super(subset);
        this.source    = source;
        this.numTiles  = toIntExact(source.getNumTiles());
        final Vector[] tileArrayInfo = source.getTileArrayInfo();
        this.tileOffsets    = tileArrayInfo[0];
        this.tileByteCounts = tileArrayInfo[1];
        /*
         * "Banks" (in `java.awt.image.DataBuffer` sense) are synonymous to "bands" for planar image only.
         * Otherwise there is only one bank no matter the number of bands. Each bank will be read separately.
         */
        final int maxBank;
        if (model instanceof BandedSampleModel) {
            numBanks = model.getNumBands();
            sourcePixelStride = targetPixelStride = 1;
            maxBank = (includedBands != null) ? includedBands[includedBands.length - 1] : numBanks - 1;
            // Note: `includedBands` is in strictly increasing order, so taking the last value is okay.
        } else {
            maxBank  = 0;
            numBanks = 1;
            sourcePixelStride = source.getNumBands();
            targetPixelStride = model .getNumBands();
        }
        final int n = tileOffsets.size();
        if (maxBank >= n / numTiles) {
            throw new DataStoreContentException(source.reader.errors().getString(
                    Errors.Keys.TooFewCollectionElements_3, "tileOffsets", (maxBank + 1) * numTiles, n));
        }
    }

    /**
     * Returns the locale for warning or error messages, or {@code null} if unspecified.
     */
    @Override
    public final Locale getLocale() {
        return source.listeners().getLocale();
    }

    /**
     * Returns an human-readable identification of this coverage.
     * The namespace should be the {@linkplain #filename() filename}
     * and the tip can be an image index, citation, or overview level.
     */
    @Override
    protected final GenericName getIdentifier() {
        // Should never be empty (see `DataCube.getIdentifier()` contract).
        return source.getIdentifier().get();
    }

    /**
     * Returns the type of data in all tiles. Note that more than one pixel may be packed in
     * a single element of the returned type (e.g. bilevel image using only one bit per pixel).
     * The {@link java.awt.image.SampleModel#getSampleSize(int)} method should be invoked for
     * a complement of information.
     */
    protected final DataType getDataType() {
        return DataType.forDataBufferType(model.getDataType());
    }

    /**
     * Returns the size of a bank (not necessarily a band) in number of primitive elements (bytes, integers, …).
     * This is tile width × height × {@link #targetPixelStride} divided by the number of sample values per element,
     * with each row starting on an element boundary.
     *
     * @param  pixelsPerElement  always 1 except when two or more pixels are packed in each element.
     * @return expected number of primitive elements in the bank.
     */
    protected final int getBankCapacity(final int pixelsPerElement) {
        // `ceilDiv(…)` must happen before multiplication by image height.
        final int scanlineStride = ceilDiv(multiplyExact(model.getWidth(), targetPixelStride), pixelsPerElement);
        return multiplyExact(scanlineStride, model.getHeight());
    }

    /**
     * Returns the input of bytes for compressed raster data. If the TIFF tag {@code FillOrder} is 2
     * (which should be very rare), the input stream reverse the order of all bits in each byte.
     */
    final ChannelDataInput input() throws IOException {
        ChannelDataInput input = source.reader.input;
        if (source.isBitOrderReversed()) {
            input = ReversedBitsChannel.wrap(input);
        }
        return input;
    }

    /**
     * Information about a tile to be read. A list of {@code Tile} is created and sorted by increasing offsets
     * before the read operation begins, in order to read tiles in the order they are written in the TIFF file.
     */
    private static final class Tile extends Snapshot implements Comparable<Tile> {
        /**
         * Value of {@link DataSubset#tileOffsets} at index {@link #getTileIndexInResource()}.
         * If pixel data are stored in different planes ("banks" in Java2D terminology),
         * then current implementation takes only the offset of the first bank to read.
         * This field contains the value that we want in increasing order.
         *
         * @see #compareTo(Tile)
         */
        private final long byteOffset;

        /**
         * Stores information about a tile to be loaded.
         *
         * @param domain         the iterator for which to create a snapshot of its current position.
         * @param tileOffsets    the {@link DataSubset#tileOffsets} vector.
         * @param includedBanks  indices of banks to read, or {@code null} for reading all of them.
         * @param numTiles       value of {@link DataSubset#numTiles} (total number of tiles in the image).
         */
        Tile(final AOI domain, final Vector tileOffsets, final int[] includedBanks, final int numTiles) {
            super(domain);
            int p = getTileIndexInResource();
            if (includedBanks != null) {
                p += includedBanks[0] * numTiles;
            }
            byteOffset = tileOffsets.longValue(p);
        }

        /**
         * Notifies the input channel about the range of bytes that we are going to read.
         *
         * @param tileOffsets    the {@link DataSubset#tileOffsets} vector.
         * @param tileByteCounts the {@link DataSubset#tileByteCounts} vector.
         * @param b              indices of banks to read.
         * @param numTiles       value of {@link DataSubset#numTiles} (total number of tiles in the image).
         * @param input          the input to notify about the ranges of bytes to read.
         */
        final void notifyInputChannel(final Vector tileOffsets, final Vector tileByteCounts,
                                      int b, final int numTiles, final ChannelDataInput input)
        {
            b = getTileIndexInResource() + b * numTiles;
            final long offset = tileOffsets.longValue(b);
            final long length = tileByteCounts.longValue(b);
            input.rangeOfInterest(offset, Numerics.saturatingAdd(offset, length));
        }

        /**
         * Copies {@link #tileOffsets} or {@link #tileByteCounts} values into the given target array.
         * Values for different planes ("banks" in Java2D terminology) are packed as consecutive values
         * in the given target array.
         *
         * @param  source    {@link DataSubset#tileOffsets} or {@link DataSubset#tileByteCounts}.
         * @param  target    the array where to copy vector values.
         * @param  numTiles  value of {@link DataSubset#numTiles}.
         */
        final void copyTileInfo(final Vector source, final long[] target, final int[] includedBanks, final int numTiles) {
            for (int j=0; j<target.length; j++) {
                final int i = getTileIndexInResource() + numTiles * (includedBanks != null ? includedBanks[j] : j);
                target[j] = source.longValue(i);
            }
        }

        /**
         * Compares this tile with the specified tile for order in which to perform read operations.
         */
        @Override public int compareTo(final Tile other) {
            return Long.compare(byteOffset, other.byteOffset);
        }
    }

    /**
     * Returns all tiles in the given area of interest. Tile indices are relative to this {@code DataSubset}:
     * (0,0) is the tile in the upper-left corner of this {@code DataSubset} (not necessarily the upper-left
     * corner of the image stored in the TIFF file).
     *
     * The {@link Raster#getMinX()} and {@code getMinY()} coordinates of returned rasters
     * will start at the given {@code offsetAOI} values.
     *
     * <p>This method is thread-safe. Synchronization is done on {@link DataCube#getSynchronizationLock()}.</p>
     *
     * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
     * @return tiles decoded from the TIFF file.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     * @throws RuntimeException if the Java2D image cannot be created for another reason
     *         (too many exception types to list them all).
     */
    @Override
    @SuppressWarnings("try")
    protected final Raster[] readTiles(final TileIterator iterator) throws IOException, DataStoreException {
        /*
         * Prepare an array for all tiles to be returned. Tiles that are already in memory will be stored
         * in this array directly. Other tiles will be declared in the `missings` array and loaded later.
         * Each tile will either store all sample values in an interleaved fashion inside a single bank
         * (`sourcePixelStride` > 1) or use one separated bank per band (`sourcePixelStride` == 1).
         */
        final ChannelDataInput input = source.reader.input;
        final int[] includedBanks = (sourcePixelStride == 1) ? includedBands : null;
        final Raster[] result = new Raster[iterator.tileCountInQuery];
        final Tile[] missings = new Tile[iterator.tileCountInQuery];
        int numMissings = 0;
        boolean needsCompaction = false;
        synchronized (source.getSynchronizationLock()) {
            do {
                final Raster tile = iterator.getCachedTile();
                if (tile != null) {
                    result[iterator.getTileIndexInResultArray()] = tile;
                } else {
                    /*
                     * Tile not yet loaded. Add to a queue of tiles to load later.
                     * Notify the input channel about the ranges of bytes to read.
                     * This notification is redundant with the same notification
                     * done in `CompressionChannel.setInputRegion(…)`, but doing
                     * all notifications in advance gives a chance to group ranges.
                     */
                    final Tile missing = new Tile(iterator, tileOffsets, includedBanks, numTiles);
                    missings[numMissings++] = missing;
                    if (includedBanks == null) {
                        missing.notifyInputChannel(tileOffsets, tileByteCounts, 0, numTiles, input);
                    } else for (int b : includedBanks) {
                        missing.notifyInputChannel(tileOffsets, tileByteCounts, b, numTiles, input);
                    }
                }
            } while (iterator.next());
            if (numMissings != 0) {
                Arrays.sort(missings, 0, numMissings);
                /*
                 * At this point we finished to list all tiles inside the Area Of Interest (AOI), both the ones that
                 * were already in memory and the new ones. The loop below processes only the new tiles, by reading
                 * them in the order they appear in the TIFF file.
                 *
                 * TODO: Use `tile.byteCount` for checking if two tiles are consecutive in the TIFF file.
                 * In such case we should send only one HTTP range request.
                 */
                final long[] lower       = new long[BIDIMENSIONAL];   // Coordinates of the first pixel to read relative to the tile.
                final long[] upper       = new long[BIDIMENSIONAL];   // Coordinates after the last pixel to read relative to the tile.
                final long[] subsampling = new long[BIDIMENSIONAL];
                final Point  origin      = new Point();
                final long[] offsets     = new long[numBanks];
                final long[] byteCounts  = new long[numBanks];
                try (Closeable finisher  = createInflater()) {
                    for (int i=0; i<numMissings; i++) {
                        final Tile tile = missings[i];
                        if (tile.getRegionInsideTile(lower, upper, subsampling, false)) {
                            origin.x = tile.originX;
                            origin.y = tile.originY;
                            tile.copyTileInfo(tileOffsets,    offsets,    includedBanks, numTiles);
                            tile.copyTileInfo(tileByteCounts, byteCounts, includedBanks, numTiles);
                            boolean isEmpty = true;
                            for (int b=0; b<offsets.length; b++) {
                                isEmpty &= (byteCounts[b] == 0);
                            }
                            /*
                             * If the length is zero for all bands, the GDAL "sparse files" convention said
                             * that pixel values are not stored in the file and are assumed zero for all pixels.
                             * This is a GDAL-specific convention but seems reasonable. Note that the default
                             * fill value zero is different than `TilePlaceholder` default, which can be NaN.
                             */
                            final Raster r;
                            if (isEmpty) {
                                if (emptyTiles == null) {
                                    Number[] values = fillValues;
                                    if (values == null) {
                                        values = new Number[model.getNumBands()];
                                        Arrays.fill(values, 0);
                                    }
                                    emptyTiles = TilePlaceholder.filled(model, values);
                                }
                                r = emptyTiles.create(origin);
                            } else {
                                r = readSlice(offsets, byteCounts, lower, upper, subsampling, origin);
                            }
                            result[tile.getTileIndexInResultArray()] = tile.cache(r);
                        } else {
                            needsCompaction = true;
                        }
                    }
                }
            }
        }
        /*
         * If the subsampling is larger than tile size, some tiles were empty and excluded.
         * The corresponding elements in the `result` array were left to the null value.
         * We need to compact the array by removing those null elements.
         */
        if (needsCompaction) {
            int n = 0;
            for (final Raster tile : result) {
                if (tile != null) result[n++] = tile;
            }
            return Arrays.copyOf(result, n);
        }
        return result;
    }

    /**
     * Invoked in a synchronized block before the first call to {@code readSlice(…)}.
     * Subclasses can override this method for allocating resources to be reused for
     * reading each tile. The {@link Closeable#close()} method of the returned object
     * will be invoked (even if an exception has been thrown during the reading process)
     * in the same synchronized block after the last call to {@code readSlice(…)}.
     *
     * <p>The default implementation returns a no-operation object. Direct subclasses
     * can ignore; they do not need to invoke {@code super.createInflater()}.
     */
    Closeable createInflater() {
        return NOOP;
    }

    /**
     * No-operation "resource" for {@link #createInflater()} default value.
     */
    private static final Closeable NOOP = () -> {};

    /**
     * Reads a two-dimensional slice of the data cube from the given input channel. This method is usually
     * invoked for reading the tile in full, in which case the {@code lower} argument is (0,0) and the
     * {@code upper} argument is the tile size. But those arguments may identify a smaller region if the
     * {@link DataSubset} contains only one (potentially large) tile.
     *
     * <p>The length of {@code lower}, {@code upper} and {@code subsampling} arrays shall be 2.</p>
     *
     * <h4>Default implementation</h4>
     * The default implementation in this base class assumes uncompressed data without band subset.
     * Subsampling on the <var>X</var> axis is not supported if the image has interleaved pixels.
     * Packed pixels (é.g. bilevel images with 8 pixels per byte) are not supported.
     * Those restrictions are verified by {@link DataCube#canReadDirect(TiledGridResource.Subset)}.
     * Subclasses must override for handling decompression or for resolving above-cited limitations.
     *
     * @todo It is possible to relax a little bit some restrictions. If the tile width is a divisor
     *       of the sample size, we could round {@code lower[0]} and {@code upper[0]} to a multiple
     *       of {@code sampleSize}. We would need to adjust the coordinates of returned image accordingly.
     *       This adjustment need to be done by the caller.
     *
     * @param  offsets      position in the channel where tile data begins, one value per bank.
     * @param  byteCounts   number of bytes for the compressed tile data, one value per bank.
     * @param  lower        (<var>x</var>, <var>y</var>) coordinates of the first pixel to read relative to the tile.
     * @param  upper        (<var>x</var>, <var>y</var>) coordinates after the last pixel to read relative to the tile.
     * @param  subsampling  (<var>sx</var>, <var>sy</var>) subsampling factors.
     * @param  location     pixel coordinates in the upper-left corner of the tile to return.
     * @return a single tile decoded from the GeoTIFF file.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     * @throws RuntimeException if the Java2D image cannot be created for another reason
     *         (too many exception types to list them all).
     *
     * @see DataCube#canReadDirect(TiledGridResource.Subset)
     */
    Raster readSlice(final long[] offsets, final long[] byteCounts, final long[] lower, final long[] upper,
                     final long[] subsampling, final Point location) throws IOException, DataStoreException
    {
        final DataType type = getDataType();
        final int sampleSize = type.size();     // Assumed same as `SampleModel.getSampleSize(…)` by preconditions.
        final long width  = subtractExact(upper[X_DIMENSION], lower[X_DIMENSION]);
        final long height = subtractExact(upper[Y_DIMENSION], lower[Y_DIMENSION]);
        /*
         * The number of bytes to read should not be greater than `byteCount`. It may be smaller however if only
         * a subregion is read. Note that the `length` value may be different than `capacity` if the tile to read
         * is smaller than the "standard" tile size of the image. It happens often when reading the last strip.
         * This length is used only for verification purpose so it does not need to be exact.
         */
        final long length = ceilDiv(width * height * sourcePixelStride * sampleSize, Byte.SIZE);
        final long[] size = new long[] {
            multiplyExact(getTileSize(X_DIMENSION), sourcePixelStride),
                          getTileSize(Y_DIMENSION)
        };
        /*
         * If we use an interleaved sample model, each "element" from `HyperRectangleReader` perspective is actually a
         * group of `sourcePixelStride` values. Note that in such case, we cannot handle subsampling on the first axis.
         * Such case should be handled by the `CompressedSubset` subclass instead, even if there is no compression.
         */
        assert sourcePixelStride == 1 || subsampling[X_DIMENSION] == 1;
        lower[X_DIMENSION] *= sourcePixelStride;
        upper[X_DIMENSION] *= sourcePixelStride;
        /*
         * Read each plane ("banks" in Java2D terminology). Note that a single bank contains all bands
         * in the interleaved sample model case. This block assumes that each bank element contains
         * exactly one sample value (verified by assertion), as documented in the Javadoc of this method.
         * If that assumption was not true, we would have to adjust `capacity`, `lower[0]` and `upper[0]`
         * (we may do that as an optimization in a future version).
         */
        final var hr     = new HyperRectangleReader(ImageUtilities.toNumberEnum(type.toDataBufferType()), input());
        final var region = new Region(size, lower, upper, subsampling);
        final var banks  = new Buffer[numBanks];
        for (int b=0; b<numBanks; b++) {
            if (b < byteCounts.length && length > byteCounts[b]) {
                throw new DataStoreContentException(source.reader.resources().getString(
                        Resources.Keys.UnexpectedTileLength_2, length, byteCounts[b]));
            }
            hr.setOrigin(offsets[b]);
            assert model.getSampleSize(b) == sampleSize;                        // See above comment.
            final Buffer bank = hr.readAsBuffer(region, getBankCapacity(1));
            fillRemainingRows(bank, b);
            banks[b] = bank;
        }
        final DataBuffer buffer = RasterFactory.wrap(type, banks);
        return createWritableRaster(buffer, location);
    }

    /**
     * Applies the fill value if it is different than the default value (zero) to all remaining rows.
     * This method is needed because the buffer filled by read methods may have less data than the buffer
     * capacity if the current tile is smaller than the expected tile size (e.g. last tile is truncated).
     *
     * @param  bank  the buffer where to fill remaining rows.
     * @param  band  index of the band to fill. Same as bank index in the particular case of {@code DataSubset} class.
     */
    final void fillRemainingRows(final Buffer bank, final int band) {
        if (fillValues != null) {
            final int limit    = bank.limit();
            final int capacity = bank.capacity();   // Equals `this.capacity` except for packed sample model.
            if (limit != capacity) {
                final Vector v = Vector.create(bank.limit(capacity), DataType.isUnsigned(model));
                final Number f = fillValues[band];
                /*
                 * If all values are the same, we can delegate (indirectly) to an `Arrays.fill(…)` method.
                 * Also, if the raster stores each band in a separated bank (banded sample model),
                 * we have only one value to set in the given bank.
                 */
                if (ArraysExt.allEquals(fillValues, f) || model instanceof BandedSampleModel) {
                    v.fill(limit, capacity, f);
                } else {
                    // Slow fallback for interleaved sample models.
                    for (int i=limit; i<capacity; i++) {
                        v.set(i, fillValues[i % fillValues.length]);
                    }
                }
            }
        }
    }

    /**
     * Creates the raster with the given data buffer, which contains the pixels that have been read.
     *
     * @param  buffer    the sample values which have been read from the GeoTIFF tile.
     * @param  location  pixel coordinates in the upper-left corner of the tile to return.
     * @return raster with the given sample values.
     */
    final Raster createWritableRaster(final DataBuffer buffer, final Point location) {
        final Number fill = source.getReplaceableFillValue();
        if (fill != null) {
            switch (buffer.getDataType()) {
                case DataBuffer.TYPE_FLOAT: {
                    for (float[] bank : ((DataBufferFloat) buffer).getBankData()) {
                        ArraysExt.replace(bank, fill.floatValue(), Float.NaN);
                    }
                    break;
                }
                case DataBuffer.TYPE_DOUBLE: {
                    for (double[] bank : ((DataBufferDouble) buffer).getBankData()) {
                        ArraysExt.replace(bank, fill.doubleValue(), Double.NaN);
                    }
                    break;
                }
            }
        }
        return Raster.createWritableRaster(model, buffer, location);
    }
}
