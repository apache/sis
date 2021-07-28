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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * A pseudo-inflater which copy values unchanged.
 * This implementation is useful for handling more complex subsampling
 * than what {@link org.apache.sis.internal.storage.io.HyperRectangleReader} can handle.
 * It is also useful for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Uncompressed extends Inflater {
    /**
     * Stream position where to perform the next reading.
     */
    private long streamPosition;

    /**
     * Whether {@link #streamPosition} needs to be refreshed by
     * a call to {@link ChannelDataInput#getStreamPosition()}.
     */
    private boolean positionNeedsRefresh;

    /**
     * Number of bytes in a sample value.
     */
    private final int sampleSize;

    /**
     * For constructors in inner classes.
     */
    private Uncompressed(final ChannelDataInput input, final long start, final int elementsPerRow,
                         final int samplesPerElement, final int[] skipAfterElements, final int sampleSize)
    {
        super(input, elementsPerRow, samplesPerElement, skipAfterElements);
        this.streamPosition = start;
        this.sampleSize = sampleSize;
    }

    /**
     * Creates a new instance.
     *
     * @param  input   the source of data to decompress.
     * @param  start   stream position where to start reading.
     * @param  count   number of elements (usually pixels) per row. Must be strictly positive.
     * @param  size    number of sample values per element (usually pixel). Must be strictly positive.
     * @param  skips   number of sample values to skip between elements (pixels). May be empty or null.
     * @param  target  where to store sample values.
     * @return the inflater for the given targe type.
     * @throws IllegalArgumentException if the buffer type is not recognized.
     */
    public static Uncompressed create(final ChannelDataInput input, final long start,
            final int count, final int size, final int[] skips, final Buffer target)
    {
        if (target instanceof ByteBuffer) return new Bytes(input, start, count, size, skips, (ByteBuffer) target);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, Classes.getClass(target)));
    }

    /**
     * Reads a row of sample values and stores them in the target buffer.
     * Subclasses must override this method and invoke {@code super.uncompress()}
     * before to do the actual reading.
     */
    @Override
    public void uncompressRow() throws IOException {
        if (!positionNeedsRefresh) {
            positionNeedsRefresh = true;
            input.seek(streamPosition);
        }
    }

    /**
     * Skips the given amount of sample values without storing them.
     * The given value is in units of sample values, not in bytes.
     *
     * @param  n  number of uncompressed sample values to ignore.
     * @throws IOException if an error occurred while reading the input channel.
     */
    @Override
    public final void skip(final long n) throws IOException {
        if (n != 0) {
            if (positionNeedsRefresh) {
                positionNeedsRefresh = false;
                streamPosition = input.getStreamPosition();
            }
            streamPosition = Math.addExact(streamPosition, n * sampleSize);
        }
    }

    /**
     * Inflater for sample values stored as bytes.
     */
    private static final class Bytes extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final ByteBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Bytes(ChannelDataInput input, long start, int count, int size, int[] skips, ByteBuffer target) {
            super(input, start, count, size, skips, Byte.BYTES);
            this.target = target;
        }

        /** Reads and decompress a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int ip = 0;
            for (int i = chunksPerRow; --i > 0;) {      // (chunksPerRow - 1) iterations.
                int n = samplesPerChunk;
                do target.put(input.readByte());
                while (--n != 0);
                /*
                 * Following loop is executed only if there is subsampling on the X axis.
                 * We invoke `readByte()` in a loop instead of invoking `skip` because if
                 * the number of bytes to skip is small, this is more efficient.
                 */
                if (skipAfterChunks != null) {
                    for (n = skipAfterChunks[ip]; --n >= 0;) {
                        input.readByte();
                    }
                    if (++ip >= skipAfterChunks.length) ip = 0;
                }
            }
            /*
             * Read the last element that was not read in above `for` loop,
             * but without skipping `skipAfterElements` sample values after.
             * This is necessary for avoiding EOF if the last pixel to read
             * is in the last column of the tile.
             */
            int n = samplesPerChunk;
            do target.put(input.readByte());
            while (--n != 0);
        }
    }
}
