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
import org.opengis.referencing.IdentifiedObject;        // For javadoc
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;
import org.apache.sis.internal.metadata.ServicesForUtility;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.metadata.iso.DefaultIdentifier;   // For javadoc

import static org.apache.sis.internal.util.Citations.equalsFiltered;


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
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Citations extends Static {
    /*
     * NOTE: other constants are defined in org.apache.sis.internal.metadata.Standards.
     */

    /**
     * The <a href="http://www.iso.org/">International Organization for Standardization</a>.
     *
     * @category Organization
     */
    public static final Citation ISO = new CitationConstant("ISO");

    /**
     * The <a href="http://www.opengeospatial.org">Open Geospatial Consortium</a> organization.
     * <cite>"Open Geospatial Consortium"</cite> is the new name for <cite>"OpenGIS consortium"</cite>.
     * An {@linkplain DefaultCitation#getAlternateTitles() alternate title} for this citation is "OGC"
     * (according ISO 19115, alternate titles often contain abbreviations).
     *
     * @category Organization
     */
    public static final Citation OGC = new CitationConstant(Constants.OGC);

    /**
     * The <a href="http://www.ogp.org.uk">International Association of Oil &amp; Gas Producers</a> organization.
     * This organization is responsible for maintainance of {@link #EPSG} database.
     *
     * @since 0.4
     *
     * @deprecated The OGP organization is now known as IOGP. This citation will be removed in SIS 0.7
     *             because of this name change and for avoiding confusion with {@link #EPSG} citation.
     */
    @Deprecated
    public static final Citation OGP = new CitationConstant(Constants.IOGP);

    /**
     * The <a href="http://www.epsg.org">EPSG Geodetic Parameter Dataset</a> identifier space.
     * EPSG is not an organization by itself, but is the <em>identifier space</em> managed by the
     * <a href="http://www.iogp.org">International Association of Oil &amp; Gas producers</a> (IOGP) organization
     * for {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system} identifiers.
     *
     * <div class="note"><b>Historical note:</b>
     * The EPSG acronym meaning was <cite>European Petroleum Survey Group</cite>.
     * But this meaning does not apply anymore since the European and American associations merged into
     * the <cite>Association of Oil &amp; Gas producers</cite> (OGP), later renamed as IOGP.
     * The legacy acronym now applies only to the database Coordinate Reference System definitions,
     * known as <cite>EPSG Geodetic Parameter Dataset</cite>.</div>
     *
     * The citation {@linkplain DefaultCitation#getCitedResponsibleParties() responsible party} is
     * the IOGP organization, but the {@link IdentifierSpace#getName() namespace} is {@code "EPSG"}.
     * The {@code "EPSG"} name shall be used in URN, but the {@code codeSpace} attribute value in
     * GML files can be “EPSG” or the authority abbreviation, either “IOGP” or the older “OGP”.
     * Example:
     *
     * {@preformat xml
     *   <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
     * }
     *
     * @see #AUTO
     * @see #AUTO2
     * @see #CRS
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> EPSG = new CitationConstant.Authority<>(Constants.EPSG);

    /**
     * The <a href="http://sis.apache.org">Apache SIS</a> project.
     *
     * @since 0.4
     */
    public static final Citation SIS = new CitationConstant(Constants.SIS);

    /**
     * The <a href="http://www.esri.com">ESRI</a> organization.
     * This company defines many Coordinate Reference Systems in addition to the {@linkplain #EPSG} ones.
     *
     * @category Organization
     *
     * @since 0.4
     */
    public static final Citation ESRI = new CitationConstant("ESRI");

    /**
     * The <a href="http://www.oracle.com">Oracle</a> organization.
     *
     * @category Organization
     *
     * @since 0.4
     */
    public static final Citation ORACLE = new CitationConstant("Oracle");

    /**
     * The <a href="http://www.unidata.ucar.edu/software/netcdf-java">NetCDF</a> specification.
     *
     * @category Specification
     *
     * @since 0.4
     */
    public static final Citation NETCDF = new CitationConstant("NetCDF");

    /**
     * The <a href="http://trac.osgeo.org/geotiff/">GeoTIFF</a> specification.
     * This specification identifies some map projections by their own numerical codes.
     *
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> GEOTIFF = new CitationConstant.Authority<>("GeoTIFF");

    /**
     * The <a href="http://trac.osgeo.org/proj/">Proj.4</a> project.
     *
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> PROJ4 = new CitationConstant.Authority<>("Proj4");

    /**
     * The MapInfo software. This software defines its own projection codes.
     *
     * @category Code space
     *
     * @since 0.6
     */
    public static final IdentifierSpace<Integer> MAP_INFO = new CitationConstant.Authority<>("MapInfo");

    /**
     * The <a href="http://www.iho.int/iho_pubs/standard/S-57Ed3.1/31Main.pdf">IHO transfer standard
     * for digital hydrographic data</a> specification.
     *
     * @category Code space
     *
     * @since 0.6
     */
    public static final IdentifierSpace<Integer> S57 = new CitationConstant.Authority<>("S57");

    /**
     * The <cite>International Standard Book Number</cite> (ISBN) defined by ISO-2108.
     * The ISO-19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * @see DefaultCitation#getISBN()
     *
     * @category Code space
     */
    public static final IdentifierSpace<String> ISBN = new NonMarshalledAuthority<>("ISBN", NonMarshalledAuthority.ISBN);

    /**
     * The <cite>International Standard Serial Number</cite> (ISSN) defined by ISO-3297.
     * The ISO-19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * @see DefaultCitation#getISSN()
     *
     * @category Code space
     */
    public static final IdentifierSpace<String> ISSN = new NonMarshalledAuthority<>("ISSN", NonMarshalledAuthority.ISSN);

    /**
     * List of citations declared in this class.
     * Most frequently used citations (at least in SIS) should be first.
     */
    private static final CitationConstant[] CITATIONS = {
        (CitationConstant) EPSG,
        (CitationConstant) OGC,
        (CitationConstant) ISO,
        (CitationConstant) OGP,
        (CitationConstant) NETCDF,
        (CitationConstant) GEOTIFF,
        (CitationConstant) ESRI,
        (CitationConstant) ORACLE,
        (CitationConstant) PROJ4,
        (CitationConstant) MAP_INFO,
        (CitationConstant) S57,
        (CitationConstant) ISBN,
        (CitationConstant) ISSN,
        (CitationConstant) SIS
    };

    static {  // Must be after CITATIONS array construction.
        SystemListener.add(new SystemListener(Modules.METADATA) {
            @Override protected void classpathChanged() {refresh();}
            @Override protected void databaseChanged()  {refresh();}
        });
    }

    /**
     * Do not allows instantiation of this class.
     */
    private Citations() {
    }

    /**
     * Invoked when the content of the citation constants (title, responsible party, URL, <i>etc.</i>)
     * may have changed. This method notifies all citations that they will need to refresh their content.
     */
    static void refresh() {
        for (final CitationConstant citation : CITATIONS) {
            citation.refresh();
        }
    }

    /**
     * Returns a citation of the given identifier. The method makes the following choice:
     *
     * <ul>
     *   <li>If the given title is {@code null} or empty (ignoring spaces), then this method returns {@code null}.</li>
     *   <li>Otherwise if the given string matches an {@linkplain DefaultCitation#getIdentifiers() identifier} of one of
     *       the pre-defined constants ({@link #EPSG}, {@link #GEOTIFF}, <i>etc.</i>), then that constant is returned.</li>
     *   <li>Otherwise, a new citation is created with the specified name as the title.</li>
     * </ul>
     *
     * @param  identifier The citation title (or alternate title), or {@code null}.
     * @return A citation using the specified name, or {@code null} if the given title is null or empty.
     */
    public static Citation fromName(String identifier) {
        if (identifier == null || ((identifier = CharSequences.trimWhitespaces(identifier)).isEmpty())) {
            return null;
        }
        for (final CitationConstant citation : CITATIONS) {
            if (equalsFiltered(identifier, citation.title)) {
                return citation;
            }
        }
        /*
         * Additional identifiers other than the ones declared to CitationConstant constructors. Those identifiers
         * shall be the same than the ones added by 'ServicesForUtility.createCitation(String)'.
         */
        if (equalsFiltered(identifier, ServicesForUtility.OGP)) {
            return OGP;
        }
        /*
         * If we found no match, org.apache.sis.internal.metadata.ServicesForUtility expects the default citation
         * to be of this exact class: SimpleCitation (not a subclass). If the type of citation created below is
         * modified, then we need to review ServicesForUtility.getCitationConstant(String) method body.
         */
        return new SimpleCitation(identifier);
    }

    /**
     * Returns {@code true} if at least one {@linkplain DefaultCitation#getTitle() title} or
     * {@linkplain DefaultCitation#getAlternateTitles() alternate title} in {@code c1} is leniently
     * equal to a title or alternate title in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The titles ordering is not significant.
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one title or alternate title matches.
     */
    public static boolean titleMatches(final Citation c1, final Citation c2) {
        return org.apache.sis.internal.util.Citations.titleMatches(c1, c2);
    }

    /**
     * Returns {@code true} if the {@linkplain DefaultCitation#getTitle() title} or any
     * {@linkplain DefaultCitation#getAlternateTitles() alternate title} in the given citation
     * matches the given string. The comparison is case-insensitive and ignores every character
     * which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  title The title or alternate title to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or an alternate
     *         title matches the given string.
     */
    public static boolean titleMatches(final Citation citation, final String title) {
        return org.apache.sis.internal.util.Citations.titleMatches(citation, title);
    }

    /**
     * Returns {@code true} if at least one {@linkplain DefaultCitation#getIdentifiers() identifier}
     * {@linkplain DefaultIdentifier#getCode() code} in {@code c1} is equal to an identifier code in
     * {@code c2}. {@linkplain DefaultIdentifier#getCodeSpace() Code spaces} are compared only if
     * provided in the two identifiers being compared. Comparisons are case-insensitive and ignores
     * every character which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     * The identifier ordering is not significant.
     *
     * <p>If (and <em>only</em> if) the citations do not contains any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,Citation) titleMatches}
     * method. This fallback exists for compatibility with client codes using the citation
     * {@linkplain DefaultCitation#getTitle() titles} without identifiers.</p>
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one identifier matches.
     */
    public static boolean identifierMatches(final Citation c1, final Citation c2) {
        return org.apache.sis.internal.util.Citations.identifierMatches(c1, c2);
    }

    /**
     * Returns {@code true} if at least one {@linkplain DefaultCitation#getIdentifiers() identifier}
     * in the given citation have a {@linkplain DefaultIdentifier#getCode() code} matching the given
     * string. The comparison is case-insensitive and ignores every character which is not a
     * {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * <p>If (and <em>only</em> if) the citation does not contain any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,String) titleMatches}
     * method. This fallback exists for compatibility with citations using
     * {@linkplain DefaultCitation#getTitle() title} and
     * {@linkplain DefaultCitation#getAlternateTitles() alternate titles} (often abbreviations)
     * without identifiers.</p>
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  identifier The identifier to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and an identifier matches the given string.
     */
    public static boolean identifierMatches(final Citation citation, final String identifier) {
        return org.apache.sis.internal.util.Citations.identifierMatches(citation, null, identifier);
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method is useful for extracting a short designation of an authority (e.g. {@code "EPSG"})
     * for display purpose. This method performs the following choices:
     *
     * <ul>
     *   <li>If the given citation is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the citation contains at least one
     *       non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated}
     *       {@linkplain DefaultCitation#getIdentifiers() identifier}, then:
     *     <ul>
     *       <li>If at least one non-deprecated identifier is a
     *           {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier unicode identifier},
     *           then the shortest of those identifiers is returned.</li>
     *       <li>Otherwise the shortest non-deprecated identifier is returned,
     *           despite not being a Unicode identifier.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise if the citation contains at least one {@linkplain DefaultCitation#getTitle() title} or
     *       {@linkplain DefaultCitation#getAlternateTitles() alternate title}, then:
     *     <ul>
     *       <li>If at least one title is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the shortest of those titles is returned.</li>
     *       <li>Otherwise the shortest title is returned, despite not being a Unicode identifier.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This method searches in alternate titles as a fallback because ISO specification said
     * that those titles are often used for abbreviations. However titles are never searched
     * if the given citation contains at least one identifier.</div>
     *
     * This method ignores leading and trailing {@linkplain Character#isWhitespace(int) whitespaces}
     * in every character sequences. Null or empty trimmed character sequences are ignored.
     * This method does <em>not</em> remove {@linkplain Character#isIdentifierIgnorable(int) ignorable characters}.
     * The result is a string which is <em>likely</em>, but not guaranteed, to be a valid XML or Unicode identifier.
     * The returned string is useful when an "identifier-like" string is desired for display or information purpose,
     * but does not need to be a strictly valid identifier.
     *
     * @param  citation The citation for which to get the identifier, or {@code null}.
     * @return A non-empty identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not declare any identifier or title.
     */
    public static String getIdentifier(final Citation citation) {
        return org.apache.sis.internal.util.Citations.getIdentifier(citation, false);
    }

    /**
     * Infers a valid Unicode identifier from the given citation, or returns {@code null} if none.
     * This method is useful for extracting a short designation of an authority (e.g. {@code "EPSG"})
     * for processing purpose. This method performs the following actions:
     *
     * <ul>
     *   <li>First, invoke {@link #getIdentifier(Citation)}.</li>
     *   <li>If the result of above method call is {@code null} or is not a
     *       {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier valid Unicode identifier},
     *       then return {@code null}.</li>
     *   <li>Otherwise remove the {@linkplain Character#isIdentifierIgnorable(int) ignorable characters},
     *       if any, and returns the result.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * examples of ignorable identifier characters are <cite>zero width space</cite> or <cite>word joiner</cite>.
     * Those characters are illegal in XML identifiers, and should therfore be removed if the Unicode identifier
     * may also be used as XML identifier.</div>
     *
     * If non-null, the result is suitable for use as a XML identifier except for a few uncommon characters
     * ({@code µ}, {@code ª} (feminine ordinal indicator), {@code º} (masculine ordinal indicator) and {@code ⁔}).
     *
     * @param  citation The citation for which to get the Unicode identifier, or {@code null}.
     * @return A non-empty Unicode identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not have any Unicode identifier or title.
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier
     * @see org.apache.sis.referencing.IdentifiedObjects#getUnicodeIdentifier(IdentifiedObject)
     * @see org.apache.sis.util.CharSequences#isUnicodeIdentifier(CharSequence)
     *
     * @since 0.6
     */
    public static String getUnicodeIdentifier(final Citation citation) {
        return org.apache.sis.internal.util.Citations.getUnicodeIdentifier(citation);
    }
}
