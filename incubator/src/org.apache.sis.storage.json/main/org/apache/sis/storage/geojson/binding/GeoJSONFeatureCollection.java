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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.sis.storage.ogcjson.Link;


/**
 * FeatureCollection
 */
@JsonPropertyOrder({
    GeoJSONFeatureCollection.PROPERTY_TYPE,
    GeoJSONFeatureCollection.PROPERTY_CONFORMS_TO,
    GeoJSONFeatureCollection.PROPERTY_LINKS,
    GeoJSONFeatureCollection.PROPERTY_TIME_STAMP,
    GeoJSONFeatureCollection.PROPERTY_NUMBER_MATCHED,
    GeoJSONFeatureCollection.PROPERTY_NUMBER_RETURNED,
    GeoJSONFeatureCollection.PROPERTY_FEATURE_TYPE,
    GeoJSONFeatureCollection.PROPERTY_GEOMETRY_DIMENSION,
    GeoJSONFeatureCollection.PROPERTY_FEATURE_SCHEMA,
    GeoJSONFeatureCollection.PROPERTY_COORD_REF_SYS,
    GeoJSONFeatureCollection.PROPERTY_MEASURES,
    GeoJSONFeatureCollection.PROPERTY_BBOX,
    GeoJSONFeatureCollection.PROPERTY_FEATURES,
})
public class GeoJSONFeatureCollection extends GeoJSONObject {

    //geojson
    public static final String PROPERTY_FEATURES = "features";
    //added in OGC-API features
    public static final String PROPERTY_LINKS = "links";
    public static final String PROPERTY_TIME_STAMP = "timeStamp";
    public static final String PROPERTY_NUMBER_MATCHED = "numberMatched";
    public static final String PROPERTY_NUMBER_RETURNED = "numberReturned";
    //added in JSON-FG
    public static final String PROPERTY_CONFORMS_TO = "conformsTo";
    public static final String PROPERTY_FEATURE_TYPE = "featureType";
    public static final String PROPERTY_GEOMETRY_DIMENSION = "geometryDimension";
    public static final String PROPERTY_FEATURE_SCHEMA = "featureSchema";
    public static final String PROPERTY_COORD_REF_SYS = "coordRefSys";
    public static final String PROPERTY_MEASURES = "measures";

    private List<GeoJSONFeature> features = new ArrayList<>();

    private List<Link> links = new ArrayList<>();

    private OffsetDateTime timeStamp;

    private Integer numberMatched;

    private Integer numberReturned;

    private List<String> conformsTo = new ArrayList<>();

    private String featureType;

    private Integer geometryDimension;

    private Object featureSchema;

    /**
     * A URI {@link String}, a {@link JSONFGCoordRefSys} object, or a {@link List} of
     * either (compound CRS), per the 'coordRefSys' member definition in JSON-FG version 1.0.0.
     */
    private Object coordRefSys;

    private JSONFGMeasures measures;

    public GeoJSONFeatureCollection() {
    }

    @Override
    public String getType() {
        return TYPE_FEATURE_COLLECTION;
    }

    /**
     * Get features
     *
     * @return features
     */
    @JsonProperty(PROPERTY_FEATURES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public List<GeoJSONFeature> getFeatures() {
        return features;
    }

    @JsonProperty(PROPERTY_FEATURES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFeatures(List<GeoJSONFeature> features) {
        this.features = features;
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
     * This property indicates the time and date when the response was generated.
     *
     * @return timeStamp
     */
    @JsonProperty(PROPERTY_TIME_STAMP)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    @JsonProperty(PROPERTY_TIME_STAMP)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTimeStamp(OffsetDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * The number of features of the feature type that match the selection parameters like &#x60;bbox&#x60;. minimum: 0
     *
     * @return numberMatched
     */
    @JsonProperty(PROPERTY_NUMBER_MATCHED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Integer getNumberMatched() {
        return numberMatched;
    }

    @JsonProperty(PROPERTY_NUMBER_MATCHED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setNumberMatched(Integer numberMatched) {
        this.numberMatched = numberMatched;
    }

    /**
     * The number of features in the feature collection. A server may omit this information in a response, if the
     * information about the number of features is not known or difficult to compute. If the value is provided, the
     * value shall be identical to the number of items in the \&quot;features\&quot; array. minimum: 0
     *
     * @return numberReturned
     */
    @JsonProperty(PROPERTY_NUMBER_RETURNED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Integer getNumberReturned() {
        return numberReturned;
    }

    @JsonProperty(PROPERTY_NUMBER_RETURNED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setNumberReturned(Integer numberReturned) {
        this.numberReturned = numberReturned;
    }

    /**
     * Get geometryDimension
     * minimum: 0
     * maximum: 3
     * @return geometryDimension
     */
    @JsonProperty(PROPERTY_GEOMETRY_DIMENSION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Integer getGeometryDimension() {
        return geometryDimension;
    }


    @JsonProperty(PROPERTY_GEOMETRY_DIMENSION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setGeometryDimension(Integer geometryDimension) {
        this.geometryDimension = geometryDimension;
    }

    /**
     * This JSON Schema is part of JSON-FG version 1.0.0. A 'conformsTo' member is only
     * meaningful (and required) when this FeatureCollection is serialized as a standalone
     * root document.
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
     * Get coordRefSys, either a URI {@link String}, a {@link JSONFGCoordRefSys} object, or a
     * {@link List} of either (compound CRS).
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
     * Return true if this FeatureCollection object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeoJSONFeatureCollection other = (GeoJSONFeatureCollection) o;
        return super.equals(o)
                && Objects.equals(this.features, other.features)
                && Objects.equals(this.links, other.links)
                && Objects.equals(this.timeStamp, other.timeStamp)
                && Objects.equals(this.numberMatched, other.numberMatched)
                && Objects.equals(this.numberReturned, other.numberReturned)
                && Objects.equals(this.conformsTo, other.conformsTo)
                && Objects.equals(this.geometryDimension, other.geometryDimension)
                && Objects.equals(this.featureType, other.featureType)
                && Objects.equals(this.featureSchema, other.featureSchema)
                && Objects.equals(this.coordRefSys, other.coordRefSys)
                && Objects.equals(this.measures, other.measures);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(features, links, timeStamp, numberMatched,
                numberReturned, conformsTo, geometryDimension, featureType, featureSchema, coordRefSys, measures);
    }

}
