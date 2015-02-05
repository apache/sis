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
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The {@code <LocalisedCharacterString>} elements nested in a {@code <textGroup>} one.
 * This element contains a string for a given {@linkplain Locale locale}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see TextGroup
 */
final class LocalisedCharacterString {
    /**
     * A prefix to concatenate with the {@linkplain Locale#getISO3Language() language code}.
     * This is a hack for a common pattern found in the way locales are specified in ISO 19139 files.
     * See <a href="https://issues.apache.org/jira/browse/SIS-137">SIS-137</a> for more information.
     */
    private static final String PREFIX = "#locale-";

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
    LocalisedCharacterString() {
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
     * Returns the locale language for {@code <LocalisedCharacterString>} attribute.
     *
     * @return The current locale.
     * @see <a href="https://issues.apache.org/jira/browse/SIS-137">SIS-137</a>
     */
    @XmlAttribute(name = "locale", required = true)
    public String getLocale() {
        if (locale == null) {
            return null;
        }
        final Context context = Context.current();
        return PREFIX.concat(Context.converter(context).toLanguageCode(context, locale));
    }

    /**
     * Sets the locale language, using a string formatted as {@code #locale-xxx},
     * where {@code xxx} are the two or three letters representing the language.
     *
     * @param localeId The new locale.
     * @see <a href="https://issues.apache.org/jira/browse/SIS-137">SIS-137</a>
     */
    public void setLocale(final String localeId) {
        if (localeId != null) {
            final Context context = Context.current();
            locale = Context.converter(context).toLocale(context, localeId.substring(localeId.indexOf('-') + 1));
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
     * {@preformat text
     *   LocalisedCharacterString[#locale-fra, “Un texte”]
     * }
     *
     * @see TextGroup#toString()
     */
    @Debug
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
