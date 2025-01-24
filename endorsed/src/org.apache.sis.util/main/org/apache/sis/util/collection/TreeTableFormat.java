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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.ConcurrentModificationException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.io.IOException;
import java.text.Format;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.internal.Acyclic;
import org.apache.sis.util.privy.PropertyFormat;
import org.apache.sis.util.privy.LocalizedParseException;
import org.apache.sis.util.privy.TreeFormatCustomization;
import static org.apache.sis.util.Characters.NO_BREAK_SPACE;


/**
 * A parser and formatter for {@link TreeTable} instances.
 * This formatter is given an arbitrary number of {@link TableColumn}s
 * to use during the formatting. The first column is taken as the node label.
 * If a {@code TreeTable} is formatted with only that column,
 * then the {@link String} result is like the following example:
 *
 * <pre class="text">
 *   Node #1
 *     ├─Node #2
 *     │   └─Node #4
 *     └─Node #3</pre>
 *
 * If the same {@code TreeTable} is formatted with two columns,
 * then the {@link String} result is like the following example:
 *
 * <pre class="text">
 *   Node #1……………………… More #1
 *     ├─Node #2…………… More #2
 *     │   └─Node #4… More #4
 *     └─Node #3…………… More #3</pre>
 *
 * This representation can be printed to the {@linkplain java.io.Console#writer() console output}
 * (for example) if the stream uses a monospaced font and supports Unicode characters.
 *
 * <h2>Customization</h2>
 * Some formatting characteristics (indentation width, column where to draw the vertical line
 * below nodes) can be modified by calls to the setter methods defined in this formatter.
 * In particular, the dots joining the node labels to their values can be specified by the
 * {@linkplain #setColumnSeparatorPattern(String) column separator pattern}.
 * The default pattern is {@code "?……[…] "}, which means <q>If the next value is non-null,
 * then insert the {@code "……"} string, repeat the {@code '…'} character as many time as needed
 * (may be zero), and finally insert a space</q>.
 *
 * <h2>Safety against infinite recursion</h2>
 * Some {@code TreeTable} implementations generate the nodes dynamically as wrappers around Java objects.
 * Such Java objects may contain cyclic associations (<var>A</var> contains <var>B</var> contains <var>C</var>
 * contains <var>A</var>), which result in a tree of infinite depth. Some examples can be found in ISO 19115
 * metadata. This {@code TreeTableFormat} class contains a safety against such cycles. The algorithm is based
 * on the assumption that for each node, the values and children are fully determined by the
 * {@linkplain TreeTable.Node#getUserObject() user object}, if non-null. Consequently, for each node <var>C</var>
 * to be formatted, if the user object of that node is the same instance (in the sense of the {@code ==} operator)
 * than the user object of a parent node <var>A</var>, then the children of the <var>C</var> node will not be formatted.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   0.3
 */
public class TreeTableFormat extends TabularFormat<TreeTable> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 147992015470098561L;

    /**
     * Shared {@code TreeTableFormat} instance for {@link DefaultTreeTable#toString()} implementation.
     * Usage of this instance shall be done in a synchronized block. Note that metadata objects defined
     * as {@link org.apache.sis.metadata.AbstractMetadata} subclasses use their own format instance.
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
    @SuppressWarnings("serial")         // The implementations that we use are Serializable.
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
     * A filter for specifying whether a node should be formatted, or {@code null} if no filtering is applied.
     * This is ignored at parsing time.
     *
     * @see #getNodeFilter()
     * @see #setNodeFilter(Predicate)
     */
    @SuppressWarnings("serial")             // Serialization of lambda functions is possible (but discouraged).
    private Predicate<TreeTable.Node> nodeFilter;

    /**
     * The set to be given to {@link Writer} constructor,
     * created when first needed and reused for subsequent formatting.
     */
    private transient Set<TreeTable.Node> recursionGuard;

    /**
     * A clone of the number format to be used with different settings (number of fraction digits, scientific notation).
     * We use a clone for avoiding to change the setting of potentially user supplied number format. This is used only
     * for floating point numbers, not for integers.
     */
    private transient DecimalFormat adaptableFormat;

    /**
     * The default pattern used by {@link #adaptableFormat}.
     * Used for switching back to default mode after scientific notation.
     */
    private transient String defaultPattern;

    /**
     * Whether {@link #adaptableFormat} is using scientific notation.
     */
    private transient boolean usingScientificNotation;

    /**
     * Creates a new tree table format.
     *
     * @param locale    the locale to use for numbers, dates and angles formatting,
     *                  or {@code null} for the {@linkplain Locale#ROOT root locale}.
     * @param timezone  the timezone, or {@code null} for UTC.
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
     * Returns the table columns to parse and format, or {@code null} for the default list of columns.
     * The default is:
     *
     * <ul>
     *   <li>On parsing, a single column containing the node label as a {@link String}.</li>
     *   <li>On formatting, {@linkplain TreeTable#getColumns() all <code>TreeTable</code> columns}.</li>
     * </ul>
     *
     * @return the table columns to parse and format, or {@code null} for the default.
     */
    public TableColumn<?>[] getColumns() {
        return (columnIndices != null) ? DefaultTreeTable.getColumns(columnIndices) : null;
    }

    /**
     * Sets the table columns to parse and format. A {@code null} value means to use the default
     * list of columns, as defined in the {@link #getColumns()} method.
     *
     * @param  columns  the table columns to parse and format, or {@code null} for the default.
     * @throws IllegalArgumentException if the given array is empty, contains a null element
     *         or a duplicated value.
     */
    public void setColumns(final TableColumn<?>... columns) throws IllegalArgumentException {
        if (columns == null) {
            columnIndices = null;
        } else {
            ArgumentChecks.ensureNonEmpty("columns", columns);
            columnIndices = DefaultTreeTable.createColumnIndices(columns);
        }
    }

    /**
     * Returns the number of spaces to add on the left margin for each indentation level.
     * The default value is 4.
     *
     * @return the current indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets the number of spaces to add on the left margin for each indentation level.
     * If the new indentation is smaller than the {@linkplain #getVerticalLinePosition()
     * vertical line position}, then the latter is also set to the given indentation value.
     *
     * @param  indentation  the new indentation.
     * @throws IllegalArgumentException if the given value is negative.
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
     * @return the current vertical line position.
     */
    public int getVerticalLinePosition() {
        return verticalLinePosition;
    }

    /**
     * Sets the position of the vertical line, relative to the position of the root label.
     * The given value cannot be greater than the {@linkplain #getIndentation() indentation}.
     *
     * @param  verticalLinePosition  the new vertical line position.
     * @throws IllegalArgumentException if the given value is negative or greater than the indentation.
     */
    public void setVerticalLinePosition(final int verticalLinePosition) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("verticalLinePosition", 0, indentation, verticalLinePosition);
        this.verticalLinePosition = verticalLinePosition;
        clearTreeSymbols();
    }

    /**
     * Returns the filter that specify whether a node should be formatted or ignored.
     * This is the predicate specified in the last call to {@link #setNodeFilter(Predicate)}.
     * If no filter has been set, then this method returns {@code null}.
     *
     * @return a filter for specifying whether a node should be formatted, or {@code null} if no filtering is applied.
     *
     * @since 1.0
     */
    public Predicate<TreeTable.Node> getNodeFilter() {
        return nodeFilter;
    }

    /**
     * Sets a filter specifying whether a node should be formatted or ignored.
     * Filters are tested at formatting time for all children of the root node (but not for the root node itself).
     * Filters are ignored at parsing time.
     *
     * @param  filter  filter for specifying whether a node should be formatted, or {@code null} for no filtering.
     *
     * @since 1.0
     */
    public void setNodeFilter(final Predicate<TreeTable.Node> filter) {
        this.nodeFilter = filter;
    }

    /**
     * Returns the locale to use for code lists, international strings and localized messages of exceptions.
     */
    private Locale getDisplayLocale() {
        return getLocale(Locale.Category.DISPLAY);
    }

    /**
     * Returns the formats to use for parsing and formatting the values of each column.
     * The returned array may contain {@code null} elements, which means that the values
     * in that column can be stored as {@code String}s.
     *
     * @param  mandatory  {@code true} if an exception shall be thrown for unrecognized types, or
     *                    {@code false} for storing a {@code null} value in the array instead.
     * @throws IllegalStateException if {@code mandatory} is {@code true} and a column
     *         contains values of an unsupported type.
     */
    final Format[] getFormats(final TableColumn<?>[] columns, final boolean mandatory) throws IllegalStateException {
        final var formats = new Format[columns.length];
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
     * or returns {@code null} if the given text does not look like a tree for this method.
     * This method can parse the trees created by the {@code format(…)} methods
     * defined in this class.
     *
     * <h4>Parsing rules</h4>
     * <ul>
     *   <li>Each node shall be represented by a single line made of two parts, in that order:
     *     <ol>
     *       <li>white spaces and tree drawing characters ({@code '│'}, {@code '├'}, {@code '└'} or {@code '─'});</li>
     *       <li>string representations of node values, separated by the
     *           {@linkplain #getColumnSeparatorPattern() colunm separator}.</li>
     *     </ol>
     *   </li>
     *   <li>The number of spaces and drawing characters before the node values determines the node
     *       indentation. This indentation does not need to be a factor of the {@link #getIndentation()}
     *       value, but must be consistent across all the parsed tree.</li>
     *   <li>The indentation determines the parent of each node.</li>
     *   <li>Parsing stops at first empty line (ignoring whitespaces), or at the end of the given text.</li>
     * </ul>
     *
     * <h4>Error index</h4>
     * If the given text does not seem to be a tree table, then this method returns {@code null}.
     * Otherwise if parsing started but failed, then:
     *
     * <ul>
     *   <li>{@link ParsePosition#getErrorIndex()} will give the index at the beginning
     *       of line or beginning of cell where the error occurred, and</li>
     *   <li>{@link ParseException#getErrorOffset()} will give either the same value,
     *       or a slightly more accurate value inside the cell.</li>
     * </ul>
     *
     * @param  text  the character sequence for the tree to parse.
     * @param  pos   the position where to start the parsing.
     * @return the parsed tree, or {@code null} if the given character sequence cannot be parsed.
     * @throws ParseException if an error occurred while parsing a node value.
     */
    @Override
    public TreeTable parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        final Matcher matcher   = getColumnSeparatorMatcher(text);
        final int length        = text.length();
        int indexOfLineStart    = pos.getIndex();
        int indentationLevel    = 0;                // Current index in the `indentations` array.
        int[] indentations      = new int[16];      // Number of spaces (ignoring drawing characters) for each level.
        TreeTable.Node lastNode = null;             // Last parsed node, having `indentation[level]` characters before its content.
        TreeTable.Node root     = null;             // First node found while parsing.
        final var table = new DefaultTreeTable(columnIndices != null ? columnIndices : TableColumn.NAME_MAP);
        final TableColumn<?>[] columns = DefaultTreeTable.getColumns(table.columnIndices);
        final Format[] formats = getFormats(columns, true);
        do {
            final int startNextLine = CharSequences.indexOfLineStart(text, 1, indexOfLineStart);
            int endOfLine = startNextLine;
            while (endOfLine > indexOfLineStart) {
                final int c = text.charAt(endOfLine-1);
                if (c != '\r' && c != '\n') break;
                endOfLine--;                                    // Skip trailing '\r' and '\n'.
            }
            /*
             * Skip leading spaces using Character.isSpaceChar(…) instead of isWhitespace(…)
             * because we need to skip non-breaking spaces as well as ordinary space. We don't
             * need to consider line feeds since they were handled by the lines just above.
             */
            boolean hasChar = false;
            int i;                                              // The indentation of current line.
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
                break;                                          // The line contains only whitespaces.
            }
            /*
             * Go back to the first non-space character (should be '─'). We do that in case the
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
            final var node = new DefaultTreeTable.Node(table);
            matcher.region(indexOfValue, endOfLine);
            for (int ci=0; ci<columns.length; ci++) {
                final boolean found = matcher.find();
                int endOfColumn = found ? matcher.start() : endOfLine;
                indexOfValue   = CharSequences.skipLeadingWhitespaces (text, indexOfValue, endOfColumn);
                int endOfValue = CharSequences.skipTrailingWhitespaces(text, indexOfValue, endOfColumn);
                if (endOfValue > indexOfValue) {
                    final String valueText = text.subSequence(indexOfValue, endOfValue).toString();
                    try {
                        parseValue(node, columns[ci], formats[ci], valueText);
                    } catch (ParseException | ClassCastException e) {
                        pos.setErrorIndex(indexOfValue);                                    // See method javadoc.
                        if (e instanceof ParseException) {
                            indexOfValue += ((ParseException) e).getErrorOffset();
                        }
                        throw new LocalizedParseException(getDisplayLocale(), Errors.Keys.UnparsableStringForClass_2,
                                new Object[] {columns[ci].getElementType(), valueText}, indexOfValue).initCause(e);
                    }
                }
                if (!found) break;
                /*
                 * The end of this column will be the beginning of the next column,
                 * after skipping the last character of the column separator.
                 */
                indexOfValue = matcher.end();
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
                     * node at `indentationLevel == 0` has a null parent and we check that case.
                     */
                    if (--indentationLevel < 0) {
                        pos.setErrorIndex(indexOfLineStart);
                        throw new LocalizedParseException(getDisplayLocale(),
                                Errors.Keys.NodeHasNoParent_1, new Object[] {node}, indexOfLineStart);
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
                                Errors.Keys.NodeHasNoParent_1, new Object[] {node}, indexOfLineStart);
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
     * <p>This work is done in a separated method instead of inlined in the
     * {@code parse(…)} method because of the {@code <V>} parametric value.</p>
     *
     * @param  <V>      the type of values in the given column.
     * @param  node     the node in which to set the value.
     * @param  column   the column in which to set the value.
     * @param  format   the format to use for parsing the value, or {@code null}.
     * @param  text     the textual representation of the value.
     * @throws ParseException if an error occurred while parsing.
     * @throws ClassCastException if the parsed value is not of the expected type.
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int indentation = this.indentation;
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * avoid conflict with the characters expected by {@link TableAppender}.
     *
     * <p>Instances of {@link Writer} are created temporarily before to begin the formatting
     * of a node, and discarded when the formatting is finished.</p>
     */
    private final class Writer extends PropertyFormat {
        /**
         * Combination of {@link #nodeFilter} with other filter that may be specified by the tree table to format.
         * The {@code TreeTable}-specific filter is specified by {@link TreeFormatCustomization}.
         */
        private final Predicate<TreeTable.Node> filter;

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
         * Whether to allows multi-lines cells instead of using Pilcrow character.
         * This is currently supported only if the number of columns is less than 2.
         */
        private final boolean multiLineCells;

        /**
         * The node that have already been formatted. We use this map as a safety against infinite recursion.
         */
        private final Set<TreeTable.Node> recursionGuard;

        /**
         * Creates a new instance which will write to the given appendable.
         *
         * @param  out               where to format the tree.
         * @param  tree              the tree table to format.
         * @param  columns           the columns of the tree table to format.
         * @param  recursionGuard  an initially empty set.
         */
        Writer(final Appendable out, final TreeTable tree, final TableColumn<?>[] columns,
               final Set<TreeTable.Node> recursionGuard)
        {
            super(columns.length >= 2 ? new TableAppender(out, "") : out);
            multiLineCells = (super.out == out);
            this.columns = columns;
            this.formats = getFormats(columns, false);
            this.values  = new Object[columns.length];
            this.isLast  = new boolean[8];
            this.recursionGuard = recursionGuard;

            @SuppressWarnings("LocalVariableHidesMemberVariable")   // To be stored in the field if successful.
            Predicate<TreeTable.Node> filter = nodeFilter;
            if (tree instanceof TreeFormatCustomization) {
                final var custom = (TreeFormatCustomization) tree;
                final Predicate<TreeTable.Node> more = custom.filter();
                if (more != null) {
                    filter = (filter != null) ? more.and(filter) : more;
                }
            }
            this.filter = filter;
            setTabulationExpanded(true);
            setLineSeparator(multiLineCells ? TreeTableFormat.this.getLineSeparator() : " ¶ ");
        }

        /**
         * Returns the locale to use for formatting property value.
         * This method is invoked by {@link PropertyFormat} when needed.
         */
        @Override
        public Locale getLocale() {
            return TreeTableFormat.this.getLocale();
        }

        /**
         * Invoked by {@link PropertyFormat} for formatting a value which has not been recognized as one of
         * the types to be handled in a special way. In particular numbers and dates should be handled here.
         * This method checks for a value-by-value format and should be invoked only in last resort.
         * If a column-wide format was specified by the {@link #columnFormat} field, then that format should
         * have been used by {@link #appendValue(Object)} code in order to produce a more uniform formatting.
         */
        @Override
        protected final String toString(final Object value) {
            final String text;
            final Format format = getFormat(value.getClass());
            if (format instanceof DecimalFormat && Numbers.isFloat(value.getClass())) {
                final double number = ((Number) value).doubleValue();
                if (number != (int) number) {   // Cast to `int` instead of `long` as a way to limit to about 2E9.
                    /*
                     * The default floating point format uses only 3 fraction digits. We adjust that to the number
                     * of digits required by the number to format. We do that only if no NumberFormat was inferred
                     * for the whole column (in order to keep column format uniform).  We use enough precision for
                     * all fraction digits except the last 2, in order to let DecimalFormat round the number.
                     */
                    if (adaptableFormat == null) {
                        adaptableFormat = (DecimalFormat) format.clone();
                        defaultPattern = adaptableFormat.toPattern();
                    }
                    final int nf = DecimalFunctions.fractionDigitsForValue(number);
                    final boolean preferScientificNotation = (nf > 20 || nf < 7);       // == (value < 1E-4 || value > 1E+9)
                    if (preferScientificNotation != usingScientificNotation) {
                        usingScientificNotation = preferScientificNotation;
                        adaptableFormat.applyPattern(preferScientificNotation ? "0.0############E0" : defaultPattern);
                    }
                    if (!preferScientificNotation) {
                        adaptableFormat.setMaximumFractionDigits(nf - 2);       // All significand fraction digits except last two.
                    }
                    text = adaptableFormat.format(value);
                } else {
                    text = format.format(value);
                }
            } else {
                text = (format != null) ? format.format(value) : value.toString();
            }
            return text;
        }

        /**
         * Appends the string representation of the given node and all its children.
         * This method invokes itself recursively.
         *
         * @param  node   the node to format.
         * @param  level  indentation level. The first level is 0.
         */
        final void format(final TreeTable.Node node, final int level) throws IOException {
            /*
             * Draw the lines of the tree in the left margin for current row.
             */
            for (int i=0; i<level; i++) {
                out.append(getTreeSymbols(i != level-1, isLast[i]));
            }
            /*
             * Fetch the values to write in current row, but do not write them now. We fetch values in advance in order
             * to detect trailing null values, so we can avoid formatting trailing blank spaces. Note that a null value
             * may be followed by a non-null value, which is why we need to check all of them before to know how many
             * columns to omit.
             */
            for (int i=0; i<columns.length; i++) {
                values[i] = node.getValue(columns[i]);
            }
            int n = values.length - 1;
            if (omitTrailingNulls) {
                while (n > 0 && values[n] == null) n--;
            }
            /*
             * Format the values that we fetched in above loop.
             */
            for (int i=0; i<=n; i++) {
                if (i != 0) {
                    // We have a TableAppender instance if and only if there is 2 or more columns.
                    writeColumnSeparator(i, (TableAppender) out);
                }
                columnFormat = formats[i];
                appendValue(values[i]);
                clear();
            }
            out.append(lineSeparator);
            if (level >= isLast.length) {
                isLast = Arrays.copyOf(isLast, level*2);
            }
            /*
             * Format the children only if we do not detect an infinite recursion. Our recursion detection
             * algorithm assumes that the Node.equals(Object) method has been implemented as specified in its javadoc.
             * In particular, the implementation may compare the values and children but shall not compare the parent.
             *
             * We skip the check for the particular case of DefaultTreeTable.Node implementation because it performs
             * a real check of values and children, which is a little bit costly and known to be unnecessary in that
             * particular case.
             */
            final boolean omitCheck = node.getClass().isAnnotationPresent(Acyclic.class);
            if (omitCheck || recursionGuard.add(node)) {
                boolean needLineSeparator = multiLineCells;
                final String lineSeparator = needLineSeparator ? getLineSeparator() : null;
                final Iterator<? extends TreeTable.Node> it = node.getChildren().iterator();
                TreeTable.Node next = next(it);
                while (next != null) {
                    final TreeTable.Node child = next;
                    next = next(it);
                    needLineSeparator |= (isLast[level] = (next == null));
                    if (needLineSeparator && lineSeparator != null) {
                        setLineSeparator(lineSeparator + getTreeSymbols(true, isLast[level]));
                    }
                    format(child, level+1);                     // `isLast` must be set before to call this method.
                }
                if (lineSeparator != null) {
                    setLineSeparator(lineSeparator);            // Restore previous state.
                }
                if (!omitCheck && !recursionGuard.remove(node)) {
                    /*
                     * Assuming that Node.hashCode() and Node.equals(Object) implementation are not broken,
                     * this exception may happen only if the node content changed during this method execution.
                     */
                    throw new ConcurrentModificationException();
                }
            } else {
                /*
                 * Detected a recursion. Format "(cycle omitted)" just below the node.
                 */
                for (int i=0; i<level; i++) {
                    out.append(getTreeSymbols(true, isLast[i]));
                }
                final Locale locale = getDisplayLocale();
                out.append(treeBlank).append('(').append(Vocabulary.forLocale(locale)
                   .getString(Vocabulary.Keys.CycleOmitted).toLowerCase(locale))
                   .append(')').append(lineSeparator);
            }
        }

        /**
         * Returns the next filtered element from the given iterator, or {@code null} if none.
         * The filter applied by this method combines {@link #getNodeFilter()} with the filter
         * returned by {@link TreeFormatCustomization#filter()}.
         */
        private TreeTable.Node next(final Iterator<? extends TreeTable.Node> it) {
            while (it.hasNext()) {
                final TreeTable.Node next = it.next();
                if (next != null) {
                    if (filter == null || filter.test(next)) {
                        return next;
                    }
                }
            }
            return null;
        }
   }

    /**
     * Writes a graphical representation of the specified tree table in the given stream or buffer.
     * This method iterates recursively over all {@linkplain TreeTable.Node#getChildren() children}.
     * For each {@linkplain #getColumns() column to format} in each node, this method gets a textual
     * representation of the {@linkplain TreeTable.Node#getValue(TableColumn) value in that column}
     * using the formatter obtained by a call to {@link #getFormat(Class)}.
     *
     * @param  tree        the tree to format.
     * @param  toAppendTo  where to format the tree.
     * @throws IOException if an error occurred while writing to the given appendable.
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
            columns = tree.getColumns().toArray(TableColumn<?>[]::new);
        }
        if (recursionGuard == null) {
            recursionGuard = new HashSet<>();
        }
        try {
            final var out = new Writer(toAppendTo, tree, columns, recursionGuard);
            out.format(tree.getRoot(), 0);
            out.flush();
        } finally {
            recursionGuard.clear();
        }
    }

    /**
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked the first time that a format is needed for the given type.
     * Subclasses can override this method if they want to configure the way dates, numbers
     * or other objects are formatted.
     * See {@linkplain org.apache.sis.io.CompoundFormat#createFormat(Class) parent class documentation}
     * for more information.
     *
     * <p>The implementation in {@code TreeTableFormat} differs from the default implementation
     * in the following aspects:</p>
     * <ul>
     *   <li>{@code UnitFormat} uses {@link UnitFormat.Style#NAME}.</li>
     * </ul>
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type, or {@code null} if none.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        final Format format = super.createFormat(valueType);
        if (format instanceof UnitFormat) {
            ((UnitFormat) format).setStyle(UnitFormat.Style.NAME);
        }
        return format;
    }

    /**
     * Writes characters between columns. The default implementation applies the configuration
     * specified by {@link #setColumnSeparatorPattern(String)} as below:
     *
     * <blockquote><code>
     * out.append({@linkplain #beforeFill beforeFill});
     * out.nextColumn({@linkplain #fillCharacter fillCharacter});
     * out.append({@linkplain #columnSeparator columnSeparator});
     * </code></blockquote>
     *
     * The output with default values is like below:
     *
     * <pre class="text">
     *   root
     *     └─column0…… column1…… column2…… column3</pre>
     *
     * Subclasses can override this method if different column separators are desired.
     * Note however that doing so may prevent the {@link #parse parse(…)} method to work.
     *
     * @param  nextColumn  zero-based index of the column to be written after the separator.
     * @param  out         where to write the column separator.
     *
     * @see TableAppender#nextColumn(char)
     *
     * @since 1.0
     */
    protected void writeColumnSeparator(final int nextColumn, final TableAppender out) {
        out.append(beforeFill);
        out.nextColumn(fillCharacter);
        out.append(columnSeparator);
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public TreeTableFormat clone() {
        final var c = (TreeTableFormat) super.clone();
        c.recursionGuard = null;
        return c;
    }
}
