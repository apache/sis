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
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.opengis.metadata.citation.Individual;
import org.opengis.metadata.citation.Organisation;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.identification.BrowseGraphic;

/**
 * Binding of the {@code CI_Organisation} JSON type.
 *
 * <p>The {@code logo} property is an extension to the ISO 19115-4 Core profile.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Organisation</a>
 */
@JsonPropertyOrder({
    JSONMDParty.PROPERTY_TYPE,
    JSONMDParty.PROPERTY_NAME,
    JSONMDParty.PROPERTY_CONTACT_INFO,
    JSONMDParty.PROPERTY_PARTY_IDENTIFIER,
    JSONMDOrganisation.PROPERTY_INDIVIDUAL,
    JSONMDOrganisation.PROPERTY_LOGO
})
public final class JSONMDOrganisation extends JSONMDParty {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "CI_Organisation";
    public static final String PROPERTY_INDIVIDUAL = "individual";
    public static final String PROPERTY_LOGO = "logo";

    @JsonProperty(PROPERTY_INDIVIDUAL)
    private List<JSONMDIndividual> individual;
    @JsonProperty(PROPERTY_LOGO)
    private List<JSONMDBrowseGraphic> logo;

    public JSONMDOrganisation() {
    }

    /**
     * Creates the binding of an organisation, or of a party which is neither an individual nor an organisation.
     */
    public JSONMDOrganisation(final Party source, final JSONMDEncoder encoder) {
        super(source, encoder);
        if (source instanceof Organisation organisation) {
            individual = encoder.list(organisation.getIndividual(), (Individual e) -> new JSONMDIndividual(e, encoder));
            logo       = encoder.list(organisation.getLogo(), (BrowseGraphic e) -> new JSONMDBrowseGraphic(e, encoder));
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<JSONMDIndividual> getIndividual() {
        return individual;
    }

    public void setIndividual(final List<JSONMDIndividual> individual) {
        this.individual = individual;
    }

    public List<JSONMDBrowseGraphic> getLogo() {
        return logo;
    }

    public void setLogo(final List<JSONMDBrowseGraphic> logo) {
        this.logo = logo;
    }

    @Override
    public DefaultOrganisation toMetadata(final JSONMDDecoder decoder) {
        final DefaultOrganisation target = new DefaultOrganisation();
        fill(target, decoder);
        target.setIndividual(decoder.list(individual, (JSONMDIndividual e) -> e.toMetadata(decoder)));
        target.setLogo(decoder.list(logo, (JSONMDBrowseGraphic e) -> e.toMetadata(decoder)));
        return target;
    }
}
