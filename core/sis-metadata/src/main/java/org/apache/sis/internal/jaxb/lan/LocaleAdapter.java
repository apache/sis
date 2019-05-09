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

import java.util.Locale;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter for XML {@code <GO_CharacterString>} or {@code <LanguageCode>} elements
 * mapped to {@link Locale}. This adapter formats the locale like below:
 *
 * {@preformat xml
 *   <gmd:language>
 *     <gmd:LanguageCode codeList="(snip)#LanguageCode" codeListValue="jpn">Japanese</gmd:LanguageCode>
 *   </gmd:language>
 * }
 *
 * This adapter is used for legacy locales in {@code gmd} namespace.
 * For locales in the newer {@code lan} namespace, see {@link PT_Locale}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 *
 * @see LanguageCode
 * @see PT_Locale
 *
 * @since 0.3
 * @module
 */
public final class LocaleAdapter extends XmlAdapter<LanguageCode, Locale> {
    /**
     * Empty constructor for JAXB.
     */
    private LocaleAdapter() {
    }

    /**
     * Converts the locale read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the adapter for this metadata value.
     * @return a {@linkplain Locale locale} which represents the metadata value.
     */
    @Override
    public Locale unmarshal(final LanguageCode value) {
        final Context context = Context.current();
        return Context.converter(context).toLocale(context, value.getLanguage());
    }

    /**
     * Converts the {@linkplain Locale locale} to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the locale value.
     * @return the adapter for the given locale.
     */
    @Override
    public LanguageCode marshal(final Locale value) {
        return LanguageCode.create(Context.current(), value);
    }




    /**
     * JAXB adapter for XML {@code <PT_Locale>} elements mapped to {@link Locale}.
     * This adapter formats the locale like below:
     *
     * {@preformat xml
     *   <gmd:locale>
     *     <gmd:PT_Locale>
     *       <gmd:language>
     *         <gmd:LanguageCode codeList="(snip)#LanguageCode" codeListValue="jpn">Japanese</gmd:LanguageCode>
     *       </gmd:language>
     *     </gmd:PT_Locale>
     *   </gmd:locale>
     * }
     *
     * This adapter is used for legacy locales in {@code gmd} namespace.
     * For locales in the newer {@code lan} namespace, see {@link PT_Locale}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    public static final class Wrapped extends XmlAdapter<PT_Locale, Locale> {
        /**
         * Empty constructor for JAXB.
         */
        private Wrapped() {
        }

        /**
         * Substitutes the locale by the wrapper to be marshalled into an XML file
         * or stream. JAXB calls automatically this method at marshalling time.
         *
         * @param  value  the locale value.
         * @return the wrapper for the locale value.
         */
        @Override
        public PT_Locale marshal(final Locale value) {
            if (value == null) {
                return null;
            }
            PT_Locale p = new PT_Locale(value);
            p.setCharacterSet(null);                // For forcing creation of child `element`.
            return p;
        }

        /**
         * Substitutes the wrapped value read from a XML stream by the object which will
         * contains the value. JAXB calls automatically this method at unmarshalling time.
         *
         * @param  value  the wrapper for this metadata value.
         * @return a locale which represents the metadata value.
         */
        @Override
        public Locale unmarshal(final PT_Locale value) {
            return (value != null) ? value.getLocale() : null;
        }
    }
}
