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
import java.util.Map;
import java.util.HashMap;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link SingletonValue}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultAttributeTest.class)
public final strictfp class SingletonValueTest extends TestCase {
    /**
     * The key used for storing value in this test class.
     */
    private static final String KEY = "test key";

    /**
     * The instance to test.
     */
    private final SingletonValue singleton;

    /**
     * The type of the attribute value in the {@link #singleton} list.
     */
    private final DefaultAttributeType<Integer> attributeType;

    /**
     * The map of properties given to the {@link #singleton} instance to test.
     */
    private final Map<String, Object> properties;

    /**
     * Arbitrary values added to {@link #properties} for making sure
     */
    private final Map<String,?> otherValues;

    /**
     * Creates a new test case.
     */
    public SingletonValueTest() {
        otherValues   = singletonMap("other key", "other value");
        properties    = new HashMap<>(otherValues);
        attributeType = new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, KEY),
                                Integer.class, null, NumberRange.create(0, true, 1, true));
        singleton = new SingletonValue(attributeType, properties, KEY);
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
        assertEquals("Other values shall be unmodified.", otherValues, properties);
    }

    /**
     * Tests the addition, setting and removal of a single element.
     */
    @Test
    @DependsOnMethod("testEmpty")
    public void testSingleton() {
        final DefaultAttribute<Integer> a1 = new DefaultAttribute<>(attributeType);
        final DefaultAttribute<Integer> a2 = new DefaultAttribute<>(attributeType);
        a1.setValue(1000);
        a2.setValue(2000);
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
        assertArrayEquals("toArray", new DefaultAttribute<?>[] {a2}, singleton.toArray());

        assertSame  ("remove",   a2, singleton.remove(0));
        assertEquals("size",      0, singleton.size());
        assertEquals("Other values shall be unmodified.", otherValues, properties);
    }

    /**
     * Ensures that we can not add more than 1 element.
     */
    @Test
    @DependsOnMethod("testSingleton")
    public void testMaximumOccurrence() {
        final DefaultAttribute<Integer> a1 = new DefaultAttribute<>(attributeType);
        final DefaultAttribute<Integer> a2 = new DefaultAttribute<>(attributeType);
        assertTrue("add", singleton.add(a1));
        try {
            assertTrue("add", singleton.add(a2));
            fail("Shall not be allowed to add more than 1 element.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(KEY));
        }
    }

    /**
     * Tests {@code addAll(…)} and {@code removeAll(…)} methods.
     */
    @Test
    @DependsOnMethod("testSingleton")
    public void testRemoveAll() {
        final Set<DefaultAttribute<Integer>> attributes = singleton(new DefaultAttribute<>(attributeType));
        assertTrue (singleton.addAll(attributes));
        assertFalse(singleton.isEmpty());
        assertTrue (singleton.removeAll(attributes));
        assertTrue (singleton.isEmpty());
    }

    /**
     * Tests the attempt to add an attribute of the wrong type.
     * {@link SingletonValue} shall not allow this operation.
     */
    @Test
    @DependsOnMethod("testSingleton")
    public void testAddWrongType() {
        final DefaultAttribute<Integer> a1 = DefaultAttributeTest.population();
        try {
            singleton.add(a1);
        } catch (RuntimeException e) { // TODO: IllegalAttributeException after GeoAPI review.
            final String message = e.getMessage();
            assertTrue(message, message.contains(KEY));
        }
    }
}
