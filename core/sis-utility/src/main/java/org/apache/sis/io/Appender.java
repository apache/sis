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

import java.io.IOException;
import java.io.CharConversionException;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.util.Characters.isLineOrParagraphSeparator;


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
 * If needed, this {@code Appender} can be viewed as a synchronized
 * {@link java.io.Writer} by invoking the {@link IO#asWriter(Appendable)} method.
 *
 * <div class="section">Flushing and closing the stream</div>
 * Subclasses implement the {@link java.io.Flushable} interface only if they
 * hold data in an internal buffer before to send them to the wrapped {@code Appendable}.
 * This is the case of {@link TableAppender} and {@link LineAppender} for instance.
 * For unconditionally flushing or closing an {@code Appendable} and its underlying stream,
 * see {@link IO#flush(Appendable)} and {@link IO#close(Appendable)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 *
 * @see java.io.FilterWriter
 *
 * @since 0.3
 * @module
 */
abstract class Appender implements Appendable {
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
     * Creates a new appender which will send its output to the given stream or buffer.
     *
     * @param out  the underlying character output stream or buffer.
     */
    protected Appender(final Appendable out) {
        ArgumentChecks.ensureNonNull("out", out);
        this.out = out;
    }

    /**
     * Finds the line separator used in the given character sequence portion, or returns
     * {@code null} if unknown. This method is designed for invocation at the beginning
     * of {@code append(CharSequence, ...)}, before the characters are effectively read.
     */
    final String lineSeparator(final CharSequence sequence, int start, final int end) {
        if (isHighSurrogate()) {
            start++;                                        // Skip invalid character.
        }
        while (start < end) {
            final int c = Character.codePointAt(sequence, start);
            final int b = start;
            start += Character.charCount(c);
            if (isLineOrParagraphSeparator(c)) {
                if (c == '\r' && (start < end) && sequence.charAt(start) == '\n') {
                    start++;
                }
                return sequence.subSequence(b, start).toString();
            }
        }
        return null;
    }

    /**
     * Returns the code point for the given character, or -1 if we need to wait for the next
     * character. This method computes the code point from the given character and the character
     * given to the previous call of this method. This works only if this method is consistently
     * invoked for every characters.
     */
    final int toCodePoint(final char c) {
        final char h = highSurrogate;
        if (h != 0) {
            highSurrogate = 0;
            if (Character.isLowSurrogate(c)) {
                return Character.toCodePoint(h, c);
            }
            /*
             * If we reach this point, we have unpaired surrogate.  This is usually an error,
             * but this not the fault of this class since we are processing data supplied by
             * the user. Be lenient.
             */
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
     * @param  sequence  the character sequence to write.
     * @param  start     index of the first character to write by this method or by the caller.
     * @param  end       index after the last character to be written by the caller.
     * @return index of the first character which need to be written by the caller.
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
     * @param  c  the code point to append.
     * @throws IOException if an error occurred while appending the code point.
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
     * @param  sequence  the character sequence to append, or {@code null}.
     * @return a reference to this {@code Appendable}.
     * @throws IOException if an I/O error occurred.
     */
    @Override
    public Appendable append(CharSequence sequence) throws IOException {
        if (sequence == null) {
            sequence = "null";
        }
        return append(sequence, 0, sequence.length());
    }

    /**
     * Returns the content of this {@code Appendable} as a string if possible,
     * or the localized <cite>"Unavailable content"</cite> string otherwise.
     *
     * @return the content of this {@code Appendable}, or a localized message for unavailable content.
     *
     * @see IO#content(Appendable)
     */
    @Override
    public String toString() {
        return IO.toString(this);
    }
}
