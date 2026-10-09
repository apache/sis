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

import org.apache.sis.geometries.DE9IM;
import org.apache.sis.geometries.Geometry;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code relate} operation of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class RelateTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code relate(Geometry, Geometry, DE9IM)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param matrix   the dimensionally extended nine-intersection matrix.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry other,
                            DE9IM matrix,
                            Boolean expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code relate(Geometry, Geometry, DE9IM)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * The interior and the boundary of the empty set meet nothing, while its exterior is the
         * whole space. Two empty geometries therefore meet only by their exteriors.
         */
        new TestCase(EMPTY_1,        EMPTY_2,         DE9IM.valueOf("FFFFFFFF2"), true,  null),
        /*
         * Between the empty set and a point, the only non-empty intersections are those of the
         * exterior of the empty set with the point, which is a position, and with the exterior
         * of the point, which is the rest of the plane. The boundary of a point being empty,
         * the boundary row and the boundary column stay empty in both cases.
         */
        new TestCase(EMPTY_1,        NON_EMPTY,       DE9IM.valueOf("FFFFFF0F2"), true,  null),
        new TestCase(NON_EMPTY,      EMPTY_1,         DE9IM.valueOf("FF0FFFFF2"), true,  null),
        /*
         * Two points at the same position have their interiors in common and nothing else.
         * Two points at different positions have no position in common, each one lying in
         * the exterior of the other.
         */
        new TestCase(POINT_A,        POINT_A_BIS,     DE9IM.valueOf("0FFFFFFF2"), true,  null),
        new TestCase(POINT_A,        POINT_B,         DE9IM.valueOf("FF0FFF0F2"), true,  null),
        new TestCase(POINT_A,        POINT_B,         DE9IM.valueOf("0FFFFFFF2"), false, null),
        /*
         * A position and a surface. The three rows of the matrix describe the interior, the
         * boundary and the exterior of the position, and its three columns those of the surface.
         * The boundary of a position being empty, the second row is always empty. The exterior
         * of a position meets the whole surface, hence the "212" of the last row in the three
         * cases below.
         *
         * The position is interior to the surface, then on its boundary, then outside of it.
         */
        new TestCase(POINT_CENTER,   SQUARE,          DE9IM.valueOf("0FFFFF212"), true,  null),
        new TestCase(POINT_ON_EDGE,  SQUARE,          DE9IM.valueOf("F0FFFF212"), true,  null),
        new TestCase(POINT_OUTSIDE,  SQUARE,          DE9IM.valueOf("FF0FFF212"), true,  null),
        /*
         * Two surfaces at the same position: their interiors cover each other, their boundaries
         * are the same curve, and neither meets the exterior of the other.
         */
        new TestCase(SQUARE,         SQUARE_BIS,      DE9IM.valueOf("2FFF1FFF2"), true,  null),
        /*
         * Two surfaces sharing nothing: each one lies entirely in the exterior of the other.
         */
        new TestCase(SQUARE,         SQUARE_DISJOINT, DE9IM.valueOf("FF2FF1212"), true,  null),
        /*
         * A surface strictly inside another one: its boundary meets the interior of the other,
         * never its boundary.
         */
        new TestCase(SQUARE_INNER,   SQUARE,          DE9IM.valueOf("2FF1FF212"), true,  null),
        /*
         * The same pair against the pattern which defines `within`, to verify that the wildcards
         * of a pattern accept any dimension.
         */
        new TestCase(SQUARE_INNER,   SQUARE,          DE9IM.valueOf("T*F**F***"), true,  null),
        new TestCase(SQUARE_OVERLAP, SQUARE,          DE9IM.valueOf("T*F**F***"), false, null)
    };

    /**
     * Tests {@code relate(Geometry, Geometry, DE9IM)} on all declared test cases.
     */
    @Test
    public void testRelate() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().relate(entry.input(), entry.other(), entry.matrix());
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
