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
package org.apache.sis.geometries.solid;

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
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.surface.ParametricCurveSurface;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Tuple;
import org.apache.sis.util.ArgumentChecks;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;


/**
 * A sphere geometry defined by a center and a radius.
 *
 * <p>As a conic surface, a sphere is a parametric curve surface given as a family of circles whose
 * positions vary linearly along the axis of the sphere and whose radius varies with the cosine of
 * the central angle. Its horizontal curves are therefore lines of constant latitude and its vertical
 * curves lines of constant longitude.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The up-normal is the outward normal when the control points are ordered by increasing
 *       longitude and increasing latitude.</li>
 *   <li>The radius is positive.</li>
 * </ul>
 *
 * <p>Difference with ISO 19107, which defines a sphere in a 3-dimensional space: even if it is
 * called a Sphere this class can handle 2 to N dimensions.</p>
 *
 * <p>Note: ISO 19107 classifies this geometry as a conic surface. It should not be confused with
 * the sphere used as a geometric reference surface (ISO 19107:2019 - 6.2.3.4).</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.5.1, 8.5.2
 */
@UML(identifier="Sphere", specification=ISO_19107)
public final class Sphere extends AbstractGeometry implements ParametricCurveSurface {

    /**
     * Must contain one point: the center of the sphere.
     */
    private final DataPoints points;

    private double radius = 1.0;

    /**
     * @param dimension number of dimensions of the sphere, must be positive.
     */
    public Sphere(int dimension) {
        this(Geometries.getUndefinedCRS(dimension));
    }

    /**
     * @param crs sphere coordinate system, not null.
     */
    public Sphere(CoordinateReferenceSystem crs) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, 1));
    }

    /**
     * @param dimension number of dimensions of the sphere, must be positive.
     * @param radius radius new sphere radius, must be positive.
     */
    public Sphere(int dimension, double radius) {
        this(Geometries.getUndefinedCRS(dimension), radius);
    }

    /**
     * @param crs sphere coordinate system, not null.
     * @param radius radius new sphere radius, must be positive.
     */
    public Sphere(CoordinateReferenceSystem crs, double radius) {
        this(crs);
        this.radius = radius;
    }

    /**
     * Creates a sphere centered on the single position of the given sequence. The sequence is
     * taken as-is, so the caller may give the sphere the attributes carried by that sequence.
     *
     * @param  points  the center of the sphere.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Sphere(DataPoints points) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Sphere sequence must contain one point");
        }
        this.points = points;
    }

    /**
     * Creates a sphere of the given radius centered on the single position of the given sequence.
     *
     * @param  points  the center of the sphere.
     * @param  radius  new sphere radius, must be positive.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Sphere(DataPoints points, double radius) {
        this(points);
        this.radius = radius;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.SPHERE;
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
     * Returns {@link SurfaceInterpolation#SPHERICAL}: this surface is a section of a sphere.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    public List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.SPHERICAL);
    }

    /**
     * @return radius of the sphere.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius new sphere radius, must be positive.
     */
    public void setRadius(double radius) {
        ArgumentChecks.ensurePositive("radius", radius);
        this.radius = radius;
    }

    /**
     * Returns a copy of the sphere center. Writing in the returned tuple does not move this
     * sphere; use {@link #setCenter(Tuple)} for that.
     *
     * @return sphere center.
     */
    public Tuple<?> getCenter() {
        return points.getPosition(0);
    }

    /**
     * @param position new center of the sphere
     */
    public void setCenter(Tuple<?> position) {
        points.setPosition(0, position);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() {
        final Tuple<?> center = getCenter();
        final BBox env = new BBox(center, center);
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        if (radius > 0) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                double c = center.get(i);
                env.setRange(i, c-radius, c+radius);
            }
        }
        return env;
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
    public Identifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the center of this sphere, as a sequence of one position. A sphere is defined by
     * that position and by its radius, so the center is the only position it holds.
     */
    @Override
    public DataPoints getDataPoints() {
        return points;
    }

}
