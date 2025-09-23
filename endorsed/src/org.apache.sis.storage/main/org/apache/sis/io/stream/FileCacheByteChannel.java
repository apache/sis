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
package org.apache.sis.io.stream;

import java.util.Collection;
import java.util.function.UnaryOperator;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.NonWritableChannelException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.RangeSet;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.storage.base.StoreUtilities.LOGGER;


/**
 * A seekable byte channel which copies data from an input stream to a temporary file.
 * This class can be used for wrapping HTTP or S3 connections for use with {@link ChannelDataInput}.
 * Characteristics:
 *
 * <ul>
 *   <li>Bytes read from the input stream are cached in a temporary file for making backward seeks possible.</li>
 *   <li>The range of bytes of interest {@linkplain #rangeOfInterest(long, long) can be specified}.
 *       It makes possible to specify the range of bytes to download with HTTP connections.</li>
 *   <li>This implementation is thread-safe.</li>
 *   <li>Current implementation is read-only.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class FileCacheByteChannel extends ByteRangeChannel implements SeekableByteChannel {
    /**
     * Size of the transfer buffer, in number of bytes.
     * This value does not need to be as large as {@link StorageConnector#DEFAULT_BUFFER_SIZE}
     * because the buffer will be used only for transferring data, with no computation done on
     * the content.
     */
    private static final int BUFFER_SIZE = StorageConnector.DEFAULT_BUFFER_SIZE / 4;

    /**
     * Threshold for implementing a change of position by closing current connection and opening a new one.
     * If the number of bytes to skip is smaller than this threshold, then we will rather continue reading
     * with the current input stream.
     *
     * <p>For an average download speed of 25 Mb/s, downloading 64 kB requires about 1/50 of second.</p>
     */
    static final int SKIP_THRESHOLD = 64 * 1024;

    /**
     * Number of nanoseconds to wait before to close an inactive connection.
     */
    private static final long TIMEOUT = 2 * 1000_000_000L;

    /**
     * Information about an input stream and its range of bytes.
     * This is the return value of {@link #openConnection(long, long)}.
     */
    protected static final class Connection extends org.apache.sis.pending.jdk.Record {
        /** The unit of ranges used in HTTP connections. */
        private static final String RANGES_UNIT = "bytes";

        /**
         * The input stream without filtering, as specified at construction time.
         * This is the same instance as {@link #input} when no filtering is applied.
         */
        public final InputStream rawInput;

        /**
         * The input stream for reading the bytes. It may be a wrapper around the input stream
         * specified at construction time if {@linkplain #setFilter a filter has been set}.
         */
        public final InputStream input;

        /** Position of the first byte read by the input stream (inclusive). */
        final long start;

        /** Position of the last byte read by the input stream (inclusive). */
        final long end;

        /** Number of bytes in the full stream, or -1 is unknown. */
        final long length;

        /** Whether connection can be created for ranges of bytes. */
        final boolean acceptRanges;

        /**
         * Creates information about a connection.
         *
         * @param owner         the channel which is opening this connection, or {@code null} if none.
         * @param input         the input stream for reading the bytes.
         * @param start         position of the first byte read by the input stream (inclusive).
         * @param end           position of the last byte read by the input stream (inclusive).
         * @param length        length of the full stream (not the content length), or -1 if unknown.
         * @param acceptRanges  whether connection can be created for ranges of bytes.
         *
         * @see #openConnection(long, long)
         */
        public Connection(final FileCacheByteChannel owner, final InputStream input,
                final long start, final long end, final long length, final boolean acceptRanges)
        {
            rawInput    = input;
            this.start  = start;
            this.end    = end;
            this.length = length;
            this.acceptRanges = acceptRanges;
            this.input  = filter(owner, input);     // Must be last.
        }

        /**
         * Creates information about a connection by parsing HTTP header without content range.
         * The "Content-Length" header value is useful to this class only if the connection was
         * opened for the full file.
         *
         * @param  owner          the channel which is opening this connection, or {@code null} if none.
         * @param  input          the input stream for reading the bytes.
         * @param  contentLength  length of the response content, or -1 if unknown.
         * @param  rangeUnits     value of "Accept-Ranges" in HTTP header, which lists the accepted units.
         * @throws IllegalArgumentException if the start, end or length cannot be parsed.
         */
        public Connection(final FileCacheByteChannel owner, final InputStream input,
                          final long contentLength, final Iterable<String> rangeUnits)
        {
            rawInput     = input;
            this.start   = 0;
            this.end     = (contentLength > 0) ? contentLength - 1 : Long.MAX_VALUE;
            this.length  = contentLength;
            acceptRanges = acceptRanges(rangeUnits);
            this.input   = filter(owner, input);
        }

        /**
         * Creates information about a connection by parsing HTTP header with content range.
         * Note that the "Content-Length" header value is not useful when a range is specified
         * because the content length is not the full length of the file.
         *
         * <p>Example of content range value: {@code "Content-Range: bytes 25000-75000/100000"}.</p>
         *
         * @param  owner         the channel which is opening this connection, or {@code null} if none.
         * @param  input         the input stream for reading the bytes.
         * @param  contentRange  value of "Content-Range" in HTTP header.
         * @param  rangeUnits    value of "Accept-Ranges" in HTTP header, which lists the accepted units.
         * @throws IllegalArgumentException if the start, end or length cannot be parsed.
         */
        public Connection(final FileCacheByteChannel owner, final InputStream input,
                          String contentRange, final Collection<String> rangeUnits)
        {
            rawInput = input;
            long contentLength = -1;
            contentRange = contentRange.trim();
            int s = contentRange.indexOf(' ');
            if (s >= 0 && (s != RANGES_UNIT.length() || !contentRange.regionMatches(true, 0, RANGES_UNIT, 0, s))) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, contentRange));
            }
            int rs = contentRange.indexOf('-', ++s);                    // Index of range separator.
            int ls = contentRange.indexOf('/', Math.max(s, rs+1));      // Index of length separator.
            if (ls >= 0) {
                final String t = contentRange.substring(ls+1).trim();
                if (!t.equals("*")) contentLength = Long.parseLong(t);
            }
            length = contentLength;
            if (ls < 0) ls = contentRange.length();
            if (rs < 0) rs = ls;
            start = Long.parseLong(contentRange.substring(s, rs).trim());
            end = (rs < ls) ? Long.parseLong(contentRange.substring(rs+1, ls).trim()) : length;
            /*
             * By default Apache server repeats the information about accepted range units,
             * while Nginx does not. If that information is not provided, assume that the
             * supported units did not changed.
             */
            acceptRanges = rangeUnits.isEmpty() || acceptRanges(rangeUnits);
            this.input = filter(owner, input);
        }

        /**
         * If an optional filtering has been specified, applied it on the given input stream.
         * This method should be invoked last in constructor, because it needs other fields.
         *
         * @param  owner  the channel which is opening a connection, or {@code null} if none.
         * @param  input  the input stream created for a new connection.
         * @return the filtered input stream, or {@code input} if there is no filtering.
         *
         * @see #setFilter(UnaryOperator)
         */
        private InputStream filter(final FileCacheByteChannel owner, InputStream input) {
            final Filter filter;
            if (owner != null && (filter = owner.filter) != null) {
                input = filter.apply(input, start, end);
            }
            return input;
        }

        /**
         * Returns {@code true} if the given "Accept-Ranges" values contains at least one "bytes" string.
         *
         * @param  values  HTTP header value for "Accept-Ranges".
         * @return whether the values contains at least one "bytes" string.
         */
        private static boolean acceptRanges(final Iterable<String> values) {
            for (final String t : values) {
                if (ArraysExt.containsIgnoreCase((String[]) CharSequences.split(t, ','), RANGES_UNIT)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Formats the "Range" value to send in an HTTP header for the specified range of bytes.
         * This is a helper method for {@link #openConnection(long, long)} implementations.
         *
         * @param  start  position of the first byte to read (inclusive).
         * @param  end    position of the last byte to read with the returned stream (inclusive),
         *                or {@link Long#MAX_VALUE} for end of stream.
         * @return the "Range" value to put in an HTTP header.
         */
        public static String formatRange(final long start, final long end) {
            final boolean hasEnd = (end > start) && (end != Long.MAX_VALUE);
            if (start == 0 && !hasEnd) {
                return null;
            }
            final StringBuilder range = new StringBuilder(RANGES_UNIT).append('=').append(start).append('-');
            if (hasEnd) {
                range.append(end);      // Inclusive.
            }
            return range.toString();
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            return Strings.toString(getClass(), null, formatRange(start, end));
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
    private volatile Connection connection;

    /**
     * An optional filter to apply on the input stream opened for a connections.
     * A filter may be installed for example for being notified of the ranges of
     * bytes that are read, or for transforming the data.
     *
     * @see #setFilter(Filter)
     * @see java.io.FilterInputStream
     * @see java.io.FilterOutputStream
     */
    public interface Filter {
        /**
         * Invoked when an input stream is created for a new connection.
         *
         * @param  input  the input stream for the new connection.
         * @param  start  position of the first byte to be returned by the input stream.
         * @param  end    position (inclusive) of the last byte to be returned.
         * @return the input stream to use for reading data.
         *
         * @see java.io.FilterInputStream
         */
        InputStream apply(InputStream input, long start, long end);
    }

    /**
     * Optional filters to apply on the streams opened for a connection.
     *
     * @see #setFilter(Filter)
     */
    private Filter filter;

    /**
     * Input/output channel on the temporary or cached file where data are copied.
     * The {@linkplain FileChannel#position() position of this file channel} shall
     * be the current position of the {@linkplain Connection#input input stream}.
     *
     * <h4>Space consumption</h4>
     * This channel should be opened on a sparse file.
     * To check if a file is sparse, the outputs of following shell commands can be compared:
     *
     * {@snippet lang="shell" :
     *   ls -l the-temporary-file
     *   du --block-size=1 the-temporary-file
     *   }
     *
     * @see <a href="https://en.wikipedia.org/wiki/Sparse_file">Sparse file on Wikipedia</a>
     */
    private final FileChannel file;

    /**
     * A temporary buffer for transferring data when we cannot write directly in the destination buffer.
     * It shall be a buffer backed by a Java array, not a direct buffer. Created when first needed.
     *
     * @see #transfer()
     */
    private ByteBuffer transfer;

    /**
     * Current position of this channel. The first byte of this channel is always at position zero.
     *
     * @see #position()
     */
    private long position;

    /**
     * Ranges of requested bytes, for choosing the ranges to request in new connections.
     * Ranges are added by calls to {@link #rangeOfInterest(long, long)} and removed
     * when the connection is created.
     *
     * @see #rangeOfInterest(long, long)
     */
    private final RangeSet<Long> rangesOfInterest;

    /**
     * Ranges of bytes in the {@linkplain #file} where data are valid.
     */
    private final RangeSet<Long> rangesOfAvailableBytes;

    /**
     * Number of bytes in the full stream, or -1 if not yet computed.
     * It will be set to {@link Connection#length} when a connection is established,
     * and updated for every new connection in case the value change.
     */
    private long length = -1;

    /**
     * Creates a new channel which will cache bytes in a temporary file.
     * The source of bytes will be provided by {@link #openConnection(long, long)}.
     *
     * @param  prefix  prefix of the temporary file to create.
     * @throws IOException if the temporary file cannot be created.
     */
    protected FileCacheByteChannel(final String prefix) throws IOException {
        rangesOfInterest       = RangeSet.create(Long.class, true, false);
        rangesOfAvailableBytes = RangeSet.create(Long.class, true, false);
        file = FileChannel.open(Files.createTempFile(prefix, null),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.SPARSE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.DELETE_ON_CLOSE);
    }

    /**
     * Applies an optional filter on the streams opened for each new connection.
     * A filter may be installed for example for being notified of the ranges of
     * bytes that are read, or for transforming the data.
     *
     * @param  filter  a function which receives in argument the stream created
     *         for a new connection, and returns the stream to use.
     *         A {@code null} function remove filtering.
     */
    public final void setFilter(final Filter filter) {
        this.filter = filter;
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
     *         May be {@code null} if this method detects that the {@code start} position is after the end of stream.
     * @throws IOException if the connection cannot be established.
     */
    protected abstract Connection openConnection(long start, long end) throws IOException;

    /**
     * Invoked when this channel is no longer interested in reading bytes from the specified stream.
     * This method is invoked for example when this channel needs to skip an arbitrarily large number
     * of bytes because the {@linkplain #position(long) position changed}. The {@code connection}
     * argument is the value returned by a previous call to {@link #openConnection(long, long)}.
     * The Boolean return value tells what this method has done:
     *
     * <ul class="verbose">
     *   <li>If this method returns {@code true}, then the input stream has been closed by this method and this
     *       channel is ready to create a new stream on the next call to {@link #openConnection(long, long)}.</li>
     *   <li>If this method returns {@code false}, then the input stream is still alive and should continue to be used.
     *       The {@link #openConnection(long, long)} method will <em>not</em> be invoked.
     *       Instead, bytes will be skipped by reading them from the current input stream and caching them.</li>
     * </ul>
     *
     * @param  connection  container of the input stream to eventually close.
     * @return whether the input stream has been closed by this method. If {@code false},
     *         then this channel should continue to use that input stream instead of opening a new connection.
     * @throws IOException if an error occurred while closing the stream or preparing for next read operations.
     */
    protected boolean abort(Connection connection) throws IOException {
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
        if (length < 0) {
            if (connection == null) {
                openConnection();
            }
            if (length < 0) {
                throw new IOException(Errors.format(Errors.Keys.Uninitialized_1, "size"));
            }
        }
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
        if (length > 0) {
            ArgumentChecks.ensureBetween("newPosition", 0, length-1, newPosition);
        } else {
            ArgumentChecks.ensurePositive("newPosition", newPosition);
        }
        position = newPosition;
        return this;
    }

    /**
     * Specifies a range of bytes which is expected to be read.
     * The range of bytes is only a hint and may be ignored, depending on subclasses.
     * Reading more bytes than specified is okay, only potentially less efficient.
     *
     * @param  lower  position (inclusive) of the first byte to be requested.
     * @param  upper  position (exclusive) of the last byte to be requested.
     */
    @Override
    public final synchronized void rangeOfInterest(final long lower, final long upper) {
        if (upper > lower) {
            rangesOfInterest.add(lower, upper);
        }
    }

    /**
     * Opens a connection on the range of bytes determined by the current channel position.
     * The range of bytes of interest is specified in the {@link #rangesOfInterest} set.
     * If no range is specified, this method requests all bytes until the end of stream.
     * If some ranges are specified, this method finds the smallest "end of range" after
     * the current position. If the gab between ranges is less than {@link #SKIP_THRESHOLD},
     * the ranges will be merged in a single request.
     *
     * @return the opened connection, or {@code null} if end of stream.
     * @throws IOException if the connection cannot be established.
     */
    private Connection openConnection() throws IOException {
        int i = Math.max(rangesOfInterest.indexOfMin(position), 0);
        final int size = rangesOfInterest.size();
        long end;
        do {                    // Should be executed exactly 1 or 2 times.
            if (i >= size) {
                if (length <= 0) {
                    end = Long.MAX_VALUE;
                } else {
                    end = length - 1;
                    if (position > end) {
                        return null;
                    }
                }
                break;
            }
            end = rangesOfInterest.getMaxLong(i) - 1;       // Inclusive
            i++;
        } while (end < position);
        /*
         * At this point we found the smallest "end of range" position.
         * If the gap with next range is small enough, merge the ranges
         * in order to make a single connection request.
         */
        while (i < size) {
            if (rangesOfInterest.getMinLong(i) - end >= SKIP_THRESHOLD) {
                break;
            }
            end = rangesOfInterest.getMaxLong(i) - 1;       // Inclusive
            i++;
        }
        /*
         * Send the HTTP or S3 request for the range of bytes.
         * Prepare the cache file to receive those bytes.
         * Save the stream length if it is known.
         */
        final Connection c = openConnection(position, end);
        if (c == null) {
            return null;
        }
        file.position(c.start);
        if (c.length >= 0) {
            length = c.length;
        }
        connection = c;                 // Set only on success.
        /*
         * Remove the requested range from the list of ranges of interest.
         * The range to remove is determined on the assumption that caller
         * makes a best effort for reading bytes in sequential order, and
         * that if the connection provides less bytes, the missing bytes
         * will probably be requested later.
         */
        end = Math.min(c.end, end);
        if (end != Long.MAX_VALUE) end++;       // Make exclusive.
        rangesOfInterest.remove(Math.min(position, c.start), end);
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
     * Tries to move the input stream by skipping the specified number of bytes.
     * This method is invoked when the source of input streams (the server) does not support ranges,
     * or when the number of bytes to skip is too small for being worth to create a new connection.
     * This method may skip less bytes than requested. The skipped bytes are saved in the cache.
     *
     * <p>The {@link #position} field (the channel position) is not modified by this method.
     * This method is invoked when input position needs to become equal to the channel position.</p>
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
                    buffer.put(0, (byte) n);                    // Do not increment buffer position.
                    n = 1;
                }
                assert buffer.position() == 0;
                cache(buffer.limit(n));
                count -= n;
            } while (count > 0);
        }
        return count;
    }

    /**
     * Attempts to read up to <var>r</var> bytes from the channel,
     * where <var>r</var> is the number of bytes remaining in the buffer.
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
        if (c != null) {
            if ((offset < 0 || (c.acceptRanges && (offset >= SKIP_THRESHOLD || position > c.end)))) {
                offset -= drainAndAbort();
                c = connection;                 // May become null as a result of `drainAndAbort()`.
            }
        }
        /*
         * At this point we need to download data from the input stream.
         * If previous connection cannot be used, open a new one for the range of bytes to read.
         * Then skip all bytes between the current position and the requested position.
         * Those bytes will be saved in the cache.
         */
        if (c == null) {
            c = openConnection();
            if (c == null) {
                return -1;
            }
            offset = position - c.start;
        }
        offset = skipInInput(offset);
        if (offset != 0) {
            count = readFromCache(dst);         // In case `skipInInput(…)` has read more bytes than desired.
            usedConnection();
            if (count >= 0) {
                return count;
            }
            throw new EOFException(Errors.format(Errors.Keys.ValueOutOfRange_4, "position", 0, length, position));
        }
        assert file.position() == position;
        /*
         * Get a buffer that we can use with `InputStream.read(byte[])`.
         * It must be a buffer backed by a Java array.
         */
        final ByteBuffer buffer;
        if (dst.hasArray()) {
            buffer = dst;
        } else {
            buffer = transfer();
            buffer.clear();
            final int r = dst.remaining();
            if (r < BUFFER_SIZE) {
                buffer.limit(r);
            }
        }
        /*
         * Transfer bytes from the input stream to the buffer. The bytes are also copied to the temporary file.
         * We try to use `dst` instead of `buffer` in call to `cache(…)` because the former may be a direct buffer.
         */
        count = c.input.read(buffer.array(), Math.addExact(buffer.arrayOffset(), buffer.position()), buffer.remaining());
        if (count > 0) {
            position += count;
            final ByteBuffer slice = dst.slice();
            if (buffer != dst) {
                dst.put(buffer.limit(count));               // Transfer from temporary buffer to destination buffer.
            } else {
                dst.position(dst.position() + count);
            }
            cache(slice.limit(count));
        }
        usedConnection();
        return count;
    }

    /**
     * Attempts to read up to <var>r</var> bytes from the cache.
     * This method does not use the connection (it may be null).
     * The {@link #position} field is updated by the number of bytes read.
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
            if (count >= 0) position += count;
        } finally {
            dst.limit(limit);
        }
        assert dst.position() == start + count;
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
        assert Thread.holdsLock(this);
        long count = 0;
        final Connection c = connection;
        final InputStream input = c.input;
        for (int r; (r = input.available()) > 0;) {
            final ByteBuffer buffer = transfer();
            buffer.clear();
            if (r < BUFFER_SIZE) buffer.limit(r);
            final int n = input.read(buffer.array(), 0, buffer.limit());
            if (n < 0) break;
            cache(buffer.limit(n));
            count += n;
        }
        if (abort(c)) {
            connection = null;
        }
        return count;
    }

    /**
     * Attempts to write up to <var>r</var> bytes to the channel,
     * where <var>r</var> is the number of bytes remaining in the buffer.
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
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public void close() throws IOException {
        final Connection c = connection;
        try (file) {
            if (c != null && !abort(c)) {
                c.input.close();
            }
        } finally {
            synchronized (this) {
                transfer    = null;
                idleHandler = null;
                connection  = null;
            }
        }
    }

    /**
     * Notifies that the connection has been used and should not be closed before some timeout.
     * This method may schedule a task to be executed in a background thread after the timeout.
     * If the connection cannot read sub-ranges of bytes, then this method does nothing because
     * reopening a new connection would be costly.
     */
    private void usedConnection() {
        assert Thread.holdsLock(this);
        final Connection c = connection;
        if (c != null && c.acceptRanges) {
            final long lastReadTime = System.nanoTime();
            if (idleHandler != null) {
                idleHandler.lastReadTime = lastReadTime;
            } else {
                idleHandler = new IdleConnectionCloser(lastReadTime);
                DelayedExecutor.schedule(idleHandler);
            }
        }
    }

    /**
     * The task which has been scheduled for closing inactive connection, or {@code null} if none.
     */
    private IdleConnectionCloser idleHandler;

    /**
     * A task to execute when the connection is inactive for a time longer than the timeout.
     * This is needed because the number of connections that we can create may be small (e.g. 50),
     * and keeping an inactive connection in this channel may prevent other channels to work.
     *
     * @see #TIMEOUT
     */
    private final class IdleConnectionCloser extends DelayedRunnable {
        /**
         * Value of {@link System#nanoTime()} at the last time that {@link #read(ByteBuffer)} has been invoked.
         */
        long lastReadTime;

        /**
         * Creates a new task to be executed at the given time relative to {@link System#nanoTime()}.
         */
        IdleConnectionCloser(final long lastReadTime) {
            super(lastReadTime + TIMEOUT);
            this.lastReadTime = lastReadTime;
        }

        /**
         * Invoked in a background thread after a delay for closing a possibly inactive connections.
         * If this method confirms that the connection has been inactive for a time longer than the timeout,
         * then the connection is closed. Otherwise a new task is scheduled for checking again later.
         */
        @Override public void run() {
            synchronized (FileCacheByteChannel.this) {
                idleHandler = null;
                final Connection c = connection;
                if (c != null && c.acceptRanges) {
                    if (System.nanoTime() - lastReadTime < TIMEOUT) {
                        idleHandler = new IdleConnectionCloser(lastReadTime);
                    } else try {
                        drainAndAbort();
                    } catch (IOException e) {
                        Logging.unexpectedException(LOGGER, IdleConnectionCloser.class, "run", e);
                    }
                }
            }
        }
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public synchronized String toString() {
        return Strings.toString(getClass(), "filename", filename(), "position", position,
                "rangesOfAvailableBytes", rangesOfAvailableBytes.size(),
                "rangesOfInterest", rangesOfInterest.size());
    }
}
