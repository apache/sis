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
package org.apache.sis.referencing;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.PathIterator;
import java.util.Random;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.opengis.test.Assert.*;
import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;


/**
 * Tests {@link GeodeticCalculator}.
 *
 * @version 1.0
 * @since 1.0
 * @module
 */
public final strictfp class GeodeticCalculatorTest extends TestCase {
    /**
     * Verifies that the given point is equals to the given latitude and longitude.
     *
     * @param φ  the expected latitude value, in degrees.
     * @param λ  the expected longitude value, in degrees.
     * @param p  the actual position to verify.
     * @param ε  the tolerance threshold.
     */
    private static void assertPositionEquals(final double φ, final double λ, final DirectPosition p, final double ε) {
        assertEquals("φ", φ, p.getOrdinate(0), ε);
        assertEquals("λ", λ, p.getOrdinate(1), ε);
    }

    /**
     * Asserts that a Java2D point is equal to the expected value. Used for verifying geodesic paths.
     *
     * @param x  the expected <var>x</var> coordinates.
     * @param y  the expected <var>y</var> coordinates.
     * @param p  the actual position to verify.
     * @param ε  the tolerance threshold.
     */
    private static void assertPointEquals(final double x, final double y, final Point2D p, final double ε) {
        assertEquals("x", x, p.getX(), ε);
        assertEquals("y", y, p.getY(), ε);
    }

    /**
     * Returns the calculator to use for testing purpose. This classes uses a calculator for a sphere.
     *
     * @param  normalized  whether to force (longitude, latitude) axis order.
     */
    private static GeodeticCalculator create(final boolean normalized) {
        return new GeodeticCalculator(normalized
                ? CommonCRS.SPHERE.normalizedGeographic()
                : CommonCRS.SPHERE.geographic());
    }

    /**
     * Tests some simple azimuth directions. The expected directions are approximately North, East,
     * South and West, but not exactly because of Earth curvature. The test verify merely that the
     * azimuths are approximately correct.
     */
    @Test
    public void testCardinalAzimuths() {
        final GeodeticCalculator c = create(false);
        final double tolerance = 0.2;
        c.setStartPoint(20, 12);
        c.setEndPoint(20, 13);  assertEquals("East",   90, c.getStartingAzimuth(), tolerance);
        c.setEndPoint(21, 12);  assertEquals("North",   0, c.getStartingAzimuth(), tolerance);
        c.setEndPoint(20, 11);  assertEquals("West",  -90, c.getStartingAzimuth(), tolerance);
        c.setEndPoint(19, 12);  assertEquals("South", 180, c.getStartingAzimuth(), tolerance);
    }

    /**
     * Tests azimuths at poles.
     */
    @Test
    public void testAzimuthAtPoles() {
        final GeodeticCalculator c = create(false);
        final double tolerance = 0.2;
        c.setStartPoint( 90,  30);
        c.setEndPoint  ( 20,  20);  assertEquals(-170, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  ( 20,  40);  assertEquals( 170, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  ( 20,  30);  assertEquals( 180, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  (-20,  30);  assertEquals( 180, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  (-90,  30);  assertEquals( 180, c.getStartingAzimuth(), tolerance);

        c.setStartPoint( 90,   0);
        c.setEndPoint  ( 20,  20);  assertEquals( 160, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  ( 20, -20);  assertEquals(-160, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  ( 20,   0);  assertEquals( 180, c.getStartingAzimuth(), tolerance);
        c.setEndPoint  (-90,   0);  assertEquals( 180, c.getStartingAzimuth(), tolerance);
    }

    /**
     * Tests geodesic distances on the equator.
     */
    @Test
    public void testDistanceAtEquator() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final GeodeticCalculator c = create(false);
        final double r = c.ellipsoid.getSemiMajorAxis() * (PI / 180);
        c.setStartPoint(0, 0);
        for (int i=0; i<100; i++) {
            final double x = 180 * random.nextDouble();
            c.setEndPoint(0, x);
            assertEquals(x * r, c.getGeodesicDistance(), Formulas.LINEAR_TOLERANCE);
        }
    }

    /**
     * Tests spherical formulas with the example given in Wikipedia.
     * This computes the great circle route from Valparaíso (33°N 71.6W) to Shanghai (31.4°N 121.8°E).
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Great-circle_navigation#Example">Great-circle navigation on Wikipedia</a>
     */
    @Test
    public void testWikipediaExample() throws TransformException {
        final GeodeticCalculator c = create(false);
        c.setStartPoint(-33.0, -71.6);          // Valparaíso
        c.setEndPoint  ( 31.4, 121.8);          // Shanghai
        /*
         * Wikipedia example gives:
         *
         *     Δλ = −166.6°
         *     α₁ = −94.41°
         *     α₂ = −78.42°
         *     Δσ = 168.56°   →    taking R = 6371 km, the distance is 18743 km.
         */
        assertEquals(Units.METRE,         c.getDistanceUnit());
        assertEquals("α₁",        -94.41, c.getStartingAzimuth(), 0.005);
        assertEquals("α₂",        -78.42, c.getEndingAzimuth(),   0.005);
        assertEquals("distance",   18743, c.getGeodesicDistance() / 1000, 0.5);
        assertPositionEquals(31.4, 121.8, c.getEndPoint(), 1E-12);                  // Should be the specified value.
        /*
         * Keep start point unchanged, but set above azimuth and distance.
         * Verify that we get the Shanghai coordinates.
         */
        c.setStartingAzimuth(-94.41);
        c.setGeodesicDistance(18743000);
        assertEquals("α₁",        -94.41, c.getStartingAzimuth(), 1E-12);           // Should be the specified value.
        assertEquals("α₂",        -78.42, c.getEndingAzimuth(),   0.01);
        assertEquals("distance",   18743, c.getGeodesicDistance() / 1000, STRICT);  // Should be the specified value.
        assertPositionEquals(31.4, 121.8, c.getEndPoint(), 0.01);
    }

    /**
     * Tests geodetic calculator involving a coordinate operation.
     * This test uses a simple CRS with only the axis order interchanged.
     * The coordinates are the same than {@link #testWikipediaExample()}.
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    @Test
    @DependsOnMethod("testWikipediaExample")
    public void testUsingTransform() throws TransformException {
        final GeodeticCalculator c = create(true);
        final double φ = -33.0;
        final double λ = -71.6;
        c.setStartPoint(new DirectPosition2D(λ, φ));
        assertPositionEquals(λ, φ, c.getStartPoint(), Formulas.ANGULAR_TOLERANCE);

        c.setStartingAzimuth(-94.41);
        c.setGeodesicDistance(18743000);
        assertPositionEquals(121.8, 31.4, c.getEndPoint(), 0.01);
    }

    /**
     * Tests {@link GeodeticCalculator#toGeodesicPath2D(double)}. This method uses a CRS
     * that swap axis order as a way to verify that user-specified CRS is taken in account.
     * The tested coordinates are from Wikipedia example.
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    @Test
    @DependsOnMethod("testUsingTransform")
    public void testGeodesicPath2D() throws TransformException {
        final GeodeticCalculator c = create(true);
        final double tolerance = 0.05;
        c.setStartPoint(-33.0, -71.6);                  // Valparaíso
        c.setEndPoint  ( 31.4, 121.8);                  // Shanghai
        final Shape path = c.toGeodesicPath2D(1000);
        assertPointEquals( -71.6, -33.0, ShapeUtilities.pointOnBezier(path, 0),   tolerance);
        assertPointEquals(-238.2,  31.4, ShapeUtilities.pointOnBezier(path, 1),   tolerance);       // λ₂ = 121.8° - 360°
        assertPointEquals(-159.2,  -6.8, ShapeUtilities.pointOnBezier(path, 0.5), tolerance);
    }

    /**
     * Verifies that all <var>y</var> coordinates are zero for a geodesic path on equator.
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    @Test
    public void testGeodesicPathOnEquator() throws TransformException {
        final GeodeticCalculator c = create(false);
        final double tolerance = 1E-12;
        c.setStartPoint(0, 20);
        c.setEndPoint  (0, 12);
        assertEquals(-90, c.getStartingAzimuth(), tolerance);
        assertEquals(-90, c.getEndingAzimuth(),   tolerance);
        final Shape geodeticCurve = c.toGeodesicPath2D(1);
        final double[] coords = new double[2];
        for (final PathIterator it = geodeticCurve.getPathIterator(null, 1); !it.isDone(); it.next()) {
            it.currentSegment(coords);
            assertEquals ("φ",  0, coords[0], tolerance);
            assertBetween("λ", 12, 20, coords[1]);
        }
    }

    /**
     * Tests path on the parallel at 45°N. This tests Data for this test have been taken from
     * <a href="http://perso.univ-lemans.fr/~hainry/articles/loxonavi.html">Orthodromie et loxodromie</a> page.
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    @Test
    public void testOnParallel45() throws TransformException {
        /*
         * Following numbers assume a radius R = 60 * 180/π nautical miles ≈ 6366 km.
         * (compare to 6371 km for authalic sphere, or (6356 / 6378) km for WGS84 semi-minor/major axis length).
         *
         * Column 1: Longitude difference in degrees.
         * Column 2: Geodesic distance in kilometers.
         * Column 3: Rhumb line distance in kilometers.
         */
        final double[] data = {
              0.00,      0,      0,
             11.25,    883,    884,
             22.50,   1762,   1768,
             33.75,   2632,   2652,
             45.00,   3489,   3536,
             56.25,   4327,   4419,
             67.50,   5140,   5303,
             78.75,   5923,   6187,
             90.00,   6667,   7071,
            101.25,   7363,   7955,
            112.50,   8002,   8839,
            123.75,   8573,   9723,
            135.00,   9064,  10607,
            146.25,   9463,  11490,
            157.50,   9758,  12374,
            168.75,   9939,  13258,
            180.00,  10000,  14142
        };
        final GeodeticCalculator c = create(false);
        final double toTestValue = (60 * NAUTICAL_MILE * 180/PI) / 6371007 / 1000;
        for (int i=0; i<data.length; i+=3) {
            c.setStartPoint(45, 0);
            c.setEndPoint(45, data[i]);
            double geodesic  = c.getGeodesicDistance();
//          double rhumbLine = c.getRhumbLineDistance();
            geodesic  *= toTestValue;
//          rhumbLine *= toTestValue;
            final double tolerance = data[i+1] / 1000;           // In kilometres.
            assertEquals("Geodesic distance",   data[i+1], geodesic,  tolerance);
//          assertEquals("Rhumb line distance", data[i+2], rhumbLine, tolerance);
            assertEquals("Distance measured along geodesic path", geodesic, length(c) * toTestValue, tolerance * 20);
        }
    }

    /**
     * Measures an estimation of the length of the path returned by {@link GeodeticCalculator#toGeodesicPath2D(double)}.
     * This method iterates over line segments and use the given calculator for computing the geodesic distance of each
     * segment. The state of the given calculator is modified by this method.
     */
    private static double length(final GeodeticCalculator c) throws TransformException {
        final PathIterator iterator = c.toGeodesicPath2D(10).getPathIterator(null, 100);
        final double[] buffer = new double[2];
        double length=0;
        while (!iterator.isDone()) {
            switch (iterator.currentSegment(buffer)) {
                default: fail("Unexpected path"); break;
                case PathIterator.SEG_MOVETO: {
                    c.setStartPoint(buffer[0], buffer[1]);
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    c.setEndPoint(buffer[0], buffer[1]);
                    length += c.getGeodesicDistance();
                    c.moveToEndPoint();
                }
            }
            iterator.next();
        }
        return length;
    }
}
