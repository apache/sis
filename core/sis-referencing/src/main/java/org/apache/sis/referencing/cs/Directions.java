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
import org.apache.sis.util.Static;

import static org.apache.sis.util.CharSequences.*;
import static org.opengis.referencing.cs.AxisDirection.*;


/**
 * Returns the direction for a pre-defined name. This class search for a match in the set of
 * known axis directions as returned by {@link AxisDirections#values()}, plus a few special
 * cases like "<cite>Geocentre &gt; equator/90°E</cite>". The later are used in the EPSG
 * database for geocentric CRS.
 *
 * <p>This class does not know about {@link DirectionAlongMeridian}. The later is a parser
 * which may create new directions, while the {@code Directions} class searches only in a
 * set of predefined directions and never create new ones.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
final class Directions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Directions() {
    }

    /**
     * Returns the direction for the given name, or {@code null} if unknown.
     * This method searches in the set of pre-defined axis names.
     */
    static AxisDirection find(String name) {
        name = trimWhitespaces(name);
        final AxisDirection[] directions = AxisDirection.values();
        AxisDirection candidate = find(directions, name);
        if (candidate == null) {
            /*
             * No match found when using the pre-defined axis name. Searches among
             * the set of geocentric directions. Expected directions are:
             *
             *    Geocentre > equator/PM      or    Geocentre > equator/0°E
             *    Geocentre > equator/90dE    or    Geocentre > equator/90°E
             *    Geocentre > north pole
             */
            int s = name.indexOf('>');
            if (s >= 0 && equalsIgnoreCase(name, 0, s, "Geocentre")) {
                String tail = name.substring(skipLeadingWhitespaces(name, s+1, name.length())).replace('_', ' ');
                s = tail.indexOf('/');
                if (s < 0) {
                    if (tail.equalsIgnoreCase("north pole")) {
                        return GEOCENTRIC_Z;
                    }
                } else if (equalsIgnoreCase(tail, 0, s, "equator")) {
                    tail = tail.substring(skipLeadingWhitespaces(tail, s+1, tail.length()));
                    if (tail.equalsIgnoreCase("PM")) {
                        return GEOCENTRIC_X;
                    }
                    // Limit the scan to 6 characters for avoiding a NumberFormatException.
                    final int length = Math.min(tail.length(), 6);
                    for (s=0; s<length; s++) {
                        final char c = tail.charAt(s);
                        if (c < '0' || c > '9') {
                            if (s == 0) break;
                            final int n = Integer.parseInt(tail.substring(0, s));
                            tail = tail.substring(skipLeadingWhitespaces(tail, s, tail.length())).replace('d', '°');
                            if (tail.equalsIgnoreCase("°E")) {
                                switch (n) {
                                    case  0: return GEOCENTRIC_X;
                                    case 90: return GEOCENTRIC_Y;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            /*
             * No match found in the set of geocentric directions neither. Searches
             * again among the standard set of names, replacing space by underscore.
             */
            String modified = name.replace('-', '_');
            if (modified != name) { // NOSONAR: really identity comparison
                name = modified;
                candidate = find(directions, modified);
            }
            if (candidate == null) {
                modified = name.replace(' ', '_');
                if (modified != name) { // NOSONAR: really identity comparison
                    candidate = find(directions, modified);
                }
            }
        }
        return candidate;
    }

    /**
     * Returns {@code true} if the given sub-sequence is equal to the given keyword, ignoring case
     * and trailing whitespaces.
     */
    private static boolean equalsIgnoreCase(final String name, final int lower, int upper, final String keyword) {
        upper = skipTrailingWhitespaces(name, lower, upper);
        final int length = upper - lower;
        return (length == keyword.length()) && name.regionMatches(true, lower, keyword, 0, length);
    }

    /**
     * Searches for the specified name in the specified set of directions.
     * This method accepts abbreviation as well, for example if the given
     * {@code name} is "W", then it will be recognized as "West".
     */
    static AxisDirection find(final AxisDirection[] directions, final String name) {
        final int length = name.length();
search: for (final AxisDirection candidate : directions) {
            final String identifier = candidate.name();
            if (name.equalsIgnoreCase(identifier)) {
                return candidate;
            }
            /*
             * Not an exact match. We will compare as an abbreviation. We process by iterating
             * over the characters of the identifier (e.g. "SOUTH_SOUTH_WEST"). For each first
             * letter or digit of a new word,  compare with a next character of the given name
             * ("SSW" if we have a match for the above example).
             */
            final int idLength = identifier.length();
            boolean isWordStart = false;
            int c, ni=0;
            for (int i=0; i<idLength; i += Character.charCount(c)) {
                c = identifier.codePointAt(i);
                if (Character.isLetterOrDigit(c) != isWordStart) {
                    isWordStart = !isWordStart;
                    if (isWordStart) {
                        if (ni >= length) {
                            continue search; // Not enough characters in the given name.
                        }
                        final int ci = Character.toUpperCase(name.codePointAt(ni));
                        if (Character.toUpperCase(c) != ci) {
                            continue search; // No match. Continue with the next direction to check.
                        }
                        ni += Character.charCount(ci);
                    }
                }
            }
            if (ni == length) {
                return candidate;
            }
        }
        return null;
    }
}
