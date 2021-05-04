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
import org.apache.sis.util.Localized;
import org.apache.sis.math.Vector;

import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static org.apache.sis.internal.jdk9.JDK9.multiplyFull;


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
        final Vector[] tileArrayInfo = source.getTileArrayInfo();
        this.source         = source;
        this.tileOffsets    = tileArrayInfo[0];
        this.tileByteCounts = tileArrayInfo[1];
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
     * Information about a tile to be read. A list of {@code Tile} is created and sorted by increasing offsets
     * before the read operation begins, in order to read tiles in the order they are written in the TIFF file.
     */
    private static final class Tile extends Snapshot implements Comparable<Tile> {
        /**
         * Value of {@link DataSubset#tileOffsets} at index {@link #indexInTileVector}.
         * This is the value that we want in increasing order.
         *
         * @see #compareTo(Tile)
         */
        final long byteOffset;

        /**
         * Stores information about a tile to be loaded.
         *
         * @param iterator    the iterator for which to create a snapshot of its current position.
         * @param byteOffset  value of {@code tileOffsets.longValue(indexInTileVector)}.
         */
        Tile(final AOI domain, final long byteOffset) {
            super(domain);
            this.byteOffset = byteOffset;
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
    protected final WritableRaster[] readTiles(final AOI iterator) throws IOException, DataStoreContentException {
        /*
         * Prepare an array for all tiles to be returned. Tiles that are already in memory will be stored
         * in this array directly. Other tiles will be declared in the `missings` array and loaded later.
         */
        final WritableRaster[] result = new WritableRaster[iterator.tileCountInQuery];
        final Tile[] missings = new Tile[iterator.tileCountInQuery];
        int numMissings = 0;
        synchronized (source.reader.store) {
            do {
                final WritableRaster tile = iterator.getCachedTile();
                if (tile != null) {
                    result[iterator.getIndexInResultArray()] = tile;
                } else {
                    // Tile not yet loaded. Add to a queue of tiles to load later.
                    missings[numMissings++] = new Tile(iterator, tileOffsets.longValue(iterator.getIndexInTileVector()));
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
            final long[] lower       = new long[BIDIMENSIONAL + 1];   // Coordinates of the first pixel to read relative to the tile.
            final long[] upper       = new long[BIDIMENSIONAL + 1];   // Coordinates after the last pixel to read relative to the tile.
            final int[]  subsampling = new int [BIDIMENSIONAL + 1];
            final Point  origin      = new Point();
            for (int i=0; i<numMissings; i++) {
                final Tile tile = missings[i];
                origin.x = tile.originX;
                origin.y = tile.originY;
                tile.getRegionInsideTile(lower, upper, subsampling, BIDIMENSIONAL);
                result[tile.indexInResultArray] = tile.cache(readSlice(
                        addExact(source.reader.origin, tile.byteOffset),
                        tileByteCounts.longValue(tile.indexInTileVector),
                        lower, upper, subsampling, origin));
            }
        }
        return result;
    }

    /**
     * Reads a two-dimensional slice of the data cube from the given input channel. This method is usually
     * invoked for reading the tile in full, in which case the {@code lower} argument is (0,0,0) and the
     * {@code upper} argument is the tile size. But those arguments may identify a smaller region if the
     * {@link DataSubset} contains only one (potentially large) tile.
     *
     * <p>The length of {@code lower}, {@code upper} and {@code subsampling} arrays shall be 3,
     * with the third dimension reserved for bands (this is not the <var>z</var> dimension).
     * This method may modify those arrays in-place.</p>
     *
     * <p>The default implementation in this base class assumes uncompressed data.
     * Subclasses must override for handling decompression.</p>
     *
     * @param  offset       position in the channel where tile data begins.
     * @param  byteCount    number of bytes for the compressed tile data.
     * @param  lower        (<var>x</var>, <var>y</var>, 0) coordinates of the first pixel to read relative to the tile.
     * @param  upper        (<var>x</var>, <var>y</var>, 0) coordinates after the last pixel to read relative to the tile.
     * @param  subsampling  (<var>sx</var>, <var>sy</var>, 1) subsampling factors.
     * @param  location     pixel coordinates in the upper-left corner of the tile to return.
     * @return image decoded from the GeoTIFF file.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreContentException if the sample model is not supported.
     * @throws RuntimeException if the Java2D image can not be created for another reason
     *         (too many exception types to list them all).
     */
    WritableRaster readSlice(final long offset, final long byteCount, final long[] lower, final long[] upper,
                             final int[] subsampling, final Point location) throws IOException, DataStoreContentException
    {
        final int  type   = model.getDataType();
        final long width  = subtractExact(upper[0], lower[0]);
        final long height = subtractExact(upper[1], lower[1]);
        final long length = multiplyExact(multiplyExact(width, height), DataBuffer.getDataTypeSize(type) / Byte.SIZE);
        if (length > byteCount) {
            // We may have less bytes to read if only a subregion is read.
            throw new DataStoreContentException(source.reader.resources().getString(
                    Resources.Keys.UnexpectedTileLength_2, length, byteCount));
        }
        final HyperRectangleReader hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(type), source.reader.input, offset);
        /*
         * "Banks" (in `java.awt.image.DataBuffer` sense) are synonymous to "bands" for planar image only.
         * Otherwise there is only one bank not matter the amount of bands. Each bank is read separately.
         */
        int numBanks = 1;
        int numInterleaved = model.getNumBands();
        if (model instanceof BandedSampleModel) {
            numBanks = numInterleaved;
            numInterleaved = 1;
        }
        final Buffer[] banks    = new Buffer[numBanks];
        final long[]   size     = new long[] {multiplyFull(numInterleaved, getTileSize(0)), getTileSize(1), numBanks};
        final int      capacity = multiplyExact(model.getWidth(), model.getHeight());
        for (int b=0; b<numBanks; b++) {
            lower[BIDIMENSIONAL] = b;
            upper[BIDIMENSIONAL] = b + 1;
            subsampling[BIDIMENSIONAL] = 1;
            final Region region = new Region(size, lower, upper, subsampling);
            final Buffer buffer = hr.readAsBuffer(region, capacity);
            /*
             * The buffer returned by `readAsBuffer(…)` may have less data than the buffer capacity
             * if the current tile is smaller than the expected tile size (e.g. truncated last tile).
             * Following code applies the fill value if it is different than the default value (zero).
             */
            if (fillValue != null) {
                final int end = buffer.limit();
                if (end != capacity) {
                    Vector.create(buffer.limit(capacity), ImageUtilities.isUnsignedType(model))
                          .fill(end, capacity, fillValue);
                }
            }
            banks[b] = buffer.limit(capacity);
        }
        final DataBuffer buffer = RasterFactory.wrap(DataType.forDataBufferType(type), banks);
        return WritableRaster.createWritableRaster(model, buffer, location);
    }
}
