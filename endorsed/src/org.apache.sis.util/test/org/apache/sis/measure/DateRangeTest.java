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

import java.time.Instant;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Range} class with temporal values.
 *
 * @author  Martin Desruisseaux (IRD)
 */
@SuppressWarnings("exports")
public final class DateRangeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DateRangeTest() {
    }

    /**
     * Tests {@link Range#union(Range)}.
     */
    @Test
    public void testUnion() {
        final Instant min = Instant.parse("1998-04-02T13:00:00Z");
        final Instant in1 = Instant.parse("1998-05-12T11:00:00Z");
        final Instant in2 = Instant.parse("1998-06-08T14:00:00Z");
        final Instant max = Instant.parse("1998-07-01T19:00:00Z");
        final Range<Instant> r1 = new Range<>(Instant.class, min, true, in2, true);
        final Range<Instant> r2 = new Range<>(Instant.class, in1, true, max, true);
        final Range<Instant> rt = r1.union(r2);
        assertEquals(min, rt.getMinValue());
        assertEquals(max, rt.getMaxValue());
        assertEquals(rt, r2.union(r1));
        /*
         * Test a range fully included in the other range.
         */
        final Range<Instant> outer = new Range<>(Instant.class, min, true, max, true);
        final Range<Instant> inner = new Range<>(Instant.class, in1, true, in2, true);
        assertSame(outer, outer.union(inner));
        assertSame(outer, inner.union(outer));
    }

    /**
     * Tests {@link Range#intersect(Range)}.
     */
    @Test
    public void testIntersect() {
        final Instant min = Instant.parse("1998-04-02T13:00:00Z");
        final Instant in1 = Instant.parse("1998-05-12T11:00:00Z");
        final Instant in2 = Instant.parse("1998-06-08T14:00:00Z");
        final Instant max = Instant.parse("1998-07-01T19:00:00Z");
        final Range<Instant> r1 = new Range<>(Instant.class, min, true, in2, true);
        final Range<Instant> r2 = new Range<>(Instant.class, in1, true, max, true);
        final Range<Instant> rt = r1.intersect(r2);
        assertEquals(in1, rt.getMinValue());
        assertEquals(in2, rt.getMaxValue());
        assertEquals(rt, r2.intersect(r1));
        /*
         * Test a range fully included in the other range.
         */
        final Range<Instant> outer = new Range<>(Instant.class, min, true, max, true);
        final Range<Instant> inner = new Range<>(Instant.class, in1, true, in2, true);
        assertSame(inner, outer.intersect(inner));
        assertSame(inner, inner.intersect(outer));
    }

    /**
     * Tests {@link Range#subtract(Range)}.
     */
    @Test
    public void testSubtract() {
        final Instant min = Instant.parse("1998-04-02T13:00:00Z");
        final Instant in1 = Instant.parse("1998-05-12T11:00:00Z");
        final Instant in2 = Instant.parse("1998-06-08T14:00:00Z");
        final Instant max = Instant.parse("1998-07-01T19:00:00Z");
        final Range<Instant> outer = new Range<>(Instant.class, min, true, max, true);
        final Range<Instant> inner = new Range<>(Instant.class, in1, true, in2, true);
        final Range<Instant>[] rt = outer.subtract(inner);
        assertEquals(2, rt.length);
        assertEquals(min, rt[0].getMinValue());
        assertEquals(in1, rt[0].getMaxValue());
        assertEquals(in2, rt[1].getMinValue());
        assertEquals(max, rt[1].getMaxValue());
    }
}
