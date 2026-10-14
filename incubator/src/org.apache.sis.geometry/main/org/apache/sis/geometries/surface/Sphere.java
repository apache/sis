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
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultSphere;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


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
 * called a Sphere this interface can handle 2 to N dimensions.</p>
 *
 * <p>Note: ISO 19107 classifies this geometry as a conic surface. It should not be confused with
 * the sphere used as a geometric reference surface (ISO 19107:2019 - 6.2.3.4).</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.5.1, 8.5.2
 */
@UML(identifier="Sphere", specification=ISO_19107)
public sealed interface Sphere extends ParametricCurveSurface
        permits DefaultSphere
{

    /**
     * Returns {@link GeometryType#SPHERE}.
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.SPHERE;
    }

    /**
     * Returns {@code false}: a sphere always has a center.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * Returns {@link SurfaceInterpolation#SPHERICAL}: this surface is a section of a sphere.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.SPHERICAL);
    }

    /**
     * Returns {@link GeometryType#CIRCLE}: the horizontal curves are the circles of constant latitude.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @Override
    default GeometryType getHorizontalCurveType() {
        return GeometryType.CIRCLE;
    }

    /**
     * Returns {@link GeometryType#CIRCLE}: the vertical curves are the great circles of constant longitude.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @Override
    default GeometryType getVerticalCurveType() {
        return GeometryType.CIRCLE;
    }

    /**
     * Returns the area of this sphere, which is 4πr² in a 3-dimensional space.
     *
     * <p>In an <var>n</var>-dimensional space, this is the measure of the (<var>n</var>−1)-sphere,
     * 2π<sup><var>n</var>/2</sup>∕Γ(<var>n</var>/2)⋅r<sup><var>n</var>−1</sup>, expressed in the
     * linear unit raised to the power <var>n</var>−1. In a 2-dimensional space, it is therefore
     * the perimeter 2πr of the circle.</p>
     */
    @Override
    default Quantity<?> getArea() {
        final int n = getDimension();
        /*
         * Measure of the unit (n−1)-sphere, from S(1) = 2 and S(2) = 2π with the recurrence
         * S(n+2) = 2π⋅S(n)/n, which avoids computing the Gamma function.
         */
        int k = (n % 2 == 0) ? 2 : 1;
        double measure = (k == 2) ? 2*Math.PI : 2;
        for (; k + 2 <= n; k += 2) {
            measure *= 2*Math.PI / k;
        }
        return Quantities.create(measure * Math.pow(getRadius(), n - 1), Geometries.getLinearUnit(this).pow(n - 1));
    }

    /**
     * @return radius of the sphere.
     */
    double getRadius();

    /**
     * @param radius new sphere radius, must be positive.
     */
    void setRadius(double radius);

    /**
     * Returns the sphere center. Writing in the returned tuple does not move this
     * sphere; use {@link #setCenter(ReadOnly.Vector)} for that.
     *
     * @return sphere center.
     */
    ReadOnly.Vector<?> getCenter();

    /**
     * @param position new center of the sphere
     */
    void setCenter(ReadOnly.Vector<?> position);

    /**
     * Returns the empty geometry: a sphere is a closed surface, so it has no edge.
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
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        if (cs.getCoordinateSystem().getDimension() != getCoordinateReferenceSystem().getCoordinateSystem().getDimension()) {
            throw new IllegalArgumentException("New CRS dimension must be the same as previous CRS");
        }
        getDataPoints().setCoordinateReferenceSystem(cs);
    }

    @Override
    default DataPointsType getDataPointsType() {
        return getDataPoints().getType();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    default Envelope getEnvelope() {
        final Vector<?> center = getCenter().copy();
        final double radius = getRadius();
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

}
