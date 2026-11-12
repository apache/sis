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
package org.apache.sis.storage.hdf.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * Read only channel returning a byte pattern repeated up to a given size.
 * Used for dataset regions which are not stored in the file and must be
 * filled with the fill value.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ConstantSeekableByteChannel implements SeekableByteChannel {

    private static final byte[] ZEROS = new byte[4096];

    private final long size;
    private final byte[] pattern;
    private long position = 0;
    private boolean open = true;

    /**
     * Creates a channel repeating the given pattern.
     *
     * @param size total number of bytes of the channel
     * @param pattern bytes to repeat, not empty
     */
    public ConstantSeekableByteChannel(long size, byte[] pattern) {
        this.size = size;

        reduce:
        if (pattern.length > 1) {
            //simplify to a single byte pattern if possible
            byte r = pattern[0];
            for (int i = 1; i < pattern.length; i++) {
                if (pattern[i] != r) break reduce;
            }
            pattern = new byte[]{r};
        }
        if (pattern.length == 1 && pattern[0] == 0) {
            pattern = ZEROS;
        }
        this.pattern = pattern;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!open) throw new ClosedChannelException();
        final long remaining = size - position;
        if (remaining <= 0) return -1;
        final long len = Math.min(dst.remaining(), remaining);
        if (pattern == ZEROS) {
            final int r = (int) Math.min(len, ZEROS.length);
            dst.put(ZEROS, 0, r);
            position += r;
            return r;
        } else {
            int r = (int) Math.min(len, remaining);
            final int total = r;
            int patternOffset = (int) (position % pattern.length);
            while (r > 0) {
                int nb = Math.min(pattern.length - patternOffset, r);
                dst.put(pattern, patternOffset, nb);
                patternOffset = 0;
                r -= nb;
            }
            position += total;
            return total;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }


    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (!open) throw new ClosedChannelException();
        if (newPosition < 0) throw new IllegalArgumentException("Position is negative");
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return size;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!open) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        if (!open) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

}
