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
import Jama.Matrix;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.apache.sis.referencing.operation.matrix.MatrixTestCase.assertEqualsJAMA;
import static org.apache.sis.referencing.operation.matrix.MatrixTestCase.assertEqualsElements;


/**
 * Tests the {@link Solver} class using <a href="http://math.nist.gov/javanumerics/jama">JAMA</a>
 * as the reference implementation.
 *
 * <div class="section">Cyclic dependency</div>
 * There is a cyclic test dependency since {@link GeneralMatrix} needs {@link Solver} for some operations,
 * and conversely. To be more specific the dependency order is:
 *
 * <ol>
 *   <li>Simple {@link GeneralMatrix} methods (construction, get/set elements)</li>
 *   <li>{@link Solver}</li>
 *   <li>More complex {@code GeneralMatrix} methods (matrix inversion, solve)</li>
 * </ol>
 *
 * We test {@code GeneralMatrix} before {@code Solver} since nothing could be done without
 * the above-cited simple operations anyway.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(GeneralMatrixTest.class) // See class javadoc
public final strictfp class SolverTest extends TestCase {
    /**
     * The tolerance threshold for this test case, which is {@value}. This value needs to be higher then the
     * {@link MatrixTestCase#TOLERANCE} one because of the increased complexity of {@link Solver} operations.
     *
     * @see MatrixTestCase#TOLERANCE
     * @see NonSquareMatrixTest#printStatistics()
     */
    protected static final double TOLERANCE = 100 * MatrixTestCase.TOLERANCE;

    /**
     * The matrix to test.
     */
    private MatrixSIS matrix;

    /**
     * A matrix to use as the reference implementation.
     * Contains the same value than {@link #matrix}.
     */
    private Matrix reference;

    /**
     * Initializes the {@link #matrix} and {@link #reference} matrices to random values.
     */
    private void createMatrices(final int numRow, final int numCol, final Random random) {
        matrix = new GeneralMatrix(numRow, numCol, false, 1);
        reference = new Matrix(numRow, numCol);
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double e = random.nextDouble() * 1000;
                matrix.setElement(j, i, e);
                reference.set(j, i, e);
            }
        }
    }

    /**
     * Tests the {@code Solver.solve(MatrixSIS, Matrix, int)} method.
     *
     * @throws NoninvertibleMatrixException Should never happen.
     */
    @Test
    public void testSolve() throws NoninvertibleMatrixException {
        final Random random;
        if (MatrixTestCase.DETERMINIST) {
            random = new Random(7671901444622173417L);
        } else {
            random = TestUtilities.createRandomNumberGenerator();
        }
        for (int k=0; k<MatrixTestCase.NUMBER_OF_REPETITIONS; k++) {
            final int size = random.nextInt(16) + 1;
            createMatrices(size, random.nextInt(16) + 1, random);
            final Matrix referenceArg = this.reference;
            final MatrixSIS matrixArg = this.matrix;
            createMatrices(size, size, random);
            final Matrix jama;
            try {
                jama = reference.solve(referenceArg);
            } catch (RuntimeException e) {
                out.println(e); // "Matrix is singular."
                continue;
            }
            final MatrixSIS U = Solver.solve(matrix, matrixArg);
            assertEqualsJAMA(jama, U, TOLERANCE);
        }
    }

    /**
     * Tests {@link Solver#inverse(MatrixSIS, boolean)} with a square matrix that contains a {@link Double#NaN} value.
     *
     * @throws NoninvertibleMatrixException Should not happen.
     */
    @Test
    @DependsOnMethod("testSolve")
    public void testInverseWithNaN() throws NoninvertibleMatrixException {
        /*
         * Just for making sure that our matrix is correct.
         */
        matrix = Matrices.create(5, 5, new double[] {
            20,  0,   0,   0, -3000,
            0, -20,   0,   0,  4000,
            0,   0,   0,   2,    20,
            0,   0, 400,   0,  2000,
            0,   0,   0,   0,     1
        });
        double[] expected = {
            0.05,  0,  0,      0,  150,
            0, -0.05,  0,      0,  200,
            0,     0,  0, 0.0025,   -5,
            0,     0,  0.5,    0,  -10,
            0,     0,  0,      0,    1
        };
        MatrixSIS inverse = Solver.inverse(matrix, false);
        assertEqualsElements(expected, 5, 5, inverse, TOLERANCE);
        /*
         * Set a scale factor to NaN. The translation term for the corresponding
         * dimension become unknown, so it most become NaN in the inverse matrix.
         */
        matrix = Matrices.create(5, 5, new double[] {
            20,  0,   0,   0, -3000,
            0, -20,   0,   0,  4000,
            0,   0,   0, NaN,    20,  // Translation is 20: can not be converted.
            0,   0, 400,   0,  2000,
            0,   0,   0,   0,     1
        });
        expected = new double[] {
            0.05,  0,  0,      0,  150,
            0, -0.05,  0,      0,  200,
            0,     0,  0, 0.0025,   -5,
            0,     0,  NaN,    0,  NaN,
            0,     0,  0,      0,    1
        };
        inverse = Solver.inverse(matrix, false);
        assertEqualsElements(expected, 5, 5, inverse, TOLERANCE);
        /*
         * Set a scale factor to NaN with translation equals to 0.
         * The zero value should be preserved, since 0 × any == 0
         * (ignoring infinities).
         */
        matrix = Matrices.create(5, 5, new double[] {
            20,  0,   0,   0, -3000,
            0, -20,   0,   0,  4000,
            0,   0,   0, NaN,     0,  // Translation is 0: should be preserved.
            0,   0, 400,   0,  2000,
            0,   0,   0,   0,     1
        });
        expected = new double[] {
            0.05,  0,  0,      0,  150,
            0, -0.05,  0,      0,  200,
            0,     0,  0, 0.0025,   -5,
            0,     0,  NaN,    0,    0,
            0,     0,  0,      0,    1
        };
        inverse = Solver.inverse(matrix, false);
        assertEqualsElements(expected, 5, 5, inverse, TOLERANCE);
        /*
         * Set a translation term to NaN. The translation should be NaN in
         * the inverse matrix too, but the scale factor can still be compute.
         */
        matrix = Matrices.create(5, 5, new double[] {
            20,  0,   0,   0, -3000,
            0, -20,   0,   0,  4000,
            0,   0,   0,   2,   NaN,
            0,   0, 400,   0,  2000,
            0,   0,   0,   0,     1
        });
        expected = new double[] {
            0.05,  0,  0,      0,  150,
            0, -0.05,  0,      0,  200,
            0,     0,  0, 0.0025,   -5,
            0,     0,  0.5,    0,  NaN,
            0,     0,  0,      0,    1
        };
        inverse = Solver.inverse(matrix, false);
        assertEqualsElements(expected, 5, 5, inverse, TOLERANCE);
    }
}
