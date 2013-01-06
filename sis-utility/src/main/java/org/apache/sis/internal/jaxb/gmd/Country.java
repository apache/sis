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
package org.apache.sis.internal.jaxb.gmd;

import java.util.Locale;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.sis.util.Locales;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.jaxb.MarshalContext;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;
import org.apache.sis.internal.jaxb.gco.CharSequenceAdapter;


/**
 * JAXB wrapper for {@link Locale}, in order to integrate the value in an element respecting
 * the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * <p>This adapter formats the locale like below:</p>
 *
 * {@preformat xml
 *   <gmd:country>
 *     <gmd:Country codeList="http://(...snip...)" codeListValue="FR">France</gmd:Country>
 *   </gmd:country>
 * }
 *
 * Note that {@code <gco:CharacterString>} can be substituted to the country code.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
@XmlType(name = "Country_PropertyType")
public final class Country extends GO_CharacterString {
    /**
     * The country using a {@link CodeList}-like format.
     */
    @XmlElement(name = "Country")
    private CodeListProxy proxy;

    /**
     * Empty constructor for JAXB only.
     */
    public Country() {
    }

    /**
     * Builds a {@code <gco:CharacterString>} element.
     * For private use by {@link #create(MarshalContext, Locale)} only.
     */
    private Country(final GO_CharacterString code) {
        super(code);
    }

    /**
     * Builds a {@code <Country>} element.
     * For private use by {@link #create(MarshalContext, Locale, CharSequenceAdapter)} only.
     *
     * @param context       The current (un)marshalling context, or {@code null} if none.
     * @param codeListValue The {@code codeListValue} attribute in the XML element.
     * @param codeSpace     The 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value         The value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    private Country(final MarshalContext context, final String codeListValue, final String codeSpace, final String value) {
        proxy = new CodeListProxy(context, "ML_gmxCodelists.xml", "Country", codeListValue, codeSpace, value);
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param locale  The value to marshal, or {@code null}.
     * @return The country to marshal, or {@code null} if the given locale was null
     *         or if its {@link Locale#getCountry()} attribute is the empty string.
     */
    static Country create(final MarshalContext context, final Locale locale) {
        if (locale != null) {
            final String codeListValue = MarshalContext.converter(context).toCountryCode(context, locale);
            if (!codeListValue.isEmpty() && MarshalContext.isFlagSet(context, MarshalContext.SUBSTITUTE_COUNTRY)) {
                /*
                 * Marshal the locale as a <gco:CharacterString> instead than <Country>,
                 * using the user-supplied anchors if any.
                 */
                final GO_CharacterString string = CharSequenceAdapter.wrap(locale, codeListValue);
                if (string != null) {
                    return new Country(string);
                }
            }
            String codeSpace = null;
            String value = null;
            if (context != null) {
                final Locale marshalLocale = context.getLocale();
                if (marshalLocale != null) {
                    codeSpace = MarshalContext.converter(context).toLanguageCode(context, locale);
                    value = locale.getDisplayCountry(marshalLocale);
                    if (value.isEmpty()) {
                        value = null;
                    }
                }
            }
            if (!codeListValue.isEmpty() || value != null) {
                return new Country(context, codeListValue, codeSpace, value);
            }
        }
        return null;
    }

    /**
     * Returns the locale for the given country (which may be null), or {@code null} if none.
     *
     * @param value The wrapper for this metadata value.
     * @return A locale which represents the metadata value.
     *
     * @see LanguageCode#getLocale(MarshalContext, LanguageCode, boolean)
     */
    static Locale getLocale(final Country value) {
        if (value != null) {
            String code = null;
            if (value.proxy != null) {
                code = value.proxy.codeListValue;
            }
            // If the country was not specified as a code list,
            // look for a simple character string declaration.
            if (code == null) {
                code = value.toString();
            }
            code = CharSequences.trimWhitespaces(code);
            if (code != null && !code.isEmpty()) {
                return Locales.unique(new Locale("", code));
            }
        }
        return null;
    }
}
