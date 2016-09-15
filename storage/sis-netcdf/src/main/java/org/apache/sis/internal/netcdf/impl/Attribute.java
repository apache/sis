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
package org.apache.sis.internal.netcdf.impl;

import java.lang.reflect.Array;
import org.apache.sis.util.CharSequences;


/**
 * Static methods about attributes found in a NetCDF file.
 * Values can be:
 *
 * <ul>
 *   <li>a {@link String}</li>
 *   <li>A {@link Number}</li>
 *   <li>an array of primitive type</li>
 * </ul>
 *
 * If the value is a {@code String}, then leading and trailing spaces and control characters
 * should be trimmed by {@link String#trim()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
final class Attribute {
    /**
     * The array to be returned by {@link #numberValues(Object)} when the given value is null.
     */
    private static final Number[] EMPTY = new Number[0];

    /**
     * Do not allow instantiation of this class.
     */
    private Attribute() {
    }

    /**
     * Returns the attribute values as an array of {@link String}s, or an empty array if none.
     *
     * @see VariableInfo#getAttributeValues(String, boolean)
     */
    static String[] stringValues(final Object value) {
        if (value == null) {
            return CharSequences.EMPTY_ARRAY;
        }
        if (value.getClass().isArray()) {
            final String[] values = new String[Array.getLength(value)];
            for (int i=0; i<values.length; i++) {
                values[i] = Array.get(value, i).toString();
            }
            return values;
        }
        return new String[] {value.toString()};
    }

    /**
     * Returns the attribute values as an array of {@link Number}, or an empty array if none.
     *
     * @see VariableInfo#getAttributeValues(String, boolean)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    static Number[] numberValues(final Object value) {
        if (value != null) {
            if (value.getClass().isArray()) {
                final Number[] values = new Number[Array.getLength(value)];
                for (int i=0; i<values.length; i++) {
                    values[i] = (Number) Array.get(value, i);
                }
                return values;
            }
            if (value instanceof Number) {
                return new Number[] {(Number) value};
            }
        }
        return EMPTY;
    }

    /**
     * Returns the attribute value as a boolean, or {@code false} if the attribute is not a boolean.
     *
     * @see VariableInfo#isUnsigned()
     */
    static boolean booleanValue(final Object value) {
        return (value instanceof String) && Boolean.valueOf((String) value);
    }
}
