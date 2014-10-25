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
package org.apache.sis.internal.shapefile.jdbc;

import java.sql.*;
import java.io.File;
import java.io.IOException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.shapefile.Database;


/**
 * Connection to a DBF database.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
final class DBFConnection extends AbstractConnection {
    /**
     * The object to use for reading the database content.
     */
    final Database database;

    /**
     * Constructs a connection to the given database.
     *
     * @param  datafile Data file ({@code .dbf} extension).
     * @throws IOException if the given file is invalid.
     */
    DBFConnection(final File datafile) throws SQLException, IOException {
        // Check that file exists.
        if (!datafile.exists()) {
            throw new SQLException(Errors.format(Errors.Keys.FileNotFound_1, datafile.getAbsolutePath()));
        }
        // Check that its not a directory.
        if (datafile.isDirectory()) {
            throw new SQLException(Errors.format(Errors.Keys.DirectoryNotExpected_1, datafile.getAbsolutePath()));
        }
        database = new Database(datafile.getAbsolutePath());
        database.loadDescriptor();
    }

    /**
     * Creates an object for sending SQL statements to the database.
     */
    @Override
    public Statement createStatement() throws SQLException {
        assertNotClosed();
        return new DBFStatement(this);
    }

    /**
     * Closes the connection to the database.
     */
    @Override
    public void close() throws SQLException {
        try {
            database.close();
        } catch (IOException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns {@code true} if this connection has been closed.
     */
    @Override
    public boolean isClosed() {
        return database.isClosed();
    }

    /**
     * Asserts that the connection is opened.
     *
     * @throws SQLException if the connection is closed.
     */
    final void assertNotClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLNonTransientException(Resources.format(Resources.Keys.ClosedConnection));
        }
    }

    /**
     * Returns {@code true} if the connection has not been closed and is still valid.
     * The timeout parameter is ignored and this method bases itself only on {@link #isClosed()} state.
     */
    @Override
    public boolean isValid(@SuppressWarnings("unused") int timeout) {
        return !isClosed();
    }
}
