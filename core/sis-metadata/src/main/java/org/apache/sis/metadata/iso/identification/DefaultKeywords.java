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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.KeywordClass;
import org.apache.sis.internal.jaxb.metadata.MD_KeywordClass;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;


/**
 * Keywords, their type and reference source.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Keywords}
 * {@code   └─keyword……} Commonly used word(s) or formalised word(s) or phrase(s) used to describe the subject.</div>
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
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlType(name = "MD_Keywords_Type", propOrder = {
    "keywords",
    "type",
    "thesaurusName",
    "keywordClass"
})
@XmlRootElement(name = "MD_Keywords")
public class DefaultKeywords extends ISOMetadata implements Keywords {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -2765705888428016211L;

    /**
     * Commonly used word(s) or formalised word(s) or phrase(s) used to describe the subject.
     */
    private Collection<InternationalString> keywords;

    /**
     * Subject matter used to group similar keywords.
     */
    private KeywordType type;

    /**
     * Name of the formally registered thesaurus or a similar authoritative source of keywords.
     */
    private Citation thesaurusName;

    /**
     * User-defined categorization of groups of keywords that extend or are orthogonal
     * to the standardized {@linkplain #getType() keyword type} codes.
     */
    private KeywordClass keywordClass;

    /**
     * Constructs an initially empty keywords.
     */
    public DefaultKeywords() {
        super();
    }

    /**
     * Creates keywords initialized to the given key word.
     *
     * @param keywords  commonly used words or formalised words or phrases used to describe the subject,
     *                  or {@code null} if none.
     */
    public DefaultKeywords(final CharSequence... keywords) {
        if (keywords != null) {
            for (final CharSequence keyword : keywords) {
                final InternationalString i18n = Types.toInternationalString(keyword);
                if (this.keywords == null) {
                    this.keywords = singleton(i18n, InternationalString.class);
                } else {
                    this.keywords.add(i18n);
                }
            }
        }
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Keywords)
     */
    public DefaultKeywords(final Keywords object) {
        super(object);
        if (object != null) {
            keywords      = copyCollection(object.getKeywords(), InternationalString.class);
            type          = object.getType();
            thesaurusName = object.getThesaurusName();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultKeywords}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultKeywords} instance is created using the
     *       {@linkplain #DefaultKeywords(Keywords) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultKeywords castOrCopy(final Keywords object) {
        if (object == null || object instanceof DefaultKeywords) {
            return (DefaultKeywords) object;
        }
        return new DefaultKeywords(object);
    }

    /**
     * Returns commonly used word(s) or formalised word(s) or phrase(s) used to describe the subject.
     *
     * @return word(s) or phrase(s) used to describe the subject.
     */
    @Override
    @XmlElement(name = "keyword", required = true)
    public Collection<InternationalString> getKeywords() {
        return keywords = nonNullCollection(keywords, InternationalString.class);
    }

    /**
     * Sets commonly used word(s) or formalised word(s) or phrase(s) used to describe the subject.
     *
     * @param newValues  the new keywords.
     */
    public void setKeywords(final Collection<? extends InternationalString> newValues) {
        keywords = writeCollection(newValues, keywords, InternationalString.class);
    }

    /**
     * Returns the subject matter used to group similar keywords.
     *
     * @return subject matter used to group similar keywords, or {@code null}.
     */
    @Override
    @XmlElement(name = "type")
    public KeywordType getType() {
        return type;
    }

    /**
     * Sets the subject matter used to group similar keywords.
     *
     * @param newValue  the new keyword type.
     */
    public void setType(final KeywordType newValue) {
        checkWritePermission(type);
        type = newValue;
    }

    /**
     * Returns the name of the formally registered thesaurus or a similar authoritative source of keywords.
     *
     * @return name of registered thesaurus or similar authoritative source of keywords, or {@code null}.
     */
    @Override
    @XmlElement(name = "thesaurusName")
    public Citation getThesaurusName() {
        return thesaurusName;
    }

    /**
     * Sets the name of the formally registered thesaurus or a similar authoritative source of keywords.
     *
     * @param newValue  the new thesaurus name.
     */
    public void setThesaurusName(final Citation newValue) {
        checkWritePermission(thesaurusName);
        thesaurusName = newValue;
    }

    /**
     * Returns the user-defined categorization of groups of keywords that extend or
     * are orthogonal to the standardized {@linkplain #getType() keyword type} codes.
     *
     * @return user-defined categorization of groups of keywords, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "keywordClass")
    @XmlJavaTypeAdapter(MD_KeywordClass.Since2014.class)
    public KeywordClass getKeywordClass() {
        return keywordClass;
    }

    /**
     * Sets the user-defined categorization of groups of keywords.
     *
     * @param newValue  new user-defined categorization of groups of keywords.
     *
     * @since 0.5
     */
    public void setKeywordClass(final KeywordClass newValue) {
        checkWritePermission(keywordClass);
        keywordClass = newValue;
    }
}
