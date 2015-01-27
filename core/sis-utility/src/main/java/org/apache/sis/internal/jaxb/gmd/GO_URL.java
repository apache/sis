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

import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter wrapping a URI in a {@code <gmd:URL>} element, for ISO-19139 compliance.
 * Note that while this object is called {@code "URL"}, we actually use the {@link URI}
 * Java object.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class GO_URL extends XmlAdapter<GO_URL, URI> {
    /**
     * The URI as a string. We uses a string in order to allow the user
     * to catch potential error at unmarshalling time.
     */
    @XmlElement(name = "URL")
    private String uri;

    /**
     * Empty constructor for JAXB only.
     */
    public GO_URL() {
    }

    /**
     * Builds an adapter for the given URI.
     *
     * @param value The URI to marshal.
     */
    private GO_URL(final URI value) {
        uri = value.toString();
    }

    /**
     * Converts a URI read from a XML stream to the object which will contains
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for this metadata value.
     * @return A {@link URI} which represents the metadata value.
     * @throws URISyntaxException if the given value contains an invalid URI.
     */
    @Override
    public URI unmarshal(final GO_URL value) throws URISyntaxException {
        if (value != null) {
            final Context context = Context.current();
            return Context.converter(context).toURI(context, value.uri);
        }
        return null;
    }

    /**
     * Converts a {@link URI} to the object to be marshalled in a XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param value The URI value.
     * @return The adapter for this URI.
     */
    @Override
    public GO_URL marshal(final URI value) {
        return (value != null) ? new GO_URL(value) : null;
    }
}
