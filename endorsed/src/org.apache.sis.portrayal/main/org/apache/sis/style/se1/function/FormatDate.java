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
package org.apache.sis.style.se1.function;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.opengis.filter.Expression;


/**
 * Formatting of dates and times for making them human readable, typically in labels.
 * The date value can be any {@link java.time} object, a {@link Date}, a {@link Calendar},
 * a GML time instant ({@link org.opengis.temporal.Instant}) or a string in ISO 8601 format,
 * including the XML schema {@code gYearMonth}, {@code gMonthDay}, {@code gDay}, {@code gMonth}
 * and {@code gYear} types. The pattern is built with the following characters:
 *
 * <ul>
 *   <li>{@code YYYY}: four digits year, {@code YY}: two digits year (without century and millennium),</li>
 *   <li>{@code MM}: two digits month, {@code M}: month with leading zero omitted,</li>
 *   <li>{@code MMM}: month as a three letters acronym ({@code "FEB"}),
 *       {@code MMMMM}: full month name ({@code "February"}). A ISO 639 two-letters language code can be
 *       appended for language-dependent variants ({@code MMMde} gives {@code "DEZ"} instead of {@code "DEC"},
 *       {@code MMMMMde} gives {@code "Februar"}). The default language is English.</li>
 *   <li>{@code DD}: two digits day, {@code D}: day with leading zero omitted,</li>
 *   <li>{@code hh}, {@code mm}, {@code ss}: two digits hour, minute and second,
 *       {@code h}, {@code m}, {@code s}: same with leading zero omitted.
 *       Hours are in 12-hour clock if the pattern contains the am/pm marker, or in 24-hour clock otherwise.</li>
 *   <li>{@code a}: am/pm marker, {@code z}: time zone (if present).</li>
 *   <li>{@code \}: quote the next character, which appears literally in the result.</li>
 *   <li>All other characters, for example {@code .}, {@code /}, {@code :} and {@code -}, appear literally.</li>
 * </ul>
 *
 * If the pattern is null or empty, the standard ISO 8601 representation {@code YYYY-MM-DDThh:mm:ss} is used,
 * restricted to the fields available in the date value (for example {@code YYYY-MM} for a {@code gYearMonth}).
 * Fields which are not available in the date value are formatted as zero for time fields,
 * and omitted for date fields. Values which are missing or not convertible to dates give null.
 *
 * <p>This function is available by the name {@code "FormatDate"} in Apache SIS filter factories,
 * with the date value and the pattern as parameters.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class FormatDate<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4706914376416227215L;

    /**
     * The name of this function.
     */
    static final String NAME = "FormatDate";

    /**
     * Parsers of ISO 8601 strings, tried in order: {@code dateTime}, {@code date}, {@code time},
     * {@code gYearMonth}, {@code gMonthDay}, {@code gDay}, {@code gMonth} and {@code gYear},
     * all with an optional time zone.
     */
    private static final List<DateTimeFormatter> PARSERS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ISO_TIME,
            DateTimeFormatter.ofPattern("uuuu-MM[XXX]"),
            DateTimeFormatter.ofPattern("--MM-dd[XXX]"),
            DateTimeFormatter.ofPattern("---dd[XXX]"),
            DateTimeFormatter.ofPattern("--MM[XXX]"),
            DateTimeFormatter.ofPattern("uuuu[XXX]"));

    /**
     * Formatter of short time zone names, such as "PST".
     */
    private static final DateTimeFormatter ZONE_NAME = DateTimeFormatter.ofPattern("z", Locale.ENGLISH);

    /**
     * The date to format.
     */
    private final Expression<R,?> dateValue;

    /**
     * The pattern, or {@code null} for ISO 8601.
     */
    private final Expression<R,?> pattern;

    /**
     * Creates a new date formatting function.
     *
     * @param  dateValue  the date to format.
     * @param  pattern    the pattern, or {@code null} for the standard ISO 8601 representation.
     */
    public FormatDate(final Expression<R,?> dateValue, final Expression<R,?> pattern) {
        super(NAME, String.class);
        this.dateValue = Objects.requireNonNull(dateValue, "dateValue");
        this.pattern   = pattern;
    }

    /**
     * Returns the date to format.
     *
     * @return the date value.
     */
    public Expression<R,?> getDateValue() {
        return dateValue;
    }

    /**
     * Returns the pattern.
     *
     * @return the pattern, or empty for the standard ISO 8601 representation.
     */
    public Optional<Expression<R,?>> getPattern() {
        return Optional.ofNullable(pattern);
    }

    /**
     * Returns the parameters of this function: the date value followed by the pattern if specified.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        return (pattern != null) ? List.of(dateValue, pattern) : List.of(dateValue);
    }

    /**
     * Formats the date value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the formatted date, or {@code null} if the value is missing or not a date.
     */
    @Override
    public String apply(final R input) {
        final TemporalAccessor date = toTemporal(dateValue.apply(input));
        if (date == null) {
            return null;
        }
        final String p = (pattern != null) ? string(pattern.apply(input)) : null;
        return format(date, (p != null && !p.isEmpty()) ? p : defaultPattern(date), false);
    }

    /**
     * Returns the ISO 8601 pattern for the fields available in the given date.
     * This is {@code YYYY-MM-DDThh:mm:ss} for a complete date and time, and the
     * XML schema representations for partial dates such as {@code --MM-DD} for {@code gMonthDay}.
     */
    private static String defaultPattern(final TemporalAccessor date) {
        final boolean month = date.isSupported(ChronoField.MONTH_OF_YEAR);
        String p = date.isSupported(ChronoField.YEAR) ? "YYYY" : "-";
        if (month) p += "-MM";
        if (date.isSupported(ChronoField.DAY_OF_MONTH)) p += month ? "-DD" : "--DD";
        if (p.equals("-")) p = "";
        if (date.isSupported(ChronoField.HOUR_OF_DAY)) p += p.isEmpty() ? "hh:mm:ss" : "Thh:mm:ss";
        return p;
    }

    /**
     * Converts the given value to a temporal object.
     *
     * @return the temporal object, or {@code null} if the value is not convertible.
     */
    static TemporalAccessor toTemporal(Object value) {
        if (value instanceof org.opengis.temporal.Instant) {
            value = ((org.opengis.temporal.Instant) value).getPosition();
        }
        if (value instanceof Instant) {
            return ((Instant) value).atZone(ZoneOffset.UTC);
        }
        if (value instanceof TemporalAccessor) {
            return (TemporalAccessor) value;
        }
        if (value instanceof Calendar) {
            final Calendar c = (Calendar) value;
            return c.toInstant().atZone(c.getTimeZone().toZoneId());
        }
        if (value instanceof Date) {
            return Instant.ofEpochMilli(((Date) value).getTime()).atZone(ZoneOffset.UTC);
        }
        if (value instanceof CharSequence) {
            final String text = value.toString().trim();
            for (DateTimeFormatter parser : PARSERS) {
                try {
                    return parser.parse(text);
                } catch (DateTimeParseException e) {
                    // Try the next parser.
                }
            }
        }
        return null;
    }

    /**
     * Formats the given date with the given Symbology Encoding pattern.
     * Hours are formatted in 12-hour clock if the pattern contains the am/pm marker.
     * Since this is known only after parsing the pattern, the formatting is redone if needed.
     *
     * @param  twelveHours  whether to format hours in 12-hour clock.
     */
    private static String format(final TemporalAccessor date, final String pattern, final boolean twelveHours) {
        final var buffer = new StringBuilder();
        boolean hasAmPm = false;
        final int length = pattern.length();
        int i = 0;
        while (i < length) {
            final char c = pattern.charAt(i);
            if (c == '\\') {
                if (i + 1 < length) buffer.append(pattern.charAt(i + 1));
                i += 2;
                continue;
            }
            int n = 1;
            while (i + n < length && pattern.charAt(i + n) == c) n++;
            switch (c) {
                case 'Y': {
                    if (date.isSupported(ChronoField.YEAR)) {
                        final int year = date.get(ChronoField.YEAR);
                        if (n == 2) pad(buffer, Math.floorMod(year, 100), 2);
                        else pad(buffer, year, n);
                    }
                    break;
                }
                case 'M': {
                    if (date.isSupported(ChronoField.MONTH_OF_YEAR)) {
                        final Month month = Month.of(date.get(ChronoField.MONTH_OF_YEAR));
                        if (n >= 3) {
                            Locale locale = Locale.ENGLISH;
                            if (i + n + 1 < length && isLowerLetter(pattern.charAt(i + n)) && isLowerLetter(pattern.charAt(i + n + 1))) {
                                locale = Locale.forLanguageTag(pattern.substring(i + n, i + n + 2));
                                i += 2;     // Skip the language code.
                            }
                            if (n >= 5) {
                                buffer.append(month.getDisplayName(TextStyle.FULL_STANDALONE, locale));
                            } else {
                                String name = month.getDisplayName(TextStyle.FULL_STANDALONE, locale);
                                name = name.substring(0, Math.min(3, name.length()));
                                buffer.append(name.toUpperCase(locale));
                            }
                        } else {
                            pad(buffer, month.getValue(), n);
                        }
                    }
                    break;
                }
                case 'D': {
                    if (date.isSupported(ChronoField.DAY_OF_MONTH)) {
                        pad(buffer, date.get(ChronoField.DAY_OF_MONTH), n);
                    }
                    break;
                }
                case 'h': pad(buffer, field(date, twelveHours ? ChronoField.CLOCK_HOUR_OF_AMPM : ChronoField.HOUR_OF_DAY), n); break;
                case 'm': pad(buffer, field(date, ChronoField.MINUTE_OF_HOUR), n); break;
                case 's': pad(buffer, field(date, ChronoField.SECOND_OF_MINUTE), n); break;
                case 'a': {
                    hasAmPm = true;
                    final int ampm = date.isSupported(ChronoField.AMPM_OF_DAY) ? date.get(ChronoField.AMPM_OF_DAY) : 0;
                    for (int k = 0; k < n; k++) buffer.append(ampm == 0 ? "AM" : "PM");
                    break;
                }
                case 'z': {
                    final ZoneId zone = date.query(TemporalQueries.zone());
                    if (zone != null) {
                        if (zone instanceof ZoneOffset) {
                            buffer.append(zone.equals(ZoneOffset.UTC) ? "GMT" : "GMT" + zone.getId());
                        } else try {
                            // Standard or daylight saving time name for that date, e.g. "PST" or "PDT".
                            buffer.append(ZONE_NAME.format(date));
                        } catch (RuntimeException e) {
                            buffer.append(zone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                        }
                    }
                    break;
                }
                default: {
                    for (int k = 0; k < n; k++) buffer.append(c);
                    break;
                }
            }
            i += n;
        }
        return (hasAmPm && !twelveHours) ? format(date, pattern, true) : buffer.toString();
    }

    /**
     * Returns the value of the given time field, or 0 if the date does not have that field.
     */
    private static int field(final TemporalAccessor date, final ChronoField field) {
        return date.isSupported(field) ? date.get(field) : (field == ChronoField.CLOCK_HOUR_OF_AMPM ? 12 : 0);
    }

    /**
     * Returns whether the given character is a lower case ASCII letter.
     */
    private static boolean isLowerLetter(final char c) {
        return c >= 'a' && c <= 'z';
    }

    /**
     * Appends the given value with leading zeros up to the given number of digits.
     */
    private static void pad(final StringBuilder buffer, final int value, final int digits) {
        final String text = Integer.toString(Math.abs(value));
        if (value < 0) buffer.append('-');
        for (int k = text.length(); k < digits; k++) buffer.append('0');
        buffer.append(text);
    }

    @Override
    Object[] properties() {
        return new Object[] {dateValue, pattern};
    }
}
