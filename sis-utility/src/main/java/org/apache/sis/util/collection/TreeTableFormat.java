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
package org.apache.sis.util.collection;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import java.text.Format;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.jcip.annotations.NotThreadSafe;
import org.apache.sis.io.LineFormatter;
import org.apache.sis.io.TableFormatter;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.LocalizedParseException;

import static org.apache.sis.util.Characters.NO_BREAK_SPACE;


/**
 * A parser and formatter for {@link TreeTable} instances.
 * This formatter is given an arbitrary number of {@link TableColumn}s
 * to use during the formatting. The first column is taken as the node label.
 * If a {@code TreeTable} is formatted with only that column,
 * then the {@link String} result is like the following example:
 *
 * {@preformat text
 *   Node #1
 *   ├───Node #2
 *   │   └───Node #4
 *   └───Node #3
 * }
 *
 * If the same {@code TreeTable} is formatted with two columns,
 * then the {@link String} result is like the following example:
 *
 * {@preformat text
 *   Node #1……………………… More #1
 *   ├───Node #2…………… More #2
 *   │   └───Node #4… More #4
 *   └───Node #3…………… More #3
 * }
 *
 * This representation can be printed to the {@linkplain java.io.Console#writer() console output}
 * (for example) if the stream uses a monospaced font and supports Unicode characters.
 * Some formatting characteristics (indentation width, column where to draw the vertical line
 * below nodes) can be modified by calls to the setter methods defined in this formatter.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.io.TableFormatter
 */
@NotThreadSafe
public class TreeTableFormat extends CompoundFormat<TreeTable> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4476366905386037025L;

    /**
     * The table columns to format, or {@code null} for formatting all of them.
     * This map shall not be modified after creation, because it may be shared
     * by many tables.
     *
     * @see #getColumns()
     * @see #setColumns(TableColumn[])
     */
    private Map<TableColumn<?>,Integer> columnIndices;

    /**
     * The number of characters to add on the left side for each indentation level.
     * The default value is 4.
     *
     * @see #getIndentation()
     * @see #setIndentation(int)
     */
    private int indentation;

    /**
     * The position of the vertical line, relative to the position of the label of the parent node.
     * The default value is 0, which means that the vertical line is drawn below the first letter
     * of the node label.
     *
     * @see #getVerticalLinePosition()
     * @see #setVerticalLinePosition(int)
     */
    private int verticalLinePosition;

    /**
     * The string to write before and after the {@link #columnSeparator},
     * or an empty string if none.
     */
    String separatorPrefix, separatorSuffix;

    /**
     * The column separator to use at formatting time if there is more than one column.
     * This character will be repeated as many time as needed.
     *
     * @see #getColumnSeparatorPattern()
     * @see #setColumnSeparatorPattern(String)
     */
    char columnSeparator;

    /**
     * The line separator to use for formatting the tree.
     *
     * @see #getLineSeparator()
     * @see #setLineSeparator(String)
     */
    String lineSeparator;

    /**
     * The tree symbols to write in the left margin, or {@code null} if not yet computed.
     * The default symbols are as below:
     *
     * <ul>
     *   <li>{@code treeBlank} = {@code "    "}</li>
     *   <li>{@code treeLine}  = {@code "│   "}</li>
     *   <li>{@code treeCross} = {@code "├───"}</li>
     *   <li>{@code treeEnd}   = {@code "└───"}</li>
     * </ul>
     *
     * @see #clearTreeSymbols()
     * @see #createTreeSymbols()
     */
    private transient String treeBlank, treeLine, treeCross, treeEnd;

    /**
     * Creates a new tree table format.
     *
     * @param locale   The locale to use for numbers, dates and angles formatting.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    public TreeTableFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        indentation     = 4;
        separatorPrefix = "……";
        columnSeparator = '…';
        separatorSuffix = " ";
        lineSeparator   = System.lineSeparator();
    }

    /**
     * Clears the symbols used when writing the tree.
     * They will be computed again when first needed.
     *
     * @see #createTreeSymbols()
     */
    private void clearTreeSymbols() {
        treeBlank = null;
        treeLine  = null;
        treeCross = null;
        treeEnd   = null;
    }

    /**
     * Returns the type of object formatted by this class, which is {@link TreeTable}.
     */
    @Override
    public final Class<TreeTable> getValueType() {
        return TreeTable.class;
    }

    /**
     * Returns the table columns to parse and format, or {@code null} for the default list of
     * columns. The default is:
     *
     * <ul>
     *   <li>On parsing, a single column containing the node label as a {@link String}.</li>
     *   <li>On formatting, {@linkplain TreeTable#getColumns() all <code>TreeTable</code> columns}.</li>
     * </ul>
     *
     * @return The table columns to parse and format, or {@code null} for the default.
     */
    public TableColumn<?>[] getColumns() {
        return (columnIndices != null) ? DefaultTreeTable.getColumns(columnIndices) : null;
    }

    /**
     * Sets the table columns to parse and format. A {@code null} value means to use the default
     * list of columns, as defined in the {@link #getColumns()} method.
     *
     * @param  columns The table columns to parse and format, or {@code null} for the default.
     * @throws IllegalArgumentException If the given array is empty, contains a null element
     *         or a duplicated value.
     */
    public void setColumns(final TableColumn<?>... columns) throws IllegalArgumentException {
        if (columns == null) {
            columnIndices = null;
        } else {
            if (columns.length == 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "columns"));
            }
            columnIndices = DefaultTreeTable.createColumnIndices(columns);
        }
    }

    /**
     * Returns the pattern of characters used in column separators. Those characters will be used
     * only if more than one column is formatted. The default pattern is {@code "……[…] "}, which
     * means "<cite>insert the {@code "……"} string, then repeat the {@code '…'} character as many
     * time as needed (may be zero), and finally insert a space</cite>".
     *
     * @return The pattern of the current column separator.
     */
    public String getColumnSeparatorPattern() {
        final StringBuilder buffer = new StringBuilder(8);
        buffer.append(separatorPrefix).append('\uFFFF').append(separatorSuffix);
        StringBuilders.replace(buffer, "\\", "\\\\");
        StringBuilders.replace(buffer, "[",  "\\[");
        StringBuilders.replace(buffer, "]",  "\\]");
        final int insertAt = buffer.indexOf("\uFFFF");
        buffer.replace(insertAt, insertAt+1, "[\uFFFF]").setCharAt(insertAt+1, columnSeparator);
        return buffer.toString();
    }

    /**
     * Sets the pattern of the characters to insert between the columns. The pattern shall contain
     * exactly one occurrence of the {@code "[ ]"} pair of bracket, with exactly one character
     * between them. This character will be repeated as many time as needed for columns alignment.
     *
     * <p>In current implementation, the above-cited repeated character must be in the
     * {@linkplain Character#isBmpCodePoint(int) Basic Multilanguage Plane}.</p>
     *
     * @param  pattern The pattern of the new column separator.
     * @throws IllegalArgumentException If the given pattern is illegal.
     */
    public void setColumnSeparatorPattern(final String pattern) throws IllegalArgumentException {
        ArgumentChecks.ensureNonEmpty("pattern", pattern);
        final int length = pattern.length();
        final StringBuilder buffer = new StringBuilder(length);
        boolean escape  = false;
        String  prefix  = null;
        int separatorIndex = -1;
scan:   for (int i=0; i<length; i++) {
            final char c = pattern.charAt(i);
            switch (c) {
                case '\uFFFF': { // This "character" is reserved.
                    prefix = null;
                    break scan; // This will cause IllegalArgumentException to be thrown.
                }
                case '\\': {
                    if (i != separatorIndex) {
                        if (escape) break;
                        escape = true;
                    }
                    continue;
                }
                case '[': {
                    if (escape) break;
                    if (i != separatorIndex) {
                        if (separatorIndex >= 0) {
                            prefix = null;
                            break scan; // This will cause IllegalArgumentException to be thrown.
                        }
                        separatorIndex = i+1;
                    }
                    continue;
                }
                case ']': {
                    if (escape) break;
                    switch (i - separatorIndex) {
                        case 0:  continue;
                        case 1:  prefix = buffer.toString(); buffer.setLength(0); continue;
                        default: prefix = null; break scan;
                    }
                }
            }
            if (i != separatorIndex) {
                buffer.append(c);
            }
        }
        if (prefix == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalFormatPatternForClass_2, pattern, TreeTable.class));
        }
        separatorPrefix = prefix;
        separatorSuffix = buffer.toString();
        columnSeparator = pattern.charAt(separatorIndex);
    }

    /**
     * Returns the current line separator. The default value is system-dependent.
     *
     * @return The current line separator.
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets the line separator.
     *
     * @param separator The new line separator.
     */
    public void setLineSeparator(final String separator) {
        ArgumentChecks.ensureNonEmpty("separator", separator);
        lineSeparator = separator;
    }

    /**
     * Returns the number of spaces to add on the left margin for each indentation level.
     * The default value is 4.
     *
     * @return The current indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets the number of spaces to add on the left margin for each indentation level.
     * If the new indentation is smaller than the {@linkplain #getVerticalLinePosition()
     * vertical line position}, then the later is also set to the given indentation value.
     *
     * @param  indentation The new indentation.
     * @throws IllegalArgumentException If the given value is negative.
     */
    public void setIndentation(final int indentation) throws IllegalArgumentException {
        ArgumentChecks.ensurePositive("indentation", indentation);
        this.indentation = indentation;
        if (verticalLinePosition > indentation) {
            verticalLinePosition = indentation;
        }
        clearTreeSymbols();
    }

    /**
     * Returns the position of the vertical line, relative to the position of the root label.
     * The default value is 0, which means that the vertical line is drawn below the first
     * letter of the root label.
     *
     * @return The current vertical line position.
     */
    public int getVerticalLinePosition() {
        return verticalLinePosition;
    }

    /**
     * Sets the position of the vertical line, relative to the position of the root label.
     * The given value can not be greater than the {@linkplain #getIndentation() indentation}.
     *
     * @param  verticalLinePosition The new vertical line position.
     * @throws IllegalArgumentException If the given value is negative or greater than the indentation.
     */
    public void setVerticalLinePosition(final int verticalLinePosition) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("verticalLinePosition", 0, indentation, verticalLinePosition);
        this.verticalLinePosition = verticalLinePosition;
        clearTreeSymbols();
    }

    /**
     * Returns the formats to use for parsing and formatting the values of each column.
     * The returned array may contain {@code null} elements, which means that the values
     * in that column can be stored as {@code String}s.
     *
     * @param  mandatoy {@code true} if an exception shall be thrown for unrecognized types,
     *         or {@code false} for storing a {@code null} value in the array instead.
     * @throws IllegalStateException If {@code mandatory} is {@code true} and a column
     *         contains values of an unsupported type.
     */
    final Format[] getFormats(final TableColumn<?>[] columns, final boolean mandatory) throws IllegalStateException {
        final Format[] formats = new Format[columns.length];
        for (int i=0; i<formats.length; i++) {
            final Class<?> valueType = columns[i].getElementType();
            if ((formats[i] = getFormat(valueType)) == null) {
                if (mandatory && !valueType.isAssignableFrom(String.class)) {
                    throw new IllegalStateException(Errors.format(
                            Errors.Keys.UnspecifiedFormatForClass_1, valueType));
                }
            }
        }
        return formats;
    }

    /**
     * Creates a tree from the given character sequence,
     * or returns {@code null} if an error occurred while parsing the characters.
     * This method can parse the trees created by the {@code format(…)} methods
     * defined in this class.
     *
     * {@section Parsing rules}
     * <ul>
     *   <li>Each node shall be represented by a single line made of two parts, in that order:
     *     <ol>
     *       <li>white spaces and tree drawing characters ({@code '│'}, {@code '├'}, {@code '└'} or {@code '─'});</li>
     *       <li>string representations of node values, separated by the
     *           {@linkplain #getColumnSeparatorPattern() colunm separator}.</li>
     *     </ol>
     *   </li>
     *   <li>The number of spaces and drawing characters before the node values determines the node
     *       indentation. This indentation doesn't need to be a factor of the {@link #getIndentation()}
     *       value, but must be consistent across all the parsed tree.</li>
     *   <li>The indentation determines the parent of each node.</li>
     *   <li>Parsing stops at first empty line (ignoring whitespaces), or at the end of the given text.</li>
     * </ul>
     *
     * @param  text The character sequence for the tree to parse.
     * @param  pos  The position where to start the parsing.
     * @return The parsed tree, or {@code null} if the given character sequence can not be parsed.
     * @throws ParseException If an error occurred while parsing a node value.
     */
    @Override
    public TreeTable parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        final Matcher matcher = Pattern.compile(
                Pattern.quote(separatorPrefix)
              + Pattern.quote(String.valueOf(columnSeparator)) + '*'
              + Pattern.quote(separatorSuffix)).matcher(text);
        final int length        = text.length();
        int indexOfLineStart    = pos.getIndex();
        int indentationLevel    = 0;                // Current index in the 'indentations' array.
        int[] indentations      = new int[16];      // Number of spaces (ignoring drawing characters) for each level.
        TreeTable.Node lastNode = null;             // Last parsed node, having 'indentation[level]' characters before its content.
        TreeTable.Node root     = null;             // First node found while parsing.
        final DefaultTreeTable table = new DefaultTreeTable(columnIndices != null ? columnIndices : TableColumn.NAME_MAP);
        final TableColumn<?>[] columns = DefaultTreeTable.getColumns(table.columnIndices);
        final Format[] formats = getFormats(columns, true);
        do {
            final int startNextLine = CharSequences.indexOfLineStart(text, 1, indexOfLineStart);
            int endOfLine = startNextLine;
            while (endOfLine > indexOfLineStart) {
                final int c = text.charAt(endOfLine-1);
                if (c != '\r' && c != '\n') break;
                endOfLine--; // Skip trailing '\r' and '\n'.
            }
            boolean hasChar = false;
            int i; // The indentation of current line.
            for (i=indexOfLineStart; i<endOfLine;) {
                final int c = Character.codePointAt(text, i);
                if (!Character.isSpaceChar(c)) {
                    hasChar = true;
                    if ("─│└├".indexOf(c) < 0) {
                        break;
                    }
                }
                i += Character.charCount(c);
            }
            if (!hasChar) {
                break; // The line contains only whitespaces.
            }
            /*
             * Go back to the fist non-space character (should be '─'). We do that in case the
             * user puts some spaces in the text of the node label, since we don't want those
             * user-spaces to interfer with the calculation of indentation.
             */
            int indexOfValue = i;
            while (i > indexOfLineStart) {
                final int c = Character.codePointBefore(text, i);
                if (!Character.isSpaceChar(c)) break;
                i -= Character.charCount(c);
            }
            i -= indexOfLineStart;
            /*
             * Found the first character which is not part of the indentation. Create a new root
             * (without parent for now) and parse the values for each column. Columns with empty
             * text are not parsed (the value is left to null).
             */
            final TreeTable.Node node = new DefaultTreeTable.Node(table);
            try {
                matcher.region(indexOfValue, endOfLine);
                for (int ci=0; ci<columns.length; ci++) {
                    final boolean found = matcher.find();
                    int endOfColumn = found ? matcher.start() : endOfLine;
                    while (indexOfValue < endOfColumn) {
                        final int c = Character.codePointAt(text, indexOfValue);
                        if (!Character.isSpaceChar(c)) break;
                        indexOfValue += Character.charCount(c);
                    }
                    // Ignore trailing spaces at the end of this column, then parse the value.
                    for (int endOfValue = endOfColumn; endOfValue > indexOfValue;) {
                        final int c = Character.codePointBefore(text, endOfValue);
                        if (!Character.isSpaceChar(c)) {
                            parseValue(node, columns[ci], formats[ci], text.subSequence(indexOfValue, endOfValue).toString());
                            break;
                        }
                        endOfValue -= Character.charCount(c);
                    }
                    if (!found) break;
                    // The end of this column will be the beginning of the next column,
                    // after skipping the last character of the column separator.
                    indexOfValue = matcher.end();
                }
            } catch (ParseException e) {
                pos.setErrorIndex(indexOfValue);
                throw e;
            }
            /*
             * If this is the first node created so far, it will be the root.
             */
            if (root == null) {
                indentations[0] = i;
                root = node;
            } else {
                int p;
                while (i < (p = indentations[indentationLevel])) {
                    /*
                     * Lower indentation level: go up in the tree until we find the new parent.
                     * Note that lastNode.getParent() should never return null, since only the
                     * node at 'indentationLevel == 0' has a null parent and we check that case.
                     */
                    if (--indentationLevel < 0) {
                        pos.setErrorIndex(indexOfLineStart);
                        throw new LocalizedParseException(locale,
                                Errors.Keys.NodeHasNoParent_1, new Object[] {node}, 0);
                    }
                    lastNode = lastNode.getParent();
                }
                if (i == p) {
                    /*
                     * The node we just created is a sibling of the previous node. This is
                     * illegal if level==0, in which case we have no parent. Otherwise add
                     * the sibling to the common parent and let the indentation level unchanged.
                     */
                    final TreeTable.Node parent = lastNode.getParent();
                    if (parent == null) {
                        pos.setErrorIndex(indexOfLineStart);
                        throw new LocalizedParseException(locale,
                                Errors.Keys.NodeHasNoParent_1, new Object[] {node}, 0);
                    }
                    parent.getChildren().add(node);
                } else if (i > p) {
                    /*
                     * The node we just created is a child of the previous node.
                     * Add a new indentation level.
                     */
                    lastNode.getChildren().add(node);
                    if (++indentationLevel == indentations.length) {
                        indentations = Arrays.copyOf(indentations, indentationLevel*2);
                    }
                    indentations[indentationLevel] = i;
                }
            }
            lastNode = node;
            indexOfLineStart = startNextLine;
        } while (indexOfLineStart != length);
        if (root == null) {
            return null;
        }
        pos.setIndex(indexOfLineStart);
        table.setRoot(root);
        return table;
    }

    /**
     * Parses the given string using a format appropriate for the type of values in
     * the given column, and stores the value in the given node.
     *
     * @param  V        The type of values in the given column.
     * @param  node     The node in which to set the value.
     * @param  column   The column in which to set the value.
     * @param  format   The format to use for parsing the value, or {@code null}.
     * @param  text     The textual representation of the value.
     * @throws ParseException If an error occurred while parsing.
     */
    private <V> void parseValue(final TreeTable.Node node, final TableColumn<V> column,
            final Format format, final String text) throws ParseException
    {
        final Object value;
        if (format != null) {
            value = format.parseObject(text);
        } else {
            value = text;
        }
        node.setValue(column, column.getElementType().cast(value));
    }

    /**
     * Computes the {@code tree*} fields from the {@link #indentation} and
     * {@link #verticalLinePosition} current values.
     *
     * @see #clearTreeSymbols()
     */
    private void createTreeSymbols() {
        final int indentation = this.indentation;
        final int verticalLinePosition = this.verticalLinePosition;
        final char[] buffer = new char[indentation];
        for (int k=0; k<4; k++) {
            final char vc, hc;
            if ((k & 2) == 0) {
                // No horizontal line
                vc = (k & 1) == 0 ? NO_BREAK_SPACE : '│';
                hc = NO_BREAK_SPACE;
            } else {
                // With a horizontal line
                vc = (k & 1) == 0 ? '└' : '├';
                hc = '─';
            }
            Arrays.fill(buffer, 0, verticalLinePosition, NO_BREAK_SPACE);
            buffer[verticalLinePosition] = vc;
            Arrays.fill(buffer, verticalLinePosition + 1, indentation, hc);
            final String symbols = String.valueOf(buffer);
            switch (k) {
                case 0: treeBlank = symbols; break;
                case 1: treeLine  = symbols; break;
                case 2: treeEnd   = symbols; break;
                case 3: treeCross = symbols; break;
                default: throw new AssertionError(k);
            }
        }
    }

    /**
     * Returns the string to write before a node.
     *
     * @param isParent {@code true} for a parent node, or {@code false} for the actual node.
     * @param isLast   {@code true} if the node is the last children of its parent node.
     */
    final String getTreeSymbols(final boolean isParent, final boolean isLast) {
        return(isParent ? (isLast ? treeBlank : treeLine)
                        : (isLast ? treeEnd   : treeCross));
    }

    /**
     * Creates string representation of the node values. Tabulations are replaced by spaces,
     * and line feeds are replaced by the Pilcrow character. This is necessary in order to
     * avoid conflict with the characters expected by {@link TableFormatter}.
     */
    private final class Writer extends LineFormatter {
        /**
         * For each indentation level, {@code true} if the previous levels are writing the last node.
         * This array will growth as needed.
         */
        private boolean[] isLast;

        /**
         * The columns to write.
         */
        private final TableColumn<?>[] columns;

        /**
         * The format to use for each column.
         */
        private final Format[] formats;

        /**
         * The node values to format.
         */
        private final Object[] values;

        /**
         * Creates a new instance which will write in the given appendable.
         */
        Writer(final Appendable out, final TableColumn<?>[] columns) {
            super(columns.length >= 2 ? new TableFormatter(out, "") : out);
            this.columns  = columns;
            this.formats  = getFormats(columns, false);
            this.values   = new Object[columns.length];
            this.isLast   = new boolean[8];
            setTabulationExpanded(true);
            setLineSeparator(" ¶ ");
        }

        /**
         * Appends a textual representation of the given value.
         *
         * @param  format The format to use.
         * @param  value  The value to format (may be {@code null}).
         */
        private void formatValue(final Format format, final Object value) throws IOException {
            final CharSequence text;
            if (value == null) {
                text = " "; // String for missing value.
            } else if (format != null) {
                if (format instanceof CompoundFormat<?>) {
                    formatValue((CompoundFormat<?>) format, value);
                    return;
                }
                text = format.format(value);
            } else {
                text = String.valueOf(value);
            }
            append(text);
        }

        /**
         * Work around for the inability to define the variable {@code <V>} locally.
         */
        @Workaround(library="JDK", version="1.7")
        private <V> void formatValue(final CompoundFormat<V> format, final Object value) throws IOException {
            format.format(format.getValueType().cast(value), this);
        }

        /**
         * Appends the string representation of the given node and all its children.
         * This method invokes itself recursively.
         *
         * @param node  The node to format.
         * @param level Indentation level. The first level is 0.
         */
        final void format(final TreeTable.Node node, final int level) throws IOException {
            for (int i=0; i<level; i++) {
                out.append(getTreeSymbols(i != level-1, isLast[i]));
            }
            int n = 0;
            for (int i=0; i<columns.length; i++) {
                if ((values[i] = node.getValue(columns[i])) != null) {
                    n = i;
                }
            }
            for (int i=0; i<=n; i++) {
                if (i != 0) {
                    // We have a TableFormatter instance if and only if there is 2 or more columns.
                    ((TableFormatter) out.append(separatorPrefix)).nextColumn(columnSeparator);
                    out.append(separatorSuffix);
                }
                formatValue(formats[i], values[i]);
                clear();
            }
            out.append(lineSeparator);
            if (level >= isLast.length) {
                isLast = Arrays.copyOf(isLast, level*2);
            }
            final List<? extends TreeTable.Node> children = node.getChildren();
            final int count = children.size();
            for (int i=0; i<count; i++) {
                isLast[level] = (i == count-1);
                format(children.get(i), level+1);
            }
        }
    }

    /**
     * Writes a graphical representation of the specified tree table in the given stream or buffer.
     * This method iterates recursively over all {@linkplain TreeTable.Node#getChildren() children}.
     * For each {@linkplain #getColumns() column to format} in each node, this method gets a textual
     * representation of the {@linkplain TreeTable.Node#getValue(TableColumn) value in that column}
     * using the formatter obtained by a call to {@link #getFormat(Class)}.
     *
     * @param  tree        The tree to format.
     * @param  toAppendTo  Where to format the tree.
     * @throws IOException If an error occurred while writing in the given appender.
     *
     * @see TreeTables#toString(TreeTable)
     */
    @Override
    public void format(final TreeTable tree, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("tree", tree);
        if (treeBlank == null) {
            createTreeSymbols();
        }
        TableColumn<?>[] columns;
        if (columnIndices != null) {
            columns = DefaultTreeTable.getColumns(columnIndices);
        } else {
            final List<TableColumn<?>> c = tree.getColumns();
            columns = c.toArray(new TableColumn<?>[c.size()]);
        }
        final Writer out = new Writer(toAppendTo, columns);
        out.format(tree.getRoot(), 0);
        out.flush();
    }
}
