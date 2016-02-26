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

import java.io.Flushable;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import java.io.CharArrayWriter;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Utilities methods working on {@link java.io} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class IO extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private IO() {
    }

    /**
     * If the given {@code out} argument implements {@link Flushable}, or is a chain
     * of wrappers defined in this package around a flushable object, invokes the
     * {@link Flushable#flush() flush()} method on that object. Otherwise do nothing.
     *
     * <p>Chains of wrappers are followed until a {@code Flushable} instance is found, if any.
     * The search stops at the first occurrence found.</p>
     *
     * @param  out The stream or buffer to flush, or {@code null}.
     * @throws IOException if an error occurred while flushing the given stream.
     */
    public static void flush(Appendable out) throws IOException {
        while (!(out instanceof Flushable)) {
            if (!(out instanceof Appender)) {
                return;
            }
            out = ((Appender) out).out;
        }
        ((Flushable) out).flush();
    }

    /**
     * If the given {@code out} argument implements {@link Closeable}, or is a chain
     * of wrappers defined in this package around a closeable object, invokes the
     * {@link Closeable#close() close()} method on that object. Otherwise do nothing.
     *
     * <p>Chains of wrappers are followed until a {@code Closeable} instance is found, if any.
     * The first {@link Flushable} instance found <em>before</em> the {@code Closeable} one,
     * if any, is {@linkplain Flushable#flush() flushed}.
     * The search stops at the first {@code Closeable} occurrence found.</p>
     *
     * @param  out The stream or buffer to close, or {@code null}.
     * @throws IOException if an error occurred while closing the given stream.
     */
    public static void close(Appendable out) throws IOException {
        boolean isFlushed = false;
        while (!(out instanceof Closeable)) {
            if (!isFlushed && out instanceof Flushable) {
                ((Flushable) out).flush();
                isFlushed = true;
            }
            if (!(out instanceof Appender)) {
                return;
            }
            out = ((Appender) out).out;
        }
        ((Closeable) out).close();
    }

    /**
     * If the given {@code out} argument implements {@link CharSequence}, or is a
     * chain of wrappers defined in this package around a {@code CharSequence},
     * returns that character sequence. Otherwise returns {@code null}.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If an {@code Appendable} is a {@link StringWriter} instance, then
     *       its {@linkplain StringWriter#getBuffer() underlying buffer} is returned.</li>
     *   <li>If an {@code Appendable} is a {@link CharArrayWriter} instance, then its content
     *       is returned {@linkplain CharArrayWriter#toString() as a string}.
     * </ul>
     *
     * <p>This method is useful for getting the result of an {@code Appendable} which wrote,
     * directly or indirectly, into a {@link StringBuilder} or similar kind of character buffer.
     * Note that this method returns the underlying buffer if possible; callers should not change
     * {@code CharSequence} content, unless the {@code Appendable} is not used anymore after this
     * method call.</p>
     *
     * <p>It may be necessary to invoke {@link #flush(Appendable)} before this method in order
     * to get proper content. In particular, this is necessary if the chain of {@code Appendable}s
     * contains {@link TableAppender} or {@link LineAppender} instances.</p>
     *
     * @param  out The stream or buffer from which to get the content, or {@code null}.
     * @return The content of the given stream of buffer, or {@code null} if unavailable.
     *
     * @see #flush(Appendable)
     */
    public static CharSequence content(Appendable out) {
        while (!(out instanceof CharSequence)) {
            if (!(out instanceof Appender)) {
                if (out instanceof StringWriter) {
                    return ((StringWriter) out).getBuffer();
                }
                if (out instanceof CharArrayWriter) {
                    return ((CharArrayWriter) out).toString();
                }
                return null;
            }
            out = ((Appender) out).out;
        }
        return (CharSequence) out;
    }

    /**
     * Returns the content of the given {@code Appendable} as a string if possible,
     * or the localized <cite>"Unavailable content"</cite> string otherwise.
     */
    static String toString(final Appendable out) {
        final CharSequence content = IO.content(out);
        if (content != null) {
            return content.toString();
        }
        return Vocabulary.format(Vocabulary.Keys.UnavailableContent);
    }

    /**
     * Returns a view of the given {@code Appendable} as a {@code Writer}.
     * If the given argument is already a {@code Writer} instance, then it is returned unchanged.
     * Otherwise if the argument is non-null, then it is wrapped in an adapter.
     * Any write operations performed on the returned writer will be forwarded
     * to the given {@code Appendable}.
     *
     * @param  out The stream or buffer to view as a {@link Writer}, or {@code null}.
     * @return A view of this {@code Appendable} as a writer, or {@code null} if the
     *         given argument was null.
     */
    public static Writer asWriter(final Appendable out) {
        if (out == null || out instanceof Writer) {
            return (Writer) out;
        }
        return new AppendableWriter(out);
    }
}
