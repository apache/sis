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
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
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
        if (target instanceof   ByteBuffer) return new Bytes  (input, start, count, size, skips,   (ByteBuffer) target);
        if (target instanceof  ShortBuffer) return new Shorts (input, start, count, size, skips,  (ShortBuffer) target);
        if (target instanceof    IntBuffer) return new Ints   (input, start, count, size, skips,    (IntBuffer) target);
        if (target instanceof   LongBuffer) return new Longs  (input, start, count, size, skips,   (LongBuffer) target);
        if (target instanceof  FloatBuffer) return new Floats (input, start, count, size, skips,  (FloatBuffer) target);
        if (target instanceof DoubleBuffer) return new Doubles(input, start, count, size, skips, (DoubleBuffer) target);
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
     * Skips the number of elements specified by the {@link #skipAfterChunks} array at the given index.
     * This method tries to move by incrementing the buffer position.
     *
     * <div class="note"><b>Design note:</b>
     * we do not use {@link ChannelDataInput#seek(long)} because the displacement is usually small.
     * Changing the buffer position is sufficient in the majority of cases. If not, then it should
     * be okay to fill the buffer with next data (instead of doing a seek operation) because there
     * is usually few remaining values to skip. Performance of this method is important, so we try
     * to avoid overhead.</div>
     *
     * @param  skipIndex  index in {@code skipAfterChunks} array.
     * @return new {@code skipIndex} value.
     */
    final int skipAfterChunk(int skipIndex) throws IOException {
        int n = skipAfterChunks[skipIndex] * sampleSize;
        while (n != 0) {
            final int p = input.buffer.position();
            final int s = Math.min(p + n, input.buffer.limit());
            input.buffer.position(s);
            if ((n -= (s - p)) == 0) break;
            input.ensureBufferContains(sampleSize);
        }
        return (++skipIndex < skipAfterChunks.length) ? skipIndex : 0;
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

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {      // (chunksPerRow - 1) iterations.
                int n = samplesPerChunk;
                input.ensureBufferContains(n);
                do target.put(input.buffer.get());      // Number of iterations should be low (often 1).
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            /*
             * Read the last element that was not read in above `for` loop,
             * but without skipping `skipAfterElements` sample values after.
             * This is necessary for avoiding EOF if the last pixel to read
             * is in the last column of the tile.
             */
            int n = samplesPerChunk;
            input.ensureBufferContains(n);
            do target.put(input.buffer.get());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as short integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Shorts extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final ShortBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Shorts(ChannelDataInput input, long start, int count, int size, int[] skips, ShortBuffer target) {
            super(input, start, count, size, skips, Short.BYTES);
            this.target = target;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = samplesPerChunk;
                input.ensureBufferContains(n * Short.BYTES);
                do target.put(input.buffer.getShort());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = samplesPerChunk;
            input.ensureBufferContains(n * Short.BYTES);
            do target.put(input.buffer.getShort());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as 32 bits integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Ints extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final IntBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Ints(ChannelDataInput input, long start, int count, int size, int[] skips, IntBuffer target) {
            super(input, start, count, size, skips, Integer.BYTES);
            this.target = target;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = samplesPerChunk;
                input.ensureBufferContains(n * Integer.BYTES);
                do target.put(input.buffer.getInt());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = samplesPerChunk;
            input.ensureBufferContains(n * Integer.BYTES);
            do target.put(input.buffer.getInt());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as long integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Longs extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final LongBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Longs(ChannelDataInput input, long start, int count, int size, int[] skips, LongBuffer target) {
            super(input, start, count, size, skips, Long.BYTES);
            this.target = target;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = samplesPerChunk;
                input.ensureBufferContains(n * Long.BYTES);
                do target.put(input.buffer.getLong());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = samplesPerChunk;
            input.ensureBufferContains(n * Long.BYTES);
            do target.put(input.buffer.getLong());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as single-precision floating point numbers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Floats extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final FloatBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Floats(ChannelDataInput input, long start, int count, int size, int[] skips, FloatBuffer target) {
            super(input, start, count, size, skips, Float.BYTES);
            this.target = target;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = samplesPerChunk;
                input.ensureBufferContains(n * Float.BYTES);
                do target.put(input.buffer.getFloat());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = samplesPerChunk;
            input.ensureBufferContains(n * Float.BYTES);
            do target.put(input.buffer.getFloat());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as double-precision floating point numbers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Doubles extends Uncompressed {
        /** Where to copy the values that we will read. */
        private final DoubleBuffer target;

        /** Creates a new inflater which will write in the given buffer. */
        Doubles(ChannelDataInput input, long start, int count, int size, int[] skips, DoubleBuffer target) {
            super(input, start, count, size, skips, Double.BYTES);
            this.target = target;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = samplesPerChunk;
                input.ensureBufferContains(n * Double.BYTES);
                do target.put(input.buffer.getDouble());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = samplesPerChunk;
            input.ensureBufferContains(n * Double.BYTES);
            do target.put(input.buffer.getDouble());
            while (--n != 0);
        }
    }
}
