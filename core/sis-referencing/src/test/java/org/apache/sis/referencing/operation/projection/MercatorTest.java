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
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.internal.referencing.provider.PseudoMercator;
import org.apache.sis.internal.referencing.provider.MillerCylindrical;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import static org.opengis.test.Assert.*;
import static org.apache.sis.referencing.operation.projection.ConformalProjectionTest.LN_INFINITY;

// Branch-specific imports
import static org.junit.Assume.assumeTrue;
import static org.apache.sis.test.Assert.PENDING_NEXT_GEOAPI_RELEASE;


/**
 * Tests the {@link Mercator} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn(ConformalProjectionTest.class)
public final strictfp class MercatorTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link Mercator} for a sphere or an ellipsoid.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createNormalizedProjection(final boolean ellipse) {
        final Mercator2SP method = new Mercator2SP();
        transform = new Mercator(method, parameters(method, ellipse));
        if (!ellipse) {
            transform = new Mercator.Spherical((Mercator) transform);
        }
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the WKT formatting of {@link NormalizedProjection}. For the Mercator projection, we expect only
     * the ellipsoid eccentricity. We expect nothing else because all other parameters are used
     * by the (de)normalization affine transforms instead than the {@link Mercator} class itself.
     *
     * @throws NoninvertibleTransformException should never happen.
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
     * @throws NoninvertibleTransformException should never happen.
     */
    @Test
    @DependsOnMethod("testNormalizedWKT")
    public void testCompleteWKT() throws FactoryException, NoninvertibleTransformException {
        createCompleteProjection(new Mercator1SP(), true,
                  0.5,    // Central meridian
                  0,      // Latitude of origin (none)
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing

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
     * @throws ProjectionException Should never happen.
     */
    @Test
    public void testSpecialLatitudes() throws ProjectionException {
        if (transform == null) {    // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true);       // Elliptical case
        }
        assertEquals ("Not a number",     NaN,                    transform(NaN),           tolerance);
        assertEquals ("Out of range",     NaN,                    transform(+2),            tolerance);
        assertEquals ("Out of range",     NaN,                    transform(-2),            tolerance);
        assertEquals ("Forward 0°N",      0,                      transform(0),             tolerance);
        assertEquals ("Forward 90°N",     POSITIVE_INFINITY,      transform(+PI/2),         tolerance);
        assertEquals ("Forward 90°S",     NEGATIVE_INFINITY,      transform(-PI/2),         tolerance);
        assertEquals ("Forward (90+ε)°N", POSITIVE_INFINITY,      transform(+nextUp(PI/2)), tolerance);
        assertEquals ("Forward (90+ε)°S", NEGATIVE_INFINITY,      transform(-nextUp(PI/2)), tolerance);
        assertBetween("Forward (90-ε)°N", +MIN_VALUE, +MAX_VALUE, transform(-nextUp(-PI/2)));
        assertBetween("Forward (90-ε)°S", -MAX_VALUE, -MIN_VALUE, transform(+nextUp(-PI/2)));

        assertEquals ("Not a number",     NaN,   inverseTransform(NaN),                tolerance);
        assertEquals ("Inverse 0 m",      0,     inverseTransform(0),                  tolerance);
        assertEquals ("Inverse +∞",       +PI/2, inverseTransform(POSITIVE_INFINITY),  tolerance);
        assertEquals ("Inverse +∞ appr.", +PI/2, inverseTransform(LN_INFINITY + 1),    tolerance);
        assertEquals ("Inverse -∞",       -PI/2, inverseTransform(NEGATIVE_INFINITY),  tolerance);
        assertEquals ("Inverse -∞ appr.", -PI/2, inverseTransform(-(LN_INFINITY + 1)), tolerance);
    }

    /**
     * Tests the derivatives at a few points. This method compares the derivatives computed by
     * the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    @DependsOnMethod("testSpecialLatitudes")
    public void testDerivative() throws TransformException {
        if (transform == null) {                                // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true);                   // Elliptical case
        }
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-9;                                       // More severe than Formulas.LINEAR_TOLERANCE.
        verifyDerivative(toRadians(15), toRadians( 30));
        verifyDerivative(toRadians(10), toRadians(-60));
    }

    /**
     * Tests the <cite>"Mercator (variant A)"</cite> case (EPSG:9804).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMercator1SP()
     */
    @Test
    @DependsOnMethod({"testSpecialLatitudes", "testDerivative"})
    public void testMercator1SP() throws FactoryException, TransformException {
        createGeoApiTest(new Mercator1SP()).testMercator1SP();
    }

    /**
     * Tests the <cite>"Mercator (variant B)"</cite> case (EPSG:9805).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMercator2SP()
     */
    @Test
    @DependsOnMethod("testMercator1SP")
    public void testMercator2SP() throws FactoryException, TransformException {
        createGeoApiTest(new Mercator2SP()).testMercator2SP();
    }

    /**
     * Tests the <cite>"Mercator (variant C)"</cite> case (EPSG:1044).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMercatorVariantC()
     */
    @Test
    @DependsOnMethod("testMercator2SP")
    public void testRegionalMercator() throws FactoryException, TransformException {
        assumeTrue(PENDING_NEXT_GEOAPI_RELEASE);   // Test not available in GeoAPI 3.0
    }

    /**
     * Tests the <cite>"Mercator (Spherical)"</cite> case (EPSG:1026).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMercatorSpherical()
     */
    @Test
    @DependsOnMethod("testMercator1SP")
    public void testMercatorSpherical() throws FactoryException, TransformException {
        assumeTrue(PENDING_NEXT_GEOAPI_RELEASE);   // Test not available in GeoAPI 3.0
    }

    /**
     * Tests the <cite>"Popular Visualisation Pseudo Mercator"</cite> case (EPSG:1024).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testPseudoMercator()
     */
    @Test
    @DependsOnMethod("testMercatorSpherical")
    public void testPseudoMercator() throws FactoryException, TransformException {
        createGeoApiTest(new PseudoMercator()).testPseudoMercator();
    }

    /**
     * Tests the <cite>"Miller Cylindrical"</cite> case.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMiller()
     */
    @Test
    @DependsOnMethod("testMercator1SP")
    public void testMiller() throws FactoryException, TransformException {
        createGeoApiTest(new MillerCylindrical()).testMiller();
    }

    /**
     * Performs the same tests than {@link #testSpecialLatitudes()} and {@link #testDerivative()},
     * but using spherical formulas.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod({"testSpecialLatitudes", "testDerivative"})
    public void testSphericalCase() throws FactoryException, TransformException {
        createNormalizedProjection(false); // Spherical case
        testSpecialLatitudes();
        testDerivative();

        // Make sure that the above methods did not overwrote the 'transform' field.
        assertEquals("transform.class", Mercator.Spherical.class, transform.getClass());
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
    @DependsOnMethod("testSphericalCase")
    public void compareEllipticalWithSpherical() throws FactoryException, TransformException {
        createCompleteProjection(new Mercator1SP(), false,
                  0.5,    // Central meridian
                  0,      // Latitude of origin (none)
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(CoordinateDomain.GEOGRAPHIC_SAFE, 0);
    }
}
