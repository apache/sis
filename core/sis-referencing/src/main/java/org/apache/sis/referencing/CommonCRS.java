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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Duration;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultTimeCS;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Exceptions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Frequently-used geodetic CRS and datum that are guaranteed to be available in SIS.
 * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
 * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
 * then {@code CommonCRS} fallback on hard-coded values. Consequently, those methods never return {@code null}.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code CommonCRS}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
 *
 * <p><b>Example:</b> the following code fetches a geographic Coordinate Reference System using
 * (<var>longitude</var>, <var>latitude</var>) axis order on the {@link #WGS84} geodetic datum:</p>
 *
 * {@preformat java
 *   GeographicCRS crs = CommonCRS.WGS84.normalizedGeographic();
 * }
 *
 * For each enumeration value, the name of the CRS, datum and ellipsoid objects may or may not be the same.
 * Below is an alphabetical list of object names available in this enumeration:
 *
 * <blockquote><table class="sis">
 *   <caption>Geodetic objects accessible by enumeration constants</caption>
 *   <tr><th>Name or alias</th>                                     <th>Object type</th>           <th>Enumeration value</th></tr>
 *   <tr><td>Clarke 1866</td>                                       <td>Ellipsoid</td>             <td>{@link #NAD27}</td></tr>
 *   <tr><td>European Datum 1950 (ED50)</td>                        <td>CRS, datum</td>            <td>{@link #ED50}</td></tr>
 *   <tr><td>European Terrestrial Reference Frame (ETRS) 1989</td>  <td>CRS, datum</td>            <td>{@link #ETRS89}</td></tr>
 *   <tr><td>European Terrestrial Reference System (ETRF) 1989</td> <td>CRS, datum</td>            <td>{@link #ETRS89}</td></tr>
 *   <tr><td>Greenwich</td>                                         <td>Prime meridian</td>        <td>{@link #WGS84}, {@link #WGS72}, {@link #ETRS89}, {@link #NAD83}, {@link #NAD27}, {@link #ED50}, {@link #SPHERE}</td></tr>
 *   <tr><td>GRS 1980</td>                                          <td>Ellipsoid</td>             <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
 *   <tr><td>GRS 1980 Authalic Sphere</td>                          <td>Ellipsoid</td>             <td>{@link #SPHERE}</td></tr>
 *   <tr><td>Hayford 1909</td>                                      <td>Ellipsoid</td>             <td>{@link #ED50}</td></tr>
 *   <tr><td>International 1924</td>                                <td>Ellipsoid</td>             <td>{@link #ED50}</td></tr>
 *   <tr><td>International 1979</td>                                <td>Ellipsoid</td>             <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
 *   <tr><td>North American Datum 1927</td>                         <td>CRS, datum</td>            <td>{@link #NAD27}</td></tr>
 *   <tr><td>North American Datum 1983</td>                         <td>CRS, datum</td>            <td>{@link #NAD83}</td></tr>
 *   <tr><td>NWL 10D</td>                                           <td>Ellipsoid</td>             <td>{@link #WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1972</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1984</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS84}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.factory.CommonAuthorityFactory
 */
@SuppressWarnings("DoubleCheckedLocking")
public enum CommonCRS {
    /**
     * World Geodetic System 1984.
     * This is the default CRS for most {@code org.apache.sis} packages.
     *
     * <blockquote><table class="compact" summary="WGS84 properties.">
     *   <tr><th>WMS identifier:</th>          <td>CRS:84</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4326 &nbsp;(<i>datum:</i> 6326, &nbsp;<i>ellipsoid:</i> 7030)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"WGS 84" &nbsp;(<i>datum:</i> "World Geodetic System 1984")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "WGS 84", &nbsp;<i>ellipsoid:</i> "WGS84")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257223563 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 60 in North and South hemispheres</td></tr>
     * </table></blockquote>
     */
    WGS84((short) 4326, (short) 4979, (short) 4978, (short) 6326, (short) 7030,     // Geodetic info
          (short) 32600, (short) 32700, (byte) 1, (byte) 60),                       // UTM info

    /**
     * World Geodetic System 1972.
     *
     * <blockquote><table class="compact" summary="WGS72 properties.">
     *   <tr><th>EPSG identifiers:</th>        <td>4322 &nbsp;(<i>datum:</i> 6322, &nbsp;<i>ellipsoid:</i> 7043)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"WGS 72" &nbsp;(<i>datum:</i> "World Geodetic System 1972")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "WGS 72", &nbsp;<i>ellipsoid:</i> "NWL 10D")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378135</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356751 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.26 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 60 in North and South hemispheres</td></tr>
     * </table></blockquote>
     */
    WGS72((short) 4322, (short) 4985, (short) 4984, (short) 6322, (short) 7043,     // Geodetic info
          (short) 32200, (short) 32300, (byte) 1, (byte) 60),                       // UTM info

    /**
     * North American Datum 1983.
     * The ellipsoid is <cite>"GRS 1980"</cite>, also known as <cite>"International 1979"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact" summary="NAD83 properties.">
     *   <tr><th>WMS identifier:</th>          <td>CRS:83</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4269 &nbsp;(<i>datum:</i> 6269, &nbsp;<i>ellipsoid:</i> 7019)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"NAD83" &nbsp;(<i>datum:</i> "North American Datum 1983", &nbsp;<i>ellipsoid:</i> "GRS 1980")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>"NAD83 (1986)" &nbsp;(<i>ellipsoid:</i> "International 1979")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 23 in the North hemisphere</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * {@link #ETRS89} uses the same ellipsoid for a different datum.
     * The <cite>Web Map Server</cite> {@code "CRS:83"} authority code uses the NAD83 datum,
     * while the {@code "IGNF:MILLER"} authority code uses the GRS80 datum.</div>
     */
    NAD83((short) 4269, (short) 0, (short) 0, (short) 6269, (short) 7019,           // Geodetic info
          (short) 26900, (short) 0, (byte) 1, (byte) 23),                           // UTM info

    /**
     * North American Datum 1927.
     *
     * <blockquote><table class="compact" summary="NAD27 properties.">
     *   <tr><th>WMS identifier:</th>          <td>CRS:27</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4267 &nbsp;(<i>datum:</i> 6267, &nbsp;<i>ellipsoid:</i> 7008)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"NAD27" &nbsp;(<i>datum:</i> "North American Datum 1927", &nbsp;<i>ellipsoid:</i> "Clarke 1866")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "NAD27")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378206.4</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356583.8 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>1 to 22 in the North hemisphere</td></tr>
     * </table></blockquote>
     */
    NAD27((short) 4267, (short) 0, (short) 0, (short) 6267, (short) 7008,           // Geodetic info
          (short) 26700, (short) 0, (byte) 1, (byte) 22),                           // UTM info

    /**
     * European Terrestrial Reference System 1989.
     * The ellipsoid is <cite>"GRS 1980"</cite>, also known as <cite>"International 1979"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact" summary="ETRS89 properties.">
     *   <tr><th>EPSG identifiers:</th>        <td>4258 &nbsp;(<i>datum:</i> 6258, &nbsp;<i>ellipsoid:</i> 7019)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"ETRS89" &nbsp;(<i>datum:</i> "European Terrestrial Reference System 1989", &nbsp;<i>ellipsoid:</i> "GRS 1980")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>"ETRF89", "EUREF89", "ETRS89-GRS80" &nbsp;(<i>ellipsoid:</i> "International 1979")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>28 to 37 in the North hemisphere</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * {@link #NAD83} uses the same ellipsoid for a different datum.
     * The <cite>Web Map Server</cite> {@code "CRS:83"} authority code uses the NAD83 datum,
     * while the {@code "IGNF:MILLER"} authority code uses the GRS80 datum.</div>
     */
    ETRS89((short) 4258, (short) 4937, (short) 4936, (short) 6258, (short) 7019,    // Geodetic info
           (short) 25800, (short) 0, (byte) 28, (byte) 37),                         // UTM info

    /**
     * European Datum 1950.
     *
     * <blockquote><table class="compact" summary="ED50 properties.">
     *   <tr><th>EPSG identifiers:</th>        <td>4230 &nbsp;(<i>datum:</i> 6230, &nbsp;<i>ellipsoid:</i> 7022)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"ED50" &nbsp;(<i>datum:</i> "European Datum 1950", &nbsp;<i>ellipsoid:</i> "International 1924")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "ED50", <i>ellipsoid:</i> "Hayford 1909")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378388</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356912 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>297 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     *   <tr><th>UTM zones:</th>               <td>28 to 38 in the North hemisphere</td></tr>
     * </table></blockquote>
     */
    ED50((short) 4230, (short) 0, (short) 0, (short) 6230, (short) 7022,            // Geodetic info
           (short) 23000, (short) 0, (byte) 28, (byte) 38),                         // UTM info

    /**
     * Unspecified datum based upon the GRS 1980 Authalic Sphere. Spheres use a simpler algorithm for
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#orthodromicDistance
     * orthodromic distance computation}, which may be faster and more robust.
     *
     * <blockquote><table class="compact" summary="Sphere properties.">
     *   <tr><th>EPSG identifiers:</th>        <td>4047 &nbsp;(<i>datum:</i> 6047, &nbsp;<i>ellipsoid:</i> 7048)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"Unspecified datum based upon the GRS 1980 Authalic Sphere"</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6371007</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6371007 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    SPHERE((short) 4047, (short) 0, (short) 0, (short) 6047, (short) 7048,          // Geodetic info
           (short) 0, (short) 0, (byte) 0, (byte) 0);                               // UTM info

    /**
     * The enum for the default CRS.
     *
     * @see #defaultGeographic()
     */
    static final CommonCRS DEFAULT = WGS84;

    /**
     * Properties to exclude when using an other object as a template.
     */
    private static final String[] EXCLUDE = new String[] {IdentifiedObject.IDENTIFIERS_KEY};

    /**
     * The EPSG code of the two-dimensional geographic CRS.
     */
    final short geographic;

    /**
     * The EPSG code of the three-dimensional geographic CRS, or 0 if none.
     * For non-zero value, this is often the {@link #geocentric} code + 1.
     */
    final short geo3D;

    /**
     * The EPSG code of the geocentric CRS, or 0 if none.
     */
    final short geocentric;

    /**
     * The EPSG code of the datum. The value is often {@link #geographic} + 2000,
     * but it doesn't have to be always the case.
     */
    final short datum;

    /**
     * The EPSG code of the ellipsoid.
     */
    final short ellipsoid;

    /**
     * EPSG codes of pseudo "UTM zone zero" (North case and South case), or 0 if none.
     */
    final short northUTM, southUTM;

    /**
     * Zone number of the first UTM and last UTM zone defined in the EPSG database, inclusive.
     */
    final byte firstZone, lastZone;

    /**
     * The cached object. This is initially {@code null}, then set to various kind of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    private transient volatile IdentifiedObject cached;

    /**
     * The normalized geographic CRS, created when first needed.
     *
     * @see #normalizedGeographic()
     */
    private transient volatile GeographicCRS cachedNormalized;

    /**
     * The three-dimensional geographic CRS, created when first needed.
     *
     * @see #geographic3D()
     */
    private transient volatile GeographicCRS cachedGeo3D;

    /**
     * The geocentric CRS using Cartesian coordinate system, created when first needed.
     *
     * @see #geocentric()
     */
    private transient volatile GeocentricCRS cachedGeocentric;

    /**
     * The geocentric CRS using spherical coordinate system, created when first needed.
     *
     * @see #spherical()
     */
    private transient volatile GeocentricCRS cachedSpherical;

    /**
     * The Universal Transverse Mercator projections, created when first needed.
     * All accesses to this map shall be synchronized on {@code cachedUTM}.
     *
     * @see #UTM(double, double)
     */
    private final Map<Integer,ProjectedCRS> cachedUTM;

    /**
     * Creates a new constant for the given EPSG or SIS codes.
     *
     * @param geographic The EPSG code for the two-dimensional geographic CRS.
     * @param geo3D      The EPSG code of the three-dimensional geographic CRS, or 0 if none.
     * @param geocentric The EPSG code of the geocentric CRS, or 0 if none.
     * @param datum      The EPSG code for the datum.
     * @param ellipsoid  The EPSG code for the ellipsoid.
     */
    private CommonCRS(final short geographic, final short geo3D, final short geocentric, final short datum, final short ellipsoid,
            final short northUTM, final short southUTM, final byte firstZone, final byte lastZone)
    {
        this.geographic = geographic;
        this.geocentric = geocentric;
        this.geo3D      = geo3D;
        this.datum      = datum;
        this.ellipsoid  = ellipsoid;
        this.northUTM   = northUTM;
        this.southUTM   = southUTM;
        this.firstZone  = firstZone;
        this.lastZone   = lastZone;
        cachedUTM = new HashMap<Integer,ProjectedCRS>();
    }

    /**
     * Registers a listeners to be invoked when the classpath changed.
     * This will clear the cache, since the EPSG database may have changed.
     */
    static {
        SystemListener.add(new SystemListener(Modules.REFERENCING) {
            @Override protected void classpathChanged() {
                for (final CommonCRS e : values()) {
                    e.clear();
                }
            }
        });
    }

    /**
     * Invoked by when the cache needs to be cleared after a classpath change.
     */
    @SuppressWarnings("NestedSynchronizedStatement")    // Safe because cachedUTM never call any method of 'this'.
    synchronized void clear() {
        cached           = null;
        cachedGeo3D      = null;
        cachedNormalized = null;
        cachedGeocentric = null;
        synchronized (cachedUTM) {
            cachedUTM.clear();
        }
    }

    /**
     * Returns the default two-dimensional normalized geographic CRS.
     * The CRS returned by this method has the following properties:
     *
     * <ul>
     *   <li>Axis order is (<var>longitude</var>, <var>latitude</var>).</li>
     *   <li>Axis directions are ({@linkplain AxisDirection#EAST East}, {@linkplain AxisDirection#NORTH North}).</li>
     *   <li>Angular unit is {@link NonSI#DEGREE_ANGLE}.</li>
     *   <li>Prime meridian in Greenwich.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This method makes no guarantees about the datum. The current default datum is WGS 84,
     * but this may change in future SIS versions if a WGS 84 replacement become in wide use.</div>
     *
     * This default CRS is assigned to
     * {@linkplain org.apache.sis.geometry.GeneralEnvelope#GeneralEnvelope(org.opengis.metadata.extent.GeographicBoundingBox)
     * envelopes created from a geographic bounding box}.
     * Since ISO 19115 {@link org.opengis.metadata.extent.GeographicBoundingBox} is approximative by definition,
     * their datum can be arbitrary.
     *
     * @return The default two-dimensional geographic CRS with (<var>longitude</var>, <var>latitude</var>) axis order.
     */
    public static GeographicCRS defaultGeographic() {
        return DEFAULT.normalizedGeographic();
    }

    /**
     * Returns a two-dimensional geographic CRS with axes in the non-standard but computationally convenient
     * (<var>longitude</var>, <var>latitude</var>) order. The coordinate system axes will be oriented toward
     * {@linkplain AxisDirection#EAST East} and {@linkplain AxisDirection#NORTH North} respectively, with units
     * in degrees. The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>Code</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td></td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td>CRS:27</td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td>CRS:83</td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td></td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>CRS:84</td></tr>
     * </table></blockquote>
     *
     * @return The geographic CRS with non-standard (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS#forConvention(AxesConvention)
     * @see AxesConvention#NORMALIZED
     */
    public GeographicCRS normalizedGeographic() {
        GeographicCRS object = cachedNormalized;
        if (object == null) {
            DefaultGeographicCRS crs = DefaultGeographicCRS.castOrCopy(geographic());
            crs = crs.forConvention(AxesConvention.RIGHT_HANDED); // Equivalent to NORMALIZED in our cases, but faster.
            synchronized (this) {
                object = cachedNormalized;
                if (object == null) {
                    cachedNormalized = object = crs;
                }
            }
        }
        return object;
    }

    /**
     * Returns the two-dimensional geographic CRS with axes in the standard (<var>latitude</var>, <var>longitude</var>)
     * order. The coordinate system axes will be oriented toward {@linkplain AxisDirection#NORTH North} and
     * {@linkplain AxisDirection#EAST East} respectively, with units in degrees.
     * The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td>4230</td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4258</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td>4267</td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td>4269</td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td>4047</td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4322</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4326</td></tr>
     * </table></blockquote>
     *
     * @return The geographic CRS with standard (<var>latitude</var>, <var>longitude</var>) axis order.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS
     */
    public GeographicCRS geographic() {
        GeographicCRS object = geographic(cached);
        if (object == null) {
            synchronized (this) {
                object = geographic(cached);
                if (object == null) {
                    final CRSAuthorityFactory factory = crsFactory();
                    if (factory != null) try {
                        cached = object = factory.createGeographicCRS(String.valueOf(geographic));
                        return object;
                    } catch (FactoryException e) {
                        failure(this, "geographic", e, geographic);
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6422.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final EllipsoidalCS cs;
                    if (this == DEFAULT) {
                        cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem((short) 6422);
                    } else {
                        cs = DEFAULT.geographic().getCoordinateSystem();
                    }
                    object = StandardDefinitions.createGeographicCRS(geographic, datum(), cs);
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the three-dimensional geographic CRS with axes in the standard (<var>latitude</var>,
     * <var>longitude</var>, <var>height</var>) order. The following table summarizes the coordinate
     * reference systems known to this class, together with an enumeration value that can be used for
     * fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geographic CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4937</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td></td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4985</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4979</td></tr>
     * </table></blockquote>
     *
     * @return The three-dimensional geographic CRS associated to this enum.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeographicCRS
     */
    public GeographicCRS geographic3D() {
        GeographicCRS object = cachedGeo3D;
        if (object == null) {
            synchronized (this) {
                object = cachedGeo3D;
                if (object == null) {
                    if (geo3D != 0) {
                        final CRSAuthorityFactory factory = crsFactory();
                        if (factory != null) try {
                            cachedGeo3D = object = factory.createGeographicCRS(String.valueOf(geo3D));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "geographic3D", e, geo3D);
                        }
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6423.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final EllipsoidalCS cs;
                    if (this == DEFAULT) {
                        cs = (EllipsoidalCS) StandardDefinitions.createCoordinateSystem((short) 6423);
                    } else {
                        cs = DEFAULT.geographic3D().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeographicCRS(properties(base, geo3D), base.getDatum(), cs);
                    cachedGeo3D = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geocentric CRS using a Cartesian coordinate system. Axis units are metres.
     * The following table summarizes the coordinate reference systems known to this class,
     * together with an enumeration value that can be used for fetching that CRS:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geocentric CRS</caption>
     *   <tr><th>Name or alias</th>            <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>ED50</td>                     <td>{@link #ED50}</td>   <td></td></tr>
     *   <tr><td>ETRS89</td>                   <td>{@link #ETRS89}</td> <td>4936</td></tr>
     *   <tr><td>NAD27</td>                    <td>{@link #NAD27}</td>  <td></td></tr>
     *   <tr><td>NAD83</td>                    <td>{@link #NAD83}</td>  <td></td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td> <td>{@link #SPHERE}</td> <td></td></tr>
     *   <tr><td>WGS 72</td>                   <td>{@link #WGS72}</td>  <td>4984</td></tr>
     *   <tr><td>WGS 84</td>                   <td>{@link #WGS84}</td>  <td>4978</td></tr>
     * </table></blockquote>
     *
     * @return The geocentric CRS associated to this enum.
     *
     * @see CRS#forCode(String)
     * @see DefaultGeocentricCRS
     */
    public GeocentricCRS geocentric() {
        GeocentricCRS object = cachedGeocentric;
        if (object == null) {
            synchronized (this) {
                object = cachedGeocentric;
                if (object == null) {
                    if (geocentric != 0) {
                        final CRSAuthorityFactory factory = crsFactory();
                        if (factory != null) try {
                            cachedGeocentric = object = factory.createGeocentricCRS(String.valueOf(geocentric));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "geocentric", e, geocentric);
                        }
                    }
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6500.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    final CartesianCS cs;
                    if (this == DEFAULT) {
                        cs = (CartesianCS) StandardDefinitions.createCoordinateSystem((short) 6500);
                    } else {
                        cs = (CartesianCS) DEFAULT.geocentric().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeocentricCRS(properties(base, geocentric), base.getDatum(), cs);
                    cachedGeocentric = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geocentric CRS using a spherical coordinate system. Axes are:
     *
     * <ol>
     *   <li>Spherical latitude in degrees oriented toward {@linkplain AxisDirection#NORTH north}.</li>
     *   <li>Spherical longitude in degrees oriented toward {@linkplain AxisDirection#EAST east}.</li>
     *   <li>Geocentric radius in metres oriented toward {@linkplain AxisDirection#UP up}.</li>
     * </ol>
     *
     * @return The geocentric CRS associated to this enum.
     *
     * @see DefaultGeocentricCRS
     *
     * @since 0.7
     */
    public GeocentricCRS spherical() {
        GeocentricCRS object = cachedSpherical;
        if (object == null) {
            synchronized (this) {
                object = cachedSpherical;
                if (object == null) {
                    /*
                     * All constants defined in this enumeration use the same coordinate system, EPSG:6404.
                     * We will arbitrarily create this CS only for the most frequently created CRS,
                     * and share that CS instance for all other constants.
                     */
                    SphericalCS cs = null;
                    if (this == DEFAULT) {
                        final CSAuthorityFactory factory = csFactory();
                        if (factory != null) try {
                            cs = factory.createSphericalCS("6404");
                        } catch (FactoryException e) {
                            failure(this, "spherical", e, (short) 6404);
                        }
                        if (cs == null) {
                            cs = (SphericalCS) StandardDefinitions.createCoordinateSystem((short) 6404);
                        }
                    } else {
                        cs = (SphericalCS) DEFAULT.spherical().getCoordinateSystem();
                    }
                    // Use same name and datum than the geographic CRS.
                    final GeographicCRS base = geographic();
                    object = new DefaultGeocentricCRS(IdentifiedObjects.getProperties(base, EXCLUDE), base.getDatum(), cs);
                    cachedSpherical = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geodetic datum associated to this geodetic object.
     * The following table summarizes the datums known to this class,
     * together with an enumeration value that can be used for fetching that datum:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used geodetic datums</caption>
     *   <tr><th>Name or alias</th>                                     <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>European Datum 1950</td>                               <td>{@link #ED50}</td>   <td>6230</td></tr>
     *   <tr><td>European Terrestrial Reference System 1989</td>        <td>{@link #ETRS89}</td> <td>6258</td></tr>
     *   <tr><td>North American Datum 1927</td>                         <td>{@link #NAD27}</td>  <td>6267</td></tr>
     *   <tr><td>North American Datum 1983</td>                         <td>{@link #NAD83}</td>  <td>6269</td></tr>
     *   <tr><td>Not specified (based on GRS 1980 Authalic Sphere)</td> <td>{@link #SPHERE}</td> <td>6047</td></tr>
     *   <tr><td>World Geodetic System 1972</td>                        <td>{@link #WGS72}</td>  <td>6322</td></tr>
     *   <tr><td>World Geodetic System 1984</td>                        <td>{@link #WGS84}</td>  <td>6326</td></tr>
     * </table></blockquote>
     *
     * @return The geodetic datum associated to this enum.
     *
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     */
    public GeodeticDatum datum() {
        GeodeticDatum object = datum(cached);
        if (object == null) {
            synchronized (this) {
                object = datum(cached);
                if (object == null) {
                    final DatumAuthorityFactory factory = datumFactory();
                    if (factory != null) try {
                        cached = object = factory.createGeodeticDatum(String.valueOf(datum));
                        return object;
                    } catch (FactoryException e) {
                        failure(this, "datum", e, datum);
                    }
                    object = StandardDefinitions.createGeodeticDatum(datum, ellipsoid(), primeMeridian());
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the ellipsoid associated to this geodetic object.
     * The following table summarizes the ellipsoids known to this class,
     * together with an enumeration value that can be used for fetching that ellipsoid:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used ellipsoids</caption>
     *   <tr><th>Name or alias</th>                    <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>Clarke 1866</td>                      <td>{@link #NAD27}</td>  <td>7008</td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td>         <td>{@link #SPHERE}</td> <td>7048</td></tr>
     *   <tr><td>International 1924</td>               <td>{@link #ED50}</td>   <td>7022</td></tr>
     *   <tr><td>International 1979 / GRS 1980</td>    <td>{@link #ETRS89}</td> <td>7019</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1972</td> <td>{@link #WGS72}</td>  <td>7043</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1984</td> <td>{@link #WGS84}</td>  <td>7030</td></tr>
     * </table></blockquote>
     *
     * @return The ellipsoid associated to this enum.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    public Ellipsoid ellipsoid() {
        Ellipsoid object = ellipsoid(cached);
        if (object == null) {
            synchronized (this) {
                object = ellipsoid(cached);
                if (object == null) {
                    if (this == NAD83) {
                        object = ETRS89.ellipsoid();            // Share the same instance for NAD83 and ETRS89.
                    } else {
                        final DatumAuthorityFactory factory = datumFactory();
                        if (factory != null) try {
                            cached = object = factory.createEllipsoid(String.valueOf(ellipsoid));
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "ellipsoid", e, ellipsoid);
                        }
                        object = StandardDefinitions.createEllipsoid(ellipsoid);
                    }
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the prime meridian associated to this geodetic object.
     * The following table summarizes the prime meridians known to this class,
     * together with an enumeration value that can be used for fetching that prime meridian:
     *
     * <blockquote><table class="sis">
     *   <caption>Commonly used prime meridians</caption>
     *   <tr><th>Name or alias</th> <th>Enum</th>           <th>EPSG</th></tr>
     *   <tr><td>Greenwich</td>     <td>{@link #WGS84}</td> <td>8901</td></tr>
     * </table></blockquote>
     *
     * @return The prime meridian associated to this enum.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    public PrimeMeridian primeMeridian() {
        PrimeMeridian object = primeMeridian(cached);
        if (object == null) {
            synchronized (this) {
                object = primeMeridian(cached);
                if (object == null) {
                    if (this != DEFAULT) {
                        object = DEFAULT.primeMeridian();           // Share the same instance for all constants.
                    } else {
                        final DatumAuthorityFactory factory = datumFactory();
                        if (factory != null) try {
                            cached = object = factory.createPrimeMeridian(StandardDefinitions.GREENWICH);
                            return object;
                        } catch (FactoryException e) {
                            failure(this, "primeMeridian", e, (short) 8901);
                        }
                        object = StandardDefinitions.primeMeridian();
                    }
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the geographic CRS associated to the given object, or {@code null} if none.
     */
    private static GeographicCRS geographic(final IdentifiedObject object) {
        return (object instanceof GeographicCRS) ? (GeographicCRS) object : null;
    }

    /**
     * Returns the datum associated to the given object, or {@code null} if none.
     */
    private static GeodeticDatum datum(final IdentifiedObject object) {
        if (object instanceof GeodeticDatum) {
            return (GeodeticDatum) object;
        }
        if (object instanceof GeodeticCRS) {
            return ((GeodeticCRS) object).getDatum();
        }
        return null;
    }

    /**
     * Returns the ellipsoid associated to the given object, or {@code null} if none.
     */
    private static Ellipsoid ellipsoid(final IdentifiedObject object) {
        if (object instanceof Ellipsoid) {
            return (Ellipsoid) object;
        }
        final GeodeticDatum datum = datum(object);
        return (datum != null) ? datum.getEllipsoid() : null;
    }

    /**
     * Returns the prime meridian associated to the given object, or {@code null} if none.
     */
    private static PrimeMeridian primeMeridian(final IdentifiedObject object) {
        if (object instanceof PrimeMeridian) {
            return (PrimeMeridian) object;
        }
        final GeodeticDatum datum = datum(object);
        return (datum != null) ? datum.getPrimeMeridian() : null;
    }

    /*
     * NOTE ABOUT MAP PROJECTION CONVENIENCE METHODS:
     * There is no convenience method for projections other than UTM because this enumeration is not a
     * factory for arbitrary CRS (the UTM projection has the advantage of being constrained to zones).
     * World-wide projections like "WGS 84 / World Mercator" are not handled neither because they make
     * sense only for some datum like WGS84 or WGS72. Application to more regional datum like NAD27 or
     * ED50 would be more questionable.
     */

    /**
     * Returns a Universal Transverse Mercator (UTM) projection for the zone containing the given point.
     * There is a total of 120 UTM zones, with 60 zones in the North hemisphere and 60 zones in the South hemisphere.
     * The projection zone is determined from the arguments as below:
     *
     * <ul>
     *   <li>The sign of the <var>latitude</var> argument determines the hemisphere:
     *       North for positive latitudes (including positive zero) or
     *       South for negative latitudes (including negative zero).
     *       The latitude magnitude is ignored, except for ensuring that the latitude is inside the [-90 … 90]° range.</li>
     *   <li>The value of the <var>longitude</var> argument determines the 6°-width zone,
     *       numbered from 1 for the zone starting at 180°W up to 60 for the zone finishing at 180°E.
     *       Longitudes outside the [-180 … 180]° range will be rolled as needed before to compute the zone.</li>
     * </ul>
     *
     * <div class="note"><b>Warning:</b>
     * be aware of parameter order! For this method, latitude is first.
     * This order is for consistency with the non-normalized {@linkplain #geographic() geographic} CRS
     * of all items in this {@code CommonCRS} enumeration.</div>
     *
     * The map projection uses the following parameters:
     *
     * <blockquote><table class="sis">
     *   <caption>Universal Transverse Mercator (UTM) parameters</caption>
     *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>0°</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>Central meridian of the UTM zone containing the given longitude</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.9996</td></tr>
     *   <tr><td>False easting</td>                  <td>500000 metres</td></tr>
     *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
     * </table></blockquote>
     *
     * The coordinate system axes are (Easting, Northing) in metres.
     *
     * @param  latitude  A latitude in the desired UTM projection zone.
     * @param  longitude A longitude in the desired UTM projection zone.
     * @return A Universal Transverse Mercator projection for the zone containing the given point.
     *
     * @since 0.7
     */
    public ProjectedCRS UTM(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude",   Latitude.MIN_VALUE,     Latitude.MAX_VALUE,     latitude);
        ArgumentChecks.ensureBetween("longitude", -Formulas.LONGITUDE_MAX, Formulas.LONGITUDE_MAX, longitude);
        final boolean isSouth = MathFunctions.isNegative(latitude);
        final int zone = TransverseMercator.zone(longitude);
        final Integer key = isSouth ? -zone : zone;
        ProjectedCRS crs;
        synchronized (cachedUTM) {
            crs = cachedUTM.get(key);
        }
        if (crs == null) {
            int code = 0;
            if (zone >= firstZone && zone <= lastZone) {
                code = JDK8.toUnsignedInt(isSouth ? southUTM : northUTM);
                if (code != 0) {
                    code += zone;
                    final CRSAuthorityFactory factory = crsFactory();
                    if (factory != null) try {
                        return factory.createProjectedCRS(String.valueOf(code));
                    } catch (FactoryException e) {
                        failure(this, "UTM", e, code);
                    }
                }
            }
            /*
             * All constants defined in this enumeration use the same coordinate system, EPSG:4400.
             * We will arbitrarily create this CS only for a frequently created CRS, and share that
             * CS instance for all other constants.
             */
            CartesianCS cs = null;
            synchronized (DEFAULT.cachedUTM) {
                final Iterator<ProjectedCRS> it = DEFAULT.cachedUTM.values().iterator();
                if (it.hasNext()) {
                    cs = it.next().getCoordinateSystem();
                }
            }
            if (cs == null) {
                if (this != DEFAULT) {
                    cs = DEFAULT.UTM(latitude, longitude).getCoordinateSystem();
                } else {
                    cs = (CartesianCS) StandardDefinitions.createCoordinateSystem((short) 4400);
                }
            }
            crs = StandardDefinitions.createUTM(code, geographic(), latitude, longitude, cs);
            final ProjectedCRS other;
            synchronized (cachedUTM) {
                other = JDK8.putIfAbsent(cachedUTM, key, crs);
            }
            if (other != null) {
                return other;
            }
        }
        return crs;
    }




    /**
     * Frequently-used vertical CRS and datum that are guaranteed to be available in SIS.
     * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
     * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
     * then {@code Vertical} fallback on hard-coded values. Consequently, those methods never return {@code null}.
     *
     * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code Vertical}
     * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
     * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a vertical Coordinate Reference System for heights
     * above the Mean Sea Level (MSL):</p>
     *
     * {@preformat java
     *   VerticalCRS crs = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
     * }
     *
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
     *   <caption>Geodetic objects accessible by enumeration constants</caption>
     *   <tr><th>Name or alias</th>                      <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Barometric altitude</td>                <td>CRS, Datum</td>  <td>{@link #BAROMETRIC}</td></tr>
     *   <!-- <s>Ellipsoidal height</s> intentionally omitted                 <td><s>{@link #ELLIPSOIDAL}</s></td> -->
     *   <tr><td>Mean Sea Level</td>                     <td>Datum</td>       <td>{@link #MEAN_SEA_LEVEL}</td></tr>
     *   <tr><td>Mean Sea Level depth</td>               <td>CRS</td>         <td>{@link #DEPTH}</td></tr>
     *   <tr><td>Mean Sea Level height</td>              <td>CRS</td>         <td>{@link #MEAN_SEA_LEVEL}</td></tr>
     *   <tr><td>NAVD88 height</td>                      <td>CRS</td>         <td>{@link #NAVD88}</td></tr>
     *   <tr><td>North American Vertical Datum 1988</td> <td>Datum</td>       <td>{@link #NAVD88}</td></tr>
     *   <tr><td>Other surface</td>                      <td>CRS, Datum</td>  <td>{@link #OTHER_SURFACE}</td></tr>
     * </table></blockquote>
     *
     * <div class="note"><b>Note:</b>
     * We do not provide a {@code GEOIDAL} value because its definition depends on the realization epoch.
     * For example EGM84, EGM96 and EGM2008 are applications of three different geoid models on the WGS 84 ellipsoid.
     * The {@link #MEAN_SEA_LEVEL} value can be used instead as an approximation of geoidal heights.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.4
     * @version 0.7
     * @module
     *
     * @see org.apache.sis.referencing.factory.CommonAuthorityFactory
     */
    public static enum Vertical {
        /**
         * Height measured by atmospheric pressure in hectopascals (hPa).
         * Hectopascals are the units of measurement used by the worldwide meteorological community.
         * The datum is not specific to any location or epoch.
         *
         * @see VerticalDatumType#BAROMETRIC
         */
        BAROMETRIC(false, Vocabulary.Keys.BarometricAltitude, Vocabulary.Keys.ConstantPressureSurface),

        /**
         * Height measured above the Mean Sea Level (MSL) in metres. Can be used as an approximation of geoidal heights
         * (height measured above an equipotential surface), except that MSL are not specific to any location or epoch.
         *
         * <blockquote><table class="compact" summary="Mean Sea Level properties.">
         *   <tr><th>EPSG identifiers:</th>         <td>5714 &nbsp;(<i>datum:</i> 5100)</td></tr>
         *   <tr><th>Primary names:</th>            <td>"MSL height" &nbsp;(<i>datum:</i> "Mean Sea Level")</td></tr>
         *   <tr><th>Abbreviations or aliases:</th> <td>"mean sea level height" &nbsp;(<i>datum:</i> "MSL")</td></tr>
         *   <tr><th>Direction:</th>                <td>{@link AxisDirection#UP}</td></tr>
         *   <tr><th>Unit:</th>                     <td>{@link SI#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see VerticalDatumType#GEOIDAL
         */
        MEAN_SEA_LEVEL(true, (short) 5714, (short) 5100),

        /**
         * Depth measured below the Mean Sea Level (MSL) in metres.
         *
         * <blockquote><table class="compact" summary="Depth properties.">
         *   <tr><th>EPSG identifiers:</th>         <td>5715 &nbsp;(<i>datum:</i> 5100)</td></tr>
         *   <tr><th>Primary names:</th>            <td>"MSL depth" &nbsp;(<i>datum:</i> "Mean Sea Level")</td></tr>
         *   <tr><th>Abbreviations or aliases:</th> <td>"mean sea level depth" &nbsp;(<i>datum:</i> "MSL")</td></tr>
         *   <tr><th>Direction:</th>                <td>{@link AxisDirection#DOWN}</td></tr>
         *   <tr><th>Unit:</th>                     <td>{@link SI#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see VerticalDatumType#GEOIDAL
         */
        DEPTH(true, (short) 5715, (short) 5100),

        /**
         * North American Vertical Datum 1988 height.
         *
         * <blockquote><table class="compact" summary="Mean Sea Level properties.">
         *   <tr><th>WMS identifier:</th>           <td>CRS:88</td></tr>
         *   <tr><th>EPSG identifiers:</th>         <td>5703 &nbsp;(<i>datum:</i> 5103)</td></tr>
         *   <tr><th>Primary names:</th>            <td>"NAVD88 height" &nbsp;(<i>datum:</i> "North American Vertical Datum 1988")</td></tr>
         *   <tr><th>Abbreviations or aliases:</th> <td>" North American Vertical Datum of 1988 height (m)" &nbsp;(<i>datum:</i> "NAVD88")</td></tr>
         *   <tr><th>Direction:</th>                <td>{@link AxisDirection#UP}</td></tr>
         *   <tr><th>Unit:</th>                     <td>{@link SI#METRE}</td></tr>
         * </table></blockquote>
         *
         * @see CommonCRS#NAD83
         *
         * @since 0.7
         */
        NAVD88(true, (short) 5703, (short) 5103),

        /**
         * Height measured along the normal to the ellipsoid used in the definition of horizontal datum.
         * The unit of measurement is metres.
         *
         * <p><b>Ellipsoidal height is not part of ISO 19111 international standard.</b>
         * Such vertical CRS is usually not recommended since ellipsoidal heights make little sense without
         * their (<var>latitude</var>, <var>longitude</var>) locations. The ISO specification defines instead
         * three-dimensional {@code GeographicCRS} for that reason. Users are encouraged to avoid this orphan
         * ellipsoidal height as much as possible.</p>
         */
        ELLIPSOIDAL(false, Vocabulary.Keys.EllipsoidalHeight, Vocabulary.Keys.Ellipsoid),

        /**
         * Height measured above other kind of surface, for example a geological feature.
         * The unit of measurement is metres.
         *
         * @see VerticalDatumType#OTHER_SURFACE
         */
        OTHER_SURFACE(false, Vocabulary.Keys.Height, Vocabulary.Keys.OtherSurface);

        /**
         * {@code true} if {@link #crs} and {@link #datum} are EPSG codes, or {@code false} if
         * they are resource keys for the name as one of the {@code Vocabulary.Keys} constants.
         */
        final boolean isEPSG;

        /**
         * The EPSG code for the CRS or the resource keys, depending on {@link #isEPSG} value.
         */
        final short crs;

        /**
         * The EPSG code for the datum or the resource keys, depending on {@link #isEPSG} value.
         */
        final short datum;

        /**
         * The cached object. This is initially {@code null}, then set to various kind of objects depending
         * on which method has been invoked. The kind of object stored in this field may change during the
         * application execution.
         */
        private transient volatile IdentifiedObject cached;

        /**
         * Creates a new enumeration value of the given name.
         *
         * <div class="note"><b>Note:</b>
         * This constructor does not expect {@link VerticalDatumType} constant in order to avoid too
         * early class initialization. In particular, we do not want early dependency to the SIS-specific
         * {@code VerticalDatumTypes.ELLIPSOIDAL} constant.</div>
         */
        private Vertical(final boolean isEPSG, final short crs, final short datum) {
            this.isEPSG = isEPSG;
            this.crs    = crs;
            this.datum  = datum;
        }

        /**
         * Registers a listeners to be invoked when the classpath changed.
         * This will clear the cache, since the factories may have changed.
         */
        static {
            SystemListener.add(new SystemListener(Modules.REFERENCING) {
                @Override protected void classpathChanged() {
                    for (final Vertical e : values()) {
                        e.clear();
                    }
                }
            });
        }

        /**
         * Invoked by when the cache needs to be cleared after a classpath change.
         */
        synchronized void clear() {
            cached = null;
        }

        /**
         * Returns the coordinate reference system associated to this vertical object.
         * The following table summarizes the CRS known to this class,
         * together with an enumeration value that can be used for fetching that CRS:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used vertical CRS</caption>
         *   <tr><th>Name or alias</th>             <th>Enum</th>                        <th>EPSG</th></tr>
         *   <tr><td>Barometric altitude</td>       <td>{@link #BAROMETRIC}</td>         <td></td></tr>
         *   <!-- <s>Ellipsoidal height</s> intentionally omitted -->
         *   <tr><td>Mean Sea Level depth</td>      <td>{@link #DEPTH}</td>              <td>5715</td></tr>
         *   <tr><td>Mean Sea Level height</td>     <td>{@link #MEAN_SEA_LEVEL}</td>     <td>5714</td></tr>
         *   <tr><td>Other surface</td>             <td>{@link #OTHER_SURFACE}</td>      <td></td></tr>
         * </table></blockquote>
         *
         * @return The CRS associated to this enum.
         *
         * @see DefaultVerticalCRS
         */
        public VerticalCRS crs() {
            VerticalCRS object = crs(cached);
            if (object == null) {
                synchronized (this) {
                    object = crs(cached);
                    if (object == null) {
                        if (isEPSG) {
                            final CRSAuthorityFactory factory = crsFactory();
                            if (factory != null) try {
                                cached = object = factory.createVerticalCRS(String.valueOf(crs));
                                return object;
                            } catch (FactoryException e) {
                                failure(this, "crs", e, crs);
                            }
                            object = StandardDefinitions.createVerticalCRS(crs, datum());
                        } else {
                            final VerticalCS cs = cs();
                            object = new DefaultVerticalCRS(IdentifiedObjects.getProperties(cs, EXCLUDE), datum(), cs);
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Creates the coordinate system associated to this vertical object.
         * This method does not cache the coordinate system.
         */
        private VerticalCS cs() {
            final Map<String,?> properties = properties(crs);
            final Unit<?> unit;
            switch (this) {
                default: {
                    unit = SI.METRE;
                    break;
                }
                case BAROMETRIC: {
                    unit = SI.MetricPrefix.HECTO(SI.PASCAL);
                    break;
                }
            }
            return new DefaultVerticalCS(properties,
                    new DefaultCoordinateSystemAxis(properties, "h", AxisDirection.UP, unit));
        }

        /**
         * Returns the datum associated to this vertical object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used vertical datum</caption>
         *   <tr><th>Name or alias</th>             <th>Enum</th>                        <th>EPSG</th></tr>
         *   <tr><td>Barometric altitude</td>       <td>{@link #BAROMETRIC}</td>         <td></td></tr>
         *   <!-- <s>Ellipsoidal height</s> intentionally omitted -->
         *   <tr><td>Mean Sea Level</td>            <td>{@link #MEAN_SEA_LEVEL}</td>     <td>5100</td></tr>
         *   <tr><td>Other surface</td>             <td>{@link #OTHER_SURFACE}</td>      <td></td></tr>
         * </table></blockquote>
         *
         * @return The datum associated to this enum.
         *
         * @see DefaultVerticalDatum
         */
        public VerticalDatum datum() {
            VerticalDatum object = datum(cached);
            if (object == null) {
                synchronized (this) {
                    object = datum(cached);
                    if (object == null) {
                        if (isEPSG) {
                            final DatumAuthorityFactory factory = datumFactory();
                            if (factory != null) try {
                                cached = object = factory.createVerticalDatum(String.valueOf(datum));
                                return object;
                            } catch (FactoryException e) {
                                failure(this, "datum", e, datum);
                            }
                            object = StandardDefinitions.createVerticalDatum(datum);
                        } else {
                            object = new DefaultVerticalDatum(properties(datum), VerticalDatumType.valueOf(name()));
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Returns the vertical CRS associated to the given object, or {@code null} if none.
         */
        private static VerticalCRS crs(final IdentifiedObject object) {
            return (object instanceof VerticalCRS) ? (VerticalCRS) object : null;
        }

        /**
         * Returns the datum associated to the given object, or {@code null} if none.
         */
        private static VerticalDatum datum(final IdentifiedObject object) {
            if (object instanceof VerticalDatum) {
                return (VerticalDatum) object;
            }
            if (object instanceof VerticalCRS) {
                return ((VerticalCRS) object).getDatum();
            }
            return null;
        }
    }




    /**
     * Frequently-used temporal CRS and datum that are guaranteed to be available in SIS.
     *
     * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code Temporal}
     * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
     * (e.g. the application is running in a container environment and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a temporal Coordinate Reference System using the Julian calendar:</p>
     *
     * {@preformat java
     *   TemporalCRS crs = CommonCRS.Temporal.JULIAN.crs();
     * }
     *
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
     *   <caption>Temporal objects accessible by enumeration constants</caption>
     *   <tr><th>Name or alias</th>    <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Dublin Julian</td>    <td>CRS, Datum</td>  <td>{@link #DUBLIN_JULIAN}</td></tr>
     *   <tr><td>Java time</td>        <td>CRS</td>         <td>{@link #JAVA}</td></tr>
     *   <tr><td>Julian</td>           <td>CRS, Datum</td>  <td>{@link #JULIAN}</td></tr>
     *   <tr><td>Modified Julian</td>  <td>CRS, Datum</td>  <td>{@link #MODIFIED_JULIAN}</td></tr>
     *   <tr><td>Truncated Julian</td> <td>CRS, Datum</td>  <td>{@link #TRUNCATED_JULIAN}</td></tr>
     *   <tr><td>Unix/POSIX time</td>  <td>CRS, Datum</td>  <td>{@link #UNIX}</td></tr>
     * </table></blockquote>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.4
     * @version 0.4
     * @module
     */
    public static enum Temporal {
        /**
         * Time measured as days since January 1st, 4713 BC at 12:00 UTC.
         */
        JULIAN(Vocabulary.Keys.Julian, -2440588 * (24*60*60*1000L) + (12*60*60*1000L)),

        /**
         * Time measured as days since November 17, 1858 at 00:00 UTC.
         * A <cite>Modified Julian day</cite> (MJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code MJD = JD − 2400000.5}.
         */
        MODIFIED_JULIAN(Vocabulary.Keys.ModifiedJulian, -40587 * (24*60*60*1000L)),

        /**
         * Time measured as days since May 24, 1968 at 00:00 UTC.
         * This epoch was introduced by NASA for the space program.
         * A <cite>Truncated Julian day</cite> (TJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code TJD = JD − 2440000.5}.
         */
        TRUNCATED_JULIAN(Vocabulary.Keys.TruncatedJulian, -587 * (24*60*60*1000L)),

        /**
         * Time measured as days since December 31, 1899 at 12:00 UTC.
         * A <cite>Dublin Julian day</cite> (DJD) is defined relative to
         * <cite>Julian day</cite> (JD) as {@code DJD = JD − 2415020}.
         */
        DUBLIN_JULIAN(Vocabulary.Keys.DublinJulian, -25568 * (24*60*60*1000L) + (12*60*60*1000L)),

        /**
         * Time measured as seconds since January 1st, 1970 at 00:00 UTC.
         */
        UNIX(Vocabulary.Keys.Time_1, 0),

        /**
         * Time measured as milliseconds since January 1st, 1970 at 00:00 UTC.
         */
        JAVA(Vocabulary.Keys.Time_1, 0);

        /**
         * The resource keys for the name as one of the {@code Vocabulary.Keys} constants.
         */
        private final short key;

        /**
         * The date and time origin of this temporal datum.
         */
        private final long epoch;

        /**
         * The cached object. This is initially {@code null}, then set to various kind of objects depending
         * on which method has been invoked. The kind of object stored in this field may change during the
         * application execution.
         */
        private transient volatile IdentifiedObject cached;

        /**
         * Creates a new enumeration value of the given name with time counted since the given epoch.
         */
        private Temporal(final short name, final long epoch) {
            this.key   = name;
            this.epoch = epoch;
        }

        /**
         * Registers a listeners to be invoked when the classpath changed.
         * This will clear the cache, since the factories may have changed.
         */
        static {
            SystemListener.add(new SystemListener(Modules.REFERENCING) {
                @Override protected void classpathChanged() {
                    for (final Temporal e : values()) {
                        e.clear();
                    }
                }
            });
        }

        /**
         * Invoked by when the cache needs to be cleared after a classpath change.
         */
        synchronized void clear() {
            cached = null;
        }

        /**
         * Returns the coordinate reference system associated to this temporal object.
         * The following table summarizes the CRS known to this class,
         * together with an enumeration value that can be used for fetching that CRS:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used temporal CRS</caption>
         *   <tr><th>Name or alias</th>      <th>Enum</th></tr>
         *   <tr><td>Dublin Julian</td>      <td>{@link #DUBLIN_JULIAN}</td></tr>
         *   <tr><td>Julian</td>             <td>{@link #JULIAN}</td></tr>
         *   <tr><td>Modified Julian</td>    <td>{@link #MODIFIED_JULIAN}</td></tr>
         *   <tr><td>Truncated Julian</td>   <td>{@link #TRUNCATED_JULIAN}</td></tr>
         *   <tr><td>Unix/POSIX or Java</td> <td>{@link #UNIX}</td></tr>
         * </table></blockquote>
         *
         * @return The CRS associated to this enum.
         *
         * @see DefaultTemporalCRS
         */
        public TemporalCRS crs() {
            TemporalCRS object = crs(cached);
            if (object == null) {
                synchronized (this) {
                    object = crs(cached);
                    if (object == null) {
                        final TemporalDatum datum = datum();
                        object = new DefaultTemporalCRS(IdentifiedObjects.getProperties(datum, EXCLUDE), datum, cs());
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Creates the coordinate system associated to this temporal object.
         * This method does not cache the coordinate system.
         */
        @SuppressWarnings("fallthrough")
        private TimeCS cs() {
            final Map<String,?> cs, axis;
            Unit<Duration> unit = SI.SECOND;
            switch (this) {
                default: {
                    // Share the coordinate system created for truncated Julian.
                    return TRUNCATED_JULIAN.crs().getCoordinateSystem();
                }
                case TRUNCATED_JULIAN: {
                    unit = NonSI.DAY;
                    // Fall through
                }
                case UNIX: {
                    // Share the NamedIdentifier created for Java time.
                    final TimeCS share = JAVA.crs().getCoordinateSystem();
                    cs   = IdentifiedObjects.getProperties(share, EXCLUDE);
                    axis = IdentifiedObjects.getProperties(share.getAxis(0), EXCLUDE);
                    break;
                }
                case JAVA: {
                    // Create all properties for a new coordinate system.
                    cs   = properties(Vocabulary.Keys.Temporal);
                    axis = properties(Vocabulary.Keys.Time);
                    unit = Units.MILLISECOND;
                    break;
                }
            }
            return new DefaultTimeCS(cs, new DefaultCoordinateSystemAxis(axis, "t", AxisDirection.FUTURE, unit));
        }

        /**
         * Returns the datum associated to this temporal object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <caption>Commonly used temporal datum</caption>
         *   <tr><th>Name or alias</th>      <th>Enum</th></tr>
         *   <tr><td>Dublin Julian</td>      <td>{@link #DUBLIN_JULIAN}</td></tr>
         *   <tr><td>Julian</td>             <td>{@link #JULIAN}</td></tr>
         *   <tr><td>Modified Julian</td>    <td>{@link #MODIFIED_JULIAN}</td></tr>
         *   <tr><td>Truncated Julian</td>   <td>{@link #TRUNCATED_JULIAN}</td></tr>
         *   <tr><td>Unix/POSIX or Java</td> <td>{@link #UNIX}</td></tr>
         * </table></blockquote>
         *
         * @return The datum associated to this enum.
         *
         * @see DefaultTemporalDatum
         */
        public TemporalDatum datum() {
            TemporalDatum object = datum(cached);
            if (object == null) {
                synchronized (this) {
                    object = datum(cached);
                    if (object == null) {
                        if (this == UNIX) {
                            object = JAVA.datum(); // Share the same instance for UNIX and JAVA.
                        } else {
                            final Map<String,?> properties;
                            if (key == Vocabulary.Keys.Time_1) {
                                properties = properties(Vocabulary.formatInternational(
                                        key, (this == JAVA) ? "Java" : "Unix/POSIX"));
                            } else {
                                properties = properties(key);
                            }
                            object = new DefaultTemporalDatum(properties, new Date(epoch));
                        }
                        cached = object;
                    }
                }
            }
            return object;
        }

        /**
         * Returns the temporal CRS associated to the given object, or {@code null} if none.
         */
        private static TemporalCRS crs(final IdentifiedObject object) {
            return (object instanceof TemporalCRS) ? (TemporalCRS) object : null;
        }

        /**
         * Returns the datum associated to the given object, or {@code null} if none.
         */
        private static TemporalDatum datum(final IdentifiedObject object) {
            if (object instanceof TemporalDatum) {
                return (TemporalDatum) object;
            }
            if (object instanceof TemporalCRS) {
                return ((TemporalCRS) object).getDatum();
            }
            return null;
        }
    }

    /**
     * Puts the name for the given key in a map of properties to be given to object constructors.
     *
     * @param  key A constant from {@link org.apache.sis.util.resources.Vocabulary.Keys}.
     * @return The properties to give to the object constructor.
     */
    static Map<String,?> properties(final short key) {
        return properties(Vocabulary.formatInternational(key));
    }

    /**
     * Puts the given name in a map of properties to be given to object constructors.
     */
    static Map<String,?> properties(final InternationalString name) {
        return singletonMap(NAME_KEY, new NamedIdentifier(null, name));
    }

    /**
     * Returns the same properties than the given object, except for the identifier which is set to the given code.
     */
    private static Map<String,?> properties(final IdentifiedObject template, final short code) {
        final Map<String,Object> properties = new HashMap<String,Object>(IdentifiedObjects.getProperties(template, EXCLUDE));
        properties.put(GeographicCRS.IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, String.valueOf(code)));
        return properties;
    }

    /**
     * Returns the EPSG factory to use for creating CRS, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    static CRSAuthorityFactory crsFactory() {
        if (!EPSGFactoryFallback.FORCE_HARDCODED) {
            final AuthorityFactory factory = AuthorityFactories.EPSG();
            if (!(factory instanceof EPSGFactoryFallback)) {
                return (CRSAuthorityFactory) factory;
            }
        }
        return null;
    }

    /**
     * Returns the EPSG factory to use for creating coordinate systems, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    static CSAuthorityFactory csFactory() {
        if (!EPSGFactoryFallback.FORCE_HARDCODED) {
            final AuthorityFactory factory = AuthorityFactories.EPSG();
            if (!(factory instanceof EPSGFactoryFallback)) {
                return (CSAuthorityFactory) factory;
            }
        }
        return null;
    }

    /**
     * Returns the EPSG factory to use for creating datum, ellipsoids and prime meridians, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    static DatumAuthorityFactory datumFactory() {
        if (!EPSGFactoryFallback.FORCE_HARDCODED) {
            final AuthorityFactory factory = AuthorityFactories.EPSG();
            if (!(factory instanceof EPSGFactoryFallback)) {
                return (DatumAuthorityFactory) factory;
            }
        }
        return null;
    }

    /**
     * Invoked when a factory failed to create an object.
     * After invoking this method, the caller will fallback on hard-coded values.
     */
    static void failure(final Object caller, final String method, final FactoryException e, final int code) {
        String message = Errors.format(Errors.Keys.CanNotInstantiate_1, "EPSG:" + code);
        message = Exceptions.formatChainedMessages(null, message, e);
        final LogRecord record = new LogRecord(Level.WARNING, message);
        if (!(e instanceof UnavailableFactoryException) || !AuthorityFactories.failure((UnavailableFactoryException) e)) {
            // Append the stack trace only if the exception is the the one we expect when the factory is not available.
            record.setThrown(e);
        }
        record.setLoggerName(Loggers.CRS_FACTORY);
        Logging.log(caller.getClass(), method, record);
    }
}
