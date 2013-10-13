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

import java.lang.reflect.Field;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.util.logging.Logging;


/**
 * Frequently-used referencing objects that are guaranteed to be available in SIS.
 * Methods in sub-classes are shortcuts for object definitions in the EPSG database.
 * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
 * then {@code StandardObjects} fallback on hard-coded values. Consequently, those methods never return {@code null}.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code StandardObjects}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
 *
 * <blockquote><font size="-1"><b>Example:</b> the following code fetches a Coordinate Reference System using
 * (<var>longitude</var>, <var>latitude</var>) axis order on the {@link Geodetic#WGS84 WGS84} geodetic datum:
 *
 * {@preformat java
 *   GeographicCRS crs = StandardObjects.Geodetic.WGS84.crs(true);
 * }
 * </font></blockquote>
 *
 * {@section Index}
 * <blockquote><table class="compact">
 *   <tr><td>Clarke 1866</td>                                       <td>{@link Geodetic#NAD27  Geodetic.NAD27}</td></tr>
 *   <tr><td>European Datum 1950 (ED50)</td>                        <td>{@link Geodetic#ED50   Geodetic.ED50}</td></tr>
 *   <tr><td>European Terrestrial Reference Frame (ETRS) 1989</td>  <td>{@link Geodetic#ETRS89 Geodetic.ETRS89}</td></tr>
 *   <tr><td>European Terrestrial Reference System (ETRF) 1989</td> <td>{@link Geodetic#ETRS89 Geodetic.ETRS89}</td></tr>
 *   <tr><td>Greenwich</td>                                         <td>{@link Geodetic#WGS84  Geodetic.WGS84}, {@link Geodetic#WGS72 WGS72}, {@link Geodetic#ETRS89 ETRS89}, {@link Geodetic#NAD83 NAD83}, {@link Geodetic#NAD27 NAD27}, {@link Geodetic#ED50 ED50}, {@link Geodetic#SPHERE SPHERE}</td></tr>
 *   <tr><td>GRS 1980</td>                                          <td>{@link Geodetic#ETRS89 Geodetic.ETRS89}, {@link Geodetic#NAD83 NAD83}</td></tr>
 *   <tr><td>Hayford 1909</td>                                      <td>{@link Geodetic#ED50   Geodetic.ED50}</td></tr>
 *   <tr><td>International 1924</td>                                <td>{@link Geodetic#ED50   Geodetic.ED50}</td></tr>
 *   <tr><td>International 1979</td>                                <td>{@link Geodetic#ETRS89 Geodetic.ETRS89}, {@link Geodetic#NAD83 NAD83}</td></tr>
 *   <tr><td>North American Datum 1927</td>                         <td>{@link Geodetic#NAD27  Geodetic.NAD27}</td></tr>
 *   <tr><td>North American Datum 1983</td>                         <td>{@link Geodetic#NAD83  Geodetic.NAD83}</td></tr>
 *   <tr><td>NWL 10D</td>                                           <td>{@link Geodetic#WGS72  Geodetic.WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1972</td>                  <td>{@link Geodetic#WGS72  Geodetic.WGS72}</td></tr>
 *   <tr><td>World Geodetic System (WGS) 1984</td>                  <td>{@link Geodetic#WGS84  Geodetic.WGS84}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public abstract class StandardObjects implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4441181475750580370L;

    /**
     * Invoked when the classpath changed. The listener will clear the cache of all constants.
     */
    static {
        SystemListener.add(new SystemListener(Modules.REFERENCING) {
            @Override protected void classpathChanged() {
                try {
                    for (final Class<?> c : StandardObjects.class.getClasses()) {
                        for (final Field field : c.getFields()) {
                            final StandardObjects s = (StandardObjects) field.get(null);
                            s.clear();
                        }
                    }
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e); // Should never happen.
                }
            }
        });
    }

    /**
     * The programmatic constant name.
     */
    private final String name;

    /**
     * The cached object. This is initially {@code null}, then set to various kind of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    transient volatile IdentifiedObject cached;

    /**
     * For sub-classes constructors only.
     *
     * @param name The programmatic constant name.
     */
    StandardObjects(final String name) {
        this.name = name;
    }

    /**
     * Invoked when the cache needs to be cleared.
     */
    synchronized void clear() {
        cached = null;
    }

    /**
     * Returns the EPSG factory to use for creating datum, ellipsoids and prime meridians, or {@code null} if none.
     * If this method returns {@code null}, then the caller will silently fallback on hard-coded values.
     */
    static DatumAuthorityFactory datumFactory() {
        return null; // TODO
    }

    /**
     * Invoked when a factory failed to create an object.
     * After invoking this method, then the caller will fallback on hard-coded values.
     */
    final void failure(final String method, final FactoryException e) {
        Logging.unexpectedException(getClass(), method, e);
    }

    /**
     * Returns a string representation of this constant.
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + '.' + name;
    }

    /**
     * Invoked on deserialization for resolving to the singleton constant.
     */
    final Object readResolve() throws ObjectStreamException {
        try {
            return getClass().getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            throw (InvalidObjectException) new InvalidObjectException(toString()).initCause(e);
        }
    }




    /**
     * Frequently-used geodetic CRS and datum that are guaranteed to be available in SIS.
     * Methods in this class are shortcuts for object definitions in the EPSG database.
     * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
     * then {@code StandardObjects} fallback on hard-coded values. Consequently, those methods never return {@code null}.
     *
     * {@section Index}
     * <blockquote><table class="compact">
     *   <tr><td>Clarke 1866</td>                                       <td>{@link #NAD27}</td></tr>
     *   <tr><td>European Datum 1950 (ED50)</td>                        <td>{@link #ED50}</td></tr>
     *   <tr><td>European Terrestrial Reference Frame (ETRS) 1989</td>  <td>{@link #ETRS89}</td></tr>
     *   <tr><td>European Terrestrial Reference System (ETRF) 1989</td> <td>{@link #ETRS89}</td></tr>
     *   <tr><td>Greenwich</td>                                         <td>{@link #WGS84}, {@link #WGS72}, {@link #ETRS89}, {@link #NAD83}, {@link #NAD27}, {@link #ED50}, {@link #SPHERE}</td></tr>
     *   <tr><td>GRS 1980</td>                                          <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
     *   <tr><td>Hayford 1909</td>                                      <td>{@link #ED50}</td></tr>
     *   <tr><td>International 1924</td>                                <td>{@link #ED50}</td></tr>
     *   <tr><td>International 1979</td>                                <td>{@link #ETRS89}, {@link #NAD83}</td></tr>
     *   <tr><td>North American Datum 1927</td>                         <td>{@link #NAD27}</td></tr>
     *   <tr><td>North American Datum 1983</td>                         <td>{@link #NAD83}</td></tr>
     *   <tr><td>NWL 10D</td>                                           <td>{@link #WGS72}</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1972</td>                  <td>{@link #WGS72}</td></tr>
     *   <tr><td>World Geodetic System (WGS) 1984</td>                  <td>{@link #WGS84}</td></tr>
     * </table></blockquote>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.4
     * @version 0.4
     * @module
     */
    public static final class Geodetic extends StandardObjects {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7751726848523836148L;

        /**
         * The EPSG or SIS code of the ellipsoid.
         */
        private final short ellipsoid;

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
        public static final Geodetic WGS84 = new Geodetic("WGS84", (short) 7030);

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
        public static final Geodetic WGS72 = new Geodetic("WGS72", (short) 7043);

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
        public static final Geodetic ETRS89 = new Geodetic("ETRS89", (short) 7019);

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
        public static final Geodetic NAD83 = new Geodetic("NAD83", (short) 7019);

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
        public static final Geodetic NAD27 = new Geodetic("NAD27", (short) 7008);

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
        public static final Geodetic ED50 = new Geodetic("ED50", (short) 7022);

        /**
         * A sphere with a radius of 6371000 metres. Spheres use a simpler algorithm for
         * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#orthodromicDistance
         * orthodromic distance computation}, which may be faster and more robust.
         *
         * <blockquote><table class="compact" style="text-align:left">
         *   <tr><th>Primary names:</th>           <td>"Sphere"</td></tr>
         *   <tr><th>Prime meridian:</th>          <td>Greenwich</td></tr>
         *   <tr><th>Semi-major axis length:</th>  <td>6371000</td></tr>
         *   <tr><th>Semi-minor axis length:</th>  <td>6371000 <i>(definitive)</i></td></tr>
         *   <tr><th>Ellipsoid axes unit:</th>     <td>{@link SI#METRE}</td></tr>
         * </table></blockquote>
         *
         * {@note This ellipsoid is close to the <cite>GRS 1980 Authalic Sphere</cite> (EPSG:7048),
         *        which has a radius of 6371007 metres.}
         */
        public static final Geodetic SPHERE = new Geodetic("SPHERE", (short) -1);

        /**
         * Creates a new constant for the given EPSG or SIS codes.
         * By convention, SIS codes are negative.
         *
         * @param name The programmatic constant name.
         * @param ellipsoid The code for the ellipsoid.
         */
        private Geodetic(final String name, final short ellipsoid) {
            super(name);
            this.ellipsoid = ellipsoid;
        }

        /**
         * Returns the prime meridian associated to this geodetic object.
         * The following table summarizes the prime meridians known to this class,
         * together with a constant that can be used for fetching that prime meridian:
         *
         * <blockquote><table class="sis">
         *   <tr><th>Name or alias</th> <th>Field</th>          <th>EPSG</th></tr>
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
                            final DatumAuthorityFactory factory = datumFactory();
                            if (factory != null) try {
                                cached = object = factory.createPrimeMeridian(StandardDefinitions.GREENWICH);
                                return object;
                            } catch (FactoryException e) {
                                failure("primeMeridian", e);
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
         * Returns the ellipsoid associated to this geodetic object.
         * The following table summarizes the ellipsoids known to this class,
         * together with a constant that can be used for fetching that ellipsoid:
         *
         * <blockquote><table class="sis">
         *   <tr><th>Name or alias</th>                    <th>Field</th>           <th>EPSG</th></tr>
         *   <tr><td>Clarke 1866</td>                      <td>{@link #NAD27}</td>  <td>7008</td></tr>
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
                            if (ellipsoid >= 0) {
                                final DatumAuthorityFactory factory = datumFactory();
                                if (factory != null) try {
                                    cached = object = factory.createEllipsoid(String.valueOf(ellipsoid));
                                    return object;
                                } catch (FactoryException e) {
                                    failure("ellipsoid", e);
                                }
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
         * Returns the prime meridian associated to the given object, or {@code null} if none.
         */
        private static PrimeMeridian primeMeridian(final IdentifiedObject object) {
            if (object instanceof PrimeMeridian) {
                return (PrimeMeridian) object;
            }
            if (object instanceof GeodeticDatum) {
                return ((GeodeticDatum) object).getPrimeMeridian();
            }
            if (object instanceof GeodeticCRS) {
                return ((GeodeticCRS) object).getDatum().getPrimeMeridian();
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
            if (object instanceof GeodeticDatum) {
                return ((GeodeticDatum) object).getEllipsoid();
            }
            if (object instanceof GeodeticCRS) {
                return ((GeodeticCRS) object).getDatum().getEllipsoid();
            }
            return null;
        }
    }
}
