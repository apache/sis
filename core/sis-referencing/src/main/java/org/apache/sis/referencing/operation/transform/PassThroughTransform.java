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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Transform which passes through a subset of ordinates to another transform.
 * This allows transforms to operate on a subset of ordinates.
 *
 * <div class="note"><b>Example:</b> giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
 * {@code PassThroughTransform} can convert the height values from feet to meters without affecting the latitude and
 * longitude values. Such transform can be built as below:
 *
 * {@preformat java
 *     MathTransform feetToMetres = MathTransforms.linear(0.3048, 0);       // One-dimensional conversion.
 *     MathTransform tr = PassThroughTransform.create(2, feetToMetres, 0);  // Three-dimensional conversion.
 * }
 * </div>
 *
 * <div class="section">Immutability and thread safety</div>
 * {@code PassThroughTransform} is immutable and thread-safe if its {@linkplain #subTransform} is also
 * immutable and thread-safe.
 *
 * <div class="section">Serialization</div>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see MathTransforms#compound(MathTransform...)
 */
public class PassThroughTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1673997634240223449L;

    /**
     * Index of the first affected ordinate.
     *
     * @see #getModifiedCoordinates()
     */
    final int firstAffectedOrdinate;

    /**
     * Number of unaffected ordinates after the affected ones.
     *
     * @see #getModifiedCoordinates()
     */
    final int numTrailingOrdinates;

    /**
     * The sub-transform to apply on the {@linkplain #getModifiedCoordinates() modified coordinates}.
     * This is often the sub-transform specified at construction time, but not necessarily.
     */
    final MathTransform subTransform;

    /**
     * The inverse transform. This field will be computed only when needed,
     * but is part of serialization in order to avoid rounding error.
     */
    PassThroughTransform inverse;

    /**
     * Constructor for sub-classes.
     * Users should invoke the static {@link #create(int, MathTransform, int)} factory method instead,
     * since the most optimal pass-through transform for the given {@code subTransform} is not necessarily
     * a {@code PassThroughTransform} instance.
     *
     * @param firstAffectedOrdinate Index of the first affected ordinate.
     * @param subTransform          The sub-transform to apply on modified coordinates.
     * @param numTrailingOrdinates  Number of trailing ordinates to pass through.
     *
     * @see #create(int, MathTransform, int)
     */
    protected PassThroughTransform(final int firstAffectedOrdinate,
                                   final MathTransform subTransform,
                                   final int numTrailingOrdinates)
    {
        ensurePositive("firstAffectedOrdinate", firstAffectedOrdinate);
        ensurePositive("numTrailingOrdinates",  numTrailingOrdinates);
        if (subTransform instanceof PassThroughTransform) {
            final PassThroughTransform passThrough = (PassThroughTransform) subTransform;
            this.firstAffectedOrdinate = passThrough.firstAffectedOrdinate + firstAffectedOrdinate;
            this.numTrailingOrdinates  = passThrough.numTrailingOrdinates  + numTrailingOrdinates;
            this.subTransform          = passThrough.subTransform;
        }  else {
            this.firstAffectedOrdinate = firstAffectedOrdinate;
            this.numTrailingOrdinates  = numTrailingOrdinates;
            this.subTransform          = subTransform;
        }
    }

    /**
     * Creates a transform which passes through a subset of ordinates to another transform.
     * This method returns a transform having the following dimensions:
     *
     * {@preformat java
     *     Source: firstAffectedOrdinate + subTransform.getSourceDimensions() + numTrailingOrdinates
     *     Target: firstAffectedOrdinate + subTransform.getTargetDimensions() + numTrailingOrdinates
     * }
     *
     * Affected ordinates will range from {@code firstAffectedOrdinate} inclusive to
     * {@code dimTarget - numTrailingOrdinates} exclusive.
     *
     * @param  firstAffectedOrdinate Index of the first affected ordinate.
     * @param  subTransform          The sub-transform to apply on modified coordinates.
     * @param  numTrailingOrdinates  Number of trailing ordinates to pass through.
     * @return A pass-through transform, not necessarily a {@code PassThroughTransform} instance.
     */
    public static MathTransform create(final int firstAffectedOrdinate,
                                       final MathTransform subTransform,
                                       final int numTrailingOrdinates)
    {
        ensureNonNull ("subTransform",          subTransform);
        ensurePositive("firstAffectedOrdinate", firstAffectedOrdinate);
        ensurePositive("numTrailingOrdinates",  numTrailingOrdinates);
        if (firstAffectedOrdinate == 0 && numTrailingOrdinates == 0) {
            return subTransform;
        }
        /*
         * Optimizes the "Identity transform" case.
         */
        if (subTransform.isIdentity()) {
            final int dimension = subTransform.getSourceDimensions();
            if (dimension == subTransform.getTargetDimensions()) {
                return IdentityTransform.create(firstAffectedOrdinate + dimension + numTrailingOrdinates);
            }
        }
        /*
         * Special case for transformation backed by a matrix. Is is possible to use a
         * new matrix for such transform, instead of wrapping the sub-transform into a
         * PassThroughTransform object. It is faster and easier to concatenate.
         */
        Matrix matrix = MathTransforms.getMatrix(subTransform);
        if (matrix != null) {
            matrix = expand(MatrixSIS.castOrCopy(matrix), firstAffectedOrdinate, numTrailingOrdinates, 1);
            return MathTransforms.linear(matrix);
        }
        /*
         * Constructs the general PassThroughTransform object. An optimization is done right in
         * the constructor for the case where the sub-transform is already a PassThroughTransform.
         */
        int dim = subTransform.getSourceDimensions();
        if (subTransform.getTargetDimensions() == dim) {
            dim += firstAffectedOrdinate + numTrailingOrdinates;
            if (dim == 2) {
                return new PassThroughTransform2D(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
            }
        }
        return new PassThroughTransform(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
    }

    /**
     * If the given matrix to be concatenated to this transform, can be concatenated to the
     * sub-transform instead, returns the matrix to be concatenated to the sub-transform.
     * Otherwise returns {@code null}.
     *
     * <p>This method assumes that the matrix size is compatible with this transform source dimension.
     * It is caller responsibility to verify this condition.</p>
     */
    final Matrix toSubMatrix(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != numCol) {
            // Current implementation requires a square matrix.
            return null;
        }
        final int subDim = subTransform.getSourceDimensions();
        final MatrixSIS sub = Matrices.createIdentity(subDim + 1);
        /*
         * Ensure that every dimensions which are scaled by the affine transform are one
         * of the dimensions modified by the sub-transform, and not any other dimension.
         */
        for (int j=numRow; --j>=0;) {
            final int sj = j - firstAffectedOrdinate;
            for (int i=numCol; --i>=0;) {
                final double element = matrix.getElement(j, i);
                if (sj >= 0 && sj < subDim) {
                    final int si;
                    final boolean pass;
                    if (i == numCol-1) { // Translation term (last column)
                        si = subDim;
                        pass = true;
                    } else { // Any term other than translation.
                        si = i - firstAffectedOrdinate;
                        pass = (si >= 0 && si < subDim);
                    }
                    if (pass) {
                        sub.setElement(sj, si, element);
                        continue;
                    }
                }
                if (element != (i == j ? 1 : 0)) {
                    // Found a dimension which perform some scaling or translation.
                    return null;
                }
            }
        }
        return sub;
    }

    /**
     * Gets the dimension of input points. This the source dimension of the
     * {@linkplain #subTransform sub-transform} plus the number of pass-through dimensions.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getSourceDimensions() {
        return firstAffectedOrdinate + subTransform.getSourceDimensions() + numTrailingOrdinates;
    }

    /**
     * Gets the dimension of output points. This the target dimension of the
     * {@linkplain #subTransform sub-transform} plus the number of pass-through dimensions.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getTargetDimensions() {
        return firstAffectedOrdinate + subTransform.getTargetDimensions() + numTrailingOrdinates;
    }

    /**
     * Returns the ordered sequence of positive integers defining the positions in a source
     * coordinate tuple of the coordinates affected by this pass-through operation.
     *
     * <div class="note"><b>API note:</b> this method is final for now because most of Apache SIS code do
     * not use the {@code modifiedCoordinates} array. Instead, SIS uses the {@code firstAffectedOrdinate}
     * and {@code numTrailingOrdinates} information provided to the constructor. Consequently overriding
     * this method may be misleading since it would be ignored by SIS. We do not want to make the "really
     * used" fields public in order to keep the flexibility to replace them by a {@code modifiedCoordinates}
     * array in a future SIS version.</div>
     *
     * @return Zero-based indices of the modified source coordinates.
     *
     * @see org.apache.sis.referencing.operation.DefaultPassThroughOperation#getModifiedCoordinates()
     */
    public final int[] getModifiedCoordinates() {
        final int[] index = new int[subTransform.getSourceDimensions()];
        for (int i=0; i<index.length; i++) {
            index[i] = i + firstAffectedOrdinate;
        }
        return index;
    }

    /**
     * Returns the sub-transform to apply on the {@linkplain #getModifiedCoordinates() modified coordinates}.
     * This is often the sub-transform specified at construction time, but not necessarily.
     *
     * @return The sub-transform.
     *
     * @see org.apache.sis.referencing.operation.DefaultPassThroughOperation#getOperation()
     */
    public final MathTransform getSubTransform() {
        return subTransform;
    }

    /**
     * Tests whether this transform does not move any points. A {@code PassThroughTransform}
     * is identity if the {@linkplain #subTransform sub-transform} is also identity.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isIdentity() {
        return subTransform.isIdentity();
    }

    /**
     * Transforms a single coordinate in a list of ordinal values, and opportunistically
     * computes the transform derivative if requested.
     *
     * @return {@inheritDoc}
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        Matrix derivative = null;
        if (derivate) {
            derivative = derivative(new DirectPositionView(srcPts, srcOff, getSourceDimensions()));
        }
        if (dstPts != null) {
            transform(srcPts, srcOff, dstPts, dstOff, 1);
        }
        return derivative;
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     *
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        int srcStep = numTrailingOrdinates;
        int dstStep = numTrailingOrdinates;
        if (srcPts == dstPts) {
            final int add = firstAffectedOrdinate + numTrailingOrdinates;
            final int dimSource = subDimSource + add;
            final int dimTarget = subDimTarget + add;
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, dimTarget, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * dimTarget;
                    srcStep -= 2*dimSource;
                    dstStep -= 2*dimTarget;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                }
            }
        }
        while (--numPts >= 0) {
            System.arraycopy(      srcPts, srcOff,
                                   dstPts, dstOff,   firstAffectedOrdinate);
            subTransform.transform(srcPts, srcOff += firstAffectedOrdinate,
                                   dstPts, dstOff += firstAffectedOrdinate, 1);
            System.arraycopy(      srcPts, srcOff += subDimSource,
                                   dstPts, dstOff += subDimTarget, numTrailingOrdinates);
            srcOff += srcStep;
            dstOff += dstStep;
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     *
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        int srcStep = numTrailingOrdinates;
        int dstStep = numTrailingOrdinates;
        if (srcPts == dstPts) {
            final int add = firstAffectedOrdinate + numTrailingOrdinates;
            final int dimSource = subDimSource + add;
            final int dimTarget = subDimTarget + add;
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, dimTarget, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * dimTarget;
                    srcStep -= 2*dimSource;
                    dstStep -= 2*dimTarget;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                }
            }
        }
        while (--numPts >= 0) {
            System.arraycopy(      srcPts, srcOff,
                                   dstPts, dstOff,   firstAffectedOrdinate);
            subTransform.transform(srcPts, srcOff += firstAffectedOrdinate,
                                   dstPts, dstOff += firstAffectedOrdinate, 1);
            System.arraycopy(      srcPts, srcOff += subDimSource,
                                   dstPts, dstOff += subDimTarget, numTrailingOrdinates);
            srcOff += srcStep;
            dstOff += dstStep;
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     *
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        while (--numPts >= 0) {
            for (int i=0; i<firstAffectedOrdinate; i++) {
                dstPts[dstOff++] = (float) srcPts[srcOff++];
            }
            subTransform.transform(srcPts, srcOff, dstPts, dstOff, 1);
            srcOff += subDimSource;
            dstOff += subDimTarget;
            for (int i=0; i<numTrailingOrdinates; i++) {
                dstPts[dstOff++] = (float) srcPts[srcOff++];
            }
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     *
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        while (--numPts >= 0) {
            for (int i=0; i<firstAffectedOrdinate; i++) {
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            subTransform.transform(srcPts, srcOff, dstPts, dstOff, 1);
            srcOff += subDimSource;
            dstOff += subDimTarget;
            for (int i=0; i<numTrailingOrdinates; i++) {
                dstPts[dstOff++] = srcPts[srcOff++];
            }
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @return {@inheritDoc}
     * @throws TransformException If the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int nSkipped = firstAffectedOrdinate + numTrailingOrdinates;
        final int transDim = subTransform.getSourceDimensions();
        ensureDimensionMatches("point", transDim + nSkipped, point);
        final GeneralDirectPosition subPoint = new GeneralDirectPosition(transDim);
        for (int i=0; i<transDim; i++) {
            subPoint.ordinates[i] = point.getOrdinate(i + firstAffectedOrdinate);
        }
        return expand(MatrixSIS.castOrCopy(subTransform.derivative(subPoint)),
                firstAffectedOrdinate, numTrailingOrdinates, 0);
    }

    /**
     * Creates a pass-through transform from a matrix.  This method is invoked when the
     * sub-transform can be expressed as a matrix. It is also invoked for computing the
     * matrix returned by {@link #derivative}.
     *
     * @param subMatrix The sub-transform as a matrix.
     * @param firstAffectedOrdinate Index of the first affected ordinate.
     * @param numTrailingOrdinates Number of trailing ordinates to pass through.
     * @param affine 0 if the matrix do not contains translation terms, or 1 if
     *        the matrix is an affine transform with translation terms.
     */
    private static Matrix expand(final MatrixSIS subMatrix,
                                 final int firstAffectedOrdinate,
                                 final int numTrailingOrdinates,
                                 final int affine)
    {
        final int nSkipped  = firstAffectedOrdinate + numTrailingOrdinates;
        final int numSubRow = subMatrix.getNumRow() - affine;
        final int numSubCol = subMatrix.getNumCol() - affine;
        final int numRow    = numSubRow + (nSkipped + affine);
        final int numCol    = numSubCol + (nSkipped + affine);
        final Number[] elements = new Number[numRow * numCol]; // Matrix elements as row major (column index varies faster).
        Arrays.fill(elements, 0);
        /*                      ┌                  ┐
         *  Set UL part to 1:   │ 1  0             │
         *                      │ 0  1             │
         *                      │                  │
         *                      │                  │
         *                      │                  │
         *                      └                  ┘
         */
        final Integer ONE = 1;
        for (int j=0; j<firstAffectedOrdinate; j++) {
            elements[j*numCol + j] = ONE;
        }
        /*                      ┌                  ┐
         *  Set central part:   │ 1  0  0  0  0  0 │
         *                      │ 0  1  0  0  0  0 │
         *                      │ 0  0  ?  ?  ?  0 │
         *                      │ 0  0  ?  ?  ?  0 │
         *                      │                  │
         *                      └                  ┘
         */
        for (int j=0; j<numSubRow; j++) {
            for (int i=0; i<numSubCol; i++) {
                /*
                 * We need to store the elements as Number, not as double, for giving to the matrix
                 * a chance to preserve the extra precision provided by DoubleDouble numbers.
                 */
                elements[(j + firstAffectedOrdinate) * numCol    // Contribution of row index
                       + (i + firstAffectedOrdinate)]            // Contribution of column index
                       = subMatrix.getNumber(j, i);
            }
        }
        /*                      ┌                  ┐
         *  Set LR part to 1:   │ 1  0  0  0  0  0 │
         *                      │ 0  1  0  0  0  0 │
         *                      │ 0  0  ?  ?  ?  0 │
         *                      │ 0  0  ?  ?  ?  0 │
         *                      │ 0  0  0  0  0  1 │
         *                      └                  ┘
         */
        final int offset    = numSubCol - numSubRow;
        final int numRowOut = numSubRow + nSkipped;
        final int numColOut = numSubCol + nSkipped;
        for (int j=numRowOut - numTrailingOrdinates; j<numRowOut; j++) {
            elements[j * numCol + (j + offset)] = ONE;
        }
        if (affine != 0) {
            // Copy the translation terms in the last column.
            for (int j=0; j<numSubRow; j++) {
                elements[(j + firstAffectedOrdinate) * numCol + numColOut] = subMatrix.getNumber(j, numSubCol);
            }
            // Copy the last row as a safety, but it should contains only 0.
            for (int i=0; i<numSubCol; i++) {
                elements[numRowOut * numCol + (i + firstAffectedOrdinate)] = subMatrix.getNumber(numSubRow, i);
            }
            // Copy the lower right corner, which should contains only 1.
            elements[numRowOut * numCol + numColOut] = subMatrix.getNumber(numSubRow, numSubCol);
        }
        return Matrices.create(numRow, numCol, elements);
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @return {@inheritDoc}
     * @throws NoninvertibleTransformException If the {@linkplain #subTransform sub-transform} is not invertible.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            inverse = new PassThroughTransform(
                    firstAffectedOrdinate, subTransform.inverse(), numTrailingOrdinates);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        // Note that numTrailingOrdinates is related to source and
        // target dimensions, which are computed by the super-class.
        return super.computeHashCode() ^ (subTransform.hashCode() + firstAffectedOrdinate);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            final PassThroughTransform that = (PassThroughTransform) object;
            return this.firstAffectedOrdinate == that.firstAffectedOrdinate &&
                   this.numTrailingOrdinates  == that.numTrailingOrdinates  &&
                   Utilities.deepEquals(this.subTransform, that.subTransform, mode);
        }
        return false;
    }

    /**
     * Formats this transform as a <cite>Well Known Text</cite> version 1 (WKT 1) element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code PassThrough_MT} is defined in the WKT 1 specification only.
     * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
     * then this method silently uses the WKT 1 convention without raising an error
     * (unless this {@code PassThroughTransform} can not be formatted as valid WKT 1 neither).</div>
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "PassThrough_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(firstAffectedOrdinate);
        if (numTrailingOrdinates != 0) {
            formatter.append(numTrailingOrdinates);
            formatter.setInvalidWKT(PassThroughTransform.class, null);
        }
        formatter.append(subTransform);
        return WKTKeywords.PassThrough_MT;
    }
}
