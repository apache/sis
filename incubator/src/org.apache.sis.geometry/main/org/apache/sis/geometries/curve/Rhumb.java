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
import org.apache.sis.geometries.Bearing;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.DefaultRhumb;
import org.apache.sis.maths.Array;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


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
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default String getGeometryType() {
        return TYPE;
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
     * Constant azimuth followed by this curve.
     *
     * <p>Note: this attribute appears in the ISO 19107 UML (figure 23) but not in its
     * {@code Rhumb} clause.</p>
     *
     * @return constant bearing of this curve.
     *
     * @see ISO 19107:2019 - 7.5.1
     */
    Bearing getBearing();

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
    default AttributesType getAttributesType() {
        return getDataPoints().getAttributesType();
    }

}
