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
package org.apache.sis.io.wkt;

import javax.measure.unit.SI;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.apache.sis.referencing.GeodeticObjects;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.util.Static;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * Utilities related to version 1 of Well Known Text format.
 * Defined in a separated classes for reducing classes loading when not necessary.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
final class Legacy extends Static {
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
     */
    private static CartesianCS standard() {
        return (CartesianCS) GeodeticObjects.WGS84.geocentric().getCoordinateSystem();
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
    static CartesianCS replace(final CartesianCS cs, final boolean toLegacy) {
        final CartesianCS check = toLegacy ? standard() : LEGACY;
        final int dimension = check.getDimension();
        if (cs.getDimension() != dimension) {
            return cs;
        }
        for (int i=0; i<dimension; i++) {
            if (!cs.getAxis(i).getDirection().equals(check.getAxis(i).getDirection())) {
                return cs;
            }
        }
        return toLegacy ? LEGACY : standard();
    }
}
