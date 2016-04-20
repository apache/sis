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
package org.apache.sis.referencing.operation.matrix;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static java.awt.geom.AffineTransform.*;


/**
 * Bridge between {@link Matrix} and Java2D {@link AffineTransform} instances.
 * Those {@code AffineTransform} instances can be viewed as 3×3 matrices.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class AffineTransforms2D extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private AffineTransforms2D() {
    }

    /**
     * Returns the given matrix as a Java2D affine transform.
     * If the given matrix is already an instance of {@link AffineTransform}, then it is returned directly.
     * Otherwise the values are copied in a new {@code AffineTransform} instance.
     *
     * @param  matrix The matrix to returns as an affine transform, or {@code null}.
     * @return The matrix argument if it can be safely casted (including {@code null} argument),
     *         or a copy of the given matrix otherwise.
     * @throws IllegalArgumentException if the given matrix size is not 3×3 or if the matrix is not affine.
     *
     * @see Matrices#isAffine(Matrix)
     */
    public static AffineTransform castOrCopy(final Matrix matrix) throws IllegalArgumentException {
        if (matrix == null || matrix instanceof AffineTransform) {
            return (AffineTransform) matrix;
        }
        MatrixSIS.ensureSizeMatch(3, 3, matrix);
        if (!Matrices.isAffine(matrix)) {
            throw new IllegalStateException(Errors.format(Errors.Keys.NotAnAffineTransform));
        }
        return new AffineTransform(matrix.getElement(0,0), matrix.getElement(1,0),
                                   matrix.getElement(0,1), matrix.getElement(1,1),
                                   matrix.getElement(0,2), matrix.getElement(1,2));
    }

    /**
     * Creates a 3×3 matrix from the given affine transform.
     *
     * @param  transform The affine transform to copy as a matrix.
     * @return A matrix containing the same terms than the given affine transform.
     */
    public static Matrix3 toMatrix(final AffineTransform transform) {
        return new Matrix3(transform.getScaleX(), transform.getShearX(), transform.getTranslateX(),
                           transform.getShearY(), transform.getScaleY(), transform.getTranslateY(),
                           0,                     0,                     1);
    }

    /**
     * Transforms the given shape.
     * This method is similar to {@link AffineTransform#createTransformedShape(Shape)} except that:
     *
     * <ul>
     *   <li>It tries to preserve the shape kind when possible. For example if the given shape
     *       is an instance of {@link RectangularShape} and the given transform does not involve
     *       rotation, then the returned shape may be some instance of the same class.</li>
     *   <li>It tries to recycle the given object if {@code overwrite} is {@code true}.</li>
     * </ul>
     *
     * @param transform      The affine transform to use.
     * @param shape          The shape to transform, or {@code null}.
     * @param allowOverwrite If {@code true}, this method is allowed to overwrite {@code shape} with the
     *                       transform result. If {@code false}, then {@code shape} is never modified.
     *
     * @return The transform of the given shape, or {@code null} if the given shape was null.
     *         May or may not be the same instance than the given shape.
     *
     * @see AffineTransform#createTransformedShape(Shape)
     */
    public static Shape transform(final AffineTransform transform, Shape shape, boolean allowOverwrite) {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (shape == null) {
            return null;
        }
        final int type = transform.getType();
        if (type == TYPE_IDENTITY) {
            return shape;
        }
        // If there is only scale, flip, quadrant rotation or translation,
        // then we can optimize the transformation of rectangular shapes.
        if ((type & (TYPE_GENERAL_ROTATION | TYPE_GENERAL_TRANSFORM)) == 0) {
            // For a Rectangle input, the output should be a rectangle as well.
            if (shape instanceof Rectangle2D) {
                final Rectangle2D rect = (Rectangle2D) shape;
                return transform(transform, rect, allowOverwrite ? rect : null);
            }
            // For other rectangular shapes, we restrict to cases without
            // rotation or flip because we don't know if the shape is symmetric.
            if ((type & (TYPE_FLIP | TYPE_MASK_ROTATION)) == 0) {
                if (shape instanceof RectangularShape) {
                    RectangularShape rect = (RectangularShape) shape;
                    if (!allowOverwrite) {
                        rect = (RectangularShape) rect.clone();
                    }
                    final Rectangle2D frame = rect.getFrame();
                    rect.setFrame(transform(transform, frame, frame));
                    return rect;
                }
            }
        }
        if (shape instanceof Path2D) {
            final Path2D path = (Path2D) shape;
            if (allowOverwrite) {
                path.transform(transform);
            } else {
                shape = path.createTransformedShape(transform);
            }
        } else if (shape instanceof Area) {
            final Area area = (Area) shape;
            if (allowOverwrite) {
                area.transform(transform);
            } else {
                shape = area.createTransformedArea(transform);
            }
        } else {
            shape = new Path2D.Double(shape, transform);
        }
        return shape;
    }

    /**
     * Calculates a rectangle which entirely contains the direct transform of {@code bounds}.
     * This operation is equivalent to the following code, except that it can reuse the
     * given {@code dest} rectangle and is potentially more efficient:
     *
     * {@preformat java
     *     return transform.createTransformedShape(bounds).getBounds2D();
     * }
     *
     * Note that if the given rectangle is an image bounds, then the given transform shall map the
     * <strong>upper-left corner</strong> of pixels (as in Java2D usage), not the center of pixels
     * (OGC usage).
     *
     * @param transform The affine transform to use.
     * @param bounds    The rectangle to transform, or {@code null}.
     *                  This rectangle will not be modified except if {@code dest} references the same object.
     * @param dest      Rectangle in which to place the result. If {@code null}, a new rectangle will be created.
     *
     * @return The direct transform of the {@code bounds} rectangle, or {@code null} if {@code bounds} was null.
     *
     * @see org.apache.sis.geometry.Envelopes#transform(MathTransform2D, Rectangle2D, Rectangle2D)
     */
    public static Rectangle2D transform(final AffineTransform transform,
            final Rectangle2D bounds, final Rectangle2D dest)
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (bounds == null) {
            return null;
        }
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        final Point2D.Double point = new Point2D.Double();
        for (int i=0; i<4; i++) {
            point.x = (i & 1) == 0 ? bounds.getMinX() : bounds.getMaxX();
            point.y = (i & 2) == 0 ? bounds.getMinY() : bounds.getMaxY();
            transform.transform(point, point);
            if (point.x < xmin) xmin = point.x;
            if (point.x > xmax) xmax = point.x;
            if (point.y < ymin) ymin = point.y;
            if (point.y > ymax) ymax = point.y;
        }
        if (dest != null) {
            dest.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
            return dest;
        }
        return new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Calculates a rectangle which entirely contains the inverse transform of {@code bounds}.
     * This operation is equivalent to the following code, except that it can reuse the
     * given {@code dest} rectangle and is potentially more efficient:
     *
     * {@preformat java
     *     return createInverse().createTransformedShape(bounds).getBounds2D();
     * }
     *
     * @param transform The affine transform to use.
     * @param bounds    The rectangle to transform, or {@code null}.
     *                  This rectangle will not be modified except if {@code dest} references the same object.
     * @param dest      Rectangle in which to place the result. If {@code null}, a new rectangle will be created.
     *
     * @return The inverse transform of the {@code bounds} rectangle, or {@code null} if {@code bounds} was null.
     * @throws NoninvertibleTransformException if the affine transform can't be inverted.
     */
    public static Rectangle2D inverseTransform(final AffineTransform transform,
            final Rectangle2D bounds, final Rectangle2D dest) throws NoninvertibleTransformException
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (bounds == null) {
            return null;
        }
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        final Point2D.Double point = new Point2D.Double();
        for (int i=0; i<4; i++) {
            point.x = (i & 1) == 0 ? bounds.getMinX() : bounds.getMaxX();
            point.y = (i & 2) == 0 ? bounds.getMinY() : bounds.getMaxY();
            transform.inverseTransform(point, point);
            if (point.x < xmin) xmin = point.x;
            if (point.x > xmax) xmax = point.x;
            if (point.y < ymin) ymin = point.y;
            if (point.y > ymax) ymax = point.y;
        }
        if (dest != null) {
            dest.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
            return dest;
        }
        return new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Calculates the inverse transform of a point without applying the translation components.
     * In other words, calculates the inverse transform of a displacement vector.
     *
     * @param transform The affine transform to use.
     * @param vector    The vector to transform stored as a point.
     *                  This point will not be modified except if {@code dest} references the same object.
     * @param dest      Point in which to place the result. If {@code null}, a new point will be created.
     *
     * @return The inverse transform of the {@code vector}, or {@code null} if {@code source} was null.
     * @throws NoninvertibleTransformException if the affine transform can't be inverted.
     */
    public static Point2D inverseDeltaTransform(final AffineTransform transform,
            final Point2D vector, final Point2D dest) throws NoninvertibleTransformException
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (vector == null) {
            return null;
        }
        final double m00 = transform.getScaleX();
        final double m11 = transform.getScaleY();
        final double m01 = transform.getShearX();
        final double m10 = transform.getShearY();
        final double det = m00*m11 - m01*m10;
        if (!(abs(det) > Double.MIN_VALUE)) {
            throw new NoninvertibleTransformException(null);
        }
        final double x0 = vector.getX();
        final double y0 = vector.getY();
        final double x = (x0*m11 - y0*m01) / det;
        final double y = (y0*m00 - x0*m10) / det;
        if (dest != null) {
            dest.setLocation(x, y);
            return dest;
        }
        return new Point2D.Double(x, y);
    }

    /**
     * Returns an estimation about whether the specified transform swaps <var>x</var> and <var>y</var> axes.
     * This method assumes that the specified affine transform is built from arbitrary translations, scales or
     * rotations, but no shear. It returns {@code +1} if the (<var>x</var>, <var>y</var>) axis order seems to be
     * preserved, {@code -1} if the transform seems to swap axis to the (<var>y</var>, <var>x</var>) axis order,
     * or {@code 0} if this method can not make a decision.
     *
     * @param  transform The affine transform to inspect.
     * @return {@code true} if the given transform seems to swap axis order.
     */
    public static int getSwapXY(final AffineTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        final int flip = getFlip(transform);
        if (flip != 0) {
            final double scaleX = getScaleX0(transform);
            final double scaleY = getScaleY0(transform) * flip;
            final double y = abs(transform.getShearY()/scaleY - transform.getShearX()/scaleX);
            final double x = abs(transform.getScaleY()/scaleY + transform.getScaleX()/scaleX);
            if (x > y) return +1;
            if (x < y) return -1;
            // At this point, we may have (x == y) or some NaN value.
        }
        return 0;
    }

    /**
     * Returns an estimation of the rotation angle in radians. This method assumes that the specified affine
     * transform is built from arbitrary translations, scales or rotations, but no shear. If a flip has been
     * applied, then this method assumes that the flipped axis is the <var>y</var> one in <cite>source CRS</cite>
     * space. For a <cite>grid to world CRS</cite> transform, this is the row number in grid coordinates.
     *
     * @param  transform The affine transform to inspect.
     * @return An estimation of the rotation angle in radians,
     *         or {@link Double#NaN NaN} if the angle can not be estimated.
     */
    public static double getRotation(final AffineTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        final int flip = getFlip(transform);
        if (flip != 0) {
            final double scaleX = getScaleX0(transform);
            final double scaleY = getScaleY0(transform) * flip;
            return atan2(transform.getShearY()/scaleY - transform.getShearX()/scaleX,
                         transform.getScaleY()/scaleY + transform.getScaleX()/scaleX);
        }
        return Double.NaN;
    }

    /**
     * Returns {@code -1} if one axis has been flipped, {@code +1} if no axis has been flipped, or 0 if unknown.
     * A flipped axis in an axis with direction reversed (typically the <var>y</var> axis). This method assumes
     * that the specified affine transform is built from arbitrary translations, scales or rotations, but no shear.
     * Note that it is not possible to determine which of the <var>x</var> or <var>y</var> axis has been flipped.
     *
     * <p>This method can be used in order to set the sign of a scale according the flipping state.
     * The example below choose to apply the sign on the <var>y</var> scale, but this is an arbitrary
     * (while common) choice:</p>
     *
     * {@preformat java
     *     double scaleX0 = getScaleX0(transform);
     *     double scaleY0 = getScaleY0(transform);
     *     int    flip    = getFlip(transform);
     *     if (flip != 0) {
     *         scaleY0 *= flip;
     *         // ... continue the process here.
     *     }
     * }
     *
     * This method is similar to the following code, except that this method distinguishes
     * between "unflipped" and "unknown" states.
     *
     * {@preformat java
     *     boolean flipped = (tr.getType() & TYPE_FLIP) != 0;
     * }
     *
     * @param transform The affine transform to inspect.
     * @return -1 if an axis has been flipped, +1 if no flipping, or 0 if unknown.
     */
    public static int getFlip(final AffineTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        final double scaleX = Math.signum(transform.getScaleX());
        final double scaleY = Math.signum(transform.getScaleY());
        final double shearX = Math.signum(transform.getShearX());
        final double shearY = Math.signum(transform.getShearY());
        if (scaleX ==  scaleY && shearX == -shearY) return +1;
        if (scaleX == -scaleY && shearX ==  shearY) return -1;
        return 0;
    }

    /**
     * Returns the magnitude of scale factor <var>x</var> by canceling the
     * effect of eventual flip and rotation. This factor is calculated by:
     *
     * <p><img src="doc-files/scaleX0.png" alt="Scale factor on x axis"></p>
     *
     * @param  transform The affine transform to inspect.
     * @return The magnitude of scale factor <var>x</var>.
     */
    public static double getScaleX0(final AffineTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        final double scale = transform.getScaleX();
        final double shear = transform.getShearX();
        if (shear == 0) return abs(scale);  // Optimization for a very common case.
        if (scale == 0) return abs(shear);  // Not as common as above, but still common enough.
        return hypot(scale, shear);
    }

    /**
     * Returns the magnitude of scale factor <var>y</var> by canceling the
     * effect of eventual flip and rotation. This factor is calculated by:
     *
     * <p><img src="doc-files/scaleY0.png" alt="Scale factor on y axis"></p>
     *
     * @param  transform The affine transform to inspect.
     * @return The magnitude of scale factor <var>y</var>.
     */
    public static double getScaleY0(final AffineTransform transform) {
        ArgumentChecks.ensureNonNull("transform", transform);
        final double scale = transform.getScaleY();
        final double shear = transform.getShearY();
        if (shear == 0) return abs(scale);  // Optimization for a very common case.
        if (scale == 0) return abs(shear);  // Not as common as above, but still common enough.
        return hypot(scale, shear);
    }
}
