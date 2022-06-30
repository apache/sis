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

import javafx.util.Callback;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.beans.value.ObservableValue;
import org.apache.sis.internal.gui.ImmutableObjectProperty;


/**
 * Builds and configures a {@link TableColumn} for colors. The {@link TableView} owner may be a table of
 * coverage categories or a table of isolines among others. {@code ColorColumnHandler} does conversions
 * between row data and {@link ColorRamp} items. There is typically a single {@code ColorColumnHandler}
 * instance shared by all {@link ColorCell} in the same column of a table.
 *
 * <p>The interfaces implemented by this class are implementation convenience
 * that may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @param  <S>  the type of row data as declared in the {@link TableView} generic type.
 *
 * @since 1.1
 * @module
 */
public abstract class ColorColumnHandler<S> implements Callback<TableColumn.CellDataFeatures<S,ColorRamp>, ObservableValue<ColorRamp>> {
    /**
     * Builds a new color table handler.
     */
    protected ColorColumnHandler() {
    }

    /**
     * Sets the color(s) associated to the given row item and returns the color type (solid or gradient).
     * The color type does not necessarily depend on the given {@code ColorRamp}; it may depend on the row
     * item instead, at implementation choice. The type determines which control (color picker, combo box,
     * <i>etc.</i>) will be shown if user wants to edit the color.
     *
     * @param  row     the row to update.
     * @param  colors  the new color(s) to assign to the given row item. May be {@code null}.
     * @return the type of color (solid or gradient) shown for the given value.
     */
    protected abstract ColorRamp.Type applyColors(S row, ColorRamp colors);

    /**
     * Gets the ARGB codes of colors to show in the cell for the given row data.
     * This method is sufficient when the color(s) can be changed only by calls to
     * {@link #applyColors(S, ColorRamp)}. If the color(s) may change externally,
     * then {@link #getObservableValue(S)} should be overridden too.
     *
     * @param  row  the row item for which to get ARGB codes to show in color cell.
     * @return the colors as ARGB codes, or {@code null} if none (transparent).
     */
    protected abstract int[] getARGB(S row);

    /**
     * Returns the color associated to given row as an observable value. The default implementation creates
     * an unmodifiable value derived from {@link #getARGB(S)}. It is okay if the color(s) can not be changed
     * in other way than by calls to {@link #applyColors(Object, ColorRamp)}. If this assumption does not hold,
     * then subclasses should override this method and return the observable which is mutated when the value change.
     *
     * @param  row  the row item for which to get color to show in color cell. Never {@code null}.
     * @return the color(s) to use for the given row, or {@code null} if none (transparent).
     */
    protected ObservableValue<ColorRamp> getObservableValue(S row) {
        final int[] ARGB = getARGB(row);
        if (ARGB != null) {
            return new ImmutableObjectProperty<>(new ColorRamp(ARGB));
        }
        return null;
    }

    /**
     * Invoked by {@link TableColumn} for computing the value of a {@link ColorCell}.
     * This method is public as an implementation side-effect; do not rely on that.
     *
     * @param  cell  the row value together with references to column and table where the show the color cell.
     * @return the color cell value, or {@code null} if none (transparent).
     */
    @Override
    public final ObservableValue<ColorRamp> call(final TableColumn.CellDataFeatures<S,ColorRamp> cell) {
        final S value = cell.getValue();
        return (value != null) ? getObservableValue(value) : null;
    }

    /**
     * Adds a colors column to the specified table.
     *
     * @param  table   the table where to add a colors column.
     * @param  header  column title to show in header row. This is typically "Color" or "Colors".
     *                 The use of singular or plural depends on whether the column allows color gradients.
     */
    protected final void addColumnTo(final TableView<S> table, final String header) {
        final TableColumn<S,ColorRamp> colors = new TableColumn<>(header);
        colors.setCellFactory((column) -> new ColorCell<S>(this));
        colors.setCellValueFactory(this);
        colors.setSortable(false);
        colors.setId("colors");
        /*
         * Handlers are invoked during the phase when events are propagated from target to root (in contrast
         * to filters which are invoked in a sooner phase when events are propagated in opposite direction).
         * By registering a handler, we intercept (consume) the event and prevent `TableView` to handle it.
         * This is necessary for avoiding `NullPointerException` observed in our experiments. That exception
         * occurred in JavaFX code that we do not control. Note: we tried to register filter directly on the
         * cell, but it didn't prevented the `NullPointerException`.
         */
        table.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                final TablePosition<?,?> focused = table.getFocusModel().getFocusedCell();
                if (focused != null) {
                    final TableColumn<?,?> column = focused.getTableColumn();
                    if (column == null || column == colors) {
                        event.consume();
                        final TablePosition<?,?> editing = table.getEditingCell();
                        final int row = (editing != null ? editing : focused).getRow();
                        if (row >= 0) {
                            table.edit(row, colors);
                        }
                    }
                }
            }
        });
        table.getColumns().add(colors);
    }
}
