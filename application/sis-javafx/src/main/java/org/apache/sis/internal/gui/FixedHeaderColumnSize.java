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
package org.apache.sis.internal.gui;

import java.util.List;
import javafx.util.Callback;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;


/**
 * A column resize policy where the size of the first column stay unchanged during table resize event.
 * But the column size can still be changed by moving the column separator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class FixedHeaderColumnSize<E> implements Callback<TableView.ResizeFeatures<E>, Boolean> {
    /**
     * The singleton instance.
     */
    @SuppressWarnings({"rawtypes","unchecked"})     // Partially unchecked for compatibility with JavaFX.
    public static final Callback<TableView.ResizeFeatures, Boolean> INSTANCE = new FixedHeaderColumnSize();

    /**
     * Creates the singleton instance.
     */
    private FixedHeaderColumnSize() {
    }

    /**
     * Returns an identifier for this policy.
     */
    @Override
    public String toString() {
        return "fixed-header-resize";
    }

    /**
     * Invoked by {@link TableView#resizeColumn(TableColumn, double)} when a column is resized.
     * This implementation temporarily freezes the size of the first column (in declaration order,
     * not necessarily in visual order) before to resize the other columns.
     *
     * @param  prop  the table and the column on which resizing is applied, together with size delta.
     * @return whether resizing is allowed.
     */
    @Override
    public Boolean call(final TableView.ResizeFeatures<E> prop) {
        if (prop.getColumn() == null) {
            final List<TableColumn<E,?>> columns = prop.getTable().getColumns();
            if (!columns.isEmpty()) {
                final TableColumn<E,?> column = columns.get(0);
                final boolean reducing = prop.getDelta() < 0;
                final double width = column.getWidth();
                final double save = reducing ? column.getMinWidth() : column.getMaxWidth();
                final Boolean result;
                try {
                    column.setMinWidth(width);
                    column.setMaxWidth(width);
                    result = TableView.CONSTRAINED_RESIZE_POLICY.call(prop);
                } finally {
                    if (reducing) {
                        column.setMinWidth(save);
                    } else {
                        column.setMaxWidth(save);
                    }
                }
                return result;
            }
        }
        return TableView.CONSTRAINED_RESIZE_POLICY.call(prop);
    }
}
