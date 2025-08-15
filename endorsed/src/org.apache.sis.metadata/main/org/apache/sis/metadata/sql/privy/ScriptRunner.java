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
package org.apache.sis.metadata.sql.privy;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Errors;


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
 * @author  Johann Sorel (Geomatys)
 */
public class ScriptRunner implements AutoCloseable {
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
     * The presumed dialect spoken by the database.
     */
    private final Dialect dialect;

    /**
     * A mapping of words to replace. The replacements are performed only for occurrences outside identifiers or texts.
     * See {@link #addReplacement(String, String)} for more explanation.
     *
     * @see #addReplacement(String, String)
     */
    private final Map<String,String> replacements;

    /**
     * The quote character for identifiers actually used in the database,
     * as determined by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    protected final String identifierQuote;

    /**
     * {@code true} if the database supports enums.
     * Example:
     *
     * {@snippet lang="sql" :
     *     CREATE TYPE metadata."CI_DateTypeCode" AS ENUM ('creation', 'publication');
     *     CREATE CAST (VARCHAR AS metadata."CI_DateTypeCode") WITH INOUT AS ASSIGNMENT;
     *     }
     *
     * <p>Notes per database product:</p>
     * <ul>
     *   <li><b>PostgreSQL:</b> while enumeration were introduced in PostgreSQL 8.3,
     *       we require PostgreSQL 8.4 because we need the {@code CAST … WITH INOUT} feature.</li>
     *   <li><b>Other databases:</b> assumed not supported.</li>
     * </ul>
     *
     * @see #statementsToSkip
     */
    protected final boolean isEnumTypeSupported;

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
     * If non-null, the SQL statements to skip (typically because not supported by the database).
     * The matcher is built as an alternation of many regular expressions separated by the pipe symbol.
     * The list of statements to skip depends on which {@code is*Supported} fields are set to {@code true}:
     *
     * <ul>
     *   <li>{@link #isEnumTypeSupported} for {@code "CREATE TYPE …"} or {@code "CREATE CAST …"} statements.</li>
     *   <li>{@link Dialect#supportsGrantUsageOnSchema} for {@code "GRANT USAGE ON SCHEMA …"} statements.</li>
     *   <li>{@link Dialect#supportsGrantSelectOnTable} for {@code "GRANT SELECT ON TABLE …"} statements.</li>
     *   <li>{@link Dialect#supportsComment} for {@code "COMMENT ON …"} statements.</li>
     * </ul>
     */
    private Matcher statementsToSkip;

    /**
     * The regular expression to use for building {@link #statementsToSkip}.
     * At most one of {@code regexOfStmtToSkip} and {@code statementsToSkip} shall be non-null.
     * Both fields may be null if there are no statements to skip.
     */
    private StringBuilder regexOfStmtToSkip;

    /**
     * Name of the SQL script under execution, or {@code null} if unknown.
     * This is used only for error reporting.
     */
    private String currentFile;

    /**
     * The line number of the SQL statement being executed. The first line in a file is numbered 1.
     * This is used only for error reporting.
     */
    private int currentLine;

    /**
     * The SQL statement being executed.
     * This is used only for error reporting.
     */
    private String currentSQL;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     *
     * <p>Some {@code maxRowsPerInsert} parameter values of interest:</p>
     * <ul>
     *   <li>A value of 0 means to create only the schema without inserting any data in them.</li>
     *   <li>A value of 1 means to use one separated {@code INSERT INTO} statement for each row, which may be slow.</li>
     *   <li>A value of 100 is a value which have been found empirically as giving good results.</li>
     *   <li>A value of {@link Integer#MAX_VALUE} means to not perform any attempt to limit the number of rows in an
     *       {@code INSERT INTO} statement. Note that this causes {@link StackOverflowError} in some JDBC driver.</li>
     * </ul>
     *
     * The {@code schemaToCreate} argument is ignored if not supported by the database.
     *
     * @param  connection        the connection to the database.
     * @param  schemaToCreate    schema to create and set as the default schema, or {@code null} if none.
     * @param  maxRowsPerInsert  maximum number of rows per {@code "INSERT INTO"} statement.
     * @throws SQLException if an error occurred while creating a SQL statement.
     */
    public ScriptRunner(final Connection connection, final String schemaToCreate, final int maxRowsPerInsert) throws SQLException {
        ArgumentChecks.ensurePositive("maxRowsPerInsert", maxRowsPerInsert);
        final DatabaseMetaData metadata;
        this.maxRowsPerInsert = maxRowsPerInsert;
        replacements     = new HashMap<>();
        metadata         = connection.getMetaData();
        dialect          = Dialect.guess(metadata);
        identifierQuote  = metadata.getIdentifierQuoteString();
        if (schemaToCreate != null && metadata.supportsSchemasInTableDefinitions()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE SCHEMA " + identifierQuote + schemaToCreate + identifierQuote);
                if (dialect.supportsGrantUsageOnSchema()) {
                    stmt.executeUpdate("GRANT USAGE ON SCHEMA " + identifierQuote + schemaToCreate + identifierQuote + " TO PUBLIC");
                }
            }
            connection.setSchema(schemaToCreate);   // Must be set before the next call to `createStatement()` below.
        }
        statement = connection.createStatement();
        switch (dialect) {
            default: {
                isEnumTypeSupported = false;
                break;
            }
            case POSTGRESQL: {
                final int version = metadata.getDatabaseMajorVersion();
                isEnumTypeSupported = (version == 8) ? metadata.getDatabaseMinorVersion() >= 4 : version >= 8;
                break;
            }
            case HSQL: {
                isEnumTypeSupported = false;
                /*
                 * HSQLDB stores tables in memory by default. For storing the tables on files, we have to
                 * use "CREATE CACHED TABLE" statement, which is HSQL-specific. For avoiding SQL dialect,
                 * the following statement change the default setting on current connection.
                 *
                 * Reference: http://hsqldb.org/doc/guide/dbproperties-chapt.html#dpc_db_props_url
                 */
                statement.execute("SET DATABASE DEFAULT TABLE TYPE CACHED");
                break;
            }
        }
        /*
         * Now build the list of statements to skip, depending of which features are supported by the database.
         * WARNING: do not use capturing group here, because some subclasses (e.g. EPSGInstaller) will use their
         * own capturing groups. A non-capturing group is declared by "(?:A|B)" instead of a plain "(A|B)".
         */
        if (!isEnumTypeSupported) {
            addStatementToSkip("CREATE\\s+(?:TYPE|CAST)\\s+.*");
        }
        if (!dialect.supportsAllGrants()) {
            addStatementToSkip("GRANT\\s+\\w+\\s+ON\\s+");
            if (dialect.supportsGrantUsageOnSchema()) {
                regexOfStmtToSkip.append("TABLE");
            } else if (dialect.supportsGrantSelectOnTable()) {
                regexOfStmtToSkip.append("SCHEMA");
            } else {
                regexOfStmtToSkip.append("(?:TABLE|SCHEMA)");
            }
            regexOfStmtToSkip.append("\\s+.*");
        }
        if (!dialect.supportsComment()) {
            addStatementToSkip("COMMENT\\s+ON\\s+.*");
        }
        if (!dialect.supportsAlterTableWithAddConstraint()) {
            addStatementToSkip("ALTER\\s+TABLE\\s+\\w+\\s+ADD\\s+CONSTRAINT\\s+.*");
        }
    }

    /**
     * Returns the connection to the database.
     *
     * @return the connection.
     * @throws SQLException if the connection cannot be obtained.
     */
    protected final Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * Adds a statement to skip. By default {@code ScriptRunner} ignores the following statements:
     *
     * <ul>
     *   <li>{@code "CREATE TYPE …"} or {@code "CREATE CAST …"} if {@link #isEnumTypeSupported} is {@code false}.</li>
     *   <li>{@code "GRANT USAGE ON SCHEMA …"} if {@link Dialect#supportsGrantUsageOnSchema} is {@code false}.</li>
     *   <li>{@code "GRANT SELECT ON TABLE …"} if {@link Dialect#supportsGrantSelectOnTable} is {@code false}.</li>
     *   <li>{@code "COMMENT ON …"} if {@link Dialect#supportsComment} is {@code false}.</li>
     * </ul>
     *
     * This method can be invoked for ignoring some additional statements.
     *
     * @param  regex  regular expression of the statement to ignore.
     */
    protected final void addStatementToSkip(final String regex) {
        if (statementsToSkip != null) {
            throw new IllegalStateException();
        }
        if (regexOfStmtToSkip == null) {
            regexOfStmtToSkip = new StringBuilder(regex);
        } else {
            regexOfStmtToSkip.append('|').append(regex);
        }
    }

    /**
     * Declares that a word in the <abbr>SQL</abbr> script needs to be replaced by the given word.
     * The replacement is performed only for occurrences outside quoted identifiers or texts.
     * For replacement of texts or identifiers, see {@link #editText(StringBuilder, int, int)}
     * and {@link #editQuotedIdentifier(StringBuilder, int, int)} instead.
     *
     * <h4>Example</h4>
     * This is used for mapping the <abbr>EPSG</abbr> table names from the mixed-cases convention used in
     * the {@code org.apache.sis.referencing.epsg} module to the lower-cases convention which is actually
     * used if the user installed the <abbr>EPSG</abbr> database manually (there is more differences than
     * only the case).
     *
     * <p>Another example is the replacement of <abbr>SQL</abbr> keywords used in the scripts by keywords
     * understood by the database. For example, if a database does not support the {@code "TEXT"} data type,
     * this method can be used for replacing {@code "TEXT"} by {@code "LONG VARCHAR"}.</p>
     *
     * <h4>Limitation</h4>
     * The {@code inScript} word to replace must be a single word with no space.
     * If the text to replace contains two words (for example {@code "CREATE TABLE"}), then revert
     * commit {@code bceb569558bfb7e3cf1a14aaf9261e786db06856} for bringing back this functionality.
     *
     * @param  inScript     the single word in the script which need to be replaced.
     * @param  replacement  the word(s) to use instead of {@code inScript} word.
     */
    protected final void addReplacement(final String inScript, final String replacement) {
        if (replacements.put(inScript, replacement) != null) {
            throw new IllegalArgumentException(inScript);
        }
    }

    /**
     * Returns the word to use instead of the given one.
     * If there is no replacement, then {@code inScript} is returned.
     *
     * @param  inScript  the word in the script which need to be replaced.
     * @return the word to use instead.
     */
    protected final String getReplacement(final String inScript) {
        return replacements.getOrDefault(inScript, inScript);
    }

    /**
     * Runs the given SQL script.
     * Lines are read and grouped up to the terminal {@value #END_OF_STATEMENT} character, then sent to the database.
     *
     * @param  statement  the SQL statements to execute.
     * @return the number of rows added or modified as a result of the statement execution.
     * @throws IOException if an error occurred while reading the input (should never happen).
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public final int run(final String statement) throws IOException, SQLException {
        return run(null, new LineNumberReader(new StringReader(statement)));
    }

    /**
     * Runs the SQL script from the given (filename, input stream) pair.
     * The file name is used only if an error needs to be reported.
     * The stream content is presumed encoded in UTF-8 and the stream will be closed by this method.
     * This method is intended to be invoked by code like this:
     *
     * {@snippet lang="java" :
     *     run("myFile.sql", MyClass.getResourceAsStream("myFile.sql"));
     * }
     *
     * <h4>Rational</h4>
     * Because {@link Class#getResourceAsStream(String)} is caller-sensitive, it must be invoked
     * from the module containing the resource. Invoking {@code getResourceAsStream(…)} from this
     * {@code run(…)} method does not work even with a {@link Class} instance passed in argument.
     *
     * @param  filename  name of the SQL script being executed. This is used only for error reporting.
     * @param  in  the stream to read. It will be closed by this method.
     * @return the number of rows added or modified as a result of the statement execution.
     * @throws IOException if an error occurred while reading the input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public final int run(final String filename, final InputStream in) throws IOException, SQLException {
        if (in == null) {
            throw new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, filename));
        }
        try (var reader = new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return run(filename, reader);
        }
    }

    /**
     * Runs the script from the given reader. Lines are read and grouped up to the
     * terminal {@value #END_OF_STATEMENT} character, then sent to the database.
     * Note that contrarily to {@link #run(String, InputStream)},
     * this method does <strong>not</strong> close the given reader.
     *
     * @param  filename  name of the SQL script being executed. This is used only for error reporting.
     * @param  in        the stream to read. It is caller's responsibility to close this reader.
     * @return the number of rows added or modified as a result of the script execution.
     * @throws IOException if an error occurred while reading the input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public final int run(final String filename, final BufferedReader in) throws IOException, SQLException {
        currentFile = filename;
        currentLine = 0;
        int statementCount            =  0;     // For informative purpose only.
        int posOpeningTextQuote       = -1;     // -1 if we are not inside a text.
        int posOpeningIdentifierQuote = -1;
        final var buffer = new StringBuilder();
        final boolean hasReplacements = !replacements.isEmpty();
        String line;
        while ((line = in.readLine()) != null) {
            currentLine++;
            /*
             * Ignore empty lines and comment lines, but only if they appear at the begining of the SQL statement.
             */
            if (buffer.length() == 0) {
                final int s = CharSequences.skipLeadingWhitespaces(line, 0, line.length());
                if (s >= line.length() || line.startsWith(COMMENT, s)) {
                    continue;
                }
                if (in instanceof LineNumberReader) {
                    currentLine = ((LineNumberReader) in).getLineNumber();
                }
            } else {
                buffer.append('\n');
            }
            /*
             * Copy the current line in the buffer. Then, the loop will search for words or characters to replace
             * (for example replacements of IDENTIFIER_QUOTE character by the database-specific quote character).
             * Replacements (if any) will be performed in-place in the buffer. Concequently the buffer length may
             * vary during the loop execution.
             */
            int pos = buffer.length();
            int length = buffer.append(line).length();
parseLine:  while (pos < length) {
                int c = buffer.codePointAt(pos);
                int n = Character.charCount(c);
                if ((posOpeningTextQuote & posOpeningIdentifierQuote) < 0) {    // True if both positions are -1.
                    int start = pos;
                    while (Character.isUnicodeIdentifierStart(c)) {
                        /*
                         * `start` is the position of the first character of a Unicode identifier. Following loop
                         * sets `pos` to the end (exclusive) of that Unicode identifier. Variable `c` will be set
                         * to the character after the Unicode identifier, provided that we have not reached EOL.
                         */
                        while ((pos += n) < length) {
                            c = buffer.codePointAt(pos);
                            n = Character.charCount(c);
                            if (!Character.isUnicodeIdentifierPart(c)) break;
                        }
                        /*
                         * Perform in-place replacement if the Unicode identifier is one of the keys listed
                         * in the `replacements` map. This operation may change the buffer length. The `pos`
                         * must be updated if needed for staying at position after the Unicode identifier.
                         */
                        if (hasReplacements) {
                            final String word = buffer.substring(start, pos);
                            final String replace = replacements.get(word);
                            if (replace != null) {
                                length = buffer.replace(start, pos, replace).length();
                                pos = start + replace.length();
                            }
                        }
                        /*
                         * Skip whitespaces and set the `c` variable to the next character, which may be either
                         * another Unicode start (to be processed by the enclosing loop) or another character
                         * (to be processed by the switch statement after the enclosing loop).
                         */
                        if (pos >= length) break parseLine;
                        while (Character.isWhitespace(c)) {
                            if ((pos += n) >= length) break parseLine;
                            c = buffer.codePointAt(pos);
                            n = Character.charCount(c);
                        }
                        start = pos;
                    }
                }
                switch (c) {
                    /*
                     * Found a character for an identifier like "Coordinate Operations".
                     * Check if we have found the opening or the closing character. Then
                     * replace the standard quote character by the database-specific one.
                     */
                    case IDENTIFIER_QUOTE: {
                        if (posOpeningTextQuote < 0) {
                            length = buffer.replace(pos, pos + n, identifierQuote).length();
                            n = identifierQuote.length();
                            if (posOpeningIdentifierQuote < 0) {
                                posOpeningIdentifierQuote = pos;
                            } else {
                                editQuotedIdentifier(buffer, posOpeningIdentifierQuote, pos += n);
                                pos -= length - (length = buffer.length());
                                posOpeningIdentifierQuote = -1;
                                continue;   // Because we already skipped the " character.
                            }
                        }
                        break;
                    }
                    /*
                     * Found a character for a text like 'This is a text'. Check if we have
                     * found the opening or closing character, ignoring the '' escape sequence.
                     */
                    case QUOTE: {
                        if (posOpeningIdentifierQuote < 0) {
                            if (posOpeningTextQuote < 0) {
                                posOpeningTextQuote = pos;
                            } else if ((pos += n) >= length || buffer.codePointAt(pos) != QUOTE) {
                                editText(buffer, posOpeningTextQuote, pos);
                                pos -= length - (length = buffer.length());
                                posOpeningTextQuote = -1;
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
                        if ((posOpeningTextQuote & posOpeningIdentifierQuote) < 0) {    // True if both are -1.
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
     * Invoked for each single-quoted text found in a <abbr>SQL</abbr> statement.
     * The text, <em>including its single quote characters</em>,
     * is the {@code sql} substring from index {@code lower} inclusive to {@code upper} exclusive.
     * Subclasses can override this method if they wish to modify the text content.
     * Modifications are applied directly in the given {@code sql} buffer.
     *
     * @param  sql    the whole SQL statement.
     * @param  lower  index of the opening quote character ({@code '}) of the text in {@code sql}.
     * @param  upper  index after the closing quote character ({@code '}) of the text in {@code sql}.
     */
    protected void editText(final StringBuilder sql, final int lower, final int upper) {
    }

    /**
     * Invoked for each double-quoted identifier found in a <abbr>SQL</abbr> statement.
     * The identifier, <em>including its double quote characters</em>,
     * is the {@code sql} substring from index {@code lower} inclusive to {@code upper} exclusive.
     * Subclasses can override this method if they wish to modify the identifier.
     * Modifications are applied directly in the given {@code sql} buffer.
     *
     * @param  sql    the whole SQL statement.
     * @param  lower  index of the opening quote character (usually {@code "}) of the identifier in {@code sql}.
     * @param  upper  index after the closing quote character (usually {@code "}) of the identifier in {@code sql}.
     */
    protected void editQuotedIdentifier(final StringBuilder sql, final int lower, final int upper) {
    }

    /**
     * Returns {@code true} if the given fragment seems outside identifier quotes or text quotes.
     * The given fragment must be the beginning or the end of an SQL statement, or be bounded by
     * indices that are known to be outside quotes. The implementation counts the occurrences of
     * {@value #IDENTIFIER_QUOTE} and {@value #QUOTE} and verifies that both of them are even.
     *
     * @param  sql   the SQL statement for which to test if a fragment is outside quotes.
     * @param  from  index of the first character of the fragment.
     * @param  to    index after the last character of the fragment.
     * @return whether the given fragment seems outside quotes.
     */
    private static boolean isOutsideQuotes(final CharSequence sql, int from, final int to) {
        int nq = 0, ni = 0;
        while (from < to) {
            switch (sql.charAt(from++)) {
                case IDENTIFIER_QUOTE: {
                    ni++;
                    break;
                }
                case QUOTE: {
                    if ((nq & 1) != 0 && from < to && sql.charAt(from) == QUOTE) {
                        from++;
                    } else {
                        nq++;
                    }
                    break;
                }
            }
        }
        return ((nq | ni) & 1) == 0;
    }

    /**
     * Returns {@code true} if the given SQL statements is supported by the database engine,
     * or {@code false} if this statement should be ignored. The default implementation checks
     * if the given query matches the regular expressions given to {@link #addStatementToSkip(String)}.
     *
     * <p>This method is only a hint; a value of {@code true} is not a guaranteed that the given
     * SQL statement is valid.</p>
     *
     * @param  sql  the SQL statement to verify.
     * @return whether the given SQL statement is supported by the database engine.
     */
    protected boolean isSupported(final CharSequence sql) {
        if (statementsToSkip != null) {
            return !statementsToSkip.reset(sql).matches();
        } else if (regexOfStmtToSkip != null) {
            // We do not use Pattern.CASE_INSENTITIVE for performance reasons.
            statementsToSkip = Pattern.compile(regexOfStmtToSkip.toString(), Pattern.DOTALL).matcher(sql);
            regexOfStmtToSkip = null;
            return !statementsToSkip.matches();
        } else {
            return true;
        }
    }

    /**
     * Executes the given SQL statement.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If {@link #isSupported(CharSequence)} returns {@code false}, then this method does nothing.</li>
     *   <li>If the statement is {@code CREATE TABLE ... INHERITS ...} but the database does not support
     *       table inheritance, then this method drops the {@code INHERITS ...} part.</li>
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
     * @param  sql  the SQL statement to execute.
     * @return the number of rows added or modified as a result of the statement execution.
     * @throws SQLException if an error occurred while executing the SQL statement.
     * @throws IOException if an I/O operation was required and failed.
     */
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        if (!isSupported(sql)) {
            return 0;
        }
        String subSQL = currentSQL = CharSequences.trimWhitespaces(sql).toString();
        if (!dialect.supportsTableInheritance() && subSQL.startsWith("CREATE TABLE")) {
            final int s = sql.lastIndexOf("INHERITS");
            if (s >= 0 && isOutsideQuotes(sql, s+8, sql.length())) {             // 8 is the length of "INHERITS".
                sql.setLength(CharSequences.skipTrailingWhitespaces(sql, 0, s));
                subSQL = currentSQL = sql.toString();
            }
        }
        int count = 0;
        /*
         * The scripts usually do not contain any SELECT statement. One exception is the creation
         * of geometry columns in a PostGIS database, which use "SELECT AddGeometryColumn(…)".
         */
        if (subSQL.startsWith(SQLBuilder.SELECT)) {
            statement.executeQuery(subSQL).close();
        } else {
            if (maxRowsPerInsert != Integer.MAX_VALUE && subSQL.startsWith("INSERT INTO")) {
                if (maxRowsPerInsert == 0) {
                    subSQL = null;              // Skip completely the "INSERT INTO" statement.
                } else {
                    int endOfLine = subSQL.indexOf('\n', 11);           // 11 is the length of "INSERT INTO".
                    if (subSQL.startsWith("VALUES", endOfLine - 6)) {
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
                            if (--nrows == 0) {    // Extract lines until we have reached the `maxRowsPerInsert` amount.
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
     * @throws SQLException if an error occurred while closing the statement.
     */
    @Override
    public void close() throws SQLException {
        statement.close();
    }

    /**
     * Returns the current position (current file and current line in that file). The returned string may also contain
     * the SQL statement under execution. The main purpose of this method is to provide information about the position
     * where an exception occurred.
     *
     * @param  locale  the locale for the message to return.
     * @return a string representation of the current position, or {@code null} if unknown.
     */
    public String status(final Locale locale) {
        String position = null;
        if (currentFile != null) {
            position = Errors.forLocale(locale).getString(Errors.Keys.ErrorInFileAtLine_2, currentFile, currentLine);
        }
        if (currentSQL != null) {
            final var buffer = new StringBuilder();
            if (position != null) {
                buffer.append(position).append('\n');
            }
            position = buffer.append("SQL: ").append(currentSQL).toString();
        }
        return position;
    }

    /**
     * Returns a string representation of this runner for debugging purpose.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "status", status(null));
    }
}
