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

import java.util.ListIterator;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ListOfUnknownSize}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ListOfUnknownSizeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ListOfUnknownSizeTest() {
    }

    /**
     * Tests {@link ListOfUnknownSize#listIterator()}.
     */
    @Test
    public void testListIterator() {
        final Integer[] data = new Integer[] {4, 7, 1, 3, 0, -4, -3};
        final ListIterator<Integer> it = new ListOfUnknownSize<Integer>() {
            @Override
            protected boolean isValidIndex(int index) {
                return index >= 0 && index < data.length;
            }

            @Override
            public Integer get(int index) {
                return data[index];
            }
        }.listIterator();

        for (int i=0; i<data.length; i++) {
            assertTrue(it.hasNext());
            assertEquals(i, it.nextIndex());
            assertEquals(data[i], it.next());
        }
        assertFalse(it.hasNext());
    }
}
