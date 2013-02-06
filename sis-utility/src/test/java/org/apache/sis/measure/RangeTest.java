/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.measure;

import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests the @link Range class
 *
 * @author Joe White
 */
public final strictfp class RangeTest extends TestCase {

    @Test
    public void testCtor()
    {
        Range intRange = new Range(Integer.class, new Integer(3), new Integer(5));
        assertTrue(intRange.isMaxIncluded());
        assertTrue(intRange.isMinIncluded());
        assertFalse(intRange.isEmpty());

        Range intRange1 = new Range(Integer.class, 3, false, 5, true);
        assertTrue(intRange1.isMaxIncluded());
        assertFalse(intRange1.isMinIncluded());
        assertFalse(intRange1.isEmpty());

        Range intRange2 = new Range(Integer.class, 2, true, 7, false);
        assertFalse(intRange2.isMaxIncluded());
        assertTrue(intRange2.isMinIncluded());
        assertFalse(intRange2.isEmpty());

        Range intRange3 = new Range(Integer.class, 3, false, 10, false);
        assertFalse(intRange3.isMaxIncluded());
        assertFalse(intRange3.isMinIncluded());
        assertFalse(intRange3.isEmpty());

        Range intRange4 = new Range(Integer.class, 10, 2);
        assertTrue(intRange4.isEmpty());
    }

    @Test (expected=IllegalArgumentException.class)
    public void testCtorErrors00()
    {
        Range doubleRange = new Range(Double.class, "error", "blast");
    }

    @Test (expected=IllegalArgumentException.class)
    public void testCtorErrors01()
    {
        Range stringRange = new Range(String.class, 123.233, 8740.09);
    }

    @Test
    public void testContains()
    {
        Range intRange = new Range(Integer.class, new Integer(3), new Integer(5));
        assertTrue(intRange.contains(4));
        assertFalse(intRange.contains(6));
        assertFalse(intRange.contains(2));
        assertTrue(intRange.contains(3));
        assertTrue(intRange.contains(5));
    }


    @Test
    public void testContainsNotInclusiveMinimum()
    {
        Range intRange = new Range(Integer.class, new Integer(2), false, new Integer(5), true);
        assertTrue(intRange.contains(4));
        assertFalse(intRange.contains(6));
        assertFalse(intRange.contains(2));
        assertTrue(intRange.contains(3));
        assertTrue(intRange.contains(5));

    }

    @Test
    public void testContainsNotInclusiveMaximum()
    {
        Range intRange = new Range(Integer.class, new Integer(3), true, new Integer(6), false);
        assertTrue(intRange.contains(4));
        assertFalse(intRange.contains(6));
        assertFalse(intRange.contains(2));
        assertTrue(intRange.contains(3));
        assertTrue(intRange.contains(5));
    }

    @Test
    public void testContainsNoLowerBound()
    {
        Range intRange = new Range(Integer.class, null, new Integer(5));
        assertTrue(intRange.contains(-555));
        assertTrue(intRange.contains(5));
        assertFalse(intRange.contains(6));
    }

    @Test
    public void testContainsNoUpperBound()
    {
        Range intRange = new Range(Integer.class, new Integer(3), null);
        assertFalse(intRange.contains(1));
        assertTrue(intRange.contains(3));
        assertTrue(intRange.contains(10000));
    }

    @Test
    public void testContainsNoBounds()
    {
        Range intRange = new Range(Integer.class, null, null);
        assertTrue(intRange.contains(-55555));
        assertTrue(intRange.contains(100000));
    }

    @Test
    public void testContainsRange()
    {
        Range intRange = new Range(Integer.class, -10, 10);
        Range testRange = new Range(Integer.class, -5, 5);

        assertTrue(intRange.contains(testRange));
        assertFalse(testRange.contains(intRange));
    }

    @Test
    public void testContainsRangeNoLowerBound()
    {
        Range intRange = new Range(Integer.class, null, new Integer(500));
        Range testRange = new Range(Integer.class, -2500, 305);

        assertTrue(intRange.contains(testRange));
        assertFalse(testRange.contains(intRange));
    }

    @Test
    public void testContainsRangeNoUpperBound()
    {
        Range intRange = new Range(Integer.class, new Integer(-2500), null);
        Range testRange = new Range(Integer.class, 17, 305);

        assertTrue(intRange.contains(testRange));
        assertFalse(testRange.contains(intRange));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIncompatibleTypeRangeContains()
    {
        Range intRange = new Range(Integer.class, new Integer(0), new Integer(10));
        Range doubleRange = new Range(Double.class, new Double(2.0), new Double(5.0));

        intRange.contains(doubleRange);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIncompatibleTypeContains()
    {
        Range intRange = new Range(Integer.class, new Integer(0), new Integer(10));
        Range doubleRange = new Range(Double.class, new Double(2.0), new Double(5.0));

        intRange.contains(doubleRange);
    }

    @Test
    public void testIntersects()
    {
        Range range1 = new Range(Character.class, 'a', 'g');
        Range range2 = new Range(Character.class, 'c', 'm');

        assertTrue(range1.intersects(range2));
        assertTrue(range2.intersects(range1));

        Range range3 = new Range(Character.class, 'o', 'z');
        assertFalse(range1.intersects(range3));
        assertFalse(range3.intersects(range1));
    }

    @Test (expected=IllegalArgumentException.class)
    public void testIntersectsIncompatibleTypes()
    {
        Range range1 = new Range(Character.class, 'a', 'g');
        Range range2 = new Range(Integer.class, 5, 7);

        boolean ok = range1.intersects(range2);

    }

    @Test
    public void testIntersection()
    {
        Range range1 = new Range(Integer.class, 1, 5);
        Range range2 = new Range(Integer.class, 4, 6);

        Range intersection1 = range1.intersect(range2);
        assertTrue(intersection1.getElementClass() == Integer.class);
        assertTrue((Integer)intersection1.getMinValue() == 4);
        assertTrue((Integer)intersection1.getMaxValue() == 5);
    }

    @Test
    public void testIntersectionOfNonIntersectingRanges()
    {
        Range range1 = new Range(Integer.class, 1, 5);
        Range range2 = new Range(Integer.class, 8, 10);

        Range intersection1 = range1.intersect(range2);
        assertTrue(intersection1.getElementClass() == Integer.class);
        assertTrue(intersection1.isEmpty());
    }

    @Test
    public void testUnion()
    {
        Range range1 = new Range(Character.class, 'a', 'f');
        Range range2 = new Range(Character.class, 'd', 'h');

        Range unionRange = range1.union(range2);
        assertFalse(unionRange.isEmpty());
        assertTrue((Character)unionRange.getMinValue() == 'a');
        assertTrue((Character)unionRange.getMaxValue() == 'h');
    }

    @Test
    public void testDisjointUnion()
    {
        Range range1 = new Range(Character.class, 'a', 'f');
        Range range2 = new Range(Character.class, 'm', 'v');

        Range unionRange = range1.union(range2);
        assertFalse(unionRange.isEmpty());
        assertTrue((Character)unionRange.getMinValue() == 'a');
        assertTrue((Character)unionRange.getMaxValue() == 'v');
    }

    @Test
    public void testEquality()
    {
        //positive test - success case
        Range range1 = new Range(Character.class, 'a', 'f');
        Range range2 = new Range(Character.class, 'a', 'f');
        assertTrue(range1.equals(range2));

        //positive test - failure case
        Range range3 = new Range(Character.class, 'a', 'g');
        assertFalse(range1.equals(range3));

        //failure due to type incompatibility
        Range range4 = new Range(String.class, "a", "g");
        assertFalse(range3.equals(range4));

        Range range5 = new Range(Character.class, 'g', 'a');
        Range range6 = new Range(Character.class, 'g', 'a');
        assertTrue(range5.isEmpty());
        assertTrue(range6.isEmpty());
        assertTrue(range5.equals(range6));


    }
}
