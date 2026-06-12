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
package org.apache.sis.io.stream.inflater;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Inflater writing all decompressed values in a temporary buffer.
 * This class does not need to care about subsampling.
 *
 * <p>The {@link #close()} method shall be invoked when this channel is no longer used.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class InflaterChannel extends ComputedByteChannel {
    /**
     * The source of data to decompress.
     */
    protected final ChannelDataInput input;

    /**
     * Stream position after the last byte to read.
     */
    private long endPosition;

    /**
     * Objects where to report warnings.
     */
    protected final StoreListeners listeners;

    /**
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input      the source of data to decompress.
     * @param  listeners  object where to report warnings.
     */
    protected InflaterChannel(final ChannelDataInput input, final StoreListeners listeners) {
        this.input     = input;
        this.listeners = listeners;
    }

    /**
     * Returns the channel from which to read compressed data.
     */
    @Override
    public final ChannelDataInput compressedInput() {
        return input;
    }

    /**
     * Prepares this channel for reading a new block of data.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        endPosition = Math.addExact(start, byteCount);
        input.seek(start);
        input.rangeOfInterest(start, endPosition);
    }

    /**
     * Returns {@code true} if the stream position reached the end of tile.
     *
     * @return whether the stream reached end of tile.
     */
    protected final boolean finished() {
        return input.getStreamPosition() >= endPosition;
    }

    /**
     * Copies the given byte <var>n</var> times in the given buffer.
     * This is a convenience method for an operation frequently found in different compression algorithms.
     *
     * @param  target  where to append the bytes.
     * @param  value   the byte to repeat.
     * @param  count   number of time to repeat the given value.
     * @throws BufferOverflowException if {@code count} is greater than the remaining space in the buffer.
     */
    protected static void repeat(final ByteBuffer target, final byte value, int count) {
        while (--count >= 0) target.put(value);
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return input.channel.isOpen();
    }

    /**
     * Releases resources used by this channel, but <strong>without</strong> closing the {@linkplain #input} channel.
     * The {@linkplain #input} channel is not closed by this operation because it will typically be needed again for
     * decompressing other tiles.
     */
    @Override
    public void close() {
        // Do NOT close `input`.
    }
}
