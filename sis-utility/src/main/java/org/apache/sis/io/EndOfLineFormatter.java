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
import org.apache.sis.util.Decorator;
import org.apache.sis.util.Characters;
import org.apache.sis.util.ArgumentChecks;


/**
 * An {@link Appendable} that writes characters while replacing various EOL by a unique string.
 * This class performs two works:
 *
 * <ul>
 *   <li>Replace all occurrences of {@linkplain #isLineSeparator(int) line separators} by
 *       the {@linkplain System#lineSeparator() platform-depend EOL string} ({@code "\r\n"}
 *       on Windows and {@code "\n"} on Unix), or any other string
 *       {@linkplain #setLineSeparator(String) explicitly set}.</li>
 *   <li>Remove trailing blanks before end of lines (this behavior can be modified
 *       by overriding the {@link #isIgnorable(int)} method).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@Decorator(Appendable.class)
public class EndOfLineFormatter extends FilteredAppendable {
    /**
     * The line separator for End Of Line (EOL).
     */
    private String lineSeparator;

    /**
     * {@code true} if the next character needs to be skipped if equals to {@code '\n'}.
     * This field is used in order to avoid writing two EOL in place of "\r\n".
     */
    private boolean skipLF;

    /**
     * Temporary buffer containing trailing ignorable characters. Those characters are stored in
     * this buffer before to be written. If ignorable characters are followed by a non-ignorable
     * one, then the ignorable characters are written to the underlying stream before the
     * non-ignorable one. Otherwise if ignorable characters are followed by a line separator,
     * then they are discarded.
     */
    private final StringBuilder ignorables = new StringBuilder(4);

    /**
     * Constructs a formatter which will use the platform-dependent line separator.
     *
     * @param out The underlying stream or buffer to write to.
     */
    public EndOfLineFormatter(final Appendable out) {
        super(out);
        lineSeparator = System.lineSeparator();
    }

    /**
     * Constructs a formatter which will use the specified line separator.
     *
     * @param out The underlying stream or buffer to write to.
     * @param lineSeparator String to use as line separator.
     */
    public EndOfLineFormatter(final Appendable out, final String lineSeparator) {
        super(out);
        ArgumentChecks.ensureNonNull("lineSeparator", lineSeparator);
        this.lineSeparator = lineSeparator;
    }

    /**
     * Returns the current line separator.
     *
     * @return The current line separator.
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Changes the line separator. This is the string to insert in place of every occurrences of
     * a {@linkplain #isLineSeparator(int) line separator} (typically {@code "\r"}, {@code "\n"}
     * or {@code "\r\n"}).
     *
     * @param  lineSeparator The new line separator.
     */
    public void setLineSeparator(final String lineSeparator) {
        ArgumentChecks.ensureNonNull("lineSeparator", lineSeparator);
        this.lineSeparator = lineSeparator;
    }

    /**
     * Writes a line separator.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void writeEOL() throws IOException {
        assert ignorables.length() == 0;
        out.append(lineSeparator);
    }

    /**
     * Flushes the content of {@link #ignorables} to the underlying stream.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void writeIgnorables() throws IOException {
        out.append(ignorables);
        ignorables.setLength(0);
    }

    /**
     * Writes a portion of a sequence of characters, excluding trailing ignorable characters.
     * This given portion shall <strong>not</strong> contains any line separator.
     *
     * <p>After this method call, the {@link #ignorables} buffer is empty.</p>
     *
     * @return The index after the last character sent to the underlying stream, or
     *         {@code lower} if the given portion contains only ignorable characters.
     */
    private int writeTrimmedLine(final CharSequence sequence, final int lower, int upper) throws IOException {
        while (upper > lower) {
            final int c = Character.codePointBefore(sequence, upper);
            if (!isIgnorable(c)) {
                writeIgnorables();
                out.append(sequence, lower, upper);
                return upper;
            }
            upper -= Character.charCount(c);
        }
        ignorables.setLength(0);
        return upper;
    }

    /**
     * Writes a single character.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public Appendable append(final char c) throws IOException {
        final int cp = toCodePoint(c);
        if (cp >= 0) {
            if (isLineSeparator(cp)) {
                final boolean skip = skipLF && (cp == '\n');
                skipLF = (cp == '\r');
                if (!skip) {
                    ignorables.setLength(0);  // Discard ignorable characters.
                    writeEOL();
                }
            } else {
                skipLF = false;
                if (isIgnorable(cp)) {
                    ignorables.appendCodePoint(cp);
                } else {
                    writeIgnorables();
                    appendCodePoint(cp);
                }
            }
        }
        return this;
    }

    /**
     * Writes a portion of a character sequence.
     *
     * @param  sequence The character sequence to be written.
     * @param  start    Index from which to start reading characters.
     * @param  end      Index of the character following the last character to read.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public Appendable append(final CharSequence sequence, int start, int end) throws IOException {
        ArgumentChecks.ensureValidIndexRange(sequence.length(), start, end);
        start = appendSurrogate(sequence, start, end);
        if (start != end) {
            if (skipLF && sequence.charAt(start) == '\n') {
                start++;
                skipLF = false;
            }
            int upper = start;
            do {
                final int cp = toCodePoint(sequence.charAt(upper++));
                if (cp >= 0 && isLineSeparator(cp)) {
                    writeTrimmedLine(sequence, start, upper - Character.charCount(cp));
                    writeEOL();
                    if (cp == '\r' && (upper != end) && sequence.charAt(upper) == '\n') {
                        upper++;
                    }
                    start = upper;
                }
            } while (upper != end);
            /*
             * Write the remainding characters and put the
             * trailing ignorable characters into the buffer.
             */
            if (isHighSurrogate()) {
                end--;
            }
            start = writeTrimmedLine(sequence, start, end);
            ignorables.append(sequence, start, end);
            skipLF = (sequence.charAt(end - 1) == '\r');
        }
        return this;
    }

    /**
     * Returns {@code true} if the specified character shall be considered as a line separator.
     * The default implementation returns {@link Characters#isLineOrParagraphSeparator(int)}.
     * Subclasses can override this method in order to change the criterion.
     *
     * @param  c The character to test.
     * @return {@code true} if {@code c} is a line separator.
     * @throws IOException if this method can not determine if the character is a line separator.
     */
    protected boolean isLineSeparator(final int c) throws IOException {
        return Characters.isLineOrParagraphSeparator(c);
    }

    /**
     * Returns {@code true} if the specified character can be ignored on end of line.
     * The default implementation returns {@link Character#isSpaceChar(int)}.
     * Subclasses can override this method in order to change the criterion.
     *
     * @param  c The character to test.
     * @return {@code true} if {@code c} is a character that can be ignored on end of line.
     * @throws IOException if this method can not determine if the character is ignorable.
     */
    protected boolean isIgnorable(final int c) throws IOException {
        return Character.isSpaceChar(c);
    }
}
