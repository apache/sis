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

import net.jcip.annotations.Immutable;


/**
 * A longitude angle in decimal degrees.
 * Positive longitudes are East, while negative longitudes are West.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see Latitude
 * @see AngleFormat
 */
@Immutable
public final class Longitude extends Angle {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8614900608052762636L;

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
     *
     * @param θ Angle in decimal degrees.
     */
    public Longitude(final double θ) {
        super(θ);
    }

    /**
     * Constructs a newly allocated {@code Longitude} object that contain the angular value
     * represented by the string. The string should represent an angle in either fractional
     * degrees (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     * The hemisphere (E or W) is optional (default to East).
     *
     * <p>This is a convenience constructor mostly for testing purpose, since it uses a fixed
     * locale. Developers should consider using {@link AngleFormat} for end-user applications
     * instead than this constructor.</p>
     *
     * @param  string A string to be converted to a {@code Longitude}.
     * @throws NumberFormatException if the string does not contain a parsable angle,
     *         or represents a latitude angle.
     *
     * @see AngleFormat#parse(String)
     */
    public Longitude(final String string) throws NumberFormatException {
        super(string);
    }
}
