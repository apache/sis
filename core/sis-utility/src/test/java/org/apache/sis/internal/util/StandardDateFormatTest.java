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

import java.text.ParseException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.TestUtilities.date;
import static org.junit.Assert.*;

// Branch-dependent imports
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * Tests the {@link StandardDateFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.8
 * @module
 */
public final strictfp class StandardDateFormatTest extends TestCase {
    /**
     * Tests parsing a date.
     * Since the implementation is completely different in JDK8 branch than in previous branch,
     * a key purpose of this test is to ensure that the parsing is consistent between the branches.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
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
     * Tests parsing a temporal object.
     *
     * @since 0.8
     */
    @Test
    public void testParseBest() {
        final long day = 1466985600000L;
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60     )*1000),      StandardDateFormat.parseBest("2016-06-27T16:48Z"));
        assertEquals(Instant.ofEpochMilli(day + ((16*60 + 48)*60 + 12)*1000),      StandardDateFormat.parseBest("2016-06-27T16:48:12Z"));
        assertEquals(Instant.ofEpochMilli(day + (( 3*60 +  2)*60 +  1)*1000 + 90), StandardDateFormat.parseBest("2016-06-27T03:02:01.09Z"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48, 12),                    StandardDateFormat.parseBest("2016-06-27T16:48:12"));
        assertEquals(LocalDateTime.of(2016, 6, 27, 16, 48),                        StandardDateFormat.parseBest("2016-06-27T16:48"));
        assertEquals(LocalDate.of(2016, 6, 27),                                    StandardDateFormat.parseBest("2016-06-27"));
    }
}
