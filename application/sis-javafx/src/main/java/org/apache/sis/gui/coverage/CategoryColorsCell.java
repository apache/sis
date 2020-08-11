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

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.GUIUtilities;
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
final class CategoryColorsCell extends TableCell<Category,CategoryColors> implements EventHandler<ActionEvent> {
    /**
     * Space (in pixels) to remove on right side of the rectangle representing colors.
     */
    private static final double WIDTH_ADJUST = -9;

    /**
     * Height (in pixels) of the rectangle representing colors.
     */
    private static final double HEIGHT = 16;

    /**
     * The function that determines which colors to apply on a given category.
     * This same instance is shared by all cells of the same category table.
     */
    private final CoverageStyling styling;

    /**
     * The control for selecting a single color, or {@code null} if not yet created.
     * This applies to qualitative categories.
     */
    private ColorPicker colorPicker;

    /**
     * The control for selecting a color ramp, or {@code null} if not yet created.
     * This applies to quantitative categories.
     *
     * @see Category#isQuantitative()
     */
    private ComboBox<CategoryColors> colorRampChooser;

    /**
     * The single color shown in the table. Created when first needed.
     */
    private Rectangle singleColor;

    /**
     * Colors to restore if user cancels an edit action.
     */
    private CategoryColors restoreOnCancel;

    /**
     * Creates a cell for the colors column.
     * This constructor is for {@link #createTable(CoverageStyling, Vocabulary)} usage only.
     */
    private CategoryColorsCell(final CoverageStyling styling) {
        this.styling = styling;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    /**
     * Invoked when the color in this cell changed. It may be because of user selection in the combo box,
     * or because this cell is now used for a new {@link Category} instance.
     *
     * <div class="note"><b>Implementation note:</b>
     * this method should not invoke {@link #setGraphic(Node)} if the current graphic is a {@link ComboBoxBase}
     * because this method may be invoked at any time, including during execution of {@link #startEdit()} or
     * {@link #commitEdit(Object)}. Adding or removing {@link ColorPicker} or {@link ComboBox} in this method
     * cause problems with focus system. In particular we must be sure to remove {@link ColorPicker} only after
     * it has lost focus.</div>
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void updateItem(final CategoryColors colors, final boolean empty) {
        super.updateItem(colors, empty);
        final Node control = getGraphic();
        if (colors != null) {
            if (control == null) {
                setColorView(colors);
            } else if (control instanceof Rectangle) {
                ((Rectangle) control).setFill(colors.paint());
            } else if (control instanceof ColorPicker) {
                ((ColorPicker) control).setValue(colors.firstColor());
            } else {
                // A ClassCastException here would be a bug in CategoryColorsCell editors management.
                colors.asSelectedItem(((ComboBox<CategoryColors>) control));
            }
        } else if (control instanceof Rectangle) {
            setGraphic(null);
        }
    }

    /**
     * Returns {@code true} if neither {@link #colorPicker} or {@link #colorRampChooser} has the focus.
     * This is used for assertions.
     */
    private boolean controlNotFocused() {
        return (colorPicker == null || !colorPicker.isFocused()) &&
                (colorRampChooser == null || !colorRampChooser.isFocused());
    }

    /**
     * Sets the color representation when no editing is under way. It is caller responsibility to ensure
     * that the current graphic is not a combo box, or that it is safe to remove that combo box from the
     * scene (i.e. that combo box does not have focus anymore).
     */
    private void setColorView(final CategoryColors colors) {
        assert controlNotFocused();
        Rectangle view = null;
        if (colors != null) {
            final Paint paint = colors.paint();
            if (paint != null) {
                if (singleColor == null) {
                    singleColor = createRectangle(WIDTH_ADJUST);
                }
                view = singleColor;
                view.setFill(paint);
            }
        }
        setGraphic(view);
    }

    /**
     * Creates the graphic to draw in a table cell or combo box cell for representing a color or color ramp.
     *
     * @param  adjust  amount of space (in pixels) to add or remove on the right size.
     *                 Should be a negative number for removing space.
     */
    private Rectangle createRectangle(final double adjust) {
        final Rectangle gr = new Rectangle();
        gr.setHeight(HEIGHT);
        gr.widthProperty().bind(widthProperty().add(adjust));
        return gr;
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
     * Transitions from non-editing state to editing state. This method is automatically invoked when a
     * row is selected and the user clicks on the color cell in that row. This method sets the combo box
     * as the graphic element in that cell and shows it immediately. The immediate {@code control.show()}
     * is for avoiding to force users to perform a third mouse click.
     */
    @Override
    public void startEdit() {
        restoreOnCancel = getItem();
        final CategoryColors colors = (restoreOnCancel != null) ? restoreOnCancel : CategoryColors.GRAYSCALE;
        final ComboBoxBase<?> control;
        if (getTableRow().getItem().isQuantitative()) {
            if (colorRampChooser == null) {
                colorRampChooser = new ComboBox<>();
                colorRampChooser.setEditable(false);
                colorRampChooser.setCellFactory((column) -> new Ramp());
                colorRampChooser.getItems().setAll(CategoryColors.GRAYSCALE, CategoryColors.BELL);
                addListeners(colorRampChooser);
            }
            colors.asSelectedItem(colorRampChooser);
            control = colorRampChooser;
        } else {
            if (colorPicker == null) {
                colorPicker = new ColorPicker();
                addListeners(colorPicker);
            }
            colorPicker.setValue(colors.firstColor());
            control = colorPicker;
        }
        /*
         * Call `startEdit()` only after above call to `setValue(…)` because we want `isEditing()`
         * to return false during above value change. This is for preventing change listeners to
         * misinterpret the value change as a user selection.
         */
        super.startEdit();
        setGraphic(control);            // Must be before `requestFocus()`, otherwise focus request is ignored.
        control.requestFocus();         // Must be before `show()`, otherwise there is apparent focus confusion.
        control.show();
    }

    /**
     * Finishes configuration of a newly created combo box.
     */
    private void addListeners(final ComboBoxBase<?> control) {
        control.setOnAction(this);
        control.setOnHidden((e) -> hidden());
    }

    /**
     * Invoked when a combo box has been hidden. This method sets the focus to the table before to remove
     * the combo box. This is necessary for causing the combo box to lost focus, otherwise focus problems
     * appear next time that the combo box is shown.
     *
     * <p>IF the cell was in editing mode when this method is invoked, it means that the user clicked outside
     * the combo box area without validating his/her choice. In this case {@link #commitEdit(Object)} has not
     * been invoked and we need to either commit now or cancel. Current implementation cancels.</p>
     */
    private void hidden() {
        if (isEditing()) {
            if (isHover()) {
                return;                 // Keep editing state.
            }
            setItem(restoreOnCancel);
            super.cancelEdit();
        }
        restoreOnCancel = null;
        getTableView().requestFocus();  // Must be before `setGraphic(…)` for causing ColorPicker to lost focus.
        setColorView(getItem());
    }

    /**
     * Transitions from an editing state into a non-editing state without saving any user input.
     * This method is automatically invoked when the user click on another table row.
     */
    @Override
    public void cancelEdit() {
        setItem(restoreOnCancel);
        restoreOnCancel = null;
        super.cancelEdit();
        assert controlNotFocused();
        setColorView(getItem());
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
     * Invoked when the user selected a new value in the color picker or color ramp chooser.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handle(final ActionEvent event) {
        if (isEditing()) {
            final Object source = event.getSource();
            final CategoryColors value;
            if (source instanceof ColorPicker) {
                final Color color = ((ColorPicker) source).getValue();
                value = (color != null) ? new CategoryColors(GUIUtilities.toARGB(color)) : null;
            } else {
                // A ClassCastException here would be a bug in CategoryColorsCell editors management.
                value = ((ComboBox<CategoryColors>) source).getValue();
            }
            commitEdit(value);
        }
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
