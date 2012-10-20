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

import static org.apache.sis.util.Characters.LINE_SEPARATOR;


/**
 * An {@link Appendable} that expand tabs ({@code '\t'}) into spaces.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 */
@Decorator(Appendable.class)
public class ExpandedTabFormatter extends FilteredAppendable {
    /**
     * Tab width (in number of spaces).
     */
    private int tabWidth;

    /**
     * Current column position. Columns are numbered from 0.
     */
    private int column;

    /**
     * Constructs a formatter which replaces tab characters ({@code '\t'})
     * by spaces. Tab widths default to 8 characters.
     *
     * @param out A writer object to provide the underlying stream.
     */
    public ExpandedTabFormatter(final Appendable out) {
        super(out);
        tabWidth = 8;
    }

    /**
     * Constructs a formatter which replaces tab characters ({@code '\t'})
     * by spaces, using the specified tab width.
     *
     * @param  out The underlying stream or buffer.
     * @param  tabWidth The tab width. Must be greater than 0.
     * @throws IllegalArgumentException if {@code tabWidth} is not greater than 0.
     */
    public ExpandedTabFormatter(final Appendable out, final int tabWidth) {
        super(out);
        ArgumentChecks.ensureStrictlyPositive("tabWidth", tabWidth);
        this.tabWidth = tabWidth;
    }

    /**
     * Returns the current tabulation width.
     *
     * @return The tabulation width.
     */
    public int getTabWidth() {
        return tabWidth;
    }

    /**
     * Sets the tabulation width.
     *
     * @param  tabWidth The tab width. Must be greater than 0.
     * @throws IllegalArgumentException if {@code tabWidth} is not greater than 0.
     */
    public void setTabWidth(final int tabWidth) {
        ArgumentChecks.ensureStrictlyPositive("tabWidth", tabWidth);
        this.tabWidth = tabWidth;
    }

    /**
     * Writes spaces for a tabulation character.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void expand() throws IOException {
        final int width = tabWidth - (column % tabWidth);
        out.append(CharSequences.spaces(width));
        column += width;
    }

    /**
     * Writes a single character.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public Appendable append(final char c) throws IOException {
        switch (c) {
            case LINE_SEPARATOR:
            case '\r': // fall through
            case '\n': column=0; break;
            case '\t': expand(); return this;
            default  : column++; break;
        }
        out.append(c);
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
        for (int i=start; i<end; i++) {
            final char c = sequence.charAt(i);
            switch (c) {
                case LINE_SEPARATOR:
                case '\r': // fall through
                case '\n': {
                    column = 0;
                    break;
                }
                case '\t': {
                    out.append(sequence, start, i);
                    start = i+1;
                    expand();
                    break;
                }
                default: {
                    column++;
                    break;
                }
            }
        }
        out.append(sequence, start, end);
        return this;
    }
}
