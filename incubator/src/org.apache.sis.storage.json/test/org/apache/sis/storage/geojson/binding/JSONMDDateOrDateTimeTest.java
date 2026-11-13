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
package org.apache.sis.storage.geojson.binding;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JSONMDDateOrDateTime}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class JSONMDDateOrDateTimeTest {

    @Test
    public void testParse() {
        assertEquals(Year.of(2003),                   new JSONMDDateOrDateTime("2003").getValue());
        assertEquals(YearMonth.of(2010, 7),           new JSONMDDateOrDateTime("2010-07").getValue());
        assertEquals(LocalDate.of(2010, 7, 5),        new JSONMDDateOrDateTime("2010-07-05").getValue());
        assertEquals(LocalDateTime.of(2010, 7, 5, 12, 30), new JSONMDDateOrDateTime("2010-07-05T12:30:00").getValue());
        assertEquals(OffsetDateTime.of(2018, 4, 20, 6, 19, 51, 0, ZoneOffset.UTC),
                     new JSONMDDateOrDateTime("2018-04-20T06:19:51Z").getValue());
        assertEquals(OffsetDateTime.of(2008, 3, 31, 0, 0, 0, 0, ZoneOffset.ofHours(11)),
                     new JSONMDDateOrDateTime("2008-03-31T00:00:00+11:00").getValue());
    }

    @Test
    public void testFormat() {
        assertEquals("2003",                 new JSONMDDateOrDateTime(Year.of(2003)).toString());
        assertEquals("2010-07",              new JSONMDDateOrDateTime(YearMonth.of(2010, 7)).toString());
        assertEquals("2010-07-05",           new JSONMDDateOrDateTime(LocalDate.of(2010, 7, 5)).toString());
        assertEquals("2010-07-05T00:00:00",  new JSONMDDateOrDateTime(LocalDateTime.of(2010, 7, 5, 0, 0)).toString());
        assertEquals("2010-02-04T00:00:00Z", new JSONMDDateOrDateTime(Instant.parse("2010-02-04T00:00:00Z")).toString());
        assertEquals("2010-02-04T00:00:00Z",
                     new JSONMDDateOrDateTime(OffsetDateTime.of(2010, 2, 4, 0, 0, 0, 0, ZoneOffset.UTC)).toString());
        assertEquals("2010-02-04T00:00:00+01:00",
                     new JSONMDDateOrDateTime(ZonedDateTime.of(2010, 2, 4, 0, 0, 0, 0, ZoneId.of("Europe/Paris"))).toString());
    }

    @Test
    public void testRoundTrip() {
        for (final String text : new String[] {"1995", "1995-02", "1995-02-09", "1995-02-09T10:15:30Z", "1995-02-09T10:15:30.5+02:00"}) {
            assertEquals(text, new JSONMDDateOrDateTime(text).toString());
        }
    }

    @Test
    public void testInvalid() {
        assertThrows(DateTimeParseException.class, () -> new JSONMDDateOrDateTime("20xx"));
        assertThrows(IllegalArgumentException.class, () -> new JSONMDDateOrDateTime(LocalTime.NOON));
    }
}
