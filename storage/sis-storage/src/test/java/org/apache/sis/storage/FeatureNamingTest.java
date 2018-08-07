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
package org.apache.sis.storage;

import org.opengis.util.GenericName;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.junit.Test;

import static org.junit.Assert.*;
import org.opengis.util.LocalName;


/**
 * Tests {@link FeatureNaming}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class FeatureNamingTest extends TestCase {
    /**
     * Name for the tests.
     */
    private final GenericName A, B, otherA;

    /**
     * Creates a new test case.
     */
    public FeatureNamingTest() {
        A      = Names.parseGenericName(null, null, "myNS:A");
        B      = Names.createLocalName (null, null, "B");
        otherA = Names.parseGenericName(null, null, "other:A");
    }

    /**
     * Tests {@link FeatureNaming#add(DataStore, GenericName, Object)} followed by
     * {@link FeatureNaming#get(DataStore, String)} in a simple case where there is no ambiguity.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    public void testSimple() throws IllegalNameException {
        final DataStoreMock store = new DataStoreMock("testDataStore");
        final FeatureNaming<Integer> map = new FeatureNaming<>();
        map.add(store, A, 1);
        map.add(store, B, 2);
        assertEquals("A", Integer.valueOf(1), map.get(store, "myNS:A"));
        assertEquals("A", Integer.valueOf(1), map.get(store, "A"));
        assertEquals("B", Integer.valueOf(2), map.get(store, "B"));
        /*
         * Above code tested normal usage. Now test error conditions.
         * First, searching a non-existent entry should raise an exception.
         */
        try {
            map.get(store, "C");
            fail("Should not find a non-existent entry.");
        } catch (IllegalNameException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("C"));
            assertTrue(message, message.contains("testDataStore"));
        }
        /*
         * Attempt to overwrite an existing entry should raise an exception
         * without modifying the existing value.
         */
        try {
            map.add(store, B, 3);
            fail("Should not overwrite an existing entry.");
        } catch (IllegalNameException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("B"));
        }
        assertEquals("Existing value should not have been modified.",
                     Integer.valueOf(2), map.get(store, "B"));
    }

    /**
     * Same test than {@link #testSimple()}, with the addition of an ambiguous name.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testAmbiguity() throws IllegalNameException {
        final DataStoreMock store = new DataStoreMock("testDataStore");
        final FeatureNaming<Integer> map = new FeatureNaming<>();
        map.add(store, A, 1);
        map.add(store, B, 2);
        map.add(store, otherA, 3);
        assertEquals("A",      Integer.valueOf(1), map.get(store, "myNS:A"));
        assertEquals("B",      Integer.valueOf(2), map.get(store, "B"));
        assertEquals("otherA", Integer.valueOf(3), map.get(store, "other:A"));
        /*
         * Attempt to query using only the "A" value was used to succeed in 'testSimple()' but
         * should now fail because this shortcut could apply to "other:A" as well as "myNS:A".
         */
        try {
            map.get(store, "A");
            fail("Should not find an ambiguous entry.");
        } catch (IllegalNameException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("myNS:A"));
            assertTrue(message, message.contains("other:A"));
            assertTrue(message, message.contains("testDataStore"));
        }
    }

    /**
     * Tests two names having the same tip, but where only one of the two names have a namespace.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testAmbiguity")
    public void testQualifiedAndUnqualifiedName() throws IllegalNameException {
        final DataStoreMock store = new DataStoreMock("testDataStore");
        final LocalName local = Names.createLocalName(null, null, "A");
        FeatureNaming<Integer> map = new FeatureNaming<>();
        map.add(store, local,  3);
        map.add(store, A,      2);
        map.add(store, B,      5);
        map.add(store, otherA, 7);
        assertEquals("B",       Integer.valueOf(5), map.get(store, "B"));
        assertEquals("A",       Integer.valueOf(3), map.get(store, "A"));
        assertEquals("myNS:A",  Integer.valueOf(2), map.get(store, "myNS:A"));
        assertEquals("other:A", Integer.valueOf(7), map.get(store, "other:A"));
        /*
         * Same tests, but with elements added in different order.
         */
        map = new FeatureNaming<>();
        map.add(store, otherA, 7);
        map.add(store, B,      5);
        map.add(store, A,      2);
        map.add(store, local,  3);
        assertEquals("B",       Integer.valueOf(5), map.get(store, "B"));
        assertEquals("A",       Integer.valueOf(3), map.get(store, "A"));
        assertEquals("myNS:A",  Integer.valueOf(2), map.get(store, "myNS:A"));
        assertEquals("other:A", Integer.valueOf(7), map.get(store, "other:A"));
    }

    /**
     * Tests removing an entry. It should change the state about whether an alias is ambiguous or not.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testAmbiguity")
    public void testRemove() throws IllegalNameException {
        final DataStoreMock store = new DataStoreMock("testDataStore");
        final FeatureNaming<Integer> map = new FeatureNaming<>();
        map.add(store, A, 1);
        map.add(store, B, 2);
        map.add(store, otherA, 3);
        /*
         * Verify that "myNS:A" exists before the removal, then does not exist anymore after the removal.
         */
        assertEquals("otherA", Integer.valueOf(3), map.get(store, "other:A"));
        assertEquals("myNS:A", Integer.valueOf(1), map.get(store, "myNS:A"));
        assertTrue("remove", map.remove(store, A));
        try {
            map.get(store, "myNS:A");
            fail("Should not find a non-existent entry.");
        } catch (IllegalNameException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("myNS:A"));
            assertTrue(message, message.contains("testDataStore"));
        }
        /*
         * The "A" shortcut should not be ambiguous anymore at this point since we removed the other name
         * ("myNS:A") which was causing the ambiguity;
         */
        assertEquals("A",      Integer.valueOf(3), map.get(store, "A"));
        assertEquals("B",      Integer.valueOf(2), map.get(store, "B"));
        assertEquals("otherA", Integer.valueOf(3), map.get(store, "other:A"));
    }
}
