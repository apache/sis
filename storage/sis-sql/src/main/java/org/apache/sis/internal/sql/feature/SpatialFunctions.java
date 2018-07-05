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

import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.storage.DataStoreException;


/**
 * Access to functions provided by geospatial databases.
 * Those functions may depend on the actual database product (PostGIS, etc).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class SpatialFunctions {
    /**
     * The tables to be ignored when inspecting the tables in a database schema.
     * Those tables are used for database (e.g. PostGIS) internal working.
     */
    private final Set<String> ignoredTables;

    /**
     * Creates a new accessor to geospatial functions for the database described by given metadata.
     */
    SpatialFunctions(final DatabaseMetaData metadata) throws SQLException {
        ignoredTables = new HashSet<>(4);
        /*
         * The following tables are defined by ISO 19125 / OGC Simple feature access part 2.
         * Note that the standard specified those names in upper-case letters, which is also
         * the default case specified by the SQL standard.  However some databases use lower
         * cases instead.
         */
        String crs  = "SPATIAL_REF_SYS";
        String geom = "GEOMETRY_COLUMNS";
        if (metadata.storesLowerCaseIdentifiers()) {
            crs  = crs .toLowerCase(Locale.US).intern();
            geom = geom.toLowerCase(Locale.US).intern();
        }
        ignoredTables.add(crs);
        ignoredTables.add(geom);
        final Dialect dialect = Dialect.guess(metadata);
        if (dialect == Dialect.POSTGRESQL) {
            ignoredTables.add("geography_columns");     // Postgis 1+
            ignoredTables.add("raster_columns");        // Postgis 2
            ignoredTables.add("raster_overviews");
        }
    }

    /**
     * Indicates whether a table is reserved for the database internal working.
     * If this method returns {@code false}, then the given table is a candidate
     * for use as a {@code FeatureType}.
     *
     * @param  name  database table name to test.
     * @return {@code true} if the named table should be ignored when looking for feature types.
     */
    final boolean isIgnoredTable(final String name) {
        return ignoredTables.contains(name);
    }

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
     * If a column is an auto-increment or has a sequence, tries to extract next value.
     *
     * @param  column  description of the database column for which to get the next value.
     * @param  cx      connection to the database.
     * @return column value or null if none.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     * @throws DataStoreException if another error occurred while fetching the next value.
     */
    public abstract Object nextValue(Column column, Connection cx) throws SQLException, DataStoreException;

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
    public abstract void decodeColumnType(final AttributeTypeBuilder<?> atb, final Connection cx,
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
    public abstract void decodeGeometryColumnType(final AttributeTypeBuilder<?> atb, final Connection cx,
            final ResultSet rs, final int columnIndex, boolean customquery) throws SQLException;

    /**
     * Creates the CRS associated to the the geometry SRID of a given column. The {@code reflect} argument
     * is the result of a call to {@link DatabaseMetaData#getColumns(String, String, String, String)
     * DatabaseMetaData.getColumns(…)} with the cursor positioned on the row to process.
     *
     * @param  reflect  the result of {@link DatabaseMetaData#getColumns DatabaseMetaData.getColumns(…)}.
     * @return CoordinateReferenceSystem ID in the database
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    public abstract CoordinateReferenceSystem createGeometryCRS(ResultSet reflect) throws SQLException;
}
