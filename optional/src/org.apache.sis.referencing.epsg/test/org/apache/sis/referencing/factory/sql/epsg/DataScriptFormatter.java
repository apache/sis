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
package org.apache.sis.referencing.factory.sql.epsg;

import java.util.Map;
import java.util.HashMap;
import java.util.function.UnaryOperator;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.URLs;
import org.apache.sis.metadata.sql.privy.SQLBuilder;
import org.apache.sis.metadata.sql.privy.ScriptRunner;

// Test dependencies
import org.apache.sis.metadata.sql.TestDatabase;


/**
 * Rewrites the {@code INSERT TO ...} statements in a SQL script in a more compact form.
 * This class is used only for updating the <abbr>SQL</abbr> scripts used by Apache SIS
 * for the <abbr>EPSG</abbr> dataset when a newer release of that dataset is available.
 * The steps to follow are documented in the {@code README.md} file.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DataScriptFormatter extends ScriptRunner {
    /**
     * Compacts the {@code *Data_Script.sql} file provided by <abbr>EPSG</abbr>.
     * This method expects two arguments:
     *
     * <ol>
     *   <li>The file of the <abbr>SQL</abbr> script to convert, which must exist.</li>
     *   <li>The file where to write the compacted script, which will be overwritten without warning if it exists.</li>
     * </ol>
     *
     * The values of those arguments are typically:
     *
     * <ol>
     *   <li>{@code $EPSG_SCRIPTS/PostgreSQL_Data_Script.sql}</li>
     *   <li>{@code sis-epsg/src/main/resources/org/apache/sis/referencing/factory/sql/epsg/Data.sql}</li>
     * </ol>
     *
     * @param  arguments  the source files and the destination file.
     * @throws Exception if an error occurred while reading of writing the file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            System.err.println("Expected two arguments: source SQL file and target SQL file.");
            return;
        }
        try (TestDatabase db = TestDatabase.create("dummy");
             Connection c = db.source.getConnection())
        {
            final var formatter = new DataScriptFormatter(c);
            formatter.run(Path.of(arguments[0]), Path.of(arguments[1]));
        }
    }

    /**
     * The {@value} keywords.
     */
    private static final String INSERT_INTO = "INSERT INTO";

    /**
     * The {@value} keywords.
     */
    private static final String VALUES = "VALUES";

    /**
     * The output stream, or {@code null} if closed or not yet created.
     */
    private Writer out;

    /**
     * The {@code INSERT INTO "Table"} statement currently executing.
     * Used in order to detect when the script start inserting values in a different table.
     */
    private String insertStatement;

    /**
     * Indices (in reversal order) of columns to change from type SMALLINT to type BOOLEAN.
     * Index 0 is the last column, index 1 is the column before the last, <i>etc</i>.
     * We use the reverse order because most Boolean columns in the EPSG dataset are last.
     */
    private int[] booleanColumnIndices;

    /**
     * The {@link #booleanColumnIndices} value for each table.
     */
    private final Map<String,int[]> booleanColumnIndicesForTables;

    /**
     * Indices (in reversal order) of columns to reformat as a double value.
     * The EPSG database contains values like {@code -0.000000000000000000000003689471323},
     * while is either not parsed by Derby because too long (Derby 10.14 documents a limit
     * of 30 characters for floating-point constants) or parsed as 0. To workaround such
     * limitation, we reformat above value as {@code -3.689471323E-24}.
     */
    private int[] doubleColumnIndices;

    /**
     * The {@link #doubleColumnIndices} value of each table for which we want to reformat the value.
     * We do not reformat the {@code change_id} columns since they are more like character strings.
     * We do not reformat east/west/north/south bounds or {@code greenwich_longitude} since their
     * values are close to integers, or {@code semi_major_axis}, {@code semi_minor_axis} and
     * {@code inv_flattening} for similar reasons.
     */
    private final Map<String,int[]> doubleColumnIndicesForTables;

    /**
     * Creates a new instance.
     *
     * @param  c  a dummy connection. Will be used for fetching metadata.
     * @throws SQLException if an error occurred while fetching metadata.
     */
    private DataScriptFormatter(final Connection c) throws SQLException {
        super(c, Integer.MAX_VALUE);
        final int[]    lastColumn  = new int[] {0  };
        final int[] twoLastColumns = new int[] {0,1};
        final var m = new HashMap<String,int[]>();
        m.put("epsg_area",                         lastColumn );
        m.put("epsg_coordinateaxisname",           lastColumn );
        m.put("epsg_coordinatereferencesystem", twoLastColumns);
        m.put("epsg_coordinatesystem",             lastColumn );
        m.put("epsg_coordoperation",            twoLastColumns);
        m.put("epsg_coordoperationmethod",     new int[] {0,8});
        m.put("epsg_coordoperationparam",          lastColumn );
        m.put("epsg_coordoperationparamusage",     lastColumn );
        m.put("epsg_datum",                        lastColumn );
        m.put("epsg_ellipsoid",                new int[] {0,6});
        m.put("epsg_namingsystem",                 lastColumn );
        m.put("epsg_primemeridian",                lastColumn );
        m.put("epsg_unitofmeasure",                lastColumn );
        booleanColumnIndicesForTables = m;
        doubleColumnIndicesForTables = Map.of("epsg_coordoperationparamvalue", new int[] {2});
    }

    /**
     * Returns {@code true} if the given line should be omitted from the script.
     *
     * @param  line  the line, without trailing {@code ';'}.
     * @return {@code true} if the line should be omitted.
     */
    private static boolean omit(final String line) {
        // We omit the following line because we changed the type from VARCHAR to DATE.
        return line.startsWith("UPDATE epsg_datum SET realization_epoch = replace(realization_epoch, CHR(182), CHR(10))");
    }

    /**
     * Compacts the given file.
     *
     * @param  inputFile    the input file where to read the SQL statements to compact.
     * @param  outputFile   the output file where to write the compacted SQL statements.
     * @throws IOException  if an I/O operation failed.
     * @throws SQLException should never happen.
     */
    private void run(final Path inputFile, final Path outputFile) throws SQLException, IOException {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("Input and output files are the same.");
        }
        out = Files.newBufferedWriter(outputFile);
        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(Files.newInputStream(inputFile), StandardCharsets.UTF_8))) {
            out.write("---\n" +
                      "---    Copyright International Association of Oil and Gas Producers (IOGP)\n" +
                      "---    See  " + URLs.EPSG_LICENSE + "  (a copy is in ./LICENSE.txt).\n" +
                      "---\n" +
                      "---    This file has been reformatted (without any change in the data) for the needs of Apache SIS project.\n" +
                      "---    See org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter.\n" +
                      "---\n" +
                      "\n");
            run(inputFile.getFileName().toString(), in);
        } finally {
            out.close();
            out = null;
        }
    }

    /**
     * EPSG scripts version 8.9 seems to have 2 errors where the {@code OBJECT_TABLE_NAME} column contains
     * {@code "AxisName"} instead of {@code "Coordinate Axis Name"}. Furthermore, the version number noted
     * in the history table is a copy-and-paste error.
     */
    @Override
    @Workaround(library="EPSG", version="8.9")
    protected void editText(final StringBuilder sql, int lower, int upper) {
        final String table;         // Name of the table where to replace a value.
        final String before;        // String that must exist before the value to replace, or null if none.
        final String oldValue;      // The old value to replace.
        final String newValue;      // The new value.
        switch (upper - lower) {    // Optimization for reducing the number of comparisons.
            default: return;
            case 10: {
                table    = "epsg_deprecation";
                before   = null;
                oldValue = "AxisName";
                newValue = "Coordinate Axis Name";
                break;
            }
            case 38: {
                table    = "epsg_versionhistory";
                before   = "'8.9'";
                oldValue = "Version 8.8 full release of Dataset.";
                newValue = "Version 8.9 full release of Dataset.";
                break;
            }
        }
        if (CharSequences.regionMatches(sql, ++lower, oldValue)) {
            final int s = CharSequences.skipLeadingWhitespaces(sql, 0, lower);
            if (CharSequences.regionMatches(sql, s, SQLBuilder.INSERT + table + " VALUES")) {
                if (--upper - lower != oldValue.length()) {
                    throw new AssertionError("Unexpected length");
                }
                if (before != null) {
                    final int i = sql.indexOf(before);
                    if (i < 0 || i >= lower) return;
                }
                sql.replace(lower, upper, newValue);
            }
        }
    }

    /**
     * Makes sure that {@link #execute(StringBuilder)} is invoked for every line. Whether the SQL statement
     * is supported or not is irrelevant for this method since we do not know yet what will be the database
     * engine; we just copy the SQL statements in a file without executing them.
     *
     * @return {@code true}.
     */
    @Override
    protected boolean isSupported(final CharSequence sql) {
        return true;
    }

    /**
     * "Executes" the given SQL statement. In the context of this {@code EPSGDataWriter} class,
     * executing a SQL statement means compacting it and writing it to the output file.
     *
     * @param  sql  the SQL statement to compact.
     * @return the number of rows added.
     * @throws IOException if an I/O operation failed.
     * @throws SQLException if a syntax error happens.
     */
    @Override
    protected int execute(final StringBuilder sql) throws IOException, SQLException {
        removeLF(sql);
        String line = CharSequences.trimWhitespaces(sql).toString();
        if (line.startsWith("UPDATE ")) {
            /*
             * Some EPSG tables have a "table_name" field which will contain the names of other EPSG tables.
             * In the EPSG scripts, the values are initially the table names used in the MS-Access database.
             * Then the MS-Access table names are replaced by statements like below:
             *
             *    UPDATE epsg_alias SET object_table_name = 'epsg_coordinateaxis' WHERE object_table_name = 'Coordinate Axis';
             *    UPDATE epsg_deprecation SET object_table_name = 'epsg_alias' WHERE object_table_name = 'Alias';
             *    etc.
             *
             * For Apache SIS, we keep the original table names as defined in MS-Access database,
             * for consistency with the table names that we actually use in our EPSG schema.
             */
            if (line.contains("object_table_name")) {
                return 0;
            }
            /*
             * Following statements do not make sense anymore on enumerated or boolean values:
             *
             *    UPDATE epsg_coordinatereferencesystem SET coord_ref_sys_kind = replace(coord_ref_sys_kind, CHR(182), CHR(10));
             *    UPDATE epsg_coordinatesystem SET coord_sys_type = replace(coord_sys_type, CHR(182), CHR(10));
             *    UPDATE epsg_datum SET datum_type = replace(datum_type, CHR(182), CHR(10));
             *    UPDATE epsg_coordoperationparamusage SET param_sign_reversal = replace(param_sign_reversal, CHR(182), CHR(10))
             */
            if (line.contains("replace")) {
                if (line.contains("param_sign_reversal") || line.contains("coord_ref_sys_kind")
                        || line.contains("coord_sys_type") || line.contains("datum_type"))
                {
                    return 0;
                }
            }
        }
        if (insertStatement != null) {
            if (line.startsWith(insertStatement)) {
                // The previous instruction was already an INSERT INTO the same table.
                out.append(",\n");      // Really want Unix EOL, not the platform-specific one.
                writeValues(editInsertValues(line));
                return 1;
            }
            // Previous instruction was the last INSERT INTO for a given table.
            // We now have a new instruction. Append the pending cariage return.
            out.append(";\n");
        }
        if (line.startsWith(INSERT_INTO)) {
            int valuesStart = line.indexOf(VALUES, INSERT_INTO.length());
            if (valuesStart < 0) {
                throw new SQLException("This simple program wants VALUES on the same line as INSERT INTO.");
            }
            final String table = CharSequences.trimWhitespaces(line, INSERT_INTO.length(), valuesStart).toString();
            booleanColumnIndices = booleanColumnIndicesForTables.getOrDefault(table, ArraysExt.EMPTY_INT);
            doubleColumnIndices  =  doubleColumnIndicesForTables.getOrDefault(table, ArraysExt.EMPTY_INT);
            /*
             * We are beginning insertions in a new table.
             */
            valuesStart += VALUES.length();     // Move to the end of "VALUES".
            insertStatement = CharSequences.trimWhitespaces(line, 0, valuesStart).toString();
            out.append(insertStatement).append('\n');
            writeValues(editInsertValues(line));
            return 1;
        }
        insertStatement = null;
        if (!omit(line)) {
            out.append(line).append(";\n");
        }
        return 0;
    }

    /**
     * Writes the values after an {@code INSERT INTO "Table" VALUES} expression.
     * This method tries to remove extra spaces before ( and after ) for producing a more compact file.
     */
    private void writeValues(final String values) throws IOException {
        if (values.startsWith("(") && values.endsWith(")")) {
            out.append('(').append(CharSequences.trimWhitespaces(values, 1, values.length() - 1)).append(')');
        } else {
            out.append(values);
        }
    }

    /**
     * Modifies the given {@code INSERT INTO table VALUE (…)} line before to write it.
     * The given line is only the {@code VALUE (…)} part for a single entry.
     * The modifications applied on entry values can be a change of integer types to
     * boolean types, and rewrite of some floating point values.
     */
    private String editInsertValues(String line) throws SQLException {
        line = CharSequences.trimWhitespaces(line, insertStatement.length(), line.length()).toString();
        line = editColumns(booleanColumnIndices, line, DataScriptFormatter::replaceIntegerByBoolean);
        line = editColumns(doubleColumnIndices,  line, DataScriptFormatter::reformatFloatingPoints);
        line = removeUselessExponents(line);
        return line;
    }

    /**
     * Modifies the content of columns identified by the given indices.
     *
     * @param  indices    indices (in reversal order) of columns to edit. Index 0 is the last column,
     *                    index 1 is the column before the last, <i>etc</i>.
     * @param  line       the line to modify.
     * @param  converter  the transformation to apply columns identified by the given indices.
     *                    A {@code null} return value means that the conversion cannot be performed.
     * @return the modified line.
     */
    private static String editColumns(final int[] indices, final String line, final UnaryOperator<String> converter)
            throws SQLException
    {
        final var buffer = new StringBuilder(line);
        int end = CharSequences.skipTrailingWhitespaces(buffer, 0, buffer.length());
        if (buffer.codePointBefore(end) == ')') end--;
        for (int n=0, columnIndex=0; n < indices.length; columnIndex++) {
            int start = end;
            for (int c; (c = buffer.codePointBefore(start)) != ',';) {
                start -= Character.charCount(c);
                if (c == '\'') {
                    while (true) {
                        c = buffer.codePointBefore(start);
                        start -= Character.charCount(c);
                        if (c == '\'') {
                            if (buffer.codePointBefore(start) != '\'') {
                                break;
                            }
                            start--;
                        }
                    }
                }
            }
            if (columnIndex == indices[n]) {
                final String value = CharSequences.trimWhitespaces(buffer, start, end).toString();
                final String c = converter.apply(value);
                if (value == null) {
                    throw new SQLException("Unexpected value \"" + value + "\" at position " + start + " in:\n" + line);
                }
                buffer.replace(start, end, c);
                n++;
            }
            end = CharSequences.skipTrailingWhitespaces(buffer, 0, start - 1);
        }
        return buffer.toString();
    }

    /**
     * Replaces the last {@code SMALLINT} types by {@code BOOLEAN}.
     * This is for consistency with the table type documented in the class javadoc.
     */
    private static String replaceIntegerByBoolean(String value) {
        if (value.equals("0") || value.equalsIgnoreCase("'No'")) {
            value = "false";
        } else if (value.equals("1") || value.equalsIgnoreCase("'Yes'")) {
            value = "true";
        } else if (value.equalsIgnoreCase("Null") || value.equals("''")) {
            value = "Null";
        } else {
            value = null;
        }
        return value;
    }

    /**
     * Reformats the given floating point number. This is used for replacing for example
     * {@code -0.000000000000000000000003689471323} by {@code -3.689471323E-24}.
     */
    private static String reformatFloatingPoints(String value) {
        if (!value.equalsIgnoreCase("Null")) {
            value = Double.toString(Double.parseDouble(value));
            value = CharSequences.trimFractionalPart(value).toString();
        }
        return value;
    }

    /**
     * For private usage by the following method only.
     */
    private final Pattern uselessExponentPattern =
            Pattern.compile("([\\(\\,]\\-?\\d+\\.\\d+)E[\\+\\-]?0+([\\,\\)])");

    /**
     * Removes the useless "E0" exponents after floating point numbers.
     */
    private String removeUselessExponents(String line) {
        StringBuilder cleaned = null;
        final Matcher matcher = uselessExponentPattern.matcher(line);
        while (true) {
            int lastIndex = 0;
            while (matcher.find()) {
                // Make sure this is not a quoted text.
                boolean quoted = false;
                for (int i=matcher.start(); (i=line.lastIndexOf('\'', i-1)) >= 0;) {
                    if (i == 0 || line.charAt(i-1) != '\\') {
                        quoted = !quoted;
                    }
                }
                if (!quoted) {
                    // Found a number outside quotes. Replace.
                    if (cleaned == null) {
                        cleaned = new StringBuilder();
                    }
                    cleaned.append(line, lastIndex, matcher.end(1));
                    lastIndex = matcher.end();
                    cleaned.append(line, matcher.start(2), lastIndex);
                }
            }
            if (lastIndex == 0) {
                return line;
            }
            cleaned.append(line, lastIndex, line.length());
            line = cleaned.toString();
            matcher.reset(line);
            cleaned.setLength(0);
        }
    }

    /**
     * Reformats a multi-line text as a single line text. For each occurrence of line feed
     * (the {@code '\n'} character) found in the given buffer, this method performs the following steps:
     *
     * <ol>
     *   <li>Remove the line feed character and the {@linkplain Character#isWhitespace(char) white spaces} around them.</li>
     *   <li>If the last character before the line feed and the first character after the line feed are both
     *       {@linkplain Character#isLetterOrDigit(char) letter or digit}, then a space is inserted between them.
     *       Otherwise they will be no space.</li>
     * </ol>
     *
     * This method is provided for {@link #execute(StringBuilder)} implementations, in order to "compress"
     * a multi-lines SQL statement on a single line before further processing by the caller.
     *
     * <p><b>Note:</b> current version does not use codepoint API
     * on the assumption that it is not needed for EPSG's SQL files.</p>
     *
     * @param  buffer  the string in which to perform the removal.
     */
    static void removeLF(final StringBuilder buffer) {
        int i = buffer.length();
        while ((i = buffer.lastIndexOf("\n", i)) >= 0) {
            final int length = buffer.length();
            int nld = 0;
            int upper = i;
            while (++upper < length) {
                final char c = buffer.charAt(upper);
                if (!Character.isWhitespace(c)) {
                    if (Character.isLetterOrDigit(c)) {
                        nld++;
                    }
                    break;
                }
            }
            while (i != 0) {
                final char c = buffer.charAt(--i);
                if (!Character.isWhitespace(c)) {
                    if (Character.isLetterOrDigit(c)) {
                        nld++;
                    }
                    i++;
                    break;
                }
            }
            if (nld == 2) {
                upper--;
            }
            buffer.delete(i, upper);
            if (nld == 2) {
                buffer.setCharAt(i, ' ');
            }
        }
    }
}
