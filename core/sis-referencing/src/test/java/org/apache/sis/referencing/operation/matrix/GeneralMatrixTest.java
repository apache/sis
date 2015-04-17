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
package org.apache.sis.referencing.operation.matrix;

import java.util.Random;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link GeneralMatrix} implementation with square matrices.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public final strictfp class GeneralMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns.
     */
    private int size;

    /**
     * Computes a random size for the next matrix to create.
     *
     * @param random The random number generator to use.
     */
    @Override
    void prepareNewMatrixSize(final Random random) {
        size = 5 + random.nextInt(8); // Matrix sizes from 5 to 12 inclusive.
    }

    /** {@inheritDoc} */ @Override int getNumRow() {return size;}
    /** {@inheritDoc} */ @Override int getNumCol() {return size;}

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validate(final MatrixSIS matrix) {
        super.validate(matrix);
        assertEquals(GeneralMatrix.class, matrix.getClass());
    }

    /**
     * Tests {@link GeneralMatrix#getExtendedElements(Matrix, int, int, boolean)}.
     * This test verifies that {@code getExtendedElements} can infer default error
     * terms for some well known values.
     *
     * @see Matrix2Test#testGetExtendedElements()
     */
    @Test
    public void testGetExtendedElements() {
        testGetExtendedElements(new GeneralMatrix(2, 2, new double[] {
                StrictMath.PI / 180, // Degrees to radians
                180 / StrictMath.PI, // Radians to degrees
                0.9,                 // Gradians to degrees
                0.1234567}));        // Random value with no special meaning.
    }

    /**
     * Implementation of {@link #testGetExtendedElements()} shared by {@link Matrix2Test}.
     */
    static void testGetExtendedElements(final MatrixSIS matrix) {
        final double[] elements = GeneralMatrix.getExtendedElements(matrix, Matrix2.SIZE, Matrix2.SIZE, false);
        assertArrayEquals(new double[] {
                // Same values than in the above matrix.
                StrictMath.PI / 180,
                180 / StrictMath.PI,
                0.9,
                0.1234567,

                // Most values below this point are error terms copied from DoubleDouble.ERRORS.
                 2.9486522708701687E-19,
                -1.9878495670576283E-15,
                -2.2204460492503132E-17,
                -2.5483615218035994E-18}, elements, STRICT);
    }

    /**
     * Tests {@link MatrixSIS#convertBefore(int, Number, Number)} using {@link AffineTranform}
     * as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertBefore() {
        testConvertBefore(new GeneralMatrix(3, 3, true, 1), true);    // Double precision
        testConvertBefore(new GeneralMatrix(3, 3, true, 2), true);    // Double-double precision
    }

    /**
     * Tests {@link MatrixSIS#convertAfter(int, Number, Number)} using {@link AffineTranform}
     * as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertAfter() {
        testConvertAfter(new GeneralMatrix(3, 3, true, 1));    // Double precision
        testConvertAfter(new GeneralMatrix(3, 3, true, 2));    // Double-double precision
    }
}
