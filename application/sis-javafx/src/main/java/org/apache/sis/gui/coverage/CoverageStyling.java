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
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.value.ObservableValue;
import org.apache.sis.coverage.Category;
import org.apache.sis.internal.coverage.j2d.Colorizer;
import org.apache.sis.internal.gui.ImmutableObjectProperty;
import org.apache.sis.internal.gui.control.ColorRamp;
import org.apache.sis.internal.gui.control.ColorColumnHandler;
import org.apache.sis.util.resources.Vocabulary;
import org.opengis.util.InternationalString;


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
final class CoverageStyling extends ColorColumnHandler<Category> implements Function<Category,Color[]> {
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
     *
     * @param  colors  the new color for the given category, or {@code null} for resetting default value.
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
     * Returns the colors to apply for the given category, or {@code null} for transparent.
     * Does the same work as {@link #apply(Category)}, but returns colors as an array of ARGB codes.
     * Contrarily to {@link #apply(Category)}, this method may return references to internal arrays;
     * <strong>do not modify.</strong>
     */
    @Override
    protected int[] getARGB(final Category category) {
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
     * Invoked when users confirmed that (s)he wants to use the selected colors.
     *
     * @param  value    the category for which to assign new color(s).
     * @param  newItem  the new color for the given category, or {@code null} for resetting default value.
     * @return the type of color (solid or gradient) shown for the given value.
     */
    @Override
    protected ColorRamp.Type applyColors(final Category value, ColorRamp newItem) {
        setARGB(value, (newItem != null) ? newItem.colors : null);
        return value.isQuantitative() ? ColorRamp.Type.GRADIENT : ColorRamp.Type.SOLID;
    }

    /**
     * Creates a table showing the color of a qualitative or quantitative coverage categories.
     * The color can be modified by selecting the table row, then clicking on the color.
     *
     * @param  vocabulary  resources for the locale in use.
     */
    final TableView<Category> createCategoryTable(final Vocabulary vocabulary) {
        final TableColumn<Category,String> name = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Name));
        name.setCellValueFactory(CoverageStyling::getCategoryName);
        name.setCellFactory(CoverageStyling::createNameCell);
        name.setEditable(false);
        name.setId("name");

        final TableView<Category> table = new TableView<>();
        table.getColumns().add(name);
        addColumnTo(table, vocabulary);
        return table;
    }

    /**
     * Invoked for creating a cell for the "name" column.
     * Returns the JavaFX default cell except for vertical alignment, which is centered.
     */
    private static TableCell<Category,String> createNameCell(final TableColumn<Category,String> column) {
        @SuppressWarnings("unchecked")
        final TableCell<Category,String> cell = (TableCell<Category,String>) TableColumn.DEFAULT_CELL_FACTORY.call(column);
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    /**
     * Invoked when the table needs to render a text in the "Name" column of the category table.
     */
    private static ObservableValue<String> getCategoryName(final TableColumn.CellDataFeatures<Category,String> cell) {
        final InternationalString name = cell.getValue().getName();
        return (name != null) ? new ImmutableObjectProperty<>(name.toString()) : null;
    }
}
