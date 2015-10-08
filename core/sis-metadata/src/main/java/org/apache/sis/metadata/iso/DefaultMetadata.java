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
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.annotation.UML;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.MetadataExtensionInformation;
import org.opengis.metadata.PortrayalCatalogueReference;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;
import org.apache.sis.internal.metadata.OtherLocales;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.jaxb.code.PT_Locale;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.xml.Namespaces;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Root entity which defines metadata about a resource or resources.
 *
 * <div class="section">Localization</div>
 * When this object is marshalled as an ISO 19139 compliant XML document, the value
 * given to the {@link #setLanguage(Locale)} method will be used for the localization
 * of {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList}
 * instances of in this {@code DefaultMetadata} object and every children, as required by
 * INSPIRE rules. If no language were specified, then the default locale will be the one
 * defined in the {@link org.apache.sis.xml.XML#LOCALE} marshaller property, if any.
 *
 * <div class="section">Limitations</div>
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
     * Unique identifier for this metadata record, or {@code null} if none.
     */
    private Identifier metadataIdentifier;

    /**
     * Language(s) used for documenting metadata.
     */
    private Collection<Locale> languages;

    /**
     * Full name of the character coding standard used for the metadata set.
     */
    private Collection<Charset> characterSets;

    /**
     * Identification of the parent metadata record.
     */
    private Citation parentMetadata;

    /**
     * Scope to which the metadata applies.
     */
    private Collection<DefaultMetadataScope> metadataScopes;

    /**
     * Parties responsible for the metadata information.
     */
    private Collection<ResponsibleParty> contacts;

    /**
     * Date(s) associated with the metadata.
     */
    private Collection<CitationDate> dateInfo;

    /**
     * Citation(s) for the standard(s) to which the metadata conform.
     */
    private Collection<Citation> metadataStandards;

    /**
     * Citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     */
    private Collection<Citation> metadataProfiles;

    /**
     * Reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     */
    private Collection<Citation> alternativeMetadataReferences;

    /**
     * Online location(s) where the metadata is available.
     */
    private Collection<OnlineResource> metadataLinkages;

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
     * Information about the provenance, sources and/or the production processes applied to the resource.
     */
    private Collection<Lineage> resourceLineages;

    /**
     * Creates an initially empty metadata.
     */
    public DefaultMetadata() {
    }

    /**
     * Creates a meta data initialized to the specified values.
     *
     * @param contact   Party responsible for the metadata information.
     * @param dateStamp Date that the metadata was created.
     * @param identificationInfo Basic information about the resource to which the metadata applies.
     */
    public DefaultMetadata(final ResponsibleParty contact,
                           final Date             dateStamp,
                           final Identification   identificationInfo)
    {
        this.contacts  = singleton(contact, ResponsibleParty.class);
        this.identificationInfo = singleton(identificationInfo, Identification.class);
        if (dateStamp != null) {
            dateInfo = singleton(new DefaultCitationDate(dateStamp, DateType.CREATION), CitationDate.class);
        }
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Metadata)
     */
    public DefaultMetadata(final Metadata object) {
        super(object);
        if (object != null) {
            contacts                      = copyCollection(object.getContacts(),                      ResponsibleParty.class);
            spatialRepresentationInfo     = copyCollection(object.getSpatialRepresentationInfo(),     SpatialRepresentation.class);
            referenceSystemInfo           = copyCollection(object.getReferenceSystemInfo(),           ReferenceSystem.class);
            metadataExtensionInfo         = copyCollection(object.getMetadataExtensionInfo(),         MetadataExtensionInformation.class);
            identificationInfo            = copyCollection(object.getIdentificationInfo(),            Identification.class);
            contentInfo                   = copyCollection(object.getContentInfo(),                   ContentInformation.class);
            distributionInfo              = object.getDistributionInfo();
            dataQualityInfo               = copyCollection(object.getDataQualityInfo(),               DataQuality.class);
            portrayalCatalogueInfo        = copyCollection(object.getPortrayalCatalogueInfo(),        PortrayalCatalogueReference.class);
            metadataConstraints           = copyCollection(object.getMetadataConstraints(),           Constraints.class);
            applicationSchemaInfo         = copyCollection(object.getApplicationSchemaInfo(),         ApplicationSchemaInformation.class);
            metadataMaintenance           = object.getMetadataMaintenance();
            acquisitionInformation        = copyCollection(object.getAcquisitionInformation(),        AcquisitionInformation.class);
            if (object instanceof DefaultMetadata) {
                final DefaultMetadata c = (DefaultMetadata) object;
                metadataIdentifier            = c.getMetadataIdentifier();
                parentMetadata                = c.getParentMetadata();
                languages                     = copyCollection(c.getLanguages(),                     Locale.class);
                characterSets                 = copyCollection(c.getCharacterSets(),                 Charset.class);
                metadataScopes                = copyCollection(c.getMetadataScopes(),                DefaultMetadataScope.class);
                dateInfo                      = copyCollection(c.getDateInfo(),                      CitationDate.class);
                metadataStandards             = copyCollection(c.getMetadataStandards(),             Citation.class);
                metadataProfiles              = copyCollection(c.getMetadataProfiles(),              Citation.class);
                alternativeMetadataReferences = copyCollection(c.getAlternativeMetadataReferences(), Citation.class);
                metadataLinkages              = copyCollection(c.getMetadataLinkages(),              OnlineResource.class);
                resourceLineages              = copyCollection(c.getResourceLineages(),              Lineage.class);
            } else {
                setFileIdentifier         (object.getFileIdentifier());
                setParentIdentifier       (object.getParentIdentifier());
                setLanguage               (object.getLanguage());
                setLocales                (object.getLocales());
                setCharacterSet           (object.getCharacterSet());
                setHierarchyLevels        (object.getHierarchyLevels());
                setHierarchyLevelNames    (object.getHierarchyLevelNames());
                setDateStamp              (object.getDateStamp());
                setMetadataStandardName   (object.getMetadataStandardName());
                setMetadataStandardVersion(object.getMetadataStandardVersion());
                try {
                    setDataSetUri(object.getDataSetUri());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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

    /*
     * Note about deprecated methods implementation: as a general guideline in our metadata implementation,
     * the deprecated getter methods invoke only the non-deprecated getter replacement, and the deprecated
     * setter methods invoke only the non-deprecated setter replacement (unless the invoked methods are final).
     * This means that if a deprecated setter methods need the old value, it will read the field directly.
     * The intend is to avoid surprising code paths for user who override some methods.
     */

    /**
     * Returns a unique identifier for this metadata record.
     *
     * @return Unique identifier for this metadata record, or {@code null}.
     *
     * @since 0.5
     */
    @UML(identifier="metadataIdentifier", obligation=OPTIONAL, specification=ISO_19115)
    public Identifier getMetadataIdentifier() {
        return metadataIdentifier;
    }

    /**
     * Sets the unique identifier for this metadata record.
     *
     * @param newValue The new identifier, or {@code null} if none.
     *
     * @since 0.5
     */
    public void setMetadataIdentifier(final Identifier newValue) {
        checkWritePermission();
        metadataIdentifier = newValue;
    }

    /**
     * Returns the unique identifier for this metadata file.
     *
     * @return Unique identifier for this metadata file, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataIdentifier()}
     *   in order to include the codespace attribute.
     */
    @Override
    @Deprecated
    @XmlElement(name = "fileIdentifier")
    public String getFileIdentifier() {
        final Identifier identifier = getMetadataIdentifier();
        return (identifier != null) ? identifier.getCode() : null;
    }

    /**
     * Sets the unique identifier for this metadata file.
     *
     * @param newValue The new identifier, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataIdentifier(Identifier)}
     */
    @Deprecated
    public void setFileIdentifier(final String newValue) {
        DefaultIdentifier identifier = DefaultIdentifier.castOrCopy(metadataIdentifier); // See "Note about deprecated methods implementation"
        if (identifier == null) {
            if (newValue == null) return;
            identifier = new DefaultIdentifier();
        }
        identifier.setCode(newValue);
        if (newValue == null && (identifier instanceof AbstractMetadata) && ((AbstractMetadata) identifier).isEmpty()) {
            identifier = null;
        }
        setMetadataIdentifier(identifier);
    }

    /**
     * Returns the language(s) used for documenting metadata.
     * The first element in iteration order is the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * <p>Unless an other locale has been specified with the {@link org.apache.sis.xml.XML#LOCALE} property,
     * this {@code DefaultMetadata} instance and its children will use the first locale returned by this method
     * for marshalling {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList} instances
     * in ISO 19115-2 compliant XML documents.
     *
     * @return Language(s) used for documenting metadata.
     *
     * @since 0.5
     */
    @UML(identifier="defaultLocale+otherLocale", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<Locale> getLanguages() {
        return languages = nonNullCollection(languages, Locale.class);
    }

    /**
     * Sets the language(s) used for documenting metadata.
     * The first element in iteration order shall be the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * @param newValues The new languages.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     *
     * @since 0.5
     */
    public void setLanguages(final Collection<Locale> newValues) {
        languages = writeCollection(newValues, languages, Locale.class);
        // The "magik" applying this language to every children
        // is performed by the 'beforeMarshal(Marshaller)' method.
    }

    /**
     * Returns the default language used for documenting metadata.
     *
     * @return Language used for documenting metadata, or {@code null}.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #getLanguages()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "language")
    public Locale getLanguage() {
        return CollectionsExt.first(getLanguages());
        // No warning if the collection contains more than one locale, because
        // this is allowed by the "getLanguage() + getLocales()" contract.
    }

    /**
     * Sets the language used for documenting metadata.
     * This method modifies the collection returned by {@link #getLanguages()} as below:
     *
     * <ul>
     *   <li>If the languages collection is empty, then this method sets the collection to the given {@code newValue}.</li>
     *   <li>Otherwise the first element in the languages collection is replaced by the given {@code newValue}.</li>
     * </ul>
     *
     * @param newValue The new language.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #setLanguages(Collection)}.
     */
    @Deprecated
    public void setLanguage(final Locale newValue) {
        checkWritePermission();
        setLanguages(OtherLocales.setFirst(languages, newValue)); // See "Note about deprecated methods implementation"
    }

    /**
     * Provides information about an alternatively used localized character string for a linguistic extension.
     *
     * @return Alternatively used localized character string for a linguistic extension.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #getLanguages()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "locale")
    @XmlJavaTypeAdapter(PT_Locale.class)
    public Collection<Locale> getLocales() {
        return OtherLocales.filter(getLanguages());
    }

    /**
     * Sets information about an alternatively used localized character string for a linguistic extension.
     *
     * @param newValues The new locales.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #setLanguages(Collection)}.
     */
    @Deprecated
    public void setLocales(final Collection<? extends Locale> newValues) {
        checkWritePermission();
        setLanguages(OtherLocales.merge(CollectionsExt.first(languages), newValues)); // See "Note about deprecated methods implementation"
    }

    /**
     * Returns the character coding standard used for the metadata set.
     * Instances can be obtained by a call to {@link Charset#forName(String)}.
     *
     * <div class="note"><b>Examples:</b>
     * {@code UCS-2}, {@code UCS-4}, {@code UTF-7}, {@code UTF-8}, {@code UTF-16},
     * {@code ISO-8859-1} (a.k.a. {@code ISO-LATIN-1}), {@code ISO-8859-2}, {@code ISO-8859-3}, {@code ISO-8859-4},
     * {@code ISO-8859-5}, {@code ISO-8859-6}, {@code ISO-8859-7}, {@code ISO-8859-8}, {@code ISO-8859-9},
     * {@code ISO-8859-10}, {@code ISO-8859-11}, {@code ISO-8859-12}, {@code ISO-8859-13}, {@code ISO-8859-14},
     * {@code ISO-8859-15}, {@code ISO-8859-16},
     * {@code JIS_X0201}, {@code Shift_JIS}, {@code EUC-JP}, {@code US-ASCII}, {@code EBCDIC}, {@code EUC-KR},
     * {@code Big5}, {@code GB2312}.
     * </div>
     *
     * @return Character coding standards used for the metadata.
     *
     * @see #getLanguages()
     * @see org.opengis.metadata.identification.DataIdentification#getCharacterSets()
     * @see Charset#forName(String)
     *
     * @since 0.5
     */
    @UML(identifier="characterSet", obligation=CONDITIONAL, specification=ISO_19115) // Actually from ISO 19115:2003
    public Collection<Charset> getCharacterSets() {
        return characterSets = nonNullCollection(characterSets, Charset.class);
    }

    /**
     * Sets the character coding standard used for the metadata set.
     *
     * @param newValues The new character coding standards.
     *
     * @since 0.5
     */
    public void setCharacterSets(final Collection<Charset> newValues) {
        characterSets = writeCollection(newValues, characterSets, Charset.class);
    }

    /**
     * Returns the character coding standard used for the metadata set.
     *
     * @return Character coding standard used for the metadata, or {@code null}.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #getCharacterSets()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "characterSet")
    public CharacterSet getCharacterSet() {
        final Charset cs = LegacyPropertyAdapter.getSingleton(getCharacterSets(),
                Charset.class, null, DefaultMetadata.class, "getCharacterSet");
        if (cs == null) {
            return null;
        }
        final String name = cs.name();
        for (final CharacterSet candidate : CharacterSet.values()) {
            for (final String n : candidate.names()) {
                if (name.equals(n)) {
                    return candidate;
                }
            }
        }
        return CharacterSet.valueOf(name);
    }

    /**
     * Sets the character coding standard used for the metadata set.
     *
     * @param newValue The new character set.
     *
     * @deprecated As of GeoAPI 3.1, replaced by {@link #setCharacterSets(Collection)}.
     */
    @Deprecated
    public void setCharacterSet(final CharacterSet newValue) {
        setCharacterSets(LegacyPropertyAdapter.asCollection((newValue != null) ? newValue.toCharset() : null));
    }

    /**
     * Returns an identification of the parent metadata record.
     * This is non-null if this metadata is a subset (child) of another metadata.
     *
     * @return Identification of the parent metadata record, or {@code null} if none.
     *
     * @since 0.5
     */
    @UML(identifier="parentMetadata", obligation=CONDITIONAL, specification=ISO_19115)
    public Citation getParentMetadata() {
        return parentMetadata;
    }

    /**
     * Sets an identification of the parent metadata record.
     *
     * @param newValue The new identification of the parent metadata record.
     *
     * @since 0.5
     */
    public void setParentMetadata(final Citation newValue) {
        checkWritePermission();
        parentMetadata = newValue;
    }

    /**
     * Returns the file identifier of the metadata to which this metadata is a subset (child).
     *
     * @return Identifier of the metadata to which this metadata is a subset, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getParentMetadata()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "parentIdentifier")
    public String getParentIdentifier() {
        final Citation parentMetadata = getParentMetadata();
        if (parentMetadata != null) {
            final InternationalString title = parentMetadata.getTitle();
            if (title != null) {
                return title.toString();
            }
        }
        return null;
    }

    /**
     * Sets the file identifier of the metadata to which this metadata is a subset (child).
     *
     * @param newValue The new parent identifier.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getParentMetadata()}.
     */
    @Deprecated
    public void setParentIdentifier(final String newValue) {
        checkWritePermission();
        DefaultCitation parent = DefaultCitation.castOrCopy(parentMetadata); // See "Note about deprecated methods implementation"
        if (parent == null) {
            parent = new DefaultCitation();
        }
        parent.setTitle(new SimpleInternationalString(newValue));
        setParentMetadata(parent);
    }

    /**
     * Returns the scope or type of resource for which metadata is provided.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code MetadataScope} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Scope or type of resource for which metadata is provided.
     *
     * @since 0.5
     */
    @UML(identifier="metadataScope", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<DefaultMetadataScope> getMetadataScopes() {
        return metadataScopes = nonNullCollection(metadataScopes, DefaultMetadataScope.class);
    }

    /**
     * Sets the scope or type of resource for which metadata is provided.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code MetadataScope} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new scope or type of resource.
     *
     * @since 0.5
     */
    public void setMetadataScopes(final Collection<? extends DefaultMetadataScope> newValues) {
        metadataScopes = writeCollection(newValues, metadataScopes, DefaultMetadataScope.class);
    }

    /**
     * Returns the scope to which the metadata applies.
     *
     * @return Scope to which the metadata applies.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataScopes()}
     *   followed by {@link DefaultMetadataScope#getResourceScope()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "hierarchyLevel")
    public final Collection<ScopeCode> getHierarchyLevels() {
        return new MetadataScopeAdapter<ScopeCode>(getMetadataScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected DefaultMetadataScope wrap(final ScopeCode value) {
                return new DefaultMetadataScope(value, null);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected ScopeCode unwrap(final DefaultMetadataScope container) {
                return container.getResourceScope();
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final DefaultMetadataScope container, final ScopeCode value) {
                container.setResourceScope(value);
                return true;
            }
        }.validOrNull();
    }

    /**
     * Sets the scope to which the metadata applies.
     *
     * @param newValues The new hierarchy levels.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataScopes(Collection)}
     *   and {@link DefaultMetadataScope#setResourceScope(ScopeCode)}.
     */
    @Deprecated
    public void setHierarchyLevels(final Collection<? extends ScopeCode> newValues) {
        checkWritePermission();
        ((LegacyPropertyAdapter<ScopeCode,?>) getHierarchyLevels()).setValues(newValues);
    }

    /**
     * Returns the name of the hierarchy levels for which the metadata is provided.
     *
     * @return Hierarchy levels for which the metadata is provided.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataScopes()}
     *   followed by {@link DefaultMetadataScope#getName()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "hierarchyLevelName")
    public final Collection<String> getHierarchyLevelNames() {
        return new MetadataScopeAdapter<String>(getMetadataScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected DefaultMetadataScope wrap(final String value) {
                return new DefaultMetadataScope(null, value);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected String unwrap(final DefaultMetadataScope container) {
                final InternationalString name = container.getName();
                return (name != null) ? name.toString() : null;
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final DefaultMetadataScope container, final String value) {
                container.setName(value != null ? new SimpleInternationalString(value) : null);
                return true;
            }
        }.validOrNull();
    }

    /**
     * Sets the name of the hierarchy levels for which the metadata is provided.
     *
     * @param newValues The new hierarchy level names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataScopes(Collection)}
     *   and {@link DefaultMetadataScope#setName(InternationalString)}.
     */
    @Deprecated
    public void setHierarchyLevelNames(final Collection<? extends String> newValues) {
        checkWritePermission();
        ((LegacyPropertyAdapter<String,?>) getHierarchyLevelNames()).setValues(newValues);
    }

    /**
     * Returns the parties responsible for the metadata information.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return Parties responsible for the metadata information.
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
     * Returns the date(s) associated with the metadata.
     *
     * @return Date(s) associated with the metadata.
     *
     * @see Citation#getDates()
     *
     * @since 0.5
     */
    @UML(identifier="dateInfo", obligation=MANDATORY, specification=ISO_19115)
    public Collection<CitationDate> getDateInfo() {
        return dateInfo = nonNullCollection(dateInfo, CitationDate.class);
    }

    /**
     * Sets the date(s) associated with the metadata.
     * The collection should contains at least an element for {@link DateType#CREATION}.
     *
     * @param newValues New dates associated with the metadata.
     *
     * @since 0.5
     */
    public void setDateInfo(final Collection<? extends CitationDate> newValues) {
        dateInfo = writeCollection(newValues, dateInfo, CitationDate.class);
    }

    /**
     * Returns the date that the metadata was created.
     *
     * @return Date that the metadata was created, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getDateInfo()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "dateStamp", required = true)
    public Date getDateStamp() {
        final Collection<CitationDate> dates = getDateInfo();
        if (dates != null) {
            for (final CitationDate date : dates) {
                if (DateType.CREATION.equals(date.getDateType())) {
                    return date.getDate();
                }
            }
        }
        return null;
    }

    /**
     * Sets the date that the metadata was created.
     *
     * @param newValue The new date stamp.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setDateInfo(Collection)}.
     */
    @Deprecated
    public void setDateStamp(final Date newValue) {
        checkWritePermission();
        Collection<CitationDate> newValues = dateInfo; // See "Note about deprecated methods implementation"
        if (newValues == null) {
            if (newValue == null) {
                return;
            }
            newValues = new ArrayList<CitationDate>(1);
        } else {
            final Iterator<CitationDate> it = newValues.iterator();
            while (it.hasNext()) {
                final CitationDate date = it.next();
                if (DateType.CREATION.equals(date.getDateType())) {
                    if (newValue == null) {
                        it.remove();
                        return;
                    }
                    if (date instanceof DefaultCitationDate) {
                        ((DefaultCitationDate) date).setDate(newValue);
                        return;
                    }
                    it.remove();
                    break;
                }
            }
        }
        newValues.add(new DefaultCitationDate(newValue, DateType.CREATION));
        setDateInfo(newValues);
    }

    /**
     * Returns the citation(s) for the standard(s) to which the metadata conform.
     * The collection returned by this method typically contains elements from the
     * {@link org.apache.sis.metadata.iso.citation.Citations#ISO_19115} list.
     *
     * @return The standard(s) to which the metadata conform.
     *
     * @see #getMetadataProfiles()
     *
     * @since 0.5
     */
    @UML(identifier="metadataStandard", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getMetadataStandards() {
        return metadataStandards = nonNullCollection(metadataStandards, Citation.class);
    }

    /**
     * Sets the citation(s) for the standard(s) to which the metadata conform.
     * Metadata standard citations should include an identifier.
     *
     * @param newValues The new standard(s) to which the metadata conform.
     *
     * @since 0.5
     */
    public void setMetadataStandards(final Collection<? extends Citation> newValues) {
        metadataStandards = writeCollection(newValues, metadataStandards, Citation.class);
    }

    /**
     * Returns the citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     *
     * @return The profile(s) to which the metadata conform.
     *
     * @see #getMetadataStandards()
     * @see #getMetadataExtensionInfo()
     *
     * @since 0.5
     */
    @UML(identifier="metadataProfile", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getMetadataProfiles() {
        return metadataProfiles = nonNullCollection(metadataProfiles, Citation.class);
    }

    /**
     * Set the citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     * Metadata profile standard citations should include an identifier.
     *
     * @param newValues The new profile(s) to which the metadata conform.
     *
     * @since 0.5
     */
    public void setMetadataProfiles(final Collection<? extends Citation> newValues) {
        metadataProfiles = writeCollection(newValues, metadataProfiles, Citation.class);
    }

    /**
     * Returns reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     *
     * @return Reference(s) to alternative metadata (e.g. Dublin core, FGDC).
     *
     * @since 0.5
     */
    @UML(identifier="alternativeMetadataReference", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getAlternativeMetadataReferences() {
        return alternativeMetadataReferences = nonNullCollection(alternativeMetadataReferences, Citation.class);
    }

    /**
     * Set reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     *
     * @param newValues The new reference(s) to alternative metadata (e.g. Dublin core, FGDC).
     *
     * @since 0.5
     */
    public void setAlternativeMetadataReferences(final Collection<? extends Citation> newValues) {
        alternativeMetadataReferences = writeCollection(newValues, alternativeMetadataReferences, Citation.class);
    }

    /**
     * Implementation of legacy {@link #getMetadataStandardName()} and {@link #getMetadataStandardVersion()} methods.
     */
    private String getMetadataStandard(final boolean version) {
        final Citation standard = LegacyPropertyAdapter.getSingleton(getMetadataStandards(),
                Citation.class, null, DefaultMetadata.class, version ? "getMetadataStandardName" : "getMetadataStandardVersion");
        if (standard != null) {
            final InternationalString title = version ? standard.getEdition() : standard.getTitle();
            if (title != null) {
                return title.toString();
            }
        }
        return null;
    }

    /**
     * Implementation of legacy {@link #setMetadataStandardName(String)} and
     * {@link #setMetadataStandardVersion(String)} methods.
     */
    private void setMetadataStandard(final boolean version, final String newValue) {
        checkWritePermission();
        final InternationalString i18n = (newValue != null) ? new SimpleInternationalString(newValue) : null;
        final List<Citation> newValues = (metadataStandards != null)
                ? new ArrayList<Citation>(metadataStandards)
                : new ArrayList<Citation>(1);
        DefaultCitation citation = newValues.isEmpty() ? null : DefaultCitation.castOrCopy(newValues.get(0));
        if (citation == null) {
            citation = new DefaultCitation();
        }
        if (version) {
            citation.setEdition(i18n);
        } else {
            citation.setTitle(i18n);
        }
        if (newValues.isEmpty()) {
            newValues.add(citation);
        } else {
            newValues.set(0, citation);
        }
        setMetadataStandards(newValues);
    }

    /**
     * Returns the name of the metadata standard (including profile name) used.
     *
     * @return Name of the metadata standard used, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#getTitle()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "metadataStandardName")
    public String getMetadataStandardName() {
        return getMetadataStandard(false);
    }

    /**
     * Name of the metadata standard (including profile name) used.
     *
     * @param newValue The new metadata standard name.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#setTitle(InternationalString)}.
     */
    @Deprecated
    public void setMetadataStandardName(final String newValue) {
        setMetadataStandard(false, newValue);
    }

    /**
     * Returns the version (profile) of the metadata standard used.
     *
     * @return Version of the metadata standard used, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#getEdition()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "metadataStandardVersion")
    public String getMetadataStandardVersion() {
        return getMetadataStandard(true);
    }

    /**
     * Sets the version (profile) of the metadata standard used.
     *
     * @param newValue The new metadata standard version.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#setEdition(InternationalString)}.
     */
    @Deprecated
    public void setMetadataStandardVersion(final String newValue) {
        setMetadataStandard(true, newValue);
    }

    /**
     * Returns the online location(s) where the metadata is available.
     *
     * @return Online location(s) where the metadata is available.
     *
     * @since 0.5
     */
    @UML(identifier="metadataLinkage", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<OnlineResource> getMetadataLinkages() {
        return metadataLinkages = nonNullCollection(metadataLinkages, OnlineResource.class);
    }

    /**
     * Sets the online location(s) where the metadata is available.
     *
     * @param newValues The new online location(s).
     *
     * @since 0.5
     */
    public void setMetadataLinkages(final Collection<? extends OnlineResource> newValues) {
        metadataLinkages = writeCollection(newValues, metadataLinkages, OnlineResource.class);
    }

    /**
     * Provides the URI of the dataset to which the metadata applies.
     *
     * @return Uniformed Resource Identifier of the dataset, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getIdentificationInfo()} followed by
     *    {@link DefaultDataIdentification#getCitation()} followed by {@link DefaultCitation#getOnlineResources()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "dataSetURI")
    public String getDataSetUri() {
        String linkage = null;
        final Collection<Identification> info = getIdentificationInfo();
        if (info != null) {
            for (final Identification identification : info) {
                final Citation citation = identification.getCitation();
                if (citation instanceof DefaultCitation) {
                    final Collection<? extends OnlineResource> onlineResources = ((DefaultCitation) citation).getOnlineResources();
                    if (onlineResources != null) {
                        for (final OnlineResource link : onlineResources) {
                            final URI uri = link.getLinkage();
                            if (uri != null) {
                                if (linkage == null) {
                                    linkage = uri.toString();
                                } else {
                                    LegacyPropertyAdapter.warnIgnoredExtraneous(
                                            OnlineResource.class, DefaultMetadata.class, "getDataSetUri");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return linkage;
    }

    /**
     * Sets the URI of the dataset to which the metadata applies.
     * This method sets the linkage of the first online resource in the citation of the first identification info.
     *
     * @param  newValue The new data set URI.
     * @throws URISyntaxException If the given value can not be parsed as a URI.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getIdentificationInfo()}
     *    followed by {@link DefaultDataIdentification#getCitation()}
     *    followed by {@link DefaultCitation#setOnlineResources(Collection)}.
     */
    @Deprecated
    public void setDataSetUri(final String newValue) throws URISyntaxException {
        final URI uri = new URI(newValue);
        checkWritePermission();
        Collection<Identification> info = identificationInfo; // See "Note about deprecated methods implementation"
        AbstractIdentification firstId = AbstractIdentification.castOrCopy(CollectionsExt.first(info));
        if (firstId == null) {
            firstId = new DefaultDataIdentification();
        }
        DefaultCitation citation = DefaultCitation.castOrCopy(firstId.getCitation());
        if (citation == null) {
            citation = new DefaultCitation();
        }
        Collection<OnlineResource> onlineResources = citation.getOnlineResources();
        DefaultOnlineResource firstOnline = DefaultOnlineResource.castOrCopy(CollectionsExt.first(onlineResources));
        if (firstOnline == null) {
            firstOnline = new DefaultOnlineResource();
        }
        firstOnline.setLinkage(uri);
        onlineResources = OtherLocales.setFirst(onlineResources, firstOnline);
        citation.setOnlineResources(onlineResources);
        firstId.setCitation(citation);
        info = OtherLocales.setFirst(info, firstId);
        setIdentificationInfo(info);
    }

    /**
     * Returns the digital representation of spatial information in the dataset.
     *
     * @return Digital representation of spatial information in the dataset.
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
     *
     * @return Spatial and temporal reference systems used in the dataset.
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
     *
     * @return Metadata extensions.
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
     *
     * @return The resource(s) to which the metadata applies.
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
     * Returns information about the feature catalogue and describes the coverage and
     * image data characteristics.
     *
     * @return The feature catalogue, coverage descriptions and image data characteristics.
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
     * Returns information about the distributor of and options for obtaining the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change will tentatively be applied in GeoAPI 4.0.
     * </div>
     *
     * @return The distributor of and options for obtaining the resource(s).
     */
    @Override
    @XmlElement(name = "distributionInfo")
    public Distribution getDistributionInfo() {
        return distributionInfo;
    }

    /**
     * Sets information about the distributor of and options for obtaining the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change will tentatively be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValue The new distribution info.
     */
    public void setDistributionInfo(final Distribution newValue) {
        checkWritePermission();
        distributionInfo = newValue;
    }

    /**
     * Returns overall assessment of quality of a resource(s).
     *
     * @return Overall assessment of quality of a resource(s).
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
     * Returns information about the catalogue of rules defined for the portrayal of a resource(s).
     *
     * @return The catalogue of rules defined for the portrayal of a resource(s).
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
    public void setPortrayalCatalogueInfo(final Collection<? extends PortrayalCatalogueReference> newValues) {
        portrayalCatalogueInfo = writeCollection(newValues, portrayalCatalogueInfo, PortrayalCatalogueReference.class);
    }

    /**
     * Returns restrictions on the access and use of data.
     *
     * @return Restrictions on the access and use of data.
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
     * Returns information about the conceptual schema of a dataset.
     *
     * @return The conceptual schema of a dataset.
     */
    @Override
    @XmlElement(name = "applicationSchemaInfo")
    public Collection<ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return applicationSchemaInfo = nonNullCollection(applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Returns information about the conceptual schema of a dataset.
     *
     * @param newValues The new application schema info.
     */
    public void setApplicationSchemaInfo(final Collection<? extends ApplicationSchemaInformation> newValues) {
        applicationSchemaInfo = writeCollection(newValues, applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Returns information about the acquisition of the data.
     *
     * @return The acquisition of data.
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
     * Returns information about the frequency of metadata updates, and the scope of those updates.
     *
     * @return The frequency of metadata updates and their scope, or {@code null}.
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
     * Returns information about the provenance, sources and/or the production processes applied to the resource.
     *
     * @return Information about the provenance, sources and/or the production processes.
     *
     * @since 0.5
     */
    @UML(identifier="resourceLineage", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Lineage> getResourceLineages() {
        return resourceLineages = nonNullCollection(resourceLineages, Lineage.class);
    }

    /**
     * Sets information about the provenance, sources and/or the production processes applied to the resource.
     *
     * @param newValues New information about the provenance, sources and/or the production processes.
     *
     * @since 0.5
     */
    public void setResourceLineages(final Collection<? extends Lineage> newValues) {
        resourceLineages = writeCollection(newValues, resourceLineages, Lineage.class);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Invoked by JAXB {@link javax.xml.bind.Marshaller} before this object is marshalled to XML.
     * This method sets the locale to be used for XML marshalling to the metadata language.
     */
    private void beforeMarshal(final Marshaller marshaller) {
        Context.push(CollectionsExt.first(languages));
    }

    /**
     * Invoked by JAXB {@link javax.xml.bind.Marshaller} after this object has been marshalled to
     * XML. This method restores the locale to be used for XML marshalling to its previous value.
     */
    private void afterMarshal(final Marshaller marshaller) {
        Context.pull();
    }
}
