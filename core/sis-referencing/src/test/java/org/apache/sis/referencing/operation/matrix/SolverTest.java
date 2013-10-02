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
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;


/**
 * Tests the {@link Solver} class using <a href="http://math.nist.gov/javanumerics/jama">JAMA</a>
 * as the reference implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class SolverTest extends TestCase {
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
        final Random random = TestUtilities.createRandomNumberGenerator();
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
            final MatrixSIS U = Solver.solve(matrix, matrixArg, matrixArg.getNumRow(), matrixArg.getNumCol());
            MatrixTestCase.assertMatrixEquals(jama, U, MatrixTestCase.TOLERANCE);
        }
    }
}
