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
package org.apache.sis.referencing.operation.transform;

import java.util.Arrays;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * An affine transform that multiply the ordinate values by constant values, and optionally drop the last ordinates.
 * This is an optimization of {@link ProjectiveTransform} for a common case.
 *
 * <div class="note"><b>Note:</b> we do not provide two-dimensional specialization because
 * {@link org.apache.sis.internal.referencing.j2d.AffineTransform2D} should be used in such case.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-176">SIS-176</a>
 */
final class ScaleTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8527439133082104085L;

    /**
     * Multiplication factors, to be applied in the same order than ordinate values.
     * The length of this array is the number of target dimensions.
     */
    private final double[] factors;

    /**
     * The error terms in double-double arithmetic, or {@code null} if none.
     * May be shorter than {@code factors} if all remaining errors are zero.
     */
    private final double[] errors;

    /**
     * Number of ordinate values to drop after the values that we multiplied.
     * Values to drop happen for example in Geographic 3D to 2D conversions.
     */
    private final int numDroppedDimensions;

    /**
     * Constructs a scale transform from a matrix having the given elements.
     * This constructors assumes that the matrix is affine and contains only
     * scale coefficients (this is not verified).
     */
    ScaleTransform(final int numRow, final int numCol, final double[] elements) {
        numDroppedDimensions = numCol - numRow;
        final int n = numRow * numCol;
        factors = new double[numRow - 1];
        double[] errors = null;
        int lastError = -1;
        for (int i=0; i<factors.length; i++) {
            int j = numCol*i + i;
            factors[i] = elements[j];
            if ((j += n) < elements.length) {
                final double e = elements[j];
                if (e != 0) {
                    if (errors == null) {
                        errors = new double[numRow];
                    }
                    errors[i] = e;
                    lastError = i;
                }
            }
        }
        this.errors = ArraysExt.resize(errors, lastError + 1);
    }

    /**
     * Returns a copy of matrix elements, including error terms if any.
     */
    @Override
    public double[] getExtendedElements() {
        final int numCol = getNumCol();
        final int n = getNumRow() * numCol;
        final double[] elements = new double[(errors == null) ? n : (n << 1)];
        for (int i=0; i<factors.length; i++) {
            final int j = numCol*i + i;
            elements[j] = factors[i];
            if (errors != null && i < errors.length) {
                elements[j + n] = errors[i];
            }
        }
        elements[n - 1] = 1;
        return elements;
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public int getSourceDimensions() {
        return factors.length + numDroppedDimensions;
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public int getTargetDimensions() {
        return factors.length;
    }

    /**
     * Returns the matrix element at the given index.
     */
    @Override
    public double getElement(final int row, final int column) {
        final int dstDim = factors.length;
        final int srcDim = dstDim + numDroppedDimensions;
        ArgumentChecks.ensureBetween("row",    0, dstDim, row);
        ArgumentChecks.ensureBetween("column", 0, srcDim, column);
        if (row == dstDim) {
            return (column == srcDim) ? 1 : 0;
        } else {
            return (row == column) ? factors[row] : 0;
        }
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * <div class="note"><b>Note:</b> this method should always returns {@code false}, since
     * {@code MathTransforms.linear(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this class directly
     * instead than using a factory method.</div>
     */
    @Override
    public boolean isIdentity() {
        if (numDroppedDimensions != 0) {
            return false;
        }
        for (int i=0; i<factors.length; i++) {
            if (factors[i] != 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a single coordinate point in a list of ordinal values,
     * and optionally computes the derivative at that location.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        transform(srcPts, srcOff, dstPts, dstOff, 1);
        return derivate ? derivative((DirectPosition) null) : null;
    }

    /**
     * Transforms an array of floating point coordinates by this matrix. Point coordinates must have a dimension
     * equal to <code>{@link Matrix#getNumCol}-1</code>. For example, for square matrix of size 4×4, coordinate
     * points are three-dimensional and stored in the arrays starting at the specified offset ({@code srcOff}) in
     * the order
     * <code>[x₀, y₀, z₀,
     *        x₁, y₁, z₁...,
     *        x<sub>n</sub>, y<sub>n</sub>, z<sub>n</sub>]</code>.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the
     *               destination array. The source and destination array sections can overlap.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts == dstPts) {
            final int dstDim = factors.length;
            final int srcDim = dstDim + numDroppedDimensions;
            if (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts) != IterationStrategy.ASCENDING) {
                srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcDim);
                srcOff = 0;
            }
        }
        while (--numPts >= 0) {
            for (int i=0; i<factors.length; i++) {
                dstPts[dstOff++] = srcPts[srcOff++] * factors[i];
            }
            srcOff += numDroppedDimensions;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix. Point coordinates must have a dimension
     * equal to <code>{@link Matrix#getNumCol()} - 1</code>. For example, for square matrix of size 4×4, coordinate
     * points are three-dimensional and stored in the arrays starting at the specified offset ({@code srcOff})
     * in the order
     * <code>[x₀, y₀, z₀,
     *        x₁, y₁, z₁...,
     *        x<sub>n</sub>, y<sub>n</sub>, z<sub>n</sub>]</code>.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the
     *               destination array. The source and destination array sections can overlap.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts == dstPts) {
            final int dstDim = factors.length;
            final int srcDim = dstDim + numDroppedDimensions;
            if (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts) != IterationStrategy.ASCENDING) {
                srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcDim);
                srcOff = 0;
            }
        }
        while (--numPts >= 0) {
            for (int i=0; i<factors.length; i++) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] * factors[i]);
            }
            srcOff += numDroppedDimensions;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            for (int i=0; i<factors.length; i++) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] * factors[i]);
            }
            srcOff += numDroppedDimensions;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            for (int i=0; i<factors.length; i++) {
                dstPts[dstOff++] = srcPts[srcOff++] * factors[i];
            }
            srcOff += numDroppedDimensions;
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the same everywhere.
     *
     * @param point Ignored (can be {@code null}).
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        final int n = factors.length;
        final MatrixSIS matrix = Matrices.createZero(n, n + numDroppedDimensions);
        for (int i=0; i<n; i++) {
            matrix.setElement(i, i, factors[i]);
        }
        return matrix;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return Arrays.hashCode(factors) + 31 * super.computeHashCode();
    }

    /**
     * Compares this math transform with an object which is known to be an instance of the same class.
     */
    @Override
    protected boolean equalsSameClass(final Object object) {
        final ScaleTransform that = (ScaleTransform) object;
        return numDroppedDimensions == that.numDroppedDimensions
               && Arrays.equals(factors, that.factors)
               && Arrays.equals(errors,  that.errors);
    }
}
