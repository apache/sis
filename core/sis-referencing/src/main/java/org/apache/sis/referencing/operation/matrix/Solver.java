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
import org.apache.sis.internal.util.DoubleDouble;
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
        final int size = X.getNumRow();
        return solve(X, IDENTITY, null, size, size);
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
     * @param  Y The desired result of {@code X} × <var>U</var>.
     * @param  size The value of {@code X.getNumRow()}, {@code X.getNumCol()} and {@code Y.getNumRow()}.
     * @param  innerSize The value of {@code Y.getNumCol()}.
     * @throws NoninvertibleMatrixException If the {@code X} matrix is singular.
     */
    static MatrixSIS solve(final MatrixSIS X, final Matrix Y, final int size, final int innerSize)
            throws NoninvertibleMatrixException
    {
        double[] eltY = null;
        if (Y instanceof GeneralMatrix) {
            eltY = ((GeneralMatrix) Y).elements;
            if (eltY.length == size * innerSize) {
                eltY = null; // Matrix does not contains error terms.
            }
        }
        return solve(X, Y, eltY, size, innerSize);
    }

    /**
     * Implementation of {@code solve} and {@code inverse} methods.
     * Use a "left-looking", dot-product, Crout/Doolittle algorithm.
     *
     * @param eltY Elements and error terms of the {@code Y} matrix, or {@code null} if not available.
     */
    private static MatrixSIS solve(final MatrixSIS X, final Matrix Y, final double[] eltY,
            final int size, final int innerSize) throws NoninvertibleMatrixException
    {
        assert (X.getNumRow() == size && X.getNumCol() == size) : size;
        assert (Y.getNumRow() == size && Y.getNumCol() == innerSize) || (Y instanceof Solver);

        final int errorLU = size * size;
        final double[] LU = GeneralMatrix.getExtendedElements(X, size, size, true);
        assert errorLU == GeneralMatrix.indexOfErrors(size, size, LU);
        final int[] pivot = new int[size];
        for (int j=0; j<size; j++) {
           pivot[j] = j;
        }
        final double[]  column = new double[size * 2]; // [0 … size-1] : column values; [size … 2*size-1] : error terms.
        final DoubleDouble acc = new DoubleDouble();   // Temporary variable for sum ("accumulator") and subtraction.
        final DoubleDouble rat = new DoubleDouble();   // Temporary variable for products and ratios.
        for (int i=0; i<size; i++) {
            /*
             * Make a copy of the i-th column.
             */
            for (int j=0; j<size; j++) {
                final int k = j*size + i;
                column[j]        = LU[k];            // Value
                column[j + size] = LU[k + errorLU];  // Error
            }
            /*
             * Apply previous transformations. This part is equivalent to the following code,
             * but using double-double arithmetic instead than the primitive 'double' type:
             *
             *     double sum = 0;
             *     for (int k=0; k<kmax; k++) {
             *         sum += LU[rowOffset + k] * column[k];
             *     }
             *     LU[rowOffset + i] = (column[j] -= sum);
             */
            for (int j=0; j<size; j++) {
                final int rowOffset = j*size;
                final int kmax = Math.min(j,i);
                acc.clear();
                for (int k=0; k<kmax; k++) {
                    rat.setFrom(LU, rowOffset + k, errorLU);
                    rat.multiply(column, k, size);
                    acc.add(rat);
                }
                acc.subtract(column, j, size);
                acc.negate();
                acc.storeTo(column, j, size);
                acc.storeTo(LU, rowOffset + i, errorLU);
            }
            /*
             * Find pivot and exchange if necessary. There is no floating-point arithmetic here
             * (ignoring the comparison for magnitude order), only work on index values.
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
                    DoubleDouble.swap(LU, pRow + k, iRow + k, errorLU);
                }
                ArraysExt.swap(pivot, p, i);
            }
            /*
             * Compute multipliers. This part is equivalent to the following code, but
             * using double-double arithmetic instead than the primitive 'double' type:
             *
             *     final double sum = LU[i*size + i];
             *     if (sum != 0.0) {
             *         for (int j=i; ++j < size;) {
             *             LU[j*size + i] /= sum;
             *         }
             *     }
             */
            acc.setFrom(LU, i*size + i, errorLU);
            if (!acc.isZero()) {
                for (int j=i; ++j < size;) {
                    final int t = j*size + i;
                    rat.setFrom(acc);
                    rat.inverseDivide(LU, t, errorLU);
                    rat.storeTo      (LU, t, errorLU);
                }
            }
        }
        /*
         * At this point, we are done computing LU.
         * Ensure that the matrix is not singular.
         */
        for (int j=0; j<size; j++) {
            rat.setFrom(LU, j*size + j, errorLU);
            if (rat.isZero()) {
                throw new NoninvertibleMatrixException();
            }
        }
        /*
         * Copy right hand side with pivoting. Write the result directly in the elements array
         * of the result matrix. This block does not perform floating-point arithmetic operations.
         */
        final GeneralMatrix result = GeneralMatrix.createExtendedPrecision(size, innerSize);
        final double[] elements = result.elements;
        final int errorOffset = size * innerSize;
        for (int k=0,j=0; j<size; j++) {
            final int p = pivot[j];
            for (int i=0; i<innerSize; i++) {
                if (eltY != null) {
                    final int t = p*innerSize + i;
                    elements[k]               = eltY[t];
                    elements[k + errorOffset] = eltY[t + errorOffset];
                } else {
                    elements[k] = Y.getElement(p, i);
                }
                k++;
            }
        }
        /*
         * Solve L*Y = B(pivot, :). The inner block is equivalent to the following line,
         * but using double-double arithmetic instead of 'double' primitive type:
         *
         *     elements[loRowOffset + i] -= (elements[rowOffset + i] * LU[luRowOffset + k]);
         */
        for (int k=0; k<size; k++) {
            final int rowOffset = k*innerSize;          // Offset of row computed by current iteration.
            for (int j=k; ++j < size;) {
                final int loRowOffset = j*innerSize;    // Offset of some row after the current row.
                final int luRowOffset = j*size;         // Offset of the corresponding row in the LU matrix.
                for (int i=0; i<innerSize; i++) {
                    acc.setFrom (elements, loRowOffset + i, errorOffset);
                    rat.setFrom (elements, rowOffset   + i, errorOffset);
                    rat.multiply(LU,       luRowOffset + k, errorLU);
                    acc.subtract(rat);
                    acc.storeTo (elements, loRowOffset + i, errorOffset);
                }
            }
        }
        /*
         * Solve U*X = Y. The content of the loop is equivalent to the following line,
         * but using double-double arithmetic instead of 'double' primitive type:
         *
         *     double sum = LU[k*size + k];
         *     for (int i=0; i<innerSize; i++) {
         *         elements[rowOffset + i] /= sum;
         *     }
         *     for (int j=0; j<k; j++) {
         *         sum = LU[j*size + k];
         *         for (int i=0; i<innerSize; i++) {
         *             elements[upRowOffset + i] -= (elements[rowOffset + i] * sum);
         *         }
         *     }
         */
        for (int k=size; --k >= 0;) {
            final int rowOffset = k*innerSize;          // Offset of row computed by current iteration.
            acc.setFrom(LU, k*size + k, errorLU);       // A diagonal element on the current row.
            for (int i=0; i<innerSize; i++) {           // Apply to all columns in the current row.
                rat.setFrom(acc);
                rat.inverseDivide(elements, rowOffset + i, errorOffset);
                rat.storeTo      (elements, rowOffset + i, errorOffset);
            }
            for (int j=0; j<k; j++) {
                final int upRowOffset = j*innerSize;    // Offset of a row before (locate upper) the current row.
                acc.setFrom(LU, j*size + k, errorLU);   // Same column than the diagonal element, but in the upper row.
                for (int i=0; i<innerSize; i++) {       // Apply to all columns in the upper row.
                    rat.setFrom(elements, rowOffset + i, errorOffset);
                    rat.multiply(acc);
                    rat.subtract(elements, upRowOffset + i, errorOffset);
                    rat.negate();
                    rat.storeTo(elements, upRowOffset + i, errorOffset);
                }
            }
        }
        return result;
    }
}
