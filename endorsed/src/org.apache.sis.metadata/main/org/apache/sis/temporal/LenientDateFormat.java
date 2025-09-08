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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.Year;
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
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.Constants;


/**
 * A date format used for parsing dates in the {@code "yyyy-MM-dd'T'HH:mm:ss.SSSX"} pattern, but in which
 * the time is optional. For this class, "Standard" is interpreted as "close to ISO 19162 requirements",
 * which is not necessarily identical to other ISO standards.
 *
 * <p>The main usage for this class is Well Known Text (WKT) parsing and formatting.
 * ISO 19162 uses ISO 8601:2004 for the dates. Any precision is allowed: the date could have only the year,
 * or only the year and month, <i>etc</i>. The clock part is optional and also have optional fields: can be
 * only hours, or only hours and minutes, <i>etc</i>. ISO 19162 said that the timezone is restricted to UTC
 * but nevertheless allows to specify a timezone.</p>
 *
 * <h2>Serialization</h2>
 * Despite inheriting the {@link java.io.Serializable} interface, this class is actually not serializable
 * because the {@link #format} field is not serializable (a constraint of {@link java.time.format}).
 * All usages in Apache SIS should be either in transient fields or in non-serializable classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")     // Not intended to be serialized.
public final class LenientDateFormat extends DateFormat {
    /**
     * Midnight (00:00) UTC.
     */
    private static final OffsetTime MIDNIGHT = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * The thread-safe instance to use for reading and formatting dates.
     * Only the year is mandatory, all other fields are optional at parsing time.
     * However, all fields are written, including milliseconds at formatting time,
     * unless that field is not available at all (which is not he same as available
     * with value zero).
     *
     * @see #parseInstantUTC(CharSequence, int, int)
     */
    public static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
            .parseLenient()                    // For allowing fields with one digit instead of two.
            .parseCaseInsensitive()            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.NORMAL)   // Proleptic year (use negative number if needed).
            .optionalStart().appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR,    2)
            .optionalStart().appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH,     2)
            .optionalStart().appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY,      2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR,   2)
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                                               .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .optionalEnd().optionalEnd().optionalEnd()    // Move back to the optional block of HOUR_OF_DAY.
            .optionalStart().optionalStart().appendLiteral(' ').optionalEnd().appendZoneOrOffsetId()
            .toFormatter(Locale.ROOT);

    /**
     * The kinds of objects to get from calls to {@link #parseBest(CharSequence)}, in preference order.
     * The time is converted to UTC timezone if possible.
     *
     * <h4>Alternative</h4>
     * If we want to preserve the timezone instead of converting to UTC, we could try replacing
     * {@code Instant::from} by {@code ZonedDateTime::from, OffsetDateTime::from}.
     *
     * @see #parseInstantUTC(CharSequence, int, int)
     */
    private static TemporalQuery<?>[] QUERIES = {
        Instant::from, LocalDateTime::from, LocalDate::from, YearMonth::from, Year::from
    };

    /**
     * Parses the given date and/or time, which may have an optional timezone. This method applies heuristic rules
     * for choosing if the object should be returned as a local date, or a date and time with timezone, <i>etc</i>.
     * The full date format is of the form "1970-01-01T00:00:00.000Z", but this method also accepts spaces in place
     * of 'T' as in "1970-01-01 00:00:00".
     *
     * @param  text  the character string to parse, or {@code null}.
     * @return a temporal object for the given text, or {@code null} if the given text was null.
     * @throws DateTimeParseException if the text cannot be parsed as a date.
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
     * @throws DateTimeParseException if the text cannot be parsed as a date.
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
     * @throws DateTimeParseException if the text cannot be parsed as a date.
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
     * The {@code java.time} parser and formatter. This is usually the {@link #FORMAT} instance
     * unless a different locale or timezone has been specified.
     */
    private DateTimeFormatter format;

    /**
     * The formatter without timezone. This is usually the same object as {@link #format},
     * unless a timezone has been set in which case this field keep the original formatter.
     * This is used for formatting local dates without the formatter timezone.
     */
    private final DateTimeFormatter formatWithoutZone;

    /**
     * Creates a new format for a default locale in the UTC timezone.
     */
    public LenientDateFormat() {
        formatWithoutZone = format = FORMAT;
    }

    /**
     * Creates a new format for the given locale in the UTC timezone.
     *
     * @param locale  the locale of the format to create.
     */
    public LenientDateFormat(final Locale locale) {
        // Same instance as FORMAT if the locales are equal.
        formatWithoutZone = format = FORMAT.withLocale(locale);
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param locale  the locale of the format to create.
     * @param zone    the timezone.
     */
    public LenientDateFormat(final Locale locale, final TimeZone zone) {
        this(locale);
        if (!Constants.UTC.equals(zone.getID())) {
            setTimeZone(zone);
        }
    }

    /**
     * Returns the calendar, creating it when first needed. This {@code LenientDateFormat} class does not use the
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
     * Returns the number format, creating it when first needed. This {@code LenientDateFormat} class does not use the
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
     * Returns the zone, or UTC if unspecified.
     */
    private ZoneId getZone() {
        final ZoneId zone = format.getZone();
        return (zone != null) ? zone : ZoneOffset.UTC;
    }

    /**
     * Returns the timezone used for formatting instants.
     *
     * @return the timezone.
     */
    @Override
    public final TimeZone getTimeZone() {
        return TimeZone.getTimeZone(getZone());
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
     * Formats the given date. If hours, minutes, seconds and milliseconds are zero in the timezone of this formatter,
     * then this method omits the clock part. The timezone is always omitted (ISO 19162 does not include timezone in
     * WKT elements because all dates are required to be in UTC).
     *
     * @param  date        the date to format.
     * @param  toAppendTo  where to format the date.
     * @param  pos         ignored.
     * @return the given buffer, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer toAppendTo, final FieldPosition pos) {
        final LocalDateTime dt = LocalDateTime.ofInstant(date.toInstant(), getZone());
        TemporalAccessor value = dt;
        if (dt.getHour() == 0 && dt.getMinute() == 0 && dt.getSecond() == 0 && dt.getNano() == 0) {
            value = dt.toLocalDate();
        }
        formatWithoutZone.formatTo(value, toAppendTo);
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
            return TemporalDate.toDate(TemporalDate.toInstant(format.parse(text, position), getZone()));
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
            return TemporalDate.toDate(TemporalDate.toInstant(format.parse(toISO(text, 0, text.length())), getZone()));
        } catch (RuntimeException e) {
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
        return (obj instanceof LenientDateFormat) && format.equals(((LenientDateFormat) obj).format);
    }
}
