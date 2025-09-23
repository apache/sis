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
package org.apache.sis.referencing.internal.shared;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.Bezier;


/**
 * Extensions to {@link ShapeUtilities} not yet needed in main API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ShapeUtilitiesExt {
    /**
     * Do not allow instantiation of this class.
     */
    private ShapeUtilitiesExt() {
    }

    /**
     * Returns a Bézier curve passing by the given points and with the given derivatives at end points.
     * The cubic curve equation is:
     *
     * <blockquote>B(t) = (1-t)³⋅P₁ + 3(1-t)²t⋅C₁ + 3(1-t)t²⋅C₂ + t³⋅P₂</blockquote>
     *
     * where t ∈ [0…1], P₁ and P₂ are end points of the curve and C₁ and C₂ are control points generally not on the curve.
     * If the full equation is required for representing the curve, then this method builds a {@link CubicCurve2D}.
     * If the same curve can be represented by a quadratic curve, then this method returns a {@link QuadCurve2D}.
     * If the curve is actually a straight line, then this method returns a {@link Line2D}.
     *
     * <p>The (x₁,y₁) arguments give the coordinates of point P₁ at <var>t</var>=0.
     * The (x<sub>m</sub>,y<sub>m</sub>) arguments give the coordinates of the point at <var>t</var>=½.
     * The (x₂,y₂) arguments give the coordinates of point P₂ at <var>t</var>=1.</p>
     *
     * @param  x1  <var>x</var> value of the starting point.
     * @param  y1  <var>y</var> value of the starting point.
     * @param  xm  <var>x</var> value of the mid-point.
     * @param  ym  <var>y</var> value of the mid-point.
     * @param  x2  <var>x</var> value of the ending point.
     * @param  y2  <var>y</var> value of the ending point.
     * @param  α1  the derivative (∂y/∂x) at starting point.
     * @param  α2  the derivative (∂y/∂x) at ending point.
     * @param  ex  maximal distance on <var>x</var> axis between the cubic Bézier curve and quadratic or linear simplifications.
     * @param  ey  maximal distance on <var>y</var> axis between the cubic Bézier curve and quadratic or linear simplifications.
     * @return the Bézier curve passing by the 3 given points and having the given derivatives at end points.
     *
     * @see <a href="https://pomax.github.io/bezierinfo/">A Primer on Bézier Curves</a>
     */
    public static Shape bezier(final double x1, final double y1,
                               final double xm, final double ym,
                               final double x2, final double y2,
                               final double α1, final double α2,
                               final double ex, final double ey)
    {
        final Bezier bezier = new Bezier(2) {
            /* Constructor */ {
                this.εx = ex;
                this.εy = ey;
            }
            @Override protected void evaluateAt(final double t) {
                final double x, y, α;
                if (t == 0) {
                    x = x1;
                    y = y1;
                    α = α1;
                } else if (t == 1) {
                    x = x2;
                    y = y2;
                    α = α2;
                } else if (t == 0.5) {
                    x = xm;
                    y = ym;
                    α = Double.NaN;
                } else {
                    x = Double.NaN;
                    y = Double.NaN;
                    α = Double.NaN;
                }
                point[0] = x;
                point[1] = y;
                dx = 2.5;           // Artificial factor for testing purpose. Otherwise could be 1.
                dy = dx * α;
            }
        };
        final Shape path;
        try {
            path = bezier.build();
        } catch (TransformException e) {
            throw new IllegalStateException(e);         // Should never happen.
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Returns a point on the given linear, quadratic or cubic Bézier curve.
     *
     * @param  bezier  a {@link Line2D}, {@link QuadCurve2D} or {@link CubicCurve2D}.
     * @param  t       a parameter from 0 to 1 inclusive.
     * @return a point on the curve for the given <var>t</var> parameter.
     *
     * @see <a href="https://en.wikipedia.org/wiki/B%C3%A9zier_curve">Bézier curve on Wikipedia</a>
     */
    public static Point2D.Double pointOnBezier(final Shape bezier, final double t) {
        final double x, y;
        final double mt = 1 - t;
        if (bezier instanceof Line2D z) {
            x = mt * z.getX1()  +  t * z.getX2();
            y = mt * z.getY1()  +  t * z.getY2();
        } else if (bezier instanceof QuadCurve2D z) {
            final double a = mt * mt;
            final double b = mt * t * 2;
            final double c =  t * t;
            x = a * z.getX1()  +  b * z.getCtrlX()  +  c * z.getX2();
            y = a * z.getY1()  +  b * z.getCtrlY()  +  c * z.getY2();
        } else if (bezier instanceof CubicCurve2D z) {
            final double a = mt * mt * mt;
            final double b = mt * mt * t  * 3;
            final double c = mt * (t * t) * 3;
            final double d =  t *  t * t;
            x = a * z.getX1()  +  b * z.getCtrlX1()  +  c * z.getCtrlX2()  +  d * z.getX2();
            y = a * z.getY1()  +  b * z.getCtrlY1()  +  c * z.getCtrlY2()  +  d * z.getY2();
        } else {
            throw new IllegalArgumentException();
        }
        return new Point2D.Double(x, y);
    }
}
