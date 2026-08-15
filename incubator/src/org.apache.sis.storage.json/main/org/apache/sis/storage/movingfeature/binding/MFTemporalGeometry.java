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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import org.apache.sis.storage.json.DataTransferObject;
import static org.apache.sis.storage.movingfeature.binding.MFTemporalGeometry.PROPERTY_CRS;
import static org.apache.sis.storage.movingfeature.binding.MFTemporalGeometry.PROPERTY_TRS;

/**
 * Base of a Moving Features JSON (MF-JSON, OGC 16-140r1) 'temporalGeometry' member value.
 *
 * <p>Unlike {@link GeoJSONGeometry} (the base of plain GeoJSON/JSON-FG geometries), this
 * class is a sibling of {@link GeoJSONObject} rather than a subclass of it: temporal
 * geometries have no 'bbox' member in MF-JSON.</p>
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "type")
@JsonSubTypes({
  @Type(value = MFMovingPoint.class, name = "MovingPoint"),
  @Type(value = MFMovingLineString.class, name = "MovingLineString"),
  @Type(value = MFMovingPolygon.class, name = "MovingPolygon"),
  @Type(value = MFMovingPointCloud.class, name = "MovingPointCloud"),
  @Type(value = MFMovingGeometryCollection.class, name = "MovingGeometryCollection")
})
@JsonPropertyOrder({
    PROPERTY_CRS,
    PROPERTY_TRS
})
public abstract class MFTemporalGeometry extends DataTransferObject {

    public static final String TYPE_MOVINGPOINT = "MovingPoint";
    public static final String TYPE_MOVINGLINESTRING = "MovingLineString";
    public static final String TYPE_MOVINGPOLYGON = "MovingPolygon";
    public static final String TYPE_MOVINGPOINTCLOUD = "MovingPointCloud";
    public static final String TYPE_MOVINGGEOMETRYCOLLECTION = "MovingGeometryCollection";

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_CRS = "crs";
    public static final String PROPERTY_TRS = "trs";

    private MFCoordRefSys crs;

    private MFCoordRefSys trs;

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public abstract String getType();

    /**
     * Get crs
     *
     * @return crs
     */
    @JsonProperty(PROPERTY_CRS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public MFCoordRefSys getCrs() {
        return crs;
    }

    @JsonProperty(PROPERTY_CRS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setCrs(MFCoordRefSys crs) {
        this.crs = crs;
    }

    /**
     * Get trs
     *
     * @return trs
     */
    @JsonProperty(PROPERTY_TRS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public MFCoordRefSys getTrs() {
        return trs;
    }

    @JsonProperty(PROPERTY_TRS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTrs(MFCoordRefSys trs) {
        this.trs = trs;
    }

    /**
     * Return true if this MFTemporalGeometry object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFTemporalGeometry other = (MFTemporalGeometry) o;
        return Objects.equals(this.getType(), other.getType())
                && Objects.equals(this.crs, other.crs)
                && Objects.equals(this.trs, other.trs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), crs, trs);
    }

}
