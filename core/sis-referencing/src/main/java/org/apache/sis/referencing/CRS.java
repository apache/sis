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
import java.util.Collections;
import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSFactory;
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
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Static;

import static java.util.Collections.singletonMap;


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
 * <div class="section">Usage example</div>
 * The most frequently used methods in this class are {@link #forCode forCode(…)}, {@link #fromWKT fromWKT(…)}
 * and {@link #findOperation findOperation(…)}. An usage example is like below
 * (see the <a href="http://sis.apache.org/book/tables/CoordinateReferenceSystems.html">Apache SIS™ Coordinate
 * Reference System (CRS) codes</a> page for the complete list of EPSG codes):
 *
 * {@preformat java
 *   CoordinateReferenceSystem source = CRS.forCode("EPSG:4326");                   // WGS 84
 *   CoordinateReferenceSystem target = CRS.forCode("EPSG:3395");                   // WGS 84 / World Mercator
 *   CoordinateOperation operation = CRS.findOperation(source, target, null);
 *   if (CRS.getLinearAccuracy(operation) > 100) {
 *       // If the accuracy is coarser than 100 metres (or any other threshold at application choice)
 *       // maybe the operation is not suitable. Decide here what to do (throw an exception, etc).
 *   }
 *   MathTransform mt = operation.getMathTransform();
 *   DirectPosition position = new DirectPosition2D(20, 30);            // 20°N 30°E   (watch out axis order!)
 *   position = mt.transform(position, position);
 *   System.out.println(position);
 * }
 *
 * <div class="section">Note on kinds of CRS</div>
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
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class CRS extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CRS() {
    }

    /**
     * Returns the Coordinate Reference System for the given authority code.
     * The set of available codes depends on the {@link CRSAuthorityFactory} instances available on the classpath.
     * There is many thousands of <a href="http://sis.apache.org/book/tables/CoordinateReferenceSystems.html">CRS
     * defined by EPSG authority or by other authorities</a>.
     * The following table lists a very small subset of codes which are guaranteed to be available
     * on any installation of Apache SIS:
     *
     * <blockquote><table class="sis">
     *   <caption>Minimal set of supported authority codes</caption>
     *   <tr><th>Code</th>      <th>Enum</th>                            <th>CRS Type</th>      <th>Description</th></tr>
     *   <tr><td>CRS:27</td>    <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>    <td>Like EPSG:4267 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:83</td>    <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>    <td>Like EPSG:4269 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:84</td>    <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>    <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>EPSG:4047</td> <td>{@link CommonCRS#SPHERE SPHERE}</td> <td>Geographic</td>    <td>GRS 1980 Authalic Sphere</td></tr>
     *   <tr><td>EPSG:4230</td> <td>{@link CommonCRS#ED50   ED50}</td>   <td>Geographic</td>    <td>European Datum 1950</td></tr>
     *   <tr><td>EPSG:4258</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic</td>    <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4267</td> <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>    <td>North American Datum 1927</td></tr>
     *   <tr><td>EPSG:4269</td> <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>    <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:4322</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4326</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4936</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geocentric</td>    <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4937</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic 3D</td> <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4978</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geocentric</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4979</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4984</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geocentric</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4985</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic 3D</td> <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:5715</td> <td>{@link CommonCRS.Vertical#DEPTH DEPTH}</td> <td>Vertical</td> <td>Mean Sea Level depth</td></tr>
     *   <tr><td>EPSG:5714</td> <td>{@link CommonCRS.Vertical#MEAN_SEA_LEVEL MEAN_SEA_LEVEL}</td> <td>Vertical</td> <td>Mean Sea Level height</td></tr>
     * </table></blockquote>
     *
     * This method accepts also the URN and URL syntax.
     * For example the following codes are considered equivalent to {@code "EPSG:4326"}:
     * <ul>
     *   <li>{@code "EPSG::4326"}</li>
     *   <li>{@code "urn:ogc:def:crs:EPSG::4326"}</li>
     *   <li>{@code "http://www.opengis.net/def/crs/epsg/0/4326"}</li>
     *   <li>{@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}</li>
     * </ul>
     *
     * Note that the {@link IdentifiedObjects#lookupURN(IdentifiedObject, Citation)}
     * method can be seen as a converse of this method.
     *
     * @param  code The authority code.
     * @return The Coordinate Reference System for the given authority code.
     * @throws NoSuchAuthorityCodeException If there is no known CRS associated to the given code.
     * @throws FactoryException if the CRS creation failed for an other reason.
     *
     * @see #getAuthorityFactory(String)
     * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory
     *
     * @category factory
     */
    public static CoordinateReferenceSystem forCode(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        try {
            return AuthorityFactories.ALL.createCoordinateReferenceSystem(code);
        } catch (UnavailableFactoryException e) {
            return AuthorityFactories.fallback(e).createCoordinateReferenceSystem(code);
        }
    }

    /**
     * Creates a Coordinate Reference System object from a <cite>Well Known Text</cite> (WKT).
     * The default {@linkplain org.apache.sis.io.wkt Apache SIS parser} understands both
     * version 1 (a.k.a. OGC 01-009) and version 2 (a.k.a. ISO 19162) of the WKT format.
     *
     * <div class="note"><b>Example:</b> below is a slightly simplified WKT 2 string for a Mercator projection.
     * For making this example smaller, some optional {@code UNIT[…]} and {@code ORDER[…]} elements have been omitted.
     *
     * {@preformat wkt
     *   ProjectedCRS["SIRGAS 2000 / Brazil Mercator",
     *     BaseGeodCRS["SIRGAS 2000",
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
     * }
     * </div>
     *
     * <div class="section">Usage and performance considerations</div>
     * This convenience method delegates to
     * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)}
     * using a default factory instance. This is okay for occasional use, but has the following limitations:
     *
     * <ul>
     *   <li>Performance may be sub-optimal in a multi-thread environment.</li>
     *   <li>No control on the WKT {@linkplain org.apache.sis.io.wkt.Convention conventions} in use.</li>
     *   <li>No control on the handling of {@linkplain org.apache.sis.io.wkt.Warnings warnings}.</li>
     * </ul>
     *
     * Applications which need to parse a large amount of WKT strings should consider to use
     * the {@link org.apache.sis.io.wkt.WKTFormat} class instead than this method.
     *
     * @param  text Coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @return The parsed Coordinate Reference System.
     * @throws FactoryException if the given WKT can not be parsed.
     *
     * @see org.apache.sis.io.wkt
     * @see org.apache.sis.geometry.Envelopes#fromWKT(CharSequence)
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
     *
     * @since 0.6
     */
    public static CoordinateReferenceSystem fromWKT(final String text) throws FactoryException {
        ArgumentChecks.ensureNonNull("text", text);
        return DefaultFactories.forBuildin(CRSFactory.class).createFromWKT(text);
    }

    /**
     * Creates a coordinate reference system object from a XML string.
     * Note that the given argument is the XML document itself, <strong>not</strong> a URL to a XML document.
     * For reading XML documents from readers or input streams,
     * see static methods in the {@link org.apache.sis.xml.XML} class.
     *
     * @param  xml Coordinate reference system encoded in XML format.
     * @return The unmarshalled Coordinate Reference System.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.xml.XML#unmarshal(String)
     *
     * @since 0.7
     */
    public static CoordinateReferenceSystem fromXML(final String xml) throws FactoryException {
        ArgumentChecks.ensureNonNull("text", xml);
        return DefaultFactories.forBuildin(CRSFactory.class).createFromXML(xml);
    }

    /**
     * Finds a mathematical operation that transforms or converts coordinates from the given source to the
     * given target coordinate reference system. If an estimation of the geographic area containing the points
     * to transform is known, it can be specified for helping this method to find a better suited operation.
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
     * @param  sourceCRS      the CRS of source coordinates.
     * @param  targetCRS      the CRS of target coordinates.
     * @param  areaOfInterest the area of interest, or {@code null} if none.
     * @return the mathematical operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be created.
     *
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
        CoordinateOperationContext context = null;
        if (areaOfInterest != null) {
            if (areaOfInterest instanceof DefaultGeographicBoundingBox && ((DefaultGeographicBoundingBox) areaOfInterest).isEmpty()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "areaOfInterest"));
            }
            context = new CoordinateOperationContext();
            context.setAreaOfInterest(areaOfInterest);
        }
        return CoordinateOperations.factory().createOperation(sourceCRS, targetCRS, context);
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
     * @param  operation The coordinate operation for which to get the accuracy estimation, or {@code null}.
     * @return The accuracy estimation (always in meters), or NaN if unknown.
     *
     * @see #findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     *
     * @since 0.7
     */
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
     * This method explores the {@linkplain AbstractCoordinateOperation#getDomainOfValidity() domain of validity}
     * associated with the given operation. If more than one geographic bounding box is found, then they will be
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add(GeographicBoundingBox) added}
     * together.
     *
     * @param  operation The coordinate operation for which to get the domain of validity, or {@code null}.
     * @return The geographic area where the operation is valid, or {@code null} if unspecified.
     *
     * @see #findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     * @see Extents#getGeographicBoundingBox(Extent)
     *
     * @category information
     *
     * @since 0.7
     */
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateOperation operation) {
        return (operation != null) ? Extents.getGeographicBoundingBox(operation.getDomainOfValidity()) : null;
    }

    /**
     * Returns the valid geographic area for the given coordinate reference system, or {@code null} if unknown.
     * This method explores the {@linkplain org.apache.sis.referencing.crs.AbstractCRS#getDomainOfValidity() domain of
     * validity} associated with the given CRS. If more than one geographic bounding box is found, then they will be
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add(GeographicBoundingBox) added}
     * together.
     *
     * @param  crs The coordinate reference system for which to get the domain of validity, or {@code null}.
     * @return The geographic area where the coordinate reference system is valid, or {@code null} if unspecified.
     *
     * @see #getEnvelope(CoordinateReferenceSystem)
     * @see Extents#getGeographicBoundingBox(Extent)
     *
     * @category information
     */
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateReferenceSystem crs) {
        return (crs != null) ? Extents.getGeographicBoundingBox(crs.getDomainOfValidity()) : null;
    }

    /**
     * Returns {@code true} if the given CRS is horizontal. The current implementation considers a
     * CRS as horizontal if it is two-dimensional and comply with one of the following conditions:
     *
     * <ul>
     *   <li>is an instance of {@link GeographicCRS} (or an equivalent {@link GeodeticCRS}), or</li>
     *   <li>is an instance of {@link ProjectedCRS}, or</li>
     *   <li>is an instance of {@link EngineeringCRS} (following
     *     <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#111">ISO 19162 §16.1</a>
     *     definition of {@literal <horizontal crs>}).</li>
     * </ul>
     *
     * In case of doubt, this method conservatively returns {@code false}.
     *
     * @todo Future SIS implementation may extend the above conditions list. For example a radar station could
     *       use a polar coordinate system in a <code>DerivedCRS</code> instance based on a projected CRS.
     *       Conversely, a future SIS versions may impose more conditions on <code>EngineeringCRS</code>.
     *       See <a href="http://issues.apache.org/jira/browse/SIS-161">SIS-161</a>.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return {@code true} if the given CRS is non-null and likely horizontal, or {@code false} otherwise.
     *
     * @see #getHorizontalComponent(CoordinateReferenceSystem)
     *
     * @category information
     */
    public static boolean isHorizontalCRS(final CoordinateReferenceSystem crs) {
        /*
         * In order to determine if the CRS is geographic, checking the CoordinateSystem type is more reliable
         * then checking if the CRS implements the GeographicCRS interface.  This is because the GeographicCRS
         * interface is GeoAPI-specific, so a CRS may be OGC-compliant without implementing that interface.
         */
        final boolean isGeodetic = (crs instanceof GeodeticCRS);
        if (isGeodetic || crs instanceof ProjectedCRS || crs instanceof EngineeringCRS) {
            @SuppressWarnings("null")
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs.getDimension() == 2) {
                return !isGeodetic || (cs instanceof EllipsoidalCS);
            }
        }
        return false;
    }

    /**
     * Returns the first horizontal coordinate reference system found in the given CRS, or {@code null} if there is
     * none. If the given CRS is already horizontal according {@link #isHorizontalCRS(CoordinateReferenceSystem)},
     * then this method returns it as-is. Otherwise if the given CRS is compound, then this method searches for the
     * first horizontal component in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem)
     * single components list}.
     *
     * <p>In the special case where a three-dimensional geographic CRS is found, this method will create a
     * two-dimensional geographic CRS without the vertical axis.</p>
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The first horizontal CRS, or {@code null} if none.
     *
     * @category information
     */
    public static SingleCRS getHorizontalComponent(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeodeticCRS) {
            CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {                          // See comment in isHorizontalCRS(…) method.
                final int i = AxisDirections.indexOfColinear(cs, AxisDirection.UP);
                if (i < 0) {
                    return (SingleCRS) crs;
                }
                final CoordinateSystemAxis xAxis = cs.getAxis(i > 0 ? 0 : 1);
                final CoordinateSystemAxis yAxis = cs.getAxis(i > 1 ? 1 : 2);
                cs = CommonCRS.DEFAULT.geographic().getCoordinateSystem();
                if (!Utilities.equalsIgnoreMetadata(cs.getAxis(0), xAxis) ||
                    !Utilities.equalsIgnoreMetadata(cs.getAxis(1), yAxis))
                {
                    // We can not reuse the name of the existing CS, because it typically
                    // contains text about axes including the axis that we just dropped.
                    cs = new DefaultEllipsoidalCS(singletonMap(EllipsoidalCS.NAME_KEY, "Ellipsoidal 2D"), xAxis, yAxis);
                }
                return new DefaultGeographicCRS(
                        ReferencingUtilities.getPropertiesForModifiedCRS(crs, CoordinateReferenceSystem.IDENTIFIERS_KEY),
                        ((GeodeticCRS) crs).getDatum(), (EllipsoidalCS) cs);
            }
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            for (final CoordinateReferenceSystem c : cp.getComponents()) {
                final SingleCRS candidate = getHorizontalComponent(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return isHorizontalCRS(crs) ? (SingleCRS) crs : null;
    }

    /**
     * Returns the first vertical coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code VerticalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first vertical component
     * in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem) single components list}.
     *
     * <div class="section">Height in a three-dimensional geographic CRS</div>
     * In ISO 19111 model, ellipsoidal heights are indissociable from geographic CRS because such heights
     * without their (<var>latitude</var>, <var>longitude</var>) locations make little sense. Consequently
     * a standard-conformant library should return {@code null} when asked for the {@code VerticalCRS}
     * component of a geographic CRS. This is what {@code getVerticalComponent(…)} does when the
     * {@code allowCreateEllipsoidal} argument is {@code false}.
     *
     * <p>However in some exceptional cases, handling ellipsoidal heights like any other kind of heights
     * may simplify the task. For example when computing <em>difference</em> between heights above the
     * same datum, the impact of ignoring locations may be smaller (but not necessarily canceled).
     * Orphan {@code VerticalCRS} may also be useful for information purpose like labeling a plot axis.
     * If the caller feels confident that ellipsoidal heights are safe for his task, he can set the
     * {@code allowCreateEllipsoidal} argument to {@code true}. In such case, this {@code getVerticalComponent(…)}
     * method will create a temporary {@code VerticalCRS} from the first three-dimensional {@code GeographicCRS}
     * <em>in last resort</em>, only if it can not find an existing {@code VerticalCRS} instance.
     * <strong>Note that this is not a valid CRS according ISO 19111</strong> — use with care.</p>
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @param  allowCreateEllipsoidal {@code true} for allowing the creation of orphan CRS for ellipsoidal heights.
     *         The recommended value is {@code false}.
     * @return The first vertical CRS, or {@code null} if none.
     *
     * @category information
     */
    public static VerticalCRS getVerticalComponent(final CoordinateReferenceSystem crs,
            final boolean allowCreateEllipsoidal)
    {
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
        if (allowCreateEllipsoidal && crs instanceof GeodeticCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {                          // See comment in isHorizontalCRS(…) method.
                final int i = AxisDirections.indexOfColinear(cs, AxisDirection.UP);
                if (i >= 0) {
                    final CoordinateSystemAxis axis = cs.getAxis(i);
                    VerticalCRS c = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                    if (!c.getCoordinateSystem().getAxis(0).equals(axis)) {
                        final Map<String,?> properties = IdentifiedObjects.getProperties(c);
                        c = new DefaultVerticalCRS(properties, c.getDatum(), new DefaultVerticalCS(properties, axis));
                    }
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first temporal coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code TemporalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first temporal component
     * in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem) single components list}.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The first temporal CRS, or {@code null} if none.
     *
     * @category information
     */
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
     * <div class="note"><b>Example:</b>
     * Apache SIS allows 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
     * coordinate reference system to be built in two different ways as shown below:
     *
     * <table class="compact" summary="Illustration of a compound CRS.">
     * <tr><th>Hierarchical structure</th><th>Flat list</th></tr>
     * <tr><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>)<br>
     *   <code>  │   ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  │   └─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote></td><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  ├─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote>
     * </td></tr></table>
     *
     * This method guaranteed that the returned list is a flat one as shown on the right side.
     * Note that such flat lists are the only one allowed by ISO/OGC standards for compound CRS.
     * The hierarchical structure is an Apache SIS flexibility.</div>
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The single coordinate reference systems, or an empty list if the given CRS is {@code null}.
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see DefaultCompoundCRS#getSingleComponents()
     */
    public static List<SingleCRS> getSingleComponents(final CoordinateReferenceSystem crs) {
        final List<SingleCRS> singles;
        if (crs == null) {
            singles = Collections.emptyList();
        } else if (crs instanceof CompoundCRS) {
            if (crs instanceof DefaultCompoundCRS) {
                singles = ((DefaultCompoundCRS) crs).getSingleComponents();
            } else {
                final List<CoordinateReferenceSystem> elements = ((CompoundCRS) crs).getComponents();
                singles = new ArrayList<SingleCRS>(elements.size());
                ReferencingUtilities.getSingleComponents(elements, singles);
            }
        } else {
            // Intentional CassCastException here if the crs is not a SingleCRS.
            singles = Collections.singletonList((SingleCRS) crs);
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
     *             is equals to {@code upper - lower};</li>
     *         <li>The sum of the number of dimensions of all previous CRS is equals to {@code lower}.</li>
     *       </ul>
     *       If such component is found, then it is returned.</li>
     *   <li>Otherwise (i.e. no component match), this method returns {@code null}.</li>
     * </ul>
     *
     * This method does <strong>not</strong> attempt to build new CRS from the components.
     * For example it does not attempt to create a 3D geographic CRS from a 2D one + a vertical component.
     *
     * @param  crs   The coordinate reference system to decompose, or {@code null}.
     * @param  lower The first dimension to keep, inclusive.
     * @param  upper The last  dimension to keep, exclusive.
     * @return The sub-coordinate system, or {@code null} if the given {@code crs} was {@code null}
     *         or can not be decomposed for dimensions in the [{@code lower} … {@code upper}] range.
     * @throws IndexOutOfBoundsException If the given index are out of bounds.
     *
     * @since 0.5
     *
     * @see org.apache.sis.geometry.GeneralEnvelope#subEnvelope(int, int)
     */
    public static CoordinateReferenceSystem getComponentAt(CoordinateReferenceSystem crs, int lower, int upper) {
        int dimension = ReferencingUtilities.getDimension(crs);
        ArgumentChecks.ensureValidIndexRange(dimension, lower, upper);
check:  while (lower != 0 || upper != dimension) {
            if (crs instanceof CompoundCRS) {
                final List<CoordinateReferenceSystem> components = ((CompoundCRS) crs).getComponents();
                final int size = components.size();
                for (int i=0; i<size; i++) {
                    crs = components.get(i);
                    dimension = crs.getCoordinateSystem().getDimension();
                    if (lower < dimension) {
                        // The requested dimensions may intersect the dimension of this CRS.
                        // The outer loop will perform the verification, and eventually go
                        // down again in the tree of sub-components.
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
     * If the prime meridian uses an other unit than degrees, then the value will be converted.
     *
     * @param  crs The coordinate reference system from which to get the prime meridian.
     * @return The Greenwich longitude (in degrees) of the prime meridian of the given CRS.
     *
     * @since 0.5
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian#getGreenwichLongitude(Unit)
     */
    public static double getGreenwichLongitude(final GeodeticCRS crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        return ReferencingUtilities.getGreenwichLongitude(crs.getDatum().getPrimeMeridian(), NonSI.DEGREE_ANGLE);
    }

    /**
     * Returns the system-wide authority factory used by {@link #forCode(String)} and other SIS methods.
     * If the given authority is non-null, then this method returns a factory specifically for that authority.
     * Otherwise, this method returns the {@link org.apache.sis.referencing.factory.MultiAuthoritiesFactory}
     * instance that manages all other factories.
     *
     * <p>The {@code authority} argument can be {@code "EPSG"}, {@code "OGC"} or any other authority found
     * on the classpath. In the {@code "EPSG"} case, whether the full set of EPSG codes is supported or not
     * depends on whether a {@linkplain org.apache.sis.referencing.factory.sql connection to the database}
     * can be established. If no connection can be established, then this method returns a small embedded
     * EPSG factory containing at least the CRS defined in the {@link #forCode(String)} method javadoc.</p>
     *
     * <p>User-defined authorities can be added to the SIS environment by creating a {@code CRSAuthorityFactory}
     * implementation with a public no-argument constructor, and declaring the fully-qualified name of that class
     * in a file at the following location:</p>
     *
     * {@preformat text
     *     META-INF/services/org.opengis.referencing.crs.CRSAuthorityFactory
     * }
     *
     * @param  authority The authority of the desired factory (typically {@code "EPSG"} or {@code "OGC"}),
     *         or {@code null} for the {@link org.apache.sis.referencing.factory.MultiAuthoritiesFactory}
     *         instance that manage all factories.
     * @return The system-wide authority factory used by SIS for the given authority.
     * @throws FactoryException if no factory can be returned for the given authority.
     *
     * @see #forCode(String)
     * @see org.apache.sis.referencing.factory.MultiAuthoritiesFactory
     *
     * @since 0.7
     */
    public static CRSAuthorityFactory getAuthorityFactory(final String authority) throws FactoryException {
        if (authority == null) {
            return AuthorityFactories.ALL;
        }
        return AuthorityFactories.ALL.getAuthorityFactory(CRSAuthorityFactory.class, authority, null);
    }
}
