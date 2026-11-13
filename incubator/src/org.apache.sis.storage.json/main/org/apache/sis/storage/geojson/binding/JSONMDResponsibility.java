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
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.extent.Extent;

/**
 * Binding of the {@code CI_Responsibility} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Responsibility</a>
 */
@JsonPropertyOrder({
    JSONMDResponsibility.PROPERTY_ROLE,
    JSONMDResponsibility.PROPERTY_PARTY,
    JSONMDResponsibility.PROPERTY_EXTENT
})
public final class JSONMDResponsibility extends DataTransferObject {

    public static final String PROPERTY_ROLE = "role";
    public static final String PROPERTY_PARTY = "party";
    public static final String PROPERTY_EXTENT = "extent";

    @JsonProperty(PROPERTY_ROLE)
    private String role;
    @JsonProperty(PROPERTY_PARTY)
    private List<JSONMDParty> party;
    @JsonProperty(PROPERTY_EXTENT)
    private List<JSONMDExtent> extent;

    public JSONMDResponsibility() {
    }

    public JSONMDResponsibility(final Responsibility source, final JSONMDEncoder encoder) {
        role   = encoder.code(source.getRole());
        party  = encoder.list(source.getParties(), (Party e) -> JSONMDParty.create(e, encoder));
        extent = encoder.list(source.getExtents(), (Extent e) -> new JSONMDExtent(e, encoder));
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public List<JSONMDParty> getParty() {
        return party;
    }

    public void setParty(final List<JSONMDParty> party) {
        this.party = party;
    }

    public List<JSONMDExtent> getExtent() {
        return extent;
    }

    public void setExtent(final List<JSONMDExtent> extent) {
        this.extent = extent;
    }

    public DefaultResponsibility toMetadata(final JSONMDDecoder decoder) {
        final DefaultResponsibility target = new DefaultResponsibility();
        target.setRole(decoder.code(Role.class, role, Role::valueOf));
        target.setParties(decoder.list(party, (JSONMDParty e) -> e.toMetadata(decoder)));
        target.setExtents(decoder.list(extent, (JSONMDExtent e) -> e.toMetadata(decoder)));
        return target;
    }
}
