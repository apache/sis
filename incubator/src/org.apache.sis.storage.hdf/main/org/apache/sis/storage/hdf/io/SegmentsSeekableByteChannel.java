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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Read only channel over the concatenation of ranges taken from several files,
 * which is how a dataset stored in external data files is laid out.
 * <p>
 * A range may extend past the end of the file it points at, in which case the
 * missing bytes read as zeroes; the format specification requires it, so that a
 * dataset can be extended without growing its files first.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SegmentsSeekableByteChannel implements SeekableByteChannel {

    /**
     * One range of one file.
     *
     * @param path file holding the data
     * @param offset byte offset of the range in that file
     * @param size number of bytes of the range
     */
    public record Segment(Path path, long offset, long size) {}

    private final List<Segment> segments;
    private final SeekableByteChannel[] channels;
    private final long[] starts;
    private final long size;
    private long position;
    private boolean open = true;

    /**
     * Creates a channel over the given ranges. Files are opened lazily, on first read.
     *
     * @param segments ranges to concatenate, in order, not null
     * @throws IOException never thrown by the current implementation.
     */
    public SegmentsSeekableByteChannel(List<Segment> segments) throws IOException {
        this.segments = segments;
        this.channels = new SeekableByteChannel[segments.size()];
        this.starts = new long[segments.size() + 1];
        long total = 0;
        for (int i = 0; i < segments.size(); i++) {
            starts[i] = total;
            total += segments.get(i).size();
        }
        starts[segments.size()] = total;
        this.size = total;
    }

    private SeekableByteChannel channel(int index) throws IOException {
        if (channels[index] == null) {
            final Path path = segments.get(index).path();
            if (!Files.exists(path)) {
                throw new IOException("External data file " + path + " is missing.");
            }
            channels[index] = Files.newByteChannel(path, StandardOpenOption.READ);
        }
        return channels[index];
    }

    private int segmentOf(long position) {
        for (int i = 0; i < segments.size(); i++) {
            if (position >= starts[i] && position < starts[i + 1]) return i;
        }
        return -1;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!open) throw new ClosedChannelException();
        if (position >= size) return -1;
        final int index = segmentOf(position);
        if (index < 0) return -1;

        final Segment segment = segments.get(index);
        final long inSegment = position - starts[index];
        final long remainingInSegment = segment.size() - inSegment;
        final int wanted = (int) Math.min(dst.remaining(), remainingInSegment);
        if (wanted == 0) return 0;

        final SeekableByteChannel channel = channel(index);
        final long fileSize = channel.size();
        final long from = segment.offset() + inSegment;

        int read = 0;
        if (from < fileSize) {
            //bytes actually present in the file
            final int available = (int) Math.min(wanted, fileSize - from);
            final int limit = dst.limit();
            dst.limit(dst.position() + available);
            channel.position(from);
            while (dst.hasRemaining()) {
                final int n = channel.read(dst);
                //a zero read would spin forever, treat it as the end of the file
                if (n <= 0) break;
                read += n;
            }
            dst.limit(limit);
        }
        if (read < wanted) {
            /*
            Past the end of the file the specification mandates zeroes rather
            than a failure.
            */
            final int padding = wanted - read;
            final byte[] zeros = new byte[padding];
            dst.put(zeros);
            read += padding;
        }
        position += read;
        return read;
    }

    @Override
    public long position() throws IOException {
        if (!open) throw new ClosedChannelException();
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

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
        IOException first = null;
        for (SeekableByteChannel channel : channels) {
            if (channel == null) continue;
            try {
                channel.close();
            } catch (IOException ex) {
                if (first == null) first = ex; else first.addSuppressed(ex);
            }
        }
        if (first != null) throw first;
    }
}
