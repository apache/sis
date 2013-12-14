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
import java.util.HashMap;
import java.util.Date;
import java.util.Locale;
import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.util.resources.Vocabulary;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;


/**
 * Frequently-used geodetic CRS and datum that are guaranteed to be available in SIS.
 * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
 * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
 * then {@code GeodeticObjects} fallback on hard-coded values. Consequently, those methods never return {@code null}.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code GeodeticObjects}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
 *
 * <p><b>Example:</b> the following code fetches a geographic Coordinate Reference System using
 * (<var>longitude</var>, <var>latitude</var>) axis order on the {@link #WGS84} geodetic datum:</p>
 *
 * {@preformat java
 *   GeographicCRS crs = GeodeticObjects.WGS84.crs(true);
 * }
 *
 * For each enumeration value, the name of the CRS, datum and ellipsoid objects may or may not be the same.
 * Below is an alphabetical list of object names available in this enumeration:
 *
 * <blockquote><table class="sis">
 *   <tr><th>Name or alias</th>                                     <th>Object type</th>           <th>Enumeration value</th></tr>
 *   <tr><td>Clarke 1866</td>                                       <td>Ellipsoid</td>             <td>{@link #NAD27}</td></tr>
 *   <tr><td>European Datum 1950 (ED50)</td>                        <td>Datum</td>                 <td>{@link #ED50}</td></tr>
 *   <tr><td>European Terrestrial Reference Frame (ETRS) 1989</td>  <td>Datum</td>                 <td>{@link #ETRS89}</td></tr>
 *   <tr><td>European Terrestrial Reference System (ETRF) 1989</td> <td>Datum</td>                 <td>{@link #ETRS89}</td></tr>
 *   <tr><td>Greenwich</td>                                         <td>Prime meridian</td>        <td>{@link #WGS84}, {@link #WGS72}, {@link #ETRS89}, {@link #NAD83}, {@link #NAD27}, {@link #ED50}, {@link #SPHERE}</td></tr>
 *   <tr><td>GRS 1980</td>                                          <td>Ellipsoid</td>             <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
 *   <tr><td>GRS 1980 Authalic Sphere</td>                          <td>Ellipsoid</td>             <td>{@link #SPHERE}</td></tr>
 *   <tr><td>Hayford 1909</td>                                      <td>Ellipsoid</td>             <td>{@link #ED50}</td></tr>
 *   <tr><td>International 1924</td>                                <td>Ellipsoid</td>             <td>{@link #ED50}</td></tr>
 *   <tr><td>International 1979</td>                                <td>Ellipsoid</td>             <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
 *   <tr><td>North American Datum 1927</td>                         <td>Datum</td>                 <td>{@link #NAD27}</td></tr>
 *   <tr><td>North American Datum 1983</td>                         <td>Datum</td>                 <td>{@link #NAD83}</td></tr>
 *   <tr><td>NWL 10D</td>                                           <td>Ellipsoid</td>             <td>{@link #WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1972</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1984</td>                  <td>CRS, datum, ellipsoid</td> <td>{@link #WGS84}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public enum GeodeticObjects {
    /**
     * World Geodetic System 1984.
     * This is the default CRS for most {@code org.apache.sis} packages.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>WMS identifier:</th>          <td>CRS:84</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4326 &nbsp;(<i>datum:</i> 6326, &nbsp;<i>ellipsoid:</i> 7030)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"WGS 84" &nbsp;(<i>datum:</i> "World Geodetic System 1984")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "WGS 84", &nbsp;<i>ellipsoid:</i> "WGS84")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257223563 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     */
    WGS84((short) 7030, (short) 6326),

    /**
     * World Geodetic System 1972.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>EPSG identifiers:</th>        <td>4322 &nbsp;(<i>datum:</i> 6322, &nbsp;<i>ellipsoid:</i> 7043)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"WGS 72" &nbsp;(<i>datum:</i> "World Geodetic System 1972")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "WGS 72", &nbsp;<i>ellipsoid:</i> "NWL 10D")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378135</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356751 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.26 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     */
    WGS72((short) 7043, (short) 6322),

    /**
     * European Terrestrial Reference System 1989.
     * The ellipsoid is <cite>"GRS 1980"</cite>, also known as <cite>"International 1979"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>EPSG identifiers:</th>        <td>4258 &nbsp;(<i>datum:</i> 6258, &nbsp;<i>ellipsoid:</i> 7019)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"ETRS89" &nbsp;(<i>datum:</i> "European Terrestrial Reference System 1989", &nbsp;<i>ellipsoid:</i> "GRS 1980")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>"ETRF89", "EUREF89", "ETRS89-GRS80" &nbsp;(<i>ellipsoid:</i> "International 1979")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     *
     * {@note <cite>NAD83</cite> uses the same ellipsoid for a different datum.
     *        The <cite>Web Map Server</cite> <code>"CRS:83"</code> authority code uses the NAD83 datum,
     *        while the <code>"IGNF:MILLER"</code> authority code uses the GRS80 datum.}
     */
    ETRS89((short) 7019, (short) 6258),

    /**
     * North American Datum 1983.
     * The ellipsoid is <cite>"GRS 1980"</cite>, also known as <cite>"International 1979"</cite>.
     * This ellipsoid is very close, but not identical, to the {@linkplain #WGS84} one.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>WMS identifier:</th>          <td>CRS:83</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4269 &nbsp;(<i>datum:</i> 6269, &nbsp;<i>ellipsoid:</i> 7019)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"NAD83" &nbsp;(<i>datum:</i> "North American Datum 1983", &nbsp;<i>ellipsoid:</i> "GRS 1980")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>"NAD83 (1986)" &nbsp;(<i>ellipsoid:</i> "International 1979")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378137</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356752 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>298.257222101 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     *
     * {@note <cite>ETRS89</cite> uses the same ellipsoid for a different datum.
     *        The <cite>Web Map Server</cite> <code>"CRS:83"</code> authority code uses the NAD83 datum,
     *        while the <code>"IGNF:MILLER"</code> authority code uses the GRS80 datum.}
     */
    NAD83((short) 7019, (short) 6269),

    /**
     * North American Datum 1927.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>WMS identifier:</th>          <td>CRS:27</td></tr>
     *   <tr><th>EPSG identifiers:</th>        <td>4267 &nbsp;(<i>datum:</i> 6267, &nbsp;<i>ellipsoid:</i> 7008)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"NAD27" &nbsp;(<i>datum:</i> "North American Datum 1927", &nbsp;<i>ellipsoid:</i> "Clarke 1866")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "NAD27")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378206.4</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356583.8 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     */
    NAD27((short) 7008, (short) 6267),

    /**
     * European Datum 1950.
     *
     * <blockquote><table class="compact" style="text-align:left">
     *   <tr><th>EPSG identifiers:</th>        <td>4230 &nbsp;(<i>datum:</i> 6230, &nbsp;<i>ellipsoid:</i> 7022)</td></tr>
     *   <tr><th>Primary names:</th>           <td>"ED50" &nbsp;(<i>datum:</i> "European Datum 1950", &nbsp;<i>ellipsoid:</i> "International 1924")</td></tr>
     *   <tr><th>Abbreviations or aliases:</th><td>(<i>datum:</i> "ED50", <i>ellipsoid:</i> "Hayford 1909")</td></tr>
     *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
     *   <tr><th>Semi-major axis length:</th>  <td>6378388</td></tr>
     *   <tr><th>Semi-minor axis length:</th>  <td>6356912 <i>(approximative)</i></td></tr>
     *   <tr><th>Inverse flattening:</th>      <td>297 <i>(definitive)</i></td></tr>
     *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
     * </table></blockquote>
     */
    ED50((short) 7022, (short) 6230),

    /**
     * Unspecified datum based upon the GRS 1980 Authalic Sphere. Spheres use a simpler algorithm for
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#orthodromicDistance
     * orthodromic distance computation}, which may be faster and more robust.
     *
     * <blockquote><table class="compact" style="text-align:left">
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
    SPHERE((short) 7048, (short) 6047);

    /**
     * The EPSG code of the ellipsoid.
     */
    final short ellipsoid;

    /**
     * The EPSG code of the datum.
     */
    final short datum;

    /**
     * The cached object. This is initially {@code null}, then set to various kind of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    private transient volatile IdentifiedObject cached;

    /**
     * Creates a new constant for the given EPSG or SIS codes.
     *
     * @param ellipsoid The EPSG code for the ellipsoid.
     * @param datum     The EPSG code for the datum.
     */
    private GeodeticObjects(final short ellipsoid, final short datum) {
        this.ellipsoid = ellipsoid;
        this.datum     = datum;
    }

    /**
     * Registers a listeners to be invoked when the classpath changed.
     * This will clear the cache, since the EPSG database may have changed.
     */
    static {
        new StandardObjects(GeodeticObjects.class); // Constructor registers itself.
    }

    /**
     * Invoked by {@link StandardObjects#classpathChanged()} when the cache needs to be cleared.
     */
    synchronized void clear() {
        cached = null;
    }

    /**
     * Returns the geodetic datum associated to this geodetic object.
     * The following table summarizes the datums known to this class,
     * together with an enumeration value that can be used for fetching that datum:
     *
     * <blockquote><table class="sis">
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
     * @return The geodetic datum associated to this constant.
     *
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     * @see DatumAuthorityFactory#createGeodeticDatum(String)
     */
    public GeodeticDatum datum() {
        GeodeticDatum object = datum(cached);
        if (object == null) {
            synchronized (this) {
                object = datum(cached);
                if (object == null) {
                    final DatumAuthorityFactory factory = StandardObjects.datumFactory();
                    if (factory != null) try {
                        cached = object = factory.createGeodeticDatum(String.valueOf(datum));
                        return object;
                    } catch (FactoryException e) {
                        StandardObjects.failure(this, "datum", e);
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
     *   <tr><th>Name or alias</th>                    <th>Enum</th>            <th>EPSG</th></tr>
     *   <tr><td>Clarke 1866</td>                      <td>{@link #NAD27}</td>  <td>7008</td></tr>
     *   <tr><td>GRS 1980 Authalic Sphere</td>         <td>{@link #SPHERE}</td> <td>7048</td></tr>
     *   <tr><td>International 1924</td>               <td>{@link #ED50}</td>   <td>7022</td></tr>
     *   <tr><td>International 1979 / GRS 1980</td>    <td>{@link #ETRS89}</td> <td>7019</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1972</td> <td>{@link #WGS72}</td>  <td>7043</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1984</td> <td>{@link #WGS84}</td>  <td>7030</td></tr>
     * </table></blockquote>
     *
     * @return The ellipsoid associated to this constant.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     * @see DatumAuthorityFactory#createEllipsoid(String)
     */
    public Ellipsoid ellipsoid() {
        Ellipsoid object = ellipsoid(cached);
        if (object == null) {
            synchronized (this) {
                object = ellipsoid(cached);
                if (object == null) {
                    if (this == NAD83) {
                        object = ETRS89.ellipsoid(); // Share the same instance for NAD83 and ETRS89.
                    } else {
                        final DatumAuthorityFactory factory = StandardObjects.datumFactory();
                        if (factory != null) try {
                            cached = object = factory.createEllipsoid(String.valueOf(ellipsoid));
                            return object;
                        } catch (FactoryException e) {
                            StandardObjects.failure(this, "ellipsoid", e);
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
     *   <tr><th>Name or alias</th> <th>Enum</th>           <th>EPSG</th></tr>
     *   <tr><td>Greenwich</td>     <td>{@link #WGS84}</td> <td>8901</td></tr>
     * </table></blockquote>
     *
     * @return The prime meridian associated to this constant.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     * @see DatumAuthorityFactory#createPrimeMeridian(String)
     */
    public PrimeMeridian primeMeridian() {
        PrimeMeridian object = primeMeridian(cached);
        if (object == null) {
            synchronized (this) {
                object = primeMeridian(cached);
                if (object == null) {
                    if (this != WGS84) {
                        object = WGS84.primeMeridian(); // Share the same instance for all constants.
                    } else {
                        final DatumAuthorityFactory factory = StandardObjects.datumFactory();
                        if (factory != null) try {
                            cached = object = factory.createPrimeMeridian(StandardDefinitions.GREENWICH);
                            return object;
                        } catch (FactoryException e) {
                            StandardObjects.failure(this, "primeMeridian", e);
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




    /**
     * Frequently-used vertical CRS and datum that are guaranteed to be available in SIS.
     * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
     * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
     * then {@code Vertical} fallback on hard-coded values. Consequently, those methods never return {@code null}.
     *
     * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code Vertical}
     * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
     * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a vertical Coordinate Reference System for height above the geoid:</p>
     *
     * {@preformat java
     *   VerticalCRS crs = GeodeticObjects.Vertical.GEOIDAL.crs();
     * }
     *
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
     *   <tr><th>Name or alias</th>       <th>Object type</th> <th>Enumeration value</th></tr>
     *   <tr><td>Barometric altitude</td> <td>CRS, Datum</td>  <td>{@link #BAROMETRIC}</td></tr>
     *   <tr><td>Geoidal height</td>      <td>CRS, Datum</td>  <td>{@link #GEOIDAL}</td></tr>
     *   <tr><td>Ellipsoidal height</td>  <td>CRS, Datum</td>  <td>{@link #ELLIPSOIDAL}</td></tr>
     *   <tr><td>Other surface</td>       <td>CRS, Datum</td>  <td>{@link #OTHER_SURFACE}</td></tr>
     * </table></blockquote>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.4
     * @version 0.4
     * @module
     */
    public static enum Vertical {
        /**
         * Height measured by atmospheric pressure.
         *
         * @see VerticalDatumType#BAROMETRIC
         */
        BAROMETRIC(Vocabulary.Keys.BarometricAltitude),

        /**
         * Height measured above an equipotential surface.
         *
         * @see VerticalDatumType#GEOIDAL
         */
        GEOIDAL(Vocabulary.Keys.Geoidal),

        /**
         * Height measured along the normal to the ellipsoid used in the definition of horizontal datum.
         *
         * <p><b>This datum is not part of ISO 19111 international standard.</b>
         * Usage of this datum is generally not recommended since ellipsoidal heights make little sense without
         * their (<var>latitude</var>, <var>longitude</var>) locations. The ISO specification defines instead
         * three-dimensional {@code GeographicCRS} for that reason. However Apache SIS provides this value
         * because it is sometime useful to temporarily express ellipsoidal heights independently from other
         * ordinate values.</p>
         */
        ELLIPSOIDAL(Vocabulary.Keys.Ellipsoidal),

        /**
         * Height measured above other kind of surface, for example a geological feature.
         *
         * @see VerticalDatumType#OTHER_SURFACE
         */
        OTHER_SURFACE(Vocabulary.Keys.OtherSurface);

        /**
         * The resource keys for the name as one of the {@code Vocabulary.Keys} constants.
         */
        private final short key;

        /**
         * The cached object. This is initially {@code null}, then set to various kind of objects depending
         * on which method has been invoked. The kind of object stored in this field may change during the
         * application execution.
         */
        private transient volatile IdentifiedObject cached;

        /**
         * Creates a new enumeration value of the given name.
         *
         * {@note This constructor does not expect <code>VerticalDatumType</code> constant in order to avoid too
         *        early class initialization. In particular, we do not want early dependency to the SIS-specific
         *        <code>VerticalDatumTypes.ELLIPSOIDAL</code> constant.}
         */
        private Vertical(final short name) {
            this.key = name;
        }

        /**
         * Returns the datum associated to this vertical object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <tr><th>Name or alias</th>       <th>Enum</th></tr>
         *   <tr><td>Barometric altitude</td> <td>{@link #BAROMETRIC}</td></tr>
         *   <tr><td>Geoidal height</td>      <td>{@link #GEOIDAL}</td></tr>
         *   <tr><td>Ellipsoidal height</td>  <td>{@link #ELLIPSOIDAL}</td></tr>
         *   <tr><td>Other surface</td>       <td>{@link #OTHER_SURFACE}</td></tr>
         * </table></blockquote>
         *
         * @return The datum associated to this constant.
         *
         * @see DefaultVerticalDatum
         * @see DatumAuthorityFactory#createVerticalDatum(String)
         */
        public VerticalDatum datum() {
            VerticalDatum object = datum(cached);
            if (object == null) {
                synchronized (this) {
                    object = datum(cached);
                    if (object == null) {
                        final Map<String,Object> properties = new HashMap<>(4);
                        final InternationalString name = Vocabulary.formatInternational(key);
                        properties.put(NAME_KEY,  name.toString(Locale.ROOT));
                        properties.put(ALIAS_KEY, name);
                        object = new DefaultVerticalDatum(properties, VerticalDatumType.valueOf(name()));
                        cached = object;
                    }
                }
            }
            return object;
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
     * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
     *
     * <p><b>Example:</b> the following code fetches a temporal Coordinate Reference System using the Julian calendar:</p>
     *
     * {@preformat java
     *   TemporalCRS crs = GeodeticObjects.Temporal.JULIAN.crs();
     * }
     *
     * Below is an alphabetical list of object names available in this enumeration:
     *
     * <blockquote><table class="sis">
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
         * Returns the datum associated to this temporal object.
         * The following table summarizes the datum known to this class,
         * together with an enumeration value that can be used for fetching that datum:
         *
         * <blockquote><table class="sis">
         *   <tr><th>Name or alias</th>      <th>Enum</th></tr>
         *   <tr><td>Dublin Julian</td>      <td>{@link #DUBLIN_JULIAN}</td></tr>
         *   <tr><td>Julian</td>             <td>{@link #JULIAN}</td></tr>
         *   <tr><td>Modified Julian</td>    <td>{@link #MODIFIED_JULIAN}</td></tr>
         *   <tr><td>Truncated Julian</td>   <td>{@link #TRUNCATED_JULIAN}</td></tr>
         *   <tr><td>Unix/POSIX or Java</td> <td>{@link #UNIX}</td></tr>
         * </table></blockquote>
         *
         * @return The datum associated to this constant.
         *
         * @see DefaultTemporalDatum
         * @see DatumAuthorityFactory#createTemporalDatum(String)
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
                            final Map<String,Object> properties;
                            properties = new HashMap<>(4);
                            final InternationalString name;
                            if (key == Vocabulary.Keys.Time_1) {
                                name = Vocabulary.formatInternational(key, this == JAVA ? "Java" : "Unix/POSIX");
                            } else {
                                name = Vocabulary.formatInternational(key);
                            }
                            properties.put(NAME_KEY,  name.toString(Locale.ROOT));
                            properties.put(ALIAS_KEY, name);
                            object = new DefaultTemporalDatum(properties, new Date(epoch));
                        }
                        cached = object;
                    }
                }
            }
            return object;
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
}
