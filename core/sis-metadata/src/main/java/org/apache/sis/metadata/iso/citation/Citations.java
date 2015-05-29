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
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.metadata.iso.DefaultIdentifier;   // For javadoc

import static org.apache.sis.internal.util.Citations.equalsFiltered;


/**
 * A set of pre-defined constants and static methods working on {@linkplain Citation citations}.
 * The most common usage for those constants is to give information about who maintains the codes
 * that we use in identifiers.
 *
 * <div class="note"><b>Example:</b> {@code "EPSG:4326"} is a widely-used identifier
 * for the <cite>World Geodetic System (WGS) 1984</cite> Coordinate Reference System (CRS).
 * The {@code "4326"} part is the identifier {@linkplain DefaultIdentifier#getCode() code} and
 * the {@code "EPSG"} part is the identifier {@linkplain DefaultIdentifier#getCodeSpace() code space}.
 * The meaning of codes in that code space is controlled by an {@linkplain DefaultIdentifier#getAuthority() authority},
 * the <cite>EPSG Geodetic Parameter Dataset</cite>. The {@linkplain DefaultCitation#getCitedResponsibleParties() cited
 * reposible party} for the EPSG dataset is the <cite>International Association of Oil &amp; Gas producers</cite> (IOGP).
 * </div>
 *
 * The constants defined in this class are typically values returned by
 * {@link org.apache.sis.metadata.iso.ImmutableIdentifier#getAuthority()}.
 * Citations to resources that define identifiers ({@linkplain #EPSG}, {@linkplain #ISBN}, {@linkplain #ISSN},
 * <i>etc.</i>) are instances of {@link IdentifierSpace}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Citations extends Static {
    /**
     * The <a href="http://www.iso.org/">International Organization for Standardization</a>.
     *
     * @deprecated No replacement since ISO is an {@linkplain DefaultOrganisation organisation} rather than a citation.
     */
    @Deprecated
    public static final Citation ISO = new SimpleCitation("ISO");

    /**
     * The <cite>ISO 19115 Geographic Information — Metadata</cite> standards published by the
     * <a href="http://www.iso.org/">International Organization for Standardization</a>.
     * Apache SIS uses this constant for a merge of two standards:
     *
     * <ul>
     *   <li>ISO 19115-1 Geographic Information — Metadata Part 1: Fundamentals</li>
     *   <li>ISO 19115-2 Geographic Information — Metadata Part 2: Extensions for imagery and gridded data</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * SIS uses a single citation that encompasses all standards of the ISO 19115 series because the SIS's API
     * tries to provide a more unified view of some classes which are splitted between the two above standards
     * (e.g. {@code MI_Band} versus {@code MD_Band}).</div>
     *
     * @see org.opengis.annotation.Specification#ISO_19115
     * @see org.opengis.annotation.Specification#ISO_19115_2
     *
     * @since 0.6
     */
    public static final Citation ISO_19115 = new CitationConstant("ISO 19115");

    /**
     * The authority for identifiers found in specifications from the
     * <a href="http://www.opengeospatial.org">Open Geospatial Consortium</a>.
     * The specification actually referenced by this citation is implementation dependent
     * and may change in future SIS version. Some of the specifications used are:
     *
     * <ul>
     *   <li><a href="http://www.opengeospatial.org/standards/ct">Coordinate Transformation Service</a></li>
     *   <li><a href="http://www.opengeospatial.org/standards/wms">Web Map Service</a></li>
     *   <li>Definition identifier URNs in OGC namespace</li>
     * </ul>
     *
     * Apache SIS uses this authority mostly for map projection methods and parameters as they were defined in older
     * OGC specifications (in more recent specifications, {@linkplain #EPSG} identifiers tend to be more widely used).
     * We do not commit to a particular OGC specification in order to keep the flexibility to change the
     * {@linkplain DefaultCitation#getTitle() title} or URL according newer OGC publications.
     *
     * @see #EPSG
     * @see #ESRI
     */
    public static final IdentifierSpace<String> OGC = new CitationConstant.Authority<>(Constants.OGC);

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
    public static final Citation OGP = new SimpleCitation("OGP");

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
     * the IOGP organization, but the {@linkplain IdentifierSpace#getName() namespace} is {@code "EPSG"}.
     * The {@code "EPSG"} name shall be used in URN, but the {@code codeSpace} attribute value in
     * GML files can be “EPSG” or the authority abbreviation, either “IOGP” or the older “OGP”,
     * as in the following example:
     *
     * {@preformat xml
     *   <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
     * }
     *
     * @category Code space
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> EPSG = new CitationConstant.Authority<>(Constants.EPSG);

    /**
     * The codespace of objects that are specific to the <a href="http://sis.apache.org">Apache SIS</a> project.
     *
     * @since 0.4
     */
    public static final Citation SIS = new CitationConstant(Constants.SIS);

    /**
     * The authority for identifiers of objects provided by <a href="http://www.esri.com">ESRI</a>.
     * This citation is used as the authority for many map projection method and parameter names
     * other than the {@linkplain #EPSG} ones.
     *
     * <div class="note"><b>Note:</b>
     * Many parameter names defined by {@linkplain #OGC} are very similar to the ESRI ones, except for the case.</div>
     *
     * @since 0.4
     *
     * @see #OGC
     * @see #EPSG
     */
    public static final Citation ESRI = new CitationConstant("ESRI");

    /**
     * The <a href="http://www.oracle.com">Oracle</a> organization.
     *
     * @deprecated No replacement since Oracle is an {@linkplain DefaultOrganisation organisation} rather
     *             than a citation, and we do not have Oracle-specific objects.
     *
     * @since 0.4
     */
    @Deprecated
    public static final Citation ORACLE = new CitationConstant("Oracle");

    /**
     * The <a href="http://www.unidata.ucar.edu/software/netcdf-java">NetCDF</a> specification.
     *
     * @since 0.4
     */
    public static final Citation NETCDF = new CitationConstant("NetCDF");

    /**
     * The <a href="http://trac.osgeo.org/geotiff/">GeoTIFF</a> specification.
     * This specification identifies some map projections by their own numerical codes.
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> GEOTIFF = new CitationConstant.Authority<>("GeoTIFF");

    /**
     * The authority for identifiers of objects defined by the <a href="http://trac.osgeo.org/proj/">Proj.4</a> project.
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> PROJ4 = new CitationConstant.Authority<>("Proj4");

    /**
     * The authority for identifiers of objects defined by MapInfo.
     *
     * @since 0.6
     */
    public static final IdentifierSpace<Integer> MAP_INFO = new CitationConstant.Authority<>("MapInfo");

    /**
     * The <a href="http://www.iho.int/iho_pubs/standard/S-57Ed3.1/31Main.pdf">IHO transfer standard
     * for digital hydrographic data</a> specification.
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
     */
    public static final IdentifierSpace<String> ISBN = new NonMarshalledAuthority<>("ISBN", NonMarshalledAuthority.ISBN);

    /**
     * The <cite>International Standard Serial Number</cite> (ISSN) defined by ISO-3297.
     * The ISO-19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * @see DefaultCitation#getISSN()
     */
    public static final IdentifierSpace<String> ISSN = new NonMarshalledAuthority<>("ISSN", NonMarshalledAuthority.ISSN);

    /**
     * List of citations declared in this class.
     * Most frequently used citations (at least in SIS) should be first.
     */
    private static final SimpleCitation[] CITATIONS = {
        (SimpleCitation) EPSG,
        (SimpleCitation) OGC,
        (SimpleCitation) ISO,
        (SimpleCitation) ISO_19115,
        (SimpleCitation) OGP,
        (SimpleCitation) NETCDF,
        (SimpleCitation) GEOTIFF,
        (SimpleCitation) ESRI,
        (SimpleCitation) ORACLE,
        (SimpleCitation) PROJ4,
        (SimpleCitation) MAP_INFO,
        (SimpleCitation) S57,
        (SimpleCitation) ISBN,
        (SimpleCitation) ISSN,
        (SimpleCitation) SIS
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
        for (final SimpleCitation citation : CITATIONS) {
            if (citation instanceof CitationConstant) {
                ((CitationConstant) citation).refresh();
            }
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
        for (final SimpleCitation citation : CITATIONS) {
            if (equalsFiltered(identifier, citation.title)) {
                return citation;
            }
        }
        /*
         * Temporary check to be removed after we deleted the deprecated citation.
         */
        if (equalsFiltered(identifier, Constants.IOGP)) {
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
     *   <li>Otherwise if the collection of {@linkplain DefaultCitation#getIdentifiers() citation identifiers}
     *       contains at least one non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated}
     *       identifier, then:
     *     <ul>
     *       <li>If the code and codespace of at least one non-deprecated identifier are
     *           {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier unicode identifiers}, then
     *           the <strong>first</strong> of those identifiers is returned in a {@code "[codespace:]code"} format.
     *           Only the first character of the resulting string needs to be an
     *           {@linkplain Character#isUnicodeIdentifierStart(int) identifier start character}.</li>
     *       <li>Otherwise the first non-empty and non-deprecated identifier is returned in a
     *           {@code "[codespace:]code"} format, despite not being a valid Unicode identifier.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise if the citation contains at least one non-deprecated {@linkplain DefaultCitation#getTitle() title}
     *       or {@linkplain DefaultCitation#getAlternateTitles() alternate title}, then:
     *     <ul>
     *       <li>If at least one non-deprecated title is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the <strong>first</strong> of those titles is returned.</li>
     *       <li>Otherwise the first non-empty and non-deprecated title is returned,
     *           despite not being a valid Unicode identifier.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This method searches in alternate titles as a fallback because ISO specification said
     * that those titles are often used for abbreviations. However titles are never searched
     * if the given citation contains at least one non-empty and non-deprecated identifier.</div>
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
     *   <li>First, performs the same work than {@link #getIdentifier(Citation)} except that {@code '_'}
     *       is used instead of {@link org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR ':'}
     *       as the separator between the codespace and the code.</li>
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
     * If non-null, the result is suitable for use as a XML identifier except for a few uncommon characters.
     *
     * <div class="note"><b>Note:</b>
     * the following characters are invalid in XML identifiers. However since they are valid in Unicode identifiers,
     * they could be included in the string returned by this method:
     * <ul>
     *   <li>{@code µ}</li>
     *   <li>{@code ª} (feminine ordinal indicator)</li>
     *   <li>{@code º} (masculine ordinal indicator)</li>
     *   <li>{@code ⁔}</li>
     * </ul></div>
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
