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
 * Tests the {@code contains} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ContainsTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code contains(Geometry, Geometry)}.
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
     * All test cases of {@code contains(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // The empty set is a superset of itself only, and a subset of every geometry.
        new TestCase(EMPTY_1,          EMPTY_1,            true,  null),
        new TestCase(EMPTY_1,          EMPTY_2,            true,  null),
        new TestCase(EMPTY_1,          NON_EMPTY,          false, null),
        new TestCase(NON_EMPTY,        EMPTY_1,            true,  null),
       // points
        new TestCase(POINT_A,          POINT_A,            true,  null),
        new TestCase(POINT_A,          POINT_A_BIS,        true,  null),
        new TestCase(POINT_A,          POINT_B,            false, null),
        new TestCase(POINT_B,          POINT_A,            false, null),
        /*
         * A surface and a position. Only the positions interior to the surface are contained:
         * the center of a surface with a hole is in that hole and is therefore not.
         *
         * A position exactly on the boundary of a surface is left out of this table on purpose.
         * That case is verified by `WithinTest`, which states the same thing the other way round
         * without going through the point-in-polygon shortcut of `contains(Polygon, Point)`.
         */
        new TestCase(SQUARE,           POINT_CENTER,       true,  null),
        new TestCase(SQUARE,           POINT_OUTSIDE,      false, null),
        new TestCase(SQUARE_WITH_HOLE, POINT_CENTER,       false, null),
        /*
         * A curve and a position. An end position of the curve is on its boundary,
         * and a geometry does not contain the positions of its own boundary.
         */
        new TestCase(LINE_BOTTOM,      POINT_ON_EDGE,      true,  null),
        new TestCase(LINE_BOTTOM,      POINT_CORNER,       false, null),
        /*
         * A surface and a curve. A curve lying on the boundary of a surface is not contained
         * by it, no position of that curve being interior to the surface.
         */
        new TestCase(SQUARE,           LINE_INSIDE,        true,  null),
        new TestCase(SQUARE,           LINE_BOTTOM,        false, null),
        new TestCase(SQUARE,           LINE_CROSSING,      false, null),
        /*
         * Two surfaces. Two surfaces at the same position contain each other.
         */
        new TestCase(SQUARE,           SQUARE_INNER,       true,  null),
        new TestCase(SQUARE,           SQUARE_OVERLAP,     false, null),
        new TestCase(SQUARE,           SQUARE_BIS,         true,  null),
        /*
         * Sets of geometries. A set is contained when all of its elements are.
         */
        new TestCase(SQUARE,           MULTI_POINT_INSIDE, true,  null),
        new TestCase(SQUARE,           MULTI_POINT_SPREAD, false, null),
        new TestCase(MULTI_POLYGON,    SQUARE,             true,  null)
    };

    /**
     * Tests {@code contains(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testContains() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().contains(entry.input(), entry.other());
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
