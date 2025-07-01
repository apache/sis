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
package org.apache.sis.geometries;

import org.apache.sis.geometries.privy.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;
import javax.measure.quantity.Length;
import org.apache.sis.geometry.GeneralEnvelope;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A LineString is a Curve with linear interpolation between Points.
 * Each consecutive pair of Points defines a Line segment.
 *
 * A Line is a LineString with exactly 2 Points.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Line", specification=ISO_19107) // section 7.1.2
public interface LineString extends Curve {

    public static final String TYPE = "LINESTRING";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @UML(identifier="interpolation", specification=ISO_19107) // section 7.1.2.2
    @Override
    public default CurveInterpolation getInterpolation() {
        return CurveInterpolation.LINEAR;
    }

    PointSequence getPoints();

    @Override
    public default LineString asLine(Length spacing, Length offset) {
        return this;
    }

    /**
     * The number of Points in this LineString.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return number of Points in this LineString.
     */
    default int getNumPoints() {
        return getPoints().size();
    }

    /**
     * Returns the specified Point N in this LineString.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.7.2
     * @return the specified Point N in this LineString.
     */
    default Point getPointN(int n) {
        return getPoints().getPoint(n);
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
    public default AttributesType getAttributesType() {
        return getPoints().getAttributesType();
    }

    /**
     * A Line is a LineString with exactly 2 Points.
     *
     * @return true if lineString is a line.
     */
    default boolean isLine() {
        return getPoints().size() == 2;
    }

    @Override
    default Envelope getEnvelope() {
        PointSequence points = getPoints();
        if (points.isEmpty()) {
            return null;
        }

        final Tuple start = points.getPosition(0);
        final GeneralEnvelope env = new GeneralEnvelope(start, start);
        for (int i = 1, n = points.size(); i < n; i++) {
            env.add(points.getPosition(i));
        }
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("LINESTRING (");
        final PointSequence points = getPoints();
        AbstractGeometry.toText(sb, points);
        sb.append(')');
        return sb.toString();
    }
}
