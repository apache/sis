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
package org.apache.sis.internal.geotiff;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


/**
 * Implementation of a {@link Predictor} to be executed after decompression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class InflaterPredictor implements ReadableByteChannel {
    /**
     * The channel from which to read data.
     */
    private final InflaterChannel input;

    /**
     * Creates a predictor.
     */
    protected InflaterPredictor(final InflaterChannel input) {
        this.input = input;
    }

    /**
     * Applies the predictor on data in the given buffer,
     * from the given start position until current buffer position.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   position of first sample value to process.
     */
    protected abstract void uncompress(ByteBuffer buffer, int start);

    /**
     * Decompresses some bytes from the {@linkplain #input} into the given destination buffer.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        final int start = target.position();
        final int n = input.read(target);
        uncompress(target, start);
        return n;
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return input.isOpen();
    }

    /**
     * Do nothing. The {@linkplain #input} channel is not closed by this operation
     * because it will typically be needed again for decompressing other tiles.
     */
    @Override
    public final void close() {
    }
}
