/**
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

package org.apache.sis.distance;

// SIS imports
import org.apache.sis.geometry.DirectPosition2D;

/**
 * Class to calculate distances on earth surface. Actual calculation code very
 * similar to Apache SIS but refractor to allow use of custom classes.
 *
 * <div class="warning"><b>Warning:</b> This class may change in a future SIS version. Current implementation
 * performs computations on a sphere of hard-coded radius. A future implementation should perform computations
 * on a given ellipsoid.</div>
 */
public class DistanceUtils {

  public static final int EARTH_RADIUS = 6371; // in km
  public static final double HALF_EARTH_CIRCUMFERENCE = 20037.58; // in km

  /**
   * Returns a coordinate on the great circle at the specified bearing.
   *
   * @param latitude
   *          the latitude of center of circle
   * @param longitude
   *          the longitude of center of circle
   * @param d
   *          the distance from the center
   * @param bearing
   *          the great circle bearing
   * @return a coordinate at the specified bearing
   */
  public static DirectPosition2D getPointOnGreatCircle(double latitude, double longitude,
      double d, double bearing) {
    double angularDistance = d / EARTH_RADIUS;

    double lon1 = Math.toRadians(longitude);
    double lat1 = Math.toRadians(latitude);

    double cosLat = Math.cos(lat1);
    double sinLat = Math.sin(lat1);

    double sinD = Math.sin(angularDistance);
    double cosD = Math.cos(angularDistance);
    double brng = Math.toRadians(bearing);

    double lat2 = Math.asin((sinLat * cosD) + (cosLat * sinD * Math.cos(brng)));
    double lon2 = lon1
        + Math.atan2(Math.sin(brng) * sinD * cosLat, cosD - sinLat
            * Math.sin(lat2));

    return new DirectPosition2D(Math.toDegrees(lat2), Math.toDegrees(lon2));
  }

  /**
   * Calculates haversine (great circle) distance between two lat/lon
   * coordinates.
   *
   * @param latitude1
   *          latitude of first coordinate
   * @param longitude1
   *          longitude of first coordinate
   * @param latitude2
   *          latitude of second coordinate
   * @param longitude2
   *          longitude of second coordinate
   * @return great circle distance between specified lat/lon coordinates
   */
  public static double getHaversineDistance(double latitude1,
      double longitude1, double latitude2, double longitude2) {
    double longRadian1 = Math.toRadians(longitude1);
    double latRadian1 = Math.toRadians(latitude1);
    double longRadian2 = Math.toRadians(longitude2);
    double latRadian2 = Math.toRadians(latitude2);
    double angularDistance = Math.acos(Math.sin(latRadian1)
        * Math.sin(latRadian2) + Math.cos(latRadian1) * Math.cos(latRadian2)
        * Math.cos(longRadian1 - longRadian2));
    return EARTH_RADIUS * angularDistance;
  }
}
