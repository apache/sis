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
package org.apache.sis.internal.sql.feature;

import java.util.Map;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataStoreException;


/**
 * Description or handling of syntax elements specific to a database.
 * The dialect provides descriptions and methods implementing the different
 * functionalities required by the data store to generate SQL statements.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class Dialect {
    /**
     * For subclass constructors.
     */
    protected Dialect() {
    }

    /**
     * Indicates if the JDBC driver support global metadata.
     * Some drivers force specifying a schema or table to return results.
     * This prevent us from loading all metadata in one request and
     * makes us loop on all tables.
     *
     * @return whether global JDBC metadata are available.
     */
    public abstract boolean supportGlobalMetadata();

    /**
     * Indicates whether a table will be used as a {@code FeatureType}.
     *
     * @param  name  database table name.
     * @return {@code true} if the named table should be ignored when looking for feature types.
     */
    public abstract boolean isTableIgnored(String name);

    /**
     * Gets the Java class mapped to a given SQL type.
     *
     * @param  sqlType      SQL type code as one of {@link java.sql.Types} constants.
     * @param  sqlTypeName  name of {@code sqlType}.
     * @return corresponding java type.
     *
     * @todo What happen if there is no match?
     */
    public abstract Class<?> getJavaType(int sqlType, String sqlTypeName);

    /**
     * Encodes the column name part of a SQL query.
     *
     * @param sql   where to write the SQL statement.
     * @param name  column name to write, not null.
     */
    public abstract void encodeColumnName(StringBuilder sql, String name);

    /**
     * Encodes the schema and table name parts of a SQL query.
     *
     * @param sql     where to write the SQL statement.
     * @param schema  database schema to write, or null if none.
     * @param table   database table to write, not null.
     */
    public abstract void encodeSchemaAndTableName(StringBuilder sql, String schema, String table);

    /**
     * If a column is an auto-increment or has a sequence, tries to extract next value.
     *
     * @param  column  description of the database column for which to get the next value.
     * @param  cx      connection to the database.
     * @return column value or null if none.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     * @throws DataStoreException if another error occurred while fetching the next value.
     */
    public abstract Object nextValue(ColumnMetaModel column, Connection cx) throws SQLException, DataStoreException;

    /**
     * Gets the value sequence name used by a column.
     *
     * @param  cx      connection to the database.
     * @param  schema  name of the database schema.
     * @param  table   name of the database table.
     * @param  column  name of the database column.
     * @return sequence name or null if none.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    public abstract String getColumnSequence(Connection cx, String schema, String table, String column) throws SQLException;

    /**
     * Builds column attribute type.
     *
     * @param  atb       builder for the attribute being created.
     * @param  cx        connection to the database.
     * @param  typeName  column data type name.
     * @param  datatype  column data type code.
     * @param  schema    name of the database schema.
     * @param  table     name of the database table.
     * @param  column    name of the database column.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    public abstract void decodeColumnType(final SingleAttributeTypeBuilder atb, final Connection cx,
            final String typeName, final int datatype, final String schema,
            final String table, final String column) throws SQLException;

    /**
     * Builds geometry column attribute type.
     *
     * @param  atb          builder for the attribute being created.
     * @param  cx           connection to the database.
     * @param  rs           connection result set.
     * @param  columnIndex  geometric column index.
     * @param  customquery  {@code true} if the request is a custom query.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    public abstract void decodeGeometryColumnType(final SingleAttributeTypeBuilder atb, final Connection cx,
            final ResultSet rs, final int columnIndex, boolean customquery) throws SQLException;

    /**
     * Gets the geometric field SRID.
     *
     * @param  schema  name of the database schema.
     * @param  table   name of the database table.
     * @param  column  name of the database column.
     * @param  cx      connection to the database.
     * @return CoordinateReferenceSystem ID in the database
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    public abstract Integer getGeometrySRID(final String schema, final String table,
            final String column, Map<String,Object> metas, final Connection cx) throws SQLException;

    /**
     * Gets a coordinate reference system from database SRID.
     *
     * @param  srid  Coordinate Reference System identifier in the database.
     * @param  cx    connection to the database.
     * @return The coordinate reference system for the given identifier.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     *
     * @todo what happen if no CRS is found for the given identifier?
     */
    public abstract CoordinateReferenceSystem createCRS(final int srid, final Connection cx) throws SQLException;
}
