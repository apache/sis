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
import java.io.IOException;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.io.X364.ESCAPE;
import static org.apache.sis.io.X364.AFTER_ESCAPE;
import static org.apache.sis.util.Characters.HYPHEN;
import static org.apache.sis.util.Characters.SOFT_HYPHEN;
import static org.apache.sis.util.Characters.isLineOrParagraphSeparator;


/**
 * An {@link Appendable} which wraps the lines to some maximal line length.
 * The default line length is 80 characters, but can be changed by a call to
 * {@link #setMaximalLineLength(int)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@Decorator(Appendable.class)
public class LineWrapFormatter extends FilteredAppendable implements Flushable {
    /**
     * The line separator. We will use the first line separator found in the
     * text to write, or the system default if none.
     */
    private String lineSeparator;

    /**
     * The maximal line length, in units of <em>code points</em> (not {@code char}).
     */
    private int maximalLineLength;

    /**
     * The length of the current line, in units of <em>code points</em> (not {@code char}).
     * It may be greater than the length of {@link #buffer} because the later contains only
     * the last word.
     */
    private int codePointCount;

    /**
     * {@code true} if an escape sequence is in progress. The escape sequence will stop
     * after the first non-digit character other than {@link #IGNORE_AFTER_ESCAPE}.
     */
    private boolean isEscapeSequence;

    /**
     * The buffer for the last word being written.
     */
    private final StringBuilder buffer = new StringBuilder(16);

    /**
     * Constructs a formatter which will wrap the lines at a maximum of 80 characters.
     * The maximal line length can be changed by a call to {@link #setMaximalLineLength(int)}.
     *
     * @param out The underlying stream or buffer to write to.
     */
    public LineWrapFormatter(final Appendable out) {
        super(out);
        maximalLineLength = 80;
    }

    /**
     * Constructs a formatter which will wrap the lines at a given maximal length.
     *
     * @param out The underlying stream or buffer to write to.
     * @param length The maximal line length.
     */
    public LineWrapFormatter(final Appendable out, final int length) {
        super(out);
        ArgumentChecks.ensureStrictlyPositive("length", length);
        maximalLineLength = length;
    }

    /**
     * Returns the maximal line length. The default value is 80.
     *
     * @return The current maximal line length.
     */
    public int getMaximalLineLength() {
        return maximalLineLength;
    }

    /**
     * Sets the maximal line length.
     *
     * @param length The new maximal line length.
     */
    public void setMaximalLineLength(final int length) {
        ArgumentChecks.ensureStrictlyPositive("length", length);
        maximalLineLength = length;
    }

    /**
     * Removes the soft hyphen characters from the given buffer. This is invoked
     * when the buffer is about to be written without being split on two lines.
     */
    private static void deleteSoftHyphen(final StringBuilder buffer) {
        for (int i=buffer.length(); --i>=0;) {
            if (buffer.charAt(i) == SOFT_HYPHEN) {
                buffer.deleteCharAt(i);
            }
        }
    }

    /**
     * Writes the specified code point.
     *
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("fallthrough")
    private void write(final int c) throws IOException {
        final StringBuilder buffer = this.buffer;
        if (isLineOrParagraphSeparator(c)) {
            deleteSoftHyphen(buffer);
            out.append(buffer);
            appendCodePoint(c);
            buffer.setLength(0);
            codePointCount = 0;
            isEscapeSequence = false; // Handle line-breaks as "end of escape sequence".
            return;
        }
        if (c == ESCAPE) {
            buffer.append(ESCAPE);
            isEscapeSequence = true;
            return;
        }
        if (Character.isSpaceChar(c)) {
            deleteSoftHyphen(buffer);
            out.append(buffer);
            buffer.setLength(0);
            isEscapeSequence = false; // Handle spaces as "end of escape sequence".
        }
        buffer.appendCodePoint(c);
        /*
         * Special handling of ANSI X3.64 escape sequences. Since they are not visible
         * characters (they are used for controlling the colors), do not count them.
         */
        if (isEscapeSequence) {
            if (c < '0' || c > '9') {
                if (c == AFTER_ESCAPE) {
                    final int previous = buffer.length() - 2;
                    if (previous >= 0 && buffer.charAt(previous) == ESCAPE) {
                        return; // Found the character to ignore.
                    }
                }
                isEscapeSequence = false;
                // The first character after the digits is not counted neither,
                // so we exit this method for it too.
            }
            return;
        }
        /*
         * The remaining of this method is executed only if we have exceeded the maximal line
         * length. First search for the hyphen character, if any. If we find one and if it is
         * preceeded by a letter, split there. The "letter before" condition is a way to avoid
         * to split at the minus sign of negative numbers like "-99", assuming that the minus
         * sign is preceeded by a space. We can not look at the character after since we may
         * not know it yet.
         */
        if (++codePointCount > maximalLineLength) {
            int n;
searchHyp:  for (int i=buffer.length(); i>0; i-=n) {
                final int b = buffer.codePointBefore(i);
                n = Character.charCount(b);
                switch (b) {
                    case '-': {
                        if (i>=n && !Character.isLetter(buffer.codePointBefore(i-n))) {
                            continue; // Continue searching previous characters.
                        }
                        // fall through
                    }
                    case HYPHEN:
                    case SOFT_HYPHEN: {
                        out.append(buffer, 0, i);
                        buffer.delete(0, i);
                        break searchHyp;
                    }
                }
            }
            /*
             * The append(CharSequence,int,int) method tries to detect the line separator
             * used in the document to be formatted. But if no line separator can be found,
             * then fallback on the system default.
             */
            if (lineSeparator == null) {
                lineSeparator = System.lineSeparator();
            }
            out.append(lineSeparator);
            final int length = buffer.length();
            codePointCount = buffer.codePointCount(0, length);
            for (int i=0; i<length;) {
                final int s = buffer.codePointAt(i);
                if (!Character.isSpaceChar(s)) {
                    buffer.delete(0, i);
                    return;
                }
                i += Character.charCount(s);
                codePointCount--;
            }
            // If we reach this point, only spaces were found in the buffer.
            assert codePointCount == 0 : codePointCount;
            buffer.setLength(0);
        }
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
            write(cp);
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
    public Appendable append(final CharSequence sequence, int start, final int end) throws IOException {
        ArgumentChecks.ensureValidIndexRange(sequence.length(), start, end);
        if (lineSeparator == null) {
            /*
             * Use the line separator found in the submitted document, if possible.
             * If we don't find any line separator in the submitted content, leave
             * the 'lineSeparator' field to null since the 'write' method will set
             * it to the default value only if it really needs it.
             */
            lineSeparator = lineSeparator(sequence, start, end);
        }
        start = appendSurrogate(sequence, start, end);
        while (start < end) {
            final int c = toCodePoint(sequence.charAt(start++));
            if (c >= 0) {
                write(c);
            }
        }
        return this;
    }

    /**
     * Sends pending characters to the underlying stream. Note that this method should
     * preferably be invoked at the end of a word, sentence or line, since invoking it
     * may prevent {@code LineWrapFormatter} to properly wrap the current line if it is
     * in the middle of a word.
     *
     * <p>Invoking this method also flushes the {@linkplain #out underlying stream}.
     * A cheaper way to send pending characters is to make sure that the last character
     * is a {@linkplain org.apache.sis.util.Characters#isLineOrParagraphSeparator(int)
     * line or paragraph terminator}.</p>
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        out.append(buffer);
        buffer.setLength(0);
        IO.flush(out);
    }
}
