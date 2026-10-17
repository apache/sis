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

import org.apache.sis.geometries.curve.ConicArc;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * A conic arc going through its data points, computed in the plane of those points. This is one arc of the chain
 * of arcs of a {@link DefaultConic}, the coordinates being handled as Cartesian ones.
 *
 * <p>A general conic arc goes through five points. An elliptic arc goes through four points, since its equation
 * has no cross term in the basis of its plane whose first axis is the projection of the first coordinate system
 * axis which is not perpendicular to that plane.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DefaultConicArc implements ConicArc {
    /**
     * The data points of the arc: five for a general conic, four for an elliptic arc.
     */
    private final Vector<?>[] p;

    /**
     * Whether the points are collinear or identical, in which case the arc is the polyline joining them
     * and the other fields are not set.
     */
    private final boolean linear;

    /**
     * Orthonormal basis (e1, e2) of the plane of the points, with the centroid as origin. The coordinates
     * in that plane are divided by the scale, which is the distance of the farthest point from the origin.
     */
    private Vector<?> origin, e1, e2;
    private double scale;

    /**
     * Coefficients of the conic A·a² + B·ab + C·b² + D·a + E·b + F = 0 in the plane.
     */
    private double A, B, C, D, E, F;

    /**
     * Coordinates in the plane of the start point p₀.
     */
    private double a0, b0;

    /**
     * Direction of the tangent at p₀, and directions from p₀ to the data points relative to the tangent.
     */
    private double tangent;
    private final double[] r;

    /**
     * Whether the directions from p₀ to the next data points increase.
     */
    private boolean increasing;

    /**
     * Computes the conic through the given points.
     *
     * @param  p         the data points of the arc, in their order along the arc.
     * @param  elliptic  {@code true} for an elliptic arc of four points, {@code false} for a conic arc of five points.
     * @throws IllegalStateException if the points are not coplanar, do not determine a single non-degenerate
     *         conic (or an ellipse), are not in their order along the conic, or are joined through a point at infinity.
     */
    DefaultConicArc(final Vector<?>[] p, final boolean elliptic) {
        final int[] terms = elliptic ? new int[] {0, 2, 3, 4, 5} : new int[] {0, 1, 2, 3, 4, 5};
        if (p.length != terms.length - 1) {
            throw new IllegalArgumentException("Expected " + (terms.length - 1) + " points, but got " + p.length + '.');
        }
        final String what = elliptic ? "The four data points of an elliptic arc" : "The five data points of a conic arc";
        this.p = p;
        this.r = new double[p.length];
        final int dimension = p[0].getDimension();
        origin = Vectors.createDouble(dimension);
        for (final Vector<?> q : p) {
            origin.add(q);
        }
        origin.scale(1.0 / p.length);
        double width = 0;
        for (final Vector<?> q : p) {
            final Vector<?> v = q.copy(DataType.DOUBLE).subtract(origin);
            final double length = v.length();
            if (length > scale) {
                scale = length;
                e1 = v;
            }
        }
        if (scale == 0) {
            linear = true;                      // All points are identical.
            return;
        }
        e1.scale(1 / scale);
        for (final Vector<?> q : p) {
            final Vector<?> v = q.copy(DataType.DOUBLE).subtract(origin).perpendicular(e1);
            final double length = v.length();
            if (length > width) {
                width = length;
                e2 = v;
            }
        }
        if (!(width > scale * 1E-10)) {
            linear = true;                      // Collinear points: a straight segment.
            return;
        }
        linear = false;
        e2.scale(1 / width);
        if (elliptic) {
            /*
             * The axes of the ellipse are along the projection on the plane of the first coordinate system axis
             * which is not perpendicular to the plane, and along the perpendicular direction in the plane.
             */
            for (int i = 0; i < dimension; i++) {
                final double c1 = e1.get(i), c2 = e2.get(i);
                final double h = Math.hypot(c1, c2);
                if (h > 1E-10) {
                    final Vector<?> u1 = e1.copy(DataType.DOUBLE).scale( c1 / h).add(e2.copy(DataType.DOUBLE).scale(c2 / h));
                    final Vector<?> u2 = e1.copy(DataType.DOUBLE).scale(-c2 / h).add(e2.copy(DataType.DOUBLE).scale(c1 / h));
                    e1 = u1;
                    e2 = u2;
                    break;
                }
            }
        }
        final double[] a = new double[p.length];            // Coordinates of the points in the plane.
        final double[] b = new double[p.length];
        for (int k = 0; k < p.length; k++) {
            final Vector<?> v = p[k].copy(DataType.DOUBLE).subtract(origin);
            a[k] = v.dot(e1) / scale;
            b[k] = v.dot(e2) / scale;
            if (v.perpendicular(e1).perpendicular(e2).length() > scale * 1E-10) {
                throw new IllegalStateException(what + " are not coplanar.");
            }
        }
        /*
         * Coefficients of the conic through the points, as the cofactors of the matrix whose rows are the terms
         * (a², ab, b², a, b, 1) for each point. The ab term is omitted for an elliptic arc.
         */
        final int m = terms.length;
        final Vector<?> solution = Vectors.createDouble(m);
        for (int j = 0; j < m; j++) {
            final double[][] minor = new double[m - 1][];
            for (int k = 0; k < m - 1; k++) {
                final double[] row = {a[k]*a[k], a[k]*b[k], b[k]*b[k], a[k], b[k], 1};
                final double[] values = new double[m - 1];
                for (int c = 0, d = 0; c < m; c++) {
                    if (c != j) values[d++] = row[terms[c]];
                }
                minor[k] = values;
            }
            solution.set(j, ((j & 1) == 0 ? 1 : -1) * determinant(minor));
        }
        if (!(solution.length() > 1E-12)) {
            throw new IllegalStateException(what + " do not determine a single conic." + (elliptic
                    ? " Two pairs of points having the same coordinate along an axis of the ellipse may be the cause." : ""));
        }
        solution.normalize();
        final double[] coef = new double[6];
        for (int c = 0; c < m; c++) {
            coef[terms[c]] = solution.get(c);
        }
        A = coef[0]; B = coef[1]; C = coef[2]; D = coef[3]; E = coef[4]; F = coef[5];
        if (Math.abs(determinant(new double[][] {{A, B/2, D/2}, {B/2, C, E/2}, {D/2, E/2, F}})) < 1E-12) {
            throw new IllegalStateException(what + " are on a degenerated conic,"
                    + " made of straight lines, because three of them are collinear.");
        }
        if (elliptic && !(A * C > 0)) {
            throw new IllegalStateException(what + " are not on an ellipse whose axes are aligned with its plane basis.");
        }
        /*
         * The lines through the start point p₀ map the conic to the directions modulo π, continuously and
         * one-to-one, p₀ itself being mapped to the direction of the tangent at p₀. The arc is therefore the
         * interval of directions going from that tangent to the direction of the last point through the other points.
         */
        final int last = p.length - 1;
        a0 = a[0];
        b0 = b[0];
        tangent = direction(-(B*a0 + 2*C*b0 + E), 2*A*a0 + B*b0 + D);
        for (int k = 1; k <= last; k++) {
            r[k] = relative(direction(a[k] - a0, b[k] - b0), tangent);
        }
        increasing = r[1] < r[last];
        for (int k = 1; k < last; k++) {
            if (increasing ? !(r[k] < r[k+1]) : !(r[k] > r[k+1])) {
                throw new IllegalStateException(what + " are not in their order along the conic.");
            }
        }
        /*
         * Points at infinity, in the directions where A·cos²θ + B·cosθ·sinθ + C·sin²θ = 0,
         * which is (A+C)/2 + R·cos(2θ − δ) = 0 with R and δ computed below.
         */
        final double R = Math.hypot((A - C) / 2, B / 2);
        if (R > 0) {
            final double cos = -(A + C) / (2 * R);
            if (Math.abs(cos) <= 1 + 1E-9) {
                final double delta = Math.atan2(B / 2, (A - C) / 2);
                final double acos  = Math.acos(Math.max(-1, Math.min(1, cos)));
                for (final double twice : new double[] {delta + acos, delta - acos}) {
                    if (onArc(relative(direction(Math.cos(twice / 2), Math.sin(twice / 2)), tangent))) {
                        throw new IllegalStateException("A conic arc goes through a point at infinity,"
                                + " from one branch of a hyperbola to the other.");
                    }
                }
            }
        }
    }

    /**
     * Returns whether the given direction from p₀, relative to the tangent, is the direction of a point of the arc.
     */
    private boolean onArc(final double rx) {
        final double end = r[r.length - 1];
        return increasing ? rx <= end : rx >= end;
    }

    /**
     * Returns the position of the given coordinates in the plane.
     */
    private Vector<?> position(final double xa, final double xb) {
        return origin.copy(DataType.DOUBLE)
                .add(e1.copy(DataType.DOUBLE).scale(scale * xa))
                .add(e2.copy(DataType.DOUBLE).scale(scale * xb));
    }

    /**
     * Expands the given envelope for containing the extremes of this arc along each axis.
     * The envelope shall already contain the data points.
     */
    void addExtremes(final NDRange envelope) {
        if (linear) {
            return;
        }
        /*
         * Along axis i, the coordinate is a linear function α·a + β·b of the coordinates in the plane.
         * Its extremes on the conic are where the gradient (Qa, Qb) is parallel to (α, β), which is the
         * line β·Qa − α·Qb = 0, written g₁·a + g₂·b + g₃ = 0. Intersecting that line with the conic gives
         * up to two points, which are kept if they are on the arc.
         */
        for (int i = 0; i < origin.getDimension(); i++) {
            final double alpha = e1.get(i), beta = e2.get(i);
            final double g1 = 2*A*beta - B*alpha;
            final double g2 = B*beta - 2*C*alpha;
            final double g3 = D*beta - E*alpha;
            final double gg = g1*g1 + g2*g2;
            if (!(gg > 0)) {
                continue;
            }
            final double pa = -g3 * g1 / gg, pb = -g3 * g2 / gg;      // A point on the line.
            final double da = -g2, db = g1;                             // Direction of the line.
            final double q2 = A*da*da + B*da*db + C*db*db;
            final double q1 = (2*A*pa + B*pb + D) * da + (B*pa + 2*C*pb + E) * db;
            final double q0 = A*pa*pa + B*pa*pb + C*pb*pb + D*pa + E*pb + F;
            for (final double s : roots(q2, q1, q0)) {
                final double xa = pa + s * da, xb = pb + s * db;
                if (!Double.isFinite(xa) || !Double.isFinite(xb) || Math.hypot(xa - a0, xb - b0) < 1E-12) {
                    continue;
                }
                if (onArc(relative(direction(xa - a0, xb - b0), tangent))) {
                    envelope.add(position(xa, xb));
                }
            }
        }
    }

    /**
     * Returns this arc as a piece of curve. The parameter is the angle between the tangent at p₀ and the
     * direction from p₀ to the position, which is the second intersection of the conic with the line through p₀
     * in that direction. If the points are collinear, the parameter is the index of the data points.
     */
    CurveLinearizer.Piece piece() {
        final double[] t = new double[p.length];
        if (linear) {
            for (int k = 0; k < t.length; k++) {
                t[k] = k;
            }
            return new CurveLinearizer.Piece((index) -> {
                final int i = Math.max(0, Math.min(p.length - 2, (int) Math.floor(index)));
                return p[i].copy(DataType.DOUBLE).lerp(p[i + 1], index - i);
            }, t, p);
        }
        for (int k = 1; k < t.length; k++) {
            t[k] = increasing ? r[k] : Math.PI - r[k];
        }
        return new CurveLinearizer.Piece((angle) -> {
            // Q(p₀ + s·d) = s·(∇Q(p₀)·d) + s²·q(d) = 0 since p₀ is on the conic.
            final double phi = tangent + (increasing ? angle : -angle);
            final double da = Math.cos(phi), db = Math.sin(phi);
            final double q = A*da*da + B*da*db + C*db*db;
            final double g = (2*A*a0 + B*b0 + D) * da + (B*a0 + 2*C*b0 + E) * db;
            final double s = (q != 0) ? -g / q : 0;
            return position(a0 + s * da, b0 + s * db);
        }, t, p);
    }

    /**
     * Returns the real roots of q₂·s² + q₁·s + q₀ = 0.
     */
    private static double[] roots(final double q2, final double q1, final double q0) {
        if (q2 == 0) {
            return (q1 != 0) ? new double[] {-q0 / q1} : new double[0];
        }
        final double discriminant = q1*q1 - 4*q2*q0;
        if (discriminant < 0) {
            return new double[0];
        }
        final double q = -(q1 + Math.copySign(Math.sqrt(discriminant), q1)) / 2;
        return (q != 0) ? new double[] {q / q2, q0 / q} : new double[] {0};
    }

    /**
     * Returns the angle of the given direction, modulo π, in the [0 … π) range.
     */
    private static double direction(final double x, final double y) {
        double angle = Math.atan2(y, x);
        if (angle < 0) angle += Math.PI;
        if (angle >= Math.PI) angle -= Math.PI;
        return angle;
    }

    /**
     * Returns the given direction relative to the reference one, in the [0 … π) range.
     */
    private static double relative(final double angle, final double reference) {
        final double r = angle - reference;
        return (r < 0) ? r + Math.PI : r;
    }

    /**
     * Returns the determinant of the given square matrix, which is overwritten.
     */
    private static double determinant(final double[][] m) {
        final int n = m.length;
        double det = 1;
        for (int c = 0; c < n; c++) {
            int pivot = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(m[r][c]) > Math.abs(m[pivot][c])) pivot = r;
            }
            if (m[pivot][c] == 0) {
                return 0;
            }
            if (pivot != c) {
                final double[] t = m[pivot]; m[pivot] = m[c]; m[c] = t;
                det = -det;
            }
            det *= m[c][c];
            for (int r = c + 1; r < n; r++) {
                final double f = m[r][c] / m[c][c];
                for (int k = c; k < n; k++) {
                    m[r][k] -= f * m[c][k];
                }
            }
        }
        return det;
    }
}
