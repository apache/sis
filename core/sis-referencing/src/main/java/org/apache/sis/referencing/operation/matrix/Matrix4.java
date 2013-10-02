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
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.MathFunctions;


/**
 * A matrix of fixed {@value #SIZE}×{@value #SIZE} size, often used in datum shifts.
 * The matrix members are:
 *
 * <blockquote><pre> ┌                 ┐
 * │ {@link #m00} {@link #m01} {@link #m02} {@link #m03} │
 * │ {@link #m10} {@link #m11} {@link #m12} {@link #m13} │
 * │ {@link #m20} {@link #m21} {@link #m22} {@link #m23} │
 * │ {@link #m30} {@link #m31} {@link #m32} {@link #m33} │
 * └                 ┘</pre></blockquote>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 *
 * @see Matrix1
 * @see Matrix2
 * @see Matrix3
 */
public final class Matrix4 extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5685762518066856310L;

    /**
     * The matrix size, which is {@value}.
     */
    public static final int SIZE = 4;

    /** The first matrix element in the first row.   */ public double m00;
    /** The second matrix element in the first row.  */ public double m01;
    /** The third matrix element in the first row.   */ public double m02;
    /** The forth matrix element in the first row.   */ public double m03;
    /** The first matrix element in the second row.  */ public double m10;
    /** The second matrix element in the second row. */ public double m11;
    /** The third matrix element in the second row.  */ public double m12;
    /** The forth matrix element in the second row.  */ public double m13;
    /** The first matrix element in the third row.   */ public double m20;
    /** The second matrix element in the third row.  */ public double m21;
    /** The third matrix element in the third row.   */ public double m22;
    /** The forth matrix element in the third row.   */ public double m23;
    /** The first matrix element in the forth row.   */ public double m30;
    /** The second matrix element in the forth row.  */ public double m31;
    /** The third matrix element in the forth row.   */ public double m32;
    /** The forth matrix element in the forth row.   */ public double m33;

    /**
     * Creates a new identity matrix.
     */
    public Matrix4() {
        m00 = m11 = m22 = m33 = 1;
    }

    /**
     * Creates a new matrix filled with only zero values.
     *
     * @param ignore Shall always be {@code false} in current version.
     */
    Matrix4(final boolean ignore) {
    }

    /**
     * Creates a new matrix initialized to the specified values.
     *
     * @param m00 The first matrix element in the first row.
     * @param m01 The second matrix element in the first row.
     * @param m02 The third matrix element in the first row.
     * @param m03 The forth matrix element in the first row.
     * @param m10 The first matrix element in the second row.
     * @param m11 The second matrix element in the second row.
     * @param m12 The third matrix element in the second row.
     * @param m13 The forth matrix element in the second row.
     * @param m20 The first matrix element in the third row.
     * @param m21 The second matrix element in the third row.
     * @param m22 The third matrix element in the third row.
     * @param m23 The forth matrix element in the third row.
     * @param m30 The first matrix element in the forth row.
     * @param m31 The second matrix element in the forth row.
     * @param m32 The third matrix element in the forth row.
     * @param m33 The forth matrix element in the forth row.
     */
    public Matrix4(double m00, double m01, double m02, double m03,
                   double m10, double m11, double m12, double m13,
                   double m20, double m21, double m22, double m23,
                   double m30, double m31, double m32, double m33)
    {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    /**
     * Creates a new matrix initialized to the specified values.
     * The length of the given array must be 16 and the values in the same order than the above constructor.
     *
     * @param elements Elements of the matrix. Column indices vary fastest.
     * @throws IllegalArgumentException If the given array does not have the expected length.
     */
    public Matrix4(final double[] elements) throws IllegalArgumentException {
        setElements(elements);
    }

    /**
     * Creates a new matrix initialized to the same value than the specified one.
     * The specified matrix size must be {@value #SIZE}×{@value #SIZE}.
     * This is not verified by this constructor, since it shall be verified by {@link Matrices}.
     *
     * @param matrix The matrix to copy.
     * @throws IllegalArgumentException if the given matrix is not of the expected size.
     */
    public Matrix4(final Matrix matrix) throws IllegalArgumentException {
        for (int j=0; j<SIZE; j++) {
            for (int i=0; i<SIZE; i++) {
                setElement(j,i, matrix.getElement(j,i));
            }
        }
    }

    /**
     * Casts or copies the given matrix to a {@code Matrix4} implementation. If the given {@code matrix}
     * is already an instance of {@code Matrix4}, then it is returned unchanged. Otherwise this method
     * verifies the matrix size, then copies all elements in a new {@code Matrix4} object.
     *
     * @param  matrix The matrix to cast or copy, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws MismatchedMatrixSizeException If the size of the given matrix is not {@value #SIZE}×{@value #SIZE}.
     */
    public static Matrix4 castOrCopy(final Matrix matrix) throws MismatchedMatrixSizeException {
        if (matrix == null || matrix instanceof Matrix4) {
            return (Matrix4) matrix;
        }
        ensureSizeMatch(SIZE, matrix);
        return new Matrix4(matrix);
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
     * @return Always {@value SIZE}.
     */
    @Override
    public final int getNumRow() {
        return SIZE;
    }

    /**
     * Returns the number of columns in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return Always {@value SIZE}.
     */
    @Override
    public final int getNumCol() {
        return SIZE;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix4},
     * then the {@link #m00} … {@link #m33} fields can be read directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @return       The current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case  0: return m00;
                case  1: return m01;
                case  2: return m02;
                case  3: return m03;
                case  4: return m10;
                case  5: return m11;
                case  6: return m12;
                case  7: return m13;
                case  8: return m20;
                case  9: return m21;
                case 10: return m22;
                case 11: return m23;
                case 12: return m30;
                case 13: return m31;
                case 14: return m32;
                case 15: return m33;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix4},
     * then the {@link #m00} … {@link #m33} fields can be set directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param value  The new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case  0: m00 = value; return;
                case  1: m01 = value; return;
                case  2: m02 = value; return;
                case  3: m03 = value; return;
                case  4: m10 = value; return;
                case  5: m11 = value; return;
                case  6: m12 = value; return;
                case  7: m13 = value; return;
                case  8: m20 = value; return;
                case  9: m21 = value; return;
                case 10: m22 = value; return;
                case 11: m23 = value; return;
                case 12: m30 = value; return;
                case 13: m31 = value; return;
                case 14: m32 = value; return;
                case 15: m33 = value; return;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Returns all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is 16.
     */
    @Override
    public final double[] getElements() {
        return new double[] {
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        };
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be 16.
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(SIZE*SIZE, elements);
        m00 = elements[ 0];
        m01 = elements[ 1];
        m02 = elements[ 2];
        m03 = elements[ 3];
        m10 = elements[ 4];
        m11 = elements[ 5];
        m12 = elements[ 6];
        m13 = elements[ 7];
        m20 = elements[ 8];
        m21 = elements[ 9];
        m22 = elements[10];
        m23 = elements[11];
        m30 = elements[12];
        m31 = elements[13];
        m32 = elements[14];
        m33 = elements[15];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        return m30 == 0 && m31 == 0 && m32 == 0 && m33 == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        return m00 == 1 && m01 == 0 && m02 == 0 && m03 == 0 &&
               m10 == 0 && m11 == 1 && m12 == 0 && m13 == 0 &&
               m20 == 0 && m21 == 0 && m22 == 1 && m23 == 0 &&
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
        swap = m03; m03 = m30; m30 = swap;
        swap = m12; m12 = m21; m21 = swap;
        swap = m13; m13 = m31; m31 = swap;
        swap = m23; m23 = m32; m32 = swap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalizeColumns() {
        double m;
        final double[] v = new double[4];
        v[0]=m00; v[1]=m10; v[2]=m20; v[3]=m30; m = MathFunctions.magnitude(v); m00 /= m; m10 /= m; m20 /= m; m30 /= m;
        v[0]=m01; v[1]=m11; v[2]=m21; v[3]=m31; m = MathFunctions.magnitude(v); m01 /= m; m11 /= m; m21 /= m; m31 /= m;
        v[0]=m02; v[1]=m12; v[2]=m22; v[3]=m32; m = MathFunctions.magnitude(v); m02 /= m; m12 /= m; m22 /= m; m32 /= m;
        v[0]=m03; v[1]=m13; v[2]=m23; v[3]=m33; m = MathFunctions.magnitude(v); m03 /= m; m13 /= m; m23 /= m; m33 /= m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS inverse() throws NoninvertibleMatrixException {
        return Solver.inverse(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS solve(final Matrix matrix) throws MismatchedMatrixSizeException, NoninvertibleMatrixException {
        throw new UnsupportedOperationException(); // TODO
    }

    /**
     * Returns {@code true} if the specified object is of type {@code Matrix4} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param object The object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof Matrix4) {
            final Matrix4 that = (Matrix4) object;
            return Numerics.equals(this.m00, that.m00) &&
                   Numerics.equals(this.m01, that.m01) &&
                   Numerics.equals(this.m02, that.m02) &&
                   Numerics.equals(this.m03, that.m03) &&
                   Numerics.equals(this.m10, that.m10) &&
                   Numerics.equals(this.m11, that.m11) &&
                   Numerics.equals(this.m12, that.m12) &&
                   Numerics.equals(this.m13, that.m13) &&
                   Numerics.equals(this.m20, that.m20) &&
                   Numerics.equals(this.m21, that.m21) &&
                   Numerics.equals(this.m22, that.m22) &&
                   Numerics.equals(this.m23, that.m23) &&
                   Numerics.equals(this.m30, that.m30) &&
                   Numerics.equals(this.m31, that.m31) &&
                   Numerics.equals(this.m32, that.m32) &&
                   Numerics.equals(this.m33, that.m33);
        }
        return false;
    }

    /**
     * Returns a hash code value based on the data values in this object.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getElements()) ^ (int) serialVersionUID;
    }
}
