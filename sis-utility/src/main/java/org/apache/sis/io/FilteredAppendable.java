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
import java.io.CharConversionException;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.util.Characters.LINE_SEPARATOR;
import static org.apache.sis.util.Characters.PARAGRAPH_SEPARATOR;


/**
 * Base class for writing filtered characters to another {@link Appendable}.
 * This base class performs a work similar to the {@link java.io.FilterWriter} work,
 * except for the following:
 *
 * <ul>
 *   <li>The filtered output is sent to an arbitrary {@link Appendable} instead than
 *       to the {@link java.io.Writer} sub-type.</li>
 *   <li>No synchronization is performed.</li>
 * </ul>
 *
 * If needed, this {@code FilteredAppendable} can be viewed as a synchronized {@link Writer}
 * by invoking the {@link #asWriter()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see java.io.FilterWriter
 */
@Decorator(Appendable.class)
public abstract class FilteredAppendable implements Appendable {
    /**
     * The underlying character output stream or buffer.
     */
    protected final Appendable out;

    /**
     * If the last character given to {@link #toCodePoint(char)} if it was a high surrogate,
     * or 0 otherwise.
     */
    private char highSurrogate;

    /**
     * Creates a new filtered formatter which will send its output to the given stream or buffer.
     *
     * @param out The underlying character output stream or buffer.
     */
    protected FilteredAppendable(final Appendable out) {
        ArgumentChecks.ensureNonNull("out", out);
        this.out = out;
    }

    /**
     * Returns {@code true} if the given character is a line separator in the sense of
     * this {@code org.apache.sis.io} package. This method performs the same work than
     * {@link org.apache.sis.util.Characters#isLineOrParagraphSeparator(int)} without
     * using the code point API. This allows simpler and faster code in subclasses working
     * only in the {@linkplain Character#isBmpCodePoint(int) Basic Multilingual Plane (BMP)}.
     * However this method assumes that all line and paragraph separators are in the BMP.
     *
     * <p>This method provides a single item to search if we need to expand our definition of
     * line separator in this package. However if such extension is needed, then developers
     * shall also search for usages of {@code LINE_SEPARATOR} and {@code PARAGRAPH_SEPARATOR}
     * constants in this package since they are sometime used directly.</p>
     *
     * @see org.apache.sis.util.Characters#isLineOrParagraphSeparator(int)
     */
    static boolean isLineSeparator(final char c) {
        return (c == '\n') || (c == '\r') || (c == LINE_SEPARATOR) || (c == PARAGRAPH_SEPARATOR);
    }

    /**
     * Returns the code point for the given character, or -1 if we need to wait for the next
     * character. This method computes the code point from the given character and the character
     * given to the previous call of this method. This works only if this method is consistently
     * invoked for every characters.
     */
    final int toCodePoint(final char c) throws IOException {
        final char h = highSurrogate;
        if (h != 0) {
            highSurrogate = 0;
            if (Character.isLowSurrogate(c)) {
                return Character.toCodePoint(h, c);
            } else {
                throw new CharConversionException();
            }
        }
        if (Character.isHighSurrogate(c)) {
            highSurrogate = c;
            return -1;
        }
        return c;
    }

    /**
     * Returns {@code true} if the last character given to {@link #toCodePoint(char)}
     * is a {@linkplain Character#isHighSurrogate(char) high surrogate}.
     */
    final boolean isHighSurrogate() {
        return highSurrogate != 0;
    }

    /**
     * If the given sequence begins with a low surrogate completing a previous high surrogate,
     * delegates to {@link #append(char)} and returns {@code start+1}. The intend is to avoid
     * processing a character sequence which starts by an invalid code point.
     *
     * @param  sequence The character sequence to write.
     * @param  start    Index of the first character to write by this method or by the caller.
     * @param  end      Index after the last character to be written by the caller.
     * @return Index of the first character which need to be written by the caller.
     */
    final int appendSurrogate(final CharSequence sequence, int start, final int end) throws IOException {
        if (start != end && highSurrogate != 0) {
            final char c = sequence.charAt(start);
            if (Character.isLowSurrogate(c)) {
                append(c);
                start++;
            } else {
                throw new CharConversionException();
            }
        }
        return start;
    }

    /**
     * Appends the given code point to the underlying {@link #out} stream or buffer.
     *
     * @param  c The code point to append.
     * @throws IOException If an error occurred while appending the code point.
     */
    final void appendCodePoint(final int c) throws IOException {
        if (Character.isBmpCodePoint(c)) {
            out.append((char) c);
        } else if (Character.isSupplementaryCodePoint(c)) {
            out.append(Character.highSurrogate(c))
               .append(Character. lowSurrogate(c));
        } else {
            throw new CharConversionException();
        }
    }

    /**
     * Appends the specified character sequence.
     * The default implementation delegates to {@link #append(CharSequence, int, int)}.
     *
     * @param  sequence The character sequence to append, or {@code null}.
     * @return A reference to this {@code Appendable}.
     * @throws IOException If an I/O error occurred.
     */
    @Override
    public Appendable append(CharSequence sequence) throws IOException {
        if (sequence == null) {
            sequence = "null";
        }
        append(sequence, 0, sequence.length());
        return this;
    }

    /**
     * If the given {@code out} argument implements {@link Flushable}, or is a
     * {@code FilteredAppendable} wrapper around a flushable object, delegates
     * to that object. Otherwise do nothing.
     */
    static void flush(Appendable out) throws IOException {
        while (!(out instanceof Flushable)) {
            if (!(out instanceof FilteredAppendable)) {
                return;
            }
            out = ((FilteredAppendable) out).out;
        }
        ((Flushable) out).flush();
    }

    /**
     * If the given {@code out} argument implements {@link Closeable}, or is a
     * {@code FilteredAppendable} wrapper around a closeable object, delegates
     * to that object. Otherwise do nothing.
     */
    static void close(Appendable out) throws IOException {
        while (!(out instanceof Closeable)) {
            if (out instanceof Flushable) {
                ((Flushable) out).flush();
            }
            if (!(out instanceof FilteredAppendable)) {
                return;
            }
            out = ((FilteredAppendable) out).out;
        }
        ((Closeable) out).close();
    }

    /**
     * Returns a view of this {@code Appendable} as a writer. Any write operations performed
     * on the writer will be forwarded to this {@code Appendable} in a synchronized block.
     *
     * @return A view of this {@code Appendable} as a writer.
     */
    public Writer asWriter() {
        // No need to cache this instance, since creating AppendableAdapter
        // is cheap and AppendableAdapter does not hold any internal state.
        return new AppendableAdapter(this);
    }
}
