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
 * @since   0.5 (derived from geotk-1.1)
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
     * @param  x0 <var>x</var> value of the starting point.
     * @param  y0 <var>y</var> value of the starting point.
     * @param  x1 <var>x</var> value of a passing point.
     * @param  y1 <var>y</var> value of a passing point.
     * @param  x2 <var>x</var> value of the ending point.
     * @param  y2 <var>y</var> value of the ending point.
     * @param  horizontal If {@code true}, the <var>x</var> axis is considered horizontal while computing the
     *         {@code y = ax²+bx+c} equation terms. If {@code false}, it is considered parallel to the line
     *         joining the {@code P0} and {@code P2} points.
     * @return The control point of a quadratic curve passing by the given points. The curve starts at {@code (x0,y0)}
     *         and ends at {@code (x2,y2)}. If two points are too close or if the three points are colinear, then this
     *         method returns {@code null}.
     */
    public static Point2D.Double parabolicControlPoint(final double x0, final double y0,
            double x1, double y1, double x2, double y2, final boolean horizontal)
    {
        /*
         * Apply a translation in such a way that (x0,y0) become the coordinate system origin.
         * After this translation, we shall not use (x0,y0) until we are done.
         */
        x1 -= x0;
        y1 -= y0;
        x2 -= x0;
        y2 -= y0;
        if (horizontal) {
            final double a = (y2 - y1*x2/x1) / (x2-x1); // Actually "a*x2"
            final double check = abs(a);
            if (!(check <= 1/EPS)) return null; // Two points have the same coordinates.
            if (!(check >=   EPS)) return null; // The three points are co-linear.
            final double b = y2/x2 - a;
            x1 = (1 + b/(2*a))*x2 - y2/(2*a);
            y1 = y0 + b*x1;
            x1 += x0;
        } else {
            /*
             * Apply a rotation in such a way that (x2,y2)
             * lies on the x axis, i.e. y2 = 0.
             */
            final double rx2 = x2;
            final double ry2 = y2;
            x2 = hypot(x2,y2);
            y2 = (x1*rx2 + y1*ry2) / x2; // use 'y2' as a temporary variable for 'x1'
            y1 = (y1*rx2 - x1*ry2) / x2;
            x1 = y2;
            y2 = 0;
            /*
             * Now compute the control point coordinates in our new coordinate system axis.
             */
            final double x = 0.5;                       // Actually "x/x2"
            final double y = (y1*x*x2) / (x1*(x2-x1));  // Actually "y/y2"
            final double check = abs(y);
            if (!(check <= 1/EPS)) return null; // Two points have the same coordinates.
            if (!(check >=   EPS)) return null; // The three points are co-linear.
            /*
             * Applies the inverse rotation then a translation to bring
             * us back to the original coordinate system.
             */
            x1 = (x*rx2 - y*ry2) + x0;
            y1 = (y*rx2 + x*ry2) + y0;
        }
        return new Point2D.Double(x1,y1);
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
        final double[] buffer = new double[6];
        final PathIterator it = path.getPathIterator(null);
        if (!it.isDone() && it.currentSegment(buffer) == PathIterator.SEG_MOVETO && !it.isDone()) {
            final double x1 = buffer[0];
            final double y1 = buffer[1];
            final int code = it.currentSegment(buffer);
            if (it.isDone()) {
                switch (code) {
                    case PathIterator.SEG_LINETO:  return new       Line2D.Double(x1,y1, buffer[0], buffer[1]);
                    case PathIterator.SEG_QUADTO:  return new  QuadCurve2D.Double(x1,y1, buffer[0], buffer[1], buffer[2], buffer[3]);
                    case PathIterator.SEG_CUBICTO: return new CubicCurve2D.Double(x1,y1, buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5]);
                }
            }
        }
        return path;
    }
}
