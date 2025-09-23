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
package org.apache.sis.referencing.operation.projection;

import static java.lang.StrictMath.*;
import static java.lang.Double.NaN;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.ToleranceModifier;


/**
 * Tests the {@link AlbersEqualArea} class. We test using various values of standard parallels.
 * We do not test with various values of the latitude of origin, because its only effect is to
 * modify the translation term on the <var>y</var> axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class AlbersEqualAreaTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public AlbersEqualAreaTest() {
    }

    /**
     * Returns whether the given projection is the spherical implementation.
     */
    private static boolean isSpherical(final AlbersEqualArea transform) {
        return transform instanceof AlbersEqualArea.Spherical;
    }

    /**
     * Tests the unitary projection on a sphere.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSphere() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                6370997,    // Semi-major axis from Snyder table 15
                6370997,    // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                29.5,       // Standard parallel 1 (from Snyder table 15)
                45.5,       // Standard parallel 2 (from Snyder table 15)
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        final double delta = toRadians(100.0 / 60) / 1852;              // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertTrue(isSpherical(kernel));
        assertEquals(0.6028370, kernel.nm, 0.5E-7);                     // Expected 'n' value from Snyder table 15.
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the
         * expected value of 6370997*ρ/n is 6910941 (value taken from ρ column in Snyder table 15).
         */
        verifyTransform(new double[] {0, 50}, new double[] {0, 5373933.180});
        /*
         * Expect 6370997*ρ/n  ≈  8022413   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 40}, new double[] {0, 4262461.266});
        /*
         * Expect 6370997*ρ/n  ≈  9695749   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 25}, new double[] {0, 2589125.654});
        /*
         * Verify consistency with random points.
         */
        verifyInDomain(new double[] {-20, 20},          // Minimal input coordinate values
                       new double[] {+20, 50},          // Maximal input coordinate values
                       new int[]    {  5,  5},          // Number of points to test
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Tests the unitary projection on an ellipse.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testEllipse() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                CLARKE_A,   // Semi-major axis from Snyder table 15
                CLARKE_B,   // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                29.5,       // Standard parallel 1 (from Snyder table 15)
                45.5,       // Standard parallel 2 (from Snyder table 15)
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        final double delta = toRadians(100.0 / 60) / 1852;                  // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertFalse(isSpherical(kernel));
        /*
         * Expected 'n' value from Snyder table 15. The division by (1-ℯ²) is because Apache SIS omits this factor
         * in its calculation of n (we rather take it in account in (de)normalization matrices and elsewhere).
         */
        assertEquals(0.6029035, kernel.nm / (1 - kernel.eccentricitySquared), 0.5E-7);
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the expected
         * value of 6378206.4*ρ/(nm/(1-ℯ²)) is 6931335 (value taken from ρ column in Snyder table 15).
         */
        verifyTransform(new double[] {0, 50}, new double[] {0, 5356698.435});
        /*
         * Expect 6378206.4*ρ/(nm/(1-ℯ²))  ≈  8042164   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 40}, new double[] {0, 4245869.390});
        /*
         * Expect 6378206.4*ρ/(nm/(1-ℯ²))  ≈  9710969   (can be verified only with the debugger)
         */
        verifyTransform(new double[] {0, 25}, new double[] {0, 2577064.350});
        /*
         * Verify consistency with random points.
         */
        verifyInDomain(new double[] {-20, 20},          // Minimal input coordinate values
                       new double[] {+20, 50},          // Maximal input coordinate values
                       new int[]    {  5,  5},          // Number of points to test
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Uses test point from PROJ library as a reference.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void compareWithPROJ() throws FactoryException, TransformException {
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;

        // Spherical case
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                RADIUS,     // Semi-major axis
                RADIUS,     // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                0,          // Standard parallel 1
                2,          // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        verifyTransform(new double[] {2, 1}, new double[] {223334.085, 111780.432});

        // Ellipsoidal case
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                6378137,            // Semi-major axis (not WGS84 despite same values)
                6356752.314140347,  // Semi-minor axis
                0,                  // Central meridian
                0,                  // Latitude of origin
                0,                  // Standard parallel 1
                2,                  // Standard parallel 2
                NaN,                // Scale factor (none)
                0,                  // False easting
                0);                 // False northing
        verifyTransform(new double[] {2, 1}, new double[] {222571.609, 110653.327});
    }

    /**
     * Tests a few "special" points which need special care in reverse projection algorithm.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSingularity() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                0,          // Central meridian
                0,          // Latitude of origin
                0,          // Standard parallel 1
                2,          // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        verifyTransform(new double[] {0,        0,
                                      0,      +90,
                                      0,      -90},
                        new double[] {0,        0,
                                      0, +6420271.594575703,    // Computed empirically with SIS (not from an external source).
                                      0, -6309429.217});
    }

    /**
     * Tests conversion of random points with non-zero central meridian, standard parallel
     * and false easting/northing.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testRandomPoints() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                12,         // Central meridian
                NaN,        // Latitude of origin (none)
                24,         // Standard parallel 1
                40,         // Standard parallel 2
                NaN,        // Scale factor (none)
                300,        // False easting
                200);       // False northing

        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(new double[] {-40, 10},                  // Minimal input coordinate values
                       new double[] {+40, 60},                  // Maximal input coordinate values
                       new int[]    {  5,  5},                  // Number of points to test
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Tests the projection of point where the difference between the given longitude value and central meridian
     * is close to 360°. In most map other map projection implementations, we rely on range reductions performed
     * automatically by trigonometric functions. However, we cannot rely on that effect in the particular case of
     * {@link AlbersEqualArea} because the longitude is pre-multiplied by a <var>n</var> factor before to be used
     * in trigonometric functions. The range reduction must be performed explicitly in map projection code.
     *
     * <p>The math transform tested here is:</p>
     * {@snippet lang="wkt" :
     *   Param_MT["Albers Equal Area",
     *     Parameter["semi_major", 6378206.4, Unit["metre"]],
     *     Parameter["semi_minor", 6356583.8, Unit["metre"]],
     *     Parameter["Latitude of false origin", 50, Unit["degree"]],
     *     Parameter["Longitude of false origin", -154, Unit["degree"]],
     *     Parameter["Latitude of 1st standard parallel", 55, Unit["degree"]],
     *     Parameter["Latitude of 2nd standard parallel", 65, Unit["degree"]]]
     *   }
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-486">SIS-486</a>
     */
    @Test
    public void testLongitudeWraparound() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.AlbersEqualArea(),
                6378206.4,  // Semi-major axis length
                6356583.8,  // Semi-minor axis length
                -154,       // Central meridian
                50,         // Latitude of origin
                55,         // Standard parallel 1
                65,         // Standard parallel 2
                NaN,        // Scale factor (none)
                NaN,        // False easting (none)
                NaN);       // False northing (none)

        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        /*
         * Skip inverse transform because the 176.003° become -183.997°. It is not the purpose
         * of this test to verify longitude wraparound in reverse projection (we do not expect
         * such wraparound to be applied).
         */
        isInverseTransformSupported = false;
        verifyTransform(new double[] {176.00296518775082, 52.00158201757688},
                        new double[] {-2000419.117, 680784.426});
    }
}
