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

import java.io.IOException;
import java.io.EOFException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;

import static org.apache.sis.util.ArgumentChecks.ensureBetween;

// Branch-dependent imports
import java.nio.channels.FileChannel;


/**
 * Provides convenience methods for working with a ({@link ReadableByteChannel}, {@link ByteBuffer}) pair.
 * The channel and the buffer must be supplied by the caller. It is okay if they have already been used
 * before {@code ChannelDataInput} creation.
 *
 * <div class="section">Encapsulation</div>
 * This class exposes publicly the {@linkplain #channel} and the {@linkplain #buffer buffer} because this class
 * is not expected to perform all possible data manipulations that we can do with the buffers. This class is only
 * a helper tool, which often needs to be completed by specialized operations performed directly on the buffer.
 * However, users are encouraged to transfer data from the channel to the buffer using only the methods provided
 * in this class if they want to keep the {@link #seek(long)} and {@link #getStreamPosition()} values accurate.
 *
 * <p>Since this class is only a helper tool, it does not "own" the channel and consequently does not provide
 * {@code close()} method. It is users responsibility to close the channel after usage.</p>
 *
 * <div class="section">Relationship with {@code DataInput}</div>
 * This class API is compatibly with the {@link java.io.DataInput} interface, so subclasses can implement that
 * interface if they wish. This class does not implement {@code DataInput} itself because it is not needed for
 * SIS purposes, and because {@code DataInput} has undesirable methods ({@code readLine()} and {@code readUTF()}).
 * However the {@link ChannelImageInputStream} class implements the {@code DataInput} interface, together with
 * the {@link javax.imageio.stream.ImageInputStream} one, mostly for situations when inter-operability with
 * {@link javax.imageio} is needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public class ChannelDataInput extends ChannelData {
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
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read.
     * @param  buffer   The buffer where to copy the data.
     * @param  filled   {@code true} if the buffer already contains data, or {@code false} if it needs
     *                  to be initially filled with some content read from the channel.
     * @throws IOException If an error occurred while reading the channel.
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
     * Tries to read more bytes from the channel without changing the buffer position.
     * This method returns a negative number if the buffer is already full or if the channel reached the
     * <cite>end of stream</cite>. Otherwise this method reads an arbitrary amount of bytes not greater
     * than the space available in the buffer, and returns the amount bytes actually read.
     *
     * @return The number of bytes read, or -2 if the buffer is full, or -1 on <cite>end of stream</cite>.
     * @throws IOException If an error occurred while reading the bytes.
     *
     * @since 0.4
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
     * Returns {@code true} if the buffer or the channel has at least one byte remaining.
     * If the {@linkplain #buffer buffer} has no remaining bytes, then this method will attempts
     * to read at least one byte from the {@linkplain #channel}. If no bytes can be read because
     * the channel has reached the end of stream, then this method returns {@code false}.
     *
     * @return {@code true} if the buffer contains at least one remaining byte.
     * @throws IOException If it was necessary to read from the channel and this operation failed.
     */
    public final boolean hasRemaining() throws IOException {
        if (buffer.hasRemaining()) {
            return true;
        }
        bufferOffset += buffer.limit();
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
     * @param  n The minimal number of bytes needed in the {@linkplain #buffer buffer}.
     * @throws EOFException If the channel has reached the end of stream.
     * @throws IOException If an other kind of error occurred while reading.
     */
    public final void ensureBufferContains(int n) throws EOFException, IOException {
        assert n >= 0 && n <= buffer.capacity() : n;
        n -= buffer.remaining();
        if (n > 0) {
            bufferOffset += buffer.position();
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
     * @throws EOFException If the channel has reached the end of stream.
     * @throws IOException If an other kind of error occurred while reading.
     */
    private void ensureNonEmpty() throws IOException {
        if (!hasRemaining()) {
            throw new EOFException(eof());
        }
    }

    /**
     * Returns the "end of file" error message, for {@link EOFException} creations.
     */
    private String eof() {
        return Errors.format(Errors.Keys.UnexpectedEndOfFile_1, filename);
    }

    /**
     * Pushes back the last processed byte. This is used when a call to {@code readBit()} did not
     * used every bits in a byte, or when {@code readLine()} checked for the Windows-style of EOL.
     */
    final void pushBack() {
        buffer.position(buffer.position() - 1);
    }

    /**
     * Reads a single bit from the stream. The bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @return The value of the next bit from the stream.
     * @throws IOException If an error occurred while reading (including EOF).
     */
    public final int readBit() throws IOException {
        return (int) readBits(1);
    }

    /**
     * Reads many bits from the stream. The first bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @param  numBits The number of bits to read.
     * @return The value of the next bits from the stream.
     * @throws IOException If an error occurred while reading (including EOF).
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
            value >>>= (-numBits); // Discard the unwanted bits.
            numBits += Byte.SIZE;
            pushBack();
        }
        setBitOffset(numBits);
        return value;
    }

    /**
     * Reads the next byte value (8 bits) from the stream. This method ensures that there is at
     * least 1 byte remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#get()}.
     *
     * @return The value of the next byte from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final byte readByte() throws IOException {
        ensureBufferContains(Byte.SIZE / Byte.SIZE);
        return buffer.get();
    }

    /**
     * Reads the next unsigned byte value (8 bits) from the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return Byte.toUnsignedInt(readByte());
     * }
     *
     * @return The value of the next unsigned byte from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    /**
     * Reads the next short value (16 bits) from the stream. This method ensures that there is at
     * least 2 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getShort()}.
     *
     * @return The value of the next short from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final short readShort() throws IOException {
        ensureBufferContains(Short.SIZE / Byte.SIZE);
        return buffer.getShort();
    }

    /**
     * Reads the next unsigned short value (16 bits) from the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return Short.toUnsignedInt(readShort());
     * }
     *
     * @return The value of the next unsigned short from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads the next character (16 bits) from the stream. This method ensures that there is at
     * least 2 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getChar()}.
     *
     * @return The value of the next character from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final char readChar() throws IOException {
        ensureBufferContains(Character.SIZE / Byte.SIZE);
        return buffer.getChar();
    }

    /**
     * Reads the next integer value (32 bits) from the stream. This method ensures that there is at
     * least 4 bytes remaining in the buffer, reading new bytes from the channel if necessary, then
     * delegates to {@link ByteBuffer#getInt()}.
     *
     * @return The value of the next integer from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final int readInt() throws IOException {
        ensureBufferContains(Integer.SIZE / Byte.SIZE);
        return buffer.getInt();
    }

    /**
     * Reads the next unsigned integer value (32 bits) from the stream.
     * The implementation is as below:
     *
     * {@preformat java
     *     return Integer.toUnsignedLong(readInt());
     * }
     *
     * @return The value of the next unsigned integer from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final long readUnsignedInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    /**
     * Reads the next long value (64 bits) from the stream. This method ensures that there is at
     * least 8 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getLong()}.
     *
     * @return The value of the next integer from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final long readLong() throws IOException {
        ensureBufferContains(Long.SIZE / Byte.SIZE);
        return buffer.getLong();
    }

    /**
     * Reads the next float value (32 bits) from the stream. This method ensures that there is at
     * least 4 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getFloat()}.
     *
     * @return The value of the next float from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final float readFloat() throws IOException {
        ensureBufferContains(Float.SIZE / Byte.SIZE);
        return buffer.getFloat();
    }

    /**
     * Reads the next double value (64 bits) from the stream. This method ensures that there is at
     * least 8 bytes remaining in the buffer, reading new bytes from the channel if necessary,
     * then delegates to {@link ByteBuffer#getDouble()}.
     *
     * @return The value of the next double from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final double readDouble() throws IOException {
        ensureBufferContains(Double.SIZE / Byte.SIZE);
        return buffer.getDouble();
    }

    /**
     * Reads the given amount of bytes from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(byte[], int, int)} with a new array.
     *
     * @param  length The number of bytes to read.
     * @return The next bytes in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final byte[] readBytes(final int length) throws IOException {
        final byte[] array = new byte[length];
        readFully(array);
        return array;
    }

    /**
     * Reads the given amount of characters from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(char[], int, int)} with a new array.
     *
     * @param  length The number of characters to read.
     * @return The next characters in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final char[] readChars(final int length) throws IOException {
        final char[] array = new char[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given amount of shorts from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(short[], int, int)} with a new array.
     *
     * @param  length The number of shorts to read.
     * @return The next shorts in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final short[] readShorts(final int length) throws IOException {
        final short[] array = new short[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given amount of integers from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(int[], int, int)} with a new array.
     *
     * @param  length The number of integers to read.
     * @return The next integers in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final int[] readInts(final int length) throws IOException {
        final int[] array = new int[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given amount of longs from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(long[], int, int)} with a new array.
     *
     * @param  length The number of longs to read.
     * @return The next longs in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final long[] readLongs(final int length) throws IOException {
        final long[] array = new long[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given amount of floats from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(float[], int, int)} with a new array.
     *
     * @param  length The number of floats to read.
     * @return The next floats in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final float[] readFloats(final int length) throws IOException {
        final float[] array = new float[length];
        readFully(array, 0, length);
        return array;
    }

    /**
     * Reads the given amount of doubles from the stream and returns them in a newly allocated array.
     * This is a convenience method for {@link #readFully(double[], int, int)} with a new array.
     *
     * @param  length The number of doubles to read.
     * @return The next doubles in a newly allocated array of the given length.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
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
     * {@preformat java
     *     return readFully(dest, 0, dest.length);
     * }
     *
     * @param  dest An array of bytes to be written to.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final byte[] dest) throws IOException {
        readFully(dest, 0, dest.length);
    }

    /**
     * Reads {@code length} bytes from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of bytes to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of bytes to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
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
    abstract class ArrayReader implements DataTransfer {
        /**
         * For subclass constructors only.
         */
        ArrayReader() {
        }

        /**
         * Returns a file identifier for error messages or debugging purpose.
         */
        @Debug
        @Override
        public final String filename() {
            return filename;
        }

        /**
         * Transfers the data from the buffer created by {@link #createView()} to array
         * of primitive Java type known by the subclass. This method may be invoked an
         * arbitrary amount of time.
         */
        abstract void transfer(int offset, int n);

        /**
         * Skips the given amount of bytes in the buffer. It is caller responsibility to ensure
         * that there is enough bytes remaining in the buffer.
         */
        private void skipInBuffer(final int n) {
            buffer.position(buffer.position() + n);
        }

        /**
         * Moves to the given position in the stream.
         */
        @Override
        public final void seek(long n) throws IOException {
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
         * @param  view     Existing buffer to use as a view over {@link ChannelDataInput#buffer}, or {@code null}.
         * @param  offset   The starting position within {@code dest} to write.
         * @param  length   The number of values to read.
         * @throws IOException if an error (including EOF) occurred while reading the stream.
         */
        @Override
        public void readFully(Buffer view, int offset, int length) throws IOException {
            final int dataSizeShift = dataSizeShift();
            ensureBufferContains(Math.min(length << dataSizeShift, buffer.capacity()));
            if (view == null) {
                view = createView();                                    // Must be after ensureBufferContains(int).
            } else {
                // Buffer position must be a multiple of the data size.
                // If not, fix that by shifting the content to index 0.
                if ((buffer.position() & ((1 << dataSizeShift) - 1)) != 0) {
                    bufferOffset += buffer.position();
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
        /** The array where to store the values. */      private byte[] dest;
        BytesReader(final byte[] dest)                  {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 0;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return buffer;}
        @Override public Buffer createView()            {return buffer;}
        @Override public void   createDataArray(int n)  {dest = new byte[n];}
        @Override        void   transfer(int p, int n)  {buffer.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (byte[]) array;};
        @Override public void readFully(Buffer view, int offset, int length) throws IOException {
            ChannelDataInput.this.readFully(dest, offset, length);
        }
    };

    /**
     * Reads characters from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class CharsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private CharBuffer view;
        /** The array where to store the values.   */    private char[] dest;
        CharsReader(final CharBuffer source)            {this.view = source;}
        CharsReader(final char[] dest)                  {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 1;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asCharBuffer();}
        @Override public void   createDataArray(int n)  {dest = new char[n];}
        @Override        void   transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (char[]) array;};
    };

    /**
     * Reads short integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class ShortsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private ShortBuffer view;
        /** The array where to store the values.   */    private short[] dest;
        ShortsReader(final ShortBuffer source)          {this.view = source;}
        ShortsReader(final short[] dest)                {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 1;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asShortBuffer();}
        @Override public void   createDataArray(int n)  {dest = new short[n];}
        @Override void          transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (short[]) array;};
    };

    /**
     * Reads integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class IntsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private IntBuffer view;
        /** The array where to store the values.   */    private int[] dest;
        IntsReader(final IntBuffer source)              {this.view = source;}
        IntsReader(final int[] dest)                    {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 2;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asIntBuffer();}
        @Override public void   createDataArray(int n)  {dest = new int[n];}
        @Override void          transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (int[]) array;};
    };

    /**
     * Reads long integers from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class LongsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private LongBuffer view;
        /** The array where to store the values.   */    private long[] dest;
        LongsReader(final LongBuffer source)            {this.view = source;}
        LongsReader(final long[] dest)                  {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 3;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asLongBuffer();}
        @Override public void   createDataArray(int n)  {dest = new long[n];}
        @Override void          transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (long[]) array;};
    };

    /**
     * Reads float values from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class FloatsReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private FloatBuffer view;
        /** The array where to store the values.   */    private float[] dest;
        FloatsReader(final FloatBuffer source)          {this.view = source;}
        FloatsReader(final float[] dest)                {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 2;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asFloatBuffer();}
        @Override public void   createDataArray(int n)  {dest = new float[n];}
        @Override void          transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (float[]) array;};
    };

    /**
     * Reads double values from the enclosing stream and stores them into the given destination array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final class DoublesReader extends ArrayReader {
        /** A view over the enclosing byte buffer. */    private DoubleBuffer view;
        /** The array where to store the values.   */    private double[] dest;
        DoublesReader(final DoubleBuffer source)        {this.view = source;}
        DoublesReader(final double[] dest)              {this.dest = dest;}
        @Override public int    dataSizeShift()         {return 3;}
        @Override public Object dataArray()             {return dest;}
        @Override public Buffer view()                  {return view;}
        @Override public Buffer createView()            {return view = buffer.asDoubleBuffer();}
        @Override public void   createDataArray(int n)  {dest = new double[n];}
        @Override void          transfer(int p, int n)  {view.get(dest, p, n);}
        @Override public void   setDest(Object array)   {dest = (double[]) array;};
    };

    /**
     * Reads {@code length} characters from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of characters to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of characters to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
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
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final short[] dest, final int offset, final int length) throws IOException {
        new ShortsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} integers from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of integers to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of integers to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final int[] dest, final int offset, final int length) throws IOException {
        new IntsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} long integers from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of long integers to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of long integers to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final long[] dest, final int offset, final int length) throws IOException {
        new LongsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} floats from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of floats to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of floats to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final float[] dest, final int offset, final int length) throws IOException {
        new FloatsReader(dest).readFully(null, offset, length);
    }

    /**
     * Reads {@code length} doubles from the stream, and stores them into
     * {@code dest} starting at index {@code offset}.
     *
     * @param  dest   An array of doubles to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The number of doubles to read.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    public final void readFully(final double[] dest, final int offset, final int length) throws IOException {
        new DoublesReader(dest).readFully(null, offset, length);
    }

    /**
     * Decodes a string from a sequence of bytes in the given encoding. This method tries to avoid the creation
     * of a temporary {@code byte[]} array when possible.
     *
     * <p>This convenience method shall be used only for relatively small amount of {@link String} instances
     * to decode, for example attribute values in the file header. For large amount of data, consider using
     * {@link java.nio.charset.CharsetDecoder} instead.</p>
     *
     * @param  length   Number of bytes to read.
     * @param  encoding The character encoding.
     * @return The string decoded from the {@code length} next bytes.
     * @throws IOException If an error occurred while reading the bytes, or if the given encoding is invalid.
     */
    public final String readString(final int length, final String encoding) throws IOException {
        if (buffer.hasArray() && length <= buffer.capacity()) {
            ensureBufferContains(length);
            final int position = buffer.position(); // Must be after 'ensureBufferContains(int)'.
            buffer.position(position + length);     // Before 'new String' for consistency with the 'else' block in case of UnsupportedEncodingException.
            return new String(buffer.array(), buffer.arrayOffset() + position, length, encoding);
        } else {
            return new String(readBytes(length), encoding);
        }
    }

    /**
     * Moves to the given position in the stream, relative to the stream position at construction time.
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
        } else if (channel instanceof FileChannel) {
            /*
             * Requested position is outside the current limits of the buffer,
             * but we can set the new position directly in the channel. Note
             * that StorageConnector.rewind() needs the buffer content to be
             * valid as a result of this seek, so we reload it immediately.
             */
            ((FileChannel) channel).position(channelOffset + position);
            bufferOffset = position;
            buffer.clear().limit(0);
        } else if (p >= 0) {
            /*
             * Requested position is after the current buffer limits and
             * we can not seek, so we have to read everything before.
             */
            do {
                bufferOffset += buffer.limit();
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
             * Requested position is before the current buffer limits
             * and we can not seek.
             */
            throw new IOException(Errors.format(Errors.Keys.StreamIsForwardOnly_1, filename));
        }
        clearBitOffset();
    }
}
