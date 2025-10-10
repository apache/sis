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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests the {@link WeakHashSet}.
 * A standard {@link HashSet} object is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class WeakHashSetTest extends TestCaseWithGC {
    /**
     * The size of the test sets to be created.
     */
    private static final int SAMPLE_SIZE = 500;

    /**
     * Number of time to retry the tests.
     */
    private static final int NUM_RETRY = 4;

    /**
     * Creates a new test case.
     */
    public WeakHashSetTest() {
    }

    /**
     * Tests the {@link WeakHashSet} using strong references.
     * The tested {@code WeakHashSet} shall behave like a standard {@link HashSet},
     * except for element order.
     */
    @Test
    public void testStrongReferences() {
        final var random = new Random();
        for (int pass=0; pass<NUM_RETRY; pass++) {
            final var weakSet = new WeakHashSet<IntObject>(IntObject.class);
            final var strongSet = new HashSet<IntObject>();
            for (int i=0; i<SAMPLE_SIZE; i++) {
                final var value = new IntObject(random.nextInt(SAMPLE_SIZE));
                if (random.nextBoolean()) {
                    /*
                     * Tests addition.
                     */
                    final boolean   weakModified = weakSet  .add(value);
                    final boolean strongModified = strongSet.add(value);
                    assertEquals(strongModified, weakModified, "add:");
                    if (strongModified) {
                        assertSame(value, weakSet.get(value), "get:");
                    } else {
                        assertEquals(value, weakSet.get(value), "get:");
                    }
                } else {
                    /*
                     * Tests remove
                     */
                    final boolean   weakModified = weakSet  .remove(value);
                    final boolean strongModified = strongSet.remove(value);
                    assertEquals(strongModified, weakModified, "remove:");
                    assertNull(weakSet.get(value), "get:");
                }
                assertEquals(strongSet.contains(value), weakSet.contains(value), "contains:");
                assertEquals(strongSet, weakSet, "equals:");
            }
            assertSetEquals(strongSet, weakSet);
        }
    }

    /**
     * Tests the {@link WeakHashSet} using weak references. In this test, we have to keep
     * in mind that some elements in {@code weakSet} may disappear at any time!
     *
     * @throws InterruptedException if the test has been interrupted.
     */
    @Test
    public void testWeakReferences() throws InterruptedException {
        final var random = new Random();
        for (int pass=0; pass<NUM_RETRY; pass++) {
            final var weakSet = new WeakHashSet<IntObject>(IntObject.class);
            final var strongSet = new HashSet<IntObject>();
            for (int i=0; i<SAMPLE_SIZE; i++) {
                final IntObject value = new IntObject(random.nextInt(SAMPLE_SIZE));     // Really need new instances.
                if (random.nextBoolean()) {
                    /*
                     * Tests addition.
                     */
                    final boolean   weakModified = weakSet  .add(value);
                    final boolean strongModified = strongSet.add(value);
                    if (weakModified) {
                        /*
                         * The element was not in the WeakHashSet, possibly GC collected it.
                         * Consequently, that element cannot be in the HashSet neither, otherwise
                         * a strong reference would exist which should have prevented the element
                         * from being removed from the WeakHashSet.
                         */
                        assertTrue(strongModified, "add:");
                    } else {
                        assertNotSame(value, weakSet.get(value));
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
                        assertTrue(c, "contains:");
                    }
                }
                assertTrue(weakSet.containsAll(strongSet), "containsAll:");
            }
            /*
             * The test below needs the garbage collector to complete fully its job in a timely manner.
             * A failure in those tests is not necessarily a WeakValueHashMap bug, as it could be caused
             * by a heavy server load preventing GC to complete its work. If this happen too often,
             * we may turn off the "allow garbage collector dependent tests" flag.
             */
            if (GC_DEPENDENT_TESTS_ENABLED) {
                waitForGarbageCollection(() -> weakSet.size() == strongSet.size());
                assertSetEquals(strongSet, weakSet);
                /*
                 * Clearing all strong references should make the set empty.
                 */
                strongSet.clear();
                assertTrue(waitForGarbageCollection(weakSet::isEmpty), "Expected an empty set.");
            }
        }
    }

    /**
     * Tests with array elements.
     */
    @Test
    public void testWithArrayElements() {
        final var weakSet = new WeakHashSet<int[]>(int[].class);
        final int[] array = new int[] {2, 5, 3};
        assertTrue (weakSet.add(array));
        assertFalse(weakSet.add(array));
        assertFalse(weakSet.add(array.clone()));
        assertTrue (weakSet.add(new int[] {2, 5, 4}));
        assertSame (array, weakSet.unique(array.clone()));
    }
}
