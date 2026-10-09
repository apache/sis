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
import org.apache.sis.geometries.point.MultiPoint;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code symDifference} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SymDifferenceTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code symDifference(Geometry, Geometry)}.
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
     * All test cases of {@code symDifference(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // (∅ − A) ∪ (A − ∅) = A: the result is the operand which is not empty.
        new TestCase(EMPTY_1,   NON_EMPTY,   NON_EMPTY,    null),
        new TestCase(NON_EMPTY, EMPTY_1,     NON_EMPTY,    null),
        new TestCase(EMPTY_1,   EMPTY_2,     EMPTY_2,      null),
        new TestCase(POINT_A,   POINT_A_BIS, EMPTY_RESULT, null)
    };

    /**
     * Tests {@code symDifference(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testSymDifference() {
        for (final TestCase entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().symDifference(entry.input(), entry.other());
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
     * Tests {@code symDifference(Geometry, Geometry)} on two points at different positions.
     * No position belongs to both points, therefore the result shall contain both of them,
     * in an unspecified order.
     */
    @Test
    public void testDistinctPoints() {
        final Geometry result = new GeometryProcessor().symDifference(POINT_A, POINT_B);
        assertInstanceOf(MultiPoint.class, result, "No position is shared by the two points.");
        final MultiPoint<?> points = (MultiPoint<?>) result;
        assertEquals(CRS_2D, points.getCoordinateReferenceSystem());
        assertEquals(2, points.getNumGeometries());
        assertPositionsEqual(points, POINT_A, POINT_B);
    }

    /**
     * Tests {@code symDifference(Geometry, Geometry)} on two surfaces. The symmetric difference
     * keeps what belongs to exactly one of the two surfaces, which is their union less what they
     * have in common.
     */
    @Test
    public void testSymDifferenceOfSurfaces() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertEquals(150, areaOf(processor.symDifference(SQUARE, SQUARE_OVERLAP)), TOLERANCE, "Two squares of area 100 sharing an area of 25: 175 of union less 25 in common.");
        assertEquals(200, areaOf(processor.symDifference(SQUARE, SQUARE_DISJOINT)), TOLERANCE, "Two disjoint squares belong to exactly one of the two operands.");
        assertEquals(96, areaOf(processor.symDifference(SQUARE, SQUARE_INNER)), TOLERANCE, "A square less the square of area 4 it contains.");
        assertTrue(processor.symDifference(SQUARE, SQUARE_BIS).isEmpty(), "Every position of a square belongs to both operands when they are equal.");
    }

}
