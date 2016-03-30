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


/**
 * A usually affine, or otherwise a projective transform for the generic cases.
 * This implementation is used for cases other than identity, 1D, 2D or axis swapping.
 *
 * <p>A projective transform is capable of mapping an arbitrary quadrilateral into another arbitrary quadrilateral,
 * while preserving the straightness of lines. In the special case where the transform is affine, the parallelism of
 * lines in the source is preserved in the output.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 *
 * @see java.awt.geom.AffineTransform
 */
class ProjectiveTransform extends AbstractLinearTransform implements ExtendedPrecisionMatrix {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2104496465933824935L;

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
     *
     * <p>This array may have twice the normal length ({@link #numRow} × {@link #numCol}),
     * in which case the second half contains the error terms in double-double arithmetic.</p>
     */
    private final double[] elt;

    /**
     * Constructs a transform from the specified matrix.
     * The matrix is usually square and affine, but this is not enforced.
     *
     * @param matrix The matrix.
     */
    protected ProjectiveTransform(final Matrix matrix) {
        numRow = matrix.getNumRow();
        numCol = matrix.getNumCol();
        if (matrix instanceof ExtendedPrecisionMatrix) {
            elt = ((ExtendedPrecisionMatrix) matrix).getExtendedElements();
            assert (elt.length % (numRow * numCol)) == 0;
        } else {
            elt = new double[numRow * numCol];
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                for (int i=0; i<numCol; i++) {
                    elt[mix++] = matrix.getElement(j,i);
                }
            }
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
        for (int i = 0; i != numCol;) {
            if (elt[n + i] != (++i == numCol ? 1 : 0)) {
                return this;    // Transform is not affine (ignoring if square or not).
            }
        }
        /*
         * Note: we could check for CopyTransform case here, but this check is rather done in
         * MathTransforms.linear(Matrix) in order to avoid ProjectiveTransform instantiation.
         */
        for (int i=0; i<n; i++) {
            // Non-zero elements are allowed to appear only on the diagonal.
            if (elt[i] != 0 && (i / numCol) != (i % numCol)) {
                return this;
            }
        }
        return new ScaleTransform(numRow, numCol, elt);
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
     * Returns a copy of matrix elements, including error terms if any.
     */
    @Override
    public final double[] getExtendedElements() {
        return elt.clone();
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
     * instead than using a factory method.</div>
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
     * Converts a single coordinate point in a list of ordinal values,
     * and optionally computes the derivative at that location.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final Matrix transform(final double[] srcPts, final int srcOff,
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
    public final void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        int srcInc = srcDim = numCol - 1; // The last ordinate will be assumed equal to 1.
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
                    if (e != 0) {
                        /*
                         * The purpose of the test for non-zero value is not performance (it is actually more likely
                         * to slow down the calculation), but to get a valid sum even if some source ordinates are NaN.
                         * This occurs when the ProjectiveTransform is used for excluding some dimensions, for example
                         * getting 2D points from 3D points. In such case, the fact that the excluded dimensions had
                         * NaN values should not force the retained dimensions to get NaN values.
                         */
                        sum += srcPts[srcOff + i] * e;
                    }
                }
                buffer[j] = sum;
                mix++;
            }
            final double w = buffer[dstDim];
            for (int j=0; j<dstDim; j++) {
                // 'w' is equal to 1 if the transform is affine.
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
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the
     *               destination array. The source and destination array sections can overlap.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        final int srcDim, dstDim;
        int srcInc = srcDim = numCol-1;
        int dstInc = dstDim = numRow-1;
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
                    if (e != 0) { // See comment in transform(double[], ...)
                        sum += srcPts[srcOff + i] * e;
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
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed.
     */
    @Override
    public final void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        final int srcDim = numCol-1;
        final int dstDim = numRow-1;
        final double[] buffer = new double[numRow];
        while (--numPts >= 0) {
            int mix = 0;
            for (int j=0; j<numRow; j++) {
                double sum = elt[mix + srcDim];
                for (int i=0; i<srcDim; i++) {
                    final double e = elt[mix++];
                    if (e != 0) { // See comment in transform(double[], ...)
                        sum += srcPts[srcOff + i] * e;
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
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of points to be transformed.
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
                    if (e != 0) { // See comment in transform(double[], ...)
                        sum += srcPts[srcOff + i] * e;
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
     * Gets the derivative of this transform at a point.
     * For a matrix transform, the derivative is the same everywhere.
     *
     * @param point Ignored (can be {@code null}).
     */
    @Override
    public final Matrix derivative(final DirectPosition point) {
        final int srcDim = numCol - 1;
        final int dstDim = numRow - 1;
        final MatrixSIS matrix = Matrices.createZero(dstDim, srcDim);
        int mix = 0;
        for (int j=0; j<dstDim; j++) {
            for (int i=0; i<srcDim; i++) {
                matrix.setElement(j, i, elt[mix++]);
            }
            mix++; // Skip translation column.
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
        return this.numRow == that.numRow &&
               this.numCol == that.numCol &&
               Arrays.equals(this.elt, that.elt);
    }
}
