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
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.StringBuilders;


/**
 * Executes the installation scripts for the "metadata" schema in the "SpatialMetadata" database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final class Installer extends ScriptRunner {
    /**
     * List of enumeration types to replace by {@code VARCHAR}
     * on implementations that do not support {@code ENUM} type.
     */
    private final String[] enumTypes;

    /**
     * Creates a new installer for the metadata database.
     *
     * @param  connection  connection to the metadata database.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    Installer(final Connection connection) throws SQLException {
        super(connection, 100);
        if (isEnumTypeSupported) {
            enumTypes = null;
        } else {
            enumTypes = new String[] {
                "RoleCode", "DateTypeCode", "PresentationFormCode", "OnLineFunctionCode", "TransferFunctionTypeCode"
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
        run(Installer.class, "Citations.sql");
        run(Installer.class, "Contents.sql");
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
