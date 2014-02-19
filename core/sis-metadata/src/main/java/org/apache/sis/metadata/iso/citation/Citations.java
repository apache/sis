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
package org.apache.sis.metadata.iso.citation;

import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Static;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.util.CharSequences;


/**
 * A set of pre-defined constants and static methods working on {@linkplain Citation citations}.
 * The citation constants declared in this class are for:
 *
 * <ul>
 *   <li><cite>Organizations</cite> (e.g. {@linkplain #OGC})</li>
 *   <li><cite>Specifications</cite> (e.g. {@linkplain #WMS})</li>
 *   <li><cite>Authorities</cite> that maintain definitions of codes (e.g. {@linkplain #EPSG})</li>
 * </ul>
 *
 * In the later case, the citations are actually of kind {@link IdentifierSpace}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
public final class Citations extends Static {
    /**
     * The <a href="http://www.iso.org/">International Organization for Standardization</a>.
     *
     * @category Organization
     */
    public static final Citation ISO = new SimpleCitation("ISO");

    /**
     * The <a href="http://www.opengeospatial.org">Open Geospatial Consortium</a> organization.
     * "Open Geospatial Consortium" is the new name for "OpenGIS consortium".
     *
     * @category Organization
     */
    public static final Citation OGC = new SimpleCitation("OGC");

    /**
     * The <a href="http://www.ogp.org.uk">International Association of Oil & Gas Producers</a> organization.
     * This organization is responsible for maintainance of {@link #EPSG} database.
     *
     * @see #EPSG
     * @category Organization
     *
     * @since 0.4
     */
    public static final Citation OGP = new SimpleCitation("OGP");

    /**
     * The <a href="http://sis.apache.org">Apache SIS</a> project.
     *
     * @since 0.4
     */
    public static final Citation SIS = new SimpleCitation("SIS");

    /**
     * The <a href="http://www.esri.com">ESRI</a> organization.
     * This company defines many Coordinate Reference Systems in addition to the {@linkplain #EPSG} ones.
     *
     * @category Organization
     *
     * @since 0.4
     */
    public static final Citation ESRI = new SimpleCitation("ESRI");

    /**
     * The <a href="http://www.oracle.com">Oracle</a> organization.
     *
     * @category Organization
     *
     * @since 0.4
     */
    public static final Citation ORACLE = new SimpleCitation("Oracle");

    /**
     * The <a href="http://www.unidata.ucar.edu/software/netcdf-java">NetCDF</a> specification.
     *
     * @category Specification
     *
     * @since 0.4
     */
    public static final Citation NETCDF = new SimpleCitation("NetCDF");

    /**
     * The <a href="http://www.remotesensing.org/geotiff/geotiff.html">GeoTIFF</a> specification.
     *
     * @category Specification
     *
     * @since 0.4
     */
    public static final Citation GEOTIFF = new SimpleCitation("GeoTIFF");

    /**
     * The <a href="http://trac.osgeo.org/proj/">Proj.4</a> project.
     *
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> PROJ4 = new Authority<String>("Proj.4", "PROJ4");

    /**
     * The <a href="http://www.epsg.org">European Petroleum Survey Group</a> authority.
     * This citation is used as an authority for
     * {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system}
     * identifiers.
     *
     * @see #OGP
     * @see #AUTO
     * @see #AUTO2
     * @see #CRS
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> EPSG = new Authority<Integer>("EPSG", "EPSG");

    /**
     * <cite>International Standard Book Number</cite> (ISBN) defined by ISO-2108.
     * The ISO-19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * @see DefaultCitation#getISBN()
     *
     * @category Code space
     */
    public static final IdentifierSpace<String> ISBN = DefaultCitation.ISBN;

    /**
     * <cite>International Standard Serial Number</cite> (ISSN) defined by ISO-3297.
     * The ISO-19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * @see DefaultCitation#getISSN()
     *
     * @category Code space
     */
    public static final IdentifierSpace<String> ISSN = DefaultCitation.ISSN;

    /**
     * List of citations declared in this class.
     */
    private static final Citation[] AUTHORITIES = {
        ISO, OGC, OGP, SIS, ESRI, ORACLE, NETCDF, GEOTIFF, PROJ4, EPSG, ISBN, ISSN
    };

    /**
     * Do not allows instantiation of this class.
     */
    private Citations() {
    }

    /**
     * Returns a citation of the given name. The method makes the following choice:
     *
     * <ul>
     *   <li>If the given title is {@code null} or empty (ignoring spaces), then this method returns {@code null}.</li>
     *   <li>Otherwise if the given name matches a {@linkplain Citation#getTitle() title} or an
     *       {@linkplain Citation#getAlternateTitles() alternate titles} of one of the pre-defined
     *       constants ({@link #EPSG}, {@link #GEOTIFF}, <i>etc.</i>), then that constant is returned.</li>
     *   <li>Otherwise, a new citation is created with the specified name as the title.</li>
     * </ul>
     *
     * @param  title The citation title (or alternate title), or {@code null}.
     * @return A citation using the specified name, or {@code null} if the given title is null or empty.
     */
    public static Citation fromName(String title) {
        if (title == null || ((title = CharSequences.trimWhitespaces(title)).isEmpty())) {
            return null;
        }
        for (final Citation citation : AUTHORITIES) {
            if (titleMatches(citation, title)) {
                return citation;
            }
        }
        return new SimpleCitation(title);
    }

    /**
     * Returns {@code true} if at least one {@linkplain Citation#getTitle() title} or
     * {@linkplain Citation#getAlternateTitles() alternate title} in {@code c1} is leniently
     * equal to a title or alternate title in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The titles ordering is not significant.
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one title or
     *         alternate title matches.
     */
    public static boolean titleMatches(final Citation c1, final Citation c2) {
        return org.apache.sis.internal.util.Citations.titleMatches(c1, c2);
    }

    /**
     * Returns {@code true} if the {@linkplain Citation#getTitle() title} or any
     * {@linkplain Citation#getAlternateTitles() alternate title} in the given citation
     * matches the given string. The comparison is case-insensitive and ignores every character
     * which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  title The title or alternate title to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or alternate
     *         title matches the given string.
     */
    public static boolean titleMatches(final Citation citation, String title) {
        return org.apache.sis.internal.util.Citations.titleMatches(citation, title);
    }

    /**
     * Returns {@code true} if at least one {@linkplain Citation#getIdentifiers() identifier} in
     * {@code c1} is equal to an identifier in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The identifier ordering is not significant.
     *
     * <p>If (and <em>only</em> if) the citations do not contains any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,Citation) titleMatches}
     * method. This fallback exists for compatibility with client codes using the citation
     * {@linkplain Citation#getTitle() titles} without identifiers.</p>
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one identifier,
     *         title or alternate title matches.
     */
    public static boolean identifierMatches(final Citation c1, final Citation c2) {
        return org.apache.sis.internal.util.Citations.identifierMatches(c1, c2);
    }

    /**
     * Returns {@code true} if any {@linkplain Citation#getIdentifiers() identifiers} in the given
     * citation matches the given string. The comparison is case-insensitive and ignores every
     * character which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * <p>If (and <em>only</em> if) the citation does not contain any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,String) titleMatches}
     * method. This fallback exists for compatibility with client codes using citation
     * {@linkplain Citation#getTitle() titles} without identifiers.</p>
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  identifier The identifier to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or alternate title
     *         matches the given string.
     */
    public static boolean identifierMatches(final Citation citation, final String identifier) {
        return org.apache.sis.internal.util.Citations.identifierMatches(citation, identifier);
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method is useful for extracting the namespace from an authority, for example {@code "EPSG"}.
     * The implementation performs the following choices:
     *
     * <ul>
     *   <li>If the given citation is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the citation contains at least one {@linkplain Citation#getIdentifiers() identifier}, then:
     *     <ul>
     *       <li>If at least one identifier is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the shortest of those identifiers is returned.</li>
     *       <li>Otherwise the shortest identifier is returned, despite not being a Unicode identifier.</li>
     *     </ul></li>
     *   <li>Otherwise if the citation contains at least one {@linkplain Citation#getTitle() title} or
     *       {@linkplain Citation#getAlternateTitles() alternate title}, then:
     *     <ul>
     *       <li>If at least one title is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the shortest of those titles is returned.</li>
     *       <li>Otherwise the shortest title is returned, despite not being a Unicode identifier.</li>
     *     </ul></li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * {@note This method searches in alternate titles as a fallback because ISO specification said
     *        that those titles are often used for abbreviations.}
     *
     * This method ignores leading and trailing whitespaces of every character sequences.
     * Null references, empty character sequences and sequences of whitespaces only are ignored.
     *
     * @param  citation The citation for which to get the identifier, or {@code null}.
     * @return A non-empty identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not declare any identifier or title.
     */
    public static String getIdentifier(final Citation citation) {
        return org.apache.sis.internal.util.Citations.getIdentifier(citation);
    }
}
