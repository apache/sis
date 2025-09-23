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
package org.apache.sis.referencing.internal.shared;

import java.util.Arrays;
import java.io.Serializable;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.CloneAccess;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * The matrix of an {@link AffineTransform}, optionally with storage for terms with extended precision.
 * This is implemented in a class separated from {@link AffineTransform2D} for avoiding a conflict
 * between {@link AffineTransform#clone()} and {@link Matrix#clone()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class AffineMatrix extends MatrixSIS implements Serializable, CloneAccess {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8659316010184028768L;

    /**
     * The number of rows and columns of this matrix.
     */
    private static final int SIZE = AffineTransform2D.DIMENSION + 1;

    /**
     * The transform from which to get the matrix terms.
     */
    private final AffineTransform transform;

    /**
     * Creates a new matrix wrapping the given transform.
     *
     * @param transform  the transform to wrap.
     */
    AffineMatrix(final AffineTransform transform) {
        this.transform = transform;
    }

    /**
     * Gets the number of rows in the matrix.
     */
    @Override
    public final int getNumRow() {
        return SIZE;
    }

    /**
     * Gets the number of columns in the matrix.
     */
    @Override
    public final int getNumCol() {
        return SIZE;
    }

    /**
     * Returns {@code true} if the backing affine transform is the identity transform.
     */
    @Override
    public final boolean isIdentity() {
        return transform.isIdentity();
    }

    /**
     * Returns the matrix element at the given index.
     *
     * @param  row     the row number to be retrieved.
     * @param  column  the column number to be retrieved.
     * @return the value at the indexed element.
     * @throws IndexOutOfBoundsException if the specified row or column is out of bounds.
     */
    @Override
    public final double getElement(final int row, final int column) {
        ArgumentChecks.ensureBetween("row",    0, AffineTransform2D.DIMENSION, row);
        ArgumentChecks.ensureBetween("column", 0, AffineTransform2D.DIMENSION, column);
        switch (row * SIZE + column) {
            case 0:  return transform.getScaleX();
            case 1:  return transform.getShearX();
            case 2:  return transform.getTranslateX();
            case 3:  return transform.getShearY();
            case 4:  return transform.getScaleY();
            case 5:  return transform.getTranslateY();
            case 8:  return 1;
            default: return 0;
        }
    }

    /**
     * Unsupported operation because this matrix is unmodifiable.
     */
    @Override
    public final void setElement(int row, int column, double value) {
        throw new UnsupportedOperationException(Resources.format(Resources.Keys.UnmodifiableAffineTransform));
    }

    /**
     * Unsupported operation because this matrix is unmodifiable.
     */
    @Override
    public final void transpose() {
        throw new UnsupportedOperationException(Resources.format(Resources.Keys.UnmodifiableAffineTransform));
    }

    /**
     * An {@code AffineMatrix} providing matrix elements with extended precision.
     */
    @SuppressWarnings("CloneableImplementsClone")
    static final class ExtendedPrecision extends AffineMatrix implements ExtendedPrecisionMatrix {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -4887280720125030417L;

        /**
         * The length of an array containing only the matrix elements to be stored.
         * The last row is omitted because it is assumed to contain (0 0 1).
         */
        private static final int LENGTH_STORED = (SIZE - 1) * SIZE;

        /**
         * The terms with extended prevision.
         * The length of this array shall be {@value #LENGTH_STORED}.
         */
        private final Number[] elements;

        /**
         * Creates a new matrix wrapping the given transform.
         * This constructor shall not modify the given array.
         *
         * @param transform  the transform to wrap.
         * @param elements   the elements used for creating the matrix.
         *                   Zero values <em>shall</em> be null.
         */
        ExtendedPrecision(final AffineTransform transform, final Number[] elements) {
            super(transform);
            this.elements = Arrays.copyOf(elements, LENGTH_STORED);
        }

        /**
         * Returns all matrix elements in row-major order.
         * Note that this is not the same order as {@link AffineTransform} constructor.
         * Zero values <em>shall</em> be null.
         */
        @Override
        public Number[] getElementAsNumbers(final boolean writable) {
            final Number[] numbers = Arrays.copyOf(elements, SIZE*SIZE);
            numbers[SIZE*SIZE - 1] = 1;
            return numbers;
        }

        /**
         * Retrieves the value at the specified row and column if different than zero.
         * If the value is zero, then this method <em>shall</em> return {@code null}.
         */
        @Override
        public Number getElementOrNull(final int row, final int column) {
            ArgumentChecks.ensureBetween("row",    0, AffineTransform2D.DIMENSION, row);
            ArgumentChecks.ensureBetween("column", 0, AffineTransform2D.DIMENSION, column);
            if (row != AffineTransform2D.DIMENSION) {
                return elements[row * SIZE + column];
            } else {
                return (column == AffineTransform2D.DIMENSION) ? 1 : null;
            }
        }

        /**
         * Returns a hash code value for this matrix.
         */
        @Override
        public int hashCode() {
            return super.hashCode() + Arrays.hashCode(elements);
        }

        /**
         * Compares this matrix with the given object for equality, including error terms (if any).
         */
        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj) && Arrays.equals(elements, ((ExtendedPrecision) obj).elements);
        }
    }

    /**
     * Returns a hash code value for this matrix.
     */
    @Override
    public int hashCode() {
        return transform.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Compares this matrix with the given object for equality, including error terms (if any).
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            return transform.equals(((AffineMatrix) obj).transform);
        }
        return false;
    }

    /**
     * Returns a copy of the matrix that user can modify.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final MatrixSIS clone() {
        return Matrices.copy(this);
    }

    /**
     * Returns a string representation of this matrix.
     *
     * @return a string representation of this matrix.
     */
    @Override
    public final String toString() {
        return Matrices.toString(this);
    }
}
