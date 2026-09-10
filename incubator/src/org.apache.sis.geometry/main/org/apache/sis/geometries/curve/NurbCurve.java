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
import org.apache.sis.geometries.internal.shared.DefaultNurbCurve;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A rational b-spline curve, i.e. a b-spline whose control points carry a weight.
 *
 * <p>A NURBS performs exactly the same computations as a {@link BSplineCurve}, but on a homogeneous
 * coordinate space, so that the interpolation is a quotient of polynomials. A correctly implemented
 * b-spline able to handle homogeneous coordinates can produce a NURBS, and a NURBS with a constant
 * weight can produce any b-spline.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>{@link #isRational()} is {@code true} and the control points are expressed in homogeneous
 *       coordinates.</li>
 * </ul>
 *
 * <p>Note: despite its name, a non-uniform rational b-spline may have a uniform knot sequence.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.8
 */
@UML(identifier="NURB", specification=ISO_19107)
public sealed interface NurbCurve extends BSplineCurve
        permits DefaultNurbCurve
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "NURBS";

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
     * Returns {@link CurveInterpolation#NURBS}.
     *
     * @see ISO 19107:2019 - 6.4.24
     */
    @UML(identifier="interpolation", specification=ISO_19107)
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

    /**
     * Returns {@code null}, since a NURBS does not approximate any particular curve by default.
     *
     * @see ISO 19107:2019 - 7.13.4.2
     */
    @Override
    default SplineCurveForm getCurveForm() {
        return null;
    }

    /**
     * Returns {@link KnotType#NON_UNIFORM}.
     *
     * @see ISO 19107:2019 - 7.13.4.5
     */
    @Override
    default KnotType getKnotSpec() {
        return KnotType.NON_UNIFORM;
    }

    /**
     * Returns {@code true}, since a NURBS is rational by definition.
     *
     * @see ISO 19107:2019 - 7.13.4.6
     */
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
