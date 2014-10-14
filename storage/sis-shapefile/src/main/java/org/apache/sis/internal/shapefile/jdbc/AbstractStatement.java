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

/**
 * This base class holds all the unimplemented feature of a Statement.
 * (In order to avoid having a Statement implementation of thousand lines and unreadable).
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
abstract public class AbstractStatement extends AbstractJDBC implements Statement {
    /**
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(@SuppressWarnings("unused") Class<T> iface) throws SQLException {
        throw unsupportedOperation(Statement.class, "unwrap");
    }

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(@SuppressWarnings("unused") Class<?> iface) throws SQLException {
        throw unsupportedOperation(Statement.class, "isWrapperFor");
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String)
     */
    @Override
    public int executeUpdate(@SuppressWarnings("unused") String sql) throws SQLException {
        throw unsupportedOperation(Statement.class, "executeUpdate");
    }

    /**
     * @see java.sql.Statement#getMaxFieldSize()
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        throw unsupportedOperation(Statement.class, "getMaxFieldSize");
    }

    /**
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    @Override
    public void setMaxFieldSize(@SuppressWarnings("unused") int max) throws SQLException {
        throw unsupportedOperation(Statement.class, "setMaxFieldSize");
    }

    /**
     * @see java.sql.Statement#getMaxRows()
     */
    @Override
    public int getMaxRows() throws SQLException {
        throw unsupportedOperation(Statement.class, "getMaxRows");
    }

    /**
     * @see java.sql.Statement#setMaxRows(int)
     */
    @Override
    public void setMaxRows(@SuppressWarnings("unused") int max) throws SQLException {
        throw unsupportedOperation(Statement.class, "setMaxRows");
    }

    /**
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    @Override
    public void setEscapeProcessing(@SuppressWarnings("unused") boolean enable) throws SQLException {
        throw unsupportedOperation(Statement.class, "setEscapeProcessing");
    }

    /**
     * @see java.sql.Statement#getQueryTimeout()
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        throw unsupportedOperation(Statement.class, "getQueryTimeout");
    }

    /**
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    @Override
    public void setQueryTimeout(@SuppressWarnings("unused") int seconds) throws SQLException {
        throw unsupportedOperation(Statement.class, "setQueryTimeout");
    }

    /**
     * @see java.sql.Statement#cancel()
     */
    @Override
    public void cancel() throws SQLException {
        throw unsupportedOperation(Statement.class, "cancel");
    }

    /**
     * @see java.sql.Statement#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw unsupportedOperation(Statement.class, "getWarnings");
    }

    /**
     * @see java.sql.Statement#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        throw unsupportedOperation(Statement.class, "clearWarnings");
    }

    /**
     * @see java.sql.Statement#setCursorName(java.lang.String)
     */
    @Override
    public void setCursorName(@SuppressWarnings("unused") String name) throws SQLException {
        throw unsupportedOperation(Statement.class, "setCursorName");
    }

    /**
     * @see java.sql.Statement#getUpdateCount()
     */
    @Override
    public int getUpdateCount() throws SQLException {
        throw unsupportedOperation(Statement.class, "getUpdateCount");
    }

    /**
     * @see java.sql.Statement#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection(@SuppressWarnings("unused") int direction) throws SQLException {
        throw unsupportedOperation(Statement.class, "setFetchDirection");
    }

    /**
     * @see java.sql.Statement#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        throw unsupportedOperation(Statement.class, "getFetchDirection");
    }

    /**
     * @see java.sql.Statement#setFetchSize(int)
     */
    @Override
    public void setFetchSize(@SuppressWarnings("unused") int rows) throws SQLException {
        throw unsupportedOperation(Statement.class, "setFetchSize");
    }

    /**
     * @see java.sql.Statement#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        throw unsupportedOperation(Statement.class, "getFetchSize");
    }

    /**
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw unsupportedOperation(Statement.class, "getResultSetConcurrency");
    }

    /**
     * @see java.sql.Statement#addBatch(java.lang.String)
     */
    @Override
    public void addBatch(@SuppressWarnings("unused") String sql) throws SQLException {
        throw unsupportedOperation(Statement.class, "addBatch");
    }

    /**
     * @see java.sql.Statement#clearBatch()
     */
    @Override
    public void clearBatch() throws SQLException {
        throw unsupportedOperation(Statement.class, "clearBatch");
    }

    /**
     * @see java.sql.Statement#executeBatch()
     */
    @Override
    public int[] executeBatch() throws SQLException {
        throw unsupportedOperation(Statement.class, "executeBatch");
    }

    /**
     * @see java.sql.Statement#getMoreResults(int)
     */
    @Override
    public boolean getMoreResults(@SuppressWarnings("unused") int current) throws SQLException {
        throw unsupportedOperation(Statement.class, "getMoreResults");
    }

    /**
     * @see java.sql.Statement#getGeneratedKeys()
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw unsupportedOperation(Statement.class, "getGeneratedKeys");
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, int)
     */
    @Override
    public int executeUpdate(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int autoGeneratedKeys) throws SQLException {
        throw unsupportedOperation(Statement.class, "executeUpdate");
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
     */
    @Override
    public int executeUpdate(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int[] columnIndexes) throws SQLException {
        throw unsupportedOperation(Statement.class, "executeUpdate");
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
     */
    @Override
    public int executeUpdate(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") String[] columnNames) throws SQLException {
        throw unsupportedOperation(Statement.class, "executeUpdate");
    }

    /**
     * @see java.sql.Statement#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        throw unsupportedOperation(Statement.class, "getResultSetHoldability");
    }

    /**
     * @see java.sql.Statement#setPoolable(boolean)
     */
    @Override
    public void setPoolable(@SuppressWarnings("unused") boolean poolable) throws SQLException {
        throw unsupportedOperation(Statement.class, "setPoolable");
    }

    /**
     * @see java.sql.Statement#isPoolable()
     */
    @Override
    public boolean isPoolable() throws SQLException {
        throw unsupportedOperation(Statement.class, "isPoolable");
    }

    /**
     * @see java.sql.Statement#closeOnCompletion()
     */
    @Override
    public void closeOnCompletion() throws SQLException {
        throw unsupportedOperation(Statement.class, "closeOnCompletion");
    }

    /**
     * @see java.sql.Statement#isCloseOnCompletion()
     */
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        throw unsupportedOperation(Statement.class, "isCloseOnCompletion");
    }

    /**
     * @see java.sql.Statement#getMoreResults()
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        throw unsupportedOperation(Statement.class, "getMoreResults");
    }

    /**
     * @see java.sql.Statement#getResultSetType()
     */
    @Override
    public int getResultSetType() throws SQLException {
        throw unsupportedOperation(Statement.class, "getResultSetType");
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, int)
     */
    @Override
    public boolean execute(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int autoGeneratedKeys) throws SQLException {
        throw unsupportedOperation(Statement.class, "execute");
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, int[])
     */
    @Override
    public boolean execute(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int[] columnIndexes) throws SQLException {
        throw unsupportedOperation(Statement.class, "execute");
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
     */
    @Override
    public boolean execute(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") String[] columnNames) throws SQLException {
        throw unsupportedOperation(Statement.class, "execute");
    }
}
