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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see LanguageCode
 * @see org.apache.sis.internal.jaxb.code.PT_Locale
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
     * @param  value The adapter for this metadata value.
     * @return A {@linkplain Locale locale} which represents the metadata value.
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
     * @param  value The locale value.
     * @return The adapter for the given locale.
     */
    @Override
    public LanguageCode marshal(final Locale value) {
        return LanguageCode.create(Context.current(), value);
    }
}
