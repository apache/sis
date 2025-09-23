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

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.provider.Mercator2SP;
import org.apache.sis.referencing.operation.provider.MercatorAuxiliarySphere;
import org.apache.sis.referencing.operation.provider.PseudoMercator;
import org.apache.sis.referencing.operation.provider.MillerCylindrical;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;
import static org.apache.sis.referencing.operation.projection.ConformalProjectionTest.LN_INFINITY;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;

// Specific to the main branch:
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.apache.sis.test.GeoapiAssert.assertBetween;
import static org.apache.sis.test.GeoapiAssert.PENDING_NEXT_GEOAPI_RELEASE;


/**
 * Tests the {@link Mercator} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class MercatorTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public MercatorTest() {
    }

    /**
     * Creates a new instance of {@link Mercator} for a sphere or an ellipsoid.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param  ellipsoidal  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createNormalizedProjection(final boolean ellipsoidal) {
        final Mercator2SP method = new Mercator2SP();
        transform = new Mercator(method, parameters(method, ellipsoidal));
        if (!ellipsoidal) {
            transform = new Mercator.Spherical((Mercator) transform);
        }
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the WKT formatting of {@link NormalizedProjection}. For the Mercator projection, we expect only
     * the ellipsoid eccentricity. We expect nothing else because all other parameters are used
     * by the (de)normalization affine transforms instead of the {@link Mercator} class itself.
     *
     * @throws NoninvertibleTransformException if the transform cannot be inverted.
     *
     * @see LambertConicConformalTest#testNormalizedWKT()
     */
    @Test
    public void testNormalizedWKT() throws NoninvertibleTransformException {
        createNormalizedProjection(true);
        assertWktEquals("PARAM_MT[“Mercator (radians domain)”,\n" +
                        "  PARAMETER[“eccentricity”, 0.0818191908426215]]");

        transform = transform.inverse();
        assertWktEquals("INVERSE_MT[\n" +
                        "  PARAM_MT[“Mercator (radians domain)”,\n" +
                        "    PARAMETER[“eccentricity”, 0.0818191908426215]]]");
    }

    /**
     * Tests WKT of a complete map projection.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws NoninvertibleTransformException if the transform cannot be inverted.
     */
    @Test
    public void testCompleteWKT() throws FactoryException, NoninvertibleTransformException {
        createCompleteProjection(new Mercator1SP(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                0.5,        // Central meridian
                NaN,        // Latitude of origin (none)
                NaN,        // Standard parallel 1 (none)
                NaN,        // Standard parallel 2 (none)
                0.997,      // Scale factor
                200,        // False easting
                100);       // False northing

        assertWktEquals("PARAM_MT[“Mercator_1SP”,\n" +
                        "  PARAMETER[“semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“semi_minor”, 6356752.314245179],\n" +
                        "  PARAMETER[“central_meridian”, 0.5],\n" +
                        "  PARAMETER[“scale_factor”, 0.997],\n" +
                        "  PARAMETER[“false_easting”, 200.0],\n" +
                        "  PARAMETER[“false_northing”, 100.0]]");

        transform = transform.inverse();
        assertWktEquals("INVERSE_MT[\n" +
                        "  PARAM_MT[“Mercator_1SP”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179],\n" +
                        "    PARAMETER[“central_meridian”, 0.5],\n" +
                        "    PARAMETER[“scale_factor”, 0.997],\n" +
                        "    PARAMETER[“false_easting”, 200.0],\n" +
                        "    PARAMETER[“false_northing”, 100.0]]]");
    }

    /**
     * Tests the projection at some special latitudes (0, ±π/2, NaN).
     *
     * @throws ProjectionException if an error occurred while projecting a point.
     */
    @Test
    public void testSpecialLatitudes() throws ProjectionException {
        if (transform == null) {                    // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true);       // Elliptical case
        }
        assertEquals(NaN,               transform(NaN),             tolerance, "Not a number");
        assertEquals(NaN,               transform(+2),              tolerance, "Out of range");
        assertEquals(NaN,               transform(-2),              tolerance, "Out of range");
        assertEquals(0,                 transform(0),               tolerance, "Forward 0°N");
        assertEquals(POSITIVE_INFINITY, transform(+PI/2),           tolerance, "Forward 90°N");
        assertEquals(NEGATIVE_INFINITY, transform(-PI/2),           tolerance, "Forward 90°S");
        assertEquals(POSITIVE_INFINITY, transform(nextUp  ( PI/2)), tolerance, "Forward (90+ε)°N");
        assertEquals(NEGATIVE_INFINITY, transform(nextDown(-PI/2)), tolerance, "Forward (90+ε)°S");
        assertBetween(+MIN_VALUE, +MAX_VALUE, transform(nextDown( PI/2)), "Forward (90-ε)°N");
        assertBetween(-MAX_VALUE, -MIN_VALUE, transform(nextUp  (-PI/2)), "Forward (90-ε)°S");

        assertEquals(NaN,   inverseTransform(NaN),                tolerance, "Not a number");
        assertEquals(0,     inverseTransform(0),                  tolerance, "Inverse 0 m");
        assertEquals(+PI/2, inverseTransform(POSITIVE_INFINITY),  tolerance, "Inverse +∞");
        assertEquals(+PI/2, inverseTransform(LN_INFINITY + 1),    tolerance, "Inverse +∞ appr.");
        assertEquals(-PI/2, inverseTransform(NEGATIVE_INFINITY),  tolerance, "Inverse −∞");
        assertEquals(-PI/2, inverseTransform(-(LN_INFINITY + 1)), tolerance, "Inverse −∞ appr.");
    }

    /**
     * Tests the derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testDerivative() throws TransformException {
        if (transform == null) {                                // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true);                   // Elliptical case
        }
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-9;                                       // More severe than Formulas.LINEAR_TOLERANCE.
        verifyDerivative(toRadians(15), toRadians( 30));
        verifyDerivative(toRadians(10), toRadians(-60));
    }

    /**
     * Tests the <q>Mercator (variant A)</q> case (EPSG:9804).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testMercator1SP() throws FactoryException, TransformException {
        createGeoApiTest(new Mercator1SP()).testMercator1SP();
    }

    /**
     * Tests the <q>Mercator (variant B)</q> case (EPSG:9805).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testMercator2SP() throws FactoryException, TransformException {
        createGeoApiTest(new Mercator2SP()).testMercator2SP();
    }

    /**
     * Tests the <q>Mercator (variant C)</q> case (EPSG:1044).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testRegionalMercator() throws FactoryException, TransformException {
        assumeTrue(PENDING_NEXT_GEOAPI_RELEASE);   // Test not available in GeoAPI 3.0
    }

    /**
     * Tests the <q>Mercator (Spherical)</q> case (EPSG:1026).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testMercatorSpherical() throws FactoryException, TransformException {
        assumeTrue(PENDING_NEXT_GEOAPI_RELEASE);   // Test not available in GeoAPI 3.0
    }

    /**
     * Tests the <q>Popular Visualisation Pseudo Mercator</q> case (EPSG:1024).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testPseudoMercator() throws FactoryException, TransformException {
        createGeoApiTest(new PseudoMercator()).testPseudoMercator();
    }

    /**
     * Tests the <q>Miller Cylindrical</q> case.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testMiller() throws FactoryException, TransformException {
        createGeoApiTest(new MillerCylindrical()).testMiller();
    }

    /**
     * Tests the <q>Mercator Auxiliary Sphere</q> case with type 3.
     * This type mandate conversion between geodetic latitude and authalic latitude.
     * The values used in this test are close to <q>Mercator (Spherical)</q> (EPSG:1026) case.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testAuthalicLatitudeConversion() throws FactoryException, TransformException {
        final double[] source = {-100.33333333333333, 24.381786944444446};
        final double[] target = {
            -11156569.90  -      0.31,      // Expected value from spherical test − empirical correction.
              2796869.94  -  11759.14
        };
        createAuxiliarySphereProjection(AuthalicMercator.TYPE);
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(source, target);
    }

    /**
     * Tests the <q>Mercator Auxiliary Sphere</q> case.
     * For the sphere type 0, which is the default, this is equivalent to pseudo-Mercator.
     * This simple test measures the length of an arc of 1 radian at equator.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testMercatorAuxiliarySphere() throws FactoryException, TransformException {
        tolerance = Formulas.LINEAR_TOLERANCE;
        for (int type = 0; type <= AuthalicMercator.TYPE; type++) {
            createAuxiliarySphereProjection(type);
            final double expected;
            switch (type) {
                case 0: expected = WGS84_A;    break;   // 6378137
                case 1: expected = WGS84_B;    break;   // 6356752.31
                case 2: expected = 6371007.18; break;   // Authalic radius
                case 3: expected = 6371007.18; break;
                default: throw new AssertionError(type);
            }
            verifyTransform(new double[] {180/Math.PI, 0, 0, 0}, new double[] {expected, 0, 0, 0});
        }
    }

    /**
     * Creates an ESRI  <q>Mercator Auxiliary Sphere</q> projection.
     * The axis lengths are those of WGS 84, which result in an authalic radius of about 6371007 meters.
     *
     * @param  type  the <q>Auxiliary sphere type</q> parameter value.
     * @throws FactoryException if an error occurred while creating the map projection.
     */
    private void createAuxiliarySphereProjection(final int type) throws FactoryException {
        final var provider = new MercatorAuxiliarySphere();
        final Parameters values = Parameters.castOrWrap(provider.getParameters().createValue());
        values.parameter(Constants.SEMI_MAJOR).setValue(WGS84_A);
        values.parameter(Constants.SEMI_MINOR).setValue(WGS84_B);
        values.parameter("Auxiliary_Sphere_Type").setValue(type);
        transform = provider.createMathTransform(new MathTransformFactoryMock(provider), values);
        validate();
    }

    /**
     * Performs the same tests as {@link #testSpecialLatitudes()} and {@link #testDerivative()},
     * but using spherical formulas.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSphericalCase() throws FactoryException, TransformException {
        createNormalizedProjection(false);          // Spherical case
        testSpecialLatitudes();
        testDerivative();

        // Make sure that the above methods did not overwrote the 'transform' field.
        assertEquals(Mercator.Spherical.class, transform.getClass());
    }

    /**
     * Verifies the consistency of elliptical formulas with the spherical formulas.
     * This test compares the results of elliptical formulas with the spherical ones
     * for some random points.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void compareEllipticalWithSpherical() throws FactoryException, TransformException {
        createCompleteProjection(new Mercator1SP(),
                6371007,    // Semi-major axis length
                6371007,    // Semi-minor axis length
                0,          // Central meridian
                NaN,        // Latitude of origin (none)
                NaN,        // Standard parallel 1 (none)
                NaN,        // Standard parallel 2 (none)
                0.997,      // Scale factor
                200,        // False easting
                100);       // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(CoordinateDomain.GEOGRAPHIC_SAFE, 0);
    }

    /**
     * Tests points with a projection having central meridian at 100°E. Conversion of points
     * at -179° and 181° of longitude (on the same latitude) should give the same result.
     * Those points are located at only 81° of central meridian.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-547">SIS-547</a>
     */
    @Test
    public void testWraparound() throws FactoryException, TransformException {
        // Use "WGS 84 / Mercator 41" (EPSG:3994)
        createCompleteProjection(new Mercator2SP(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                100,        // Central meridian
                NaN,        // Latitude of origin (none)
                -41,        // Standard parallel 1
                NaN,        // Standard parallel 2 (none)
                1,          // Scale factor
                0,          // False easting
                0);         // False northing

        final double[] coordinates = {179, -41, 181, -41, -179, -41};
        final double[] expected = {
            6646679.62, -3767131.99,
            6814949.99, -3767131.99,
            6814949.99, -3767131.99     // This result was different before SIS-547 fix.
        };
        isInverseTransformSupported = false;
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(coordinates, expected);
        /*
         * Replace the 181° longitude by -179° so we can test reverse projection.
         */
        coordinates[2] = -179;
        isInverseTransformSupported = true;
        verifyTransform(coordinates, expected);
    }
}
