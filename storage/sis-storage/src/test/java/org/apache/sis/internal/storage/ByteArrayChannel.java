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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import java.nio.channels.ByteChannel;


/**
 * A readable and writable channel backed by an array.
 * Used for testing {@link ChannelDataOutput} and {@link ChannelImageOutputStream} classes.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see ChannelDataOutputTest
 * @see ChannelImageOutputStream
 */
final strictfp class ByteArrayChannel implements ByteChannel {
    /**
     * Bytes array where to write the data.
     * The length of this array is the capacity.
     */
    private final byte[] data;

    /**
     * Number of valid bytes in the {@link #data} array.
     */
    private int limit;

    /**
     * Current position in the {@link #data} array.
     */
    private int position;

    /**
     * Sets to {@code true} after {@link #close()} has been invoked.
     */
    private boolean isClosed;

    /**
     * Creates a channel which will store all written data in the given array.
     *
     * @param data Bytes array where to write the data. The length of this array is the capacity.
     * @param isContentValid {@code true} if the channel should be initialized with all the {@code data} elements,
     *        or {@code false} if the channel should be considered initially empty.
     */
    ByteArrayChannel(final byte[] data, final boolean isDataValid) {
        this.data = data;
        if (isDataValid) {
            limit = data.length;
        }
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     */
    @Override
    public int read(final ByteBuffer dst) throws IOException {
        ensureOpen();
        if (position >= limit) {
            return -1;
        }
        final int length = StrictMath.min(dst.remaining(), limit - position);
        dst.put(data, position, length);
        position += length;
        return length;
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     */
    @Override
    public int write(final ByteBuffer src) throws IOException {
        ensureOpen();
        final int length = src.remaining();
        src.get(data, position, length);
        position += length;
        limit = StrictMath.max(limit, position);
        return length;
    }

    /**
     * Returns this channel position.
     */
    public long position() throws IOException {
        ensureOpen();
        return position;
    }

    /**
     * Sets this channel position.
     */
    public ByteArrayChannel position(final long newPosition) throws IOException {
        ensureOpen();
        ArgumentChecks.ensureBetween("position", 0, data.length, newPosition);
        position = (int) newPosition;
        return this;
    }

    /**
     * Returns the current size.
     */
    public long size() throws IOException {
        ensureOpen();
        return limit;
    }

    /**
     * Truncates the data to the given size.
     */
    public ByteArrayChannel truncate(final long size) throws IOException {
        ensureOpen();
        ArgumentChecks.ensureBetween("position", 0, limit, size);
        limit = (int) size;
        return this;
    }

    /**
     * Verifies that the channel is open.
     */
    private void ensureOpen() throws IOException {
        if (isClosed) {
            throw new ClosedChannelException();
        }
    }

    /**
     * Tells whether or not this channel is open.
     */
    @Override
    public boolean isOpen() {
        return !isClosed;
    }

    /**
     * Closes this channel.
     */
    @Override
    public void close() throws IOException {
        isClosed = true;
    }
}
