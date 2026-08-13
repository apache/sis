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

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link DOMXMLStreamReader} class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class DOMXMLStreamReaderTest {
    /**
     * Creates a new test case.
     */
    public DOMXMLStreamReaderTest() {
    }

    /**
     * Parses the given XML document with a namespace-aware builder and returns its root element.
     */
    private static Element parse(final String xml) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
    }

    /**
     * Verifies the claim documented in the class javadoc: the JDK's built-in StAX implementation
     * does not support creating a {@link XMLStreamReader} from a {@link DOMSource}, which is the
     * gap that {@link DOMXMLStreamReader} is meant to fill.
     *
     * @throws Exception if an error occurred while building the test document.
     */
    @Test
    public void testJdkDefaultFactoryRejectsDOMSource() throws Exception {
        final Element element = parse("<root/>");
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        assertThrows(UnsupportedOperationException.class,
                () -> factory.createXMLStreamReader(new DOMSource(element)));
    }

    /**
     * Tests traversal of a simple document containing nested elements and text content,
     * verifying that events are reported in document order.
     *
     * @throws Exception if an error occurred while building the test document or reading it.
     */
    @Test
    public void testTraversalOrder() throws Exception {
        final Element root = parse("<root><a>text</a><b/></root>");
        final XMLStreamReader reader = new DOMXMLStreamReader(root);

        assertEquals(XMLStreamConstants.START_DOCUMENT, reader.getEventType());
        assertTrue(reader.hasNext());

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals("root", reader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals("a", reader.getLocalName());

        assertEquals(XMLStreamConstants.CHARACTERS, reader.next());
        assertEquals("text", reader.getText());

        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals("a", reader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals("b", reader.getLocalName());

        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals("b", reader.getLocalName());

        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals("root", reader.getLocalName());

        assertTrue(reader.hasNext());
        assertEquals(XMLStreamConstants.END_DOCUMENT, reader.next());
        assertFalse(reader.hasNext());
    }

    /**
     * Tests that traversal never escapes above the element given to the constructor,
     * even when that element has a parent in a larger document.
     *
     * @throws Exception if an error occurred while building the test document or reading it.
     */
    @Test
    public void testTraversalStaysUnderGivenElement() throws Exception {
        final Element root = parse("<root><a><b/></a><sibling/></root>");
        final Element a = (Element) root.getFirstChild();
        final XMLStreamReader reader = new DOMXMLStreamReader(a);

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals("a", reader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals("b", reader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals("b", reader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals("a", reader.getLocalName());
        assertEquals(XMLStreamConstants.END_DOCUMENT, reader.next());
        assertFalse(reader.hasNext());
    }

    /**
     * Tests reading of attributes on an element, and that namespace-declaration attributes
     * are excluded from the plain attribute list.
     *
     * @throws Exception if an error occurred while building the test document or reading it.
     */
    @Test
    public void testAttributes() throws Exception {
        final Element root = parse("<root xmlns:ex=\"http://example.org\" id=\"42\" ex:kind=\"foo\"/>");
        final XMLStreamReader reader = new DOMXMLStreamReader(root);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());

        assertEquals(2, reader.getAttributeCount());
        assertEquals("42", reader.getAttributeValue(null, "id"));
        assertEquals("foo", reader.getAttributeValue("http://example.org", "kind"));

        // A null namespace means "do not check the namespace", so it matches regardless of "kind" actual namespace.
        assertEquals("foo", reader.getAttributeValue(null, "kind"));

        // A non-matching namespace must not match, even though the local name is the same.
        assertNull(reader.getAttributeValue("http://other.org", "kind"));

        assertEquals(1, reader.getNamespaceCount());
        assertEquals("ex", reader.getNamespacePrefix(0));
        assertEquals("http://example.org", reader.getNamespaceURI(0));
        assertEquals("http://example.org", reader.getNamespaceURI("ex"));
    }

    /**
     * Tests {@link DOMXMLStreamReader#getName()} and related methods on a namespaced element.
     *
     * @throws Exception if an error occurred while building the test document or reading it.
     */
    @Test
    public void testElementName() throws Exception {
        final Element root = parse("<ex:root xmlns:ex=\"http://example.org\"/>");
        final XMLStreamReader reader = new DOMXMLStreamReader(root);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());

        assertEquals("root", reader.getLocalName());
        assertEquals("ex", reader.getPrefix());
        assertEquals("http://example.org", reader.getNamespaceURI());
        assertEquals("http://example.org", reader.getName().getNamespaceURI());
        assertEquals("root", reader.getName().getLocalPart());
    }

    /**
     * Tests {@link DOMXMLStreamReader#getElementText()}, which concatenates the text content
     * of an element and leaves the cursor on the closing tag.
     *
     * @throws Exception if an error occurred while building the test document or reading it.
     */
    @Test
    public void testGetElementText() throws Exception {
        final Element root = parse("<root>hello <!--comment--> world</root>");
        final XMLStreamReader reader = new DOMXMLStreamReader(root);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());

        assertEquals("hello  world", reader.getElementText());
        assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
        assertEquals("root", reader.getLocalName());
    }

    /**
     * Tests that {@link DOMXMLStreamReader#next()} throws an exception once the end
     * of the document has already been reached.
     *
     * @throws Exception if an error occurred while building the test document.
     */
    @Test
    public void testNextAfterEndDocument() throws Exception {
        final Element root = parse("<root/>");
        final XMLStreamReader reader = new DOMXMLStreamReader(root);
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals(XMLStreamConstants.END_ELEMENT, reader.next());
        assertEquals(XMLStreamConstants.END_DOCUMENT, reader.next());
        assertThrows(java.util.NoSuchElementException.class, reader::next);
    }

    /**
     * Tests that the constructor rejects a {@code null} element.
     */
    @Test
    public void testConstructorRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new DOMXMLStreamReader(null));
    }
}
