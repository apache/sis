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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A polynomial curve interpolated by spline functions, using either polynomial or rational functions.
 *
 * <p>Splines come in two flavours. An <cite>interpolant</cite> spline passes exactly through its
 * data points, its shape being fixed by additional constraints on the derivatives at the ends and
 * on the continuity order. An <cite>approximant</cite> spline only approaches its control points:
 * it uses weight functions forming a partition of unity, so that the curve always stays inside the
 * convex hull of the control points whose weight is currently non-zero.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.1, 7.13.4
 */
@UML(identifier="SplineCurve", specification=ISO_19107)
public sealed interface SplineCurve extends PolynomialCurve
        permits PolynomialSpline,
                BSplineCurve
{

    /**
     * Kind of curve which this spline approximates, or {@code null} if this spline does not
     * approximate any particular curve. Given for information only.
     *
     * @return kind of curve approximated by this spline, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 7.13.4.2
     */
    @UML(identifier="curveForm", specification=ISO_19107)
    SplineCurveForm getCurveForm();

    /**
     * Knot values defining the parameter space of this spline and its basis functions.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Values are strictly increasing: repeated knots are expressed by their multiplicity
     *       rather than by repetition.</li>
     *   <li>The knot at index <var>i</var> is mapped to the data point at index <var>i</var>.</li>
     *   <li>The multiplicity of a knot is at most the {@linkplain #getDegree() degree} of this
     *       spline, since it counterbalances that degree in the smoothness at the knot.</li>
     * </ul>
     *
     * @return knot values of this spline.
     *
     * @see ISO 19107:2019 - 7.13.4.3
     */
    @UML(identifier="knot", specification=ISO_19107)
    @Override
    double[] getKnots();

    /**
     * Degree of the polynomials defining the interpolation of this spline.
     * For a {@linkplain #isRational() rational} spline, this is the limiting degree of the
     * numerator and of the denominator of the rational functions.
     *
     * @return degree of this spline.
     *
     * @see ISO 19107:2019 - 7.13.4.4
     */
    @UML(identifier="degree", specification=ISO_19107)
    @Override
    int getDegree();

    /**
     * Distribution of the {@linkplain #getKnots() knots} of this spline. Given for information only.
     *
     * @return knot distribution of this spline.
     *
     * @see ISO 19107:2019 - 7.13.4.5
     */
    @UML(identifier="knotSpec", specification=ISO_19107)
    KnotType getKnotSpec();

    /**
     * Returns whether this spline uses rational functions, i.e. a polynomial spline computed on
     * homogeneous coordinates and projected back to regular coordinates.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@code isRational()} is {@code true} if and only if the control points of this spline
     *       are expressed in homogeneous coordinates, each of them carrying a weight.</li>
     *   <li>If all the weights are equal, the curve is equivalent to the corresponding polynomial
     *       spline after projection.</li>
     * </ul>
     *
     * @return {@code true} if this spline is rational.
     *
     * @see ISO 19107:2019 - 7.13.4.6
     */
    @UML(identifier="isRational", specification=ISO_19107)
    boolean isRational();

}
