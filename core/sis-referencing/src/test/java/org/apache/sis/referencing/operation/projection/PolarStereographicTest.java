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
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.internal.referencing.provider.PolarStereographicB;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.*;


/**
 * Tests the {@link PolarStereographic} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class PolarStereographicTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link PolarStereographic}.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @param latitudeOfOrigin The latitude of origin, in decimal degrees.
     */
    private void createNormalizedProjection(final boolean ellipse, final double latitudeOfOrigin) {
        final PolarStereographicA method = new PolarStereographicA();
        final Parameters parameters = parameters(method, ellipse);
        parameters.getOrCreate(PolarStereographicA.LATITUDE_OF_ORIGIN).setValue(latitudeOfOrigin);
        NormalizedProjection projection = new PolarStereographic(method, parameters);
        if (!ellipse) {
            projection = new ProjectionResultComparator(projection,
                    new PolarStereographic.Spherical((PolarStereographic) projection));
        }
        transform = projection;
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Verifies the consistency between spherical and elliptical formulas in the South pole.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSphericalCaseSouth() throws FactoryException, TransformException {
        createNormalizedProjection(false, -90);
        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_SOUTH, 56763886);
    }

    /**
     * Verifies the consistency between spherical and elliptical formulas in the North pole.
     * This is the same formulas than the South case, but with the sign of some coefficients negated.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    @DependsOnMethod("testSphericalCaseSouth")
    public void testSphericalCaseNorth() throws FactoryException, TransformException {
        createNormalizedProjection(false, 90);
        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH, 56763886);
    }

    /**
     * Tests the <cite>Polar Stereographic (variant A)</cite> case (EPSG:9810).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testPolarStereographicA()
     */
    @Test
    public void testPolarStereographicA() throws FactoryException, TransformException {
        createGeoApiTest(new PolarStereographicA()).testPolarStereographicA();
    }

    /**
     * Tests the <cite>Polar Stereographic (variant B)</cite> case (EPSG:9829).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testPolarStereographicB()
     */
    @Test
    public void testPolarStereographicB() throws FactoryException, TransformException {
        createGeoApiTest(new PolarStereographicB()).testPolarStereographicB();
    }
}
