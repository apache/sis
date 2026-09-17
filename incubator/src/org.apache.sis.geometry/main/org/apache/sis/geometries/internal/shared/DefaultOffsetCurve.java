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
package org.apache.sis.geometries.internal.shared;

import java.util.Objects;
import javax.measure.Quantity;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Bearing;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.maths.Array;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A curve at a constant distance and bearing from a base curve. All the non-positional attributes,
 * such as the coordinate reference system, are those of the base curve.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultOffsetCurve extends AbstractGeometry implements OffsetCurve {

    /**
     * Curve from which this curve is defined as an offset.
     */
    protected final Curve baseCurve;

    /**
     * Distance at which this curve is generated from the base curve.
     */
    protected final Quantity<?> distance;

    /**
     * Direction in which this curve is offset, or {@code null} if the spatial dimension is 2.
     */
    protected final Bearing refDirection;

    /**
     * Creates a curve offset from the given base curve.
     *
     * @param  baseCurve     curve from which this curve is offset. It shall have a well-defined
     *                       tangent at every position.
     * @param  distance      constant offset distance. In a 2-dimensional coordinate system, a
     *                       positive distance designates the left side of the base curve with
     *                       respect to the tangent, and a negative distance its right side.
     * @param  refDirection  reference direction of the offset, whose cross product with the tangent
     *                       of the base curve gives the offset direction in a 3-dimensional
     *                       coordinate system, or {@code null} if the spatial dimension is 2.
     */
    public DefaultOffsetCurve(final Curve baseCurve, final Quantity<?> distance, final Bearing refDirection) {
        this.baseCurve    = Objects.requireNonNull(baseCurve);
        this.distance     = Objects.requireNonNull(distance);
        this.refDirection = refDirection;
    }

    @Override
    public Curve getBaseCurve() {
        return baseCurve;
    }

    @Override
    public Quantity<?> getDistance() {
        return distance;
    }

    @Override
    public Bearing getRefDirection() {
        return refDirection;
    }

    @Override
    public DataPoints getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Array getControlPoints() {
        return baseCurve.getControlPoints();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return baseCurve.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        baseCurve.setCoordinateReferenceSystem(crs);
    }

    @Override
    public AttributesType getAttributesType() {
        return baseCurve.getAttributesType();
    }

    @Override
    public boolean isEmpty() {
        return baseCurve.isEmpty();
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
