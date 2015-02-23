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

import java.util.Arrays;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link LazySet}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class LazySetTest extends TestCase {
    /**
     * The test data.
     */
    private static final String[] LABELS = {"one", "two", "three", "four"};

    /**
     * Creates the set to use for testing purpose.
     */
    private static LazySet<String> create() {
        return new LazySet<>(Arrays.asList(LABELS).iterator());
    }

    /**
     * Tests {@link LazySet#isEmpty()} followed by {@link LazySet#size()}.
     */
    @Test
    public void testIsEmptyAndSize() {
        final LazySet<String> set = create();
        assertFalse(set.isEmpty());
        assertEquals(LABELS.length, set.size());
        assertFalse(set.isEmpty());
    }

    /**
     * Tests {@link LazySet#size()} followed by {@link LazySet#isEmpty()}.
     */
    @Test
    @DependsOnMethod("testIsEmptyAndSize")
    public void testSizeAndIsEmpty() {
        final LazySet<String> set = create();
        assertEquals(LABELS.length, set.size());
        assertFalse(set.isEmpty());
        assertEquals(LABELS.length, set.size());
    }

    /**
     * Tests iteration.
     */
    @Test
    public void testIteration() {
        final LazySet<String> set = create();
        assertArrayEquals(LABELS, set.toArray());
        assertEquals(LABELS.length, set.size());
    }
}
