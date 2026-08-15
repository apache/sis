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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometry;
import org.apache.sis.storage.geojson.binding.GeoJSONObject;
import org.apache.sis.storage.ogcjson.Link;


/**
 * MFMovingFeature, per Moving Features JSON (MF-JSON, OGC 16-140r1).
 *
 * <p>This class extends {@link GeoJSONObject} rather than {@link GeoJSONFeature}: both
 * reuse {@code type}/{@code id}/{@code geometry}/{@code properties}/{@code bbox} from
 * plain GeoJSON, but MF-JSON's own {@code time} member (a 2-element lifespan array of
 * date strings) is a different shape than JSON-FG's {@code time} member (a
 * {@link JSONFGTime} object) already declared on {@code GeoJSONFeature} — extending it
 * would conflict on that one property, so this class stays a sibling of
 * {@code GeoJSONFeature} instead, both rooted at {@code GeoJSONObject}.</p>
 */
@JsonPropertyOrder({
    MFMovingFeature.PROPERTY_TYPE,
    MFMovingFeature.PROPERTY_ID,
    MFMovingFeature.PROPERTY_CRS,
    MFMovingFeature.PROPERTY_TRS,
    MFMovingFeature.PROPERTY_LINKS,
    MFMovingFeature.PROPERTY_BBOX,
    MFMovingFeature.PROPERTY_TIME,
    MFMovingFeature.PROPERTY_TEMPORAL_GEOMETRY,
    MFMovingFeature.PROPERTY_TEMPORAL_PROPERTIES,
    MFMovingFeature.PROPERTY_GEOMETRY,
    MFMovingFeature.PROPERTY_PROPERTIES
})
public class MFMovingFeature extends GeoJSONObject {

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_GEOMETRY = "geometry";
    public static final String PROPERTY_PROPERTIES = "properties";
    public static final String PROPERTY_LINKS = "links";
    public static final String PROPERTY_CRS = "crs";
    public static final String PROPERTY_TRS = "trs";
    public static final String PROPERTY_TIME = "time";
    public static final String PROPERTY_TEMPORAL_GEOMETRY = "temporalGeometry";
    public static final String PROPERTY_TEMPORAL_PROPERTIES = "temporalProperties";

    private Object id;

    private GeoJSONGeometry geometry;

    private Map<String, Object> properties;

    private List<Link> links = new ArrayList<>();

    private MFCoordRefSys crs;

    private MFCoordRefSys trs;

    private List<String> time = new ArrayList<>();

    private MFTemporalGeometry temporalGeometry;

    private List<MFTemporalPropertyGroup> temporalProperties = new ArrayList<>();

    public MFMovingFeature() {
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
     * Get geometry, the optional static/fallback geometry (as opposed to
     * {@link #getTemporalGeometry()}, which carries the actual movement).
     *
     * @return geometry
     */
    @JsonProperty(PROPERTY_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public GeoJSONGeometry getGeometry() {
        return geometry;
    }

    @JsonProperty(PROPERTY_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public void setGeometry(GeoJSONGeometry geometry) {
        this.geometry = geometry;
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
     * Get time, the feature's lifespan: exactly 2 ISO 8601 date/time strings
     * (start, end), or an empty/null list if not provided.
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
     * Get temporalGeometry
     *
     * @return temporalGeometry
     */
    @JsonProperty(PROPERTY_TEMPORAL_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public MFTemporalGeometry getTemporalGeometry() {
        return temporalGeometry;
    }

    @JsonProperty(PROPERTY_TEMPORAL_GEOMETRY)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTemporalGeometry(MFTemporalGeometry temporalGeometry) {
        this.temporalGeometry = temporalGeometry;
    }

    /**
     * Get temporalProperties
     *
     * @return temporalProperties
     */
    @JsonProperty(PROPERTY_TEMPORAL_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<MFTemporalPropertyGroup> getTemporalProperties() {
        return temporalProperties;
    }

    @JsonProperty(PROPERTY_TEMPORAL_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTemporalProperties(List<MFTemporalPropertyGroup> temporalProperties) {
        this.temporalProperties = temporalProperties;
    }

    /**
     * Return true if this MFMovingFeature object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFMovingFeature other = (MFMovingFeature) o;
        return super.equals(o)
                && Objects.equals(this.id, other.id)
                && Objects.equals(this.geometry, other.geometry)
                && Objects.equals(this.properties, other.properties)
                && Objects.equals(this.links, other.links)
                && Objects.equals(this.crs, other.crs)
                && Objects.equals(this.trs, other.trs)
                && Objects.equals(this.time, other.time)
                && Objects.equals(this.temporalGeometry, other.temporalGeometry)
                && Objects.equals(this.temporalProperties, other.temporalProperties);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(id, geometry, properties, links, crs, trs,
                time, temporalGeometry, temporalProperties);
    }

}
