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
package org.apache.sis.internal.jaxb.gco;

import java.util.Locale;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB adapter for XML {@code <GO_CharacterString>} element mapped to {@link String}.
 * This adapter is similar to {@link InternationalStringAdapter}, except that the {@code unmarshal}
 * method needs to localize {@link InternationalString} instances for the locale specified in the
 * current marshaller context.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class StringAdapter extends XmlAdapter<GO_CharacterString, String> {
    /**
     * Empty constructor for JAXB or subclasses.
     */
    protected StringAdapter() {
    }

    /**
     * Returns a string representation of the given character sequence. If the given
     * sequence is an instance of {@link InternationalString}, then the locale from
     * the current unmashalling context is used in order to get a string.
     * If the context is {@code null} or does not specify any locale, then the choice
     * of locale is left to the {@link InternationalString#toString()} implementation.
     *
     * @param  text  the {@code CharSequence} to convert to a {@code String}, or {@code null}.
     * @return the localized representation of the given text, or {@code null} if the text was null.
     *
     * @see org.apache.sis.xml.XML#LOCALE
     */
    public static String toString(final CharSequence text) {
        if (text == null) {
            return null;
        }
        if (text instanceof InternationalString) {
            final Context context = Context.current();
            if (context != null) {
                final Locale locale = context.getLocale();
                if (locale != null) {
                    /*
                     * While Apache SIS accepts null locale, foreigner
                     * implementations are not guaranteed to support null.
                     */
                    return ((InternationalString) text).toString(locale);
                }
            }
        }
        return text.toString();
    }

    /**
     * Returns the string representation of the given {@code GO_CharacterString} for the current locale.
     * The locale is determined by the {@link org.apache.sis.xml.XML#LOCALE} property given to the marshaller.
     *
     * @param  value  the wrapper for the value, or {@code null}.
     * @return the string representation of the given text, or {@code null}.
     */
    static String toString(final GO_CharacterString value) {
        return (value != null) ? toString(value.toCharSequence()) : null;
    }

    /**
     * Converts a string read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time. If the character
     * sequence is an instance of {@link InternationalString}, then the locale from
     * the current unmashalling context is used in order to get a string.
     *
     * @param  value  the wrapper for the value, or {@code null}.
     * @return the unwrapped {@link String} value, or {@code null}.
     */
    @Override
    public String unmarshal(final GO_CharacterString value) {
        return toString(value);
    }

    /**
     * Converts a {@linkplain String string} to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the string value, or {@code null}.
     * @return the wrapper for the given string, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final String value) {
        return CharSequenceAdapter.wrap(Context.current(), value, value);
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends StringAdapter {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_CharacterString marshal(final String value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
