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
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.util.Debug;


/**
 * Properties common to {@link ChannelDataInput} and {@link ChannelDataOutput}.
 * This base class contains a reference to a {@link ByteBuffer} supplied by the user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.3)
 * @module
 */
abstract class ChannelData {
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
     * Creates a new instance for the given channel and using the given buffer.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read or where to wrote.
     * @param  buffer   The buffer where to store the data.
     * @throws IOException If an error occurred while reading the channel.
     */
    public ChannelData(final String filename, final Channel channel, final ByteBuffer buffer) throws IOException {
        this.filename      = filename;
        this.buffer        = buffer;
        this.channelOffset = (channel instanceof SeekableByteChannel) ? ((SeekableByteChannel) channel).position() : 0;
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
    protected void onEmptyChannelBuffer() throws IOException {
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
     * Sets the current byte position of the stream. This method does <strong>not</strong> seeks the stream;
     * this method only modifies the value to be returned by {@link #getStreamPosition()}. This method can
     * be invoked when some external code has performed some work with the channel and wants to inform this
     * {@code ChannelData} about the new position resulting from this work.
     *
     * <p>This method does not need to be invoked when only the {@linkplain ByteBuffer#position() buffer position}
     * has changed.</p>
     *
     * @param position The new position of the stream.
     */
    public final void setStreamPosition(final long position) {
        bufferOffset = position - buffer.position();
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return The position of the stream.
     */
    public final long getStreamPosition() {
        return bufferOffset + buffer.position();
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     *
     * @return A string representation of this object.
     */
    @Debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[“" + filename + "” at " + getStreamPosition() + ']';
    }
}
