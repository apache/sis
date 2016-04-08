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
import java.io.Serializable;
import java.awt.geom.AffineTransform;   // For javadoc
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link Matrix} able to perform some operations of interest to Spatial Information Systems (SIS).
 * This class completes the GeoAPI {@link Matrix} interface with some operations used by {@code sis-referencing}.
 * It is not a {@code MatrixSIS} goal to provide all possible Matrix operations, as there is too many of them.
 * This class focuses on:
 *
 * <ul>
 *   <li>Only the basic matrix operations needed for <cite>referencing by coordinates</cite>:
 *     <ul>
 *       <li>{@link #isIdentity()}</li>
 *       <li>{@link #multiply(Matrix)}</li>
 *       <li>{@link #inverse()}</li>
 *       <li>{@link #transpose()}</li>
 *     </ul>
 *   </li><li>Other operations which are not general-purpose matrix operations,
 *     but are needed in the context of referencing by coordinates:
 *     <ul>
 *       <li>{@link #isAffine()}</li>
 *       <li>{@link #normalizeColumns()}</li>
 *       <li>{@link #convertBefore(int, Number, Number)}</li>
 *       <li>{@link #convertAfter(int, Number, Number)}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see Matrices
 */
public abstract class MatrixSIS implements Matrix, LenientComparable, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3075280376118406219L;

    /**
     * For sub-class constructors.
     */
    protected MatrixSIS() {
    }

    /**
     * Ensures that the given array is non-null and has the expected length.
     * This is a convenience method for subclasses constructors.
     *
     * @throws IllegalArgumentException If the given array does not have the expected length.
     */
    static void ensureLengthMatch(final int expected, final double[] elements) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("elements", elements);
        if (elements.length != expected) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, expected, elements.length));
        }
    }

    /**
     * Ensures that the given matrix has the given dimension.
     * This is a convenience method for subclasses.
     */
    static void ensureSizeMatch(final int numRow, final int numCol, final Matrix matrix)
            throws MismatchedMatrixSizeException
    {
        final int othRow = matrix.getNumRow();
        final int othCol = matrix.getNumCol();
        if (numRow != othRow || numCol != othCol) {
            throw new MismatchedMatrixSizeException(Errors.format(
                    Errors.Keys.MismatchedMatrixSize_4, numRow, numCol, othRow, othCol));
        }
    }

    /**
     * Ensures that the number of rows of a given matrix matches the given value.
     * This is a convenience method for {@link #multiply(Matrix)} implementations.
     *
     * @param expected The expected number of rows.
     * @param actual   The actual number of rows in the matrix to verify.
     * @param numCol   The number of columns to report in case of errors. This is an arbitrary
     *                 value and have no incidence on the verification performed by this method.
     */
    static void ensureNumRowMatch(final int expected, final int actual, final int numCol) {
        if (actual != expected) {
            throw new MismatchedMatrixSizeException(Errors.format(
                    Errors.Keys.MismatchedMatrixSize_4, expected, "⒩", actual, numCol));
        }
    }

    /**
     * Returns an exception for the given indices.
     */
    static IndexOutOfBoundsException indexOutOfBounds(final int row, final int column) {
        return new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndicesOutOfBounds_2, row, column));
    }

    /**
     * Casts or copies the given matrix to a SIS implementation. If {@code matrix} is already
     * an instance of {@code MatrixSIS}, then it is returned unchanged. Otherwise all elements
     * are copied in a new {@code MatrixSIS} object.
     *
     * @param  matrix The matrix to cast or copy, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     *
     * @see Matrices#copy(Matrix)
     */
    public static MatrixSIS castOrCopy(final Matrix matrix) {
        if (matrix == null || matrix instanceof MatrixSIS) {
            return (MatrixSIS) matrix;
        }
        return Matrices.copy(matrix);
    }

    /**
     * Stores the value at the specified row and column in the given {@code dd} object.
     * This method does not need to verify argument validity.
     */
    void get(final int row, final int column, final DoubleDouble dd) {
        dd.value = getElement(row, column);
        dd.error = DoubleDouble.errorForWellKnownValue(dd.value);
    }

    /**
     * Stores the value of the given {@code dd} object at the specified row and column.
     * This method does not need to verify argument validity.
     *
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     */
    void set(final int row, final int column, final DoubleDouble dd) {
        setElement(row, column, dd.value);
    }

    /**
     * Retrieves the value at the specified row and column of this matrix, wrapped in a {@code Number}.
     * The {@code Number} type depends on the matrix accuracy.
     *
     * @param row    The row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param column The column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return       The current value at the given row and column.
     */
    public Number getNumber(int row, int column) {
        return getElement(row, column);
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     *
     * @param row    The row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param column The column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return       The current value at the given row and column.
     */
    @Override
    public abstract double getElement(int row, int column);

    /**
     * Returns a copy of all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is <code>{@linkplain #getNumRow()} * {@linkplain #getNumCol()}</code>.
     *
     * @return A copy of all current matrix elements in a row-major array.
     */
    public double[] getElements() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = new double[numRow * numCol];
        for (int k=0,j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                elements[k++] = getElement(j, i);
            }
        }
        return elements;
    }

    /**
     * Stores all matrix elements in the given flat array. This method does not verify the array length.
     * All subclasses in this {@code org.apache.sis.referencing.operation.matrix} package override this
     * method with a more efficient implementation.
     *
     * @param dest The destination array. May be longer than necessary (this happen when the caller needs to
     *             append {@link org.apache.sis.internal.util.DoubleDouble#error} values after the elements).
     */
    void getElements(final double[] dest) {
        final double[] elements = getElements();
        System.arraycopy(elements, 0, dest, 0, elements.length);
    }

    /**
     * Copies the elements of the given matrix in the given array.
     * This method ignores the error terms, if any.
     *
     * @param matrix   The matrix to copy.
     * @param numRow   {@code matrix.getNumRow()}.
     * @param numCol   {@code matrix.getNumCol()}.
     * @param elements Where to copy the elements.
     */
    static void getElements(final Matrix matrix, final int numRow, final int numCol, final double[] elements) {
        if (matrix instanceof MatrixSIS) {
            ((MatrixSIS) matrix).getElements(elements);
        } else {
            for (int k=0,j=0; j<numRow; j++) {
                for (int i=0; i<numCol; i++) {
                    elements[k++] = matrix.getElement(j, i);
                }
            }
        }
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be <code>{@linkplain #getNumRow()} * {@linkplain #getNumCol()}</code>.
     *
     * @param elements The new matrix elements in a row-major array.
     * @throws IllegalArgumentException if the given array does not have the expected length.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see Matrices#create(int, int, double[])
     */
    public abstract void setElements(final double[] elements);

    /**
     * Sets elements in a sub-region of this matrix, optionally including error terms.
     *
     * @param source    Row-major values as given by {@link ExtendedPrecisionMatrix#getExtendedElements()}.
     * @param length    Number of elements ({@code numRow} × {@code numCol}) in the source matrix, not including error terms.
     * @param stride    Number of columns in the source matrix, used for computing indices in {@code source} array.
     * @param srcRow    Index of the first row from the {@code source} to copy in {@code this}.
     * @param srcCol    Index of the first column from the {@code source} to copy in {@code this}.
     * @param dstRow    Index of the first row in {@code this} where to copy the {@code source} values.
     * @param dstCol    Index of the first column in {@code this} where to copy the {@code source} values.
     * @param numRow    Number of rows to copy.
     * @param numCol    Number of columns to copy.
     * @param transfer  If both {@code source} and {@code this} use extended precision,
     *                  the temporary object to use for transferring values. Otherwise {@code null}.
     */
    final void setElements(final double[] source, final int length, final int stride, final DoubleDouble transfer,
                           int srcRow, final int srcCol,
                           int dstRow, final int dstCol,
                           int numRow, final int numCol)
    {
        while (--numRow >= 0) {
            final int valueOffset = srcRow*stride + srcCol;
            for (int i=0; i<numCol; i++) {
                if (transfer != null) {
                    transfer.setFrom(source, valueOffset + i, length);
                    set(dstRow, dstCol + i, transfer);
                } else {
                    setElement(dstRow, dstCol + i, source[valueOffset + i]);
                }
            }
            srcRow++;
            dstRow++;
        }
    }

    /**
     * Sets this matrix to the values of another matrix.
     * The given matrix must have the same size.
     *
     * @param matrix  The matrix to copy.
     * @throws MismatchedMatrixSizeException if the given matrix has a different size than this matrix.
     *
     * @since 0.7
     */
    public void setMatrix(final Matrix matrix) throws MismatchedMatrixSizeException {
        ArgumentChecks.ensureNonNull("matrix", matrix);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        ensureSizeMatch(numRow, numCol, matrix);
        final int count = numRow * numCol;
        final double[] elements;
        /*
         * If both matrices use extended precision, the elements array will have twice the expected length
         * with the matrix values in the first half and the error terms in the second half.  If we want to
         * preserve the extended precision, we have to transfer the values between the two matrices with a
         * DoubleDouble object.
         */
        if (isExtendedPrecision() && matrix instanceof ExtendedPrecisionMatrix) {
            elements = ((ExtendedPrecisionMatrix) matrix).getExtendedElements();
            if (elements.length > count) {
                final DoubleDouble t = new DoubleDouble();
                for (int i=0; i<count; i++) {
                    t.value = elements[i];
                    t.error = elements[i + count];
                    set(i / numCol, i % numCol, t);
                }
                return;
            }
        } else {
            // Fallback for matrices that do not use extended precision.
            elements = new double[count];
            getElements(matrix, numRow, numCol, elements);
        }
        setElements(elements);
    }

    /**
     * Returns {@code true} if this matrix uses extended precision.
     */
    boolean isExtendedPrecision() {
        return false;
    }

    /**
     * Returns {@code true} if this matrix represents an affine transform.
     * A transform is affine if the matrix is square and its last row contains
     * only zeros, except in the last column which contains 1.
     *
     * @return {@code true} if this matrix represents an affine transform.
     *
     * @see Matrices#isAffine(Matrix)
     * @see org.apache.sis.referencing.operation.transform.LinearTransform#isAffine()
     */
    public boolean isAffine() {
        return isAffine(this);
    }

    /**
     * Fallback for matrix of unknown implementation.
     */
    static boolean isAffine(final Matrix matrix) {
        int j = matrix.getNumRow();
        int i = matrix.getNumCol();
        if (i != j--) {
            return false; // Matrix is not square.
        }
        double e = 1;
        while (--i >= 0) {
            if (matrix.getElement(j, i) != e) {
                return false;
            }
            e = 0;
        }
        return true;
    }

    /**
     * Returns {@code true} if this matrix is an identity matrix.
     * This method is equivalent to the following code, except that it is potentially more efficient:
     *
     * {@preformat java
     *     return Matrices.isIdentity(this, 0.0);
     * }
     *
     * @return {@code true} if this matrix is an identity matrix.
     *
     * @see Matrices#isIdentity(Matrix, double)
     * @see AffineTransform#isIdentity()
     */
    @Override
    public abstract boolean isIdentity();

    /**
     * Sets the value of this matrix to its transpose.
     *
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     */
    public abstract void transpose();

    /**
     * Normalizes all columns in-place. Each columns in this matrix is considered as a vector.
     * For each column (vector), this method computes the magnitude (vector length) as the square
     * root of the sum of all square values. Then, all values in the column are divided by that
     * magnitude.
     *
     * <p>This method is useful when the matrix is a
     * {@linkplain org.opengis.referencing.operation.MathTransform#derivative transform derivative}.
     * In such matrix, each column is a vector representing the displacement in target space when an
     * ordinate in the source space is increased by one. Invoking this method turns those vectors
     * into unitary vectors, which is useful for forming the basis of a new coordinate system.</p>
     *
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     */
    public void normalizeColumns() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final DoubleDouble sum = new DoubleDouble();
        final DoubleDouble dot = new DoubleDouble();
        final DoubleDouble tmp = new DoubleDouble();
        for (int i=0; i<numCol; i++) {
            sum.clear();
            for (int j=0; j<numRow; j++) {
                get(j, i, dot);
                dot.square();
                sum.add(dot);
            }
            sum.sqrt();
            for (int j=0; j<numRow; j++) {
                get(j, i, tmp);
                dot.setFrom(sum);
                dot.inverseDivide(tmp);
                set(j, i, dot);
            }
        }
    }

    /**
     * Assuming that this matrix represents an affine transform, concatenates a scale and a translation on the
     * given dimension. Converting a point with the resulting matrix is equivalent to first convert the point
     * with {@code ordinates[srcDim] = ordinates[srcDim] * scale + offset}, then apply the original matrix.
     *
     * <div class="section">Equivalence between this method and Java2D {@code AffineTransform} methods</div>
     * If this matrix was an instance of Java2D {@link AffineTransform}, then invoking this method would
     * be equivalent to invoke the following {@code AffineTransform} methods in the order shown below:
     *
     * <table class="sis" summary="Equivalence between this method and AffineTransform methods">
     *   <tr>
     *     <th>{@code MatrixSIS} method</th>
     *     <th class="sep">{@code AffineTransform} methods</th>
     *   </tr><tr>
     *     <td>{@code concatenate(0, scale, offset)}</td>
     *     <td class="sep"><code>at.{@linkplain AffineTransform#translate(double, double) translate}(offset, 0);
     *     at.{@linkplain AffineTransform#scale(double, double) scale}(scale, 1);</code></td>
     *   </tr><tr>
     *     <td class="hsep">{@code concatenate(1, scale, offset)}</td>
     *     <td class="hsep sep"><code>at.{@linkplain AffineTransform#translate(double, double) translate}(0, offset);
     *     at.{@linkplain AffineTransform#scale(double, double) scale}(1, scale);</code></td>
     *   </tr>
     * </table>
     *
     * @param srcDim The dimension of the ordinate to rescale in the source coordinates.
     * @param scale  The amount by which to multiply the source ordinate value before to apply the transform, or {@code null} if none.
     * @param offset The amount by which to translate the source ordinate value before to apply the transform, or {@code null} if none.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see AffineTransform#concatenate(AffineTransform)
     *
     * @since 0.6
     */
    public void convertBefore(final int srcDim, final Number scale, final Number offset) {
        final int lastCol = getNumCol() - 1;
        ArgumentChecks.ensureValidIndex(lastCol, srcDim);
        final DoubleDouble s = new DoubleDouble();
        final DoubleDouble t = new DoubleDouble();
        for (int j = getNumRow(); --j >= 0;) {
            if (offset != null) {
                get(j, srcDim,  s);     // Scale factor
                get(j, lastCol, t);     // Translation factor
                s.multiply(offset);
                t.add(s);
                set(j, lastCol, t);
            }
            if (scale != null) {
                get(j, srcDim, s);      // Scale factor
                s.multiply(scale);
                set(j, srcDim, s);
            }
        }
    }

    /**
     * Assuming that this matrix represents an affine transform, pre-concatenates a scale and a translation on the
     * given dimension. Converting a point with the resulting matrix is equivalent to first convert the point with
     * the original matrix, then convert the result with {@code ordinates[tgtDim] = ordinates[tgtDim] * scale + offset}.
     *
     * @param tgtDim The dimension of the ordinate to rescale in the target coordinates.
     * @param scale  The amount by which to multiply the target ordinate value after this transform, or {@code null} if none.
     * @param offset The amount by which to translate the target ordinate value after this transform, or {@code null} if none.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see AffineTransform#preConcatenate(AffineTransform)
     *
     * @since 0.6
     */
    public void convertAfter(final int tgtDim, final Number scale, final Number offset) {
        final int lastRow = getNumRow() - 1;
        final int lastCol = getNumCol() - 1;
        ArgumentChecks.ensureValidIndex(lastRow, tgtDim);
        final DoubleDouble s = new DoubleDouble();
        if (scale != null) {
            for (int i=lastCol; i>=0; i--) {
                get(tgtDim, i, s);
                s.multiply(scale);
                set(tgtDim, i, s);
            }
        }
        if (offset != null) {
            get(tgtDim, lastCol, s);
            s.add(offset);
            set(tgtDim, lastCol, s);
        }
    }

    /**
     * Returns a new matrix which is the result of multiplying this matrix with the specified one.
     * In other words, returns {@code this} × {@code matrix}.
     *
     * <div class="section">Relationship with coordinate operations</div>
     * In the context of coordinate operations, {@code Matrix.multiply(other)} is equivalent to
     * <code>{@linkplain AffineTransform#concatenate AffineTransform.concatenate}(other)</code>:
     * first transforms by the supplied transform and then transform the result by the original transform.
     *
     * @param  matrix The matrix to multiply to this matrix.
     * @return The result of {@code this} × {@code matrix}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals to the
     *         number of columns in this matrix.
     */
    public MatrixSIS multiply(final Matrix matrix) throws MismatchedMatrixSizeException {
        final int nc = matrix.getNumCol();
        ensureNumRowMatch(getNumCol(), matrix.getNumRow(), nc);
        final GeneralMatrix result = GeneralMatrix.createExtendedPrecision(getNumRow(), nc, false);
        result.setToProduct(this, matrix);
        return result;
    }

    /**
     * Returns the value of <var>U</var> which solves {@code this} × <var>U</var> = {@code matrix}.
     * This is equivalent to first computing the inverse of {@code this}, then multiplying the result
     * by the given matrix.
     *
     * @param  matrix The matrix to solve.
     * @return The <var>U</var> matrix that satisfies {@code this} × <var>U</var> = {@code matrix}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals
     *         to the number of columns in this matrix.
     * @throws NoninvertibleMatrixException if this matrix is not invertible.
     */
    public MatrixSIS solve(final Matrix matrix) throws MismatchedMatrixSizeException, NoninvertibleMatrixException {
        return Solver.solve(this, matrix);
    }

    /**
     * Returns the inverse of this matrix.
     *
     * @return The inverse of this matrix.
     * @throws NoninvertibleMatrixException if this matrix is not invertible.
     *
     * @see AffineTransform#createInverse()
     */
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        return Solver.inverse(this, true);
    }

    /**
     * Returns a new matrix with the same elements than this matrix except for the specified rows.
     * This method is useful for removing a range of <em>target</em> dimensions in an affine transform.
     *
     * @param  lower Index of the first row to remove (inclusive).
     * @param  upper Index after the last row to remove (exclusive).
     * @return A copy of this matrix with the specified rows removed.
     *
     * @since 0.7
     */
    public MatrixSIS removeRows(final int lower, final int upper) {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        ArgumentChecks.ensureValidIndexRange(numRow, lower, upper);
        final DoubleDouble dd = isExtendedPrecision() ? new DoubleDouble() : null;
        final MatrixSIS reduced = Matrices.createZero(numRow - (upper - lower), numCol, dd != null);
        int dest = 0;
        for (int j=0; j<numRow; j++) {
            if (j == lower) {
                j = upper;
                if (j == numRow) break;
            }
            for (int i=0; i<numCol; i++) {
                if (dd != null) {
                    get(j, i, dd);
                    reduced.set(dest, i, dd);
                } else {
                    reduced.setElement(dest, i, getElement(j, i));
                }
            }
            dest++;
        }
        return reduced;
    }

    /**
     * Returns a new matrix with the same elements than this matrix except for the specified columns.
     * This method is useful for removing a range of <em>source</em> dimensions in an affine transform.
     * Coordinates will be converted as if the values in the removed dimensions were zeros.
     *
     * @param  lower  Index of the first column to remove (inclusive).
     * @param  upper  Index after the last column to remove (exclusive).
     * @return A copy of this matrix with the specified columns removed.
     *
     * @since 0.7
     */
    public MatrixSIS removeColumns(final int lower, final int upper) {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        ArgumentChecks.ensureValidIndexRange(numCol, lower, upper);
        final DoubleDouble dd = isExtendedPrecision() ? new DoubleDouble() : null;
        final MatrixSIS reduced = Matrices.createZero(numRow, numCol - (upper - lower), dd != null);
        int dest = 0;
        for (int i=0; i<numCol; i++) {
            if (i == lower) {
                i = upper;
                if (i == numCol) break;
            }
            for (int j=0; j<numRow; j++) {
                if (dd != null) {
                    get(j, i, dd);
                    reduced.set(j, dest, dd);
                } else {
                    reduced.setElement(j, dest, getElement(j, i));
                }
            }
            dest++;
        }
        return reduced;
    }

    /**
     * Returns a hash code value based on the data values in this matrix.
     *
     * @return A hash code value for this matrix.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getElements()) ^ (int) serialVersionUID;
    }

    /**
     * Returns {@code true} if the specified object is of the same class than this matrix and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param object The object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final int numRow = getNumRow();
            final int numCol = getNumCol();
            final MatrixSIS that = (MatrixSIS) object;
            if (that.getNumRow() == numRow && that.getNumCol() == numCol) {
                for (int j=numRow; --j >= 0;) {
                    for (int i=numCol; --i >= 0;) {
                        if (!Numerics.equals(that.getElement(j, i), getElement(j, i))) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Compares the given matrices for equality, using the given absolute tolerance threshold.
     * The given matrix does not need to be the same implementation class than this matrix.
     *
     * <p>The matrix elements are compared as below:</p>
     * <ul>
     *   <li>{@link Double#NaN} values are considered equals to all other NaN values.</li>
     *   <li>Infinite values are considered equal to other infinite values of the same sign.</li>
     *   <li>All other values are considered equal if the absolute value of their difference is
     *       smaller than or equals to the given threshold.</li>
     * </ul>
     *
     * @param matrix    The matrix to compare.
     * @param tolerance The tolerance value.
     * @return {@code true} if this matrix is close enough to the given matrix given the tolerance value.
     *
     * @see Matrices#equals(Matrix, Matrix, double, boolean)
     */
    public boolean equals(final Matrix matrix, final double tolerance) {
        return Matrices.equals(this, matrix, tolerance, false);
    }

    /**
     * Compares this matrix with the given object for equality. To be considered equal, the two
     * objects must meet the following conditions, which depend on the {@code mode} argument:
     *
     * <ul>
     *   <li>{@link ComparisonMode#STRICT STRICT}:
     *       the two matrices must be of the same class, have the same size and the same element values.</li>
     *   <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}:
     *       the two matrices must have the same size and the same element values,
     *       but are not required to be the same implementation class (any {@link Matrix} is okay).</li>
     *   <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}: same as {@code BY_CONTRACT}.
     *   <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:
     *       the two matrices must have the same size, but the element values can differ up to some threshold.
     *       The threshold value is determined empirically and may change in any future SIS versions.</li>
     * </ul>
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @see Matrices#equals(Matrix, Matrix, ComparisonMode)
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return (object instanceof Matrix) && Matrices.equals(this, (Matrix) object, mode);
    }

    /**
     * Returns a clone of this matrix.
     *
     * @return A new matrix of the same class and with the same values than this matrix.
     *
     * @see Matrices#copy(Matrix)
     */
    @Override
    public MatrixSIS clone() {
        try {
            return (MatrixSIS) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // Should never happen, since we are cloneable.
        }
    }

    /**
     * Returns a unlocalized string representation of this matrix.
     * For each column, the numbers are aligned on the decimal separator.
     *
     * @see Matrices#toString(Matrix)
     */
    @Override
    public String toString() {
        return Matrices.toString(this);
    }
}
