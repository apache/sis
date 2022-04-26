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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.InvalidSeekException;


/**
 * A {@link LineNumberReader} which may be rewound to its original position even if the mark is no longer valid.
 * This class assumes that {@link #mark(int)} has not been invoked, or has been invoked at the position where to
 * rewind. A call to {@link #rewind()} performs the following actions:
 *
 * <ul>
 *   <li>Attempt to call {@link #reset()}.</li>
 *   <li>If {@code reset()} failed, then attempt to seek the input stream to its original position and create a new reader.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
public final class RewindableLineReader extends LineNumberReader {
    /**
     * Size of the buffer, in number of characters.
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * The input stream, or {@code null} if this reader can not rewind anymore.
     *
     * <div class="note"><b>Note:</b> we do not use the more generic {@link java.io.InputStream} class
     * because this whole {@code RewindableLineReader} class is useless if we can not seek in this stream.</div>
     */
    private InputStreamAdapter input;

    /**
     * The character encoding, or {@code null} for the platform default.
     */
    private final Charset encoding;

    /**
     * Whether calls to {@link #mark(int)} and {@link #reset()} should throw {@link IOException}.
     * This is initially {@code false} and may be set to {@code true} if the caller wants to block
     * users from overwriting the mark (s)he just did. This flag changes also the {@link #close()}
     * behavior. It is used for probing the content of the same file by different data stores and we
     * want a safety against implementations that do not follow the {@code probeContent(…)} contract.
     */
    private boolean isMarkProtected;

    /**
     * Creates a line reader wrapping the given input stream.
     *
     * @param  input     the input stream from which to read characters.
     * @param  encoding  the character encoding, or {@code null} for the platform default.
     * @throws IOException if an error occurred while marking the reader.
     */
    public RewindableLineReader(final InputStream input, final Charset encoding) throws IOException {
        super(encoding != null ? new InputStreamReader(input, encoding)
                               : new InputStreamReader(input), BUFFER_SIZE);
        if (input instanceof InputStreamAdapter) {
            this.input = (InputStreamAdapter) input;
        }
        this.encoding = encoding;
        /*
         * By default, this.lock is set to InputStreamReader. But InputStreamReader.lock has itself
         * been set on the given InputStreamAdapter.  So we set this.lock to InputStreamReader.lock
         * in order to have a single synchronization lock.
         */
        lock = input;
        super.mark(BUFFER_SIZE);
    }

    /**
     * Returns a reader rewound to the beginning of data to read. This method invokes {@link #reset()} first.
     * If that call succeed, then this method returns {@code this}. Otherwise this method returns a new reader.
     * In the latter case, {@code this} reader should not be used anymore.
     *
     * @return the reader to use for next read operation (may be {@code this}).
     * @throws IOException if an error occurred while rewinding the reader.
     */
    public RewindableLineReader rewind() throws IOException {
        synchronized (lock) {
            try {
                super.reset();
                return this;
            } catch (IOException e1) {
                final InputStreamAdapter stream = input;
                if (stream == null) {
                    throw new InvalidSeekException();
                }
                /*
                 * Releases the resources used by this reader, but without closing the underlying input stream.
                 * This reader is considered closed after this method call, but the underlying input stream can
                 * still be given to another reader.
                 */
                input = null;
                assert Thread.holdsLock(stream);    // Should be okay since we set lock = stream in constructor.
                try {
                    stream.keepOpen = true;
                    super.close();
                } finally {
                    stream.keepOpen = false;
                }
                /*
                 * Try to seek to the data origin. Note that `seek(0)` below does not necessarily
                 * move to the beginning of file, because `ChannelDataInput` may contain an offset.
                 */
                try {
                    stream.input.seek(0);
                } catch (IOException e2) {
                    e2.addSuppressed(e1);
                    throw e2;
                }
                return new RewindableLineReader(stream, encoding);
            }
        }
    }

    /**
     * Marks current stream position and blocks all subsequent calls to {@link #mark(int)},
     * {@link #reset()} and {@link #close()} until {@link #protectedReset()} is invoked.
     *
     * @throws IOException if the stream can not be marked.
     */
    public final void protectedMark() throws IOException {
        synchronized (lock) {
            super.mark(BUFFER_SIZE);
            isMarkProtected = true;
        }
    }

    /**
     * Stops the protection given by {@link #protectedMark()} and reset the stream to its marked position.
     *
     * @throws IOException if the stream can not be reset.
     */
    public final void protectedReset() throws IOException {
        synchronized (lock) {
            isMarkProtected = false;
            super.reset();
        }
    }

    /**
     * Tells whether this stream supports the mark and reset operations.
     * This is {@code true} by default but can be set to {@code false}
     * if the mark is reserved to internal usage.
     *
     * @return true if this stream supports the mark operation.
     */
    @Override
    public boolean markSupported() {
        synchronized (lock) {
            return !isMarkProtected;
        }
    }

    /**
     * Marks the present position in the stream if allowed to do so.
     * If {@link #isMarkProtected} is {@code true}, then this method throws an exception.
     *
     * @param  readlimit  limit on the number of characters that may be read.
     * @throws IOException if the stream can not be marked.
     */
    @Override
    public void mark(final int readlimit) throws IOException {
        synchronized (lock) {
            if (isMarkProtected) {
                throw new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, "mark"));
            }
            super.mark(readlimit);
        }
    }

    /**
     * Resets the stream if allowed to do so.
     * If {@link #isMarkProtected} is {@code true}, then this method throws an exception.
     *
     * @throws IOException if the stream can not be reset.
     */
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            if (isMarkProtected) {
                throw new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, "reset"));
            }
            super.reset();
        }
    }

    /**
     * Closes this reader. The underlying stream will be either reset or closed
     * depending on the {@link #isMarkProtected} mode.
     *
     * @throws IOException if an error occurred while closing the reader.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            input = null;
            if (isMarkProtected) {
                super.reset();
            } else {
                super.close();
            }
        }
    }
}
