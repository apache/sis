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

package org.apache.sis.core;

import org.apache.sis.geometry.DirectPosition2D;

/**
 * Represents 2D point on earth surface by latitude and longitude.
 *
 * @deprecated Replaced by {@link org.opengis.geometry.DirectPosition}, which is derived from OGC/ISO specifications.
 */
@Deprecated
public class LatLon extends DirectPosition2D {

  /**
   * LatLon to represent geo point.
   *
   * @param lat
   *          the latitude
   * @param lon
   *          the longitude
   */
  public LatLon(double lat, double lon) {
    super(lon, lat);
  }

  /**
   * Shifts the latitude by +90.0 so that all latitude lies in the positive
   * coordinate. Used mainly for Java 2D geometry.
   *
   * @return latitude shifted by +90.0
   */
  public double getShiftedLat() {
    return this.y + 90.0;
  }

  /**
   * Shifts the longitude by +180.0 so that all longitude lies in the positive
   * coordinate. Used mainly for Java 2D geometry.
   *
   * @return longitude shifted by +180.0
   */
  public double getShiftedLon() {
    return this.x + 180.0;
  }

  /**
   * Returns the latitude.
   *
   * @return latitude
   */
  public double getLat() {
    return this.y;
  }

  /**
   * Returns the longitude.
   *
   * @return longitude
   */
  public double getLon() {
    return this.x;
  }

  /**
   * Normalizes the longitude values to be between -180.0 and 180.0
   *
   * @return longitude value that is between -180.0 and 180.0 inclusive
   */
  public double getNormLon() {
    double normLon = this.x;
    if (normLon > 180.0) {
      while (normLon > 180.0) {
        normLon -= 360.0;
      }
    } else if (normLon < -180.0) {
      while (normLon < -180.0) {
        normLon += 360.0;
      }
    }
    return normLon;
  }

  @Override
  public String toString() {
    return this.y + "," + this.x;
  }
}
