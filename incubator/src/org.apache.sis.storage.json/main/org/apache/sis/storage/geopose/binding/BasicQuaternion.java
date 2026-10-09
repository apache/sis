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
 * Basic GeoPose using a unit quaternion to specify orientation.
 *
 * <p>The outer frame is the WGS 84 geographic 3D reference frame (EPSG::4979) and the inner
 * frame is the local tangent plane east-north-up frame at the given position.</p>
 *
 * <ul>
 *   <li>Both members are mandatory.</li>
 *   <li>Unknown members are preserved.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Basic-Quaternion"
 */
@JsonPropertyOrder({
    BasicQuaternion.JSON_PROPERTY_POSITION,
    BasicQuaternion.JSON_PROPERTY_QUATERNION
})
public class BasicQuaternion extends DataTransferObject {

    public static final String JSON_PROPERTY_POSITION = "position";
    private Position position;

    public static final String JSON_PROPERTY_QUATERNION = "quaternion";
    private Quaternion quaternion;

    public BasicQuaternion() {
    }

    public BasicQuaternion(final Position position, final Quaternion quaternion) {
        this.position = position;
        this.quaternion = quaternion;
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
     * @return the unit quaternion
     */
    @JsonProperty(JSON_PROPERTY_QUATERNION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public Quaternion getQuaternion() {
        return quaternion;
    }

    @JsonProperty(JSON_PROPERTY_QUATERNION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setQuaternion(final Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BasicQuaternion other = (BasicQuaternion) o;
        return Objects.equals(this.position,   other.position)
            && Objects.equals(this.quaternion, other.quaternion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, quaternion);
    }
}
