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

import java.util.Random;
import java.math.BigDecimal;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.LambertConformal1SP;
import org.apache.sis.internal.referencing.provider.LambertConformal2SP;
import org.apache.sis.internal.referencing.provider.LambertConformalWest;
import org.apache.sis.internal.referencing.provider.LambertConformalBelgium;
import org.apache.sis.internal.referencing.provider.LambertConformalMichigan;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static java.lang.Double.*;
import static org.apache.sis.test.Assert.*;

// Branch-specific imports
import static org.junit.Assume.assumeTrue;
import static org.apache.sis.test.Assert.PENDING_NEXT_GEOAPI_RELEASE;


/**
 * Tests the {@link LambertConicConformal} class. We test using various values of the latitude of origin.
 * We do not test with various values of standard parallels, because it is just an other way to set
 * the value of the <var>n</var> field in {@code LambertConicConformal}. As long as we make this value varying,
 * the latitude of origin is the simplest approach.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn(ConformalProjectionTest.class)
public final strictfp class LambertConicConformalTest extends MapProjectionTestCase {
    /**
     * Verifies the value of the constant used in <cite>"Lambert Conic Conformal (2SP Belgium)"</cite> projection.
     *
     * @see #testLambertConicConformalBelgium()
     */
    @Test
    public void verifyBelgeConstant() {
        final DoubleDouble BELGE_A = (DoubleDouble) LambertConicConformal.belgeA();
        BigDecimal a = new BigDecimal(BELGE_A.value);
        a = a.add     (new BigDecimal(BELGE_A.error));
        a = a.multiply(new BigDecimal("57.29577951308232087679815481410517"));  // Conversion from radians to degrees.
        a = a.multiply(new BigDecimal(60 * 60));                                // Conversion from degrees to seconds.
        a = a.add     (new BigDecimal("29.2985"));                              // The standard value.
        assertTrue(abs(a.doubleValue()) < 1E-31);
    }

    /**
     * Creates a new instance of {@link LambertConicConformal}. See the class javadoc for an explanation
     * about why we ask only for the latitude of origin and not the standard parallels.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @param latitudeOfOrigin The latitude of origin, in decimal degrees.
     */
    private void createNormalizedProjection(final boolean ellipse, final double latitudeOfOrigin) {
        final LambertConformal1SP method = new LambertConformal1SP();
        final Parameters parameters = parameters(method, ellipse);
        parameters.getOrCreate(LambertConformal1SP.LATITUDE_OF_ORIGIN).setValue(latitudeOfOrigin);
        transform = new LambertConicConformal(method, parameters);
        if (!ellipse) {
            transform = new LambertConicConformal.Spherical((LambertConicConformal) transform);
        }
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the WKT formatting of {@link NormalizedProjection}. For the Lambert Conformal projection, we expect
     * the internal {@code n} parameter in addition to the eccentricity.
     *
     * <div class="section">Note on accuracy</div>
     * The value of the eccentricity parameter should be fully accurate because it is calculated using only the
     * {@link Math#sqrt(double)} function (ignoring basic algebraic operations) which, according javadoc, must
     * give the result closest to the true mathematical result. But the functions involved in the calculation of
     * <var>n</var> do not have such strong guarantees. So we use a regular expression in this test for ignoring
     * the 2 last digits of <var>n</var>.
     */
    @Test
    public void testNormalizedWKT() {
        createNormalizedProjection(true, 40);
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“Lambert conic conformal (radians domain)”,\n" +
                "  PARAMETER[“eccentricity”, 0.0818191908426215],\n" +
                "  PARAMETER[“n”, 0.64278760968653\\E\\d*\\Q]]\\E");  // 0.6427876096865393 in the original test.
    }

    /**
     * Tests the projection at some special latitudes (0, ±π/2, NaN and others).
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    public void testSpecialLatitudes() throws ProjectionException {
        if (transform == null) {    // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true, 40);  // Elliptical case
        }
        final double INF = POSITIVE_INFINITY;
        assertEquals ("Not a number",     NaN, transform(NaN),            NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(+2),             NORMALIZED_TOLERANCE);
        assertEquals ("Out of range",     NaN, transform(-2),             NORMALIZED_TOLERANCE);
        assertEquals ("Forward 0°N",      1,   transform(0),              NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°S",     0,   transform(-PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward 90°N",     INF, transform(+PI/2),          NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°S", 0,   transform(-nextUp( PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90+ε)°N", INF, transform(+nextUp(+PI/2)), NORMALIZED_TOLERANCE);
        assertEquals ("Forward (90-ε)°S", 0,   transform(+nextUp(-PI/2)), 1E-10);

        assertEquals ("Not a number", NaN, inverseTransform(NaN),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse 0",  -PI/2, inverseTransform( 0),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse +1",     0, inverseTransform(+1),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse -1",     0, inverseTransform(-1),   NORMALIZED_TOLERANCE);
        assertEquals ("Inverse +∞", +PI/2, inverseTransform(INF),  NORMALIZED_TOLERANCE);
        assertEquals ("Inverse -∞", +PI/2, inverseTransform(-INF), NORMALIZED_TOLERANCE);

        // Like the north case, but with sign inversed.
        createNormalizedProjection(((LambertConicConformal) transform).eccentricity != 0, -40);
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
        if (transform == null) {                                // May have been initialized by 'testSphericalCase'.
            createNormalizedProjection(true, 40);               // Elliptical case
        }
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
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
    @DependsOnMethod({"testLambertConicConformal2SP", "verifyBelgeConstant"})
    public void testLambertConicConformalBelgium() throws FactoryException, TransformException {
        createGeoApiTest(new LambertConformalBelgium()).testLambertConicConformalBelgium();
    }

    /**
     * Tests the <cite>"Lambert Conic Conformal (2SP Michigan)"</cite> case (EPSG:1051).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testLambertConicConformalMichigan()
     */
    @Test
    @DependsOnMethod("testLambertConicConformal2SP")
    public void testLambertConicConformalMichigan() throws FactoryException, TransformException {
        assumeTrue(PENDING_NEXT_GEOAPI_RELEASE);   // Test not available in GeoAPI 3.0
    }

    /**
     * Tests the <cite>"Lambert Conic Conformal (1SP West Orientated)"</cite> case (EPSG:9826)).
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod("testLambertConicConformal1SP")
    public void testLambertConicConformalWestOrientated() throws FactoryException, TransformException {
        createCompleteProjection(new LambertConformal1SP(), false,
                  0.5,    // Central meridian
                 40,      // Latitude of origin
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing
        final MathTransform reference = transform;

        createCompleteProjection(new LambertConformalWest(), false,
                  0.5,    // Central meridian
                 40,      // Latitude of origin
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing

        final Random random = TestUtilities.createRandomNumberGenerator();
        final double[] sources = new double[20];
        for (int i=0; i<sources.length;) {
            sources[i++] = 20 * random.nextDouble();      // Longitude
            sources[i++] = 10 * random.nextDouble() + 35; // Latitude
        }
        final double[] expected = new double[sources.length];
        reference.transform(sources, 0, expected, 0, sources.length/2);
        /*
         * At this point, we have the source coordinates and the expected projected coordinates calculated
         * by the "Lambert Conic Conformal (1SP)" method. Now convert those projected coordinates into the
         * coordinates that we expect from the "Lambert Conic Conformal (1SP West Orientated)".  If we had
         * no false easting, we would just revert the sign of 'x' values. But because of the false easting,
         * we expect an additional offset of two time that easting. This is because (quoting the EPSG guide):
         *
         *    the term FE retains its definition, i.e. in the Lambert Conic Conformal (West Orientated)
         *    method it increases the Westing value at the natural origin.
         *    In this method it is effectively false westing (FW).
         *
         * So the conversion for this test case should be:     W = 400 - E
         *
         * However our map projection "kernel" implementation does not reverse the sign of 'x' values,
         * because this reversal is the job of a separated method (CoordinateSystems.swapAndScaleAxes)
         * which does is work by examining the axis directions. So we the values that we expect are:
         *
         *     expected  =  -W  =  E - 400
         */
        for (int i=0; i<sources.length; i += 2) {
            expected[i] -= 400;
        }
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(sources, expected);
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
        createNormalizedProjection(false, 40); // Spherical case
        testSpecialLatitudes();
        testDerivative();

        // Make sure that the above methods did not overwrote the 'transform' field.
        assertEquals("transform.class", LambertConicConformal.Spherical.class, transform.getClass());
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
        createCompleteProjection(new LambertConformal1SP(), false,
                  0.5,    // Central meridian
                 40,      // Latitude of origin
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(CoordinateDomain.GEOGRAPHIC_SAFE, 0);
    }

    /**
     * Verifies that deserialized projections work as expected. This implies that deserialization
     * recomputed the internal transient fields, especially the series expansion coefficients.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod("testLambertConicConformal1SP")
    public void testSerialization() throws FactoryException, TransformException {
        createNormalizedProjection(true, 40);
        final double[] source = CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH.generateRandomInput(TestUtilities.createRandomNumberGenerator(), 2, 10);
        final double[] target = new double[source.length];
        transform.transform(source, 0, target, 0, 10);
        transform = assertSerializedEquals(transform);
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(source, target);
    }
}
