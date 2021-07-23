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
     * A chunk is a pixel, except if we can optimize by reading the whole row as a single chunk.
     */
    protected final int chunksPerRow;

    /**
     * Number of sample values per chunk, as a strictly positive integer.
     * A chunk is a pixel, except if we can optimize by reading the whole row as a single chunk.
     */
    protected final int samplesPerChunk;

    /**
     * Number of sample values to skip between pixels. Positive but often zero.
     */
    protected final int interpixels;

    /**
     * Creates a new instance.
     *
     * @param  input            the source of data to decompress.
     * @param  pixelsPerRow     number of pixels per row. Must be strictly positive.
     * @param  samplesPerPixel  number of sample values per pixel. Must be strictly positive.
     * @param  interpixels      number of sample values to skip between pixels. May be zero.
     */
    protected Inflater(final ChannelDataInput input, final int pixelsPerRow, final int samplesPerPixel, final int interpixels) {
        ensureStrictlyPositive("pixelsPerRow",    pixelsPerRow);
        ensureStrictlyPositive("samplesPerPixel", samplesPerPixel);
        ensurePositive        ("interpixels",     interpixels);
        ensureNonNull         ("input",           input);
        if (interpixels == 0) {
            chunksPerRow    = 1;
            samplesPerChunk = Math.multiplyExact(samplesPerPixel, pixelsPerRow);
        } else {
            chunksPerRow    = pixelsPerRow;
            samplesPerChunk = samplesPerPixel;
        }
        this.interpixels = interpixels;
        this.input       = input;
    }

    /**
     * Reads a row of sample values and stores them in the target buffer.
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
