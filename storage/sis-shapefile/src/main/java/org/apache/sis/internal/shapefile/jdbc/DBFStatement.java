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
import org.apache.sis.util.ArgumentChecks;


/**
 * DBF Statement.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
final class DBFStatement extends AbstractStatement {
    /**
     * Connection this statement is relying on.
     */
    final DBFConnection connection;

    /**
     * The current result set, or {@code null} if none.
     */
    private DBFResultSet resultSet;

    /**
     * Indicates if the statement is currently closed.
     */
    private boolean isClosed;

    /**
     * Constructs a statement.
     *
     * @param connection Connection associated to this statement.
     */
    DBFStatement(final DBFConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns the connection.
     */
    @Override
    public Connection getConnection() throws SQLException {
        assertNotClosed();
        return connection;
    }

    /**
     * Executes the given SQL statement.
     */
    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        ArgumentChecks.ensureNonNull("sql", sql);
        assertNotClosed();
        if (resultSet != null) {
            resultSet.close();
        }
        return resultSet = new DBFResultSet(this);
    }

    /**
     * Returns the result set created by the last call to {@link #executeQuery(String)}.
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        assertNotClosed();
        return resultSet;
    }

    /**
     * Closes this statement.
     */
    @Override
    public void close() {
        if (resultSet != null) {
            resultSet.close();
        }
        isClosed = true;
    }

    /**
     * Returns {@code true} if this statement has been closed
     * or if the underlying connection is closed.
     */
    @Override
    public boolean isClosed() {
        return isClosed || connection.isClosed();
    }

    /**
     * Asserts that the connection and the statement are together opened.
     *
     * @throws SQLException if one of them is closed.
     */
    final void assertNotClosed() throws SQLException {
        connection.assertNotClosed();
        if (isClosed) {
            throw new SQLNonTransientException(Resources.format(Resources.Keys.ClosedStatement));
        }
    }
}
