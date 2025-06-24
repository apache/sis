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
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.internal.Arithmetic;
import org.apache.sis.util.ArgumentChecks;


/**
 * An affine transform that translate the coordinate values by constant values.
 *
 * <h2>Design note</h2>
 * We do not provide two-dimensional specialization because
 * {@link org.apache.sis.referencing.privy.AffineTransform2D} should be used in such case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-176">SIS-176</a>
 */
final class TranslationTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3713820636959453961L;

    /**
     * Translation terms, to be applied in the same order as coordinate values.
     * The length of this array is the number of target dimensions.
     */
    private final double[] offsets;

    /**
     * The offsets with potentially extended precision.
     * Zero values <em>shall</em> be represented by null elements.
     */
    private final Number[] numbers;

    /**
     * Constructs an uniform translation transform for the given offset applied on all dimensions.
     */
    TranslationTransform(final int dim, final double offset) {
        offsets = new double[dim];
        numbers = new Double[dim];
        Arrays.fill(offsets, offset);       // Unconditional because the offset may be -0.
        if (offset != 0) {
            Arrays.fill(numbers, offset);
        }
    }

    /**
     * Constructs a translation transform for the given offset vector.
     */
    TranslationTransform(final double[] offsets) {
        this.offsets = offsets.clone();
        this.numbers = wrap(this.offsets);
    }

    /**
     * Creates a transform as the inverse of the given transform.
     */
    private TranslationTransform(final TranslationTransform other) {
        final int dim = other.offsets.length;
        offsets = new double[dim];
        numbers = new Number[dim];
        for (int i=0; i<dim; i++) {
            offsets[i] = -other.offsets[i];
            numbers[i] = Arithmetic.negate(other.numbers[i]);
        }
        inverse = other;
    }

    /**
     * Constructs a translation transform from a matrix having the given elements.
     * This constructors assumes that the matrix is square, affine and contains only
     * translation terms (this is not verified).
     */
    TranslationTransform(final int size, final Number[] elements) {
        final int dim = size - 1;
        numbers = new Number[dim];
        offsets = ScaleTransform.store(elements, numbers, (i) -> dim + i*size);
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major array.
     * Zero values <em>shall</em> be null. Callers can write in the returned array.
     */
    @Override
    public Number[] getElementAsNumbers(final boolean writable) {
        final int dim  = numbers.length;
        final int size = dim + 1;
        final Number[] elements = new Number[size * size];
        for (int i=0; i<dim; i++) {
            int j = i * size;
            elements[j + i]   = 1;
            elements[j + dim] = numbers[i];
        }
        elements[elements.length - 1] = 1;
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
     * Retrieves the value at the specified row and column of the matrix.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     */
    @Override
    public final Number getElementOrNull(final int row, final int column) {
        final int dim = numbers.length;
        ArgumentChecks.ensureBetween("row",    0, dim, row);
        ArgumentChecks.ensureBetween("column", 0, dim, column);
        return (column == row) ? 1 : (column == dim) ? numbers[row] : null;
    }

    /**
     * Tests whether this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        for (int i=0; i<numbers.length; i++) {
            if (numbers[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a single position in a sequence of coordinate tuples,
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
     * @hidden because nothing new to said.
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
        return Arrays.equals(offsets, that.offsets) && Arrays.equals(numbers, that.numbers);
    }
}
