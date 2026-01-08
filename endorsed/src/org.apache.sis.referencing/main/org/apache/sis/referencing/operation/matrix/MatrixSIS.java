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
import java.util.Objects;
import java.io.Serializable;
import java.awt.geom.AffineTransform;                       // For javadoc
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.internal.Arithmetic;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link Matrix} able to perform some operations of interest to Spatial Information Systems (SIS).
 * This class completes the GeoAPI {@link Matrix} interface with some operations used by {@code org.apache.sis.referencing}.
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
 * @version 1.5
 *
 * @see Matrices
 *
 * @since 0.4
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
     * @throws IllegalArgumentException if the given array does not have the expected length.
     */
    static void ensureLengthMatch(final int expected, final double[] elements) throws IllegalArgumentException {
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
     * @param  expected  the expected number of rows.
     * @param  actual    the actual number of rows in the matrix to verify.
     * @param  numCol    the number of columns to report in case of errors. This is an arbitrary
     *                   value and have no incidence on the verification performed by this method.
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
     * @param  matrix  the matrix to cast or copy, or {@code null}.
     * @return the matrix argument if it can be safely cast (including {@code null} argument),
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
     * Returns the given matrix as an extended precision matrix.
     *
     * @see ExtendedPrecisionMatrix#castOrWrap(Matrix)
     */
    static ExtendedPrecisionMatrix asExtendedPrecision(final Matrix matrix) {
        if (matrix instanceof UnmodifiableMatrix) {
            return ((UnmodifiableMatrix) matrix).asExtendePrecision();
        } else if (matrix instanceof ExtendedPrecisionMatrix) {
            return (ExtendedPrecisionMatrix) matrix;
        } else {
            return new UnmodifiableMatrix(matrix);
        }
    }

    /**
     * Retrieves the value at the specified row and column if different than zero.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     * The use of {@code null} for zero is a way to identify zero easily no matter
     * the value type.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column, or {@code null} if the value is zero.
     * @throws IndexOutOfBoundsException if the specified row or column is out of bounds.
     */
    Number getElementOrNull(final int row, final int column) {
        final double value = getElement(row, column);
        return (value == 0) ? null : value;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix, wrapped in a {@code Number}.
     * The {@code Number} type depends on the matrix accuracy.
     *
     * <h4>Use case</h4>
     * This method may be more accurate than {@link #getElement(int, int)} in some implementations
     * when the value is expected to be an integer, for example in conversions of pixel coordinates.
     * {@link Number#longValue()} can be more accurate than {@link Number#doubleValue()} because a
     * {@code long} may have more significant digits than what a {@code double} can contain.
     * For safety against rounding errors and overflows,
     * {@link Numbers#round(Number)} should be used instead of {@code Number.longValue()}.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @return the current value at the given row and column.
     * @throws IndexOutOfBoundsException if the specified row or column is out of bounds.
     *
     * @see #getElement(int, int)
     * @see Numbers#round(Number)
     */
    public Number getNumber(int row, int column) {
        return getElement(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method is the converses of {@link #getNumber(int, int)}.
     *
     * @param  row     the row index, from 0 inclusive to {@link #getNumRow()} exclusive.
     * @param  column  the column index, from 0 inclusive to {@link #getNumCol()} exclusive.
     * @param  value   the new matrix element value. A {@code null} value is interpreted as zero.
     * @throws IndexOutOfBoundsException if the specified row or column is out of bounds.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see #setElement(int, int, double)
     *
     * @since 0.8
     */
    public void setNumber(int row, int column, final Number value) {
        setElement(row, column, (value != null) ? value.doubleValue() : 0);
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is <code>{@linkplain #getNumRow()} * {@linkplain #getNumCol()}</code>.
     *
     * @return a copy of all current matrix elements in a row-major array.
     */
    public double[] getElements() {
        final int numCol = getNumCol();
        final double[] elements = new double[getNumRow() * numCol];
        for (int i=0; i<elements.length; i++) {
            elements[i] = getElement(i / numCol, i % numCol);
        }
        return elements;
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
    public void setElements(final double[] elements) {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        ensureLengthMatch(numRow * numCol, elements);
        for (int k=0,j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                setElement(j, i, elements[k++]);
            }
        }
    }

    /**
     * Sets elements in a sub-region of this matrix.
     *
     * @param  source    the matrix to copy.
     * @param  srcRow    index of the first row from the {@code source} to copy in {@code this}.
     * @param  srcCol    index of the first column from the {@code source} to copy in {@code this}.
     * @param  dstRow    index of the first row in {@code this} where to copy the {@code source} values.
     * @param  dstCol    index of the first column in {@code this} where to copy the {@code source} values.
     * @param  numRow    number of rows to copy.
     * @param  numCol    number of columns to copy.
     */
    final void setElements(final Matrix source,
                           int srcRow, final int srcCol,
                           int dstRow, final int dstCol,
                           int numRow, final int numCol)
    {
        final var exp = asExtendedPrecision(source);
        while (--numRow >= 0) {
            for (int i=0; i<numCol; i++) {
                final int s = srcCol + i;
                final int t = dstCol + i;
                final Number n = exp.getElementOrNull(srcRow, s);
                if (n != null) {
                    setNumber(dstRow, t, n);
                } else {
                    setElement(dstRow, t, source.getElement(srcRow, s));    // Preserve the sign of 0.
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
     * @param  source  the matrix to copy.
     * @throws MismatchedMatrixSizeException if the given matrix has a different size than this matrix.
     *
     * @since 0.7
     */
    public void setMatrix(final Matrix source) throws MismatchedMatrixSizeException {
        Objects.requireNonNull(source);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        ensureSizeMatch(numRow, numCol, source);
        setElements(source, 0, 0, 0, 0, numRow, numCol);
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
            return false;       // Matrix is not square.
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
     * {@snippet lang="java" :
     *     return Matrices.isIdentity(this, 0.0);
     *     }
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
     * Normalizes all columns in-place and returns their magnitudes as a row vector.
     * Each columns in this matrix is considered as a vector. For each column (vector),
     * this method computes the magnitude (vector length) as the square root of the sum of all squared values.
     * Then, all values in the column are divided by that magnitude.
     *
     * <p>This method is useful when the matrix is a
     * {@linkplain org.opengis.referencing.operation.MathTransform#derivative transform derivative}.
     * In such matrix, each column is a vector representing the displacement in target space when an
     * coordinate in the source space is increased by one. Invoking this method turns those vectors
     * into unitary vectors, which is useful for forming the basis of a new coordinate system.</p>
     *
     * @return the magnitude for each column in a matrix having only one row.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     */
    public MatrixSIS normalizeColumns() {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS magnitudes = new NonSquareMatrix(1, numCol, false);
        for (int i=0; i<numCol; i++) {
            Number sum = null;
            for (int j=0; j<numRow; j++) {
                final Number element = getElementOrNull(j, i);
                sum = Arithmetic.add(sum, Arithmetic.square(element));
            }
            sum = Arithmetic.sqrt(sum);
            if (sum != null) {
                int rowOfOne = -1;
                for (int j=0; j<numRow; j++) {
                    final Number element = getElementOrNull(j, i);
                    final Number dot = Arithmetic.divide(element, sum);
                    setNumber(j, i, dot);
                    if (dot != null && Math.abs(dot.doubleValue()) >= 1) {
                        rowOfOne = j;
                    }
                }
                /*
                 * If a value is exactly 1, then all other values should be exactly zero.
                 * We observe that the other values are sometimes close to 0.5 ULP of 1.
                 * Forcing those values to 0 can help the caller to apply optimizations.
                 */
                if (rowOfOne >= 0) {
                    for (int j=0; j<numRow; j++) {
                        final double sign = getElement(j, i);
                        setElement(j, i, Math.copySign(j == rowOfOne ? 1 : 0, sign));
                    }
                }
                magnitudes.setNumber(0, i, sum);
            }
        }
        return magnitudes;
    }

    /**
     * Assuming that this matrix represents an affine transform, concatenates a scale and a translation on the
     * given dimension. Converting a point with the resulting matrix is equivalent to first convert the point
     * with {@code coordinates[srcDim] = coordinates[srcDim] * scale + offset}, then apply the original matrix.
     *
     * <h4>Equivalence between this method and Java2D {@code AffineTransform} methods</h4>
     * If this matrix was an instance of Java2D {@link AffineTransform}, then invoking this method would
     * be equivalent to invoking the following {@code AffineTransform} methods in the order shown below:
     *
     * <table class="sis">
     * <caption>Equivalence between this method and AffineTransform methods</caption>
     *   <tr>
     *     <th>{@code MatrixSIS} method</th>
     *     <th class="sep">{@code AffineTransform} methods</th>
     *   </tr><tr>
     *     <td>{@code convertBefore(0, scale, offset)}</td>
     *     <td class="sep"><code>at.{@linkplain AffineTransform#translate(double, double) translate}(offset, 0);
     *     at.{@linkplain AffineTransform#scale(double, double) scale}(scale, 1);</code></td>
     *   </tr><tr>
     *     <td class="hsep">{@code convertBefore(1, scale, offset)}</td>
     *     <td class="hsep sep"><code>at.{@linkplain AffineTransform#translate(double, double) translate}(0, offset);
     *     at.{@linkplain AffineTransform#scale(double, double) scale}(1, scale);</code></td>
     *   </tr>
     * </table>
     *
     * @param  srcDim  the dimension of the coordinate to rescale in the source coordinates.
     * @param  scale   the amount by which to multiply the source coordinate value before to apply the transform, or {@code null} if none.
     * @param  offset  the amount by which to translate the source coordinate value before to apply the transform, or {@code null} if none.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see AffineTransform#concatenate(AffineTransform)
     *
     * @since 0.6
     */
    public void convertBefore(final int srcDim, final Number scale, final Number offset) {
        final int lastCol = getNumCol() - 1;
        Objects.checkIndex(srcDim, lastCol);
        for (int j = getNumRow(); --j >= 0;) {
            if (offset != null) {
                final Number s = getElementOrNull(j, srcDim);           // Scale factor
                final Number t = getElementOrNull(j, lastCol);          // Translation factor
                setNumber(j, lastCol, Arithmetic.add(t, Arithmetic.multiply(s, offset)));
            }
            if (scale != null) {
                final Number s = getElementOrNull(j, srcDim);           // Scale factor
                setNumber(j, srcDim, Arithmetic.multiply(s, scale));
            }
        }
    }

    /**
     * Assuming that this matrix represents an affine transform, pre-concatenates a scale and a translation on the
     * given dimension. Converting a point with the resulting matrix is equivalent to first convert the point with
     * the original matrix, then convert the result with {@code coordinates[tgtDim] = coordinates[tgtDim] * scale + offset}.
     *
     * @param  tgtDim  the dimension of the coordinate to rescale in the target coordinates.
     * @param  scale   the amount by which to multiply the target coordinate value after this transform, or {@code null} if none.
     * @param  offset  the amount by which to translate the target coordinate value after this transform, or {@code null} if none.
     * @throws UnsupportedOperationException if this matrix is unmodifiable.
     *
     * @see AffineTransform#preConcatenate(AffineTransform)
     *
     * @since 0.6
     */
    public void convertAfter(final int tgtDim, final Number scale, final Number offset) {
        final int lastRow = getNumRow() - 1;
        final int lastCol = getNumCol() - 1;
        Objects.checkIndex(tgtDim, lastRow);
        if (scale != null) {
            for (int i=lastCol; i>=0; i--) {
                final Number s = getElementOrNull(tgtDim, i);
                setNumber(tgtDim, i, Arithmetic.multiply(s, scale));
            }
        }
        if (offset != null) {
            final Number t = getElementOrNull(tgtDim, lastCol);
            setNumber(tgtDim, lastCol, Arithmetic.add(t, offset));
        }
    }

    /**
     * Returns a new matrix which is the result of multiplying this matrix with the specified one.
     * In other words, returns {@code this} × {@code other}.
     *
     * <h4>Relationship with coordinate operations</h4>
     * In the context of coordinate operations, {@code other.multiply(other)} is equivalent to
     * <code>{@linkplain AffineTransform#concatenate AffineTransform.concatenate}(other)</code>:
     * first transforms by the {@code other} transform and then transform the result by {@code this} transform.
     *
     * @param  other  the matrix to multiply to this matrix.
     * @return the result of {@code this} × {@code other}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals to the
     *         number of columns in this matrix.
     */
    public MatrixSIS multiply(final Matrix other) throws MismatchedMatrixSizeException {
        final int nc = other.getNumCol();
        ensureNumRowMatch(getNumCol(), other.getNumRow(), nc);
        final GeneralMatrix result = GeneralMatrix.create(getNumRow(), nc, false);
        result.setToProduct(this, other);
        return result;
    }

    /**
     * Returns a new vector which is the result of multiplying this matrix with the specified vector.
     * In other words, returns {@code this} × {@code vector}. The length of the given vector must be
     * equal to the number of columns in this matrix, and the length of the returned vector will be
     * equal to the number of rows in this matrix.
     *
     * <h4>Relationship with coordinate operations</h4>
     * In the context of coordinate operations, {@code Matrix.multiply(vector)} is related to
     * <code>{@linkplain AffineTransform#transform(double[], int, double[], int, int) AffineTransform.transform}(…)</code>
     * except that the last {@code vector} number is implicitly 1 in {@code AffineTransform} operations.
     * While this {@code multiply(double[])} method could be used for coordinate transformation, it is not its purpose.
     * This method is designed for occasional uses when accuracy is more important than performance.
     *
     * @param  vector  the vector to multiply to this matrix.
     * @return the result of {@code this} × {@code vector}.
     * @throws MismatchedMatrixSizeException if the length of the given vector is not equals to the
     *         number of columns in this matrix.
     *
     * @since 0.8
     */
    public double[] multiply(final double[] vector) {
        final int numCol = getNumCol();
        ensureLengthMatch(numCol, vector);
        final double[] target = new double[getNumRow()];
        for (int j=0; j<target.length; j++) {
            Number sum = null;
            for (int i=0; i<numCol; i++) {
                final Number element = getElementOrNull(j, i);
                sum = Arithmetic.add(sum, Arithmetic.multiply(element, vector[i]));
            }
            if (sum != null) {
                target[j] = sum.doubleValue();
            }
        }
        return target;
    }

    /**
     * Multiplies this matrix by a translation matrix. Invoking this method is equivalent to invoking
     * <code>{@linkplain #multiply(Matrix) multiply}(T)</code> where <var>T</var> is a matrix like
     * below (size varies):
     *
     * <pre class="math">
     *        ┌                    ┐
     *        │ 1  0  0  vector[0] │
     *    T = │ 0  1  0  vector[1] │
     *        │ 0  0  1  vector[2] │
     *        │ 0  0  0  vector[3] │
     *        └                    ┘</pre>
     *
     * The length of the given vector must be equal to the number of columns in this matrix.
     * The last vector element is 1 for an affine transform, but other values are allowed.
     * This matrix will be modified in-place.
     *
     * <p>If this matrix is used for coordinate conversions, then converting a position with
     * the resulting matrix is equivalent to first translating the point by the given vector,
     * then applying the conversion represented by the original matrix.</p>
     *
     * @param  vector  a vector representing a translation to be applied before this matrix.
     *
     * @since 1.0
     *
     * @see AffineTransform#translate(double, double)
     */
    public void translate(final double[] vector) {
        final int numCol = getNumCol();
        ensureLengthMatch(numCol, vector);
        final int numRow = getNumRow();
        for (int j=0; j<numRow; j++) {
            Number sum = null;
            for (int i=0; i<numCol; i++) {
                final double value = vector[i];
                if (value != 0) {   // This is not just an optimization, as we want 0 × NaN = 0 instead of NaN.
                    final Number element = getElementOrNull(j, i);
                    sum = Arithmetic.add(sum, Arithmetic.multiply(element, value));
                }
            }
            setNumber(j, numCol-1, sum);
        }
    }

    /**
     * Returns the value of <var>U</var> which solves {@code this} × <var>U</var> = {@code target}.
     * This is equivalent to first computing the inverse of {@code this}, then multiplying the result
     * by the given matrix.
     *
     * @param  target  the matrix to solve.
     * @return the <var>U</var> matrix that satisfies {@code this} × <var>U</var> = {@code target}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals
     *         to the number of columns in this matrix.
     * @throws NoninvertibleMatrixException if this matrix is not invertible.
     */
    public MatrixSIS solve(final Matrix target) throws MismatchedMatrixSizeException, NoninvertibleMatrixException {
        return Solver.solve(this, target);
    }

    /**
     * Returns the inverse of this matrix.
     *
     * @return the inverse of this matrix.
     * @throws NoninvertibleMatrixException if this matrix is not invertible.
     *
     * @see AffineTransform#createInverse()
     */
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        return Solver.inverse(this);
    }

    /**
     * Returns a new matrix with the same elements as this matrix except for the specified rows.
     * This method is useful for removing a range of <em>target</em> dimensions in an affine transform.
     *
     * @param  lower  index of the first row to remove (inclusive).
     * @param  upper  index after the last row to remove (exclusive).
     * @return a copy of this matrix with the specified rows removed.
     *
     * @since 0.7
     */
    public MatrixSIS removeRows(final int lower, final int upper) {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        Objects.checkFromToIndex(lower, upper, numRow);
        final MatrixSIS reduced = Matrices.createZero(numRow - (upper - lower), numCol, this);
        int dest = 0;
        for (int j=0; j<numRow; j++) {
            if (j == lower) {
                j = upper;
                if (j == numRow) break;
            }
            for (int i=0; i<numCol; i++) {
                reduced.setNumber(dest, i, getNumber(j, i));
            }
            dest++;
        }
        return reduced;
    }

    /**
     * Returns a new matrix with the same elements as this matrix except for the specified columns.
     * This method is useful for removing a range of <em>source</em> dimensions in an affine transform.
     * Coordinates will be converted as if the values in the removed dimensions were zeros.
     *
     * @param  lower  index of the first column to remove (inclusive).
     * @param  upper  index after the last column to remove (exclusive).
     * @return a copy of this matrix with the specified columns removed.
     *
     * @since 0.7
     */
    public MatrixSIS removeColumns(final int lower, final int upper) {
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        Objects.checkFromToIndex(lower, upper, numCol);
        final MatrixSIS reduced = Matrices.createZero(numRow, numCol - (upper - lower), this);
        int dest = 0;
        for (int i=0; i<numCol; i++) {
            if (i == lower) {
                i = upper;
                if (i == numCol) break;
            }
            for (int j=0; j<numRow; j++) {
                reduced.setNumber(j, dest, getNumber(j, i));
            }
            dest++;
        }
        return reduced;
    }

    /**
     * Returns a hash code value based on the data values in this matrix.
     *
     * @return a hash code value for this matrix.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getElements()) ^ (int) serialVersionUID;
    }

    /**
     * Returns {@code true} if the specified object is of the same class as this matrix and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param  object  the object to compare with this matrix for equality.
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
     * The given matrix does not need to be the same implementation class as this matrix.
     *
     * <p>The matrix elements are compared as below:</p>
     * <ul>
     *   <li>{@link Double#NaN} values are considered equals to all other NaN values.</li>
     *   <li>Infinite values are considered equal to other infinite values of the same sign.</li>
     *   <li>All other values are considered equal if the absolute value of their difference is
     *       smaller than or equals to the given threshold.</li>
     * </ul>
     *
     * @param  matrix     the matrix to compare.
     * @param  tolerance  the tolerance value.
     * @return {@code true} if this matrix is close enough to the given matrix given the tolerance value.
     *
     * @see Matrices#equals(Matrix, Matrix, double, boolean)
     */
    public boolean equals(final Matrix matrix, final double tolerance) {
        return Matrices.equals(this, matrix, tolerance, false);
    }

    /**
     * Compares this matrix with the given object for equality. To be considered equal, the two
     * objects must met the following conditions, which depend on the {@code mode} argument:
     *
     * <ul>
     *   <li>{@link ComparisonMode#STRICT STRICT}:
     *       the two matrices must be of the same class, have the same size and the same element values.</li>
     *   <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}:
     *       the two matrices must have the same size and the same element values,
     *       but are not required to be the same implementation class (any {@link Matrix} is okay).</li>
     *   <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}: same as {@code BY_CONTRACT}.
     *   <li>{@link ComparisonMode#APPROXIMATE APPROXIMATE}:
     *       the two matrices must have the same size, but the element values can differ up to some threshold.
     *       The threshold value is determined empirically and may change in any future SIS versions.</li>
     * </ul>
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
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
     * @return a new matrix of the same class and with the same values as this matrix.
     *
     * @see Matrices#copy(Matrix)
     */
    @Override
    public MatrixSIS clone() {
        try {
            return (MatrixSIS) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);            // Should never happen since we are cloneable.
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
