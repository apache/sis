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
import java.util.Map;
import org.apache.sis.storage.shapefile.Database;


/**
 * DBF ResultSet.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
final class DBFResultSet extends AbstractResultSet {
    /**
     * Parent statement.
     */
    private final DBFStatement statement;

    /**
     * A copy of the {@link DBFConnection#database} reference, copied here for efficiency.
     */
    private final Database database;

    /**
     * Indicates if the ResultSet is closed.
     */
    private boolean isClosed;

    /**
     * The current records.
     */
    private Map<String, Object> records;

    /**
     * Constructs a result set.
     *
     * @param statement Parent statement.
     */
    DBFResultSet(final DBFStatement statement) {
        this.statement = statement;
        database = statement.connection.database;
    }

    /**
     * Returns the parent statement.
     */
    @Override
    public Statement getStatement() throws SQLException {
        assertNotClosed();
        return statement;
    }

    /**
     * Moves the cursor forward one row from its current position.
     */
    @Override
    public boolean next() throws SQLException {
        assertNotClosed();

        // Check that we aren't at the end of the Database file.
        final int remaining = database.getRecordCount() - database.getRowNum();
        if (remaining <= 0) {
            throw new SQLNonTransientException(Resources.format(Resources.Keys.NoMoreResults));
        }

        // TODO : Currently this function is only able to return String objects.
        records = database.readNextRowAsObjects();

        // Return the availability of a next record.
        return remaining > 1;
    }

    /**
     * Returns the value in the current row for the given column.
     */
    @Override
    public String getString(String columnLabel) throws SQLException {
        assertNotClosed();
        final Object value = records.get(columnLabel);
        if (value != null) {
            return (String) value;
        }
        throw new SQLNonTransientException(Resources.format(Resources.Keys.NoSuchColumn_1, columnLabel));
    }

    /**
     * Closes this result set.
     */
    @Override
    public void close() {
        isClosed = true;
    }

    /**
     * Returns {@code true} if this result set has been closed.
     */
    @Override
    public boolean isClosed() {
        return isClosed || statement.isClosed();
    }

    /**
     * Asserts that the connection, statement and result set are together opened.
     *
     * @throws SQLException if one of them is closed.
     */
    private void assertNotClosed() throws SQLException {
        statement.assertNotClosed();
        if (isClosed) {
            throw new SQLNonTransientException(Resources.format(Resources.Keys.ClosedResultSet));
        }
    }
}
