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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.Geometry;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code overlaps} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class OverlapsTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code overlaps(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry other,
                            Boolean expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code overlaps(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * Overlapping requires the two interiors to share a position while neither geometry
         * contains the other. The empty set has no interior, therefore it overlaps nothing.
         */
        new TestCase(EMPTY_1,            NON_EMPTY,          false, null),
        new TestCase(NON_EMPTY,          EMPTY_1,            false, null),
        new TestCase(EMPTY_1,            EMPTY_1,            false, null),
        new TestCase(EMPTY_1,            EMPTY_2,            false, null),
        /*
         * Two points cannot overlap: either they are at the same position, and then each one
         * contains the other, or they share no position at all.
         */
        new TestCase(POINT_A,            POINT_A_BIS,        false, null),
        new TestCase(POINT_A,            POINT_B,            false, null),
        /*
         * Overlapping requires the two geometries to have the same dimension, so a position
         * never overlaps a surface and a curve never does either.
         */
        new TestCase(POINT_CENTER,       SQUARE,             false, null),
        new TestCase(LINE_BOTTOM,        SQUARE,             false, null),
        /*
         * Two curves overlap when they share a whole segment. Sharing a single position is not
         * overlapping: what they have in common is then of a lower dimension than themselves.
         */
        new TestCase(LINE_BOTTOM,        LINE_COLLINEAR,     true,  null),
        new TestCase(LINE_BOTTOM,        LINE_CROSSING,      false, null),
        new TestCase(LINE_BOTTOM,        LINE_DIAGONAL,      false, null),
        /*
         * Two surfaces. Neither geometry may contain the other, so a surface overlaps neither
         * a surface it contains nor a surface at its own position.
         */
        new TestCase(SQUARE,             SQUARE_OVERLAP,     true,  null),
        new TestCase(SQUARE,             SQUARE_INNER,       false, null),
        new TestCase(SQUARE,             SQUARE_BIS,         false, null),
        new TestCase(SQUARE,             SQUARE_TOUCHING,    false, null),
        new TestCase(SQUARE,             SQUARE_DISJOINT,    false, null),
        /*
         * Two sets of positions sharing one position, neither of them containing the other.
         */
        new TestCase(MULTI_POINT_INSIDE, MULTI_POINT_SPREAD, true,  null)
    };

    /**
     * Tests {@code overlaps(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testOverlaps() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().overlaps(entry.input(), entry.other());
                assertNull(entry.error(), "An exception was expected.");
                assertEquals(entry.expected(), result);
            } catch (Exception ex) {
                if (entry.error() == null || !entry.error().isInstance(ex)) {
                    throw new AssertionError("Unexpected exception for " + entry, ex);
                }
            }
        }
    }
}
