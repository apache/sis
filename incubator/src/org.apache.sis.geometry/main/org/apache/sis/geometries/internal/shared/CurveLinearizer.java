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
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleFunction;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * Approximates curves by polylines whose points lie on the curves, for implementing
 * {@link Curve#asLine(Quantity, Quantity)}.
 *
 * <p>A curve is described by one or more {@linkplain Piece pieces}, each one being a function from a parameter
 * to a position, with the parameters of the positions which must be in the polyline (the start and end of the
 * piece, the data points, the knots). Each interval between two such parameters is halved until both criteria
 * of {@code asLine} are met:</p>
 * <ul>
 *   <li>the arc length of each interval is at most the spacing. It is estimated from the lengths of the chords
 *       joining 2 and 4 sub-intervals, extrapolated to an infinite number of chords;</li>
 *   <li>the distance between the curve and the chord of each interval is at most the offset.
 *       It is estimated from the curve positions at one quarter, half and three quarters of the interval.</li>
 * </ul>
 *
 * <p>If both the spacing and the offset are zero, the offset is a fraction of the extent of the curve.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CurveLinearizer {
    /**
     * Offset used when no criterion is given, relative to the extent of the curve.
     */
    private static final double DEFAULT_OFFSET = 1E-3;

    /**
     * Offset used for estimating the length of the curve, relative to its extent.
     * The length is needed when the spacing or the offset is given in percent of the length.
     */
    private static final double LENGTH_OFFSET = 1E-6;

    /**
     * Maximal number of interval halvings, which stops the refinement at the precision of the parameter.
     */
    private static final int MAX_DEPTH = 50;

    /**
     * A part of a curve, as a function from a parameter to a position.
     *
     * @param function    the position on the curve for a given parameter.
     * @param parameters  the parameters of the positions which shall be in the polyline, in their order along
     *                    the curve. The first and last ones are the start and end of the piece. The values may
     *                    be increasing or decreasing.
     * @param positions   the exact positions at the given parameters, or {@code null} for computing all of them
     *                    with the function. A {@code null} element is also computed with the function.
     */
    public record Piece(DoubleFunction<? extends ReadOnly.Vector<?>> function, double[] parameters,
                        ReadOnly.Vector<?>[] positions)
    {
        /**
         * Returns the position at the parameter of the given index.
         */
        ReadOnly.Vector<?> position(final int i) {
            if (positions != null && positions[i] != null) {
                return positions[i];
            }
            return function.apply(parameters[i]);
        }
    }

    /**
     * Maximal arc length and offset of the intervals, in the unit of the coordinates. Zero means no limit.
     */
    private final double spacing, offset;

    /**
     * The points of the polyline, in order.
     */
    private final List<ReadOnly.Vector<?>> points = new ArrayList<>();

    private CurveLinearizer(final double spacing, final double offset) {
        this.spacing = spacing;
        this.offset  = offset;
    }

    /**
     * Returns a polyline approximating a curve made of the given pieces, consecutive pieces sharing their end point.
     *
     * @param  curve    the curve to approximate, for its reference system and the unit of its coordinates.
     * @param  spacing  maximal distance along the curve between two points of the polyline, or zero or {@code null}
     *                  for no limit. May be in percent of the curve length.
     * @param  offset   maximal distance between the curve and the polyline, or zero or {@code null} for no limit.
     *                  May be in percent of the curve length.
     * @param  pieces   the pieces of the curve.
     * @return the polyline approximating the curve.
     * @throws IllegalArgumentException if the spacing or the offset is negative, or cannot be converted to
     *         the unit of the coordinates.
     */
    public static LineString linearize(final Curve curve, final Quantity<?> spacing, final Quantity<?> offset,
                                       final List<Piece> pieces)
    {
        final Unit<?> unit = Geometries.getLinearUnit(curve);
        double length = Double.NaN;
        if (isPercent(spacing) || isPercent(offset)) {
            length = length(run(0, LENGTH_OFFSET * extent(pieces), pieces));
        }
        double s = distance(spacing, unit, length);
        double o = distance(offset,  unit, length);
        if (s == 0 && o == 0) {
            o = DEFAULT_OFFSET * extent(pieces);
        }
        return lineString(curve.getCoordinateReferenceSystem(), run(s, o, pieces));
    }

    /**
     * Computes the points of the polyline approximating the given pieces.
     */
    private static List<ReadOnly.Vector<?>> run(final double spacing, final double offset, final List<Piece> pieces) {
        final var linearizer = new CurveLinearizer(spacing, offset);
        for (final Piece piece : pieces) {
            final double[] t = piece.parameters();
            ReadOnly.Vector<?> previous = piece.position(0);
            if (linearizer.points.isEmpty()) {
                linearizer.points.add(previous);
            }
            for (int i = 1; i < t.length; i++) {
                final ReadOnly.Vector<?> next = piece.position(i);
                linearizer.refine(piece.function(), t[i-1], previous, t[i], next, 0);
                previous = next;
            }
        }
        return linearizer.points;
    }

    /**
     * Adds the points of the polyline between the given positions, excluding the first one.
     */
    private void refine(final DoubleFunction<? extends ReadOnly.Vector<?>> function,
                        final double ta, final ReadOnly.Vector<?> pa,
                        final double tb, final ReadOnly.Vector<?> pb, final int depth)
    {
        final double tm = (ta + tb) / 2;
        if (depth < MAX_DEPTH && tm != ta && tm != tb) {
            final ReadOnly.Vector<?> pm = function.apply(tm);
            final ReadOnly.Vector<?> q1 = function.apply((ta + tm) / 2);
            final ReadOnly.Vector<?> q3 = function.apply((tm + tb) / 2);
            boolean split = false;
            if (spacing > 0) {
                final double half    = distance(pa, pm) + distance(pm, pb);
                final double quarter = distance(pa, q1) + distance(q1, pm) + distance(pm, q3) + distance(q3, pb);
                split = quarter + (quarter - half) / 3 > spacing;
            }
            if (!split && offset > 0) {
                split = Math.max(distance(pm, pa, pb), Math.max(distance(q1, pa, pb), distance(q3, pa, pb))) > offset;
            }
            if (split) {
                refine(function, ta, pa, tm, pm, depth + 1);
                refine(function, tm, pm, tb, pb, depth + 1);
                return;
            }
        }
        points.add(pb);
    }

    /**
     * Returns the circular arc going from {@code start} to {@code end} through {@code middle}, as a function of the
     * angle in radians from the start point. The arc is in the plane of its three points, so it works in any number
     * of dimensions. If the three points are collinear, the piece is the straight segment from start to end.
     * If the start and end points are identical, the piece is a full circle whose diameter goes from them to
     * the middle point, turning counterclockwise.
     *
     * @param  start   the start point.
     * @param  middle  a point on the arc, distinct from its start and end points.
     * @param  end     the end point.
     * @param  keep    whether the middle point is a data point which shall be in the polyline.
     * @return the arc as a piece of curve.
     * @throws IllegalStateException if the arc is a full circle in more than two dimensions,
     *         since its plane is then not determined.
     */
    public static Piece circularArc(final ReadOnly.Vector<?> start, final ReadOnly.Vector<?> middle,
                                    final ReadOnly.Vector<?> end, final boolean keep)
    {
        final int dimension = start.getDimension();
        final Vector<?> u = end.copy(DataType.DOUBLE).subtract(start);
        final double chord = u.length();
        final Vector<?> centre, e1, e2;
        final double radius;
        if (chord == 0) {
            if (dimension != 2) {
                throw new IllegalStateException("A circular arc with identical start and end points"
                        + " is a full circle, which plane is not determined in " + dimension + " dimensions.");
            }
            centre = start.copy(DataType.DOUBLE).lerp(middle, 0.5);
            radius = distance(start, centre);
            e1 = start.copy(DataType.DOUBLE).subtract(centre).scale(1 / radius);
            e2 = e1.copy(DataType.DOUBLE);
            e2.set(0, -e1.get(1));
            e2.set(1,  e1.get(0));
        } else {
            u.scale(1 / chord);
            final Vector<?> mid = start.copy(DataType.DOUBLE).lerp(end, 0.5);
            final Vector<?> n = middle.copy(DataType.DOUBLE).subtract(mid);
            final double pu = n.dot(u);
            n.perpendicular(u);
            final double pn = n.length();
            if (!(pn > chord * 1E-12)) {
                // Collinear points: a straight segment.
                final double tm = middle.copy(DataType.DOUBLE).subtract(start).dot(u) / chord;
                return new Piece((t) -> start.copy(DataType.DOUBLE).lerp(end, t),
                        keep ? new double[] {0, tm, 1} : new double[] {0, 1},
                        keep ? new ReadOnly.Vector<?>[] {start, middle, end} : new ReadOnly.Vector<?>[] {start, end});
            }
            n.scale(1 / pn);
            // Same circle as in `Curves.addArc(…)`.
            final double c = chord / 2;
            final double t = (pu*pu + pn*pn - c*c) / (2*pn);
            radius = Math.hypot(c, t);
            centre = mid.add(n.copy(DataType.DOUBLE).scale(t));
            e1 = start.copy(DataType.DOUBLE).subtract(centre).scale(1 / radius);
            e2 = n.perpendicular(e1);
            e2.normalize();
        }
        /*
         * Angles of the middle and end points in the basis (e1, e2), in the [0 … 2π) range. If the middle point
         * comes first when turning in the direction of positive angles, the arc turns in that direction.
         * Otherwise it turns in the direction of negative angles.
         */
        double am = angle(middle, centre, e1, e2);
        double ae = (chord == 0) ? 2*Math.PI : angle(end, centre, e1, e2);
        if (am > ae) {
            am -= 2*Math.PI;
            ae -= 2*Math.PI;
        }
        final DoubleFunction<Vector<?>> function = (a) -> centre.copy(DataType.DOUBLE)
                .add(e1.copy(DataType.DOUBLE).scale(radius * Math.cos(a)))
                .add(e2.copy(DataType.DOUBLE).scale(radius * Math.sin(a)));
        return new Piece(function,
                keep ? new double[] {0, am, ae} : new double[] {0, ae},
                keep ? new ReadOnly.Vector<?>[] {start, middle, end} : new ReadOnly.Vector<?>[] {start, end});
    }

    /**
     * Returns the angle of the given position around the given centre, in the basis (e1, e2), in the [0 … 2π) range.
     */
    private static double angle(final ReadOnly.Vector<?> p, final Vector<?> centre, final Vector<?> e1, final Vector<?> e2) {
        final Vector<?> v = p.copy(DataType.DOUBLE).subtract(centre);
        final double a = Math.atan2(v.dot(e2), v.dot(e1));
        return (a < 0) ? a + 2*Math.PI : a;
    }

    /**
     * Returns the distance between two positions.
     */
    private static double distance(final ReadOnly.Vector<?> a, final ReadOnly.Vector<?> b) {
        return a.copy(DataType.DOUBLE).subtract(b).length();
    }

    /**
     * Returns the distance between a position and the segment from {@code a} to {@code b}.
     */
    private static double distance(final ReadOnly.Vector<?> p, final ReadOnly.Vector<?> a, final ReadOnly.Vector<?> b) {
        final Vector<?> ab = b.copy(DataType.DOUBLE).subtract(a);
        final Vector<?> ap = p.copy(DataType.DOUBLE).subtract(a);
        final double square = ab.lengthSquare();
        final double t = (square > 0) ? Math.max(0, Math.min(1, ap.dot(ab) / square)) : 0;
        return ap.subtract(ab.scale(t)).length();
    }

    /**
     * Returns the length of the given polyline.
     */
    private static double length(final List<ReadOnly.Vector<?>> points) {
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            length += distance(points.get(i - 1), points.get(i));
        }
        return length;
    }

    /**
     * Returns the diagonal of the box containing the given pieces, estimated from a few positions of each interval.
     */
    private static double extent(final List<Piece> pieces) {
        GeometryEnvelope box = null;
        for (final Piece piece : pieces) {
            final double[] t = piece.parameters();
            for (int i = 0; i < t.length; i++) {
                final int samples = (i == 0) ? 1 : 8;
                for (int k = samples; --k >= 0;) {
                    final ReadOnly.Vector<?> p = (k == 0) ? piece.position(i)
                            : piece.function().apply(t[i] + (t[i-1] - t[i]) * k / samples);
                    if (box == null) {
                        box = new GeometryEnvelope(p, p);
                    } else {
                        box.add(p);
                    }
                }
            }
        }
        return diagonal(box);
    }

    /**
     * Returns whether the given distance is in percent of the curve length.
     */
    private static boolean isPercent(final Quantity<?> q) {
        return q != null && Curves.isPercent(q);
    }

    /**
     * Converts the given spacing or offset to the unit of the coordinates.
     *
     * @param  q       the spacing or the offset, or {@code null}.
     * @param  unit    the unit of the coordinates.
     * @param  length  the length of the curve, used if {@code q} is in percent.
     * @return the distance in the unit of the coordinates, or 0 for no limit.
     */
    private static double distance(final Quantity<?> q, final Unit<?> unit, final double length) {
        if (q == null) {
            return 0;
        }
        final double value = isPercent(q) ? q.getValue().doubleValue() / 100 * length : Curves.convert(q, unit);
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("The spacing and the offset of a linear approximation must be"
                    + " positive or zero, but got " + q + '.');
        }
        return value;
    }

    /**
     * Returns a polyline approximating a curve made of the given parts, each part ending where the next one starts.
     * The polylines of the parts are joined, without repeating the shared points.
     *
     * @param  curve    the curve made of the given parts.
     * @param  parts    the parts of the curve, in order.
     * @param  spacing  maximal distance along the curve between two points of the polyline, or zero or {@code null}
     *                  for no limit. May be in percent of the length of the whole curve.
     * @param  offset   maximal distance between the curve and the polyline, or zero or {@code null} for no limit.
     *                  May be in percent of the length of the whole curve.
     * @return the polyline approximating the curve.
     */
    public static LineString concatenate(final Curve curve, final List<? extends Curve> parts,
                                         Quantity<?> spacing, Quantity<?> offset)
    {
        if (isPercent(spacing) || isPercent(offset)) {
            final Unit<?> unit = Geometries.getLinearUnit(curve);
            double length = 0;
            for (final Curve part : parts) {
                final Quantity<?> precision = Quantities.create(LENGTH_OFFSET * diagonal(part.getEnvelope()), unit);
                length += length(positions(part.asLine(null, precision)));
            }
            spacing = Quantities.create(distance(spacing, unit, length), unit);
            offset  = Quantities.create(distance(offset,  unit, length), unit);
        }
        final var points = new ArrayList<ReadOnly.Vector<?>>();
        for (final Curve part : parts) {
            final List<ReadOnly.Vector<?>> positions = positions(part.asLine(spacing, offset));
            points.addAll(points.isEmpty() ? positions : positions.subList(Math.min(1, positions.size()), positions.size()));
        }
        return lineString(curve.getCoordinateReferenceSystem(), points);
    }

    /**
     * Returns the polyline approximating the given curve, with its points in reverse order.
     *
     * @param  curve    the reversed curve.
     * @param  base     the curve which is reversed.
     * @param  spacing  maximal distance along the curve between two points of the polyline, or zero or {@code null}.
     * @param  offset   maximal distance between the curve and the polyline, or zero or {@code null}.
     * @return the polyline approximating the reversed curve.
     */
    public static LineString reverse(final Curve curve, final Curve base, final Quantity<?> spacing, final Quantity<?> offset) {
        final List<ReadOnly.Vector<?>> points = positions(base.asLine(spacing, offset));
        Collections.reverse(points);
        return lineString(curve.getCoordinateReferenceSystem(), points);
    }

    /**
     * Returns the positions of the given line string.
     */
    private static List<ReadOnly.Vector<?>> positions(final LineString line) {
        final DataPoints points = line.getDataPoints();
        final var positions = new ArrayList<ReadOnly.Vector<?>>(points.size());
        for (int i = 0; i < points.size(); i++) {
            positions.add(points.getPosition(i).copy(DataType.DOUBLE));
        }
        return positions;
    }

    /**
     * Returns the length of the diagonal of the given envelope, or 0 if there is no envelope.
     */
    private static double diagonal(final Envelope envelope) {
        double sum = 0;
        if (envelope != null) {
            for (int i = 0; i < envelope.getDimension(); i++) {
                final double span = envelope.getSpan(i);
                if (Double.isFinite(span)) {
                    sum += span * span;
                }
            }
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns a line string made of the given positions.
     *
     * @param  crs     reference system of the positions.
     * @param  points  positions of the line string.
     * @return the line string.
     */
    public static LineString lineString(final CoordinateReferenceSystem crs, final List<? extends ReadOnly.Vector<?>> points) {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        return factory.createLineString(factory.createDataPoints(NDArrays.of(points, SampleSystem.of(crs), DataType.DOUBLE)));
    }

    /**
     * Returns the given polyline with points added on its segments, so that consecutive points are at most at
     * the given spacing. The attributes of the added points are interpolated between the ends of their segment.
     *
     * @param  curve    the polyline, for its unit of coordinates.
     * @param  points   the points of the polyline.
     * @param  spacing  maximal distance between two consecutive points, or zero or {@code null} for no limit.
     *                  May be in percent of the polyline length.
     * @return the points of the densified polyline, or {@code points} if no point needs to be added.
     */
    public static DataPoints densify(final Curve curve, final DataPoints points, final Quantity<?> spacing) {
        final int n = points.size();
        final double[] segments = new double[Math.max(n - 1, 0)];
        double length = 0;
        for (int i = 0; i < segments.length; i++) {
            segments[i] = distance(points.getPosition(i), points.getPosition(i + 1));
            length += segments[i];
        }
        final double s = distance(spacing, Geometries.getLinearUnit(curve), length);
        if (s == 0) {
            return points;
        }
        final var result = new ArrayList<Point>();
        for (int i = 0; i < segments.length; i++) {
            final int parts = Math.max(1, (int) Math.ceil(segments[i] / s));
            for (int k = 0; k < parts; k++) {
                result.add((k == 0) ? points.getPoint(i) : points.getInterpolatedPoint(i, i + 1, k / (double) parts));
            }
        }
        if (n != 0) {
            result.add(points.getPoint(n - 1));
        }
        if (result.size() == n) {
            return points;
        }
        final DataPointsType type = points.getType();
        final DataPoints copy = GeometryFactory.DEFAULT.createDataPoints(type, result.size());
        for (int i = 0; i < result.size(); i++) {
            for (final String name : type.getAttributeNames()) {
                copy.setAttribute(i, name, result.get(i).getAttribute(name));
            }
        }
        return copy;
    }
}
