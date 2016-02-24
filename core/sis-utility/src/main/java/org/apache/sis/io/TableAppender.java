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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.Flushable;
import java.io.IOException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.X364;

import static org.apache.sis.util.Characters.isLineOrParagraphSeparator;

// Related to JK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * An {@link Appendable} which formats the text as a table suitable for displaying in devices using
 * a monospaced font. Columns are separated by tabulations ({@code '\t'}) and rows are separated by
 * {@linkplain org.apache.sis.util.Characters#isLineOrParagraphSeparator(int) line or paragraph separators}.
 * The content of every table cells are stored in memory until the {@link #flush()} method is invoked.
 * When invoked, {@code flush()} copies the cell contents to the {@linkplain #out underlying stream
 * or buffer} while replacing tabulations by some amount of spaces and drawing borders.
 * The exact number of spaces is computed from the cell widths.
 *
 * <p>For example, the following code:</p>
 *
 * {@preformat java
 *     StringBuilder  buffer = new StringBuilder();
 *     TableAppender table  = new TableAppender(buffer);
 *     table.nextLine('═');
 *     table.append("English\tFrench\tr.e.d.\n");
 *     table.nextLine('-');
 *     table.append("Mercury\tMercure\t0.382\n")
 *          .append("Venus\tVénus\t0.949\n")
 *          .append("Earth\tTerre\t1.00\n")
 *          .append("Mars\tMars\t0.532\n");
 *     table.nextLine('═');
 *     table.flush();
 * }
 *
 * produces the following output:
 *
 * {@preformat text
 *   ╔═════════╤═════════╤════════╗
 *   ║ English │ French  │ r.e.d. ║
 *   ╟─────────┼─────────┼────────╢
 *   ║ Mercury │ Mercure │ 0.382  ║
 *   ║ Venus   │ Vénus   │ 0.949  ║
 *   ║ Earth   │ Terre   │ 1.00   ║
 *   ║ Mars    │ Mars    │ 0.532  ║
 *   ╚═════════╧═════════╧════════╝
 * }
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.util.collection.TreeTableFormat
 */
public class TableAppender extends Appender implements Flushable {
    /**
     * A possible value for cell alignment. This specifies that the text is aligned
     * to the left indent and extra whitespace should be placed on the right.
     */
    public static final byte ALIGN_LEFT = -1;

    /**
     * A possible value for cell alignment. This specifies that the text is aligned
     * to the center and extra whitespace should be placed equally on the left and right.
     */
    public static final byte ALIGN_CENTER = 0;

    /**
     * A possible value for cell alignment. This specifies that the text is aligned
     * to the right indent and extra whitespace should be placed on the left.
     */
    public static final byte ALIGN_RIGHT = +1;

    /**
     * Drawing-box characters. The last two characters
     * are horizontal and vertical line respectively.
     */
    private static final char[][] BOX = new char[][] {
        {// [0000]: single horizontal, single vertical
            '┌','┬','┐',
            '├','┼','┤',
            '└','┴','┘',
            '─','│'
        },
        {// [0001]: single horizontal, double vertical
            '╓','╥','╖',
            '╟','╫','╢',
            '╙','╨','╜',
            '─','║'
        },
        {// [0010]: double horizontal, single vertical
            '╒','╤','╕',
            '╞','╪','╡',
            '╘','╧','╛',
            '═','│'
        },
        {// [0011]: double horizontal, double vertical
            '╔','╦','╗',
            '╠','╬','╣',
            '╚','╩','╝',
            '═','║'
        }
    };

    /**
     * The character for empty spaces to insert between columns.
     */
    private static final char SPACE = ' ';

    /**
     * Temporary string buffer. This buffer contains only one cell content.
     */
    private final StringBuilder buffer = new StringBuilder(64);

    /**
     * List of {@link Cell} objects, from left to right and top to bottom.
     * By convention, a {@code null} value or a {@link Cell} object with
     * <code>{@linkplain Cell#text} == null</code> means that we need to move
     * to the next line.
     */
    private final List<Cell> cells = new ArrayList<Cell>();

    /**
     * Alignment for current and next cells.
     *
     * @see #getCellAlignment()
     * @see #setCellAlignment(byte)
     */
    private byte alignment = ALIGN_LEFT;

    /**
     * Column position of the cell currently being written. The field
     * is incremented every time {@link #nextColumn()} is invoked.
     */
    private int currentColumn;

    /**
     * Line position of the cell currently being written. The field
     * is incremented every time {@link #nextLine()} is invoked.
     */
    private int currentRow;

    /**
     * Maximum width for each columns. This array length must
     * be equal to the number of columns in this table.
     */
    private int[] maximalColumnWidths = ArraysExt.EMPTY_INT;

    /**
     * The line separator. We will use the first line separator found in the
     * text to provided by the user, or the system default if none.
     */
    private String lineSeparator;

    /**
     * The column separator.
     */
    private final String columnSeparator;

    /**
     * The left table border.
     */
    private final String leftBorder;

    /**
     * The right table border.
     */
    private final String rightBorder;

    /**
     * Tells if cells can span more than one line. If {@code true}, then EOL characters likes
     * {@code '\n'} move to the next line <em>inside</em> the current cell. If {@code false},
     * then EOL characters move to the next table row. Default value is {@code false}.
     */
    private boolean multiLinesCells;

    /**
     * {@code true} if the next character needs to be skipped if equals to {@code '\n'}.
     */
    private boolean skipLF;

    /**
     * Sets to {@code true} at construction time if {@link #out} has been created by the
     * constructor rather than supplied by the user.
     */
    private boolean ownOut;

    /**
     * Creates a new table formatter writing in an internal buffer with a default column separator.
     * The default is a vertical double line for the left and right table borders, and a single
     * line between the columns.
     */
    public TableAppender() {
        this(new StringBuilder(256));
        ownOut = true;
    }

    /**
     * Creates a new table formatter writing in an internal buffer with the specified column separator.
     *
     * @param separator String to write between columns.
     */
    public TableAppender(final String separator) {
        this(new StringBuilder(256), separator);
        ownOut = true;
    }

    /**
     * Creates a new table formatter writing in the given output with a default column separator.
     * The default is a vertical double line for the left and right table borders, and a single
     * line between the columns.
     *
     * @param out The underlying stream or buffer to write to.
     */
    public TableAppender(final Appendable out) {
        super(out);
        leftBorder      =  "║ ";
        rightBorder     = " ║" ;
        columnSeparator = " │ ";
    }

    /**
     * Creates a new table formatter writing in the given output with the specified column separator.
     *
     * @param out The underlying stream or buffer to write to.
     * @param separator String to write between columns.
     */
    public TableAppender(final Appendable out, final String separator) {
        super(out);
        /*
         * Following methods use Character.isWhitespace(…) instead of Character.isSpaceChar(…).
         * This has the effect of removing some ISO control characters (line feeds, tabulation,
         * etc.) from the border. If this policy is changed, search for other occurrences of
         * 'isWhitespace' in this class for ensuring consistency. Note however that the same
         * policy is not necessarily applied everywhere.
         */
        final int length = separator.length();
        leftBorder      = separator.substring(   CharSequences.skipLeadingWhitespaces (separator, 0, length));
        rightBorder     = separator.substring(0, CharSequences.skipTrailingWhitespaces(separator, 0, length));
        columnSeparator = separator;
    }

    /**
     * Writes a border or a corner to the underlying stream or buffer.
     *
     * @param  horizontalBorder -1 for left border, +1 for right border,  0 for center.
     * @param  verticalBorder   -1 for top  border, +1 for bottom border, 0 for center.
     * @param  horizontalChar   Character to use for horizontal line.
     * @throws IOException      If the writing operation failed.
     */
    private void writeBorder(final int  horizontalBorder,
                             final int  verticalBorder,
                             final char horizontalChar) throws IOException
    {
        /*
         * Get the set of characters to use for the horizontal line.
         */
        int boxCount = 0;
        final char[][] box = new char[BOX.length][];
        for (final char[] row : BOX) {
            if (row[9] == horizontalChar) {
                box[boxCount++] = row;
            }
        }
        /*
         * Get a string which contains the vertical lines to draw
         * on the left, on the right or in the center of the table.
         */
        final String border;
        switch (horizontalBorder) {
            case -1: border = leftBorder;  break;
            case +1: border = rightBorder; break;
            case  0: border = columnSeparator;   break;
            default: throw new AssertionError(horizontalBorder);
        }
        assert (verticalBorder >= -1) && (verticalBorder <= +1) : verticalBorder;
        /*
         * Remplaces spaces by the horizontal lines, and vertical lines by an intersection.
         * Use Character.isWhitespace(…) instead of Character.isSpaceChar(…) for consistency
         * with the policy used in the constructor, since we work on the same object (namely
         * the border strings).
         */
        final int index = (horizontalBorder+1) + (verticalBorder+1)*3;
        final int borderLength = border.length();
        for (int i=0; i<borderLength;) {
            int c = border.codePointAt(i);
            i += Character.charCount(c);
            if (Character.isWhitespace(c)) {
                c = horizontalChar;
            } else {
                for (int j=0; j<boxCount; j++) {
                    if (box[j][10] == c) {
                        c = box[j][index];
                        break;
                    }
                }
            }
            appendCodePoint(c);
        }
    }

    /**
     * Returns {@code true} if EOL characters are used for line feeds inside current cells.
     *
     * @return {@code true} if EOL characters are to be write inside the cell.
     */
    public boolean isMultiLinesCells() {
        return multiLinesCells;
    }

    /**
     * Sets the desired behavior for EOL and tabulations characters.
     *
     * <ul>
     *   <li>If {@code true}, then tabulations,
     *       {@linkplain org.apache.sis.util.Characters#isLineOrParagraphSeparator(int)
     *       line and paragraph separator} characters are copied into the current cell.
     *       Subsequent writing operations will continue inside the same cell.</li>
     *   <li>If {@code false}, then tabulations move to next column and EOL move
     *       to the first cell of next row (i.e. tabulation and EOL are equivalent to
     *       {@link #nextColumn()} and {@link #nextLine()} calls respectively).</li>
     * </ul>
     *
     * The default value is {@code false}.
     *
     * @param multiLines {@code true} true if EOL are used for line feeds inside
     *        current cells, or {@code false} if EOL move to the next row.
     */
    public void setMultiLinesCells(final boolean multiLines) {
        multiLinesCells = multiLines;
    }

    /**
     * Returns the alignment of the text inside the current cell.
     * The default value is {@link #ALIGN_LEFT}.
     *
     * @return Current cell alignment as one of the {@link #ALIGN_LEFT},
     *         {@link #ALIGN_RIGHT} or {@link #ALIGN_CENTER} constants.
     */
    public byte getCellAlignment() {
        return alignment;
    }

    /**
     * Sets the alignment of the text inside the current cell. The alignments of any cell
     * written prior this method call are left unchanged. The new alignment will apply to
     * the next cells too until this {@code setCellAlignment(…)} method is invoked again
     * with a different value.
     *
     * <p>If this method is never invoked, then the default alignment is {@link #ALIGN_LEFT}.</p>
     *
     * @param alignment The new cell alignment as one of the {@link #ALIGN_LEFT},
     *        {@link #ALIGN_RIGHT} or {@link #ALIGN_CENTER} constants.
     */
    public void setCellAlignment(final byte alignment) {
        if (alignment < ALIGN_LEFT || alignment > ALIGN_RIGHT) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "alignment", alignment));
        }
        this.alignment = alignment;
    }

    /**
     * Returns the number of rows in this table. This count is reset to 0 by {@link #flush()}.
     *
     * @return The number of rows in this table.
     */
    public int getRowCount() {
        int count = currentRow;
        if (currentColumn != 0) {
            count++; // Some writting has begun in the current row.
        }
        return count;
    }

    /**
     * Returns the number of columns in this table.
     *
     * @return The number of columns in this table.
     */
    public int getColumnCount() {
        return maximalColumnWidths.length;
    }

    /**
     * Writes a single character. If {@link #isMultiLinesCells()} is {@code false}
     * (which is the default), then:
     *
     * <ul>
     *   <li>Tabulations ({@code '\t'}) are replaced by calls to {@link #nextColumn()}.</li>
     *   <li>{@linkplain org.apache.sis.util.Characters#isLineOrParagraphSeparator(int)
     *       line or paragraph separators} are replaced by calls to {@link #nextLine()}.</li>
     * </ul>
     *
     * @param  c Character to write.
     * @return {@code this}.
     */
    @Override
    public TableAppender append(final char c) {
        final int cp = toCodePoint(c);
        if (!multiLinesCells) {
            if (cp == '\t') {
                nextColumn();
                skipLF = false;
                return this;
            }
            if (isLineOrParagraphSeparator(cp)) {
                if (cp == '\n') {
                    if (!skipLF) {
                        nextLine();
                    }
                    skipLF = false;
                } else {
                    nextLine();
                    skipLF = true;
                }
                return this;
            }
        }
        buffer.appendCodePoint(cp);
        skipLF = false;
        return this;
    }

    /**
     * Appends the specified character sequence.
     *
     * @param  sequence The character sequence to append, or {@code null}.
     * @return A reference to this {@code Appendable}.
     */
    @Override
    public TableAppender append(CharSequence sequence) {
        if (sequence == null) {
            sequence = "null";
        }
        return append(sequence, 0, sequence.length());
    }

    /**
     * Writes a portion of a character sequence. Tabulations and line separators are
     * interpreted as by {@link #append(char)}.
     *
     * @param  sequence The character sequence to be written.
     * @param  start    Index from which to start reading characters.
     * @param  end      Index of the character following the last character to read.
     * @return {@code this}.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public TableAppender append(final CharSequence sequence, int start, int end) {
        ArgumentChecks.ensureValidIndexRange(sequence.length(), start, end);
        if (lineSeparator == null) {
            lineSeparator = lineSeparator(sequence, start, end);
        }
        try {
            start = appendSurrogate(sequence, start, end);
        } catch (IOException e) {
            // Should never happen, because appendSurrogate(…) delegates to append(char)
            // which is overriden without 'throws IOException' clause in this class.
            throw new AssertionError(e);
        }
        if (start != end) {
            if (skipLF && sequence.charAt(start) == '\n') {
                start++;
            }
            if (!multiLinesCells) {
                int cp = 0;
                int upper = start;
                while (upper != end) {
                    cp = toCodePoint(sequence.charAt(upper++));
                    if (cp >= 0 && (cp == '\t' || isLineOrParagraphSeparator(cp))) {
                        buffer.append(sequence, start, upper - Character.charCount(cp));
                        switch (cp) {
                            case '\r': if (upper < end && sequence.charAt(upper) == '\n') upper++;
                            default:   nextLine();   break; // Applies also to the above '\r' case.
                            case '\t': nextColumn(); break;
                        }
                        start = upper;
                    }
                }
                skipLF = (cp == '\r'); // Check the last character.
            } else {
                /*
                 * The call to 'toCodePoint' is for forcing the initialization of
                 * super.highSurrogate field value. Even if we fall in the middle
                 * of a surrogate pair, it should not hurt because in this context,
                 * toCodePoint should either returns -1 or its argument unchanged.
                 */
                assert !isHighSurrogate();
                skipLF = (toCodePoint(sequence.charAt(end - 1)) == '\r');
            }
            if (isHighSurrogate()) {
                end--;
            }
            buffer.append(sequence, start, end);
        }
        return this;
    }

    /**
     * Writes an horizontal separator.
     */
    public void appendHorizontalSeparator() {
        if (currentColumn != 0 || buffer.length() != 0) {
            nextLine();
        }
        nextLine('─');
    }

    /**
     * Moves one column to the right.
     * The subsequent writing operations will occur in a new cell on the same row.
     */
    public void nextColumn() {
        nextColumn(SPACE);
    }

    /**
     * Moves one column to the right, filling remaining space with the given character.
     * The subsequent writing operations will occur in a new cell on the same row.
     *
     * <p>Calling {@code nextColumn('*')} from the first character
     * in a cell is a convenient way to put a pad value in this cell.</p>
     *
     * @param fill Character filling the cell (default to whitespace).
     */
    public void nextColumn(final char fill) {
        final String cellText = buffer.toString();
        cells.add(new Cell(cellText, alignment, fill));
        if (currentColumn >= maximalColumnWidths.length) {
            maximalColumnWidths = Arrays.copyOf(maximalColumnWidths, currentColumn+1);
        }
        int width     = 0;
        int lineStart = 0;
        final int length = cellText.length();
        while (lineStart < length) {
            final int nextLine = CharSequences.indexOfLineStart(cellText, 1, lineStart);
            for (int i=nextLine; --i >= lineStart;) {
                if (!Character.isISOControl(cellText.charAt(i))) {
                    final int lg = X364.lengthOfPlain(cellText, lineStart, i+1);
                    if (lg > width) {
                        width = lg;
                    }
                }
            }
            lineStart = nextLine;
        }
        if (width > maximalColumnWidths[currentColumn]) {
            maximalColumnWidths[currentColumn] = width;
        }
        currentColumn++;
        buffer.setLength(0);
    }

    /**
     * Moves to the first column on the next row.
     * The subsequent writing operations will occur on a new row.
     */
    public void nextLine() {
        nextLine(SPACE);
    }

    /**
     * Moves to the first column on the next row, filling every remaining cell in the current
     * row with the specified character. The subsequent writing operations will occur on a new
     * row.
     *
     * <p>Calling {@code nextLine('-')} or {@code nextLine('═')} from the first column of a row
     * is a convenient way to fill this row with a line separator.</p>
     *
     * @param fill Character filling the rest of the line (default to whitespace).
     *             This character may be use as a row separator.
     */
    public void nextLine(final char fill) {
        if (buffer.length() != 0) {
            nextColumn(fill);
        }
        assert buffer.length() == 0;
        cells.add((fill != SPACE) ? new Cell(null, alignment, fill) : null);
        currentColumn = 0;
        currentRow++;
    }

    /**
     * Flushes the table content to the underlying stream or buffer. This method should not
     * be called before the table is completed (otherwise, columns may have the wrong width).
     *
     * @throws IOException if an output operation failed.
     */
    @Override
    public void flush() throws IOException {
        if (buffer.length() != 0) {
            nextLine();
            assert buffer.length() == 0;
        }
        if (!ownOut) {
            writeTable();
        }
        cells.clear();
        currentRow    = 0;
        currentColumn = 0;
        if (!(out instanceof TableAppender)) {
            /*
             * Flush only if this table is not included in an outer (bigger) table.
             * This is because flushing the outer table would break its formatting.
             */
            IO.flush(out);
        }
    }

    /**
     * Returns the content of this {@code TableAppender} as a string if possible.
     *
     * <ul>
     *   <li>If this {@code TableAppender} has been created without explicit {@link Appendable},
     *       then this method always returns the current table content formatted as a string.</li>
     *   <li>Otherwise, if {@link #out} implements {@link CharSequence} or is directly or
     *       indirectly a wrapper around a {@code CharSequence}, returns its {@code toString()}
     *       representation. The string will contain this table content only if {@link #flush()}
     *       has been invoked prior this {@code toString()} method.</li>
     *   <li>Otherwise returns the localized <cite>"Unavailable content"</cite> string.</li>
     * </ul>
     */
    @Override
    public String toString() {
        if (ownOut) {
            ((StringBuilder) out).setLength(0);
            try {
                writeTable();
            } catch (IOException e) {
                // Should never happen because we are writing in a StringBuilder.
                throw new AssertionError(e);
            }
        }
        return super.toString();
    }

    /**
     * Writes the table without clearing the {@code TableAppender} content.
     * Invoking this method many time would result in the same table being
     * repeated.
     */
    private void writeTable() throws IOException {
        final String columnSeparator = this.columnSeparator;
        final Cell[]     currentLine = new Cell[maximalColumnWidths.length];
        final int          cellCount = cells.size();
        for (int cellIndex=0; cellIndex<cellCount; cellIndex++) {
            /*
             * Copies in 'currentLine' every cells to write in the current table row.
             * Those elements exclude the last null sentinal value. The 'currentLine'
             * array initially contains no null element, but some element will be set
             * to null as we progress in the writing process.
             */
            Cell lineFill = null;
            int currentCount = 0;
            do {
                final Cell cell = cells.get(cellIndex);
                if (cell == null) {
                    break;
                }
                if (cell.text == null) {
                    lineFill = new Cell("", cell.alignment, cell.fill);
                    break;
                }
                currentLine[currentCount++] = cell;
            }
            while (++cellIndex < cellCount);
            Arrays.fill(currentLine, currentCount, currentLine.length, lineFill);
            /*
             * The loop below will be executed as long as we have some lines to write,
             * (i.e. as long as at least one element is non-null). If a cell contains
             * EOL characters, then we will need to format it as a multi-lines cell.
             */
            while (!isEmpty(currentLine)) {
                for (int j=0; j<currentLine.length; j++) {
                    final boolean isFirstColumn = (j   == 0);
                    final boolean isLastColumn  = (j+1 == currentLine.length);
                    final Cell    cell          = currentLine[j];
                    final int     cellWidth     = maximalColumnWidths[j];
                    final int     cellPadding   = isLastColumn && rightBorder.isEmpty() ? 0 : cellWidth;
                    if (cell == null) {
                        if (isFirstColumn) {
                            out.append(leftBorder);
                        }
                        repeat(out, SPACE, cellPadding);
                        out.append(isLastColumn ? rightBorder : columnSeparator);
                        continue;
                    }
                    String cellText = cell.text;
                    int textLength = cellText.length();
                    Cell remaining = null;
                    for (int endOfFirstLine=0; endOfFirstLine < textLength;) {
                        int c = cellText.codePointAt(endOfFirstLine);
                        int next = endOfFirstLine + Character.charCount(c);
                        if (isLineOrParagraphSeparator(c)) {
                            /*
                             * If a EOL character has been found, write only the first line in the cell.
                             * The 'currentLine[j]' element will be modified in order to contain only
                             * the remaining lines, which will be written in next loop iterations.
                             */
                            if (c == '\r' && (next < textLength) && cellText.charAt(next) == '\n') {
                                next++;
                            }
                            /*
                             * Verify if the remaining contains only white spaces. If so, those spaces
                             * will be ignored. But if there is at least one non-white character, then
                             * we will not skip those spaces. We use Character.isWhitespace(…) instead
                             * of Character.isSpaceChar(…) in order to consider non-breaking spaces as
                             * non-white characters. This is similar to the use of &nbsp; in HTML tables,
                             * which can be used for forcing the insertion of an otherwise ignored space.
                             */
                            for (int i=next; i<textLength; i += Character.charCount(c)) {
                                c = cellText.codePointAt(i);
                                if (!Character.isWhitespace(c)) {
                                    remaining = cell.substring(next);
                                    break;
                                }
                            }
                            cellText = cellText.substring(0, endOfFirstLine);
                            break;
                        }
                        endOfFirstLine = next;
                    }
                    currentLine[j] = remaining;
                    textLength = X364.lengthOfPlain(cellText, 0, cellText.length());
                    /*
                     * If the cell to write is actually a border, do a special processing
                     * in order to use the characters defined in the BOX static constant.
                     */
                    if (currentCount == 0) {
                        assert textLength == 0;
                        final int verticalBorder;
                        if      (cellIndex == 0)           verticalBorder = -1;
                        else if (cellIndex >= cellCount-1) verticalBorder = +1;
                        else                               verticalBorder =  0;
                        if (isFirstColumn) {
                            writeBorder(-1, verticalBorder, cell.fill);
                        }
                        repeat(out, cell.fill, Character.isWhitespace(cell.fill) ? cellPadding : cellWidth);
                        writeBorder(isLastColumn ? +1 : 0, verticalBorder, cell.fill);
                        continue;
                    }
                    /*
                     * If the cell is not a border, it is a normal cell.
                     * Write a single line of this cell content.
                     */
                    if (isFirstColumn) {
                        out.append(leftBorder);
                    }
                    final Appendable tabExpander = (cellText.indexOf('\t') >= 0)
                            ? new LineAppender(out, Integer.MAX_VALUE, true) : out;
                    switch (cell.alignment) {
                        default: {
                            throw new AssertionError(cell.alignment);
                        }
                        case ALIGN_LEFT: {
                            tabExpander.append(cellText);
                            repeat(tabExpander, cell.fill, cellPadding - textLength);
                            break;
                        }
                        case ALIGN_RIGHT: {
                            repeat(tabExpander, cell.fill, cellWidth - textLength);
                            tabExpander.append(cellText);
                            break;
                        }
                        case ALIGN_CENTER: {
                            final int leftPadding = (cellWidth - textLength) / 2;
                            repeat(tabExpander, cell.fill, leftPadding);
                            tabExpander.append(cellText);
                            repeat(tabExpander, cell.fill, (cellPadding - leftPadding) - textLength);
                            break;
                        }
                    }
                    out.append(isLastColumn ? rightBorder : columnSeparator);
                }
                if (lineSeparator == null) {
                    lineSeparator = JDK7.lineSeparator();
                }
                out.append(lineSeparator);
            }
        }
    }

    /**
     * Checks if {@code array} contains only {@code null} elements.
     */
    private static boolean isEmpty(final Object[] array) {
        for (int i=array.length; --i>=0;) {
            if (array[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Repeats a character. The {@code count} value may be negative,
     * which is handled as if it was zero.
     *
     * @param out   The stream or buffer where to repeat the character.
     * @param car   Character to write (usually ' ').
     * @param count Number of repetition.
     */
    private static void repeat(final Appendable out, final char car, int count) throws IOException {
        while (--count >= 0) {
            out.append(car);
        }
    }

    /**
     * A class wrapping a cell content and its text alignment.
     * This class if for internal use only.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private static final class Cell {
        /**
         * The text to write inside the cell.
         */
        final String text;

        /**
         * The alignment for {@link #text} inside the cell.
         */
        byte alignment;

        /**
         * The fill character, used for filling space inside the cell.
         */
        final char fill;

        /**
         * Returns a new cell wrapping the specified string with the
         * specified alignment and fill character.
         */
        Cell(final String text, final byte alignment, final char fill) {
            this.text      = text;
            this.alignment = alignment;
            this.fill      = fill;
        }

        /**
         * Returns a new cell which contains substring of this cell.
         */
        Cell substring(final int lower) {
            return new Cell(text.substring(lower), alignment, fill);
        }

        /**
         * Returns the cell content.
         */
        @Override
        public String toString() {
            return text;
        }
    }
}
