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

import java.io.IOException;
import java.nio.Buffer;
import java.awt.Point;
import java.awt.image.WritableRaster;
import org.apache.sis.internal.storage.TiledGridResource;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.coverage.j2d.RasterFactory;
import org.apache.sis.internal.geotiff.Compression;
import org.apache.sis.internal.geotiff.Predictor;
import org.apache.sis.internal.geotiff.Inflater;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.image.DataType;

import static java.lang.Math.toIntExact;
import static org.apache.sis.internal.jdk9.JDK9.multiplyFull;


/**
 * Raster data obtained from a compressed GeoTIFF file in the domain requested by user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CompressedSubset extends DataSubset {
    /**
     * The compression method.
     */
    private final Compression compression;

    /**
     * The mathematical operator that is applied to the image data before an encoding scheme is applied.
     */
    private final Predictor predictor;

    /**
     * Number of sample values to skip for moving to the next row of a tile in the GeoTIFF file.
     * This is not necessarily the same scanline stride than for the tiles created by this class.
     */
    private final long scanlineStride;

    /**
     * Number of sample values to skip before to read the first value of the first pixel in a row.
     * The first pixel is at column index 0; subsampling offset is not included in this calculation.
     */
    private final int beforeFirstBand;

    /**
     * Number of sample values to skip for reaching end-of-row after reading the last value of the
     * <em>first</em> pixel in a row. For computing the actual number of sample values to skip,
     * the number of sample values read or skipped before the last pixel must be subtracted.
     */
    private final int afterLastBand;

    /**
     * Number of sample values to skip after a chunk has been read, or {@code null} if none.
     * In this class, a "chunk" is a sample value or a complete pixel, depending on {@link #samplesPerChunk}
     * (note that the definition of "chunk" can be expanded to an entire row by the {@code Inflater} class).
     * The {@code skipAfterChunks} array is used in a cyclic way:
     *
     * <ul>
     *   <li>Skip {@code skipAfterChunks[0]} sample values between chunk 0 and chunk 1.</li>
     *   <li>Skip {@code skipAfterChunks[1]} sample values between chunk 1 and chunk 2.</li>
     *   <li><i>etc</i>. When we reach the array end, continue at {@code skipAfterChunks[0]}.</li>
     *   <li>When we start a new row, unconditionally restart at {@code skipAfterChunks[0]}.</li>
     * </ul>
     *
     * More generally, skip {@code skipAfterChunks[x % skipAfterChunks.length]} sample values
     * after chunk at the zero-based column index <var>x</var>.
     */
    private final int[] skipAfterChunks;

    /**
     * Number of sample values that compose a chunk (pixel or sample) in the GeoTIFF file.
     * The value of this field can be:
     *
     * <ul>
     *   <li>1 if a chunk is a sample value.</li>
     *   <li>{@link #sourcePixelStride} if a chunk is a full pixel.</li>
     *   <li>Any intermediate value if some optimizations have been applied,
     *       for example for taking advantage of consecutive indices in {@link #includedBands}.</li>
     * </ul>
     *
     * This value shall always be a divisor of {@link #targetPixelStride}.
     */
    private final int samplesPerChunk;

    /**
     * Creates a new data subset. All parameters should have been validated
     * by {@link ImageFileDirectory#validateMandatoryTags()} before this call.
     * This constructor should be invoked inside a synchronized block.
     *
     * @param  source       the resource which contain this {@code DataSubset}.
     * @param  subset       description of the {@code owner} subset to cover.
     * @param  rasters      potentially shared cache of rasters read by this {@code DataSubset}.
     * @param  compression  the compression method.
     * @param  predictor    the mathematical operator applied to image data before compression.
     * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
     */
    CompressedSubset(final DataCube source, final TiledGridResource.Subset subset,
                     final Compression compression, final Predictor predictor)
            throws DataStoreException
    {
        super(source, subset);
        this.compression  = compression;
        this.predictor    = predictor;
        scanlineStride    = multiplyFull(getTileSize(0), sourcePixelStride);
        final int between = sourcePixelStride * (getSubsampling(0) - 1);
        int afterLastBand = sourcePixelStride * (getTileSize(0) - 1);
        if (includedBands != null && sourcePixelStride > 1) {
            final int[] skips = new int[includedBands.length];
            final int m = skips.length - 1;
            int b = sourcePixelStride;
            for (int i=m; i >= 0; --i) {
                // Number of sample values to skip after each band.
                skips[i] = b - (b = includedBands[i]) - 1;
            }
            beforeFirstBand = b;                            // After above loop, `b` became the index of first band.
            afterLastBand  += skips[m];                     // Add trailing bands that were left unread.
            skips[m]       += between + beforeFirstBand;    // Add pixels skipped by subsampling and move to first band.
            /*
             * If there is more than one band and all of them are consecutive, then we can optimize a little bit
             * by reading "chunks" of the size of those consecutive bands instead of reading each band separately.
             *
             * Example: if the image has 5 bands and users requested bands 1, 2 and 3 (no empty space between bands),
             * then we can read those 3 bands as a "chunk" on 3 sample values instead of reading 3 chunks of 1 value.
             */
            if (m != 0 && startsWithZeros(skips, m)) {
                samplesPerChunk = includedBands.length;
                skipAfterChunks = new int[] {skips[m]};
            } else {
                samplesPerChunk = 1;
                skipAfterChunks = skips;
            }
        } else {
            /*
             * Case when all bands are read. If there is a subsampling, then `between` is the space (in number of
             * sample values) between two pixels. If that space is zero, it will be possible to read the whole row
             * in a single read operation, but that optimization is done in `Inflater` constructor.
             */
            skipAfterChunks = (between != 0) ? new int[] {between} : null;
            samplesPerChunk = sourcePixelStride;
            beforeFirstBand = 0;
        }
        this.afterLastBand = afterLastBand;
        /*
         * Invariant documented in Javadoc.
         * Calculation correctness depends on this condition.
         */
        assert targetPixelStride % samplesPerChunk == 0 : samplesPerChunk;
    }

    /**
     * Returns {@code true} if all array elements except the last one are zeros.
     * A {@code true} value means that all sample values in a pixel are consecutive.
     *
     * @param  m  {@code skipAfterChunks.length} - 1. Shall be greater than zero.
     */
    private static boolean startsWithZeros(final int[] skipAfterChunks, int m) {
        do if (skipAfterChunks[--m] != 0) return false;
        while (m != 0);
        return true;
    }

    /**
     * Computes the number of pixels to read in dimension <var>i</var>.
     * The arguments given to this method are the ones given to the {@code readSlice(…)} method.
     */
    private static int pixelCount(final long[] lower, final long[] upper, final int[] subsampling, final int i) {
        final int n = toIntExact((upper[i] - lower[i] - 1) / subsampling[i] + 1);
        assert (n > 0) : n;
        return n;
    }

    /**
     * Reads a two-dimensional slice of the data cube from the given input channel.
     *
     * @param  offsets      position in the channel where tile data begins, one value per bank.
     * @param  byteCounts   number of bytes for the compressed tile data, one value per bank.
     * @param  lower        (<var>x</var>, <var>y</var>) coordinates of the first pixel to read relative to the tile.
     * @param  upper        (<var>x</var>, <var>y</var>) coordinates after the last pixel to read relative to the tile.
     * @param  subsampling  (<var>sx</var>, <var>sy</var>) subsampling factors.
     * @param  location     pixel coordinates in the upper-left corner of the tile to return.
     * @return a single tile decoded from the GeoTIFF file.
     */
    @Override
    WritableRaster readSlice(final long[] offsets, final long[] byteCounts, final long[] lower, final long[] upper,
                             final int[] subsampling, final Point location) throws IOException, DataStoreException
    {
        final DataType type     = getDataType();
        final int  width        = pixelCount(lower, upper, subsampling, 0);
        final int  height       = pixelCount(lower, upper, subsampling, 1);
        final int  chunksPerRow = width * (targetPixelStride / samplesPerChunk);
        final int  betweenRows  = subsampling[1] - 1;
        final long head         = beforeFirstBand + sourcePixelStride * (lower[0]);
        final long tail         = afterLastBand   - sourcePixelStride * (lower[0] + (width-1)*subsampling[0]);
        /*
         * `head` and `tail` are the number of sample values to skip at the beginning and end of each row.
         * `betweenPixels` is the number of sample values to skip between each pixel, but the actual skips
         * are more complicated if only a subset of the bands are read. The actual number of sample values
         * to skip between "chunks" is detailed by `skipAfterChunks`.
         *
         * `pixelsPerElement` below is a factor for converting a count of pixels to a count of primitive elements
         * in the bank. The `pixelsPerElement` factor is usually 1, except when more than one pixel is packed in
         * each single primitive type (e.g. 8 bits per byte in bilevel image). The `head` needs to be a multiple
         * of `pixelsPerElement`; this restriction is documented in `Inflater.skip(long)` and should have been
         * verified by `TiledGridResource`.
         */
        final int pixelsPerElement = getPixelsPerElement();                 // Always ≥ 1 and usually = 1.
        assert (head % pixelsPerElement) == 0 : head;
        final int capacity = getBankCapacity(pixelsPerElement);
        final Buffer[] banks = new Buffer[numBanks];
        final ChannelDataInput input = input();
        for (int b=0; b<numBanks; b++) {
            /*
             * Prepare the object which will perform the actual decompression row-by-row,
             * optionally skipping chunks if a subsampling is applied.
             */
            final Buffer bank = RasterFactory.createBuffer(type, capacity);
            final Inflater algo = Inflater.create(compression, input, offsets[b], byteCounts[b],
                                    getTileSize(0), chunksPerRow, samplesPerChunk, skipAfterChunks,
                                    pixelsPerElement, bank);
            if (algo == null) {
                throw new DataStoreContentException(reader().resources().getString(
                        Resources.Keys.UnsupportedCompressionMethod_1, compression));
            }
            // TODO: Add predictor handling here.
            if (predictor != Predictor.NONE) {
                throw new DataStoreContentException(reader().resources().getString(
                        Resources.Keys.UnsupportedPredictor_1, predictor));
            }
            for (long y = lower[1]; --y >= 0;) {
                algo.skip(scanlineStride);          // `skip(…)` may round to next element boundary.
            }
            for (int y = height; --y > 0;) {        // (height - 1) iterations.
                algo.skip(head);
                algo.uncompressRow();
                algo.skip(tail);
                for (int j=betweenRows; --j>=0;) {
                    algo.skip(scanlineStride);
                }
            }
            algo.skip(head);                        // Last iteration without the trailing `skip(…)` calls.
            algo.uncompressRow();
            fillRemainingRows(bank.flip());
            banks[b] = bank;
        }
        return WritableRaster.createWritableRaster(model, RasterFactory.wrap(type, banks), location);
    }
}
