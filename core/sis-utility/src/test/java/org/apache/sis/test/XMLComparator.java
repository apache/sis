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
package org.apache.sis.test;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.net.URI;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.ArgumentChecks;

import static java.lang.StrictMath.*;
import static org.opengis.test.Assert.*;
import static org.apache.sis.util.Characters.NO_BREAK_SPACE;
import static org.apache.sis.util.CharSequences.trimWhitespaces;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Compares the XML document produced by a test method with the expected XML document.
 * The two XML documents are specified at construction time. The comparison is performed
 * by a call to the {@link #compare()} method. The execution is delegated to the various
 * protected methods defined in this class, which can be overridden.
 *
 * <p>By default, this comparator expects the documents to contain the same elements and
 * the same attributes (but the order of attributes may be different).
 * However it is possible to:</p>
 *
 * <ul>
 *   <li>Specify whether comments shall be ignored (see {@link #ignoreComments})</li>
 *   <li>Specify attributes to ignore in comparisons (see {@link #ignoredAttributes})</li>
 *   <li>Specify nodes to ignore, including children (see {@link #ignoredNodes})</li>
 *   <li>Specify a tolerance threshold for comparisons of numerical values (see {@link #tolerance})</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see XMLTestCase
 * @see Assert#assertXmlEquals(Object, Object, String[])
 */
public strictfp class XMLComparator {
    /**
     * Commonly used prefixes for namespaces. Used as shorthands for calls to
     * {@link Assert#assertXmlEquals(Object, Object, String[])}.
     *
     * @see #substitutePrefix(String)
     */
    private static final Map<String, String> PREFIX_URL = new HashMap<String, String>(16);
    static {
        final Map<String,String> map = PREFIX_URL;
        map.put("xmlns", "http://www.w3.org/2000/xmlns"); // No trailing slash.
        map.put("xlink", Namespaces.XLINK);
        map.put("xsi",   Namespaces.XSI);
        map.put("gml",   Namespaces.GML);
        map.put("gmd",   Namespaces.GMD);
        map.put("gmx",   Namespaces.GMX);
        map.put("gmi",   Namespaces.GMI);
        map.put("gco",   Namespaces.GCO);
    }

    /**
     * The DOM factory, created when first needed.
     *
     * @see #newDocumentBuilder()
     */
    private static DocumentBuilderFactory factory;

    /**
     * The expected document.
     */
    private final Node expectedDoc;

    /**
     * The document resulting from the test method.
     */
    private final Node actualDoc;

    /**
     * {@code true} if the comments shall be ignored. The default value is {@code false}.
     */
    public boolean ignoreComments;

    /**
     * The fully-qualified name of attributes to ignore in comparisons.
     * This collection is initially empty. Users can add or remove elements in this collection as they wish.
     * The content of this collection will be honored by the default {@link #compareAttributes(Node, Node)}
     * implementation.
     *
     * <p>The elements shall be names in the form {@code "namespace:name"}, or only {@code "name"} if there
     * is no namespace. In order to ignore everything in a namespace, use {@code "namespace:*"}.</p>
     *
     * <p>Whether the namespace is the full URL or only the prefix depends on whether
     * {@link DocumentBuilderFactory#setNamespaceAware(boolean)} was set to {@code true}
     * or {@code false} respectively before the XML document has been built.
     * For example in order to ignore the standard {@code "schemaLocation"} attribute:</p>
     *
     * <ul>
     *   <li>If {@code NamespaceAware} is {@code true}, then this {@code ignoredAttributes} collection
     *       shall contains {@code "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"}.</li>
     *   <li>If {@code NamespaceAware} is {@code false}, then this {@code ignoredAttributes} collection
     *       shall contains {@code "xsi:schemaLocation"}, assuming that {@code "xsi"} is the prefix for
     *       {@code "http://www.w3.org/2001/XMLSchema-instance"}.</li>
     * </ul>
     *
     * <p>{@code XMLComparator} is namespace aware. The second case in the above-cited choice may happen only
     * if the user provided {@link Node} instances to the constructor. In such case, {@code XMLComparator} has
     * no control on whether the nodes contain namespaces or not.</p>
     *
     * <p>For example in order to ignore the namespace, type and schema location declaration,
     * the following strings can be added in this set:</p>
     *
     * {@preformat text
     *   "http://www.w3.org/2000/xmlns:*",
     *   "http://www.w3.org/2001/XMLSchema-instance:schemaLocation",
     *   "http://www.w3.org/2001/XMLSchema-instance:type"
     * }
     *
     * Note that for convenience, the {@link Assert#assertXmlEquals(Object, Object, String[])} method
     * automatically replaces some widely used prefixes by their full URL.
     */
    public final Set<String> ignoredAttributes;

    /**
     * The fully-qualified name of nodes to ignore in comparisons. The name shall be in the form
     * {@code "namespace:name"}, or only {@code "name"} if there is no namespace. In order to
     * ignore everything in a namespace, use {@code "namespace:*"}.
     *
     * <p>This set provides a way to ignore a node of the given name <em>and all its children</em>.
     * In order to ignore a node but still compare its children, override the
     * {@link #compareNode(Node, Node)} method instead.</p>
     *
     * <p>This set is initially empty. Users can add or remove elements in this set as they wish.
     * The content of this set will be honored by the default {@link #compareChildren(Node, Node)}
     * implementation.</p>
     */
    public final Set<String> ignoredNodes;

    /**
     * The tolerance threshold for comparisons of numerical values, or 0 for strict comparisons.
     * The default value is 0.
     */
    public double tolerance;

    /**
     * Creates a new comparator for the given inputs.
     * The inputs can be any of the following types:
     *
     * <ul>
     *   <li>{@link Node}; used directly without further processing.</li>
     *   <li>{@link File}, {@link URL} or {@link URI}: the stream is opened and parsed as a XML document.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.</li>
     * </ul>
     *
     * @param  expected  The expected XML document.
     * @param  actual    The XML document to compare.
     * @throws IOException If the stream can not be read.
     * @throws ParserConfigurationException If a {@link DocumentBuilder} can not be created.
     * @throws SAXException If an error occurred while parsing the XML document.
     */
    public XMLComparator(final Object expected, final Object actual)
            throws IOException, ParserConfigurationException, SAXException
    {
        ArgumentChecks.ensureNonNull("expected", expected);
        ArgumentChecks.ensureNonNull("actual",   actual);
        DocumentBuilder builder = null;
        if (expected instanceof Node) {
            expectedDoc = (Node) expected;
        } else {
            builder = newDocumentBuilder();
            final InputStream stream = toInputStream(expected);
            try {
                expectedDoc = builder.parse(stream);
            } finally {
                stream.close();
            }
        }
        if (actual instanceof Node) {
            actualDoc = (Node) actual;
        } else {
            if (builder == null) {
                builder = newDocumentBuilder();
            }
            final InputStream stream = toInputStream(actual);
            try {
                actualDoc = builder.parse(stream);
            } finally {
                stream.close();
            }
        }
        ignoredAttributes = new HashSet<String>();
        ignoredNodes      = new HashSet<String>();
    }

    /**
     * Creates a new document builder.
     */
    private synchronized static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        if (factory == null) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
        }
        return factory.newDocumentBuilder();
    }

    /**
     * Converts the given object to a stream.
     * See the constructor Javadoc for the list of allowed input type.
     */
    private static InputStream toInputStream(final Object input) throws IOException {
        if (input instanceof InputStream) return (InputStream) input;
        if (input instanceof File)        return new FileInputStream((File) input);
        if (input instanceof URI)         return ((URI) input).toURL().openStream();
        if (input instanceof URL)         return ((URL) input).openStream();
        if (input instanceof String)      return new ByteArrayInputStream(input.toString().getBytes("UTF-8"));
        throw new IOException("Can not handle input type: " + (input != null ? input.getClass() : input));
    }

    /**
     * If the given attribute name begins with one of the well known prefixes,
     * substitutes the prefix by the full URL. Otherwise returns the name unchanged.
     */
    static String substitutePrefix(final String attribute) {
        final int s = attribute.lastIndexOf(':');
        if (s >= 0) {
            final String url = PREFIX_URL.get(attribute.substring(0, s));
            if (url != null) {
                return url.concat(attribute.substring(s));
            }
        }
        return attribute;
    }

    /**
     * Compares the XML document specified at construction time. Before to invoke this
     * method, users may consider to add some values to the {@link #ignoredAttributes}
     * set.
     */
    public void compare() {
        compareNode(expectedDoc, actualDoc);
    }

    /**
     * Compares the two given nodes. This method delegates to one of the given methods depending
     * on the expected node type:
     *
     * <ul>
     *   <li>{@link #compareCDATASectionNode(CDATASection, Node)}</li>
     *   <li>{@link #compareTextNode(Text, Node)}</li>
     *   <li>{@link #compareCommentNode(Comment, Node)}</li>
     *   <li>{@link #compareProcessingInstructionNode(ProcessingInstruction, Node)}</li>
     *   <li>For all other types, {@link #compareNames(Node, Node)} and
     *       {@link #compareAttributes(Node, Node)}</li>
     * </ul>
     *
     * Then this method invokes itself recursively for every children,
     * by a call to {@link #compareChildren(Node, Node)}.
     *
     * @param expected The expected node.
     * @param actual The node to compare.
     */
    protected void compareNode(final Node expected, final Node actual) {
        if (expected == null || actual == null) {
            fail(formatErrorMessage(expected, actual));
            return;
        }
        /*
         * Check text value for types:
         * TEXT_NODE, CDATA_SECTION_NODE, COMMENT_NODE, PROCESSING_INSTRUCTION_NODE
         */
        if (expected instanceof CDATASection) {
            compareCDATASectionNode((CDATASection) expected, actual);
        } else if (expected instanceof Text) {
            compareTextNode((Text) expected, actual);
        } else if (expected instanceof Comment) {
            compareCommentNode((Comment) expected, actual);
        } else if (expected instanceof ProcessingInstruction) {
            compareProcessingInstructionNode((ProcessingInstruction) expected, actual);
        } else if (expected instanceof Attr) {
            compareAttributeNode((Attr) expected, actual);
        } else {
            compareNames(expected, actual);
            compareAttributes(expected, actual);
        }
        /*
         * Check child nodes recursivly if it's not an attribut.
         */
        if (expected.getNodeType() != Node.ATTRIBUTE_NODE) {
            compareChildren(expected, actual);
        }
    }

    /**
     * Compares a node which is expected to be of {@link Text} type. The default implementation
     * ensures that the given node is an instance of {@link Text}, then ensures that both nodes
     * have the same names, attributes and text content.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual   The actual node.
     */
    protected void compareTextNode(final Text expected, final Node actual) {
        assertInstanceOf("Actual node is not of the expected type.", Text.class, actual);
        compareNames(expected, actual);
        compareAttributes(expected, actual);
        assertTextContentEquals(expected, actual);
    }

    /**
     * Compares a node which is expected to be of {@link CDATASection} type. The default
     * implementation ensures that the given node is an instance of {@link CDATASection},
     * then ensures that both nodes have the same names, attributes and text content.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual   The actual node.
     */
    protected void compareCDATASectionNode(final CDATASection expected, final Node actual) {
        assertInstanceOf("Actual node is not of the expected type.", CDATASection.class, actual);
        compareNames(expected, actual);
        compareAttributes(expected, actual);
        assertTextContentEquals(expected, actual);
    }

    /**
     * Compares a node which is expected to be of {@link Comment} type. The default
     * implementation ensures that the given node is an instance of {@link Comment},
     * then ensures that both nodes have the same names, attributes and text content.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual   The actual node.
     */
    protected void compareCommentNode(final Comment expected, final Node actual) {
        assertInstanceOf("Actual node is not of the expected type.", Comment.class, actual);
        compareNames(expected, actual);
        compareAttributes(expected, actual);
        assertTextContentEquals(expected, actual);
    }

    /**
     * Compares a node which is expected to be of {@link ProcessingInstruction} type. The default
     * implementation ensures that the given node is an instance of {@link ProcessingInstruction},
     * then ensures that both nodes have the same names, attributes and text content.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual   The actual node.
     */
    protected void compareProcessingInstructionNode(final ProcessingInstruction expected, final Node actual) {
        assertInstanceOf("Actual node is not of the expected type.", ProcessingInstruction.class, actual);
        compareNames(expected, actual);
        compareAttributes(expected, actual);
        assertTextContentEquals(expected, actual);
    }

    /**
     * Compares a node which is expected to be of {@link Attr} type. The default
     * implementation ensures that the given node is an instance of {@link Attr},
     * then ensures that both nodes have the same names and text content.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual   The actual node.
     */
    protected void compareAttributeNode(final Attr expected, final Node actual) {
        assertInstanceOf("Actual node is not of the expected type.", Attr.class, actual);
        compareNames(expected, actual);
        compareAttributes(expected, actual);
        assertTextContentEquals(expected, actual);
    }

    /**
     * Compares the children of the given nodes. The node themselves are not compared.
     * Children shall appear in the same order. Nodes having a name declared in the
     * {@link #ignoredNodes} set are ignored.
     *
     * <p>Subclasses can override this method if they need a different processing.</p>
     *
     * @param expected The expected node.
     * @param actual The node for which to compare children.
     */
    protected void compareChildren(Node expected, Node actual) {
        expected = firstNonEmptySibling(expected.getFirstChild());
        actual   = firstNonEmptySibling(actual  .getFirstChild());
        while (expected != null) {
            compareNode(expected, actual);
            expected = firstNonEmptySibling(expected.getNextSibling());
            actual   = firstNonEmptySibling(actual  .getNextSibling());
        }
        if (actual != null) {
            fail(formatErrorMessage(expected, actual));
        }
    }

    /**
     * Compares the names and namespaces of the given node.
     * Subclasses can override this method if they need a different comparison.
     *
     * @param expected The node having the expected name and namespace.
     * @param actual The node to compare.
     */
    protected void compareNames(final Node expected, final Node actual) {
        assertPropertyEquals("namespace", expected.getNamespaceURI(), actual.getNamespaceURI(), expected, actual);
        String expectedName = expected.getLocalName();
        String actualName   = actual.getLocalName();
        if (expectedName == null || actualName == null) {
            expectedName = expected.getNodeName();
            actualName   = actual.getNodeName();
        }
        assertPropertyEquals("name", expectedName, actualName, expected, actual);
    }

    /**
     * Compares the attributes of the given nodes.
     * Subclasses can override this method if they need a different comparison.
     *
     * <p><strong>NOTE:</strong> Current implementation requires the number of attributes to be the
     * same only if the {@link #ignoredAttributes} set is empty. If the {@code ignoredAttributes}
     * set is not empty, then the actual node could have more attributes than the expected node;
     * the extra attributes are ignored. This may change in a future version if it appears to be
     * a problem in practice.</p>
     *
     * @param expected The node having the expected attributes.
     * @param actual The node to compare.
     */
    @SuppressWarnings("null")
    protected void compareAttributes(final Node expected, final Node actual) {
        final NamedNodeMap expectedAttributes = expected.getAttributes();
        final NamedNodeMap actualAttributes   = actual.getAttributes();
        final int n = (expectedAttributes != null) ? expectedAttributes.getLength() : 0;
        if (ignoredAttributes.isEmpty()) {
            assertPropertyEquals("nbAttributes", n,
                    (actualAttributes != null) ? actualAttributes.getLength() : 0, expected, actual);
        }
        for (int i=0; i<n; i++) {
            final Node expAttr = expectedAttributes.item(i);
            final String ns    = expAttr.getNamespaceURI();
            String name        = expAttr.getLocalName();
            if (name == null) {
                /*
                 * The above variables may be null if the node has been built from a DOM Level 1 API,
                 * or if the DocumentBuilder was not namespace-aware. In the following table, the first
                 * column shows the usual case for "http://www.w3.org/2000/xmlns/gml". The second column
                 * shows the case if the DocumentBuilder was not aware of namespaces. The last column is
                 * a case sometime observed.
                 *
                 * ┌───────────────────┬─────────────────────────────────┬──────────────┬─────────────┐
                 * │ Node method       │ Namespace (NS) aware            │ Non NS-aware │ Other case  │
                 * ├───────────────────┼─────────────────────────────────┼──────────────┼─────────────┤
                 * │ getNamespaceURI() │ "http://www.w3.org/2000/xmlns/" │  null        │ "xmlns"     │
                 * │ getLocalName()    │ "gml"                           │  null        │ "gml"       │
                 * │ getNodeName()     │ "xmlns:gml"                     │ "xmlns:gml"  │             │
                 * └───────────────────┴─────────────────────────────────┴──────────────┴─────────────┘
                 *
                 * By default, this block is not be executed. However if the user gave us Nodes that are
                 * not namespace aware, then the 'isIgnored(…)' method will try to parse the node name.
                 */
                name = expAttr.getNodeName();
            }
            if (!isIgnored(ignoredAttributes, ns, name)) {
                final Node actAttr;
                if (ns == null) {
                    actAttr = actualAttributes.getNamedItem(name);
                } else {
                    actAttr = actualAttributes.getNamedItemNS(ns, name);
                }
                compareNode(expAttr, actAttr);
            }
        }
    }

    /**
     * Returns {@code true} if the given node or attribute shall be ignored.
     *
     * @param ignored The set of node or attribute fully qualified names to ignore.
     * @param ns      The node or attribute namespace, or {@code null}.
     * @param name    The node or attribute name.
     * @return        {@coce true} if the node or attribute shall be ignored.
     */
    private static boolean isIgnored(final Set<String> ignored, String ns, final String name) {
        if (!ignored.isEmpty()) {
            if (ns == null) {
                /*
                 * If there is no namespace, then the 'name' argument should be the qualified name
                 * (with a prefix). Example: "xsi:schemaLocation". We will look first for an exact
                 * name match, then for a match after replacing the local name by "*".
                 */
                if (ignored.contains(name)) {
                    return true;
                }
                final int s = name.indexOf(':');
                if (s >= 1 && ignored.contains(name.substring(0, s+1) + '*')) {
                    return true;
                }
            } else {
                /*
                 * If there is a namespace (which is the usual case), perform the concatenation
                 * with the name before to check in the collection of ignored attributes.
                 */
                final StringBuilder buffer = new StringBuilder(ns);
                int length = buffer.length();
                if (length != 0 && buffer.charAt(length - 1) == '/') {
                    buffer.setLength(--length);
                }
                /*
                 * Check if the fully qualified attribute name is one of the attributes to ignore.
                 * Typical example: "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"
                 */
                buffer.append(':').append(name, name.indexOf(':') + 1, name.length());
                if (ignored.contains(buffer.toString())) {
                    return true;
                }
                /*
                 * The given attribute does not appear explicitely in the set of attributes to ignore.
                 * But maybe the user asked to ignore all attributes in the namespace.
                 * Typical example: "http://www.w3.org/2000/xmlns:*"
                 */
                buffer.setLength(length + 1);
                if (ignored.contains(buffer.append('*').toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the first sibling of the given node having a non-empty text content, or {@code null}
     * if none. This method first check the given node, then check all siblings. Attribute nodes are
     * ignored.
     *
     * @param  node The node to check, or {@code null}.
     * @return The first node having a non-empty text content, or {@code null} if none.
     */
    private Node firstNonEmptySibling(Node node) {
        for (; node != null; node = node.getNextSibling()) {
            if (!isIgnored(ignoredNodes, node.getNamespaceURI(), node.getNodeName())) {
                switch (node.getNodeType()) {
                    // For attribute node, continue the search unconditionally.
                    case Node.ATTRIBUTE_NODE: continue;

                    // For text node, continue the search if the node is empty.
                    case Node.TEXT_NODE: {
                        final String text = trimWhitespaces(node.getTextContent());
                        if (text == null || text.isEmpty()) {
                            continue;
                        }
                        break;
                    }

                    // Ignore comment nodes only if requested.
                    case Node.COMMENT_NODE: {
                        if (ignoreComments) {
                            continue;
                        }
                        break;
                    }
                }
                // Found a node: stop the search.
                break;
            }
        }
        return node;
    }

    /**
     * Verifies that the text content of the given nodes are equal.
     *
     * @param expected The node that contains the expected text.
     * @param actual   The node that contains the actual text to verify.
     */
    protected void assertTextContentEquals(final Node expected, final Node actual) {
        assertPropertyEquals("textContent", expected.getTextContent(), actual.getTextContent(), expected, actual);
    }

    /**
     * Verifies that the given property (text or number) are equal, ignoring spaces. If they are
     * not equal, then an error message is formatted using the given property name and nodes.
     *
     * @param propertyName The name of the property being compared (typically "name", "namespace", etc.).
     * @param expected     The property value from the expected node to compare.
     * @param actual       The property value to compare to the expected one.
     * @param expectedNode The node from which the expected property has been fetched.
     * @param actualNode   The node being compared to the expected node.
     */
    protected void assertPropertyEquals(final String propertyName, Comparable<?> expected, Comparable<?> actual,
            final Node expectedNode, final Node actualNode)
    {
        expected = trim(expected);
        actual   = trim(actual);
        if ((expected != actual) && (expected == null || !expected.equals(actual))) {
            // Before to declare a test failure, compares again as numerical values if possible.
            if (abs(doubleValue(expected) - doubleValue(actual)) <= tolerance) {
                return;
            }
            final String lineSeparator = JDK7.lineSeparator();
            final StringBuilder buffer = new StringBuilder(1024).append("Expected ")
                    .append(propertyName).append(" \"")
                    .append(expected).append("\" but got \"")
                    .append(actual).append("\" for nodes:")
                    .append(lineSeparator);
            formatErrorMessage(buffer, expectedNode, actualNode, lineSeparator);
            fail(buffer.toString());
        }
    }

    /**
     * Trims the leading and trailing spaces in the given property
     * if it is actually a {@link String} object.
     */
    private static Comparable<?> trim(final Comparable<?> property) {
        return (property instanceof String) ? trimWhitespaces(((String) property)) : property;
    }

    /**
     * Parses the given text as a number. If the given text is null or can not be parsed,
     * returns {@code NaN}. This is used only if a {@linkplain #tolerance} threshold greater
     * than zero has been provided.
     */
    @SuppressWarnings("unchecked")
    private static double doubleValue(final Comparable<?> property) {
        if (property instanceof Number) {
            return ((Number) property).doubleValue();
        }
        if (property instanceof CharSequence) try {
            return Double.parseDouble(property.toString());
        } catch (NumberFormatException e) {
            // Ignore, as specified in method javadoc.
        }
        return Double.NaN;
    }

    /**
     * Formats an error message for a node mismatch. The message will contain a string
     * representation of the expected and actual node.
     *
     * @param expected The expected node.
     * @param result   The actual node.
     * @return         An error message containing the expected and actual node.
     */
    protected String formatErrorMessage(final Node expected, final Node result) {
        final String lineSeparator = JDK7.lineSeparator();
        final StringBuilder buffer = new StringBuilder(256).append("Nodes are not equal:").append(lineSeparator);
        formatErrorMessage(buffer, expected, result, lineSeparator);
        return buffer.toString();
    }

    /**
     * Formats in the given buffer an error message for a node mismatch.
     *
     * @param lineSeparator The platform-specific line separator.
     */
    private static void formatErrorMessage(final StringBuilder buffer, final Node expected,
            final Node result, final String lineSeparator)
    {
        formatNode(buffer.append("Expected node: "), expected, lineSeparator);
        formatNode(buffer.append("Actual node:   "), result,   lineSeparator);
        buffer.append("Expected hierarchy:").append(lineSeparator);
        final List<String> hierarchy = formatHierarchy(buffer, expected, null, lineSeparator);
        buffer.append("Actual hierarchy:").append(lineSeparator);
        formatHierarchy(buffer, result, hierarchy, lineSeparator);
    }

    /**
     * Appends to the given buffer the string representation of the node hierarchy.
     * The first line will contains the root of the tree. Other lines will contain
     * the child down in the hierarchy until the given node, inclusive.
     *
     * <p>This method formats only a summary if the hierarchy is equals to the expected one.</p>
     *
     * @param buffer        The buffer in which to append the formatted hierarchy.
     * @param node          The node for which to format the parents.
     * @param expected      The expected hierarchy, or {@code null} if unknown.
     * @param lineSeparator The platform-specific line separator.
     */
    private static List<String> formatHierarchy(final StringBuilder buffer, Node node,
            final List<String> expected, final String lineSeparator)
    {
        final List<String> hierarchy = new ArrayList<String>();
        while (node != null) {
            hierarchy.add(node.getNodeName());
            if (node instanceof Attr) {
                node = ((Attr) node).getOwnerElement();
            } else {
                node = node.getParentNode();
            }
        }
        if (hierarchy.equals(expected)) {
            buffer.append("└─Same as expected").append(lineSeparator);
        } else {
            int indent = 2;
            for (int i=hierarchy.size(); --i>=0;) {
                for (int j=indent; --j>=0;) {
                    buffer.append(NO_BREAK_SPACE);
                }
                buffer.append("└─").append(hierarchy.get(i)).append(lineSeparator);
                indent += 4;
            }
        }
        return hierarchy;
    }

    /**
     * Appends to the given buffer a string representation of the given node.
     * The string representation is terminated by a line feed.
     *
     * @param buffer        The buffer in which to append the formatted node.
     * @param node          The node to format.
     * @param lineSeparator The platform-specific line separator.
     */
    @SuppressWarnings("null")
    private static void formatNode(final StringBuilder buffer, final Node node, final String lineSeparator) {
        if (node == null) {
            buffer.append("(no node)").append(lineSeparator);
            return;
        }
        // Format the text content, together with the text content of the
        // child if there is exactly one child.
        final String ns = node.getNamespaceURI();
        if (ns != null) {
            buffer.append(ns).append(':');
        }
        buffer.append(node.getNodeName());
        boolean hasText = appendTextContent(buffer, node);
        final NodeList children = node.getChildNodes();
        int numChildren = 0;
        if (children != null) {
            numChildren = children.getLength();
            if (numChildren == 1 && !hasText) {
                hasText = appendTextContent(buffer, children.item(0));
            }
        }

        // Format the number of children and the number of attributes, if any.
        String separator = " (";
        if (numChildren != 0) {
            buffer.append(separator).append("nbChild=").append(numChildren);
            separator = ", ";
        }
        final NamedNodeMap atts = node.getAttributes();
        int numAtts = 0;
        if (atts != null) {
            numAtts = atts.getLength();
            if (numAtts != 0) {
                buffer.append(separator).append("nbAtt=").append(numAtts);
                separator = ", ";
            }
        }
        if (!separator.equals(" (")) {
            buffer.append(')');
        }

        // Format all attributes, if any.
        separator = " [";
        for (int i=0; i<numAtts; i++) {
            buffer.append(separator).append(atts.item(i));
            separator = ", ";
        }
        if (!separator.equals(" [")) {
            buffer.append(']');
        }
        buffer.append(lineSeparator);
    }

    /**
     * Appends the text content of the given node only if the node is an instance of {@link Text}
     * or related type ({@link CDATASection}, {@link Comment} or {@link ProcessingInstruction}).
     * Otherwise this method does nothing.
     *
     * @param  buffer The buffer in which to append text content.
     * @param  node   The node for which to append text content.
     * @return {@code true} if a text has been formatted.
     */
    private static boolean appendTextContent(final StringBuilder buffer, final Node node) {
        if (node instanceof Text ||
            node instanceof Comment ||
            node instanceof CDATASection ||
            node instanceof ProcessingInstruction)
        {
            buffer.append("=\"").append(node.getTextContent()).append('"');
            return true;
        }
        return false;
    }
}
