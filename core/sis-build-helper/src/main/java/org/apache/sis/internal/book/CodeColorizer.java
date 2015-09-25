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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Apply syntax colorization on Java code. This class is different than most other colorization tools
 * since its apply different colors depending on whether a word is known to be defined in an OGC/ISO
 * standard, in GeoAPI or in Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class CodeColorizer {
    /**
     * Lists of Java keywords.
     */
    public static final Set<String> JAVA_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "abstract", "continue", "for",        "new",        "switch",
        "assert",   "default",  "goto",       "package",    "synchronized",
        "boolean",  "do",       "if",         "private",    "this",
        "break",    "double",   "implements", "protected",  "throw",
        "byte",     "else",     "import",     "public",     "throws",
        "case",     "enum",     "instanceof", "return",     "transient",
        "catch",    "extends",  "int",        "short",      "try",
        "char",     "final",    "interface",  "static",     "void",
        "class",    "finally",  "long",       "strictfp",   "volatile",
        "const",    "float",    "native",     "super",      "while",
        /* literals: */ "true", "false", "null")));

    /**
     * Returns all nodes in the given list as an array. This method is used for getting a snapshot
     * of the list before to modify it (for example before the elements are moved to another node).
     */
    static Node[] toArray(final NodeList nodes) {
        final Node[] children = new Node[nodes.getLength()];
        for (int i=0; i<children.length; i++) {
            children[i] = nodes.item(i);
        }
        return children;
    }

    /**
     * The origin of an identifier.
     */
    private static enum Origin {
        OGC("OGC"), GEOAPI("GeoAPI"), SIS("SIS");

        final String style;
        private Origin(final String style) {
            this.style = style;
        }
    };

    /**
     * Map pre-defined identifiers to their origin.
     */
    private final Map<String,Origin> identifierOrigins;

    /**
     * The object to use for creating nodes.
     */
    private final Document document;

    /**
     * Creates a new color colorizer.
     *
     * @param document the object to use for creating nodes.
     * @throws IOException if an error occurred while reading the list of pre-defined identifiers.
     * @throws BookException if an identifier is defined twice.
     */
    public CodeColorizer(final Document document) throws IOException, BookException {
        this.document = document;
        identifierOrigins = new HashMap<>(1000);
        for (final Origin origin : Origin.values()) {
            final String filename;
            switch (origin) {
                case OGC:    filename = "OGC.txt";    break;
                case GEOAPI: filename = "GeoAPI.txt"; break;
                default: continue;
            }
            final InputStream in = CodeColorizer.class.getResourceAsStream(filename);
            if (in == null) {
                throw new FileNotFoundException(filename);
            }
            try (final BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (identifierOrigins.put(line, origin) != null) {
                        throw new BookException(line + " is defined twice.");
                    }
                }
            }
        }
    }

    /**
     * Applies emphasing on the words found in all text node of the given node.
     *
     * @param  parent the root element where to put Java keywords in bold characters.
     *         This is typically a {@code <pre>} or {@code <code>} element.
     * @param  type {@code "xml"} if the element to process is XML rather than Java code.
     * @throws BookException if an element can not be processed.
     */
    public void highlight(final Node parent, final String type) throws BookException {
        final boolean isXML = "xml".equals(type);
        final boolean isJava = !isXML;   // Future version may add more choices.
        Element syntaticElement = null;  // E.g. comment block or a String.
        String  stopCondition   = null;  // Identify 'syntaticElement' end.
        for (final Node node : toArray(parent.getChildNodes())) {
            /*
             * The following condition happen only if a quoted string or a comment started in a previous
             * node and is continuing in the current node. In such case we need to transfer everything we
             * found into the 'syntaticElement' node, until we found the 'stopCondition'.
             */
            if (stopCondition != null) {
                if (node.getNodeType() != Node.TEXT_NODE) {
                    syntaticElement.appendChild(node);  // Also remove from its previous position.
                    continue;
                }
                final String text = node.getTextContent();
                int lower = text.indexOf(stopCondition);
                if (lower >= 0) {
                    lower += stopCondition.length();
                    stopCondition = null;
                } else {
                    lower = text.length();
                }
                syntaticElement.appendChild(document.createTextNode(text.substring(0, lower)));
                node.setTextContent(text.substring(lower));
                // Continue below in case there is some remaining characters to analyse.
            }
            /*
             * The following is the usual code path where we search for Java keywords, OGC/ISO, GeoAPI and SIS
             * identifiers, and where the quoted strings and the comments are recognized.
             */
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE: {
                    highlight(node, type);
                    break;
                }
                case Node.TEXT_NODE: {
                    final String text = node.getTextContent();
                    int nextSubstringStart = 0, lower = 0;
                    while (lower < text.length()) {
                        int c = text.codePointAt(lower);
                        if (!Character.isJavaIdentifierStart(c)) {
                            if (c == '"') {
                                stopCondition = "\"";
                                syntaticElement = document.createElement("i");
                            } else if (isJava && text.regionMatches(lower, "/*", 0, 2)) {
                                stopCondition = "*/";
                                syntaticElement = document.createElement("code");
                                syntaticElement.setAttribute("class", "comment");
                            } else if (isJava && text.regionMatches(lower, "//", 0, 2)) {
                                stopCondition = "\n";
                                syntaticElement = document.createElement("code");
                                syntaticElement.setAttribute("class", "comment");
                            } else if (isXML && text.regionMatches(lower, "<!--", 0, 4)) {
                                stopCondition = "-->";
                                syntaticElement = document.createElement("code");
                                syntaticElement.setAttribute("class", "comment");
                            } else {
                                lower += Character.charCount(c);
                                continue;  // "Ordinary" character: scan next characters.
                            }
                            /*
                             * Found the begining of a comment block or a string. Search where that block ends
                             * (it may be in another node) and store all text between the current position and
                             * the end into 'syntaticElement'.
                             */
                            if (nextSubstringStart != lower) {
                                parent.insertBefore(document.createTextNode(text.substring(nextSubstringStart, lower)), node);
                            }
                            nextSubstringStart = lower;
                            lower = text.indexOf(stopCondition, lower+1);
                            if (lower >= 0) {
                                lower += stopCondition.length();
                                stopCondition = null;
                            } else {
                                lower = text.length();
                                // Keep stopCondition; we will need to search for it in next nodes.
                            }
                            syntaticElement.setTextContent(text.substring(nextSubstringStart, lower));
                            parent.insertBefore(syntaticElement, node);
                            nextSubstringStart = lower;
                        } else {
                            /*
                             * Found the beginning of a Java identifier. Search where it ends.
                             */
                            int upper = lower;
                            while (Character.isJavaIdentifierPart(c)) {
                                upper += Character.charCount(c);
                                if (upper >= text.length()) {
                                    break;
                                }
                                c = text.codePointAt(upper);
                            }
                            /*
                             * The following code is executed for each word which is a valid Java identifier.
                             * Different kind of emphase may be applied: bold for Java keywords, some colors
                             * for OGC/ISO classes, other colors for SIS classes, etc.
                             */
                            final String word = text.substring(lower, upper);
                            Element emphase = null;
                            if (JAVA_KEYWORDS.contains(word)) {
                                emphase = document.createElement("b");
                            } else if (isJava) {
                                final Origin origin = identifierOrigins.get(word);
                                if (origin != null) {
                                    emphase = document.createElement("code");
                                    emphase.setAttribute("class", origin.style);
                                }
                            }
                            if (emphase != null) {
                                emphase.setTextContent(word);
                                if (nextSubstringStart != lower) {
                                    parent.insertBefore(document.createTextNode(text.substring(nextSubstringStart, lower)), node);
                                }
                                parent.insertBefore(emphase, node);
                                nextSubstringStart = upper;
                            }
                            lower = upper;
                        }
                    }
                    node.setTextContent(text.substring(nextSubstringStart));
                }
            }
        }
    }
}
