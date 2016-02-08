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

import java.util.Locale;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.PerformanceLevel;

// Branch-specific imports
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sis.internal.jdk8.BiFunction;


/**
 * Runs the SQL scripts for creating an EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class EPSGInstaller extends ScriptRunner {
    /**
     * The embedded SQL scripts to execute for creating the EPSG database, in that order.
     * The {@code ".sql"} suffix is omitted. The {@code "Grant"} script must be last.
     */
    private static final String[] SCRIPTS = {
        "Tables", "Data", "Patches", "FKeys", "Indexes", "Grant"         // "Grant" must be last.
    };

    /**
     * The encoding used in the SQL scripts.
     */
    static final String ENCODING = "ISO-8859-1";

    /**
     * The pattern for an {@code "UPDATE … SET … REPLACE"} instruction.
     * Example:
     *
     * {@preformat sql
     *     UPDATE epsg_datum
     *     SET datum_name = replace(datum_name, CHAR(182), CHAR(10));
     * }
     */
    static final String REPLACE_STATEMENT =
            "\\s*UPDATE\\s+[\\w\\.\" ]+\\s+SET\\s+(\\w+)\\s*=\\s*replace\\s*\\(\\s*\\1\\W+.*";

    /**
     * {@code true} if the Pilcrow character (¶ - decimal code 182) should be replaced by Line Feed
     * (LF - decimal code 10). This is a possible workaround when the database does not support the
     * {@code REPLACE(column, CHAR(182), CHAR(10))} SQL statement, but accepts LF.
     */
    private final boolean replacePilcrow;

    /**
     * Non-null if there is SQL statements to skip. This is the case of {@code UPDATE … SET x = REPLACE(x, …)}
     * functions, since Derby does not supports the {@code REPLACE} function.
     */
    private final Matcher statementToSkip;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     * The encoding is {@code "ISO-8859-1"}, which is the encoding used for the files provided by EPSG.
     *
     * @param connection The connection to the database.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public EPSGInstaller(final Connection connection) throws SQLException {
        super(connection, ENCODING, 100);
        boolean isReplaceSupported = false;
        final DatabaseMetaData metadata = connection.getMetaData();
        final String functions = metadata.getStringFunctions();
        for (final StringTokenizer tk = new StringTokenizer(functions, ","); tk.hasMoreTokens();) {
            final String token = tk.nextToken().trim();
            if (token.equalsIgnoreCase("REPLACE")) {
                isReplaceSupported = true;
                break;
            }
        }
        if (isReplaceSupported) {
            statementToSkip = null;
        } else {
            statementToSkip = Pattern.compile(REPLACE_STATEMENT, Pattern.CASE_INSENSITIVE).matcher("");
        }
        replacePilcrow = false;         // Never supported for now.
    }

    /**
     * Creates immediately a schema of the given name in the database and remember that the
     * {@code "epsg_"} prefix in table names will need to be replaced by path to that schema.
     *
     * <p>This method should be invoked only once. It does nothing if the database does not supports schema.</p>
     *
     * @param schema The schema (usually {@code "epsg"}).
     * @throws SQLException if the schema can not be created.
     * @throws IOException if an I/O operation was required and failed.
     */
    public void setSchema(final String schema) throws SQLException, IOException {
        if (isSchemaSupported) {
            /*
             * Creates the schema on the database. We do that before to setup the 'toSchema' map, while the map still null.
             * Note that we do not quote the schema name, which is a somewhat arbitrary choice.
             */
            execute(new StringBuilder("CREATE SCHEMA ").append(schema));
            if (isGrantOnSchemaSupported) {
                execute(new StringBuilder("GRANT USAGE ON SCHEMA ").append(schema).append(" TO ").append(PUBLIC));
            }
            /*
             * Mapping from the table names used in the SQL scripts to the original names used in the MS-Access database.
             * We use those original names because they are easier to read than the names in SQL scripts.
             */
            addReplacement(SQLTranslator.TABLE_PREFIX + "alias",                      "Alias");
            addReplacement(SQLTranslator.TABLE_PREFIX + "area",                       "Area");
            addReplacement(SQLTranslator.TABLE_PREFIX + "change",                     "Change");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordinateaxis",             "Coordinate Axis");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordinateaxisname",         "Coordinate Axis Name");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperation",             "Coordinate_Operation");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperationmethod",       "Coordinate_Operation Method");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperationparam",        "Coordinate_Operation Parameter");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperationparamusage",   "Coordinate_Operation Parameter Usage");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperationparamvalue",   "Coordinate_Operation Parameter Value");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordoperationpath",         "Coordinate_Operation Path");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordinatereferencesystem",  "Coordinate Reference System");
            addReplacement(SQLTranslator.TABLE_PREFIX + "coordinatesystem",           "Coordinate System");
            addReplacement(SQLTranslator.TABLE_PREFIX + "datum",                      "Datum");
            addReplacement(SQLTranslator.TABLE_PREFIX + "deprecation",                "Deprecation");
            addReplacement(SQLTranslator.TABLE_PREFIX + "ellipsoid",                  "Ellipsoid");
            addReplacement(SQLTranslator.TABLE_PREFIX + "namingsystem",               "Naming System");
            addReplacement(SQLTranslator.TABLE_PREFIX + "primemeridian",              "Prime Meridian");
            addReplacement(SQLTranslator.TABLE_PREFIX + "supersession",               "Supersession");
            addReplacement(SQLTranslator.TABLE_PREFIX + "unitofmeasure",              "Unit of Measure");
            addReplacement(SQLTranslator.TABLE_PREFIX + "versionhistory",             "Version History");
            prependNamespace(schema);
        }
    }

    /**
     * Prepends the given schema or catalog to all table names.
     */
    final void prependNamespace(final String schema) {
        modifyReplacements(new BiFunction<String,String,String>() {
            @Override public String apply(String key, String value) {
                return key.startsWith(SQLTranslator.TABLE_PREFIX) ?
                        schema + '.' + identifierQuote + value + identifierQuote : value;
            }
        });
    }

    /**
     * Invoked for each text found in a SQL statement. This method replaces {@code ''} by {@code Null}.
     * The intend is to consistently use the null value for meaning "no information", which is not the
     * same than "information is an empty string". This replacement is okay in this particular case
     * since there is no field in the EPSG database for which we really want an empty string.
     *
     * @param sql   The whole SQL statement.
     * @param lower Index of the first character of the text in {@code sql}.
     * @param upper Index after the last character of the text in {@code sql}.
     */
    @Override
    protected void editText(final StringBuilder sql, final int lower, final int upper) {
        final String replacement;
        switch (upper - lower) {
            default: {
                return;
            }
            /*
             * Replace '' by Null for every table.
             */
            case 2: {
                replacement = "Null";
                break;
            }
        }
        sql.replace(lower, upper, replacement);
    }

    /**
     * Modifies the SQL statement before to execute it, or omit unsupported statements.
     *
     * @throws SQLException if an error occurred while executing the SQL statement.
     * @throws IOException if an I/O operation was required and failed.
     */
    @Override
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        /*
         * The SQL scripts provided by EPSG contains some lines with only a "COMMIT" statement.
         * This statement is not understood by all databases, and interferes with our calls to
         * setAutoCommit(false) ... commit() / rollback().
         */
        if (CharSequences.equalsIgnoreCase(sql, "COMMIT")) {
            return 0;
        }
        if (statementToSkip != null && statementToSkip.reset(sql).matches()) {
            return 0;
        }
        if (replacePilcrow) {
            StringBuilders.replace(sql, "¶", "\n");
        }
        return super.execute(sql);
    }

    /**
     * Processes to the creation of the EPSG database using the files in the given directory.
     * The given directory should contain at least files similar to the following ones
     * (files without {@code ".sql"} extension are ignored):
     *
     * <ul>
     *   <li>{@code EPSG_v8_18.mdb_Tables_PostgreSQL.sql}</li>
     *   <li>{@code EPSG_v6_18.mdb_Data_PostgreSQL.sql}</li>
     *   <li>{@code EPSG_v6_18.mdb_FKeys_PostgreSQL.sql}</li>
     *   <li>Optional but recommended: {@code EPSG_v6_18.mdb_Indexes_PostgreSQL.sql}.</li>
     * </ul>
     *
     * The suffix may be different (for example {@code "_MySQL.sql"} instead of {@code "_PostgreSQL.sql"})
     * and the version number may be different.
     *
     * <p>If the given directory is {@code null}, then the scripts will be read from the resources.
     * If no resources is found, then an exception will be thrown.
     *
     * @throws IOException if an error occurred while reading an input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public void run(final Path scriptDirectory) throws SQLException, IOException {
        long time = System.nanoTime();
        log(Messages.getResources(null).getLogRecord(Level.INFO, Messages.Keys.CreatingSchema_2, Constants.EPSG, getURL()));
        int numScripts = SCRIPTS.length;
        if (!isGrantOnTableSupported) {
            numScripts--;
        }
        int numRows = 0;
        for (int i=0; i<numScripts; i++) {
            final String script = SCRIPTS[i] + ".sql";
            final InputStream in;
            if (scriptDirectory != null) {
                in = Files.newInputStream(scriptDirectory.resolve(script));
            } else {
                in = EPSGInstaller.class.getResourceAsStream(script);
                if (in == null) {
                    throw new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, script));
                }
            }
            numRows += run(script, in);
            // The stream will be closed by the run method.
        }
        time = System.nanoTime() - time;
        log(Messages.getResources(null).getLogRecord(
                PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS),
                Messages.Keys.InsertDuration_2, numRows, time / 1E9f));
    }

    /**
     * Returns a simplified form of the URL (truncated before the first ? or ; character),
     * for logging purpose only.
     */
    private String getURL() throws SQLException {
        String url = getConnection().getMetaData().getURL();
        int s1 = url.indexOf('?'); if (s1 < 0) s1 = url.length();
        int s2 = url.indexOf(';'); if (s2 < 0) s2 = url.length();
        return url.substring(0, Math.min(s1, s2));
    }

    /**
     * Logs a message reporting the failure to create EPSG database.
     */
    final void logFailure(final Locale locale) {
        String message = Messages.getResources(locale).getString(Messages.Keys.CanNotCreateSchema_1, Constants.EPSG);
        String status = status(locale);
        if (status != null) {
            message = message + ' ' + status;
        }
        log(new LogRecord(Level.WARNING, message));
    }

    /**
     * Logs the given record. This method pretend that the record has been logged by
     * {@code EPSGFactory.install(…)} because it is the public API using this class.
     */
    private static void log(final LogRecord record) {
        record.setLoggerName(Loggers.CRS_FACTORY);
        Logging.log(EPSGFactory.class, "install", record);
    }
}
