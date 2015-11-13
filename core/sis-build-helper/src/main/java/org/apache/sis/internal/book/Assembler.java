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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
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

// Share a convenience method.
import static org.apache.sis.internal.book.CodeColorizer.toArray;


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
 *   <li>Replace the {@code <!-- TOC -->} comment by a table of content generated from all {@code <h1>}, {@code <h2>}, <i>etc.</i>
 *       found in the document.</li>
 *
 *   <li>Generate below {@code <h1>} elements a table of content containing only the sections in that chapter.</li>
 *
 *   <li>Generate below {@code <h1>} elements the navigation bar with links to the previous and next chapters.</li>
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
     * Minimal number of characters in a Java identifier before to allows a line break before the next identifier.
     * This value if used in expressions like {@code foo.bar()} for deciding whether or not we accept line break
     * between {@code foo} and {@code .bar()}.
     */
    private static final int MINIMAL_LENGTH_BEFORE_BREAK = 3;

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
     * The node where to write the table of content for the whole document.
     */
    private final Element tableOfContent;

    /**
     * The node where to write the table of content for the current chapter.
     */
    private Element tableOfChapterContent;

    /**
     * The {@code title} attributes found in abbreviations.
     */
    private final Map<String,String> abbreviations = new HashMap<String,String>();

    /**
     * Whether we found an abbreviation after the last {@code h?} element.
     * This is used in order to avoid inserting too many abbreviation title.
     */
    private final Set<String> writtenAbbreviations = new HashSet<String>();

    /**
     * Section numbers, incremented when a new {@code <h1>}, {@code <h2>}, <i>etc.</i> element is found.
     */
    private final int[] sectionNumbering = new int[9];

    /**
     * The last {@code <h1>} element found while parsing the document, or {@code null} if none.
     */
    private Element previousChapter;

    /**
     * Localized resources.
     */
    private final ResourceBundle resources;

    /**
     * Helper class for applying colors on content of {@code <pre>} and {@code <code>} elements.
     */
    private final CodeColorizer colorizer;

    /**
     * Creates a new assembler for the given input and output files.
     *
     * @param  input  the input file (e.g. {@code "site/book/en/body.html"}).
     * @param  locale the locale for the message to generates in HTML code.
     * @throws ParserConfigurationException if this constructor can not build the XML document.
     * @throws IOException if an error occurred while reading the file.
     * @throws SAXException if an error occurred while parsing the XML.
     * @throws BookException if a logical error occurred while initializing the assembler.
     */
    public Assembler(final File input, final Locale locale)
            throws ParserConfigurationException, IOException, SAXException, BookException
    {
        resources = ResourceBundle.getBundle("org.apache.sis.internal.book.Resources", locale);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // No setXIncludeAware(true) -  we will handle <xi:include> elements ourself.
        factory.setNamespaceAware(true);
        inputDirectory = input.getParentFile();
        builder        = factory.newDocumentBuilder();
        document       = load(input.getName());
        colorizer      = new CodeColorizer(document);
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
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE: {
                if ("pre".equals(node.getNodeName())) {
                    return;
                }
                break;
            }
            case Node.TEXT_NODE: {
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
    private Node replaceByBody(final String filename, final Node toReplace) throws IOException, SAXException, BookException {
        final NodeList nodes = load(filename).getElementsByTagName("body");
        if (nodes.getLength() != 1) {
            throw new BookException(filename + ": expected exactly one <body> element.");
        }
        final Node element = document.createElement("section");
        toReplace.getParentNode().replaceChild(element, toReplace);
        for (Node child : toArray(nodes.item(0).getChildNodes())) {
            child = document.importNode(child, true);   // document.adoptNode(child) would have been more efficient but does not seem to work.
            if (child == null) {
                throw new BookException("Failed to copy subtree.");
            }
            element.appendChild(child);
        }
        return element;
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
     *
     * @param index {@code true} for including the {@code <h1>}, <i>etc.</i> texts in the Table Of Content (TOC).
     *        This is set to {@code false} when parsing the content of {@code <aside>} or {@code <article>} elements.
     */
    private void process(Node node, boolean index) throws IOException, SAXException, BookException {
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
                /* switch (name) */ {
                    if (name.equals("xi:include")) {
                        node = replaceByBody(((Element) node).getAttribute("href"), node);
                    }
                    else if (name.equals("aside") || name.equals("article")) {
                        index = false;
                    }
                    else if (name.equals("abbr")) {
                        processAbbreviation((Element) node);
                    }
                    else if (name.equals("pre")) {
                        colorizer.highlight(node, ((Element) node).getAttribute("class"));
                    }
                    else if (name.equals("code")) {
                        if (!((Element) node).hasAttribute("class")) {
                            final String style = colorizer.styleForSingleIdentifier(node.getTextContent());
                            if (style != null) {
                                ((Element) node).setAttribute("class", style);
                            }
                        }
                        String text = insertWordSeparator(node.getTextContent());
                        if (text != null) {
                            node.setTextContent(text);
                        }
                        return; // Do not scan recursively the <code> text content.
                    }
                    else {
                        if (name.length() == 2 && name.charAt(0) == 'h') {
                            final int c = name.charAt(1) - '0';
                            if (c >= 1 && c <= 9) {
                                writtenAbbreviations.clear();
                                if (index) {
                                    sectionNumbering[c-1]++;
                                    Arrays.fill(sectionNumbering, c, sectionNumbering.length, 0);
                                    appendToTableOfContent(tableOfContent, c, (Element) node);
                                    if (c == 1) {
                                        linkToSiblingChapters((Element) node);
                                        tableOfChapterContent = document.createElement("ul");
                                        tableOfChapterContent.setAttribute("class", "toc");
                                        final Node nav = document.createElement("nav");
                                        nav.appendChild(document.createTextNode(resources.getString("this-chapter")));
                                        nav.appendChild(tableOfChapterContent);
                                        Node insertionPoint = node.getParentNode();             // The <header> element.
                                        do insertionPoint = insertionPoint.getNextSibling();    // The first paragraph.
                                        while (insertionPoint.getNodeType() == Node.TEXT_NODE);
                                        insertionPoint.getParentNode().insertBefore(nav, insertionPoint);
                                        insertLineSeparator(insertionPoint);
                                    } else {
                                        appendToTableOfContent(tableOfChapterContent, c-1, (Element) node);
                                    }
                                    prependSectionNumber(c, node);  // Only after insertion in TOC.
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        for (final Node child : toArray(node.getChildNodes())) {
            process(child, index);
        }
    }

    /**
     * Prepend the current section numbers to the given node.
     * The given node shall be a {@code <h1>}, {@code <h2>}, <i>etc.</i> element.
     *
     * @param level 1 if {@code head} is {@code <h1>}, 2 if {@code head} is {@code <h2>}, <i>etc.</i>
     * @param head  the {@code <h1>}, {@code <h2>}, {@code <h3>}, {@code <h4>}, <i>etc.</i> element.
     */
    private void prependSectionNumber(final int level, final Node head) {
        final Element number = document.createElement("span");
        number.setAttribute("class", "section-number");
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<level; i++) {
            buffer.append(sectionNumbering[i]).append('.');
        }
        number.setTextContent(buffer.toString());
        head.insertBefore(document.createTextNode(" "), head.getFirstChild());
        head.insertBefore(number, head.getFirstChild());
    }

    /**
     * Appends the given header to the table of content.
     *
     * @param appendTo   The root node of the table of content where to append a new line.
     * @param level      Level of the {@code <h1>}, {@code <h2>}, {@code <h3>}, <i>etc.</i> element found.
     * @param referenced The {@code <h1>}, {@code <h2>}, {@code <h3>}, <i>etc.</i> element to reference.
     */
    private void appendToTableOfContent(Node appendTo, int level, final Element referenced) throws BookException {
        final String id = referenced.getAttribute("id");
        final String text = referenced.getTextContent();
        if (id.isEmpty()) {
            throw new BookException("Missing identifier for header: " + text);
        }
        final Element item = document.createElement("li");
        item.appendChild(createLink(id, text));
        while (--level > 0) {
            appendTo = appendTo.getLastChild();     // Last <li> element.
            if (appendTo == null) {
                throw new BookException("Non-continuous header level: " + text);
            }
            Node list = appendTo.getLastChild();    // Search for <ul> element in above <li>.
            if (list == null || !"ul".equals(list.getNodeName())) {
                list = document.createElement("ul");
                appendTo.appendChild(list);
            }
            appendTo = list;
        }
        appendTo.appendChild(document.createTextNode(LINE_SEPARATOR));
        appendTo.appendChild(item);
    }

    /**
     * Generates a {@code <nav>} element below the given {@code <h1>} element with navigation links
     * to previous and next chapters.
     *
     * @param head the {@code <h1>} element.
     */
    private void linkToSiblingChapters(final Element head) throws BookException {
        final Element links = document.createElement("div");
        links.setAttribute("class", "chapter-links");
        if (previousChapter != null) {
            /*
             * Generate the link to previous chapter with the following pattern:
             *
             *     <div class="previous-chapter">⬅ <a href="#id">Previous chapter</a></div>
             */
            final Element previous = document.createElement("div");
            previous.setAttribute("class", "previous-chapter");
            previous.appendChild(document.createTextNode("⬅ "));
            previous.appendChild(createLink(previousChapter.getAttribute("id"), resources.getString("previous-chapter")));
            links.appendChild(previous);
            /*
             * Update the previous <h1> element with the link to the next chapter,
             * which is the given 'head' element. The pattern is:
             *
             *     <div class="next-chapter"><a href="#id">Next chapter</a> ➡</div>
             */
            final Element next = document.createElement("div");
            next.setAttribute("class", "next-chapter");
            next.appendChild(createLink(head.getAttribute("id"), resources.getString("next-chapter")));
            next.appendChild(document.createTextNode(" ➡"));

            Node previousNav = previousChapter;
            previousNav = previousNav.getNextSibling();     // The line separator after <h1>.
            previousNav = previousNav.getNextSibling();     // The <nav> element.
            previousNav = previousNav.getFirstChild();      // The <div class="chapter-links"> element.
            previousNav.appendChild(next);
        }
        final Element nav = document.createElement("nav");
        nav.appendChild(links);
        head.getParentNode().insertBefore(nav, head.getNextSibling());
        insertLineSeparator(nav);
        previousChapter = head;
    }

    /**
     * Creates a {@code <a href="reference">text</a>} node.
     */
    private Element createLink(final String reference, final String text) throws BookException {
        if (reference.isEmpty()) {
            throw new BookException("Missing reference for: " + text);
        }
        final Element ref = document.createElement("a");
        ref.setAttribute("href", "#" + reference);
        ref.setTextContent(text);
        return ref;
    }

    /**
     * Allows word break before the code in expression like {@code Class.method()}.
     * If there is nothing to change in the given text, returns {@code null}.
     */
    private static String insertWordSeparator(String text) {
        StringBuilder buffer = null;
        for (int i=text.length() - 1; --i > MINIMAL_LENGTH_BEFORE_BREAK;) {
            if (text.charAt(i) == '.' && Character.isJavaIdentifierStart(text.charAt(i+1))) {
                final char b = text.charAt(i-1);
                if (Character.isJavaIdentifierPart(b) || b == ')') {
                    /*
                     * Verifiy if the element to eventually put on the next line is a call to a method.
                     * For now we split only calls to method for avoiding to split for example every
                     * elements in a package name.
                     */
                    for (int j=i; ++j < text.length();) {
                        final char c = text.charAt(j);
                        if (!Character.isJavaIdentifierPart(c)) {
                            if (c == '(') {
                                /*
                                 * Found a call to a method. But we also require the word before it
                                 * to have more than 3 letters.
                                 */
                                for (j = i; Character.isJavaIdentifierPart(text.charAt(--j));) {
                                    if (j == i - MINIMAL_LENGTH_BEFORE_BREAK) {
                                        if (buffer == null) {
                                            buffer = new StringBuilder(text);
                                        }
                                        buffer.insert(i, '\u200B');     // Zero-width space.
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return (buffer != null) ? buffer.toString() : null;
    }

    /**
     * Inserts a line separator just before the given node.
     */
    private void insertLineSeparator(final Node insertionPoint) {
        insertionPoint.getParentNode().insertBefore(document.createTextNode(LINE_SEPARATOR), insertionPoint);
    }

    /**
     * Assembles the document and writes to the destination.
     *
     * @param  output the output file (e.g. {@code "site/content/en/developer-guide.html"}).
     * @throws IOException if an error occurred while reading or writing file.
     * @throws SAXException if an error occurred while parsing an input XML.
     * @throws BookException if an error was found in the content of the XML file.
     * @throws TransformerException if an error occurred while formatting the output XML.
     */
    public void run(final File output) throws IOException, SAXException, BookException, TransformerException {
        process(document.getDocumentElement(), true);
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
