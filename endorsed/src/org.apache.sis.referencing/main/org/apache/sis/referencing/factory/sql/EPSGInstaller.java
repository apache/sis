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
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.privy.Constants;
import static org.apache.sis.util.privy.Constants.EPSG;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.metadata.sql.privy.ScriptRunner;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.system.Fallback;
import org.apache.sis.setup.InstallationResources;


/**
 * Runs the <abbr>SQL</abbr> scripts for creating an <abbr>EPSG</abbr> database.
 *
 * See {@code org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter}
 * in the test directory for more information about how the scripts are formatted.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class EPSGInstaller extends ScriptRunner {
    /**
     * The pattern for an {@code "UPDATE … SET … REPLACE"} instruction.
     * Example:
     *
     * {@snippet lang="sql" :
     *     UPDATE epsg_datum
     *     SET datum_name = replace(datum_name, CHAR(182), CHAR(10));
     *     }
     *
     * Note: this regular expression uses a capturing group.
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
        final DatabaseMetaData metadata = connection.getMetaData();
        final String[] functions = metadata.getStringFunctions().split("\\s*,\\s*");
        if (!ArraysExt.containsIgnoreCase(functions, "REPLACE")) {
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
     * Creates immediately a schema of the given name in the database.
     * The {@code "epsg_"} prefix in table names or the {@code "epsg"}
     * schema name will be replaced by the given schema name.
     *
     * <p>This method should be invoked only once. It does nothing if the database does not supports schema.</p>
     *
     * @param  schema  the schema (usually {@code "epsg"}).
     * @throws SQLException if the schema cannot be created.
     * @throws IOException if an I/O operation was required and failed.
     */
    public void setSchema(final String schema) throws SQLException, IOException {
        if (isSchemaSupported) {
            /*
             * Creates the schema on the database.
             * Note that we do not quote the schema name, which is a somewhat arbitrary choice.
             */
            final var sb = new StringBuilder(40);
            execute(sb.append("CREATE SCHEMA ").append(identifierQuote).append(schema).append(identifierQuote));
            if (isGrantOnSchemaSupported) {
                sb.setLength(0);
                execute(sb.append("GRANT USAGE ON SCHEMA ")
                        .append(identifierQuote).append(schema).append(identifierQuote).append(" TO ").append(PUBLIC));
            }
            /*
             * Mapping from the table names used in the SQL scripts to the names used in `EPSGDataAccess`.
             * Those names are the ones that were used in the original EPSG dataset in MS-Access database.
             * We use those original names because they are easier to read than the names in SQL scripts.
             *
             * TODO: move to org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter.
             */
            addReplacement(SQLTranslator.TABLE_PREFIX + "alias",                      "Alias");
            addReplacement(SQLTranslator.TABLE_PREFIX + "area",                       "Area");      // Deprecated (removed in EPSG 10)
            addReplacement(SQLTranslator.TABLE_PREFIX + "change",                     "Change");
            addReplacement(SQLTranslator.TABLE_PREFIX + "conventionalrs",             "ConventionalRS");
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
            addReplacement(SQLTranslator.TABLE_PREFIX + "datumensemble",              "DatumEnsemble");
            addReplacement(SQLTranslator.TABLE_PREFIX + "datumensemblemember",        "DatumEnsembleMember");
            addReplacement(SQLTranslator.TABLE_PREFIX + "datumrealizationmethod",     "DatumRealizationMethod");
            addReplacement(SQLTranslator.TABLE_PREFIX + "definingoperation",          "DefiningOperation");
            addReplacement(SQLTranslator.TABLE_PREFIX + "deprecation",                "Deprecation");
            addReplacement(SQLTranslator.TABLE_PREFIX + "ellipsoid",                  "Ellipsoid");
            addReplacement(SQLTranslator.TABLE_PREFIX + "extent",                     "Extent");
            addReplacement(SQLTranslator.TABLE_PREFIX + "namingsystem",               "Naming System");
            addReplacement(SQLTranslator.TABLE_PREFIX + "primemeridian",              "Prime Meridian");
            addReplacement(SQLTranslator.TABLE_PREFIX + "scope",                      "Scope");
            addReplacement(SQLTranslator.TABLE_PREFIX + "supersession",               "Supersession");
            addReplacement(SQLTranslator.TABLE_PREFIX + "unitofmeasure",              "Unit of Measure");
            addReplacement(SQLTranslator.TABLE_PREFIX + "usage",                      "Usage");
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
            addReplacement(SQLTranslator.TABLE_PREFIX + "table_name", TableInfo.ENUM_REPLACEMENT);
        }
    }

    /**
     * Prepends the given schema or catalog to all table names.
     */
    final void prependNamespace(final String schema) {
        modifyReplacements((key, value) -> {
            if (key.startsWith(SQLTranslator.TABLE_PREFIX)) {
                final var buffer = new StringBuilder(value.length() + schema.length() + 5);
                buffer.append(identifierQuote).append(schema).append(identifierQuote).append('.');
                final boolean isQuoted = value.endsWith(identifierQuote);
                if (!isQuoted) buffer.append(identifierQuote);
                buffer.append(value);
                if (!isQuoted) buffer.append(identifierQuote);
                value = buffer.toString();
            }
            return value;
        });
    }

    /**
     * Invoked for each text found in a SQL statement. This method replaces {@code ''} by {@code Null}.
     * The intent is to consistently use the null value for meaning "no information", which is not the
     * same as "information is an empty string". This replacement is okay in this particular case
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
     * @param  locale          the locale for information or warning messages, if any.
     * @return whether the database has been installed.
     * @throws FileNotFoundException if a SQL script has not been found.
     * @throws IOException  if another error occurred while reading an input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public boolean run(InstallationResources scriptProvider, final Locale locale) throws SQLException, IOException {
        long time = System.nanoTime();
        if (scriptProvider == null) {
            scriptProvider = lookupProvider(locale);
            if (scriptProvider == null) {
                return false;
            }
        }
        InstallationScriptProvider.log(Messages.forLocale(locale).createLogRecord(
                Level.INFO,
                Messages.Keys.CreatingSchema_2,
                EPSG,
                SQLUtilities.getSimplifiedURL(getConnection().getMetaData())));
        final String[] scripts = scriptProvider.getResourceNames(EPSG);
        int numRows = 0;
        for (int i=0; i<scripts.length; i++) {
            try (BufferedReader in = scriptProvider.openScript(EPSG, i)) {
                numRows += run(scripts[i], in);
            }
        }
        time = System.nanoTime() - time;
        InstallationScriptProvider.log(Messages.forLocale(locale).createLogRecord(
                PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS),
                Messages.Keys.InsertDuration_2,
                numRows,
                time / (float) Constants.NANOS_PER_SECOND));
        return true;
    }

    /**
     * Searches for a SQL script provider on the module path before to fallback on the default provider.
     * The returned provider will be, in preference order:
     *
     * <ol>
     *   <li>A provider from a publicly supported dependency such as {@code sis-epsg.jar} or {@code sis-embedded.jar}.
     *       Users have to put one of those dependencies in the module path themselves. This action is interpreted as
     *       an acceptance of EPSG terms of use, so no license agreement window will popup.</li>
     *   <li>A provider using the SQL scripts in the {@code $SIS_DATA/Databases/ExternalSources} directory.
     *       Users have to put those scripts in that directory manually. This action is interpreted as an
     *       acceptance of EPSG terms of use, so no license agreement window will popup.</li>
     *   <li>A provider offering users to automatically download the data. Those providers are defined by
     *       {@code org.apache.sis.console} and {@code org.apache.sis.gui} modules.
     *       Users must accept EPSG terms of use before the database can be installed.
     * </ol>
     *
     * @param  locale  the locale for information or warning messages, if any.
     * @return the SQL preferred script provider, or {@code null} if none.
     */
    private static InstallationResources lookupProvider(final Locale locale) throws IOException {
        InstallationResources fallback = null;
        for (final InstallationResources provider : InstallationResources.load()) {
            if (provider.getAuthorities().contains(EPSG)) {
                if (!provider.getClass().isAnnotationPresent(Fallback.class)) {
                    return provider;
                }
                fallback = provider;
            }
        }
        /*
         * If we did not found a provider ready to use such as "sis-epsg.jar" or "sis-embedded.jar",
         * we may fallback on a provider offering to download the data (those fallbacks are provided
         * by `org.apache.sis.console` and `org.apache.sis.gui` modules). Those fallbacks will ask to
         * the user if (s)he accepts EPSG terms of use. But before to use those fallbacks, check if the
         * data have not been downloaded manually in the "$SIS_DATA/Databases/ExternalSources" directory.
         */
        final var manual = new InstallationScriptProvider.Default(locale);
        return manual.getAuthorities().isEmpty() ? fallback : manual;
    }

    /**
     * Creates a message reporting the failure to create EPSG database. This method is invoked when {@link EPSGFactory}
     * caught an exception. This method completes the exception message with the file name and line number where the
     * error occurred, if such information is available.
     */
    final String failure(final Locale locale) {
        String message = Messages.forLocale(locale).getString(Messages.Keys.CanNotCreateSchema_1, EPSG);
        String status = status(locale);
        if (status != null) {
            message = message + ' ' + status;
        }
        return message;
    }
}
