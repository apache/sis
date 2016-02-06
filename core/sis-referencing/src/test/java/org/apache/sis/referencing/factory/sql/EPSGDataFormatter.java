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
package org.apache.sis.referencing.factory.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.internal.metadata.sql.TestDatabase;


/**
 * Rewrites the {@code INSERT TO ...} statements in a SQL script in a more compact form.
 * This class is used only for updating the SQL scripts used by Apache SIS for the EPSG
 * dataset when a newer release of the EPSG dataset is available. Steps to follow:
 *
 * <ol class="verbose">
 *   <li><p>Download the latest SQL scripts for PostgreSQL from <a href="http://www.epsg.org">http://www.epsg.org</a>.</p></li>
 *   <li><p>Unzip in the directory of your choice (replace "8.8" by the appropriate version number in the ZIP filename),
 *          and remember the path to that directory:</p>
 *
 *          {@preformat text
 *            unzip epsg-v8_8sql-PostgreSQL.zip
 *            cd epsg-v8_8sql-PostgreSQL
 *            export EPSG_SCRIPTS=$PWD
 *          }
 *   </li>
 *
 *   <li><p>Move to the directory which contains the Apache SIS scripts:</p>
 *
 *         {@preformat text
 *           cd <SIS_HOME>/core/sis-referencing/src/main/resources/org/apache/sis/referencing/factory/sql/
 *         }
 *   </li>
 *
 *   <li><p>Overwrite {@code Tables.sql} and {@code FKeys.sql} with the new SQL scripts
 *          (replace "8.8" by the appropriate version number in the commands show below).
 *          Do not overwrite {@code Data.sql} and {@code Indexes.sql}:</p>
 *
 *          {@preformat text
 *            cp $EPSG_SCRIPTS/EPSG_v8_8.mdb_Tables_PostgreSQL.sql Tables.sql
 *            cp $EPSG_SCRIPTS/EPSG_v8_8.mdb_FKeys_PostgreSQL.sql  FKeys.sql
 *          }
 *   </li>
 *
 *   <li><p>Open the {@code Tables.sql} file for edition:</p>
 *     <ul>
 *       <li>In the statement creating the {@code epsg_datum} table,
 *           change the type of the {@code realization_epoch} column to {@code SMALLINT}.</li>
 *       <li>In the statement creating the {@code coordinateaxis} table,
 *           add the {@code NOT NULL} constraint to the {@code coord_axis_code} column.</li>
 *       <li>In the statement creating the {@code change} table,
 *           remove the {@code UNIQUE} constraint on the {@code change_id} column
 *           and add a {@code CONSTRAINT pk_change PRIMARY KEY (change_id)} line instead.</li>
 *       <li>Suppress trailing spaces, format the statement as in the previous version
 *           for reducing the amount of difference and save.</li>
 *     </ul>
 *     <p>Usually this results in no change at all compared to the previous script (ignoring white spaces),
 *        in which case the maintainer can just revert the changes in order to preserve the formatting.</p>
 *   </li>
 *
 *   <li><p>Open the {@code FKeys.sql} file for edition:</p>
 *     <ul>
 *       <li>At the end of all {@code ALTER TABLE} statement,
 *           append {@code ON UPDATE RESTRICT ON DELETE RESTRICT}.</li>
 *       <li>suppress trailing spaces and save.</li>
 *     </ul>
 *     <p>In most cases this results in unmodified {@code FKeys.sql} file compared to the previous version.</p>
 *   </li>
 *
 *   <li><p>Run the {@code main} method of this class.</p></li>
 *
 *   <li><p>Upgrade the {@code FACTORY.VERSION} value defined in the
 *          {@code org.apache.sis.referencing.report.CoordinateReferenceSystems} class.</p></li>
 * </ol>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class EPSGDataFormatter extends ScriptRunner {
    /**
     * Compacts the {@code Data.sql} file provided by EPSG. This method expects two arguments.
     * The first argument is the file of the SQL script to read, which must exist.
     * The second argument is the file where to write the compacted SQL script,
     * which will be overwritten without warning if it exists.
     * The values of those arguments are typically:
     *
     * <ol>
     *   <li>{@code EPSG_vX.mdb_Data_MySQL.sql}</li>
     *   <li>{@code core/sis-referencing/src/main/resources/org/apache/sis/referencing/factory/sql/Data.sql}</li>
     * </ol>
     *
     * @param  arguments The source files and the destination file.
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
            final EPSGDataFormatter f = new EPSGDataFormatter(c);
            f.run(new File(arguments[0]), new File(arguments[1]));
        } finally {
            TestDatabase.drop(ds);
        }
    }

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
     * {@code true} if insertions are currently done in the datum table.
     */
    private boolean insertDatum;

    /**
     * Creates a new instance.
     *
     * @param  c A dummy connection. Will be used for fetching metadata.
     * @throws SQLException if an error occurred while fetching metadata.
     */
    private EPSGDataFormatter(final Connection c) throws SQLException {
        super(c, EPSGInstaller.ENCODING, Integer.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the given line should be omitted from the script.
     *
     * @param  line The line, without trailing {@code ';'}.
     * @return {@code true} if the line should be omitted.
     */
    private static boolean omit(final String line) {
        // We omit the following line because we changed the type from VARCHAR to SMALLINT.
        return line.startsWith("UPDATE epsg_datum SET realization_epoch = replace(realization_epoch, CHR(182), CHR(10))");
    }

    /**
     * Compacts the given file.
     *
     * @param  inputFile    The input file where to read the SQL statements to compact.
     * @param  outputFile   The output file where to write the compacted SQL statements.
     * @param  encoding     The character encoding for both input and output files.
     * @throws IOException  if an I/O operation failed.
     * @throws SQLException should never happen.
     */
    private void run(final File inputFile, final File outputFile) throws SQLException, IOException {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("Input and output files are the same.");
        }
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), EPSGInstaller.ENCODING));
        try (final InputStream in = new FileInputStream(inputFile)) {
            run(inputFile.getName(), in);
        } finally {
            out.close();
            out = null;
        }
    }

    /**
     * "Executes" the given SQL statement. In the context of this {@code EPSGDataWriter} class,
     * executing a SQL statement means compacting it and writing it to the output file.
     *
     * @param  sql The SQL statement to compact.
     * @return The number of rows added.
     * @throws IOException if an I/O operation failed.
     */
    @Override
    protected int execute(final StringBuilder sql) throws IOException {
        removeLF(sql);
        String line = CharSequences.trimWhitespaces(sql).toString();
        if (insertStatement != null) {
            if (line.startsWith(insertStatement)) {
                // The previous instruction was already an INSERT INTO the same table.
                line = CharSequences.trimWhitespaces(line, insertStatement.length(), line.length()).toString();
                line = removeUselessExponents(line);
                if (insertDatum) {
                    line = removeRealizationEpochQuotes(line);
                }
                out.append(",\n"); // Really want Unix EOL, not the platform-specific one.
                writeValues(line);
                return 1;
            }
            // Previous instruction was the last INSERT INTO for a given table.
            // We now have a new instruction. Append the pending cariage return.
            out.append(";\n");
        }
        if (line.startsWith("INSERT INTO")) {
            insertDatum = line.startsWith("INSERT INTO EPSG_DATUM VALUES");
            int valuesStart = line.indexOf("VALUES", 11);
            if (valuesStart >= 0) {
                // We are beginning insertions in a new table.
                valuesStart += 6; // Move to the end of "VALUES".
                insertStatement = CharSequences.trimWhitespaces(line, 0, valuesStart).toString();
                line = CharSequences.trimWhitespaces(line, insertStatement.length(), line.length()).toString();
                line = removeUselessExponents(line);
                if (insertDatum) {
                    line = removeRealizationEpochQuotes(line);
                }
                out.append(insertStatement);
                out.append('\n');
                writeValues(line);
                return 1;
            }
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
     * For private usage by the following method only.
     */
    private static final Pattern uselessExponentPattern =
            Pattern.compile("([\\(\\,]\\-?\\d+\\.\\d+)E[\\+\\-]?0+([\\,\\)])");

    /**
     * Removes the useless "E0" exponents after floating point numbers.
     */
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
     * Removes the quotes in REALIZATION_EPOCH column (i.e. change the type from TEXT to INTEGER).
     * This is the 5th column.
     */
    private static String removeRealizationEpochQuotes(final String line) {
        int index = getIndexForColumn(line, 5);
        if (line.charAt(index) != '\'') {
            return line;
        }
        final StringBuilder cleaned = new StringBuilder(line.substring(0, index));
        if (line.charAt(++index) == '\'') {
            cleaned.append("Null");
        } else do {
            cleaned.append(line.charAt(index));
        }
        while (line.charAt(++index) != '\'');
        cleaned.append(line, index+1, line.length());
        return cleaned.toString();
    }

    /**
     * Returns the start index for the given column in the specified {@code VALUES} string.
     * Column numbers start at 1.
     */
    private static int getIndexForColumn(final String line, int column) {
        if (--column == 0) {
            return 0;
        }
        boolean quote = false;
        final int length = line.length();
        for (int index=0; index<length; index++) {
            switch (line.charAt(index)) {
                case '\'': {
                    if (index == 0 || line.charAt(index-1) != '\\') {
                        quote = !quote;
                    }
                    break;
                }
                case ',': {
                    if (!quote && --column==0) {
                        return CharSequences.skipLeadingWhitespaces(line, index+1, length);
                    }
                    break;
                }
            }
        }
        return length;
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
     * @param buffer The string in which to perform the removal.
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
