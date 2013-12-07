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
 * arguments filtered before to be delegated.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class FilteredStreamReader extends StreamReaderDelegate {
    /**
     * The other version to unmarshall from.
     */
    private final FilterVersion version;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredStreamReader(final XMLStreamReader in, final FilterVersion version) {
        super(in);
        this.version = version;
    }

    /**
     * Converts a JAXB URI to the URI seen by the consumer of this wrapper.
     */
    private String toView(final String uri) {
        final String replacement = version.toView.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /**
     * Converts a URI read from the XML document to the URI to give to JAXB.
     */
    private String toImpl(final String uri) {
        final String replacement = version.toImpl.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /**
     * Converts a name read from the XML document to the name to give to JAXB.
     */
    private QName toImpl(QName name) {
        final String namespaceURI = name.getNamespaceURI();
        final String replacement = toImpl(namespaceURI);
        if (replacement != namespaceURI) { // Really identity check.
            name = new QName(namespaceURI, name.getLocalPart(), name.getPrefix());
        }
        return name;
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
        super.require(type, toView(namespaceURI), localName);
    }

    /** Returns the context of the underlying reader wrapped in a filter that convert the namespaces on the fly. */
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

    /** Forwards the call, then replaces the returned URI if needed. */
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
