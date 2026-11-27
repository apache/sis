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
package org.apache.sis.referencing.dggs.isea.geometry;

import org.apache.sis.referencing.dggs.isea.generic.Trigonometric;
import org.apache.sis.referencing.dggs.isea.geo.WGS84;

/**
 * Geographic coordinates of a location on Earth.
 *
 * @author Franz-Benjamin Mocnik
 */
public class GeoCoordinates implements Comparable<GeoCoordinates> {
    private final Double _lat;
    private final Double _lon;

    public GeoCoordinates(Double lat, Double lon) throws Exception {
        if (lat < -90 || lat > 90) throw new Exception("invalid latitude");
        lon %= 360;
        if (lon > 180) lon -= 360;
        else if (lon < -180) lon += 360;
        this._lat = lat;
        this._lon = lon;
    }

    public Double getLat() {
        return this._lat;
    }

    public Double getLon() {
        return this._lon;
    }

    public Double distanceTo(GeoCoordinates other) {
        return WGS84.radiusAuthalic * 2 * Math.asin(Math.sqrt(Math.pow(Trigonometric.sin((this.getLat() - other.getLat()) / 2), 2) + Math.pow(Trigonometric.sin((this.getLon() - other.getLon()) / 2), 2) * Trigonometric.cos(this.getLat()) * Trigonometric.cos(other.getLat())));
    }

    @Override
    public String toString() {
        return String.format("lat %f lon %f", this._lat, this._lon);
    }

    @Override
    public int compareTo(GeoCoordinates o) {
        int d = Double.compare(this._lat, o._lat);
        if (d != 0) return d;
        return Double.compare(this._lon, o._lon);
    }
}
