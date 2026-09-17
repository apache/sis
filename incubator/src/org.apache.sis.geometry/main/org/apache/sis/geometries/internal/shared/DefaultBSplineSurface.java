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

import java.util.List;
import java.util.Objects;
import javax.measure.quantity.Area;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.surface.BSplineSurface;
import org.apache.sis.geometries.surface.BSplineSurfaceForm;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;


/**
 * A rational or polynomial parametric surface represented by control points, b-spline basis
 * functions and possibly weights.
 *
 * <p>For the rational flavour, which carries a weight per control point and can therefore be
 * evaluated, see {@link DefaultNurbSurface}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultBSplineSurface extends AbstractGeometry implements BSplineSurface {

    /**
     * Positions of this surface at the knots of the parameter grid, in row-major order.
     */
    protected final DataPoints points;

    /**
     * Control points of the section curves, in row-major order, or an empty list if none.
     */
    protected final List<DirectPosition> controlPoints;

    /**
     * Number of rows in the parameter grid.
     */
    protected final int rows;

    /**
     * Number of columns in the parameter grid.
     */
    protected final int columns;

    /**
     * The two knot sequences used to define the basis functions, one per surface parameter.
     */
    protected final List<double[]> knots;

    /**
     * Algebraic degree of the b-spline basis functions.
     */
    protected final int degree;

    /**
     * Distribution of the knots, given for information only.
     */
    protected final KnotType knotSpec;

    /**
     * Kind of surface which this spline approximates.
     */
    protected final BSplineSurfaceForm surfaceForm;

    /**
     * Whether this surface is a polynomial spline rather than a rational one.
     */
    protected final boolean polynomial;

    /**
     * Creates a b-spline surface over the given parameter grid.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} of them.
     * @param  controlPoints  control points in row-major order, or {@code null} if none.
     * @param  rows           number of rows in the parameter grid.
     * @param  columns        number of columns in the parameter grid.
     * @param  knots          exactly two knot sequences, one per surface parameter, knots with a
     *                        multiplicity greater than one being repeated in the sequence.
     * @param  degree         algebraic degree of the b-spline basis functions.
     * @param  knotSpec       distribution of the knots, or {@code null} if unspecified.
     * @param  surfaceForm    kind of surface approximated by this spline, or {@code null} if none.
     * @param  polynomial     {@code true} if this surface is polynomial, {@code false} if the
     *                        control points are expressed in homogeneous coordinates, which makes
     *                        it rational.
     */
    public DefaultBSplineSurface(final DataPoints points, final List<DirectPosition> controlPoints,
            final int rows, final int columns, final List<double[]> knots, final int degree,
            final KnotType knotSpec, final BSplineSurfaceForm surfaceForm, final boolean polynomial)
    {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = (controlPoints == null) ? List.of() : List.copyOf(controlPoints);
        this.rows          = rows;
        this.columns       = columns;
        this.knots         = (knots == null) ? List.of() : List.copyOf(knots);
        this.degree        = degree;
        this.knotSpec      = knotSpec;
        this.surfaceForm   = surfaceForm;
        this.polynomial    = polynomial;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public List<double[]> getKnots() {
        return knots;
    }

    @Override
    public KnotType getKnotSpec() {
        return knotSpec;
    }

    @Override
    public BSplineSurfaceForm getSurfaceForm() {
        return surfaceForm;
    }

    @Override
    public boolean isPolynomial() {
        return polynomial;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        return controlPoints;
    }

    @Override
    public Curve getHorizontalCurve(double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getVerticalCurve(double u) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectPosition getSurface(double u, double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Area getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
