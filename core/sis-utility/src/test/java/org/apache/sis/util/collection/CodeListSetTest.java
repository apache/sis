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
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.util.iso.LargeCodeList;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.referencing.cs.AxisDirection.*;


/**
 * Tests the {@link CodeListSet} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class CodeListSetTest extends TestCase {
    /**
     * Creates a new set filled with up to 4 axis directions.
     * The directions are (NORTH, EAST, UP, FUTURE) in that order.
     *
     * @param n Number of code list to add.
     */
    @SuppressWarnings("fallthrough")
    private CodeListSet<AxisDirection> create(final int n) {
        final CodeListSet<AxisDirection> c = new CodeListSet<AxisDirection>(AxisDirection.class);
        assertTrue(c.isEmpty());
        switch (n) {
            default: throw new AssertionError(n);
            case 4: assertTrue(c.add(FUTURE)); // Fallthrough everywhere.
            case 3: assertTrue(c.add(UP));
            case 2: assertTrue(c.add(EAST));
            case 1: assertTrue(c.add(NORTH));
            case 0: break;
        }
        assertEquals("SortedSet.size()", n, c.size());
        return c;
    }

    /**
     * Creates a code list of an other kind. The returned set contains a code list having
     * the same ordinal value than {@link AxisDirection#NORTH}, so we can detect if the
     * {@code SortedSet} confuses the code list types.
     */
    private CodeListSet<OnLineFunction> createOtherKind() {
        // For the validity of the tests, ordinal value must be the same.
        assertEquals(NORTH.ordinal(), OnLineFunction.INFORMATION.ordinal());
        final CodeListSet<OnLineFunction> c = new CodeListSet<OnLineFunction>(OnLineFunction.class);
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
        assertArrayEquals(new Object[] {NORTH, EAST, UP, FUTURE}, c.toArray());
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
        assertTrue ("NORTH",  c.contains(NORTH));
        assertFalse("SOUTH",  c.contains(SOUTH));
        assertTrue ("FUTURE", c.contains(FUTURE));
        assertFalse("PAST",   c.contains(PAST));
        assertTrue ("EAST",   c.contains(EAST));
        assertFalse("WEST",   c.contains(WEST));
        assertTrue ("UP",     c.contains(UP));
        assertFalse("DOWN",   c.contains(DOWN));

        assertFalse("Should be null-safe.", c.contains(null));
        assertFalse("Code list of other kind should not be included.",
                c.contains(OnLineFunction.INFORMATION));
    }

    /**
     * Tests the {@link CodeListSet#remove(Object)} method.
     */
    @Test
    @DependsOnMethod("testContains")
    public void testRemove() {
        final CodeListSet<AxisDirection> c = create(4);
        assertFalse("Should be null-safe.", c.remove(null));
        assertFalse("Code list of other kind should not be included.",
                c.remove(OnLineFunction.INFORMATION));

        assertTrue ("NORTH",  c.remove  (NORTH));
        assertFalse("SOUTH",  c.remove  (SOUTH));
        assertFalse("NORTH",  c.contains(NORTH));
        assertEquals(3, c.size());

        assertTrue ("FUTURE", c.remove  (FUTURE));
        assertFalse("FUTURE", c.contains(FUTURE));
        assertFalse("PAST",   c.remove  (PAST));
        assertEquals(2, c.size());

        assertTrue ("EAST",   c.remove  (EAST));
        assertTrue ("UP",     c.remove  (UP));
        assertTrue (c.isEmpty());
    }

    /**
     * Tests the {@link CodeListSet#containsAll(Collection)} method.
     */
    @Test
    @DependsOnMethod("testRemove")
    public void testContainsAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(4);
        assertTrue (c.containsAll(o));
        assertTrue (o.remove(NORTH));
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
    @DependsOnMethod("testToArray")
    public void testRemoveAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(2);
        assertTrue(o.add(NORTH_EAST)); // Extra value shall be ignored.

        assertFalse(c.removeAll(createOtherKind()));
        assertTrue(c.removeAll(o));
        assertArrayEquals(new Object[] {UP, FUTURE}, c.toArray());
        assertFalse("Invoking a second time should not make any difference.", c.removeAll(o));
        assertEquals(2, c.size());
    }

    /**
     * Tests the {@link CodeListSet#removeAll(Collection)} method.
     */
    @Test
    @DependsOnMethod("testToArray")
    public void testRetainAll() {
        final CodeListSet<AxisDirection> c = create(4);
        final CodeListSet<AxisDirection> o = create(2);
        assertTrue(o.add(NORTH_EAST)); // Extra value shall be ignored.

        assertTrue(c.retainAll(o));
        assertArrayEquals(new Object[] {NORTH, EAST}, c.toArray());
        assertFalse("Invoking a second time should not make any difference.", c.retainAll(o));
        assertEquals(2, c.size());
        assertTrue(c.retainAll(createOtherKind()));
        assertTrue(c.isEmpty());
    }

    /**
     * Tests the {@link CodeListSet#addAll(Collection)} method.
     */
    @Test
    @DependsOnMethod("testToArray")
    public void testAddAll() {
        final CodeListSet<AxisDirection> c = create(1);
        final CodeListSet<AxisDirection> o = create(3);
        assertTrue(c.add(NORTH_EAST));

        assertTrue(c.addAll(o));
        assertArrayEquals(new Object[] {NORTH, NORTH_EAST, EAST, UP}, c.toArray());
        assertFalse("Invoking a second time should not make any difference.", c.addAll(o));
    }

    /**
     * Tests the creation of a set filled with with all known values.
     */
    @Test
    @DependsOnMethod("testContains")
    public void testFill() {
        final CodeListSet<AxisDirection> c = new CodeListSet<AxisDirection>(AxisDirection.class, true);
        assertTrue("Expect at least 32 elements as of GeoAPI 3.0.0.", c.size() >= 32);
        assertTrue(c.toString().startsWith("[AxisDirection[OTHER], AxisDirection[NORTH], "));
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
        final Set<LargeCodeList> master = new HashSet<LargeCodeList>(Arrays.asList(LargeCodeList.values()));
        assertTrue("This test requires more than 64 elements.", master.size() > Long.SIZE);
        final CodeListSet<LargeCodeList> c = new CodeListSet<LargeCodeList>(LargeCodeList.class);
        /*
         * Copy all content from the master to the CodeListSet. This will indirectly
         * test CodeListSet.add(E), through the AbstractSet.addAll(Collection) method.
         */
        assertTrue(c.addAll(master));
        assertEquals(master.size(), c.size());
        assertEquals(master, c);
        assertFalse("Invoking a second time should not make any difference.", c.addAll(master));
        /*
         * Keep a copy of the set before we modify it.
         */
        final CodeListSet<LargeCodeList> clone = c.clone();
        assertNotSame("Clone shall be a new instance.", c, clone);
        assertEquals("Clone shall be equal to the original.", master, clone);
        assertEquals(clone, new CodeListSet<LargeCodeList>(LargeCodeList.class, true));
        /*
         * Tests contains(Object) and remove(Object). We also remove elements
         * from the master set, then we verify that the result is the same.
         */
        LargeCodeList lastRemoved = null;
        final Random random = new Random();
        do {
            for (final Iterator<LargeCodeList> it=master.iterator(); it.hasNext();) {
                final LargeCodeList code = it.next();
                assertTrue(code.name(), c.contains(code));
                if (random.nextBoolean()) {
                    assertTrue (code.name(), c.remove(code));
                    assertFalse(code.name(), c.contains(code));
                    it.remove();
                    lastRemoved = code;
                    if (master.size() == 1) {
                        // Very unlikely, but let be safe since the tests
                        // after the look require at least one element.
                        break;
                    }
                }
            }
        } while (lastRemoved == null);
        assertEquals(master, c);
        assertFalse(c.isEmpty());
        /*
         * Test containsAll(Collection) and removeAll(Collection).
         */
        assertTrue ("The original set shall contain the decimated set.",   clone.containsAll(c));
        assertFalse("The decimated set can not contain the original set.", c.containsAll(clone));
        assertTrue ("Original set minus one element.",                     clone.remove(lastRemoved));
        assertTrue ("Add an element to be ignored by removeAll(…).",       c.add(lastRemoved));
        assertTrue ("Remove all elements found in the decimated set.",     clone.removeAll(c));
        assertTrue ("Expect no common elements.", Collections.disjoint(master, clone));
        assertFalse("Invoking a second time should not make any difference.", clone.removeAll(c));
        /*
         * Test retainAll(Collection).
         */
        assertTrue("Add the element to be retained.", clone.add(lastRemoved));
        assertTrue(c.retainAll(clone));
        assertEquals(Collections.singleton(lastRemoved), c);
    }
}
