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
import java.util.Locale;
import java.nio.charset.Charset;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.util.InternationalString;

import static org.apache.sis.internal.jaxb.gco.PropertyType.LEGACY_XML;


/**
 * Information required to identify a dataset.
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
@XmlType(name = "MD_DataIdentification_Type", propOrder = {
    "languages",
    "characterSets",
    "topicCategory",
    "environmentDescription",
    "extent",
    "supplementalInformation"
})
@XmlRootElement(name = "MD_DataIdentification")
public class DefaultDataIdentification extends AbstractIdentification implements DataIdentification {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 6104637930243499850L;

    /**
     * Language(s) used within the dataset.
     */
    private Collection<Locale> languages;

    /**
     * Full name of the character coding standard used for the dataset.
     */
    private Collection<CharacterSet> characterSets;

    /**
     * Description of the dataset in the producers processing environment, including items
     * such as the software, the computer operating system, file name, and the dataset size
     */
    private InternationalString environmentDescription;

    /**
     * Any other descriptive information about the dataset.
     */
    private InternationalString supplementalInformation;

    /**
     * Constructs an initially empty data identification.
     */
    public DefaultDataIdentification() {
    }

    /**
     * Creates a data identification initialized to the specified values.
     *
     * @param citation      The citation data for the resource(s), or {@code null} if none.
     * @param abstracts     A brief narrative summary of the content of the resource(s), or {@code null} if none.
     * @param language      The language used within the dataset, or {@code null} if none.
     * @param topicCategory The main theme of the dataset, or {@code null} if none.
     */
    public DefaultDataIdentification(final Citation citation,
                                     final CharSequence abstracts,
                                     final Locale language,
                                     final TopicCategory topicCategory)
    {
        super(citation, abstracts);
        languages = singleton(language, Locale.class);
        super.setTopicCategories(singleton(topicCategory, TopicCategory.class));
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataIdentification)
     */
    public DefaultDataIdentification(final DataIdentification object) {
        super(object);
        if (object != null) {
            languages                  = copyCollection(object.getLanguages(), Locale.class);
            characterSets              = copyCollection(object.getCharacterSets(), CharacterSet.class);
            environmentDescription     = object.getEnvironmentDescription();
            supplementalInformation    = object.getSupplementalInformation();
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
     *       {@linkplain #DefaultDataIdentification(DataIdentification) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDataIdentification castOrCopy(final DataIdentification object) {
        if (object == null || object instanceof DefaultDataIdentification) {
            return (DefaultDataIdentification) object;
        }
        return new DefaultDataIdentification(object);
    }

    /**
     * Returns the language(s) used within the resource.
     * The first element in iteration order shall be the default language.
     * All other elements, if any, are alternate language(s) used within the resource.
     *
     * <p>The language string representations should use ISO 639-2 language code as
     * returned by {@link Locale#getISO3Language()}.</p>
     *
     * @return Language(s) used.
     *
     * @see Locale#getISO3Language()
     */
    @Override
    @XmlElement(name = "language", required = true)
    public Collection<Locale> getLanguages() {
        return languages = nonNullCollection(languages, Locale.class);
    }

    /**
     * Sets the language(s) used within the dataset.
     *
     * @param newValues The new languages.
     */
    public void setLanguages(final Collection<? extends Locale> newValues)  {
        languages = writeCollection(newValues, languages, Locale.class);
    }

    /**
     * Returns the character coding standard used for the dataset.
     *
     * <div class="warning"><b>Upcoming API change — JDK integration</b><br>
     * The element type may change to the {@link Charset} class in GeoAPI 4.0.
     * </div>
     *
     * @return Character coding standard(s) used.
     */
    @Override
    @XmlElement(name = "characterSet")
    public Collection<CharacterSet> getCharacterSets() {
        return characterSets = nonNullCollection(characterSets, CharacterSet.class);
    }

    /**
     * Sets the character coding standard used for the dataset.
     *
     * <div class="warning"><b>Upcoming API change — JDK integration</b><br>
     * The element type may change to the {@link Charset} class in GeoAPI 4.0.
     * </div>
     *
     * @param newValues The new character sets.
     */
    public void setCharacterSets(final Collection<? extends CharacterSet> newValues) {
        characterSets = writeCollection(newValues, characterSets, CharacterSet.class);
    }

    /**
     * Returns a description of the resource in the producer's processing environment. This includes
     * items such as the software, the computer operating system, file name, and the dataset size.
     *
     * @return Description of the resource in the producer's processing environment, or {@code null}.
     */
    @Override
    @XmlElement(name = "environmentDescription")
    public InternationalString getEnvironmentDescription() {
        return environmentDescription;
    }

    /**
     * Sets the description of the resource in the producers processing environment.
     *
     * @param newValue The new environment description.
     */
    public void setEnvironmentDescription(final InternationalString newValue)  {
        checkWritePermission();
        environmentDescription = newValue;
    }

    /**
     * Any other descriptive information about the resource.
     *
     * @return Other descriptive information, or {@code null}.
     */
    @Override
    @XmlElement(name = "supplementalInformation")
    public InternationalString getSupplementalInformation() {
        return supplementalInformation;
    }

    /**
     * Sets any other descriptive information about the resource.
     *
     * @param newValue The new supplemental information.
     */
    public void setSupplementalInformation(final InternationalString newValue) {
        checkWritePermission();
        supplementalInformation = newValue;
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
     * For JAXB marhalling of ISO 19115:2003 document only.
     */
    @XmlElement(name = "topicCategory")
    private Collection<TopicCategory> getTopicCategory()  {
        return LEGACY_XML ? getTopicCategories() : null;
    }

    /**
     * For JAXB unmarhalling of ISO 19115:2003 document only.
     */
    private void setTopicCategory(final Collection<? extends TopicCategory> newValues) {
        setTopicCategories(newValues);
    }

    /**
     * For JAXB marhalling of ISO 19115:2003 document only.
     */
    @XmlElement(name = "extent")
    private Collection<Extent> getExtent() {
        return LEGACY_XML ? getExtents() : null;
    }

    /**
     * For JAXB unmarhalling of ISO 19115:2003 document only.
     */
    private void setExtent(final Collection<? extends Extent> newValues) {
        setExtents(newValues);
    }
}
