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
import org.apache.sis.internal.storage.io.ChannelDataInput;


/**
 * Inflater for values encoded with the "PackBits" compression.
 * This compression is described in section 9 of TIFF 6 specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PackBits extends InflaterChannel {
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
     */
    PackBits(final ChannelDataInput input, final long start, final long byteCount) throws IOException {
        super(input, start, byteCount);
    }

    /**
     * Decompresses some bytes from the {@linkplain #input} into the given destination buffer.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        final ByteBuffer source = input.buffer;
        int total = 0;                              // Number of bytes that have been read.
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
                int n;
                do {
                    if (input.getStreamPosition() >= endPosition) {
                        return (total != 0) ? total : -1;             // If no byte read, declare end-of-stream.
                    }
                    n = input.readByte();
                } while (n == Byte.MIN_VALUE);
                if (n >= 0) {
                    literalCount = n + 1;
                } else {
                    duplicatedCount = 1 - n;
                    duplicated = input.readByte();
                }
            }
            /*
             * Execute the instruction (literal copy or byte duplication) in the limit of space available
             * in both buffers. If the instruction can not be executed fully, then one of `literalCount`
             * or `duplicatedCount` fields will be non-zero.
             */
            int n;
            if (literalCount != 0) {
                n = Math.min(literalCount, target.remaining());
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
                n = Math.min(duplicatedCount, target.remaining());
                for (int i=0; i<n; i++) target.put(duplicated);
                duplicatedCount -= n;
            }
            total += n;
        }
        return total;
    }
}
