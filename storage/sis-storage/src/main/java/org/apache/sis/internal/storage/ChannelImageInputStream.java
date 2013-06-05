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

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureBetween;

// Related to JDK7
import java.nio.channels.FileChannel;


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
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 *
 * @see javax.imageio.stream.FileImageInputStream
 * @see javax.imageio.ImageIO#createImageInputStream(Object)
 * @see java.nio.channels.Channels#newInputStream(ReadableByteChannel)
 */
public class ChannelImageInputStream extends ChannelDataInput implements ImageInputStream {
    /**
     * Number of bits needed for storing the bit offset in {@link #bitPosition}.
     * The following condition must hold:
     *
     * {@preformat java
     *     (1 << BIT_OFFSET_SIZE) == Byte.SIZE
     * }
     */
    private static final int BIT_OFFSET_SIZE = 3;

    /**
     * The current bit position within the stream. The 3 lowest bits are the bit offset,
     * and the remaining of the {@code long} value is the stream position where the bit
     * offset is valid.
     *
     * @see #getBitOffset()
     */
    private long bitPosition;

    /**
     * The most recent mark, or {@code null} if none.
     * This is the tail of a chained list of marks.
     */
    private Mark mark;

    /**
     * A mark pushed by the {@link ChannelImageInputStream#mark()} method
     * and pooled by the {@link ChannelImageInputStream#reset()} method.
     */
    private static final class Mark {
        final long position;
        final byte bitOffset;
        Mark next;

        Mark(long position, byte bitOffset, Mark next) {
            this.position  = position;
            this.bitOffset = bitOffset;
            this.next      = next;
        }
    }

    /**
     * Creates a new input source for the given channel and using the given buffer.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read.
     * @param  buffer   The buffer where to copy the data.
     * @param  filled   {@code true} if the buffer already contains data, or {@code false} if it needs
     *                  to be initially filled with some content read from the channel.
     * @throws IOException If an error occurred while reading the channel.
     */
    public ChannelImageInputStream(final String filename, final ReadableByteChannel channel,
            final ByteBuffer buffer, final boolean filled) throws IOException
    {
        super(filename, channel, buffer, filled);
    }

    /**
     * Creates a new input source from the given {@code ChannelDataInput}.
     * This constructor is invoked when we need to change the implementation class
     * from {@code ChannelDataInput} to {@code ChannelImageInputStream}.
     *
     * @param  input The existing instance from which to takes the channel and buffer.
     * @throws IOException If an error occurred while reading the channel.
     */
    public ChannelImageInputStream(final ChannelDataInput input) throws IOException {
        super(input.filename, input.channel, input.buffer, true);
    }

    /**
     * Sets the desired byte order for future reads of data values from this stream.
     * The default value is {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
     */
    @Override
    public final void setByteOrder(final ByteOrder byteOrder) {
        buffer.order(byteOrder);
    }

    /**
     * Returns the byte order with which data values will be read from this stream.
     */
    @Override
    public final ByteOrder getByteOrder() {
        return buffer.order();
    }

    /**
     * Returns the length of the stream (in bytes), or -1 if unknown.
     *
     * @throws IOException If an error occurred while fetching the stream length.
     */
    @Override
    public final long length() throws IOException {
        if (channel instanceof FileChannel) {
            return ((FileChannel) channel).size();
        }
        return -1;
    }

    /**
     * Returns the earliest position in the stream to which {@linkplain #seek(long) seeking}
     * may be performed.
     *
     * @return the earliest legal position for seeking.
     */
    @Override
    public final long getFlushedPosition() {
        return getStreamPosition() - buffer.position();
    }

    /**
     * Push back the last processed byte. This is used when a call to {@link #readBit()}
     * did not used every bits in a byte, or when {@link #readLine()} checked for the
     * Windows-style of EOL.
     */
    private void pushBack() {
        buffer.position(buffer.position() - 1);
    }

    /**
     * Returns the current bit offset, as an integer between 0 and 7 inclusive.
     *
     * <p>According {@link ImageInputStream} contract, the bit offset shall be reset to 0 by every call to
     * any {@code read} method except {@code readBit()} and {@link #readBits(int)}.</p>
     *
     * @return The bit offset of the stream.
     */
    @Override
    public final int getBitOffset() {
        final long currentPosition = getStreamPosition();
        if ((bitPosition >>> BIT_OFFSET_SIZE) != currentPosition) {
            bitPosition = currentPosition << BIT_OFFSET_SIZE;
        }
        return (int) (bitPosition & (Byte.SIZE - 1));
    }

    /**
     * Sets the bit offset to the given value.
     *
     * @param bitOffset The new bit offset of the stream.
     */
    @Override
    public final void setBitOffset(final int bitOffset) {
        ensureBetween("bitOffset", 0, Byte.SIZE - 1, bitOffset);
        bitPosition = (getStreamPosition() << BIT_OFFSET_SIZE) | bitOffset;
    }

    /**
     * Reads a single bit from the stream. The bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @return The value of the next bit from the stream.
     * @throws IOException If an error occurred while reading (including EOF).
     */
    @Override
    public final int readBit() throws IOException {
        return (int) (readBits(1) & 1);
    }

    /**
     * Reads many bits from the stream. The first bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * @param  numBits The number of bits to read.
     * @return The value of the next bits from the stream.
     * @throws IOException If an error occurred while reading (including EOF).
     */
    @Override
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
     * Reads a byte from the stream and returns a {@code true} if it is nonzero, {@code false} otherwise.
     * The implementation is as below:
     *
     * {@preformat java
     *     return readByte() != 0;
     * }
     *
     * @return The value of the next boolean from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
     * Reads in a string that has been encoded using a UTF-8 string.
     *
     * @return The string reads from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
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
     * This method is provided for compliance with the {@link DataInput} interface,
     * but is generally not recommended.
     *
     * @return The next line, or {@code null} if the EOF has been reached.
     * @throws IOException If an error occurred while reading.
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
     * @return The next byte as an unsigned integer, or -1 on end of stream.
     * @throws IOException If an error occurred while reading the stream.
     */
    @Override
    public final int read() throws IOException {
        return hasRemaining() ? buffer.get() & 0xFF : -1;
    }

    /**
     * Reads up to {@code dest.length} bytes from the stream, and stores them into
     * {@code dest} starting at index 0. The default implementation is as below:
     *
     * {@preformat java
     *     return read(dest, 0, dest.length);
     * }
     *
     * @param  dest An array of bytes to be written to.
     * @return The number of bytes actually read, or -1 on EOF.
     * @throws IOException If an error occurred while reading.
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
     * @param  dest   An array of bytes to be written to.
     * @param  offset The starting position within {@code dest} to write.
     * @param  length The maximum number of bytes to read.
     * @return The number of bytes actually read, or -1 on EOF.
     * @throws IOException If an error occurred while reading.
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
     * @param  dest The buffer to be written to.
     * @param  length The maximum number of bytes to read.
     * @throws IOException If an error occurred while reading.
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
     * This implementation does not skip more bytes than the buffer capacity.
     *
     * @param  n Maximal number of bytes to skip.
     * @return Number of bytes actually skipped.
     * @throws IOException If an error occurred while reading.
     */
    @Override
    public final int skipBytes(int n) throws IOException {
        if (!hasRemaining()) {
            return 0;
        }
        int r = buffer.remaining();
        if (n >= r) {
            n = r;
        }
        buffer.position(buffer.position() + n);
        return n;
    }

    /**
     * Advances the current stream position by the given amount of bytes.
     * The bit offset is reset to 0 by this method.
     *
     * @param  n The number of bytes to seek forward.
     * @return The number of bytes skipped.
     * @throws IOException If an error occurred while skipping.
     */
    @Override
    public final long skipBytes(final long n) throws IOException {
        return skipBytes(Math.min((int) n, Integer.MAX_VALUE));
    }

    /**
     * Pushes the current stream position onto a stack of marked positions.
     */
    @Override
    public final void mark() {
        mark = new Mark(getStreamPosition(), (byte) getBitOffset(), mark);
    }

    /**
     * Resets the current stream byte and bit positions from the stack of marked positions.
     * An {@code IOException} will be thrown if the previous marked position lies in the
     * discarded portion of the stream.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public final void reset() throws IOException {
        if (mark == null) {
            throw new IOException("No marked position.");
        }
        seek(mark.position);
        setBitOffset(mark.bitOffset);
        mark = mark.next;
    }

    /**
     * Discards the initial position of the stream prior to the current stream position.
     * The implementation is as below:
     *
     * {@preformat java
     *     flushBefore(getStreamPosition());
     * }
     *
     * @throws IOException If an I/O error occurred.
     */
    @Override
    public final void flush() throws IOException {
        flushBefore(getStreamPosition());
    }

    /**
     * Discards the initial portion of the stream prior to the indicated position.
     * Attempting to {@linkplain #seek(long) seek} to an offset within the flushed
     * portion of the stream will result in an {@link IndexOutOfBoundsException}.
     *
     * <p>This method moves the data starting at the given position to the beginning of the {@link #buffer},
     * thus making more room for new data before the data at the given position is discarded.</p>
     *
     * @param  position The length of the stream prefix that may be flushed.
     * @throws IOException If an I/O error occurred.
     */
    @Override
    public final void flushBefore(final long position) throws IOException {
        final long bufferOffset    = getFlushedPosition();
        final long currentPosition = getStreamPosition();
        if (position < bufferOffset || position > currentPosition) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                    "position", bufferOffset, currentPosition, position));
        }
        final int n = (int) (position - bufferOffset);
        final int p = buffer.position() - n;
        final int r = buffer.limit() - n;
        buffer.position(n); // Number of bytes to forget.
        buffer.compact().position(p).limit(r);
        setStreamPosition(currentPosition);

        // Discard obolete marks.
        Mark parent = null;
        for (Mark m = mark; m != null; m = m.next) {
            if (m.position < position) {
                if (parent != null) {
                    parent.next = null;
                } else {
                    mark = null;
                }
                break;
            }
            parent = m;
        }
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
     *
     * @throws IOException If an error occurred while closing the channel.
     */
    @Override
    public final void close() throws IOException {
        channel.close();
    }
}
