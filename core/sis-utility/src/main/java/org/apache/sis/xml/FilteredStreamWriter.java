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


/**
 * A filter replacing the namespaces used by JAXB by other namespaces to be used in the XML document
 * at marshalling time. This class forwards every method calls to the wrapped {@link XMLStreamWriter},
 * with all {@code namespaceURI} arguments filtered before to be delegated.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class FilteredStreamWriter implements XMLStreamWriter {
    /**
     * Where to write the XML.
     */
    private final XMLStreamWriter out;

    /**
     * The other version to marshall to.
     */
    private final FilterVersion version;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredStreamWriter(final XMLStreamWriter out, final FilterVersion version) {
        this.out     = out;
        this.version = version;
    }

    /**
     * Returns the URI to write in the XML document.
     */
    private String toView(final String uri) {
        final String replacement = version.toView.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartElement(final String localName) throws XMLStreamException {
        out.writeStartElement(localName);
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

    /** Forwards the call verbatim. */
    @Override
    public void writeEmptyElement(final String localName) throws XMLStreamException {
        out.writeEmptyElement(localName);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEndElement() throws XMLStreamException {
        out.writeEndElement();
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEndDocument() throws XMLStreamException {
        out.writeEndDocument();
    }

    /** Forwards the call verbatim. */
    @Override
    public void close() throws XMLStreamException {
        out.close();
    }

    /** Forwards the call verbatim. */
    @Override
    public void flush() throws XMLStreamException {
        out.flush();
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeAttribute(final String localName, final String value) throws XMLStreamException {
        out.writeAttribute(localName, value);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeAttribute(final String prefix, final String namespaceURI,final  String localName,
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

    /** Forwards the call verbatim. */
    @Override
    public void writeComment(final String data) throws XMLStreamException {
        out.writeComment(data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeProcessingInstruction(final String target) throws XMLStreamException {
        out.writeProcessingInstruction(target);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
        out.writeProcessingInstruction(target, data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCData(final String data) throws XMLStreamException {
        out.writeCData(data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        out.writeDTD(dtd);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        out.writeEntityRef(name);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument() throws XMLStreamException {
        out.writeStartDocument();
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
        out.writeStartDocument(version);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        out.writeStartDocument(encoding, version);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        out.writeCharacters(text);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        out.writeCharacters(text, start, len);
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

    /** Forwards the call verbatim. */
    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        return out.getProperty(name);
    }
}
