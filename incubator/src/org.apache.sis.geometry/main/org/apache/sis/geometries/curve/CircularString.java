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
package org.apache.sis.geometries.curve;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.DataPoints;


/**
 * A curve made of circular arcs joined end to end, each arc being defined by three points: its
 * start, a point somewhere along it, and its end. Consecutive arcs share a point, so a circular
 * string of <var>k</var> arcs has 2·<var>k</var>+1 points — always an odd number, and at least
 * three.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#circular_string
 * @see GML ArcString
 */
public interface CircularString extends Curve {

    public static final String TYPE = "CIRCULARSTRING";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns the number of arcs this circular string is made of.
     */
    default int getNumArcs() {
        final int size = getDataPoints().size();
        return (size == 0) ? 0 : (size - 1) / 2;
    }

    /**
     * Returns {@link CurveInterpolation#CIRCULAR}.
     */
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.CIRCULAR;
    }

    /**
     * Returns the control points of this circular string: for each arc, its start point, a point
     * on the arc and its end point, with consecutive arcs sharing a point.
     *
     * @return the control points, never null. Its size is odd and at least 3, or 0 if empty.
     */
    @Override
    DataPoints getDataPoints();

    /**
     * @return null, a CircularString has no control points
     */
    @Override
    public default Array getControlPoints() {
        return null;
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getDataPoints().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        getDataPoints().setCoordinateReferenceSystem(cs);
    }

    @Override
    default AttributesType getAttributesType() {
        return getDataPoints().getAttributesType();
    }

    @Override
    default boolean isEmpty() {
        return getDataPoints().isEmpty();
    }

    @Override
    default Point getStartPoint() {
        return getDataPoints().getPoint(0);
    }

    @Override
    default Point getEndPoint() {
        return getDataPoints().getPoint(getDataPoints().size() - 1);
    }

    @Override
    default boolean isClosed() {
        final DataPoints points = getDataPoints();
        final int size = points.size();
        if (size == 0) {
            return false;
        }
        return points.getPosition(0).equals(points.getPosition(size - 1), 0);
    }
}
