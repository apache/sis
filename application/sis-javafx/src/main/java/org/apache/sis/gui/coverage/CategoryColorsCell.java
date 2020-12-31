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

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ContentDisplay;
import javafx.geometry.Pos;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.control.ColorCell;
import org.apache.sis.coverage.Category;


/**
 * Cell representing the color of a qualitative or quantitative category.
 * The color can be modified by selecting the table row, then clicking on the color.
 *
 * <p>The interfaces implemented by this class are implementation convenience
 * that may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CategoryColorsCell extends ColorCell<Category,CategoryColors> {
    /**
     * The function that determines which colors to apply on a given category.
     * This same instance is shared by all cells of the same category table.
     */
    private final CoverageStyling styling;

    /**
     * The control for selecting a color ramp, or {@code null} if not yet created.
     * This applies to quantitative categories. By contrast, {@link #colorPicker}
     * applies to qualitative categories.
     *
     * @see Category#isQuantitative()
     */
    private ComboBox<CategoryColors> colorRampChooser;

    /**
     * Creates a cell for the colors column.
     * This constructor is for {@link #createTable(CoverageStyling, Vocabulary)} usage only.
     */
    private CategoryColorsCell(final CoverageStyling styling) {
        this.styling = styling;
    }

    /**
     * Returns the initial item for a new cell.
     */
    @Override
    protected CategoryColors getDefaultItem() {
        return CategoryColors.GRAYSCALE;
    }

    /**
     * Creates an item for a new color selected by user.
     * The given object may be an instance of one of the following classes:
     *
     * <ul>
     *   <li>{@link Color} if the chooser was {@link javafx.scene.control.ColorPicker}.</li>
     *   <li>{@link CategoryColors} if the chooser was {@code ComboBox<CategoryColors>}.</li>
     * </ul>
     *
     * @param  value  the color or gradient paint selected by the user.
     * @return the item to store in this cell for the given color or gradient.
     */
    @Override
    protected CategoryColors createItemForSelection(final Object value) {
        if (value instanceof Color) {
            return new CategoryColors(GUIUtilities.toARGB((Color) value));
        } else {
            // A ClassCastException here would be a bug in ColorsCell editors management.
            return (CategoryColors) value;
        }
    }

    /**
     * Returns a color or gradient chooser initialized to the given item.
     * This is invoked when the user clicks on a cell for modifying the color value.
     *
     * @param  colors  the initial color or gradient to show.
     * @return the control to show to user.
     */
    @Override
    protected ComboBoxBase<?> getControlForEdit(final CategoryColors colors) {
        if (getTableRow().getItem().isQuantitative()) {
            if (colorRampChooser == null) {
                colorRampChooser = new ComboBox<>();
                colorRampChooser.setEditable(false);
                colorRampChooser.setCellFactory((column) -> new Ramp());
                colorRampChooser.getItems().setAll(CategoryColors.GRAYSCALE, CategoryColors.BELL);
                addListeners(colorRampChooser);
            }
            colors.asSelectedItem(colorRampChooser);
            return colorRampChooser;
        } else {
            return super.getControlForEdit(colors);
        }
    }

    /**
     * Returns {@code true} if neither {@link #colorPicker} or {@link #colorRampChooser} has the focus.
     * This is used for assertions.
     */
    @Override
    protected boolean controlNotFocused() {
        return super.controlNotFocused() && (colorRampChooser == null || !colorRampChooser.isFocused());
    }

    /**
     * Cell for a color ramp in a {@link ComboBox}.
     */
    private final class Ramp extends ListCell<CategoryColors> {
        /** Creates a new cell. */
        Ramp() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setMaxWidth(Double.POSITIVE_INFINITY);
        }

        /** Sets the colors to show in the combo box item. */
        @Override
        protected void updateItem(final CategoryColors colors, final boolean empty) {
            super.updateItem(colors, empty);
            if (colors == null) {
                setGraphic(null);
            } else {
                Rectangle r = (Rectangle) getGraphic();
                if (r == null) {
                    r = createRectangle(-40);
                    setGraphic(r);
                }
                r.setFill(colors.paint());
            }
        }
    }

    /**
     * Invoked when users confirmed that (s)he wants to use the selected colors.
     *
     * @param  colors  the colors to use.
     */
    @Override
    public void commitEdit(final CategoryColors colors) {
        super.commitEdit(colors);
        styling.setARGB(getTableRow().getItem(), colors.colors);
    }

    /**
     * Creates a table of categories.
     *
     * @param  styling     function that determines which colors to apply on a given category.
     * @param  vocabulary  resources for the locale in use.
     */
    static TableView<Category> createTable(final CoverageStyling styling, final Vocabulary vocabulary) {
        final TableColumn<Category,String> name = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Name));
        name.setCellValueFactory(CategoryColorsCell::getCategoryName);
        name.setCellFactory(CategoryColorsCell::createNameCell);
        name.setEditable(false);
        name.setId("name");

        final TableColumn<Category,CategoryColors> colors = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Colors));
        colors.setCellValueFactory(styling);
        colors.setCellFactory((column) -> new CategoryColorsCell(styling));
        colors.setId("colors");
        colors.setSortable(false);

        final TableView<Category> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().setAll(name, colors);
        table.setEditable(true);
        return table;
    }

    /**
     * Invoked for creating a cell for the "name" column.
     * Returns the JavaFX default cell except for vertical alignment, which is centered.
     */
    private static TableCell<Category,String> createNameCell(final TableColumn<Category,String> column) {
        @SuppressWarnings("unchecked")
        final TableCell<Category,String> cell =
                (TableCell<Category,String>) TableColumn.DEFAULT_CELL_FACTORY.call(column);
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    /**
     * Invoked when the table needs to render a text in the "Name" column of the category table.
     */
    private static ObservableValue<String> getCategoryName(final TableColumn.CellDataFeatures<Category,String> cell) {
        final InternationalString name = cell.getValue().getName();
        return (name != null) ? new ReadOnlyObjectWrapper<>(name.toString()) : null;
    }
}
