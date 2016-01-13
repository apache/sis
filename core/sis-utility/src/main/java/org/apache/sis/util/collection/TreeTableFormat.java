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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import java.text.Format;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.regex.Matcher;
import org.opengis.util.CodeList;
import java.nio.charset.Charset;
import org.opengis.util.Type;
import org.opengis.util.Record;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.io.LineAppender;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.LocalizedParseException;

import static org.apache.sis.util.Characters.NO_BREAK_SPACE;


/**
 * A parser and formatter for {@link TreeTable} instances.
 * This formatter is given an arbitrary number of {@link TableColumn}s
 * to use during the formatting. The first column is taken as the node label.
 * If a {@code TreeTable} is formatted with only that column,
 * then the {@link String} result is like the following example:
 *
 * {@preformat text
 *   Node #1
 *     ├─Node #2
 *     │   └─Node #4
 *     └─Node #3
 * }
 *
 * If the same {@code TreeTable} is formatted with two columns,
 * then the {@link String} result is like the following example:
 *
 * {@preformat text
 *   Node #1……………………… More #1
 *     ├─Node #2…………… More #2
 *     │   └─Node #4… More #4
 *     └─Node #3…………… More #3
 * }
 *
 * This representation can be printed to the {@linkplain java.io.Console#writer() console output}
 * (for example) if the stream uses a monospaced font and supports Unicode characters.
 *
 * <div class="section">Customization</div>
 * Some formatting characteristics (indentation width, column where to draw the vertical line
 * below nodes) can be modified by calls to the setter methods defined in this formatter.
 * In particular, the dots joining the node labels to their values can be specified by the
 * {@linkplain #setColumnSeparatorPattern(String) column separator pattern}.
 * The default pattern is {@code "?……[…] "}, which means <cite>"If the next value is non-null,
 * then insert the {@code "……"} string, repeat the {@code '…'} character as many time as needed
 * (may be zero), and finally insert a space"</cite>.
 *
 * <div class="section">Safety against infinite recursivity</div>
 * Some {@code TreeTable} implementations generate the nodes dynamically as wrappers around Java objects.
 * Such Java objects may contain cyclic associations (<var>A</var> contains <var>B</var> contains <var>C</var>
 * contains <var>A</var>), which result in a tree of infinite depth. Some examples can been found in ISO 19115
 * metadata. This {@code TreeTableFormat} class contains a safety against such cycles. The algorithm is based
 * on the assumption that for each node, the values and children are fully determined by the
 * {@linkplain TreeTable.Node#getUserObject() user object}, if non-null. Consequently for each node <var>C</var>
 * to be formatted, if the user object of that node is the same instance (in the sense of the {@code ==} operator)
 * than the user object of a parent node <var>A</var>, then the children of the <var>C</var> node will not be formatted.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public class TreeTableFormat extends TabularFormat<TreeTable> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 147992015470098561L;

    /**
     * Shared {@code TreeTableFormat} instance for {@link TreeTable#toString()} implementation.
     * Usage of this instance shall be done in a synchronized block.
     */
    static final TreeTableFormat INSTANCE = new TreeTableFormat(null, null);

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
     * The default value is 2, which means that the vertical line is drawn below the third letter
     * of the node label.
     *
     * @see #getVerticalLinePosition()
     * @see #setVerticalLinePosition(int)
     */
    private int verticalLinePosition;

    /**
     * The tree symbols to write in the left margin, or {@code null} if not yet computed.
     * The default symbols are as below:
     *
     * <ul>
     *   <li>{@code treeBlank} = {@code "    "}</li>
     *   <li>{@code treeLine}  = {@code "  │ "}</li>
     *   <li>{@code treeCross} = {@code "  ├─"}</li>
     *   <li>{@code treeEnd}   = {@code "  └─"}</li>
     * </ul>
     *
     * @see #clearTreeSymbols()
     * @see #createTreeSymbols()
     */
    private transient String treeBlank, treeLine, treeCross, treeEnd;

    /**
     * The map to be given to {@link Writer#parentObjects}, created when first needed
     * and reused for subsequent formating.
     */
    private transient Map<Object,Object> parentObjects;

    /**
     * Creates a new tree table format.
     *
     * @param locale   The locale to use for numbers, dates and angles formatting,
     *                 or {@code null} for the {@linkplain Locale#ROOT root locale}.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    public TreeTableFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        indentation          = 4;
        verticalLinePosition = 2;
        beforeFill           = "……";
        fillCharacter        = '…';
        omitTrailingNulls    = true;
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
     * Returns the type of objects formatted by this class.
     *
     * @return {@code TreeTable.class}
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
     * The default value is 2, which means that the vertical line is drawn below the third
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
     * Returns the locale to use for code lists, international strings and exception messages.
     */
    final Locale getDisplayLocale() {
        return getLocale(); // Implemented as getLocale(Locale.Category.DISPLAY) on the JDK7 branch.
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
     * <div class="section">Parsing rules</div>
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
    @SuppressWarnings("null")
    public TreeTable parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        final Matcher matcher   = getColumnSeparatorMatcher(text);
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
            /*
             * Skip leading spaces using Character.isSpaceChar(…) instead than isWhitespace(…)
             * because we need to skip non-breaking spaces as well as ordinary space. We don't
             * need to consider line feeds since they were handled by the lines just above.
             */
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
            i = CharSequences.skipTrailingWhitespaces(text, indexOfLineStart, i) - indexOfLineStart;
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
                    indexOfValue   = CharSequences.skipLeadingWhitespaces (text, indexOfValue, endOfColumn);
                    int endOfValue = CharSequences.skipTrailingWhitespaces(text, indexOfValue, endOfColumn);
                    if (endOfValue > indexOfValue) {
                        parseValue(node, columns[ci], formats[ci], text.subSequence(indexOfValue, endOfValue).toString());
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
                        throw new LocalizedParseException(getDisplayLocale(),
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
                        throw new LocalizedParseException(getDisplayLocale(),
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
     * Writes the column separator to the given appendable. This is a helper method for the
     * {@link Writer} inner class,  defined here because it uses many protected fields from
     * the superclass.  Accessing those fields from the inner class generate many synthetic
     * methods, so we are better to define only one method here doing the work.
     */
    final void writeColumnSeparator(final Appendable out) throws IOException {
        // We have a TableAppender instance if and only if there is 2 or more columns.
        ((TableAppender) out.append(beforeFill)).nextColumn(fillCharacter);
        out.append(columnSeparator);
    }

    /**
     * Creates string representation of the node values. Tabulations are replaced by spaces,
     * and line feeds are replaced by the Pilcrow character. This is necessary in order to
     * avoid conflict with the characters expected by {@link TableAppender}.
     */
    private final class Writer extends LineAppender {
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
         * For each indentation level, {@code true} if the previous levels are writing the last node.
         * This array will growth as needed.
         */
        private boolean[] isLast;

        /**
         * The {@linkplain TreeTable.Node#getUserObject() user object} of the parent nodes.
         * We use this map as a safety against infinite recursivity, on the assumption that
         * the node content and children are fully determined by the user object.
         *
         * <p>User objects in this map will be compared by the identity comparator rather than by the
         * {@code equals(Object)} method because the later may be costly and could itself be vulnerable
         * to infinite recursivity if it has not been implemented defensively.</p>
         *
         * <p>We check the user object instead than the node itself because the same node instance can
         * theoretically not appear twice with a different parent.</p>
         */
        private final Map<Object,Object> parentObjects;

        /**
         * The format for the column in process of being written. This is a format to use for the column as a whole.
         * This field is updated for every new column to write. May be {@code null} if the format is unspecified.
         */
        private transient Format columnFormat;

        /**
         * Creates a new instance which will write to the given appendable.
         *
         * @param out           Where to format the tree.
         * @param column        The columns of the tree table to format.
         * @param parentObjects An initially empty {@link IdentityHashMap}.
         */
        Writer(final Appendable out, final TableColumn<?>[] columns, final Map<Object,Object> parentObjects) {
            super(columns.length >= 2 ? new TableAppender(out, "") : out);
            this.columns = columns;
            this.formats = getFormats(columns, false);
            this.values  = new Object[columns.length];
            this.isLast  = new boolean[8];
            this.parentObjects = parentObjects;
            setTabulationExpanded(true);
            setLineSeparator(" ¶ ");
        }

        /**
         * Localizes the given name in the display locale, or returns "(Unnamed)" if no localized value is found.
         */
        private String toString(final GenericName name) {
            final Locale locale = getDisplayLocale();
            if (name != null) {
                final InternationalString i18n = name.toInternationalString();
                if (i18n != null) {
                    final String localized = i18n.toString(locale);
                    if (localized != null) {
                        return localized;
                    }
                }
                final String localized = name.toString();
                if (localized != null) {
                    return localized;
                }
            }
            return '(' + Vocabulary.getResources(locale).getString(Vocabulary.Keys.Unnamed) + ')';
        }

        /**
         * Appends a textual representation of the given value.
         *
         * @param  value     The value to format (may be {@code null}).
         * @param  recursive {@code true} if this method is invoking itself for writing collection values.
         */
        private void formatValue(final Object value, final boolean recursive) throws IOException {
            final CharSequence text;
            if (value == null) {
                text = " "; // String for missing value.
            } else if (columnFormat != null) {
                if (columnFormat instanceof CompoundFormat<?>) {
                    formatValue((CompoundFormat<?>) columnFormat, value);
                    return;
                }
                text = columnFormat.format(value);
            } else if (value instanceof InternationalString) {
                text = ((InternationalString) value).toString(getDisplayLocale());
            } else if (value instanceof CharSequence) {
                text = value.toString();
            } else if (value instanceof CodeList<?>) {
                text = Types.getCodeTitle((CodeList<?>) value).toString(getDisplayLocale());
            } else if (value instanceof Enum<?>) {
                text = CharSequences.upperCaseToSentence(((Enum<?>) value).name());
            } else if (value instanceof Type) {
                text = toString(((Type) value).getTypeName());
            } else if (value instanceof Locale) {
                final Locale locale = getDisplayLocale();
                text = (locale != Locale.ROOT) ? ((Locale) value).getDisplayName(locale) : value.toString();
            } else if (value instanceof TimeZone) {
                final Locale locale = getDisplayLocale();
                text = (locale != Locale.ROOT) ? ((TimeZone) value).getDisplayName(locale) : ((TimeZone) value).getID();
            } else if (value instanceof Charset) {
                final Locale locale = getDisplayLocale();
                text = (locale != Locale.ROOT) ? ((Charset) value).displayName(locale) : ((Charset) value).name();
            } else if (value instanceof Record) {
                formatCollection(((Record) value).getAttributes().values(), recursive);
                return;
            } else if (value instanceof Iterable<?>) {
                formatCollection((Iterable<?>) value, recursive);
                return;
            } else if (value instanceof Object[]) {
                formatCollection(Arrays.asList((Object[]) value), recursive);
                return;
            } else {
                /*
                 * Check for a value-by-value format only as last resort. If a column-wide format was specified by
                 * the 'columnFormat' field, that format should have been used by above code in order to produce a
                 * more uniform formatting.
                 */
                final Format format = getFormat(value.getClass());
                text = (format != null) ? format.format(value) : value.toString();
            }
            append(text);
        }

        /**
         * Writes the values of the given collection. A maximum of 10 values will be written.
         * If the collection contains other collections, the other collections will <strong>not</strong>
         * be written recursively.
         */
        private void formatCollection(final Iterable<?> values, final boolean recursive)
                throws IOException
        {
            if (values != null) {
                if (recursive) {
                    append('…');                                // Do not format collections inside collections.
                } else {
                    int count = 0;
                    for (final Object value : values) {
                        if (value != null) {
                            if (count != 0) append(", ");
                            formatValue(value, true);
                            if (++count == 10) {                // Arbitrary limit.
                                append(", …");
                                break;
                            }
                        }
                    }
                }
            }
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
            if (!omitTrailingNulls) {
                n = values.length - 1;
            }
            for (int i=0; i<=n; i++) {
                if (i != 0) {
                    writeColumnSeparator(out);
                }
                columnFormat = formats[i];
                formatValue(values[i], false);
                clear();
            }
            out.append(lineSeparator);
            if (level >= isLast.length) {
                isLast = Arrays.copyOf(isLast, level*2);
            }
            /*
             * Format the children only if we do not detect an infinite recursivity. Our recursivity detection
             * algorithm assumes that the node content is fully determined by the user object. If that assumption
             * holds, then we have an infinite recursivity if the user object of the current node is also the user
             * object of a parent node.
             *
             * Note that the value stored in the 'parentObjects' map needs to be the 'userObject' because we want
             * the map value to be null if the user object is null, in order to format the children even if many
             * null user objects exist in the tree.
             */
            final Object userObject = node.getUserObject();
            if (parentObjects.put(userObject, userObject) == null) {
                final Iterator<? extends TreeTable.Node> it = node.getChildren().iterator();
                boolean hasNext = it.hasNext();
                while (hasNext) {
                    final TreeTable.Node child = it.next();
                    hasNext = it.hasNext();
                    isLast[level] = !hasNext; // Must be set before the call to 'format' below.
                    format(child, level+1);
                }
                parentObjects.remove(userObject);
            } else {
                /*
                 * Detected a recursivity. Format "(cycle omitted)" just below the node.
                 */
                for (int i=0; i<level; i++) {
                    out.append(getTreeSymbols(true, isLast[i]));
                }
                final Locale locale = getDisplayLocale();
                out.append('(').append(Vocabulary.getResources(locale)
                   .getString(Vocabulary.Keys.CycleOmitted).toLowerCase(locale))
                   .append(')').append(lineSeparator);
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
     * @throws IOException If an error occurred while writing to the given appendable.
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
        if (parentObjects == null) {
            parentObjects = new IdentityHashMap<Object,Object>();
        }
        try {
            final Writer out = new Writer(toAppendTo, columns, parentObjects);
            out.format(tree.getRoot(), 0);
            out.flush();
        } finally {
            parentObjects.clear();
        }
    }
}
