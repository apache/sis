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
package org.apache.sis.xml.internal.shared;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.NamespaceContext;
import org.apache.sis.util.Workaround;


/**
 * Base class for deriving an {@code XMLStreamWriter} filters.
 * By default each method does nothing but call the corresponding method on the wrapped instance.
 *
 * <p>This class is the complement of {@link javax.xml.stream.util.StreamReaderDelegate} provided
 * in standard JDK. For an unknown reason, Java 8 does not provide a {@code StreamWriterDelegate}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see javax.xml.stream.util.StreamReaderDelegate
 */
@Workaround(library = "JDK", version = "1.8")
public class StreamWriterDelegate implements XMLStreamWriter {
    /**
     * Where to write the XML.
     */
    protected final XMLStreamWriter out;

    /**
     * Creates a new filter.
     *
     * @param out  where to write the XML.
     */
    protected StreamWriterDelegate(XMLStreamWriter out) {
        this.out = out;
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        out.writeStartElement(localName);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeStartElement(namespaceURI, localName);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        out.writeStartElement(prefix, localName, namespaceURI);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        out.writeEmptyElement(localName);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeEmptyElement(namespaceURI, localName);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        out.writeEmptyElement(prefix, localName, namespaceURI);
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
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        out.writeAttribute(localName, value);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        out.writeAttribute(prefix, namespaceURI, localName, value);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        out.writeAttribute(namespaceURI, localName, value);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        out.writeNamespace(prefix, namespaceURI);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        out.writeDefaultNamespace(namespaceURI);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeComment(String data) throws XMLStreamException {
        out.writeComment(data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        out.writeProcessingInstruction(target);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        out.writeProcessingInstruction(target, data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCData(String data) throws XMLStreamException {
        out.writeCData(data);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        out.writeDTD(dtd);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        out.writeEntityRef(name);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument() throws XMLStreamException {
        out.writeStartDocument();
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        out.writeStartDocument(version);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        out.writeStartDocument(encoding, version);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        out.writeCharacters(text);
    }

    /** Forwards the call verbatim. */
    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        out.writeCharacters(text, start, len);
    }

    /** Forwards the call verbatim. */
    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return out.getPrefix(uri);
    }

    /** Forwards the call verbatim. */
    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        out.setPrefix(prefix, uri);
    }

    /** Forwards the call verbatim. */
    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        out.setDefaultNamespace(uri);
    }

    /** Forwards the call verbatim. */
    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        out.setNamespaceContext(context);
    }

    /** Forwards the call verbatim. */
    @Override
    public NamespaceContext getNamespaceContext() {
        return out.getNamespaceContext();
    }

    /** Forwards the call verbatim. */
    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return out.getProperty(name);
    }
}
