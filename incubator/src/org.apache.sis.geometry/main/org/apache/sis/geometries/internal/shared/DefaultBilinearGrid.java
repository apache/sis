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
import javax.measure.Quantity;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.surface.BilinearGrid;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A parametric curve surface using polylines as both horizontal and vertical curves, each cell of
 * the parameter grid being interpolated bilinearly over the unit square.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultBilinearGrid extends AbstractGeometry implements BilinearGrid {

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
     * Construction parameter values matching the data points, the first sequence being for the
     * <var>u</var> parameter and the second one for <var>v</var>.
     */
    protected final List<double[]> knots;

    /**
     * Creates a bilinear grid over the given parameter grid.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} of them.
     * @param  controlPoints  control points of the section curves in row-major order,
     *                        or {@code null} if none.
     * @param  rows           number of rows in the parameter grid.
     * @param  columns        number of columns in the parameter grid.
     * @param  knots          knot values, one sequence per surface parameter, or {@code null} if none.
     */
    public DefaultBilinearGrid(final DataPoints points, final List<DirectPosition> controlPoints,
            final int rows, final int columns, final List<double[]> knots)
    {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = (controlPoints == null) ? List.of() : List.copyOf(controlPoints);
        this.rows          = rows;
        this.columns       = columns;
        this.knots         = (knots == null) ? List.of() : List.copyOf(knots);
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
    public List<double[]> getKnots() {
        return knots;
    }

    /**
     * Returns {@link GeometryType#LINE}: the horizontal sections of a bilinear grid are polylines.
     */
    @Override
    public GeometryType getHorizontalCurveType() {
        return GeometryType.LINE;
    }

    /**
     * Returns {@link GeometryType#LINE}: the vertical sections of a bilinear grid are polylines.
     */
    @Override
    public GeometryType getVerticalCurveType() {
        return GeometryType.LINE;
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
    public Quantity<?> getArea() {
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
