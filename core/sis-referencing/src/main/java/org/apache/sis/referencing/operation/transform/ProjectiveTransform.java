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
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;


/**
 * A usually affine, or otherwise a projective transform for the generic cases.
 * This implementation is used for cases other than identity, translation only,
 * scale only, 1D transform, 2D transform or axis swapping.
 *
 * <p>A projective transform is capable of mapping an arbitrary quadrilateral into another arbitrary quadrilateral,
 * while preserving the straightness of lines. In the special case where the transform is affine, the parallelism of
 * lines in the source is preserved in the output.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see java.awt.geom.AffineTransform
 *
 * @since 0.5
 */
class ProjectiveTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5616239625370371256L;

    /**
     * The number of rows.
     */
    private final int numRow;

    /**
     * The number of columns.
     */
    private final int numCol;

    /**
     * Elements of the matrix. Column indices vary fastest.
     */
    private final double[] elt;

    /**
     * Same numbers than {@link #elt} with potentially extended precision.
     * Zero values <em>shall</em> be represented by null elements.
     */
    private final Number[] numbers;

    /**
     * Constructs a transform from the specified matrix.
     * The matrix is usually square and affine, but this is not enforced.
     *
     * @param matrix  the matrix.
     */
    protected ProjectiveTransform(final Matrix matrix) {
        numRow = matrix.getNumRow();
        numCol = matrix.getNumCol();
        Number[] numbers = null;
        if (matrix instanceof ExtendedPrecisionMatrix) {
            // Use `writable = true` because we need a copy protected from changes.
            numbers = ((ExtendedPrecisionMatrix) matrix).getElementAsNumbers(true);
        }
        if (matrix instanceof MatrixSIS) {
            final MatrixSIS m = (MatrixSIS) matrix;
            elt = m.getElements();
            if (elt.length != numRow * numCol) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
            if (numbers == null) {
                numbers = new Number[elt.length];
                for (int i=0; i<numbers.length; i++) {
                    final Number element = m.getNumber(i / numCol, i % numCol);
                    if (!ExtendedPrecisionMatrix.isZero(element)) {
                        numbers[i] = element;
                    }
                }
            }
        } else {
            elt = new double[numRow * numCol];
            for (int i=0; i<elt.length; i++) {
                elt[i] = matrix.getElement(i / numCol, i % numCol);
            }
            if (numbers == null) {
                numbers = wrap(elt);
            }
        }
        this.numbers = numbers;
        if (elt.length != numbers.length) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
        }
    }

    /**
     * If a more efficient implementation of this math transform can be used, returns it.
     * Otherwise returns {@code this} unchanged.
     */
    final LinearTransform optimize() {
        if (numCol < numRow) {
            return this;
        }
        final int n = (numRow - 1) * numCol;
        for (int i = 0; i < numCol;) {
            if (elt[n + i] != (++i == numCol ? 1 : 0)) {
                return this;            // Transform is not affine (ignoring if square or not).
            }
        }
        /*
         * Note: we could check for CopyTransform case here, but this check is rather done in
         * MathTransforms.linear(Matrix) in order to avoid ProjectiveTransform instantiation.
         *
         * Check which elements are non-zero. For ScaleTransform, they must be on the diagonal.
         * For TranslationTransform, they must be in the last column. Note that the transform
         * should not be identity (except for testing purpose) since the identity case should
         * have been handled by MathTransforms.linear(Matrix) before to reach this point.
         */
        boolean isScale       = true;                       // ScaleTransform accepts non-square matrix.
        boolean isTranslation = (numRow == numCol);         // TranslationTransform is restricted to square matrix.
        final int lastColumn  = numCol - 1;
        for (int i=0; i<n; i++) {
            final double v = elt[i];
            final int row  = (i / numCol);
            final int col  = (i % numCol);
            isScale       &= (col == row)        || (v == 0);
            isTranslation &= (col == lastColumn) || (v == (col == row ? 1 : 0));
            if (!(isScale | isTranslation)) {
                return this;
            }
        }
        if (isTranslation) {
            return new TranslationTransform(numRow, numbers);
        } else {
            return new ScaleTransform(numRow, numCol, numbers);
        }
    }

    /**
     * Gets the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return numCol - 1;
    }

    /**
     * Gets the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return numRow - 1;
    }

    /**
     * Gets the number of rows in the matrix.
     */
    @Override
    public final int getNumRow() {
        return numRow;
    }

    /**
     * Gets the number of columns in the matrix.
     */
    @Override
    public final int getNumCol() {
        return numCol;
    }

    /**
     * Returns a copy of all matrix elements in a flat, row-major array. Zero values <em>shall</em> be null.
     * Callers can write in the returned array if and only if the {@code writable} argument is {@code true}.
     */
    @Override
    public final Number[] getElementAsNumbers(final boolean writable) {
        return writable ? numbers.clone() : numbers;
    }

    /**
     * Retrieves the value at the specified row and column of the matrix.
     * If the value is zero, then this method <em>shall</em> return {@code null}.
     */
    @Override
    public final Number getElementOrNull(final int row, final int column) {
        ArgumentChecks.ensureBetween("row",    0, numRow - 1, row);
        ArgumentChecks.ensureBetween("column", 0, numCol - 1, column);
        return numbers[row * numCol + column];
    }

    /**
     * Returns the matrix element at the given index.
     */
    @Override
    public final double getElement(final int row, final int column) {
        ArgumentChecks.ensureBetween("row",    0, numRow - 1, row);
        ArgumentChecks.ensureBetween("column", 0, numCol - 1, column);
        return elt[row * numCol + column];
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * <div class="note"><b>Note:</b> this method should always returns {@code false}, since
     * {@code MathTransforms.linear(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this class directly
     * instead of using a factory method.</div>
     */
    @Override
    public final boolean isIdentity() {
        if (numRow != numCol) {
            return false;
        }
        int mix = 0;
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                if (elt[mix++] != (i == j ? 1 : 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Converts a single coordinate tuple in a list of coordinate values,
     * and optionally computes the derivative at that location.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final Matrix transform(final double[] srcPts, final int srcOff,
                                  final double[] dstPts, final int dstOff,
                                  final boolean derivate)
    {
        if (!derivate) {
            transform(srcPts, srcOff, dstPts, dstOff, 1);
            return null;
        }
        // A non-null position is required in case this transform is non-affine.
        Matrix derivative = derivative(new DirectPositionView.Double(srcPts, srcOff, getSourceDimensions()));
        transform(srcPts, srcOff, dstPts, dstOff, 1);
        return derivative;
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
    public final void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        int srcInc = srcDim = numCol - 1;               // The last coordinate will be assumed equal to 1.
        int dstInc = dstDim = numRow - 1;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * srcDim;
                    dstOff += (numPts - 1) * dstDim;
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
        final double[] buffer = new double[numRow];
        while (--numPts >= 0) {
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                double sum = elt[mix + srcDim];         // Initialize to translation term.
                for (int i=0; i<srcDim; i++) {
                    final double e = elt[mix++];
                    if (e != 0) {
                        /*
                         * The purpose of the test for non-zero value is not performance (it is actually more likely
                         * to slow down the calculation), but to get a valid sum even if some source coordinates are NaN.
                         * This occurs when the ProjectiveTransform is used for excluding some dimensions, for example
                         * getting 2D points from 3D points. In such case, the fact that the excluded dimensions had
                         * NaN values should not force the retained dimensions to get NaN values.
                         */
                        if (Formulas.USE_FMA) {
                            sum = Math.fma(srcPts[srcOff + i], e, sum);
                        } else {
                            sum += srcPts[srcOff + i] * e;
                        }
                    }
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[dstDim];
            for (int j=0; j<dstDim; j++) {
                // `w` is equal to 1 if the transform is affine.
                dstPts[dstOff + j] = buffer[j] / w;
            }
            srcOff += srcInc;
            dstOff += dstInc;
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
    public final void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        int srcInc = srcDim = numCol - 1;
        int dstInc = dstDim = numRow - 1;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * srcDim;
                    dstOff += (numPts - 1) * dstDim;
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
        final double[] buffer = new double[numRow];
        while (--numPts >= 0) {
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                double sum = elt[mix + srcDim];
                for (int i=0; i<srcDim; i++) {
                    final double e = elt[mix++];
                    if (e != 0) {                                   // See comment in transform(double[], ...)
                        if (Formulas.USE_FMA) {
                            sum = Math.fma(srcPts[srcOff + i], e, sum);
                        } else {
                            sum += srcPts[srcOff + i] * e;
                        }
                    }
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[dstDim];
            for (int j=0; j<dstDim; j++) {
                dstPts[dstOff + j] = (float) (buffer[j] / w);
            }
            srcOff += srcInc;
            dstOff += dstInc;
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
    public final void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        final int srcDim = numCol - 1;
        final int dstDim = numRow - 1;
        final double[] buffer = new double[numRow];
        while (--numPts >= 0) {
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                double sum = elt[mix + srcDim];
                for (int i=0; i<srcDim; i++) {
                    final double e = elt[mix++];
                    if (e != 0) {                                   // See comment in transform(double[], ...)
                        if (Formulas.USE_FMA) {
                            sum = Math.fma(srcPts[srcOff + i], e, sum);
                        } else {
                            sum += srcPts[srcOff + i] * e;
                        }
                    }
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[dstDim];
            for (int j=0; j<dstDim; j++) {
                dstPts[dstOff++] = (float) (buffer[j] / w);
            }
            srcOff += srcDim;
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
    public final void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        final int srcDim = numCol - 1;
        final int dstDim = numRow - 1;
        final double[] buffer = new double[numRow];
        while (--numPts >= 0) {
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                double sum = elt[mix + srcDim];
                for (int i=0; i<srcDim; i++) {
                    final double e = elt[mix++];
                    if (e != 0) {                                   // See comment in transform(double[], ...)
                        if (Formulas.USE_FMA) {
                            sum = Math.fma(srcPts[srcOff + i], e, sum);
                        } else {
                            sum += srcPts[srcOff + i] * e;
                        }
                    }
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[dstDim];
            for (int j=0; j<dstDim; j++) {
                dstPts[dstOff++] = buffer[j] / w;
            }
            srcOff += srcDim;
        }
    }

    /**
     * Gets the derivative of this transform at a point. For an affine transform,
     * the derivative is the same everywhere and the given point is ignored.
     * For a perspective transform, the given point is used.
     *
     * @param  point  the coordinate tuple where to evaluate the derivative.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) {
        final int srcDim = numCol - 1;
        final int dstDim = numRow - 1;
        /*
         * In the `transform(…)` method, all coordinate values are divided by a `w` coefficient
         * which depends on the position. We need to reproduce that division here. Note that `w`
         * coefficient is different than 1 only if the transform is non-affine.
         */
        int mix = dstDim * numCol;
        double w = elt[mix + srcDim];                   // `w` is equal to 1 if the transform is affine.
        for (int i=0; i<srcDim; i++) {
            final double e = elt[mix++];
            if (e != 0) {                               // For avoiding NullPointerException if affine.
                w += point.getOrdinate(i) * e;
            }
        }
        /*
         * In the usual affine case (w=1), the derivative is a copy of this matrix
         * with last row and last column omitted.
         */
        mix = 0;
        final MatrixSIS matrix = Matrices.createZero(dstDim, srcDim);
        for (int j=0; j<dstDim; j++) {
            for (int i=0; i<srcDim; i++) {
                matrix.setElement(j, i, elt[mix++] / w);
            }
            mix++;                                      // Skip translation column.
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
        return Arrays.hashCode(elt) + 31 * super.computeHashCode();
    }

    /**
     * Compares this math transform with an object which is known to be an instance of the same class.
     */
    @Override
    protected boolean equalsSameClass(final Object object) {
        final ProjectiveTransform that = (ProjectiveTransform) object;
        return numRow == that.numRow &&
               numCol == that.numCol &&
               Arrays.equals(elt, that.elt) &&
               Arrays.equals(numbers, that.numbers);
    }
}
