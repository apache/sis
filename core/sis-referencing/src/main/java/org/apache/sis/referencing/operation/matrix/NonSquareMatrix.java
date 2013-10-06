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
     * <p>This method performs a special check for non-square matrix in an attempt to invert them anyway.
     * This is possible only if some columns or rows contain contain only 0 elements.</p>
     */
    @Override
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        final int numRow = this.numRow; // Protection against accidental changes.
        final int numCol = this.numCol;
        if (numRow < numCol) {
            /*
             * Target points have fewer ordinates than source point. If a column contains only zero values,
             * then this means that the ordinate at the corresponding column is simply deleted. We can omit
             * that column. We check the last columns before the first columns on the assumption that last
             * dimensions are more likely to be independant dimensions like time.
             */
            int oi = numCol - numRow;
            final int[] omitted = new int[oi];
skipColumn: for (int i=numCol; --i>=0;) {
                for (int j=numRow; --j>=0;) {
                    if (getElement(j, i) != 0) {
                        continue skipColumn;
                    }
                }
                // Found a column which contains only 0 elements.
                omitted[--oi] = i;
                if (oi == 0) {
                    break; // Found enough columns to skip.
                }
            }
            if (oi == 0) {
                /*
                 * Create a square matrix omitting some or all columns containing only 0 elements, and invert
                 * that matrix. Finally, create a new matrix with new rows added for the omitted ordinates.
                 */
                MatrixSIS squareMatrix = new GeneralMatrix(numRow, numRow, false, 2);
                for (int k=0,i=0; i<numCol; i++) {
                    if (oi != omitted.length && i == omitted[oi]) {
                        oi++;
                    } else {
                        for (int j=numRow; --j>=0;) {
                            squareMatrix.setElement(j, k, getElement(j, i));
                        }
                        k++;
                    }
                }
                squareMatrix = squareMatrix.inverse();
                /*
                 * From this point, the meaning of 'numCol' and 'numRow' are interchanged.
                 */
                final MatrixSIS inverse = new NonSquareMatrix(numCol, numRow, false, 2);
                oi = 0;
                for (int k=0,j=0; j<numCol; j++) {
                    if (oi != omitted.length && j == omitted[oi]) {
                        if (j < numRow) {
                            inverse.setElement(j, j, 0);
                        }
                        inverse.setElement(j, numRow-1, Double.NaN);
                        oi++;
                    } else {
                        for (int i=numRow; --i>=0;) {
                            inverse.setElement(j, i, squareMatrix.getElement(k, i));
                        }
                        k++;
                    }
                }
                return inverse;
            }
        }
        return super.inverse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS clone() {
        return new NonSquareMatrix(this);
    }
}
