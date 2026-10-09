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
 * One repeated element of a GeoPose stream.
 *
 * <p>This is the standardization target sent repeatedly after a {@link StreamHeader},
 * which is why the inner frame and time pair is wrapped in its own object.</p>
 *
 * <ul>
 *   <li>{@code streamElement} is mandatory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Stream Element"
 */
@JsonPropertyOrder({
    StreamElement.JSON_PROPERTY_STREAM_ELEMENT
})
public class StreamElement extends DataTransferObject {

    public static final String JSON_PROPERTY_STREAM_ELEMENT = "streamElement";
    private FrameAndTime streamElement;

    public StreamElement() {
    }

    public StreamElement(final FrameAndTime streamElement) {
        this.streamElement = streamElement;
    }

    /**
     * Returns the inner frame and its validity time.
     *
     * @return the frame and time pair
     */
    @JsonProperty(JSON_PROPERTY_STREAM_ELEMENT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public FrameAndTime getStreamElement() {
        return streamElement;
    }

    @JsonProperty(JSON_PROPERTY_STREAM_ELEMENT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setStreamElement(final FrameAndTime streamElement) {
        this.streamElement = streamElement;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StreamElement other = (StreamElement) o;
        return Objects.equals(this.streamElement, other.streamElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamElement);
    }
}
