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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Primitive;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.Array;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;


/**
 * A curve traversed in the opposite direction to the one it is defined in.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultReversedCurve extends AbstractGeometry implements Curve {

    private final Curve base;

    public DefaultReversedCurve(Curve base) {
        this.base = Objects.requireNonNull(base);
    }

    @Override
    public Sign getOrientationSign() {
        return Sign.NEGATIVE;
    }

    @Override
    public Primitive getPrimitive() {
        return base;
    }

    @Override
    public Orientable getProxy() {
        return base;
    }

    @Override
    public Curve getReverse() {
        return base;
    }

    /**
     * Returns the geometry type of the base curve: reversing a curve does not change what kind of
     * curve it is.
     */
    @Override
    public GeometryType getGeometryType() {
        return base.getGeometryType();
    }

    /**
     * Returns the end point of the base curve, which is where this curve starts.
     */
    @Override
    public Point getStartPoint() {
        return base.getEndPoint();
    }

    /**
     * Returns the start point of the base curve, which is where this curve ends.
     */
    @Override
    public Point getEndPoint() {
        return base.getStartPoint();
    }

    @Override
    public CurveInterpolation getInterpolation() {
        return base.getInterpolation();
    }

    @Override
    public Quantity<?> getLength() {
        return base.getLength();
    }

    /**
     * Returns the point of the base curve at the given distance from its end, which is where this
     * curve starts.
     */
    @Override
    public Point subCurve(Quantity<?> dist) {
        if (Curves.isPercent(dist)) {
            return base.subCurve(Quantities.create(100 - dist.getValue().doubleValue(), Units.PERCENT));
        }
        final Quantity<?> length = base.getLength();
        final double s = Curves.toArcLength(dist, length.getValue().doubleValue(), length.getUnit());
        return base.subCurve(Quantities.create(length.getValue().doubleValue() - s, length.getUnit()));
    }

    @Override
    public boolean isClosed() {
        return base.isClosed();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return base.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        base.setCoordinateReferenceSystem(cs);
    }

    @Override
    public DataPointsType getDataPointsType() {
        return base.getDataPointsType();
    }

    @Override
    public Envelope getEnvelope() {
        return base.getEnvelope();
    }

    @Override
    public Array getControlPoints() {
        return base.getControlPoints();
    }

    @Override
    public DataPoints getDataPoints() {
        return base.getDataPoints();
    }

    /**
     * Returns the knots of the base curve: reversing a curve keeps its construction parameterization.
     */
    @Override
    public double[] getKnots() {
        return base.getKnots();
    }

    /**
     * Returns the end construction parameter of the base curve, which is where this curve starts.
     */
    @Override
    public double getStartConstrParam() {
        return base.getEndConstrParam();
    }

    /**
     * Returns the start construction parameter of the base curve, which is where this curve ends.
     */
    @Override
    public double getEndConstrParam() {
        return base.getStartConstrParam();
    }

    /**
     * Returns the polyline of the base curve with its points in reverse order.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        return CurveLinearizer.reverse(this, base, spacing, offset);
    }
}
