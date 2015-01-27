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
 * Tests the {@link Range} class with date values. A previous version was using a dedicated
 * {@code DateRange} for this purpose. However the specialized class has been removed because
 * usage of {@code java.util.Date} is replaced by usage of ISO 19108 (temporal schema) types.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(RangeTest.class)
public final strictfp class DateRangeTest extends TestCase {
    /**
     * Tests {@link Range#union(Range)}.
     */
    @Test
    public void testUnion() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final Range<Date> r1 = new Range<Date>(Date.class, min, true, in2, true);
        final Range<Date> r2 = new Range<Date>(Date.class, in1, true, max, true);
        final Range<Date> rt = r1.union(r2);
        assertEquals(min, rt.getMinValue());
        assertEquals(max, rt.getMaxValue());
        assertEquals(rt, r2.union(r1));
        /*
         * Test a range fully included in the other range.
         */
        final Range<Date> outer = new Range<Date>(Date.class, min, true, max, true);
        final Range<Date> inner = new Range<Date>(Date.class, in1, true, in2, true);
        assertSame(outer, outer.union(inner));
        assertSame(outer, inner.union(outer));
    }

    /**
     * Tests {@link Range#intersect(Range)}.
     */
    @Test
    public void testIntersect() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final Range<Date> r1 = new Range<Date>(Date.class, min, true, in2, true);
        final Range<Date> r2 = new Range<Date>(Date.class, in1, true, max, true);
        final Range<Date> rt = r1.intersect(r2);
        assertEquals(in1, rt.getMinValue());
        assertEquals(in2, rt.getMaxValue());
        assertEquals(rt, r2.intersect(r1));
        /*
         * Test a range fully included in the other range.
         */
        final Range<Date> outer = new Range<Date>(Date.class, min, true, max, true);
        final Range<Date> inner = new Range<Date>(Date.class, in1, true, in2, true);
        assertSame(inner, outer.intersect(inner));
        assertSame(inner, inner.intersect(outer));
    }

    /**
     * Tests {@link Range#subtract(Range)}.
     */
    @Test
    public void testSubtract() {
        final Date min = date("1998-04-02 13:00:00");
        final Date in1 = date("1998-05-12 11:00:00");
        final Date in2 = date("1998-06-08 14:00:00");
        final Date max = date("1998-07-01 19:00:00");
        final Range<Date> outer = new Range<Date>(Date.class, min, true, max, true);
        final Range<Date> inner = new Range<Date>(Date.class, in1, true, in2, true);
        final Range<Date>[] rt = outer.subtract(inner);
        assertEquals(2, rt.length);
        assertEquals(min, rt[0].getMinValue());
        assertEquals(in1, rt[0].getMaxValue());
        assertEquals(in2, rt[1].getMinValue());
        assertEquals(max, rt[1].getMaxValue());
    }
}
