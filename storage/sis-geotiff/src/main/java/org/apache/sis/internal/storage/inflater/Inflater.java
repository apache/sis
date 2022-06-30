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
package org.apache.sis.internal.storage.inflater;

import java.util.Arrays;
import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;
import org.apache.sis.image.DataType;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.geotiff.Compression;
import org.apache.sis.internal.geotiff.Predictor;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Localized;

import static org.apache.sis.internal.util.Numerics.ceilDiv;


/**
 * Copies values from an input buffer of bytes to the destination image buffer,
 * potentially applying decompression, sub-region, subsampling and band subset
 * on-the-fly. Decompression is applied row-by-row.
 *
 * <p>If a decompression algorithm can handle all above-cited aspects directly,
 * it can extend this class directly. If it would be too complicated, an easier
 * approach is to extend {@link CompressionChannel} instead and wrap that inflater
 * in a {@link CopyFromBytes} subclass for managing the sub-region, subsampling
 * and band subset.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class Inflater implements Closeable {
    /**
     * The source of data to decompress.
     */
    protected final ChannelDataInput input;

    /**
     * Number of chunk per row, as a strictly positive integer.
     * See {@link #elementsPerChunk} for more details about what is a "chunk".
     */
    protected final int chunksPerRow;

    /**
     * Number of primitive elements per chunk, as a strictly positive integer.
     * An element is a byte, an integer (16, 32 or 64) bits or a floating point number.
     * An element is a sample value except in multi-pixels packed images.
     * A chunk can be:
     * <ul>
     *   <li>A sample value ({@code samplesPerChunk} = 1).</li>
     *   <li>A pixel with one sample value per band ({@code samplesPerChunk} = number of bands).</li>
     *   <li>Multi-pixels (e.g. bilevel image with 8 pixels per byte, which means 8 pixels per "chunk").</li>
     *   <li>A full row (optimization when it is possible to read the row in a single I/O method call).</li>
     * </ul>
     */
    protected final int elementsPerChunk;

    /**
     * Number of primitive elements to skip between chunks on the same row, or {@code null} if none.
     * If non-null then after reading the chunk at zero-based index <var>x</var>, inflater shall skip
     * {@code skipAfterChunks[x % skipAfterChunks.length]} bank elements before to read the next chunk.
     * The <var>x</var> index is reset to zero at the beginning of every new row.
     *
     * <p>This array may be shared by various objects; it is usually not cloned. Do not modify.</p>
     */
    protected final int[] skipAfterChunks;

    /**
     * Creates a new inflater instance.
     *
     * <h4>Note on sample definition</h4>
     * A "sample" is usually a primitive type such as a byte or a float, but may be a unit smaller than a byte
     * (e.g. 1 bit) if {@code pixelsPerElement} is greater than 1. In that case, the {@link #elementsPerChunk}
     * and {@link #skipAfterChunks} values will be divided by {@code pixelsPerElement}.
     *
     * @param  input             the source of data to decompress.
     * @param  chunksPerRow      number of chunks (usually pixels) per row in target image. Must be strictly positive.
     * @param  samplesPerChunk   number of sample values per chunk (sample or pixel). Must be strictly positive.
     * @param  skipAfterChunks   number of sample values to skip between chunks. May be empty or null.
     * @param  pixelsPerElement  number of pixels per primitive element. Always 1 except for multi-pixels packed images.
     * @param  maxChunkSize      maximal value (in number of samples) for the {@link #elementsPerChunk} field.
     */
    protected Inflater(final ChannelDataInput input, int chunksPerRow, int samplesPerChunk,
                       int[] skipAfterChunks, final int pixelsPerElement, final int maxChunkSize)
    {
        this.input = input;
        ArgumentChecks.ensureStrictlyPositive("chunksPerRow",     chunksPerRow);
        ArgumentChecks.ensureStrictlyPositive("samplesPerChunk",  samplesPerChunk);
        ArgumentChecks.ensureStrictlyPositive("pixelsPerElement", pixelsPerElement);
        if (skipAfterChunks != null) {
            if (pixelsPerElement != 1) {
                skipAfterChunks = skipAfterChunks.clone();
                for (int i=0; i<skipAfterChunks.length; i++) {
                    final int s = skipAfterChunks[i];
                    ArgumentChecks.ensurePositive("skipAfterChunks", s);
                    ArgumentChecks.ensureMultiple("skipAfterChunks", pixelsPerElement, s);
                    skipAfterChunks[i] /= pixelsPerElement;
                }
            }
        } else {
            samplesPerChunk = Math.multiplyExact(samplesPerChunk, chunksPerRow);
            chunksPerRow = 1;
            if (samplesPerChunk > maxChunkSize) {
                /*
                 * We want the smallest divisor n ≥ s/maxChunkSize. Note that `i` is guaranteed
                 * to be inside the array index range because the last array element is `s` and
                 * the value that we search can not be greater.
                 */
                final int[] divisors = MathFunctions.divisors(samplesPerChunk);
                int i = Arrays.binarySearch(divisors, Numerics.ceilDiv(samplesPerChunk, maxChunkSize));
                if (i < 0) i = ~i;      // No need for array bound check.
                /*
                 * Following loop iterates exactly once unless `pixelsPerElement` > 1.
                 * The intent is to ensure that `samplesPerChunk` is also a divisor of `pixelsPerElement`.
                 * If we can not find such value, current implementation will fail at `ensureDivisor` call.
                 *
                 * TODO: to avoid this problem, one possible approach could be to force `maxChunkSize` to be
                 * wide enough for a full row when `samplesPerChunk` > 1.
                 */
                do {
                    chunksPerRow = divisors[i];
                    if ((samplesPerChunk % (chunksPerRow * pixelsPerElement)) == 0) break;
                } while (++i < divisors.length);
                samplesPerChunk /= chunksPerRow;
            }
        }
        /*
         * Following condition can be relaxed when entire rows are read because image formats
         * are expected to pad the end of every rows. Each new row is aligned on a primitive
         * element boundary.
         */
        if (chunksPerRow != 1) {
            ArgumentChecks.ensureDivisor("pixelsPerElement", samplesPerChunk, pixelsPerElement);
        }
        this.elementsPerChunk = ceilDiv(samplesPerChunk, pixelsPerElement);
        this.skipAfterChunks  = skipAfterChunks;
        this.chunksPerRow     = chunksPerRow;
    }

    /**
     * Creates a new instance for the given compression.
     * If the given method is unrecognized, then this method returns {@code null}.
     *
     * <h4>Note on sample definition</h4>
     * A "sample" is usually a primitive type such as a byte or a float, but may be a unit smaller than a byte
     * (e.g. 1 bit) if {@code pixelsPerElement} is greater than 1. If that case, the {@link #elementsPerChunk}
     * and {@link #skipAfterChunks} values will be divided by {@code pixelsPerElement}.
     *
     * @param  caller             object calling this method (used in case an error message most be produced).
     * @param  input              the source of data to decompress.
     * @param  compression        the compression method.
     * @param  predictor          the mathematical operator to apply after decompression.
     * @param  sourcePixelStride  number of sample values per pixel in the source image.
     * @param  sourceWidth        number of pixels in a row of source image.
     * @param  chunksPerRow       number of chunks (usually pixels) per row in target image. Must be strictly positive.
     * @param  samplesPerChunk    number of sample values per chunk (sample or pixel). Must be strictly positive.
     * @param  skipAfterChunks    number of sample values to skip between chunks. May be empty or null.
     * @param  pixelsPerElement   number of pixels per primitive element. Always 1 except for multi-pixels packed images.
     * @param  dataType           primitive type used for storing data elements in the bank.
     * @return the inflater for the given targe type.
     * @throws IOException if an I/O operation was required and failed.
     * @throws UnsupportedEncodingException if the compression, predictor or data type is unsupported.
     *
     * @todo This is a very long argument list… This method is invoked only by {@code CompressedSubset}.
     *       Maybe we should inline this method in {@code CompressedSubset}. Before doing so, we should
     *       probably refactor for bringing {@code CompressedSubset} and related classes in this package,
     *       in a way that make them usable with other tiled formats than TIFF.
     */
    @SuppressWarnings("fallthrough")
    public static Inflater create(final Localized        caller,
                                  final ChannelDataInput input,
                                  final Compression      compression,
                                  final Predictor        predictor,
                                  final int              sourcePixelStride,
                                  final int              sourceWidth,
                                  final int              chunksPerRow,
                                  final int              samplesPerChunk,
                                  final int[]            skipAfterChunks,
                                  final int              pixelsPerElement,
                                  final DataType         dataType)
            throws IOException, UnsupportedEncodingException
    {
        ArgumentChecks.ensureNonNull("input", input);
        final CompressionChannel inflater;
        switch (compression) {
            case LZW:      inflater = new LZW     (input); break;
            case DEFLATE:  inflater = new ZIP     (input); break;
            case PACKBITS: inflater = new PackBits(input); break;
            case CCITTRLE: inflater = new CCITTRLE(input, sourceWidth); break;
            case NONE: {
                if (predictor == Predictor.NONE) {
                    return CopyFromBytes.create(input, dataType, chunksPerRow, samplesPerChunk, skipAfterChunks, pixelsPerElement);
                }
                throw unsupportedEncoding(caller, Resources.Keys.UnsupportedPredictor_1, predictor);
            }
            default: {
                throw unsupportedEncoding(caller, Resources.Keys.UnsupportedCompressionMethod_1, compression);
            }
        }
        final PixelChannel channel;
        switch (predictor) {
            case NONE: {
                channel = inflater;
                break;
            }
            case HORIZONTAL: {
                if (sourceWidth == 1) {
                    channel = inflater;     // Horizontal predictor is no-op if image width is 1 pixel.
                    break;
                }
                if (pixelsPerElement == 1) {
                    channel = HorizontalPredictor.create(inflater, dataType, sourcePixelStride, sourceWidth);
                    if (channel != null) break;
                }
                // Fallthrough.
            }
            default: {
                throw unsupportedEncoding(caller, Resources.Keys.UnsupportedPredictor_1, predictor);
            }
        }
        final int scanlineStride = Math.multiplyExact(sourceWidth, sourcePixelStride * dataType.bytes());
        return CopyFromBytes.create(inflater.createDataInput(channel, scanlineStride),
                dataType, chunksPerRow, samplesPerChunk, skipAfterChunks, pixelsPerElement);
    }

    /**
     * Returns the exception to throw for an unsupported compression or predictor.
     */
    private static UnsupportedEncodingException unsupportedEncoding(final Localized caller, final short key, final Enum<?> value) {
        return new UnsupportedEncodingException(Resources.forLocale(caller.getLocale()).getString(key, value));
    }

    /**
     * Sets the input and output and prepares this inflater for reading a new tile or band of a tile.
     *
     * @param  start      input stream position where to start reading.
     * @param  byteCount  number of bytes to read before decompression.
     * @param  bank       where to store sample values.
     * @throws IOException if an I/O operation was required and failed.
     */
    public void setInputOutput(final long start, final long byteCount, final Buffer bank) throws IOException {
        /*
         * If the input is a wrapper around a decompression algorithm (PackBits, CCITTRLE, etc),
         * we need to inform the wrapper about the new operation. The call to `setInputRegion(…)`
         * will perform a seek operation. As a consequence the buffer content become invalid and
         * must be emptied.
         */
        if (input.channel instanceof PixelChannel) {
            ((PixelChannel) input.channel).setInputRegion(start, byteCount);
            input.buffer.limit(0);
            input.setStreamPosition(start);         // Must be after above call to `limit(0)`.
        }
    }

    /**
     * Reads the given amount of sample values without storing them.
     * The given value is in units of sample values, not in bytes.
     *
     * <h4>Case of multi-pixels packed image</h4>
     * If there is more than one sample value per element, then this method may round (at implementation choice)
     * the stream position to the first element boundary after skipping <var>n</var> sample values. For example
     * a bilevel image packs 8 sample values per byte. Consequently a call to {@code skip(10)} may skip 2 bytes:
     * one byte for the first 8 bits, then 2 bits rounded to the next byte boundary.
     *
     * <p>The exact rounding mode depends on the compression algorithm. To avoid erratic behavior, callers shall
     * restrict <var>n</var> to multiples of {@code pixelsPerElement} before to read the first pixel in a row.
     * This restriction does not apply when skipping remaining data after the last pixels in a row, because the
     * next row will be realigned on an element boundary anyway. The way to perform this realignment depends on
     * the compression, which is the reason why the exact rounding mode may vary with compression algorithms.</p>
     *
     * <p>Restricting <var>n</var> to multiples of {@code pixelsPerElement} implies the following restrictions:</p>
     * <ul>
     *   <li>No subsampling on the <var>x</var> axis (however subsampling on other axes is still allowed).</li>
     *   <li>No band subset if interleaved sample model, because the effect is similar to subsampling.</li>
     *   <li>If {@link org.apache.sis.coverage.grid.GridDerivation} is used for computing the sub-region to read,
     *       it should have been configured with {@code chunkSize(pixelsPerElement)}
     *       (unless chunk size is already used for tile size).</li>
     * </ul>
     *
     * @param  n  number of uncompressed sample values to ignore.
     * @throws IOException if an error occurred while reading the input channel.
     */
    public abstract void skip(long n) throws IOException;

    /**
     * Reads a row of sample values and stores them in the target buffer.
     * This is not a complete row; caller may invoke {@link #skip(long)}
     * for ignoring leading and trailing sample values.
     *
     * @throws IOException if an error occurred while reading the input channel.
     */
    public abstract void uncompressRow() throws IOException;

    /**
     * Releases resources used by this inflater.
     */
    @Override
    public final void close() throws IOException {
        if (input.channel instanceof PixelChannel) {
            input.channel.close();
        }
    }
}
