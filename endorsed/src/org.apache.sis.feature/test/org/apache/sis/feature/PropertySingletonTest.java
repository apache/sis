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

import java.util.Map;
import java.util.Set;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link PropertySingleton}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class PropertySingletonTest extends TestCase {
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
        attribute = new SingletonAttribute<>(new DefaultAttributeType<>(
                        Map.of(DefaultAttributeType.NAME_KEY, "test"),
                        Integer.class, 0, 1, null));
        singleton = new PropertySingleton<>(attribute);
    }

    /**
     * Tests an empty list.
     */
    @Test
    public void testEmpty() {
        assertEquals(0, singleton.size());
        assertTrue  (   singleton.isEmpty());
        assertEquals(0, singleton.toArray().length);
        assertFalse (singleton.iterator().hasNext());
        assertFalse (singleton.listIterator().hasNext());

        IndexOutOfBoundsException e;
        e = assertThrows(IndexOutOfBoundsException.class, () -> singleton.get(0));
        assertMessageContains(e);

        e = assertThrows(IndexOutOfBoundsException.class, () -> singleton.remove(0));
        assertMessageContains(e);
    }

    /**
     * Tests the addition, setting and removal of a single element.
     */
    @Test
    public void testSingleton() {
        final Integer a1 = 1000;
        final Integer a2 = 2000;
        assertEquals(-1, singleton.indexOf(a1));
        assertTrue  (    singleton.add(a1));
        assertEquals( 1, singleton.size());
        assertFalse (    singleton.isEmpty());
        assertEquals( 0, singleton.indexOf(a1));
        assertSame  (a1, singleton.get(0));
        assertSame  (a1, assertSingleton(singleton));
        assertSame  (a1, singleton.set(0, a2));
        assertSame  (a2, singleton.get(0));
        assertSame  (a2, assertSingleton(singleton));
        assertArrayEquals(new Object[] {a2}, singleton.toArray());
        assertSame  (a2, singleton.remove(0));
        assertEquals( 0, singleton.size());
    }

    /**
     * Ensures that we cannot add more than 1 element.
     */
    @Test
    public void testMaximumOccurrence() {
        final Integer a1 = 1000;
        final Integer a2 = 2000;
        assertTrue(singleton.add(a1));

        var e = assertThrows(IllegalStateException.class, () -> assertTrue(singleton.add(a2)),
                             "Shall not be allowed to add more than 1 element.");
        assertMessageContains(e, "test");
    }

    /**
     * Tests {@code addAll(…)} and {@code removeAll(…)} methods.
     */
    @Test
    public void testRemoveAll() {
        final Set<Integer> attributes = Set.of(1000);
        assertTrue (singleton.addAll(attributes));
        assertFalse(singleton.isEmpty());
        assertTrue (singleton.removeAll(attributes));
        assertTrue (singleton.isEmpty());
    }
}
