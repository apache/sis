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
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.AbstractResult;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationMethod;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;

/**
 * Binding of the {@code QualityElementUnion} JSON type, all data quality elements sharing the same properties.
 *
 * <p>The concrete type is given by the {@code type} member, see {@link JSONMDQualityElementType}.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code evaluationMethod} is an array in JSON but a single value in SIS, only the first method is kept,</li>
 *   <li>{@code relatedQualityElement} of metaquality elements is stored with the derived elements, as in ISO 19157:2013.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#QualityElementUnion</a>
 */
@JsonPropertyOrder({
    JSONMDQualityElement.PROPERTY_TYPE,
    JSONMDQualityElement.PROPERTY_MEASURE,
    JSONMDQualityElement.PROPERTY_EVALUATION_METHOD,
    JSONMDQualityElement.PROPERTY_RESULT,
    JSONMDQualityElement.PROPERTY_DERIVED_ELEMENT,
    JSONMDQualityElement.PROPERTY_RELATED_QUALITY_ELEMENT
})
public final class JSONMDQualityElement extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_MEASURE = "measure";
    public static final String PROPERTY_EVALUATION_METHOD = "evaluationMethod";
    public static final String PROPERTY_RESULT = "result";
    public static final String PROPERTY_DERIVED_ELEMENT = "derivedElement";
    public static final String PROPERTY_RELATED_QUALITY_ELEMENT = "relatedQualityElement";

    @JsonProperty(PROPERTY_TYPE)
    private String type;
    @JsonProperty(PROPERTY_MEASURE)
    private JSONMDMeasureReference measure;
    @JsonProperty(PROPERTY_EVALUATION_METHOD)
    private List<JSONMDEvaluationMethod> evaluationMethod;
    @JsonProperty(PROPERTY_RESULT)
    private List<JSONMDQualityResult> result;
    @JsonProperty(PROPERTY_DERIVED_ELEMENT)
    private List<JSONMDQualityElement> derivedElement;
    @JsonProperty(PROPERTY_RELATED_QUALITY_ELEMENT)
    private JSONMDQualityElement relatedQualityElement;

    public JSONMDQualityElement() {
    }

    private JSONMDQualityElement(final Element source, final JSONMDQualityElementType kind, final JSONMDEncoder encoder) {
        type             = kind.getName();
        measure          = (source.getMeasureReference() == null) ? null
                         : new JSONMDMeasureReference(source.getMeasureReference(), encoder);
        evaluationMethod = (source.getEvaluationMethod() == null) ? null
                         : List.of(new JSONMDEvaluationMethod(source.getEvaluationMethod(), encoder));
        result           = encoder.list(source.getResults(), (Result e) -> JSONMDQualityResult.create(e, encoder));
        derivedElement   = encoder.list(source.getDerivedElements(), (Element e) -> create(e, encoder));
    }

    /**
     * Creates the binding of a data quality element.
     *
     * <p>Returns {@code null} with a warning if the element is not of a type listed in {@link JSONMDQualityElementType}.
     */
    public static JSONMDQualityElement create(final Element source, final JSONMDEncoder encoder) {
        final JSONMDQualityElementType kind = JSONMDQualityElementType.of(source);
        if (kind == null) {
            encoder.warning("Quality element type is not supported: " + source.getClass().getName());
            return null;
        }
        return new JSONMDQualityElement(source, kind, encoder);
    }

    /**
     * Returns the name of the quality element type in the JSON schema.
     */
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public JSONMDMeasureReference getMeasure() {
        return measure;
    }

    public void setMeasure(final JSONMDMeasureReference measure) {
        this.measure = measure;
    }

    public List<JSONMDEvaluationMethod> getEvaluationMethod() {
        return evaluationMethod;
    }

    public void setEvaluationMethod(final List<JSONMDEvaluationMethod> evaluationMethod) {
        this.evaluationMethod = evaluationMethod;
    }

    public List<JSONMDQualityResult> getResult() {
        return result;
    }

    public void setResult(final List<JSONMDQualityResult> result) {
        this.result = result;
    }

    public List<JSONMDQualityElement> getDerivedElement() {
        return derivedElement;
    }

    public void setDerivedElement(final List<JSONMDQualityElement> derivedElement) {
        this.derivedElement = derivedElement;
    }

    public JSONMDQualityElement getRelatedQualityElement() {
        return relatedQualityElement;
    }

    public void setRelatedQualityElement(final JSONMDQualityElement relatedQualityElement) {
        this.relatedQualityElement = relatedQualityElement;
    }

    /**
     * Returns the quality element, or {@code null} with a warning if the type is unknown.
     */
    public AbstractElement toMetadata(final JSONMDDecoder decoder) {
        final JSONMDQualityElementType kind = JSONMDQualityElementType.of(type);
        if (kind == null) {
            decoder.warning("Unknown quality element type \"" + type + "\", the element is ignored.");
            return null;
        }
        final AbstractElement target = kind.newElement();
        if (measure != null) {
            target.setMeasureReference(measure.toMetadata(decoder));
        }
        if (evaluationMethod != null && !evaluationMethod.isEmpty()) {
            if (evaluationMethod.size() > 1) {
                decoder.warning("Only the first evaluation method of a quality element is kept.");
            }
            final JSONMDEvaluationMethod first = evaluationMethod.get(0);
            final DefaultEvaluationMethod method = (first == null) ? null : first.toMetadata(decoder);
            target.setEvaluationMethod(method);
        }
        final List<AbstractResult> results = decoder.list(result, (JSONMDQualityResult e) -> e.toMetadata(decoder));
        target.setResults(results);
        final List<AbstractElement> derived = new ArrayList<>(decoder.list(derivedElement, (JSONMDQualityElement e) -> e.toMetadata(decoder)));
        if (relatedQualityElement != null) {
            if (!kind.isMetaquality()) {
                decoder.warning("Related quality element is defined only for metaquality elements, it is stored as a derived element.");
            }
            final AbstractElement related = relatedQualityElement.toMetadata(decoder);
            if (related != null) {
                derived.add(related);
            }
        }
        target.setDerivedElements(derived);
        return target;
    }
}
