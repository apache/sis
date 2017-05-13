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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.internal.metadata.sql.TestDatabase;


/**
 * Rewrites the {@code INSERT TO ...} statements in a SQL script in a more compact form.
 * This class is used only for updating the SQL scripts used by Apache SIS for the EPSG
 * dataset when a newer release of the EPSG dataset is available.
 * The steps to follow are documented in the {@code package.html} file.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final class DataScriptFormatter extends ScriptRunner {
    /**
     * Compacts the {@code Data.sql} file provided by EPSG. This method expects two arguments.
     * The first argument is the file of the SQL script to read, which must exist.
     * The second argument is the file where to write the compacted SQL script,
     * which will be overwritten without warning if it exists.
     * The values of those arguments are typically:
     *
     * <ol>
     *   <li>{@code EPSG_vX.mdb_Data_PostgreSQL.sql}</li>
     *   <li>{@code core/sis-referencing/src/main/resources/org/apache/sis/referencing/factory/sql/Data.sql}</li>
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
        final DataSource ds = TestDatabase.create("dummy");
        try (Connection c = ds.getConnection()) {
            final DataScriptFormatter f = new DataScriptFormatter(c);
            f.run(new File(arguments[0]), new File(arguments[1]));
        } finally {
            TestDatabase.drop(ds);
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
     * Index (in reversal order) of columns to change from type SMALLINT to type BOOLEAN.
     * Index 0 is the last columns, index 1 is the column before the last, <i>etc</i>.
     * We use the reverse order because most boolean columns in the EPSG dataset are last.
     */
    private int[] booleanColumnIndices;

    /**
     * The {@link #booleanColumnIndices} value for each table.
     */
    private final Map<String,int[]> booleanColumnIndicesForTables;

    /**
     * Creates a new instance.
     *
     * @param  c  a dummy connection. Will be used for fetching metadata.
     * @throws SQLException if an error occurred while fetching metadata.
     */
    private DataScriptFormatter(final Connection c) throws SQLException {
        super(c, Integer.MAX_VALUE);
        final Map<String,int[]> m = new HashMap<>();
        m.put("epsg_alias",                     new int[] {   });
        m.put("epsg_area",                      new int[] {0  });
        m.put("epsg_change",                    new int[] {   });
        m.put("epsg_coordinateaxis",            new int[] {   });
        m.put("epsg_coordinateaxisname",        new int[] {0  });
        m.put("epsg_coordinatereferencesystem", new int[] {0,1});
        m.put("epsg_coordinatesystem",          new int[] {0  });
        m.put("epsg_coordoperation",            new int[] {0,1});
        m.put("epsg_coordoperationmethod",      new int[] {0,8});
        m.put("epsg_coordoperationparam",       new int[] {0  });
        m.put("epsg_coordoperationparamusage",  new int[] {0  });
        m.put("epsg_coordoperationparamvalue",  new int[] {   });
        m.put("epsg_coordoperationpath",        new int[] {   });
        m.put("epsg_datum",                     new int[] {0  });
        m.put("epsg_deprecation",               new int[] {   });
        m.put("epsg_ellipsoid",                 new int[] {0,6});
        m.put("epsg_namingsystem",              new int[] {0  });
        m.put("epsg_primemeridian",             new int[] {0  });
        m.put("epsg_supersession",              new int[] {   });
        m.put("epsg_unitofmeasure",             new int[] {0  });
        m.put("epsg_versionhistory",            new int[] {   });
        booleanColumnIndicesForTables = m;
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
    private void run(final File inputFile, final File outputFile) throws SQLException, IOException {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("Input and output files are the same.");
        }
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.ISO_8859_1));
        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.ISO_8859_1))) {
            out.write("---\n" +
                      "---    Copyright International Association of Oil and Gas Producers (IOGP)\n" +
                      "---    See  http://www.epsg.org/TermsOfUse  (a copy is in ./LICENSE.txt).\n" +
                      "---\n" +
                      "---    This file has been reformatted (without any change in the data) for the needs of Apache SIS project.\n" +
                      "---    See org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter.\n" +
                      "---\n" +
                      "\n");
            run(inputFile.getName(), in);
        } finally {
            out.close();
            out = null;
        }
    }

    /**
     * EPSG scripts version 8.9 seems to have 2 errors where the {@code OBJECT_TABLE_NAME} column contains
     * {@code "AxisName"} instead of {@code "Coordinate Axis Name"}.
     */
    @Override
    @Workaround(library="EPSG", version="8.9")
    protected void editText(final StringBuilder sql, int lower, final int upper) {
        if (upper - lower == 10 && CharSequences.regionMatches(sql, ++lower, "AxisName")) {
            sql.replace(lower, upper-1, "Coordinate Axis Name");
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
                line = CharSequences.trimWhitespaces(line, insertStatement.length(), line.length()).toString();
                line = replaceIntegerByBoolean(line);
                line = removeUselessExponents(line);
                out.append(",\n");      // Really want Unix EOL, not the platform-specific one.
                writeValues(line);
                return 1;
            }
            // Previous instruction was the last INSERT INTO for a given table.
            // We now have a new instruction. Append the pending cariage return.
            out.append(";\n");
        }
        if (line.startsWith(INSERT_INTO)) {
            int valuesStart = line.indexOf(VALUES, INSERT_INTO.length());
            if (valuesStart < 0) {
                throw new SQLException("This simple program wants VALUES on the same line than INSERT INTO.");
            }
            final String table = CharSequences.trimWhitespaces(line, INSERT_INTO.length(), valuesStart).toString();
            booleanColumnIndices = booleanColumnIndicesForTables.get(table);
            /*
             * We are beginning insertions in a new table.
             */
            valuesStart += VALUES.length();     // Move to the end of "VALUES".
            insertStatement = CharSequences.trimWhitespaces(line, 0, valuesStart).toString();
            line = CharSequences.trimWhitespaces(line, insertStatement.length(), line.length()).toString();
            line = replaceIntegerByBoolean(line);
            line = removeUselessExponents(line);
            out.append(insertStatement);
            out.append('\n');
            writeValues(line);
            return 1;
        }
        insertStatement = null;
        if (!omit(line)) {
            out.append(line);
            out.append(";\n");
        }
        return 0;
    }

    /**
     * Writes the values after an {@code INSERT INTO "Table" VALUES} expression.
     * This method tries to remove extra spaces before ( and after ) for producing a more compact file.
     */
    private void writeValues(final String values) throws IOException {
        if (values.startsWith("(") && values.endsWith(")")) {
            out.append('(');
            out.append(CharSequences.trimWhitespaces(values, 1, values.length() - 1));
            out.append(')');
        } else {
            out.append(values);
        }
    }

    /**
     * Replaces the last {@code SMALLINT} types by {@code BOOLEAN}.
     * This is for consistency with the table type documented in the class javadoc.
     */
    private String replaceIntegerByBoolean(final String line) throws SQLException {
        final StringBuilder buffer = new StringBuilder(line);
        int end = CharSequences.skipTrailingWhitespaces(buffer, 0, buffer.length());
        if (buffer.codePointBefore(end) == ')') end--;
        for (int n=0, columnIndex=0; n < booleanColumnIndices.length; columnIndex++) {
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
            if (columnIndex == booleanColumnIndices[n]) {
                String value = CharSequences.trimWhitespaces(buffer, start, end).toString();
                if (value.equals("0") || value.equalsIgnoreCase("'No'")) {
                    value = "false";
                } else if (value.equals("1") || value.equalsIgnoreCase("'Yes'")) {
                    value = "true";
                } else if (value.equalsIgnoreCase("Null") || value.equals("''")) {
                    value = "Null";
                } else {
                    throw new SQLException("Unexpected boolean value \"" + value + "\" at position " + start + " in:\n" + line);
                }
                buffer.replace(start, end, value);
                n++;
            }
            end = CharSequences.skipTrailingWhitespaces(buffer, 0, start - 1);
        }
        return buffer.toString();
    }

    /**
     * For private usage by the following method only.
     */
    private static final Pattern uselessExponentPattern =
            Pattern.compile("([\\(\\,]\\-?\\d+\\.\\d+)E[\\+\\-]?0+([\\,\\)])");

    /**
     * Removes the useless "E0" exponents after floating point numbers.
     */
    @SuppressWarnings("null")
    private static String removeUselessExponents(String line) {
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
