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
package org.apache.sis.internal.geotiff;

import java.nio.Buffer;
import java.io.IOException;
import java.util.Arrays;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.internal.util.Numerics.ceilDiv;


/**
 * Copies values from an input buffer of bytes to the destination image buffer,
 * potentially applying decompression, sub-region, subsampling and band subset
 * on-the-fly. Decompression is applied row-by-row.
 *
 * <p>If a decompression algorithm can handle all above-cited aspects directly,
 * it can extend this class directly. If it would be too complicated, an easier
 * approach is to extend {@link InflaterChannel} instead and wrap that inflater
 * in a {@link CopyFromBytes} subclass for managing the sub-region, subsampling
 * and band subset.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Inflater {
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
     * (e.g. 1 bit) if {@code samplesPerElement} is greater than 1. In that case, the {@link #elementsPerChunk}
     * and {@link #skipAfterChunks} values will be divided by {@code samplesPerElement}.
     *
     * @param  input              the source of data to decompress.
     * @param  chunksPerRow       number of chunks (usually pixels) per row in target image. Must be strictly positive.
     * @param  samplesPerChunk    number of sample values per chunk (sample or pixel). Must be strictly positive.
     * @param  skipAfterChunks    number of sample values to skip between chunks. May be empty or null.
     * @param  samplesPerElement  number of sample values per primitive element. Always 1 except for multi-pixels packed images.
     * @param  maxChunkSize       maximal value (in number of samples) for the {@link #elementsPerChunk} field.
     */
    protected Inflater(final ChannelDataInput input, int chunksPerRow, int samplesPerChunk,
                       int[] skipAfterChunks, final int samplesPerElement, final int maxChunkSize)
    {
        this.input = input;
        ArgumentChecks.ensureStrictlyPositive("chunksPerRow",      chunksPerRow);
        ArgumentChecks.ensureStrictlyPositive("samplesPerChunk",   samplesPerChunk);
        ArgumentChecks.ensureStrictlyPositive("samplesPerElement", samplesPerElement);
        if (skipAfterChunks != null) {
            if (samplesPerElement != 1) {
                skipAfterChunks = skipAfterChunks.clone();
                for (int i=0; i<skipAfterChunks.length; i++) {
                    final int s = skipAfterChunks[i];
                    ArgumentChecks.ensurePositive("skipAfterChunks", s);
                    ArgumentChecks.ensureDivisor("samplesPerElement", s, samplesPerElement);
                    skipAfterChunks[i] /= samplesPerElement;
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
                 * Following loop iterates exactly once unless `samplesPerElement` > 1.
                 * The intend is to ensure that `samplesPerChunk` is also a divisor of `samplesPerElement`.
                 * If we can not find such value, current implementation will fail at `ensureDivisor` call.
                 *
                 * TODO: to avoid this problem, one possible approach could be to force `maxChunkSize` to be
                 * wide enough for a full row when `samplesPerChunk` > 1.
                 */
                do {
                    chunksPerRow = divisors[i];
                    if ((samplesPerChunk % (chunksPerRow * samplesPerElement)) == 0) break;
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
            ArgumentChecks.ensureDivisor("samplesPerElement", samplesPerChunk, samplesPerElement);
        }
        this.elementsPerChunk = ceilDiv(samplesPerChunk, samplesPerElement);
        this.skipAfterChunks  = skipAfterChunks;
        this.chunksPerRow     = chunksPerRow;
    }

    /**
     * Creates a new instance for the given compression.
     * If the given method is unrecognized, then this method returns {@code null}.
     *
     * <h4>Note on sample definition</h4>
     * A "sample" is usually a primitive type such as a byte or a float, but may be a unit smaller than a byte
     * (e.g. 1 bit) if {@code samplesPerElement} is greater than 1. If that case, the {@link #elementsPerChunk}
     * and {@link #skipAfterChunks} values will be divided by {@code samplesPerElement}.
     *
     * @param  compression        the compression method.
     * @param  input              the source of data to decompress.
     * @param  start              stream position where to start reading.
     * @param  byteCount          number of bytes to read before decompression.
     * @param  sourceWidth        number of pixels in a row of source image.
     * @param  chunksPerRow       number of chunks (usually pixels) per row in target image. Must be strictly positive.
     * @param  samplesPerChunk    number of sample values per chunk (sample or pixel). Must be strictly positive.
     * @param  skipAfterChunks    number of sample values to skip between chunks. May be empty or null.
     * @param  samplesPerElement  number of sample values per primitive element. Always 1 except for multi-pixels packed images.
     * @param  banks              where to store sample values.
     * @return the inflater for the given targe type, or {@code null} if the compression method is unknown.
     * @throws IOException if an I/O operation was required and failed.
     */
    public static Inflater create(final Compression compression,
            final ChannelDataInput input, final long start, final long byteCount, final int sourceWidth,
            final int chunksPerRow, final int samplesPerChunk, final int[] skipAfterChunks,
            final int samplesPerElement, final Buffer banks)
            throws IOException
    {
        ArgumentChecks.ensureNonNull("input", input);
        ArgumentChecks.ensureNonNull("banks", banks);
        final InflaterChannel inflated;
        switch (compression) {
            case NONE: {
                return CopyFromBytes.create(input, start, chunksPerRow, samplesPerChunk, skipAfterChunks, samplesPerElement, banks);
            }
            case PACKBITS: inflated = new PackBits(input, start, byteCount); break;
            case CCITTRLE: inflated = new CCITTRLE(input, start, byteCount, sourceWidth); break;
            default: return null;
        }
        return CopyFromBytes.create(inflated.createDataInput(), 0,
                chunksPerRow, samplesPerChunk, skipAfterChunks, samplesPerElement, banks);
    }

    /**
     * Reads the given amount of sample values without storing them.
     * The given value is in units of sample values, not in bytes.
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
}
