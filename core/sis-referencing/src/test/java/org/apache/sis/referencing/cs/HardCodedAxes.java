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

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.internal.metadata.AxisDirections;


/**
 * Collection of axes for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class HardCodedAxes {
    /**
     * Axis for geodetic longitudes in a {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is <cite>"geodetic longitude"</cite> and the abbreviation is "λ" (lambda).
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #SPHERICAL_LONGITUDE
     * @see #GEODETIC_LATITUDE
     */
    public static final DefaultCoordinateSystemAxis GEODETIC_LONGITUDE = create(AxisNames.GEODETIC_LONGITUDE, "λ",
            AxisDirection.EAST, NonSI.DEGREE_ANGLE, -180, 180, RangeMeaning.WRAPAROUND);

    /**
     * Axis for geodetic latitudes in a {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is <cite>"geodetic latitude"</cite> and the abbreviation is "φ" (phi).
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #SPHERICAL_LATITUDE
     * @see #GEODETIC_LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis GEODETIC_LATITUDE = create(AxisNames.GEODETIC_LATITUDE, "φ",
            AxisDirection.NORTH, NonSI.DEGREE_ANGLE, -90, 90, RangeMeaning.EXACT);

    /**
     * Identical to {@link #GEODETIC_LONGITUDE} except for the range of longitude values.
     */
    public static final DefaultCoordinateSystemAxis SHIFTED_LONGITUDE = create(AxisNames.GEODETIC_LONGITUDE, "λ",
            AxisDirection.EAST, NonSI.DEGREE_ANGLE, 0, 360, RangeMeaning.WRAPAROUND);

    /**
     * Axis for longitudes in gradian units.
     * The axis name is {@code "Longitude"} (the {@code "Geodetic"} prefix is omitted for testing purpose).
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East} and units are {@link NonSI#GRADE}.
     * The abbreviation is "λ" (lambda). The unit symbol is "gon".
     *
     * @see #GEODETIC_LONGITUDE
     * @see #SPHERICAL_LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis LONGITUDE_gon = create(AxisNames.LONGITUDE, "λ",
            AxisDirection.EAST, NonSI.GRADE, -200, 200, RangeMeaning.WRAPAROUND);

    /**
     * Axis for latitudes in gradian units.
     * The axis name is {@code "Latitude"} (the {@code "Geodetic"} prefix is omitted for testing purpose).
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North} and units are {@link NonSI#GRADE}.
     * The abbreviation is "φ" (phi). The unit symbol is "gon".
     *
     * @see #GEODETIC_LATITUDE
     * @see #SPHERICAL_LATITUDE
     */
    public static final DefaultCoordinateSystemAxis LATITUDE_gon = create(AxisNames.LATITUDE, "φ",
            AxisDirection.NORTH, NonSI.GRADE, -100, 100, RangeMeaning.EXACT);

    /**
     * Axis for height values above the ellipsoid in a
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"ellipsoidal height"</cite> and the abbreviation is lower case <cite>"h"</cite>.
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ELLIPSOIDAL_HEIGHT} set.</p>
     *
     * @see #ALTITUDE
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis ELLIPSOIDAL_HEIGHT = create(AxisNames.ELLIPSOIDAL_HEIGHT, "h",
            AxisDirection.UP, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for height values above the ellipsoid in a
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#CENTIMETRE centimetres}.
     *
     * @since 0.7
     */
    public static final DefaultCoordinateSystemAxis ELLIPSOIDAL_HEIGHT_cm = create(AxisNames.ELLIPSOIDAL_HEIGHT, "h",
            AxisDirection.UP, SI.CENTIMETRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for height values measured from gravity.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"gravity-related height"</cite> and the abbreviation is upper case <cite>"H"</cite>.
     *
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis GRAVITY_RELATED_HEIGHT = create(AxisNames.GRAVITY_RELATED_HEIGHT, "H",
            AxisDirection.UP, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * A height in centimetres.
     */
    public static final DefaultCoordinateSystemAxis HEIGHT_cm = create("Height", "h",
            AxisDirection.UP, SI.CENTIMETRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for altitude values.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case <cite>"h"</cite>.
     *
     * <p>This axis is usually part of a {@link #GEODETIC_LONGITUDE}, {@link #GEODETIC_LATITUDE},
     * {@link #ALTITUDE} tuple.</p>
     *
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis ALTITUDE = create("Altitude", "h",
            AxisDirection.UP, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for depth.
     * Increasing ordinates values go {@linkplain AxisDirection#DOWN down} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"depth"</cite>.
     *
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GEOCENTRIC_RADIUS
     * @see #GRAVITY_RELATED_HEIGHT
     */
    public static final DefaultCoordinateSystemAxis DEPTH = create(AxisNames.DEPTH, "D",
            AxisDirection.DOWN, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for radius in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"geocentric radius"</cite> and the abbreviation is upper-case <cite>"R"</cite>.
     *
     * <div class="note"><b>Note:</b>
     * The uses upper-case <cite>"R"</cite> come from EPSG dataset 8.9.
     * ISO 19111 and 19162 use lower-case <cite>"r"</cite> instead,
     * but with "awayFrom" direction instead of "geocentricRadius".
     * In this class, <cite>"r"</cite> is taken by {@link #DISTANCE}.</div>
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #DISTANCE
     * @see #ALTITUDE
     * @see #ELLIPSOIDAL_HEIGHT
     * @see #GRAVITY_RELATED_HEIGHT
     * @see #DEPTH
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_RADIUS = create(AxisNames.GEOCENTRIC_RADIUS, "R",
            AxisDirection.UP, SI.METRE, 0, Double.POSITIVE_INFINITY, RangeMeaning.EXACT);

    /**
     * Axis for longitudes in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is <cite>"spherical longitude"</cite> and the abbreviation is "θ" (theta).
     *
     * <p>This axis is close to the definition found in the EPSG database, except for the "long" abbreviation which
     * is replaced by "θ". Note that other conventions exist, in which the meaning of φ and θ are interchanged.
     * ISO mentions also the symbol Ω, but it is not clear if it applies to longitude or latitude.
     * The "θ" abbreviation used here is found in ISO 19162.
     * See {@link AxisNames#SPHERICAL_LONGITUDE} for other information.</p>
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #GEODETIC_LONGITUDE
     * @see #SPHERICAL_LATITUDE
     *
     * @see <a href="http://en.wikipedia.org/wiki/Spherical_coordinate_system">Spherical coordinate system on Wikipedia</a>
     * @see <a href="http://mathworld.wolfram.com/SphericalCoordinates.html">Spherical coordinate system on MathWorld</a>
     */
    public static final DefaultCoordinateSystemAxis SPHERICAL_LONGITUDE = create(AxisNames.SPHERICAL_LONGITUDE, "θ",
            AxisDirection.EAST, NonSI.DEGREE_ANGLE, -180, 180, RangeMeaning.WRAPAROUND);

    /**
     * Axis for latitudes in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North}
     * and units are {@linkplain NonSI#DEGREE_ANGLE degrees}.
     * The ISO 19111 name is <cite>"spherical latitude"</cite> and the abbreviation is "φ′" (phi prime).
     *
     * <p>This axis is close to the definition found in the EPSG database, except for the "lat" abbreviation
     * which is replaced by "φ′". Note that other conventions exist, in which the meaning of φ and θ are
     * interchanged or in which this axis is named "elevation" and is oriented toward "Up".
     * Other conventions use symbol Ψ or Ω.
     * The "φ" abbreviation used here is found in ISO 19162.
     * See {@link AxisNames#SPHERICAL_LATITUDE} for other information.</p>
     *
     * <p>This axis is usually part of a {@link #SPHERICAL_LONGITUDE}, {@link #SPHERICAL_LATITUDE},
     * {@link #GEOCENTRIC_RADIUS} set.</p>
     *
     * @see #GEODETIC_LATITUDE
     * @see #SPHERICAL_LONGITUDE
     */
    public static final DefaultCoordinateSystemAxis SPHERICAL_LATITUDE = create(AxisNames.SPHERICAL_LATITUDE, "φ′",
            AxisDirection.NORTH, NonSI.DEGREE_ANGLE, -90, 90, RangeMeaning.EXACT);

    /**
     * Axis for <var>x</var> values in a {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East} and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case <cite>"x"</cite>.
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
            AxisDirection.EAST, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>y</var> values in a {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North} and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case <cite>"y"</cite>.
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
            AxisDirection.NORTH, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>z</var> values in a {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values go {@linkplain AxisDirection#UP up} and units are {@linkplain SI#METRE metres}.
     * The abbreviation is lower case <cite>"z"</cite>.
     *
     * <p>This axis is usually part of a {@link #X}, {@link #Y}, {@link #Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis Z = create("z", "z",
            AxisDirection.UP, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>x</var> values in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward prime meridian, but the actual axis direction
     * is {@link AxisDirection#GEOCENTRIC_X GEOCENTRIC_X}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"Geocentric X"</cite> and the abbreviation is upper case <cite>"X"</cite>.
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT 1 format),
     * the direction was {@link AxisDirection#OTHER OTHER}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_X = create(AxisNames.GEOCENTRIC_X, "X",
            AxisDirection.GEOCENTRIC_X, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>y</var> values in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward East, but the actual axis direction is
     * {@link AxisDirection#GEOCENTRIC_Y GEOCENTRIC_Y}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"Geocentric Y"</cite> and the abbreviation is upper case <cite>"Y"</cite>.
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT 1 format),
     * the direction was {@link AxisDirection#EAST EAST}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_Y = create(AxisNames.GEOCENTRIC_Y, "Y",
            AxisDirection.GEOCENTRIC_Y, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>z</var> values in a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric CRS}
     * using {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian CS}.
     * Increasing ordinates values goes typically toward North, but the actual axis direction is
     * {@link AxisDirection#GEOCENTRIC_Z GEOCENTRIC_Z}. The units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"Geocentric Z"</cite> and the abbreviation is upper case <cite>"Z"</cite>.
     *
     * <p>In legacy OGC 01-009 specification (still in use for WKT 1 format),
     * the direction was {@link AxisDirection#NORTH NORTH}).</p>
     *
     * <p>This axis is usually part of a {@link #GEOCENTRIC_X}, {@link #GEOCENTRIC_Y}, {@link #GEOCENTRIC_Z} set.</p>
     */
    public static final DefaultCoordinateSystemAxis GEOCENTRIC_Z = create(AxisNames.GEOCENTRIC_Z, "Z",
            AxisDirection.GEOCENTRIC_Z, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for Easting values in a {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#EAST East} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"easting"</cite> and the abbreviation is upper case <cite>"E"</cite>.
     *
     * <p>This axis is usually part of a {@link #EASTING}, {@link #NORTHING} set.</p>
     *
     * @see #X
     * @see #EASTING
     * @see #WESTING
     */
    public static final DefaultCoordinateSystemAxis EASTING = create(AxisNames.EASTING, "E",
            AxisDirection.EAST, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for Westing values in a {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#WEST West} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"westing"</cite> and the abbreviation is upper case <cite>"W"</cite>.
     *
     * @see #X
     * @see #EASTING
     * @see #WESTING
     */
    public static final DefaultCoordinateSystemAxis WESTING = create(AxisNames.WESTING, "W",
            AxisDirection.WEST, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for Northing values in a {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#NORTH North} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"northing"</cite> and the abbreviation is upper case <cite>"N"</cite>.
     *
     * <p>This axis is usually part of a {@link #EASTING}, {@link #NORTHING} set.</p>
     *
     * @see #Y
     * @see #NORTHING
     * @see #SOUTHING
     */
    public static final DefaultCoordinateSystemAxis NORTHING = create(AxisNames.NORTHING, "N",
            AxisDirection.NORTH, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for Southing values in a {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     * Increasing ordinates values go {@linkplain AxisDirection#SOUTH South} and units are {@linkplain SI#METRE metres}.
     * The ISO 19111 name is <cite>"southing"</cite> and the abbreviation is upper case <cite>"S"</cite>.
     *
     * @see #Y
     * @see #NORTHING
     * @see #SOUTHING
     */
    public static final DefaultCoordinateSystemAxis SOUTHING = create(AxisNames.SOUTHING, "S",
            AxisDirection.SOUTH, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * An axis with North-East orientation.
     */
    public static final DefaultCoordinateSystemAxis NORTH_EAST = create("North-East", "NE",
            AxisDirection.NORTH_EAST, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * An axis with South-East orientation.
     */
    public static final DefaultCoordinateSystemAxis SOUTH_EAST = create("South-East", "SE",
            AxisDirection.SOUTH_EAST, SI.METRE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * An axis for a distance from an origin.
     * This is part of a polar or engineering spherical coordinate system
     * (not to be confused with geodetic spherical coordinate system).
     *
     * @see #GEOCENTRIC_RADIUS
     */
    public static final DefaultCoordinateSystemAxis DISTANCE = create("Distance", "r",
            AxisDirections.AWAY_FROM, SI.METRE, 0, Double.POSITIVE_INFINITY, RangeMeaning.EXACT);

    /**
     * An axis with clockwise orientation.
     * This is part of a polar or engineering spherical coordinate system
     * (not to be confused with geodetic spherical coordinate system).
     */
    public static final DefaultCoordinateSystemAxis BEARING = create("Bearing", "θ",
            AxisDirections.CLOCKWISE, NonSI.DEGREE_ANGLE, -180, +180, RangeMeaning.WRAPAROUND);

    /**
     * An axis with for elevation angle.
     * This is part of an engineering spherical coordinate system
     * (not to be confused with geodetic spherical coordinate system).
     */
    public static final DefaultCoordinateSystemAxis ELEVATION = create("Elevation", "φ",
            AxisDirection.UP, NonSI.DEGREE_ANGLE, -90, +90, RangeMeaning.WRAPAROUND);

    /**
     * Axis for time values in a {@linkplain org.apache.sis.referencing.cs.DefaultTimeCS time CS}.
     * Increasing time go toward {@linkplain AxisDirection#FUTURE future} and units are {@linkplain NonSI#DAY days}.
     * The abbreviation is lower case <cite>"t"</cite>.
     */
    public static final DefaultCoordinateSystemAxis TIME = create("Time", "t",
            AxisDirection.FUTURE, NonSI.DAY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for column indices in a {@linkplain org.opengis.coverage.grid.GridCoverage grid coverage}.
     * Increasing values go toward {@linkplain AxisDirection#COLUMN_POSITIVE positive column number}.
     * The abbreviation is lower case <cite>"i"</cite>.
     */
    public static final DefaultCoordinateSystemAxis COLUMN = create("Column", "i",
            AxisDirection.COLUMN_POSITIVE, Unit.ONE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for row indices in a {@linkplain org.opengis.coverage.grid.GridCoverage grid coverage}.
     * Increasing values go toward {@linkplain AxisDirection#ROW_POSITIVE positive row number}.
     * The abbreviation is lower case <cite>"j"</cite>.
     */
    public static final DefaultCoordinateSystemAxis ROW = create("Row", "j",
            AxisDirection.ROW_POSITIVE, Unit.ONE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>x</var> values in a display device.
     * Increasing values go toward {@linkplain AxisDirection#DISPLAY_RIGHT display right}.
     * The abbreviation is lower case <cite>"x"</cite>.
     */
    public static final DefaultCoordinateSystemAxis DISPLAY_X = create("x", "x",
            AxisDirection.DISPLAY_RIGHT, Unit.ONE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Axis for <var>y</var> values in a display device.
     * Increasing values go toward {@linkplain AxisDirection#DISPLAY_DOWN display down}.
     * The abbreviation is lower case <cite>"y"</cite>.
     */
    public static final DefaultCoordinateSystemAxis DISPLAY_Y = create("y", "y",
            AxisDirection.DISPLAY_DOWN, Unit.ONE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Undefined or unknown axis. Axis direction is {@link AxisDirection#OTHER OTHER}
     * and the unit is dimensionless. This constant is sometime used as a placeholder
     * for axes that were not properly defined.
     */
    public static final DefaultCoordinateSystemAxis UNDEFINED = create("Undefined", "z",
            AxisDirection.OTHER, Unit.ONE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null);

    /**
     * Creates a new axis of the given name, abbreviation, direction and unit.
     */
    static DefaultCoordinateSystemAxis create(final String name, final String abbreviation,
            final AxisDirection direction, final Unit<?> unit, final double minimum, final double maximum,
            final RangeMeaning meaning)
    {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        properties.put(DefaultCoordinateSystemAxis.NAME_KEY, name);
        properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, minimum);
        properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, maximum);
        properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, meaning);
        return new DefaultCoordinateSystemAxis(properties, abbreviation, direction, unit);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedAxes() {
    }
}
