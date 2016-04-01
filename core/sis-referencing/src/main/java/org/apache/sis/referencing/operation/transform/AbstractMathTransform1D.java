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

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix1;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * Base class for math transforms that are known to be one-dimensional in all cases.
 * One-dimensional math transforms are not required to extend this class,
 * however doing so may simplify their implementation.
 *
 * <p>The simplest way to implement this abstract class is to provide an implementation for the following methods
 * only:</p>
 * <ul>
 *   <li>{@link #transform(double)}</li>
 *   <li>{@link #derivative(double)}</li>
 * </ul>
 *
 * <div class="section">Immutability and thread safety</div>
 * All Apache SIS implementations of {@code MathTransform1D} are immutable and thread-safe.
 * It is highly recommended that third-party implementations be immutable and thread-safe too.
 * This means that unless otherwise noted in the javadoc, {@code MathTransform1D} instances can
 * be shared by many objects and passed between threads without synchronization.
 *
 * <div class="section">Serialization</div>
 * {@code MathTransform1D} may or may not be serializable, at implementation choices.
 * Most Apache SIS implementations are serializable, but the serialized objects are not guaranteed to be compatible
 * with future SIS versions. Serialization should be used only for short term storage or RMI between applications
 * running the same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class AbstractMathTransform1D extends AbstractMathTransform implements MathTransform1D {
    /**
     * Constructor for subclasses.
     */
    protected AbstractMathTransform1D() {
    }

    /**
     * Returns the dimension of input points, which is always 1.
     */
    @Override
    public final int getSourceDimensions() {
        return 1;
    }

    /**
     * Returns the dimension of output points, which is always 1.
     */
    @Override
    public final int getTargetDimensions() {
        return 1;
    }

    /**
     * Transforms the specified value.
     *
     * @param  value The value to transform.
     * @return the transformed value.
     * @throws TransformException if the value can not be transformed.
     */
    @Override
    public abstract double transform(double value) throws TransformException;

    /**
     * Transforms a single point in the given array and opportunistically computes its derivative if requested.
     * The default implementation delegates to {@link #transform(double)} and potentially to {@link #derivative(double)}.
     * Subclasses may override this method for performance reason.
     *
     * @return {@inheritDoc}
     * @throws TransformException {@inheritDoc}
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double ordinate = srcPts[srcOff];
        if (dstPts != null) {
            dstPts[dstOff] = transform(ordinate);
        }
        return derivate ? new Matrix1(derivative(ordinate)) : null;
    }

    /**
     * Gets the derivative of this function at a value. The derivative is the 1×1 matrix
     * of the non-translating portion of the approximate affine map at the value.
     *
     * @param  value The value where to evaluate the derivative.
     * @return The derivative at the specified point.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public abstract double derivative(double value) throws TransformException;

    /**
     * Gets the derivative of this transform at a point. The default implementation ensures that
     * {@code point} is one-dimensional, then delegates to {@link #derivative(double)}.
     *
     * @param  point The coordinate point where to evaluate the derivative, or {@code null}.
     * @return The derivative at the specified point (never {@code null}).
     * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final double ordinate;
        if (point == null) {
            ordinate = Double.NaN;
        } else {
            ensureDimensionMatches("point", 1, point);
            ordinate = point.getOrdinate(0);
        }
        return new Matrix1(derivative(ordinate));
    }

    /**
     * Returns the inverse transform of this object.
     * The default implementation returns {@code this} if this transform is an identity transform,
     * or throws an exception otherwise. Subclasses should override this method.
     */
    @Override
    public MathTransform1D inverse() throws NoninvertibleTransformException {
        return (MathTransform1D) super.inverse();
    }

    /**
     * Base class for implementation of inverse math transforms.
     * This inner class is the inverse of the enclosing {@link AbstractMathTransform1D}.
     *
     * <div class="section">Serialization</div>
     * Instances of this class are serializable only if the enclosing math transform is also serializable.
     * Serialized math transforms are not guaranteed to be compatible with future SIS versions.
     * Serialization, if allowed, should be used only for short term storage or RMI between applications
     * running the same SIS version.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    protected abstract class Inverse extends AbstractMathTransform.Inverse implements MathTransform1D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 2018412413506158560L;

        /**
         * Constructs an inverse math transform.
         */
        protected Inverse() {
            AbstractMathTransform1D.this.super();
        }

        /**
         * Returns the enclosing math transform.
         */
        @Override
        public MathTransform1D inverse() {
            return (MathTransform1D) super.inverse();
        }

        /**
         * Transforms a single point in the given array and opportunistically computes its derivative if requested.
         * The default implementation delegates to {@link #transform(double)} and potentially to {@link #derivative(double)}.
         * Subclasses may override this method for performance reason.
         *
         * @return {@inheritDoc}
         * @throws TransformException {@inheritDoc}
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            final double ordinate = srcPts[srcOff];
            if (dstPts != null) {
                dstPts[dstOff] = transform(ordinate);
            }
            return derivate ? new Matrix1(derivative(ordinate)) : null;
        }

        /**
         * Gets the derivative of this transform at a point. The default implementation ensures that
         * {@code point} is one-dimensional, then delegates to {@link #derivative(double)}.
         *
         * @param  point The coordinate point where to evaluate the derivative, or {@code null}.
         * @return The derivative at the specified point (never {@code null}).
         * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
         * @throws TransformException if the derivative can not be evaluated at the specified point.
         */
        @Override
        public Matrix derivative(final DirectPosition point) throws TransformException {
            final double ordinate;
            if (point == null) {
                ordinate = Double.NaN;
            } else {
                ensureDimensionMatches("point", 1, point);
                ordinate = point.getOrdinate(0);
            }
            return new Matrix1(derivative(ordinate));
        }
    }
}
