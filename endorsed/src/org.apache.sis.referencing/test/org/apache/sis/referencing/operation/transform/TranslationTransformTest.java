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
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.util.internal.shared.DoubleDouble;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.Assertions;


/**
 * Tests the {@link TranslationTransform} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class TranslationTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public TranslationTransformTest() {
    }

    /**
     * Sets the {@link #transform} field to the {@link TranslationTransform} instance to test.
     *
     * @param  dimensions  expected number of source and target dimensions.
     * @param  matrix      the data to use for creating the transform.
     */
    private void create(final int dimensions, final MatrixSIS matrix) {
        final Number[] elements = TranslationTransform.wrap(matrix.getElements());
        final TranslationTransform tr = new TranslationTransform(matrix.getNumRow(), elements);
        assertEquals(dimensions, tr.getSourceDimensions());
        assertEquals(dimensions, tr.getTargetDimensions());
        Assertions.assertMatrixEquals(matrix, tr, "matrix");
        assertArrayEquals(elements, getElementAsNumbers(tr));
        transform = tr;
        validate();
    }

    /**
     * Tests a transform created from a square matrix.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testConstantDimension() throws TransformException {
        create(3, new Matrix4(
                1, 0, 0, 2,
                0, 1, 0, 3,
                0, 0, 1, 8,
                0, 0, 0, 1));

        verifyTransform(new double[] {1,1,1,   6, 0,  2,   2, Double.NaN,  6},
                        new double[] {3,4,9,   8, 3, 10,   4, Double.NaN, 14});
    }

    /**
     * Verifies that {@link TranslationTransform} stores the numbers with their extended precision.
     */
    @Test
    public void testExtendedPrecision() {
        final Number O = 0;
        final Number l = 1;
        final DoubleDouble r = DoubleDouble.DEGREES_TO_RADIANS;
        final Number[] elements = {
            l, O, O, r,
            O, l, O, r,
            O, O, l, O,
            O, O, O, l
        };
        final MatrixSIS matrix = Matrices.create(4, 4, elements);
        final TranslationTransform tr = new TranslationTransform(4, elements);
        assertEquals(3, tr.getSourceDimensions());
        assertEquals(3, tr.getTargetDimensions());
        Assertions.assertMatrixEquals(matrix, tr, "matrix");

        replaceZeroByNull(elements, O);
        assertArrayEquals(elements, tr.getElementAsNumbers(false));
        transform = tr;
        validate();
    }

    /**
     * Replaces all occurrences of the specified zero value by {@code null}.
     * This is required before an array of expected elements can be compared
     * with {@link LinearTransform1D#getElementAsNumbers(boolean)}.
     */
    static void replaceZeroByNull(final Number[] elements, final Number zero) {
        for (int i=0; i<elements.length; i++) {
            if (elements[i] == zero) {
                elements[i] = null;
            }
        }
    }

    /**
     * Returns {@code tr.getElementAsNumbers(true)} will all instances
     * of {@link Integer} replaced by instances of {@link Double}.
     * This is required before to compare with expected values.
     */
    static Number[] getElementAsNumbers(final ExtendedPrecisionMatrix tr) {
        final Number[] elements = tr.getElementAsNumbers(true);
        for (int i=0; i<elements.length; i++) {
            final Number e = elements[i];
            if (e instanceof Integer) {
                elements[i] = Double.valueOf(e.intValue());
            }
        }
        return elements;
    }
}
