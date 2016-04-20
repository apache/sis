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
import java.util.Collections;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;

import static org.opengis.referencing.IdentifiedObject.*;
import static org.opengis.referencing.ReferenceSystem.DOMAIN_OF_VALIDITY_KEY;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;


/**
 * Collection of coordinate reference systems for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class HardCodedCRS {
    /**
     * A two-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>latitude</var>, <var>longitude</var>) ordinates with latitude values
     * increasing towards the North and longitude values increasing towards the East.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4326}.</p>
     */
    public static final DefaultGeographicCRS WGS84_φλ = new DefaultGeographicCRS(
            properties("WGS 84 (φ,λ)", "4326"), HardCodedDatum.WGS84, HardCodedCS.GEODETIC_φλ);

    /**
     * A two-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) ordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4326} except for axis order,
     * since EPSG puts latitude before longitude.</p>
     */
    public static final DefaultGeographicCRS WGS84 = new DefaultGeographicCRS(
            properties("WGS 84", null), HardCodedDatum.WGS84, HardCodedCS.GEODETIC_2D);

    /**
     * A three-dimensional geographic coordinate reference system using the WGS84 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) ordinates with longitude values
     * increasing towards East, latitude values increasing towards North and height positive above the ellipsoid.
     * The angular units are decimal degrees, the height unit is the metre, and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:4979} (the successor to {@code EPSG:4329}, itself the successor
     * to {@code EPSG:4327}) except for axis order, since EPSG puts latitude before longitude.</p>
     */
    public static final DefaultGeographicCRS WGS84_3D = new DefaultGeographicCRS(
            properties("WGS 84 (3D)", null), HardCodedDatum.WGS84, HardCodedCS.GEODETIC_3D);

    /**
     * A two-dimensional geographic coordinate reference system using the Paris prime meridian.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) ordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units for the prime meridian and the axes are grades.
     *
     * <p>This CRS is equivalent to {@code EPSG:4807} except for axis order, since EPSG defines
     * (<var>latitude</var>, <var>longitude</var>).</p>
     *
     * @since 0.6
     */
    public static final DefaultGeographicCRS NTF = new DefaultGeographicCRS(
            Collections.singletonMap(DefaultGeographicCRS.NAME_KEY, "NTF (Paris)"),
            HardCodedDatum.NTF, HardCodedCS.ELLIPSOIDAL_gon);

    /**
     * A two-dimensional geographic coordinate reference system using the Paris prime meridian.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) ordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees except for the prime meridian (Paris),
     * which is measured in grades.
     *
     * <p>This CRS is equivalent to {@code EPSG:4807} except for axis order and units of measurement,
     * since EPSG defines (<var>latitude</var>, <var>longitude</var>) in grades. The main purpose of
     * this CRS is to test the convolved case where the unit of prime meridian is different than the
     * axis units.</p>
     *
     * @since 0.6
     */
    public static final DefaultGeographicCRS NTF_NORMALIZED_AXES = new DefaultGeographicCRS(
            Collections.singletonMap(DefaultGeographicCRS.NAME_KEY, NTF.getName()),
            HardCodedDatum.NTF, HardCodedCS.GEODETIC_2D);

    /**
     * A three-dimensional geographic coordinate reference system using the Tokyo datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) ordinates
     * with longitude values increasing towards the East, latitude values increasing towards
     * the North and ellipsoidal eight increasing toward up.
     * The angular units are decimal degrees and the linear units are metres.
     *
     * <p>This CRS is equivalent to {@code EPSG:4301} except for axis order and the addition
     * of ellipsoidal height.</p>
     *
     * @since 0.7
     */
    public static final DefaultGeographicCRS TOKYO = new DefaultGeographicCRS(
            Collections.singletonMap(DefaultGeographicCRS.NAME_KEY, "Tokyo"),
            HardCodedDatum.TOKYO, HardCodedCS.GEODETIC_3D);

    /**
     * A two-dimensional geographic coordinate reference system using the JGD2000 datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) ordinates
     * with longitude values increasing towards the East, latitude values increasing towards
     * the North and ellipsoidal eight increasing toward up.
     * The angular units are decimal degrees and the linear units are metres.
     *
     * <p>This CRS is equivalent to {@code EPSG:4612} except for axis order and the addition
     * of ellipsoidal height.</p>
     *
     * @since 0.7
     */
    public static final DefaultGeographicCRS JGD2000 = new DefaultGeographicCRS(
            Collections.singletonMap(DefaultGeographicCRS.NAME_KEY, "JGD2000"),
            HardCodedDatum.JGD2000, HardCodedCS.GEODETIC_3D);

    /**
     * A two-dimensional geographic coordinate reference system using a spherical datum.
     * This CRS uses (<var>longitude</var>, <var>latitude</var>) ordinates with longitude values
     * increasing towards the East and latitude values increasing towards the North.
     * The angular units are decimal degrees and the prime meridian is Greenwich.
     */
    public static final DefaultGeographicCRS SPHERE = new DefaultGeographicCRS(
            getProperties(HardCodedDatum.SPHERE), HardCodedDatum.SPHERE, HardCodedCS.GEODETIC_2D);

    /**
     * A geocentric CRS with a spherical coordinate system.
     * Prime meridian is Greenwich, geodetic datum is WGS84 and linear units are metres.
     */
    public static final DefaultGeocentricCRS SPHERICAL = new DefaultGeocentricCRS(
            getProperties(HardCodedCS.SPHERICAL), HardCodedDatum.WGS84, HardCodedCS.SPHERICAL);

    /**
     * A geocentric CRS with a Cartesian coordinate system.
     * Prime meridian is Greenwich, geodetic datum is WGS84 and linear units are metres.
     * The <var>X</var> axis points towards the prime meridian.
     * The <var>Y</var> axis points East.
     * The <var>Z</var> axis points North.
     */
    public static final DefaultGeocentricCRS GEOCENTRIC = new DefaultGeocentricCRS(
            getProperties(HardCodedCS.GEOCENTRIC), HardCodedDatum.WGS84, HardCodedCS.GEOCENTRIC);

    /**
     * A two-dimensional Cartesian coordinate reference system with (x,y) axes in metres.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS can't be reprojected to a geographic coordinate reference system for example).
     */
    public static final DefaultEngineeringCRS CARTESIAN_2D = new DefaultEngineeringCRS(
            getProperties(HardCodedCS.CARTESIAN_2D), HardCodedDatum.UNKNOWN, HardCodedCS.CARTESIAN_2D);

    /**
     * A two-dimensional Cartesian coordinate reference system with (x,y,z) axes in metres.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS can't be reprojected to a geographic coordinate reference system for example).
     */
    public static final DefaultEngineeringCRS CARTESIAN_3D = new DefaultEngineeringCRS(
            getProperties(HardCodedCS.CARTESIAN_3D), HardCodedDatum.UNKNOWN, HardCodedCS.CARTESIAN_3D);

    /**
     * A vertical coordinate reference system using ellipsoidal datum.
     * Ellipsoidal heights are measured along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>This is not a valid vertical CRS according ISO 19111.
     * This CRS is used by Apache SIS for internal calculation.</p>
     */
    public static final DefaultVerticalCRS ELLIPSOIDAL_HEIGHT = new DefaultVerticalCRS(
            getProperties(HardCodedCS.ELLIPSOIDAL_HEIGHT), HardCodedDatum.ELLIPSOID, HardCodedCS.ELLIPSOIDAL_HEIGHT);

    /**
     * A vertical coordinate reference system using ellipsoidal datum.
     * Ellipsoidal heights are measured along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>This is not a valid vertical CRS according ISO 19111.
     * This CRS is used by Apache SIS for internal calculation.</p>
     *
     * @since 0.7
     */
    public static final DefaultVerticalCRS ELLIPSOIDAL_HEIGHT_cm = new DefaultVerticalCRS(
            getProperties(HardCodedCS.ELLIPSOIDAL_HEIGHT_cm), HardCodedDatum.ELLIPSOID, HardCodedCS.ELLIPSOIDAL_HEIGHT_cm);

    /**
     * A vertical coordinate reference system using Mean Sea Level datum.
     */
    public static final DefaultVerticalCRS GRAVITY_RELATED_HEIGHT = new DefaultVerticalCRS(
            properties("MSL height", "5714"), HardCodedDatum.MEAN_SEA_LEVEL, HardCodedCS.GRAVITY_RELATED_HEIGHT);

    /**
     * A vertical coordinate reference system using Mean Sea Level datum.
     */
    public static final DefaultVerticalCRS DEPTH = new DefaultVerticalCRS(
            getProperties(HardCodedCS.DEPTH), HardCodedDatum.MEAN_SEA_LEVEL, HardCodedCS.DEPTH);

    /**
     * A temporal coordinate reference system for time in days elapsed since November 17, 1858 at 00:00 UTC.
     */
    public static final DefaultTemporalCRS TIME = new DefaultTemporalCRS(
            getProperties(HardCodedCS.DAYS), HardCodedDatum.MODIFIED_JULIAN, HardCodedCS.DAYS);

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
     * A two-dimensional Cartesian coordinate reference system with (column, row) axes.
     * By default, this CRS has no transformation path to any other CRS (i.e. a map using
     * this CS can't be reprojected to a geographic coordinate reference system for example).
     *
     * <p>The {@code PixelInCell} attribute of the associated {@code ImageDatum}
     * is set to {@link PixelInCell#CELL_CENTER}.</p>
     */
    public static final DefaultImageCRS IMAGE = new DefaultImageCRS(
            getProperties(HardCodedDatum.IMAGE), HardCodedDatum.IMAGE, HardCodedCS.GRID);

    /**
     * Creates a map of properties for the given name and code with world extent.
     */
    private static Map<String,?> properties(final String name, final String code) {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
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
