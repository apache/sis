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
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryType;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.solid.BSplineSolid;
import org.apache.sis.geometries.DataPointsType;


/**
 * A parametric curve solid whose three families of curves are b-splines.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultBSplineSolid extends AbstractGeometry implements BSplineSolid {

    /**
     * Positions of this solid at the knots of the parameter grid, in row-major order.
     */
    protected final DataPoints points;

    /**
     * Control points of the section curves, in row-major order, or an empty list if none.
     */
    protected final List<DirectPosition> controlPoints;

    /**
     * Number of horizontal rows in the parameter grid.
     */
    protected final int rows;

    /**
     * Number of vertical columns in the parameter grid.
     */
    protected final int columns;

    /**
     * Number of depth files in the parameter grid.
     */
    protected final int files;

    /**
     * Creates a b-spline solid over the given parameter grid.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} × {@code files} of them.
     * @param  controlPoints  control points in row-major order, or {@code null} if none.
     * @param  rows           number of horizontal rows in the parameter grid.
     * @param  columns        number of vertical columns in the parameter grid.
     * @param  files          number of depth files in the parameter grid.
     */
    public DefaultBSplineSolid(final DataPoints points, final List<DirectPosition> controlPoints,
            final int rows, final int columns, final int files)
    {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = (controlPoints == null) ? List.of() : List.copyOf(controlPoints);
        this.rows          = rows;
        this.columns       = columns;
        this.files         = files;
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
    public int getFiles() {
        return files;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        return controlPoints;
    }

    /**
     * Returns {@link GeometryType#SPLINECURVE}: the horizontal sections are b-splines.
     */
    @Override
    public GeometryType getHorizontalCurveType() {
        return GeometryType.SPLINECURVE;
    }

    /**
     * Returns {@link GeometryType#SPLINECURVE}: the vertical sections are b-splines.
     */
    @Override
    public GeometryType getVerticalCurveType() {
        return GeometryType.SPLINECURVE;
    }

    /**
     * Returns {@link GeometryType#SPLINECURVE}: the depth sections are b-splines.
     */
    @Override
    public GeometryType getDepthCurveType() {
        return GeometryType.SPLINECURVE;
    }

    @Override
    public Curve getHorizontalCurve(double b, double c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getVerticalCurve(double a, double c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getDepthCurve(double a, double b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectPosition getSurface(double a, double b, double c) {
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
