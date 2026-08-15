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
 * CoordRefSys "by-reference" object form ({@code {"type":"Reference","href":"...","epoch":...}}).
 *
 * <p>This is only one of the forms the JSON-FG 1.0.0 'coordRefSys' member may take: per
 * {@code coordrefsys.json}, its value can also be a plain URI {@link String} (the form used by
 * most real-world producers, e.g. {@code "coordRefSys": "http://www.opengis.net/def/crs/EPSG/0/3857"}),
 * or an array mixing either form (a compound CRS). For that reason, {@code coordRefSys} accessors
 * on {@link GeoJSONGeometry}, {@link GeoJSONFeature} and {@link GeoJSONFeatureCollection} are typed
 * as {@link Object} rather than this class.</p>
 */
@JsonPropertyOrder({
    JSONFGCoordRefSys.PROPERTY_TYPE,
    JSONFGCoordRefSys.PROPERTY_HREF,
    JSONFGCoordRefSys.PROPERTY_EPOCH
})
public class JSONFGCoordRefSys extends DataTransferObject {

    public static final String TYPE_REFERENCE = "Reference";

    public static final String PROPERTY_HREF = "href";
    public static final String PROPERTY_TYPE = GeoJSONObject.PROPERTY_TYPE;
    public static final String PROPERTY_EPOCH = "epoch";

    private String type;

    private String href;

    private Double epoch;

    public JSONFGCoordRefSys() {
    }

    /**
     * Get type
     *
     * @return type
     */
    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
        return type;
    }

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get href
     *
     * @return href
     */
    @JsonProperty(PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getHref() {
        return href;
    }

    @JsonProperty(PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Get epoch
     *
     * @return epoch
     */
    @JsonProperty(PROPERTY_EPOCH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Double getEpoch() {
        return epoch;
    }

    @JsonProperty(PROPERTY_EPOCH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setEpoch(Double epoch) {
        this.epoch = epoch;
    }

    /**
     * Return true if this CoordRefSys object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGCoordRefSys other = (JSONFGCoordRefSys) o;
        return Objects.equals(this.href, other.href)
                && Objects.equals(this.type, other.type)
                && Objects.equals(this.epoch, other.epoch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, type, epoch);
    }

}
