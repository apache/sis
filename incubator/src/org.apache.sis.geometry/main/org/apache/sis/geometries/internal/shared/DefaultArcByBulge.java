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

import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.math.Vector;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultArcByBulge extends AbstractGeometry implements ArcByBulge {

    private final PointSequence points;
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
    public DefaultArcByBulge(final PointSequence points, final double bulge, final Vector<?> normal) {
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
    public PointSequence getPoints() {
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

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(TYPE).append(" (");
        toText(sb, points);
        sb.append(", BULGE ").append(bulge);
        sb.append(", NORMAL ");
        toText(sb, normal);
        return sb.append(')').toString();
    }
}
