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

import java.awt.Color;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import javafx.util.Callback;
import javafx.scene.control.TableColumn;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.sis.coverage.Category;
import org.apache.sis.internal.coverage.j2d.Colorizer;


/**
 * Colors to apply on coverages based on their {@link Category} instances.
 *
 * <p>The interfaces implemented by this class are implementation convenience
 * that may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageStyling implements Function<Category,Color[]>,
        Callback<TableColumn.CellDataFeatures<Category,CategoryColors>, ObservableValue<CategoryColors>>
{
    /**
     * Customized colors selected by user. Keys are English names of categories.
     *
     * @see #key(Category)
     */
    private final Map<String,int[]> customizedColors;

    /**
     * The fallback to use if no color is defined in this {@code CoverageStyling} for a category.
     */
    private final Function<Category,Color[]> fallback;

    /**
     * The view to notify when a color changed, or {@code null} if none.
     */
    private final CoverageCanvas canvas;

    /**
     * Creates a new styling instance.
     */
    CoverageStyling(final CoverageCanvas canvas) {
        customizedColors = new HashMap<>();
        this.canvas = canvas;
        if (canvas != null) {
            final Function<Category, Color[]> c = canvas.getCategoryColors();
            if (c != null) {
                fallback = c;
                return;
            }
        }
        fallback = Colorizer.GRAYSCALE;
    }

    /**
     * Returns the key to use in {@link #customizedColors} for the given category.
     */
    private static String key(final Category category) {
        return category.getName().toString(Locale.ENGLISH);
    }

    /**
     * Associates colors to the given category.
     */
    final void setARGB(final Category category, final int[] colors) {
        final String key = key(category);
        final int[] old;
        if (colors != null && colors.length != 0) {
            old = customizedColors.put(key, colors);
        } else {
            old = customizedColors.remove(key);
        }
        if (canvas != null && !Arrays.equals(colors, old)) {
            canvas.setCategoryColors(this);                     // Causes a repaint event.
        }
    }

    /**
     * Same as {@link #apply(Category)}, but returns colors as an array of ARGB codes.
     * Contrarily to {@link #apply(Category)}, this method may return references to
     * internal arrays; <strong>do not modify.</strong>
     */
    private int[] getARGB(final Category category) {
        int[] ARGB = customizedColors.get(key(category));
        if (ARGB == null) {
            final Color[] colors = fallback.apply(category);
            if (colors != null) {
                ARGB = new int[colors.length];
                for (int i=0; i<colors.length; i++) {
                    ARGB[i] = colors[i].getRGB();
                }
            }
        }
        return ARGB;
    }

    /**
     * Returns the colors to apply for the given category, or {@code null} for transparent.
     * This method returns copies of internal arrays; changes to the returned array do not
     * affect this {@code CoverageStyling} (assuming {@link #fallback} also does copies).
     *
     * @param  category  the category for which to get the colors.
     * @return colors to apply for the given category, or {@code null}.
     */
    @Override
    public Color[] apply(final Category category) {
        final int[] ARGB = customizedColors.get(key(category));
        if (ARGB != null) {
            final Color[] colors = new Color[ARGB.length];
            for (int i=0; i<colors.length; i++) {
                colors[i] = new Color(ARGB[i], true);
            }
            return colors;
        }
        return fallback.apply(category);
    }

    /**
     * Invoked by {@link TableColumn} for computing value of a {@link CategoryColorsCell}.
     * This method is public as an implementation side-effect; do not rely on that.
     */
    @Override
    public ObservableValue<CategoryColors> call(final TableColumn.CellDataFeatures<Category,CategoryColors> cell) {
        final Category category = cell.getValue();
        if (category != null) {
            final int[] ARGB = getARGB(category);
            if (ARGB != null) {
                return new SimpleObjectProperty<>(new CategoryColors(ARGB));
            }
        }
        return null;
    }
}
