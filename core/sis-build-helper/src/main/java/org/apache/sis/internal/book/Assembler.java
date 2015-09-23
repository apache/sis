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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
 *   <li>Comments of the form {@code <!-- file:introduction.html -->} are replaced by content
 *       of the {@code <body>} element in the given file.</li>
 *
 *   <li>{@code <abbr>} elements without {@code title} attribute automatically get the last title used for that abbreviation.
 *        However this automatic insertion is performed only for the first occurrence of that abbreviation after a {@code h?}
 *        element.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 */
public final class Assembler {
    /**
     * The prefix used in comments for instructing {@code Assembler} to insert the content of another file.
     * This is used in the source HTML file like the following example:
     *
     * <pre>{@literal <!-- file:introduction.html -->}</pre>
     */
    private static final String INCLUDE_PREFIX = "file:";

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
        inputDirectory = input.getParentFile();
        builder        = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document       = load(input.getName());
        tableOfContent = document.createElement("ul");
        tableOfContent.setAttribute("class", "toc");
    }

    /**
     * Loads the XML document from the given file in the same directory than the input file given to the constructor.
     */
    private Document load(final String filename) throws IOException, SAXException {
        final Document include;
        try (final InputStream in = new FileInputStream(new File(inputDirectory, filename))) {
            include = builder.parse(in);
        }
        builder.reset();
        return include;
    }

    /**
     * Copies the body of the given source HTML file in-place of the given target node.
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
                if (text.startsWith(INCLUDE_PREFIX)) {
                    node = replaceByBody(text.substring(INCLUDE_PREFIX.length()), node);
                } else if (text.equals("TOC")) {
                    node.getParentNode().replaceChild(tableOfContent, node);
                    return;
                }
                break;
            }
            case Node.ELEMENT_NODE: {
                final String name = node.getNodeName();
                switch (name) {
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
        final NodeList nodes = node.getChildNodes();
        final int length = nodes.getLength();
        for (int i=0; i<length; i++) {
            process(nodes.item(i));
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
        node.appendChild(document.createTextNode("\n"));
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
        tableOfContent.appendChild(document.createTextNode("\n"));
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(output));
    }
}
