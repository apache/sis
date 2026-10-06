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
 * An outer frame and a linear sequence of transformations down to an innermost frame.
 *
 * <ul>
 *   <li>All three members are mandatory.</li>
 *   <li>{@code frameChain} holds at least two frames, ordered from the outermost to the
 *       innermost.</li>
 *   <li>{@code validTime} is expressed in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, standardization target Chain"
 */
@JsonPropertyOrder({
    Chain.JSON_PROPERTY_VALID_TIME,
    Chain.JSON_PROPERTY_OUTER_FRAME,
    Chain.JSON_PROPERTY_FRAME_CHAIN
})
public class Chain extends DataTransferObject {

    public static final String JSON_PROPERTY_VALID_TIME = "validTime";
    private long validTime;

    public static final String JSON_PROPERTY_OUTER_FRAME = "outerFrame";
    private FrameSpecification outerFrame;

    public static final String JSON_PROPERTY_FRAME_CHAIN = "frameChain";
    private List<FrameSpecification> frameChain = new ArrayList<>();

    public Chain() {
    }

    public Chain(final long validTime, final FrameSpecification outerFrame,
            final List<FrameSpecification> frameChain)
    {
        this.validTime = validTime;
        this.outerFrame = outerFrame;
        this.frameChain = frameChain;
    }

    /**
     * Returns the time at which this chain is valid.
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
     * Returns the frame in which the chain is anchored.
     *
     * @return the outer frame
     */
    @JsonProperty(JSON_PROPERTY_OUTER_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public FrameSpecification getOuterFrame() {
        return outerFrame;
    }

    @JsonProperty(JSON_PROPERTY_OUTER_FRAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setOuterFrame(final FrameSpecification outerFrame) {
        this.outerFrame = outerFrame;
    }

    /**
     * Returns the successive frames of the chain.
     *
     * @return at least two frames, from the outermost to the innermost
     */
    @JsonProperty(JSON_PROPERTY_FRAME_CHAIN)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<FrameSpecification> getFrameChain() {
        return frameChain;
    }

    @JsonProperty(JSON_PROPERTY_FRAME_CHAIN)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFrameChain(final List<FrameSpecification> frameChain) {
        this.frameChain = frameChain;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Chain other = (Chain) o;
        return this.validTime == other.validTime
            && Objects.equals(this.outerFrame, other.outerFrame)
            && Objects.equals(this.frameChain, other.frameChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validTime, outerFrame, frameChain);
    }
}
