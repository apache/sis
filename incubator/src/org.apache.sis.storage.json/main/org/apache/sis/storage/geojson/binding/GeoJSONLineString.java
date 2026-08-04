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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GeoJSONLineString
 */
@JsonPropertyOrder({
    GeoJSONLineString.PROPERTY_TYPE,
    GeoJSONLineString.PROPERTY_BBOX,
    GeoJSONLineString.PROPERTY_COORDINATES
})
public class GeoJSONLineString extends GeoJSONGeometry {

    public static final String PROPERTY_COORDINATES = "coordinates";
    private List<List<Double>> coordinates = new ArrayList<>();

    public GeoJSONLineString() {
    }

    @Override
    public String getType() {
        return TYPE_LINESTRING;
    }

    /**
     * Get coordinates
     *
     * @return coordinates
     */
    @JsonProperty(PROPERTY_COORDINATES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    @JsonProperty(PROPERTY_COORDINATES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Return true if this GeoJSON_LineString object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeoJSONLineString geoJSONLineString = (GeoJSONLineString) o;
        return super.equals(o)
                && Objects.equals(this.coordinates, geoJSONLineString.coordinates);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(coordinates);
    }

}
