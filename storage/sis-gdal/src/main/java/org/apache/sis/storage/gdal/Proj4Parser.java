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
    private static final Set<String> EXCLUDES = new HashSet<>(12);
    static {
        EXCLUDES.add("init");           // Authority code
        EXCLUDES.add("datum");          // Geodetic datum name
        EXCLUDES.add("ellps");          // Ellipsoid name
        EXCLUDES.add("pm");             // Prime meridian
        EXCLUDES.add("units");          // Axis units
        EXCLUDES.add("towgs84");        // Datum shift
        EXCLUDES.add("axis");           // Axis order
    }

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
                if (!EXCLUDES.contains(keyword)) {
                    final String value = CharSequences.trimWhitespaces(definition, end+1, (next >= 0) ? next : length).toString();
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
     * Returns the operation method inferred from the {@code "proj"} parameter value.
     * This method must be invoked at least once before {@link #parameters()}.
     */
    final OperationMethod method(final DefaultCoordinateOperationFactory opFactory) throws FactoryException {
        if (method == null) {
            final String name = parameters.remove("proj");
            if (name == null) {
                throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.ElementNotFound_1, "proj"));
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
            final ParameterValue<?> value = pg.parameter(entry.getKey());
            value.setValue(Double.parseDouble(entry.getValue()));
        }
        return pg;
    }
}
