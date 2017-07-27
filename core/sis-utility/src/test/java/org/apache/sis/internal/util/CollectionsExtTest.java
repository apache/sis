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
package org.apache.sis.internal.util;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Predicate;


/**
 * Tests the {@link CollectionsExt} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public final strictfp class CollectionsExtTest extends TestCase {
    /**
     * Tests {@link CollectionsExt#nonNullArraySet(String, Object, Object[])}.
     */
    @Test
    public void testNonNullArraySet() {
        final String name = "test";
        final String[] emptyArray = new String[0];
        assertSame(emptyArray,
                CollectionsExt.nonNullArraySet(name, null, emptyArray));
        assertSame(emptyArray,
                CollectionsExt.nonNullArraySet(name, emptyArray, emptyArray));
        assertArrayEquals(new String[] {"A"},
                CollectionsExt.nonNullArraySet(name, "A", emptyArray));
        assertArrayEquals(new String[] {"A"},
                CollectionsExt.nonNullArraySet(name, new String[] {"A", null, "A"}, emptyArray));
        assertArrayEquals(new String[] {"B", "A", "C"},
                CollectionsExt.nonNullArraySet(name, new String[] {"B", "A", "B", "C", null, "A"}, emptyArray));
        /*
         * Verify that an exception is thrown in case of illegal value type.  Note that the Object[] type
         * could be accepted if all elements are String instances, however the current method contract is
         * to not accept them, so we will ensure that.
         */
        try {
            CollectionsExt.nonNullArraySet(name, new Object[] {"A"}, emptyArray);
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(name));
            assertTrue(message, message.contains("Object[]"));
        }
    }

    /**
     * Tests {@link CollectionsExt#createSetForType(Class, int)}.
     */
    @Test
    public void testCreateSetForType() {
        Set<?> set = CollectionsExt.createSetForType(java.lang.annotation.ElementType.class, 0);
        assertTrue("isEmpty", set.isEmpty());
        assertInstanceOf("Set<ElementType>", EnumSet.class, set);

        set = CollectionsExt.createSetForType(org.opengis.referencing.cs.AxisDirection.class, 0);
        assertTrue("isEmpty", set.isEmpty());
        assertInstanceOf("Set<AxisDirection>", CodeListSet.class, set);

        set = CollectionsExt.createSetForType(String.class, 0);
        assertTrue("isEmpty", set.isEmpty());
        assertInstanceOf("Set<String>", HashSet.class, set);
    }

    /**
     * Tests {@link CollectionsExt#addToMultiValuesMap(Map, Object, Object)}, then
     * opportunistically tests {@link CollectionsExt#removeFromMultiValuesMap(Map, Object, Object)},
     */
    @Test
    public void testAddAndRemoveToMultiValuesMap() {
        final Map<String, List<Integer>> map = new LinkedHashMap<>();
        final Integer A1 = 2;
        final Integer A2 = 4;
        final Integer B1 = 3;
        final Integer B2 = 6;
        final Integer B3 = 9;
        assertArrayEquals(new Integer[] {A1},         CollectionsExt.addToMultiValuesMap(map, "A", A1).toArray());
        assertArrayEquals(new Integer[] {B1},         CollectionsExt.addToMultiValuesMap(map, "B", B1).toArray());
        assertArrayEquals(new Integer[] {B1, B2},     CollectionsExt.addToMultiValuesMap(map, "B", B2).toArray());
        assertArrayEquals(new Integer[] {A1, A2},     CollectionsExt.addToMultiValuesMap(map, "A", A2).toArray());
        assertArrayEquals(new Integer[] {B1, B2, B3}, CollectionsExt.addToMultiValuesMap(map, "B", B3).toArray());
        assertArrayEquals(new String[]  {"A", "B"},   map.keySet().toArray());
        assertArrayEquals(new Integer[] {A1, A2},     map.get("A").toArray());
        assertArrayEquals(new Integer[] {B1, B2, B3}, map.get("B").toArray());

        assertNull(                                   CollectionsExt.removeFromMultiValuesMap(map, "C", A2));
        assertArrayEquals(new Integer[] {A1},         CollectionsExt.removeFromMultiValuesMap(map, "A", A2).toArray());
        assertArrayEquals(new Integer[] {B1, B3},     CollectionsExt.removeFromMultiValuesMap(map, "B", B2).toArray());
        assertArrayEquals(new Integer[] {},           CollectionsExt.removeFromMultiValuesMap(map, "A", A1).toArray());
        assertArrayEquals(new String[]  {"B"},        map.keySet().toArray());
        assertArrayEquals(new Integer[] {B1, B3},     map.get("B").toArray());
    }

    /**
     * Tests {@link CollectionsExt#toCaseInsensitiveNameMap(Collection, Locale)}.
     */
    @Test
    public void testToCaseInsensitiveNameMap() {
        final List<Map.Entry<String,String>> elements = new ArrayList<>();
        elements.add(new AbstractMap.SimpleEntry<>("AA", "AA"));
        elements.add(new AbstractMap.SimpleEntry<>("Aa", "Aa"));
        elements.add(new AbstractMap.SimpleEntry<>("BB", "BB"));
        elements.add(new AbstractMap.SimpleEntry<>("bb", "bb"));
        elements.add(new AbstractMap.SimpleEntry<>("CC", "CC"));

        final Map<String,String> expected = new HashMap<>();
        assertNull(expected.put("AA", "AA"));
        assertNull(expected.put("Aa", "Aa"));   // No mapping for "aa", because of ambiguity between "AA" and "Aa".
        assertNull(expected.put("BB", "BB"));
        assertNull(expected.put("bb", "bb"));
        assertNull(expected.put("CC", "CC"));
        assertNull(expected.put("cc", "CC"));   // Automatically added.

        for (int i=0; i<10; i++) {
            Collections.shuffle(elements);
            assertMapEquals(expected, CollectionsExt.toCaseInsensitiveNameMap(elements, Locale.ROOT));
        }
    }

    /**
     * Tests {@link CollectionsExt#filter(Iterator, Predicate)}.
     */
    @Test
    public void testFilter() {
        final Iterator<Integer> it = CollectionsExt.filter(Arrays.asList(2, 5, 7, 4, 8).iterator(), new Predicate<Integer>() {
            @Override public boolean test(Integer n) {
                return (n & 1) == 0;
            }
        });
        assertTrue  (   it.hasNext());
        assertEquals(2, it.next().intValue());
        assertEquals(4, it.next().intValue());
        assertTrue  (   it.hasNext());
        assertTrue  (   it.hasNext());
        assertEquals(8, it.next().intValue());
        assertFalse (   it.hasNext());
    }

    /**
     * Tests {@link CollectionsExt#identityEquals(Iterator, Iterator)}.
     */
    @Test
    public void testIdentityEquals() {
        final List<String> c1 = Arrays.asList("A", "B", "C");
        final List<String> c2 = Arrays.asList("A", "B");
        assertFalse(CollectionsExt.identityEquals(c1.iterator(), c2.iterator()));
        assertFalse(CollectionsExt.identityEquals(c2.iterator(), c1.iterator()));
        assertTrue(CollectionsExt.identityEquals(c1.iterator(), Arrays.asList("A", "B", "C").iterator()));
    }

    /**
     * Tests {@link CollectionsExt#toArray(Collection, Class)}.
     *
     * @since 0.6
     */
    @Test
    public void testToArray() {
        final String[] expected = new String[] {"One", "Two", "Three"};
        final String[] actual = CollectionsExt.toArray(Arrays.asList(expected), String.class);
        assertArrayEquals(expected, actual);
    }
}
