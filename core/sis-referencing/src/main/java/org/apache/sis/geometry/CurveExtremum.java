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

import static java.lang.Math.*;


/**
 * Finds the extremum of the unique cubic curve which fit the two given points and derivatives.
 * First, this method finds the A, B, C and D coefficients for the following equation:
 *
 * <blockquote><var>y</var> = A + B<var>x</var> + C<var>x</var>² + D<var>x</var>³</blockquote>
 *
 * Next, this method finds the extremum by finding the (<var>x</var>,<var>y</var>) values
 * that satisfy the following equation (which is the derivative of the above equation):
 *
 * <blockquote>B + 2C<var>x</var> + 3D<var>x</var>² = 0</blockquote>
 *
 * A cubic curve can have two extremum, which are stored in this object in no particular order.
 * The distance separating the two extremum is sometime a useful information for determining if
 * a quadratic equation would be a sufficient approximation.
 *
 * <p>The points stored in this object may contains {@linkplain Double#NaN NaN} values if the
 * given geometry is actually a line segment ({@code dy1} = {@code dy2} = slope from P1 to P2).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class CurveExtremum {
    /**
     * Coordinate of the first extremum point (P1).
     */
    double ex1, ey1;

    /**
     * Coordinate of the second extremum point (P2).
     */
    double ex2, ey2;

    /**
     * Creates a new object for computing curve extremum.
     */
    CurveExtremum() {
    }

    /**
     * Finds the extremum of the unique cubic curve which fit the two given points and derivatives.
     * See class javadoc for more information.
     *
     * @param  x1   The <var>x</var> ordinate of the first point.
     * @param  y1   The <var>y</var> ordinate of the first point.
     * @param  dy1  The ∂<var>x</var>/∂<var>y</var> value at the first point.
     * @param  x2   The <var>x</var> ordinate of the second point.
     * @param  y2   The <var>y</var> ordinate of the second point.
     * @param  dy2  The ∂<var>x</var>/∂<var>y</var> value at the second point.
     * @return The two points located on the extremum of the fitted cubic curve.
     */
    void resolve(double x1, double y1, final double dy1,
                 double x2, double y2, final double dy2)
    {
        /*
         * Equation for a cubic curve is y = A + Bx + Cx² + Dx³.
         * Before to compute, translate the curve such that (x1,y1) = (0,0),
         * which simplify a lot the equation. In such case:
         *
         *   A = 0
         *   B = dy1
         *   C and D: see code below.
         */
        x2 -= x1;
        y2 -= y1;
        final double d = (dy2 - dy1)   / x2;
        final double w = (dy1 - y2/x2) / x2;
        final double D = (2*w + d)     / x2;
        final double C = -3*w - d;
        /*
         * For locating the minimum, we search the location where the derivative is zero:
         *
         *    B + 2Cx + 3Dx² == 0    ⇒    x = (-b ± √(b² - 4ac)) / (2a)
         *
         * where, a = 3*D,  b = 2*C  and  c = B = dy1
         */
        final double a  = 3 * D;
        final double b  = 2 * C;
        final double q  = -0.5 * (b + copySign(sqrt(b*b - 4*a*dy1), b));
        final double r1 = q / a;
        final double r2 = dy1 / q;
        ex1 = x1 + r1;
        ex2 = x1 + r2;
        ey1 = y1 + r1*(dy1 + r1*(C + r1*D));
        ey2 = y1 + r2*(dy1 + r2*(C + r2*D));
    }
}
