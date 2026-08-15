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
import static org.apache.sis.storage.movingfeature.binding.MFTemporalGeometry.PROPERTY_CRS;
import static org.apache.sis.storage.movingfeature.binding.MFTemporalGeometry.PROPERTY_TRS;


/**
 * MFMovingGeometryCollection, a Moving Features JSON (MF-JSON, OGC 16-140r1) temporal
 * geometry aggregating several primitive temporal geometries (the specification's
 * "temporalComplexGeometry"). Unlike its primitive siblings, it has no {@code datetimes}/
 * {@code interpolation}/{@code base}/{@code orientations} of its own — each element of
 * {@link #getPrisms()} carries those individually — so this class extends
 * {@link MFTemporalGeometry} directly rather than {@link MFTemporalPrimitiveGeometry}.
 */
@JsonPropertyOrder({
    MFMovingGeometryCollection.PROPERTY_TYPE,
    PROPERTY_CRS,
    PROPERTY_TRS,
    MFMovingGeometryCollection.PROPERTY_PRISMS
})
public class MFMovingGeometryCollection extends MFTemporalGeometry {

    public static final String PROPERTY_PRISMS = "prisms";
    private List<MFTemporalPrimitiveGeometry> prisms = new ArrayList<>();

    public MFMovingGeometryCollection() {
    }

    @Override
    public String getType() {
        return TYPE_MOVINGGEOMETRYCOLLECTION;
    }

    /**
     * Get prisms
     *
     * @return prisms
     */
    @JsonProperty(PROPERTY_PRISMS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<MFTemporalPrimitiveGeometry> getPrisms() {
        return prisms;
    }

    @JsonProperty(PROPERTY_PRISMS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setPrisms(List<MFTemporalPrimitiveGeometry> prisms) {
        this.prisms = prisms;
    }

    /**
     * Return true if this MFMovingGeometryCollection object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFMovingGeometryCollection other = (MFMovingGeometryCollection) o;
        return super.equals(o)
                && Objects.equals(this.prisms, other.prisms);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(prisms);
    }

}
