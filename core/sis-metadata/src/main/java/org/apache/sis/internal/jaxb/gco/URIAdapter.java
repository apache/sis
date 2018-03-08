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

import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter wrapping a URI value with a {@code <gcx:FileName>} element.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class URIAdapter extends XmlAdapter<GO_CharacterString, URI> {
    /**
     * Empty constructor for JAXB.
     */
    public URIAdapter() {
    }

    /**
     * Converts a URI read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the wrapper for the URI value, or {@code null}.
     * @return a {@link URI} which represents the URI value, or {@code null}.
     * @throws URISyntaxException if the string is not a valid URI.
     */
    @Override
    public final URI unmarshal(final GO_CharacterString value) throws URISyntaxException {
        final String text = StringAdapter.toString(value);
        if (text != null) {
            final Context context = Context.current();
            return Context.converter(context).toURI(context, text);
        }
        return null;
    }

    /**
     * Converts a {@link URI} to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the URI value, or {@code null}.
     * @return the wrapper for the given URI, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final URI value) {
        if (value != null) {
            final Context context = Context.current();
            final GO_CharacterString wrapper = CharSequenceAdapter.wrap(context, value, value.toString());
            if (wrapper != null && wrapper.type == 0) {
                if (!Context.isFlagSet(context, Context.SUBSTITUTE_FILENAME)) {
                    wrapper.type = GO_CharacterString.FILENAME;
                }
                return wrapper;
            }
        }
        return null;
    }

    /**
     * Replace {@code <gcx:FileName>} by {@code <gmd:URL>} if marshalling legacy ISO 19139:2007 document.
     */
    public static final class AsURL extends URIAdapter {
        /** Empty constructor used only by JAXB. */
        public AsURL() {
        }

        /**
         * Replaces {@code <gcx:FileName>} by {@code <gmd:URL>} if marshalling legacy ISO 19139:2007 document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_CharacterString marshal(final URI value) {
            final GO_CharacterString wrapper = super.marshal(value);
            if (wrapper != null && wrapper.type == GO_CharacterString.FILENAME && !FilterByVersion.CURRENT_METADATA.accept()) {
                wrapper.type = GO_CharacterString.URL;
            }
            return wrapper;
        }
    }
}
