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
import java.util.Collection;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link CheckedArrayList} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
public final strictfp class CheckedArrayListTest extends TestCase {
    /**
     * Tests {@link CheckedArrayList#add(Object)}.
     */
    @Test
    public void testAdd() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        assertTrue(list.add("One"));
        assertTrue(list.add("Two"));
        assertTrue(list.add("Three"));
        assertEquals(Arrays.asList("One", "Two", "Three"), list);
    }

    /**
     * Tests {@link CheckedArrayList#addAll(Collection)}.
     */
    @Test
    public void testAddAll() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        assertTrue(list.add("One"));
        assertTrue(list.addAll(Arrays.asList("Two", "Three")));
        assertEquals(Arrays.asList("One", "Two", "Three"), list);
    }

    /**
     * Ensures that we can not add null elements.
     */
    @Test
    public void testAddNull() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        try {
            list.add(null);
        } catch (NullArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message.contains("CheckedArrayList<String>"));
        }
    }

    /**
     * Ensures that we can not add null elements.
     */
    @Test
    public void testAddAllNull() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        final Collection<String> toAdd = Arrays.asList("One", null, "Three");
        try {
            list.addAll(toAdd);
        } catch (NullArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message.contains("CheckedArrayList<String>"));
        }
    }

    /**
     * Ensures that we can not element of the wrong type.
     */
    @Test
    public void testAddWrongType() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        final String message = testAddWrongType(list);
        assertTrue("element", message.contains("element"));
        assertTrue("Integer", message.contains("Integer"));
        assertTrue("String",  message.contains("String"));
    }

    /**
     * Implementation of {@link #testAddWrongType()}, also shared by {@link #testAddWrongTypeToSublist()}.
     * Returns the exception message.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static String testAddWrongType(final List list) {
        try {
            list.add(Integer.valueOf(4));
            fail("Shall not be allowed to add an integer to the list.");
            return null;
        } catch (ClassCastException e) {
            return e.getMessage();
        }
    }

    /**
     * Ensures that we can not element of the wrong type in a sublist.
     */
    @Test
    @DependsOnMethod("testAddWrongType")
    public void testAddWrongTypeToSublist() {
        final CheckedArrayList<String> list = new CheckedArrayList<String>(String.class);
        assertTrue(list.add("One"));
        assertTrue(list.add("Two"));
        assertTrue(list.add("Three"));
        testAddWrongType(list.subList(1, 3));
        // Exception message is JDK-dependent, so we can not test it.
    }

    /**
     * Tests {@link CheckedArrayList#castOrCopy(Collection, Class)}.
     */
    @Test
    @DependsOnMethod("testAddAll")
    public void testCastOrCopy() {
        assertNull(CheckedArrayList.castOrCopy(null, String.class));
        final List<String> fruits = Arrays.asList("Apple", "Orange", "Raisin");
        final CheckedArrayList<String> asStrings = CheckedArrayList.castOrCopy(fruits, String.class);
        assertEquals ("Should have the given element type.", String.class, asStrings.getElementType());
        assertNotSame("Should have created a new instance.", fruits, asStrings);
        assertEquals ("Should contain the same data.",       fruits, asStrings);
        assertSame   ("Should cast existing instance.", asStrings, CheckedArrayList.castOrCopy(asStrings, String.class));

        final CheckedArrayList<CharSequence> asChars = CheckedArrayList.castOrCopy(asStrings, CharSequence.class);
        assertEquals ("Should have the given element type.", CharSequence.class, asChars.getElementType());
        assertNotSame("Should have created a new instance.", asStrings, asChars);
        assertEquals ("Should contain the same data.",       asStrings, asChars);
        assertEquals ("Should contain the same data.",       fruits,    asChars);

        try {
            CheckedArrayList.castOrCopy(asChars, Integer.class);
            fail("Should not be allowed to cast String to Integer.");
        } catch (ClassCastException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("String"));
            assertTrue(message, message.contains("Integer"));
        }
    }
}
