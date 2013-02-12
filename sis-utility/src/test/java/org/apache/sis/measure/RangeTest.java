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

import org.apache.sis.test.TestCase;
import org.junit.Test;

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
        Range<Integer> range = new Range<Integer>(Integer.class, 3, 5);
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

        range = new Range<Integer>(Integer.class, 10, 2);
        assertEquals(Integer.valueOf(10), range.getMinValue());
        assertEquals(Integer.valueOf( 2), range.getMaxValue());
        assertTrue (range.isEmpty());
    }

    /**
     * Tests the detection of illegal arguments at {@link Range} creation time.
     * Note that such error should never happen when parameterized types are used.
     * The check performed by the constructor is a safety in case the user bypass
     * the parameterized type check by using the raw type instead.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testConstructorErrors00() {
        new Range(Double.class, "error", "blast");
    }

    /**
     * Tests the detection of illegal arguments at {@link Range} creation time.
     * Note that such error should never happen when parameterized types are used.
     * The check performed by the constructor is a safety in case the user bypass
     * the parameterized type check by using the raw type instead.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testConstructorErrors01() {
        new Range(String.class, 123.233, 8740.09);
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method.
     */
    @Test
    public void testContains() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 3, 5);
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
     * Tests the {@link Range#contains(Comparable)} method without lower bound.
     */
    @Test
    public void testContainsNoLowerBound() {
        final Range<Integer> range = new Range<Integer>(Integer.class, null, 5);
        assertTrue (range.contains(-555));
        assertTrue (range.contains(5));
        assertFalse(range.contains(6));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method without upper bound.
     */
    @Test
    public void testContainsNoUpperBound() {
        final Range<Integer> range = new Range<Integer>(Integer.class, 3, null);
        assertFalse(range.contains(1));
        assertTrue (range.contains(3));
        assertTrue (range.contains(10000));
    }

    /**
     * Tests the {@link Range#contains(Comparable)} method without lower or upper bounds.
     */
    @Test
    public void testContainsNoBounds() {
        final Range<Integer> range = new Range<Integer>(Integer.class, null, null);
        assertTrue(range.contains(-55555));
        assertTrue(range.contains(100000));
    }

    /**
     * Tests the {@link Range#contains(Range)} method.
     */
    @Test
    public void testContainsRange() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -10, 10);
        final Range<Integer> inside = new Range<Integer>(Integer.class,  -5,  5);

        assertTrue(range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#contains(Range)} method without lower bound.
     */
    @Test
    public void testContainsRangeNoLowerBound() {
        final Range<Integer> range  = new Range<Integer>(Integer.class,  null, 500);
        final Range<Integer> inside = new Range<Integer>(Integer.class, -2500, 305);

        assertTrue (range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#contains(Range)} method without upper bound.
     */
    @Test
    public void testContainsRangeNoUpperBound() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -2500, null);
        final Range<Integer> inside = new Range<Integer>(Integer.class,    17,  305);

        assertTrue(range.contains(inside));
        assertFalse(inside.contains(range));
    }

    /**
     * Tests the {@link Range#contains(Range)} method with a range of incompatible type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleTypeRangeContains() {
        final Range<Integer> intRange = new Range<Integer>(Integer.class, 0, 10);
        final Range<Double> doubleRange = new Range<Double>(Double.class, 2.0, 5.0);

        intRange.contains(doubleRange);
    }

    /**
     * Tests the {@link Range#contains(Range)} method with a range of incompatible type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleTypeContains() {
        final Range<Integer> intRange = new Range<Integer>(Integer.class, 0, 10);
        final Range<Double> doubleRange = new Range<Double>(Double.class, 2.0, 5.0);

        intRange.contains(doubleRange);
    }

    /**
     * Tests the {@link Range#intersects(Range)} method.
     */
    @Test
    public void testIntersects() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', 'g');
        final Range<Character> range2 = new Range<Character>(Character.class, 'c', 'm');
        final Range<Character> range3 = new Range<Character>(Character.class, 'o', 'z');

        assertTrue (range1.intersects(range2));
        assertTrue (range2.intersects(range1));
        assertFalse(range1.intersects(range3));
        assertFalse(range3.intersects(range1));
    }

    /**
     * Tests the {@link Range#intersects(Range)} method with a range of incompatible type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIntersectsIncompatibleTypes() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', 'g');
        final Range<Integer>   range2 = new Range<Integer>(Integer.class, 5, 7);

        range1.intersects(range2);
    }

    /**
     * Tests the {@link Range#intersect(Range)} method.
     */
    @Test
    public void testIntersection() {
        final Range<Integer> range1 = new Range<Integer>(Integer.class, 1, 5);
        final Range<Integer> range2 = new Range<Integer>(Integer.class, 4, 6);

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
        final Range<Integer> range1 = new Range<Integer>(Integer.class, 1,  5);
        final Range<Integer> range2 = new Range<Integer>(Integer.class, 8, 10);

        final Range<?> intersection = range1.intersect(range2);
        assertEquals(Integer.class, intersection.getElementType());
        assertTrue(intersection.isEmpty());
    }

    /**
     * Tests the {@link Range#union(Range)} method.
     */
    @Test
    public void testUnion() {
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', 'f');
        final Range<Character> range2 = new Range<Character>(Character.class, 'd', 'h');

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
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', 'f');
        final Range<Character> range2 = new Range<Character>(Character.class, 'm', 'v');

        final Range<?> unionRange = range1.union(range2);
        assertFalse(unionRange.isEmpty());
        assertEquals(Character.valueOf('a'), unionRange.getMinValue());
        assertEquals(Character.valueOf('v'), unionRange.getMaxValue());
    }

    /**
     * Tests the {@link Range#equals(Object)} method.
     */
    @Test
    public void testEquality() {
        // Positive test - success case
        final Range<Character> range1 = new Range<Character>(Character.class, 'a', 'f');
        final Range<Character> range2 = new Range<Character>(Character.class, 'a', 'f');
        assertTrue(range1.equals(range2));

        // Positive test - failure case
        final Range<Character> range3 = new Range<Character>(Character.class, 'a', 'g');
        assertFalse(range1.equals(range3));

        // Failure due to type incompatibility
        final Range<String> range4 = new Range<String>(String.class, "a", "g");
        assertFalse(range3.equals(range4));

        final Range<Character> range5 = new Range<Character>(Character.class, 'g', 'a');
        final Range<Character> range6 = new Range<Character>(Character.class, 'g', 'a');
        assertTrue(range5.isEmpty());
        assertTrue(range6.isEmpty());
        assertTrue(range5.equals(range6));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Range<Integer> range  = new Range<Integer>(Integer.class, -10, 10);
        assertNotSame(range, assertSerializedEquals(range));
    }
}
