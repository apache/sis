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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;

import static java.lang.StrictMath.*;
import static java.lang.Double.NaN;

// Test dependencies
import org.opengis.test.ToleranceModifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link AlbersEqualArea} class. We test using various values of standard parallels.
 * We do not test with various values of the latitude of origin, because its only effect is to
 * modify the translation term on the <var>y</var> axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn(CylindricalEqualAreaTest.class)
public final strictfp class AlbersEqualAreaTest extends MapProjectionTestCase {
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
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
                6370997,    // Semi-major axis from Synder table 15
                6370997,    // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                29.5,       // Standard parallel 1 (from Synder table 15)
                45.5,       // Standard parallel 2 (from Synder table 15)
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        final double delta = toRadians(100.0 / 60) / 1852;                  // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertTrue("isSpherical", isSpherical(kernel));
        assertEquals("n", 0.6028370, kernel.nm, 0.5E-7);                    // Expected 'n' value from Synder table 15.
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the
         * expected value of 6370997*ρ/n is 6910941 (value taken from ρ column in Synder table 15).
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
        verifyInDomain(new double[] {-20, 20},          // Minimal input ordinate values
                       new double[] {+20, 50},          // Maximal input ordinate values
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
    @DependsOnMethod("testSphere")
    public void testEllipse() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
                6378206.4,  // Semi-major axis from Synder table 15
                6356583.8,  // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                29.5,       // Standard parallel 1 (from Synder table 15)
                45.5,       // Standard parallel 2 (from Synder table 15)
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        final double delta = toRadians(100.0 / 60) / 1852;                  // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;
        final AlbersEqualArea kernel = (AlbersEqualArea) getKernel();
        assertFalse("isSpherical", isSpherical(kernel));
        /*
         * Expected 'n' value from Synder table 15. The division by (1-ℯ²) is because Apache SIS omits this factor
         * in its calculation of n (we rather take it in account in (de)normalization matrices and elsewhere).
         */
        assertEquals("n", 0.6029035, kernel.nm / (1 - kernel.eccentricitySquared), 0.5E-7);
        /*
         * When stepping into the AlbersEqualArea.Sphere.transform(…) method with a debugger, the expected
         * value of 6378206.4*ρ/(nm/(1-ℯ²)) is 6931335 (value taken from ρ column in Synder table 15).
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
        verifyInDomain(new double[] {-20, 20},          // Minimal input ordinate values
                       new double[] {+20, 50},          // Maximal input ordinate values
                       new int[]    {  5,  5},          // Number of points to test
                       TestUtilities.createRandomNumberGenerator());
    }

    /**
     * Uses Proj.4 test point has a reference.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @DependsOnMethod("testEllipse")
    public void compareWithProj4() throws FactoryException, TransformException {
        toleranceModifier = ToleranceModifier.PROJECTION;
        tolerance = Formulas.LINEAR_TOLERANCE;

        // Spherical case
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
                6400000,    // Semi-major axis
                6400000,    // Semi-minor axis
                0,          // Central meridian
                0,          // Latitude of origin
                0,          // Standard parallel 1
                2,          // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing

        verifyTransform(new double[] {2, 1}, new double[] {223334.085, 111780.432});

        // Ellipsoidal case
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
                6378137,            // Semi-major axis
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
     * Tests a few "special" points which need special care in inverse projection algorithm.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @DependsOnMethod("testEllipse")
    public void testSingularity() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
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
    @DependsOnMethod("testEllipse")
    public void testRandomPoints() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.AlbersEqualArea(),
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
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(new double[] {-40, 10},                  // Minimal input ordinate values
                       new double[] {+40, 60},                  // Maximal input ordinate values
                       new int[]    {  5,  5},                  // Number of points to test
                       TestUtilities.createRandomNumberGenerator());
    }
}
