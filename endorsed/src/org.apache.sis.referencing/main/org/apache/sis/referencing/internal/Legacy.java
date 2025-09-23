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
package org.apache.sis.referencing.internal;

import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;


/**
 * Utilities related to version 1 of Well Known Text format, or to ISO 19111:2007.
 * Defined in a separated classes for reducing classes loading when not necessary.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Legacy {
    /**
     * Key for the <code>{@value}</code> property to be given to {@code DerivedCRS} constructors.
     * This is used for specifying which interface the derived CRS should implement.
     * If no value is associated to this key, then the interface will be inferred
     * from the type of the base CRS and the derived coordinate system.
     *
     * <p>Value shall be an instance of {@code Class} such as {@code EngineeringCRS.class}.
     * This key can be used when the type of the CRS to create is potentially ambiguous.
     * For example a CRS derived from a {@link GeodeticCRS} can be itself geodetic (which is the default),
     * but could also be an {@link EngineeringCRS} if a change of datum type is allowed.
     * The latter is normally not allowed, but was nevertheless done in older versions of ISO 19111.</p>
     */
    public static final String DERIVED_TYPE_KEY = "derivedType";

    /**
     * A three-dimensional Cartesian CS with the legacy set of geocentric axes.
     * OGC 01-009 defines the default geocentric axes as:
     *
     * {@snippet lang="wkt" :
     *   AXIS[“X”,OTHER], AXIS[“Y”,EAST], AXIS[“Z”,NORTH]
     *   }
     *
     * where the {@code OTHER} axis is toward prime meridian. Those directions and axis names are different than
     * the ISO 19111's ones (ISO names are "Geocentric X", "Geocentric Y" and "Geocentric Z"). This constant uses
     * the invalid names and directions for WKT 1 parsing/formatting purposes.
     */
    @SuppressWarnings("deprecation")
    private static final CartesianCS GEOCENTRIC = new DefaultCartesianCS(Map.of(NAME_KEY, "Legacy geocentric"),
            new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, "X"), "X", AxisDirection.OTHER, Units.METRE),
            new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, "Y"), "Y", AxisDirection.EAST,  Units.METRE),
            new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, "Z"), "Z", AxisDirection.NORTH, Units.METRE));

    /**
     * Do not allow instantiation of this class.
     */
    private Legacy() {
    }

    /**
     * The standard three-dimensional geocentric Cartesian CS as defined by ISO 19111.
     *
     * @param  unit  the linear unit of the desired coordinate system, or {@code null} for metres.
     * @return the ISO 19111 coordinate system.
     */
    public static CartesianCS standard(final Unit<?> unit) {
        return replaceUnit((CartesianCS) CommonCRS.WGS84.geocentric().getCoordinateSystem(), unit);
    }

    /**
     * Returns the axes to use instead of the ones in the given coordinate system.
     * If the coordinate system axes should be used as-is, returns {@code cs}.
     *
     * @param  cs  the coordinate system for which to compare the axis directions.
     * @param  toLegacy {@code true} for replacing ISO directions by the legacy ones,
     *         or {@code false} for the other way around.
     * @return the axes to use instead of the ones in the given CS,
     *         or {@code cs} if the CS axes should be used as-is.
     */
    public static CartesianCS forGeocentricCRS(final CartesianCS cs, final boolean toLegacy) {
        final CartesianCS check = toLegacy ? standard(null) : GEOCENTRIC;
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
        return toLegacy ? replaceUnit(GEOCENTRIC, unit) : standard(unit);
    }

    /**
     * Returns the coordinate system of a geocentric CRS using axes in the given unit of measurement.
     * This method presumes that the given {@code cs} uses {@link Units#METRE} (this is not verified).
     *
     * @param  cs    the coordinate system for which to perform the unit replacement.
     * @param  unit  the unit of measurement for the geocentric CRS axes.
     * @return the coordinate system for a geocentric CRS with axes using the given unit of measurement.
     */
    public static CartesianCS replaceUnit(CartesianCS cs, final Unit<?> unit) {
        if (unit != null && !unit.equals(Units.METRE)) {
            cs = (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, unit.asType(Length.class));
        }
        return cs;
    }
}
