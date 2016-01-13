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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Random;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link IntegerList} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class IntegerListTest extends TestCase {
    /**
     * The list of integers.
     */
    private IntegerList list;

    /**
     * Writes values and read them again for making sure they are the expected ones.
     *
     * @param maximalValue The maximal value allowed.
     */
    private void testReadWrite(final int maximalValue) {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final int length = 400;
        // Use half the lenght as initial capacity in order to test dynamic resizing.
        list = new IntegerList(length / 2, maximalValue);
        assertTrue(list.maximalValue() >= maximalValue);
        final List<Integer> copy = new ArrayList<Integer>(length);
        for (int i=0; i<length; i++) {
            assertEquals(i, list.size());
            final Integer value = nextInt(random, maximalValue);
            assertTrue(copy.add(value));
            assertTrue(list.add(value));
        }
        assertEquals(copy, list);
        assertEquals(copy.hashCode(), list.hashCode());
        /*
         * Overwrite 1/10 of the values.
         */
        for (int i=0; i<length; i+=10) {
            final Integer value = nextInt(random, maximalValue);
            final Integer old = copy.set(i, value);
            assertNotNull(old);
            assertEquals(old, list.set(i, value));
        }
        for (int i=0; i<length; i++) {
            assertEquals(String.valueOf(i), copy.get(i), list.get(i));
        }
        assertEquals(copy, list);
        assertEquals(copy.hashCode(), list.hashCode());
        assertNotSame(list, assertSerializedEquals(list));
        /*
         * Tests cloning and removal of values.
         */
        final List<Integer> clone = list.clone();
        assertEquals(copy, clone);
        assertEquals(copy.remove(100), clone.remove(100));
        assertEquals(copy, clone);
        copy .subList(128, 256).clear();
        clone.subList(128, 256).clear();
        assertEquals(copy, clone);
    }

    /**
     * Returns the next number from the random number generator.
     */
    private static int nextInt(final Random random, final int maximalValue) {
        if (maximalValue == Integer.MAX_VALUE) {
            return abs(random.nextInt());
        } else {
            return random.nextInt(maximalValue + 1);
        }
    }

    /**
     * Tests the fill value using the existing list, which is assumed
     * already filled with random values prior this method call.
     */
    private void testFill(final int value) {
        assertEquals(400, list.size());
        final Set<Integer> set = new HashSet<Integer>();
        list.fill(value);
        set.addAll(list);
        assertEquals(Collections.singleton(value), set);
        list.fill(0);
        set.clear();
        set.addAll(list);
        assertEquals(Collections.singleton(0), set);
    }

    /**
     * Tests with a maximal value of 1.
     */
    @Test
    public void test1() {
        testReadWrite(1);
        testFill(1);
    }

    /**
     * Tests with a maximal value of 2.
     */
    @Test
    public void test2() {
        testReadWrite(2);
        testFill(2);
    }

    /**
     * Tests with a maximal value of 3.
     */
    @Test
    public void test3() {
        testReadWrite(3);
        testFill(3);
    }

    /**
     * Tests with a maximal value of 10.
     */
    @Test
    public void test10() {
        testReadWrite(10);
        testFill(10);
    }

    /**
     * Tests with a maximal value of 100.
     */
    @Test
    public void test100() {
        testReadWrite(100);
        assertEquals(400, list.size());
        final int old100 = list.getInt(100);
        list.resize(101);
        assertEquals(old100, list.getInt(100));
        list.resize(200);
        assertEquals(200, list.size());
        assertEquals(old100, list.getInt(100));
        assertEquals(0, list.getInt(101));
        for (int i=101; i<200; i++) {
            assertEquals(0, list.getInt(i));
        }
        list.resize(400);
        testFill(100);
    }

    /**
     * Tests with a maximal value of 100000.
     */
    @Test
    public void test100000() {
        testReadWrite(100000);
        testFill(17);
    }

    /**
     * Tests with a maximal value of {@value Integer#MAX_VALUE}.
     */
    @Test
    public void testMax() {
        testReadWrite(Integer.MAX_VALUE);
        testFill(17);
    }
}
