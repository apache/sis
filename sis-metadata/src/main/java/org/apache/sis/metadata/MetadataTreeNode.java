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
package org.apache.sis.metadata;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.resources.Errors;


/**
 * A node in a {@link MetadataTreeTable} view. The {@code MetadataTreeTable} class is used directly
 * only for the root node, or for nodes containing a fixed value instead than a value fetched from
 * the metadata object. For all other nodes, the actual node class shall be either {@link Element}
 * or {@link CollectionElement}.
 *
 * <p>The value of a node is extracted from the {@linkplain #metadata} object by {@link #getUserObject()}.
 * For each instance of {@code MetadataTreeTable}, that value is always a singleton, never a collection.
 * If a metadata property is a collection, then there is an instance of the {@link CollectionElement}
 * subclass for each element in the collection.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class MetadataTreeNode implements TreeTable.Node, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3499128444388320L;

    /**
     * The table for which this node is an element. Contains information like
     * the metadata standard and the value existence policy.
     *
     * <p>All {@code MetadataTreeNode} instances in the same tree have
     * a reference to the same {@code MetadataTreeTable} instance.</p>
     */
    final MetadataTreeTable table;

    /**
     * The parent of this node to be returned by {@link #getParent()},
     * or {@code null} if this node is the root of the tree.
     *
     * @see #getParent()
     */
    private final MetadataTreeNode parent;

    /**
     * The metadata object from which the {@link #getUserObject()} method will fetch the value.
     * The value is fetched in different ways, which depend on the {@code MetadataTreeNode} subclass:
     *
     * <ul>
     *   <li>For {@code MetadataTreeNode} (usually the root of the tree),
     *       this is the value to return directly.</li>
     *   <li>For {@link Element} (a metadata property which is not a collection)
     *       The value is {@code accessor.get(indexInData, metadata)}.</li>
     *   <li>For {@link CollectionElement} (an element in a collection),
     *       an other index is used for fetching the element in that collection.</li>
     * </ul>
     *
     * This field shall never be null.
     *
     * @see #getUserObject()
     */
    final Object metadata;

    /**
     * The value of {@link TableColumn#NAME}, computed by {@link #getName()} then cached.
     *
     * @see #getName()
     */
    private transient String name;

    /**
     * The children of this node, or {@code null} if not yet computed. If and only if the node
     * can not have children (i.e. {@linkplain #isLeaf() is a leaf}), then this field is set to
     * {@link Collections#EMPTY_LIST}.
     *
     * @see #getChildren()
     */
    private transient List<TreeTable.Node> children;

    /**
     * Creates the root node of a new metadata tree table.
     *
     * @param  table    The table which is creating this root node.
     * @param  metadata The root metadata object (can not be null).
     */
    MetadataTreeNode(final MetadataTreeTable table, final Object metadata) {
        this.table    = table;
        this.parent   = null;
        this.metadata = metadata;
    }

    /**
     * Creates a new child for an element of the given metadata.
     *
     * @param  parent   The parent of this node.
     * @param  metadata The metadata object for which this node will be a value.
     */
    MetadataTreeNode(final MetadataTreeNode parent, final Object metadata) {
        this.table    = parent.table;
        this.parent   = parent;
        this.metadata = metadata;
    }

    /**
     * Gets the name of this node. The name shall be stable, since it will be cached by the caller.
     * The default implementation is suitable only for the root node - subclasses must override.
     */
    String getName() {
        final Class<?> type = metadata.getClass();
        String name = Types.getStandardName(type);
        if (name == null) {
            name = Classes.getShortName(type);
        }
        return name;
    }

    /**
     * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
     * The default implementation is suitable only for the root node - subclasses must override.
     */
    void identifier(final StringBuilder buffer) {
        buffer.append(Classes.getShortClassName(metadata));
    }

    /**
     * Returns the base type of values to be returned by {@link #getUserObject()}.
     * The default implementation is suitable only for the root node - subclasses must override.
     */
    public Class<?> getElementType() {
        return table.standard.getInterface(metadata.getClass());
    }

    /**
     * The metadata value for this node, to be returned by {@code getValue(TableColumn.VALUE)}.
     * The default implementation is suitable only for the root node - subclasses must override.
     */
    @Override
    public Object getUserObject() {
        return metadata;
    }

    /**
     * Sets the metadata value for this node. Subclasses must override this method.
     *
     * @throws UnsupportedOperationException If the metadata value is not writable.
     */
    void setUserObject(final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableCellValue_2,
                getValue(TableColumn.NAME), TableColumn.VALUE.getHeader()));
    }

    /**
     * Returns {@code true} if the metadata value can be set.
     * Subclasses must override this method.
     */
    boolean isWritable() {
        return false;
    }

    /**
     * A node for a metadata property value. This class does not store the property value directly.
     * Instead, is stores a reference to the metadata object that contains the property values,
     * together with the index for fetching the value in that object. That way, the real storage
     * objects still the metadata object, which allow {@link MetadataTreeTable} to be a dynamic view.
     *
     * <p>Instances of this class shall be instantiated only for metadata singletons. If a metadata
     * property is a collection, then the {@link CollectionElement} subclass shall be instantiated
     * instead.</p>
     */
    static class Element extends MetadataTreeNode {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -1837090036924521907L;

        /**
         * The accessor to use for fetching the property names, types and values from the
         * {@link #metadata} object. Note that the value of this field is the same for all
         * siblings.
         */
        private final PropertyAccessor accessor;

        /**
         * Index of the value in the {@link #metadata} object to be fetched with the
         * {@link #accessor}.
         */
        private final int indexInData;

        /**
         * Creates a new child for a property of the given metadata at the given index.
         *
         * @param  parent      The parent of this node.
         * @param  metadata    The metadata object for which this node will be a value.
         * @param  accessor    Accessor to use for fetching the name, type and value.
         * @param  indexInData Index to be given to the accessor of fetching the value.
         */
        Element(final MetadataTreeNode parent, final Object metadata,
                final PropertyAccessor accessor, final int indexInData)
        {
            super(parent, metadata);
            this.accessor = accessor;
            this.indexInData = indexInData;
        }

        /**
         * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
         */
        @Override
        void identifier(final StringBuilder buffer) {
            super.identifier(buffer);
            buffer.append('.').append(accessor.name(indexInData, KeyNamePolicy.JAVABEANS_PROPERTY));
        }

        /**
         * Gets the name of this node. Current implementation derives the name from the
         * {@link KeyNamePolicy#UML_IDENTIFIER} instead than {@link KeyNamePolicy#JAVABEANS_PROPERTY}
         * in order to get the singular form instead of the plural one, because we will create one
         * node for each element in a collection.
         */
        @Override
        String getName() {
            return CharSequences.camelCaseToSentence(accessor.name(indexInData, KeyNamePolicy.UML_IDENTIFIER)).toString();
        }

        /**
         * Returns the type of property elements.
         */
        @Override
        public final Class<?> getElementType() {
            return accessor.type(indexInData, TypeValuePolicy.ELEMENT_TYPE);
        }

        /**
         * Fetches the node value from the metadata object.
         */
        @Override
        public Object getUserObject() {
            return accessor.get(indexInData, metadata);
        }

        /**
         * Sets the metadata value for this node.
         */
        @Override
        void setUserObject(final Object value) {
            accessor.set(indexInData, metadata, value, false);
        }

        /**
         * Returns {@code true} if the metadata is writable.
         */
        @Override
        final boolean isWritable() {
            return accessor.isWritable(indexInData);
        }
    }

    /**
     * A node for an element in a collection. This class needs the iteration order to be stable.
     */
    static final class CollectionElement extends Element {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -1156865958960250473L;

        /**
         * Index of the element in the collection, in iteration order.
         */
        final int indexInList;

        /**
         * Creates a new node for the given collection element.
         *
         * @param  parent      The parent of this node.
         * @param  metadata    The metadata object for which this node will be a value.
         * @param  accessor    Accessor to use for fetching the name, type and collection.
         * @param  indexInData Index to be given to the accessor of fetching the collection.
         * @param  indexInList Index of the element in the collection, in iteration order.
         */
        CollectionElement(final MetadataTreeNode parent, final Object metadata,
                final PropertyAccessor accessor, final int indexInData, final int indexInList)
        {
            super(parent, metadata, accessor, indexInData);
            this.indexInList = indexInList;
        }

        /**
         * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
         */
        @Override
        void identifier(final StringBuilder buffer) {
            super.identifier(buffer);
            buffer.append('[').append(indexInList).append(']');
        }

        /**
         * Fetches the node value from the metadata object.
         */
        @Override
        public Object getUserObject() {
            final Collection<?> values = (Collection<?>) super.getUserObject();
            try {
                if (values instanceof List<?>) {
                    return ((List<?>) values).get(indexInList);
                }
                final Iterator<?> it = values.iterator();
                for (int i=0; i<indexInList; i++) {
                    it.next();
                }
                return it.next();
            } catch (IndexOutOfBoundsException | NoSuchElementException e) {
                throw new ConcurrentModificationException(e);
            }
        }
    }

    /**
     * Returns the parent node, or {@code null} if this node is the root of the tree.
     */
    @Override
    public final TreeTable.Node getParent() {
        return parent;
    }

    /**
     * Returns {@code false} if the value is a metadata object (and consequently can have children),
     * or {@code true} if the value is not a metadata object.
     */
    @Override
    public final boolean isLeaf() {
        return !table.standard.isMetadata(getElementType());
    }

    /**
     * Returns the children of this node, or an empty list if none.
     * Only metadata object can have children.
     */
    @Override
    public final List<TreeTable.Node> getChildren() {
        /*
         * 'children' is set to EMPTY_LIST if an only if the node *can not* have children,
         * in which case we do not need to check for changes in the underlying metadata.
         */
        if (children != Collections.EMPTY_LIST) {
            final Object value = getUserObject();
            if (value == null) {
                /*
                 * If there is no value, returns an empty list but *do not* set 'children'
                 * to that list, in order to allow this method to check again the next time
                 * that this method is invoked.
                 */
                children = null; // Let GC do its work.
                return Collections.emptyList();
            }
            /*
             * If there is a value, check if the cached list is still applicable.
             */
            if (children instanceof MetadataTreeChildren) {
                final MetadataTreeChildren candidate = (MetadataTreeChildren) children;
                if (candidate.metadata == value) {
                    return candidate;
                }
            }
            /*
             * At this point, we need to create a new list. The property accessor will be
             * null if the value is not a metadata object, in which case we will remember
             * that fact by setting the children list definitively to an empty list.
             */
            final PropertyAccessor accessor = table.standard.getAccessor(value.getClass(), false);
            if (accessor != null) {
                children = new MetadataTreeChildren(this, value, accessor);
            } else {
                children = Collections.emptyList();
            }
        }
        return children;
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException}, because there is no
     * way we can safely determine which metadata property a new child would be for.
     */
    @Override
    public final TreeTable.Node newChild() {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "newChild"));
    }

    /**
     * Returns the value of this node in the given column, or {@code null} if none. This method verifies
     * the {@code column} argument, then delegates to {@link #getName()}, {@link #getElementType()} or
     * {@link #getUserObject()}.
     */
    @Override
    public final <V> V getValue(final TableColumn<V> column) {
        ArgumentChecks.ensureNonNull("column", column);
        Object value = null;
        if (column == TableColumn.NAME) {
            value = name;
            if (value == null) {
                value = name = getName();
            }
        } else if (column == TableColumn.VALUE) {
            value = getUserObject();
        } else if (column == TableColumn.TYPE) {
            value = getElementType();
        }
        return column.getElementType().cast(value);
    }

    /**
     * Sets the value if the given column is {@link TableColumn#VALUE}. This method verifies
     * the {@code column} argument, then delegates to {@link #setUserObject(Object)}.
     */
    @Override
    public final <V> void setValue(final TableColumn<V> column, final V value) throws UnsupportedOperationException {
        ArgumentChecks.ensureNonNull("column", column);
        if (column == TableColumn.VALUE) {
            setUserObject(value);
        } else if (MetadataTreeTable.COLUMNS.contains(column)) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableCellValue_2,
                    getValue(TableColumn.NAME), column.getHeader()));
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "column", column));
        }
    }

    /**
     * Returns {@code true} if the given column is {@link TableColumn#VALUE} and the property is writable,
     * or {@code false} in all other cases. This method verifies the {@code column} argument, then delegates
     * to {@link #isWritable()}.
     */
    @Override
    public boolean isEditable(final TableColumn<?> column) {
        ArgumentChecks.ensureNonNull("column", column);
        return (column == TableColumn.VALUE) && isWritable();
    }

    /**
     * Returns a string representation of this node for debugging purpose.
     */
    @Debug
    @Override
    public final String toString() {
        final StringBuilder buffer = new StringBuilder(60);
        toString(buffer);
        return buffer.toString();
    }

    /**
     * Implementation of {@link #toString()} appending the string representation
     * in the given buffer.
     */
    final void toString(final StringBuilder buffer) {
        identifier(buffer.append("Node["));
        buffer.append(" : ").append(Classes.getShortName(getElementType())).append(']');
    }
}
