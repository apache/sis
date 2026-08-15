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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.geojson.binding.GeoJSONObject;
import org.apache.sis.storage.ogcjson.Link;


/**
 * MFMovingFeatureCollection, per Moving Features JSON (MF-JSON, OGC 16-140r1).
 *
 * <p>Like {@link MFMovingFeature}, this class extends {@link GeoJSONObject} rather than
 * {@link GeoJSONFeatureCollection}, for the same {@code time} member shape reason.</p>
 */
@JsonPropertyOrder({
    MFMovingFeatureCollection.PROPERTY_TYPE,
    MFMovingFeatureCollection.PROPERTY_LINKS,
    MFMovingFeatureCollection.PROPERTY_TIME_STAMP,
    MFMovingFeatureCollection.PROPERTY_NUMBER_MATCHED,
    MFMovingFeatureCollection.PROPERTY_NUMBER_RETURNED,
    MFMovingFeatureCollection.PROPERTY_CRS,
    MFMovingFeatureCollection.PROPERTY_TRS,
    MFMovingFeatureCollection.PROPERTY_BBOX,
    MFMovingFeatureCollection.PROPERTY_TIME,
    MFMovingFeatureCollection.PROPERTY_LABEL,
    MFMovingFeatureCollection.PROPERTY_FEATURES
})
public class MFMovingFeatureCollection extends GeoJSONObject {

    public static final String PROPERTY_FEATURES = "features";
    public static final String PROPERTY_LINKS = "links";
    public static final String PROPERTY_TIME_STAMP = "timeStamp";
    public static final String PROPERTY_NUMBER_MATCHED = "numberMatched";
    public static final String PROPERTY_NUMBER_RETURNED = "numberReturned";
    public static final String PROPERTY_CRS = "crs";
    public static final String PROPERTY_TRS = "trs";
    public static final String PROPERTY_TIME = "time";
    public static final String PROPERTY_LABEL = "label";

    private List<MFMovingFeature> features = new ArrayList<>();

    private List<Link> links = new ArrayList<>();

    private OffsetDateTime timeStamp;

    private Integer numberMatched;

    private Integer numberReturned;

    private MFCoordRefSys crs;

    private MFCoordRefSys trs;

    private List<String> time = new ArrayList<>();

    private String label;

    public MFMovingFeatureCollection() {
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
    public List<MFMovingFeature> getFeatures() {
        return features;
    }

    @JsonProperty(PROPERTY_FEATURES)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setFeatures(List<MFMovingFeature> features) {
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
     * Get numberMatched
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
     * Get numberReturned
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
     * Get time, the collection's overall lifespan: exactly 2 ISO 8601 date/time
     * strings (start, end), or an empty/null list if not provided.
     *
     * @return time
     */
    @JsonProperty(PROPERTY_TIME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<String> getTime() {
        return time;
    }

    @JsonProperty(PROPERTY_TIME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTime(List<String> time) {
        this.time = time;
    }

    /**
     * Get label
     *
     * @return label
     */
    @JsonProperty(PROPERTY_LABEL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getLabel() {
        return label;
    }

    @JsonProperty(PROPERTY_LABEL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Return true if this MFMovingFeatureCollection object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFMovingFeatureCollection other = (MFMovingFeatureCollection) o;
        return super.equals(o)
                && Objects.equals(this.features, other.features)
                && Objects.equals(this.links, other.links)
                && Objects.equals(this.timeStamp, other.timeStamp)
                && Objects.equals(this.numberMatched, other.numberMatched)
                && Objects.equals(this.numberReturned, other.numberReturned)
                && Objects.equals(this.crs, other.crs)
                && Objects.equals(this.trs, other.trs)
                && Objects.equals(this.time, other.time)
                && Objects.equals(this.label, other.label);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(features, links, timeStamp, numberMatched,
                numberReturned, crs, trs, time, label);
    }

}
