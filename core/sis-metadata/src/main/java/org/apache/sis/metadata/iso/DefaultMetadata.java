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

import java.util.Date;
import java.util.Locale;
import java.util.Collection;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.ApplicationSchemaInformation;
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
import org.apache.sis.internal.jaxb.code.PT_Locale;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.xml.Namespaces;

import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Root entity which defines metadata about a resource or resources.
 *
 * {@section Localization}
 * When this object is marshalled as an ISO 19139 compliant XML document, the value
 * given to the {@link #setLanguage(Locale)} method will be used for the localization
 * of {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList}
 * instances of in this {@code DefaultMetadata} object and every children, as required by
 * INSPIRE rules. If no language were specified, then the default locale will be the one
 * defined in the {@link org.apache.sis.xml.XML#LOCALE} marshaller property, if any.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Metadata_Type", propOrder = {
    "fileIdentifier",
    "language",
    "characterSet",
    "parentIdentifier",
    "hierarchyLevels",
    "hierarchyLevelNames",
    "contacts",
    "dateStamp",
    "metadataStandardName",
    "metadataStandardVersion",
    "dataSetUri",
    "locales",
    "spatialRepresentationInfo",
    "referenceSystemInfo",
    "metadataExtensionInfo",
    "identificationInfo",
    "contentInfo",
    "distributionInfo",
    "dataQualityInfo",
    "portrayalCatalogueInfo",
    "metadataConstraints",
    "applicationSchemaInfo",
    "metadataMaintenance",
    "acquisitionInformation"
})
@XmlRootElement(name = "MD_Metadata")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_Metadata.class)
public class DefaultMetadata extends ISOMetadata implements Metadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7337533776231004504L;

    /**
     * Unique identifier for this metadata file, or {@code null} if none.
     */
    private String fileIdentifier;

    /**
     * Language used for documenting metadata.
     */
    private Locale language;

    /**
     * Information about an alternatively used localized character
     * strings for linguistic extensions.
     */
    private Collection<Locale> locales;

    /**
     * Full name of the character coding standard used for the metadata set.
     */
    private CharacterSet characterSet;

    /**
     * File identifier of the metadata to which this metadata is a subset (child).
     */
    private String parentIdentifier;

    /**
     * Scope to which the metadata applies.
     */
    private Collection<ScopeCode> hierarchyLevels;

    /**
     * Name of the hierarchy levels for which the metadata is provided.
     */
    private Collection<String> hierarchyLevelNames;

    /**
     * Parties responsible for the metadata information.
     */
    private Collection<ResponsibleParty> contacts;

    /**
     * Date that the metadata was created, in milliseconds elapsed since January 1st, 1970.
     * If not defined, then then value is {@link Long#MIN_VALUE}.
     */
    private long dateStamp;

    /**
     * Name of the metadata standard (including profile name) used.
     */
    private String metadataStandardName;

    /**
     * Version (profile) of the metadata standard used.
     */
    private String metadataStandardVersion;

    /**
     * Uniformed Resource Identifier (URI) of the dataset to which the metadata applies.
     */
    private String dataSetUri;

    /**
     * Digital representation of spatial information in the dataset.
     */
    private Collection<SpatialRepresentation> spatialRepresentationInfo;

    /**
     * Description of the spatial and temporal reference systems used in the dataset.
     */
    private Collection<ReferenceSystem> referenceSystemInfo;

    /**
     * Information describing metadata extensions.
     */
    private Collection<MetadataExtensionInformation> metadataExtensionInfo;

    /**
     * Basic information about the resource(s) to which the metadata applies.
     */
    private Collection<Identification> identificationInfo;

    /**
     * Provides information about the feature catalogue and describes the coverage and
     * image data characteristics.
     */
    private Collection<ContentInformation> contentInfo;

    /**
     * Provides information about the distributor of and options for obtaining the resource(s).
     */
    private Distribution distributionInfo;

    /**
     * Provides overall assessment of quality of a resource(s).
     */
    private Collection<DataQuality> dataQualityInfo;

    /**
     * Provides information about the catalogue of rules defined for the portrayal of a resource(s).
     */
    private Collection<PortrayalCatalogueReference> portrayalCatalogueInfo;

    /**
     * Provides restrictions on the access and use of data.
     */
    private Collection<Constraints> metadataConstraints;

    /**
     * Provides information about the conceptual schema of a dataset.
     */
    private Collection<ApplicationSchemaInformation> applicationSchemaInfo;

    /**
     * Provides information about the frequency of metadata updates, and the scope of those updates.
     */
    private MaintenanceInformation metadataMaintenance;

    /**
     * Provides information about the acquisition of the data.
     */
    private Collection<AcquisitionInformation> acquisitionInformation;

    /**
     * Creates an initially empty metadata.
     */
    public DefaultMetadata() {
        dateStamp = Long.MIN_VALUE;
    }

    /**
     * Creates a meta data initialized to the specified values.
     *
     * @param contact   Party responsible for the metadata information.
     * @param dateStamp Date that the metadata was created.
     * @param identificationInfo Basic information about the resource
     *        to which the metadata applies.
     */
    public DefaultMetadata(final ResponsibleParty contact,
                           final Date             dateStamp,
                           final Identification   identificationInfo)
    {
        this.contacts  = singleton(contact, ResponsibleParty.class);
        this.dateStamp = toMilliseconds(dateStamp);
        this.identificationInfo = singleton(identificationInfo, Identification.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Metadata)
     */
    public DefaultMetadata(final Metadata object) {
        super(object);
        fileIdentifier            = object.getFileIdentifier();
        language                  = object.getLanguage();
        characterSet              = object.getCharacterSet();
        parentIdentifier          = object.getParentIdentifier();
        hierarchyLevels           = copyCollection(object.getHierarchyLevels(), ScopeCode.class);
        hierarchyLevelNames       = copyCollection(object.getHierarchyLevelNames(), String.class);
        contacts                  = copyCollection(object.getContacts(), ResponsibleParty.class);
        dateStamp                 = toMilliseconds(object.getDateStamp());
        metadataStandardName      = object.getMetadataStandardName();
        metadataStandardVersion   = object.getMetadataStandardVersion();
        dataSetUri                = object.getDataSetUri();
        locales                   = copyCollection(object.getLocales(), Locale.class);
        spatialRepresentationInfo = copyCollection(object.getSpatialRepresentationInfo(), SpatialRepresentation.class);
        referenceSystemInfo       = copyCollection(object.getReferenceSystemInfo(), ReferenceSystem.class);
        metadataExtensionInfo     = copyCollection(object.getMetadataExtensionInfo(), MetadataExtensionInformation.class);
        identificationInfo        = copyCollection(object.getIdentificationInfo(), Identification.class);
        contentInfo               = copyCollection(object.getContentInfo(), ContentInformation.class);
        distributionInfo          = object.getDistributionInfo();
        dataQualityInfo           = copyCollection(object.getDataQualityInfo(), DataQuality.class);
        portrayalCatalogueInfo    = copyCollection(object.getPortrayalCatalogueInfo(), PortrayalCatalogueReference.class);
        metadataConstraints       = copyCollection(object.getMetadataConstraints(), Constraints.class);
        applicationSchemaInfo     = copyCollection(object.getApplicationSchemaInfo(), ApplicationSchemaInformation.class);
        metadataMaintenance       = object.getMetadataMaintenance();
        acquisitionInformation    = copyCollection(object.getAcquisitionInformation(), AcquisitionInformation.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultMetadata}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMetadata} instance is created using the
     *       {@linkplain #DefaultMetadata(Metadata) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMetadata castOrCopy(final Metadata object) {
        if (object == null || object instanceof DefaultMetadata) {
            return (DefaultMetadata) object;
        }
        return new DefaultMetadata(object);
    }

    /**
     * Returns the unique identifier for this metadata file, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "fileIdentifier")
    public String getFileIdentifier() {
        return fileIdentifier;
    }

    /**
     * Sets the unique identifier for this metadata file, or {@code null} if none.
     *
     * @param newValue The new identifier.
     */
    public void setFileIdentifier(final String newValue) {
        checkWritePermission();
        fileIdentifier = newValue;
    }

    /**
     * Returns the language used for documenting metadata. This {@code DefaultMetadata} object and
     * its children will use that locale for marshalling {@link org.opengis.util.InternationalString}
     * and {@link org.opengis.util.CodeList} instances in ISO 19139 compliant XML documents.
     */
    @Override
    @XmlElement(name = "language")
    public Locale getLanguage() {
        return language;
    }

    /**
     * Sets the language used for documenting metadata. This {@code DefaultMetadata} object and its
     * children will use the given locale for marshalling {@link org.opengis.util.InternationalString}
     * and {@link org.opengis.util.CodeList} instances in ISO 19139 compliant XML documents.
     *
     * @param newValue The new language.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     */
    public void setLanguage(final Locale newValue) {
        checkWritePermission();
        language = newValue;
        // The "magik" applying this language to every children
        // is performed by the 'beforeMarshal(Marshaller)' method.
    }

    /**
     * Provides information about an alternatively used localized character
     * string for a linguistic extension.
     */
    @Override
    @XmlElement(name = "locale")
    @XmlJavaTypeAdapter(PT_Locale.class)
    public Collection<Locale> getLocales() {
        return locales = nonNullCollection(locales, Locale.class);
    }

    /**
     * Sets information about an alternatively used localized character
     * string for a linguistic extension.
     *
     * @param newValues The new locales.
     */
    public void setLocales(final Collection<? extends Locale> newValues) {
        locales = writeCollection(newValues, locales, Locale.class);
    }

    /**
     * Returns the full name of the character coding standard used for the metadata set.
     */
    @Override
    @XmlElement(name = "characterSet")
    public CharacterSet getCharacterSet()  {
        return characterSet;
    }

    /**
     * Sets the full name of the character coding standard used for the metadata set.
     *
     * @param newValue The new character set.
     */
    public void setCharacterSet(final CharacterSet newValue) {
        checkWritePermission();
        characterSet = newValue;
    }

    /**
     * Returns the file identifier of the metadata to which this metadata is a subset (child).
     */
    @Override
    @XmlElement(name = "parentIdentifier")
    public String getParentIdentifier() {
        return parentIdentifier;
    }

    /**
     * Sets the file identifier of the metadata to which this metadata is a subset (child).
     *
     * @param newValue The new parent identifier.
     */
    public void setParentIdentifier(final String newValue) {
        checkWritePermission();
        parentIdentifier = newValue;
    }

    /**
     * Returns the scope to which the metadata applies.
     */
    @Override
    @XmlElement(name = "hierarchyLevel")
    public Collection<ScopeCode> getHierarchyLevels() {
        return hierarchyLevels = nonNullCollection(hierarchyLevels, ScopeCode.class);
    }

    /**
     * Sets the scope to which the metadata applies.
     *
     * @param newValues The new hierarchy levels.
     */
    public void setHierarchyLevels(final Collection<? extends ScopeCode> newValues) {
        hierarchyLevels = writeCollection(newValues, hierarchyLevels, ScopeCode.class);
    }

    /**
     * Returns the name of the hierarchy levels for which the metadata is provided.
     */
    @Override
    @XmlElement(name = "hierarchyLevelName")
    public Collection<String> getHierarchyLevelNames() {
        return hierarchyLevelNames = nonNullCollection(hierarchyLevelNames, String.class);
    }

    /**
     * Sets the name of the hierarchy levels for which the metadata is provided.
     *
     * @param newValues The new hierarchy level names.
     */
    public void setHierarchyLevelNames(final Collection<? extends String> newValues) {
        hierarchyLevelNames = writeCollection(newValues, hierarchyLevelNames, String.class);
    }

    /**
     * Returns the parties responsible for the metadata information.
     */
    @Override
    @XmlElement(name = "contact", required = true)
    public Collection<ResponsibleParty> getContacts() {
        return contacts = nonNullCollection(contacts, ResponsibleParty.class);
    }

    /**
     * Sets the parties responsible for the metadata information.
     *
     * @param newValues The new contacts.
     */
    public void setContacts(final Collection<? extends ResponsibleParty> newValues) {
        checkWritePermission();
        contacts = writeCollection(newValues, contacts, ResponsibleParty.class);
    }

    /**
     * Returns the date that the metadata was created.
     */
    @Override
    @XmlElement(name = "dateStamp", required = true)
    public Date getDateStamp() {
        return toDate(dateStamp);
    }

    /**
     * Sets the date that the metadata was created.
     *
     * @param newValue The new date stamp.
     */
    public void setDateStamp(final Date newValue) {
        checkWritePermission();
        dateStamp = toMilliseconds(newValue);
    }

    /**
     * Returns the name of the metadata standard (including profile name) used.
     */
    @Override
    @XmlElement(name = "metadataStandardName")
    public String getMetadataStandardName() {
        return metadataStandardName;
    }

    /**
     * Name of the metadata standard (including profile name) used.
     *
     * @param newValue The new metadata standard name.
     */
    public void setMetadataStandardName(final String newValue) {
        checkWritePermission();
        metadataStandardName = newValue;
    }

    /**
     * Returns the version (profile) of the metadata standard used.
     */
    @Override
    @XmlElement(name = "metadataStandardVersion")
    public String getMetadataStandardVersion() {
        return metadataStandardVersion;
    }

    /**
     * Sets the version (profile) of the metadata standard used.
     *
     * @param newValue The new metadata standard version.
     */
    public void setMetadataStandardVersion(final String newValue) {
        checkWritePermission();
        metadataStandardVersion = newValue;
    }

    /**
     * Provides the URI of the dataset to which the metadata applies.
     */
    @Override
    @XmlElement(name = "dataSetURI")
    public String getDataSetUri() {
        return dataSetUri;
    }

    /**
     * Sets the URI of the dataset to which the metadata applies.
     *
     * @param newValue The new data set URI.
     */
    public void setDataSetUri(final String newValue) {
        checkWritePermission();
        dataSetUri = newValue;
    }

    /**
     * Returns the digital representation of spatial information in the dataset.
     */
    @Override
    @XmlElement(name = "spatialRepresentationInfo")
    public Collection<SpatialRepresentation> getSpatialRepresentationInfo() {
        return spatialRepresentationInfo = nonNullCollection(spatialRepresentationInfo, SpatialRepresentation.class);
    }

    /**
     * Sets the digital representation of spatial information in the dataset.
     *
     * @param newValues The new spatial representation info.
     */
    public void setSpatialRepresentationInfo(final Collection<? extends SpatialRepresentation> newValues) {
        spatialRepresentationInfo = writeCollection(newValues, spatialRepresentationInfo, SpatialRepresentation.class);
    }

    /**
     * Returns the description of the spatial and temporal reference systems used in the dataset.
     */
    @Override
    @XmlElement(name = "referenceSystemInfo")
    public Collection<ReferenceSystem> getReferenceSystemInfo() {
        return referenceSystemInfo = nonNullCollection(referenceSystemInfo, ReferenceSystem.class);
    }

    /**
     * Sets the description of the spatial and temporal reference systems used in the dataset.
     *
     * @param newValues The new reference system info.
     */
    public void setReferenceSystemInfo(final Collection<? extends ReferenceSystem> newValues) {
        referenceSystemInfo = writeCollection(newValues, referenceSystemInfo, ReferenceSystem.class);
    }

    /**
     * Returns information describing metadata extensions.
     */
    @Override
    @XmlElement(name = "metadataExtensionInfo")
    public Collection<MetadataExtensionInformation> getMetadataExtensionInfo() {
        return metadataExtensionInfo = nonNullCollection(metadataExtensionInfo, MetadataExtensionInformation.class);
    }

    /**
     * Sets information describing metadata extensions.
     *
     * @param newValues The new metadata extension info.
     */
    public void setMetadataExtensionInfo(final Collection<? extends MetadataExtensionInformation> newValues) {
        metadataExtensionInfo = writeCollection(newValues, metadataExtensionInfo, MetadataExtensionInformation.class);
    }

    /**
     * Returns basic information about the resource(s) to which the metadata applies.
     */
    @Override
    @XmlElement(name = "identificationInfo", required = true)
    public Collection<Identification> getIdentificationInfo() {
        return identificationInfo = nonNullCollection(identificationInfo, Identification.class);
    }

    /**
     * Sets basic information about the resource(s) to which the metadata applies.
     *
     * @param newValues The new identification info.
     */
    public void setIdentificationInfo(final Collection<? extends Identification> newValues) {
        identificationInfo = writeCollection(newValues, identificationInfo, Identification.class);
    }

    /**
     * Provides information about the feature catalogue and describes the coverage and
     * image data characteristics.
     */
    @Override
    @XmlElement(name = "contentInfo")
    public Collection<ContentInformation> getContentInfo() {
        return contentInfo = nonNullCollection(contentInfo, ContentInformation.class);
    }

    /**
     * Sets information about the feature catalogue and describes the coverage and
     * image data characteristics.
     *
     * @param newValues The new content info.
     */
    public void setContentInfo(final Collection<? extends ContentInformation> newValues) {
        contentInfo = writeCollection(newValues, contentInfo, ContentInformation.class);
    }

    /**
     * Provides information about the distributor of and options for obtaining the resource(s).
     */
    @Override
    @XmlElement(name = "distributionInfo")
    public Distribution getDistributionInfo() {
        return distributionInfo;
    }

    /**
     * Provides information about the distributor of and options for obtaining the resource(s).
     *
     * @param newValue The new distribution info.
     */
    public void setDistributionInfo(final Distribution newValue) {
        checkWritePermission();
        distributionInfo = newValue;
    }

    /**
     * Provides overall assessment of quality of a resource(s).
     */
    @Override
    @XmlElement(name = "dataQualityInfo")
    public Collection<DataQuality> getDataQualityInfo() {
        return dataQualityInfo = nonNullCollection(dataQualityInfo, DataQuality.class);
    }

    /**
     * Sets overall assessment of quality of a resource(s).
     *
     * @param newValues The new data quality info.
     */
    public void setDataQualityInfo(final Collection<? extends DataQuality> newValues) {
        dataQualityInfo = writeCollection(newValues, dataQualityInfo, DataQuality.class);
    }

    /**
     * Provides information about the catalogue of rules defined for the portrayal of a
     * resource(s).
     */
    @Override
    @XmlElement(name = "portrayalCatalogueInfo")
    public Collection<PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return portrayalCatalogueInfo = nonNullCollection(portrayalCatalogueInfo, PortrayalCatalogueReference.class);
    }

    /**
     * Sets information about the catalogue of rules defined for the portrayal of a resource(s).
     *
     * @param newValues The new portrayal catalog info.
     */
    public void setPortrayalCatalogueInfo(
            final Collection<? extends PortrayalCatalogueReference> newValues)
    {
        portrayalCatalogueInfo = writeCollection(newValues, portrayalCatalogueInfo, PortrayalCatalogueReference.class);
    }

    /**
     * Provides restrictions on the access and use of data.
     */
    @Override
    @XmlElement(name = "metadataConstraints")
    public Collection<Constraints> getMetadataConstraints() {
        return metadataConstraints = nonNullCollection(metadataConstraints, Constraints.class);
    }

    /**
     * Sets restrictions on the access and use of data.
     *
     * @param newValues The new metadata constraints.
     */
    public void setMetadataConstraints(final Collection<? extends Constraints> newValues) {
        metadataConstraints = writeCollection(newValues, metadataConstraints, Constraints.class);
    }

    /**
     * Provides information about the conceptual schema of a dataset.
     */
    @Override
    @XmlElement(name = "applicationSchemaInfo")
    public Collection<ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return applicationSchemaInfo = nonNullCollection(applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Provides information about the conceptual schema of a dataset.
     *
     * @param newValues The new application schema info.
     */
    public void setApplicationSchemaInfo(final Collection<? extends ApplicationSchemaInformation> newValues) {
        applicationSchemaInfo = writeCollection(newValues, applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Provides information about the frequency of metadata updates, and the scope of those updates.
     */
    @Override
    @XmlElement(name = "metadataMaintenance")
    public MaintenanceInformation getMetadataMaintenance() {
        return metadataMaintenance;
    }

    /**
     * Sets information about the frequency of metadata updates, and the scope of those updates.
     *
     * @param newValue The new metadata maintenance.
     */
    public void setMetadataMaintenance(final MaintenanceInformation newValue) {
        checkWritePermission();
        metadataMaintenance = newValue;
    }

    /**
     * Provides information about the acquisition of the data.
     */
    @Override
    @XmlElement(name = "acquisitionInformation", namespace = Namespaces.GMI)
    public Collection<AcquisitionInformation> getAcquisitionInformation() {
        return acquisitionInformation = nonNullCollection(acquisitionInformation, AcquisitionInformation.class);
    }

    /**
     * Sets information about the acquisition of the data.
     *
     * @param newValues The new acquisition information.
     */
    public void setAcquisitionInformation(final Collection<? extends AcquisitionInformation> newValues) {
        acquisitionInformation = writeCollection(newValues, acquisitionInformation, AcquisitionInformation.class);
    }

    /**
     * Invoked by JAXB {@link javax.xml.bind.Marshaller} before this object is marshalled to XML.
     * This method sets the locale to be used for XML marshalling to the metadata language.
     */
    private void beforeMarshal(final Marshaller marshaller) {
        Context.push(language);
    }

    /**
     * Invoked by JAXB {@link javax.xml.bind.Marshaller} after this object has been marshalled to
     * XML. This method restores the locale to be used for XML marshalling to its previous value.
     */
    private void afterMarshal(final Marshaller marshaller) {
        Context.pull();
    }
}
