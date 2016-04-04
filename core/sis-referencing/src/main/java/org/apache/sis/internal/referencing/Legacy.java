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

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.AxisFilter;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * Utilities related to version 1 of Well Known Text format.
 * Defined in a separated classes for reducing classes loading when not necessary.
 *
 * <p>This class implements the {@link AxisFilter} interface for opportunistic reasons.
 * Callers should ignore this implementation detail.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class Legacy {
    /**
     * A three-dimensional Cartesian CS with the legacy set of geocentric axes.
     * OGC 01-009 defines the default geocentric axes as:
     *
     * {@preformat wkt
     *   AXIS[“X”,OTHER], AXIS[“Y”,EAST], AXIS[“Z”,NORTH]
     * }
     *
     * where the {@code OTHER} axis is toward prime meridian. Those directions and axis names are different than
     * the ISO 19111's ones (ISO names are "Geocentric X", "Geocentric Y" and "Geocentric Z"). This constant uses
     * the invalid names and directions for WKT 1 parsing/formatting purposes.
     */
    private static final CartesianCS LEGACY = new DefaultCartesianCS(singletonMap(NAME_KEY, "Legacy geocentric"),
            new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "X"), "X", AxisDirection.OTHER, SI.METRE),
            new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Y"), "Y", AxisDirection.EAST,  SI.METRE),
            new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Z"), "Z", AxisDirection.NORTH, SI.METRE));

    /**
     * Do not allow instantiation of this class.
     */
    private Legacy() {
    }

    /**
     * The standard three-dimensional Cartesian CS as defined by ISO 19111.
     *
     * @param  unit The linear unit of the desired coordinate system, or {@code null} for metres.
     * @return The ISO 19111 coordinate system.
     */
    public static CartesianCS standard(final Unit<?> unit) {
        return replaceUnit((CartesianCS) CommonCRS.WGS84.geocentric().getCoordinateSystem(), unit);
    }

    /**
     * Returns the axes to use instead of the ones in the given coordinate system.
     * If the coordinate system axes should be used as-is, returns {@code cs}.
     *
     * @param  cs The coordinate system for which to compare the axis directions.
     * @param  toLegacy {@code true} for replacing ISO directions by the legacy ones,
     *         or {@code false} for the other way around.
     * @return The axes to use instead of the ones in the given CS,
     *         or {@code cs} if the CS axes should be used as-is.
     */
    public static CartesianCS forGeocentricCRS(final CartesianCS cs, final boolean toLegacy) {
        final CartesianCS check = toLegacy ? standard(null) : LEGACY;
        final int dimension = check.getDimension();
        if (cs.getDimension() != dimension) {
            return cs;
        }
        for (int i=0; i<dimension; i++) {
            if (!cs.getAxis(i).getDirection().equals(check.getAxis(i).getDirection())) {
                return cs;
            }
        }
        final Unit<?> unit = ReferencingUtilities.getUnit(cs);
        return toLegacy ? replaceUnit(LEGACY, unit) : standard(unit);
    }

    /**
     * Returns the coordinate system of a geocentric CRS using axes in the given unit of measurement.
     * This method presumes that the given {@code cs} uses {@link SI#METRE} (this is not verified).
     *
     * @param  cs The coordinate system for which to perform the unit replacement.
     * @param  unit The unit of measurement for the geocentric CRS axes.
     * @return The coordinate system for a geocentric CRS with axes using the given unit of measurement.
     *
     * @since 0.6
     */
    public static CartesianCS replaceUnit(CartesianCS cs, final Unit<?> unit) {
        if (unit != null && !unit.equals(SI.METRE)) {
            cs = (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, unit.asType(Length.class));
        }
        return cs;
    }
}
