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
import javax.measure.unit.SI;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.measure.Units;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;


/**
 * Collection of coordinate systems for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class HardCodedCS {
    /**
     * A two-dimensional ellipsoidal CS with
     * <var>{@linkplain HardCodedAxes#GEODETIC_LATITUDE geodetic latitude}</var>,
     * <var>{@linkplain HardCodedAxes#GEODETIC_LONGITUDE geodetic longitude}</var>
     * axes in decimal degrees.
     */
    public static final DefaultEllipsoidalCS GEODETIC_φλ = new DefaultEllipsoidalCS(
            singletonMap(NAME_KEY, "Geodetic 2D (φ,λ)"),
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.GEODETIC_LONGITUDE);

    /**
     * A two-dimensional ellipsoidal CS with
     * <var>{@linkplain HardCodedAxes#GEODETIC_LONGITUDE geodetic longitude}</var>,
     * <var>{@linkplain HardCodedAxes#GEODETIC_LATITUDE geodetic latitude}</var>
     * axes in decimal degrees.
     */
    public static final DefaultEllipsoidalCS GEODETIC_2D = new DefaultEllipsoidalCS(
            singletonMap(NAME_KEY, "Geodetic 2D"),
            HardCodedAxes.GEODETIC_LONGITUDE,
            HardCodedAxes.GEODETIC_LATITUDE);

    /**
     * A three-dimensional ellipsoidal CS with
     * <var>{@linkplain HardCodedAxes#GEODETIC_LONGITUDE geodetic longitude}</var>,
     * <var>{@linkplain HardCodedAxes#GEODETIC_LATITUDE geodetic latitude}</var>,
     * <var>{@linkplain HardCodedAxes#ELLIPSOIDAL_HEIGHT ellipsoidal height}</var>
     * axes.
     */
    public static final DefaultEllipsoidalCS GEODETIC_3D = new DefaultEllipsoidalCS(
            singletonMap(NAME_KEY, "Geodetic 3D"),
            HardCodedAxes.GEODETIC_LONGITUDE,
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.ELLIPSOIDAL_HEIGHT);

    /**
     * A two-dimensional ellipsoidal CS with
     * <var>{@linkplain HardCodedAxes#LONGITUDE_gon longitude}</var>,
     * <var>{@linkplain HardCodedAxes#LATITUDE_gon latitude}</var>
     * axes (without "Geodetic" prefix) in gradians.
     *
     * <p>This coordinate system is used for testing unit conversions without axes swapping.</p>
     */
    public static final DefaultEllipsoidalCS ELLIPSOIDAL_gon = new DefaultEllipsoidalCS(
            singletonMap(NAME_KEY, "Ellipsoidal (gon)"),
            HardCodedAxes.LONGITUDE_gon,
            HardCodedAxes.LATITUDE_gon);

    /**
     * A three-dimensional spherical CS for geodetic use with
     * <var>{@linkplain HardCodedAxes#SPHERICAL_LATITUDE latitude}</var>,
     * <var>{@linkplain HardCodedAxes#SPHERICAL_LONGITUDE longitude}</var>,
     * <var>{@linkplain HardCodedAxes#GEOCENTRIC_RADIUS radius}</var> axes.
     * This axis order is the one of EPSG:6404.
     * Note that this is not a right-handed system.
     *
     * @see #SPHERICAL_ENGINEERING
     */
    public static final DefaultSphericalCS SPHERICAL = new DefaultSphericalCS(
            singletonMap(NAME_KEY, "Spherical"),
            HardCodedAxes.SPHERICAL_LATITUDE,
            HardCodedAxes.SPHERICAL_LONGITUDE,
            HardCodedAxes.GEOCENTRIC_RADIUS);

    /**
     * A three-dimensional spherical CS for geodetic use with
     * <var>{@linkplain HardCodedAxes#DISTANCE distance}</var>,
     * <var>{@linkplain HardCodedAxes#BEARING bearing}</var>,
     * <var>{@linkplain HardCodedAxes#ELEVATION elevation}</var> axes.
     */
    public static final DefaultSphericalCS SPHERICAL_ENGINEERING = new DefaultSphericalCS(
            singletonMap(NAME_KEY, SPHERICAL.getName()),
            HardCodedAxes.DISTANCE,
            HardCodedAxes.BEARING,
            HardCodedAxes.ELEVATION);

    /**
     * A three-dimensional Cartesian CS with geocentric
     * <var>{@linkplain HardCodedAxes#GEOCENTRIC_X x}</var>,
     * <var>{@linkplain HardCodedAxes#GEOCENTRIC_Y y}</var>,
     * <var>{@linkplain HardCodedAxes#GEOCENTRIC_Z z}</var>
     * axes in metres.
     */
    public static final DefaultCartesianCS GEOCENTRIC = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Geocentric"),
            HardCodedAxes.GEOCENTRIC_X,
            HardCodedAxes.GEOCENTRIC_Y,
            HardCodedAxes.GEOCENTRIC_Z);

    /**
     * A three-dimensional cylindrical CS with
     * <var>{@linkplain HardCodedAxes#DISTANCE distance}</var>,
     * <var>{@link HardCodedAxes#BEARING bearing}</var>,
     * <var>{@linkplain HardCodedAxes#Z z}</var> axes.
     * Note that this is not a right-handed system.
     *
     * @since 0.7
     */
    public static final DefaultCylindricalCS CYLINDRICAL = new DefaultCylindricalCS(
            singletonMap(NAME_KEY, "Cylindrical"),
            HardCodedAxes.DISTANCE,
            HardCodedAxes.BEARING,
            HardCodedAxes.Z);

    /**
     * A three-dimensional polar CS with
     * <var>{@linkplain HardCodedAxes#DISTANCE distance}</var>,
     * <var>{@link HardCodedAxes#BEARING bearing}</var> axes.
     * Note that this is not a right-handed system.
     *
     * @since 0.7
     */
    public static final DefaultPolarCS POLAR = new DefaultPolarCS(
            singletonMap(NAME_KEY, "Polar"),
            HardCodedAxes.DISTANCE,
            HardCodedAxes.BEARING);

    /**
     * A two-dimensional Cartesian CS with
     * <var>{@linkplain HardCodedAxes#EASTING Easting}</var>,
     * <var>{@linkplain HardCodedAxes#NORTHING Northing}</var>
     * axes in metres.
     */
    public static final DefaultCartesianCS PROJECTED = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Projected"),
            HardCodedAxes.EASTING,
            HardCodedAxes.NORTHING);

    /**
     * A two-dimensional Cartesian CS with
     * <var>{@linkplain HardCodedAxes#X x}</var>,
     * <var>{@linkplain HardCodedAxes#Y y}</var>
     * axes in metres.
     */
    public static final DefaultCartesianCS CARTESIAN_2D = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Cartesian 2D"),
            HardCodedAxes.X,
            HardCodedAxes.Y);

    /**
     * A three-dimensional Cartesian CS with
     * <var>{@linkplain HardCodedAxes#X x}</var>,
     * <var>{@linkplain HardCodedAxes#Y y}</var>,
     * <var>{@linkplain HardCodedAxes#Z z}</var>
     * axes in metres.
     */
    public static final DefaultCartesianCS CARTESIAN_3D = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Cartesian 3D"),
            HardCodedAxes.X,
            HardCodedAxes.Y,
            HardCodedAxes.Z);

    /**
     * A two-dimensional Cartesian CS with
     * <var>{@linkplain HardCodedAxes#COLUMN column}</var>,
     * <var>{@linkplain HardCodedAxes#ROW row}</var>
     * axes.
     */
    public static final DefaultCartesianCS GRID = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Grid"),
            HardCodedAxes.COLUMN,
            HardCodedAxes.ROW);

    /**
     * A two-dimensional Cartesian CS with
     * <var>{@linkplain HardCodedAxes#DISPLAY_X display x}</var>,
     * <var>{@linkplain HardCodedAxes#DISPLAY_Y display y}</var>
     * axes.
     */
    public static final DefaultCartesianCS DISPLAY = new DefaultCartesianCS(
            singletonMap(NAME_KEY, "Display"),
            HardCodedAxes.DISPLAY_X,
            HardCodedAxes.DISPLAY_Y);

    /**
     * A one-dimensional vertical CS with
     * <var>{@linkplain HardCodedAxes#ELLIPSOIDAL_HEIGHT ellipsoidal height}</var>
     * axis in metres.
     */
    public static final DefaultVerticalCS ELLIPSOIDAL_HEIGHT = new DefaultVerticalCS(
            getProperties(HardCodedAxes.ELLIPSOIDAL_HEIGHT), HardCodedAxes.ELLIPSOIDAL_HEIGHT);

    /**
     * A one-dimensional vertical CS with
     * <var>{@linkplain HardCodedAxes#ELLIPSOIDAL_HEIGHT ellipsoidal height}</var>
     * axis in centimetres.
     *
     * @since 0.7
     */
    public static final DefaultVerticalCS ELLIPSOIDAL_HEIGHT_cm = new DefaultVerticalCS(
            getProperties(HardCodedAxes.ELLIPSOIDAL_HEIGHT_cm), HardCodedAxes.ELLIPSOIDAL_HEIGHT_cm);

    /**
     * A one-dimensional vertical CS with
     * <var>{@linkplain HardCodedAxes#GRAVITY_RELATED_HEIGHT gravity-related height}</var>
     * axis in metres.
     */
    public static final DefaultVerticalCS GRAVITY_RELATED_HEIGHT = new DefaultVerticalCS(
            getProperties(HardCodedAxes.GRAVITY_RELATED_HEIGHT), HardCodedAxes.GRAVITY_RELATED_HEIGHT);

    /**
     * A one-dimensional vertical CS with
     * <var>{@linkplain HardCodedAxes#DEPTH depth}</var>
     * axis in metres.
     */
    public static final DefaultVerticalCS DEPTH = new DefaultVerticalCS(
            getProperties(HardCodedAxes.DEPTH), HardCodedAxes.DEPTH);

    /**
     * A one-dimensional temporal CS with
     * <var>{@linkplain HardCodedAxes#TIME time}</var>,
     * axis in {@linkplain javax.measure.unit.NonSI#DAY day} units.
     */
    public static final DefaultTimeCS DAYS;

    /**
     * A one-dimensional temporal CS with
     * <var>{@linkplain HardCodedAxes#TIME time}</var>,
     * axis in {@linkplain javax.measure.unit.SI#SECOND second} units.
     */
    public static final DefaultTimeCS SECONDS;

    /**
     * A one-dimensional temporal CS with
     * <var>{@linkplain HardCodedAxes#TIME time}</var>,
     * axis in millisecond units.
     */
    public static final DefaultTimeCS MILLISECONDS;
    static {
        final Map<String,?> properties = getProperties(HardCodedAxes.TIME);
        DAYS         = new DefaultTimeCS(properties, HardCodedAxes.TIME);
        SECONDS      = new DefaultTimeCS(properties, new DefaultCoordinateSystemAxis(properties, "t", AxisDirection.FUTURE, SI.SECOND));
        MILLISECONDS = new DefaultTimeCS(properties, new DefaultCoordinateSystemAxis(properties, "t", AxisDirection.FUTURE, Units.MILLISECOND));
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedCS() {
    }
}
