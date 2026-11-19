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
 * Read only channel over the concatenation of two channels.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SequenceSeekableByteChannel implements SeekableByteChannel {

    private final SeekableByteChannel first;
    private final SeekableByteChannel second;
    private final long separation;
    private long position = 0;
    private boolean open = true;

    /**
     * Creates a channel reading {@code first} then {@code second}.
     * Both channels are closed when this channel is closed.
     *
     * @param first channel read first, its size is taken at construction time
     * @param second channel read after the end of {@code first}
     * @throws IOException if the size of the first channel could not be obtained.
     */
    public SequenceSeekableByteChannel(SeekableByteChannel first, SeekableByteChannel second) throws IOException {
        this.first = first;
        this.second = second;
        this.separation = first.size();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        SeekableByteChannel channel = position < separation ? first : second;
        int nb = channel.read(dst);
        if (nb < 0) return nb;
        position += nb;
        return nb;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (position < separation) first.position(newPosition);
        else second.position(newPosition - separation);
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return first.size() + second.size();
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

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        first.close();
        second.close();
        open = false;
    }

}
