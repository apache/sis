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
 * One inner frame of an irregular sequence, together with the time at which it is valid.
 *
 * <ul>
 *   <li>{@code frame} is mandatory.</li>
 *   <li>{@code validTime} is optional and expressed in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition FrameAndTime"
 */
@JsonPropertyOrder({
    FrameAndTime.JSON_PROPERTY_FRAME,
    FrameAndTime.JSON_PROPERTY_VALID_TIME
})
public class FrameAndTime extends DataTransferObject {

    public static final String JSON_PROPERTY_FRAME = "frame";
    private FrameSpecification frame;

    public static final String JSON_PROPERTY_VALID_TIME = "validTime";
    private Long validTime;

    public FrameAndTime() {
    }

    public FrameAndTime(final FrameSpecification frame, final Long validTime) {
        this.frame = frame;
        this.validTime = validTime;
    }

    /**
     * Returns the inner frame.
     *
     * @return the frame specification, never null in a valid document
     */
    @JsonProperty(JSON_PROPERTY_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public FrameSpecification getFrame() {
        return frame;
    }

    @JsonProperty(JSON_PROPERTY_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFrame(final FrameSpecification frame) {
        this.frame = frame;
    }

    /**
     * Returns the time at which the frame is valid.
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
        final FrameAndTime other = (FrameAndTime) o;
        return Objects.equals(this.frame,     other.frame)
            && Objects.equals(this.validTime, other.validTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frame, validTime);
    }
}
