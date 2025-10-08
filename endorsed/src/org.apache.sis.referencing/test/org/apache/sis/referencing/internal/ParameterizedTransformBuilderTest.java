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
package org.apache.sis.referencing.internal;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertMatrixEquals;


/**
 * Tests the {@link ParameterizedTransformBuilder} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ParameterizedTransformBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ParameterizedTransformBuilderTest() {
    }

    /**
     * Tests {@link ParameterizedTransformBuilder#swapAndScaleAxes(MathTransform)}
     * with different number of dimensions.
     *
     * @throws FactoryException if the transform construction failed.
     */
    @Test
    public void testSwapAndScaleAxes() throws FactoryException {
        final var context = new ParameterizedTransformBuilder(DefaultMathTransformFactory.provider(), null);
        context.setSourceAxes(HardCodedCS.GEODETIC_3D,  null);
        context.setTargetAxes(HardCodedCS.CARTESIAN_3D, null);
        /*
         * Simulate a case where the parameterized transform is a two-dimensional map projection,
         * but the input and output CRS are three-dimensional geographic and projected CRS respectively.
         */
        MathTransform mt = context.swapAndScaleAxes(MathTransforms.identity(2));
        assertEquals(3, mt.getSourceDimensions());
        assertEquals(3, mt.getTargetDimensions());
        assertTrue(mt.isIdentity());
        /*
         * Transform from 3D to 2D. Height dimension is dropped.
         */
        context.setSourceAxes(HardCodedCS.GEODETIC_3D, null);
        context.setTargetAxes(HardCodedCS.GEODETIC_2D, null);
        mt = context.swapAndScaleAxes(MathTransforms.identity(2));
        assertMatrixEquals(
                Matrices.create(3, 4, new double[] {
                        1, 0, 0, 0,
                        0, 1, 0, 0,
                        0, 0, 0, 1}),
                mt, "3D → 2D");
        /*
         * Transform from 2D to 3D. Coordinate values in the height dimension are unknown (NaN).
         * This case happen when the third dimension is handled as a "pass through" dimension.
         */
        context.setSourceAxes(HardCodedCS.GEODETIC_2D, null);
        context.setTargetAxes(HardCodedCS.GEODETIC_3D, null);
        mt = context.swapAndScaleAxes(MathTransforms.identity(2));
        assertMatrixEquals(
                Matrices.create(4, 3, new double[] {
                        1, 0, 0,
                        0, 1, 0,
                        0, 0, Double.NaN,
                        0, 0, 1}),
                mt, "2D → 3D");
        /*
         * Same transform from 2D to 3D, but this time with the height consumed by the parameterized operation.
         * This is differentiated from the previous case by the fact that the parameterized operation is three-dimensional.
         */
        mt = context.swapAndScaleAxes(MathTransforms.identity(3));
        assertMatrixEquals(
                Matrices.create(4, 3, new double[] {
                        1, 0, 0,
                        0, 1, 0,
                        0, 0, 0,
                        0, 0, 1}),
                mt, "2D → 3D");
        /*
         * Test error message when adding a dimension that is not ellipsoidal height.
         */
        context.setSourceAxes(HardCodedCS.CARTESIAN_2D, null);
        context.setTargetAxes(HardCodedCS.CARTESIAN_3D, null);
        var e = assertThrows(InvalidGeodeticParameterException.class,
                () -> context.swapAndScaleAxes(MathTransforms.identity(2)),
                "Should not have accepted the given coordinate systems.");
        assertMessageContains(e, "2D → tr(2D → 2D) → 3D");
    }
}
