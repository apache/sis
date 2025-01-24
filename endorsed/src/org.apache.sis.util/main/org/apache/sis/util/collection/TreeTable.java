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
 * <pre class="text">
 *   Citation
 *     ├─Title…………………………………………………………… Open Geospatial Consortium
 *     ├─Presentation Forms………………………… document digital
 *     ├─Cited Responsible Parties
 *     │   ├─Organisation Name………………… Open Geospatial Consortium
 *     │   ├─Role…………………………………………………… resource provider
 *     │   └─Contact Info
 *     │       └─Online Resource
 *     │           ├─Linkage……………………… https://www.ogc.org/
 *     │           └─Function…………………… information
 *     └─Identifiers
 *         └─Code…………………………………………………… OGC</pre>
 *
 * In many cases, the columns are known in advance as hard-coded static constants.
 * Those column constants are typically documented close to the class producing the
 * {@code TreeTable} instance. Using directly those static constants provides type
 * safety, as in the following example:
 *
 * {@snippet lang="java" :
 *     TreeTable table = ...;                   // Put here a TreeTable instance.
 *     TreeTable.Node node = table.getRoot();
 *     CharSequence   name = node.getValue(TableColumn.NAME);
 *     Class<?>       type = node.getValue(TableColumn.TYPE);
 *     }
 *
 * In the above example, the type of value returned by the {@link Node#getValue(TableColumn)}
 * method is determined by the column constant. However, this approach is possible only when
 * the table structure is known in advance. If a method needs to work with arbitrary tables,
 * then that method can get the list of columns by a call to {@link #getColumns()}. However
 * this column list does not provide the above type-safety.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 */
public interface TreeTable {
    /**
     * Returns the table columns, in the order they shall be rendered by default.
     * This method returns the union of all table columns in every nodes of this
     * tree. However, any {@link Node} instance can return {@code null} for a
     * particular column if the node doesn't have that column.
     *
     * @return the union of all table columns in every tree node.
     *
     * @see Node#getValue(TableColumn)
     * @see Node#setValue(TableColumn, Object)
     */
    List<TableColumn<?>> getColumns();

    /**
     * Returns the root node of the tree.
     *
     * @return the root node of the tree.
     */
    Node getRoot();

    /**
     * A node in a tree combined with a row in a table. A {@code TreeTable.Node} can be seen as a
     * tree node associated to a single {@linkplain #getUserObject() user object} (like ordinary trees),
     * augmented with the capability to describe some aspects of the user object in predefined columns.
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
     * <h2>Default implementation</h2>
     * The methods providing a default implementations are suitable for unmodifiable tree nodes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 1.5
     * @since   0.3
     */
    public interface Node {
        /**
         * Returns the parent node, or {@code null} if this node is the root of the tree.
         *
         * <p>There is intentionally no {@code setParent(Node)} method, as children and parent managements
         * are highly implementation-dependant. If the {@linkplain #getChildren() children collection} is
         * modifiable, then implementations are encouraged to update automatically the parent when a child
         * is <em>added to</em> or <em>removed from</em> that collection.</p>
         *
         * @return the parent, or {@code null} if none.
         * @category tree
         */
        Node getParent();

        /**
         * Returns {@code true} if this node cannot have any children. The {@linkplain #getChildren() children
         * collection} of a leaf node can only be empty, and adding {@linkplain #newChild() new child}
         * is an unsupported operation.
         *
         * <p>This value is provided as a tip for graphical user interfaces, in order to determine if
         * a node is expandable (even if empty). {@link TreeTableFormat} does not use this value.</p>
         *
         * @return {@code true} if this node cannot have any children.
         */
        boolean isLeaf();

        /**
         * Returns the children of this node. The returned collection may or may not be modifiable, at
         * implementation choice. If the collection is modifiable, then it shall be <em>live</em>,
         * i.e. any modification to the returned collection are reflected immediately in the tree.
         * This allows addition or removal of child nodes as below:
         *
         * {@snippet lang="java" :
         *     TreeTable.Node newNode = new ...; // Create a new node here.
         *     parent.getChildren().add(newNode);
         *     }
         *
         * The collection is often a {@link List}, but not necessarily. For some implementations like the
         * {@linkplain org.apache.sis.metadata.AbstractMetadata#asTreeTable() metadata tree table view},
         * compliance to the {@code List} contract is impractical or inefficient.
         *
         * @return the children, or an empty collection if none.
         * @category tree
         */
        Collection<Node> getChildren();

        /**
         * Creates a new child with the same columns as the other children, and adds it to
         * the {@linkplain #getChildren() children collection}. The new child is typically added at
         * the end of the collection, but this is not mandatory: implementations can add the child
         * at whatever position they see fit.
         *
         * @return the new child.
         * @throws UnsupportedOperationException if this node cannot add new children.
         */
        default Node newChild() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the value in the given column, or {@code null} if none.
         *
         * @param  <V>     the base type of values in the given column.
         * @param  column  identifier of the column from which to get the value.
         * @return the value in the given column, or {@code null} if none.
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
         * @param  <V>     the base type of values in the given column.
         * @param  column  identifier of the column into which to set the value.
         * @param  value   the value to set.
         * @throws IllegalArgumentException if the given column is not a legal column for this node.
         * @throws UnsupportedOperationException if values in the given column cannot be modified.
         *
         * @see TreeTable#getColumns()
         * @see #isEditable(TableColumn)
         * @category table
         */
        default <V> void setValue(TableColumn<V> column, V value) throws IllegalArgumentException, UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Determines whether the value in the specified column is editable. If the given
         * column is not a legal column for this {@code Node} instance, then this method
         * returns {@code false}.
         *
         * @param  column  the column to query.
         * @return {@code true} if the given column is a legal column for this {@code Node}
         *         implementation and the corresponding value is editable, or {@code false}
         *         otherwise.
         * @category table
         */
        default boolean isEditable(TableColumn<?> column) {
            return false;
        }

        /**
         * Returns the user object associated to this node.
         * The user object is for information purpose only and does not appear in the rendered tree.
         * It is typically a Java object whose content is split into the various table columns.
         *
         * <h4>Example</h4>
         * If a {@code CityLocation} class is defined as a (<var>city name</var>, <var>latitude</var>,
         * <var>longitude</var>) tuple, then a {@code TreeTable.Node} could be defined to have 3 columns for the
         * above 3 tuple components, and the user object could be the original {@code CityLocation} instance.
         *
         * @return any object stored at this node by the user, or {@code null} if none.
         * @category tree
         */
        default Object getUserObject() {
            return null;
        }

        /**
         * Returns {@code true} if the given object is a node with the same content as this node.
         * For this method, the meaning of <dfn>same content</dfn> is defined as below:
         *
         * <ul>
         *   <li>The given object is also a {@code Node}.</li>
         *   <li>The list returned by {@link TreeTable#getColumns()} is equal for both nodes.</li>
         *   <li>The objects returned by {@link #getValue(TableColumn)} are equal for each column.</li>
         *   <li>The list returned by {@linkplain #getChildren() children} is equal for both node.</li>
         * </ul>
         *
         * The node returned by {@link #getParent()} shall <strong>not</strong> be taken in account.
         * It is necessary to ignore the parent for consistency with {@linkplain DefaultTreeTable#clone() clone}
         * and for avoiding infinite recursion when comparing the children.
         * A third reason is given below.
         *
         * <h4>Purpose of this method: example with ISO metadata</h4>
         * Consider the following tree made of ISO 19115 metadata objects: a platform containing a list of instruments,
         * and an instrument containing a reference to the platform on which the instrument is installed. In this example,
         * nodes 2 and 4 contain a reference to the same {@code Platform} instance, so we have a cyclic graph:
         *
         * <table class="compact">
         * <caption>Metadata tree example</caption>
         * <tr><th>Node 1:</th><td>{@code  }{@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition information}</td></tr>
         * <tr><th>Node 2:</th><td>{@code   └─}{@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform Platform}</td></tr>
         * <tr><th>Node 3:</th><td>{@code      └─}{@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument Instrument}</td></tr>
         * <tr><th>Node 4:</th><td>{@code         └─}{@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform Platform} (same instance as above)</td></tr>
         * <tr><th>Node 5:</th><td>{@code            └─}<i>etc…</i></td></tr>
         * </table>
         *
         * The {@link org.apache.sis.metadata.AbstractMetadata#asTreeTable()} method gives a view in which each node
         * has its content fully generated from wrapped metadata object. Consequently, a naive walk over the above tree
         * causes an infinite loop with {@code TreeTable} generating nodes with identical content as we bounce between
         * {@code Platform} and {@code Instrument} metadata objects. To break this loop, we need to know when the
         * <em>content</em> of a node (in this example, the wrapped metadata object) has already been visited.
         * The parent shall <strong>not</strong> be taken in account since node 2 and 4 have different parents
         * despite having the same {@code Platform} content.
         *
         * <p>In this use case, the {@code Node.equals(Object)} implementation needs only to compare the wrapped
         * metadata (usually given by the {@linkplain #getUserObject() user object}) since the node content,
         * including the list of children, is fully determined by those metadata. An identity comparison
         * (with {@code ==}) is sufficient for the purpose of avoiding infinite recursion.</p>
         *
         * <h4>Flexibility in implementations</h4>
         * The above list specifies minimal conditions that must be true when two nodes are considered equal.
         * Implementations should not relax those conditions, but are free to make them more restrictive.
         * In particular, many implementations will require that the two nodes are instances of the same class.
         * Some implementations may also perform identity comparisons (with the {@code ==} operator) between values
         * instead of using {@link Object#equals(Object)}. This flexibility means that even if all above conditions
         * are true, this is not a guarantee that this method will return {@code true}.
         *
         * <p>It is okay to <em>not</em> override this method at all since the identity comparison inherited from
         * {@link Object#equals(Object)} is consistent with this method contract. Alternatively, {@code Node}
         * implementations having a content fully determined by the wrapped {@linkplain #getUserObject() user
         * object} need only the following implementation:</p>
         *
         * {@snippet lang="java" :
         *     @Override
         *     public boolean equals(Object obj) {
         *         return (obj instanceof MyNode) && ((MyNode) obj).getUserObject() == getUserObject();
         *     }
         * }
         *
         * Implementation details may vary, for example in the way to compare {@code null} user objects or by invoking
         * {@link Object#equals(Object)} instead of performing identity comparisons. Note however that since this
         * method purpose is to detect cyclic graphs (see above example), user objects should be compared with
         * {@code equals(Object)} only if their implementations are known to be safe against infinite recursion.
         *
         * @param  other  the other object to compare with this node.
         * @return whether the two objects are nodes with equal values and equal children, ignoring parents.
         *
         * @since 0.8
         */
        @Override
        boolean equals(Object other);

        /**
         * Returns a hash code value consistent with the {@code equals(Object)} implementation for this node.
         * If the {@link #equals(Object)} method has not been overridden, then this {@code hashCode()} method
         * should not be overridden neither. Otherwise if this node content ({@linkplain #getValue values} and
         * {@linkplain #getChildren() children}) is fully generated from the {@linkplain #getUserObject() user
         * object}, then the {@code equals(…)} and {@code hashCode()} methods may be implemented like below:
         *
         * {@snippet lang="java" :
         *     @Override
         *     public boolean equals(Object obj) {
         *         return (obj instanceof MyNode) && ((MyNode) obj).getUserObject() == getUserObject();
         *     }
         *
         *     @Override
         *     public int hashCode() {
         *         return System.identityHashCode(getUserObject());
         *     }
         * }
         *
         * Otherwise this method should compute a hash code based on values and children of this node, ignoring parent.
         *
         * @return a hash code for this node, potentially based on values and children but ignoring parent.
         *
         * @since 0.8
         */
        @Override
        int hashCode();
    }
}
