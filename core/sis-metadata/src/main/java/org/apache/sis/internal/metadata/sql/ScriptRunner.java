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
package org.apache.sis.internal.metadata.sql;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.EOFException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;

// Branch-specific imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk8.BiFunction;
import org.apache.sis.internal.jdk7.AutoCloseable;


/**
 * Run SQL scripts. The script is expected to use a standardized syntax, where the {@value #QUOTE} character
 * is used for quoting text, the {@value #IDENTIFIER_QUOTE} character is used for quoting identifier and the
 * {@value #END_OF_STATEMENT} character is used at the end for every SQL statement. Those characters will be
 * replaced on-the-fly by the characters actually used by the database engine.
 *
 * <p><strong>This class is not intended for executing arbitrary SQL scripts.</strong>
 * This class is for executing known scripts bundled with Apache SIS or in an extension
 * (for example the scripts for creating the EPSG database). We do not try to support SQL
 * functionalities other than what we need for those scripts.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@AutoCloseable
public class ScriptRunner {
    /**
     * The database user having read (not write) permissions.
     *
     * @see #isGrantOnSchemaSupported
     * @see #isGrantOnTableSupported
     */
    protected static final String PUBLIC = "PUBLIC";

    /**
     * The sequence for SQL comments. Leading lines starting by those characters will be ignored.
     */
    private static final String COMMENT = "--";

    /**
     * The quote character expected to be found in the SQL script.
     * This character shall not be a whitespace or a Unicode identifier part.
     */
    private static final char QUOTE = '\'';

    /**
     * The quote character for identifiers expected to be found in the SQL script.
     * This character shall not be a whitespace or a Unicode identifier part.
     */
    private static final char IDENTIFIER_QUOTE = '"';

    /**
     * The character at the end of statements.
     * This character shall not be a whitespace or a Unicode identifier part.
     */
    private static final char END_OF_STATEMENT = ';';

    /**
     * The characters for escaping a portion of the SQL script. This is used by PostgreSQL
     * for the definition of triggers. Those characters should appear at the beginning of
     * a line (ignoring whitespaces), because the text before it will not be parsed.
     *
     * <p>This string shall not begin with a whitespace or
     * {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier part}.</p>
     */
    private static final String ESCAPE = "$BODY$";

    /**
     * The presumed dialect spoken by the database.
     */
    private final Dialect dialect;

    /**
     * A mapping of words to replace. The replacements are performed only for occurrences outside identifiers or texts.
     * See {@link #replace(String, String)} for more explanation.
     *
     * @see #replace(String, String)
     */
    private final Map<String,String> replacements = new HashMap<String,String>();

    /**
     * A sentinel value for the {@linkplain #replace replacements} map meaning that {@code ScriptRunner}
     * needs to look also at the word after the word associated to {@code MORE_WORDS}.
     *
     * @see #replace(String, String)
     */
    protected static final String MORE_WORDS = "…";

    /**
     * The quote character for identifiers actually used in the database,
     * as determined by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    protected final String identifierQuote;

    /**
     * {@code true} if the database supports enums.
     *
     * <p>Notes per database product:</p>
     * <ul>
     *   <li><b>PostgreSQL:</b> while enumeration were introduced in PostgreSQL 8.3,
     *       we require PostgreSQL 8.4 because we need the {@code CAST … WITH INOUT} feature.</li>
     *   <li><b>Other databases:</b> assumed not supported.</li>
     * </ul>
     */
    protected final boolean isEnumTypeSupported;

    /**
     * {@code true} if the database supports catalogs.
     */
    protected final boolean isCatalogSupported;

    /**
     * {@code true} if the database supports schemas.
     */
    protected final boolean isSchemaSupported;

    /**
     * {@code true} if the database supports {@code "GRANT USAGE ON SCHEMA"} statements.
     * Read-only permissions are typically granted to {@link #PUBLIC}.
     */
    protected final boolean isGrantOnSchemaSupported;

    /**
     * {@code true} if the database supports {@code "GRANT SELECT ON TABLE"} statements.
     * Read-only permissions are typically granted to {@link #PUBLIC}.
     */
    protected final boolean isGrantOnTableSupported;

    /**
     * {@code true} if the following instruction shall be executed
     * (assuming that the PostgreSQL {@code "plpgsql"} language is desired):
     *
     * {@code sql
     *   CREATE TRUSTED PROCEDURAL LANGUAGE 'plpgsql'
     *     HANDLER plpgsql_call_handler
     *     VALIDATOR plpgsql_validator;
     * }
     *
     * <p>Notes per database product:</p>
     * <ul>
     *   <li><b>PostgreSQL:</b> {@code true} only for database prior to version 9.
     *       Starting at version 9, the language is installed by default.</li>
     *   <li><b>Other databases:</b> {@code false} because not supported.</li>
     * </ul>
     */
    protected final boolean isCreateLanguageRequired;

    /**
     * The maximum number of rows allowed per {@code "INSERT"} statement.
     * This is 1 if the database does not support multi-rows insertion.
     * For other database, this is set to an arbitrary "reasonable" value since attempts to insert
     * too many rows with a single statement on Derby database cause a {@link StackOverflowError}.
     */
    private final int maxRowsPerInsert;

    /**
     * The statement created from a connection to the database.
     */
    private final Statement statement;

    /**
     * Name of the SQL script under execution, or {@code null} if unknown.
     * This is used only for error reporting.
     */
    @Debug
    private String currentFile;

    /**
     * The line number of the SQL statement being executed. The first line in a file is numbered 1.
     * This is used only for error reporting.
     */
    @Debug
    private int currentLine;

    /**
     * The SQL statement being executed.
     * This is used only for error reporting.
     */
    @Debug
    private String currentSQL;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     *
     * <p>Some {@code maxRowsPerInsert} parameter values of interest:</p>
     * <ul>
     *   <li>A value of 0 means to create only the schemas without inserting any data in them.</li>
     *   <li>A value of 1 means to use one separated {@code INSERT INTO} statement for each row, which may be slow.</li>
     *   <li>A value of 100 is a value which have been found empirically as giving good results.</li>
     *   <li>A value of {@link Integer#MAX_VALUE} means to not perform any attempt to limit the number of rows in an
     *       {@code INSERT INTO} statement. Note that this causes {@link StackOverflowError} in some JDBC driver.</li>
     * </ul>
     *
     * @param connection        The connection to the database.
     * @param maxRowsPerInsert  Maximum number of rows per {@code "INSERT INTO"} statement.
     * @throws SQLException if an error occurred while creating a SQL statement.
     */
    protected ScriptRunner(final Connection connection, int maxRowsPerInsert) throws SQLException {
        ArgumentChecks.ensureNonNull("connection", connection);
        ArgumentChecks.ensurePositive("maxRowsPerInsert", maxRowsPerInsert);
        final DatabaseMetaData metadata = connection.getMetaData();
        this.dialect            = Dialect.guess(metadata);
        this.identifierQuote    = metadata.getIdentifierQuoteString();
        this.isSchemaSupported  = metadata.supportsSchemasInTableDefinitions() &&
                                  metadata.supportsSchemasInDataManipulation();
        this.isCatalogSupported = metadata.supportsCatalogsInTableDefinitions() &&
                                  metadata.supportsCatalogsInDataManipulation();
        switch (dialect) {
            default: {
                isEnumTypeSupported      = false;
                isGrantOnSchemaSupported = false;
                isGrantOnTableSupported  = false;
                isCreateLanguageRequired = false;
                break;
            }
            case POSTGRESQL: {
                final int version = metadata.getDatabaseMajorVersion();
                isEnumTypeSupported      = (version == 8) ? metadata.getDatabaseMinorVersion() >= 4 : version >= 8;
                isGrantOnSchemaSupported = true;
                isGrantOnTableSupported  = true;
                isCreateLanguageRequired = (version < 9);
                break;
            }
            case HSQL: {
                isEnumTypeSupported      = false;
                isGrantOnSchemaSupported = false;
                isGrantOnTableSupported  = false;
                isCreateLanguageRequired = false;
                if (maxRowsPerInsert != 0) {
                    maxRowsPerInsert = 1;
                }
                /*
                 * HSQLDB does not seem to support the {@code UNIQUE} keyword in {@code CREATE TABLE} statements.
                 * In addition, we must declare explicitly that we want the tables to be cached on disk. Finally,
                 * HSQL expects "CHR" to be spelled "CHAR".
                 */
                addReplacement("UNIQUE", "");
                addReplacement("CHR", "CHAR");
                addReplacement("CREATE", MORE_WORDS);
                addReplacement("CREATE TABLE", "CREATE CACHED TABLE");
                break;
            }
        }
        this.maxRowsPerInsert = maxRowsPerInsert;
        statement = connection.createStatement();
    }

    /**
     * Returns the connection to the database.
     *
     * @return The connection.
     * @throws SQLException if the connection can not be obtained.
     */
    protected final Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * Declares that a word in the SQL script needs to be replaced by the given word.
     * The replacement is performed only for occurrences outside identifiers or texts.
     *
     * <div class="note"><b>Example</b>
     * this is used for mapping the table names in the EPSG scripts to table names as they were in the MS-Access
     * flavor of EPSG database. It may also contains the mapping between SQL keywords used in the SQL scripts to
     * SQL keywords understood by the database (for example Derby does not support the {@code TEXT} data type,
     * which need to be replaced by {@code VARCHAR}).</div>
     *
     * If a text to replace contains two or more words, then this map needs to contain an entry for the first word
     * associated to the {@link #MORE_WORDS} value. For example if one needs to replace the {@code "CREATE TABLE"}
     * words, then in addition to the {@code "CREATE TABLE"} entry this {@code replacements} map shall also contain
     * a {@code "CREATE"} entry associated with the {@link #MORE_WORDS} value.
     *
     * @param inScript The word in the script which need to be replaced.
     * @param replacement The word to use instead.
     */
    protected final void addReplacement(final String inScript, final String replacement) {
        if (replacements.put(inScript, replacement) != null) {
            throw new IllegalArgumentException(inScript);
        }
    }

    /**
     * Returns the word to use instead than the given one.
     * If there is no replacement, then {@code inScript} is returned.
     *
     * @param inScript The word in the script which need to be replaced.
     * @return The word to use instead.
     */
    protected final String getReplacement(final String inScript) {
        return JDK8.getOrDefault(replacements, inScript, inScript);
    }

    /**
     * For every entries in the replacements map, replace the entry value by the value returned by
     * {@code function(key, value)}.
     *
     * @param function The function that modify the replacement mapping.
     */
    protected final void modifyReplacements(final BiFunction<String,String,String> function) {
        JDK8.replaceAll(replacements, function);
    }

    /**
     * Runs the given SQL script.
     * Lines are read and grouped up to the terminal {@value #END_OF_STATEMENT} character, then sent to the database.
     *
     * @param  statement The SQL statements to execute.
     * @return The number of rows added or modified as a result of the statement execution.
     * @throws IOException if an error occurred while reading the input (should never happen).
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public final int run(final String statement) throws IOException, SQLException {
        return run(null, new LineNumberReader(new StringReader(statement)));
    }

    /**
     * Runs the script from the given reader. Lines are read and grouped up to the
     * terminal {@value #END_OF_STATEMENT} character, then sent to the database.
     *
     * @param  filename Name of the SQL script being executed. This is used only for error reporting.
     * @param  in The stream to read. It is caller's responsibility to close this reader.
     * @return The number of rows added or modified as a result of the script execution.
     * @throws IOException if an error occurred while reading the input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public final int run(final String filename, final BufferedReader in) throws IOException, SQLException {
        currentFile = filename;
        currentLine = 0;
        int     statementCount     = 0;         // For informative purpose only.
        int     posOpeningQuote    = -1;        // -1 if we are not inside a text.
        boolean isInsideIdentifier = false;
        final StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            /*
             * Ignore empty lines and comment lines, but only if they appear at the begining of the SQL statement.
             */
            if (buffer.length() == 0) {
                final int s = CharSequences.skipLeadingWhitespaces(line, 0, line.length());
                if (s >= line.length() || line.regionMatches(s, COMMENT, 0, COMMENT.length())) {
                    continue;
                }
                if (in instanceof LineNumberReader) {
                    currentLine = ((LineNumberReader) in).getLineNumber();
                }
            } else {
                buffer.append('\n');
            }
            /*
             * If we find the "$BODY$" string, copy verbatism (without any attempt to parse the lines) until
             * the next occurrence of "$BODY$".  This simple algorithm does not allow more than one block of
             * "$BODY$ ... $BODY$" on the same statement and presumes that the text before "$BODY$" contains
             * nothing that need to be parsed.
             */
            int pos = line.indexOf(ESCAPE);
            if (pos >= 0) {
                pos += ESCAPE.length();
                while ((pos = line.indexOf(ESCAPE, pos)) < 0) {
                    buffer.append(line).append('\n');
                    line = in.readLine();
                    if (line == null) {
                        throw new EOFException();
                    }
                    pos = 0;
                }
                pos += ESCAPE.length();
                buffer.append(line, 0, pos);
                line = line.substring(pos);
            }
            /*
             * Copy the current line in the buffer. Then, the loop will search for words or characters to replace
             * (for example replacements of IDENTIFIER_QUOTE character by the database-specific quote character).
             * Replacements (if any) will be performed in-place in the buffer. Concequently the buffer length may
             * vary during the loop execution.
             */
            pos = buffer.length();
            int length = buffer.append(line).length();
parseLine:  while (pos < length) {
                int c = buffer.codePointAt(pos);
                int n = Character.charCount(c);
                if (posOpeningQuote < 0 && !isInsideIdentifier) {
                    int start = pos;
                    while (Character.isUnicodeIdentifierStart(c)) {
                        /*
                         * 'start' is the position of the first character of a Unicode identifier. Following loop
                         * sets 'pos' to the end (exclusive) of that Unicode identifier. Variable 'c' will be set
                         * to the character after the Unicode identifier, provided that we have not reached EOL.
                         */
                        while ((pos += n) < length) {
                            c = buffer.codePointAt(pos);
                            n = Character.charCount(c);
                            if (!Character.isUnicodeIdentifierPart(c)) break;
                        }
                        /*
                         * Perform in-place replacement if the Unicode identifier is one of the keys in the listed
                         * in the 'replacements' map. This operation may change the buffer length.  The 'pos' must
                         * be updated if needed for staying the position after the Unicode identifier.
                         */
                        final String word = buffer.substring(start, pos);
                        final String replace = replacements.get(word);
                        boolean moreWords = false;
                        if (replace != null) {
                            moreWords = replace.equals(MORE_WORDS);
                            if (!moreWords) {
                                length = buffer.replace(start, pos, replace).length();
                                pos = start + replace.length();
                            }
                        }
                        /*
                         * Skip whitespaces and set the 'c' variable to the next character, which may be either
                         * another Unicode start (to be processed by the enclosing loop) or another character
                         * (to be processed by the switch statement after the enclosing loop).
                         */
                        if (pos >= length) break parseLine;
                        while (Character.isWhitespace(c)) {
                            if ((pos += n) >= length) break parseLine;
                            c = buffer.codePointAt(pos);
                            n = Character.charCount(c);
                        }
                        if (!moreWords) {
                            start = pos;
                        }
                    }
                }
                switch (c) {
                    /*
                     * Found a character for an identifier like "Coordinate Operations".
                     * Check if we have found the opening or the closing character. Then
                     * replace the standard quote character by the database-specific one.
                     */
                    case IDENTIFIER_QUOTE: {
                        if (posOpeningQuote < 0) {
                            isInsideIdentifier = !isInsideIdentifier;
                            length = buffer.replace(pos, pos + n, identifierQuote).length();
                            n = identifierQuote.length();
                        }
                        break;
                    }
                    /*
                     * Found a character for a text like 'This is a text'. Check if we have
                     * found the opening or closing character, ignoring the '' escape sequence.
                     */
                    case QUOTE: {
                        if (!isInsideIdentifier) {
                            if (posOpeningQuote < 0) {
                                posOpeningQuote = pos;
                            } else if ((pos += n) >= length || buffer.codePointAt(pos) != QUOTE) {
                                editText(buffer, posOpeningQuote, pos);
                                pos -= length - (length = buffer.length());
                                posOpeningQuote = -1;
                                continue;   // Because we already skipped the ' character.
                            } // else found a double ' character, which means to escape it.
                        }
                        break;
                    }
                    /*
                     * Found the end of statement. Remove that character if it is the last non-white character,
                     * since SQL statement in JDBC are not expected to contain it.
                     */
                    case END_OF_STATEMENT: {
                        if (posOpeningQuote < 0 && !isInsideIdentifier) {
                            if (CharSequences.skipLeadingWhitespaces(buffer, pos + n, length) >= length) {
                                buffer.setLength(pos);
                            }
                            statementCount += execute(buffer);
                            buffer.setLength(0);
                            break parseLine;
                        }
                        break;
                    }
                }
                pos += n;
            }
        }
        line = buffer.toString().trim();
        if (!line.isEmpty() && !line.startsWith(COMMENT)) {
            throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfString_1, line));
        }
        currentFile = null;
        return statementCount;
    }

    /**
     * Invoked for each text found in a SQL statement. The text, <em>including its quote characters</em>,
     * is the {@code sql} substring from index {@code lower} inclusive to {@code upper} exclusive.
     * Subclasses can override this method if they wish to modify the text content.
     * Modifications are applied directly in the given {@code sql} buffer.
     *
     * @param sql   The whole SQL statement.
     * @param lower Index of the opening quote character ({@code '}) of the text in {@code sql}.
     * @param upper Index after the closing quote character ({@code '}) of the text in {@code sql}.
     */
    protected void editText(final StringBuilder sql, final int lower, final int upper) {
    }

    /**
     * Executes the given SQL statement.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the {@code maxRowsPerInsert} argument given at construction time was zero,
     *       then this method skips {@code "INSERT INTO"} statements but executes all other.</li>
     *   <li>Otherwise this method executes the given statement with the following modification:
     *       if the statement is an {@code "INSERT INTO"} with many values, then this method may break
     *       that statement into many {@code "INSERT INTO"} where each statements does not have move
     *       than {@code maxRowsPerInsert} rows.</li>
     * </ul>
     *
     * Subclasses that override this method can freely edit the {@link StringBuilder} content before
     * to invoke this method.
     *
     * @param  sql The SQL statement to execute.
     * @return The number of rows added or modified as a result of the statement execution.
     * @throws SQLException if an error occurred while executing the SQL statement.
     * @throws IOException if an I/O operation was required and failed.
     */
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        String subSQL = currentSQL = CharSequences.trimWhitespaces(sql).toString();
        int count = 0;
        /*
         * The scripts usually do not contain any SELECT statement. One exception is the creation
         * of geometry columns in a PostGIS database, which use "SELECT AddGeometryColumn(…)".
         */
        if (subSQL.startsWith("SELECT ")) {
            statement.executeQuery(subSQL).close();
        } else {
            if (maxRowsPerInsert != Integer.MAX_VALUE && subSQL.startsWith("INSERT INTO")) {
                if (maxRowsPerInsert == 0) {
                    subSQL = null;              // Skip completely the "INSERT INTO" statement.
                } else {
                    int endOfLine = subSQL.indexOf('\n', 11);                    // 11 is the length of "INSERT INTO".
                    if (subSQL.regionMatches(endOfLine - 6, "VALUES", 0, 6)) {   //  6 is the length of "VALUES".
                        /*
                         * The following code is very specific to the syntax of the scripts generated by SIS.
                         * This code fetches the "INSERT INTO" part, which is expected to be on its own line.
                         * We will left this part of the buffer unchanged and write only after the offset.
                         */
                        sql.setLength(0);   // Rewrite from the beginning in case we trimmed whitespaces.
                        final int startOfValues = sql.append(subSQL, 0, endOfLine).append(' ').length();
                        int nrows = maxRowsPerInsert;
                        int begin = endOfLine + 1;
                        while ((endOfLine = subSQL.indexOf('\n', ++endOfLine)) >= 0) {
                            if (--nrows == 0) {    // Extract lines until we have reached the 'maxRowsPerInsert' amount.
                                int end = endOfLine;
                                if (subSQL.charAt(end - 1) == ',') {
                                    end--;
                                }
                                count += statement.executeUpdate(currentSQL = sql.append(subSQL, begin, end).toString());
                                sql.setLength(startOfValues);       // Prepare for next INSERT INTO statement.
                                nrows = maxRowsPerInsert;
                                begin = endOfLine + 1;
                            }
                        }
                        // The remaining of the statement to be executed.
                        int end = CharSequences.skipTrailingWhitespaces(subSQL, begin, subSQL.length());
                        currentSQL = subSQL = (end > begin) ? sql.append(subSQL, begin, end).toString() : null;
                    }
                }
            }
            if (subSQL != null) {
                count += statement.executeUpdate(subSQL);
            }
        }
        currentSQL = null;      // Clear on success only.
        return count;
    }

    /**
     * Closes the statement used by this runner. Note that this method does not close the connection
     * given to the constructor; this connection still needs to be closed explicitly by the caller.
     *
     * @throws SQLException If an error occurred while closing the statement.
     */
    public void close() throws SQLException {
        statement.close();
    }

    /**
     * Returns the current position (current file and current line in that file). The returned string may also contain
     * the SQL statement under execution. The main purpose of this method is to provide information about the position
     * where an exception occurred.
     *
     * @param locale The locale for the message to return.
     * @return A string representation of the current position, or {@code null} if unknown.
     */
    public String status(final Locale locale) {
        String position = null;
        if (currentFile != null) {
            position = Errors.getResources(locale).getString(Errors.Keys.ErrorInFileAtLine_2, currentFile,
                    (currentLine != 0) ? currentLine : '?');
        }
        if (currentSQL != null) {
            final StringBuilder buffer = new StringBuilder();
            if (position != null) {
                buffer.append(position).append('\n');
            }
            position = buffer.append("SQL: ").append(currentSQL).toString();
        }
        return position;
    }

    /**
     * Returns a string representation of this runner for debugging purpose. Current implementation returns the
     * current position in the script being executed, and the SQL statement. This method may be invoked after a
     * {@link SQLException} occurred in order to determine the line in the SQL script that caused the error.
     *
     * @return The current position in the script being executed.
     */
    @Debug
    @Override
    public String toString() {
        return status(null);
    }
}
