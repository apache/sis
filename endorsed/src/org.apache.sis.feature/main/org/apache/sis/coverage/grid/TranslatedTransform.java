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
package org.apache.sis.coverage.grid;

import java.util.List;
import java.util.Arrays;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * A transform which forces a translation in one specific dimension before to apply another transform.
 * This class intentionally blocks the optimization which consists in optimizing two consecutive linear
 * transforms with a matrix multiplication. The purpose of this transform is to replace some coordinate
 * values by zero before the matrix multiplication is applied, because Apache <abbr>SIS</abbr> handles
 * 0 × NaN in a special resulting in 0 instead of NaN (okay if NaN is interpreted as "any finite number").
 *
 * <p>The current implementation has no tolerance threshold,
 * but a future implementation could add such tolerance if it appears useful.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TranslatedTransform extends AbstractMathTransform {
    /**
     * The dimension where to apply the translation.
     */
    private final int dimension;

    /**
     * The offset to apply in the specified dimension.
     */
    private final double offset;

    /**
     * Tolerance factor for deciding that a value was equal to {@link #offset}.
     */
    private final double tolerance;

    /**
     * The transform to apply after the translation.
     */
    private final MathTransform transform;

    /**
     * Creates a new transform applying the given offset.
     *
     * @param dimension  the dimension where to apply the translation.
     * @param offset     the offset to apply in the specified dimension.
     * @param transform  the transform to apply after the translation.
     */
    private TranslatedTransform(final int dimension, final double offset, final MathTransform transform) {
        this.dimension = dimension;
        this.offset    = offset;
        this.tolerance = Math.abs(offset) * Numerics.COMPARISON_THRESHOLD;
        this.transform = transform;
    }

    /**
     * Returns a transform equivalent to the given {@code crsToGrid} transform, but potentially with
     * workarounds for making possible to transform grid origin despite NaN values in scale factors.
     * The workaround consists in translating the input coordinates ("real world") in such a way that,
     * for the point that corresponds to grid origin, the NaN scale factors are multiplied by zero.
     * This workaround relies on Apache <abbr>SIS</abbr> making a special case for NaN × 0 = 0.
     *
     * @param  crsToGrid     the transform on which to apply workaround.
     * @param  gridGeometry  the grid geometry from which the transform was extracted.
     * @return {@code crsToGrid} or a transform equivalent to {@code crsToGrid} with workarounds.
     * @throws NoninvertibleTransformException if this method cannot compute the inverse of a {@code crsToGrid} step.
     */
    static MathTransform resolveNaN(MathTransform crsToGrid, final GridGeometry gridGeometry)
            throws NoninvertibleTransformException
    {
        MathTransform analyzing = MathTransforms.getLastStep(crsToGrid);
        final Matrix toGrid = MathTransforms.getMatrix(analyzing);
        if (toGrid != null) {
            long dimensionBitMask = 0;      // Bitmask of dimensions for which to create `TranslatedTransform`.
            long zeroingGridIndex = 0;      // Bitmask of grid dimensions where to force translation term to zero.
            /*
             * A NaN value in the translation column may be the consequence of a NaN scale factor.
             * Typically, a real non-zero value existed in the "grid to CRS" column, which was the
             * real world coordinate at grid index 0 in a dimension of unknown resolution. But for
             * the inverse of that matrix ("CRS to grid"), there is no translation term which will
             * give the desired result for a non-zero real world coordinate while resulting in NaN
             * for all other real world coordinates. Since no number exists with those properties,
             * the translation term had to be NaN. However, it become possible to get the desired
             * behavior if we translate the real world coordinate from non-zero to zero.
             */
            for (int j = toGrid.getNumRow() - 1; --j >= 0;) {
                int  i = toGrid.getNumCol() - 1;
                if (Double.isNaN(toGrid.getElement(j, i))) {
                    while (--i >= 0) {
                        if (Double.isNaN(toGrid.getElement(j, i))) {
                            dimensionBitMask |= (1L << i);
                            zeroingGridIndex |= (1L << j);  // Set only if at least one scale factor is NaN.
                            if ((i | j) >= Long.SIZE) {
                                throw new ArithmeticException(Errors.format(
                                        Errors.Keys.ExcessiveNumberOfDimensions_1, Math.max(i, j) + 1));
                            }
                        }
                    }
                }
            }
            /*
             * If at least one scale factor is NaN, get the translation to apply for allowing those
             * scale factors to be multiplied by coordinate value 0 when the point is at grid origin.
             * We use the "pixel center" convention when possible, or "pixel corner" as a fallback.
             * Since the resolution is unknown, only one of center/corner conventions may be available.
             */
            if (zeroingGridIndex != 0) {
                Matrix fromGrid, fallback;
                final MathTransform fromCenter = MathTransforms.getFirstStep(gridGeometry.gridToCRS);
                final MathTransform fromCorner = MathTransforms.getFirstStep(gridGeometry.cornerToCRS);
                analyzing = analyzing.inverse();
                if (Utilities.equalsApproximately(analyzing, fromCenter) ||
                    Utilities.equalsApproximately(analyzing, fromCorner))
                {
                    fromGrid = MathTransforms.getMatrix(fromCenter);
                    fallback = MathTransforms.getMatrix(fromCorner);
                    if (fromGrid == null) {
                        fromGrid = fallback;
                        fallback = null;
                    }
                } else {  // Happens if we have more than one step.
                    fromGrid = MathTransforms.getMatrix(analyzing);
                    fallback = null;
                }
                if (fromGrid != null) {
                    /*
                     * Get the translation terms of the matrix, but only the ones that are real numbers
                     * and only in the dimensions where at least one scale factor is NaN.
                     */
                    final int translationColumn = fromGrid.getNumCol() - 1;
                    final double[] offsets = new double[fromGrid.getNumRow()];
                    offsets[offsets.length - 1] = 1;
                    while (dimensionBitMask != 0) {
                        final int j = Long.numberOfTrailingZeros(dimensionBitMask);
                        dimensionBitMask &= ~(1L << j);
                        double offset = fromGrid.getElement(j, translationColumn);
                        if (Double.isNaN(offset)) {
                            if (fallback == null || Double.isNaN(offset = fallback.getElement(j, translationColumn))) {
                                zeroingGridIndex &= ~(1L << j);
                                continue;
                            }
                        }
                        offsets[j] = offset;
                    }
                    /*
                     * Since we are going to subtract `offsets` from input coordinates, we need to add `offsets`
                     * back in order to get the same result. This is done by `translate(…)` call. It will have no
                     * effect on the translation terms that are NaN, despite those NaN being the reason why we do
                     * all this stuff. But the coordinates modified by `offsets` may have an impact on some terms
                     * in other rows.
                     */
                    if (zeroingGridIndex != 0) {
                        final MatrixSIS translated = Matrices.copy(toGrid);
                        translated.translate(offsets);
                        do {
                            final int j = Long.numberOfTrailingZeros(zeroingGridIndex);
                            translated.setElement(j, translated.getNumCol() - 1, 0);
                            zeroingGridIndex &= ~(1L << j);
                        } while (zeroingGridIndex != 0);
                        /*
                         * Rebuild a chain of transforms from last step to first step, but with translations
                         * before the affine transform in order to get NaN × 0 = 0 operations when possible.
                         */
                        final List<MathTransform> steps = MathTransforms.getSteps(crsToGrid);
                        crsToGrid = MathTransforms.linear(translated);
                        for (int dimension = offsets.length - 2; dimension >= 0; dimension--) {
                            final double offset = offsets[dimension];
                            if (offset != 0) {
                                crsToGrid = new TranslatedTransform(dimension, offset, crsToGrid);
                            }
                        }
                        for (int i = steps.size() - 2; i >= 0; i--) {   // Omit last step because it has been replaced.
                            crsToGrid = MathTransforms.concatenate(steps.get(i), crsToGrid);
                        }
                    }
                }
            }
        }
        return crsToGrid;
    }

    /**
     * Returns the number of dimensions of input points.
     */
    @Override
    public int getSourceDimensions() {
        return transform.getSourceDimensions();
    }

    /**
     * Returns the number of dimensions of output points.
     */
    @Override
    public int getTargetDimensions() {
        return transform.getTargetDimensions();
    }

    /**
     * Transforms a single coordinate tuple in an array, optionally with the transform derivative at that location.
     *
     * @param  srcPts    the array containing the source coordinates (cannot be {@code null}).
     * @param  srcOff    the offset to the point to be transformed in the source array.
     * @param  dstPts    the array into which the transformed coordinates is returned.
     * @param  dstOff    the offset to the location of the transformed point that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the transform derivative at the given source position, or {@code null}.
     * @throws TransformException if the point or the derivative cannot be computed.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        Matrix derivative = null;
        if (derivate) {
            final double[] coordinates = Arrays.copyOfRange(srcPts, srcOff, srcOff + getSourceDimensions());
            if (Math.abs(coordinates[dimension] -= offset) < tolerance) {
                coordinates[dimension] = 0;
            }
            derivative = transform.derivative(new DirectPositionView.Double(coordinates));
        }
        transform(srcPts, srcOff, dstPts, dstOff, 1);
        return derivative;
    }

    /**
     * Transforms a list of coordinate tuples.
     *
     * <h4>Implementation note</h4>
     * In principle, we should not modify source coordinates as below.
     * However, this transform is for {@link DefaultEvaluator} internal usage
     * and is used in contexts where it is okay to overwrite source coordinates.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point cannot be transformed.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff, final int numPts)
            throws TransformException
    {
        final int srcDim = getSourceDimensions();
        final int stop = srcOff + numPts * srcDim;
        for (int i = srcOff + dimension; i < stop; i+= srcDim) {
            if (Math.abs(srcPts[i] -= offset) < tolerance) {
                srcPts[i] = 0;
            }
        }
        transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }
}
