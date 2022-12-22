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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.NonWritableChannelException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.RangeSet;


/**
 * A seekable byte channel which copies data from an input stream to a temporary file.
 * This class can be used for wrapping HTTP or S3 connections for use with {@link ChannelDataInput}.
 * Characteristics:
 *
 * <ul>
 *   <li>Bytes read from the input stream are cached in a temporary file for making backward seeks possible.</li>
 *   <li>The number of bytes of interest {@linkplain #endOfInterest(long) can be specified}.
 *       It makes possible to specify the range of bytes to download with HTTP connections.</li>
 *   <li>This implementation is thread-safe.</li>
 *   <li>Current implementation is read-only.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 * @module
 */
public abstract class FileCacheByteChannel implements SeekableByteChannel {
    /**
     * Size of the transfer buffer, in number of bytes.
     */
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
     * Threshold for implementing a change of position by closing current connection and opening a new one.
     * If the number of bytes to skip is smaller than this threshold, then we will rather continue reading
     * with the current input stream.
     *
     * <p>For an average download speed of 25 Mb/s, downloading 64 kB requires about 1/50 of second.</p>
     */
    static final int SKIP_THRESHOLD = 64 * 1024;

    /**
     * The unit of ranges used in HTTP connections.
     */
    protected static final String RANGES_UNIT = "bytes";

    /**
     * Information about an input stream and its range of bytes.
     * This is the return value of {@link #openConnection(long, long)}.
     */
    protected static final class Connection extends org.apache.sis.internal.jdk17.Record {
        /** The input stream for reading the bytes. */
        final InputStream input;

        /** Position of the first byte read by the input stream (inclusive). */
        final long start;

        /** Position of the last byte read by the input stream (inclusive). */
        final long end;

        /** Total length of the stream, or -1 is unknown. */
        final long length;

        /** Whether connection can be created for ranges of bytes. */
        final boolean acceptRanges;

        /**
         * Creates information about a connection.
         *
         * @param input          the input stream for reading the bytes.
         * @param start          position of the first byte read by the input stream (inclusive).
         * @param end            position of the last byte read by the input stream (inclusive).
         * @param contentLength  total length of the stream, or -1 if unknown.
         * @param acceptRanges   whether connection can be created for ranges of bytes.
         *
         * @see #openConnection(long, long)
         */
        public Connection(final InputStream input, final long start, final long end, final long contentLength, final boolean acceptRanges) {
            this.input  = input;
            this.start  = start;
            this.end    = end;
            this.length = contentLength;
            this.acceptRanges = acceptRanges;
        }

        /**
         * Creates information about a connection by parsing HTTP header.
         * Example: "Content-Range: bytes 25000-75000/100000".
         *
         * @param  input          the input stream for reading the bytes.
         * @param  contentRange   value of "Content-Range" in HTTP header.
         * @param  acceptRanges   value of "Accept-Ranges" in HTTP header.
         * @param  contentLength  total length of the stream, or -1 if unknown.
         * @throws IllegalArgumentException if the start, end of length cannot be parsed.
         */
        public Connection(final InputStream input, String contentRange, long contentLength, final Iterable<String> acceptRanges) {
            this.input = input;
            contentRange = contentRange.trim();
            int s = contentRange.indexOf(' ');
            if (s >= 0 && (s != RANGES_UNIT.length() || !contentRange.regionMatches(true, 0, RANGES_UNIT, 0, s))) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, contentRange));
            }
            int rs = contentRange.indexOf('-', ++s);                    // Index of range separator.
            int ls = contentRange.indexOf('/', Math.max(s, rs+1));      // Index of length separator.
            if (contentLength < 0 && ls >= 0) {
                final String t = contentRange.substring(ls+1).trim();
                if (!t.equals("*")) contentLength = Long.parseLong(t);
            }
            length = contentLength;
            if (ls < 0) ls = contentRange.length();
            if (rs < 0) rs = ls;
            start = Long.parseLong(contentRange.substring(s, rs).trim());
            end = (rs < ls) ? Long.parseLong(contentRange.substring(rs+1, ls).trim()) : length;
            this.acceptRanges = acceptRanges(acceptRanges);
        }

        /**
         * Returns {@code true} if the given "Accept-Ranges" values contains at least one "bytes" string.
         *
         * @param  values  HTTP header value for "Accept-Ranges".
         * @return whether the values contains at least one "bytes" string.
         */
        public static boolean acceptRanges(final Iterable<String> values) {
            for (final String t : values) {
                if (ArraysExt.containsIgnoreCase((String[]) CharSequences.split(t, ','), RANGES_UNIT)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            return Strings.toString(getClass(), "start", start, "end", end);
        }
    }

    /**
     * The source of bytes to read, or {@code null} if the connection has not yet been established.
     * The stream can be closed and replaced by another stream if various connections are opened
     * for downloading various ranges of bytes. When a new stream is created, the position of the
     * {@linkplain #file} channel shall be synchronized with the input stream position (taking in
     * account the start of the range).
     *
     * @see #openConnection()
     * @see #openConnection(long, long)
     * @see #abort(InputStream)
     */
    private Connection connection;

    /**
     * Input/output channel on the temporary or cached file where data are copied.
     * The {@linkplain FileChannel#position() position of this file channel} shall
     * be the current position of the {@linkplain Connection#input input stream}.
     *
     * <h4>Space consumption</h4>
     * This channel should be opened on a sparse file.
     * To check if a file is sparse, the outputs of following shell commands can be compared:
     *
     * {@preformat shell
     *   ls -l the-temporary-file
     *   du --block-size=1 the-temporary-file
     * }
     *
     * @see <a href="https://en.wikipedia.org/wiki/Sparse_file">Sparse file on Wikipedia</a>
     */
    private final FileChannel file;

    /**
     * A temporary buffer for transferring data when we cannot write directly in the destination buffer.
     * It shall be a buffer backed by a Java array, not a direct buffer. Created when first needed.
     */
    private ByteBuffer transfer;

    /**
     * Current position of this channel. The first byte of this channel is always at position zero.
     *
     * @see #position()
     */
    private long position;

    /**
     * Position after the last requested byte, or ≤ {@linkplain #position} if unknown.
     * It can be used for specifying the range of bytes to download from an HTTP connection.
     *
     * @see #endOfInterest(long)
     */
    private long endOfInterest;

    /**
     * Ranges of bytes in the {@linkplain #file} where data are valid.
     */
    private final RangeSet<Long> rangesOfAvailableBytes;

    /**
     * Number of bytes in the full stream, or 0 if not yet computed.
     */
    private long length;

    /**
     * Creates a new channel which will cache bytes in a temporary file.
     * The source of bytes will be provided by {@link #openConnection(long, long)}.
     *
     * @param  prefix  prefix of the temporary file to create.
     * @throws IOException if the temporary file can not be created.
     */
    protected FileCacheByteChannel(final String prefix) throws IOException {
        rangesOfAvailableBytes = RangeSet.create(Long.class, true, false);
        file = FileChannel.open(Files.createTempFile(prefix, null),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.SPARSE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.DELETE_ON_CLOSE);
    }

    /**
     * Returns the filename to use in error messages.
     *
     * @return a filename for error messages.
     */
    protected abstract String filename();

    /**
     * Creates an input stream which provides the bytes to read starting at the specified position.
     * If the caller needs only a sub-range of bytes, then the end of the desired range is specified.
     * That end is only a hint and can be ignored.
     *
     * @param  start  position of the first byte to read (inclusive).
     * @param  end    position of the last byte to read with the returned stream (inclusive),
     *                or {@link Long#MAX_VALUE} for end of stream.
     * @return information about the input stream providing the bytes to read starting at the given start position.
     * @throws IOException if the connection can not be established.
     */
    protected abstract Connection openConnection(long start, long end) throws IOException;

    /**
     * Invoked when this channel is no longer interested in reading bytes from the specified stream.
     * This method is invoked for example when this channel needs to skip an arbitrarily large number
     * of bytes because the {@linkplain #position(long) position changed}. The {@code input} argument
     * is the value in the record returned by a previous call to {@link #openConnection(long, long)}.
     * The boolean return value tells what this method has done:
     *
     * <ul class="verbose">
     *   <li>If this method returns {@code true}, then the given stream has been closed by this method and this
     *       channel is ready to create a new stream on the next call to {@link #openConnection(long, long)}.</li>
     *   <li>If this method returns {@code false}, then the given stream is still alive and should continue to be used.
     *       The {@link #openConnection(long, long)} method will <em>not</em> be invoked.
     *       Instead, bytes will be skipped by reading them from the current input stream and caching them.</li>
     * </ul>
     *
     * @param  input  the input stream to eventually close.
     * @return whether the given input stream has been closed by this method. If {@code false},
     *         then this channel should continue to use that input stream instead of opening a new connection.
     * @throws IOException if an error occurred while closing the stream or preparing for next read operations.
     */
    protected boolean abort(InputStream input) throws IOException {
        return false;
    }

    /**
     * Returns the number of bytes in the input stream.
     *
     * @return number of bytes in the input stream.
     * @throws IOException if the information is not available.
     */
    @Override
    public synchronized long size() throws IOException {
        return length;
    }

    /**
     * Returns this channel's position.
     * The first byte read by this channel is always at position zero.
     *
     * @return the current channel position.
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
        position = newPosition;
        if (endOfInterest - newPosition < SKIP_THRESHOLD) {
            endOfInterest = 0;      // Read until end of stream.
        }
        return this;
    }

    /**
     * Specifies the position after the last byte which is expected to be read.
     * The number of bytes is only a hint and may be ignored, depending on subclasses.
     * Reading more bytes than specified is okay, only potentially less efficient.
     * Values ≤ {@linkplain #position() position} means to read until the end of stream.
     *
     * @param  end  position after the last desired byte, or a value ≤ position for reading until the end of stream.
     */
    final synchronized void endOfInterest(final long end) {
        endOfInterest = end;
    }

    /**
     * Opens a connection on the range of bytes determined by the current channel position.
     * The {@link #endOfInterest} position is considered unspecified if not greater than
     * {@link #position} (it may be 0).
     *
     * @return the opened connection (never {@code null}).
     * @throws IOException if the connection can not be established.
     */
    private Connection openConnection() throws IOException {
        long end = endOfInterest;
        if (end > position) end--;      // Make inclusive.
        else end = (length > 0) ? length-1 : Long.MAX_VALUE;
        var c = openConnection(position, end);
        file.position(c.start);
        if (c.length >= 0) {
            length = c.length;
        }
        connection = c;                 // Set only on success.
        return c;
    }

    /**
     * Returns a buffer for transferring bytes from the input stream to the cache.
     * This buffer must be backed by a Java array (not a direct buffer).
     */
    private ByteBuffer transfer() {
        if (transfer == null) {
            transfer = ByteBuffer.allocate(BUFFER_SIZE);
        }
        return transfer;
    }

    /**
     * Tries to move the input stream by skipping the specified amount of bytes.
     * This method is invoked when the source of input streams (the server) does not support ranges,
     * or when the number of bytes to skip is too small for being worth to create a new connection.
     * This method may skip less bytes than requested. The skipped bytes are saved in the cache.
     *
     * @param  count  number of bytes to skip.
     * @return remaining number of bytes to skip after this method execution.
     * @throws IOException if an I/O error occurred.
     */
    private long skipInInput(long count) throws IOException {
        if (count < 0) {
            throw new IOException(Resources.format(Resources.Keys.StreamIsReadOnce_1, filename()));
        } else if (count != 0) {
            final InputStream input = connection.input;
            final ByteBuffer buffer = transfer();
            do {
                buffer.clear();
                if (count < BUFFER_SIZE) {
                    buffer.limit((int) count);
                }
                int n = input.read(buffer.array(), 0, buffer.limit());
                if (n <= 0) {
                    if (n != 0 || (n = input.read()) < 0) {     // Block until we get one byte.
                        break;                                  // End of stream, but maybe it was a sub-range.
                    }
                    buffer.put((byte) n);
                    n = 1;
                }
                cache(buffer.limit(n));
                count -= n;
            } while (count > 0);
        }
        return count;
    }

    /**
     * Attempts to read up to <i>r</i> bytes from the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer.
     * Bytes are written in the given buffer starting at the current position.
     * Upon return, the buffer position is advanced by the number of bytes read.
     *
     * @param  dst  the buffer where to store the bytes that are read.
     * @return number of bytes read, or -1 if end of file.
     * @throws IOException if an error occurred when reading the bytes.
     */
    @Override
    public synchronized int read(final ByteBuffer dst) throws IOException {
        /*
         * If the channel position is inside a range of cached data (i.e. a backward seek has been done before),
         * use those data without downloading more data from the input stream. Maybe available data are enough.
         */
        int count = readFromCache(dst);
        if (count >= 0) {
            return count;
        }
        /*
         * If we reach this point, the byte at `position` is not in the cache.
         * If a connection exists, we need to either discard it or skip bytes.
         */
        Connection c = connection;
        long offset = position - file.position();
        if (offset != 0 && c != null) {
            if ((offset < 0 || (c.acceptRanges && (offset >= SKIP_THRESHOLD || position > c.end)))) {
                offset -= drainAndAbort();
                c = connection;
            }
        }
        /*
         * At this point we need to download data from the input stream.
         * If previous connection can not be used, open a new one for the range of bytes to read.
         * Then skip all bytes between the current position and the requested position.
         * Those bytes will be saved in the cache.
         */
        if (c == null) {
            c = openConnection();
            offset = position - c.start;
        }
        offset = skipInInput(offset);
        if (offset != 0) {
            count = readFromCache(dst);
            if (count >= 0) {
                return count;
            }
            throw new EOFException(Errors.format(Errors.Keys.ValueOutOfRange_4, "position", 0, length, position));
        }
        /*
         * Get a buffer that we can use with `InputStream.read(byte[])`.
         * It must be a buffer backed by a Java array.
         */
        final ByteBuffer buffer;
        if (dst.hasArray()) {
            buffer = dst;
        } else {
            buffer = transfer();
            buffer.clear().limit(dst.remaining());
        }
        /*
         * Transfer bytes from the input stream to the buffer.
         * The bytes are also copied to the temporary file.
         */
        final int limit = buffer.limit();
        final int start = buffer.position();
        count = c.input.read(buffer.array(), Math.addExact(buffer.arrayOffset(), start), buffer.remaining());
        if (count > 0) {
            try {
                cache(buffer.limit(start + count));
            } finally {
                buffer.limit(limit);
            }
            if (buffer != dst) {
                dst.put(buffer.flip());     // Transfer temporary to destination buffer.
            }
            position += count;
        }
        return count;
    }

    /**
     * Attempts to read up to bytes from the cache.
     *
     * @param  dst  the buffer where to store the bytes that are read.
     * @return number of bytes read, or -1 if the cache does not contain the requested range of bytes.
     * @throws IOException if an error occurred when reading the bytes.
     */
    private int readFromCache(final ByteBuffer dst) throws IOException {
        final int indexOfRange = rangesOfAvailableBytes.indexOfRange(position);
        if (indexOfRange < 0) {
            return -1;
        }
        final long endOfCache = rangesOfAvailableBytes.getMaxLong(indexOfRange);
        final int limit = dst.limit();
        final int start = dst.position();
        final int end   = (int) Math.min(limit, start + (endOfCache - position));
        final int count;
        try {
            count = file.read(dst.limit(end), position);
            position += count;
        } finally {
            dst.limit(limit);
        }
        return count;
    }

    /**
     * Writes fully the given buffer in the cache {@linkplain #file}.
     * The data to write starts at current buffer position and stops at the buffer limit.
     * This method changes the {@link Connection#input} and {@link #file} positions by the same amount.
     *
     * @param  buffer  buffer containing data to cache.
     */
    private void cache(final ByteBuffer buffer) throws IOException {
        do {
            final long start = file.position();
            final int  count = file.write(buffer);
            if (count <= 0) {
                // Should never happen, but check anyway as a safety against never-ending loop.
                throw new IOException();
            }
            long end = start + count;
            if (end < start) end = Long.MAX_VALUE;      // Safety against overflow.
            rangesOfAvailableBytes.add(start, end);
        } while (buffer.hasRemaining());
    }

    /**
     * Reads and caches the bytes that are already available in the input stream, then aborts download.
     * This method may set {@link #connection} to null.
     *
     * @return number of bytes that have been read.
     * @throws IOException if an I/O error occurred.
     */
    private long drainAndAbort() throws IOException {
        long count = 0;
        final InputStream input = connection.input;
        for (int c; (c = input.available()) > 0;) {
            final ByteBuffer buffer = transfer();
            buffer.clear();
            if (c < BUFFER_SIZE) buffer.limit(c);
            final int n = input.read(buffer.array(), 0, buffer.limit());
            if (n < 0) break;
            cache(buffer.limit(n));
            count += n;
        }
        if (abort(input)) {
            connection = null;
        }
        return count;
    }

    /**
     * Attempts to write up to <i>r</i> bytes to the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer.
     * Bytes are read from the given buffer starting at the current position.
     * Upon return, the buffer position is advanced by the number of bytes written.
     *
     * <p>The default implementation throws {@link IOException}.</p>
     *
     * @param  src  the buffer containing the bytes to write.
     * @return number of bytes actually written.
     * @throws IOException if an error occurred while writing the bytes.
     */
    @Override
    public int write(final ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    /**
     * Truncates the file to the given size.
     *
     * @param  size  the new size in bytes.
     * @throws IOException if the operation is not supported.
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }

    /**
     * Tells whether this channel is open.
     *
     * @return {@code true} if this channel is open.
     */
    @Override
    public boolean isOpen() {     // No synchronization, rely on `FileChannel` thread safety instead.
        return file.isOpen();
    }

    /**
     * Closes this channel and releases resources.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public synchronized void close() throws IOException {
        final Connection c = connection;
        connection = null;
        transfer = null;
        try (file) {
            if (c != null && !abort(c.input)) {
                c.input.close();
            }
        }
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public synchronized String toString() {
        return Strings.toString(getClass(), "filename", filename(), "position", position, "rangeCount", rangesOfAvailableBytes.size());
    }
}
