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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.sis.metadata.iso.quality.AbstractResult;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.CoverageResult;
import org.opengis.metadata.quality.DescriptiveResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;

/**
 * Binding of the {@code QualityResultUnion} JSON type, the parent of all data quality results.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#QualityResultUnion</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
              property = JSONMDQualityResult.PROPERTY_TYPE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JSONMDConformanceResult.class,  name = JSONMDConformanceResult.TYPE),
    @JsonSubTypes.Type(value = JSONMDCoverageResult.class,     name = JSONMDCoverageResult.TYPE),
    @JsonSubTypes.Type(value = JSONMDDescriptiveResult.class,  name = JSONMDDescriptiveResult.TYPE),
    @JsonSubTypes.Type(value = JSONMDQuantitativeResult.class, name = JSONMDQuantitativeResult.TYPE)
})
@JsonPropertyOrder({
    JSONMDQualityResult.PROPERTY_TYPE,
    JSONMDQualityResult.PROPERTY_DATE_TIME,
    JSONMDQualityResult.PROPERTY_RESULT_SCOPE
})
public abstract class JSONMDQualityResult extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_DATE_TIME = "dateTime";
    public static final String PROPERTY_RESULT_SCOPE = "resultScope";

    @JsonProperty(PROPERTY_DATE_TIME)
    private JSONMDDateOrDateTime dateTime;
    @JsonProperty(PROPERTY_RESULT_SCOPE)
    private JSONMDScope resultScope;

    protected JSONMDQualityResult() {
    }

    protected JSONMDQualityResult(final Result source, final JSONMDEncoder encoder) {
        dateTime    = encoder.date(source.getDateTime());
        resultScope = (source.getResultScope() == null) ? null : new JSONMDScope(source.getResultScope(), encoder);
    }

    /**
     * Creates the binding matching the result type.
     *
     * <p>Returns {@code null} with a warning for other result types.
     */
    public static JSONMDQualityResult create(final Result source, final JSONMDEncoder encoder) {
        if (source instanceof ConformanceResult result) {
            return new JSONMDConformanceResult(result, encoder);
        } else if (source instanceof QuantitativeResult result) {
            return new JSONMDQuantitativeResult(result, encoder);
        } else if (source instanceof DescriptiveResult result) {
            return new JSONMDDescriptiveResult(result, encoder);
        } else if (source instanceof CoverageResult result) {
            return new JSONMDCoverageResult(result, encoder);
        }
        encoder.warning("Quality result type is not supported: " + source.getClass().getName());
        return null;
    }

    /**
     * Returns the name of the result type in the JSON schema.
     */
    @JsonProperty(PROPERTY_TYPE)
    public abstract String getType();

    public JSONMDDateOrDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(final JSONMDDateOrDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public JSONMDScope getResultScope() {
        return resultScope;
    }

    public void setResultScope(final JSONMDScope resultScope) {
        this.resultScope = resultScope;
    }

    public abstract AbstractResult toMetadata(JSONMDDecoder decoder);

    /**
     * Copies the properties common to all results.
     */
    protected final void fill(final AbstractResult target, final JSONMDDecoder decoder) {
        if (dateTime != null) {
            target.setDateTime(dateTime.getValue());
        }
        if (resultScope != null) {
            target.setResultScope(resultScope.toMetadata(decoder));
        }
    }
}
