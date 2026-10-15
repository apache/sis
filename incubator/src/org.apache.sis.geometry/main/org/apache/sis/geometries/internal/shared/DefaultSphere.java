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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;


/**
 * Default implementation of a sphere, storing its center in a {@code DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultSphere extends AbstractGeometry implements Sphere {

    /**
     * Must contain one point: the center of the sphere.
     */
    private final DataPoints points;

    private double radius = 1.0;

    /**
     * @param dimension number of dimensions of the sphere, must be positive.
     */
    public DefaultSphere(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs sphere coordinate system, not null.
     */
    public DefaultSphere(CoordinateReferenceSystem crs) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
    }

    /**
     * @param dimension number of dimensions of the sphere, must be positive.
     * @param radius radius new sphere radius, must be positive.
     */
    public DefaultSphere(int dimension, double radius) {
        this(Geometries.getUndefinedCRS(dimension), radius);
    }

    /**
     * @param crs sphere coordinate system, not null.
     * @param radius radius new sphere radius, must be positive.
     */
    public DefaultSphere(CoordinateReferenceSystem crs, double radius) {
        this(crs);
        this.radius = radius;
    }

    /**
     * Creates a sphere centered on the single position of the given {@code DataPoints}.
     * The {@code DataPoints} is taken as-is, so the caller may give the sphere the attributes
     * carried by that {@code DataPoints}.
     *
     * @param  points  the center of the sphere.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly one position.
     */
    public DefaultSphere(DataPoints points) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Sphere sequence must contain one point");
        }
        this.points = points;
    }

    /**
     * Creates a sphere of the given radius centered on the single position of the given {@code DataPoints}.
     *
     * @param  points  the center of the sphere.
     * @param  radius  new sphere radius, must be positive.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly one position.
     */
    public DefaultSphere(DataPoints points, double radius) {
        this(points);
        this.radius = radius;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public void setRadius(double radius) {
        ArgumentChecks.ensurePositive("radius", radius);
        this.radius = radius;
    }

    @Override
    public ReadOnly.Vector<?> getCenter() {
        return points.getPosition(0);
    }

    @Override
    public void setCenter(ReadOnly.Vector<?> position) {
        points.setPosition(0, position);
    }

    // methods from ParametricCurveSurface

    @Override
    public int getRows() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getColumns() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Array getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<double[]> getKnots() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public Vector<?> getSurface(double u, double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the center of this sphere, as a {@code DataPoints} of one position. A sphere is defined by
     * that position and by its radius, so the center is the only position it holds.
     */
    @Override
    public DataPoints getDataPoints() {
        return points;
    }

}
