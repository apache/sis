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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.setup.GeometryLibrary;


/**
 * Access to functions provided by geospatial databases.
 * Those functions may depend on the actual database product (PostGIS, etc).
 * Protected methods in this class can be overridden in subclasses
 * for handling database-specific features.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
class SpatialFunctions {
    /**
     * Whether {@link Types#TINYINT} is an unsigned integer. Both conventions (-128 … 127 range and
     * 0 … 255 range) are found on the web. If unspecified, we conservatively assume unsigned bytes.
     * All other integer types are presumed signed.
     */
    private final boolean isByteUnsigned;

    /**
     * The library to use for creating geometric objects, or {@code null} for the default.
     */
    final GeometryLibrary library;

    private final ANSIMapping defaultMapping;
    private final Optional<DialectMapping> specificMapping;

    /**
     * Creates a new accessor to geospatial functions for the database described by given metadata.
     */
    SpatialFunctions(final Connection c, final DatabaseMetaData metadata) throws SQLException {
        /*
         * Get information about whether byte are unsigned.
         * According JDBC specification, the rows shall be ordered by DATA_TYPE.
         * But the PostgreSQL driver 42.2.2 still provides rows in random order.
         */
        boolean unsigned = true;
        try (ResultSet reflect = metadata.getTypeInfo()) {
            while (reflect.next()) {
                if (reflect.getInt(Reflection.DATA_TYPE) == Types.TINYINT) {
                    unsigned = reflect.getBoolean(Reflection.UNSIGNED_ATTRIBUTE);
                    if (unsigned) break;        // Give precedence to "true" value.
                }
            }
        }
        isByteUnsigned = unsigned;
        /*
         * The library to use depends on the database implementation.
         * For now use the default library.
         */
        library = null;

        final Dialect dialect = Dialect.guess(metadata);
        specificMapping = forDialect(dialect, c);
        defaultMapping = new ANSIMapping(isByteUnsigned);
    }

    /**
     * Maps a given SQL type to a Java class.
     * This method shall not return primitive types; their wrappers shall be used instead.
     * It may return array of primitive types however.
     * If no match is found, then this method returns {@code null}.
     *
     * <p>The default implementation handles the types declared in {@link Types} class.
     * Subclasses should handle the geometry types declared by spatial extensions.</p>
     *
     * @param  columnDefinition Definition of source database column, including its SQL type and type name.
     * @return corresponding java type, or {@code null} if unknown.
     */
    @SuppressWarnings("fallthrough")
    protected ColumnAdapter<?> toJavaType(final SQLColumn columnDefinition) {
        return specificMapping.flatMap(dialect -> dialect.getMapping(columnDefinition))
                .orElseGet(() -> defaultMapping.getMappingImpl(columnDefinition));
    }

    /**
     * Creates the Coordinate Reference System associated to the the geometry SRID of a given column.
     * The {@code reflect} argument is the result of a call to {@link DatabaseMetaData#getColumns
     * DatabaseMetaData.getColumns(…)} with the cursor positioned on the row describing the column.
     *
     * <p>The default implementation returns {@code null}. Subclasses may override.</p>
     *
     * @param  reflect  the result of {@link DatabaseMetaData#getColumns DatabaseMetaData.getColumns(…)}.
     * @return Coordinate Reference System in the database for the given column, or {@code null} if unknown.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     */
    protected CoordinateReferenceSystem createGeometryCRS(ResultSet reflect) throws SQLException {
        return null;
    }

    static Optional<DialectMapping> forDialect(final Dialect dialect, Connection c) throws SQLException {
        switch (dialect) {
            case POSTGRESQL: return new PostGISMapping.Spi().create(c);
            default: return Optional.empty();
        }
    }
}
