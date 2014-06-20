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
 * @version 0.5
 * @module
 */
public class CoordinateDomainTest extends TestCase {
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
            -1277391.5,  -6002208.9,   1732520.1,
               74602.9,   -589190.3,   6337303.4,
            -4761978.5,  -1597877.1,  -3914795.3,
            -4580863.2,   4167915.7,  -1530203.8,
            -5088029.0,   1419280.2,   3574467.8
        }, CoordinateDomain.GEOCENTRIC.generateRandomInput(new Random(6711701980687388701L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC}.
     * Latitude values shall be in the [-90 … +90]° range.
     */
    @Test
    public void testGeographic() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              83.5,   82.2,  -7586.1,
             -87.6,  -88.4,    231.1,
            -123.5,   18.3,   5601.5,
              87.7,  -14.1,   8710.9,
              52.4,  -68.4,  -4706.3
        }, CoordinateDomain.GEOGRAPHIC.generateRandomInput(new Random(9157324015136982593L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_SAFE}.
     * Latitude values shall be in the [-70 … +70]° range.
     */
    @Test
    public void testGeographicSafe() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
             -18.4,  -35.6,  -3804.6,
             152.4,    3.5,   3166.6,
             -28.7,   14.7,   2820.3,
             -38.9,  -36.0,  -1568.6,
             161.0,   46.2,   2492.8
        }, CoordinateDomain.GEOGRAPHIC_SAFE.generateRandomInput(new Random(8378805665450590968L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_POLES}.
     * Latitude values shall be in the [-90 … -70]° or [70 … 90]° range.
     */
    @Test
    public void testGeographicPole() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -136.4,   87.3,  -1490.4,
             -97.8,   71.1,  -4503.6,
             -45.3,   75.7,  -3179.1,
             -45.1,  -79.6,  -3440.5,
              11.1,  -83.6,   2017.0
        }, CoordinateDomain.GEOGRAPHIC_POLES.generateRandomInput(new Random(6784869539382621962L), 3, 5), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS}.
     */
    @Test
    public void testGeographicRadians() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -144.2,   66.4,  -3970.0,
              -3.1,   -8.0,   6627.9,
             -20.4,   37.0,  -9071.7,
             151.2,  -43.0,  -8346.5,
            -121.7,   33.8,  -5843.4
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
             -38.4,  -42.3,  -6357.0,
              36.7,   18.2,  -2924.6,
             -41.9,    2.4,  -9840.4,
              69.7,   31.2,   1866.3,
              15.4,  -89.1,   3977.3
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_HALF_λ.generateRandomInput(new Random(544370108347649978L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_NORTH}.
     */
    @Test
    public void testGeographicRadiansNorth() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              70.2,    0.3,  -2152.7,
            -142.0,   21.1,  -6983.9,
             -30.1,   20.5,   6443.0,
              48.8,   35.1,   4712.3,
               7.4,   16.6,  -7030.0
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH.generateRandomInput(new Random(2332709146110987009L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_SOUTH}.
     */
    @Test
    public void testGeographicRadiansSouth() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              11.8,  -29.7,  -3783.7,
            -140.4,  -62.1,  -8471.8,
              64.2,   -2.5,  -1100.3,
             -41.0,  -86.7,   4986.0,
            -174.4,  -84.5,   5615.6
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_SOUTH.generateRandomInput(new Random(3024333515949168349L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_EAST}.
     */
    @Test
    public void testGeographicRadiansEast() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
              53.0,    22.9, -1166.6,
              86.1,    12.1,  -255.3,
             142.9,     8.1, -3782.4,
             138.1,     3.4,  1172.8,
             146.4,   -33.8, -4487.7
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_EAST.generateRandomInput(new Random(3351157046773088704L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#GEOGRAPHIC_RADIANS_WEST}.
     */
    @Test
    public void testGeographicRadiansWest() {
        assertArrayEquals(new double[] {
            // ……λ   …………φ   ………………H
            -175.6,    74.1,  8059.1,
            -173.9,    81.9, -6712.6,
             -11.3,    74.8,  4056.4,
             -81.4,    -1.4, -9255.1,
            -133.4,   -50.5, -2251.5
        }, toDegrees(CoordinateDomain.GEOGRAPHIC_RADIANS_WEST.generateRandomInput(new Random(7320025557405586859L), 3, 5)), TOLERANCE);
    }

    /**
     * Tests {@link CoordinateDomain#PROJECTED}.
     */
    @Test
    public void testProjected() {
        assertArrayEquals(new double[] {
            // ………………x   ……………………………y   ………………h
            11376932.6,  -212881715.5,  -5712.7,
             7918069.3,  -142014756.3,  -2917.9,
            -1243860.7,  -209994389.7,   -893.4,
            16373202.6,   414305247.8,  -6783.9,
            -7715982.4,   -20958213.4,  -1625.9
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
     * Tests {@link CoordinateDomain#GAUSSIAN}.
     */
    @Test
    public void testGaussian() {
        assertArrayEquals(new double[] {
            -0.916,
             1.777,
            -0.896,
             0.093,
             0.428
        }, CoordinateDomain.GAUSSIAN.generateRandomInput(new Random(7679314270394487033L), 1, 5), 0.0005);
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
