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
package org.apache.sis.referencing.operation.transform;

import java.util.Random;

// Test imports
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CoordinateDomain}.
 * The main intend of this class is to allow visual inspection (by looking in source code) of sampled data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public strictfp class CoordinateDomainTest extends TestCase {
    /**
     * The tolerance threshold used in this test suite.
     */
    private static final double TOLERANCE = 0.05;

    /**
     * Tests {@link CoordinateDomain#GEOCENTRIC}.
     */
    @Test
    public void testGeocentric() {
        assertArrayEquals(new double[] {
            // ………………X   ………………………Y    ……………………Z
             -790009.4,  -3712097.2,  -5115300.9,
              876346.5,    537685.1,  -6296051.5,
             -790310.7,   4969804.0,  -3922073.1,
              729581.8,  -5762010.5,  -2627726.9,
            -2261070.0,    427543.0,   5948444.0
        }, CoordinateDomain.GEOCENTRIC.generateRandomInput(new Random(6711701980687388701L), 3, 5), TOLERANCE);
    }

/*
    TIP: If the values need to be regenerated after a change in CoordinateDomain implementation,
    one can use the following code:

    public static void main(String[] args) {
        double[] ordinates = CoordinateDomain.GEOGRAPHIC.generateRandomInput(new Random(9157324015136982593L), 3, 5));
        for (int i=0; i<ordinates.length;) {
            System.out.format(java.util.Locale.US, "            %6.1f, %6.1f, %8.1f,%n", ordinates[i++], ordinates[i++], ordinates[i++]);
        }
    }
*/

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC}.
     * Latitude values shall be in the [-90 … +90]° range.
     */
    @Test
    public void testGeographic() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              83.5,    2.1,  -1566.8,
             164.3,  -61.8,   8710.9,
            -136.6,   18.3,   2910.8,
             -87.6,   50.4,  -7603.3,
            -176.8,   43.9,  -4706.3
        }, CoordinateDomain.GEOGRAPHIC.generateRandomInput(new Random(9157324015136982593L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_SAFE}.
     * Latitude values shall be in the [-66 … +66]° range approximatively.
     */
    @Test
    public void testGeographicSafe() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
             -18.4,   42.2,  -5144.4,
             -91.0,  -10.7,  -3137.2,
            -136.2,   14.0,   8993.6,
             152.4,   37.5,   6593.3,
               9.0,  -14.5,   4985.6
        }, CoordinateDomain.GEOGRAPHIC_SAFE.generateRandomInput(new Random(8378805665450590968L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_POLES}.
     * Latitude values shall be in the [-90 … -66]° or [66 … 90]° range approximatively.
     */
    @Test
    public void testGeographicPole() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -136.6,  -74.3,   -130.1,
             -17.9,   74.7,   -646.5,
              98.2,   66.9,  -1687.9,
             151.9,   73.8,   1197.0,
             -90.5,  -86.4,  -7658.8
        }, CoordinateDomain.GEOGRAPHIC_POLES.generateRandomInput(new Random(6784869539382621964L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_NORTH_POLE}.
     */
    @Test
    public void testGeographicNorthPole() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
             -41.7,   72.6,   9605.3,
            -107.7,   68.2,    388.5,
              32.9,   69.3,   2553.6,
             106.5,   81.0,  -7334.3,
             135.2,   87.6,  -3518.9
        }, CoordinateDomain.GEOGRAPHIC_NORTH_POLE.generateRandomInput(new Random(2141713460614422218L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_SOUTH_POLE}.
     */
    @Test
    public void testGeographicSouthPole() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -106.2,  -71.9, -2202.6,
            -172.6,  -89.5,  2428.1,
              33.2,  -84.3,  6068.1,
             -64.3,  -76.3, -3436.7,
             -97.6,  -72.5,  8702.2
        }, CoordinateDomain.GEOGRAPHIC_SOUTH_POLE.generateRandomInput(new Random(5769644852151897296L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS}.
     */
    @Test
    public void testGeographicRadians() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -144.2,   59.7,  -4779.9,
             132.7,  -10.2,  -8346.5,
             -71.5,   37.0,  -6761.6,
              -3.1,  -81.6,   3751.3,
             -16.0,   75.6,  -5843.4
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS.generateRandomInput(new Random(8149671419063258264L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_HALF_λ}.
     * Longitude values shall be in the [-90 … +90]° range.
     */
    @Test
    public void testGeographicRadiansHalfλ() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
             -38.4,  -26.3,   3464.7,
             -42.3,  -41.9,   1866.3,
             -57.2,    2.4,   1713.4,
              36.7,  -88.6,  -9903.1,
              18.2,   69.7,   3977.3
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_HALF_λ.generateRandomInput(new Random(544370108347649978L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_NORTH}.
     */
    @Test
    public void testGeographicRadiansNorth() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              70.2,   13.6,  -2197.0,
            -178.8,   37.5,   4712.3,
             -38.7,   20.5,    413.2,
            -142.0,   74.0,  -6309.6,
             -95.8,   57.2,  -7030.0
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH.generateRandomInput(new Random(2332709146110987009L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_SOUTH}.
     */
    @Test
    public void testGeographicRadiansSouth() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              11.8,  -83.1,   9272.3,
             -61.2,  -29.0,   4986.0,
             -68.1,  -87.5,  -9688.0,
            -140.4,  -50.0,   8784.8,
              68.5,  -55.3,   5615.6
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_SOUTH.generateRandomInput(new Random(3024333515949168349L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_EAST}.
     */
    @Test
    public void testGeographicRadiansEast() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              53.0,   -2.3,    373.2,
             112.9,   52.9,   1172.8,
              79.5,    8.1,   6267.0,
              86.1,  -34.0,  -3750.7,
             102.1,   48.1,  -4487.7
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_EAST.generateRandomInput(new Random(3351157046773088704L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_WEST}.
     */
    @Test
    public void testGeographicRadiansWest() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              -4.4,  -60.4,   -153.3,
             -15.9,  -78.7,  -9255.1,
             -17.5,   74.8,   4824.4,
              -6.1,   36.5,  -5616.2,
              -8.1,   -8.6,  -2251.5
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_WEST.generateRandomInput(new Random(7320025557405586859L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#PROJECTED}.
     */
    @Test
    public void testProjected() {
        assertArrayEquals(new double[] {
            // ………………x   ……………………………y   ………………h
            11376932.6,  -133745096.2,   9038.7,
            -9313575.1,   -28431102.7,  -6783.9,
           -11455966.3,  -209994389.7,  -3847.7,
             7918069.3,   -40952573.2,   -457.2,
            -6213145.6,   374244630.7,  -1625.9
        }, toDegrees(CoordinateDomain.PROJECTED.generateRandomInput(new Random(4961499932406116863L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#RANGE_10}.
     */
    @Test
    public void testRange10() {
        assertArrayEquals(new double[] {
            -7.3,
             6.0,
             7.8,
            -2.3,
            -9.5
        }, CoordinateDomain.RANGE_10.generateRandomInput(new Random(2954568576395177702L), 1, 5), TOLERANCE);
    }

    /**
     * Converts longitude and latitude values from radians to degrees, for easier reading of test methods.
     */
    private static double[] toDegrees(final double[] coordinates) {
        for (int i=0; i<coordinates.length; i++) {
            coordinates[i] = StrictMath.toDegrees(coordinates[i]); i++;
            coordinates[i] = StrictMath.toDegrees(coordinates[i]); i++;
            // Skip the third dimension (height).
        }
        return coordinates;
    }
}
