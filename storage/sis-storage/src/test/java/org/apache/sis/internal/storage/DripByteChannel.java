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
package org.apache.sis.internal.storage;

import java.util.Random;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ClosedChannelException;

import static java.lang.StrictMath.min;


/**
 * A {@link ReadableByteChannel} with a {@code read} methods that do not return all available bytes.
 * This class is used for simulating a socket connection where some data may not be immediately available
 * from the socket's input buffer.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DripByteChannel implements ReadableByteChannel {
    /**
     * The data to provide.
     */
    private final byte[] data;

    /**
     * The random number generator to use for determining how many bytes to return.
     */
    private final Random random;

    /**
     * Minimal (inclusive) and maximal (exclusive) amount of bytes to read.
     */
    private final int lower, upper;

    /**
     * Current position in the data array.
     */
    private int position;

    /**
     * Sets to {@code true} after {@link #close()} has been invoked.
     */
    private boolean isClosed;

    /**
     * Creates a new {@code DripByteChannel} wrapping the given data.
     *
     * @param data    The data to provide.
     * @param random  The random number generator to use for determining how many bytes to return.
     * @param lower   Minimal amount of bytes to read, inclusive.
     * @param upper   Maximal amount of bytes to read, exclusive.
     */
    public DripByteChannel(final byte[] data, final Random random, final int lower, final int upper) {
        this.data    = data;
        this.random  = random;
        this.lower   = lower;
        this.upper   = upper;
    }

    /**
     * Reads a random number of bytes from the data array.
     *
     * @param buffer The buffer where to copy the bytes.
     * @throws IOException If this channel is closed.
     */
    @Override
    public int read(final ByteBuffer buffer) throws IOException {
        if (isClosed) {
            throw new ClosedChannelException();
        }
        final int remaining = data.length - position;
        if (remaining == 0) {
            return -1;
        }
        final int n = min(random.nextInt(upper - lower) + lower, min(remaining, buffer.remaining()));
        buffer.put(data, position, n);
        position += n;
        return n;
    }

    /**
     * Returns {@code true} if the channel has not yet been closed.
     */
    @Override
    public boolean isOpen() {
        return !isClosed;
    }

    /**
     * Closes the channel.
     */
    @Override
    public void close() {
        isClosed = true;
    }
}
