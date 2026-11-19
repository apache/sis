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

import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.Geodesic;
import org.apache.sis.maths.Vector;
import org.apache.sis.referencing.GeodeticCalculator;


/**
 * Default implementation of {@link Geodesic}, storing its data points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultGeodesic extends AbstractGeometry implements Geodesic {

    private final DataPoints points;

    public DefaultGeodesic(DataPoints points) {
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
     * Returns the envelope of the geodesic segments.
     * <p>
     * Longitude changes monotonically along a geodesic segment, but latitude may reach an extreme between
     * the end points, at the vertex of the geodesic. That extreme is computed with Clairaut's relation
     * (sin α cos β is constant, with β the reduced latitude) and included in the envelope.
     * <ul>
     *   <li>In reference systems without ellipsoidal axes, segments are straight and the envelope is the one
     *       of the data points.</li>
     *   <li>Geodesics are computed on the ellipsoid of the geographic component. Other ellipsoidal or spherical
     *       coordinate systems, such as rotated poles, are not supported.</li>
     *   <li>A segment passing over a pole crosses all longitudes.</li>
     *   <li>Data points on a pole take the longitude of the adjacent segment.</li>
     *   <li>The longitude range crosses the anti-meridian (lower &gt; upper) when the geodesic does.</li>
     *   <li>Other dimensions, such as heights, use the range of the data points.</li>
     * </ul>
     *
     * @return envelope of the geodesic, or {@code null} if it has no data point.
     * @throws UnsupportedOperationException if the reference system is not supported.
     * @see ISO 19107:2019 - 7.3.2
     */
    @Override
    public Envelope getEnvelope() {
        if (points.isEmpty()) {
            return null;
        }
        final GeographicAxes axes = GeographicAxes.of(points.getCoordinateReferenceSystem(), Geodesic.TYPE);
        if (axes == null) {
            return points.getEnvelope();
        }
        final GeodeticCalculator calculator = GeodeticCalculator.create(axes.geographic());
        final double axisRatio = axes.axisRatio();
        final HorizontalBounds bounds = axes.start(points);
        double latitude = axes.latitude(points.getPosition(0));
        for (int i = 1, n = points.size(); i < n; i++) {
            final Vector<?> position = points.getPosition(i);
            final double nextLatitude = axes.latitude(position);
            addSegment(calculator, axisRatio, bounds, latitude, nextLatitude, axes.longitude(position));
            latitude = nextLatitude;
        }
        return axes.toEnvelope(points.getEnvelope(), bounds);
    }

    /**
     * Adds to the bounds the geodesic segment from the current point to the given one.
     *
     * @param  calculator  calculator on the ellipsoid of the geodesic.
     * @param  axisRatio   ratio of the semi-minor axis to the semi-major axis of the ellipsoid.
     * @param  bounds      bounds of the previous segments, ending at the current point.
     * @param  latitude    latitude of the current point, in degrees.
     * @param  endLatitude   latitude of the segment end point, in degrees.
     * @param  endLongitude  longitude of the segment end point, in degrees.
     */
    private static void addSegment(final GeodeticCalculator calculator, final double axisRatio,
            final HorizontalBounds bounds, final double latitude, final double endLatitude, final double endLongitude)
    {
        if (HorizontalBounds.isPole(endLatitude)) {
            bounds.moveTo(0, endLatitude);
            return;
        }
        double change = bounds.shorterChangeTo(endLongitude);
        if (HorizontalBounds.isPole(latitude)) {
            // Meridian from the pole, which belongs to all meridians.
            bounds.moveTo(change, endLatitude);
            return;
        }
        calculator.setStartGeographicPoint(latitude, bounds.longitude());
        calculator.setEndGeographicPoint(endLatitude, endLongitude);
        final double startAzimuth = Math.toRadians(calculator.getStartingAzimuth());
        final double endAzimuth   = Math.toRadians(calculator.getEndingAzimuth());
        final double sinStart = Math.sin(startAzimuth);
        final double cosStart = Math.cos(startAzimuth);
        if (cosStart * Math.cos(endAzimuth) < 0) {
            // Latitude increases then decreases, or the opposite: the segment passes the vertex.
            final double reducedLatitude = Math.atan(axisRatio * Math.tan(Math.toRadians(latitude)));
            final double vertexReduced   = Math.acos(Math.min(1, Math.abs(sinStart * Math.cos(reducedLatitude))));
            final double vertexLatitude  = Math.toDegrees(Math.atan(Math.tan(vertexReduced) / axisRatio));
            bounds.addLatitude(cosStart > 0 ? vertexLatitude : -vertexLatitude);
            if (HorizontalBounds.isPole(vertexLatitude)) {
                bounds.coverAllLongitudes();
            }
        }
        if (sinStart > 0 && change < 0) {
            change += 360;
        } else if (sinStart < 0 && change > 0) {
            change -= 360;
        }
        bounds.moveTo(change, endLatitude);
    }

}
