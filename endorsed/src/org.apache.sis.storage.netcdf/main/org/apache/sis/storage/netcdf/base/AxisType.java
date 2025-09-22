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
package org.apache.sis.storage.netcdf.base;

import java.util.Map;
import java.util.HashMap;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.measure.Units;


/**
 * Type of coordinate system axis, in the order they should appear for a "normalized" coordinate reference system.
 * The enumeration name matches the name of the {@code "axis"} attribute in CF-convention.
 * Enumeration order is the desired order of coordinate values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum AxisType {
    /**
     * X (usually longitude) coordinate axis.
     */
    X,

    /**
     * Y (usually latitude) coordinate axis.
     */
    Y,

    /**
     * Z (usually height) coordinate axis.
     */
    Z,

    /**
     * Time coordinate axis.
     */
    T;

    /**
     * Mapping from values of the {@code "_CoordinateAxisType"} attribute or axis name to the abbreviation.
     * Keys are lower cases and values are controlled vocabulary documented in {@link Axis#abbreviation}.
     *
     * <div class="note">"GeoX" and "GeoY" stands for projected coordinates, not geocentric coordinates.</div>
     *
     * @see abbreviation(String)
     */
    private static final Map<String,Character> TYPES = new HashMap<>(26);

    /**
     * The enumeration values for given abbreviations.
     */
    private static final Map<Character,AxisType> VALUES = new HashMap<>(13);
    static {
        addAxisTypes(X, 'λ', "longitude", "lon", "long");
        addAxisTypes(Y, 'φ', "latitude",  "lat");
        addAxisTypes(Z, 'H', "pressure", "height", "altitude", "barometric_altitude", "elevation", "elev", "geoz");
        addAxisTypes(Z, 'D', "depth", "depth_below_geoid");
        addAxisTypes(X, 'E', "geox", "projection_x_coordinate");
        addAxisTypes(Y, 'N', "geoy", "projection_y_coordinate");
        addAxisTypes(T, 't', "t", "time", "runtime");
        addAxisTypes(X, 'x', "x");
        addAxisTypes(Y, 'y', "y");
        addAxisTypes(Z, 'z', "z");
    }

    /**
     * Adds a sequence of axis types or variable names for the given abbreviation.
     */
    private static void addAxisTypes(final AxisType value, final char abbreviation, final String... names) {
        final Character c = abbreviation;
        for (final String name : names) {
            TYPES.put(name, c);
        }
        VALUES.put(c, value);
    }

    /**
     * Returns the axis type (identified by its abbreviation) for an axis of the given name, or null if unknown.
     * The returned code is one of the controlled vocabulary documented in {@link Axis#abbreviation}.
     *
     * @param  type  the {@code "_CoordinateAxisType"} attribute value or another description used as fallback.
     * @return axis abbreviation for the given type or name, or {@code null} if none.
     */
    private static Character abbreviation(final String type) {
        return (type != null) ? TYPES.get(type.toLowerCase(Decoder.DATA_LOCALE)) : null;
    }

    /**
     * Returns {@code true} if the given abbreviation is null or is ambiguous.
     * The latter happens when the {@code axis} attribute value or the variable name is "X", "Y" or "Z",
     * which could be longitude, latitude or height as well as axes in any other coordinate system.
     *
     * @param  abbreviation  the axis abbreviation, or {@code null}.
     * @return whether the given abbreviation is considered ambiguous.
     */
    private static boolean isNullOrAmbiguous(final Character abbreviation) {
        return (abbreviation == null) || (abbreviation >= 'x' && abbreviation <= 'z');
    }

    /**
     * Returns the axis type (identified by its abbreviation) for the given axis, or 0 if unknown.
     * The returned code is one of the controlled vocabulary documented in {@link Axis#abbreviation}.
     *
     * @param  axis     axis for which to get an abbreviation.
     * @param  useUnit  whether this method is allowed to check the unit of measurement.
     * @return abbreviation for the given axis, or 0 if none.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-552">SIS-552</a>
     */
    public static char abbreviation(final Variable axis, final boolean useUnit) {
        /*
         * In Apache SIS implementation, the abbreviation determines the axis type. If a "_CoordinateAxisType" attribute
         * exists, il will have precedence over all other heuristic rules in this method because it is the most specific
         * information about axis type. Otherwise the "standard_name" attribute is our first fallback since valid values
         * are standardized to "longitude" and "latitude" among others.
         */
        Character abbreviation = abbreviation(axis.getAxisType());
        if (isNullOrAmbiguous(abbreviation)) {
            Character fallback = abbreviation;
            abbreviation = abbreviation(axis.getAttributeAsString(CF.STANDARD_NAME));    // No fallback on variable name.
            if (fallback == null) fallback = abbreviation;
            /*
             * If the abbreviation is still unknown, look at the "long_name", "description" or "title" attribute. Those
             * attributes are not standardized, so they are less reliable than "standard_name". But they are still more
             * reliable that the variable name since the long name may be "Longitude" or "Latitude" while the variable
             * name is only "x" or "y".
             */
            if (isNullOrAmbiguous(abbreviation)) {
                abbreviation = abbreviation(axis.getDescription());
                if (fallback == null) fallback = abbreviation;
                if (isNullOrAmbiguous(abbreviation)) {
                    /*
                     * Actually the "degree_east" and "degree_north" units of measurement are the most reliable way to
                     * identify geographic system, but we nevertheless check them almost last because the direction is
                     * already verified by Axis constructor. By checking the variable attributes first, we give a chance
                     * to Axis constructor to report a warning if there is an inconsistency.
                     */
                    if (useUnit && Units.isAngular(axis.getUnit())) {
                        final AxisDirection direction = AxisDirections.absolute(Axis.direction(axis.getUnitsString()));
                        if (direction == AxisDirection.EAST) {
                            return 'λ';
                        } else if (direction == AxisDirection.NORTH) {
                            return 'φ';
                        }
                    }
                    /*
                     * We test the variable name last because that name is more at risk of being an uninformative "x" or "y" name.
                     * If even the variable name is not sufficient, we use some easy to recognize units.
                     */
                    abbreviation = abbreviation(axis.getName());
                    if (fallback == null) fallback = abbreviation;
                    if (isNullOrAmbiguous(abbreviation)) {
                        if (useUnit) {
                            final Unit<?> unit = axis.getUnit();
                            if (Units.isTemporal(unit)) {
                                return 't';
                            } else if (Units.isPressure(unit)) {
                                return 'z';
                            }
                        }
                        return (fallback != null) ? fallback : 0;
                    }
                }
            }
        }
        return abbreviation;
    }

    /**
     * Returns the enumeration value for the given variable, or {@code null} if none.
     *
     * @param  axis     axis for which to get an enumeration value.
     * @param  useUnit  whether this method is allowed to check the unit of measurement.
     * @return enumeration value for the given axis, or {@code null} if none.
     */
    static AxisType valueOf(final Variable axis, final boolean useUnit) {
        final char abbreviation = abbreviation(axis, useUnit);
        return (abbreviation != 0) ? VALUES.get(abbreviation) : null;
    }
}
