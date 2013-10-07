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


/**
 * A matrix which is not square.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see Matrices#create(int, int)
 */
final class NonSquareMatrix extends GeneralMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5481206711680231697L;

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol}.
     * If {@code setToIdentity} is {@code true}, then the elements
     * on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * @param numRow Number of rows.
     * @param numCol Number of columns.
     * @param setToIdentity {@code true} for initializing the matrix to the identity matrix,
     *        or {@code false} for leaving it initialized to zero.
     * @param precision 1 for normal precision, or 2 for extended precision.
     */
    NonSquareMatrix(final int numRow, final int numCol, final boolean setToIdentity, final int precision) {
        super(numRow, numCol, setToIdentity, precision);
    }

    /**
     * Constructs a {@code numRow} × {@code numCol} matrix initialized to the values in the {@code elements} array.
     * The array values are copied in one row at a time in row major fashion.
     * The array shall be exactly {@code numRow*numCol} in length.
     *
     * @param numRow Number of rows.
     * @param numCol Number of columns.
     * @param elements Initial values.
     */
    NonSquareMatrix(final int numRow, final int numCol, final double[] elements) {
        super(numRow, numCol, elements);
    }

    /**
     * Constructs a new matrix and copies the initial values from the given matrix.
     *
     * @param matrix The matrix to copy.
     */
    NonSquareMatrix(final Matrix matrix) {
        super(matrix);
    }

    /**
     * Creates a clone of the given matrix, for {@link #clone()} usage only.
     */
    private NonSquareMatrix(final GeneralMatrix matrix) {
        super(matrix);
    }

    /**
     * Sets the value of this matrix to its transpose.
     */
    @Override
    public void transpose() {
        final short numRow = this.numRow; // Protection against accidental changes before we are done.
        final short numCol = this.numCol;
        final int   errors = indexOfErrors(numRow, numCol, elements); // Where error values start, or 0 if none.
        final double[] copy = elements.clone();
        int k = 0;
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final int t = i*numRow + j;
                elements[t] = copy[k];
                if (errors != 0) {
                    elements[t + errors] = copy[k + errors];
                }
                k++;
            }
        }
        this.numRow = numCol;
        this.numCol = numRow;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method delegates the work to {@code inverse().multiply(matrix)} in order to leverage
     * the special handling done by {@code inverse()} for non-square matrices.</p>
     */
    @Override
    public MatrixSIS solve(final Matrix matrix) throws MismatchedMatrixSizeException, NoninvertibleMatrixException {
        MatrixSIS result = inverse();
        if (!matrix.isIdentity()) {
            result = result.multiply(matrix);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method performs a special check for non-square matrices in an attempt to invert them anyway.
     * If this matrix has more columns than rows, then this method can invert that matrix if and only if
     * some columns contain only 0 elements. In such case, the dimensions corresponding to those columns are
     * considered independent of all other dimensions. This happen typically with the dimension of <var>z</var>
     * and <var>t</var> ordinate values.</p>
     *
     * <p><b>Example:</b> in a conversion from (x₁,y₁,z,t) to (x₂,y₂), if the (x,y) dimensions are independent
     * of z and t dimensions, then we do not need those (z,t) dimensions for calculating the inverse of (x₁,y₁)
     * to (x₂,y₂). We can omit the (z,t) dimensions in order to have a square matrix, perform the inversion,
     * then insert NaN in place of the omitted dimensions. In the matrix below, we can see that (x,y) are
     * independent of (z,t) because the 3th and 4th columns contains only 0 elements:</p>
     *
     * {@preformat math
     *   ┌               ┐ -1        ┌                  ┐
     *   │ 2  0  0  0  8 │           │ 0.5  0     -4.00 │
     *   │ 0  4  0  0  5 │     =     │ 0    0.25  -1.25 │
     *   │ 0  0  0  0  1 │           │ 0    0       NaN │
     *   └               ┘           │ 0    0       NaN │
     *                               │ 0    0      1    │
     *                               └                  ┘
     * }
     *
     * There is an issue about whether the full row shall contains NaN, or only the last element (the translation
     * term) as in the above example.  The current implementation inserts a NaN value in the translation term and
     * sets all other values to 0 on the assumption that if (x₂,y₂) do not depend on (z,t), then conversely (z,t)
     * do not depend on (x₂,y₂) neither.
     */
    @Override
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        final int numRow = this.numRow; // Protection against accidental changes.
        final int numCol = this.numCol;
        if (numRow < numCol) {
            final int length = numRow * numCol;
            /*
             * Target points have fewer ordinates than source points. If a column contains only zero values,
             * then this means that the ordinate at the corresponding column is simply deleted. We can omit
             * that column. We check the last columns before the first columns on the assumption that last
             * dimensions are more likely to be independant dimensions like time.
             */
            int oi = numCol - numRow;
            final int[] omitted = new int[oi];
skipColumn: for (int i=numCol; --i>=0;) {
                for (int j=length + i; (j -= numCol) >= 0;) {
                    if (elements[j] != 0) {
                        continue skipColumn;
                    }
                }
                omitted[--oi] = i; // Found a column which contains only 0 elements.
                if (oi == 0) {
                    /*
                     * Found enough columns containing only zero elements. Create a square matrix omitting those
                     * columns, and invert that matrix. Note that we also need to either copy the error terms,
                     * or to infer them.
                     */
                    GeneralMatrix squareMatrix = new GeneralMatrix(numRow, numRow, false, 2);
                    int j=0;
                    for (i=0; i<numCol; i++) {
                        if (oi != omitted.length && i == omitted[oi]) oi++;
                        else copyColumnTo(i, squareMatrix, j++); // Copy only if not skipped.
                    }
                    // If the source matrix does not use double-double arithmetic, infer the error terms.
                    if (indexOfErrors(numRow, numCol, elements) == 0) {
                        inferErrors(squareMatrix.elements);
                    }
                    squareMatrix = (GeneralMatrix) Solver.inverse(squareMatrix, false);
                    /*
                     * Create a new matrix with new rows added for the omitted ordinates.
                     * From this point, the meaning of 'numCol' and 'numRow' are interchanged.
                     */
                    final NonSquareMatrix inverse = new NonSquareMatrix(numCol, numRow, false, 2);
                    for (oi=0, j=0, i=0; i<numCol; i++) {
                        if (oi != omitted.length && i == omitted[oi]) {
                            inverse.setElement(i, numRow-1, Double.NaN);
                            oi++;
                        } else {
                            inverse.copyRowFrom(squareMatrix, j++, i);
                        }
                    }
                    return inverse;
                }
            }
        }
        /*
         * If we reach this point, we have not been able to replace the non-square matrix by a square one.
         * Delegate to the super-class method as a matter of principle, but that method is expected to fail.
         */
        return super.inverse();
    }

    /**
     * Copies a column from this matrix to the given matrix, including the double-double arithmetic error terms
     * if any. The given matrix must have the same number of rows than this matrix, and must have enough room for
     * error terms (this is not verified).
     *
     * @param srcIndex  Index of the column to copy from this matrix.
     * @param target    The matrix where to copy the column.
     * @param dstIndex  Index of the column where to copy in the target matrix.
     */
    private void copyColumnTo(int srcIndex, final GeneralMatrix target, int dstIndex) {
        assert target.numRow == numRow;
        while (srcIndex < elements.length) {
            target.elements[dstIndex] = elements[srcIndex];
            srcIndex += this.numCol;
            dstIndex += target.numCol;
        }
    }

    /**
     * Copies a row from the given matrix to this matrix, including the double-double arithmetic error terms.
     * The two matrix must have the same number of columns, and both of them must have room for the error terms
     * (this is not verified).
     *
     * @param source   The matrix from which to copy a row.
     * @param srcIndex Index of the row to copy from the source matrix.
     * @param dstIndex Index of the row where to copy in this matrix.
     */
    private void copyRowFrom(final GeneralMatrix source, int srcIndex, int dstIndex) {
        final int numCol = this.numCol;
        assert numCol == source.numCol;
        srcIndex *= numCol;
        dstIndex *= numCol;
        System.arraycopy(source.elements, srcIndex, elements, dstIndex, numCol);  // Copy main values
        System.arraycopy(source.elements, srcIndex + numCol * source.numRow,      // Copy error terms
                                elements, dstIndex + numCol * numRow, numCol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS clone() {
        return new NonSquareMatrix(this);
    }
}
