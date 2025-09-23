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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.system.Configuration;
import org.apache.sis.referencing.internal.Arithmetic;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;


/**
 * A two dimensional array of numbers. Row and column numbering begins with zero.
 * Matrix elements are stored as {@link Number} instances for allowing the use of
 * extended precision when needed.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see Matrices#createDiagonal(int, int)
 */
class GeneralMatrix extends MatrixSIS implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1794420009565835530L;

    /**
     * Threshold value relative to 1 ULP of the greatest magnitude of elements added in a sum.
     * For example, in a sum like {@code A + B + C + D}, if the greatest term in absolute value
     * is {@code C}, then the threshold is <code>Math.ulp(C) * {@value}</code>.  If the sum is
     * lower than that threshold, then the result is assumed to be zero.
     *
     * <p>Note that if we were using {@code double} arithmetic instead of double-double, then all results smaller
     * than {@code Math.ulp(max)} would not be significant. Those cases could be caught by a {@code ZERO_THRESHOLD}
     * value of 1.  On the other hand, if all the extra precision of double-double arithmetic was considered valid,
     * then the {@code ZERO_THRESHOLD} value would be approximately 1E-16.   In reality, the extra digits in our
     * double-double arithmetic were usually guessed rather than provided, and the last digits are also subject to
     * rounding errors anyway. So we put the threshold to some arbitrary mid-value, which may change in any future
     * SIS version according experience gained. As long as the value is smaller than 1, it still more accurate than
     * {@code double} arithmetic anyway.</p>
     *
     * @see org.apache.sis.math.Plane#ZERO_THRESHOLD
     * @see Numerics#COMPARISON_THRESHOLD
     */
    @Configuration
    private static final double ZERO_THRESHOLD = 1E-14;

    /**
     * All matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is <code>{@linkplain #numRow} * {@linkplain #numCol}</code>.
     * Zero values <em>shall</em> be {@code null}, not 0 integer or float values.
     *
     * <p>The use of null elements make easy to identify zero values no matter the value type.
     * Special checks for zero values are needed for example during matrix multiplications,
     * because we need 0 × NaN = 0 instead of NaN.</p>
     *
     * @see ExtendedPrecisionMatrix#isZero(Number)
     * @see Arithmetic#isOne(Number)
     */
    final Number[] elements;

    /**
     * Number of rows and columns.
     * This is non-final only for {@link NonSquareMatrix#transpose()} purpose.
     */
    short numRow, numCol;

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol}.
     * If {@code setToIdentity} is {@code true}, then the elements
     * on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * <p>Do not invoke this constructor directly (except by {@link NonSquareMatrix} constructor) unless
     * the matrix is known to be square. If this is not the case, invoke a factory method instead.</p>
     *
     * @param  numRow         number of rows.
     * @param  numCol         number of columns.
     * @param  setToIdentity  {@code true} for initializing the matrix to the identity matrix,
     *                        or {@code false} for leaving it initialized to zero.
     *
     * @see #createExtendedPrecision(int, int, boolean)
     */
    GeneralMatrix(final int numRow, final int numCol, final boolean setToIdentity) {
        ArgumentChecks.ensureBetween("numRow", 1, Numerics.MAXIMUM_MATRIX_SIZE, numRow);
        ArgumentChecks.ensureBetween("numCol", 1, Numerics.MAXIMUM_MATRIX_SIZE, numCol);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        elements = new Number[numRow * numCol];
        if (setToIdentity) {
            final int stop = Math.min(numRow, numCol) * numCol;
            for (int i=0; i<stop; i += numCol+1) {
                elements[i] = 1;
            }
        }
    }

    /**
     * Constructs a {@code numRow} × {@code numCol} matrix initialized to the values in the {@code elements} array.
     * The array values are copied in one row at a time in row major fashion.
     * The array shall be exactly {@code numRow*numCol} in length.
     * The caller is responsible to check the array length.
     *
     * @param  numRow    number of rows.
     * @param  numCol    number of columns.
     * @param  elements  initial values.
     */
    GeneralMatrix(final int numRow, final int numCol, final Number[] elements) {
        this(numRow, numCol, false);
        assert elements.length == this.elements.length;
        for (int k=0,j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final Number element = elements[k++];
                if (!ExtendedPrecisionMatrix.isZero(element)) {
                    this.elements[j * numCol + i] = element;
                }
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
        assert isValid();
    }

    /**
     * Verifies that this matrix is well-formed. This method verifies that the matrix size is valid,
     * that the {@link #elements} array is non-null and has a valid length, and that all elements are
     * either null or non-zero.
     *
     * @return whether this matrix is well-formed.
     * @throws NullPointerException if the {@link #elements} array is null.
     */
    private boolean isValid() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int numRow = this.numRow, numCol = this.numCol;       // Protection against accidental changes.
        final int length = elements.length;
        int i = numRow * numCol;                // Cannot overflow.
        if ((numRow |  numCol) < 0 || (length != i) ||
            (numRow != numCol) != (this instanceof NonSquareMatrix))
        {
            return false;
        }
        boolean isValid = true;
        while (--i >= 0) {
            final Number element = elements[i];
            isValid &= (element == null) || !ExtendedPrecisionMatrix.isZero(element);
        }
        return isValid;
    }

    /**
     * Creates a new matrix of the given size.
     *
     * @param  numRow         number of rows.
     * @param  numCol         number of columns.
     * @param  setToIdentity  {@code true} for initializing the matrix to the identity matrix,
     *                        or {@code false} for leaving it initialized to zero.
     */
    static GeneralMatrix create(final int numRow, final int numCol, final boolean setToIdentity) {
        if (numRow == numCol) {
            return new GeneralMatrix(numRow, numCol, setToIdentity);
        } else {
            return new NonSquareMatrix(numRow, numCol, setToIdentity);
        }
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
     * Returns a copy of all matrix elements in a flat, row-major array. Zero values <em>shall</em> be null.
     * Callers can write in the returned array if and only if the {@code writable} argument is {@code true}.
     */
    @Override
    public final Number[] getElementAsNumbers(final boolean writable) {
        return writable ? elements.clone() : elements;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     * If the value is zero, then this method returns {@code null}.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column, or {@code null} if the value is zero.
     */
    @Override
    public final Number getElementOrNull(final int row, final int column) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            return elements[row * numCol + column];
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Retrieves the value at the specified row and column of this matrix as a {@code Number}.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column.
     */
    @Override
    public final Number getNumber(final int row, final int column) {
        final Number value = getElementOrNull(row, column);
        return (value != null) ? value : 0;
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method is the converses of {@link #getNumber(int, int)}.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @param  value   the new matrix element value, or {@code null} for zero.
     *
     * @see #setElement(int, int, double)
     */
    @Override
    public final void setNumber(final int row, final int column, Number value) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            if (ExtendedPrecisionMatrix.isZero(value)) {
                value = null;
            }
            elements[row * numCol + column] = value;
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        final Number value = getElementOrNull(row, column);
        return (value != null) ? value.doubleValue() : 0;
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow() } exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @param  value   the new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        setNumber(row, column, value);
    }

    /**
     * Sets this matrix to the values of another matrix. This method overrides the default implementation with a more
     * efficient implementation in the particular case where the other matrix is an instance of {@code GeneralMatrix}.
     *
     * @param  matrix  the matrix to copy.
     * @throws MismatchedMatrixSizeException if the given matrix has a different size than this matrix.
     */
    @Override
    public final void setMatrix(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix instanceof GeneralMatrix) {
            final GeneralMatrix gm = (GeneralMatrix) matrix;
            ensureSizeMatch(numRow, numCol, matrix);
            System.arraycopy(gm.elements, 0, elements, 0, elements.length);
        } else {
            super.setMatrix(matrix);
        }
        assert isValid();
    }

    /**
     * Returns {@code true} if this matrix represents an affine transform.
     */
    @Override
    public final boolean isAffine() {
        return isAffine(true);
    }

    /**
     * Implementation of {@link #isAffine()} with control on whether to require the matrix to be square.
     *
     * @param  square  {@code true} if the matrix must be square, or {@code false} for allowing non-square matrices.
     */
    final boolean isAffine(final boolean square) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int numRow = this.numRow, numCol = this.numCol;       // Protection against accidental changes.
        if (numRow == numCol || !square) {
            int i = numRow * numCol;
            if (Arithmetic.isOne(elements[--i])) {
                final int base = (numRow - 1) * numCol;
                while (--i >= base) {
                    if (elements[i] != null) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this matrix is an identity matrix.
     */
    @Override
    public final boolean isIdentity() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int numRow = this.numRow, numCol = this.numCol;       // Protection against accidental changes.
        if (numRow != numCol) {
            return false;
        }
        int di = 0;                                 // Index of next diagonal element.
        final int length = numRow * numCol;
        for (int i=0; i<length; i++) {
            final Number element = elements[i];
            if (i == di) {
                if (!Arithmetic.isOne(element)) {
                    return false;
                }
                di += numCol + 1;                   // Next index where a 1 value is expected.
            } else if (element != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the value of this matrix to its transpose.
     * The implementation provided by {@code GeneralMatrix} is valid only for square matrix.
     * {@link NonSquareMatrix} must override.
     */
    @Override
    public void transpose() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int numRow = this.numRow, numCol = this.numCol;       // Protection against accidental changes.
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<j; i++) {
                final int lo = j*numCol + i;
                final int up = i*numCol + j;
                ArraysExt.swap(elements, lo, up);
            }
        }
    }

    /**
     * Sets this matrix to the product of the given matrices: {@code this = A × B}.
     * The matrix sizes much match - this is not verified unless assertions are enabled.
     */
    final void setToProduct(final Matrix A, final Matrix B) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int numRow = this.numRow, numCol = this.numCol;       // Protection against accidental changes.
        final int nc = A.getNumCol();
        assert B.getNumRow() == nc;
        assert numRow == A.getNumRow() && numCol == B.getNumCol();
        final ExtendedPrecisionMatrix eA = asExtendedPrecision(A);
        final ExtendedPrecisionMatrix eB = asExtendedPrecision(B);
        /*
         * Compute the product, to be stored directly in `this`.
         * Null value means zero, and we skip it in dot product
         * because we want 0 × NaN = 0 instead of NaN.
         */
        int k = 0;
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                Number sum = null;
                double max = 0;
                for (int m=0; m<nc; m++) {
                    final Number mA = eA.getElementOrNull(j, m);
                    if (mA != null) {
                        final Number mB  = eB.getElementOrNull(m, i);
                        final Number dot = Arithmetic.multiply(mA, mB);
                        if (dot != null) {
                            sum = Arithmetic.add(sum, dot);
                            final double value = Math.abs(dot.doubleValue());
                            if (value > max) max = value;
                        }
                    }
                }
                if (sum != null && Math.abs(sum.doubleValue()) < Math.ulp(max) * ZERO_THRESHOLD) {
                    sum = null;             // Sum is not significant according double arithmetic.
                }
                elements[k++] = sum;
            }
        }
        assert k == elements.length;
        assert isValid();
    }

    /**
     * Returns {@code true} if the specified object is of type {@code GeneralMatrix} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param  object  the object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public final boolean equals(final Object object) {
        if (object instanceof GeneralMatrix) {
            final var that = (GeneralMatrix) object;
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
    public final int hashCode() {
        return ((numRow << Short.SIZE) | numCol) ^ Arrays.hashCode(elements) ^ (int) serialVersionUID;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public MatrixSIS clone() {
        return new GeneralMatrix(this);
    }
}
