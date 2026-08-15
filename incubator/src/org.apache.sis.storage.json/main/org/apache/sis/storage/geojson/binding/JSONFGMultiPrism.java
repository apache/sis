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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * JSONFGMultiPrism
 */
@JsonPropertyOrder({
    JSONFGMultiPrism.PROPERTY_TYPE,
    JSONFGMultiPrism.PROPERTY_BBOX,
    JSONFGMultiPrism.PROPERTY_PRISMS
})
public class JSONFGMultiPrism extends GeoJSONGeometry {

    public static final String PROPERTY_PRISMS = "prisms";
    private List<JSONFGPrism> prisms = new ArrayList<>();

    public JSONFGMultiPrism() {
    }

    @Override
    public String getType() {
        return TYPE_MULTIPRISM;
    }

    /**
     * Get prisms
     *
     * @return prisms
     */
    @JsonProperty(PROPERTY_PRISMS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<JSONFGPrism> getPrisms() {
        return prisms;
    }

    @JsonProperty(PROPERTY_PRISMS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setPrisms(List<JSONFGPrism> prisms) {
        this.prisms = prisms;
    }

    /**
     * Return true if this JSON_FG_Multi_Prism object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGMultiPrism jsONFGMultiPrism = (JSONFGMultiPrism) o;
        return super.equals(o)
                && Objects.equals(this.prisms, jsONFGMultiPrism.prisms);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(prisms);
    }

}
