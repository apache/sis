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

import static java.lang.Double.NaN;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.parameter.Parameters;
import static org.apache.sis.math.MathFunctions.SQRT_2;
import static org.apache.sis.referencing.internal.shared.Formulas.LINEAR_TOLERANCE;
import static org.apache.sis.referencing.internal.shared.Formulas.ANGULAR_TOLERANCE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;


/**
 * Tests the {@link LambertAzimuthalEqualArea} class. We test using various values
 * of the latitude of origin, which is the only parameter impacting the internal
 * coefficients of that class (except for the eccentricity).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class LambertAzimuthalEqualAreaTest extends MapProjectionTestCase {
    /**
     * The radius of the sphere used in sphere test cases.
     */
    private static final double SPHERE_RADIUS = ReferencingServices.AUTHALIC_RADIUS;

    /**
     * Creates a new test case.
     */
    public LambertAzimuthalEqualAreaTest() {
    }

    /**
     * Returns the provider for the map projection to tesT.
     */
    private static MapProjection provider(final boolean elliptical) {
        return elliptical ? new org.apache.sis.referencing.operation.provider.LambertAzimuthalEqualArea()
                          : new org.apache.sis.referencing.operation.provider.LambertAzimuthalEqualAreaSpherical();
    }

    /**
     * Creates and validates a new instance of {@link LambertAzimuthalEqualArea}.
     *
     * @param  elliptical        {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @param  latitudeOfOrigin  the latitude of origin.
     * @param  complete          whether to create the full projection, working on degrees instead of radians.
     * @throws FactoryException if an error occurred while creating the map projection.
     */
    private void createProjection(final boolean elliptical, final double latitudeOfOrigin, final boolean complete)
            throws FactoryException
    {
        final MapProjection provider = provider(elliptical);
        final Parameters parameters = parameters(provider, elliptical);
        parameters.parameter("latitude_of_origin").setValue(latitudeOfOrigin);
        LambertAzimuthalEqualArea projection = new LambertAzimuthalEqualArea(provider, parameters);
        if (complete) {
            transform = projection.createMapProjection(context(new MathTransformFactoryMock(provider), parameters));
        } else {
            transform = projection;
        }
        validate();
    }

    /**
     * Tests oblique case with a point computed with PROJ.
     * Command line was:
     *
     * <pre>cs2cs "EPSG:4326" +to +type=crs +proj=laea +no_defs +lat_0=90 +lon_0=0 +datum=WGS84 &lt;&lt;&lt; "72 50"</pre>
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testPolar() throws FactoryException, TransformException {
        tolerance = LINEAR_TOLERANCE;
        createProjection(true, 90, true);
        final double[] point    = new double[] {50, 72};
        final double[] expected = new double[] {1533302.80, -1286593.82};
        verifyTransform(point, expected);
    }

    /**
     * Tests oblique case with a point computed with PROJ.
     * Command line was:
     *
     * <pre>cs2cs "EPSG:4326" +to +type=crs +proj=laea +no_defs +lat_0=37 +lon_0=0 +datum=WGS84 &lt;&lt;&lt; "25 -9"</pre>
     *
     * Note that {@link #runGeoapiTest()} performs a similar test.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testOblique() throws FactoryException, TransformException {
        tolerance = LINEAR_TOLERANCE;
        createProjection(true, 37, true);
        final double[] point    = new double[] {-9, 25};
        final double[] expected = new double[] {-911656.53, -1288191.92};
        verifyTransform(point, expected);
    }

    /**
     * Tests self-consistency of the unitary projection on a sphere.
     * The projection works with radians on a sphere of radius 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testConsistencyOfUnitaryOnSphere() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;              // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = ANGULAR_TOLERANCE;
        for (int φ = -90; φ <= 90; φ += 15) {
            createProjection(false, φ, false);
            verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS, 862247543);
        }
    }

    /**
     * Tests self-consistency of the unitary projection on an ellipse.
     * The projection works with radians on an ellipsoid of semi-major axis length 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testConsistencyOfUnitaryOnEllipse() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;              // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = ANGULAR_TOLERANCE;
        for (int φ = -90; φ <= 90; φ += 15) {
            createProjection(true, φ, false);
            verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS, 484117986);
        }
    }

    /**
     * Tests the projection at a few particular points in the oblique case.
     * The particular points are the origin and the (almost) antipodal points.
     * The projection works with radians on a sphere of radius 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testParticularPointsWithObliqueOnSphere() throws FactoryException, TransformException {
        testParticularPointsWithOblique(false);
    }

    /**
     * Tests the projection at a few particular points in the oblique case.
     * The particular points are the origin and the (almost) antipodal points.
     * The projection works with degrees on an ellipsoid of semi-major axis length 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testParticularPointsWithObliqueOnEllipse() throws FactoryException, TransformException {
        testParticularPointsWithOblique(true);
    }

    /**
     * Implementation of {@link #testParticularPointsWithObliqueOnSphere()}
     * and {@link #testParticularPointsWithObliqueOnEllipse()}.
     */
    private void testParticularPointsWithOblique(final boolean elliptical) throws FactoryException, TransformException {
        tolerance = LINEAR_TOLERANCE;
        createProjection(elliptical, 45, true);

        // Projects the origin.
        final double[] point    = new double[] {0, 45};
        final double[] expected = new double[] {0, 0};
        verifyTransform(point, expected);

        // Project the antipode.
        point[0] = 180;
        point[1] = -45;
        transform.transform(point, 0, point, 0, 1);
        assertEquals(NaN, point[0], tolerance, "E");
        assertEquals(NaN, point[1], tolerance, "N");

        // Project the almost-antipode.
        point[0] = 180;
        point[1] = -44.999;
        transform.transform(point, 0, point, 0, 1);
        assertEquals(0, point[0], tolerance, "E");
        assertEquals(2*SPHERE_RADIUS, point[1], 0.002*SPHERE_RADIUS, "N");
    }

    /**
     * Tests the projection at a few extreme points in the polar case.
     * The projection works with radians on a sphere of radius 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testParticularPointsWithPolarOnSphere() throws FactoryException, TransformException {
        testParticularPointsWithPolar(false);
    }

    /**
     * Tests the projection at a few extreme points in the polar case.
     * The projection works with radians on an ellipsoid of semi-major axis length 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testParticularPointsWithPolarOnEllipse() throws FactoryException, TransformException {
        testParticularPointsWithPolar(true);
    }

    /**
     * Implementation of {@link #testParticularPointsWithPolarOnSphere()}
     * and {@link #testParticularPointsWithPolarOnEllipse()}.
     */
    private void testParticularPointsWithPolar(final boolean elliptical) throws FactoryException, TransformException {
        tolerance = LINEAR_TOLERANCE;
        createProjection(elliptical, 90, true);
        /*
         * Project the origin. Result should be (0,0). Do not test the reverse projection
         * because the longitude could be anything and still be the North pole. We test that
         * by projecting again with another longitude, set to 45°, and expect the same result.
         */
        final double[] point    = new double[] {0, 90};
        final double[] expected = new double[] {0,  0};
        isInverseTransformSupported = false;
        verifyTransform(point, expected);
        transform.inverse().transform(expected, 0, point, 0, 1);
        assertEquals( 0, point[0], 180, "λ");
        assertEquals(90, point[1], ANGULAR_TOLERANCE, "φ");
        /*
         * Same point as above (i.e. projection origin), but using a different longitude value.
         * Same expected result because longitude should have no effect at a pole.
         */
        point[0] = 45;
        point[1] = 90;
        verifyTransform(point, expected);
        transform.inverse().transform(expected, 0, point, 0, 1);
        assertEquals(45, point[0], 180, "λ");
        assertEquals(90, point[1], ANGULAR_TOLERANCE, "φ");
        /*
         * Project a point on the equator, at 0° and at 180° longitude.
         * Result should be (0, √2) positive or negative depending on the longitude.
         */
        point[0] = 0; expected[0] = 0;
        point[1] = 0; expected[1] = -SQRT_2*SPHERE_RADIUS;
        isInverseTransformSupported = true;
        if (elliptical) tolerance = 0.5;        // Because the use of `SPHERE_RADIUS` is approximate.
        verifyTransform(point, expected);

        point[0] = 180; expected[0] = 0;
        point[1] =   0; expected[1] = SQRT_2*SPHERE_RADIUS;
        verifyTransform(point, expected);
        /*
         * Project the antipode. Result would be (0, -2) if this operation was allowed.
         * Actually the formulas would work, but every points on a circle or radius 2
         * would be the pole so returning a single value may not be appropriate.
         */
        point[0] =   0;
        point[1] = -90;
        transform.transform(point, 0, point, 0, 1);
        assertEquals(NaN, point[0], tolerance, "E");
        assertEquals(NaN, point[1], tolerance, "N");
    }

    /**
     * Tests consistency when converting a point forward then backward.
     * The projection works with radians on a sphere of radius 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testRoundtrip() throws FactoryException, TransformException {
        tolerance = ANGULAR_TOLERANCE;
        createProjection(false, -75, false);
        final double[] point = new double[] {toRadians(-90), toRadians(-8)};
        transform.transform(point, 0, point, 0, 1);
        transform.inverse().transform(point, 0, point, 0, 1);
        assertEquals(-90, toDegrees(point[0]), tolerance);
        assertEquals( -8, toDegrees(point[1]), tolerance);
    }

    /**
     * Creates a projection and tests the derivatives at a few points.
     * The projection works with radians on an ellipsoid of semi-major axis length 1.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        tolerance = 1E-9;
        final double delta = toRadians(100.0 / 60) / 1852;              // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};

        // Polar projection.
        createProjection(true, 90, false);
        verifyDerivative(toRadians(-6), toRadians(80));

        // Intentionally above the pole.
        verifyDerivative(toRadians(-6), toRadians(100));

        // Polar projection, spherical formulas.
        createProjection(false, 90, false);
        verifyDerivative(toRadians(-6), toRadians(85));

        // Equatorial projection, spherical formulas.
        createProjection(false, 0, false);
        verifyDerivative(toRadians(3), toRadians(4));

        // Oblique projection, ellipsoidal formulas.
        createProjection(true, 8, false);
        verifyDerivative(toRadians(-6), toRadians(2));

        // Oblique projection, spherical formulas.
        createProjection(false, 8, false);
        verifyDerivative(toRadians(-6), toRadians(2));
    }
}
