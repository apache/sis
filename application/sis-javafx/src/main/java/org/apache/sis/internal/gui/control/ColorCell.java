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
package org.apache.sis.internal.gui.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;


/**
 * Cell representing the color of an object.
 * The color can be modified by selecting the table row, then clicking on the color.
 * Subclasses should override the following methods:
 *
 * <ul>
 *   <li>{@link #getDefaultItem()}</li>
 *   <li>{@link #createItemForSelection(Object)}</li>
 *   <li>{@link #getControlForEdit(T)} (unless showing only solid colors)</li>
 *   <li>{@link #controlNotFocused()} (unless showing only solid colors)</li>
 *   <li>{@link #commitEdit(Object)}</li>
 * </ul>
 *
 * All methods are invoked in JavaFX thread.
 *
 * <p>The interfaces implemented by this class are implementation convenience
 * that may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <S>  the type of the {@code TableView} generic type.
 * @param  <T>  the type of the item contained within the cell.
 *
 * @since 1.1
 * @module
 */
public abstract class ColorCell<S,T extends ColorCell.Item> extends TableCell<S,T> implements EventHandler<ActionEvent> {
    /**
     * Gradient paint, colors or string representation of the rectangle to show in {@link ColorCell}.
     * This is the object stored in {@link TableCell} after conversion from user value of type {@code <S>}.
     *
     * @see TableCell#getItem()
     */
    public abstract static class Item {
        /**
         * Returns the paint to use for filling a rectangle in {@link ColorCell}, or {@code null} if none.
         * The default implementation returns the solid {@linkplain #color() color} (no gradient).
         *
         * @return color or gradient paint for table cell, or {@code null} if none.
         */
        protected Paint paint() {
            return color();
        }

        /**
         * Returns a solid color to use for filling a rectangle in {@link ColorCell}.
         * If this view has many colors (for example because it uses a gradient),
         * then some representative color or an arbitrary color should be returned.
         *
         * @return color for table cell, or {@code null} if none.
         */
        protected abstract Color color();

        /**
         * Updates a control with the current color of this item. Default implementation
         * recognizes {@link ColorPicker}. Subclasses should override if different kinds
         * of controls need to be handled.
         *
         * @param  control  the control to update.
         * @return whether the given control has been recognized.
         */
        protected boolean updateControl(final Node control) {
            if (control instanceof Rectangle) {
                ((Rectangle) control).setFill(paint());
            } else if (control instanceof ColorPicker) {
                ((ColorPicker) control).setValue(color());
            } else {
                return false;
            }
            return true;
        }
    }

    /**
     * Space (in pixels) to remove on right side of the rectangle showing colors.
     */
    private static final double WIDTH_ADJUST = -9;

    /**
     * Height (in pixels) of the rectangle showing colors.
     */
    private static final double HEIGHT = 16;

    /**
     * The control for selecting a single color, or {@code null} if not yet created.
     */
    private ColorPicker colorPicker;

    /**
     * The single color shown in a cell of the "color" column. Created when first needed.
     * User can modify the color of this rectangle with {@link #colorPicker} or other control
     * provided by {@link #getControlForEdit(T)}.
     */
    private Rectangle singleColor;

    /**
     * Colors to restore if user cancels an edit action.
     */
    private T restoreOnCancel;

    /**
     * Creates a new cell for the colors column.
     */
    protected ColorCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    /**
     * Returns the initial item for a new cell. This is invoked when the
     * user wants to edit a cell but {@link #getItem()} is still null.
     *
     * @return initial color or paint for a new cell.
     */
    protected abstract T getDefaultItem();

    /**
     * Creates an item for a new color selected using {@link ColorPicker} or other chooser.
     * The given object may be an instance of one of the following classes:
     *
     * <ul>
     *   <li>{@link Color} if the chooser was {@link ColorPicker}.</li>
     *   <li>{@code <T>} if the chooser was {@code ComboBox<T>}.</li>
     *   <li>Any other kind of value depending on {@link #getControlForEdit(T)}.</li>
     * </ul>
     *
     * @param  value  the color or gradient paint selected by the user.
     * @return the item to store in this cell for the given color or gradient.
     */
    protected abstract T createItemForSelection(Object value);

    /**
     * Returns a color or gradient chooser initialized to the given item.
     * This is invoked when the user clicks on a cell for modifying the color value.
     * The default implementation returns a {@link ColorPicker}.
     *
     * @param  colors  the initial color or gradient to show.
     * @return the control to show to user.
     */
    protected ComboBoxBase<?> getControlForEdit(final T colors) {
        if (colorPicker == null) {
            colorPicker = new ColorPicker();
            addListeners(colorPicker);
        }
        colorPicker.setValue(colors.color());
        return colorPicker;
    }

    /**
     * Returns {@code true} if the {@link #colorPicker} does not have the focus.
     * This is used for assertions. Subclasses should override if there is more
     * controls that may have the focus.
     *
     * @return {@code true} if no control has the focus.
     */
    protected boolean controlNotFocused() {
        return (colorPicker == null) || !colorPicker.isFocused();
    }

    /**
     * Creates the graphic to draw in a table cell or combo box cell for representing a color or color ramp.
     * This method may be invoked by subclasses for building an editor in {@link #getControlForEdit(T)}.
     *
     * @param  adjust  amount of space (in pixels) to add on the right size.
     *                 Can be a negative number for removing space.
     * @return graphic to draw in a table cell or combo box cell.
     */
    protected final Rectangle createRectangle(final double adjust) {
        final Rectangle gr = new Rectangle();
        gr.setHeight(HEIGHT);
        gr.widthProperty().bind(widthProperty().add(adjust));
        return gr;
    }

    /**
     * Finishes configuration of a newly created combo box.
     * This method may be invoked by subclasses for building an editor in {@link #getControlForEdit(T)}.
     *
     * @param  control  the {@link ColorPicker} or other combo box on which to add listeners.
     */
    protected final void addListeners(final ComboBoxBase<?> control) {
        control.setOnAction(this);
        control.setOnHidden((e) -> hidden());
    }


    //
    // Methods below this line should not be called or overridden by subclasses.
    //


    /**
     * Invoked when the color in this cell changed. It may be because of user selection in a combo box,
     * or because this cell is now used for a new {@code <S>} instance.
     *
     * <div class="note"><b>Implementation note:</b>
     * this method should not invoke {@link #setGraphic(Node)} if the current graphic is a {@link ComboBoxBase}
     * (the parent of {@link ColorPicker}) because this method may be invoked at any time, including during the
     * execution of {@link #startEdit()} or {@link #commitEdit(Object)} methods.
     * Adding or removing {@link ComboBoxBase} in this method cause problems with focus system.
     * In particular we must be sure to remove {@link ColorPicker} only after it has lost focus.</div>
     *
     * @param  colors  the new object to show as a color or gradient in this cell.
     * @param  empty   {@code true} if this method is invoked for creating an empty cell.
     */
    @Override
    protected final void updateItem(final T colors, final boolean empty) {
        super.updateItem(colors, empty);
        final Node control = getGraphic();
        if (colors != null) {
            if (control == null) {
                setColorItem(colors);
            } else {
                colors.updateControl(control);
            }
        } else if (control instanceof Rectangle) {
            setGraphic(null);
        }
    }

    /**
     * Sets the color representation when no editing is under way. It is caller responsibility to ensure
     * that the current graphic is not a combo box, or that it is safe to remove that combo box from the
     * scene (i.e. that combo box does not have focus anymore).
     *
     * @param  colors  current value of {@link #getItem()}.
     */
    private void setColorItem(final T colors) {
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
     * Transitions from non-editing state to editing state. This method is automatically invoked when a
     * row is selected and the user clicks on the color cell in that row. This method sets the combo box
     * as the graphic element in that cell and shows it immediately. The immediate {@code control.show()}
     * is for avoiding to force users to perform a third mouse click.
     */
    @Override
    public final void startEdit() {
        restoreOnCancel = getItem();
        final T colors = (restoreOnCancel != null) ? restoreOnCancel : getDefaultItem();
        final ComboBoxBase<?> control = getControlForEdit(colors);
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
     * Transitions from an editing state into a non-editing state without saving any user input.
     * This method is automatically invoked when the user click on another table row.
     */
    @Override
    public final void cancelEdit() {
        setItem(restoreOnCancel);
        restoreOnCancel = null;
        super.cancelEdit();
        assert controlNotFocused();
        setColorItem(getItem());
    }

    /**
     * Invoked when a combo box has been hidden. This method sets the focus to the table before to remove
     * the combo box. This is necessary for causing the combo box to lost focus, otherwise focus problems
     * appear next time that the combo box is shown.
     *
     * <p>If the cell was in editing mode when this method is invoked, it means that the user clicked outside
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
        setColorItem(getItem());
    }

    /**
     * Invoked when the user selected a new value in the color picker or color ramp chooser.
     * This method is public as an implementation side-effect and should never be invoked directly.
     *
     * @param  event  the {@link ComboBoxBase} on which a selection occurred.
     */
    @Override
    public final void handle(final ActionEvent event) {
        if (isEditing()) {
            final Object source = event.getSource();
            final T value;
            if (source instanceof ComboBoxBase<?>) {
                value = createItemForSelection(((ComboBoxBase<?>) source).getValue());
            } else {
                value = restoreOnCancel;
            }
            commitEdit(value);
        }
    }
}
