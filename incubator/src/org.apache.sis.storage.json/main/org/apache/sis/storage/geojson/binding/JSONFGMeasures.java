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
package org.apache.sis.storage.geojson.binding;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Value of a JSON-FG 'measures' member, part of JSON-FG version 1.0.0.
 * States whether the 4th element of positions (when present) shall be
 * interpreted as a measure value, and gives optional metadata about it.
 */
@JsonPropertyOrder({
    JSONFGMeasures.PROPERTY_ENABLED,
    JSONFGMeasures.PROPERTY_UNIT,
    JSONFGMeasures.PROPERTY_DESCRIPTION
})
public class JSONFGMeasures extends DataTransferObject {

    public static final String PROPERTY_ENABLED = "enabled";
    public static final String PROPERTY_UNIT = "unit";
    public static final String PROPERTY_DESCRIPTION = "description";

    private Boolean enabled;

    private String unit;

    private String description;

    public JSONFGMeasures() {
    }

    /**
     * Get enabled
     *
     * @return enabled
     */
    @JsonProperty(PROPERTY_ENABLED)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public Boolean getEnabled() {
        return enabled;
    }

    @JsonProperty(PROPERTY_ENABLED)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get unit
     *
     * @return unit
     */
    @JsonProperty(PROPERTY_UNIT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getUnit() {
        return unit;
    }

    @JsonProperty(PROPERTY_UNIT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Get description
     *
     * @return description
     */
    @JsonProperty(PROPERTY_DESCRIPTION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getDescription() {
        return description;
    }

    @JsonProperty(PROPERTY_DESCRIPTION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return true if this JSONFGMeasures object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGMeasures other = (JSONFGMeasures) o;
        return Objects.equals(this.enabled, other.enabled)
                && Objects.equals(this.unit, other.unit)
                && Objects.equals(this.description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, unit, description);
    }

}
