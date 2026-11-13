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
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.opengis.metadata.quality.ConformanceResult;

/**
 * Binding of the {@code ConformanceResult} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#ConformanceResult</a>
 */
@JsonPropertyOrder({
    JSONMDQualityResult.PROPERTY_TYPE,
    JSONMDQualityResult.PROPERTY_DATE_TIME,
    JSONMDQualityResult.PROPERTY_RESULT_SCOPE,
    JSONMDConformanceResult.PROPERTY_SPECIFICATION,
    JSONMDConformanceResult.PROPERTY_EXPLANATION,
    JSONMDConformanceResult.PROPERTY_PASS
})
public final class JSONMDConformanceResult extends JSONMDQualityResult {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "ConformanceResult";
    public static final String PROPERTY_SPECIFICATION = "specification";
    public static final String PROPERTY_EXPLANATION = "explanation";
    public static final String PROPERTY_PASS = "pass";

    @JsonProperty(PROPERTY_SPECIFICATION)
    private JSONMDCitation specification;
    @JsonProperty(PROPERTY_EXPLANATION)
    private String explanation;
    @JsonProperty(PROPERTY_PASS)
    private Boolean pass;

    public JSONMDConformanceResult() {
    }

    public JSONMDConformanceResult(final ConformanceResult source, final JSONMDEncoder encoder) {
        super(source, encoder);
        specification = (source.getSpecification() == null) ? null : new JSONMDCitation(source.getSpecification(), encoder);
        explanation   = encoder.text(source.getExplanation());
        pass          = source.pass();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public JSONMDCitation getSpecification() {
        return specification;
    }

    public void setSpecification(final JSONMDCitation specification) {
        this.specification = specification;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(final String explanation) {
        this.explanation = explanation;
    }

    public Boolean getPass() {
        return pass;
    }

    public void setPass(final Boolean pass) {
        this.pass = pass;
    }

    @Override
    public DefaultConformanceResult toMetadata(final JSONMDDecoder decoder) {
        final DefaultConformanceResult target = new DefaultConformanceResult();
        fill(target, decoder);
        if (specification != null) {
            target.setSpecification(specification.toMetadata(decoder));
        }
        target.setExplanation(decoder.text(explanation));
        target.setPass(pass);
        return target;
    }
}
