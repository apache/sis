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
import org.apache.sis.metadata.iso.maintenance.DefaultMaintenanceInformation;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.maintenance.MaintenanceFrequency;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.Scope;

/**
 * Binding of the {@code MD_MaintenanceInformation} JSON type.
 *
 * <p>The user defined maintenance frequency is written as an ISO 8601 duration.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_MaintenanceInformation</a>
 */
@JsonPropertyOrder({
    JSONMDMaintenanceInformation.PROPERTY_MAINTENANCE_AND_UPDATE_FREQUENCY,
    JSONMDMaintenanceInformation.PROPERTY_MAINTENANCE_DATE,
    JSONMDMaintenanceInformation.PROPERTY_USER_DEFINED_MAINTENANCE_FREQUENCY,
    JSONMDMaintenanceInformation.PROPERTY_MAINTENANCE_SCOPE,
    JSONMDMaintenanceInformation.PROPERTY_MAINTENANCE_NOTE,
    JSONMDMaintenanceInformation.PROPERTY_CONTACT
})
public final class JSONMDMaintenanceInformation extends DataTransferObject {

    public static final String PROPERTY_MAINTENANCE_AND_UPDATE_FREQUENCY = "maintenanceAndUpdateFrequency";
    public static final String PROPERTY_MAINTENANCE_DATE = "maintenanceDate";
    public static final String PROPERTY_USER_DEFINED_MAINTENANCE_FREQUENCY = "userDefinedMaintenanceFrequency";
    public static final String PROPERTY_MAINTENANCE_SCOPE = "maintenanceScope";
    public static final String PROPERTY_MAINTENANCE_NOTE = "maintenanceNote";
    public static final String PROPERTY_CONTACT = "contact";

    @JsonProperty(PROPERTY_MAINTENANCE_AND_UPDATE_FREQUENCY)
    private String maintenanceAndUpdateFrequency;
    @JsonProperty(PROPERTY_MAINTENANCE_DATE)
    private JSONMDDates maintenanceDate;
    @JsonProperty(PROPERTY_USER_DEFINED_MAINTENANCE_FREQUENCY)
    private String userDefinedMaintenanceFrequency;
    @JsonProperty(PROPERTY_MAINTENANCE_SCOPE)
    private List<JSONMDScope> maintenanceScope;
    @JsonProperty(PROPERTY_MAINTENANCE_NOTE)
    private List<String> maintenanceNote;
    @JsonProperty(PROPERTY_CONTACT)
    private List<JSONMDResponsibility> contact;

    public JSONMDMaintenanceInformation() {
    }

    public JSONMDMaintenanceInformation(final MaintenanceInformation source, final JSONMDEncoder encoder) {
        maintenanceAndUpdateFrequency   = encoder.code(source.getMaintenanceAndUpdateFrequency());
        maintenanceDate                 = encoder.dates(source.getMaintenanceDates());
        userDefinedMaintenanceFrequency = encoder.duration(source.getUserDefinedMaintenanceFrequency());
        maintenanceScope                = encoder.list(source.getMaintenanceScopes(), (Scope e) -> new JSONMDScope(e, encoder));
        maintenanceNote                 = encoder.texts(source.getMaintenanceNotes());
        contact                         = encoder.list(source.getContacts(), (Responsibility e) -> new JSONMDResponsibility(e, encoder));
    }

    public String getMaintenanceAndUpdateFrequency() {
        return maintenanceAndUpdateFrequency;
    }

    public void setMaintenanceAndUpdateFrequency(final String maintenanceAndUpdateFrequency) {
        this.maintenanceAndUpdateFrequency = maintenanceAndUpdateFrequency;
    }

    public JSONMDDates getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(final JSONMDDates maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public String getUserDefinedMaintenanceFrequency() {
        return userDefinedMaintenanceFrequency;
    }

    public void setUserDefinedMaintenanceFrequency(final String userDefinedMaintenanceFrequency) {
        this.userDefinedMaintenanceFrequency = userDefinedMaintenanceFrequency;
    }

    public List<JSONMDScope> getMaintenanceScope() {
        return maintenanceScope;
    }

    public void setMaintenanceScope(final List<JSONMDScope> maintenanceScope) {
        this.maintenanceScope = maintenanceScope;
    }

    public List<String> getMaintenanceNote() {
        return maintenanceNote;
    }

    public void setMaintenanceNote(final List<String> maintenanceNote) {
        this.maintenanceNote = maintenanceNote;
    }

    public List<JSONMDResponsibility> getContact() {
        return contact;
    }

    public void setContact(final List<JSONMDResponsibility> contact) {
        this.contact = contact;
    }

    public DefaultMaintenanceInformation toMetadata(final JSONMDDecoder decoder) {
        final DefaultMaintenanceInformation target = new DefaultMaintenanceInformation();
        target.setMaintenanceAndUpdateFrequency(decoder.code(MaintenanceFrequency.class,
                maintenanceAndUpdateFrequency, MaintenanceFrequency::valueOf));
        if (maintenanceDate != null) {
            target.setMaintenanceDates(maintenanceDate.toMetadata(decoder));
        }
        target.setUserDefinedMaintenanceFrequency(decoder.duration(userDefinedMaintenanceFrequency));
        target.setMaintenanceScopes(decoder.list(maintenanceScope, (JSONMDScope e) -> e.toMetadata(decoder)));
        target.setMaintenanceNotes(decoder.texts(maintenanceNote));
        target.setContacts(decoder.list(contact, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
        return target;
    }
}
