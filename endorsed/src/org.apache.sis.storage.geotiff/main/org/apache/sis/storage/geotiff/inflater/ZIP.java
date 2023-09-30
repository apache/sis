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
package org.apache.sis.storage.geotiff.inflater;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.io.stream.ChannelDataInput;


/**
 * Inflater for values encoded with the "Deflate" compression.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ZIP extends CompressionChannel {
    /**
     * Access to the ZLIB compression library.
     * Must be released by call to {@link Inflater#end()} after decompression is completed.
     */
    private final Inflater inflater;

    /**
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input      the source of data to decompress.
     * @param  listeners  object where to report warnings.
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    public ZIP(final ChannelDataInput input, final StoreListeners listeners) {
        super(input, listeners);
        inflater = new Inflater();
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        inflater.reset();
    }

    /**
     * Decompresses some bytes from the {@linkplain #input input} into the given destination buffer.
     *
     * @param  target  the buffer into which bytes are to be transferred.
     * @return the number of bytes read, or -1 if end-of-stream.
     * @throws IOException if some other I/O error occurs.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        final int start = target.position();
        int required = 0;
        try {
            int n;
            while ((n = inflater.inflate(target)) == 0) {
                if (inflater.needsInput()) {
                    if (++required >= input.buffer.capacity()) {
                        throw new BufferOverflowException();
                    }
                    input.ensureBufferContains(required);
                    inflater.setInput(input.buffer);
                } else if (inflater.finished()) {
                    return -1;
                } else {
                    throw new IOException();
                }
            }
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
        return target.position() - start;
    }

    /**
     * Releases resources used by the inflater.
     */
    @Override
    public void close() {
        inflater.end();
        super.close();
    }
}
