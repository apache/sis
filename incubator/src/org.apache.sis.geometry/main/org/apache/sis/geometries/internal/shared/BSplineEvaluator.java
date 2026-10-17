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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributeInterpolation;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.GaussLegendreQuadrature;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * Arc length parameterization of a B-spline, optionally rational, in the coordinate space of its
 * control points. A Bézier curve is the B-spline of its control points with clamped knots.
 *
 * <p>The arc length is integrated over each knot span by adaptive Gauss-Legendre quadrature of the
 * speed, computed from the analytic derivatives of the basis functions. The parameter at a given
 * arc length is then found by Newton iterations safeguarded by bisection.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class BSplineEvaluator {
    /**
     * The quadrature used for integrating the speed into an arc length.
     */
    private static final GaussLegendreQuadrature QUADRATURE = new GaussLegendreQuadrature(10);

    /**
     * Relative accuracy of the integrated arc length.
     */
    private static final double TOLERANCE = 1E-13;

    /**
     * Maximal number of interval halvings when integrating the arc length.
     */
    private static final int MAX_DEPTH = 20;

    private final DataPoints controls;
    private final double[][] positions;
    private final double[] weights;
    private final double[] knots;
    private final int degree;

    /**
     * Index of the last control point.
     */
    private final int last;

    /**
     * Intervals of the parameter over which the arc length has been integrated, in increasing order.
     * Each entry is {start, end, arc length at start, arc length of the interval, knot span index}.
     */
    private final List<double[]> intervals = new ArrayList<>();

    /**
     * Total arc length.
     */
    private double length;

    /**
     * Creates an evaluator for the given B-spline.
     *
     * @param  controls  control points, with their attributes.
     * @param  weights   weight of each control point, or {@code null} for a non-rational curve.
     * @param  knots     knot vector, of length {@code controls.size() + degree + 1}.
     * @param  degree    degree of the basis functions.
     * @throws IllegalArgumentException if the arguments do not define a B-spline.
     */
    BSplineEvaluator(final DataPoints controls, final double[] weights, final double[] knots, final int degree) {
        final int count = controls.size();
        if (degree < 0 || count < degree + 1) {
            throw new IllegalArgumentException("A curve of degree " + degree + " needs at least " + (degree + 1)
                    + " control points, but got " + count + '.');
        }
        if (knots.length != count + degree + 1) {
            throw new IllegalArgumentException("A curve of degree " + degree + " with " + count + " control points needs "
                    + (count + degree + 1) + " knots, but got " + knots.length + '.');
        }
        if (weights != null && weights.length != count) {
            throw new IllegalArgumentException("Expected " + count + " weights, but got " + weights.length + '.');
        }
        this.controls = controls;
        this.knots    = knots;
        this.degree   = degree;
        this.last     = count - 1;
        this.weights  = (weights != null) ? weights : filled(count, 1);
        positions = new double[count][];
        for (int i = 0; i < count; i++) {
            positions[i] = controls.getPosition(i).toArrayDouble();
        }
        for (int k = degree; k <= last; k++) {
            final double a = knots[k], b = knots[k+1];
            if (b > a) {
                final int span = k;
                QUADRATURE.integrate((u) -> speed(u, span), a, b, TOLERANCE, MAX_DEPTH, (start, end, integral) -> {
                    intervals.add(new double[] {start, end, length, integral, span});
                    length += integral;
                });
            }
        }
    }

    private static double[] filled(final int count, final double value) {
        final double[] array = new double[count];
        Arrays.fill(array, value);
        return array;
    }

    /**
     * Returns a knot vector clamped at both ends, with the given knots in between.
     *
     * @param  count        number of control points.
     * @param  degree       degree of the basis functions.
     * @param  breakpoints  the distinct knot values, from the first to the last one, or an empty array
     *                      for uniformly distributed knots between 0 and 1.
     * @return the knot vector, of length {@code count + degree + 1}.
     * @throws IllegalArgumentException if the number of breakpoints does not match the number of control points.
     */
    static double[] clampedKnots(final int count, final int degree, double[] breakpoints) {
        final int spans = count - degree;
        if (spans < 1) {
            throw new IllegalArgumentException("A curve of degree " + degree + " needs at least " + (degree + 1)
                    + " control points, but got " + count + '.');
        }
        if (breakpoints.length == 0) {
            breakpoints = new double[spans + 1];
            for (int i = 0; i <= spans; i++) {
                breakpoints[i] = i / (double) spans;
            }
        } else if (breakpoints.length != spans + 1) {
            throw new IllegalArgumentException("A clamped curve of degree " + degree + " with " + count
                    + " control points needs " + (spans + 1) + " distinct knots, but got " + breakpoints.length + '.');
        }
        final double[] knots = new double[count + degree + 1];
        Arrays.fill(knots, 0, degree + 1, breakpoints[0]);
        System.arraycopy(breakpoints, 1, knots, degree + 1, spans - 1);
        Arrays.fill(knots, count, knots.length, breakpoints[spans]);
        return knots;
    }

    /**
     * Returns the knot vector of a piecewise Bézier curve: each piece is a span of the parameter,
     * delimited by knots of multiplicity {@code degree}.
     *
     * @param  count   number of control points, {@code degree} × pieces + 1.
     * @param  degree  degree of the Bézier pieces.
     * @return the knot vector, of length {@code count + degree + 1}.
     * @throws IllegalArgumentException if the control points do not make a whole number of pieces.
     */
    static double[] bezierKnots(final int count, final int degree) {
        if (degree < 1 || count < degree + 1 || (count - 1) % degree != 0) {
            throw new IllegalArgumentException("A Bézier curve of degree " + degree + " needs a multiple of "
                    + degree + " plus one control points, but got " + count + '.');
        }
        final int pieces = (count - 1) / degree;
        final double[] knots = new double[count + degree + 1];
        for (int i = 0; i < knots.length; i++) {
            knots[i] = Math.min(pieces, Math.max(0, (i - 1) / degree));
        }
        return knots;
    }

    /**
     * Returns the given control points as a {@code DataPoints} in the given reference system.
     * The control points of some curves are only positions, sometimes without reference system.
     *
     * @param  points  the control points.
     * @param  crs     the reference system of the curve.
     * @return the control points as a {@code DataPoints} of positions.
     */
    static DataPoints asDataPoints(final Array points, final CoordinateReferenceSystem crs) {
        if (points.getCoordinateReferenceSystem() != null) {
            return GeometryFactory.DEFAULT.createDataPoints(points);
        }
        final Array copy = NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, points.getLength());
        for (long i = 0, n = points.getLength(); i < n; i++) {
            copy.set(i, points.get(i));
        }
        return GeometryFactory.DEFAULT.createDataPoints(copy);
    }

    /**
     * Returns the total arc length, in the unit of the coordinates.
     */
    double length() {
        return length;
    }

    /**
     * Returns the point at the given distance along the curve.
     *
     * @param  dist  the distance, in {@code unit} or in percent of the length.
     * @param  unit  unit of the coordinates.
     * @return the point at the given distance.
     * @throws IllegalArgumentException if the distance is outside the curve.
     */
    Point subCurve(final Quantity<?> dist, final Unit<?> unit) {
        final double s = Curves.toArcLength(dist, length, unit);
        if (intervals.isEmpty()) {
            return pointAt(knots[degree], degree);                  // Degenerate curve with no span.
        }
        double[] interval = intervals.get(intervals.size() - 1);
        for (final double[] candidate : intervals) {
            if (candidate[2] + candidate[3] >= s) {
                interval = candidate;
                break;
            }
        }
        final double a = interval[0], c0 = interval[2], len = interval[3];
        final int k = (int) interval[4];
        double u = a;
        if (len > 0) {
            double lo = a, hi = interval[1];
            u = a + (hi - a) * Math.min(1, (s - c0) / len);
            for (int i = 0; i < 60; i++) {
                final double f = c0 + quadrature(k, a, u) - s;
                if (Math.abs(f) <= TOLERANCE * length) break;
                if (f > 0) hi = u; else lo = u;
                final double speed = speed(u, k);
                double next = (speed > 0) ? u - f / speed : Double.NaN;
                if (!(next > lo && next < hi)) {
                    next = (lo + hi) / 2;               // Bisection when Newton leaves the bracket.
                }
                if (next == u) break;
                u = next;
            }
        }
        return pointAt(u, k);
    }

    /**
     * Returns the distinct knot values from the start to the end of the curve domain.
     * The curve is a polynomial or rational function between two consecutive values.
     */
    double[] breakpoints() {
        final double[] values = new double[last - degree + 2];
        int n = 0;
        for (int k = degree; k <= last + 1; k++) {
            if (n == 0 || knots[k] > values[n - 1]) {
                values[n++] = knots[k];
            }
        }
        return Arrays.copyOf(values, n);
    }

    /**
     * Returns a polyline whose points are on the curve, including the positions at the knots.
     *
     * @param  curve    the curve evaluated by this object.
     * @param  spacing  maximal distance along the curve between two points, or zero or {@code null} for no limit.
     * @param  offset   maximal distance between the curve and the polyline, or zero or {@code null} for no limit.
     * @return the polyline approximating the curve.
     */
    LineString asLine(final Curve curve, final Quantity<?> spacing, final Quantity<?> offset) {
        return CurveLinearizer.linearize(curve, spacing, offset,
                List.of(new CurveLinearizer.Piece(this::position, breakpoints(), null)));
    }

    /**
     * Returns the position of the curve at parameter {@code u}, without the other attributes.
     *
     * @param  u  the parameter, in the curve domain.
     * @return the position at the given parameter.
     */
    Vector<?> position(final double u) {
        int k = degree;
        for (int i = degree; i <= last; i++) {
            if (knots[i] < knots[i+1] && (u >= knots[i] || i == degree)) {
                k = i;
            }
        }
        final double[] R = new double[degree + 1];
        basis(u, k, R, null);
        final int dim = positions[0].length;
        final double[] sum = new double[dim];
        double W = 0;
        for (int r = 0; r <= degree; r++) {
            final int i = k - degree + r;
            final double w = R[r] * weights[i];
            W += w;
            for (int c = 0; c < dim; c++) {
                sum[c] += w * positions[i][c];
            }
        }
        final Vector<?> position = Vectors.create(SampleSystem.of(controls.getCoordinateReferenceSystem()), DataType.DOUBLE);
        for (int c = 0; c < dim; c++) {
            position.set(c, sum[c] / W);
        }
        return position;
    }

    /**
     * Integrates the speed over [a … b] in span {@code k}, which gives the arc length.
     */
    private double quadrature(final int k, final double a, final double b) {
        return QUADRATURE.integrate((u) -> speed(u, k), a, b);
    }

    /**
     * Returns the norm of the derivative of the curve at parameter {@code u} in span {@code k}.
     */
    private double speed(final double u, final int k) {
        final double[] N = new double[degree + 1], dN = new double[degree + 1];
        basis(u, k, N, dN);
        final int dim = positions[0].length;
        final double[] A = new double[dim], dA = new double[dim];
        double W = 0, dW = 0;
        for (int r = 0; r <= degree; r++) {
            final int i = k - degree + r;
            final double w = weights[i];
            W  += N[r]  * w;
            dW += dN[r] * w;
            for (int c = 0; c < dim; c++) {
                A[c]  += N[r]  * w * positions[i][c];
                dA[c] += dN[r] * w * positions[i][c];
            }
        }
        double sum = 0;
        for (int c = 0; c < dim; c++) {
            final double d = (dA[c] - dW * A[c] / W) / W;      // Quotient rule on A/W.
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * Computes the {@code degree + 1} basis functions which are non-zero in span {@code k},
     * and optionally their derivatives.
     *
     * @param u   the parameter, in span {@code k}.
     * @param k   index of the knot span.
     * @param N   where to store the values of the basis functions {@code k - degree} to {@code k}.
     * @param dN  where to store their derivatives, or {@code null} if not needed.
     */
    private void basis(final double u, final int k, final double[] N, final double[] dN) {
        final double[] left = new double[degree + 1], right = new double[degree + 1];
        double[] lower = null;                  // Basis functions of degree − 1.
        N[0] = 1;
        for (int j = 1; j <= degree; j++) {
            if (j == degree) {
                lower = Arrays.copyOf(N, degree);
            }
            left[j]  = u - knots[k + 1 - j];
            right[j] = knots[k + j] - u;
            double saved = 0;
            for (int r = 0; r < j; r++) {
                final double denom = right[r + 1] + left[j - r];
                final double temp  = (denom != 0) ? N[r] / denom : 0;
                N[r]  = saved + right[r + 1] * temp;
                saved = left[j - r] * temp;
            }
            N[j] = saved;
        }
        if (dN != null) {
            for (int r = 0; r <= degree; r++) {
                double d = 0;
                if (degree > 0) {
                    final int i = k - degree + r;
                    if (r >= 1) {
                        final double denom = knots[i + degree] - knots[i];
                        if (denom != 0) d += lower[r - 1] / denom;
                    }
                    if (r < degree) {
                        final double denom = knots[i + degree + 1] - knots[i + 1];
                        if (denom != 0) d -= lower[r] / denom;
                    }
                }
                dN[r] = degree * d;
            }
        }
    }

    /**
     * Returns the point of the curve at parameter {@code u} in span {@code k}, with all the attributes
     * of the control points. Attributes interpolated linearly are blended with the same rational basis
     * functions as the position. Otherwise the value is the one of the control point having the largest
     * weight, or of the first control point of the span for leading values.
     */
    private Point pointAt(final double u, final int k) {
        final double[] R = new double[degree + 1];
        basis(u, k, R, null);
        double W = 0;
        int nearest = 0;
        for (int r = 0; r <= degree; r++) {
            R[r] *= weights[k - degree + r];
            W += R[r];
        }
        for (int r = 0; r <= degree; r++) {
            R[r] /= W;
            if (R[r] > R[nearest]) nearest = r;
        }
        final int first = k - degree;
        final int leading = (u >= knots[last + 1]) ? last : first;
        final int dominant = first + nearest;
        final DataPointsType type = controls.getType();
        final Point point = GeometryFactory.DEFAULT.createPoint(type);
        for (final String name : type.getAttributeNames()) {
            final Vector<?> value;
            final AttributeInterpolation method = type.getAttributeInterpolation(name);
            switch (method) {
                case LINEAR:
                case LINEAR_NORMALIZED:
                case TRANSLATE: {
                    value = controls.getAttribute(first, name).scale(R[0]);
                    for (int r = 1; r <= degree; r++) {
                        value.add(controls.getAttribute(first + r, name).scale(R[r]));
                    }
                    if (method == AttributeInterpolation.LINEAR_NORMALIZED) {
                        value.normalize();
                    }
                    break;
                }
                case LEADING: value = controls.getAttribute(leading, name);  break;
                default:      value = controls.getAttribute(dominant, name); break;
            }
            point.setAttribute(name, value);
        }
        return point;
    }
}
