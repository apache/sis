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
 * Tests the {@code crosses} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CrossesTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code crosses(Geometry, Geometry)}.
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
     * All test cases of {@code crosses(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * Crossing requires the two interiors to share a position while neither geometry
         * contains the other. The empty set has no interior, therefore it crosses nothing.
         */
        new TestCase(EMPTY_1,            NON_EMPTY,      false, null),
        new TestCase(NON_EMPTY,          EMPTY_1,        false, null),
        new TestCase(EMPTY_1,            EMPTY_1,        false, null),
        new TestCase(EMPTY_1,            EMPTY_2,        false, null),
       // points
        new TestCase(POINT_A,            POINT_A_BIS,    false, null),
        new TestCase(POINT_A,            POINT_B,        false, null),
        /*
         * A single position never crosses anything: crossing requires a position shared with
         * the interior of the other geometry and another one in its exterior, and a position
         * has only one position to offer.
         */
        new TestCase(POINT_CENTER,       SQUARE,         false, null),
        new TestCase(POINT_ON_EDGE,      LINE_BOTTOM,    false, null),
        /*
         * Two curves cross when their interiors meet at isolated positions. Sharing only an end
         * position is not crossing, and sharing a whole segment is not either: what they have in
         * common is then a curve, not a position.
         */
        new TestCase(LINE_BOTTOM,        LINE_CROSSING,  true,  null),
        new TestCase(LINE_BOTTOM,        LINE_DIAGONAL,  false, null),
        new TestCase(LINE_BOTTOM,        LINE_COLLINEAR, false, null),
        /*
         * A curve and a surface. The curve crosses when it passes through the surface, and does
         * not when it stays inside it or on its boundary.
         */
        new TestCase(LINE_CROSSING,      SQUARE,         true,  null),
        new TestCase(LINE_INSIDE,        SQUARE,         false, null),
        new TestCase(LINE_BOTTOM,        SQUARE,         false, null),
        /*
         * Two surfaces never cross: what they share is a surface, never of a lower dimension.
         */
        new TestCase(SQUARE,             SQUARE_OVERLAP, false, null),
        /*
         * A set of positions straddling a surface crosses it, which a single position cannot do.
         */
        new TestCase(MULTI_POINT_SPREAD, SQUARE,         true,  null),
        new TestCase(MULTI_POINT_INSIDE, SQUARE,         false, null)
    };

    /**
     * Tests {@code crosses(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testCrosses() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().crosses(entry.input(), entry.other());
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
