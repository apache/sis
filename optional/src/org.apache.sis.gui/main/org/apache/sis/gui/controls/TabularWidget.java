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

import javafx.util.Callback;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import org.apache.sis.gui.Widget;


/**
 * Base class of widgets providing a table of something.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class TabularWidget extends Widget {
    /**
     * Width of a checkbox or radio item in a table cell.
     */
    private static final int CHECKBOX_WIDTH = 40;

    /**
     * Creates a new widget.
     */
    TabularWidget() {
    }

    /**
     * Creates an initially empty table.
     *
     * @param  <S>  the type of objects contained within the {@link TableView} items list.
     * @return the initially empty table.
     */
    static <S> TableView<S> newTable() {
        TableView<S> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);
        return table;
    }

    /**
     * Creates a new column for a Boolean value represented by a checkbox.
     *
     * @param  <S>      the type of objects contained within the {@link TableView} items list.
     * @param  header   column header.
     * @param  factory  link to the Boolean property.
     * @return a column for checkbox.
     */
    static <S> TableColumn<S,Boolean> newBooleanColumn(final String header,
            final Callback<CellDataFeatures<S,Boolean>, ObservableValue<Boolean>> factory)
    {
        final var c = new TableColumn<S,Boolean>(header);
        c.setCellFactory(CheckBoxTableCell.forTableColumn(c));
        c.setCellValueFactory(factory);
        c.setSortable(false);
        c.setResizable(false);
        c.setMinWidth(CHECKBOX_WIDTH);
        c.setMaxWidth(CHECKBOX_WIDTH);
        return c;
    }

    /**
     * Creates a new column for a string value in a text field.
     *
     * @param  <S>      the type of objects contained within the {@link TableView} items list.
     * @param  header   column header.
     * @param  factory  link to the string property.
     * @return a column for text field.
     */
    static <S> TableColumn<S,String> newStringColumn(final String header,
            final Callback<CellDataFeatures<S,String>, ObservableValue<String>> factory)
    {
        final var c = new TableColumn<S,String>(header);
        c.setCellFactory(TextFieldTableCell.forTableColumn());
        c.setCellValueFactory(factory);
        return c;
    }
}
