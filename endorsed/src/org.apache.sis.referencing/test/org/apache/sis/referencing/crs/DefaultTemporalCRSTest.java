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
package org.apache.sis.referencing.crs;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.io.wkt.Convention;
import static org.apache.sis.util.privy.Constants.MILLISECONDS_PER_DAY;
import static org.apache.sis.util.privy.Constants.NANOS_PER_MILLISECOND;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests {@link DefaultTemporalCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultTemporalCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultTemporalCRSTest() {
    }

    /**
     * Tests WKT 1 pseudo-formatting.
     * This is not part of OGC 01-009 standard.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "TIMECRS[“Time”,\n" +
                "  TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17]],\n" +
                "  TIMEUNIT[“day”, 86400],\n" +
                "  AXIS[“Time”, FUTURE]]",
                HardCodedCRS.TIME);
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    public void testWKT2() {
        assertWktEquals(Convention.WKT2_2015,
                "TIMECRS[“Time”,\n" +
                "  TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17]],\n" +
                "  CS[temporal, 1],\n" +
                "    AXIS[“Time (t)”, future, ORDER[1]],\n" +
                "    TIMEUNIT[“day”, 86400]]",
                HardCodedCRS.TIME);
    }

    /**
     * Tests WKT 2 "simplified" formatting.
     */
    @Test
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "TimeCRS[“Time”,\n" +
                "  TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17]],\n" +
                "  CS[temporal, 1],\n" +
                "    Axis[“Time (t)”, future],\n" +
                "    TimeUnit[“day”, 86400]]",      // ISO 19162 does not allow "Unit" keyword here.
                HardCodedCRS.TIME);
    }

    /**
     * Tests {@link DefaultTemporalCRS#toDate(double)} and its converse.
     * Also compares with {@link DefaultTemporalCRS#toInstant(double)}.
     */
    @Test
    public void testDateConversion() {
        final DefaultTemporalCRS crs = HardCodedCRS.TIME;
        final double  value   = 58543.25;                               // 2019-03-01T06:00:00Z
        final Date    date    = crs.toDate(value);
        final Instant instant = crs.toInstant(value);
        assertEquals(Instant.ofEpochSecond(1551420000L), instant);
        assertEquals(instant, date.toInstant());
        assertEquals(value, crs.toValue(instant));
        assertEquals(value, crs.toValue(date));
    }

    /**
     * Same as {@link #testDateConversion()} but with a nanosecond component.
     * Fraction of seconds need to be handled in special way by current implementation.
     */
    @Test
    public void testDateConversionWithNanos() {
        final var datum = new DefaultTemporalDatum(
                Map.of(DefaultTemporalDatum.NAME_KEY, "For test"),
                Instant.ofEpochMilli(10000L * MILLISECONDS_PER_DAY + 12345));       // 1997-05-19T00:00:12.345Z
        final var crs = new DefaultTemporalCRS(
                Map.of(DefaultTemporalCRS.NAME_KEY, datum.getName()),
                datum, null, HardCodedCS.DAYS);
        /*
         * DefaultTemporalCRS.toSeconds converter should have a non-zero offset because of the 0.345 seconds offset
         * in temporal datum. Ask for a date two days after the origin and verify that the 12.345 part is missing.
         */
        final double ε = 1E-15;
        assertEquals(2 - 12.345 / (60*60*24), crs.toValue(new Date(10002L * MILLISECONDS_PER_DAY)),  ε);
        assertEquals(2 - 12.345 / (60*60*24), crs.toValue(Instant.ofEpochSecond(10002L*(60*60*24))), ε);
        /*
         * Add a millisecond component and test again.
         */
        final Instant t = Instant.ofEpochSecond(10002L*(60*60*24) + 15, 789 * NANOS_PER_MILLISECOND);
        final double  v = 2 + (15.789 - 12.345) / (60*60*24);
        assertEquals(v,            crs.toValue(t), ε);
        assertEquals(v,            crs.toValue(Date.from(t)), ε);
        assertEquals(t,            crs.toInstant(v));
        assertEquals(Date.from(t), crs.toDate(v));
    }

    /**
     * Tests {@link DefaultTemporalCRS#toDuration(double)} and its converse.
     */
    @Test
    public void testDurationConversion() {
        final DefaultTemporalCRS crs = HardCodedCRS.TIME;
        final Duration duration = crs.toDuration(4.25);
        assertEquals(  4, duration.toDays());
        assertEquals(102, duration.toHours());
        assertEquals(4.25, crs.toValue(duration));
    }
}
