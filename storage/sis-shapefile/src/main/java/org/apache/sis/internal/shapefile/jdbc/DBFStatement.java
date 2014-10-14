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

import static java.util.logging.Level.*;

import java.sql.*;
import java.util.*;

/**
 * DBF Statement.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFStatement extends AbstractStatement {
    /** Connection this statement is relying on. */
    private DBFConnection m_parentConnection;

    /** Indicate if the statement is currently closed. */
    boolean m_closed;

    /**
     * Create a statement.
     * @param connection Connection associated to this statement.
     */
    DBFStatement(DBFConnection connection) {
        Objects.requireNonNull(connection, "The parent Connection of the ResulSet cannot be null.");

        m_parentConnection = connection;
        m_closed = false;
    }

    /**
     * @see java.sql.Statement#executeQuery(java.lang.String)
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        Objects.requireNonNull(sql, "The SQL query for executeQuery cannot be null.");

        assertNotClosed();
        return new DBFResultSet(this);
    }

    /**
     * @see java.sql.Statement#close()
     */
    @Override
    public void close() {
        m_closed = true;
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String)
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        Objects.requireNonNull(sql, "The SQL query for execute cannot be null.");
        return false;
    }

    /**
     * @see java.sql.Statement#getResultSet()
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        assertNotClosed();
        return new DBFResultSet(this);
    }


    /**
     * @see java.sql.Statement#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (m_parentConnection.isClosed())
            throw new SQLException(this.format(SEVERE, "excp.closed_connection"));
        else
            return m_parentConnection;
    }

    /**
     * @see java.sql.Statement#isClosed()
     */
    @Override
    public boolean isClosed() {
        assert(m_parentConnection != null);
        return m_parentConnection.isClosed() || m_closed;
    }

    /**
     * Asserts that the connection and the statement are together opened.
     * @throws SQLException if one of them is closed.
     */
    private void assertNotClosed() throws SQLException {
        if (isClosed())
            throw new SQLException(this.format(SEVERE, "excp.closed_connection"));
    }
}
