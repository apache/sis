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
package org.apache.sis.internal.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;

// Branch-dependent imports
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;


/**
 * A date format used for parsing dates in the {@code "yyyy-MM-dd'T'HH:mm:ss.SSSX"} pattern, but in which
 * the time is optional. For this class, "Standard" is interpreted as "close to ISO 19162 requirements",
 * which is not necessarily identical to other ISO standards.
 *
 * External users should use nothing else than the parsing and formating methods.
 * The methods for configuring the {@code DateFormat} instances may or may not work
 * depending on the branch.
 *
 * <p>The main usage for this class is Well Known Text (WKT) parsing and formatting.
 * ISO 19162 uses ISO 8601:2004 for the dates. Any precision is allowed: the date could have only the year,
 * or only the year and month, <i>etc</i>. The clock part is optional and also have optional fields: can be
 * only hours, or only hours and minutes, <i>etc</i>. ISO 19162 said that the timezone is restricted to UTC
 * but nevertheless allows to specify a timezone.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.6
 * @module
 */
public final class StandardDateFormat extends DateFormat {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2764313272939921664L;

    /**
     * The {@value} timezone ID.
     */
    public static final String UTC = "UTC";

    /**
     * The thread-safe instance to use for reading and formatting dates.
     * Only the year is mandatory, all other fields are optional.
     */
    public static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
            // parseLenient() is for allowing fields with one digit instead of two.
            .parseLenient()                    .appendValue(ChronoField.YEAR, 4, 5, SignStyle.NORMAL)    // Proleptic year (use negative number if needed).
            .optionalStart().appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR,    2)
            .optionalStart().appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH,     2)
            .optionalStart().appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY,      2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR,   2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                                               .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .optionalEnd().optionalEnd().optionalEnd()    // Move back to the optional block of HOUR_OF_DAY.
            .optionalStart().appendOffsetId()
            .toFormatter(Locale.ROOT);

    /**
     * The kind of objects to get from calls to {@link #parseBest(CharSequence)}, in preference order.
     * The time is converted to UTC timezone if possible.
     *
     * Tip: if we want to preserve the timezone instead than converting to UTC, we could try replacing
     * {@code Instant::from} by {@code ZonedDateTime::from, OffsetDateTime::from}.
     */
    private static TemporalQuery<?>[] QUERIES = {
        Instant::from, LocalDateTime::from, LocalDate::from
    };

    /**
     * Parses the given date and/or time, which may have an optional timezone. This method applies heuristic rules
     * for choosing if the object should be returned as a local date, or a date and time with timezone, <i>etc</i>.
     *
     * @param  text  the character string to parse, or {@code null}.
     * @return a temporal object for the given text, or {@code null} if the given text was null.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     *
     * @since 0.8
     */
    public static Temporal parseBest(final CharSequence text) {
        // Cast is safe if all QUERIES elements return a Temporal subtype.
        return (text != null) ? (Temporal) FORMAT.parseBest(text, QUERIES) : null;
    }

    /**
     * The length of a day in number of milliseconds.
     */
    public static final int MILLISECONDS_PER_DAY = 24*60*60*1000;

    /**
     * Number of nanoseconds in one millisecond.
     */
    public static final long NANOS_PER_MILLISECOND = 1000000;

    /**
     * Number of nanoseconds in one second.
     */
    public static final long NANOS_PER_SECOND = 1000000000;

    /**
     * Converts the given legacy {@code Date} object into a {@code java.time} implementation in given timezone.
     * The method performs the following choice:
     *
     * <ul>
     *   <li>If the given date has zero values in hours, minutes, seconds and milliseconds fields in UTC timezone,
     *       then the returned implementation will be a {@link LocalDate}, dropping the timezone information (i.e.
     *       the date is considered approximative). Note that this is consistent with ISO 19162 requirement that
     *       dates are always in UTC, even if Apache SIS allows some flexibility.</li>
     *   <li>Otherwise if the timezone is not {@code null} and not UTC, then this method returns an {@link OffsetDateTime}.</li>
     *   <li>Otherwise this method returns a {@link LocalDateTime} in the given timezone.</li>
     * </ul>
     *
     * @param  date  the date to convert, or {@code null}.
     * @param  zone  the timezone of the temporal object to obtain, or {@code null} for UTC.
     * @return the temporal object for the given date, or {@code null} if the given argument was null.
     */
    public static Temporal toHeuristicTemporal(final Date date, ZoneId zone) {
        if (date == null) {
            return null;
        }
        final long time = date.getTime();
        if ((time % MILLISECONDS_PER_DAY) == 0) {
            return LocalDate.ofEpochDay(time / MILLISECONDS_PER_DAY);
        }
        final Instant instant = Instant.ofEpochMilli(time);
        if (zone == null) {
            zone = ZoneOffset.UTC;
        } else if (!zone.equals(ZoneOffset.UTC)) {
            return OffsetDateTime.ofInstant(instant, zone);
        }
        return LocalDateTime.ofInstant(instant, zone);
    }

    /**
     * Converts the given temporal object into a date.
     * The given temporal object is typically the value parsed by {@link #FORMAT}.
     *
     * @param  temporal  the temporal object to convert, or {@code null}.
     * @return the legacy date for the given temporal object, or {@code null} if the argument was null.
     * @throws DateTimeException if a value for the field cannot be obtained.
     * @throws ArithmeticException if the number of milliseconds is too large.
     */
    public static Date toDate(final TemporalAccessor temporal) {
        if (temporal == null) {
            return null;
        }
        long millis;
        if (temporal instanceof Instant) {
            millis = ((Instant) temporal).toEpochMilli();
        } else if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            millis = Math.multiplyExact(temporal.getLong(ChronoField.INSTANT_SECONDS), 1000);
            millis = Math.addExact(millis, temporal.getLong(ChronoField.NANO_OF_SECOND) / 1000000);
        } else {
            // Note that the timezone may be unknown here. We assume UTC.
            millis = Math.multiplyExact(temporal.getLong(ChronoField.EPOCH_DAY), MILLISECONDS_PER_DAY);
            if (temporal.isSupported(ChronoField.MILLI_OF_DAY)) {
                millis = Math.addExact(millis, temporal.getLong(ChronoField.MILLI_OF_DAY));
            }
        }
        return new Date(millis);
    }

    /**
     * The {@code java.time} parser and formatter. This is usually the {@link #FORMAT} instance
     * unless a different locale or timezone has been specified.
     */
    private DateTimeFormatter format;

    /**
     * Creates a new format for a default locale in the UTC timezone.
     */
    public StandardDateFormat() {
        format = FORMAT;
    }

    /**
     * Creates a new format for the given locale in the UTC timezone.
     *
     * @param locale  the locale of the format to create.
     */
    public StandardDateFormat(final Locale locale) {
        format = FORMAT.withLocale(locale);             // Same instance as FORMAT if the locales are equal.
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param locale  the locale of the format to create.
     * @param zone    the timezone.
     */
    public StandardDateFormat(final Locale locale, final TimeZone zone) {
        this(locale);
        if (!UTC.equals(zone.getID())) {
            setTimeZone(zone);
        }
    }

    /**
     * Returns the calendar, creating it when first needed. This {@code StandardDateFormat} class does not use the
     * calendar, but we nevertheless create it if requested in order to comply with {@code DateFormat} contract.
     *
     * @return a calendar, created when first needed.
     */
    @Override
    public final Calendar getCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance(getTimeZone(), format.getLocale());
        }
        return calendar;
    }

    /**
     * Returns the number format, creating it when first needed. This {@code StandardDateFormat} class does not use the
     * number format, but we nevertheless create it if requested in order to comply with {@code DateFormat} contract.
     *
     * @return a number format, created when first needed.
     */
    @Override
    public final NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance(format.getLocale());
        }
        return numberFormat;
    }

    /**
     * Returns the timezone used for formatting instants.
     *
     * @return the timezone.
     */
    @Override
    public final TimeZone getTimeZone() {
        final ZoneId zone = format.getZone();
        return TimeZone.getTimeZone(zone != null ? zone : ZoneOffset.UTC);
    }

    /**
     * Sets the timezone.
     *
     * @param  zone  the new timezone.
     */
    @Override
    public final void setTimeZone(final TimeZone zone) {
        format = format.withZone(zone.toZoneId());
        if (calendar != null) {
            super.setTimeZone(zone);
        }
    }

    /**
     * Overridden for compliance with {@code DateFormat} contract, but has no incidence on this format.
     *
     * @param  lenient  value forwarded to {@link Calendar#setLenient(boolean)}.
     */
    @Override
    public final void setLenient(boolean lenient) {
        getCalendar().setLenient(lenient);
    }

    /**
     * Overridden for compliance with {@code DateFormat} contract, but has no incidence on this format.
     *
     * @return value fetched {@link Calendar#isLenient()}.
     */
    @Override
    public final boolean isLenient() {
        return getCalendar().isLenient();
    }

    /**
     * Formats the given date. If hours, minutes, seconds and milliseconds are zero and the timezone is UTC,
     * then this method omits the clock part (unless the user has overridden the pattern).
     *
     * @param  date        the date to format.
     * @param  toAppendTo  where to format the date.
     * @param  pos         ignored.
     * @return the given buffer, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer toAppendTo, final FieldPosition pos) {
        format.formatTo(toHeuristicTemporal(date, null), toAppendTo);
        return toAppendTo;
    }

    /**
     * Parses the given text starting at the given position.
     *
     * @param  text      the text to parse.
     * @param  position  position where to start the parsing.
     * @return the date, or {@code null} if we failed to parse it.
     */
    @Override
    public Date parse(final String text, final ParsePosition position) {
        try {
            return toDate(format.parse(text, position));
        } catch (DateTimeException | ArithmeticException e) {
            position.setErrorIndex(getErrorIndex(e, position));
            return null;
        }
    }

    /**
     * Parses the given text.
     *
     * @param  text  the text to parse.
     * @return the date (never null).
     * @throws ParseException if the parsing failed.
     */
    @Override
    public Date parse(final String text) throws ParseException {
        try {
            return toDate(format.parse(text));
        } catch (DateTimeException | ArithmeticException e) {
            throw (ParseException) new ParseException(e.getLocalizedMessage(), getErrorIndex(e, null)).initCause(e);
        }
    }

    /**
     * Tries to infer the index where the parsing error occurred.
     */
    private static int getErrorIndex(final RuntimeException e, final ParsePosition position) {
        if (e instanceof DateTimeParseException) {
            return ((DateTimeParseException) e).getErrorIndex();
        } else if (position != null) {
            return position.getIndex();
        } else {
            return 0;
        }
    }

    /**
     * Returns a hash code value for this format.
     *
     * @return a hash code value for this format.
     */
    @Override
    public int hashCode() {
        return 31 * format.hashCode();
    }

    /**
     * Compares this format with the given object for equality.
     *
     * @param  obj  the object to compare with this format.
     * @return if the two objects format in the same way.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof StandardDateFormat) && format.equals(((StandardDateFormat) obj).format);
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone() {
        final StandardDateFormat clone = new StandardDateFormat();
        clone.format = format;
        return clone;
    }
}
