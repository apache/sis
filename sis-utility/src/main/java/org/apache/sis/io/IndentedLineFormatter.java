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
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;


/**
 * An {@link Appendable} that put some spaces in front of every lines.
 * The indentation is initially set to 0 spaces. Users must invoke {@link #setIndentation(int)}
 * or {@link #setMargin(String)} in order to set a different value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Decorator(Appendable.class)
public class IndentedLineFormatter extends FilteredAppendable {
    /**
     * A string with a length equal to the indentation.
     */
    private String margin = "";

    /**
     * {@code true} if we are about to write a new line.
     */
    private boolean newLine = true;

    /**
     * {@code true} if we are waiting for a {@code '\n'} character.
     */
    private boolean waitLF;

    /**
     * Constructs a formatter which will add spaces in front of every lines.
     * The {@link #setIndentation(int)} or {@link #setMargin(String)} methods
     * must be invoked after this constructor in order to specify the amount
     * of spaces to add.
     *
     * @param out The underlying stream or buffer to write to.
     */
    public IndentedLineFormatter(final Appendable out) {
        super(out);
    }

    /**
     * Constructs a formatter which will add the given amount of spaces in front of every lines.
     *
     * @param out   The underlying stream or buffer to write to.
     * @param width The number of space to insert at the beginning of every line.
     */
    public IndentedLineFormatter(final Appendable out, final int width) {
        super(out);
        ArgumentChecks.ensurePositive("width", width);
        margin = CharSequences.spaces(width);
    }

    /**
     * Returns the current indentation. This is either the value given to the last call
     * to {@link #setIndentation(int)} method, or the length of the string given to the
     * {@link #setMargin(String)} method.
     *
     * @return The current indentation.
     */
    public int getIdentation() {
        return margin.length();
    }

    /**
     * Sets the indentation to the specified value. This method will
     * {@linkplain #setMargin(String) defines a margin} as the given number of white spaces.
     *
     * @param width The number of space to insert at the beginning of every line.
     */
    public void setIndentation(final int width) {
        ArgumentChecks.ensurePositive("width", width);
        margin = CharSequences.spaces(width);
    }

    /**
     * Returns the margin which is written at the beginning of every line. The default
     * value is an empty string. This value can be modified either explicitely by a call to
     * {@link #setMargin(String)}, or implicitly by a call to {@link #setIndentation(int)}.
     *
     * @return The string which is inserted at the beginning of every lines.
     */
    public String getMargin() {
        return margin;
    }

    /**
     * Sets the margin to be written at the beginning of every line.
     *
     * @param margin The string to be inserted at the beginning of every lines.
     */
    public void setMargin(final String margin) {
        ArgumentChecks.ensureNonNull("margin", margin);
        this.margin = margin;
    }

    /**
     * Invoked when a new line is beginning. The default implementation writes the
     * margin specified by the last call to {@link #setMargin(String)}.
     *
     * @throws IOException If an I/O error occurs
     */
    protected void beginNewLine() throws IOException {
        out.append(margin);
    }

    /**
     * Writes the specified character.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void write(final char c) throws IOException {
        if (newLine && (c != '\n' || !waitLF)) {
            beginNewLine();
        }
        out.append(c);
        waitLF  = (c == '\r');
        newLine = (waitLF || c == '\n');
    }

    /**
     * Writes a single character.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public Appendable append(final char c) throws IOException {
        write(c);
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
part:   while (start != end) {
            if (newLine) {
                write(sequence.charAt(start++));
            } else {
                final int previous = start;
                do {
                    final char c = sequence.charAt(start);
                    if (c == '\r' || c == '\n') {
                        out.append(sequence, previous, start);
                        write(c);
                        start++;
                        continue part;
                    }
                } while (++start != end);
                out.append(sequence, previous, start);
                break;
            }
        }
        return this;
    }
}
