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

import java.util.EnumMap;
import java.io.Serializable;
import org.apache.sis.internal.util.X364;


/**
 * The colors to use for formatting <cite>Well Known Text</cite> (WKT) objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public class Colors implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 256160285861027191L;

    /**
     * The immutable default set of colors.
     */
    public static final Colors DEFAULT = new Immutable();
    static {
        final EnumMap<Element,X364> map = DEFAULT.map;
        map.put(Element.NUMBER,     X364.FOREGROUND_YELLOW);
        map.put(Element.INTEGER,    X364.FOREGROUND_YELLOW);
        map.put(Element.UNIT,       X364.FOREGROUND_YELLOW);
        map.put(Element.AXIS,       X364.FOREGROUND_CYAN);
        map.put(Element.CODE_LIST,  X364.FOREGROUND_CYAN);
        map.put(Element.PARAMETER,  X364.FOREGROUND_GREEN);
        map.put(Element.METHOD,     X364.FOREGROUND_GREEN);
        map.put(Element.DATUM,      X364.FOREGROUND_GREEN);
        map.put(Element.ERROR,      X364.BACKGROUND_RED);
    }

    /**
     * Keys for syntactic elements to be colorized.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.4 (derived from geotk-3.00)
     * @version 0.4
     * @module
     */
    public static enum Element {
        /**
         * Floating point numbers (excluding integer types).
         */
        NUMBER,

        /**
         * Integer numbers.
         */
        INTEGER,

        /**
         * {@linkplain javax.measure.unit.Unit Units of measurement}.
         * In referencing WKT, this is the text inside {@code UNIT} elements.
         */
        UNIT,

        /**
         * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis Axes}.
         * In referencing WKT, this is the text inside {@code AXIS} elements.
         */
        AXIS,

        /**
         * {@linkplain org.opengis.util.CodeList Code list} values.
         */
        CODE_LIST,

        /**
         * {@linkplain org.opengis.parameter.ParameterValue Parameter values}.
         * In referencing WKT, this is the text inside {@code PARAMETER} elements.
         */
        PARAMETER,

        /**
         * {@linkplain org.opengis.referencing.operation.OperationMethod Operation methods}.
         * In referencing WKT, this is the text inside {@code PROJECTION} elements.
         */
        METHOD,

        /**
         * {@linkplain org.opengis.referencing.datum.Datum Datum}.
         * In referencing WKT, this is the text inside {@code DATUM} elements.
         */
        DATUM,

        /**
         * Unformattable elements.
         */
        ERROR
    }

    /**
     * The map of colors.
     */
    private final EnumMap<Element,X364> map;

    /**
     * Creates a new, initially empty, set of colors.
     */
    public Colors() {
        map = new EnumMap<>(Element.class);
    }

    /**
     * Sets the color of the given syntactic element.
     * The color names supported in the current implementation are
     * {@code "red"}, {@code "green"}, {@code "yellow"}, {@code "blue"}, {@code "magenta"}, {@code "cyan"}
     * and {@code "gray"}.
     *
     * @param  key   The syntactic element for which to set the color.
     * @param  color The color to give to the specified element, or {@code null} if none.
     * @throws IllegalArgumentException If the given color name is not recognized.
     */
    public void set(final Element key, final String color) throws IllegalArgumentException {
        if (color == null) {
            map.remove(key);
        } else {
            map.put(key, X364.forColorName(color));
        }
    }

    /**
     * Returns the color for the given syntactic element.
     *
     * @param key The syntactic element for which to get the color.
     * @return The color of the specified element, or {@code null} if none.
     */
    public String get(final Element key) {
        final X364 color = map.get(key);
        return (color != null) ? color.color : null;
    }

    /**
     * An immutable subclass of {@link Colors} for the {@link Colors#DEFAULT} constant.
     */
    private static final class Immutable extends Colors {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -2349530616334766576L;

        /**
         * Creates an initially empty set of colors.
         * Callers must put colors directly in the {@link #map}.
         */
        Immutable() {
        }

        /**
         * Do not allow color changes.
         */
        @Override
        public void set(final Element key, final String color) {
            throw new UnsupportedOperationException();
        }
    }
}
