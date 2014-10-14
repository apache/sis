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
import java.util.concurrent.*;

/**
 * This base class holds all the unimplemented feature of a Connection.
 * (In order to avoid having a Connection implementation of thousand lines and unreadable).
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
abstract public class AbstractConnection extends AbstractJDBC implements Connection {
    /**
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(@SuppressWarnings("unused") Class<T> iface) throws SQLException {
        throw unsupportedOperation(Connection.class, "unwrap(..)");
    }

    /**
     * No non-standard features are currently handled.
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(@SuppressWarnings("unused") Class<?> iface) {
        return false;
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    @Override
    public CallableStatement prepareCall(@SuppressWarnings("unused") String sql) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareCall");
    }

    /**
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    @Override
    public String nativeSQL(String sql) {
        return sql; // We do nothing at the moment.
    }

    /**
     * Commit / rollback are currently ignored (autocommit = true).
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    @Override
    public void setAutoCommit(boolean autoCommit){
        format(WARNING, "log.autocommit_ignored", autoCommit);
    }

    /**
     * Currently the autocommit state is not handled : always true.
     * @see java.sql.Connection#getAutoCommit()
     */
    @Override
    public boolean getAutoCommit() {
        return true;
    }

    /**
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() {
        format(WARNING, "log.commit_rollback_ignored");
    }

    /**
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() {
        format(WARNING, "log.commit_rollback_ignored");
    }

    /**
     * @see java.sql.Connection#getMetaData()
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw unsupportedOperation(Connection.class, "getMetaData()");
    }

    /**
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly(@SuppressWarnings("unused") boolean readOnly) {
        logUnsupportedOperation(WARNING, Connection.class, "setReadOnly(..)");
    }

    /**
     * @see java.sql.Connection#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    @Override
    public void setCatalog(@SuppressWarnings("unused") String catalog) {
        logUnsupportedOperation(WARNING, Connection.class, "setCatalog(..)");
    }

    /**
     * @see java.sql.Connection#getCatalog()
     */
    @Override
    public String getCatalog() throws SQLException {
        throw unsupportedOperation(Connection.class, "getCatalog()");
    }

    /**
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    @Override
    public void setTransactionIsolation(@SuppressWarnings("unused") int level) {
        logUnsupportedOperation(WARNING, Connection.class, "transaction isolation");
    }

    /**
     * @see java.sql.Connection#getTransactionIsolation()
     */
    @Override
    public int getTransactionIsolation() {
        return 0; // No guarantees of anything.
    }

    /**
     * @see java.sql.Connection#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() {
        logUnsupportedOperation(WARNING, Connection.class, "returning SQL warnings");
        return null;
    }

    /**
     * @see java.sql.Connection#clearWarnings()
     */
    @Override
    public void clearWarnings() {
        logUnsupportedOperation(WARNING, Connection.class, "clearing SQL warnings");
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    @Override
    public CallableStatement prepareCall(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency) throws SQLException {
        throw unsupportedOperation(Connection.class, "stored procedures (prepareCall(..))");
    }

    /**
     * @see java.sql.Connection#getTypeMap()
     */
    @Override
    public Map<String, Class<?>> getTypeMap() {
        logUnsupportedOperation(WARNING, Connection.class, "getTypeMap()");
        return new HashMap<>();
    }

    /**
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    @Override
    public void setTypeMap(@SuppressWarnings("unused") Map<String, Class<?>> map) {
        logUnsupportedOperation(WARNING, Connection.class, "setTypeMap(..)");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement(String sql, int resultSetType, int resultSetConcurrency)");
    }

    /**
     * @see java.sql.Connection#setHoldability(int)
     */
    @Override
    public void setHoldability(@SuppressWarnings("unused") int holdability) {
        logUnsupportedOperation(WARNING, Connection.class, "setHoldability(..)");
    }

    /**
     * @see java.sql.Connection#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        throw unsupportedOperation(Connection.class, "getHoldability()");
    }

    /**
     * @see java.sql.Connection#setSavepoint()
     */
    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw unsupportedOperation(Connection.class, "setSavepoint()");
    }

    /**
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    @Override
    public Savepoint setSavepoint(@SuppressWarnings("unused") String name) throws SQLException {
        throw unsupportedOperation(Connection.class, "setSavepoint(..)");
    }

    /**
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    @Override
    public void rollback(@SuppressWarnings("unused") Savepoint savepoint) throws SQLException {
        throw unsupportedOperation(Connection.class, "rollback(Savepoint)");
    }

    /**
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    @Override
    public void releaseSavepoint(@SuppressWarnings("unused") Savepoint savepoint) throws SQLException {
        throw unsupportedOperation(Connection.class, "releaseSavepoint(Savepoint)");
    }

    /**
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    @Override
    public Statement createStatement(@SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency, @SuppressWarnings("unused") int resultSetHoldability) throws SQLException {
        throw unsupportedOperation(Connection.class, "createStatement(int, int, int)");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency, @SuppressWarnings("unused") int resultSetHoldability) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement(String, int, int, int)");
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    @Override
    public CallableStatement prepareCall(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency, @SuppressWarnings("unused") int resultSetHoldability) throws SQLException {
        throw unsupportedOperation(Connection.class, "stored procedures (prepareCall(String, int, int, int))");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement(String sql)");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int autoGeneratedKeys) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement with autogenerated keys (prepareStatement(String, int))");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") int[] columnIndexes) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement with autogenerated keys (prepareStatement(String, int[]))");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    @Override
    public PreparedStatement prepareStatement(@SuppressWarnings("unused") String sql, @SuppressWarnings("unused") String[] columnNames) throws SQLException {
        throw unsupportedOperation(Connection.class, "prepareStatement with autogenerated keys (prepareStatement(String, String[]))");
    }

    /**
     * @see java.sql.Connection#createClob()
     */
    @Override
    public Clob createClob() throws SQLException {
        throw unsupportedOperation(Connection.class, "createClob()");
    }

    /**
     * @see java.sql.Connection#createBlob()
     */
    @Override
    public Blob createBlob() throws SQLException {
        throw unsupportedOperation(Connection.class, "createBlob()");
    }

    /**
     * @see java.sql.Connection#createNClob()
     */
    @Override
    public NClob createNClob() throws SQLException {
        throw unsupportedOperation(Connection.class, "createNClob()");
    }

    /**
     * @see java.sql.Connection#createSQLXML()
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw unsupportedOperation(Connection.class, "createSQLXML()");
    }

    /**
     * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void setClientInfo(@SuppressWarnings("unused") String name, @SuppressWarnings("unused") String value) {
        logUnsupportedOperation(WARNING, Connection.class, "setClientInfo(..)");
    }

    /**
     * @see java.sql.Connection#setClientInfo(java.util.Properties)
     */
    @Override
    public void setClientInfo(@SuppressWarnings("unused") Properties properties) {
        logUnsupportedOperation(WARNING, Connection.class, "setClientInfo(Properties)");
    }

    /**
     * @see java.sql.Connection#getClientInfo(java.lang.String)
     */
    @Override
    public String getClientInfo(@SuppressWarnings("unused") String name) throws SQLException {
        throw unsupportedOperation(Connection.class, "getClientInfo(String name)");
    }

    /**
     * @see java.sql.Connection#getClientInfo()
     */
    @Override
    public Properties getClientInfo() throws SQLException {
        throw unsupportedOperation(Connection.class, "getClientInfo()");
    }

    /**
     * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
     */
    @Override
    public Array createArrayOf(@SuppressWarnings("unused") String typeName, @SuppressWarnings("unused") Object[] elements) throws SQLException {
        throw unsupportedOperation(Connection.class, "createArrayOf(String typeName, Object[] elements)");
    }

    /**
     * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
     */
    @Override
    public Struct createStruct(@SuppressWarnings("unused") String typeName, @SuppressWarnings("unused") Object[] attributes) throws SQLException {
        throw unsupportedOperation(Connection.class, "createStruct(String typeName, Object[] attributes)");
    }

    /**
     * @see java.sql.Connection#setSchema(java.lang.String)
     */
    @Override
    public void setSchema(@SuppressWarnings("unused") String schema) throws SQLException {
        throw unsupportedOperation(Connection.class, "setSchema(String schema)");
    }

    /**
     * @see java.sql.Connection#getSchema()
     */
    @Override
    public String getSchema() throws SQLException {
        throw unsupportedOperation(Connection.class, "getSchema()");
    }

    /**
     * @see java.sql.Connection#abort(java.util.concurrent.Executor)
     */
    @Override
    public void abort(@SuppressWarnings("unused") Executor executor) throws SQLException {
        throw unsupportedOperation(Connection.class, "abort(Executor executor)");
    }

    /**
     * @see java.sql.Connection#setNetworkTimeout(java.util.concurrent.Executor, int)
     */
    @Override
    public void setNetworkTimeout(@SuppressWarnings("unused") Executor executor, @SuppressWarnings("unused") int milliseconds) {
        logUnsupportedOperation(WARNING, Connection.class, "setNetworkTimeout(Executor executor, int milliseconds)");
    }

    /**
     * @see java.sql.Connection#getNetworkTimeout()
     */
    @Override
    public int getNetworkTimeout() {
        logUnsupportedOperation(WARNING, Connection.class, "getNetworkTimeout()");
        return 0;
    }

    /**
     * @see java.sql.Connection#createStatement(int, int)
     */
    @Override
    public Statement createStatement(@SuppressWarnings("unused") int resultSetType, @SuppressWarnings("unused") int resultSetConcurrency) throws SQLException {
        throw unsupportedOperation(Connection.class, "createStatement(int resultSetType, int resultSetConcurrency)");
    }
}
