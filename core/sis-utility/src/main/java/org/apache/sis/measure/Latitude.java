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

import org.apache.sis.util.Immutable;


/**
 * A latitude angle in decimal degrees.
 * Positive latitudes are North, while negative latitudes are South.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see Longitude
 * @see AngleFormat
 */
@Immutable
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
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
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
}
