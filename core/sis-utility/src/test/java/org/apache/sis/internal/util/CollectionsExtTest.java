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
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Related to JDK8
import org.apache.sis.internal.jdk8.Function;


/**
 * Tests the {@link CollectionsExt} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
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
     * Tests {@link CollectionsExt#addToMultiValuesMap(Map, Object, Object)}.
     */
    @Test
    public void testAddToMultiValuesMap() {
        final Map<String, List<Integer>> map = new LinkedHashMap<String, List<Integer>>();
        final Integer A1 = 2;
        final Integer A2 = 4;
        final Integer B1 = 3;
        final Integer B2 = 6;
        final Integer B3 = 9;
        assertArrayEquals(new Integer[] {A1},
                CollectionsExt.addToMultiValuesMap(map, "A", A1).toArray());
        assertArrayEquals(new Integer[] {B1},
                CollectionsExt.addToMultiValuesMap(map, "B", B1).toArray());
        assertArrayEquals(new Integer[] {B1, B2},
                CollectionsExt.addToMultiValuesMap(map, "B", B2).toArray());
        assertArrayEquals(new Integer[] {A1, A2},
                CollectionsExt.addToMultiValuesMap(map, "A", A2).toArray());
        assertArrayEquals(new Integer[] {B1, B2, B3},
                CollectionsExt.addToMultiValuesMap(map, "B", B3).toArray());
        assertArrayEquals(new String[] {"A", "B"}, map.keySet().toArray());
    }

    /**
     * Tests {@link CollectionsExt#toCaseInsensitiveNameMap(Collection, Function, Locale)}.
     */
    @Test
    public void testToCaseInsensitiveNameMap() {
        final Function<String,String> nameFunction = ObjectConverters.identity(String.class);
        final Map<String,String> expected = new HashMap<String,String>();
        assertNull(expected.put("AA", "AA"));
        assertNull(expected.put("Aa", "Aa")); // No mapping for "aa", because of ambiguity between "AA" and "Aa".
        assertNull(expected.put("BB", "BB"));
        assertNull(expected.put("bb", "bb"));
        assertNull(expected.put("CC", "CC"));
        assertNull(expected.put("cc", "CC")); // Automatically added.

        final List<String> elements = Arrays.asList("AA", "Aa", "BB", "bb", "CC");
        for (int i=0; i<10; i++) {
            Collections.shuffle(elements);
            assertMapEquals(expected, CollectionsExt.toCaseInsensitiveNameMap(elements, nameFunction, Locale.ROOT));
        }
    }
}
