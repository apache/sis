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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * MFMovingLineString, a Moving Features JSON (MF-JSON, OGC 16-140r1) temporal geometry: a
 * line string that moves and/or deforms over time. Each element of
 * {@link #getCoordinates()} is one list of positions (the line string's shape at that
 * instant), matching the corresponding entry of {@link #getDatetimes()}.
 */
@JsonPropertyOrder({
    MFMovingLineString.PROPERTY_TYPE,
    MFMovingLineString.PROPERTY_COORDINATES
})
public class MFMovingLineString extends MFTemporalPrimitiveGeometry {

    public static final String PROPERTY_COORDINATES = "coordinates";
    private List<List<List<Double>>> coordinates = new ArrayList<>();

    public MFMovingLineString() {
    }

    @Override
    public String getType() {
        return TYPE_MOVINGLINESTRING;
    }

    /**
     * Get coordinates
     *
     * @return coordinates
     */
    @JsonProperty(PROPERTY_COORDINATES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<List<List<Double>>> getCoordinates() {
        return coordinates;
    }

    @JsonProperty(PROPERTY_COORDINATES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setCoordinates(List<List<List<Double>>> coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Return true if this MFMovingLineString object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFMovingLineString other = (MFMovingLineString) o;
        return super.equals(o)
                && Objects.equals(this.coordinates, other.coordinates);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(coordinates);
    }

}
