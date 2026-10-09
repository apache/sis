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
 * Pose relationships modelled as a graph of frames joined by transforms.
 *
 * <p>Frames are the nodes and transforms are the edges; each edge of {@code transformList}
 * holds indices into {@code frameList}.</p>
 *
 * <ul>
 *   <li>All three members are mandatory.</li>
 *   <li>{@code frameList} holds at least two frames and {@code transformList} at least one edge.</li>
 *   <li>{@code validTime} is expressed in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Graph"
 */
@JsonPropertyOrder({
    Graph.JSON_PROPERTY_VALID_TIME,
    Graph.JSON_PROPERTY_FRAME_LIST,
    Graph.JSON_PROPERTY_TRANSFORM_LIST
})
public class Graph extends DataTransferObject {

    public static final String JSON_PROPERTY_VALID_TIME = "validTime";
    private long validTime;

    public static final String JSON_PROPERTY_FRAME_LIST = "frameList";
    private List<FrameSpecification> frameList = new ArrayList<>();

    public static final String JSON_PROPERTY_TRANSFORM_LIST = "transformList";
    private List<FrameTransformPair> transformList = new ArrayList<>();

    public Graph() {
    }

    public Graph(final long validTime, final List<FrameSpecification> frameList,
            final List<FrameTransformPair> transformList)
    {
        this.validTime = validTime;
        this.frameList = frameList;
        this.transformList = transformList;
    }

    /**
     * Returns the time at which this graph is valid.
     *
     * @return milliseconds since the Unix epoch
     */
    @JsonProperty(JSON_PROPERTY_VALID_TIME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public long getValidTime() {
        return validTime;
    }

    @JsonProperty(JSON_PROPERTY_VALID_TIME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setValidTime(final long validTime) {
        this.validTime = validTime;
    }

    /**
     * Returns the nodes of the graph.
     *
     * @return at least two frames
     */
    @JsonProperty(JSON_PROPERTY_FRAME_LIST)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<FrameSpecification> getFrameList() {
        return frameList;
    }

    @JsonProperty(JSON_PROPERTY_FRAME_LIST)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFrameList(final List<FrameSpecification> frameList) {
        this.frameList = frameList;
    }

    /**
     * Returns the edges of the graph.
     *
     * @return at least one frame pair
     */
    @JsonProperty(JSON_PROPERTY_TRANSFORM_LIST)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<FrameTransformPair> getTransformList() {
        return transformList;
    }

    @JsonProperty(JSON_PROPERTY_TRANSFORM_LIST)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setTransformList(final List<FrameTransformPair> transformList) {
        this.transformList = transformList;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Graph other = (Graph) o;
        return this.validTime == other.validTime
            && Objects.equals(this.frameList,     other.frameList)
            && Objects.equals(this.transformList, other.transformList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validTime, frameList, transformList);
    }
}
