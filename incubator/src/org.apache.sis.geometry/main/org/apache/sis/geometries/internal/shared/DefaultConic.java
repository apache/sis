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
import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.curve.Conic;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * A chain of conic section arcs, each of them determined by five data points and drawn in the
 * tangent plane at one control point.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultConic extends AbstractGeometry implements Conic {

    /**
     * Points lying on the conic, four of them being consumed by each arc plus the one shared with
     * the previous arc.
     */
    protected final DataPoints points;

    /**
     * Centers of the exponential maps in which the arcs are constructed, one per arc.
     * May be {@code null} if this conic declares no control point.
     */
    protected final Array controlPoints;

    /**
     * Whether this conic closes on itself.
     */
    protected final boolean cycle;

    /**
     * Creates a conic from the given data points and control points.
     *
     * @param  points         points lying on the conic. At least five of them, the first one also
     *                        acting as the last one when {@code cycle} is {@code true}.
     * @param  controlPoints  centers of the exponential maps of the arcs, or {@code null} if none.
     * @param  cycle          whether this conic closes on itself.
     */
    public DefaultConic(final DataPoints points, final Array controlPoints, final boolean cycle) {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = controlPoints;
        this.cycle         = cycle;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Array getControlPoints() {
        return controlPoints;
    }

    @Override
    public boolean isCycle() {
        return cycle;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(crs);
    }

    @Override
    public DataPointsType getDataPointsType() {
        return points.getType();
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    /**
     * Returns the smallest envelope containing this conic. That envelope contains the data points,
     * plus each point where an arc reaches an extreme along an axis. Each arc is computed in the plane
     * of its five data points, with the coordinates handled as Cartesian ones: the exponential map at
     * the control points is the identity.
     *
     * @throws IllegalStateException if the data points cannot be split in arcs of five points,
     *         or if the points of an arc are not coplanar, do not determine a single non-degenerate conic,
     *         are not in their order along the conic, or are joined through a point at infinity.
     */
    @Override
    public Envelope getEnvelope() {
        final var envelope = new GeneralEnvelope(points.getEnvelope());
        if (points.isEmpty()) {
            return envelope;
        }
        final var positions = new ArrayList<Vector<?>>();
        for (int i = 0; i < points.size(); i++) {
            positions.add(points.getPosition(i).copy(DataType.DOUBLE));
        }
        if (cycle && positions.size() % 4 == 0) {
            positions.add(positions.get(0));        // The first data point closes the curve.
        }
        final int n = positions.size();
        if (n < 5 || (n - 1) % 4 != 0) {
            throw new IllegalStateException("A conic is made of arcs of five data points, consecutive arcs"
                    + " sharing a point, so it needs 4k+1 data points, but got " + n + '.');
        }
        for (int i = 0; i + 4 < n; i += 4) {
            addArc(envelope, positions.subList(i, i + 5).toArray(Vector<?>[]::new));
        }
        return envelope;
    }

    /**
     * Expands the given envelope for containing the extremes along each axis of the conic arc
     * going through the five given points. The envelope shall already contain those points.
     */
    private static void addArc(final GeneralEnvelope envelope, final Vector<?>[] p) {
        final int dimension = p[0].getDimension();
        /*
         * Builds an orthonormal basis (e1, e2) of the plane of the points, with the centroid as origin.
         * The coordinates in that plane are divided by the distance of the farthest point from the origin,
         * for having values close to 1 in the computation of the conic coefficients.
         */
        final Vector<?> origin = Vectors.createDouble(dimension);
        for (final Vector<?> q : p) {
            origin.add(q);
        }
        origin.scale(1.0 / p.length);
        Vector<?> e1 = null, e2 = null;
        double scale = 0, width = 0;
        for (final Vector<?> q : p) {
            final Vector<?> v = q.copy(DataType.DOUBLE).subtract(origin);
            final double length = v.length();
            if (length > scale) {
                scale = length;
                e1 = v;
            }
        }
        if (scale == 0) {
            return;                                 // All points are identical.
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
            return;                                 // Collinear points: a straight segment.
        }
        e2.scale(1 / width);
        final double[] a = new double[p.length];            // Coordinates of the points in the plane.
        final double[] b = new double[p.length];
        for (int k = 0; k < p.length; k++) {
            final Vector<?> v = p[k].copy(DataType.DOUBLE).subtract(origin);
            a[k] = v.dot(e1) / scale;
            b[k] = v.dot(e2) / scale;
            if (v.perpendicular(e1).perpendicular(e2).length() > scale * 1E-10) {
                throw new IllegalStateException("The five data points of a conic arc are not coplanar.");
            }
        }
        /*
         * Coefficients of the conic A·a² + B·ab + C·b² + D·a + E·b + F = 0 through the five points,
         * as the cofactors of the 5×6 matrix whose rows are (a², ab, b², a, b, 1) for each point.
         */
        final Vector<?> coef = Vectors.createDouble(6);
        for (int j = 0; j < 6; j++) {
            final double[][] minor = new double[5][];
            for (int k = 0; k < 5; k++) {
                final double[] row = {a[k]*a[k], a[k]*b[k], b[k]*b[k], a[k], b[k], 1};
                final double[] m = new double[5];
                for (int c = 0, d = 0; c < 6; c++) {
                    if (c != j) m[d++] = row[c];
                }
                minor[k] = m;
            }
            coef.set(j, ((j & 1) == 0 ? 1 : -1) * determinant(minor));
        }
        if (!(coef.length() > 1E-12)) {
            throw new IllegalStateException("The five data points of a conic arc do not determine a single conic.");
        }
        coef.normalize();
        final double A = coef.get(0), B = coef.get(1), C = coef.get(2), D = coef.get(3), E = coef.get(4), F = coef.get(5);
        if (Math.abs(determinant(new double[][] {{A, B/2, D/2}, {B/2, C, E/2}, {D/2, E/2, F}})) < 1E-12) {
            throw new IllegalStateException("The five data points of a conic arc are on a degenerated conic,"
                    + " made of straight lines, because three of them are collinear.");
        }
        /*
         * The lines through the start point p₀ map the conic to the directions modulo π, continuously and
         * one-to-one, p₀ itself being mapped to the direction of the tangent at p₀. The arc is therefore the
         * interval of directions going from that tangent to the direction of p₄ through the other points.
         */
        final double a0 = a[0], b0 = b[0];
        final double tangent = direction(-(B*a0 + 2*C*b0 + E), 2*A*a0 + B*b0 + D);
        final double[] r = new double[5];
        for (int k = 1; k < 5; k++) {
            r[k] = relative(direction(a[k] - a0, b[k] - b0), tangent);
        }
        final boolean increasing = r[2] < r[4];
        for (int k = 1; k < 4; k++) {
            if (increasing ? !(r[k] < r[k+1]) : !(r[k] > r[k+1])) {
                throw new IllegalStateException("The five data points of a conic arc are not in their order along the conic.");
            }
        }
        final double end = r[4];
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
                    final double rx = relative(direction(Math.cos(twice / 2), Math.sin(twice / 2)), tangent);
                    if (increasing ? rx <= end : rx >= end) {
                        throw new IllegalStateException("A conic arc goes through a point at infinity,"
                                + " from one branch of a hyperbola to the other.");
                    }
                }
            }
        }
        /*
         * Along axis i, the coordinate is a linear function α·a + β·b of the coordinates in the plane.
         * Its extremes on the conic are where the gradient (Qa, Qb) is parallel to (α, β), which is the
         * line β·Qa − α·Qb = 0, written g₁·a + g₂·b + g₃ = 0. Intersecting that line with the conic gives
         * up to two points, which are kept if they are on the arc.
         */
        for (int i = 0; i < dimension; i++) {
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
                final double rx = relative(direction(xa - a0, xb - b0), tangent);
                if (increasing ? rx <= end : rx >= end) {
                    final Vector<?> position = origin.copy(DataType.DOUBLE)
                            .add(e1.copy(DataType.DOUBLE).scale(scale * xa))
                            .add(e2.copy(DataType.DOUBLE).scale(scale * xb));
                    envelope.add(Vectors.asDirectPostion(position));
                }
            }
        }
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
