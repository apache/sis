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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.metadata.iso.quality.DefaultCoverageResult;
import org.opengis.metadata.quality.CoverageResult;
import org.opengis.metadata.spatial.SpatialRepresentationType;

/**
 * Binding of the {@code CoverageResult} JSON type.
 *
 * <p>Only the spatial representation type is encoded, as in the ISO 19115-4 Core profile.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#CoverageResult</a>
 */
@JsonPropertyOrder({
    JSONMDQualityResult.PROPERTY_TYPE,
    JSONMDQualityResult.PROPERTY_DATE_TIME,
    JSONMDQualityResult.PROPERTY_RESULT_SCOPE,
    JSONMDCoverageResult.PROPERTY_SPATIAL_REPRESENTATION_TYPE
})
public final class JSONMDCoverageResult extends JSONMDQualityResult {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "CoverageResult";
    public static final String PROPERTY_SPATIAL_REPRESENTATION_TYPE = "spatialRepresentationType";

    @JsonProperty(PROPERTY_SPATIAL_REPRESENTATION_TYPE)
    private String spatialRepresentationType;

    public JSONMDCoverageResult() {
    }

    public JSONMDCoverageResult(final CoverageResult source, final JSONMDEncoder encoder) {
        super(source, encoder);
        spatialRepresentationType = encoder.code(source.getSpatialRepresentationType());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getSpatialRepresentationType() {
        return spatialRepresentationType;
    }

    public void setSpatialRepresentationType(final String spatialRepresentationType) {
        this.spatialRepresentationType = spatialRepresentationType;
    }

    @Override
    public DefaultCoverageResult toMetadata(final JSONMDDecoder decoder) {
        final DefaultCoverageResult target = new DefaultCoverageResult();
        fill(target, decoder);
        target.setSpatialRepresentationType(decoder.code(SpatialRepresentationType.class,
                spatialRepresentationType, SpatialRepresentationType::valueOf));
        return target;
    }
}
