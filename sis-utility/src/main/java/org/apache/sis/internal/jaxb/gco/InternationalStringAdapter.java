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
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class InternationalStringAdapter extends XmlAdapter<GO_CharacterString, InternationalString> {
    /**
     * The adapter on which to delegate the marshalling processes.
     */
    private final CharSequenceAdapter adapter;

    /**
     * Empty constructor for JAXB.
     */
    private InternationalStringAdapter() {
        adapter = new CharSequenceAdapter();
    }

    /**
     * Creates a new adapter which will use the anchor map from the given adapter.
     *
     * @param adapter The adaptor on which to delegate the work.
     */
    public InternationalStringAdapter(final CharSequenceAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Converts an object read from a XML stream to an {@link InternationalString} implementation.
     * JAXB invokes automatically this method at unmarshalling time.
     *
     * @param value The adapter for the string value.
     * @return An {@link InternationalString} for the string value.
     */
    @Override
    public InternationalString unmarshal(final GO_CharacterString value) {
        final CharSequence text = adapter.unmarshal(value);
        if (text != null) {
            if (text instanceof InternationalString) {
                return (InternationalString) text;
            }
            return new SimpleInternationalString(text.toString());
        }
        return null;
    }

    /**
     * Converts an {@link InternationalString} to an object to format into a XML stream.
     * JAXB invokes automatically this method at marshalling time.
     *
     * @param  value The string value.
     * @return The adapter for the string.
     */
    @Override
    public GO_CharacterString marshal(final InternationalString value) {
        return adapter.marshal(value);
    }
}
