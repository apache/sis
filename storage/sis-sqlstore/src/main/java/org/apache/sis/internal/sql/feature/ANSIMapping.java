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

import org.apache.sis.internal.metadata.sql.Dialect;

public class ANSIMapping implements DialectMapping {

    /**
     * Whether {@link Types#TINYINT} is an unsigned integer. Both conventions (-128 … 127 range and
     * 0 … 255 range) are found on the web. If unspecified, we conservatively assume unsigned bytes.
     * All other integer types are presumed signed.
     */
    private final boolean isByteUnsigned;

    public ANSIMapping(boolean isByteUnsigned) {
        this.isByteUnsigned = isByteUnsigned;
    }

    @Override
    public Dialect getDialect() {
        return Dialect.ANSI;
    }

    @Override
    public Optional<ColumnAdapter<?>> getMapping(int sqlType, String sqlTypeName) {
        return Optional.ofNullable(getMappingImpl(sqlType, sqlTypeName));
    }

    public ColumnAdapter<?> getMappingImpl(int sqlType, String sqlTypeName) {
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
            case Types.TIME:                    return new ColumnAdapter<>(LocalTime.class, ANSIMapping::toLocalTime);
            case Types.TIMESTAMP:               return new ColumnAdapter<>(Instant.class, ANSIMapping::toInstant);
            case Types.TIME_WITH_TIMEZONE:      return new ColumnAdapter<>(OffsetTime.class, ANSIMapping::toOffsetTime);
            case Types.TIMESTAMP_WITH_TIMEZONE: return new ColumnAdapter<>(OffsetDateTime.class, ANSIMapping::toODT);
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

    private static <T> ColumnAdapter<T> forceCast(final Class<T> targetType) {
        return new ColumnAdapter<>(targetType, (r, i) -> forceCast(targetType, r, i));
    }

    private static <T> T forceCast(final Class<T> targetType, ResultSet source, final Integer columnIndex) throws SQLException {
        final Object value = source.getObject(columnIndex);
        return value == null ? null : targetType.cast(value);
    }
}
