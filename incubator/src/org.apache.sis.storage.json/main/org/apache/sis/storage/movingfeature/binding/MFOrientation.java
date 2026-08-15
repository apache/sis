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
package org.apache.sis.storage.movingfeature.binding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.sis.storage.json.DataTransferObject;

/**
 * One entry of a Moving Features JSON (MF-JSON, OGC 16-140r1) 'orientations' array:
 * the pose (scale and rotation angles, on the 3 axes) of a {@link MFTemporalPrimitiveGeometry}'s
 * {@link MFMovingGeometryBase} asset at the corresponding datetime.
 */
@JsonPropertyOrder({
    MFOrientation.PROPERTY_SCALES,
    MFOrientation.PROPERTY_ANGLES
})
public class MFOrientation extends DataTransferObject {

    public static final String PROPERTY_SCALES = "scales";
    public static final String PROPERTY_ANGLES = "angles";

    private List<Double> scales = new ArrayList<>();

    private List<Double> angles = new ArrayList<>();

    public MFOrientation() {
    }

    /**
     * Get scales, 3 values for the X, Y and Z axes.
     *
     * @return scales
     */
    @JsonProperty(PROPERTY_SCALES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<Double> getScales() {
        return scales;
    }

    @JsonProperty(PROPERTY_SCALES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setScales(List<Double> scales) {
        this.scales = scales;
    }

    /**
     * Get angles, 3 values for the X, Y and Z axes.
     *
     * @return angles
     */
    @JsonProperty(PROPERTY_ANGLES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<Double> getAngles() {
        return angles;
    }

    @JsonProperty(PROPERTY_ANGLES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setAngles(List<Double> angles) {
        this.angles = angles;
    }

    /**
     * Return true if this MFOrientation object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFOrientation other = (MFOrientation) o;
        return Objects.equals(this.scales, other.scales)
                && Objects.equals(this.angles, other.angles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scales, angles);
    }

}
