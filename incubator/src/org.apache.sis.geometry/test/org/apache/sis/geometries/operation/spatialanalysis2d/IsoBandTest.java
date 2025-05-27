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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.MeshPrimitive;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.TupleArrays;
import static org.apache.sis.geometries.operation.spatialanalysis2d.ISOLine.interpolateToCoord2D;
import org.apache.sis.measure.NumberRange;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.FactoryException;

public class IsoBandTest {

    static double tolerance;

    @BeforeAll
    public static void setUp() {
        tolerance = 0.0001;
    }

    @Test
    public void testInterpolateToCoord2D() {

        final double[] p0 = new double[]{0, 0, 1.5};
        final double[] p1 = new double[]{2, 0, 1.5};
        assertThrows(IllegalStateException.class, () -> interpolateToCoord2D(p0, p1, 1));

        double[] pX = new double[]{2, 0, 0.5};
        Coordinate interpolated = interpolateToCoord2D(p0, pX, 1);
        assertNotNull(interpolated);
        assertArrayEquals(new double[]{1, 0, 1}, new double[]{interpolated.getX(), interpolated.getY(), interpolated.getZ()}, tolerance);

        pX = new double[]{0, 2, 0.5};
        interpolated = interpolateToCoord2D(p0, pX, 1);
        assertNotNull(interpolated);
        assertArrayEquals(new double[]{0, 1, 1}, new double[]{interpolated.getX(), interpolated.getY(), interpolated.getZ()}, tolerance);

        pX = new double[]{2, 2, 0.5};
        interpolated = interpolateToCoord2D(p0, pX, 1);
        assertNotNull(interpolated);
        assertArrayEquals(new double[]{1, 1, 1}, new double[]{interpolated.getX(), interpolated.getY(), interpolated.getZ()}, tolerance);

        assertThrows(IllegalStateException.class, () -> interpolateToCoord2D(p0, new double[]{2, 2, 1}, 0.5));

        final double[] p2 = new double[]{0, 0, 2};
        final double[] p3 = new double[]{2, 0, 0.5};
        Coordinate interpolated1 = interpolateToCoord2D(p2, p3, 1);
        Coordinate interpolated2 = interpolateToCoord2D(p3, p2, 1);
        assertArrayEquals(new double[]{interpolated1.getX(), interpolated1.getY(), interpolated1.getZ()},
                new double[]{interpolated2.getX(), interpolated2.getY(), interpolated2.getZ()},
                tolerance);
    }

    @Test
    public void testAllOnBand() {
        List<Polygon> testedPolygons = new ArrayList<>();

        final double minStep = 1.;
        final double maxStep = 2.;

        final double[] p0 = new double[]{0, 0, 1.5};
        final double[] p1 = new double[]{2, 0, 1.5};
        final double[] p2 = new double[]{1, 1, 1.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");

        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();
        assertEquals(4, coordinates.length);

        // Check coordinates
        Coordinate[] expected = new Coordinate[]{
                new Coordinate(p0[0], p0[1], p0[2]),
                new Coordinate(p1[0], p1[1], p1[2]),
                new Coordinate(p2[0], p2[1], p2[2]),
                new Coordinate(p0[0], p0[1], p0[2])
        };

        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
        testedPolygons.clear();

        ISOBand.computeStepBand(minStep, maxStep, p1, p2, p0, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");

        testedPolygon = testedPolygons.get(0);
        coordinates = testedPolygon.getCoordinates();

        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
        testedPolygons.clear();

        ISOBand.computeStepBand(minStep, maxStep, p1, p0, p2, testedPolygons);

        assertNotNull(testedPolygons);
        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");

        testedPolygon = testedPolygons.get(0);
        coordinates = testedPolygon.getCoordinates();

        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
        testedPolygons.clear();
    }

    @Test
    public void testAllBelow() {
        List<Polygon> testedPolygons = new ArrayList<>();

        final double minStep = 1.;
        final double maxStep = 2.;


        final double[] p0 = new double[]{0, 0, 0.5};
        final double[] p1 = new double[]{2, 0, 0.5};
        final double[] p2 = new double[]{1, 1, 0.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(0, testedPolygons.size(), "No polygon intersect the band.");
    }

    @Test
    public void testAllAbove() {
        List<Polygon> testedPolygons = new ArrayList<>();

        final double minStep = 1.;
        final double maxStep = 2.;


        double[] p0 = new double[]{0, 0, 2.1};
        double[] p1 = new double[]{2, 0, 2.1};
        double[] p2 = new double[]{1, 1, 2.1};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(0, testedPolygons.size(), "No polygon should intersect the band.");
        testedPolygons.clear();

        p0 = new double[]{0, 0, 2.5};
        p1 = new double[]{2, 0, 2.5};
        p2 = new double[]{1, 1, 2.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(0, testedPolygons.size(), "No polygon should intersect the band.");
    }

    @Test
    public void test1In2Under() {
        List<Polygon> testedPolygons = new ArrayList<>();

        final double minStep = 1.;
        final double maxStep = 2.;


        double[] p0 = new double[]{0, 0, 1.5};
        double[] p1 = new double[]{2, 0, 0.5};
        double[] p2 = new double[]{1, 1, 0.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");

        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        // Check each coordinate
        Coordinate[] expected = new Coordinate[]{
                new Coordinate(p0[0], p0[1], p0[2]),
                interpolateToCoord2D(p1, p0, minStep),
                interpolateToCoord2D(p2, p0, minStep),
                new Coordinate(p0[0], p0[1], p0[2])
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order."
            );
        testedPolygons.clear();

        p0 = new double[]{2, 0, 1.5};
        p1 = new double[]{0, 0, 0.5};
        p2 = new double[]{1, 1, 0.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");

        testedPolygon = testedPolygons.get(0);
        coordinates = testedPolygon.getCoordinates();

        // Check each coordinate
        expected = new Coordinate[]{
                new Coordinate(p0[0], p0[1], p0[2]),
                interpolateToCoord2D(p1, p0, minStep),
                interpolateToCoord2D(p2, p0, minStep),
                new Coordinate(p0[0], p0[1], p0[2])
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }

    @Test
    public void test2In1Under() {
        List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{0, 0, 1.5};
        double[] p1 = new double[]{2, 0, 1.5};
        double[] p2 = new double[]{1, 1, 0.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        // Check each coordinate
        Coordinate[] expected = new Coordinate[]{
                new Coordinate(p0[0], p0[1], p0[2]),
                new Coordinate(p1[0], p1[1], p1[2]),
                interpolateToCoord2D(p1, p2, minStep),
                interpolateToCoord2D(p2, p0, minStep),
                new Coordinate(p0[0], p0[1], p0[2])
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
        testedPolygons.clear();


        p0 = new double[]{2, 0, 1.6};
        p1 = new double[]{1, 1, 1.5};
        p2 = new double[]{0, 0, 0.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");
        testedPolygon = testedPolygons.get(0);
        coordinates = testedPolygon.getCoordinates();


        // Check each coordinate
        expected = new Coordinate[]{
                new Coordinate(p0[0], p0[1], p0[2]),
                new Coordinate(p1[0], p1[1], p1[2]),
                interpolateToCoord2D(p1, p2, minStep),
                interpolateToCoord2D(p2, p0, minStep),
                new Coordinate(p0[0], p0[1], p0[2])
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }

    @Test
    public void test2In1Above() {
        List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{0, 0, 2.5};
        double[] p1 = new double[]{2, 0, 1.5};
        double[] p2 = new double[]{1, 1, 1.2};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        // Check each coordinate
        Coordinate[] expected = new Coordinate[]{
                new Coordinate(p1[0], p1[1], p1[2]),
                new Coordinate(p2[0], p2[1], p2[2]),
                interpolateToCoord2D(p0, p2, maxStep),
                interpolateToCoord2D(p1, p0, maxStep),
                new Coordinate(p1[0], p1[1], p1[2])
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }

    @Test
    public void test2Above1In() {
        List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{2, 0, 4.5};
        double[] p1 = new double[]{0, 0, 3.5};
        double[] p2 = new double[]{1, 1, 1.5};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(),
                "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        assertCoordinateEquals(
                interpolateToCoord2D(p0, p2, maxStep),
                interpolateToCoord2D(p2, p0, maxStep), tolerance);

        // Check each coordinate
        Coordinate[] expected = new Coordinate[]{
                new Coordinate(p2[0], p2[1], p2[2]),
                interpolateToCoord2D(p1, p2, maxStep),
                interpolateToCoord2D(p0, p2, maxStep),
                new Coordinate(p2[0], p2[1], p2[2]),
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }

    @Test
    public void test1Above2Below() {
        List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{2, 0, 4.5};
        double[] p1 = new double[]{0, 0, 0.5};
        double[] p2 = new double[]{1, 1, 0.1};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        // Check each coordinate
        Coordinate p0_p1_max = interpolateToCoord2D(p0, p1, maxStep);
        Coordinate[] expected = new Coordinate[]{
                p0_p1_max,
                interpolateToCoord2D(p0, p1, minStep),
                interpolateToCoord2D(p0, p2, minStep),
                interpolateToCoord2D(p0, p2, maxStep),
                p0_p1_max
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }


    @Test
    public void test2Above1Below() {
        final List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{2, 0, 4.5};
        double[] p1 = new double[]{0, 0, 3.5};
        double[] p2 = new double[]{1, 1, 0.1};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(),
                "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        assertEquals(5, coordinates.length);

        // Check each coordinate
        Coordinate p0_p2_min = interpolateToCoord2D(p0, p2, minStep);
        Coordinate[] expected = new Coordinate[]{
                p0_p2_min,
                interpolateToCoord2D(p0, p2, maxStep),
                interpolateToCoord2D(p1, p2, maxStep),
                interpolateToCoord2D(p1, p2, minStep),
                p0_p2_min
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");
    }


    @Test
    public void test1Above1In1Below() {
        final List<Polygon> testedPolygons = new ArrayList<>();
        final double minStep = 1.;
        final double maxStep = 2.;

        double[] p0 = new double[]{2, 0, 4.5};
        double[] p1 = new double[]{0, 0, 1.5};
        double[] p2 = new double[]{1, 1, 0.1};

        ISOBand.computeStepBand(minStep, maxStep, p0, p1, p2, testedPolygons);

        assertEquals(1, testedPolygons.size(), "There should be exactly one polygon");
        Polygon testedPolygon = testedPolygons.get(0);
        Coordinate[] coordinates = testedPolygon.getCoordinates();

        assertEquals(6, coordinates.length);

        // Check each coordinate
        final Coordinate p0_p1_max = interpolateToCoord2D(p1, p0, maxStep);
        Coordinate[] expected = new Coordinate[]{
                p0_p1_max,
                new Coordinate(p1[0], p1[1], p1[2]),
                interpolateToCoord2D(p1, p2, minStep),
                interpolateToCoord2D(p2, p0, minStep),
                interpolateToCoord2D(p2, p0, maxStep),
                p0_p1_max
        };
        assertTrue(assertPolygonEquals(expected, coordinates, tolerance),
                "Computed Polygon vertices "+Arrays.toString(coordinates)+"\ndoesn't match the expected polygon's coordinate     " +Arrays.toString(expected)+"\nup to permutation and order.");

    }

    @Test
    public void testBandStrategy() throws FactoryException {

        /*
            3  2
            +--+
            |\ |
            | \|
            +--+
            0  1
        */
        final double[] positions = new double[]{0, 0, 1., 1, 0, 1., 1, 1, 2., 0, 1, 1.};
        final int[] index = new int[]{3, 1, 0, 3, 2, 1};

        final SampleSystem ss = SampleSystem.of(Geometries.PSEUDOGEO_3D);

        final MeshPrimitive.Triangles triangles = new MeshPrimitive.Triangles();
        triangles.setPositions(TupleArrays.of(ss, positions));
        triangles.setIndex(TupleArrays.ofUnsigned(1, index));

        //Check BOTH
        final double[] steps = new double[]{0., 1., 2.};
        Map<NumberRange<Double>, List<Polygon>> resultBothIn = ISOBand.create(triangles, steps, IsoInclusion.BOTH);

        NumberRange<Double> expectedRange0 = NumberRange.create(0., true, 1., true);
        NumberRange<Double> expectedRange1 = NumberRange.create(1., true, 2., true);

        List<Polygon> b0 = resultBothIn.get(expectedRange0);
        assertNotNull(b0);
        assertEquals(1, b0.size());
        Coordinate[] expectedB0 = new Coordinate[]{
                new Coordinate(0, 0, 1.),
                new Coordinate(1, 0, 1.),
                new Coordinate(0, 1, 1.),
                new Coordinate(0, 0, 1.),
        };
        Coordinate[] expectedB1 = new Coordinate[]{
                new Coordinate(0, 0, 1.),
                new Coordinate(1, 0, 1.),
                new Coordinate(1, 1, 2.),
                new Coordinate(0, 1, 1.),
                new Coordinate(0, 0, 1.),
        };

        assertTrue(assertPolygonEquals(expectedB0, b0.get(0).getCoordinates(), tolerance));
        List<Polygon> b1 = resultBothIn.get(expectedRange1);

        assertNotNull(b1);
        assertEquals(1, b1.size());
        assertTrue(assertPolygonEquals(expectedB1, b1.get(0).getCoordinates(), tolerance));


        //Check MAX
        Map<NumberRange<Double>, List<Polygon>> resultMinOut = ISOBand.create(triangles, steps, IsoInclusion.MAX);

        b0 = resultMinOut.get(expectedRange0);
        assertNotNull(b0);
        assertEquals(1, b0.size());

        expectedB0 = new Coordinate[]{
                new Coordinate(0, 0, 1.),
                new Coordinate(1, 0, 1.),
                new Coordinate(0, 1, 1.),
                new Coordinate(0, 0, 1.),
        };

        assertTrue(assertPolygonEquals(expectedB0, b0.get(0).getCoordinates(), tolerance));


        /*
            3  2
            +--+
             \ |
              \|
               +
               1
          check triangle 3, 1, 0 on iso of z =1. is excluded
        */
        expectedB1 = new Coordinate[]{
                new Coordinate(1, 0, 1.),
                new Coordinate(1, 1, 2.),
                new Coordinate(0, 1, 1.),
                new Coordinate(1, 0, 1.),
        };
        b1 = resultMinOut.get(expectedRange1);

        assertNotNull(b1);
        assertEquals(1, b1.size());

        assertTrue(assertPolygonEquals(expectedB1, b1.get(0).getCoordinates(), tolerance));


        //Check MIN
        Map<NumberRange<Double>, List<Polygon>> resultMaxOut = ISOBand.create(triangles, steps, IsoInclusion.MIN);

        b0 = resultMaxOut.get(expectedRange0);
        assertNotNull(b0);
        assertEquals(0, b0.size());
        expectedB1 = new Coordinate[]{
                new Coordinate(1, 0, 1.),
                new Coordinate(1, 1, 2.),
                new Coordinate(0, 1, 1.),
                new Coordinate(0, 0, 1.),
                new Coordinate(1, 0, 1.),
        };

        b1 = resultMaxOut.get(expectedRange1);

        assertTrue(assertPolygonEquals(expectedB1, b1.get(0).getCoordinates(), tolerance));
    }

    @Test
    public void testExcludeMaxStrategy() throws FactoryException {

        /*
            3  2
            +--+
            |\ |
            | \|
            +--+
            0  1
        */
        final double[] positions = new double[]{0, 0, 1., 1, 0, 2., 1, 1, 2., 0, 1, 2.};
        final int[] index = new int[]{3, 1, 0, 3, 2, 1};

        final SampleSystem ss = SampleSystem.of(Geometries.PSEUDOGEO_3D);

        final MeshPrimitive.Triangles triangles = new MeshPrimitive.Triangles();
        triangles.setPositions(TupleArrays.of(ss, positions));
        triangles.setIndex(TupleArrays.ofUnsigned(1, index));

        final double[] steps = new double[]{0., 1., 2.};

        NumberRange<Double> expectedRange0 = NumberRange.create(0., true, 1., true);
        NumberRange<Double> expectedRange1 = NumberRange.create(1., true, 2., true);

        Map<NumberRange<Double>, List<Polygon>> resultMaxOut = ISOBand.create(triangles, steps, IsoInclusion.MIN);

        List<Polygon> b0 = resultMaxOut.get(expectedRange0);
        assertNotNull(b0);
        assertEquals(0, b0.size());

        /*
            3
            +
            |\
            | \
            +--+
            0  1
            Check triangle 3, 2, 1 on iso of z =2. is excluded
        */
        Coordinate[] expectedB1 = new Coordinate[]{
                new Coordinate(0, 0, 1.),
                new Coordinate(0, 1, 2.),
                new Coordinate(1, 0, 2.),
                new Coordinate(0, 0, 1.),
        };

        List<Polygon> b1 = resultMaxOut.get(expectedRange1);

        assertTrue(assertPolygonEquals(expectedB1, b1.get(0).getCoordinates(), tolerance));
    }

    private void assertCoordinateEquals(Coordinate expected, Coordinate actual,  double tolerance) {
        assertEquals(expected.x, actual.x, tolerance, "(x-coordinate miss match)");
        assertEquals(expected.y, actual.y, tolerance, "(y-coordinate miss match)");
        assertEquals(expected.z, actual.z, tolerance, "(z-coordinate miss match)");
    }

    private boolean isCoordinateEquals(Coordinate expected, Coordinate actual, double tolerance) {
        return (Math.abs(expected.getX() - actual.getX()) <= tolerance)
                && (Math.abs(expected.getY() - actual.getY()) <= tolerance)
                && (!Double.isNaN(expected.getZ()) ? Math.abs(expected.getZ() - actual.getZ()) <= tolerance : Double.isNaN(actual.getZ()));
    }

    private boolean assertPolygonEquals(Coordinate[] expectedCoordinates, Coordinate[] checkCoordinates, final double tolerance) {
        return assertPolygonEquals(
                new ArrayList(Arrays.asList(expectedCoordinates)),  // can be modified in assertion method.
                Arrays.asList(checkCoordinates), tolerance);
    }

    /**
     * Check that 2 list of coordinates associated with closed polygons' vertices match each other.
     * The comparison accepts a precision gap (according to the input tolerance value) as well as permutation and vertex
     * ordering
     * @param expectedCoordinates expected list of coordinates (not null or empty)
     * @param checkCoordinates compared  list of coordinates (not null or empty)
     * @param tolerance tolerance value for the ordinates values.
     * @return true if the list of coordinates are matching
     */
    private boolean assertPolygonEquals(List<Coordinate> expectedCoordinates, List<Coordinate> checkCoordinates, final double tolerance) {
        int size = checkCoordinates.size();
        assert size > 1;
        Coordinate check = checkCoordinates.get(0);
        if (expectedCoordinates.size() != size) throw new RuntimeException("Computed polygon doesn't match the expected polygon's size.\nExpected :"+expectedCoordinates+"\nActual   :"+checkCoordinates);
        final int maxI = size-1;
        if (!isCoordinateEquals(check, checkCoordinates.get(size-1), tolerance)) throw new RuntimeException("Expected polygon to be closed : first coordinate must match the last coordinate.\nExpected :"+expectedCoordinates+"\nActual   :"+checkCoordinates);

        int start= -1;
        boolean sameOrder = false;

        for (int j = 0; j < maxI ; j++) {// do not re-check closing vertex
            if (isCoordinateEquals(expectedCoordinates.get(j), check, tolerance)) {
                start = j;
                sameOrder = isCoordinateEquals(expectedCoordinates.get(j+1), checkCoordinates.get(1), tolerance);
                break;
            }
        }
        if (start == -1) throw new RuntimeException("No vertex of expected coordinates match checked coordinate.\nExpected :"+check+"\nActual   :"+checkCoordinates);

        assert isCoordinateEquals(expectedCoordinates.get(0), expectedCoordinates.get(maxI), tolerance);
        expectedCoordinates.remove(maxI); // Remove last point as it is equal to first coordinate before permuting the elements.
        Collections.rotate(expectedCoordinates, -start);

        for (int i = 1; i < maxI ; i++) {
            check = checkCoordinates.get(i);
            if (!isCoordinateEquals(check, expectedCoordinates.get(sameOrder? i: maxI-i), tolerance))
                return false;
        }
        return true;
    }
}
