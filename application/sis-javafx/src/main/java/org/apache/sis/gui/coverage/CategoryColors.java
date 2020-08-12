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
package org.apache.sis.gui.coverage;

import java.util.Arrays;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import org.apache.sis.internal.gui.ColorName;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.GUIUtilities;


/**
 * Represents a single color or a color ramp that can be represented in {@link CategoryColorsCell}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CategoryColors {
    /**
     * Default color palette.
     */
    static final CategoryColors GRAYSCALE = new CategoryColors(0xFF000000, 0xFFFFFFFF);

    /**
     * Blue – Cyan – White – Yellow – Red.
     */
    static final CategoryColors BELL = new CategoryColors(0xFF0000FF, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFFFF00, 0xFFFF0000);

    /**
     * ARGB codes of this single color or color ramp.
     * If null or empty, then default to transparent.
     */
    final int[] colors;

    /**
     * The paint for this palette, created when first needed.
     */
    private transient Paint paint;

    /**
     * A name for this palette, computed when first needed.
     *
     * @see #toString()
     */
    private transient String name;

    /**
     * Creates a new palette for the given colors.
     */
    CategoryColors(final int... colors) {
        this.colors = colors;
    }

    /**
     * Declares this {@code CategoryColors} as the selected item in the given chooser.
     * If this instance is not found, then it is added to the chooser list.
     */
    final void asSelectedItem(final ComboBox<CategoryColors> colorRampChooser) {
        final ObservableList<CategoryColors> items = colorRampChooser.getItems();
        int i = items.indexOf(this);
        if (i < 0) {
            i = items.size();
            items.add(this);
        }
        colorRampChooser.getSelectionModel().select(i);
    }

    /**
     * Returns the first color, or {@code null} if none.
     * This is used for qualitative categories, which are expected to contain only one color.
     */
    final Color firstColor() {
        if (colors != null && colors.length != 0) {
            return GUIUtilities.fromARGB(colors[0]);
        } else {
            return null;
        }
    }

    /**
     * Gets the paint to use for filling a rectangle using this color palette.
     * Returns {@code null} if this {@code CategoryColors} contains no color.
     */
    final Paint paint() {
        if (paint == null) {
            switch (colors.length) {
                case 0: break;
                case 1: {
                    paint = GUIUtilities.fromARGB(colors[0]);
                    break;
                }
                default: {
                    final Stop[] stops = new Stop[colors.length];
                    final double scale = 1d / (stops.length - 1);
                    for (int i=0; i<stops.length; i++) {
                        stops[i] = new Stop(scale*i, GUIUtilities.fromARGB(colors[i]));
                    }
                    paint = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
                }
            }
        }
        return paint;
    }

    /**
     * Returns a string representation of this color palette.
     * This string representation will appear in the combo box when that box is shown.
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
     * Returns a hash code value for this palette.
     * Defined mostly for consistency with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(colors) ^ 81;
    }

    /**
     * Returns whether the given object is equal to this {@code CategoryColors}.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof CategoryColors) && Arrays.equals(colors, ((CategoryColors) other).colors);
    }
}
