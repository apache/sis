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

import static org.junit.Assert.*;


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
 * @version 0.4
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
     * TODO: inverse transform not yet implemented for non-square matrix.
     */
    @Override
    @org.junit.Ignore
    public void testInverse() throws NoninvertibleMatrixException {
    }

    /**
     * TODO: inverse transform not yet implemented for non-square matrix.
     */
    @Override
    @org.junit.Ignore
    public void testSolve() throws NoninvertibleMatrixException {
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
