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
import static java.lang.Double.NaN;

// Test dependencies
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link NonSquareMatrix} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * <h2>Test order</h2>
 * This class is expected to be the last {@code MatrixTestCase} subclass to be executed,
 * because it sends the {@link #statistics} to {@link #out}. However, it is okay if this
 * condition is broken, as the only consequence is that reported statistics will be incomplete.
 *
 * @todo Use JUnit 5 ordering mechanism for running other matrix tests first.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NonSquareMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns, initialized by {@link #prepareNewMatrixSize(Random)}.
     */
    private int numRow, numCol;

    /**
     * Creates a new test case.
     */
    public NonSquareMatrixTest() {
    }

    /**
     * Computes a random size for the next matrix to create.
     *
     * @param  random  the random number generator to use.
     */
    @Override
    void prepareNewMatrixSize(final Random random) {
        numRow = 5 + random.nextInt(8);                 // Matrix sizes from 5 to 12 inclusive.
        int n;
        do n = 5 + random.nextInt(8);
        while (n == numRow);
        numCol = n;
    }

    /** {@inheritDoc} */ @Override int getNumRow() {return numRow;}
    /** {@inheritDoc} */ @Override int getNumCol() {return numCol;}

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validateImplementation(final MatrixSIS matrix) {
        super.validateImplementation(matrix);
        assertEquals(NonSquareMatrix.class, matrix.getClass());
    }

    /**
     * Tests {@link NonSquareMatrix#inverse()} with a non-square matrix.
     *
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    @Test
    @Override
    public void testInverse() throws NoninvertibleMatrixException {
        testDimensionReduction(null, 1);
        testDimensionIncrease (null, 1);
    }

    /**
     * Tests inversion of a matrix with a column containing only a translation term.
     * The purpose is to test the algorithm that selects the rows to omit.
     *
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    @Test
    public void testInverseWithTranslationTerm() throws NoninvertibleMatrixException {
        final NonSquareMatrix m = new NonSquareMatrix(5, 3, new Number[] {
            2, 0, 0,
            0, 0, 0,
            0, 4, 0,
            0, 0, 3,
            0, 0, 1
        });
        MatrixSIS inverse = m.inverse();
        assertMatrixEquals(
                new NonSquareMatrix(3, 5, new Number[] {
                        0.5, 0,   0,    0,   0,
                        0,   0,   0.25, 0,   0,
                        0,   0,   0,    0,   1}),
                inverse,
                "Inverse of non-square matrix.");

        assertMatrixEquals(
                new NonSquareMatrix(5, 3, new Number[] {
                        2, 0, 0,
                        0, 0, NaN,
                        0, 4, 0,
                        0, 0, NaN,
                        0, 0, 1}),
                inverse.inverse(),
                "Back to original.");
        /*
         * Change the [0 0 3] row into [1 0 3]. The NonSquareMarix class should no longer omit that row.
         * As a consequence, the matrix cannot be inverted anymore.
         */
        m.setElement(3, 0, 1);
        try {
            m.inverse();
            fail("Matrix should not be invertible.");
        } catch (NoninvertibleMatrixException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@link NonSquareMatrix#solve(Matrix)} with a non-square matrix.
     *
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    @Test
    @Override
    public void testSolve() throws NoninvertibleMatrixException {
        testDimensionReduction(new Matrix3(
                2, 0, 0,
                0, 2, 0,
                0, 0, 1), 2);
        testDimensionIncrease(new GeneralMatrix(5, 5, new Number[] {
                2, 0, 0, 0, 0,
                0, 2, 0, 0, 0,
                0, 0, 2, 0, 0,
                0, 0, 0, 2, 0,
                0, 0, 0, 0, 1}), 2);
    }

    /**
     * Tests {@link NonSquareMatrix#inverse()} or {@link NonSquareMatrix#solve(Matrix)} with a conversion
     * matrix having more source dimensions (columns) than target dimensions (rows).
     *
     * @param  Y   the matrix to give to {@code solve(Y)}, {@code null} for testing {@code inverse()}.
     * @param  sf  the scale factor by which to multiply all expected scale elements.
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    private static void testDimensionReduction(final MatrixSIS Y, final double sf) throws NoninvertibleMatrixException {
        final MatrixSIS matrix = Matrices.create(3, 5, new double[] {
            2, 0, 0, 0, 8,
            0, 0, 4, 0, 5,
            0, 0, 0, 0, 1
        });
        final double[] expected = {
            0.5*sf,  0,           -4,
            0,       0,          NaN,
            0,       0.25*sf,  -1.25,
            0,       0,          NaN,
            0,       0,            1
        };
        final MatrixSIS inverse = (Y != null) ? matrix.solve(Y) : matrix.inverse();
        assertEqualsElements(expected, 5, 3, inverse, TOLERANCE);
    }

    /**
     * Tests {@link NonSquareMatrix#inverse()} or {@link NonSquareMatrix#solve(Matrix)} with a conversion
     * matrix having more target dimensions (rows) than source dimensions (columns).
     *
     * @param  Y   the matrix to give to {@code solve(Y)}, {@code null} for testing {@code inverse()}.
     * @param  sf  the scale factor by which to multiply all expected scale elements.
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    private static void testDimensionIncrease(final MatrixSIS Y, final double sf)
            throws NoninvertibleMatrixException
    {
        final MatrixSIS matrix = Matrices.create(5, 3, new double[] {
              2,   0,   8,
            NaN, NaN, NaN,
              0,   4,   5,
              0,   0,   0,
              0,   0,   1
        });
        final double[] expected = {
            0.5*sf,  0,  0,        0,  -4,
            0,       0,  0.25*sf,  0,  -1.25,
            0,       0,  0,        0,   1
        };
        final MatrixSIS inverse = (Y != null) ? matrix.solve(Y) : matrix.inverse();
        assertEqualsElements(expected, 3, 5, inverse, TOLERANCE);
    }

    /**
     * Prints the statistics about the differences between JAMA and SIS matrix elements.
     * Those statistics will be visible only if {@link #verbose()} returns {@code true}.
     */
    @AfterAll
    public static void printStatistics() {
        if (statistics != null) {
            out.printSeparator("Overall statistics on agreement of matrix arithmetic");
            synchronized (statistics) {
                out.println(statistics);
            }
            out.flushUnconditionally();
        }
    }
}
