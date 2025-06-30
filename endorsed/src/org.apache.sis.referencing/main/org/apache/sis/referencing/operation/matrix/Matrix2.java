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


/**
 * A matrix of fixed {@value #SIZE}×{@value #SIZE} size,
 * typically resulting from {@link org.opengis.referencing.operation.MathTransform2D} derivative computation.
 * The matrix members are:
 *
 * <blockquote><pre> ┌         ┐
 * │ {@linkplain #m00} {@linkplain #m01} │
 * │ {@linkplain #m10} {@linkplain #m11} │
 * └         ┘</pre></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see Matrix1
 * @see Matrix3
 * @see Matrix4
 *
 * @since 0.4
 */
public class Matrix2 extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7116561372481474290L;

    /**
     * The matrix size, which is {@value}.
     */
    public static final int SIZE = 2;

    /** The first matrix element in the first row.   */ public double m00;
    /** The second matrix element in the first row.  */ public double m01;
    /** The first matrix element in the second row.  */ public double m10;
    /** The second matrix element in the second row. */ public double m11;

    /**
     * Creates a new identity matrix.
     */
    public Matrix2() {
        m00 = m11 = 1;
    }

    /**
     * Creates a new matrix filled with only zero values.
     *
     * @param ignore  shall always be {@code false} in current version.
     */
    Matrix2(final boolean ignore) {
    }

    /**
     * Creates a new matrix initialized to the specified values.
     *
     * @param m00  the first matrix element in the first row.
     * @param m01  the second matrix element in the first row.
     * @param m10  the first matrix element in the second row.
     * @param m11  the second matrix element in the second row.
     */
    public Matrix2(final double m00, final double m01,
                   final double m10, final double m11)
    {
        this.m00 = m00;    this.m01 = m01;
        this.m10 = m10;    this.m11 = m11;
    }

    /**
     * Creates a new matrix initialized to the specified values.
     * The length of the given array must be 4 and the values in the same order as the above constructor.
     *
     * @param  elements  elements of the matrix. Column indices vary fastest.
     * @throws IllegalArgumentException if the given array does not have the expected length.
     *
     * @see #setElements(double[])
     * @see Matrices#create(int, int, double[])
     */
    public Matrix2(final double[] elements) throws IllegalArgumentException {
        setElements(elements);
    }

    /**
     * Creates a new matrix initialized to the same value as the specified one.
     * The specified matrix size must be {@value #SIZE}×{@value #SIZE}.
     * This is not verified by this constructor, since it shall be verified by {@link Matrices}.
     *
     * @param  matrix  the matrix to copy.
     */
    Matrix2(final Matrix matrix) {
        m00 = matrix.getElement(0,0);
        m01 = matrix.getElement(0,1);
        m10 = matrix.getElement(1,0);
        m11 = matrix.getElement(1,1);
    }

    /**
     * Casts or copies the given matrix to a {@code Matrix2} implementation. If the given {@code matrix}
     * is already an instance of {@code Matrix2}, then it is returned unchanged. Otherwise this method
     * verifies the matrix size, then copies all elements in a new {@code Matrix2} object.
     *
     * @param  matrix  the matrix to cast or copy, or {@code null}.
     * @return the matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws MismatchedMatrixSizeException if the size of the given matrix is not {@value #SIZE}×{@value #SIZE}.
     */
    public static Matrix2 castOrCopy(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix == null || matrix instanceof Matrix2) {
            return (Matrix2) matrix;
        }
        ensureSizeMatch(SIZE, SIZE, matrix);
        return new Matrix2(matrix);
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
     * If the matrix is known to be an instance of {@code Matrix2},
     * then the {@link #m00} … {@link #m11} fields can be read directly for efficiency.
     *
     * @param  row     the row index, which can only be 0 or 1.
     * @param  column  the column index, which can only be 0 or 1.
     * @return the current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: return m00;
                case 1: return m01;
                case 2: return m10;
                case 3: return m11;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix2},
     * then the {@link #m00} … {@link #m11} fields can be set directly for efficiency.
     *
     * @param  row     the row index, which can only be 0 or 1.
     * @param  column  the column index, which can only be 0 or 1.
     * @param  value   the new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: m00 = value; return;
                case 1: m01 = value; return;
                case 2: m10 = value; return;
                case 3: m11 = value; return;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Returns all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is 4.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final double[] getElements() {
        return new double[] {
            m00, m01,
            m10, m11
        };
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be 4.
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(SIZE*SIZE, elements);
        m00 = elements[0];    m01 = elements[1];
        m10 = elements[2];    m11 = elements[3];
    }

    /**
     * Sets the elements to a rotation matrix of the given arithmetic angle.
     * Angle 0 is oriented toward positive <var>x</bar> axis,
     * rotation is counter-clockwise and the unit of measurement is radians.
     * The resulting matrix is not affine in the sense of {@link #isAffine()}.
     * The matrix is:
     *
     * <pre class="math">
     *        ┌                  ┐
     *        │ cos(θ)  −sin(θ)  │
     *        │ sin(θ)   cos(θ)  │
     *        └                  ┘</pre>
     *
     * @param θ  arithmetic rotation angle in radians.
     * @since 1.5
     */
    public void setToRotation(double θ) {
        m00 =  (m11 = Math.cos(θ));
        m01 = -(m10 = Math.sin(θ));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        return m10 == 0 && m11 == 1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        return m00 == 1 && m10 == 0 &&
               m01 == 0 && m11 == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transpose() {
        final double swap = m10;
        m10 = m01;
        m01 = swap;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    @Override
    public double[] multiply(final double[] vector) {
        ensureLengthMatch(2, vector);
        final double x = vector[0];
        final double y = vector[1];
        return new double[] {
            m00 * x + m01 * y,
            m10 * x + m11 * y
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Matrix2 clone() {
        return (Matrix2) super.clone();
    }

    /**
     * Returns {@code true} if the specified object is of type {@code Matrix2} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param  object  the object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final Matrix2 that = (Matrix2) object;
            return Numerics.equals(this.m00, that.m00) &&
                   Numerics.equals(this.m01, that.m01) &&
                   Numerics.equals(this.m10, that.m10) &&
                   Numerics.equals(this.m11, that.m11);
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
        return Long.hashCode(serialVersionUID ^
                (((Double.doubleToLongBits(m00)  +
              31 * Double.doubleToLongBits(m01)) +
              31 * Double.doubleToLongBits(m10)) +
              31 * Double.doubleToLongBits(m11)));
    }
}
