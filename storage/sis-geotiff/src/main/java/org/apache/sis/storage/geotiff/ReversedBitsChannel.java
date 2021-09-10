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
package org.apache.sis.storage.geotiff;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.util.resources.Errors;


/**
 * A channel with a {@code read(…)} method fetching all bytes with theirs bits in reverse order.
 * For example byte {@code 11010100} will become {@code 00101011}.
 * The use of this class should be very rare.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ReversedBitsChannel implements ReadableByteChannel, SeekableByteChannel {
    /**
     * Lookup table for reversing the order of bits in a byte.
     */
    private static final byte[] REVERSE;
    static {
        REVERSE = new byte[256];
        for (int i=0; i<256; i++) {
            REVERSE[i] = (byte) (Integer.reverse(i) >>> 24);
        }
    }

    /**
     * Source channel where to read bytes before bit order reversal.
     */
    private final ChannelDataInput input;

    /**
     * Creates a new channel which will reverse the bit order of bytes read from the given channel.
     */
    private ReversedBitsChannel(final ChannelDataInput input) {
        this.input = input;
    }

    /**
     * Creates a new input channel at the same position and with the same content than the given channel,
     * but with bits order reversed in every byte. The new channel uses a temporary buffer of relatively
     * small size because invoking {@link #read(ByteBuffer)} is presumed not too costly for this class,
     * and because a new buffer is created for each strip or tile to read.
     */
    static ChannelDataInput wrap(final ChannelDataInput input) throws IOException {
        final ChannelDataInput output = new ChannelDataInput(
                input.filename, new ReversedBitsChannel(input),
                (ByteBuffer) ByteBuffer.allocate(2048).order(input.buffer.order()).limit(0), true);     // TODO! remove cast widh JDK9.
        output.setStreamPosition(input.getStreamPosition());
        return output;
    }

    /**
     * Implemented as a matter of principle, but not needed in the context of TIFF reader.
     */
    @Override
    public long size() throws IOException {
        if (input.channel instanceof SeekableByteChannel) {
            return ((SeekableByteChannel) input.channel).size();
        } else {
            throw unsupported("size");
        }
    }

    /**
     * Returns the current stream position.
     */
    @Override
    public long position() {
        return input.getStreamPosition();
    }

    /**
     * Moves to the specified stream position.
     */
    @Override
    public SeekableByteChannel position(final long p) throws IOException {
        input.setStreamPosition(p);
        return this;
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     * For each byte, bits order will be reversed.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        int n = target.remaining();
        if (n != 0) {
            final ByteBuffer source = input.buffer;
            if (!source.hasRemaining()) {
                input.ensureBufferContains(Byte.BYTES);
            }
            n = Math.min(n, source.remaining());
            for (int r = n; --r >= 0;) {
                target.put(REVERSE[Byte.toUnsignedInt(source.get())]);
            }
        }
        return n;
    }

    /**
     * Unsupported operation (for now).
     */
    @Override
    public int write(ByteBuffer source) throws IOException {
        throw unsupported("write");
    }

    /**
     * Unsupported operation (for now).
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw unsupported("truncate");
    }

    /**
     * Creates the exception to throw for an unsupported operation.
     */
    private static IOException unsupported(final String operation) {
        return new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, operation));
    }

    /**
     * Tells whether this channel is still open.
     */
    @Override
    public final boolean isOpen() {
        return input.channel.isOpen();
    }

    /**
     * Do nothing. The {@linkplain #input} channel is not closed by this operation
     * because it will typically be needed again for decompressing other tiles.
     */
    @Override
    public final void close() {
    }
}
