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

import java.util.Locale;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Range} class.
 *
 * @author  Joe White
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class RangeTest extends TestCase {
    /**
     * Tests the creation of {@link Range} objects under normal conditions.
     */
    @Test
    public void testConstructor() {
        Range<Integer> range = new Range<Integer>(Integer.class, 3, true, 5, true);
        assertEquals(Integer.valueOf(3), range.getMinValue());
        assertEquals(Integer.valueOf(5), range.getMaxValue());
        assertTrue  (range.isMaxIncluded());
        assertTrue  (range.isMinIncluded());
        assertFalse (range.isEmpty());

        range = new Range<Integer>(Integer.class, 3, false, 5, true);
        assertEquals(Integer.valueOf(3), range.getMinValue());
        assertEquals(Integer.valueOf(5), range.getMaxValue());
        assertTrue  (range.isMaxIncluded());
        assertFalse (range.isMinIncluded());
        assertFalse (range.isEmpty());

        range = new Range<Integer>(Integer.class, 2, true, 7, false);
        assertEquals(Integer.valueOf(2), range.getMinValue());
        assertEquals(Integer.valueOf(7), range.getMaxValue());
        assertFalse (range.isMaxIncluded());
        assertTrue  (range.isMinIncluded());
        assertFalse (range.isEmpty());

        range = new Range<Integer>(Integer.class, 3, false, 10, false);
        assertEquals(Integer.valueOf( 3), range.getMinValue());
        assertEquals(Integer.valueOf(10), range.getMaxValue());
        assertFalse (range.isMaxIncluded());
        assertFalse (range.isMinIncluded());
        assertFalse (range.isEmpty());

        range = new Range<Integer>(Integer.class, 10, true, 2, true);
        assertEquals(Integer.valueOf(10), range.getMinValue());
        assertEquals(Integer.valueOf( 2), range.getMaxValue());
        assertTrue (range.isEmpty());
    }

    /**
     * Tests the detection of illegal arguments at {@link Range} creation time.
     * Note that such error should never happen when parameterized types are used.
     * The check performed by the constructor is a safety in case the user bypass
     * the parameterized type check by using the raw type instead.
     *
     * <p>This test requires assertions to be enabled.</p>
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testConstructorErrors00() {
        assumeTrue(Range.class.desiredAssertionStatus());
        new Range(Double.class, "error", true, "blast", true);
    }

    /**
     * Tests the detection of illegal arguments at {@link Range} creation time.
     * Note that such error should never happen when parameterized types are used.
     * The check performed by the constructor is a safety in case the user bypass
     * the parameterized type check by using the raw type instead.
     *
     * <p>This test requires assertions to be enabled.</p>
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testConstructorErrors01() {
        assumeTrue(Range.class.desiredAssertionStatus());
        new Range(String.class, 123.233, true, 8740.09, true);
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method.
     */
    @Test
    public void testContains() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 3, true, 5, true);
        assertTrue (range.contains(4));
        assertFalse(range.contains(6));
        assertFalse(range.contains(2));
        assertTrue (range.contains(3));
        assertTrue (range.contains(5));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method with an exclusive minimal value.
     */
    @Test
    public void testContainsNotInclusiveMinimum() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 2, false, 5, true);
        assertTrue (range.contains(4));
        assertFalse(range.contains(6));
        assertFalse(range.contains(2));
        assertTrue (range.contains(3));
        assertTrue (range.contains(5));

    }

    /**
     * Tests the {@link Range#contains(Comparable)} method with an exclusive maximal value.
     */
    @Test
    public void testContainsNotInclusiveMaximum() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 3, true, 6, false);
        assertTrue (range.contains(4));
        assertFalse(range.contains(6));
        assertFalse(range.contains(2));
        assertTrue (range.contains(3));
        assertTrue (range.contains(5));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method without lower endpoint.
     */
    @Test
    public void testContainsNoLowerEndpoint() {
        final Range<Integer> range = new Range<Integer>(Integer.class, null, true, 5, true);
        assertTrue (range.contains(-555));
        assertTrue (range.contains(5));
        assertFalse(range.contains(6));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method without upper endpoint.
     */
    @Test
    public void testContainsNoUpperEndpoint() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 3, true, null, true);
        assertFalse(range.contains(1));
        assertTrue (range.contains(3));
        assertTrue (range.contains(10000));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method without lower or upper endpoints.
     */
    @Test
    public void testContainsNoEndpoints() {
        final Range<Integer> range = new Range<Integer>(Integer.class, null, true, null, true);
        assertTrue(range.contains(-55555));
        assertTrue(range.contains(100000));
    }

    /**
     * Tests the {@link Range#contains(Range)} method.
     */
    @Test
    public void testContainsRange() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -10, true, 10, true);
        final Range<Integer> inside = new Range<Integer>(Integer.class,  -5, true,  5, true);

        assertTrue(range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#contains(Range)} method without lower endpoint.
     */
    @Test
    public void testContainsRangeNoLowerEndpoint() {
        final Range<Integer> range  = new Range<Integer>(Integer.class,  null, true, 500, true);
        final Range<Integer> inside = new Range<Integer>(Integer.class, -2500, true, 305, true);

        assertTrue (range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#contains(Range)} method without upper endpoint.
     */
    @Test
    public void testContainsRangeNoUpperEndpoint() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -2500, true, null, true);
        final Range<Integer> inside = new Range<Integer>(Integer.class,    17, true,  305, true);

        assertTrue(range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#intersects(Range)} method.
     */
    @Test
    public void testIntersects() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', true, 'g', true);
        final Range<Character> range2 = new Range<Character>(Character.class, 'c', true, 'm', true);
        final Range<Character> range3 = new Range<Character>(Character.class, 'o', true, 'z', true);

        assertTrue (range1.intersects(range2));
        assertTrue (range2.intersects(range1));
        assertFalse(range1.intersects(range3));
        assertFalse(range3.intersects(range1));
    }

    /**
     * Tests the {@link Range#intersect(Range)} method.
     */
    @Test
    public void testIntersection() {
        final Range<Integer> range1 = new Range<Integer>(Integer.class, 1, true, 5, true);
        final Range<Integer> range2 = new Range<Integer>(Integer.class, 4, true, 6, true);

        final Range<?> intersection = range1.intersect(range2);
        assertEquals(Integer.class, intersection.getElementType());
        assertEquals(Integer.valueOf(4), intersection.getMinValue());
        assertEquals(Integer.valueOf(5), intersection.getMaxValue());
    }

    /**
     * Tests the {@link Range#intersect(Range)} method with arguments resulting in empty range.
     */
    @Test
    public void testIntersectionOfNonIntersectingRanges() {
        final Range<Integer> range1 = new Range<Integer>(Integer.class, 1, true,  5, true);
        final Range<Integer> range2 = new Range<Integer>(Integer.class, 8, true, 10, true);

        final Range<?> intersection = range1.intersect(range2);
        assertEquals(Integer.class, intersection.getElementType());
        assertTrue(intersection.isEmpty());
    }

    /**
     * Tests the {@link Range#union(Range)} method.
     */
    @Test
    public void testUnion() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', true, 'f', true);
        final Range<Character> range2 = new Range<Character>(Character.class, 'd', true, 'h', true);

        final Range<?> union = range1.union(range2);
        assertFalse(union.isEmpty());
        assertEquals(Character.valueOf('a'), union.getMinValue());
        assertEquals(Character.valueOf('h'), union.getMaxValue());
    }

    /**
     * Tests the {@link Range#union(Range)} method with disjoint ranges.
     */
    @Test
    public void testDisjointUnion() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', true, 'f', true);
        final Range<Character> range2 = new Range<Character>(Character.class, 'm', true, 'v', true);

        final Range<?> unionRange = range1.union(range2);
        assertFalse(unionRange.isEmpty());
        assertEquals(Character.valueOf('a'), unionRange.getMinValue());
        assertEquals(Character.valueOf('v'), unionRange.getMaxValue());
    }

    /**
     * Tests the {@link Range#subtract(Range)} method.
     */
    @Test
    public void testSubtract() {
        final Range<Integer> range1 = new Range<Integer>(Integer.class, 10, true, 40, true);
        final Range<Integer> range2 = new Range<Integer>(Integer.class, 20, true, 25, true);
        final Range<Integer>[] subtract = range1.subtract(range2);
        assertEquals(2, subtract.length);
        assertEquals(new Range<Integer>(Integer.class, 10, true,  20, false), subtract[0]);
        assertEquals(new Range<Integer>(Integer.class, 25, false, 40, true),  subtract[1]);
    }

    /**
     * Tests the {@link Range#toString()} method.
     */
    @Test
    public void testToString() {
        assertEquals("{}",        new Range<Integer>(Integer.class, 10, false, 10, false).toString());
        assertEquals("{10}",      new Range<Integer>(Integer.class, 10, true,  10, true ).toString());
        assertEquals("[10 … 20]", new Range<Integer>(Integer.class, 10, true,  20, true ).toString());
        assertEquals("(10 … 20)", new Range<Integer>(Integer.class, 10, false, 20, false).toString());
        assertEquals("[10 … 20)", new Range<Integer>(Integer.class, 10, true,  20, false).toString());
        assertEquals("(10 … 20]", new Range<Integer>(Integer.class, 10, false, 20, true ).toString());
    }

    /**
     * Tests the {@link Range#formatTo(Formatter, int, int, int)} method.
     */
    @Test
    public void testFormatTo() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 10, true,  20, false);
        assertEquals("[10 … 20)",    String.format(Locale.CANADA, "%s", range));
        assertEquals("[10 … 20)",    String.format(Locale.CANADA, "%4s", range));
        assertEquals("[10 … 20)   ", String.format(Locale.CANADA, "%-12s", range));
        assertEquals("   [10 … 20)", String.format(Locale.CANADA, "%12s", range));
        assertEquals("[10 … 20[   ", String.format(Locale.CANADA, "%#-12s", range));
    }

    /**
     * Tests the {@link Range#equals(Object)} method.
     */
    @Test
    public void testEquality() {
        // Positive test - success case
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', true, 'f', true);
        final Range<Character> range2 = new Range<Character>(Character.class, 'a', true, 'f', true);
        assertTrue(range1.equals(range2));

        // Positive test - failure case
        final Range<Character> range3 = new Range<Character>(Character.class, 'a', true, 'g', true);
        assertFalse(range1.equals(range3));

        // Failure due to type incompatibility
        final Range<String> range4 = new Range<String>(String.class, "a", true, "g", true);
        assertFalse(range3.equals(range4));

        final Range<Character> range5 = new Range<Character>(Character.class, 'g', true, 'a', true);
        final Range<Character> range6 = new Range<Character>(Character.class, 'g', true, 'a', true);
        assertTrue(range5.isEmpty());
        assertTrue(range6.isEmpty());
        assertTrue(range5.equals(range6));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -10, true, 10, true);
        assertNotSame(range, assertSerializedEquals(range));
    }
}
