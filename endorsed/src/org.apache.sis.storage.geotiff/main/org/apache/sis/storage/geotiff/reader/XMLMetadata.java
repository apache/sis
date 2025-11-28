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
package org.apache.sis.storage.geotiff.reader;

import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.stax.StAXSource;
import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.XML;


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
 * in the <i>GeoTIFF Profile for Georeferenced Imagery</i> standard.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://www.dgiwg.org/dgiwg-standards">DGIWG Standards</a>
 */
public final class XMLMetadata implements Filter {
    /**
     * The {@value} string, used in GDAL metadata.
     */
    private static final String ITEM = "Item", NAME = "name";

    /**
     * The bytes to decode as an XML document.
     * <abbr>DGIWG</abbr> specification mandates UTF-8 encoding.
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
     * Where to report non-fatal warnings.
     */
    private final StoreListeners listeners;

    /**
     * {@code true} if the XML is GDAL metadata. Example:
     *
     * {@snippet lang="xml" :
     *   <GDALMetadata>
     *     <Item name="acquisitionEndDate">2016-09-08T15:53:00+05:00</Item>
     *     <Item name="acquisitionStartDate">2016-09-08T15:56:00+05:00</Item>
     *   </GDALMetadata>
     *   }
     */
    private final boolean isGDAL;

    /**
     * The next metadata in a list of linked metadata. Should always be {@code null},
     * but we nevertheless define this field in case a file defines more than one
     * {@code GEO_METADATA} or {@code GDAL_METADATA} tags.
     */
    private XMLMetadata next;

    /**
     * Creates a new instance with the given XML. Used for testing purposes.
     */
    XMLMetadata(final String xml, final boolean isGDAL) {
        this.isGDAL = isGDAL;
        this.string = xml;
        listeners   = null;
    }

    /**
     * Creates new metadata which will decode the given vector of bytes.
     *
     * @param  input      the input channel from which to read the tag.
     * @param  encoding   the encoding of characters (usually US ASCII).
     * @param  listeners  where to report warnings.
     * @param  type       type of the metadata tag to read.
     * @param  count      number of bytes or characters in the value to read.
     * @param  tag        the tag where the metadata was stored.
     * @throws IOException if an error occurred while reading the TIFF tag content.
     */
    public XMLMetadata(final ChannelDataInput input, final Charset encoding, final StoreListeners listeners,
                       final Type type, final long count, final short tag) throws IOException
    {
        this.listeners = listeners;
        isGDAL = (tag == Tags.GDAL_METADATA);
        switch (type) {
            case ASCII: {
                final String[] cs = type.readAsStrings(input, count, encoding);
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
                bytes = ((ByteBuffer) type.readAsVector(input, count).buffer().get()).array();
                break;
            }
        }
    }

    /**
     * Appends this metadata at the end of a linked list starting with the given element.
     * This method is inefficient because it iterates over all elements for reaching the tail,
     * but it should not be an issue because this method is invoked only in the unlikely case
     * where a file would define more than one {@code *_METADATA} tag.
     *
     * @param  head  first element of the linked list where to append this metadata.
     */
    final void appendTo(XMLMetadata head) {
        while (head.next != null) {
            head = head.next;
        }
        head.next = this;
    }

    /**
     * Returns the name of the tag from which the XML has been read.
     * This is used for error messages.
     */
    String tag() {
        return Tags.name(isGDAL ? Tags.GDAL_METADATA : Tags.GEO_METADATA);
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
    @Override
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
    public static final class Root extends DefaultTreeTable.Node {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -3656784393688796818L;

        /**
         * Column for the name associated to the element.
         * Should be same as {@code NativeMetadata.NAME}.
         */
        private static final TableColumn<CharSequence> NAME = TableColumn.NAME;

        /**
         * Column for the value associated to the element.
         * Should be same as {@code NativeMetadata.VALUE}.
         */
        private static final TableColumn<Object> VALUE = TableColumn.VALUE;

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
         * @param  name    name to assign to this root node.
         * @return {@code true} on success, or {@code false} if the XML document could not be decoded.
         */
        public Root(final XMLMetadata source, final DefaultTreeTable.Node parent, final String name) {
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
        final boolean isItem = isGDAL && currentElement.equals(ITEM);
        final Iterator<Attribute> attributes = element.getAttributes();
        while (attributes.hasNext()) {
            final Attribute attribute = attributes.next();
            if (attribute.isSpecified()) {
                final String name  = attribute.getName().getLocalPart();
                final String value = attribute.getValue();
                if (isItem && name.equals(NAME)) {
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
        final var buffer = new StringJoiner("");
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

    /**
     * Appends the content of this object to the given metadata builder.
     *
     * @param  metadata  the builder where to append the content of this {@code XMLMetadata}.
     * @throws XMLStreamException if an error occurred while parsing the XML.
     * @throws JAXBException if an error occurred while parsing the XML.
     * @return the next metadata in a linked list of metadata, or {@code null} if none.
     */
    public XMLMetadata appendTo(final MetadataBuilder metadata) throws XMLStreamException, JAXBException {
        final XMLEventReader reader = toXML();
        if (reader != null) {
            if (isGDAL) {
                /*
                 * We expect a list of XML elements as below:
                 *
                 *   <Item name="acquisitionEndDate">2016-09-08T15:53:00+05:00</Item>
                 *
                 * Those items should be children of <GDALMetadata> node. That node should be the root node,
                 * but current implementation searches <GDALMetadata> recursively if not found at the root.
                 */
                final Parser parser = new Parser(reader, metadata);
                while (reader.hasNext()) {
                    final XMLEvent event = reader.nextEvent();
                    if (event.isStartElement()) {
                        parser.root(event.asStartElement());
                    }
                }
                parser.flush();
            } else {
                /*
                 * Parse as an ISO 19115 document and get the content as a `Metadata` object.
                 * Some other types are accepted as well (e.g. `IdentificationInfo`).
                 * The `mergeMetadata` method applies heuristic rules for adding components.
                 */
                metadata.mergeMetadata(XML.unmarshal(new StAXSource(reader),
                        Map.of(XML.WARNING_FILTER, this)),
                        (listeners != null) ? listeners.getLocale() : null);
            }
            reader.close();     // No need to close the underlying input stream.
        }
        return next;
    }

    /**
     * Parser of GDAL metadata.
     */
    private static final class Parser {
        /** The XML reader from which to get XML elements. */
        private final XMLEventReader reader;

        /** A qualified name with the {@value #NAME} local part, used for searching attributes. */
        private final QName name;

        /** A value increased for each level of nested {@code <Item>} element. */
        private int depth;

        /** Where to write metadata. */
        private final MetadataBuilder metadata;

        /** Temporary storage for metadata values that need a little processing. */
        private Instant startTime, endTime;

        /**
         * Creates a new reader.
         *
         * @param reader     the source of XML elements.
         * @param metadata   the target of metadata elements.
         */
        Parser(final XMLEventReader reader, final MetadataBuilder metadata) {
            this.reader = reader;
            this.metadata = metadata;
            name = new QName(NAME);
        }

        /**
         * Parses a {@code <GDALMetadata>} element and its children. After this method returns,
         * the reader is positioned after the closing {@code </GDALMetadata>} tag.
         *
         * @param  start  the {@code <GDALMetadata>} element.
         */
        void root(final StartElement start) throws XMLStreamException {
            final boolean parse = start.getName().getLocalPart().equals("GDALMetadata");
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    if (parse) {
                        item(event.asStartElement());       // Parse <Item> elements.
                    } else {
                        root(event.asStartElement());       // Search a nested <GDALMetadata>.
                    }
                } else if (event.isEndElement()) {
                    break;
                }
            }
        }

        /**
         * Parses a {@code <Item>} element and its children. After this method returns,
         * the reader is positioned after the closing {@code </Item>} tag.
         *
         * @param  start  the {@code <Item>} element.
         */
        private void item(final StartElement start) throws XMLStreamException {
            String attribute = null;
            if (depth == 0 && start.getName().getLocalPart().equals(ITEM)) {
                final Attribute a = start.getAttributeByName(name);
                if (a != null) attribute = a.getValue();
            }
            final var buffer = new StringJoiner("");
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    break;
                } else if (event.isCharacters()) {
                    buffer.add(event.asCharacters().getData());
                } else if (event.isStartElement()) {
                    depth++;
                    item(event.asStartElement());
                    depth--;
                }
            }
            if (attribute != null) {
                if (Character.isUpperCase(attribute.codePointAt(0))) {
                    attribute = attribute.toLowerCase(Locale.US);
                }
                final String content = buffer.toString();
                if (!content.isEmpty()) {
                    switch (attribute) {
                        case "acquisitionStartDate": startTime = LenientDateFormat.parseInstantUTC(content); break;
                        case "acquisitionEndDate":   endTime   = LenientDateFormat.parseInstantUTC(content); break;
                        case "title": metadata.addTitle(content); break;
                    }
                }
            }
        }

        /**
         * Writes to {@link MetadataBuilder} all information that were pending parsing completion.
         */
        void flush() {
            metadata.addTemporalExtent(startTime, endTime);
        }
    }

    /**
     * Invoked when a non-fatal warning occurs during the parsing of XML document.
     */
    @Override
    public boolean isLoggable(final LogRecord warning) {
        listeners.warning(warning);
        return false;
    }
}
