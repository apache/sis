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

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
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
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Basic information required to uniquely identify a resource or resources.
 *
 * <p><b>Limitations:</b></p>
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
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "AbstractMD_Identification_Type", propOrder = {
    "citation",
    "abstract",
    "purpose",
    "credits",
    "status",
    "pointOfContacts",
    "resourceMaintenances",
    "graphicOverviews",
    "resourceFormats",
    "descriptiveKeywords",
    "resourceSpecificUsages",
    "resourceConstraints",
    "aggregationInfo",
    "spatialRepresentationTypes", // After 'pointOfContact' according ISO 19115:2014, but here for ISO 19115:2003 compatibility.
    "spatialResolutions"          // Shall be kept next to 'spatialRepresentationTypes'
})
@XmlRootElement(name = "MD_Identification")
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
    private Citation citation;

    /**
     * Brief narrative summary of the resource(s).
     */
    private InternationalString abstracts;

    /**
     * Summary of the intentions with which the resource(s) was developed.
     */
    private InternationalString purpose;

    /**
     * Recognition of those who contributed to the resource(s).
     */
    private Collection<String> credits;

    /**
     * Status of the resource(s).
     */
    private Collection<Progress> status;

    /**
     * Identification of, and means of communication with, person(s) and organizations(s)
     * associated with the resource(s).
     */
    private Collection<ResponsibleParty> pointOfContacts;

    /**
     * Methods used to spatially represent geographic information.
     */
    private Collection<SpatialRepresentationType> spatialRepresentationTypes;

    /**
     * Factor which provides a general understanding of the density of spatial data in the resource(s).
     */
    private Collection<Resolution> spatialResolutions;

    /**
     * Main theme(s) of the resource.
     */
    private Collection<TopicCategory> topicCategories;

    /**
     * Spatial and temporal extent of the resource.
     */
    private Collection<Extent> extents;

    /**
     * Other documentation associated with the resource.
     */
    private Collection<Citation> additionalDocumentations;

    /**
     * Code that identifies the level of processing in the producers coding system of a resource.
     */
    private Identifier processingLevel;

    /**
     * Provides information about the frequency of resource updates, and the scope of those updates.
     */
    private Collection<MaintenanceInformation> resourceMaintenances;

    /**
     * Provides a graphic that illustrates the resource(s) (should include a legend for the graphic).
     */
    private Collection<BrowseGraphic> graphicOverviews;

    /**
     * Provides a description of the format of the resource(s).
     */
    private Collection<Format> resourceFormats;

    /**
     * Provides category keywords, their type, and reference source.
     */
    private Collection<Keywords> descriptiveKeywords;

    /**
     * Provides basic information about specific application(s) for which the resource(s)
     * has/have been or is being used by different users.
     */
    private Collection<Usage> resourceSpecificUsages;

    /**
     * Provides information about constraints which apply to the resource(s).
     */
    private Collection<Constraints> resourceConstraints;

    /**
     * Provides aggregate dataset information.
     */
    private Collection<DefaultAssociatedResource> associatedResources;

    /**
     * Constructs an initially empty identification.
     */
    public AbstractIdentification() {
    }

    /**
     * Creates an identification initialized to the specified values.
     *
     * @param citation  The citation data for the resource(s), or {@code null} if none.
     * @param abstracts A brief narrative summary of the content of the resource(s), or {@code null} if none.
     */
    public AbstractIdentification(final Citation citation, final CharSequence abstracts) {
        this.citation = citation;
        this.abstracts = Types.toInternationalString(abstracts);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
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
            resourceMaintenances       = copyCollection(object.getResourceMaintenances(), MaintenanceInformation.class);
            graphicOverviews           = copyCollection(object.getGraphicOverviews(), BrowseGraphic.class);
            resourceFormats            = copyCollection(object.getResourceFormats(), Format.class);
            descriptiveKeywords        = copyCollection(object.getDescriptiveKeywords(), Keywords.class);
            resourceSpecificUsages     = copyCollection(object.getResourceSpecificUsages(), Usage.class);
            resourceConstraints        = copyCollection(object.getResourceConstraints(), Constraints.class);
            if (object instanceof AbstractIdentification) {
                final AbstractIdentification c = (AbstractIdentification) object;
                spatialRepresentationTypes = copyCollection(c.getSpatialRepresentationTypes(), SpatialRepresentationType.class);
                spatialResolutions         = copyCollection(c.getSpatialResolutions(), Resolution.class);
                topicCategories            = copyCollection(c.getTopicCategories(), TopicCategory.class);
                extents                    = copyCollection(c.getExtents(), Extent.class);
                additionalDocumentations   = copyCollection(c.getAdditionalDocumentations(), Citation.class);
                processingLevel            = c.getProcessingLevel();
                associatedResources        = copyCollection(c.getAssociatedResources(), DefaultAssociatedResource.class);
            } else {
                setAggregationInfo(object.getAggregationInfo());
            }
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
     *       {@linkplain #AbstractIdentification(Identification) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Citation for the resource(s).
     */
    @Override
    @XmlElement(name = "citation", required = true)
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the citation for the resource(s).
     *
     * @param newValue The new citation.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns a brief narrative summary of the resource(s).
     *
     * @return Brief narrative summary of the resource(s).
     */
    @Override
    @XmlElement(name = "abstract", required = true)
    public InternationalString getAbstract() {
        return abstracts;
    }

    /**
     * Sets a brief narrative summary of the resource(s).
     *
     * @param newValue The new summary of resource(s).
     */
    public void setAbstract(final InternationalString newValue) {
        checkWritePermission();
        abstracts = newValue;
    }

    /**
     * Returns a summary of the intentions with which the resource(s) was developed.
     *
     * @return The intentions with which the resource(s) was developed, or {@code null}.
     */
    @Override
    @XmlElement(name = "purpose")
    public InternationalString getPurpose() {
        return purpose;
    }

    /**
     * Sets a summary of the intentions with which the resource(s) was developed.
     *
     * @param newValue The new summary of intention.
     */
    public void setPurpose(final InternationalString newValue) {
        checkWritePermission();
        purpose = newValue;
    }

    /**
     * Returns the recognition of those who contributed to the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type may be changed to the {@code InternationalString} interface in GeoAPI 4.0.
     * </div>
     *
     * @return Recognition of those who contributed to the resource(s).
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
     * @param newValues The new credits.
     */
    public void setCredits(final Collection<? extends String> newValues) {
        credits = writeCollection(newValues, credits, String.class);
    }

    /**
     * Returns the status of the resource(s).
     *
     * @return Status of the resource(s), or {@code null}.
     */
    @Override
    @XmlElement(name = "status")
    public Collection<Progress> getStatus() {
        return status = nonNullCollection(status, Progress.class);
    }

    /**
     * Sets the status of the resource(s).
     *
     * @param newValues The new status.
     */
    public void setStatus(final Collection<? extends Progress> newValues) {
        status = writeCollection(newValues, status, Progress.class);
    }

    /**
     * Returns the identification of, and means of communication with, person(s) and organizations(s)
     * associated with the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return Means of communication with person(s) and organizations(s) associated with the resource(s).
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
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValues The new points of contacts.
     */
    public void setPointOfContacts(final Collection<? extends ResponsibleParty> newValues) {
        pointOfContacts = writeCollection(newValues, pointOfContacts, ResponsibleParty.class);
    }

    /**
     * Returns the methods used to spatially represent geographic information.
     *
     * @return Methods used to spatially represent geographic information.
     *
     * @since 0.5
     */
    @XmlElement(name = "spatialRepresentationType")
    @UML(identifier="spatialRepresentationType", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return spatialRepresentationTypes = nonNullCollection(spatialRepresentationTypes, SpatialRepresentationType.class);
    }

    /**
     * Sets the method used to spatially represent geographic information.
     *
     * @param newValues The new spatial representation types.
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
     * @return Factor which provides a general understanding of the density of spatial data.
     *
     * @since 0.5
     */
    @XmlElement(name = "spatialResolution")
    @UML(identifier="spatialResolution", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Resolution> getSpatialResolutions() {
        return spatialResolutions = nonNullCollection(spatialResolutions, Resolution.class);
    }

    /**
     * Sets the factor which provides a general understanding of the density of spatial data in the resource(s).
     *
     * @param newValues The new spatial resolutions.
     *
     * @since 0.5
     */
    public void setSpatialResolutions(final Collection<? extends Resolution> newValues) {
        spatialResolutions = writeCollection(newValues, spatialResolutions, Resolution.class);
    }

    /**
     * Returns the main theme(s) of the resource.
     *
     * @return Main theme(s).
     *
     * @since 0.5
     */
/// @XmlElement(name = "topicCategory")
    @UML(identifier="topicCategory", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<TopicCategory> getTopicCategories()  {
        return topicCategories = nonNullCollection(topicCategories, TopicCategory.class);
    }

    /**
     * Sets the main theme(s) of the resource.
     *
     * @param newValues The new topic categories.
     *
     * @since 0.5
     */
    public void setTopicCategories(final Collection<? extends TopicCategory> newValues) {
        topicCategories = writeCollection(newValues, topicCategories, TopicCategory.class);
    }

    /**
     * Returns the spatial and temporal extent of the resource.
     *
     * @return Spatial and temporal extent of the resource.
     *
     * @since 0.5
     */
/// @XmlElement(name = "extent")
    @UML(identifier="extent", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets the spatial and temporal extent of the resource.
     *
     * @param newValues The new extents
     *
     * @since 0.5
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns other documentation associated with the resource.
     *
     * @return Other documentation associated with the resource.
     *
     * @since 0.5
     */
/// @XmlElement(name = "additionalDocumentation")
    @UML(identifier="additionalDocumentation", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getAdditionalDocumentations() {
        return additionalDocumentations = nonNullCollection(additionalDocumentations, Citation.class);
    }

    /**
     * Sets other documentation associated with the resource.
     *
     * @param newValues The documentation to associate with the resource.
     *
     * @since 0.5
     */
    public void setAdditionalDocumentations(final Collection<? extends Citation> newValues) {
        additionalDocumentations = writeCollection(newValues, additionalDocumentations, Citation.class);
    }

    /**
     * Returns code(s) that identifies the level of processing in the producers coding system of a resource.
     *
     * @return Code(s) that identifies the level of processing in the producers coding system of a resource.
     *
     * @since 0.5
     */
/// @XmlElement(name = "processingLevel")
    @UML(identifier="processingLevel", obligation=OPTIONAL, specification=ISO_19115)
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
        checkWritePermission();
        processingLevel = newValue;
    }

    /**
     * Provides information about the frequency of resource updates, and the scope of those updates.
     *
     * @return Frequency and scope of resource updates.
     */
    @Override
    @XmlElement(name = "resourceMaintenance")
    public Collection<MaintenanceInformation> getResourceMaintenances() {
        return resourceMaintenances = nonNullCollection(resourceMaintenances, MaintenanceInformation.class);
    }

    /**
     * Sets information about the frequency of resource updates, and the scope of those updates.
     *
     * @param newValues The new resource maintenance info.
     */
    public void setResourceMaintenances(final Collection<? extends MaintenanceInformation> newValues) {
        resourceMaintenances = writeCollection(newValues, resourceMaintenances, MaintenanceInformation.class);
    }

    /**
     * Provides a graphic that illustrates the resource(s) (should include a legend for the graphic).
     *
     * @return A graphic that illustrates the resource(s).
     */
    @Override
    @XmlElement(name = "graphicOverview")
    public Collection<BrowseGraphic> getGraphicOverviews() {
        return graphicOverviews = nonNullCollection(graphicOverviews, BrowseGraphic.class);
    }

    /**
     * Sets a graphic that illustrates the resource(s).
     *
     * @param newValues The new graphics overviews.
     */
    public void setGraphicOverviews(final Collection<? extends BrowseGraphic> newValues) {
        graphicOverviews = writeCollection(newValues, graphicOverviews, BrowseGraphic.class);
    }

    /**
     * Provides a description of the format of the resource(s).
     *
     * @return Description of the format.
     */
    @Override
    @XmlElement(name = "resourceFormat")
    public Collection<Format> getResourceFormats() {
        return resourceFormats = nonNullCollection(resourceFormats, Format.class);
    }

    /**
     * Sets a description of the format of the resource(s).
     *
     * @param newValues The new resource format.
     */
    public void setResourceFormats(final Collection<? extends Format> newValues) {
        resourceFormats = writeCollection(newValues, resourceFormats, Format.class);
    }

    /**
     * Provides category keywords, their type, and reference source.
     *
     * @return Category keywords, their type, and reference source.
     */
    @Override
    @XmlElement(name = "descriptiveKeywords")
    public Collection<Keywords> getDescriptiveKeywords() {
        return descriptiveKeywords = nonNullCollection(descriptiveKeywords, Keywords.class);
    }

    /**
     * Sets category keywords, their type, and reference source.
     *
     * @param newValues The new descriptive keywords.
     */
    public void setDescriptiveKeywords(final Collection<? extends Keywords> newValues) {
        descriptiveKeywords = writeCollection(newValues, descriptiveKeywords, Keywords.class);
    }

    /**
     * Provides basic information about specific application(s) for which the resource(s)
     * has/have been or is being used by different users.
     *
     * @return Information about specific application(s) for which the resource(s)
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
     * @param newValues The new resource specific usages.
     */
    public void setResourceSpecificUsages(final Collection<? extends Usage> newValues) {
        resourceSpecificUsages = writeCollection(newValues, resourceSpecificUsages, Usage.class);
    }

    /**
     * Provides information about constraints which apply to the resource(s).
     *
     * @return Constraints which apply to the resource(s).
     */
    @Override
    @XmlElement(name = "resourceConstraints")
    public Collection<Constraints> getResourceConstraints() {
        return resourceConstraints = nonNullCollection(resourceConstraints, Constraints.class);
    }

    /**
     * Sets information about constraints which apply to the resource(s).
     *
     * @param newValues The new resource constraints.
     */
    public void setResourceConstraints(final Collection<? extends Constraints> newValues) {
        resourceConstraints = writeCollection(newValues, resourceConstraints, Constraints.class);
    }

    /**
     * Provides associated resource information.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code AssociatedResource} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Associated resource information.
     *
     * @since 0.5
     */
/// @XmlElement(name = "associatedResource")
    @UML(identifier="associatedResource", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultAssociatedResource> getAssociatedResources() {
        return associatedResources = nonNullCollection(associatedResources, DefaultAssociatedResource.class);
    }

    /**
     * Sets associated resource information.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code AssociatedResource} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new associated resources.
     *
     * @since 0.5
     */
    public void setAssociatedResources(final Collection<? extends DefaultAssociatedResource> newValues) {
        associatedResources = writeCollection(newValues, associatedResources, DefaultAssociatedResource.class);
    }

    /**
     * Provides aggregate dataset information.
     *
     * @return Aggregate dataset information.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getAssociatedResources()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "aggregationInfo")
    public Collection<AggregateInformation> getAggregationInfo() {
        return new LegacyPropertyAdapter<AggregateInformation,DefaultAssociatedResource>(getAssociatedResources()) {
            @Override protected DefaultAssociatedResource wrap(final AggregateInformation value) {
                return DefaultAssociatedResource.castOrCopy(value);
            }

            @Override protected AggregateInformation unwrap(final DefaultAssociatedResource container) {
                if (container instanceof AggregateInformation) {
                    return (AggregateInformation) container;
                } else {
                    return new DefaultAggregateInformation(container);
                }
            }

            @Override protected boolean update(final DefaultAssociatedResource container, final AggregateInformation value) {
                return container == value;
            }
        }.validOrNull();
    }

    /**
     * Sets aggregate dataset information.
     *
     * @param newValues The new aggregation info.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setAssociatedResources(Collection)}.
     */
    @Deprecated
    public void setAggregationInfo(final Collection<? extends AggregateInformation> newValues) {
        checkWritePermission();
        /*
         * We can not invoke getAggregationInfo().setValues(newValues) because this method
         * is invoked by the constructor, which is itself invoked at JAXB marshalling time,
         * in which case getAggregationInfo() may return null.
         */
        List<DefaultAssociatedResource> r = null;
        if (newValues != null) {
            r = new ArrayList<DefaultAssociatedResource>(newValues.size());
            for (final AggregateInformation value : newValues) {
                r.add(DefaultAssociatedResource.castOrCopy(value));
            }
        }
        setAssociatedResources(r);
    }
}
