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
package org.apache.sis.storage.gdal;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.measure.Units;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


/**
 * Creates a parameter value group from a {@literal Proj.4} definition string.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Proj4Parser {
    /**
     * {@literal Proj.4} parameters to ignore when building the parameter value group of a map projection.
     * We ignore those parameters because they define for example the datum, prime meridian, <i>etc</i>.
     * Those information will be fetched by other classes than this {@code Proj4Parser}.
     */
    private static final Set<String> EXCLUDES = new HashSet<>(16);
    static {
        EXCLUDES.add("init");           // Authority code
        EXCLUDES.add("datum");          // Geodetic datum name
        EXCLUDES.add("ellps");          // Ellipsoid name
        EXCLUDES.add("pm");             // Prime meridian
        EXCLUDES.add("units");          // Axis units
        EXCLUDES.add("vunits");         // Vertical axis units
        EXCLUDES.add("to_meter");       // Axis units conversion factor
        EXCLUDES.add("vto_meter");      // Vertical axis units conversion factor
        EXCLUDES.add("towgs84");        // Datum shift
        EXCLUDES.add("axis");           // Axis order
    }

    /**
     * The {@value} keyword used for projection name in {@literal Proj.4} definition strings.
     */
    static final String PROJ = "proj";

    /**
     * The values extracted from the {@literal Proj.4} definition strings.
     * Keys are Proj.4 keywords like {@code "a"} or {@code "ts"}.
     */
    private final Map<String,String> parameters = new LinkedHashMap<>();

    /**
     * The map projection method inferred from the {@code "+proj="}, or {@code null} if not yet fetched.
     */
    private OperationMethod method;

    /**
     * Creates a parser for the given {@literal Proj.4} definition string.
     */
    Proj4Parser(final String definition) throws InvalidGeodeticParameterException {
        final int length = definition.length();
        int start = definition.indexOf('+');
        while (start >= 0) {
            final int end  = definition.indexOf('=', ++start);
            final int next = definition.indexOf('+',   start);
            if (end >= 0 && (next < 0 || end < next)) {
                final String keyword = CharSequences.trimWhitespaces(definition, start, end).toString().toLowerCase(Locale.US);
                final String value   = CharSequences.trimWhitespaces(definition, end+1, (next >= 0) ? next : length).toString();
                if (!value.isEmpty()) {
                    final String old = parameters.put(keyword, value);
                    if (old != null && !old.equals(value)) {
                        throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.DuplicatedElement_1, keyword));
                    }
                }
            }
            start = next;
        }
    }

    /**
     * Returns a suggested name for the coordinate reference system object to build.
     */
    final String name(final boolean isProjected) {
        String name = parameters.get("datum");
        if (name == null) {
            name = parameters.get("ellps");
            if (name == null) {
                name = Proj4Factory.UNNAMED;
            }
        }
        if (isProjected) {
            final String proj = parameters.get(PROJ);
            if (proj != null) {
                name = name + ' ' + Proj4Factory.PROJ_PARAM + proj;
            }
        }
        return name;
    }

    /**
     * Returns the parameter value for the given keyword, or the given default value if none.
     * The parameter value is removed from the map.
     */
    final String value(final String keyword, final String defaultValue) {
        final String value = parameters.remove(keyword);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the operation method inferred from the {@code "proj"} parameter value.
     * This method must be invoked at least once before {@link #parameters()}.
     */
    final OperationMethod method(final DefaultCoordinateOperationFactory opFactory) throws FactoryException {
        if (method == null) {
            final String name = parameters.remove(PROJ);
            if (name == null) {
                throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.ElementNotFound_1, PROJ));
            }
            method = opFactory.getOperationMethod(name);
        }
        return method;
    }

    /**
     * Returns the parameter value group filled from the {@literal Proj.4} parameters.
     *
     * @throws IllegalArgumentException if a Proj.4 parameter can not be converted to the expected type.
     */
    final ParameterValueGroup parameters() throws IllegalArgumentException {
        final ParameterValueGroup pg = method.getParameters().createValue();
        for (final Map.Entry<String,String> entry : parameters.entrySet()) {
            final String keyword = entry.getKey();
            if (!EXCLUDES.contains(keyword)) {
                final ParameterValue<?> value = pg.parameter(keyword);
                value.setValue(Double.parseDouble(entry.getValue()));
            }
        }
        return pg;
    }

    /**
     * Returns the vertical or horizontal axis unit.
     * This unit applies only to linear axes, not angular axes neither parameters.
     *
     * @param  vertical  {@code true} for querying the vertical unit, or {@code false} for the horizontal one.
     * @return the vertical or horizontal axis unit of measurement, or {@code Units.METRE} if unspecified.
     * @throws NumberFormatException if the unit conversion factor can not be parsed.
     * @throws ParserException if the unit symbol can not be parsed.
     */
    final Unit<?> unit(final boolean vertical) throws ParserException {
        String v = parameters.remove(vertical ? "vto_meter" : "to_meter");
        if (v != null) {
            return Units.METRE.divide(Double.parseDouble(v));
        }
        v = parameters.remove(vertical ? "vunits" : "units");
        if (v != null) {
            return Units.valueOf(v);
        }
        return Units.METRE;
    }
}
