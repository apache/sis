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

import java.util.Arrays;
import java.util.Collections;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link FrequencySortedSet} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class FrequencySortedSetTest extends TestCase {
    /**
     * A simple case with only two elements, the first one being omitted.
     */
    @Test
    public void testSimple() {
        boolean reverse = false;
        do {
            final FrequencySortedSet<Integer> set = new FrequencySortedSet<>(reverse);
            assertFalse(set.add(12, 0));
            assertTrue (set.add(18, 11));
            assertEquals(Collections.singleton(18), set);
            assertArrayEquals(new int[] {11}, set.frequencies());
        } while ((reverse = !reverse) == true);
    }

    /**
     * Simple test with 2 elements.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testTwoElements() {
        final FrequencySortedSet<Integer> set = new FrequencySortedSet<>(true);
        for (int i=0; i<10; i++) {
            if ((i % 3) == 0) {
                set.add(11);
            }
            set.add(9);
        }
        assertEquals(2, set.size());
        assertEquals(Integer.valueOf(9), set.first());
        assertEquals(Integer.valueOf(11), set.last());
        assertArrayEquals(new int[] {10, 4}, set.frequencies());
    }

    /**
     * Tests creation of various subsets.
     */
    @Test
    @DependsOnMethod("testTwoElements")
    public void testSubSet() {
        final FrequencySortedSet<Integer> set = new FrequencySortedSet<>();
        set.addAll(Arrays.asList(2, 5, 3, 2, 4, 2, 3, 6, 2));
        assertArrayEquals(new Integer[] {5, 4, 6, 3, 2}, set.toArray());
        assertArrayEquals(new int[] {1, 1, 1, 2, 4}, set.frequencies());

        assertArrayEquals("Expected all elements occurring less often than 2.",
                          new Integer[] {5, 4, 6, 3}, set.headSet(2).toArray());

        assertArrayEquals("Expected all elements occurring less often than 3.",
                          new Integer[] {5, 4, 6}, set.headSet(3).toArray());

        assertArrayEquals("Expected all elements occurring at least as often than 3.",
                          new Integer[] {3, 2}, set.tailSet(3).toArray());

        assertArrayEquals("Expected all elements occurring at least as often than 3 but less than 2.",
                          new Integer[] {3}, set.subSet(3, 2).toArray());

        assertTrue(set.subSet(2, 3).isEmpty());
    }
}
