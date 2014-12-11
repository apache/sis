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
package org.apache.sis.internal.shapefile.jdbc.metadata;

import java.sql.ResultSetMetaData;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Objects;

import org.apache.sis.internal.shapefile.jdbc.AbstractJDBC;
import org.apache.sis.internal.shapefile.jdbc.resultset.DBFRecordBasedResultSet;
import org.apache.sis.internal.shapefile.jdbc.resultset.SQLIllegalColumnIndexException;
import org.apache.sis.storage.shapefile.DataType;
import org.apache.sis.storage.shapefile.Database;
import org.apache.sis.storage.shapefile.FieldDescriptor;

/**
 * ResultSet Metadata.
 * @author Marc LE BIHAN
 */
public class DBFResultSetMataData extends AbstractJDBC implements ResultSetMetaData {
    /** ResultSet. */
    private DBFRecordBasedResultSet m_rs;
    
    /**
     * Construct a ResultSetMetaData.
     * @param rs ResultSet.
     */
    public DBFResultSetMataData(DBFRecordBasedResultSet rs) {
        Objects.requireNonNull(rs, "A non null ResultSet is required.");
        m_rs = rs;
    }
    
    /**
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override public <T> T unwrap(Class<T> iface) throws SQLFeatureNotSupportedException {
        throw unsupportedOperation("unwrap", iface);
    }

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        logStep("isWrapperFor", iface);
        return iface.isAssignableFrom(getInterface());
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnCount()
     */
    @Override public int getColumnCount() {
        logStep("getColumnCount");
        return m_rs.getDatabase().getFieldsDescriptor().size();
    }

    /**
     * @see java.sql.ResultSetMetaData#isAutoIncrement(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public boolean isAutoIncrement(int column) throws SQLIllegalColumnIndexException {
        logStep("isAutoIncrement", column);
        
        FieldDescriptor field = getField(column);
        return field.getType().equals(DataType.AutoIncrement);
    }

    /**
     * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
     */
    @Override public boolean isCaseSensitive(int column) {
        logStep("isCaseSensitive", column);
        return true; // Yes, because behind, there's a HashMap.
    }

    /**
     * @see java.sql.ResultSetMetaData#isSearchable(int)
     */
    @Override public boolean isSearchable(int column) {
        logStep("isSearchable", column);
        return true; // All currently are searcheable.
    }

    /**
     * @see java.sql.ResultSetMetaData#isCurrency(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public boolean isCurrency(int column) throws SQLIllegalColumnIndexException {
        logStep("isCurrency", column);

        FieldDescriptor field = getField(column);
        return field.getType().equals(DataType.Currency);
    }

    /**
     * @see java.sql.ResultSetMetaData#isNullable(int)
     */
    @Override public int isNullable(int column) {
        logStep("isNullable", column);
        return ResultSetMetaData.columnNullableUnknown; // TODO Check if somes settings exists for that in field descriptor.
    }

    /**
     * @see java.sql.ResultSetMetaData#isSigned(int)
     */
    @Override public boolean isSigned(int column) {
        logStep("isSigned", column);
        return true;  // TODO Check if somes settings exists for that in field descriptor.
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public int getColumnDisplaySize(int column) throws SQLIllegalColumnIndexException {
        logStep("getColumnDisplaySize", column);
        
        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
            case Character:
            case Integer:
               return field.getLength();
                
            case Date:
                return 8;
                
            // Add decimal separator for decimal numbers.
            case Double:
            case FloatingPoint:
            case Number:
                return field.getLength() + 1;
                
            case Logical:
                return 5; // Translation for true, false, null.

            // Unhandled types default to field length.
            case Currency:
            case DateTime:
            case Memo:
            case Picture:
            case TimeStamp:
            case VariField:
            case Variant:
                return field.getLength();
                
            default:
                return field.getLength();
        }
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnLabel(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public String getColumnLabel(int column) throws SQLIllegalColumnIndexException {
        logStep("getColumnLabel", column);

        FieldDescriptor field = getField(column);
        return field.getName();
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnName(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public String getColumnName(int column) throws SQLIllegalColumnIndexException {
        logStep("getColumnName", column);

        FieldDescriptor field = getField(column);
        return field.getName();
    }

    /**
     * @see java.sql.ResultSetMetaData#getSchemaName(int)
     */
    @Override public String getSchemaName(int column) {
        logStep("getSchemaName", column);

        return ""; // No schema name in DBase 3.
    }

    /**
     * @see java.sql.ResultSetMetaData#getPrecision(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public int getPrecision(int column) throws SQLIllegalColumnIndexException {
        logStep("getPrecision", column);

        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
            case Character:
            case Integer:
               return field.getLength();
                
            case Date:
                return 8;
                
            case Double:
            case FloatingPoint:
            case Number:
                return field.getLength();
                
            case Logical:
                return 0;

            case Currency:
            case DateTime:
            case TimeStamp:
                return field.getLength();

            case Memo:
            case Picture:
            case VariField:
            case Variant:
                return 0;

            default:
                return field.getLength();
        }
    }

    /**
     * @see java.sql.ResultSetMetaData#getScale(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public int getScale(int column) throws SQLIllegalColumnIndexException {
        logStep("getScale", column);

        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
            case Character:
            case Integer:
               return field.getLength();
                
            case Date:
                return 8;
                
            case Double:
            case FloatingPoint:
            case Number:
                return field.getLength();
                
            case Logical:
                return 0;

            case Currency:
            case DateTime:
            case TimeStamp:
                return field.getLength();

            case Memo:
            case Picture:
            case VariField:
            case Variant:
                return 0;

            default:
                return field.getLength();
        }
    }

    /**
     * @see java.sql.ResultSetMetaData#getTableName(int)
     */
    @Override public String getTableName(int column) {
        logStep("getTableName", column);

        // The table default to the file name (without its extension .dbf).
        String fileName = m_rs.getDatabase().getFile().getName(); 
        int indexDBF = fileName.lastIndexOf(".");
        String tableName = fileName.substring(0, indexDBF);
        
        return tableName;
    }

    /**
     * @see java.sql.ResultSetMetaData#getCatalogName(int)
     */
    @Override public String getCatalogName(int column) {
        logStep("getCatalogName", column);
        return "";
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnType(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public int getColumnType(int column) throws SQLIllegalColumnIndexException {
        logStep("getColumnType", column);

        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
                return Types.INTEGER;
                
            case Character:
                return Types.CHAR;
                
            case Integer:
               return Types.INTEGER;
                
            case Date:
                return Types.DATE;
                
            case Double:
                return Types.DOUBLE;
                
            case FloatingPoint:
                return Types.FLOAT;
                
            case Number:
                return Types.DECIMAL;
                
            case Logical:
                return Types.BOOLEAN;

            case Currency:
                return Types.NUMERIC;
                
            case DateTime:
                return Types.TIMESTAMP; // TODO : I think ?
                
            case TimeStamp:
                return Types.TIMESTAMP;

            case Memo:
                return Types.BLOB;
                
            case Picture:
                return Types.BLOB;
                
            case VariField:
                return Types.OTHER;
                
            case Variant:
                return Types.OTHER;

            default:
                return Types.OTHER;
        }
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public String getColumnTypeName(int column) throws SQLIllegalColumnIndexException {
        logStep("getColumnTypeName", column);

        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
                return "AUTO_INCREMENT";
                
            case Character:
                return "CHAR";
                
            case Integer:
               return "INTEGER";
                
            case Date:
                return "DATE";
                
            case Double:
                return "DOUBLE";
                
            case FloatingPoint:
                return "FLOAT";
                
            case Number:
                return "DECIMAL";
                
            case Logical:
                return "BOOLEAN";

            case Currency:
                return "CURRENCY";
                
            case DateTime:
                return "DATETIME";
                
            case TimeStamp:
                return "TIMESTAMP";

            case Memo:
                return "MEMO";
                
            case Picture:
                return "PICTURE";
                
            case VariField:
                return "VARIFIELD";
                
            case Variant:
                return "VARIANT";

            default:
                return "UNKNOWN";
        }
    }

    /**
     * @see java.sql.ResultSetMetaData#isReadOnly(int)
     */
    @Override public boolean isReadOnly(int column) {
        logStep("isReadOnly", column);
        return false; // TODO Check if somes settings exists for that in field descriptor.
    }

    /**
     * @see java.sql.ResultSetMetaData#isWritable(int)
     */
    @Override public boolean isWritable(int column) {
        logStep("isWritable", column);
        return true;  // TODO Check if somes settings exists for that in field descriptor.
    }

    /**
     * @see java.sql.ResultSetMetaData#isDefinitelyWritable(int)
     */
    @Override public boolean isDefinitelyWritable(int column) {
        logStep("isDefinitelyWritable", column);
        return true; // TODO Check if somes settings exists for that in field descriptor.
    }

    /**
     * @see java.sql.ResultSetMetaData#getColumnClassName(int)
     * @throws SQLFeatureNotSupportedException if underlying class implementing a type isn't currently set. 
     * @throws SQLIllegalColumnIndexException if the column index is illegal. 
     */
    @Override public String getColumnClassName(int column) throws SQLFeatureNotSupportedException, SQLIllegalColumnIndexException {
        logStep("getColumnClassName", column);

        FieldDescriptor field = getField(column);
        
        switch(field.getType()) {
            case AutoIncrement:
                return Integer.class.getName();
                
            case Character:
                return String.class.getName();
                
            case Integer:
                return Integer.class.getName();
                
            case Date:
                return java.sql.Date.class.getName();
                
            case Double:
                return Double.class.getName();
                
            case FloatingPoint:
                return Float.class.getName();
                
            case Number:
                return Double.class.getName();
                
            case Logical:
                return Boolean.class.getName();

            case Currency:
                return Double.class.getName();
                
            case DateTime:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on DateTime");
                
            case TimeStamp:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on TimeStamp");

            case Memo:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on Memo");
                
            case Picture:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on Picture");
                
            case VariField:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on VariField");
                
            case Variant:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on Variant");

            default:
                throw unsupportedOperation("ResultSetMetaData.getColumnClassName(..) on " + field.getType());
        }
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.AbstractJDBC#getInterface()
     */
    @Override protected Class<?> getInterface() {
        return ResultSetMetaData.class; 
    }
    
    /**
     * Return the underlying database binary representation.
     * This function shall not check the closed state of this connection, as it can be used in exception messages descriptions.
     * @return Database.
     */
    public Database getDatabase() {
        return(m_rs.getDatabase());
    }

    /**
     * Returns the field descriptor of a given ResultSet column index.
     * @param columnIndex Column index, first column is 1, second is 2, etc.
     * @return Field Descriptor.
     * @throws SQLIllegalColumnIndexException if the index is out of bounds.
     */
    private FieldDescriptor getField(int columnIndex) throws SQLIllegalColumnIndexException {
        if (columnIndex < 1 || columnIndex > getDatabase().getFieldsDescriptor().size()) {
            String message = format("excp.illegal_column_index_metadata", columnIndex, getDatabase().getFieldsDescriptor().size());
            throw new SQLIllegalColumnIndexException(message, m_rs.getSQL(), getDatabase().getFile(), columnIndex);
        }
        
        return getDatabase().getFieldsDescriptor().get(columnIndex-1);
    }
}
