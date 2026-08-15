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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static org.apache.sis.storage.geojson.binding.GeoJSONGeometry.PROPERTY_COORD_REF_SYS;
import static org.apache.sis.storage.geojson.binding.GeoJSONGeometry.PROPERTY_MEASURES;
import static org.apache.sis.storage.geojson.binding.GeoJSONObject.PROPERTY_TYPE;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "type")
@JsonSubTypes({
  @Type(value = GeoJSONPoint.class, name = "Point"),
  @Type(value = GeoJSONLineString.class, name = "LineString"),
  @Type(value = GeoJSONPolygon.class, name = "Polygon"),
  @Type(value = GeoJSONMultiPoint.class, name = "MultiPoint"),
  @Type(value = GeoJSONMultiLineString.class, name = "MultiLineString"),
  @Type(value = GeoJSONMultiPolygon.class, name = "MultiPolygon"),
  @Type(value = GeoJSONGeometryCollection.class, name = "GeometryCollection"),
  @Type(value = JSONFGPrism.class, name = "Prism"),
  @Type(value = JSONFGMultiPrism.class, name = "MultiPrism"),
  @Type(value = JSONFGPolyhedron.class, name = "Polyhedron"),
  @Type(value = JSONFGMultiPolyhedron.class, name = "MultiPolyhedron"),
  @Type(value = JSONFGCircularString.class, name = "CircularString"),
  @Type(value = JSONFGCompoundCurve.class, name = "CompoundCurve"),
  @Type(value = JSONFGCurvePolygon.class, name = "CurvePolygon"),
  @Type(value = JSONFGMultiCurve.class, name = "MultiCurve"),
  @Type(value = JSONFGMultiSurface.class, name = "MultiSurface")
})
@JsonPropertyOrder({
    PROPERTY_COORD_REF_SYS,
    PROPERTY_MEASURES
})
public abstract class GeoJSONGeometry extends GeoJSONObject {

    //added in JSON-FG
    public static final String PROPERTY_COORD_REF_SYS = "coordRefSys";
    //added in JSON-FG
    public static final String PROPERTY_MEASURES = "measures";

    /**
     * A URI {@link String}, a {@link JSONFGCoordRefSys} object, or a {@link java.util.List} of
     * either (compound CRS), per the 'coordRefSys' member definition in JSON-FG version 1.0.0.
     */
    private Object coordRefSys;

    private JSONFGMeasures measures;

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Override
    public abstract String getType();

    /**
     * Get coordRefSys
     * @return coordRefSys
     */
    @JsonProperty(PROPERTY_COORD_REF_SYS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Object getCoordRefSys() {
        return coordRefSys;
    }


    @JsonProperty(PROPERTY_COORD_REF_SYS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setCoordRefSys(Object coordRefSys) {
        this.coordRefSys = coordRefSys;
    }

    /**
     * Get measures
     * @return measures
     */
    @JsonProperty(PROPERTY_MEASURES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public JSONFGMeasures getMeasures() {
        return measures;
    }

    @JsonProperty(PROPERTY_MEASURES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setMeasures(JSONFGMeasures measures) {
        this.measures = measures;
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
        GeoJSONGeometry other = (GeoJSONGeometry) o;
        return super.equals(o)
                && Objects.equals(this.coordRefSys, other.coordRefSys)
                && Objects.equals(this.measures, other.measures);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(coordRefSys, measures);
    }
}
