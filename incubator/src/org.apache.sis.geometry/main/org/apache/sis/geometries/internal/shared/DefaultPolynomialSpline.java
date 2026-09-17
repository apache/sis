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

import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.FunctionArc;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.curve.PolynomialSpline;
import org.apache.sis.geometries.curve.SplineCurveForm;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A spline which interpolates its data points, i.e. a polynomial curve passing through them,
 * defined piecewise between knot values by a polynomial per coordinate offset.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultPolynomialSpline extends AbstractGeometry implements PolynomialSpline {

    /**
     * Points this spline passes through.
     */
    protected final DataPoints points;

    /**
     * Control points of this spline, or {@code null} if none.
     */
    protected final Array controlPoints;

    /**
     * Knot values defining the parameter space, strictly increasing, one per data point.
     */
    protected final double[] knots;

    /**
     * Degree of the polynomials defining the interpolation.
     */
    protected final int degree;

    /**
     * Kind of curve which this spline approximates, or {@code null} if none.
     */
    protected final SplineCurveForm curveForm;

    /**
     * Distribution of the knots, given for information only.
     */
    protected final KnotType knotSpec;

    /**
     * Derivative imposed at the start point.
     */
    protected final Vector derivativeAtStart;

    /**
     * Derivative imposed at the end point.
     */
    protected final Vector derivativeAtEnd;

    /**
     * Number of continuous derivatives guaranteed at the interior knots.
     */
    protected final int derivativeInterior;

    /**
     * Creates a polynomial spline interpolating the given data points.
     *
     * @param  points              points this spline passes through, in order.
     * @param  controlPoints       control points of this spline, or {@code null} if none.
     * @param  knots               knot values, strictly increasing, one per data point.
     * @param  degree              degree of the interpolating polynomials.
     * @param  curveForm           kind of curve approximated by this spline, or {@code null} if none.
     * @param  knotSpec            distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart   derivative imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd     derivative imposed at the end point, or {@code null} if none.
     * @param  derivativeInterior  number of continuous derivatives at the interior knots,
     *                             at most {@code degree} − 1.
     */
    public DefaultPolynomialSpline(final DataPoints points, final Array controlPoints,
            final double[] knots, final int degree, final SplineCurveForm curveForm,
            final KnotType knotSpec, final Vector derivativeAtStart, final Vector derivativeAtEnd,
            final int derivativeInterior)
    {
        this.points             = Objects.requireNonNull(points);
        this.controlPoints      = controlPoints;
        this.knots              = (knots == null) ? new double[0] : knots.clone();
        this.degree             = degree;
        this.curveForm          = curveForm;
        this.knotSpec           = knotSpec;
        this.derivativeAtStart  = derivativeAtStart;
        this.derivativeAtEnd    = derivativeAtEnd;
        this.derivativeInterior = derivativeInterior;
    }

    @Override
    public Vector getDerivativeAtStart() {
        return derivativeAtStart;
    }

    @Override
    public Vector getDerivativeAtEnd() {
        return derivativeAtEnd;
    }

    @Override
    public int getDerivativeInterior() {
        return derivativeInterior;
    }

    @Override
    public SplineCurveForm getCurveForm() {
        return curveForm;
    }

    @Override
    public KnotType getKnotSpec() {
        return knotSpec;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public double[] getKnots() {
        return knots.clone();
    }

    /**
     * Returns the number of intervals in the knot array, which is the number of arcs of this spline.
     */
    @Override
    public Integer getNumArc() {
        return Math.max(0, knots.length - 1);
    }

    @Override
    public FunctionArc getSegment(int idx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Array getControlPoints() {
        return controlPoints;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(crs);
    }

    @Override
    public AttributesType getAttributesType() {
        return points.getAttributesType();
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
