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
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.datum.HardCodedDatum;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link EllipsoidToRadiusTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EllipsoidToRadiusTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public EllipsoidToRadiusTransformTest() {
    }

    /**
     * Convenience method for creating an instance from an ellipsoid.
     *
     * @param  ellipsoid   the semi-major and semi-minor axis lengths with their unit of measurement.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    final void createGeodeticConversion(final Ellipsoid ellipsoid) throws FactoryException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        transform = EllipsoidToRadiusTransform.createGeodeticConversion(factory, ellipsoid);
        if (ellipsoid.isSphere()) {
            var tr = new EllipsoidToRadiusTransform(ellipsoid);
            transform = new TransformResultComparator(transform, tr.context.completeTransform(factory, tr), 1E-2);
        }
    }

    /**
     * Tests some radius calculations.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testRadius() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid());
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres
        derivativeDeltas = new double[] {delta, delta};             // (Δλ, Δφ)
        tolerance = 1E-8;
        verifyTransform(new double[] {10,   0}, new double[] {10,   0, 6378137});
        verifyTransform(new double[] {10,  90}, new double[] {10,  90, 6356752.314245179});
        verifyTransform(new double[] {10, -90}, new double[] {10, -90, 6356752.314245179});
        verifyTransform(new double[] {10,  32}, new double[] {10,  32, 6372110.088381336});
        verifyConsistency(new float[] {
            15,  0,
            20, 90,
            30, 32
        });
    }

    /**
     * Tests some radius calculations on a sphere.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testOnSphere() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.SPHERE.getEllipsoid());
        // No call to `validate()` because of `TransformResultComparator`.

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres
        derivativeDeltas = new double[] {delta, delta};             // (Δλ, Δφ)
        tolerance = 1E-8;
        verifyTransform(new double[] {10,  0}, new double[] {10,  0, 6371007});
        verifyTransform(new double[] {10, 90}, new double[] {10, 90, 6371007});
        verifyConsistency(new float[] {
            15,  0,
            20, 90,
            30, 32
        });
    }

    /**
     * Tests a concatenation of transform that can be simplified by moving an offset of the longitude value.
     */
    @Test
    public void testPassThrough() {
        transform = new EllipsoidToRadiusTransform(HardCodedDatum.WGS84.getEllipsoid());
        transform = MathTransforms.concatenate(MathTransforms.scale(2, 3), transform, MathTransforms.translation(-5, 0, 0));
        final ConcatenatedTransform c = assertInstanceOf(ConcatenatedTransform.class, transform);
        final EllipsoidToRadiusTransform tr2 = assertInstanceOf(EllipsoidToRadiusTransform.class, c.transform2);
        assertEquals(0.006694379990141317, tr2.eccentricitySquared, 1E-17);
        final Matrix tr1 = MathTransforms.getMatrix(c.transform1);
        assertNotNull(tr1);
        assertMatrixEquals(new Matrix3(
                2, 0, -5,
                0, 3,  0,
                0, 0,  1), tr1, null, null);
    }

    /**
     * Tests serialization.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testSerialization() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid());
        transform = assertSerializedEquals(transform);
        verifyTransform(new double[] {10,  32}, new double[] {10, 32, 6372110.088381336});
    }

    /**
     * Tests the Well Known Text (version 1) formatting.
     * The result is what we show to users, but is quite different than what <abbr>SIS</abbr> has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testWKT() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid());
        assertWktEquals("PARAM_MT[“Spherical2D to 3D conversion”,\n" +
                        "  PARAMETER[“semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“semi_minor”, 6356752.314245179]]");

        transform = transform.inverse();
        assertWktEquals("PARAM_MT[“Spherical3D to 2D conversion”,\n" +
                        "  PARAMETER[“semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“semi_minor”, 6356752.314245179]]");
    }

    /**
     * Tests the internal Well Known Text formatting.
     * This WKT shows what SIS has in memory for debugging purpose.
     * This is normally not what we show to users.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testInternalWKT() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid());
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine parametric transformation”,\n" +
                "    Parameter[“B1”, 0.017453292519943295, Id[“EPSG”, 8640]]],\n" +
                "  Param_MT[“Spherical2D to 3D (radians domain)”,\n" +
                "    Parameter[“eccentricity”, 0.0818191908426215]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6356752.314245179]]]");
    }
}
