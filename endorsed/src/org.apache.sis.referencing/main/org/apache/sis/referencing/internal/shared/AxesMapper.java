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
package org.apache.sis.referencing.internal.shared;

import static java.lang.Long.numberOfTrailingZeros;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * Maps coordinate axes for a "sub-coordinate system" to the axes of a coordinate systems with more dimensions.
 * This enumeration contains the criterion to apply for matching axes, in priority order.
 *
 * <p>In current implementation, the enumeration values are used only internally.
 * The useful method is {@link #indices(CoordinateSystem, CoordinateSystem)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum AxesMapper {
    /**
     * Match axes having the exact same direction.
     */
    SAME,

    /**
     * Match axes having opposite direction.
     */
    OPPOSITE,

    /**
     * Match axes of the kind "South along 90° East".
     */
    POLAR;

    /**
     * Bitmask for axis direction handled in a special way.
     * They are the substitute for directions like "South along 90° East".
     * We arbitrarily replace the first direction by East and the second direction by North.
     */
    private static final int EAST = 1, NORTH = 2;

    /**
     * Returns the indices in {@code cs} of axes presumed covariant with the {@code subCS} axes.
     * Directions such as "South along 90°E" are arbitrarily handled as if they were covariant with East and North.
     *
     * <h4>Limitations</h4>
     * Current implementation considers only the first 64 dimensions.
     * If there is more dimensions, the extra ones are silently ignored.
     *
     * @param  cs     the coordinate system which contains all axes, or {@code null}.
     * @param  subCS  the coordinate system to search into {@code cs}.
     * @return indices in {@code cs} of axes covariant with {@code subCS} axes in the order they appear in {@code subCS},
     *         or {@code null} if at least one {@code subCS} axis cannot be mapped to a {@code cs} axis.
     */
    public static int[] indices(final CoordinateSystem cs, final CoordinateSystem subCS) {
        final int[] indices = new int[subCS.getDimension()];
        long  axesToSearch  = Numerics.bitmask(indices.length) - 1;      // Bit mask of `subCS` axes not yet used.
        long  availableAxes = Numerics.bitmask(cs.getDimension()) - 1;   // Bit mask of `cs` axes not yet used.
        int   directionUsed = 0;
        for (final AxesMapper comparisonMode : values()) {
            /*
             * We will do 3 attempts to match axes, with matching criteria relaxed on each iteration:
             *
             *   1) Matches axes having the same direction.
             *   2) Matches axes having opposite directions.
             *   3) Matches axes of the kind "South along 90° East".
             *
             * On each `comparisonMode` iteration, we iterate over all `subCS` axes which have not be selected in
             * a previous iteration. The `iterSubCS` bitmask has a bit set to 1 for each remaining axes to visit.
             */
            long iterSubCS = axesToSearch, clearSubCS;
            for (int dimSubCS; hasMore(dimSubCS = numberOfTrailingZeros(iterSubCS)); iterSubCS &= clearSubCS) {
                clearSubCS = clearMask(dimSubCS);
                AxisDirection search = subCS.getAxis(dimSubCS).getDirection();
                switch (comparisonMode) {
                    case OPPOSITE: {
                        if (search == (search = AxisDirections.opposite(search)) || search == null) {
                            continue;               // Axis already examined in previous iteration.
                        }
                        break;
                    }
                    case POLAR: {
                        if (CoordinateSystems.isAlongMeridian(search)) {
                            switch (directionUsed) {
                                case 0:     // For first axis, fallback on EAST.
                                case NORTH: search = AxisDirection.EAST;  break;
                                case EAST:  search = AxisDirection.NORTH; break;
                                default:    return null;  // Too many such axes.
                            }
                        }
                    }
                }
                /*
                 * At this point we got the axis direction to search adjusted for the comparison mode.
                 * Now iterate over all `cs` dimensions in search for an axis having a compatible direction.
                 */
                long iterCS = availableAxes, clearCS;
                for (int dimCS; hasMore(dimCS = numberOfTrailingZeros(iterCS)); iterCS &= clearCS) {
                    clearCS = clearMask(dimCS);
                    AxisDirection candidate = cs.getAxis(dimCS).getDirection();
                    if (comparisonMode == POLAR && CoordinateSystems.isAlongMeridian(candidate)) {
                        switch (directionUsed) {
                            case 0:     // For first axis, fallback on EAST.
                            case NORTH: candidate = AxisDirection.EAST;  break;
                            case EAST:  candidate = AxisDirection.NORTH; break;
                            default:    return null;    // Too many such axes.
                        }
                    }
                    if (search.equals(candidate)) {
                        if (search.equals(AxisDirection.EAST))  directionUsed |= EAST;
                        if (search.equals(AxisDirection.NORTH)) directionUsed |= NORTH;
                        indices[dimSubCS] = dimCS;
                        availableAxes &= clearCS;
                        axesToSearch  &= clearSubCS;
                        if (axesToSearch == 0) {
                            return indices;
                        }
                        break;      // Move to next `subCS` axis.
                    }
                }
            }
        }
        return (axesToSearch == 0) ? indices : null;
    }

    /**
     * Returns {@code true} if the given value computed by {@link Long#numberOfTrailingZeros(long)}
     * means that there is at least one more dimension to process. This is used for stopping iterations.
     */
    private static boolean hasMore(final int numBits) {
        return (numBits & ~(Long.SIZE - 1)) == 0;
    }

    /**
     * Returns a mask for clearing the bit associated to the given coordinate system dimension.
     */
    private static long clearMask(final int dim) {
        return ~(1L << dim);
    }
}
