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
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB adapter for XML {@code <GO_CharacterString>} element mapped to {@link InternationalString}.
 * This adapter is similar to {@link StringAdapter}, except that the {@code unmarshall} method does
 * not need to localize {@code InternationalString} instances for the locale specified in the current
 * marshaller context.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class InternationalStringAdapter extends XmlAdapter<GO_CharacterString, InternationalString> {
    /**
     * Empty constructor for JAXB.
     */
    InternationalStringAdapter() {
    }

    /**
     * Converts an object read from a XML stream to an {@link InternationalString} implementation.
     * JAXB invokes automatically this method at unmarshalling time.
     *
     * @param  value  the wrapper for the value, or {@code null}.
     * @return the unwrapped {@link String} value, or {@code null}.
     */
    @Override
    public final InternationalString unmarshal(final GO_CharacterString value) {
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
     * @param  value  the string value, or {@code null}.
     * @return the wrapper for the given string, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final InternationalString value) {
        return CharSequenceAdapter.wrap(value);
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends InternationalStringAdapter {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_CharacterString marshal(final InternationalString value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
