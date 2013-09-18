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

import java.util.Map;
import java.util.HashMap;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.util.Static;

import static org.opengis.referencing.cs.AxisDirection.*;

// Related to JDK7
import java.util.Objects;


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
     * Do not allow instantiation of this class.
     */
    private AxisDirections() {
    }

    /**
     * For each direction, the opposite direction.
     */
    private static final Map<AxisDirection,AxisDirection> opposites = new HashMap<>(35);
    static {
        opposites.put(OTHER, OTHER);
        final AxisDirection[] dir = {
            NORTH,            SOUTH,
            NORTH_NORTH_EAST, SOUTH_SOUTH_WEST,
            NORTH_EAST,       SOUTH_WEST,
            EAST_NORTH_EAST,  WEST_SOUTH_WEST,
            EAST,             WEST,
            EAST_SOUTH_EAST,  WEST_NORTH_WEST,
            SOUTH_EAST,       NORTH_WEST,
            SOUTH_SOUTH_EAST, NORTH_NORTH_WEST,
            UP,               DOWN,
            FUTURE,           PAST,
            COLUMN_POSITIVE,  COLUMN_NEGATIVE,
            ROW_POSITIVE,     ROW_NEGATIVE,
            DISPLAY_RIGHT,    DISPLAY_LEFT,
            DISPLAY_DOWN,     DISPLAY_UP // y values increase toward down.
        };
        for (int i=0; i<dir.length; i++) {
            if (opposites.put(dir[i], dir[i ^ 1]) != null) {
                throw new AssertionError(i);
            }
        }
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
     * @param  dir The direction for which to return the absolute direction.
     * @return The direction from the above table.
     */
    public static AxisDirection absolute(final AxisDirection dir) {
        if (dir != null) {
            final AxisDirection opposite = opposite(dir);
            if (opposite != null) {
                if (opposite.ordinal() < dir.ordinal()) {
                    return opposite;
                }
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
     * @param  dir The direction for which to return the opposite direction.
     * @return The opposite direction, or {@code null} if none or unknown.
     */
    public static AxisDirection opposite(final AxisDirection dir) {
        return opposites.get(dir);
    }

    /**
     * Returns {@code true} if the given direction is an "opposite" direction.
     * If this method can not determine if the given direction is an "opposite"
     * one, then it conservatively returns {@code true}.
     *
     * @param  dir The direction to test, or {@code null}.
     * @return {@code true} if the given direction is an "opposite".
     */
    public static boolean isOpposite(final AxisDirection dir) {
        return Objects.equals(dir, opposite(absolute(dir)));
    }

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
    public static int indexOf(final CoordinateSystem cs, final AxisDirection direction) {
        int fallback = -1;
        if (cs != null) {
            final AxisDirection opposite = opposite(direction);
            final int dimension = cs.getDimension();
            for (int i=0; i<dimension; i++) {
                final AxisDirection d = cs.getAxis(i).getDirection();
                if (direction.equals(d)) {
                    return i;
                }
                if (fallback < 0 && opposite != null && opposite.equals(d)) {
                    fallback = i;
                }
            }
        }
        return fallback;
    }
}
