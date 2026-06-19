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
package org.apache.sis.io.stream.inflater;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Base class of channels providing values that are computed on-the-fly.
 * The computation typically uses another channel as its source.
 * Examples:
 *
 * <ul>
 *   <li>Decompression alone, in which case this class is a subtype of {@link InflaterChannel}.</li>
 *   <li>Decompression followed by some mathematical operation applied on the data after decompression.
 *       In that case this class is a subtype of {@link PredictorChannel}.</li>
 * </ul>
 *
 * The {@link #close()} method shall be invoked when this channel is no longer used.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ComputedByteChannel implements ReadableByteChannel {
    /**
     * Desired size of the buffer where to temporarily copy decompressed data.
     * The actual buffer size may become larger (but not smaller)
     * because we try to use a multiple of scanline stride.
     */
    private static final int BUFFER_SIZE = StorageConnector.DEFAULT_BUFFER_SIZE / 2;

    /**
     * Creates a new channel.
     */
    ComputedByteChannel() {
    }

    /**
     * Returns the channel from which to read compressed data.
     *
     * @return the input channel, typically opened on a file.
     */
    public abstract ChannelDataInput compressedInput();

    /**
     * Prepares this channel for reading a new block of data.
     * A block may be, for example, a tile or a band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    public abstract void setInputRegion(long start, long byteCount) throws IOException;

    /**
     * Returns whether the inflater or predictor algorithm prefers native byte buffer.
     * The default implementation returns {@code false}.
     * Subclasses that depends on native library may return {@code true}.
     *
     * @return whether the inflater or predictor prefers native byte buffer.
     */
    protected boolean preferNativeBuffer() {
        return false;
    }

    /**
     * Creates the data input stream to use for getting uncompressed data.
     * The source {@link ChannelDataInput} must be on the start position before to invoke this method.
     *
     * <p>This method tries to create a buffer of the size of scanline stride, or a multiple of that size,
     * for performance reasons. A well adjusted buffer size reduces calls to {@link ByteBuffer#compact()},
     * which in turn reduces the number of copy operations between different regions of the buffer.</p>
     *
     * @param  scanlineStride  the scanline stride of the image to read. Used for choosing a buffer size.
     * @throws IOException if an error occurred while filling the buffer with initial data.
     * @return the data input for uncompressed data.
     */
    public final ChannelDataInput createDataInput(final int scanlineStride) throws IOException {
        final int capacity;
        if (scanlineStride > BUFFER_SIZE) {
            final int[] divisors = MathFunctions.divisors(scanlineStride);
            int i = Arrays.binarySearch(divisors, BUFFER_SIZE);
            if (i < 0) i = ~i;              // Really tild, not minus.
            capacity = divisors[i];         // Smallest divisor ≥ BUFFER_SIZE
        } else if (scanlineStride > Long.SIZE) {
            capacity = JDK18.ceilDiv(BUFFER_SIZE, scanlineStride) * scanlineStride;      // ≥ BUFFER_SIZE
        } else {
            capacity = BUFFER_SIZE;
        }
        final ChannelDataInput input = compressedInput();
        ByteBuffer buffer = preferNativeBuffer() ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
        buffer = buffer.order(input.buffer.order()).limit(0);
        return new ChannelDataInput(input.filename, this, buffer, true);
    }
}
