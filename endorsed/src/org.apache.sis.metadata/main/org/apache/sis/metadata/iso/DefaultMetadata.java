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
import java.util.Set;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.temporal.Temporal;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.lan.LocaleAndCharset;
import org.apache.sis.xml.bind.lan.LocaleAdapter;
import org.apache.sis.xml.bind.lan.OtherLocales;
import org.apache.sis.xml.bind.lan.PT_Locale;
import org.apache.sis.xml.bind.metadata.CI_Citation;
import org.apache.sis.xml.bind.metadata.MD_Identifier;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.converter.SurjectiveConverter;
import org.apache.sis.math.FunctionProperty;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.citation.ResponsibleParty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.MetadataScope;


/**
 * Root entity which defines metadata about a resource or resources.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Metadata}
 * {@code   ├─language…………………………………………………} Language used for documenting metadata.
 * {@code   ├─characterSet………………………………………} Full name of the character coding standard used for the metadata set.
 * {@code   ├─contact……………………………………………………} Parties responsible for the metadata information.
 * {@code   │   ├─party………………………………………………} Information about the parties.
 * {@code   │   │   └─name………………………………………} Name of the party.
 * {@code   │   └─role…………………………………………………} Function performed by the responsible party.
 * {@code   ├─identificationInfo………………………} Basic information about the resource(s) to which the metadata applies.
 * {@code   │   ├─citation………………………………………} Citation data for the resource(s).
 * {@code   │   │   ├─title……………………………………} Name by which the cited resource is known.
 * {@code   │   │   └─date………………………………………} Reference date for the cited resource.
 * {@code   │   ├─abstract………………………………………} Brief narrative summary of the content of the resource(s).
 * {@code   │   ├─extent……………………………………………} Bounding polygon, vertical, and temporal extent of the dataset.
 * {@code   │   │   ├─description……………………} The spatial and temporal extent for the referring object.
 * {@code   │   │   ├─geographicElement……} Geographic component of the extent of the referring object.
 * {@code   │   │   ├─temporalElement…………} Temporal component of the extent of the referring object.
 * {@code   │   │   └─verticalElement…………} Vertical component of the extent of the referring object.
 * {@code   │   └─topicCategory…………………………} Main theme(s) of the dataset.
 * {@code   ├─dateInfo…………………………………………………} Date(s) associated with the metadata.
 * {@code   ├─metadataScope……………………………………} The scope or type of resource for which metadata is provided.
 * {@code   │   └─resourceScope…………………………} Resource scope
 * {@code   └─parentMetadata…………………………………} Identification of the parent metadata record.
 * {@code       ├─title………………………………………………} Name by which the cited resource is known.
 * {@code       └─date…………………………………………………} Reference date for the cited resource.</div>
 *
 * <h2>Localization</h2>
 * When this object is marshalled as an ISO 19139 compliant XML document, the value
 * given to the {@link #setLanguage(Locale)} method will be used for the localization
 * of {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList}
 * instances of in this {@code DefaultMetadata} object and every children, as required by
 * INSPIRE rules. If no language were specified, then the default locale will be the one
 * defined in the {@link org.apache.sis.xml.XML#LOCALE} marshaller property, if any.
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
 * @version 1.6
 *
 * @see org.apache.sis.storage.Resource#getMetadata()
 *
 * @since 0.3
 */
@XmlType(name = "MD_Metadata_Type", propOrder = {
    // Attributes new in ISO 19115:2014
    "metadataIdentifier",
    "defaultLocale",
    "parentMetadata",

    // Legacy ISO 19115:2003 attributes
    "fileIdentifier",
    "language",
    "charset",
    "parentIdentifier",
    "hierarchyLevels",
    "hierarchyLevelNames",

    // Common to both versions
    "contacts",

    // Attributes new in ISO 19115:2014
    "dates",                            // actually "dateInfo"
    "metadataStandard",
    "metadataProfile",
    "alternativeMetadataReference",
    "otherLocales",
    "metadataLinkage",

    // Legacy ISO 19115:2003 attributes
    "dateStamp",
    "metadataStandardName",
    "metadataStandardVersion",
    "dataSetUri",
    "locales",

    // Common to both metadata models
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
    "resourceLineage",

    // Attributes new in ISO 19115:2014
    "metadataScope",

    // GMI extension
    "acquisitionInformation"
})
@XmlRootElement(name = "MD_Metadata")
@XmlSeeAlso(org.apache.sis.xml.bind.gmi.MI_Metadata.class)
public class DefaultMetadata extends ISOMetadata implements Metadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -76483485174667242L;

    /**
     * Language(s) and character set(s) used within the dataset.
     */
    @SuppressWarnings("serial")
    private Map<Locale,Charset> locales;

    /**
     * Identification of the parent metadata record.
     */
    @SuppressWarnings("serial")
    private Citation parentMetadata;

    /**
     * Scope to which the metadata applies.
     */
    @SuppressWarnings("serial")
    private Collection<MetadataScope> metadataScopes;

    /**
     * Parties responsible for the metadata information.
     */
    @SuppressWarnings("serial")
    private Collection<ResponsibleParty> contacts;

    /**
     * Date(s) associated with the metadata.
     */
    @SuppressWarnings("serial")
    private Collection<CitationDate> dateInfo;

    /**
     * Citation(s) for the standard(s) to which the metadata conform.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> metadataStandards;

    /**
     * Citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> metadataProfiles;

    /**
     * Reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> alternativeMetadataReferences;

    /**
     * Online location(s) where the metadata is available.
     */
    @SuppressWarnings("serial")
    private Collection<OnlineResource> metadataLinkages;

    /**
     * Digital representation of spatial information in the dataset.
     */
    @SuppressWarnings("serial")
    private Collection<SpatialRepresentation> spatialRepresentationInfo;

    /**
     * Description of the spatial and temporal reference systems used in the dataset.
     */
    @SuppressWarnings("serial")
    private Collection<ReferenceSystem> referenceSystemInfo;

    /**
     * Information describing metadata extensions.
     */
    @SuppressWarnings("serial")
    private Collection<MetadataExtensionInformation> metadataExtensionInfo;

    /**
     * Basic information about the resource(s) to which the metadata applies.
     */
    @SuppressWarnings("serial")
    private Collection<Identification> identificationInfo;

    /**
     * Provides information about the feature catalogue and describes the coverage and
     * image data characteristics.
     */
    @SuppressWarnings("serial")
    private Collection<ContentInformation> contentInfo;

    /**
     * Provides information about the distributor of and options for obtaining the resource(s).
     */
    @SuppressWarnings("serial")
    private Distribution distributionInfo;

    /**
     * Provides overall assessment of quality of a resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<DataQuality> dataQualityInfo;

    /**
     * Provides information about the catalogue of rules defined for the portrayal of a resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<PortrayalCatalogueReference> portrayalCatalogueInfo;

    /**
     * Provides restrictions on the access and use of metadata.
     */
    @SuppressWarnings("serial")
    private Collection<Constraints> metadataConstraints;

    /**
     * Provides information about the conceptual schema of a dataset.
     */
    @SuppressWarnings("serial")
    private Collection<ApplicationSchemaInformation> applicationSchemaInfo;

    /**
     * Provides information about the frequency of metadata updates, and the scope of those updates.
     */
    @SuppressWarnings("serial")
    private MaintenanceInformation metadataMaintenance;

    /**
     * Provides information about the acquisition of the data.
     */
    @SuppressWarnings("serial")
    private Collection<AcquisitionInformation> acquisitionInformation;

    /**
     * Information about the provenance, sources and/or the production processes applied to the resource.
     */
    @SuppressWarnings("serial")
    private Collection<Lineage> resourceLineages;

    /**
     * Creates an initially empty metadata.
     */
    public DefaultMetadata() {
    }

    /**
     * Creates a meta data initialized to the specified values.
     *
     * @param contact             party responsible for the metadata information.
     * @param dateStamp           date that the metadata was created.
     * @param identificationInfo  basic information about the resource to which the metadata applies.
     *
     * @since 1.5
     */
    public DefaultMetadata(final ResponsibleParty contact,
                           final Temporal       dateStamp,
                           final Identification identificationInfo)
    {
        this.contacts  = singleton(contact, ResponsibleParty.class);
        this.identificationInfo = singleton(identificationInfo, Identification.class);
        if (dateStamp != null) {
            dateInfo = singleton(new DefaultCitationDate(dateStamp, DateType.CREATION), CitationDate.class);
        }
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Metadata)
     */
    public DefaultMetadata(final Metadata object) {
        super(object);
        if (object != null) {
            identifiers                   = singleton(object.getMetadataIdentifier(), Identifier.class);
            parentMetadata                = object.getParentMetadata();
            locales                       = copyMap       (object.getLocalesAndCharsets(),            Locale.class);
            metadataScopes                = copyCollection(object.getMetadataScopes(),                MetadataScope.class);
            contacts                      = copyCollection(object.getContacts(),                      ResponsibleParty.class);
            dateInfo                      = copyCollection(object.getDateInfo(),                      CitationDate.class);
            metadataStandards             = copyCollection(object.getMetadataStandards(),             Citation.class);
            metadataProfiles              = copyCollection(object.getMetadataProfiles(),              Citation.class);
            alternativeMetadataReferences = copyCollection(object.getAlternativeMetadataReferences(), Citation.class);
            metadataLinkages              = copyCollection(object.getMetadataLinkages(),              OnlineResource.class);
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
            resourceLineages              = copyCollection(object.getResourceLineages(),              Lineage.class);
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
     *       {@linkplain #DefaultMetadata(Metadata) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * <h4>Use case</h4>
     * This method is useful before {@linkplain org.apache.sis.xml.XML#marshal(Object) XML marshalling}
     * or serialization, which may not be supported by all implementations.
     * However, the returned metadata is not guaranteed to be {@linkplain State#EDITABLE editable}.
     * For editable metadata, see {@link #deepCopy(Metadata)}.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMetadata castOrCopy(final Metadata object) {
        if (object == null || object instanceof DefaultMetadata) {
            return (DefaultMetadata) object;
        }
        return new DefaultMetadata(object);
    }

    /**
     * Returns an editable copy of the given metadata. All children are also copied.
     * This method is more expensive than {@link #castOrCopy(Metadata)} because the
     * copy is unconditional and much deeper.
     * However, the result is guaranteed to be editable.
     *
     * <h4>Use case</h4>
     * Metadata returned by {@link org.apache.sis.storage.Resource#getMetadata()} are typically unmodifiable.
     * This {@code deepCopy(…)} method is useful for completing those metadata with new elements, for example
     * before insertion in a catalog.
     *
     * @param  object  the metadata to copy, or {@code null} if none.
     * @return a deep copy of the given object, or {@code null} if the argument was null.
     *
     * @see #deepCopy(State)
     * @see State#EDITABLE
     *
     * @since 1.1
     */
    public static DefaultMetadata deepCopy(final Metadata object) {
        if (object == null) {
            return null;
        }
        return (DefaultMetadata) new MetadataCopier(MetadataStandard.ISO_19115).copy(Metadata.class, object);
    }

    /*
     * Note about deprecated methods implementation: as a general guideline in our metadata implementation,
     * the deprecated getter methods invoke only the non-deprecated getter replacement, and the deprecated
     * setter methods invoke only the non-deprecated setter replacement (unless the invoked methods are final).
     * This means that if a deprecated setter methods need the old value, it will read the field directly.
     * The intent is to avoid surprising code paths for user who override some methods.
     */

    /**
     * Returns a unique identifier for this metadata record.
     *
     * <h4>Standard usage</h4>
     * OGC 07-045 (Catalog Service Specification — ISO metadata application profile) recommends usage
     * of a UUID (Universal Unique Identifier) as specified by <a href="http://www.ietf.org">IETF</a>
     * to ensure identifier’s uniqueness.
     *
     * @return unique identifier for this metadata record, or {@code null}.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "metadataIdentifier")
    @XmlJavaTypeAdapter(MD_Identifier.Since2014.class)
    public Identifier getMetadataIdentifier() {
        return super.getIdentifier();
    }

    /**
     * Sets the unique identifier for this metadata record.
     *
     * @param  newValue  the new identifier, or {@code null} if none.
     *
     * @since 0.5
     */
    public void setMetadataIdentifier(final Identifier newValue) {
        super.setIdentifier(newValue);
    }

    /**
     * Returns the unique identifier for this metadata file.
     *
     * @return unique identifier for this metadata file, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataIdentifier()}
     *   in order to include the codespace attribute.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMetadataIdentifier")
    @XmlElement(name = "fileIdentifier", namespace = LegacyNamespaces.GMD)
    public String getFileIdentifier() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Identifier identifier = getMetadataIdentifier();
            if (identifier != null) return identifier.getCode();
        }
        return null;
    }

    /**
     * Sets the unique identifier for this metadata file.
     *
     * @param  newValue  the new identifier, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataIdentifier(Identifier)}
     */
    @Deprecated(since="1.0")
    public void setFileIdentifier(final String newValue) {
        // See "Note about deprecated methods implementation"
        DefaultIdentifier identifier = DefaultIdentifier.castOrCopy(super.getIdentifier());
        if (identifier == null) {
            if (newValue == null) return;
            identifier = new DefaultIdentifier();
        }
        identifier.setCode(newValue);
        if (newValue == null && (identifier instanceof Emptiable) && ((Emptiable) identifier).isEmpty()) {
            identifier = null;
        }
        setMetadataIdentifier(identifier);
    }

    /**
     * Returns the language(s) and character set(s) used for documenting metadata.
     * The first entry in iteration order is the default language and its character set.
     * All other entries, if any, are alternate language(s) and character set(s) used within the resource.
     *
     * <p>Unless another locale has been specified with the {@link org.apache.sis.xml.XML#LOCALE} property,
     * this {@code DefaultMetadata} instance and its children will use the first locale returned by this method
     * for marshalling {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList} instances
     * in ISO 19115-2 compliant XML documents.</p>
     *
     * <p>Each ({@link Locale}, {@link Charset}) entry is equivalent to an instance of ISO 19115 {@code PT_Locale}
     * class. The language code and the character set are mandatory elements in ISO standard. Consequently, this map
     * should not contain null key or null values, but Apache SIS implementations is tolerant for historical reasons.
     * The same character set may be associated to many languages.</p>
     *
     * @return language(s) and character set(s) used for documenting metadata.
     *
     * @since 1.0
     */
    @Override
    // @XmlElement at the end of this class.
    public Map<Locale,Charset> getLocalesAndCharsets() {
        return locales = nonNullMap(locales, Locale.class);
    }

    /**
     * Sets the language(s) and character set(s) used within the dataset.
     * The first element in iteration order should be the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * @param  newValues  the new language(s) and character set(s) used for documenting metadata.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     *
     * @since 1.0
     */
    public void setLocalesAndCharsets(final Map<? extends Locale, ? extends Charset> newValues) {
        locales = writeMap(newValues, locales, Locale.class);
        /*
         * The "magic" applying this language to every children
         * is performed by the 'beforeMarshal(Marshaller)' method.
         */
    }

    /**
     * Returns the default language used for documenting metadata.
     *
     * @return language used for documenting metadata, or {@code null}.
     *
     * @deprecated Replaced by <code>{@linkplain #getLocalesAndCharsets()}.keySet()</code>.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    @XmlElement(name = "language", namespace = LegacyNamespaces.GMD)
    public Locale getLanguage() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return Containers.peekFirst(LocaleAndCharset.getLanguages(getLocalesAndCharsets()));
            /*
             * No warning if the collection contains more than one locale, because
             * this is allowed by the "getLanguage() + getLocales()" contract.
             */
        }
        return null;
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
     * @param  newValue  the new language.
     *
     * @deprecated Replaced by <code>{@linkplain #getLocalesAndCharsets()}.put(newValue, …)</code>.
     */
    @Deprecated(since="1.0")
    public void setLanguage(final Locale newValue) {
        setLocalesAndCharsets(OtherLocales.setFirst(locales, new PT_Locale(newValue)));
    }

    /**
     * Provides information about an alternatively used localized character string for a linguistic extension.
     *
     * @return alternatively used localized character string for a linguistic extension.
     *
     * @deprecated Replaced by <code>{@linkplain #getLocalesAndCharsets()}.keySet()</code>.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    @XmlElement(name = "locale", namespace = LegacyNamespaces.GMD)
    @XmlJavaTypeAdapter(LocaleAdapter.Wrapped.class)
    public Collection<Locale> getLocales() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return Containers.derivedSet(OtherLocales.filter(getLocalesAndCharsets()), ToLocale.INSTANCE);
        }
        return null;
    }

    /**
     * Converter from {@link PT_Locale} and {@link Locale}.
     */
    private static final class ToLocale extends SurjectiveConverter<PT_Locale,Locale> {
        static final ToLocale INSTANCE = new ToLocale();
        private ToLocale() {}
        @Override public Class<PT_Locale> getSourceClass()   {return PT_Locale.class;}
        @Override public Class<Locale>    getTargetClass()   {return    Locale.class;}
        @Override public Locale           apply(PT_Locale p) {return p.getLocale();}
        @Override public ObjectConverter<Locale, PT_Locale> inverse() {return FromLocale.INSTANCE;}
    }

    /**
     * Converter from {@link Locale} and {@link PT_Locale}.
     */
    private static final class FromLocale implements ObjectConverter<Locale,PT_Locale> {
        static final FromLocale INSTANCE = new FromLocale();
        private FromLocale() {}
        @Override public Set<FunctionProperty> properties()     {return EnumSet.of(FunctionProperty.INJECTIVE);}
        @Override public Class<Locale>         getSourceClass() {return Locale.class;}
        @Override public Class<PT_Locale>      getTargetClass() {return PT_Locale.class;}
        @Override public PT_Locale             apply(Locale o)  {return (o != null) ? new PT_Locale(o) : null;}
        @Override public ObjectConverter<PT_Locale, Locale> inverse() {return ToLocale.INSTANCE;}
    }

    /**
     * Returns the character coding standard used for the metadata set.
     *
     * @return character coding standards used for the metadata.
     */
    private Charset getCharacterSets() {
        return LegacyPropertyAdapter.getSingleton(
                (LocaleAndCharset.getCharacterSets(getLocalesAndCharsets())),
                Charset.class, null, DefaultMetadata.class, "getCharacterSet");
    }

    /**
     * Returns the character coding standard used for the metadata set.
     *
     * @return character coding standard used for the metadata, or {@code null}.
     *
     * @deprecated Replaced by <code>{@linkplain #getLocalesAndCharsets()}.values()</code>.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    // @XmlElement at the end of this class.
    public CharacterSet getCharacterSet() {
        return CharacterSet.fromCharset(getCharacterSets());
    }

    /**
     * Sets the character coding standard used for the metadata set.
     *
     * @param  newValue  the new character set.
     *
     * @deprecated Replaced by <code>{@linkplain #getLocalesAndCharsets()}.put(…, newValue)</code>.
     */
    @Deprecated(since="1.0")
    public void setCharacterSet(final CharacterSet newValue) {
        setCharset((newValue != null) ? newValue.toCharset() : null);
    }

    /**
     * Returns an identification of the parent metadata record.
     * This is non-null if this metadata is a subset (child) of another metadata that is described elsewhere.
     *
     * @return identification of the parent metadata record, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "parentMetadata")
    @XmlJavaTypeAdapter(CI_Citation.Since2014.class)
    public Citation getParentMetadata() {
        return parentMetadata;
    }

    /**
     * Sets an identification of the parent metadata record.
     *
     * @param  newValue  the new identification of the parent metadata record.
     *
     * @since 0.5
     */
    public void setParentMetadata(final Citation newValue) {
        checkWritePermission(parentMetadata);
        parentMetadata = newValue;
    }

    /**
     * Returns the file identifier of the metadata to which this metadata is a subset (child).
     *
     * @return identifier of the metadata to which this metadata is a subset, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getParentMetadata()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getParentMetadata")
    @XmlElement(name = "parentIdentifier", namespace = LegacyNamespaces.GMD)
    public String getParentIdentifier() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Citation parentMetadata = getParentMetadata();
            if (parentMetadata != null) {
                final InternationalString title = parentMetadata.getTitle();
                if (title != null) {
                    return title.toString();
                }
            }
        }
        return null;
    }

    /**
     * Sets the file identifier of the metadata to which this metadata is a subset (child).
     *
     * @param  newValue  the new parent identifier.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getParentMetadata()}.
     */
    @Deprecated(since="1.0")
    public void setParentIdentifier(final String newValue) {
        checkWritePermission(parentMetadata);
        // See "Note about deprecated methods implementation"
        DefaultCitation parent = DefaultCitation.castOrCopy(parentMetadata);
        if (newValue != null) {
            if (parent == null) {
                parent = new DefaultCitation();
            }
            parent.setTitle(new SimpleInternationalString(newValue));
            setParentMetadata(parent);
        } else if (parent != null) {
            parent.setTitle(null);
        }
    }

    /**
     * Returns the scope or type of resource for which metadata is provided.
     *
     * @return scope or type of resource for which metadata is provided.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<MetadataScope> getMetadataScopes() {
        return metadataScopes = nonNullCollection(metadataScopes, MetadataScope.class);
    }

    /**
     * Sets the scope or type of resource for which metadata is provided.
     *
     * @param  newValues  the new scope or type of resource.
     *
     * @since 0.5
     */
    public void setMetadataScopes(final Collection<? extends MetadataScope> newValues) {
        metadataScopes = writeCollection(newValues, metadataScopes, MetadataScope.class);
    }

    /**
     * Returns the scope to which the metadata applies.
     *
     * @return scope to which the metadata applies.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataScopes()}
     *   followed by {@link DefaultMetadataScope#getResourceScope()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMetadataScopes")
    @XmlElement(name = "hierarchyLevel", namespace = LegacyNamespaces.GMD)
    public final Collection<ScopeCode> getHierarchyLevels() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new MetadataScopeAdapter<ScopeCode>(getMetadataScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected MetadataScope wrap(final ScopeCode value) {
                return new DefaultMetadataScope(value, null);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected ScopeCode unwrap(final MetadataScope container) {
                return container.getResourceScope();
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final MetadataScope container, final ScopeCode value) {
                if (container instanceof DefaultMetadataScope) {
                    ((DefaultMetadataScope) container).setResourceScope(value);
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the scope to which the metadata applies.
     *
     * @param  newValues  the new hierarchy levels.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataScopes(Collection)}
     *   and {@link DefaultMetadataScope#setResourceScope(ScopeCode)}.
     */
    @Deprecated(since="1.0")
    public void setHierarchyLevels(final Collection<? extends ScopeCode> newValues) {
        checkWritePermission(ImplementationHelper.valueIfDefined(metadataScopes));
        ((LegacyPropertyAdapter<ScopeCode,?>) getHierarchyLevels()).setValues(newValues);
    }

    /**
     * Returns the name of the hierarchy levels for which the metadata is provided.
     *
     * @return hierarchy levels for which the metadata is provided.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataScopes()}
     *   followed by {@link DefaultMetadataScope#getName()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMetadataScopes")
    @XmlElement(name = "hierarchyLevelName", namespace = LegacyNamespaces.GMD)
    public final Collection<String> getHierarchyLevelNames() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new MetadataScopeAdapter<String>(getMetadataScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected MetadataScope wrap(final String value) {
                return new DefaultMetadataScope(null, value);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected String unwrap(final MetadataScope container) {
                final InternationalString name = container.getName();
                return (name != null) ? name.toString() : null;
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final MetadataScope container, final String value) {
                if (container instanceof DefaultMetadataScope) {
                    ((DefaultMetadataScope) container).setName(value != null ? new SimpleInternationalString(value) : null);
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the name of the hierarchy levels for which the metadata is provided.
     *
     * @param  newValues  the new hierarchy level names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMetadataScopes(Collection)}
     *   and {@link DefaultMetadataScope#setName(InternationalString)}.
     */
    @Deprecated(since="1.0")
    public void setHierarchyLevelNames(final Collection<? extends String> newValues) {
        checkWritePermission(ImplementationHelper.valueIfDefined(metadataScopes));
        ((LegacyPropertyAdapter<String,?>) getHierarchyLevelNames()).setValues(newValues);
    }

    /**
     * Returns the parties responsible for the metadata information.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return parties responsible for the metadata information.
     */
    @Override
    @XmlElement(name = "contact", required = true)
    public Collection<ResponsibleParty> getContacts() {
        return contacts = nonNullCollection(contacts, ResponsibleParty.class);
    }

    /**
     * Sets the parties responsible for the metadata information.
     *
     * @param  newValues  the new contacts.
     */
    public void setContacts(final Collection<? extends ResponsibleParty> newValues) {
        contacts = writeCollection(newValues, contacts, ResponsibleParty.class);
    }

    /**
     * Returns the date(s) associated with the metadata.
     *
     * @return date(s) associated with the metadata.
     *
     * @see Citation#getDates()
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<CitationDate> getDateInfo() {
        return dateInfo = nonNullCollection(dateInfo, CitationDate.class);
    }

    /**
     * Sets the date(s) associated with the metadata.
     * The collection should contain at least an element for {@link DateType#CREATION}.
     *
     * @param  newValues  new dates associated with the metadata.
     *
     * @since 0.5
     */
    public void setDateInfo(final Collection<? extends CitationDate> newValues) {
        dateInfo = writeCollection(newValues, dateInfo, CitationDate.class);
    }

    /**
     * Returns the date that the metadata was created.
     *
     * @return date that the metadata was created, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getDateInfo()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getDateInfo")
    @XmlElement(name = "dateStamp", namespace = LegacyNamespaces.GMD)
    public Date getDateStamp() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Collection<CitationDate> dates = getDateInfo();
            if (dates != null) {
                for (final CitationDate date : dates) {
                    if (date.getDateType() == DateType.CREATION) {
                        return date.getDate();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the date that the metadata was created.
     *
     * @param  newValue  the new date stamp.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setDateInfo(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setDateStamp(final Date newValue) {
        checkWritePermission(ImplementationHelper.valueIfDefined(dateInfo));
        Collection<CitationDate> newValues = dateInfo;      // See "Note about deprecated methods implementation"
        if (newValues == null) {
            if (newValue == null) {
                return;
            }
            newValues = new ArrayList<>(1);
        } else {
            final Iterator<CitationDate> it = newValues.iterator();
            while (it.hasNext()) {
                final CitationDate date = it.next();
                if (date.getDateType() == DateType.CREATION) {
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
        newValues.add(new DefaultCitationDate(TemporalDate.toTemporal(newValue), DateType.CREATION));
        setDateInfo(newValues);
    }

    /**
     * Returns the citation(s) for the standard(s) to which the metadata conform.
     * The collection returned by this method typically contains elements from the
     * {@link org.apache.sis.metadata.iso.citation.Citations#ISO_19115} list.
     *
     * @return the standard(s) to which the metadata conform.
     *
     * @see #getMetadataProfiles()
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getMetadataStandards() {
        return metadataStandards = nonNullCollection(metadataStandards, Citation.class);
    }

    /**
     * Sets the citation(s) for the standard(s) to which the metadata conform.
     * Metadata standard citations should include an identifier.
     *
     * @param  newValues  the new standard(s) to which the metadata conform.
     *
     * @since 0.5
     */
    public void setMetadataStandards(final Collection<? extends Citation> newValues) {
        metadataStandards = writeCollection(newValues, metadataStandards, Citation.class);
    }

    /**
     * Returns the citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     *
     * @return the profile(s) to which the metadata conform.
     *
     * @see #getMetadataStandards()
     * @see #getMetadataExtensionInfo()
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getMetadataProfiles() {
        return metadataProfiles = nonNullCollection(metadataProfiles, Citation.class);
    }

    /**
     * Set the citation(s) for the profile(s) of the metadata standard to which the metadata conform.
     * Metadata profile standard citations should include an identifier.
     *
     * @param  newValues  the new profile(s) to which the metadata conform.
     *
     * @since 0.5
     */
    public void setMetadataProfiles(final Collection<? extends Citation> newValues) {
        metadataProfiles = writeCollection(newValues, metadataProfiles, Citation.class);
    }

    /**
     * Returns reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     *
     * @return reference(s) to alternative metadata (e.g. Dublin core, FGDC).
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getAlternativeMetadataReferences() {
        return alternativeMetadataReferences = nonNullCollection(alternativeMetadataReferences, Citation.class);
    }

    /**
     * Set reference(s) to alternative metadata or metadata in a non-ISO standard for the same resource.
     *
     * @param  newValues  the new reference(s) to alternative metadata (e.g. Dublin core, FGDC).
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
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Citation standard = LegacyPropertyAdapter.getSingleton(getMetadataStandards(),
                    Citation.class, null, DefaultMetadata.class,
                    version ? "getMetadataStandardName" : "getMetadataStandardVersion");
            if (standard != null) {
                final InternationalString title = version ? standard.getEdition() : standard.getTitle();
                if (title != null) {
                    return title.toString();
                }
            }
        }
        return null;
    }

    /**
     * Implementation of legacy {@link #setMetadataStandardName(String)} and
     * {@link #setMetadataStandardVersion(String)} methods.
     */
    private void setMetadataStandard(final boolean version, final String newValue) {
        checkWritePermission(ImplementationHelper.valueIfDefined(metadataStandards));
        final InternationalString i18n = (newValue != null) ? new SimpleInternationalString(newValue) : null;
        final List<Citation> newValues = (metadataStandards != null)
                ? new ArrayList<>(metadataStandards)
                : new ArrayList<>(1);
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
     * @return name of the metadata standard used, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#getTitle()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMetadataStandards")
    @XmlElement(name = "metadataStandardName", namespace = LegacyNamespaces.GMD)
    public String getMetadataStandardName() {
        return getMetadataStandard(false);
    }

    /**
     * Name of the metadata standard (including profile name) used.
     *
     * @param  newValue  the new metadata standard name.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#setTitle(InternationalString)}.
     */
    @Deprecated(since="1.0")
    public void setMetadataStandardName(final String newValue) {
        setMetadataStandard(false, newValue);
    }

    /**
     * Returns the version (profile) of the metadata standard used.
     *
     * @return version of the metadata standard used, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#getEdition()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMetadataStandards")
    @XmlElement(name = "metadataStandardVersion", namespace = LegacyNamespaces.GMD)
    public String getMetadataStandardVersion() {
        return getMetadataStandard(true);
    }

    /**
     * Sets the version (profile) of the metadata standard used.
     *
     * @param  newValue  the new metadata standard version.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMetadataStandards()}
     *   followed by {@link DefaultCitation#setEdition(InternationalString)}.
     */
    @Deprecated(since="1.0")
    public void setMetadataStandardVersion(final String newValue) {
        setMetadataStandard(true, newValue);
    }

    /**
     * Returns the online location(s) where the metadata is available.
     *
     * @return online location(s) where the metadata is available.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<OnlineResource> getMetadataLinkages() {
        return metadataLinkages = nonNullCollection(metadataLinkages, OnlineResource.class);
    }

    /**
     * Sets the online location(s) where the metadata is available.
     *
     * @param  newValues  the new online location(s).
     *
     * @since 0.5
     */
    public void setMetadataLinkages(final Collection<? extends OnlineResource> newValues) {
        metadataLinkages = writeCollection(newValues, metadataLinkages, OnlineResource.class);
    }

    /**
     * Provides the URI of the dataset to which the metadata applies.
     *
     * @return Uniform Resource Identifier of the dataset, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getIdentificationInfo()} followed by
     *    {@link DefaultDataIdentification#getCitation()} followed by {@link DefaultCitation#getOnlineResources()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getIdentificationInfo")
    @XmlElement(name = "dataSetURI", namespace = LegacyNamespaces.GMD)
    public String getDataSetUri() {
        String linkage = null;
        final Collection<Identification> info;
        if (FilterByVersion.LEGACY_METADATA.accept() && (info = getIdentificationInfo()) != null) {
            for (final Identification identification : info) {
                final Citation citation = identification.getCitation();
                if (citation != null) {
                    final Collection<? extends OnlineResource> onlineResources = citation.getOnlineResources();
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
     * @param  newValue  the new data set URI.
     * @throws URISyntaxException if the given value cannot be parsed as a URI.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getIdentificationInfo()}
     *    followed by {@link DefaultDataIdentification#getCitation()}
     *    followed by {@link DefaultCitation#setOnlineResources(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setDataSetUri(final String newValue) throws URISyntaxException {
        final URI uri = (newValue != null) ? new URI(newValue) : null;
        Collection<Identification> info = identificationInfo;   // See "Note about deprecated methods implementation"
        checkWritePermission(ImplementationHelper.valueIfDefined(info));
        AbstractIdentification firstId = AbstractIdentification.castOrCopy(Containers.peekFirst(info));
        if (firstId == null) {
            if (uri == null) return;
            firstId = new DefaultDataIdentification();
        }
        DefaultCitation citation = DefaultCitation.castOrCopy(firstId.getCitation());
        if (citation == null) {
            if (uri == null) return;
            citation = new DefaultCitation();
        }
        Collection<OnlineResource> onlineResources = citation.getOnlineResources();
        DefaultOnlineResource firstOnline = DefaultOnlineResource.castOrCopy(Containers.peekFirst(onlineResources));
        if (firstOnline == null) {
            if (uri == null) return;
            firstOnline = new DefaultOnlineResource();
        }
        firstOnline.setLinkage(uri);
        onlineResources = ImplementationHelper.setFirst(onlineResources, firstOnline);
        citation.setOnlineResources(onlineResources);
        firstId.setCitation(citation);
        info = ImplementationHelper.setFirst(info, firstId);
        setIdentificationInfo(info);
    }

    /**
     * Returns the digital representation of spatial information in the dataset.
     *
     * @return digital representation of spatial information in the dataset.
     */
    @Override
    @XmlElement(name = "spatialRepresentationInfo")
    public Collection<SpatialRepresentation> getSpatialRepresentationInfo() {
        return spatialRepresentationInfo = nonNullCollection(spatialRepresentationInfo, SpatialRepresentation.class);
    }

    /**
     * Sets the digital representation of spatial information in the dataset.
     *
     * @param  newValues  the new spatial representation info.
     */
    public void setSpatialRepresentationInfo(final Collection<? extends SpatialRepresentation> newValues) {
        spatialRepresentationInfo = writeCollection(newValues, spatialRepresentationInfo, SpatialRepresentation.class);
    }

    /**
     * Returns the description of the spatial and temporal reference systems used in the dataset.
     *
     * @return spatial and temporal reference systems used in the dataset.
     */
    @Override
    @XmlElement(name = "referenceSystemInfo")
    public Collection<ReferenceSystem> getReferenceSystemInfo() {
        return referenceSystemInfo = nonNullCollection(referenceSystemInfo, ReferenceSystem.class);
    }

    /**
     * Sets the description of the spatial and temporal reference systems used in the dataset.
     *
     * @param  newValues  the new reference system info.
     */
    public void setReferenceSystemInfo(final Collection<? extends ReferenceSystem> newValues) {
        referenceSystemInfo = writeCollection(newValues, referenceSystemInfo, ReferenceSystem.class);
    }

    /**
     * Returns information describing metadata extensions.
     *
     * @return metadata extensions.
     */
    @Override
    @XmlElement(name = "metadataExtensionInfo")
    public Collection<MetadataExtensionInformation> getMetadataExtensionInfo() {
        return metadataExtensionInfo = nonNullCollection(metadataExtensionInfo, MetadataExtensionInformation.class);
    }

    /**
     * Sets information describing metadata extensions.
     *
     * @param  newValues  the new metadata extension info.
     */
    public void setMetadataExtensionInfo(final Collection<? extends MetadataExtensionInformation> newValues) {
        metadataExtensionInfo = writeCollection(newValues, metadataExtensionInfo, MetadataExtensionInformation.class);
    }

    /**
     * Returns basic information about the resource(s) to which the metadata applies.
     *
     * @return the resource(s) to which the metadata applies.
     */
    @Override
    @XmlElement(name = "identificationInfo", required = true)
    public Collection<Identification> getIdentificationInfo() {
        return identificationInfo = nonNullCollection(identificationInfo, Identification.class);
    }

    /**
     * Sets basic information about the resource(s) to which the metadata applies.
     *
     * @param  newValues  the new identification info.
     */
    public void setIdentificationInfo(final Collection<? extends Identification> newValues) {
        identificationInfo = writeCollection(newValues, identificationInfo, Identification.class);
    }

    /**
     * Returns information about the feature catalogue and describes the coverage and
     * image data characteristics.
     *
     * @return the feature catalogue, coverage descriptions and image data characteristics.
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
     * @param  newValues  the new content info.
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
     * @return the distributor of and options for obtaining the resource(s).
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
     * @param  newValue  the new distribution info.
     */
    public void setDistributionInfo(final Distribution newValue) {
        checkWritePermission(distributionInfo);
        distributionInfo = newValue;
    }

    /**
     * Returns overall assessment of quality of a resource(s).
     *
     * @return overall assessment of quality of a resource(s).
     */
    @Override
    @XmlElement(name = "dataQualityInfo")
    public Collection<DataQuality> getDataQualityInfo() {
        return dataQualityInfo = nonNullCollection(dataQualityInfo, DataQuality.class);
    }

    /**
     * Sets overall assessment of quality of a resource(s).
     *
     * @param  newValues  the new data quality info.
     */
    public void setDataQualityInfo(final Collection<? extends DataQuality> newValues) {
        dataQualityInfo = writeCollection(newValues, dataQualityInfo, DataQuality.class);
    }

    /**
     * Returns information about the catalogue of rules defined for the portrayal of a resource(s).
     *
     * @return the catalogue of rules defined for the portrayal of a resource(s).
     */
    @Override
    @XmlElement(name = "portrayalCatalogueInfo")
    public Collection<PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return portrayalCatalogueInfo = nonNullCollection(portrayalCatalogueInfo, PortrayalCatalogueReference.class);
    }

    /**
     * Sets information about the catalogue of rules defined for the portrayal of a resource(s).
     *
     * @param  newValues  the new portrayal catalog info.
     */
    public void setPortrayalCatalogueInfo(final Collection<? extends PortrayalCatalogueReference> newValues) {
        portrayalCatalogueInfo = writeCollection(newValues, portrayalCatalogueInfo, PortrayalCatalogueReference.class);
    }

    /**
     * Returns restrictions on the access and use of metadata.
     *
     * @return restrictions on the access and use of metadata.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#getResourceConstraints()
     */
    @Override
    @XmlElement(name = "metadataConstraints")
    public Collection<Constraints> getMetadataConstraints() {
        return metadataConstraints = nonNullCollection(metadataConstraints, Constraints.class);
    }

    /**
     * Sets restrictions on the access and use of metadata.
     *
     * @param  newValues  the new metadata constraints.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#setResourceConstraints(Collection)
     */
    public void setMetadataConstraints(final Collection<? extends Constraints> newValues) {
        metadataConstraints = writeCollection(newValues, metadataConstraints, Constraints.class);
    }

    /**
     * Returns information about the conceptual schema of a dataset.
     *
     * @return the conceptual schema of a dataset.
     */
    @Override
    @XmlElement(name = "applicationSchemaInfo")
    public Collection<ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return applicationSchemaInfo = nonNullCollection(applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Returns information about the conceptual schema of a dataset.
     *
     * @param  newValues  the new application schema info.
     */
    public void setApplicationSchemaInfo(final Collection<? extends ApplicationSchemaInformation> newValues) {
        applicationSchemaInfo = writeCollection(newValues, applicationSchemaInfo, ApplicationSchemaInformation.class);
    }

    /**
     * Returns information about the acquisition of the data.
     *
     * @return the acquisition of data.
     */
    @Override
    @XmlElement(name = "acquisitionInformation")
    public Collection<AcquisitionInformation> getAcquisitionInformation() {
        return acquisitionInformation = nonNullCollection(acquisitionInformation, AcquisitionInformation.class);
    }

    /**
     * Sets information about the acquisition of the data.
     *
     * @param  newValues  the new acquisition information.
     */
    public void setAcquisitionInformation(final Collection<? extends AcquisitionInformation> newValues) {
        acquisitionInformation = writeCollection(newValues, acquisitionInformation, AcquisitionInformation.class);
    }

    /**
     * Returns information about the frequency of metadata updates, and the scope of those updates.
     *
     * @return the frequency of metadata updates and their scope, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#getResourceMaintenances()
     */
    @Override
    @XmlElement(name = "metadataMaintenance")
    public MaintenanceInformation getMetadataMaintenance() {
        return metadataMaintenance;
    }

    /**
     * Sets information about the frequency of metadata updates, and the scope of those updates.
     *
     * @param  newValue  the new metadata maintenance.
     *
     * @see org.apache.sis.metadata.iso.identification.AbstractIdentification#setResourceMaintenances(Collection)
     */
    public void setMetadataMaintenance(final MaintenanceInformation newValue) {
        checkWritePermission(metadataMaintenance);
        metadataMaintenance = newValue;
    }

    /**
     * Returns information about the provenance, sources and/or the production processes applied to the resource.
     *
     * @return information about the provenance, sources and/or the production processes.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Lineage> getResourceLineages() {
        return resourceLineages = nonNullCollection(resourceLineages, Lineage.class);
    }

    /**
     * Sets information about the provenance, sources and/or the production processes applied to the resource.
     *
     * @param newValues new information about the provenance, sources and/or the production processes.
     *
     * @since 0.5
     */
    public void setResourceLineages(final Collection<? extends Lineage> newValues) {
        resourceLineages = writeCollection(newValues, resourceLineages, Lineage.class);
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
     * Invoked by JAXB {@link jakarta.xml.bind.Marshaller} before this object is marshalled to XML.
     * This method sets the locale to be used for XML marshalling to the metadata language.
     */
    @SuppressWarnings("unused")
    private void beforeMarshal(final Marshaller marshaller) {
        Context.push(Containers.peekFirst(LocaleAndCharset.getLanguages(getLocalesAndCharsets())));
    }

    /**
     * Invoked by JAXB {@link jakarta.xml.bind.Marshaller} after this object has been marshalled to XML.
     * This method restores the locale to be used for XML marshalling to its previous value.
     */
    @SuppressWarnings("unused")
    private void afterMarshal(final Marshaller marshaller) {
        Context.pull();
    }

    /**
     * Gets the default locale for this record (used in ISO 19115-3:2016 format).
     */
    @XmlElement(name = "defaultLocale")
    private PT_Locale getDefaultLocale() {
        return FilterByVersion.CURRENT_METADATA.accept() ? PT_Locale.first(getLocalesAndCharsets()) : null;
    }

    /**
     * Sets the default locale for this record (used in ISO 19115-3:2016 format).
     */
    private void setDefaultLocale(final PT_Locale newValue) {
        setLocalesAndCharsets(OtherLocales.setFirst(locales, newValue));
    }

    /**
     * Gets the other locales for this record (used in ISO 19115-3:2016 format).
     */
    @XmlElement(name = "otherLocale")
    private Collection<PT_Locale> getOtherLocales() {
        return FilterByVersion.CURRENT_METADATA.accept() ? OtherLocales.filter(getLocalesAndCharsets()) : null;
    }

    /**
     * Returns the character coding for the metadata set (used in legacy ISO 19157 format).
     */
    @XmlElement(name = "characterSet", namespace = LegacyNamespaces.GMD)
    private Charset getCharset() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return getCharacterSets();
        }
        return null;
    }

    /**
     * Sets the character coding standard for the metadata set (used in legacy ISO 19157 format).
     */
    private void setCharset(final Charset newValue) {
        setLocalesAndCharsets(LocaleAndCharset.setCharacterSets(getLocalesAndCharsets(), Containers.singletonOrEmpty(newValue)));
    }

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "dateInfo", required = true)
    private Collection<CitationDate> getDates() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getDateInfo() : null;
    }

    @XmlElement(name = "metadataStandard")
    private Collection<Citation> getMetadataStandard() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMetadataStandards() : null;
    }

    @XmlElement(name = "metadataProfile")
    private Collection<Citation> getMetadataProfile() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMetadataProfiles() : null;
    }

    @XmlElement(name = "alternativeMetadataReference")
    private Collection<Citation> getAlternativeMetadataReference() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAlternativeMetadataReferences() : null;
    }

    @XmlElement(name = "metadataLinkage")
    private Collection<OnlineResource> getMetadataLinkage() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMetadataLinkages() : null;
    }

    @XmlElement(name = "resourceLineage")
    private Collection<Lineage> getResourceLineage() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getResourceLineages() : null;
    }

    @XmlElement(name = "metadataScope")
    private Collection<MetadataScope> getMetadataScope() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMetadataScopes() : null;
    }
}
