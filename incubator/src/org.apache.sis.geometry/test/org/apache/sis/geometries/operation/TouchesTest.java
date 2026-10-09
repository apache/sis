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
 * Tests the {@code touches} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TouchesTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code touches(Geometry, Geometry)}.
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
     * All test cases of {@code touches(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * Touching requires a shared position which is in the boundary of at least one of the
         * two geometries. The empty set has no boundary, therefore it touches nothing.
         */
        new TestCase(EMPTY_1,       NON_EMPTY,       false, null),
        new TestCase(NON_EMPTY,     EMPTY_1,         false, null),
        new TestCase(EMPTY_1,       EMPTY_1,         false, null),
        new TestCase(EMPTY_1,       EMPTY_2,         false, null),
        /*
         * Two points cannot touch: the boundary of a point is empty, therefore the only
         * position they may share is interior to both of them.
         */
        new TestCase(POINT_A,       POINT_A_BIS,     false, null),
        new TestCase(POINT_A,       POINT_B,         false, null),
        /*
         * A position and a surface. Touching requires a shared position which is interior to
         * neither geometry, which for a position means that it lies on the boundary.
         */
        new TestCase(POINT_ON_EDGE, SQUARE,          true,  null),
        new TestCase(POINT_CENTER,  SQUARE,          false, null),
        new TestCase(POINT_OUTSIDE, SQUARE,          false, null),
        /*
         * A position and a curve, the position being an end of the curve or interior to it.
         */
        new TestCase(POINT_CORNER,  LINE_BOTTOM,     true,  null),
        new TestCase(POINT_ON_EDGE, LINE_BOTTOM,     false, null),
        /*
         * Two curves. They touch when they share only an end position, and do not when their
         * interiors meet, whether they meet at one position or along a whole segment.
         */
        new TestCase(LINE_BOTTOM,   LINE_DIAGONAL,   true,  null),
        new TestCase(LINE_BOTTOM,   LINE_CROSSING,   false, null),
        new TestCase(LINE_BOTTOM,   LINE_COLLINEAR,  false, null),
        /*
         * A curve and a surface. A curve lying on the boundary of a surface touches it,
         * no position of it being interior to the surface.
         */
        new TestCase(LINE_BOTTOM,   SQUARE,          true,  null),
        new TestCase(LINE_INSIDE,   SQUARE,          false, null),
        /*
         * Two surfaces sharing a whole edge but no interior position.
         */
        new TestCase(SQUARE,        SQUARE_TOUCHING, true,  null),
        new TestCase(SQUARE,        SQUARE_OVERLAP,  false, null),
        new TestCase(SQUARE,        SQUARE_INNER,    false, null),
        new TestCase(SQUARE,        SQUARE_DISJOINT, false, null)
    };

    /**
     * Tests {@code touches(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testTouches() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().touches(entry.input(), entry.other());
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
