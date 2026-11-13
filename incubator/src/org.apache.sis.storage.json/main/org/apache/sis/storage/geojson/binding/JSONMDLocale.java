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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import org.apache.sis.storage.json.DataTransferObject;
import org.apache.sis.xml.ValueConverter;

/**
 * Binding of the {@code PT_Locale} JSON type, a language with its country and character encoding.
 *
 * <p>SIS stores locales as a map from {@code Locale} to {@code Charset}, where the first entry is the default locale.
 *
 * <p>Constraints:
 * <ul>
 *   <li>the language is written as an ISO 639-2 three letters code when available,</li>
 *   <li>the character encoding is written as an ISO 19115 {@code MD_CharacterSetCode} (e.g. {@code utf8}).</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#PT_Locale</a>
 */
@JsonPropertyOrder({
    JSONMDLocale.PROPERTY_LANGUAGE,
    JSONMDLocale.PROPERTY_COUNTRY,
    JSONMDLocale.PROPERTY_CHARACTER_ENCODING
})
public final class JSONMDLocale extends DataTransferObject {

    public static final String PROPERTY_LANGUAGE = "language";
    public static final String PROPERTY_COUNTRY = "country";
    public static final String PROPERTY_CHARACTER_ENCODING = "characterEncoding";

    @JsonProperty(PROPERTY_LANGUAGE)
    private String language;
    @JsonProperty(PROPERTY_COUNTRY)
    private String country;
    @JsonProperty(PROPERTY_CHARACTER_ENCODING)
    private String characterEncoding;

    public JSONMDLocale() {
    }

    /**
     * Creates the binding of a locale and its character encoding.
     *
     * @param locale   the locale, or {@code null}.
     * @param encoding the character encoding, or {@code null}.
     */
    public JSONMDLocale(final Locale locale, final Charset encoding) {
        if (locale != null) {
            String code;
            try {
                code = ValueConverter.DEFAULT.toLanguageCode(null, locale);
            } catch (MissingResourceException e) {
                code = locale.getLanguage().isEmpty() ? null : locale.getLanguage();
            }
            language = code;
            country  = ValueConverter.DEFAULT.toCountryCode(null, locale);
        }
        characterEncoding = ValueConverter.DEFAULT.toCharsetCode(null, encoding);
    }

    /**
     * Creates the bindings of all locales, in iteration order.
     *
     * <p>Returns {@code null} if the map is empty.
     */
    public static List<JSONMDLocale> create(final Map<Locale, Charset> locales) {
        if (locales == null || locales.isEmpty()) {
            return null;
        }
        final List<JSONMDLocale> result = new ArrayList<>(locales.size());
        for (final Map.Entry<Locale, Charset> entry : locales.entrySet()) {
            result.add(new JSONMDLocale(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(final String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * Adds this locale and its character encoding to the given map.
     *
     * <p>Invalid language, country or encoding codes are ignored with a warning.
     */
    public void addTo(final Map<Locale, Charset> target, final JSONMDDecoder decoder) {
        Locale locale = null;
        if (language != null) {
            final String code = (country == null) ? language : language + '_' + country;
            try {
                locale = ValueConverter.DEFAULT.toLocale(null, code);
            } catch (IllformedLocaleException e) {
                decoder.warning("Invalid locale \"" + code + "\": " + e.getMessage());
            }
        }
        Charset encoding = null;
        if (characterEncoding != null) {
            try {
                encoding = ValueConverter.DEFAULT.toCharset(null, characterEncoding);
            } catch (IllegalArgumentException e) {
                decoder.warning("Invalid character encoding \"" + characterEncoding + "\": " + e.getMessage());
            }
        }
        if (locale != null || encoding != null) {
            target.putIfAbsent(locale, encoding);
        }
    }
}
