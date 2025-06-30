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
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * A matrix of fixed {@value #SIZE}×{@value #SIZE} size,
 * typically resulting from {@link org.opengis.referencing.operation.MathTransform1D} derivative computation.
 * The matrix member is:
 *
 * <blockquote><pre> ┌     ┐
 * │ {@linkplain #m00} │
 * └     ┘</pre></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see Matrix2
 * @see Matrix3
 * @see Matrix4
 *
 * @since 0.4
 */
public class Matrix1 extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4829171016106097031L;

    /**
     * The matrix size, which is {@value}.
     */
    public static final int SIZE = 1;

    /**
     * The only element in this matrix.
     */
    public double m00;

    /**
     * Creates a new identity matrix.
     */
    public Matrix1() {
        m00 = 1;
    }

    /**
     * Creates a new matrix filled with only zero values.
     *
     * @param ignore  shall always be {@code false} in current version.
     */
    Matrix1(final boolean ignore) {
    }

    /**
     * Creates a new matrix initialized to the specified value.
     *
     * @param m00 The element in this matrix.
     */
    public Matrix1(final double m00) {
        this.m00 = m00;
    }

    /**
     * Creates a new matrix initialized to the specified values.
     * The length of the given array must be 1.
     *
     * @param  elements  elements of the matrix.
     * @throws IllegalArgumentException if the given array does not have the expected length.
     *
     * @see #setElements(double[])
     * @see Matrices#create(int, int, double[])
     */
    public Matrix1(final double[] elements) throws IllegalArgumentException {
        setElements(elements);
    }

    /**
     * Creates a new matrix initialized to the same value as the specified one.
     * The specified matrix size must be {@value #SIZE}×{@value #SIZE}.
     * This is not verified by this constructor, since it shall be verified by {@link Matrices}.
     *
     * @param  matrix  the matrix to copy.
     */
    Matrix1(final Matrix matrix) {
        m00 = matrix.getElement(0,0);
    }

    /**
     * Casts or copies the given matrix to a {@code Matrix1} implementation. If the given {@code matrix}
     * is already an instance of {@code Matrix1}, then it is returned unchanged. Otherwise this method
     * verifies the matrix size, then copies the element in a new {@code Matrix1} object.
     *
     * @param  matrix  the matrix to cast or copy, or {@code null}.
     * @return the matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws MismatchedMatrixSizeException if the size of the given matrix is not {@value #SIZE}×{@value #SIZE}.
     */
    public static Matrix1 castOrCopy(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix == null || matrix instanceof Matrix1) {
            return (Matrix1) matrix;
        }
        ensureSizeMatch(SIZE, SIZE, matrix);
        return new Matrix1(matrix);
    }

    /*
     * The 'final' modifier in following method declarations is redundant with the 'final' modifier
     * in this class declaration, but we keep them as a reminder of which methods should stay final
     * if this class was modified to a non-final class. Some methods should stay final because:
     *
     *  - returning a different value would make no-sense for this class (e.g. 'getNumRow()');
     *  - they are invoked by a constructor or by another method expecting this exact semantic.
     */

    /**
     * Returns the number of rows in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return always {@value #SIZE}.
     */
    @Override
    public final int getNumRow() {
        return SIZE;
    }

    /**
     * Returns the number of columns in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return always {@value #SIZE}.
     */
    @Override
    public final int getNumCol() {
        return SIZE;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix1},
     * then the {@link #m00} field can be read directly for efficiency.
     *
     * @param  row     the row index, which can only be 0.
     * @param  column  the column index, which can only be 0.
     * @return the current value.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row == 0 && column == 0) {
            return m00;
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix1},
     * then the {@link #m00} field can be set directly for efficiency.
     *
     * @param  row     the row index, which can only be 0.
     * @param  column  the column index, which can only be 0.
     * @param  value   the new value to set.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row == 0 && column == 0) {
            m00 = value;
        } else {
            throw indexOutOfBounds(row, column);
        }
    }

    /**
     * Returns all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is 1.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final double[] getElements() {
        return new double[] {m00};
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be 1.
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(SIZE*SIZE, elements);
        m00 = elements[0];
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        return m00 == 1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        return m00 == 1;
    }

    /**
     * For a 1×1 matrix, this method does nothing.
     */
    @Override
    public void transpose() {
        // Nothing to do for a 1x1 matrix.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] multiply(double[] v) {
        if (v.length != 1) {
            throw new MismatchedMatrixSizeException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, 1, v.length));
        }
        return new double[]{
            m00 * v[0]
        };
    }
    /**
     * Normalizes all columns in-place.
     * For a 1×1 matrix with non-NaN value, this method sets the {@link #m00} value
     * to +1, -1 or 0 with the same sign as the original value.
     *
     * @return the magnitude of the column, which is the absolute value of {@link #m00}.
     */
    @Override
    public MatrixSIS normalizeColumns() {
        final MatrixSIS magnitudes = new Matrix1(Math.abs(m00));
        m00 = Math.signum(m00);
        return magnitudes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Matrix1 clone() {
        return (Matrix1) super.clone();
    }

    /**
     * Returns {@code true} if the specified object is of type {@code Matrix1} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param  object  the object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final Matrix1 that = (Matrix1) object;
            return Numerics.equals(m00, that.m00);
        }
        return false;
    }

    /**
     * Returns a hash code value based on the data values in this object.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(m00) ^ serialVersionUID);
    }
}
