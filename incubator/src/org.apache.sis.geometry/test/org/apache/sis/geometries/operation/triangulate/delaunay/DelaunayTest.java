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
package org.apache.sis.geometries.operation.triangulate.delaunay;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveComparator;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DelaunayTest {

    private static final CoordinateReferenceSystem CRS = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Test triangulation of 3 points.
     * 2
     * +
     * |\
     * | \
     * +--+
     * 0  1
     */
    @Test
    public void testTriangle() {

        final Array positions = NDArrays.of(CRS,
                0,0,
                1,0,
                0,1);

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{0,1,2}, triangles);
    }

    /**
     * Test triangulation of 4 points.
     * The points are on a circle, so both diagonals give a valid Delaunay triangulation.
     * Which one is chosen depends on the points insertion order.
     * 2  3
     * +--+
     * |\ |
     * | \|
     * +--+
     * 0  1
     */
    @Test
    public void testQuad() {

        final Array positions = NDArrays.of(CRS,
                0,0,
                1,0,
                0,1,
                1,1);

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        final Array triangles = delaunay.getTrianglesIndex();

        try {
            compare(positions, new int[]{0,1,2, 1,3,2}, triangles);
        } catch (IllegalArgumentException e) {
            compare(positions, new int[]{0,1,3, 0,3,2}, triangles);
        }
    }

    /**
     * Test triangulation of 4 points.
     *    2
     *    +
     *   /|\
     *  / | \
     * +--+--+
     * 0  3  1
     */
    @Test
    public void testEdgeSplit() {

        final Array positions = NDArrays.of(CRS,
                0,0,
                2,0,
                1,1,
                1,0);

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{2,0,3, 1,2,3}, triangles);
    }

    /**
     * Test triangulation of 5 points.split edge on two sides
     *      2
     *      +
     *     /|\
     *    / | \
     * 0 +--4--+ 1
     *    \ | /
     *     \|/
     *      +
     *      3
     */
    @Test
    public void testTwoEdgeSplit() {

        final Array positions = NDArrays.of(CRS,
                0,0,
                2,0,
                1,2,
                1,-2,
                1,0
                );

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{0,4,2, 4,1,2, 0,3,4, 4,3,1}, triangles);
    }

    /**
     * Test from example A of https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
     */
    @Test
    public void testFlipScanA() {
        final Array positions = NDArrays.of(CRS,
                69,-157, //start
                38,-192,
                96,-199,
                70,-237 //end
                );
        final Array constraints = NDArrays.ofUnsigned(1,
                new int[]{0,3});

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        delaunay.pushConstraint(constraints, true);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{
            0,1,3,
            0,3,2
        }, triangles);
    }

    /**
     * Test from example B of https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
     */
    @Test
    public void testFlipScanB() {
        final Array positions = NDArrays.of(CRS,
                73,-252, //start
                40,-286,
                39,-313,
                73,-360, //end
                99,-322,
                99,-294
                );
        final Array constraints = NDArrays.ofUnsigned(1,
                new int[]{0,3});

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        delaunay.pushConstraint(constraints, true);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{
            0,1,2,
            0,2,3,
            0,3,4,
            0,4,5
        }, triangles);
    }

    /**
     * Test from example C of https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
     *
     * Note : we need to insert a soft constraint from 2 to 5 to match the example
     */
    @Test
    public void testFlipScanC() {
        final Array positions = NDArrays.of(CRS,
                84,-368, //start
                53,-402,
                20,-413,
                51,-466,
                86,-510, //end
                112,-439,
                111,-411
                );
        final Array constraints = NDArrays.ofUnsigned(1,
                new int[]{0,4});

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        //add a soft constraint to match the example
        delaunay.pushConstraint(2,5, false);
        delaunay.pushConstraint(constraints, true);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{
            0,1,3,
            1,2,3,
            0,3,4,
            0,4,5,
            0,5,6,
            //an additional triangle create here to have all delaunay triangles
            0,2,1
        }, triangles);
    }

    /**
     * Test from example D of https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
     *
     * Note : we need to insert soft constraints 1-5 and 2-5 to match the example
     */
    @Test
    public void testFlipScanD() {
        final Array positions = NDArrays.of(CRS,
                91,-524, //start
                82,-559,
                37,-578,
                64,-605,
                92,-660, //end
                128,-611,
                104,-571
                );
        final Array constraints = NDArrays.ofUnsigned(1,
                new int[]{0,4});

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        //add a soft constraint to match the example
        delaunay.pushConstraint(1,5, false);
        delaunay.pushConstraint(2,5, false);
        delaunay.pushConstraint(constraints, true);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{
            0,1,4,
            1,2,3,
            1,3,4,
            0,4,6,
            4,5,6,
            //an additional triangle create here to have all delaunay triangles
            0,2,1
        }, triangles);
    }

    /**
     * Test from example E of https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
     *
     * Note : we need to insert soft constraints 1-5 and 2-5 to match the example
     */
    @Test
    public void testFlipScanE() {
        final Array positions = NDArrays.of(CRS,
                84,-674, //start
                80,-715,
                55,-731,
                52,-778,
                85,-837, //end
                94,-806,
                124,-749,
                97,-724
                );
        final Array constraints = NDArrays.ofUnsigned(1,
                new int[]{0,4});

        final MeshPrimitive.Points points = new MeshPrimitive.Points();
        points.setPositions(positions);

        final Delaunay delaunay = new Delaunay();
        delaunay.build(points, false);
        delaunay.pushConstraint(constraints, true);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{
            0,1,4,
            1,2,4,
            2,3,4,
            0,4,5,
            0,5,7,
            5,6,7,
            //additional triangles create here to have all delaunay triangles
            0,2,1,
            0,7,6
        }, triangles);
    }

    /**
     * Small triangles far from the origin, as longitudes and latitudes, with collinear points.
     * Floating point circle tests are not accurate enough for those coordinates and could flip
     * non convex quadrilaterals, creating inverted triangles. All triangles shall be counterclockwise
     * and the constraints shall be edges of the triangulation.
     */
    @Test
    public void testSmallFarTriangles() {
        final double x0 = 5.386590116164263, y0 = 43.31551467671114;
        final double[] xy = {
            x0, y0,                                         // 0
            x0 + 3.082451586e-6, y0 + 3.082451585e-6,       // 1, almost collinear with 0 and 4
            5.386562347412109, 43.31554111681486,           // 2
            5.386562347412109, 43.315521240234375,          // 3
            5.386607056910576, 43.31553161745745,           // 4
            x0 + 6.1649e-6, y0 + 6.1649e-6,                 // 5, on the same line
            x0 + 1.2e-5, y0 - 4.1e-6,                       // 6
            x0 - 7.3e-6, y0 - 9.8e-6,                       // 7
            x0 + 2.2e-5, y0 + 3.0e-6,                       // 8
            x0 - 1.1e-5, y0 + 2.9e-5                        // 9
        };
        final int[] constraints = {0,1, 1,5, 5,4, 3,0, 7,6, 6,8};
        final int[] triangles = Delaunay.triangulate(xy, constraints, false);
        assertTrue(triangles.length >= 3*8, "Missing triangles.");
        final java.util.Set<Long> edges = new java.util.HashSet<>();
        for (int i = 0; i < triangles.length; i += 3) {
            final int a = triangles[i]*2, b = triangles[i+1]*2, c = triangles[i+2]*2;
            assertTrue(Maths.lineSide(xy[a], xy[a+1], xy[b], xy[b+1], xy[c], xy[c+1]) > 0, "Inverted or flat triangle.");
            for (int k = 0; k < 3; k++) {
                final int u = triangles[i+k], v = triangles[i+(k+1)%3];
                edges.add(((long) Math.min(u, v) << 32) | Math.max(u, v));
            }
        }
        for (int i = 0; i < constraints.length; i += 2) {
            final int u = constraints[i], v = constraints[i+1];
            assertTrue(edges.contains(((long) Math.min(u, v) << 32) | Math.max(u, v)), "Missing constraint " + u + "-" + v);
        }
    }

    /**
     * Compare created index, ignoring order of elements.
     */
    private static void compare(Array positions, int[] expected, Array actual) {
        final MeshPrimitive.Triangles exp = new MeshPrimitive.Triangles();
        exp.setPositions(positions);
        exp.setIndex(NDArrays.ofUnsigned(1, expected));
        final MeshPrimitive.Triangles act = new MeshPrimitive.Triangles();
        act.setPositions(positions);
        act.setIndex(actual);

        final MeshPrimitiveComparator comparator = new MeshPrimitiveComparator();
        comparator.compareByElement(true);
        comparator.compare(exp, act);
    }
}
