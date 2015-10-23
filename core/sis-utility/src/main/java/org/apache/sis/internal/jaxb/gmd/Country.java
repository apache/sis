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
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;
import org.apache.sis.internal.jaxb.gco.CharSequenceAdapter;
import org.apache.sis.util.resources.Errors;


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
 * @since   0.3
 * @version 0.4
 * @module
 */
@XmlType(name = "Country_PropertyType")
public final class Country extends GO_CharacterString {
    /**
     * The country using a {@link org.opengis.util.CodeList}-like format.
     */
    @XmlElement(name = "Country")
    private CodeListUID identifier;

    /**
     * Empty constructor for JAXB only.
     */
    private Country() {
    }

    /**
     * Builds a {@code <gco:Country>} element.
     * For private use by {@link #create(Context, Locale)} only.
     */
    private Country(final CharSequence code) {
        super(code);
    }

    /**
     * Builds a {@code <Country>} element.
     * For private use by {@link #create(Context, Locale)} only.
     *
     * @param context       The current (un)marshalling context, or {@code null} if none.
     * @param codeListValue The {@code codeListValue} attribute in the XML element.
     * @param codeSpace     The 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value         The value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    private Country(final Context context, final String codeListValue, final String codeSpace, final String value) {
        identifier = new CodeListUID(context, "Country", codeListValue, codeSpace, value);
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param context The current (un)marshalling context, or {@code null} if none.
     * @param locale  The value to marshal, or {@code null}.
     * @return The country to marshal, or {@code null} if the given locale was null
     *         or if its {@link Locale#getCountry()} attribute is the empty string.
     */
    public static Country create(final Context context, final Locale locale) {
        final String codeListValue = Context.converter(context).toCountryCode(context, locale);
        if (codeListValue != null) {
            if (!codeListValue.isEmpty() && Context.isFlagSet(context, Context.SUBSTITUTE_COUNTRY)) {
                /*
                 * Marshal the locale as a <gco:CharacterString> instead than <Country>,
                 * using the user-supplied anchors if any.
                 */
                final CharSequence string = CharSequenceAdapter.value(context, locale, codeListValue);
                if (string != null) {
                    return new Country(string);
                }
            }
            final Locale marshalLocale = LanguageCode.marshalLocale(context);
            String codeSpace = Context.converter(context).toLanguageCode(context, marshalLocale);
            String value = locale.getDisplayCountry(marshalLocale);
            if (value.isEmpty()) {
                codeSpace = null;
                value = null;
            }
            if (!codeListValue.isEmpty() || value != null) {
                return new Country(context, codeListValue, codeSpace, value);
            }
        }
        return null;
    }

    /**
     * Returns the locale for the given language and country (which may be null), or {@code null} if none.
     *
     * @param  context  The current (un)marshalling context, or {@code null} if none.
     * @param  language The wrapper for the language value.
     * @param  country  The wrapper for the country value.
     * @param  caller   The class which is invoking this method, used only in case of warning.
     * @return A locale which represents the language and country value.
     */
    public static Locale getLocale(final Context context, final LanguageCode language, final Country country,
            final Class<?> caller)
    {
        String code = null;
        if (language != null) {
            code = language.getLanguage();
        }
        if (country != null) {
            final CodeListUID identifier = country.identifier;
            final String c = CharSequences.trimWhitespaces((identifier != null ? identifier : country).toString());
            if (c != null && !c.isEmpty()) {
                if (code == null) {
                    code = "";
                }
                int i = code.indexOf('_');
                if (i < 0) {
                    code = code + '_' + c;
                } else {
                    final int length = code.length();
                    if (++i == code.length() || code.charAt(i) == '_') {
                        code = new StringBuilder().append(code, 0, i).append(c).append(code, i, length).toString();
                    } else if (!c.equals(CharSequences.token(code, i))) {
                        Context.warningOccured(context, caller, "unmarshal", Errors.class,
                                Errors.Keys.IncompatiblePropertyValue_1, "country");
                    }
                }
            }
        }
        return Context.converter(context).toLocale(context, code);
    }
}
