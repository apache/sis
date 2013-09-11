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
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;

// Related to JDK7
import java.util.Objects;


/**
 * Static utility methods for creating and manipulating {@link Matrix} instances.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
public final class Matrices extends Static {
    /**
     * Number of spaces to put between columns formatted by {@link #toString(Matrix)}.
     */
    private static final int MARGIN = 2;

    /**
     * Do not allows instantiation of this class.
     */
    private Matrices() {
    }

    /**
     * Creates a square identity matrix of size {@code size} × {@code size}.
     * Elements on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * <p>For an affine transform, the {@code size} is the number of
     * {@linkplain MathTransform#getSourceDimensions() source} and
     * {@linkplain MathTransform#getTargetDimensions() target} dimensions + 1.</p>
     *
     * @param size Numbers of row and columns.
     * @return An identity matrix of the given size.
     */
    public static MatrixSIS createIdentity(final int size) {
        switch (size) {
            case 1: return new Matrix1();
            case 2: return new Matrix2();
            case 3: return new Matrix3();
            case 4: return new Matrix4();
        }
        return new GeneralMatrix(size, size, true);
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol}.
     * Elements on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * @param numRow For an affine transform, this is the number of {@linkplain MathTransform#getTargetDimensions() target dimensions} + 1.
     * @param numCol For an affine transform, this is the number of {@linkplain MathTransform#getSourceDimensions() source dimensions} + 1.
     * @return An identity matrix of the given size.
     */
    public static MatrixSIS create(final int numRow, final int numCol) {
        if (numRow == numCol) {
            return createIdentity(numRow);
        } else {
            return new GeneralMatrix(numRow, numCol, true);
        }
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol} filled with zero values.
     * This constructor is convenient when the caller want to initialize the matrix elements himself.
     *
     * @param numRow For an affine transform, this is the number of {@linkplain MathTransform#getTargetDimensions() target dimensions} + 1.
     * @param numCol For an affine transform, this is the number of {@linkplain MathTransform#getSourceDimensions() source dimensions} + 1.
     * @return A matrix of the given size with only zero values.
     */
    public static MatrixSIS createZero(final int numRow, final int numCol) {
        if (numRow == numCol) {
            switch (numRow) {
                case 1: return new Matrix1(false);
                case 2: return new Matrix2(false);
                case 3: return new Matrix3(false);
                case 4: return new Matrix4(false);
            }
        }
        return new GeneralMatrix(numRow, numCol, false);
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol} initialized to the given elements.
     * The elements array size must be equals to {@code numRow*numCol}. Column indices vary fastest.
     *
     * @param  numRow   Number of rows.
     * @param  numCol   Number of columns.
     * @param  elements The matrix elements in a row-major array. Column indices vary fastest.
     * @return A matrix initialized to the given elements.
     *
     * @see MatrixSIS#setElements(double[])
     */
    public static MatrixSIS create(final int numRow, final int numCol, final double[] elements) {
        if (numRow == numCol) switch (numRow) {
            case 1: return new Matrix1(elements);
            case 2: return new Matrix2(elements);
            case 3: return new Matrix3(elements);
            case 4: return new Matrix4(elements);
        }
        return new GeneralMatrix(numRow, numCol, elements);
    }

    /**
     * Creates a new matrix which is a copy of the given matrix.
     *
     * @param matrix The matrix to copy, or {@code null}.
     * @return A copy of the given matrix, or {@code null} if the given matrix was null.
     */
    public static MatrixSIS copy(final Matrix matrix) {
        if (matrix == null) {
            return null;
        }
        final int size = matrix.getNumRow();
        if (size == matrix.getNumCol()) {
            switch (size) {
                case 1: return new Matrix1(matrix);
                case 2: return new Matrix2(matrix);
                case 3: return new Matrix3(matrix);
                case 4: return new Matrix4(matrix);
            }
        }
        return new GeneralMatrix(matrix);
    }

    /**
     * Casts or copies the given matrix to a SIS implementation. If {@code matrix} is already
     * an instance of {@code MatrixSIS}, then it is returned unchanged. Otherwise all elements
     * are copied in a new {@code MatrixSIS} object.
     *
     * @param  matrix The matrix to cast or copy, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     */
    public static MatrixSIS castOrCopy(final Matrix matrix) {
        if (matrix instanceof MatrixSIS) {
            return (MatrixSIS) matrix;
        }
        return copy(matrix);
    }

    /**
     * Returns {@code true} if the given matrix is close to an identity matrix, given a tolerance threshold.
     * This method is equivalent to computing the difference between the given matrix and an identity matrix
     * of identical size, and returning {@code true} if and only if all differences are smaller than or equal
     * to {@code tolerance}.
     *
     * @param  matrix The matrix to test for identity.
     * @param  tolerance The tolerance value, or 0 for a strict comparison.
     * @return {@code true} if this matrix is close to the identity matrix given the tolerance threshold.
     *
     * @see MatrixSIS#isIdentity(double)
     */
    public static boolean isIdentity(final Matrix matrix, final double tolerance) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != numCol) {
            return false;
        }
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                double e = matrix.getElement(j,i);
                if (i == j) {
                    e--;
                }
                if (!(Math.abs(e) <= tolerance)) {  // Uses '!' in order to catch NaN values.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares the given matrices for equality, using the given relative or absolute tolerance threshold.
     * The matrix elements are compared as below:
     *
     * <ul>
     *   <li>{@link Double#NaN} values are considered equals to all other NaN values</li>
     *   <li>Infinite values are considered equal to other infinite values of the same sign</li>
     *   <li>All other values are considered equal if the absolute value of their difference is
     *       smaller than or equals to the threshold described below.</li>
     * </ul>
     *
     * If {@code relative} is {@code true}, then for any pair of values <var>v1</var><sub>j,i</sub>
     * and <var>v2</var><sub>j,i</sub> to compare, the tolerance threshold is scaled by
     * {@code max(abs(v1), abs(v2))}. Otherwise the threshold is used as-is.
     *
     * @param  m1       The first matrix to compare, or {@code null}.
     * @param  m2       The second matrix to compare, or {@code null}.
     * @param  epsilon  The tolerance value.
     * @param  relative If {@code true}, then the tolerance value is relative to the magnitude
     *         of the matrix elements being compared.
     * @return {@code true} if the values of the two matrix do not differ by a quantity
     *         greater than the given tolerance threshold.
     *
     * @see MatrixSIS#equals(Matrix, double)
     */
    public static boolean equals(final Matrix m1, final Matrix m2, final double epsilon, final boolean relative) {
        if (m1 != m2) {
            if (m1 == null || m2 == null) {
                return false;
            }
            final int numRow = m1.getNumRow();
            if (numRow != m2.getNumRow()) {
                return false;
            }
            final int numCol = m1.getNumCol();
            if (numCol != m2.getNumCol()) {
                return false;
            }
            for (int j=0; j<numRow; j++) {
                for (int i=0; i<numCol; i++) {
                    final double v1 = m1.getElement(j, i);
                    final double v2 = m2.getElement(j, i);
                    double tolerance = epsilon;
                    if (relative) {
                        tolerance *= Math.max(Math.abs(v1), Math.abs(v2));
                    }
                    if (!(Math.abs(v1 - v2) <= tolerance)) {
                        if (Numerics.equals(v1, v2)) {
                            // Special case for NaN and infinite values.
                            continue;
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Compares the given matrices for equality, using the given comparison strictness level.
     * To be considered equal, the two matrices must meet the following conditions, which depend
     * on the {@code mode} argument:
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
     * @param  m1  The first matrix to compare, or {@code null}.
     * @param  m2  The second matrix to compare, or {@code null}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both matrices are equal.
     *
     * @see MatrixSIS#equals(Object, ComparisonMode)
     */
    public static boolean equals(final Matrix m1, final Matrix m2, final ComparisonMode mode) {
        switch (mode) {
            case STRICT:          return Objects.equals(m1, m2);
            case BY_CONTRACT:     // Fall through
            case IGNORE_METADATA: return equals(m1, m2, 0, false);
            case DEBUG:           // Fall through
            case APPROXIMATIVE:   return equals(m1, m2, Numerics.COMPARISON_THRESHOLD, true);
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownEnumValue_1, mode));
        }
    }

    /**
     * Returns a unlocalized string representation of the given matrix.
     * For each column, the numbers are aligned on the decimal separator.
     *
     * {@note Formatting on a per-column basis is convenient for the kind of matrices used in referencing by coordinates,
     *        because each column is typically a displacement vector in a different dimension of the source coordinate
     *        reference system. In addition, the last column is often a translation vector having a magnitude very
     *        different than the other columns.}
     *
     * @param  matrix The matrix for which to get a string representation.
     * @return A string representation of the given matrix.
     */
    public static String toString(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        final String[] elements = new String[numRow * numCol];
        final int[] columnWidth = new int[numCol];
        final int[] maximumFractionDigits = new int[numCol];
        final int[] maximumRemainingWidth = new int[numCol]; // Minus sign (if any) + integer digits + decimal separator + 2 spaces.
        int totalWidth = 1;
        /*
         * Create now the string representation of all matrix elements and measure the width
         * of the integer field and the fraction field, then the total width of each column.
         */
        for (int i=0; i<numCol; i++) {
            for (int j=0; j<numRow; j++) {
                final String element = Double.toString(matrix.getElement(j,i));
                elements[j*numCol + i] = element;
                int width = element.length();
                int s = element.lastIndexOf('.');
                if (s >= 0) {
                    width = (maximumRemainingWidth[i] = Math.max(maximumRemainingWidth[i], ++s + MARGIN))
                          + (maximumFractionDigits[i] = Math.max(maximumFractionDigits[i], width - s));
                } else {
                    // NaN or Infinity.
                    width += MARGIN;
                }
                columnWidth[i] = Math.max(columnWidth[i], width);
            }
            totalWidth += columnWidth[i];
        }
        /*
         * Now append the formatted elements with the appropriate amount of spaces
         * and trailling zeros for each column.
         */
        final String lineSeparator = System.lineSeparator();
        final CharSequence whiteLine = CharSequences.spaces(totalWidth);
        final StringBuffer buffer = new StringBuffer((totalWidth + 2 + lineSeparator.length()) * (numRow + 2));
        buffer.append('┌').append(whiteLine).append('┐').append(lineSeparator);
        for (int k=0,j=0; j<numRow; j++) {
            buffer.append('│');
            for (int i=0; i<numCol; i++) {
                final String element = elements[k++];
                final int width = element.length();
                int s = element.lastIndexOf('.');
                buffer.append(CharSequences.spaces(s >= 0 ? maximumRemainingWidth[i] - ++s : columnWidth[i] - width)).append(element);
                s += maximumFractionDigits[i] - width;
                while (--s >= 0) {
                    buffer.append('0');
                }
            }
            buffer.append(" │").append(lineSeparator);
        }
        return buffer.append('└').append(whiteLine).append('┘').append(lineSeparator).toString();
    }
}
