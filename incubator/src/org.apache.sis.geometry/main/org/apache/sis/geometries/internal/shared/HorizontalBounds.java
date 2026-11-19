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


/**
 * Longitude and latitude ranges swept by a curve on an ellipsoid, accumulated segment by segment.
 * <p>
 * Longitudes are tracked without wrapping: each segment adds its signed longitude change to the current
 * longitude. Since the curve is continuous, the swept longitudes are the range between the lowest and the
 * highest tracked values, which may cross the anti-meridian once normalized.
 * <ul>
 *   <li>All values are in degrees, longitudes positive east and latitudes positive north.</li>
 *   <li>A segment sweeping longitudes in both directions, as a geodesic over a pole, must declare
 *       {@linkplain #coverAllLongitudes() all longitudes}.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class HorizontalBounds {

    /**
     * Tolerance in degrees for considering a latitude on a pole.
     */
    private static final double POLE_TOLERANCE = 1e-10;

    /**
     * Current longitude, without wrapping.
     */
    private double longitude;

    private double minLongitude;
    private double maxLongitude;
    private double south;
    private double north;
    private boolean allLongitudes;

    /**
     * Starts the bounds at the first point of a curve.
     *
     * @param  longitude  longitude of the first point.
     * @param  latitude   latitude of the first point.
     */
    HorizontalBounds(final double longitude, final double latitude) {
        this.longitude = longitude;
        minLongitude   = longitude;
        maxLongitude   = longitude;
        south          = latitude;
        north          = latitude;
    }

    /**
     * Returns the longitude of the current point, without wrapping.
     */
    double longitude() {
        return longitude;
    }

    /**
     * Returns the signed longitude change from the current point to the given longitude, going the shorter way.
     * When both ways have the same length, the change is eastward (+180°).
     */
    double shorterChangeTo(final double target) {
        return shorterChange(longitude, target);
    }

    /**
     * Returns the signed longitude change between the given longitudes, going the shorter way.
     * When both ways have the same length, the change is eastward (+180°).
     */
    static double shorterChange(final double source, final double target) {
        final double change = Math.IEEEremainder(target - source, 360);
        return (change == -180) ? 180 : change;
    }

    /**
     * Adds a latitude reached between two points, such as the vertex of a geodesic.
     */
    void addLatitude(final double latitude) {
        south = Math.min(south, latitude);
        north = Math.max(north, latitude);
    }

    /**
     * Moves to the next point of the curve.
     *
     * @param  longitudeChange  signed longitude change from the current point, in the direction of the curve.
     * @param  latitude         latitude of the next point.
     */
    void moveTo(final double longitudeChange, final double latitude) {
        longitude   += longitudeChange;
        minLongitude = Math.min(minLongitude, longitude);
        maxLongitude = Math.max(maxLongitude, longitude);
        addLatitude(latitude);
    }

    /**
     * Declares that the curve crosses all meridians.
     */
    void coverAllLongitudes() {
        allLongitudes = true;
    }

    /**
     * Returns whether the curve crosses all meridians.
     */
    boolean spansAllLongitudes() {
        return allLongitudes || maxLongitude - minLongitude >= 360;
    }

    /**
     * Returns the west bound, normalized to [−180 … 180)°. It is −180° when all longitudes are crossed.
     */
    double west() {
        return spansAllLongitudes() ? -180 : normalize(minLongitude);
    }

    /**
     * Returns the east bound, normalized to (−180 … 180]°. It is 180° when all longitudes are crossed,
     * and it is less than the {@linkplain #west() west bound} when the range crosses the anti-meridian.
     */
    double east() {
        if (spansAllLongitudes()) {
            return 180;
        }
        final double east = west() + (maxLongitude - minLongitude);
        return (east > 180) ? east - 360 : east;
    }

    double south() {
        return south;
    }

    double north() {
        return north;
    }

    /**
     * Returns whether the given latitude is on a pole, where longitudes are undefined.
     */
    static boolean isPole(final double latitude) {
        return Math.abs(latitude) >= 90 - POLE_TOLERANCE;
    }

    /**
     * Normalizes a longitude to the [−180 … 180)° range.
     */
    private static double normalize(final double value) {
        final double result = value - 360 * Math.floor((value + 180) / 360);
        return (result >= 180) ? result - 360 : result;
    }
}
