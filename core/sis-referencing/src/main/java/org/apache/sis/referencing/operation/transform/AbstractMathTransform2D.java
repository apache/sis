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
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.util.resources.Errors;


/**
 * Base class for math transforms that are known to be two-dimensional in all cases.
 * Two-dimensional math transforms are not required to extend this class,
 * however doing so may simplify their implementation.
 *
 * <p>The simplest way to implement this abstract class is to provide an implementation for the following methods
 * only:</p>
 * <ul>
 *   <li>{@link #transform(double[], int, double[], int, boolean)}</li>
 * </ul>
 *
 * However more performance may be gained by overriding the other {@code transform} methods as well.
 *
 * <div class="section">Immutability and thread safety</div>
 * All Apache SIS implementations of {@code MathTransform2D} are immutable and thread-safe.
 * It is highly recommended that third-party implementations be immutable and thread-safe too.
 * This means that unless otherwise noted in the javadoc, {@code MathTransform2D} instances can
 * be shared by many objects and passed between threads without synchronization.
 *
 * <div class="section">Serialization</div>
 * {@code MathTransform2D} may or may not be serializable, at implementation choices.
 * Most Apache SIS implementations are serializable, but the serialized objects are not guaranteed to be compatible
 * with future SIS versions. Serialization should be used only for short term storage or RMI between applications
 * running the same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class AbstractMathTransform2D extends AbstractMathTransform implements MathTransform2D {
    /**
     * Constructor for subclasses.
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
        return transform(this, ptSrc, ptDst);
    }

    /**
     * Implementation of {@link #transform(Point2D, Point2D)} shared by the inverse transform.
     */
    static Point2D transform(final AbstractMathTransform tr, final Point2D ptSrc, final Point2D ptDst) throws TransformException {
        final double[] ord = new double[] {ptSrc.getX(), ptSrc.getY()};
        tr.transform(ord, 0, ord, 0, false);
        if (ptDst != null) {
            ptDst.setLocation(ord[0], ord[1]);
            return ptDst;
        } else {
            return new Point2D.Double(ord[0], ord[1]);
        }
    }

    /**
     * Transforms the specified shape. The default implementation computes quadratic curves
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
     * The new object is often a {@link Path2D}, but may also be a {@link Line2D} or a {@link QuadCurve2D}
     * if such simplification is possible.
     *
     * @param  mt            The math transform to use.
     * @param  shape         The geometric shape to transform.
     * @param  preTransform  An optional affine transform to apply <em>before</em> the
     *                       transformation using {@code this}, or {@code null} if none.
     * @param  postTransform An optional affine transform to apply <em>after</em> the transformation
     *                       using {@code this}, or {@code null} if none.
     * @param  horizontal    {@code true} for forcing parabolic equation.
     *
     * @return The transformed geometric shape.
     * @throws TransformException If a transformation failed.
     */
    static Shape createTransformedShape(final MathTransform2D mt,
                                        final Shape           shape,
                                        final AffineTransform preTransform,
                                        final AffineTransform postTransform,
                                        final boolean         horizontal)
            throws TransformException
    {
        final PathIterator     it = shape.getPathIterator(preTransform);
        final Path2D.Double  path = new Path2D.Double(it.getWindingRule());
        final double[]     buffer = new double[6];

        double ax=0, ay=0;  // Coordinate of the last point before transform.
        double px=0, py=0;  // Coordinate of the last point after  transform.
        for (; !it.isDone(); it.next()) {
            switch (it.currentSegment(buffer)) {
                default: {
                    throw new IllegalPathStateException();
                }
                case PathIterator.SEG_CLOSE: {
                    /*
                     * Close the geometric shape and continues the loop. We use the 'continue' instruction
                     * here instead of 'break' because we do not want to execute the code after the switch
                     * (addition of transformed points into the path - there is no such point in a SEG_CLOSE).
                     */
                    path.closePath();
                    continue;
                }
                case PathIterator.SEG_MOVETO: {
                    /*
                     * Transform the single point and adds it to the path. We use the 'continue' instruction
                     * here instead of 'break' because we do not want to execute the code after the switch
                     * (addition of a line or a curve - there is no such curve to add here; we are just moving
                     * the cursor).
                     */
                    ax = buffer[0];
                    ay = buffer[1];
                    mt.transform(buffer, 0, buffer, 0, 1);
                    px = buffer[0];
                    py = buffer[1];
                    path.moveTo(px, py);
                    continue;
                }
                case PathIterator.SEG_LINETO: {
                    /*
                     * Insert a new control point at 'buffer[0,1]'. This control point will
                     * be initialised with coordinates in the middle of the straight line:
                     *
                     *  x = 0.5 * (x1+x2)
                     *  y = 0.5 * (y1+y2)
                     *
                     * This point will be transformed after the 'switch', which is why we use
                     * the 'break' statement here instead of 'continue' as in previous case.
                     */
                    buffer[0] = 0.5 * (ax + (ax = buffer[0]));
                    buffer[1] = 0.5 * (ay + (ay = buffer[1]));
                    buffer[2] = ax;
                    buffer[3] = ay;
                    break;
                }
                case PathIterator.SEG_QUADTO: {
                    /*
                     * Replace the control point in 'buffer[0,1]' by a new control point lying on the quadratic curve.
                     * Coordinates for a point in the middle of the curve can be computed with:
                     *
                     *  x = 0.5 * (ctrlx + 0.5 * (x1+x2))
                     *  y = 0.5 * (ctrly + 0.5 * (y1+y2))
                     *
                     * There is no need to keep the old control point because it was not lying on the curve.
                     */
                    buffer[0] = 0.5 * (buffer[0] + 0.5*(ax + (ax = buffer[2])));
                    buffer[1] = 0.5 * (buffer[1] + 0.5*(ay + (ay = buffer[3])));
                    break;
                }
                case PathIterator.SEG_CUBICTO: {
                    /*
                     * Replace the control point in 'buffer[0,1]' by a new control point lying on the cubic curve.
                     * Coordinates for a point in the middle of the curve can be computed with:
                     *
                     *  x = 0.25 * (1.5 * (ctrlx1 + ctrlx2) + 0.5 * (x1 + x2));
                     *  y = 0.25 * (1.5 * (ctrly1 + ctrly2) + 0.5 * (y1 + y2));
                     *
                     * There is no need to keep the old control point because it was not lying on the curve.
                     *
                     * NOTE: The computed point is on the curve, but may not be representative of the shape.
                     *       This algorithm replaces two control points by a single one, because we did not
                     *       venture into a more sophisticated algorithm producing a CubicCurve2D. For now,
                     *       we presume that the current algorithm is okay if the curve is smooth enough.
                     */
                    buffer[0] = 0.25 * (1.5 * (buffer[0] + buffer[2]) + 0.5 * (ax + (ax = buffer[4])));
                    buffer[1] = 0.25 * (1.5 * (buffer[1] + buffer[3]) + 0.5 * (ay + (ay = buffer[5])));
                    buffer[2] = ax;
                    buffer[3] = ay;
                    break;
                }
            }
            /*
             * Apply the transform on the point in the buffer, and append the transformed points
             * to the general path. Try to add them as a quadratic line, or as a straight line if
             * the computed control point is colinear with the starting and ending points.
             */
            mt.transform(buffer, 0, buffer, 0, 2);
            final Point2D ctrlPoint = ShapeUtilities.parabolicControlPoint(px, py,
                    buffer[0], buffer[1],
                    buffer[2], buffer[3],
                    horizontal);
            px = buffer[2];
            py = buffer[3];
            if (ctrlPoint != null) {
                path.quadTo(ctrlPoint.getX(), ctrlPoint.getY(), px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        /*
         * Shape transformation is done. Apply an affine transform if it was requested,
         * then simplify the geometric object (not the coordinate values) if possible.
         */
        if (postTransform != null) {
            path.transform(postTransform);
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Gets the derivative of this transform at a point.
     * The default implementation performs the following steps:
     *
     * <ul>
     *   <li>Copy the coordinate in a temporary array and pass that array to the
     *       {@link #transform(double[], int, double[], int, boolean)} method,
     *       with the {@code derivate} boolean argument set to {@code true}.</li>
     *   <li>If the later method returned a non-null matrix, returns that matrix.
     *       Otherwise throws {@link TransformException}.</li>
     * </ul>
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point as a 2×2 matrix.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final Point2D point) throws TransformException {
        return derivative(this, point);
    }

    /**
     * Implementation of {@link #derivative(DirectPosition)} shared by the inverse transform.
     */
    static Matrix derivative(final AbstractMathTransform tr, final Point2D point) throws TransformException {
        final double[] coordinate = new double[] {point.getX(), point.getY()};
        final Matrix derivative = tr.transform(coordinate, 0, null, 0, true);
        if (derivative == null) {
            throw new TransformException(Errors.format(Errors.Keys.CanNotComputeDerivative));
        }
        return derivative;
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
     * <div class="section">Serialization</div>
     * Instances of this class are serializable only if the enclosing math transform is also serializable.
     * Serialized math transforms are not guaranteed to be compatible with future SIS versions.
     * Serialization, if allowed, should be used only for short term storage or RMI between applications
     * running the same SIS version.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.5
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
            return AbstractMathTransform2D.transform(this, ptSrc, ptDst);
        }

        /**
         * Transforms the specified shape. The default implementation computes quadratic curves
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
            return isIdentity() ? shape : AbstractMathTransform2D.createTransformedShape(this, shape, null, null, false);
        }

        /**
         * Gets the derivative of this transform at a point.
         * The default implementation performs the following steps:
         *
         * <ul>
         *   <li>Copy the coordinate in a temporary array and pass that array to the
         *       {@link #transform(double[], int, double[], int, boolean)} method,
         *       with the {@code derivate} boolean argument set to {@code true}.</li>
         *   <li>If the later method returned a non-null matrix, returns that matrix.
         *       Otherwise throws {@link TransformException}.</li>
         * </ul>
         *
         * @param  point The coordinate point where to evaluate the derivative.
         * @return The derivative at the specified point as a 2×2 matrix.
         * @throws TransformException if the derivative can not be evaluated at the specified point.
         */
        @Override
        public Matrix derivative(final Point2D point) throws TransformException {
            return AbstractMathTransform2D.derivative(this, point);
        }
    }
}
