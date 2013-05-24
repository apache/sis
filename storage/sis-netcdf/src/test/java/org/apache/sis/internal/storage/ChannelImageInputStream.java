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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;

import static org.apache.sis.util.ArgumentChecks.ensureBetween;


/**
 * Adds the missing methods in {@code ChannelDataInput} for implementing the {@code ImageInputStream} interface.
 * This class is provided for testing the compatibility of {@code ChannelDataInput} API with {@code ImageInputStream},
 * and as a placeholder in case we want to move this implementation in the main code in a future SIS version.
 *
 * <p>Note that the JDK approach for creating an image input stream from a channel would be as below:</p>
 *
 * {@preformat java
 *     ReadableByteChannel channel = ...;
 *     ImageInputStream stream = ImageIO.createImageInputStream(Channels.newInputStream(channel));
 * }
 *
 * However the standard {@link javax.imageio.stream.ImageInputStreamImpl} implementation performs many work by itself,
 * including supporting various {@linkplain ByteOrder byte order}, which could be more efficiently done by NIO.
 *
 * <p>This class is provided as a <em>proof of concept</em> only - it is not intended to be used in the main SIS code.
 * This class lives in the {@code test} directory for that reason, where the "test" is to ensure that the API of the
 * {@link ChannelDataInput} base class is compatible with the {@link ImageInputStream} API.</p>
 *
 * <p>Note in particular that the {@link #readBit()} and {@link #readBits(int)} methods are not strictly compliant
 * to the {@code ImageInputStream} contract.</p>
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
public class ChannelImageInputStream extends ChannelDataInputCompleted implements ImageInputStream {
    /**
     * The current bit offset within the stream.
     *
     * @see #getBitOffset()
     */
    private int bitOffset;

    /**
     * The most recent mark, or {@code null} if none.
     */
    private Mark mark;

    /**
     * A mark pushed by the {@link ChannelImageInputStream#mark()} method
     * and pooled by the {@link ChannelImageInputStream#reset()} method.
     */
    private static final class Mark {
        final long position;
        final int  bitOffset;
        final Mark next;

        Mark(long position, int bitOffset, Mark next) {
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
     * {@section Contract violation}
     * According {@link ImageInputStream} contract, the bit offset shall be reset to 0 by every call to
     * any {@code read} method except {@code readBit()} and {@link #readBits(int)}. This is not done in
     * this {@code ChannelImageInputStream} class because the {@code read} methods are final, and we
     * don't want to alter the main SIS code just for this "proof of concept" class.
     *
     * @return The bit offset of the stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public final int getBitOffset() throws IOException {
        return bitOffset;
    }

    /**
     * Sets the bit offset to the given value.
     *
     * {@section Contract violation}
     * According {@link ImageInputStream} contract, the bit offset shall be reset to 0 by every call to
     * any {@code read} method except {@code readBit()} and {@link #readBits(int)}. This is not done in
     * this {@code ChannelImageInputStream} class because the {@code read} methods are final, and we
     * don't want to alter the main SIS code just for this "proof of concept" class.
     *
     * @param bitOffset The new bit offset of the stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public final void setBitOffset(final int bitOffset) throws IOException {
        ensureBetween("bitOffset", 0, Byte.SIZE-1, bitOffset);
        this.bitOffset = bitOffset;
    }

    /**
     * Reads a single bit from the stream. The bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * {@section Contract violation}
     * According {@link ImageInputStream} contract, the bit offset shall be reset to 0 by every call to
     * any {@code read} method except {@code readBit()} and {@link #readBits(int)}. This is not done in
     * this {@code ChannelImageInputStream} class because the {@code read} methods are final, and we
     * don't want to alter the main SIS code just for this "proof of concept" class.
     *
     * @return The value of the next bit from the stream.
     * @throws IOException If an error occurred while reading (including EOF).
     */
    @Override
    public final int readBit() throws IOException {
        int value = readUnsignedByte();
        final int toShift = (Byte.SIZE - ++bitOffset);
        if (toShift == 0) {
            bitOffset = 0;
        } else {
            pushBack();
            value >>= toShift;
        }
        return value & 1;
    }

    /**
     * Reads many bits from the stream. The first bit to be read depends on the
     * {@linkplain #getBitOffset() current bit offset}.
     *
     * {@section Contract violation}
     * According {@link ImageInputStream} contract, the bit offset shall be reset to 0 by every call to
     * any {@code read} method except {@code readBit()} and {@link #readBits(int)}. This is not done in
     * this {@code ChannelImageInputStream} class because the {@code read} methods are final, and we
     * don't want to alter the main SIS code just for this "proof of concept" class.
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
        long value = readByte() & (0xFF >>> bitOffset);
        numBits -= (Byte.SIZE - bitOffset);
        while (numBits > 0) {
            value = (value << Byte.SIZE) | readUnsignedByte();
            numBits -= Byte.SIZE;
        }
        if (numBits != 0) {
            value >>>= (-numBits); // Discard the unwanted bits.
            bitOffset = Byte.SIZE + numBits;
            pushBack();
        } else {
            bitOffset = 0;
        }
        return value;
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
        bitOffset = 0;
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
     * Advances the current stream position by the given amount of bytes.
     * The bit offset is reset to 0 by this method.
     *
     * @param  n The number of bytes to seek forward.
     * @return The number of bytes skipped.
     * @throws IOException If an error occurred while skiping.
     */
    @Override
    public final long skipBytes(final long n) throws IOException {
        bitOffset = 0;
        if (n > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException();
        }
        return skipBytes((int) n);
    }

    /**
     * Pushes the current stream position onto a stack of marked positions.
     */
    @Override
    public final void mark() {
        mark = new Mark(getStreamPosition(), bitOffset, mark);
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
        bitOffset = mark.bitOffset;
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
     * @param  pos The length of the stream prefix that may be flushed.
     * @throws IOException If an I/O error occurred.
     */
    @Override
    public final void flushBefore(long pos) throws IOException {
        // No-op for now.
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream} caches data itself in order to
     * allow {@linkplain #seek(long) seeking} backwards.
     *
     * @return {@code true} If this {@code ImageInputStream} caches data.
     *
     * @see #isCachedMemory()
     * @see #isCachedFile()
     */
    @Override
    public final boolean isCached() {
        return false;
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream} caches data itself in order to
     * allow {@linkplain #seek(long) seeking} backwards, and the cache is kept in main memory.
     *
     * @return {@code true} if this {@code ImageInputStream} caches data in main memory.
     */
    @Override
    public final boolean isCachedMemory() {
        return false;
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream} caches data itself in order to allow
     * {@linkplain #seek(long) seeking} backwards, and the cache is kept in a temporary file.
     *
     * @return {@code true} if this {@code ImageInputStream} caches data in a temporary file.
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
