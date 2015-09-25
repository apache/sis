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
import java.util.HashSet;
import java.util.Set;
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
     * The object to use for creating nodes.
     */
    private final Document document;

    /**
     * Creates a new color colorizer.
     *
     * @param document the object to use for creating nodes.
     */
    CodeColorizer(final Document document) {
        this.document = document;
    }

    /**
     * Applies emphasing on the words found in all text node of the given node.
     *
     * @param parent the root element where to put Java keywords in bold characters.
     *        This is typically a {@code <pre>} or {@code <code>} element.
     * @param isInsideQuotes if the text begin inside a quoted text.
     */
    public void highlight(final Node parent, boolean isInsideQuotes) {
        for (final Node node : toArray(parent.getChildNodes())) {
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE: {
                    highlight(node, isInsideQuotes);
                    break;
                }
                case Node.TEXT_NODE: {
                    final String text = node.getTextContent();
                    int nextSubstringStart = 0, lower = 0;
                    while (lower < text.length()) {
                        int c = text.codePointAt(lower);
                        if (isInsideQuotes || !Character.isJavaIdentifierStart(c)) {
                            if (c == '"') {
                                isInsideQuotes = !isInsideQuotes;
                            }
                            lower += Character.charCount(c);
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
