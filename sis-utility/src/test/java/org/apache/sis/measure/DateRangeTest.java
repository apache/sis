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
package org.apache.sis.measure;

import java.util.Date;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests the {@link DateRange} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@DependsOn(RangeTest.class)
public final strictfp class DateRangeTest extends TestCase {
    /**
     * Tests {@link DateRange#union(Range)}.
     */
    @Test
    public void testUnion() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final DateRange r1 = new DateRange(min, in2);
        final DateRange r2 = new DateRange(in1, max);
        final DateRange rt = r1.union(r2);
        assertEquals(min, rt.getMinValue());
        assertEquals(max, rt.getMaxValue());
        assertEquals(rt, r2.union(r1));
        /*
         * Test a range fully included in the other range.
         */
        final DateRange outer = new DateRange(min, max);
        final DateRange inner = new DateRange(in1, in2);
        assertSame(outer, outer.union(inner));
        assertSame(outer, inner.union(outer));
        /*
         * Same test than above, but with a cast from Range to DateRange.
         */
        final Range<Date> outerAsRange = new Range<Date>(Date.class, min, max);
        assertSame(outerAsRange, outerAsRange.union(inner));
        assertEquals(outer, inner.union(outerAsRange));
    }

    /**
     * Tests {@link DateRange#intersect(Range)}.
     */
    @Test
    public void testIntersect() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final DateRange r1 = new DateRange(min, in2);
        final DateRange r2 = new DateRange(in1, max);
        final DateRange rt = r1.intersect(r2);
        assertEquals(in1, rt.getMinValue());
        assertEquals(in2, rt.getMaxValue());
        assertEquals(rt, r2.intersect(r1));
        /*
         * Test a range fully included in the other range.
         */
        final DateRange outer = new DateRange(min, max);
        final DateRange inner = new DateRange(in1, in2);
        assertSame(inner, outer.intersect(inner));
        assertSame(inner, inner.intersect(outer));
        /*
         * Same test than above, but with a cast from Range to DateRange.
         */
        final Range<Date> innerAsRange = new Range<Date>(Date.class, in1, in2);
        assertSame(innerAsRange, innerAsRange.intersect(outer));
        assertEquals(inner, outer.intersect(innerAsRange));
    }

    /**
     * Tests {@link DateRange#subtract(Range)}.
     */
    @Test
    public void testSubtract() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final DateRange outer = new DateRange(min, max);
        final DateRange inner = new DateRange(in1, in2);
        final DateRange[] rt = outer.subtract(inner);
        assertEquals(2, rt.length);
        assertEquals(min, rt[0].getMinValue());
        assertEquals(in1, rt[0].getMaxValue());
        assertEquals(in2, rt[1].getMinValue());
        assertEquals(max, rt[1].getMaxValue());
    }
}
