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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;
import static org.junit.Assert.*;


/**
 * Tests {@link CoordinateFrameRotation} and {@link CoordinateFrameRotation3D}.
 * This test uses the same sample point than {@link PositionVector7ParamTest},
 * but with the rotation in the opposite direction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    PositionVector7ParamTest.class
})
public final strictfp class CoordinateFrameRotationTest extends MathTransformTestCase {
    /**
     * Creates the transformation from WGS 72 to WGS 84.
     */
    private void createTransform(final GeocentricAffine method) throws FactoryException {
        transform = PositionVector7ParamTest.createTransform(method, -1);
        validate();
    }

    /**
     * Tests <cite>"Coordinate Frame Rotation (geocentric domain)"</cite> (EPSG:1032).
     * with a sample point from WGS 72 to WGS 84 (EPSG Dataset transformation code 1238).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    public void testGeocentricDomain() throws FactoryException, TransformException {
        createTransform(new CoordinateFrameRotation());
        tolerance = 0.01;  // Precision for (X,Y,Z) values given by EPSG
        derivativeDeltas = new double[] {100, 100, 100};    // In metres
        assertTrue(transform instanceof LinearTransform);
        verifyTransform(PositionVector7ParamTest.samplePoint(2),
                        PositionVector7ParamTest.samplePoint(3));
    }

    /**
     * Tests <cite>"Coordinate Frame Rotation (geog3D domain)"</cite> (EPSG:1038).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeocentricDomain")
    public void testGeographicDomain() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};    // (Δλ, Δφ, Δh)
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        zTolerance = Formulas.LINEAR_TOLERANCE;
        zDimension = new int[] {2};
        tolerance  = Formulas.LINEAR_TOLERANCE;                 // Other SIS branches use a stricter threshold.
        createTransform(new CoordinateFrameRotation3D());
        assertFalse(transform instanceof LinearTransform);
        verifyTransform(PositionVector7ParamTest.samplePoint(1),
                        PositionVector7ParamTest.samplePoint(4));
    }
}
