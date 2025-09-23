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
import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.Equirectangular;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.AUTHALIC_RADIUS;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.Assertions;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;


/**
 * Tests the affine transform created by the {@link Equirectangular} class. This map projection is a
 * special case since the transform is implemented by an affine transform instead of a class from
 * the {@link org.apache.sis.referencing.operation.projection} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EquirectangularTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public EquirectangularTest() {
    }

    /**
     * Initializes a simple Equirectangular projection on sphere. This method is different than the
     * {@code createNormalizedProjection(boolean)} method in all other test classes, because it does
     * not create an instance of {@link NormalizedProjection}. Instead, it creates an affine transform
     * for the whole projection (not only the normalized part).
     */
    private void createCompleteProjection() throws FactoryException {
        final var provider = new Equirectangular();
        final Parameters parameters = parameters(provider, false);
        transform = provider.createMathTransform(new MathTransformFactoryMock(provider), parameters);
        tolerance = Formulas.LINEAR_TOLERANCE;  // Not NORMALIZED_TOLERANCE since this is not a NormalizedProjection.
        validate();
    }

    /**
     * Tests the WKT formatting of an Equirectangular projection. While the projection is implemented by
     * an affine transform, the WKT formatter should handle this projection in a special way and shows the
     * projection parameters instead of the affine transform parameters (except in "show internal" mode).
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     */
    @Test
    public void testWKT() throws FactoryException {
        createCompleteProjection();
        assertWktEquals(
                "PARAM_MT[“Equirectangular”,\n" +
                "  PARAMETER[“semi_major”, 6371007.0],\n" +
                "  PARAMETER[“semi_minor”, 6371007.0]]");
        /*
         * MathTransforms are not defined in WKT 2, so the following WKTs do not exist in standards.
         * Since the semi-major and semi-minor parameters do not have EPSG codes, the ID[…] elements
         * below show whatever identifier come first for each parameter (currently GeoTIFF identifiers,
         * but the authority may change in any future SIS version).
         */
        Assertions.assertWktEquals(Convention.WKT2,
                "PARAM_MT[“Equidistant Cylindrical (Spherical)”,\n" +
                "  PARAMETER[“semi_major”, 6371007.0, LENGTHUNIT[“metre”, 1], ID[“GeoTIFF”, 2057]],\n" +
                "  PARAMETER[“semi_minor”, 6371007.0, LENGTHUNIT[“metre”, 1], ID[“GeoTIFF”, 2058]]]", transform);

        Assertions.assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "Param_MT[“Equidistant Cylindrical (Spherical)”,\n" +
                "  Parameter[“semi_major”, 6371007.0, Unit[“metre”, 1]],\n" +
                "  Parameter[“semi_minor”, 6371007.0, Unit[“metre”, 1]]]", transform);

        Assertions.assertWktEquals(Convention.INTERNAL,
                "Param_MT[“Affine parametric transformation”,\n" +
                "  Parameter[“A0”, 111195.04881760638, Id[“EPSG”, 8623]],\n" +
                "  Parameter[“B1”, 111195.04881760638, Id[“EPSG”, 8640]]]", transform);
    }

    /**
     * Tests a simple transform on a sphere.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSimpleTransform() throws FactoryException, TransformException {
        createCompleteProjection();
        verifyTransform(
                new double[] {          // (λ,φ) coordinates in degrees to project.
                    0, 0,
                    2, 0,
                    0, 3
                },
                new double[] {          // Expected (x,y) results in metres.
                    0,                            0,
                    AUTHALIC_RADIUS*toRadians(2), 0,
                    0, AUTHALIC_RADIUS*toRadians(3)
                });
    }

    /**
     * Tests conversion of random points. This test is actually of limited interest since the Equirectangular
     * projection is implemented by an affine transform, which has been tested elsewhere.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testRandomPoints() throws FactoryException, TransformException {
        createCompleteProjection(new Equirectangular(),
                WGS84_A,    // Semi-major axis length
                WGS84_B,    // Semi-minor axis length
                0.5,        // Central meridian
                0,          // Latitude of origin (none)
                20,         // Standard parallel 1
                NaN,        // Standard parallel 2
                NaN,        // Scale factor (none)
                200,        // False easting
                100);       // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;  // Not NORMALIZED_TOLERANCE since this is not a NormalizedProjection.
        derivativeDeltas = new double[] {100, 100};
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 0);
    }
}
