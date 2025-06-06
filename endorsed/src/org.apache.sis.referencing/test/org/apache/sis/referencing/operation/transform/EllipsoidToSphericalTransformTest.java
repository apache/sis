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
package org.apache.sis.referencing.operation.transform;

import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.datum.HardCodedDatum;


/**
 * Tests {@link EllipsoidToCentricTransform} targeting a spherical coordinate system.
 * The expected results of all tests are still in Cartesian geocentric coordinates.
 * See {@link #targetType} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EllipsoidToSphericalTransformTest extends EllipsoidToCentricTransformTest {
    /**
     * Creates a new test case.
     */
    public EllipsoidToSphericalTransformTest() {
        targetType = EllipsoidToCentricTransform.TargetType.SPHERICAL;
        addSphericalToCartesian = true;
    }

    /**
     * Creates a spherical cases of the transformation.
     */
    private void createSpherical(final boolean withHeight) throws FactoryException {
        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres
        derivativeDeltas = new double[] {delta, delta};             // (Δλ, Δφ)
        tolerance = 1E-8;
        addSphericalToCartesian = false;
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), withHeight);
    }

    /**
     * Tests some conversions with three-dimensional input coordinates.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testTransform3D() throws FactoryException, TransformException {
        createSpherical(true);
        verifyTransform(new double[] {10,   0,     0}, new double[] {10,   0,                 6378137});
        verifyTransform(new double[] {10,   0, 10000}, new double[] {10,   0,                 6388137});
        verifyTransform(new double[] {10,  90,     0}, new double[] {10,  90,                 6356752.314245179});
        verifyTransform(new double[] {10,  90, 20000}, new double[] {10,  90,                 6376752.314245178});
        verifyTransform(new double[] {10, -90,     0}, new double[] {10, -90,                 6356752.314245179});
        verifyTransform(new double[] {10, -90, 10000}, new double[] {10, -90,                 6366752.314245179});
        verifyTransform(new double[] {10,  32,     0}, new double[] {10,  31.827305280575516, 6372168.063359014});
        verifyTransform(new double[] {10,  32, 20000}, new double[] {10,  31.82784561198946,  6392167.972795855});
        verifyConsistency(new float[] {
            15,  0,  10000,
            20, 90,  20000,
            30, 32, -10000
        });
    }

    /**
     * Tests some conversions with two-dimensional input coordinates.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testTransform2D() throws FactoryException, TransformException {
        createSpherical(false);
        verifyTransform(new double[] {10,   0}, new double[] {10,   0,                 6378137});
        verifyTransform(new double[] {10,  90}, new double[] {10,  90,                 6356752.314245179});
        verifyTransform(new double[] {10, -90}, new double[] {10, -90,                 6356752.314245179});
        verifyTransform(new double[] {10,  32}, new double[] {10,  31.827305280575516, 6372168.063359014});
        verifyConsistency(new float[] {
            15,  0,
            20, 90,
            30, 32
        });
    }

    /**
     * Tests derivative on spherical coordinates. The result should be identity matrices.
     */
    @Test
    @Override
    public void testDerivativeOnSphere() throws FactoryException, TransformException {
        addSphericalToCartesian = false;
        super.testDerivativeOnSphere();
    }

    /**
     * Tests derivative.
     */
    @Test
    @Override
    public void testDerivative() throws FactoryException, TransformException {
        addSphericalToCartesian = false;
        super.testDerivative();
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting for three-dimensional transforms.
     * The tests in this class haves an additional "Cartesian to spherical" step compared to the parent class.
     */
    @Test
    @Override
    public void testWKT3D() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
        assertWktEquals("CONCAT_MT[\n" +
                        "  PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Spherical to Cartesian”]]");

        transform = transform.inverse();
        assertWktEquals("CONCAT_MT[\n" +
                        "  PARAM_MT[“Cartesian to spherical”],\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]]]");
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting for two-dimensional transforms.
     * The tests in this class haves an additional "Cartesian to spherical" step compared to the parent class.
     */
    @Test
    @Override
    public void testWKT2D() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), false);
        assertWktEquals("CONCAT_MT[\n" +
                        "  INVERSE_MT[PARAM_MT[“Geographic3D to 2D conversion”]],\n" +
                        "  PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Spherical to Cartesian”]]");

        transform = transform.inverse();
        assertWktEquals("CONCAT_MT[\n" +
                        "  PARAM_MT[“Cartesian to spherical”],\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Geographic3D to 2D conversion”]]");
    }

    /**
     * Tests the internal Well Known Text formatting.
     * The tests in this class haves an additional "Cartesian to spherical" step compared to the parent class.
     * Furthermore, it has no conversion of degrees to radians for the longitude values.
     */
    @Test
    @Override
    public void testInternalWKT() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_1_1”, 0.017453292519943295],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Ellipsoid (radians domain) to centric”,\n" +
                "    Parameter[“eccentricity”, 0.0818191908426215],\n" +
                "    Parameter[“csType”, “SPHERICAL”],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6378137.0]],\n" +
                "  Concat_MT[\n" +
                "    Param_MT[“Affine”,\n" +
                "      Parameter[“num_row”, 4],\n" +
                "      Parameter[“num_col”, 4],\n" +
                "      Parameter[“elt_0_0”, 0.017453292519943295],\n" +
                "      Parameter[“elt_1_1”, 0.017453292519943295]],\n" +
                "    Param_MT[“Spherical to Cartesian”]]]");
    }
}
