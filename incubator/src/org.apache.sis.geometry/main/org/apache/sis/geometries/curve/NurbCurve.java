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
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="NURB", specification=ISO_19107) // section 7.13.8
public interface NurbCurve extends BSplineCurve {

    public static final String TYPE = "NURBS";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    @UML(identifier="interpolation", specification=ISO_19107) // section 7.1.2.2
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.NURBS;
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getDataPoints().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        getDataPoints().setCoordinateReferenceSystem(crs);
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
    default SplineCurveForm getCurveForm() {
        return null;
    }

    @Override
    default KnotType getKnotSpec() {
        return KnotType.NON_UNIFORM;
    }

    @Override
    default boolean isRational() {
        return true;
    }

    @Override
    default Integer getNumArc() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    default FunctionArc getSegment(int idx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("NURBS (");
        final DataPoints points = getDataPoints();
        AbstractGeometry.toText(sb, points);
        sb.append(')');
        return sb.toString();
    }
}
