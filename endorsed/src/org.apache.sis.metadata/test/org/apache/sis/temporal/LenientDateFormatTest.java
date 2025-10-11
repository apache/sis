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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Year;
import java.util.Date;
import java.text.ParseException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link LenientDateFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LenientDateFormatTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LenientDateFormatTest() {
    }

    /**
     * Tests {@link LenientDateFormat#toISO(CharSequence, int, int)}.
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
        return LenientDateFormat.toISO(text, 0, text.length()).toString();
    }

    /**
     * Tests parsing a date.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
    public void testParse() throws ParseException {
        final long day = 1466985600000L;
        final var f = new LenientDateFormat();
        assertEquals(day + ((16*60 + 48)*60     )*1000,      f.parse("2016-06-27T16:48Z")      .getTime());
        assertEquals(day + ((16*60 + 48)*60 + 12)*1000,      f.parse("2016-06-27T16:48:12Z")   .getTime());
        assertEquals(day,                                    f.parse("2016-06-27")             .getTime());
        assertEquals(day + (( 3*60 +  2)*60 +  1)*1000 + 90, f.parse("2016-06-27T03:02:01.09Z").getTime());

        assertDateEquals("2009-01-01T05:00:00Z", f, "2009-01-01T06:00:00+01:00");
        assertDateEquals("2005-09-22T04:30:15Z", f, "2005-09-22T04:30:15Z");
        assertDateEquals("2005-09-22T04:30:15Z", f, "2005-09-22T04:30:15");
        assertDateEquals("2005-09-22T04:30:00Z", f, "2005-09-22T04:30");
        assertDateEquals("2005-09-22T04:30:00Z", f, "2005-09-22 04:30");
        assertDateEquals("2005-09-22T04:00:00Z", f, "2005-09-22T04");
        assertDateEquals("2005-09-22T00:00:00Z", f, "2005-09-22");
        assertDateEquals("2005-09-22T00:00:00Z", f, "2005-9-22");
        assertDateEquals("1992-01-01T00:00:00Z", f, "1992-1-1");
    }

    /**
     * Asserts that parsing the given date produces the expected result.
     */
    private static void assertDateEquals(final String expected, final LenientDateFormat f, final String date) throws ParseException {
        assertEquals(Instant.parse(expected), f.parse(date).toInstant());
    }

    /**
     * Tests parsing a temporal object.
     */
    @Test
    public void testParseBest() {
        final long day = 1466985600000L;
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      LenientDateFormat.parseBest("2016-06-27T16:48Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      LenientDateFormat.parseBest("2016-06-27T16:48:12Z"));
        assertEquals(Instant.ofEpochMilli(day + (( 3*60 +  2)*60 +  1)*1000 + 90), LenientDateFormat.parseBest("2016-06-27T03:02:01.09Z"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48, 12),                    LenientDateFormat.parseBest("2016-06-27T16:48:12"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48),                        LenientDateFormat.parseBest("2016-06-27T16:48"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48),                        LenientDateFormat.parseBest("2016-06-27 16:48"));
        assertEquals(LocalDate.of(2016, 6, 27),                                    LenientDateFormat.parseBest("2016-06-27"));
        assertEquals(YearMonth.of(2016, 6),                                        LenientDateFormat.parseBest("2016-06"));
        assertEquals(Year.of(2016),                                                LenientDateFormat.parseBest("2016"));
    }

    /**
     * Tests parsing a date as an instant, assuming UTC timezone if unspecified.
     */
    @Test
    public void testParseInstant() {
        final long day = 1466985600000L;
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      LenientDateFormat.parseInstantUTC("2016-06-27T16:48Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      LenientDateFormat.parseInstantUTC("2016-06-27T16:48:12Z"));
        assertEquals(Instant.ofEpochMilli(day + (( 3*60 +  2)*60 +  1)*1000 + 90), LenientDateFormat.parseInstantUTC("2016-06-27T03:02:01.09Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      LenientDateFormat.parseInstantUTC("2016-06-27T16:48:12"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      LenientDateFormat.parseInstantUTC("2016-06-27T16:48"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      LenientDateFormat.parseInstantUTC("2016-06-27 16:48"));
        assertEquals(Instant.ofEpochMilli(day),                                    LenientDateFormat.parseInstantUTC("2016-06-27"));
        assertEquals(Instant.ofEpochMilli(day + 2000),                             LenientDateFormat.parseInstantUTC("2016-06-27 00:00:02 UTC"));
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
        final var julian = new Date(-210866760000000L);         // Same epoch as CommonCRS.Temporal.JULIAN.
        final var expected = "-4713-11-24T12:00:00.000";        // Proleptic Gregorian calendar, astronomical year.
        final var f = new LenientDateFormat();
        assertEquals(expected, f.format(julian));
        assertEquals(julian, f.parse(expected));
    }
}
