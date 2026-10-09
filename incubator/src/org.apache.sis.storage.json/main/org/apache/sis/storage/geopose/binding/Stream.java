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
 * Open-ended irregular GeoPose time series, suitable for streaming from a sensor or a service.
 *
 * <p>A stream differs from an irregular series in having no pose count and no trailer, since
 * its length is not known in advance.</p>
 *
 * <ul>
 *   <li>Both members are mandatory.</li>
 *   <li>{@code streamElements} holds at least one element.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Stream"
 */
@JsonPropertyOrder({
    Stream.JSON_PROPERTY_HEADER,
    Stream.JSON_PROPERTY_STREAM_ELEMENTS
})
public class Stream extends DataTransferObject {

    public static final String JSON_PROPERTY_HEADER = "header";
    private StreamHeader header;

    public static final String JSON_PROPERTY_STREAM_ELEMENTS = "streamElements";
    private List<StreamElement> streamElements = new ArrayList<>();

    public Stream() {
    }

    public Stream(final StreamHeader header, final List<StreamElement> streamElements) {
        this.header = header;
        this.streamElements = streamElements;
    }

    /**
     * Returns the opening block of the stream.
     *
     * @return the stream header
     */
    @JsonProperty(JSON_PROPERTY_HEADER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public StreamHeader getHeader() {
        return header;
    }

    @JsonProperty(JSON_PROPERTY_HEADER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setHeader(final StreamHeader header) {
        this.header = header;
    }

    /**
     * Returns the elements streamed after the header.
     *
     * @return at least one stream element
     */
    @JsonProperty(JSON_PROPERTY_STREAM_ELEMENTS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<StreamElement> getStreamElements() {
        return streamElements;
    }

    @JsonProperty(JSON_PROPERTY_STREAM_ELEMENTS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setStreamElements(final List<StreamElement> streamElements) {
        this.streamElements = streamElements;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Stream other = (Stream) o;
        return Objects.equals(this.header,         other.header)
            && Objects.equals(this.streamElements, other.streamElements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, streamElements);
    }
}
