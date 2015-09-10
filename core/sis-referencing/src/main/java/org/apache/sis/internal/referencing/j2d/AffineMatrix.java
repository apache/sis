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
package org.apache.sis.internal.referencing.j2d;

import java.util.Arrays;
import java.io.Serializable;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * The matrix of an {@link AffineTransform}, optionally with storage for the error terms
 * used in double-double arithmetic.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class AffineMatrix implements ExtendedPrecisionMatrix, Serializable, Cloneable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1605578645060388327L;

    /**
     * The number of rows and columns of this matrix.
     */
    private static final int SIZE = 3;

    /**
     * The length of an array containing all matrix elements.
     */
    private static final int LENGTH = SIZE * SIZE;

    /**
     * The length of an array containing only the matrix elements to be stored.
     * The last row is omitted because it is assumed to contain (0 0 1).
     */
    private static final int LENGTH_STORED = (SIZE - 1) * SIZE;

    /**
     * The length of an array containing all matrix elements together with error terms.
     */
    private static final int LENGTH_EXTENDED = 2 * LENGTH;

    /**
     * The transform from which to get the matrix terms.
     */
    private final AffineTransform transform;

    /**
     * The error terms, or {@code null} if none.
     * If non-null, then the length of this array shall be 6.
     */
    private final double[] errors;

    /**
     * Creates a new matrix wrapping the given transform.
     *
     * @param transform The transform to wrap.
     * @param elements The elements used for creating the matrix (optionally with error terms), or {@code null}.
     */
    AffineMatrix(final AffineTransform transform, final double[] elements) {
        this.transform = transform;
        if (elements != null) {
            assert elements.length == LENGTH || elements.length == LENGTH_EXTENDED;
            if (elements.length == LENGTH_EXTENDED) {
                errors = Arrays.copyOfRange(elements, LENGTH, LENGTH + LENGTH_STORED);
                /*
                 * At this point we could check:
                 *
                 *   assert Arrays.equals(elements, getExtendedElements());
                 *
                 * but we do not, because the terms in the last row may not be exactly 0 or 1
                 * because of rounding errors.
                 */
                return;
            }
        }
        errors = null;
    }

    /**
     * Gets the number of rows in the matrix.
     */
    @Override
    public int getNumRow() {
        return SIZE;
    }

    /**
     * Gets the number of columns in the matrix.
     */
    @Override
    public int getNumCol() {
        return SIZE;
    }

    /**
     * Returns {@code true} if the backing affine transform is the identity transform.
     */
    @Override
    public boolean isIdentity() {
        return transform.isIdentity();
    }

    /**
     * Returns all matrix elements.
     */
    @Override
    public double[] getExtendedElements() {
        final double[] elements = new double[errors != null ? LENGTH_EXTENDED : LENGTH];
        if (errors != null) {
            System.arraycopy(errors, 0, elements, LENGTH, LENGTH_STORED);
        }
        elements[0] = transform.getScaleX();
        elements[1] = transform.getShearX();
        elements[2] = transform.getTranslateX();
        elements[3] = transform.getShearY();
        elements[4] = transform.getScaleY();
        elements[5] = transform.getTranslateY();
        elements[8] = 1;
        return elements;
    }

    /**
     * Returns the matrix element at the given index.
     */
    @Override
    public final double getElement(final int row, final int column) {
        ArgumentChecks.ensureBetween("row",    0, SIZE, row);
        ArgumentChecks.ensureBetween("column", 0, SIZE, column);
        switch (row * SIZE + column) {
            case 0: return transform.getScaleX();
            case 1: return transform.getShearX();
            case 2: return transform.getTranslateX();
            case 3: return transform.getShearY();
            case 4: return transform.getScaleY();
            case 5: return transform.getTranslateY();
            case 6: // Fallthrough
            case 7: return 0;
            case 8: return 1;
            default: throw new AssertionError();
        }
    }

    /**
     * Unsupported operation, since this matrix is unmodifiable.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableAffineTransform));
    }

    /**
     * Returns a copy of the matrix that user can modify.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final Matrix clone() {
        return Matrices.copy(this);
    }

    /**
     * Returns a string representation of this matrix.
     *
     * @return String representation of this matrix.
     */
    @Override
    public String toString() {
        return Matrices.toString(this);
    }
}
