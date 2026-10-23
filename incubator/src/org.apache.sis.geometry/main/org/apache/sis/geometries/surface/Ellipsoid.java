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
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultEllipsoid;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;


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
public sealed interface Ellipsoid extends ParametricCurveSurface
        permits DefaultEllipsoid
{

    /**
     * Returns {@link GeometryType#ELLIPSOID}.
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.ELLIPSOID;
    }

    /**
     * Returns {@code false}: an ellipsoid always has a center.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * Returns {@link SurfaceInterpolation#ELLIPTICAL}: this surface is a section of an ellipsoid.
     *
     * @see ISO 19107:2019 - 8.5.2
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.ELLIPTICAL);
    }

    /**
     * Returns {@link GeometryType#ELLIPTICARC}: the horizontal curves are the ellipses of constant latitude.
     */
    @Override
    default GeometryType getHorizontalCurveType() {
        return GeometryType.ELLIPTICARC;
    }

    /**
     * Returns {@link GeometryType#ELLIPTICARC}: the vertical curves are the lines of constant longitude,
     * which are halves of ellipses going from the south pole to the north pole.
     */
    @Override
    default GeometryType getVerticalCurveType() {
        return GeometryType.ELLIPTICARC;
    }

    /**
     * Returns a copy of the semi-axis lengths, one per axis of the coordinate system.
     * Writing in the returned array does not reshape this ellipsoid;
     * use {@link #setSemiAxes(double...)} for that.
     *
     * @return length of the semi-axis along each axis.
     */
    double[] getSemiAxes();

    /**
     * @param  dimension  index of the axis along which to measure the semi-axis.
     * @return length of the semi-axis along the given axis.
     */
    double getSemiAxis(int dimension);

    /**
     * @param semiAxes new semi-axis lengths, one per dimension. Each of them must be positive.
     * @throws IllegalArgumentException if the number of semi-axes is not the number of dimensions.
     */
    void setSemiAxes(double... semiAxes);

    /**
     * @param dimension index of the axis along which to set the semi-axis.
     * @param length    new semi-axis length, must be positive.
     */
    void setSemiAxis(int dimension, double length);

    /**
     * Returns the ellipsoid center. Writing in the returned tuple does not move this
     * ellipsoid; use {@link #setCenter(Vector)} for that.
     *
     * @return ellipsoid center.
     */
    Vector<?> getCenter();

    /**
     * @param position new center of the ellipsoid
     */
    void setCenter(Vector<?> position);

    /**
     * Returns the empty geometry: an ellipsoid is a closed surface, so it has no edge.
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
     *
     * <p>An ellipsoid reaches its semi-axis away from its center on each axis, so the smallest box
     * containing it is the box of that extent around the center.</p>
     */
    @Override
    default Envelope getEnvelope() {
        final Vector<?> center = getCenter();
        final NDRange env = new NDRange(getCoordinateReferenceSystem());
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double c = center.get(i);
            final double a = getSemiAxis(i);
            env.setRange(i, c-a, c+a);
        }
        return env;
    }

}
