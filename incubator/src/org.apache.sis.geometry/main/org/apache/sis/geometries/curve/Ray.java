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
package org.apache.sis.geometries.curve;

import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.DefaultRay;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.measure.Quantities;


/**
 * A ray is a one direction infinite line.
 * It is build from a position and a direction.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getDataPoints() data points} hold exactly one position,
 *       the {@linkplain #getStartPoint() start point} of the ray.</li>
 *   <li>The ray has no end point and its length is infinite.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface Ray extends Curve
        permits DefaultRay
{

    /**
     * Returns {@link GeometryType#RAY}.
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.RAY;
    }

    /**
     * Returns the position the ray starts from.
     *
     * @return the start position of this ray.
     */
    ReadOnly.Vector<?> getPosition();

    /**
     * Sets the position the ray starts from.
     *
     * @param  position  the new start position, not null.
     */
    void setPosition(ReadOnly.Vector<?> position);

    /**
     * Returns the direction the ray extends toward.
     *
     * @return the direction of this ray.
     */
    ReadOnly.Vector<?> getDirection();

    /**
     * Sets the direction the ray extends toward.
     *
     * @param  direction  the new direction, not null.
     */
    void setDirection(ReadOnly.Vector<?> direction);

    /**
     * Returns {@code false}: a ray always has a start position.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }

    /**
     * Returns {@link CurveInterpolation#LINEAR}.
     */
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.LINEAR;
    }

    /**
     * Returns null, a ray has no control points.
     */
    @Override
    default Array getControlPoints() {
        return null;
    }

    /**
     * Returns an infinite length, in the linear unit of the reference system of this ray.
     */
    @Override
    default Quantity<?> getLength() {
        return Quantities.create(Double.POSITIVE_INFINITY, Geometries.getLinearUnit(this));
    }

    /**
     * Returns {@code null}: the ray extends to infinity, it has no end point.
     */
    @Override
    default Point getEndPoint() {
        return null;
    }

    /**
     * Returns the {@linkplain #getStartPoint() start point}, which lies on the ray.
     */
    @Override
    default Point getRepresentativePoint() {
        return getStartPoint();
    }

    /**
     * Returns {@code false}: the ray extends to infinity and never comes back to its start point.
     */
    @Override
    default boolean isClosed() {
        return false;
    }

    /**
     * Returns {@code true}: a straight half-line never crosses itself.
     */
    @Override
    default boolean isSimple() {
        return true;
    }

    /**
     * Returns the position the ray starts from: the ray extends to infinity
     * in its direction, so its origin is its only end.
     */
    @Override
    default Point getBoundary() {
        return getStartPoint();
    }

    /**
     * Returns the envelope of this ray, infinite on each axis where the direction is not zero.
     */
    @Override
    default BBox getEnvelope() {
        final ReadOnly.Vector<?> position = getPosition();
        final ReadOnly.Vector<?> direction = getDirection();
        final int dim = direction.getDimension();
        final BBox bbox = new BBox(getCoordinateReferenceSystem());
        for (int i=0;i<dim;i++){
            double d = direction.get(i);
            if (d<0){
                bbox.setRange(i, Double.NEGATIVE_INFINITY, position.get(i));
            } else if (d>0){
                bbox.setRange(i, position.get(i), Double.POSITIVE_INFINITY);
            } else {
                bbox.setRange(i, position.get(i), position.get(i));
            }
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
