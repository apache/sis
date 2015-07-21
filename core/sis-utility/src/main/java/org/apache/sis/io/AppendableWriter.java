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
import java.io.StringWriter;
import java.io.IOException;
import java.nio.CharBuffer;


/**
 * Wraps a {@link Appendable} as a {@link Writer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class AppendableWriter extends Writer {
    /**
     * The underlying character output stream or buffer.
     */
    private final Appendable out;

    /**
     * Creates a new filtered formatter which will send its output to the given stream or buffer.
     *
     * @param out The underlying character output stream or buffer.
     */
    AppendableWriter(final Appendable out) {
        super(getLock(out));
        this.out = out;
    }

    /**
     * Returns the synchronization lock to use for writing to the given {@code Appendable}.
     * In particular if the final destination is a {@link StringBuffer}, we want to lock on
     * that buffer since it is already synchronized on itself (so we get only one lock, not
     * two). If the final destination is an other writer, we would use its {@link Writer#lock}
     * field if it wasn't protected... As a fallback we use the writer itself, since writers
     * are often synchronized on themselves.
     */
    private static Object getLock(Appendable out) {
        while (out instanceof Appender) {
            out = ((Appender) out).out;
        }
        // StringWriter performs its synchronizations on its StringBuffer.
        if (out instanceof StringWriter) {
            return ((StringWriter) out).getBuffer();
        }
        return out;
    }

    /**
     * Forwards the given single character to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public Writer append(final char c) throws IOException {
        synchronized (lock) {
            out.append(c);
        }
        return this;
    }

    /**
     * Forwards the given single character to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void write(int c) throws IOException {
        synchronized (lock) {
            out.append((char) c);
        }
    }

    /**
     * Forwards the given character sequence to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public Writer append(final CharSequence sequence) throws IOException {
        synchronized (lock) {
            out.append(sequence);
        }
        return this;
    }

    /**
     * Forwards the given string to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void write(final String string) throws IOException {
        synchronized (lock) {
            out.append(string);
        }
    }

    /**
     * Forwards the given character sub-sequence to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public Writer append(final CharSequence sequence, final int start, final int end) throws IOException {
        synchronized (lock) {
            out.append(sequence, start, end);
        }
        return this;
    }

    /**
     * Forwards the given sub-string to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void write(final String string, final int start, final int length) throws IOException {
        synchronized (lock) {
            out.append(string, start, start + length);
        }
    }

    /**
     * Forwards the given character array to {@link #out}.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void write(final char[] cbuf, final int offset, final int length) throws IOException {
        synchronized (lock) {
            out.append(CharBuffer.wrap(cbuf, offset, length));
        }
    }

    /**
     * If {@link #out} implements {@link Flushable}, or is a {@link Appender} wrapper
     * around a flushable object, delegates to that object. Otherwise do nothing.
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void flush() throws IOException {
        synchronized (lock) {
            IO.flush(out);
        }
    }

    /**
     * If {@link #out} implements {@link Closeable}, or is a {@link Appender} wrapper
     * around a closeable object, delegates to that object. Otherwise just flush (if possible).
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void close() throws IOException {
        synchronized (lock) {
            IO.close(out);
        }
    }

    /**
     * Returns the content of the underlying {@link Appendable} as a string if possible,
     * or the localized <cite>"Unavailable content"</cite> string otherwise.
     *
     * @return The content of the underlying {@code Appendable},
     *         or a localized message for unavailable content.
     *
     * @see IO#content(Appendable)
     */
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public String toString() {
        synchronized (lock) {
            return IO.toString(out);
        }
    }
}
