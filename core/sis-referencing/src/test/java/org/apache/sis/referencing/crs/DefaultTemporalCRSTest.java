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

import java.time.Instant;
import java.util.Date;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultTemporalCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final strictfp class DefaultTemporalCRSTest extends TestCase {
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
        assertWktEquals(Convention.WKT2,
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
        final double  value   = 58543.25;                               // 3 march 2019.
        final Date    date    = HardCodedCRS.TIME.toDate(value);
        final Instant instant = HardCodedCRS.TIME.toInstant(value);
        assertEquals("toInstant", Instant.ofEpochSecond(1551420000L), instant);
        assertEquals("toDate",    instant, date.toInstant());
        assertEquals("toValue",   value, HardCodedCRS.TIME.toValue(instant), STRICT);
        assertEquals("toValue",   value, HardCodedCRS.TIME.toValue(date), STRICT);
    }
}
