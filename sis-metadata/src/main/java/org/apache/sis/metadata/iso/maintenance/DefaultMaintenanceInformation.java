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
package org.apache.sis.metadata.iso.maintenance;

import java.util.Collection;
import java.util.Date;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.maintenance.MaintenanceFrequency;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.opengis.temporal.PeriodDuration;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultMaintenanceInformation extends ISOMetadata implements MaintenanceInformation {

    private MaintenanceFrequency maintenanceAndUpdateFrequency;

    private long dateOfNextUpdate = Long.MIN_VALUE;

    private PeriodDuration userDefinedMaintenanceFrequency;

    private Collection<ScopeCode> updateScopes;

    private Collection<ScopeDescription> updateScopeDescriptions;

    private Collection<InternationalString> maintenanceNotes;

    private Collection<ResponsibleParty> contacts;

    @Override
    public synchronized MaintenanceFrequency getMaintenanceAndUpdateFrequency() {
        return maintenanceAndUpdateFrequency;
    }

    @Override
    public synchronized Date getDateOfNextUpdate() {
        return (dateOfNextUpdate != Long.MIN_VALUE) ? new Date(dateOfNextUpdate) : null;
    }

    @Override
    public synchronized PeriodDuration getUserDefinedMaintenanceFrequency() {
        return userDefinedMaintenanceFrequency;
    }

    @Override
    public synchronized Collection<ScopeCode> getUpdateScopes() {
        return updateScopes;
    }

    @Override
    public synchronized Collection<ScopeDescription> getUpdateScopeDescriptions() {
        return updateScopeDescriptions;
    }

    @Override
    public synchronized Collection<InternationalString> getMaintenanceNotes() {
        return maintenanceNotes;
    }

    @Override
    public synchronized Collection<ResponsibleParty> getContacts() {
        return contacts;
    }
}
