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

import java.util.Collection;
import java.util.List;


/**
 * Defines the structure (list of columns) of a table and provides the root of the tree
 * containing the data. {@code TreeTable} can be seen as a table in which the first
 * column contains a tree. Every row in this table is a {@link Node} instance, and each
 * node can have an arbitrary number of {@linkplain Node#getChildren() children} nodes.
 *
 * <p>Below is an example of what a two-columns {@code TreeTable} instance may look like
 * when {@linkplain TreeTableFormat formatted as a text}:</p>
 *
 * {@preformat text
 *   Citation
 *     ├─Title…………………………………………………………… Open Geospatial Consortium
 *     ├─Presentation Forms………………………… document digital
 *     ├─Cited Responsible Parties
 *     │   ├─Organisation Name………………… Open Geospatial Consortium
 *     │   ├─Role…………………………………………………… resource provider
 *     │   └─Contact Info
 *     │       └─Online Resource
 *     │           ├─Linkage……………………… http://www.opengeospatial.org/
 *     │           └─Function…………………… information
 *     └─Identifiers
 *         └─Code…………………………………………………… OGC
 * }
 *
 * <p>In many cases, the columns are known in advance as hard-coded static constants.
 * Those column constants are typically documented close to the class producing the
 * {@code TreeTable} instance. Using directly those static constants provides type
 * safety, as in the following example:</p>
 *
 * {@preformat java
 *     TreeTable table = ...; // Put here a TreeTable instance.
 *     TreeTable.Node node = table.getRoot();
 *     CharSequence   name = node.getValue(TableColumn.NAME);
 *     Class<?>       type = node.getValue(TableColumn.TYPE);
 * }
 *
 * In the above example, the type of value returned by the {@link Node#getValue(TableColumn)}
 * method is determined by the column constant. However this approach is possible only when
 * the table structure is known in advance. If a method needs to work with arbitrary tables,
 * then that method can get the list of columns by a call to {@link #getColumns()}. However
 * this column list does not provide the above type-safety.
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
     * tree. However any {@link Node} instance can return {@code null} for a
     * particular column if the node doesn't have that column.
     *
     * @return The union of all table columns in every tree node.
     *
     * @see Node#getValue(TableColumn)
     * @see Node#setValue(TableColumn, Object)
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
     * <caption>Tree-table methods</caption>
     * <tr>
     *   <th>Tree-related methods</th>
     *   <th>Table-related methods</th>
     * </tr>
     * <tr><td><ul>
     *   <li>{@link #getParent()}</li>
     *   <li>{@link #getChildren()}</li>
     *   <li>{@link #newChild()}</li>
     * </ul></td>
     * <td><ul>
     *   <li>{@link #getValue(TableColumn)}</li>
     *   <li>{@link #setValue(TableColumn, Object)}</li>
     *   <li>{@link #isEditable(TableColumn)}</li>
     * </ul></td></tr>
     * </table>
     *
     * In addition, each {@code Node} can be associated to an arbitrary object by the
     * {@link #getUserObject()} method. This object is not used directly by the tree tables.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    public static interface Node {
        /**
         * Returns the parent node, or {@code null} if this node is the root of the tree.
         *
         * <p>There is intentionally no {@code setParent(Node)} method, as children and parent managements
         * are highly implementation-dependant. If the {@linkplain #getChildren() children collection} is
         * modifiable, then implementations are encouraged to update automatically the parent when a child
         * is <em>added to</em> or <em>removed from</em> that collection.</p>
         *
         * @return The parent, or {@code null} if none.
         * @category tree
         */
        Node getParent();

        /**
         * Returns {@code true} if this node can not have any children. The {@linkplain #getChildren() children
         * collection} of a leaf node can only be empty, and adding {@linkplain #newChild() new child}
         * is an unsupported operation.
         *
         * <p>This value is provided as a tip for graphical user interfaces, in order to determine if
         * a node is expandable (even if empty). {@link TreeTableFormat} does not use this value.</p>
         *
         * @return {@code true} if this node can not have any children.
         */
        boolean isLeaf();

        /**
         * Returns the children of this node. The returned collection may or may not be modifiable, at
         * implementation choice. If the collection is modifiable, then it shall be <cite>live</cite>,
         * i.e. any modification to the returned collection are reflected immediately in the tree.
         * This allows addition or removal of child nodes as below:
         *
         * {@preformat java
         *     TreeTable.Node newNode = new ...; // Create a new node here.
         *     parent.getChildren().add(newNode);
         * }
         *
         * The collection is often a {@link List}, but not necessarily. For some implementations like the
         * {@linkplain org.apache.sis.metadata.AbstractMetadata#asTreeTable() metadata tree table view},
         * compliance to the {@code List} contract is impractical or inefficient.
         *
         * @return The children, or an empty collection if none.
         * @category tree
         */
        Collection<Node> getChildren();

        /**
         * Creates a new child with the same columns than the other children, and adds it to
         * the {@linkplain #getChildren() children collection}. The new child is typically added at
         * the end of the collection, but this is not mandatory: implementations can add the child
         * at whatever position they see fit.
         *
         * @return The new child.
         * @throws UnsupportedOperationException If this node can not add new children.
         */
        Node newChild() throws UnsupportedOperationException;

        /**
         * Returns the value in the given column, or {@code null} if none.
         *
         * @param  <V>    The base type of values in the given column.
         * @param  column Identifier of the column from which to get the value.
         * @return The value in the given column, or {@code null} if none.
         *
         * @see TreeTable#getColumns()
         * @category table
         */
        <V> V getValue(TableColumn<V> column);

        /**
         * Sets the value for the given column (optional operation).
         * The {@link #isEditable(TableColumn)} method can be invoked before this setter method
         * for determining if the given column is modifiable.
         *
         * @param  <V>    The base type of values in the given column.
         * @param  column Identifier of the column into which to set the value.
         * @param  value  The value to set.
         * @throws IllegalArgumentException If the given column is not a legal column for this node.
         * @throws UnsupportedOperationException If values in the given column can not be modified.
         *
         * @see TreeTable#getColumns()
         * @see #isEditable(TableColumn)
         * @category table
         */
        <V> void setValue(TableColumn<V> column, V value) throws IllegalArgumentException, UnsupportedOperationException;

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
         * <div class="note"><b>Example:</b>
         * If a {@code CityLocation} class is defined as a (<var>city name</var>, <var>latitude</var>,
         * <var>longitude</var>) tuple, then a {@code TreeTable.Node} could be defined to have 3 columns for the
         * above 3 tuple components, and the user object could be the original {@code CityLocation} instance.</div>
         *
         * @return Any object stored at this node by the user, or {@code null} if none.
         * @category tree
         */
        Object getUserObject();
    }
}
