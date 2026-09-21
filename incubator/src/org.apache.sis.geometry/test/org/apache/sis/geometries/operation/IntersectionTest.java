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
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.referencing.CommonCRS;
import static org.apache.sis.geometries.operation.TestData.EMPTY_1;
import static org.apache.sis.geometries.operation.TestData.EMPTY_2;
import static org.apache.sis.geometries.operation.TestData.NON_EMPTY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class IntersectionTest {

    private static final SampleSystem CRS2D = SampleSystem.of(CommonCRS.WGS84.geographic());

    /**
     * Test primitives triangles against primitive points.
     * - 2 triangles.
     * - 4 points, one in triangle, one on vertex, one on edge, one outside.
     *
     * 2-4  5
     *  +---o
     *  |\  |
     *  | o | o
     *  |o \|
     *  +---+
     *  0  1-3
     */
    @Test
    public void testPrimitiveTrianglesPrimitivePoints() {

        final MeshPrimitive geom1 = new MeshPrimitive.Triangles();
        geom1.setPositions(NDArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", NDArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Points();
        geom2.setPositions(NDArrays.of(CRS2D,
                0.2, 0.2,
                0.5, 0.5,
                1.0, 1.0,
                0.5, 2.0
                ));

        final Geometry intersection = new GeometryProcessor().intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Points);
        final MeshPrimitive result = (MeshPrimitive) intersection;
        final Array positions = result.getPositions();
        final Array test = result.getAttribute("test");

        assertArrayEquals(new double[]{
                0.2, 0.2,
                0.5, 0.5,
                1.0, 1.0
        }, positions.toArrayDouble(), 0.0);
        assertArrayEquals(new double[]{
                0.6,
                1.5,
                8.0,
        }, test.toArrayDouble(), 0.0001);
    }

    /**
     * Test primitives triangles against primitive lines.
     * - 2 triangles.
     * - 3 lines, one inside triangles, one crossing outward, one outside
     *
     *   2-4  5
     *    +---+
     *    |\ ++--+
     *    |+-+|
     *    |  \|   +--+
     *    +---+
     *    0  1-3
     */
    @Test
    public void testPrimitiveTrianglesPrimitiveLines() {

        final MeshPrimitive geom1 = new MeshPrimitive.Triangles();
        geom1.setPositions(NDArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", NDArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Lines();
        geom2.setPositions(NDArrays.of(CRS2D,
                0.1, 0.5,  0.9, 0.5, //crossing 2 triangles
                0.8, 0.9,  1.2, 0.9, //crossout 1 triangle
                3.0, 0.2,  4.0, 0.2  //outside
                ));

        final Geometry intersection = new GeometryProcessor().intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Lines);
        final MeshPrimitive.Lines result = (MeshPrimitive.Lines) intersection;
        final Array positions = result.getPositions();
        final Array test = result.getAttribute("test");

        assertArrayEquals(new double[]{
                //first line, cut in two
                0.1, 0.5,  0.5, 0.5,
                0.5, 0.5,  0.9, 0.5,
                //second line, truncated
                0.8, 0.9,  1.0, 0.9
                //no third line
        }, positions.toArrayDouble(), 0.0001);
        assertArrayEquals(new double[]{
                1.1, 1.5,
                1.5, 3.9,
                6.1, 7.3
        }, test.toArrayDouble(), 0.0001);
    }

    /**
     * The inputs and expected result of a single test of {@code intersection(Geometry, Geometry)}.
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
     * All test cases of {@code intersection(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // ∅ ∩ A = ∅ and A ∩ ∅ = ∅: the result is the operand which is already empty.
        new TestCase(EMPTY_1,   NON_EMPTY, EMPTY_1, null),
        new TestCase(NON_EMPTY, EMPTY_1,   EMPTY_1, null),
        new TestCase(EMPTY_1,   EMPTY_2,   EMPTY_1, null)
    };

    /**
     * Tests {@code intersection(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testIntersection() {
        for (final TestCase entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().intersection(entry.input(), entry.other());
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
