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

import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Units;


/**
 * Helper methods for computations on curves, such as locating positions at a distance along a curve
 * or measuring the area enclosed by a ring.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Curves {

    private Curves() {
    }

    /**
     * Returns the area enclosed by the given ring of positions.
     *
     * <ul>
     *   <li>In 2 dimensions, the area is computed with the shoelace formula.</li>
     *   <li>In 3 dimensions or more, the area is computed on the first three coordinates with Newell's
     *       method: half the norm of the sum of the cross products of consecutive positions. This is
     *       exact for a ring lying in a plane, in any orientation of that plane, and gives the area
     *       projected on the best-fitting plane otherwise.</li>
     * </ul>
     *
     * <p>The ring may repeat its first position at the end or not. The positions are taken relative
     * to the first one, which avoids losing precision with large coordinate values.</p>
     *
     * @param  ring  positions of the ring, in order.
     * @return the area enclosed by the ring, in the square of the unit of the coordinates.
     */
    public static double ringArea(final DataPoints ring) {
        final int n = ring.size();
        final int dim = ring.getDimension();
        if (n < 3 || dim < 2) {
            return 0;
        }
        final Vector<?> origin = ring.getPosition(0);
        if (dim == 2) {
            final double ox = origin.get(0);
            final double oy = origin.get(1);
            Vector<?> p = ring.getPosition(n - 1);
            double ax = p.get(0) - ox;
            double ay = p.get(1) - oy;
            double sum = 0;
            for (int i = 0; i < n; i++) {
                p = ring.getPosition(i);
                final double bx = p.get(0) - ox;
                final double by = p.get(1) - oy;
                sum += ax*by - ay*bx;
                ax = bx;
                ay = by;
            }
            return Math.abs(sum) / 2;
        } else {
            final double ox = origin.get(0);
            final double oy = origin.get(1);
            final double oz = origin.get(2);
            Vector<?> p = ring.getPosition(n - 1);
            double ax = p.get(0) - ox;
            double ay = p.get(1) - oy;
            double az = p.get(2) - oz;
            double nx = 0, ny = 0, nz = 0;
            for (int i = 0; i < n; i++) {
                p = ring.getPosition(i);
                final double bx = p.get(0) - ox;
                final double by = p.get(1) - oy;
                final double bz = p.get(2) - oz;
                nx += ay*bz - az*by;
                ny += az*bx - ax*bz;
                nz += ax*by - ay*bx;
                ax = bx;
                ay = by;
                az = bz;
            }
            return Math.sqrt(nx*nx + ny*ny + nz*nz) / 2;
        }
    }

    /**
     * Expands the given envelope for containing the circular arc going from {@code start} to {@code end}
     * through {@code middle}. The envelope shall already contain the start and end points. This method
     * adds each point where the circle reaches an extreme along an axis, if that point is on the arc.
     * It works in any number of dimensions, the arc being in the plane of its three points.
     *
     * <p>If the three points are collinear, the arc is a straight segment and the envelope is unchanged.
     * If the start and end points are identical, the arc is a full circle whose diameter goes from them
     * to the middle point.</p>
     *
     * @param  envelope  the envelope to expand, containing the start and end points.
     * @param  start     the start point.
     * @param  middle    a point on the arc, distinct from its start and end points.
     * @param  end       the end point.
     * @throws IllegalStateException if the arc is a full circle in more than two dimensions,
     *         since its plane is then not determined.
     */
    public static void addArc(final NDRange envelope, final ReadOnly.Vector<?> start,
                              final ReadOnly.Vector<?> middle, final ReadOnly.Vector<?> end)
    {
        final int dimension = start.getDimension();
        final Vector<?> mid = start .copy(DataType.DOUBLE).lerp(end, 0.5);      // Middle of the chord.
        final Vector<?> u   = end   .copy(DataType.DOUBLE).subtract(start);     // Unit vector along the chord.
        final Vector<?> n   = middle.copy(DataType.DOUBLE).subtract(mid);       // Unit vector perpendicular to the chord, toward the arc.
        final double chord = u.length();
        if (chord == 0) {
            if (dimension != 2) {
                throw new IllegalStateException("A circular arc with identical start and end points"
                        + " is a full circle, which plane is not determined in " + dimension + " dimensions.");
            }
            final double r = n.length() / 2;
            final Vector<?> centre = start.copy(DataType.DOUBLE).lerp(middle, 0.5);
            for (int i = 0; i < dimension; i++) {
                extend(envelope, i, centre.get(i) - r);
                extend(envelope, i, centre.get(i) + r);
            }
            return;
        }
        u.scale(1 / chord);
        final double pu = n.dot(u);
        n.perpendicular(u);
        final double pn = n.length();
        if (!(pn > chord * 1E-12)) {
            return;                                     // Collinear points: a straight segment.
        }
        n.scale(1 / pn);
        /*
         * With the half chord c, the centre is at t·n from the middle of the chord, where t makes the
         * centre equidistant from the start point and the middle point: c² + t² = pu² + (t − pn)².
         * In the plane (u, n), the circle reaches its extremes along axis i in the directions
         * ±(uᵢ·u + nᵢ·n), at distances ±r·hypot(uᵢ, nᵢ) from the centre along that axis.
         * The arc is the part of the circle on the side of n from the chord.
         */
        final double c = chord / 2;
        final double t = (pu*pu + pn*pn - c*c) / (2*pn);
        final double r = Math.hypot(c, t);
        for (int i = 0; i < dimension; i++) {
            final double h = Math.hypot(u.get(i), n.get(i));
            if (h == 0) {
                continue;                               // The plane of the arc is perpendicular to this axis.
            }
            final double centre = mid.get(i) + t * n.get(i);
            for (int sign = -1; sign <= 1; sign += 2) {
                if (t + sign * r * n.get(i) / h >= 0) {
                    extend(envelope, i, centre + sign * r * h);
                }
            }
        }
    }

    /**
     * Expands the range of the given envelope axis for containing the given value.
     */
    private static void extend(final NDRange envelope, final int axis, final double value) {
        envelope.setRange(axis, Math.min(envelope.getMinimum(axis), value),
                                Math.max(envelope.getMaximum(axis), value));
    }

    /**
     * Returns whether the given distance is expressed in percent of the curve length.
     *
     * @param  dist  the distance along a curve.
     * @return whether the distance is a percentage.
     */
    public static boolean isPercent(final Quantity<?> dist) {
        return Units.PERCENT.equals(dist.getUnit());
    }

    /**
     * Converts the given quantity to the given unit.
     *
     * @param  q     the quantity to convert.
     * @param  unit  the target unit.
     * @return the value of the quantity in the target unit.
     * @throws IllegalArgumentException if the quantity cannot be expressed in the target unit.
     */
    public static double convert(final Quantity<?> q, final Unit<?> unit) {
        try {
            return q.getUnit().getConverterToAny(unit).convert(q.getValue().doubleValue());
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException("Cannot express " + q + " in " + unit + '.', e);
        }
    }

    /**
     * Converts a distance along a curve to an arc length in the unit of the curve length.
     * A distance in {@linkplain Units#PERCENT percent} is relative to the given length.
     *
     * @param  dist    the distance along the curve.
     * @param  length  the length of the curve, in {@code unit}.
     * @param  unit    the unit of the curve length.
     * @return the arc length, between 0 and {@code length} inclusive.
     * @throws IllegalArgumentException if the distance cannot be expressed in the length unit,
     *         or is outside the curve.
     */
    public static double toArcLength(final Quantity<?> dist, final double length, final Unit<?> unit) {
        double s = isPercent(dist) ? dist.getValue().doubleValue() / 100 * length : convert(dist, unit);
        // Rounding errors of unit conversions and of length sums are not a distance outside the curve.
        final double tolerance = length * 1E-12;
        if (s < 0 && s >= -tolerance) {
            s = 0;
        } else if (s > length && s <= length + tolerance) {
            s = length;
        }
        if (!(s >= 0 && s <= length)) {
            throw new IllegalArgumentException("The distance " + dist + " is outside the curve, whose length is "
                    + length + ' ' + unit + '.');
        }
        return s;
    }

    /**
     * Returns the point at the given distance along the line segments joining consecutive points.
     *
     * @param  points  the points of the polyline.
     * @param  dist    the distance along the polyline, in {@code unit} or in percent.
     * @param  unit    the unit of the coordinates.
     * @return the point at the given distance, or {@code null} if there are no points.
     * @throws IllegalArgumentException if the distance is outside the polyline.
     */
    public static Point alongPolyline(final DataPoints points, final Quantity<?> dist, final Unit<?> unit) {
        final int n = points.size();
        if (n == 0) {
            return null;
        }
        final double[] segments = new double[n - 1];
        double total = 0;
        for (int i=0; i<segments.length; i++) {
            segments[i] = points.getPosition(i).subtract(points.getPosition(i+1)).length();
            total += segments[i];
        }
        double s = toArcLength(dist, total, unit);
        for (int i=0; i<segments.length; i++) {
            final double length = segments[i];
            if (s <= length || i == segments.length - 1) {
                final double ratio = (length > 0) ? Math.min(s / length, 1) : 0;
                return points.getInterpolatedPoint(i, i+1, ratio);
            }
            s -= length;
        }
        return points.getInterpolatedPoint(0, 0, 0);    // A single position, at distance zero.
    }
}
