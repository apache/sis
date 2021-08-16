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
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * A pseudo-inflater which copies values from a buffer of bytes to the destination image buffer.
 * When reading uncompressed TIFF images, the source buffer is the direct buffer used for I/O operations.
 * When reading compressed TIFF images, the source buffer is a temporary buffer where data segments are
 * uncompressed before to be copied to the destination image. This is useful when handling subsampling
 * on-the-fly at decompression time would be too difficult: implementers can decompress everything in
 * a temporary buffer and let this {@code CopyFromBytes} class do the subsampling.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class CopyFromBytes extends Inflater {
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
     * Number of bytes in a bank element. A bank element is usually a sample value.
     */
    private final int bytesPerElement;

    /**
     * Number of pixels per primitive element.
     * Always 1 except for multi-pixels packed images.
     *
     * <div class="note"><b>Note:</b>
     * this is "pixels per element", not "samples per element", because the value of this field shall be 1
     * in the {@link java.awt.image.SinglePixelPackedSampleModel} case (by contrast a "samples per element"
     * would have a value greater than 1). But this field can nevertheless be understood as a "samples per
     * element" value where only one band is considered at a time.</div>
     */
    private final int pixelsPerElement;

    /**
     * For constructors in inner classes.
     *
     * @param  input             the source of data to decompress.
     * @param  start             position in the input stream of the first byte to read.
     * @param  chunksPerRow      number of chunks (usually pixels) per row in target image. Must be strictly positive.
     * @param  samplesPerChunk   number of sample values per chunk (sample, pixel or row). Must be strictly positive.
     * @param  skipAfterChunks   number of sample values to skip between chunks. May be empty or null.
     * @param  pixelsPerElement  number of pixels per primitive element. Always 1 except for multi-pixels packed images.
     * @param  bytesPerElement   number of bytes in a bank element (a bank element is usually a sample value).
     */
    private CopyFromBytes(final ChannelDataInput input, final long start, final int chunksPerRow,
                          final int samplesPerChunk, final int[] skipAfterChunks,
                          final int pixelsPerElement, final int bytesPerElement)
    {
        super(input, chunksPerRow, samplesPerChunk, skipAfterChunks, pixelsPerElement, input.buffer.capacity() / bytesPerElement);
        this.streamPosition   = start;
        this.bytesPerElement  = bytesPerElement;
        this.pixelsPerElement = pixelsPerElement;
    }

    /**
     * Creates a new instance.
     *
     * @param  input    the source of data to decompress.
     * @param  start    stream position where to start reading.
     * @param  count    number of chunks (usually pixels) per row. Must be strictly positive.
     * @param  size     number of sample values per chunk (sample, pixel or row). Must be strictly positive.
     * @param  skips    number of sample values to skip between chunks. May be empty or null.
     * @param  divisor  factor by which to divide sample size values. Always ≥ 1 and usually = 1.
     * @param  banks    where to store sample values.
     * @return the inflater for the given targe type.
     * @throws UnsupportedEncodingException if the buffer type is not recognized.
     */
    public static CopyFromBytes create(final ChannelDataInput input, final long start,
            final int count, final int size, final int[] skips, final int divisor, final Buffer banks)
            throws UnsupportedEncodingException
    {
        if (banks instanceof   ByteBuffer) return new Bytes  (input, start, count, size, skips, divisor,   (ByteBuffer) banks);
        if (banks instanceof  ShortBuffer) return new Shorts (input, start, count, size, skips, divisor,  (ShortBuffer) banks);
        if (banks instanceof    IntBuffer) return new Ints   (input, start, count, size, skips, divisor,    (IntBuffer) banks);
        if (banks instanceof   LongBuffer) return new Longs  (input, start, count, size, skips, divisor,   (LongBuffer) banks);
        if (banks instanceof  FloatBuffer) return new Floats (input, start, count, size, skips, divisor,  (FloatBuffer) banks);
        if (banks instanceof DoubleBuffer) return new Doubles(input, start, count, size, skips, divisor, (DoubleBuffer) banks);
        throw new UnsupportedEncodingException(Errors.format(Errors.Keys.UnsupportedType_1, Classes.getClass(banks)));
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
     * <h4>Case of multi-pixels packed image</h4>
     * It is caller's responsibility to ensure that <var>n</var> is a multiple of {@link #pixelsPerElement}
     * if this method is not invoked for skipping all remaining values until end of row.
     * See {@link Inflater#skip(long)} for more information.
     *
     * @param  n  number of uncompressed sample values to ignore.
     * @throws IOException if an error occurred while reading the input channel.
     */
    @Override
    public final void skip(long n) throws IOException {
        if (n != 0) {
            if (positionNeedsRefresh) {
                positionNeedsRefresh = false;
                streamPosition = input.getStreamPosition();
            }
            /*
             * Convert number of sample values to number of elements, then to number of bytes.
             * The number of sample values to skip shall be a multiple of `pixelsPerElement`,
             * except when skipping all remaining values until end of row. We do not verify
             * because this method does not know when the row ends.
             */
            final boolean r = (n % pixelsPerElement) > 0;
            n /= pixelsPerElement; if (r) n++;              // Round after division to next element boundary.
            n *= bytesPerElement;                           // Must be multiplied only after above rounding.
            streamPosition = Math.addExact(streamPosition, n);
        }
    }

    /**
     * Skips the number of chunks specified by the {@link #skipAfterChunks} array at the given index.
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
        int n = skipAfterChunks[skipIndex] * bytesPerElement;
        while (n != 0) {
            final int p = input.buffer.position();
            final int s = Math.min(p + n, input.buffer.limit());
            input.buffer.position(s);
            if ((n -= (s - p)) == 0) break;
            input.ensureBufferContains(bytesPerElement);
        }
        return (++skipIndex < skipAfterChunks.length) ? skipIndex : 0;
    }

    /**
     * Inflater for sample values stored as bytes.
     */
    private static final class Bytes extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final ByteBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Bytes(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, ByteBuffer banks) {
            super(input, start, count, size, skips, divisor, Byte.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {      // (chunksPerRow - 1) iterations.
                int n = elementsPerChunk;
                input.ensureBufferContains(n);
                do banks.put(input.buffer.get());       // Number of iterations should be low (often 1).
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            /*
             * Read the last chunk that was not read in above `for` loop,
             * but without skipping `skipAfterChunks` sample values after.
             * This is necessary for avoiding EOF if the last pixel to read
             * is in the last column of the tile.
             */
            int n = elementsPerChunk;
            input.ensureBufferContains(n);
            do banks.put(input.buffer.get());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as short integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Shorts extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final ShortBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Shorts(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, ShortBuffer banks) {
            super(input, start, count, size, skips, divisor, Short.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = elementsPerChunk;
                input.ensureBufferContains(n * Short.BYTES);
                do banks.put(input.buffer.getShort());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = elementsPerChunk;
            input.ensureBufferContains(n * Short.BYTES);
            do banks.put(input.buffer.getShort());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as 32 bits integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Ints extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final IntBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Ints(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, IntBuffer banks) {
            super(input, start, count, size, skips, divisor, Integer.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = elementsPerChunk;
                input.ensureBufferContains(n * Integer.BYTES);
                do banks.put(input.buffer.getInt());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = elementsPerChunk;
            input.ensureBufferContains(n * Integer.BYTES);
            do banks.put(input.buffer.getInt());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as long integers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Longs extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final LongBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Longs(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, LongBuffer banks) {
            super(input, start, count, size, skips, divisor, Long.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = elementsPerChunk;
                input.ensureBufferContains(n * Long.BYTES);
                do banks.put(input.buffer.getLong());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = elementsPerChunk;
            input.ensureBufferContains(n * Long.BYTES);
            do banks.put(input.buffer.getLong());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as single-precision floating point numbers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Floats extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final FloatBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Floats(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, FloatBuffer banks) {
            super(input, start, count, size, skips, divisor, Float.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = elementsPerChunk;
                input.ensureBufferContains(n * Float.BYTES);
                do banks.put(input.buffer.getFloat());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = elementsPerChunk;
            input.ensureBufferContains(n * Float.BYTES);
            do banks.put(input.buffer.getFloat());
            while (--n != 0);
        }
    }

    /**
     * Inflater for sample values stored as double-precision floating point numbers.
     * This is a copy of {@link Bytes} implementation with only the type changed.
     */
    private static final class Doubles extends CopyFromBytes {
        /** Where to copy the values that we will read. */
        private final DoubleBuffer banks;

        /** Creates a new inflater which will write in the given buffer. */
        Doubles(ChannelDataInput input, long start, int count, int size, int[] skips, int divisor, DoubleBuffer banks) {
            super(input, start, count, size, skips, divisor, Double.BYTES);
            this.banks = banks;
        }

        /** Reads a row of sample values. */
        @Override public void uncompressRow() throws IOException {
            super.uncompressRow();
            int skipIndex = 0;
            for (int i = chunksPerRow; --i > 0;) {
                int n = elementsPerChunk;
                input.ensureBufferContains(n * Double.BYTES);
                do banks.put(input.buffer.getDouble());
                while (--n != 0);
                if (skipAfterChunks != null) {
                    skipIndex = skipAfterChunk(skipIndex);
                }
            }
            int n = elementsPerChunk;
            input.ensureBufferContains(n * Double.BYTES);
            do banks.put(input.buffer.getDouble());
            while (--n != 0);
        }
    }
}
