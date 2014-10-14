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

import org.apache.sis.storage.shapefile.Database;
import static java.util.logging.Level.*;

import java.math.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.*;

import org.apache.sis.storage.shapefile.*;

/**
 * DBF ResultSet.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFResultSet extends AbstractResultSet {
    /** Parent statement. */
    private DBFStatement m_parentStatement;

    /** Indicate if the ResultSet is closed. */
    private boolean m_closed;

    /** Current record. */
    private HashMap<String, Object> m_currentRecord;

    /**
     * Constructs a ResultSet.
     * @param parentStatement Parent statement.
     */
    DBFResultSet(DBFStatement parentStatement) {
        Objects.requireNonNull(parentStatement, "The parent Statement of the ResulSet cannot be null.");

        m_parentStatement = parentStatement;
        m_closed = false;
    }

    /**
     * @see java.sql.ResultSet#next()
     */
    @Override
    public boolean next() throws SQLException {
        assertNotClosed();

        // Check that we aren't at the end of the Database file.
        if (getDatabase().getRowNum() >= getDatabase().getRecordCount()) {
            String message = format(Level.SEVERE, "excp.no_more_results");
            throw new SQLException(message);
        }

        // TODO : Currently this function is only able to return String objects.
        m_currentRecord = getDatabase().readNextRowAsObjects();

        // Return the availability of a next record.
        boolean nextRecord = getDatabase().getRowNum() < getDatabase().getRecordCount();
        return nextRecord;
    }

    /**
     * @see java.sql.ResultSet#close()
     */
    @Override
    public void close() {
        m_closed = true;
    }

    /**
     * @see java.sql.ResultSet#isClosed()
     */
    @Override
    public boolean isClosed() {
        return m_closed || this.m_parentStatement.isClosed();
    }

    /**
     * @see java.sql.ResultSet#getString(int)
     */
    @Override
    public String getString(int columnIndex) throws SQLException {
        assertNotClosed();

        return null;
    }

    /**
     * @see java.sql.ResultSet#getBoolean(int)
     */
    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        assertNotClosed();
        return false;
    }

    /**
     * @see java.sql.ResultSet#getByte(int)
     */
    @Override
    public byte getByte(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getShort(int)
     */
    @Override
    public short getShort(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getInt(int)
     */
    @Override
    public int getInt(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getLong(int)
     */
    @Override
    public long getLong(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getFloat(int)
     */
    @Override
    public float getFloat(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getDouble(int)
     */
    @Override
    public double getDouble(int columnIndex) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(int, int)
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getBytes(int)
     */
    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getDate(int)
     */
    @Override
    public Date getDate(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getTime(int)
     */
    @Override
    public Time getTime(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getTimestamp(int)
     */
    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getString(java.lang.String)
     */
    @Override
    public String getString(String columnLabel) throws SQLException {
        assertNotClosed();
        return (String)m_currentRecord.get(columnLabel);
    }

    /**
     * @see java.sql.ResultSet#getBoolean(java.lang.String)
     */
    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        assertNotClosed();
        return false;
    }

    /**
     * @see java.sql.ResultSet#getByte(java.lang.String)
     */
    @Override
    public byte getByte(String columnLabel) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getShort(java.lang.String)
     */
    @Override
    public short getShort(String columnLabel) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getInt(java.lang.String)
     */
    @Override
    public int getInt(String columnLabel) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getLong(java.lang.String)
     */
    @Override
    public long getLong(String columnLabel) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getFloat(java.lang.String)
     */
    @Override
    public float getFloat(String columnLabel) throws SQLException {
        assertNotClosed();
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getDouble(java.lang.String)
     */
    @Override
    public double getDouble(String columnLabel) throws SQLException {
        assertNotClosed();
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String, int)
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getBytes(java.lang.String)
     */
    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getDate(java.lang.String)
     */
    @Override
    public Date getDate(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getTime(java.lang.String)
     */
    @Override
    public Time getTime(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getTimestamp(java.lang.String)
     */
    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }


    /**
     * @see java.sql.ResultSet#getMetaData()
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getObject(int)
     */
    @Override
    public Object getObject(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getObject(java.lang.String)
     */
    @Override
    public Object getObject(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(int)
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String)
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        assertNotClosed();
        return null;
    }

    /**
     * @see java.sql.ResultSet#getStatement()
     */
    @Override
    public Statement getStatement() throws SQLException {
        assertNotClosed();
        return m_parentStatement;
    }

    /**
     * Asserts that the connection and the statement are together opened.
     * @throws SQLException if one of them is closed.
     */
    private void assertNotClosed() throws SQLException {
        if (isClosed())
            throw new SQLException(this.format(SEVERE, "excp.closed_connection"));
    }

    /**
     * Return the underlying database binary representation.
     * @return Database.
     * @throws SQLException if the database is closed.
     */
    private Database getDatabase() throws SQLException {
        assertNotClosed();
        return ((DBFConnection)m_parentStatement.getConnection()).getDatabase();
    }
}
