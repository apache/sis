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

import java.util.Arrays;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.xml.internal.shared.StreamWriterDelegate;


/**
 * Adds indentation to a XML output.
 *
 * <h2>Design note</h2>
 * An alternative approach would have been to provide {@code startIdentation()} and {@code endIndentation()}
 * convenience methods in {@link StaxStreamWriter}, and let subclasses perform their own formatting. It would
 * reduce the need to try to guess some formatting aspects (e.g. whether to format on a single line or not).
 * However, that approach does not integrate very well with JAXB. The {@code Marshaller.JAXB_FORMATTED_OUTPUT}
 * property seems to be ignored when marshalling a fragment using {@code XMLStreamWriter}.
 * Even if that property was supported, we found no way to tell to JAXB to begin the indentation at some level
 * (for taking in account the indentation of the elements containing the fragment to marshal with JAXB).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FormattedWriter extends StreamWriterDelegate {
    /**
     * A predefined number of spaces, used by {@link #indent()} for writing a greater number of
     * spaces in one call to {@link XMLStreamWriter#writeCharacters(char[], int, int)} methods.
     */
    private static final char[] SPACES = new char[12];
    static {
        Arrays.fill(SPACES, ' ');
    }

    /**
     * The line separator to use.
     */
    private final String lineSeparator;

    /**
     * Number of spaces to add for each new indentation level.
     */
    private final int indentation;

    /**
     * The number of spaces to write before next XML start tags. This value is incremented or decremented
     * by the {@link #indentation} value every time a XML start element or end element is encountered.
     */
    private int margin;

    /**
     * {@code true} if the last {@code writeStartElement(…)} method invocation has not yet been followed by a
     * {@code writeEndElement(…)} method invocation. In such case, the start and end tags can be written on
     * the same line.
     */
    private boolean inline;

    /**
     * Creates a new XML writer with indentation.
     *
     * @param  out  where to write the XML.
     */
    FormattedWriter(final XMLStreamWriter out, final int indentation) throws XMLStreamException {
        super(out);
        this.indentation = indentation;
        lineSeparator = System.lineSeparator();
    }

    /**
     * Writes a line separator, then the given number of spaces.
     */
    private void indent() throws XMLStreamException {
        int n = margin;
        out.writeCharacters(lineSeparator);
        final int length = SPACES.length;
        while (n > length) {
            out.writeCharacters(SPACES, 0, length);
            n -= length;
        }
        out.writeCharacters(SPACES, 0, n);
    }

    /**
     * Appends indentation before an empty tag.
     */
    private void emptyIndent() throws XMLStreamException {
        indent();
        inline = false;
    }

    /**
     * Increases the indentation level and appends indentation before a start tag.
     */
    final void startIndent() throws XMLStreamException {
        indent();
        inline = true;
        margin += indentation;
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        startIndent();
        out.writeStartElement(localName);
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        startIndent();
        out.writeStartElement(namespaceURI, localName);
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        startIndent();
        out.writeStartElement(prefix, localName, namespaceURI);
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        emptyIndent();
        out.writeEmptyElement(namespaceURI, localName);
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        emptyIndent();
        out.writeEmptyElement(prefix, localName, namespaceURI);
    }

    /**
     * Appends indentation, then forwards the call verbatim.
     */
    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        emptyIndent();
        out.writeEmptyElement(localName);
    }

    /**
     * Appends indentation if the value is not inline, then forwards the call verbatim.
     */
    @Override
    public void writeEndElement() throws XMLStreamException {
        margin -= indentation;
        if (!inline) indent();
        inline = false;
        out.writeEndElement();
    }

    /**
     * Appends a new line after the document.
     */
    @Override
    public void writeEndDocument() throws XMLStreamException {
        out.writeCharacters(lineSeparator);
        out.writeEndDocument();
        out.writeCharacters(lineSeparator);
    }
}
