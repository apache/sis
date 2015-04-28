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
package org.apache.sis.internal.jaxb.code;

import java.util.Locale;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.identification.CharacterSet;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gmd.Country;
import org.apache.sis.internal.jaxb.gmd.LanguageCode;


/**
 * JAXB adapter for {@link Locale}, in order to integrate the value in an element respecting
 * the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * <p>This adapter formats the locale like below:</p>
 *
 * {@preformat xml
 *   <gmd:locale>
 *     <gmd:PT_Locale id="locale-eng">
 *       <gmd:languageCode>
 *         <gmd:LanguageCode codeList="./resources/Codelists.xml#LanguageCode" codeListValue="eng">eng</gmd:LanguageCode>
 *       </gmd:languageCode>
 *       <gmd:country>
 *         <gmd:Country codeList="./resources/Codelists.xml#Country" codeListValue="GB">GB</gmd:Country>
 *       </gmd:country>
 *       <gmd:characterEncoding>
 *         <gmd:MD_CharacterSetCode codeList="./resources/Codelists.xml#MD_CharacterSetCode"
 *                 codeListValue="8859part15">8859part15</gmd:MD_CharacterSetCode>
 *       </gmd:characterEncoding>
 *     </gmd:PT_Locale>
 *   </gmd:locale>
 * }
 *
 * For an alternative (simpler) format, see {@link org.apache.sis.internal.jaxb.gmd.LocaleAdapter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see LanguageCode
 * @see Country
 * @see org.apache.sis.internal.jaxb.gmd.LocaleAdapter
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
    @XmlType(name = "PT_Locale", propOrder = { "languageCode", "country", "characterEncoding" })
    private static final class Wrapper {
        /**
         * The language code, or {@code null} if none.
         */
        @XmlElement(required = true)
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
         * use of a single character set such as UTF-8. In order to avoid this, the {@link LocalisedCharacterString}
         * class is implemented specifically to allow a by-reference containment of the {@link PT_FreeText#textGroup}
         * property, and the {@link PT_LocaleContainer} is the recommended root element to be instantiated in a
         * dedicated XML file. The localized string related to a given locale can be stored in a corresponding locale
         * container (i.e. XML file) and referenced from the {@link PT_FreeText#textGroup} property instances.
         * </blockquote>
         *
         * @todo Current SIS implementation does not yet support {@code PT_LocaleContainer}.
         */
        @XmlElement(required = true)
        @XmlJavaTypeAdapter(MD_CharacterSetCode.class)
        CharacterSet characterEncoding;

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
            languageCode = LanguageCode.create(context, locale);
            country      = Country     .create(context, locale);
            // The characterEncoding field will be initialized at marshalling time (see method below).
        }

        /**
         * Invoked by JAXB {@link javax.xml.bind.Marshaller} before this object is marshalled to XML.
         * This method sets the {@link #characterEncoding} to the XML encoding.
         *
         * <div class="note"><b>Note:</b> This is totally redundant with the encoding declared in the XML header.
         * Unfortunately, the {@code <gmd:characterEncoding>} element is mandatory according OGC/ISO schemas.</div>
         */
        public void beforeMarshal(final Marshaller marshaller) {
            final String encoding;
            try {
                encoding = (String) marshaller.getProperty(Marshaller.JAXB_ENCODING);
            } catch (Exception e) { // (PropertyException | ClassCastException) on the JDK7 branch.
                // Should never happen. But if it happen anyway, just let the
                // characterEncoding unitialized: it will not be marshalled.
                Context.warningOccured(Context.current(), PT_Locale.class, "beforeMarshal", e, true);
                return;
            }
            if (encoding != null) {
                characterEncoding = Types.forCodeName(CharacterSet.class, encoding, true);
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
     * @param value The locale value.
     * @return The wrapper for the locale value.
     */
    @Override
    public PT_Locale marshal(final Locale value) {
        return (value != null) ? new PT_Locale(value) : null;
    }

    /**
     * Substitutes the wrapped value read from a XML stream by the object which will
     * contains the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The wrapper for this metadata value.
     * @return A locale which represents the metadata value.
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
