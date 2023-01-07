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
import org.apache.sis.internal.util.DoubleDouble;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link GeneralMatrix} implementation with square matrices.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.4
 */
public final class GeneralMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns.
     */
    private int size;

    /**
     * Computes a random size for the next matrix to create.
     *
     * @param  random  the random number generator to use.
     */
    @Override
    void prepareNewMatrixSize(final Random random) {
        size = 5 + random.nextInt(8);                   // Matrix sizes from 5 to 12 inclusive.
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
     * Tests {@link GeneralMatrix#getNumber(int, int)} and {@link GeneralMatrix#getInteger(int, int)}
     * using a value which cannot be stored accurately in a {@code double} type.
     */
    @Test
    public void testExtendedPrecision() {
        final long value = 1000000000000010000L;
        assertNotEquals(value, StrictMath.round((double) value));       // Otherwise the test would be useless.
        final GeneralMatrix m = new GeneralMatrix(1, 1, false);
        final DoubleDouble ddval = DoubleDouble.of(value);
        m.setNumber(0, 0, ddval);
        assertEquals(value, ddval.longValue());
        assertEquals(ddval, m.getNumber (0, 0));
        assertEquals(value, m.getInteger(0, 0));
    }

    /**
     * Tests {@link GeneralMatrix#getElementAsNumbers(boolean)}.
     * This test verifies that the extra precision is preserved.
     */
    @Test
    public void testGetElementAsNumbers() {
        final Number[] numbers = {
                DoubleDouble.DEGREES_TO_RADIANS,
                DoubleDouble.RADIANS_TO_DEGREES,
                0,
                0.1234567   // Random value with no special meaning.
        };
        GeneralMatrix matrix = new GeneralMatrix(2, 2, numbers);
        final Number[] elements = matrix.getElementAsNumbers(true);
        assertNotSame("Shall be a copy.", numbers, elements);
        /*
         * The constructor shall have replaced 0 by null value.
         */
        assertEquals(0, numbers[2]);
        numbers[2] = null;
        assertArrayEquals(numbers, elements);
    }

    /**
     * Tests {@link MatrixSIS#convertBefore(int, Number, Number)}
     * using {@link java.awt.geom.AffineTransform} as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertBefore() {
        testConvertBefore(new GeneralMatrix(3, 3, true), true);
    }

    /**
     * Tests {@link MatrixSIS#convertAfter(int, Number, Number)}
     * using {@link java.awt.geom.AffineTransform} as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertAfter() {
        testConvertAfter(new GeneralMatrix(3, 3, true));
    }

    /**
     * Tests {@link MatrixSIS#multiply(double[])}
     * using {@link java.awt.geom.AffineTransform} as a reference implementation.
     *
     * @since 0.8
     */
    @Test
    public void testMultiplyVector() {
        testMultiplyVector(new GeneralMatrix(3, 3, true));
    }

    /**
     * Tests {@link MatrixSIS#translate(double[])}
     * using {@link java.awt.geom.AffineTransform} as a reference implementation.
     *
     * @since 1.0
     */
    @Test
    public void testTranslateVector() {
        testTranslateVector(new GeneralMatrix(3, 3, true));
    }
}
