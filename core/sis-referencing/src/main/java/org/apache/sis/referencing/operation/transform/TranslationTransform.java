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
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * An affine transform that translate the coordinate values by constant values.
 *
 * <div class="note"><b>Note:</b> we do not provide two-dimensional specialization because
 * {@link org.apache.sis.internal.referencing.j2d.AffineTransform2D} should be used in such case.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-176">SIS-176</a>
 *
 * @since 1.0
 * @module
 */
final class TranslationTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7382503993222285134L;

    /**
     * Translation terms, to be applied in the same order than coordinate values.
     * The length of this array is the number of target dimensions.
     */
    private final double[] offsets;

    /**
     * The error terms in double-double arithmetic, or {@code null} if none.
     * May be shorter than {@code offsets} if all remaining errors are zero.
     */
    private final double[] errors;

    /**
     * Constructs an uniform translation transform for the given offset applied on all dimensions.
     */
    TranslationTransform(final int dimension, double offset) {
        offsets = new double[dimension];
        Arrays.fill(offsets, offset);
        errors = null;
    }

    /**
     * Constructs a translation transform for the given offset vector.
     */
    TranslationTransform(final double[] offsets) {
        this.offsets = offsets.clone();
        this.errors  = null;
    }

    /**
     * Creates a transform as the inverse of the given transform.
     */
    private TranslationTransform(final TranslationTransform other) {
        offsets = negate(other.offsets);
        errors  = negate(other.errors);
        inverse = other;
    }

    /**
     * Returns a new array with negative values of given array (can be {@code null}).
     */
    private static double[] negate(double[] array) {
        if (array != null) {
            array = array.clone();
            for (int i=0; i<array.length; i++) {
                array[i] = -array[i];
            }
        }
        return array;
    }

    /**
     * Constructs a translation transform from a matrix having the given elements.
     * This constructors assumes that the matrix is square, affine and contains only
     * translation terms (this is not verified).
     */
    TranslationTransform(final int size, final double[] elements) {
        final int n = size * size;
        final int dim = size - 1;
        offsets = new double[dim];
        double[] errors = null;
        int lastError = -1;
        for (int i=0; i<dim; i++) {
            int j = dim + i*size;
            offsets[i] = elements[j];
            if ((j += n) < elements.length) {
                final double e = elements[j];
                if (e != 0) {
                    if (errors == null) {
                        errors = new double[dim];
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
        final int dim = offsets.length;
        final int numCol = getNumCol();
        final int n = getNumRow() * numCol;
        final double[] elements = new double[(errors == null) ? n : (n << 1)];
        for (int i=0; i<dim; i++) {
            int j = i*numCol;
            elements[j +    i] = 1;
            elements[j += dim] = offsets[i];
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
        return offsets.length;
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public int getTargetDimensions() {
        return offsets.length;
    }

    /**
     * Returns the matrix element at the given index.
     */
    @Override
    public double getElement(final int row, final int column) {
        final int dim = offsets.length;
        ArgumentChecks.ensureBetween("row",    0, dim, row);
        ArgumentChecks.ensureBetween("column", 0, dim, column);
        if (column == row) {
            return 1;
        } else if (column == dim) {
            return offsets[row];
        } else {
            return 0;
        }
    }

    /**
     * Tests whether this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        for (int i=0; i<offsets.length; i++) {
            if (offsets[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a single position in a list of coordinate values,
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
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the
     *                destination array. The source and destination array sections can overlap.
     * @param numPts  the number of points to be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts == dstPts) {
            final int dim = offsets.length;
            if (IterationStrategy.suggest(srcOff, dim, dstOff, dim, numPts) != IterationStrategy.ASCENDING) {
                srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dim);
                srcOff = 0;
            }
        }
        while (--numPts >= 0) {
            for (int i=0; i<offsets.length; i++) {
                dstPts[dstOff++] = srcPts[srcOff++] + offsets[i];
            }
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
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the
     *                destination array. The source and destination array sections can overlap.
     * @param numPts  the number of points to be transformed.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts == dstPts) {
            final int dim = offsets.length;
            if (IterationStrategy.suggest(srcOff, dim, dstOff, dim, numPts) != IterationStrategy.ASCENDING) {
                srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dim);
                srcOff = 0;
            }
        }
        while (--numPts >= 0) {
            for (int i=0; i<offsets.length; i++) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + offsets[i]);
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     *
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts  the number of points to be transformed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            for (int i=0; i<offsets.length; i++) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + offsets[i]);
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this matrix.
     *
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts  the number of points to be transformed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            for (int i=0; i<offsets.length; i++) {
                dstPts[dstOff++] = srcPts[srcOff++] + offsets[i];
            }
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the same everywhere.
     *
     * @param  point  ignored (can be {@code null}).
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        return Matrices.createIdentity(offsets.length);
    }

    /**
     * Invoked by {@link #inverse()} the first time that the inverse transform needs to be computed.
     */
    @Override
    final LinearTransform createInverse() {
        return new TranslationTransform(this);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return Arrays.hashCode(offsets) + 31 * super.computeHashCode();
    }

    /**
     * Compares this math transform with an object which is known to be an instance of the same class.
     */
    @Override
    protected boolean equalsSameClass(final Object object) {
        final TranslationTransform that = (TranslationTransform) object;
        return Arrays.equals(offsets, that.offsets) && Arrays.equals(errors, that.errors);
    }
}
