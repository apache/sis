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

import java.awt.geom.AffineTransform;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.util.Static;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Utility methods related to {@link MathTransform}s. This class centralizes in one place some of the
 * most commonly used functions from the {@link org.apache.sis.referencing.operation.transform} package,
 * thus reducing the need to explore that low-level package.
 * The {@code MathTransforms} class provides the following services:
 *
 * <ul>
 *   <li>Create various SIS implementations of {@link MathTransform}.</li>
 *   <li>Perform non-standard operations on arbitrary instances.</li>
 * </ul>
 *
 * The factory static methods are provided as convenient alternatives to the GeoAPI {@link MathTransformFactory}
 * interface. However users seeking for more implementation neutrality are encouraged to limit themselves to the
 * GeoAPI factory interfaces instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.20)
 * @version 0.5
 * @module
 *
 * @see MathTransformFactory
 */
public final class MathTransforms extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private MathTransforms() {
    }

    /**
     * Concatenates the two given transforms. The returned transform will implement
     * {@link MathTransform1D} or {@link MathTransform2D} if the dimensions of the
     * concatenated transform are equal to 1 or 2 respectively.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of the first transform
     *         does not match the input dimension of the second transform.
     *
     * @see MathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)
     */
    public static MathTransform concatenate(final MathTransform tr1, final MathTransform tr2)
            throws MismatchedDimensionException
    {
        ensureNonNull("tr1", tr1);
        ensureNonNull("tr2", tr2);
        return ConcatenatedTransform.create(tr1, tr2);
    }

    /**
     * Concatenates the given one-dimensional transforms. This is a convenience methods
     * delegating to {@link #concatenate(MathTransform, MathTransform)} and casting the
     * result to a {@link MathTransform1D} instance.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of the first transform
     *         does not match the input dimension of the second transform.
     */
    public static MathTransform1D concatenate(MathTransform1D tr1, MathTransform1D tr2)
            throws MismatchedDimensionException
    {
        return (MathTransform1D) concatenate((MathTransform) tr1, (MathTransform) tr2);
    }

    /**
     * Concatenates the given two-dimensional transforms. This is a convenience methods
     * delegating to {@link #concatenate(MathTransform, MathTransform)} and casting the
     * result to a {@link MathTransform2D} instance.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of the first transform
     *         does not match the input dimension of the second transform.
     */
    public static MathTransform2D concatenate(MathTransform2D tr1, MathTransform2D tr2)
            throws MismatchedDimensionException
    {
        return (MathTransform2D) concatenate((MathTransform) tr1, (MathTransform) tr2);
    }

    /**
     * Concatenates the three given transforms. This is a convenience methods doing its job
     * as two consecutive concatenations.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @param tr3 The third math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of a transform
     *         does not match the input dimension of next transform.
     */
    public static MathTransform concatenate(MathTransform tr1, MathTransform tr2, MathTransform tr3)
            throws MismatchedDimensionException
    {
        ensureNonNull("tr1", tr1);
        ensureNonNull("tr2", tr2);
        ensureNonNull("tr3", tr3);
        return concatenate(concatenate(tr1, tr2), tr3);
    }

    /**
     * Concatenates the three given one-dimensional transforms. This is a convenience methods
     * delegating to {@link #concatenate(MathTransform, MathTransform, MathTransform)} and
     * casting the result to a {@link MathTransform1D} instance.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @param tr3 The third math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of a transform
     *         does not match the input dimension of next transform.
     */
    public static MathTransform1D concatenate(MathTransform1D tr1, MathTransform1D tr2, MathTransform1D tr3)
            throws MismatchedDimensionException
    {
        return (MathTransform1D) concatenate((MathTransform) tr1, (MathTransform) tr2, (MathTransform) tr3);
    }

    /**
     * Concatenates the three given two-dimensional transforms. This is a convenience methods
     * delegating to {@link #concatenate(MathTransform, MathTransform, MathTransform)} and
     * casting the result to a {@link MathTransform2D} instance.
     *
     * @param tr1 The first math transform.
     * @param tr2 The second math transform.
     * @param tr3 The third math transform.
     * @return    The concatenated transform.
     * @throws MismatchedDimensionException if the output dimension of a transform
     *         does not match the input dimension of next transform.
     */
    public static MathTransform2D concatenate(MathTransform2D tr1, MathTransform2D tr2, MathTransform2D tr3)
            throws MismatchedDimensionException
    {
        return (MathTransform2D) concatenate((MathTransform) tr1, (MathTransform) tr2, (MathTransform) tr3);
    }

    /**
     * If the given transform is linear, returns its coefficients as a matrix.
     * More specifically:
     *
     * <ul>
     *   <li>If the given transform is an instance of {@link LinearTransform}, returns
     *       {@link LinearTransform#getMatrix()}.</li>
     *   <li>Otherwise if the given transform is an instance of {@link AffineTransform},
     *       returns its coefficients in a {@link Matrix3} instance.</li>
     *   <li>Otherwise returns {@code null}.</li>
     * </ul>
     *
     * @param  transform The transform, or {@code null}.
     * @return The matrix of the given transform, or {@code null} if none.
     */
    public static Matrix getMatrix(final MathTransform transform) {
        if (transform instanceof LinearTransform) {
            return ((LinearTransform) transform).getMatrix();
        }
        if (transform instanceof AffineTransform) {
            return AffineTransforms2D.toMatrix((AffineTransform) transform);
        }
        return null;
    }

    /**
     * A buckle method for calculating derivative and coordinate transformation in a single step.
     * The transform result is stored in the given destination array, and the derivative matrix
     * is returned. Invoking this method is equivalent to the following code, except that it may
     * execute faster with some {@code MathTransform} implementations:
     *
     * {@preformat java
     *     DirectPosition ptSrc = ...;
     *     DirectPosition ptDst = ...;
     *     Matrix matrixDst = derivative(ptSrc);
     *     ptDst = transform(ptSrc, ptDst);
     * }
     *
     * @param transform The transform to use.
     * @param srcPts The array containing the source coordinate.
     * @param srcOff The offset to the point to be transformed in the source array.
     * @param dstPts the array into which the transformed coordinate is returned.
     * @param dstOff The offset to the location of the transformed point that is stored in the destination array.
     * @return The matrix of the transform derivative at the given source position.
     * @throws TransformException If the point can't be transformed or if a problem occurred
     *         while calculating the derivative.
     */
    public static Matrix derivativeAndTransform(final MathTransform transform,
                                                final double[] srcPts, final int srcOff,
                                                final double[] dstPts, final int dstOff)
            throws TransformException
    {
        if (transform instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) transform).transform(srcPts, srcOff, dstPts, dstOff, true);
        }
        // Must be calculated before to transform the coordinate.
        final Matrix derivative = transform.derivative(new DirectPositionView(srcPts, srcOff, transform.getSourceDimensions()));
        if (dstPts != null) {
            transform.transform(srcPts, srcOff, dstPts, dstOff, 1);
        }
        return derivative;
    }
}
