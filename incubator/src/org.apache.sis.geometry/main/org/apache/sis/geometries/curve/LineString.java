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

import javax.measure.quantity.Length;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultLineString;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.triangulate.delaunay.OrientedEdge;
import org.apache.sis.maths.Array;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A curve using a linear interpolation between its data points,
 * each consecutive pair of points defining a line segment.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>At least two data points, the first one being the {@linkplain #getStartPoint() start point}
 *       and the last one the {@linkplain #getEndPoint() end point}.</li>
 *   <li>The position at the construction parameter <var>λ</var> between two consecutive data points
 *       <var>P<sub>i</sub></var> and <var>P<sub>i+1</sub></var> is
 *       (1 − <var>λ</var>)⋅<var>P<sub>i</sub></var> + <var>λ</var>⋅<var>P<sub>i+1</sub></var>
 *       for <var>λ</var> ∈ [0 … 1].</li>
 * </ul>
 *
 * <p>Note: ISO 19107 names this class a {@code Line}, even if it has more than 2 points.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.7
 * @see ISO 19107:2019 - 7.1.2
 */
@UML(identifier="Line", specification=ISO_19107)
public sealed interface LineString extends Curve
        permits LinearRing,
                DefaultLineString,
                MeshPrimitive.LineLoop,
                MeshPrimitive.LineStrip,
                OrientedEdge
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "LINESTRING";

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
     * Returns {@link CurveInterpolation#LINEAR}.
     *
     * @see ISO 19107:2019 - 7.1.2.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.LINEAR;
    }

    /**
     * Returns {@code this}, since a line string is already a linear approximation of itself.
     *
     * @see ISO 19107:2019 - 6.4.18.17
     */
    @Override
    default LineString asLine(Length spacing, Length offset) {
        return this;
    }

    /**
     * The number of Points in this LineString.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return number of Points in this LineString.
     */
    default int getNumPoints() {
        return getDataPoints().size();
    }

    /**
     * Returns the specified Point N in this LineString.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return the specified Point N in this LineString.
     */
    default Point getPointN(int n) {
        return getDataPoints().getPoint(n);
    }

    /**
     * @return null, a LineString has no control points
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

    /**
     * A Line is a LineString with exactly 2 Points.
     *
     * @return true if lineString is a line.
     */
    default boolean isLine() {
        return getDataPoints().size() == 2;
    }

    @Override
    default Envelope getEnvelope() {
        DataPoints points = getDataPoints();
        if (points.isEmpty()) {
            return null;
        }
        return points.getEnvelope();
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("LINESTRING (");
        final DataPoints points = getDataPoints();
        AbstractGeometry.toText(sb, points);
        sb.append(')');
        return sb.toString();
    }
}
