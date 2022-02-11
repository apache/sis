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
import java.lang.reflect.Array;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.internal.referencing.MathTransformsOrFactory;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.WKTKeywords;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArraysExt;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Transform which passes through a subset of coordinates to another transform.
 * This allows transforms to operate on a subset of coordinate values.
 *
 * <div class="note"><b>Example:</b> giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
 * {@code PassThroughTransform} can convert the height values from feet to meters without affecting the latitude and
 * longitude values. Such transform can be built as below:
 *
 * {@preformat java
 *     MathTransform feetToMetres = MathTransforms.linear(0.3048, 0);       // One-dimensional conversion.
 *     MathTransform tr = MathTransforms.passThrough(2, feetToMetres, 0);   // Three-dimensional conversion.
 * }
 * </div>
 *
 * <h2>Immutability and thread safety</h2>
 * {@code PassThroughTransform} is immutable and thread-safe if its {@linkplain #subTransform} is also
 * immutable and thread-safe.
 *
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 *
 * @see MathTransforms#passThrough(int, MathTransform, int)
 * @see MathTransforms#compound(MathTransform...)
 *
 * @since 0.5
 * @module
 */
public class PassThroughTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -910726602881388979L;

    /**
     * Index of the first affected coordinate.
     *
     * @see #getModifiedCoordinates()
     */
    final int firstAffectedCoordinate;

    /**
     * Number of unaffected coordinates after the affected ones.
     *
     * @see #getModifiedCoordinates()
     */
    final int numTrailingCoordinates;

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
     * Users should invoke the static {@link MathTransforms#passThrough(int, MathTransform, int)} factory method instead,
     * since the most optimal pass-through transform for the given {@code subTransform} is not necessarily
     * a {@code PassThroughTransform} instance.
     *
     * @param firstAffectedCoordinate  index of the first affected coordinate.
     * @param subTransform             the sub-transform to apply on modified coordinates.
     * @param numTrailingCoordinates   number of trailing coordinates to pass through.
     *
     * @see MathTransforms#passThrough(int, MathTransform, int)
     */
    protected PassThroughTransform(final int firstAffectedCoordinate,
                                   final MathTransform subTransform,
                                   final int numTrailingCoordinates)
    {
        ensurePositive("firstAffectedCoordinate", firstAffectedCoordinate);
        ensurePositive("numTrailingCoordinates",  numTrailingCoordinates);
        if (subTransform instanceof PassThroughTransform) {
            final PassThroughTransform passThrough = (PassThroughTransform) subTransform;
            this.firstAffectedCoordinate = passThrough.firstAffectedCoordinate + firstAffectedCoordinate;
            this.numTrailingCoordinates  = passThrough.numTrailingCoordinates  + numTrailingCoordinates;
            this.subTransform            = passThrough.subTransform;
        }  else {
            this.firstAffectedCoordinate = firstAffectedCoordinate;
            this.numTrailingCoordinates  = numTrailingCoordinates;
            this.subTransform            = subTransform;
        }
    }

    /**
     * Creates a transform which passes through a subset of coordinates to another transform.
     * This method returns a transform having the following dimensions:
     *
     * {@preformat java
     *     Source: firstAffectedCoordinate + subTransform.getSourceDimensions() + numTrailingCoordinates
     *     Target: firstAffectedCoordinate + subTransform.getTargetDimensions() + numTrailingCoordinates
     * }
     *
     * Affected coordinates will range from {@code firstAffectedCoordinate} inclusive to
     * {@code dimTarget - numTrailingCoordinates} exclusive.
     *
     * @param  firstAffectedCoordinate  index of the first affected coordinate.
     * @param  subTransform           the sub-transform to apply on modified coordinates.
     * @param  numTrailingCoordinates   number of trailing coordinates to pass through.
     * @return a pass-through transform, not necessarily a {@code PassThroughTransform} instance.
     */
    static MathTransform create(final int firstAffectedCoordinate, final MathTransform subTransform, final int numTrailingCoordinates) {
        Matrix matrix = MathTransforms.getMatrix(subTransform);
        if (matrix != null) {
            return newInstance(firstAffectedCoordinate, matrix, numTrailingCoordinates);
        }
        /*
         * Above checks tried to avoid the creation of PassThroughTransform instance. At this point we can not
         * avoid it anymore. But maybe we can merge two PassThroughTransforms into a single one. It may happen
         * if 'subTransform' is a concatenation of a linear transform + pass through transform (in any order).
         * In such case, moving the linear transform outside 'subTransform' enable above-cited merge.
         */
        if (subTransform instanceof ConcatenatedTransform) {
            MathTransform transform1 = ((ConcatenatedTransform) subTransform).transform1;
            MathTransform transform2 = ((ConcatenatedTransform) subTransform).transform2;
            matrix = MathTransforms.getMatrix(transform1);
            if (matrix != null && transform2 instanceof PassThroughTransform) {
                transform1 = newInstance(firstAffectedCoordinate, matrix,     numTrailingCoordinates);
                transform2 = newInstance(firstAffectedCoordinate, transform2, numTrailingCoordinates);
                return MathTransforms.concatenate(transform1, transform2);
            }
            matrix = MathTransforms.getMatrix(transform2);
            if (matrix != null && transform1 instanceof PassThroughTransform) {
                transform1 = newInstance(firstAffectedCoordinate, transform1, numTrailingCoordinates);
                transform2 = newInstance(firstAffectedCoordinate, matrix,     numTrailingCoordinates);
                return MathTransforms.concatenate(transform1, transform2);
            }
        }
        return newInstance(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
    }

    /**
     * Special case for transformation backed by a matrix. Is is possible to use a new matrix for such transform,
     * instead of wrapping the sub-transform into a {@code PassThroughTransform} object. It is faster and easier
     * to concatenate.
     */
    private static LinearTransform newInstance(int firstAffectedCoordinate, Matrix subTransform, int numTrailingCoordinates) {
        return MathTransforms.linear(expand(MatrixSIS.castOrCopy(subTransform), firstAffectedCoordinate, numTrailingCoordinates, 1));
    }

    /**
     * Constructs the general {@code PassThroughTransform} object. An optimization is done right in
     * the constructor for the case where the sub-transform is already a {@code PassThroughTransform}.
     * It is caller's responsibility to ensure that the argument values are valid.
     */
    private static PassThroughTransform newInstance(final int firstAffectedCoordinate,
                                                    final MathTransform subTransform,
                                                    final int numTrailingCoordinates)
    {
        int dim = subTransform.getSourceDimensions();
        if (subTransform.getTargetDimensions() == dim) {
            dim += firstAffectedCoordinate + numTrailingCoordinates;
            if (dim == 2) {
                return new PassThroughTransform2D(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
            }
        }
        return new PassThroughTransform(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
    }

    /**
     * Gets the dimension of input points. This the source dimension of the
     * {@linkplain #subTransform sub-transform} plus the number of pass-through dimensions.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getSourceDimensions() {
        return firstAffectedCoordinate + subTransform.getSourceDimensions() + numTrailingCoordinates;
    }

    /**
     * Gets the dimension of output points. This the target dimension of the
     * {@linkplain #subTransform sub-transform} plus the number of pass-through dimensions.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getTargetDimensions() {
        return firstAffectedCoordinate + subTransform.getTargetDimensions() + numTrailingCoordinates;
    }

    /**
     * Returns the ordered sequence of positive integers defining the positions in a source
     * coordinate tuple of the coordinates affected by this pass-through operation.
     *
     * <div class="note"><b>API note:</b> this method is final for now because most of Apache SIS code do
     * not use the {@code modifiedCoordinates} array. Instead, SIS uses the {@code firstAffectedCoordinate}
     * and {@code numTrailingCoordinates} information provided to the constructor. Consequently overriding
     * this method may be misleading since it would be ignored by SIS. We do not want to make the "really
     * used" fields public in order to keep the flexibility to replace them by a {@code modifiedCoordinates}
     * array in a future SIS version.</div>
     *
     * @return Zero-based indices of the modified source coordinates.
     *
     * @see org.apache.sis.referencing.operation.DefaultPassThroughOperation#getModifiedCoordinates()
     */
    public final int[] getModifiedCoordinates() {
        return ArraysExt.range(firstAffectedCoordinate, firstAffectedCoordinate + subTransform.getSourceDimensions());
    }

    /**
     * Returns the sub-transform to apply on the {@linkplain #getModifiedCoordinates() modified coordinates}.
     * This is often the sub-transform specified at construction time, but not necessarily.
     *
     * @return the sub-transform.
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
     * Transforms a single position in a list of coordinate values, and opportunistically
     * computes the transform derivative if requested.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        Matrix derivative = null;
        if (derivate) {
            derivative = derivative(new DirectPositionView.Double(srcPts, srcOff, getSourceDimensions()));
        }
        if (dstPts != null) {
            transform(srcPts, srcOff, dstPts, dstOff, 1);
        }
        return derivative;
    }

    /**
     * Creates a new array of the same kind than the given array.
     * This is used for creating {@code float[]} or {@code double[]} arrays.
     */
    private static Object newArray(final Object array, final int length) {
        return Array.newInstance(array.getClass().getComponentType(), length);
    }

    /**
     * Transforms an array of points with potentially overlapping source and target.
     *
     * @param  srcPts  the point to transform, as a {@code float[]} or {@code double[]} array.
     * @param  srcOff  the offset to the point to be transformed in the array.
     * @param  dstPts  where to store the transformed points, as an array of same type than {@code srcPts}.
     * @param  dstOff  the offset to the location of the transformed point that is stored in the destination array.
     * @param  numPts  number of points to transform.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private void transformOverlapping(final Object srcPts, final int srcOff,
                                      final Object dstPts, int dstOff, int numPts) throws TransformException
    {
        if (numPts <= 0) return;
        final int subDimSource   = subTransform.getSourceDimensions();
        final int subDimTarget   = subTransform.getTargetDimensions();
        final int numPassThrough = firstAffectedCoordinate + numTrailingCoordinates;
        final int dimSource      = subDimSource + numPassThrough;
        final int dimTarget      = subDimTarget + numPassThrough;
        /*
         * Copy the pass-through coordinates (both before and after the sub-transform) into the 'pasPts'
         * temporary array. This will allow us to compact the coordinates to give to the sub-transform,
         * so we can process them in a single 'transform' method call. We do that also for avoiding tricky
         * issues with overlapping regions, because coordinate tuples are not processed automically the
         * way 'IterationStrategy' expects.
         */
        final Object pasPts;
        {
            pasPts = newArray(srcPts, numPassThrough * numPts);
            System.arraycopy(srcPts, srcOff, pasPts, 0, firstAffectedCoordinate);
            int pasOff = firstAffectedCoordinate;
            int srcCpk = srcOff + pasOff + subDimSource;            // "Cpk" stands for "cherry-pick".
            int n = numPts - 1;
            while (--n >= 0) {
                System.arraycopy(srcPts, srcCpk, pasPts, pasOff, numPassThrough);
                pasOff += numPassThrough;
                srcCpk += dimSource;
            }
            System.arraycopy(srcPts, srcCpk, pasPts, pasOff, numTrailingCoordinates);
        }
        /*
         * Copy in a compact array the coordinates to be given to the sub-transform.
         * We do the compaction in the destination array (if it is large enough) for
         * avoiding the need to create a temporary buffer. We can do that only after
         * all pass-through coordinates have been copied by above loop, for avoiding
         * to overwrite values if the source and destination array regions overlap.
         */
        Object subPts = dstPts;
        {
            int subOff = dstOff;
            int srcCpk = srcOff + firstAffectedCoordinate;    // "Cpk" stands for "cherry-pick".
            int srcInc = dimSource;
            int dstInc = subDimSource;
            final IterationStrategy strategy;
            if (subDimSource > subDimTarget + numPassThrough) {
                // If the destination array does not have enough room, create a temporary buffer.
                strategy = IterationStrategy.BUFFER_TARGET;
            } else if (srcPts != dstPts) {
                strategy = IterationStrategy.ASCENDING;
            } else {
                strategy = IterationStrategy.suggest(srcOff, srcInc, dstOff, dstInc, numPts);
            }
            switch (strategy) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcCpk += (numPts-1) * srcInc; srcInc = -srcInc;
                    subOff += (numPts-1) * dstInc; dstInc = -dstInc;
                    break;
                }
                default: {
                    subPts = newArray(subPts, Math.max(subDimSource, subDimTarget) * numPts);
                    subOff = 0;
                    break;
                }
            }
            int n = numPts;
            do {
                System.arraycopy(srcPts, srcCpk, subPts, subOff, subDimSource);
                subOff += dstInc;
                srcCpk += srcInc;
            } while (--n != 0);
        }
        /*
         * All sub-transform coordinates have been compacted as consecutive tuples.
         * Convert them in-place, overwriting the previous values.
         */
        int subOff = (subPts == dstPts) ? dstOff : 0;
        if (subPts instanceof double[]) {
            subTransform.transform((double[]) subPts, subOff, (double[]) subPts, subOff, numPts);
        } else {
            subTransform.transform( (float[]) subPts, subOff,  (float[]) subPts, subOff, numPts);
        }
        /*
         * Copies the transformed coordinates to their final location, inserting pass-through
         * coordinates between them in the process. Note that we avoided to modify 'dstOff'
         * and 'numPts' before this point, but now we are free to do so since this is the last
         * step.
         */
        int pasOff = numPts * numPassThrough;
        subOff    += numPts * subDimTarget;
        dstOff    += numPts * dimTarget;
        if (--numPts >= 0) {
            System.arraycopy(pasPts, pasOff -= numTrailingCoordinates,
                             dstPts, dstOff -= numTrailingCoordinates, numTrailingCoordinates);
            System.arraycopy(subPts, subOff -= subDimTarget,
                             dstPts, dstOff -= subDimTarget, subDimTarget);
            while (--numPts >= 0) {
                System.arraycopy(pasPts, pasOff -= numPassThrough,
                                 dstPts, dstOff -= numPassThrough, numPassThrough);
                System.arraycopy(subPts, subOff -= subDimTarget,
                                 dstPts, dstOff -= subDimTarget, subDimTarget);
            }
            System.arraycopy(pasPts, pasOff - firstAffectedCoordinate,
                             dstPts, dstOff - firstAffectedCoordinate, firstAffectedCoordinate);
        }
    }

    /**
     * Transforms many positions in a list of coordinate values.
     *
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        transformOverlapping(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     *
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        transformOverlapping(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many positions in a list of coordinate values.
     *
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        while (--numPts >= 0) {
            for (int i=0; i < firstAffectedCoordinate; i++) {
                dstPts[dstOff++] = (float) srcPts[srcOff++];
            }
            subTransform.transform(srcPts, srcOff, dstPts, dstOff, 1);
            srcOff += subDimSource;
            dstOff += subDimTarget;
            for (int i=0; i < numTrailingCoordinates; i++) {
                dstPts[dstOff++] = (float) srcPts[srcOff++];
            }
        }
    }

    /**
     * Transforms many positions in a list of coordinate values.
     *
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        final int subDimSource = subTransform.getSourceDimensions();
        final int subDimTarget = subTransform.getTargetDimensions();
        while (--numPts >= 0) {
            for (int i=0; i < firstAffectedCoordinate; i++) {
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            subTransform.transform(srcPts, srcOff, dstPts, dstOff, 1);
            srcOff += subDimSource;
            dstOff += subDimTarget;
            for (int i=0; i < numTrailingCoordinates; i++) {
                dstPts[dstOff++] = srcPts[srcOff++];
            }
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the {@linkplain #subTransform sub-transform} failed.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int nSkipped = firstAffectedCoordinate + numTrailingCoordinates;
        final int transDim = subTransform.getSourceDimensions();
        ensureDimensionMatches("point", transDim + nSkipped, point);
        final GeneralDirectPosition subPoint = new GeneralDirectPosition(transDim);
        for (int i=0; i<transDim; i++) {
            subPoint.coordinates[i] = point.getOrdinate(i + firstAffectedCoordinate);
        }
        return expand(MatrixSIS.castOrCopy(subTransform.derivative(subPoint)),
                firstAffectedCoordinate, numTrailingCoordinates, 0);
    }

    /**
     * Creates a pass-through transform from a matrix.  This method is invoked when the
     * sub-transform can be expressed as a matrix. It is also invoked for computing the
     * matrix returned by {@link #derivative}.
     *
     * @param subMatrix                the sub-transform as a matrix.
     * @param firstAffectedCoordinate  index of the first affected coordinate.
     * @param numTrailingCoordinates   number of trailing coordinates to pass through.
     * @param affine                   0 if the matrix do not contains translation terms, or 1 if
     *                                 the matrix is an affine transform with translation terms.
     */
    private static Matrix expand(final MatrixSIS subMatrix,
                                 final int firstAffectedCoordinate,
                                 final int numTrailingCoordinates,
                                 final int affine)
    {
        final int nSkipped  = firstAffectedCoordinate + numTrailingCoordinates;
        final int numSubRow = subMatrix.getNumRow() - affine;
        final int numSubCol = subMatrix.getNumCol() - affine;
        final int numRow    = numSubRow + (nSkipped + affine);
        final int numCol    = numSubCol + (nSkipped + affine);
        final Number[] elements = new Number[numRow * numCol];      // Matrix elements as row major (column index varies faster).
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
        for (int j=0; j<firstAffectedCoordinate; j++) {
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
                elements[(j + firstAffectedCoordinate) * numCol    // Contribution of row index
                       + (i + firstAffectedCoordinate)]            // Contribution of column index
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
        for (int j=numRowOut - numTrailingCoordinates; j<numRowOut; j++) {
            elements[j * numCol + (j + offset)] = ONE;
        }
        if (affine != 0) {
            // Copy the translation terms in the last column.
            for (int j=0; j<numSubRow; j++) {
                elements[(j + firstAffectedCoordinate) * numCol + numColOut] = subMatrix.getNumber(j, numSubCol);
            }
            // Copy the last row as a safety, but it should contain only 0.
            for (int i=0; i<numSubCol; i++) {
                elements[numRowOut * numCol + (i + firstAffectedCoordinate)] = subMatrix.getNumber(numSubRow, i);
            }
            // Copy the lower right corner, which should contain only 1.
            elements[numRowOut * numCol + numColOut] = subMatrix.getNumber(numSubRow, numSubCol);
        }
        return Matrices.create(numRow, numCol, elements);
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @return {@inheritDoc}
     * @throws NoninvertibleTransformException if the {@linkplain #subTransform sub-transform} is not invertible.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            inverse = new PassThroughTransform(firstAffectedCoordinate, subTransform.inverse(), numTrailingCoordinates);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * If the given matrix to be concatenated to this transform, can be concatenated to the
     * sub-transform instead, returns the matrix to be concatenated to the sub-transform.
     * Otherwise returns {@code null}.
     *
     * <p>This method does not verify if the matrix size is compatible with this transform dimension.</p>
     *
     * @param  applyOtherFirst  {@code true} if the transformation order is {@code matrix} followed by {@code this}, or
     *                          {@code false} if the transformation order is {@code this} followed by {@code matrix}.
     */
    private Matrix toSubMatrix(final boolean applyOtherFirst, final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != numCol) {
            // Current implementation requires a square matrix.
            return null;
        }
        final int subDim = applyOtherFirst ? subTransform.getSourceDimensions()
                                           : subTransform.getTargetDimensions();
        final MatrixSIS sub = Matrices.createIdentity(subDim + 1);
        /*
         * Ensure that every dimensions which are scaled by the affine transform are one
         * of the dimensions modified by the sub-transform, and not any other dimension.
         */
        for (int j=numRow; --j>=0;) {
            final int sj = j - firstAffectedCoordinate;
            for (int i=numCol; --i>=0;) {
                final double element = matrix.getElement(j, i);
                if (sj >= 0 && sj < subDim) {
                    final int si;
                    final boolean copy;
                    if (i == numCol-1) {                    // Translation term (last column)
                        si = subDim;
                        copy = true;
                    } else {                                // Any term other than translation.
                        si = i - firstAffectedCoordinate;
                        copy = (si >= 0 && si < subDim);
                    }
                    if (copy) {
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
     * Concatenates or pre-concatenates in an optimized way this transform with the given transform, if possible.
     * This method applies the following special cases:
     *
     * <ul>
     *  <li>If the other transform is also a {@code PassThroughTransform}, then the two transforms may be merged
     *      in a single {@code PassThroughTransform} instance.</li>
     *  <li>If the other transform discards some dimensions, verify if we still need a {@code PassThroughTransform}.</li>
     * </ul>
     *
     * @return the simplified transform, or {@code null} if no such optimization is available.
     * @throws FactoryException if an error occurred while combining the transforms.
     *
     * @since 1.0
     */
    @Override
    protected MathTransform tryConcatenate(final boolean applyOtherFirst, final MathTransform other, final MathTransformFactory factory)
            throws FactoryException
    {
        final MathTransformsOrFactory proxy = MathTransformsOrFactory.wrap(factory);
        if (other instanceof PassThroughTransform) {
            final PassThroughTransform opt = (PassThroughTransform) other;
            if (opt.firstAffectedCoordinate == firstAffectedCoordinate && opt.numTrailingCoordinates == numTrailingCoordinates) {
                final MathTransform sub = proxy.concatenate(applyOtherFirst, subTransform, opt.subTransform);
                return proxy.passThrough(firstAffectedCoordinate, sub, numTrailingCoordinates);
            }
        }
        final Matrix m = MathTransforms.getMatrix(other);
        if (m != null) {
            /*
             * If the other transform is a linear transform and all passthrough coordinates are unchanged by the matrix,
             * we can move the matrix inside the passthrough transform. It reduces the number of dimension on which the
             * linear transform operate, and gives a chance for another optimization in the concatenation between that
             * linear transform and the sub-transform.
             */
            final Matrix sub = toSubMatrix(applyOtherFirst, m);
            if (sub != null) {
                MathTransform tr = proxy.linear(sub);
                tr = proxy.concatenate(applyOtherFirst, subTransform, tr);
                return proxy.passThrough(firstAffectedCoordinate, tr, numTrailingCoordinates);
            }
            /*
             * If this PassThroughTransform is followed by a matrix discarding some dimensions, identify which dimensions
             * are discarded. If all dimensions computed by the sub-transform are discarded, then we no longer need it.
             * If some pass-through dimensions are discarded, then we can reduce the number of pass-through dimensions.
             */
            if (!applyOtherFirst) {
                final int dimension = m.getNumCol() - 1;            // Number of source dimensions (ignore translations column).
                if (dimension <= Long.SIZE) {                       // Because retained dimensions stored as a mask on 64 bits.
                    long retainedDimensions = 0;
                    final int numRows = m.getNumRow();              // Number of target dimensions + 1.
                    for (int i=0; i<dimension; i++) {
                        for (int j=0; j<numRows; j++) {
                            if (m.getElement(j,i) != 0) {
                                retainedDimensions |= (1L << i);    // Found a source dimension which is required by target dimension.
                                break;
                            }
                        }
                    }
                    /*
                     * Verify if matrix discards the sub-transform. If it does not, then we need to keep all the sub-transform
                     * dimensions (discarding them is a "all or nothing" operation). Other dimensions (leading and trailing)
                     * can be keep or discarded on a case-by-case basis.
                     */
                    final long    fullTransformMask = maskLowBits(dimension);
                    final long    subTransformMask  = maskLowBits(subTransform.getTargetDimensions()) << firstAffectedCoordinate;
                    final boolean keepSubTransform  = (retainedDimensions & subTransformMask) != 0;
                    if (keepSubTransform) {
                        retainedDimensions |= subTransformMask;           // Ensure that we keep all sub-transform dimensions.
                    }
                    if (retainedDimensions != fullTransformMask) {
                        final int change = subTransform.getSourceDimensions() - subTransform.getTargetDimensions();
                        if (change == 0 && !keepSubTransform) {
                            return other;                                 // Shortcut avoiding creation of new MathTransforms.
                        }
                        /*
                         * We enter in this block if some dimensions can be discarded. We want to discard them before the
                         * PassThroughTransform instead of after. The matrix for that purpose will be computed later.
                         * Before that, the loop below modifies a copy of the 'other' matrix as if those dimensions were
                         * already removed.
                         */
                        MatrixSIS reduced = MatrixSIS.castOrCopy(m);
                        long columnsToRemove = ~retainedDimensions & fullTransformMask;       // Can not be 0 at this point.
                        do {
                            final int lower = Long.numberOfTrailingZeros(columnsToRemove);
                            final int upper = Long.numberOfTrailingZeros(~(columnsToRemove | maskLowBits(lower)));
                            reduced = reduced.removeColumns(lower, upper);
                            columnsToRemove &= ~maskLowBits(upper);
                            columnsToRemove >>>= (upper - lower);
                        } while (columnsToRemove != 0);
                        /*
                         * Expands the 'retainedDimensions' bitmask into a list of indices of dimensions to keep.   However
                         * those indices are for dimensions to keep after the PassThroughTransform.  Because we rather want
                         * indices for dimensions to keep before the PassThroughTransform, we need to adjust for difference
                         * in number of dimensions. This change is represented by the 'change' integer computed above.
                         * We apply two strategies:
                         *
                         *    1) If we keep the sub-transform, then the loop while surely sees the 'firstAffectedCoordinate'
                         *       dimension since we ensured that we keep all sub-transform dimensions. When it happens, we
                         *       add or remove bits at that point for the dimensionality changes.
                         *
                         *    2) If we do not keep the sub-transform, then code inside 'if (dim == firstAffectedCoordinate)'
                         *       should not have been executed. Instead we will adjust the indices after the loop.
                         */
                        final long leadPassThroughMask = maskLowBits(firstAffectedCoordinate);
                        final int numKeepAfter  = Long.bitCount(retainedDimensions & ~(leadPassThroughMask | subTransformMask));
                        final int numKeepBefore = Long.bitCount(retainedDimensions & leadPassThroughMask);
                        final int[] indices = new int[Long.bitCount(retainedDimensions) + change];
                        for (int i=0; i<indices.length; i++) {
                            int dim = Long.numberOfTrailingZeros(retainedDimensions);
                            if (dim == firstAffectedCoordinate) {
                                if (change < 0) {
                                    retainedDimensions >>>= -change;                        // Discard dimensions to skip.
                                    retainedDimensions &= ~leadPassThroughMask;             // Clear previous dimension flags.
                                } else {
                                    retainedDimensions <<= change;                          // Add dimensions.
                                    retainedDimensions |= maskLowBits(change) << dim;       // Set flags for new dimensions.
                                }
                            }
                            retainedDimensions &= ~(1L << dim);
                            indices[i] = dim;
                        }
                        if (!keepSubTransform) {
                            for (int i=indices.length; --i >= 0;) {
                                final int dim = indices[i];
                                if (dim <= firstAffectedCoordinate) break;
                                indices[i] = dim - change;
                            }
                        }
                        /*
                         * Concatenate:
                         *   1) An affine transform discarding some dimensions (no other operation).
                         *   2) The passthrough transform with less input and output dimensions.
                         *   3) The 'other' transform with less input dimensions.
                         */
                        MathTransform tr = proxy.linear(Matrices.createDimensionSelect(dimension + change, indices));
                        if (keepSubTransform) {
                            tr = proxy.concatenate(tr, proxy.passThrough(numKeepBefore, subTransform, numKeepAfter));
                        }
                        tr = proxy.concatenate(tr, proxy.linear(reduced));
                        return tr;
                    }
                }
            }
        }
        /*
         * Do not invoke super.tryConcatenate(applyOtherFirst, other, factory); we do not want to test if this transform
         * is the inverse of the other transform as it is costly and unnecessary.  If it was the case, the concatenation
         * of 'this.subTransform' with 'other.subTransform' done at the beginning of this method would have produced the
         * identity transform already.
         */
        return null;
    }

    /**
     * Returns a mask for the {@code n} lowest bits. This is a convenience method for a frequently
     * used operation in {@link #tryConcatenate(boolean, MathTransform, MathTransformFactory)}.
     */
    private static long maskLowBits(final int n) {
        return Numerics.bitmask(n) - 1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        // Note that numTrailingCoordinates is related to source and
        // target dimensions, which are computed by the super-class.
        return super.computeHashCode() ^ (subTransform.hashCode() + firstAffectedCoordinate);
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
            return this.firstAffectedCoordinate == that.firstAffectedCoordinate &&
                   this.numTrailingCoordinates  == that.numTrailingCoordinates  &&
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
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "PassThrough_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(firstAffectedCoordinate);
        if (numTrailingCoordinates != 0) {
            formatter.append(numTrailingCoordinates);
        }
        formatter.append(subTransform);
        if (numTrailingCoordinates != 0) {
            /*
             * setInvalidWKT(…) shall be invoked only after we finished to format
             * sub-transform, otherwise the wrong WKT element will be highlighted.
             */
            formatter.setInvalidWKT(PassThroughTransform.class, null);
        }
        return WKTKeywords.PassThrough_MT;
    }
}
