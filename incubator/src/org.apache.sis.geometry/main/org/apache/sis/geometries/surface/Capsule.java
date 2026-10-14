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
import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultCapsule;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;


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
     * @return the center of the bottom sphere
     */
    ReadOnly.Vector<?> getBottom();

    /**
     * @param position new center of the bottom sphere
     */
    void setBottom(ReadOnly.Vector<?> position);

    /**
     * @return the center of the top sphere
     */
    ReadOnly.Vector<?> getTop();

    /**
     * @param position new center of the top sphere
     */
    void setTop(ReadOnly.Vector<?> position);

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
        final ReadOnly.Vector<?> bottom = getBottom();
        final Vector<?> axis = Vectors.create(bottom.getSampleSystem(), DataType.DOUBLE);
        axis.set(getTop());
        axis.subtract(bottom);
        return axis.length();
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
    default BBox getEnvelope() {
        final ReadOnly.Vector<?> bottom = getBottom();
        final ReadOnly.Vector<?> top = getTop();
        final double radiusBottom = getRadiusBottom();
        final double radiusTop = getRadiusTop();
        final int dim = getDataPoints().getDimension();
        final BBox bbox = new BBox(dim);
        for (int i=0;i<dim;i++){
            final double b = bottom.get(i);
            final double t = top.get(i);
            bbox.setRange(i, Math.min(b - radiusBottom, t - radiusTop),
                             Math.max(b + radiusBottom, t + radiusTop));
        }
        return bbox;
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
