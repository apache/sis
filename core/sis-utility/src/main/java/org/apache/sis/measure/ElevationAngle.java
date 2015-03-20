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

import org.opengis.referencing.cs.AxisDirection; // For javadoc


/**
 * The angular height of an object measured from the horizontal plane.
 * The elevation angle is part of <cite>local topocentric coordinates</cite> together with azimuth and distance.
 * For visible objects the elevation is an angle between 0° and 90°.
 *
 * <div class="note"><b>Note:</b>
 * <cite>Elevation angle</cite> and <cite>altitude angle</cite> may be used interchangeably.
 * Both <cite>altitude</cite> and <cite>elevation</cite> words are also used to describe the
 * height in meters above sea level.</div>
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.cs.CoordinateSystems#angle(AxisDirection, AxisDirection)
 */
public final class ElevationAngle extends Angle {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 442355803542468396L;

    /**
     * An elevation angle of 90° for an imaginary point directly above a location.
     * This is the opposite of {@link #NADIR} direction.
     */
    public static final ElevationAngle ZENITH = new ElevationAngle(90);

    /**
     * An elevation angle of -90° for an imaginary point directly below a location.
     * This is the opposite of {@link #ZENITH} direction.
     */
    public static final ElevationAngle NADIR = new ElevationAngle(-90);

    /**
     * Constructs a new elevation angle with the specified angular value.
     *
     * @param ε Elevation angle value in decimal degrees.
     */
    public ElevationAngle(final double ε) {
        super(ε);
    }

    /**
     * Constructs a newly allocated {@code ElevationAngle} object that contain the angular value
     * represented by the string. The string should represent an angle in either fractional degrees
     * (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     *
     * @param  string A string to be converted to an {@code ElevationAngle}.
     * @throws NumberFormatException if the string does not contain a parsable angle,
     *         or represents an elevation angle.
     */
    public ElevationAngle(final String string) throws NumberFormatException {
        super(string);
    }
}
