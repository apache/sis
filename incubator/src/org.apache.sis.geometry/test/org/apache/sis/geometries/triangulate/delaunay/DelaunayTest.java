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
package org.apache.sis.geometries.triangulate.delaunay;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveComparator;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.math.Array;


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
        delaunay.build(points);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{0,1,2}, triangles);
    }

    /**
     * Test triangulation of 4 points.
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
        delaunay.build(points);
        final Array triangles = delaunay.getTrianglesIndex();

        compare(positions, new int[]{0,1,2, 1,3,2}, triangles);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
        delaunay.build(points);
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
