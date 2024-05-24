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
package org.apache.sis.util.privy;

import java.util.Date;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.chrono.ChronoZonedDateTime;
import org.apache.sis.util.resources.Errors;


/**
 * A date which is wrapping a {@code java.time} temporal object.
 * This is used for interoperability in a situation where we are mixing
 * legacy API working on {@link Date} with newer API working on {@link Temporal}.
 *
 * <h2>Design note</h2>
 * This class intentionally don't implement {@link Temporal} in order to force unwrapping
 * if a temporal object is desired.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalDate extends Date {     // Intentionally do not implement Temporal.
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8239300258490556354L;

    /**
     * The temporal object wrapped by this date.
     */
    @SuppressWarnings("serial")     // Most implementations are serializable.
    private final Temporal temporal;

    /**
     * Creates a new date for the given instant.
     *
     * @param  temporal the temporal object to wrap in a new date.
     * @throws ArithmeticException if numeric overflow occurs.
     */
    private TemporalDate(final Instant temporal) {
        super(temporal.toEpochMilli());
        this.temporal = temporal;
    }

    /**
     * Creates a new date for the given temporal.
     *
     * @param  temporal the temporal object to wrap in a new date.
     * @throws ArithmeticException if numeric overflow occurs.
     */
    private TemporalDate(final Temporal temporal) {
        super(toInstant(temporal, ZoneOffset.UTC).toEpochMilli());
        this.temporal = temporal;
    }

    /**
     * Returns the given temporal object as a date.
     * Used for interoperability in situations where old and new Java API are mixed.
     *
     * @param  time  the temporal object to return as a date, or {@code null}.
     * @return the given temporal object as a date, or {@code null} if the given argument was null.
     * @throws ArithmeticException if numeric overflow occurs.
     */
    public static Date toDate(final Temporal time) {
        return (time == null) ? null : new TemporalDate(time);
    }

    /**
     * Returns the given temporal object as a date.
     * Used for interoperability in situations where old and new Java API are mixed.
     *
     * @param  time  the temporal object to return as a date, or {@code null}.
     * @return the given temporal object as a date, or {@code null} if the given argument was null.
     * @throws ArithmeticException if numeric overflow occurs.
     */
    public static Date toDate(final Instant time) {
        return (time == null) ? null : new TemporalDate(time);
    }

    /**
     * Returns the given date as a temporal object.
     * Used for interoperability in situations where old and new Java API are mixed.
     *
     * @param  time  the date to return as a temporal object, or {@code null}.
     * @return the given date as a temporal object, or {@code null} if the given argument was null.
     */
    public static Temporal toTemporal(final Date time) {
        return (time == null) ? null : (time instanceof TemporalDate) ? ((TemporalDate) time).temporal : time.toInstant();
    }

    /**
     * Returns the given date as an instant object.
     * Used for interoperability in situations where old and new Java API are mixed.
     *
     * @param  time  the date to return as an instant object, or {@code null}.
     * @return the given date as an instant object, or {@code null} if the given argument was null.
     */
    public static Instant toInstant(final Date time) {
        return (time == null) ? null : time.toInstant();
    }

    /**
     * Converts the given temporal object into an instant.
     * If the timezone is unspecified, then UTC is assumed.
     *
     * @param  date  the temporal object to convert, or {@code null}.
     * @param  zone  the timezone to use if the time is local, or {@code null} if none.
     * @return the instant for the given temporal object, or {@code null} if the argument was null.
     * @throws DateTimeException if the given date does not support a field required by this method.
     */
    public static Instant toInstant(final TemporalAccessor date, final ZoneId zone) {
        if (date == null) {
            return null;
        }
        if (date instanceof Instant) {
            return (Instant) date;
        } else if (date instanceof OffsetDateTime) {
            return ((OffsetDateTime) date).toInstant();
        } else if (date instanceof ChronoZonedDateTime) {
            return ((ChronoZonedDateTime) date).toInstant();
        } else if (zone != null) {
            if (date instanceof LocalDateTime) {
                final var t = (LocalDateTime) date;
                if (zone instanceof ZoneOffset) {
                    return t.atOffset((ZoneOffset) zone).toInstant();
                } else {
                    return t.atZone(zone).toInstant();
                }
            } else if (date instanceof LocalDate) {
                final var t = (LocalDate) date;
                return t.atStartOfDay(zone).toInstant();
            }
        }
        Instant time;
        final ChronoField nano;
        if (zone == null || date.isSupported(ChronoField.INSTANT_SECONDS)) {
            time = Instant.ofEpochSecond(date.getLong(ChronoField.INSTANT_SECONDS));
            nano = ChronoField.NANO_OF_SECOND;
        } else if (zone.equals(ZoneOffset.UTC)) {
            // Note that the timezone of the temporal value is unknown here. We assume UTC.
            time = Instant.ofEpochSecond(Math.multiplyExact(date.getLong(ChronoField.EPOCH_DAY), Constants.SECONDS_PER_DAY));
            nano = ChronoField.NANO_OF_DAY;
        } else {
            throw new DateTimeException(Errors.format(Errors.Keys.CanNotConvertFromType_2, date.getClass(), Instant.class));
        }
        if (date.isSupported(nano)) {
            time = time.plusNanos(date.getLong(nano));
        }
        return time;
    }

    /**
     * Returns this date as an instant.
     */
    @Override
    public Instant toInstant() {
        if (temporal instanceof Instant) {
            return (Instant) temporal;
        }
        return super.toInstant();
    }

    /**
     * Adds the given amount of seconds to the given instant.
     *
     * @param  time   the instant to which to add seconds, or {@code null}.
     * @param  value  number of seconds.
     * @return the shifted time, or {@code null} if the given instant was null or the given value was NaN.
     */
    public static Instant addSeconds(final Instant time, final double value) {
        if (time == null || Double.isNaN(value)) {
            return null;
        }
        final long r = Math.round(value);
        return time.plusSeconds(r).plusNanos(Math.round((value - r) * Constants.NANOS_PER_SECOND));
    }

    /**
     * Returns {@code true} if objects of the given class have day, month and hour fields.
     * This method is defined here for having a single class where to concentrate such heuristic rules.
     * Note that {@link Instant} does not have date fields.
     *
     * @param  date  class of object to test (may be {@code null}).
     * @return whether the given class is {@link LocalDate} or one of the classes with date + time.
     *         This list may be expanded in future versions.
     */
    public static boolean hasDateFields(final Class<?> date) {
        return date == LocalDate.class
            || date == LocalDateTime.class
            || date == OffsetDateTime.class
            || date == ZonedDateTime.class;
    }

    /**
     * Returns {@code true} if objects of the given class have time fields.
     * This method is defined here for having a single class where to concentrate such heuristic rules.
     * Note that {@link Instant} does not have hour fields.
     *
     * @param  date  class of object to test (may be {@code null}).
     * @return whether the given class is {@link LocalTime}, {@link OffsetTime} or one of the classes with date + time.
     *         This list may be expanded in future versions.
     */
    public static boolean hasTimeFields(final Class<?> date) {
        return date == LocalTime.class
            || date == OffsetTime.class
            || date == LocalDateTime.class
            || date == OffsetDateTime.class
            || date == ZonedDateTime.class;
    }
}
