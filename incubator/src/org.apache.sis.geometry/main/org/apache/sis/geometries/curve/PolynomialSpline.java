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

import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A spline which interpolates its data points, i.e. a polynomial curve passing through them.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A spline of degree <var>n</var> is defined, for each coordinate offset and piecewise
 *       between knot values, by a polynomial curve of degree <var>n</var>.</li>
 *   <li>The continuity at the knots where the defining polynomial changes is up to
 *       C<sup><var>n</var>−1</sup>, and is controlled by
 *       {@link #getDerivativeInterior()} whose default value is <var>n</var> − 1.</li>
 *   <li>Construction parameters may constrain up to <var>n</var> − 1 derivatives at each knot.</li>
 *   <li>Coordinate offsets being treated separately, homogeneous coordinates shall not be used;
 *       a fitted spline is never {@linkplain #isRational() rational}.</li>
 * </ul>
 *
 * <p>Note: a polyline is a polynomial spline of degree 1 and is therefore only C⁰.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.5
 */
@UML(identifier="PolynomialSpline", specification=ISO_19107)
public sealed interface PolynomialSpline extends SplineCurve
        permits CubicSpline,
                Bezier
{

    /**
     * Returns {@link CurveInterpolation#POLYNOMIAL_SPLINE}.
     *
     * @see ISO 19107:2019 - 7.13.5
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.POLYNOMIAL_SPLINE;
    }

    /**
     * Returns {@code false}: coordinate offsets being fitted separately,
     * an interpolant spline never uses homogeneous coordinates.
     *
     * @see ISO 19107:2019 - 7.13.5
     */
    @UML(identifier="isRational", specification=ISO_19107)
    @Override
    default boolean isRational() {
        return false;
    }

    /**
     * Derivative imposed at the start point of this spline.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Derivatives up to the order {@link #getDegree()} − 2 may be constrained.</li>
     *   <li>The construction parameterization not being the arc length, this vector is not
     *       necessarily a unit vector, and its length influences the shape of the curve.</li>
     * </ul>
     *
     * @return derivative at the start point.
     *
     * @see ISO 19107:2019 - 7.13.5.2
     */
    @UML(identifier="derivativeAtStart", specification=ISO_19107)
    Vector getDerivativeAtStart();

    /**
     * Number of continuous derivatives guaranteed at the interior knots of this spline,
     * where the defining polynomial changes. The default value is {@link #getDegree()} − 1.
     *
     * @return continuity order in the interior of this spline.
     *
     * @see ISO 19107:2019 - 7.13.5.2
     */
    @UML(identifier="derivativeInterior", specification=ISO_19107)
    int getDerivativeInterior();

    /**
     * Derivative imposed at the end point of this spline.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Derivatives up to the order {@link #getDegree()} − 2 may be constrained.</li>
     *   <li>The construction parameterization not being the arc length, this vector is not
     *       necessarily a unit vector, and its length influences the shape of the curve.</li>
     * </ul>
     *
     * @return derivative at the end point.
     *
     * @see ISO 19107:2019 - 7.13.5.2
     */
    @UML(identifier="derivativeAtEnd", specification=ISO_19107)
    Vector getDerivativeAtEnd();
}
