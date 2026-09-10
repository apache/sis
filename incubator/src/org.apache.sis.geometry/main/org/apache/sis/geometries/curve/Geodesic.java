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

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultGeodesic;
import org.apache.sis.maths.Array;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A curve following the shortest path between its data points on the geometric reference surface.
 *
 * <p>Where the reference surface is not flat, as on a geoid, an ellipsoid or a sphere, the shortest
 * path between two positions is a geodesic curve rather than a straight {@link LineString}. Since
 * the reference surface is embedded in a 3-dimensional Euclidean space, a geodesic is also a curve
 * of that space.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A geodesic is a chain of geodesic segments, each of them covering two consecutive
 *       data points.</li>
 *   <li>The curvature vector of a geodesic is normal to the reference surface, therefore its
 *       tangential curvature is zero and it depends only on the surface and on its local radius.</li>
 *   <li>The shortest path is not always unique: on an ellipsoid, two antipodal positions are joined
 *       by several geodesics.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.3.2
 */
@UML(identifier="Geodesic", specification=ISO_19107)
public sealed interface Geodesic extends Curve
        permits DefaultGeodesic
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "GEODESIC";

    /**
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@link CurveInterpolation#GEODESIC}.
     * The interpolation between two data points is computed by the associated geometric coordinate
     * system, from its distance, bearing and point-at-distance operations.
     *
     * @see ISO 19107:2019 - 7.3.2.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.GEODESIC;
    }

    /**
     * The number of Points in this Geodesic.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return number of Points in this Geodesic.
     */
    default int getNumPoints() {
        return getDataPoints().size();
    }

    /**
     * Returns the specified Point N in this Geodesic.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return the specified Point N in this Geodesic.
     */
    default Point getPointN(int n) {
        return getDataPoints().getPoint(n);
    }

    /**
     * @return null, a Geodesic has no control points
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
    default AttributesType getAttributesType() {
        return getDataPoints().getAttributesType();
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("GEODESIC (");
        final DataPoints points = getDataPoints();
        AbstractGeometry.toText(sb, points);
        sb.append(')');
        return sb.toString();
    }
}
