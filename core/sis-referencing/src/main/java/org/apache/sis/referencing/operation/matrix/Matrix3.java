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
 * A matrix of fixed {@value #SIZE}×{@value #SIZE} size.
 * The matrix members are:
 *
 * <blockquote><pre> ┌             ┐
 * │ {@linkplain #m00} {@linkplain #m01} {@linkplain #m02} │
 * │ {@linkplain #m10} {@linkplain #m11} {@linkplain #m12} │
 * │ {@linkplain #m20} {@linkplain #m21} {@linkplain #m22} │
 * └             ┘</pre></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see Matrix1
 * @see Matrix2
 * @see Matrix4
 */
public final class Matrix3 extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8902061778871586611L;

    /**
     * The matrix size, which is {@value}.
     */
    public static final int SIZE = 3;

    /** The first matrix element in the first row.   */ public double m00;
    /** The second matrix element in the first row.  */ public double m01;
    /** The third matrix element in the first row.   */ public double m02;
    /** The first matrix element in the second row.  */ public double m10;
    /** The second matrix element in the second row. */ public double m11;
    /** The third matrix element in the second row.  */ public double m12;
    /** The first matrix element in the third row.   */ public double m20;
    /** The second matrix element in the third row.  */ public double m21;
    /** The third matrix element in the third row.   */ public double m22;

    /**
     * Creates a new identity matrix.
     */
    public Matrix3() {
        m00 = m11 = m22 = 1;
    }

    /**
     * Creates a new matrix filled with only zero values.
     *
     * @param ignore Shall always be {@code false} in current version.
     */
    Matrix3(final boolean ignore) {
    }

    /**
     * Creates a new matrix initialized to the specified values.
     *
     * @param m00 The first matrix element in the first row.
     * @param m01 The second matrix element in the first row.
     * @param m02 The third matrix element in the first row.
     * @param m10 The first matrix element in the second row.
     * @param m11 The second matrix element in the second row.
     * @param m12 The third matrix element in the second row.
     * @param m20 The first matrix element in the third row.
     * @param m21 The second matrix element in the third row.
     * @param m22 The third matrix element in the third row.
     */
    public Matrix3(final double m00, final double m01, final double m02,
                   final double m10, final double m11, final double m12,
                   final double m20, final double m21, final double m22)
    {
        this.m00 = m00;    this.m01 = m01;    this.m02 = m02;
        this.m10 = m10;    this.m11 = m11;    this.m12 = m12;
        this.m20 = m20;    this.m21 = m21;    this.m22 = m22;
    }

    /**
     * Creates a new matrix initialized to the specified values.
     * The length of the given array must be 9 and the values in the same order than the above constructor.
     *
     * @param elements Elements of the matrix. Column indices vary fastest.
     * @throws IllegalArgumentException If the given array does not have the expected length.
     *
     * @see #setElements(double[])
     * @see Matrices#create(int, int, double[])
     */
    public Matrix3(final double[] elements) throws IllegalArgumentException {
        setElements(elements);
    }

    /**
     * Creates a new matrix initialized to the same value than the specified one.
     * The specified matrix size must be {@value #SIZE}×{@value #SIZE}.
     * This is not verified by this constructor, since it shall be verified by {@link Matrices}.
     *
     * @param matrix The matrix to copy.
     */
    Matrix3(final Matrix matrix) {
        for (int j=0; j<SIZE; j++) {
            for (int i=0; i<SIZE; i++) {
                setElement(j,i, matrix.getElement(j,i));
            }
        }
    }

    /**
     * Casts or copies the given matrix to a {@code Matrix3} implementation. If the given {@code matrix}
     * is already an instance of {@code Matrix3}, then it is returned unchanged. Otherwise this method
     * verifies the matrix size, then copies all elements in a new {@code Matrix3} object.
     *
     * @param  matrix The matrix to cast or copy, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws MismatchedMatrixSizeException If the size of the given matrix is not {@value #SIZE}×{@value #SIZE}.
     */
    public static Matrix3 castOrCopy(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix == null || matrix instanceof Matrix3) {
            return (Matrix3) matrix;
        }
        ensureSizeMatch(SIZE, SIZE, matrix);
        return new Matrix3(matrix);
    }

    /*
     * The 'final' modifier in following method declarations is redundant with the 'final' modifier
     * in this class declaration, but we keep them as a reminder of which methods should stay final
     * if this class was modified to a non-final class. Some methods should stay final because:
     *
     *  - returning a different value would make no-sense for this class (e.g. 'getNumRow()');
     *  - they are invoked by a constructor or by an other method expecting this exact semantic.
     */

    /**
     * Returns the number of rows in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return Always {@value #SIZE}.
     */
    @Override
    public final int getNumRow() {
        return SIZE;
    }

    /**
     * Returns the number of columns in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return Always {@value #SIZE}.
     */
    @Override
    public final int getNumCol() {
        return SIZE;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix3},
     * then the {@link #m00} … {@link #m22} fields can be read directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @return       The current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: return m00;
                case 1: return m01;
                case 2: return m02;
                case 3: return m10;
                case 4: return m11;
                case 5: return m12;
                case 6: return m20;
                case 7: return m21;
                case 8: return m22;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix3},
     * then the {@link #m00} … {@link #m22} fields can be set directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param value  The new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: m00 = value; return;
                case 1: m01 = value; return;
                case 2: m02 = value; return;
                case 3: m10 = value; return;
                case 4: m11 = value; return;
                case 5: m12 = value; return;
                case 6: m20 = value; return;
                case 7: m21 = value; return;
                case 8: m22 = value; return;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Returns all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is 9.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final double[] getElements() {
        final double[] elements = new double[SIZE*SIZE];
        getElements(elements);
        return elements;
    }

    /**
     * Copies the matrix elements in the given flat array.
     * The array length shall be at least 9, may also be 18.
     */
    @Override
    final void getElements(final double[] elements) {
        elements[0] = m00;    elements[1] = m01;    elements[2] = m02;
        elements[3] = m10;    elements[4] = m11;    elements[5] = m12;
        elements[6] = m20;    elements[7] = m21;    elements[8] = m22;
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be 9.
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(SIZE*SIZE, elements);
        m00 = elements[0];    m01 = elements[1];    m02 = elements[2];
        m10 = elements[3];    m11 = elements[4];    m12 = elements[5];
        m20 = elements[6];    m21 = elements[7];    m22 = elements[8];
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        return m20 == 0 && m21 == 0 && m22 == 1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        return m00 == 1 && m01 == 0 && m02 == 0 &&
               m10 == 0 && m11 == 1 && m12 == 0 &&
               isAffine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transpose() {
        double swap;
        swap = m01; m01 = m10; m10 = swap;
        swap = m02; m02 = m20; m20 = swap;
        swap = m12; m12 = m21; m21 = swap;
    }
}
