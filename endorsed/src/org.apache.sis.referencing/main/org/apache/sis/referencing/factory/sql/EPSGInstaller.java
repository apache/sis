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
import java.util.Map;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.sis.util.privy.Constants;
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
     * The quoted identifiers to replace, or an empty map if none.
     * Used for replacing enumeration types when not supported by the target database.
     */
    private final Map<String, String> identifierReplacements;

    /**
     * Whether to apply the replacements in the {@link #identifierReplacements} map.
     * Used for temporarily disabling the replacements during the execution of the
     * {@code Prepare.sql} script, which defines the enumerations.
     */
    private boolean applyReplacements;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     * This constructor creates immediately a schema of the given name in the database.
     *
     * @param  connection  the connection to the database.
     * @param  schema      the schema, or {@code null} for any, or empty for none.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public EPSGInstaller(final Connection connection, final String schema) throws SQLException {
        super(connection, schema == null ? Constants.EPSG : schema.isEmpty() ? null : schema, 100);
        if (isEnumTypeSupported) {
            identifierReplacements = Map.of();
        } else {
            identifierReplacements = Map.of(
                    "Datum Kind",        "VARCHAR(16)",    // Original: VARCHAR(24) for column "datum_type".
                    "CRS Kind",          "VARCHAR(13)",    // Original: VARCHAR(24) for column "coord_ref_sys_kind".
                    "CS Kind",           "VARCHAR(15)",    // Original: VARCHAR(24) for column "coord_sys_type".
                    "Supersession Type", "VARCHAR(12)",    // Original: VARCHAR(50) for column "supersession_type".
                    "Table Name",        "VARCHAR(36)");   // Original: VARCHAR(80) for columns "object_table_name".
        }
    }

    /**
     * Invoked for each text found in a SQL statement. This method replaces {@code ''} by {@code Null}.
     * The intent is to consistently use the null value for meaning "no information", which is not the
     * same as "information is an empty string". This replacement is okay in this particular case
     * since there is no field in the EPSG database for which we really want an empty string.
     *
     * @param sql    the whole <abbr>SQL</abbr> statement.
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
            case 2: {
                replacement = "Null";   // Replace '' by Null for every table.
                break;
            }
        }
        sql.replace(lower, upper, replacement);
    }

    /**
     * Replaces an enumeration type by a standard type if enumerations are not supported.
     *
     * @param sql    the whole <abbr>SQL</abbr> statement.
     * @param lower  index of the first character of the identifier in {@code sql}.
     * @param upper  index after the last character of the identifier in {@code sql}.
     */
    @Override
    protected void editQuotedIdentifier(final StringBuilder sql, final int lower, final int upper) {
        if (applyReplacements) {
            final int n = identifierQuote.length();
            final String r = identifierReplacements.get(sql.substring(lower + n, upper - n));
            if (r != null) {
                sql.replace(lower, upper, r);   // This replacement removes the quotes.
            }
        }
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
                Constants.EPSG,
                SQLUtilities.getSimplifiedURL(getConnection().getMetaData())));

        int numRows = 0;    // For logging purpose only.
        final String[] scripts = scriptProvider.getResourceNames(Constants.EPSG);
        for (int i=0; i<scripts.length; i++) {
            final String script = scripts[i];
            applyReplacements = (i != 0) && !identifierReplacements.isEmpty();
            try (BufferedReader in = scriptProvider.openScript(Constants.EPSG, i)) {
                numRows += run(script, in);
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
            if (provider.getAuthorities().contains(Constants.EPSG)) {
                if (!provider.getClass().isAnnotationPresent(Fallback.class)) {
                    return provider;
                }
                fallback = provider;
            }
        }
        /*
         * If we did not found a provider ready to use such as `sis-epsg.jar` or `sis-embedded.jar`,
         * we may fallback on a provider offering to download the data (those fallbacks are provided
         * by `org.apache.sis.console` and `org.apache.sis.gui` modules). Those fallbacks will ask to
         * the users if they accept the EPSG Terms of Use.
         */
        return fallback;
    }

    /**
     * Creates a message reporting the failure to create EPSG database. This method is invoked when {@link EPSGFactory}
     * caught an exception. This method completes the exception message with the file name and line number where the
     * error occurred, if such information is available.
     */
    final String failure(final Locale locale) {
        String message = Messages.forLocale(locale).getString(Messages.Keys.CanNotCreateSchema_1, Constants.EPSG);
        String status = status(locale);
        if (status != null) {
            message = message + ' ' + status;
        }
        return message;
    }
}
