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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.geometry.DirectPosition2D;


/**
 * A pass-through transform in the two-dimensional case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class PassThroughTransform2D extends PassThroughTransform implements MathTransform2D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5637760772953973708L;

    /**
     * Creates a pass through transform.
     *
     * @param firstAffectedOrdinate Index of the first affected ordinate.
     * @param subTransform The sub transform.
     * @param numTrailingOrdinates Number of trailing ordinates to pass through.
     */
    PassThroughTransform2D(final int firstAffectedOrdinate,
                           final MathTransform subTransform,
                           final int numTrailingOrdinates)
    {
        super(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * Implementation is similar but not identical to {@link AbstractMathTransform2D#transform(Point2D, Point2D)}.
     * The difference is in the {@code transform(…)} method invoked.
     */
    @Override
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException {
        final double[] ord = new double[] {ptSrc.getX(), ptSrc.getY()};
        transform(ord, 0, ord, 0, 1);
        if (ptDst != null) {
            ptDst.setLocation(ord[0], ord[1]);
            return ptDst;
        } else {
            return new Point2D.Double(ord[0], ord[1]);
        }
    }

    /**
     * Transforms the specified shape.
     */
    @Override
    public Shape createTransformedShape(final Shape shape) throws TransformException {
        return AbstractMathTransform2D.createTransformedShape(this, shape, null, null, false);
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @return {@inheritDoc}
     * @throws TransformException If the {@linkplain #getSubTransform() sub-transform} failed.
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
    public synchronized MathTransform2D inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            inverse = new PassThroughTransform2D(
                    firstAffectedOrdinate, subTransform.inverse(), numTrailingOrdinates);
            inverse.inverse = this;
        }
        return (MathTransform2D) inverse;
    }
}
