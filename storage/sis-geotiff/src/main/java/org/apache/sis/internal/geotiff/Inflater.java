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

import static org.apache.sis.util.ArgumentChecks.ensureStrictlyPositive;
import static org.apache.sis.util.ArgumentChecks.ensurePositive;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Decompression algorithm.
 * Decompression is applied row-by-row.
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
     * See {@link #samplesPerChunk} for more details about what is a "chunk".
     */
    protected final int chunksPerRow;

    /**
     * Number of sample values per chunk, as a strictly positive integer.
     * A chunk can be:
     * <ul>
     *   <li>A sample value ({@code samplesPerChunk} = 1).</li>
     *   <li>A pixel with one sample value per band ({@code samplesPerChunk} = number of bands).</li>
     *   <li>A full row (optimization when it is possible to read the row in a single I/O method call).</li>
     * </ul>
     */
    protected final int samplesPerChunk;

    /**
     * Number of sample values to skip between chunks on the same row, or {@code null} if none.
     * If non-null then after reading the chunk at zero-based index <var>x</var>, inflater shall skip
     * {@code skipAfterChunks[x % skipAfterChunks.length]} sample values before to read the next chunk.
     * The <var>x</var> index is reset to zero at the beginning of every new row.
     */
    protected final int[] skipAfterChunks;

    /**
     * Creates a new instance.
     *
     * @param  input              the source of data to decompress.
     * @param  elementsPerRow     number of elements (usually pixels) per row. Must be strictly positive.
     * @param  samplesPerElement  number of sample values per element (usually pixel). Must be strictly positive.
     * @param  skipAfterElements  number of sample values to skip between elements (pixels). May be empty or null.
     * @param  maxChunkSize       maximal value (in number of elements) for the {@link #samplesPerChunk} field.
     */
    protected Inflater(final ChannelDataInput input, final int elementsPerRow, final int samplesPerElement,
                       final int[] skipAfterElements, final int maxChunkSize)
    {
        this.input = input;
        skipAfterChunks = skipAfterElements;
        if (skipAfterElements != null) {
            for (int i=0; i<skipAfterElements.length; i++) {
                ensurePositive("skipAfterElements", skipAfterElements[i]);
            }
            chunksPerRow    = elementsPerRow;
            samplesPerChunk = samplesPerElement;
        } else {
            int n = 1;
            int s = Math.multiplyExact(samplesPerElement, elementsPerRow);
            if (s > maxChunkSize) {
                /*
                 * We want the smallest divisor n ≥ s/maxChunkSize. Note that `i` is guaranteed
                 * to be inside the array index range because the last array element is `s` and
                 * the value that we search can not be greater.
                 */
                final int[] divisors = MathFunctions.divisors(s);
                int i = Arrays.binarySearch(divisors, Numerics.ceilDiv(s, maxChunkSize));
                if (i < 0) i = ~i;      // No need for array bound check.
                n = divisors[i];
                s /= n;
            }
            chunksPerRow    = n;
            samplesPerChunk = s;
        }
    }

    /**
     * Creates a new instance for the given compression.
     * If the given method is unrecognized, then this method returns {@code null}.
     *
     * @param  compression        the compression method.
     * @param  input              the source of data to decompress.
     * @param  start              stream position where to start reading.
     * @param  byteCount          number of bytes to read before decompression.
     * @param  elementsPerRow     number of elements (usually pixels) per row. Must be strictly positive.
     * @param  samplesPerElement  number of sample values per element (usually pixel). Must be strictly positive.
     * @param  skipAfterElements  number of sample values to skip between elements (pixels). May be empty or null.
     * @param  target             where to store sample values.
     * @return the inflater for the given targe type, or {@code null} if the compression method is unknown.
     * @throws IOException if an I/O operation was required and failed.
     */
    public static Inflater create(final Compression compression,
            final ChannelDataInput input, final long start, final long byteCount,
            final int elementsPerRow, final int samplesPerElement, final int[] skipAfterElements, final Buffer target)
            throws IOException
    {
        ensureNonNull("input", input);
        ensureStrictlyPositive("elementsPerRow",    elementsPerRow);
        ensureStrictlyPositive("samplesPerElement", samplesPerElement);
        final InflaterChannel inflated;
        switch (compression) {
            case NONE: {
                return CopyFromBytes.create(input, start, elementsPerRow, samplesPerElement, skipAfterElements, target);
            }
            case PACKBITS: {
                inflated = new PackBits(input, start, byteCount);
                break;
            }
            default: {
                return null;
            }
        }
        return CopyFromBytes.create(inflated.createDataInput(),
                0, elementsPerRow, samplesPerElement, skipAfterElements, target);
    }

    /**
     * Reads a row of sample values and stores them in the target buffer.
     * This is not a complete row; caller may invoke {@link #skip(long)}
     * for ignoring leading and trailing sample values.
     *
     * @throws IOException if an error occurred while reading the input channel.
     */
    public abstract void uncompressRow() throws IOException;

    /**
     * Reads the given amount of sample values without storing them.
     * The given value is in units of sample values, not in bytes.
     *
     * @param  n  number of uncompressed sample values to ignore.
     * @throws IOException if an error occurred while reading the input channel.
     */
    public abstract void skip(long n) throws IOException;
}
