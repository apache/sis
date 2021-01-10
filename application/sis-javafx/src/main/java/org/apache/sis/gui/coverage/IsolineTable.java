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

import java.text.NumberFormat;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.apache.sis.internal.gui.control.FormatTableCell;
import org.apache.sis.internal.gui.control.ColorColumnHandler;
import org.apache.sis.internal.gui.control.ColorRamp;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Table of isolines (values and colors).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class IsolineTable extends ColorColumnHandler<IsolineLevel> {
    /**
     * The format to use for formatting isoline levels.
     * The same instance will be shared by all {@link FormatTableCell}s in this table.
     */
    private final NumberFormat format;

    /**
     * Creates a new handler.
     */
    IsolineTable() {
        format = NumberFormat.getInstance();
    }

    /**
     * Returns the colors to apply for the given isoline level, or {@code null} for transparent.
     * This method is defined for safety but should not be invoked; use {@link #getObservableValue(S)} instead.
     */
    @Override
    protected int[] getARGB(final IsolineLevel level) {
        final ColorRamp r = level.color.get();
        return (r != null) ? r.colors : null;
    }

    /**
     * Returns the color associated to given row as an observable value.
     *
     * @param  level  the isoline level for which to get color to show in color cell.
     * @return the color(s) to use for the given isoline, or {@code null} if none (transparent).
     */
    @Override
    protected ObservableValue<ColorRamp> getObservableValue(final IsolineLevel level) {
        return level.color;
    }

    /**
     * Invoked when users confirmed that (s)he wants to use the selected colors.
     *
     * @param  level   the isoline level for which to assign new color(s).
     * @param  colors  the new color for the given isoline, or {@code null} for resetting default value.
     * @return the type of color (always solid for this class).
     */
    @Override
    protected ColorRamp.Type applyColors(final IsolineLevel level, ColorRamp colors) {
        level.color.set(colors);
        return ColorRamp.Type.SOLID;
    }

    /**
     * Invoked when an isoline value has been edited. This method saves the value, checks the isoline
     * as visible and set a default color if no color was already set.
     */
    private static void commitEdit(final TableColumn.CellEditEvent<IsolineLevel,Number> event) {
        final IsolineLevel level = event.getRowValue();
        final Number value = event.getNewValue();
        level.value.set(value != null ? value.doubleValue() : Double.NaN);
        if (level.color.get() == null) {
            level.color.set(new ColorRamp(Color.BLACK));
        }
        level.visible.set(true);
        /*
         * If the edited line was the insertion row, we need to add a new insertion row.
         */
        final ObservableList<IsolineLevel> items = event.getTableView().getItems();
        final int row = event.getTablePosition().getRow();
        if (row >= items.size() - 1) {
            items.add(new IsolineLevel());
        }
    }

    /**
     * Creates a table showing the color of isoline levels.
     * The color can be modified by selecting the table row, then clicking on the color.
     *
     * @param  vocabulary  localized resources, given because already known by the caller
     *                     (this argument would be removed if this method was public API).
     */
    final TableView<IsolineLevel> createIsolineTable(final Vocabulary vocabulary) {
        /*
         * First column containing a checkbox for choosing whether the isoline should be drawn or not.
         */
        final TableColumn<IsolineLevel,Boolean> visible = new TableColumn<>("\uD83D\uDD89");
        visible.setCellFactory(CheckBoxTableCell.forTableColumn(visible));
        visible.setCellValueFactory((cell) -> cell.getValue().visible);
        visible.setSortable(false);
        visible.setResizable(false);
        visible.setMinWidth(Styles.CHECKBOX_WIDTH);
        visible.setMaxWidth(Styles.CHECKBOX_WIDTH);
        /*
         * Second column containing the level value.
         * The number can be edited using a `NumberFormat` in current locale.
         */
        final TableColumn<IsolineLevel,Number> level = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Level));
        level.setCellValueFactory((cell) -> cell.getValue().value);
        level.setCellFactory((column) -> new FormatTableCell<>(Number.class, format));
        level.setOnEditCommit(IsolineTable::commitEdit);
        level.setSortable(false);                           // We will do our own sorting.
        level.setId("level");
        /*
         * Create the table with above "category name" column (read-only),
         * and add an editable column for color(s).
         */
        final TableView<IsolineLevel> table = new TableView<>();
        table.getColumns().setAll(visible, level);
        addColumnTo(table, vocabulary);
        /*
         * Add an empty row that user can edit for adding new data.
         */
        table.getItems().add(new IsolineLevel());
        return table;
    }
}
