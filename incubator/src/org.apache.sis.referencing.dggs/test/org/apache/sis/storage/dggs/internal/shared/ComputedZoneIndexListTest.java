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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.ArrayList;
import java.util.List;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ComputedZoneIndexList}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ComputedZoneIndexListTest {

    /**
     * Creates the list 10, 20, 30.
     */
    private static ComputedZoneIndexList create() {
        return new ComputedZoneIndexList(10, 10, 3, (long id) -> id);
    }

    /**
     * Tests that iterating over the list returns each element once.
     */
    @Test
    public void testIteration() {
        final ComputedZoneIndexList list = create();
        final List<Object> copy = new ArrayList<>();
        for (final Object id : list) {
            copy.add(id);
        }
        assertEquals(List.of(10L, 20L, 30L), copy);
        assertArrayEquals(new Object[] {10L, 20L, 30L}, list.toArray());
        assertEquals(30L, list.listIterator(3).previous());
    }

    /**
     * Tests {@link ComputedZoneIndexList#indexOf(Object)} on values inside and outside the list.
     */
    @Test
    public void testIndexOf() {
        final ComputedZoneIndexList list = create();
        assertEquals( 1, list.indexOf(20L));
        assertEquals( 2, list.indexOf(30));
        assertEquals(-1, list.indexOf(15L));
        assertEquals(-1, list.indexOf(9L));
        assertEquals(-1, list.indexOf(40L));
        assertFalse(list.contains(9L));
    }
}
