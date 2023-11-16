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

import static org.apache.sis.util.CharSequences.*;


/**
 * Basic support of X-Path in {@code ValueReference} expression.
 * This is intended to be only a lightweight support, not a replacement for {@link javax.xml.xpath} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class XPath {
    /**
     * The separator between path components.
     * Should not be used for URL or Unix name separator, even if the character is the same.
     * We use this constant for identifying locations in the code where there is some X-Path parsing.
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
        int length = xpath.length();
        int start  = skipLeadingWhitespaces(xpath, 0, length);
        if (start >= length) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "xpath"));
        }
        if (xpath.charAt(start) == SEPARATOR) {
            start = skipLeadingWhitespaces(xpath, start+1, length);
            isAbsolute = true;
        }
        /*
         * Check for braced URI literal, for example "Q{http://example.com}".
         * The "Q" prefix is mandated by XPath 3.1 specification, but optional in this implementation.
         * Any other prefix is considered an error, as the brackets may have another signification.
         */
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
            final String part = trimWhitespaces(xpath, open, close).toString();
            if (part.indexOf(OPEN) >= 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCharacter_2, part, OPEN));
            }
            path = new ArrayList<>(4);
            path.add(part);
            start = close + 1;
        }
        /*
         * Add all components before the last "/" characters.
         * The remaining is the tip, stored separately.
         */
        int next;
        while ((next = xpath.indexOf(SEPARATOR, start)) >= 0) {
            if (path == null) {
                path = new ArrayList<>(4);
            }
            path.add(trimWhitespaces(xpath, start, next).toString());
            start = next + 1;
        }
        tip = trimWhitespaces(xpath, start, length).toString();
        if (tip.isEmpty() || (path != null && path.stream().anyMatch(String::isEmpty))) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath));
        }
    }

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
     * Rewrites the XPath from its components and the tip.
     *
     * @return the XPath.
     */
    @Override
    public String toString() {
        if (!isAbsolute && path == null) {
            return tip;
        }
        final var sb = new StringBuilder(40);
        if (isAbsolute) sb.append(SEPARATOR);
        if (path != null) {
            final int size = path.size();
            for (int i=0; i<size; i++) {
                final String part = path.get(i);
                if (i == 0 && part.indexOf(SEPARATOR) >= 0) {
                    sb.append(BRACED_URI_PREFIX).append(OPEN).append(part).append(CLOSE);
                } else {
                    sb.append(part).append(SEPARATOR);
                }
            }
        }
        return sb.append(tip).toString();
    }
}
