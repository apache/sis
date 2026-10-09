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
 * Opening block of a GeoPose series, describing its extent and transition model.
 *
 * <ul>
 *   <li>{@code poseCount}, {@code startInstant}, {@code stopInstant} and {@code transitionModel}
 *       are mandatory.</li>
 *   <li>{@code integrityCheck} is optional and may be null.</li>
 *   <li>Instants are expressed in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition SeriesHeader"
 */
@JsonPropertyOrder({
    SeriesHeader.JSON_PROPERTY_POSE_COUNT,
    SeriesHeader.JSON_PROPERTY_INTEGRITY_CHECK,
    SeriesHeader.JSON_PROPERTY_START_INSTANT,
    SeriesHeader.JSON_PROPERTY_STOP_INSTANT,
    SeriesHeader.JSON_PROPERTY_TRANSITION_MODEL
})
public class SeriesHeader extends DataTransferObject {

    public static final String JSON_PROPERTY_POSE_COUNT = "poseCount";
    private int poseCount;

    public static final String JSON_PROPERTY_INTEGRITY_CHECK = "integrityCheck";
    private String integrityCheck;

    public static final String JSON_PROPERTY_START_INSTANT = "startInstant";
    private long startInstant;

    public static final String JSON_PROPERTY_STOP_INSTANT = "stopInstant";
    private long stopInstant;

    public static final String JSON_PROPERTY_TRANSITION_MODEL = "transitionModel";
    private TransitionModel transitionModel;

    public SeriesHeader() {
    }

    public SeriesHeader(final int poseCount, final String integrityCheck, final long startInstant,
            final long stopInstant, final TransitionModel transitionModel)
    {
        this.poseCount = poseCount;
        this.integrityCheck = integrityCheck;
        this.startInstant = startInstant;
        this.stopInstant = stopInstant;
        this.transitionModel = transitionModel;
    }

    /**
     * Returns the number of poses in the series.
     *
     * @return the pose count
     */
    @JsonProperty(JSON_PROPERTY_POSE_COUNT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public int getPoseCount() {
        return poseCount;
    }

    @JsonProperty(JSON_PROPERTY_POSE_COUNT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setPoseCount(final int poseCount) {
        this.poseCount = poseCount;
    }

    /**
     * Returns the integrity check value of the series.
     *
     * @return the integrity check, or null if none
     */
    @JsonProperty(JSON_PROPERTY_INTEGRITY_CHECK)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getIntegrityCheck() {
        return integrityCheck;
    }

    @JsonProperty(JSON_PROPERTY_INTEGRITY_CHECK)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setIntegrityCheck(final String integrityCheck) {
        this.integrityCheck = integrityCheck;
    }

    /**
     * Returns the time of the first pose of the series.
     *
     * @return milliseconds since the Unix epoch
     */
    @JsonProperty(JSON_PROPERTY_START_INSTANT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public long getStartInstant() {
        return startInstant;
    }

    @JsonProperty(JSON_PROPERTY_START_INSTANT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setStartInstant(final long startInstant) {
        this.startInstant = startInstant;
    }

    /**
     * Returns the time of the last pose of the series.
     *
     * @return milliseconds since the Unix epoch
     */
    @JsonProperty(JSON_PROPERTY_STOP_INSTANT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public long getStopInstant() {
        return stopInstant;
    }

    @JsonProperty(JSON_PROPERTY_STOP_INSTANT)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setStopInstant(final long stopInstant) {
        this.stopInstant = stopInstant;
    }

    /**
     * Returns the model describing how the pose evolves between two consecutive poses.
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SeriesHeader other = (SeriesHeader) o;
        return this.poseCount == other.poseCount
            && this.startInstant == other.startInstant
            && this.stopInstant == other.stopInstant
            && Objects.equals(this.integrityCheck,  other.integrityCheck)
            && Objects.equals(this.transitionModel, other.transitionModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poseCount, integrityCheck, startInstant, stopInstant, transitionModel);
    }
}
