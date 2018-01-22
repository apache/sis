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
package org.apache.sis.xml;

import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;


/**
 * A filter replacing the namespaces found in a XML document by the namespaces expected by SIS at unmarshalling time.
 * This class forwards every method calls to the wrapped {@link XMLStreamReader}, with all {@code namespaceURI}
 * arguments filtered before being delegated.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
class FilteredStreamReader extends StreamReaderDelegate {
    /**
     * The other version to unmarshal from.
     */
    private final FilterVersion version;

    /**
     * Creates a new filter for the given version of the standards.
     * Use {@link #create(XMLStreamReader, FilterVersion)} instead.
     */
    FilteredStreamReader(final XMLStreamReader in, final FilterVersion version) {
        super(in);
        this.version = version;
    }

    /**
     * Creates a new filter for the given version of the standards.
     */
    static FilteredStreamReader create(final XMLStreamReader in, final FilterVersion version) {
        if (version.manyToOne) {
            return new FilteredStreamResolver(in, version);
        } else {
            return new FilteredStreamReader(in, version);
        }
    }

    /**
     * Converts a JAXB URI to the URI seen by the consumer of this wrapper.
     */
    private String toView(final String uri) {
        return version.toView.getOrDefault(uri, uri);
    }

    /**
     * Converts a URI read from the XML document to the URI to give to JAXB.
     */
    final String toImpl(final String uri) {
        return version.toImpl.getOrDefault(uri, uri);
    }

    /**
     * Converts a name read from the XML document to the name to give to JAXB.
     * The default implementation assumes a simple bijective mapping between the namespace URIs.
     * The {@link FilteredStreamResolver} subclass implements a more complex mapping where the
     * namespace depends on the element (class or attribute) name.
     */
    QName toImpl(QName name) {
        final String namespaceURI = name.getNamespaceURI();
        final String replacement = toImpl(namespaceURI);
        if (replacement != namespaceURI) {                          // Identity checks are okay here.
            name = new QName(namespaceURI, name.getLocalPart(), name.getPrefix());
        }
        return name;
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
        super.require(type, toView(namespaceURI), localName);
    }

    /** Returns the context of the underlying reader wrapped in a filter that converts the namespaces on the fly. */
    @Override
    public NamespaceContext getNamespaceContext() {
        return new FilteredNamespaces(super.getNamespaceContext(), version, true);
    }

    /** Forwards the call, then replaces the namespace URI if needed. */
    @Override
    public QName getName() {
        return toImpl(super.getName());
    }

    /** Forwards the call, then replaces the namespace URI if needed. */
    @Override
    public QName getAttributeName(final int index) {
        return toImpl(super.getAttributeName(index));
    }

    /** Forwards the call, then replaces the returned URI if needed. */
    @Override
    public String getNamespaceURI() {
        return toImpl(super.getNamespaceURI());
    }

    /**
     * Forwards the call, then replaces the returned URI if needed.
     *
     * <b>Note:</b> the index passed to this method is the index of a namespace declaration on the root element.
     * This should not matter as long as each <em>element</em> has the proper namespace URI.
     */
    @Override
    public String getNamespaceURI(int index) {
        return toImpl(super.getNamespaceURI(index));
    }

    /** Forwards the call, then replaces the returned URI if needed. */
    @Override
    public String getNamespaceURI(final String prefix) {
        return toImpl(super.getNamespaceURI(prefix));
    }

    /** Forwards the call, then replaces the returned URI if needed. */
    @Override
    public String getAttributeNamespace(final int index) {
        return toImpl(super.getAttributeNamespace(index));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public String getAttributeValue(final String namespaceUri, final String localName) {
        return super.getAttributeValue(toView(namespaceUri), localName);
    }
}
