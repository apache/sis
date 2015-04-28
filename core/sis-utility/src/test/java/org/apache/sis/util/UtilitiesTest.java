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
package org.apache.sis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.apache.sis.test.TestCase;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link Utilities} static methods.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class UtilitiesTest extends TestCase {
    /**
     * Tests {@link Utilities#deepEquals(Object, Object, ComparisonMode)}.
     */
    @Test
    public void testDeepEquals() {
        testDeepEquals(null, true);
        testDeepEquals(null, false);

        testDeepEquals(ComparisonMode.STRICT, true);
        testDeepEquals(ComparisonMode.STRICT, false);
    }

    /**
     * Tests {@link Utilities#deepEquals(Object, Object, ComparisonMode)} using the given
     * comparison mode with the given collections.
     */
    private static void testDeepEquals(final ComparisonMode mode, final boolean orderIsSignificant) {
        final DummyLenient e1 = new DummyLenient("Janvier", mode);
        final DummyLenient e2 = new DummyLenient("Juin",    mode);
        final DummyLenient e3 = new DummyLenient("Janvier", mode);
        final DummyLenient e4 = new DummyLenient("Juin",    mode);
        assertTrue (Utilities.deepEquals(e1, e1, mode));
        assertFalse(Utilities.deepEquals(e1, e2, mode));
        assertTrue (Utilities.deepEquals(e1, e3, mode));
        assertFalse(Utilities.deepEquals(e1, e4, mode));
        assertFalse(Utilities.deepEquals(e2, e3, mode));
        assertTrue (Utilities.deepEquals(e2, e4, mode));
        assertFalse(Utilities.deepEquals(e3, e4, mode));

        final Collection<DummyLenient> c1, c2;
        if (orderIsSignificant) {
            c1 = new ArrayList<DummyLenient>();
            c2 = new ArrayList<DummyLenient>();
        } else {
            c1 = new LinkedHashSet<DummyLenient>();
            c2 = new LinkedHashSet<DummyLenient>();
        }
        assertTrue(c1.add(e1)); assertTrue(c1.add(e2));
        assertTrue(c2.add(e3)); assertTrue(c2.add(e4));
        assertTrue(Utilities.deepEquals(c1, c2, mode));
        assertTrue(c2.remove(e3));
        assertFalse(Utilities.deepEquals(c1, c2, mode));
        assertTrue(c2.add(e3));
        assertEquals(!orderIsSignificant, Utilities.deepEquals(c1, c2, mode));

        assertTrue(e1.comparisonCount != 0);
        assertTrue(e2.comparisonCount != 0);
        assertTrue(e3.comparisonCount != 0);
    }

    /**
     * For {@link #testDeepEquals()} purpose only.
     */
    private static final strictfp class DummyLenient implements LenientComparable {
        /** Label to be used in comparison. */
        private final String label;

        /** The expected comparison mode. */
        private final ComparisonMode expected;

        /** Number of comparison performed. */
        int comparisonCount;

        /** Creates a new instance expecting the given comparison mode. */
        DummyLenient(final String label, final ComparisonMode expected) {
            this.label = label;
            this.expected = expected;
        }

        /** Compares this object with the given one. */
        @Override public boolean equals(final Object other, final ComparisonMode mode) {
            assertEquals(label, expected, mode);
            comparisonCount++;
            return equals(other);
        }

        /** Compares this dummy object with the given object. */
        @Override public boolean equals(final Object other) {
            return (other instanceof DummyLenient) && label.equals(((DummyLenient) other).label);
        }

        /** For consistency with {@link #equals(Object)}. */
        @Override public int hashCode() {
            return label.hashCode();
        }

        /** For debugging purpose only. */
        @Override public String toString() {
            return label;
        }
    }
}
