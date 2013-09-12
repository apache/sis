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

import static org.junit.Assert.*;


/**
 * Tests the {@link GeneralMatrix} implementation with square matrices.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class GeneralMatrixTest extends MatrixTestCase {
    /**
     * Number of rows and columns, initialized by {@link #initialize(String, boolean)}.
     */
    private int size;

    /** {@inheritDoc} */ @Override int getNumRow() {return size;}
    /** {@inheritDoc} */ @Override int getNumCol() {return size;}

    /**
     * Chooses a random size for the square matrix.
     *
     * @param testMethod  The name of the method which need a random number generator.
     * @param needsRandom Ignored.
     */
    @Override
    void initialize(final String testMethod, final boolean needsRandom) {
        super.initialize(testMethod, true);
        size = 5 + random.nextInt(8); // Matrix sizes from 5 to 12 inclusive.
    }

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validate(final MatrixSIS matrix) {
        super.validate(matrix);
        assertEquals(GeneralMatrix.class, matrix.getClass());
    }
}
