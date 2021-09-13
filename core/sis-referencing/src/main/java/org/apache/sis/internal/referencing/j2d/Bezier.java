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
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import org.opengis.referencing.operation.TransformException;

import static java.lang.Math.abs;


/**
 * Helper class for appending Bézier curves to a path. This class computes cubic Bézier from 3 points and two slopes.
 * The three points are the start point (t=0), middle point (t=½) and end point (t=1). The slopes are at start point
 * and end point. The Bézier curve will obey exactly to those conditions (up to rounding errors).
 *
 * <p>After creating each Bézier curve, this class performs a quality control using 2 additional points located at
 * one quarter (t≈¼) and three quarters (t≈¾) of the curve. If the distance between given points and a close point
 * on the curve is greater than {@linkplain #εx} and {@linkplain #εy} thresholds, then the curve is divided in two
 * smaller curves and the process is repeated until curves meet the quality controls.</p>
 *
 * <p>If a quadratic curve degenerates to a cubic curve or a straight line, ignoring errors up to {@linkplain #εx}
 * and {@linkplain #εy}, then {@link CubicCurve2D} are replaced by {@link QuadCurve2D} or {@link Line2D}.</p>
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
     * Limit the maximal depth for avoiding shapes of unreasonable size.
     *
     * @see #depth
     */
    private static final int DEPTH_LIMIT = 12;

    /**
     * Maximum number of iterations for iterative computations. We use a small value because failure to provide
     * an answer with a small number of iterations probably means that the curve needs to be separated again in
     * two smaller curves.
     */
    private static final int MAXIMUM_ITERATIONS = 4;

    /**
     * The path where to append Bézier curves.
     */
    private final Path2D path;

    /**
     * Maximal distance (approximate) on <var>x</var> and <var>y</var> axis between Bézier curves and desired curve.
     * Also used for deciding if a cubic Bézier curve can be simplified to quadratic curve or straight line. Initial
     * value is zero. Subclasses should set the values either in their constructor or at {@link #evaluateAt(double)}
     * method call. Can be set to infinity or NaN for disabling the quality checks, or to negative value for forcing
     * unconditional divisions of Bézier curves in two sub-curves.
     */
    protected double εx, εy;

    /**
     * (<var>x</var> and <var>y</var>) coordinates at starting point <var>t</var>=0.
     * This is initialized by {@link #build()} and updated by {@link #sequence} when moving along the path.
     */
    private double x0, y0;

    /**
     * Components of α=(∂y/∂x) derivative at starting point <var>t</var>=0.
     * Initialized and updated at the same time than {@link #x0} and {@link #y0}.
     */
    private double dx0, dy0;

    /**
     * Components of α=(∂y/∂x) derivative at the point evaluated by {@link #evaluateAt(double)}.
     */
    protected double dx, dy;

    /**
     * A buffer used by subclasses for storing results of {@link #evaluateAt(double)}.
     * The two first elements are <var>x</var> and <var>y</var> coordinates respectively.
     * Other elements (if any) are ignored.
     */
    protected final double[] point;

    /**
     * The number of times a curve has been divided in smaller curves.
     *
     * @see #DEPTH_LIMIT
     */
    protected int depth;

    /**
     * Whether to force the creation of {@link QuadCurve2D}. The default value is {@code false}, which allows simplification
     * to {@link CubicCurve2D} or {@link Line2D}. This flag can be set to {@code true} when building circular shapes, in which
     * case simplifications to {@link CubicCurve2D} produce bad results.
     */
    protected boolean forceCubic;

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
     * first element and <var>y</var> coordinate in the second element. This method shall also store derivative (∂y/∂x)
     * at that location in the {@link #dx} and {@link #dy} fields. If this method can not compute a coordinate, it can
     * store {@link Double#NaN} values except for <var>t</var>=0, ½ and 1 where finite coordinates are mandatory.</p>
     *
     * <p>Subclasses can optionally update the {@link #εx} and {@link #εy} values if the tolerance thresholds change as a
     * result of this method call, for example because we come closer to a pole. The tolerance values used for each Bézier
     * curve are the ones computed at <var>t</var>=¼ and <var>t</var>=¾ of that curve.</p>
     *
     * @param  t  desired point on the curve, from 0 (start point) to 1 (end point) inclusive.
     * @throws TransformException if the point coordinates can not be computed.
     */
    protected abstract void evaluateAt(double t) throws TransformException;

    /**
     * Creates a sequence of Bézier curves from the position given by {@code evaluateAt(0)} to the position given
     * by {@code evaluateAt(1)}. This method determines the number of intermediate points required for achieving
     * the precision requested by the {@code εx} and {@code εy} parameters given at construction time.
     *
     * @return the sequence of Bézier curves.
     * @throws TransformException if the coordinates of a point can not be computed.
     */
    public final Path2D build()  throws TransformException {
        evaluateAt(0);
        x0  = point[0];
        y0  = point[1];
        dx0 = dx;
        dy0 = dy;
        path.moveTo(x0, y0);
        evaluateAt(0.5);
        final double  x2 = point[0];
        final double  y2 = point[1];
        final double dx2 = dx;
        final double dy2 = dy;
        evaluateAt(1);                                                  // `point` become (x₄,y₄) with this method call.
        sequence(0, 1, x2, y2, dx2, dy2, point[0], point[1], dx, dy);   // Must be after the call to `evaluateAt(t₄)`.
        return path;
    }

    /**
     * Creates a sequence of Bézier curves from the position given by {@code evaluateAt(tmin)} to the position given by
     * {@code evaluateAt(tmax)}. This method invokes itself recursively as long as more points are needed for achieving
     * the precision requested by the {@code εx} and {@code εy} parameters given at construction time.
     *
     * <p>The {@link #x0}, {@link #y0}, {@link #dx0} and {@link #dy0} fields must be set to the start point before this
     * method is invoked. On method completion those fields will be updated to the coordinates and derivative of end point,
     * thus enabling chaining with the next {@code sequence(…)} call.</p>
     *
     * @param  tmin  <var>t</var> value of the start point (initially 0).
     * @param  tmax  <var>t</var> value of the end point   (initially 1).
     * @param  x2    <var>x</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  y2    <var>y</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  dx2   <var>x</var> component of the the derivative (∂y/∂x) at mid-point.
     * @param  dy2   <var>y</var> component of the the derivative (∂y/∂x) at mid-point.
     * @param  x4    <var>x</var> coordinate at <var>t</var>=1 (end point).
     * @param  y4    <var>y</var> coordinate at <var>t</var>=1 (end point).
     * @param  dx4   <var>x</var> component of the derivative (∂y/∂x) at end point.
     * @param  dy4   <var>y</var> component of the derivative (∂y/∂x) at end point.
     * @throws TransformException if the coordinates of a point can not be computed.
     */
    private void sequence(final double tmin, final double tmax,
            final double x2, final double y2, final double dx2, final double dy2,
            final double x4, final double y4, final double dx4, final double dy4) throws TransformException
    {
        evaluateAt(0.25*tmin + 0.75*tmax);
        final double  x3 = point[0];
        final double  y3 = point[1];
        final double dx3 = dx;
        final double dy3 = dy;
        final double  tx = εx;
        final double  ty = εy;
        evaluateAt(0.75*tmin + 0.25*tmax);                          // `point` become (x₁,y₁) with this method call.
        final double  x1 = point[0];                                // Must be after the call to `evaluateAt(t₁)`.
        final double  y1 = point[1];
        final double dx1 = dx;
        final double dy1 = dy;
        if (tx < εx) εx = tx;                                       // Take smallest tolerance values.
        if (ty < εy) εy = ty;
        if (curve(x1, y1, x2, y2, x3, y3, x4, y4, dx4, dy4)) {
            x0  =  x4;
            y0  =  y4;
            dx0 = dx4;
            dy0 = dy4;
        } else {
            depth++;
            final double εxo = εx;
            final double εyo = εy;
            final double th = 0.5 * (tmin + tmax);
            sequence(tmin, th, x1, y1, dx1, dy1, x2, y2, dx2, dy2);
            sequence(th, tmax, x3, y3, dx3, dy3, x4, y4, dx4, dy4);
            εx = εxo;
            εy = εyo;
            depth--;
        }
    }

    /**
     * Computes a Bézier curve passing by the given points and with the given derivatives at end points.
     * The cubic curve equation is:
     *
     * <blockquote>B(t) = (1−t)³⋅P₀ + 3(1−t)²t⋅A + 3(1−t)t²⋅B + t³⋅P₄</blockquote>
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
     * <p>If the full equation is required for representing the curve, then this method appends a {@link CubicCurve2D}.
     * If the same curve can be represented by a quadratic curve, then this method appends a {@link QuadCurve2D}.
     * If the curve is actually a straight line, then this method appends a {@link Line2D}.</p>
     *
     * @param  x1   <var>x</var> coordinate at <var>t</var>≈¼ (quality control, may be NaN).
     * @param  y1   <var>y</var> coordinate at <var>t</var>≈¼ (quality control, may be NaN).
     * @param  x2   <var>x</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  y2   <var>y</var> coordinate at <var>t</var>=½ (mid-point).
     * @param  x3   <var>x</var> coordinate at <var>t</var>≈¾ (quality control, may be NaN).
     * @param  y3   <var>y</var> coordinate at <var>t</var>≈¾ (quality control, may be NaN).
     * @param  x4   <var>x</var> coordinate at <var>t</var>=1 (end point).
     * @param  y4   <var>y</var> coordinate at <var>t</var>=1 (end point).
     * @param  dx4  <var>x</var> component of the derivative α₄=∂y/∂x at ending point.
     * @param  dy4  <var>y</var> component of the derivative α₄=∂y/∂x at ending point.
     * @return {@code true} if the curve has been added to the path, or {@code false} if the approximation is not sufficient.
     */
    private boolean curve(double x1, double y1,
                          double x2, double y2,
                          double x3, double y3,
                          final double  x4, final double  y4,
                          final double dx4, final double dy4)
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
        if (!forceCubic && (abs(dx0) >= abs(dy0) ? abs(Δx*(dy0/dx0) - Δy) <= εy : abs(Δy*(dx0/dy0) - Δx) <= εx) &&
                           (abs(dx4) >= abs(dy4) ? abs(Δx*(dy4/dx4) - Δy) <= εy : abs(Δy*(dx4/dy4) - Δx) <= εx))
        {
            /*
             * Verify that all points are on the line joining P₀ to P₄. If any coordinate tested below is NaN,
             * ignore it since they are not used in Line2D computation. This allows `evaluateAt(t)` method to
             * return NaN if it can not provide a point for a given `t` value.
             */
            if (depth < DEPTH_LIMIT) {
                if (abs(Δx) >= abs(Δy)) {
                    final double m = Δy/Δx;
                    if (abs(m*x2 - y2) > εy || abs(m*x1 - y1) > εy || abs(m*x3 - y3) > εy) {
                        return false;
                    }
                } else {
                    final double m = Δx/Δy;
                    if (abs(m*y2 - x2) > εx || abs(m*y1 - x1) > εx || abs(m*y3 - x3) > εx) {
                        return false;
                    }
                }
            }
            path.lineTo(x4, y4);
            return true;
        }
        /*
         * Bezier curve equation for starting point P₀, ending point P₄ and control points A and B
         *
         * t ∈ [0…1]:  B(t)  =  (1−t)³⋅P₀ + 3(1−t)²t⋅A + 3(1−t)t²⋅B + t³⋅P₄
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
         *     (Ay − y₀) / (Ax − x₀) = α₀
         *     (y₄ − By) / (x₄ − Bx) = α₄
         *
         * Setting (x₀,y₀) = (0,0) for simplicity and rearranging above equations:
         *
         *     Ax = (8⋅x₂ − x₄)/3 − Bx              where    Bx = x₄ − (y₄ − By)/α₄
         *        = (8⋅x₂ − 4⋅x₄)/3 + (y₄ − By)/α₄
         *
         * Doing similar rearrangement for y:
         *
         *     By = (8⋅y₂ − y₄)/3 − Ay    where    Ay = Ax⋅α₀
         *        = (8⋅y₂ − y₄)/3 − Ax⋅α₀
         *
         * Putting together and isolating Ax:
         *
         *      Ax = (8⋅x₂ − 4⋅x₄)/3 + (Ax⋅α₀ − (8⋅y₂ − 4⋅y₄)/3)/α₄
         *         = (8⋅x₂ − 4⋅x₄ − (8⋅y₂ − 4⋅y₄)/α₄) / 3(1 − α₀/α₄)
         *
         * We substitute α₀=dy₀/dx₀ and α₄=dy₄/dx₄ and rearrange the dx and dy terms in order
         * to avoid division by zero. The results are the equations in the code below.
         */
        final double m  = ((8*x2 - 4*Δx)*dy4 - (8*y2 - 4*Δy)*dx4) / (3*(dx0*dy4 - dy0*dx4));
        final double ax = m * dx0;
        final double ay = m * dy0;
        final double bx, by;
        if (abs(dx4) >= abs(dy4)) {
            bx = (8*x2 - Δx)/3 - ax;
            by = Δy - (Δx - bx)*(dy4/dx4);
        } else {
            by = (8*y2 - Δy)/3 - ay;
            bx = Δx - (Δy - by)*(dx4/dy4);
        }
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
        if (depth < DEPTH_LIMIT) {
            double x = 27./64*ax + 9./64*bx + 1./64*Δx;
            double y = 27./64*ay + 9./64*by + 1./64*Δy;
            if (abs(x - x1) > εx || abs(y - y1) > εy) {
                /*
                 * Above code tested (x,y) coordinates at t=¼ exactly (we will test t=¾ later). However this t value does not
                 * necessarily correspond to one quarter of the distance, because the speed at which t varies is not the same
                 * than the speed at which Bézier curve length increases. Unfortunately computing the t values at a given arc
                 * length is complicated. We tested an approach based on computing the y value on the curve for a given x value
                 * by starting from the Bézier curve equation:
                 *
                 *     x(t) = x₀(1−t)³ + 3aₓ(1−t)²t + 3bₓ(1−t)t² + x₄t³
                 *
                 * rearranged as:
                 *
                 *     (−x₀ + 3aₓ − 3bₓ + x₄)t³ + (3x₀ − 6aₓ + 3bₓ)t² + (−3x₀ + 3aₓ)t + x₀ − x = 0
                 *
                 * and finding the roots with the CubicCurve2D.solveCubic(…) method. However the results were worst than using
                 * fixed t values. Instead we perform another test using the tangent of the curve at point P₁ (and later P₃).
                 *
                 *     x′(t) = 3(1−t)²(aₓ − x₀) + 6t(1−t)(bₓ − aₓ) + 3t²(x₄ − bₓ)           and same for y′(t).
                 *
                 * The derivatives give us a straight line (the tangent) as an approximation of the curve around P₁.
                 * We can then compute the point on that line which is nearest to P₁. It should be close to current
                 * (xi,yi) coordinates, but may vary a little bit.
                 */
                if (!isNear(x,  y,  0.25,           // Point at t=¼
                           ax, ay, 27./16,          // Coefficient for t=¼
                           bx, by, 18./16,
                           Δx, Δy,  3./16,
                           x1, y1))                 // Point for which to evaluate the distance.
                {
                    return false;
                }
            }
            /*
             * Same than above, but with point P₁ replaced by P₃ and t=¼ replaced by t=¾.
             * The change of t value changes the coefficients in formulas below.
             */
            x = 9./64*ax + 27./64*bx + 27./64*Δx;
            y = 9./64*ay + 27./64*by + 27./64*Δy;
            if (abs(x - x3) > εx || abs(y - y3) > εy) {
                if (!isNear(x,  y,  0.75,
                           ax, ay,  3./16,
                           bx, by, 18./16,
                           Δx, Δy, 27./16,
                           x3, y3))
                {
                    return false;
                }
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
         *     C   =  (3A − P₀)/2
         *     C   =  (3B − P₄)/2
         *
         * We compute C both ways and check if they are close enough to each other:
         *
         *     ΔC  =  (3⋅(B − A) − (P₄ − P₀))/2
         *
         * We multiply tolerance factor by 2 because moving the quadratic curve control point by 1 can move the closest
         * point on the curve by at most ½.
         */
        final double Δqx, Δqy;
        if (!forceCubic && abs(Δqx = (3*(bx - ax) - Δx)/2) <= 2*εx &&           // P₀ is zero.
                           abs(Δqy = (3*(by - ay) - Δy)/2) <= 2*εy)
        {
            final double qx = (3*ax + Δqx)/2;               // Take average of 2 control points.
            final double qy = (3*ay + Δqy)/2;
            path.quadTo(qx+x0, qy+y0, x4, y4);
        } else {
            path.curveTo(ax+x0, ay+y0, bx+x0, by+y0, x4, y4);
        }
        return true;
    }

    /**
     * Returns {@code true} if the point specified by the ({@code xp}, {@code yp}) coordinates is close to the Bézier curve
     * specified by the given control points. All given control points shall be shifted as if (x₀,y₀) coordinates are (0,0).
     * If this method can not determine for sure that the given point is close, it conservatively returns {@code false}.
     * This is okay since failure to determine whether the point is close may mean that the curvature is strong, in which
     * case we should probably split the curves in two smaller curves again.
     *
     * @param  ax  the <var>x</var> coordinate of first control point.
     * @param  ay  the <var>y</var> coordinate of first control point.
     * @param  bx  the <var>x</var> coordinate of mid-point.
     * @param  by  the <var>y</var> coordinate of mid-point.
     * @param  x4  the <var>x</var> coordinate of last control point.
     * @param  y4  the <var>y</var> coordinate of last control point.
     * @param  xp  the <var>x</var> coordinate of the point to test for proximity.
     * @param  yp  the <var>y</var> coordinate of the point to test for proximity.
     * @param   x  the <var>x</var> coordinate of a close point on the curve.
     * @param   y  the <var>y</var> coordinate of a close point on the curve.
     * @param   t  the <var>t</var> parameter of Bézier curve where (<var>x</var>,<var>y</var>), and <var>m</var> are evaluated.
     * @param  am  the 3(1−t)²      multiplication factor of {@code ax} and {@code ay} coordinates for given <var>t</var> value.
     * @param  bm  the 6t(1−t)      multiplication factor of {@code bx} and {@code by} coordinates for given <var>t</var> value.
     * @param  cm  the 3t²          multiplication factor of {@code x4} and {@code y4} coordinates for given <var>t</var> value.
     * @return {@code true} if the given point is close to the Bézier curve, or {@code false} otherwise or in case of doubt.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-455">SIS-455</a>
     */
    private boolean isNear(double x, double y, double t,
            final double ax, final double ay, double am,
            final double bx, final double by, double bm,
            final double x4, final double y4, double cm,
            final double xp, final double yp)
    {
        final double Δax = bx - ax;
        final double Δay = by - ay;
        final double Δbx = x4 - bx;
        final double Δby = y4 - by;
        int n = MAXIMUM_ITERATIONS;
        for (;;) {
            /*
             * Tangent of the curve:
             *
             *     x′(t) = 3(1−t)²(aₓ − x₀) + 6t(1−t)(bₓ − aₓ) + 3t²(x₄ − bₓ)           and same for y′(t).
             *
             * The derivatives give us a straight line (the tangent) as an approximation of the curve around t.
             * Then we compute the point on that line which is nearest to the given point (xp,yp). It should be
             * close to current (x,y) coordinates, but may vary a little bit.
             */
            final double tx = am * ax  +  bm * Δax  +  cm * Δbx;            // ∂x/∂t
            final double ty = am * ay  +  bm * Δay  +  cm * Δby;            // ∂y/∂t
            double xi, yi;
            if (abs(tx) >= abs(ty)) {
                final double slope  = ty / tx;                              // ∂y/∂x at t=¼.
                final double offset = (y - slope*x);                        // Value of y at x=0.
                xi = ((yp - offset) * slope + xp) / (slope*slope + 1);      // Move closer to (xp,yp).
                t += (xi - x) / tx;                                         // Using ∆x≈(∂x/∂t)⋅∆t
            } else {
                final double slope  = tx / ty;
                final double offset = (x - slope*y);
                yi = ((xp - offset) * slope + yp) / (slope*slope + 1);
                t += (yi - y) / ty;
            }
            if      (t < 0) t = 0;
            else if (t > 1) t = 1;
            /*
             * At this point we got the xi or yi coordinate or the closest point on the linear approximation,
             * and an estimation of the t Bézier parameter for that point. Use that t value for finding the
             * (xi,yi) coordinates on the Bézier curve.
             */
            am = 1 - t;                                             // New coefficients for the x(t) and y(t).
            bm = 3*am;
            cm = t*t;
            xi = ((ax*am + t*bx)*bm + x4*cm)*t;                     // 3(1−t)²t⋅aₓ + 3(1−t)t²⋅bₓ + t³⋅x₄
            yi = ((ay*am + t*by)*bm + y4*cm)*t;
            if (abs(xi - xp) <= εx && abs(yi - yp) <= εy) {
                return true;                                        // Point is close enough.
            }
            if (--n >= 0 && (abs(xi - x) > εx/2 || abs(yi - y) > εy/2)) {
                /*
                 * We need one more iteration. Change the coefficients for making them apply
                 * to the x′(t) and y′(t) derivatives instead of the x(t) and y(t) functions.
                 */
                am *= bm;           // 3(1−t)²
                bm *= 2*t;          // 6t(1−t)
                cm *= 3;            // 3t²
                x = xi;
                y = yi;
            } else {
                /*
                 * Too many iterations, or a coordinate value is NaN, or we find the nearest point
                 * but that point is too far from the given (xp,yp) coordinates.
                 */
                return false;
            }
        }
    }
}
