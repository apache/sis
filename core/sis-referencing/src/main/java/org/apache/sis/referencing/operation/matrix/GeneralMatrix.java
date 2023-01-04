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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;


/**
 * A two dimensional array of numbers. Row and column numbering begins with zero.
 *
 * <h2>Support for extended precision</h2>
 * This class can optionally support extended precision using the <cite>double-double arithmetic</cite>.
 * In extended precision mode, the {@link #elements} array have twice its normal length. The first half
 * of the array contains the same value than in normal precision mode, while the second half contains
 * the {@link DoubleDouble#error}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see Matrices#createDiagonal(int, int)
 *
 * @since 0.4
 */
class GeneralMatrix extends MatrixSIS implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8447482612423035360L;

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
     * <div class="note"><b>Note:</b>
     * A similar constant exists in {@code org.apache.sis.math.Plane}.
     * </div>
     */
    private static final double ZERO_THRESHOLD = 1E-14;

    /**
     * All matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is <code>{@linkplain #numRow} * {@linkplain #numCol}</code>.
     *
     * <p>In <cite>extended precision mode</cite>, the length of this array is actually twice the above-cited length.
     * The first half contains {@link DoubleDouble#value}, and the second half contains the {@link DoubleDouble#error}
     * for each value in the first half.</p>
     */
    final double[] elements;

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
     * @param  precision      1 for normal precision, or 2 for extended precision.
     *                        No other value is allowed (this is not verified).
     *
     * @see #createExtendedPrecision(int, int, boolean)
     */
    GeneralMatrix(final int numRow, final int numCol, final boolean setToIdentity, final int precision) {
        ensureValidSize(numRow, numCol);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        elements = new double[numRow * numCol * precision];
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
     *
     * @param  numRow    number of rows.
     * @param  numCol    number of columns.
     * @param  elements  initial values.
     */
    GeneralMatrix(final int numRow, final int numCol, final double[] elements) {
        ensureValidSize(numRow, numCol);
        ensureLengthMatch(numRow * numCol, elements);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        this.elements = elements.clone();
        assert isValid();
    }

    /**
     * Constructs a new matrix and copies the initial values from the given matrix.
     *
     * @param  matrix  the matrix to copy.
     */
    GeneralMatrix(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        ensureValidSize(numRow, numCol);
        this.numRow = (short) numRow;
        this.numCol = (short) numCol;
        if (matrix instanceof ExtendedPrecisionMatrix) {
            elements = ((ExtendedPrecisionMatrix) matrix).getExtendedElements();
            assert (elements.length % (numRow * numCol)) == 0;
        } else {
            elements = new double[numRow * numCol];
            getElements(matrix, numRow, numCol, elements);
        }
        assert isValid();
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
     * that the {@link #elements} array is non-null and has a valid length, and that all error terms
     * are smaller than 1 ULP of the corresponding matrix element value.
     *
     * @return whether this matrix is well-formed.
     * @throws NullPointerException if the {@link #elements} array is null.
     * @throws AssertionError (thrown by {@link DoubleDouble#DoubleDouble(double, double)} constructor)
     *         if an error term is not smaller than the corresponding matrix element value.
     */
    private boolean isValid() {
        final int numRow = this.numRow;
        final int numCol = this.numCol;
        final int length = elements.length;
        int i = numRow * numCol;                // Cannot overflow.
        if ((numRow |  numCol) < 0 || (length != i && length != i*2) ||
            (numRow != numCol) != (this instanceof NonSquareMatrix))
        {
            return false;
        }
        boolean isValid = true;
        while (--i >= 0) {
            final double a = getNumber(i / numCol, i % numCol).doubleValue();
            final double b = elements[i];
            isValid &= (a == b) || (Double.isNaN(a) == Double.isNaN(b));
        }
        return isValid;
    }

    /**
     * Creates a new extended precision matrix of the given size.
     *
     * @param  numRow         number of rows.
     * @param  numCol         number of columns.
     * @param  setToIdentity  {@code true} for initializing the matrix to the identity matrix,
     *                        or {@code false} for leaving it initialized to zero.
     */
    static GeneralMatrix createExtendedPrecision(final int numRow, final int numCol, final boolean setToIdentity) {
        if (numRow == numCol) {
            return new GeneralMatrix(numRow, numCol, setToIdentity, 2);
        } else {
            return new NonSquareMatrix(numRow, numCol, setToIdentity, 2);
        }
    }

    /**
     * Infers all {@link DoubleDouble#error} with a default values inferred from {@link DoubleDouble#value}.
     * For example if a matrix element is exactly 3.141592653589793, there is good chances that the user's
     * intent was to specify the {@link Math#PI} value, in which case this method will infer that we would
     * need to add 1.2246467991473532E-16 in order to get a value closer to π.
     */
    static void inferErrors(final double[] elements) {
        final int length = elements.length / 2;
        for (int i=length; i<elements.length; i++) {
            elements[i] = DoubleDouble.errorForWellKnownValue(elements[i - length]);
        }
    }

    /**
     * Returns the index of the first {@link DoubleDouble#error} value in the {@link #elements} array,
     * or 0 if none. This method returns a non-zero value only if the matrix has been created in extended
     * precision mode.
     */
    static int indexOfErrors(final int numRow, final int numCol, final double[] elements) {
        assert elements.length % (numRow * numCol) == 0;
        return (numRow * numCol) % elements.length;         // A % B is for getting 0 without branching if A == B.
    }

    /**
     * Ensures that the given matrix size is valid for this {@code GeneralMatrix} implementation.
     */
    private static void ensureValidSize(final int numRow, final int numCol) {
        ArgumentChecks.ensureBetween("numRow", 1, Numerics.MAXIMUM_MATRIX_SIZE, numRow);
        ArgumentChecks.ensureBetween("numCol", 1, Numerics.MAXIMUM_MATRIX_SIZE, numCol);
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
     * Returns {@code true} if this matrix uses extended precision.
     */
    @Override
    final boolean isExtendedPrecision() {
        return elements.length > numRow * numCol;
    }

    /**
     * Stores the value at the specified row and column in the given {@code dd} object.
     * This method does not need to verify argument validity.
     */
    @Override
    final DoubleDouble getDD(final int row, final int column) {
        int i = row * numCol + column;
        final double value = elements[i];
        i += numRow * numCol;
        if (i < elements.length) {
            final DoubleDouble dd = DoubleDouble.of(value, elements[i]);
            assert dd.equals(getNumber(row, column));
            return dd;
        } else {
            return DoubleDouble.of(value);
        }
    }

    /**
     * Stores the value of the given {@code dd} object at the specified row and column.
     * This method does not need to verify argument validity.
     */
    @Override
    final void set(final int row, final int column, final DoubleDouble dd) {
        final int iv = row * numCol + column;
        final int ie = iv  + numRow * numCol;
        if (ie < elements.length) {
            elements[iv] = dd.value;
            elements[ie] = dd.error;
            assert dd.equals(getNumber(row, column));
        } else {
            elements[iv] = dd.doubleValue();
        }
    }

    /**
     * Retrieves the value at the specified row and column of this matrix, rounded to nearest integer.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column, rounded to nearest integer.
     * @throws ArithmeticException if the value is NaN or overflows integer capacity.
     *
     * @see DoubleDouble#longValue()
     *
     * @since 1.3
     */
    @Override
    public long getInteger(int row, int column) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            int i = row * numCol + column;
            final double value = elements[i];
            long r = Math.round(value);
            i += numRow * numCol;
            if (i < elements.length) {
                r += (long) elements[i];            // Really want rounding toward zero.
            }
            if (Math.abs(r - value) <= 0.5) {
                return r;
            }
            throw new ArithmeticException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, Long.TYPE));
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Retrieves the value at the specified row and column of this matrix, wrapped in a {@code Number}
     * or a {@link DoubleDouble} depending on available precision.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column.
     */
    @Override
    public Number getNumber(int row, int column) {
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            int i = row * numCol + column;
            final double value = elements[i];
            i += numRow * numCol;
            if (i < elements.length) {
                return DoubleDouble.of(value, elements[i]);
            } else {
                return value;
            }
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
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            return elements[row * numCol + column];
        } else {
            throw indexOutOfBounds(row, column);
        }
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
        if (row >= 0 && row < numRow && column >= 0 && column < numCol) {
            int i = row * numCol + column;
            elements[i] = value;
            i += numRow * numCol;
            if (i < elements.length) {
                elements[i] = DoubleDouble.errorForWellKnownValue(value);
            }
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * Returns all elements of the given matrix followed by the error terms for extended-precision arithmetic.
     * The array will have twice the normal length. See {@link #elements} for more discussion.
     *
     * <p>This method may return a direct reference to the internal array. <strong>Do not modify.</strong>,
     * unless the {@code copy} argument is {@code true}.</p>
     *
     * @param  copy  if {@code true}, then the returned array is guaranteed to be a copy, never the internal array.
     */
    static double[] getExtendedElements(final Matrix matrix, final int numRow, final int numCol, final boolean copy) {
        double[] elements;
        final int length = numRow * numCol * 2;
        if (matrix instanceof GeneralMatrix) {
            elements = ((GeneralMatrix) matrix).elements;
            if (elements.length == length) {
                if (copy) {
                    elements = elements.clone();
                }
                return elements;                                // Internal array already uses extended precision.
            } else {
                elements = Arrays.copyOf(elements, length);
            }
        } else if (matrix instanceof ExtendedPrecisionMatrix) {
            elements = ((ExtendedPrecisionMatrix) matrix).getExtendedElements();
            if (elements.length == length) {
                return elements;
            } else {
                elements = Arrays.copyOf(elements, length);
            }
        } else {
            elements = new double[length];
            getElements(matrix, numRow, numCol, elements);
        }
        inferErrors(elements);
        return elements;
    }

    /**
     * Returns a copy of all matrix elements, potentially followed by the error terms for extended-precision arithmetic.
     * Matrix elements are returned in a flat, row-major (column indices vary fastest) array.
     */
    @Override
    public final double[] getExtendedElements() {
        return elements.clone();
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The returned array does <em>not</em> include error terms used in double-double arithmetic.
     */
    @Override
    public final double[] getElements() {
        return Arrays.copyOf(elements, numRow*numCol);
    }

    /**
     * Copies the matrix elements in the given flat array. This method does not verify the array length,
     * since the destination array may contain room for {@link DoubleDouble#error} terms.
     */
    @Override
    final void getElements(final double[] dest) {
        System.arraycopy(elements, 0, dest, 0, numRow*numCol);
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The given array shall not contain error terms. The error terms will be set to default values.
     */
    @Override
    public final void setElements(final double[] newValues) {
        ensureLengthMatch(numRow*numCol, newValues);
        System.arraycopy(newValues, 0, elements, 0, newValues.length);
        if (elements.length != newValues.length) {
            inferErrors(elements);
        }
        assert isValid();
    }

    /**
     * Sets all matrix elements like {@link #setElements(double[])}, but from an array of {@code Number} instead
     * of {@code double}. The main purpose of this method is to fetch the {@link DoubleDouble#error} terms when
     * such instances are found.
     *
     * <h4>Restrictions</h4>
     * <ul>
     *   <li>This matrix must use extended-precision elements as by {@link #createExtendedPrecision(int, int, boolean)}.</li>
     *   <li>If this method returns {@code false}, then error terms are <strong>not</strong> initialized - they
     *       may have any values.</li>
     * </ul>
     *
     * @param  newValues  the new matrix elements in a row-major array.
     * @return {@code true} if at least one {@link DoubleDouble} instance has been found, in which case all
     *         errors terms have been initialized, or {@code false} otherwise, in which case no error term
     *         has been initialized (this is a <cite>all or nothing</cite> operation).
     * @throws IllegalArgumentException if the given array does not have the expected length.
     *
     * @see Matrices#create(int, int, Number[])
     */
    final boolean setElements(final Number[] newValues) {
        final int numRow = this.numRow;                         // Protection against accidental changes.
        final int numCol = this.numCol;
        final int length = numRow * numCol;
        if (newValues.length != length) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, length, newValues.length));
        }
        boolean isExtended = false;
        for (int i=0; i<length; i++) {
            final DoubleDouble value = DoubleDouble.of(newValues[i]);
            final double element = value.doubleValue();
            elements[i] = element;
            final double error;
            if (value.error != 0) {
                error = value.error;
                /*
                 * If this is the first time that we found an explicit error term, then we need to
                 * initialize all elements before the current one because they were left unitialized
                 * (i.e. we perform lazy initialization).
                 */
                if (!isExtended) {
                    isExtended = true;
                    for (int j=0; j<i; j++) {
                        elements[j + length] = DoubleDouble.errorForWellKnownValue(elements[j]);
                    }
                }
            } else {
                /*
                 * For any kind of numbers other than DoubleDoube, calculate the error term only if we know
                 * that the final matrix will use extended precision (i.e. we previously found at least one
                 * DoubleDouble instance). Otherwise skip the error calculation since maybe it will be discarded.
                 */
                if (!isExtended) {
                    continue;
                }
                error = DoubleDouble.errorForWellKnownValue(element);
            }
            elements[i + length] = error;
        }
        if (isExtended) {
            assert isValid();
        }
        return isExtended;
    }

    /**
     * Sets this matrix to the values of another matrix. This method overrides the default implementation with a more
     * efficient implementation in the particular case where the other matrix is an instance of {@code GeneralMatrix}.
     *
     * @param  matrix  the matrix to copy.
     * @throws MismatchedMatrixSizeException if the given matrix has a different size than this matrix.
     *
     * @since 0.7
     */
    @Override
    public void setMatrix(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix instanceof GeneralMatrix) {
            final GeneralMatrix gm = (GeneralMatrix) matrix;
            ensureSizeMatch(numRow, numCol, matrix);
            final int length = gm.elements.length;
            if (elements.length <= length) {
                System.arraycopy(gm.elements, 0, elements, 0, elements.length);
            } else {
                System.arraycopy(gm.elements, 0, elements, 0, length);
                inferErrors(elements);
            }
        } else {
            super.setMatrix(matrix);
        }
        assert isValid();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method does not check the error terms, because those terms are not visible to the user
     * (they cannot appear in the value returned by {@link #getElement(int, int)}, and are not shown
     * by {@link #toString()}) - returning {@code false} while the matrix clearly looks like affine
     * would be confusing for the user. Furthermore, the errors can be non-zero only in the very last
     * element and that value always smaller than 2.3E-16.</p>
     */
    @Override
    public final boolean isAffine() {
        return isAffine(true);
    }

    /**
     * Implementation of {@link #isAffine()} with control on whether we require the matrix to be square.
     *
     * @param  square  {@code true} if the matrix must be square, or {@code false} for allowing non-square matrices.
     */
    final boolean isAffine(final boolean square) {
        final int numRow = this.numRow;                     // Protection against accidental changes.
        final int numCol = this.numCol;
        if (numRow == numCol || !square) {
            int i = numRow * numCol;
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
     *
     * <p>This method does not check the error terms, because those terms are not visible to the user
     * (they cannot appear in the value returned by {@link #getElement(int, int)}, and are not shown
     * by {@link #toString()}) - returning {@code false} while the matrix clearly looks like identity
     * would be confusing for the user. Furthermore, the errors can be non-zero only on the diagonal,
     * and those values always smaller than 2.3E-16.</p>
     *
     * <p>Another argument is that the extended precision is for reducing rounding errors during
     * matrix arithmetic. But since the user provided the original data as {@code double} values,
     * the extra precision usually have no "real" meaning.</p>
     */
    @Override
    public final boolean isIdentity() {
        final int numRow = this.numRow;                     // Protection against accidental changes.
        final int numCol = this.numCol;
        if (numRow != numCol) {
            return false;
        }
        int di = 0;                                         // Index of next diagonal element.
        final int length = numRow * numCol;
        for (int i=0; i<length; i++) {
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
     *
     * The implementation provided by {@code GeneralMatrix} is valid only for square matrix.
     * {@link NonSquareMatrix} must override.
     */
    @Override
    public void transpose() {
        final int numRow = this.numRow;                                 // Protection against accidental changes.
        final int numCol = this.numCol;
        final int errors = indexOfErrors(numRow, numCol, elements);     // Where error values start, or 0 if none.
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<j; i++) {
                final int lo = j*numCol + i;
                final int up = i*numCol + j;
                ArraysExt.swap(elements, lo, up);
                if (errors != 0) {
                    // Swap also the error terms in extended precision mode.
                    ArraysExt.swap(elements, lo + errors, up + errors);
                }
            }
        }
    }

    /**
     * Sets this matrix to the product of the given matrices: {@code this = A × B}.
     * The matrix sizes much match - this is not verified unless assertions are enabled.
     */
    final void setToProduct(final Matrix A, final Matrix B) {
        final int numRow = this.numRow;         // Protection against accidental changes.
        final int numCol = this.numCol;
        final int nc = A.getNumCol();
        assert B.getNumRow() == nc;
        assert numRow == A.getNumRow() && numCol == B.getNumCol();
        /*
         * Get the matrix element values, together with the error terms if the matrix
         * use extended precision (double-double arithmetic).
         */
        final double[] eltA   = getExtendedElements(A, numRow, nc, false);
        final double[] eltB   = getExtendedElements(B, nc, numCol, false);
        final int errorOffset = numRow * numCol;            // Where error terms start.
        final int errA        = numRow * nc;
        final int errB        = nc * numCol;
        /*
         * Compute the product, to be stored directly in 'this'.
         */
        for (int k=0,j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                var sum = DoubleDouble.ZERO;
                double max = 0;
                int iB = i;                                 // Index of values in a single column of B.
                int iA = j * nc;                            // Index of values in a single row of A.
                final int nextRow = iA + nc;
                while (iA < nextRow) {
                    var dot = DoubleDouble.ofPair(eltA, iA, errA);
                    if (!dot.isZero()) {                    // For avoiding multiplication with NaN values.
                        final var vB = DoubleDouble.ofPair(eltB, iB, errB);
                        if (!vB.isZero()) {                 // For avoiding multiplication with NaN values.
                            dot = dot.multiply(vB);
                            sum = sum.add(dot);
                            final double value = Math.abs(dot.value);
                            if (value > max) max = value;
                        }
                    }
                    iB += numCol;                           // Move to next row of B.
                    iA++;                                   // Move to next column of A.
                }
                if (Math.abs(sum.value) < Math.ulp(max) * ZERO_THRESHOLD) {
                    sum = DoubleDouble.ZERO;                // Sum is not significant according double arithmetic.
                }
                sum.storeTo(elements, k++, errorOffset);
            }
        }
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
    public final int hashCode() {
        return ((numRow << Short.SIZE) | numCol) ^ Arrays.hashCode(elements) ^ (int) serialVersionUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public MatrixSIS clone() {
        return new GeneralMatrix(this);
    }
}
