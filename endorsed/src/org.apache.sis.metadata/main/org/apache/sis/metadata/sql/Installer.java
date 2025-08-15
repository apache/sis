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
package org.apache.sis.metadata.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.sis.metadata.sql.privy.ScriptRunner;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.StringBuilders;


/**
 * Executes the installation scripts for the "metadata" schema in the "SpatialMetadata" database.
 *
 * @todo We should replace the SQL {@code "CREATE TABLE"} statements in SQL scripts by something like
 *       {@code "GENERATE TABLE"}, to be handled in a special way by this {@code Installer} class.
 *       The {@code "GENERATE TABLE"} statement would enumerate only the columns, and this installer
 *       would delegate to {@link MetadataWriter} for inferring the column types and the {@code ENUM}
 *       dependencies.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Installer extends ScriptRunner {
    /**
     * List of enumeration types to replace by {@code VARCHAR}
     * on implementations that do not support {@code ENUM} type.
     *
     * @todo This field can be removed if we apply the "todo" documented in class javadoc.
     */
    private final String[] enumTypes;

    /**
     * Creates a new installer for the metadata database.
     *
     * @param  connection  connection to the metadata database.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    Installer(final Connection connection) throws SQLException {
        super(connection, null, 100);
        if (isEnumTypeSupported) {
            enumTypes = null;
        } else {
            enumTypes = new String[] {
                "RoleCode", "DateTypeCode", "PresentationFormCode", "OnLineFunctionCode", "TransferFunctionTypeCode",
                "AxisDirection"
            };
            for (int i=0; i<enumTypes.length; i++) {
                enumTypes[i] = "metadata.\"" + enumTypes[i] + '"';
            }
        }
    }

    /**
     * Runs the installation scripts.
     */
    public void run() throws IOException, SQLException {
        final String[] scripts = {
            "Citations.sql",
            "Contents.sql",
            "Metadata.sql",
            "Referencing.sql"
        };
        for (final String filename : scripts) {
            run(filename, Installer.class.getResourceAsStream(filename));
        }
    }

    /**
     * Invoked for each line of the SQL installation script to execute.
     * If the database does not support enumerations, replaces enumeration columns by {@code VARCHAR}.
     */
    @Override
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        if (!isEnumTypeSupported && CharSequences.startsWith(sql, "CREATE TABLE", true)) {
            for (final String type : enumTypes) {
                StringBuilders.replace(sql, type, "VARCHAR(25)");
            }
        }
        return super.execute(sql);
    }
}
