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

import javax.measure.Unit;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.math.Array;


/**
 * A single circular arc described by the centre of its circle, the radius of that circle and the
 * angles at which the arc starts and ends.
 *
 * <p>This is the parameterisation that GML calls {@code gml:ArcByCenterPoint}, and it is a
 * genuinely different one from {@link CircularString}'s: a circular string carries points
 * <em>on</em> the curve and computes the circle from them, whereas here the circle is given and the
 * points on the curve have to be computed from it. Neither can be converted to the other without
 * either solving for a circle or evaluating trigonometric functions, which is why the two coexist
 * rather than one being expressed in terms of the other.</p>
 *
 * <p>Because the arc is defined by a bearing sweep around a centre, this parameterisation is
 * two-dimensional by nature; GML says as much.</p>
 *
 * @author Johann Sorel (Geomatys)
 * @see GML ArcByCenterPoint
 */
public interface ArcByCenterPoint extends Curve {

    public static final String TYPE = "ARCBYCENTERPOINT";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns the centre of the circle this arc is a part of.
     *
     * @return the centre point, never null.
     */
    Point getCenter();

    /**
     * Returns the radius of the circle this arc is a part of,
     * expressed in {@linkplain #getRadiusUnit() its unit}.
     *
     * @return the radius, always greater than zero.
     */
    double getRadius();

    /**
     * Returns the unit the {@linkplain #getRadius() radius} is expressed in, or {@code null} if
     * unspecified. A {@code null} unit means that the radius is expressed in the units of the
     * coordinate system axes, which is what a document declaring no unit leaves implied.
     *
     * <p>Unlike the angles, the radius is <em>not</em> normalised to a canonical unit: there is
     * none. A radius is a length in the coordinate system this arc lives in, and that system may
     * measure its axes in metres, in feet or in degrees of arc; converting to any one of those
     * would be meaningless for the others.</p>
     *
     * @return the unit of the radius, or {@code null} if the radius is in coordinate system units.
     */
    Unit<?> getRadiusUnit();

    /**
     * Returns the bearing at which the arc starts, in decimal degrees.
     *
     * @return the start angle, in decimal degrees.
     */
    double getStartAngle();

    /**
     * Returns the bearing at which the arc ends, in decimal degrees.
     *
     * @return the end angle, in decimal degrees.
     */
    double getEndAngle();

    /**
     * Returns {@link CurveInterpolation#CIRCULAR}.
     */
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.CIRCULAR;
    }

    @Override
    public default Array getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    /**
     * @return null, a ArcByCenterPoint has no control points
     */
    @Override
    public default Array getControlPoints() {
        return null;
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getCenter().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        getCenter().setCoordinateReferenceSystem(cs);
    }

    @Override
    default AttributesType getAttributesType() {
        return getCenter().getAttributesType();
    }

    /**
     * Returns {@code false}: an arc by centre point always has a centre and a radius,
     * so it is never the empty point set.
     */
    @Override
    default boolean isEmpty() {
        return false;
    }
}
