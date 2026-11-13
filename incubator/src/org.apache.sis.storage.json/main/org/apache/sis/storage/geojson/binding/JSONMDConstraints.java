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
import java.util.List;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.SecurityConstraints;

/**
 * Binding of the {@code MD_Constraints} JSON type, also the parent of legal and security constraints.
 *
 * <p>An object without {@code type} member is read as {@code MD_Constraints}.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#Abstract_ConstraintUnion</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
              property = JSONMDConstraints.PROPERTY_TYPE, defaultImpl = JSONMDConstraints.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JSONMDConstraints.class,         name = JSONMDConstraints.TYPE),
    @JsonSubTypes.Type(value = JSONMDLegalConstraints.class,    name = JSONMDLegalConstraints.TYPE),
    @JsonSubTypes.Type(value = JSONMDSecurityConstraints.class, name = JSONMDSecurityConstraints.TYPE)
})
@JsonPropertyOrder({
    JSONMDConstraints.PROPERTY_TYPE,
    JSONMDConstraints.PROPERTY_USE_LIMITATION,
    JSONMDConstraints.PROPERTY_CONSTRAINT_APPLICATION_SCOPE,
    JSONMDConstraints.PROPERTY_REFERENCE,
    JSONMDConstraints.PROPERTY_RESPONSIBLE_PARTY
})
public class JSONMDConstraints extends DataTransferObject {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "MD_Constraints";
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_USE_LIMITATION = "useLimitation";
    public static final String PROPERTY_CONSTRAINT_APPLICATION_SCOPE = "constraintApplicationScope";
    public static final String PROPERTY_REFERENCE = "reference";
    public static final String PROPERTY_RESPONSIBLE_PARTY = "responsibleParty";

    @JsonProperty(PROPERTY_USE_LIMITATION)
    private List<String> useLimitation;
    @JsonProperty(PROPERTY_CONSTRAINT_APPLICATION_SCOPE)
    private JSONMDScope constraintApplicationScope;
    @JsonProperty(PROPERTY_REFERENCE)
    private List<JSONMDCitation> reference;
    @JsonProperty(PROPERTY_RESPONSIBLE_PARTY)
    private List<JSONMDResponsibility> responsibleParty;

    public JSONMDConstraints() {
    }

    public JSONMDConstraints(final Constraints source, final JSONMDEncoder encoder) {
        useLimitation              = encoder.texts(source.getUseLimitations());
        constraintApplicationScope = (source.getConstraintApplicationScope() == null) ? null
                                   : new JSONMDScope(source.getConstraintApplicationScope(), encoder);
        reference                  = encoder.list(source.getReferences(), (Citation e) -> new JSONMDCitation(e, encoder));
        responsibleParty           = encoder.list(source.getResponsibleParties(), (Responsibility e) -> new JSONMDResponsibility(e, encoder));
    }

    /**
     * Creates the binding matching the constraints type.
     */
    public static JSONMDConstraints create(final Constraints source, final JSONMDEncoder encoder) {
        if (source instanceof LegalConstraints legal) {
            return new JSONMDLegalConstraints(legal, encoder);
        } else if (source instanceof SecurityConstraints security) {
            return new JSONMDSecurityConstraints(security, encoder);
        }
        return new JSONMDConstraints(source, encoder);
    }

    /**
     * Returns the name of the constraints type in the JSON schema.
     */
    @JsonProperty(PROPERTY_TYPE)
    public String getType() {
        return TYPE;
    }

    public List<String> getUseLimitation() {
        return useLimitation;
    }

    public void setUseLimitation(final List<String> useLimitation) {
        this.useLimitation = useLimitation;
    }

    public JSONMDScope getConstraintApplicationScope() {
        return constraintApplicationScope;
    }

    public void setConstraintApplicationScope(final JSONMDScope constraintApplicationScope) {
        this.constraintApplicationScope = constraintApplicationScope;
    }

    public List<JSONMDCitation> getReference() {
        return reference;
    }

    public void setReference(final List<JSONMDCitation> reference) {
        this.reference = reference;
    }

    public List<JSONMDResponsibility> getResponsibleParty() {
        return responsibleParty;
    }

    public void setResponsibleParty(final List<JSONMDResponsibility> responsibleParty) {
        this.responsibleParty = responsibleParty;
    }

    public DefaultConstraints toMetadata(final JSONMDDecoder decoder) {
        final DefaultConstraints target = new DefaultConstraints();
        fill(target, decoder);
        return target;
    }

    /**
     * Copies the properties common to all constraints.
     */
    protected final void fill(final DefaultConstraints target, final JSONMDDecoder decoder) {
        target.setUseLimitations(decoder.texts(useLimitation));
        if (constraintApplicationScope != null) {
            target.setConstraintApplicationScope(constraintApplicationScope.toMetadata(decoder));
        }
        target.setReferences(decoder.list(reference, (JSONMDCitation e) -> e.toMetadata(decoder)));
        target.setResponsibleParties(decoder.list(responsibleParty, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
    }
}
