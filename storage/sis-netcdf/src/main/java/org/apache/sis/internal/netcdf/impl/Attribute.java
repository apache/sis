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
import org.apache.sis.util.Debug;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Function;


/**
 * Attribute found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
final class Attribute {
    /**
     * The function for obtaining the name of an attribute.
     */
    static final Function<Attribute,String> NAME_FUNCTION = new Function<Attribute,String>() {
        @Override public String apply(final Attribute value) {
            return value.name;
        }
    };

    /**
     * The attribute name.
     */
    final String name;

    /**
     * The value, either as a {@link String} or as an array of primitive type.
     * Never {@code null} and never an empty string or empty array.
     *
     * <p>If the value is a {@code String}, then leading and trailing spaces and control characters
     * have been trimmed by {@link String#trim()}.</p>
     */
    final Object value;

    /**
     * Creates a new attribute of the given name and value.
     *
     * @param name  The attribute name (can not be null).
     * @param value The value (trimmed if a {@code String}).
     */
    Attribute(final String name, final Object value) {
        this.name  = name;
        this.value = value;
    }

    /**
     * Returns the attribute values as an array of {@link String}.
     *
     * @see VariableInfo#getAttributeValues(String, boolean)
     */
    final String[] stringValues() {
        if (value instanceof String) {
            return new String[] {(String) value};
        }
        final String[] values = new String[Array.getLength(value)];
        for (int i=0; i<values.length; i++) {
            values[i] = Array.get(value, i).toString();
        }
        return values;
    }

    /**
     * Returns the attribute values as an array of {@link Number}, or an empty array if none.
     *
     * @see VariableInfo#getAttributeValues(String, boolean)
     */
    final Number[] numberValues() {
        final Number[] values = new Number[(value instanceof String) ? 0 : Array.getLength(value)];
        for (int i=0; i<values.length; i++) {
            values[i] = (Number) Array.get(value, i);
        }
        return values;
    }

    /**
     * Returns the attribute value as a boolean, or {@code false} if the attribute is not a boolean.
     *
     * @see VariableInfo#isUnsigned()
     */
    final boolean booleanValue() {
        return (value instanceof String) && Boolean.valueOf((String) value);
    }

    /**
     * Modifies, if needed, the given date in order to make it compliant with the ISO 8601 format.
     * For example missing minutes or seconds fields are automatically added. The intend is to turn
     * a NetCDF date into something parseable by {@code java.util.time} or {@code javax.xml.bind}.
     *
     * @param  date The date to parse, or {@code null}.
     * @return The date modified if needed or {@code null} if the given string was {@code null}.
     *
     * @since 0.5 (derived from 0.3)
     */
    static String dateToISO(String date) {
        date = CharSequences.trimWhitespaces(date);
        if (date != null && !date.isEmpty()) {
            /*
             * Check for missing time fields and time zone. For example if the given date is
             * "2005-09-22T00:00", then this block will complete it as "2005-09-22T00:00:00".
             * In addition, a 'Z' suffix will be appended if 'defaultToUTC' is true.
             */
            int timeFieldStart  = date.lastIndexOf('T') + 1; // 0 if there is no time field.
            int timeFieldEnd    = date.length();             // To be updated if there is a time field.
            int missingFields   = 2;                         // Number of missing time fields.
            boolean hasTimeZone = date.charAt(timeFieldEnd - 1) == 'Z';
            if (timeFieldStart != 0) {
                if (hasTimeZone) {
                    timeFieldEnd--;
                } else {
                    final int s = Math.max(date.indexOf('+', timeFieldStart),
                                           date.indexOf('-', timeFieldStart));
                    if (hasTimeZone = (s >= 0)) {
                        timeFieldEnd = s;
                    }
                }
                for (int i=timeFieldStart; i<timeFieldEnd; i++) {
                    if (date.charAt(i) == ':') {
                        if (--missingFields == 0) break;
                    }
                }
            }
            /*
             * If we have determined that there is some missing time fields,
             * append default values for them.
             */
            CharSequence modified = date;
            if (missingFields != 0 || !hasTimeZone) {
                final StringBuilder buffer = new StringBuilder(date);
                buffer.setLength(timeFieldEnd);
                if (timeFieldStart == 0) {
                    buffer.append("T00");
                }
                while (--missingFields >= 0) {
                    buffer.append(":00");
                }
                if (hasTimeZone) {
                    buffer.append(date, timeFieldEnd, date.length());
                } else {
                    buffer.append('Z');
                }
                modified = buffer;
            }
            /*
             * Now ensure that all numbers have at least two digits.
             */
            int indexOfLastDigit = 0;
            for (int i=modified.length(); --i >= 0;) {
                char c = modified.charAt(i);
                final boolean isDigit = (c >= '0' && c <= '9'); // Do not use Character.isDigit(char).
                if (indexOfLastDigit == 0) {
                    // We were not scaning a number. Check if we are now starting doing so.
                    if (isDigit) {
                        indexOfLastDigit = i;
                    }
                } else {
                    // We were scaning a number. Check if we found the begining.
                    if (!isDigit) {
                        if (indexOfLastDigit - i == 1) {
                            // Reuse the buffer if it exists, or create a new one otherwise.
                            final StringBuilder buffer;
                            if (modified == date) {
                                modified = buffer = new StringBuilder(date);
                            } else {
                                buffer = (StringBuilder) modified;
                            }
                            buffer.insert(i+1, '0');
                        }
                        indexOfLastDigit = 0;
                    }
                }
            }
            date = modified.toString();
        }
        return date;
    }

    /**
     * A string representation of this dimension for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return name + " = " + Utilities.deepToString(value);
    }
}
