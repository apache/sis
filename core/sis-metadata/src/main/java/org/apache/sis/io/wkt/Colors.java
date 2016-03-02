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
import org.apache.sis.util.resources.Errors;


/**
 * The colors to use for formatting <cite>Well Known Text</cite> (WKT) objects.
 * Colors are identified by their names and can be mapped to {@link ElementKind}.
 * The currently supported color names are:
 *
 * <ul>
 *   <li>{@code "red"}, {@code "green"}, {@code "yellow"}, {@code "blue"}, {@code "magenta"}, {@code "cyan"},
 *       {@code "gray"}.</li>
 * </ul>
 *
 * The above list may be expanded in any future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see WKTFormat#getColors()
 * @see WKTFormat#setColors(Colors)
 */
public class Colors implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 256160285861027191L;

    /**
     * The default colors used by {@link FormattableObject#print()}.
     * Those colors give better results on a {@link java.io.Console} with black background.
     * This map is immutable.
     *
     * @see FormattableObject#print()
     */
    public static final Colors DEFAULT = new Colors();
    static {
        final EnumMap<ElementKind,X364> map = DEFAULT.map;
        map.put(ElementKind.NUMBER,     X364.FOREGROUND_YELLOW);
        map.put(ElementKind.INTEGER,    X364.FOREGROUND_YELLOW);
        map.put(ElementKind.UNIT,       X364.FOREGROUND_YELLOW);
        map.put(ElementKind.AXIS,       X364.FOREGROUND_CYAN);
        map.put(ElementKind.CODE_LIST,  X364.FOREGROUND_CYAN);
        map.put(ElementKind.PARAMETER,  X364.FOREGROUND_GREEN);
        map.put(ElementKind.METHOD,     X364.FOREGROUND_GREEN);
        map.put(ElementKind.DATUM,      X364.FOREGROUND_GREEN);     // Note: datum names in SIS are like identifiers.
        map.put(ElementKind.IDENTIFIER, X364.FOREGROUND_RED);
        map.put(ElementKind.SCOPE,      X364.FOREGROUND_GRAY);
        map.put(ElementKind.EXTENT,     X364.FOREGROUND_GRAY);
        map.put(ElementKind.CITATION,   X364.FOREGROUND_GRAY);
        map.put(ElementKind.REMARKS,    X364.FOREGROUND_GRAY);
        map.put(ElementKind.ERROR,      X364.BACKGROUND_RED);
        DEFAULT.isImmutable = true;
    }

    /**
     * Emphases on identification information
     * ({@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() name} and
     *  {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers() identifiers}) only.
     * This map is immutable.
     */
    public static final Colors NAMING = new Colors();
    static {
        final EnumMap<ElementKind,X364> map = NAMING.map;
        map.put(ElementKind.NAME,       X364.FOREGROUND_GREEN);
        map.put(ElementKind.IDENTIFIER, X364.FOREGROUND_YELLOW);
        NAMING.isImmutable = true;
    }

    /**
     * The map of colors.
     * Consider this field as final — it is modified only by {@link #clone()}.
     */
    private EnumMap<ElementKind,X364> map;

    /**
     * {@code true} if this instance shall be considered as immutable.
     */
    private boolean isImmutable;

    /**
     * Creates a new, initially empty, set of colors.
     */
    public Colors() {
        map = new EnumMap<ElementKind,X364>(ElementKind.class);
    }

    /**
     * Creates a new set of colors initialized to a copy of the given one.
     *
     * @param colors The set of colors to copy.
     */
    public Colors(final Colors colors) {
        map = new EnumMap<ElementKind,X364>(colors.map);
    }

    /**
     * Sets the color of the given syntactic element from a color name.
     * The color names supported in the current implementation are
     * {@code "red"}, {@code "green"}, {@code "yellow"}, {@code "blue"}, {@code "magenta"}, {@code "cyan"}
     * and {@code "gray"}, case-insensitive.
     *
     * @param  key   The syntactic element for which to set the color.
     * @param  color The color to give to the specified element, or {@code null} if none.
     * @throws IllegalArgumentException If the given color name is not recognized.
     */
    public void setName(final ElementKind key, final String color) throws IllegalArgumentException {
        if (isImmutable) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Colors"));
        }
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
    public final String getName(final ElementKind key) { // Declared final for consistency with getAnsiSequence(…)
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
     * Returns an immutable copy of this set of colors, or {@code this} if this instance is already immutable.
     */
    final Colors immutable() {
        if (isImmutable) {
            return this;
        }
        final Colors clone = clone();
        clone.isImmutable = true;
        return clone;
    }

    /**
     * Returns a clone of this {@code Colors}.
     *
     * @return A clone of this {@code Colors}.
     */
    @Override
    public Colors clone() {
        final Colors clone;
        try {
            clone = (Colors) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        clone.map = clone.map.clone();
        clone.isImmutable = false;
        return clone;
    }

    /**
     * Compares this {@code Colors} with the given object for equality.
     *
     * @param  other The object to compare with this {@code Colors}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Colors) {
            final Colors that = (Colors) other;
            return map.equals(that.map);
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return map.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Replaces the deserialized instance by {@link #DEFAULT} one if possible.
     *
     * @return The object to use after deserialization.
     */
    final Object readResolve() {
        return isImmutable && map.equals(DEFAULT.map) ? DEFAULT : this;
    }
}
