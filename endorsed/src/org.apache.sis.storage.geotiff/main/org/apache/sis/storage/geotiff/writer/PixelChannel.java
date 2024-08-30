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
import java.nio.channels.WritableByteChannel;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * A channel of pixel values after all steps have been completed.
 * The steps may be:
 *
 * <ul>
 *   <li>Compression alone, in which case this class is a subtype of {@link CompressionChannel}.</li>
 *   <li>Compression after some mathematical operation applied on the data before compression.
 *       In that case this class is a subtype of {@link PredictorChannel}.</li>
 * </ul>
 *
 * The {@link #close()} method shall be invoked when this channel is no longer used.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class PixelChannel implements WritableByteChannel {
    /**
     * Creates a new channel.
     */
    protected PixelChannel() {
    }

    /**
     * Creates a buffer to use with this compression channel.
     * The buffer size, and whether the buffer should be direct or not,
     * depends on the decompression implementation.
     * The byte order shall be set by the caller.
     */
    abstract ByteBuffer createBuffer();

    /**
     * Writes any pending data and reset the deflater for the next tile to compress.
     *
     * @param  owner  the data output which is writing in this channel.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    public abstract void finish(ChannelDataOutput owner) throws IOException;
}
