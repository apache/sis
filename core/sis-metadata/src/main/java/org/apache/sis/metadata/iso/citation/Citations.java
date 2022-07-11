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

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Iterator;
import java.util.Collection;
import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Static;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.metadata.Identifiers;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.metadata.iso.DefaultIdentifier;           // For javadoc


/**
 * A set of pre-defined constants and static methods working on {@linkplain Citation citations}.
 * This class provides two kinds of {@code Citation} constants:
 *
 * <ul>
 *   <li>Instances of {@link Citation} are references mostly for human reading.</li>
 *   <li>Instances of {@link IdentifierSpace} provide the code spaces for identifiers
 *       (most often {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} identifiers)
 *       together with information about who maintains those identifiers.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b> {@code "EPSG:4326"} is a widely-used identifier
 * for the <cite>“World Geodetic System (WGS) 1984”</cite> Coordinate Reference System (CRS).
 * The {@code "4326"} part is the identifier {@linkplain DefaultIdentifier#getCode() code} and
 * the {@code "EPSG"} part is the identifier {@linkplain DefaultIdentifier#getCodeSpace() code space}.
 * The meaning of codes in that code space is controlled by an {@linkplain DefaultIdentifier#getAuthority() authority},
 * the <cite>“EPSG Geodetic Parameter Dataset”</cite>. The {@linkplain DefaultCitation#getCitedResponsibleParties() cited
 * responsible party} for the EPSG dataset is the <cite>“International Association of Oil &amp; Gas producers”</cite> (IOGP).
 * </div>
 *
 * The constants defined in this class are typically values returned by:
 * <ul>
 *   <li>{@link DefaultCitation#getIdentifiers()} for the {@link #ISBN} and {@link #ISSN} constants.</li>
 *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()} for other {@code IdentifierSpace} constants.</li>
 *   <li>{@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataStandards()} for other {@code Citation} constants.</li>
 * </ul>
 *
 * The static methods defined in this class are for:
 * <ul>
 *   <li>Inferring an identifier from a citation (this is useful mostly with {@code IdentifierSpace} instances).</li>
 *   <li>Determining if two instances can be considered the same {@code Citation} by comparing their titles or
 *       their identifiers.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.2
 * @since   0.3
 * @module
 */
public final class Citations extends Static {
    /**
     * The <cite>Geographic Information — Metadata</cite> standards defined by ISO 19115.
     * This list contains two standards published by the
     * <a href="https://www.iso.org/">International Organization for Standardization</a>:
     *
     * <ul>
     *   <li>ISO 19115-1 Geographic Information — Metadata Part 1: Fundamentals</li>
     *   <li>ISO 19115-2 Geographic Information — Metadata Part 2: Extensions for imagery and gridded data</li>
     * </ul>
     *
     * Apache SIS always uses those standards together (actually the SIS's API is a merge of those two standards,
     * providing for example a unified view of {@code MI_Band} and {@code MD_Band}). This is why those standards
     * are defined in a collection rather than as separated constants.
     *
     * <h4>Content and future evolution</h4>
     * The content of this list may vary in future Apache SIS versions depending on the evolution of standards
     * and in the way that SIS support them. The current content is:
     *
     * <ul>
     *   <li>{@linkplain org.opengis.annotation.Specification#ISO_19115   ISO 19115-1:2014} at index 0.</li>
     *   <li>{@linkplain org.opengis.annotation.Specification#ISO_19115_2 ISO 19115-2:2019} at index 1.</li>
     * </ul>
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataStandards()}</li>
     * </ul>
     *
     * @since 0.6
     */
    public static final List<Citation> ISO_19115 = UnmodifiableArrayList.wrap(new CitationConstant[] {
        new CitationConstant("ISO 19115-1"),
        new CitationConstant("ISO 19115-2")
    });

    /**
     * The <a href="https://www.iogp.org/">International Association of Oil &amp; Gas producers</a> (IOGP) organization.
     * This organization is responsible for maintainance of {@link #EPSG} database.
     *
     * <p>We do not expose this citation in public API because it is an organization rather than a reference
     * to a document or a database (see SIS-200). For now we keep this citation mostly for resolving the legacy
     * "OGP" identifier as "IOGP" (see the special case in fromName(String) method). This is also a way to share
     * the same citation instance in GML like below:</p>
     *
     * {@preformat xml
     *   <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
     * }
     *
     * @see #fromName(String)
     * @see org.apache.sis.internal.jaxb.referencing.Code#getIdentifier()
     * @see <a href="http://issues.apache.org/jira/browse/SIS-200">SIS-200</a>
     */
    static final CitationConstant IOGP = new CitationConstant(Constants.IOGP);

    /**
     * The authority for identifiers of objects defined by the
     * <a href="https://epsg.org/">EPSG Geodetic Parameter Dataset</a>.
     * EPSG is not an organization by itself, but is the <em>identifier space</em> managed by the
     * <a href="https://www.iogp.org/">International Association of Oil &amp; Gas producers</a> (IOGP) organization
     * for {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} identifiers.
     * EPSG is the default namespace of map projection method and parameter names in Apache SIS.
     *
     * <div class="note"><b>Historical note:</b>
     * The EPSG acronym meaning was <cite>“European Petroleum Survey Group”</cite>.
     * But this meaning does not apply anymore since the European and American associations merged into
     * the <cite>“Association of Oil &amp; Gas producers”</cite> (OGP), later renamed as IOGP.
     * The legacy acronym now applies only to the database Coordinate Reference System definitions,
     * known as <cite>“EPSG Geodetic Parameter Dataset”</cite>.</div>
     *
     * The citation {@linkplain DefaultCitation#getCitedResponsibleParties() responsible party} is
     * the IOGP organization, but the {@linkplain IdentifierSpace#getName() namespace} is {@code "EPSG"}.
     *
     * <h4>When to use "EPSG" and "IOGP"</h4>
     * For all usages except GML, the {@code "EPSG"} namespace shall be used for identifying
     * {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference System} objects.
     * But in the particular case of GML, both {@code "EPSG"} and {@code "IOGP"} appear in different locations.
     * For example:
     *
     * {@preformat xml
     *   <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
     * }
     *
     * Guidelines:
     * <ul>
     *   <li>The {@code "EPSG"} name shall be used in URN. This part of the URN is called "the authority" and
     *       is returned by both {@code Identifier.getAuthority()} and {@code Identifier.getCodeSpace()}.</li>
     *   <li>The {@code "IOGP"} name appears in the GML {@code codeSpace} attribute, but despite the attribute name this
     *       is <strong>not</strong> the {@code Identifier.getCodeSpace()} value of the {@code "EPSG:4326"} identifier.
     *       Instead, Apache SIS considers {@code "IOGP"} as the codespace of the {@code "IOGP:EPSG"} identifier,
     *       which is the {@linkplain DefaultCitation#getIdentifiers() identifier of this citation}.</li>
     * </ul>
     *
     * In other words, Apache SIS considers "IOGP" as the codespace of the "EPSG" codespace, but there is usually
     * no need to go to such depth in identifier hierarchy except when handling GML documents. For this reason,
     * the {@code "IOGP:EPSG"} identifier is handled in a special way by {@link #getIdentifier(Citation)},
     * which return only {@code "EPSG"}.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     *   <li>{@link org.apache.sis.referencing.factory.sql.EPSGFactory#getAuthority()}
     *       with the addition of version information.</li>
     * </ul>
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> EPSG = new CitationConstant.Authority<>(Constants.EPSG);

    /**
     * The authority for identifiers of objects defined by the
     * <a href="https://www.ogc.org/standards/wms">Web Map Service</a> (WMS) specification.
     * The WMS 1.3 specifications is also known as ISO 19128
     * <cite>Geographic Information — Web map server interface</cite> standard.
     *
     * <p>The citation {@linkplain DefaultCitation#getCitedResponsibleParties() responsible parties}
     * are the OGC and ISO organizations.
     * The {@linkplain IdentifierSpace#getName() namespace} declared by this constant is {@code "OGC"},
     * but the {@code "CRS"}, {@code "AUTO"} and {@code "AUTO2"} namespaces are also commonly found in practice.</p>
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.factory.CommonAuthorityFactory#getAuthority()}</li>
     * </ul>
     *
     * @since 0.7
     */
    public static final IdentifierSpace<Integer> WMS = new CitationConstant.Authority<>("WMS", Constants.OGC);

    /**
     * The authority for identifiers found in specifications from the
     * <a href="https://www.ogc.org/">Open Geospatial Consortium</a>.
     * The {@linkplain IdentifierSpace#getName() name} of this identifier space is fixed to {@code "OGC"}.
     * Apache SIS uses this authority mostly for map projection methods and parameters as they were defined in older
     * OGC specifications (in more recent specifications, {@linkplain #EPSG} identifiers tend to be more widely used).
     *
     * <div class="note"><b>Example</b>
     * the Mercator projection can be defined by an operation method having the {@code "OGC:Mercator_1SP"} identifier
     * and the following parameters:
     *
     * <table class="sis">
     * <caption>Example of identifiers in OGC name space</caption>
     * <tr><th>Name in OGC namespace</th>           <th>Name in default namespace (EPSG)</th></tr>
     * <tr><td>{@code "OGC:semi_major"}</td>        <td></td></tr>
     * <tr><td>{@code "OGC:semi_minor"}</td>        <td></td></tr>
     * <tr><td>{@code "OGC:latitude_of_origin"}</td><td>Latitude of natural origin</td></tr>
     * <tr><td>{@code "OGC:central_meridian"}</td>  <td>Longitude of natural origin</td></tr>
     * <tr><td>{@code "OGC:scale_factor"}</td>      <td>Scale factor at natural origin</td></tr>
     * <tr><td>{@code "OGC:false_easting"}</td>     <td>False easting</td></tr>
     * <tr><td>{@code "OGC:false_northing"}</td>    <td>False northing</td></tr>
     * </table></div>
     *
     * <h4>Specifications referenced</h4>
     * The specification actually referenced by this citation is implementation dependent
     * and may change in future SIS version. Some of the specifications used are:
     *
     * <ul>
     *   <li><a href="https://www.ogc.org/">Coordinate Transformation Service</a></li>
     *   <li><a href="https://www.ogc.org/standards/wms">Web Map Service</a></li>
     *   <li><a href="https://portal.ogc.org/files/?artifact_id=24045">Definition identifier URNs in OGC namespace</a></li>
     * </ul>
     *
     * We do not commit to a particular OGC specification in order to keep the flexibility to change the
     * {@linkplain DefaultCitation#getTitle() title} or URL according newer OGC publications.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @see #EPSG
     * @see #ESRI
     */
    public static final IdentifierSpace<String> OGC = new CitationConstant.Authority<>(Constants.OGC);

    /**
     * The authority for identifiers of objects defined by <a href="https://www.esri.com">ESRI</a>.
     * The {@linkplain IdentifierSpace#getName() name} of this identifier space is fixed to {@code "ESRI"}.
     * This citation is used as the authority for many map projection method and parameter names
     * other than the {@linkplain #EPSG} ones.
     *
     * <div class="note"><b>Note</b>
     * many parameter names defined by {@linkplain #OGC} are very similar to the ESRI ones,
     * except for the case. Examples:
     *
     * <table class="sis">
     * <caption>Example of identifiers in ESRI name space</caption>
     * <tr><th>Name in ESRI namespace</th>           <th>Name in OGC namespace</th></tr>
     * <tr><td>{@code "ESRI:Semi_Major"}</td>        <td>{@code "OGC:semi_major"}</td></tr>
     * <tr><td>{@code "ESRI:Semi_Minor"}</td>        <td>{@code "OGC:semi_minor"}</td></tr>
     * <tr><td>{@code "ESRI:Latitude_Of_Origin"}</td><td>{@code "OGC:latitude_of_origin"}</td></tr>
     * <tr><td>{@code "ESRI:Central_Meridian"}</td>  <td>{@code "OGC:central_meridian"}</td></tr>
     * <tr><td>{@code "ESRI:Scale_Factor"}</td>      <td>{@code "OGC:scale_factor"}</td></tr>
     * <tr><td>{@code "ESRI:False_Easting"}</td>     <td>{@code "OGC:false_easting"}</td></tr>
     * <tr><td>{@code "ESRI:False_Northing"}</td>    <td>{@code "OGC:false_northing"}</td></tr>
     * </table></div>
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @see #OGC
     * @see #EPSG
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> ESRI = new CitationConstant.Authority<>("ArcGIS", "ESRI");

    /**
     * The authority for identifiers of objects defined by the
     * <a href="https://www.wmo.int">World Meteorological Organization</a>.
     * The {@linkplain IdentifierSpace#getName() name} of this identifier space is fixed to {@code "WMO"}.
     * This citation is used as the authority for some coordinate operations other than EPSG and ESRI ones,
     * for example "Rotated latitude/longitude".
     *
     * @since 1.2
     */
    public static final IdentifierSpace<String> WMO = new CitationConstant.Authority<>("WMO");

    /**
     * The authority for identifiers of objects defined by the netCDF specification.
     * The {@linkplain IdentifierSpace#getName() name} of this identifier space is fixed to {@code "NetCDF"}.
     * This citation is used as the authority for some map projection method and parameter names
     * as used in netCDF files.
     *
     * <div class="note"><b>Example</b>
     * the Mercator projection can be defined in a netCDF file with the following parameters:
     *
     * <table class="sis">
     * <caption>Example of identifiers in netCDF name space</caption>
     * <tr><th>Name in netCDF namespace</th>                           <th>Name in default namespace (EPSG)</th></tr>
     * <tr><td>{@code "NetCDF:semi_major_axis"}</td>                   <td></td></tr>
     * <tr><td>{@code "NetCDF:semi_minor_axis"}</td>                   <td></td></tr>
     * <tr><td>{@code "NetCDF:latitude_of_projection_origin"}</td>     <td>Latitude of natural origin</td></tr>
     * <tr><td>{@code "NetCDF:longitude_of_projection_origin"}</td>    <td>Longitude of natural origin</td></tr>
     * <tr><td>{@code "NetCDF:scale_factor_at_projection_origin"}</td> <td>Scale factor at natural origin</td></tr>
     * <tr><td>{@code "NetCDF:false_easting"}</td>                     <td>False easting</td></tr>
     * <tr><td>{@code "NetCDF:false_northing"}</td>                    <td>False northing</td></tr>
     * </table></div>
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> NETCDF = new CitationConstant.Authority<>("NetCDF");

    /**
     * The authority for identifiers of objects defined by the
     * the <a href="https://www.ogc.org/standards/geotiff">GeoTIFF</a> specification.
     * This specification identifies some map projections by their own numerical codes.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @since 0.4
     */
    public static final IdentifierSpace<Integer> GEOTIFF = new CitationConstant.Authority<>(Constants.GEOTIFF);

    /**
     * The authority for identifiers of objects defined by the <a href="https://proj.org/">PROJ</a> project.
     * We use the {@code PROJ4} name for historical reasons, because those identifiers were defined mostly
     * when the project was known as "Proj.4". Starting at PROJ version 6, EPSG identifiers should be used
     * instead.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @since 0.4
     */
    public static final IdentifierSpace<String> PROJ4 = new CitationConstant.Authority<>("PROJ", Constants.PROJ4);

    /**
     * The authority for identifiers of objects defined by MapInfo.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @since 0.6
     */
    public static final IdentifierSpace<Integer> MAP_INFO = new CitationConstant.Authority<>("MapInfo");

    /**
     * The <cite>IHO transfer standard for digital hydrographic data</cite> specification.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.referencing.ImmutableIdentifier#getAuthority()}</li>
     * </ul>
     *
     * @since 0.6
     */
    public static final IdentifierSpace<Integer> S57 = new CitationConstant.Authority<>("IHO S-57", "S57");

    /**
     * The <cite>International Standard Book Number</cite> (ISBN) defined by ISO-2108.
     * The ISO 19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link DefaultCitation#getIdentifiers()}</li>
     * </ul>
     *
     * @see DefaultCitation#getISBN()
     */
    public static final IdentifierSpace<String> ISBN = new NonMarshalledAuthority<>("ISBN", NonMarshalledAuthority.ISBN);

    /**
     * The <cite>International Standard Serial Number</cite> (ISSN) defined by ISO-3297.
     * The ISO 19115 metadata standard defines a specific attribute for this information,
     * but the SIS library handles it like any other identifier.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link DefaultCitation#getIdentifiers()}</li>
     * </ul>
     *
     * @see DefaultCitation#getISSN()
     */
    public static final IdentifierSpace<String> ISSN = new NonMarshalledAuthority<>("ISSN", NonMarshalledAuthority.ISSN);

    /**
     * The codespace of objects that are specific to the <a href="https://sis.apache.org/">Apache SIS</a> project.
     *
     * <h4>Main usage</h4>
     * This value can be returned by:
     * <ul>
     *   <li>{@link org.apache.sis.metadata.iso.quality.DefaultConformanceResult#getSpecification()}</li>
     * </ul>
     *
     * @since 0.4
     */
    public static final Citation SIS = new CitationConstant.Authority<String>(Constants.SIS);

    /**
     * List of <em>public</em> citations declared in this class.
     * Most frequently used citations (at least in SIS) should be first.
     * Non-public citations like {@link #IOGP} are handled separately.
     */
    private static final CitationConstant[] CITATIONS = {
        (CitationConstant) EPSG,
        (CitationConstant) OGC,
        (CitationConstant) WMS,                 // Must be after OGC because it declares the same namespace.
        (CitationConstant) ESRI,
        (CitationConstant) WMO,
        (CitationConstant) NETCDF,
        (CitationConstant) GEOTIFF,
        (CitationConstant) PROJ4,
        (CitationConstant) MAP_INFO,
        (CitationConstant) S57,
        (CitationConstant) ISBN,
        (CitationConstant) ISSN,
        (CitationConstant) SIS,
        (CitationConstant) ISO_19115.get(0),
        (CitationConstant) ISO_19115.get(1)
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
        IOGP.refresh();
    }

    /**
     * Returns the values declared in this {@code Citations} class.
     *
     * @return the value declared in this {@code Citations} class.
     *
     * @since 1.0
     */
    public static Citation[] values() {
        return Arrays.copyOf(CITATIONS, CITATIONS.length, Citation[].class);
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
     * @param  identifier  the citation title (or alternate title), or {@code null}.
     * @return a citation using the specified name, or {@code null} if the given title is null or empty.
     */
    public static Citation fromName(String identifier) {
        if (identifier == null || ((identifier = CharSequences.trimWhitespaces(identifier)).isEmpty())) {
            return null;
        }
        for (final CitationConstant citation : CITATIONS) {
            if (equalsFiltered(identifier, citation.namespace)) {
                return citation;
            }
        }
        if (equalsFiltered(identifier, Constants.CRS) || equalsFiltered(identifier, "WMS")) {
            return WMS;
        }
        // "OGP" is the old name of "IOGP" organization.
        if (equalsFiltered(identifier, "IOGP") || equalsFiltered(identifier, "OGP")) {
            return IOGP;
        }
        /*
         * If we found no match, org.apache.sis.internal.metadata.ServicesForUtility expects
         * that we return anything that is not an instance of CitationConstant.
         */
        return new SimpleCitation(identifier);
    }

    /**
     * The method to be used consistently for comparing titles or identifiers in all {@code fooMathes(…)}
     * methods declared in this class.
     *
     * @param  s1  the first characters sequence to compare, or {@code null}.
     * @param  s2  the second characters sequence to compare, or {@code null}.
     * @return {@code true} if both arguments are {@code null} or if the two given texts are equal,
     *         ignoring case and any characters other than digits and letters.
     */
    private static boolean equalsFiltered(final CharSequence s1, final CharSequence s2) {
        return CharSequences.equalsFiltered(s1, s2, Characters.Filter.LETTERS_AND_DIGITS, true);
    }

    /**
     * Returns the collection iterator, or {@code null} if the given collection is null or empty.
     *
     * @param  <E>         the type of elements in the collection.
     * @param  collection  the collection from which to get the iterator, or {@code null}.
     * @return the iterator over the given collection elements, or {@code null}.
     */
    private static <E> Iterator<E> nonEmptyIterator(final Collection<E> collection) {
        return (collection != null && !collection.isEmpty()) ? collection.iterator() : null;
    }

    /**
     * Returns {@code true} if at least one {@linkplain DefaultCitation#getTitle() title} or
     * {@linkplain DefaultCitation#getAlternateTitles() alternate title} in {@code c1} is leniently
     * equal to a title or alternate title in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The titles ordering is not significant.
     *
     * @param  c1  the first citation to compare, or {@code null}.
     * @param  c2  the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one title or alternate title matches.
     */
    public static boolean titleMatches(final Citation c1, final Citation c2) {
        if (c1 != null && c2 != null) {
            if (c1 == c2) {
                return true;                                                // Optimisation for a common case.
            }
            InternationalString candidate = c2.getTitle();
            Iterator<? extends InternationalString> iterator = null;
            do {
                if (candidate != null) {
                    final String unlocalized = candidate.toString(Locale.ROOT);
                    if (titleMatches(c1, unlocalized)) {
                        return true;
                    }
                    final String localized = candidate.toString();
                    if (!Objects.equals(localized, unlocalized)             // Slight optimization for a common case.
                            && titleMatches(c1, localized))
                    {
                        return true;
                    }
                }
                if (iterator == null) {
                    iterator = nonEmptyIterator(c2.getAlternateTitles());
                    if (iterator == null) break;
                }
                if (!iterator.hasNext()) break;
                candidate = iterator.next();
            } while (true);
        }
        return false;
    }

    /**
     * Returns {@code true} if the {@linkplain DefaultCitation#getTitle() title} or any
     * {@linkplain DefaultCitation#getAlternateTitles() alternate title} in the given citation
     * matches the given string. The comparison is case-insensitive and ignores every character
     * which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * @param  citation  the citation to check for, or {@code null}.
     * @param  title     the title or alternate title to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or an alternate
     *         title matches the given string.
     */
    public static boolean titleMatches(final Citation citation, final String title) {
        if (citation != null && title != null) {
            InternationalString candidate = citation.getTitle();
            Iterator<? extends InternationalString> iterator = null;
            do {
                if (candidate != null) {
                    final String unlocalized = candidate.toString(Locale.ROOT);
                    if (equalsFiltered(unlocalized, title)) {
                        return true;
                    }
                    final String localized = candidate.toString();
                    if (!Objects.equals(localized, unlocalized)             // Slight optimization for a common case.
                            && equalsFiltered(localized, title))
                    {
                        return true;
                    }
                }
                if (iterator == null) {
                    iterator = nonEmptyIterator(citation.getAlternateTitles());
                    if (iterator == null) break;
                }
                if (!iterator.hasNext()) break;
                candidate = iterator.next();
            } while (true);
        }
        return false;
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
     * @param  c1  the first citation to compare, or {@code null}.
     * @param  c2  the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one identifier matches.
     */
    public static boolean identifierMatches(Citation c1, final Citation c2) {
        if (c1 != null && c2 != null) {
            if (c1 == c2) {
                return true;                            // Optimisation for a common case.
            }
            /*
             * If both argument are one of the constants defined in the Citations class,
             * then we do not need to compare identifier; call to `equals` is sufficient.
             * This special case avoids the potentially costly call to `getIdentifiers()`
             * since that call may cause a connection to the spatial metadata database.
             */
            if (c1 instanceof CitationConstant && c2 instanceof CitationConstant) {
                return c1.equals(c2);
            }
            /*
             * If there is no identifier in both citations, fallback on title comparisons.
             * If there is identifiers in only one citation, make sure that this citation
             * is the second one (c2) in order to allow at least one call to
             * 'identifierMatches(c1, String)'.
             */
            Iterator<? extends Identifier> iterator = nonEmptyIterator(c2.getIdentifiers());
            if (iterator == null) {
                iterator = nonEmptyIterator(c1.getIdentifiers());
                if (iterator == null) {
                    return titleMatches(c1, c2);
                }
                c1 = c2;
            }
            do {
                final Identifier id = iterator.next();
                if (id != null && identifierMatches(c1, id, id.getCode())) {
                    return true;
                }
            } while (iterator.hasNext());
        }
        return false;
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
     * @param  citation    the citation to check for, or {@code null}.
     * @param  identifier  the identifier to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and an identifier matches the given string.
     */
    public static boolean identifierMatches(final Citation citation, final String identifier) {
        return identifierMatches(citation, null, identifier);
    }

    /**
     * Returns {@code true} if the given citation has at least one identifier equals to the given string,
     * ignoring case and non-alphanumeric characters. If and <em>only</em> if the citation does not contain
     * any identifier, then this method fallback on titles comparison.
     *
     * @param  citation    the citation to check for, or {@code null}.
     * @param  identifier  the identifier to compare, or {@code null} if unknown.
     * @param  code        value of {@code identifier.getCode()}, or {@code null}.
     * @return {@code true} if both arguments are non-null, and an identifier matches the given string.
     */
    static boolean identifierMatches(final Citation citation, final Identifier identifier, final String code) {
        if (citation != null && code != null) {
            final Collection<? extends Identifier> citIds = citation.getIdentifiers();
            Iterator<? extends Identifier> it = nonEmptyIterator(citIds);
            if (it == null) {
                return titleMatches(citation, code);
            }
            while (it.hasNext()) {
                final Identifier citId = it.next();
                if (citId != null && equalsFiltered(code, citId.getCode())) {
                    /*
                     * Found a possible match. We will take the code space in account only if it is defined
                     * by both identifiers. If a code space is undefined, we consider that we have a match.
                     */
                    if (identifier != null) {
                        final String codeSpace = identifier.getCodeSpace();
                        if (codeSpace != null) {
                            final String cs = citId.getCodeSpace();
                            if (cs != null && !equalsFiltered(codeSpace, cs)) {
                                continue;       // Check other identifiers.
                            }
                        }
                    }
                    return true;
                }
            }
            /*
             * Before to give up, maybe the given code argument is actually written using a "codeSpace:code" syntax.
             * Try to parse that syntax only if no Identifier argument were specified (otherwise we require the code
             * and code space to be splitted as defined in the identifier).
             */
            if (identifier == null) {
                int s = 0;
                final int length = code.length();
                while ((s = CharSequences.indexOf(code, Constants.DEFAULT_SEPARATOR, s, length)) >= 0) {
                    final CharSequence codeSpace = code.subSequence(0, s);
                    final CharSequence localPart = code.subSequence(++s, length);
                    for (it = citIds.iterator(); it.hasNext();) {
                        final Identifier id = it.next();
                        if (equalsFiltered(codeSpace, id.getCodeSpace()) && equalsFiltered(localPart, id.getCode())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method is useful for extracting a short designation of an authority (e.g. {@code "EPSG"})
     * for display purpose. This method performs the following choices:
     *
     * <ul class="verbose">
     *   <li>If the given citation is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the collection of {@linkplain DefaultCitation#getIdentifiers() citation identifiers}
     *       contains at least one non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated}
     *       identifier, then:
     *     <ul>
     *       <li>If the <var>codespace</var> (if any) and the <var>code</var> of at least one non-deprecated identifier
     *           are {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier valid Unicode identifiers}
     *           (with relaxed rules regarding the code), then the <strong>first</strong> of those identifiers
     *           is returned in a {@code "[codespace:]code"} format. If a <var>codespace</var> exists,
     *           then the above restriction about the <var>code</var> is relaxed in two ways:
     *         <ul>
     *           <li>The code is allowed to start with a
     *               Unicode identifier {@linkplain Character#isUnicodeIdentifierPart(int) part}
     *               (not necessarily {@linkplain Character#isUnicodeIdentifierStart(int) start})
     *               since the <var>codespace</var> already provides the start character.</li>
     *           <li>The code is allowed to contain some other characters (currently {@code '.'} and {@code '-'})
     *               commonly found in identifiers in the codespace managed by various authorities.</li>
     *         </ul>
     *       </li>
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
     * @param  citation  the citation for which to get the identifier, or {@code null}.
     * @return a non-empty identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not declare any identifier or title.
     */
    public static String getIdentifier(final Citation citation) {
        return Identifiers.getIdentifier(citation, false);
    }

    /**
     * Infers a code space from the given citation, or returns {@code null} if none.
     * This method is useful for extracting a short designation of an authority (e.g. {@code "EPSG"})
     * for processing purpose. This method performs the following actions:
     *
     * <ul class="verbose">
     *   <li>If the given citation is an instance of {@code IdentifierSpace},
     *       returns {@link IdentifierSpace#getName()}.</li>
     *   <li>Otherwise, performs the same work than {@link #getIdentifier(Citation)} except that {@code '_'}
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
     * @param  citation  the citation for which to infer the code space, or {@code null}.
     * @return a non-empty code space for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not have any Unicode identifier or title.
     *
     * @since 1.0
     */
    public static String toCodeSpace(final Citation citation) {
        if (citation instanceof IdentifierSpace<?>) {
            return ((IdentifierSpace<?>) citation).getName();
        } else {
            return removeIgnorableCharacters(Identifiers.getIdentifier(citation, true));
        }
    }

    /**
     * Removes characters that are ignorable according Unicode specification.
     *
     * @param  identifier  the character sequence from which to remove ignorable characters, or {@code null}.
     * @return a character sequence with ignorable character removed. May be the same instance than the given argument.
     */
    private static String removeIgnorableCharacters(final String identifier) {
        if (identifier != null) {
            /*
             * First perform a quick check to see if there is any ignorable characters.
             * We make this check because those characters are valid according Unicode
             * but not according XML. However there is usually no such characters, so
             * we will avoid the StringBuilder creation in the vast majority of times.
             *
             * Note that 'µ' and its friends are not ignorable, so we do not remove them.
             * This method is aimed for "getUnicodeIdentifier", not "getXmlIdentifier".
             */
            final int length = identifier.length();
            for (int i=0; i<length;) {
                int c = identifier.codePointAt(i);
                int n = Character.charCount(c);
                if (Character.isIdentifierIgnorable(c)) {
                    /*
                     * Found an ignorable character. Create the buffer and copy non-ignorable characters.
                     * Following algorithm is inefficient, since we fill the buffer character-by-character
                     * (a more efficient approach would be to perform bulk appends). However we presume
                     * that this block will be rarely executed, so it is not worth to optimize it.
                     */
                    final StringBuilder buffer = new StringBuilder(length - n).append(identifier, 0, i);
                    while ((i += n) < length) {
                        c = identifier.codePointAt(i);
                        n = Character.charCount(c);
                        if (!Character.isIdentifierIgnorable(c)) {
                            buffer.appendCodePoint(c);
                        }
                    }
                    /*
                     * No need to verify if the buffer is empty, because ignorable
                     * characters are not legal Unicode identifier start.
                     */
                    return buffer.toString();
                }
                i += n;
            }
        }
        return identifier;
    }
}
