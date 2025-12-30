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

import java.util.Objects;
import java.util.Collection;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Description of the type of values in a column, together with a method for fetching the values from a result set.
 * The {@code ValueGetter} getter method will typically delegate to the most appropriate {@link ResultSet} getter
 * method for the column type, but may also perform some conversions such as parsing geometry Well-Known Binary (WKB).
 *
 * <p>The {@link #getValue(InfoStatements, ResultSet, int)} method is invoked with the result set cursor placed on the
 * row of interest. The index of the column to read must be specified. It allows to reuse the same {@code ValueGetter}
 * instance for an arbitrary number of columns.</p>
 *
 * <h2>Multi-threading</h2>
 * {@code ValueGetter} instances shall be thread-safe.
 *
 * @param  <T>  type of values in the column.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ValueGetter<T> {
    /**
     * A getter of {@link LocalDate} values from the current row of a {@link ResultSet}.
     * According JDBC 4.2 specification, {@link java.sql.Types#DATE} shall be mapped to
     * this class.
     */
    static final ValueGetter<LocalDate> LOCAL_DATE = new ValueGetter<>(LocalDate.class);

    /**
     * A getter of {@link LocalTime} values from the current row of a {@link ResultSet}.
     * According JDBC 4.2 specification, {@link java.sql.Types#TIME} without timezone
     * shall be mapped to this class.
     */
    static final ValueGetter<LocalTime> LOCAL_TIME = new ValueGetter<>(LocalTime.class);

    /**
     * A getter of {@link LocalDateTime} values from the current row of a {@link ResultSet}.
     * According JDBC 4.2 specification, {@link java.sql.Types#TIMESTAMP} without timezone
     * shall be mapped to this class.
     */
    static final ValueGetter<LocalDateTime> LOCAL_DATE_TIME = new ValueGetter<>(LocalDateTime.class);

    /**
     * A getter of {@link OffsetTime} values from the current row of a {@link ResultSet}.
     * According JDBC 4.2 specification, {@link java.sql.Types#TIME_WITH_TIMEZONE} shall
     * be mapped to this class.
     */
    static final ValueGetter<OffsetTime> OFFSET_TIME = new ValueGetter<>(OffsetTime.class);

    /**
     * A getter of {@link OffsetDateTime} values from the current row of a {@link ResultSet}.
     * According JDBC 4.2 specification, {@link java.sql.Types#TIMESTAMP_WITH_TIMEZONE}
     * shall be mapped to this class.
     */
    static final ValueGetter<OffsetDateTime> OFFSET_DATE_TIME = new ValueGetter<>(OffsetDateTime.class);

    /**
     * The type of Java objects fetched from the column.
     * The value shall not be a primitive type; the wrapper class shall be used instead.
     * The value may be an array of a primitive type however.
     */
    protected final Class<? extends T> valueType;

    /**
     * Creates a new column value getter.
     *
     * @param  valueType  the type of Java objects fetched from the column.
     */
    protected ValueGetter(final Class<? extends T> valueType) {
        this.valueType = Objects.requireNonNull(valueType);
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * <h4>Usage note</h4>
     * The {@code stmts} is the same reference for all features created by a new {@link FeatureIterator} instance,
     * including its dependencies. But the {@code source} will vary depending on whether we are iterating over the
     * main feature or one of its dependencies.
     *
     * <h4>Default implementation</h4>
     * The default implementation delegates to {@link ResultSet#getObject(int, Class)}.
     * This is okay if the database is known to support conversions to {@link #valueType}.
     *
     * @param  stmts        prepared statements for fetching CRS from SRID, or {@code null} if none.
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     */
    public T getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws Exception {
        return source.getObject(columnIndex, valueType);
    }

    /**
     * A getter of {@link Object} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getObject(int)} and returns that value with no change.
     */
    static final class AsObject extends ValueGetter<Object> {
        /** The unique instance of this accessor. */
        public static final AsObject INSTANCE = new AsObject();
        private AsObject() {super(Object.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Object getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws Exception {
            Object value = source.getObject(columnIndex);
            if (value instanceof Array) {
                value = toCollection(stmts, null, (Array) value);
            }
            return value;
        }
    }

    /**
     * A getter of {@link String} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getString(int)} and returns that value with no change.
     */
    static final class AsString extends ValueGetter<String> {
        /** The unique instance of this accessor. */
        public static final AsString INSTANCE = new AsString();
        private AsString() {super(String.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public String getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            return source.getString(columnIndex);
        }
    }

    /**
     * A getter of {@code byte[]} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getBytes(int)} and returns that value with no change.
     */
    static final class AsBytes extends ValueGetter<byte[]> {
        /** The encoding of bytes returned by JDBC driver. */
        private final BinaryEncoding encoding;

        /** The instance of this accessor for array of bytes without encoding. */
        public static final AsBytes INSTANCE    = new AsBytes(BinaryEncoding.RAW);
        public static final AsBytes HEXADECIMAL = new AsBytes(BinaryEncoding.HEXADECIMAL);
        private AsBytes(final BinaryEncoding encoding) {
            super(byte[].class);
            this.encoding = encoding;
        }

        /** Fetches the value from the specified column in the given result set. */
        @Override public byte[] getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            return encoding.getBytes(source, columnIndex);
        }
    }

    /**
     * A getter of signed {@link Byte} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getByte(int)} and wraps the result if the
     * value is not null.
     */
    static final class AsByte extends ValueGetter<Byte> {
        /** The unique instance of this accessor. */
        public static final AsByte INSTANCE = new AsByte();
        private AsByte() {super(Byte.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Byte getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            byte value = source.getByte(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of signed {@link Short} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getShort(int)} and wraps the result if the
     * value is not null.
     */
    static final class AsShort extends ValueGetter<Short> {
        /** The unique instance of this accessor. */
        public static final AsShort INSTANCE = new AsShort();
        private AsShort() {super(Short.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Short getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            short value = source.getShort(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of signed {@link Integer} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getInt(int)} and wraps the result if the
     * value is not null.
     */
    static final class AsInteger extends ValueGetter<Integer> {
        /** The unique instance of this accessor. */
        public static final AsInteger INSTANCE = new AsInteger();
        private AsInteger() {super(Integer.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Integer getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            int value = source.getInt(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of signed {@link Long} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getLong(int)} and wraps the result if the
     * value is not null.
     */
    static final class AsLong extends ValueGetter<Long> {
        /** The unique instance of this accessor. */
        public static final AsLong INSTANCE = new AsLong();
        private AsLong() {super(Long.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Long getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            long value = source.getLong(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of {@link Float} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getFloat(int)} and wraps the result
     * if the value is not null.
     */
    static final class AsFloat extends ValueGetter<Float> {
        /** The unique instance of this accessor. */
        public static final AsFloat INSTANCE = new AsFloat();
        private AsFloat() {super(Float.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Float getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            float value = source.getFloat(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of {@link Double} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getDouble(int)} and wraps the result
     * if the value is not null.
     */
    static final class AsDouble extends ValueGetter<Double> {
        /** The unique instance of this accessor. */
        public static final AsDouble INSTANCE = new AsDouble();
        private AsDouble() {super(Double.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Double getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            double value = source.getDouble(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of {@link BigDecimal} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getBigDecimal(int)} and returns that value with no change.
     */
    static final class AsBigDecimal extends ValueGetter<BigDecimal> {
        /** The unique instance of this accessor. */
        public static final AsBigDecimal INSTANCE = new AsBigDecimal();
        private AsBigDecimal() {super(BigDecimal.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public BigDecimal getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            return source.getBigDecimal(columnIndex);
        }
    }

    /**
     * A getter of signed {@link Boolean} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getBoolean(int)} and wraps the result if the
     * value is not null.
     */
    static final class AsBoolean extends ValueGetter<Boolean> {
        /** The unique instance of this accessor. */
        public static final AsBoolean INSTANCE = new AsBoolean();
        private AsBoolean() {super(Boolean.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Boolean getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            boolean value = source.getBoolean(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of {@link LocalDate} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getDate(int)} then converts that value
     * by a call to {@link Date#toLocalDate()}.
     *
     * <p>This is fallback used when {@link ResultSet#getObject(int, Class)} does not support the
     * conversion from {@link java.sql.Types#DATE} to Java time API as specified by JDBC 4.2.</p>
     */
    static final class AsLocalDate extends ValueGetter<LocalDate> {
        /** The unique instance of this accessor. */
        public static final AsLocalDate INSTANCE = new AsLocalDate();
        private AsLocalDate() {super(LocalDate.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public LocalDate getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            final Date date = source.getDate(columnIndex);
            return (date != null) ? date.toLocalDate() : null;
        }
    }

    /**
     * A getter of {@link LocalTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTime(int)}, then converts the object
     * by a call to {@link Time#toLocalTime()}.
     *
     * <p>This is fallback used when {@link ResultSet#getObject(int, Class)} does not support the
     * conversion from {@link java.sql.Types#TIME} to Java time API as specified by JDBC 4.2.</p>
     */
    static final class AsLocalTime extends ValueGetter<LocalTime> {
        /** The unique instance of this accessor. */
        public static final AsLocalTime INSTANCE = new AsLocalTime();
        private AsLocalTime() {super(LocalTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public LocalTime getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            final Time time = source.getTime(columnIndex);
            if (time == null) return null;
            /*
             * `Time.toLocalTime()` does not use sub-second precision.
             * However, some databases provide millisecond precision.
             */
            final int milli = (int) (time.getTime() % Constants.MILLIS_PER_SECOND);
            return time.toLocalTime().withNano(milli * Constants.NANOS_PER_MILLISECOND);
        }
    }

    /**
     * A getter of {@link LocalDateTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTimestamp(int)}, then converts the object
     * by a call to {@link Timestamp#toLocalDateTime()}.
     *
     * <p>This is fallback used when {@link ResultSet#getObject(int, Class)} does not support the
     * conversion from {@link java.sql.Types#TIMESTAMP} to Java time API as specified by JDBC 4.2.</p>
     */
    static final class AsLocalDateTime extends ValueGetter<LocalDateTime> {
        /** The unique instance of this accessor. */
        public static final AsLocalDateTime INSTANCE = new AsLocalDateTime();
        private AsLocalDateTime() {super(LocalDateTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public LocalDateTime getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            final Timestamp time = source.getTimestamp(columnIndex);
            return (time != null) ? time.toLocalDateTime() : null;
        }
    }

    /**
     * A getter of {@link OffsetDateTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTimestamp(int)}, converts the object by a
     * call to {@link Timestamp#toInstant()} then apply the time zone offset.
     *
     * <p>This is fallback used when {@link ResultSet#getObject(int, Class)} does not support conversion
     * from {@link java.sql.Types#TIMESTAMP_WITH_TIMEZONE} to Java time API as specified by JDBC 4.2.</p>
     *
     * <h4>Implementation note</h4>
     * PostgreSQL always return the time in the local time zone, while HSQLDB and H2 return the time as
     * inserted in the database but ignoring the timezone offset. The latter implies that we don't know
     * how to convert a HSQLDB and H2 date to local or UTC timezone. Current implementation assumes the
     * PostgreSQL behavior, which is the only one that we can map to {@link OffsetDateTime}.
     * Specifying a {@link java.util.Calendar} seems to have no effect.
     */
    static final class AsOffsetDateTime extends ValueGetter<OffsetDateTime> {
        /** The unique instance of this accessor. */
        public static final AsOffsetDateTime INSTANCE = new AsOffsetDateTime();
        private AsOffsetDateTime() {super(OffsetDateTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public OffsetDateTime getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            final Timestamp time = source.getTimestamp(columnIndex);
            if (time == null) return null;
            final int offsetMinute = -time.getTimezoneOffset();
            return time.toInstant().atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
        }
    }

    /**
     * A getter of {@link OffsetTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTime(int)}, converts the object by a
     * call to {@link Time#toLocalTime()} then apply the time zone offset.
     *
     * <p>This is fallback used when {@link ResultSet#getObject(int, Class)} does not support conversion
     * from {@link java.sql.Types#TIME_WITH_TIMEZONE} to Java time API as specified by JDBC 4.2.</p>
     *
     * <h4>Implementation note</h4>
     * PostgreSQL always return the time in the local time zone, while HSQLDB and H2 return the time as
     * inserted in the database but ignoring the timezone offset. The latter implies that we don't know
     * how convert a HSQLDB and H2 to local or UTC timezone. Current implementation assumes PostgreSQL
     * behavior, which is the only one that we can map to {@link OffsetTime}.
     * Specifying a {@link java.util.Calendar} seems to have no effect.
     */
    static final class AsOffsetTime extends ValueGetter<OffsetTime> {
        /** The unique instance of this accessor. */
        public static final AsOffsetTime INSTANCE = new AsOffsetTime();
        private AsOffsetTime() {super(OffsetTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public OffsetTime getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws SQLException {
            final Time time = source.getTime(columnIndex);
            if (time == null) return null;
            final int offsetMinute = -time.getTimezoneOffset();
            final int milli = (int) (time.getTime() % Constants.MILLIS_PER_SECOND);
            return time.toLocalTime().withNano(milli * Constants.NANOS_PER_MILLISECOND)
                    .atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
        }
    }

    /**
     * A getter of values specified as Java array.
     * This is okay for array of reasonable size.
     * Should not be used for very large arrays.
     */
    static final class AsArray extends ValueGetter<Collection<?>> {
        /** The getter for components in the array, or {@code null} for automatic. */
        public final ValueGetter<?> cmget;

        /** Accessor for components of automatic type. */
        public static final AsArray INSTANCE = new AsArray(null);

        /** Creates a new getter of arrays. */
        @SuppressWarnings({"unchecked","rawtypes"})
        AsArray(final ValueGetter<?> cmget) {
            super((Class) Collection.class);
            this.cmget = cmget;
        }

        /** Fetches the value from the specified column in the given result set. */
        @Override public Collection<?> getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws Exception {
            return toCollection(stmts, cmget, source.getArray(columnIndex));
        }
    }

    /**
     * Converts the given SQL array to a Java array and frees the SQL array.
     * The returned array may be a primitive array or an array of objects.
     *
     * @param  stmts  information about the statement being executed, or {@code null} if none.
     * @param  cmget  the getter for components in the array, or {@code null} for automatic.
     * @param  array  the SQL array, or {@code null} if none.
     * @return the Java array, or {@code null} if the given SQL array is null.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     */
    protected static Collection<?> toCollection(final InfoStatements stmts, ValueGetter<?> cmget, final Array array) throws Exception {
        if (array == null) {
            return null;
        }
        Object result = array.getArray();
        if (cmget == null && stmts != null) {
            /*
             * Get a function for getting values of components in the array.
             * If no match is found, then `cmget` stay null.
             */
            cmget = stmts.database.getMapping(new Column(array.getBaseType(), array.getBaseTypeName(), "element"));
        }
        Class<?> componentType = Numbers.primitiveToWrapper(result.getClass().getComponentType());
        if (cmget != null && !cmget.valueType.isAssignableFrom(componentType)) {
            /*
             * If the elements in the `result` array are not of the expected type, fetch them again
             * but this time using the converter. This fallback is inefficient because we fetch the
             * same data that we already have, but the array should be short and this fallback will
             * hopefully not be needed most of the time. It is also the only way to have the number
             * of elements in advance.
             */
            componentType = Numbers.wrapperToPrimitive(cmget.valueType);
            final int length = java.lang.reflect.Array.getLength(result);
            result = java.lang.reflect.Array.newInstance(componentType, length);
            try (ResultSet r = array.getResultSet()) {
                while (r.next()) {
                    java.lang.reflect.Array.set(result, r.getInt(1) - 1, cmget.getValue(stmts, r, 2));
                }
            }
        }
        array.free();
        if (Numbers.isNumber(componentType)) {
            return Vector.create(result, true);
        }
        return Containers.viewAsUnmodifiableList((Object[]) result);
    }
}
