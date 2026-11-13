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
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Individual;
import org.opengis.metadata.citation.Party;

/**
 * Binding of the {@code CI_PartyUnion} JSON type, the parent of individuals and organisations.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_PartyUnion</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = JSONMDParty.PROPERTY_TYPE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JSONMDIndividual.class,   name = JSONMDIndividual.TYPE),
    @JsonSubTypes.Type(value = JSONMDOrganisation.class, name = JSONMDOrganisation.TYPE)
})
@JsonPropertyOrder({
    JSONMDParty.PROPERTY_TYPE,
    JSONMDParty.PROPERTY_NAME,
    JSONMDParty.PROPERTY_CONTACT_INFO,
    JSONMDParty.PROPERTY_PARTY_IDENTIFIER
})
public abstract class JSONMDParty extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_CONTACT_INFO = "contactInfo";
    public static final String PROPERTY_PARTY_IDENTIFIER = "partyIdentifier";

    @JsonProperty(PROPERTY_NAME)
    private String name;
    @JsonProperty(PROPERTY_CONTACT_INFO)
    private List<JSONMDContact> contactInfo;
    @JsonProperty(PROPERTY_PARTY_IDENTIFIER)
    private List<JSONMDIdentifier> partyIdentifier;

    protected JSONMDParty() {
    }

    protected JSONMDParty(final Party source, final JSONMDEncoder encoder) {
        name            = encoder.text(source.getName());
        contactInfo     = encoder.list(source.getContactInfo(), (Contact e) -> new JSONMDContact(e, encoder));
        partyIdentifier = encoder.list(source.getIdentifiers(), (Identifier e) -> new JSONMDIdentifier(e, encoder));
    }

    /**
     * Creates the binding matching the party type.
     *
     * <p>A party which is neither an individual nor an organisation is written as an organisation.
     */
    public static JSONMDParty create(final Party source, final JSONMDEncoder encoder) {
        if (source instanceof Individual individual) {
            return new JSONMDIndividual(individual, encoder);
        }
        return new JSONMDOrganisation(source, encoder);
    }

    /**
     * Returns the name of the party type in the JSON schema.
     */
    @JsonProperty(PROPERTY_TYPE)
    public abstract String getType();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<JSONMDContact> getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(final List<JSONMDContact> contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<JSONMDIdentifier> getPartyIdentifier() {
        return partyIdentifier;
    }

    public void setPartyIdentifier(final List<JSONMDIdentifier> partyIdentifier) {
        this.partyIdentifier = partyIdentifier;
    }

    public abstract AbstractParty toMetadata(JSONMDDecoder decoder);

    /**
     * Copies the properties common to all parties.
     */
    protected final void fill(final AbstractParty target, final JSONMDDecoder decoder) {
        target.setName(decoder.text(name));
        target.setContactInfo(decoder.list(contactInfo, (JSONMDContact e) -> e.toMetadata(decoder)));
        target.setIdentifiers(decoder.list(partyIdentifier, (JSONMDIdentifier e) -> e.toMetadata(decoder)));
    }
}
