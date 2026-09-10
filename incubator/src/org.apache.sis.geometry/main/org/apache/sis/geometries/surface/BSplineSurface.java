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

import java.util.List;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.KnotType;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A rational or polynomial parametric surface represented by control points, basis functions and
 * possibly weights.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The knot spans of the parameter grid are defined by spline functions.</li>
 *   <li>If {@link #isPolynomial()} is {@code true}, the surface is piecewise polynomial and the
 *       coordinate reference system is non-homogeneous.</li>
 *   <li>If {@link #isPolynomial()} is {@code false}, the surface is piecewise rational and the
 *       coordinate reference system is augmented with a weight column.</li>
 *   <li>The {@linkplain #getHorizontalCurveType() horizontal} and
 *       {@linkplain #getVerticalCurveType() vertical} curve types are both b-splines.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.7.1, 8.7.2
 */
@UML(identifier="BSplineSurface", specification=ISO_19107)
public sealed interface BSplineSurface extends ParametricCurveSurface
        permits NurbSurface
{

    /**
     * Algebraic degree of the b-spline basis functions of this surface.
     *
     * @return degree of this surface.
     *
     * @see ISO 19107:2019 - 8.7.2.2
     */
    @UML(identifier="degree", specification=ISO_19107)
    int getDegree();

    /**
     * The two knot sequences used to define the b-spline basis functions,
     * one for each of the two surface parameters.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Exactly two sequences.</li>
     *   <li>Knots with a multiplicity greater than one are repeated in the sequence.</li>
     * </ul>
     *
     * @return knot values, one sequence per surface parameter.
     *
     * @see ISO 19107:2019 - 8.7.2.3
     */
    @UML(identifier="knot", specification=ISO_19107)
    @Override
    List<double[]> getKnots();

    /**
     * Distribution of the {@linkplain #getKnots() knots} of this surface.
     * Given for information only.
     *
     * @return knot distribution of this surface.
     *
     * @see ISO 19107:2019 - 8.7.2.5
     */
    @UML(identifier="knotSpec", specification=ISO_19107)
    KnotType getKnotSpec();

    /**
     * Kind of surface which this spline approximates, or
     * {@link BSplineSurfaceForm#UNSPECIFIED} if this spline does not approximate any particular
     * surface. Given for information only.
     *
     * @return kind of surface approximated by this spline.
     *
     * @see ISO 19107:2019 - 8.7.2.4
     */
    @UML(identifier="surfaceForm", specification=ISO_19107)
    BSplineSurfaceForm getSurfaceForm();

    /**
     * Returns whether this surface is a polynomial spline rather than a rational one.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@code isPolynomial()} is {@code false} if and only if the control points of this
     *       surface are expressed in homogeneous coordinates, each of them carrying a weight,
     *       which makes it a non-uniform rational b-spline (NURBS).</li>
     * </ul>
     *
     * @return {@code true} if this surface is polynomial, {@code false} if it is rational.
     *
     * @see ISO 19107:2019 - 8.7.2.6
     */
    @UML(identifier="isPolynomial", specification=ISO_19107)
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

    /**
     * Returns {@link GeometryType#SPLINECURVE}.
     *
     * @see ISO 19107:2019 - 8.7.2.1
     */
    @Override
    default GeometryType getHorizontalCurveType() {
        return GeometryType.SPLINECURVE;
    }

    /**
     * Returns {@link GeometryType#SPLINECURVE}.
     *
     * @see ISO 19107:2019 - 8.7.2.1
     */
    @Override
    default GeometryType getVerticalCurveType() {
        return GeometryType.SPLINECURVE;
    }
}
