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

import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.conics.CircularString;
import org.opengis.geometry.Envelope;

/**
 * A curve made of circular arcs, each defined by three of the control points.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultCircularString extends AbstractGeometry implements CircularString {

    private final PointSequence points;

    /**
     * Creates a circular string from the given control points.
     *
     * @param  points  start point, mid point and end point of each arc, consecutive arcs sharing a
     *                 point. Its size must be odd and at least 3, or 0 for an empty circular string.
     * @throws IllegalArgumentException if the number of points cannot describe a whole number of arcs.
     */
    public DefaultCircularString(PointSequence points) {
        final int size = points.size();
        if (size != 0 && (size < 3 || (size % 2) == 0)) {
            throw new IllegalArgumentException("A circular string needs an odd number of at least 3 points "
                    + "(start, mid and end of each arc, consecutive arcs sharing a point), but got " + size + '.');
        }
        this.points = points;
    }

    @Override
    public PointSequence getPoints() {
        return points;
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(TYPE).append(" (");
        toText(sb, points);
        return sb.append(')').toString();
    }

}
