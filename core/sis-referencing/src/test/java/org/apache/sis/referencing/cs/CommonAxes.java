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
package org.apache.sis.referencing.cs;

import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import static java.util.Collections.singletonMap;


/**
 * Collection of axes for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class CommonAxes {
    /**
     * Axis info for geodetic longitudes in a
     * {@linkplain org.opengis.referencing.crs.GeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is "<cite>geodetic longitude</cite>" and the abbreviation is "λ" (lambda).
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #LONGITUDE
     * @see #SPHERICAL_LONGITUDE
     * @see #GEODETIC_LATITUDE
     */
    public static final DefaultCoordinateSystemAxis GEODETIC_LONGITUDE = create("Geodetic longitude", "λ",
            AxisDirection.EAST, 180, NonSI.DEGREE_ANGLE);

    /**
     * Default axis info for geodetic latitudes in a
     * {@linkplain org.opengis.referencing.crs.GeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is "<cite>geodetic latitude</cite>" and the abbreviation is "φ" (phi).
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #LATITUDE
     * @see #SPHERICAL_LATITUDE
     * @see #GEODETIC_LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis GEODETIC_LATITUDE = create("Geodetic latitude", "φ",
            AxisDirection.NORTH, 90, NonSI.DEGREE_ANGLE);

    /**
     * Default axis info for longitudes.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The abbreviation is "λ" (lambda).
     *
     * <p>This axis is usually part of a {@link #LONGITUDE}, {@link #LATITUDE}, {@link #ALTITUDE} set.</p>
     *
     * @see #GEODETIC_LONGITUDE
     * @see #SPHERICAL_LONGITUDE
     * @see #LATITUDE
     */
    public static final DefaultCoordinateSystemAxis LONGITUDE = create("Longitude", "λ",
            AxisDirection.EAST, 180, NonSI.DEGREE_ANGLE);

    /**
     * Default axis info for latitudes.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The abbreviation is "φ" (phi).
     *
     * <p>This axis is usually part of a {@link #LONGITUDE}, {@link #LATITUDE}, {@link #ALTITUDE} set.</p>
     *
     * @see #GEODETIC_LATITUDE
     * @see #SPHERICAL_LATITUDE
     * @see #LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis LATITUDE = create("Latitude", "φ",
            AxisDirection.NORTH, 90, NonSI.DEGREE_ANGLE);

    /**
     * The default axis for height values above the ellipsoid in a
     * {@linkplain org.opengis.referencing.crs.GeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>ellipsoidal heigt</cite>" and the abbreviation is lower case "<var>h</var>".
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #ALTITUDE
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis ELLIPSOIDAL_HEIGHT = create("Ellipsoidal height", "h",
            AxisDirection.UP, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * The default axis for height values measured from gravity.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>gravity-related height</cite>" and the abbreviation is upper case "<var>H</var>".
     *
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis GRAVITY_RELATED_HEIGHT = create("Gravity-related height", "H",
            AxisDirection.UP, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * The default axis for altitude values.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case "<var>h</var>".
     *
     * <p>This axis is usually part of a {@link #LONGITUDE}, {@link #LATITUDE}, {@link #ALTITUDE} set.</p>
     *
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis ALTITUDE = create("Altitude", "h",
            AxisDirection.UP, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * The default axis for depth.
     * Increasing ordinates values go {@linkplain AxisDirection#DOWN down} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>depth</cite>".
     *
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     */
    public static final DefaultCoordinateSystemAxis DEPTH = create("Depth", "d",
            AxisDirection.DOWN, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for radius in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.cs.SphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>geocentric radius</cite>" and the abbreviation is lower case "<var>r</var>".
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_RADIUS = create("Geocentric radius", "r",
            AxisDirection.UP, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for longitudes in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.crs.SphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is "<cite>spherical longitude</cite>" and the abbreviation is "Ω" (omega).
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #LONGITUDE
     * @see #GEODETIC_LONGITUDE
     * @see #SPHERICAL_LATITUDE
     */
    public static final DefaultCoordinateSystemAxis SPHERICAL_LONGITUDE = create("Spherical longitude", "Ω",
            AxisDirection.EAST, 180, NonSI.DEGREE_ANGLE);

    /**
     * Default axis info for latitudes in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.cs.SphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is "<cite>spherical latitude</cite>" and the abbreviation is "Θ" (theta).
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #LATITUDE
     * @see #GEODETIC_LATITUDE
     * @see #SPHERICAL_LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis SPHERICAL_LATITUDE = create("Spherical latitude", "Θ",
            AxisDirection.NORTH, 90, NonSI.DEGREE_ANGLE);

    /**
     * Default axis info for <var>x</var> values in a
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case "<var>x</var>".
     *
     * <p>This axis is usually part of a {@link #X}, {@link #Y}, {@link #Z} set.</p>
     *
     * @see #EASTING
     * @see #WESTING
     * @see #GEOCENTRIC_X
     * @see #DISPLAY_X
     * @see #COLUMN
     */
    public static final DefaultCoordinateSystemAxis X = create("x", "x",
            AxisDirection.EAST, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for <var>y</var> values in a
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case "<var>y</var>".
     *
     * <p>This axis is usually part of a {@link #X}, {@link #Y}, {@link #Z} set.</p>
     *
     * @see #NORTHING
     * @see #SOUTHING
     * @see #GEOCENTRIC_Y
     * @see #DISPLAY_Y
     * @see #ROW
     */
    public static final DefaultCoordinateSystemAxis Y = create("y", "y",
            AxisDirection.NORTH, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for <var>z</var> values in a
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up}
     * and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case "<var>z</var>".
     *
     * <p>This axis is usually part of a {@link #X}, {@link #Y}, {@link #Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis Z = create("z", "z",
            AxisDirection.UP, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for <var>x</var> values in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward prime meridian, but the actual axis direction
     * is {@link AxisDirection#GEOCENTRIC_X GEOCENTRIC_X}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>geocentric X</cite>" and the abbreviation is upper case "<var>X</var>".
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT format),
     * the direction was {@link AxisDirection#OTHER OTHER}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_X = create("X", "X",
            AxisDirection.GEOCENTRIC_X, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for <var>y</var> values in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward East, but the actual axis direction is
     * {@link AxisDirection#GEOCENTRIC_Y GEOCENTRIC_Y}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>geocentric Y</cite>" and the abbreviation is upper case "<var>Y</var>".
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT format),
     * the direction was {@link AxisDirection#EAST EAST}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_Y = create("Y", "Y",
            AxisDirection.GEOCENTRIC_Y, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for <var>z</var> values in a
     * {@linkplain org.opengis.referencing.crs.GeocentricCRS geocentric CRS} using
     * {@linkplain org.opengis.referencing.cs.CartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward North, but the actual axis direction is
     * {@link AxisDirection#GEOCENTRIC_Z GEOCENTRIC_Z}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>geocentric Z</cite>" and the abbreviation is upper case "<var>Z</var>".
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT format),
     * the direction was {@link AxisDirection#NORTH NORTH}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_Z = create("Z", "Z",
            AxisDirection.GEOCENTRIC_Z, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for Easting values in a
     * {@linkplain org.opengis.referencing.crs.ProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>easting</cite>" and the abbreviation is upper case "<var>E</var>".
     *
     * <p>This axis is usually part of a {@link #EASTING}, {@link #NORTHING} set.</p>
     *
     * @see #X
     * @see #EASTING
     * @see #WESTING
     */
    public static final DefaultCoordinateSystemAxis EASTING = create("Easting", "E",
            AxisDirection.EAST, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for Westing values in a
     * {@linkplain org.opengis.referencing.crs.ProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#WEST West} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>westing</cite>" and the abbreviation is upper case "<var>W</var>".
     *
     * @see #X
     * @see #EASTING
     * @see #WESTING
     */
    public static final DefaultCoordinateSystemAxis WESTING = create("Westing", "W",
            AxisDirection.WEST, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for Northing values in a
     * {@linkplain org.opengis.referencing.crs.ProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>northing</cite>" and the abbreviation is upper case "<var>N</var>".
     *
     * <p>This axis is usually part of a {@link #EASTING}, {@link #NORTHING} set.</p>
     *
     * @see #Y
     * @see #NORTHING
     * @see #SOUTHING
     */
    public static final DefaultCoordinateSystemAxis NORTHING = create("Northing", "N",
            AxisDirection.NORTH, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * Default axis info for Southing values in a
     * {@linkplain org.opengis.referencing.crs.ProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#SOUTH South} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is "<cite>southing</cite>" and the abbreviation is upper case "<var>S</var>".
     *
     * @see #Y
     * @see #NORTHING
     * @see #SOUTHING
     */
    public static final DefaultCoordinateSystemAxis SOUTHING = create("Southing", "S",
            AxisDirection.SOUTH, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * A default axis for time values in a {@linkplain org.opengis.referencing.cs.TimeCS time CS}.
     * Increasing time go toward {@linkplain AxisDirection#FUTURE future} and units are {@linkplain NonSI#DAY days}.
     * The abbreviation is lower case "<var>t</var>".
     */
    public static final DefaultCoordinateSystemAxis TIME = create("Time", "t",
            AxisDirection.FUTURE, Double.POSITIVE_INFINITY, NonSI.DAY);

    /**
     * A default axis for column indices in a {@linkplain org.opengis.coverage.grid.GridCoverage grid coverage}.
     * Increasing values go toward {@linkplain AxisDirection#COLUMN_POSITIVE positive column number}.
     * The abbreviation is lower case "<var>i</var>".
     */
    public static final DefaultCoordinateSystemAxis COLUMN = create("Column", "i",
            AxisDirection.COLUMN_POSITIVE, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * A default axis for row indices in a {@linkplain org.opengis.coverage.grid.GridCoverage grid coverage}.
     * Increasing values go toward {@linkplain AxisDirection#ROW_POSITIVE positive row number}.
     * The abbreviation is lower case "<var>j</var>".
     */
    public static final DefaultCoordinateSystemAxis ROW = create("Row", "j",
            AxisDirection.ROW_POSITIVE, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * A default axis for <var>x</var> values in a display device. Increasing values go toward
     * {@linkplain AxisDirection#DISPLAY_RIGHT display right}.
     * The abbreviation is lower case "<var>x</var>".
     */
    public static final DefaultCoordinateSystemAxis DISPLAY_X = create("x", "x",
            AxisDirection.DISPLAY_RIGHT, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * A default axis for <var>y</var> values in a display device. Increasing values go toward
     * {@linkplain AxisDirection#DISPLAY_DOWN display down}.
     * The abbreviation is lower case "<var>y</var>".
     */
    public static final DefaultCoordinateSystemAxis DISPLAY_Y = create("y", "y",
            AxisDirection.DISPLAY_DOWN, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * Undefined or unknown axis. Axis direction is {@link AxisDirection#OTHER OTHER}
     * and the unit is dimensionless. This constant is sometime used as a placeholder
     * for axes that were not properly defined.
     */
    public static final DefaultCoordinateSystemAxis UNDEFINED = create("Undefined", "?",
            AxisDirection.OTHER, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * Creates a new axis of the given name, abbreviation, direction and unit.
     */
    private static DefaultCoordinateSystemAxis create(final String name, final String abbreviation,
            final AxisDirection direction, final double maximum, final Unit<?> unit)
    {
        return new DefaultCoordinateSystemAxis(singletonMap(DefaultCoordinateSystemAxis.NAME_KEY, name),
                abbreviation, direction, unit, name.endsWith("radius") ? 0 : -maximum, maximum,
                name.endsWith("longitude") ? RangeMeaning.WRAPAROUND : RangeMeaning.EXACT);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private CommonAxes() {
    }
}
