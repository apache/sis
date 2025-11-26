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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.internal.Resources;
import static org.apache.sis.util.ArgumentChecks.ensureBetween;


/**
 * Properties common to {@link ChannelDataInput} and {@link ChannelDataOutput}.
 * Common properties include a reference to a {@link ByteBuffer} supplied by the user and methods for
 * querying or modifying the stream position. This class does not define any read or write operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ChannelData implements Markable {
    /**
     * Number of bits needed for storing the bit offset in {@link #bitPosition}.
     * The following condition must hold:
     *
     * {@snippet lang="java" :
     *     assert (1 << BIT_OFFSET_SIZE) == Byte.SIZE;
     *     }
     */
    static final int BIT_OFFSET_SIZE = 3;

    /**
     * A short identifier (typically a filename without path) used for formatting error message.
     * This is often the value given by {@link org.apache.sis.storage.StorageConnector#getStorageName()}.
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
     *
     * <p>This value is added to the argument given to the {@link #seek(long)} method. Users can ignore
     * this field, unless they want to invoke {@link SeekableByteChannel#position(long)} directly.</p>
     *
     * @see #toSeekableByteChannelPosition(long)
     */
    private long channelOffset;

    /**
     * The channel position where is located the {@link #buffer}.
     * This is initially zero and shall be incremented as below:
     *
     * <ul>
     *   <li>By {@link ByteBuffer#position()} every time {@link ByteBuffer#compact()} is invoked.</li>
     *   <li>By {@link ByteBuffer#limit()}    every time {@link ByteBuffer#clear()}   is invoked.</li>
     * </ul>
     *
     * <h4>Subclass-dependent interpretation</h4>
     * The value depends on whether the buffer is used before or after transfer from/to the channel.
     * For {@link ChannelDataOutput}, the value is the channel position of buffer position 0.
     * For {@link ChannelDataInput},  the value is the channel position of buffer limit.
     */
    long bufferOffset;

    /**
     * The current bit position within the stream. The 3 lowest bits are the bit offset,
     * and the remaining of the {@code long} value is the stream position where the bit
     * offset is valid.
     *
     * <h4>Subclass-dependent interpretation</h4>
     * Which byte contains the bits depends on whether this {@code ChannelData} is an input or an output.
     * For {@link ChannelDataOutput}, the bits are stored in the byte before the current buffer position.
     * For {@link ChannelDataInput},  the bits are stored in the byte at the current buffer position.
     *
     * @see #getBitOffset()
     * @see #setBitOffset(int)
     * @see #skipRemainingBits()
     */
    long bitPosition;

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
     * @param  filename  a short identifier (typically a filename without path) used for formatting error message.
     * @param  channel   the channel from where data are read or where to wrote.
     * @param  buffer    the buffer where to store the data.
     * @throws IOException if an error occurred while reading the channel.
     */
    ChannelData(final String filename, final Channel channel, final ByteBuffer buffer) throws IOException {
        this.filename = filename;
        this.buffer   = buffer;
        if (channel instanceof SeekableByteChannel) {
            channelOffset = ((SeekableByteChannel) channel).position();
        }
    }

    /**
     * Creates a new instance from the given {@code ChannelData}.
     * This constructor is invoked when we need to change the implementation class.
     * If {@code replacing} is {@code true}, then the old {@code ChannelData} should
     * not be used anymore after this constructor.
     *
     * @param  other      the existing instance from which to takes the channel and buffer.
     * @param  buffer     either {@code other.buffer} or a new buffer with an equivalent position.
     * @param  replacing  {@code true} if {@code other} will be discarded in favor of the new instance.
     */
    ChannelData(final ChannelData other, final ByteBuffer buffer, final boolean replacing) {
        filename      = other.filename;
        channelOffset = other.channelOffset;
        bufferOffset  = other.bufferOffset;
        bitPosition   = other.bitPosition;
        this.buffer   = buffer;
        if (replacing) {
            mark = other.mark;
        }
    }

    /**
     * Creates a new instance for a buffer filled with the bytes to use.
     * This constructor uses an independent, read-only view of the given buffer.
     * No reference to the given buffer will be retained.
     *
     * @param  filename  a short identifier (typically a filename without path) used for formatting error message.
     * @param  data      the buffer filled with all bytes to read.
     */
    ChannelData(final String filename, final ByteBuffer data) {
        this.filename = filename;
        buffer = data.asReadOnlyBuffer();
        buffer.order(data.order());
    }

    /**
     * Returns the wrapped channel from which data are read or where data are written.
     * This is the {@code channel} field of the {@code ChannelData} subclass.
     *
     * @return the channel for actual read or write operations.
     *
     * @see ChannelDataInput#channel
     * @see ChannelDataOutput#channel
     */
    public abstract Channel channel();

    /**
     * Returns the length of the stream (in bytes), or -1 if unknown.
     * The length is relative to the position during the last call to {@link #relocateOrigin()}.
     * If the latter method has never been invoked, then the length is relative to the channel
     * position at {@code ChannelData} construction time.
     *
     * @return the length of the stream (in bytes) relative to origin, or -1 if unknown.
     * @throws IOException if an error occurred while fetching the stream length.
     */
    public final long length() throws IOException {     // Method signature must match ImageInputStream.length().
        final Channel channel = channel();
        if (channel instanceof SeekableByteChannel) {
            final long length = Math.subtractExact(((SeekableByteChannel) channel).size(), channelOffset);
            if (length >= 0) {
                return Math.max(length, Math.addExact(bufferOffset, buffer.limit()));
            }
        }
        return -1;
    }

    /**
     * Returns the current bit offset, as an integer between 0 and 7 inclusive.
     *
     * <p>According {@link javax.imageio.stream.ImageInputStream} contract, the bit offset shall be reset to 0
     * by every call to any {@code read} or {@code write} method except {@code readBit()}, {@code readBits(int)},
     * {@code writeBit(int)} and {@code writeBits(long, int)}.</p>
     *
     * @return the bit offset of the stream.
     */
    public final int getBitOffset() {
        // No need to use `Math.addExact(…)` because integer overflow results in `false`.
        if ((bitPosition >>> BIT_OFFSET_SIZE) == bufferOffset + buffer.position()) {
            return (int) (bitPosition & ((1L << BIT_OFFSET_SIZE) - 1));
        }
        bitPosition = 0;    // Bits are invalid even if the user moves back to this position later.
        return 0;
    }

    /**
     * Sets the bit offset to the given value.
     * The given value shall be between 0 inclusive to {@value Byte#SIZE} exclusive.
     *
     * @param  bitOffset  the new bit offset of the stream.
     * @throws IllegalArgumentException if the given offset is out of range.
     */
    public final void setBitOffset(final int bitOffset) {
        ensureBetween("bitOffset", 0, Byte.SIZE - 1, bitOffset);
        bitPosition = (position() << BIT_OFFSET_SIZE) | bitOffset;
    }

    /**
     * Moves the stream position to the next byte boundary.
     * If the bit offset is zero, this method does nothing.
     * Otherwise it skips the remaining bits in current byte.
     */
    public abstract void skipRemainingBits();

    /**
     * Returns the current byte position of the stream.
     * The returned value is relative to the position during the last call to {@link #relocateOrigin()}.
     * If the latter method has never been invoked, then the returned value is relative to the channel
     * position at {@code ChannelData} construction time.
     *
     * @return the position of the stream.
     */
    @Override
    public abstract long getStreamPosition();

    /**
     * Returns the current byte position of the stream, ignoring overriding by subclasses.
     * The returned value is relative to the position during the last call to {@link #relocateOrigin()}.
     * If the latter method has never been invoked, then the returned value is relative to the channel
     * position at {@code ChannelData} construction time.
     */
    final long position() {
        return Math.addExact(bufferOffset, buffer.position());
    }

    /**
     * Returns the earliest position in the stream to which {@linkplain #seek(long) seeking} may be performed.
     *
     * @return the earliest legal position for seeking.
     */
    public final long getFlushedPosition() {
        return bufferOffset;
    }

    /**
     * Discards the initial portion of the stream prior to the indicated position.
     * Attempting to {@linkplain #seek(long) seek} to an offset within the flushed
     * portion of the stream may result in an {@link IndexOutOfBoundsException}.
     *
     * <p>If the {@link #buffer} is read-only, then this method does nothing. Otherwise
     * this method moves the data starting at the given position to the beginning of the {@link #buffer},
     * thus making more room for new data before the data at the given position is discarded.</p>
     *
     * @param  position  the length of the stream prefix that may be flushed.
     * @throws IOException if an I/O error occurred.
     */
    public final void flushBefore(final long position) throws IOException {
        final long currentPosition = getStreamPosition();
        if (position > currentPosition) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                    "position", bufferOffset, currentPosition, position));
        }
        if (buffer.isReadOnly()) {
            return;
        }
        final long count = Math.subtractExact(position, bufferOffset);
        if (count > 0) {
            flushNBytes((int) Math.min(count, buffer.limit()));
        }
        /*
         * Discard trailing obsolete marks. Note that obsolete marks between valid marks
         * cannot be discarded - only the trailing obsolete marks can be removed.
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
     * Flushes the given number of bytes in the buffer.
     * This is invoked for making room for more bytes.
     * If the given count is larger than the buffer content, then this method flushes everything.
     * If the given count is zero or negative, then this method does nothing.
     *
     * @param  count  number of bytes to write, between 1 and buffer limit.
     * @throws IOException if an error occurred while writing the bytes to the channel.
     */
    abstract void flushNBytes(final int count) throws IOException;

    /**
     * Moves to the given position in the stream. The given position is relative to the position during
     * the last call to {@link #relocateOrigin()}. If the latter method has never been invoked, then the
     * argument is relative to the channel position at {@code ChannelData} construction time.
     *
     * @param  position  the position where to move.
     * @throws IOException if the stream cannot be moved to the given position.
     */
    public abstract void seek(final long position) throws IOException;

    /**
     * Pushes the current stream position onto a stack of marked positions.
     * Note that {@code ChannelData} maintains its own marks - the buffer's
     * mark is left unchanged.
     */
    @Override
    public final void mark() {
        mark = new Mark(position(), (byte) getBitOffset(), mark);
    }

    /**
     * Resets the current stream byte and bit positions from the stack of marked positions.
     * An {@code IOException} may be be thrown if the previous marked position lies in the
     * discarded portion of the stream.
     *
     * <h4>Departure from Image I/O specification</h4>
     * The {@link javax.imageio.stream.ImageInputStream#reset()} contract specifies that if there is no matching mark,
     * then this method shall do nothing. This method throws {@link IOException} instead; silently ignoring mismatch
     * is considered too dangerous. This is a departure from {@code ImageInputStream} but is consistent with
     * {@link java.io.InputStream#reset()} contract.
     *
     * @throws IOException if this stream cannot move to the last mark position.
     */
    @Override
    public final void reset() throws IOException {
        final Mark m = mark;
        if (m == null) {
            throw new IOException(Resources.format(Resources.Keys.StreamHasNoMark));
        }
        mark = m.next;
        seek(m.position);
        setBitOffset(m.bitOffset);
    }

    /**
     * Moves to the given position in the stream and discards all marks at or after that position.
     * If a mark exists at the given position, the bit offset is also restored.
     *
     * @param  position  position where to seek.
     * @throws IOException if this stream cannot move to the specified mark position.
     */
    @Override
    public final void reset(final long position) throws IOException {
        Mark lastValid = null;
        while (mark != null && mark.position >= position) {
            final boolean found = mark.position == position;
            if (found) lastValid = mark;
            mark = mark.next;               // Discard all marks after the specified position.
            if (found) break;
        }
        seek(position);
        if (lastValid != null) {
            setBitOffset(lastValid.bitOffset);
        }
    }

    /**
     * Empties the buffer and sets the channel position to the beginning of this stream (the origin).
     * This method is similar to {@code seek(0)} except that the buffer content and all marks are discarded,
     * and that this method returns {@code false} instead of throwing an exception if the channel is not seekable.
     *
     * @return {@code true} on success, or {@code false} if it is not possible to reset the position.
     * @throws IOException if an error occurred while setting the channel position.
     */
    public final boolean rewind() throws IOException {
        final Channel channel = channel();
        if (channel instanceof SeekableByteChannel) {
            ((SeekableByteChannel) channel).position(channelOffset);
            buffer.clear().limit(0);
            bufferOffset = 0;
            bitPosition  = 0;
            mark = null;
            return true;
        }
        return false;
    }

    /**
     * Copies all field values to the given object for {@code yield(…)} implementation in subclasses.
     * This is used for notifying that read or write operations will continue with the specified take over.
     * The two {@code ChannelData} instances should share the same {@link Channel}, or use two channels that
     * are at the same {@linkplain SeekableByteChannel#position() channel position}.
     *
     * @param  takeOver   the {@code ChannelData} which will continue operations after this one.
     */
    final void copyTo(final ChannelData takeOver) {
        assert takeOver.channel() == channel();
        takeOver.bufferOffset  = bufferOffset;
        takeOver.channelOffset = channelOffset;
        takeOver.bitPosition   = bitPosition;
        takeOver.mark          = mark;
        mark = null;
    }

    /**
     * Invalidates the buffer content and updates the value reported as the stream position.
     * This method is not a {@linkplain #seek(long) seek},
     * i.e. it does not change the {@linkplain #channel() channel} position,
     * This method only modifies the value returned by the {@link #getStreamPosition()} method.
     * This {@code refresh(long)} method can be invoked when external code has performed some work
     * directly on the {@linkplain #channel() channel} and wants to inform this {@code ChannelData}
     * about the new position resulting from this work.
     *
     * <b>Notes:</b>
     * <ul>
     *   <li>Invoking this method clears the {@linkplain #getBitOffset() bit offset} and {@linkplain #mark() marks}.</li>
     *   <li>Invoking this method sets the {@linkplain #buffer} {@linkplain ByteBuffer#limit() limit} to zero.</li>
     *   <li>This method does not need to be invoked when only the {@linkplain ByteBuffer#position() buffer position}
     *       has changed.</li>
     * </ul>
     *
     * @param  position  the new position of the stream.
     */
    public final void refresh(final long position) {
        buffer.limit(0);
        bufferOffset = position;
        /*
         * Clearing the bit offset is needed if we don't want to handle the case of `ChannelDataOutput`,
         * which use a different stream position calculation when the bit offset is non-zero.
         */
        bitPosition = 0;
        mark = null;
    }

    /**
     * Sets the current position as the new origin of this {@code ChannelData}.
     * After this method call, {@link #getStreamPosition()} will return zero when
     * {@code ChannelData} is about to read the byte located at the current position.
     *
     * <p>Note that invoking this method may change the value returned by {@link #length()},
     * because the length is relative to the origin.</p>
     */
    public final void relocateOrigin() {
        final long position = getStreamPosition();
        channelOffset = toSeekableByteChannelPosition(position);
        bufferOffset  = Math.subtractExact(bufferOffset, position);
    }

    /**
     * Converts a position in this {@code ChannelData} to position in the Java NIO channel.
     * This is often the same value, but not necessarily.
     *
     * @param  position  position in this {@code ChannelData}.
     * @return Corresponding position in the {@code SeekableByteChannel}.
     */
    final long toSeekableByteChannelPosition(final long position) {
        return Math.addExact(channelOffset, position);
    }

    /**
     * Translates the buffer by the given number of bytes.
     * Callers should subtract the same amount from the buffer position.
     *
     * @param  count  number of bytes to add to {@link #bufferOffset}.
     */
    final void moveBufferForward(final int count) {
        bufferOffset = Math.addExact(bufferOffset, count);
    }

    /**
     * Invoked when an operation between the channel and the buffer transferred no byte. Note that this is unrelated
     * to end-of-file, in which case {@link java.nio.channels.ReadableByteChannel#read(ByteBuffer)} returns -1.
     * A return value of 0 happen for example if the channel is a socket in non-blocking mode and the socket buffer
     * has not yet transferred new data.
     *
     * <p>The current implementation sleeps an arbitrary number of time before to allow a new try.
     * We do that in order to avoid high CPU consumption when data are expected to take more than
     * a few nanoseconds to arrive.</p>
     *
     * @throws IOException if the implementation chooses to stop the process.
     */
    protected void onEmptyTransfer() throws IOException {
        if (buffer.capacity() == 0) {
            // For avoiding never-ending loop.
            throw new IOException(Errors.format(Errors.Keys.Uninitialized_1, "buffer"));
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            /*
             * Someone doesn't want to let us sleep. Stop the reading or writing process. We don't try to go back to
             * work, because the waiting time was short and this method is invoked in loops. Consequently, if the user
             * interrupted us, it is probably because he waited for a long time and we still have not transferred any
             * new data.
             */
            throw new IOException(e);
        }
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder().append(getClass().getSimpleName()).append("[“").append(filename).append('”');
        // Even if the buffer should not be null, it is useful to keep the toString() method robust.
        if (buffer != null) b.append(" at ").append(getStreamPosition());
        return b.append(']').toString();
    }
}
