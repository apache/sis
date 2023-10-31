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
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.storage.geotiff.base.Predictor;


/**
 * Implementation of a {@link Predictor} to be executed before compression.
 * A predictor is a mathematical operator that is applied to the image data
 * before an encoding scheme is applied, in order to improve compression.
 *
 * <p>Note that this channel may modify in-place the content of the buffer
 * given in calls to {@link #write(ByteBuffer)}. That buffer should contain
 * only temporary data, typically copied from a raster data buffer.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class PredictorChannel extends PixelChannel {
    /**
     * The channel where to write data.
     */
    protected final PixelChannel output;

    /**
     * Creates a predictor.
     *
     * @param  output  the channel that compress data.
     */
    protected PredictorChannel(final PixelChannel output) {
        this.output = output;
    }

    /**
     * Writes any pending data and reset the deflater for the next tile to compress.
     *
     * @param  owner  the data output which is writing in this channel.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    @Override
    public void finish(final ChannelDataOutput owner) throws IOException {
        output.finish(owner);
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return output.isOpen();
    }

    /**
     * Closes {@link #output}. Note that it will <strong>not</strong> closes the channel wrapped by {@link #output}
     * because that channel will typically be needed again for compressing other tiles.
     */
    @Override
    public final void close() throws IOException {
        output.close();
    }
}
