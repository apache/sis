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
package org.apache.sis.metadata.iso;

import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataExtensionInformation;
import org.opengis.metadata.PortrayalCatalogueReference;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;

public class DefaultMetadata extends ISOMetadata implements Metadata {

    private String fileIdentifier;

    private Locale language;

    private Collection<Locale> locales;

    private CharacterSet characterSet;

    private String parentIdentifier;

    private Collection<ScopeCode> hierarchyLevels;

    private Collection<String> hierarchyLevelNames;

    private Collection<ResponsibleParty> contacts;

    private long dateStamp = Long.MIN_VALUE;

    private String metadataStandardName;

    private String metadataStandardVersion;

    private String dataSetUri;

    private Collection<SpatialRepresentation> spatialRepresentationInfo;

    private Collection<ReferenceSystem> referenceSystemInfo;

    private Collection<MetadataExtensionInformation> metadataExtensionInfo;

    private Collection<Identification> identificationInfo;

    private Collection<ContentInformation> contentInfo;

    private Distribution distributionInfo;

    private Collection<DataQuality> dataQualityInfo;

    private Collection<PortrayalCatalogueReference> portrayalCatalogueInfo;

    private Collection<Constraints> metadataConstraints;

    private Collection<ApplicationSchemaInformation> applicationSchemaInfo;

    private MaintenanceInformation metadataMaintenance;

    private Collection<AcquisitionInformation> acquisitionInformation;

    @Override
    public synchronized String getFileIdentifier() {
        return fileIdentifier;
    }

    @Override
    public synchronized Locale getLanguage() {
        return language;
    }

    @Override
    public synchronized Collection<Locale> getLocales() {
        return locales;
    }

    @Override
    public synchronized CharacterSet getCharacterSet() {
        return characterSet;
    }

    @Override
    public synchronized String getParentIdentifier() {
        return parentIdentifier;
    }

    @Override
    public synchronized Collection<ScopeCode> getHierarchyLevels() {
        return hierarchyLevels;
    }

    @Override
    public synchronized Collection<String> getHierarchyLevelNames() {
        return hierarchyLevelNames;
    }

    @Override
    public synchronized Collection<ResponsibleParty> getContacts() {
        return contacts;
    }

    @Override
    public synchronized Date getDateStamp() {
        return (dateStamp != Long.MIN_VALUE) ? new Date(dateStamp) : null;
    }

    @Override
    public synchronized String getMetadataStandardName() {
        return metadataStandardName;
    }

    @Override
    public synchronized String getMetadataStandardVersion() {
        return metadataStandardVersion;
    }

    @Override
    public synchronized String getDataSetUri() {
        return dataSetUri;
    }

    @Override
    public synchronized Collection<SpatialRepresentation> getSpatialRepresentationInfo() {
        return spatialRepresentationInfo;
    }

    @Override
    public synchronized Collection<ReferenceSystem> getReferenceSystemInfo() {
        return referenceSystemInfo;
    }

    @Override
    public synchronized Collection<MetadataExtensionInformation> getMetadataExtensionInfo() {
        return metadataExtensionInfo;
    }

    @Override
    public synchronized Collection<Identification> getIdentificationInfo() {
        return identificationInfo;
    }

    @Override
    public synchronized Collection<ContentInformation> getContentInfo() {
        return contentInfo;
    }

    @Override
    public synchronized Distribution getDistributionInfo() {
        return distributionInfo;
    }

    @Override
    public synchronized Collection<DataQuality> getDataQualityInfo() {
        return dataQualityInfo;
    }

    @Override
    public synchronized Collection<PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return portrayalCatalogueInfo;
    }

    @Override
    public synchronized Collection<Constraints> getMetadataConstraints() {
        return metadataConstraints;
    }

    @Override
    public synchronized Collection<ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return applicationSchemaInfo;
    }

    @Override
    public synchronized MaintenanceInformation getMetadataMaintenance() {
        return metadataMaintenance;
    }

    @Override
    public synchronized Collection<AcquisitionInformation> getAcquisitionInformation() {
        return acquisitionInformation;
    }
}
