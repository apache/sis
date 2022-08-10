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
package org.apache.sis.filter;

import java.util.List;
import java.util.ArrayList;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.CharSequences.*;


/**
 * Basic support of X-Path in {@link PropertyValue} expression.
 * This is intended to be only a lightweight support, not a replacement for {@link javax.xml.xpath} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.4
 * @module
 */
final class XPath extends Static {
    /**
     * The separator between path components.
     */
    public static final char SEPARATOR = '/';

    /**
     * Do not allow instantiation of this class.
     */
    private XPath() {
    }

    /**
     * Splits the given URL around the {@code '/'} separator, or returns {@code null} if there is no separator.
     * By convention if the URL is absolute, then the leading {@code '/'} character is kept in the first element.
     * For example {@code "/∗/property"} is splitted as two elements: {@code "/∗"} and {@code "property"}.
     *
     * <p>This method trims the whitespaces of components except the last one (the tip),
     * for consistency with the case where this method returns {@code null}.</p>
     *
     * @param  xpath  the URL to split.
     * @return the splitted URL with the heading separator kept in the first element, or {@code null}
     *         if there is no separator. If non-null, the list always contains at least one element.
     * @throws IllegalArgumentException if the XPath contains at least one empty component.
     */
    static List<String> split(final String xpath) {
        int next = xpath.indexOf(SEPARATOR);
        if (next < 0) {
            return null;
        }
        final List<String> components = new ArrayList<>(4);
        int start = skipLeadingWhitespaces(xpath, 0, next);
        if (start < next) {
            // No leading '/' (the characters before it are a path element, added below).
            components.add(xpath.substring(start, skipTrailingWhitespaces(xpath, start, next)));
            start = ++next;
        } else {
            // Keep the `start` position on the leading '/'.
            next++;
        }
        while ((next = xpath.indexOf(SEPARATOR, next)) >= 0) {
            components.add(trimWhitespaces(xpath, start, next).toString());
            start = ++next;
        }
        components.add(xpath.substring(start));         // No whitespace trimming.
        if (components.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedXPath_1, xpath));
        }
        return components;
    }
}
