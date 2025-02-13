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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.InvalidSeekException;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.util.ArgumentChecks.ensureBetween;


/**
 * Provides convenience methods for working with a ({@link ReadableByteChannel}, {@link ByteBuffer}) pair.
 * The channel and the buffer must be supplied by the caller. It is okay if they have already been used
 * before {@code ChannelDataInput} creation.
 *
 * <h2>Encapsulation</h2>
 * This class exposes publicly the {@linkplain #channel} and the {@linkplain #buffer buffer} because this class
 * is not expected to perform all possible data manipulations that we can do with the buffers. This class is only
 * a helper tool, which often needs to be completed by specialized operations performed directly on the buffer.
 * However, users are encouraged to transfer data from the channel to the buffer using only the methods provided
 * in this class if they want to keep the {@link #seek(long)} and {@link #getStreamPosition()} values accurate.
 *
 * <p>Since this class is only a helper tool, it does not "own" the channel and consequently does not provide
 * {@code close()} method. It is users responsibility to close the channel after usage.</p>
 *
 * <h2>Relationship with {@code ChannelImageInputStream}</h2>
 * This class API is compatible with the {@link javax.imageio.stream.ImageInputStream} interface, so subclasses
 * can implement that interface if they wish. This is done by {@link ChannelImageInputStream} for situations
 * when inter-operability with {@link javax.imageio} is needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ChannelDataInput extends ChannelData implements DataInput {
    /**
     * Minimum number of bytes to skip in the {@code seek(long)} operation.
     * If there is less bytes to skip, then it is not worth to do a seek
     * and we will continue reading as normal instead.
     *
     * <p>This threshold is used because some formats add padding to their data for aligning on 32 bits boundary.
     * It may result in very small seeks before reading the next chunk of data. A common padding is 32 bits,
     * but there is no harm in using a larger one here (64 bits).</p>
     */
    private static final int SEEK_THRESHOLD = Long.BYTES;

    /**
     * The channel from where data are read.
     * This is supplied at construction time.
     */
    public final ReadableByteChannel channel;

    /**
     * Creates a new data input for the given channel and using the given buffer.
     * If the buffer already contains some data, then the {@code filled} argument shall be {@code true}.
     * Otherwise (e.g. if it is a newly created buffer), then {@code filled} shall be {@code false}.
     *
     * @param  filename  a short identifier (typically a filename without path) used for formatting error message.
     * @param  channel   the channel from where data are read.
     * @param  buffer    the buffer where to copy the data.
     * @param  filled    {@code true} if the buffer already contains data, or {@code false} if it needs
     *                   to be initially filled with some content read from the channel.
     * @throws IOException if an error occurred while reading the channel.
     */
    public ChannelDataInput(final String filename, final ReadableByteChannel channel, final ByteBuffer buffer,
            final boolean filled) throws IOException
    {
        super(filename, channel, buffer);
        this.channel = channel;
        if (!filled) {
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
        }
    }

    /**
     * Creates a new data input with the same name and position than the given object, but a different channel.
     * This is used when the channel performs some filtering on the data, for example inflating a ZIP file.
     *
     * @param other    the other stream from which to copy the filename and position.
     * @param channel  the new channel to use. Stream position shall be the same as {@code other.channel} position.
     * @param buffer   the new buffer to use. Its content will be discarded (limit set to 0).
     */
    @SuppressWarnings("this-escape")    // `moveBufferForward(int)` is safe.
    public ChannelDataInput(final ChannelDataInput other, final ReadableByteChannel channel, final ByteBuffer buffer) {
        super(other, buffer, false);
        this.channel = channel;
        moveBufferForward(other.buffer.limit());
        buffer.limit(0);
        bitPosition = 0;
    }

    /**
     * Creates a new instance for a buffer filled with the bytes to use.
     * This constructor uses an independent, read-only view of the given buffer.
     * No reference to the given buffer will be retained.
     *
     * @param  filename  a short identifier (typically a filename without path) used for formatting error message.
     * @param  data      the buffer filled with all bytes to read.
     */
    public ChannelDataInput(final String filename, final ByteBuffer data) {
        super(filename, data);
        channel = new NullChannel();
    }

    /**
     * Creates a new input stream from the given {@code ChannelDataInput}.
     * This constructor is invoked when we need to change the implementation class.
     * The old input should not be used anymore after this constructor has been invoked.
     *
     * @param  input  the existing instance from which to takes the channel and buffer.
     */
    ChannelDataInput(final ChannelDataInput input) {
        super(input, input.buffer, true);
        channel = input.channel;
    }

    /**
     * {@return the wrapped channel where data are read}.
     * This is the {@link #channel} field value.
     *
     * @see #channel
     */
    @Override
    public final Channel channel() {
        return channel;
    }

    /**
     * Tries to read more bytes from the channel without changing the buffer position.
     * This method returns a negative number if the buffer is already full or if the channel reached the
     * <i>end of stream</i>. Otherwise this method reads an arbitrary number of bytes not greater
     * than the space available in the buffer, and returns the amount bytes actually read.
     *
     * @return the number of bytes read, or -2 if the buffer is full, or -1 on <i>end of stream</i>.
     * @throws IOException if an error occurred while reading the bytes.
     */
    public final int prefetch() throws IOException {
        final int limit    = buffer.limit();
        final int capacity = buffer.capacity();
        if (limit == capacity) {
            return -2;
        }
        final int position = buffer.position();
        buffer.limit(capacity).position(limit);
        int c = channel.read(buffer);
        while (c == 0) {
            onEmptyTransfer();
            c = channel.read(buffer);
        }
        buffer.limit(buffer.position()).position(position);
        return c;
    }

    /**
     * Moves the stream position to the next byte boundary.
     * If the bit offset is zero, this method does nothing.
     * Otherwise it skips the remaining bits in current byte.
     */
    @Override
    public final void skipRemainingBits() {
        if (bitPosition != 0) {             // Quick check for common case.
            if (getBitOffset() != 0) {
                buffer.get();               // Should never fail, otherwise bit offset should have been invalid.
            }
            bitPosition = 0;
        }
    }

    /**
     * Returns {@code true} if the buffer or the channel has at least one byte remaining.
     * If the {@linkplain #buffer buffer} has no remaining bytes, then this method will attempt
     * to read at least one byte from the {@linkplain #channel}. If no bytes can be read because
     * the channel has reached the end of stream, then this method returns {@code false}.
     *
     * @return {@code true} if the buffer contains at least one remaining byte.
     * @throws IOException if it was necessary to read from the channel and this operation failed.
     */
    public final boolean hasRemaining() throws IOException {
        if (buffer.hasRemaining()) {
            return true;
        }
        moveBufferForward(buffer.limit());
        buffer.clear();
        int c = channel.read(buffer);
        while (c == 0) {
            onEmptyTransfer();
            c = channel.read(buffer);
        }
        buffer.flip();
        return c >= 0;
    }

    /**
     * Makes sure that the buffer contains at least <var>n</var> remaining bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the {@linkplain ByteBuffer#capacity() buffer capacity}.
     *
     * @param  n  the minimal number of bytes needed in the {@linkplain #buffer buffer}.
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if another kind of error occurred while reading.
     */
    public final void ensureBufferContains(int n) throws EOFException, IOException {
        assert n >= 0 && n <= buffer.capacity() : n;
        n -= buffer.remaining();
        if (n > 0) {
            moveBufferForward(buffer.position());
            buffer.compact();
            do {
                final int c = channel.read(buffer);
                if (c <= 0) {
                    if (c != 0) {
                        throw new EOFException(eof());
                    }
                    onEmptyTransfer();
                }
                n -= c;
            } while (n > 0);
            buffer.flip();
        }
    }

    /**
     * Makes sure that the buffer contains at least one remaining byte.
     *
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if another kind of error occurred while reading.
     */
    private void ensureNonEmpty() throws IOException {
        if (!hasRemaining()) {
            throw new EOFException(eof());
        }
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     */
    @Override
    public final long getStreamPosition() {
        return position();
    }

    /**
     * Returns the "end of file" error message, for {@link EOFException} creations.
     */
    private String eof() {
        return Errors.format(Errors.Keys.UnexpectedEndOfFile_1, filename);
    }

    /**
     * Pushes back the last processed byte. This is used when a call to {@link #readBits(int)} did not
     * used every bits in a byte, or when {@link #readLine()} checked for the Windows-style of EOL.
     */
    private void pushBack() {
        buffer.position(buffer.position() - 1);
    }

    /**
     * Reads a single bit from the stream. The bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @return the value of the next bit from the stream.
     * @throws IOException if an error occurred while reading (including EOF).
     *
     * @see #readBoolean()
     */
    public final int readBit() throws IOException {
        ensureBufferContains(Byte.BYTES);
        final int bp = buffer.position();
        final long position = Math.addExact(bufferOffset, bp);      // = position() but inlined for reusing `bp`.
        if ((bitPosition >>> BIT_OFFSET_SIZE) != position) {
            bitPosition = position << BIT_OFFSET_SIZE;              // Clear the bits and mark as valid position.
        }
        final int bitOffset = (Byte.SIZE - 1) - (int) (bitPosition++ & ((1L << BIT_OFFSET_SIZE) - 1));
        final byte value = (bitOffset != 0) ? buffer.get(bp) : buffer.get();
        return (value & (1 << bitOffset)) == 0 ? 0 : 1;
    }

    /**
     * Reads many bits from the stream. The first bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @param  numBits  the number of bits to read.
     * @return the value of the next bits from the stream.
     * @throws IOException if an error occurred while reading (including EOF).
     */
    public final long readBits(int numBits) throws IOException {
        ensureBetween("numBits", 0, Long.SIZE, numBits);
        if (numBits == 0) {
            return 0;
        }
        /*
         * Reads the bits available in the next bytes (all of them if bitOffset == 0)
         * and compute the number of bits that still need to be read. That number may
         * be negative if we have read too many bits.
         */
        final int bitOffset = getBitOffset();
        long value = readByte() & (0xFF >>> bitOffset);
        numBits -= (Byte.SIZE - bitOffset);
        while (numBits > 0) {
            value = (value << Byte.SIZE) | readUnsignedByte();
            numBits -= Byte.SIZE;
        }
        if (numBits != 0) {
            value >>>= (-numBits);                      // Discard the unwanted bits.
            numBits += Byte.SIZE;
            pushBack();
        }
        setBitOffset(numBits);
        return value;
    }

    /**
     * Reads a byte from the stream and returns {@code true} if it is nonzero, {@code false} otherwise.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return readByte() != 0;
     *     }
     *
     * For reading a single bit, use {@link #readBit()} instead.
     *
     * @return the value of the next boolean from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     *
     * @see #readBit()
     */
    @Override
    public final boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
     * Reads the next byte value (8 bits) from the stream. This method ensures that there is at
     * least 1 byte remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#get()}.
     *
     * @return the value of the next byte from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final byte readByte() throws IOException {
        ensureBufferContains(Byte.BYTES);
        return buffer.get();
    }

    /**
     * Reads the next unsigned byte value (8 bits) from the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return Byte.toUnsignedInt(readByte());
     *     }
     *
     * @return the value of the next unsigned byte from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    /**
     * Reads the next short value (16 bits) from the stream. This method ensures that there is at
     * least 2 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getShort()}.
     *
     * @return the value of the next short from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final short readShort() throws IOException {
        ensureBufferContains(Short.BYTES);
        return buffer.getShort();
    }

    /**
     * Reads the next unsigned short value (16 bits) from the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return Short.toUnsignedInt(readShort());
     *     }
     *
     * @return the value of the next unsigned short from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(readShort());
    }

    /**
     * Reads the next character (16 bits) from the stream. This method ensures that there is at
     * least 2 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getChar()}.
     *
     * @return the value of the next character from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final char readChar() throws IOException {
        ensureBufferContains(Character.BYTES);
        return buffer.getChar();
    }

    /**
     * Reads the next integer value (32 bits) from the stream. This method ensures that there is at
     * least 4 bytes remaining in the buffer, reading new bytes from the channel if necessary, then
     * delegates to {@link ByteBuffer#getInt()}.
     *
     * @return the value of the next integer from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final int readInt() throws IOException {
        ensureBufferContains(Integer.BYTES);
        return buffer.getInt();
    }

    /**
     * Reads the next unsigned integer value (32 bits) from the stream.
     * The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return Integer.toUnsignedLong(readInt());
     *     }
     *
     * @return the value of the next unsigned integer from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final long readUnsignedInt() throws IOException {
        return Integer.toUnsignedLong(readInt());
    }

    /**
     * Reads the next long value (64 bits) from the stream. This method ensures that there is at
     * least 8 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getLong()}.
     *
     * @return the value of the next integer from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final long readLong() throws IOException {
        ensureBufferContains(Long.BYTES);
        return buffer.getLong();
    }

    /**
     * Reads the next float value (32 bits) from the stream. This method ensures that there is at
     * least 4 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getFloat()}.
     *
     * @return the value of the next float from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final float readFloat() throws IOException {
        ensureBufferContains(Float.BYTES);
        return buffer.getFloat();
    }

    /**
     * Reads the next double value (64 bits) from the stream. This method ensures that there is at
     * least 8 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getDouble()}.
     *
     * @return the value of the next double from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final double readDouble() throws IOException {
        ensureBufferContains(Double.BYTES);
        return buffer.getDouble();
    }

    /**
     * Reads the given number of bytes from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(byte[], int, int)} with a new array.
     *
     * @param  length The number of bytes to read.
     * @return the next bytes in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final byte[] readBytes(final int length) throws IOException {
        final byte[] array = new byte[length];
        readFully(array);
        return array;
    }

    /**
     * Reads the given number of characters from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(char[], int, int)} with a new array.
     *
     * @param  length The number of characters to read.
     * @return the next characters in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final char[] readChars(final int length) throws IOException {
        final char[] array = new char[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given number of shorts from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(short[], int, int)} with a new array.
     *
     * @param  length The number of shorts to read.
     * @return the next shorts in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final short[] readShorts(final int length) throws IOException {
        final short[] array = new short[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given number of integers from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(int[], int, int)} with a new array.
     *
     * @param  length The number of integers to read.
     * @return the next integers in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final int[] readInts(final int length) throws IOException {
        final int[] array = new int[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given number of longs from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(long[], int, int)} with a new array.
     *
     * @param  length The number of longs to read.
     * @return the next longs in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final long[] readLongs(final int length) throws IOException {
        final long[] array = new long[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given number of floats from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(float[], int, int)} with a new array.
     *
     * @param  length The number of floats to read.
     * @return the next floats in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final float[] readFloats(final int length) throws IOException {
        final float[] array = new float[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given number of doubles from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(double[], int, int)} with a new array.
     *
     * @param  length The number of doubles to read.
     * @return the next doubles in a newly allocated array of the given length.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final double[] readDoubles(final int length) throws IOException {
        final double[] array = new double[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads {@code dest.length} bytes from the stream, and stores them into
     * {@code dest} starting at index 0. The implementation is as below:
     *
     * {@snippet lang="java" :
     *     return readFully(dest, 0, dest.length);
     *     }
     *
     * @param  dest An array of bytes to be written to.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final void readFully(final byte[] dest) throws IOException {
        readFully(dest, 0, dest.length);
    }

    /**
     * Reads {@code length} bytes from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest    an array of bytes to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of bytes to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final void readFully(final byte[] dest, int offset, int length) throws IOException {
        while (length != 0) {
            ensureNonEmpty();
            final int n = Math.min(buffer.remaining(), length);
            buffer.get(dest, offset, n);
            offset += n;
            length -= n;
        }
    }

    /**
     * Helper class for the {@code readFully(…)} methods,
     * in order to avoid duplicating almost identical code many times.
     */
    abstract class ArrayReader extends DataTransfer {
        /**
         * For subclass constructors only.
         */
        ArrayReader() {
        }

        /**
         * Returns a file identifier for error messages or debugging purpose.
         */
        @Override
        final String filename() {
            return filename;
        }

        /**
         * Transfers the data from the buffer created by {@link #createView()} to array
         * of primitive Java type known by the subclass. This method may be invoked an
         * arbitrary number of time.
         */
        abstract void transfer(int offset, int n);

        /**
         * Skips the given number of bytes in the buffer. It is caller responsibility to ensure
         * that there is enough bytes remaining in the buffer.
         */
        private void skipInBuffer(final int n) {
            buffer.position(buffer.position() + n);
        }

        /**
         * Moves to the given position in the stream.
         */
        @Override
        final void seek(long n) throws IOException {
            ChannelDataInput.this.seek(n);
        }

        /**
         * Reads {@code length} values from the stream, and stores them into the array known to subclass,
         * starting at index {@code offset}.
         *
         * <p>If a non-null {@code Buffer} is given in argument to this method, then it must be a view over
         * the full content of {@link ChannelDataInput#buffer} (i.e. the view element at index 0 shall be
         * defined by the buffer elements starting at index 0).</p>
         *
         * @param  view     existing buffer to use as a view over {@link ChannelDataInput#buffer}, or {@code null}.
         * @param  offset   the starting position within {@code dest} to write.
         * @param  length   the number of values to read.
         * @throws IOException if an error (including EOF) occurred while reading the stream.
         */
        @Override
        void readFully(Buffer view, int offset, int length) throws IOException {
            final int dataSizeShift = dataSizeShift();
            ensureBufferContains(Math.min(length << dataSizeShift, buffer.capacity()));
            if (view == null) {
                view = createView();                                    // Must be after ensureBufferContains(int).
            } else {
                // Buffer position must be a multiple of the data size.
                // If not, fix that by shifting the content to index 0.
                if ((buffer.position() & ((1 << dataSizeShift) - 1)) != 0) {
                    moveBufferForward(buffer.position());
                    buffer.compact().flip();
                }
                view.limit   (buffer.limit()    >> dataSizeShift)
                    .position(buffer.position() >> dataSizeShift);      // See assumption documented in Javadoc.
            }
            int n = Math.min(view.remaining(), length);
            transfer(offset, n);
            skipInBuffer(n << dataSizeShift);
            while ((length -= n) != 0) {
                offset += n;
                ensureBufferContains(1 << dataSizeShift);               // Actually read as much data as possible.
                view.rewind().limit(buffer.remaining() >> dataSizeShift);
                transfer(offset, n = Math.min(view.remaining(), length));
                skipInBuffer(n << dataSizeShift);
            }
        }
    }

    /**
     * Reads bytes from the enclosing stream and stores them into the given destination array. This implementation
     * actually redirects the reading process to {@link ChannelDataInput#readFully(byte[], int, int)} because this
     * specialization does not need a view. This implementation is useless for {@code ChannelDataInput}, but avoid
     * the need to implement special cases in other classes like {@link HyperRectangleReader}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class BytesReader extends ArrayReader {
        /** The array where to store the values. */ private byte[] dest;
        BytesReader(final byte[] dest)           {this.dest = dest;}
        @Override int    dataSizeShift()         {return 0;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return ByteBuffer.wrap(dest);}
        @Override Buffer view()                  {return buffer;}
        @Override Buffer createView()            {return buffer;}
        @Override void   createDataArray(int n)  {dest = new byte[n];}
        @Override void   transfer(int p, int n)  {buffer.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (byte[]) array;};
        @Override void readFully(Buffer view, int offset, int length) throws IOException {
            ChannelDataInput.this.readFully(dest, offset, length);
        }
    };

    /**
     * Reads characters from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class CharsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private CharBuffer view;
        /** The array where to store the values.   */ private char[] dest;
        CharsReader(final CharBuffer source)     {this.view = source;}
        CharsReader(final char[] dest)           {this.dest = dest;}
        @Override int    dataSizeShift()         {return 1;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return CharBuffer.wrap(dest);}
        @Override Buffer view()                  {return view;}
        @Override Buffer createView()            {return view = buffer.asCharBuffer();}
        @Override void   createDataArray(int n)  {dest = new char[n];}
        @Override void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (char[]) array;};
    };

    /**
     * Reads short integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class ShortsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private ShortBuffer view;
        /** The array where to store the values.   */ private short[] dest;
        ShortsReader(final ShortBuffer source)   {this.view = source;}
        ShortsReader(final short[] dest)         {this.dest = dest;}
        @Override int    dataSizeShift()         {return 1;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return ShortBuffer.wrap(dest);}
        @Override Buffer view()                  {return view;}
        @Override Buffer createView()            {return view = buffer.asShortBuffer();}
        @Override void   createDataArray(int n)  {dest = new short[n];}
        @Override void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (short[]) array;};
    };

    /**
     * Reads integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class IntsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private IntBuffer view;
        /** The array where to store the values.   */ private int[] dest;
        IntsReader(final IntBuffer source)        {this.view = source;}
        IntsReader(final int[] dest)              {this.dest = dest;}
        @Override int    dataSizeShift()          {return 2;}
        @Override Object dataArray()              {return dest;}
        @Override Buffer dataArrayAsBuffer()      {return IntBuffer.wrap(dest);}
        @Override Buffer view()                   {return view;}
        @Override Buffer createView()             {return view = buffer.asIntBuffer();}
        @Override void   createDataArray(int n)   {dest = new int[n];}
        @Override void   transfer(int p, int n)   {view.get(dest, p, n);}
        @Override void   setDest(Object array)    {dest = (int[]) array;};
    };

    /**
     * Reads long integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class LongsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private LongBuffer view;
        /** The array where to store the values.   */ private long[] dest;
        LongsReader(final LongBuffer source)     {this.view = source;}
        LongsReader(final long[] dest)           {this.dest = dest;}
        @Override int    dataSizeShift()         {return 3;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return LongBuffer.wrap(dest);}
        @Override Buffer view()                  {return view;}
        @Override Buffer createView()            {return view = buffer.asLongBuffer();}
        @Override void   createDataArray(int n)  {dest = new long[n];}
        @Override void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (long[]) array;};
    };

    /**
     * Reads float values from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class FloatsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private FloatBuffer view;
        /** The array where to store the values.   */ private float[] dest;
        FloatsReader(final FloatBuffer source)   {this.view = source;}
        FloatsReader(final float[] dest)         {this.dest = dest;}
        @Override int    dataSizeShift()         {return 2;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return FloatBuffer.wrap(dest);}
        @Override Buffer view()                  {return view;}
        @Override Buffer createView()            {return view = buffer.asFloatBuffer();}
        @Override void   createDataArray(int n)  {dest = new float[n];}
        @Override void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (float[]) array;};
    };

    /**
     * Reads double values from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class DoublesReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */ private DoubleBuffer view;
        /** The array where to store the values.   */ private double[] dest;
        DoublesReader(final DoubleBuffer source) {this.view = source;}
        DoublesReader(final double[] dest)       {this.dest = dest;}
        @Override int    dataSizeShift()         {return 3;}
        @Override Object dataArray()             {return dest;}
        @Override Buffer dataArrayAsBuffer()     {return DoubleBuffer.wrap(dest);}
        @Override Buffer view()                  {return view;}
        @Override Buffer createView()            {return view = buffer.asDoubleBuffer();}
        @Override void   createDataArray(int n)  {dest = new double[n];}
        @Override void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override void   setDest(Object array)   {dest = (double[]) array;};
    };

    /**
     * Reads {@code length} characters from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of characters to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of characters to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final char[] dest, final int offset, final int length) throws IOException {
        new CharsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} short integers from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of short integers to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of short integers to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final short[] dest, final int offset, final int length) throws IOException {
        new ShortsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} integers from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest    an array of integers to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of integers to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final int[] dest, final int offset, final int length) throws IOException {
        new IntsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} long integers from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest    an array of long integers to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of long integers to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final long[] dest, final int offset, final int length) throws IOException {
        new LongsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} floats from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest    an array of floats to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of floats to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final float[] dest, final int offset, final int length) throws IOException {
        new FloatsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} doubles from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest    an array of doubles to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of doubles to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final double[] dest, final int offset, final int length) throws IOException {
        new DoublesReader(dest).readFully(null, offset, length);
    }

    /**
     * Decodes a string from a sequence of bytes in the given encoding.
     * This method tries to avoid the creation of a temporary {@code byte[]} array when possible.
     *
     * <h4>Performance note</h4>
     * This convenience method should be used only for small number of short {@link String} instances
     * to decode, for example attribute values in the file header. For large amount of data, consider
     * using {@link java.nio.charset.CharsetDecoder} instead.
     *
     * @param  length    number of bytes to read.
     * @param  encoding  the character encoding.
     * @return the string decoded from the {@code length} next bytes.
     * @throws IOException if an error occurred while reading the bytes, or if the given encoding is invalid.
     */
    public final String readString(int length, final Charset encoding) throws IOException {
        final byte[] array;
        int position;
        if (buffer.hasArray() && length <= buffer.capacity()) {
            ensureBufferContains(length);
            position = buffer.position();           // Must be after `ensureBufferContains(int)`.
            buffer.position(position + length);     // Before `new String` for consistency with the `else` block in case of UnsupportedEncodingException.
            array = buffer.array();
            position += buffer.arrayOffset();
        } else {
            array = readBytes(length);
            position = 0;
        }
        while (length > 0 && array[position + (length - 1)] == 0) {
            length--;                               // Skip trailing 0 (end of string in C/C++ languages).
        }
        return new String(array, position, length, encoding);
    }

    /**
     * Reads a null-terminated US-ASCII, ISO-LATIN-1, UTF-8, UTF-16 or UTF-32 string.
     * Note that {@code 0x00} is always the one-byte NUL character in UTF-8.
     * It cannot be part of a multi-byte character's representation by design.
     *
     * <p>The character encoding should be specified in argument. If {@code null}, this method infers
     * the encoding with the following rules specified by ISO 14496-12 (Base Media File Format):</p>
     *
     * <ul>
     *   <li>If the string starts with a Byte Order Mark (<abbr>BOM</abbr>), then UTF-16 encoding is assumed.</li>
     *   <li>Otherwise, UTF-8 encoding is assumed. This method does not test whether the string is well-formed.</li>
     * </ul>
     *
     * <h4>Limitations</h4>
     * This convenience method should be used only for small number of short {@link String} instances to decode
     * using one of the encoding specified in {@link StandardCharsets}. For large amount of data, or for support
     * of any encoding other than the standard ones, use {@link java.nio.charset.CharsetDecoder} instead.
     *
     * @param  encoding  the character encoding, or {@code null} for UTF-8 or UTF-16 depending on whether a <abbr>BOM</abbr> is present.
     * @return the character string, possibly empty.
     * @throws UnsupportedEncodingException if the encoding is not one of the {@link StandardCharsets}.
     * @throws IOException if an error occurred while reading the string.
     */
    public final String readNullTerminatedString(Charset encoding) throws IOException {
        long start = position();
        if (encoding == null) {
            /*
             * If the string may be UTF-16, check for the Byte Order Mark (BOM).
             * If none, UTF-8 is assumed. This semantic is used by ISO 14496-12
             * (Base Media File Format).
             */
            switch (readByte()) {
                case (byte) 0x00: return "";
                case (byte) 0xFE: if (readByte() == (byte) 0xFF) encoding = StandardCharsets.UTF_16BE; break;
                case (byte) 0xFF: if (readByte() == (byte) 0xFE) encoding = StandardCharsets.UTF_16LE; break;
            }
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8;
                buffer.position(buffer.position() - Byte.BYTES);
                // No need to push back the first character because it is known to be non-zero.
            } else {
                start += Short.BYTES;
            }
        }
        /*
         * Get the number of bytes per character. This number determines the size of the NUL terminator.
         * This information is not provided in the `Charset` API (as of Java 23), which is the reason why
         * this method supports only `StandardCharsets` values.
         */
        final int charSize;
        final String name = encoding.name();
        if (name.equals("US-ASCII") || name.equals("ISO-8859-1") || name.equals("UTF-8")) {
            charSize = Byte.BYTES;
        } else if (name.startsWith("UTF-16")) {
            charSize = Short.BYTES;
        } else if (name.startsWith("UTF-32")) {
            charSize = Integer.BYTES;
        } else {
            throw new UnsupportedEncodingException(name);
        }
        /*
         * Search the nul terminator directly in the buffer. If we need to read more bytes,
         * we will try to do that without discarding the first characters of the strings.
         */
        int base = buffer.position();
search: for (;;) {
            if (charSize == 1) {
                // Optimization for the most common cases: US-ASCII, ISO-LATIN-1, UTF-8.
                while (buffer.hasRemaining()) {
                    if (buffer.get() == 0) {
                        break search;
                    }
                }
            } else {
                while (buffer.remaining() >= charSize) {
                    int c = (charSize <= Short.BYTES) ? buffer.getShort() : buffer.getInt();
                    if (c == 0) break search;
                }
            }
            /*
             * Need more bytes. If there is enough room in the buffer either at the beginning (base > 0)
             * or at the end (limit < capacity), temporarily move the position back to the base before
             * to invoke `ensureBufferContains(…)` for avoiding to discard the bytes that we will need.
             */
            final int count = buffer.position() - base;
            final int need  = count + charSize;
            if (buffer.capacity() - need >= 0) {
                buffer.position(base);
                ensureBufferContains(need);
                base = buffer.position();
                buffer.position(base + count);
            } else {
                // Cannot avoid to discard what we have read before.
                ensureBufferContains(charSize);
            }
        }
        int size = Math.toIntExact(position() - start - charSize);
        if (size <= 0) return "";   // Shortcut for a common case.
        seek(start);
        String value = readString(size, encoding);
        skipNBytes(charSize);       // Skip the NUL terminal character.
        return value;
    }

    /**
     * Reads in a string that has been encoded using a Java modified UTF-8 string.
     * The number of bytes to read is encoded in the next unsigned short integer of the stream.
     *
     * @return the string reads from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     *
     * @see DataInput#readUTF()
     */
    @Override
    public final String readUTF() throws IOException {
        final ByteOrder oldOrder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);
        try {
            return DataInputStream.readUTF(this);
        } finally {
            buffer.order(oldOrder);
        }
    }

    /**
     * Reads new bytes until the next EOL. This method can read only US-ASCII strings.
     * This method is provided for compliance with the {@link DataInput} interface,
     * but is generally not recommended.
     *
     * @return the next line, or {@code null} if the EOF has been reached.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final String readLine() throws IOException {
        if (!hasRemaining()) {
            return null;
        }
        int c = Byte.toUnsignedInt(buffer.get());
        StringBuilder line = new StringBuilder();
        line.append((char) c);
loop:   while (hasRemaining()) {
            c = Byte.toUnsignedInt(buffer.get());
            switch (c) {
                case '\n': break loop;
                case '\r': {
                    if (hasRemaining() && buffer.get() != '\n') {
                        pushBack();
                    }
                    break loop;
                }
            }
            line.append((char) c);
        }
        return line.toString();
    }

    /**
     * Tries to skip over <var>n</var> bytes of data from the input stream.
     * This method may skip over some smaller number of bytes, possibly zero.
     * A negative value move backward in the input stream.
     *
     * @param  n  maximal number of bytes to skip. Can be negative.
     * @return number of bytes actually skipped.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public int skipBytes(int n) throws IOException {
        if (!hasRemaining()) {
            return 0;
        }
        n = Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + n);
        return n;
    }

    /**
     * Skips over and discards exactly <var>n</var> bytes of data from this input stream.
     *
     * @param  n  number of bytes to skip. Can be negative.
     * @throws IOException if an error occurred while reading.
     */
    public final void skipNBytes(int n) throws IOException {
        n -= skipBytes(n);
        if (n != 0) {
            seek(Math.addExact(position(), n));
        }
    }

    /**
     * Moves to the given position in this stream.
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
        } else if ((p < 0 || p - buffer.limit() >= SEEK_THRESHOLD) && channel instanceof SeekableByteChannel) {
            /*
             * Requested position is outside the current limits of the buffer,
             * but we can set the new position directly in the channel.
             */
            ((SeekableByteChannel) channel).position(toSeekableByteChannelPosition(position));
            bufferOffset = position;
            buffer.clear().limit(0);
        } else if (p >= 0) {
            /*
             * Requested position is after the current buffer limit and
             * we cannot seek, so we have to read everything before.
             */
            do {
                moveBufferForward(buffer.limit());
                p -= buffer.limit();
                buffer.clear();
                final int c = channel.read(buffer);
                if (c <= 0) {
                    if (c != 0) {
                        throw new EOFException(eof());
                    }
                    onEmptyTransfer();
                }
                buffer.flip();
            } while (p > buffer.limit());
            buffer.position((int) p);
        } else {
            /*
             * Requested position is before the current buffer limits and we cannot seek.
             */
            throw new InvalidSeekException(Resources.format(Resources.Keys.StreamIsForwardOnly_1, filename));
        }
        bitPosition = 0;
        assert position() == position : position;
    }

    /**
     * Specifies a range of bytes which is expected to be read.
     * The range of bytes is only a hint and may be ignored, depending on subclasses.
     * Reading more bytes than specified is okay, only potentially less efficient.
     *
     * @param  lower  position (inclusive) of the first byte to be requested.
     * @param  upper  position (exclusive) of the last byte to be requested.
     */
    public final void rangeOfInterest(long lower, long upper) {
        if (channel instanceof ByteRangeChannel) {
            lower = toSeekableByteChannelPosition(lower);
            upper = toSeekableByteChannelPosition(upper);
            ((ByteRangeChannel) channel).rangeOfInterest(lower, upper);
        }
    }

    /**
     * Forgets the given number of bytes in the buffer.
     * This is invoked for making room for more bytes.
     *
     * @param  count  number of bytes to forget, between 1 and buffer limit.
     */
    @Override
    final void flushNBytes(final int count) throws IOException {
        final int p = buffer.position();
        buffer.position(count).compact()
              .limit(buffer.position())     // Not the same value as `p`. It is rather equal to `limit - count`.
              .position(p - count);
        bufferOffset = Math.addExact(bufferOffset, count);
    }

    /**
     * Notifies two {@code ChannelData} instances that operations will continue with the specified take over.
     * This method should be invoked when read operations with this {@code ChannelDataInput} are completed for
     * now, and write operations are about to begin with a {@link ChannelDataOutput} sharing the same channel.
     *
     * <h4>Usage</h4>
     * This method is used when a {@link ChannelDataInput} and a {@link ChannelDataOutput} are wrapping
     * the same {@link java.nio.channels.ByteChannel} and used alternatively for reading and writing.
     * After a read operation, {@code in.yield(out)} should be invoked for ensuring that the output
     * position is valid for the new channel position.
     *
     * @param  takeOver  the {@link ChannelDataOutput} which will continue operations after this instance.
     *
     * @see ChannelDataOutput#ChannelDataOutput(ChannelDataInput)
     */
    public final void yield(final ChannelDataOutput takeOver) throws IOException {
        int bitOffset = 0;
        byte bits = 0;
        /*
         * If we filled the buffer with more bytes than the buffer position,
         * the channel position is too far ahead. We need to seek backward.
         * Note that if `bitOffset` is not zero, then there is at least one
         * remaining byte, which is the byte where bits are read from.
         */
        if (buffer.hasRemaining()) {
            if (!(channel instanceof SeekableByteChannel)) {
                throw new IOException(Resources.format(Resources.Keys.StreamIsForwardOnly_1, takeOver.filename));
            }
            bitOffset = getBitOffset();
            if (bitOffset != 0) {
                bits = savedBitsForOutput(bitOffset);
            }
            final long p = position();
            ((SeekableByteChannel) channel).position(toSeekableByteChannelPosition(p));
            bufferOffset = p;                   // Modify object state only on success.
        } else {
            moveBufferForward(buffer.limit());
        }
        copyTo(takeOver);
        takeOver.buffer.limit(0);               // Also set the position to 0.
        if (bitOffset != 0) {
            takeOver.buffer.limit(1).put(bits);
            takeOver.bitPosition += (1L << BIT_OFFSET_SIZE);        // In output mode, position is after the byte.
        }
    }

    /**
     * Returns the bits to save in {@link ChannelDataOutput} for avoiding information lost.
     * This method is invoked by {@link #yield(ChannelDataOutput)} if this input channel was reading
     * some bits in the middle of a byte. In order to keep the same bit offset in the output channel,
     * the output buffer must contain that byte for allowing to continue to write bits in that byte.
     * This method returns that byte to copy from the input channel to the output channel.
     *
     * @param  bitOffset  current value of {@link #getBitOffset()}, which must be non-zero.
     * @return the byte to copy from the input channel to the output channel.
     */
    byte savedBitsForOutput(final int bitOffset) {
        /*
         * We do not check the position validity because it is guaranteed valid when bitOffset > 0.
         * An IndexOutOfBoundsException here with would be a bug in the way we manage bit offsets.
         */
        return buffer.get(buffer.position());
    }
}
