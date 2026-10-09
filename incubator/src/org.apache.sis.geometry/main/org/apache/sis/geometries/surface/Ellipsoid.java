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
package org.apache.sis.geometries.surface;

import java.util.Arrays;
import java.util.List;
import javax.measure.Quantity;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.surface.ParametricCurveSurface;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;

// Specific to the geoapi-3.1 branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * An ellipsoid geometry defined by a center and one semi-axis length per axis of the coordinate
 * system.
 *
 * <p>As a conic surface, an ellipsoid is a parametric curve surface given as a family of ellipses
 * whose positions vary along the axis of the ellipsoid. Its horizontal curves are therefore lines
 * of constant latitude and its vertical curves lines of constant longitude.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The up-normal is the outward normal when the control points are ordered by increasing
 *       longitude and increasing latitude.</li>
 *   <li>There is one semi-axis per axis of the coordinate system, and each of them is positive.</li>
 * </ul>
 *
 * <p>Note: this geometry is not the ellipsoid used as a geometric reference surface
 * (ISO 19107:2019 - 6.2.3.4), which {@link org.opengis.referencing.datum.Ellipsoid} describes.
 * That one is a property of the coordinate reference system; this one is a geometry expressed
 * in such a system.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Ellipsoid extends AbstractGeometry implements ParametricCurveSurface {

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
    public Ellipsoid(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs ellipsoid coordinate system, not null.
     */
    public Ellipsoid(CoordinateReferenceSystem crs) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
        semiAxes = unitSemiAxes(crs.getCoordinateSystem().getDimension());
    }

    /**
     * @param dimension number of dimensions of the ellipsoid, must be positive.
     * @param semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                  Each of them must be positive.
     */
    public Ellipsoid(int dimension, double... semiAxes) {
        this(Geometries.getUndefinedCRS(dimension), semiAxes);
    }

    /**
     * @param crs       ellipsoid coordinate system, not null.
     * @param semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                  Each of them must be positive.
     */
    public Ellipsoid(CoordinateReferenceSystem crs, double... semiAxes) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
        this.semiAxes = checkedSemiAxes(crs.getCoordinateSystem().getDimension(), semiAxes);
    }

    /**
     * Creates an ellipsoid centered on the single position of the given sequence. The sequence is
     * taken as-is, so the caller may give the ellipsoid the attributes carried by that sequence.
     *
     * @param  points  the center of the ellipsoid.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Ellipsoid(DataPoints points) {
        this.points = checkedPoints(points);
        semiAxes = unitSemiAxes(points.getDimension());
    }

    /**
     * Creates an ellipsoid of the given semi-axes centered on the single position of the given
     * sequence.
     *
     * @param  points    the center of the ellipsoid.
     * @param  semiAxes  length of the semi-axis along each axis, one value per dimension.
     *                   Each of them must be positive.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position,
     *         or if the number of semi-axes is not the number of dimensions.
     */
    public Ellipsoid(DataPoints points, double... semiAxes) {
        this.points = checkedPoints(points);
        this.semiAxes = checkedSemiAxes(points.getDimension(), semiAxes);
    }

    /**
     * Returns the given sequence if it holds the single position an ellipsoid is centered on.
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
    public GeometryType getGeometryType() {
        return GeometryType.ELLIPSOID;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Quantity<?> getArea() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        if (cs.getCoordinateSystem().getDimension() != getCoordinateReferenceSystem().getCoordinateSystem().getDimension()) {
            throw new IllegalArgumentException("New CRS dimension must be the same as previous CRS");
        }
        points.setCoordinateReferenceSystem(cs);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public DataPointsType getDataPointsType() {
        return points.getType();
    }

    /**
     * Returns {@link SurfaceInterpolation#ELLIPTICAL}: this surface is a section of an ellipsoid.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    public List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.ELLIPTICAL);
    }

    /**
     * Returns a copy of the semi-axis lengths, one per axis of the coordinate system.
     * Writing in the returned array does not reshape this ellipsoid;
     * use {@link #setSemiAxes(double...)} for that.
     *
     * @return length of the semi-axis along each axis.
     */
    public double[] getSemiAxes() {
        return semiAxes.clone();
    }

    /**
     * @param  dimension  index of the axis along which to measure the semi-axis.
     * @return length of the semi-axis along the given axis.
     */
    public double getSemiAxis(int dimension) {
        return semiAxes[dimension];
    }

    /**
     * @param semiAxes new semi-axis lengths, one per dimension. Each of them must be positive.
     * @throws IllegalArgumentException if the number of semi-axes is not the number of dimensions.
     */
    public void setSemiAxes(double... semiAxes) {
        ensureValidSemiAxes(this.semiAxes.length, semiAxes);
        System.arraycopy(semiAxes, 0, this.semiAxes, 0, this.semiAxes.length);
    }

    /**
     * @param dimension index of the axis along which to set the semi-axis.
     * @param length    new semi-axis length, must be positive.
     */
    public void setSemiAxis(int dimension, double length) {
        ArgumentChecks.ensurePositive("length", length);
        semiAxes[dimension] = length;
    }

    /**
     * Returns a copy of the ellipsoid center. Writing in the returned tuple does not move this
     * ellipsoid; use {@link #setCenter(Tuple)} for that.
     *
     * @return ellipsoid center.
     */
    public ReadOnly.Vector<?> getCenter() {
        return points.getPosition(0);
    }

    /**
     * @param position new center of the ellipsoid
     */
    public void setCenter(ReadOnly.Vector<?> position) {
        points.setPosition(0, position);
    }

    /**
     * {@inheritDoc }
     *
     * <p>An ellipsoid reaches its semi-axis away from its center on each axis, so the smallest box
     * containing it is the box of that extent around the center.</p>
     */
    @Override
    public Envelope getEnvelope() {
        final Vector<?> center = getCenter().copy();
        final BBox env = new BBox(center, center);
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double c = center.get(i);
            final double a = semiAxes[i];
            if (a > 0) {
                env.setRange(i, c-a, c+a);
            }
        }
        return env;
    }

    @Override
    public Geometry getBoundary() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryType getHorizontalCurveType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryType getVerticalCurveType() {
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
    public DirectPosition getSurface(double u, double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ReferenceIdentifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the center of this ellipsoid, as a sequence of one position. An ellipsoid is defined
     * by that position and by its semi-axes, so the center is the only position it holds.
     */
    @Override
    public DataPoints getDataPoints() {
        return points;
    }

}
