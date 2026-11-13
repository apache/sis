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
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.OnlineResource;

/**
 * Binding of the {@code CI_Contact} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Contact</a>
 */
@JsonPropertyOrder({
    JSONMDContact.PROPERTY_PHONE,
    JSONMDContact.PROPERTY_ADDRESS,
    JSONMDContact.PROPERTY_ONLINE_RESOURCE,
    JSONMDContact.PROPERTY_HOURS_OF_SERVICE,
    JSONMDContact.PROPERTY_CONTACT_INSTRUCTIONS,
    JSONMDContact.PROPERTY_CONTACT_TYPE
})
public final class JSONMDContact extends DataTransferObject {

    public static final String PROPERTY_PHONE = "phone";
    public static final String PROPERTY_ADDRESS = "address";
    public static final String PROPERTY_ONLINE_RESOURCE = "onlineResource";
    public static final String PROPERTY_HOURS_OF_SERVICE = "hoursOfService";
    public static final String PROPERTY_CONTACT_INSTRUCTIONS = "contactInstructions";
    public static final String PROPERTY_CONTACT_TYPE = "contactType";

    @JsonProperty(PROPERTY_PHONE)
    private JSONMDTelephones phone;
    @JsonProperty(PROPERTY_ADDRESS)
    private List<JSONMDAddress> address;
    @JsonProperty(PROPERTY_ONLINE_RESOURCE)
    private List<JSONMDOnlineResource> onlineResource;
    @JsonProperty(PROPERTY_HOURS_OF_SERVICE)
    private List<String> hoursOfService;
    @JsonProperty(PROPERTY_CONTACT_INSTRUCTIONS)
    private String contactInstructions;
    @JsonProperty(PROPERTY_CONTACT_TYPE)
    private String contactType;

    public JSONMDContact() {
    }

    public JSONMDContact(final Contact source, final JSONMDEncoder encoder) {
        final JSONMDTelephones phones = new JSONMDTelephones(source.getPhones(), encoder);
        phone               = phones.getNumbers().isEmpty() ? null : phones;
        address             = encoder.list(source.getAddresses(), (Address e) -> new JSONMDAddress(e, encoder));
        onlineResource      = encoder.list(source.getOnlineResources(), (OnlineResource e) -> new JSONMDOnlineResource(e, encoder));
        hoursOfService      = encoder.texts(source.getHoursOfService());
        contactInstructions = encoder.text(source.getContactInstructions());
        contactType         = encoder.text(source.getContactType());
    }

    public JSONMDTelephones getPhone() {
        return phone;
    }

    public void setPhone(final JSONMDTelephones phone) {
        this.phone = phone;
    }

    public List<JSONMDAddress> getAddress() {
        return address;
    }

    public void setAddress(final List<JSONMDAddress> address) {
        this.address = address;
    }

    public List<JSONMDOnlineResource> getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(final List<JSONMDOnlineResource> onlineResource) {
        this.onlineResource = onlineResource;
    }

    public List<String> getHoursOfService() {
        return hoursOfService;
    }

    public void setHoursOfService(final List<String> hoursOfService) {
        this.hoursOfService = hoursOfService;
    }

    public String getContactInstructions() {
        return contactInstructions;
    }

    public void setContactInstructions(final String contactInstructions) {
        this.contactInstructions = contactInstructions;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(final String contactType) {
        this.contactType = contactType;
    }

    public DefaultContact toMetadata(final JSONMDDecoder decoder) {
        final DefaultContact target = new DefaultContact();
        if (phone != null) {
            target.setPhones(phone.toMetadata(decoder));
        }
        target.setAddresses(decoder.list(address, (JSONMDAddress e) -> e.toMetadata(decoder)));
        target.setOnlineResources(decoder.list(onlineResource, (JSONMDOnlineResource e) -> e.toMetadata(decoder)));
        target.setHoursOfService(decoder.texts(hoursOfService));
        target.setContactInstructions(decoder.text(contactInstructions));
        target.setContactType(decoder.text(contactType));
        return target;
    }
}
