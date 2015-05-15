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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.NoSuchElementException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link AbstractMap} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class AbstractMapTest extends TestCase {
    /**
     * A dummy implementation of {@link AbstractMap} which will contain the English words
     * for numbers 1 to 4 inclusive. This implementation does not check argument validity
     * or consistency.
     */
    private static final class Count extends AbstractMap<Integer,String> {
        private final List<String> values = new ArrayList<String>(Arrays.asList("one", "two", "three"));

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
     * Tests {@link AbstractMap#keySet()}, {@link AbstractMap#values()} and {@link AbstractMap#entrySet()}.
     * This method will also opportunistically tests basic methods like {@link AbstractMap#isEmpty()} and
     * {@link AbstractMap#containsValue(Object)}. This test does not add new values in the map.
     */
    @Test
    public void testReadOnly() {
        final Count map = new Count();
        assertEquals("size", 3,       map.size());
        assertFalse ("isEmpty",       map.isEmpty());
        assertTrue  ("containsKey",   map.containsKey(3));
        assertTrue  ("containsValue", map.containsValue("three"));
        assertFalse ("containsValue", map.containsValue("six"));

        final Collection<Integer> keys = map.keySet();
        assertTrue("contains", keys.contains(3));
        assertArrayEquals("keySet", new Integer[] {1, 2, 3}, keys.toArray());

        final Collection<String> values = map.values();
        assertTrue ("contains", values.contains("three"));
        assertFalse("contains", values.contains("six"));
        assertArrayEquals("values", new String[] {"one", "two", "three"}, values.toArray());

        final Collection<Map.Entry<Integer,String>> entries = map.entrySet();
        assertTrue ("contains", entries.contains(new SimpleEntry<Integer,String>(2, "two")));
        assertFalse("contains", entries.contains(new SimpleEntry<Integer,String>(2, "deux")));
        assertArrayEquals("entrySet", new SimpleEntry<?,?>[] {
                    new SimpleEntry<Integer,String>(1, "one"),
                    new SimpleEntry<Integer,String>(2, "two"),
                    new SimpleEntry<Integer,String>(3, "three")
                }, entries.toArray());

        map.clear();
        assertFalse ("containsValue", map.containsValue("three"));
        assertTrue  ("isEmpty", map    .isEmpty());
        assertTrue  ("isEmpty", keys   .isEmpty());
        assertTrue  ("isEmpty", values .isEmpty());
        assertTrue  ("isEmpty", entries.isEmpty());
        assertEquals("size", 0, map    .size());
        assertEquals("size", 0, keys   .size());
        assertEquals("size", 0, values .size());
        assertEquals("size", 0, entries.size());
    }

    /**
     * Tests adding an element in {@link AbstractMap#keySet()}.
     * This is a non-standard feature of our {@link AbstractMap}.
     */
    @Test
    @DependsOnMethod("testReadOnly")
    public void testAddKey() {
        final Count map = new Count();
        assertEquals("size", 3, map.size());
        assertTrue(map.keySet().add(4));
        assertEquals("size", 4, map.size());
        assertArrayEquals("entrySet", new SimpleEntry<?,?>[] {
                    new SimpleEntry<Integer,String>(1, "one"),
                    new SimpleEntry<Integer,String>(2, "two"),
                    new SimpleEntry<Integer,String>(3, "three"),
                    new SimpleEntry<Integer,String>(4, "four")
                }, map.entrySet().toArray());
    }

    /**
     * Tests adding an element in {@link AbstractMap#values()}.
     * This is a non-standard feature of our {@link AbstractMap}.
     */
    @Test
    @DependsOnMethod("testReadOnly")
    public void testAddValue() {
        final Count map = new Count();
        assertEquals("size", 3, map.size());
        assertTrue(map.values().add("quatre"));
        assertEquals("size", 4, map.size());
        assertArrayEquals("entrySet", new SimpleEntry<?,?>[] {
                    new SimpleEntry<Integer,String>(1, "one"),
                    new SimpleEntry<Integer,String>(2, "two"),
                    new SimpleEntry<Integer,String>(3, "three"),
                    new SimpleEntry<Integer,String>(4, "quatre")
                }, map.entrySet().toArray());
    }

    /**
     * Tests {@link AbstractMap#equals(Object)} and {@link AbstractMap#hashCode()}.
     * We use {@link HashMap} as the reference implementation for checking hash code value.
     */
    @Test
    public void testEquals() {
        final Count map = new Count();
        final Map<Integer,String> copy = new HashMap<Integer,String>(map);
        assertTrue  ("equals",   copy.equals(map));
        assertTrue  ("equals",   map.equals(copy));
        assertEquals("hashCode", copy.hashCode(), map.hashCode());

        // Make a change and test again.
        assertEquals("put", "two", map.put(2, "deux"));
        assertFalse("equals",   copy.equals(map));
        assertFalse("equals",   map.equals(copy));
        assertFalse("hashCode", copy.hashCode() == map.hashCode());
    }

    /**
     * Tests exceptions during iteration.
     */
    @Test
    public void testIterationException() {
        final Iterator<Integer> it = new Count().keySet().iterator();
        try {
            it.remove();
            fail("Should not be allowed to invoke Iterator.remove() before next().");
        } catch (IllegalStateException e) {
            // This is the expected exception.
        }
        // Iterating without invoking Iterator.hasNext() should work anyway.
        for (int i=1; i<=3; i++) {
            assertEquals(Integer.valueOf(i), it.next());
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException e) {
                // This is the expected exception.
            }
        }
        try {
            it.next();
            fail("Expected end of iteration.");
        } catch (NoSuchElementException e) {
            // This is the expected exception.
        }
        assertFalse("Expected end of iteration.", it.hasNext());
    }
}
