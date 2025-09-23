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
package org.apache.sis.geometry;

import java.util.Set;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.ShapeUtilities;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;


/**
 * Utility methods working on Java2D shapes.
 * The {@code transform(…)} methods in this class work in the same way as the methods of the same signature
 * in {@link Envelopes}, except that they work on {@link Rectangle2D} objects instead of {@code Envelope}.
 * In particular, the same treatment for curvatures and poles is applied.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.3
 * @since   0.8
 */
public final class Shapes2D extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Shapes2D() {
    }

    /**
     * Returns the intersection point between two line segments. The lines do not continue to infinity;
     * if the intersection does not occur between the ending points {@linkplain Line2D#getP1() P1} and
     * {@linkplain Line2D#getP2() P2} of the two line segments, then this method returns {@code null}.
     *
     * @param  a  the first line segment.
     * @param  b  the second line segment.
     * @return the intersection point, or {@code null} if none.
     */
    public static Point2D intersectionPoint(final Line2D a, final Line2D b) {
        return ShapeUtilities.intersectionPoint(a.getX1(), a.getY1(), a.getX2(), a.getY2(),
                                                b.getX1(), b.getY1(), b.getX2(), b.getY2());
    }

    /**
     * Returns the point on the given {@code line} segment which is closest to the given {@code point}.
     * Let {@code result} be the returned point. This method guarantees (except for rounding errors) that:
     *
     * <ul>
     *   <li>{@code result} is a point on the {@code line} segment. It is located between
     *       the {@linkplain Line2D#getP1() P1} and {@linkplain Line2D#getP2() P2} ending
     *       points of that line segment.</li>
     *   <li>The distance between the {@code result} point and the given {@code point} is
     *       the shortest distance among the set of points meeting the previous condition.
     *       This distance can be obtained with {@code point.distance(result)}.</li>
     * </ul>
     *
     * @param  segment  the line on which to search for a point.
     * @param  point    a point close to the given line.
     * @return the nearest point on the given line.
     *
     * @see #colinearPoint(Line2D, Point2D, double)
     */
    public static Point2D nearestColinearPoint(final Line2D segment, final Point2D point) {
        return ShapeUtilities.nearestColinearPoint(segment.getX1(), segment.getY1(),
                                                   segment.getX2(), segment.getY2(),
                                                     point.getX(),    point.getY());
    }

    /**
     * Returns a point on the given {@code line} segment located at the given {@code distance} from that line.
     * Let {@code result} be the returned point. If {@code result} is not null, then this method guarantees
     * (except for rounding error) that:
     *
     * <ul>
     *   <li>{@code result} is a point on the {@code line} segment. It is located between
     *       the {@linkplain Line2D#getP1() P1} and {@linkplain Line2D#getP2() P2} ending
     *       points of that line segment.</li>
     *   <li>The distance between the {@code result} and the given {@code point} is exactly
     *       equal to {@code distance}.</li>
     * </ul>
     *
     * If no result point meets those conditions, then this method returns {@code null}.
     * If two result points met those conditions, then this method returns the point
     * which is the closest to {@code line.getP1()}.
     *
     * @param  line      the line on which to search for a point.
     * @param  point     a point close to the given line.
     * @param  distance  the distance between the given point and the point to be returned.
     * @return a point on the given line located at the given distance from the given point.
     *
     * @see #nearestColinearPoint(Line2D, Point2D)
     */
    public static Point2D colinearPoint(Line2D line, Point2D point, double distance) {
        return ShapeUtilities.colinearPoint(line.getX1(), line.getY1(),
                                            line.getX2(), line.getY2(),
                                            point.getX(), point.getY(),
                                            distance);
    }

    /**
     * Returns a circle passing by the 3 given points.
     *
     * @param  P1  the first point.
     * @param  P2  the second point.
     * @param  P3  the third point.
     * @return a circle passing by the given points.
     */
    public static Ellipse2D circle(final Point2D P1, final Point2D P2, final Point2D P3) {
        final Point2D.Double center = ShapeUtilities.circleCentre(P1.getX(), P1.getY(),
                                                                  P2.getX(), P2.getY(),
                                                                  P3.getX(), P3.getY());
        final double radius = center.distance(P2);
        return new Ellipse2D.Double(center.x - radius,
                                    center.y - radius,
                                    2*radius, 2*radius);
    }

    /**
     * Sets the given shape to the intersection of that shape with the given envelope.
     *
     * @param  shape     the shape to intersect. Will be modified in-place.
     * @param  envelope  the envelope to intersect with the given shape.
     * @param  xdim      dimension of <var>x</var> coordinates in the envelope. This is usually 0.
     * @param  ydim      dimension of <var>y</var> coordinates in the envelope. This is usually 1.
     *
     * @since 1.3
     */
    public static void intersect(final RectangularShape shape, final Envelope envelope, final int xdim, final int ydim) {
        final double xmin = Math.max(shape.getMinX(), envelope.getMinimum(xdim));
        final double ymin = Math.max(shape.getMinY(), envelope.getMinimum(ydim));
        final double xmax = Math.min(shape.getMaxX(), envelope.getMaximum(xdim));
        final double ymax = Math.min(shape.getMaxY(), envelope.getMaximum(ydim));
        shape.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Transforms a rectangular envelope using the given math transform.
     * The transformation is only approximated: the returned envelope may be bigger than
     * necessary, or smaller than required if the bounding box contains a pole.
     *
     * <p>Note that this method cannot handle the case where the rectangle contains the North or South pole,
     * or when it cross the ±180° longitude, because {@code MathTransform} does not carry sufficient information.
     * For a more robust rectangle transformation, use {@link #transform(CoordinateOperation, Rectangle2D, Rectangle2D)}
     * instead.</p>
     *
     * @param  transform    the transform to use. Source and target dimension must be 2.
     * @param  envelope     the rectangle to transform (may be {@code null}).
     * @param  destination  the destination rectangle (may be {@code envelope}).
     *         If {@code null}, a new rectangle will be created and returned.
     * @return {@code destination}, or a new rectangle if {@code destination} was null and {@code envelope} was non-null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(CoordinateOperation, Rectangle2D, Rectangle2D)
     * @see Envelopes#transform(MathTransform, Envelope)
     */
    public static Rectangle2D transform(final MathTransform2D transform,
                                        final Rectangle2D     envelope,
                                              Rectangle2D     destination)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (transform instanceof AffineTransform) {
            // Common case implemented in a more efficient way (less points to transform).
            return AffineTransforms2D.transform((AffineTransform) transform, envelope, destination);
        }
        return transform(transform, envelope, destination, new double[2]);
    }

    /**
     * Implementation of {@link #transform(MathTransform2D, Rectangle2D, Rectangle2D)} with the
     * opportunity to save the projected center coordinate. This method sets {@code point} to
     * the center of the source envelope projected to the target CRS.
     */
    @SuppressWarnings("fallthrough")
    private static Rectangle2D transform(final MathTransform2D transform,
                                         final Rectangle2D     envelope,
                                               Rectangle2D     destination,
                                         final double[]        point)
            throws TransformException
    {
        if (envelope == null) {
            return null;
        }
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        final WraparoundInEnvelope.Controller wc = new WraparoundInEnvelope.Controller(transform);
        do {
            /*
             * Notation (as if we were applying a map projection, but this is not necessarily the case):
             *   - (λ,φ) are coordinate values before projection.
             *   - (x,y) are coordinate values after projection.
             *   - D[00|01|10|11] are the ∂x/∂λ, ∂x/∂φ, ∂y/∂λ and ∂y/∂φ derivatives respectively.
             *   - Variables with indice 0 are for the very first point in iteration order.
             *   - Variables with indice 1 are for the values of the previous iteration.
             *   - Variables with indice 2 are for the current values in the iteration.
             *   - P1-P2 form a line segment to be checked for curvature.
             */
            double x0=0, y0=0, λ0=0, φ0=0;
            double x1=0, y1=0, λ1=0, φ1=0;
            Matrix D0=null, D1=null, D2=null;
            // x2 and y2 defined inside the loop.
            boolean isDerivativeSupported = true;
            final CurveExtremum extremum = new CurveExtremum();
            for (int i=0; i<=8; i++) {
                /*
                 * Iteration order (center must be last):
                 *
                 *   (6)────(5)────(4)
                 *    |             |
                 *   (7)    (8)    (3)
                 *    |             |
                 *   (0)────(1)────(2)
                 */
                double λ2, φ2;
                switch (i) {
                    case 0: case 6: case 7: λ2 = envelope.getMinX();    break;
                    case 1: case 5: case 8: λ2 = envelope.getCenterX(); break;
                    case 2: case 3: case 4: λ2 = envelope.getMaxX();    break;
                    default: throw new AssertionError(i);
                }
                switch (i) {
                    case 0: case 1: case 2: φ2 = envelope.getMinY();    break;
                    case 3: case 7: case 8: φ2 = envelope.getCenterY(); break;
                    case 4: case 5: case 6: φ2 = envelope.getMaxY();    break;
                    default: throw new AssertionError(i);
                }
                point[0] = λ2;
                point[1] = φ2;
                try {
                    D1 = D2;
                    D2 = Envelopes.derivativeAndTransform(wc.transform, point, point, 0, isDerivativeSupported && i != 8);
                } catch (TransformException e) {
                    if (!isDerivativeSupported) {
                        throw e;                        // Derivative were already disabled, so something went wrong.
                    }
                    isDerivativeSupported = false; D2 = null;
                    point[0] = λ2;
                    point[1] = φ2;
                    wc.transform.transform(point, 0, point, 0, 1);
                    Envelopes.recoverableException(Shapes2D.class, e);  // Log only if the above call was successful.
                }
                double x2 = point[0];
                double y2 = point[1];
                if (x2 < xmin) xmin = x2;
                if (x2 > xmax) xmax = x2;
                if (y2 < ymin) ymin = y2;
                if (y2 > ymax) ymax = y2;
                switch (i) {
                    case 0: {                           // Remember the first point.
                        λ0=λ2; x0=x2;
                        φ0=φ2; y0=y2;
                        D0=D2;
                        break;
                    }
                    case 8: {                           // Close the iteration with the first point.
                        λ2=λ0; x2=x0;                   // Discard P2 because it is the rectangle center.
                        φ2=φ0; y2=y0;
                        D2=D0;
                        break;
                    }
                }
                /*
                 * At this point, we expanded the rectangle using the projected points. Now try
                 * to use the information provided by derivatives at those points, if available.
                 * For the following block, notation is:
                 *
                 *   - s  are coordinate values in the source space (λ or φ)
                 *   - t  are coordinate values in the target space (x or y)
                 *
                 * They are not necessarily in the same dimension. For example, would could have
                 * s=λ while t=y. This is typically the case when inspecting the top or bottom
                 * line segment of the rectangle.
                 *
                 * The same technic is also applied in the transform(MathTransform, Envelope) method.
                 * The general method is more "elegant", at the cost of more storage requirement.
                 */
                if (D1 != null && D2 != null) {
                    final int srcDim;
                    final double s1, s2;                // Coordinate values in source space (before projection)
                    switch (i) {
                        case 1: case 2: case 5: case 6: {assert φ2==φ1; srcDim=0; s1=λ1; s2=λ2; break;}     // Horizontal segment
                        case 3: case 4: case 7: case 8: {assert λ2==λ1; srcDim=1; s1=φ1; s2=φ2; break;}     // Vertical segment
                        default: throw new AssertionError(i);
                    }
                    final double min, max;
                    if (s1 < s2) {min=s1; max=s2;}
                    else         {min=s2; max=s1;}
                    int tgtDim = 0;
                    do { // Executed exactly twice, for dimensions 0 and 1 in the projected space.
                        extremum.resolve(s1, (tgtDim == 0) ? x1 : y1, D1.getElement(tgtDim, srcDim),
                                         s2, (tgtDim == 0) ? x2 : y2, D2.getElement(tgtDim, srcDim));
                        /*
                         * At this point we found the extremum of the projected line segment
                         * using a cubic curve t = A + Bs + Cs² + Ds³ approximation.  Before
                         * to add those extremum into the projected bounding box, we need to
                         * ensure that the source coordinate is inside the original
                         * (unprojected) bounding box.
                         */
                        boolean isP2 = false;
                        do { // Executed exactly twice, one for each point.
                            final double se = isP2 ? extremum.ex2 : extremum.ex1;
                            if (se > min && se < max) {
                                final double te = isP2 ? extremum.ey2 : extremum.ey1;
                                if ((tgtDim == 0) ? (te < xmin || te > xmax) : (te < ymin || te > ymax)) {
                                    /*
                                     * At this point, we have determined that adding the extremum point
                                     * to the rectangle would have expanded it. However, we will not add
                                     * that point directly, because maybe its position is not quite right
                                     * (since we used a cubic curve approximation). Instead, we project
                                     * the point on the rectangle border which is located vis-à-vis the
                                     * extremum. Our tests show that the correction can be as much as 50
                                     * metres.
                                     */
                                    final double oldX = point[0];
                                    final double oldY = point[1];
                                    if (srcDim == 0) {
                                        point[0] = se;
                                        point[1] = φ1; // == φ2 since we have an horizontal segment.
                                    } else {
                                        point[0] = λ1; // == λ2 since we have a vertical segment.
                                        point[1] = se;
                                    }
                                    wc.transform.transform(point, 0, point, 0, 1);
                                    final double x = point[0];
                                    final double y = point[1];
                                    if (x < xmin) xmin = x;
                                    if (x > xmax) xmax = x;
                                    if (y < ymin) ymin = y;
                                    if (y > ymax) ymax = y;
                                    point[0] = oldX;
                                    point[1] = oldY;
                                }
                            }
                        } while ((isP2 = !isP2) == true);
                    } while (++tgtDim == 1);
                }
                λ1=λ2; x1=x2;
                φ1=φ2; y1=y2;
                D1=D2;
            }
        } while (wc.translate());
        if (destination != null) {
            destination.setRect(xmin, ymin, xmax - xmin, ymax - ymin);
        } else {
            destination = new IntervalRectangle(xmin, ymin, xmax, ymax);
        }
        /*
         * Note: a previous version had an "assert" statement here comparing our calculation
         * with the calculation performed by the more general method working on Envelope. We
         * verified that the same values (coordinate tuples and derivatives) were ultimately
         * passed to the CurveExtremum.resolve(…) method, so we would expect the same result.
         * However, the iteration order is different. The result seems insensitive to iteration
         * order most of the time, but not always. However, it seems that the cases were the
         * results are different are the cases where the methods working with CoordinateOperation
         * object wipe out that difference anyway.
         */
        return destination;
    }

    /**
     * Transforms a rectangular envelope using the given coordinate operation.
     * The transformation is only approximated: the returned envelope may be bigger
     * than the smallest possible bounding box, but should not be smaller in most cases.
     *
     * <p>This method can handle the case where the rectangle contains the North or South pole,
     * or when it cross the ±180° longitude.</p>
     *
     * @param  operation    the operation to use. Source and target dimension must be 2.
     * @param  envelope     the rectangle to transform (may be {@code null}).
     * @param  destination  the destination rectangle (may be {@code envelope}).
     *         If {@code null}, a new rectangle will be created and returned.
     * @return {@code destination}, or a new rectangle if {@code destination} was null and {@code envelope} was non-null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(MathTransform2D, Rectangle2D, Rectangle2D)
     * @see Envelopes#transform(CoordinateOperation, Envelope)
     */
    public static Rectangle2D transform(final CoordinateOperation operation,
                                        final Rectangle2D         envelope,
                                              Rectangle2D         destination)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("operation", operation);
        if (envelope == null) {
            return null;
        }
        final MathTransform2D mt = MathTransforms.bidimensional(operation.getMathTransform());
        final double[] center = new double[2];
        destination = transform(mt, envelope, destination, center);
        /*
         * If the source envelope crosses the expected range of valid coordinates, also projects
         * the range bounds as a safety. See the comments in transform(Envelope, ...).
         */
        final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
        if (sourceCRS != null) {
            final CoordinateSystem cs = sourceCRS.getCoordinateSystem();
            if (cs != null && cs.getDimension() == 2) {                         // Paranoiac check.
                CoordinateSystemAxis axis = cs.getAxis(0);
                double min = envelope.getMinX();
                double max = envelope.getMaxX();
                Point2D.Double pt = null;
                for (int i=0; i<4; i++) {
                    if (i == 2) {
                        axis = cs.getAxis(1);
                        min = envelope.getMinY();
                        max = envelope.getMaxY();
                    }
                    final double v = (i & 1) == 0 ? axis.getMinimumValue() : axis.getMaximumValue();
                    if (!(v > min && v < max)) {
                        continue;
                    }
                    if (pt == null) {
                        pt = new Point2D.Double();
                    }
                    if ((i & 2) == 0) {
                        pt.x = v;
                        pt.y = envelope.getCenterY();
                    } else {
                        pt.x = envelope.getCenterX();
                        pt.y = v;
                    }
                    destination.add(mt.transform(pt, pt));
                }
            }
        }
        /*
         * Now take the target CRS in account.
         */
        final CoordinateReferenceSystem targetCRS = operation.getTargetCRS();
        if (targetCRS == null) {
            return destination;
        }
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        if (targetCS == null || targetCS.getDimension() != 2) {
            // It should be an error, but we keep this method tolerant.
            return destination;
        }
        /*
         * Checks for singularity points. See the Envelopes.transform(CoordinateOperation, Envelope)
         * method for comments about the algorithm. The code below is the same algorithm adapted for
         * the 2D case and the related objects (Point2D, Rectangle2D, etc.).
         *
         * The `border` variable in the loop below is used in order to compress 2 dimensions
         * and 2 extremums in a single loop, in this order: (xmin, xmax, ymin, ymax).
         */
        MathTransform2D    inverse  = null;
        TransformException warning  = null;
        Point2D            sourcePt = null;
        Point2D            targetPt = null;
        Point2D            revertPt = null;
        int includedBoundsValue = 0;                        // A bitmask for each (dimension, extremum) pairs.
        for (int border=0; border<4; border++) {            // 2 dimensions and 2 extremums compacted in a flag.
            final int dimension = border >>> 1;             // The dimension index being examined.
            final CoordinateSystemAxis axis = targetCS.getAxis(dimension);
            if (axis == null) {                             // Should never be null, but check as a paranoiac safety.
                continue;
            }
            final double extremum = (border & 1) == 0 ? axis.getMinimumValue() : axis.getMaximumValue();
            if (!Double.isFinite(extremum)) {
                continue;
            }
            if (inverse == null) {
                try {
                    inverse = mt.inverse();
                } catch (NoninvertibleTransformException exception) {
                    Envelopes.recoverableException(Shapes2D.class, exception);
                    return destination;
                }
                targetPt = new Point2D.Double();
            }
            switch (dimension) {
                case 0: targetPt.setLocation(extremum,  center[1]); break;
                case 1: targetPt.setLocation(center[0], extremum ); break;
                default: throw new AssertionError(border);
            }
            try {
                sourcePt = inverse.transform(targetPt, sourcePt);
                if (CoordinateOperations.isWrapAround(axis)) {
                    revertPt = mt.transform(sourcePt, revertPt);
                    final double delta = Math.abs((dimension == 0 ? revertPt.getX() : revertPt.getY()) - extremum);
                    if (!(delta < Envelopes.SPAN_FRACTION_AS_BOUND * (axis.getMaximumValue() - axis.getMinimumValue()))) {
                        continue;
                    }
                }
                if (envelope.contains(sourcePt)) {
                    destination.add(targetPt);
                    includedBoundsValue |= (1 << border);
                }
            } catch (TransformException exception) {
                if (warning == null) {
                    warning = exception;
                } else {
                    warning.addSuppressed(exception);
                }
            }
        }
        /*
         * Iterate over all dimensions of type "WRAPAROUND" for which minimal or maximal axis
         * values have not yet been included in the envelope. We could inline this check inside
         * the above loop, but we don't in order to have a chance to exclude the dimensions for
         * which the point have already been added.
         *
         * See transform(CoordinateOperation, Envelope) for more comments about the algorithm.
         */
        if (includedBoundsValue != 0) {
            /*
             * Bits mask transformation:
             *   1) Swaps the two dimensions               (YyXx  →  XxYy)
             *   2) Insert a space between each bits       (XxYy  →  X.x.Y.y.)
             *   3) Fill the space with duplicated values  (X.x.Y.y.  →  XXxxYYyy)
             *
             * In terms of bit positions 1,2,4,8 (not bit values), we have:
             *
             *   8421  →  22881144
             *   i.e. (ymax, ymin, xmax, xmin)  →  (xmax², ymax², xmin², ymin²)
             *
             * Now look at the last part: (xmin², ymin²). The next step is to perform a bitwise
             * AND operation in order to have only both of the following conditions:
             *
             *   Borders not yet added to the envelope: ~(ymax, ymin, xmax, xmin)
             *   Borders in which a singularity exists:  (xmin, xmin, ymin, ymin)
             *
             * The same operation is repeated on the next 4 bits for (xmax, xmax, ymax, ymax).
             */
            int toTest = ((includedBoundsValue & 1) << 3) | ((includedBoundsValue & 4) >>> 1) |
                         ((includedBoundsValue & 2) << 6) | ((includedBoundsValue & 8) << 2);
            toTest |= (toTest >>> 1); // Duplicate the bit values.
            toTest &= ~(includedBoundsValue | (includedBoundsValue << 4));
            /*
             * Forget any axes that are not of kind "WRAPAROUND". Then get the final
             * bit pattern indicating which points to test. Iterate over that bits.
             */
            if ((toTest & 0x33333333) != 0 && !CoordinateOperations.isWrapAround(targetCS.getAxis(0))) toTest &= 0xCCCCCCCC;
            if ((toTest & 0xCCCCCCCC) != 0 && !CoordinateOperations.isWrapAround(targetCS.getAxis(1))) toTest &= 0x33333333;
            while (toTest != 0) {
                final int border = Integer.numberOfTrailingZeros(toTest);
                final int bitMask = 1 << border;
                toTest &= ~bitMask;                                 // Clear now the bit, for the next iteration.
                final int dimensionToAdd = (border >>> 1) & 1;
                final CoordinateSystemAxis toAdd = targetCS.getAxis(dimensionToAdd);
                final CoordinateSystemAxis added = targetCS.getAxis(dimensionToAdd ^ 1);
                final double x = (border & 1) == 0 ? toAdd.getMinimumValue() : toAdd.getMaximumValue();
                final double y = (border & 4) == 0 ? added.getMinimumValue() : added.getMaximumValue();
                if (dimensionToAdd == 0) {
                    targetPt.setLocation(x, y);
                } else {
                    targetPt.setLocation(y, x);
                }
                try {
                    sourcePt = inverse.transform(targetPt, sourcePt);
                    if (envelope.contains(sourcePt)) {
                        destination.add(targetPt);
                    }
                } catch (TransformException exception) {
                    if (warning == null) {
                        warning = exception;
                    } else {
                        warning.addSuppressed(exception);
                    }
                }
            }
        }
        /*
         * At this point we finished envelope transformation. Verify if some coordinates need to be "wrapped around"
         * as a result of the coordinate operation. This is usually the longitude axis where the source CRS uses
         * the [-180 … +180]° range and the target CRS uses the [0 … 360]° range, or the converse. In such case we
         * set the rectangle to the full range (we do not use the mechanism documented in Envelope2D) because most
         * Rectangle2D implementations do not support crossing the anti-meridian. This results in larger rectangle
         * than what would be possible with GeneralEnvelope or Envelope2D, but we try to limit the situation where
         * this expansion is applied.
         */
        final Set<Integer> wrapAroundChanges;
        if (operation instanceof AbstractCoordinateOperation) {
            wrapAroundChanges = ((AbstractCoordinateOperation) operation).getWrapAroundChanges();
        } else {
            wrapAroundChanges = CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS);
        }
        for (int dim : wrapAroundChanges) {                               // Empty in the vast majority of cases.
            final CoordinateSystemAxis axis = targetCS.getAxis(dim);
            final double minimum = axis.getMinimumValue();
            final double maximum = axis.getMaximumValue();
            final double o1, o2;
            if (dim == 0) {
                o1 = destination.getMinX();
                o2 = destination.getMaxX();
            } else {
                o1 = destination.getMinY();
                o2 = destination.getMaxY();
            }
            if (o1 < minimum || o2 > maximum) {
                final double span = maximum - minimum;
                if (dim == 0) {
                    destination.setRect(minimum, destination.getY(), span, destination.getHeight());
                } else {
                    destination.setRect(destination.getX(), minimum, destination.getWidth(), span);
                }
            }
        }
        if (warning != null) {
            Envelopes.recoverableException(Shapes2D.class, warning);
        }
        return destination;
    }
}
