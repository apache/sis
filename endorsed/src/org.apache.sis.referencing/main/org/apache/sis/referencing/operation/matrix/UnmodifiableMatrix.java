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
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.util.resources.Errors;


/**
 * An unmodifiable view of a matrix. This matrix is immutable only if the wrapped matrix
 * is not modified anymore after {@code UnmodifiableMatrix} construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class UnmodifiableMatrix extends MatrixSIS implements ExtendedPrecisionMatrix {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7239828819464047564L;

    /**
     * The wrapped matrix.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final Matrix matrix;

    /**
     * Creates an unmodifiable view of the given matrix.
     */
    UnmodifiableMatrix(final Matrix matrix) {
        this.matrix = matrix;
    }

    /**
     * Returns the most direct matrix that we can use for extended precision.
     * The returned matrix shall be considered read-only.
     */
    final ExtendedPrecisionMatrix asExtendePrecision() {
        return (matrix instanceof ExtendedPrecisionMatrix) ? (ExtendedPrecisionMatrix) matrix : this;
    }

    /**
     * Returns the number of rows in the wrapped matrix.
     */
    @Override
    public int getNumRow() {
        return matrix.getNumRow();
    }

    /**
     * Returns the number of columns in the wrapped matrix.
     */
    @Override
    public int getNumCol() {
        return matrix.getNumCol();
    }

    /**
     * Returns {@code true} if the wrapped matrix is the identity matrix.
     */
    @Override
    public boolean isIdentity() {
        return matrix.isIdentity();
    }

    /**
     * Returns the element in the wrapped matrix at the given row and column.
     */
    @Override
    public double getElement(final int row, final int column) {
        return matrix.getElement(row, column);
    }

    /**
     * Returns all elements in the wrapped matrix in a row-major array.
     */
    @Override
    public double[] getElements() {
        if (matrix instanceof MatrixSIS) {
            return ((MatrixSIS) matrix).getElements();
        } else {
            return super.getElements();
        }
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major (column indices vary fastest) array.
     * Zero values <em>shall</em> be null. Callers can write in the returned array if and only if the
     * {@code writable} argument is {@code true}.
     */
    @Override
    public Number[] getElementAsNumbers(final boolean writable) {
        if (matrix instanceof ExtendedPrecisionMatrix) {
            return ((ExtendedPrecisionMatrix) matrix).getElementAsNumbers(writable);
        }
        return ExtendedPrecisionMatrix.super.getElementAsNumbers(writable);
    }

    /**
     * Retrieves the value at the specified row and column if different than zero.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     */
    @Override
    public Number getElementOrNull(final int row, final int column) {
        if (matrix instanceof ExtendedPrecisionMatrix) {
            return ((ExtendedPrecisionMatrix) matrix).getElementOrNull(row, column);
        }
        final double element = matrix.getElement(row, column);
        return (element != 0) ? element : null;
    }

    /**
     * Returns the exception to throw when a setter method is invoked.
     */
    private UnsupportedOperationException canNotModify() {
        return new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException} since this view is unmodifiable.
     */
    @Override
    public void setElements(double[] elements) {
        throw canNotModify();
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException} since this view is unmodifiable.
     */
    @Override
    public void setElement(int row, int column, double value) {
        throw canNotModify();
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException} since this view is unmodifiable.
     */
    @Override
    public void transpose() {
        throw canNotModify();
    }

    /**
     * Returns a copy of this matrix that users can modify.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public MatrixSIS clone() {
        return castOrCopy(matrix.clone());
    }
}
