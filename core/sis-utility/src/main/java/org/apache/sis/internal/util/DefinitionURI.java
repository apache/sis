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

import java.util.Map;
import java.util.Collections;

import org.apache.sis.util.CharSequences;
import static org.apache.sis.util.CharSequences.*;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.util.Utilities.appendUnicodeIdentifier;


/**
 * Utility methods for parsing OGC's URI (URN or URL) in the {@code "urn:ogc:def"} namespace.
 *
 * <p>For example, all the following URIs are for the same object:</p>
 * <ul>
 *   <li>{@code "4326"} (codespace inferred by the caller)</li>
 *   <li>{@code "EPSG:4326"} (older format)</li>
 *   <li>{@code "EPSG::4326"} (often seen for similarity with URN below)</li>
 *   <li>{@code "urn:ogc:def:crs:EPSG::4326"} (version number is omitted)</li>
 *   <li>{@code "urn:ogc:def:crs:EPSG:8.2:4326"} (explicit version number, here 8.2)</li>
 *   <li>{@code "urn:x-ogc:def:crs:EPSG::4326"} (prior registration of {@code "ogc"} to IANA)</li>
 *   <li>{@code "http://www.opengis.net/def/crs/EPSG/0/4326"}</li>
 *   <li>{@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}</li>
 * </ul>
 *
 * This class does not attempt to decode URL characters. For example a URL for "m/s" may be encoded as below,
 * in which case the value in the {@code #code} field will be {@code "m%2Fs"} instead of {@code "m/s"}.
 * <ul>
 *   <li>{@code http://www.opengis.net/def/uom/SI/0/m%2Fs}</li>
 * </ul>
 *
 * <div class="section">Components or URN</div>
 * URN begins with {@code "urn:ogc:def:"} (formerly {@code "urn:x-ogc:def:"}) followed by:
 * <ul>
 *   <li>an object {@linkplain #type}</li>
 *   <li>an {@linkplain #authority}</li>
 *   <li>an optional {@linkplain #version} number (often omitted)</li>
 *   <li>the {@linkplain #code}</li>
 *   <li>an arbitrary amount of {@linkplain #parameters}</li>
 * </ul>
 *
 * The <cite>object type</cite> can be one of the following name.
 * Mapping between those names and GeoAPI interfaces is provided by the
 * {@link org.apache.sis.internal.metadata.NameMeaning} class.
 * <table class="sis">
 *   <caption>Recognized object types in URN</caption>
 *   <tr><th>Object type</th>         <th>Meaning</th></tr>
 *   <tr><td>axis</td>                <td>Coordinate system axe definition</td></tr>
 *   <tr><td>axisDirection</td>       <td>Axis direction code definition</td></tr>
 *   <tr><td>coordinateOperation</td> <td>Coordinate operation definition</td></tr>
 *   <tr><td>crs</td>                 <td>Coordinate reference system definition</td></tr>
 *   <tr><td>cs</td>                  <td>Coordinate system definition</td></tr>
 *   <tr><td>datum</td>               <td>Datum definition</td></tr>
 *   <tr><td>dataType</td>            <td>Data type definition</td></tr>
 *   <tr><td>derivedCRSType</td>      <td>Derived CRS type code definition</td></tr>
 *   <tr><td>documentType</td>        <td>Document type definition</td></tr>
 *   <tr><td>ellipsoid</td>           <td>Ellipsoid definition</td></tr>
 *   <tr><td>featureType</td>         <td>Feature type definition</td></tr>
 *   <tr><td>group</td>               <td>Operation parameter group definition</td></tr>
 *   <tr><td>meaning</td>             <td>Parameter meaning definition</td></tr>
 *   <tr><td>meridian</td>            <td>Prime meridian definition</td></tr>
 *   <tr><td>method</td>              <td>Operation method definition</td></tr>
 *   <tr><td>nil</td>                 <td>Explanations for missing information</td></tr>
 *   <tr><td>parameter</td>           <td>Operation parameter definition</td></tr>
 *   <tr><td>phenomenon</td>          <td>Observable property definition</td></tr>
 *   <tr><td>pixelInCell</td>         <td>Pixel in cell code definition</td></tr>
 *   <tr><td>rangeMeaning</td>        <td>Range meaning code definition</td></tr>
 *   <tr><td>referenceSystem</td>     <td>Value reference system definition</td></tr>
 *   <tr><td>uom</td>                 <td>Unit of measure definition</td></tr>
 *   <tr><td>verticalDatumType</td>   <td>Vertical datum type code definition</td></tr>
 * </table>
 *
 * Some example of <cite>authorities</cite> are:
 * <table class="sis">
 *   <caption>Authority examples</caption>
 *   <tr><th>Authority</th>      <th>Purpose</th></tr>
 *   <tr><td>{@code "OGC"}</td>  <td>Objects defined by the Open Geospatial Consortium.</td></tr>
 *   <tr><td>{@code "EPSG"}</td> <td>Referencing objects defined in the EPSG database.</td></tr>
 *   <tr><td>{@code "EDCS"}</td> <td>Environmental Data Coding Specification.</td></tr>
 *   <tr><td>{@code "SI"}</td>   <td>International System of Units.</td></tr>
 *   <tr><td>{@code "UCUM"}</td> <td>Unified Code for Units of Measure.</td></tr>
 * </table>
 *
 * <div class="section">Combined URNs</div>
 * This implementation does not handle combined URNs. An example of combined URN would be
 * {@code "urn:ogc:def:crs,crs:EPSG:6.3:27700,crs:EPSG:6.3:5701"}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.internal.metadata.NameMeaning
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=24045">Definition identifier URNs in OGC namespace</a>
 * @see <a href="http://www.opengeospatial.org/ogcna">OGC Naming Authority</a>
 */
public final class DefinitionURI {
    /**
     * The {@value} prefix used in all URN supported by this class.
     */
    public static final String PREFIX = "urn:ogc:def";

    /**
     * The URN separator.
     */
    public static final char SEPARATOR = ':';

    /**
     * The domain of URLs in the OGC namespace.
     */
    private static final String DOMAIN = "www.opengis.net";

    /**
     * Server and path portions of HTTP URL for various types (currently {@code "crs"}).
     * For each URL, value starts after the protocol part and finishes before the authority filename.
     *
     * <p>As of Apache SIS 0.4, this map has a single entry. However more entries may be added in future SIS versions.
     * If new entries are added, then see the TODO comment in the {@link #codeForGML(String, String, String, int,
     * DefinitionURI)} method.</p>
     */
    private static final Map<String,String> PATHS = Collections.singletonMap("crs", "//" + DOMAIN + "/gml/srs/");

    /**
     * A version number to be considered as if no version were provided.
     * This value is part of OGC specification (not a SIS-specific hack).
     */
    public static final String NO_VERSION = "0";

    /**
     * {@code true} if the URI is a {@code "http://www.opengis.net/…"} URL, or
     * {@code false} if the URI is a {@code "urn:ogc:def:…"} URN.
     */
    public boolean isHTTP;

    /**
     * {@code true} if the URI is a {@code "http://www.opengis.net/gml/…"} URL.
     * A value of {@code true} should imply that {@link #isHTTP} is also {@code true}.
     */
    public boolean isGML;

    /**
     * The type part of a URI, or {@code null} if none (empty).
     * Note that the set of valid types in OGC namespace is restricted.
     * See class javadoc for more information.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "crs"}.</div>
     *
     * @see org.apache.sis.internal.metadata.NameMeaning#toObjectType(Class)
     */
    public String type;

    /**
     * The authority part of a URI, or {@code null} if none (empty).
     * Note that the set of valid authorities in OGC namespace is restricted.
     * See class javadoc for more information.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "EPSG"}.</div>
     *
     * @see org.apache.sis.internal.metadata.NameMeaning#authority(String)
     */
    public String authority;

    /**
     * The version part of a URI, or {@code null} if none (empty).
     * This field is null if the version in the parsed string was {@value #NO_VERSION}.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "8.2"}.</div>
     */
    public String version;

    /**
     * The code part of a URI, or {@code null} if none (empty).
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "4326"}.</div>
     */
    public String code;

    /**
     * The parameters, or {@code null} if none.
     *
     * <div class="note"><b>Example:</b>
     * In the {@code "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"} URN, this is <code>{"1", "-100", "45"}</code>}.</div>
     */
    public String[] parameters;

    /**
     * For {@link #parse(String)} usage only.
     */
    private DefinitionURI() {
    }

    /**
     * Attempts to parse the given URI, which may either a URN or URL.
     * If this method does not recognize the given URI, then it returns {@code null}.
     * If the given URI is incomplete, then the {@link #code} value will be {@code null}.
     *
     * @param  uri The URI to parse.
     * @return The parse result, or {@code null} if the given URI is not recognized.
     */
    public static DefinitionURI parse(final String uri) {
        ensureNonNull("uri", uri);
        DefinitionURI result = null;
        char separator = SEPARATOR;
        int upper = -1;
        /*
         * Loop on all parts that we expect in the URI. Those parts are:
         *
         *   0:  "urn" or "http://www.opengis.net"
         *   1:  "ogc" or "x-ogc"
         *   2:  "def"
         *   3:  "crs", "datum" or other types. The value is not controlled by this method.
         *   4:  "ogc", "epsg", or other authorities. The value is not controlled by this method.
         *   5:  version, or null if none.
         *   6:  code
         *   7:  parameters, or null if none.
         */
        for (int p=0; p<=6; p++) {
            final int lower = upper + 1;
            upper = uri.indexOf(separator, lower);
            if (upper < 0) {
                upper = uri.length();
                if (lower > upper) {
                    return result;        // Happen if a component is missing.
                }
            }
            switch (p) {
                /*
                 * Verifies that the 3 first components are "urn:ogc:def:" or "http://www.opengis.net/def/"
                 * without storing them. In the particular case of second component, we also accept "x-ogc"
                 * in addition to "ogc" in URN.
                 */
                case 0: {
                    if (regionMatches("http", uri, lower, upper)) {
                        result = new DefinitionURI();
                        result.isHTTP = true;
                        if (codeForGML(null, null, uri, ++upper, result) != null) {
                            return result;
                        }
                        if (!uri.regionMatches(upper, "//", 0, 2)) {
                            return null;
                        }
                        upper++;
                        separator = '/';    // Separator for the HTTP namespace.
                    } else if (!regionMatches("urn", uri, lower, upper)) {
                        return null;
                    }
                    break;
                }
                case 1: {
                    final boolean isHTTP = (separator != SEPARATOR);
                    if (!regionMatches(isHTTP ? DOMAIN : "ogc", uri, lower, upper)) {
                        if (isHTTP  ||  !regionMatches("x-ogc", uri, lower, upper)) {
                            return null;
                        }
                    }
                    break;
                }
                case 2: {
                    if (!regionMatches("def", uri, lower, upper)) {
                        return null;
                    }
                    break;
                }
                /*
                 * For all components after the first 3 ones, trim whitespaces and store non-empty values.
                 */
                default: {
                    final String value = trimWhitespaces(uri, lower, upper).toString();
                    if (!value.isEmpty() && (p != 5 || !NO_VERSION.equals(value))) {
                        if (result == null) {
                            result = new DefinitionURI();
                        }
                        switch (p) {
                            case 3:  result.type      = value; break;
                            case 4:  result.authority = value; break;
                            case 5:  result.version   = value; break;
                            case 6:  result.code      = value; break;
                            default: throw new AssertionError(p);
                        }
                    }
                }
            }
        }
        /*
         * Take every remaining components as parameters.
         */
        if (result != null && ++upper < uri.length()) {
            result.parameters = (String[]) split(uri.substring(upper), separator);
        }
        return result;
    }

    /**
     * Returns {@code true} if a sub-region of {@code urn} matches the given {@code component},
     * ignoring case, leading and trailing whitespaces.
     *
     * @param  component The expected component ({@code "urn"}, {@code "ogc"}, {@code "def"}, <i>etc.</i>)
     * @param  urn       The URN for which to test a subregion.
     * @param  lower     Index of the first character in {@code urn} to compare, after skipping whitespaces.
     * @param  upper     Index after the last character in {@code urn} to compare, ignoring whitespaces.
     * @return {@code true} if the given sub-region of {@code urn} match the given component.
     */
    static boolean regionMatches(final String component, final String urn, int lower, int upper) {
        lower = skipLeadingWhitespaces (urn, lower, upper);
        upper = skipTrailingWhitespaces(urn, lower, upper);
        final int length = upper - lower;
        return (length == component.length()) && urn.regionMatches(true, lower, component, 0, length);
    }

    /**
     * Returns the substring of the given URN, ignoring whitespaces and version number if present.
     * The substring is expected to contains at most one {@code ':'} character. If such separator
     * character is present, then that character and everything before it are ignored. The ignored
     * part should be the version number, but this is not verified.
     *
     * <p>If the remaining substring is empty or contains more {@code ':'} characters, then this method
     * returns {@code null}. The presence of more {@code ':'} characters means that the code has parameters,
     * (e.g. {@code "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"}) which are not handled by this method.</p>
     *
     * @param  urn The URN from which to get the code.
     * @param  fromIndex Index of the first character in {@code urn} to check.
     * @return The code part of the URN, or {@code null} if empty or invalid.
     */
    private static String codeIgnoreVersion(final String urn, int fromIndex) {
        final int length = urn.length();
        fromIndex = skipLeadingWhitespaces(urn, fromIndex, length);
        if (fromIndex >= length) {
            return null; // Empty code.
        }
        final int s = urn.indexOf(SEPARATOR, fromIndex);
        if (s >= 0) {
            // Ignore the version number (actually everything up to the first ':').
            fromIndex = skipLeadingWhitespaces(urn, s+1, length);
            if (fromIndex >= length || urn.indexOf(SEPARATOR, fromIndex) >= 0) {
                return null;    // Empty code, or the code is followed by parameters.
            }
        }
        return urn.substring(fromIndex, skipTrailingWhitespaces(urn, fromIndex, length));
    }

    /**
     * Returns the code part of the given URI, provided that it matches the given object type and authority.
     * This lightweight method is useful when:
     *
     * <ul>
     *   <li>the URI is expected to have a specific <cite>object type</cite> and <cite>authority</cite>;</li>
     *   <li>the version number is considered irrelevant;</li>
     *   <li>the code is expected to have no parameters.</li>
     * </ul>
     *
     * This method accepts the following URI representations:
     *
     * <ul>
     *   <li>Code alone, without any {@code ':'} character (e.g. {@code "4326"}).</li>
     *   <li>The given authority followed by the code (e.g. {@code "EPSG:4326"}).</li>
     *   <li>The URN form (e.g. {@code "urn:ogc:def:crs:EPSG::4326"}), ignoring version number.
     *       This method accepts also the former {@code "x-ogc"} in place of {@code "ogc"}.</li>
     *   <li>The HTTP form (e.g. {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}).</li>
     * </ul>
     *
     * @param  type      The expected object type (e.g. {@code "crs"}) in lower cases. See class javadoc for a list of types.
     * @param  authority The expected authority, typically {@code "epsg"}. See class javadoc for a list of authorities.
     * @param  uri       The URI to parse.
     * @return The code part of the given URI, or {@code null} if the codespace does not match the given type
     *         and authority, the code is empty, or the code is followed by parameters.
     */
    public static String codeOf(final String type, final String authority, final String uri) {
        ensureNonNull("type",      type);
        ensureNonNull("authority", authority);
        ensureNonNull("uri",       uri);
        /*
         * Get the part before the first ':' character. If none, assume that the given URI is already the code.
         * Otherwise the part may be either "http" or "urn" protocol, or the given authority (typically "EPSG").
         * In the later case, we return immediately the code after the authority part.
         */
        int upper = uri.indexOf(SEPARATOR);
        if (upper < 0) {
            return trimWhitespaces(uri);
        }
        int lower  = skipLeadingWhitespaces(uri, 0, upper);
        int length = skipTrailingWhitespaces(uri, lower, upper) - lower;
        if (length == authority.length() && uri.regionMatches(true, lower, authority, 0, length)) {
            return codeIgnoreVersion(uri, upper+1);
        }
        /*
         * Check for supported protocols: only "urn" and "http" at this time.
         * All other protocols are rejected as unrecognized.
         */
        String component;
        switch (length) {
            case 3:  component = "urn";  break;
            case 4:  component = "http"; break;
            default: return null;
        }
        if (!uri.regionMatches(true, lower, component, 0, length)) {
            return null;
        }
        if (length == 4) {
            return codeForGML(type, authority, uri, upper+1, null);
        }
        /*
         * At this point we have determined that the protocol is URN. The next components after "urn"
         * shall be "ogc" or "x-ogc", then "def", then the type and authority given in arguments.
         */
        for (int p=0; p!=4; p++) {
            lower = upper + 1;
            upper = uri.indexOf(SEPARATOR, lower);
            if (upper < 0) {
                return null; // No more components.
            }
            switch (p) {
                case 0: if (regionMatches("ogc", uri, lower, upper)) {
                            continue; // "ogc" is tested before "x-ogc" because more common.
                        }
                        component = "x-ogc";   break; // Fallback if the component is not "ogc".
                case 1: component = "def";     break;
                case 2: component = type;      break;
                case 3: component = authority; break;
                default: throw new AssertionError(p);
            }
            if (!regionMatches(component, uri, lower, upper)) {
                return null;
            }
        }
        return codeIgnoreVersion(uri, upper+1);
    }

    /**
     * Implementation of URI parser for the HTTP forms in GML namespace.
     * The current implementation recognizes the following types:
     *
     * <ul>
     *   <li>{@code crs} for Coordinate Reference System objects
     *       (example: {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"})</li>
     * </ul>
     *
     * @param type      The expected type in lower cases, or {@code null} for any.
     * @param authority The expected authority, or {@code null} for any.
     * @param url       The URL to parse.
     * @param result    If non-null, store the type, authority and code in that object.
     */
    @SuppressWarnings("fallthrough")
    private static String codeForGML(final String type, String authority, final String url, int lower,
            final DefinitionURI result)
    {
        Map<String, String> paths = PATHS;
        if (type != null) {
            final String path = paths.get(type);
            if (path == null) {
                return null;
            }
            // TODO: For now do nothing since PATHS is a singleton. However if a future SIS version
            //       defines more PATHS entries, then we should replace here the 'paths' reference by
            //       a new Collections.singletonMap containing only the entry of interest.
        }
        for (final Map.Entry<String,String> entry : paths.entrySet()) {
            final String path = entry.getValue();
            if (url.regionMatches(true, lower, path, 0, path.length())) {
                lower = CharSequences.skipLeadingWhitespaces(url, lower + path.length(), url.length());
                if (authority == null) {
                    authority = url.substring(lower, skipIdentifierPart(url, lower));
                } else if (!url.regionMatches(true, lower, authority, 0, authority.length())) {
                    continue;
                }
                lower += authority.length();
                int upper = url.length();
                if (lower < upper) {
                    switch (url.charAt(lower)) {
                        case '.': {
                            // Ignore the extension (typically ".xml", but we accept anything).
                            lower = url.indexOf('#', lower + 1);
                            if (lower < 0) continue;
                            // Fall through
                        }
                        case '#': {
                            final String code = trimWhitespaces(url, lower+1, upper).toString();
                            if (result != null) {
                                result.isGML     = true;
                                result.type      = entry.getKey();
                                result.authority = authority;
                                result.code      = code;
                            }
                            return code;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the index after the last identifier character.
     */
    private static int skipIdentifierPart(final String text, int i) {
        while (i < text.length()) {
            final int c = text.codePointAt(i);
            if (!Character.isUnicodeIdentifierPart(c)) break;
            i += Character.charCount(c);
        }
        return i;
    }

    /**
     * Formats the given identifier using the {@code "ogc:urn:def:"} syntax. The identifier code space,
     * version and code are appended omitting any characters that are not valid for a Unicode identifier.
     * If some information are missing in the given identifier, then this method returns {@code null}.
     *
     * @param  type      The object type as one of the types documented in class javadoc, or {@code null}.
     * @param  authority The authority as one of the values documented in class javadoc, or {@code null}.
     * @param  version   The code version, or {@code null}. This is the only optional information.
     * @param  code      The code, or {@code null}.
     * @return An identifier using the URN syntax, or {@code null} if a mandatory information is missing.
     *
     * @see org.apache.sis.internal.metadata.NameMeaning#toURN(Class, String, String, String)
     */
    public static String format(final String type, final String authority, final String version, final String code) {
        final StringBuilder buffer = new StringBuilder(PREFIX);
        for (int p=0; p<4; p++) {
            final String component;
            switch (p) {
                case 0:  component = type;      break;
                case 1:  component = authority; break;
                case 2:  component = version;   break;
                case 3:  component = code;      break;
                default: throw new AssertionError(p);
            }
            if (!appendUnicodeIdentifier(buffer.append(SEPARATOR), '\u0000', component, ".-", false)) {
                /*
                 * Only the version (p = 2) is optional. All other fields are mandatory.
                 * If no character has been added for a mandatory field, we can not build a URN.
                 */
                if (p != 2) {
                    return null;
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Returns a string representation of this URI. If the URI were originally a GML's URL, then this method formats
     * the URI in the {@code "http://www.opengis.net/gml/srs/"} namespace. Otherwise the URI were originally an URL,
     * then this method formats the URI in the {@code "http://www.opengis.net/"} namespace.
     * Otherwise this method formats the URI as a URN.
     *
     * @return The string representation of this URI.
     */
    @Override
    public String toString() {
        if (isGML) {
            final String path = PATHS.get(type);
            if (path != null) {
                return "http:" + path + authority + ".xml#" + code;
            }
        }
        final StringBuilder buffer = new StringBuilder(PREFIX);
        char separator = SEPARATOR;
        if (isHTTP) {
            buffer.setLength(0);
            buffer.append("http://").append(DOMAIN).append("/def");
            separator = '/';
        }
        int n = 4;
        if (parameters != null) {
            n += parameters.length;
        }
        for (int p=0; p<n; p++) {
            String component;
            switch (p) {
                case 0:  component = type;            break;
                case 1:  component = authority;       break;
                case 2:  component = version;         break;
                case 3:  component = code;            break;
                default: component = parameters[p-4]; break;
            }
            buffer.append(separator);
            if (component == null) {
                if (!isHTTP) continue;
                component = NO_VERSION;
            }
            buffer.append(component);
        }
        return buffer.toString();
    }
}
