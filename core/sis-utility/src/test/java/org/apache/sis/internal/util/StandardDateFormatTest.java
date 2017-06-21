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
 * @version 0.8
 * @since   0.6
 * @module
 */
public final strictfp class StandardDateFormatTest extends TestCase {
    /**
     * Verifies the condition documented in {@link StandardDateFormat#SHORT_PATTERN} javadoc.
     */
    @Test
    public void testDatePatterns() {
        assertTrue(StandardDateFormat.PATTERN.startsWith(StandardDateFormat.SHORT_PATTERN));
    }

    /**
     * Tests {@link StandardDateFormat#dateToISO(CharSequence, int, boolean)} method.
     */
    @Test
    public void testDateToISO() {
        assertEquals("2009-01-01T06:00:00.000+01:00", StandardDateFormat.dateToISO("2009-01-01T06:00:00+01:00", 0, false));
        assertEquals("2005-09-22T04:30:15.432Z",      StandardDateFormat.dateToISO("2005-09-22T04:30:15.4321Z", 0, false));
        assertEquals("2005-09-22T04:30:15.432Z",      StandardDateFormat.dateToISO("2005-09-22T04:30:15.432Z",  0, false));
        assertEquals("2005-09-22T04:30:15.000Z",      StandardDateFormat.dateToISO("2005-09-22T04:30:15Z",      0, false));
        assertEquals("2005-09-22T04:30:15.000Z",      StandardDateFormat.dateToISO("2005-09-22T04:30:15",       0, false));
        assertEquals("2005-09-22T04:30:00.000Z",      StandardDateFormat.dateToISO("2005-09-22T04:30",          0, false));
        assertEquals("2005-09-22T04:00:00.000Z",      StandardDateFormat.dateToISO("2005-09-22T04",             0, false));
        assertEquals("2005-09-22T00:00:00.000Z",      StandardDateFormat.dateToISO("2005-09-22",                0, false));
        assertEquals("2005-09-22T00:00:00.000Z",      StandardDateFormat.dateToISO("2005-9-22",                 0, false));

        String text = "2016-06-27T16:48:12Z";
        String modified = StandardDateFormat.dateToISO(text, 0, false);
        assertEquals("2016-06-27T16:48:12.000Z", modified);
        assertEquals("An index before", 18, StandardDateFormat.adjustIndex(text, modified, 0, 18));
        assertEquals("An index after",  19, StandardDateFormat.adjustIndex(text, modified, 0, 23));

        text = "2016-06-27T16:48:12.48Z";
        modified = StandardDateFormat.dateToISO(text, 0, false);
        assertEquals("2016-06-27T16:48:12.480Z", modified);
        assertEquals("An index before", 18, StandardDateFormat.adjustIndex(text, modified, 0, 18));
        assertEquals("An index after",  22, StandardDateFormat.adjustIndex(text, modified, 0, 23));
    }

    /**
     * Tests parsing a date.
     * Since the implementation is completely different in JDK8 branch than in previous branch,
     * a key purpose of this test is to ensure that the parsing is consistent between the branches.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
    @DependsOnMethod("testDateToISO")
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
        assertEquals(date("2005-09-22 04:00:00"), f.parse("2005-09-22T04"));
        assertEquals(date("2005-09-22 00:00:00"), f.parse("2005-09-22"));
        assertEquals(date("2005-09-22 00:00:00"), f.parse("2005-9-22"));
        assertEquals(date("1992-01-01 00:00:00"), f.parse("1992-1-1"));
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
        final String expected = "-4713-11-24T12:00:00.000Z";        // Proleptic Gregorian calendar, astronomical year.
        final StandardDateFormat f = new StandardDateFormat();
        assertEquals(expected, f.format(julian));
        assertEquals(julian, f.parse(expected));
    }
}
