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
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.X364;

// Related to JK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * An {@link Appendable} which can apply different kinds of reformatting that depend on the
 * <cite>End Of Line</cite> (EOL) occurrences. Available reformatting include inserting a
 * a margin before each line, wrapping to a maximal line length and replacing tabulations or
 * EOL characters. The actual work to be done can be enabled by invoking one or many of the
 * following methods:
 *
 * <ul>
 *   <li>{@link #setMaximalLineLength(int)} for wrapping the lines to some maximal line length,
 *       typically 80 Unicode characters (code points).</li>
 *   <li>{@link #setTabulationExpanded(boolean)} for replacing tabulation characters by spaces.</li>
 *   <li>{@link #setLineSeparator(String)} for replacing all occurrences of
 *       {@linkplain Characters#isLineOrParagraphSeparator(int) line separators} by the given string.</li>
 * </ul>
 *
 * In addition this class removes trailing {@linkplain Character#isWhitespace(int) whitespaces}
 * before end of lines.
 *
 * <div class="section">How line lengths are calculated</div>
 * Line length are measured in unit of Unicode <cite>code points</cite>. This is usually the same
 * than the number of {@code char} primitive values, but not always. Combining characters are not
 * yet recognized by this class, but future versions may improve on that.
 *
 * <p>For proper line length calculation in presence of tabulation characters ({@code '\t'}),
 * this class needs to known the tabulation width. The default value is 8, but this can be changed
 * by a call to {@link #setTabulationWidth(int)}. Note that invoking that method affects only line
 * length calculation; it does not replace tabulations by spaces. For tabulation expansion, see
 * {@link #setTabulationExpanded(boolean)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public class LineAppender extends Appender implements Flushable {
    /**
     * The line separator, or {@code null} if not yet determined. If {@code null}, then the
     * {@link #append(CharSequence, int, int)} method will try to infer it from the submitted text.
     *
     * <p>If {@link #isEndOfLineReplaced} is {@code false} (the default), then this line separator
     * will be used only when this class inserts new line separators as a consequence of line wraps;
     * line separators found in the texts given by the user will be passed "as is". If {@code true},
     * then all line separators are replaced.</p>
     */
    private String lineSeparator;

    /**
     * The maximal line length, in units of <em>code points</em> (not {@code char}).
     * Can be set to {@link Integer#MAX_VALUE} if there is no limit.
     *
     * @see #setMaximalLineLength(int)
     */
    private int maximalLineLength;

    /**
     * The length of the current line, in units of <em>code points</em> (not {@code char}).
     * It may be greater than the length of {@link #buffer} because the later contains only
     * the last word.
     */
    private int codePointCount;

    /**
     * The tabulation width, in number of code points.
     *
     * @see #setTabulationWidth(int)
     */
    private short tabulationWidth = 8;

    /**
     * {@code true} if this formatter shall expands tabulations into spaces.
     *
     * @see #setTabulationExpanded(boolean)
     */
    private boolean isTabulationExpanded;

    /**
     * {@code true} if all occurrences of EOL sequences shall be replaced by
     * the {@link #lineSeparator}, or {@code false} for keeping EOL unchanged.
     */
    private boolean isEndOfLineReplaced;

    /**
     * {@code true} if the next character needs to be skipped if equals to {@code '\n'}.
     * This field is used in order to avoid writing two EOL in place of {@code "\r\n"}.
     */
    private boolean skipLF;

    /**
     * {@code true} if the next character will be at the beginning of a new line.
     * This flag is set to {@code true} only for "real" new lines, as a result of
     * line separator found in the input given to this formatter. The "generated"
     * new lines (resulting from line wrap) will invoke {@link #onLineBegin(boolean)}
     * directly without the help of this temporary variable.
     *
     * @see #transfer(int)
     */
    private boolean isNewLine = true;

    /**
     * {@code true} if an escape sequence is in progress. The escape sequence will stop
     * after the first non-digit character other than {@link X364#BRACKET}.
     */
    private boolean isEscapeSequence;

    /**
     * The buffer for the last word being written.
     * This buffer will also contain trailing whitespace characters. If whitespaces are followed
     * by at least one non-white character, then the whitespaces are written to the underlying
     * stream before the non-ignorable one. Otherwise if whitespaces are followed by a line
     * separator, then they are discarded.
     */
    private final StringBuilder buffer = new StringBuilder();

    /**
     * The number of Java characters (not Unicode code points) in {@link #buffer},
     * ignoring trailing whitespaces.
     */
    private int printableLength;

    /**
     * Constructs a default formatter. Callers should invoke at least one of the following methods
     * after construction in order to perform useful work:
     *
     * <ul>
     *   <li>{@link #setMaximalLineLength(int)}</li>
     *   <li>{@link #setTabulationExpanded(boolean)}</li>
     *   <li>{@link #setLineSeparator(String)}</li>
     * </ul>
     *
     * @param out The underlying stream or buffer to write to.
     */
    public LineAppender(final Appendable out) {
        super(out);
        maximalLineLength = Integer.MAX_VALUE;
    }

    /**
     * Constructs a formatter which will replaces line separators by the given string.
     *
     * @param out                   The underlying stream or buffer to write to.
     * @param lineSeparator         The line separator to send to {@code out}, or {@code null}
     *                              for forwarding the EOL sequences unchanged.
     * @param isTabulationExpanded  {@code true} for expanding tabulations into spaces,
     *                              or {@code false} for sending {@code '\t'} characters as-is.
     */
    public LineAppender(final Appendable out, final String lineSeparator, final boolean isTabulationExpanded) {
        super(out);
        maximalLineLength = Integer.MAX_VALUE;
        this.lineSeparator        = lineSeparator;
        this.isEndOfLineReplaced  = (lineSeparator != null);
        this.isTabulationExpanded = isTabulationExpanded;
    }

    /**
     * Constructs a formatter which will wrap the lines at a given maximal length.
     *
     * @param out                   The underlying stream or buffer to write to.
     * @param maximalLineLength     The maximal number of Unicode characters per line,
     *                              or {@link Integer#MAX_VALUE} if there is no limit.
     * @param isTabulationExpanded  {@code true} for expanding tabulations into spaces,
     *                              or {@code false} for forwarding {@code '\t'} characters as-is.
     */
    public LineAppender(final Appendable out, final int maximalLineLength, final boolean isTabulationExpanded) {
        super(out);
        ArgumentChecks.ensureStrictlyPositive("maximalLineLength", maximalLineLength);
        this.maximalLineLength    = maximalLineLength;
        this.isTabulationExpanded = isTabulationExpanded;
    }

    /**
     * Returns the maximal line length, in unit of Unicode characters (code point count).
     * The default value is no limit.
     *
     * @return The current maximal number of Unicode characters per line,
     *         or {@link Integer#MAX_VALUE} if there is no limit.
     */
    public int getMaximalLineLength() {
        return maximalLineLength;
    }

    /**
     * Sets the maximal line length, in units of Unicode characters (code point count).
     *
     * @param length The new maximal number of Unicode characters per line,
     *               or {@link Integer#MAX_VALUE} if there is no limit.
     */
    public void setMaximalLineLength(final int length) {
        ArgumentChecks.ensureStrictlyPositive("length", length);
        maximalLineLength = length;
    }

    /**
     * Returns the current tabulation width, in unit of Unicode characters (code point count).
     * The default value is 8.
     *
     * @return The current tabulation width in number of Unicode characters.
     */
    public int getTabulationWidth() {
        return tabulationWidth;
    }

    /**
     * Sets the tabulation width, in unit of Unicode characters (code point count).
     *
     * @param  width The new tabulation width. Must be greater than 0.
     * @throws IllegalArgumentException if {@code tabWidth} is not greater than 0
     *         or is unreasonably high.
     */
    public void setTabulationWidth(final int width) {
        ArgumentChecks.ensureStrictlyPositive("width", width);
        ArgumentChecks.ensureBetween("width", 1, Integer.MAX_VALUE, width);
        tabulationWidth = (short) width;
    }

    /**
     * Returns {@code true} if this formatter expands tabulations into spaces.
     * The default value is {@code false}, which means that {@code '\t'} characters
     * are sent to the underlying appendable <i>as-is</i>.
     *
     * @return {@code true} if this formatter expands tabulations into spaces,
     *         or {@code false} if {@code '\t'} characters are forwarded <i>as-is</i>.
     */
    public boolean isTabulationExpanded() {
        return isTabulationExpanded;
    }

    /**
     * Sets whether this class formatter expands tabulations into spaces.
     *
     * @param expanded {@code true} if this class shall expands tabulations into spaces,
     *                 or {@code false} for forwarding {@code '\t'} characters as-is.
     */
    public void setTabulationExpanded(final boolean expanded) {
        isTabulationExpanded = expanded;
    }

    /**
     * Returns the line separator to be sent to the underlying appendable,
     * or {@code null} if EOL sequences are forwarded unchanged.
     *
     * @return The current line separator, or {@code null} if EOL are forwarded <i>as-is</i>.
     */
    public String getLineSeparator() {
        return isEndOfLineReplaced ? lineSeparator : null;
    }

    /**
     * Changes the line separator to be sent to the underlying appendable.
     * This is the string to insert in place of every occurrences of {@code "\r"}, {@code "\n"},
     * {@code "\r\n"} or other {@linkplain Characters#isLineOrParagraphSeparator(int) line separators}.
     * If {@code null} (the default), then the line separators given to the {@code append}
     * methods are forwarded unchanged.
     *
     * @param  lineSeparator The new line separator, or {@code null} for forwarding EOL <i>as-is</i>.
     *
     * @see System#lineSeparator()
     * @see Characters#isLineOrParagraphSeparator(int)
     */
    public void setLineSeparator(final String lineSeparator) {
        this.lineSeparator  = lineSeparator;
        isEndOfLineReplaced = (lineSeparator != null);
    }

    /**
     * Writes a line separator to {@link #out}. This method is invoked for new line separators
     * generated by this class, not for the line separators found in the texts supplied by the
     * user, unless {@link #isEndOfLineReplaced} is {@code true}.
     *
     * The {@link #append(CharSequence,int,int)} method tries to detect the line separator used
     * in the text, but if no line separator has been found we have to use some fallback.
     */
    private void writeLineSeparator() throws IOException {
        if (lineSeparator == null) {
            lineSeparator = JDK7.lineSeparator();
        }
        out.append(lineSeparator);
    }

    /**
     * Writes pending non-white characters, discards trailing whitespaces, and resets column
     * position to zero. This method does <strong>not</strong> write the line separator and
     * does not modify the status of the {@link #skipLF} flag; those tasks are caller's
     * responsibility.
     */
    private void endOfLine() throws IOException {
        buffer.setLength(printableLength); // Reduce the amount of work for StringBuilder.deleteCharAt(int).
        deleteSoftHyphen(printableLength - 1);
        transfer(printableLength);
        printableLength  = 0;
        codePointCount   = 0;
        isEscapeSequence = false; // Handle line-breaks as "end of escape sequence".
        isNewLine        = true;
    }

    /**
     * Removes the soft hyphen characters from the given buffer. This is invoked
     * when the buffer is about to be written without being split on two lines.
     *
     * @param i Index after the last character to check. This is either {@link printableLength}
     *          for checking all characters, or {@code printableLength-1} for preserving the last
     *          soft hyphen on the line (while removing all others).
     */
    private void deleteSoftHyphen(int i) {
        while (--i >= 0) {
            if (buffer.charAt(i) == Characters.SOFT_HYPHEN) {
                buffer.deleteCharAt(i);
                printableLength--;
            }
        }
    }

    /**
     * Writes the given amount of characters from the {@linkplain #buffer},
     * then removes those characters from the buffer. This method does not
     * adjust {@link #printableLength}; it is caller responsibility to do so.
     */
    private void transfer(final int length) throws IOException {
        if (isNewLine) {
            isNewLine = false;
            onLineBegin(false);
        }
        out.append(buffer, 0, length);
        buffer.delete(0, length);
    }

    /**
     * Writes the specified code point.
     *
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("fallthrough")
    private void write(final int c) throws IOException {
        final StringBuilder buffer = this.buffer;
        /*
         * If the character to write is a EOL sequence, then:
         *
         *   1) Trim trailing whitespaces in the buffer.
         *   2) Remove unused soft-hyphens (otherwise some consoles display them).
         *   3) Flush the buffer to the underlying appendable.
         *   4) Write the line separator.
         */
        if (Characters.isLineOrParagraphSeparator(c)) {
            final boolean skip;
            switch (c) {
                case '\r': skip = false;  skipLF = true;  break;
                case '\n': skip = skipLF; skipLF = false; break;
                default:   skip = false;  skipLF = false; break;
            }
            if (!skip) {
                endOfLine();
            }
            if (!isEndOfLineReplaced) {
                appendCodePoint(c); // Forward EOL sequences "as-is".
            } else if (!skip) {
                writeLineSeparator(); // Replace EOL sequences by the unique line separator.
            }
            return;
        }
        skipLF = false;
        /*
         * If the character to write is a whitespace, then write any pending characters from
         * the buffer to the underlying appendable since we know that those characters didn't
         * exceeded the line length limit.
         *
         * We use Character.isWhitespace(…) instead of Character.isSpaceChar(…) because
         * the former returns 'true' tabulations (which we want), and returns 'false'
         * for non-breaking spaces (which we also want).
         */
        if (Character.isWhitespace(c)) {
            if (printableLength != 0) {
                deleteSoftHyphen(printableLength);
                transfer(printableLength);
                printableLength = 0;
            }
            if (c != '\t') {
                codePointCount++;
            } else {
                final int width = tabulationWidth - (codePointCount % tabulationWidth);
                codePointCount += width;
                if (isTabulationExpanded) {
                    buffer.append(CharSequences.spaces(width));
                    return;
                }
            }
            buffer.appendCodePoint(c);
            return;
        }
        buffer.appendCodePoint(c);
        printableLength = buffer.length();
        /*
         * Special handling of ANSI X3.64 escape sequences. Since they are not visible
         * characters (they are used for controlling the colors), do not count them in
         * 'codePointCount' (but still count them as "printable" characters, since we
         * don't want to trim them). The sequence pattern is "CSI <digits> <command>"
         * where <command> is a single letter.
         */
        if (c == X364.ESCAPE) {
            isEscapeSequence = true;
            return;
        } else if (isEscapeSequence) {
            final char previous = buffer.charAt(printableLength - 2);
            if (previous != X364.ESCAPE) {
                isEscapeSequence = (c >= '0' && c <= '9');
                return; // The letter after the digits will be the last character to skip.
            } else if (c == X364.BRACKET) {
                return; // Found the second part of the Control Sequence Introducer (CSI).
            }
            // [ESC] was not followed by '['. Proceed as a normal character.
            isEscapeSequence = false;
        }
        /*
         * The remaining of this method is executed only if we exceeded the maximal line length.
         * First, search for the hyphen character, if any. If we find one and if it is preceeded
         * by a letter, split there. The "letter before" condition is a way to avoid to split at
         * the minus sign of negative numbers like "-99", assuming that the minus sign is preceeded
         * by a space. We can not look at the character after since we may not know it yet.
         */
        if (++codePointCount > maximalLineLength) {
searchHyp:  for (int i=buffer.length(); i>0;) {
                final int b = buffer.codePointBefore(i);
                final int n = Character.charCount(b);
                switch (b) {
                    case '-': {
                        if (i>=n && !Character.isLetter(buffer.codePointBefore(i-n))) {
                            break; // Continue searching previous characters.
                        }
                        // fall through
                    }
                    case Characters.HYPHEN:
                    case Characters.SOFT_HYPHEN: {
                        transfer(i);
                        break searchHyp;
                    }
                }
                i -= n;
            }
            /*
             * At this point, all the remaining content of the buffer must move on the next line.
             * Skip the leading whitespaces on the new line.
             */
            writeLineSeparator();
            final int length = buffer.length();
            for (int i=0; i<length;) {
                final int s = buffer.codePointAt(i);
                if (!Character.isWhitespace(s)) {
                    buffer.delete(0, i);
                    break;
                }
                i += Character.charCount(s);
            }
            printableLength = buffer.length();
            codePointCount  = buffer.codePointCount(0, printableLength);
            onLineBegin(true);
        }
    }

    /**
     * Writes a single character.
     *
     * @param  c The character to append.
     * @return A reference to this {@code Appendable}.
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
     * @return A reference to this {@code Appendable}.
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
     * Resets the {@code LineAppender} internal state as if a new line was beginning.
     * Trailing whitespaces not yet sent to the {@linkplain #out underlying appendable}
     * are discarded, and the column position (for tabulation expansion calculation) is
     * reset to 0. This method does not write any line separator.
     *
     * @throws IOException If an error occurred while sending the trailing non-white
     *         characters to the underlying stream.
     */
    public void clear() throws IOException {
        endOfLine();
        skipLF = false;
    }

    /**
     * Sends all pending characters to the underlying appendable, including trailing whitespaces.
     * Note that this method should preferably be invoked at the end of a word, sentence or line,
     * since invoking this method may prevent {@code LineAppender} to properly wrap the current
     * line if the current position is in the middle of a word.
     *
     * <p>Invoking this method also flushes the underlying stream, if {@linkplain Flushable flushable}.
     * A cheaper way to send pending characters is to make sure that the last character is a
     * {@linkplain Characters#isLineOrParagraphSeparator(int) line or paragraph terminator},
     * or to invoke {@link #clear()}.</p>
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        out.append(buffer);
        buffer.setLength(0);
        printableLength = 0;
        IO.flush(out);
    }

    /**
     * Invoked when a new line is beginning. The default implementation does nothing,
     * but subclasses can override this method for example in order to insert a margin
     * on the left side before each line.
     *
     * <p>If an implementation wishes to write characters, it shall do so by writing
     * directly to {@link #out}, <strong>not</strong> by invoking the {@code append}
     * methods of this class.</p>
     *
     * @param  isContinuation {@code true} if the new line is the continuation of the previous
     *         line after a "line wrap", or {@code false} if a line or paragraph separator has
     *         been explicitly sent to this formatter.
     * @throws IOException if an error occurred while writing to {@link #out}.
     */
    protected void onLineBegin(boolean isContinuation) throws IOException {
    }
}
