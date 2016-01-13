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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;


/**
 * Projective transform in 2D case.
 * This class is used only if the transform is not affine, i.e. the last row in the 3×3 matrix is not [0 0 1].
 * Otherwise {@link org.apache.sis.internal.referencing.j2d.AffineTransform2D} should be used instead
 * (unless {@link java.awt.geom.AffineTransform} is not available on the target platform).
 *
 * @author  Jan Jezek (UWB)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
final class ProjectiveTransform2D extends ProjectiveTransform implements MathTransform2D, LinearTransform {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3101392684596817045L;

    /**
     * Creates projective transform from a matrix.
     */
    ProjectiveTransform2D(final Matrix matrix) {
        super(matrix);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * Implementation is similar but not identical to {@link AbstractMathTransform2D#transform(Point2D, Point2D)}.
     * The difference is in the {@code transform(…)} method invoked.
     */
    @Override
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) {
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
     * @param  point Ignored, since derivative of a linear transform is the same everywhere.
     * @return The derivative at the specified point as a 2×2 matrix.
     */
    @Override
    public Matrix derivative(final Point2D point) {
        return super.derivative(null);
    }

    /**
     * Creates the inverse transform of this object.
     * The inverse shall be linear and two-dimensional.
     */
    @Override
    public ProjectiveTransform2D inverse() throws NoninvertibleTransformException {
        final LinearTransform inv = super.inverse();
        if (inv instanceof ProjectiveTransform2D) {
            return (ProjectiveTransform2D) inv;
        } else {
            return new ProjectiveTransform2D(inv.getMatrix());
        }
    }
}
