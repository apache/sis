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
 * JSONFGCurvePolygon, added in JSON-FG version 1.0.0, "Circular Arcs" conformance class.
 *
 * <p>Each element of {@code geometries} (the exterior ring, then holes) shall be a
 * {@link GeoJSONLineString}, a {@link JSONFGCircularString}, or a {@link JSONFGCompoundCurve};
 * this is not enforced by this class.</p>
 */
@JsonPropertyOrder({
    JSONFGCurvePolygon.PROPERTY_TYPE,
    JSONFGCurvePolygon.PROPERTY_BBOX,
    JSONFGCurvePolygon.PROPERTY_GEOMETRIES
})
public class JSONFGCurvePolygon extends GeoJSONGeometry {

    public static final String PROPERTY_GEOMETRIES = "geometries";
    private List<GeoJSONGeometry> geometries = new ArrayList<>();

    public JSONFGCurvePolygon() {
    }

    @Override
    public String getType() {
        return TYPE_CURVEPOLYGON;
    }

    /**
     * Get geometries
     *
     * @return geometries
     */
    @JsonProperty(PROPERTY_GEOMETRIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<GeoJSONGeometry> getGeometries() {
        return geometries;
    }

    @JsonProperty(PROPERTY_GEOMETRIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setGeometries(List<GeoJSONGeometry> geometries) {
        this.geometries = geometries;
    }

    /**
     * Return true if this JSONFGCurvePolygon object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JSONFGCurvePolygon other = (JSONFGCurvePolygon) o;
        return super.equals(o)
                && Objects.equals(this.geometries, other.geometries);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(geometries);
    }

}
