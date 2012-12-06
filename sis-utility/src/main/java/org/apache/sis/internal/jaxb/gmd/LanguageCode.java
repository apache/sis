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

import org.apache.sis.internal.jaxb.MarshalContext;
import org.apache.sis.internal.jaxb.code.CodeListProxy;
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
 *   <gmd:language>
 *     <gmd:LanguageCode codeList="http://(...snip...)" codeListValue="eng">English</gmd:LanguageCode>
 *   </gmd:language>
 * }
 *
 * Note that {@code <gco:CharacterString>} can be substituted to the language code.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
@XmlType(name = "LanguageCode_PropertyType")
public final class LanguageCode extends GO_CharacterString {
    /**
     * The language using a {@link CodeList}-like format.
     */
    @XmlElement(name = "LanguageCode")
    private CodeListProxy proxy;

    /**
     * Empty constructor for JAXB only.
     */
    public LanguageCode() {
    }

    /**
     * Builds a {@code <gco:CharacterString>} element.
     * For private use by {@link #create(MarshalContext, Locale, CharSequenceAdapter)} only.
     */
    private LanguageCode(final GO_CharacterString code) {
        super(code);
    }

    /**
     * Builds a {@code <LanguageCode>} element.
     * For private use by {@link #create(MarshalContext, Locale, CharSequenceAdapter)} only.
     *
     * @param context       The current (un)marshalling context, or {@code null} if none.
     * @param codeListValue The {@code codeListValue} attribute in the XML element.
     * @param codeSpace     The 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value         The value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    private LanguageCode(final MarshalContext context, final String codeListValue, final String codeSpace, final String value) {
        proxy = new CodeListProxy(context, "ML_gmxCodelists.xml", "LanguageCode", codeListValue, codeSpace, value);
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param locale  The value to marshall, or {@code null}.
     * @paral anchors If non-null, marshall the locale as a {@code <gco:CharacterString>} instead
     *                than {@code <LanguageCode>}, using the given anchors if any.
     * @return The language to marshal, or {@code null} if the given locale was null
     *         or if its {@link Locale#getLanguage()} attribute is the empty string.
     */
    static LanguageCode create(final MarshalContext context, final Locale locale, final CharSequenceAdapter anchors) {
        if (locale != null) {
            final String codeListValue = MarshalContext.converter(context).toLanguageCode(context, locale);
            if (anchors != null && !codeListValue.isEmpty()) {
                final GO_CharacterString string = anchors.marshal(codeListValue);
                if (string != null) {
                    return new LanguageCode(string);
                }
            }
            String codeSpace = null;
            String value = null;
            if (context != null) {
                final Locale marshalLocale = context.getLocale();
                if (marshalLocale != null) {
                    codeSpace = MarshalContext.converter(context).toLanguageCode(context, locale);
                    value = locale.getDisplayLanguage(marshalLocale);
                    if (value.isEmpty()) {
                        value = null;
                    }
                }
            }
            if (!codeListValue.isEmpty() || value != null) {
                return new LanguageCode(context, codeListValue, codeSpace, value);
            }
        }
        return null;
    }

    /**
     * Returns the locale for the given language (which may be null), or {@code null} if none.
     *
     * @param value The wrapper for this metadata value.
     * @param useCharSequence Whatever this method should fallback on the
     *        {@code gco:CharacterString} element if no value were specified for the
     *        {@code gml:LanguageCode} element.
     * @return A locale which represents the metadata value.
     *
     * @see Country#getLocale(Country)
     */
    static Locale getLocale(final MarshalContext context, final LanguageCode value, final boolean useCharSequence) {
        if (value != null) {
            final CodeListProxy proxy = value.proxy;
            if (proxy != null) {
                final Locale locale = MarshalContext.converter(context).toLocale(context, proxy.codeListValue);
                if (locale != null) {
                    return locale;
                }
            }
            if (useCharSequence) {
                return MarshalContext.converter(context).toLocale(context, value.toString());
            }
        }
        return null;
    }
}
