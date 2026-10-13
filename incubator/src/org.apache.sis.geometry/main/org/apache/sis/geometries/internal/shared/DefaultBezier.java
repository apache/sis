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

import javax.measure.Quantity;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.Bezier;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.curve.SplineCurveForm;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * An approximating spline using the Bézier (Bernstein) polynomials as partition of unity,
 * the curve passing only through its first and last control point.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultBezier extends DefaultPolynomialSpline implements Bezier {

    /**
     * Creates a Bézier curve of the given degree.
     *
     * @param  points             end points of the segments of this curve.
     * @param  controlPoints      control points of this curve. For a uniform knot sequence of
     *                            degree <var>n</var> there are <var>s</var>⋅<var>n</var> + 1 of
     *                            them, each subsequence of <var>n</var> + 1 points starting at a
     *                            multiple of <var>n</var> defining one segment.
     * @param  knots              knot values. For a Bézier curve the only knots are 0 and 1.
     * @param  degree             degree of the Bernstein polynomials.
     * @param  curveForm          kind of curve approximated by this spline, or {@code null} if none.
     * @param  knotSpec           distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart  derivative imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd    derivative imposed at the end point, or {@code null} if none.
     */
    public DefaultBezier(final DataPoints points, final Array controlPoints, final double[] knots,
            final int degree, final SplineCurveForm curveForm, final KnotType knotSpec,
            final Vector derivativeAtStart, final Vector derivativeAtEnd)
    {
        super(points, controlPoints, knots, degree, curveForm, knotSpec,
              derivativeAtStart, derivativeAtEnd, Math.max(0, degree - 1));
    }

    /**
     * Returns the arc length of this curve, integrated in the coordinate space of its control points.
     */
    @Override
    public Quantity<?> getLength() {
        final double length = isEmpty() ? 0 : evaluator().length();
        return Quantities.create(length, Geometries.getLinearUnit(this));
    }

    /**
     * Returns the point at the given distance along this curve, in the unit of the coordinates or in
     * percent of the length.
     */
    @Override
    public Point subCurve(final Quantity<?> dist) {
        if (isEmpty()) {
            return null;
        }
        return evaluator().subCurve(dist, Geometries.getLinearUnit(this));
    }

    /**
     * Returns the arc length parameterization of this curve, as the B-spline whose knots of multiplicity
     * {@code degree} delimit the Bézier pieces. The control points are the data points if no control
     * point is given.
     */
    private BSplineEvaluator evaluator() {
        final DataPoints ctrl = (controlPoints != null)
                ? BSplineEvaluator.asDataPoints(controlPoints, getCoordinateReferenceSystem()) : points;
        return new BSplineEvaluator(ctrl, null, BSplineEvaluator.bezierKnots(ctrl.size(), degree), degree);
    }

}
