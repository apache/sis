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
     * @param precision 1 for normal precision, or 2 for extended precision.
     */
    NonSquareMatrix(final int numRow, final int numCol, final double[] elements, final int precision) {
        super(numRow, numCol, elements, precision);
    }

    /**
     * Constructs a new matrix and copies the initial values from the given matrix.
     *
     * @param matrix The matrix to copy.
     * @param precision 1 for normal precision, or 2 for extended precision.
     */
    NonSquareMatrix(final Matrix matrix, final int precision) {
        super(matrix, precision);
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
     */
    @Override
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        // TODO: This is where we will need a special treatment different than what JAMA do (because different needs).
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS clone() {
        return new NonSquareMatrix(this);
    }
}
