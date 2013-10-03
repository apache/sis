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
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.referencing.operation.matrix.Matrix2.SIZE;


/**
 * Tests the {@link Matrix2} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(SolverTest.class)
public final strictfp class Matrix2Test extends MatrixTestCase {
    /**
     * Returns the size of the matrix of interest for this test class.
     */
    @Override int getNumRow() {return SIZE;}
    @Override int getNumCol() {return SIZE;}

    /**
     * Ensures that the given matrix is an instance of the expected type.
     */
    @Override
    void validate(final MatrixSIS matrix) {
        super.validate(matrix);
        assertEquals(Matrix2.class, matrix.getClass());
    }

    /**
     * Tests the {@link Matrix2#Matrix2(double, double, double, double)} constructor.
     * This constructor is specific to the implementation class.
     */
    @Test
    public void testConstructor() {
        initialize(-8453835559080304420L);
        final double[] elements = createRandomPositiveValues(SIZE * SIZE);
        final Matrix2 matrix = new Matrix2(
                elements[0],
                elements[1],
                elements[2],
                elements[3]);
        validate(matrix);
        assertArrayEquals(elements, matrix.getElements(), STRICT);
    }

    /**
     * Tests {@link GeneralMatrix#getExtendedElements(Matrix, int, int, boolean)}.
     * This test is a copy of {@link GeneralMatrixTest#testGetExtendedElements()},
     * but on a {@link Matrix2} instance instead of {@link GeneralMatrix}.
     */
    @Test
    public void testGetExtendedElements() {
        GeneralMatrixTest.testGetExtendedElements(new Matrix2(
                StrictMath.PI / 180, // Degrees to radians
                180 / StrictMath.PI, // Radians to degrees
                0.9,                 // Gradians to degrees
                0.1234567));         // Random value with no special meaning.
    }
}
