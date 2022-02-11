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
import org.apache.sis.internal.storage.io.ChannelDataInput;


/**
 * Inflater for values encoded with the "PackBits" compression.
 * This compression is described in section 9 of TIFF 6 specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class PackBits extends CompressionChannel {
    /**
     * Number of bytes to copy literally from the input.
     * Only one of {@code literalCount} and {@link #duplicatedCount} can be non-zero.
     */
    private int literalCount;

    /**
     * Number of times to copy the {@link #duplicated} byte.
     * Only one of {@link #literalCount} and {@code duplicatedCount} can be non-zero.
     */
    private int duplicatedCount;

    /**
     * Byte to copy many times. The number of copies is given by {@link #duplicatedCount}.
     */
    private byte duplicated;

    /**
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input  the source of data to decompress.
     */
    public PackBits(final ChannelDataInput input) {
        super(input);
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        literalCount    = 0;
        duplicatedCount = 0;
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
        final ByteBuffer source = input.buffer;
        while (target.hasRemaining()) {
            /*
             * If we stopped in the middle of a decompression during the previous call of this method,
             * skip this `if` block for continuing where we were. Otherwise this block decodes the next
             * byte, but without executing immediately:
             *
             *   - If -128, no operation.
             *   - Otherwise if positive, copy the next n+1 bytes literally.
             *   - Otherwise if negative, copy the next byte -n+1 times.
             */
            if ((literalCount | duplicatedCount) == 0) {
                int code;
                do {
                    if (finished()) {
                        final int n = target.position() - start;
                        return (n > 0) ? n : -1;              // If no byte read, declare end-of-stream.
                    }
                    code = input.readByte();
                } while (code == Byte.MIN_VALUE);
                if (code >= 0) {
                    literalCount = code + 1;
                } else {
                    duplicatedCount = 1 - code;
                    duplicated = input.readByte();
                }
            }
            /*
             * Execute the instruction (literal copy or byte duplication) in the limit of space available
             * in both buffers. If the instruction can not be executed fully, then one of `literalCount`
             * or `duplicatedCount` fields will be non-zero.
             */
            if (literalCount != 0) {
                int n = Math.min(literalCount, target.remaining());
                int r = source.remaining();
                if (r == 0) {
                    input.ensureBufferContains(Math.min(n, source.capacity()));
                    r = source.remaining();
                }
                if (r <= n) {
                    target.put(source);
                    n = r;
                } else {
                    final int limit = source.limit();
                    source.limit(source.position() + n);
                    target.put(source);
                    source.limit(limit);
                }
                literalCount -= n;
            } else {
                final int n = Math.min(duplicatedCount, target.remaining());
                repeat(target, duplicated, n);
                duplicatedCount -= n;
            }
        }
        return target.position() - start;
    }
}
