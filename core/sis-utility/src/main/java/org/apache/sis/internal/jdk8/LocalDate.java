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

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Placeholder for the {@link java.time.LocalDate} class.
 */
public final class LocalDate {
    /**
     * The calendar to use for computing dates.
     * Use of this calendar must be synchronized.
     */
    private static final GregorianCalendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);

    /**
     * Number of milliseconds since January 1st, 1970 midnight UTC.
     */
    private final long millis;

    /**
     * Creates a new date.
     *
     * @param millis Number of milliseconds since January 1st, 1970 midnight UTC.
     */
    private LocalDate(final long millis) {
        this.millis = millis;
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
     * Returns the number of days elapsed since January 1st, 1970.
     *
     * @return number of days elapsed since January 1st, 1970.
     */
    public long toEpochDay() {
        return millis / (24 * 60 * 60 * 1000L);
    }
}
