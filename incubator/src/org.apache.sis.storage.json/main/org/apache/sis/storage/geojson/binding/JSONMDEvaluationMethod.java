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
import java.time.temporal.Temporal;
import java.util.List;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationMethod;
import org.apache.sis.metadata.iso.quality.DefaultIndirectEvaluation;
import org.apache.sis.metadata.iso.quality.DefaultSampleBasedInspection;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.EvaluationMethod;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.metadata.quality.IndirectEvaluation;
import org.opengis.metadata.quality.SampleBasedInspection;

/**
 * Binding of the {@code EvaluationMethodUnion} JSON type, all evaluation methods sharing the same properties.
 *
 * <p>The concrete type is given by the {@code type} member, see {@link JSONMDEvaluationMethodKind}.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code name} is not supported by SIS and is ignored with a warning,</li>
 *   <li>{@code deductiveSource} is kept only for {@code IndirectEvaluation},</li>
 *   <li>the sampling properties are kept only for {@code SampleBasedInspection},</li>
 *   <li>at most two {@code dateTime} values are kept.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#EvaluationMethodUnion</a>
 */
@JsonPropertyOrder({
    JSONMDEvaluationMethod.PROPERTY_TYPE,
    JSONMDEvaluationMethod.PROPERTY_NAME,
    JSONMDEvaluationMethod.PROPERTY_EVALUATION_METHOD_DESCRIPTION,
    JSONMDEvaluationMethod.PROPERTY_EVALUATION_METHOD_TYPE,
    JSONMDEvaluationMethod.PROPERTY_EVALUATION_PROCEDURE,
    JSONMDEvaluationMethod.PROPERTY_DATE_TIME,
    JSONMDEvaluationMethod.PROPERTY_REFERENCE_DOC,
    JSONMDEvaluationMethod.PROPERTY_DEDUCTIVE_SOURCE,
    JSONMDEvaluationMethod.PROPERTY_SAMPLING_SCHEME,
    JSONMDEvaluationMethod.PROPERTY_LOT_DESCRIPTION,
    JSONMDEvaluationMethod.PROPERTY_SAMPLING_RATIO
})
public final class JSONMDEvaluationMethod extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_EVALUATION_METHOD_DESCRIPTION = "evaluationMethodDescription";
    public static final String PROPERTY_EVALUATION_METHOD_TYPE = "evaluationMethodType";
    public static final String PROPERTY_EVALUATION_PROCEDURE = "evaluationProcedure";
    public static final String PROPERTY_DATE_TIME = "dateTime";
    public static final String PROPERTY_REFERENCE_DOC = "referenceDoc";
    public static final String PROPERTY_DEDUCTIVE_SOURCE = "deductiveSource";
    public static final String PROPERTY_SAMPLING_SCHEME = "samplingScheme";
    public static final String PROPERTY_LOT_DESCRIPTION = "lotDescription";
    public static final String PROPERTY_SAMPLING_RATIO = "samplingRatio";

    /**
     * Maximal number of dates supported by SIS.
     */
    private static final int MAX_DATES = 2;

    @JsonProperty(PROPERTY_TYPE)
    private String type;
    @JsonProperty(PROPERTY_NAME)
    private String name;
    @JsonProperty(PROPERTY_EVALUATION_METHOD_DESCRIPTION)
    private String evaluationMethodDescription;
    @JsonProperty(PROPERTY_EVALUATION_METHOD_TYPE)
    private String evaluationMethodType;
    @JsonProperty(PROPERTY_EVALUATION_PROCEDURE)
    private JSONMDCitation evaluationProcedure;
    @JsonProperty(PROPERTY_DATE_TIME)
    private List<JSONMDDateOrDateTime> dateTime;
    @JsonProperty(PROPERTY_REFERENCE_DOC)
    private List<JSONMDCitation> referenceDoc;
    @JsonProperty(PROPERTY_DEDUCTIVE_SOURCE)
    private String deductiveSource;
    @JsonProperty(PROPERTY_SAMPLING_SCHEME)
    private String samplingScheme;
    @JsonProperty(PROPERTY_LOT_DESCRIPTION)
    private String lotDescription;
    @JsonProperty(PROPERTY_SAMPLING_RATIO)
    private String samplingRatio;

    public JSONMDEvaluationMethod() {
    }

    public JSONMDEvaluationMethod(final EvaluationMethod source, final JSONMDEncoder encoder) {
        type                        = JSONMDEvaluationMethodKind.of(source).getName();
        evaluationMethodDescription = encoder.text(source.getEvaluationMethodDescription());
        evaluationMethodType        = encoder.code(source.getEvaluationMethodType());
        evaluationProcedure         = (source.getEvaluationProcedure() == null) ? null
                                    : new JSONMDCitation(source.getEvaluationProcedure(), encoder);
        dateTime                    = encoder.list(source.getDates(), encoder::date);
        referenceDoc                = encoder.list(source.getReferenceDocuments(), (Citation e) -> new JSONMDCitation(e, encoder));
        if (source instanceof IndirectEvaluation indirect) {
            deductiveSource = encoder.text(indirect.getDeductiveSource());
        }
        if (source instanceof SampleBasedInspection sample) {
            samplingScheme = encoder.text(sample.getSamplingScheme());
            lotDescription = encoder.text(sample.getLotDescription());
            samplingRatio  = encoder.text(sample.getSamplingRatio());
        }
    }

    /**
     * Returns the name of the evaluation method type in the JSON schema.
     */
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEvaluationMethodDescription() {
        return evaluationMethodDescription;
    }

    public void setEvaluationMethodDescription(final String evaluationMethodDescription) {
        this.evaluationMethodDescription = evaluationMethodDescription;
    }

    public String getEvaluationMethodType() {
        return evaluationMethodType;
    }

    public void setEvaluationMethodType(final String evaluationMethodType) {
        this.evaluationMethodType = evaluationMethodType;
    }

    public JSONMDCitation getEvaluationProcedure() {
        return evaluationProcedure;
    }

    public void setEvaluationProcedure(final JSONMDCitation evaluationProcedure) {
        this.evaluationProcedure = evaluationProcedure;
    }

    public List<JSONMDDateOrDateTime> getDateTime() {
        return dateTime;
    }

    public void setDateTime(final List<JSONMDDateOrDateTime> dateTime) {
        this.dateTime = dateTime;
    }

    public List<JSONMDCitation> getReferenceDoc() {
        return referenceDoc;
    }

    public void setReferenceDoc(final List<JSONMDCitation> referenceDoc) {
        this.referenceDoc = referenceDoc;
    }

    public String getDeductiveSource() {
        return deductiveSource;
    }

    public void setDeductiveSource(final String deductiveSource) {
        this.deductiveSource = deductiveSource;
    }

    public String getSamplingScheme() {
        return samplingScheme;
    }

    public void setSamplingScheme(final String samplingScheme) {
        this.samplingScheme = samplingScheme;
    }

    public String getLotDescription() {
        return lotDescription;
    }

    public void setLotDescription(final String lotDescription) {
        this.lotDescription = lotDescription;
    }

    public String getSamplingRatio() {
        return samplingRatio;
    }

    public void setSamplingRatio(final String samplingRatio) {
        this.samplingRatio = samplingRatio;
    }

    /**
     * Returns the evaluation method, or {@code null} with a warning if the type is unknown.
     */
    public DefaultEvaluationMethod toMetadata(final JSONMDDecoder decoder) {
        final JSONMDEvaluationMethodKind kind = JSONMDEvaluationMethodKind.of(type);
        if (kind == null) {
            decoder.warning("Unknown evaluation method type \"" + type + "\", the evaluation method is ignored.");
            return null;
        }
        if (name != null) {
            decoder.warning("Evaluation method name \"" + name + "\" is not supported and is ignored.");
        }
        final DefaultEvaluationMethod target = kind.newMethod();
        target.setEvaluationMethodDescription(decoder.text(evaluationMethodDescription));
        target.setEvaluationMethodType(decoder.code(EvaluationMethodType.class, evaluationMethodType, EvaluationMethodType::valueOf));
        if (evaluationProcedure != null) {
            target.setEvaluationProcedure(evaluationProcedure.toMetadata(decoder));
        }
        List<Temporal> dates = decoder.list(dateTime, JSONMDDateOrDateTime::getValue);
        if (dates.size() > MAX_DATES) {
            decoder.warning("Evaluation methods have at most " + MAX_DATES + " dates, other dates are ignored.");
            dates = dates.subList(0, MAX_DATES);
        }
        target.setDates(dates);
        target.setReferenceDocuments(decoder.list(referenceDoc, (JSONMDCitation e) -> e.toMetadata(decoder)));
        if (target instanceof DefaultIndirectEvaluation indirect) {
            indirect.setDeductiveSource(decoder.text(deductiveSource));
        } else if (deductiveSource != null) {
            decoder.warning("Deductive source is supported only by indirect evaluations and is ignored.");
        }
        if (target instanceof DefaultSampleBasedInspection sample) {
            sample.setSamplingScheme(decoder.text(samplingScheme));
            sample.setLotDescription(decoder.text(lotDescription));
            sample.setSamplingRatio(decoder.text(samplingRatio));
        } else if (samplingScheme != null || lotDescription != null || samplingRatio != null) {
            decoder.warning("Sampling properties are supported only by sample based inspections and are ignored.");
        }
        return target;
    }
}
