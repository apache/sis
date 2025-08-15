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

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.postgresql.ds.PGSimpleDataSource;
import org.apache.sis.referencing.factory.sql.SQLTranslator;


/**
 * A command-line tool for updating the <abbr>EPSG</abbr> {@code Tables.sql} file distributed by Apache <abbr>SIS</abbr>.
 * This application makes many assumptions about how {@code Tables.sql} is formatted in the <abbr>SIS</abbr> repository.
 * Therefore, this application is valid only for that specific file.
 * The steps to follow are documented in the {@code README.md} file.
 *
 * <p>This application requires a connection to a PostgreSQL database of the given name on the local host.
 * It only rewrites the {@code VARCHAR} types with the minimal length which is required for holding the data.
 * Other changes must be done manually as documented in {@code README.md}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TableScriptUpdater {
    /**
     * The statement to search for identifying the table name.
     */
    private static final String CREATE = "CREATE TABLE \"";

    /**
     * The keyword to search in order to update them.
     */
    private static final String VARCHAR = "VARCHAR(";

    /**
     * The file to update.
     */
    private final Path file;

    /**
     * All lines in the file.
     */
    private final List<String> lines;

    /**
     * Creates a new checker.
     *
     * @param file  the file to update.
     * @throws IOException if an error occurred while reading the {@code Tables.sql} file.
     */
    private TableScriptUpdater(final Path file) throws IOException {
        this.file = file;
        lines = Files.readAllLines(file);
    }

    /**
     * Verifies all lines that have been read.
     *
     * @param  database  the database on which to connect.
     * @throws SQLException if an error occurred while querying the <abbr>EPSG</abbr> database.
     */
    private void update(final String database) throws SQLException {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(database);
        try (Connection connection = dataSource.getConnection();
             Statement  statement  = connection.createStatement())
        {
            final var translator = new SQLTranslator(connection.getMetaData(), null, null);
            String table = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(CREATE)) {
                    table = translator.toActualTableName(line.substring(CREATE.length(), line.indexOf('"', CREATE.length())));
                    continue;
                }
                if (line.isBlank() || !Character.isLowerCase(line.codePointAt(0))) {
                    continue;   // In `Tables.sql` all columns are in lower case.
                }
                final String column = line.substring(0, line.indexOf(' '));
                if (table == null) {
                    throw new RuntimeException("CREATE TABLE statement not found before column " + column);
                }
                /*
                 * Check if a nullable column should be declared not null or the converse.
                 * Note that EPSG sometime stores empty texts, which we replace by nulls.
                 * Remarks are kept optional even if EPSG always provide a value.
                 */
                final boolean hasNull;
                if (column.equals("remarks") || column.equals("description") || column.equals("formula")) {
                    hasNull = true;
                } else try (ResultSet result = statement.executeQuery(
                        "SELECT " + column + " FROM " + table + " WHERE "
                                  + column + " IS NULL OR CAST(" + column + " AS VARCHAR(4000)) = ''"))
                {
                    hasNull = result.next();
                }
                if (hasNull == line.contains("NOT NULL")) {
                    if (hasNull) {
                        line = line.replace("NOT NULL", "");
                    } else {
                        final int s = line.lastIndexOf(',');
                        line = line.substring(0, s) + " NOT NULL" + line.substring(s);
                    }
                }
                /*
                 * Restrict the length of a `VARCHAR` to the smallest length which is sufficient for all data.
                 */
                int start = line.indexOf(VARCHAR, column.length());
                if (start >= 0) {
                    int length = 1;     // Minimal length.
                    try (ResultSet result = statement.executeQuery("SELECT MAX(LENGTH(" + column + ")) FROM " + table)) {
                        while (result.next()) {
                            length = Math.max(length, result.getInt(1));
                        }
                    }
                    start += VARCHAR.length();      // Start of the string where the length is written.
                    final int end = line.indexOf(')', start);
                    line = line.substring(0, start) + length + line.substring(end);
                }
                lines.set(i, line);
            }
        }
    }

    /**
     * Writes the updated {@code Tables.sql} file.
     */
    private void write() throws IOException {
        Files.write(file, lines);
    }

    /**
     * Updates the {@code Tables.sql} file.
     * This method expects two arguments:
     *
     * <ol>
     *   <li>The file of the <abbr>SQL</abbr> script to update, which must exist.</li>
     *   <li>The name of a PostgreSQL database containing the <abbr>EPSG</abbr> geodetic dataset.</li>
     * </ol>
     *
     * @param  arguments  the files and the destination file.
     * @throws IOException if an error occurred while reading of writing the {@code Tables.sql} file.
     * @throws SQLException if an error occurred while querying the <abbr>EPSG</abbr> database.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] arguments) throws IOException, SQLException {
        if (arguments.length != 2) {
            System.err.println("Expected two arguments: SQL file and PostgreSQL database.");
            return;
        }
        final var t = new TableScriptUpdater(Path.of(arguments[0]));
        t.update(arguments[1]);
        t.write();
    }
}
