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
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.TransverseMercatorSouth;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link TransverseMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class TransverseMercatorTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link TransverseMercator}.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createNormalizedProjection(final boolean ellipse, final double latitudeOfOrigin) {
        final org.apache.sis.internal.referencing.provider.TransverseMercator method =
                new org.apache.sis.internal.referencing.provider.TransverseMercator();
        final Parameters parameters = parameters(method, ellipse);
        parameters.getOrCreate(org.apache.sis.internal.referencing.provider.TransverseMercator.LATITUDE_OF_ORIGIN).setValue(latitudeOfOrigin);
        transform = new TransverseMercator(method, parameters);
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the <cite>Transverse Mercator</cite> case (EPSG:9807).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testTransverseMercator()
     */
    @Test
    public void testTransverseMercator() throws FactoryException, TransformException {
        new org.apache.sis.internal.referencing.provider.TransverseMercator();  // Test creation only, as GeoAPI 3.0 did not yet had the test method.
    }

    /**
     * Tests the <cite>Transverse Mercator (South Orientated)</cite> case (EPSG:9808).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testTransverseMercatorSouthOrientated()
     */
    @Test
    @DependsOnMethod("testTransverseMercator")
    public void testTransverseMercatorSouthOrientated() throws FactoryException, TransformException {
        new TransverseMercatorSouth();  // Test creation only, as GeoAPI 3.0 did not yet had the test method.
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
    @DependsOnMethod("testTransverseMercator")
    public void compareEllipticalWithSpherical() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.TransverseMercator(), false,
                  0.5,    // Central meridian
                  2.5,    // Latitude of origin
                  0,      // Standard parallel (none)
                  0.997,  // Scale factor
                200,      // False easting
                100);     // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(CoordinateDomain.RANGE_10, 0);
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testSphericalDerivative() throws TransformException {
        createNormalizedProjection(false, 0);
        tolerance = 1E-9;

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testEllipsoidalDerivative() throws TransformException {
        createNormalizedProjection(true, 0);
        tolerance = 1E-9;

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }

    /**
     * Verifies that deserialized projections work as expected. This implies that deserialization
     * recomputed the internal transient fields, especially the series expansion coefficients.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod("testTransverseMercator")
    public void testSerialization() throws FactoryException, TransformException {
        createNormalizedProjection(true, 40);
        /*
         * Use a fixed seed for the random number generator in this test, because in case of failure this class will not
         * report which seed it used. This limitation exists because this test class does not extend the SIS TestCase.
         */
        final double[] source = CoordinateDomain.GEOGRAPHIC_RADIANS_HALF_λ.generateRandomInput(new Random(5346144739450824145L), 2, 10);
        final double[] target = new double[source.length];
        transform.transform(source, 0, target, 0, 10);
        transform = assertSerializedEquals(transform);
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(source, target);
    }
}
