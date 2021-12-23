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

import java.util.Optional;
import java.util.Calendar;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * Description of the type of values in a column, together with a method for fetching the values from a result set.
 * The {@code ValueGetter} getter method will typically delegate to the most appropriate {@link ResultSet} getter
 * method for the column type, but may also perform some conversions such as parsing geometry Well-Known Binary (WKB).
 *
 * <p>The {@link #getValue(ResultSet, int)} method is invoked with the result set cursor placed on the row of interest.
 * The index of the column to read must be specified. It allows to reuse the same {@code ValueGetter} instance for an
 * arbitrary amount of columns.</p>
 *
 * @param  <T>  type of values in the column.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class ValueGetter<T> {
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
        ArgumentChecks.ensureNonNull("valueType", valueType);
        this.valueType = valueType;
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     */
    public abstract T getValue(ResultSet source, int columnIndex) throws Exception;

    /**
     * Returns the default coordinate reference system for this column.
     * The default CRS is declared in the {@code "GEOMETRY_COLUMNS"} table.
     *
     * <div class="note"><b>Note:</b>
     * this method could be used not only for geometric fields, but also on numeric ones representing 1D systems.
     * </div>
     *
     * @return the default coordinate reference system for values in this column.
     */
    public Optional<CoordinateReferenceSystem> getCRS() {
        return Optional.empty();
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
        @Override public Object getValue(ResultSet source, int columnIndex) throws SQLException {
            return source.getObject(columnIndex);
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
        @Override public String getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public byte[] getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Byte getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Short getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Integer getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Long getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Float getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Double getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public BigDecimal getValue(ResultSet source, int columnIndex) throws SQLException {
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
        @Override public Boolean getValue(ResultSet source, int columnIndex) throws SQLException {
            boolean value = source.getBoolean(columnIndex);
            return source.wasNull() ? null : value;
        }
    }

    /**
     * A getter of {@link Date} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getDate(int)} and returns that value with no change.
     *
     * @todo Delegate to {@link ResultSet#getDate(int, Calendar)} instead.
     */
    static final class AsDate extends ValueGetter<Date> {
        /** The unique instance of this accessor. */
        public static final AsDate INSTANCE = new AsDate();
        private AsDate() {super(Date.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Date getValue(ResultSet source, int columnIndex) throws SQLException {
            return source.getDate(columnIndex);
        }
    }

    /**
     * A getter of {@link LocalTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTime(int)}, then converts the object
     * by a call to {@link Time#toLocalTime()}.
     *
     * @todo Delegate to {@link ResultSet#getTime(int, Calendar)} instead.
     */
    static final class AsLocalTime extends ValueGetter<LocalTime> {
        /** The unique instance of this accessor. */
        public static final AsLocalTime INSTANCE = new AsLocalTime();
        private AsLocalTime() {super(LocalTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public LocalTime getValue(ResultSet source, int columnIndex) throws SQLException {
            final Time time = source.getTime(columnIndex);
            return (time != null) ? time.toLocalTime() : null;
        }
    }

    /**
     * A getter of {@link Instant} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTimestamp(int)}, then converts the
     * object by a call to {@link Timestamp#toInstant()}.
     *
     * @todo Delegate to {@link ResultSet#getTimestamp(int, Calendar)} instead.
     */
    static final class AsInstant extends ValueGetter<Instant> {
        /** The unique instance of this accessor. */
        public static final AsInstant INSTANCE = new AsInstant();
        private AsInstant() {super(Instant.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public Instant getValue(ResultSet source, int columnIndex) throws SQLException {
            final Timestamp time = source.getTimestamp(columnIndex);
            return (time != null) ? time.toInstant() : null;
        }
    }

    /**
     * A getter of {@link OffsetDateTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTimestamp(int)}, converts the object by a
     * call to {@link Timestamp#toInstant()} then apply the time zone offset.
     *
     * @todo Delegate to {@link ResultSet#getTimestamp(int, Calendar)} instead.
     */
    static final class AsOffsetDateTime extends ValueGetter<OffsetDateTime> {
        /** The unique instance of this accessor. */
        public static final AsOffsetDateTime INSTANCE = new AsOffsetDateTime();
        private AsOffsetDateTime() {super(OffsetDateTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public OffsetDateTime getValue(ResultSet source, int columnIndex) throws SQLException {
            final Timestamp time = source.getTimestamp(columnIndex);
            if (time == null) return null;
            final int offsetMinute = time.getTimezoneOffset();
            return time.toInstant().atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
        }
    }

    /**
     * A getter of {@link OffsetDateTime} values from the current row of a {@link ResultSet}.
     * This getter delegates to {@link ResultSet#getTime(int)}, converts the object by a call
     * to {@link Time#toLocalTime()} then apply the time zone offset.
     *
     * @todo Delegate to {@link ResultSet#getTime(int, Calendar)} instead.
     */
    static final class AsOffsetTime extends ValueGetter<OffsetTime> {
        /** The unique instance of this accessor. */
        public static final AsOffsetTime INSTANCE = new AsOffsetTime();
        private AsOffsetTime() {super(OffsetTime.class);}

        /** Fetches the value from the specified column in the given result set. */
        @Override public OffsetTime getValue(ResultSet source, int columnIndex) throws SQLException {
            final Time time = source.getTime(columnIndex);
            if (time == null) return null;
            final int offsetMinute = time.getTimezoneOffset();
            return time.toLocalTime().atOffset(ZoneOffset.ofHoursMinutes(offsetMinute / 60, offsetMinute % 60));
        }
    }
}
