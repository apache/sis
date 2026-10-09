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
 * Orientation of a GeoPose inner frame expressed as yaw, pitch and roll angles.
 *
 * <p>The three angles are successive rotations about the z, y and x axes of the local
 * tangent plane east-north-up frame, in that order (the Z-Y'-X" convention).</p>
 *
 * <ul>
 *   <li>Angles are expressed in decimal degrees.</li>
 *   <li>{@code yaw} is in the [-180 … +180] range.</li>
 *   <li>{@code pitch} is in the [-90 … +90] range.</li>
 *   <li>{@code roll} is in the [-180 … +180] range.</li>
 *   <li>All three members are mandatory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, clause 2.3 and definition angles"
 */
@JsonPropertyOrder({
    YawPitchRoll.JSON_PROPERTY_YAW,
    YawPitchRoll.JSON_PROPERTY_PITCH,
    YawPitchRoll.JSON_PROPERTY_ROLL
})
public class YawPitchRoll extends DataTransferObject {

    public static final String JSON_PROPERTY_YAW = "yaw";
    private double yaw;

    public static final String JSON_PROPERTY_PITCH = "pitch";
    private double pitch;

    public static final String JSON_PROPERTY_ROLL = "roll";
    private double roll;

    public YawPitchRoll() {
    }

    public YawPitchRoll(final double yaw, final double pitch, final double roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    /**
     * Returns the rotation about the up axis, in decimal degrees.
     *
     * @return yaw in the [-180 … +180] range
     */
    @JsonProperty(JSON_PROPERTY_YAW)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getYaw() {
        return yaw;
    }

    @JsonProperty(JSON_PROPERTY_YAW)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setYaw(final double yaw) {
        this.yaw = yaw;
    }

    /**
     * Returns the rotation about the north axis, in decimal degrees.
     *
     * @return pitch in the [-90 … +90] range
     */
    @JsonProperty(JSON_PROPERTY_PITCH)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getPitch() {
        return pitch;
    }

    @JsonProperty(JSON_PROPERTY_PITCH)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setPitch(final double pitch) {
        this.pitch = pitch;
    }

    /**
     * Returns the rotation about the east axis, in decimal degrees.
     *
     * @return roll in the [-180 … +180] range
     */
    @JsonProperty(JSON_PROPERTY_ROLL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public double getRoll() {
        return roll;
    }

    @JsonProperty(JSON_PROPERTY_ROLL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setRoll(final double roll) {
        this.roll = roll;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final YawPitchRoll other = (YawPitchRoll) o;
        return Double.compare(this.yaw,   other.yaw)   == 0
            && Double.compare(this.pitch, other.pitch) == 0
            && Double.compare(this.roll,  other.roll)  == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch, roll);
    }
}
