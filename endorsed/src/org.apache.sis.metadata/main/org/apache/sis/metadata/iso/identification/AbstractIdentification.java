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
import java.time.temporal.TemporalAmount;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.identification.ServiceIdentification;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.metadata.MD_Identifier;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.iso.Types;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.citation.ResponsibleParty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.identification.AssociatedResource;


/**
 * Basic information required to uniquely identify a resource or resources.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Identification}
 * {@code   ├─citation………………………………………} Citation data for the resource(s).
 * {@code   │   ├─title……………………………………} Name by which the cited resource is known.
 * {@code   │   └─date………………………………………} Reference date for the cited resource.
 * {@code   ├─abstract………………………………………} Brief narrative summary of the content of the resource(s).
 * {@code   ├─extent……………………………………………} Bounding polygon, vertical, and temporal extent of the dataset.
 * {@code   │   ├─description……………………} The spatial and temporal extent for the referring object.
 * {@code   │   ├─geographicElement……} Geographic component of the extent of the referring object.
 * {@code   │   ├─temporalElement…………} Temporal component of the extent of the referring object.
 * {@code   │   └─verticalElement…………} Vertical component of the extent of the referring object.
 * {@code   └─topicCategory…………………………} Main theme(s) of the dataset.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
@XmlType(name = "AbstractMD_Identification_Type", propOrder = {
    "citation",
    "abstract",
    "purpose",
    "credits",
    "status",
    "pointOfContacts",
    "spatialRepresentationTypes",       // Here in ISO 19115:2014 (was after 'aggregationInfo' in ISO 19115:2003)
    "spatialResolutions",               // Shall be kept next to 'spatialRepresentationTypes'
    "temporalResolution",               // ISO 19115-3 only
    "topicCategories",                  // Here in ISO 19115:2014 (was in subclasses in ISO 19115:2003)
    "extents",                          // Here in ISO 19115:2014 (was in subclasses in ISO 19115:2003)
    "additionalDocumentation",          // ISO 19115:2014 only
    "processingLevel",                  // ISO 19115:2014 only
    "resourceMaintenances",
    "graphicOverviews",
    "resourceFormats",
    "descriptiveKeywords",
    "resourceSpecificUsages",
    "resourceConstraints",
    "associatedResource",
    "aggregationInfo",                  // Legacy ISO 19115:2003 (replaced by 'associatedResources')
    /*
     * NOTE: legacy ISO 19115:2003 specification had 'spatialRepresentationTypes' and 'spatialResolutions'
     *       elements last. If we wanted to produce strictly compliant legacy XML documents, we would have
     *       to duplicate those attributes. We avoid this complexity on the assumption that readers are
     *       tolerant to different order (this relaxation is needed only for legacy XML).
     */
})
@XmlRootElement(name = "AbstractMD_Identification")
@XmlSeeAlso({
    DefaultDataIdentification.class,
    DefaultServiceIdentification.class
})
public class AbstractIdentification extends ISOMetadata implements Identification {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 157053637951213015L;

    /**
     * Citation for the resource(s).
     */
    @SuppressWarnings("serial")
    private Citation citation;

    /**
     * Brief narrative summary of the resource(s).
     */
    @SuppressWarnings("serial")
    private InternationalString abstracts;

    /**
     * Summary of the intentions with which the resource(s) was developed.
     */
    @SuppressWarnings("serial")
    private InternationalString purpose;

    /**
     * Recognition of those who contributed to the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<String> credits;

    /**
     * Status of the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<Progress> status;

    /**
     * Identification of, and means of communication with, person(s) and organizations(s)
     * associated with the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<ResponsibleParty> pointOfContacts;

    /**
     * Methods used to spatially represent geographic information.
     */
    @SuppressWarnings("serial")
    private Collection<SpatialRepresentationType> spatialRepresentationTypes;

    /**
     * Factor which provides a general understanding of the density of spatial data in the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<Resolution> spatialResolutions;

    /**
     * Smallest resolvable temporal period in a resource.
     */
    @SuppressWarnings("serial")
    private Collection<TemporalAmount> temporalResolutions;

    /**
     * Main theme(s) of the resource.
     */
    @SuppressWarnings("serial")
    private Collection<TopicCategory> topicCategories;

    /**
     * Spatial and temporal extent of the resource.
     */
    @SuppressWarnings("serial")
    private Collection<Extent> extents;

    /**
     * Other documentation associated with the resource.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> additionalDocumentations;

    /**
     * Code that identifies the level of processing in the producers coding system of a resource.
     */
    @SuppressWarnings("serial")
    private Identifier processingLevel;

    /**
     * Provides information about the frequency of resource updates, and the scope of those updates.
     */
    @SuppressWarnings("serial")
    private Collection<MaintenanceInformation> resourceMaintenances;

    /**
     * Provides a graphic that illustrates the resource(s) (should include a legend for the graphic).
     */
    @SuppressWarnings("serial")
    private Collection<BrowseGraphic> graphicOverviews;

    /**
     * Provides a description of the format of the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<Format> resourceFormats;

    /**
     * Provides category keywords, their type, and reference source.
     */
    @SuppressWarnings("serial")
    private Collection<Keywords> descriptiveKeywords;

    /**
     * Provides basic information about specific application(s) for which the resource(s)
     * has/have been or is being used by different users.
     */
    @SuppressWarnings("serial")
    private Collection<Usage> resourceSpecificUsages;

    /**
     * Provides information about constraints which apply to the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<Constraints> resourceConstraints;

    /**
     * Provides aggregate dataset information.
     */
    @SuppressWarnings("serial")
    private Collection<AssociatedResource> associatedResources;

    /**
     * Constructs an initially empty identification.
     */
    public AbstractIdentification() {
    }

    /**
     * Creates an identification initialized to the specified values.
     *
     * @param citation   the citation data for the resource(s), or {@code null} if none.
     * @param abstracts  a brief narrative summary of the content of the resource(s), or {@code null} if none.
     */
    public AbstractIdentification(final Citation citation, final CharSequence abstracts) {
        this.citation = citation;
        this.abstracts = Types.toInternationalString(abstracts);
    }

    /**
     * Creates an identification initialized to the specified values.
     *
     * @param citation       the citation data for the resource(s), or {@code null} if none.
     * @param abstracts      a brief narrative summary of the content of the resource(s), or {@code null} if none.
     * @param topicCategory  the main theme of the dataset, or {@code null} if none.
     *
     * @since 1.5
     */
    public AbstractIdentification(final Citation citation, final CharSequence abstracts, final TopicCategory topicCategory) {
        this.citation = citation;
        this.abstracts = Types.toInternationalString(abstracts);
        topicCategories = singleton(topicCategory, TopicCategory.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Identification)
     */
    public AbstractIdentification(final Identification object) {
        super(object);
        if (object != null) {
            citation                   = object.getCitation();
            abstracts                  = object.getAbstract();
            purpose                    = object.getPurpose();
            credits                    = copyCollection(object.getCredits(), String.class);
            status                     = copyCollection(object.getStatus(), Progress.class);
            pointOfContacts            = copyCollection(object.getPointOfContacts(), ResponsibleParty.class);
            spatialRepresentationTypes = copyCollection(object.getSpatialRepresentationTypes(), SpatialRepresentationType.class);
            spatialResolutions         = copyCollection(object.getSpatialResolutions(), Resolution.class);
            temporalResolutions        = copyCollection(object.getTemporalResolutions(), TemporalAmount.class);
            topicCategories            = copyCollection(object.getTopicCategories(), TopicCategory.class);
            extents                    = copyCollection(object.getExtents(), Extent.class);
            additionalDocumentations   = copyCollection(object.getAdditionalDocumentations(), Citation.class);
            processingLevel            = object.getProcessingLevel();
            resourceMaintenances       = copyCollection(object.getResourceMaintenances(), MaintenanceInformation.class);
            graphicOverviews           = copyCollection(object.getGraphicOverviews(), BrowseGraphic.class);
            resourceFormats            = copyCollection(object.getResourceFormats(), Format.class);
            descriptiveKeywords        = copyCollection(object.getDescriptiveKeywords(), Keywords.class);
            resourceSpecificUsages     = copyCollection(object.getResourceSpecificUsages(), Usage.class);
            resourceConstraints        = copyCollection(object.getResourceConstraints(), Constraints.class);
            associatedResources        = copyCollection(object.getAssociatedResources(), AssociatedResource.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link DataIdentification} or
     *       {@link ServiceIdentification}, then this method delegates to the {@code castOrCopy(…)}
     *       method of the corresponding SIS subclass. Note that if the given object implements
     *       more than one of the above-cited interfaces, then the {@code castOrCopy(…)} method
     *       to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractIdentification}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractIdentification} instance is created using the
     *       {@linkplain #AbstractIdentification(Identification) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractIdentification castOrCopy(final Identification object) {
        if (object instanceof DataIdentification) {
            return DefaultDataIdentification.castOrCopy((DataIdentification) object);
        }
        if (object instanceof ServiceIdentification) {
            return DefaultServiceIdentification.castOrCopy((ServiceIdentification) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractIdentification) {
            return (AbstractIdentification) object;
        }
        return new AbstractIdentification(object);
    }

    /**
     * Returns the citation for the resource(s).
     *
     * @return citation for the resource(s).
     */
    @Override
    @XmlElement(name = "citation", required = true)
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the citation for the resource(s).
     *
     * @param  newValue  the new citation.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission(citation);
        citation = newValue;
    }

    /**
     * Returns a brief narrative summary of the resource(s).
     *
     * @return brief narrative summary of the resource(s).
     */
    @Override
    @XmlElement(name = "abstract", required = true)
    public InternationalString getAbstract() {
        return abstracts;
    }

    /**
     * Sets a brief narrative summary of the resource(s).
     *
     * @param  newValue  the new summary of resource(s).
     */
    public void setAbstract(final InternationalString newValue) {
        checkWritePermission(abstracts);
        abstracts = newValue;
    }

    /**
     * Returns a summary of the intentions with which the resource(s) was developed.
     *
     * @return the intentions with which the resource(s) was developed, or {@code null}.
     */
    @Override
    @XmlElement(name = "purpose")
    public InternationalString getPurpose() {
        return purpose;
    }

    /**
     * Sets a summary of the intentions with which the resource(s) was developed.
     *
     * @param  newValue  the new summary of intention.
     */
    public void setPurpose(final InternationalString newValue) {
        checkWritePermission(purpose);
        purpose = newValue;
    }

    /**
     * Returns the recognition of those who contributed to the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type may be changed to the {@code InternationalString} interface in GeoAPI 4.0.
     * </div>
     *
     * @return recognition of those who contributed to the resource(s).
     */
    @Override
    @XmlElement(name = "credit")
    public Collection<String> getCredits() {
        return credits = nonNullCollection(credits, String.class);
    }

    /**
     * Sets the recognition of those who contributed to the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type may be changed to the {@code InternationalString} interface in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new credits.
     */
    public void setCredits(final Collection<? extends String> newValues) {
        credits = writeCollection(newValues, credits, String.class);
    }

    /**
     * Returns the status of the resource(s).
     *
     * @return status of the resource(s), or {@code null}.
     */
    @Override
    @XmlElement(name = "status")
    public Collection<Progress> getStatus() {
        return status = nonNullCollection(status, Progress.class);
    }

    /**
     * Sets the status of the resource(s).
     *
     * @param  newValues  the new status.
     */
    public void setStatus(final Collection<? extends Progress> newValues) {
        status = writeCollection(newValues, status, Progress.class);
    }

    /**
     * Returns the identification of, and means of communication with, person(s) and organizations(s)
     * associated with the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return means of communication with person(s) and organizations(s) associated with the resource(s).
     *
     * @see org.apache.sis.metadata.iso.DefaultMetadata#getContacts()
     */
    @Override
    @XmlElement(name = "pointOfContact")
    public Collection<ResponsibleParty> getPointOfContacts() {
        return pointOfContacts = nonNullCollection(pointOfContacts, ResponsibleParty.class);
    }

    /**
     * Sets the means of communication with persons(s) and organizations(s) associated with the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new points of contacts.
     */
    public void setPointOfContacts(final Collection<? extends ResponsibleParty> newValues) {
        pointOfContacts = writeCollection(newValues, pointOfContacts, ResponsibleParty.class);
    }

    /**
     * Returns the methods used to spatially represent geographic information.
     *
     * @return methods used to spatially represent geographic information.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "spatialRepresentationType")
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return spatialRepresentationTypes = nonNullCollection(spatialRepresentationTypes, SpatialRepresentationType.class);
    }

    /**
     * Sets the method used to spatially represent geographic information.
     *
     * @param  newValues  the new spatial representation types.
     *
     * @since 0.5
     */
    public void setSpatialRepresentationTypes(final Collection<? extends SpatialRepresentationType> newValues) {
        spatialRepresentationTypes = writeCollection(newValues, spatialRepresentationTypes, SpatialRepresentationType.class);
    }

    /**
     * Returns the factor which provides a general understanding of the density of spatial data in the resource(s).
     * This element should be repeated when describing upper and lower range.
     *
     * @return factor which provides a general understanding of the density of spatial data.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "spatialResolution")
    public Collection<Resolution> getSpatialResolutions() {
        return spatialResolutions = nonNullCollection(spatialResolutions, Resolution.class);
    }

    /**
     * Sets the factor which provides a general understanding of the density of spatial data in the resource(s).
     *
     * @param  newValues  the new spatial resolutions.
     *
     * @since 0.5
     */
    public void setSpatialResolutions(final Collection<? extends Resolution> newValues) {
        spatialResolutions = writeCollection(newValues, spatialResolutions, Resolution.class);
    }

    /**
     * Returns the smallest resolvable temporal period in a resource.
     *
     * @return smallest resolvable temporal period in a resource.
     *
     * @since 1.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<TemporalAmount> getTemporalResolutions() {
        return temporalResolutions = nonNullCollection(temporalResolutions, TemporalAmount.class);
    }

    /**
     * Sets the smallest resolvable temporal period in a resource.
     *
     * @param  newValues  the new temporal resolutions.
     *
     * @since 1.5
     */
    public void setTemporalResolutions(final Collection<? extends TemporalAmount> newValues) {
        temporalResolutions = writeCollection(newValues, temporalResolutions, TemporalAmount.class);
    }

    /**
     * Returns the main theme(s) of the resource.
     *
     * @return main theme(s).
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "topicCategory")
    public Collection<TopicCategory> getTopicCategories()  {
        return topicCategories = nonNullCollection(topicCategories, TopicCategory.class);
    }

    /**
     * Sets the main theme(s) of the resource.
     *
     * @param  newValues  the new topic categories.
     *
     * @since 0.5
     */
    public void setTopicCategories(final Collection<? extends TopicCategory> newValues) {
        topicCategories = writeCollection(newValues, topicCategories, TopicCategory.class);
    }

    /**
     * Returns the spatial and temporal extent of the resource.
     *
     * @return spatial and temporal extent of the resource.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "extent")
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets the spatial and temporal extent of the resource.
     *
     * @param  newValues  the new extents
     *
     * @since 0.5
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns other documentation associated with the resource.
     *
     * @return other documentation associated with the resource.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getAdditionalDocumentations() {
        return additionalDocumentations = nonNullCollection(additionalDocumentations, Citation.class);
    }

    /**
     * Sets other documentation associated with the resource.
     *
     * @param  newValues  the documentation to associate with the resource.
     *
     * @since 0.5
     */
    public void setAdditionalDocumentations(final Collection<? extends Citation> newValues) {
        additionalDocumentations = writeCollection(newValues, additionalDocumentations, Citation.class);
    }

    /**
     * Returns code(s) that identifies the level of processing in the producers coding system of a resource.
     *
     * @return code(s) that identifies the level of processing in the producers coding system of a resource.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "processingLevel")
    @XmlJavaTypeAdapter(MD_Identifier.Since2014.class)
    public Identifier getProcessingLevel() {
        return processingLevel;
    }

    /**
     * Sets code that identifies the level of processing in the producers coding system of a resource.
     *
     * @param newValue New code that identifies the level of processing.
     *
     * @since 0.5
     */
    public void setProcessingLevel(final Identifier newValue) {
        checkWritePermission(processingLevel);
        processingLevel = newValue;
    }

    /**
     * Provides information about the frequency of resource updates, and the scope of those updates.
     *
     * @return frequency and scope of resource updates.
     */
    @Override
    @XmlElement(name = "resourceMaintenance")
    public Collection<MaintenanceInformation> getResourceMaintenances() {
        return resourceMaintenances = nonNullCollection(resourceMaintenances, MaintenanceInformation.class);
    }

    /**
     * Sets information about the frequency of resource updates, and the scope of those updates.
     *
     * @param  newValues  the new resource maintenance info.
     */
    public void setResourceMaintenances(final Collection<? extends MaintenanceInformation> newValues) {
        resourceMaintenances = writeCollection(newValues, resourceMaintenances, MaintenanceInformation.class);
    }

    /**
     * Provides a graphic that illustrates the resource(s) (should include a legend for the graphic).
     *
     * @return a graphic that illustrates the resource(s).
     */
    @Override
    @XmlElement(name = "graphicOverview")
    public Collection<BrowseGraphic> getGraphicOverviews() {
        return graphicOverviews = nonNullCollection(graphicOverviews, BrowseGraphic.class);
    }

    /**
     * Sets a graphic that illustrates the resource(s).
     *
     * @param  newValues  the new graphics overviews.
     */
    public void setGraphicOverviews(final Collection<? extends BrowseGraphic> newValues) {
        graphicOverviews = writeCollection(newValues, graphicOverviews, BrowseGraphic.class);
    }

    /**
     * Provides a description of the format of the resource(s).
     *
     * @return description of the format.
     *
     * @see org.apache.sis.metadata.iso.distribution.DefaultDistribution#getDistributionFormats()
     */
    @Override
    @XmlElement(name = "resourceFormat")
    public Collection<Format> getResourceFormats() {
        return resourceFormats = nonNullCollection(resourceFormats, Format.class);
    }

    /**
     * Sets a description of the format of the resource(s).
     *
     * @param  newValues  the new resource format.
     *
     * @see org.apache.sis.metadata.iso.distribution.DefaultDistribution#setDistributionFormats(Collection)
     */
    public void setResourceFormats(final Collection<? extends Format> newValues) {
        resourceFormats = writeCollection(newValues, resourceFormats, Format.class);
    }

    /**
     * Provides category keywords, their type, and reference source.
     *
     * @return category keywords, their type, and reference source.
     */
    @Override
    @XmlElement(name = "descriptiveKeywords")
    public Collection<Keywords> getDescriptiveKeywords() {
        return descriptiveKeywords = nonNullCollection(descriptiveKeywords, Keywords.class);
    }

    /**
     * Sets category keywords, their type, and reference source.
     *
     * @param  newValues  the new descriptive keywords.
     */
    public void setDescriptiveKeywords(final Collection<? extends Keywords> newValues) {
        descriptiveKeywords = writeCollection(newValues, descriptiveKeywords, Keywords.class);
    }

    /**
     * Provides basic information about specific application(s) for which the resource(s)
     * has/have been or is being used by different users.
     *
     * @return information about specific application(s) for which the resource(s)
     *         has/have been or is being used.
     */
    @Override
    @XmlElement(name = "resourceSpecificUsage")
    public Collection<Usage> getResourceSpecificUsages() {
        return resourceSpecificUsages = nonNullCollection(resourceSpecificUsages, Usage.class);
    }

    /**
     * Sets basic information about specific application(s).
     *
     * @param  newValues  the new resource specific usages.
     */
    public void setResourceSpecificUsages(final Collection<? extends Usage> newValues) {
        resourceSpecificUsages = writeCollection(newValues, resourceSpecificUsages, Usage.class);
    }

    /**
     * Provides information about constraints which apply to the resource(s).
     *
     * @return constraints which apply to the resource(s).
     */
    @Override
    @XmlElement(name = "resourceConstraints")
    public Collection<Constraints> getResourceConstraints() {
        return resourceConstraints = nonNullCollection(resourceConstraints, Constraints.class);
    }

    /**
     * Sets information about constraints which apply to the resource(s).
     *
     * @param  newValues  the new resource constraints.
     */
    public void setResourceConstraints(final Collection<? extends Constraints> newValues) {
        resourceConstraints = writeCollection(newValues, resourceConstraints, Constraints.class);
    }

    /**
     * Provides associated resource information.
     *
     * @return associated resource information.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<AssociatedResource> getAssociatedResources() {
        return associatedResources = nonNullCollection(associatedResources, AssociatedResource.class);
    }

    /**
     * Sets associated resource information.
     *
     * @param  newValues  the new associated resources.
     *
     * @since 0.5
     */
    public void setAssociatedResources(final Collection<? extends AssociatedResource> newValues) {
        associatedResources = writeCollection(newValues, associatedResources, AssociatedResource.class);
    }

    /**
     * Provides aggregate dataset information.
     *
     * @return aggregate dataset information.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getAssociatedResources()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getAssociatedResources")
    @XmlElement(name = "aggregationInfo", namespace = LegacyNamespaces.GMD)
    public Collection<AggregateInformation> getAggregationInfo() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new LegacyPropertyAdapter<AggregateInformation,AssociatedResource>(getAssociatedResources()) {
            @Override protected AssociatedResource wrap(final AggregateInformation value) {
                return value;
            }

            @Override protected AggregateInformation unwrap(final AssociatedResource container) {
                return DefaultAggregateInformation.castOrCopy(container);
            }

            @Override protected boolean update(final AssociatedResource container, final AggregateInformation value) {
                return container == value;
            }
        }.validOrNull();
    }

    /**
     * Sets aggregate dataset information.
     *
     * @param  newValues  the new aggregation info.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setAssociatedResources(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setAggregationInfo(final Collection<? extends AggregateInformation> newValues) {
        setAssociatedResources(newValues);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     *
     * @todo Currently, the {@code XmlJavaTypeAdapter} used here just internally converts {@code Duration} objects
     *       into {@code PeriodDuration} objects. Need to add support for {@code IntervalLength} in the future.
     */
    @XmlElement(name = "temporalResolution")
    private Collection<TemporalAmount> getTemporalResolution() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getTemporalResolutions() : null;
    }

    @XmlElement(name = "additionalDocumentation")
    private Collection<Citation> getAdditionalDocumentation() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAdditionalDocumentations() : null;
    }

    @XmlElement(name = "associatedResource")
    private Collection<AssociatedResource> getAssociatedResource() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAssociatedResources() : null;
    }
}
