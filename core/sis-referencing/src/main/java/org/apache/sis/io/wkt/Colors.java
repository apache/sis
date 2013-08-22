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
import java.io.ObjectStreamException;
import org.apache.sis.internal.util.X364;
import org.apache.sis.util.resources.Errors;


/**
 * The colors to use for formatting <cite>Well Known Text</cite> (WKT) objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 *
 * @see WKTFormat#getColors()
 * @see WKTFormat#setColors(Colors)
 */
public class Colors implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 256160285861027191L;

    /**
     * The immutable default set of colors.
     *
     * @see Symbols#DEFAULT
     */
    public static final Colors DEFAULT = new Immutable();
    static {
        final EnumMap<ElementKind,X364> map = DEFAULT.map;
        map.put(ElementKind.NUMBER,     X364.FOREGROUND_YELLOW);
        map.put(ElementKind.INTEGER,    X364.FOREGROUND_YELLOW);
        map.put(ElementKind.UNIT,       X364.FOREGROUND_YELLOW);
        map.put(ElementKind.AXIS,       X364.FOREGROUND_CYAN);
        map.put(ElementKind.CODE_LIST,  X364.FOREGROUND_CYAN);
        map.put(ElementKind.PARAMETER,  X364.FOREGROUND_GREEN);
        map.put(ElementKind.METHOD,     X364.FOREGROUND_GREEN);
        map.put(ElementKind.DATUM,      X364.FOREGROUND_GREEN);
        map.put(ElementKind.ERROR,      X364.BACKGROUND_RED);
    }

    /**
     * The map of colors.
     */
    private final EnumMap<ElementKind,X364> map;

    /**
     * Creates a new, initially empty, set of colors.
     */
    public Colors() {
        map = new EnumMap<>(ElementKind.class);
    }

    /**
     * Creates a new set of colors initialized to a copy of the given one.
     *
     * @param colors The set of colors to copy.
     */
    public Colors(final Colors colors) {
        map = new EnumMap<>(colors.map);
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
    public void set(final ElementKind key, final String color) throws IllegalArgumentException {
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
    public String get(final ElementKind key) {
        final X364 color = map.get(key);
        return (color != null) ? color.color : null;
    }

    /**
     * Returns the ANSI sequence for the given syntactic element, or {@code null} if none.
     */
    final String getAnsiSequence(final ElementKind key) {
        final X364 color = map.get(key);
        return (color != null) ? color.sequence() : null;
    }

    /**
     * An immutable subclass of {@link Colors} for the {@link Colors#DEFAULT} constant
     * or for the object to be used by {@link WKTFormat}.
     */
    static final class Immutable extends Colors {
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
         * Creates a immutable copy of the given set of colors.
         */
        Immutable(final Colors colors) {
            super(colors);
        }

        /**
         * Do not allow color changes.
         */
        @Override
        public void set(final ElementKind key, final String color) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Colors"));
        }

        /**
         * Replaces the deserialized instance by {@link #DEFAULT} one if possible.
         */
        Object readResolve() throws ObjectStreamException {
            return super.map.equals(DEFAULT.map) ? DEFAULT : this;
        }
    }
}
