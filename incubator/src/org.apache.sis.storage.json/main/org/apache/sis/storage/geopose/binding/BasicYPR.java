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
 * Basic GeoPose using yaw, pitch and roll angles to specify orientation.
 *
 * <p>The outer frame is the WGS 84 geographic 3D reference frame (EPSG::4979) and the inner
 * frame is the local tangent plane east-north-up frame at the given position.</p>
 *
 * <ul>
 *   <li>Both members are mandatory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Basic-YPR"
 */
@JsonPropertyOrder({
    BasicYPR.JSON_PROPERTY_POSITION,
    BasicYPR.JSON_PROPERTY_ANGLES
})
public class BasicYPR extends DataTransferObject {

    public static final String JSON_PROPERTY_POSITION = "position";
    private Position position;

    public static final String JSON_PROPERTY_ANGLES = "angles";
    private YawPitchRoll angles;

    public BasicYPR() {
    }

    public BasicYPR(final Position position, final YawPitchRoll angles) {
        this.position = position;
        this.angles = angles;
    }

    /**
     * Returns the origin of the inner frame.
     *
     * @return the geodetic position
     */
    @JsonProperty(JSON_PROPERTY_POSITION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public Position getPosition() {
        return position;
    }

    @JsonProperty(JSON_PROPERTY_POSITION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setPosition(final Position position) {
        this.position = position;
    }

    /**
     * Returns the orientation of the inner frame.
     *
     * @return the yaw, pitch and roll angles
     */
    @JsonProperty(JSON_PROPERTY_ANGLES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public YawPitchRoll getAngles() {
        return angles;
    }

    @JsonProperty(JSON_PROPERTY_ANGLES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setAngles(final YawPitchRoll angles) {
        this.angles = angles;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BasicYPR other = (BasicYPR) o;
        return Objects.equals(this.position, other.position)
            && Objects.equals(this.angles,   other.angles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, angles);
    }
}
