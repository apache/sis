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
package org.apache.sis.measure;


/**
 * A latitude angle in decimal degrees.
 * Positive latitudes are North, while negative latitudes are South.
 * The latitude symbol is the Greek lower-case letter phi (φ).
 *
 * <p>Because the Earth is not a perfect sphere, there is small differences in the latitude values of a point
 * depending on how the latitude is defined:</p>
 *
 * <ul>
 *   <li><cite>Geodetic latitude</cite> is the angle between the equatorial plane and a line perpendicular
 *       to the {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid} surface.</li>
 *   <li><cite>Geocentric latitude</cite> is the angle between the equatorial plane and a line going from
 *       the Earth center. It differs from geodetic latitude by less than 11 angular minutes.</li>
 *   <li><cite>Astronomical latitude</cite> is the angle between the equatorial plane and a line given
 *       by the direction of a plumb line (the "true vertical").</li>
 *   <li>Above list is not exhaustive. There is also <cite>geomagnetic latitude</cite>, <i>etc.</i></li>
 * </ul>
 *
 * The kind of latitude is unspecified by this {@code Latitude} class, and rather depends on the context:
 * the latitude is <cite>geodetic</cite> if the coordinate reference system is
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic},
 * or <cite>geocentric</cite> if the coordinate reference system is
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric}.
 * If the context is unknown, then geodetic latitude can usually be assumed.
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see Longitude
 * @see AngleFormat
 */
public final class Latitude extends Angle {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2227675003893702061L;

    /**
     * Minimum usual value for latitude ({@value}°).
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMinimumValue()
     */
    public static final double MIN_VALUE = -90;

    /**
     * Maximum usual value for latitude (+{@value}°).
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMaximumValue()
     */
    public static final double MAX_VALUE = +90;

    /**
     * Construct a new latitude with the specified angular value.
     *
     * @param φ Latitude value in decimal degrees.
     */
    public Latitude(final double φ) {
        super(φ);
    }

    /**
     * Constructs a newly allocated {@code Latitude} object that contain the angular value
     * represented by the string. The string should represent an angle in either fractional
     * degrees (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     * The hemisphere (N or S) is optional (default to North).
     *
     * <p>This is a convenience constructor mostly for testing purpose, since it uses a fixed
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
     * instead than this constructor.</p>
     *
     * @param  string A string to be converted to a {@code Latitude}.
     * @throws NumberFormatException if the string does not contain a parsable angle,
     *         or represents a longitude angle.
     *
     * @see AngleFormat#parse(String)
     */
    public Latitude(final String string) throws NumberFormatException {
        super(string);
    }

    /**
     * Returns the hemisphere character for an angle of the given sign.
     * This is used only by {@link #toString()}, not by {@link AngleFormat}.
     */
    @Override
    final char hemisphere(final boolean negative) {
        return negative ? 'S' : 'N';
    }

    /**
     * Upper threshold before to format an angle as an ordinary number.
     * This is used only by {@link #toString()}, not by {@link AngleFormat}.
     */
    @Override
    final double maximum() {
        return 90;
    }

    /**
     * Returns the given latitude value clamped to the [{@linkplain #MIN_VALUE -90} … {@linkplain #MAX_VALUE 90}]° range.
     * If the given value is outside the latitude range, then this method replaces it by ±90° with the same sign than the
     * given φ value.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@linkplain Double#NaN NaN} values are returned unchanged</li>
     *   <li>±∞ are mapped to ±90° (with the same sign)</li>
     *   <li>±0 are returned unchanged (i.e. the sign of negative and positive zero is preserved)</li>
     * </ul>
     *
     * @param  φ The latitude value in decimal degrees.
     * @return The given value clamped to the [-90 … 90]° range, or NaN if the given value was NaN.
     *
     * @see Longitude#normalize(double)
     *
     * @since 0.4
     */
    public static double clamp(final double φ) {
        if (φ < MIN_VALUE) return MIN_VALUE;
        if (φ > MAX_VALUE) return MAX_VALUE;
        return φ;
    }
}
