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
package org.apache.sis.referencing.privy;

import java.util.Map;
import java.util.Objects;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import static org.opengis.referencing.cs.AxisDirection.*;
import org.apache.sis.metadata.privy.NameToIdentifier;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;
import static org.apache.sis.util.CharSequences.*;


/**
 * Utilities methods related to {@link AxisDirection}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AxisDirections {
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
     * Ordinal of the last element in the {@link AxisDirection} code list.
     * This is used for differentiating the standard codes from the user-defined ones.
     *
     * @see #isUserDefined(AxisDirection)
     */
    private static final int LAST_ORDINAL = UNSPECIFIED.ordinal();

    /**
     * Proposed abbreviations for some axis directions.
     */
    @SuppressWarnings("deprecation")
    private static final Map<AxisDirection,String> ABBREVIATIONS = Map.of(
            FUTURE,            "t",
            COLUMN_POSITIVE,   "i",
            ROW_POSITIVE,      "j",
            DISPLAY_RIGHT,     "x",
            DISPLAY_UP,        "y",
            UNSPECIFIED,       "m",     // Arbitrary abbreviation, may change in any future SIS version.
            OTHER,             "m",     // Idem.
            AWAY_FROM,         "r",
            COUNTER_CLOCKWISE, "θ");

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
     * <table class="sis">
     * <caption>Mapping to "absolute" directions</caption><tr>
     * <tr><th>Directions</th>                                      <th>Absolute value</th></tr>
     * <tr><td>{@code NORTH},           {@code SOUTH}</td>            <td>{@code NORTH}</td></tr>
     * <tr><td>{@code EAST},            {@code WEST}</td>             <td>{@code EAST}</td></tr>
     * <tr><td>{@code UP},              {@code DOWN}</td>             <td>{@code UP}</td></tr>
     * <tr><td>{@code FUTURE},          {@code PAST}</td>             <td>{@code FUTURE}</td></tr>
     * <tr><td>{@code COLUMN_POSITIVE}, {@code COLUMN_NEGATIVE}</td>  <td>{@code COLUMN_POSITIVE}</td></tr>
     * <tr><td>{@code ROW_POSITIVE},    {@code ROW_NEGATIVE}</td>     <td>{@code ROW_POSITIVE}</td></tr>
     * <tr><td>{@code DISPLAY_RIGHT},   {@code DISPLAY_LEFT}</td>     <td>{@code DISPLAY_RIGHT}</td></tr>
     * <tr><td>{@code DISPLAY_UP},      {@code DISPLAY_DOWN}</td>     <td>{@code DISPLAY_UP}</td></tr>
     * <tr><td>{@code CLOCKWISE},       {@code COUNTERCLOCKWISE}</td> <td>{@code COUNTERCLOCKWISE}</td></tr>
     * <tr><td>{@code AWAY_FROM},       {@code TOWARDS}</td>          <td>{@code AWAY_FROM}</td></tr>
     * <tr><td>{@code OTHER}</td>                                     <td>{@code OTHER}</td></tr>
     * </table>
     *
     * @param  dir  the direction for which to return the absolute direction, or {@code null}.
     * @return the direction from the above table, or {@code null} if the given direction was null.
     */
    public static AxisDirection absolute(AxisDirection dir) {
        final AxisDirection opposite = opposite(dir);
        if (opposite != null) {
            if (opposite.ordinal() < dir.ordinal()) {
                dir = opposite;
            }
            // Ordinal values do not have the desired order for this particular case.
            if (dir == CLOCKWISE) {
                dir = COUNTER_CLOCKWISE;
            } else if (dir == TOWARDS) {
                dir = AWAY_FROM;
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
     * @param  dir  the direction for which to return the opposite direction, or {@code null}.
     * @return the opposite direction, or {@code null} if none or unknown.
     */
    public static AxisDirection opposite(AxisDirection dir) {
        return (dir != null) ? dir.opposite().orElse(null) : null;
    }

    /**
     * Returns {@code true} if the given direction is an "opposite" direction.
     * If the given argument is {@code null} or is not a known direction, then
     * this method conservatively returns {@code false}.
     *
     * @param  dir  the direction to test, or {@code null}.
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
     * @param  dir  the direction to test, or {@code null}.
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
     * Returns {@code true} if the specified direction is cardinal direction.
     * Cardinal directions are {@code NORTH}, {@code SOUTH}, {@code EAST} and {@code WEST}.
     *
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the given direction is a cardinal direction.
     */
    public static boolean isCardinal(final AxisDirection dir) {
        if (dir == null) return false;
        final int n  = dir.ordinal() - NORTH.ordinal();
        return n >= 0 && n < COMPASS_COUNT && (n & 3) == 0;
    }

    /**
     * Returns {@code true} if the specified direction is an inter-cardinal direction.
     * Inter-cardinal directions are {@code NORTH_EAST}, {@code SOUTH_SOUTH_EAST}, etc.
     *
     * @param  dir  the direction to test, or {@code null}.
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
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the direction is vertical, or {@code false} otherwise.
     */
    public static boolean isVertical(final AxisDirection dir) {
        return dir == UP || dir == DOWN;
    }

    /**
     * Returns {@code true} if the given direction is {@code FUTURE} or {@code PAST}.
     *
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the direction is temporal, or {@code false} otherwise.
     */
    public static boolean isTemporal(final AxisDirection dir) {
        return dir == FUTURE || dir == PAST;
    }

    /**
     * Returns {@code true} if the given direction is {@code GEOCENTRIC_X}, {@code GEOCENTRIC_Y}
     * or {@code GEOCENTRIC_Z}.
     *
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the given direction is one of geocentric directions.
     */
    public static boolean isGeocentric(final AxisDirection dir) {
        return dir == GEOCENTRIC_X || dir == GEOCENTRIC_Y || dir == GEOCENTRIC_Z;
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
     * @param  dir    the direction to test, or {@code null}.
     * @param  image  {@code true} for accepting grid and image axis directions in addition to spatial ones.
     * @return {@code true} if the given direction is presumed for spatial CS.
     */
    public static boolean isSpatialOrUserDefined(final AxisDirection dir, final boolean image) {
        if (dir == null || dir == PAST || dir == FUTURE) return false;
        return image || dir.ordinal() < COLUMN_POSITIVE.ordinal() || dir.ordinal() > DISPLAY_DOWN.ordinal();
    }

    /**
     * Returns {@code true} if the given direction is a user-defined direction (i.e., is not defined by GeoAPI).
     *
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the given direction is user-defined.
     */
    public static boolean isUserDefined(final AxisDirection dir) {
        return (dir != null) && dir.ordinal() > LAST_ORDINAL;
    }

    /**
     * Returns {@code true} if the given direction is {@code COLUMN_POSITIVE}, {@code COLUMN_NEGATIVE},
     * {@code ROW_POSITIVE} or {@code ROW_NEGATIVE}.
     *
     * @param  dir  the direction to test, or {@code null}.
     * @return {@code true} if the given direction is presumed for grid CS.
     */
    public static boolean isGrid(final AxisDirection dir) {
        if (dir == null) return false;
        final int ordinal = dir.ordinal();
        return ordinal >= COLUMN_POSITIVE.ordinal() && ordinal <= ROW_NEGATIVE.ordinal();
    }

    /**
     * Arithmetic angle between forward/aft/port/starboard directions only.
     * This is the angle as viewed from above the vehicle.
     *
     * @param  source  the start direction.
     * @param  target  the final direction.
     * @return the angle as a multiple of 90°, or {@link Integer#MIN_VALUE} if none.
     */
    public static int angleForVehicle(final AxisDirection source, final AxisDirection target) {
        if (source == STARBOARD && target == FORWARD) return +1;
        if (source == FORWARD && target == STARBOARD) return -1;
        return Integer.MIN_VALUE;
    }

    /**
     * Angle between geocentric directions only.
     *
     * @param  source  the start direction.
     * @param  target  the final direction.
     * @return the angle as a multiple of 90°, or {@link Integer#MIN_VALUE} if none.
     */
    public static int angleForGeocentric(final AxisDirection source, final AxisDirection target) {
        final int base = GEOCENTRIC_X.ordinal();
        final int src  = source.ordinal() - base;
        if (src >= 0 && src < GEOCENTRIC_COUNT) {
            final int tgt = target.ordinal() - base;
            if (tgt >= 0 && tgt < GEOCENTRIC_COUNT) {
                int n = (tgt - src);
                n -= GEOCENTRIC_COUNT * (n/2);      // If -2 add 3.  If +2 subtract 3.  Otherwise do nothing.
                return n;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Arithmetic angle between compass directions only (not for angle between direction along meridians).
     *
     * @param  source  the start direction.
     * @param  target  the final direction.
     * @return the arithmetic angle as a multiple of 360/{@link #COMPASS_COUNT}, or {@link Integer#MIN_VALUE} if none.
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
     * Arithmetic angle between display directions only.
     *
     * @param  source  the start direction.
     * @param  target  the final direction.
     * @return the arithmetic angle as a multiple of 360/{@link #DISPLAY_COUNT}, or {@link Integer#MIN_VALUE} if none.
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
     * Returns the angular unit of the specified coordinate system.
     * The preference will be given to the longitude axis, if found.
     *
     * @param  cs        the coordinate system from which to get the angular unit, or {@code null}.
     * @param  fallback  the default unit to return if no angular unit is found.
     * @return the angular unit, of {@code unit} if no angular unit was found.
     *
     * @see org.apache.sis.referencing.privy.ReferencingUtilities#getUnit(CoordinateSystem)
     */
    public static Unit<Angle> getAngularUnit(final CoordinateSystem cs, Unit<Angle> fallback) {
        if (cs != null) {
            for (int i = cs.getDimension(); --i>=0;) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis != null) {                                                     // Paranoiac check.
                    final Unit<?> candidate = axis.getUnit();
                    if (Units.isAngular(candidate)) {
                        fallback = candidate.asType(Angle.class);
                        if (absolute(axis.getDirection()) == AxisDirection.EAST) {
                            break;                                                      // Found the longitude axis.
                        }
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Returns whether the given coordinate system has the given axis directions in the same order.
     * The coordinate system must have a number of axes at least equal to {@code directions.length}.
     * If the coordinate system has more dimensions, the extraneous axes are ignored.
     *
     * @param  cs          the coordinate system for which to check axis directions.
     * @param  directions  the expected axis directions.
     * @return whether the coordinate system has the given axis direction, in the same order.
     */
    public static boolean hasPrefix(final CoordinateSystem cs, final AxisDirection... directions) {
        if (cs.getDimension() < directions.length) {
            return false;
        }
        for (int i=0; i<directions.length; i++) {
            if (!directions[i].equals(cs.getAxis(i).getDirection())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the second axis is colinear with the first axis. This method returns {@code true}
     * if the {@linkplain #absolute absolute} direction of the given directions are equal.
     * For example, "down" is considered colinear with "up".
     *
     * @param  d1  the first axis direction to compare.
     * @param  d2  the second axis direction to compare.
     * @return {@code true} if both directions are colinear.
     */
    public static boolean isColinear(final AxisDirection d1, final AxisDirection d2) {
        return Objects.equals(absolute(d1), absolute(d2));
    }

    /**
     * Finds the dimension of an axis having the given direction or its opposite.
     * If more than one axis has the given direction, only the first occurrence is returned.
     * If both the given direction and its opposite exist, then the dimension for the given
     * direction has precedence over the opposite direction.
     *
     * @param  cs         the coordinate system to inspect, or {@code null}.
     * @param  direction  the direction of the axis to search.
     * @return the dimension of the axis using the given direction or its opposite, or -1 if none.
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
     * Returns the index of the first dimension in {@code cs} where axes are colinear with the {@code subCS} axes.
     * If no such dimension is found, returns -1. If more than one sequence of {@code cs} axes are colinear with
     * all {@code subCS} axes, then the following rules apply:
     *
     * <ol>
     *   <li>If a sequence of {@code cs} axes are equal to the {@code subCS} axes, that sequence has precedence.</li>
     *   <li>Otherwise if a sequence of {@code cs} axes have similar names than the {@code subCS} axes (as determined
     *       by {@linkplain org.apache.sis.referencing.IdentifiedObjects#isHeuristicMatchForName heuristic match},
     *       that sequence has precedence.</li>
     *   <li>Otherwise the index of the first sequence is returned, regardless axis names.</li>
     * </ol>
     *
     * Note that colinear axes are normally not allowed, except in the case of {@link org.opengis.referencing.crs.TemporalCRS}
     * when one time axis is the runtime (the date where a numerical model has been executed) and the other time axis is the
     * forecast time (the date at which a prevision is made).
     *
     * @param  cs     the coordinate system which contains all axes, or {@code null}.
     * @param  subCS  the coordinate system to search into {@code cs}.
     * @return the first dimension of a sequence of axes colinear with {@code subCS} axes, or {@code -1} if none.
     */
    public static int indexOfColinear(final CoordinateSystem cs, final CoordinateSystem subCS) {
        int fallback = -1;
        if (cs != null) {
            boolean fallbackMatches = false;
            final int subDim = subCS.getDimension();
            final int limit = cs.getDimension() - subDim;
next:       for (int i=0; i <= limit; i++) {
                boolean equal = true;
                boolean match = true;
                for (int j=0; j<subDim; j++) {
                    final CoordinateSystemAxis expected = subCS.getAxis(j);
                    final CoordinateSystemAxis actual = cs.getAxis(i + j);
                    if (!isColinear(expected.getDirection(), actual.getDirection())) {
                        continue next;
                    }
                    if (equal) {
                        equal = Utilities.deepEquals(expected, actual, ComparisonMode.BY_CONTRACT);
                        if (equal) continue;
                    }
                    if (match) {
                        match = NameToIdentifier.isHeuristicMatchForName(actual, expected.getName().getCode());
                    }
                }
                if (equal) {
                    return i;
                }
                if (fallback < 0 | (match & !fallbackMatches)) {
                    fallbackMatches = match;
                    fallback = i;
                }
            }
        }
        return fallback;
    }

    /**
     * Returns the indices of {@code cs} axes presumed covariant with {@code subCS} axes.
     * The mapping is based on axis directions only, with colinear axes mapped in priority.
     * If some axes cannot be mapped using collinearity criterion, then directions from poles
     * (e.g. <q>South along 90°E</q>) are arbitrarily handled as if they were covariant
     * with East and North directions, in that order.
     *
     * @param  cs     the coordinate system which contains all axes, or {@code null}.
     * @param  subCS  the coordinate system for which to search axes into {@code cs}.
     * @return indices in {@code cs} of axes covariant with {@code subCS} axes in the order they appear in {@code subCS},
     *         or {@code null} if at least one {@code subCS} axis cannot be mapped to a {@code cs} axis.
     *
     * @see #indexOfColinear(CoordinateSystem, CoordinateSystem)
     */
    public static int[] indicesOfLenientMapping(final CoordinateSystem cs, final CoordinateSystem subCS) {
        final int index = indexOfColinear(cs, subCS);           // More robust than fallback below.
        if (index >= 0) {
            return ArraysExt.range(index, index + subCS.getDimension());
        }
        return AxesMapper.indices(cs, subCS);
    }

    /**
     * Searches for an axis direction having the given name in the specified list of directions.
     * This method compares the given name with the name of each {@code AxisDirection} in a lenient way:
     *
     * <ul>
     *   <li>Comparisons are case-insensitive.</li>
     *   <li>Any character which is not a letter or a digit is ignored. For example, {@code "NorthEast"},
     *       {@code "North-East"} and {@code "NORTH_EAST"} are considered equivalent.</li>
     *   <li>This method accepts abbreviations as well, for example if the given {@code name} is {@code "W"},
     *       then it will be considered equivalent to {@code "WEST"}.</li>
     * </ul>
     *
     * @param  name        the name of the axis direction to search.
     * @param  directions  the list of axis directions in which to search.
     * @return the first axis direction having a name matching the given one, or {@code null} if none.
     */
    public static AxisDirection find(final String name, final AxisDirection[] directions) {
        for (final AxisDirection candidate : directions) {
            final String identifier = candidate.name();
            if (equalsFiltered(name, identifier, Characters.Filter.LETTERS_AND_DIGITS, true) || isAcronymForWords(name, identifier)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Searches predefined {@link AxisDirection} for a given name. This method searches for a match in the set
     * of known axis directions as returned by {@link AxisDirection#values()}, plus a few special cases like
     * <q>Geocentre &gt; equator/90°E</q>. The latter are used in the EPSG database for geocentric CRS.
     *
     * <p>This method does not know about {@code org.apache.sis.referencing.cs.DirectionAlongMeridian}.
     * The latter is a parser which may create new directions, while this method searches only in a set
     * of predefined directions and never create new ones.</p>
     *
     * @param  name  the name of the axis direction to search.
     * @return the first axis direction having a name matching the given one, or {@code null} if none.
     */
    public static AxisDirection valueOf(String name) {
        name = name.replace('_', ' ').strip();
        final AxisDirection[] directions = AxisDirection.values();
        AxisDirection candidate = find(name, directions);
        if (candidate == null) {
            /*
             * No match found when using the predefined axis name. Searches among
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
                        return GEOCENTRIC_Z;                                // "Geocentre > north pole"
                    }
                } else if (equalsIgnoreCase(name, d, skipTrailingWhitespaces(name, d, s), "equator")) {
                    s = skipLeadingWhitespaces(name, s+1, length);
                    if (equalsIgnoreCase(name, s, length, "PM")) {
                        return GEOCENTRIC_X;                                // "Geocentre > equator/PM"
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
                                    case  0: return GEOCENTRIC_X;           // "Geocentre > equator/0°E"
                                    case 90: return GEOCENTRIC_Y;           // "Geocentre > equator/90°E"
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

    /**
     * Returns {@code true} if the given name starts or ends with the given keyword, ignoring case.
     *
     * @param end  {@code false} if the given keyword is expected at the beginning of the name,
     *             or {@code true} if expected at the end.
     */
    private static boolean contains(final String name, final String keyword, final boolean end) {
        final int length = keyword.length();
        return name.regionMatches(true, end ? name.length() - length : 0, keyword, 0, length);
    }

    /**
     * Suggests an abbreviation for the given axis direction. The unit of measurement may be used
     * for resolving some ambiguities like whether {@link AxisDirection#EAST} is for "x" (Easting)
     * or "λ" (Longitude).
     *
     * @param  name       the axis name for which to suggest an abbreviation.
     * @param  direction  the axis direction for which to suggest an abbreviation.
     * @param  unit       the axis unit of measurement, for disambiguation.
     * @return a suggested abbreviation.
     */
    public static String suggestAbbreviation(final String name, final AxisDirection direction, final Unit<?> unit) {
        if (name.length() == 1) {
            return name;                    // Most common cases are "x", "y", "z", "t", "i" and "j".
        }
        /*
         * Direction may be both "compass" (e.g. "North") or "non-compass" (e.g. "Away from").
         * Even if the radius at θ = 0° is oriented toward North, we do not want the "N" abbreviation.
         */
        if (contains(name, "radius", true)) {
            return "r";
        }
        if (isCompass(direction)) {
            /*
             * NORTH, EAST, SOUTH, WEST and all intercardinal directions (SOUTH_SOUTH_WEST, etc.):
             * we will use the acronym (e.g. "SE" for SOUTH_EAST), unless the axis is likely to be
             * a longitude or latitude axis. We detect those later cases by the unit of measurement.
             */
            if (!isIntercardinal(direction) && Units.isAngular(unit)) {
                if (contains(name, "Spherical", false)) {
                    return NORTH.equals(absolute(direction)) ? "Ω" : "θ";
                } else {
                    return NORTH.equals(absolute(direction)) ? "φ" : "λ";
                }
            }
        } else {
            /*
             * All cases other than NORTH, SOUTH, etc. The most common direction is UP, in which case the
             * abbreviation shall be "H" for Gravity-related height and "h" for ellipsoidal height. Those
             * two names are specified by ISO 19111, but we will use "Gravity" as a criterion because we
             * use "h" as the fallback for unknown vertical axis.
             */
            if (UP.equals(direction)) {
                if (Units.isAngular(unit))               return "α";                // Elevation angle
                if (contains(name, "Gravity",    false)) return "H";                // Gravity-related height
                if (contains(name, "Geocentric", false)) return "r";                // Geocentric radius
                return "h";                                                         // Ellipsoidal height
            } else if (DOWN.equals(direction)) {
                return "D";                                                         // Depth
            } else if (isGeocentric(direction)) {
                // For GEOCENTRIC_X, GEOCENTRIC_Y or GEOCENTRIC_Z, just take the last letter.
                final String dir = direction.name();
                return dir.substring(dir.length() - 1).trim();
            }
            final String abbreviation = ABBREVIATIONS.get(absolute(direction));
            if (abbreviation != null) {
                return abbreviation;
            }
        }
        String id = direction.identifier();
        if (id == null) id = direction.name();
        return camelCaseToAcronym(id).toString().intern();
    }

    /**
     * Returns an axis direction for the given abbreviation. This method is (partially) the converse
     * of {@link #suggestAbbreviation(String, AxisDirection, Unit)}. Current implementation does not
     * recognize all abbreviation generated by above method, but only the main ones.  This method is
     * defined here for making easier to maintain consistency with {@code suggestAbbreviation(…)}.
     *
     * @param  abbreviation  the abbreviation.
     * @return axis direction for the given abbreviation, or {@code null} if unrecognized.
     */
    public static AxisDirection fromAbbreviation(char abbreviation) {
        if (abbreviation >= 'a' && abbreviation <= 'z') {
            abbreviation -= ('a' - 'A');                    // To upper case, but only for latin characters.
        }
        final AxisDirection dir;
        switch (abbreviation) {
            default:                      dir = null;                 break;
            case 'W':                     dir = AxisDirection.WEST;   break;
            case 'S':                     dir = AxisDirection.SOUTH;  break;
            case 'θ': case 'λ': case 'E': dir = AxisDirection.EAST;   break;
            case 'Ω': case 'φ': case 'N': dir = AxisDirection.NORTH;  break;
            case 'R': case 'H':           dir = AxisDirection.UP;     break;
            case 'D':                     dir = AxisDirection.DOWN;   break;
            case 'T':                     dir = AxisDirection.FUTURE; break;
        }
        return dir;
    }

    /**
     * Builds a coordinate system name from the given array of axes.
     * This method expects a {@code StringBuilder} pre-filled with the coordinate system name.
     * The axis directions and abbreviations will be appended after the CS name.
     * Examples:
     *
     * <ul>
     *   <li>Ellipsoidal CS: North (°), East (°).</li>
     *   <li>Cartesian CS: East (km), North (km).</li>
     *   <li>Compound CS: East (km), North (km), Up (m).</li>
     * </ul>
     *
     * @param  buffer  a buffer pre-filled with the name header.
     * @param  axes    the axes to append in the given buffer.
     * @return a name for the given coordinate system type and axes.
     */
    public static String appendTo(final StringBuilder buffer, final CoordinateSystemAxis[] axes) {
        String separator = ": ";
        for (final CoordinateSystemAxis axis : axes) {
            buffer.append(separator).append(Types.getCodeLabel(axis.getDirection()));
            separator = ", ";
            final Unit<?> unit = axis.getUnit();
            if (unit != null) {
                final String symbol = unit.toString();
                if (symbol != null && !symbol.isEmpty()) {
                    buffer.append(" (").append(symbol).append(')');
                }
            }
        }
        return buffer.append('.').toString();
    }
}
