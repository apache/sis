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
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * JAXB adapter for XML {@code <GO_CharacterString>} element mapped to {@link InternationalString}.
 * This adapter is similar to {@link StringAdapter}, except that the {@code unmarshall} method does
 * not need to localize {@code InternationalString} instances for the locale specified in the current
 * marshaller context.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class InternationalStringAdapter extends XmlAdapter<GO_CharacterString, InternationalString> {
    /**
     * Empty constructor for JAXB.
     */
    private InternationalStringAdapter() {
    }

    /**
     * Converts an object read from a XML stream to an {@link InternationalString} implementation.
     * JAXB invokes automatically this method at unmarshalling time.
     *
     * @param  value The wrapper for the value, or {@code null}.
     * @return The unwrapped {@link String} value, or {@code null}.
     */
    @Override
    public InternationalString unmarshal(final GO_CharacterString value) {
        if (value != null) {
            final CharSequence text = value.toCharSequence();
            if (text != null) {
                if (text instanceof InternationalString) {
                    return (InternationalString) text;
                }
                return new SimpleInternationalString(text.toString());
            }
        }
        return null;
    }

    /**
     * Converts an {@link InternationalString} to an object to format into a XML stream.
     * JAXB invokes automatically this method at marshalling time.
     *
     * @param  value The string value, or {@code null}.
     * @return The wrapper for the given string, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final InternationalString value) {
        return CharSequenceAdapter.wrap(value);
    }
}
