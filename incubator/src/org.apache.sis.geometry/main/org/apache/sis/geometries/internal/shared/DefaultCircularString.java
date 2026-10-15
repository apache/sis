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
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;


/**
 * A curve made of circular arcs, each defined by three of the control points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCircularString extends AbstractGeometry implements CircularString {

    private final DataPoints points;

    /**
     * Creates a circular string from the given control points.
     *
     * @param  points  start point, mid point and end point of each arc, consecutive arcs sharing a
     *                 point. Its size must be odd and at least 3, or 0 for an empty circular string.
     * @throws IllegalArgumentException if the number of points cannot describe a whole number of arcs.
     */
    public DefaultCircularString(DataPoints points) {
        final int size = points.size();
        if (size != 0 && (size < 3 || (size % 2) == 0)) {
            throw new IllegalArgumentException("A circular string needs an odd number of at least 3 points "
                    + "(start, mid and end of each arc, consecutive arcs sharing a point), but got " + size + '.');
        }
        this.points = points;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    /**
     * Returns the smallest envelope containing this circular string. That envelope contains the
     * points of each arc, plus each point where an arc reaches an extreme along an axis.
     *
     * @throws IllegalStateException if an arc is a full circle in more than two dimensions.
     */
    @Override
    public Envelope getEnvelope() {
        final var envelope = new GeneralEnvelope(points.getEnvelope());
        Vector<?> start = (points.size() != 0) ? points.getPosition(0).copy(DataType.DOUBLE) : null;
        for (int i = 2; i < points.size(); i += 2) {
            final Vector<?> end = points.getPosition(i).copy(DataType.DOUBLE);
            Curves.addArc(envelope, start, points.getPosition(i - 1).copy(DataType.DOUBLE), end);
            start = end;
        }
        return envelope;
    }

    /**
     * Returns a polyline whose points are on this circular string, including all its data points.
     *
     * @throws IllegalStateException if an arc is a full circle in more than two dimensions.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        final var pieces = new ArrayList<CurveLinearizer.Piece>();
        for (int i = 2; i < points.size(); i += 2) {
            pieces.add(CurveLinearizer.circularArc(points.getPosition(i - 2).copy(DataType.DOUBLE),
                    points.getPosition(i - 1).copy(DataType.DOUBLE), points.getPosition(i).copy(DataType.DOUBLE), true));
        }
        if (pieces.isEmpty()) {
            return GeometryFactory.DEFAULT.createLineString(points);
        }
        return CurveLinearizer.linearize(this, spacing, offset, pieces);
    }

}
