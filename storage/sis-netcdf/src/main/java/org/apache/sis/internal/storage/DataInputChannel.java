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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;


/**
 * Provides convenience methods for working with a ({@link ReadableByteChannel}, {@link ByteBuffer}) pair.
 * The channel and the buffer must be supplied by the caller. It is okay if they have already been used
 * before {@code DataInputChannel} creation.
 *
 * {@note This class provides no <code>close()</code> method since it doesn't "own" the channel.}
 *
 * {@section Relationship with <code>DataInput</code>}
 * This class API is compatibly with the {@link java.io.DataInput} interface, so subclasses can implement that
 * interface if they wish. This class does not implement {@code DataInput} itself because it is not needed for
 * SIS purposes, and because {@code DataInput} has undesirable methods ({@code readLine()} and {@code readUTF()}).
 * However the {@code DataInputChannelCompleted} class in the test directory implements the {@code DataInput}
 * interface, both for testing API compatibility and in case we choose to implement that interface after all
 * in a future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 */
public class DataInputChannel {
    /**
     * A file identifier used only for formatting error message.
     */
    public final String filename;

    /**
     * The channel from where data are read.
     * This is supplied at construction time.
     */
    public final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel to memory.
     */
    public final ByteBuffer buffer;

    /**
     * The position of the channel when this {@code DataInputChannel} has been created.
     * This is almost always 0, but we allow other values in case the data to read are
     * encompassed inside a bigger file.
     */
    private final long channelOffset;

    /**
     * The position in {@link #channel} where is located the {@link #buffer} value at index 0.
     * This is initially zero and shall be updated every time {@link ByteBuffer#compact()} or
     * {@link ByteBuffer#clear()} is invoked.
     */
    private long bufferOffset;

    /**
     * Creates a new input source for the given channel and using the given buffer.
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
    public DataInputChannel(final String filename, final ReadableByteChannel channel, final ByteBuffer buffer,
            final boolean filled) throws IOException
    {
        this.filename      = filename;
        this.channel       = channel;
        this.buffer        = buffer;
        this.channelOffset = (channel instanceof SeekableByteChannel) ? ((SeekableByteChannel) channel).position() : 0;
        if (!filled) {
            channel.read(buffer);
            buffer.flip();
        }
    }

    /**
     * Invoked when a call to {@link ReadableByteChannel#read(ByteBuffer)} has returned zero.
     * Note that this is unrelated to end-of-file, in which case {@code read} returns -1.
     * A return value of 0 happen for example if the channel is a socket in non-blocking mode
     * and the socket buffer has not yet received new data.
     *
     * <p>The current implementation sleeps an arbitrary amount of time before to allow a new try.
     * We do that in order to avoid high CPU consumption when data are expected to take more than
     * a few nanoseconds to arrive.</p>
     *
     * @throws IOException If the implementation chooses to stop the reading process.
     */
    protected void onEmptyChannelBuffer() throws IOException {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Someone doesn't want to let us sleep. Go back to work.
        }
    }

    /**
     * Returns {@code true} if the buffer or the channel has at least one byte remaining.
     * If the {@linkplain #buffer} has no remaining bytes, then this method will attempts
     * to read at least one byte from the {@linkplain #channel}. If no bytes can be read
     * because the channel has reached the end of stream, then this method returns {@code false}.
     *
     * @return {@code true} if the buffer contains at least one remaining byte.
     * @throws IOException If it was necessary to read from the channel and this operation failed.
     */
    public final boolean hasRemaining() throws IOException {
        if (buffer.hasRemaining()) {
            return true;
        }
        bufferOffset += buffer.position();
        buffer.clear();
        int c = channel.read(buffer);
        while (c == 0) {
            onEmptyChannelBuffer();
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
     * @param  n The minimal number of bytes needed in the {@linkplain #buffer}.
     * @throws EOFException If the channel has reached the end of stream.
     * @throws IOException If an other kind of error occurred while reading.
     */
    public final void ensureBufferContains(int n) throws IOException {
        assert n <= buffer.capacity() : n;
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
                    onEmptyChannelBuffer();
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
     *     return readByte() & 0xFF;
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
     *     return readShort() & 0xFFFF;
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
     *     return readInt() & 0xFFFFFFFFL;
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
     * Moves to the given position in the stream, relative to the stream position at construction time.
     *
     * @param  position The position where to move.
     * @throws IOException If the stream can not be moved to the given position.
     */
    public final void seek(final long position) throws IOException {
        long p = position - bufferOffset;
        if (p >= 0 && p <= buffer.limit()) {
            /*
             * Requested position is inside the current limits of the buffer.
             */
            buffer.position((int) p);
        } else if (channel instanceof SeekableByteChannel) {
            /*
             * Requested position is outside the current limits of the buffer,
             * but we can set the new position directly in the channel.
             */
            ((SeekableByteChannel) channel).position(channelOffset + position);
            bufferOffset = position;
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
        } else if (p >= 0) {
            /*
             * Requested position is after the current buffer limits and
             * we can not seek, so we have to read everything before.
             */
            do {
                bufferOffset += buffer.position();
                buffer.clear();
                final int c = channel.read(buffer);
                if (c < 0) {
                    throw new EOFException(eof());
                }
                p -= c;
            } while (p > buffer.limit());
            buffer.flip().position((int) p);
        } else {
            /*
             * Requested position is before the current buffer limits
             * and we can not seek.
             */
            throw new IOException(Errors.format(Errors.Keys.StreamIsForwardOnly_1, filename));
        }
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[“" + filename + "” at " + bufferOffset + buffer.position() + ']';
    }
}
