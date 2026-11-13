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
import java.util.List;
import org.apache.sis.metadata.iso.maintenance.DefaultScopeDescription;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.maintenance.ScopeDescription;

/**
 * Binding of the {@code MD_ScopeDescription} JSON type, a union where exactly one property is set.
 *
 * <p>When reading an object with more than one property, only the first one in the following order is kept:
 * {@code dataset}, {@code features}, {@code attributes}, {@code featureInstances}, {@code attributeInstances}, {@code other}.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_ScopeDescription</a>
 */
@JsonPropertyOrder({
    JSONMDScopeDescription.PROPERTY_ATTRIBUTES,
    JSONMDScopeDescription.PROPERTY_FEATURES,
    JSONMDScopeDescription.PROPERTY_FEATURE_INSTANCES,
    JSONMDScopeDescription.PROPERTY_ATTRIBUTE_INSTANCES,
    JSONMDScopeDescription.PROPERTY_DATASET,
    JSONMDScopeDescription.PROPERTY_OTHER
})
public final class JSONMDScopeDescription extends DataTransferObject {

    public static final String PROPERTY_ATTRIBUTES = "attributes";
    public static final String PROPERTY_FEATURES = "features";
    public static final String PROPERTY_FEATURE_INSTANCES = "featureInstances";
    public static final String PROPERTY_ATTRIBUTE_INSTANCES = "attributeInstances";
    public static final String PROPERTY_DATASET = "dataset";
    public static final String PROPERTY_OTHER = "other";

    @JsonProperty(PROPERTY_ATTRIBUTES)
    private List<String> attributes;
    @JsonProperty(PROPERTY_FEATURES)
    private List<String> features;
    @JsonProperty(PROPERTY_FEATURE_INSTANCES)
    private List<String> featureInstances;
    @JsonProperty(PROPERTY_ATTRIBUTE_INSTANCES)
    private List<String> attributeInstances;
    @JsonProperty(PROPERTY_DATASET)
    private String dataset;
    @JsonProperty(PROPERTY_OTHER)
    private String other;

    public JSONMDScopeDescription() {
    }

    public JSONMDScopeDescription(final ScopeDescription source, final JSONMDEncoder encoder) {
        attributes         = encoder.list(source.getAttributes(), CharSequence::toString);
        features           = encoder.list(source.getFeatures(), CharSequence::toString);
        featureInstances   = encoder.list(source.getFeatureInstances(), CharSequence::toString);
        attributeInstances = encoder.list(source.getAttributeInstances(), CharSequence::toString);
        dataset            = source.getDataset();
        other              = encoder.text(source.getOther());
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(final List<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(final List<String> features) {
        this.features = features;
    }

    public List<String> getFeatureInstances() {
        return featureInstances;
    }

    public void setFeatureInstances(final List<String> featureInstances) {
        this.featureInstances = featureInstances;
    }

    public List<String> getAttributeInstances() {
        return attributeInstances;
    }

    public void setAttributeInstances(final List<String> attributeInstances) {
        this.attributeInstances = attributeInstances;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(final String dataset) {
        this.dataset = dataset;
    }

    public String getOther() {
        return other;
    }

    public void setOther(final String other) {
        this.other = other;
    }

    /**
     * Returns the scope description, or {@code null} with a warning if no property is set.
     */
    public DefaultScopeDescription toMetadata(final JSONMDDecoder decoder) {
        final DefaultScopeDescription target = new DefaultScopeDescription();
        if (dataset != null) {
            target.setDataset(dataset);
        } else if (features != null && !features.isEmpty()) {
            target.setFeatures(decoder.textSet(features));
        } else if (attributes != null && !attributes.isEmpty()) {
            target.setAttributes(decoder.textSet(attributes));
        } else if (featureInstances != null && !featureInstances.isEmpty()) {
            target.setFeatureInstances(decoder.textSet(featureInstances));
        } else if (attributeInstances != null && !attributeInstances.isEmpty()) {
            target.setAttributeInstances(decoder.textSet(attributeInstances));
        } else if (other != null) {
            target.setOther(decoder.text(other));
        } else {
            decoder.warning("Scope description without value is ignored.");
            return null;
        }
        return target;
    }
}
