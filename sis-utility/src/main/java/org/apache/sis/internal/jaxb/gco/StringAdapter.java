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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.MarshalContext;


/**
 * JAXB adapter for XML {@code <GO_CharacterString>} element mapped to {@link String}.
 * This adapter is similar to {@link InternationalStringAdapter}, except that the {@code unmarshall}
 * method needs to localize {@link InternationalString} instances for the locale specified in the
 * current marshaller context.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class StringAdapter extends XmlAdapter<GO_CharacterString, String> {
    /**
     * The adapter on which to delegate the marshalling processes.
     */
    private final CharSequenceAdapter adapter;

    /**
     * Empty constructor for JAXB.
     */
    private StringAdapter() {
        adapter = new CharSequenceAdapter();
    }

    /**
     * Creates a new adapter which will use the anchor map from the given adapter.
     *
     * @param adapter The adaptor on which to delegate the work.
     */
    public StringAdapter(final CharSequenceAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a string representation of the given character sequence. If the given
     * sequence is an instance of {@link InternationalString}, then the locale from
     * the current unmashalling context is used in order to get a string.
     *
     * @param text The text for which to get a string representation, or {@code null}.
     * @return The string representation of the given text, or {@code null}.
     */
    public static String toString(final CharSequence text) {
        if (text != null) {
            if (text instanceof InternationalString) {
                final MarshalContext context = MarshalContext.current();
                return ((InternationalString) text).toString(context != null ? context.getLocale() : null);
            }
            return text.toString();
        }
        return null;
    }

    /**
     * Converts a string read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for this metadata value.
     * @return A {@link String} which represents the metadata value.
     */
    @Override
    public String unmarshal(final GO_CharacterString value) {
        return toString(adapter.unmarshal(value));
    }

    /**
     * Converts a {@linkplain String string} to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param value The string value.
     * @return The adapter for this string.
     */
    @Override
    public GO_CharacterString marshal(final String value) {
        return adapter.marshal(value);
    }
}
