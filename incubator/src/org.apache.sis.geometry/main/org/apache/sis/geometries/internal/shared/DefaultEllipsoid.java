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

import java.util.Arrays;
import java.util.List;
import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.surface.Ellipsoid;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;


/**
 * Default implementation of an ellipsoid, storing its center in a {@code DataPoints}
 * and its semi-axes in an array.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultEllipsoid extends AbstractGeometry implements Ellipsoid {

    /**
     * Must contain one point: the center of the ellipsoid.
     */
    private final DataPoints points;

    /**
     * Length of the semi-axis along each axis of the coordinate system,
     * in the units of that axis. There is one value per dimension.
     */
    private final double[] semiAxes;

    /**
     * @param dimension number of dimensions of the ellipsoid, must be positive.
     */
    public DefaultEllipsoid(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs ellipsoid coordinate system, not null.
     */
    public DefaultEllipsoid(CoordinateReferenceSystem crs) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
        semiAxes = unitSemiAxes(crs.getCoordinateSystem().getDimension());
    }

    /**
     * @param dimension number of dimensions of the ellipsoid, must be positive.
     * @param semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                  Each of them must be positive.
     */
    public DefaultEllipsoid(int dimension, double... semiAxes) {
        this(Geometries.getUndefinedCRS(dimension), semiAxes);
    }

    /**
     * @param crs       ellipsoid coordinate system, not null.
     * @param semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                  Each of them must be positive.
     */
    public DefaultEllipsoid(CoordinateReferenceSystem crs, double... semiAxes) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
        this.semiAxes = checkedSemiAxes(crs.getCoordinateSystem().getDimension(), semiAxes);
    }

    /**
     * Creates an ellipsoid centered on the single position of the given {@code DataPoints}.
     * The {@code DataPoints} is taken as-is, so the caller may give the ellipsoid the attributes
     * carried by that {@code DataPoints}.
     *
     * @param  points  the center of the ellipsoid.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly one position.
     */
    public DefaultEllipsoid(DataPoints points) {
        this.points = checkedPoints(points);
        semiAxes = unitSemiAxes(points.getDimension());
    }

    /**
     * Creates an ellipsoid of the given semi-axes centered on the single position of the given
     * {@code DataPoints}.
     *
     * @param  points    the center of the ellipsoid.
     * @param  semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                   Each of them must be positive.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly one position,
     *         or if the number of semi-axes is not the number of dimensions.
     */
    public DefaultEllipsoid(DataPoints points, double... semiAxes) {
        this.points = checkedPoints(points);
        this.semiAxes = checkedSemiAxes(points.getDimension(), semiAxes);
    }

    /**
     * Returns the given {@code DataPoints} if it holds the single position an ellipsoid is centered on.
     */
    private static DataPoints checkedPoints(final DataPoints points) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Ellipsoid sequence must contain one point");
        }
        return points;
    }

    /**
     * Returns the semi-axes of an ellipsoid of the given number of dimensions which is a unit sphere.
     */
    private static double[] unitSemiAxes(final int dimension) {
        final var lengths = new double[dimension];
        Arrays.fill(lengths, 1.0);
        return lengths;
    }

    /**
     * Returns a copy of the given semi-axes after verifying that there is one of them per dimension
     * and that all of them are positive.
     */
    private static double[] checkedSemiAxes(final int dimension, final double[] semiAxes) {
        ensureValidSemiAxes(dimension, semiAxes);
        return semiAxes.clone();
    }

    /**
     * Verifies that there is one semi-axis per dimension and that all of them are positive.
     */
    private static void ensureValidSemiAxes(final int dimension, final double[] semiAxes) {
        if (semiAxes.length != dimension) {
            throw new IllegalArgumentException("Ellipsoid must have one semi-axis per dimension, expected "
                    + dimension + " but found " + semiAxes.length);
        }
        for (int i=0; i<semiAxes.length; i++) {
            ArgumentChecks.ensurePositive("semiAxes[" + i + ']', semiAxes[i]);
        }
    }

    @Override
    public Quantity<?> getArea() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public double[] getSemiAxes() {
        return semiAxes.clone();
    }

    @Override
    public double getSemiAxis(int dimension) {
        return semiAxes[dimension];
    }

    @Override
    public void setSemiAxes(double... semiAxes) {
        ensureValidSemiAxes(this.semiAxes.length, semiAxes);
        System.arraycopy(semiAxes, 0, this.semiAxes, 0, this.semiAxes.length);
    }

    @Override
    public void setSemiAxis(int dimension, double length) {
        ArgumentChecks.ensurePositive("length", length);
        semiAxes[dimension] = length;
    }

    @Override
    public Vector<?> getCenter() {
        return points.getPosition(0);
    }

    @Override
    public void setCenter(Vector<?> position) {
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

    /**
     * Returns the line of constant latitude, {@code v} being the latitude in radians.
     * At the poles, the returned line string is reduced to the pole.
     */
    @Override
    public Curve getHorizontalCurve(double v) {
        return EllipsoidalSurfaces.parallel(getCoordinateReferenceSystem(), getCenter(), semiAxes, v, false);
    }

    /**
     * Returns the line of constant longitude from the south pole to the north pole,
     * {@code u} being the longitude in radians.
     */
    @Override
    public Curve getVerticalCurve(double u) {
        return EllipsoidalSurfaces.meridian(getCoordinateReferenceSystem(), getCenter(), semiAxes, u, false);
    }

    /**
     * Returns the position of this ellipsoid at longitude {@code u} and latitude {@code v}, in radians.
     *
     * @throws IllegalStateException if the coordinate system has less than three dimensions.
     */
    @Override
    public Vector<?> getSurface(double u, double v) {
        return EllipsoidalSurfaces.position(getCenter(), semiAxes, u, v);
    }

    /**
     * Returns the center of this ellipsoid, as a {@code DataPoints} of one position. An ellipsoid is defined
     * by that position and by its semi-axes, so the center is the only position it holds.
     */
    @Override
    public DataPoints getDataPoints() {
        return points;
    }

}
