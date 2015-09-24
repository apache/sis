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
package org.apache.sis.internal.book;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Generates the developer guide from the given input file.
 * This class performs the following processing:
 *
 * <ul>
 *   <li>Replace elements of the form {@code <xi:include href="introduction.html"/>} by content of the {@code <body>} element
 *       in the file given by the {@code href} attribute.</li>
 *
 *   <li>Complete {@code <abbr>} elements without {@code title} attribute by reusing the last title used for the same abbreviation.
 *       This automatic insertion is performed only for the first occurrence of that abbreviation after a {@code h?} element.</li>
 *
 *   <li>Replace the {@code <!-- TOC -->} comment by a table of content generated from all {@code <h1>}, {@code <h2>}, etc.
 *       found in the document.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 */
public final class Assembler {
    /**
     * The line separator to be used in the output file.
     * We fix it to the Unix style (not the native style of the platform) for more compact output file.
     */
    private static final String LINE_SEPARATOR = "\n";

    /**
     * The directory of all input files to process.
     */
    private final File inputDirectory;

    /**
     * The factory for creating new XML nodes.
     */
    private final DocumentBuilder builder;

    /**
     * The XML document to write. This is initially the XML document parsed from the given input file.
     * Then all included files are inserted in-place and some nodes are processed as documented in the
     * class javadoc.
     */
    private final Document document;

    /**
     * The node where to write the table of content.
     */
    private final Element tableOfContent;

    /**
     * The {@code title} attributes found in abbreviations.
     */
    private final Map<String,String> abbreviations = new HashMap<>();

    /**
     * Whether we found an abbreviation after the last {@code h?} element.
     * This is used in order to avoid inserting too many abbreviation title.
     */
    private final Set<String> writtenAbbreviations = new HashSet<>();

    /**
     * Creates a new assembler for the given input and output files.
     *
     * @param  input the input file (e.g. {@code "site/book/en/body.html"}).
     * @throws ParserConfigurationException if this constructor can not build the XML document.
     * @throws IOException if an error occurred while reading the file.
     * @throws SAXException if an error occurred while parsing the XML.
     */
    public Assembler(final File input) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // No setXIncludeAware(true) -  we will handle <xi:include> elements ourself.
        factory.setNamespaceAware(true);
        inputDirectory = input.getParentFile();
        builder        = factory.newDocumentBuilder();
        document       = load(input.getName());
        tableOfContent = document.createElement("ul");
        tableOfContent.setAttribute("class", "toc");
        /*
         * Remove the "http://www.w3.org/2001/XInclude" namespace since we
         * should have no <xi:include> elements left in the output file.
         */
        ((Element) document.getElementsByTagName("html").item(0)).removeAttribute("xmlns:xi");
        /*
         * Replace the License comment by a shorter one followed by the
         * "This is an automatically generated file"> notice.
         */
        for (final Node node : toArray(document.getDocumentElement().getParentNode().getChildNodes())) {
            if (node.getNodeType() == Node.COMMENT_NODE) {
                node.setNodeValue(LINE_SEPARATOR + LINE_SEPARATOR +
                        "  Licensed to the Apache Software Foundation (ASF)" + LINE_SEPARATOR +
                        LINE_SEPARATOR +
                        "      http://www.apache.org/licenses/LICENSE-2.0" + LINE_SEPARATOR +
                        LINE_SEPARATOR +
                        "  This is an automatically generated file. DO NOT EDIT." + LINE_SEPARATOR +
                        "  See the files in the ../../../book/ directory instead." + LINE_SEPARATOR +
                        LINE_SEPARATOR);
                break;
            }
        }
    }

    /**
     * Loads the XML document from the given file in the same directory than the input file given to the constructor.
     */
    private Document load(final String filename) throws IOException, SAXException {
        final Document include = builder.parse(new File(inputDirectory, filename));
        builder.reset();
        removeIndentation(include.getDocumentElement());
        return include;
    }

    /**
     * Removes the indentation at the beginning of lines in the given node and all child nodes.
     * This can reduce the file length by as much as 20%. Note that the indentation was broken
     * anyway after the treatment of {@code <xi:include>}, because included file does not use
     * the right amount of spaces for the location where it is introduced.
     */
    private void removeIndentation(final Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            boolean       newLine = false;
            StringBuilder buffer  = null;
            CharSequence  text    = node.getTextContent();
            for (int i=0; i<text.length(); i++) {
                switch (text.charAt(i)) {
                    case '\r': break;  // Delete all occurrences of '\r'.
                    case '\n': newLine = true;  continue;
                    default  : newLine = false; continue;
                    case ' ' : if (newLine) break; else continue;
                }
                if (buffer == null) {
                    text = buffer = new StringBuilder(text);
                }
                buffer.deleteCharAt(i--);
            }
            if (buffer != null) {
                node.setNodeValue(buffer.toString());
            }
            return;
        }
        final NodeList children = node.getChildNodes();
        final int length = children.getLength();
        for (int i=0; i<length; i++) {
            removeIndentation(children.item(i));
        }
    }

    /**
     * Copies the body of the given source HTML file in-place of the given target node.
     * This method is doing the work of {@code <xi:include>} element. We do this work ourself instead than relying on
     * {@link DocumentBuilder} build-in support mostly because we have been unable to get the {@code xpointer} to work.
     *
     * @param filename  the source XML file in the same directory than the input file given to the constructor.
     * @param toReplace the target XML node to be replaced by the content of the given file.
     */
    private Node replaceByBody(final String filename, final Node toReplace) throws IOException, SAXException {
        final NodeList nodes = load(filename).getElementsByTagName("body");
        if (nodes.getLength() != 1) {
            throw new IOException(filename + ": expected exactly one <body> element.");
        }
        final Node element = document.createElement("section");
        toReplace.getParentNode().replaceChild(element, toReplace);
        for (Node child : toArray(nodes.item(0).getChildNodes())) {
            child = document.importNode(child, true);   // document.adoptNode(child) would have been more efficient but does not seem to work.
            if (child == null) {
                throw new IOException("Failed to copy subtree.");
            }
            element.appendChild(child);
        }
        return element;
    }

    /**
     * Returns all nodes in the given list as an array. This method is used for getting a snapshot
     * of the list before to modify it (for example before the elements are moved to another node).
     */
    private static Node[] toArray(final NodeList nodes) {
        final Node[] children = new Node[nodes.getLength()];
        for (int i=0; i<children.length; i++) {
            children[i] = nodes.item(i);
        }
        return children;
    }

    /**
     * Automatically inserts a {@code title} attribute in the given {@code <abbr>} element
     * if it meets the condition documented in the class javadoc.
     */
    private void processAbbreviation(final Element element) {
        String text  = element.getTextContent();
        String title = element.getAttribute("title");
        if (!title.isEmpty()) {
            abbreviations.put(text, title);
        }
        if (writtenAbbreviations.add(text) && title.isEmpty()) {
            title = abbreviations.get(text);
            if (title != null) {
                element.setAttribute("title", title);
            }
        }
    }

    /**
     * Performs on the given node the processing documented in the class javadoc.
     * This method invokes itself recursively.
     */
    private void process(Node node) throws IOException, SAXException {
        switch (node.getNodeType()) {
            case Node.COMMENT_NODE: {
                final String text = node.getNodeValue().trim();
                if ("TOC".equals(text)) {
                    node.getParentNode().replaceChild(tableOfContent, node);
                }
                return;
            }
            case Node.ELEMENT_NODE: {
                final String name = node.getNodeName();
                switch (name) {
                    case "xi:include": {
                        node = replaceByBody(((Element) node).getAttribute("href"), node);
                        break;
                    }
                    case "abbr": {
                        processAbbreviation((Element) node);
                        break;
                    }
                    default: {
                        if (name.length() == 2 && name.charAt(0) == 'h') {
                            final int c = name.charAt(1) - '0';
                            if (c >= 1 && c <= 9) {
                                writtenAbbreviations.clear();
                                appendToTableOfContent(c, ((Element) node).getAttribute("id"), node.getTextContent());
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
        for (final Node child : toArray(node.getChildNodes())) {
            process(child);
        }
    }

    /**
     * Appends the given header to the table of content.
     *
     * @param level level of the {@code <h1>}, {@code <h2>}, {@code <h3>}, etc. element found.
     */
    private void appendToTableOfContent(int level, final String id, final String text) throws IOException {
        if (id.isEmpty()) {
            throw new IOException("Missing identifier for header: " + text);
        }
        final Element item = document.createElement("li");
        final Element ref = document.createElement("a");
        ref.setAttribute("href", "#" + id);
        ref.setTextContent(text);
        item.appendChild(ref);
        Node node = tableOfContent;
        while (--level > 0) {
            node = node.getLastChild(); // Last <li> element.
            if (node == null) {
                throw new IOException("Non-continuous header level: " + text);
            }
            Node list = node.getLastChild();    // Search for <ul> element in above <li>.
            if (list == null || !"ul".equals(list.getNodeName())) {
                list = document.createElement("ul");
                node.appendChild(list);
            }
            node = list;
        }
        node.appendChild(document.createTextNode(LINE_SEPARATOR));
        node.appendChild(item);
    }

    /**
     * Assembles the document and writes to the destination.
     *
     * @param  output the output file (e.g. {@code "site/content/en/developer-guide.html"}).
     * @throws IOException if an error occurred while reading or writing file.
     * @throws SAXException if an error occurred while parsing an input XML.
     * @throws TransformerException if an error occurred while formatting the output XML.
     */
    public void run(final File output) throws IOException, SAXException, TransformerException {
        process(document.getDocumentElement());
        tableOfContent.appendChild(document.createTextNode(LINE_SEPARATOR));
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "about:legacy-compat");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(new DOMSource(document), new StreamResult(output));
    }
}
