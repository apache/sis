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
 * One entry of a Moving Features JSON (MF-JSON, OGC 16-140r1) 'temporalProperties' array
 * (the specification's "parametricValues" object): a shared {@code datetimes} array plus
 * an arbitrary number of arbitrarily-named time-varying property entries, e.g.:
 * <pre>{@code
 * {
 *   "datetimes": ["2011-07-14T22:01:01.450Z", "2011-07-14T23:01:01.450Z"],
 *   "length": {"type": "Measure", "values": [1.0, 2.4], "interpolation": "Linear"},
 *   "labels": {"type": "Text", "values": ["car", "human"], "interpolation": "Discrete"}
 * }
 * }</pre>
 *
 * <p>Since property names ({@code "length"}, {@code "labels"} above) are chosen freely by
 * the data producer, they are not declared as individual Java fields. They are instead
 * captured by the {@link DataTransferObject#otherFields()} catch-all this class inherits,
 * each value being an {@link MFTemporalPropertyValue}-shaped object.</p>
 */
@JsonPropertyOrder({
    MFTemporalPropertyGroup.PROPERTY_DATETIMES
})
public class MFTemporalPropertyGroup extends DataTransferObject {

    public static final String PROPERTY_DATETIMES = "datetimes";

    private List<String> datetimes = new ArrayList<>();

    public MFTemporalPropertyGroup() {
    }

    /**
     * Get datetimes
     *
     * @return datetimes
     */
    @JsonProperty(PROPERTY_DATETIMES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<String> getDatetimes() {
        return datetimes;
    }

    @JsonProperty(PROPERTY_DATETIMES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setDatetimes(List<String> datetimes) {
        this.datetimes = datetimes;
    }

    /**
     * Return true if this MFTemporalPropertyGroup object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFTemporalPropertyGroup other = (MFTemporalPropertyGroup) o;
        return Objects.equals(this.datetimes, other.datetimes)
                && Objects.equals(this.otherFields(), other.otherFields());
    }

    @Override
    public int hashCode() {
        return Objects.hash(datetimes, otherFields());
    }

}
