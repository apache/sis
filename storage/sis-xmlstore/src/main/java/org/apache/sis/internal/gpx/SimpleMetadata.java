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
package org.apache.sis.internal.gpx;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataExtensionInformation;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.PortrayalCatalogueReference;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.AssociatedResource;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.temporal.Duration;
import org.opengis.util.InternationalString;

/**
 * Default simple metadata implementation.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class SimpleMetadata implements Metadata, DataIdentification, Citation {

    @Override
    public Identifier getMetadataIdentifier() {
        return null;
    }

    @Override
    public String getFileIdentifier() {
        return null;
    }

    @Override
    public Collection<Locale> getLanguages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public Collection<Locale> getLocales() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Charset> getCharacterSets() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public CharacterSet getCharacterSet() {
        return null;
    }

    @Override
    public Citation getParentMetadata() {
        return null;
    }

    @Override
    public String getParentIdentifier() {
        return null;
    }

    @Override
    public Collection<? extends MetadataScope> getMetadataScopes() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<ScopeCode> getHierarchyLevels() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<String> getHierarchyLevelNames() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Responsibility> getContacts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends CitationDate> getDateInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Date getDateStamp() {
        return null;
    }

    @Override
    public String getMetadataStandardName() {
        return null;
    }

    @Override
    public String getMetadataStandardVersion() {
        return null;
    }

    @Override
    public Collection<? extends Citation> getMetadataStandards() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Citation> getMetadataProfiles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Citation> getAlternativeMetadataReferences() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends OnlineResource> getMetadataLinkages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getDataSetUri() {
        return null;
    }

    @Override
    public Collection<? extends SpatialRepresentation> getSpatialRepresentationInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends ReferenceSystem> getReferenceSystemInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends MetadataExtensionInformation> getMetadataExtensionInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Identification> getIdentificationInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends ContentInformation> getContentInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Distribution> getDistributionInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends DataQuality> getDataQualityInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Constraints> getMetadataConstraints() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends AcquisitionInformation> getAcquisitionInformation() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public MaintenanceInformation getMetadataMaintenance() {
        return null;
    }

    @Override
    public Collection<? extends Lineage> getResourceLineages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getEnvironmentDescription() {
        return null;
    }

    @Override
    public InternationalString getSupplementalInformation() {
        return null;
    }

    @Override
    public Citation getCitation() {
        return null;
    }

    @Override
    public InternationalString getAbstract() {
        return null;
    }

    @Override
    public InternationalString getPurpose() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getCredits() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Progress> getStatus() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Responsibility> getPointOfContacts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Resolution> getSpatialResolutions() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Duration> getTemporalResolutions() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TopicCategory> getTopicCategories() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Extent> getExtents() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Citation> getAdditionalDocumentations() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Identifier getProcessingLevel() {
        return null;
    }

    @Override
    public Collection<? extends MaintenanceInformation> getResourceMaintenances() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends BrowseGraphic> getGraphicOverviews() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Format> getResourceFormats() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Keywords> getDescriptiveKeywords() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Usage> getResourceSpecificUsages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Constraints> getResourceConstraints() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends AssociatedResource> getAssociatedResources() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends AggregateInformation> getAggregationInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getTitle() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getAlternateTitles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends CitationDate> getDates() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getEdition() {
        return null;
    }

    @Override
    public Date getEditionDate() {
        return null;
    }

    @Override
    public Collection<? extends Identifier> getIdentifiers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Responsibility> getCitedResponsibleParties() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Series getSeries() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getOtherCitationDetails() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getCollectiveTitle() {
        return null;
    }

    @Override
    public String getISBN() {
        return null;
    }

    @Override
    public String getISSN() {
        return null;
    }

    @Override
    public Collection<? extends OnlineResource> getOnlineResources() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends BrowseGraphic> getGraphics() {
        return Collections.EMPTY_LIST;
    }

}
