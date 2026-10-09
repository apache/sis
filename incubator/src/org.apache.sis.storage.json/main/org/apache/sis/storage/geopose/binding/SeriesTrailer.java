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
 * Closing block of a GeoPose series, repeating the pose count for integrity checking.
 *
 * <ul>
 *   <li>{@code poseCount} is mandatory and should equal the value in the series header.</li>
 *   <li>{@code integrityCheck} is optional and may be null.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition SeriesTrailer"
 */
@JsonPropertyOrder({
    SeriesTrailer.JSON_PROPERTY_POSE_COUNT,
    SeriesTrailer.JSON_PROPERTY_INTEGRITY_CHECK
})
public class SeriesTrailer extends DataTransferObject {

    public static final String JSON_PROPERTY_POSE_COUNT = "poseCount";
    private int poseCount;

    public static final String JSON_PROPERTY_INTEGRITY_CHECK = "integrityCheck";
    private String integrityCheck;

    public SeriesTrailer() {
    }

    public SeriesTrailer(final int poseCount, final String integrityCheck) {
        this.poseCount = poseCount;
        this.integrityCheck = integrityCheck;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SeriesTrailer other = (SeriesTrailer) o;
        return this.poseCount == other.poseCount
            && Objects.equals(this.integrityCheck, other.integrityCheck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poseCount, integrityCheck);
    }
}
