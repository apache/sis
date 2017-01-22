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

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.NamespaceContext;
import org.apache.sis.internal.util.StreamWriterDelegate;


/**
 * A filter replacing the namespaces used by JAXB by other namespaces to be used in the XML document
 * at marshalling time. This class forwards every method calls to the wrapped {@link XMLStreamWriter},
 * with all {@code namespaceURI} arguments filtered before to be delegated.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.8
 * @module
 */
final class FilteredStreamWriter extends StreamWriterDelegate {
    /**
     * The other version to marshal to.
     */
    private final FilterVersion version;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredStreamWriter(final XMLStreamWriter out, final FilterVersion version) {
        super(out);
        this.version = version;
    }

    /**
     * Returns the URI to write in the XML document.
     */
    private String toView(final String uri) {
        final String replacement = version.toView.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        out.writeStartElement(toView(namespaceURI), localName);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeStartElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException
    {
        out.writeStartElement(prefix, localName, toView(namespaceURI));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        out.writeEmptyElement(toView(namespaceURI), localName);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException
    {
        out.writeEmptyElement(prefix, localName, toView(namespaceURI));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName,
            final String value) throws XMLStreamException
    {
        out.writeAttribute(prefix, toView(namespaceURI), localName, value);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final String value)
            throws XMLStreamException
    {
        out.writeAttribute(toView(namespaceURI), localName, value);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        out.writeNamespace(prefix, toView(namespaceURI));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        out.writeDefaultNamespace(toView(namespaceURI));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return out.getPrefix(toView(uri));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        out.setPrefix(prefix, toView(uri));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        out.setDefaultNamespace(toView(uri));
    }

    /** Unwraps the original context and forwards the call. */
    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (context instanceof FilteredNamespaces) {
            context = ((FilteredNamespaces) context).inverse(version);
        } else {
            context = new FilteredNamespaces(context, version, true);
        }
        out.setNamespaceContext(context);
    }

    /** Returns the context of the underlying writer wrapped in a filter that convert the namespaces on the fly. */
    @Override
    public NamespaceContext getNamespaceContext() {
        return new FilteredNamespaces(out.getNamespaceContext(), version, false);
    }
}
