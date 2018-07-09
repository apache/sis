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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.sql.Types;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.metadata.sql.Reflection;


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
     * Creates a new accessor to geospatial functions for the database described by given metadata.
     */
    SpatialFunctions(final DatabaseMetaData metadata) throws SQLException {
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
     * @param  sqlType      SQL type code as one of {@link java.sql.Types} constants.
     * @param  sqlTypeName  data source dependent type name. For User Defined Type (UDT) the name is fully qualified.
     * @return corresponding java type, or {@code null} if unknown.
     */
    @SuppressWarnings("fallthrough")
    protected Class<?> toJavaType(final int sqlType, final String sqlTypeName) {
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:                 return Boolean.class;
            case Types.TINYINT:                 if (!isByteUnsigned) return Byte.class;         // else fallthrough.
            case Types.SMALLINT:                return Short.class;
            case Types.INTEGER:                 return Integer.class;
            case Types.BIGINT:                  return Long.class;
            case Types.REAL:                    return Float.class;
            case Types.FLOAT:                   // Despite the name, this is implemented as DOUBLE in major databases.
            case Types.DOUBLE:                  return Double.class;
            case Types.NUMERIC:                 // Similar to DECIMAL except that it uses exactly the specified precision.
            case Types.DECIMAL:                 return BigDecimal.class;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:             return String.class;
            case Types.DATE:                    return LocalDate.class;
            case Types.TIME:                    return LocalTime.class;
            case Types.TIMESTAMP:               return LocalDateTime.class;
            case Types.TIME_WITH_TIMEZONE:      return OffsetTime.class;
            case Types.TIMESTAMP_WITH_TIMEZONE: return OffsetDateTime.class;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:           return byte[].class;
            case Types.ARRAY:                   return Object[].class;
            case Types.OTHER:                   // Database-specific accessed via getObject and setObject.
            case Types.JAVA_OBJECT:             return Object.class;
            default:                            return null;
        }
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
}
