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

import java.util.Locale;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.cat.CodeListUID;
import org.apache.sis.xml.bind.gco.GO_CharacterString;
import org.apache.sis.xml.bind.gco.CharSequenceAdapter;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;


/**
 * JAXB wrapper for {@link Locale}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * <p>This adapter formats the locale like below:</p>
 *
 * {@snippet lang="xml" :
 *   <cit:country>
 *     <lan:Country codeList="http://(...snip...)" codeListValue="FR">France</lan:Country>
 *   </cit:country>
 * }
 *
 * Note that {@code <gco:CharacterString>} can be substituted to the country code.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@XmlType(name = "Country_PropertyType")
public final class Country extends GO_CharacterString {
    /**
     * The country using a {@link org.opengis.util.CodeList}-like format.
     * This was called "Country" in ISO 19139:2007 and has been renamed "CountryCode" in ISO 19115-3
     */
    private CodeListUID identifier;

    /**
     * {@code true} if marshalling ISO 19139:2007, or {@code false} if marshalling ISO 19115-3.
     */
    private boolean isLegacyMetadata;

    /**
     * Empty constructor for JAXB only.
     */
    public Country() {
    }

    /**
     * Builds a {@code <gco:Country>} element.
     * For private use by {@link #create(Context, Locale)} only.
     */
    private Country(final CharSequence code) {
        super(code);
        detectVersion();
    }

    /**
     * Builds a {@code <Country>} element.
     * For private use by {@link #create(Context, Locale)} only.
     *
     * @param context        the current (un)marshalling context, or {@code null} if none.
     * @param codeListValue  the {@code codeListValue} attribute in the XML element.
     * @param codeSpace      the 3-letters language code of the {@code value} attribute, or {@code null} if none.
     * @param value          the value in the language specified by the {@code codeSpace} attribute, or {@code null} if none.
     */
    private Country(final Context context, final String codeListValue, final String codeSpace, final String value) {
        identifier = new CodeListUID(context, "Country", codeListValue, codeSpace, value);
        detectVersion();
    }

    /**
     * Determines if we are marshalling ISO 19139:2007 or ISO 19115-3 documents.
     */
    private void detectVersion() {
        isLegacyMetadata = !FilterByVersion.CURRENT_METADATA.accept();
    }

    /**
     * Gets the value of the Country code using ISO 19139:2007 element name.
     *
     * @return the ISO country code.
     */
    @XmlElement(name = "Country", namespace = LegacyNamespaces.GMD)
    public CodeListUID getCountry() {
        return isLegacyMetadata ? identifier : null;
    }

    /**
     * Sets the value of the Country code in ISO 19139:2007 element name.
     *
     * @param  newValue  the ISO country code.
     */
    public void setCountry(CodeListUID newValue) {
        identifier = newValue;
    }

    /**
     * Gets the value of the Country code using ISO 19115-3 element name.
     *
     * @return the ISO country code.
     */
    @XmlElement(name = "CountryCode")
    public CodeListUID getCountryCode() {
        return isLegacyMetadata ? null : identifier;
    }

    /**
     * Sets the value of the Country code in ISO 19115-3 element name.
     *
     * @param  newValue  the ISO country code.
     */
    public void setCountryCode(CodeListUID newValue) {
        identifier = newValue;
    }

    /**
     * Creates a new wrapper for the given locale.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  locale   the value to marshal, or {@code null}.
     * @return the country to marshal, or {@code null} if the given locale was null
     *         or if its {@link Locale#getCountry()} attribute is the empty string.
     */
    public static Country create(final Context context, final Locale locale) {
        final String codeListValue = Context.converter(context).toCountryCode(context, locale);
        if (codeListValue != null) {
            if (!codeListValue.isEmpty() && Context.isFlagSet(context, Context.SUBSTITUTE_COUNTRY)) {
                /*
                 * Marshal the locale as a <gco:CharacterString> instead of <Country>,
                 * using the user supplied anchors if any.
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
     * @param  context   the current (un)marshalling context, or {@code null} if none.
     * @param  language  the wrapper for the language value.
     * @param  country   the wrapper for the country value.
     * @param  caller    the class which is invoking this method, used only in case of warning.
     * @return a locale which represents the language and country value.
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
            // Note: `CodeListUID.toString()` and `Country.toString()` may return null.
            final String c = Strings.trimOrNull((identifier != null ? identifier : country).toString());
            if (c != null) {
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
