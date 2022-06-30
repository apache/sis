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
import java.nio.channels.ReadableByteChannel;


/**
 * A channel of pixel values after all steps have been completed.
 * The steps may be:
 *
 * <ul>
 *   <li>Decompression alone, in which case this class is a subtype of {@link CompressionChannel}.</li>
 *   <li>Decompression followed by some mathematical operation applied on the data after decompression.
 *       In that case this class is a subtype of {@link PredictorChannel}.</li>
 * </ul>
 *
 * The {@link #close()} method shall be invoked when this channel is no longer used.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
abstract class PixelChannel implements ReadableByteChannel {
    /**
     * Creates a new channel.
     */
    protected PixelChannel() {
    }

    /**
     * Prepares this channel for reading a new tile or a new band of a planar image.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    public abstract void setInputRegion(long start, long byteCount) throws IOException;
}
