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
import javax.imageio.stream.ImageInputStream;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.UncheckedIOException;


/**
 * Wraps an {@link ImageInputStream} as a standard {@link InputStream}.
 *
 * <div class="section">Thread-safety</div>
 * This class is thread-safe only if the underlying {@link ImageInputStream} is itself thread-safe.
 * For performance reasons, this class does not synchronize the frequently invoked {@code read(…)}
 * methods since they do nothing else than delegating to {@code ImageInputStream}. This means that
 * if the wrapped input is {@link ChannelImageInputStream}, then this class is <strong>not</strong>
 * thread-safe. This is not necessarily a contradiction with Java API since input streams define no
 * explicit synchronization lock (contrarily to {@link java.io.Reader}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
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
     * Position of the last mark created by {@link #mark(int)}, or the file beginning if there is no mark.
     */
    private long markPosition;

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
        markPosition = input.getStreamPosition();
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
     * Discards all previous marks and marks the current position in this input stream.
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
            input.flushBefore(markPosition);
            nestedMarks = 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // InputStream.mark() does not allow us to throw IOException.
        }
    }

    /**
     * Marks the current position in this input stream.
     * This method is part of {@link Markable} API, where marks can be nested.
     * It is okay to invoke this method after {@link #mark(int)} (but not before).
     */
    @Override
    public synchronized void mark() {
        input.mark();
        nestedMarks++;
    }

    /**
     * Repositions this stream to the position at the time the {@code mark} method was last called.
     * This method has to comply with both {@link InputStream#reset()} and {@link Markable#reset()}
     * contracts. It does that by choosing the first option in following list:
     *
     * <ul>
     *   <li>If there is nested {@link #mark()} calls, then this {@code reset()} method sets the stream
     *       position to the most recent unmatched call to {@code mark()}.</li>
     *   <li>Otherwise if the {@link #mark(int)} method has been invoked, then this method sets the stream
     *       position to the mark created by the most recent call to {@code mark(int)}. The {@code reset()}
     *       method can be invoked many time; it will always set the position to the same mark
     *       (this behavior is required by {@link InputStream} contract).</li>
     *   <li>Otherwise this method sets the stream position to the position it had when this
     *       {@code InputStreamAdapter} has been created.</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void reset() throws IOException {
        if (--nestedMarks >= 0) {
            input.reset();
        } else {
            input.seek(markPosition);
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
