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

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestConfiguration;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.waitForGarbageCollection;


/**
 * Tests the {@link WeakValueHashMap}.
 * A standard {@link HashMap} object is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.util.ArraysExtTest.class)
public final strictfp class WeakValueHashMapTest extends TestCase {
    /**
     * The size of the test sets to be created.
     */
    static final int SAMPLE_SIZE = 400;

    /**
     * Number of time to retry the tests.
     */
    private static final int NUM_RETRY = 2;

    /**
     * Tests the {@link WeakValueHashMap} using strong references.
     * The tested {@code WeakValueHashMap} shall behave like a standard {@link HashMap},
     * except for element order.
     */
    @Test
    public void testStrongReferences() {
        testStrongReferences(new WeakValueHashMap<Integer,Integer>(Integer.class));
    }

    /**
     * Implementation of the {@link #testStrongReferences()} method,
     * to be reused by {@link CacheTest}.
     *
     * @param weakMap The map implementation to test.
     */
    static void testStrongReferences(final Map<Integer,Integer> weakMap) {
        final Random random = new Random();
        for (int pass=0; pass<NUM_RETRY; pass++) {
            weakMap.clear();
            final HashMap<Integer,Integer> strongMap = new HashMap<Integer,Integer>();
            for (int i=0; i<SAMPLE_SIZE; i++) {
                final Integer key   = random.nextInt(SAMPLE_SIZE);
                final Integer value = random.nextInt(SAMPLE_SIZE);
                assertEquals("containsKey:",   strongMap.containsKey(key),     weakMap.containsKey(key));
                assertEquals("containsValue:", strongMap.containsValue(value), weakMap.containsValue(value));
                assertSame  ("get:",           strongMap.get(key),             weakMap.get(key));
                if (random.nextBoolean()) {
                    // Test addition.
                    assertSame("put:", strongMap.put(key, value), weakMap.put(key, value));
                } else {
                    // Test remove
                    assertSame("remove:", strongMap.remove(key), weakMap.remove(key));
                }
                assertMapEquals(strongMap, weakMap);
            }
        }
    }

    /**
     * Tests the {@link WeakValueHashMap} using weak references.
     * In this test, we have to keep in mind than some elements
     * in {@code weakMap} may disappear at any time.
     *
     * @throws InterruptedException If the test has been interrupted.
     */
    @Test
    @DependsOnMethod("testStrongReferences")
    public void testWeakReferences() throws InterruptedException {
        testWeakReferences(new WeakValueHashMap<Integer,Integer>(Integer.class));
    }

    /**
     * Implementation of the {@link #testWeakReferences()} method,
     * to be reused by {@link CacheTest}.
     *
     * @param weakMap The map implementation to test.
     */
    static void testWeakReferences(final Map<Integer,Integer> weakMap) throws InterruptedException {
        final Random random = new Random();
        for (int pass=0; pass<NUM_RETRY; pass++) {
            weakMap.clear();
            final HashMap<Integer,Integer> strongMap = new HashMap<Integer,Integer>();
            for (int i=0; i<SAMPLE_SIZE; i++) {
                // We really want new instances here.
                final Integer key   = new Integer(random.nextInt(SAMPLE_SIZE));
                final Integer value = new Integer(random.nextInt(SAMPLE_SIZE));
                if (random.nextBoolean()) {
                    /*
                     * Tests addition.
                     */
                    final Integer   weakPrevious = weakMap  .put(key, value);
                    final Integer strongPrevious = strongMap.put(key, value);
                    if (weakPrevious == null) {
                        /*
                         * The element was not in the WeakValueHashMap, possibly GC collected it.
                         * Consequently that element can not be in the HashMap neither, otherwise
                         * a strong reference would exist which should have prevented the element
                         * from being removed from the WeakValueHashMap.
                         */
                        assertNull("put:", strongPrevious);
                    } else {
                        assertNotSame(value, weakPrevious);
                    }
                    if (strongPrevious != null) {
                        // Note: If 'strongPrevious==null', 'weakPrevious' can not
                        //       be null since GC has not collected its entry yet.
                        assertSame("put:", strongPrevious, weakPrevious);
                    }
                } else {
                    /*
                     * Tests remove.
                     */
                    final Integer   weakPrevious = weakMap.get(key);
                    final Integer strongPrevious = strongMap.remove(key);
                    if (strongPrevious != null) {
                        assertSame("remove:", strongPrevious, weakPrevious);
                    }
                }
                assertTrue("containsAll:", weakMap.entrySet().containsAll(strongMap.entrySet()));
            }
            /*
             * The test below needs the garbage collector to complete fully its job in a timely
             * manner. A failure in those tests is not necessarily a WeakValueHashMap bug, as it
             * could be caused by a heavy server load preventing GC to complete its work. If this
             * happen too often, we may turn off the "allow garbage collector dependent tests" flag.
             */
            if (TestConfiguration.allowGarbageCollectorDependentTests()) {
                waitForGarbageCollection(new Callable<Boolean>() {
                    @Override public Boolean call() {
                        return weakMap.size() == strongMap.size();
                    }
                });
                assertMapEquals(strongMap, weakMap);
                /*
                 * Clearing all strong references should make the map empty.
                 */
                strongMap.clear();
                assertTrue("Expected an empty map.", waitForGarbageCollection(new Callable<Boolean>() {
                    @Override public Boolean call() {
                        return weakMap.isEmpty();
                    }
                }));
            }
        }
    }

    /**
     * Tests with array keys.
     */
    @Test
    @DependsOnMethod("testStrongReferences")
    public void testWithArrayKeys() {
        final WeakValueHashMap<int[],Integer> weakMap = new WeakValueHashMap<int[],Integer>(int[].class);
        final int[] k1 = new int[] {2, 5, 3};
        final int[] k2 = new int[] {2, 5, 4};
        final Integer v1 = 1;
        final Integer v2 = 2;
        assertNull (    weakMap.put(k1,         v1));
        assertSame (v1, weakMap.put(k1,         v1));
        assertSame (v1, weakMap.put(k1.clone(), v1));
        assertNull (    weakMap.put(k2,         v2));
        assertSame (v2, weakMap.put(k2,         v2));
        assertSame (v1, weakMap.get(k1));
        assertSame (v2, weakMap.get(k2));
    }

    /**
     * Tests using identity comparisons. This test uses two {@link Integer} keys having the same value
     * but being different instances.
     *
     * @since 0.4
     */
    @Test
    @DependsOnMethod("testStrongReferences")
    public void testIdentityComparisons() {
        final WeakValueHashMap<Integer,Integer> weakMap = new WeakValueHashMap<Integer,Integer>(Integer.class, true);
        final Integer k1 = 10;
        final Integer k2 = 20;
        final Integer k3 = new Integer(10); // Really want a new instance.
        final Integer v1 = 1;
        final Integer v2 = 2;
        final Integer v3 = 3;
        assertEquals(k1, k3); // Necessary condition for the test to be valid.
        assertNull(weakMap.put(k1, v1));  assertSame(v1, weakMap.put(k1, v1));
        assertNull(weakMap.put(k2, v2));  assertSame(v2, weakMap.put(k2, v2));
        assertNull(weakMap.put(k3, v3));  assertSame(v3, weakMap.put(k3, v3));
        assertSame(v1, weakMap.get(k1));
        assertSame(v2, weakMap.get(k2));
        assertSame(v3, weakMap.get(k3));
    }
}
