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

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.math.MathFunctions;


/**
 * A longitude angle in decimal degrees.
 * Positive longitudes are East, while negative longitudes are West.
 * The longitude symbol is the Greek lower-case letter lambda (λ).
 *
 * <p>Longitudes are not necessarily relative to the Greenwich meridian. The
 * {@linkplain org.apache.sis.referencing.datum.DefaultPrimeMeridian prime meridian}
 * depends on the context, typically specified through the geodetic datum of a
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.</p>
 *
 * <h2>Immutability and thread safety</h2>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 *
 * @see Latitude
 * @see AngleFormat
 * @see org.apache.sis.geometry.CoordinateFormat
 *
 * @since 0.3
 * @module
 */
public final class Longitude extends Angle {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3203511772374891877L;

    /**
     * Minimum usual value for longitude ({@value}°).
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMinimumValue()
     */
    public static final double MIN_VALUE = -180;

    /**
     * Maximum usual value for longitude (+{@value}°).
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getMaximumValue()
     */
    public static final double MAX_VALUE = +180;

    /**
     * Construct a new longitude with the specified angular value.
     * This constructor does <strong>not</strong> {@linkplain #normalize(double) normalize} the given value.
     *
     * @param  λ  longitude value in decimal degrees.
     */
    public Longitude(final double λ) {
        super(λ);
    }

    /**
     * Constructs a newly allocated {@code Longitude} object that contain the angular value
     * represented by the string. The string should represent an angle in either fractional
     * degrees (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     * The hemisphere (E or W) is optional (default to East).
     *
     * <p>This is a convenience constructor mostly for testing purpose, since it uses a fixed
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
     * instead of this constructor.</p>
     *
     * @param  string  a string to be converted to a {@code Longitude}.
     * @throws NumberFormatException if the string does not contain a parsable angle,
     *         or represents a longitude angle.
     *
     * @see AngleFormat#parse(String)
     */
    public Longitude(final String string) throws NumberFormatException {
        super(string);
    }

    /**
     * Constructs a newly allocated object containing the longitude value of the given position.
     * For this method, the longitude value is defined as the angular value associated to the first axis
     * oriented toward {@linkplain AxisDirection#EAST East} or {@linkplain AxisDirection#WEST West}.
     * Note that this is not necessarily the <cite>geodetic longitudes</cite> used in
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS};
     * it may also be <cite>geocentric longitudes</cite>.
     *
     * <p>If the axis direction is West, then the sign of the coordinate value is inverted.
     * If the coordinate value uses another angular units than {@linkplain Units#DEGREE degrees},
     * then a unit conversion is applied.</p>
     *
     * @param  position  the coordinate from which to extract the longitude value in degrees.
     * @throws IllegalArgumentException if the given coordinate it not associated to a CRS,
     *         or if no axis oriented toward East or West is found, or if that axis does
     *         not use {@linkplain Units#isAngular angular units}.
     *
     * @since 0.8
     */
    public Longitude(final DirectPosition position) throws IllegalArgumentException {
        super(valueOf(position, AxisDirection.EAST, AxisDirection.WEST));
    }

    /**
     * Returns the hemisphere character for an angle of the given sign.
     * This is used only by {@link #toString()}, not by {@link AngleFormat}.
     */
    @Override
    final char hemisphere(final boolean negative) {
        return negative ? 'W' : 'E';
    }

    /**
     * Returns the given longitude value normalized to the [{@value #MIN_VALUE} … {@value #MAX_VALUE})°
     * range (upper value is exclusive). If the given value is outside the longitude range, then this
     * method adds or subtracts a multiple of 360° in order to bring back the value to that range.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@linkplain Double#NaN NaN} values are returned unchanged</li>
     *   <li>±∞ are mapped to NaN</li>
     *   <li>±180° are both mapped to -180° (i.e. the range is from -180° inclusive to +180° <em>exclusive</em>)</li>
     *   <li>±0 are returned unchanged (i.e. the sign of negative and positive zero is preserved)</li>
     * </ul>
     *
     * Note that the given value should not be greater than 4×10⁸ degrees if a centimetric precision is desired.
     *
     * @param  λ  the longitude value in decimal degrees.
     * @return the given value normalized to the [-180 … 180)° range, or NaN if the given value was NaN of infinite.
     *
     * @see Latitude#clamp(double)
     *
     * @since 0.4
     */
    public static double normalize(double λ) {
        /*
         * Following should be simplified as only one branch by javac since
         * the values used in the `if` statement are compile-time constants.
         * For verifying: javap -c org.apache.sis.measure.Longitude
         */
        if (MIN_VALUE == -MAX_VALUE) {
            λ = Math.IEEEremainder(λ, MAX_VALUE - MIN_VALUE);
            if (λ == MAX_VALUE) λ = MIN_VALUE;
            return λ;
        } else {
            // Normally excluded from compiled file, but defined in case someone modifies MIN/MAX_VALUE.
            return λ - Math.floor((λ - MIN_VALUE) / (MAX_VALUE - MIN_VALUE)) * (MAX_VALUE - MIN_VALUE);
        }
    }

    /**
     * Returns {@code true} if the given longitude range crosses the anti-meridian in a way expressed by
     * <var>west</var> &gt; <var>east</var>. For the purpose of this method, +0 is considered "greater" than −0.
     * See {@link org.apache.sis.geometry.GeneralEnvelope} for a wraparound illustration.
     *
     * @param  west  the west bound longitude. This is the minimum value when there is no wraparound.
     * @param  east  the east bound longitude. This is the maximum value when there is no wraparound.
     * @return {@code true} if <var>west</var> &gt; <var>east</var> or if the arguments are (+0, −0),
     *         {@code false} otherwise (including when at least one argument is NaN).
     *
     * @since 1.1
     */
    public static boolean isWraparound(final double west, final double east) {
        return (west > east) || (MathFunctions.isPositiveZero(west) &&
                                 MathFunctions.isNegativeZero(east));
    }
}
