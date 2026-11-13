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
import org.apache.sis.metadata.iso.constraint.DefaultSecurityConstraints;
import org.opengis.metadata.constraint.Classification;
import org.opengis.metadata.constraint.SecurityConstraints;

/**
 * Binding of the {@code MD_SecurityConstraints} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_SecurityConstraints</a>
 */
@JsonPropertyOrder({
    JSONMDConstraints.PROPERTY_TYPE,
    JSONMDConstraints.PROPERTY_USE_LIMITATION,
    JSONMDConstraints.PROPERTY_CONSTRAINT_APPLICATION_SCOPE,
    JSONMDConstraints.PROPERTY_REFERENCE,
    JSONMDConstraints.PROPERTY_RESPONSIBLE_PARTY,
    JSONMDSecurityConstraints.PROPERTY_CLASSIFICATION,
    JSONMDSecurityConstraints.PROPERTY_USER_NOTE,
    JSONMDSecurityConstraints.PROPERTY_CLASSIFICATION_SYSTEM,
    JSONMDSecurityConstraints.PROPERTY_HANDLING_DESCRIPTION
})
public final class JSONMDSecurityConstraints extends JSONMDConstraints {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "MD_SecurityConstraints";
    public static final String PROPERTY_CLASSIFICATION = "classification";
    public static final String PROPERTY_USER_NOTE = "userNote";
    public static final String PROPERTY_CLASSIFICATION_SYSTEM = "classificationSystem";
    public static final String PROPERTY_HANDLING_DESCRIPTION = "handlingDescription";

    @JsonProperty(PROPERTY_CLASSIFICATION)
    private String classification;
    @JsonProperty(PROPERTY_USER_NOTE)
    private String userNote;
    @JsonProperty(PROPERTY_CLASSIFICATION_SYSTEM)
    private String classificationSystem;
    @JsonProperty(PROPERTY_HANDLING_DESCRIPTION)
    private String handlingDescription;

    public JSONMDSecurityConstraints() {
    }

    public JSONMDSecurityConstraints(final SecurityConstraints source, final JSONMDEncoder encoder) {
        super(source, encoder);
        classification       = encoder.code(source.getClassification());
        userNote             = encoder.text(source.getUserNote());
        classificationSystem = encoder.text(source.getClassificationSystem());
        handlingDescription  = encoder.text(source.getHandlingDescription());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(final String classification) {
        this.classification = classification;
    }

    public String getUserNote() {
        return userNote;
    }

    public void setUserNote(final String userNote) {
        this.userNote = userNote;
    }

    public String getClassificationSystem() {
        return classificationSystem;
    }

    public void setClassificationSystem(final String classificationSystem) {
        this.classificationSystem = classificationSystem;
    }

    public String getHandlingDescription() {
        return handlingDescription;
    }

    public void setHandlingDescription(final String handlingDescription) {
        this.handlingDescription = handlingDescription;
    }

    @Override
    public DefaultSecurityConstraints toMetadata(final JSONMDDecoder decoder) {
        final DefaultSecurityConstraints target = new DefaultSecurityConstraints();
        fill(target, decoder);
        target.setClassification(decoder.code(Classification.class, classification, Classification::valueOf));
        target.setUserNote(decoder.text(userNote));
        target.setClassificationSystem(decoder.text(classificationSystem));
        target.setHandlingDescription(decoder.text(handlingDescription));
        return target;
    }
}
