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

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.io.InvalidSeekException;
import org.apache.sis.internal.storage.Resources;


/**
 * Wraps an {@link ImageInputStream} as a standard {@link InputStream}.
 *
 * <h2>Thread-safety</h2>
 * This class is thread-safe only if the underlying {@link ImageInputStream} is itself thread-safe.
 * For performance reasons, this class does not synchronize the frequently invoked {@code read(…)}
 * methods since they do nothing else than delegating to {@code ImageInputStream}. This means that
 * if the wrapped input is {@link ChannelImageInputStream}, then this class is <strong>not</strong>
 * thread-safe. This is not necessarily a contradiction with Java API since input streams define no
 * explicit synchronization lock (contrarily to {@link java.io.Reader}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.2
 *
 * @see OutputStreamAdapter
 *
 * @since 0.4
 * @module
 */
public final class InputStreamAdapter extends InputStream implements Markable {
    /**
     * The underlying data input stream. In principle, public access to this field breaks encapsulation.
     * But since {@code InputStreamAdapter} does not hold any state and just forwards every method calls
     * to that {@code ImageInputStream}, using on object or the other does not make a difference.
     */
    public final ImageInputStream input;

    /**
     * Position of the last mark created by {@link #mark(int)}. Undefined if {@link #markIndex} is negative.
     */
    private long markPosition;

    /**
     * Value of {@link #nestedMarks} at the time when {@link #markPosition} has been set, or -1 if none.
     * Used for differentiating the (single) mark created by {@link #mark(int)} from the (possibly many)
     * marks created by {@link #mark()}. This complexity exists because {@link #reset()} must comply with
     * two inconsistent {@code mark(…)} method contracts.
     */
    private int markIndex;

    /**
     * Count of marks created by {@link #mark()}, not counting the mark created by {@link #mark(int)}.
     * We have to keep this count ourselves because {@link ImageInputStream#reset()} does nothing if
     * there is no mark, and provides no API for letting us know if {@code reset()} worked.
     */
    private int nestedMarks;

    /**
     * Temporarily set to {@code true} if a call to {@link #close()} should not be propagated to the {@link #input}.
     *
     * @see RewindableLineReader#rewind()
     */
    boolean keepOpen;

    /**
     * Constructs a new input stream.
     *
     * @param  input  the stream to wrap.
     * @throws IOException  if an error occurred while creating the adapter.
     */
    public InputStreamAdapter(final ImageInputStream input) throws IOException {
        assert !(input instanceof InputStream);
        this.input = input;
        markIndex = -1;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return the next byte, or -1 if the end of the stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        return input.read();
    }

    /**
     * Reads some number of bytes from the input stream.
     *
     * @return total number of bytes read, or -1 if the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return input.read(b);
    }

    /**
     * Reads up to {@code len} bytes of data from the input stream.
     *
     * @return total number of bytes read, or -1 if the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return input.read(b, off, len);
    }

    /**
     * Skips over and discards {@code n} bytes of data from this input stream.
     *
     * @return total number of bytes skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public long skip(final long n) throws IOException {
        return input.skipBytes(n);
    }

    /**
     * Returns always {@code true}, since marks support is mandatory in image input stream.
     *
     * @return {@code true}.
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Discards the previous mark created by {@code mark(int)} and marks the current stream position.
     * This method is part of {@link InputStream} API, where only one mark can be set and multiple
     * calls to {@code reset()} move to the same position until {@code mark(int)} is invoked again.
     *
     * @param  readlimit  ignored.
     * @throws UncheckedIOException if the mark can not be set.
     */
    @Override
    public synchronized void mark(final int readlimit) {
        try {
            markPosition = input.getStreamPosition();
            if (nestedMarks == 0) {
                input.flushBefore(markPosition);
            }
            markIndex = nestedMarks;                // Set only on success.
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // InputStream.mark() does not allow us to throw IOException.
        }
    }

    /**
     * Marks the current position in this input stream.
     * This method is part of {@link Markable} API, where marks can be nested.
     */
    @Override
    public synchronized void mark() {
        input.mark();
        nestedMarks++;
    }

    /**
     * Repositions this stream to the position at the time the {@code mark} method was last called.
     * This method has to comply with both {@link InputStream#reset()} and {@link Markable#reset()}
     * contracts. It does that by pulling from most recent mark to oldest mark regardless if marks
     * were created by {@link #mark()} or {@link #mark(int)}, except that all marks created by
     * {@link #mark(int)} are ignored except the most recent one.
     *
     * <p>Implementations of {@code reset()} in Java I/O package does not discard the mark.
     * The implementation in this {@code InputStreamAdapter} class does not discard the mark
     * neither if the mark done by a call to {@link #mark(int)} is the only mark remaining.
     * Some code depends on the ability to do many {@code reset()} for the same mark.</p>
     *
     * @throws IOException if this stream can not move to the last mark position.
     */
    @Override
    public synchronized void reset() throws IOException {
        if (markIndex == nestedMarks) {
            if (markIndex != 0) {           // Do not clear if it is the only mark (see javadoc).
                markIndex = -1;             // Clear first in case of failure in next line.
            }
            input.seek(markPosition);
        } else if (nestedMarks > 0) {
            nestedMarks--;
            input.reset();
        } else {
            throw new IOException(Resources.format(Resources.Keys.StreamHasNoMark));
        }
    }

    /**
     * Moves to the given position in the stream and discards all marks at or after that position.
     * This convolved method exists because of the attempt to conciliate two different APIs in this class
     * (see {@link #reset()}). This method does not simply call {@link ImageInputStream#seek(long)}
     * because we need to keep track of the marks.
     *
     * @param  mark  position where to seek.
     * @throws IOException if this stream can not move to the specified mark position.
     */
    @Override
    public synchronized void reset(final long mark) throws IOException {
        long p;
        int n;
        do {
            n = nestedMarks;
            reset();
            p = input.getStreamPosition();
        } while (p > mark && n > 0);
        if (p != mark) {
            throw new InvalidSeekException();
        }
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     * @throws IOException if the position can not be obtained.
     */
    @Override
    public long getStreamPosition() throws IOException {
        return input.getStreamPosition();
    }

    /**
     * Closes this input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void close() throws IOException {
        if (!keepOpen) input.close();
    }
}
