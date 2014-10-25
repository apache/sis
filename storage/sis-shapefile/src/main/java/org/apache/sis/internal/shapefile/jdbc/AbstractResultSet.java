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
import java.net.URL;
import java.util.Map;
import java.util.Calendar;
import java.math.BigDecimal;
import java.io.Reader;
import java.io.InputStream;


/**
 * Holds all the unimplemented feature of a {@code ResultSet}.
 * This is in order to avoid having a ResultSet implementation of thousand lines and unreadable.
 *
 * <table class="sis">
 *   <caption>Connection default values</caption>
 *   <tr><th>Property</th>                           <th>Value</th></tr>
 *   <tr><td>{@link #getType()}</td>                 <td>{@link Statement#getResultSetType()}</td></tr>
 *   <tr><td>{@link #getConcurrency()}</td>          <td>{@link Statement#getResultSetConcurrency()}</td></tr>
 *   <tr><td>{@link #getHoldability()}</td>          <td>{@link Statement#getResultSetHoldability()}</td></tr>
 *   <tr><td>{@link #getFetchDirection()}</td>       <td>{@link Statement#getFetchDirection()}</td></tr>
 *   <tr><td>{@link #getFetchSize()}</td>            <td>{@link Statement#getFetchSize()}</td></tr>
 *   <tr><td>{@link #isBeforeFirst()}</td>           <td>Compute from {@link #getRow()}</td></tr>
 *   <tr><td>{@link #isFirst()}</td>                 <td>Compute from {@link #getRow()}</td></tr>
 *   <tr><td>{@link #relative(int)}</td>             <td>Use {@link #absolute(int)}</td></tr>
 *   <tr><td>{@link #beforeFirst()}</td>             <td>Use {@link #absolute(int)}</td></tr>
 *   <tr><td>{@link #first()}</td>                   <td>Use {@link #absolute(int)}</td></tr>
 *   <tr><td>{@link #last()}</td>                    <td>Use {@link #absolute(int)}</td></tr>
 *   <tr><td>{@link #afterLast()}</td>               <td>Use {@link #absolute(int)}</td></tr>
 *   <tr><td>{@link #previous()}</td>                <td>Use {@link #relative(int)}</td></tr>
 *   <tr><td>{@link #getNString(int)}</td>           <td>{@link #getString(int)}</td></tr>
 *   <tr><td>{@link #getNCharacterStream(int)}</td>  <td>{@link #getCharacterStream(int)}</td></tr>
 *   <tr><td>{@link #getWarnings()}</td>             <td>{@code null}</td></tr>
 *   <tr><td>{@link #clearWarnings()}</td>           <td>Ignored</td></tr>
 * </table>
 *
 * Furthermore, most methods expecting a column label of type {@code String} first invoke {@link #findColumn(String)},
 * then invoke the method of the same name expecting a column index as an {@code int}.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
@SuppressWarnings("unused")
abstract class AbstractResultSet extends AbstractJDBC implements ResultSet {
    /**
     * Constructs a new {@code ResultSet} instance.
     */
    AbstractResultSet() {
    }

    /**
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     */
    @Override
    final Class<?> getInterface() {
        return ResultSet.class;
    }

    /**
     * Unsupported by default.
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw unsupportedOperation("getMetaData");
    }

    /**
     * Defaults to {@link Statement#getResultSetType()}.
     */
    @Override
    public int getType() throws SQLException {
        return getStatement().getResultSetType();
    }

    /**
     * Defaults to {@link Statement#getResultSetConcurrency()}.
     */
    @Override
    public int getConcurrency() throws SQLException {
        return getStatement().getResultSetConcurrency();
    }

    /**
     * Defaults to {@link Statement#getResultSetHoldability()}.
     */
    @Override
    public int getHoldability() throws SQLException {
        return getStatement().getResultSetHoldability();
    }

    /**
     * Defaults to {@link Statement#getFetchDirection()}.
     */
    @Override
    public int getFetchDirection() throws SQLException {
        return getStatement().getFetchDirection();
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw unsupportedOperation("setFetchDirection");
    }

    /**
     * Defaults to {@link Statement#getFetchSize()}.
     */
    @Override
    public int getFetchSize() throws SQLException {
        return getStatement().getFetchSize();
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        throw unsupportedOperation("setFetchSize");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public String getCursorName() throws SQLException {
        throw unsupportedOperation("getCursorName");
    }

    /**
     * Retrieves the current row number (first row is 1). This method is unsupported by default.
     * Implementing this method will allow {@link #relative(int)} and other methods to work with
     * their default implementation.
     */
    @Override
    public int getRow() throws SQLException {
        throw unsupportedOperation("getRow");
    }

    /**
     * Defaults to {@link #getRow()}.
     */
    @Override
    public boolean isBeforeFirst() throws SQLException {
        return getRow() == 0;
    }

    /**
     * Defaults to {@link #getRow()}.
     */
    @Override
    public boolean isFirst() throws SQLException {
        return getRow() == 1;
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean isLast() throws SQLException {
        throw unsupportedOperation("isLast");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean isAfterLast() throws SQLException {
        throw unsupportedOperation("isAfterLast");
    }

    /**
     * Moves the cursor to the given row number (first row is 1).
     * Special cases:
     * <ul>
     *   <li>Negative numbers move to an absolute row position with respect to the end of the result set.</li>
     *   <li>-1 moves on the last row.</li>
     *   <li> 0 moves the cursor before the first row.</li>
     * </ul>
     *
     * This method is unsupported by default. Implementing this method will allow
     * {@link #relative(int)} and other methods to work with their default implementation.
     *
     * @return {@code true} if the cursor is on a row.
     */
    @Override
    public boolean absolute(int row) throws SQLException {
        throw unsupportedOperation("absolute");
    }

    /**
     * Defaults to {@link #absolute(int)} with an offset computed from {@link #getRow()}.
     */
    @Override
    public boolean relative(int rows) throws SQLException {
        return absolute(rows - getRow());
    }

    /**
     * Defaults to {@link #absolute(int)}.
     */
    @Override
    public void beforeFirst() throws SQLException {
        absolute(0);
    }

    /**
     * Defaults to {@link #absolute(int)}.
     */
    @Override
    public boolean first() throws SQLException {
        return absolute(1);
    }

    /**
     * Defaults to {@link #absolute(int)}.
     */
    @Override
    public boolean last() throws SQLException {
        return absolute(-1);
    }

    /**
     * Defaults to {@link #last()} followed by {@link #next()}.
     */
    @Override
    public void afterLast() throws SQLException {
        if (last()) next();
    }

    /**
     * Defaults to {@link #relative(int)}.
     */
    @Override
    public boolean previous() throws SQLException {
        return relative(-1);
    }

    /**
     * Returns the column index for the given column name.
     * The default implementation of all methods expecting a column label will invoke this method.
     * The default implementation throws a {@link SQLFeatureNotSupportedException}.
     *
     * @param  columnLabel The name of the column.
     * @return The index of the given column name
     * @exception SQLException if this feature is not implemented, or if there is no column of the given name.
     */
    @Override
    public int findColumn(String columnLabel) throws SQLException {
        throw unsupportedOperation("findColumn");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean wasNull() throws SQLException {
        throw unsupportedOperation("wasNull");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw unsupportedOperation("getRowId");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public String getString(int columnIndex) throws SQLException {
        throw unsupportedOperation("getString");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }

    /**
     * Defaults to {@link #getString(int)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getNString(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        throw unsupportedOperation("getBoolean");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public byte getByte(int columnIndex) throws SQLException {
        throw unsupportedOperation("getByte");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        throw unsupportedOperation("getBytes");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public short getShort(int columnIndex) throws SQLException {
        throw unsupportedOperation("getShort");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public int getInt(int columnIndex) throws SQLException {
        throw unsupportedOperation("getInt");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public long getLong(int columnIndex) throws SQLException {
        throw unsupportedOperation("getLong");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public float getFloat(int columnIndex) throws SQLException {
        throw unsupportedOperation("getFloat");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public double getDouble(int columnIndex) throws SQLException {
        throw unsupportedOperation("getDouble");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        throw unsupportedOperation("getBigDecimal");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    /**
     * @deprecated Replaced by {@link #getBigDecimal(int)}.
     * Defaults to {@link #getBigDecimal(int)} followed by {@link BigDecimal#setScale(int)}.
     */
    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        final BigDecimal d = getBigDecimal(columnIndex);
        return (d != null) ? d.setScale(scale) : null;
    }

    /**
     * @deprecated Replaced by {@link #getBigDecimal(String)}.
     * Defaults to {@link #getBigDecimal(String)} followed by {@link BigDecimal#setScale(int)}.
     */
    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        final BigDecimal d = getBigDecimal(columnLabel);
        return (d != null) ? d.setScale(scale) : null;
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Date getDate(int columnIndex) throws SQLException {
        throw unsupportedOperation("getDate");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw unsupportedOperation("getDate");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(findColumn(columnLabel), cal);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Time getTime(int columnIndex) throws SQLException {
        throw unsupportedOperation("getTime");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw unsupportedOperation("getTime");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(findColumn(columnLabel), cal);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        throw unsupportedOperation("getTimestamp");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw unsupportedOperation("getTimestamp");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(findColumn(columnLabel), cal);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public URL getURL(int columnIndex) throws SQLException {
        throw unsupportedOperation("getURL");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return getURL(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw unsupportedOperation("getArray");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return getArray(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw unsupportedOperation("getSQLXML");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Object getObject(int columnIndex) throws SQLException {
        throw unsupportedOperation("getObject");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw unsupportedOperation("getObject");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(findColumn(columnLabel), map);
    }

    /**
     * Unsupported by default.
     */
    // No @Override on JDK6
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw unsupportedOperation("getObject");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    // No @Override on JDK6
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw unsupportedOperation("getRef");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return getRef(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw unsupportedOperation("getBlob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return getBlob(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw unsupportedOperation("getClob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return getClob(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw unsupportedOperation("getNClob");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw unsupportedOperation("getNClob");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw unsupportedOperation("getAsciiStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return getAsciiStream(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw unsupportedOperation("getCharacterStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStream(findColumn(columnLabel));
    }

    /**
     * Defaults to {@link #getCharacterStream(int)} on the assumption that the fact that Java use UTF-16 internally
     * makes the two methods identical in behavior.
     */
    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return getCharacterStream(columnIndex);
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return getNCharacterStream(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw unsupportedOperation("getUnicodeStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    @Deprecated
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return getUnicodeStream(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw unsupportedOperation("getBinaryStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw unsupportedOperation("updateNull");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateNull(String columnLabel) throws SQLException {
        updateNull(findColumn(columnLabel));
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw unsupportedOperation("updateRowId");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        updateRowId(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw unsupportedOperation("updateString");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        updateString(findColumn(columnLabel), x);
    }

    /**
     * Defaults to {@link #updateString(int, String)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        updateString(columnIndex, nString);
    }

    /**
     * Defaults to {@link #updateString(String, String)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        updateString(columnLabel, nString);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw unsupportedOperation("updateBoolean");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        updateBoolean(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw unsupportedOperation("updateByte");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        updateByte(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw unsupportedOperation("updateBytes");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        updateBytes(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw unsupportedOperation("updateShort");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        updateShort(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw unsupportedOperation("updateInt");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        updateInt(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw unsupportedOperation("updateLong");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        updateLong(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw unsupportedOperation("updateFloat");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        updateFloat(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw unsupportedOperation("updateDouble");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        updateDouble(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw unsupportedOperation("updateBigDecimal");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        updateBigDecimal(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw unsupportedOperation("updateDate");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        updateDate(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw unsupportedOperation("updateTime");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        updateTime(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw unsupportedOperation("updateTimestamp");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        updateTimestamp(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw unsupportedOperation("updateArray");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        updateArray(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw unsupportedOperation("updateObject");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        updateObject(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw unsupportedOperation("updateObject");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        updateObject(findColumn(columnLabel), x, scaleOrLength);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateSQLXML(int columnIndex, SQLXML x) throws SQLException {
        throw unsupportedOperation("updateSQLXML");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateSQLXML(String columnLabel, SQLXML x) throws SQLException {
        updateSQLXML(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw unsupportedOperation("updateRef");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        updateRef(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw unsupportedOperation("updateBlob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        updateBlob(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw unsupportedOperation("updateBlob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBlob(String columnLabel, InputStream x, long length) throws SQLException {
        updateBlob(findColumn(columnLabel), x, length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw unsupportedOperation("updateClob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        updateClob(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw unsupportedOperation("updateClob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateClob(String columnLabel, Reader x, long length) throws SQLException {
        updateClob(findColumn(columnLabel), x, length);
    }

    /**
     * Defaults to {@link #updateClob(int, Clob)} on the assumption that the fact that Java use UTF-16 internally
     * makes the two methods identical in behavior.
     */
    @Override
    public void updateNClob(int columnIndex, NClob x) throws SQLException {
        updateClob(columnIndex, x);
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateNClob(String columnLabel, NClob x) throws SQLException {
        updateNClob(findColumn(columnLabel), x);
    }

    /**
     * Defaults to {@link #updateClob(int, Reader, long)} on the assumption that the fact that Java use UTF-16 internally
     * makes the two methods identical in behavior.
     */
    @Override
    public void updateNClob(int columnIndex, Reader x, long length) throws SQLException {
        updateClob(columnIndex, x, length);
    }

    /**
     * Defaults to {@link #updateClob(String, Reader, long)} on the assumption that the fact that Java use UTF-16 internally
     * makes the two methods identical in behavior.
     */
    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        updateClob(columnLabel, reader, length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw unsupportedOperation("updateAsciiStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        updateAsciiStream(findColumn(columnLabel), x);
    }

    /**
     * Delegates to {@link #updateAsciiStream(int, InputStream, long)}
     */
    @Override
    public final void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        updateAsciiStream(columnIndex, x, (long) length);
    }

    /**
     * Delegates to {@link #updateAsciiStream(String, InputStream, long)}
     */
    @Override
    public final void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        updateAsciiStream(columnLabel, x, (long) length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw unsupportedOperation("updateAsciiStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        updateAsciiStream(findColumn(columnLabel), x, length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw unsupportedOperation("updateCharacterStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateCharacterStream(String columnLabel, Reader x) throws SQLException {
        updateCharacterStream(findColumn(columnLabel), x);
    }

    /**
     * Delegates to {@link #updateCharacterStream(int, Reader, long)}
     */
    @Override
    public final void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        updateCharacterStream(columnIndex, x, (long) length);
    }

    /**
     * Delegates to {@link #updateCharacterStream(String, Reader, long)}
     */
    @Override
    public final void updateCharacterStream(String columnLabel, Reader x, int length) throws SQLException {
        updateCharacterStream(columnLabel, x, (long) length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw unsupportedOperation("updateCharacterStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateCharacterStream(String columnLabel, Reader x, long length) throws SQLException {
        updateCharacterStream(findColumn(columnLabel), x, length);
    }

    /**
     * Defaults to {@link #updateCharacterStream(int, Reader)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        updateCharacterStream(columnIndex, x);
    }

    /**
     * Defaults to {@link #updateCharacterStream(String, Reader)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNCharacterStream(String columnLabel, Reader x) throws SQLException {
        updateCharacterStream(columnLabel, x);
    }

    /**
     * Defaults to {@link #updateCharacterStream(int, Reader, int)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        updateCharacterStream(columnIndex, x, length);
    }

    /**
     * Defaults to {@link #updateCharacterStream(String, Reader, int)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNCharacterStream(String columnLabel, Reader x, long length) throws SQLException {
        updateCharacterStream(columnLabel, x, length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw unsupportedOperation("updateBinaryStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        updateBinaryStream(findColumn(columnLabel), x);
    }

    /**
     * Delegates to {@link #updateBinaryStream(int, InputStream, long)}.
     */
    @Override
    public final void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        updateBinaryStream(columnIndex, x, (long) length);
    }

    /**
     * Delegates to {@link #updateBinaryStream(String, InputStream, long)}.
     */
    @Override
    public final void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        updateBinaryStream(columnLabel, x, (long) length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw unsupportedOperation("updateBinaryStream");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        updateBinaryStream(findColumn(columnLabel), x, length);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateBlob(int columnIndex, InputStream x) throws SQLException {
        throw unsupportedOperation("updateBlob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateBlob(String columnLabel, InputStream x) throws SQLException {
        updateBlob(findColumn(columnLabel), x);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateClob(int columnIndex, Reader x) throws SQLException {
        throw unsupportedOperation("updateClob");
    }

    /**
     * Defaults to the index-based version of this method.
     * The given column name is mapped to a column index by {@link #findColumn(String)}.
     */
    @Override
    public void updateClob(String columnLabel, Reader x) throws SQLException {
        updateClob(findColumn(columnLabel), x);
    }

    /**
     * Defaults to {@link #updateClob(int, Reader)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        updateClob(columnIndex, reader);
    }

    /**
     * Defaults to {@link #updateClob(String, Reader)} on the assumption that the fact that
     * Java use UTF-16 internally makes the two methods identical in behavior.
     */
    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        updateClob(columnLabel, reader);
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void insertRow() throws SQLException {
        throw unsupportedOperation("insertRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void updateRow() throws SQLException {
        throw unsupportedOperation("updateRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void deleteRow() throws SQLException {
        throw unsupportedOperation("deleteRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void refreshRow() throws SQLException {
        throw unsupportedOperation("refreshRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void cancelRowUpdates() throws SQLException {
        throw unsupportedOperation("cancelRowUpdates");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void moveToInsertRow() throws SQLException {
        throw unsupportedOperation("moveToInsertRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public void moveToCurrentRow() throws SQLException {
        throw unsupportedOperation("moveToCurrentRow");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean rowUpdated() throws SQLException {
        throw unsupportedOperation("rowUpdated");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean rowInserted() throws SQLException {
        throw unsupportedOperation("rowInserted");
    }

    /**
     * Unsupported by default.
     */
    @Override
    public boolean rowDeleted() throws SQLException {
        throw unsupportedOperation("rowDeleted");
    }
}
