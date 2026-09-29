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
package org.apache.sis.geometries.internal.shared;

import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.CubicSpline;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.curve.SplineCurveForm;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;


/**
 * A polynomial spline of degree 3, C² everywhere and passing through its data points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCubicSpline extends DefaultPolynomialSpline implements CubicSpline {

    /**
     * Number of continuous derivatives at the interior knots of a cubic spline,
     * which is its degree minus one.
     */
    private static final int DERIVATIVE_INTERIOR = 2;

    /**
     * Creates a cubic spline interpolating the given data points.
     *
     * @param  points             points this spline passes through, in order.
     * @param  controlPoints      control points of this spline, or {@code null} if none.
     * @param  knots              knot values, strictly increasing, one per data point.
     * @param  curveForm          kind of curve approximated by this spline, or {@code null} if none.
     * @param  knotSpec           distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart  tangent imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd    tangent imposed at the end point, or {@code null} if none.
     */
    public DefaultCubicSpline(final DataPoints points, final Array controlPoints, final double[] knots,
            final SplineCurveForm curveForm, final KnotType knotSpec,
            final Vector derivativeAtStart, final Vector derivativeAtEnd)
    {
        super(points, controlPoints, knots, 3, curveForm, knotSpec,
              derivativeAtStart, derivativeAtEnd, DERIVATIVE_INTERIOR);
    }

    /**
     * Returns 3: a cubic spline is defined by polynomials of degree 3.
     */
    @Override
    public int getDegree() {
        return 3;
    }

}
