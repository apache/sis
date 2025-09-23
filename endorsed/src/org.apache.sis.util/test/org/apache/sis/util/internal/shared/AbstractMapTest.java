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
package org.apache.sis.util.internal.shared;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.NoSuchElementException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link AbstractMap} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AbstractMapTest extends TestCase {
    /**
     * A dummy implementation of {@link AbstractMap} which will contain the English words
     * for numbers 1 to 4 inclusive. This implementation does not check argument validity
     * or consistency.
     */
    private static final class Count extends AbstractMap<Integer,String> {
        private final List<String> values = new ArrayList<>(List.of("one", "two", "three"));

        @Override public    void    clear   ()                    {       values.clear();}
        @Override public    String  get     (Object  k)           {return values.get(((Integer) k) - 1);}
        @Override public    String  put     (Integer k, String v) {return values.set(k - 1, v);}
        @Override protected boolean addKey  (Integer k)           {return values.add(k == 4 ? "four" : "other");}
        @Override protected boolean addValue(String  v)           {return values.add(v);}
        @Override protected EntryIterator<Integer,String> entryIterator() {
            return new EntryIterator<Integer,String>() {
                private int key;
                @Override protected boolean next()     {return ++key <= values.size();}
                @Override protected Integer getKey()   {return key;}
                @Override protected String  getValue() {return get(key);}
            };
        }
    }

    /**
     * Creates a new test case.
     */
    public AbstractMapTest() {
    }

    /**
     * Tests {@link AbstractMap#keySet()}, {@link AbstractMap#values()} and {@link AbstractMap#entrySet()}.
     * This method will also opportunistically tests basic methods like {@link AbstractMap#isEmpty()} and
     * {@link AbstractMap#containsValue(Object)}. This test does not add new values in the map.
     */
    @Test
    public void testReadOnly() {
        final Count map = new Count();
        assertEquals(3,       map.size());
        assertFalse (map.isEmpty());
        assertTrue  (map.containsKey(3));
        assertTrue  (map.containsValue("three"));
        assertFalse (map.containsValue("six"));

        final Collection<Integer> keys = map.keySet();
        assertTrue(keys.contains(3));
        assertArrayEquals(new Integer[] {1, 2, 3}, keys.toArray());

        final Collection<String> values = map.values();
        assertTrue (values.contains("three"));
        assertFalse(values.contains("six"));
        assertArrayEquals(new String[] {"one", "two", "three"}, values.toArray());

        final Collection<Map.Entry<Integer,String>> entries = map.entrySet();
        assertTrue (entries.contains(new SimpleEntry<>(2, "two")));
        assertFalse(entries.contains(new SimpleEntry<>(2, "deux")));
        assertArrayEquals(new SimpleEntry<?,?>[] {
                    new SimpleEntry<>(1, "one"),
                    new SimpleEntry<>(2, "two"),
                    new SimpleEntry<>(3, "three")
                }, entries.toArray());

        map.clear();
        assertFalse (map.containsValue("three"));
        assertTrue  (map    .isEmpty());
        assertTrue  (keys   .isEmpty());
        assertTrue  (values .isEmpty());
        assertTrue  (entries.isEmpty());
        assertEquals(0, map    .size());
        assertEquals(0, keys   .size());
        assertEquals(0, values .size());
        assertEquals(0, entries.size());
    }

    /**
     * Tests adding an element in {@link AbstractMap#keySet()}.
     * This is a non-standard feature of our {@link AbstractMap}.
     */
    @Test
    public void testAddKey() {
        final Count map = new Count();
        assertEquals(3, map.size());
        assertTrue(map.keySet().add(4));
        assertEquals(4, map.size());
        assertArrayEquals(new SimpleEntry<?,?>[] {
                    new SimpleEntry<>(1, "one"),
                    new SimpleEntry<>(2, "two"),
                    new SimpleEntry<>(3, "three"),
                    new SimpleEntry<>(4, "four")
                }, map.entrySet().toArray());
    }

    /**
     * Tests adding an element in {@link AbstractMap#values()}.
     * This is a non-standard feature of our {@link AbstractMap}.
     */
    @Test
    public void testAddValue() {
        final Count map = new Count();
        assertEquals(3, map.size());
        assertTrue(map.values().add("quatre"));
        assertEquals(4, map.size());
        assertArrayEquals(new SimpleEntry<?,?>[] {
                    new SimpleEntry<>(1, "one"),
                    new SimpleEntry<>(2, "two"),
                    new SimpleEntry<>(3, "three"),
                    new SimpleEntry<>(4, "quatre")
                }, map.entrySet().toArray());
    }

    /**
     * Tests {@link AbstractMap#equals(Object)} and {@link AbstractMap#hashCode()}.
     * We use {@link HashMap} as the reference implementation for checking hash code value.
     */
    @Test
    public void testEquals() {
        final Count map = new Count();
        final Map<Integer,String> copy = new HashMap<>(map);
        assertTrue  (copy.equals(map));
        assertTrue  (map.equals(copy));
        assertEquals(copy.hashCode(), map.hashCode());

        // Make a change and test again.
        assertEquals("two", map.put(2, "deux"));
        assertFalse(copy.equals(map));
        assertFalse(map.equals(copy));
        assertNotEquals(copy.hashCode(), map.hashCode());
    }

    /**
     * Tests exceptions during iteration.
     */
    @Test
    public void testIterationException() {
        final Iterator<Integer> it = new Count().keySet().iterator();
        RuntimeException e;

        e = assertThrows(IllegalStateException.class, () -> it.remove(),
                         "Should not be allowed to invoke Iterator.remove() before next().");
        assertNotNull(e);

        // Iterating without invoking Iterator.hasNext() should work anyway.
        for (int i=1; i<=3; i++) {
            assertEquals(Integer.valueOf(i), it.next());
            e = assertThrows(UnsupportedOperationException.class, () -> it.remove());
            assertNotNull(e);
        }
        e = assertThrows(NoSuchElementException.class, () -> it.next(), "Expected end of iteration.");
        assertNotNull(e);

        assertFalse(it.hasNext());
    }
}
