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
package org.apache.sis.internal.referencing.j2d;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import org.apache.sis.util.Static;

import static java.lang.Math.*;


/**
 * Static methods operating on shapes from the {@link java.awt.geom} package.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class ShapeUtilities extends Static {
    /**
     * Threshold value for determining whether two points are the same, or whether two lines are colinear.
     */
    private static final double EPS = 1E-6;

    /**
     * Do not allow instantiation of this class.
     */
    private ShapeUtilities() {
    }

    /**
     * Returns the intersection point between two line segments. The lines do not continue to infinity;
     * if the intersection does not occur between the ending points {@linkplain Line2D#getP1() P1} and
     * {@linkplain Line2D#getP2() P2} of the two line segments, then this method returns {@code null}.
     *
     * @param  ax1 <var>x</var> value of the first point on the first  line.
     * @param  ay1 <var>y</var> value of the first point on the first  line.
     * @param  ax2 <var>x</var> value of the last  point on the first  line.
     * @param  ay2 <var>y</var> value of the last  point on the first  line.
     * @param  bx1 <var>x</var> value of the first point on the second line.
     * @param  by1 <var>y</var> value of the first point on the second line.
     * @param  bx2 <var>x</var> value of the last  point on the second line.
     * @param  by2 <var>y</var> value of the last  point on the second line.
     * @return The intersection point, or {@code null} if none.
     *
     * @todo This method is used by Geotk (a sandbox for code that may migrate to SIS), but not yet by SIS.
     *       We temporarily keep this code here, but may delete or move it elsewhere in a future SIS version
     *       depending whether we port to SIS the sandbox code.
     */
    public static Point2D.Double intersectionPoint(final double ax1, final double ay1, double ax2, double ay2,
                                                   final double bx1, final double by1, double bx2, double by2)
    {
        ax2 -= ax1;
        ay2 -= ay1;
        bx2 -= bx1;
        by2 -= by1;
        double x = ay2 * bx2;
        double y = ax2 * by2;
        /*
         * The above (x,y) coordinate is temporary. If and only if the two line are parallel, then x == y.
         * Following code computes the real (x,y) coordinates of the intersection point.
         */
        x = ((by1-ay1) * (ax2*bx2) + x*ax1 - y*bx1) / (x-y);
        y = abs(bx2) > abs(ax2) ?
                (by2 / bx2) * (x - bx1) + by1 :
                (ay2 / ax2) * (x - ax1) + ay1;
        /*
         * The '!=0' expressions below are important for avoiding rounding errors with
         * horizontal or vertical lines. The '!' are important for handling NaN values.
         */
        if (ax2 != 0 && !(ax2 < 0 ? (x <= ax1 && x >= ax1 + ax2) : (x >= ax1 && x <= ax1 + ax2))) return null;
        if (bx2 != 0 && !(bx2 < 0 ? (x <= bx1 && x >= bx1 + bx2) : (x >= bx1 && x <= bx1 + bx2))) return null;
        if (ay2 != 0 && !(ay2 < 0 ? (y <= ay1 && y >= ay1 + ay2) : (y >= ay1 && y <= ay1 + ay2))) return null;
        if (by2 != 0 && !(by2 < 0 ? (y <= by1 && y >= by1 + by2) : (y >= by1 && y <= by1 + by2))) return null;
        return new Point2D.Double(x,y);
    }

    /**
     * Returns the point on the given {@code line} segment which is closest to the given {@code point}.
     * Let {@code result} be the returned point. This method guarantees (except for rounding errors) that:
     *
     * <ul>
     *   <li>{@code result} is a point on the {@code line} segment. It is located between the
     *       {@linkplain Line2D#getP1() P1} and {@linkplain Line2D#getP2() P2} ending points
     *       of that line segment.</li>
     *   <li>The distance between the {@code result} point and the given {@code point} is
     *       the shortest distance among the set of points meeting the previous condition.
     *       This distance can be obtained with {@code point.distance(result)}.</li>
     * </ul>
     *
     * @param  x1 <var>x</var> value of the first point on the line.
     * @param  y1 <var>y</var> value of the first point on the line.
     * @param  x2 <var>x</var> value of the last  point on the line.
     * @param  y2 <var>y</var> value of the last  point on the line.
     * @param  x  <var>x</var> value of a point close to the given line.
     * @param  y  <var>y</var> value of a point close to the given line.
     * @return The nearest point on the given line.
     *
     * @see #colinearPoint(double,double , double,double , double,double , double)
     *
     * @todo This method is used by Geotk (a sandbox for code that may migrate to SIS), but not yet by SIS.
     *       We temporarily keep this code here, but may delete or move it elsewhere in a future SIS version
     *       depending whether we port to SIS the sandbox code.
     */
    public static Point2D.Double nearestColinearPoint(final double x1, final double y1,
                                                      final double x2, final double y2,
                                                            double x,        double y)
    {
        final double slope = (y2-y1) / (x2-x1);
        if (!Double.isInfinite(slope)) {
            final double y0 = (y2 - slope*x2);
            x = ((y - y0) * slope + x) / (slope*slope + 1);
            y = x*slope + y0;
        } else {
            x = x2;
        }
        if (x1 <= x2) {
            if (x < x1) x = x1;
            if (x > x2) x = x2;
        } else {
            if (x > x1) x = x1;
            if (x < x2) x = x2;
        }
        if (y1 <= y2) {
            if (y < y1) y = y1;
            if (y > y2) y = y2;
        } else {
            if (y > y1) y = y1;
            if (y < y2) y = y2;
        }
        return new Point2D.Double(x,y);
    }

    /**
     * Returns a point on the given {@code line} segment located at the given {@code distance}
     * from that line. Let {@code result} be the returned point. If {@code result} is not null,
     * then this method guarantees (except for rounding error) that:
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
     * If two result points meet those conditions, then this method returns the point
     * which is the closest to {@code line.getP1()}.
     *
     * @param  x1 <var>x</var> value of the first point on the line.
     * @param  y1 <var>y</var> value of the first point on the line.
     * @param  x2 <var>x</var> value of the last  point on the line.
     * @param  y2 <var>y</var> value of the last  point on the line.
     * @param  x  <var>x</var> value of a point close to the given line.
     * @param  y  <var>y</var> value of a point close to the given line.
     * @param  distance The distance between the given point and the point to be returned.
     * @return A point on the given line located at the given distance from the given point.
     *
     * @see #nearestColinearPoint(double,double , double,double , double,double)
     *
     * @todo This method is used by Geotk (a sandbox for code that may migrate to SIS), but not yet by SIS.
     *       We temporarily keep this code here, but may delete or move it elsewhere in a future SIS version
     *       depending whether we port to SIS the sandbox code.
     */
    public static Point2D.Double colinearPoint(double x1, double y1, double x2, double y2,
                                               double x, double y, double distance)
    {
        final double ox1 = x1;
        final double oy1 = y1;
        final double ox2 = x2;
        final double oy2 = y2;
        distance *= distance;
        if (x1 == x2) {
            double dy = x1 - x;
            dy = sqrt(distance - dy*dy);
            y1 = y - dy;
            y2 = y + dy;
        } else if (y1 == y2) {
            double dx = y1 - y;
            dx = sqrt(distance - dx*dx);
            x1 = x - dx;
            x2 = x + dx;
        } else {
            final double m  = (y1-y2) / (x2-x1);
            final double y0 = (y2-y) + m*(x2-x);
            final double B  = m * y0;
            final double A  = m*m + 1;
            final double C  = sqrt(B*B + A*(distance - y0*y0));
            x1 = (B+C) / A;
            x2 = (B-C) / A;
            y1 = y + y0 - m*x1;
            y2 = y + y0 - m*x2;
            x1 += x;
            x2 += x;
        }
        boolean in1, in2;
        if (oy1 > oy2) {
            in1 = (y1 <= oy1 && y1 >= oy2);
            in2 = (y2 <= oy1 && y2 >= oy2);
        } else {
            in1 = (y1 >= oy1 && y1 <= oy2);
            in2 = (y2 >= oy1 && y2 <= oy2);
        }
        if (ox1 > ox2) {
            in1 &= (x1 <= ox1 && x1 >= ox2);
            in2 &= (x2 <= ox1 && x2 >= ox2);
        } else {
            in1 &= (x1 >= ox1 && x1 <= ox2);
            in2 &= (x2 >= ox1 && x2 <= ox2);
        }
        if (!in1 && !in2) return null;
        if (!in1) return new Point2D.Double(x2,y2);
        if (!in2) return new Point2D.Double(x1,y1);
        x = x1 - ox1;
        y = y1 - oy1;
        final double d1 = x*x + y*y;
        x = x2 - ox1;
        y = y2 - oy1;
        final double d2 = x*x + y*y;
        if (d1 > d2) return new Point2D.Double(x2,y2);
        else         return new Point2D.Double(x1,y1);
    }

    /**
     * Returns a quadratic curve passing by the 3 given points. There is an infinity of quadratic curves passing by
     * 3 points. We can express the curve we are looking for as a parabolic equation of the form {@code y=ax²+bx+c}
     * but where the <var>x</var> axis is not necessarily horizontal. The orientation of the <var>x</var> axis in
     * the above equation is determined by the {@code horizontal} parameter:
     *
     * <ul>
     *   <li>A value of {@code true} means that the <var>x</var> axis must be horizontal. The quadratic curve
     *       will then look like an ordinary parabolic curve as we see in mathematic school book.</li>
     *   <li>A value of {@code false} means that the <var>x</var> axis must be parallel to the
     *       line segment joining the {@code P0} and {@code P2} ending points.</li>
     * </ul>
     *
     * Note that if {@code P0.y == P2.y}, then both {@code horizontal} values produce the same result.
     *
     * @param  x1 <var>x</var> value of the starting point.
     * @param  y1 <var>y</var> value of the starting point.
     * @param  px <var>x</var> value of a passing point.
     * @param  py <var>y</var> value of a passing point.
     * @param  x2 <var>x</var> value of the ending point.
     * @param  y2 <var>y</var> value of the ending point.
     * @param  horizontal If {@code true}, the <var>x</var> axis is considered horizontal while computing the
     *         {@code y=ax²+bx+c} equation terms. If {@code false}, it is considered parallel to the line
     *         joining the {@code P0} and {@code P2} points.
     * @return A quadratic curve passing by the given points. The curve starts at {@code P0} and ends at {@code P2}.
     *         If two points are too close or if the three points are colinear, then this method returns {@code null}.
     *
     * @todo This method is used by Geotk (a sandbox for code that may migrate to SIS), but not yet by SIS.
     *       We temporarily keep this code here, but may delete or move it elsewhere in a future SIS version
     *       depending whether we port to SIS the sandbox code.
     */
    public static QuadCurve2D.Double fitParabol(final double x1, final double y1,
                                                final double px, final double py,
                                                final double x2, final double y2,
                                                final boolean horizontal)
    {
        final Point2D.Double p = parabolicControlPoint(x1, y1, px, py, x2, y2, horizontal);
        return (p != null) ? new QuadCurve2D.Double(x1, y1, p.x, p.y, x2, y2) : null;
    }

    /**
     * Returns the control point of a quadratic curve passing by the 3 given points. There is an infinity of quadratic
     * curves passing by 3 points. We can express the curve we are looking for as a parabolic equation of the form
     * {@code y = ax²+bx+c}, but the <var>x</var> axis is not necessarily horizontal. The <var>x</var> axis orientation
     * in the above equation is determined by the {@code horizontal} parameter:
     *
     * <ul>
     *   <li>A value of {@code true} means that the <var>x</var> axis must be horizontal.
     *       The quadratic curve will then look like an ordinary parabolic curve as we see
     *       in mathematic school book.</li>
     *   <li>A value of {@code false} means that the <var>x</var> axis must be parallel to the
     *       line segment joining the {@code P0} and {@code P2} ending points.</li>
     * </ul>
     *
     * Note that if {@code P0.y == P2.y}, then both {@code horizontal} values produce the same result.
     *
     * @param  x1 <var>x</var> value of the starting point.
     * @param  y1 <var>y</var> value of the starting point.
     * @param  px <var>x</var> value of a passing point.
     * @param  py <var>y</var> value of a passing point.
     * @param  x2 <var>x</var> value of the ending point.
     * @param  y2 <var>y</var> value of the ending point.
     * @param  horizontal If {@code true}, the <var>x</var> axis is considered horizontal while computing the
     *         {@code y = ax²+bx+c} equation terms. If {@code false}, it is considered parallel to the line
     *         joining the {@code P0} and {@code P2} points.
     * @return The control point of a quadratic curve passing by the given points. The curve starts at {@code (x0,y0)}
     *         and ends at {@code (x2,y2)}. If two points are too close or if the three points are colinear, then this
     *         method returns {@code null}.
     */
    public static Point2D.Double parabolicControlPoint(final double x1, final double y1,
            double px, double py, double x2, double y2, final boolean horizontal)
    {
        /*
         * Apply a translation in such a way that (x0,y0) become the coordinate system origin.
         * After this translation, we shall not use (x0,y0) until we are done.
         */
        px -= x1;
        py -= y1;
        x2 -= x1;
        y2 -= y1;
        if (horizontal) {
            final double a = (y2 - py*x2/px) / (x2-px); // Actually "a*x2"
            final double check = abs(a);
            if (!(check <= 1/EPS)) return null; // Two points have the same coordinates.
            if (!(check >=   EPS)) return null; // The three points are co-linear.
            final double b = y2/x2 - a;
            px = (1 + b/(2*a))*x2 - y2/(2*a);
            py = y1 + b*px;
            px += x1;
        } else {
            /*
             * Apply a rotation in such a way that (x2,y2)
             * lies on the x axis, i.e. y2 = 0.
             */
            final double rx2 = x2;
            final double ry2 = y2;
            x2 = hypot(x2,y2);
            y2 = (px*rx2 + py*ry2) / x2; // use 'y2' as a temporary variable for 'x1'
            py = (py*rx2 - px*ry2) / x2;
            px = y2;
            y2 = 0; // set as a matter of principle (but not used).
            /*
             * Now compute the control point coordinates in our new coordinate system axis.
             */
            final double x = 0.5;                       // Actually "x/x2"
            final double y = (py*x*x2) / (px*(x2-px));  // Actually "y/y2"
            final double check = abs(y);
            if (!(check <= 1/EPS)) return null; // Two points have the same coordinates.
            if (!(check >=   EPS)) return null; // The three points are co-linear.
            /*
             * Applies the inverse rotation then a translation to bring
             * us back to the original coordinate system.
             */
            px = (x*rx2 - y*ry2) + x1;
            py = (y*rx2 + x*ry2) + y1;
        }
        return new Point2D.Double(px,py);
    }

    /**
     * Returns a circle passing by the 3 given points. The distance between the returned
     * point and any of the given points will be constant; it is the circle radius.
     *
     * @param  x1 <var>x</var> value of the first  point.
     * @param  y1 <var>y</var> value of the first  point.
     * @param  x2 <var>x</var> value of the second point.
     * @param  y2 <var>y</var> value of the second point.
     * @param  x3 <var>x</var> value of the third  point.
     * @param  y3 <var>y</var> value of the third  point.
     * @return A circle passing by the given points.
     *
     * @todo This method is used by Geotk (a sandbox for code that may migrate to SIS), but not yet by SIS.
     *       We temporarily keep this code here, but may delete or move it elsewhere in a future SIS version
     *       depending whether we port to SIS the sandbox code.
     */
    public static Point2D.Double circleCentre(double x1, double y1,
                                              double x2, double y2,
                                              double x3, double y3)
    {
        x2 -= x1;
        x3 -= x1;
        y2 -= y1;
        y3 -= y1;
        final double sq2 = (x2*x2 + y2*y2);
        final double sq3 = (x3*x3 + y3*y3);
        final double x   = (y2*sq3 - y3*sq2) / (y2*x3 - y3*x2);
        return new Point2D.Double(x1 + 0.5*x, y1 + 0.5*(sq2 - x*x2)/y2);
    }

    /**
     * Attempts to replace an arbitrary shape by one of the standard Java2D constructs.
     * For example if the given {@code path} is a {@link Path2D} containing only a single
     * line or a quadratic curve, then this method replaces it by a {@link Line2D} or
     * {@link QuadCurve2D} object respectively.
     *
     * @param  path The shape to replace by a simpler Java2D construct.
     *         This is generally an instance of {@link Path2D}, but not necessarily.
     * @return A simpler Java construct, or {@code path} if no better construct is proposed.
     */
    public static Shape toPrimitive(final Shape path) {
        final PathIterator it = path.getPathIterator(null);
        if (!it.isDone()) {
            final double[] buffer = new double[6];
            if (it.currentSegment(buffer) == PathIterator.SEG_MOVETO) {
                it.next();
                if (!it.isDone()) {
                    final double x1 = buffer[0];
                    final double y1 = buffer[1];
                    final int code = it.currentSegment(buffer);
                    it.next();
                    if (it.isDone()) {
                        switch (code) {
                            case PathIterator.SEG_LINETO:  return new       Line2D.Double(x1,y1, buffer[0], buffer[1]);
                            case PathIterator.SEG_QUADTO:  return new  QuadCurve2D.Double(x1,y1, buffer[0], buffer[1], buffer[2], buffer[3]);
                            case PathIterator.SEG_CUBICTO: return new CubicCurve2D.Double(x1,y1, buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5]);
                        }
                    }
                }
            }
        }
        return path;
    }
}
