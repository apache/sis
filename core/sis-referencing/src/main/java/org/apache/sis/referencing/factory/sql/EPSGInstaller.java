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
import java.io.BufferedReader;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Fallback;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.setup.InstallationResources;

import static org.apache.sis.internal.util.Constants.EPSG;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.BiFunction;


/**
 * Runs the SQL scripts for creating an EPSG database.
 *
 * See {@code org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter}
 * in the test directory for more information about how the scripts are formatted.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
final class EPSGInstaller extends ScriptRunner {
    /**
     * The pattern for an {@code "UPDATE … SET … REPLACE"} instruction.
     * Example:
     *
     * {@preformat sql
     *     UPDATE epsg_datum
     *     SET datum_name = replace(datum_name, CHAR(182), CHAR(10));
     * }
     *
     * Note: this regular expression use a capturing group.
     */
    static final String REPLACE_STATEMENT =
            "UPDATE\\s+[\\w\\.\" ]+\\s+SET\\s+(\\w+)\\s*=\\s*replace\\s*\\(\\s*\\1\\W+.*";

    /**
     * {@code true} if the Pilcrow character (¶ - decimal code 182) should be replaced by Line Feed
     * (LF - decimal code 10). This is a possible workaround when the database does not support the
     * {@code REPLACE(column, CHAR(182), CHAR(10))} SQL statement, but accepts LF.
     */
    private final boolean replacePilcrow;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     * The encoding is {@code "ISO-8859-1"}, which is the encoding used for the files provided by EPSG.
     *
     * @param  connection  the connection to the database.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public EPSGInstaller(final Connection connection) throws SQLException {
        super(connection, 100);
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
        if (!isReplaceSupported) {
            addStatementToSkip(REPLACE_STATEMENT);
        }
        /*
         * The SQL scripts provided by EPSG contains some lines with only a "COMMIT" statement.
         * This statement is not understood by all databases, and interferes with our calls to
         * setAutoCommit(false) ... commit() / rollback().
         */
        addStatementToSkip("COMMIT");
        replacePilcrow = false;         // Never supported for now.
    }

    /**
     * Creates immediately a schema of the given name in the database and remember that the
     * {@code "epsg_"} prefix in table names will need to be replaced by path to that schema.
     *
     * <p>This method should be invoked only once. It does nothing if the database does not supports schema.</p>
     *
     * @param  schema  the schema (usually {@code "epsg"}).
     * @throws SQLException if the schema can not be created.
     * @throws IOException if an I/O operation was required and failed.
     */
    public void setSchema(final String schema) throws SQLException, IOException {
        if (isSchemaSupported) {
            /*
             * Creates the schema on the database. We do that before to setup the 'toSchema' map, while the map still null.
             * Note that we do not quote the schema name, which is a somewhat arbitrary choice.
             */
            execute(new StringBuilder("CREATE SCHEMA ").append(identifierQuote).append(schema).append(identifierQuote));
            if (isGrantOnSchemaSupported) {
                execute(new StringBuilder("GRANT USAGE ON SCHEMA ")
                        .append(identifierQuote).append(schema).append(identifierQuote).append(" TO ").append(PUBLIC));
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
            if (isEnumTypeSupported) {
                addReplacement(SQLTranslator.TABLE_PREFIX + "datum_kind",             "Datum Kind");
                addReplacement(SQLTranslator.TABLE_PREFIX + "crs_kind",               "CRS Kind");
                addReplacement(SQLTranslator.TABLE_PREFIX + "cs_kind",                "CS Kind");
                addReplacement(SQLTranslator.TABLE_PREFIX + "table_name",             "Table Name");
            }
            prependNamespace(schema);
        }
        if (!isEnumTypeSupported) {
            addReplacement(SQLTranslator.TABLE_PREFIX + "datum_kind", TableInfo.ENUM_REPLACEMENT);
            addReplacement(SQLTranslator.TABLE_PREFIX + "crs_kind",   TableInfo.ENUM_REPLACEMENT);
            addReplacement(SQLTranslator.TABLE_PREFIX + "cs_kind",    TableInfo.ENUM_REPLACEMENT);
            addReplacement(SQLTranslator.TABLE_PREFIX + "table_name", "VARCHAR(80)");
        }
    }

    /**
     * Prepends the given schema or catalog to all table names.
     */
    final void prependNamespace(final String schema) {
        modifyReplacements(new BiFunction<String,String,String>() {
            @Override public String apply(String key, String value) {
                if (key.startsWith(SQLTranslator.TABLE_PREFIX)) {
                    final StringBuilder buffer = new StringBuilder(value.length() + schema.length() + 5);
                    buffer.append(identifierQuote).append(schema).append(identifierQuote).append('.');
                    final boolean isQuoted = value.endsWith(identifierQuote);
                    if (!isQuoted) buffer.append(identifierQuote);
                    buffer.append(value);
                    if (!isQuoted) buffer.append(identifierQuote);
                    value = buffer.toString();
                }
                return value;
            }
        });
    }

    /**
     * Invoked for each text found in a SQL statement. This method replaces {@code ''} by {@code Null}.
     * The intend is to consistently use the null value for meaning "no information", which is not the
     * same than "information is an empty string". This replacement is okay in this particular case
     * since there is no field in the EPSG database for which we really want an empty string.
     *
     * @param sql    the whole SQL statement.
     * @param lower  index of the first character of the text in {@code sql}.
     * @param upper  index after the last character of the text in {@code sql}.
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
     * @throws IOException  if an I/O operation was required and failed.
     */
    @Override
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        if (replacePilcrow) {
            StringBuilders.replace(sql, "¶", "\n");
        }
        return super.execute(sql);
    }

    /**
     * Processes to the creation of the EPSG database using the SQL scripts from the given provider.
     *
     * @param  scriptProvider  user-provided scripts, or {@code null} for automatic lookup.
     * @throws IOException  if an error occurred while reading an input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public void run(InstallationResources scriptProvider, final Locale locale) throws SQLException, IOException {
        long time = System.nanoTime();
        InstallationScriptProvider.log(Messages.getResources(locale).getLogRecord(Level.INFO,
                Messages.Keys.CreatingSchema_2, EPSG, SQLUtilities.getSimplifiedURL(getConnection().getMetaData())));
        if (scriptProvider == null) {
            scriptProvider = lookupProvider(locale);
        }
        final String[] scripts = scriptProvider.getResourceNames(EPSG);
        int numRows = 0;
        for (int i=0; i<scripts.length; i++) {
            try (BufferedReader in = scriptProvider.openScript(EPSG, i)) {
                numRows += run(scripts[i], in);
            }
        }
        time = System.nanoTime() - time;
        InstallationScriptProvider.log(Messages.getResources(locale).getLogRecord(
                PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS),
                Messages.Keys.InsertDuration_2, numRows, time / 1E9f));
    }

    /**
     * Searches for a SQL script provider on the classpath before to fallback on the default provider.
     */
    private static InstallationResources lookupProvider(final Locale locale) throws IOException {
        InstallationResources fallback = null;
        for (final InstallationResources provider : DefaultFactories.createServiceLoader(InstallationResources.class)) {
            if (provider.getAuthorities().contains(EPSG)) {
                if (provider.getClass().isAnnotationPresent(Fallback.class)) {
                    return provider;
                }
                fallback = provider;
            }
        }
        return (fallback != null) ? fallback : new InstallationScriptProvider.Default(locale);
    }

    /**
     * Logs a message reporting the failure to create EPSG database. This method is invoked when {@link EPSGFactory}
     * caught an exception. This log completes rather than replaces the exception message since {@code EPSGFactory}
     * lets the exception propagate. Another code (for example {@link org.apache.sis.referencing.CRS#forCode(String)})
     * may catch that exception and log another record with the exception message.
     */
    final void logFailure(final Locale locale, final Exception cause) {
        String message = Messages.getResources(locale).getString(Messages.Keys.CanNotCreateSchema_1, EPSG);
        String status = status(locale);
        if (status != null) {
            message = message + ' ' + status;
        }
        message = Exceptions.formatChainedMessages(locale, message, cause);
        InstallationScriptProvider.log(new LogRecord(Level.WARNING, message));
    }
}
