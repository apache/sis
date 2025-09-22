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
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.SimpleObjectProperty;
import org.opengis.util.InternationalString;
import org.apache.sis.coverage.Category;
import org.apache.sis.image.Colorizer;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.ImmutableObjectProperty;
import org.apache.sis.gui.controls.ColorRamp;
import org.apache.sis.gui.controls.ColorColumnHandler;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Colors to apply on coverages based on their {@link Category} instances.
 *
 * <p>The interfaces implemented by this class are implementation convenience
 * that may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoverageStyling extends ColorColumnHandler<Category>
        implements Function<Category, Color[]>, ChangeListener<ColorRamp>
{
    /**
     * Customized colors selected by user. Keys are English names of categories.
     *
     * @see #apply(Category)
     */
    private final Map<String, SimpleObjectProperty<ColorRamp>> customizedColors;

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
            canvas.setColorizer(Colorizer.forCategories(this));
        }
    }

    /**
     * Copies styling information from the given source.
     * This is used when the user clicks on "New window" button.
     */
    final void copyStyling(final CoverageStyling source) {
        for (Map.Entry<String, SimpleObjectProperty<ColorRamp>> entry : source.customizedColors.entrySet()) {
            final var property = new SimpleObjectProperty<>(entry.getValue().getValue());
            customizedColors.put(entry.getKey(), property);
            property.addListener(this);
        }
        if (canvas != null) {
            canvas.stylingChanged();
        }
    }

    /**
     * Resets all colors to their default values.
     *
     * @param items  list of items of the table to clear.
     */
    private void clear(final ObservableList<Category> items) {
        final Category[] content = items.toArray(Category[]::new);
        items.clear();              // For forcing a repaint of the table.
        customizedColors.clear();
        items.setAll(content);
        if (canvas != null) {
            canvas.stylingChanged();
        }
    }

    /**
     * Returns the key to use in {@link #customizedColors} for the given category.
     */
    private static String key(final Category category) {
        return category.getName().toString(Locale.ROOT);
    }

    /**
     * Returns the colors to apply for the given category as an observable value.
     *
     * @param  row  the row item for which to get color to show in color cell. Never {@code null}.
     * @return the color(s) to use for the given row. A property value of {@code null} means transparent.
     */
    @Override
    protected SimpleObjectProperty<ColorRamp> getObservableValue(final Category category) {
        return customizedColors.computeIfAbsent(key(category), (key) -> {
            final var property = new SimpleObjectProperty<ColorRamp>();
            if (category.isQuantitative()) {
                property.setValue(ColorRamp.DEFAULT);
            }
            property.addListener(this);
            return property;
        });
    }

    /**
     * Invoked when the colors have changed. This method notifies the canvas that it needs to repaint itself.
     */
    @Override
    public void changed(ObservableValue<? extends ColorRamp> property, ColorRamp old, ColorRamp colors) {
        if (canvas != null && !Objects.equals(colors, old)) {
            canvas.stylingChanged();
        }
    }

    /**
     * Returns the colors to apply for the given category, or {@code null} for default.
     * This method returns copies of internal arrays; changes to the returned array do
     * not affect this {@code CoverageStyling}.
     *
     * @param  category  the category for which to get the colors.
     * @return colors to apply for the given category, or {@code null} if the category is unrecognized.
     */
    @Override
    public Color[] apply(final Category category) {
        final SimpleObjectProperty<ColorRamp> property = customizedColors.get(key(category));
        if (property != null) {
            final ColorRamp ramp = property.getValue();
            if (ramp != null) {
                final int[] ARGB = ramp.colors;
                if (ARGB != null) {
                    final var colors = new Color[ARGB.length];
                    for (int i=0; i<colors.length; i++) {
                        colors[i] = new Color(ARGB[i], true);
                    }
                    return colors;
                }
            }
        }
        return null;
    }

    /**
     * Associates colors to the given category. This method is invoked when new categories are shown
     * in table column managed by this {@code CoverageStyling}, and when user selects new colors.
     *
     * @param  category  the category for which to assign new color(s).
     * @param  colors    the new color for the given category, or {@code null} for resetting default value.
     * @return the type of color (solid or gradient) shown for the given value.
     */
    @Override
    protected ColorRamp.Type applyColors(final Category category, final ColorRamp colors) {
        final SimpleObjectProperty<ColorRamp> property;
        if (colors != null) {
            property = getObservableValue(category);
            property.setValue(colors);
        } else {
            property = customizedColors.get(key(category));
            if (property != null) {
                property.setValue(category.isQuantitative() ? ColorRamp.DEFAULT : null);
            }
        }
        return category.isQuantitative() ? ColorRamp.Type.GRADIENT : ColorRamp.Type.SOLID;
    }

    /**
     * Creates a table showing the color of a qualitative or quantitative coverage categories.
     * The color can be modified by selecting the table row, then clicking on the color.
     *
     * @param  vocabulary  localized resources, given because already known by the caller
     *                     (this argument would be removed if this method was public API).
     */
    final TableView<Category> createCategoryTable(final Resources resources, final Vocabulary vocabulary) {
        final var name = new TableColumn<Category,String>(vocabulary.getString(Vocabulary.Keys.Name));
        name.setCellValueFactory(CoverageStyling::getCategoryName);
        name.setCellFactory(CoverageStyling::createNameCell);
        name.setEditable(false);
        name.setId("name");
        /*
         * Create the table with above "category name" column (read-only),
         * and add an editable column for color(s).
         */
        final TableView<Category> table = new TableView<>();
        table.getColumns().add(name);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        addColumnTo(table, vocabulary.getString(Vocabulary.Keys.Colors));
        /*
         * Add contextual menu items.
         */
        final MenuItem reset = new MenuItem(resources.getString(Resources.Keys.ClearAll));
        reset.setOnAction((e) -> clear(table.getItems()));
        table.setContextMenu(new ContextMenu(reset));
        table.setEditable(true);
        return table;
    }

    /**
     * Invoked for creating a cell for the "name" column.
     * Returns the JavaFX default cell except for vertical alignment, which is centered.
     */
    private static TableCell<Category,String> createNameCell(final TableColumn<Category,String> column) {
        @SuppressWarnings("unchecked")
        final var cell = (TableCell<Category,String>) TableColumn.DEFAULT_CELL_FACTORY.call(column);
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
