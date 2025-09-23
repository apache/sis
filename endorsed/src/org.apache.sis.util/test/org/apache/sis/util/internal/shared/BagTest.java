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
package org.apache.sis.util.internal.shared;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Bag}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BagTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public BagTest() {
    }

    /**
     * Creates an instance initialized to the given values.
     */
    private static Bag<Integer> create(final int... values) {
        final List<Integer> list = new ArrayList<>(values.length);
        for (final int v : values) list.add(v);
        return new Bag<Integer>() {
            @Override public int               size()     {return list.size();}
            @Override public Iterator<Integer> iterator() {return list.iterator();}
        };
    }

    /**
     * Asserts that the following bags are equal.
     */
    private static void assertBagEquals(final Bag<?> b1, final Bag<?> b2) {
        assertEquals(b1, b2);
        assertEquals(b2, b1);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    /**
     * Asserts that the following bags are not equal.
     */
    private static void assertBagNotEquals(final Bag<?> b1, final Bag<?> b2) {
        assertNotEquals(b1, b2);
        assertNotEquals(b2, b1);
        assertNotEquals(b1.hashCode(), b2.hashCode());
    }

    /**
     * Tests {@link Bag#equals(Object)} and {@link Bag#hashCode()}.
     */
    @Test
    public void testEquals() {
        Bag<Integer> b1 = create(4, 8, 3, 2);
        Bag<Integer> b2 = create(8, 2, 3, 4);
        Bag<Integer> b3 = create(8, 5, 4, 3);
        assertEquals      (b1, b1);
        assertBagEquals   (b1, b2);
        assertBagNotEquals(b1, b3);
        assertBagNotEquals(b2, b3);
    }
}
