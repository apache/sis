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
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.temporal.TemporalAmount;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;

/**
 * Binding of the {@code MD_DataIdentification} JSON type.
 *
 * <p>The {@code graphicOverview}, {@code spatialResolution} and {@code resourceSpecificUsage} properties are extensions
 * to the ISO 19115-4 Core profile.
 *
 * <p>Constraints:
 * <ul>
 *   <li>any {@code Identification} can be written, but only the properties of {@code DataIdentification} are encoded,</li>
 *   <li>temporal resolutions are written as ISO 8601 durations.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_DataIdentification</a>
 */
@JsonPropertyOrder({
    JSONMDDataIdentification.PROPERTY_CITATION,
    JSONMDDataIdentification.PROPERTY_ABSTRACT,
    JSONMDDataIdentification.PROPERTY_PURPOSE,
    JSONMDDataIdentification.PROPERTY_CREDIT,
    JSONMDDataIdentification.PROPERTY_STATUS,
    JSONMDDataIdentification.PROPERTY_POINT_OF_CONTACT,
    JSONMDDataIdentification.PROPERTY_DEFAULT_LOCALE,
    JSONMDDataIdentification.PROPERTY_OTHER_LOCALE,
    JSONMDDataIdentification.PROPERTY_SPATIAL_REPRESENTATION_TYPE,
    JSONMDDataIdentification.PROPERTY_SPATIAL_RESOLUTION,
    JSONMDDataIdentification.PROPERTY_TEMPORAL_RESOLUTION,
    JSONMDDataIdentification.PROPERTY_TOPIC_CATEGORY,
    JSONMDDataIdentification.PROPERTY_EXTENT,
    JSONMDDataIdentification.PROPERTY_ADDITIONAL_DOCUMENTATION,
    JSONMDDataIdentification.PROPERTY_PROCESSING_LEVEL,
    JSONMDDataIdentification.PROPERTY_RESOURCE_MAINTENANCE,
    JSONMDDataIdentification.PROPERTY_GRAPHIC_OVERVIEW,
    JSONMDDataIdentification.PROPERTY_RESOURCE_FORMAT,
    JSONMDDataIdentification.PROPERTY_DESCRIPTIVE_KEYWORDS,
    JSONMDDataIdentification.PROPERTY_RESOURCE_SPECIFIC_USAGE,
    JSONMDDataIdentification.PROPERTY_RESOURCE_CONSTRAINTS,
    JSONMDDataIdentification.PROPERTY_ENVIRONMENT_DESCRIPTION,
    JSONMDDataIdentification.PROPERTY_SUPPLEMENTAL_INFORMATION
})
public final class JSONMDDataIdentification extends DataTransferObject {

    public static final String PROPERTY_CITATION = "citation";
    public static final String PROPERTY_ABSTRACT = "abstract";
    public static final String PROPERTY_PURPOSE = "purpose";
    public static final String PROPERTY_CREDIT = "credit";
    public static final String PROPERTY_STATUS = "status";
    public static final String PROPERTY_POINT_OF_CONTACT = "pointOfContact";
    public static final String PROPERTY_DEFAULT_LOCALE = "defaultLocale";
    public static final String PROPERTY_OTHER_LOCALE = "otherLocale";
    public static final String PROPERTY_SPATIAL_REPRESENTATION_TYPE = "spatialRepresentationType";
    public static final String PROPERTY_SPATIAL_RESOLUTION = "spatialResolution";
    public static final String PROPERTY_TEMPORAL_RESOLUTION = "temporalResolution";
    public static final String PROPERTY_TOPIC_CATEGORY = "topicCategory";
    public static final String PROPERTY_EXTENT = "extent";
    public static final String PROPERTY_ADDITIONAL_DOCUMENTATION = "additionalDocumentation";
    public static final String PROPERTY_PROCESSING_LEVEL = "processingLevel";
    public static final String PROPERTY_RESOURCE_MAINTENANCE = "resourceMaintenance";
    public static final String PROPERTY_GRAPHIC_OVERVIEW = "graphicOverview";
    public static final String PROPERTY_RESOURCE_FORMAT = "resourceFormat";
    public static final String PROPERTY_DESCRIPTIVE_KEYWORDS = "descriptiveKeywords";
    public static final String PROPERTY_RESOURCE_SPECIFIC_USAGE = "resourceSpecificUsage";
    public static final String PROPERTY_RESOURCE_CONSTRAINTS = "resourceConstraints";
    public static final String PROPERTY_ENVIRONMENT_DESCRIPTION = "environmentDescription";
    public static final String PROPERTY_SUPPLEMENTAL_INFORMATION = "supplementalInformation";

    @JsonProperty(PROPERTY_CITATION)
    private JSONMDCitation citation;
    @JsonProperty(PROPERTY_ABSTRACT)
    private String abstracts;
    @JsonProperty(PROPERTY_PURPOSE)
    private String purpose;
    @JsonProperty(PROPERTY_CREDIT)
    private List<String> credit;
    @JsonProperty(PROPERTY_STATUS)
    private List<String> status;
    @JsonProperty(PROPERTY_POINT_OF_CONTACT)
    private List<JSONMDResponsibility> pointOfContact;
    @JsonProperty(PROPERTY_DEFAULT_LOCALE)
    private JSONMDLocale defaultLocale;
    @JsonProperty(PROPERTY_OTHER_LOCALE)
    private List<JSONMDLocale> otherLocale;
    @JsonProperty(PROPERTY_SPATIAL_REPRESENTATION_TYPE)
    private List<String> spatialRepresentationType;
    @JsonProperty(PROPERTY_SPATIAL_RESOLUTION)
    private List<JSONMDResolution> spatialResolution;
    @JsonProperty(PROPERTY_TEMPORAL_RESOLUTION)
    private List<String> temporalResolution;
    @JsonProperty(PROPERTY_TOPIC_CATEGORY)
    private List<String> topicCategory;
    @JsonProperty(PROPERTY_EXTENT)
    private List<JSONMDExtent> extent;
    @JsonProperty(PROPERTY_ADDITIONAL_DOCUMENTATION)
    private List<JSONMDCitation> additionalDocumentation;
    @JsonProperty(PROPERTY_PROCESSING_LEVEL)
    private JSONMDIdentifier processingLevel;
    @JsonProperty(PROPERTY_RESOURCE_MAINTENANCE)
    private List<JSONMDMaintenanceInformation> resourceMaintenance;
    @JsonProperty(PROPERTY_GRAPHIC_OVERVIEW)
    private List<JSONMDBrowseGraphic> graphicOverview;
    @JsonProperty(PROPERTY_RESOURCE_FORMAT)
    private List<JSONMDFormat> resourceFormat;
    @JsonProperty(PROPERTY_DESCRIPTIVE_KEYWORDS)
    private List<JSONMDKeywords> descriptiveKeywords;
    @JsonProperty(PROPERTY_RESOURCE_SPECIFIC_USAGE)
    private List<JSONMDUsage> resourceSpecificUsage;
    @JsonProperty(PROPERTY_RESOURCE_CONSTRAINTS)
    private List<JSONMDConstraints> resourceConstraints;
    @JsonProperty(PROPERTY_ENVIRONMENT_DESCRIPTION)
    private String environmentDescription;
    @JsonProperty(PROPERTY_SUPPLEMENTAL_INFORMATION)
    private String supplementalInformation;

    public JSONMDDataIdentification() {
    }

    public JSONMDDataIdentification(final Identification source, final JSONMDEncoder encoder) {
        citation                  = (source.getCitation() == null) ? null : new JSONMDCitation(source.getCitation(), encoder);
        abstracts                 = encoder.text(source.getAbstract());
        purpose                   = encoder.text(source.getPurpose());
        credit                    = encoder.texts(source.getCredits());
        status                    = encoder.codes(source.getStatus());
        pointOfContact            = encoder.list(source.getPointOfContacts(), (Responsibility e) -> new JSONMDResponsibility(e, encoder));
        spatialRepresentationType = encoder.codes(source.getSpatialRepresentationTypes());
        spatialResolution         = encoder.list(source.getSpatialResolutions(), (Resolution e) -> new JSONMDResolution(e, encoder));
        temporalResolution        = encoder.list(source.getTemporalResolutions(), encoder::duration);
        topicCategory             = encoder.codes(source.getTopicCategories());
        extent                    = encoder.list(source.getExtents(), (Extent e) -> new JSONMDExtent(e, encoder));
        additionalDocumentation   = encoder.list(source.getAdditionalDocumentations(), (Citation e) -> new JSONMDCitation(e, encoder));
        processingLevel           = (source.getProcessingLevel() == null) ? null : new JSONMDIdentifier(source.getProcessingLevel(), encoder);
        resourceMaintenance       = encoder.list(source.getResourceMaintenances(),
                                        (MaintenanceInformation e) -> new JSONMDMaintenanceInformation(e, encoder));
        graphicOverview           = encoder.list(source.getGraphicOverviews(), (BrowseGraphic e) -> new JSONMDBrowseGraphic(e, encoder));
        resourceFormat            = encoder.list(source.getResourceFormats(), (Format e) -> new JSONMDFormat(e, encoder));
        descriptiveKeywords       = encoder.list(source.getDescriptiveKeywords(), (Keywords e) -> new JSONMDKeywords(e, encoder));
        resourceSpecificUsage     = encoder.list(source.getResourceSpecificUsages(), (Usage e) -> new JSONMDUsage(e, encoder));
        resourceConstraints       = encoder.list(source.getResourceConstraints(), (Constraints e) -> JSONMDConstraints.create(e, encoder));
        if (source instanceof DataIdentification data) {
            final List<JSONMDLocale> locales = JSONMDLocale.create(data.getLocalesAndCharsets());
            if (locales != null) {
                defaultLocale = locales.get(0);
                otherLocale   = (locales.size() > 1) ? locales.subList(1, locales.size()) : null;
            }
            environmentDescription  = encoder.text(data.getEnvironmentDescription());
            supplementalInformation = encoder.text(data.getSupplementalInformation());
        }
    }

    public JSONMDCitation getCitation() {
        return citation;
    }

    public void setCitation(final JSONMDCitation citation) {
        this.citation = citation;
    }

    public String getAbstract() {
        return abstracts;
    }

    public void setAbstract(final String abstracts) {
        this.abstracts = abstracts;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(final String purpose) {
        this.purpose = purpose;
    }

    public List<String> getCredit() {
        return credit;
    }

    public void setCredit(final List<String> credit) {
        this.credit = credit;
    }

    public List<String> getStatus() {
        return status;
    }

    public void setStatus(final List<String> status) {
        this.status = status;
    }

    public List<JSONMDResponsibility> getPointOfContact() {
        return pointOfContact;
    }

    public void setPointOfContact(final List<JSONMDResponsibility> pointOfContact) {
        this.pointOfContact = pointOfContact;
    }

    public JSONMDLocale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(final JSONMDLocale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<JSONMDLocale> getOtherLocale() {
        return otherLocale;
    }

    public void setOtherLocale(final List<JSONMDLocale> otherLocale) {
        this.otherLocale = otherLocale;
    }

    public List<String> getSpatialRepresentationType() {
        return spatialRepresentationType;
    }

    public void setSpatialRepresentationType(final List<String> spatialRepresentationType) {
        this.spatialRepresentationType = spatialRepresentationType;
    }

    public List<JSONMDResolution> getSpatialResolution() {
        return spatialResolution;
    }

    public void setSpatialResolution(final List<JSONMDResolution> spatialResolution) {
        this.spatialResolution = spatialResolution;
    }

    public List<String> getTemporalResolution() {
        return temporalResolution;
    }

    public void setTemporalResolution(final List<String> temporalResolution) {
        this.temporalResolution = temporalResolution;
    }

    public List<String> getTopicCategory() {
        return topicCategory;
    }

    public void setTopicCategory(final List<String> topicCategory) {
        this.topicCategory = topicCategory;
    }

    public List<JSONMDExtent> getExtent() {
        return extent;
    }

    public void setExtent(final List<JSONMDExtent> extent) {
        this.extent = extent;
    }

    public List<JSONMDCitation> getAdditionalDocumentation() {
        return additionalDocumentation;
    }

    public void setAdditionalDocumentation(final List<JSONMDCitation> additionalDocumentation) {
        this.additionalDocumentation = additionalDocumentation;
    }

    public JSONMDIdentifier getProcessingLevel() {
        return processingLevel;
    }

    public void setProcessingLevel(final JSONMDIdentifier processingLevel) {
        this.processingLevel = processingLevel;
    }

    public List<JSONMDMaintenanceInformation> getResourceMaintenance() {
        return resourceMaintenance;
    }

    public void setResourceMaintenance(final List<JSONMDMaintenanceInformation> resourceMaintenance) {
        this.resourceMaintenance = resourceMaintenance;
    }

    public List<JSONMDBrowseGraphic> getGraphicOverview() {
        return graphicOverview;
    }

    public void setGraphicOverview(final List<JSONMDBrowseGraphic> graphicOverview) {
        this.graphicOverview = graphicOverview;
    }

    public List<JSONMDFormat> getResourceFormat() {
        return resourceFormat;
    }

    public void setResourceFormat(final List<JSONMDFormat> resourceFormat) {
        this.resourceFormat = resourceFormat;
    }

    public List<JSONMDKeywords> getDescriptiveKeywords() {
        return descriptiveKeywords;
    }

    public void setDescriptiveKeywords(final List<JSONMDKeywords> descriptiveKeywords) {
        this.descriptiveKeywords = descriptiveKeywords;
    }

    public List<JSONMDUsage> getResourceSpecificUsage() {
        return resourceSpecificUsage;
    }

    public void setResourceSpecificUsage(final List<JSONMDUsage> resourceSpecificUsage) {
        this.resourceSpecificUsage = resourceSpecificUsage;
    }

    public List<JSONMDConstraints> getResourceConstraints() {
        return resourceConstraints;
    }

    public void setResourceConstraints(final List<JSONMDConstraints> resourceConstraints) {
        this.resourceConstraints = resourceConstraints;
    }

    public String getEnvironmentDescription() {
        return environmentDescription;
    }

    public void setEnvironmentDescription(final String environmentDescription) {
        this.environmentDescription = environmentDescription;
    }

    public String getSupplementalInformation() {
        return supplementalInformation;
    }

    public void setSupplementalInformation(final String supplementalInformation) {
        this.supplementalInformation = supplementalInformation;
    }

    public DefaultDataIdentification toMetadata(final JSONMDDecoder decoder) {
        final DefaultDataIdentification target = new DefaultDataIdentification();
        if (citation != null) {
            target.setCitation(citation.toMetadata(decoder));
        }
        target.setAbstract(decoder.text(abstracts));
        target.setPurpose(decoder.text(purpose));
        target.setCredits(decoder.texts(credit));
        target.setStatus(decoder.codes(Progress.class, status, Progress::valueOf));
        target.setPointOfContacts(decoder.list(pointOfContact, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
        final Map<Locale, Charset> locales = new LinkedHashMap<>();
        if (defaultLocale != null) {
            defaultLocale.addTo(locales, decoder);
        }
        if (otherLocale != null) {
            for (final JSONMDLocale locale : otherLocale) {
                if (locale != null) {
                    locale.addTo(locales, decoder);
                }
            }
        }
        target.setLocalesAndCharsets(locales);
        target.setSpatialRepresentationTypes(decoder.codes(SpatialRepresentationType.class,
                spatialRepresentationType, SpatialRepresentationType::valueOf));
        target.setSpatialResolutions(decoder.list(spatialResolution, (JSONMDResolution e) -> e.toMetadata(decoder)));
        final List<TemporalAmount> resolutions = decoder.list(temporalResolution, decoder::duration);
        target.setTemporalResolutions(resolutions);
        final List<TopicCategory> topics = decoder.list(topicCategory, decoder::topic);
        target.setTopicCategories(topics);
        target.setExtents(decoder.list(extent, (JSONMDExtent e) -> e.toMetadata(decoder)));
        target.setAdditionalDocumentations(decoder.list(additionalDocumentation, (JSONMDCitation e) -> e.toMetadata(decoder)));
        if (processingLevel != null) {
            target.setProcessingLevel(processingLevel.toMetadata(decoder));
        }
        target.setResourceMaintenances(decoder.list(resourceMaintenance, (JSONMDMaintenanceInformation e) -> e.toMetadata(decoder)));
        target.setGraphicOverviews(decoder.list(graphicOverview, (JSONMDBrowseGraphic e) -> e.toMetadata(decoder)));
        target.setResourceFormats(decoder.list(resourceFormat, (JSONMDFormat e) -> e.toMetadata(decoder)));
        target.setDescriptiveKeywords(decoder.list(descriptiveKeywords, (JSONMDKeywords e) -> e.toMetadata(decoder)));
        target.setResourceSpecificUsages(decoder.list(resourceSpecificUsage, (JSONMDUsage e) -> e.toMetadata(decoder)));
        target.setResourceConstraints(decoder.list(resourceConstraints, (JSONMDConstraints e) -> e.toMetadata(decoder)));
        target.setEnvironmentDescription(decoder.text(environmentDescription));
        target.setSupplementalInformation(decoder.text(supplementalInformation));
        return target;
    }
}
