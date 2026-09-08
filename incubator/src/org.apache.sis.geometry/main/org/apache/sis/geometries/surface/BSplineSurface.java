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
package org.apache.sis.geometries.surface;

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.KnotType;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="BSplineSurface", specification=ISO_19107) // section 8.7.2
public sealed interface BSplineSurface extends ParametricCurveSurface
        permits NurbSurface
{

    @UML(identifier="degree", specification=ISO_19107) // section 8.7.2.2
    int getDegree();

    @UML(identifier="knot", specification=ISO_19107) // section 8.7.2.3
    @Override
    double[] getKnots();

    @UML(identifier="knotSpec", specification=ISO_19107) // section 8.7.2.5
    KnotType getKnotSpec();

    @UML(identifier="surfaceForm", specification=ISO_19107) // section 8.7.2.4
    BSplineSurfaceForm getSurfaceForm();

    @UML(identifier="isPolynomial", specification=ISO_19107) // section 8.7.2.6
    boolean isPolynomial();

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
    default GeometryType getHorizontalCurveType() {
        return GeometryType.SPLINECURVE;
    }

    @Override
    default GeometryType getVerticalCurveType() {
        return GeometryType.SPLINECURVE;
    }
}
