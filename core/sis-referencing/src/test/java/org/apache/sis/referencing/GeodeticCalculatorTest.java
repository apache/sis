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

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

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
     * Tests spherical formulas with the example given in Wikipedia.
     * This computes the great circle route from Valparaíso (33°N 71.6W) to Shanghai (31.4°N 121.8°E).
     *
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    @Test
    public void testWikipediaExample() throws TransformException {
        final GeodeticCalculator c = new GeodeticCalculator(CommonCRS.SPHERE.geographic());
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
        c.setDirection(-94.41, 18743000);
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
}
