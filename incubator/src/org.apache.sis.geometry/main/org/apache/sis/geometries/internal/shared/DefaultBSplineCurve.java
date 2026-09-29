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
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.BSplineCurve;
import org.apache.sis.geometries.curve.FunctionArc;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.curve.SplineCurveForm;
import org.apache.sis.maths.Array;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;


/**
 * An approximating spline using the b-spline basis functions as partition of unity,
 * so that the curve stays inside the convex hull of the control points whose weight is
 * currently non-zero.
 *
 * <p>For the rational flavour, which carries a weight per control point and can therefore be
 * evaluated, see {@link DefaultNurbCurve}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultBSplineCurve extends AbstractGeometry implements BSplineCurve {

    /**
     * Points of this curve.
     */
    protected final DataPoints points;

    /**
     * Control points of this curve, or {@code null} if none.
     */
    protected final Array controlPoints;

    /**
     * Knot values defining the parameter space and the basis functions, strictly increasing.
     */
    protected final double[] knots;

    /**
     * Degree of the b-spline basis functions.
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
     * Whether this spline uses rational functions, i.e. whether its control points are expressed
     * in homogeneous coordinates.
     */
    protected final boolean rational;

    /**
     * Creates a b-spline curve.
     *
     * @param  points         points of this curve.
     * @param  controlPoints  control points of this curve, or {@code null} if none.
     * @param  knots          knot values, strictly increasing, repeated knots being expressed by
     *                        their multiplicity rather than by repetition.
     * @param  degree         degree of the b-spline basis functions.
     * @param  curveForm      kind of curve approximated by this spline, or {@code null} if none.
     * @param  knotSpec       distribution of the knots, or {@code null} if unspecified.
     * @param  rational       whether the control points are expressed in homogeneous coordinates.
     */
    public DefaultBSplineCurve(final DataPoints points, final Array controlPoints, final double[] knots,
            final int degree, final SplineCurveForm curveForm, final KnotType knotSpec,
            final boolean rational)
    {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = controlPoints;
        this.knots         = (knots == null) ? new double[0] : knots.clone();
        this.degree        = degree;
        this.curveForm     = curveForm;
        this.knotSpec      = knotSpec;
        this.rational      = rational;
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
    public boolean isRational() {
        return rational;
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
     * Returns the number of intervals in the knot array, which is the number of arcs of this curve.
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
    public DataPointsType getDataPointsType() {
        return points.getType();
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
