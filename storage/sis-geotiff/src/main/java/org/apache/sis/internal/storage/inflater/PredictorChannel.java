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
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.geotiff.Predictor;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Implementation of a {@link Predictor} to be executed after decompression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
abstract class PredictorChannel extends PixelChannel {
    /**
     * The channel from which to read data.
     */
    private final CompressionChannel input;

    /**
     * If {@link #read(ByteBuffer)} could not process some trailing bytes,
     * a copy of those bytes for processing in the next method invocation.
     */
    private byte[] deferred;

    /**
     * Number of values in the {@link #deferred} array.
     */
    private int deferredCount;

    /**
     * Creates a predictor.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input  the channel that decompress data.
     */
    protected PredictorChannel(final CompressionChannel input) {
        this.input = input;
        deferred = ArraysExt.EMPTY_BYTE;
    }

    /**
     * Prepares this predictor for reading a new tile or a new band of a planar image.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        input.setInputRegion(start, byteCount);
        deferredCount = 0;
    }

    /**
     * Applies the predictor on data in the given buffer,
     * from the given start position until current buffer position.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   position of first sample value to process.
     * @return position after the last sample value processed. Should be {@link ByteBuffer#position()},
     *         unless the predictor needs more data for processing the last bytes.
     */
    protected abstract int apply(ByteBuffer buffer, int start);

    /**
     * Decompresses some bytes from the {@linkplain #input} into the given destination buffer.
     *
     * @param  target  the buffer into which bytes are to be transferred.
     * @return the number of bytes read, or -1 if end-of-stream.
     * @throws IOException if some other I/O error occurs.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        final int start = target.position();
        if (deferredCount != 0) {
            /*
             * If we had some bytes from the previous invocation that `apply(…)` could not use,
             * append those bytes in the target buffer before to read new bytes from the channel.
             * We may not be able to append all bytes.
             */
            final int n = Math.min(deferredCount, target.remaining());
            target.put(deferred, 0, n);
            System.arraycopy(deferred, n, deferred, 0, deferredCount -= n);
        }
        input.read(target);
        final int end = apply(target, start);
        final int remaining = target.position() - end;
        if (remaining != 0) {
            /*
             * If there is some bytes that `apply(…)` could not use, save those bytes for next
             * invocation of this `read(…)` method. Those bytes may need to be appended after
             * bytes that previous code block has not been able to put in the target buffer.
             */
            final int length = deferredCount + remaining;
            if (length > deferred.length) {
                deferred = new byte[length];
            }
            JDK9.get(target, end, deferred, deferredCount, remaining);
            target.position(end);
            deferredCount = length;
        }
        return end - start;
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return input.isOpen();
    }

    /**
     * Closes {@link #input}. Note that it will <strong>not</strong> closes the channel wrapped by {@link #input}
     * because that channel will typically be needed again for decompressing other tiles.
     */
    @Override
    public final void close() {
        input.close();
    }
}
