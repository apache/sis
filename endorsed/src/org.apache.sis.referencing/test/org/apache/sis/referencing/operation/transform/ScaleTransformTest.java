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

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.util.internal.shared.DoubleDouble;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.Assertions;


/**
 * Tests the {@link ScaleTransform} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ScaleTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public ScaleTransformTest() {
    }

    /**
     * Sets the {@link #transform} field to the {@link ScaleTransform} instance to test.
     *
     * @param  sourceDimensions  expected number of source dimensions.
     * @param  targetDimensions  expected number of source dimensions.
     * @param  matrix            the data to use for creating the transform.
     */
    private void create(final int sourceDimensions, final int targetDimensions, final MatrixSIS matrix) {
        final Number[] elements = ScaleTransform.wrap(matrix.getElements());
        final ScaleTransform tr = new ScaleTransform(matrix.getNumRow(), matrix.getNumCol(), elements);
        assertEquals(sourceDimensions, tr.getSourceDimensions());
        assertEquals(targetDimensions, tr.getTargetDimensions());
        Assertions.assertMatrixEquals(matrix, tr, "matrix");
        assertArrayEquals(elements, TranslationTransformTest.getElementAsNumbers(tr));
        transform = tr;
        validate();
    }

    /**
     * Tests a transform created from a square matrix.
     * In this test, no dimension are dropped.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testConstantDimension() throws TransformException {
        create(3, 3, new Matrix4(
                2, 0, 0, 0,
                0, 3, 0, 0,
                0, 0, 8, 0,
                0, 0, 0, 1));

        verifyTransform(new double[] {1,1,1,   6, 0,  2,   2, Double.NaN,  6},
                        new double[] {2,3,8,  12, 0, 16,   4, Double.NaN, 48});
    }

    /**
     * Tests a transform with less output dimensions than input dimensions.
     * This transform drops the last dimension.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testDimensionReduction() throws TransformException {
        isInverseTransformSupported = false;                            // Because matrix is not square.
        create(3, 2, Matrices.create(3, 4, new double[] {
                2, 0, 0, 0,
                0, 3, 0, 0,
                0, 0, 0, 1}));

        verifyTransform(new double[] {1,1,1,   6, 0, 2,   2, Double.NaN, 6},
                        new double[] {2,3,    12, 0,      4, Double.NaN});
    }

    /**
     * Tests a transform with more output dimensions than input dimensions.
     * The extra dimension has values set to 0. This kind of transform happen
     * in the inverse of <q>Geographic 3D to 2D conversion</q> (EPSG:9659).
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testDimensionAugmentation() throws TransformException {
        transform = new ProjectiveTransform(Matrices.create(4, 3, new double[] {
                2, 0, 0,
                0, 3, 0,
                0, 0, 0,
                0, 0, 1}));

        assertInstanceOf(ScaleTransform.class, transform.inverse());
        verifyTransform(new double[] {1,1,    6,  0,     2, Double.NaN},
                        new double[] {2,3,0,  12, 0, 0,  4, Double.NaN, 0});
    }

    /**
     * Verifies that {@link ScaleTransform} stores the numbers with their extended precision.
     */
    @Test
    public void testExtendedPrecision() {
        final Number O = 0;
        final Number l = 1;
        final DoubleDouble r = DoubleDouble.DEGREES_TO_RADIANS;
        final Number[] elements = {
            r, O, O, O,
            O, r, O, O,
            O, O, l, O,
            O, O, O, l
        };
        final MatrixSIS matrix = Matrices.create(4, 4, elements);
        final ScaleTransform tr = new ScaleTransform(4, 4, elements);
        assertEquals(3, tr.getSourceDimensions());
        assertEquals(3, tr.getTargetDimensions());
        Assertions.assertMatrixEquals(matrix, tr, "matrix");

        TranslationTransformTest.replaceZeroByNull(elements, O);
        assertArrayEquals(elements, tr.getElementAsNumbers(false));
        transform = tr;
        validate();
    }
}
