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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Objects;
import java.io.Serializable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Cloner;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.internal.Acyclic;
import org.apache.sis.pending.jdk.JDK19;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * A {@link TreeTable} implementation with a {@linkplain #getColumns() list of columns} given at
 * construction time. The list of columns is unmodifiable, but the {@linkplain #getRoot() root node}
 * can be modified.
 *
 * Example:
 *
 * {@snippet lang="java" :
 *     public class CityLocation {
 *         public static final TableColumn<String> CITY_NAME  = new TableColumn<>(String.class, "City name");
 *         public static final TableColumn<Float>  LATITUDE   = new TableColumn<>(Float.class,  "Latitude");
 *         public static final TableColumn<Float>  LONGTITUDE = new TableColumn<>(Float.class,  "Longitude");
 *
 *         public TreeTable createTable() {
 *             DefaultTreeTable table = new DefaultTreeTable(CITY_NAME, LATITUDE, LONGITUDE);
 *             TreeTable.Node   city  = table.getRoot();
 *             city.setValue(CITY_NAME, "Rimouski");
 *             city.setValue(LATITUDE,   48.470417);
 *             city.setValue(LONGITUDE, -68.521385);
 *             return table;
 *         }
 *     }
 *     }
 *
 * The {@code setRoot(…)} method accepts arbitrary {@link TreeTable.Node} implementations.
 * However, it is likely to be safer and more memory efficient when used together with the
 * implementation provided in the {@link Node} inner class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see Node
 * @see TableColumn
 *
 * @since 0.3
 */
public class DefaultTreeTable implements TreeTable, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7991792044044382191L;

    /**
     * The root node, or {@code null} if not yet specified.
     *
     * @see #getRoot()
     * @see #setRoot(TreeTable.Node)
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
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
     * map shall be read-only since many {@code Node} instances may share it.
     *
     * <h4>Implementation note</h4>
     * This field and the {@link #columns} field could be computed from each other.
     * But we serialize this field anyway because children nodes will typically hold
     * a reference to that map, and we want to preserve the references tree.
     *
     * @see DefaultTreeTable.Node#columnIndices
     */
    @SuppressWarnings("serial")
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
     * @param  columns  the list of table columns.
     */
    public DefaultTreeTable(TableColumn<?>... columns) {
        ArgumentChecks.ensureNonEmpty("columns", columns);
        /*
         * Copy the array for safety against user changes, and also for forcing the element type
         * to TableColumn, not a subclass, because of the UnmodifiableArrayList.wrap(E[]) contract.
         */
        columns = Arrays.copyOf(columns, columns.length, TableColumn[].class);
        this.columnIndices = createColumnIndices(columns);
        this.columns = UnmodifiableArrayList.wrap(columns);
    }

    /**
     * Creates a new tree table initialized to the given root.
     * The {@linkplain #getColumns() list of columns} is inferred from the given node.
     *
     * @param  root  the tree table root (cannot be null).
     */
    public DefaultTreeTable(final Node root) {
        // Implicit null check below.
        this.root = root;
        columnIndices = root.columnIndices;
    }

    /**
     * Creates a map of column indices from the given list of columns.
     * This method is invoked for initializing the {@link #columnIndices} field.
     *
     * @param  columns  the list of columns.
     * @return the map of column indices.
     */
    static Map<TableColumn<?>,Integer> createColumnIndices(final TableColumn<?>[] columns) {
        Map<TableColumn<?>,Integer> map;
        switch (columns.length) {
            case 0:  map = Map.of(); break;
            case 1:  map = null; break;         // Will be created inside the loop (common case).
            default: map = JDK19.newLinkedHashMap(columns.length); break;
        }
        for (int i=0; i<columns.length; i++) {
            final TableColumn<?> column = columns[i];
            ArgumentChecks.ensureNonNullElement("columns", i, column);
            final Integer pos = i;
            if (map == null) {
                map = Map.of(column, pos);
            } else if (map.put(column, pos) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, column));
            }
        }
        return map;
    }

    /**
     * Returns all columns in the given map, sorted by increasing index value.
     * This method relies on {@link LinkedHashMap} preserving insertion order.
     *
     * @return the columns in an array of elements of type {@code TableColumn},
     *         <strong>not a subtype</strong> for allowing usage in
     *         {@link UnmodifiableArrayList#wrap(Object[])}.
     */
    static TableColumn<?>[] getColumns(final Map<TableColumn<?>,Integer> columnIndices) {
        return columnIndices.keySet().toArray(TableColumn<?>[]::new);
    }

    /**
     * Returns the table columns given at construction time.
     * The returned list is never null neither empty.
     *
     * @see Node#getValue(TableColumn)
     * @see Node#setValue(TableColumn, Object)
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Safe because returned list is unmodifiable.
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
     */
    @Override
    public TreeTable.Node getRoot() {
        if (root == null) {
            root = new Node(this);
            initialize(root);
        }
        return root;
    }

    /**
     * Sets the root to the given node. If a root already existed prior this method call,
     * then the previous root node will be discarded.
     *
     * @param  root  the new root node (cannot be null).
     * @throws IllegalArgumentException if the table columns in the given node are inconsistent
     *         with the table columns in this {@code DefaultTreeTable}.
     */
    public void setRoot(final TreeTable.Node root) {
        if (root instanceof Node) {
            final Map<TableColumn<?>,Integer> other = ((Node) root).columnIndices;
            if (other != columnIndices && !columnIndices.keySet().containsAll(other.keySet())) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.InconsistentTableColumns));
            }
        }
        this.root = Objects.requireNonNull(root);
    }

    /**
     * Invoked when {@link #getRoot()} is invoked for the first time and no root had been specified to the constructor.
     * The {@code root} argument is a newly created empty node to be returned by {@link #getRoot()}.
     * The default implementation does nothing.
     * Subclasses can override for lazy initialization of tree table content.
     *
     * @param  root  a newly created tree table root.
     *
     * @since 1.2
     */
    protected void initialize(final TreeTable.Node root) {
    }

    /**
     * Returns a clone of this table. This method clones the {@linkplain #getRoot() root} node.
     * If the root is an instance of {@link Node}, then cloning the root will recursively clone
     * all its {@linkplain Node#getChildren() children}.
     *
     * @return a clone of this table.
     * @throws CloneNotSupportedException if this table, the root node or one of its children
     *         cannot be cloned.
     *
     * @see Node#clone()
     */
    @Override
    public DefaultTreeTable clone() throws CloneNotSupportedException {
        final var clone = (DefaultTreeTable) super.clone();
        if (clone.root instanceof Cloneable) {
            clone.root = (TreeTable.Node) Cloner.cloneIfPublic(clone.root);
        }
        return clone;
    }

    /**
     * Compares the given object with this tree table for equality. This method compares the
     * {@linkplain #getColumns() columns} and the {@linkplain #getRoot() root node}. If the
     * later is an instance of the {@link Node} inner class, then all node values and children
     * will be compared recursively.
     *
     * @param  other  the object to compare with this table.
     * @return {@code true} if the two objects are equal.
     *
     * @see Node#equals(Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final var that = (DefaultTreeTable) other;
            return columnIndices.equals(that.columnIndices) &&
                    Objects.equals(getRoot(), that.getRoot());
        }
        return false;
    }

    /**
     * Returns a hash code value for this table.
     * This method is defined for consistency with {@link #equals(Object)} contract.
     *
     * @see Node#hashCode()
     */
    @Override
    public int hashCode() {
        return (columnIndices.hashCode() + 31*Objects.hashCode(getRoot())) ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this tree table.
     * The current implementation uses a shared instance of {@link TreeTableFormat}.
     * This is okay for debugging or occasional usages. However for more extensive usages,
     * developers are encouraged to create and configure their own {@link TreeTableFormat}
     * instance.
     *
     * @return a string representation of this tree table.
     */
    @Override
    public String toString() {
        synchronized (TreeTableFormat.INSTANCE) {
            return TreeTableFormat.INSTANCE.format(this);
        }
    }




    /**
     * A {@link TreeTable.Node} implementation which can store values for a predefined list of columns.
     * The list of columns is specified by a {@link TreeTable}, or inherited from a parent node.
     *
     * <h2>Note on the parent node</h2>
     * The value returned by the {@link #getParent()} method is updated automatically when this node
     * is <em>added to</em> or <em>removed from</em> the {@linkplain #getChildren() list of children}
     * of another {@code Node} instance - there is no {@code setParent(Node)} method. Since the parent
     * is inferred rather than user-specified, it is ignored by the {@link #clone()},
     * {@link #equals(Object)} and {@link #hashCode()} methods.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.3
     * @since   0.3
     *
     * @see DefaultTreeTable
     * @see TableColumn
     */
    @Acyclic
    public static class Node implements TreeTable.Node, Cloneable, Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5729029633479218691L;

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
             * @param  parent  the node which will own this list.
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
                            Errors.Keys.IllegalArgumentClass_3, "node", Node.class, node.getClass()));
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
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        private TreeTable.Node parent;

        /**
         * The list of children, or {@code null} if none.
         * Created only when first needed.
         */
        @SuppressWarnings("serial")
        private List<TreeTable.Node> children;

        /**
         * The index of values associated to each column. This map is used by the
         * {@link #getValue(TableColumn)} and {@link #setValue(TableColumn, Object)}
         * methods for identifying the index where to store values in the {@link #values} array.
         *
         * <p>This map shall be read-only since many {@code Node} instances may share it.</p>
         *
         * @see DefaultTreeTable#columnIndices
         */
        @SuppressWarnings("serial")
        final Map<TableColumn<?>,Integer> columnIndices;

        /**
         * The values, or {@code null} if not yet created.
         */
        @SuppressWarnings("serial")
        private Object[] values;

        /**
         * Creates a new node for the given table. The new node will be able to store a value
         * for each {@linkplain TreeTable#getColumns() columns} defined in the given table.
         *
         * <p>This method does not set the new node as the root of the given table. If desired, it
         * is the caller responsibility to {@linkplain DefaultTreeTable#setRoot set the table root
         * node}.</p>
         *
         * @param  table  the table for which this node is created.
         */
        public Node(final TreeTable table) {
            // Implicit null check below.
            if (table instanceof DefaultTreeTable) {
                // Share the same instance if possible.
                columnIndices = ((DefaultTreeTable) table).columnIndices;
            } else {
                final List<TableColumn<?>> columns = table.getColumns();
                columnIndices = createColumnIndices(columns.toArray(TableColumn<?>[]::new));
            }
        }

        /**
         * Creates a new node with the given parent. The new node is added at the end of the parent
         * {@linkplain #getChildren() list of children}. The new node will be able to store values
         * for the same columns as the parent node.
         *
         * @param  parent  the parent of the new node.
         */
        @SuppressWarnings("this-escape")
        public Node(final Node parent) {
            this.parent = parent;
            columnIndices = parent.columnIndices;
            final var addTo = (TreeNodeList) parent.getChildren();
            addTo.addChild(addTo.size(), this);
        }

        /**
         * Creates a new node with the given parent. The new node is added to the parent
         * {@linkplain #getChildren() list of children} at the given index. The new node
         * will be able to store values for the same columns as the parent node.
         *
         * @param  parent  the parent of the new node.
         * @param  index   the index where to add the new node in the parent list of children.
         */
        @SuppressWarnings("this-escape")
        public Node(final Node parent, final int index) {
            this.parent = parent;
            columnIndices = parent.columnIndices;
            final var addTo = (TreeNodeList) parent.getChildren();
            addTo.addChild(Objects.checkIndex(index, addTo.size() + 1), this);
        }

        /**
         * Creates a node with a single column for object names (<i>convenience constructor</i>).
         * The node will have the following columns:
         *
         * <table class="sis">
         *   <caption>Node columns</caption>
         *   <tr><th>Header</th> <th>Type</th>                 <th>Initial value</th></tr>
         *   <tr><td>"Name"</td> <td>{@link CharSequence}</td> <td>{@code name}</td></tr>
         * </table>
         *
         * @param  name  the initial value for the "Name" column (can be {@code null}).
         */
        public Node(final CharSequence name) {
            columnIndices = TableColumn.NAME_MAP;
            if (name != null) {
                values = new CharSequence[] {name};
            }
        }

        /**
         * Returns the parent of this node. On {@code Node} creation, this value may be initially
         * {@code null}. It will be automatically set to a non-null value when this node will be
         * added as a child of another {@code Node} instance.
         *
         * <p>Note that the parent is intentionally ignored by the {@link #clone()},
         * {@link #equals(Object)} and {@link #hashCode()} methods.</p>
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
         * Returns {@code true} if this node cannot have any children. The default implementation
         * unconditionally returns {@code false} even if the list of children is empty, because the
         * list is allowed to grow at any time.
         *
         * <p>Subclasses can override this method if they can determine which nodes are leaves.
         * In the current implementation, the return value shall be stable (i.e. a node cannot
         * alternate between leaf and non-leaf state). However, this restriction may be relaxed
         * in a future SIS version.</p>
         */
        @Override
        public boolean isLeaf() {
            return false;
        }

        /**
         * Returns the children of this node. For non-leaf nodes, the list is modifiable and will
         * automatically updates the {@linkplain #getParent() parent} reference of any {@code Node}
         * instance added to or removed from the list.
         *
         * <p>For leaf nodes, this method returns an unmodifiable {@linkplain List#of() empty list}.</p>
         */
        /* NOTE: If a future version removes the "final" keyword, then search for calls to
         * this method where the return value is cast to TreeNodeList. Any unconditional
         * cast will need to be replaced by an "instanceof" check.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Returned list is modifiable on intent.
        public final List<TreeTable.Node> getChildren() {
            if (children == null) {
                if (isLeaf()) {
                    children = List.of();
                } else {
                    children = new Children(this);
                }
            }
            return children;
        }

        /**
         * Adds a new child in the {@linkplain #getChildren() children list}.
         * The default implementation first checks that this node is not a leaf,
         * then delegates to the {@code Node(Node)} constructor.
         * That constructor call has the following implications:
         *
         * <ul>
         *   <li>The new node inherits the columns of this node, on the assumption that
         *       they are the same set of columns than other children nodes.</li>
         *   <li>The new node is appended at the end of the children list.</li>
         * </ul>
         *
         * Subclasses may override this method with different behavior.
         *
         * @throws UnsupportedOperationException if this node {@linkplain #isLeaf() is a leaf}.
         */
        @Override
        public Node newChild() {
            if (isLeaf()) {
                throw new UnsupportedOperationException(Errors.format(Errors.Keys.NodeIsLeaf_1, this));
            }
            return new Node(this);
        }

        /**
         * Returns the value in the given column, or {@code null} if none.
         *
         * @param  <V>     the base type of values in the given column.
         * @param  column  identifier of the column from which to get the value.
         * @return the value in the given column, or {@code null} if none.
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
         * @param  <V>     the base type of values in the given column.
         * @param  column  identifier of the column into which to set the value.
         * @param  value   the value to set.
         * @throws IllegalArgumentException if the given column is not a legal column for this node.
         *
         * @see #isEditable(TableColumn)
         */
        @Override
        public <V> void setValue(final TableColumn<V> column, final V value) throws IllegalArgumentException {
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
         * Returns a clone of this node without parent.
         * This method recursively clones all {@linkplain #getChildren() children},
         * but does not clone the column {@linkplain #getValue(TableColumn) values}.
         * The parent of the cloned node is set to {@code null}.
         *
         * @return a clone of this node without parent.
         * @throws CloneNotSupportedException if this node or one of its children cannot be cloned.
         */
        @Override
        public Node clone() throws CloneNotSupportedException {
            final Node clone = (Node) super.clone();
            clone.parent = null;
            if (clone.values != null) {
                clone.values = clone.values.clone();
            }
            if (clone.children != null) {
                clone.children = new Children(clone);
                for (final TreeTable.Node child : children) {
                    /*
                     * Implementation note: we could have used the Cloner for cloning arbitrary
                     * node implementations, but children.add(...) would fail anyway because it
                     * cannot set the parent of unknown implementation.
                     */
                    if (!(child instanceof Node)) {
                        throw new CloneNotSupportedException(Errors.format(
                                Errors.Keys.CloneNotSupported_1, child.getClass()));
                    }
                    clone.children.add(((Node) child).clone());
                }
            }
            return clone;
        }

        /**
         * Compares the given object with this node for {@linkplain #getValue(TableColumn) values}
         * and {@linkplain #getChildren() children} equality, ignoring the {@linkplain #getParent()
         * parent}. This method can be used for determining if two branches of a same tree or of two
         * different trees are identical.
         *
         * <h4>Implementation note</h4>
         * This method ignores the parent because:
         * <ul>
         *   <li>When comparing the children recursively, comparing the parents would cause infinite recursion.</li>
         *   <li>For consistency with the {@link #clone()} method, which cannot clone the parent.</li>
         *   <li>For making possible to compare branches instead of only whole trees.</li>
         * </ul>
         *
         * @param  other  the object to compare with this node.
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
                    if (v1 != v2) {                                 // For skipping the loop if v1 and v2 are null.
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Object[] values = this.values;
            if (values != null) {
                /*
                 * Do not use Objects.hashCode(…) because we want the result of array
                 * containing only null elements to be the same as null array (zero).
                 */
                for (int i=values.length; --i>=0;) {
                    hash = 31*hash + Objects.hashCode(values[i]);
                }
            }
            /*
             * Do not use Objects.hashCode(…) because we
             * want the same result for null and empty list.
             */
            if (!isNullOrEmpty(children)) {
                hash += 37 * children.hashCode();
            }
            return hash ^ (int) serialVersionUID;
        }

        /**
         * Returns a string representation of this node for identification in error message or in debugger.
         * The default implementation returns the {@code toString()} value of the first non-empty
         * {@link CharSequence} found in the {@linkplain #getValue(TableColumn) values}, if any.
         * If no such value is found, then this method returns "<var>Node</var>-<var>i</var>"
         * where <var>Node</var> is the {@linkplain Class#getSimpleName() simple classname}
         * and <var>i</var> is the index of this node in the parent node.
         *
         * @return a string representation of this node.
         */
        @Override
        public String toString() {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Object[] values = this.values;
            if (values != null) {
                for (final Object value : values) {
                    if (value instanceof CharSequence) {
                        String text = value.toString();
                        if (text != null && !(text = text.strip()).isEmpty()) {
                            return text;
                        }
                    }
                }
            }
            String name = getClass().getSimpleName();
            if (parent != null) {
                final Collection<TreeTable.Node> pc = parent.getChildren();
                if (pc instanceof List<?>) {
                    name = name + '-' + ((List<TreeTable.Node>) pc).indexOf(this);
                }
            }
            return name;
        }
    }
}
