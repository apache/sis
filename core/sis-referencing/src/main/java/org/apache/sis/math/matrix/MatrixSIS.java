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
package org.apache.sis.math.matrix;

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;


/**
 * A matrix able to perform some operations of interest to Spatial Information Systems (SIS).
 * The GeoAPI {@link Matrix} interface is basically a two dimensional array of numbers.
 * The {@code MatrixSIS} class adds some operations.
 *
 * <p>It is not a {@code MatrixSIS} goal to provide all possible Matrix operations, as there is too many of them.
 * This interface focuses only on basic operations needed for <cite>referencing by coordinates</cite>
 * ({@link #negate()}, {@link #transpose()}, {@link #inverse()}, {@link #multiply(Matrix)}),
 * completed by some operations more specific to referencing by coordinates
 * ({@link #isAffine()}, {@link #normalizeColumns()}).</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 *
 * @see Matrices#toSIS(Matrix)
 */
public interface MatrixSIS extends Matrix, LenientComparable {
    /**
     * Returns {@code true} if this matrix represents an affine transform.
     * A transform is affine if the matrix is square and its last row contains
     * only zeros, except in the last column which contains 1.
     *
     * <p>In the two dimensional case, the matrix for an affine transform is:</p>
     *
     * <p><center><img src="doc-files/AffineTransform.png"></center></p>
     *
     * @return {@code true} if this matrix is affine.
     */
    boolean isAffine();

    /**
     * Returns {@code true} if this matrix is close to an identity matrix, given a tolerance threshold.
     * This method is equivalent to computing the difference between this matrix and an identity
     * matrix of identical size, and returning {@code true} if and only if all differences are
     * smaller than or equal to {@code tolerance}.
     *
     * @param  tolerance The tolerance value, or 0 for a strict comparison.
     * @return {@code true} if this matrix is close to the identity matrix given the tolerance threshold.
     */
    boolean isIdentity(double tolerance);

    /**
     * Sets this matrix to zero everywhere except for the elements on the diagonal, which are set to 1.
     * If this matrix contains more rows than columns, then the extra rows will contain only zero values.
     * If this matrix contains more columns than rows, then the extra columns will contain only zero values.
     */
    void setToIdentity();

    /**
     * Sets all the values in this matrix to zero.
     */
    void setToZero();

    /**
     * Negates the values of this matrix: {@code this} = {@code -this}.
     */
    void negate();

    /**
     * Sets the value of this matrix to its transpose.
     */
    void transpose();

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
    void normalizeColumns();

    /**
     * Returns a new matrix which is the result of multiplying this matrix with the specified one.
     * In other words, returns {@code this} × {@code matrix}.
     *
     * <p>In the context of coordinate transformations, this is equivalent to
     * {@link java.awt.geom.AffineTransform#concatenate AffineTransform.concatenate(…)}:
     * first transforms by the supplied transform and then transform the result by the original transform.</p>
     *
     * @param  matrix The matrix to multiply to this matrix.
     * @return The result of {@code this} × {@code matrix}.
     * @throws MismatchedMatrixSizeException if the number of rows in the given matrix is not equals to the
     *         number of columns in this matrix.
     */
    MatrixSIS multiply(Matrix matrix) throws MismatchedMatrixSizeException;

    /**
     * Returns the inverse of this matrix.
     *
     * @return The inverse of this matrix.
     * @throws SingularMatrixException if this matrix is not invertible.
     */
    MatrixSIS inverse() throws SingularMatrixException;

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
    boolean equals(Matrix matrix, double tolerance);

    /**
     * Compares this matrix with the given object for equality. To be considered equal, the two
     * objects must meet the following conditions, which depend on the {@code mode} argument:
     *
     * <ul>
     *   <li><b>{@link ComparisonMode#STRICT STRICT}:</b> the two matrices must be of the same class,
     *       have the same size and the same element values.</li>
     *   <li><b>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT} or {@link ComparisonMode#IGNORE_METADATA
     *       IGNORE_METADATA}:</b> the two matrices must have the same size and the same element values,
     *       but are not required to be the same implementation class (any {@link Matrix} is okay).</li>
     *   <li><b>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:</b> the two matrices must have
     *       the same size, but the element values can differ up to some threshold. The threshold
     *       value is determined empirically and may change in future SIS versions.</li>
     * </ul>
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @see Matrices#equals(Matrix, Matrix, ComparisonMode)
     */
    @Override
    boolean equals(Object object, ComparisonMode mode);

    /**
     * Returns a clone of this matrix.
     *
     * @return A new matrix of the same class and with the same values than this matrix.
     */
    @Override
    MatrixSIS clone();
}
