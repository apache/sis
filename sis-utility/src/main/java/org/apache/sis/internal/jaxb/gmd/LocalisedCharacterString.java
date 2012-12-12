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
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import org.apache.sis.internal.jaxb.MarshalContext;

// Related to JDK7
import org.apache.sis.internal.util.Objects;


/**
 * The {@code <LocalisedCharacterString>} elements nested in a {@code <textGroup>} one.
 * This element contains a string for a given {@linkplain Locale locale}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see TextGroup
 */
final class LocalisedCharacterString {
    /**
     * A prefix to concatenate with the {@linkplain Locale#getISO3Language() language code}
     * in order to get the attribute value specified in ISO-19139 for this elements.
     */
    private static final String LOCALE = "#locale-";

    /**
     * The locale value for this string.
     */
    Locale locale;

    /**
     * The text in the locale of this localized string. JAXB uses this field for formatting
     * the {@code <LocalisedCharacterString>} elements in the XML tree at marshalling-time.
     */
    @XmlValue
    String text;

    /**
     * Empty constructor only used by JAXB.
     */
    public LocalisedCharacterString() {
    }

    /**
     * Constructs a localized string for the given locale and text.
     *
     * @param locale The string language.
     * @param text The string.
     */
    LocalisedCharacterString(final Locale locale, final String text) {
        this.locale = locale;
        this.text = text;
    }

    /**
     * Returns the locale language, as specified by ISO-19139 for
     * {@code <LocalisedCharacterString>} attribute.
     *
     * @return The current locale.
     */
    @XmlAttribute(name = "locale", required = true)
    public String getLocale() {
        if (locale == null) {
            return null;
        }
        final MarshalContext context = MarshalContext.current();
        return LOCALE.concat(MarshalContext.converter(context).toLanguageCode(context, locale));
    }

    /**
     * Sets the locale language, using a string formatted as {@code #locale-xxx},
     * where {@code xxx} are the two or three letters representing the language.
     *
     * @param localeId The new locale.
     */
    public void setLocale(final String localeId) {
        if (localeId != null) {
            final MarshalContext context = MarshalContext.current();
            locale = MarshalContext.converter(context).toLocale(context, localeId.substring(localeId.indexOf('-') + 1));
        } else {
            locale = null;
        }
    }

    /**
     * Returns a hash code value for this string.
     */
    @Override
    public int hashCode() {
        return Objects.hash(locale, text);
    }

    /**
     * Compares this string with the given object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof LocalisedCharacterString)) {
            return false;
        }
        final LocalisedCharacterString that = (LocalisedCharacterString) object;
        return Objects.equals(locale, that.locale) && Objects.equals(text, that.text);
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     * Example:
     *
     * {@preformat
     *   LocalisedCharacterString[#locale-fra, “Un texte”]
     * }
     *
     * @see TextGroup#toString()
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(80)
                .append(getClass().getSimpleName()).append('[').append(getLocale());
        if (text != null) {
            buffer.append(", “").append(text).append('”');
        }
        return buffer.append(']').toString();
    }
}
