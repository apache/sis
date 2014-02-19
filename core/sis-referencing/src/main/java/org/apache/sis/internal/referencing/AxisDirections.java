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
package org.apache.sis.internal.referencing;

import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Static;

import static org.opengis.referencing.cs.AxisDirection.*;
import static org.apache.sis.util.CharSequences.*;


/**
 * Utilities methods related to {@link AxisDirection}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.13)
 * @version 0.4
 * @module
 */
public final class AxisDirections extends Static {
    /**
     * Number of directions like "North", "North-North-East", "North-East", etc.
     * The first of those directions is {@link AxisDirection#NORTH}.
     */
    public static final int COMPASS_COUNT = 16;

    /**
     * Number of geocentric directions.
     * The first of those directions is {@link AxisDirection#GEOCENTRIC_X}.
     */
    public static final int GEOCENTRIC_COUNT = 3;

    /**
     * Number of directions like "Display right", "Display down", etc.
     * The first of those directions is {@link AxisDirection#DISPLAY_RIGHT}.
     */
    public static final int DISPLAY_COUNT = 4;

    /**
     * For each direction, the opposite direction.
     */
    private static final AxisDirection[] OPPOSITES = new AxisDirection[DISPLAY_DOWN.ordinal() + 1];
    static {
        put(OTHER,            OTHER);
        put(NORTH,            SOUTH);
        put(NORTH_NORTH_EAST, SOUTH_SOUTH_WEST);
        put(NORTH_EAST,       SOUTH_WEST);
        put(EAST_NORTH_EAST,  WEST_SOUTH_WEST);
        put(EAST,             WEST);
        put(EAST_SOUTH_EAST,  WEST_NORTH_WEST);
        put(SOUTH_EAST,       NORTH_WEST);
        put(SOUTH_SOUTH_EAST, NORTH_NORTH_WEST);
        put(UP,               DOWN);
        put(FUTURE,           PAST);
        put(COLUMN_POSITIVE,  COLUMN_NEGATIVE);
        put(ROW_POSITIVE,     ROW_NEGATIVE);
        put(DISPLAY_RIGHT,    DISPLAY_LEFT);
        put(DISPLAY_UP,       DISPLAY_DOWN);
    }

    /**
     * Stores the given directions in the {@link #OPPOSITES} array.
     */
    private static void put(final AxisDirection dir, final AxisDirection opposite) {
        OPPOSITES[dir.ordinal()] = opposite;
        OPPOSITES[opposite.ordinal()] = dir;
    }

    /**
     * Ordinal of the last element in the {@link AxisDirection} code list.
     * This is used for differentiating the standard codes from the user-defined ones.
     */
    private static final int LAST_ORDINAL = DISPLAY_DOWN.ordinal();

    /**
     * Do not allow instantiation of this class.
     */
    private AxisDirections() {
    }

    /**
     * Returns the "absolute" direction of the given direction.
     * This "absolute" operation is similar to the {@code Math.abs(int)} method in that "negative" directions like
     * ({@code SOUTH}, {@code WEST}, {@code DOWN}, {@code PAST}) are changed for their "positive" counterparts
     * ({@code NORTH}, {@code EAST}, {@code UP}, {@code FUTURE}).
     * More specifically, the following conversion table is applied:
     *
     * <table class="compact"><tr>
     * <td><table class="sis">
     *   <tr>
     *     <th width='50%'>Direction</th>
     *     <th width='50%'>Absolute value</th>
     *   </tr>
     *   <tr><td width='50%'>{@code NORTH}</td> <td width='50%'>{@code NORTH}</td> </tr>
     *   <tr><td width='50%'>{@code SOUTH}</td> <td width='50%'>{@code NORTH}</td> </tr>
     *   <tr><td width='50%'>{@code EAST}</td>  <td width='50%'>{@code EAST}</td>  </tr>
     *   <tr><td width='50%'>{@code WEST}</td>  <td width='50%'>{@code EAST}</td>  </tr>
     *   <tr><td width='50%'>{@code UP}</td>    <td width='50%'>{@code UP}</td>    </tr>
     *   <tr><td width='50%'>{@code DOWN}</td>  <td width='50%'>{@code UP}</td>    </tr>
     * </table></td>
     * <td width='50%'><table class="sis">
     *   <tr>
     *     <th width='50%'>Direction</th>
     *     <th width='50%'>Absolute value</th>
     *   </tr>
     *   <tr><td width='50%'>{@code DISPLAY_RIGHT}</td> <td width='50%'>{@code DISPLAY_RIGHT}</td> </tr>
     *   <tr><td width='50%'>{@code DISPLAY_LEFT}</td>  <td width='50%'>{@code DISPLAY_RIGHT}</td> </tr>
     *   <tr><td width='50%'>{@code DISPLAY_UP}</td>    <td width='50%'>{@code DISPLAY_UP}</td>    </tr>
     *   <tr><td width='50%'>{@code DISPLAY_DOWN}</td>  <td width='50%'>{@code DISPLAY_UP}</td>    </tr>
     *   <tr><td width='50%'>{@code FUTURE}</td>        <td width='50%'>{@code FUTURE}</td>        </tr>
     *   <tr><td width='50%'>{@code PAST}</td>          <td width='50%'>{@code FUTURE}</td>        </tr>
     * </table></td></tr>
     *   <tr align="center"><td width='50%'>{@code OTHER}</td><td width='50%'>{@code OTHER}</td></tr>
     * </table>
     *
     * @param  dir The direction for which to return the absolute direction, or {@code null}.
     * @return The direction from the above table, or {@code null} if the given direction was null.
     */
    public static AxisDirection absolute(final AxisDirection dir) {
        final AxisDirection opposite = opposite(dir);
        if (opposite != null) {
            if (opposite.ordinal() < dir.ordinal()) {
                return opposite;
            }
        }
        return dir;
    }

    /**
     * Returns the opposite direction of the given direction. The opposite direction of
     * {@code NORTH} is {@code SOUTH}, and the opposite direction of {@code SOUTH} is {@code NORTH}.
     * The same applies to {@code EAST}-{@code WEST}, {@code UP}-{@code DOWN} and {@code FUTURE}-{@code PAST},
     * <i>etc.</i> If the given axis direction has no opposite, then this method returns {@code null}.
     *
     * @param  dir The direction for which to return the opposite direction, or {@code null}.
     * @return The opposite direction, or {@code null} if none or unknown.
     */
    public static AxisDirection opposite(AxisDirection dir) {
        if (dir != null) {
            final int ordinal = dir.ordinal();
            if (ordinal >= 0 && ordinal < OPPOSITES.length) {
                dir = OPPOSITES[ordinal];
            }
        }
        return dir;
    }

    /**
     * Returns {@code true} if the given direction is an "opposite" direction.
     * If the given argument is {@code null} or is not a known direction, then
     * this method conservatively returns {@code false}.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is an "opposite".
     */
    public static boolean isOpposite(final AxisDirection dir) {
        final AxisDirection opposite = opposite(dir);
        return (opposite != null) && opposite.ordinal() < dir.ordinal();
    }

    /**
     * Returns {@code true} if the specified direction is a compass direction.
     * Compass directions are {@code NORTH}, {@code EAST}, {@code NORTH_EAST}, etc.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is a compass direction.
     *
     * @see #angleForCompass(AxisDirection, AxisDirection)
     */
    public static boolean isCompass(final AxisDirection dir) {
        if (dir == null) return false;
        final int n  = dir.ordinal() - NORTH.ordinal();
        return n >= 0 && n < COMPASS_COUNT;
    }

    /**
     * Returns {@code true} if the specified direction is an inter-cardinal direction.
     * Inter-cardinal directions are {@code NORTH_EAST}, {@code SOUTH_SOUTH_EAST}, etc.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is an inter-cardinal direction.
     */
    public static boolean isIntercardinal(final AxisDirection dir) {
        if (dir == null) return false;
        final int n  = dir.ordinal() - NORTH.ordinal();
        return n >= 0 && n < COMPASS_COUNT && (n & 3) != 0;
    }

    /**
     * Returns {@code true} if the given direction is {@code UP} or {@code DOWN}.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the direction is vertical, or {@code false} otherwise.
     */
    public static boolean isVertical(final AxisDirection dir) {
        if (dir == null) return false;
        return ((dir.ordinal() - UP.ordinal()) & ~1) == 0;
    }

    /**
     * Returns {@code true} if the given direction is {@code GEOCENTRIC_X}, {@code GEOCENTRIC_Y}
     * or {@code GEOCENTRIC_Z}.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is one of geocentric directions.
     */
    public static boolean isGeocentric(final AxisDirection dir) {
        if (dir == null) return false;
        final int ordinal = dir.ordinal();
        return ordinal >= GEOCENTRIC_X.ordinal() && ordinal <= GEOCENTRIC_Z.ordinal();
    }

    /**
     * Returns {@code true} if the given direction is a spatial axis direction (including vertical and geocentric axes).
     * The current implementation conservatively returns {@code true} for every non-null directions except a hard-coded
     * set of directions which are known to be non-spatial. We conservatively accept unknown axis directions because
     * some of them are created from strings like "South along 90°E".
     *
     * <p>If the {@code image} argument is {@code true}, then this method additionally accepts grid and display
     * axis directions.</p>
     *
     * <p>The rules implemented by this method may change in any future SIS version.</p>
     *
     * @param  dir The direction to test, or {@code null}.
     * @param  image {@code true} for accepting grid and image axis directions in addition to spatial ones.
     * @return {@code true} if the given direction is presumed for spatial CS.
     */
    public static boolean isSpatialOrUserDefined(final AxisDirection dir, final boolean image) {
        if (dir == null) return false;
        final int ordinal = dir.ordinal();
        return ordinal < FUTURE.ordinal() || ordinal > (image ? PAST : DISPLAY_DOWN).ordinal();
    }

    /**
     * Returns {@code true} if the given direction is a user-defined direction (i.e. is not defined by GeoAPI).
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is user-defined.
     */
    public static boolean isUserDefined(final AxisDirection dir) {
        return (dir != null) && dir.ordinal() > LAST_ORDINAL;
    }

    /**
     * Returns {@code true} if the given direction is {@code COLUMN_POSITIVE}, {@code COLUMN_NEGATICE},
     * {@code ROW_POSITIVE} or {@code ROW_NEGATIVE}.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is presumed for grid CS.
     */
    public static boolean isGrid(final AxisDirection dir) {
        if (dir == null) return false;
        final int ordinal = dir.ordinal();
        return ordinal >= COLUMN_POSITIVE.ordinal() && ordinal <= ROW_NEGATIVE.ordinal();
    }

    /**
     * Angle between geocentric directions only.
     *
     * @param  source The start direction.
     * @param  target The final direction.
     * @return The angle as a multiple of 90°, or {@link Integer#MIN_VALUE} if none.
     */
    public static int angleForGeocentric(final AxisDirection source, final AxisDirection target) {
        final int base = GEOCENTRIC_X.ordinal();
        final int src  = source.ordinal() - base;
        if (src >= 0 && src < GEOCENTRIC_COUNT) {
            final int tgt = target.ordinal() - base;
            if (tgt >= 0 && tgt < GEOCENTRIC_COUNT) {
                int n = (tgt - src);
                n -= GEOCENTRIC_COUNT * (n/2); // If -2 add 3.  If +2 subtract 3.  Otherwise do nothing.
                return n;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Angle between compass directions only (not for angle between direction along meridians).
     *
     * @param  source The start direction.
     * @param  target The final direction.
     * @return The arithmetic angle as a multiple of 360/{@link #COMPASS_COUNT}, or {@link Integer#MIN_VALUE} if none.
     *
     * @see #isCompass(AxisDirection)
     */
    public static int angleForCompass(final AxisDirection source, final AxisDirection target) {
        final int base = NORTH.ordinal();
        final int src  = source.ordinal() - base;
        if (src >= 0 && src < COMPASS_COUNT) {
            final int tgt = target.ordinal() - base;
            if (tgt >= 0 && tgt < COMPASS_COUNT) {
                int n = src - tgt;
                if (n < -COMPASS_COUNT/2) {
                    n += COMPASS_COUNT;
                } else if (n > COMPASS_COUNT/2) {
                    n -= COMPASS_COUNT;
                }
                return n;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Angle between display directions only.
     *
     * @param  source The start direction.
     * @param  target The final direction.
     * @return The arithmetic angle as a multiple of 360/{@link #DISPLAY_RIGHT}, or {@link Integer#MIN_VALUE} if none.
     */
    public static int angleForDisplay(final AxisDirection source, final AxisDirection target) {
        final int base = DISPLAY_RIGHT.ordinal();
        int src  = source.ordinal() - base;
        if (src >= 0 && src < DISPLAY_COUNT) {
            int tgt = target.ordinal() - base;
            if (tgt >= 0 && tgt < DISPLAY_COUNT) {
                /*
                 * Display directions are RIGHT, LEFT, UP, DOWN. We need to reorder them as UP, RIGHT, DOWN, LEFT.
                 */
                src = DISPLAY_ORDER[src];
                tgt = DISPLAY_ORDER[tgt];
                int n = src - tgt;
                if (n < -DISPLAY_COUNT/2) {
                    n += DISPLAY_COUNT;
                } else if (n > DISPLAY_COUNT/2) {
                    n -= DISPLAY_COUNT;
                }
                return n;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Maps RIGHT, LEFT, UP, DOWN display order to UP, RIGHT, DOWN, LEFT.
     */
    private static final byte[] DISPLAY_ORDER = {1, 3, 0, 2};

    /**
     * Finds the dimension of an axis having the given direction or its opposite.
     * If more than one axis has the given direction, only the first occurrence is returned.
     * If both the given direction and its opposite exist, then the dimension for the given
     * direction has precedence over the opposite direction.
     *
     * @param  cs The coordinate system to inspect, or {@code null}.
     * @param  direction The direction of the axis to search.
     * @return The dimension of the axis using the given direction or its opposite, or -1 if none.
     */
    public static int indexOfColinear(final CoordinateSystem cs, final AxisDirection direction) {
        int fallback = -1;
        if (cs != null) {
            final int dimension = cs.getDimension();
            for (int i=0; i<dimension; i++) {
                final AxisDirection d = cs.getAxis(i).getDirection();
                if (direction.equals(d)) {
                    return i;
                }
                if (fallback < 0 && d.equals(opposite(direction))) {
                    fallback = i;
                }
            }
        }
        return fallback;
    }

    /**
     * Searches for an axis direction having the given name in the specified list of directions.
     * This method compares the given name with the name of each {@code AxisDirection} in a lenient way:
     *
     * <ul>
     *   <li>Comparisons are case-insensitive.</li>
     *   <li>Any character which is not a letter or a digit is ignored. For example {@code "NorthEast"},
     *       {@code "North-East"} and {@code "NORTH_EAST"} are considered equivalent.</li>
     *   <li>This method accepts abbreviations as well, for example if the given {@code name} is {@code "W"},
     *       then it will be considered equivalent to {@code "WEST"}.</li>
     * </ul>
     *
     * @param  name The name of the axis direction to search.
     * @param  directions The list of axis directions in which to search.
     * @return The first axis direction having a name matching the given one, or {@code null} if none.
     */
    public static AxisDirection find(final String name, final AxisDirection[] directions) {
        for (final AxisDirection candidate : directions) {
            final String identifier = candidate.name();
            if (equalsFiltered(name, identifier, Characters.Filter.LETTERS_AND_DIGITS, true)
                    || isAcronymForWords(name, identifier))
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Searches pre-defined {@link AxisDirection} for a given name. This method searches for a match in the set
     * of known axis directions as returned by {@link AxisDirections#values()}, plus a few special cases like
     * "<cite>Geocentre &gt; equator/90°E</cite>". The later are used in the EPSG database for geocentric CRS.
     *
     * <p>This method does not know about {@link DirectionAlongMeridian}. The later is a parser which may create
     * new directions, while this method searches only in a set of predefined directions and never create new ones.</p>
     *
     * @param  name The name of the axis direction to search.
     * @return The first axis direction having a name matching the given one, or {@code null} if none.
     */
    public static AxisDirection valueOf(String name) {
        name = trimWhitespaces(name.replace('_', ' '));
        final AxisDirection[] directions = AxisDirection.values();
        AxisDirection candidate = find(name, directions);
        if (candidate == null) {
            /*
             * No match found when using the pre-defined axis name. Searches among
             * the set of geocentric directions. Expected directions are:
             *
             *    Geocentre > equator/PM      or    Geocentre > equator/0°E
             *    Geocentre > equator/90dE    or    Geocentre > equator/90°E
             *    Geocentre > north pole
             */
            int d = name.indexOf('>');
            if (d >= 0 && equalsIgnoreCase(name, 0, skipTrailingWhitespaces(name, 0, d), "Geocentre")) {
                final int length = name.length();
                d = skipLeadingWhitespaces(name, d+1, length);
                int s = name.indexOf('/', d);
                if (s < 0) {
                    if (equalsIgnoreCase(name, d, length, "north pole")) {
                        return GEOCENTRIC_Z; // "Geocentre > north pole"
                    }
                } else if (equalsIgnoreCase(name, d, skipTrailingWhitespaces(name, d, s), "equator")) {
                    s = skipLeadingWhitespaces(name, s+1, length);
                    if (equalsIgnoreCase(name, s, length, "PM")) {
                        return GEOCENTRIC_X; // "Geocentre > equator/PM"
                    }
                    /*
                     * At this point, the name may be "Geocentre > equator/0°E",
                     * "Geocentre > equator/90°E" or "Geocentre > equator/90dE".
                     * Parse the number, limiting the scan to 6 characters for
                     * avoiding a NumberFormatException.
                     */
                    final int stopAt = Math.min(s + 6, length);
                    for (int i=s; i<stopAt; i++) {
                        final char c = name.charAt(i);
                        if (c < '0' || c > '9') {
                            if (i == s) break;
                            final int n = Integer.parseInt(name.substring(s, i));
                            i = skipLeadingWhitespaces(name, i, length);
                            if (equalsIgnoreCase(name, i, length, "°E") || equalsIgnoreCase(name, i, length, "dE")) {
                                switch (n) {
                                    case  0: return GEOCENTRIC_X; // "Geocentre > equator/0°E"
                                    case 90: return GEOCENTRIC_Y; // "Geocentre > equator/90°E"
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return candidate;
    }

    /**
     * Returns {@code true} if the given sub-sequence is equal to the given keyword, ignoring case.
     */
    private static boolean equalsIgnoreCase(final String name, final int lower, final int upper, final String keyword) {
        final int length = upper - lower;
        return (length == keyword.length()) && name.regionMatches(true, lower, keyword, 0, length);
    }
}
