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

import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;


/**
 * A transform which is linear in the forward direction, but non-linear in the inverse direction.
 * This case happens when the original transform is non-linear, but the inverse of that transform
 * just drops the non-linear dimension. We want the inverse of the inverse to return the original
 * transform.
 *
 * <p>Subclasses must implement {@link #inverse()}. That information is not stored as a field in this
 * {@code OnewayLinearTransform} class because subclasses typically need a specific inverse subclass.
 * Implementations should also override {@link #getContextualParameters()} and related methods.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class OnewayLinearTransform extends AbstractMathTransform.Inverse implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3677320306734738831L;

    /**
     * The transform on which to delegate all operations except inverse.
     */
    @SuppressWarnings("serial")     // Most SIS implementations are serializable.
    protected final LinearTransform delegate;

    /**
     * Creates a new instance which will delegate most operations to the given transform.
     *
     * @param delegate  the transform on which to delegate all operations except inverse.
     */
    protected OnewayLinearTransform(final LinearTransform delegate) {
        this.delegate = delegate;
    }

    /**
     * Case where {@code delegate} is the result of a concatenation of a kernel with normalization
     * and denormalization matrices. Because of optimization, the result of the concatenation is a
     * single {@link LinearTransform} with no information about the steps that produced the result.
     * This class keeps (indirectly) a reference to the contextual parameters.
     */
    static final class Concatenated extends OnewayLinearTransform {
        /** Serial number for inter-operability with different versions. */
        private static final long serialVersionUID = -4439900049126605063L;

        /**
         * The kernel of {@link #delegate}, without the normalization and denormalization.
         * Used mostly for Well Known Text formatting. May be {@code null} if none.
         */
        @SuppressWarnings("serial")     // Most SIS implementations are serializable.
        private final AbstractMathTransform kernel;

        /**
         * The original transform for which this inverse is created.
         */
        @SuppressWarnings("serial")     // Most SIS implementations are serializable.
        private final MathTransform inverse;

        /**
         * Creates a new one-way linear transform.
         *
         * @param delegate  the transform on which to delegate all operations except inverse.
         * @param kernel    the kernel of {@code delegate}, without the normalization and denormalization.
         * @param inverse   the original transform for which this inverse is created.
         */
        Concatenated(final LinearTransform delegate, final AbstractMathTransform kernel, final MathTransform inverse) {
            super(delegate);
            this.kernel  = kernel;
            this.inverse = inverse;
        }

        /**
         * Returns the descriptor of the parameters returned by {@link #getParameterValues()}.
         * See that latter method for more information.
         */
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            final ParameterValueGroup parameters = getParameterValues();
            return (parameters != null) ? parameters.getDescriptor() : null;
        }

        /**
         * Returns the contextual parameters of the {@linkplain #kernel} as the parameters of this
         * concatenated transform. The contextual parameters describes a kernel operation together
         * with its normalization and denormalization matrices. Since those 3 transforms have been
         * combined into a single transform (which is {@link #delegate}), the contextual parameters
         * of the {@linkplain #kernel} applies to the parameters of this concatenated transform.
         */
        @Override
        public ParameterValueGroup getParameterValues() {
            return (kernel != null) ? kernel.getContextualParameters() : null;
        }

        /**
         * Returns the original transform for which this transform is the inverse.
         */
        @Override
        public MathTransform inverse() {
            return inverse;
        }
    }

    /**
     * Returns whether the {@code tr} transform is null or is the actual implementation of {@code wrapper}.
     * This method is used for assertions.
     *
     * @param  tr       the transform which is expected to be null or wrapped.
     * @param  wrapper  the transform which is potentially a wrapper for {@code delegate}.
     * @return whether {@code tr} is null or the implementation of {@code wrapper}.
     */
    static boolean isNullOrDelegate(final MathTransform tr, final MathTransform wrapper) {
        return (tr == null) || (wrapper instanceof OnewayLinearTransform && ((OnewayLinearTransform) wrapper).delegate == tr);
    }

    /**
     * Computes the derivative at the given location.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        return delegate.derivative(point);
    }

    /**
     * Transforms the given array of point and optionally computes the derivative.
     * The implementation delegates to the {@link #delegate} linear transform.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        if (delegate instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) delegate).transform(srcPts, srcOff, dstPts, dstOff, derivate);
        }
        if (dstPts != null) {
            delegate.transform(srcPts, srcOff, dstPts, dstOff, 1);
        }
        return derivate ? delegate.derivative(null) : null;      // Position of a linear transform can be null.
    }

    @Override
    public DirectPosition transform(DirectPosition source, DirectPosition target) throws TransformException {
        return delegate.transform(source, target);
    }

    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        delegate.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        delegate.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        delegate.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        delegate.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public int transform(DoubleBuffer source, DoubleBuffer target) throws TransformException {
        return delegate.transform(source, target);
    }

    @Override
    public int transform(FloatBuffer source, FloatBuffer target) throws TransformException {
        return delegate.transform(source, target);
    }

    @Override
    public int transform(FloatBuffer source, DoubleBuffer target) throws TransformException {
        return delegate.transform(source, target);
    }

    @Override
    public int transform(DoubleBuffer source, FloatBuffer target) throws TransformException {
        return delegate.transform(source, target);
    }
}
