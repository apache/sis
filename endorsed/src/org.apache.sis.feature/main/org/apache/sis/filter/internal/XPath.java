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
package org.apache.sis.filter.internal;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;

import static org.apache.sis.util.CharSequences.*;


/**
 * Basic support of XPath in {@code ValueReference} expression.
 * This is intended to be only a lightweight support, not a replacement for {@link javax.xml.xpath} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class XPath {
    /**
     * The separator between path components.
     * Should not be used for URL or Unix name separator, even if the character is the same.
     * We use this constant for identifying locations in the code where there is some XPath parsing.
     */
    public static final char SEPARATOR = '/';

    /**
     * The prefix for names qualified by their URI instead of prefix.
     * Example: {@code "Q{http://example.com/foo/bar}feature/property"}.
     *
     * @see <a href="https://www.w3.org/TR/xpath-31/#doc-xpath31-URIQualifiedName">XPath 3.1 qualified name</a>
     */
    private static final char BRACED_URI_PREFIX = 'Q';

    /**
     * The characters used as delimiters for braced URI literals.
     * The open bracket should be prefixed by {@value #BRACED_URI_PREFIX},
     * but this is optional in this implementation.
     */
    private static final char OPEN = '{', CLOSE = '}';

    /**
     * The components of the XPath before the tip, or {@code null} if none.
     * This list, if non-null, contains at least one element but not the {@linkplain #tip}.
     */
    public List<String> path;

    /**
     * The tip of the XPath.
     * This is the part after the last occurrence of {@value #SEPARATOR},
     * unless that occurrence was inside curly brackets for qualified name.
     */
    public String tip;

    /**
     * Whether the XPath has a leading {@value #SEPARATOR} character.
     */
    public boolean isAbsolute;

    /**
     * Creates a XPath with the given path components.
     * The components are assumed already parsed (no braced URI literals).
     *
     * @param  path  components of the XPath before the tip, or {@code null} if none.
     * @param  tip   the last component of the XPath.
     */
    public XPath(final String[] path, final String tip) {
        if (path != null) {
            this.path = Arrays.asList(path);
        }
        this.tip = tip;
    }

    /**
     * Splits the given XPath around the {@code '/'} separator, except for the part between curly brackets.
     * If a leading {@code '/'} character is present, it is removed and {@link #isAbsolute} is set to true.
     * This method trims the whitespaces of all components.
     *
     * @param  xpath  the XPath to split.
     * @throws IllegalArgumentException if the XPath contains at least one empty component.
     */
    public XPath(final String xpath) {
        /*
         * Check whether the XPath is absolute.
         * This is identified by a leading "/".
         */
        final int length = xpath.length();
        int start  = skipLeadingWhitespaces(xpath, 0, length);
        if (start >= length) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "xpath"));
        }
        if (xpath.charAt(start) == SEPARATOR) {
            start = skipLeadingWhitespaces(xpath, start+1, length);
            isAbsolute = true;
        }
        String namespace;
        for (;;) {
            /*
             * Check for braced URI literal, for example "Q{http://example.com}".
             * The "Q" prefix is mandated by XPath 3.1 specification, but optional in this implementation.
             * Any other prefix is considered an error, as the brackets may have another signification.
             */
            namespace = null;
            int open = xpath.indexOf(OPEN, start);
            if (open >= 0) {
                final int before = skipLeadingWhitespaces(xpath, start, open);
                if (before != open && (before != open-1 || xpath.charAt(before) != BRACED_URI_PREFIX)) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath.substring(before)));
                }
                final int close = xpath.indexOf(CLOSE, ++open);
                if (close < 0) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingCharacterInElement_2, xpath.substring(before), CLOSE));
                }
                namespace = trimWhitespaces(xpath, open, close).toString();
                if (namespace.indexOf(OPEN) >= 0) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCharacter_2, namespace, OPEN));
                }
                start = close + 1;
            }
            /*
             * Add the name component before the next "/" character.
             * The loop is repeated for all components except the last one.
             */
            final int next = xpath.indexOf(SEPARATOR, start);
            if (next < 0) break;
            if (path == null) {
                path = new ArrayList<>(4);
            }
            path.add(toQualifiedName(namespace, xpath, start, next));
            start = next + 1;
        }
        /*
         * The remaining is the tip, stored separately.
         */
        tip = toQualifiedName(namespace, xpath, start, length);
        if (tip.isEmpty() || (path != null && path.stream().anyMatch(String::isEmpty))) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath));
        }
    }

    /**
     * Creates a qualified name with a name space (optional) followed by a name extracted
     * from a sub-string of the XPath. Leading and trailing white spaces are omitted.
     *
     * @param  namespace  the name space, or {@code null} if none.
     * @param  xpath      the XPath containing the name.
     * @param  start      index of the first character of the name.
     * @param  end        index after the last character of the name.
     * @return {@code namespace:name}, or only the name if the name spae is null.
     */
    private static String toQualifiedName(final String namespace, final String xpath, final int start, final int end) {
        String name = trimWhitespaces(xpath, start, end).toString();
        if (namespace != null) {
            name = namespace + DefaultNameSpace.DEFAULT_SEPARATOR + name;
        }
        return name;
    }

    /**
     * Reformat a name component as a braced URI, if needed.
     *
     * @param  component  the name component to reformat.
     * @param  sb  where to write the name component.
     * @return the builder, for chained method calls.
     */
    private static StringBuilder toBracedURI(final String component, final StringBuilder sb) {
        final int end = component.lastIndexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
        if (end >= 0 && component.lastIndexOf(SEPARATOR, end) >= 0) {
            return sb.append(BRACED_URI_PREFIX).append(OPEN).append(component, 0, end).append(CLOSE)
                     .append(component, end+1, component.length());
        } else {
            return sb.append(component);
        }
    }

    /**
     * Rewrites the XPath from its components and the tip.
     *
     * @return the XPath.
     */
    @Override
    public String toString() {
        if (!isAbsolute && path == null && tip.indexOf(SEPARATOR) < 0) {
            return tip;
        }
        final var sb = new StringBuilder(40);
        if (isAbsolute) sb.append(SEPARATOR);
        if (path != null) {
            for (final String component : path) {
                toBracedURI(component, sb).append(SEPARATOR);
            }
        }
        return toBracedURI(tip, sb).toString();
    }
}
