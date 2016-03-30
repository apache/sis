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
import java.nio.InvalidMarkException;
import java.nio.channels.Channel;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureBetween;

// Branch-dependent imports
import java.nio.channels.FileChannel;


/**
 * Properties common to {@link ChannelDataInput} and {@link ChannelDataOutput}.
 * Common properties include a reference to a {@link ByteBuffer} supplied by the user and methods for
 * querying or modifying the stream position. This class does not define any read or write operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.3)
 * @version 0.5
 * @module
 */
public abstract class ChannelData {
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
     * A file identifier used only for formatting error message.
     */
    public final String filename;

    /**
     * The buffer to use for transferring data between the channel to memory.
     */
    public final ByteBuffer buffer;

    /**
     * The position of the channel when this {@code ChannelData} has been created.
     * This is almost always 0, but we allow other values in case the data to read
     * or write are part of a bigger file.
     */
    final long channelOffset;

    /**
     * The channel position where is located the {@link #buffer} value at index 0.
     * This is initially zero and shall be incremented as below:
     *
     * <ul>
     *   <li>By {@link ByteBuffer#position()} every time {@link ByteBuffer#compact()} is invoked.</li>
     *   <li>By {@link ByteBuffer#limit()}    every time {@link ByteBuffer#clear()}   is invoked.</li>
     * </ul>
     */
    long bufferOffset;

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
     * A mark pushed by the {@link ChannelData#mark()} method
     * and pooled by the {@link ChannelData#reset()} method.
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
     * Creates a new instance for the given channel and using the given buffer.
     * The channel is not stored by this class - it shall be stored by the subclass.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read or where to wrote.
     * @param  buffer   The buffer where to store the data.
     * @throws IOException If an error occurred while reading the channel.
     */
    ChannelData(final String filename, final Channel channel, final ByteBuffer buffer) throws IOException {
        this.filename      = filename;
        this.buffer        = buffer;
        this.channelOffset = (channel instanceof FileChannel) ? ((FileChannel) channel).position() : 0;
    }

    /**
     * Returns the current bit offset, as an integer between 0 and 7 inclusive.
     *
     * <p>According {@link javax.imageio.stream.ImageInputStream} contract, the bit offset shall be reset to 0
     * by every call to any {@code read} or {@code write} method except {@code readBit()}, {@code readBits(int)},
     * {@code writeBit(int)} and {@code writeBits(long, int)}.</p>
     *
     * @return The bit offset of the stream.
     */
    public final int getBitOffset() {
        final long position = bufferOffset + buffer.position();
        if ((bitPosition >>> BIT_OFFSET_SIZE) != position) {
            bitPosition = position << BIT_OFFSET_SIZE;
        }
        return (int) (bitPosition & ((1 << BIT_OFFSET_SIZE) - 1));
    }

    /**
     * Sets the bit offset to the given value.
     *
     * @param bitOffset The new bit offset of the stream.
     */
    public final void setBitOffset(final int bitOffset) {
        ensureBetween("bitOffset", 0, Byte.SIZE - 1, bitOffset);
        final long position = bufferOffset + buffer.position();
        bitPosition = (position << BIT_OFFSET_SIZE) | bitOffset;
    }

    /**
     * Sets the bit offset to zero.
     */
    final void clearBitOffset() {
        bitPosition = 0;
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return The position of the stream.
     */
    public long getStreamPosition() {
        return bufferOffset + buffer.position();
    }

    /**
     * Sets the current byte position of the stream. This method does <strong>not</strong> seeks the stream;
     * this method only modifies the value to be returned by {@link #getStreamPosition()}. This method can
     * be invoked when some external code has performed some work with the channel and wants to inform this
     * {@code ChannelData} about the new position resulting from this work.
     *
     * <b>Notes:</b>
     * <ul>
     *   <li>Invoking this method clears the {@linkplain #getBitOffset() bit offset}
     *       and the {@linkplain #mark() marks}.</li>
     *   <li>This method does not need to be invoked when only the {@linkplain ByteBuffer#position() buffer position}
     *       has changed.</li>
     * </ul>
     *
     * @param position The new position of the stream.
     */
    public final void setStreamPosition(final long position) {
        bufferOffset = position - buffer.position();
        // Clearing the bit offset is needed if we don't want to handle the case of ChannelDataOutput,
        // which use a different stream position calculation when the bit offset is non-zero.
        clearBitOffset();
        mark = null;
    }

    /**
     * Returns the earliest position in the stream to which {@linkplain #seek(long) seeking}
     * may be performed.
     *
     * @return the earliest legal position for seeking.
     */
    public final long getFlushedPosition() {
        return bufferOffset;
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
    public final void flushBefore(final long position) throws IOException {
        final long currentPosition = getStreamPosition();
        if (position < bufferOffset || position > currentPosition) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                    "position", bufferOffset, currentPosition, position));
        }
        final int n = (int) (position - bufferOffset);
        final int p = buffer.position() - n;
        final int r = buffer.limit() - n;
        flushAndSetPosition(n);                             // Number of bytes to forget.
        buffer.compact().position(p).limit(r);
        /*
         * Discard trailing obsolete marks. Note that obsolete marks between valid marks
         * can not be discarded - only the trailing obsolete marks can be removed.
         */
        Mark lastValid = null;
        for (Mark m = mark; m != null; m = m.next) {
            if (m.position >= position) {
                lastValid = m;
            }
        }
        if (lastValid != null) {
            lastValid.next = null;
        } else {
            mark = null;
        }
    }

    /**
     * Writes (if applicable) the buffer content up to the given position, then sets the buffer position
     * to the given value. The {@linkplain ByteBuffer#limit() buffer limit} is unchanged, and the buffer
     * offset is incremented by the given value.
     */
    void flushAndSetPosition(final int position) throws IOException {
        buffer.position(position);
        bufferOffset += position;
    }

    /**
     * Moves to the given position in the stream, relative to the stream position at construction time.
     *
     * @param  position The position where to move.
     * @throws IOException If the stream can not be moved to the given position.
     */
    public abstract void seek(final long position) throws IOException;

    /**
     * Pushes the current stream position onto a stack of marked positions.
     * Note that {@code ChannelData} maintains its own marks - the buffer's
     * mark is left unchanged.
     */
    public final void mark() {
        mark = new Mark(getStreamPosition(), (byte) getBitOffset(), mark);
    }

    /**
     * Resets the current stream byte and bit positions from the stack of marked positions.
     * An {@code IOException} may be be thrown if the previous marked position lies in the
     * discarded portion of the stream.
     *
     * <p>This method differs from the {@link javax.imageio.stream.ImageInputStream} contract in two aspects:</p>
     * <ul>
     *   <li>This method may, under some conditions, be able to perform its work even if the marked
     *       position is before the flushed position.</li>
     *   <li>If there is no mark, this method throws an {@link InvalidMarkException} rather than
     *       doing nothing. Doing nothing is considered a too high risk of error.</li>
     * </ul>
     *
     * @throws IOException If an I/O error occurs.
     */
    public final void reset() throws IOException {
        final Mark m = mark;
        if (m == null) {
            throw new InvalidMarkException();
        }
        mark = m.next;
        seek(m.position);
        setBitOffset(m.bitOffset);
    }

    /**
     * Invoked when an operation between the channel and the buffer transfered no byte. Note that this is unrelated
     * to end-of-file, in which case {@link java.nio.channels.ReadableByteChannel#read(ByteBuffer)} returns -1.
     * A return value of 0 happen for example if the channel is a socket in non-blocking mode and the socket buffer
     * has not yet transfered new data.
     *
     * <p>The current implementation sleeps an arbitrary amount of time before to allow a new try.
     * We do that in order to avoid high CPU consumption when data are expected to take more than
     * a few nanoseconds to arrive.</p>
     *
     * @throws IOException If the implementation chooses to stop the process.
     */
    protected void onEmptyTransfer() throws IOException {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            /*
             * Someone doesn't want to let us sleep. Stop the reading or writing process. We don't try to go back to
             * work, because the waiting time was short and this method is invoked in loops. Consequently if the user
             * interrupted us, it is probably because he waited for a long time and we still have not transfered any
             * new data.
             */
            throw new IOException(e);
        }
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     *
     * @return A string representation of this object.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder().append(getClass().getSimpleName()).append("[“").append(filename).append('”');
        // Even if the buffer should not be null, it is useful to keep the toString() method robust.
        if (buffer != null) b.append(" at ").append(getStreamPosition());
        return b.append(']').toString();
    }
}
