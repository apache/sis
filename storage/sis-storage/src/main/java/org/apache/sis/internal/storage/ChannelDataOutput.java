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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.io.Flushable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureBetween;

// Branch-dependent imports
import java.nio.channels.FileChannel;


/**
 * Provides convenience methods for working with a ({@link WritableByteChannel}, {@link ByteBuffer}) pair.
 * The channel and the buffer must be supplied by the caller. It is okay if they have already been used
 * before {@code ChannelDataOutput} creation.
 *
 * <div class="section">Encapsulation</div>
 * This class exposes publicly the {@linkplain #channel} and the {@linkplain #buffer buffer} because this class
 * is not expected to perform all possible data manipulations that we can do with the buffers. This class is only
 * a helper tool, which often needs to be completed by specialized operations performed directly on the buffer.
 * However, users are encouraged to transfer data from the buffer to the channel using only the methods provided
 * in this class if they want to keep the {@link #seek(long)} and {@link #getStreamPosition()} values accurate.
 *
 * <p>Since this class is only a helper tool, it does not "own" the channel and consequently does not provide
 * {@code close()} method. It is users responsibility to close the channel after usage.</p>
 *
 * <div class="section">Relationship with {@code DataOutput}</div>
 * This class API is compatibly with the {@link java.io.DataOutput} interface, so subclasses can implement that
 * interface if they wish. This class does not implement {@code DataOutput} itself because it is not needed for
 * SIS purposes.
 * However the {@link ChannelImageOutputStream} class implements the {@code DataOutput} interface, together with
 * the {@link javax.imageio.stream.ImageOutputStream} one, mostly for situations when inter-operability with
 * {@link javax.imageio} is needed.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class ChannelDataOutput extends ChannelData implements Flushable {
    /**
     * The channel where data are written.
     * This is supplied at construction time.
     */
    public final WritableByteChannel channel;

    /**
     * Creates a new data output for the given channel and using the given buffer.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel where data are written.
     * @param  buffer   The buffer where to put the data.
     * @throws IOException If an error occurred while creating the data output.
     */
    public ChannelDataOutput(final String filename, final WritableByteChannel channel, final ByteBuffer buffer)
            throws IOException
    {
        super(filename, channel, buffer);
        this.channel = channel;
        buffer.limit(0);
    }

    /**
     * Makes sure that the buffer can accept at least <var>n</var> more bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the {@linkplain ByteBuffer#capacity() buffer capacity}.
     *
     * <p>After this method call, the buffer {@linkplain ByteBuffer#limit() limit}
     * will be equal or greater than {@code position + n}.</p>
     *
     * @param  n The minimal number of additional bytes that the {@linkplain #buffer buffer} shall accept.
     * @throws IOException If an error occurred while writing to the channel.
     */
    private void ensureBufferAccepts(final int n) throws IOException {
        final int capacity = buffer.capacity();
        assert n >= 0 && n <= capacity : n;
        int after = buffer.position() + n;
        if (after > buffer.limit()) {
            /*
             * We will increase the limit for every new 'put' operation in order to maintain the number
             * of valid bytes in the buffer. If the new limit would exceed the buffer capacity, then we
             * need to write some bytes now.
             */
            if (after > capacity) {
                buffer.flip();
                do {
                    final int c = channel.write(buffer);
                    if (c == 0) {
                        onEmptyTransfer();
                    }
                    after -= c;
                } while (after > capacity);
                /*
                 * We wrote a sufficient amount of bytes - usually all of them, but not necessarily.
                 * If there is some unwritten bytes, move them the the beginning of the buffer.
                 */
                bufferOffset += buffer.position();
                buffer.compact();
                assert after >= buffer.position();
            }
            buffer.limit(after);
        }
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return The position of the stream.
     */
    @Override
    public long getStreamPosition() {
        long position = super.getStreamPosition();
        /*
         * ChannelDataOutput uses a different strategy than ChannelDataInput: if some bits were in process
         * of being written, the buffer position is set to the byte AFTER the byte containing the bits. We
         * need to apply a correction here for this strategy.
         */
        if (getBitOffset() != 0) {
            position--;
        }
        return position;
    }

    /**
     * Writes a single bit. This method uses only the rightmost bit of the given argument;
     * the upper 31 bits are ignored.
     *
     * @param bit The bit to write (rightmost bit).
     * @throws IOException If an error occurred while creating the data output.
     */
    public final void writeBit(final int bit) throws IOException {
        writeBits(bit, 1);
    }

    /**
     * Writes a sequence of bits. This method uses only the <code>numBits</code> rightmost bits;
     * other bits are ignored.
     *
     * @param  bits The bits to write (rightmost bits).
     * @param  numBits The number of bits to write.
     * @throws IOException If an error occurred while creating the data output.
     */
    public final void writeBits(long bits, int numBits) throws IOException {
        ensureBetween("numBits", 0, Long.SIZE, numBits);
        if (numBits != 0) {
            int bitOffset = getBitOffset();
            if (bitOffset != 0) {
                bits &= (1L << numBits) - 1; // Make sure that high-order bits are zero.
                final int r = numBits - (Byte.SIZE - bitOffset);
                /*
                 * 'r' is the number of bits than we can not store in the current byte. This value may be negative,
                 * which means that the current byte has space for more bits than what we have, in which case some
                 * room will still exist after this method call (i.e. the 'bitOffset' will still non-zero).
                 */
                final long mask;
                if (r >= 0) {
                    mask = bits >>> r;
                    bitOffset = 0;
                } else {
                    mask = bits << -r;
                    bitOffset += numBits;
                }
                numBits = r;
                assert (mask & ~0xFFL) == 0 : mask;
                final int p = buffer.position() - 1;
                buffer.put(p, (byte) (buffer.get(p) | mask));
            }
            /*
             * At this point, we are going to write only whole bytes.
             */
            while (numBits > 0) {
                numBits -= Byte.SIZE;
                final long part;
                if (numBits >= 0) {
                    part = bits >>> numBits;
                } else {
                    part = bits << -numBits;
                    bitOffset = Byte.SIZE + numBits;
                }
                writeByte((int) part);
            }
            setBitOffset(bitOffset);
        }
    }

    /**
     * Writes the 8 low-order bits of {@code v} to the stream.
     * The 24 high-order bits of {@code v} are ignored.
     * This method ensures that there is space for at least 1 byte in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(byte)}.
     *
     * @param  v byte to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeByte(final int v) throws IOException {
        ensureBufferAccepts(Byte.SIZE / Byte.SIZE);
        buffer.put((byte) v);
    }

    /**
     * Writes the 16 low-order bits of value to the stream.
     * The 16 high-order bits of {@code v} are ignored.
     * This method ensures that there is space for at least 2 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(short)}.
     *
     * @param  v short to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeShort(final int v) throws IOException {
        ensureBufferAccepts(Short.SIZE / Byte.SIZE);
        buffer.putShort((short) v);
    }

    /**
     * Writes char value (16 bits) into the steam.
     * This method ensures that there is space for at least 2 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(char)}.
     *
     * @param  v char to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeChar(final int v) throws IOException {
        ensureBufferAccepts(Character.SIZE / Byte.SIZE);
        buffer.putChar((char) v);
    }

    /**
     * Writes integer value (32 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(int)}.
     *
     * @param  v Integer to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeInt(final int v) throws IOException {
        ensureBufferAccepts(Integer.SIZE / Byte.SIZE);
        buffer.putInt(v);
    }

    /**
     * Writes long value (64 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(long)}.
     *
     * @param  v Long to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeLong(final long v) throws IOException {
        ensureBufferAccepts(Long.SIZE / Byte.SIZE);
        buffer.putLong(v);
    }

    /**
     * Writes float value (32 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(float)}.
     *
     * @param  v Float to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeFloat(final float v) throws IOException {
        ensureBufferAccepts(Float.SIZE / Byte.SIZE);
        buffer.putFloat(v);
    }

    /**
     * Writes double value (64 bits) into the steam.
     * This method ensures that there is space for at least 8 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(double)}.
     *
     * @param  v Double to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    public final void writeDouble(final double v) throws IOException {
        ensureBufferAccepts(Double.SIZE / Byte.SIZE);
        buffer.putDouble(v);
    }

    /**
     * Writes all bytes from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return write(src, 0, src.length);
     * }
     *
     * @param  src An array of bytes to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void write(final byte[] src) throws IOException {
        write(src, 0, src.length);
    }

    /**
     * Writes all shorts from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeShorts(src, 0, src.length);
     * }
     *
     * @param  src An array of shorts to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeShorts(final short[] src) throws IOException {
        writeShorts(src, 0, src.length);
    }

    /**
     * Writes all characters from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeChars(src, 0, src.length);
     * }
     *
     * @param  src An array of characters to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeChars(final char[] src) throws IOException {
        writeChars(src, 0, src.length);
    }

    /**
     * Writes all integers from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeInts(src, 0, src.length);
     * }
     *
     * @param  src An array of integers to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeInts(final int[] src) throws IOException {
        writeInts(src, 0, src.length);
    }

    /**
     * Writes all longs from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeLongs(src, 0, src.length);
     * }
     *
     * @param  src An array of longs to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeLongs(final long[] src) throws IOException {
        writeLongs(src, 0, src.length);
    }

    /**
     * Writes all floats from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeFloats(src, 0, src.length);
     * }
     *
     * @param  src An array of floats to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeFloats(final float[] src) throws IOException {
        writeFloats(src, 0, src.length);
    }

    /**
     * Writes all doubles from the given array into the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return writeDoubles(src, 0, src.length);
     * }
     *
     * @param  src An array of doubles to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeDoubles(final double[] src) throws IOException {
        writeDoubles(src, 0, src.length);
    }

    /**
     * Writes {@code length} bytes starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the bytes to write.
     * @param  offset Index within {@code src} of the first byte to write.
     * @param  length The number of bytes to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void write(final byte[] src, int offset, int length) throws IOException {
        if (length != 0) {
            do {
                final int n = Math.min(buffer.capacity(), length);
                ensureBufferAccepts(n);
                buffer.put(src, offset, n);
                offset += n;
                length -= n;
            } while (length != 0);
        } else {
            /*
             * Since the 'bitOffset' validity is determined by the position, if the position
             * did not changed, then we need to clear the 'bitOffset' flag manually.
             */
            clearBitOffset();
        }
    }

    /**
     * Helper class for the {@code writeFully(…)} methods,
     * in order to avoid duplicating almost identical code many times.
     */
    private abstract class ArrayWriter {
        /**
         * Creates a new buffer of the type required by the array to write.
         * This method is guaranteed to be invoked exactly once.
         */
        abstract Buffer createView();

        /**
         * Transfers the data from the array of primitive Java type known by the subclass into buffer
         * created by {@link #createView()}. This method may be invoked an arbitrary amount of time.
         */
        abstract void transfer(int offset, int length);

        /**
         * Skips the given amount of bytes in the buffer. It is caller responsibility to ensure
         * that there is enough bytes remaining in the buffer.
         *
         * @param nByte byte shift of buffer position.
         */
        private void skipInBuffer(int nByte) {
            buffer.position(buffer.position() + nByte);
        }

        /**
         * Writes {@code length} characters from the array to the stream.
         *
         * @param  dataSize The size of the Java primitive type which is the element of the array.
         * @param  offset   The starting position within {@code src} to write.
         * @param  length   The number of characters to write.
         * @throws IOException If an error occurred while writing the stream.
         */
        final void writeFully(final int dataSize, int offset, int length) throws IOException {
            clearBitOffset(); // Actually needed only if length == 0.
            ensureBufferAccepts(Math.min(length * dataSize, buffer.capacity()));
            final Buffer view = createView(); // Must be after ensureBufferAccept
            int n = Math.min(view.remaining(), length);
            transfer(offset, n);
            skipInBuffer(n * dataSize);
            while ((length -= n) != 0) {
                offset += n;
                ensureBufferAccepts(Math.min(length, view.capacity()) * dataSize);
                view.rewind().limit(buffer.remaining() / dataSize);
                transfer(offset, n = view.remaining());
                skipInBuffer(n * dataSize);
            }
        }
    }

    /**
     * Writes {@code length} chars starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the characters to write.
     * @param  offset Index within {@code src} of the first char to write.
     * @param  length The number of chars to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeChars(final char[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private CharBuffer view;
            @Override Buffer createView() {return view = buffer.asCharBuffer();}
            @Override void transfer(int offset, int n) {view.put(src, offset, n);}
        }.writeFully(Character.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Writes {@code length} shorts starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the shorts to write.
     * @param  offset Index within {@code src} of the first short to write.
     * @param  length The number of shorts to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeShorts(final short[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private ShortBuffer view;
            @Override Buffer createView() {return view = buffer.asShortBuffer();}
            @Override void transfer(int offset, int length) {view.put(src, offset, length);}
        }.writeFully(Short.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Writes {@code length} integers starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the integers to write.
     * @param  offset Index within {@code src} of the first integer to write.
     * @param  length The number of integers to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeInts(final int[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private IntBuffer view;
            @Override Buffer createView() {return view = buffer.asIntBuffer();}
            @Override void transfer(int offset, int n) {view.put(src, offset, n);}
        }.writeFully(Integer.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Writes {@code length} longs starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the longs to write.
     * @param  offset Index within {@code src} of the first long to write.
     * @param  length The number of longs to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeLongs(final long[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private LongBuffer view;
            @Override Buffer createView() {return view = buffer.asLongBuffer();}
            @Override void transfer(int offset, int n) {view.put(src, offset, n);}
        }.writeFully(Long.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Writes {@code length} floats starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the floats to write.
     * @param  offset Index within {@code src} of the first float to write.
     * @param  length The number of floats to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeFloats(final float[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private FloatBuffer view;
            @Override Buffer createView() {return view = buffer.asFloatBuffer();}
            @Override void transfer(int offset, int n) {view.put(src, offset, n);}
        }.writeFully(Float.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Writes {@code length} doubles starting at index {@code offset} from the given array.
     *
     * @param  src    An array containing the doubles to write.
     * @param  offset Index within {@code src} of the first double to write.
     * @param  length The number of doubles to write.
     * @throws IOException If an error occurred while writing the stream.
     */
    public final void writeDoubles(final double[] src, int offset, int length) throws IOException {
        new ArrayWriter() {
            private DoubleBuffer view;
            @Override Buffer createView() {return view = buffer.asDoubleBuffer();}
            @Override void transfer(int offset, int n) {view.put(src, offset, n);}
        }.writeFully(Double.SIZE / Byte.SIZE, offset, length);
    }

    /**
     * Fills the buffer with the zero values from its position up to the limit.
     * After this method call, the position is undetermined and shall be set to
     * a new value by the caller.
     */
    private void clear() {
        if (buffer.hasArray()) {
            final int offset = buffer.arrayOffset();
            Arrays.fill(buffer.array(), offset + buffer.position(), offset + buffer.limit(), (byte) 0);
        } else {
            while (buffer.hasRemaining()) {
                buffer.put((byte) 0);
            }
        }
    }

    /**
     * Moves to the given position in the stream, relative to the stream position at construction time.
     * If the given position is greater than the stream length, then the values of bytes between the
     * previous stream length and the given position are unspecified. The limit is unchanged.
     *
     * @param  position The position where to move.
     * @throws IOException If the stream can not be moved to the given position.
     */
    @Override
    public final void seek(final long position) throws IOException {
        long p = position - bufferOffset;
        if (p >= 0 && p <= buffer.limit()) {
            /*
             * Requested position is inside the current limits of the buffer.
             */
            buffer.position((int) p);
            clearBitOffset();
        } else if (channel instanceof FileChannel) {
            /*
             * Requested position is outside the current limits of the buffer,
             * but we can set the new position directly in the channel.
             */
            flush();
            ((FileChannel) channel).position(channelOffset + position);
            bufferOffset = position;
        } else if (p >= 0) {
            /*
             * Requested position is after the current buffer limit and
             * we can not seek, so we have to pad with some zero values.
             */
            p -= buffer.limit();
            flush(); // Also set the position to 0.
            if (p <= buffer.capacity()) {
                buffer.limit((int) p);
                clear();
                buffer.position((int) p);
            } else {
                buffer.clear();
                clear();
                do {
                    if (channel.write(buffer) == 0) {
                        onEmptyTransfer();
                    }
                    bufferOffset += buffer.position();
                    p -= buffer.position();
                    buffer.rewind();
                } while (p > buffer.capacity());
                buffer.limit((int) p).position((int) p);
            }
        } else {
            // We can not move position beyond the buffered part.
            throw new IOException(Errors.format(Errors.Keys.StreamIsForwardOnly_1, filename));
        }
    }

    /**
     * Flushes the {@link #buffer buffer} content to the channel.
     * This method does <strong>not</strong> flush the channel itself.
     *
     * @throws IOException If an error occurred while writing to the channel.
     */
    @Override
    public final void flush() throws IOException {
        buffer.rewind();
        writeFully();
        buffer.limit(0);
        clearBitOffset();
    }

    /**
     * Writes the buffer content up to the given position, then set the buffer position to the given value.
     * The {@linkplain ByteBuffer#limit() buffer limit} is unchanged, and the buffer offset is incremented
     * by the given value.
     */
    @Override
    final void flushAndSetPosition(final int position) throws IOException {
        final int limit = buffer.limit();
        buffer.rewind().limit(position);
        writeFully();
        buffer.limit(limit);
    }

    /**
     * Writes fully the buffer content from its position to its limit.
     * After this method call, the buffer position is equals to its limit.
     *
     * @throws IOException If an error occurred while writing to the channel.
     */
    private void writeFully() throws IOException {
        int n = buffer.remaining();
        bufferOffset += n;
        while (n != 0) {
            final int c = channel.write(buffer);
            if (c == 0) {
                onEmptyTransfer();
            }
            n -= c;
        }
        assert !buffer.hasRemaining() : buffer;
    }
}
