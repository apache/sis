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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.io.Serializable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.collection.Collections.isNullOrEmpty;
import static org.apache.sis.util.collection.Collections.hashMapCapacity;

// Related to JDK7
import java.util.Objects;


/**
 * A {@link TreeTable} implementation with a {@linkplain #getColumns() list of columns} given at
 * construction time. The list of columns is unmodifiable, but the {@linkplain #getRoot() root node}
 * can be modified.
 *
 * Example:
 *
 * {@preformat java
 *     class CityLocation {
 *         public static final TableColumn<String> CITY_NAME  = new MyColumn<>(String.class);
 *         public static final TableColumn<Float>  LATITUDE   = new MyColumn<>(Float .class);
 *         public static final TableColumn<Float>  LONGTITUDE = new MyColumn<>(Float .class);
 *
 *         TreeTable createTable() {
 *             DefaultTreeTable table = new DefaultTreeTable(CITY_NAME, LATITUDE, LONGITUDE);
 *             TreeTable.Node   city  = new DefaultTreeTable.Node(table);
 *             city.setValue(CITY_NAME, "Rimouski");
 *             city.setValue(LATITUDE,   48.470417);
 *             city.setValue(LONGITUDE, -68.521385);
 *             table.setRoot(city);
 *             return table;
 *         }
 *     }
 * }
 *
 * {@code DefaultTreeTable} accepts arbitrary {@link TreeTable.Node} implementations.
 * However it is likely to be safer and more memory efficient when used together with
 * the implementation provided in the {@link Node} inner class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@NotThreadSafe
public class DefaultTreeTable implements TreeTable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1951201018202846555L;

    /**
     * The root node, or {@code null} if not yet specified.
     *
     * @see #getRoot()
     * @see #setRoot(TreeTable.Node)
     */
    private TreeTable.Node root;

    /**
     * The table columns as an unmodifiable list, or {@code null} if not yet created.
     * The content of this list is the {@link #columnIndices} keys sorted by their index values.
     *
     * @see #getColumns()
     */
    private transient List<TableColumn<?>> columns;

    /**
     * The index of values associated to each column. This is used by the {@link Node}
     * implementation for storing values in a single flat array. After creation, this
     * map shall be read-only since many {@code Node} instances may share it.
     *
     * {@note This field and the {@link #columns} field could be computed from each other.
     *        We serialize this field because children nodes will typically hold a reference
     *        to that map, and we want to preserve the references tree.}
     *
     * @see DefaultTreeTable.Node#columnIndices
     */
    final Map<TableColumn<?>,Integer> columnIndices;

    /**
     * Creates a new table using the given columns.
     */
    DefaultTreeTable(final Map<TableColumn<?>,Integer> columnIndices) {
        this.columnIndices = columnIndices;
    }

    /**
     * Creates a new tree table with the given columns. The given array shall not be null or
     * empty, and shall not contain null or duplicated elements.
     *
     * <p>The {@linkplain #getRoot() root} node is initially {@code null}. Callers can initialize
     * it after construction time by a call to the {@link #setRoot(TreeTable.Node)} method.</p>
     *
     * @param columns The table columns.
     */
    public DefaultTreeTable(TableColumn<?>... columns) {
        ArgumentChecks.ensureNonNull("columns", columns);
        if (columns.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "columns"));
        }
        columns = columns.clone();
        this.columnIndices = createColumnIndices(columns);
        this.columns = UnmodifiableArrayList.wrap(columns);
    }

    /**
     * Creates a new tree table initialized to the given root.
     * The {@linkplain #getColumns() list of columns} is inferred from the given node.
     *
     * @param root The tree table root (can not be null).
     */
    public DefaultTreeTable(final Node root) {
        ArgumentChecks.ensureNonNull("root", root);
        this.root = root;
        columnIndices = root.columnIndices;
    }

    /**
     * Creates a map of column indices from the given list of columns.
     * This method is invoked for initializing the {@link #columnIndices} field.
     *
     * @param  columns The list of columns.
     * @return The map of column indices.
     */
    static Map<TableColumn<?>,Integer> createColumnIndices(final TableColumn<?>[] columns) {
        Map<TableColumn<?>,Integer> map;
        switch (columns.length) {
            case 0:  map = Collections.emptyMap(); break;
            case 1:  map = null; break; // Will be created inside the loop (common case).
            default: map = new LinkedHashMap<>(hashMapCapacity(columns.length)); break;
        }
        for (int i=0; i<columns.length; i++) {
            ArgumentChecks.ensureNonNull("columns", i, columns);
            final TableColumn<?> column = columns[i];
            final Integer pos = i;
            if (map == null) {
                map = Collections.<TableColumn<?>,Integer>singletonMap(column, pos);
            } else if (map.put(column, pos) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedValue_1, column));
            }
        }
        return map;
    }

    /**
     * Returns all columns in the given map, sorted by increasing index value.
     * This method relies on {@link LinkedHashSet} preserving insertion order.
     */
    static TableColumn<?>[] getColumns(final Map<TableColumn<?>,Integer> columnIndices) {
        return columnIndices.keySet().toArray(new TableColumn<?>[columnIndices.size()]);
    }

    /**
     * Returns the table columns given at construction time.
     * The returned list is never null neither empty.
     */
    @Override
    public final List<TableColumn<?>> getColumns() {
        if (columns == null) {
            columns = UnmodifiableArrayList.wrap(getColumns(columnIndices));
        }
        return columns;
    }

    /**
     * Returns the root node. This method returns the node specified at
     * {@linkplain #DefaultTreeTable(Node) construction time} or to the
     * last call of the {@link #setRoot(TreeTable.Node)} method.
     *
     * @throws IllegalStateException If the root node has not yet been specified.
     */
    @Override
    public TreeTable.Node getRoot() {
        if (root == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.NodeNotFound_1, "root"));
        }
        return root;
    }

    /**
     * Sets the root to the given node. If a root already existed prior this method call,
     * then the previous root node will be discarded.
     *
     * @param  root The new root node (can not be null).
     * @throws IllegalArgumentException If the table columns in the given node are inconsistent
     *         with the table columns in this {@code DefaultTreeTable}.
     */
    public void setRoot(final TreeTable.Node root) {
        ArgumentChecks.ensureNonNull("root", root);
        if (root instanceof Node) {
            final Map<TableColumn<?>,Integer> other = ((Node) root).columnIndices;
            if (other != columnIndices && !columnIndices.keySet().containsAll(other.keySet())) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.InconsistentTableColumns));
            }
        }
        this.root = root;
    }

    /**
     * Compares the given object with this tree table for equality. This method compares the
     * {@linkplain #getColumns() columns} and the {@linkplain #getRoot() root node}. If the
     * later is an instance of the {@link Node} inner class, then all node values and children
     * will be {@linkplain Node#equals(Object) compared} recursively.
     *
     * @param  other The object to compare with this table.
     * @return {@code true} if the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final DefaultTreeTable that = (DefaultTreeTable) other;
            return columnIndices.equals(that.columnIndices) &&
                    Objects.equals(root, that.root);
        }
        return false;
    }

    /**
     * Returns a hash code value for this table.
     * This method is defined for consistency with {@link #equals(Object)} contract.
     */
    @Override
    public int hashCode() {
        return (columnIndices.hashCode() + 31*Objects.hashCode(root)) ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this tree table.
     * The default implementation delegates to {@link TreeTables#toString(TreeTable)}.
     * This is okay for debugging or occasional usages. However for more extensive usages,
     * developers are encouraged to create and configure their own {@link TreeTableFormat}
     * instance.
     *
     * @return A string representation of this tree table.
     */
    @Override
    public String toString() {
        return TreeTables.toString(this);
    }




    /**
     * A {@link TreeTable.Node} implementation which can store values for a pre-defined list
     * of columns. The list of columns is specified by a {@link TreeTable}, or inherited from
     * a parent node.
     *
     * <p>The {@linkplain #getChildren() list of children} provided by this class is <cite>live</cite>:
     * adding a {@code Node} child to that list will automatically set its parent to {@code this},
     * and removing a {@code Node} from that list will set its parent to {@code null}.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    @NotThreadSafe
    public static class Node implements TreeTable.Node, Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2931274954865719140L;

        /**
         * Implementation of {@link Node} children list. This list updates automatically the
         * {@link Node#parent} field when the enclosing node is added to or removed from the
         * list of children of another {@code Node} instance.
         */
        private static final class Children extends TreeNodeList {
            /**
             * For cross-version compatibility.
             */
            private static final long serialVersionUID = -1543888535672160884L;

            /**
             * Creates a new, initially empty, node list. The node given in argument to this
             * constructor will be the parent of all nodes added as children to this list.
             *
             * @param parent The node which will own this list.
             */
            Children(final TreeTable.Node parent) {
                super(parent);
            }

            /**
             * Sets the parent of the given node if it is an instance of {@link Node},
             * or throws an exception otherwise. This method is invoked when a node is
             * added to or removed from the list.
             */
            @Override
            protected void setParentOf(final TreeTable.Node node, final int mode) throws IllegalArgumentException {
                if (!(node instanceof Node)) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalArgumentClass_3, "node", node.getClass(), Node.class));
                }
                final TreeTable.Node p;
                switch (mode) {
                    case NULL: p = null;   break;
                    case THIS: p = parent; break;
                    case DRY_RUN: return;
                    default: throw new AssertionError(mode);
                }
                ((Node) node).setParent(p);
            }
        }

        /**
         * The parent of this node, or {@code null} if none.
         *
         * @see #getParent()
         * @see #setParent(TreeTable.Node)
         */
        private TreeTable.Node parent;

        /**
         * The list of children, or {@code null} if none.
         * Created only when first needed.
         */
        private List<TreeTable.Node> children;

        /**
         * The index of values associated to each column. This map is used by the
         * {@link #getValue(TableColumn)} and {@link #setValue(TableColumn, Object)}
         * methods for identifying the index where to store values in the {@link #values} array.
         *
         * <p>This map shall be read-only since many {@code Node} instances may share it.</p>
         *
         * @see DefaultTreeTable#columnIndices
         */
        final Map<TableColumn<?>,Integer> columnIndices;

        /**
         * The values, or {@code null} if not yet created.
         */
        private Object[] values;

        /**
         * Creates a new node with the given shared map of columns and the given values.
         */
        Node(final Map<TableColumn<?>,Integer> columnIndices, final Object[] values) {
            this.columnIndices = columnIndices;
            this.values = values;
        }

        /**
         * Creates a new node for the given table. The new node will be able to store a value
         * for each {@linkplain TreeTable#getColumns() columns} defined in the given table.
         *
         * <p>This method does not set the new node as the root of the given table. If desired, it
         * is the caller responsibility to {@linkplain DefaultTreeTable#setRoot set the table root
         * node}.</p>
         *
         * @param table The table for which this node is created.
         */
        public Node(final TreeTable table) {
            ArgumentChecks.ensureNonNull("table", table);
            if (table instanceof DefaultTreeTable) {
                // Share the same instance if possible.
                columnIndices = ((DefaultTreeTable) table).columnIndices;
            } else {
                final List<TableColumn<?>> columns = table.getColumns();
                columnIndices = createColumnIndices(columns.toArray(new TableColumn<?>[columns.size()]));
            }
        }

        /**
         * Creates a new node with the given parent. The new node is added at the end of the parent
         * {@linkplain #getChildren() list of children}. The new node will be able to store values
         * for the same columns than the parent node.
         *
         * @param parent The parent of the new node.
         */
        public Node(final Node parent) {
            ArgumentChecks.ensureNonNull("parent", parent);
            this.parent = parent;
            columnIndices = parent.columnIndices;
            final TreeNodeList addTo = (TreeNodeList) parent.getChildren();
            addTo.addChild(addTo.size(), this);
        }

        /**
         * Creates a new node with the given parent. The new node is added to the parent
         * {@linkplain #getChildren() list of children} at the given index. The new node
         * will be able to store values for the same columns than the parent node.
         *
         * @param parent The parent of the new node.
         * @param index  The index where to add the new node in the parent list of children.
         */
        public Node(final Node parent, final int index) {
            ArgumentChecks.ensureNonNull("parent", parent);
            this.parent = parent;
            columnIndices = parent.columnIndices;
            final TreeNodeList addTo = (TreeNodeList) parent.getChildren();
            ArgumentChecks.ensureValidIndex(addTo.size() + 1, index);
            addTo.addChild(index, this);
        }

        /**
         * Returns the parent of this node. On {@code Node} creation, this value may be initially
         * {@code null}. It will be automatically set to a non-null value when this node will be
         * added as a child of another {@code Node} instance.
         */
        @Override
        public final TreeTable.Node getParent() {
            return parent;
        }

        /**
         * Sets the parent to the given node. Before doing so, this method ensures that the
         * columns in this node are consistent with the columns in the parent node.
         */
        final void setParent(final TreeTable.Node node) {
            if (node instanceof Node) {
                final Map<TableColumn<?>,Integer> other = ((Node) node).columnIndices;
                if (other != columnIndices && !other.keySet().containsAll(columnIndices.keySet())) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.InconsistentTableColumns));
                }
            }
            parent = node;
        }

        /**
         * Returns the node children. This list is modifiable and updates automatically the
         * {@linkplain #getParent() parent} reference of any {@code Node} instance added to
         * ore removed from this list.
         */
        /* NOTE: If a future version removes the "final" keyword, then search for calls to
         * this method where the return value is casted to TreeNodeList. Any unconditional
         * cast will need to be replaced by an "instanceof" check.
         */
        @Override
        public final List<TreeTable.Node> getChildren() {
            if (children == null) {
                children = new Children(this);
            }
            return children;
        }

        /**
         * Returns the value in the given column, or {@code null} if none.
         *
         * @param  <V>    The base type of values in the given column.
         * @param  column Identifier of the column from which to get the value.
         * @return The value in the given column, or {@code null} if none.
         */
        @Override
        public <V> V getValue(final TableColumn<V> column) {
            ArgumentChecks.ensureNonNull("column", column);
            if (values != null) {
                final Integer index = columnIndices.get(column);
                if (index != null) {
                    return column.getElementType().cast(values[index]);
                }
            }
            return null;
        }

        /**
         * Sets the value for the given column.
         * The {@link #isEditable(TableColumn)} method can be invoked before this setter method
         * for determining if the given column is modifiable.
         *
         * @param  <V>    The base type of values in the given column.
         * @param  column Identifier of the column into which to set the value.
         * @param  value  The value to set.
         * @throws IllegalArgumentException If the given column is not a legal column for this node.
         *
         * @see #isEditable(TableColumn)
         */
        @Override
        public <V> void setValue(final TableColumn<V> column, final V value) {
            ArgumentChecks.ensureNonNull("column", column);
            final Integer index = columnIndices.get(column);
            if (index == null) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalArgumentValue_2, "column", column));
            }
            if (values == null) {
                if (value == null) return;
                values = new Object[columnIndices.size()];
            }
            values[index] = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEditable(final TableColumn<?> column) {
            ArgumentChecks.ensureNonNull("column", column);
            return columnIndices.containsKey(column);
        }

        /**
         * Returns the user object associated to this node.
         * The default implementation returns {@code null}.
         */
        @Override
        public Object getUserObject() {
            return null;
        }

        /**
         * Compares the given object with this node for {@linkplain #getValue(TableColumn) values}
         * and {@linkplain #getChildren() children} equality, ignoring the {@linkplain #getParent()
         * parent}. This method can be used for determining if two branches of a same tree or of two
         * different trees are identical.
         *
         * <p>This method compares children recursively, which is another reason why the parents
         * need to be ignored.</p>
         *
         * @param  other The object to compare with this node.
         * @return {@code true} if the two objects are equal, ignoring the parent node.
         */
        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (other != null && other.getClass() == getClass()) {
                final Node that = (Node) other;
                if (columnIndices.equals(that.columnIndices)) {
                    final Object[] v1 = this.values;
                    final Object[] v2 = that.values;
                    if (v1 != v2) { // For skipping the loop if v1 and v2 are null.
                        for (int i=columnIndices.size(); --i>=0;) {
                            if (!Objects.equals((v1 != null) ? v1[i] : null,
                                                (v2 != null) ? v2[i] : null))
                            {
                                return false;
                            }
                        }
                    }
                    final List<TreeTable.Node> c1 = this.children;
                    final List<TreeTable.Node> c2 = that.children;
                    final int n = (c1 != null) ? c1.size() : 0;
                    if (((c2 != null) ? c2.size() : 0) == n) {
                        for (int i=0; i<n; i++) {
                            if (!c1.get(i).equals(c2.get(i))) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Returns a hash-code value computed from the {@linkplain #getValue(TableColumn) values}
         * and {@linkplain #getChildren() children}, ignoring the {@linkplain #getParent() parent}.
         * This method is defined for consistency with {@link #equals(Object)} contract.
         */
        @Override
        public int hashCode() {
            int hash = 0;
            final Object[] values = this.values;
            if (values != null) {
                // Do not use Objects.hashCode(...) because we want the result of array
                // containing only null elements to be the same than null array (zero).
                for (int i=values.length; --i>=0;) {
                    hash = 31*hash + Objects.hash(values[i]);
                }
            }
            // Do not use Objects.hashCode(...) because we
            // want the same result for null and empty list.
            if (!isNullOrEmpty(children)) {
                hash += 37 * children.hashCode();
            }
            return hash ^ (int) serialVersionUID;
        }

        /**
         * Returns a string representation of this node, for identification in error message
         * or in debugger.
         *
         * @return A string representation of this node.
         */
        @Override
        public String toString() {
            Object value = getUserObject();
            if (value instanceof CharSequence) {
                return value.toString();
            }
            final Object[] values = this.values;
            if (values != null) {
                for (int i=0; i<values.length; i++) {
                    value = values[i];
                    if (value instanceof CharSequence) {
                        return value.toString();
                    }
                }
            }
            String name = getClass().getSimpleName();
            if (parent != null) {
                name = name + '-' + parent.getChildren().indexOf(this);
            }
            return name;
        }
    }
}
