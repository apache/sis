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

import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.metadata.citation.OnLineFunction;
import static org.opengis.referencing.cs.AxisDirection.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link CodeListSet} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CodeListSetTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CodeListSetTest() {
    }

    /**
     * Creates a new set filled with up to 4 axis directions.
     * The directions are (NORTH_NORTH_EAST, EAST, UP, FUTURE) in that order.
     *
     * @param n Number of code list to add.
     */
    @SuppressWarnings("fallthrough")
    private CodeListSet<AxisDirection> create(final int n) {
        final CodeListSet<AxisDirection> c = new CodeListSet<>(AxisDirection.class);
        assertTrue(c.isEmpty());
        switch (n) {
            default: throw new AssertionError(n);
            case 4: assertTrue(c.add(FUTURE));              // Fallthrough everywhere.
            case 3: assertTrue(c.add(UP));
            case 2: assertTrue(c.add(EAST));
            case 1: assertTrue(c.add(NORTH_NORTH_EAST));
            case 0: break;
        }
        assertEquals(n, c.size());
        return c;
    }

    /**
     * Creates a code list of another kind. The returned set contains a code list having
     * the same ordinal value as {@link AxisDirection#NORTH_NORTH_EAST}, so we can detect
     * if the {@code SortedSet} confuses the code list types.
     */
    private CodeListSet<OnLineFunction> createOtherKind() {
        // For the validity of the tests, ordinal value must be the same.
        assertEquals(NORTH_NORTH_EAST.ordinal(), OnLineFunction.INFORMATION.ordinal());
        final CodeListSet<OnLineFunction> c = new CodeListSet<>(OnLineFunction.class);
        assertTrue(c.add(OnLineFunction.INFORMATION));
        return c;
    }

    /**
     * Tests the {@link CodeListSet#toArray()} method.
     * This will indirectly tests the iterator.
     */
    @Test
    public void testToArray() {
        final CodeListSet<AxisDirection> c = create(4);
        assertArrayEquals(new Object[] {NORTH_NORTH_EAST, EAST, UP, FUTURE}, c.toArray());
    }

    /**
     * Tests the {@link CodeListSet#clear()} method.
     */
    @Test
    public void testClear() {
        final CodeListSet<AxisDirection> c = create(2);
        assertFalse(c.isEmpty());
        c.clear();
        assertTrue(c.isEmpty());
    }

    /**
     * Tests the {@link CodeListSet#contains(Object)} method.
     */
    @Test
    public void testContains() {
        final CodeListSet<AxisDirection> c = create(4);
        assertTrue (c.contains(NORTH_NORTH_EAST));
        assertFalse(c.contains(SOUTH));
        assertTrue (c.contains(FUTURE));
        assertFalse(c.contains(PAST));
        assertTrue (c.contains(EAST));
        assertFalse(c.contains(WEST));
        assertTrue (c.contains(UP));
        assertFalse(c.contains(DOWN));

        assertFalse(c.contains(null), "Should be null-safe.");
        assertFalse(c.contains(OnLineFunction.INFORMATION), "Code list of other kind should not be included.");
    }

    /**
     * Tests the {@link CodeListSet#remove(Object)} method.
     */
    @Test
    public void testRemove() {
        final CodeListSet<AxisDirection> c = create(4);
        assertFalse(c.remove(null), "Should be null-safe.");
        assertFalse(c.remove(OnLineFunction.INFORMATION), "Code list of other kind should not be included.");

        assertTrue (c.remove  (NORTH_NORTH_EAST));
        assertFalse(c.remove  (SOUTH));
        assertFalse(c.contains(NORTH_NORTH_EAST));
        assertEquals(3, c.size());

        assertTrue (c.remove  (FUTURE));
        assertFalse(c.contains(FUTURE));
        assertFalse(c.remove  (PAST));
        assertEquals(2, c.size());

        assertTrue (c.remove  (EAST));
        assertTrue (c.remove  (UP));
        assertTrue (c.isEmpty());
    }

    /**
     * Tests the {@link CodeListSet#containsAll(Collection)} method.
     */
    @Test
    public void testContainsAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(4);
        assertTrue (c.containsAll(o));
        assertTrue (o.remove(NORTH_NORTH_EAST));
        assertTrue (o.remove(FUTURE));
        assertTrue (c.containsAll(o));
        assertTrue (o.add(NORTH_EAST));
        assertFalse(c.containsAll(o));
        assertFalse(c.containsAll(createOtherKind()));
    }

    /**
     * Tests the {@link CodeListSet#removeAll(Collection)} method.
     */
    @Test
    public void testRemoveAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(2);
        assertTrue(o.add(NORTH_EAST));      // Extra value shall be ignored.

        assertFalse(c.removeAll(createOtherKind()));
        assertTrue(c.removeAll(o));
        assertArrayEquals(new Object[] {UP, FUTURE}, c.toArray());
        assertFalse(c.removeAll(o));        // Invoking a second time should not make any difference.
        assertEquals(2, c.size());
    }

    /**
     * Tests the {@link CodeListSet#removeAll(Collection)} method.
     */
    @Test
    public void testRetainAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(2);
        assertTrue(o.add(NORTH_EAST));      // Extra value shall be ignored.

        assertTrue(c.retainAll(o));
        assertArrayEquals(new Object[] {NORTH_NORTH_EAST, EAST}, c.toArray());
        assertFalse(c.retainAll(o));        // Invoking a second time should not make any difference.
        assertEquals(2, c.size());
        assertTrue(c.retainAll(createOtherKind()));
        assertTrue(c.isEmpty());
    }

    /**
     * Tests the {@link CodeListSet#addAll(Collection)} method.
     */
    @Test
    public void testAddAll() {
        final CodeListSet<AxisDirection> c = create(1);
        final CodeListSet<AxisDirection> o = create(3);
        assertTrue(c.add(NORTH_EAST));

        assertTrue(c.addAll(o));
        assertArrayEquals(new Object[] {NORTH_NORTH_EAST, NORTH_EAST, EAST, UP}, c.toArray());
        assertFalse(c.addAll(o));       // Invoking a second time should not make any difference.
    }

    /**
     * Tests the creation of a set filled with with all known values.
     */
    @Test
    public void testFill() {
        final CodeListSet<AxisDirection> c = new CodeListSet<>(AxisDirection.class, true);
        assertTrue(c.size() >= 32, "Expect at least 32 elements as of GeoAPI 3.0.");
        assertTrue(c.toString().startsWith("[AxisDirection.NORTH, AxisDirection.NORTH_NORTH_EAST, "));
        /*
         * Testing the full array would be too long and may change in future GeoAPI version
         * anyway. Actually the main interest of this test is to ensure that the toString()
         * method doesn't throw an IndexOutOfBoundsException (as it would be the case if
         * the constructor had set too many bits).
         */
    }

    /**
     * Tests the various methods with a code list containing more than 64 elements.
     */
    @Test
    public void testLargeCodeList() {
        final Set<LargeCodeList> main = new HashSet<>(Arrays.asList(LargeCodeList.values()));
        assertTrue(main.size() > Long.SIZE, "This test requires more than 64 elements.");
        final CodeListSet<LargeCodeList> c = new CodeListSet<>(LargeCodeList.class);
        /*
         * Copy all content from the `main` to the CodeListSet. This will indirectly
         * test CodeListSet.add(E), through the AbstractSet.addAll(Collection) method.
         */
        assertTrue(c.addAll(main));
        assertEquals(main.size(), c.size());
        assertEquals(main, c);
        assertFalse(c.addAll(main));        // Invoking a second time should not make any difference.
        /*
         * Keep a copy of the set before we modify it.
         */
        final CodeListSet<LargeCodeList> clone = c.clone();
        assertNotSame(c, clone);
        assertEquals(main, clone);
        assertEquals(clone, new CodeListSet<>(LargeCodeList.class, true));
        /*
         * Tests contains(Object) and remove(Object). We also remove elements
         * from the `main` set, then we verify that the result is the same.
         */
        LargeCodeList lastRemoved = null;
        final Random random = new Random();
        do {
            for (final Iterator<LargeCodeList> it=main.iterator(); it.hasNext();) {
                final LargeCodeList code = it.next();
                assertTrue(c.contains(code), code.name());
                if (random.nextBoolean()) {
                    assertTrue (c.remove(code), code.name());
                    assertFalse(c.contains(code), code.name());
                    it.remove();
                    lastRemoved = code;
                    if (main.size() == 1) {
                        // Very unlikely, but let be safe since the tests
                        // after the look require at least one element.
                        break;
                    }
                }
            }
        } while (lastRemoved == null);
        assertEquals(main, c);
        assertFalse(c.isEmpty());
        /*
         * Test containsAll(Collection) and removeAll(Collection).
         */
        assertTrue (clone.containsAll(c));                  // The original set shall contain the decimated set.
        assertFalse(c.containsAll(clone));                  // The decimated set cannot contain the original set.
        assertTrue (clone.remove(lastRemoved));             // Original set minus one element.
        assertTrue (c.add(lastRemoved));                    // Add an element to be ignored by removeAll(…).
        assertTrue (clone.removeAll(c));                    // Remove all elements found in the decimated set.
        assertTrue (Collections.disjoint(main, clone));     // Expect no common elements.
        assertFalse(clone.removeAll(c));                    // Invoking a second time should not make any difference.
        /*
         * Test retainAll(Collection).
         */
        assertTrue(clone.add(lastRemoved));                 // Add the element to be retained.
        assertTrue(c.retainAll(clone));
        assertEquals(Set.of(lastRemoved), c);
    }
}
