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
package org.apache.sis.util.collection;

import java.util.Date;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.io.PrintWriter;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.Performance;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link RangeSet} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.measure.RangeTest.class)
public final strictfp class RangeSetTest extends TestCase {
    /**
     * Tolerance factor for comparison of floating point numbers.
     * Actually we expect exact matches, because {@link RangeSet} does not perform any calculation
     * other than {@code min} and {@code max} - it just stores the values.
     */
    private static final double EPS = 0;

    /**
     * Asserts that the two given values are equals to the expected one.
     * This method is used for testing {@link RangeSet#first()} and {@link RangeSet#last()}
     * in same time than the values from the iterator.
     */
    private static void assertEqual(final Range<?> expected,
            final Range<?> fromIterator, final Range<?> fromGetter)
    {
        assertEquals("Value from iterator", expected, fromIterator);
        assertEquals("Value from getter",   expected, fromGetter);
    }

    /**
     * Verifies the value of {@link RangeSet#contains(Range, boolean)}.
     *
     * @param ranges   The range set to test.
     * @param range    The range to check for inclusion.
     * @param exact    The expected value in exact mode.
     * @param included The expected value in non-exact mode.
     */
    private static <E extends Comparable<? super E>> void checkContains(final RangeSet<E> ranges,
            final Range<E> range, final boolean exact, final boolean included)
    {
        assertEquals("RangeSet.contains(…, true)",  exact,    ranges.contains(range, true));
        assertEquals("RangeSet.contains(…, false)", included, ranges.contains(range));
    }

    /**
     * Tests {@link RangeSet#add(Range)} using integer values.
     */
    @Test
    public void testRangeOfIntegers() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertTrue(ranges.isEmpty());
        /*
         * Add a singleton element.
         */
        assertTrue(ranges.add(10, 22));
        assertEquals(1, ranges.size());
        checkContains(ranges, NumberRange.create(10, true, 22, false), true,  true);
        checkContains(ranges, NumberRange.create(10, true, 20, false), false, true);
        checkContains(ranges, NumberRange.create(10, true, 24, false), false, false);
        checkContains(ranges, NumberRange.create( 8, true, 20, false), false, false);
        /*
         * Add a new element which should be merged with the previous one.
         */
        assertTrue(ranges.add(14, 25));
        assertEquals(1, ranges.size());
        checkContains(ranges, NumberRange.create(10, true, 22, false), false, true);
        checkContains(ranges, NumberRange.create(10, true, 25, false), true,  true);
        checkContains(ranges, NumberRange.create(10, true, 25, true ), false, false);
        /*
         * Add a new element which is disjoint with other element.
         */
        assertTrue(ranges.add(-5, 5));
        assertEquals(2, ranges.size());
        checkContains(ranges, NumberRange.create(10, true, 25, false), true, true);
        checkContains(ranges, NumberRange.create(-5, true,  5, false), true, true);
        /*
         * Merge the two ranges together.
         */
        assertTrue(ranges.add(NumberRange.create(5, true, 10, false)));
        assertEquals(1, ranges.size());
        checkContains(ranges, NumberRange.create(10, true, 25, false), false, true);
        checkContains(ranges, NumberRange.create(-5, true,  5, false), false, true);
        checkContains(ranges, NumberRange.create(-5, true, 25, false), true,  true);
        /*
         * Add more ranges.
         */
        assertTrue(ranges.add(40, 50));
        assertTrue(ranges.add(30, 35));
        assertTrue(ranges.add(NumberRange.create(28, true, 32, false)));
        assertTrue(ranges.add(-20, -10));
        assertTrue(ranges.add(60, 70));
        assertEquals(5, ranges.size());
        /*
         * Verify the RangeSet content.
         */
        final Iterator<Range<Integer>> it = ranges.iterator();
        assertEqual (NumberRange.create(-20, true, -10, false), it.next(), ranges.first());
        assertEquals(NumberRange.create( -5, true,  25, false), it.next());
        assertEquals(NumberRange.create( 28, true,  35, false), it.next());
        assertEquals(NumberRange.create( 40, true,  50, false), it.next());
        assertEqual (NumberRange.create( 60, true,  70, false), it.next(), ranges.last());
        assertFalse(it.hasNext());
    }

    /**
     * Tests {@link RangeSet#add(Range)} using date values.
     */
    @Test
    public void testRangeOfDates() {
        final RangeSet<Date> ranges = RangeSet.create(Date.class, true, false);
        assertTrue(ranges.isEmpty());
        /*
         * Add a singleton range.
         */
        final long day = 24*60*60*1000L;
        final Date now = new Date();
        final Date yesterday = new Date(now.getTime() - day);
        assertTrue(ranges.add(yesterday, now));
        assertEquals(1, ranges.size());
        checkContains(ranges, new Range<Date>(Date.class, yesterday, true, now, false), true, true);
        /*
         * Add a disjoint range.
         */
        final Date lastWeek = new Date(now.getTime() - 7*day);
        final Date other = new Date(lastWeek.getTime() + 2*day);
        assertTrue(ranges.add(new Range<Date>(Date.class, lastWeek, true, other, false)));
        assertEquals(2, ranges.size());
        /*
         * Verify the RangeSet content.
         */
        final Iterator<Range<Date>> it = ranges.iterator();
        assertEqual(new Range<Date>(Date.class, lastWeek,  true, other, false), it.next(), ranges.first());
        assertEqual(new Range<Date>(Date.class, yesterday, true, now,   false), it.next(), ranges.last());
        assertFalse(it.hasNext());
    }

    /**
     * Tests {@link RangeSet#add(Range)} using string values.
     */
    @Test
    public void testRangeOfStrings() {
        final RangeSet<String> ranges = RangeSet.create(String.class, true, false);
        assertTrue(ranges.isEmpty());
        assertTrue(ranges.add("FAA", "FBB"));
        assertEquals(1, ranges.size());
        checkContains(ranges, new Range<String>(String.class, "FAA", true, "FBB", false), true, true);
        /*
         * Merge the singleton range with the given range.
         */
        assertTrue(ranges.add("FAZ", "FCC"));
        assertEquals(1, ranges.size());
        checkContains(ranges, new Range<String>(String.class, "FAA", true, "FCC", false), true, true);
        /*
         * Add a disjoint range.
         */
        assertTrue(ranges.add("GAA", "GBB"));
        assertEquals(2, ranges.size());
        /*
         * Verify the RangeSet content.
         */
        final Iterator<Range<String>> it = ranges.iterator();
        assertEqual(new Range<String>(String.class, "FAA", true, "FCC", false), it.next(), ranges.first());
        assertEqual(new Range<String>(String.class, "GAA", true, "GBB", false), it.next(), ranges.last());
        assertFalse(it.hasNext());
    }

    /**
     * Tests {@link RangeSet#comparator()}.
     */
    @Test
    public void testComparator() {
        final Comparator<Range<Integer>> comparator = RangeSet.create(Integer.class, true, false).comparator();
        @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
        final Range<Integer>[] sorted = new Range[] {
            NumberRange.create(-20, true, -10, false),
            NumberRange.create( -5, true,  25, false),
            NumberRange.create( 28, true,  35, false),
            NumberRange.create( 40, true,  50, false),
            NumberRange.create( 60, true,  70, false)
        };
        final Range<Integer>[] ranges = sorted.clone();
        Collections.shuffle(Arrays.asList(ranges));
        Arrays.sort(ranges, comparator);
        assertArrayEquals(sorted, ranges);
    }

    /**
     * Tests the {@link RangeSet#indexOfRange(Comparable)} method.
     */
    @Test
    public void testIndexOfRange() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertTrue(ranges.add( 40,  50));
        assertTrue(ranges.add( 28,  35));
        assertTrue(ranges.add(-20, -10));
        assertTrue(ranges.add( 60,  70));
        assertTrue(ranges.add( -5,  25));
        assertEquals( 0, ranges.indexOfRange(-15));
        assertEquals( 1, ranges.indexOfRange( 20));
        assertEquals( 2, ranges.indexOfRange( 28));
        assertEquals( 3, ranges.indexOfRange( 49));
        assertEquals( 4, ranges.indexOfRange( 69));
        assertEquals(-1, ranges.indexOfRange( 70));
        assertEquals(-1, ranges.indexOfRange( 26));
        assertEquals(-1, ranges.indexOfRange(-30));
    }

    /**
     * Tests the {@link RangeSet#getMinLong(int)}, {@link RangeSet#getMaxLong(int)},
     * {@link RangeSet#getMinDouble(int)} and {@link RangeSet#getMaxDouble(int)} methods.
     */
    @Test
    public void testGetAsPrimitiveType() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertTrue(ranges.add( -5,  25));
        assertTrue(ranges.add(-20, -10));
        assertEquals(-20L,  ranges.getMinLong(0));
        assertEquals(-10L,  ranges.getMaxLong(0));
        assertEquals( -5L,  ranges.getMinLong(1));
        assertEquals( 25L,  ranges.getMaxLong(1));
        assertEquals(-20.0, ranges.getMinDouble(0), 0.0);
        assertEquals(-10.0, ranges.getMaxDouble(0), 0.0);
        assertEquals( -5.0, ranges.getMinDouble(1), 0.0);
        assertEquals( 25.0, ranges.getMaxDouble(1), 0.0);
    }

    /**
     * Tests the {@link RangeSet#intersect(Range)} method. The {@code subSet(…)}, {@code headSet(…)}
     * and {@code tailSet(…)} methods delegate their work to that {@code intersect(…)} method.
     */
    @Test
    public void testIntersect() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertTrue(ranges.add(-20, -10));
        assertTrue(ranges.add( -5,  25));
        assertTrue(ranges.add( 28,  35));
        assertTrue(ranges.add( 40,  50));
        assertTrue(ranges.add( 60,  70));
        final SortedSet<Range<Integer>> subset = ranges.intersect(NumberRange.create(5, true, 45, false));
        /*
         * Verify the content. Note that the first and last ranges
         * are expected to be intersected with the [5 … 45) range.
         */
        assertEquals(5, ranges.size());
        assertEquals(3, subset.size());
        Iterator<Range<Integer>> it = subset.iterator();
        assertEqual (NumberRange.create( 5, true, 25, false), it.next(), subset.first());
        assertEquals(NumberRange.create(28, true, 35, false), it.next());
        assertEqual (NumberRange.create(40, true, 45, false), it.next(), subset.last());
        assertFalse(it.hasNext());
        /*
         * Add a range and verify the content in the sub-set.
         * Verify also that the enclosing set changed.
         */
        assertTrue(subset.add(NumberRange.create(35, true, 48, false)));
        assertEquals(4, ranges.size());
        assertEquals(2, subset.size());
        it = subset.iterator();
        assertEqual(NumberRange.create( 5, true, 25, false), it.next(), subset.first());
        assertEqual(NumberRange.create(28, true, 48, false), it.next(), subset.last());
        assertFalse(it.hasNext());
    }

    /**
     * Tests the {@link RangeSet#remove(Comparable, Comparable)} method with integer values.
     * The test is run for 4 different cases, 3 of them resulting in one range and one case
     * resulting in 2 ranges.
     *
     * @since 0.5
     */
    @Test
    @DependsOnMethod("testRangeOfIntegers")
    public void testRemoveRangeOfIntegers() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertFalse("Remove on empty collection should return false.", ranges.remove(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertTrue(ranges.add(-20, -10));
        /*
         *                   A             B
         * Range  :          [-------------]
         * Remove :                 |------------|
         *                          RA          RB
         * Expected result : [------|
         *                   A      RA
         */
        assertTrue(ranges.remove(-15, -5));
        assertEquals("size", 1, ranges.size());
        Range<Integer> r = ranges.first();
        assertEquals(-20, (int) r.getMinValue());
        assertEquals(-15, (int) r.getMaxValue());
        /*
         *                          A             B
         * Range  :                 [-------------]
         * Remove :          |------------|
         *                   RA          RB
         * Expected result :              |-------]
         *                                RB      B
         */
        assertTrue(ranges.add(-20, -10));
        assertEquals("size", 1, ranges.size());
        assertTrue(ranges.remove(-25, -15));
        assertEquals("size", 1, ranges.size());
        r = ranges.first();
        assertEquals(-15, (int) r.getMinValue());
        assertEquals(-10, (int) r.getMaxValue());
        /*
         *                   A                       B
         * Range  :          [-----------------------]
         * Remove :                 |----------|
         *                          RA         RB
         * Expected result : [------|          |-----]
         *                   A      RA         RB    B
         */
        assertTrue(ranges.add(-20, -10));
        assertEquals("size", 1, ranges.size());
        assertTrue(ranges.remove(-17, -13));
        assertEquals("size", 2, ranges.size());
        r = ranges.getRange(0);
        assertEquals(-20, (int) r.getMinValue());
        assertEquals(-17, (int) r.getMaxValue());
        r = ranges.getRange(1);
        assertEquals(-17, (int) r.getMinValue());
        assertEquals(-13, (int) r.getMaxValue());
        r = ranges.getRange(2);
        assertEquals(-13, (int) r.getMinValue());
        assertEquals(-10, (int) r.getMaxValue());
        /*
         *                       A                B
         * Range  :              [----------------]
         * Remove :           |----------------------|
         *                    RA                     RB
         * Expected result :           "empty"
         */
        assertTrue(ranges.add(-20, -10));
        assertEquals("size", 1, ranges.size());
        assertTrue(ranges.remove(-21, -9));
        assertTrue(ranges.isEmpty());
    }

    /**
     * Tests the {@link RangeSet#remove(Comparable, Comparable)} method with double values.
     * This test uses more ranges than {@link #testRemoveRangeOfIntegers()} did.
     *
     * @since 0.5
     */
    @Test
    @DependsOnMethod("testRemoveRangeOfIntegers")
    public void testRemoveRangeOfDoubles() {
        /*
         *                       A0   B0    Ai       Bi    An    Bn
         * Range  :              [----] ... [-------] ... [-----]
         * Remove :           |---------------|
         *                    RA              RB
         *
         * Expected result :                  |-----] ... [-----]
         *                                    RB    Bi    An    Bn
         */
        final RangeSet<Double> ranges = RangeSet.create(Double.class, true, false);
        assertTrue(ranges.add(-20.2, -10.1));
        assertTrue(ranges.add( -9.5,  -7.9));
        assertTrue(ranges.add( -6.7,  -3.3));
        assertTrue(ranges.add( -2.4,   1.1));
        assertTrue(ranges.add(  1.9,   4.3));
        assertTrue(ranges.add(  6.1,  12.7));
        assertTrue(ranges.add( 15.3,  21.71));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(-21.0, -1.4));
        assertEquals("size", 4, ranges.size());
        Range<Double> r = ranges.first();
        assertEquals(-1.4, r.getMinValue(), EPS);
        assertEquals( 1.1, r.getMaxValue(), EPS);
        r = ranges.last();
        assertEquals(15.3,  r.getMinValue(), EPS);
        assertEquals(21.71, r.getMaxValue(), EPS);
        /*
         *                       A0   B0    Ai       Bi    An    Bn
         * Range  :              [----] ... [-------] ... [-----]
         * Remove :                              |------------------|
         *                                       RA                 RB
         *
         * Expected result :     [-----] ... [---|
         *                       A0    B0    Ai  RA
         */
        assertTrue(ranges.add(-20.2, -10.1));
        assertTrue(ranges.add( -9.5,  -7.9));
        assertTrue(ranges.add( -6.7,  -3.3));
        assertTrue(ranges.add( -2.4,   1.1 ));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(0.7, 22.3));
        assertEquals("size", 4, ranges.size());
        r = ranges.first();
        assertEquals(-20.2, r.getMinValue(), EPS);
        assertEquals(-10.1, r.getMaxValue(), EPS);
        r = ranges.last();
        assertEquals(-2.4, r.getMinValue(), EPS);
        assertEquals( 0.7, r.getMaxValue(), EPS);
        /*
         *                       A0   B0    Ai          Bi    An    Bn
         * Range  :              [----] ... [-----------] ... [-----]
         * Remove :                             |---|
         *                                      RA  RB
         *
         * Expected result :     [----] ... [---|   |---] ... [-----]
         *                       A0    B0   Ai  RA  RB  Bi    An    Bn
         */
        assertTrue(ranges.add(-2.4,  1.1));
        assertTrue(ranges.add( 1.9,  4.3));
        assertTrue(ranges.add( 6.1, 12.7));
        assertTrue(ranges.add(15.3, 21.71));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(-5.4, -3.9));
        assertEquals("size", 8, ranges.size());
        r = ranges.getRange(4);
        assertEquals(-6.7, r.getMinValue(), EPS);
        assertEquals(-5.4, r.getMaxValue(), EPS);
        r = ranges.getRange(6);
        assertEquals(-3.9, r.getMinValue(), EPS);
        assertEquals(-3.3, r.getMaxValue(), EPS);
        /*
         *                       A0   B0    Ai    Bi   Aj    Bj    Ak     Bk    An    Bn
         * Range  :              [----] ... [-----] ...[-----] ... [------] ... [-----]
         * Remove :                             |---------------------|
         *                                      RA                   RB
         *
         * Expected result :     [----] ... [-- |                     |-- ] ... [-----]
         *                       A0    B0   Ai  RA                    RB  Bk    An    Bn
         */
        assertTrue(ranges.add(-6.7, -3.3));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(-5.4, 3.1));
        assertEquals("size", 6, ranges.size());
        r = ranges.getRange(4);
        assertEquals(-6.7, r.getMinValue(), EPS);
        assertEquals(-5.4, r.getMaxValue(), EPS);
        r = ranges.getRange(6);
        assertEquals(3.1, r.getMinValue(), EPS);
        assertEquals(4.3, r.getMaxValue(), EPS);
        /*
         *                       A0   B0   Ai    Bi Ai+1  Bi+1  Ak     Bk Ak+1  Bk+1  An    Bn
         * Range  :              [----] ...[-----]  [-----] ... [------]  [-----] ... [-----]
         * Remove :                                |---------------------|
         *                                         RA                   RB
         *
         * Expected result :     [----] ... [----]                         [-----] ... [-----]
         *                       A0    B0   Ai   Bi                        Ak+1  Bk+1  An    Bn
         */
        assertTrue(ranges.add(-6.7, -3.3));
        assertTrue(ranges.add(-2.4,  1.1));
        assertTrue(ranges.add( 1.9,  4.3));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(-7.1, 5.2));
        assertEquals("size", 4, ranges.size());
        r = ranges.getRange(2);
        assertEquals(-9.5, r.getMinValue(), EPS);
        assertEquals(-7.9, r.getMaxValue(), EPS);
        r = ranges.getRange(4);
        assertEquals( 6.1, r.getMinValue(), EPS);
        assertEquals(12.7, r.getMaxValue(), EPS);
        /*
         *                       A0   B0    An    Bn
         * Range  :              [----] ... [-----]
         * Remove :            |---------------------|
         *                     RA                    RB
         *
         * Expected result :           "Empty"
         */
        assertTrue(ranges.add(-6.7, -3.3));
        assertTrue(ranges.add(-2.4,  1.1));
        assertTrue(ranges.add( 1.9,  4.3));
        assertEquals("size", 7, ranges.size());
        assertTrue(ranges.remove(-50.5, 45.3));
        assertTrue(ranges.isEmpty());
    }

    /**
     * Tests {@link RangeSet#clone()}.
     */
    @Test
    public void testClone() {
        final RangeSet<Integer> ranges = RangeSet.create(Integer.class, true, false);
        assertTrue(ranges.add(-20, -10));
        assertTrue(ranges.add( 40,  50));
        final RangeSet<Integer> clone = ranges.clone();
        assertEquals("The clone shall be equals to the original set.", ranges, clone);
        assertTrue(ranges.add(60, 70));
        assertFalse("Modifying the original set shall not modify the clone.", ranges.equals(clone));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final RangeSet<Double> ranges = RangeSet.create(Double.class, true, false);
        assertTrue(ranges.add(12.0, 12.5));
        assertTrue(ranges.add(18.0, 18.5));
        assertTrue(ranges.add(19.0, 20.0));
        assertNotSame(ranges, assertSerializedEquals(ranges));
    }

    /**
     * Tests the performance of {@link RangeSet} implementation. This test is not executed
     * in normal SIS build. We run this test only when the {@link RangeSet} implementation
     * changed, and we want to test the impact of that change on the performance.
     *
     * @throws InterruptedException If the test has been interrupted.
     */
    @Performance
    public void stress() throws InterruptedException {
        final PrintWriter out = TestCase.out;
        final Random r = TestUtilities.createRandomNumberGenerator();
        for (int p=0; p<10; p++) {
            final long start = System.nanoTime();
            final RangeSet<Integer> set = RangeSet.create(Integer.class, true, false);
            for (int i=0; i<100000; i++) {
                final int lower = r.nextInt(1000000) - 500;
                final int upper = lower + r.nextInt(100) + 1;
                if (r.nextBoolean()) {
                    set.add(lower, upper);
                } else {
                    set.remove(lower, upper);
                }
            }
            out.print((System.nanoTime() - start) / 1E9);
            out.print(" seconds for a size of ");
            out.println(set.size());
            Thread.sleep(1000);
        }
    }
}
