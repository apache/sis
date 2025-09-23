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
package org.apache.sis.xml.bind.lan;

import java.util.Set;
import java.util.Map;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Locale;
import java.nio.charset.Charset;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.metadata.code.MD_CharacterSetCode;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.internal.shared.CollectionsExt;


/**
 * A {@link Locale} associated to {@link Charset}.
 * This class wraps the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 * This wrapper formats the locale like below:
 *
 * {@snippet lang="xml" :
 *   <lan:locale>
 *     <lan:PT_Locale id="locale-eng">
 *       <lan:language>
 *         <lan:LanguageCode codeList="./resources/Codelists.xml#LanguageCode" codeListValue="eng">eng</lan:LanguageCode>
 *       </lan:language>
 *       <lan:country>
 *         <lan:Country codeList="./resources/Codelists.xml#Country" codeListValue="GB">GB</lan:Country>
 *       </lan:country>
 *       <lan:characterEncoding>
 *         <lan:MD_CharacterSetCode codeList="./resources/Codelists.xml#MD_CharacterSetCode"
 *                 codeListValue="8859part15">8859part15</lan:MD_CharacterSetCode>
 *       </lan:characterEncoding>
 *     </lan:PT_Locale>
 *   </lan:locale>
 * }
 *
 * For an alternative (simpler) format used in the legacy {@code gmd} namespace, see {@link LocaleAdapter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 *
 * @see LanguageCode
 * @see Country
 * @see LocaleAdapter
 */
public final class PT_Locale {
    /**
     * The wrapped locale, for information purpose. This object is not marshalled directly.
     * Instead, it will be decomposed in language and country components in {@link Wrapper}.
     *
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * The attributes wrapped in a {@code "PT_Locale"} element.
     */
    @XmlElement(name = "PT_Locale")
    public Wrapper element;

    /**
     * Wraps the {@code "locale"} attributes in a {@code "PT_Locale"} element.
     */
    @XmlType(name = "PT_Locale_Type", propOrder = {
        "languageCode",         // Legacy ISO 19115:2003
        "language",             // New in ISO 19115:2014
        "country",
        "characterEncoding"
    })
    public static final class Wrapper {
        /**
         * The language code, or {@code null} if none.
         */
        LanguageCode language;

        /**
         * The country code, or {@code null} if none.
         */
        @XmlElement
        public Country country;

        /**
         * The character encoding. If {@code null}, then this property will be set to the encoding of XML file.
         * The specification said:
         *
         * <blockquote>Indeed, an XML file can only support data expressed in a single character set, which is generally
         * declared in the XML file header. Having all the localized strings stored in a single XML file would limit the
         * use of a single character set such as UTF-8. In order to avoid this, the {@code LocalisedCharacterString}
         * class is implemented specifically to allow a by-reference containment of the {@link PT_FreeText#textGroup}
         * property, and the {@code PT_LocaleContainer} is the recommended root element to be instantiated in a
         * dedicated XML file. The localized string related to a given locale can be stored in a corresponding locale
         * container (i.e. XML file) and referenced from the {@link PT_FreeText#textGroup} property instances.
         * </blockquote>
         *
         * @todo Current SIS implementation does not yet support {@code PT_LocaleContainer}.
         */
        @XmlElement(required = true)
        @XmlJavaTypeAdapter(MD_CharacterSetCode.class)
        public Charset characterEncoding;

        /**
         * {@code true} if marshalling an element from the ISO 19115:2003 model,
         * or {@code false} if marshalling an element from the ISO 19115:2014 model.
         */
        private boolean isLegacyMetadata;

        /**
         * Empty constructor for JAXB only.
         */
        public Wrapper() {
        }

        /**
         * Creates a new wrapper for the given locale.
         *
         * @param  locale    the locale to marshal, or {@code null}.
         * @param  encoding  the character set, or {@code null} for defaulting to the encoding of XML document.
         */
        Wrapper(final Locale locale, final Charset encoding) {
            final Context context = Context.current();
            isLegacyMetadata  = Context.isFlagSet(context, Context.LEGACY_METADATA);
            language          = LanguageCode.create(context, locale);
            country           = Country     .create(context, locale);
            characterEncoding = encoding;
        }

        /**
         * Gets the language code for this PT_Locale. Used in ISO 19115:2003 model.
         *
         * @return the ISO language code.
         */
        @XmlElement(name = "languageCode", namespace = LegacyNamespaces.GMD)
        public LanguageCode getLanguageCode() {
            return isLegacyMetadata ? language : null;
        }

        /**
         * Sets the language code for this PT_Locale. Used in ISO 19115:2003 model.
         *
         * @param  newValue  the ISO language code.
         */
        public void setLanguageCode(LanguageCode newValue) {
            language = newValue;
        }

        /**
         * Gets the language code for this PT_Locale. Used in ISO 19115:2014 model.
         *
         * @return the ISO language code.
         */
        @XmlElement(name = "language", required = true)
        public LanguageCode getLanguage() {
            return isLegacyMetadata ? null : language;
        }

        /**
         * Sets the language code for this PT_Locale. Used in ISO 19115:2014 model.
         *
         * @param  newValue  the ISO language code.
         */
        public void setLanguage(LanguageCode newValue) {
            language = newValue;
        }

        /**
         * Invoked by JAXB {@link jakarta.xml.bind.Marshaller} before this object is marshalled to XML.
         * If the {@link #characterEncoding} is not set, then this method set a default value.
         * That default is the encoding of the XML document being written.
         *
         * <div class="note"><b>Note:</b> This is redundant with the encoding declared in the XML header.
         * But the {@code <lan:characterEncoding>} element is mandatory according OGC/ISO schemas.</div>
         *
         * @param  marshaller  the marshaller invoking this method.
         */
        public void beforeMarshal(final Marshaller marshaller) {
            if (characterEncoding == null) {
                final String encoding;
                try {
                    encoding = (String) marshaller.getProperty(Marshaller.JAXB_ENCODING);
                } catch (PropertyException | ClassCastException e) {
                    /*
                     * Should never happen. But if it happen anyway, just let the
                     * characterEncoding unitialized: it will not be marshalled.
                     */
                    Context.warningOccured(Context.current(), PT_Locale.class, "beforeMarshal", e, true);
                    return;
                }
                if (encoding != null) {
                    final Context context = Context.current();
                    characterEncoding = Context.converter(context).toCharset(context, encoding);
                }
            }
        }
    }

    /**
     * Empty constructor for JAXB only.
     */
    public PT_Locale() {
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param  locale  the language and country components of {@code PT_Locale}.
     */
    public PT_Locale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Creates a new wrapper for the given locale and character set.
     *
     * @param  entry  the locale to marshal together with its charset.
     */
    private PT_Locale(final Map.Entry<Locale,Charset> entry) {
        locale  = entry.getKey();
        setCharacterSet(entry.getValue());
    }

    /**
     * Returns the Java locale wrapped by this {@link PT_Locale} instance.
     * This method returns a cached instance if possible.
     *
     * @return the wrapped locale, or {@code null} if none.
     */
    public Locale getLocale() {
        if (locale == null && element != null) {
            locale = Country.getLocale(Context.current(), element.language, element.country, PT_Locale.class);
        }
        return locale;
    }

    /**
     * Returns the character set, or {@code null} if none.
     */
    final Charset getCharacterSet() {
        return (element != null) ? element.characterEncoding : null;
    }

    /**
     * Sets the character set to the given value.
     */
    final void setCharacterSet(final Charset encoding) {
        element = new Wrapper(locale, encoding);
    }

    /**
     * Infers a locale and character set from this wrapper and adds them as an entry in the given map.
     *
     * @param  addTo  the map where to add an entry for the locale and character set.
     * @return whether the given map has been modified.
     */
    final boolean addInto(final Map<Locale,Charset> addTo) {
        final Locale locale = getLocale();
        final Charset encoding = getCharacterSet();
        if (locale != null || encoding != null) {
            // We need a special check if (encoding == null) since put(…) != encoding will not work in that case.
            final boolean wasAbsent = (encoding == null) && !addTo.containsKey(locale);
            return (addTo.put(locale, encoding) != encoding) | wasAbsent;
        }
        return false;
    }

    /**
     * Returns the first element of the given map, or {@code null} if none.
     *
     * @param  locales  the locales and character sets, or {@code null}.
     * @return the first element of the given map, or {@code null}.
     */
    public static PT_Locale first(final Map<Locale,Charset> locales) {
        if (locales != null) {
            final Map.Entry<Locale,Charset> first = CollectionsExt.first(locales.entrySet());
            if (first != null) return new PT_Locale(first);
        }
        return null;
    }

    /**
     * Wraps all elements of the given map in a sequence of {@link PT_Locale}.
     *
     * @param  locales  the locales and character sets, or {@code null}.
     * @return the all elements of the given map, or {@code null} if the given map is null or empty.
     */
    public static Set<PT_Locale> wrap(final Map<Locale,Charset> locales) {
        return (locales != null && !locales.isEmpty()) ? new Sequence(locales) : null;
    }

    /**
     * A set of {@link PT_Locale} instances backed by a {@code Map<Locale,Charset>}.
     * This is used at marshalling and unmarshalling time only.
     */
    private static final class Sequence extends AbstractSet<PT_Locale> {
        /** The languages and character sets. */
        final Map<Locale,Charset> locales;

        /** Creates a new set backed by the given map. */
        Sequence(final Map<Locale,Charset> locales) {
            this.locales = locales;
        }

        /** Returns the number of elements in this set. */
        @Override public int size() {
            return locales.size();
        }

        /** Add the given {@code PT_Locale} in the backing map. */
        @Override public boolean add(final PT_Locale value) {
            return (value != null) && value.addInto(locales);
        }

        /** Returns an iterator over the entries in this set. */
        @Override public Iterator<PT_Locale> iterator() {
            final Iterator<Map.Entry<Locale,Charset>> it = locales.entrySet().iterator();
            return new Iterator<PT_Locale>() {
                @Override public boolean   hasNext() {return it.hasNext();}
                @Override public PT_Locale next()    {return new PT_Locale(it.next());}
                @Override public void      remove()  {it.remove();}
            };
        }
    }
}
