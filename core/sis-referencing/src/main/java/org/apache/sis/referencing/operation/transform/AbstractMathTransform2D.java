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

import java.util.List;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;


/**
 * Base class for math transforms that are known to be two-dimensional in all cases.
 * Two-dimensional math transforms are not required to extend this class,
 * however doing so may simplify their implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-2.0)
 * @version 0.5
 * @module
 */
public abstract class AbstractMathTransform2D extends AbstractMathTransform implements MathTransform2D {
    /**
     * Constructs a default math transform.
     */
    protected AbstractMathTransform2D() {
    }

    /**
     * Returns the dimension of input points, which is always 2.
     */
    @Override
    public final int getSourceDimensions() {
        return 2;
    }

    /**
     * Returns the dimension of output points, which is always 2.
     */
    @Override
    public final int getTargetDimensions() {
        return 2;
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * The default implementation invokes {@link #transform(double[], int, double[], int, boolean)}
     * using a temporary array of doubles.
     *
     * @param  ptSrc The coordinate point to be transformed.
     * @param  ptDst The coordinate point that stores the result of transforming {@code ptSrc},
     *               or {@code null} if a new point shall be created.
     * @return The coordinate point after transforming {@code ptSrc} and storing the result in {@code ptDst},
     *         or in a new point if {@code ptDst} was null.
     * @throws TransformException If the point can not be transformed.
     *
     * @see MathTransform2D#transform(Point2D, Point2D)
     */
    @Override
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException {
        final double[] ord = new double[] {ptSrc.getX(), ptSrc.getY()};
        transform(ord, 0, ord, 0, false);
        if (ptDst != null) {
            ptDst.setLocation(ord[0], ord[1]);
            return ptDst;
        } else {
            return new Point2D.Double(ord[0], ord[1]);
        }
    }

    /**
     * Transform the specified shape. The default implementation computes quadratic curves
     * using three points for each line segment in the shape. The returned object is often
     * a {@link Path2D}, but may also be a {@link Line2D} or a {@link QuadCurve2D} if such
     * simplification is possible.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape, or {@code shape} if this transform is the identity transform.
     * @throws TransformException if a transform failed.
     */
    @Override
    public Shape createTransformedShape(final Shape shape) throws TransformException {
        return isIdentity() ? shape : createTransformedShape(this, shape, null, null, false);
    }

    /**
     * Transforms a geometric shape. This method always copy transformed coordinates in a new object.
     * The new object is often a {@link Path2D}, but may also be a {@link Line2D} or a
     * {@link QuadCurve2D} if such simplification is possible.
     *
     * @param  transform     The transform to use.
     * @param  shape         The geometric shape to transform.
     * @param  preTransform  An optional affine transform to apply <em>before</em> the
     *                       transformation using {@code this}, or {@code null} if none.
     * @param  postTransform An optional affine transform to apply <em>after</em> the transformation
     *                       using {@code this}, or {@code null} if none.
     * @param  horizontal    {@code true} for forcing parabolic equation.
     *
     * @return The transformed geometric shape.
     * @throws MismatchedDimensionException if this transform doesn't is not two-dimensional.
     * @throws TransformException If a transformation failed.
     */
    static Shape createTransformedShape(final MathTransform2D transform,
                                        final Shape           shape,
                                        final AffineTransform preTransform,
                                        final AffineTransform postTransform,
                                        final boolean         horizontal)
            throws TransformException
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the inverse transform of this object.
     * The default implementation returns {@code this} if this transform is an identity transform,
     * or throws an exception otherwise. Subclasses should override this method.
     */
    @Override
    public MathTransform2D inverse() throws NoninvertibleTransformException {
        return (MathTransform2D) super.inverse();
    }

    /**
     * Base class for implementation of inverse math transforms.
     * This inner class is the inverse of the enclosing {@link AbstractMathTransform2D}.
     *
     * {@section Serialization}
     * Instances of this class are serializable only if the enclosing math transform is also serializable.
     * Serialized math transforms are not guaranteed to be compatible with future SIS versions.
     * Serialization, if allowed, should be used only for short term storage or RMI between applications
     * running the same SIS version.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.5 (derived from geotk-3.00)
     * @version 0.5
     * @module
     */
    protected abstract class Inverse extends AbstractMathTransform.Inverse implements MathTransform2D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 5751908928042026412L;

        /**
         * Constructs an inverse math transform.
         */
        protected Inverse() {
            AbstractMathTransform2D.this.super();
        }

        /**
         * Returns the enclosing math transform.
         */
        @Override
        public MathTransform2D inverse() {
            return (MathTransform2D) super.inverse();
        }

        /**
         * Same work than {@link AbstractMathTransform2D#beforeFormat(List, int, boolean)}
         * but with the knowledge that this transform is an inverse transform.
         */
        @Override
        final int beforeFormat(final List<MathTransform> transforms, final int index, final boolean inverse) {
            return AbstractMathTransform2D.this.beforeFormat(transforms, index, !inverse);
        }
    }
}
