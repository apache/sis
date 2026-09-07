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
import org.apache.sis.maths.Array;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A curve between two point which is interpolated on the ellipsid.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Geodesic", specification=ISO_19107) // section 7.3.2
public interface Geodesic extends Curve {

    public static final String TYPE = "GEODESIC";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @UML(identifier="interpolation", specification=ISO_19107) // section 7.3.2.2
    @Override
    public default CurveInterpolation getInterpolation() {
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
    public default AttributesType getAttributesType() {
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
