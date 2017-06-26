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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Temporal;
import org.apache.sis.internal.jdk8.DateTimeException;


/**
 * A date format used for parsing dates in the {@code "yyyy-MM-dd'T'HH:mm:ss.SSSX"} pattern, but in which
 * the time is optional. For this class, "Standard" is interpreted as "close to ISO 19162 requirements",
 * which is not necessarily identical to other ISO standards.
 *
 * <p>This class is implemented in two different way depending on the Apache SIS branch:</p>
 * <ul>
 *   <li>Branches for JDK8 and more use {@link java.time.format.DateTimeFormatter}.</li>
 *   <li>Branches for older JDKs use {@link java.text.SimpleDateFormat} together with some hacks
 *       for allowing some fields to be optional (for example adding ":00" is seconds are missing).</li>
 * </ul>
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
public final class StandardDateFormat extends SimpleDateFormat {
    /**
     * For cross-version compatibility.
     * This number must be different between the JDK8 branch and pre-JDK8 branches.
     */
    private static final long serialVersionUID = 1552761359761440473L;

    /**
     * The {@value} timezone ID.
     */
    public static final String UTC = "UTC";

    /**
     * Short version of {@link #PATTERN}, to be used when formatting temporal extents
     * if the duration is greater than some threshold (typically one day). This pattern must
     * be a prefix of {@link #PATTERN}, since we will use that condition for deciding
     * if this pattern is really shorter (the user could have created his own date format
     * with a different pattern).
     */
    public static final String SHORT_PATTERN = "yyyy-MM-dd";

    /**
     * The pattern of time. We use 3 fraction digits for the seconds because {@code SimpleDateFormat} parses the
     * milliseconds as an integer instead than as fraction digits. For example with 1 fraction digits, "00:00:01.4"
     * is parsed as 1.004 seconds instead of 1.4. While surprising, this is conform to the {@code SimpleDateFormat}
     * specification. Note that this is different than {@link java.time.LocalDateTime} which parse those numbers as
     * fraction digits.
     */
    public static final String TIME_PATTERN = "HH:mm:ss.SSSX";

    /**
     * Number of fraction digits in {@link #TIME_PATTERN}.
     */
    private static final int NUM_FRACTION_DIGITS = 3;

    /**
     * The pattern of dates.
     */
    public static final String PATTERN = SHORT_PATTERN + "'T'" + TIME_PATTERN;

    /**
     * Number of nanoseconds in one millisecond.
     */
    public static final long NANOS_PER_MILLISECOND = 1000000;

    /**
     * Number of nanoseconds in one second.
     */
    public static final long NANOS_PER_SECOND = 1000000000;

    /**
     * The length of a day in number of milliseconds.
     */
    public static final int MILLISECONDS_PER_DAY = 24*60*60*1000;

    /**
     * Converts the given temporal object into a date.
     *
     * @param  temporal  the temporal object to convert, or {@code null}.
     * @return the legacy date for the given temporal object, or {@code null} if the argument was null.
     * @throws DateTimeException if a value for the field cannot be obtained.
     * @throws ArithmeticException if the number of milliseconds is too large.
     */
    public static Date toDate(final Temporal temporal) {
        if (temporal == null) {
            return null;
        }
        return new Date(temporal.millis);
    }

    /**
     * {@code true} if the user has invoked {@link #applyPattern(String)} or {@link #applyLocalizedPattern(String)}.
     */
    private boolean isUserSpecifiedPattern;

    /**
     * Creates a new format for a default locale in the UTC timezone.
     */
    public StandardDateFormat() {
        this(Locale.CANADA);        // Canada locale symbols are close to the ISO ones.
    }

    /**
     * Creates a new format for the given locale in the UTC timezone.
     *
     * @param locale  the locale of the format to create.
     */
    public StandardDateFormat(final Locale locale) {
        this(locale, TimeZone.getTimeZone(UTC));
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param locale  the locale of the format to create.
     * @param zone    the timezone.
     */
    public StandardDateFormat(final Locale locale, final TimeZone zone) {
        super(PATTERN, locale);
        calendar = new ISOCalendar(locale, zone);
    }

    /**
     * Sets a user-specified pattern.
     *
     * @param pattern the user-specified pattern.
     */
    @Override
    public void applyPattern(final String pattern) {
        super.applyPattern(pattern);
        isUserSpecifiedPattern = true;
    }

    /**
     * Sets a user-specified pattern.
     *
     * @param pattern the user-specified pattern.
     */
    @Override
    public void applyLocalizedPattern(final String pattern) {
        super.applyLocalizedPattern(pattern);
        isUserSpecifiedPattern = true;
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
        if (!isUserSpecifiedPattern && (date.getTime() % MILLISECONDS_PER_DAY) == 0 && UTC.equals(getTimeZone().getID())) {
            try {
                super.applyPattern(SHORT_PATTERN);
                return super.format(date, toAppendTo, pos);
            } finally {
                super.applyPattern(PATTERN);
            }
        }
        return super.format(date, toAppendTo, pos);
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
        if (isUserSpecifiedPattern) {
            return super.parse(text, position);
        }
        final int fromIndex = position.getIndex();
        final String modified = dateToISO(text, fromIndex, false);
        position.setIndex(0);
        final Date date = super.parse(modified, position);
        position.setIndex     (adjustIndex(text, modified, fromIndex, position.getIndex()));
        position.setErrorIndex(adjustIndex(text, modified, fromIndex, position.getErrorIndex()));
        return date;
    }

    /**
     * Modifies if needed a given input string in order to make it compliant with JDK7 implementation of
     * {@code SimpleDateFormat}. That implementation expects the exact same number of fraction digits in
     * the second fields than specified by the {@code "ss.SSS"} part of the pattern. This method adds or
     * removes fraction digits as needed, and adds a {@code "Z"} suffix if the string has no timezone.
     *
     * <p>The string returned by this method starts at {@code fromIndex} and stop after an arbitrary amount
     * of characters (may be more characters than actually needed for parsing the date).</p>
     *
     * @param  text       the text to adapt.
     * @param  fromIndex  index in {@code text} where to start the adaptation.
     * @param  isTime     {@code true} if parsing only a time, or {@code false} if parsing a day and a time.
     * @return the modified input string, with second fraction digits added or removed.
     */
    @SuppressWarnings("fallthrough")
    public static String dateToISO(final CharSequence text, int fromIndex, boolean isTime) {
        if (text == null) {
            return null;
        }
        final StringBuilder modified = new StringBuilder(30);
        /*
         * Copy characters from the given text to the buffer as long as it seems to be part of a date.
         * We do not perform a strict check, so we may copy more characters than needed; it will be the
         * DateFormat' job to tell to the caller where the date ends.
         */
        int numDigits = 0;
        int missingTimeFields = 2;
        boolean isFraction = false;
        boolean isTimeZone = false;
copy:   while (fromIndex < text.length()) {
            char c = text.charAt(fromIndex++);
            if (c >= '0' && c <= '9') {
                if (++numDigits > NUM_FRACTION_DIGITS && isFraction) {
                    continue;       // Ignore extraneous fraction digits.
                }
            } else {
                switch (c) {
                    default: {
                        break copy;
                    }
                    case 'T': {
                        if (isTime) break copy;
                        isTime = true;
                        break;
                    }
                    case ':': {
                        if (!isTime | isFraction) break copy;
                        missingTimeFields--;
                        break;
                    }
                    case '.': {
                        if (!isTime | isFraction | isTimeZone) break copy;
                        isFraction = true;
                        break;
                    }
                    case '-': {
                        if (!isTime) break;      // Separator between year-month-day: nothing to do.
                        // Otherwise timezone offset: same work than for the '+' case (fallthrough).
                    }
                    case '+':
                    case 'Z': {
                        if (!isTime | isTimeZone) break copy;
                        if (!isFraction) {
                            while (--missingTimeFields >= 0) {
                                modified.append(":00");
                            }
                            modified.append('.');
                            numDigits = 0;
                        }
                        while (numDigits < NUM_FRACTION_DIGITS) {
                            modified.append('0');
                            numDigits++;
                        }
                        isFraction = false;
                        isTimeZone = true;
                        break;
                    }
                }
                if (numDigits == 1) {
                    modified.insert(modified.length() - 1, '0');
                }
                numDigits = 0;
            }
            modified.append(c);
        }
        /*
         * Check for missing time fields and time zone. For example if the given date is
         * "2005-09-22T00:00", then this method will completes it as "2005-09-22T00:00:00".
         * In addition, a 'Z' suffix will be appended if needed.
         */
        if (numDigits == 1) {
            modified.insert(modified.length() - 1, '0');
        }
        if (!isTimeZone) {
            if (!isTime) {
                modified.append("T00");
            }
            if (!isFraction) {
                while (--missingTimeFields >= 0) {
                    modified.append(":00");
                }
                modified.append('.');
                numDigits = 0;
            }
            while (numDigits < NUM_FRACTION_DIGITS) {
                modified.append('0');
                numDigits++;
            }
            modified.append('Z');
        }
        return modified.toString();
    }

    /**
     * Maps an index in the modified string to the index in the original string.
     *
     * @param  text      the original text specified by the user.
     * @param  modified  the modified text that has been parsed.
     * @param  offset    index of the first {@code text} character copied in {@code modified}.
     * @param  index     the index in the modified string.
     * @return the corresponding index in the original text.
     */
    static int adjustIndex(final String text, final String modified, int offset, final int index) {
        if (index < 0) {
            return index;
        }
        if (!text.isEmpty()) {
            for (int i=0; i<index; i++) {
                if (modified.charAt(i) == text.charAt(offset)) {
                    if (++offset >= text.length()) break;
                }
            }
        }
        return offset;
    }
}
