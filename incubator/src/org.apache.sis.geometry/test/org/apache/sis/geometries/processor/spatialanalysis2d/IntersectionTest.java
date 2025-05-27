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
package org.apache.sis.geometries.processor.spatialanalysis2d;


import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.MeshPrimitive;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class IntersectionTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

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
        geom1.setPositions(TupleArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", TupleArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Points();
        geom2.setPositions(TupleArrays.of(CRS2D,
                0.2, 0.2,
                0.5, 0.5,
                1.0, 1.0,
                0.5, 2.0
                ));

        final Geometry intersection = GeometryOperations.SpatialAnalysis2D.intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Points);
        final MeshPrimitive result = (MeshPrimitive) intersection;
        final TupleArray positions = result.getPositions();
        final TupleArray test = result.getAttribute("test");

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
        geom1.setPositions(TupleArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", TupleArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Lines();
        geom2.setPositions(TupleArrays.of(CRS2D,
                0.1, 0.5,  0.9, 0.5, //crossing 2 triangles
                0.8, 0.9,  1.2, 0.9, //crossout 1 triangle
                3.0, 0.2,  4.0, 0.2  //outside
                ));

        final Geometry intersection = GeometryOperations.SpatialAnalysis2D.intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Lines);
        final MeshPrimitive.Lines result = (MeshPrimitive.Lines) intersection;
        final TupleArray positions = result.getPositions();
        final TupleArray test = result.getAttribute("test");

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
}
