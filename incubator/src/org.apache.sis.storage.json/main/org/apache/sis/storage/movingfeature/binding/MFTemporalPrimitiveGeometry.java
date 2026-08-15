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
 * Base of the 4 Moving Features JSON (MF-JSON, OGC 16-140r1) "primitive" temporal
 * geometry types: {@link MFMovingPoint}, {@link MFMovingLineString}, {@link MFMovingPolygon}
 * and {@link MFMovingPointCloud}. Adds the {@code datetimes} array shared by all of them
 * (one entry per position/position-set in the subtype's {@code coordinates}), and the
 * optional {@code interpolation}, {@code base} and {@code orientations} members.
 */
@JsonPropertyOrder({
    MFTemporalPrimitiveGeometry.PROPERTY_TYPE,
    PROPERTY_CRS,
    PROPERTY_TRS,
    MFTemporalPrimitiveGeometry.PROPERTY_DATETIMES,
    MFTemporalPrimitiveGeometry.PROPERTY_INTERPOLATION,
    MFTemporalPrimitiveGeometry.PROPERTY_BASE,
    MFTemporalPrimitiveGeometry.PROPERTY_ORIENTATIONS
})
public abstract class MFTemporalPrimitiveGeometry extends MFTemporalGeometry {

    public static final String PROPERTY_DATETIMES = "datetimes";
    public static final String PROPERTY_INTERPOLATION = "interpolation";
    public static final String PROPERTY_BASE = "base";
    public static final String PROPERTY_ORIENTATIONS = "orientations";

    private List<String> datetimes = new ArrayList<>();

    private String interpolation;

    private MFMovingGeometryBase base;

    private List<MFOrientation> orientations = new ArrayList<>();

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
     * Get interpolation, one of {@link MFTemporalPropertyValue#INTERPOLATION_DISCRETE},
     * "Step", "Linear", "Quadratic", "Cube", or an arbitrary URI.
     *
     * @return interpolation
     */
    @JsonProperty(PROPERTY_INTERPOLATION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getInterpolation() {
        return interpolation;
    }

    @JsonProperty(PROPERTY_INTERPOLATION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Get base
     *
     * @return base
     */
    @JsonProperty(PROPERTY_BASE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public MFMovingGeometryBase getBase() {
        return base;
    }

    @JsonProperty(PROPERTY_BASE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setBase(MFMovingGeometryBase base) {
        this.base = base;
    }

    /**
     * Get orientations
     *
     * @return orientations
     */
    @JsonProperty(PROPERTY_ORIENTATIONS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<MFOrientation> getOrientations() {
        return orientations;
    }

    @JsonProperty(PROPERTY_ORIENTATIONS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setOrientations(List<MFOrientation> orientations) {
        this.orientations = orientations;
    }

    /**
     * Return true if this MFTemporalPrimitiveGeometry object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFTemporalPrimitiveGeometry other = (MFTemporalPrimitiveGeometry) o;
        return super.equals(o)
                && Objects.equals(this.datetimes, other.datetimes)
                && Objects.equals(this.interpolation, other.interpolation)
                && Objects.equals(this.base, other.base)
                && Objects.equals(this.orientations, other.orientations);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(datetimes, interpolation, base, orientations);
    }

}
