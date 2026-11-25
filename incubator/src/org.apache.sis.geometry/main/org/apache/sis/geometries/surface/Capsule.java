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
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Representation;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultCapsule;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * A capsule (cylinder with hemispherical ends) defined by two "capping" spheres centered on two
 * positions, with potentially different radii.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getDataPoints() data points} hold exactly two positions:
 *       the center of the bottom sphere, then the center of the top one.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_12113 KHR_implicit_shapes extension Capsule
 */
@UML(identifier="Capsule", specification=ISO_12113)
public sealed interface Capsule extends Surface
        permits DefaultCapsule
{

    /**
     * Returns {@link GeometryType#CAPSULE}.
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.CAPSULE;
    }

    /**
     * @return a Capsule is ANALYTIC
     */
    @Override
    default Representation getRepresentation() {
        return Representation.ANALYTIC;
    }

    /**
     * Returns {@code false}: a capsule always has its two sphere centers.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * Returns {@link SurfaceInterpolation#SPHERICAL} for the two hemispherical ends
     * and {@link SurfaceInterpolation#CONIC} for the band joining them.
     */
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.SPHERICAL, SurfaceInterpolation.CONIC);
    }

    /**
     * @return capsule top sphere radius
     */
    double getRadiusTop();

    /**
     * @param radius new capsule top radius
     */
    void setRadiusTop(double radius);

    /**
     * @return capsule bottom sphere radius
     */
    double getRadiusBottom();

    /**
     * @param radius new capsule bottom radius
     */
    void setRadiusBottom(double radius);

    /**
     * The height is the length of the axis going from the center of the bottom sphere to the
     * center of the top one, so it is not set but derived from the two positions this capsule
     * is built on. It does not include the two hemispherical ends.
     *
     * @return the capsule height
     */
    default double getHeight() {
        final Vector<?> bottom = getDataPoints().getPosition(0);
        return getDataPoints().getPosition(1).copy(DataType.DOUBLE).subtract(bottom).length();
    }

    /**
     * Returns the area of this capsule.
     * A capsule is the convex hull of its two spheres: a band of cone tangent to both spheres,
     * between a cap of each sphere. With <var>d</var> the {@linkplain #getHeight() height} and
     * <var>s</var> = (r₁ − r₂) ∕ <var>d</var> the sine of the cone half-angle, the area is the sum of:
     *
     * <ul>
     *   <li>the bottom cap, 2πr₁²(1 + <var>s</var>),</li>
     *   <li>the top cap, 2πr₂²(1 − <var>s</var>),</li>
     *   <li>the band, π(r₁ + r₂)<var>d</var>(1 − <var>s</var>²).</li>
     * </ul>
     *
     * <p>A capsule whose larger sphere contains the smaller one is that larger sphere.</p>
     *
     * @throws UnsupportedOperationException if this capsule is not 3-dimensional.
     */
    @Override
    default Quantity<?> getArea() {
        final double r1 = getRadiusBottom();
        final double r2 = getRadiusTop();
        final double[] areas = areas(r1, r2, getHeight());
        final double area;
        if (areas != null) {
            area = areas[0] + areas[1] + areas[2];
        } else {
            final double r = Math.max(r1, r2);
            area = 4 * r*r;
        }
        return Quantities.create(Math.PI * area, Geometries.getAreaUnit(this));
    }

    /**
     * Returns the volume enclosed by this capsule. With the notation of {@link #getArea()},
     * the volume is the sum of:
     *
     * <ul>
     *   <li>the bottom spherical cap, πr₁³(1 + <var>s</var>)²(2 − <var>s</var>)∕3,</li>
     *   <li>the top spherical cap, πr₂³(1 − <var>s</var>)²(2 + <var>s</var>)∕3,</li>
     *   <li>the frustum between them, π<var>d</var>(1 − <var>s</var>²)²(r₁² + r₁r₂ + r₂²)∕3.</li>
     * </ul>
     *
     * <p>Not in ISO 19107, which defines volumes on solids only.
     * A capsule whose larger sphere contains the smaller one encloses the ball of that larger sphere.</p>
     *
     * @return volume enclosed by this capsule.
     * @throws UnsupportedOperationException if this capsule is not 3-dimensional.
     */
    default Quantity<?> getEnclosedVolume() {
        final double r1 = getRadiusBottom();
        final double r2 = getRadiusTop();
        final double d  = getHeight();
        final double volume;
        if (areas(r1, r2, d) != null) {     // Also verifies that this capsule is 3-dimensional.
            final double s = (r1 - r2) / d;
            final double c = 1 - s*s;
            volume = r1*r1*r1 * (1 + s) * (1 + s) * (2 - s)
                   + r2*r2*r2 * (1 - s) * (1 - s) * (2 + s)
                   + d * c * c * (r1*r1 + r1*r2 + r2*r2);
        } else {
            final double r = Math.max(r1, r2);
            volume = 4 * r*r*r;
        }
        return Quantities.create(Math.PI * volume / 3, Geometries.getVolumeUnit(this));
    }

    /**
     * Returns the centroid of this surface, which is on the axis of this capsule.
     * The two data points are interpolated, so the centroid gets their attributes.
     *
     * <ul>
     *   <li>The centroid is the mean of the centroids of the two caps and of the band
     *       described in {@link #getArea()}, weighted by their areas.</li>
     *   <li>It may be outside the segment joining the two data points when the radii differ.</li>
     *   <li>A capsule whose larger sphere contains the smaller one has the center of that sphere as centroid.</li>
     *   <li>A capsule without area has its centroid at the middle of its axis.</li>
     * </ul>
     *
     * @throws UnsupportedOperationException if this capsule is not 3-dimensional.
     *
     * @see "ISO 19107:2019 section 6.4.4.8"
     */
    @Override
    default Point getCentroid() {
        final double r1 = getRadiusBottom();
        final double r2 = getRadiusTop();
        final double d  = getHeight();
        final double[] areas = areas(r1, r2, d);
        final double ratio;
        if (areas == null) {
            ratio = (r1 >= r2) ? 0 : 1;
        } else {
            final double area = areas[0] + areas[1] + areas[2];
            if (area > 0) {
                /*
                 * Positions along the axis, from the bottom center, of the centroids of each part.
                 * The area of a spherical zone is uniform along the axis, so the centroid of a cap
                 * is in the middle of its extent. The band is the lateral surface of a frustum.
                 */
                final double s = (r1 - r2) / d;
                final double bottom = r1 * (s - 1) / 2;
                final double top    = d + r2 * (s + 1) / 2;
                final double band   = r1 * s + d * (1 - s*s) * (r1 + 2*r2) / (3 * (r1 + r2));
                ratio = (areas[0] * bottom + areas[1] * top + areas[2] * band) / (area * d);
            } else {
                ratio = 0.5;
            }
        }
        return getDataPoints().getInterpolatedPoint(0, 1, ratio);
    }

    /**
     * Returns the areas divided by π of the bottom cap, the top cap and the band joining them,
     * or {@code null} if the larger sphere contains the smaller one.
     *
     * @throws UnsupportedOperationException if this capsule is not 3-dimensional.
     */
    private double[] areas(final double r1, final double r2, final double d) {
        if (getDataPoints().getDimension() != 3) {
            throw new UnsupportedOperationException("The measures of a capsule are computed in 3 dimensions only.");
        }
        if (d <= Math.abs(r1 - r2)) {
            return null;
        }
        final double s = (r1 - r2) / d;
        return new double[] {
            2 * r1*r1 * (1 + s),
            2 * r2*r2 * (1 - s),
            (r1 + r2) * d * (1 - s*s)
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>A capsule is the convex hull of its two capping spheres, so it reaches the whole radius
     * of each of them on every axis. Contrarily to a {@linkplain Cylinder cylinder}, whose flat
     * ends spread less than their radius on the axis they face, a capsule is therefore bounded
     * by the union of the two boxes bounding its spheres.</p>
     */
    @Override
    default Envelope getEnvelope() {
        final Vector<?> bottom = getDataPoints().getPosition(0);
        final Vector<?> top = getDataPoints().getPosition(1);
        final double radiusBottom = getRadiusBottom();
        final double radiusTop = getRadiusTop();
        final int dim = getDataPoints().getDimension();
        final NDRange bbox = new NDRange(getCoordinateReferenceSystem());
        for (int i=0;i<dim;i++){
            final double b = bottom.get(i);
            final double t = top.get(i);
            bbox.setRange(i, Math.min(b - radiusBottom, t - radiusTop),
                             Math.max(b + radiusBottom, t + radiusTop));
        }
        return bbox;
    }

    /**
     * Returns the empty geometry: a capsule is a closed surface, so it has no edge.
     */
    @Override
    default Geometry getBoundary() {
        return GeometryFactory.DEFAULT.createEmpty(getCoordinateReferenceSystem());
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
