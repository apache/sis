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
package org.apache.sis.internal.util;

import org.apache.sis.util.Static;

import static org.apache.sis.util.CharSequences.*;
import static org.apache.sis.internal.util.DefinitionURI.regionMatches;


/**
 * Utility methods related to x-paths.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.8
 * @module
 */
public final class XPaths extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private XPaths() {
    }

    /**
     * If the given character sequences seems to be a URI, returns the presumed end of that URN.
     * Otherwise returns -1.
     * Examples:
     * <ul>
     *   <li>{@code "urn:ogc:def:uom:EPSG::9001"}</li>
     *   <li>{@code "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"}</li>
     * </ul>
     *
     * @param  uri     the URI candidate to verify.
     * @param  offset  index of the first character to verify.
     * @return index after the last character of the presumed URI, or -1 if this
     *         method thinks that the given character sequence is not a URI.
     *
     * @since 0.8
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
                        if (Character.isWhitespace(c)) break;           // Not supposed to be valid, but be lenient.
                        if (parenthesis != 0) break;
                        break scan;                                     // Non-valid character outside parenthesis.
                    }
                }
            }
            offset += Character.charCount(c);
        }
        return isURI ? offset : -1;
    }

    /**
     * Parses a URL which contains a pointer to a XML fragment.
     * The current implementation recognizes the following types:
     *
     * <ul>
     *   <li>{@code uom} for Unit Of Measurement (example:
     *       {@code "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"})</li>
     * </ul>
     *
     * @param  type  the object type.
     * @param  url   the URL to parse.
     * @return the reference, or {@code null} if none.
     */
    public static String xpointer(final String type, final String url) {
        if (type.equals("uom")) {
            final int f = url.indexOf('#');
            if (f >= 1) {
                /*
                 * For now we accept any path as long as it ends with the "gmxUom.xml" file
                 * because resources may be hosted on different servers, or the path may be
                 * relative instead than absolute.
                 */
                int i = url.lastIndexOf('/', f-1) + 1;
                if (regionMatches("gmxUom.xml", url, i, f) || regionMatches("ML_gmxUom.xml", url, i, f)) {
                    /*
                     * The fragment should typically be of the form "xpointer(//*[@gml:id='m'])".
                     * However sometime we found no "xpointer", but directly the unit instead.
                     */
                    i = url.indexOf('(', f+1);
                    if (i >= 0 && regionMatches("xpointer", url, f+1, i)) {
                        i = url.indexOf("@gml:id=", i+1);
                        if (i >= 0) {
                            i = skipLeadingWhitespaces(url, i+8, url.length());     // 8 is the length of "@gml:id="
                            final int c = url.charAt(i);
                            if (c == '\'' || c == '"') {
                                final int s = url.indexOf(c, ++i);
                                if (s >= 0) {
                                    return trimWhitespaces(url, i, s).toString();
                                }
                            }
                        }
                    } else {
                        return trimWhitespaces(url, f+1, url.length()).toString();
                    }
                }
            }
        }
        return null;
    }
}
