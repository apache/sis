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
package org.apache.sis.storage.sql.feature;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.geometry.wrapper.GeometryType;


/**
 * Specifies how the geometry type is encoded in the {@code "GEOMETRY_TYPE"} column.
 * The OGC standard defines numeric values, but PostGIS uses textual values.
 *
 * @see #configureSpatialColumns(PreparedStatement, TableReference, Map, GeometryTypeEncoding)
 */
public enum GeometryTypeEncoding {
    /**
     * {@code "GEOMETRY_TYPE"} column is expected to contain an integer value.
     * This is the encoding used in OGC standard.
     */
    NUMERIC,

    /**
     * {@code "GEOMETRY_TYPE"} column is expected to contain a textual value.
     * This is the encoding used by PostGIS, but using a different column name
     * ({@code "TYPE"} instead of {@code "GEOMETRY_TYPE"}) for avoiding confusion.
     */
    TEXTUAL() {
        @Override GeometryType parse(final ResultSet result, final int columnIndex) throws SQLException {
            return GeometryType.forName(result.getString(columnIndex));
        }
    };

    /**
     * Decodes the geometry type encoded in the specified column of the given result set.
     * If there is no type information, then this method returns {@code null}.
     *
     * @throws IllegalArgumentException if the type cannot be decoded.
     */
    GeometryType parse(final ResultSet result, final int columnIndex) throws SQLException {
        final int code = result.getInt(columnIndex);
        return result.wasNull() ? null : GeometryType.forBinaryType(code);
    }
}
