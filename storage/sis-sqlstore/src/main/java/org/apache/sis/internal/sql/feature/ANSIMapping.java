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
import java.util.Optional;

/**
 * Default JDBC mapping type, used as a fallback when no other database-specific mapping can handle value binding.
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
class ANSIMapping implements DialectMapping {

    /**
     * Whether {@link Types#TINYINT} is an unsigned integer. Both conventions (-128 … 127 range and
     * 0 … 255 range) are found on the web. If unspecified, we conservatively assume unsigned bytes.
     * All other integer types are presumed signed.
     */
    private final boolean isByteUnsigned;

    ANSIMapping(boolean isByteUnsigned) {
        this.isByteUnsigned = isByteUnsigned;
    }

    @Override
    public Spi getSpi() {
        return null;
    }

    @Override
    public void close() throws SQLException {}

    @Override
    public Optional<ColumnAdapter<?>> getMapping(SQLColumn columnDefinition) {
        return Optional.ofNullable(getMappingImpl(columnDefinition));
    }

    ColumnAdapter<?> getMappingImpl(SQLColumn columnDefinition) {
        switch (columnDefinition.type) {
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
            case Types.LONGVARCHAR:             return new ColumnAdapter.Simple<>(String.class, ResultSet::getString);
            case Types.DATE:                    return new ColumnAdapter.Simple<>(Date.class, ResultSet::getDate);
            case Types.TIME:                    return new ColumnAdapter.Simple<>(LocalTime.class, ANSIMapping::toLocalTime);
            case Types.TIMESTAMP:               return new ColumnAdapter.Simple<>(Instant.class, ANSIMapping::toInstant);
            case Types.TIME_WITH_TIMEZONE:      return new ColumnAdapter.Simple<>(OffsetTime.class, ANSIMapping::toOffsetTime);
            case Types.TIMESTAMP_WITH_TIMEZONE: return new ColumnAdapter.Simple<>(OffsetDateTime.class, ANSIMapping::toODT);
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:           return new ColumnAdapter.Simple<>(byte[].class, ResultSet::getBytes);
            case Types.ARRAY:                   return forceCast(Object[].class);
            case Types.OTHER:

            // Database-specific accessed via getObject and setObject.
            case Types.JAVA_OBJECT:             return new ColumnAdapter.Simple<>(Object.class, ResultSet::getObject);
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

    private static <T> ColumnAdapter<T> forceCast(final Class<T> targetType) {
        return new ColumnAdapter.Simple<>(targetType, (r, i) -> forceCast(targetType, r, i));
    }

    private static <T> T forceCast(final Class<T> targetType, ResultSet source, final Integer columnIndex) throws SQLException {
        final Object value = source.getObject(columnIndex);
        return value == null ? null : targetType.cast(value);
    }
}
