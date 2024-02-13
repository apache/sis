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
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link FeatureNaming}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FeatureNamingTest extends TestCase {
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
        assertEquals(Integer.valueOf(1), map.get(store, "myNS:A"));
        assertEquals(Integer.valueOf(1), map.get(store, "A"));
        assertEquals(Integer.valueOf(2), map.get(store, "B"));
        /*
         * Above code tested normal usage. Now test error conditions.
         * First, searching a non-existent entry should raise an exception.
         */
        IllegalNameException e;
        e = assertThrows(IllegalNameException.class, () -> map.get(store, "C"),
                         "Should not find a non-existent entry.");
        assertMessageContains(e, "C", "testDataStore");
        /*
         * Attempt to overwrite an existing entry should raise an exception
         * without modifying the existing value.
         */
        e = assertThrows(IllegalNameException.class, () -> map.add(store, B, 3),
                         "Should not overwrite an existing entry.");
        assertMessageContains(e, "B");
        assertEquals(Integer.valueOf(2), map.get(store, "B"),
                     "Existing value should not have been modified.");
    }

    /**
     * Same test as {@link #testSimple()}, with the addition of an ambiguous name.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testAmbiguity() throws IllegalNameException {
        final var store = new DataStoreMock("testDataStore");
        final var map   = new FeatureNaming<Integer>();
        map.add(store, A, 1);
        map.add(store, B, 2);
        map.add(store, otherA, 3);
        assertEquals(Integer.valueOf(1), map.get(store, "myNS:A"));
        assertEquals(Integer.valueOf(2), map.get(store, "B"));
        assertEquals(Integer.valueOf(3), map.get(store, "other:A"));
        /*
         * Attempt to query using only the "A" value was used to succeed in 'testSimple()' but
         * should now fail because this shortcut could apply to "other:A" as well as "myNS:A".
         */
        var e = assertThrows(IllegalNameException.class, () -> map.get(store, "A"),
                             "Should not find an ambiguous entry.");
        assertMessageContains(e, "myNS:A", "other:A", "testDataStore");
    }

    /**
     * Tests two names having the same tip, but where only one of the two names have a namespace.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testAmbiguity")
    public void testQualifiedAndUnqualifiedName() throws IllegalNameException {
        final var store = new DataStoreMock("testDataStore");
        final var local = Names.createLocalName(null, null, "A");
        FeatureNaming<Integer> map = new FeatureNaming<>();
        map.add(store, local,  3);
        map.add(store, A,      2);
        map.add(store, B,      5);
        map.add(store, otherA, 7);
        assertEquals(Integer.valueOf(5), map.get(store, "B"));
        assertEquals(Integer.valueOf(3), map.get(store, "A"));
        assertEquals(Integer.valueOf(2), map.get(store, "myNS:A"));
        assertEquals(Integer.valueOf(7), map.get(store, "other:A"));
        /*
         * Same tests, but with elements added in different order.
         */
        map = new FeatureNaming<>();
        map.add(store, otherA, 7);
        map.add(store, B,      5);
        map.add(store, A,      2);
        map.add(store, local,  3);
        assertEquals(Integer.valueOf(5), map.get(store, "B"));
        assertEquals(Integer.valueOf(3), map.get(store, "A"));
        assertEquals(Integer.valueOf(2), map.get(store, "myNS:A"));
        assertEquals(Integer.valueOf(7), map.get(store, "other:A"));
    }

    /**
     * Tests removing an entry. It should change the state about whether an alias is ambiguous or not.
     *
     * @throws IllegalNameException if an unexpected error occurred while adding or getting an element.
     */
    @Test
    @DependsOnMethod("testAmbiguity")
    public void testRemove() throws IllegalNameException {
        final var store = new DataStoreMock("testDataStore");
        final var map   = new FeatureNaming<Integer>();
        map.add(store, A, 1);
        map.add(store, B, 2);
        map.add(store, otherA, 3);
        /*
         * Verify that "myNS:A" exists before the removal, then does not exist anymore after the removal.
         */
        assertEquals(Integer.valueOf(3), map.get(store, "other:A"));
        assertEquals(Integer.valueOf(1), map.get(store, "myNS:A"));
        assertTrue(map.remove(store, A));
        var e = assertThrows(IllegalNameException.class, () -> map.get(store, "myNS:A"),
                             "Should not find a non-existent entry.");
        assertMessageContains(e, "myNS:A", "testDataStore");
        /*
         * The "A" shortcut should not be ambiguous anymore at this point since we removed the other name
         * ("myNS:A") which was causing the ambiguity;
         */
        assertEquals(Integer.valueOf(3), map.get(store, "A"));
        assertEquals(Integer.valueOf(2), map.get(store, "B"));
        assertEquals(Integer.valueOf(3), map.get(store, "other:A"));
    }
}
