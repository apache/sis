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
package org.apache.sis.referencing.internal;

import org.opengis.referencing.operation.Matrix;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.util.internal.shared.CloneAccess;


/**
 * A matrix augmented with annotation about transformation accuracy.
 * We use this class for passing additional information in methods that returns only a {@link Matrix}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AnnotatedMatrix implements Matrix, CloneAccess {
    /**
     * The matrix which contains the actual values.
     */
    private final Matrix matrix;

    /**
     * Accuracy associated with this matrix.
     */
    public final PositionalAccuracy accuracy;

    /**
     * Creates a new matrix wrapping the given matrix.
     *
     * @param  matrix    the matrix which contains the actual values.
     * @param  accuracy  accuracy associated with this matrix.
     */
    private AnnotatedMatrix(final Matrix matrix, final PositionalAccuracy accuracy) {
        this.matrix   = matrix;
        this.accuracy = accuracy;
    }

    /**
     * Returns an {@link AnnotatedMatrix} associates with {@link PositionalAccuracyConstant#INDIRECT_SHIFT_APPLIED}.
     *
     * @param  matrix     the matrix to wrap.
     * @param  intersect  whether an intersection has been found between the two datum.
     * @return the annotated matrix.
     */
    public static Matrix indirect(final Matrix matrix, final boolean intersect) {
        return new AnnotatedMatrix(matrix,
                intersect ? PositionalAccuracyConstant.INDIRECT_SHIFT_APPLIED
                          : PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);
    }

    /**
     * Returns the number of rows in this matrix.
     */
    @Override
    public int getNumRow() {
        return matrix.getNumRow();
    }

    /**
     * Returns the number of columns in this matrix.
     */
    @Override
    public int getNumCol() {
        return matrix.getNumCol();
    }

    /**
     * Returns {@code true} if this matrix is an identity matrix.
     */
    @Override
    public boolean isIdentity() {
        return matrix.isIdentity();
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     */
    @Override
    public double getElement(int row, int column) {
        return matrix.getElement(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     */
    @Override
    public void setElement(int row, int column, double value) {
        matrix.setElement(row, column, value);
    }

    /**
     * Returns a clone of this matrix.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Matrix clone() {
        return new AnnotatedMatrix(matrix.clone(), accuracy);
    }

    /**
     * Compares this matrix with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AnnotatedMatrix) {
            final AnnotatedMatrix other = (AnnotatedMatrix) obj;
            return matrix.equals(other.matrix) && accuracy.equals(other.accuracy);
        }
        return false;
    }

    /**
     * Returns a hash code value for this matrix.
     */
    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public String toString() {
        return matrix.toString();
    }
}
