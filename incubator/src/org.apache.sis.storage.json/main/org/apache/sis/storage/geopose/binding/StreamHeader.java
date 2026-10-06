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
 * Opening block of a GeoPose stream, sent once before the stream elements.
 *
 * <p>Unlike a series header, a stream header carries no pose count and no time extent,
 * because a stream is open-ended.</p>
 *
 * <ul>
 *   <li>Both members are mandatory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition StreamHeader"
 */
@JsonPropertyOrder({
    StreamHeader.JSON_PROPERTY_TRANSITION_MODEL,
    StreamHeader.JSON_PROPERTY_OUTER_FRAME
})
public class StreamHeader extends DataTransferObject {

    public static final String JSON_PROPERTY_TRANSITION_MODEL = "transitionModel";
    private TransitionModel transitionModel;

    public static final String JSON_PROPERTY_OUTER_FRAME = "outerFrame";
    private FrameSpecification outerFrame;

    public StreamHeader() {
    }

    public StreamHeader(final TransitionModel transitionModel, final FrameSpecification outerFrame) {
        this.transitionModel = transitionModel;
        this.outerFrame = outerFrame;
    }

    /**
     * Returns the model describing how the pose evolves between two consecutive elements.
     *
     * @return the transition model
     */
    @JsonProperty(JSON_PROPERTY_TRANSITION_MODEL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public TransitionModel getTransitionModel() {
        return transitionModel;
    }

    @JsonProperty(JSON_PROPERTY_TRANSITION_MODEL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setTransitionModel(final TransitionModel transitionModel) {
        this.transitionModel = transitionModel;
    }

    /**
     * Returns the frame in which every element of the stream is expressed.
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StreamHeader other = (StreamHeader) o;
        return Objects.equals(this.transitionModel, other.transitionModel)
            && Objects.equals(this.outerFrame,      other.outerFrame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transitionModel, outerFrame);
    }
}
