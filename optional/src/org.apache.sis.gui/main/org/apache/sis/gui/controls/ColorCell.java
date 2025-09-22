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

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;


/**
 * Cell showing the color of an object (category, isoline, <i>etc</i>).
 * The color can be modified by selecting the table row, then clicking on the color.
 * The conversion between {@link ColorRamp} and the {@code <S>} object for row data
 * is handled by a {@link ColorColumnHandler}. The same handler may be shared by all
 * {@code ColorCell}s in a table. All methods are invoked in JavaFX thread.
 *
 * <p>The interfaces implemented by this class are implementation convenience that may change in any future version.
 * {@link EventHandler} is for reacting to user color selection using the control shown.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <S>  the type of row data as declared in the {@code TableView} generic type.
 */
final class ColorCell<S> extends TableCell<S,ColorRamp> implements EventHandler<ActionEvent> {
    /**
     * Space (in pixels) to remove on right side of the rectangle showing colors.
     */
    private static final double WIDTH_ADJUST = -9;

    /**
     * Height (in pixels) of the rectangle showing colors.
     */
    private static final double HEIGHT = 16;

    /**
     * The converter between row data and the item to store in this {@code ColorCell}.
     * There is typically only one instance for the whole color column.
     */
    private final ColorColumnHandler<S> handler;

    /**
     * The type of color ramp as determined by {@link ColorColumnHandler#applyColors(Object, ColorRamp)}.
     * This is updated by {@link #updateItem(ColorRamp, boolean)} when the value changes, and is stored
     * for keeping that value stable (this class does not support mutable colors type).
     * May be {@code null} if there are no values in the row of this cell.
     */
    private ColorRamp.Type type;

    /**
     * The control for selecting a single color, or {@code null} if not yet created.
     */
    private ColorPicker colorPicker;

    /**
     * The control for selecting a color ramp, or {@code null} if not yet created.
     */
    private ComboBox<ColorRamp> colorRampChooser;

    /**
     * The color(s) shown in this cell when no control is shown, or {@code null} if not yet created.
     * User can modify the color of this rectangle with {@link #colorPicker} or {@link #colorRampChooser}.
     */
    private Rectangle colorView;

    /**
     * Creates a new cell for the colors column.
     *
     * @param  handler  the converter between row data and the item to store in this {@code ColorCell}.
     */
    ColorCell(final ColorColumnHandler<S> handler) {
        this.handler = handler;
        setOnMouseClicked(ColorCell::mouseClicked);
    }

    /**
     * Invoked when user clicked on the cell. If the cell was not already in editing state, this method
     * transitions to editing state. It has the effect of showing the color picker or color ramp chooser.
     */
    private static void mouseClicked(final MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (((ColorCell<?>) event.getSource()).requestEdit()) {
                event.consume();
            }
        }
    }

    /**
     * Returns {@code true} if neither {@link #colorPicker} or {@link #colorRampChooser} has the focus.
     * This is used for assertions: we should not add or remove (by calls to {@link #setGraphic(Node)})
     * one of those {@link ComboBoxBase}s before it has lost focus, otherwise it causes problems with
     * the focus system.
     *
     * @return {@code true} if no control has the focus.
     */
    private boolean controlNotFocused() {
        return (colorPicker == null || !colorPicker.isFocused()) &&
                (colorRampChooser == null || !colorRampChooser.isFocused());
    }

    /**
     * Shows the control button (not the popup window) for choosing color(s) in this cell. The control type will be
     * {@link ColorPicker} or {@link ComboBox} depending on {@link ColorColumnHandler#applyColors(Object, ColorRamp)}.
     * This method is invoked when edition started and does nothing if the button is already visible in the cell.
     *
     * @return the control shown, or {@code null} if none.
     *
     * @see #hideControlButton()
     */
    private ComboBoxBase<?> showControlButton() {
        final Node current = getGraphic();
        if (current instanceof ComboBoxBase<?>) {
            return (ComboBoxBase<?>) current;
        }
        assert controlNotFocused();
        final ComboBoxBase<?> control;
        if (type == null) {
            control = null;
        } else {
            final boolean isNewControl;
            switch (type) {
                default: throw new AssertionError(type);
                case SOLID: {
                    if (isNewControl = (colorPicker == null)) {
                        colorPicker = new ColorPicker();
                        colorPicker.setMaxWidth(Double.MAX_VALUE);      // Take all the width inside the cell.
                        updateColorPicker(getItem());
                    }
                    control = colorPicker;
                    break;
                }
                case GRADIENT: {
                    if (isNewControl = (colorRampChooser == null)) {
                        colorRampChooser = new ComboBox<>();
                        colorRampChooser.setEditable(false);
                        colorRampChooser.setMaxWidth(Double.MAX_VALUE);
                        colorRampChooser.setCellFactory((column) -> new RampChoice());
                        colorRampChooser.getItems().setAll(ColorRamp.DEFAULTS);
                        updateColorRampChooser(getItem());
                    }
                    control = colorRampChooser;
                    break;
                }
            }
            /*
             * Add listeners only after the control got its initial value, for avoiding change event.
             * We do not need to update the value here after control creation because future updates
             * are handled by `updateItem(…)`.
             */
            if (isNewControl) {
                control.setOnAction(this);
            }
        }
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(control);
        return control;
    }

    /**
     * Cell for a color ramp in a list of choices shown by {@link ComboBox}.
     * This is used by {@link #showControlButton()} for building {@link #colorRampChooser}.
     */
    private final class RampChoice extends ListCell<ColorRamp> {
        /** Creates a new combo box choice. */
        RampChoice() {
            setMaxWidth(Double.POSITIVE_INFINITY);
        }

        /** Sets the colors to show in the combo box item. */
        @Override protected void updateItem(final ColorRamp colors, final boolean empty) {
            super.updateItem(colors, empty);
            if (empty || colors == null) {
                setText(null);
                setGraphic(null);
            } else if (colors.isTransparent()) {
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                setText(colors.toString());
                setGraphic(null);
            } else {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setText(null);
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
     * Creates the graphic to draw in a table cell or combo box cell for showing color(s).
     * This method is invoked for building an editor in {@link #showControlButton()} or for
     * rendering the {@link ColorRamp} in the table.
     *
     * @param  adjust  number of spaces (in pixels) to add on the right size.
     *                 Can be a negative number for removing space.
     * @return graphic to draw in a table cell or combo box cell.
     *
     * @see #setColorItem(ColorRamp)
     */
    private Rectangle createRectangle(final double adjust) {
        final var gr = new Rectangle();
        gr.setHeight(HEIGHT);
        gr.widthProperty().bind(widthProperty().add(adjust));
        return gr;
    }

    /**
     * Updates {@link #colorPicker} for the new item value.
     */
    private void updateColorPicker(final ColorRamp item) {
        colorPicker.setValue(item != null ? item.color() : null);
    }

    /**
     * Updates {@link #colorRampChooser} for the new item value. This method declares the given {@link ColorRamp}
     * as the selected item in the chooser. If the item is not found, then it is added to the chooser list.
     */
    private void updateColorRampChooser(final ColorRamp item) {
        if (item != null) {
            final ObservableList<ColorRamp> items = colorRampChooser.getItems();
            int i = items.indexOf(item);
            if (i < 0) {
                i = items.size();
                items.add(item);
            }
            colorRampChooser.getSelectionModel().select(i);
        } else {
            colorRampChooser.getSelectionModel().clearSelection();
        }
    }

    /**
     * Invoked when the color in this cell changed. It may be because of user selection in a combo box,
     * or because this cell is now used for a new {@code <S>} instance. This method is invoked when the
     * row value (of type {@code <S>}) is modified.
     *
     * <h4>Implementation note</h4>
     * This method should not invoke {@link #setGraphic(Node)} if the current graphic is a {@link ComboBoxBase}
     * (the parent of {@link ComboBox} and {@link ColorPicker}) because this method may be invoked at any time,
     * including during the execution of {@link #startEdit()} or {@link #commitEdit(Object)} methods.
     * Adding or removing {@link ComboBoxBase} in this method cause problems with focus system.
     * In particular we must be sure to remove {@link ColorPicker} only after it has lost focus.
     *
     * @param  colors  the new object to show as a color or gradient in this cell.
     * @param  empty   {@code true} if this method is invoked for creating an empty cell.
     */
    @Override
    protected final void updateItem(final ColorRamp colors, final boolean empty) {
        super.updateItem(colors, empty);
        /*
         * Associate the new colors to the row in a way determined by the `ColorColumnHandler` class.
         * Then get the new color type (solid or gradient) for the current row. Note that `TableRow`
         * may be null early in the `TableCell` lifecycle.
         */
        type = null;
        if (!empty) {
            final TableRow<S> row = getTableRow();
            if (row != null) {
                final S item = row.getItem();
                if (item != null) {
                    type = handler.applyColors(item, (colors != ColorRamp.DEFAULT) ? colors : null);
                }
            }
        }
        /*
         * Update the visual representation. Update also the control even if it is hidden, because
         * those updates should be less frequent than show/hide cycles. It avoids the need to update
         * the control every time that it is shown.
         */
        if (type != null) {
            switch (type) {
                default: throw new AssertionError(type);
                case SOLID: {
                    if (colorPicker != null) {
                        updateColorPicker(colors);
                    }
                    break;
                }
                case GRADIENT: {
                    if (colorRampChooser != null) {
                        updateColorRampChooser(colors);
                    }
                    break;
                }
            }
        }
        if (!(getGraphic() instanceof Control)) {
            setColorItem(colors);
        }
    }

    /**
     * Sets the color representation when no editing is under way. It is caller responsibility to ensure
     * that the current graphic is not a combo box, or that it is safe to remove that combo box from the
     * scene (i.e. that combo box does not have focus anymore).
     *
     * @param  colors  current value of {@link #getItem()}.
     */
    private void setColorItem(final ColorRamp colors) {
        assert controlNotFocused();
        Rectangle view = null;
        String label = null;
        if (colors != null) {
            final Paint paint = colors.paint();
            if (paint != null) {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                if (colorView == null) {
                    colorView = createRectangle(WIDTH_ADJUST);
                }
                view = colorView;
                view.setFill(paint);
            } else {
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                label = colors.toString();
            }
        }
        setGraphic(view);
        setText(label);
    }

    /**
     * Removes the control in the cell and paints the color in a rectangle instead.
     * This method does nothing if the control is already hidden.
     *
     * <p>This method sets the focus to the table before to remove the combo box.
     * This is necessary for causing the combo box to lost focus, otherwise focus
     * problems appear next time that the combo box is shown.</p>
     *
     * @see #showControlButton()
     */
    private void hideControlButton() {
        final Node control = getGraphic();
        if (control instanceof Control) {
            if (control.isFocused()) {
                // Must be before `setGraphic(…)` for causing ColorPicker to lost focus.
                getTableView().requestFocus();
            }
            setColorItem(getItem());
        }
    }

    /**
     * Requests this cell to transition to editing state. The request is made on the {@link TableView},
     * which will invoke {@link #startEdit()}. The edition request must be done on {@code TableView} for
     * allowing the table to know the row and column indices of the cell being edited.
     *
     * @return {@code true} if this cell transitioned to editing state,
     *         or {@code false} if this cell was already in editing state.
     */
    private boolean requestEdit() {
        if (isEditing()) {
            popup(showControlButton());
            return false;
        } else {
            final int row = getTableRow().getIndex();
            final TableView<S> table = getTableView();
            table.getSelectionModel().select(row);
            table.edit(row, getTableColumn());
            // JavaFX will call `startEdit()`.
            return true;
        }
    }

    /**
     * Transitions from non-editing state to editing state. This method is automatically invoked when a
     * row is selected and the user clicks on the color cell in that row. This method sets the combo box
     * as the graphic element in that cell and shows it immediately. The immediate {@code control.show()}
     * is for avoiding to force users to perform a third mouse click.
     *
     * <p>This method should not be invoked directly. Invoke {@link #requestEdit()} instead.</p>
     */
    @Override
    public final void startEdit() {
        final ComboBoxBase<?> control = showControlButton();
        /*
         * Call `startEdit()` only after above call to `setValue(…)` because we want `isEditing()`
         * to return false during above value change. This is for preventing change listeners to
         * misinterpret the value change as a user selection.
         */
        super.startEdit();
        popup(control);
    }

    /**
     * Shows the popup window of the given control. This method does nothing if the control is null.
     */
    private static void popup(final ComboBoxBase<?> control) {
        if (control != null) {
            control.requestFocus();     // Must be before `show()`, otherwise there is apparent focus confusion.
            control.show();             // For requiring one less mouse click by user.
        }
    }

    /**
     * Transitions from an editing state into a non-editing state without saving any user input.
     * This method is automatically invoked when the user clicks on another table row.
     */
    @Override
    public final void cancelEdit() {
        super.cancelEdit();
        hideControlButton();
    }

    /**
     * Invoked when the user selected a new value in the color picker or color ramp chooser.
     * This handler creates an item for the new color(s). The selected value may be an instance
     * of one of the following classes:
     *
     * <ul>
     *   <li>{@link Color} if the chooser was {@link ColorPicker}.</li>
     *   <li>{@code ColorRamp} if the chooser was {@code ComboBox<ColorRamp>}.</li>
     * </ul>
     *
     * This method is public as an implementation side-effect and should never be invoked directly.
     *
     * @param  event  the {@link ComboBoxBase} on which a selection occurred.
     */
    @Override
    public final void handle(final ActionEvent event) {
        if (isEditing()) {
            final Object value = ((ComboBoxBase<?>) event.getSource()).getValue();
            final ColorRamp colors;
            if (value instanceof Color) {
                colors = new ColorRamp((Color) value);
            } else {
                // A ClassCastException here would be a bug in ColorCell editors management.
                colors = (ColorRamp) value;
            }
            commitEdit(colors);
        }
        hideControlButton();
    }
}
