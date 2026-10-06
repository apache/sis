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
package org.apache.sis.storage.geopose.binding;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Geodetic position of a GeoPose outer frame origin.
 *
 * <p>The position is expressed in the WGS 84 geographic 3D reference frame (EPSG::4979),
 * which is the outer frame of every Basic standardization target.</p>
 *
 * <ul>
 *   <li>{@code lat} is a latitude in decimal degrees, in the [-90 … +90] range.</li>
 *   <li>{@code lon} is a longitude in decimal degrees, in the [-180 … +180] range.</li>
 *   <li>{@code h} is an ellipsoidal height in metres.</li>
 *   <li>All three members are mandatory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition Position"
 */
@JsonPropertyOrder({
    Position.JSON_PROPERTY_LAT,
    Position.JSON_PROPERTY_LON,
    Position.JSON_PROPERTY_H
})
public class Position extends DataTransferObject {

    public static final String JSON_PROPERTY_LAT = "lat";
    private double lat;

    public static final String JSON_PROPERTY_LON = "lon";
    private double lon;

    public static final String JSON_PROPERTY_H = "h";
    private double h;

    public Position() {
    }

    public Position(final double lat, final double lon, final double h) {
        this.lat = lat;
        this.lon = lon;
        this.h = h;
    }

    /**
     * Returns the latitude in decimal degrees.
     *
     * @return latitude in the [-90 … +90] range
     */
    @JsonProperty(JSON_PROPERTY_LAT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getLat() {
        return lat;
    }

    @JsonProperty(JSON_PROPERTY_LAT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLat(final double lat) {
        this.lat = lat;
    }

    /**
     * Returns the longitude in decimal degrees.
     *
     * @return longitude in the [-180 … +180] range
     */
    @JsonProperty(JSON_PROPERTY_LON)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getLon() {
        return lon;
    }

    @JsonProperty(JSON_PROPERTY_LON)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLon(final double lon) {
        this.lon = lon;
    }

    /**
     * Returns the ellipsoidal height in metres.
     *
     * @return height above the WGS 84 ellipsoid
     */
    @JsonProperty(JSON_PROPERTY_H)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getH() {
        return h;
    }

    @JsonProperty(JSON_PROPERTY_H)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setH(final double h) {
        this.h = h;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Position other = (Position) o;
        return Double.compare(this.lat, other.lat) == 0
            && Double.compare(this.lon, other.lon) == 0
            && Double.compare(this.h,   other.h)   == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, h);
    }
}
