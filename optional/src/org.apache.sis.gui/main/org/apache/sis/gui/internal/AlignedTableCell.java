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
package org.apache.sis.gui.internal;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;


/**
 * A factory of table cell with some alignment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <S>  type of rows in the table.
 * @param  <V>  type of values in the table.
 */
public final class AlignedTableCell<S,T> implements Callback<TableColumn<S,T>, TableCell<S,T>> {
    /**
     * A factory of cells that are vertically centered.
     */
    private static final AlignedTableCell<?,?> CENTER_LEFT = new AlignedTableCell<>(Pos.CENTER_LEFT);

    /**
     * A factory of cells that are right-aligned.
     */
    private static final AlignedTableCell<?,?> BASELINE_RIGHT = new AlignedTableCell<>(Pos.BASELINE_RIGHT);

    /**
     * The desired alignment of text in the field.
     */
    private final Pos alignment;

    /**
     * Creates a new factory.
     *
     * @param  alignment  the desired alignment of text in the field
     */
    private AlignedTableCell(final Pos alignment) {
        this.alignment = alignment;
    }

    /**
     * Creates a new cell.
     *
     * @param  column  the column for which to create a cell.
     * @return cell factory for the given column.
     */
    @Override
    public TableCell<S,T> call(final TableColumn<S,T> column) {
        @SuppressWarnings("unchecked")
        final var cell = (TableCell<S,T>) TableColumn.DEFAULT_CELL_FACTORY.call(column);
        cell.setAlignment(alignment);
        return cell;
    }

    /**
     * Returns a factory of cells that are vertically centered.
     */
    @SuppressWarnings("unchecked")
    public static <S,T> AlignedTableCell<S,T> centerLeft() {
        return (AlignedTableCell<S,T>) CENTER_LEFT;
    }

    /**
     * Returns a factory of cells that are right-aligned.
     */
    @SuppressWarnings("unchecked")
    public static <S,T> AlignedTableCell<S,T> baselineRight() {
        return (AlignedTableCell<S,T>) BASELINE_RIGHT;
    }
}
