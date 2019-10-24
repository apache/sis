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
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOn;
import org.junit.*;

import static java.lang.StrictMath.*;


/**
 * Tests the {@link Orthographic} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class OrthographicTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link Orthographic} using spherical formulas.
     *
     * @param  φ0  latitude of projection centre.
     */
    private void createSpherical(final double φ0) {
        final MapProjection provider = new org.apache.sis.internal.referencing.provider.Orthographic();
        final Parameters parameters = parameters(provider, false);
        parameters.parameter("latitude_of_origin").setValue(φ0);
        transform = new Orthographic(provider, parameters);
        final double delta = toRadians(100.0 / 60) / 1852;              // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        validate();
    }

    /**
     * Tests the equatorial projection on a sphere.
     * This method uses points from Snyder table 22.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testEquatorial() throws TransformException {
        createSpherical(0);
        tolerance = 1E-4;                       // Accuracy of numbers provided in Snyder tables.
        verifyTransform(
            new double[] {                      // (λ,φ) coordinates in radians to project.
                toRadians( 0), toRadians( 0),
                toRadians(20), toRadians( 0),
                toRadians( 0), toRadians(40),
                toRadians(30), toRadians(20),
                toRadians(20), toRadians(80)
            },
            new double[] {                      // Expected (x,y) results.
                0.0,           0.0,
                0.3420,        0.0,
                0.0,           0.6428,
                0.4698,        0.3420,
                0.0594,        0.9848
            });

        tolerance = NORMALIZED_TOLERANCE;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_HALF_λ, 209067359);
        verifyDerivative(toRadians(5), toRadians(3));
    }

    /**
     * Tests the oblique projection on a sphere.
     * This method uses points from Snyder table 23.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testOblique() throws TransformException {
        createSpherical(40);
        tolerance = 1E-4;                       // Accuracy of numbers provided in Snyder tables.
        verifyTransform(
            new double[] {                      // (λ,φ) coordinates in radians to project.
                toRadians( 0), toRadians(40),
                toRadians( 0), toRadians( 0),
                toRadians(20), toRadians( 0),
                toRadians( 0), toRadians(50),
                toRadians(30), toRadians(20),
                toRadians(20), toRadians(80)
            },
            new double[] {                      // Expected (x,y) results.
                0.0,           0.0,
                0.0,          -0.6428,
                0.3420,       -0.6040,
                0.0,           0.1736,
                0.4698,       -0.2611,
                0.0594,        0.6495
            });

        tolerance = NORMALIZED_TOLERANCE;
        verifyDerivative(toRadians(5), toRadians(30));
    }

    /**
     * Tests the polar projection on a sphere.
     * There is no reference points for this method;
     * we just test consistency between forward and inverse methods.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testPolarNorth() throws TransformException {
        createSpherical(+90);
        tolerance = NORMALIZED_TOLERANCE;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH, 753524735);
        verifyDerivative(toRadians(5), toRadians(85));
    }

    /**
     * Tests the polar projection on a sphere.
     * There is no reference points for this method;
     * we just test consistency between forward and inverse methods.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testPolarSouth() throws TransformException {
        createSpherical(-90);
        tolerance = NORMALIZED_TOLERANCE;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_SOUTH, 753524735);
        verifyDerivative(toRadians(5), toRadians(-85));
    }

    /**
     * Tests the <cite>"Orthographic"</cite> (EPSG:9840) projection method.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testOrthographic()
     */
    @Test
    public void runGeoapiTest() throws FactoryException, TransformException {
        createGeoApiTest(new org.apache.sis.internal.referencing.provider.Orthographic()).testOrthographic();
    }
}
