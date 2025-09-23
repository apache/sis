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
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.referencing.operation.provider.PolarStereographicB;
import org.apache.sis.referencing.operation.provider.PolarStereographicC;
import org.apache.sis.referencing.operation.provider.PolarStereographicNorth;
import org.apache.sis.referencing.operation.provider.PolarStereographicSouth;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;


/**
 * Tests the {@link PolarStereographic} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PolarStereographicTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public PolarStereographicTest() {
    }

    /**
     * Creates a new instance of {@link PolarStereographic}.
     */
    private void createNormalizedProjection(final MapProjection method) {
        final Parameters parameters = parameters(method, false);
        NormalizedProjection projection = new PolarStereographic(method, parameters);
        projection = new ProjectionResultComparator(projection,
                new PolarStereographic.Spherical((PolarStereographic) projection));
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
        createNormalizedProjection(new PolarStereographicSouth());
        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_SOUTH, 56763886);
    }

    /**
     * Verifies the consistency between spherical and elliptical formulas in the North pole.
     * This is the same formulas as the South case, but with the sign of some coefficients negated.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSphericalCaseNorth() throws FactoryException, TransformException {
        createNormalizedProjection(new PolarStereographicNorth());
        final double delta = toRadians(100.0 / 60) / 1852;                // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC_RADIANS_NORTH, 56763886);
    }

    /**
     * Tests <q>Stereographic North Pole</q>. The tested point is adapted from
     * <a href="https://gdal.org/proj_list/polar_stereographic.html">Polar Stereographic
     * from GeoTIFF projections list</a>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testPolarStereographicNorth() throws FactoryException, TransformException {
        final var method = new PolarStereographicNorth();
        final Parameters pg = parameters(method, true);
        pg.parameter("standard_parallel_1").setValue(71.0);
        pg.parameter("central_meridian").setValue(-96.0);
        transform = method.createMathTransform(new MathTransformFactoryMock(method), pg);
        tolerance = 0.02;
        verifyTransform(new double[] {
            -121 - (20 + 22.380/60)/60,     // 121°20'22.380"W
              39 + ( 6 +  4.508/60)/60      //  39°06'04.508"N
        }, new double[] {
            -2529570,
            -5341800
        });
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

    /**
     * Tests the <cite>Polar Stereographic (variant C)</cite> case (EPSG:9830).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testPolarStereographicC()
     */
    @Test
    public void testPolarStereographicC() throws FactoryException, TransformException {
        createGeoApiTest(new PolarStereographicC()).testPolarStereographicC();
    }

    /**
     * Verifies the consistency of elliptical formulas with the spherical formulas.
     * This test compares the results of elliptical formulas with the spherical ones
     * for some random points.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    private void compareEllipticalWithSpherical(final CoordinateDomain domain, final double latitudeOfOrigin,
            final long randomSeed) throws FactoryException, TransformException
    {
        createCompleteProjection(new PolarStereographicA(),
                6371007,            // Semi-major axis length
                6371007,            // Semi-minor axis length
                0,                  // Central meridian
                latitudeOfOrigin,   // Latitude of origin
                NaN,                // Standard parallel 1 (none)
                NaN,                // Standard parallel 2 (none)
                0.994,              // Scale factor
                200,                // False easting
                100);               // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(domain, randomSeed);
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
        compareEllipticalWithSpherical(CoordinateDomain.GEOGRAPHIC_SOUTH_POLE, -90,  17326686);
        compareEllipticalWithSpherical(CoordinateDomain.GEOGRAPHIC_NORTH_POLE, +90, 970559366);
    }
}
