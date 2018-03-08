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
package org.apache.sis.internal.jaxb.lan;

import java.util.Locale;
import java.nio.charset.Charset;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.internal.jaxb.code.MD_CharacterSetCode;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter for {@link Locale}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * <p>This adapter formats the locale like below:</p>
 *
 * {@preformat xml
 *   <lan:locale>
 *     <lan:PT_Locale id="locale-eng">
 *       <lan:languageCode>
 *         <lan:LanguageCode codeList="./resources/Codelists.xml#LanguageCode" codeListValue="eng">eng</lan:LanguageCode>
 *       </lan:languageCode>
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
 * For an alternative (simpler) format, see {@link LocaleAdapter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 *
 * @see LanguageCode
 * @see Country
 * @see LocaleAdapter
 *
 * @since 0.3
 * @module
 */
public final class PT_Locale extends XmlAdapter<PT_Locale, Locale> {
    /**
     * The attributes wrapped in a {@code "PT_Locale"} element.
     */
    @XmlElement(name = "PT_Locale")
    private Wrapper element;

    /**
     * Wraps the {@code "locale"} attributes in a {@code "PT_Locale"} element.
     */
    @XmlType(name = "PT_Locale_Type", propOrder = {
        "languageCode", "language", "country", "characterEncoding"
    })
    private static final class Wrapper {
        /**
         * The language code, or {@code null} if none.
         */
        LanguageCode languageCode;

        /**
         * The country code, or {@code null} if none.
         */
        @XmlElement
        Country country;

        /**
         * The character encoding. The specification said:
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
        Charset characterEncoding;

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
         */
        Wrapper(final Locale locale) {
            final Context context = Context.current();
            isLegacyMetadata = Context.isFlagSet(context, Context.LEGACY_METADATA);
            languageCode     = LanguageCode.create(context, locale);
            country          = Country     .create(context, locale);
            // The characterEncoding field will be initialized at marshalling time (see method below).
        }

        /**
         * Gets the language code for this PT_Locale. Used in ISO 19115:2003 model.
         */
        @XmlElement(name = "languageCode", namespace = LegacyNamespaces.GMD)
        private LanguageCode getLanguageCode() {
            return isLegacyMetadata ? languageCode : null;
        }

        /**
         * Sets the language code for this PT_Locale. Used in ISO 19115:2003 model.
         */
        @SuppressWarnings("unused")
        private void setLanguageCode(LanguageCode newValue) {
            languageCode = newValue;
        }

        /**
         * Gets the language code for this PT_Locale. Used in ISO 19115-3.
         */
        @XmlElement(name = "language", required = true)
        private LanguageCode getLanguage() {
            return isLegacyMetadata ? null : languageCode;
        }

        /**
         * Sets the language code for this PT_Locale. Used in ISO 19115:2003 model.
         */
        @SuppressWarnings("unused")
        private void setLanguage(LanguageCode newValue) {
            languageCode = newValue;
        }

        /**
         * Invoked by JAXB {@link javax.xml.bind.Marshaller} before this object is marshalled to XML.
         * This method sets the {@link #characterEncoding} to the XML encoding.
         *
         * <div class="note"><b>Note:</b> This is totally redundant with the encoding declared in the XML header.
         * Unfortunately, the {@code <lan:characterEncoding>} element is mandatory according OGC/ISO schemas.</div>
         */
        public void beforeMarshal(final Marshaller marshaller) {
            final String encoding;
            try {
                encoding = (String) marshaller.getProperty(Marshaller.JAXB_ENCODING);
            } catch (PropertyException | ClassCastException e) {
                // Should never happen. But if it happen anyway, just let the
                // characterEncoding unitialized: it will not be marshalled.
                Context.warningOccured(Context.current(), PT_Locale.class, "beforeMarshal", e, true);
                return;
            }
            if (encoding != null) {
                final Context context = Context.current();
                characterEncoding = Context.converter(context).toCharset(context, encoding);
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
     */
    private PT_Locale(final Locale locale) {
        element = new Wrapper(locale);
    }

    /**
     * Substitutes the locale by the wrapper to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the locale value.
     * @return the wrapper for the locale value.
     */
    @Override
    public PT_Locale marshal(final Locale value) {
        return (value != null) ? new PT_Locale(value) : null;
    }

    /**
     * Substitutes the wrapped value read from a XML stream by the object which will
     * contains the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the wrapper for this metadata value.
     * @return a locale which represents the metadata value.
     */
    @Override
    public Locale unmarshal(final PT_Locale value) {
        if (value != null) {
            final Wrapper element = value.element;
            if (element != null) {
                return Country.getLocale(Context.current(), element.languageCode, element.country, PT_Locale.class);
            }
        }
        return null;
    }
}
