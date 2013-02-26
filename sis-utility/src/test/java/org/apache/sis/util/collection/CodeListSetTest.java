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

import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.referencing.cs.AxisDirection.*;


/**
 * Test the {@link CodeListSet} implementation.
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
        final CodeListSet<AxisDirection> c = new CodeListSet<>(AxisDirection.class);
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
    }

    /**
     * Tests the {@link CodeListSet#remove(Object)} method.
     */
    @Test
    @DependsOnMethod("testContains")
    public void testRemove() {
        final CodeListSet<AxisDirection> c = create(4);
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
}
