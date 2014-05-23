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
package org.apache.sis.feature;

import java.util.Set;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link PropertySingleton}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class PropertySingletonTest extends TestCase {
    /**
     * The instance to test.
     */
    private final PropertySingleton<Integer> singleton;

    /**
     * The attribute wrapped by the {@link #singleton} list.
     */
    private final AbstractAttribute<Integer> attribute;

    /**
     * Creates a new test case.
     */
    public PropertySingletonTest() {
        attribute = new SingletonAttribute<Integer>(new DefaultAttributeType<Integer>(
                singletonMap(DefaultAttributeType.NAME_KEY, "test"), Integer.class, 0, 1, null));
        singleton = new PropertySingleton<Integer>(attribute);
    }

    /**
     * Tests an empty list.
     */
    @Test
    public void testEmpty() {
        assertEquals("size",    0, singleton.size());
        assertTrue  ("isEmpty",    singleton.isEmpty());
        assertEquals("toArray", 0, singleton.toArray().length);
        assertFalse ("iterator.hasNext", singleton.iterator().hasNext());
        assertFalse ("listIterator.hasNext", singleton.listIterator().hasNext());
        try {
            singleton.get(0);
            fail("Element 0 is not expected to exist.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        try {
            singleton.remove(0);
            fail("Element 0 is not expected to exist.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests the addition, setting and removal of a single element.
     */
    @Test
    @DependsOnMethod("testEmpty")
    public void testSingleton() {
        final Integer a1 = 1000;
        final Integer a2 = 2000;
        assertEquals("indexOf",  -1, singleton.indexOf(a1));
        assertTrue  ("add",          singleton.add(a1));
        assertEquals("size",      1, singleton.size());
        assertFalse ("isEmpty",      singleton.isEmpty());
        assertEquals("indexOf",   0, singleton.indexOf(a1));
        assertSame  ("get",      a1, singleton.get(0));
        assertSame  ("iterator", a1, getSingleton(singleton));
        assertSame  ("set",      a1, singleton.set(0, a2));
        assertSame  ("get",      a2, singleton.get(0));
        assertSame  ("iterator", a2, getSingleton(singleton));
        assertArrayEquals("toArray", new Object[] {a2}, singleton.toArray());

        assertSame  ("remove",   a2, singleton.remove(0));
        assertEquals("size",      0, singleton.size());
    }

    /**
     * Ensures that we can not add more than 1 element.
     */
    @Test
    @DependsOnMethod("testSingleton")
    public void testMaximumOccurrence() {
        final Integer a1 = 1000;
        final Integer a2 = 2000;
        assertTrue("add", singleton.add(a1));
        try {
            assertTrue("add", singleton.add(a2));
            fail("Shall not be allowed to add more than 1 element.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("test"));
        }
    }

    /**
     * Tests {@code addAll(…)} and {@code removeAll(…)} methods.
     */
    @Test
    @DependsOnMethod("testSingleton")
    public void testRemoveAll() {
        final Set<Integer> attributes = singleton(1000);
        assertTrue (singleton.addAll(attributes));
        assertFalse(singleton.isEmpty());
        assertTrue (singleton.removeAll(attributes));
        assertTrue (singleton.isEmpty());
    }
}
