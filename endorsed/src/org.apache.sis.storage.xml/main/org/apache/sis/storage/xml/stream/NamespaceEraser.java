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
package org.apache.sis.storage.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.xml.internal.shared.StreamWriterDelegate;


/**
 * A filter replacing the given namespace by the default namespace.
 * This is used for removing unnecessary namespace declarations introduced by JAXB marshaller.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class NamespaceEraser extends StreamWriterDelegate {
    /**
     * The default namespace.
     */
    private final String defaultNamespace;

    /**
     * Creates a new filter for the given default namespace.
     */
    NamespaceEraser(final XMLStreamWriter out, final String namespaceURI) {
        super(out);
        defaultNamespace = namespaceURI;
    }

    /**
     * Returns {@code true} if the given namespace is the default one.
     */
    private boolean isDefault(final String ns) {
        return defaultNamespace.equals(ns);
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        if (isDefault(namespaceURI)) {
            out.writeStartElement(localName);
        } else {
            out.writeStartElement(namespaceURI, localName);
        }
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeStartElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException
    {
        if (isDefault(namespaceURI)) {
            out.writeStartElement(localName);
        } else {
            out.writeStartElement(prefix, localName, namespaceURI);
        }
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        if (isDefault(namespaceURI)) {
            out.writeEmptyElement(localName);
        } else {
            out.writeEmptyElement(namespaceURI, localName);
        }
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI)
            throws XMLStreamException
    {
        if (isDefault(namespaceURI)) {
            out.writeEmptyElement(localName);
        } else {
            out.writeEmptyElement(prefix, localName, namespaceURI);
        }
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName,
            final String value) throws XMLStreamException
    {
        if (isDefault(namespaceURI)) {
            out.writeAttribute(localName, value);
        } else {
            out.writeAttribute(prefix, namespaceURI, localName, value);
        }
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final String value)
            throws XMLStreamException
    {
        if (isDefault(namespaceURI)) {
            out.writeAttribute(localName, value);
        } else {
            out.writeAttribute(namespaceURI, localName, value);
        }
    }

    /** Do nothing if the given namespace is the default one. */
    @Override
    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        if (!isDefault(namespaceURI)) {
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                throw new XMLStreamException(namespaceURI);         // Do not allow change of default namespace.
            }
            out.writeNamespace(prefix, namespaceURI);
        }
    }

    /** Do nothing if the given namespace is the default one. */
    @Override
    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        if (!isDefault(namespaceURI)) {
            throw new XMLStreamException(namespaceURI);             // Do not allow change of default namespace.
        }
    }

    /** Do nothing if the given namespace is the default one. */
    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        if (!isDefault(uri)) {
            throw new XMLStreamException(uri);                      // Do not allow change of default namespace.
        }
    }
}
