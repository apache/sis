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
package org.apache.sis.internal.sql.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.sql.feature.ColumnMetaModel;
import org.apache.sis.internal.sql.feature.Dialect;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.storage.DataStoreException;


/**
 * Implements PostgreSQL-specific functionalities.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class PostgresDialect extends Dialect {

    private static final Set<String> IGNORE_TABLES = new HashSet<>(8);
    static {
        // Postgis 1+ geometry and referencing
        IGNORE_TABLES.add("spatial_ref_sys");
        IGNORE_TABLES.add("geometry_columns");
        IGNORE_TABLES.add("geography_columns");
        // Postgis 2 raster
        IGNORE_TABLES.add("raster_columns");
        IGNORE_TABLES.add("raster_overviews");
    }

    @Override
    public boolean isTableIgnored(String name) {
        return IGNORE_TABLES.contains(name.toLowerCase());
    }

    @Override
    public Class<?> getJavaType(int sqlType, String sqlTypeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object nextValue(ColumnMetaModel column, Connection cx) throws SQLException, DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getColumnSequence(Connection cx, String schemaName, String tableName, String columnName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void decodeColumnType(AttributeTypeBuilder<?> atb, Connection cx, String typeName, int datatype, String schemaName, String tableName, String columnName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void decodeGeometryColumnType(AttributeTypeBuilder<?> atb, Connection cx, ResultSet rs, int columnIndex, boolean customquery) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName, Map<String, Object> metas, Connection cx) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoordinateReferenceSystem createCRS(int srid, Connection cx) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
