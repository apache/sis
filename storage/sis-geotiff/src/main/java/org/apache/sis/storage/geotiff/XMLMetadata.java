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
package org.apache.sis.storage.geotiff;

import java.util.Iterator;
import java.util.StringJoiner;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.resources.Errors;


/**
 * Supports for metadata encoded in XML inside a GeoTIFF tags.
 * This is a temporary object used only at parsing time.
 * Two TIFF tags are associated to XML data:
 *
 * <ul>
 *   <li>{@code GDAL_METADATA} (A480) stored as ASCII characters.</li>
 *   <li>{@code  GEO_METADATA} (C6DD) stored as bytes with UTF-8 encoding.</li>
 * </ul>
 *
 * {@code GEO_METADATA} is defined by the Defense Geospatial Information Working Group (DGIWG)
 * in the <cite>GeoTIFF Profile for Georeferenced Imagery</cite> standard.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://www.dgiwg.org/dgiwg-standards">DGIWG Standards</a>
 *
 * @since 1.2
 * @module
 */
final class XMLMetadata {
    /**
     * The bytes to decode as an XML document.
     * DGIWG specification mandates UTF-8 encoding.
     */
    private byte[] bytes;

    /**
     * The XML document as string.
     */
    private String string;

    /**
     * Name of the XML element being processed. Used for error message only:
     * this field is left to a non-null value if an exception occurred during XML parsing.
     */
    private String currentElement;

    /**
     * {@code true} if the XML is GDAL metadata. Example:
     *
     * {@preformat xml
     *   <GDALMetadata>
     *     <Item name="acquisitionEndDate">2016-09-08T15:53:00+05:00</Item>
     *     <Item name="acquisitionStartDate">2016-09-08T15:56:00+05:00</Item>
     *   </GDALMetadata>
     * }
     */
    private final boolean isGDAL;

    /**
     * Creates new metadata which will decode the given vector of bytes.
     *
     * @param  reader  the TIFF reader.
     * @param  type    type of the metadata tag to read.
     * @param  count   number of bytes or characters in the value to read.
     * @param  isGDAL  {@code true} if the XML is GDAL metadata.
     */
    XMLMetadata(final Reader reader, final Type type, final long count, final boolean isGDAL) throws IOException {
        this.isGDAL = isGDAL;
        switch (type) {
            case ASCII: {
                final String[] cs = type.readString(reader.input, count, reader.store.encoding);
                switch (cs.length) {
                    case 0:  break;
                    case 1:  string = cs[0]; break;      // Usual case.
                    default: string = String.join(System.lineSeparator(), cs); break;
                }
                break;
            }
            case BYTE:
            case UBYTE: {
                /*
                 * NoSuchElementException, ClassCastException and UnsupportedOperationException
                 * should never happen here because we verified that the vector type is byte.
                 */
                bytes = ((ByteBuffer) type.readVector(reader.input, count).buffer().get()).array();
                break;
            }
        }
    }

    /**
     * Returns {@code true} if the XML document could not be read.
     */
    public boolean isEmpty() {
        return bytes == null && string == null;
    }

    /**
     * Returns the XML document as a character string, or {@code null} if the document could not be read.
     */
    public String toString() {
        if (string == null) {
            if (bytes == null) {
                return null;
            }
            string = new String(bytes, StandardCharsets.UTF_8);
        }
        return string;
    }

    /**
     * Returns a reader for the XML document, or {@code null} if the document could not be read.
     */
    private XMLEventReader toXML() throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        if (bytes != null) {
            return factory.createXMLEventReader(new ByteArrayInputStream(bytes), "UTF-8");
        } else if (string != null) {
            return factory.createXMLEventReader(new StringReader(string));
        } else {
            return null;
        }
    }

    /**
     * A tree-table representation of the XML document contained in the enclosing {@link XMLMetadata}.
     * The root node contains the XML document as a {@linkplain #getUserObject() user object}.
     * It allows JavaFX application to support the "copy to clipboard" operation.
     */
    static final class Root extends DefaultTreeTable.Node {
        /**
         * Column for the name associated to the element.
         */
        private static final TableColumn<CharSequence> NAME = NativeMetadata.NAME;

        /**
         * Column for the value associated to the element.
         */
        private static final TableColumn<Object> VALUE = NativeMetadata.VALUE;

        /**
         * A string representation of the XML document.
         *
         * @see #getUserObject()
         */
        private final String xml;

        /**
         * Returns the XML document as a user object.
         * It allows JavaFX application to support the "copy to clipboard" operation.
         */
        @Override
        public Object getUserObject() {
            return xml;
        }

        /**
         * Converts the XML document to a tree table.
         * This method writes in the {@link NativeMetadata#NAME} and {@link NativeMetadata#VALUE} columns.
         * If an exception occurs during XML parsing, then the node content will be set to the raw XML and
         * the only child will be the {@link Throwable}. The error message will appear as a single line
         * when the tree node values are formatted by {@link Object#toString()}, but the full stack trace
         * is available if the user invokes {@code getValue(NativeMetadata.VALUE)}.
         * It allows GUI applications to provide details if requested.
         *
         * @param  source  the XML document to represent as a tree table.
         * @param  target  where to append this root node.
         * @return {@code true} on success, or {@code false} if the XML document could not be decoded.
         */
        Root(final XMLMetadata source, final DefaultTreeTable.Node parent, final String name) {
            super(parent);
            xml = source.toString();
            source.currentElement = name;
            setValue(NAME, name);
            try {
                final XMLEventReader reader = source.toXML();
                if (reader != null) {
                    while (reader.hasNext()) {
                        final XMLEvent event = reader.nextEvent();
                        if (event.isStartElement()) {
                            source.append(reader, event.asStartElement(), newChild());
                        }
                    }
                    reader.close();
                }
            } catch (XMLStreamException e) {
                getChildren().clear();
                setValue(VALUE, xml);
                final TreeTable.Node child = newChild();
                child.setValue(NAME, Errors.format(Errors.Keys.CanNotRead_1, source.currentElement));
                child.setValue(VALUE, e);       // We want the full throwable, not only its string representation.
            }
            source.currentElement = null;
        }
    }

    /**
     * Converts an XML element and its children to a tree table node.
     * This is used for {@link NativeMetadata} representation.
     *
     * @param  reader   the XML reader with its cursor set after the XML element.
     * @param  element  the XML element to append.
     * @param  node     an initially empty node which is added to the tree.
     */
    private void append(final XMLEventReader reader, final StartElement element, final TreeTable.Node node)
            throws XMLStreamException
    {
        final String previous = currentElement;
        currentElement = element.getName().getLocalPart();
        node.setValue(Root.NAME, currentElement);
        final boolean isItem = isGDAL && currentElement.equals("Item");
        final Iterator<Attribute> attributes = element.getAttributes();
        while (attributes.hasNext()) {
            final Attribute attribute = attributes.next();
            if (attribute.isSpecified()) {
                final String name  = attribute.getName().getLocalPart();
                final String value = attribute.getValue();
                if (isItem && name.equals("name")) {
                    /*
                     * GDAL metadata does not really use of XML schema.
                     * Instead, it is a collection of lines like below:
                     *
                     *   <Item name="acquisitionEndDate">2016-09-08T15:53:00+05:00</Item>
                     *
                     * For more natural tree, we rename the "Item" element using the name
                     * specified by the attribute ("acquisitionEndDate" is above example).
                     */
                    node.setValue(Root.NAME, value);
                } else {
                    final TreeTable.Node child = node.newChild();
                    child.setValue(Root.NAME,  name);
                    child.setValue(Root.VALUE, value);
                }
            }
        }
        final StringJoiner buffer = new StringJoiner("");
        while (reader.hasNext()) {
            final XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                append(reader, event.asStartElement(), node.newChild());
            }
            if (event.isCharacters()) {
                final Characters characters = event.asCharacters();
                if (!characters.isWhiteSpace()) {
                    buffer.add(characters.getData());
                }
            }
            if (event.isEndElement()) {
                break;
            }
        }
        final String value = buffer.toString();
        if (!value.isEmpty()) {
            node.setValue(Root.VALUE, value);
        }
        currentElement = previous;
    }
}
