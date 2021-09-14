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

import org.apache.sis.internal.jaxb.cat.CodeListUID;
import java.util.Locale;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;
import org.apache.sis.internal.jaxb.gco.CharSequenceAdapter;


/**
 * JAXB wrapper for {@link Locale}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * <p>This adapter formats the locale like below (by {@link LocaleAdapter}):</p>
 *
 * {@preformat xml
 *   <gmd:language>
 *     <gmd:LanguageCode codeList="http://(...snip...)" codeListValue="eng">English</gmd:LanguageCode>
 *   </gmd:language>
 * }
 *
 * or (when using {@link PT_Locale} adapter):
 *
 * {@preformat xml
 *   <lan:PT_Locale>
 *     <lan:language>
 *       <lan:LanguageCode codeList="http://(...snip...)" codeListValue="eng">English</lan:LanguageCode>
 *     </lan:language>
 *   </lan:PT_Locale>
 * }
 *
 * Note that {@code <gco:CharacterString>} can be substituted to the language code.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlType(name = "LanguageCode_PropertyType")
public final class LanguageCode extends GO_CharacterString {
    /**
     * The language using a {@link org.opengis.util.CodeList}-like format.
     */
    @XmlElement(name = "LanguageCode")
    private CodeListUID identifier;

    /**
     * Empty constructor for JAXB only.
     */
    private LanguageCode() {
    }

    /**
     * Builds a {@code <gco:LanguageCode>} element.
     * For private use by {@link #create(Context, Locale)} only.
     */
    private LanguageCode(final CharSequence code) {
        super(code);
    }

    /**
     * Builds a {@code <LanguageCode>} element.
     * For private use by {@link #create(Context, Locale)} only.
     *
     * @param  context        the current (un)marshalling context, or {@code null} if none.
     * @param  codeListValue  the {@code codeListValue} attribute in the XML element.
     * @param  codeSpace      the 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param  value          the value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    private LanguageCode(final Context context, final String codeListValue, final String codeSpace, final String value) {
        identifier = new CodeListUID(context, "LanguageCode", codeListValue, codeSpace, value);
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  locale   the value to marshal, or {@code null}.
     * @return the language to marshal, or {@code null} if the given locale was null
     *         or if its {@link Locale#getLanguage()} attribute is the empty string.
     */
    public static LanguageCode create(final Context context, final Locale locale) {
        final String codeListValue = Context.converter(context).toLanguageCode(context, locale);
        if (codeListValue != null) {
            if (!codeListValue.isEmpty() && Context.isFlagSet(context, Context.SUBSTITUTE_LANGUAGE)) {
                /*
                 * Marshal the locale as a <gco:CharacterString> instead of <LanguageCode>,
                 * using the user-supplied anchors if any.
                 */
                final CharSequence string = CharSequenceAdapter.value(context, locale, codeListValue);
                if (string != null) {
                    return new LanguageCode(string);
                }
            }
            final Locale marshalLocale = marshalLocale(context);
            String codeSpace = Context.converter(context).toLanguageCode(context, marshalLocale);
            String value = locale.getDisplayLanguage(marshalLocale);
            if (value.isEmpty()) {
                codeSpace = null;
                value = null;
            }
            if (!codeListValue.isEmpty() || value != null) {
                return new LanguageCode(context, codeListValue, codeSpace, value);
            }
        }
        return null;
    }

    /**
     * Returns the locale to use at marshalling time, or the default locale if unspecified.
     */
    static Locale marshalLocale(final Context context) {
        if (context != null) {
            final Locale marshalLocale = context.getLocale();
            if (marshalLocale != null) {
                return marshalLocale;
            }
        }
        return Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Returns the language, or {@code null} if none. The language is expected to
     * be a 2- or 3-letters ISO 639 code, but this is not verified by this method.
     *
     * @return the language code
     */
    public String getLanguage() {
        String code;
        if (identifier != null) {
            /*
             * <gmd:language>
             *   <gmd:LanguageCode codeList="(snip)#LanguageCode" codeListValue="jpn">Japanese</gmd:LanguageCode>
             * </gmd:language>
             */
            code = identifier.toString(); // May still be null.
        } else {
            /*
             * <gmd:language>
             *   <gco:CharacterString>jpn</gco:CharacterString>
             * </gmd:language>
             */
            code = toString(); // May still be null.
        }
        /*
         * Do not trim whitespaces. We leave that decision to ValueConverter.
         * The default implementation of ValueConverter does that.
         */
        return code;
    }
}
