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

import java.util.List;
import java.util.function.DoubleFunction;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultArcByCenterPoint extends AbstractGeometry implements ArcByCenterPoint {

    private final Point center;
    private final double radius;
    private final Unit<?> radiusUnit;
    private final double startAngle;
    private final double endAngle;

    /**
     * Creates an arc around the given centre.
     *
     * @param  center      centre of the circle the arc is a part of.
     * @param  radius      radius of that circle, expressed in {@code radiusUnit}. Must be greater than zero.
     * @param  radiusUnit  unit of {@code radius}, or {@code null} if the radius is expressed in the
     *                     units of the coordinate system axes.
     * @param  startAngle  bearing at which the arc starts, in decimal degrees.
     * @param  endAngle    bearing at which the arc ends, in decimal degrees.
     * @throws IllegalArgumentException if the radius is not a strictly positive real number,
     *         or if either angle is not a real number.
     */
    public DefaultArcByCenterPoint(final Point center, final double radius, final Unit<?> radiusUnit,
            final double startAngle, final double endAngle)
    {
        if (center == null) {
            throw new IllegalArgumentException("An arc by centre point needs a centre point.");
        }
        if (!(radius > 0) || Double.isInfinite(radius)) {           // Rejects NaN as well.
            throw new IllegalArgumentException("The radius of an arc must be a strictly positive"
                    + " real number, but got " + radius + '.');
        }
        if (!Double.isFinite(startAngle) || !Double.isFinite(endAngle)) {
            throw new IllegalArgumentException("The start and end angles of an arc must be real"
                    + " numbers, but got " + startAngle + " and " + endAngle + '.');
        }
        this.center     = center;
        this.radius     = radius;
        this.radiusUnit = radiusUnit;
        this.startAngle = startAngle;
        this.endAngle   = endAngle;
    }

    @Override
    public Point getCenter() {
        return center;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public Unit<?> getRadiusUnit() {
        return radiusUnit;
    }

    @Override
    public double getStartAngle() {
        return startAngle;
    }

    @Override
    public double getEndAngle() {
        return endAngle;
    }

    /**
     * Returns the smallest envelope containing this arc. That envelope contains the start and end points,
     * plus each point where the arc reaches an extreme along the first or second axis. Those extremes are
     * at the multiples of 90° the arc sweeps over.
     *
     * @throws UnsupportedOperationException if the radius unit cannot be converted to the unit of an axis,
     *         for example a radius in metres around a centre in a geographic reference system.
     */
    @Override
    public Envelope getEnvelope() {
        // Axes after the first two keep the coordinates of the centre.
        final Vector<?> c = center.getPosition();
        final double cx = c.get(0);
        final double cy = c.get(1);
        final double rx = radius(0);
        final double ry = radius(1);
        // The sweep direction does not change the positions covered by the arc.
        final double lo = Math.min(startAngle, endAngle);
        final double hi = Math.max(startAngle, endAngle);
        // The centre is not on the arc: the envelope starts from an end point, not from the centre.
        double t = Math.toRadians(lo);
        final Vector<?> start = position(c, cx + rx * Math.cos(t), cy + ry * Math.sin(t));
        final var envelope = new NDRange(getCoordinateReferenceSystem(), start, start);
        t = Math.toRadians(hi);
        envelope.add(position(c, cx + rx * Math.cos(t), cy + ry * Math.sin(t)));
        // Any sweep of 360° or more contains the four extremes, so the loop stops after one turn.
        final double last = Math.min(hi, lo + 360);
        for (double q = Math.ceil(lo / 90) * 90; q <= last; q += 90) {
            switch (Math.floorMod((long) (q / 90), 4)) {
                case 0:  envelope.add(position(c, cx + rx, cy)); break;
                case 1:  envelope.add(position(c, cx, cy + ry)); break;
                case 2:  envelope.add(position(c, cx - rx, cy)); break;
                default: envelope.add(position(c, cx, cy - ry)); break;
            }
        }
        return envelope;
    }

    /**
     * Returns a polyline whose points are on this arc, from its start angle to its end angle.
     *
     * @throws UnsupportedOperationException if the radius unit cannot be converted to the unit of an axis.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        final Vector<?> c = center.getPosition();
        final double cx = c.get(0);
        final double cy = c.get(1);
        final double rx = radius(0);
        final double ry = radius(1);
        final DoubleFunction<Vector<?>> function = (angle) -> {
            final double t = Math.toRadians(angle);
            return position(c, cx + rx * Math.cos(t), cy + ry * Math.sin(t));
        };
        return CurveLinearizer.linearize(this, spacing, offset,
                List.of(new CurveLinearizer.Piece(function, new double[] {startAngle, endAngle}, null)));
    }

    /**
     * Returns a position of type {@code DOUBLE} with its first two coordinates set to the given ones.
     * The other coordinates are those of the centre.
     */
    private static Vector<?> position(final Vector<?> centre, final double x, final double y) {
        final double[] coordinates = centre.toArrayDouble();
        coordinates[0] = x;
        coordinates[1] = y;
        return Vectors.create(coordinates, DataType.DOUBLE);
    }

    /**
     * Returns the radius expressed in the unit of the given axis.
     *
     * @throws UnsupportedOperationException if the radius unit cannot be converted to the axis unit.
     */
    private double radius(final int axis) {
        final Unit<?> axisUnit = getCoordinateReferenceSystem().getCoordinateSystem().getAxis(axis).getUnit();
        if (radiusUnit == null || axisUnit == null) {
            return radius;
        }
        try {
            return radiusUnit.getConverterToAny(axisUnit).convert(radius);
        } catch (IncommensurableException e) {
            throw new UnsupportedOperationException("The radius in " + radiusUnit
                    + " cannot be converted to the unit of axis " + axis + " (" + axisUnit + ").", e);
        }
    }

    /**
     * Returns the center of this arc, as a {@code DataPoints} of one position. An arc by center point is
     * defined by that position and by angles, so the center is the only position it holds.
     */
    @Override
    public DataPoints getDataPoints() {
        return center.getDataPoints();
    }

}
