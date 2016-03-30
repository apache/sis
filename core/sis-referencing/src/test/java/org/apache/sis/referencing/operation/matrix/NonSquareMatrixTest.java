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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.AfterClass;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link NonSquareMatrix} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * <p>This class is expected to be the last {@code MatrixTestCase} subclass to be executed,
 * because it sends the {@link #statistics} to {@link #out}. This condition is ensured if
 * the tests are executed by {@link org.apache.sis.test.suite.ReferencingTestSuite}.
 * However it is not a big deal if this condition is broken, as the only consequence
 * is that reported statistics will be incomplete.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn(SolverTest.class)
public final strictfp class NonSquareMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns, initialized by {@link #initialize(String, boolean)}.
     */
    private int numRow, numCol;

    /**
     * Computes a random size for the next matrix to create.
     *
     * @param random The random number generator to use.
     */
    @Override
    void prepareNewMatrixSize(final Random random) {
        numRow = 5 + random.nextInt(8); // Matrix sizes from 5 to 12 inclusive.
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
    void validate(final MatrixSIS matrix) {
        super.validate(matrix);
        assertEquals(NonSquareMatrix.class, matrix.getClass());
    }

    /**
     * Tests {@link NonSquareMatrix#inverse()} with a non-square matrix.
     *
     * @throws NoninvertibleMatrixException if the matrix can not be inverted.
     */
    @Test
    @Override
    public void testInverse() throws NoninvertibleMatrixException {
        testDimensionReduction(null, 1, 0);
        testDimensionIncrease (null, 1);
    }

    /**
     * Tests inversion of a matrix with a column containing only a translation term.
     * The purpose is to test the algorithm that selects the rows to omit.
     *
     * @throws NoninvertibleMatrixException if the matrix can not be inverted.
     */
    @Test
    public void testInverseWithTranslationTerm() throws NoninvertibleMatrixException {
        final NonSquareMatrix m = new NonSquareMatrix(5, 3, new double[] {
            2, 0, 0,
            0, 0, 0,
            0, 4, 0,
            0, 0, 3,
            0, 0, 1
        });
        MatrixSIS inverse = m.inverse();
        assertMatrixEquals("Inverse of non-square matrix.", new NonSquareMatrix(3, 5, new double[] {
            0.5, 0,   0,    0,   0,
            0,   0,   0.25, 0,   0,
            0,   0,   0,    0,   1}), inverse, STRICT);

        assertMatrixEquals("Back to original.", new NonSquareMatrix(5, 3, new double[] {
            2, 0, 0,
            0, 0, NaN,
            0, 4, 0,
            0, 0, NaN,
            0, 0, 1}), inverse.inverse(), STRICT);
        /*
         * Change the [0 0 3] row into [1 0 3]. The NonSquareMarix class should no longer omit that row.
         * As a consequence, the matrix can not be inverted anymore.
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
     * @throws NoninvertibleMatrixException if the matrix can not be inverted.
     */
    @Test
    @Override
    public void testSolve() throws NoninvertibleMatrixException {
        testDimensionReduction(new Matrix3(
                2, 0, 0,
                0, 2, 0,
                0, 0, 1), 2, NaN);
        testDimensionIncrease(new GeneralMatrix(5, 5, new double[] {
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
     * @param  Y    The matrix to give to {@code solve(Y)}, {@code null} for testing {@code inverse()}.
     * @param  sf   The scale factor by which to multiply all expected scale elements.
     * @param  uks  Value of unknown scales (O for {@code inverse()}, or NaN for {@code solve(Y)}).
     * @throws NoninvertibleMatrixException if the matrix can not be inverted.
     */
    private static void testDimensionReduction(final MatrixSIS Y, final double sf, final double uks)
            throws NoninvertibleMatrixException
    {
        final MatrixSIS matrix = Matrices.create(3, 5, new double[] {
            2, 0, 0, 0, 8,
            0, 0, 4, 0, 5,
            0, 0, 0, 0, 1
        });
        final double[] expected = {
            0.5*sf,  0,           -4,
            uks,     uks,        NaN,
            0,       0.25*sf,  -1.25,
            uks,     uks,        NaN,
            0,       0,            1
        };
        final MatrixSIS inverse = (Y != null) ? matrix.solve(Y) : matrix.inverse();
        assertEqualsElements(expected, 5, 3, inverse, TOLERANCE);
    }

    /**
     * Tests {@link NonSquareMatrix#inverse()} or {@link NonSquareMatrix#solve(Matrix)} with a conversion
     * matrix having more target dimensions (rows) than source dimensions (columns).
     *
     * @param  Y    The matrix to give to {@code solve(Y)}, {@code null} for testing {@code inverse()}.
     * @param  sf   The scale factor by which to multiply all expected scale elements.
     * @throws NoninvertibleMatrixException if the matrix can not be inverted.
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
     * Those statistics will be visible only if {@link #verbose} is {@code true}.
     */
    @AfterClass
    public static void printStatistics() {
        if (statistics != null) {
            TestUtilities.printSeparator("Overall statistics on agreement of matrix arithmetic");
            synchronized (statistics) {
                out.println(statistics);
            }
            TestUtilities.forceFlushOutput();
        }
    }
}
