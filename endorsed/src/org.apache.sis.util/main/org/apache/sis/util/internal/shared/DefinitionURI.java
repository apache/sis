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
package org.apache.sis.util.internal.shared;

import java.util.Map;
import java.util.TreeMap;
import static java.util.AbstractMap.SimpleEntry;
import static java.util.logging.Logger.getLogger;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;
import static org.apache.sis.util.CharSequences.*;


/**
 * Utility methods for parsing OGC's URI (URN or URL) in the {@code "urn:ogc:def"} namespace.
 *
 * <p>For example, all the following URIs are for the same object:</p>
 * <ul>
 *   <li>{@code "EPSG:4326"} (older format)</li>
 *   <li>{@code "EPSG::4326"} (often seen for similarity with URN below)</li>
 *   <li>{@code "urn:ogc:def:crs:EPSG::4326"} (version number is omitted)</li>
 *   <li>{@code "urn:ogc:def:crs:EPSG:8.2:4326"} (explicit version number, here 8.2)</li>
 *   <li>{@code "urn:x-ogc:def:crs:EPSG::4326"} (prior registration of {@code "ogc"} to IANA)</li>
 *   <li>{@code "http://www.opengis.net/def/crs/EPSG/0/4326"}</li>
 *   <li>{@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}</li>
 * </ul>
 *
 * This class does not attempt to decode URL characters. For example, a URL for "m/s" may be encoded as below,
 * in which case the value in the {@link #code} field will be {@code "m%2Fs"} instead of {@code "m/s"}.
 * <ul>
 *   <li>{@code http://www.opengis.net/def/uom/SI/0/m%2Fs}</li>
 * </ul>
 *
 * <h2>Parts of URN</h2>
 * URN begins with {@code "urn:ogc:def:"} (formerly {@code "urn:x-ogc:def:"}) followed by:
 * <ul>
 *   <li>an object {@linkplain #type}</li>
 *   <li>an {@linkplain #authority}</li>
 *   <li>an optional {@linkplain #version} number (often omitted)</li>
 *   <li>the {@linkplain #code}</li>
 *   <li>an arbitrary number of {@linkplain #parameters}</li>
 * </ul>
 *
 * The <i>object type</i> can be one of the following name.
 * Mapping between those names and GeoAPI interfaces is provided by the
 * {@link org.apache.sis.metadata.internal.shared.NameMeaning} class.
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
 * Some examples of <i>authorities</i> are:
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
 * <h2>Combined URNs</h2>
 * This implementation does not handle combined URNs. An example of combined URN would be
 * {@code "urn:ogc:def:crs,crs:EPSG:6.3:27700,crs:EPSG:6.3:5701"}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.metadata.internal.shared.NameMeaning
 * @see <a href="https://portal.ogc.org/files/?artifact_id=24045">Definition identifier URNs in OGC namespace</a>
 * @see <a href="https://www.ogc.org/ogcna">OGC Naming Authority</a>
 */
public final class DefinitionURI {
    /**
     * The {@value} prefix used in all URN supported by this class.
     */
    public static final String PREFIX = "urn:ogc:def";

    /**
     * The path separator in URN.
     */
    public static final char SEPARATOR = ':';

    /**
     * The separator between {@linkplain #components} in a URN.
     *
     * <h4>Example</h4>
     * In {@code "urn:ogc:def:crs,crs:EPSG:9.1:27700,crs:EPSG:9.1:5701"}, the components are
     * {@code "crs:EPSG:9.1:27700"} and {@code "crs:EPSG:9.1:5701"}.
     */
    public static final char COMPONENT_SEPARATOR = ',';

    /**
     * The separator between a URL and its first {@linkplain #components}.
     * In URL syntax, this is the separator between URL path and the query.
     *
     * <h4>Example</h4>
     * <code>http://www.opengis.net/def/crs-compound<u>?</u>1=…&amp;2=…</code>
     */
    private static final char COMPONENT_SEPARATOR_1 = '?';

    /**
     * The separator between {@linkplain #components} in a URL after the first component.
     *
     * <h4>Example</h4>
     * <code>http://www.opengis.net/def/crs-compound?1=…<u>&amp;</u>2=…</code>
     */
    private static final char COMPONENT_SEPARATOR_2 = '&';

    /**
     * Separator between keys and values in the query part of a URL.
     *
     * <h4>Example</h4>
     * <code>http://www.opengis.net/def/crs-compound?1<u>=</u>…&amp;2<u>=</u>…</code>
     */
    private static final char KEY_VALUE_SEPARATOR = '=';

    /**
     * The domain of URLs in the OGC namespace.
     */
    public static final String DOMAIN = "www.opengis.net";

    /**
     * Server and path portions of HTTP URL for various types (currently {@code "crs"}).
     * For each URL, value starts after the protocol part and finishes before the authority filename.
     *
     * <p>As of Apache SIS 0.4, this map has a single entry. However, more entries may be added in future SIS versions.
     * If new entries are added, then see the TODO comment in the {@link #codeForGML(String, String, String, int,
     * DefinitionURI)} method.</p>
     */
    private static final Map<String,String> PATHS = Map.of("crs", "//" + DOMAIN + "/gml/srs/");

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
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "crs"}.
     *
     * @see org.apache.sis.metadata.internal.shared.NameMeaning#toObjectType(Class)
     */
    public String type;

    /**
     * The authority part of a URI, or {@code null} if none (empty).
     * Note that the set of valid authorities in OGC namespace is restricted.
     * See class javadoc for more information.
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "EPSG"}.
     */
    public String authority;

    /**
     * The version part of a URI, or {@code null} if none (empty).
     * This field is null if the version in the parsed string was {@value #NO_VERSION}.
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "8.2"}.
     */
    public String version;

    /**
     * The code part of a URI, or {@code null} if none (empty).
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:EPSG:8.2:4326"} URN, this is {@code "4326"}.
     */
    public String code;

    /**
     * The parameters, or {@code null} if none.
     *
     * <h4>Example</h4>
     * In the {@code "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"} URN, this is <code>{"1", "-100", "45"}</code>}.
     */
    public String[] parameters;

    /**
     * If the URI contains sub-components, those sub-components. Otherwise {@code null}.
     * Note that this array may contain {@code null} elements if we failed to parse the corresponding component.
     *
     * <h4>URN example</h4>
     * If the URI is {@code "urn:ogc:def:crs,crs:EPSG:9.1:27700,crs:EPSG:9.1:5701"}, then this
     * {@code DefinitionURI} will contain the {@code "urn:ogc:def:crs"} header with two components:
     * <ol>
     *   <li>{@code "urn:ogc:def:crs:EPSG:9.1:27700"}</li>
     *   <li>{@code "urn:ogc:def:crs:EPSG:9.1:5701"}</li>
     * </ol>
     *
     * <h4>HTTP example</h4>
     * If the URI is
     * {@code "http://www.opengis.net/def/crs-compound?1=(…)/crs/EPSG/9.1/27700&2=(…)/crs/EPSG/9.1/5701"},
     * then this {@code DefinitionURI} will contain the {@code "http://www.opengis.net/def/crs-compound"}
     * header with two components:
     * <ol>
     *   <li>{@code "http://http://www.opengis.net/def/crs/EPSG/9.1/27700"}</li>
     *   <li>{@code "http://http://www.opengis.net/def/crs/EPSG/9.1/5701"}</li>
     * </ol>
     */
    public DefinitionURI[] components;

    /**
     * For {@link #parse(String)} usage only.
     */
    private DefinitionURI() {
    }

    /**
     * Attempts to parse the given URI, which may be either a URN or URL.
     * If this method does not recognize the given URI, then it returns {@code null}.
     * If the given URI is incomplete, then the {@link #code} value will be {@code null}.
     *
     * @param  uri  the URI to parse.
     * @return the parse result, or {@code null} if the given URI is not recognized.
     */
    public static DefinitionURI parse(String uri) {
        uri = CharSequences.trimIgnorables(uri).toString();
        return parse(uri, false, -1, uri.length());
    }

    /**
     * Parses a sub-region of the given URI. This method can start parsing for an arbitrary URI part,
     * no necessarily the root. The first URI part is identified by an ordinal number:
     *
     * This method may invoke itself recursively if the URI contains sub-components.
     *
     * @param  uri               the URI to parse.
     * @param  prefixIsOptional  {@code true} if {@value #PREFIX} may not be present.
     * @param  upper             upper index of the previous URI part, or -1 if none.
     * @param  stopAt            index (exclusive) where to stop parsing.
     * @return the parse result, or {@code null} if the URI is not recognized.
     */
    @SuppressWarnings("fallthrough")
    private static DefinitionURI parse(final String uri, boolean prefixIsOptional, int upper, int stopAt) {
        DefinitionURI result    = null;
        char separator          = SEPARATOR;                    // Separator character of URI parts.
        char componentSeparator = COMPONENT_SEPARATOR;          // Separator character of components.
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
        for (int part = 0; part <= 6; part++) {
            final int lower = upper + 1;
            upper = uri.indexOf(separator, lower);
            if (upper < 0 || upper >= stopAt) {
                upper = stopAt;
                if (lower > upper) {
                    return result;                      // Happen if a part is missing.
                }
            }
            switch (part) {
                /*
                 * Verifies that the 3 first parts are "urn:ogc:def:" or "http://www.opengis.net/def/"
                 * without storing them. In the particular case of second part, we also accept "x-ogc"
                 * in addition to "ogc" in URN.
                 */
                case 0: {
                    if (regionMatches(Constants.HTTP, uri, lower, upper)) {
                        result = new DefinitionURI();
                        result.isHTTP = true;
                        if (codeForGML(null, null, uri, ++upper, result) != null) {
                            return result;
                        }
                        if (!uri.startsWith("//", upper)) {
                            return null;                                // Prefix is never optional for HTTP.
                        }
                        upper++;
                        separator = '/';                                // Separator for the HTTP namespace.
                        componentSeparator = COMPONENT_SEPARATOR_1;     // Separator for the query part in URL.
                        prefixIsOptional = false;
                        break;
                    } else if (regionMatches("urn", uri, lower, upper)) {
                        prefixIsOptional = false;
                        break;
                    } else if (!prefixIsOptional) {
                        return null;
                    }
                    part++;
                    // Part is not "urn" but its presence was optional. Maybe it is "ogc". Fall through for checking.
                }
                case 1: {
                    final boolean isHTTP = (separator != SEPARATOR);
                    if (regionMatches(isHTTP ? DOMAIN : "ogc", uri, lower, upper) ||
                            (!isHTTP && regionMatches("x-ogc", uri, lower, upper)))
                    {
                        prefixIsOptional = false;
                        break;
                    } else if (!prefixIsOptional) {
                        return null;
                    }
                    part++;
                    // Part is not "ogc" but its presence was optional. Maybe it is "def". Fall through for checking.
                }
                case 2: {
                    if (regionMatches("def", uri, lower, upper)) {
                        prefixIsOptional = false;
                        break;
                    } else if (!prefixIsOptional) {
                        return null;
                    }
                    part++;
                    // Part is not "def" but its presence was optional. Maybe it is "crs". Fall through for checking.
                }
                /*
                 * The forth part is the first one that we want to remember; all cases before this one were
                 * only verification. This case is also the first part where component separator may appear,
                 * for example as in "urn:ogc:def:crs,crs:EPSG:9.1:27700,crs:EPSG:9.1:5701". We verify here
                 * if such components exist, and if so we parse them recursively.
                 */
                case 3: {
                    int splitAt = uri.indexOf(componentSeparator, lower);
                    if (splitAt >= 0 && splitAt < stopAt) {
                        final int componentsEnd = stopAt;
                        stopAt = splitAt;                   // Upper limit of the DefinitionURI created in this method call.
                        if (stopAt < upper) {
                            upper = stopAt;
                        }
                        if (componentSeparator == COMPONENT_SEPARATOR_1) {
                            componentSeparator =  COMPONENT_SEPARATOR_2;    // E.g. http://(…)/crs-compound?1=(…)&2=(…)
                        }
                        if (result == null) {
                            result = new DefinitionURI();
                        }
                        final boolean isURN = !result.isHTTP;
                        final Map<Integer,DefinitionURI> orderedComponents = new TreeMap<>();
                        boolean hasMore;
                        do {
                            /*
                             * Find indices of URI sub-component to parse. The sub-component will
                             * go from `splitAt` to `next` exclusive (`splitAt` is exclusive too).
                             */
                            int next = uri.indexOf(componentSeparator, splitAt+1);
                            hasMore = next >= 0 && next < componentsEnd;
                            if (!hasMore) next = componentsEnd;
                            /*
                             * HTTP uses key-value pairs as in "http://something?1=...&2=...
                             * URN uses a comma-separated value list without number.
                             * We support both forms, regardless if HTTP or URN.
                             */
                            int sequenceNumber = orderedComponents.size() + 1;      // Default value if no explicit key.
                            final int s = splitKeyValue(uri, splitAt+1, next);
                            if (s >= 0) try {
                                sequenceNumber = Integer.parseInt(trimWhitespaces(uri, splitAt+1, s).toString());
                                splitAt = s;                      // Set only on success.
                            } catch (NumberFormatException e) {
                                /*
                                 * Ignore. The URN is likely to be invalid, but we let parse(…) determines that.
                                 * Current version assumes that the URN was identifying a CRS, but future version
                                 * could choose the logger in a more dynamic way.
                                 */
                                Logging.recoverableException(getLogger(Loggers.CRS_FACTORY), DefinitionURI.class, "parse", e);
                            }
                            orderedComponents.put(sequenceNumber, parse(uri, isURN, splitAt, next));
                            splitAt = next;
                        } while (hasMore);
                        result.components = orderedComponents.values().toArray(DefinitionURI[]::new);
                    }
                    // Fall through
                }
                /*
                 * For all parts after the first 3 ones, trim whitespaces and store non-empty values.
                 */
                default: {
                    final String value = trimWhitespaces(uri, lower, upper).toString();
                    if (!value.isEmpty() && (part != 5 || !NO_VERSION.equals(value))) {
                        if (result == null) {
                            result = new DefinitionURI();
                        }
                        switch (part) {
                            case 3:  result.type      = value; break;
                            case 4:  result.authority = value; break;
                            case 5:  result.version   = value; break;
                            case 6:  result.code      = value; break;
                            default: throw new AssertionError(part);
                        }
                    }
                }
            }
        }
        /*
         * Take every remaining parts as parameters.
         */
        if (result != null && ++upper < stopAt) {
            result.parameters = (String[]) split(uri.substring(upper), separator);
        }
        return result;
    }

    /**
     * Returns {@code true} if a sub-region of {@code urn} matches the given {@code part},
     * ignoring case, leading and trailing whitespaces.
     *
     * @param  part   the expected part ({@code "urn"}, {@code "ogc"}, {@code "def"}, <i>etc.</i>)
     * @param  urn    the URN for which to test a subregion.
     * @param  lower  index of the first character in {@code urn} to compare, after skipping whitespaces.
     * @param  upper  index after the last character in {@code urn} to compare, ignoring whitespaces.
     * @return {@code true} if the given sub-region of {@code urn} match the given part.
     */
    public static boolean regionMatches(final String part, final String urn, int lower, int upper) {
        lower = skipLeadingWhitespaces (urn, lower, upper);
        upper = skipTrailingWhitespaces(urn, lower, upper);
        final int length = upper - lower;
        return (length == part.length()) && urn.regionMatches(true, lower, part, 0, length);
    }

    /**
     * Returns the index of the {@code '='} character in the given sub-string, provided that all characters
     * before it are spaces or decimal digits. Returns -1 if the key-value separator character is not found.
     * Note that a positive return value does not guarantee that the number is parsable.
     */
    private static int splitKeyValue(final String uri, int lower, final int upper) {
        while (lower < upper) {
            int c = uri.codePointAt(lower);
            if ((c < '0' || c > '9') && !Character.isWhitespace(c)) {
                if (c == KEY_VALUE_SEPARATOR) return lower;
                break;
            }
            lower += Character.charCount(c);
        }
        return -1;
    }

    /**
     * Returns {@code true} if the given URI is recognized as an URN or URL.
     * The details of this check may change in any future Apache SIS version.
     *
     * @param  uri  the URI to check.
     * @return whether the given URI seems to be an URN or URL.
     */
    public static boolean isAbsolute(final String uri) {
        final int s = uri.indexOf(SEPARATOR);
        if (s <= 0) return false;
        final String c = CharSequences.trimWhitespaces(uri, 0, s).toString();
        return c.equalsIgnoreCase("urn") || c.equalsIgnoreCase(Constants.HTTP) || c.equalsIgnoreCase(Constants.HTTPS);
    }

    /**
     * Returns the code part of the given URI, provided that it matches the given object type and authority.
     * This method is useful when:
     *
     * <ul>
     *   <li>the URI is expected to have a specific <i>object type</i> and <i>authority</i>;</li>
     *   <li>the version number is considered irrelevant;</li>
     *   <li>the code is expected to have no parameters.</li>
     * </ul>
     *
     * This method accepts the following URI representations:
     *
     * <ul>
     *   <li>The given authority followed by the code (e.g. {@code "EPSG:4326"}).</li>
     *   <li>The URN form (e.g. {@code "urn:ogc:def:crs:EPSG::4326"}), ignoring version number.
     *       This method accepts also the former {@code "x-ogc"} in place of {@code "ogc"}.</li>
     *   <li>The HTTP form (e.g. {@code "http://www.opengis.net/def/crs/EPSG/0/4326"}).</li>
     *   <li>The GML form (e.g. {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}).</li>
     * </ul>
     *
     * @param  type         the expected object type (e.g. {@code "crs"}) in lower cases. See class javadoc for a list of types.
     * @param  authorities  the expected authorities, typically {@code "EPSG"}. See class javadoc for a list of authorities.
     * @param  uri          the URI to parse.
     * @return the code part of the given URI together with the authority index, or {@code null} if the codespace
     *         does not match any given type and authority, the code is empty, or the code is followed by parameters.
     */
    public static Map.Entry<Integer,String> codeOf(final String type, final String[] authorities, final CharSequence uri) {
        final int length = uri.length();
        int s = indexOf(uri, SEPARATOR, 0, length);
        if (s >= 0) {
            int from = skipLeadingWhitespaces(uri, 0, s);                   // Start of authority part.
            final int span = skipTrailingWhitespaces(uri, from, s) - from;
            for (int i=0; i < authorities.length; i++) {
                final String authority = authorities[i];
                if (span == authority.length() && CharSequences.regionMatches(uri, from, authority, true)) {
                    from = skipLeadingWhitespaces(uri, s+1, length);        // Start of code part.
                    if (from >= length) {
                        return null;
                    }
                    /*
                     * The substring is expected to contains zero or one more separator character.
                     * If present, then the separator character and everything before it are ignored.
                     * The ignored part should be the version number, but this is not verified.
                     */
                    s = indexOf(uri, SEPARATOR, from, length);
                    if (s >= 0) {
                        from = skipLeadingWhitespaces(uri, s+1, length);
                        if (from >= length || indexOf(uri, SEPARATOR, from, length) >= 0) {
                            /*
                             * If the remaining substring contains more ':' characters, then it means that
                             * the code has parameters, e.g. "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45".
                             */
                            return null;
                        }
                    }
                    final String code = uri.subSequence(from, skipTrailingWhitespaces(uri, from, length)).toString();
                    return new SimpleEntry<>(i, code);
                }
            }
            final DefinitionURI def = parse(uri.toString());
            if (def != null && def.parameters == null && type.equalsIgnoreCase(def.type)) {
                for (int i=0; i < authorities.length; i++) {
                    final String authority = authorities[i];
                    if (authority.equalsIgnoreCase(def.authority)) {
                        String code = def.code;
                        if (code == null) {
                            code = def.version;     // May happen with for example "EPSG:4326" instead of "EPSG::4326".
                        }
                        return new SimpleEntry<>(i, code);
                    }
                }
            }
        }
        return null;
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
     * @param  type       the expected type in lower cases, or {@code null} for any.
     * @param  authority  the expected authority, or {@code null} for any.
     * @param  url        the URL to parse.
     * @param  result     if non-null, store the type, authority and code in that object.
     */
    @SuppressWarnings("fallthrough")
    private static String codeForGML(final String type, String authority, final String url, int lower, final DefinitionURI result) {
        Map<String, String> paths = PATHS;
        if (type != null) {
            final String path = paths.get(type);
            if (path == null) {
                return null;
            }
            /*
             * TODO: For now do nothing because PATHS is a singleton. However if a future SIS version
             *       defines more PATHS entries, then we should replace here the `paths` reference by
             *       a new `Map.of(…)` containing only the entry of interest.
             */
        }
        for (final Map.Entry<String,String> entry : paths.entrySet()) {
            final String path = entry.getValue();
            if (url.regionMatches(true, lower, path, 0, path.length())) {
                lower = skipLeadingWhitespaces(url, lower + path.length(), url.length());
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
     * Returns a string representation of this URI. If the URI were originally a GML's URL, then this method formats
     * the URI in the {@code "http://www.opengis.net/gml/srs/"} namespace. Otherwise the URI were originally a URL,
     * then this method formats the URI in the {@code "http://www.opengis.net/"} namespace.
     * Otherwise this method formats the URI as a URN.
     *
     * @return the string representation of this URI.
     */
    @Override
    public String toString() {
        if (isGML) {
            final String path = PATHS.get(type);
            if (path != null) {
                return Constants.HTTP + ':' + path + authority + ".xml#" + code;
            }
        }
        final StringBuilder buffer = new StringBuilder(40);
        if (!isHTTP) {
            buffer.append(PREFIX);
        }
        appendStringTo(buffer, SEPARATOR);
        return buffer.toString();
    }

    /**
     * Formats the string representation of this URI into the given buffer. This method invoke itself recursively
     * if this URI has {@linkplain #components}. The {@value #PREFIX} must be appended by the caller, if applicable.
     *
     * @param  buffer     where to format the string representation.
     * @param  separator  first separator to append. Ignored if the URI is actually a URL.
     */
    private void appendStringTo(final StringBuilder buffer, char separator) {
        if (isHTTP) {
            buffer.append(Constants.HTTP + "://").append(DOMAIN).append("/def");
            separator = '/';
        }
        int n = 4;
        if (parameters != null) {
            n += parameters.length;
        }
        for (int p=0; p<n; p++) {
            String part;
            switch (p) {
                case 0:  part = type;            break;
                case 1:  part = authority;       break;
                case 2:  part = version;         break;
                case 3:  part = code;            break;
                default: part = parameters[p-4]; break;
            }
            buffer.append(separator);
            if (isHTTP) {
                if (part == null) {
                    part = NO_VERSION;
                }
            } else {
                separator = SEPARATOR;
                if (part == null) {
                    continue;
                }
            }
            buffer.append(part);
        }
        /*
         * Before to return the URI, trim trailing separators. For example if the URN has only a type
         * (no authority, version, code, etc.), then we want to return only "urn:ogc:def:crs" instead
         * of "urn:ogc:def:crs:::"). This happen with URN defining compound CRS for instance.
         */
        int length = buffer.length();
        n = isHTTP ? 2 : 1;
        while ((length -= n) >= 0 && buffer.charAt(length) == separator) {
            if (isHTTP && buffer.charAt(length + 1) != NO_VERSION.charAt(0)) break;
            buffer.setLength(length);
        }
        /*
         * If there is components, format them recursively. Note that the format is different depending if
         * we are formatting URN or HTTP.  Example: "urn:ogc:def:crs,crs:EPSG:9.1:27700,crs:EPSG:9.1:5701"
         * and "http://www.opengis.net/def/crs-compound?1=(…)/crs/EPSG/9.1/27700&2=(…)/crs/EPSG/9.1/5701".
         */
        if (components != null) {
            boolean first = true;
            for (int i=0; i<components.length;) {
                final DefinitionURI c = components[i++];
                if (c != null) {
                    if (isHTTP) {
                        buffer.append(first ? COMPONENT_SEPARATOR_1
                                            : COMPONENT_SEPARATOR_2)
                              .append(i).append(KEY_VALUE_SEPARATOR);
                    }
                    c.appendStringTo(buffer, COMPONENT_SEPARATOR);
                    first = false;
                }
            }
        }
    }
}
