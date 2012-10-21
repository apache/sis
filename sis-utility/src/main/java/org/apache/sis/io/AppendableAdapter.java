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
package org.apache.sis.io;

import java.io.Writer;
import java.io.Flushable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;


/**
 * Wraps a {@link Appendable} as a {@link Writer}. This adapter performs the
 * synchronizations on the {@linkplain #out underlying stream or buffer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class AppendableAdapter extends Writer {
    /**
     * The underlying character output stream or buffer.
     */
    private final Appendable out;

    /**
     * Creates a new filtered formatter which will send its output to the given stream or buffer.
     *
     * @param out The underlying character output stream or buffer.
     */
    AppendableAdapter(final Appendable out) {
        super(out); // Synchronization lock.
        this.out = out;
    }

    /**
     * Forwards the given single character to {@link #out}.
     */
    @Override
    public Writer append(final char c) throws IOException {
        synchronized (out) {
            out.append(c);
        }
        return this;
    }

    /**
     * Forwards the given single character to {@link #out}.
     */
    @Override
    public void write(int c) throws IOException {
        synchronized (out) {
            out.append((char) c);
        }
    }

    /**
     * Forwards the given character sequence to {@link #out}.
     */
    @Override
    public Writer append(final CharSequence sequence) throws IOException {
        synchronized (out) {
            out.append(sequence);
        }
        return this;
    }

    /**
     * Forwards the given string to {@link #out}.
     */
    @Override
    public void write(final String string) throws IOException {
        synchronized (out) {
            out.append(string);
        }
    }

    /**
     * Forwards the given character sub-sequence to {@link #out}.
     */
    @Override
    public Writer append(final CharSequence sequence, final int start, final int end) throws IOException {
        synchronized (out) {
            out.append(sequence, start, end);
        }
        return this;
    }

    /**
     * Forwards the given sub-string to {@link #out}.
     */
    @Override
    public void write(final String string, final int start, final int length) throws IOException {
        synchronized (out) {
            out.append(string, start, start + length);
        }
    }

    /**
     * Forwards the given character array to {@link #out}.
     */
    @Override
    public void write(final char[] cbuf, final int offset, final int length) throws IOException {
        synchronized (lock) {
            out.append(CharBuffer.wrap(cbuf, offset, length));
        }
    }

    /**
     * IF {@link #out} implements {@link Flushable}, or is a {@link FilteredAppendable} wrapper
     * around a flushable object, delegates to that object. Otherwise do nothing.
     */
    @Override
    public void flush() throws IOException {
        FilteredAppendable.flush(out);
    }

    /**
     * IF {@link #out} implements {@link Closeable}, or is a {@link FilteredAppendable} wrapper
     * around a closeable object, delegates to that object. Otherwise just flush (if possible).
     */
    @Override
    public void close() throws IOException {
        FilteredAppendable.close(out);
    }
}
