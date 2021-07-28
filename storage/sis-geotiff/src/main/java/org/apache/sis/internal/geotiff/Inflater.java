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

import java.io.IOException;
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
     */
    protected Inflater(final ChannelDataInput input, final int elementsPerRow, final int samplesPerElement, final int[] skipAfterElements) {
        ensureNonNull("input", input);
        ensureStrictlyPositive("elementsPerRow",    elementsPerRow);
        ensureStrictlyPositive("samplesPerElement", samplesPerElement);
        this.input = input;
        skipAfterChunks = skipAfterElements;
        if (skipAfterElements != null) {
            for (int i=0; i<skipAfterElements.length; i++) {
                ensurePositive("skipAfterElements", skipAfterElements[i]);
            }
            chunksPerRow    = elementsPerRow;
            samplesPerChunk = samplesPerElement;
        } else {
            chunksPerRow    = 1;
            samplesPerChunk = Math.multiplyExact(samplesPerElement, elementsPerRow);
        }
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
