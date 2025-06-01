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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link EllipsoidToCentricTransform} targeting a spherical coordinate system.
 * The expected results of all tests are still in Cartesian geocentric coordinates.
 * See {@link #targetType} for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
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
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting for two-dimensional transforms.
     * The tests in this class haves an additional "Cartesian to spherical" step compared to the parent class.
     */
    @Test
    @Override
    public void testWKT2D() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), false);
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the internal Well Known Text formatting.
     * The tests in this class haves an additional "Cartesian to spherical" step compared to the parent class.
     * Furthermore, it has no conversion of degrees to radians for the longitude values.
     */
    @Test
    @Override
    public void testInternalWKT() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_1_1”, 0.017453292519943295],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Ellipsoid (radians domain) to centric”,\n" +
                "    Parameter[“eccentricity”, 0.08181919084262157],\n" +
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

        loggings.assertNoUnexpectedLog();
    }
}
