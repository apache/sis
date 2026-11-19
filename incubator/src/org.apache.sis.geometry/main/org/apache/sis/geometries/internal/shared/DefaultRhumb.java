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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.cs.Bearing;
import org.apache.sis.geometries.cs.FixedDirection;
import org.apache.sis.geometries.cs.Rotation;
import org.apache.sis.geometries.curve.Rhumb;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Sample;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.GeodeticCalculator;


/**
 * Default implementation of {@link Rhumb}, storing its data points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultRhumb extends AbstractGeometry implements Rhumb {

    private final DataPoints points;

    public DefaultRhumb(DataPoints points) {
        this.points = points;
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    /**
     * Returns the envelope of the rhumb segments.
     * <p>
     * Latitude and longitude change monotonically along a rhumb segment, so the envelope of a segment is the one
     * of its end points, with the longitude range going the shorter way around the globe.
     * <ul>
     *   <li>In reference systems without ellipsoidal axes, segments are straight and the envelope is the one
     *       of the data points.</li>
     *   <li>Other ellipsoidal or spherical coordinate systems than geographic ones, such as rotated poles,
     *       are not supported.</li>
     *   <li>A segment whose end points are 180° of longitude apart goes east.</li>
     *   <li>Data points on a pole take the longitude of the adjacent segment.</li>
     *   <li>The longitude range crosses the anti-meridian (lower &gt; upper) when the rhumb does.</li>
     *   <li>Other dimensions, such as heights, use the range of the data points.</li>
     * </ul>
     *
     * @return envelope of the rhumb, or {@code null} if it has no data point.
     * @throws UnsupportedOperationException if the reference system is not supported.
     * @see ISO 19107:2019 - 7.5.2
     */
    @Override
    public Envelope getEnvelope() {
        if (points.isEmpty()) {
            return null;
        }
        final GeographicAxes axes = GeographicAxes.of(points.getCoordinateReferenceSystem(), Rhumb.TYPE);
        if (axes == null) {
            return points.getEnvelope();
        }
        final HorizontalBounds bounds = axes.start(points);
        for (int i = 1, n = points.size(); i < n; i++) {
            final Vector<?> position = points.getPosition(i);
            final double latitude = axes.latitude(position);
            if (HorizontalBounds.isPole(latitude)) {
                bounds.moveTo(0, latitude);
            } else {
                bounds.moveTo(bounds.shorterChangeTo(axes.longitude(position)), latitude);
            }
        }
        return axes.toEnvelope(points.getEnvelope(), bounds);
    }

    /**
     * Returns the constant azimuth followed by the given rhumb segment.
     * <p>
     * The azimuth is measured clockwise from true north, on the ellipsoid of the geographic component.
     * <ul>
     *   <li>In reference systems without ellipsoidal axes, segments are straight. The bearing is their direction,
     *       measured clockwise from grid north.</li>
     *   <li>Other ellipsoidal or spherical coordinate systems than geographic ones, such as rotated poles,
     *       are not supported.</li>
     *   <li>A segment whose end points are 180° of longitude apart goes east.</li>
     *   <li>A segment from or to a pole follows a meridian.</li>
     *   <li>On the ellipsoid, other dimensions such as heights are ignored.</li>
     * </ul>
     *
     * @param  segment  index of the segment, which goes from data point {@code segment} to the next one.
     * @return bearing of the segment, or {@code null} if the segment has no length.
     * @throws IndexOutOfBoundsException if the index is negative or not less than the number of segments.
     * @throws UnsupportedOperationException if the reference system is not supported.
     * @see ISO 19107:2019 - 7.5.2
     */
    @Override
    public Bearing getBearing(final int segment) {
        Objects.checkIndex(segment, points.size() - 1);
        final Vector<?> start = points.getPosition(segment);
        final Vector<?> end   = points.getPosition(segment + 1);
        final GeographicAxes axes = GeographicAxes.of(points.getCoordinateReferenceSystem(), Rhumb.TYPE);
        return (axes != null) ? azimuthOnEllipsoid(axes, start, end) : straightDirection(start, end);
    }

    /**
     * Returns the azimuth of the rhumb line between the given points, going the shorter way around the globe.
     *
     * @return the bearing, or {@code null} if both points are the same.
     */
    private static Bearing azimuthOnEllipsoid(final GeographicAxes axes, final Vector<?> start, final Vector<?> end) {
        final double latitude    = axes.latitude(start);
        final double endLatitude = axes.latitude(end);
        final boolean fromPole   = HorizontalBounds.isPole(latitude);
        final boolean toPole     = HorizontalBounds.isPole(endLatitude);
        final double azimuth;
        if (fromPole || toPole) {
            if (fromPole && toPole && (latitude > 0) == (endLatitude > 0)) {
                return null;
            }
            azimuth = (endLatitude > latitude) ? 0 : 180;
        } else {
            final double longitude    = axes.longitude(start);
            final double endLongitude = axes.longitude(end);
            final double change = HorizontalBounds.shorterChange(longitude, endLongitude);
            if (change == 0 && endLatitude == latitude) {
                return null;
            }
            final GeodeticCalculator calculator = GeodeticCalculator.create(axes.geographic());
            calculator.setStartGeographicPoint(latitude, longitude);
            calculator.setEndGeographicPoint(endLatitude, endLongitude);
            // The calculator may go west for a 180° change, so the side is taken from the change.
            azimuth = Math.copySign(Math.abs(calculator.getConstantAzimuth()), change);
        }
        return Bearing.ofAzimuth(Quantities.create(azimuth, Units.DEGREE));
    }

    /**
     * Returns the direction of the straight segment between the given points.
     *
     * @return the bearing, or {@code null} if both points are the same.
     */
    private Bearing straightDirection(final Vector<?> start, final Vector<?> end) {
        final Vector<?> step = end.copy(DataType.DOUBLE).subtract(start);
        if (step.length() == 0) {
            return null;
        }
        final CoordinateReferenceSystem crs = points.getCoordinateReferenceSystem();
        final SampleSystem system = (crs != null) ? SampleSystem.of(crs) : SampleSystem.ofSize(step.getDimension());
        return Bearing.ofDirection(new Sample(system, step), FixedDirection.GRID_NORTH, Rotation.CLOCKWISE);
    }

}
