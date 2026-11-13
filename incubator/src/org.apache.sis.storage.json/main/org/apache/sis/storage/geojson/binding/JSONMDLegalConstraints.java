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
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;

/**
 * Binding of the {@code MD_LegalConstraints} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_LegalConstraints</a>
 */
@JsonPropertyOrder({
    JSONMDConstraints.PROPERTY_TYPE,
    JSONMDConstraints.PROPERTY_USE_LIMITATION,
    JSONMDConstraints.PROPERTY_CONSTRAINT_APPLICATION_SCOPE,
    JSONMDConstraints.PROPERTY_REFERENCE,
    JSONMDConstraints.PROPERTY_RESPONSIBLE_PARTY,
    JSONMDLegalConstraints.PROPERTY_ACCESS_CONSTRAINTS,
    JSONMDLegalConstraints.PROPERTY_USE_CONSTRAINTS,
    JSONMDLegalConstraints.PROPERTY_OTHER_CONSTRAINTS
})
public final class JSONMDLegalConstraints extends JSONMDConstraints {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "MD_LegalConstraints";
    public static final String PROPERTY_ACCESS_CONSTRAINTS = "accessConstraints";
    public static final String PROPERTY_USE_CONSTRAINTS = "useConstraints";
    public static final String PROPERTY_OTHER_CONSTRAINTS = "otherConstraints";

    @JsonProperty(PROPERTY_ACCESS_CONSTRAINTS)
    private List<String> accessConstraints;
    @JsonProperty(PROPERTY_USE_CONSTRAINTS)
    private List<String> useConstraints;
    @JsonProperty(PROPERTY_OTHER_CONSTRAINTS)
    private List<String> otherConstraints;

    public JSONMDLegalConstraints() {
    }

    public JSONMDLegalConstraints(final LegalConstraints source, final JSONMDEncoder encoder) {
        super(source, encoder);
        accessConstraints = encoder.codes(source.getAccessConstraints());
        useConstraints    = encoder.codes(source.getUseConstraints());
        otherConstraints  = encoder.texts(source.getOtherConstraints());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<String> getAccessConstraints() {
        return accessConstraints;
    }

    public void setAccessConstraints(final List<String> accessConstraints) {
        this.accessConstraints = accessConstraints;
    }

    public List<String> getUseConstraints() {
        return useConstraints;
    }

    public void setUseConstraints(final List<String> useConstraints) {
        this.useConstraints = useConstraints;
    }

    public List<String> getOtherConstraints() {
        return otherConstraints;
    }

    public void setOtherConstraints(final List<String> otherConstraints) {
        this.otherConstraints = otherConstraints;
    }

    @Override
    public DefaultLegalConstraints toMetadata(final JSONMDDecoder decoder) {
        final DefaultLegalConstraints target = new DefaultLegalConstraints();
        fill(target, decoder);
        target.setAccessConstraints(decoder.codes(Restriction.class, accessConstraints, Restriction::valueOf));
        target.setUseConstraints(decoder.codes(Restriction.class, useConstraints, Restriction::valueOf));
        target.setOtherConstraints(decoder.texts(otherConstraints));
        return target;
    }
}
