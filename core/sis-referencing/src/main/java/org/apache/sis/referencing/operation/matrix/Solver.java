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

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ArraysExt;


/**
 * Computes the value of <var>U</var> which solves {@code X} × <var>U</var> = {@code Y}.
 * The {@link #solve(MatrixSIS, Matrix, int)} method in this class is adapted from the {@code LUDecomposition}
 * class of the <a href="http://math.nist.gov/javanumerics/jama">JAMA matrix package</a>. JAMA is provided in
 * the public domain.
 *
 * <p>This class implements the {@link Matrix} interface as an implementation convenience.
 * This implementation details can be ignored.</p>
 *
 * @since   0.4
 * @version 0.4
 * @module
 */
final class Solver implements Matrix {
    /**
     * A immutable identity matrix without defined size.
     * This is used only for computing the inverse.
     */
    private static final Matrix IDENTITY = new Solver();

    /**
     * For the {@link #IDENTITY} constant only.
     */
    private Solver() {
    }

    /**
     * Returns {@code true} since this matrix is the identity matrix.
     */
    @Override
    public boolean isIdentity() {
        return true;
    }

    /**
     * Returns 1 for elements on the diagonal, 0 otherwise.
     * This method never thrown exception.
     */
    @Override
    public double getElement(final int j, final int i) {
        return (j == i) ? 1 : 0;
    }

    /**
     * Unsupported operation since this matrix is immutable.
     */
    @Override
    public void setElement(int j, int i, double d) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code this} since this matrix is immutable.
     */
    @Override
    public Matrix clone() {
        return this;
    }

    /**
     * Arbitrarily returns 0. The actual value does not matter for the purpose of {@code Solver}.
     */
    @Override
    public int getNumRow() {
        return 0;
    }

    /**
     * Arbitrarily returns 0. The actual value does not matter for the purpose of {@code Solver}.
     */
    @Override
    public int getNumCol() {
        return 0;
    }

    /**
     * Computes the inverse of the given matrix. This method shall be invoked only for square matrices
     * (this is <strong>not</strong> verified by this method).
     */
    static MatrixSIS inverse(final MatrixSIS X) throws NoninvertibleMatrixException {
        return solve(X, IDENTITY, X.getNumRow());
    }

    /**
     * Solves {@code X} × <var>U</var> = {@code Y}.
     * This method is an adaptation of the {@code LUDecomposition} class of the JAMA matrix package.
     *
     * <p>This method does <strong>not</strong> checks the matrix size.
     * Check for matrix size shall be performed by the caller like below:</p>
     *
     * {@preformat java
     *     final int size = X.getNumRow();
     *     if (X.getNumCol() != size) {
     *         throw new NoninvertibleTransformException("Matrix must be square.");
     *     }
     *     if (Y.getNumRow() != size) {
     *         throw new MismatchedMatrixSizeException("Matrix row dimensions must agree.");
     *     }
     * }
     *
     * @param  X The matrix to invert.
     * @param  innerSize The value of {@code Y.getNumCol()}.
     * @throws NoninvertibleMatrixException If the {@code X} matrix is singular.
     */
    static MatrixSIS solve(final MatrixSIS X, final Matrix Y, final int innerSize)
            throws NoninvertibleMatrixException
    {
        final int size = X.getNumRow();
        /*
         * Use a "left-looking", dot-product, Crout/Doolittle algorithm.
         */
        final double[] LU = X.getElements();
        final int[] pivot = new int[size];
        for (int j=0; j<size; j++) {
           pivot[j] = j;
        }
        final double[] column = new double[size];
        for (int i=0; i<size; i++) {
            /*
             * Make a copy of the i-th column.
             */
            for (int j=0; j<size; j++) {
                column[j] = LU[j*size + i];
            }
            /*
             * Apply previous transformations.
             */
            for (int j=0; j<size; j++) {
                final int rowOffset = j*size;
                final int kmax = Math.min(j,i);
                double s = 0.0;
                for (int k=0; k<kmax; k++) {
                   s += LU[rowOffset + k] * column[k];
                }
                LU[rowOffset + i] = (column[j] -= s);
            }
            /*
             * Find pivot and exchange if necessary.
             */
            int p = i;
            for (int j=i; ++j < size;) {
                if (Math.abs(column[j]) > Math.abs(column[p])) {
                    p = j;
                }
            }
            if (p != i) {
                final int pRow = p*size;
                final int iRow = i*size;
                for (int k=0; k<size; k++) { // Swap two full rows.
                    ArraysExt.swap(LU, pRow + k, iRow + k);
                }
                ArraysExt.swap(pivot, p, i);
            }
            /*
             * Compute multipliers.
             */
            final double d = LU[i*size + i];
            if (d != 0.0) {
                for (int j=i; ++j < size;) {
                    LU[j*size + i] /= d;
                }
            }
        }
        /*
         * At this point, we are done computing LU.
         * Ensure that the matrix is not singular.
         */
        for (int j=0; j<size; j++) {
            if (LU[j*size + j] == 0) {
                throw new NoninvertibleMatrixException();
            }
        }
        /*
         * Copy right hand side with pivoting.
         * We will write the result of this method directly in the elements array.
         */
        final double[] elements = new double[size * innerSize];
        for (int k=0,j=0; j<size; j++) {
            final int p = pivot[j];
            for (int i=0; i<innerSize; i++) {
                elements[k++] = Y.getElement(p, i);
            }
        }
        /*
         * Solve L*Y = B(pivot, :)
         */
        for (int k=0; k<size; k++) {
            final int rowOffset = k*innerSize;          // Offset of row computed by current iteration.
            for (int j=k; ++j < size;) {
                final int loRowOffset = j*innerSize;    // Offset of a row after (locate lower) the current row.
                final int luRowOffset = j*size;  // Offset of the corresponding row in the LU matrix.
                for (int i=0; i<innerSize; i++) {
                    elements[loRowOffset + i] -= (elements[rowOffset + i] * LU[luRowOffset + k]);
                }
            }
        }
        /*
         * Solve U*X = Y
         */
        for (int k=size; --k >= 0;) {
            final int rowOffset = k*innerSize;          // Offset of row computed by current iteration.
            final double d = LU[k*size + k];     // A diagonal element on the current row.
            for (int i=0; i<innerSize; i++) {           // Apply to all columns in the current row.
                elements[rowOffset + i] /= d;
            }
            for (int j=0; j<k; j++) {
                final int upRowOffset = j*innerSize;    // Offset of a row before (locate upper) the current row.
                final double c = LU[j*size + k]; // Same column than the diagonal element, but in the upper row.
                for (int i=0; i<innerSize; i++) {       // Apply to all columns in the upper row.
                    elements[upRowOffset + i] -= (elements[rowOffset + i] * c);
                }
            }
        }
        return Matrices.create(size, innerSize, elements);
    }
}
