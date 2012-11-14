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
package org.apache.sis.util.collection;

import java.util.List;


/**
 * The root of a tree of nodes, together with the definition of table columns.
 * The {@link #getColumns()} method gives the list of all columns that can be found
 * in a {@code TreeTable}. Usually some or all of those columns are also available as
 * {@link TableColumn} constants defined close to the method creating the tree tables.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface TreeTable {
    /**
     * Returns the table columns, in the order they shall be rendered by default.
     * This method returns the union of all table columns in every nodes of this
     * tree. However any {@link Node} instance can return {@code null} for a
     * particular column if the node doesn't have that column.
     *
     * @return The union of all table columns in every tree node.
     *
     * @see Node#getValue(TableColumn)
     */
    List<TableColumn<?>> getColumns();

    /**
     * Returns the root node of the tree.
     *
     * @return The root node of the tree.
     */
    Node getRoot();

    /**
     * A node in a tree combined with a row in a table. A {@code TreeTable.Node} can be seen as a
     * tree node associated to a single {@linkplain #getUserObject() user object} (like ordinary trees),
     * augmented with the capability to describe some aspects of the user object in pre-defined columns.
     * The list of allowed columns is given by the {@link TreeTable#getColumns()} method.
     *
     * <p>The following table summarizes the tree-related and table-related methods:</p>
     *
     * <table class="sis">
     * <tr>
     *   <th>Tree-related methods</th>
     *   <th>Table-related methods</th>
     * </tr>
     * <tr><td><ul>
     *   <li>{@link #getParent()}</li>
     *   <li>{@link #getChildren()}</li>
     *   <li>{@link #getUserObject()}</li>
     * </ul></td>
     * <td><ul>
     *   <li>{@link #getValue(TableColumn)}</li>
     *   <li>{@link #setValue(TableColumn, Object)}</li>
     *   <li>{@link #isEditable(TableColumn)}</li>
     * </ul></td></tr>
     * </table>
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3 (derived from geotk-3.19)
     * @version 0.3
     * @module
     */
    public static interface Node {
        /**
         * Returns the parent node, or {@code null} if this node is the root of the tree.
         *
         * @return The parent, or {@code null} if none.
         * @category tree
         */
        Node getParent();

        /**
         * Returns the children of this node. The returned list may or may not be modifiable, at
         * implementation choice. If the list is modifiable, then it shall be <cite>live</cite>,
         * i.e. any modification to the returned list are reflected immediately in the tree.
         *
         * @return The children, or an empty list if none.
         * @category tree
         */
        List<Node> getChildren();

        /**
         * Returns the value in the given column, or {@code null} if none.
         *
         * @param  <T>    The base type of values in the given column.
         * @param  column Identifier of the column from which to get the value.
         * @return The value in the given column, or {@code null} if none.
         *
         * @see TreeTable#getColumns()
         * @category table
         */
        <T> T getValue(TableColumn<T> column);

        /**
         * Sets the value for the given column (optional operation).
         * The {@link #isEditable(TableColumn)} method can be invoked before this setter method
         * for determining if the given column is modifiable.
         *
         * @param  <T>    The base type of values in the given column.
         * @param  column Identifier of the column into which to set the value.
         * @param  value  The value to set.
         * @throws IllegalArgumentException If the given column is not a legal column for this node.
         * @throws UnsupportedOperationException If values in the given column can not be modified.
         *
         * @see TreeTable#getColumns()
         * @see #isEditable(TableColumn)
         * @category table
         */
        <T> void setValue(TableColumn<T> column, T value);

        /**
         * Determines whether the value in the specified column is editable. If the given
         * column is not a legal column for this {@code Node} instance, then this method
         * returns {@code false}.
         *
         * @param  column The column to query.
         * @return {@code true} if the given column is a legal column for this {@code Node}
         *         implementation and the corresponding value is editable, or {@code false}
         *         otherwise.
         * @category table
         */
        boolean isEditable(TableColumn<?> column);

        /**
         * Returns the user object associated to this node.
         * The user object is for information purpose only and does not appear in the rendered tree.
         * It is typically a Java object whose content is splitted into the various table columns.
         *
         * <blockquote><font size="-1"><b>Example:<.b>
         * If a {@code CityLocation} class is defined as a (<var>city name</var>, <var>latitude</var>,
         * <var>longitude</var>) tuple, then a {@code TreeTable.Node} could be defined to have
         * 3 columns for the above 3 tuple components, and the user object could be the original
         * {@code CityLocation} instance.</font></blockquote>
         *
         * @return Any object stored at this node by the user, or {@code null} if none.
         * @category tree
         */
        Object getUserObject();
    }
}
