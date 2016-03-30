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
package org.apache.sis.internal.metadata;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;

// Branch-specific imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Constants for axis names specified by ISO 19111 and ISO 19162.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class AxisNames {
    /**
     * The ISO 19162 <cite>"Longitude"</cite> name.
     */
    public static final String LONGITUDE = "Longitude";

    /**
     * The ISO 19162 <cite>"Latitude"</cite> name.
     */
    public static final String LATITUDE = "Latitude";

    /**
     * The ISO 19111 <cite>"geodetic longitude"</cite> name. Abbreviation is "λ" (lambda).
     */
    public static final String GEODETIC_LONGITUDE = "Geodetic longitude";

    /**
     * The ISO 19111 <cite>"geodetic latitude"</cite> name. Abbreviation is "φ" (phi).
     */
    public static final String GEODETIC_LATITUDE = "Geodetic latitude";

    /**
     * The ISO 19111 <cite>"spherical longitude"</cite> name. Abbreviation used by SIS is "θ" (theta)
     * for consistency with ISO 19162 <cite>Axis name and abbreviation</cite> section,
     * but some other conventions use φ or Ω instead.
     * Direction in the EPSG database is "East", but "counterClockwise" may also be used.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Spherical_coordinate_system">Spherical coordinate system on Wikipedia</a>
     * @see <a href="http://mathworld.wolfram.com/SphericalCoordinates.html">Spherical coordinate system on MathWorld</a>
     */
    public static final String SPHERICAL_LONGITUDE = "Spherical longitude";

    /**
     * The ISO 19111 <cite>"spherical latitude"</cite> name. Abbreviation used by SIS is "φ′" (phi prime)
     * for consistency with ISO 19162 <cite>Axis name and abbreviation</cite> section,
     * but some other conventions use θ, Ω or Ψ instead.
     * Direction in the EPSG database is "North", but the "Up" direction may also be used with a similar
     * axis named "elevation".
     */
    public static final String SPHERICAL_LATITUDE = "Spherical latitude";

    /**
     * The ISO 19111 <cite>"ellipsoidal height"</cite> name. Abbreviation is lower case <cite>"h"</cite>.
     */
    public static final String ELLIPSOIDAL_HEIGHT = "Ellipsoidal height";

    /**
     * The ISO 19111 <cite>"gravity-related height"</cite> name. Abbreviation is upper case <cite>"H"</cite>.
     */
    public static final String GRAVITY_RELATED_HEIGHT = "Gravity-related height";

    /**
     * The ISO 19111 <cite>"depth"</cite> name.
     */
    public static final String DEPTH = "Depth";

    /**
     * The ISO 19111 <cite>"Geocentric X"</cite> name. Abbreviation is upper case <cite>"X"</cite>.
     */
    public static final String GEOCENTRIC_X = "Geocentric X";

    /**
     * The ISO 19111 <cite>"Geocentric Y"</cite> name. Abbreviation is upper case <cite>"Y"</cite>.
     */
    public static final String GEOCENTRIC_Y = "Geocentric Y";

    /**
     * The ISO 19111 <cite>"Geocentric Z"</cite> name. Abbreviation is upper case <cite>"Z"</cite>.
     */
    public static final String GEOCENTRIC_Z = "Geocentric Z";

    /**
     * The ISO 19111 <cite>"geocentric radius"</cite> name. Abbreviation is upper case <cite>"R"</cite>
     * for consistency with EPSG database.
     *
     * <div class="note"><b>Note:</b>Lower case <cite>"r"</cite> is used for non-geocentric radius
     * or axes named "distance" with "awayFrom" direction.</div>
     */
    public static final String GEOCENTRIC_RADIUS = "Geocentric radius";

    /**
     * The ISO 19111 <cite>"easting"</cite> name. Abbreviation is upper case <cite>"E"</cite>.
     */
    public static final String EASTING = "Easting";

    /**
     * The ISO 19111 <cite>"westing"</cite> name. Abbreviation is upper case <cite>"W"</cite>.
     */
    public static final String WESTING = "Westing";

    /**
     * The ISO 19111 <cite>"northing"</cite> name. Abbreviation is upper case <cite>"N"</cite>.
     */
    public static final String NORTHING = "Northing";

    /**
     * The ISO 19111 <cite>"southing"</cite> name. Abbreviation is upper case <cite>"S"</cite>.
     */
    public static final String SOUTHING = "Southing";

    /**
     * A ISO 19162 frequently used name.
     */
    public static final String TIME = "Time";

    /**
     * The map of all of the above values, used for fixing the case.
     * Shall not be modified after construction.
     */
    private static final Map<String,String> VALUES;
    static {
        final Map<String,String> values = new HashMap<String,String>(22);
        final StringBuilder buffer = new StringBuilder(22);     // Length of the longuest string: "Gravity-related height"
        try {
            for (final Field f : AxisNames.class.getFields()) {
                final String name = (String) f.get(null);
                values.put(toUpperCase(name, buffer).intern(), name);
                buffer.setLength(0);
                /*
                 * The call to 'intern()' is because many upper-case strings match the field name
                 * (e.g. "LATITUDE", "NORTHING", "DEPTH", etc.), so we use the same String instance.
                 */
            }
        } catch (IllegalAccessException e) {
            /*
             * Should never happen. But if it happen anyway, do not kill the application for that.
             * We will take the values that we have been able to map so far. The other values will
             * just not have their case fixed.
             */
            Logging.unexpectedException(Logging.getLogger(Modules.REFERENCING), AxisNames.class, "<cinit>", e);
        }
        VALUES = values;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private AxisNames() {
    }

    /**
     * Returns the given axis name in upper case without punctuation characters.
     *
     * @param  name   The axis name to return in upper-case.
     * @param  buffer A temporary buffer to use. Must be initially empty.
     * @return The given name converted to upper-case.
     */
    private static String toUpperCase(final String name, final StringBuilder buffer) {
        for (int i=0; i<name.length(); i++) {
            final char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                buffer.append(Character.toUpperCase(c));
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the given name in camel case if it is one of the know names.
     * This method canonicalizes also the use of {@code '_'}, {@code '-'} and {@code ' '}.
     *
     * <div class="note"><b>Rational:</b>
     * Axis names are not really free text. They are specified by ISO 19111 and ISO 19162.
     * SIS does not put restriction on axis names, but we nevertheless try to use a unique
     * name when we recognize it.</div>
     *
     * @param  name The name in any case.
     * @return The given name in camel case.
     */
    public static String toCamelCase(final String name) {
        return JDK8.getOrDefault(VALUES, toUpperCase(name, new StringBuilder(name.length())), name);
    }
}
