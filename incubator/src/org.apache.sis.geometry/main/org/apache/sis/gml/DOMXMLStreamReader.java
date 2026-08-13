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
package org.apache.sis.gml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * A pull-style {@link XMLStreamReader} (StAX cursor) that walks over an existing DOM subtree instead of parsing
 * bytes/characters.
 *
 * <p>
 * This fills the gap left by the JDK's built-in {@code com.sun.xml.internal.stream.XMLInputFactoryImpl}, whose
 * {@code createXMLStreamReader(Source)} throws {@code UnsupportedOperationException} for a {@code DOMSource}.
 * </p>
 *
 * <pre>{@code
 * XMLStreamReader reader = new DOMXMLStreamReader(element);
 * while (reader.hasNext()) {
 *     int event = reader.next();
 *     // ...
 * }
 * }</pre>
 *
 * <p>
 * The reader visits the tree in document order. Each element produces a {@code START_ELEMENT} on the way down and an
 * {@code END_ELEMENT} on the way back up; text, CDATA, comment and processing-instruction nodes are reported as leaf
 * events. Traversal never escapes above the element handed to the constructor, even if that element has a parent in a
 * larger document.</p>
 *
 * <p>
 * Notes / limitations:</p>
 * <ul>
 * <li>The DOM should ideally have been built with a namespace-aware parser
 * ({@code DocumentBuilderFactory.setNamespaceAware(true)}); otherwise namespace URIs and prefixes are unavailable and
 * local names fall back to the qualified node name.</li>
 * <li>Namespace-declaration attributes ({@code xmlns} / {@code xmlns:*}) are reported through the {@code getNamespace*}
 * methods, not the {@code getAttribute*} methods, as required by StAX.</li>
 * <li>All text nodes are reported as {@code CHARACTERS} (never {@code SPACE}); {@link #isWhiteSpace()} still works by
 * inspecting the content.</li>
 * <li>{@link #getLocation()} returns an unknown location (DOM keeps no source offsets).</li>
 * </ul>
 *
 * <p>
 * This class is not thread-safe.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public class DOMXMLStreamReader implements XMLStreamReader {

    private final Node root;

    /**
     * Node whose event is currently being reported (null only before the first next()).
     */
    private Node node;

    /**
     * Current event type, one of the {@link javax.xml.stream.XMLStreamConstants} values.
     */
    private int event = START_DOCUMENT;

    /**
     * Attributes of the current element, excluding namespace declarations.
     */
    private final List<Attr> attributes = new ArrayList<>();

    /**
     * Namespace-declaration attributes (xmlns / xmlns:*) of the current element.
     */
    private final List<Attr> namespaces = new ArrayList<>();

    /**
     * Creates a reader positioned before {@code element} (current event is {@code START_DOCUMENT}). The first call to
     * {@link #next()} moves to the element's {@code START_ELEMENT}.
     *
     * @param element the element to stream; must not be {@code null}
     */
    public DOMXMLStreamReader(Element element) {
        if (element == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        this.root = element;
    }

    // ------------------------------------------------------------------
    // Cursor movement
    // ------------------------------------------------------------------
    @Override
    public boolean hasNext() {
        return event != END_DOCUMENT;
    }

    @Override
    public int next() throws XMLStreamException {
        if (event == END_DOCUMENT) {
            throw new java.util.NoSuchElementException("past the end of the document");
        }

        if (event == START_DOCUMENT) {
            // Enter the root element.
            node = root;
            event = START_ELEMENT;
            refreshElementState();
            return event;
        }

        if (event == START_ELEMENT) {
            // Descend into the first child, or close an empty element in place.
            Node child = node.getFirstChild();
            if (child != null) {
                node = child;
                event = eventTypeOf(node);
            } else {
                event = END_ELEMENT; // node unchanged
            }
            refreshElementState();
            return event;
        }

        // We just finished reporting `node` as a completed unit
        // (END_ELEMENT of a child, or a leaf text/comment/PI/CDATA node).
        // The root's END_ELEMENT is the only place we stop.
        if (node == root) {
            event = END_DOCUMENT;
            node = null;
            attributes.clear();
            namespaces.clear();
            return event;
        }

        Node sibling = node.getNextSibling();
        if (sibling != null) {
            node = sibling;
            event = eventTypeOf(node);
        } else {
            // No more siblings: climb to the parent and close it.
            node = node.getParentNode();
            event = END_ELEMENT;
        }
        refreshElementState();
        return event;
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int type = next();
        while ((type == CHARACTERS && isWhiteSpace())
                || (type == CDATA && isWhiteSpace())
                || type == SPACE
                || type == PROCESSING_INSTRUCTION
                || type == COMMENT) {
            type = next();
        }
        if (type != START_ELEMENT && type != END_ELEMENT) {
            throw new XMLStreamException("expected start or end tag, got event " + type, getLocation());
        }
        return type;
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (event != START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", getLocation());
        }
        StringBuilder text = new StringBuilder();
        int type = next();
        while (type != END_ELEMENT) {
            switch (type) {
                case CHARACTERS:
                case CDATA:
                case SPACE:
                case ENTITY_REFERENCE:
                    text.append(getText());
                    break;
                case PROCESSING_INSTRUCTION:
                case COMMENT:
                    // ignorable inside element text content
                    break;
                case END_DOCUMENT:
                    throw new XMLStreamException("unexpected end of document while reading element text");
                case START_ELEMENT:
                    throw new XMLStreamException("element text content may not contain START_ELEMENT", getLocation());
                default:
                    throw new XMLStreamException("unexpected event type " + type, getLocation());
            }
            type = next();
        }
        return text.toString();
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        if (type != event) {
            throw new XMLStreamException("expected event type " + type + " but was " + event, getLocation());
        }
        if (localName != null) {
            if (event != START_ELEMENT && event != END_ELEMENT && event != ENTITY_REFERENCE) {
                throw new XMLStreamException("localName check not valid for event " + event, getLocation());
            }
            if (!localName.equals(getLocalName())) {
                throw new XMLStreamException("expected local name '" + localName + "' but was '" + getLocalName() + "'", getLocation());
            }
        }
        if (namespaceURI != null) {
            if (!namespaceURI.equals(getNamespaceURI())) {
                throw new XMLStreamException("expected namespace '" + namespaceURI + "' but was '" + getNamespaceURI() + "'", getLocation());
            }
        }
    }

    @Override
    public void close() {
        node = null;
        attributes.clear();
        namespaces.clear();
    }

    // ------------------------------------------------------------------
    // Event / state inspection
    // ------------------------------------------------------------------
    @Override
    public int getEventType() {
        return event;
    }

    @Override
    public boolean isStartElement() {
        return event == START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
        return event == END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
        return event == CHARACTERS;
    }

    @Override
    public boolean isWhiteSpace() {
        if (event != CHARACTERS && event != SPACE && event != CDATA) {
            return false;
        }
        String value = node.getNodeValue();
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasName() {
        return event == START_ELEMENT || event == END_ELEMENT;
    }

    @Override
    public boolean hasText() {
        return event == CHARACTERS || event == CDATA || event == COMMENT
                || event == SPACE || event == ENTITY_REFERENCE || event == DTD;
    }

    @Override
    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("property name must not be null");
        }
        return null;
    }

    @Override
    public Location getLocation() {
        return UNKNOWN_LOCATION;
    }

    // ------------------------------------------------------------------
    // Element name / prefix / namespace
    // ------------------------------------------------------------------
    @Override
    public QName getName() {
        checkElement();
        return qNameOf(node);
    }

    @Override
    public String getLocalName() {
        if (event == ENTITY_REFERENCE) {
            return node.getNodeName();
        }
        checkElement();
        String local = node.getLocalName();
        return local != null ? local : node.getNodeName();
    }

    @Override
    public String getPrefix() {
        checkElement();
        String prefix = node.getPrefix();
        return prefix != null ? prefix : XMLConstants.DEFAULT_NS_PREFIX;
    }

    @Override
    public String getNamespaceURI() {
        checkElement();
        return node.getNamespaceURI(); // may be null (StAX: null == no namespace)
    }

    // ------------------------------------------------------------------
    // Attributes (excluding namespace declarations)
    // ------------------------------------------------------------------
    @Override
    public int getAttributeCount() {
        checkStartElement("getAttributeCount");
        return attributes.size();
    }

    @Override
    public QName getAttributeName(int index) {
        checkStartElement("getAttributeName");
        return qNameOf(attributes.get(index));
    }

    @Override
    public String getAttributeLocalName(int index) {
        checkStartElement("getAttributeLocalName");
        Attr attr = attributes.get(index);
        String local = attr.getLocalName();
        return local != null ? local : attr.getNodeName();
    }

    @Override
    public String getAttributeNamespace(int index) {
        checkStartElement("getAttributeNamespace");
        return attributes.get(index).getNamespaceURI();
    }

    @Override
    public String getAttributePrefix(int index) {
        checkStartElement("getAttributePrefix");
        String prefix = attributes.get(index).getPrefix();
        return prefix != null ? prefix : XMLConstants.DEFAULT_NS_PREFIX;
    }

    @Override
    public String getAttributeType(int index) {
        checkStartElement("getAttributeType");
        return "CDATA";
    }

    @Override
    public String getAttributeValue(int index) {
        checkStartElement("getAttributeValue");
        return attributes.get(index).getValue();
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        checkStartElement("getAttributeValue");
        for (Attr attr : attributes) {
            String local = attr.getLocalName();
            if (local == null) {
                local = attr.getNodeName();
            }
            if (local.equals(localName)) {
                String ns = attr.getNamespaceURI();
                if (namespaceURI == null || namespaceURI.equals(ns)) {
                    return attr.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        checkStartElement("isAttributeSpecified");
        return attributes.get(index).getSpecified();
    }

    // ------------------------------------------------------------------
    // Namespace declarations
    // ------------------------------------------------------------------
    @Override
    public int getNamespaceCount() {
        if (event != START_ELEMENT && event != END_ELEMENT) {
            throw new IllegalStateException("getNamespaceCount is only valid on START_ELEMENT / END_ELEMENT");
        }
        return namespaces.size();
    }

    @Override
    public String getNamespacePrefix(int index) {
        Attr decl = namespaces.get(index);
        String name = decl.getNodeName();
        if ("xmlns".equals(name)) {
            return null; // default namespace declaration
        }
        return name.substring("xmlns:".length());
    }

    @Override
    public String getNamespaceURI(int index) {
        return namespaces.get(index).getValue();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix must not be null");
        }
        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return XMLConstants.XML_NS_URI;
        }
        if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        Node context = (node != null) ? node : root;
        String lookup = prefix.isEmpty() ? null : prefix;
        return context.lookupNamespaceURI(lookup);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return new DomNamespaceContext();
    }

    // ------------------------------------------------------------------
    // Text content
    // ------------------------------------------------------------------
    @Override
    public String getText() {
        if (!hasText()) {
            throw new IllegalStateException("getText not valid for event " + event);
        }
        String value = node.getNodeValue();
        return value != null ? value : "";
    }

    @Override
    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        if (target == null) {
            throw new NullPointerException("target array must not be null");
        }
        String text = getText();
        if (sourceStart < 0 || sourceStart > text.length() || length < 0
                || targetStart < 0 || targetStart + length > target.length) {
            throw new IndexOutOfBoundsException();
        }
        int available = text.length() - sourceStart;
        int count = Math.min(length, available);
        text.getChars(sourceStart, sourceStart + count, target, targetStart);
        return count;
    }

    @Override
    public int getTextStart() {
        if (!hasText()) {
            throw new IllegalStateException("getTextStart not valid for event " + event);
        }
        return 0;
    }

    @Override
    public int getTextLength() {
        return getText().length();
    }

    @Override
    public String getEncoding() {
        return null;
    }

    // ------------------------------------------------------------------
    // Processing instructions
    // ------------------------------------------------------------------
    @Override
    public String getPITarget() {
        if (event != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("getPITarget not valid for event " + event);
        }
        return ((ProcessingInstruction) node).getTarget();
    }

    @Override
    public String getPIData() {
        if (event != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("getPIData not valid for event " + event);
        }
        return ((ProcessingInstruction) node).getData();
    }

    // ------------------------------------------------------------------
    // Document-level properties (not available from a DOM subtree)
    // ------------------------------------------------------------------
    @Override
    public String getCharacterEncodingScheme() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean standaloneSet() {
        return false;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------
    /**
     * Maps a DOM node type to the corresponding StAX event constant.
     */
    private int eventTypeOf(Node n) {
        switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                return START_ELEMENT;
            case Node.TEXT_NODE:
                return CHARACTERS;
            case Node.CDATA_SECTION_NODE:
                return CDATA;
            case Node.COMMENT_NODE:
                return COMMENT;
            case Node.PROCESSING_INSTRUCTION_NODE:
                return PROCESSING_INSTRUCTION;
            case Node.ENTITY_REFERENCE_NODE:
                return ENTITY_REFERENCE;
            default:
                // DocumentType, Notation, etc. are not expected inside an element subtree.
                return CHARACTERS;
        }
    }

    /**
     * Recomputes the attribute / namespace lists when {@code node} is an element.
     */
    private void refreshElementState() {
        attributes.clear();
        namespaces.clear();
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }
        NamedNodeMap map = node.getAttributes();
        if (map == null) {
            return;
        }
        for (int i = 0; i < map.getLength(); i++) {
            Attr attr = (Attr) map.item(i);
            String name = attr.getNodeName();
            if ("xmlns".equals(name) || name.startsWith("xmlns:")) {
                namespaces.add(attr);
            } else {
                attributes.add(attr);
            }
        }
    }

    private QName qNameOf(Node n) {
        String local = n.getLocalName();
        if (local == null) {
            local = n.getNodeName();
        }
        String ns = n.getNamespaceURI();
        String prefix = n.getPrefix();
        if (ns == null) {
            ns = XMLConstants.NULL_NS_URI;
        }
        if (prefix == null) {
            prefix = XMLConstants.DEFAULT_NS_PREFIX;
        }
        return new QName(ns, local, prefix);
    }

    private void checkElement() {
        if (event != START_ELEMENT && event != END_ELEMENT) {
            throw new IllegalStateException("name access is only valid on START_ELEMENT / END_ELEMENT, event is " + event);
        }
    }

    private void checkStartElement(String method) {
        if (event != START_ELEMENT) {
            throw new IllegalStateException(method + " is only valid on START_ELEMENT, event is " + event);
        }
    }

    private static final Location UNKNOWN_LOCATION = new Location() {
        @Override
        public int getLineNumber() {
            return -1;
        }

        @Override
        public int getColumnNumber() {
            return -1;
        }

        @Override
        public int getCharacterOffset() {
            return -1;
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public String getSystemId() {
            return null;
        }
    };

    /**
     * NamespaceContext backed by DOM lookups on the current node.
     */
    private final class DomNamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            return DOMXMLStreamReader.this.getNamespaceURI(prefix) == null
                    ? XMLConstants.NULL_NS_URI
                    : DOMXMLStreamReader.this.getNamespaceURI(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI == null) {
                throw new IllegalArgumentException("namespaceURI must not be null");
            }
            if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
                return XMLConstants.XML_NS_PREFIX;
            }
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                return XMLConstants.XMLNS_ATTRIBUTE;
            }
            Node context = (node != null) ? node : root;
            return context.lookupPrefix(namespaceURI);
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            String prefix = getPrefix(namespaceURI);
            if (prefix == null) {
                return Collections.<String>emptyList().iterator();
            }
            return Collections.singletonList(prefix).iterator();
        }
    }
}
