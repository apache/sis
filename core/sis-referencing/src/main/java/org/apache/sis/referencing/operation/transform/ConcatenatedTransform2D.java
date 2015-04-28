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

import java.awt.Shape;
import java.awt.geom.Point2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;


/**
 * Concatenated transform in which the resulting transform is two-dimensional.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class ConcatenatedTransform2D extends ConcatenatedTransform implements MathTransform2D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7307709788564866500L;

    /**
     * Constructs a concatenated transform.
     */
    ConcatenatedTransform2D(final MathTransform transform1,
                            final MathTransform transform2)
    {
        super(transform1, transform2);
    }

    /**
     * Checks if transforms are compatibles with this implementation.
     */
    @Override
    boolean isValid() {
        return super.isValid() && (getSourceDimensions() == 2) && (getTargetDimensions() == 2);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * This method is a copy of {@link AbstractMathTransform2D#transform(Point2D, Point2D)}.
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
     * Transforms the specified shape.
     *
     * @param  shape Shape to transform.
     * @return Transformed shape.
     * @throws TransformException if a transform failed.
     */
    @Override
    public Shape createTransformedShape(final Shape shape) throws TransformException {
        return AbstractMathTransform2D.createTransformedShape(this, shape, null, null, false);
    }

    /**
     * Gets the derivative of this transform at a point. This method delegates to the
     * {@link #derivative(DirectPosition)} method because the transformation steps
     * {@link #transform1} and {@link #transform2} may not be instances of {@link MathTransform2D}.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point as a 2×2 matrix.
     * @throws TransformException if the derivative can't be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final Point2D point) throws TransformException {
        return super.derivative(point instanceof DirectPosition ?
                (DirectPosition) point : new DirectPosition2D(point.getX(), point.getY()));
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public MathTransform2D inverse() throws NoninvertibleTransformException {
        return (MathTransform2D) super.inverse();
    }
}
