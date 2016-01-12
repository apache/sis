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
 * A longitude angle in decimal degrees.
 * Positive longitudes are East, while negative longitudes are West.
 * The longitude symbol is the Greek lower-case letter lambda (λ).
 *
 * <p>Longitudes are not necessarily relative to the Greenwich meridian. The
 * {@linkplain org.apache.sis.referencing.datum.DefaultPrimeMeridian prime meridian}
 * depends on the context, typically specified through the geodetic datum of a
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see Latitude
 * @see AngleFormat
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
     * @param λ Longitude value in decimal degrees.
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
     * instead than this constructor.</p>
     *
     * @param  string A string to be converted to a {@code Longitude}.
     * @throws NumberFormatException if the string does not contain a parsable angle,
     *         or represents a longitude angle.
     *
     * @see AngleFormat#parse(String)
     */
    public Longitude(final String string) throws NumberFormatException {
        super(string);
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
     * Returns the given longitude value normalized to the [{@linkplain #MIN_VALUE -180} … {@linkplain #MAX_VALUE 180})°
     * range (upper value is exclusive). If the given value is outside the longitude range, then this method adds or
     * subtracts a multiple of 360° in order to bring back the value to that range.
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
     * @param  λ The longitude value in decimal degrees.
     * @return The given value normalized to the [-180 … 180)° range, or NaN if the given value was NaN of infinite.
     *
     * @see Latitude#clamp(double)
     *
     * @since 0.4
     */
    public static double normalize(final double λ) {
        return λ - Math.floor((λ - MIN_VALUE) / (MAX_VALUE - MIN_VALUE)) * (MAX_VALUE - MIN_VALUE);
    }
}
