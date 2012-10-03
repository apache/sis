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

import java.util.HashSet;
import java.util.Random;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link WeakHashSet}.
 * A standard {@link HashSet} object is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
public final strictfp class WeakHashSetTest extends TestCase {
    /**
     * Tests the {@link WeakHashSet} using strong references.
     * The tested {@link WeakHashSet} should behave like a standard {@link Set} object.
     */
    @Test
    public void testStrongReferences() {
        final Random random = new Random();
        for (int pass=0; pass<20; pass++) {
            final WeakHashSet<Integer> weakSet = WeakHashSet.newInstance(Integer.class);
            final HashSet<Integer> strongSet = new HashSet<Integer>();
            for (int i=0; i<1000; i++) {
                final Integer value = random.nextInt(500);
                if (random.nextBoolean()) {
                    /*
                     * Tests addition.
                     */
                    final boolean   weakModified = weakSet  .add(value);
                    final boolean strongModified = strongSet.add(value);
                    assertEquals("add:", strongModified, weakModified);
                    if (strongModified) {
                        assertSame("get:", value, weakSet.get(value));
                    } else {
                        assertEquals("get:",  value, weakSet.get(value));
                    }
                } else {
                    /*
                     * Tests remove
                     */
                    final boolean   weakModified = weakSet  .remove(value);
                    final boolean strongModified = strongSet.remove(value);
                    assertEquals("remove:", strongModified, weakModified);
                    assertNull("get:", weakSet.get(value));
                }
                assertEquals("contains:", strongSet.contains(value), weakSet.contains(value));
                assertEquals("equals:", strongSet, weakSet);
            }
        }
    }

    /**
     * Tests the {@link WeakHashSet} using weak references. In this test, we have to keep
     * in mind that some elements in {@code weakSet} may disappear at any time!
     *
     * @throws InterruptedException If the test has been interrupted.
     */
    @Test
    @DependsOnMethod("testStrongReferences")
    public void testWeakReferences() throws InterruptedException {
        final Random random = new Random();
        for (int pass=0; pass<2; pass++) {
            final WeakHashSet<Integer> weakSet = WeakHashSet.newInstance(Integer.class);
            final HashSet<Integer> strongSet = new HashSet<Integer>();
            for (int i=0; i<500; i++) {
                final Integer value = new Integer(random.nextInt(500)); // Really need new instances
                if (random.nextBoolean()) {
                    /*
                     * Tests addition.
                     */
                    final boolean   weakModified = weakSet  .add(value);
                    final boolean strongModified = strongSet.add(value);
                    if (weakModified) {
                        /*
                         * The element was not in the WeakHashSet, possibly GC collected it.
                         * Consequently that element can not be in the HashSet neither, otherwise
                         * a strong reference would exist which should have prevented the element
                         * from being removed from the WeakHashSet.
                         */
                        assertTrue("add:", strongModified);
                    } else {
                        assertTrue(value != weakSet.get(value));
                        if (strongModified) {
                            /*
                             * The element was not in HashSet but still exist in the WeakHashSet.
                             * This is because GC has not cleared it yet. Replace the reference
                             * by 'value', otherwise it may be cleared later and the 'contains'
                             * test below would fail.
                             *
                             * Note: we don't test if 'remove' below returns 'true', because GC
                             *       may have already done its work since the few previous lines!
                             */
                            weakSet.remove(value);
                            assertTrue(weakSet.add(value));
                            assertSame(value, weakSet.get(value));
                        }
                    }
                } else {
                    /*
                     * Test remove.
                     */
                    final boolean c = weakSet.contains(value);
                    if (strongSet.remove(value)) {
                        assertTrue("contains:", c);
                    }
                }
                assertTrue("containsAll:", weakSet.containsAll(strongSet));
            }
            // Do our best to lets GC finish its work.
            for (int i=0; i<4; i++) {
                Thread.sleep(50);
                System.gc();
            }
            assertEquals("equals:", strongSet, weakSet);
        }
    }

    /**
     * Tests with array elements.
     */
    @Test
    public void testArray() {
        final WeakHashSet<int[]> weakSet = WeakHashSet.newInstance(int[].class);
        final int[] array = new int[] {2, 5, 3};
        assertTrue (weakSet.add(array));
        assertFalse(weakSet.add(array));
        assertFalse(weakSet.add(array.clone()));
        assertTrue (weakSet.add(new int[] {2, 5, 4}));
        assertSame (array, weakSet.unique(array.clone()));
    }
}
