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
package org.apache.sis.internal.storage.io;

import java.util.Objects;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ClosedChannelException;


/**
 * A channel which read no values. This class behaves as if the channel already reached the end of file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
final class NullChannel implements ReadableByteChannel {
    /**
     * Whether this channel has been closed.
     */
    private volatile boolean closed;

    /**
     * Creates an initially open channel.
     */
    NullChannel() {
    }

    /**
     * Pretends to read a sequence of bytes and indicates that the channel reached the end of file.
     * Read-only buffers are accepted (this is required for {@link ChannelImageInputStream}).
     *
     * @param  dst  ignored except for non-null check.
     * @return always -1.
     * @throws ClosedChannelException if this channel has been closed.
     */
    @Override
    public int read(ByteBuffer dst) throws ClosedChannelException {
        Objects.requireNonNull(dst);
        if (closed) {
            throw new ClosedChannelException();
        }
        return -1;
    }

    /**
     * Returns whether this channel is still open.
     */
    @Override
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Closes this channel. If this channel is already closed, then this method does nothing.
     */
    @Override
    public void close() {
        closed = true;
    }
}
