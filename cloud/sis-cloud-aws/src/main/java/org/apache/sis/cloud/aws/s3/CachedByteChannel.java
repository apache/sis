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
package org.apache.sis.cloud.aws.s3;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;


/**
 * A seekable byte channel which copies S3 data to a temporary file for caching purposes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class CachedByteChannel implements SeekableByteChannel {
    /**
     * Size of the transfer buffer, in number of bytes.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * The input stream from which to read data.
     */
    private final ResponseInputStream<GetObjectResponse> input;

    /**
     * The file where data are copied.
     */
    private final FileChannel file;

    /**
     * A temporary buffer for transferring data when we
     * can not write directly in the destination buffer.
     */
    private ByteBuffer transfer;

    /**
     * Current position of this channel.
     */
    private long position;

    /**
     * Number of bytes in the temporary file.
     *
     * In current implementation this value shall be identical to {@code file.position()}.
     * However in a future implementation it will become different if we allow some parts
     * of the file to be without data (sparse file), with data fetched using HTTP ranges.
     */
    private long validLength;

    /**
     * Creates a new channel.
     */
    CachedByteChannel(final ResponseInputStream<GetObjectResponse> stream) throws IOException {
        input = stream;
        file = FileChannel.open(Files.createTempFile("S3-", null),
                StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DELETE_ON_CLOSE);
    }

    /**
     * Attempts to read up to <i>r</i> bytes from the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer.
     * Bytes are written in the given buffer starting at the current position.
     * Upon return, the buffer position is advanced by the number of bytes read.
     *
     * @return number of bytes read, or -1 if end of file.
     */
    @Override
    public synchronized int read(final ByteBuffer dst) throws IOException {
        /*
         * If the channel position is before the end of cached data (i.e. a backward seek has been done before),
         * use those data without downloading more data from the input stream. Maybe available data are enough.
         */
        if (position < validLength) {
            final int limit = dst.limit();
            final int start = dst.position();
            final int end   = (int) Math.min(limit, start + (validLength - position));
            final int count;
            dst.limit(end);
            try {
                count = file.read(dst, position);
                position += count;
            } finally {
                dst.limit(limit);
            }
            return count;
        }
        /*
         * At this point we need to download data from the input stream.
         * Get a buffer that we can use with `InputStream.read(byte[])`.
         * It must be a buffer backed by a Java array.
         */
        final ByteBuffer buffer;
        if (dst.hasArray()) {
            buffer = dst;
        } else {
            if (transfer == null) {
                transfer = ByteBuffer.allocate(BUFFER_SIZE);
            }
            buffer = transfer;
            buffer.clear();
            buffer.limit(dst.remaining());
        }
        /*
         * Transfer bytes from the input stream to the buffer.
         * The bytes are also copied to the temporary file.
         */
        final int limit = buffer.limit();
        final int start = buffer.position();
        final int count = input.read(buffer.array(), buffer.arrayOffset() + start, limit - start);
        if (count > 0) {
            buffer.limit(start + count);
            try {
                cache(buffer);
            } finally {
                buffer.limit(limit);
            }
            /*
             * If we used a temporary buffer, transfer to the destination buffer.
             */
            if (buffer != dst) {
                buffer.flip();
                dst.put(buffer);
            }
            position += count;
        }
        return count;
    }

    /**
     * Writes fully the given buffer in the cache {@linkplain #file}.
     * The data to write starts at current buffer position and stops at the buffer limit.
     */
    private void cache(final ByteBuffer buffer) throws IOException {
        do {
            if (file.write(buffer) == 0) {
                // Should never happen, but check anyway as a safety against never-ending loop.
                throw new IOException();
            }
            validLength = file.position();
        } while (buffer.hasRemaining());
    }

    /**
     * Attempts to write up to <i>r</i> bytes to the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer.
     * Bytes are read from the given buffer starting at the current position.
     * Upon return, the buffer position is advanced by the number of bytes written.
     */
    @Override
    public int write(final ByteBuffer src) throws IOException {
        throw new IOException("Not supported yet.");
    }

    /**
     * Returns this channel's position.
     */
    @Override
    public synchronized long position() {
        return position;
    }

    /**
     * Sets this channel's position.
     *
     * @param  newPosition  number of bytes from the beginning to the desired position.
     * @return {@code this} for method call chaining.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized SeekableByteChannel position(final long newPosition) throws IOException {
        ArgumentChecks.ensurePositive("newPosition", newPosition);
        long remaining = newPosition - validLength;
        if (remaining > 0) {
            if (transfer == null) {
                transfer = ByteBuffer.allocate(BUFFER_SIZE);
            }
            final ByteBuffer buffer = transfer;
            do {
                buffer.clear();
                if (remaining < BUFFER_SIZE) {
                    buffer.limit((int) remaining);
                }
                final int count = input.read(buffer.array(), 0, buffer.limit());
                if (count <= 0) {
                    final Long size = input.response().contentLength();
                    throw new EOFException(Errors.format(Errors.Keys.ValueOutOfRange_4, "newPosition", 0, size, newPosition));
                }
                buffer.limit(count);
                cache(buffer);
                remaining -= count;
            } while (remaining > 0);
        }
        position = newPosition;
        return this;
    }

    /**
     * Returns the size of the S3 file.
     *
     * @return number of bytes in the file.
     * @throws IOException if the information is not available.
     */
    @Override
    public long size() throws IOException {
        final Long size = input.response().contentLength();
        if (size != null) return size;
        throw new IOException();
    }

    /**
     * Truncates the file to the given size.
     *
     * @param  size  the new size in bytes.
     * @throws IOException if the operation is not supported.
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new IOException("Not supported yet.");
    }

    /**
     * Tells whether this channel is open.
     *
     * @return {@code true} if this channel is open.
     */
    @Override
    public boolean isOpen() {       // No synchronization, rely on `FileChannel` thread safety instead.
        return file.isOpen();
    }

    /**
     * Closes this channel and releases resources.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public synchronized void close() throws IOException {
        transfer = null;
        try {
            file.close();
        } catch (Throwable e) {
            try {
                input.close();
            } catch (Throwable s) {
                e.addSuppressed(s);
            }
            throw e;
        }
        input.close();
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "position", position, "validLength", validLength);
    }
}
