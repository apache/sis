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

import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;


/**
 * Tests the {@link NonSquareMatrix} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(GeneralMatrixTest.class)
public final strictfp class NonSquareMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns, initialized by {@link #initialize(String, boolean)}.
     */
    private int numRow, numCol;

    /** {@inheritDoc} */ @Override int getNumRow() {return numRow;}
    /** {@inheritDoc} */ @Override int getNumCol() {return numCol;}

    /**
     * Chooses a random size for the matrix and ensure that the matrix is not square.
     *
     * @param testMethod  The name of the method which need a random number generator.
     * @param needsRandom Ignored.
     */
    @Override
    void initialize(final String testMethod, final boolean needsRandom) {
        super.initialize(testMethod, true);
        numRow = 5 + random.nextInt(8); // Matrix sizes from 5 to 12 inclusive.
        do numCol = 5 + random.nextInt(8);
        while (numCol == numRow);
    }

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validate(final MatrixSIS matrix) {
        super.validate(matrix);
        assertEquals(NonSquareMatrix.class, matrix.getClass());
    }
}
