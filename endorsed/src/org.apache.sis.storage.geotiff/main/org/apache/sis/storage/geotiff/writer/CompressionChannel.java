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
package org.apache.sis.storage.geotiff.writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * Deflater using a temporary buffer where to compress data before writing to the channel.
 * This class does not need to care about subsampling.
 *
 * <p>The {@link #close()} method shall be invoked when this channel is no longer used.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class CompressionChannel extends PixelChannel {
    /**
     * Size of the buffer where to temporarily copy data to compress. The buffer should be
     * large enough for allowing compression algorithms and predictors to work comfortably
     * (potentially modifying the buffer content in-place). But a too large value may also
     * be counter-productive, maybe because it causes more frequent CPU cache invalidation.
     */
    private static final int BUFFER_SIZE = StorageConnector.DEFAULT_BUFFER_SIZE / 2;

    /**
     * The destination where to write compressed data.
     */
    protected final ChannelDataOutput output;

    /**
     * Number of bytes to be compressed.
     */
    protected final long length;

    /**
     * Creates a new channel which will compress data to the given output.
     *
     * @param  output  the destination of compressed data.
     * @param  length  number of bytes to be compressed.
     */
    protected CompressionChannel(final ChannelDataOutput output, final long length) {
        this.output = output;
        this.length = length;
    }

    /**
     * Returns a proposed buffer capacity.
     * This is a helper method for {@link #createBuffer()} implementations.
     */
    final int bufferCapacity() {
        /*
         * Size of compressed data should be less than `length`, but we do not try to do a better
         * estimation because the length will usually be limited by the maximal value below anyway.
         * Those minimal and maximal capacity values are arbitrary.
         */
        return Math.max((int) Math.min(length, BUFFER_SIZE), 1024);
    }

    /**
     * Creates a buffer to use with this compression channel.
     * The byte order shall be set by the caller.
     *
     * <p>The default implementation creates a buffer on heap,
     * which is suitable for decompression implemented in Java.
     * Decompression implemented by native libraries may prefer direct buffer.</p>
     */
    @Override
    ByteBuffer createBuffer() {
        return ByteBuffer.allocate(bufferCapacity());
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return output.channel.isOpen();
    }

    /**
     * Writes any pending data and reset the deflater for the next tile to compress.
     *
     * @param  owner  the data output which is writing in this channel.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    @Override
    public void finish(final ChannelDataOutput owner) throws IOException {
        owner.flush();
        owner.clear();
    }

    /**
     * Releases resources used by this channel, but <strong>without</strong> closing the {@linkplain #output} channel.
     * The {@linkplain #output} channel is not closed by this operation because it will typically be needed again for
     * compressing other tiles.
     */
    @Override
    public void close() {
        // Do NOT close `output`.
    }
}
