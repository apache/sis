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
package org.apache.sis.metadata.iso.identification;

import java.util.Collection;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;


public class AbstractIdentification extends ISOMetadata implements Identification {

    private Citation citation;

    private InternationalString abstracts;

    private InternationalString purpose;

    private Collection<String> credits;

    private Collection<Progress> status;

    private Collection<ResponsibleParty> pointOfContacts;

    private Collection<MaintenanceInformation> resourceMaintenances;

    private Collection<BrowseGraphic> graphicOverviews;

    private Collection<Format> resourceFormats;

    private Collection<Keywords> descriptiveKeywords;

    private Collection<Usage> resourceSpecificUsages;

    private Collection<Constraints> resourceConstraints;

    private Collection<AggregateInformation> aggregationInfo;

    @Override
    public synchronized Citation getCitation() {
        return citation;
    }

    @Override
    public synchronized InternationalString getAbstract() {
        return abstracts;
    }

    @Override
    public synchronized InternationalString getPurpose() {
        return purpose;
    }

    @Override
    public synchronized Collection<String> getCredits() {
        return credits;
    }

    @Override
    public synchronized Collection<Progress> getStatus() {
        return status;
    }

    @Override
    public synchronized Collection<ResponsibleParty> getPointOfContacts() {
        return pointOfContacts;
    }

    @Override
    public synchronized Collection<MaintenanceInformation> getResourceMaintenances() {
        return resourceMaintenances;
    }

    @Override
    public synchronized Collection<BrowseGraphic> getGraphicOverviews() {
        return graphicOverviews;
    }

    @Override
    public synchronized Collection<Format> getResourceFormats() {
        return resourceFormats;
    }

    @Override
    public synchronized Collection<Keywords> getDescriptiveKeywords() {
        return descriptiveKeywords;
    }

    @Override
    public synchronized Collection<Usage> getResourceSpecificUsages() {
        return resourceSpecificUsages;
    }

    @Override
    public synchronized Collection<Constraints> getResourceConstraints() {
        return resourceConstraints;
    }

    @Override
    public synchronized Collection<AggregateInformation> getAggregationInfo() {
        return aggregationInfo;
    }
}
