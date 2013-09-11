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

import java.io.Serializable;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;


/**
 * A {@link Matrix} able to perform some operations of interest to Spatial Information Systems (SIS).
 * This class completes the GeoAPI {@link Matrix} interface with some operations used by {@code sis-referencing}.
 * It is not a {@code MatrixSIS} goal to provide all possible Matrix operations, as there is too many of them.
 * This class focuses only on:
 *
 * <ul>
 *   <li>basic operations needed for <cite>referencing by coordinates</cite>:
 *       {@link #negate()}, {@link #transpose()}, {@link #inverse()}, {@link #multiply(Matrix)};</li>
 *   <li>some operations more specific to referencing by coordinates:
 *       {@link #isAffine()}, {@link #normalizeColumns()}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
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
     * Ensures that the given matrix is a square matrix having the given dimension.
     * This is a convenience method for subclasses.
     */
    static void ensureSizeMatch(final int size, final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != size || numCol != size) {
            final Integer n = size;
            throw new MismatchedMatrixSizeException(Errors.format(
                    Errors.Keys.MismatchedMatrixSize_4, n, n, numRow, numCol));
        }
    }

    /**
     * Returns an exception for the given indices.
     */
    static IndexOutOfBoundsException indexOutOfBounds(final int row, final int column) {
        return new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndicesOutOfBounds_2, row, column));
    }

    /**
     * Returns {@code true} if this matrix represents an affine transform.
     * A transform is affine if the matrix is square and its last row contains
     * only zeros, except in the last column which contains 1.
     *
     * @return {@code true} if this matrix is affine.
     */
    public abstract boolean isAffine();

    /**
     * Returns {@code true} if this matrix is an identity matrix.
     * Invoking this method is equivalent to invoking <code>{@linkplain #isIdentity(double) isIdentity}(0.0)</code>,
     * except that it is potentially more efficient.
     *
     * @return {@code true} if this matrix is an identity matrix.
     *
     * @see #setToIdentity()
     * @see java.awt.geom.AffineTransform#isIdentity()
     */
    @Override
    public abstract boolean isIdentity();

    /**
     * Returns {@code true} if this matrix is close to an identity matrix, given a tolerance threshold.
     * This method is equivalent to computing the difference between this matrix and an identity
     * matrix of identical size, and returning {@code true} if and only if all differences are
     * smaller than or equal to {@code tolerance}.
     *
     * @param  tolerance The tolerance value, or 0 for a strict comparison.
     * @return {@code true} if this matrix is close to the identity matrix given the tolerance threshold.
     *
     * @see Matrices#isIdentity(Matrix, double)
     */
    public boolean isIdentity(final double tolerance) {
        return Matrices.isIdentity(this, tolerance);
    }

    /**
     * Sets this matrix to zero everywhere except for the elements on the diagonal, which are set to 1.
     * If this matrix contains more rows than columns, then the extra rows will contain only zero values.
     * If this matrix contains more columns than rows, then the extra columns will contain only zero values.
     *
     * {@section Use case}
     * This method is often used together with {@link #isIdentity(double)} in order to workaround rounding errors,
     * like below:
     *
     * {@preformat java
     *     if (matrix.isIdentity(1E-10)) {
     *         matrix.setToIdentity();
     *     }
     * }
     *
     * @see #isIdentity()
     * @see java.awt.geom.AffineTransform#setToIdentity()
     */
    public abstract void setToIdentity();

    /**
     * Sets all the values in this matrix to zero.
     */
    public abstract void setToZero();

    /**
     * Negates the values of this matrix: {@code this} = {@code -this}.
     */
    public abstract void negate();

    /**
     * Sets the value of this matrix to its transpose.
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
     */
    public abstract void normalizeColumns();

    /**
     * Returns a new matrix which is the result of multiplying this matrix with the specified one.
     * In other words, returns {@code this} × {@code matrix}.
     *
     * {@section Relationship with coordinate operations}
     * In the context of coordinate operations, {@code Matrix.multiply(other)} is equivalent to
     * <code>{@linkplain java.awt.geom.AffineTransform#concatenate AffineTransform.concatenate}(other)</code>:
     * first transforms by the supplied transform and then transform the result by the original transform.
     *
     * @param  matrix The matrix to multiply to this matrix.
     * @return The result of {@code this} × {@code matrix}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals to the
     *         number of columns in this matrix.
     */
    public abstract MatrixSIS multiply(Matrix matrix) throws MismatchedMatrixSizeException;

    /**
     * Returns the inverse of this matrix.
     *
     * @return The inverse of this matrix.
     * @throws SingularMatrixException if this matrix is not invertible.
     *
     * @see java.awt.geom.AffineTransform#createInverse()
     */
    public abstract MatrixSIS inverse() throws SingularMatrixException;

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
