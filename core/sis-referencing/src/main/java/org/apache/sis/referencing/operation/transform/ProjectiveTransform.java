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
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.parameter.TensorParameters;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A usually affine, or otherwise a projective transform for the generic cases.
 * This implementation is used for cases other than identity, 1D, 2D or axis swapping.
 *
 * <p>A projective transform is capable of mapping an arbitrary quadrilateral into another arbitrary quadrilateral,
 * while preserving the straightness of lines. In the special case where the transform is affine, the parallelism of
 * lines in the source is preserved in the output.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5 (derived from geotk-1.2)
 * @version 0.5
 * @module
 *
 * @see java.awt.geom.AffineTransform
 */
class ProjectiveTransform extends AbstractMathTransform implements LinearTransform, ExtendedPrecisionMatrix,
        Serializable // Not Cloneable, despite the clone() method.
{
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
     * The inverse transform. Will be created only when first needed. This field is part of the serialization form
     * in order to avoid rounding errors if a user asks for the inverse of the inverse (i.e. the original transform)
     * after deserialization.
     */
    AbstractMathTransform inverse;

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
     * Returns a copy of the matrix given to the constructor.
     */
    @Override
    public final Matrix getMatrix() {
        return this;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     *
     * @return {@inheritDoc}
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Affine.PARAMETERS;
    }

    /**
     * Returns the matrix elements as a group of parameters values. The number of parameters depends on the
     * matrix size. Only matrix elements different from their default value will be included in this group.
     *
     * @return A copy of the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return TensorParameters.WKT1.createValueGroup(Affine.IDENTIFICATION, getMatrix());
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
     * Unsupported operation, since this matrix is unmodifiable.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        throw new UnsupportedOperationException(Matrices.isAffine(this)
                ? Errors.format(Errors.Keys.UnmodifiableAffineTransform)
                : Errors.format(Errors.Keys.UnmodifiableObject_1, ProjectiveTransform.class));
    }

    /**
     * Returns a copy of the matrix that user can modify.
     */
    @Override
    public final Matrix clone() {
        return Matrices.copy(this);
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * <span class="note"><b>Note:</b> this method should always returns {@code false}, since
     * {@code MathTransforms.linear(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this class directly
     * instead than using a factory method.</span>
     */
    @Override
    public boolean isIdentity() {
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
     * <code>[x<sub>0</sub>, y<sub>0</sub>, z<sub>0</sub>,
     *        x<sub>1</sub>, y<sub>1</sub>, z<sub>1</sub>...,
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
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
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
     * <code>[x<sub>0</sub>, y<sub>0</sub>, z<sub>0</sub>,
     *        x<sub>1</sub>, y<sub>1</sub>, z<sub>1</sub>...,
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
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
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
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
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
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
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
    public Matrix derivative(final DirectPosition point) {
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
     * Creates the inverse transform of this object.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            /*
             * Note: we do not perform the following optimization, because MathTransforms.linear(…)
             *       should never instantiate this class in the identity case.
             *
             *       if (isIdentity()) {
             *           inverse = this;
             *       } else { ... }
             */
            MatrixSIS matrix = Matrices.copy(this);
            matrix = matrix.inverse();
            ProjectiveTransform inv = createInverse(matrix);
            inv.inverse = this;
            inverse = inv;
        }
        return inverse;
    }

    /**
     * Creates an inverse transform using the specified matrix.
     * To be overridden by {@link GeocentricTranslation}.
     */
    ProjectiveTransform createInverse(final Matrix matrix) {
        return new ProjectiveTransform(matrix);
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
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) { // Slight optimization
            return true;
        }
        if (mode != ComparisonMode.STRICT) {
            if (object instanceof LinearTransform) {
                return Matrices.equals(this, ((LinearTransform) object).getMatrix(), mode);
            }
        } else if (super.equals(object, mode)) {
            final ProjectiveTransform that = (ProjectiveTransform) object;
            return this.numRow == that.numRow &&
                   this.numCol == that.numCol &&
                   Arrays.equals(this.elt, that.elt);
        }
        return false;
    }
}
