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
package org.apache.sis.storage.base;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.DefinitionURI;


/**
 * Heuristic rules for determining whether an authority code seems to be actually a file path,
 * or a code defined in the URN namespace, or a simple code.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public enum CodeType {
    /**
     * The code is a filename like {@code "/path/to/file"} or {@code "C:\path\to\file"}.
     * Could also be a directory name.
     */
    FILE(false, false),

    /**
     * The code is a URL like {@code "http:"} or {@code "file:"},
     * with the exception of HTTP in the "www.opengis.net" domain.
     * The latter case is identified by {@link #HTTP_OGC} instead of this enum.
     */
    URL(false, true),

    /**
     * The code is an authority code defined in the {@code "urn:"} namespace.
     */
    URN(true, true),

    /**
     * The code is an URL in the {@code "http://www.opengis.net"} namespace.
     */
    HTTP_OGC(true, true),

    /**
     * The code is not defined in the URN namespace but is nevertheless presumed to be an authority code.
     * Example: {@code "EPSG:4326"}.
     */
    IDENTIFIER(true, false),

    /**
     * Cannot resolve whether the code is a local file like {@code "myfile.wkt"} or an identifier without
     * authority like {@code "4326"}. Such code without cannot be decoded by {@code CRS.forCode(String)},
     * but may be understood by a more specific authority factory.
     */
    UNKNOWN(false, false);

    /**
     * Whether the code may be understood by the {@link org.apache.sis.referencing.CRS#forCode(String)}.
     * A value of {@code true} does not guaranteed the code is valid. It only said that there is some
     * chances that the code is valid.
     */
    public final boolean isAuthorityCode;

    /**
     * Whether the code is a <abbr>URL</abbr> or <abbr>URN</abbr>.
     * This is not necessarily the <abbr>URI</abbr> of an authority code.
     */
    public final boolean isURI;

    /**
     * Creates a new enum value.
     */
    private CodeType(final boolean isAuthorityCode, final boolean isURI) {
        this.isAuthorityCode = isAuthorityCode;
        this.isURI = isURI;
    }

    /**
     * The types for a list of known protocols. Protocol must be lower-cases.
     *
     * <p>This map is used for resolving ambiguity between "PROTOCOL:FILE" and "AUTHORITY:CODE".
     * If the given path begins with a file separator like "PROTOCOL:/PATH/FILE", then the path
     * is presumed to be a URL even if the protocol is not in this map.</p>
     */
    private static final Map<String,CodeType> FOR_PROTOCOL;
    static {
        FOR_PROTOCOL = new HashMap<>();
        FOR_PROTOCOL.put("urn",           CodeType.URN);
        FOR_PROTOCOL.put(Constants.HTTP,  CodeType.HTTP_OGC);   // Will actually need verification.
        FOR_PROTOCOL.put(Constants.HTTPS, CodeType.HTTP_OGC);   // Will actually need verification.
        FOR_PROTOCOL.put("shttp",         CodeType.HTTP_OGC);   // Not widely used but nevertheless exist.
        for (final String p : new String[] {"cvs", "dav", "file", "ftp", "git", "jar", "nfs", "sftp", "ssh", "svn"}) {
            if (FOR_PROTOCOL.put(p, CodeType.URL) != null) {
                throw new AssertionError(p);
            }
        }
    }

    /**
     * Infers the type for the given authority code.
     *
     * @param  codeOrPath  the code or file path.
     * @return whether the given argument seems to be a file path, URL, URN of authority code.
     */
    public static CodeType guess(final String codeOrPath) {
        final int length = codeOrPath.length();
        final int start  = CharSequences.skipLeadingWhitespaces(codeOrPath, 0, length);
        int separator    = codeOrPath.indexOf(':', start);
        final int end    = CharSequences.skipTrailingWhitespaces(codeOrPath, start, separator);
        if (end <= start) {
            // Check for presence of file separator, including the Unix and Windows ones.
            if (codeOrPath.contains(File.separator) || codeOrPath.indexOf('/') >= 0 || codeOrPath.indexOf('\\') >= 0) {
                return FILE;
            }
            return UNKNOWN;
        }
        /*
         * Characters in the [start … end) range may be the authority ("EPSG", "CRS", "AUTO2", etc.),
         * the protocol ("http", "ftp", etc.) or a drive letter on a Windows system ("A", "C", etc.).
         * Skip following spaces and dots so if codeOrPath="C:.\path", then the separator is at the
         * position of \.
         */
        char c;
        do {
            separator = CharSequences.skipLeadingWhitespaces(codeOrPath, separator+1, length);
            if (separator >= length) {
                return FILE;            // Relative directory name, for example "C:..".
            }
            c = codeOrPath.charAt(separator);
        } while (c == '.');
        /*
         * If the ':' if followed by at least one '/' (ignoring spaces and dots), then it is presumed
         * to be a URL protocol. In the special case where the protocol is "http(s)" and the domain after
         * the '/' characters is "www.opengis.net", return HTTP_OGC instead of URL.
         */
        final CodeType known = FOR_PROTOCOL.get(codeOrPath.substring(start, end).toLowerCase(Locale.US));
        if (known != null) {
            if (known != HTTP_OGC) {
                return known;
            }
            if (c == '/') {
                while (++separator < length) {
                    c = codeOrPath.charAt(separator);
                    if (c != '/') {
                        separator = CharSequences.skipLeadingWhitespaces(codeOrPath, separator, length);
                        if (CharSequences.regionMatches(codeOrPath, separator, DefinitionURI.DOMAIN, true)) {
                            separator += DefinitionURI.DOMAIN.length();
                            if (separator >= length || codeOrPath.charAt(separator) == '/') {
                                return known;
                            }
                        }
                        break;
                    }
                }
            }
            return URL;
        }
        if (c == '/') {
            return URL;
        }
        /*
         * If the ':' is followed by '\', then the part before ':' is presumed to be a Windows drive letter.
         * Example "C:\file" or "C:..\file". Note that it does NOT include "C:file" since the latter cannot
         * be distinguished from an authority code. If a relative filename is desired, use "C:.\file".
         */
        if (c == '\\' || c == File.separatorChar || end == start+1) {
            return FILE;
        }
        return IDENTIFIER;
    }
}
