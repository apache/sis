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

import java.util.Arrays;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.MathFunctions;


/**
 * A two dimensional array of numbers. Row and column numbering begins with zero.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 *
 * @see Matrices#create(int, int)
 */
class GeneralMatrix extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8447482612423035360L;

    /**
     * All matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is <code>{@linkplain #numRow} * {@linkplain #numCol}</code>.
     */
    private final double[] elements;

    /**
     * Number of rows and columns.
     */
    private final short numRow, numCol;

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol}.
     * If {@code setToIdentity} is {@code true}, then the elements
     * on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * @param numRow Number of rows.
     * @param numCol Number of columns.
     * @param setToIdentity {@code true} for initializing the matrix to the identity matrix,
     *        or {@code false} for leaving it initialized to zero.
     */
    public GeneralMatrix(final int numRow, final int numCol, final boolean setToIdentity) {
        ensureValidSize(numRow, numCol);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        elements = new double[numRow * numCol];
        if (setToIdentity) {
            for (int i=0; i<elements.length; i += numCol+1) {
                elements[i] = 1;
            }
        }
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
    public GeneralMatrix(final int numRow, final int numCol, final double[] elements) {
        ensureValidSize(numRow, numCol);
        ensureLengthMatch(numRow*numCol, elements);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        this.elements = elements.clone();
    }

    /**
     * Constructs a new matrix and copies the initial values from the given matrix.
     *
     * @param matrix The matrix to copy.
     */
    public GeneralMatrix(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        ensureValidSize(numRow, numCol);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        elements = new double[numRow * numCol];
        for (int k=0,j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                elements[k++] = matrix.getElement(j, i);
            }
        }
    }

    /**
     * Creates a clone of the given matrix, for {@link #clone()} usage only.
     */
    GeneralMatrix(final GeneralMatrix matrix) {
        numRow   = matrix.numRow;
        numCol   = matrix.numCol;
        elements = matrix.elements.clone();
    }

    /**
     * Ensures that the given matrix size is valid for this {@code GeneralMatrix} implementation.
     */
    private static void ensureValidSize(final int numRow, final int numCol) {
        ArgumentChecks.ensureBetween("numRow", 1, Short.MAX_VALUE, numRow);
        ArgumentChecks.ensureBetween("numCol", 1, Short.MAX_VALUE, numCol);
    }

    /**
     * Returns the number of rows in this matrix.
     */
    @Override
    public final int getNumRow() {
        return numRow;
    }

    /**
     * Returns the number of columns in this matrix.
     */
    @Override
    public final int getNumCol() {
        return numCol;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     *
     * @param row    The row index, from 0 inclusive to {@link #getNumRow() } exclusive.
     * @param column The column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return       The current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            return elements[row * numCol + column];
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     *
     * @param row    The row index, from 0 inclusive to {@link #getNumRow() } exclusive.
     * @param column The column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @param value  The new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            elements[row * numCol + column] = value;
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final double[] getElements() {
        return elements.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(this.elements.length, elements);
        System.arraycopy(elements, 0, this.elements, 0, elements.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        if (numRow == numCol) {
            int i = elements.length;
            if (elements[--i] == 1) {
                final int base = (numRow - 1) * numCol;
                while (--i >= base) {
                    if (elements[i] != 0) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        if (numRow != numCol) {
            return false;
        }
        int di = 0; // Index of next diagonal element.
        for (int i=0; i<elements.length; i++) {
            final double element = elements[i];
            if (i == di) {
                if (element != 1) return false;
                di += numCol + 1;
            } else {
                if (element != 0) return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setToIdentity() {
        Arrays.fill(elements, 0);
        for (int i=0; i<elements.length; i += numCol+1) {
            elements[i] = 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void negate() {
        for (int i=0; i<elements.length; i++) {
            elements[i] = -elements[i];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void transpose() {
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<j; i++) {
                final int lowerLeft  = j*numCol + i;
                final int upperRight = i*numCol + j;
                final double swap = elements[lowerLeft];
                elements[lowerLeft] = elements[upperRight];
                elements[upperRight] = swap;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void normalizeColumns() {
        final double[] column = new double[numRow];
        for (int i=0; i<numCol; i++) {
            for (int j=0; j<numRow; j++) {
                column[j] = elements[j*numCol + i];
            }
            final double m = MathFunctions.magnitude(column);
            for (int j=0; j<numRow; j++) {
                elements[j*numCol + i] /= m;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS inverse() throws SingularMatrixException {
        throw new UnsupportedOperationException(); // TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS multiply(final Matrix matrix) {
        final int nc = matrix.getNumCol();
        if (matrix.getNumRow() != numCol) {
            throw new MismatchedMatrixSizeException(Errors.format(
                    Errors.Keys.MismatchedMatrixSize_4, numCol, nc, matrix.getNumRow(), nc));
        }
        final MatrixSIS result = Matrices.createZero(numRow, nc);
        for (int j=0; j<numRow; j++) {
            final int srcOff = j * numCol;
            for (int i=0; i<nc; i++) {
                double sum = 0;
                for (int k=0; k<numCol; k++) {
                    sum += elements[srcOff + k] * matrix.getElement(k, i);
                }
                result.setElement(j, i, sum);
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the specified object is of type {@code GeneralMatrix} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param object The object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof GeneralMatrix) {
            final GeneralMatrix that = (GeneralMatrix) object;
            return numRow == that.numRow &&
                   numCol == that.numCol &&
                   Arrays.equals(elements, that.elements);
        }
        return false;
    }

    /**
     * Returns a hash code value based on the data values in this object.
     */
    @Override
    public int hashCode() {
        return ((numRow << Short.SIZE) | numCol) ^ Arrays.hashCode(elements) ^ (int) serialVersionUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS clone() {
        return new GeneralMatrix(this);
    }
}
