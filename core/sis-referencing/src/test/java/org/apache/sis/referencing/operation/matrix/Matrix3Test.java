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
import static org.apache.sis.referencing.operation.matrix.Matrix3.SIZE;


/**
 * Tests the {@link Matrix3} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(SolverTest.class)
public final strictfp class Matrix3Test extends MatrixTestCase {
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
        assertEquals(Matrix3.class, matrix.getClass());
    }

    /**
     * Tests the {@link Matrix3#Matrix3(double, double, double,
     * double, double, double, double, double, double)} constructor.
     * This constructor is specific to the implementation class.
     */
    @Test
    public void testConstructor() {
        initialize(-2078758443421995879L);
        final double[] elements = createRandomPositiveValues(SIZE * SIZE);
        final Matrix3 matrix = new Matrix3(
                elements[0],
                elements[1],
                elements[2],
                elements[3],
                elements[4],
                elements[5],
                elements[6],
                elements[7],
                elements[8]);
        validate(matrix);
        assertArrayEquals(elements, matrix.getElements(), STRICT);
    }

    /**
     * Verifies our claim that {@code A.solve(B)} is equivalent to {@code A.inverse().multiply(B)}.
     * This claim is documented in {@link MatrixSIS#solve(Matrix)} javadoc.
     *
     * @throws NoninvertibleMatrixException Should not happen.
     */
    @Test
    public void testSolveEquivalence() throws NoninvertibleMatrixException {
        final Matrix3 A = new Matrix3(
                0.5,  0,    0,
                0,    0.5,  0,
                0,    0,    1);

        final Matrix3 B = new Matrix3(
                0,  3,  0,
                3,  0,  0,
                0,  0,  1);

        // Verify the result of A.inverse().multiply(B) as a matter of principle.
        final double[] expected = A.inverse().multiply(B).getElements();
        assertArrayEquals(new double[] {
                0,  6,  0,
                6,  0,  0,
                0,  0,  1}, expected, TOLERANCE);

        // Now the actual test.
        assertEqualsElements(expected, SIZE, SIZE, A.solve(B), TOLERANCE);
    }

    /**
     * Tests {@link MatrixSIS#convertBefore(int, Number, Number)} using {@link AffineTranform}
     * as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertBefore() {
        testConvertBefore(new Matrix3(), true);
    }

    /**
     * Tests {@link MatrixSIS#convertAfter(int, Number, Number)} using {@link AffineTranform}
     * as a reference implementation.
     *
     * @since 0.6
     */
    @Test
    public void testConvertAfter() {
        testConvertAfter(new Matrix3());
    }
}
