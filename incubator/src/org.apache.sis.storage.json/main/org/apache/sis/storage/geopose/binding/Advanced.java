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
 * Advanced GeoPose allowing a configurable outer frame and a validity time.
 *
 * <p>Unlike the Basic targets, which fix the outer frame to EPSG::4979, this target names its
 * outer frame explicitly through a {@link FrameSpecification}.</p>
 *
 * <ul>
 *   <li>{@code frameSpecification} and {@code quaternion} are mandatory.</li>
 *   <li>{@code validTime} is optional and expressed in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Advanced"
 */
@JsonPropertyOrder({
    Advanced.JSON_PROPERTY_FRAME_SPECIFICATION,
    Advanced.JSON_PROPERTY_QUATERNION,
    Advanced.JSON_PROPERTY_VALID_TIME
})
public class Advanced extends DataTransferObject {

    public static final String JSON_PROPERTY_FRAME_SPECIFICATION = "frameSpecification";
    private FrameSpecification frameSpecification;

    public static final String JSON_PROPERTY_QUATERNION = "quaternion";
    private Quaternion quaternion;

    public static final String JSON_PROPERTY_VALID_TIME = "validTime";
    private Long validTime;

    public Advanced() {
    }

    public Advanced(final FrameSpecification frameSpecification, final Quaternion quaternion,
            final Long validTime)
    {
        this.frameSpecification = frameSpecification;
        this.quaternion = quaternion;
        this.validTime = validTime;
    }

    /**
     * Returns the frame in which the orientation is expressed.
     *
     * @return the frame specification
     */
    @JsonProperty(JSON_PROPERTY_FRAME_SPECIFICATION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public FrameSpecification getFrameSpecification() {
        return frameSpecification;
    }

    @JsonProperty(JSON_PROPERTY_FRAME_SPECIFICATION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFrameSpecification(final FrameSpecification frameSpecification) {
        this.frameSpecification = frameSpecification;
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

    /**
     * Returns the time at which this pose is valid.
     *
     * @return milliseconds since the Unix epoch, or null if unspecified
     */
    @JsonProperty(JSON_PROPERTY_VALID_TIME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Long getValidTime() {
        return validTime;
    }

    @JsonProperty(JSON_PROPERTY_VALID_TIME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setValidTime(final Long validTime) {
        this.validTime = validTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Advanced other = (Advanced) o;
        return Objects.equals(this.frameSpecification, other.frameSpecification)
            && Objects.equals(this.quaternion,         other.quaternion)
            && Objects.equals(this.validTime,          other.validTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frameSpecification, quaternion, validTime);
    }
}
