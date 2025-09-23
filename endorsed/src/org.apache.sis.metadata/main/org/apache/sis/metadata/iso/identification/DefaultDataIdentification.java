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

import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.nio.charset.Charset;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.DataIdentification;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.lan.LocaleAndCharset;
import org.apache.sis.xml.bind.lan.OtherLocales;
import org.apache.sis.xml.bind.lan.PT_Locale;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.metadata.internal.Dependencies;

// Specific to the main and geoapi-3.1 branches:
import java.util.stream.Collectors;
import org.opengis.metadata.identification.CharacterSet;


/**
 * Information required to identify a dataset.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_DataIdentification}
 * {@code   ├─citation………………………………………} Citation data for the resource(s).
 * {@code   │   ├─title……………………………………} Name by which the cited resource is known.
 * {@code   │   └─date………………………………………} Reference date for the cited resource.
 * {@code   ├─abstract………………………………………} Brief narrative summary of the content of the resource(s).
 * {@code   ├─language………………………………………} Language(s) used within the dataset.
 * {@code   ├─characterSet……………………………} Full name of the character coding standard(s) used for the dataset.
 * {@code   ├─topicCategory…………………………} Main theme(s) of the dataset.
 * {@code   └─extent……………………………………………} Bounding polygon, vertical, and temporal extent of the dataset.
 * {@code       ├─description……………………} The spatial and temporal extent for the referring object.
 * {@code       ├─geographicElement……} Geographic component of the extent of the referring object.
 * {@code       ├─temporalElement…………} Temporal component of the extent of the referring object.
 * {@code       └─verticalElement…………} Vertical component of the extent of the referring object.</div>
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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_DataIdentification_Type", propOrder = {
    "languages",                // Legacy ISO 19115:2003
    "characterSet",             // Legacy ISO 19115:2003
    "defaultLocale",            // New in ISO 19115:2014
    "otherLocales",             // New in ISO 19115:2014
    "environmentDescription",
    "supplementalInformation"
    /*
     * In ISO 19115:2003, we had an "topicCategory" attribute before "environmentDescription"
     * and an "extent" attribute before "supplementalInformation". In ISO 19115:2014 revision,
     * those attributes moved to the parent class. Apache SIS 1.0 aligns itself on the latest
     * standard, but the consequence is that attribute order is wrong when marshalling an ISO
     * 19139:2007 document.  We could workaround by defining private methods, but it confuses
     * PropertyAccessor. We choose to avoid this complication in this class and handle element
     * reordering in org.apache.sis.xml.TransformingWriter instead.
     */
})
@XmlRootElement(name = "MD_DataIdentification")
public class DefaultDataIdentification extends AbstractIdentification implements DataIdentification {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 7302901752833238436L;

    /**
     * Language(s) and character set(s) used within the dataset.
     */
    @SuppressWarnings("serial")
    private Map<Locale,Charset> locales;

    /**
     * Description of the dataset in the producer's processing environment, including items
     * such as the software, the computer operating system, file name, and the dataset size
     */
    @SuppressWarnings("serial")
    private InternationalString environmentDescription;

    /**
     * Any other descriptive information about the dataset.
     */
    @SuppressWarnings("serial")
    private InternationalString supplementalInformation;

    /**
     * Constructs an initially empty data identification.
     */
    public DefaultDataIdentification() {
    }

    /**
     * Creates a data identification initialized to the specified values.
     *
     * @param citation       the citation data for the resource(s), or {@code null} if none.
     * @param abstracts      a brief narrative summary of the content of the resource(s), or {@code null} if none.
     * @param language       the language used within the dataset, or {@code null} if none.
     * @param topicCategory  the main theme of the dataset, or {@code null} if none.
     */
    public DefaultDataIdentification(final Citation citation,
                                     final CharSequence abstracts,
                                     final Locale language,
                                     final TopicCategory topicCategory)
    {
        super(citation, abstracts, topicCategory);
        if (language != null) {
            locales = copyMap(Collections.singletonMap(language, null), Locale.class);
        }
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataIdentification)
     */
    public DefaultDataIdentification(final DataIdentification object) {
        super(object);
        if (object != null) {
            locales                 = copyMap(object.getLocalesAndCharsets(), Locale.class);
            environmentDescription  = object.getEnvironmentDescription();
            supplementalInformation = object.getSupplementalInformation();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDataIdentification}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDataIdentification} instance is created using the
     *       {@linkplain #DefaultDataIdentification(DataIdentification) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDataIdentification castOrCopy(final DataIdentification object) {
        if (object == null || object instanceof DefaultDataIdentification) {
            return (DefaultDataIdentification) object;
        }
        return new DefaultDataIdentification(object);
    }

    /**
     * Returns the language(s) and character set(s) used within the dataset.
     * The first element in iteration order is the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * @return language(s) and character set(s) used within the dataset.
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
     * @param  newValues  the new language(s) and character set(s) used within the dataset.
     *
     * @since 1.0
     */
    public void setLocalesAndCharsets(final Map<? extends Locale, ? extends Charset> newValues) {
        locales = writeMap(newValues, locales, Locale.class);
    }

    /**
     * Returns the language(s) used within the resource.
     * The first element in iteration order shall be the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * <p>The language string representations should use ISO 639-2 language code as
     * returned by {@link Locale#getISO3Language()}.</p>
     *
     * @return language(s) used.
     *
     * @deprecated Replaced by {@code getLocalesAndCharsets().keySet()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    @XmlElement(name = "language", namespace = LegacyNamespaces.GMD)
    public Collection<Locale> getLanguages() {
        return FilterByVersion.LEGACY_METADATA.accept() ? LocaleAndCharset.getLanguages(getLocalesAndCharsets()) : null;
    }

    /**
     * Sets the language(s) used within the resource.
     *
     * @param  newValues  the new languages.
     *
     * @deprecated Replaced by putting keys in {@link #getLocalesAndCharsets()} map.
     */
    @Deprecated(since="1.0")
    public void setLanguages(final Collection<? extends Locale> newValues) {
        // TODO: delete after SIS 1.0 release (method not needed by JAXB).
        setLocalesAndCharsets(LocaleAndCharset.setLanguages(getLocalesAndCharsets(), newValues));
    }

    /**
     * Returns the character coding standard used for the dataset.
     *
     * <div class="warning"><b>Upcoming API change — JDK integration</b><br>
     * The element type may change to the {@link Charset} class in GeoAPI 4.0.
     * </div>
     *
     * @return character coding standard(s) used.
     *
     * @deprecated Replaced by {@code getLocalesAndCharsets().values()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    // @XmlElement at the end of this class.
    public Collection<CharacterSet> getCharacterSets() {
        return getLocalesAndCharsets().values().stream().map(CharacterSet::fromCharset).collect(Collectors.toSet());
    }

    /**
     * Sets the character coding standard used for the dataset.
     *
     * <div class="warning"><b>Upcoming API change — JDK integration</b><br>
     * The element type may change to the {@link Charset} class in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new character sets.
     *
     * @deprecated Replaced by putting values in {@link #getLocalesAndCharsets()} map.
     */
    @Deprecated(since="1.0")
    public void setCharacterSets(final Collection<? extends CharacterSet> newValues) {
        // TODO: delete after SIS 1.0 release (method not needed by JAXB).
        Collection<Charset> c = null;
        if (newValues != null) {
            c = newValues.stream().map(CharacterSet::toCharset).collect(Collectors.toSet());
        }
        setLocalesAndCharsets(LocaleAndCharset.setCharacterSets(getLocalesAndCharsets(), c));
    }

    /**
     * Returns a description of the resource in the producer's processing environment. This includes
     * items such as the software, the computer operating system, file name, and the dataset size.
     *
     * @return description of the resource in the producer's processing environment, or {@code null}.
     */
    @Override
    @XmlElement(name = "environmentDescription")
    public InternationalString getEnvironmentDescription() {
        return environmentDescription;
    }

    /**
     * Sets the description of the resource in the producer's processing environment.
     *
     * @param  newValue  the new environment description.
     */
    public void setEnvironmentDescription(final InternationalString newValue)  {
        checkWritePermission(environmentDescription);
        environmentDescription = newValue;
    }

    /**
     * Any other descriptive information about the resource.
     *
     * @return other descriptive information, or {@code null}.
     */
    @Override
    @XmlElement(name = "supplementalInformation")
    public InternationalString getSupplementalInformation() {
        return supplementalInformation;
    }

    /**
     * Sets any other descriptive information about the resource.
     *
     * @param  newValue  the new supplemental information.
     */
    public void setSupplementalInformation(final InternationalString newValue) {
        checkWritePermission(supplementalInformation);
        supplementalInformation = newValue;
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
     * Gets the default locale for this record (used in ISO 19115-3 format).
     */
    @XmlElement(name = "defaultLocale")
    private PT_Locale getDefaultLocale() {
        return FilterByVersion.CURRENT_METADATA.accept() ? PT_Locale.first(getLocalesAndCharsets()) : null;
    }

    /**
     * Sets the default locale for this record (used in ISO 19115-3 format).
     */
    @SuppressWarnings("unused")
    private void setDefaultLocale(final PT_Locale newValue) {
        setLocalesAndCharsets(OtherLocales.setFirst(locales, newValue));
    }

    /**
     * Gets the other locales for this record (used in ISO 19115-3 format).
     */
    @XmlElement(name = "otherLocale")
    private Collection<PT_Locale> getOtherLocales() {
        return FilterByVersion.CURRENT_METADATA.accept() ? OtherLocales.filter(getLocalesAndCharsets()) : null;
    }

    /**
     * Returns the character coding for the metadata set (used in legacy ISO 19157 format).
     *
     * @see #getCharacterSets()
     */
    @XmlElement(name = "characterSet", namespace = LegacyNamespaces.GMD)
    private Collection<Charset> getCharacterSet() {
        return FilterByVersion.LEGACY_METADATA.accept() ? LocaleAndCharset.getCharacterSets(getLocalesAndCharsets()) : null;
    }
}
