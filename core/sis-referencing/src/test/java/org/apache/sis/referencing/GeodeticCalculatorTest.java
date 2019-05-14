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

import java.util.Random;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.junit.Assert.*;


/**
 * Tests {@link GeodeticCalculator}.
 *
 * @version 1.0
 * @since 1.0
 * @module
 */
public final strictfp class GeodeticCalculatorTest extends TestCase {
    /**
     * Returns the calculator to use for testing purpose.
     * This classes uses calculator for a sphere.
     */
    private static GeodeticCalculator create() {
        return new GeodeticCalculator(CommonCRS.SPHERE.geographic());
    }

    /**
     * Tests some simple azimuth directions. The expected directions are approximately North, East,
     * South and West, but not exactly because of Earth curvature. The test verify merely that the
     * azimuths are approximately correct.
     */
    @Test
    public void testCardinalAzimuths() {
        final GeodeticCalculator c = create();
        final double EPS = 0.2;
        c.setStartPoint(20, 12);
        c.setEndPoint(20, 13);  assertEquals("East",   90, c.getStartingAzimuth(), EPS);
        c.setEndPoint(21, 12);  assertEquals("North",   0, c.getStartingAzimuth(), EPS);
        c.setEndPoint(20, 11);  assertEquals("West",  -90, c.getStartingAzimuth(), EPS);
        c.setEndPoint(19, 12);  assertEquals("South", 180, c.getStartingAzimuth(), EPS);
    }

    /**
     * Tests azimuths at poles.
     */
    @Test
    public void testPoles() {
        final GeodeticCalculator c = create();
        final double EPS = 0.2;
        c.setStartPoint( 90,  30);
        c.setEndPoint  ( 20,  20);  assertEquals(-170, c.getStartingAzimuth(), EPS);
        c.setEndPoint  ( 20,  40);  assertEquals( 170, c.getStartingAzimuth(), EPS);
        c.setEndPoint  ( 20,  30);  assertEquals( 180, c.getStartingAzimuth(), EPS);
        c.setEndPoint  (-20,  30);  assertEquals( 180, c.getStartingAzimuth(), EPS);
        c.setEndPoint  (-90,  30);  assertEquals( 180, c.getStartingAzimuth(), EPS);

        c.setStartPoint( 90,   0);
        c.setEndPoint  ( 20,  20);  assertEquals( 160, c.getStartingAzimuth(), EPS);
        c.setEndPoint  ( 20, -20);  assertEquals(-160, c.getStartingAzimuth(), EPS);
        c.setEndPoint  ( 20,   0);  assertEquals( 180, c.getStartingAzimuth(), EPS);
        c.setEndPoint  (-90,   0);  assertEquals( 180, c.getStartingAzimuth(), EPS);
    }

    /**
     * Tests geodesic distances on the equator.
     */
    @Test
    public void testEquator() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final GeodeticCalculator c = create();
        final double r = c.ellipsoid.getSemiMajorAxis() * (PI / 180);
        c.setStartPoint(0, 0);
        for (double i=0; i<100; i++) {
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
     */
    @Test
    public void testWikipediaExample() throws TransformException {
        final GeodeticCalculator c = create();
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
     * Verifies that the given point is equals to the given latitude and longitude.
     *
     * @param φ  the expected latitude value, in degrees.
     * @param λ  the expected longitude value, in degrees.
     * @param p  the position to verify.
     * @param ε  the tolerance.
     */
    private static void assertPositionEquals(final double φ, final double λ, final DirectPosition p, final double ε) {
        assertEquals("φ", φ, p.getOrdinate(0), ε);
        assertEquals("λ", λ, p.getOrdinate(1), ε);
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
        final GeodeticCalculator c = new GeodeticCalculator(CommonCRS.SPHERE.normalizedGeographic());
        final double φ = -33.0;
        final double λ = -71.6;
        c.setStartPoint(new DirectPosition2D(λ, φ));
        assertPositionEquals(λ, φ, c.getStartPoint(), Formulas.ANGULAR_TOLERANCE);

        c.setStartingAzimuth(-94.41);
        c.setGeodesicDistance(18743000);
        assertPositionEquals(121.8, 31.4, c.getEndPoint(), 0.01);
    }
}
