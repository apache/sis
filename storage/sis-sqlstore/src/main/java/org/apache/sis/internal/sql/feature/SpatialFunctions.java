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
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.function.Function;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.setup.GeometryLibrary;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


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
        /*
         * The library to use depends on the database implementation.
         * For now use the default library.
         */
        library = null;
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
    protected ColumnAdapter<?> toJavaType(final int sqlType, final String sqlTypeName) {
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:                 return forceCast(Boolean.class);
            case Types.TINYINT:                 if (!isByteUnsigned) return forceCast(Byte.class);  // else fallthrough.
            case Types.SMALLINT:                return forceCast(Short.class);
            case Types.INTEGER:                 return forceCast(Integer.class);
            case Types.BIGINT:                  return forceCast(Long.class);
            case Types.REAL:                    return forceCast(Float.class);
            case Types.FLOAT:                   // Despite the name, this is implemented as DOUBLE in major databases.
            case Types.DOUBLE:                  return forceCast(Double.class);
            case Types.NUMERIC:                 // Similar to DECIMAL except that it uses exactly the specified precision.
            case Types.DECIMAL:                 return forceCast(BigDecimal.class);
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:             return new ColumnAdapter<>(String.class, ResultSet::getString);
            case Types.DATE:                    return new ColumnAdapter<>(Date.class, ResultSet::getDate);
            case Types.TIME:                    return new ColumnAdapter<>(LocalTime.class, SpatialFunctions::toLocalTime);
            case Types.TIMESTAMP:               return new ColumnAdapter<>(Instant.class, SpatialFunctions::toInstant);
            case Types.TIME_WITH_TIMEZONE:      return new ColumnAdapter<>(OffsetTime.class, SpatialFunctions::toOffsetTime);
            case Types.TIMESTAMP_WITH_TIMEZONE: return new ColumnAdapter<>(OffsetDateTime.class, SpatialFunctions::toODT);
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:           return new ColumnAdapter<>(byte[].class, ResultSet::getBytes);
            case Types.ARRAY:                   return forceCast(Object[].class);
            case Types.OTHER:                   // Database-specific accessed via getObject and setObject.
            case Types.JAVA_OBJECT:             return new ColumnAdapter<>(Object.class, ResultSet::getObject);
            default:                            return null;
        }
    }

    private static LocalTime toLocalTime(ResultSet source, int columnIndex) throws SQLException {
        final Time time = source.getTime(columnIndex);
        return time == null ? null : time.toLocalTime();
    }

    private static Instant toInstant(ResultSet source, int columnIndex) throws SQLException {
        final Timestamp t = source.getTimestamp(columnIndex);
        return t == null ? null : t.toInstant();
    }

    private static OffsetDateTime toODT(ResultSet source, int columnIndex) throws SQLException {
        final Timestamp t = source.getTimestamp(columnIndex);
        final int offsetMinute = t.getTimezoneOffset();
        return t == null ? null : t.toInstant()
                .atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
    }

    private static OffsetTime toOffsetTime(ResultSet source, int columnIndex) throws SQLException {
        final Time t = source.getTime(columnIndex);
        final int offsetMinute = t.getTimezoneOffset();
        return t == null ? null : t.toLocalTime()
                .atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
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

    private static <T> ColumnAdapter<T> forceCast(final Class<T> targetType) {
        return new ColumnAdapter<>(targetType, (r, i) -> forceCast(targetType, r, i));
    }

    private static <T> T forceCast(final Class<T> targetType, ResultSet source, final Integer columnIndex) throws SQLException {
        final Object value = source.getObject(columnIndex);
        return value == null ? null : targetType.cast(value);
    }

    protected static class ColumnAdapter<T> implements SQLBiFunction<ResultSet, Integer, T> {
        final Class<T> javaType;
        private final SQLBiFunction<ResultSet, Integer, T> fetchValue;

        protected ColumnAdapter(Class<T> javaType, SQLBiFunction<ResultSet, Integer, T> fetchValue) {
            ensureNonNull("Result java type", javaType);
            ensureNonNull("Function for value retrieval", fetchValue);
            this.javaType = javaType;
            this.fetchValue = fetchValue;
        }

        @Override
        public T apply(ResultSet resultSet, Integer integer) throws SQLException {
            return fetchValue.apply(resultSet, integer);
        }

        @Override
        public <V> SQLBiFunction<ResultSet, Integer, V> andThen(Function<? super T, ? extends V> after) {
            return fetchValue.andThen(after);
        }
    }
}
