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
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.ogcjson.Link;


/**
 * GeoJSONFeature
 */
@JsonPropertyOrder({
    GeoJSONFeature.PROPERTY_TYPE,
    GeoJSONFeature.PROPERTY_ID,
    GeoJSONFeature.PROPERTY_CONFORMS_TO,
    GeoJSONFeature.PROPERTY_FEATURE_TYPE,
    GeoJSONFeature.PROPERTY_FEATURE_SCHEMA,
    GeoJSONFeature.PROPERTY_LINKS,
    GeoJSONFeature.PROPERTY_BBOX,
    GeoJSONFeature.PROPERTY_TIME,
    GeoJSONFeature.PROPERTY_COORD_REF_SYS,
    GeoJSONFeature.PROPERTY_MEASURES,
    GeoJSONFeature.PROPERTY_GEOMETRY,
    GeoJSONFeature.PROPERTY_PLACE,
    GeoJSONFeature.PROPERTY_PROPERTIES
})
public class GeoJSONFeature extends GeoJSONObject {

    //geojson
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_GEOMETRY = "geometry";
    public static final String PROPERTY_PROPERTIES = "properties";
    //added in OGC-API features
    public static final String PROPERTY_LINKS = "links";
    //added in JSON-FG
    public static final String PROPERTY_CONFORMS_TO = "conformsTo";
    public static final String PROPERTY_FEATURE_TYPE = "featureType";
    public static final String PROPERTY_FEATURE_SCHEMA = "featureSchema";
    public static final String PROPERTY_TIME = "time";
    public static final String PROPERTY_COORD_REF_SYS = "coordRefSys";
    public static final String PROPERTY_MEASURES = "measures";
    public static final String PROPERTY_PLACE = "place";


    private Map<String, Object> properties;

    private GeoJSONGeometry geometry;

    private Object id;

    private List<Link> links = new ArrayList<>();

    private List<String> conformsTo = new ArrayList<>();

    private String featureType;

    private Object featureSchema;

    private JSONFGTime time;

    /**
     * A URI {@link String}, a {@link JSONFGCoordRefSys} object, or a {@link List} of
     * either (compound CRS), per the 'coordRefSys' member definition in JSON-FG version 1.0.0.
     */
    private Object coordRefSys;

    private JSONFGMeasures measures;

    private GeoJSONGeometry place;


    public GeoJSONFeature() {
    }

    @Override
    public String getType() {
        return TYPE_FEATURE;
    }

    /**
     * Get id
     *
     * @return id
     */
    @JsonProperty(PROPERTY_ID)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Object getId() {
        return id;
    }

    @JsonProperty(PROPERTY_ID)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Get properties
     *
     * @return properties
     */
    @JsonProperty(PROPERTY_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonProperty(PROPERTY_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Get geometry
     *
     * @return geometry
     */
    @JsonProperty(PROPERTY_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public GeoJSONGeometry getGeometry() {
        return geometry;
    }

    @JsonProperty(PROPERTY_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setGeometry(GeoJSONGeometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Get links
     *
     * @return links
     */
    @JsonProperty(PROPERTY_LINKS)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty(PROPERTY_LINKS)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     * This JSON Schema is part of JSON-FG version 1.0.0. A 'conformsTo' member is only
     * meaningful (and required) when this Feature is serialized as a standalone root
     * document; a Feature nested inside a FeatureCollection's 'features' array must not
     * carry one.
     * @return conformsTo
     */
    @JsonProperty(PROPERTY_CONFORMS_TO)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<String> getConformsTo() {
        return conformsTo;
    }

    @JsonProperty(PROPERTY_CONFORMS_TO)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setConformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    /**
     * Get featureType
     * @return featureType
     */
    @JsonProperty(PROPERTY_FEATURE_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getFeatureType() {
        return featureType;
    }

    @JsonProperty(PROPERTY_FEATURE_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    /**
     * Get featureSchema, either a URI {@link String} or an object mapping feature type
     * names to schema URIs.
     * @return featureSchema
     */
    @JsonProperty(PROPERTY_FEATURE_SCHEMA)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Object getFeatureSchema() {
        return featureSchema;
    }


    @JsonProperty(PROPERTY_FEATURE_SCHEMA)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setFeatureSchema(Object featureSchema) {
        this.featureSchema = featureSchema;
    }

    /**
     * Get time
     * @return time
     */
    @JsonProperty(PROPERTY_TIME)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public JSONFGTime getTime() {
        return time;
    }


    @JsonProperty(PROPERTY_TIME)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setTime(JSONFGTime time) {
        this.time = time;
    }

    /**
     * Get coordRefSys, either a URI {@link String}, a {@link JSONFGCoordRefSys} object, or a
     * {@link List} of either (compound CRS).
     * @return coordRefSys
     */
    @JsonProperty(PROPERTY_COORD_REF_SYS)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public Object getCoordRefSys() {
        return coordRefSys;
    }


    @JsonProperty(PROPERTY_COORD_REF_SYS)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setCoordRefSys(Object coordRefSys) {
        this.coordRefSys = coordRefSys;
    }

    /**
     * Get measures
     * @return measures
     */
    @JsonProperty(PROPERTY_MEASURES)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public JSONFGMeasures getMeasures() {
        return measures;
    }

    @JsonProperty(PROPERTY_MEASURES)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setMeasures(JSONFGMeasures measures) {
        this.measures = measures;
    }

    /**
     * Get place
     * @return place
     */
    @JsonProperty(PROPERTY_PLACE)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public GeoJSONGeometry getPlace() {
        return place;
    }

    @JsonProperty(PROPERTY_PLACE)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setPlace(GeoJSONGeometry place) {
        this.place = place;
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
        GeoJSONFeature other = (GeoJSONFeature) o;
        return super.equals(o)
                && Objects.equals(this.id, other.id)
                && Objects.equals(this.properties, other.properties)
                && Objects.equals(this.geometry, other.geometry)
                && Objects.equals(this.links, other.links)
                && Objects.equals(this.conformsTo, other.conformsTo)
                && Objects.equals(this.featureType, other.featureType)
                && Objects.equals(this.featureSchema, other.featureSchema)
                && Objects.equals(this.time, other.time)
                && Objects.equals(this.place, other.place)
                && Objects.equals(this.coordRefSys, other.coordRefSys)
                && Objects.equals(this.measures, other.measures);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(id, properties, geometry, links, conformsTo, featureType, featureSchema, time, place, coordRefSys, measures);
    }

}
