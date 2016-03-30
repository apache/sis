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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;


/**
 * A date format used for parsing date in the {@code "yyyy-MM-dd'T'HH:mm:ss.SX"} pattern, but in which
 * the time is optional. The "Apache SIS for JDK8" branch can use the {@link java.time.format} package,
 * while other branches use {@link java.text.SimpleDateFormat}.
 *
 * <p>External users should use nothing else than the parsing and formating methods. The methods for
 * configuring the {@code DateFormat} may not be available between different SIS branches.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class StandardDateFormat extends SimpleDateFormat {
    /**
     * For compatibility between versions of a same branch. The Apache SIS form JDK8 and for JDK7 branches
     * may have incompatible classes, and consequently different serial version UID.
     */
    private static final long serialVersionUID = 1552761359761440473L;

    /**
     * The pattern of dates.
     *
     * The JDK7 branch has a 'X' pattern at the end of this format. But JDK6 does not support that pattern.
     * As a workaround, code using this pattern will append a hard-coded {@code "'Z'"} if the timezone is
     * known to be UTC.
     */
    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.S";

    /**
     * Short version of {@link #PATTERN}, to be used when formatting temporal extents
     * if the duration is greater than some threshold (typically one day). This pattern must
     * be a prefix of {@link #PATTERN}, since we will use that condition for deciding
     * if this pattern is really shorter (the user could have created his own date format
     * with a different pattern).
     */
    public static final String SHORT_PATTERN = "yyyy-MM-dd";

    /**
     * Creates a new format for a default locale in the UTC timezone.
     */
    public StandardDateFormat() {
        this(Locale.CANADA);        // Canada locale symbols are close to the ISO ones.
    }

    /**
     * Creates a new format for the given locale in the UTC timezone.
     *
     * @param locale The locale of the format to create.
     */
    public StandardDateFormat(final Locale locale) {
        this(locale, TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param locale The locale of the format to create.
     * @param timezone The timezone.
     */
    public StandardDateFormat(final Locale locale, final TimeZone timezone) {
        super("UTC".equals(timezone.getID()) ? PATTERN + "'Z'" : PATTERN, locale);
        setTimeZone(timezone);
    }

    /**
     * Parses the given text starting at the given position.
     *
     * @param  text The text to parse.
     * @param  position Position where to start the parsing.
     * @return The date, or {@code null} if we failed to parse it.
     */
    @Override
    public Date parse(final String text, final ParsePosition position) {
        Date date = super.parse(text, position);
        if (date == null) {
            /*
             * The "yyyy-MM-dd'T'HH:mm:ss.SX" pattern may fail if the user did not specified the time part.
             * So if we fail to parse using the full pattern, try again with only the "yyyy-MM-dd" pattern.
             */
            final String pattern = toPattern();
            if (pattern.startsWith(SHORT_PATTERN)) {
                final int errorIndex = position.getErrorIndex();
                position.setErrorIndex(-1);
                applyPattern(SHORT_PATTERN);
                try {
                    date = parse(text, position);
                } finally {
                    applyPattern(pattern);
                }
                if (date == null) {
                    position.setErrorIndex(errorIndex); // Reset original error index.
                }
            }
        } else {
            /*
             * Following is a workaround specific to the JDK6 branch. Since JDK6 does not understand the 'Z' suffix,
             * we handle it in this method. The Apache SIS branch for JDK7 does not need this hack since JDK7 supports
             * parsing the 'Z' suffix.
             */
            final int p = position.getIndex();
            if (p < text.length() && text.charAt(p) == 'Z') {
                position.setIndex(p + 1);
                final Calendar cal = calendar;
                final int year   = cal.get(Calendar.YEAR);
                final int month  = cal.get(Calendar.MONTH);
                final int day    = cal.get(Calendar.DAY_OF_MONTH);
                final int hour   = cal.get(Calendar.HOUR_OF_DAY);
                final int minute = cal.get(Calendar.MINUTE);
                final int second = cal.get(Calendar.SECOND);
                final int millis = cal.get(Calendar.MILLISECOND);
                final TimeZone timezone = cal.getTimeZone();
                final long time;
                try {
                    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
                    cal.set(year, month, day, hour, minute, second);
                    cal.set(Calendar.MILLISECOND, millis);
                    time = cal.getTimeInMillis();
                } finally {
                    cal.setTimeZone(timezone);
                }
                date.setTime(time);
            }
        }
        return date;
    }
}
