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

import java.awt.geom.Path2D;
import org.opengis.referencing.operation.TransformException;

import static java.lang.Math.abs;
import static java.lang.Double.isInfinite;


/**
 * Helper class for appending Bézier curves to a path.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://pomax.github.io/bezierinfo/">A Primer on Bézier Curves</a>
 *
 * @since 1.0
 * @module
 */
public abstract class Bezier {
    /**
     * The path where to append Bézier curves.
     */
    private final Path2D path;

    /**
     * Maximal distance on <var>x</var> and <var>y</var> axis between the cubic Bézier curve and quadratic
     * or linear simplifications. Initial value is zero. Subclasses should set the values either in their
     * constructor or at {@link #evaluateAt(double)} method call.
     */
    protected double εx, εy;

    /**
     * (<var>x</var> and <var>y</var>) coordinates and (∂y/∂x) derivative of starting point at <var>t</var>=0.
     */
    private double x0, y0, α0;

    /**
     * A buffer used by subclasses for storing results of {@link #evaluateAt(double)}.
     * The two first elements are <var>x</var> and <var>y</var> coordinates respectively.
     * Other elements (if any) are ignored.
     */
    protected final double[] point;

    /**
     * Creates a new builder.
     *
     * @param  dimension  length of the {@link #point} array. Must be at least 2.
     */
    protected Bezier(final int dimension) {
        point = new double[dimension];
        path  = new Path2D.Double();
    }

    /**
     * Invoked for computing a new point on the Bézier curve. This method is invoked with a <var>t</var> value varying from
     * 0 to 1 inclusive. Value 0 is for the starting point and value 1 is for the ending point. Other values are for points
     * interpolated between the start and end points. In particular value ½ is for the point in the middle of the curve.
     * This method will also be invoked at least for values ¼ and ¾, and potentially for other values too.
     *
     * <p>This method shall store the point coordinates in the {@link #point} array with <var>x</var> coordinate in the
     * first element and <var>y</var> coordinate in the second element. The returned value is the derivative (∂y/∂x) at
     * that location. If this method can not compute a coordinate, it can store {@link Double#NaN} values except for
     * <var>t</var>=0, ½ and 1 where finite coordinates are mandatory.</p>
     *
     * <p>Subclasses can optionally update the {@link #εx} and {@link #εy} values if the tolerance thresholds change as a
     * result of this method call, for example because we come closer to a pole. The tolerance values used for each Bézier
     * curve are the ones computed at <var>t</var>=¼ and <var>t</var>=¾ of that curve.</p>
     *
     * @param  t  desired point on the curve, from 0 (start point) to 1 (end point) inclusive.
     * @return derivative (∂y/∂x) at the point.
     * @throws TransformException if the point coordinates can not be computed.
     */
    protected abstract double evaluateAt(double t) throws TransformException;

    /**
     * Creates a sequence of Bézier curves from the position given by {@code evaluateAt(0)} to the position given
     * by {@code evaluateAt(0)}. This method determines the number of intermediate points required for achieving
     * the precision requested by the {@code εx} and {@code εy} parameters given at construction time.
     *
     * @return the sequence of Bézier curves.
     * @throws TransformException if the coordinates of a point can not be computed.
     */
    public final Path2D build()  throws TransformException {
        α0 = evaluateAt(0);
        x0 = point[0];
        y0 = point[1];
        path.moveTo(x0, y0);
        final double α2 = evaluateAt(0.5);
        final double x2 = point[0];
        final double y2 = point[1];
        final double α4 = evaluateAt(1);                                // `point` become (x₄,y₄) with this method call.
        sequence(0, 1, x2, y2, α2, point[0], point[1], α4);             // Must be after the call to `evaluateAt(t₄)`.
        return path;
    }

    /**
     * Creates a sequence of Bézier curves from the position given by {@code evaluateAt(tmin)} to the position given by
     * {@code evaluateAt(tmax)}. This method invokes itself recursively as long as more points are needed for achieving
     * the precision requested by the {@code εx} and {@code εy} parameters given at construction time.
     *
     * <p>The {@link #x0}, {@link #y0} and {@link #α0} fields must be set to the start point before this method is invoked.
     * On method completion those fields will be updated to the coordinates and derivative of end point, thus enabling
     * chaining with the next {@code sequence(…)} call.</p>
     *
     * @param  tmin  <var>t</var> value of the start point (initially 0).
     * @param  tmax  <var>t</var> value of the end point   (initially 1).
     * @param  x2    <var>x</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  y2    <var>y</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  α2    the derivative (∂y/∂x) at mid-point.
     * @param  x4    <var>x</var> coordinate at <var>t</var>=1 (end point).
     * @param  y4    <var>y</var> coordinate at <var>t</var>=1 (end point).
     * @param  α4    the derivative (∂y/∂x) at end point.
     * @throws TransformException if the coordinates of a point can not be computed.
     */
    private void sequence(final double tmin, final double tmax,
            final double x2, final double y2, final double α2,
            final double x4, final double y4, final double α4) throws TransformException
    {
        final double α3 = evaluateAt(0.25*tmin + 0.75*tmax);
        final double x3 = point[0];
        final double y3 = point[1];
        final double tx = εx;
        final double ty = εy;
        final double α1 = evaluateAt(0.75*tmin + 0.25*tmax);            // `point` become (x₁,y₁) with this method call.
        if (tx < εx) εx = tx;                                           // Take smallest tolerance values.
        if (ty < εy) εy = ty;
        if (curve(point[0], point[1], x2, y2, x3, y3, x4, y4, α4)) {    // Must be after the call to `evaluateAt(t₁)`.
            x0 = x4;
            y0 = y4;
            α0 = α4;
        } else {
            final double εxo = εx;
            final double εyo = εy;
            final double th = 0.5 * (tmin + tmax);
            sequence(tmin, th, point[0], point[1], α1, x2, y2, α2);
            sequence(th, tmax, x3,       y3,       α3, x4, y4, α4);
            εx = εxo;
            εy = εyo;
        }
    }

    /**
     * Computes a Bézier curve passing by the given points and with the given derivatives at end points.
     * The cubic curve equation is:
     *
     * <blockquote>B(t) = (1-t)³⋅P₀ + 3(1-t)²t⋅A + 3(1-t)t²⋅B + t³⋅P₄</blockquote>
     *
     * where t ∈ [0…1], P₀ and P₄ are start and end points of the curve and A and B are control points generally not on the curve.
     * For any point <var>P<sub>i</sub></var>, the index <var>i</var> gives the value of the parameter <var>t</var> where the point
     * is located with <var>t</var> = <var>i</var>/4, and coordinates (<var>x</var>, <var>y</var>) follow the same indices.
     * The (x₀,y₀) fields give the coordinates of point P₀ at <var>t</var>=0 (0/4).
     * The (x₂,y₂) arguments give the coordinates of the point at <var>t</var>=½ (2/4).
     * The (x₄,y₄) arguments give the coordinates of point P₄ at <var>t</var>=1 (4/4).
     *
     * <p>The P₁ and P₃ points are for quality control. They should be points at <var>t</var>≈¼ <var>t</var>≈¾ respectively.
     * Those points are not used for computing the curve, but are used for checking if the curve is an accurate approximation.
     * If the curve is not accurate enough, then this method does nothing and return {@code false}. In that case, the caller
     * should split the curve in two smaller parts and invoke this method again.</p>
     *
     * <p>If the full equation is required for representing the curve, then this method appends a {@link java.awt.geom.CubicCurve2D}.
     * If the same curve can be represented by a quadratic curve, then this method appends a {@link java.awt.geom.QuadCurve2D}.
     * If the curve is actually a straight line, then this method appends a {@link java.awt.geom.Line2D}.</p>
     *
     * @param  x1  <var>x</var> coordinate at <var>t</var>≈¼ (quality control, may be NaN).
     * @param  y1  <var>y</var> coordinate at <var>t</var>≈¼ (quality control, may be NaN).
     * @param  x2  <var>x</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  y2  <var>y</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  x3  <var>x</var> coordinate at <var>t</var>≈¾ (quality control, may be NaN).
     * @param  y3  <var>y</var> coordinate at <var>t</var>≈¾ (quality control, may be NaN).
     * @param  x4  <var>x</var> coordinate at <var>t</var>=1 (end point).
     * @param  y4  <var>y</var> coordinate at <var>t</var>=1 (end point).
     * @param  α4  the derivative (∂y/∂x) at ending point.
     * @return {@code true} if the curve has been added to the path, or {@code false} if the approximation is not sufficient.
     */
    private boolean curve(double x1, double y1,
                          double x2, double y2,
                          double x3, double y3,
                          final double x4, final double y4, final double α4)
    {
        /*
         * Equations in this method are simplified as if (x₀,y₀) coordinates are (0,0).
         * Adjust (x₁,y₁) to (x₄,y₄) consequently. If derivatives are equal, equation
         * for cubic curve will not work (division by zero). But we can return a line
         * instead if derivatives are equal to Δy/Δx slope and way-points are colinear.
         */
        x1 -= x0;  x2 -= x0;  x3 -= x0;
        y1 -= y0;  y2 -= y0;  y3 -= y0;
        final double Δx = x4 - x0;
        final double Δy = y4 - y0;
        final boolean isVertical = abs(Δx) <= εx;
        if (isVertical || ((isInfinite(α0) || abs(Δx*α0 - Δy) <= εy)
                       &&  (isInfinite(α4) || abs(Δx*α4 - Δy) <= εy)))
        {
            /*
             * Following tests are partially redundant with above tests, but may detect a larger
             * error than above tests did. They are also necessary if a derivative was infinite,
             * or if `isVertical` was true.
             */
            final boolean isHorizontal = abs(Δy) <= εy;
            if (isHorizontal || ((α0 == 0 || abs(Δy/α0 - Δx) <= εx)
                             &&  (α4 == 0 || abs(Δy/α4 - Δx) <= εx)))
            {
                /*
                 * Verify that all points are on the line joining P₀ to P₄. We test P₂ first since it is the most likely
                 * to be away if the curve is not almost a line. If any coordinate tested below is NaN, ignore it since
                 * they are not used in Line2D computation. This allows `evaluateAt(t)` method to return NaN if it can
                 * not provide a point for a given `t` value.
                 */
                final double slope = Δy / Δx;
                if ((!isVertical   && (abs(x2*slope - y2) > εy || abs(x1*slope - y1) > εy || abs(x3*slope - y3) > εy)) ||
                    (!isHorizontal && (abs(y2/slope - x2) > εx || abs(y1/slope - x1) > εx || abs(y3/slope - x3) > εx)))
                {
                    return false;
                }
                path.lineTo(x4, y4);
                return true;
            }
        }
        /*
         * Bezier curve equation for starting point P₀, ending point P₄ and control points A and B
         *
         * t ∈ [0…1]:  B(t)  =  (1-t)³⋅P₀ + 3(1-t)²t⋅A + 3(1-t)t²⋅B + t³⋅P₄
         * Midpoint:   B(½)  =  ⅛⋅(P₀ + P₄) + ⅜⋅(A + B)
         *
         * Notation:   (x₀, y₀)   are coordinates of P₀ (same rule for P₄).
         *             (x₂, y₂)   are coordinates of midpoint.
         *             (Ax, Ay)   are coordinates of control point A (same rule for B).
         *             α₀ and α₄  are derivative (∂y/∂x) at P₀ and P₄ respectively.
         *
         * Some relationships:
         *
         *     x₂ = ⅛⋅(x₀ + x₄) + ⅜⋅(Ax + Bx)
         *     (Ay - y₀) / (Ax - x₀) = α₀
         *     (y₄ - By) / (x₄ - Bx) = α₄
         *
         * Setting (x₀,y₀) = (0,0) for simplicity and rearranging above equations:
         *
         *     Ax = (8⋅x₂ - x₄)/3 - Bx              where    Bx = x₄ - (y₄ - By)/α₄
         *        = (8⋅x₂ - 4⋅x₄)/3 + (y₄ - By)/α₄
         *
         * Doing similar rearrangement for y:
         *
         *     By = (8⋅y₂ - y₄)/3 - Ay    where    Ay = Ax⋅α₀
         *        = (8⋅y₂ - y₄)/3 - Ax⋅α₀
         *
         * Putting together and isolating Ax:
         *
         *      Ax = (8⋅x₂ - 4⋅x₄)/3 + (Ax⋅α₀ - (8⋅y₂ - 4⋅y₄)/3)/α₄
         *         = (8⋅x₂ - 4⋅x₄ - (8⋅y₂ - 4⋅y₄)/α₄) / 3(1 - α₀/α₄)
         */
        final double ax = ((8*x2 - 4*Δx)*α4 - (8*y2 - 4*Δy)) / (3*(α4 - α0));
        final double ay = ax * α0;
        final double bx = (8*x2 - Δx)/3 - ax;
        final double by = Δy - (Δx - bx)*α4;
        /*
         * At this point we got the control points A and B. Verify if the curve is a good approximation.
         * We compute the points on the curve at t=¼ and t=¾ as below (simplified with P₀ = (0,0)) and
         * compare with the given values:
         *
         *     P(¼) = (27⋅P₀ + 27⋅A +  9⋅B +    P₄)/64
         *     P(¾) = (   P₀ +  9⋅A + 27⋅B + 27⋅P₄)/64
         *
         * If any of (x₁,y₁) and (x₃,y₃) coordinates is NaN, then this method accepts the curve as valid.
         * This allows `evaluateAt(t)` to return NaN if it can not provide a point for a given `t` value.
         */
        double xi = 27./64*ax + 9./64*bx + 1./64*Δx;        // "xi" is for "x interpolated (on curve)".
        double yi = 27./64*ay + 9./64*by + 1./64*Δy;
        if (abs(xi - x1) > εx || abs(yi - y1) > εy) {
            /*
             * Above code tested (x,y) coordinates at t=¼ exactly (we will test t=¾ later). However this t value does not
             * necessarily correspond to one quarter of the distance, because the speed at which t varies is not the same
             * than the speed at which Bézier curve length increases. Unfortunately computing the t values at a given arc
             * length is complicated. We tested an approach based on computing the y value on the curve for a given x value
             * by starting from the Bézier curve equation:
             *
             *     x(t) = x₀(1-t)³ + 3a(1-t)²t + 3b(1-t)t² + x₄t³
             *
             * rearranged as:
             *
             *     (-x₀ + 3a - 3b + x₄)t³ + (3x₀ - 6a + 3b)t² + (-3x₀ + 3a)t + x₀ - x = 0
             *
             * and finding the roots with the CubicCurve2D.solveCubic(…) method. However the results were worst than using
             * fixed t values. If we want to improve on that in a future version, we would need a function for computing
             * arc length (for example based on https://pomax.github.io/bezierinfo/#arclength), then use iterative method
             * like https://www.geometrictools.com/Documentation/MovingAlongCurveSpecifiedSpeed.pdf (retrieved May 2019).
             *
             * Instead we perform another test using the tangent of the curve at point P₁ (and later P₃).
             *
             *     x′(t) = 3(1-t)²(a-x₀) + 6(1-t)t(b-a) + 3t²(x₄ - b)       and same for y′(t).
             *
             * The derivatives give us a straight line (the tangent) as an approximation of the curve around P₁.
             * We can then compute the point on that line which is nearest to P₁. It should be close to current
             * (xi,yi) coordinates, but may vary a little bit.
             */
            double slope  = (27./16*ay + 18./16*(by-ay) + 3./16*(y4-by))
                          / (27./16*ax + 18./16*(bx-ax) + 3./16*(x4-bx));       // ∂y/∂x at t=¼.
            double offset = (yi - slope*xi);                                    // Value of y at x=0.
            xi = ((y1 - offset) * slope + x1) / (slope*slope + 1);              // Closer (xi,yi) coordinates.
            yi = offset + xi*slope;
            xi = abs(xi - x1);                                                  // At this point (xi,yi) are distances.
            yi = abs(yi - y1);
            if ((xi > εx && xi != Double.POSITIVE_INFINITY) ||
                (yi > εy && yi != Double.POSITIVE_INFINITY))
            {
                return false;
            }
        }
        /*
         * Same than above, but with point P₁ replaced by P₃ and t=¼ replaced by t=¾.
         * The change of t value changes the coefficients in formulas below.
         */
        xi = 9./64*ax + 27./64*bx + 27./64*Δx;
        yi = 9./64*ay + 27./64*by + 27./64*Δy;
        if (abs(xi - x3) > εx || abs(yi - y3) > εy) {
            double slope  = (3./16*ay + 18./16*(by-ay) + 27./16*(y4-by))
                          / (3./16*ax + 18./16*(bx-ax) + 27./16*(x4-bx));
            double offset = (yi - slope*xi);
            xi = ((y3 - offset) * slope + x3) / (slope*slope + 1);
            yi = offset + xi*slope;
            xi = abs(xi - x3);
            yi = abs(yi - y3);
            if ((xi > εx && xi != Double.POSITIVE_INFINITY) ||
                (yi > εy && yi != Double.POSITIVE_INFINITY))
            {
                return false;
            }
        }
        /*
         * Verify if we can simplify cubic curve to a quadratic curve. If we were elevating the Bézier curve degree from
         * quadratic to cubic, the control points A and B would be computed from control point C of the quadratic curve:
         *
         *     A  =  ⅓P₀ + ⅔C
         *     B  =  ⅓P₄ + ⅔C
         *
         * We want C instead, which can be computed in two ways:
         *
         *     C   =  (3A - P₀)/2
         *     C   =  (3B - P₄)/2
         *
         * We compute C both ways and check if they are close enough to each other:
         *
         *     ΔC  =  (3⋅(B - A) - (P₄ - P₀))/2
         */
        final double Δqx, Δqy;
        if (abs(Δqx = (3*(bx - ax) - Δx)/2) <= εx &&        // P₀ is zero.
            abs(Δqy = (3*(by - ay) - Δy)/2) <= εy)
        {
            final double qx = (3*ax + Δqx)/2;               // Take average of 2 control points.
            final double qy = (3*ay + Δqy)/2;
            path.quadTo(qx+x0, qy+y0, x4, y4);
        } else {
            path.curveTo(ax+x0, ay+y0, bx+x0, by+y0, x4, y4);
        }
        return true;
    }
}
