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
package org.apache.sis.console;

import java.util.Locale;
import org.apache.sis.util.resources.Errors;


/**
 * All command-line options allowed by the SIS command line. The name used on the command-line
 * is the lower-cases variant of the enumeration name, except for a few cases with camel cases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum Option {
    /**
     * The Coordinate Reference System of input data.
     */
    SOURCE_CRS(true),

    /**
     * The Coordinate Reference System of input data.
     */
    TARGET_CRS(true),

    /**
     * The Coordinate Operation to apply on data.
     * This option can be used as an alternative to the {@link #SOURCE_CRS} and {@link #TARGET_CRS} pair.
     */
    OPERATION(true),

    /**
     * Use the inverse of the coordinate operation. The transform will be inverted <em>after</em> all
     * other options ({@link #OPERATION}, {@link #SOURCE_CRS} and {@link #TARGET_CRS}) have been applied.
     */
    INVERSE(false),

    /**
     * Relative path to an auxiliary metadata file.
     */
    METADATA(true),

    /**
     * The file to write.
     */
    OUTPUT(true),

    /**
     * The output format. Examples: {@code "xml"}, {@code "text"}.
     */
    FORMAT(true),

    /**
     * The locale for the output produced by the command.
     */
    LOCALE(true),

    /**
     * The timezone for the dates to be formatted.
     */
    TIMEZONE(true),

    /**
     * The encoding for the output produced by the command.
     */
    ENCODING(true),

    /**
     * Whether colorized output shall be enabled.
     */
    COLORS(true),

    /**
     * Whether the output should contain only brief information.
     * This option expects no value.
     */
    BRIEF(false),

    /**
     * Whether the output should contain more detailed information.
     * This option expects no value.
     */
    VERBOSE(false),

    /**
     * Whether to print the full stack trace in case of error.
     * This option expects no value.
     */
    DEBUG(false),

    /**
     * Lists the options accepted by a command.
     */
    HELP(false);

    /**
     * The prefix to prepend to option names.
     */
    static final String PREFIX = "--";

    /**
     * Boolean values accepted on the command line. Values at even indices are {@code false}
     * and values at odd indices are {@code true}.
     *
     * @see #parseBoolean(Object)
     */
    private static final String[] BOOLEAN_VALUES = {
        "false", "true",
        "off",   "on",
        "no",    "yes"
    };

    /**
     * The string representation of this option, as used on the command line.
     * This is usually the lower-case version of {@link #name()}.
     *
     * @see #label()
     */
    private String label;

    /**
     * Whether this option expects a value.
     */
    final boolean hasValue;

    /**
     * Creates a new option.
     *
     * @param hasValue  whether this option expects a value.
     */
    private Option(final boolean hasValue) {
        this.hasValue = hasValue;
    }

    /**
     * Special case for which {@code label()} should not be only the lower-case of enum name.
     */
    static {
        SOURCE_CRS.label = "sourceCRS";
        TARGET_CRS.label = "targetCRS";
    }

    /**
     * Return the string representation as used on the command line.
     */
    synchronized String label() {
        if (label == null) {
            label = name().toLowerCase(Locale.US);
        }
        return label;
    }

    /**
     * Returns the option for the given string.
     */
    static Option forLabel(final String label) throws InvalidOptionException {
        for (final Option option : values()) {
            if (label.equalsIgnoreCase(option.name().replace("_", ""))) {
                return option;
            }
        }
        throw new InvalidOptionException(Errors.format(Errors.Keys.UnknownOption_1, label), label);
    }

    /**
     * Parses the given value as a boolean.
     *
     * @param  value  the value to parse.
     * @return the value as a boolean.
     * @throws InvalidOptionException if the given value is not recognized as a boolean.
     */
    boolean parseBoolean(final Object value) throws InvalidOptionException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        final String s = value.toString();
        for (int i=0; i<BOOLEAN_VALUES.length; i++) {
            if (s.equalsIgnoreCase(BOOLEAN_VALUES[i])) {
                return (i & 1) != 0;
            }
        }
        final String name = label();
        throw new InvalidOptionException(Errors.format(Errors.Keys.IllegalOptionValue_2, name, value), name);
    }
}
