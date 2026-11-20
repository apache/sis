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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Representation;
import org.apache.sis.geometries.cs.Bearing;
import org.apache.sis.geometries.internal.shared.DefaultRhumb;
import org.apache.sis.maths.Array;


/**
 * A curve on the ellipsoid following a constant bearing from one point to the next.
 * Also called a loxodrome, it crosses all the meridians at the same angle.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A rhumb is a chain of rhumb segments, each of them covering two consecutive
 *       data points at a constant azimuth.</li>
 *   <li>Like a {@link LineString}, a rhumb passes through all the latitudes and longitudes of the
 *       bounding box of any two consecutive data points.</li>
 *   <li>The {@linkplain #getControlPoints() control points} and the
 *       {@linkplain #getDataPoints() data points} are identical.</li>
 * </ul>
 *
 * <p>Note: on a sphere or an ellipsoid, the only rhumb lines which are also geodesics are the
 * equator and the meridians.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.5.1, 7.5.2
 */
@UML(identifier="Rhumb", specification=ISO_19107)
public sealed interface Rhumb extends Curve
        permits DefaultRhumb
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "RHUMB";

    /**
     * Returns {@link GeometryType#RHUMB}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.RHUMB;
    }

    /**
     * @return a Rhumb is PARAMETRIC
     */
    @Override
    default Representation getRepresentation() {
        return Representation.PARAMETRIC;
    }

    /**
     * Returns {@link CurveInterpolation#RHUMB}.
     *
     * @see ISO 19107:2019 - 7.5.2.1
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.RHUMB;
    }

    /**
     * Constant azimuth followed by the given segment of this curve.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Segment <var>i</var> goes from data point <var>i</var> to data point <var>i</var> + 1,
     *       so the index ranges from 0 inclusive to {@link #getNumPoints()} − 1 exclusive.</li>
     * </ul>
     *
     * <p>Difference with ISO 19107: the UML (figure 23) declares a single derived {@code bearing}
     * attribute, which does not appear in the {@code Rhumb} clause. Since each rhumb segment has its
     * own constant azimuth, the bearing is given per segment.</p>
     *
     * @param  segment  index of the segment.
     * @return constant bearing of the segment, or {@code null} if the segment has no length.
     * @throws IndexOutOfBoundsException if the index is outside the range of segments.
     *
     * @see ISO 19107:2019 - 7.5.1, 7.5.2
     */
    Bearing getBearing(int segment);

    /**
     * The number of Points in this Rhumb.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return number of Points in this Geodesic.
     */
    default int getNumPoints() {
        return getDataPoints().size();
    }

    /**
     * Returns the specified Point N in this Rhumb.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return the specified Point N in this Geodesic.
     */
    default Point getPointN(int n) {
        return getDataPoints().getPoint(n);
    }

    /**
     * @return null, a Rhumb has no control points
     *
     * @see ISO 19107:2019 - 6.4.18.2
     */
    @Override
    default Array getControlPoints() {
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
    default DataPointsType getDataPointsType() {
        return getDataPoints().getType();
    }

}
