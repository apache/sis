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
package org.apache.sis.io.stream;

import java.util.Arrays;
import java.io.DataOutput;
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
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.storage.internal.Resources;
import static org.apache.sis.util.ArgumentChecks.ensureBetween;


/**
 * Provides convenience methods for working with a ({@link WritableByteChannel}, {@link ByteBuffer}) pair.
 * The channel and the buffer must be supplied by the caller. It is okay if they have already been used
 * before {@code ChannelDataOutput} creation.
 *
 * <h2>Encapsulation</h2>
 * This class exposes publicly the {@linkplain #channel} and the {@linkplain #buffer buffer} because this class
 * is not expected to perform all possible data manipulations that we can do with the buffers. This class is only
 * a helper tool, which often needs to be completed by specialized operations performed directly on the buffer.
 * However, users are encouraged to transfer data from the buffer to the channel using only the methods provided
 * in this class if they want to keep the {@link #seek(long)} and {@link #getStreamPosition()} values accurate.
 *
 * <p>Since this class is only a helper tool, it does not "own" the channel and consequently does not provide
 * {@code close()} method. It is users responsibility to close the channel after usage.</p>
 *
 * <h2>Interpretation of buffer position and limit</h2>
 * The buffer position is the position where to write the next byte.
 * It may be either a new byte appended to the channel, or byte overwriting an existing byte.
 * Those two case are differentiated by the buffer limit, which is the number of valid bytes in the buffer.
 *
 * <h2>Relationship with {@code ImageOutputStream}</h2>
 * This class API is compatibly with the {@link javax.imageio.stream.ImageOutputStream} interface, so subclasses
 * can implement that interface if they wish. This class does not implement {@code ImageOutputStream} because it
 * is not needed for SIS purposes.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ChannelDataOutput extends ChannelData implements DataOutput, Flushable {
    /**
     * The channel where data are written.
     * This is supplied at construction time.
     */
    public final WritableByteChannel channel;

    /**
     * Creates a new data output for the given channel and using the given buffer.
     *
     * @param  filename  a file identifier used only for formatting error message.
     * @param  channel   the channel where data are written.
     * @param  buffer    the buffer where to put the data.
     * @throws IOException if an error occurred while creating the data output.
     */
    public ChannelDataOutput(final String filename, final WritableByteChannel channel, final ByteBuffer buffer)
            throws IOException
    {
        super(filename, channel, buffer);
        this.channel = channel;
        buffer.limit(0);
    }

    /**
     * Creates a new data output which will write in the same channel as the given input.
     * The new instance will share the same channel and buffer than the given {@code input}.
     * Callers should not use the two {@code ChannelData} in same time for avoiding chaos.
     * Bytes will be written starting at the current position of the given input.
     *
     * <p>Callers <strong>must</strong> invoke {@link ChannelDataInput#yield(ChannelData)}
     * before the first use of this output. Example:</p>
     *
     * {@snippet lang="java":
     *     ChannelDataInput  input  = ...;
     *     ChannelDataOutput output = new ChannelDataOutput(input);
     *     input.yield(output)
     *     // ...some writing to `output` here...
     *     output.yield(input);
     *     // ...some reading from `input` here...
     *     input.yield(output)
     *     // ...some writing to `output` here...
     * }
     *
     * @param  input  the input to make writable.
     * @throws ClassCastException if the given input is not writable.
     *
     * @see #flush()
     * @see #yield(ChannelData)
     */
    public ChannelDataOutput(final ChannelDataInput input) {
        super(input, input.buffer, false);
        channel = (WritableByteChannel) input.channel;      // `ClassCastException` is part of the contract.
        // Do not invoke `input.yield(this)` because caller may want to do some more read operations first.
    }

    /**
     * {@return the wrapped channel where data are written}.
     * This is the {@link #channel} field value.
     *
     * @see #channel
     */
    @Override
    public final Channel channel() {
        return channel;
    }

    /**
     * Makes sure that the buffer can accept at least <var>n</var> more bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the {@linkplain ByteBuffer#capacity() buffer capacity}.
     *
     * <p>After this method call, the buffer {@linkplain ByteBuffer#limit() limit}
     * will be equal or greater than {@code position + n}. This limit is the number
     * of valid bytes in the buffer, i.e. bytes that already exist in the channel.
     * If the caller is appending new bytes and does not use all the space specified
     * to this method, then the caller should adjust the limit after writing.</p>
     *
     * @param  n  the minimal number of additional bytes that the {@linkplain #buffer buffer} shall accept.
     * @throws IOException if an error occurred while writing to the channel.
     */
    public final void ensureBufferAccepts(final int n) throws IOException {
        assert n >= 0 && n <= buffer.capacity() : n;
        int after = buffer.position() + n;
        if (after > buffer.limit()) {
            /*
             * We will increase the limit for every new `put` operation in order to maintain the number
             * of valid bytes in the buffer. If the new limit would exceed the buffer capacity, then we
             * need to write some bytes now.
             */
            if (after > buffer.capacity()) {
                buffer.flip();
                do {
                    final int c = channel.write(buffer);
                    if (c == 0) {
                        onEmptyTransfer();
                    }
                    after -= c;
                } while (after > buffer.capacity());
                /*
                 * We wrote a sufficient number of bytes - usually all of them, but not necessarily.
                 * If there is some unwritten bytes, move them the beginning of the buffer.
                 */
                moveBufferForward(buffer.position());
                buffer.compact();
                assert after >= buffer.position() : after;
            }
            buffer.limit(after);
        }
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     */
    @Override
    public final long getStreamPosition() {
        long position = position();
        /*
         * ChannelDataOutput uses a different strategy than ChannelDataInput: if some bits were in process
         * of being written, the buffer position is set to the byte AFTER the byte containing the bits.
         * We need to apply a correction here for this strategy.
         */
        if (getBitOffset() != 0) {
            position--;
        }
        return position;
    }

    /**
     * Moves the stream position to the next byte boundary.
     */
    @Override
    public final void skipRemainingBits() {
        // See the comment in `getStreamPosition()` method body.
        bitPosition = 0;
    }

    /**
     * Writes a single bit. This method uses only the rightmost bit of the given argument;
     * the upper 31 bits are ignored.
     *
     * @param  bit  the bit to write (rightmost bit).
     * @throws IOException if an error occurred while creating the data output.
     *
     * @see #writeBoolean(boolean)
     */
    public final void writeBit(final int bit) throws IOException {
        writeBits(bit, 1);
    }

    /**
     * Writes a sequence of bits. This method uses only the <code>numBits</code> rightmost bits;
     * other bits are ignored.
     *
     * @param  bits     the bits to write (rightmost bits).
     * @param  numBits  the number of bits to write.
     * @throws IOException if an error occurred while creating the data output.
     */
    public final void writeBits(long bits, int numBits) throws IOException {
        ensureBetween("numBits", 0, Long.SIZE, numBits);
        if (numBits != 0) {
            int bitOffset = getBitOffset();
            if (bitOffset != 0) {
                bits &= Numerics.bitmask(numBits) - 1;                  // Make sure that high-order bits are zero.
                final int r = numBits - (Byte.SIZE - bitOffset);
                /*
                 * `r` is the number of bits that we cannot store in the current byte. This value may be negative,
                 * which means that the current byte has space for more bits than what we have, in which case some
                 * room will still exist after this method call (i.e. the `bitOffset` will still non-zero).
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
     * Writes boolean value (8 bits) into the steam. This method delegates to {@linkplain #writeByte(int)}.
     * If boolean {@code v} is {@code true} the byte value 1 is written whereas if boolean is {@code false}
     * zero is written.
     *
     * <p>For writing a single bit, see {@link #writeBit(int)} instead.
     *
     * @param  v  boolean to be written.
     * @throws IOException if some I/O exception occurs during writing.
     *
     * @see #writeBit(int)
     */
    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        writeByte(v ? 1 : 0);
    }

    /**
     * Writes a single byte to the stream at the current position.
     * The 24 high-order bits of {@code v} are ignored.
     *
     * @param  v  an integer whose lower 8 bits are to be written.
     * @throws IOException if some I/O exception occurs during writing.
     *
     * @deprecated Prefer {@link #writeByte(int)} for readability.
     */
    @Override
    @Deprecated
    public final void write(final int v) throws IOException {
        writeByte(v);
    }

    /**
     * Writes the 8 low-order bits of {@code v} to the stream.
     * The 24 high-order bits of {@code v} are ignored.
     * This method ensures that there is space for at least 1 byte in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#put(byte)}.
     *
     * @param  value  byte to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeByte(final int value) throws IOException {
        ensureBufferAccepts(Byte.BYTES);
        buffer.put((byte) value);
    }

    /**
     * Writes the 16 low-order bits of value to the stream.
     * The 16 high-order bits of {@code v} are ignored.
     * This method ensures that there is space for at least 2 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putShort(short)}.
     *
     * @param  value  short integer to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeShort(final int value) throws IOException {
        ensureBufferAccepts(Short.BYTES);
        buffer.putShort((short) value);
    }

    /**
     * Writes char value (16 bits) into the steam.
     * This method ensures that there is space for at least 2 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putChar(char)}.
     *
     * @param  value  character to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeChar(final int value) throws IOException {
        ensureBufferAccepts(Character.BYTES);
        buffer.putChar((char) value);
    }

    /**
     * Writes integer value (32 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putInt(int)}.
     *
     * @param  value  integer to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeInt(final int value) throws IOException {
        ensureBufferAccepts(Integer.BYTES);
        buffer.putInt(value);
    }

    /**
     * Writes long value (64 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putLong(long)}.
     *
     * @param  value  long integer to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeLong(final long value) throws IOException {
        ensureBufferAccepts(Long.BYTES);
        buffer.putLong(value);
    }

    /**
     * Writes float value (32 bits) into the steam.
     * This method ensures that there is space for at least 4 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putFloat(float)}.
     *
     * @param  value floating point value to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeFloat(final float value) throws IOException {
        ensureBufferAccepts(Float.BYTES);
        buffer.putFloat(value);
    }

    /**
     * Writes double value (64 bits) into the steam.
     * This method ensures that there is space for at least 8 bytes in the buffer,
     * (writing previous bytes into the channel if necessary), then delegates to {@link ByteBuffer#putDouble(double)}.
     *
     * @param  value  double precision floating point value to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeDouble(final double value) throws IOException {
        ensureBufferAccepts(Double.BYTES);
        buffer.putDouble(value);
    }

    /**
     * Writes all bytes from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return write(src, 0, src.length);
     *     }
     *
     * @param  src  an array of bytes to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    @Override
    public final void write(final byte[] src) throws IOException {
        write(src, 0, src.length);
    }

    /**
     * Writes all shorts from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeShorts(src, 0, src.length);
     *     }
     *
     * @param  src  an array of shorts to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeShorts(final short[] src) throws IOException {
        writeShorts(src, 0, src.length);
    }

    /**
     * Writes all characters from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeChars(src, 0, src.length);
     *     }
     *
     * @param  src  an array of characters to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeChars(final char[] src) throws IOException {
        writeChars(src, 0, src.length);
    }

    /**
     * Writes all integers from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeInts(src, 0, src.length);
     *     }
     *
     * @param  src  an array of integers to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeInts(final int[] src) throws IOException {
        writeInts(src, 0, src.length);
    }

    /**
     * Writes all longs from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeLongs(src, 0, src.length);
     *     }
     *
     * @param  src  an array of longs to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeLongs(final long[] src) throws IOException {
        writeLongs(src, 0, src.length);
    }

    /**
     * Writes all floats from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeFloats(src, 0, src.length);
     *     }
     *
     * @param  src  an array of floats to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeFloats(final float[] src) throws IOException {
        writeFloats(src, 0, src.length);
    }

    /**
     * Writes all doubles from the given array into the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return writeDoubles(src, 0, src.length);
     *     }
     *
     * @param  src  an array of doubles to be written into stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeDoubles(final double[] src) throws IOException {
        writeDoubles(src, 0, src.length);
    }

    /**
     * Writes {@code length} bytes starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the bytes to write.
     * @param  offset  index within {@code src} of the first byte to write.
     * @param  length  the number of bytes to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    @Override
    public final void write(final byte[] src, int offset, int length) throws IOException {
        /*
         * Since the `bitOffset` validity is determined by the position, if the position
         * did not changed, then we need to clear the `bitOffset` flag manually.
         */
        skipRemainingBits();
        if (length != 0) {
            do {
                final int n = Math.min(buffer.capacity(), length);
                ensureBufferAccepts(n);
                buffer.put(src, offset, n);
                offset += n;
                length -= n;
            } while (length != 0);
        }
    }

    /**
     * Helper class for the {@code writeFully(…)} methods,
     * in order to avoid duplicating almost identical code many times.
     */
    private abstract class ArrayWriter {
        /**
         * Creates a new writer.
         */
        ArrayWriter() {
        }

        /**
         * Creates a new buffer of the type required by the array to write.
         * This method is guaranteed to be invoked exactly once.
         */
        abstract Buffer createView();

        /**
         * Transfers the data from the array of primitive Java type known by the subclass into buffer
         * created by {@link #createView()}. This method may be invoked an arbitrary number of times.
         */
        abstract void transfer(int offset, int length);

        /**
         * Skips the given number of bytes in the buffer. It is caller responsibility to ensure
         * that there is enough bytes remaining in the buffer.
         *
         * @param  nByte  byte shift of buffer position.
         */
        private void skipInBuffer(int nByte) {
            buffer.position(buffer.position() + nByte);
        }

        /**
         * Writes {@code length} elements from the array to the stream.
         *
         * @param  dataSize  the size of the Java primitive type which is the element of the array.
         * @param  offset    the starting position within {@code src} to write.
         * @param  length    the number of elements to write.
         * @throws IOException if an error occurred while writing the stream.
         */
        final void writeFully(final int dataSize, int offset, int length) throws IOException {
            skipRemainingBits();                        // Actually needed only if `length` = 0.
            ensureBufferAccepts(Math.min(length * dataSize, buffer.capacity()));
            final Buffer view = createView();           // Must be after `ensureBufferAccept(…)`
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
     * @param  src     an array containing the characters to write.
     * @param  offset  index within {@code src} of the first char to write.
     * @param  length  the number of chars to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeChars(final char[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private CharBuffer view;
            @Override Buffer createView() {return view = buffer.asCharBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Character.BYTES, offset, length);
    }

    /**
     * Writes {@code length} shorts starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the shorts to write.
     * @param  offset  index within {@code src} of the first short to write.
     * @param  length  the number of shorts to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeShorts(final short[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private ShortBuffer view;
            @Override Buffer createView() {return view = buffer.asShortBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Short.BYTES, offset, length);
    }

    /**
     * Writes {@code length} integers starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the integers to write.
     * @param  offset  index within {@code src} of the first integer to write.
     * @param  length  the number of integers to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeInts(final int[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private IntBuffer view;
            @Override Buffer createView() {return view = buffer.asIntBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Integer.BYTES, offset, length);
    }

    /**
     * Writes {@code length} longs starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the longs to write.
     * @param  offset  index within {@code src} of the first long to write.
     * @param  length  the number of longs to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeLongs(final long[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private LongBuffer view;
            @Override Buffer createView() {return view = buffer.asLongBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Long.BYTES, offset, length);
    }

    /**
     * Writes {@code length} floats starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the floats to write.
     * @param  offset  index within {@code src} of the first float to write.
     * @param  length  the number of floats to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeFloats(final float[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private FloatBuffer view;
            @Override Buffer createView() {return view = buffer.asFloatBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Float.BYTES, offset, length);
    }

    /**
     * Writes {@code length} doubles starting at index {@code offset} from the given array.
     *
     * @param  src     an array containing the doubles to write.
     * @param  offset  index within {@code src} of the first double to write.
     * @param  length  the number of doubles to write.
     * @throws IOException if an error occurred while writing the stream.
     */
    public final void writeDoubles(final double[] src, final int offset, final int length) throws IOException {
        new ArrayWriter() {
            private DoubleBuffer view;
            @Override Buffer createView() {return view = buffer.asDoubleBuffer();}
            @Override void transfer(int start, int n) {view.put(src, start, n);}
        }.writeFully(Double.BYTES, offset, length);
    }

    /**
     * Writes the lower-order byte of each character. The high-order eight bits of each character
     * in the string are ignored - this method does <strong>not</strong> applies any encoding.
     *
     * <p>This method is provided because required by the {@link DataOutput} interface,
     * but its usage should generally be avoided.</p>
     *
     * @param  ascii  the string to be written.
     * @throws IOException if an error occurred while writing the stream.
     */
    @Override
    public void writeBytes(final String ascii) throws IOException {
        final byte[] data = new byte[ascii.length()];
        for (int i=0; i<data.length; i++) {
            data[i] = (byte) ascii.charAt(i);
        }
        write(data);
    }

    /**
     * Writes all characters from the given text into the stream.
     *
     * @param  text  the characters to be written into the stream.
     * @throws IOException if an error occurred while writing the stream.
     */
    @Override
    public final void writeChars(final String text) throws IOException {
        writeChars(text.toCharArray());
    }

    /**
     * Writes two bytes of length information to the output stream, followed by the modified UTF-8 representation
     * of every character in the {@code str} string. Each character is converted to a group of one, two, or three
     * bytes, depending on the character code point value.
     *
     * @param  s  the string to be written.
     * @throws IOException if an error occurred while writing the stream.
     */
    @Override
    public void writeUTF(final String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        final int length = data.length;
        if (length > Short.MAX_VALUE) {
            throw new IllegalArgumentException(Resources.format(
                    Resources.Keys.ExcessiveStringSize_3, filename, Short.MAX_VALUE, length));
        }
        ensureBufferAccepts(Short.BYTES);
        buffer.put((byte) (length >>> Byte.SIZE));      // Write using ByteOrder.BIG_ENDIAN.
        buffer.put((byte) length);
        write(data);
    }

    /**
     * Repeats the same byte many times.
     * This method can be used for filling a region of the output stream.
     *
     * @param  count  number of bytes to write.
     * @param  value  the byte to repeat the given number of times.
     */
    public final void repeat(long count, final byte value) throws IOException {
        skipRemainingBits();        // Actually needed only if `count` = 0.
        if (count > 0) {
            /*
             * Fill the buffer with the specified value. The filling is done only once,
             * even if the number of bytes to write is greater than the buffer capacity.
             * We can do that because the same buffer content can be reused during each
             * `WritableByteChannel.write(ByteBuffer)` call.
             */
            long n = Math.min(count, buffer.capacity());
            ensureBufferAccepts((int) n);
            if (buffer.hasArray()) {
                final int offset = buffer.arrayOffset();
                final int lower  = buffer.position();
                final int upper  = lower + (int) n;
                Arrays.fill(buffer.array(), offset + lower, offset + upper, value);
                buffer.position(upper);
            } else {
                for (int i = (int) n; --i >= 0;) {
                    buffer.put(value);
                }
            }
            /*
             * If the number of bytes to write is greater than the capacity, we need to flush the buffer.
             * Not necessarily fully however, because maybe there is not so much extra bytes to write.
             */
            if ((count -= n) > 0) {                                 // What remains, not counting what we put in the buffer.
                assert buffer.position() == buffer.capacity();      // Because of `ensureBufferAccepts(capacity)`.
                int c;
                do {
                    c = channel.write(buffer.rewind());
                    moveBufferForward(c);
                    if (c == 0) {
                        onEmptyTransfer();
                    }
                    c = buffer.remaining();                         // Remaining data that were not written.
                    n = Math.min(count, buffer.capacity() - c);     // Number of bytes to append in the buffer.
                } while ((count -= n) > 0);                         // What remains, not counting what we put in the buffer.
                buffer.limit(c + (int) n);                          // Set also the position to the limit.
            }
        }
    }

    /**
     * Truncates the stream to the given position. If the given size is greater than the current stream size,
     * then this method does nothing. Otherwise this method set the stream size to the given size and, if the
     * current position is greater than the new size, also set the stream to that position.
     *
     * @param  size  the position where to truncate.
     * @throws IOException if the stream cannot be truncated to the given position.
     */
    public final void truncate(final long size) throws IOException {
        if (channel instanceof SeekableByteChannel) {
            // Unconditional truncate because more data may exist after current position.
            ((SeekableByteChannel) channel).truncate(toSeekableByteChannelPosition(size));
            if (size <= bufferOffset) {
                bufferOffset = size;
                bitPosition = 0;
                buffer.limit(0);
                return;
            }
        }
        final long p = Math.subtractExact(size, bufferOffset);
        if (p >= 0 && p <= buffer.limit()) {
            buffer.limit((int) p);
            bitPosition = 0;
        } else {
            throw new IOException(Resources.format(Resources.Keys.StreamIsForwardOnly_1, filename));
        }
    }

    /**
     * Moves to the given position in this stream.
     * If the given position is greater than the stream length, then the values of all bytes between
     * the previous stream length and the given position are unspecified. The limit is unchanged.
     *
     * @param  position  the position where to move.
     * @throws IOException if the stream cannot be moved to the given position.
     */
    @Override
    public final void seek(final long position) throws IOException {
        long p = Math.subtractExact(position, bufferOffset);
        if (p >= 0 && p <= buffer.limit()) {
            /*
             * Requested position is inside the current limits of the buffer.
             */
            buffer.position((int) p);
            bitPosition = 0;
        } else if (channel instanceof SeekableByteChannel) {
            /*
             * Requested position is outside the current limits of the buffer,
             * but we can set the new position directly in the channel.
             */
            flush();
            ((SeekableByteChannel) channel).position(toSeekableByteChannelPosition(position));
            bufferOffset = position;
        } else if ((p -= buffer.position()) >= 0) {
            /*
             * Requested position is after the current buffer limit and
             * we cannot seek, so we have to pad with some zero values.
             */
            repeat(p, (byte) 0);
        } else {
            // We cannot move position beyond the buffered part.
            throw new IOException(Resources.format(Resources.Keys.StreamIsForwardOnly_1, filename));
        }
        assert position() == position : position;
    }

    /**
     * Flushes the buffer content to the channel, from buffer beginning to buffer limit.
     * If the buffer position is not already at the buffer limit, the position is moved.
     * The buffer is empty after this method call, i.e. the limit is zero.
     * This method does <strong>not</strong> flush the channel itself.
     *
     * @throws IOException if an error occurred while writing to the channel.
     */
    @Override
    public final void flush() throws IOException {
        skipRemainingBits();
        writeFully();
        bufferOffset = position();
        buffer.limit(0);
    }

    /**
     * Writes the given number of bytes from the buffer.
     * This is invoked for making room for more bytes.
     *
     * @param  count  number of bytes to write, between 1 and buffer limit.
     * @throws IOException if an error occurred while writing the bytes to the channel.
     */
    @Override
    final void flushNBytes(final int count) throws IOException {
        final int position = buffer.position();
        final int validity = buffer.limit();
        buffer.limit(count);
        writeFully();
        bufferOffset = position();
        buffer.limit(validity).compact().limit(buffer.position()).position(position - count);
    }

    /**
     * Writes fully the buffer content from beginning to buffer limit.
     * Caller must update the buffer position after this method call.
     *
     * @throws IOException if an error occurred while writing to the channel.
     */
    private void writeFully() throws IOException {
        buffer.rewind();
        while (buffer.hasRemaining()) {
            if (channel.write(buffer) == 0) {
                onEmptyTransfer();
            }
        }
    }

    /**
     * Notifies two {@code ChannelData} instances that operations will continue with the specified take over.
     * This method should be invoked when write operations with this {@code ChannelDataOutput} are completed
     * for now, and read operations are about to begin with a {@link ChannelDataInput} sharing the same channel.
     *
     * <h4>Usage</h4>
     * This method is used when a {@link ChannelDataInput} and a {@link ChannelDataOutput} are wrapping
     * the same {@link java.nio.channels.ByteChannel} and used alternatively for reading and writing.
     * After a read operation, {@code in.yield(out)} should be invoked for ensuring that the output
     * position is valid for the new channel position.
     *
     * @param  takeOver  the {@link ChannelDataInput} which will continue operations after this instance.
     *
     * @see ChannelDataOutput#ChannelDataOutput(ChannelDataInput)
     */
    public final void yield(final ChannelDataInput takeOver) throws IOException {
        /*
         * Flush the full buffer content for avoiding data lost. Note that the buffer position
         * is not necessarily at the buffer limit, so some bytes may be written past the position.
         * The buffer content is still valid, so we configure `takeOver` as if the write operation
         * was immediately following by a read of the same bytes. This is not only an optimization,
         * but also needed for preserving the byte containing the bits when `bitOffset` is non-zero.
         */
        int position = buffer.position();
        writeFully();
        if (getBitOffset() != 0) {
            position--;     // For input, the byte containing bits is at instead of after the position.
            bitPosition -= (1L << BIT_OFFSET_SIZE);
        }
        buffer.position(position);
        copyTo(takeOver);
        assert takeOver.buffer == buffer;
        /*
         * If above assertion is false, we could still work with:
         *
         *     moveBufferForward(buffer.limit());
         *     takeOver.buffer.limit(0);
         *
         * However there is a slight complication because of the need to copy bits if `bitOffset` is non-zero
         * (see ChannelDataInput.yield(…) for code example). For now it is not needed.
         */
    }

    /**
     * Clears the buffer and set the position to 0.
     * This method does not read or write any byte.
     */
    public final void clear() {
        buffer.clear().limit(0);
        bufferOffset = 0;
        bitPosition  = 0;
    }
}
