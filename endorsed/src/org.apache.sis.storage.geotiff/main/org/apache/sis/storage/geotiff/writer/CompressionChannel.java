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
import java.nio.channels.WritableByteChannel;
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
abstract class CompressionChannel implements WritableByteChannel {
    /**
     * Desired size of the temporary buffer where to compress data.
     */
    static final int BUFFER_SIZE = StorageConnector.DEFAULT_BUFFER_SIZE / 2;

    /**
     * The destination where to write compressed data.
     */
    protected final ChannelDataOutput output;

    /**
     * Creates a new channel which will compress data to the given output.
     *
     * @param  output  the destination of compressed data.
     */
    protected CompressionChannel(final ChannelDataOutput output) {
        this.output = output;
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
    public void finish(final ChannelDataOutput owner) throws IOException {
        assert owner.channel == this;
        owner.flush();
        owner.clear();
    }

    /**
     * Releases resources used by this channel, but <strong>without</strong> closing the {@linkplain #output} channel.
     * The {@linkplain #output} channel is not closed by this operation because it will typically be needed again for
     * compressing other tiles.
     *
     * @throws IOException if an error occurred while flushing last data to the channel.
     */
    @Override
    public void close() throws IOException {
        // Do NOT close `output`.
    }
}
