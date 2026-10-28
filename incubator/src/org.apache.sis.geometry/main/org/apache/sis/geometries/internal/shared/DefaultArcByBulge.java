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
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultArcByBulge extends AbstractGeometry implements ArcByBulge {

    private final DataPoints points;
    private final double bulge;
    private final Vector<?> normal;

    /**
     * Creates an arc between the two given points.
     *
     * @param  points  the start point followed by the end point. Its size must be exactly 2.
     * @param  bulge   distance from the midpoint of the chord to the arc, along {@code normal}.
     * @param  normal  direction the arc bulges towards, perpendicular to the chord.
     * @throws IllegalArgumentException if the number of points is not 2, if the bulge is not a
     *         real number, or if no normal is given.
     */
    public DefaultArcByBulge(final DataPoints points, final double bulge, final Vector<?> normal) {
        if (points == null || points.size() != 2) {
            throw new IllegalArgumentException("An arc by bulge is defined by exactly 2 points"
                    + " (its start and its end), but got " + ((points != null) ? points.size() : 0) + '.');
        }
        if (!Double.isFinite(bulge)) {
            throw new IllegalArgumentException("The bulge of an arc must be a real number, but got " + bulge + '.');
        }
        if (normal == null) {
            throw new IllegalArgumentException("An arc by bulge needs a normal: without it, the two"
                    + " arcs joining the end points cannot be told apart.");
        }
        this.points = points;
        this.bulge  = bulge;
        this.normal = normal;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public double getBulge() {
        return bulge;
    }

    @Override
    public Vector<?> getNormal() {
        return normal;
    }

    /**
     * Returns the smallest envelope containing this arc. That envelope contains the two end points,
     * plus each point where the circle reaches an extreme along an axis, if that point is on the side
     * of the chord the arc bulges towards. This works in any number of dimensions, the arc being in
     * the plane of the chord and the normal.
     *
     * @throws IllegalStateException if the two end points are identical, or if the normal is parallel to the chord.
     */
    @Override
    public Envelope getEnvelope() {
        final var envelope = new NDRange(points.getEnvelope());
        if (bulge != 0) {
            final Vector<?> start = points.getPosition(0).copy(DataType.DOUBLE);
            final Vector<?> end   = points.getPosition(1).copy(DataType.DOUBLE);
            Curves.addArc(envelope, start, apex(start, end), end);
        }
        return envelope;
    }

    /**
     * Returns a polyline whose points are on this arc, from its start point to its end point.
     *
     * @throws IllegalStateException if the two end points are identical, or if the normal is parallel to the chord.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        final Vector<?> start = points.getPosition(0).copy(DataType.DOUBLE);
        final Vector<?> end   = points.getPosition(1).copy(DataType.DOUBLE);
        final Vector<?> apex  = (bulge != 0) ? apex(start, end) : start.lerp(end, 0.5);
        return CurveLinearizer.linearize(this, spacing, offset, List.of(CurveLinearizer.circularArc(start, apex, end, false)));
    }

    /**
     * Returns the point of the arc at the bulge distance from the middle of the chord.
     *
     * @throws IllegalStateException if the two end points are identical, or if the normal is parallel to the chord.
     */
    private Vector<?> apex(final Vector<?> start, final Vector<?> end) {
        Vector<?> u = end.copy(DataType.DOUBLE).subtract(start);          // Unit vector along the chord.
        Vector<?> n = normal.copy(DataType.DOUBLE);                        // Unit vector toward the side the arc bulges to.
        final double chord = u.length();
        if (!(chord > 0)) {
            throw new IllegalStateException("The two end points of an arc by bulge are identical, so they do not determine a circle.");
        }
        u = u.scale(1 / chord);
        // Only the part of the normal perpendicular to the chord tells the side of the arc.
        final double length = n.length();
        n = n.perpendicular(u);
        final double perpendicular = n.length();
        if (!(perpendicular > length * 1E-12)) {
            throw new IllegalStateException("The normal of an arc by bulge is parallel to its chord,"
                    + " so it does not tell which side the arc bulges towards.");
        }
        return start.lerp(end, 0.5).add(n.scale(bulge / perpendicular));
    }

}
