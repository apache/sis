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

import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import javafx.util.Callback;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import org.apache.sis.gui.internal.AlignedTableCell;
import org.apache.sis.gui.internal.ImmutableObjectProperty;


/**
 * A table cell factory for values formatted using {@code java.text.Format}.
 * Each table row shall be a {@link List} of objects recognized by the format.
 * Values are presumed immutable. Instances of this class should be discarded
 * when the table content change, because it retains references to old values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of value in each row of the table.
 */
public final class FormatTableCellFactory<R> implements Function<Object, ObservableValue<String>>,
        Callback<TableColumn.CellDataFeatures<R, String>, ObservableValue<String>>
{
    /**
     * Creates columns.
     *
     * @param  <R>      the type of values in the column.
     * @param  header   header of each column. The length of this array will be the number of columns.
     * @param  formats  a supplier of formats for given column indexes.
     * @return columns to put in the table.
     */
    public static <R> TableColumn<R, String>[] createColumns(final String[] header, final IntFunction<Format> formats) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final TableColumn<R, String>[] columns = new TableColumn[header.length];
        final HashMap<Format, Format> previous = HashMap.newHashMap(columns.length);
        for (int i = 0; i < columns.length; i++) {
            Format format = formats.apply(i);
            Format shared = previous.putIfAbsent(format, format);
            if (shared != null) {
                format = shared;    // We can often use the same format instance for many columns.
            }
            final var column = new TableColumn<R, String>(header[i]);
            column.setCellValueFactory(new FormatTableCellFactory<>(i, format));
            column.setCellFactory(AlignedTableCell.baselineRight());
            column.setMinWidth(60);
            columns[i] = column;
        }
        return columns;
    }

    /**
     * The column index.
     */
    private final int index;

    /**
     * The format to use for formatting all values in this column.
     */
    private final Format format;

    /**
     * String representations of all formatted values.
     */
    private final Map<Object, ObservableValue<String>> values;

    /**
     * Creates a new factory which will format values using the given format.
     */
    private FormatTableCellFactory(final int index, final Format format) {
        this.index  = index;
        this.format = format;
        this.values = new HashMap<>();
    }

    /**
     * Invoked when a new string representation of a cell is requested.
     *
     * @param  cell  the table cell to format.
     * @return string representation of the cell value.
     */
    @Override
    public ObservableValue<String> call(final TableColumn.CellDataFeatures<R, String> cell) {
        return values.computeIfAbsent((cell.getValue() instanceof List<?> row) ? row.get(index) : null, this);
    }

    /**
     * Invoked when a new string representation of a value is requested.
     *
     * @param  value  the value to format.
     * @return string representation of the value.
     */
    @Override
    public ObservableValue<String> apply(final Object value) {
        return new ImmutableObjectProperty<>(value == null ? null : format.format(value));
    }
}
