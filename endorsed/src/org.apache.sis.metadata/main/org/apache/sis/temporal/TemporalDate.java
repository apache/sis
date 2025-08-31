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
package org.apache.sis.temporal;

import java.util.Date;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.chrono.ChronoZonedDateTime;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;


/**
 * A date which is wrapping a {@code java.time} temporal object.
 * This is used for interoperability in a situation where we are mixing
 * legacy API working on {@link Date} with newer API working on {@link Temporal}.
 * This class also opportunistically defines some methods relative to temporal objects.
 *
 * <h2>Design note</h2>
 * This class intentionally don't implement {@link Temporal} in order to force unwrapping
 * if a temporal object is desired.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalDate extends Date implements LenientComparable {     // Intentionally do not implement Temporal.
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8239300258490556354L;

    /**
     * The temporal object wrapped by this date.
     */
    @SuppressWarnings("serial")     // Most implementations are serializable.
    public final Temporal temporal;

    /**
     * Creates a new date for the given time since epoch and the given temporal object.
     *
     * @param  time     number of milliseconds since the epoch.
     * @param  temporal the temporal object to wrap in a new date.
     */
    public TemporalDate(final long time, final Temporal temporal) {
        super(time);
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
        return (time == null) ? null : new TemporalDate(toInstant(time, ZoneOffset.UTC).toEpochMilli(), time);
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
        return (time == null) ? null : new TemporalDate(time.toEpochMilli(), time);
    }

    /**
     * Returns the given date as a temporal object.
     * Used for interoperability in situations where old and new Java API are mixed.
     *
     * @param  time  the date to return as a temporal object, or {@code null}.
     * @return the given date as a temporal object, or {@code null} if the given argument was null.
     */
    public static Temporal toTemporal(final Date time) {
        if (time == null) {
            return null;
        }
        if (time instanceof TemporalDate) {
            return ((TemporalDate) time).temporal;
        }
        try {
            return time.toInstant();
        } catch (UnsupportedOperationException e) {
            if (time instanceof java.sql.Date) {
                return LocalDate.ofEpochDay(time.getTime() / Constants.MILLISECONDS_PER_DAY);
            }
            if (time instanceof java.sql.Time) {
                return LocalTime.ofSecondOfDay(time.getTime());
            }
            throw e;
        }
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
     * Compares two temporal objects that may not be of the same type.
     * First, this method tries to compare the positions on the timeline.
     * Only if this comparison cannot be done, then this method fallbacks on {@link Comparable}.
     * The comparisons are in that order because {@code Comparable.compareTo(…)} does not always
     * compare position on the timeline. It may apply additional criterion for consistency with
     * {@code equals(Object)}, which is not desired here.
     *
     * @param  t1  the first temporal object to compare.
     * @param  t2  the second temporal object to compare.
     * @return negative if {@code t1} is before {@code t2}, positive if after, or 0 if equal.
     * @throws DateTimeException if the given objects are not comparable.
     */
    public static int compare(final Temporal t1, final Temporal t2) {
        int c = -2;   // Any value different than 0 is okay, even the values potentially returned by Long.compare(…).
        try {
            c = Long.compare(t1.getLong(ChronoField.INSTANT_SECONDS), t2.getLong(ChronoField.INSTANT_SECONDS));
            if (c == 0) {
                // According Javadoc, `NANO_OF_SECOND` should be present if `INSTANT_SECONDS` is present.
                c = Long.compare(t1.getLong(ChronoField.NANO_OF_SECOND), t2.getLong(ChronoField.NANO_OF_SECOND));
            }
        } catch (DateTimeException e) {
            if (c != 0) {   // Value is 0 if `INSTANT_SECONDS` succeeded before `NANO_OF_SECOND` failed.
                if (t1 instanceof Comparable<?> && t1.getClass().isInstance(t2)) {
                    @SuppressWarnings("unchecked")
                    int cc = ((Comparable) t1).compareTo((Comparable) t2);
                    return cc;
                }
                throw e;
            }
        }
        return c;
    }

    /**
     * Compares this date with the given object.
     *
     * @param  other  the other object to compare with this date.
     * @param  mode   whether to require strict equality or to be lenient.
     * @return comparison result.
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (mode.isCompatibility() && other instanceof TemporalDate) {
            /*
             * The number of milliseconds may differ when comparing two local dates,
             * depending on which timezone was assumed when `Date` has been constructed.
             */
            if (compare(temporal, ((TemporalDate) other).temporal) == 0) {
                return true;
            }
        }
        return equals(other);
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return toInstant().toString() + " (" + temporal + ')';
    }
}
