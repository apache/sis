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
import org.apache.sis.geometries.internal.shared.DefaultCylinder;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;


/**
 * A cylinder along the axis joining two positions, with potentially different radii at each end.
 * <p>
 * A cone is a special case of cylinder when one of the radii is zero.
 * <p>
 * Synonym : conical frustrum is a volume defined by 2 circles aligned on the same axis
 *
 * ISO 19107 : classified as a conic surface
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
     * Returns {@link SurfaceInterpolation#CONIC}: ISO 19107 classifies cylinders and cones as conic surfaces.
     *
     * @see ISO 19107:2019 - 8.5.3, 8.5.4
     */
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.CONIC);
    }

    /**
     * Returns {@code false}: a cylinder always has its two circle centers.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * @return the center of the bottom circle
     */
    ReadOnly.Vector<?> getBottom();

    /**
     * @param position new center of the bottom circle
     */
    void setBottom(ReadOnly.Vector<?> position);

    /**
     * @return the center of the top circle
     */
    ReadOnly.Vector<?> getTop();

    /**
     * @param position new center of the top circle
     */
    void setTop(ReadOnly.Vector<?> position);

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
        final ReadOnly.Vector<?> bottom = getBottom();
        final Vector<?> axis = Vectors.create(bottom.getSampleSystem(), DataType.DOUBLE);
        axis.set(getTop());
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
    default BBox getEnvelope() {
        final ReadOnly.Vector<?> bottom = getBottom();
        final ReadOnly.Vector<?> top = getTop();
        final double radiusBottom = getRadiusBottom();
        final double radiusTop = getRadiusTop();
        final Vector<?> axis = getAxis();
        final double height = axis.length();
        final int dim = getDataPoints().getDimension();
        final BBox bbox = new BBox(dim);
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
