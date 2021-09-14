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
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

import org.apache.sis.util.CharSequences;


/**
 * A date format used for parsing dates in the {@code "yyyy-MM-dd'T'HH:mm:ss.SSSX"} pattern, but in which
 * the time is optional. For this class, "Standard" is interpreted as "close to ISO 19162 requirements",
 * which is not necessarily identical to other ISO standards.
 *
 * <p>External users should use nothing else than the parsing and formatting methods.
 * The methods for configuring the {@code DateFormat} instances may or may not work
 * depending on the branch.</p>
 *
 * <p>The main usage for this class is Well Known Text (WKT) parsing and formatting.
 * ISO 19162 uses ISO 8601:2004 for the dates. Any precision is allowed: the date could have only the year,
 * or only the year and month, <i>etc</i>. The clock part is optional and also have optional fields: can be
 * only hours, or only hours and minutes, <i>etc</i>. ISO 19162 said that the timezone is restricted to UTC
 * but nevertheless allows to specify a timezone.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
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
     *
     * @see ZoneOffset#UTC
     */
    public static final String UTC = "UTC";

    /**
     * Midnight (00:00) UTC.
     */
    private static final OffsetTime MIDNIGHT = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * The thread-safe instance to use for reading and formatting dates.
     * Only the year is mandatory, all other fields are optional at parsing time.
     * However all fields are written, including milliseconds at formatting time.
     *
     * @see #parseInstantUTC(CharSequence, int, int)
     */
    public static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
            .parseLenient()                    // For allowing fields with one digit instead of two.
            .parseCaseInsensitive()            .appendValue(ChronoField.YEAR, 4, 5, SignStyle.NORMAL)    // Proleptic year (use negative number if needed).
            .optionalStart().appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR,    2)
            .optionalStart().appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH,     2)
            .optionalStart().appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY,      2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR,   2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                                               .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .optionalEnd().optionalEnd().optionalEnd()    // Move back to the optional block of HOUR_OF_DAY.
            .optionalStart().appendZoneOrOffsetId()
            .toFormatter(Locale.ROOT);

    /**
     * The kinds of objects to get from calls to {@link #parseBest(CharSequence)}, in preference order.
     * The time is converted to UTC timezone if possible.
     *
     * <div class="note"><b>Tip:</b>
     * if we want to preserve the timezone instead of converting to UTC, we could try replacing
     * {@code Instant::from} by {@code ZonedDateTime::from, OffsetDateTime::from}.</div>
     *
     * @see #parseInstantUTC(CharSequence, int, int)
     */
    private static TemporalQuery<?>[] QUERIES = {
        Instant::from, LocalDateTime::from, LocalDate::from
    };

    /**
     * Parses the given date and/or time, which may have an optional timezone. This method applies heuristic rules
     * for choosing if the object should be returned as a local date, or a date and time with timezone, <i>etc</i>.
     * The full date format is of the form "1970-01-01T00:00:00.000Z", but this method also accepts spaces in place
     * of 'T' as in "1970-01-01 00:00:00".
     *
     * @param  text  the character string to parse, or {@code null}.
     * @return a temporal object for the given text, or {@code null} if the given text was null.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     *
     * @since 0.8
     */
    public static Temporal parseBest(final CharSequence text) {
        // Cast is safe if all QUERIES elements return a Temporal subtype.
        return (text != null) ? (Temporal) FORMAT.parseBest(toISO(text, 0, text.length()), QUERIES) : null;
    }

    /**
     * Parses the given date as an instant, assuming UTC timezone if unspecified.
     *
     * @param  text   the text to parse as an instant in UTC timezone by default, or {@code null}.
     * @return the instant for the given text, or {@code null} if the given text was null.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     */
    public static Instant parseInstantUTC(final CharSequence text) {
        return (text != null) ? parseInstantUTC(text, 0, text.length()) : null;
    }

    /**
     * Parses the given date as an instant, assuming UTC timezone if unspecified.
     *
     * @param  text   the text to parse as an instant in UTC timezone by default.
     * @param  lower  index of the first character to parse.
     * @param  upper  index after the last character to parse.
     * @return the instant for the given text.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     */
    public static Instant parseInstantUTC(final CharSequence text, final int lower, final int upper) {
        TemporalAccessor date = FORMAT.parseBest(toISO(text, lower, upper), QUERIES);
        if (date instanceof Instant) {
            return (Instant) date;
        }
        final OffsetDateTime time;
        if (date instanceof LocalDateTime) {
            time = ((LocalDateTime) date).atOffset(ZoneOffset.UTC);
        } else {
            time = ((LocalDate) date).atTime(MIDNIGHT);
        }
        return time.toInstant();
    }

    /**
     * Modifies the given date and time string for making it more compliant to ISO syntax.
     * If date and time are separated by spaces, then this method replaces those spaces by
     * the 'T' letter. All other spaces that are not between two digits are removed.
     *
     * @param  text   the text to make more compliant with ISO syntax.
     * @param  lower  index of the first character to examine.
     * @param  upper  index after the last character to examine.
     * @return sub-sequence of {@code text} from {@code lower} to {@code upper}, potentially modified.
     */
    static CharSequence toISO(CharSequence text, int lower, int upper) {
        lower = CharSequences.skipLeadingWhitespaces (text, lower, upper);
        upper = CharSequences.skipTrailingWhitespaces(text, lower, upper);
        StringBuilder buffer = null;
        int cp = 0;   // Non-whitespace character from previous iteration.
        for (int i = upper; i > lower;) {
            int c = Character.codePointBefore(text, i);
            int n = Character.charCount(c);
replace:    if (Character.isWhitespace(c)) {
                /*
                 * Found whitespaces from `i` inclusive (after computation below) to `end` exclusive.
                 * If no concurrent change, i > lower because text.charAt(lower) is not a whitespace.
                 * Set `c` to the character before whitespaces. `cp` is the character after spaces.
                 */
                int end = i;
                i = CharSequences.skipTrailingWhitespaces(text, lower, i - n);
                c = Character.codePointBefore(text, i);
                n = Character.charCount(c);
                boolean isDateTimeSeparator = false;
                if (Character.isDigit(cp) && Character.isDigit(c)) {
                    /*
                     * If the character before and after whitespaces are digits, maybe we have
                     * the separation between date and timezone. Use `:` position as a check.
                     */
                    isDateTimeSeparator = CharSequences.indexOf(text, ':', lower, upper) > end;
                    if (!isDateTimeSeparator) break replace;               // Skip replacement.
                }
                if (buffer == null) {
                    text  = buffer = new StringBuilder(upper - lower).append(text, lower, upper);
                    i    -= lower;
                    end  -= lower;
                    lower = 0;
                }
                if (isDateTimeSeparator) {
                    buffer.replace(i, end, "T");
                } else {
                    buffer.delete(i, end);
                }
                upper = buffer.length();
            }
            i -= n;
            cp = c;
        }
        return text.subSequence(lower, upper);
    }

    /**
     * The length of a day in number of milliseconds.
     * Can be casted to {@code float} with exact precision.
     */
    public static final int MILLISECONDS_PER_DAY = 24*60*60*1000;

    /**
     * Number of milliseconds in one second.
     * Can be casted to {@code float} with exact precision.
     */
    public static final int MILLIS_PER_SECOND = 1000;

    /**
     * Number of nanoseconds in one millisecond.
     * Can be casted to {@code float} with exact precision.
     */
    public static final int NANOS_PER_MILLISECOND = 1000_000;

    /**
     * Number of nanoseconds in one second.
     * Can be casted to {@code float} with exact precision.
     */
    public static final int NANOS_PER_SECOND = 1000_000_000;

    /**
     * Converts the given legacy {@code Date} object into a {@code java.time} implementation in given timezone.
     * The method performs the following choice:
     *
     * <ul>
     *   <li>If the given date has zero values in hours, minutes, seconds and milliseconds fields in UTC timezone,
     *       then the returned implementation will be a {@link LocalDate}, dropping the timezone information (i.e.
     *       the date is considered an approximation). Note that this is consistent with ISO 19162 requirement that
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
     * Contrarily to {@link #parse(String)}, this method does not accept spaces as a separator between date and time.
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
     * Parses the given text. This method accepts space as a separator between date and time.
     *
     * @param  text  the text to parse.
     * @return the date (never null).
     * @throws ParseException if the parsing failed.
     */
    @Override
    public Date parse(final String text) throws ParseException {
        try {
            return toDate(format.parse(toISO(text, 0, text.length())));
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
