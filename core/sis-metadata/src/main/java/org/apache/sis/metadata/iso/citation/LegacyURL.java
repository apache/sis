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
package org.apache.sis.metadata.iso.citation;

import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.LegacyNamespaces;


/**
 * JAXB wrapper for an URI in a {@code <gmd:URL>} element, for ISO 19139 compliance.
 * Note that while this object is called {@code "URL"}, we actually use the {@link URI}
 * Java object.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class LegacyURL {
    /**
     * The URI as a string. We uses a string in order to allow the user
     * to catch potential error at unmarshalling time.
     */
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlElement(name = "URL", namespace = LegacyNamespaces.GMD)
    private String uri;

    /**
     * Empty constructor for JAXB only.
     */
    LegacyURL() {
    }

    /**
     * Builds an adapter for the given URI.
     *
     * @param  value  the URI to marshal.
     */
    private LegacyURL(final URI value) {
        uri = value.toString();
    }

    /**
     * Returns {@code true} if the given value contains a non-null URI.
     */
    static boolean isNonNull(final LegacyURL value) {
        return (value != null) && (value.uri != null);
    }

    /**
     * Converts a {@link URI} to the object to be marshalled in a XML file.
     *
     * @param  value  the URI value.
     * @return the wrapper for the given URI.
     */
    static LegacyURL wrap(final URI value) {
        return (value != null) ? new LegacyURL(value) : null;
    }

    /**
     * Converts an URI read from a XML stream to the object which will contains the value.
     *
     * @return a {@link URI} which represents the metadata value.
     * @throws URISyntaxException if the given value contains an invalid URI.
     */
    URI unwrap() throws URISyntaxException {
        final Context context = Context.current();
        return Context.converter(context).toURI(context, uri);
    }
}
