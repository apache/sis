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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Basic GeoPose using Euler angle rotations to specify orientation.
 *
 * <p>This target flattens the position into three top level members instead of nesting it in a
 * {@link Position} object, and carries the orientation as an array rather than as named angles.</p>
 *
 * <ul>
 *   <li>All four members are mandatory.</li>
 *   <li>{@code longitude} is in the [-180 … +180] range, {@code latitude} in [-90 … +90],
 *       both in decimal degrees; {@code height} is in metres.</li>
 *   <li>{@code rotations} holds exactly three angles, in the same yaw, pitch, roll order as
 *       {@link YawPitchRoll}.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Basic-Euler"
 */
@JsonPropertyOrder({
    BasicEuler.JSON_PROPERTY_LONGITUDE,
    BasicEuler.JSON_PROPERTY_LATITUDE,
    BasicEuler.JSON_PROPERTY_HEIGHT,
    BasicEuler.JSON_PROPERTY_ROTATIONS
})
public class BasicEuler extends DataTransferObject {

    public static final String JSON_PROPERTY_LONGITUDE = "longitude";
    private double longitude;

    public static final String JSON_PROPERTY_LATITUDE = "latitude";
    private double latitude;

    public static final String JSON_PROPERTY_HEIGHT = "height";
    private double height;

    public static final String JSON_PROPERTY_ROTATIONS = "rotations";
    private List<Double> rotations = new ArrayList<>();

    public BasicEuler() {
    }

    public BasicEuler(final double longitude, final double latitude, final double height,
            final List<Double> rotations)
    {
        this.longitude = longitude;
        this.latitude = latitude;
        this.height = height;
        this.rotations = rotations;
    }

    /**
     * Returns the longitude in decimal degrees.
     *
     * @return longitude in the [-180 … +180] range
     */
    @JsonProperty(JSON_PROPERTY_LONGITUDE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getLongitude() {
        return longitude;
    }

    @JsonProperty(JSON_PROPERTY_LONGITUDE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLongitude(final double longitude) {
        this.longitude = longitude;
    }

    /**
     * Returns the latitude in decimal degrees.
     *
     * @return latitude in the [-90 … +90] range
     */
    @JsonProperty(JSON_PROPERTY_LATITUDE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getLatitude() {
        return latitude;
    }

    @JsonProperty(JSON_PROPERTY_LATITUDE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLatitude(final double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the ellipsoidal height in metres.
     *
     * @return height above the WGS 84 ellipsoid
     */
    @JsonProperty(JSON_PROPERTY_HEIGHT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getHeight() {
        return height;
    }

    @JsonProperty(JSON_PROPERTY_HEIGHT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setHeight(final double height) {
        this.height = height;
    }

    /**
     * Returns the three Euler angles, in decimal degrees.
     *
     * @return the rotations, in yaw, pitch, roll order
     */
    @JsonProperty(JSON_PROPERTY_ROTATIONS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<Double> getRotations() {
        return rotations;
    }

    @JsonProperty(JSON_PROPERTY_ROTATIONS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setRotations(final List<Double> rotations) {
        this.rotations = rotations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BasicEuler other = (BasicEuler) o;
        return Double.compare(this.longitude, other.longitude) == 0
            && Double.compare(this.latitude,  other.latitude)  == 0
            && Double.compare(this.height,    other.height)    == 0
            && Objects.equals(this.rotations, other.rotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longitude, latitude, height, rotations);
    }
}
