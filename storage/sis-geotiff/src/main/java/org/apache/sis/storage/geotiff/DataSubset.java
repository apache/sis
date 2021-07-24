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
import java.io.IOException;
import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.storage.io.Region;
import org.apache.sis.internal.storage.io.HyperRectangleReader;
import org.apache.sis.internal.storage.TiledGridCoverage;
import org.apache.sis.internal.storage.TiledGridResource;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.RasterFactory;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Localized;
import org.apache.sis.math.Vector;

import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static org.apache.sis.internal.jdk9.JDK9.multiplyFull;
import static java.lang.Math.toIntExact;


/**
 * Raster data obtained from a GeoTIFF file in the domain requested by user. The number of dimensions is 2
 * for standard TIFF files, but this class accepts higher number of dimensions if 3- or 4-dimensional data
 * are stored in a GeoTIFF file using some convention. This base class transfers uncompressed data.
 * Compressed data are handled by specialized subclasses.
 *
 * <h2>Cell Coordinates</h2>
 * When there is no subsampling, {@code DataSubset} uses the same cell coordinates than {@link DataCube}.
 * When there is a subsampling, cell coordinates in this subset are divided by the subsampling factors.
 * Conversion is done by {@link #toFullResolution(long, int)}.
 *
 * <h2>Tile Matrix Coordinates</h2>
 * In each {@code DataSubset}, indices of tiles starts at (0, 0, …). This class does not use
 * the same tile indices than {@link DataCube} in order to avoid integer overflow.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class DataSubset extends TiledGridCoverage implements Localized {
    /**
     * The resource which contain this {@code DataSubset}.
     * Used for fetching information like the input channel and where to report warnings.
     */
    private final DataCube source;

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
     * Elements are in the same order than {@link #tileOffsets}.
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
     * Number of banks in the image data buffer.
     * This is equal to the number of bands only for planar images, and 1 in all other cases.
     */
    protected final int numBanks;

    /**
     * Number of interleaved sample values in a pixel. For planar images, this is equal to 1.
     * For interleaved sample model, this is equal to the number of bands. This value is often
     * equal to the {@linkplain java.awt.image.ComponentSampleModel#getPixelStride() pixel stride}.
     */
    protected final int numInterleaved;

    /**
     * Number of sample values in a bank (not necessarily a band).
     * This is tile width × height × {@link #numInterleaved}.
     */
    protected final int capacity;

    /**
     * Creates a new data subset. All parameters should have been validated
     * by {@link ImageFileDirectory#validateMandatoryTags()} before this call.
     * This constructor should be invoked inside a synchronized block.
     *
     * @param  source   the resource which contain this {@code DataSubset}.
     * @param  subset   description of the {@code owner} subset to cover.
     * @param  rasters  potentially shared cache of rasters read by this {@code DataSubset}.
     * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
     */
    DataSubset(final DataCube source, final TiledGridResource.Subset subset) throws DataStoreException {
        super(subset, source.getSampleModel(), source.getColorModel(), source.fillValue());
        this.source   = source;
        this.numTiles = toIntExact(source.getNumTiles());
        final Vector[] tileArrayInfo = source.getTileArrayInfo();
        this.tileOffsets    = tileArrayInfo[0];
        this.tileByteCounts = tileArrayInfo[1];
        /*
         * "Banks" (in `java.awt.image.DataBuffer` sense) are synonymous to "bands" for planar image only.
         * Otherwise there is only one bank no matter the amount of bands. Each bank will be read separately.
         */
        if (model instanceof BandedSampleModel) {
            numBanks = model.getNumBands();
            numInterleaved = 1;
        } else {
            numBanks = 1;
            numInterleaved = model.getNumBands();
        }
        final int n = tileOffsets.size();
        if (numBanks > n / numTiles) {
            throw new DataStoreContentException(source.reader.errors().getString(
                    Errors.Keys.TooFewCollectionElements_3, "tileOffsets", numBanks * numTiles, n));
        }
        capacity = multiplyExact(multiplyExact(model.getWidth(), model.getHeight()), numInterleaved);
    }

    /**
     * Returns the locale for warning or error messages, or {@code null} if unspecified.
     */
    @Override
    public final Locale getLocale() {
        return source.getLocale();
    }

    /**
     * Returns an human-readable identification of this coverage for error messages.
     */
    @Override
    protected final String getDisplayName() {
        return source.filename();
    }

    /**
     * Returns the type of data in all tiles.
     */
    protected final DataType getDataType() {
        return DataType.forDataBufferType(model.getDataType());
    }

    /**
     * Returns the GeoTIFF reader which contains this subset.
     */
    final Reader reader() {
        return source.reader;
    }

    /**
     * Information about a tile to be read. A list of {@code Tile} is created and sorted by increasing offsets
     * before the read operation begins, in order to read tiles in the order they are written in the TIFF file.
     */
    private static final class Tile extends Snapshot implements Comparable<Tile> {
        /**
         * Value of {@link DataSubset#tileOffsets} at index {@link #indexInTileVector}.
         * If pixel data are stored in different planes ("banks" in Java2D terminology),
         * this is the smallest value of all banks.
         * This is the value that we want in increasing order.
         *
         * @see #compareTo(Tile)
         */
        private final long byteOffset;

        /**
         * Stores information about a tile to be loaded.
         *
         * @param iterator  the iterator for which to create a snapshot of its current position.
         */
        Tile(final AOI domain, final Vector tileOffsets, final int numTiles) {
            super(domain);
            int p = indexInTileVector;
            long offset = tileOffsets.longValue(p);
            final int limit = tileOffsets.size() - numTiles;
            while (p < limit) {
                // TODO: should take in account only the bands selected by user.
                offset = Math.min(offset, tileOffsets.longValue(p += numTiles));
            }
            byteOffset = offset;
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
        final void copyTileInfo(final Vector source, final long[] target, final int numTiles) {
            int i = indexInTileVector;
            for (int j=0; j<target.length; j++) {
                target[j] = source.longValue(i);
                i += numTiles;
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
     * The {@link WritableRaster#getMinX()} and {@code getMinY()} coordinates of returned rasters
     * will start at the given {@code offsetAOI} values.
     *
     * <p>This method is thread-safe.</p>
     *
     * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
     * @return tiles decoded from the TIFF file.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     * @throws RuntimeException if the Java2D image can not be created for another reason
     *         (too many exception types to list them all).
     */
    @Override
    protected final WritableRaster[] readTiles(final AOI iterator) throws IOException, DataStoreException {
        /*
         * Prepare an array for all tiles to be returned. Tiles that are already in memory will be stored
         * in this array directly. Other tiles will be declared in the `missings` array and loaded later.
         */
        final WritableRaster[] result = new WritableRaster[iterator.tileCountInQuery];
        final Tile[] missings = new Tile[iterator.tileCountInQuery];
        int numMissings = 0;
        synchronized (reader().store) {
            do {
                final WritableRaster tile = iterator.getCachedTile();
                if (tile != null) {
                    result[iterator.getIndexInResultArray()] = tile;
                } else {
                    // Tile not yet loaded. Add to a queue of tiles to load later.
                    missings[numMissings++] = new Tile(iterator, tileOffsets, numTiles);
                }
            } while (iterator.next());
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
            final int[]  subsampling = new int [BIDIMENSIONAL];
            final Point  origin      = new Point();
            final long[] offsets     = new long[tileOffsets.size() / numTiles];
            final long[] byteCounts  = new long[tileByteCounts.size() / numTiles];
            boolean needsCompaction  = false;
            for (int i=0; i<numMissings; i++) {
                final Tile tile = missings[i];
                if (tile.getRegionInsideTile(lower, upper, subsampling, BIDIMENSIONAL)) {
                    origin.x = tile.originX;
                    origin.y = tile.originY;
                    tile.copyTileInfo(tileOffsets,    offsets,    numTiles);
                    tile.copyTileInfo(tileByteCounts, byteCounts, numTiles);
                    for (int b=0; b<offsets.length; b++) {
                        offsets[b] = addExact(offsets[b], reader().origin);
                    }
                    result[tile.indexInResultArray] = tile.cache(
                            readSlice(offsets, byteCounts, lower, upper, subsampling, origin));
                } else {
                    needsCompaction = true;
                }
            }
            /*
             * If the subsampling is larger than tile size, some tiles were empty and excluded.
             * The corresponding elements in the `result` array were left to the null value.
             * We need to compact the array by removing those null elements.
             */
            if (needsCompaction) {
                int n = 0;
                for (final WritableRaster tile : result) {
                    if (tile != null) result[n++] = tile;
                }
                return Arrays.copyOf(result, n);
            }
        }
        return result;
    }

    /**
     * Reads a two-dimensional slice of the data cube from the given input channel. This method is usually
     * invoked for reading the tile in full, in which case the {@code lower} argument is (0,0) and the
     * {@code upper} argument is the tile size. But those arguments may identify a smaller region if the
     * {@link DataSubset} contains only one (potentially large) tile.
     *
     * <p>The length of {@code lower}, {@code upper} and {@code subsampling} arrays shall be 2.</p>
     *
     * <p>The default implementation in this base class assumes uncompressed data.
     * Subclasses must override for handling decompression.</p>
     *
     * @param  offsets      position in the channel where tile data begins, one value per bank.
     * @param  byteCounts   number of bytes for the compressed tile data, one value per bank.
     * @param  lower        (<var>x</var>, <var>y</var>) coordinates of the first pixel to read relative to the tile.
     * @param  upper        (<var>x</var>, <var>y</var>) coordinates after the last pixel to read relative to the tile.
     * @param  subsampling  (<var>sx</var>, <var>sy</var>) subsampling factors.
     * @param  location     pixel coordinates in the upper-left corner of the tile to return.
     * @return image decoded from the GeoTIFF file.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     * @throws RuntimeException if the Java2D image can not be created for another reason
     *         (too many exception types to list them all).
     */
    WritableRaster readSlice(final long[] offsets, final long[] byteCounts, final long[] lower, final long[] upper,
                             final int[] subsampling, final Point location) throws IOException, DataStoreException
    {
        final DataType type = getDataType();
        final long width  = subtractExact(upper[0], lower[0]);
        final long height = subtractExact(upper[1], lower[1]);
        /*
         * The number of bytes to read should not be greater than `byteCount`. It may be smaller however if only
         * a subregion is read. Note that the `length` value may be different than `capacity` if the tile to read
         * is smaller than the "standard" tile size of the image. It happens often when reading the last strip.
         */
        final long length  = multiplyExact(type.size() / Byte.SIZE,
                             multiplyExact(multiplyExact(width, height), numInterleaved));
        final long[] size = new long[] {multiplyFull(numInterleaved, getTileSize(0)), getTileSize(1)};
        /*
         * If we use an interleaved sample model, each "element" from `HyperRectangleReader` perspective is actually
         * a group of `numInterleaved` values. Note that in such case, we can not handle subsampling on the first axis.
         * Such case should be handled by the `CompressedSubset` subclass instead, even if there is no compression.
         */
        assert numInterleaved == 1 || subsampling[0] == 1;
        lower[0] *= numInterleaved;
        upper[0] *= numInterleaved;
        /*
         * Read each plane ("banks" in Java2D terminology). Note that a single bank contain all bands
         * in the interleaved sample model case.
         */
        final HyperRectangleReader hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(type.toDataBufferType()), reader().input);
        final Region region = new Region(size, lower, upper, subsampling);
        final Buffer[] banks = new Buffer[numBanks];
        for (int b=0; b<numBanks; b++) {
            if (b < byteCounts.length && length > byteCounts[b]) {
                throw new DataStoreContentException(reader().resources().getString(
                        Resources.Keys.UnexpectedTileLength_2, length, byteCounts[b]));
            }
            hr.setOrigin(offsets[b]);
            final Buffer bank = hr.readAsBuffer(region, capacity);
            fillRemainingRows(bank);
            banks[b] = bank;
        }
        final DataBuffer buffer = RasterFactory.wrap(type, banks);
        return WritableRaster.createWritableRaster(model, buffer, location);
    }

    /**
     * Applies the fill value if it is different than the default value (zero) to all remaining rows.
     * This method is needed because the buffer filled by read methods may have less data than the buffer
     * capacity if the current tile is smaller than the expected tile size (e.g. last tile is truncated).
     *
     * @param  bank  the buffer where to fill remaining rows.
     */
    final void fillRemainingRows(final Buffer bank) {
        if (fillValue != null) {
            final int end = bank.limit();
            if (end != capacity) {
                Vector.create(bank.limit(capacity), ImageUtilities.isUnsignedType(model))
                      .fill(end, capacity, fillValue);
                bank.limit(capacity);
            }
        }
    }
}
