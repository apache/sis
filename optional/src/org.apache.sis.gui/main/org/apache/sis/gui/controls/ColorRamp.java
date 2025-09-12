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
package org.apache.sis.gui.controls;

import java.util.Arrays;
import java.util.Objects;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import org.apache.sis.gui.internal.ColorName;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A single color or a gradient of colors shown as a rectangle in a {@link ColorCell}.
 * Can also produce a string representation to be shown in a list.
 * Instances should be considered immutable.
 * The same instance may be shared by many cells.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ColorCell#getItem()
 */
public final class ColorRamp {
    /**
     * The type of colors to show in a cell.
     * The type determines how user can choose a value.
     */
    public enum Type {
        /**
         * Single color selected by {@link ColorPicker}.
         */
        SOLID,

        /**
         * Gradient of colors (or a color ramp) selected by {@link ComboBox}.
         */
        GRADIENT
    }

    /**
     * Original colors of the rendered image.
     */
    public static final ColorRamp DEFAULT = new ColorRamp(Vocabulary.format(Vocabulary.Keys.OriginalColors));

    /**
     * Grayscale color ramp.
     */
    private static final ColorRamp GRAYSCALE = new ColorRamp(0xFF000000, 0xFFFFFFFF);

    /**
     * Default colors to put in a {@link ColorCell} combox box.
     * This array shall not be modified.
     */
    static final ColorRamp[] DEFAULTS = {
        DEFAULT,
        GRAYSCALE,
        new ColorRamp(0xFF0000FF, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFFFF00, 0xFFFF0000),  // Blue – Cyan – White – Yellow – Red.
        new ColorRamp(0xFF0000FF, 0xFFFF00FF, 0xFFFF0000)                           // Blue – Magenta – Red.
    };

    /**
     * ARGB codes of this single color or color ramp.
     * If null or empty, then default to transparent.
     *
     * <p><strong>This array should be read-only.</strong> We make it public because this class is internal.
     * If this {@code ColorRamp} class moves to public API, then we would need to replace this public access
     * by an accessor doing a copy.</p>
     *
     * @see #isTransparent()
     */
    public final int[] colors;

    /**
     * A single color created from {@link #colors} when first needed.
     *
     * @see #color()
     */
    private transient Color color;

    /**
     * A gradient of colors created from {@link #colors} when first needed.
     * May be an instance of {@link Color} if there is only one color.
     *
     * @see #paint()
     */
    private transient Paint paint;

    /**
     * A name for this color ramp created from {@link #colors} when first needed.
     *
     * @see #toString()
     */
    private transient String name;

    /**
     * Creates a new item for the given color.
     *
     * @param  color  the solid color.
     */
    public ColorRamp(final Color color) {
        paint = this.color = color;
        colors = new int[] {GUIUtilities.toARGB(color)};
    }

    /**
     * Creates a new item for the given colors.
     *
     * @param  colors  ARGB codes of this single color or color ramp.
     */
    public ColorRamp(final int... colors) {
        this.colors = colors;
    }

    /**
     * Creates a new item for the given text.
     */
    private ColorRamp(final String text) {
        colors = null;
        name = text;
    }

    /**
     * Returns {@code true} if this ramp has no color with a non-zero transparency.
     * If this method returns {@code false}, then {@link #colors} is guaranteed non-empty.
     *
     * @return whether this color ramp is fully transparent.
     */
    public final boolean isTransparent() {
        if (colors != null) {
            for (final int code : colors) {
                if ((code & 0xFF000000) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a solid color to use for filling a rectangle in {@link ColorCell}.
     * If this item has many colors (for example because it uses a gradient),
     * then an arbitrary color is returned.
     *
     * @return single color to show in table cell, or {@code null} if none.
     */
    public final Color color() {
        if (color == null) {
            if (paint instanceof Color) {
                color = (Color) paint;
            } else if (colors != null && colors.length != 0) {
                color = GUIUtilities.fromARGB(colors[colors.length / 2]);
            }
        }
        return color;
    }

    /**
     * Returns the paint to use for filling a rectangle in {@link ColorCell}, or {@code null} if none.
     *
     * @return color or gradient paint for table cell, or {@code null} if none.
     */
    final Paint paint() {
        if (paint == null && colors != null) {
            switch (colors.length) {
                case 0: break;
                case 1: {
                    if (color == null) {
                        color = GUIUtilities.fromARGB(colors[0]);
                    }
                    paint = color;
                    break;
                }
                default: {
                    final Stop[] stops = new Stop[colors.length];
                    final double scale = 1d / (stops.length - 1);
                    for (int i=0; i<stops.length; i++) {
                        stops[i] = new Stop(scale*i, GUIUtilities.fromARGB(colors[i]));
                    }
                    paint = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
                    break;
                }
            }
        }
        return paint;
    }

    /**
     * Returns a string representation of this color ramp.
     * It may be used as an alternative to colored rectangle.
     */
    @Override
    public String toString() {
        if (name == null) {
            final int n;
            if (colors == null || (n = colors.length) == 0) {
                name = Vocabulary.format(Vocabulary.Keys.Transparent);
            } else if (equals(GRAYSCALE)) {
                name = Vocabulary.format(Vocabulary.Keys.Grayscale);
            } else {
                name = ColorName.of(colors[0]);
                if (n > 1) {
                    final StringBuilder buffer = new StringBuilder(name);
                    if (n > 2) {
                        buffer.append(" – ").append(ColorName.of(colors[n / 2]));
                    }
                    name = buffer.append(" – ").append(ColorName.of(colors[n - 1])).toString();
                }
            }
        }
        return name;
    }

    /**
     * Returns a hash code value for this color ramp.
     * Defined mostly for consistency with {@link #equals(Object)}.
     *
     * @return a hash code value for this color ramp.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(colors) ^ 81;
    }

    /**
     * Returns whether the given object is equal to this {@code ColorRamp}.
     * This is used for locating this {@code ColorRamp} in a {@link ComboBox}.
     *
     * @param  obj  the object to compare with {@code this} for equality.
     * @return whether the given object is equal to this color ramp.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ColorRamp) {
            final var other = (ColorRamp) obj;
            if (Arrays.equals(colors, other.colors)) {
                return (colors != null) || Objects.equals(name, other.name);
            }
        }
        return false;
    }
}
