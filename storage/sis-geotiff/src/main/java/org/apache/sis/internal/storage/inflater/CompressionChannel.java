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
package org.apache.sis.internal.storage.inflater;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.io.ChannelDataInput;


/**
 * Inflater writing all decompressed values in a temporary buffer.
 * This class does not need to care about subsampling.
 *
 * <p>The {@link #close()} method shall be invoked when this channel is no longer used.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
abstract class CompressionChannel extends PixelChannel {
    /**
     * Size of the buffer where to temporarily copy decompressed data.
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * The source of data to decompress.
     */
    protected final ChannelDataInput input;

    /**
     * Stream position after the last byte to read.
     */
    private long endPosition;

    /**
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input  the source of data to decompress.
     */
    protected CompressionChannel(final ChannelDataInput input) {
        this.input = input;
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of byte to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        endPosition = Math.addExact(start, byteCount);
        input.seek(start);
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
     * Creates the data input stream to use for getting uncompressed data.
     * The {@linkplain #input} stream must be on the start position before to invoke this method.
     *
     * @param  channel  the channel to wrap. This is {@code this} unless a {@link Predictor} is applied.
     * @throws IOException if an error occurred while filling the buffer with initial data.
     * @return the data input for uncompressed data.
     */
    final ChannelDataInput createDataInput(final PixelChannel channel) throws IOException {
        // TODO: remove cast with JDK9.
        final ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocate(BUFFER_SIZE).order(input.buffer.order()).limit(0);
        return new ChannelDataInput(input.filename, channel, buffer, true);
    }

    /**
     * Copies the given byte <var>n</var> times in the given buffer.
     */
    static void repeat(final ByteBuffer target, final byte b, int n) {
        while (--n >= 0) target.put(b);
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

    /**
     * Returns the resources for error messages. Current implementation does not know the locale.
     * But if this information become known in a future version, this is the code to update.
     */
    final Resources resources() {
        return Resources.forLocale(null);
    }
}
