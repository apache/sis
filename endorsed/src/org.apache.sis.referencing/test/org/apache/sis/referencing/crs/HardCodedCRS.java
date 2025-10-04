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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.HashMap;
import static org.opengis.referencing.IdentifiedObject.*;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.legacy.DefaultImageCRS;
import org.apache.sis.metadata.iso.extent.Extents;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;

// Test dependencies
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.datum.GeodeticDatumMock;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.datum.PixelInCell;

// Specific to the main branch:
import static org.opengis.referencing.ReferenceSystem.DOMAIN_OF_VALIDITY_KEY;


/**
 * Collection of coordinate reference systems for testing purpose.
 * This class defines geographic, vertical, temporal and engineering CRS, but no projected CRS.
 * For projected CRS, see {@link org.apache.sis.referencing.operation.HardCodedConversions}.
 * Except specified otherwise, all geographic CRS have (longitude, latitude) axis order.
 * This is the opposite of traditional axis order, but it matches the order used internally
 * by Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HardCodedCRS {
    /**
     * A two-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>latitude</var>, <var>longitude</var>) coordinates with latitude values
     * increasing towards the North and longitude values increasing towards the East.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4326}.</p>
     */
    public static final DefaultGeographicCRS WGS84_LATITUDE_FIRST = new DefaultGeographicCRS(
            properties("WGS 84 (φ,λ)", "4326"), HardCodedDatum.WGS84, null, HardCodedCS.GEODETIC_φλ);

    /**
     * A two-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) coordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4326} except for axis order,
     * since EPSG puts latitude before longitude.</p>
     */
    public static final DefaultGeographicCRS WGS84 = new DefaultGeographicCRS(
            properties("WGS 84", null), HardCodedDatum.WGS84, null, HardCodedCS.GEODETIC_2D);

    /**
     * A three-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) coordinates with longitude values
     * increasing towards East, latitude values increasing towards North and height positive above the ellipsoid.
     * The angular units are decimal degrees, the height unit is the metre, and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4979} (the successor to {@code EPSG:4329}, itself the successor
     * to {@code EPSG:4327}) except for axis order, since EPSG puts latitude before longitude.</p>
     */
    public static final DefaultGeographicCRS WGS84_3D = new DefaultGeographicCRS(
            properties("WGS 84 (3D)", null), HardCodedDatum.WGS84, null, HardCodedCS.GEODETIC_3D);

    /**
     * A four-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>, <var>time</var>)
     * with the 3 first dimensions specified by {@link #WGS84_3D} and the fourth dimension specified
     * by {@link #TIME}.
     *
     * @see #WGS84_4D_TIME_FIRST
     */
    public static final DefaultCompoundCRS WGS84_4D;

    /**
     * A (λ,φ,t) CRS where <var>t</var> is the {@link #TIME}.
     */
    public static final DefaultCompoundCRS WGS84_WITH_TIME;

    /**
     * A (λ,φ,t) CRS where <var>t</var> is the {@link #DAY_OF_YEAR}.
     * This CRS has two wraparound axes: <var>λ</var> and <var>t</var>.
     */
    public static final DefaultCompoundCRS WGS84_WITH_CYCLIC_TIME;

    /**
     * A four-dimensional geographic coordinate reference system with time as the first axis.
     * This CRS uses (<var>time</var>, <var>longitude</var>, <var>latitude</var>, <var>height</var>)
     * with the first dimension specified by {@link #TIME} and the 3 last dimensions specified by {@link #WGS84_3D}.
     * Such axis order is unusual but we use it as a way to verify that SIS is robust to arbitrary axis order.
     */
    public static final DefaultCompoundCRS WGS84_4D_TIME_FIRST;

    /**
     * A two-dimensional geographic coordinate reference system using the Paris prime meridian.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) coordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units for the prime meridian and the axes are grads.
     *
     * <p>This CRS is equivalent to {@code EPSG:4807} except for axis order, since EPSG defines
     * (<var>latitude</var>, <var>longitude</var>).</p>
     */
    public static final DefaultGeographicCRS NTF = new DefaultGeographicCRS(
            Map.of(DefaultGeographicCRS.NAME_KEY, "NTF (Paris)"),
            HardCodedDatum.NTF, null, HardCodedCS.ELLIPSOIDAL_gon);

    /**
     * A two-dimensional geographic coordinate reference system using the Paris prime meridian.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) coordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees except for the prime meridian (Paris),
     * which is measured in grads.
     *
     * <p>This CRS is equivalent to {@code EPSG:4807} except for axis order and units of measurement,
     * since EPSG defines (<var>latitude</var>, <var>longitude</var>) in grads.  The main purpose of
     * this CRS is to test the convolved case where the unit of prime meridian is different than the
     * axis units.</p>
     */
    public static final DefaultGeographicCRS NTF_NORMALIZED_AXES = new DefaultGeographicCRS(
            Map.of(DefaultGeographicCRS.NAME_KEY, NTF.getName()),
            HardCodedDatum.NTF, null, HardCodedCS.GEODETIC_2D);

    /**
     * A three-dimensional geographic coordinate reference system using the Tokyo datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) coordinates
     * with longitude values increasing towards the East, latitude values increasing towards
     * the North and ellipsoidal eight increasing toward up.
     * The angular units are decimal degrees and the linear units are metres.
     *
     * <p>This CRS is equivalent to {@code EPSG:4301} except for axis order and the addition
     * of ellipsoidal height.</p>
     */
    public static final DefaultGeographicCRS TOKYO = new DefaultGeographicCRS(
            Map.of(DefaultGeographicCRS.NAME_KEY, "Tokyo"),
            HardCodedDatum.TOKYO, null, HardCodedCS.GEODETIC_3D);

    /**
     * A two-dimensional geographic coordinate reference system using the JGD2000 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) coordinates
     * with longitude values increasing towards the East, latitude values increasing towards
     * the North and ellipsoidal eight increasing toward up.
     * The angular units are decimal degrees and the linear units are metres.
     *
     * <p>This CRS is equivalent to {@code EPSG:4612} except for axis order and the addition
     * of ellipsoidal height.</p>
     */
    public static final DefaultGeographicCRS JGD2000 = new DefaultGeographicCRS(
            Map.of(DefaultGeographicCRS.NAME_KEY, "JGD2000"),
            HardCodedDatum.JGD2000, null, HardCodedCS.GEODETIC_3D);

    /**
     * A two-dimensional geographic coordinate reference system using an unknown datum based on the GRS 1980 ellipsoid.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) coordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units for the prime meridian and the axes are degrees.
     *
     * <p>This CRS is almost identical to {@link #WGS84}.
     * It can be used for testing tiny differences between two datum.</p>
     */
    public static final DefaultGeographicCRS GRS80 = new DefaultGeographicCRS(
            Map.of(DefaultGeographicCRS.NAME_KEY, "Unknown datum based on GRS 1980 ellipsoid"),
            GeodeticDatumMock.GRS80, null, HardCodedCS.GEODETIC_2D);

    /**
     * A two-dimensional geographic coordinate reference system using a spherical datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) coordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     */
    public static final DefaultGeographicCRS SPHERE = new DefaultGeographicCRS(
            getProperties(HardCodedDatum.SPHERE), HardCodedDatum.SPHERE, null, HardCodedCS.GEODETIC_2D);

    /**
     * A two-dimensional geographic coordinate reference system using a spherical datum.
     * This CRS uses (<var>latitude</var>, <var>longitude</var>) coordinates with latitude
     * values increasing towards the North and longitude values increasing towards the East.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     */
    public static final DefaultGeographicCRS SPHERE_LATITUDE_FIRST = new DefaultGeographicCRS(
            getProperties(HardCodedDatum.SPHERE), HardCodedDatum.SPHERE, null, HardCodedCS.GEODETIC_φλ);

    /**
     * A geocentric CRS with a spherical coordinate system.
     * Prime meridian is Greenwich, geodetic reference frame is WGS84 and linear units are metres.
     */
    @SuppressWarnings("deprecation")
    public static final DefaultGeocentricCRS SPHERICAL = new DefaultGeocentricCRS(
            getProperties(HardCodedCS.SPHERICAL), HardCodedDatum.WGS84, null, HardCodedCS.SPHERICAL);

    /**
     * A geocentric CRS with a Cartesian coordinate system.
     * Prime meridian is Greenwich, geodetic reference frame is WGS84 and linear units are metres.
     * The <var>X</var> axis points towards the prime meridian.
     * The <var>Y</var> axis points East.
     * The <var>Z</var> axis points North.
     */
    @SuppressWarnings("deprecation")
    public static final DefaultGeocentricCRS GEOCENTRIC = new DefaultGeocentricCRS(
            getProperties(HardCodedCS.GEOCENTRIC), HardCodedDatum.WGS84, null, HardCodedCS.GEOCENTRIC);

    /**
     * A two-dimensional Cartesian coordinate reference system with (x,y) axes in metres.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS cannot be reprojected to a geographic coordinate reference system for example).
     */
    public static final DefaultEngineeringCRS CARTESIAN_2D = new DefaultEngineeringCRS(
            getProperties(HardCodedCS.CARTESIAN_2D), HardCodedDatum.UNKNOWN, null, HardCodedCS.CARTESIAN_2D);

    /**
     * A two-dimensional Cartesian coordinate reference system with (x,y,z) axes in metres.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS cannot be reprojected to a geographic coordinate reference system for example).
     */
    public static final DefaultEngineeringCRS CARTESIAN_3D = new DefaultEngineeringCRS(
            getProperties(HardCodedCS.CARTESIAN_3D), HardCodedDatum.UNKNOWN, null, HardCodedCS.CARTESIAN_3D);

    /**
     * A vertical coordinate reference system using ellipsoidal datum.
     * Ellipsoidal heights are measured along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>This is not a valid vertical CRS according ISO 19111.
     * This CRS is used by Apache SIS for internal calculation.</p>
     */
    public static final DefaultVerticalCRS ELLIPSOIDAL_HEIGHT = new DefaultVerticalCRS(
            getProperties(HardCodedCS.ELLIPSOIDAL_HEIGHT), HardCodedDatum.ELLIPSOID, null, HardCodedCS.ELLIPSOIDAL_HEIGHT);

    /**
     * A vertical coordinate reference system using ellipsoidal datum.
     * Ellipsoidal heights are measured along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>This is not a valid vertical CRS according ISO 19111.
     * This CRS is used by Apache SIS for internal calculation.</p>
     */
    public static final DefaultVerticalCRS ELLIPSOIDAL_HEIGHT_cm = new DefaultVerticalCRS(
            getProperties(HardCodedCS.ELLIPSOIDAL_HEIGHT_cm), HardCodedDatum.ELLIPSOID, null, HardCodedCS.ELLIPSOIDAL_HEIGHT_cm);

    /**
     * A vertical coordinate reference system using Mean Sea Level datum.
     */
    public static final DefaultVerticalCRS GRAVITY_RELATED_HEIGHT = new DefaultVerticalCRS(
            properties("MSL height", "5714"), HardCodedDatum.MEAN_SEA_LEVEL, null, HardCodedCS.GRAVITY_RELATED_HEIGHT);

    /**
     * A vertical coordinate reference system using Mean Sea Level datum.
     */
    public static final DefaultVerticalCRS DEPTH = new DefaultVerticalCRS(
            getProperties(HardCodedCS.DEPTH), HardCodedDatum.MEAN_SEA_LEVEL, null, HardCodedCS.DEPTH);

    /**
     * A temporal coordinate reference system for time in days elapsed since November 17, 1858 at 00:00 UTC.
     */
    public static final DefaultTemporalCRS TIME = new DefaultTemporalCRS(
            getProperties(HardCodedCS.DAYS), HardCodedDatum.MODIFIED_JULIAN, null, HardCodedCS.DAYS);

    static {
        // Declared here because otherwise it would be illegal forward references.
        WGS84_4D = new DefaultCompoundCRS(properties("WGS 84 (3D) + time", null), WGS84_3D, TIME);
        WGS84_4D_TIME_FIRST = new DefaultCompoundCRS(properties("time + WGS 84 (3D)", null), TIME, WGS84_3D);
    }

    /**
     * A parametric CRS for day of year, without any particular year.
     * The axis is cyclic: after day 365 we restart at day 1.
     */
    public static final DefaultParametricCRS DAY_OF_YEAR = new DefaultParametricCRS(
            getProperties(HardCodedCS.DAY_OF_YEAR), HardCodedDatum.DAY_OF_YEAR, null, HardCodedCS.DAY_OF_YEAR);

    static {
        WGS84_WITH_TIME = new DefaultCompoundCRS(properties("WGS 84 + time", null), WGS84, TIME);
        WGS84_WITH_CYCLIC_TIME = new DefaultCompoundCRS(properties("WGS 84 + day of year", null), WGS84, DAY_OF_YEAR);
    }

    /**
     * A (λ,φ,H) CRS where <var>H</var> is the {@link #GRAVITY_RELATED_HEIGHT}.
     * This constant uses the "geoid" term as an approximation for the gravity related height.
     */
    public static final DefaultCompoundCRS GEOID_3D = new DefaultCompoundCRS(
            properties("WGS 84 + height", null), WGS84, GRAVITY_RELATED_HEIGHT);

    /**
     * A (λ,φ,H,t) CRS where <var>H</var> is the {@link #GRAVITY_RELATED_HEIGHT} and <var>t</var> is {@link #TIME}.
     * This constant uses the "geoid" term as an approximation for the gravity related height.
     */
    public static final DefaultCompoundCRS GEOID_4D = new DefaultCompoundCRS(
            properties("WGS 84 + height + time", null), WGS84, GRAVITY_RELATED_HEIGHT, TIME);

    /**
     * A (H,t,φ,λ) CRS where <var>H</var> is the {@link #GRAVITY_RELATED_HEIGHT}.
     * Such axis order is unusual but we use it as a way to verify that SIS is robust to arbitrary axis order.
     */
    public static final DefaultCompoundCRS GEOID_4D_MIXED_ORDER = new DefaultCompoundCRS(
            properties("height + time + WGS 84 (φ,λ)", null), GRAVITY_RELATED_HEIGHT, TIME, WGS84_LATITUDE_FIRST);

    /**
     * A (λ,φ,H,t) CRS as a nested compound CRS.
     */
    public static final DefaultCompoundCRS NESTED = new DefaultCompoundCRS(
            properties("(WGS 84 + height) + time", null), GEOID_3D, TIME);

    /**
     * A two-dimensional Cartesian coordinate reference system with (column, row) axes.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS cannot be reprojected to a geographic coordinate reference system for example).
     *
     * <p>The {@code pixelInCell} attribute of the associated {@code ImageDatum}
     * is set to {@link PixelInCell#CELL_CENTER}.</p>
     */
    @SuppressWarnings("exports")
    public static final DefaultImageCRS IMAGE = new DefaultImageCRS(
            getProperties(HardCodedDatum.IMAGE), HardCodedDatum.IMAGE, HardCodedCS.GRID);

    /**
     * Creates a map of properties for the given name and code with world extent.
     */
    private static Map<String,?> properties(final String name, final String code) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, name);
        properties.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        if (code != null) {
            properties.put(IDENTIFIERS_KEY, new NamedIdentifier(HardCodedCitations.EPSG, code));
        }
        return properties;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedCRS() {
    }
}
