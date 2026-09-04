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

import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.ArcByCenterPoint;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultArcByCenterPoint extends AbstractGeometry implements ArcByCenterPoint {

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

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(TYPE).append(" (");
        toText(sb, center.getPosition());
        sb.append(", RADIUS ").append(radius);
        if (radiusUnit != null) {
            sb.append(' ').append(radiusUnit);
        }
        sb.append(", ANGLES ").append(startAngle).append(' ').append(endAngle);
        return sb.append(')').toString();
    }
}
