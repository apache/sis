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
package org.apache.sis.sql.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.apache.sis.internal.sql.SingleAttributeTypeBuilder;
import org.apache.sis.internal.sql.reverse.ColumnMetaModel;
import org.apache.sis.storage.DataStoreException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Each database has specific syntax elements.
 *
 * The dialect provide descriptions and methods to process the different
 * needs required by the store to generate the SQL requests.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface SQLDialect {

    /**
     * Indicate if JDBC driver support global metadatas.
     * Some driver force specifying a schema or table to return results.
     * This prevent us from loading the all metadata in one request and
     * makes us loop on all tables.
     *
     * @return true if global JDBC metadatas are available.
     */
    boolean supportGlobalMetadata();

    /**
     * Test if a table is to be used as a FeatureType.
     * @param name database table name
     * @return true if table should be ignored as a feature type.
     */
    boolean ignoreTable(String name);

    /**
     * Get java mapping type for SQL type.
     *
     * @param sqlType SQL type identifier code
     * @param sqlTypeName SQL type identifier name
     * @return corresponding java type
     */
    Class<?> getJavaType(int sqlType, String sqlTypeName);

    /**
     * Encode column name.
     *
     * @param sql StringBuilder to write into
     * @param name column name, not null
     */
    void encodeColumnName(StringBuilder sql, String name);

    /**
     * Encode schema and table name portion of an sql query.
     *
     * @param sql StringBuilder to write into
     * @param databaseSchema database schema, can be null
     * @param tableName database table, not null
     */
    void encodeSchemaAndTableName(StringBuilder sql, String databaseSchema, String tableName);

    /**
     * If a column is an Auto-increment or has a sequence, try to extract next value.
     *
     * @param column database column description
     * @param cx database connection
     * @return column value or null
     * @throws SQLException
     * @throws DataStoreException
     */
    Object nextValue(ColumnMetaModel column, Connection cx) throws SQLException, DataStoreException;

    /**
     * Get value sequence name used by a column.
     *
     * @param cx database connection
     * @param schemaName database schema
     * @param tableName database table
     * @param columnName database column
     * @return sequence name or null
     * @throws SQLException
     */
    String getColumnSequence(Connection cx, String schemaName, String tableName, String columnName) throws SQLException;

    /**
     * Build column attribute type.
     *
     * @param atb attribute currently created
     * @param cx database connection
     * @param typeName column data type name
     * @param datatype column data type identifier
     * @param schemaName database schema
     * @param tableName database table
     * @param columnName database column
     * @throws SQLException
     */
    void decodeColumnType(final SingleAttributeTypeBuilder atb, final Connection cx,
            final String typeName, final int datatype, final String schemaName,
            final String tableName, final String columnName) throws SQLException;
    /**
     * Build geometry column attribute type.
     *
     * @param atb attribute currently created
     * @param cx database connection
     * @param rs connection result set
     * @param columnIndex geometric column index
     * @param customquery true if the request is a custom query
     * @throws SQLException
     */
    void decodeGeometryColumnType(final SingleAttributeTypeBuilder atb, final Connection cx,
            final ResultSet rs, final int columnIndex, boolean customquery) throws SQLException;

    /**
     * Get geometric field SRID.
     *
     * @param schemaName database schema
     * @param tableName database table
     * @param columnName database column
     * @param metas
     * @param cx database connection
     * @return CoordinateReferenceSystem ID in the database
     * @throws SQLException
     */
    Integer getGeometrySRID(final String schemaName, final String tableName,
            final String columnName, Map<String,Object> metas, final Connection cx) throws SQLException;

    /**
     * Get CoordinateReferenceSystem from database SRID.
     *
     * @param srid CoordinateReferenceSystem ID in the database
     * @param cx database connection
     * @return CoordinateReferenceSystem
     * @throws SQLException
     */
    CoordinateReferenceSystem createCRS(final int srid, final Connection cx) throws SQLException;

}
