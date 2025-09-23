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

import java.util.List;
import java.util.Optional;
import java.io.Serializable;
import java.time.Period;
import java.time.Duration;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Instant;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.temporal.IndeterminatePositionException;


/**
 * A data type to be used for describing length or distance in the temporal dimension.
 * This implementation combines {@link java.time.Period} with {@link java.time.Duration}
 * for situations where both of them are needed together (which is not recommended).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeneralDuration implements TemporalAmount, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -521478824158640275L;

    /**
     * The period in numbers of years, months and days.
     * Shall be non-null and non-zero.
     */
    public final Period period;

    /**
     * The time part of the period in numbers of hours, minutes and seconds.
     * Shall be non-null, non-zero and less than one day.
     */
    public final Duration time;

    /**
     * Creates a new instance with the given parts.
     * The two parts must be non-null and non-zero.
     *
     * @param  period  the period in numbers of years, months and days.
     * @param  time    the time part of the period in numbers of hours, minutes and seconds.
     */
    private GeneralDuration(final Period period, final Duration time) {
        this.period = period;
        this.time   = time;
    }

    /**
     * Returns the duration for the given components.
     * If any component is zero, the other component is returned.
     *
     * @param  period  the period.
     * @param  time    the component.
     * @return the temporal amount from the given components.
     */
    public static TemporalAmount of(final Period period, final Duration time) {
        if (period.isZero()) return time;
        if (time.isZero()) return period;
        return new GeneralDuration(period, time);
    }

    /**
     * Parses a temporal amount which may contain a period and a duration part.
     * This method returns a {@link Period} or {@link Duration} if those objects
     * are sufficient, or an instance of {@code GeneralDuration} is last resort.
     *
     * @param  text  the text to parse.
     * @return the parsed period and/or duration.
     * @throws DateTimeParseException if the given text cannot be parsed.
     *
     * @see Period#parse(CharSequence)
     * @see Duration#parse(CharSequence)
     */
    public static TemporalAmount parse(final CharSequence text) {
        char previousLetter = 0;
        final int length = text.length();
        for (int i=0; i<length; i++) {
            char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') c -= 'a' - 'A';       // Quick upper case, ASCII characters only.
            if (c >= 'A' && c <= 'Z') {
                if (c == 'T') {
                    if (previousLetter == 'P') {
                        return Duration.parse(text);
                    }
                    return of(Period.parse(text.subSequence(0, i)),
                            Duration.parse(new StringBuilder(length - i + 1).append('P').append(text, i, length)));
                }
                previousLetter = c;
            }
        }
        return Period.parse(text);
    }

    /**
     * Returns the temporal position of the given instant if that position is determinate or is "now".
     * Otherwise, throws an exception. If the position is "now", then this method returns {@code null}
     * instead of fetching the current time in order to avoid mismatch when comparing two "now" values
     * that are a few nanoseconds apart.
     *
     * @param  t  the instant for which to get the temporal position, or {@code null}.
     * @return temporal position of the given instant, or {@code null} for "now".
     * @throws DateTimeException if the given instant is null or its position is indeterminate.
     */
    private static Temporal getDeterminatePosition(final Instant t) {
        if (t != null) {
            final Optional<IndeterminateValue> p = t.getIndeterminatePosition();
            if (p.isEmpty()) {
                return t.getPosition();
            }
            if (p.get() == IndeterminateValue.NOW) {
                return null;        // Avoid fetching the current time now.
            }
        }
        throw new IndeterminatePositionException(Errors.format(Errors.Keys.IndeterminatePosition));
    }

    /**
     * Returns the distance between the two given ISO 19108 instants.
     * If the result is negative, then the return value depends on the {@code absolute} argument:
     * If {@code true}, this method returns the absolute value. Otherwise, it returns {@code null}.
     *
     * <p>If everything else is equal, methods such as {@link ChronoLocalDate#until(ChronoLocalDate)}
     * will be invoked on the {@code self} instance. It makes a difference in the type of the result.
     * For computing a duration with arguments in the reverse order, the {@code negate} parameter can
     * be set to {@code true}.</p>
     *
     * @param  self      the first instant from which to measure the distance.
     * @param  other     the second instant from which to measure the distance.
     * @param  negate    whether to negate the result. True for duration from {@code other} to {@code self}.
     * @param  absolute  whether to return absolute value. If false, negative result is replaced by null.
     * @return the distance, or {@code null} if the result is negative and {@code absolute} is false.
     * @throws DateTimeException if the duration cannot be computed.
     * @throws ArithmeticException if the calculation exceeds the integer capacity.
     */
    static TemporalAmount distance(final Instant self, final Instant other, final boolean negate, final boolean absolute) {
        /*
         * Get the temporal value, or null if "now". Other indeterminate values cause an exception to be thrown.
         * We use null for "now" instead of fetching the current time for two reasons: avoid mismatch by a few
         * nanoseconds when comparing `t1` with `t2`, and for choosing a type compatible with the other instant.
         */
        Temporal t1 = getDeterminatePosition(self);
        Temporal t2 = getDeterminatePosition(other);
        /*
         * Ensures that the given objects both have a date part, or that none of them have a date part.
         * Note that the "epoch day" field is supported by `LocalDate` as well as the dates with zone ID.
         */
        final boolean hasDate = isSupportedByBoth(ChronoField.EPOCH_DAY, t1, t2);
        /*
         * If at least one date has a timezone, then we require that both dates have a timezone.
         * It allows an unambiguous duration in number of days, without time-varying months or years.
         * If one date has a timezone and the other does not, a `DateTimeException` will be thrown.
         *
         * Note 1: we could be lenient and handle the two dates as if they were local, ignoring the timezone.
         * But we avoid false sense of accuracy for now. We may revisit this policy later if there is a need.
         */
        if (t1 != null && t1.isSupported(ChronoField.OFFSET_SECONDS)) {
            if (t2 == null) t2 = ZonedDateTime.now();
            final Duration p = Duration.between(t1, t2);
            return (absolute || p.isNegative() == negate) ? p.abs() : null;
        }
        if (t2 != null && (!hasDate || t2.isSupported(ChronoField.OFFSET_SECONDS))) {
            if (t1 == null) t1 = ZonedDateTime.now();
            final Duration p = Duration.between(t2, t1);        // Negative of the result.
            return (absolute || p.isNegative() != negate) ? p.abs() : null;
        }
        /*
         * Ensure that the given temporal objects both have a time part, or none of them have a time part.
         * If only one of them has a time part, we do not interpret the other one as an instant at midnight
         * in order to avoid false sense of accuracy.
         */
        final boolean hasTime = isSupportedByBoth(ChronoField.SECOND_OF_DAY, t1, t2);
        if (t1 == null) t1 = LocalDateTime.now();
        if (t2 == null) t2 = LocalDateTime.now();
        ChronoLocalDate d1 = null, d2 = null;
        if (hasDate) {
            d1 = ChronoLocalDate.from(t1);
            d2 = ChronoLocalDate.from(t2);
            if (!absolute && (negate ? d1.isBefore(d2) : d1.isAfter(d2))) {
                return null;        // Stop early if we can.
            }
        }
        /*
         * Compute the duration in the time part. If negative (after negation if `negate` is true),
         * then we add the necessary number of days to make it positive and remove the same number
         * of days from the date. We adjust the date instead of the period computed by `d1.until(d2)`
         * in order to have the correct adjustment for the variable number of days in each month.
         */
        Duration time = Duration.ZERO;
        if (hasTime) {
            time = Duration.between(LocalTime.from(t1), LocalTime.from(t2));
            if (hasDate) {
                final boolean isPositive = d1.isBefore(d2);
                if (isPositive || d1.isAfter(d2)) {                 // Require the period to be non-zero.
                    if (isPositive ? time.isNegative() : JDK18.isPositive(time)) {
                        long n = time.toDays();                     // Truncated toward 0.
                        if (isPositive) {
                            d2 = d2.plus (--n, ChronoUnit.DAYS);    // `n` is negative. Reduces period by decreasing the ending.
                        } else {
                            d1 = d1.minus(++n, ChronoUnit.DAYS);    // `n` is positive. Reduces period by increasing the beginning.
                        }
                        time = time.minusDays(n);                   // If negative, make positive. If positive, make negative.
                    }
                }
            }
        }
        /*
         * Get the period for the date part, then combine with the time part if non-zero.
         * The result shall be either positive or null.
         */
        if (hasDate) {
            ChronoPeriod period = d1.until(d2);
            if (period.isZero()) {
                if (time.isZero()) {
                    return period;
                }
            } else {
                if (period.isNegative()) {
                    if (!(negate | absolute)) {                 // Equivalent to (!negate && !absolute).
                        return null;
                    }
                    period = period.negated();
                } else if (negate & !absolute) {                // Equivalent to (negate && !absolute).
                    return null;
                }
                return time.isZero() ? period : new GeneralDuration(Period.from(period), time.abs());
            }
        }
        return (absolute || time.isNegative() == negate) ? time.abs() : null;
    }

    /**
     * Verifies if the given field is supported by both temporal objects.
     * Either the field is supported by both objects, or either it is supported by none of them.
     * If one object support the field and the other does not, the two objects are considered incompatible.
     * At least one of the given objects shall be non-null.
     *
     * @param  field  the field to test.
     * @param  t1     the first temporal object, or {@code null} for "now".
     * @param  t2     the second temporal object, or {@code null} for "now".
     * @return whether the given object is supported.
     * @throws DateTimeException if the two objects are incompatible.
     */
    private static boolean isSupportedByBoth(final ChronoField field, final Temporal t1, final Temporal t2) {
        final boolean isSupported = (t1 != null ? t1 : t2).isSupported(field);
        if (t1 != null && t2 != null && isSupported != t2.isSupported(field)) {
            throw new DateTimeException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                    (isSupported ? t2 : t1).getClass(),
                    (isSupported ? t1 : t2).getClass()));
        }
        return isSupported;
    }

    /**
     * Returns the list of units that are supported by this implementation.
     * This is the union of the units supported by the date part and by the time part.
     */
    @Override
    public List<TemporalUnit> getUnits() {
        var prefix = period.getUnits();
        var suffix = time.getUnits();
        int i      = prefix.size();
        var units  = prefix.toArray(new TemporalUnit[i + suffix.size()]);
        for (TemporalUnit unit : suffix) {
            units[i++] = unit;
        }
        return UnmodifiableArrayList.wrap(units);
    }

    /**
     * Returns the value of the requested unit.
     *
     * @param  unit  the unit to get;
     * @return value of the specified unit.
     * @throws UnsupportedTemporalTypeException if the {@code unit} is not supported.
     */
    @Override
    public long get(final TemporalUnit unit) {
        return (unit.isDateBased() ? period : time).get(unit);
    }

    /**
     * Adds this duration to the specified temporal object.
     *
     * @param temporal  the temporal object to add the amount to.
     * @return an object with the addition done.
     */
    @Override
    public Temporal addTo(Temporal temporal) {
        return temporal.plus(period).plus(time);
    }

    /**
     * Subtracts this duration from the specified temporal object.
     *
     * @param temporal  the temporal object to subtract the amount from.
     * @return an object with the subtraction done.
     */
    @Override
    public Temporal subtractFrom(Temporal temporal) {
        return temporal.minus(period).minus(time);
    }

    /**
     * Compares this duration with the given object for equality.
     *
     * @param  object  the object to compare with this duration.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof GeneralDuration) {
            final var other = (GeneralDuration) object;
            return period.equals(other.period) && time.equals(other.time);
        }
        return false;
    }

    /**
     * Returns a hash code value for this duration.
     *
     * @return a hash code value for this duration.
     */
    @Override
    public int hashCode() {
        return period.hashCode() * 31 + time.hashCode();
    }

    /**
     * Returns this duration in ISO 8601 format.
     */
    @Override
    public String toString() {
        return period.toString() + time.toString().substring(1);
    }
}
