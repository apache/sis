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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code difference} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DifferenceTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code difference(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry other,
                            Geometry expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code difference(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // ∅ − A = ∅ and A − ∅ = A, which are both the first operand.
        new TestCase(EMPTY_1,   NON_EMPTY,   EMPTY_1,      null),
        new TestCase(NON_EMPTY, EMPTY_1,     NON_EMPTY,    null),
        new TestCase(EMPTY_1,   EMPTY_2,     EMPTY_1,      null),
        // points
        new TestCase(POINT_A,   POINT_B,     POINT_A,      null),
        new TestCase(POINT_A,   POINT_A_BIS, EMPTY_RESULT, null)
    };

    /**
     * Tests {@code difference(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testDifference() {
        for (final TestCase entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().difference(entry.input(), entry.other());
                assertNull(entry.error(), "An exception was expected.");
                assertEquals(entry.expected(), result);
            } catch (Exception ex) {
                if (entry.error() == null || !entry.error().isInstance(ex)) {
                    throw new AssertionError("Unexpected exception for " + entry, ex);
                }
            }
        }
    }

    /**
     * Tests {@code difference(Geometry, Geometry)} on two surfaces. Removing a surface from
     * another one leaves the part of the first one which the second does not cover.
     */
    @Test
    public void testDifferenceOfSurfaces() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertEquals(75, areaOf(processor.difference(SQUARE, SQUARE_OVERLAP)), TOLERANCE, "A square of area 100 less the area of 25 it shares with another one.");
        assertEquals(96, areaOf(processor.difference(SQUARE, SQUARE_INNER)), TOLERANCE, "A square of area 100 less an inner square of area 4, which digs a hole.");
        assertEquals(100, areaOf(processor.difference(SQUARE, SQUARE_DISJOINT)), TOLERANCE, "Removing a disjoint surface changes nothing.");
        assertEquals(100, areaOf(processor.difference(SQUARE, SQUARE_TOUCHING)), TOLERANCE, "Removing a surface sharing only an edge changes no area.");
        assertTrue(processor.difference(SQUARE, SQUARE_BIS).isEmpty(), "Removing a square from itself leaves nothing.");
    }

}
