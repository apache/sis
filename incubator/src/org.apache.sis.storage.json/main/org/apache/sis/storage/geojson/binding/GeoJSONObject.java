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
import org.apache.sis.storage.json.DataTransferObject;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonPropertyOrder({
    GeoJSONObject.PROPERTY_TYPE,
    GeoJSONObject.PROPERTY_BBOX
})
public abstract class GeoJSONObject extends DataTransferObject {

    public static final String TYPE_FEATURE = "Feature";
    public static final String TYPE_FEATURE_COLLECTION = "FeatureCollection";
    public static final String TYPE_POINT = "Point";
    public static final String TYPE_LINESTRING = "LineString";
    public static final String TYPE_POLYGON = "Polygon";
    public static final String TYPE_MULTIPOINT = "MultiPoint";
    public static final String TYPE_MULTILINESTRING = "MultiLineString";
    public static final String TYPE_MULTIPOLYGON = "MultiPolygon";
    public static final String TYPE_GEOMETRYCOLLECTION = "GeometryCollection";
    // JSON-FG
    public static final String TYPE_POLYHEDRON = "Polyhedron";
    public static final String TYPE_MULTIPOLYHEDRON = "MultiPolyhedron";
    public static final String TYPE_PRISM = "Prism";
    public static final String TYPE_MULTIPRISM = "MultiPrism";
    // JSON-FG, Circular Arcs conformance class
    public static final String TYPE_CIRCULARSTRING = "CircularString";
    public static final String TYPE_COMPOUNDCURVE = "CompoundCurve";
    public static final String TYPE_CURVEPOLYGON = "CurvePolygon";
    public static final String TYPE_MULTICURVE = "MultiCurve";
    public static final String TYPE_MULTISURFACE = "MultiSurface";

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_BBOX = "bbox";

    private List<Double> bbox = new ArrayList<>();

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public abstract String getType();

    /**
     * Get bbox
     *
     * @return bbox [minx,miny,maxx,maxy]
     */
    @JsonProperty(PROPERTY_BBOX)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Double> getBbox() {
        return bbox;
    }

    @JsonProperty(PROPERTY_BBOX)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setBbox(List<Double> bbox) {
        this.bbox = bbox;
    }

    /**
     * Return true if this GeoJSON_Feature object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GeoJSONObject other = (GeoJSONObject) o;
        return Objects.equals(this.getType(), other.getType())
                && Objects.equals(this.bbox, other.bbox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), bbox);
    }
}
