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
import java.util.function.IntUnaryOperator;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.internal.Arithmetic;
import org.apache.sis.util.ArgumentChecks;


/**
 * An affine transform that multiply the coordinate values by constant values, and optionally drop the last coordinates.
 * This is an optimization of {@link ProjectiveTransform} for a common case.
 *
 * <h2>Design note</h2>
 * We do not provide two-dimensional specialization because
 * {@link org.apache.sis.referencing.privy.AffineTransform2D} should be used in such case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-176">SIS-176</a>
 */
final class ScaleTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7236779710212360309L;

    /**
     * Multiplication factors, to be applied in the same order as coordinate values.
     * The length of this array is the number of target dimensions.
     */
    private final double[] factors;

    /**
     * The scale factors with potentially extended precision.
     * Zero values <em>shall</em> be represented by null elements.
     */
    private final Number[] numbers;

    /**
     * Number of coordinate values to drop after the values that we multiplied.
     * Values to drop happen for example in Geographic 3D to 2D conversions.
     */
    private final int numDroppedDimensions;

    /**
     * Constructs a scale transform for the given scale factors.
     */
    ScaleTransform(final double[] factors) {
        this.factors = factors.clone();
        this.numbers = wrap(this.factors);
        numDroppedDimensions = 0;
    }

    /**
     * Creates a transform as the inverse of the given transform.
     * This constructors assumes that the given transform does not drop any dimension.
     */
    private ScaleTransform(final ScaleTransform other) {
        final int dim = other.factors.length;
        factors = new double[dim];
        numbers = new Number[dim];
        for (int i=0; i<dim; i++) {
            factors[i] = 1 / other.factors[i];
            numbers[i] = Arithmetic.inverse(other.numbers[i]);
        }
        inverse = other;
        numDroppedDimensions = 0;
    }

    /**
     * Constructs a scale transform from a matrix having the given elements.
     * This constructors assumes that the matrix is affine and contains only
     * scale coefficients (this is not verified).
     */
    ScaleTransform(final int numRow, final int numCol, final Number[] elements) {
        numDroppedDimensions = numCol - numRow;
        numbers = new Number[numRow - 1];
        factors = store(elements, numbers, (i) -> numCol*i + i);
    }

    /**
     * Copies non-zero numbers from {@code source} to {@code target}.
     * The copies numbers are also returned as floating point values.
     *
     * @param  source    the numbers to copy. This array will not be modified.
     * @param  target    where to store the non-zero numbers.
     * @param  indices   maps array index from {@code target} to {@code source}.
     * @return a copy of {@code target} as floating point numbers.
     */
    static double[] store(final Number[] source, final Number[] target, final IntUnaryOperator indices) {
        final double[] values = new double[target.length];
        for (int i=0; i<target.length; i++) {
            final Number value = source[indices.applyAsInt(i)];
            if (value != null) {
                values[i] = value.doubleValue();               // Unconditional store because may be -0.
                if (!ExtendedPrecisionMatrix.isZero(value)) {
                    target[i] = value;
                }
            }
        }
        return values;
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major array.
     * Zero values <em>shall</em> be null. Callers can write in the returned array.
     */
    @Override
    public Number[] getElementAsNumbers(final boolean writable) {
        final int numCol = getNumCol();
        final Number[] elements = new Number[getNumRow() * numCol];
        for (int i=0; i<numbers.length; i++) {
            elements[numCol*i + i] = numbers[i];
        }
        elements[elements.length - 1] = 1;
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
     * Retrieves the value at the specified row and column of the matrix.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     */
    @Override
    public final Number getElementOrNull(final int row, final int column) {
        final int dstDim = numbers.length;
        final int srcDim = dstDim + numDroppedDimensions;
        ArgumentChecks.ensureBetween("row",    0, dstDim, row);
        ArgumentChecks.ensureBetween("column", 0, srcDim, column);
        if (row == dstDim) {
            if (column == srcDim) return 1;
        } else if (row == column) {
            return numbers[row];
        }
        return null;
    }

    /**
     * Tests whether this transform does not move any points.
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
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts  the number of points to be transformed.
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
     * @param srcPts  the array containing the source point coordinates.
     * @param srcOff  the offset to the first point to be transformed in the source array.
     * @param dstPts  the array into which the transformed point coordinates are returned.
     * @param dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts  the number of points to be transformed.
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
     * Invoked by {@link #inverse()} the first time that the inverse transform needs to be computed.
     */
    @Override
    final LinearTransform createInverse() throws NoninvertibleTransformException {
        if (numDroppedDimensions == 0) {
            return new ScaleTransform(this);
        }
        return super.createInverse();
    }

    /**
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the same everywhere.
     *
     * @param  point  ignored (can be {@code null}).
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
     * @hidden because nothing new to said.
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
               && Arrays.equals(numbers, that.numbers);
    }
}
