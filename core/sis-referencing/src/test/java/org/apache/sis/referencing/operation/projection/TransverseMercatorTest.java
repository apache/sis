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
import org.apache.sis.internal.referencing.provider.TransverseMercatorSouth;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;


/**
 * Tests the {@link TransverseMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
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
        createGeoApiTest(new org.apache.sis.internal.referencing.provider.TransverseMercator()).testTransverseMercator();
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
    public void testTransverseMercatorSouthOrientated() throws FactoryException, TransformException {
        createGeoApiTest(new TransverseMercatorSouth()).testTransverseMercatorSouthOrientated();
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

        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
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

        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }
}
