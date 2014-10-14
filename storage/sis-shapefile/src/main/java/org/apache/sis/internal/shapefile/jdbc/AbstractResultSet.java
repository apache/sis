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

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;


/**
 * This base class holds all the unimplemented feature of a ResultSet.
 * (In order to avoid having a ResultSet implementation of thousand lines and unreadable).
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
abstract public class AbstractResultSet extends AbstractJDBC implements ResultSet {
    /**
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(@SuppressWarnings("unused") Class<T> iface) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(@SuppressWarnings("unused") Class<?> iface) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#wasNull()
     */
    @Override
    public boolean wasNull() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getAsciiStream(int)
     */
    @Override
    public InputStream getAsciiStream(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getUnicodeStream(int)
     */
    @Override
    @Deprecated
    public InputStream getUnicodeStream(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getBinaryStream(int)
     */
    @Override
    public InputStream getBinaryStream(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getAsciiStream(java.lang.String)
     */
    @Override
    public InputStream getAsciiStream(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getUnicodeStream(java.lang.String)
     */
    @Override
    @Deprecated
    public InputStream getUnicodeStream(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getBinaryStream(java.lang.String)
     */
    @Override
    public InputStream getBinaryStream(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getCursorName()
     */
    @Override
    public String getCursorName() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }


    /**
     * @see java.sql.ResultSet#findColumn(java.lang.String)
     */
    @Override
    public int findColumn(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getCharacterStream(int)
     */
    @Override
    public Reader getCharacterStream(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getCharacterStream(java.lang.String)
     */
    @Override
    public Reader getCharacterStream(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#isBeforeFirst()
     */
    @Override
    public boolean isBeforeFirst() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#isAfterLast()
     */
    @Override
    public boolean isAfterLast() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#isFirst()
     */
    @Override
    public boolean isFirst() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#isLast()
     */
    @Override
    public boolean isLast() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#beforeFirst()
     */
    @Override
    public void beforeFirst() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#afterLast()
     */
    @Override
    public void afterLast() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#first()
     */
    @Override
    public boolean first() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#last()
     */
    @Override
    public boolean last() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getRow()
     */
    @Override
    public int getRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#absolute(int)
     */
    @Override
    public boolean absolute(@SuppressWarnings("unused") int row) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#relative(int)
     */
    @Override
    public boolean relative(@SuppressWarnings("unused") int rows) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#previous()
     */
    @Override
    public boolean previous() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection(@SuppressWarnings("unused") int direction) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#setFetchSize(int)
     */
    @Override
    public void setFetchSize(@SuppressWarnings("unused") int rows) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getType()
     */
    @Override
    public int getType() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getConcurrency()
     */
    @Override
    public int getConcurrency() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#rowUpdated()
     */
    @Override
    public boolean rowUpdated() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#rowInserted()
     */
    @Override
    public boolean rowInserted() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#rowDeleted()
     */
    @Override
    public boolean rowDeleted() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNull(int)
     */
    @Override
    public void updateNull(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBoolean(int, boolean)
     */
    @Override
    public void updateBoolean(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") boolean x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateByte(int, byte)
     */
    @Override
    public void updateByte(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") byte x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateShort(int, short)
     */
    @Override
    public void updateShort(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") short x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateInt(int, int)
     */
    @Override
    public void updateInt(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") int x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateLong(int, long)
     */
    @Override
    public void updateLong(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") long x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateFloat(int, float)
     */
    @Override
    public void updateFloat(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") float x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateDouble(int, double)
     */
    @Override
    public void updateDouble(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") double x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBigDecimal(int, java.math.BigDecimal)
     */
    @Override
    public void updateBigDecimal(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") BigDecimal x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateString(int, java.lang.String)
     */
    @Override
    public void updateString(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") String x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBytes(int, byte[])
     */
    @Override
    public void updateBytes(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") byte[] x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateDate(int, java.sql.Date)
     */
    @Override
    public void updateDate(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Date x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateTime(int, java.sql.Time)
     */
    @Override
    public void updateTime(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Time x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateTimestamp(int, java.sql.Timestamp)
     */
    @Override
    public void updateTimestamp(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Timestamp x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, int)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, int)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, int)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader x, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object, int)
     */
    @Override
    public void updateObject(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Object x, @SuppressWarnings("unused") int scaleOrLength) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object)
     */
    @Override
    public void updateObject(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Object x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNull(java.lang.String)
     */
    @Override
    public void updateNull(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBoolean(java.lang.String, boolean)
     */
    @Override
    public void updateBoolean(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") boolean x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateByte(java.lang.String, byte)
     */
    @Override
    public void updateByte(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") byte x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateShort(java.lang.String, short)
     */
    @Override
    public void updateShort(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") short x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateInt(java.lang.String, int)
     */
    @Override
    public void updateInt(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") int x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateLong(java.lang.String, long)
     */
    @Override
    public void updateLong(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") long x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateFloat(java.lang.String, float)
     */
    @Override
    public void updateFloat(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") float x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateDouble(java.lang.String, double)
     */
    @Override
    public void updateDouble(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") double x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    @Override
    public void updateBigDecimal(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") BigDecimal x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateString(java.lang.String, java.lang.String)
     */
    @Override
    public void updateString(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") String x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBytes(java.lang.String, byte[])
     */
    @Override
    public void updateBytes(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") byte[] x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateDate(java.lang.String, java.sql.Date)
     */
    @Override
    public void updateDate(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Date x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateTime(java.lang.String, java.sql.Time)
     */
    @Override
    public void updateTime(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Time x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateTimestamp(java.lang.String, java.sql.Timestamp)
     */
    @Override
    public void updateTimestamp(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Timestamp x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, int)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, int)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, int)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") int length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object, int)
     */
    @Override
    public void updateObject(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Object x, @SuppressWarnings("unused") int scaleOrLength) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object)
     */
    @Override
    public void updateObject(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Object x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#insertRow()
     */
    @Override
    public void insertRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateRow()
     */
    @Override
    public void updateRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#deleteRow()
     */
    @Override
    public void deleteRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#refreshRow()
     */
    @Override
    public void refreshRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#cancelRowUpdates()
     */
    @Override
    public void cancelRowUpdates() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#moveToInsertRow()
     */
    @Override
    public void moveToInsertRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#moveToCurrentRow()
     */
    @Override
    public void moveToCurrentRow() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getRef(int)
     */
    @Override
    public Ref getRef(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getBlob(int)
     */
    @Override
    public Blob getBlob(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getClob(int)
     */
    @Override
    public Clob getClob(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getArray(int)
     */
    @Override
    public Array getArray(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getObject(java.lang.String, java.util.Map)
     */
    @Override
    public Object getObject(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Map<String, Class<?>> map) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getRef(java.lang.String)
     */
    @Override
    public Ref getRef(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getBlob(java.lang.String)
     */
    @Override
    public Blob getBlob(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getClob(java.lang.String)
     */
    @Override
    public Clob getClob(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getArray(java.lang.String)
     */
    @Override
    public Array getArray(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getDate(int, java.util.Calendar)
     */
    @Override
    public Date getDate(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getDate(java.lang.String, java.util.Calendar)
     */
    @Override
    public Date getDate(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getTime(int, java.util.Calendar)
     */
    @Override
    public Time getTime(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getTime(java.lang.String, java.util.Calendar)
     */
    @Override
    public Time getTime(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getTimestamp(int, java.util.Calendar)
     */
    @Override
    public Timestamp getTimestamp(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getTimestamp(java.lang.String, java.util.Calendar)
     */
    @Override
    public Timestamp getTimestamp(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Calendar cal) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getURL(int)
     */
    @Override
    public URL getURL(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getURL(java.lang.String)
     */
    @Override
    public URL getURL(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateRef(int, java.sql.Ref)
     */
    @Override
    public void updateRef(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Ref x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateRef(java.lang.String, java.sql.Ref)
     */
    @Override
    public void updateRef(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Ref x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(int, java.sql.Blob)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Blob x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.sql.Blob)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Blob x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(int, java.sql.Clob)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Clob x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.sql.Clob)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Clob x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateArray(int, java.sql.Array)
     */
    @Override
    public void updateArray(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Array x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateArray(java.lang.String, java.sql.Array)
     */
    @Override
    public void updateArray(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Array x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getRowId(int)
     */
    @Override
    public RowId getRowId(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getRowId(java.lang.String)
     */
    @Override
    public RowId getRowId(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateRowId(int, java.sql.RowId)
     */
    @Override
    public void updateRowId(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") RowId x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateRowId(java.lang.String, java.sql.RowId)
     */
    @Override
    public void updateRowId(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") RowId x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNString(int, java.lang.String)
     */
    @Override
    public void updateNString(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") String nString) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNString(java.lang.String, java.lang.String)
     */
    @Override
    public void updateNString(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") String nString) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(int, java.sql.NClob)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") NClob nClob) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.sql.NClob)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") NClob nClob) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNClob(int)
     */
    @Override
    public NClob getNClob(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNClob(java.lang.String)
     */
    @Override
    public NClob getNClob(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getSQLXML(int)
     */
    @Override
    public SQLXML getSQLXML(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getSQLXML(java.lang.String)
     */
    @Override
    public SQLXML getSQLXML(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateSQLXML(int, java.sql.SQLXML)
     */
    @Override
    public void updateSQLXML(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") SQLXML xmlObject) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateSQLXML(java.lang.String, java.sql.SQLXML)
     */
    @Override
    public void updateSQLXML(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") SQLXML xmlObject) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNString(int)
     */
    @Override
    public String getNString(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNString(java.lang.String)
     */
    @Override
    public String getNString(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNCharacterStream(int)
     */
    @Override
    public Reader getNCharacterStream(@SuppressWarnings("unused") int columnIndex) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getNCharacterStream(java.lang.String)
     */
    @Override
    public Reader getNCharacterStream(@SuppressWarnings("unused") String columnLabel) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader, long)
     */
    @Override
    public void updateNCharacterStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateNCharacterStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, long)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, long)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, long)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream, long)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream inputStream, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream inputStream, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader, long)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader, long)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader, @SuppressWarnings("unused") long length) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader)
     */
    @Override
    public void updateNCharacterStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateNCharacterStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateAsciiStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateBinaryStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream x) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateCharacterStream(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") InputStream inputStream) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateBlob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") InputStream inputStream) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateNClob(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Reader reader) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getObject(int, java.lang.Class)
     */
    @Override
    public <T> T getObject(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Class<T> type) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getObject(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getObject(@SuppressWarnings("unused") String columnLabel, @SuppressWarnings("unused") Class<T> type) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }

    /**
     * @see java.sql.ResultSet#getObject(int, java.util.Map)
     */
    @Override
    public Object getObject(@SuppressWarnings("unused") int columnIndex, @SuppressWarnings("unused") Map<String, Class<?>> map) throws SQLException {
        throw unsupportedOperation(ResultSet.class, "");
    }
}
