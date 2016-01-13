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
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;


/**
 * A transform which copy the ordinates in the source array to different locations in the target array.
 * This is a special case of {@link ProjectiveTransform} where the matrix coefficients are zero everywhere,
 * except one value by row which is set to 1 and is not the translation term. Those transforms are used for
 * swapping axis order, or selecting the dimension to retain when converting from a large dimension to a smaller one.
 * This transform has the particularity to involve no floating point operation - just copy of values with no change -
 * and consequently works well with NaN ordinate values.
 *
 * <p>We do not provide a subclass for the 2D case because our policy is to use
 * an {@link java.awt.geom.AffineTransform} for every 2D affine conversions.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
final class CopyTransform extends AbstractLinearTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5457032501070947956L;

    /**
     * The dimension of source coordinates.
     * Must be greater than the highest value in {@link #indices}.
     */
    private final int srcDim;

    /**
     * The indices of ordinates to copy in the source array.
     * The length of this array is the target dimension.
     */
    private final int[] indices;

    /**
     * Creates a new transform.
     *
     * @param srcDim The dimension of source coordinates.
     *        Must be greater than the highest value in {@code indices}.
     * @param indices The indices of ordinates to copy in the source array.
     *        The length of this array is the target dimension.
     */
    CopyTransform(final int srcDim, final int[] indices) {
        this.srcDim  = srcDim;
        this.indices = indices;
    }

    /**
     * If a transform can be created from the given matrix, returns it.
     * Otherwise returns {@code null}.
     */
    static CopyTransform create(final Matrix matrix) {
        final int srcDim = matrix.getNumCol() - 1;
        final int dstDim = matrix.getNumRow() - 1;
        for (int i=0; i <= srcDim; i++) {
            if (matrix.getElement(dstDim, i) != (i == srcDim ? 1 : 0)) {
                // Not an affine transform (ignoring if square or not).
                return null;
            }
        }
        final int[] indices = new int[dstDim];
        for (int j=0; j<dstDim; j++) {
            if (matrix.getElement(j, srcDim) != 0) {
                // The matrix has translation terms.
                return null;
            }
            boolean found = false;
            for (int i=0; i<srcDim; i++) {
                final double elt = matrix.getElement(j, i);
                if (elt != 0) {
                    if (elt != 1 || found) {
                        // Not a simple copy operation.
                        return null;
                    }
                    indices[j] = i;
                    found = true;
                }
            }
            if (!found) {
                // Target ordinate unconditionally set to 0 (not a copy).
                return null;
            }
        }
        return new CopyTransform(srcDim, indices);
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public int getSourceDimensions() {
        return srcDim;
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public int getTargetDimensions() {
        return indices.length;
    }

    /**
     * Returns {@code true} if this transform is affine.
     */
    @Override
    public boolean isAffine() {
        return srcDim == indices.length;
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
        if (srcDim != indices.length) {
            return false;
        }
        for (int i=indices.length; --i>=0;) {
            if (indices[i] != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transforms a single coordinate in a list of ordinal values, and optionally returns
     * the derivative at that location.
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
     * Transforms an array of floating point coordinates by this matrix.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        final int[] indices = this.indices;
        int srcInc = srcDim = this.srcDim;
        int dstInc = dstDim = indices.length;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcDim;
                    dstOff += (numPts-1) * dstDim;
                    srcInc = -srcInc;
                    dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcDim);
                    srcOff = 0;
                    break;
                }
            }
        }
        if (srcPts != dstPts) {
            // Optimisation for a common case.
            while (--numPts >= 0) {
                for (int i=0; i<dstDim; i++) {
                    dstPts[dstOff++] = srcPts[srcOff + indices[i]];
                }
                srcOff += srcDim;
            }
        } else {
            // General case: there is a risk that two coordinates overlap.
            final double[] buffer = new double[dstDim];
            while (--numPts >= 0) {
                for (int i=0; i<dstDim; i++) {
                    buffer[i] = srcPts[srcOff + indices[i]];
                }
                System.arraycopy(buffer, 0, dstPts, dstOff, dstDim);
                srcOff += srcInc;
                dstOff += dstInc;
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        final int[] indices = this.indices;
        int srcInc = srcDim = this.srcDim;
        int dstInc = dstDim = indices.length;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcDim;
                    dstOff += (numPts-1) * dstDim;
                    srcInc = -srcInc;
                    dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcDim);
                    srcOff = 0;
                    break;
                }
            }
        }
        if (srcPts != dstPts) {
            // Optimisation for a common case.
            while (--numPts >= 0) {
                for (int i=0; i<dstDim; i++) {
                    dstPts[dstOff++] = srcPts[srcOff + indices[i]];
                }
                srcOff += srcDim;
            }
        } else {
            // General case: there is a risk that two coordinates overlap.
            final float[] buffer = new float[dstDim];
            while (--numPts >= 0) {
                for (int i=0; i<dstDim; i++) {
                    buffer[i] = srcPts[srcOff + indices[i]];
                }
                System.arraycopy(buffer, 0, dstPts, dstOff, dstDim);
                srcOff += srcInc;
                dstOff += dstInc;
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        final int[] indices = this.indices;
        final int srcDim = this.srcDim;
        final int dstDim = indices.length;
        while (--numPts >= 0) {
            for (int i=0; i<dstDim; i++) {
                dstPts[dstOff++] = (float) srcPts[srcOff + indices[i]];
            }
            srcOff += srcDim;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        final int[] indices = this.indices;
        final int srcDim = this.srcDim;
        final int dstDim = indices.length;
        while (--numPts >= 0) {
            for (int i=0; i<dstDim; i++) {
                dstPts[dstOff++] = srcPts[srcOff + indices[i]];
            }
            srcOff += srcDim;
        }
    }

    /**
     * Returns the matrix element at the given row and column.
     *
     * @throws IndexOutOfBoundsException if {@code row} is out of bounds.
     */
    @Override
    public double getElement(final int row, final int column) {
        return (((row == indices.length) ? srcDim : indices[row]) == column) ? 1 : 0;
    }

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the same everywhere.
     *
     * @param point Ignored (can be {@code null}).
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        final MatrixSIS matrix = Matrices.createZero(indices.length, srcDim);
        for (int j=0; j<indices.length; j++) {
            matrix.setElement(j, indices[j], 1);
        }
        return matrix;
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    @SuppressWarnings("DoubleCheckedLocking")  // Okay since 'inverse' is volatile.
    public LinearTransform inverse() throws NoninvertibleTransformException {
        LinearTransform inv = inverse;
        if (inv == null) {
            synchronized (this) {
                inv = inverse;
                if (inv == null) {
                    /*
                     * Note: no need to perform the following check at this point because MathTransforms.linear(…)
                     *       should never instantiate this class in the identity case and because we perform an
                     *       equivalent check later anyway.
                     *
                     *       if (isIdentity()) {
                     *           inverse = this;
                     *       } else { ... }
                     */
                    final int srcDim = this.srcDim;
                    final int dstDim = indices.length;
                    final int[] reverse = new int[srcDim];
                    Arrays.fill(reverse, -1);
                    for (int i=dstDim; --i>=0;) {
                        reverse[indices[i]] = i;
                    }
                    /*
                     * Check if there is any unassigned dimension. In such case,
                     * delegates to the generic ProjectiveTransform with a matrix
                     * which set the missing values to NaN.
                     */
                    for (int j=srcDim; --j>=0;) {
                        if (reverse[j] < 0) {
                            final MatrixSIS matrix = Matrices.createZero(srcDim + 1, dstDim + 1);
                            for (j=0; j<srcDim; j++) {      // Okay to reuse 'j' since the outer loop will not continue.
                                final int i = reverse[j];
                                if (i >= 0) {
                                    matrix.setElement(j, i, 1);
                                } else {
                                    matrix.setElement(j, dstDim, Double.NaN);
                                }
                            }
                            matrix.setElement(srcDim, dstDim, 1);
                            inv = MathTransforms.linear(matrix);
                            if (inv instanceof AbstractLinearTransform) {
                                ((AbstractLinearTransform) inv).inverse = this;
                            }
                            inverse = inv;
                            return inv;
                        }
                    }
                    /*
                     * At this point, we know that we can create the inverse transform.
                     * If this transform is the identity transform (we should never happen,
                     * but we are paranoiac), then the old and new arrays would be equal.
                     */
                    CopyTransform copyInverse = this;
                    if (!Arrays.equals(reverse, indices)) {
                        copyInverse = new CopyTransform(indices.length, reverse);
                        copyInverse.inverse = this;
                    }
                    inverse = inv = copyInverse;
                }
            }
        }
        return inv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return Arrays.hashCode(indices) + 31 * super.computeHashCode();
    }

    /**
     * Compares this math transform with an object which is known to be an instance of the same class.
     */
    @Override
    protected boolean equalsSameClass(final Object object) {
        final CopyTransform that = (CopyTransform) object;
        return srcDim == that.srcDim && Arrays.equals(indices, that.indices);
    }
}
