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
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.beans.value.ObservableValue;
import org.apache.sis.util.resources.Vocabulary;
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
 * @version 1.1
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
     * @param  row      the row to update.
     * @param  newItem  the new color(s) to assign to the given row item. May be {@code null}.
     * @return the type of color (solid or gradient) shown for the given value.
     */
    protected abstract ColorRamp.Type applyColors(S row, ColorRamp newItem);

    /**
     * Gets the ARGB codes of colors to shown in the cell for the given row data.
     *
     * @param  row  the row item for which to get ARGB codes to show in color cell.
     * @return the colors as ARGB codes, or {@code null} if none (transparent).
     */
    protected abstract int[] getARGB(S row);

    /**
     * If a {@code Color} instance already exists for the given row, that instance. Otherwise {@code null}.
     * In that later case, a {@code Color} or {@code Paint} instance will be created from {@link #getARGB(S)}.
     * This method is only for avoiding to create new {@code Color} instances when it already exists.
     *
     * @param  row  the row item for which to get ARGB codes to show in color cell.
     * @return the color to use for the given row, if an instance already exists.
     */
    protected Color getColor(S row) {
        return null;
    }

    /**
     * Invoked by {@link TableColumn} for computing the value of a {@link ColorCell}.
     * This method is public as an implementation side-effect; do not rely on that.
     *
     * @param  cell  the row value together with references to column and table where the show the color cell.
     * @return the color cell value.
     */
    @Override
    public final ObservableValue<ColorRamp> call(final TableColumn.CellDataFeatures<S,ColorRamp> cell) {
        final S value = cell.getValue();
        if (value == null) {
            return null;
        }
        final ColorRamp ramp;
        final Color color = getColor(value);
        if (color != null) {
            ramp = new ColorRamp(color);
        } else {
            final int[] ARGB = getARGB(value);
            if (ARGB == null) {
                return null;
            }
            ramp = new ColorRamp(ARGB);
        }
        return new ImmutableObjectProperty<>(ramp);
    }

    /**
     * Adds a colors column to the specified table.
     * This method also modifies the table configuration.
     *
     * @param  table       the table where to add a colors column.
     * @param  vocabulary  localized resources, provided in argument because often already known by caller.
     */
    protected final void addColumnTo(final TableView<S> table, final Vocabulary vocabulary) {
        final TableColumn<S,ColorRamp> colors = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Colors));
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
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}
