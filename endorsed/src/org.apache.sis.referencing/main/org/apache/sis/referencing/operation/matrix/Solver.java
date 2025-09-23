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
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.util.ArraysExt;
import static org.apache.sis.referencing.internal.Arithmetic.add;
import static org.apache.sis.referencing.internal.Arithmetic.subtract;
import static org.apache.sis.referencing.internal.Arithmetic.multiply;
import static org.apache.sis.referencing.internal.Arithmetic.divide;


/**
 * Computes the value of <var>U</var> which solves {@code X} × <var>U</var> = {@code Y}.
 * The {@link #solve(double[], Matrix, double[], int, int)} method in this class is adapted from the
 * {@code LUDecomposition} class of the <a href="http://math.nist.gov/javanumerics/jama">JAMA matrix package</a>.
 * JAMA is provided in the public domain.
 *
 * <p>This class implements the {@link Matrix} interface as an implementation convenience.
 * It implements an identity matrix of any size. This implementation details can be ignored.</p>
 *
 * @author  JAMA team
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Solver implements ExtendedPrecisionMatrix {                 // Not Cloneable, despite the clone() method.
    /**
     * The size of the (i, j, s) tuples used internally by {@link #solve(Matrix, Matrix, double[], int, int, boolean)}
     * for storing information about the NaN values.
     */
    private static final int TUPLE_SIZE = 3;

    /**
     * A immutable identity matrix without defined size.
     * This is used only for computing the inverse.
     */
    private static final ExtendedPrecisionMatrix IDENTITY = new Solver();

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
     * Returns 1 for elements on the diagonal, {@code null} otherwise.
     * This method never throws exception.
     */
    @Override
    public Number getElementOrNull(int j, int i) {
        return (j == i) ? 1 : null;
    }

    /**
     * Returns 1 for elements on the diagonal, 0 otherwise.
     * This method never throws exception.
     */
    @Override
    public double getElement(final int j, final int i) {
        return (j == i) ? 1 : 0;
    }

    /**
     * Returns {@code this} since this matrix is immutable.
     * This method is defined because required by {@link Matrix} interface.
     */
    @Override
    @SuppressWarnings({"CloneInNonCloneableClass", "CloneDoesntCallSuperClone"})
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
     * Computes the inverse of the given matrix. This method shall be invoked only for square matrices.
     *
     * @param  X  the matrix to invert, which must be square.
     * @throws NoninvertibleMatrixException if the {@code X} matrix is not square or singular.
     */
    static GeneralMatrix inverse(final Matrix X) throws NoninvertibleMatrixException {
        final int size = X.getNumRow();
        final int numCol = X.getNumCol();
        if (numCol != size) {
            throw new NoninvertibleMatrixException(Resources.format(Resources.Keys.NonInvertibleMatrix_2, size, numCol));
        }
        return solve(MatrixSIS.asExtendedPrecision(X), IDENTITY, size, size);
    }

    /**
     * Solves {@code X} × <var>U</var> = {@code Y}.
     * This method is an adaptation of the {@code LUDecomposition} class of the JAMA matrix package.
     *
     * @param  X  the matrix to invert.
     * @param  Y  the desired result of {@code X} × <var>U</var>.
     * @throws NoninvertibleMatrixException if the {@code X} matrix is not square or singular.
     */
    static GeneralMatrix solve(final Matrix X, final Matrix Y) throws NoninvertibleMatrixException {
        final int size   = X.getNumRow();
        final int numCol = X.getNumCol();
        if (numCol != size) {
            throw new NoninvertibleMatrixException(Resources.format(Resources.Keys.NonInvertibleMatrix_2, size, numCol));
        }
        final int innerSize = Y.getNumCol();
        GeneralMatrix.ensureNumRowMatch(size, Y.getNumRow(), innerSize);
        return solve(MatrixSIS.asExtendedPrecision(X),
                     MatrixSIS.asExtendedPrecision(Y),
                     size, innerSize);
    }

    /**
     * Implementation of {@code solve} and {@code inverse} methods, with filtering of NaN values.
     * This method searches for NaN values before to attempt solving or inverting the matrix.
     * If some NaN values are found but the matrix is written in such a way that each NaN value
     * is used for exactly one coordinate value (i.e. a matrix row is used for a one-dimensional
     * conversion which is independent of all other dimensions), then we will edit the matrix in
     * such a way that this NaN value does not prevent the inverse matrix to be computed.
     *
     * <p>This method does <strong>not</strong> checks the matrix size.
     * Check for matrix size shall be performed by the caller like below:</p>
     *
     * {@snippet lang="java" :
     *     final int size = X.getNumRow();
     *     if (X.getNumCol() != size) {
     *         throw new NoninvertibleMatrixException("Matrix must be square.");
     *     }
     *     if (Y.getNumRow() != size) {
     *         throw new MismatchedMatrixSizeException("Matrix row dimensions must agree.");
     *     }
     *     }
     *
     * @param  X          the matrix to invert, which must be square.
     * @param  Y          the desired result of {@code X} × <var>U</var>.
     * @param  size       the value of {@code X.getNumRow()}, {@code X.getNumCol()} and {@code Y.getNumRow()}.
     * @param  innerSize  the value of {@code Y.getNumCol()}.
     * @throws NoninvertibleMatrixException if the {@code X} matrix is not square or singular.
     */
    private static GeneralMatrix solve(final ExtendedPrecisionMatrix X, final ExtendedPrecisionMatrix Y,
            final int size, final int innerSize) throws NoninvertibleMatrixException
    {
        assert (X.getNumRow() == size && X.getNumCol() == size) : size;
        assert (Y.getNumRow() == size && Y.getNumCol() == innerSize) || (Y instanceof Solver);
        final Number[] LU = X.getElementAsNumbers(true);    // We will write in the LU array.
        final int lastRowOrColumn = size - 1;
        /*
         * indexOfNaN array will be created only if at least one NaN value is found, and those NaN met
         * the conditions documented in the code below. In such case, the array will contain a sequence
         * of (i,j,s) where (i,j) are the indices where the NaN value has been found and s is the column
         * of the scale factor.
         */
        int[] indexOfNaN = null;
        int   indexCount = 0;
        if ((X instanceof MatrixSIS) ? ((MatrixSIS) X).isAffine() : MatrixSIS.isAffine(X)) {    // Avoid dependency to Matrices class.
            /*
             * Conservatively search for NaN values only if the matrix looks like an affine transform.
             * If the matrix is affine, then we will assume that we can interpret the last column as
             * translation terms and other columns as scale factors.
             *
             * Note: the iteration below skips the last row, since it is (0, 0, ..., 1).
             */
searchNaN:  for (int flatIndex = (size - 1) * size; --flatIndex >= 0;) {
                if (Double.isNaN(doubleValue(LU[flatIndex]))) {
                    final int j = flatIndex / size;
                    final int i = flatIndex % size;
                    /*
                     * Found a NaN value. First, if we are not in the translation column, ensure
                     * that the column contains only zero values except on the current line.
                     */
                    int columnOfScale = -1;
                    if (i != lastRowOrColumn) {                     // Enter only if this column is not for translations.
                        columnOfScale = i;                          // The non-translation element is the scale factor.
                        for (int k=lastRowOrColumn; --k>=0;) {      // Scan all other rows in the current column.
                            if (k != j && LU[k*size + i] != null) {
                                /*
                                 * Found a non-zero element in the current column.
                                 * We cannot proceed - cancel everything.
                                 */
                                indexOfNaN = null;
                                indexCount = 0;
                                break searchNaN;
                            }
                        }
                    }
                    /*
                     * Next, ensure that the row contains only zero elements except for
                     * the scale factor and the offset (the element in the translation
                     * column, which is not checked by the loop below).
                     */
                    for (int k=lastRowOrColumn; --k>=0;) {
                        if (k != i && LU[j*size + k] != null) {
                            if (columnOfScale >= 0) {
                                /*
                                 * If there is more than 1 non-zero element,
                                 * abandon the attempt to handle NaN values.
                                 */
                                indexOfNaN = null;
                                indexCount = 0;
                                break searchNaN;
                            }
                            columnOfScale = k;
                        }
                    }
                    /*
                     * At this point, the NaN element has been determined as replaceable.
                     * Remember its index; the replacement will be performed later.
                     */
                    if (indexOfNaN == null) {
                        indexOfNaN = new int[lastRowOrColumn * (2*TUPLE_SIZE)];     // At most one scale and one offset per row.
                    }
                    indexOfNaN[indexCount++] = i;
                    indexOfNaN[indexCount++] = j;
                    indexOfNaN[indexCount++] = columnOfScale;                   // May be -1 (while uncommon)
                    assert (indexCount % TUPLE_SIZE) == 0;
                }
            }
            /*
             * If there is any NaN value to edit, replace them by 0 if they appear in the translation column
             * or by 1 otherwise (scale or shear). We perform this replacement after the loop searching for
             * NaN, not inside the loop, in order to not change anything if the search has been canceled.
             */
            for (int k=0; k<indexCount; k += TUPLE_SIZE) {
                final int i = indexOfNaN[k  ];
                final int j = indexOfNaN[k+1];
                LU[j*size + i] = (i == lastRowOrColumn) ? null : 1;
            }
        }
        /*
         * Now apply the inversion.
         */
        final GeneralMatrix matrix = solve(LU, Y, size, innerSize);
        /*
         * At this point, the matrix has been inverted. If they were any NaN value in the original
         * matrix, set the corresponding scale factor and offset to NaN in the resulting matrix.
         */
        for (int k=0; k<indexCount;) {
            assert (k % TUPLE_SIZE) == 0;
            final int i = indexOfNaN[k++];
            final int j = indexOfNaN[k++];
            final int s = indexOfNaN[k++];
            if (i != lastRowOrColumn) {
                // Found a scale factor to set to NaN.
                matrix.setElement(i, j, Double.NaN);                      // Note that i,j indices are interchanged.
                if (matrix.getElementOrNull(i, lastRowOrColumn) != null) {
                    matrix.setElement(i, lastRowOrColumn, Double.NaN);    // = -offset/scale, so 0 stay 0.
                }
            } else if (s >= 0) {
                // Found a translation factory to set to NaN.
                matrix.setElement(s, lastRowOrColumn, Double.NaN);
            }
        }
        return matrix;
    }

    /**
     * Implementation of {@code solve} and {@code inverse} methods.
     * This method contains the code ported from the JAMA package.
     * Use a "left-looking", dot-product, Crout/Doolittle algorithm.
     *
     * <p>This method does <strong>not</strong> checks the matrix size.
     * It is caller's responsibility to ensure that the following hold:</p>
     *
     * {@snippet lang="java" :
     *     assert X.getNumRow() == size;
     *     assert X.getNumCol() == size;
     *     assert Y.getNumRow() == size;
     *     assert Y.getNumCol() == innerSize;
     *     }
     *
     * @param  LU         elements of the {@code X} matrix to invert.
     * @param  Y          the desired result of {@code X} × <var>U</var>.
     * @param  size       the value of {@code X.getNumRow()}, {@code X.getNumCol()} and {@code Y.getNumRow()}.
     * @param  innerSize  the value of {@code Y.getNumCol()}.
     * @throws NoninvertibleMatrixException if the {@code X} matrix is not square or singular.
     */
    private static GeneralMatrix solve(final Number[] LU, final ExtendedPrecisionMatrix Y,
            final int size, final int innerSize) throws NoninvertibleMatrixException
    {
        final int[] pivot = ArraysExt.range(0, size);
        final Number[] column = new Number[size];
        for (int i=0; i<size; i++) {
            /*
             * Make a copy of the i-th column.
             * The array may contain null elements, which stand for zero.
             */
            for (int j=0; j<size; j++) {
                column[j] = LU[j*size + i];
            }
            /*
             * Apply previous transformations. This part is equivalent to the following code,
             * but using double-double arithmetic instead of the primitive `double` type:
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
                Number sum = null;
                for (int k=0; k<kmax; k++) {
                    sum = add(sum, multiply(LU[rowOffset + k], column[k]));
                }
                LU[rowOffset + i] = column[j] = subtract(column[j], sum);
            }
            /*
             * Find pivot and exchange if necessary. There is no floating-point arithmetic here
             * (ignoring the comparison for magnitude order), only work on index values.
             */
            int p = i;
            for (int j=i; ++j < size;) {
                if (Math.abs(doubleValue(column[j])) > Math.abs(doubleValue(column[p]))) {
                    p = j;
                }
            }
            if (p != i) {
                final int pRow = p*size;
                final int iRow = i*size;
                for (int k=0; k<size; k++) {                    // Swap two full rows.
                    ArraysExt.swap(LU, pRow + k, iRow + k);
                }
                ArraysExt.swap(pivot, p, i);
            }
            /*
             * Compute multipliers. This part is equivalent to the following code, but
             * using double-double arithmetic instead of the primitive `double` type:
             *
             *     final double sum = LU[i*size + i];
             *     if (sum != 0.0) {
             *         for (int j=i; ++j < size;) {
             *             LU[j*size + i] /= sum;
             *         }
             *     }
             */
            final Number sum = LU[i*size + i];
            if (sum != null) {
                for (int j=i; ++j < size;) {
                    final int t = j*size + i;
                    LU[t] = divide(LU[t], sum);
                }
            }
        }
        /*
         * At this point, we are done computing LU.
         * Ensure that the matrix is not singular.
         */
        for (int j=0; j<size; j++) {
            if (LU[j*size + j] == null) {
                throw new NoninvertibleMatrixException(Resources.format(Resources.Keys.SingularMatrix));
            }
        }
        /*
         * Copy right hand side with pivoting. Write the result directly in the elements array
         * of the result matrix. This block does not perform floating-point arithmetic operations.
         */
        final GeneralMatrix result = GeneralMatrix.create(size, innerSize, false);
        final Number[] elements = result.elements;
        for (int k=0,j=0; j<size; j++) {
            final int p = pivot[j];
            for (int i=0; i<innerSize; i++) {
                elements[k++] = Y.getElementOrNull(p, i);
            }
        }
        /*
         * Solve L*Y = B(pivot, :). The inner block is equivalent to the following line,
         * but using double-double arithmetic instead of `double` primitive type:
         *
         *     elements[loRowOffset + i] -= (elements[rowOffset + i] * LU[luRowOffset + k]);
         */
        for (int k=0; k<size; k++) {
            final int rowOffset = k*innerSize;              // Offset of row computed by current iteration.
            for (int j=k; ++j < size;) {
                final int loRowOffset = j*innerSize;        // Offset of some row after the current row.
                final int luRowOffset = j*size;             // Offset of the corresponding row in the LU matrix.
                for (int i=0; i<innerSize; i++) {
                    final int t = loRowOffset + i;
                    elements[t] = subtract(elements[t], multiply(elements[rowOffset + i], LU[luRowOffset + k]));
                }
            }
        }
        /*
         * Solve U*X = Y. The content of the loop is equivalent to the following line,
         * but using double-double arithmetic instead of `double` primitive type:
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
            final int rowOffset = k*innerSize;              // Offset of row computed by current iteration.
            Number sum = LU[k*size + k];                    // A diagonal element on the current row.
            for (int i=0; i<innerSize; i++) {               // Apply to all columns in the current row.
                final int t = rowOffset + i;
                elements[t] = divide(elements[t], sum);
            }
            for (int j=0; j<k; j++) {
                final int upRowOffset = j*innerSize;        // Offset of a row before (locate upper) the current row.
                sum = LU[j*size + k];                       // Same column as the diagonal element, but in the upper row.
                for (int i=0; i<innerSize; i++) {           // Apply to all columns in the upper row.
                    final int t = upRowOffset + i;
                    elements[t] = subtract(elements[t], multiply(elements[rowOffset + i], sum));
                }
            }
        }
        return result;
    }

    /**
     * Returns the value with {@code null} replaced by zero.
     */
    private static double doubleValue(final Number value) {
        return (value != null) ? value.doubleValue() : 0;
    }
}
