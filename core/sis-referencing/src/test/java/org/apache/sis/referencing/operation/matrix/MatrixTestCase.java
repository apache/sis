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
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Base classes of tests for {@link MatrixSIS} implementations.
 * This class uses the following {@code Matrices} factory methods:
 *
 * <ul>
 *   <li>{@link Matrices#create(int, int)} (sometime delegates to {@link Matrices#createIdentity(int)})</li>
 *   <li>{@link Matrices#create(int, int, double[])}</li>
 *   <li>{@link Matrices#createZero(int, int)}</li>
 * </ul>
 *
 * So this class is indirectly a test of those factory methods.
 * However this class does not test any other {@code Matrices} methods.
 *
 * <p>This class uses <a href="http://math.nist.gov/javanumerics/jama">JAMA</a> as the reference implementation.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public abstract strictfp class MatrixTestCase extends TestCase {
    /**
     * A constant for any test in this class or a subclass which expect
     * a floating point value to be strictly equals to an other value.
     */
    static final double STRICT = 0;

    /**
     * Random number generator, created by {@link #initialize(String, boolean)} when first needed.
     */
    final Random random;

    /**
     * For subclasses only.
     */
    MatrixTestCase() {
        random = TestUtilities.createRandomNumberGenerator();
    }

    /** Returns the number of rows of the matrix being tested.    */ abstract int getNumRow();
    /** Returns the number of columns of the matrix being tested. */ abstract int getNumCol();

    /**
     * Validates the given matrix.
     * The default implementation verifies only the matrix size. Subclasses should override this method
     * for additional checks, typically ensuring that it is an instance of the expected class.
     */
    void validate(final MatrixSIS matrix) {
        assertEquals("numRow", getNumRow(), matrix.getNumRow());
        assertEquals("numCol", getNumCol(), matrix.getNumCol());
    }

    /**
     * Verifies that the SIS matrix is equals to the JAMA one, up to the given tolerance value.
     */
    private static void assertMatrixEquals(final Matrix expected, final MatrixSIS actual, final double tolerance) {
        final int numRow = actual.getNumRow();
        final int numCol = actual.getNumCol();
        assertEquals("numRow", expected.getRowDimension(),    numRow);
        assertEquals("numCol", expected.getColumnDimension(), numCol);
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                assertEquals(expected.get(j,i), actual.getElement(j,i), tolerance);
            }
        }
    }

    /**
     * Creates an array of the given length filled with random values.
     * This is a convenience method for the {@link testConstructor()} methods in {@code Matrix1…4Test} subclasses.
     */
    final double[] createRandomElements(final int length) {
        final double[] elements = new double[length];
        for (int k=0; k<length; k++) {
            elements[k] = random.nextDouble() * 100;
        }
        return elements;
    }

    /**
     * Creates a matrix initialized with a random array of element values,
     * then tests the {@link MatrixSIS#getElement(int, int)} method for each element.
     * This test will use {@link Matrices#create(int, int, double[])} for creating the matrix.
     *
     * <p>If this test fails, then all other tests in this class will be skipped since it would
     * not be possible to verify the result of any matrix operation.</p>
     */
    @Test
    public void testGetElements() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = createRandomElements(numRow * numCol);
        final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
        validate(matrix);
        /*
         * The JAMA constructor uses column-major array (FORTRAN convention), while SIS uses
         * row-major array. So we have to transpose the JAMA matrix after construction.
         */
        assertMatrixEquals(new Matrix(elements, numCol).transpose(), matrix, STRICT);
        assertArrayEquals("getElements", elements, matrix.getElements(), STRICT);
    }

    /**
     * Tests {@link MatrixSIS#getElement(int, int)} and {@link MatrixSIS#setElement(int, int, double)}.
     * This test sets random values in elements at random index, and compares with a JAMA matrix taken
     * as the reference implementation.
     */
    @Test
    @DependsOnMethod("testGetElements")
    public void testSetElement() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.createZero(numRow, numCol);
        validate(matrix);
        final Matrix reference = new Matrix(numRow, numCol);
        /*
         * End of initialization - now perform the actual test.
         */
        assertMatrixEquals(reference, matrix, STRICT);
        for (int k=0; k<50; k++) {
            final int    j = random.nextInt(numRow);
            final int    i = random.nextInt(numCol);
            final double e = random.nextDouble() * 100;
            reference.set(j, i, e);
            matrix.setElement(j, i, e);
            assertMatrixEquals(reference, matrix, STRICT);
        }
    }

    /**
     * Tests {@link MatrixSIS#isIdentity()}. This method will first invoke {@link Matrices#create(int, int)}
     * and ensure that the result contains 1 on the diagonal and 0 elsewhere.
     *
     * <p>This method will opportunistically tests {@link MatrixSIS#isAffine()}. The two methods are related
     * since {@code isIdentity()} delegates part of its work to {@code isAffine()}.</p>
     */
    @Test
    @DependsOnMethod("testSetElement")
    public void testIsIdentity() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.create(numRow, numCol);
        validate(matrix);
        /*
         * End of initialization - now perform the actual test.
         */
        assertEquals("isAffine",   numRow == numCol, matrix.isAffine());
        assertEquals("isIdentity", numRow == numCol, matrix.isIdentity());
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = matrix.getElement(j,i);
                assertEquals((i == j) ? 1 : 0, element, STRICT);
                matrix.setElement(j, i, random.nextDouble() - 1.1);
                assertEquals("isAffine", (numRow == numCol) && (j != numRow-1), matrix.isAffine());
                assertFalse("isIdentity", matrix.isIdentity());
                matrix.setElement(j, i, element);
            }
        }
        assertEquals("isAffine",   numRow == numCol, matrix.isAffine());
        assertEquals("isIdentity", numRow == numCol, matrix.isIdentity());
    }
}
