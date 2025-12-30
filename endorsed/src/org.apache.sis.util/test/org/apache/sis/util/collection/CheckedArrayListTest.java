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

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link CheckedArrayList} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CheckedArrayListTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CheckedArrayListTest() {
    }

    /**
     * Tests {@link CheckedArrayList#add(Object)}.
     */
    @Test
    public void testAdd() {
        final var list = new CheckedArrayList<>(String.class);
        assertTrue(list.add("One"));
        assertTrue(list.add("Two"));
        assertTrue(list.add("Three"));
        assertEquals(List.of("One", "Two", "Three"), list);
    }

    /**
     * Tests {@link CheckedArrayList#addAll(Collection)}.
     */
    @Test
    public void testAddAll() {
        final var list = new CheckedArrayList<>(String.class);
        assertTrue(list.add("One"));
        assertTrue(Collections.addAll(list, "Two", "Three"));
        assertEquals(List.of("One", "Two", "Three"), list);
    }

    /**
     * Ensures that we cannot add null elements.
     */
    @Test
    public void testAddNull() {
        final var list = new CheckedArrayList<>(String.class);
        var e = assertThrows(NullPointerException.class, () -> list.add(null));
        assertMessageContains(e, "CheckedArrayList<String>");
    }

    /**
     * Ensures that we cannot add null elements.
     */
    @Test
    public void testAddAllNull() {
        final var list = new CheckedArrayList<>(String.class);
        final var toAdd = Arrays.asList("One", null, "Three");
        var e = assertThrows(NullPointerException.class, () -> list.addAll(toAdd));
        assertMessageContains(e, "CheckedArrayList<String>");
    }

    /**
     * Ensures that we cannot element of the wrong type.
     */
    @Test
    public void testAddWrongType() {
        final var list = new CheckedArrayList<>(String.class);
        final String message = testAddWrongType(list);
        assertTrue(message.contains("element"));
        assertTrue(message.contains("Integer"));
        assertTrue(message.contains("String"));
    }

    /**
     * Implementation of {@link #testAddWrongType()}, also shared by {@link #testAddWrongTypeToSublist()}.
     * Returns the exception message.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static String testAddWrongType(final List list) {
        var e = assertThrows(ClassCastException.class, () -> list.add(Integer.valueOf(4)),
                             "Shall not be allowed to add an integer to the list.");
        return e.getMessage();
    }

    /**
     * Ensures that we cannot element of the wrong type in a sublist.
     */
    @Test
    public void testAddWrongTypeToSublist() {
        final var list = new CheckedArrayList<>(String.class);
        assertTrue(list.add("One"));
        assertTrue(list.add("Two"));
        assertTrue(list.add("Three"));
        testAddWrongType(list.subList(1, 3));
        // Exception message is JDK-dependent, so we cannot test it.
    }
}
