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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.time.temporal.Temporal;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.system.Modules;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.ScopedIdentifier;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.referencing.internal.shared.DefinitionVerifier;
import org.apache.sis.referencing.cs.AxisFilter;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.datum.DynamicReferenceFrame;
import org.opengis.coordinate.CoordinateMetadata;


/**
 * Static methods working on {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}.
 * The methods defined in this class can be grouped in three categories:
 *
 * <ul>
 *   <li>Factory methods, the most notable one being {@link #forCode(String)}.</li>
 *   <li>Methods providing information, like {@link #isHorizontalCRS(CoordinateReferenceSystem)}.</li>
 *   <li>Finding coordinate operations between a source and a target CRS.</li>
 * </ul>
 *
 * <h2>Usage example</h2>
 * The most frequently used methods in this class are {@link #forCode forCode(…)}, {@link #fromWKT fromWKT(…)}
 * and {@link #findOperation findOperation(…)}. An usage example is like below
 * (see the <a href="https://sis.apache.org/tables/CoordinateReferenceSystems.html">Apache SIS™ Coordinate
 * Reference System (CRS) codes</a> page for the complete list of EPSG codes):
 *
 * {@snippet lang="java" :
 *     CoordinateReferenceSystem source = CRS.forCode("EPSG:4326");                   // WGS 84
 *     CoordinateReferenceSystem target = CRS.forCode("EPSG:3395");                   // WGS 84 / World Mercator
 *     CoordinateOperation operation = CRS.findOperation(source, target, null);
 *     if (CRS.getLinearAccuracy(operation) > 100) {
 *         // If the accuracy is coarser than 100 metres (or any other threshold at application choice)
 *         // maybe the operation is not suitable. Decide here what to do (throw an exception, etc).
 *     }
 *     MathTransform mt = operation.getMathTransform();
 *     DirectPosition position = new DirectPosition2D(20, 30);            // 20°N 30°E   (watch out axis order!)
 *     position = mt.transform(position, position);
 *     System.out.println(position);
 *     }
 *
 * <h2>Note on kinds of CRS</h2>
 * The {@link #getSingleComponents(CoordinateReferenceSystem)} method decomposes an arbitrary CRS into a flat
 * list of single components. In such flat list, vertical and temporal components can easily be identified by
 * {@code instanceof} checks. But identifying the horizontal component is not as easy. The list below suggests
 * ways to classify the components:
 *
 * <ul>
 *   <li><code>if (crs instanceof TemporalCRS)</code> determines if the CRS is for the temporal component.</li>
 *   <li><code>if (crs instanceof VerticalCRS)</code> determines if the CRS is for the vertical component.</li>
 *   <li><code>if (CRS.{@linkplain #isHorizontalCRS(CoordinateReferenceSystem) isHorizontalCRS}(crs))</code>
 *       determines if the CRS is for the horizontal component.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.6
 * @since   0.3
 */
public final class CRS {
    /**
     * The logger for referencing operations.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.REFERENCING);

    /**
     * Do not allow instantiation of this class.
     */
    private CRS() {
    }

    /**
     * Returns the Coordinate Reference System for the given authority code.
     * There is <a href="https://sis.apache.org/tables/CoordinateReferenceSystems.html">many thousands
     * of reference systems</a> defined by the EPSG authority or by other authorities.
     * Whether those codes are recognized or not depends on whether the
     * <a href="https://sis.apache.org/epsg.html">EPSG dataset is installed</a>.
     * The following table lists a small subset of codes which are guaranteed to be available
     * on any installation of Apache SIS:
     *
     * <blockquote><table class="sis">
     *   <caption>Minimal set of supported authority codes</caption>
     *   <tr><th>Code</th>          <th>Enum</th>                            <th>CRS Type</th>        <th>Description</th></tr>
     *   <tr><td>CRS:27</td>        <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>      <td>Like EPSG:4267 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:83</td>        <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>      <td>Like EPSG:4269 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:84</td>        <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>      <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:88</td>        <td>{@link CommonCRS.Vertical#NAVD88 NAVD88}</td><td>Vertical</td><td>North American Vertical Datum 1988 height</td></tr>
     *   <tr><td>EPSG:3395</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / World Mercator</td></tr>
     *   <tr><td>EPSG:3857</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / Pseudo-Mercator</td></tr>
     *   <tr><td>EPSG:4230</td>     <td>{@link CommonCRS#ED50   ED50}</td>   <td>Geographic</td>      <td>European Datum 1950</td></tr>
     *   <tr><td>EPSG:4258</td>     <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic</td>      <td>European Terrestrial Reference System 1989</td></tr>
     *   <tr><td>EPSG:4267</td>     <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>      <td>North American Datum 1927</td></tr>
     *   <tr><td>EPSG:4269</td>     <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>      <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:4322</td>     <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic</td>      <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4326</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>      <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4936</td>     <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geocentric</td>      <td>European Terrestrial Reference System 1989</td></tr>
     *   <tr><td>EPSG:4937</td>     <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic 3D</td>   <td>European Terrestrial Reference System 1989</td></tr>
     *   <tr><td>EPSG:4978</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geocentric</td>      <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4979</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic 3D</td>   <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4984</td>     <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geocentric</td>      <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4985</td>     <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic 3D</td>   <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:5041</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / UPS North (E,N)</td></tr>
     *   <tr><td>EPSG:5042</td>     <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / UPS South (E,N)</td></tr>
     *   <tr><td>EPSG:322##</td>    <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Projected</td>       <td>WGS 72 / UTM zone ##N</td></tr>
     *   <tr><td>EPSG:323##</td>    <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Projected</td>       <td>WGS 72 / UTM zone ##S</td></tr>
     *   <tr><td>EPSG:326##</td>    <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / UTM zone ##N</td></tr>
     *   <tr><td>EPSG:327##</td>    <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Projected</td>       <td>WGS 84 / UTM zone ##S</td></tr>
     *   <tr><td>EPSG:5714</td>     <td>{@link CommonCRS.Vertical#MEAN_SEA_LEVEL MEAN_SEA_LEVEL}</td> <td>Vertical</td> <td>Mean Sea Level height</td></tr>
     *   <tr><td>EPSG:5715</td>     <td>{@link CommonCRS.Vertical#DEPTH  DEPTH}</td>  <td>Vertical</td> <td>Mean Sea Level depth</td></tr>
     *   <tr><td>OGC:JulianDate</td><td>{@link CommonCRS.Temporal#JULIAN JULIAN}</td> <td>Temporal</td> <td>Julian date (days)</td></tr>
     *   <tr><td>OGC:TruncatedJulianDate</td><td>{@link CommonCRS.Temporal#TRUNCATED_JULIAN TRUNCATED_JULIAN}</td> <td>Temporal</td> <td>Truncated Julian date (days)</td></tr>
     *   <tr><td>OGC:UnixTime</td>  <td>{@link CommonCRS.Temporal#UNIX   UNIX}</td>   <td>Unix</td>     <td>Unix time (seconds)</td></tr>
     * </table></blockquote>
     *
     * For codes in above table, the EPSG geodetic database is used when available,
     * otherwise Apache SIS fallbacks on definitions from public sources with no EPSG metadata except the identifiers.
     * If the EPSG geodetic dataset has been used, the {@linkplain NamedIdentifier#getAuthority() authority} title
     * will be something like <q>EPSG geodetic dataset</q>, otherwise it will be <q>Subset of EPSG</q>.
     *
     * <h4>Extended set of codes</h4>
     * If this method is invoked during the parsing of a GML document,
     * then the set of known codes temporarily includes the {@code <gml:identifier>} values
     * of all {@link CoordinateReferenceSystem} definitions which have been parsed before this method call.
     * Those codes are local to the document being parsed (many documents can be parsed concurrently without conflict),
     * and are discarded after the parsing is completed (e.g., on {@link org.apache.sis.xml.XML#unmarshal(String)} return).
     * This feature allows embedded or linked data to references a CRS definition
     * in the same file or a file included by an {@code xlink:href} attribute.
     *
     * <h4>URI forms</h4>
     * This method accepts also the URN and URL syntaxes.
     * For example, the following codes are considered equivalent to {@code "EPSG:4326"}:
     * <ul>
     *   <li>{@code "EPSG::4326"}</li>
     *   <li>{@code "urn:ogc:def:crs:EPSG::4326"}</li>
     *   <li>{@code "http://www.opengis.net/def/crs/epsg/0/4326"}</li>
     *   <li>{@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}</li>
     * </ul>
     *
     * URIs can be combined for creating larger objects. For example, the following URIs combine a
     * two-dimensional WGS84 reference system (EPSG:4326) with a Mean Sea Level height (EPSG:5714).
     * The result is a three-dimensional {@linkplain org.apache.sis.referencing.crs.DefaultCompoundCRS
     * compound coordinate reference system}:
     *
     * <ul>
     *   <li>{@code "urn:ogc:def:crs,crs:EPSG::4326,crs:EPSG::5714"}</li>
     *   <li><code>"http://www.opengis.net/def/crs-compound?<br>
     *            1=http://www.opengis.net/def/crs/epsg/0/4326&amp;<br>
     *            2=http://www.opengis.net/def/crs/epsg/0/5714"</code></li>
     * </ul>
     *
     * <p>URNs (but not URLs) can also combine a
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic reference frame} with an
     * {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal coordinate system} for creating a new
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}, or a base geographic CRS with a
     * {@linkplain org.apache.sis.referencing.operation.DefaultConversion conversion} and a
     * {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian coordinate system} for creating a new
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected coordinate reference system}.</p>
     *
     * <h4>Reverse operation</h4>
     * The {@link IdentifiedObjects#lookupURN(IdentifiedObject, Citation)} method can be seen
     * as a converse of this method: from a CRS object, it tries to find a URN that describes it.
     * More codes may also be supported depending on which extension modules are available.
     *
     * @param  code  the authority code.
     * @return the Coordinate Reference System for the given authority code.
     * @throws NoSuchAuthorityCodeException if there is no known CRS associated to the given code.
     * @throws FactoryException if the CRS creation failed for another reason.
     *
     * @see #getAuthorityFactory(String)
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory
     * @see <a href="https://epsg.org/">EPSG Geodetic Registry</a>
     *
     * @category factory
     */
    public static CoordinateReferenceSystem forCode(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        try {
            /*
             * Gives precedence to the database for consistency reasons.
             * The GML definitions are checked only in last resort.
             */
            try {
                return AuthorityFactories.ALL.createCoordinateReferenceSystem(code);
            } catch (UnavailableFactoryException e) {
                return AuthorityFactories.fallback(e).createCoordinateReferenceSystem(code);
            }
        } catch (NoSuchAuthorityCodeException e) {
            final Context context = Context.current();
            if (context != null) {
                var crs = new ScopedIdentifier<>(CoordinateReferenceSystem.class, code).get(context);
                if (crs != null) {
                    return crs;
                }
            }
            throw e;
        }
    }

    /**
     * Creates a Coordinate Reference System object from a <i>Well Known Text</i> (WKT).
     * The default {@linkplain org.apache.sis.io.wkt Apache SIS parser} understands both
     * version 1 (a.k.a. OGC 01-009) and version 2 (a.k.a. ISO 19162) of the WKT format.
     *
     * <h4>Example</h4>
     * Below is a slightly simplified WKT 2 string for a Mercator projection.
     * For making this example smaller, some optional {@code UNIT[…]} and {@code ORDER[…]} elements have been omitted.
     *
     * {@snippet lang="wkt" :
     *   ProjectedCRS["SIRGAS 2000 / Brazil Mercator",
     *     BaseGeogCRS["SIRGAS 2000",
     *       Datum["Sistema de Referencia Geocentrico para las Americas 2000",
     *         Ellipsoid["GRS 1980", 6378137, 298.257222101]]],
     *     Conversion["Petrobras Mercator",
     *       Method["Mercator (variant B)", Id["EPSG",9805]],
     *       Parameter["Latitude of 1st standard parallel", -2],
     *       Parameter["Longitude of natural origin", -43],
     *       Parameter["False easting", 5000000],
     *       Parameter["False northing", 10000000]],
     *     CS[cartesian,2],
     *       Axis["easting (E)", east],
     *       Axis["northing (N)", north],
     *       LengthUnit["metre", 1],
     *     Id["EPSG",5641]]
     *   }
     *
     * <h4>Logging</h4>
     * If the parsing produced warnings, they will be reported in a logger named {@code "org.apache.sis.io.wkt"}.
     * In particular, this method verifies if the description provided by the WKT matches the description provided
     * by the authority ({@code "EPSG:5641"} in the above example) and reports discrepancies.
     * Note that this comparison between parsed CRS and authoritative CRS is specific to this convenience method;
     * other APIs documented in <i>see also</i> section do not perform this comparison automatically.
     * Should the WKT description and the authoritative description be in conflict, the WKT description prevails
     * as mandated by ISO 19162 standard (see {@link #fromAuthority fromAuthority(…)} if a different behavior is needed).
     *
     * <h4>Usage and performance considerations</h4>
     * This convenience method delegates to {@link GeodeticObjectFactory#createFromWKT(String)}
     * using a default factory instance. This is okay for occasional use, but has the following limitations:
     *
     * <ul>
     *   <li>Performance may be sub-optimal in a multi-thread environment.</li>
     *   <li>No control on the WKT {@linkplain org.apache.sis.io.wkt.Convention conventions} in use.</li>
     *   <li>No control on the handling of {@linkplain org.apache.sis.io.wkt.Warnings warnings}.</li>
     * </ul>
     *
     * Applications which need to parse a large number of WKT strings should consider to use
     * the {@link org.apache.sis.io.wkt.WKTFormat} class instead of this method.
     *
     * @param  wkt  coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @return the parsed Coordinate Reference System.
     * @throws FactoryException if the given WKT cannot be parsed.
     *
     * @see org.apache.sis.io.wkt.WKTFormat
     * @see GeodeticObjectFactory#createFromWKT(String)
     * @see org.apache.sis.geometry.Envelopes#fromWKT(CharSequence)
     * @see <a href="https://www.ogc.org/standards/wkt-crs/">WKT 2 specification</a>
     *
     * @since 0.6
     */
    public static CoordinateReferenceSystem fromWKT(final String wkt) throws FactoryException {
        final CoordinateReferenceSystem crs = GeodeticObjectFactory.provider().createFromWKT(wkt);
        DefinitionVerifier.withAuthority(crs, Loggers.WKT, CRS.class, "fromWKT");
        return crs;
    }

    /**
     * Creates a coordinate reference system object from a XML string.
     * Note that the given argument is the XML document itself, <strong>not</strong> a URL to a XML document.
     * For reading XML documents from readers or input streams,
     * see static methods in the {@link org.apache.sis.xml.XML} class.
     *
     * <p>If the unmarshalling produced warnings, they will be reported in a logger named {@code "org.apache.sis.xml"}.
     * In particular, this method verifies if the description provided by the XML matches the description provided by
     * the authority code given in {@code <gml:identifier>} element, and reports discrepancies.
     * Note that this comparison between unmarshalled CRS and authoritative CRS is specific to this convenience method;
     * other APIs documented in <i>see also</i> section do not perform this comparison automatically.
     * Should the XML description and the authoritative description be in conflict, the XML description prevails
     * (see {@link #fromAuthority fromAuthority(…)} if a different behavior is needed).</p>
     *
     * @param  xml  coordinate reference system encoded in XML format.
     * @return the unmarshalled Coordinate Reference System.
     * @throws FactoryException if the object creation failed.
     *
     * @see GeodeticObjectFactory#createFromXML(String)
     * @see org.apache.sis.xml.XML#unmarshal(String)
     *
     * @since 0.7
     */
    public static CoordinateReferenceSystem fromXML(final String xml) throws FactoryException {
        ArgumentChecks.ensureNonNull("text", xml);
        final CoordinateReferenceSystem crs = GeodeticObjectFactory.provider().createFromXML(xml);
        DefinitionVerifier.withAuthority(crs, Loggers.XML, CRS.class, "fromXML");
        return crs;
    }

    /**
     * Replaces the given coordinate reference system by an authoritative description, if one can be found.
     * This method can be invoked after constructing a CRS in a context where the EPSG (or other authority)
     * code is suspected more reliable than the rest of the description. A common case is a <i>Well Known
     * Text</i> (WKT) string declaring wrong projection method or parameter values for the EPSG code that
     * it pretends to describe. For example:
     *
     * <blockquote>
     *   {@code PROJCS["WGS 84 / Pseudo-Mercator",}<br>
     *   {@code   }(…base CRS omitted for brevity…)<br>
     *   {@code   PROJECTION["Mercator (variant A)"],} — <em><b>wrong:</b> shall be "Popular Visualisation Pseudo Mercator"</em><br>
     *   {@code   }(…parameters and axes omitted for brevity…)<br>
     *   {@code   AUTHORITY["EPSG", "3857"]]}
     * </blockquote>
     *
     * In such cases, Apache SIS behavior in {@link #fromWKT(String)}, {@link #fromXML(String)} and other methods
     * is conform to the <a href="https://www.ogc.org/standards/wkt-crs/"><abbr>WKT</abbr> 2 specification</a>:
     *
     * <blockquote><q>Should any attributes or values given in the cited identifier be in conflict with attributes
     * or values given explicitly in the WKT description, the WKT values shall prevail.</q></blockquote>
     *
     * In situations where the opposite behavior is desired (i.e. to make the authority identifier prevails),
     * this method can be invoked. This method performs the following actions:
     *
     * <ul>
     *   <li>If the given CRS has an {@linkplain AbstractIdentifiedObject#getIdentifiers() identifier} and if the authority factory can
     *     {@linkplain org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createCoordinateReferenceSystem(String) create a CRS}
     *     for that identifier, then:
     *     <ul>
     *       <li>If the CRS defined by the authority is {@linkplain #equivalent equivalent} to the given CRS,
     *         then this method returns silently the <em>authoritative</em> CRS.</li>
     *       <li>Otherwise if the CRS defined by the authority is equal, ignoring axis order and units, to the given CRS,
     *         then this method returns a <em>new</em> CRS derived from the authoritative one but with same
     *         {@linkplain org.apache.sis.referencing.cs.AxesConvention axes convention} than the given CRS.
     *         A warning is emitted.</li>
     *       <li>Otherwise this method discards the given CRS and returns the <em>authoritative</em> CRS.
     *         A warning is emitted with a message indicating where a difference has been found.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise if the given CRS does not have identifier, then this method
     *       {@linkplain org.apache.sis.referencing.factory.IdentifiedObjectFinder searches for an equivalent CRS}
     *       defined by the authority factory. If such CRS is found, then:
     *     <ul>
     *       <li>If the CRS defined by the authority is {@linkplain #equivalent equivalent} to the given CRS,
     *         then this method returns silently the <em>authoritative</em> CRS.</li>
     *       <li>Otherwise if the CRS defined by the authority is equal, ignoring axis order and units, to the given CRS,
     *         then this method returns silently a <em>new</em> CRS derived from the authoritative one but with same
     *         {@linkplain org.apache.sis.referencing.cs.AxesConvention axes convention} than the given CRS.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise this method silently returns the given CRS as-is.</li>
     * </ul>
     *
     * <h4>Avoiding warning redundancies</h4>
     * The warnings logged by this method are redundant with warnings logged by other methods in this class,
     * in particular {@link #fromWKT(String)} and {@link #fromXML(String)} methods. For avoiding this annoyance,
     * a {@code null} value for the {@code warningFilter} argument means to shut off those redundant loggings.
     * A non-null {@code warningFilter} argument is more useful for CRS parsed by methods outside this class,
     * for example {@link org.apache.sis.io.wkt.WKTFormat} or {@link org.apache.sis.xml.XML#unmarshal(String)}.
     *
     * @param  crs            the CRS to replace by an authoritative CRS, or {@code null}.
     * @param  factory        the factory where to search for authoritative definitions, or {@code null} for the default.
     * @param  warningFilter  whether to log warnings, or {@code null} for the default behavior (which is to filter out
     *                        the warnings that are redundant with warnings emitted by other methods in this class).
     * @return the suggested CRS to use (may be the {@code crs} argument itself), or {@code null} if the given CRS was null.
     * @throws FactoryException if an error occurred while querying the authority factory.
     *
     * @since 1.0
     */
    public static CoordinateReferenceSystem fromAuthority(CoordinateReferenceSystem crs,
            final CRSAuthorityFactory factory, final Filter warningFilter) throws FactoryException
    {
        if (crs != null) {
            final DefinitionVerifier verification = DefinitionVerifier.withAuthority(crs, factory, true, null);
            if (verification != null) {
                crs = verification.recommendation;
                if (warningFilter != null) {
                    final LogRecord record = verification.warning(false);
                    if (record != null) {
                        record.setLoggerName(Modules.REFERENCING);
                        record.setSourceClassName(CRS.class.getName());
                        record.setSourceMethodName("fromAuthority");
                        if (warningFilter.isLoggable(record)) {
                            LOGGER.log(record);
                        }
                    }
                }
            }
        }
        return crs;
    }

    /**
     * Returns whether the given Coordinate Reference Systems can be considered as equivalent.
     * Two <abbr>CRS</abbr>s are considered equivalent if they may have some differences in metadata or data structure,
     * but replacing one <abbr>CRS</abbr> by the other should not change the input and output coordinate values in operations.
     *
     * <h4>Implementation note</h4>
     * This is a convenience method for the following method call:
     *
     * {@snippet lang="java" :
     *     return deepEquals(object1, object2, ComparisonMode.IGNORE_METADATA);
     *     }
     *
     * @param  object1  the first object to compare (may be null).
     * @param  object2  the second object to compare (may be null).
     * @return {@code true} if both objects are equivalent.
     *
     * @see ComparisonMode#COMPATIBILITY
     * @see Utilities#equalsIgnoreMetadata(Object, Object)
     *
     * @since 1.5
     */
    public static boolean equivalent(final CoordinateReferenceSystem object1, final CoordinateReferenceSystem object2) {
        return Utilities.deepEquals(object1, object2, ComparisonMode.COMPATIBILITY);
    }

    /**
     * Suggests a coordinate reference system which could be a common target for coordinate operations having the
     * given sources. This method compares the {@linkplain #getGeographicBoundingBox(CoordinateReferenceSystem)
     * domain of validity} of all given CRSs. If a CRS has a domain of validity that contains the domain of all other
     * CRS, then that CRS is returned. Otherwise this method verifies if a {@linkplain DerivedCRS#getBaseCRS()
     * base CRS} (usually a {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS} instance)
     * would be suitable. If no suitable CRS is found, then this method returns {@code null}.
     *
     * <h4>Use case</h4>
     * Before to test if two arbitrary envelopes {@linkplain GeneralEnvelope#intersects(Envelope) intersect} each other,
     * they need to be {@linkplain Envelopes#transform(Envelope, CoordinateReferenceSystem) transformed} in the same CRS.
     * However if one CRS is a Transverse Mercator projection while the other CRS is a world-wide geographic CRS, then
     * attempts to use the Transverse Mercator projection as the common CRS is likely to fail since the geographic envelope
     * may span an area far outside the projection domain of validity. This {@code suggestCommonTarget(…)} method can used
     * for choosing a common CRS which is less likely to fail.
     *
     * @param  regionOfInterest  the geographic area for which the coordinate operations will be applied,
     *                           or {@code null} if unknown. Will be intersected with CRS domains of validity.
     * @param  sourceCRS         the coordinate reference systems for which a common target CRS is desired.
     *                           May contain {@code null} elements, which are ignored.
     * @return a CRS that may be used as a common target for all the given source CRS in the given region of interest,
     *         or {@code null} if this method did not find a common target CRS. The returned CRS may be different than
     *         all given CRS.
     *
     * @since 0.8
     */
    @OptionalCandidate
    public static CoordinateReferenceSystem suggestCommonTarget(GeographicBoundingBox regionOfInterest,
                                                                CoordinateReferenceSystem... sourceCRS)
    {
        CoordinateReferenceSystem bestCRS = null;
        /*
         * Compute the union of the domain of validity of all CRS. If a CRS does not specify a domain of validity,
         * then assume that the CRS is valid for the whole world if the CRS is geodetic (otherwise ignore that CRS).
         * Opportunistically remember the domain of validity of each CRS in this loop since we will need them later.
         */
        boolean worldwide = false;
        DefaultGeographicBoundingBox domain = null;
        final var domains = new GeographicBoundingBox[sourceCRS.length];
        for (int i=0; i < sourceCRS.length; i++) {
            final CoordinateReferenceSystem crs = sourceCRS[i];
            final GeographicBoundingBox bbox = getGeographicBoundingBox(crs);
            if (bbox != null) {
                domains[i] = bbox;
                if (!worldwide) {
                    if (domain == null) {
                        domain = new DefaultGeographicBoundingBox(bbox);
                    } else {
                        domain.add(bbox);
                    }
                }
            } else if (crs instanceof GeodeticCRS) {
                /*
                 * Geodetic CRS (geographic or geocentric) can generally be presumed valid in a worldwide area.
                 * The 'worldwide' flag is a little optimization for remembering that we do not need to compute
                 * the union anymore, but we still need to continue the loop for fetching all bounding boxes.
                 */
                bestCRS = crs;                      // Fallback to be used if we don't find anything better.
                worldwide = true;
            }
        }
        /*
         * At this point we got the union of the domain of validity of all CRS. We are interested only in the
         * part that intersect the region of interest. If the union is whole world, we do not need to compute
         * the intersection; we can just leave the region of interest unchanged.
         */
        if (domain != null && !worldwide) {
            if (regionOfInterest != null) {
                domain.intersect(regionOfInterest);
            }
            regionOfInterest = domain;
            domain = null;
        }
        /*
         * Iterate again over the domain of validity of all CRS.  For each domain of validity, compute the area
         * which is inside the domain or interest and the area which is outside. The "best CRS" will be the one
         * which comply with the following rules, in preference order:
         *
         *   1) The CRS which is valid over the largest area of the region of interest.
         *   2) If two CRS are equally good according rule 1, then the CRS with the smallest "outside area".
         *
         * Example: given two source CRS, a geographic one and a projected one:
         *
         *   - If the projected CRS contains fully the region of interest, then it will be returned.
         *     The preference is given to the projected CRS because geometric operations are likely
         *     to be more accurate in that space. Furthermore, forward conversions from geographic to
         *     projected CRS are usually faster than inverse conversions.
         *
         *   - Otherwise (i.e. if the region of interest is likely to be wider than the projected CRS
         *     domain of validity), then the geographic CRS will be returned.
         */
        final double roiArea  = Extents.area(regionOfInterest);   // NaN if `regionOfInterest` is null.
        double maxInsideArea  = 0;
        double minOutsideArea = Double.POSITIVE_INFINITY;
        boolean tryDerivedCRS = false;
        do {
            for (int i=0; i < domains.length; i++) {
                final GeographicBoundingBox bbox = domains[i];
                if (bbox != null) {
                    double insideArea  = Extents.area(bbox);
                    double outsideArea = 0;
                    if (regionOfInterest != null) {
                        if (domain == null) {
                            domain = new DefaultGeographicBoundingBox(bbox);
                        } else {
                            domain.setBounds(bbox);
                        }
                        domain.intersect(regionOfInterest);
                        final double area = insideArea;
                        insideArea = Extents.area(domain);
                        outsideArea = area - insideArea;
                    }
                    if (insideArea > maxInsideArea || (insideArea == maxInsideArea && outsideArea < minOutsideArea)) {
                        maxInsideArea  = insideArea;
                        minOutsideArea = outsideArea;
                        bestCRS        = sourceCRS[i];
                    }
                }
            }
            /*
             * If the best CRS does not cover fully the region of interest, then we will redo the check again
             * but using base CRS instead. For example if the list of source CRS had some projected CRS, we
             * will try with the geographic CRS on which those projected CRS are based.
             */
            if (Double.isNaN(roiArea) || maxInsideArea < roiArea) {
                if (tryDerivedCRS) break;                                               // Do not try twice.
                final SingleCRS[] derivedCRS = new SingleCRS[sourceCRS.length];
                for (int i=0; i < derivedCRS.length; i++) {
                    GeographicBoundingBox bbox = null;
                    final CoordinateReferenceSystem crs = sourceCRS[i];
                    if (crs instanceof DerivedCRS) {
                        final SingleCRS baseCRS = ((DerivedCRS) crs).getBaseCRS();
                        bbox = getGeographicBoundingBox(baseCRS);
                        if (bbox == null && bestCRS == null && baseCRS instanceof GeodeticCRS) {
                            bestCRS = baseCRS;      // Fallback to be used if we don't find anything better.
                        }
                        tryDerivedCRS = true;
                        derivedCRS[i] = baseCRS;
                    }
                    domains[i] = bbox;
                }
                sourceCRS = derivedCRS;
            } else {
                break;
            }
        } while (tryDerivedCRS);
        return bestCRS;
    }

    /**
     * Finds a mathematical operation that transforms or converts coordinates between the given <abbr>CRS</abbr>s and epochs.
     * This method performs the same work as the {@linkplain #findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem,
     * GeographicBoundingBox) variant working on CRS objects}, except that the coordinate epochs may be taken in account.
     *
     * @param  source          the CRS and epoch of source coordinates.
     * @param  target          the CRS and epoch of target coordinates.
     * @param  areaOfInterest  the area of interest, or {@code null} if none.
     * @return the mathematical operation from {@code source} to {@code target}.
     * @throws OperationNotFoundException if no operation was found between the given pair of <abbr>CRS</abbr>s and epochs.
     * @throws FactoryException if the operation cannot be created for another reason.
     *
     * @since 1.5
     */
    public static CoordinateOperation findOperation(final CoordinateMetadata source,
                                                    final CoordinateMetadata target,
                                                    final GeographicBoundingBox areaOfInterest)
            throws FactoryException
    {
        // TODO: take epoch in account.
        if (source.getCoordinateEpoch().isPresent() || target.getCoordinateEpoch().isPresent()) {
            throw new FactoryException("This version of Apache SIS does not yet support coordinate epoch.");
        }
        return findOperation(source.getCoordinateReferenceSystem(), target.getCoordinateReferenceSystem(), areaOfInterest);
    }

    /**
     * Finds a mathematical operation that transforms or converts coordinates from the given source to the
     * given target coordinate reference system. If an estimation of the geographic area containing the points
     * to transform is known, it can be specified for helping this method to find a better suited operation.
     * If no area of interest is specified, then the current default is the widest
     * {@linkplain DefaultObjectDomain#getDomainOfValidity() domain of validity}.
     * A future Apache SIS version may also take the country of current locale in account.
     *
     * <div class="note"><b>Note:</b>
     * the area of interest is just one aspect that may affect the coordinate operation.
     * Other aspects are the time of interest (because some coordinate operations take in account the
     * plate tectonics movement) or the desired accuracy. For more control on the coordinate operation
     * to create, see {@link CoordinateOperationContext}.</div>
     *
     * After the caller received a {@code CoordinateOperation} instance, the following methods can be invoked
     * for checking if the operation suits the caller's needs:
     *
     * <ul>
     *   <li>{@link #getGeographicBoundingBox(CoordinateOperation)}
     *       for checking if the operation is valid in the caller's area of interest.</li>
     *   <li>{@link #getLinearAccuracy(CoordinateOperation)}
     *       for checking if the operation has sufficient accuracy for caller's purpose.</li>
     * </ul>
     *
     * If the source and target CRS are equivalent, then this method returns an operation backed by an
     * {@linkplain org.opengis.referencing.operation.MathTransform#isIdentity() identity} transform.
     * If there is no known operation between the given pair of CRS, then this method throws an
     * {@link OperationNotFoundException}.
     *
     * <p>Note that <code>CRS.findOperation(<var>B</var>, <var>A</var>, <var>aoi</var>)</code> is not necessarily
     * the exact converse of <code>CRS.findOperation(<var>A</var>, <var>B</var>, <var>aoi</var>)</code>.
     * Some deviations may exist for example because of different paths explored in the geodetic database.
     * For the inverse of an existing {@link CoordinateOperation}, using
     * {@link org.opengis.referencing.operation.MathTransform#inverse()} is preferable.</p>
     *
     * @param  sourceCRS       the CRS of source coordinates.
     * @param  targetCRS       the CRS of target coordinates.
     * @param  areaOfInterest  the area of interest, or {@code null} if none.
     * @return the mathematical operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation was found between the given pair of <abbr>CRS</abbr>s.
     * @throws FactoryException if the operation cannot be created for another reason.
     *
     * @see Envelopes#findOperation(Envelope, Envelope)
     * @see DefaultCoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)
     *
     * @since 0.7
     */
    public static CoordinateOperation findOperation(final CoordinateReferenceSystem sourceCRS,
                                                    final CoordinateReferenceSystem targetCRS,
                                                    final GeographicBoundingBox areaOfInterest)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        final CoordinateOperationContext context = CoordinateOperationContext.fromBoundingBox(areaOfInterest);
        /*
         * In principle following code should just delegate to factory.createOperation(…). However, that operation
         * may fail if a connection to the EPSG database has been found, but the EPSG tables do not yet exist in
         * that database and we do not have the SQL scripts for creating them.
         */
        final DefaultCoordinateOperationFactory factory = DefaultCoordinateOperationFactory.provider();
        try {
            return factory.createOperation(sourceCRS, targetCRS, context);
        } catch (UnavailableFactoryException e) {
            if (AuthorityFactories.isUnavailable(e)) {
                throw e;
            } else try {
                // Above method call replaced the EPSG factory by a fallback. Try again.
                return factory.createOperation(sourceCRS, targetCRS, context);
            } catch (FactoryException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
    }

    /**
     * Finds mathematical operations that transform or convert coordinates from the given source to the
     * given target coordinate reference system. If at least one operation exists, they are returned in
     * preference order: the operation having the widest intersection between its
     * {@linkplain DefaultObjectDomain#getDomainOfValidity() domain of validity}
     * and the given area of interest are returned first.
     *
     * @param  sourceCRS       the CRS of source coordinates.
     * @param  targetCRS       the CRS of target coordinates.
     * @param  areaOfInterest  the area of interest, or {@code null} if none.
     * @return mathematical operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation was found between the given pair of CRS.
     * @throws FactoryException if the operation cannot be created for another reason.
     *
     * @see DefaultCoordinateOperationFactory#createOperations(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)
     *
     * @since 1.0
     */
    public static List<CoordinateOperation> findOperations(final CoordinateReferenceSystem sourceCRS,
                                                           final CoordinateReferenceSystem targetCRS,
                                                           final GeographicBoundingBox areaOfInterest)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        final CoordinateOperationContext context = CoordinateOperationContext.fromBoundingBox(areaOfInterest);
        final DefaultCoordinateOperationFactory factory = DefaultCoordinateOperationFactory.provider();
        try {
            return factory.createOperations(sourceCRS, targetCRS, context);
        } catch (UnavailableFactoryException e) {
            if (AuthorityFactories.isUnavailable(e)) {
                throw e;
            } else try {
                // Above method call replaced the EPSG factory by a fallback. Try again.
                return List.of(factory.createOperation(sourceCRS, targetCRS, context));
            } catch (FactoryException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
    }

    /**
     * Returns a positional accuracy estimation in metres for the given operation, or {@code NaN} if unknown.
     * This method applies the following heuristics:
     *
     * <ul>
     *   <li>If the given operation is an instance of {@link AbstractCoordinateOperation}, then delegate to the
     *       operation {@link AbstractCoordinateOperation#getLinearAccuracy() getLinearAccuracy()} method.</li>
     *
     *   <li>Otherwise if at least one {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult
     *       quantitative result} is found with a linear unit, then return the largest value converted to metres.</li>
     *
     *   <li>Otherwise if the operation is a {@linkplain org.apache.sis.referencing.operation.DefaultConversion
     *       conversion}, then returns 0 since a conversion is by definition accurate up to rounding errors.</li>
     *
     *   <li>Otherwise if the operation is a {@linkplain org.apache.sis.referencing.operation.DefaultTransformation
     *       transformation}, then the returned value depends on whether the datum shift were applied with the help
     *       of Bursa-Wolf parameters of not.</li>
     * </ul>
     *
     * See {@link AbstractCoordinateOperation#getLinearAccuracy()} for more details on the above heuristic rules.
     *
     * @param  operation  the coordinate operation for which to get the accuracy estimation, or {@code null}.
     * @return the accuracy estimation (always in meters), or NaN if unknown.
     *
     * @see #findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     * @see DatumOrEnsemble#getAccuracy(IdentifiedObject)
     *
     * @since 0.7
     */
    @OptionalCandidate
    public static double getLinearAccuracy(final CoordinateOperation operation) {
        if (operation == null) {
            return Double.NaN;
        } else if (operation instanceof AbstractCoordinateOperation) {
            return ((AbstractCoordinateOperation) operation).getLinearAccuracy();
        } else {
            return PositionalAccuracyConstant.getLinearAccuracy(operation);
        }
    }

    /**
     * Returns the valid geographic area for the given coordinate operation, or {@code null} if unknown.
     * This method explores the {@linkplain DefaultObjectDomain#getDomainOfValidity() domain of validity}
     * associated with the given operation. If more than one geographic bounding box is found, then this method
     * computes their {@linkplain DefaultGeographicBoundingBox#add(GeographicBoundingBox) union}.
     *
     * <p><b>Fallback:</b> if the given operation does not declare explicitly a domain of validity, then this
     * method computes the intersection of the domain of validity declared by source and target CRS. If no CRS
     * declare a domain of validity, then this method returns {@code null}.</p>
     *
     * @param  operation  the coordinate operation for which to get the domain of validity, or {@code null}.
     * @return the geographic area where the operation is valid, or {@code null} if unspecified.
     *
     * @see #findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     * @see Extents#getGeographicBoundingBox(Extent)
     *
     * @category information
     *
     * @since 0.7
     */
    @OptionalCandidate
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateOperation operation) {
        if (operation == null) {
            return null;
        }
        return IdentifiedObjects.getGeographicBoundingBox(operation).orElseGet(
                () -> Extents.intersection(getGeographicBoundingBox(operation.getSourceCRS()),
                                           getGeographicBoundingBox(operation.getTargetCRS())));
    }

    /**
     * Returns the valid geographic area for the given coordinate reference system, or {@code null} if unknown.
     * This method explores the {@linkplain DefaultObjectDomain#getDomainOfValidity() domain of validity}
     * associated with the given CRS. If more than one geographic bounding box is found, then this method
     * computes their {@linkplain DefaultGeographicBoundingBox#add(GeographicBoundingBox) union}.
     * together.
     *
     * @param  crs  the coordinate reference system for which to get the domain of validity, or {@code null}.
     * @return the geographic area where the coordinate reference system is valid, or {@code null} if unspecified.
     *
     * @see #getDomainOfValidity(CoordinateReferenceSystem)
     * @see Extents#getGeographicBoundingBox(Extent)
     *
     * @category information
     */
    @OptionalCandidate
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateReferenceSystem crs) {
        return IdentifiedObjects.getGeographicBoundingBox(crs).orElse(null);
    }

    /**
     * Returns the domain of validity of the specified coordinate reference system, or {@code null} if unknown.
     * If non-null, then the returned envelope will use the same coordinate reference system than the given CRS
     * argument.
     *
     * <p>This method looks in two places:</p>
     * <ol>
     *   <li>First, it checks the {@linkplain DefaultObjectDomain#getDomainOfValidity() domain of validity}
     *       associated with the given CRS. Only geographic extents that are instances of
     *       {@link BoundingPolygon} associated to the given CRS are taken in account for this first step.</li>
     *   <li>If the above step did not found found any bounding polygon, then the
     *       {@linkplain #getGeographicBoundingBox(CoordinateReferenceSystem) geographic bounding boxes}
     *       are used as a fallback and transformed to the given CRS.</li>
     * </ol>
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return the envelope with coordinates in the given CRS, or {@code null} if none.
     *
     * @see #getGeographicBoundingBox(CoordinateReferenceSystem)
     *
     * @category information
     * @since 0.8
     */
    @OptionalCandidate
    public static Envelope getDomainOfValidity(final CoordinateReferenceSystem crs) {
        Envelope envelope = null;
        GeneralEnvelope merged = null;
        if (crs != null) {
            for (final ObjectDomain domain : crs.getDomains()) {
                final Extent domainOfValidity = domain.getDomainOfValidity();
                if (domainOfValidity != null) {
                    for (final GeographicExtent extent : domainOfValidity.getGeographicElements()) {
                        if (extent instanceof BoundingPolygon && !Boolean.FALSE.equals(extent.getInclusion())) {
                            for (final Geometry geometry : ((BoundingPolygon) extent).getPolygons()) {
                                final Envelope candidate = geometry.getEnvelope();
                                if (candidate != null) {
                                    final CoordinateReferenceSystem sourceCRS = candidate.getCoordinateReferenceSystem();
                                    if (sourceCRS == null || equivalent(sourceCRS, crs)) {
                                        if (envelope == null) {
                                            envelope = candidate;
                                        } else {
                                            if (merged == null) {
                                                envelope = merged = new GeneralEnvelope(envelope);
                                            }
                                            merged.add(envelope);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
         * If no envelope was found, uses the geographic bounding box as a fallback. We will
         * need to transform it from WGS84 to the supplied CRS. This step was not required in
         * the previous block because the latter selected only envelopes in the right CRS.
         */
        if (envelope == null) {
            final GeographicBoundingBox bounds = getGeographicBoundingBox(crs);
            if (bounds != null && !Boolean.FALSE.equals(bounds.getInclusion())) {
                /*
                 * We do not assign WGS84 unconditionally to the geographic bounding box, because
                 * it is not defined to be on a particular datum; it is only approximated bounds.
                 * We try to get the GeographicCRS from the user supplied CRS in order to reduce
                 * the number of transformations needed.
                 */
                final SingleCRS targetCRS = getHorizontalComponent(crs);
                final GeographicCRS sourceCRS = ReferencingUtilities.toNormalizedGeographicCRS(targetCRS, false, false);
                if (sourceCRS != null) {
                    envelope = merged = new GeneralEnvelope(bounds);
                    merged.translate(-getGreenwichLongitude(sourceCRS), 0);
                    merged.setCoordinateReferenceSystem(sourceCRS);
                    try {
                        envelope = Envelopes.transform(envelope, targetCRS);
                    } catch (TransformException exception) {
                        /*
                         * The envelope is probably outside the range of validity for this CRS.
                         * It should not occurs, since the envelope is supposed to describe the
                         * CRS area of validity. Logs a warning and returns null, since it is a
                         * legal return value according this method contract.
                         */
                        unexpectedException("getEnvelope", exception);
                        envelope = null;
                    }
                }
            }
        }
        return envelope;
    }

    /**
     * Returns the epoch to which the coordinates of stations defining the dynamic CRS are referenced.
     * If the CRS is associated to a {@linkplain DynamicReferenceFrame dynamic datum}, then the epoch
     * of that datum is returned. Otherwise if the CRS is {@linkplain CompoundCRS compound}, then this
     * method requires that all dynamic components have the same epoch.
     *
     * @param  crs  the coordinate reference frame from which to get the epoch, or {@code null}.
     * @return epoch to which the coordinates of stations defining the dynamic CRS frame are referenced.
     * @throws GeodeticException if some CRS components haave different epochs.
     *
     * @since 1.5
     */
    public static Optional<Temporal> getFrameReferenceEpoch(final CoordinateReferenceSystem crs) {
        Temporal epoch = null;
        if (crs instanceof SingleCRS) {
            final Datum datum = ((SingleCRS) crs).getDatum();
            if (datum instanceof DynamicReferenceFrame) {
                epoch = ((DynamicReferenceFrame) datum).getFrameReferenceEpoch();
            }
        } else if (crs instanceof CompoundCRS) {
            for (SingleCRS component : ((CompoundCRS) crs).getSingleComponents()) {
                final Datum datum = component.getDatum();
                if (datum instanceof DynamicReferenceFrame) {
                    final Temporal t = ((DynamicReferenceFrame) datum).getFrameReferenceEpoch();
                    if (t != null) {
                        if (epoch == null) epoch = t;
                        else if (!epoch.equals(t)) {
                            throw new GeodeticException(Resources.format(Resources.Keys.InconsistentEpochs_2, epoch, t));
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(epoch);
    }

    /**
     * Returns the geodetic reference frame used by the given coordinate reference system.
     * If the given <abbr>CRS</abbr> is an instance of {@link GeodeticCRS}, then this method returns the
     * <abbr>CRS</abbr>'s datum. Otherwise, if the given <abbr>CRS</abbr> is an instance of {@link CompoundCRS},
     * then this method searches for the first geodetic component. Otherwise, this method returns an empty value.
     *
     * @param  crs  the coordinate reference system for which to get the geodetic reference frame, or {@code null}.
     * @return the geodetic reference frame, or an empty value if none.
     *
     * @see DatumOrEnsemble#getEllipsoid(CoordinateReferenceSystem)
     * @see DatumOrEnsemble#getPrimeMeridian(CoordinateReferenceSystem)
     * @see #getGreenwichLongitude(GeodeticCRS)
     *
     * @since 1.6
     */
    public static Optional<GeodeticDatum> getGeodeticReferenceFrame(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeodeticCRS) {
            return Optional.ofNullable(DatumOrEnsemble.asDatum((GeodeticCRS) crs));
        } else if (crs instanceof CompoundCRS) {
            for (CoordinateReferenceSystem component : ((CompoundCRS) crs).getComponents()) {
                final Optional<GeodeticDatum> datum = getGeodeticReferenceFrame(component);
                if (datum.isPresent()) {
                    return datum;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a compound coordinate reference system from an ordered list of CRS components.
     * A CRS is inferred from the given components and the domain of validity is set to the
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent#intersect intersection}
     * of the domain of validity of all components.
     *
     * <h4>Ellipsoidal height</h4>
     * If a two-dimensional geographic or projected CRS is followed or preceded by a vertical CRS with ellipsoidal
     * {@linkplain org.apache.sis.referencing.datum.DefaultVerticalDatum#getRealizationMethod() realization method},
     * this method combines them in a single three-dimensional geographic or projected CRS.  Note that standalone
     * ellipsoidal heights are not allowed according ISO 19111. But if such situation is nevertheless found, then
     * the action described here fixes the issue. This is the reverse of <code>{@linkplain #getVerticalComponent
     * getVerticalComponent}(crs, true)</code>.
     *
     * <h4>Components order</h4>
     * Apache SIS is permissive on the order of components that can be used in a compound CRS.
     * However for better inter-operability, users are encouraged to follow the order mandated by ISO 19162:
     *
     * <ol>
     *   <li>A mandatory horizontal CRS (only one of two-dimensional {@code GeographicCRS} or {@code ProjectedCRS} or {@code EngineeringCRS}).</li>
     *   <li>Optionally followed by a {@code VerticalCRS} or a {@code ParametricCRS} (but not both).</li>
     *   <li>Optionally followed by a {@code TemporalCRS}.</li>
     * </ol>
     *
     * @param  components  the sequence of coordinate reference systems making the compound CRS.
     * @return the compound CRS, or {@code components[0]} if the given array contains only one component.
     * @throws IllegalArgumentException if the given array is empty or if the array contains incompatible components.
     * @throws FactoryException if the geodetic factory failed to create the compound CRS.
     *
     * @since 0.8
     *
     * @see org.apache.sis.referencing.crs.DefaultCompoundCRS
     * @see GeodeticObjectFactory#createCompoundCRS(Map, CoordinateReferenceSystem...)
     * @see org.apache.sis.geometry.Envelopes#compound(Envelope...)
     * @see org.apache.sis.referencing.operation.transform.MathTransforms#compound(MathTransform...)
     */
    public static CoordinateReferenceSystem compound(final CoordinateReferenceSystem... components) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("components", components);
        if (components.length == 1) {
            final CoordinateReferenceSystem crs = components[0];
            if (crs != null) return crs;
        }
        return new EllipsoidalHeightCombiner().createCompoundCRS(components);
    }

    /**
     * Gets or creates a coordinate reference system with a subset of the dimensions of the given CRS.
     * This method can be used for dimensionality reduction, but not for changing axis order.
     * The specified dimensions are used as if they were in strictly increasing order without duplicated values.
     *
     * <h4>Ellipsoidal height</h4>
     * This method can transform a three-dimensional geographic CRS into a two-dimensional geographic CRS.
     * In this aspect, this method is the converse of {@link #compound(CoordinateReferenceSystem...)}.
     * This method can also extract the {@linkplain CommonCRS.Vertical#ELLIPSOIDAL ellipsoidal height}
     * from a three-dimensional geographic CRS, but this is generally not recommended since ellipsoidal
     * heights make little sense without their (<var>latitude</var>, <var>longitude</var>) locations.
     *
     * @param  crs         the CRS to reduce the dimensionality, or {@code null} if none.
     * @param  dimensions  the dimensions to retain. The dimensions will be taken in increasing order, ignoring duplicated values.
     * @return a coordinate reference system for the given dimensions. May be the given {@code crs}, which may be {@code null}.
     * @throws IllegalArgumentException if the given array is empty or if the array contains invalid indices.
     * @throws FactoryException if this method needed to create a new CRS and that operation failed.
     *
     * @see #getComponentAt(CoordinateReferenceSystem, int, int)
     * @see #compound(CoordinateReferenceSystem...)
     *
     * @since 1.3
     */
    public static CoordinateReferenceSystem selectDimensions(final CoordinateReferenceSystem crs,
            final int... dimensions) throws FactoryException
    {
        final var components = selectComponents(crs, dimensions);
        if (components.isEmpty()) {
            return null;
        }
        return compound(components.toArray(CoordinateReferenceSystem[]::new));
    }

    /**
     * Gets or creates CRS components for a subset of the dimensions of the given CRS.
     * The method performs the same work as {@link #selectDimensions(CoordinateReferenceSystem, int...)}
     * except that it does not build new {@link CompoundCRS} instances when the specified dimensions span
     * more than one {@linkplain DefaultCompoundCRS#getComponents() component}.
     * Instead, the components are returned directly.
     *
     * <p>While this method does not create new {@code CompoundCRS} instances, it still may create other
     * kinds of CRS for handling ellipsoidal height as documented in the {@code selectDimensions(…)} method.</p>
     *
     * @param  crs         the CRS from which to get a subset of the components, or {@code null} if none.
     * @param  dimensions  the dimensions to retain. The dimensions will be taken in increasing order, ignoring duplicated values.
     * @return components in the specified dimensions, or an empty list if the specified {@code crs} is {@code null}.
     * @throws IllegalArgumentException if the given array is empty or if the array contains invalid indices.
     * @throws FactoryException if this method needed to create a new CRS and that operation failed.
     *
     * @see #selectDimensions(CoordinateReferenceSystem, int...)
     *
     * @since 1.4
     */
    public static List<CoordinateReferenceSystem> selectComponents(final CoordinateReferenceSystem crs,
            final int... dimensions) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        if (crs == null) {
            return List.of();
        }
        final int dimension = ReferencingUtilities.getDimension(crs);
        long selected = 0;
        for (final int d : dimensions) {
            if (d < 0 || d >= dimension) {
                throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, d));
            }
            if (d >= Long.SIZE) {
                throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, d+1));
            }
            selected |= (1L << d);
        }
        if (selected == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "dimensions"));
        }
        final List<CoordinateReferenceSystem> components = new ArrayList<>(Long.bitCount(selected));
        reduce(0, crs, dimension, selected, components);
        return components;
    }

    /**
     * Adds the components of reduced CRS into the given list.
     * This method may invoke itself recursively for walking through compound CRS.
     *
     * @param  previous    number of dimensions of previous CRS.
     * @param  crs         the CRS for which to select components.
     * @param  dimension   number of dimensions of {@code crs}.
     * @param  selected    bitmask of dimensions to select.
     * @param  addTo       where to add CRS components.
     * @return new bitmask after removal of dimensions of the components added to {@code addTo}.
     */
    private static long reduce(int previous, final CoordinateReferenceSystem crs, int dimension, long selected,
            final List<CoordinateReferenceSystem> addTo) throws FactoryException
    {
        final long current = (Numerics.bitmask(dimension) - 1) << previous;
        final long intersect = selected & current;
choice: if (intersect != 0) {
            if (intersect == current) {
                addTo.add(crs);
                selected &= ~current;
            } else if (crs instanceof CompoundCRS) {
                for (final CoordinateReferenceSystem component : ((CompoundCRS) crs).getComponents()) {
                    dimension = ReferencingUtilities.getDimension(component);
                    selected = reduce(previous, component, dimension, selected, addTo);
                    if ((selected & current) == 0) break;           // Stop if it would be useless to continue.
                    previous += dimension;
                }
            } else if (dimension == 3) {
                final GeodeticCRS baseCRS;
                if (crs instanceof GeodeticCRS) {
                    baseCRS = (GeodeticCRS) crs;
                } else if (crs instanceof ProjectedCRS) {
                    baseCRS = ((ProjectedCRS) crs).getBaseCRS();
                } else {
                    break choice;
                }
                final boolean isVertical = Long.bitCount(intersect) == 1;               // Presumed for now, verified later.
                final int verticalDimension = Long.numberOfTrailingZeros((isVertical ? intersect : ~intersect) >>> previous);
                final CoordinateSystemAxis verticalAxis = crs.getCoordinateSystem().getAxis(verticalDimension);
                if (AxisDirections.isVertical(verticalAxis.getDirection())) try {
                    addTo.add(new EllipsoidalHeightSeparator(baseCRS, isVertical).separate((SingleCRS) crs));
                    selected &= ~current;
                } catch (IllegalArgumentException | ClassCastException e) {
                    throw new FactoryException(Resources.format(Resources.Keys.CanNotSeparateCRS_1, crs.getName()));
                }
            }
        }
        if ((selected & current) != 0) {
            throw new FactoryException(Resources.format(Resources.Keys.CanNotSeparateCRS_1, crs.getName()));
        }
        return selected;
    }

    /**
     * Returns {@code true} if the given CRS is horizontal. The current implementation considers a
     * CRS as horizontal if it is two-dimensional and comply with one of the following conditions:
     *
     * <ul>
     *   <li>is an instance of {@link GeographicCRS} (or an equivalent {@link GeodeticCRS}), or</li>
     *   <li>is an instance of {@link ProjectedCRS}, or</li>
     *   <li>is an instance of {@link EngineeringCRS} (following <abbr>WKT</abbr> 2 definition of {@literal <horizontal crs>}).</li>
     * </ul>
     *
     * In case of doubt, this method conservatively returns {@code false}.
     *
     * @todo Future SIS implementation may extend the above conditions list. For example, a radar station
     *       could use a polar coordinate system in a {@code DerivedCRS} instance based on a projected CRS.
     *       Conversely, a future SIS versions may impose more conditions on <code>EngineeringCRS</code>.
     *       See <a href="http://issues.apache.org/jira/browse/SIS-161">SIS-161</a>.
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return {@code true} if the given CRS is non-null and likely horizontal, or {@code false} otherwise.
     *
     * @see #getHorizontalComponent(CoordinateReferenceSystem)
     *
     * @category information
     */
    public static boolean isHorizontalCRS(final CoordinateReferenceSystem crs) {
        return horizontalCode(crs) == 2;
    }

    /**
     * If the given CRS would qualify as horizontal except for its number of dimensions, returns that number.
     * Otherwise returns 0. The number of dimensions can only be 2 or 3.
     */
    private static int horizontalCode(final CoordinateReferenceSystem crs) {
        /*
         * In order to determine if the CRS is geographic, checking the CoordinateSystem type is more reliable
         * then checking if the CRS implements the GeographicCRS interface.  This is because the GeographicCRS
         * type did not existed in ISO 19111:2007, so a CRS could be standard-compliant without implementing
         * the GeographicCRS interface.
         */
        boolean isEngineering = false;
        final boolean isGeodetic = (crs instanceof GeodeticCRS);
        if (isGeodetic || crs instanceof ProjectedCRS || (isEngineering = (crs instanceof EngineeringCRS))) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            final int dim = cs.getDimension();
            if ((dim & ~1) == 2 && (!isGeodetic || (cs instanceof EllipsoidalCS))) {
                if (isEngineering) {
                    int n = 0;
                    for (int i=0; i<dim; i++) {
                        if (AxisDirections.isCompass(cs.getAxis(i).getDirection())) n++;
                    }
                    // If we don't have exactly 2 east, north, etc. directions, consider as non-horizontal.
                    if (n != 2) return 0;
                }
                return dim;
            }
        }
        return 0;
    }

    /**
     * Returns {@code true} if the given coordinate reference system has an horizontal component.
     * The horizontal component may be part of a higher dimensional CRS, either in the form of a
     * three-dimensional CRS (with an ellipsoidal height) or a compound CRS.
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return whether the given CRS has an horizontal component.
     *
     * @category information
     *
     * @since 1.5
     */
    public static boolean hasHorizontalComponent(final CoordinateReferenceSystem crs) {
        if (horizontalCode(crs) != 0) {
            return true;
        }
        if (crs instanceof CompoundCRS) {
            for (CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                if (hasHorizontalComponent(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the first horizontal coordinate reference system found in the given CRS, or {@code null} if there is
     * none. If the given CRS is already horizontal according {@link #isHorizontalCRS(CoordinateReferenceSystem)},
     * then this method returns it as-is. Otherwise if the given CRS is compound, then this method searches for the
     * first horizontal component in the {@linkplain #getSingleComponents(CoordinateReferenceSystem)
     * list of single components}.
     *
     * <p>In the special case where a three-dimensional geographic or projected CRS is found, this method
     * will create a two-dimensional geographic or projected CRS without the vertical axis.</p>
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return the first horizontal CRS, or {@code null} if none.
     *
     * @category information
     *
     * @see org.apache.sis.geometry.GeneralEnvelope#horizontal()
     */
    @OptionalCandidate
    public static SingleCRS getHorizontalComponent(final CoordinateReferenceSystem crs) {
        switch (horizontalCode(crs)) {
            /*
             * If the CRS is already two-dimensional and horizontal, return as-is.
             * We don't need to check if crs is an instance of SingleCRS since all
             * CRS accepted by horizontalCode(…) are SingleCRS.
             */
            case 2: {
                return (SingleCRS) crs;
            }
            case 3: {
                /*
                 * The CRS would be horizontal if we can remove the vertical axis. CoordinateSystems.replaceAxes(…)
                 * will do this task for us. We can verify if the operation has been successful by checking that
                 * the number of dimensions has been reduced by 1 (from 3 to 2).
                 */
                final CoordinateSystem cs = CoordinateSystems.replaceAxes(crs.getCoordinateSystem(), new AxisFilter() {
                    @Override public boolean accept(final CoordinateSystemAxis axis) {
                        return !AxisDirections.isVertical(axis.getDirection());
                    }
                });
                if (cs.getDimension() != 2) break;
                /*
                 * Most of the time, the CRS to rebuild will be geodetic. In such case we known that the
                 * coordinate system is ellipsoidal because (i.e. the CRS is geographic) because it was
                 * a condition verified by horizontalCode(…). A ClassCastException would be a bug.
                 */
                final Map<String, ?> properties = ReferencingUtilities.getPropertiesForModifiedCRS(crs);
                if (crs instanceof GeodeticCRS) {
                    final var source = (GeodeticCRS) crs;
                    return new DefaultGeographicCRS(properties, source.getDatum(), source.getDatumEnsemble(), (EllipsoidalCS) cs);
                }
                /*
                 * In Apache SIS implementation, the Conversion contains the source and target CRS together with
                 * a MathTransform.   We need to recreate the same conversion, but without CRS and MathTransform
                 * for letting SIS create or associate new ones, which will be two-dimensional now.
                 */
                if (crs instanceof ProjectedCRS) {
                    final var proj = (ProjectedCRS) crs;
                    final var base = (GeodeticCRS) getHorizontalComponent(proj.getBaseCRS());
                    Conversion fromBase = proj.getConversionFromBase();
                    fromBase = new DefaultConversion(IdentifiedObjects.getProperties(fromBase),
                            fromBase.getMethod(), null, fromBase.getParameterValues());
                    return new DefaultProjectedCRS(properties, base, fromBase, (CartesianCS) cs);
                }
                /*
                 * If the CRS is neither geographic or projected, then it is engineering.
                 */
                final var source = (EngineeringCRS) crs;
                return new DefaultEngineeringCRS(properties, source.getDatum(), source.getDatumEnsemble(), cs);
            }
        }
        if (crs instanceof CompoundCRS) {
            for (CoordinateReferenceSystem c : ((CompoundCRS) crs).getComponents()) {
                final SingleCRS candidate = getHorizontalComponent(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first vertical coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code VerticalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first vertical component
     * in the {@linkplain #getSingleComponents(CoordinateReferenceSystem) list of single components}.
     *
     * <h4>Height in a three-dimensional geographic CRS</h4>
     * In ISO 19111 model, ellipsoidal heights are indissociable from geographic CRS because such heights
     * without their (<var>latitude</var>, <var>longitude</var>) locations make little sense. Consequently
     * a standard-conformant library should return {@code null} when asked for the {@code VerticalCRS}
     * component of a geographic CRS. This is what {@code getVerticalComponent(…)} does when the
     * {@code allowCreateEllipsoidal} argument is {@code false}.
     *
     * <p>However, in some exceptional cases, handling ellipsoidal heights like any other kind of heights
     * may simplify the task. For example, when computing <em>difference</em> between heights above the
     * same datum, the impact of ignoring locations may be smaller (but not necessarily canceled).
     * Orphan {@code VerticalCRS} may also be useful for information purpose like labeling a plot axis.
     * If the caller feels confident that ellipsoidal heights are safe for his task, he can set the
     * {@code allowCreateEllipsoidal} argument to {@code true}. In such case, this {@code getVerticalComponent(…)}
     * method will create a temporary {@code VerticalCRS} from the first three-dimensional {@code GeographicCRS}
     * <em>in last resort</em>, only if it cannot find an existing {@code VerticalCRS} instance.
     * <strong>Note that this is not a valid CRS according ISO 19111</strong> — use with care.</p>
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @param  allowCreateEllipsoidal {@code true} for allowing the creation of orphan CRS for ellipsoidal heights.
     *         The recommended value is {@code false}.
     * @return the first vertical CRS, or {@code null} if none.
     *
     * @see #compound(CoordinateReferenceSystem...)
     *
     * @category information
     */
    @OptionalCandidate
    public static VerticalCRS getVerticalComponent(final CoordinateReferenceSystem crs, final boolean allowCreateEllipsoidal) {
        if (crs instanceof VerticalCRS) {
            return (VerticalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            boolean a = false;
            do { // Executed at most twice.
                for (final CoordinateReferenceSystem c : cp.getComponents()) {
                    final VerticalCRS candidate = getVerticalComponent(c, a);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            } while ((a = !a) == allowCreateEllipsoidal);
        }
        if (allowCreateEllipsoidal && horizontalCode(crs) == 3) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            final int i = AxisDirections.indexOfColinear(cs, AxisDirection.UP);
            if (i >= 0) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                VerticalCRS c = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                if (!c.getCoordinateSystem().getAxis(0).equals(axis)) {
                    final Map<String,?> properties = IdentifiedObjects.getProperties(c);
                    c = new DefaultVerticalCRS(properties, c.getDatum(), c.getDatumEnsemble(), new DefaultVerticalCS(properties, axis));
                }
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the first temporal coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code TemporalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first temporal component
     * in the {@linkplain #getSingleComponents(CoordinateReferenceSystem) list of single components}.
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return the first temporal CRS, or {@code null} if none.
     *
     * @category information
     */
    @OptionalCandidate
    public static TemporalCRS getTemporalComponent(final CoordinateReferenceSystem crs) {
        if (crs instanceof TemporalCRS) {
            return (TemporalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            for (final CoordinateReferenceSystem c : cp.getComponents()) {
                final TemporalCRS candidate = getTemporalComponent(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the ordered list of single coordinate reference systems for the specified CRS.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given CRS is null, returns an empty list.</li>
     *   <li>If the given CRS is an instance of {@link SingleCRS}, returns that instance in a singleton list.</li>
     *   <li>If the given CRS is an instance of {@link CompoundCRS}, returns a flattened list of its
     *       {@linkplain DefaultCompoundCRS#getComponents() components}. Some components may themselves be
     *       other {@code CompoundCRS} instances, in which case those compound CRS are also flattened in their
     *       list of {@code SingleCRS} components.</li>
     *   <li>Otherwise throws a {@code ClassCastException}.</li>
     * </ul>
     *
     * <h4>Example</h4>
     * Apache SIS allows 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
     * coordinate reference system to be built in two different ways as shown below:
     *
     * <div class="horizontal-flow">
     * <div><p><b>Hierarchical structure</b></p>
     * <blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>)<br>
     *   <code>  │   ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  │   └─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote></div>
     * <div><p><b>Flat list</b></p>
     * <blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  ├─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote>
     * </div></div>
     *
     * This method guaranteed that the returned list is a flat one as shown on the right side.
     * Note that such flat lists are the only one allowed by ISO/OGC standards for compound CRS.
     * The hierarchical structure is an Apache SIS flexibility.
     *
     * @param  crs  the coordinate reference system, or {@code null}.
     * @return the single coordinate reference systems, or an empty list if the given CRS is {@code null}.
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see DefaultCompoundCRS#getSingleComponents()
     */
    public static List<SingleCRS> getSingleComponents(final CoordinateReferenceSystem crs) {
        final List<SingleCRS> singles;
        if (crs == null) {
            singles = List.of();
        } else if (crs instanceof CompoundCRS) {
            singles = ((CompoundCRS) crs).getSingleComponents();
        } else {
            // Intentional CassCastException here if the crs is not a SingleCRS.
            singles = List.of((SingleCRS) crs);
        }
        return singles;
    }

    /**
     * Returns the coordinate reference system in the given range of dimension indices.
     * This method processes as below:
     *
     * <ul>
     *   <li>If the given {@code crs} is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if {@code lower} is 0 and {@code upper} is the number of CRS dimensions,
     *       then this method returns the given CRS unchanged.</li>
     *   <li>Otherwise if the given CRS is an instance of {@link CompoundCRS}, then this method
     *       searches for a {@linkplain CompoundCRS#getComponents() component} where:
     *       <ul>
     *         <li>The {@linkplain org.apache.sis.referencing.cs.AbstractCS#getDimension() number of dimensions}
     *             is equal to {@code upper - lower};</li>
     *         <li>The sum of the number of dimensions of all previous CRS is equal to {@code lower}.</li>
     *       </ul>
     *       If such component is found, then it is returned.</li>
     *   <li>Otherwise (i.e. no component match), this method returns {@code null}.</li>
     * </ul>
     *
     * This method does <strong>not</strong> build new CRS from the components. For example, this method does not
     * create a {@link CompoundCRS} or a three-dimensional CRS if the given range spans more than one component.
     *
     * @param  crs    the coordinate reference system to decompose, or {@code null}.
     * @param  lower  the first dimension to keep, inclusive.
     * @param  upper  the last  dimension to keep, exclusive.
     * @return the sub-coordinate system, or {@code null} if the given {@code crs} was {@code null}
     *         or cannot be decomposed for dimensions in the [{@code lower} … {@code upper}] range.
     * @throws IndexOutOfBoundsException if the given index are out of bounds.
     *
     * @see #selectDimensions(CoordinateReferenceSystem, int...)
     * @see org.apache.sis.geometry.GeneralEnvelope#subEnvelope(int, int)
     *
     * @since 0.5
     */
    @OptionalCandidate
    public static CoordinateReferenceSystem getComponentAt(CoordinateReferenceSystem crs, int lower, int upper) {
        if (crs == null) return null;     // Skip bounds check.
        int dimension = ReferencingUtilities.getDimension(crs);
        Objects.checkFromToIndex(lower, upper, dimension);
check:  while (lower != 0 || upper != dimension) {
            if (crs instanceof CompoundCRS) {
                // We need nested CompoundCRS (if any) below, not a flattened list of SingleCRS.
                final List<CoordinateReferenceSystem> components = ((CompoundCRS) crs).getComponents();
                final int size = components.size();
                for (int i=0; i<size; i++) {
                    crs = components.get(i);
                    dimension = crs.getCoordinateSystem().getDimension();
                    if (lower < dimension) {
                        /*
                         * The requested dimensions may intersect the dimension of this CRS.
                         * The outer loop will perform the verification, and eventually go
                         * down again in the tree of sub-components.
                         */
                        continue check;
                    }
                    lower -= dimension;
                    upper -= dimension;
                }
            }
            return null;
        }
        return crs;
    }

    /**
     * Returns the Greenwich longitude of the prime meridian of the given CRS in degrees.
     * If the prime meridian uses another unit than degrees, then the value will be converted.
     *
     * @param  crs  the coordinate reference system from which to get the prime meridian.
     * @return the Greenwich longitude (in degrees) of the prime meridian of the given CRS.
     *
     * @see DefaultPrimeMeridian#getGreenwichLongitude(Unit)
     * @see DatumOrEnsemble#getPrimeMeridian(CoordinateReferenceSystem)
     *
     * @since 0.5
     */
    public static double getGreenwichLongitude(final GeodeticCRS crs) {
        return ReferencingUtilities.getGreenwichLongitude(DatumOrEnsemble.getPrimeMeridian(crs).orElse(null), Units.DEGREE);
    }

    /**
     * Returns the system-wide authority factory used by {@link #forCode(String)} and other <abbr>SIS</abbr> methods.
     * If the given authority is non-null, then this method returns a factory specifically for that authority.
     * Otherwise, this method returns the {@link org.apache.sis.referencing.factory.MultiAuthoritiesFactory}
     * instance that manages all other factories.
     *
     * <p>The {@code authority} argument can be {@code "EPSG"}, {@code "OGC"}
     * or any other authority found on the module path.
     * In the {@code "EPSG"} case, whether the full set of <abbr>EPSG</abbr> codes is supported or not
     * depends on whether a {@linkplain org.apache.sis.referencing.factory.sql connection to the database}
     * can be established. If no connection can be established, then this method returns a small embedded
     * <abbr>EPSG</abbr> factory containing at least the <abbr>CRS</abbr>s defined in the
     * {@link #forCode(String)} method javadoc.</p>
     *
     * <p>User-defined authorities can be added to the <abbr>SIS</abbr> environment by creating a {@code CRSAuthorityFactory}
     * implementation with a public no-argument constructor or a public static {@code provider()} method,
     * and declaring the name of that class in the {@code module-info.java} file as a provider of the
     * {@code org.opengis.referencing.crs.CRSAuthorityFactory} service.</p>
     *
     * @param  authority  the authority of the desired factory (typically {@code "EPSG"} or {@code "OGC"}),
     *         or {@code null} for the {@link org.apache.sis.referencing.factory.MultiAuthoritiesFactory}
     *         instance that manage all factories.
     * @return the system-wide authority factory used by <abbr>SIS</abbr> for the given authority.
     * @throws FactoryException if no factory can be returned for the given authority.
     *
     * @see MultiRegisterOperations
     *
     * @since 0.7
     */
    public static CRSAuthorityFactory getAuthorityFactory(final String authority) throws FactoryException {
        if (authority == null) {
            return AuthorityFactories.ALL;
        }
        if (authority.equalsIgnoreCase(Constants.EPSG)) {
            CRSAuthorityFactory factory = ParameterizedTransformBuilder.CREATOR.get();
            if (factory != null) {
                return factory;
            }
        }
        return AuthorityFactories.ALL.getAuthorityFactory(CRSAuthorityFactory.class, authority, null);
    }

    /**
     * Invoked when an unexpected exception occurred. Those exceptions must be non-fatal, i.e. the caller
     * <strong>must</strong> have a reasonable fallback (otherwise it should propagate the exception).
     */
    private static void unexpectedException(final String methodName, final Exception exception) {
        Logging.unexpectedException(LOGGER, CRS.class, methodName, exception);
    }
}
