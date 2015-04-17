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
import org.apache.sis.internal.referencing.provider.LambertConformal1SP;
import org.apache.sis.internal.referencing.provider.LambertConformal2SP;
import org.apache.sis.internal.referencing.provider.LambertConformalBelgium;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static java.lang.Double.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link LambertConformal} class. We test using various values of the latitude of origin.
 * We do not test with various values of standard parallels, because it is just an other way to set
 * the value of the <var>n</var> field in {@code LambertConformal}. As long as we make this value varying,
 * the latitude of origin is the simplest approach.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class LambertConformalTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link LambertConformal}. See the class javadoc for an explanation
     * about why we ask only for the latitude of origin and not the standard parallels.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @param latitudeOfOrigin The latitude of origin, in decimal degrees.
     */
    private void initialize(final boolean ellipse, final double latitudeOfOrigin) {
        final LambertConformal1SP method = new LambertConformal1SP();
        final Parameters parameters = parameters(method, ellipse);
        parameters.getOrCreate(LambertConformal1SP.LATITUDE_OF_ORIGIN).setValue(latitudeOfOrigin);
        transform = new LambertConformal(method, parameters);
        if (!ellipse) {
            transform = new LambertConformal.Spherical((LambertConformal) transform);
        }
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the WKT formatting of {@link NormalizedProjection}. For the Lambert Conformal projection, we expect
     * the standard parallels or the latitude of origin in addition to the semi-major and semi-minor axis length.
     */
    @Test
    public void testNormalizedWKT() {
        initialize(true, 40);
        assertWktEquals(
                "PARAM_MT[“Lambert_Conformal_Conic_1SP”,\n" +
                "  PARAMETER[“semi_major”, 1.0],\n" +
                "  PARAMETER[“semi_minor”, 0.9966471893352525],\n" +
                "  PARAMETER[“latitude_of_origin”, 40.0]]");
    }

    /**
     * Tests the projection at some special latitudes (0, ±π/2, NaN and others).
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    public void testSpecialLatitudes() throws ProjectionException {
        if (transform == null) {    // May have been initialized by 'testSphericalCase'.
            initialize(true, 40);   // Elliptical case
        }
        final double INF = POSITIVE_INFINITY;
        assertEquals ("Not a number",     NaN, transform(NaN),            NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(+2),             NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(-2),             NORMALIZED_TOLERANCE);
        assertEquals ("Forward 0°N",      1,   transform(0),              NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°N",     0,   transform(+PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°S",     INF, transform(-PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°N", 0,   transform(+nextUp(+PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°S", INF, transform(-nextUp( PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90-ε)°N", 0,   transform(-nextUp(-PI/2)), 1E-10);

        assertEquals ("Not a number", NaN, inverseTransform(NaN),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse 0",  +PI/2, inverseTransform( 0),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse +1",     0, inverseTransform(+1),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse -1",     0, inverseTransform(-1),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse +∞", -PI/2, inverseTransform(INF),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse -∞", -PI/2, inverseTransform(-INF), NORMALIZED_TOLERANCE);

        // Like the north case, but with sign inversed.
        initialize(((LambertConformal) transform).excentricity != 0, -40);
        validate();

        assertEquals ("Not a number",     NaN, transform(NaN),            NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(+2),             NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(-2),             NORMALIZED_TOLERANCE);
        assertEquals ("Forward 0°N",      1,   transform(0),              NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°N",     INF, transform(+PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°S",     0,   transform(-PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°N", INF, transform(+nextUp(+PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°S", 0,   transform(-nextUp( PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90-ε)°S", 0,   transform( nextUp(-PI/2)), 1E-10);

        assertEquals ("Not a number", NaN, inverseTransform(NaN),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse 0",  -PI/2, inverseTransform( 0),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse +∞", +PI/2, inverseTransform(INF),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse -∞", +PI/2, inverseTransform(-INF), NORMALIZED_TOLERANCE);
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
        if (transform == null) {    // May have been initialized by 'testSphericalCase'.
            initialize(true, 40);   // Elliptical case
        }
        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-9;
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(15), toRadians(30));
        verifyDerivative(toRadians(10), toRadians(60));
    }

    /**
     * Tests the <cite>"Lambert Conic Conformal (1SP)"</cite> case (EPSG:9801).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testLambertConicConformal1SP()
     */
    @Test
    @DependsOnMethod({"testSpecialLatitudes", "testDerivative"})
    public void testLambertConicConformal1SP() throws FactoryException, TransformException {
        createGeoApiTest(new LambertConformal1SP()).testLambertConicConformal1SP();
    }

    /**
     * Tests the <cite>"Lambert Conic Conformal (2SP)"</cite> case (EPSG:9802).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testLambertConicConformal1SP()
     */
    @Test
    @DependsOnMethod("testLambertConicConformal1SP")
    public void testLambertConicConformal2SP() throws FactoryException, TransformException {
        createGeoApiTest(new LambertConformal2SP()).testLambertConicConformal2SP();
    }

    /**
     * Tests the <cite>"Lambert Conic Conformal (2SP Belgium)"</cite> case (EPSG:9803).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testLambertConicConformal1SP()
     */
    @Test
    @DependsOnMethod("testLambertConicConformal2SP")
    public void testLambertConicConformalBelgium() throws FactoryException, TransformException {
        createGeoApiTest(new LambertConformalBelgium()).testLambertConicConformalBelgium();
    }

    /**
     * Verifies the consistency of spherical formulas with the elliptical formulas.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod("testSpecialLatitudes")
    public void testSphericalCase() throws FactoryException, TransformException {
        initialize(false, 40); // Spherical case
        testSpecialLatitudes();
        testDerivative();
        /*
         * Make sure that the above methods did not changed the 'transform' field type.
         */
        assertEquals("transform.class", LambertConformal.Spherical.class, transform.getClass());
        /*
         * For some random points, compare the result of spherical formulas with the ellipsoidal ones.
         */
        initialize(new LambertConformal1SP(), false, true, false, true);
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_SAFE, 268617081);
    }
}
