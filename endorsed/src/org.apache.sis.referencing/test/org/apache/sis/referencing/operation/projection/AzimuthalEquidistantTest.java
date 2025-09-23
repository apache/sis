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
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.parameter.Parameters;

// Test dependencies
import org.junit.jupiter.api.Test;


/**
 * Tests the {@link AzimuthalEquidistant} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class AzimuthalEquidistantTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public AzimuthalEquidistantTest() {
    }

    /**
     * Returns the method to be tested.
     */
    MapProjection method() {
        return new org.apache.sis.referencing.operation.provider.AzimuthalEquidistantSpherical();
    }

    /**
     * Creates a new instance of {@link AzimuthalEquidistant} for a sphere or an ellipsoid.
     * The new instance is stored in the inherited {@link #transform} field.
     */
    private void createNormalizedProjection() {
        final MapProjection op = method();
        final Parameters p = Parameters.castOrWrap(op.getParameters().createValue());
        p.parameter("semi_major").setValue(WGS84_A);
        p.parameter("semi_minor").setValue(WGS84_B);
        p.parameter("Latitude of natural origin").setValue(0);
        transform = new ObliqueStereographic(op, p);
    }

    /**
     * Tests the forward and reverse projection using test point given in Snyder page 337.
     * The Snyder's test uses a sphere of radius R=3 and a center at 40°N and 100°W.
     * The test in this class modify the longitude to 10°W for avoiding to mix wraparound
     * considerations in this test.
     *
     * @throws FactoryException if an error occurred while creating the projection.
     * @throws TransformException if an error occurred while projecting the test point.
     */
    @Test
    public void testSpherical() throws FactoryException, TransformException {
        createCompleteProjection(method(),
                  3,            // Semi-major axis
                  3,            // Semi-minor axis
                -10,            // Longitude of natural origin (central-meridian)
                 40,            // Latitude of natural origin
                Double.NaN,     // Standard parallel 1
                Double.NaN,     // Standard parallel 2
                Double.NaN,     // Scale factor
                  0,            // False easting
                  0);           // False Northing

        tolerance = 2E-7;
        verifyTransform(new double[] {
            -170,               // Was 1OO°E in Snyder test, shifted by 90° in our test.
             -20                // 20°S
        }, new double[] {
            -5.8311398,
             5.5444634
        });
    }

    /**
     * Tests with the point published in EPSG guidance note.
     *
     * @throws FactoryException if an error occurred while creating the projection.
     * @throws TransformException if an error occurred while projecting the test point.
     */
    @Test
    public void testWithEPSG() throws FactoryException, TransformException {
        /*
         * Since we are testing spherical formulas with a sample point calculated
         * for ellipsoidal formulas, we have to use a high tolerance threshold.
         */
        tolerance = 20;
        testWithEPSG(CLARKE_A, CLARKE_B);
    }

    /**
     * Tests with the point published in EPSG guidance note.
     * Callers must set {@link #tolerance} before to invoke this method.
     *
     * @param  semiMajor {@link #CLARKE_A}, or an alternative value if desired.
     * @param  semiMinor {@link #CLARKE_B}, or an alternative value if desired.
     * @throws FactoryException if an error occurred while creating the projection.
     * @throws TransformException if an error occurred while projecting the test point.
     */
    final void testWithEPSG(final double semiMajor, final double semiMinor) throws FactoryException, TransformException {
        createCompleteProjection(method(),
                semiMajor,
                semiMinor,
                138 + (10 +  7.48/60)/60,       // Longitude of natural origin (central-meridian)
                  9 + (32 + 48.15/60)/60,       // Latitude of natural origin
                Double.NaN,                     // Standard parallel 1
                Double.NaN,                     // Standard parallel 2
                Double.NaN,                     // Scale factor
                40000,                          // False easting
                60000);                         // False Northing
        /*
         * Test point given in EPSG guidance note.
         */
        verifyTransform(new double[] {
            138 + (11 + 34.908/60)/60,          // 138°11'34.908"E
              9 + (35 + 47.493/60)/60           //   9°35'47.493"N
        }, new double[] {
            42665.90,
            65509.82
        });
        /*
         * North of map origin, for entering in the special case for c/sin(c)
         * when c is close to zero. This point is not given by EPSG guidance
         * notes; this is an anti-regression test.
         */
        verifyTransform(new double[] {
            138 + (10 +  7.48/60)/60 + 0.00000000001,
              9 + (32 + 48.15/60)/60 + 0.01
        }, new double[] {
            40000.00,
            61105.98
        });
    }

    /**
     * Tests the derivatives at a few points on a sphere. This method compares the derivatives computed
     * by the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        createCompleteProjection(method(),
                CLARKE_A,
                CLARKE_B,
                 40,                // Longitude of natural origin (central-meridian)
                 25,                // Latitude of natural origin
                Double.NaN,         // Standard parallel 1
                Double.NaN,         // Standard parallel 2
                Double.NaN,         // Scale factor
                40000,              // False easting
                60000);             // False Northing
        final double delta = (1.0 / 60) / 1852;                 // Approximately 1 metre.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = Formulas.LINEAR_TOLERANCE / 100;
        verifyDerivative(30, 27);
        verifyDerivative(27, 20);
        verifyDerivative(40, 25);
    }

    /**
     * Tests {@link AzimuthalEquidistant#inverseTransform(double[], int, double[], int)} with input
     * coordinates close to zero. The tested method implementation has an indetermination at D = 0,
     * so we test its behavior close to that indetermination point.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testValuesNearZero() throws TransformException {
        createNormalizedProjection();
        transform = transform.inverse();
        tolerance = 1E-15;
        verifyValuesNearZero(0, 0);
    }
}
