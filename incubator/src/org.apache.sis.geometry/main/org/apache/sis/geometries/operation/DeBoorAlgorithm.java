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
package org.apache.sis.geometries.operation;

import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;

/**
 * De Boor's algorithm: evaluation of a B-spline of degree p at a parameter u, by repeated linear interpolation of the
 * control points over the knot vector.
 *
 * <p>
 * The algorithm makes no assumption about the meaning of the coordinates: the points may be plain points or points in
 * homogeneous coordinates (weighted, with the weight as an extra last component). Callers dealing with NURBS therefore
 * pass homogeneous points in and dehomogenize the returned point themselves, which is also what allows two passes to be
 * chained for the tensor product of a surface.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DeBoorAlgorithm {

    private DeBoorAlgorithm() {
    }

    /**
     * Index of the knot span containing u, that is the index k such as {@code knots[k] <= u < knots[k+1]}, clamped to
     * the valid range [degree … n].
     *
     * @param u parameter to locate
     * @param knots knot vector
     * @param degree curve degree
     * @param n index of the last control point
     * @return index of the knot span containing u
     */
    public static int findKnotSpan(final double u, final double[] knots, final int degree, final int n) {
        if (u >= knots[n + 1]) {
            return n;
        }
        if (u <= knots[degree]) {
            return degree;
        }

        int low = degree, high = n + 1;
        int mid = (low + high) / 2;
        while (u < knots[mid] || u >= knots[mid + 1]) {
            if (u < knots[mid]) {
                high = mid;
            } else {
                low = mid;
            }
            mid = (low + high) / 2;
        }
        return mid;
    }

    /**
     * Evaluates the curve at parameter u. Only the degree+1 control points of the knot span containing u contribute to
     * the result, they are selected before delegating to {@link #evaluateWindow evaluateWindow(…)}.
     *
     * @param u parameter at which to evaluate the curve
     * @param points all the control points of the curve
     * @param knots knot vector
     * @param degree curve degree
     * @return the point of the curve at parameter u
     */
    public static Vector<?> evaluate(final double u, final Vector<?>[] points, final double[] knots, final int degree) {
        final int n = points.length - 1;
        final int k = findKnotSpan(u, knots, degree, n);
        final Vector<?>[] window = new Vector<?>[degree + 1];
        for (int j = 0; j <= degree; j++) {
            window[j] = points[k - degree + j];
        }
        return evaluateWindow(u, window, knots, degree, k);
    }

    /**
     * The heart of the algorithm, taking ONLY the degree+1 relevant control points (already selected through the knot
     * span k) so that it can be reused as-is by curves AND by surfaces (tensor product), the latter feeding it a window
     * it has computed itself.
     *
     * @param u parameter at which to evaluate the curve
     * @param window the degree+1 control points of the knot span k
     * @param knots knot vector
     * @param degree curve degree
     * @param k index of the knot span containing u
     * @return the point of the curve at parameter u
     */
    public static Vector<?> evaluateWindow(final double u, final Vector<?>[] window, final double[] knots, final int degree, final int k) {
        final Vector<?>[] d = window.clone();
        for (int r = 1; r <= degree; r++) {
            for (int j = degree; j >= r; j--) {
                final int i = k - degree + j;
                final double denom = knots[i + degree - r + 1] - knots[i];
                final double alpha = (denom == 0.0) ? 0.0 : (u - knots[i]) / denom;
                // Linear interpolation (1-alpha)*d[j-1] + alpha*d[j] : the heart of the recursion.
                d[j] = d[j - 1].copy().lerp(d[j], alpha);
            }
        }
        return d[degree];
    }

    /**
     * Control points of the derivative of a B-spline (standard formula): the derivative of a curve of degree p is a
     * curve of degree p-1 with control points Q_i = p * (P_{i+1} - P_i) / (U_{i+p+1} - U_{i+1}). The derived curve is
     * evaluated with the knot vector of the original curve deprived of its first and last values.
     *
     * <p>Works on "plain" points as well as on homogeneous points (Vector makes no distinction), hence its reuse as-is
     * by the curves and by the surfaces.</p>
     *
     * @param ctrl control points of the curve to differentiate
     * @param knots knot vector
     * @param degree curve degree
     * @return the control points of the derivative, one less than the given ones
     */
    public static Vector<?>[] derivativeControlPoints(final ReadOnly.Vector<?>[] ctrl, final double[] knots, final int degree) {
        final int m = ctrl.length - 1;
        final Vector<?>[] deriv = new Vector<?>[m];
        for (int i = 0; i < m; i++) {
            final double denom = knots[i + degree + 1] - knots[i + 1];
            deriv[i] = ctrl[i + 1].copy().subtract(ctrl[i]).scale((denom == 0.0) ? 0 : degree / denom);
        }
        return deriv;
    }
}
