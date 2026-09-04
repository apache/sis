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
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.Vector;


/**
 * A single circular arc described by its two end points, how far it bulges away from the chord
 * joining them, and which side of the chord it bulges towards.
 *
 * <p>This is the parameterisation that GML calls {@code gml:ArcByBulge}. Compared with
 * {@link CircularString}, which needs a third point <em>on</em> the arc, this one replaces that
 * point by a scalar {@linkplain #getBulge() bulge} — the distance from the midpoint of the chord to
 * the arc, measured along the {@linkplain #getNormal() normal}. The two carry the same information
 * and neither is an approximation of the other, but converting between them means solving for the
 * circle, so both are kept as they were written.</p>
 *
 * @author Johann Sorel (Geomatys)
 * @see GML ArcByBulge
 */
public interface ArcByBulge extends Curve {

    public static final String TYPE = "ARCBYBULGE";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns the two end points of this arc: its start point followed by its end point.
     *
     * @return the start and end points, never null and always of size 2.
     */
    PointSequence getPoints();

    /**
     * Returns the distance from the midpoint of the chord joining the two end points to the arc,
     * measured along the {@linkplain #getNormal() normal}. It is expressed in the units of the
     * coordinate system axes.
     *
     * @return the bulge of this arc.
     */
    double getBulge();

    /**
     * Returns the direction the arc bulges towards, as a vector perpendicular to the chord joining
     * the two end points.
     *
     * @return the normal to the chord, never null.
     */
    Vector<?> getNormal();

    /**
     * Returns {@link CurveInterpolation#CIRCULAR}.
     */
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.CIRCULAR;
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getPoints().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        getPoints().setCoordinateReferenceSystem(cs);
    }

    @Override
    default AttributesType getAttributesType() {
        return getPoints().getAttributesType();
    }

    @Override
    default boolean isEmpty() {
        return getPoints().isEmpty();
    }

    @Override
    default Point getStartPoint() {
        return getPoints().getPoint(0);
    }

    @Override
    default Point getEndPoint() {
        return getPoints().getPoint(getPoints().size() - 1);
    }

    /**
     * Returns {@code false}: an arc joining two distinct end points cannot close on itself, and two
     * identical end points would give a chord of zero length, for which no circle is determined.
     */
    @Override
    default boolean isClosed() {
        return false;
    }
}
