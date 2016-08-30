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
package org.apache.sis.internal.jdk8;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.sis.internal.util.StandardDateFormat;


/**
 * Placeholder for the {@link java.time.LocalDate} class.
 */
public final class LocalDate extends Temporal {
    /**
     * The calendar to use for computing dates.
     * Use of this calendar must be synchronized.
     */
    private static final GregorianCalendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);

    /**
     * A shared object for parsing and formatting dates.
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat(StandardDateFormat.SHORT_PATTERN, Locale.US);
    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a new date.
     *
     * @param millis  number of milliseconds since January 1st, 1970 midnight UTC.
     */
    private LocalDate(final long millis) {
        super(millis);
    }

    /**
     * Creates a date.
     *
     * @param  year        the year.
     * @param  month       the month-of-year from 1 to 12.
     * @param  dayOfMonth  the day-of-month from 1 to 31
     * @return the local date.
     */
    public static LocalDate of(int year, int month, int dayOfMonth) {
        final long millis;
        synchronized (CALENDAR) {
            CALENDAR.clear();
            CALENDAR.set(year, month - 1, dayOfMonth);
            millis = CALENDAR.getTimeInMillis();
        }
        return new LocalDate(millis);
    }

    /**
     * Returns the combination of this date with the given offset time.
     *
     * @param  time  the time to combine with.
     * @return combination of this date with the given time.
     */
    public OffsetDateTime atTime(final OffsetTime time) {
        return new OffsetDateTime(millis + time.millis);
    }

    /**
     * Parses a text string like {@code 2007-12-03}.
     *
     * @param  text  the text to parse.
     * @return the parsed date.
     * @throws DateTimeParseException if the text cannot be parsed.
     */
    public static LocalDate parse(final String text) {
        final Date date;
        try {
            synchronized (FORMAT) {
                date = FORMAT.parse(text);
            }
        } catch (ParseException e) {
            throw new DateTimeParseException(e.getLocalizedMessage());
        }
        return new LocalDate(date.getTime());
    }

    /**
     * Returns the number of days elapsed since January 1st, 1970.
     *
     * @return number of days elapsed since January 1st, 1970.
     */
    public long toEpochDay() {
        return millis / (24 * 60 * 60 * 1000L);
    }

    /**
     * Returns the value of the given field.
     *
     * @param  field  the field to get.
     * @return the value for the field
     * @throws UnsupportedTemporalTypeException if the field is not supported
     */
    public long getLong(final ChronoField field) {
        switch (field) {
            case INSTANT_SECONDS: return millis / 1000;
            default: throw new UnsupportedTemporalTypeException(field.toString());
        }
    }
}
