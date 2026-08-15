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


/**
 * JSONFGPrism
 */
@JsonPropertyOrder({
    JSONFGPrism.PROPERTY_TYPE,
    JSONFGPrism.PROPERTY_BBOX,
    JSONFGPrism.PROPERTY_BASE,
    JSONFGPrism.PROPERTY_LOWER,
    JSONFGPrism.PROPERTY_UPPER
})
public class JSONFGPrism extends GeoJSONGeometry {

    public static final String PROPERTY_BASE = "base";
    private GeoJSONGeometry base;

    public static final String PROPERTY_LOWER = "lower";
    private Double lower;

    public static final String PROPERTY_UPPER = "upper";
    private Double upper;

    public JSONFGPrism() {
    }

    @Override
    public String getType() {
        return TYPE_PRISM;
    }

    /**
     * Get base
     *
     * @return base
     */
    @JsonProperty(PROPERTY_BASE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public GeoJSONGeometry getBase() {
        return base;
    }

    @JsonProperty(PROPERTY_BASE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setBase(GeoJSONGeometry base) {
        this.base = base;
    }

    /**
     * Get lower
     *
     * @return lower
     */
    @JsonProperty(PROPERTY_LOWER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Double getLower() {
        return lower;
    }

    @JsonProperty(PROPERTY_LOWER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setLower(Double lower) {
        this.lower = lower;
    }

    /**
     * Get upper
     *
     * @return upper
     */
    @JsonProperty(PROPERTY_UPPER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public Double getUpper() {
        return upper;
    }

    @JsonProperty(PROPERTY_UPPER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setUpper(Double upper) {
        this.upper = upper;
    }

    /**
     * Return true if this JSON_FG_Prism object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGPrism jsONFGPrism = (JSONFGPrism) o;
        return super.equals(o)
                && Objects.equals(this.base, jsONFGPrism.base)
                && Objects.equals(this.lower, jsONFGPrism.lower)
                && Objects.equals(this.upper, jsONFGPrism.upper);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(base, lower, upper);
    }

}
