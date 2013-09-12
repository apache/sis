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
     * Random number generator, created by {@link #initialize(String, boolean)} when first needed.
     */
    Random random;

    /**
     * For subclasses only.
     */
    MatrixTestCase() {
    }

    /**
     * Initializes the test. This method shall be invoked at the beginning of each test method.
     *
     * @param testMethod The name of the method which need a random number generator.
     * @param needsRandom {@code true} if the test method will need random numbers.
     */
    void initialize(final String testMethod, final boolean needsRandom) {
        if (needsRandom && random == null) {
            random = TestUtilities.createRandomNumberGenerator(testMethod);
        }
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
     * Tests {@link MatrixSIS#isIdentity()}. This method will first invoke {@link Matrices#create(int, int)}
     * and ensure that the result contains 1 on the diagonal and 0 elsewhere.
     */
    @Test
    public void testIsIdentity() {
        initialize("testIsIdentity", false);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.create(numRow, numCol);
        validate(matrix);
        assertEquals("isIdentity", numRow == numCol, matrix.isIdentity());
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = matrix.getElement(j,i);
                assertEquals((i == j) ? 1 : 0, element, 0);
                matrix.setElement(j, i, 2);
                assertFalse("isIdentity", matrix.isIdentity());
                matrix.setElement(j, i, element);
            }
        }
        assertEquals("isIdentity", numRow == numCol, matrix.isIdentity());
    }
}
