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
 * Tests the {@code intersects} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class IntersectsTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code intersects(Geometry, Geometry)}.
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
     * All test cases of {@code intersects(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // Negation of `disjoint(Geometry, Geometry)`.
        new TestCase(EMPTY_1,            NON_EMPTY,        false, null),
        new TestCase(NON_EMPTY,          EMPTY_1,          false, null),
        new TestCase(EMPTY_1,            EMPTY_1,          false, null),
        new TestCase(EMPTY_1,            EMPTY_2,          false, null),
        //points
        new TestCase(POINT_A,            POINT_A_BIS,      true,  null),
        new TestCase(POINT_A,            POINT_B,          false, null),
        new TestCase(POINT_B,            POINT_A,          false, null),
        /*
         * A position and a surface. A position on the boundary of a surface meets it, and the
         * center of a surface with a hole does not, that position being in the hole.
         */
        new TestCase(POINT_CENTER,       SQUARE,           true,  null),
        new TestCase(POINT_ON_EDGE,      SQUARE,           true,  null),
        new TestCase(POINT_OUTSIDE,      SQUARE,           false, null),
        new TestCase(POINT_CENTER,       SQUARE_WITH_HOLE, false, null),
        /*
         * A position and a curve, the position being interior to the curve, an end of it,
         * or on neither.
         */
        new TestCase(POINT_ON_EDGE,      LINE_BOTTOM,      true,  null),
        new TestCase(POINT_CORNER,       LINE_BOTTOM,      true,  null),
        new TestCase(POINT_OUTSIDE,      LINE_BOTTOM,      false, null),
        /*
         * Two curves crossing each other, sharing only an end position, or sharing nothing.
         */
        new TestCase(LINE_BOTTOM,        LINE_CROSSING,    true,  null),
        new TestCase(LINE_BOTTOM,        LINE_DIAGONAL,    true,  null),
        new TestCase(LINE_BOTTOM,        LINE_FAR,         false, null),
        /*
         * A curve and a surface, the curve passing through it, lying on its boundary, or
         * staying away from it.
         */
        new TestCase(LINE_CROSSING,      SQUARE,           true,  null),
        new TestCase(LINE_BOTTOM,        SQUARE,           true,  null),
        new TestCase(LINE_FAR,           SQUARE,           false, null),
        /*
         * Two surfaces overlapping, sharing only an edge, or sharing nothing.
         */
        new TestCase(SQUARE,             SQUARE_OVERLAP,   true,  null),
        new TestCase(SQUARE,             SQUARE_TOUCHING,  true,  null),
        new TestCase(SQUARE,             SQUARE_DISJOINT,  false, null),
        /*
         * Sets of geometries. A set meets another geometry as soon as one of its elements does.
         */
        new TestCase(MULTI_POINT_SPREAD, SQUARE,           true,  null),
        new TestCase(MULTI_LINE,         SQUARE,           true,  null),
        new TestCase(MULTI_POLYGON,      SQUARE_INNER,     true,  null),
        new TestCase(COLLECTION,         SQUARE,           true,  null)
    };

    /**
     * Tests {@code intersects(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testIntersects() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().intersects(entry.input(), entry.other());
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
