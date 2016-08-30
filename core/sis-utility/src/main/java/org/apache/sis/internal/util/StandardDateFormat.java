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


/**
 * A date format used for parsing date in the {@code "yyyy-MM-dd'T'HH:mm:ss.SSSX"} pattern, but in which
 * the time is optional. The "Apache SIS for JDK8" branch can use the {@link java.time.format} package,
 * while other branches use {@link java.text.SimpleDateFormat}.
 *
 * <p>External users should use nothing else than the parsing and formating methods. The methods for
 * configuring the {@code DateFormat} may not be available between different SIS branches.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.8
 * @module
 */
public final class StandardDateFormat extends SimpleDateFormat {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1552761359761440473L;

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
     * {@code false} if this {@code StandardDateFormat} object is parsing only the days, without time.
     */
    private boolean hasTime;

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
        this(locale, TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param locale    the locale of the format to create.
     * @param timezone  the timezone.
     */
    public StandardDateFormat(final Locale locale, final TimeZone timezone) {
        super(PATTERN, locale);
        super.setTimeZone(timezone);
        hasTime = true;
    }

    /**
     * Formats the given date.
     *
     * @param  date    the date to format.
     * @param  buffer  where to format the date.
     * @param  pos     where to store information about a date field.
     * @return the given buffer, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer buffer, final FieldPosition pos) {
        if (!hasTime) {
            applyPattern(PATTERN);
            hasTime = true;
        }
        return super.format(date, buffer, pos);
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
        final Fix fix = Fix.apply(text, position.getIndex(), 0);
        if (fix == null) {
            if (hasTime) {
                applyPattern(SHORT_PATTERN);
                hasTime = false;
            }
            return super.parse(text, position);
        }
        if (!hasTime) {
            applyPattern(PATTERN);
            hasTime = true;
        }
        final Date date = super.parse(fix.text, position);
        position.setIndex     (fix.adjustIndex(position.getIndex()));
        position.setErrorIndex(fix.adjustIndex(position.getErrorIndex()));
        return date;
    }

    /**
     * Modifies if needed a given input string in order to make it compliant with JDK7 implementation of
     * {@code SimpleDateFormat}. That implementation expects the exact some number of fraction digits in
     * the second fields than specified by the {@code "ss.SSS"} part of the pattern. This method adds or
     * removes fraction digits as needed.
     *
     * @param  text  the text to adapt.
     * @param  time  {@code true} if parsing only a time, or {@code false} if parsing a day and a time.
     * @return the modified input string, with second fraction digits added or removed.
     */
    public static String fix(final String text, final boolean time) {
        final Fix fix = Fix.apply(text, 0, time ? 1 : 0);
        return (fix != null) ? fix.text : text;
    }

    /**
     * Implementation of {@link StandardDateFormat#fix(String)} method together with additional information.
     */
    static final class Fix {
        /**
         * The modified input string, with second fraction digits added or removed.
         */
        public final String text;

        /**
         * Index of the first character added or removed, or {@code input.length()} if none.
         */
        private final int lower;

        /**
         * Number of characters added (positive number) or removed (negative number).
         */
        final int change;

        /**
         * Wraps information about an input string that has been made parsable.
         */
        private Fix(final String text, final int lower, final int change) {
            this.text   = text;
            this.lower  = lower;
            this.change = change;
        }

        /**
         * Performs various adjustments for making the given text compliant with the format expected by
         * a {@link SimpleDateFormat} using the {@value #PATTERN} pattern.
         *
         * @param  text       the text to adapt.
         * @param  s          index in {@code text} where to start the parsing.
         * @param  timeField  0 if parsing starts on days, 1 if starting on hours field,
         *                    2 if starting on minutes field or 3 if starting on seconds field.
         * @return information about the input string made parsable,
         *         or {@code null} if the given text does not contain a time field.
         */
        static Fix apply(final String text, int s, int timeField) {
            final int length = text.length();
search:     while (s < length) {
                char c = text.charAt(s);
                if (c < '0' || c > '9') {
                    switch (c) {
                        default: {
                            break search;
                        }
                        case '-': {
                            if (timeField != 0) break search;
                            break;
                        }
                        case 'T': {
                            if (timeField != 0) break search;
                            timeField = 1;
                            break;
                        }
                        case ':': {
                            if (timeField == 0 || ++timeField > 3) break search;
                            break;
                        }
                        case '.': {
                            if (timeField != 3) break search;
                            /*
                             * If the user specified too few or too many fraction digits, add or truncate.
                             * If the number of digits is already the expected ones (which should be rare),
                             * nevertheless create a new Fix instance for notifying the parse method that
                             * the text contains a time.
                             */
                            final int start = ++s;
                            while (s < length && (c = text.charAt(s)) >= '0' && c <= '9') s++;
                            final int change = NUM_FRACTION_DIGITS - (s - start);
                            if (change == 0) {
                                return new Fix(text, length, 0);                    // See above comment.
                            }
                            final StringBuilder buffer = new StringBuilder(text);
                            if (change >= 0) {
                                for (int i = change; --i >= 0;) {
                                    buffer.insert(s, '0');
                                }
                            } else {
                                final int upper = s;
                                s = start + NUM_FRACTION_DIGITS;
                                buffer.delete(s, upper);
                            }
                            return new Fix(buffer.toString(), s, change);
                        }
                    }
                }
                s++;
            }
            /*
             * If the user did not specified any fraction digits, add them.
             * (NUM_FRACTION_DIGITS + 1) shall be the length of the inserted string.
             */
            final String time;
            switch (timeField) {
                default: return null;
                case 1:  time = ":00:00.000"; break;
                case 2:  time =    ":00.000"; break;
                case 3:  time =       ".000"; break;
            }
            return new Fix(new StringBuilder(text).insert(s, time).toString(), s, time.length());
        }

        /**
         * Map an index in the modified string to the index in the original string.
         *
         * @param  index  the index in the modified string.
         * @return the corresponding index in the original string.
         */
        int adjustIndex(int index) {
            if (index >= lower) {
                index = Math.max(lower, index - change);
            }
            return index;
        }
    }
}
