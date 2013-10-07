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
import static org.apache.sis.referencing.operation.matrix.Matrix4.SIZE;


/**
 * Tests the {@link Matrix4} implementation.
 * This class inherits all tests defined in {@link MatrixTestCase}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(SolverTest.class)
public final strictfp class Matrix4Test extends MatrixTestCase {
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
        assertEquals(Matrix4.class, matrix.getClass());
    }

    /**
     * Tests the {@link Matrix4#Matrix4(double, double, double, double, double, double, double,
     * double, double, double, double, double, double, double, double, double)} constructor.
     * This constructor is specific to the implementation class.
     */
    @Test
    public void testConstructor() {
        initialize(-7053945420932915425L);
        final double[] elements = createRandomPositiveValues(SIZE * SIZE);
        final Matrix4 matrix = new Matrix4(
                elements[ 0],
                elements[ 1],
                elements[ 2],
                elements[ 3],
                elements[ 4],
                elements[ 5],
                elements[ 6],
                elements[ 7],
                elements[ 8],
                elements[ 9],
                elements[10],
                elements[11],
                elements[12],
                elements[13],
                elements[14],
                elements[15]);
        validate(matrix);
        assertArrayEquals(elements, matrix.getElements(), STRICT);
    }

    /**
     * Tests the accuracy of a chain of matrix operations.
     *
     * @throws NoninvertibleMatrixException Should never happen.
     */
    @Test
    public void testAccuracy() throws NoninvertibleMatrixException {
        final double parisMeridian = 2 + (20 + 13.82/60)/60;  // Paris meridian: 2°20'13.82"
        final double toRadians = StrictMath.PI / 180;
        /*
         * Gradians to degrees with a Prime Meridian shift
         * and a random conversion factor for z values.
         */
        final Matrix4 step1 = new Matrix4(
                0.9,  0,    0,    parisMeridian,
                0,    0.9,  0,    0,
                0,    0,    0.8,  0, // Random conversion factor for z values.
                0,    0,    0,    1);
        /*
         * Degrees to radians with swapping of (longitude, latitude) axes
         * and a conversion factor of z values from feet to metres.
         */
        final Matrix4 step2 = new Matrix4(
                0, toRadians, 0, 0,
                toRadians, 0, 0, 0,
                0, 0, 0.3048, 0,
                0, 0, 0, 1);
        /*
         * Converse of the above operations.
         */
        final MatrixSIS step3 = step2.multiply(step1).inverse();
        /*
         * Concatenate everything, which should go back to the identity transform.
         * Note that the 'isIdentity()' test fail if the double-double arithmetic is
         * disabled, because some scale factors will be 0.9999999999999999 instead of 1.
         */
        final MatrixSIS result = step3.multiply(step2).multiply(step1);
        assertTrue("isIdentity()", result.isIdentity());
    }
}
