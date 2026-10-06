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
 * Edge of a GeoPose graph, linking two frames of the frame list.
 *
 * <ul>
 *   <li>{@code link} holds indices into the {@link Graph} frame list.</li>
 *   <li>{@code link} is mandatory, but may be null in the JSON encoding.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition FrameTransformPair"
 */
@JsonPropertyOrder({
    FrameTransformPair.JSON_PROPERTY_LINK
})
public class FrameTransformPair extends DataTransferObject {

    public static final String JSON_PROPERTY_LINK = "link";
    private List<Integer> link = new ArrayList<>();

    public FrameTransformPair() {
    }

    public FrameTransformPair(final List<Integer> link) {
        this.link = link;
    }

    /**
     * Returns the indices of the two frames joined by this edge.
     *
     * @return indices into the graph frame list
     */
    @JsonProperty(JSON_PROPERTY_LINK)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<Integer> getLink() {
        return link;
    }

    @JsonProperty(JSON_PROPERTY_LINK)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setLink(final List<Integer> link) {
        this.link = link;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FrameTransformPair other = (FrameTransformPair) o;
        return Objects.equals(this.link, other.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }
}
