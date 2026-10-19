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

import java.util.ArrayList;
import java.util.List;
import javax.measure.Quantity;
import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.geometries.internal.shared.DefaultCylinder;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * A cylinder along the axis joining two positions, with potentially different radii at each end.
 * <p>
 * A cone is a special case of cylinder when one of the radii is zero.
 * <p>
 * Synonym : conical frustrum is a volume defined by 2 circles aligned on the same axis
 *
 * ISO 19107 : classified as a conic surface
 *
 * <p>A cylinder is either {@linkplain #isClosed() closed} by two disks at its ends, which makes it
 * the boundary of a volume as in the KHR_implicit_shapes extension, or open, in which case it is
 * only the lateral surface that ISO 19107 defines.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getDataPoints() data points} hold exactly two positions:
 *       the center of the bottom circle, then the center of the top one.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_19107 section 8.5.4  Cylinder
 * @spec ISO_19107 section 8.5.3  Cone
 * @spec ISO_12113 KHR_implicit_shapes extension Cylinder
 */
@UML(identifier="Cylinder", specification=ISO_12113)
public sealed interface Cylinder extends Surface
        permits DefaultCylinder
{

    /**
     * Returns {@link GeometryType#CYLINDER}.
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.CYLINDER;
    }

    /**
     * Returns {@link SurfaceInterpolation#CONIC}, since ISO 19107 classifies cylinders and cones
     * as conic surfaces, followed by {@link SurfaceInterpolation#PLANAR} for the end disks if this
     * cylinder is {@linkplain #isClosed() closed}.
     *
     * @see ISO 19107:2019 - 8.5.3, 8.5.4
     */
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return isClosed() ? List.of(SurfaceInterpolation.CONIC, SurfaceInterpolation.PLANAR)
                          : List.of(SurfaceInterpolation.CONIC);
    }

    /**
     * Returns whether this cylinder is closed by a disk at each of its ends.
     * An end whose radius is zero, the apex of a cone, needs no disk.
     *
     * @return {@code true} if the ends are closed by disks, or {@code false} for the lateral surface only.
     */
    boolean isClosed();

    /**
     * Sets whether this cylinder is closed by a disk at each of its ends.
     *
     * @param closed {@code true} to close the ends by disks, or {@code false} for the lateral surface only.
     */
    void setClosed(boolean closed);

    /**
     * Returns {@code false}: a cylinder always has its two circle centers.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * @return cylinder top circle radius
     */
    double getRadiusTop();

    /**
     * @param radius new cylinder top radius
     */
    void setRadiusTop(double radius);

    /**
     * @return cylinder bottom circle radius
     */
    double getRadiusBottom();

    /**
     * @param radius new cylinder bottom radius
     */
    void setRadiusBottom(double radius);

    /**
     * Returns the vector going from the center of the bottom circle to the center of the top one.
     * It is built on doubles whatever the type of the positions, the direction and the length of
     * an axis being real values even when the positions they are derived from are integers.
     */
    private Vector<?> getAxis() {
        final ReadOnly.Vector<?> bottom = getDataPoints().getPosition(0);
        final Vector<?> axis = Vectors.create(bottom.getDimension(), DataType.DOUBLE);
        axis.set(getDataPoints().getPosition(1));
        axis.subtract(bottom);
        return axis;
    }

    /**
     * The height is the length of the axis going from the center of the bottom circle to the
     * center of the top one, so it is not set but derived from the two positions this cylinder
     * is built on.
     *
     * @return the cylinder height
     */
    default double getHeight() {
        return getAxis().length();
    }

    /**
     * A cylinder becomes a cone when the top or bottom radius is set to 0.0.
     *
     * @return true if cylinder is a cone, top or bottom radius is 0.0.
     */
    default boolean isCone() {
        return getRadiusBottom() == 0.0 || getRadiusTop() == 0.0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A circle of radius <var>r</var> perpendicular to a unit axis <var>n</var> spreads by
     * <var>r</var>·√(1 − <var>n</var>ᵢ²) on the axis <var>i</var>, which is the whole radius on
     * the axes the circle faces and nothing on the axis it is perpendicular to. The envelope is
     * the union of the two circles spread that way. A cylinder whose two positions are the same
     * has no axis to speak of, and is then bounded by its radii on every axis.</p>
     */
    @Override
    default Envelope getEnvelope() {
        final ReadOnly.Vector<?> bottom = getDataPoints().getPosition(0);
        final ReadOnly.Vector<?> top = getDataPoints().getPosition(1);
        final double radiusBottom = getRadiusBottom();
        final double radiusTop = getRadiusTop();
        final Vector<?> axis = getAxis();
        final double height = axis.length();
        final int dim = getDataPoints().getDimension();
        final NDRange bbox = new NDRange(getCoordinateReferenceSystem());
        for (int i=0;i<dim;i++){
            final double b = bottom.get(i);
            final double t = top.get(i);
            final double spread;
            if (height > 0) {
                final double n = axis.get(i) / height;
                spread = Math.sqrt(Math.max(0, 1 - n*n));
            } else {
                spread = 1;
            }
            bbox.setRange(i, Math.min(b - radiusBottom*spread, t - radiusTop*spread),
                             Math.max(b + radiusBottom*spread, t + radiusTop*spread));
        }
        return bbox;
    }

    /**
     * Returns the area of the lateral surface, π(r₁ + r₂)⋅√(h² + (r₁ − r₂)²) where <var>h</var> is the
     * {@linkplain #getHeight() height}, plus the areas π<var>r</var>² of the two end disks if this
     * cylinder is {@linkplain #isClosed() closed}.
     *
     * @throws UnsupportedOperationException if this cylinder is not 3-dimensional.
     */
    @Override
    default Quantity<?> getArea() {
        if (getDataPoints().getDimension() != 3) {
            throw new UnsupportedOperationException("The area of a cylinder is computed in 3 dimensions only.");
        }
        final double r1 = getRadiusBottom();
        final double r2 = getRadiusTop();
        final double h  = getHeight();
        double area = Math.PI * (r1 + r2) * Math.hypot(h, r1 - r2);
        if (isClosed()) {
            area += Math.PI * (r1*r1 + r2*r2);
        }
        return Quantities.create(area, Geometries.getAreaUnit(this));
    }

    /**
     * Returns the empty geometry if this cylinder is {@linkplain #isClosed() closed},
     * or the circles at its ends otherwise.
     *
     * <p>The circles of an open cylinder are oriented so that the lateral surface is on their left
     * when seen from its outward normal: the bottom circle turns counterclockwise around the axis
     * going from the bottom to the top, and the top circle clockwise. An end whose radius is zero,
     * the apex of a cone, is a single point which bounds nothing, so it has no circle.</p>
     *
     * @return the empty geometry, or a collection of the end circles.
     * @throws UnsupportedOperationException if this cylinder is open and not 3-dimensional.
     * @throws IllegalStateException if this cylinder is open and its two positions are the same.
     */
    @Override
    default Geometry getBoundary() {
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (isClosed()) {
            return GeometryFactory.DEFAULT.createEmpty(crs);
        }
        if (getDataPoints().getDimension() != 3) {
            throw new UnsupportedOperationException("The boundary of an open cylinder is computed in 3 dimensions only.");
        }
        final Vector<?> axis = getAxis();
        final double height = axis.length();
        if (!(height > 0)) {
            throw new IllegalStateException("The cylinder has no axis, its two positions are the same.");
        }
        axis.scale(1 / height);
        /*
         * Builds (u, w, axis) as a right-handed orthonormal basis. The vector `u` is derived from the
         * coordinate axis which is the most perpendicular to the cylinder axis, for numerical stability.
         */
        int k = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(axis.get(i)) < Math.abs(axis.get(k))) k = i;
        }
        final Vector<?> u = Vectors.create(axis.getDimension(), DataType.DOUBLE);
        u.set(k, 1);
        u.subtract(axis.copy().scale(axis.get(k)));
        u.normalize();
        final Vector<?> w = axis.cross(u);
        final var circles = new ArrayList<Circle>(2);
        final double radiusBottom = getRadiusBottom();
        final double radiusTop = getRadiusTop();
        final SampleSystem ss = SampleSystem.of(crs);
        if (radiusBottom > 0) {
            circles.add(circle(ss, getDataPoints().getPosition(0), radiusBottom, u, w));
        }
        if (radiusTop > 0) {
            circles.add(circle(ss, getDataPoints().getPosition(1), radiusTop, u, w.copy().scale(-1)));
        }
        return GeometryFactory.DEFAULT.createMultiCurve(crs, circles.toArray(Circle[]::new));
    }

    /**
     * Returns the circle of the given center and radius in the plane of the two given unit vectors,
     * turning from {@code u} toward {@code w}. The circle is made of four quarter arcs, since a
     * single arc of a circle shall stay below a half turn.
     */
    private static Circle circle(SampleSystem system, final ReadOnly.Vector<?> center, final double radius,
                                 final ReadOnly.Vector<?> u, final ReadOnly.Vector<?> w)
    {
        final var points  = new ArrayList<Vector<?>>(5);
        final var centers = new ArrayList<Vector<?>>(4);
        final ReadOnly.Vector<?>[] directions = {u, w, u.copy().scale(-1), w.copy().scale(-1), u};
        for (final ReadOnly.Vector<?> direction : directions) {
            final Vector<?> p = Vectors.create(center.getDimension(), DataType.DOUBLE);
            p.set(center);
            p.add(direction.copy().scale(radius));
            points.add(p);
        }
        for (int i = 0; i < 4; i++) {
            final Vector<?> c = Vectors.create(center.getDimension(), DataType.DOUBLE);
            c.set(center);
            centers.add(c);
        }
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        return factory.createCircle(
                factory.createDataPoints(NDArrays.of(points, system, DataType.DOUBLE)),
                NDArrays.of(centers, system, DataType.DOUBLE), null);
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getDataPoints().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        getDataPoints().setCoordinateReferenceSystem(crs);
    }

    @Override
    default DataPointsType getDataPointsType() {
        return getDataPoints().getType();
    }

}
