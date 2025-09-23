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
import org.apache.sis.referencing.operation.provider.LambertCylindricalEqualArea;
import org.apache.sis.referencing.operation.provider.LambertCylindricalEqualAreaSpherical;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;

// Test dependencies
import org.junit.jupiter.api.Test;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.ToleranceModifier;


/**
 * Tests the {@link CylindricalEqualArea} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CylindricalEqualAreaTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public CylindricalEqualAreaTest() {
    }

    /**
     * Tests the derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws TransformException if an error occurred while projecting a point.
     */
    private void testDerivative() throws TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-6;                                       // More severe than Formulas.LINEAR_TOLERANCE.
        verifyDerivative(toRadians(15), toRadians( 30));
        verifyDerivative(toRadians(10), toRadians(-60));
    }

    /**
     * Tests <cite>Lambert Cylindrical Equal Area</cite> projection of a point in the in ellipsoidal case.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testEllipsoidal() throws FactoryException, TransformException {
        createCompleteProjection(new LambertCylindricalEqualArea(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                0,          // Central meridian
                NaN,        // Latitude of origin
                0,          // Standard parallel 1
                NaN,        // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double λ = 2;
        final double φ = 1;
        final double x = 222638.98;             // Test point from PROJ library.
        final double y = 110568.81;
        verifyTransform(new double[] {λ, φ,  -λ, φ,  λ, -φ,  -λ, -φ},
                        new double[] {x, y,  -x, y,  x, -y,  -x, -y});
        testDerivative();
    }

    /**
     * Tests <cite>Lambert Cylindrical Equal Area</cite> projection of a point in the in spherical case.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSpherical() throws FactoryException, TransformException {
        createCompleteProjection(new LambertCylindricalEqualArea(),
                6371007,    // Semi-major axis length
                6371007,    // Semi-minor axis length
                0,          // Central meridian
                NaN,        // Latitude of origin
                0,          // Standard parallel 1
                NaN,        // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double λ = 2;
        final double φ = 1;
        final double x = 222390.10;             // Anti-regression values (not from an external source).
        final double y = 111189.40;
        verifyTransform(new double[] {λ, φ,  -λ, φ,  λ, -φ,  -λ, -φ},
                        new double[] {x, y,  -x, y,  x, -y,  -x, -y});
        testDerivative();
    }

    /**
     * Tests <cite>Lambert Cylindrical Equal Area (Spherical)</cite> projection.
     * The difference between this test and {@link #testSpherical()} is that this case shall
     * compute the radius of the conformal sphere instead of using the semi-major axis length.
     * The result near the equator are almost the same however.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSphericalWithConformalSphereRadius() throws FactoryException, TransformException {
        createCompleteProjection(new LambertCylindricalEqualAreaSpherical(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                0,          // Central meridian
                NaN,        // Latitude of origin
                0,          // Standard parallel 1
                NaN,        // Standard parallel 2
                NaN,        // Scale factor (none)
                0,          // False easting
                0);         // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double λ = 2;
        final double φ = 1;
        final double x = 222390.10;             // Anti-regression values (not from an external source).
        final double y = 111189.40;
        verifyTransform(new double[] {λ, φ,  -λ, φ,  λ, -φ,  -λ, -φ},
                        new double[] {x, y,  -x, y,  x, -y,  -x, -y});
        testDerivative();
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
        createCompleteProjection(new LambertCylindricalEqualArea(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                12,         // Central meridian
                NaN,        // Latitude of origin (none)
                24,         // Standard parallel 1
                NaN,        // Standard parallel 2
                NaN,        // Scale factor (none)
                300,        // False easting
                200);       // False northing

        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_SAFE, 0);
    }
}
