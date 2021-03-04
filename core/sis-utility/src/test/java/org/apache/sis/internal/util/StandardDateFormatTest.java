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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.text.ParseException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.TestUtilities.date;
import static org.junit.Assert.*;


/**
 * Tests the {@link StandardDateFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.6
 * @module
 */
public final strictfp class StandardDateFormatTest extends TestCase {
    /**
     * Verifies the {@link StandardDateFormat#MILLISECONDS_PER_DAY}, {@link StandardDateFormat#NANOS_PER_MILLISECOND}
     * and {@link StandardDateFormat#NANOS_PER_SECOND} constant values.
     */
    @Test
    public void verifyConstantValues() {
        assertEquals("MILLISECONDS_PER_DAY",  TimeUnit.DAYS.toMillis(1),        StandardDateFormat.MILLISECONDS_PER_DAY);
        assertEquals("NANOS_PER_MILLISECOND", TimeUnit.MILLISECONDS.toNanos(1), StandardDateFormat.NANOS_PER_MILLISECOND);
        assertEquals("NANOS_PER_SECOND",      TimeUnit.SECONDS.toNanos(1),      StandardDateFormat.NANOS_PER_SECOND);
    }

    /**
     * Tests {@link StandardDateFormat#toISO(CharSequence, int, int)}.
     */
    @Test
    public void testToISO() {
        assertSame  ("2009-01-01T06:00:00+01:00", toISO("2009-01-01T06:00:00+01:00"));
        assertEquals("2005-09-22T04:30:15",       toISO("2005-09-22 04:30:15"));
        assertSame  ("2005-09-22",                toISO("2005-09-22"));
        assertEquals("2005-09-22T04:30:15",       toISO("  2005-09-22   04 : 30 : 15 "));
        assertEquals("1992-10-8T15:15:42.5-6:00", toISO("1992-10-8 15:15:42.5 -6:00"));
        assertEquals("1960-01-01T00:00:00Z",      toISO("1960-01-01 00:00:00 Z"));
    }

    /**
     * Helper method for {@link #testToISO()}.
     */
    private static String toISO(final String text) {
        return StandardDateFormat.toISO(text, 0, text.length()).toString();
    }

    /**
     * Tests parsing a date.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
    @DependsOnMethod("testToISO")
    public void testParse() throws ParseException {
        final long day = 1466985600000L;
        final StandardDateFormat f = new StandardDateFormat();
        assertEquals("millis", day + ((16*60 + 48)*60     )*1000,      f.parse("2016-06-27T16:48Z")      .getTime());
        assertEquals("millis", day + ((16*60 + 48)*60 + 12)*1000,      f.parse("2016-06-27T16:48:12Z")   .getTime());
        assertEquals("millis", day,                                    f.parse("2016-06-27")             .getTime());
        assertEquals("millis", day + (( 3*60 +  2)*60 +  1)*1000 + 90, f.parse("2016-06-27T03:02:01.09Z").getTime());

        assertEquals(date("2009-01-01 05:00:00"), f.parse("2009-01-01T06:00:00+01:00"));
        assertEquals(date("2005-09-22 04:30:15"), f.parse("2005-09-22T04:30:15Z"));
        assertEquals(date("2005-09-22 04:30:15"), f.parse("2005-09-22T04:30:15"));
        assertEquals(date("2005-09-22 04:30:00"), f.parse("2005-09-22T04:30"));
        assertEquals(date("2005-09-22 04:30:00"), f.parse("2005-09-22 04:30"));
        assertEquals(date("2005-09-22 04:00:00"), f.parse("2005-09-22T04"));
        assertEquals(date("2005-09-22 00:00:00"), f.parse("2005-09-22"));
        assertEquals(date("2005-09-22 00:00:00"), f.parse("2005-9-22"));
        assertEquals(date("1992-01-01 00:00:00"), f.parse("1992-1-1"));
    }

    /**
     * Tests parsing a temporal object.
     *
     * @since 0.8
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseBest() {
        final long day = 1466985600000L;
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      StandardDateFormat.parseBest("2016-06-27T16:48Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      StandardDateFormat.parseBest("2016-06-27T16:48:12Z"));
        assertEquals(Instant.ofEpochMilli(day + (( 3*60 +  2)*60 +  1)*1000 + 90), StandardDateFormat.parseBest("2016-06-27T03:02:01.09Z"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48, 12),                    StandardDateFormat.parseBest("2016-06-27T16:48:12"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48),                        StandardDateFormat.parseBest("2016-06-27T16:48"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48),                        StandardDateFormat.parseBest("2016-06-27 16:48"));
        assertEquals(LocalDate.of(2016, 6, 27),                                    StandardDateFormat.parseBest("2016-06-27"));
    }

    /**
     * Tests parsing a date as an instant, assuming UTC timezone if unspecified.
     *
     * @since 1.0
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseInstant() {
        final long day = 1466985600000L;
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      StandardDateFormat.parseInstantUTC("2016-06-27T16:48Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      StandardDateFormat.parseInstantUTC("2016-06-27T16:48:12Z"));
        assertEquals(Instant.ofEpochMilli(day + (( 3*60 +  2)*60 +  1)*1000 + 90), StandardDateFormat.parseInstantUTC("2016-06-27T03:02:01.09Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      StandardDateFormat.parseInstantUTC("2016-06-27T16:48:12"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      StandardDateFormat.parseInstantUTC("2016-06-27T16:48"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      StandardDateFormat.parseInstantUTC("2016-06-27 16:48"));
        assertEquals(Instant.ofEpochMilli(day),                                    StandardDateFormat.parseInstantUTC("2016-06-27"));
        assertEquals(Instant.ofEpochMilli(day + 2000),                             StandardDateFormat.parseInstantUTC("2016-06-27 00:00:02 UTC"));
    }

    /**
     * Tests formatting and parsing a negative year.
     * This test uses the Julian epoch (January 1st, 4713 BC at 12:00 UTC in proleptic Julian calendar;
     * equivalent to November 24, 4714 BC when expressed in the proleptic Gregorian calendar instead).
     * We use astronomical year numbering: 4714 BC is numbered -4713.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
    public void testNegativeYear() throws ParseException {
        final Date julian = new Date(-210866760000000L);            // Same epoch than CommonCRS.Temporal.JULIAN.
        final String expected = "-4713-11-24T12:00:00.000";         // Proleptic Gregorian calendar, astronomical year.
        final StandardDateFormat f = new StandardDateFormat();
        assertEquals(expected, f.format(julian));
        assertEquals(julian, f.parse(expected));
    }
}
