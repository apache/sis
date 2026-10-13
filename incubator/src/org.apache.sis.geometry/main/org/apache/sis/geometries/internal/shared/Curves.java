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

import java.util.LinkedHashMap;
import java.util.Map;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.apache.sis.geometries.AttributeInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Units;


/**
 * Helper methods for locating positions at a distance along a curve.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Curves {

    private Curves() {
    }

    /**
     * Returns whether the given distance is expressed in percent of the curve length.
     *
     * @param  dist  the distance along a curve.
     * @return whether the distance is a percentage.
     */
    public static boolean isPercent(final Quantity<?> dist) {
        return Units.PERCENT.equals(dist.getUnit());
    }

    /**
     * Converts the given quantity to the given unit.
     *
     * @param  q     the quantity to convert.
     * @param  unit  the target unit.
     * @return the value of the quantity in the target unit.
     * @throws IllegalArgumentException if the quantity cannot be expressed in the target unit.
     */
    public static double convert(final Quantity<?> q, final Unit<?> unit) {
        try {
            return q.getUnit().getConverterToAny(unit).convert(q.getValue().doubleValue());
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException("Cannot express " + q + " in " + unit + '.', e);
        }
    }

    /**
     * Converts a distance along a curve to an arc length in the unit of the curve length.
     * A distance in {@linkplain Units#PERCENT percent} is relative to the given length.
     *
     * @param  dist    the distance along the curve.
     * @param  length  the length of the curve, in {@code unit}.
     * @param  unit    the unit of the curve length.
     * @return the arc length, between 0 and {@code length} inclusive.
     * @throws IllegalArgumentException if the distance cannot be expressed in the length unit,
     *         or is outside the curve.
     */
    public static double toArcLength(final Quantity<?> dist, final double length, final Unit<?> unit) {
        double s = isPercent(dist) ? dist.getValue().doubleValue() / 100 * length : convert(dist, unit);
        // Rounding errors of unit conversions and of length sums are not a distance outside the curve.
        final double tolerance = length * 1E-12;
        if (s < 0 && s >= -tolerance) {
            s = 0;
        } else if (s > length && s <= length + tolerance) {
            s = length;
        }
        if (!(s >= 0 && s <= length)) {
            throw new IllegalArgumentException("The distance " + dist + " is outside the curve, whose length is "
                    + length + ' ' + unit + '.');
        }
        return s;
    }

    /**
     * Returns the point at the given distance along the line segments joining consecutive points.
     *
     * @param  points  the points of the polyline.
     * @param  dist    the distance along the polyline, in {@code unit} or in percent.
     * @param  unit    the unit of the coordinates.
     * @return the point at the given distance, or {@code null} if there are no points.
     * @throws IllegalArgumentException if the distance is outside the polyline.
     */
    public static Point alongPolyline(final DataPoints points, final Quantity<?> dist, final Unit<?> unit) {
        final int n = points.size();
        if (n == 0) {
            return null;
        }
        final double[] segments = new double[n - 1];
        double total = 0;
        for (int i=0; i<segments.length; i++) {
            segments[i] = points.getPosition(i).subtract(points.getPosition(i+1)).length();
            total += segments[i];
        }
        double s = toArcLength(dist, total, unit);
        for (int i=0; i<segments.length; i++) {
            final double length = segments[i];
            if (s <= length || i == segments.length - 1) {
                final double ratio = (length > 0) ? Math.min(s / length, 1) : 0;
                return interpolate(points, i, i+1, ratio);
            }
            s -= length;
        }
        return interpolate(points, 0, 0, 0);    // A single position, at distance zero.
    }

    /**
     * Returns a new point on the segment joining two points of the given sequence.
     * The position is linearly interpolated, and each other attribute is computed as declared by
     * its {@linkplain DataPointsType#getAttributeInterpolation(String) attribute interpolation}.
     *
     * @param  points  the sequence holding the two points.
     * @param  i0      index of the point at ratio 0, the start of the segment.
     * @param  i1      index of the point at ratio 1, the end of the segment.
     * @param  ratio   position between the two points, from 0 to 1.
     * @return a new point holding the computed attributes, independent of the given sequence.
     */
    public static Point interpolate(final DataPoints points, final int i0, final int i1, final double ratio) {
        final DataPointsType type = points.getType();
        final Map<String,Array> attributes = new LinkedHashMap<>();
        for (final String name : type.getAttributeNames()) {
            final AttributeInterpolation method = DataPointsType.ATT_POSITION.equals(name)
                    ? AttributeInterpolation.LINEAR : type.getAttributeInterpolation(name);
            final Vector<?> value;
            switch ((method != null) ? method : AttributeInterpolation.NEAREST) {
                case LINEAR:
                case TRANSLATE: value = points.getAttribute(i0, name).lerp(points.getAttribute(i1, name), ratio); break;
                case LEADING:   value = points.getAttribute(ratio < 1 ? i0 : i1, name); break;
                default:        value = points.getAttribute(ratio <= 0.5 ? i0 : i1, name); break;
            }
            final Array array = NDArrays.of(type.getAttributeSystem(name), type.getAttributeType(name), 1);
            array.set(0, value);
            attributes.put(name, array);
        }
        return GeometryFactory.DEFAULT.createPoint(GeometryFactory.DEFAULT.createDataPoints(attributes));
    }
}
