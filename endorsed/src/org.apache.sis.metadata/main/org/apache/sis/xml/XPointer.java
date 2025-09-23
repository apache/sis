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
package org.apache.sis.xml;

import static org.apache.sis.util.CharSequences.*;
import static org.apache.sis.util.internal.shared.DefinitionURI.regionMatches;


/**
 * Parsers of pointers in XPaths, adapted to the syntax found in GML documents.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum XPointer {
    /**
     * Pointer to units of measurement. Example:
     * {@code "http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"})
     */
    UOM(new String[] {"gmxUom.xml", "ML_gmxUom.xml"}, "@gml:id=");

    /**
     * The documents expected at the end of the URL, before the fragment.
     * One of those document should be present.
     */
    private final String[] documents;

    /**
     * The text prefixing the identifier.
     */
    private final String identifier;

    /**
     * Creates a new enumeration value.
     */
    private XPointer(final String[] documents, final String identifier) {
        this.documents  = documents;
        this.identifier = identifier;
    }

    /**
     * Returns the index where the document fragment starts in the given URL, or -1 if none.
     * For now we accept any path as long as it ends with the {@code "gmxUom.xml"} file (for example)
     * because resources may be hosted on different servers, or the path may be relative instead of absolute.
     */
    private int startOfFragment(final String url) {
        final int f = url.indexOf('#');
        if (f >= 1) {
            final int i = url.lastIndexOf('/', f-1) + 1;
            for (final String document : documents) {
                if (regionMatches(document, url, i, f)) {
                    return f + 1;
                }
            }
        }
        return -1;
    }

    /**
     * Parses a URL which contains a pointer to a XML fragment.
     * The current implementation recognizes the following examples:
     *
     * <ul>
     *   <li>{@link #UOM}: {@code "http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"})</li>
     * </ul>
     *
     * @param  url  the URL to parse.
     * @return the reference, or {@code null} if none.
     */
    public String reference(final String url) {
        final int f = startOfFragment(url);
        if (f >= 0) {
            /*
             * The fragment should typically be of the form "xpointer(//*[@gml:id='m'])".
             * However, sometimes we found no "xpointer" but directly the unit instead.
             */
            int i = url.indexOf('(', f);
            if (i >= 0 && regionMatches("xpointer", url, f, i)) {
                i = url.indexOf(identifier, i+1);
                if (i >= 0) {
                    i = skipLeadingWhitespaces(url, i + identifier.length(), url.length());
                    final int c = url.charAt(i);
                    if (c == '\'' || c == '"') {
                        final int s = url.indexOf(c, ++i);
                        if (s >= 0) {
                            return trimWhitespaces(url, i, s).toString();
                        }
                    }
                }
            } else {
                return trimWhitespaces(url, f, url.length()).toString();
            }
        }
        return null;
    }

    /**
     * If the given character sequences seems to be a URI, returns the presumed end of that URN.
     * Otherwise returns -1.
     * Examples:
     * <ul>
     *   <li>{@code "urn:ogc:def:uom:EPSG::9001"}</li>
     *   <li>{@code "http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"}</li>
     * </ul>
     *
     * @param  uri     the URI candidate to verify.
     * @param  offset  index of the first character to verify.
     * @return index after the last character of the presumed URI, or -1 if this
     *         method thinks that the given character sequence is not a URI.
     */
    public static int endOfURI(final CharSequence uri, int offset) {
        boolean isURI = false;
        int parenthesis = 0;
        final int length = uri.length();
scan:   while (offset < length) {
            final int c = Character.codePointAt(uri, offset);
            if (!Character.isLetterOrDigit(c)) {
                switch (c) {
                    case '#':                                           // Anchor in URL, presumed followed by xpointer.
                    case ':': isURI |= (parenthesis == 0); break;       // Scheme or URN separator.
                    case '_':
                    case '-':                                           // Valid character in URL.
                    case '%':                                           // Encoded character in URL.
                    case '.':                                           // Domain name separator in URL.
                    case '/': break;                                    // Path separator, but could also be division as in "m/s".
                    case '(': parenthesis++; break;
                    case ')': parenthesis--; break;
                    default: {
                        if (Character.isSpaceChar(c)) break;            // Not supposed to be valid, but be lenient.
                        if (parenthesis != 0) break;
                        break scan;                                     // Non-valid character outside parenthesis.
                    }
                }
            }
            offset += Character.charCount(c);
        }
        return isURI ? offset : -1;
    }
}
