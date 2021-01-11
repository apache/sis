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
        final Number obj = event.getNewValue();
        final double value = (obj != null) ? obj.doubleValue() : Double.NaN;
        level.value.set(value);
        level.visible.set(true);
        /*
         * Search for index where to move the row in order to keep ascending value order.
         * The algorithm below is okay if the new position is close to current position.
         * A binary search would be more efficient in the general case, but it may not be
         * worth the additional complexity. We do not use `items.sort(…)` because we want
         * to move only one row and its new position will determine the default color.
         */
        final TableView<IsolineLevel> table = event.getTableView();
        final ObservableList<IsolineLevel> items = table.getItems();
        final int row = event.getTablePosition().getRow();
        int dst = row;
        while (--dst >= 0) {
            // Use `!` for stopping if `value` is NaN.
            if (!(items.get(dst).value.get() >= value)) break;
        }
        final int size = items.size() - 1;                  // Excluding insertion row.
        while (++dst < size) {
            // No `!` for continuing until the end if `value` is NaN.
            if (dst != row && items.get(dst).value.get() >= value) break;
        }
        if (dst != row) {
            if (dst >= row) dst--;
            items.add(dst, items.remove(row));
            table.getSelectionModel().select(dst);
        }
        if (row >= size) {
            // If the edited line was the insertion row, add a new insertion row.
            items.add(new IsolineLevel());
        }
        /*
         * If the row has no color (should be the case only for insertion row), interpolate a default color
         * using the isolines before and after the new row. If we are at the beginning or end of the list,
         * then interpolation will actually be an extrapolation.
         */
        if (level.color.get() == null) {
            Color color = Color.BLACK;
            final int last = items.size() - 2;      // -1 for excluding insertion row, -1 again for last item.
            if (last >= 2) {                        // Need 3 items: the new row + 2 items for interpolation.
                int ilo = dst - 1;
                int iup = dst + 1;
                if (ilo < 0) {                       // (row index) == 0
                    ilo = 1;
                    iup = 2;
                } else if (iup > last) {             // (row index) == last
                    iup = last - 1;
                    ilo = last - 2;
                }
                final IsolineLevel lo = items.get(ilo);
                final IsolineLevel up = items.get(iup);
                final double base = lo.value.get();
                final double f = (value - base) / (up.value.get() - base);
                final Color clo = lo.color.get().color();
                final Color cup = up.color.get().color();
                color = new Color(interpolate(f, clo.getRed(),     cup.getRed()),
                                  interpolate(f, clo.getGreen(),   cup.getGreen()),
                                  interpolate(f, clo.getBlue(),    cup.getBlue()),
                                  interpolate(f, clo.getOpacity(), cup.getOpacity()));
            }
            level.color.set(new ColorRamp(color));
        }
    }

    /**
     * Interpolates or extrapolates a color component. Note: JavaFX provides a
     * {@code Color.interpolate(…)} method, but it does not perform extrapolations.
     *
     * @param  f   factor between 0 and 1 for interpolations, outside that range for extrapolations.
     * @param  lo  color component at f = 0.
     * @param  up  color component at f = 1.
     * @return interpolated or extrapolated color component.
     *
     * @see Color#interpolate(Color, double)
     */
    private static double interpolate(final double f, final double lo, final double up) {
        return Math.max(0, Math.min(1, (up - lo) * f + lo));
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
         * Header text is 🖉 (lower left pencil).
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
        final FormatTableCell.Trigger<IsolineLevel> trigger = new FormatTableCell.Trigger<>(level, format);
        level.setCellFactory((column) -> new FormatTableCell<>(Number.class, format, trigger));
        level.setCellValueFactory((cell) -> cell.getValue().value);
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
         * Add an empty row that user can edit for adding new data. This row will automatically enter in edition state
         * when a digit is typed (this is the purpose of `trigger`). For making easier to edit the cell in current row,
         * a listener on F2 key (same as Excel and OpenOffice) is also registered.
         */
        table.getItems().add(new IsolineLevel());
        trigger.registerTo(table);
        return table;
    }
}
