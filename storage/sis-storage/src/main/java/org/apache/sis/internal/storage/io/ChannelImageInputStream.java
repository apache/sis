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
package org.apache.sis.internal.storage.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;


/**
 * Adds the missing methods in {@code ChannelDataInput} for implementing the {@code ImageInputStream} interface.
 * The JDK approach for creating an image input stream from a channel would be as below:
 *
 * {@preformat java
 *     ReadableByteChannel channel = ...;
 *     ImageInputStream stream = ImageIO.createImageInputStream(Channels.newInputStream(channel));
 * }
 *
 * However the standard {@link javax.imageio.stream.ImageInputStreamImpl} implementation performs many work by itself,
 * including supporting various {@linkplain ByteOrder byte order}, which could be more efficiently done by NIO.
 * Furthermore, this class allows us to reuse an existing buffer (especially direct buffer, which are costly to create)
 * and allow subclasses to store additional information, for example the file path.
 *
 * <p>This class is used when compatibility with {@link javax.imageio.ImageReader} is needed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see javax.imageio.stream.FileImageInputStream
 * @see javax.imageio.ImageIO#createImageInputStream(Object)
 * @see java.nio.channels.Channels#newInputStream(ReadableByteChannel)
 *
 * @since 0.3
 * @module
 */
public class ChannelImageInputStream extends ChannelDataInput implements ImageInputStream {
    /**
     * Creates a new input stream for the given channel and using the given buffer.
     *
     * @param  filename  a file identifier used only for formatting error message.
     * @param  channel   the channel from where data are read.
     * @param  buffer    the buffer where to copy the data.
     * @param  filled    {@code true} if the buffer already contains data, or {@code false} if it needs
     *                   to be initially filled with some content read from the channel.
     * @throws IOException if an error occurred while reading the channel.
     */
    public ChannelImageInputStream(final String filename, final ReadableByteChannel channel,
            final ByteBuffer buffer, final boolean filled) throws IOException
    {
        super(filename, channel, buffer, filled);
    }

    /**
     * Creates a new input stream from the given {@code ChannelDataInput}.
     * This constructor is invoked when we need to change the implementation class
     * from {@code ChannelDataInput} to {@code ChannelImageInputStream}.
     *
     * @param  input  the existing instance from which to takes the channel and buffer.
     * @throws IOException if an error occurred while reading the channel.
     */
    public ChannelImageInputStream(final ChannelDataInput input) throws IOException {
        super(input.filename, input.channel, input.buffer, true);
    }

    /**
     * Sets the desired byte order for future reads of data values from this stream.
     * The default value is {@link ByteOrder#BIG_ENDIAN}.
     *
     * @param  byteOrder  the new {@linkplain #buffer buffer} byte order.
     */
    @Override
    public final void setByteOrder(final ByteOrder byteOrder) {
        buffer.order(byteOrder);
    }

    /**
     * Returns the byte order with which data values will be read from this stream.
     * This is the {@linkplain #buffer buffer} byte order.
     *
     * @return the {@linkplain #buffer buffer} byte order.
     */
    @Override
    public final ByteOrder getByteOrder() {
        return buffer.order();
    }

    /**
     * Reads a byte from the stream and returns a {@code true} if it is nonzero, {@code false} otherwise.
     * The implementation is as below:
     *
     * {@preformat java
     *     return readByte() != 0;
     * }
     *
     * @return the value of the next boolean from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
     * Reads in a string that has been encoded using a UTF-8 string.
     *
     * @return the string reads from the stream.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
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
     * Reads the new bytes until the next EOL. This method can read only US-ASCII strings.
     * This method is provided for compliance with the {@link java.io.DataInput} interface,
     * but is generally not recommended.
     *
     * @return the next line, or {@code null} if the EOF has been reached.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final String readLine() throws IOException {
        int c = read();
        if (c < 0) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        line.append((char) c);
loop:   while ((c = read()) >= 0) {
            switch (c) {
                case '\r': {
                    c = read();
                    if (c >= 0 && c != '\n') {
                        pushBack();
                    }
                    break loop;
                }
                case '\n': {
                    break loop;
                }
            }
            line.append((char) c);
        }
        return line.toString();
    }

    /**
     * Returns the next byte from the stream as an unsigned integer between 0 and 255,
     * or -1 if we reached the end of stream.
     *
     * @return the next byte as an unsigned integer, or -1 on end of stream.
     * @throws IOException if an error occurred while reading the stream.
     */
    @Override
    public final int read() throws IOException {
        return hasRemaining() ? Byte.toUnsignedInt(buffer.get()) : -1;
    }

    /**
     * Reads up to {@code dest.length} bytes from the stream, and stores them into
     * {@code dest} starting at index 0. The default implementation is as below:
     *
     * {@preformat java
     *     return read(dest, 0, dest.length);
     * }
     *
     * @param  dest  an array of bytes to be written to.
     * @return the number of bytes actually read, or -1 on EOF.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final int read(final byte[] dest) throws IOException {
        return read(dest, 0, dest.length);
    }

    /**
     * Reads up to {@code length} bytes from the stream, and stores them into {@code dest}
     * starting at index {@code offset}. If no bytes can be read because the end of the stream
     * has been reached, -1 is returned.
     *
     * @param  dest    an array of bytes to be written to.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the maximum number of bytes to read.
     * @return the number of bytes actually read, or -1 on EOF.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final int read(final byte[] dest, int offset, int length) throws IOException {
        if (!hasRemaining()) {
            return -1;
        }
        final int requested = length;
        while (length != 0 && hasRemaining()) {
            final int n = Math.min(buffer.remaining(), length);
            buffer.get(dest, offset, n);
            offset += n;
            length -= n;
        }
        return requested - length;
    }

    /**
     * Reads up to {@code length} bytes from the stream, and modifies the supplied
     * {@code IIOByteBuffer} to indicate the byte array, offset, and length where
     * the data may be found.
     *
     * @param  dest    the buffer to be written to.
     * @param  length  the maximum number of bytes to read.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final void readBytes(final IIOByteBuffer dest, int length) throws IOException {
        final byte[] data = new byte[length];
        length = read(data);
        dest.setData(data);
        dest.setOffset(0);
        dest.setLength(length);
    }

    /**
     * Skips over <var>n</var> bytes of data from the input stream.
     * A negative value move backward in the input stream.
     *
     * <h4>Design note</h4>
     * A previous version was skipping no more bytes than the buffer capacity.
     * But experience shows that various {@code ImageReader} implementations outside Apache SIS
     * expect that we skip exactly the specified amount of bytes and ignore the returned value.
     *
     * @param  n  maximal number of bytes to skip. Can be negative.
     * @return number of bytes actually skipped.
     * @throws IOException if an error occurred while reading.
     */
    @Override
    public final int skipBytes(int n) throws IOException {
        if (n != 0) {
            long p = buffer.position() + n;
            if (p >= 0 && p <= buffer.limit()) {
                buffer.position((int) p);
                clearBitOffset();
            } else {
                final long offset = getStreamPosition();
                p = Math.max(Math.addExact(offset, n), 0);
                final long length = length();
                if (length >= offset && p > length) {
                    p = length;
                }
                n = Math.toIntExact(p - offset);
                seek(p);
            }
        }
        return n;
    }

    /**
     * Advances the current stream position by the given amount of bytes.
     * The bit offset is reset to 0 by this method.
     *
     * @param  n  the number of bytes to seek forward.
     * @return the number of bytes skipped.
     * @throws IOException if an error occurred while skipping.
     */
    @Override
    public final long skipBytes(final long n) throws IOException {
        return skipBytes((int) Math.min(n, Integer.MAX_VALUE));
    }

    /**
     * Discards the initial position of the stream prior to the current stream position.
     * The implementation is as below:
     *
     * {@preformat java
     *     flushBefore(getStreamPosition());
     * }
     *
     * @throws IOException if an I/O error occurred.
     */
    @Override
    public final void flush() throws IOException {
        flushBefore(getStreamPosition());
    }

    /**
     * Synonymous of {@link #isCachedMemory()} since the caching behavior of this class is uniquely determined
     * by the policy that we choose for {@code isCachedMemory()}. This class never creates temporary files.
     *
     * @see #isCachedMemory()
     * @see #isCachedFile()
     */
    @Override
    public final boolean isCached() {
        return isCachedMemory();
    }

    /**
     * Returns {@code false} since this {@code ImageInputStream} does not cache data itself in order to
     * allow {@linkplain #seek(long) seeking} backwards. Actually, we could consider the {@link #buffer}
     * as a cache in main memory. But this buffer has a maximal capacity, which would be a violation of
     * {@code ImageInputStream} contract.
     *
     * @return {@code false} since this {@code ImageInputStream} does not caches data in main memory
     *         (ignoring the {@link #buffer}).
     */
    @Override
    public final boolean isCachedMemory() {
        return false;
    }

    /**
     * Returns {@code false} since this {@code ImageInputStream} does not cache data in a temporary file.
     *
     * @return {@code false} since this {@code ImageInputStream} does not cache data in a temporary file.
     */
    @Override
    public final boolean isCachedFile() {
        return false;
    }

    /**
     * Closes the {@linkplain #channel}.
     * If the channel is backed by an {@link java.io.InputStream}, that stream will be closed too.
     *
     * <h4>Departure from Image I/O standard implementation</h4>
     * Java Image I/O wrappers around input/output streams do not close the underlying stream (see for example
     * {@link javax.imageio.stream.FileCacheImageInputStream#close()} specification). But Apache SIS needs the
     * underlying stream to be closed because we do not keep reference to the original input stream. Note that
     * channels created by {@link java.nio.channels.Channels#newChannel(java.io.InputStream)} close the stream,
     * which is the desired behavior for this method.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public final void close() throws IOException {
        channel.close();
    }
}
