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
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable.Node;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A node in a {@link TreeTableView} view. The {@code TreeTableView} class is used directly
 * only for the root node, or for nodes containing a fixed value instead than a value fetched from
 * the metadata object. For all other nodes, the actual node class shall be either {@link Element}
 * or {@link CollectionElement}.
 *
 * <p>The value of a node is extracted from the {@linkplain #metadata} object by {@link #getUserObject()}.
 * For each instance of {@code TreeTableView}, that value is always a singleton, never a collection.
 * If a metadata property is a collection, then there is an instance of the {@link CollectionElement}
 * subclass for each element in the collection.</p>
 *
 * <p>The {@link #newChild()} operation is supported if the node is not a leaf. The user shall
 * set the identifier and the value, in that order, before any other operation on the new child.
 * See {@code newChild()} javadoc for an example.</p>
 *
 * <div class="note"><b>API note:</b>
 * This class is not serializable because the values of the {@link #indexInData} and {@link #indexInList} fields
 * may not be stable. The former may be invalid if the node is serialized and deserialized by two different versions
 * of Apache SIS having properties in different order. The second may be invalid if the collection is not guaranteed
 * to preserve order on serialization (e.g. {@code CodeListSet} with user-supplied elements, in which case the elements
 * order depends on the instantiation order).</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class TreeNode implements Node {
    /**
     * The collection of {@linkplain #children} to return when the node does not allow children
     * (i.e. is a leaf). This constant is also used as a sentinel value by {@link #isLeaf()}.
     *
     * <p>We choose an empty set instead than an empty list because {@link TreeNodeChildren}
     * does not implement the {@link List} interface. So we are better to never give to the user
     * a collection implementing {@code List} in order to signal incorrect casts sooner.</p>
     */
    private static final Collection<Node> LEAF = Collections.emptySet();

    /**
     * The table for which this node is an element. Contains information like
     * the metadata standard and the value existence policy.
     *
     * <p>All {@code TreeNode} instances in the same tree have
     * a reference to the same {@code TreeTableView} instance.</p>
     */
    final TreeTableView table;

    /**
     * The parent of this node to be returned by {@link #getParent()},
     * or {@code null} if this node is the root of the tree.
     *
     * @see #getParent()
     */
    private final TreeNode parent;

    /**
     * The metadata object from which the {@link #getUserObject()} method will fetch the value.
     * The value is fetched in different ways, which depend on the {@code TreeNode} subclass:
     *
     * <ul>
     *   <li>For {@code TreeNode} (the root of the tree),
     *       the value is directly {@link #metadata}.</li>
     *   <li>For {@link Element} (a metadata property which is not a collection),
     *       the value is {@code accessor.get(indexInData, metadata)}.</li>
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
    private transient CharSequence name;

    /**
     * The children of this node, or {@code null} if not yet computed. If and only if the node
     * can not have children (i.e. {@linkplain #isLeaf() is a leaf}), then this field is set to
     * {@link #LEAF}.
     *
     * @see #getChildren()
     */
    private transient Collection<Node> children;

    /**
     * The value which existed when the {@link TreeNodeChildren#iterator()} traversed this node.
     * This value is cached on the assumption that users will ask for value or for children soon
     * after they iterated over this node. The cached value is cleared after its first use.
     *
     * <p>This value shall be either {@code null}, or the exact same value than what a call to
     * {@link #getUserObject()} would return, assuming that the underlying {@linkplain #metadata}
     * object didn't changed.</p>
     *
     * <p>The purpose of this cache is to avoid invoking (by reflection) the same getter methods
     * twice in common situations like the {@link TreeTableView#toString()} implementation or in
     * Graphical User Interface. However we may remove this field in any future SIS version if
     * experience shows that it is more problematic than helpful.</p>
     */
    transient Object cachedValue;

    /**
     * Creates the root node of a new metadata tree table.
     *
     * @param  table    The table which is creating this root node.
     * @param  metadata The root metadata object (can not be null).
     */
    TreeNode(final TreeTableView table, final Object metadata) {
        this.table    = table;
        this.parent   = null;
        this.metadata = metadata;
    }

    /**
     * Creates a new child for an element of the given metadata.
     * This constructor is for the {@link Element} subclass only.
     * Callers must invoke {@link #init()} after construction.
     *
     * @param  parent   The parent of this node.
     * @param  metadata The metadata object for which this node will be a value.
     */
    TreeNode(final TreeNode parent, final Object metadata) {
        this.table    = parent.table;
        this.parent   = parent;
        this.metadata = metadata;
    }

    /**
     * Must be invoked after construction. The work performed by this method can not be done
     * in the {@code TreeNode} constructor, because it needs the subclasses to finish their
     * construction first.
     */
    final void init() {
        if (!table.standard.isMetadata(getElementType())) {
            children = LEAF;
        }
    }

    /**
     * Returns the UML identifier defined by the standard. The default implementation is suitable
     * only for the root node, since it returns the class identifier. Subclasses must override in
     * order to return the property identifier instead.
     */
    String getIdentifier() {
        final Class<?> type = table.standard.getInterface(metadata.getClass());
        final String id = Types.getStandardName(type);
        return (id != null) ? id : Classes.getShortName(type);
    }

    /**
     * Returns the index in the collection if the metadata property type is a collection,
     * or {@code null} otherwise. The (<var>identifier</var>, <var>index</var>) pair can
     * be used as a primary key for identifying this node among its siblings.
     */
    Integer getIndex() {
        return null;
    }

    /**
     * Gets the human-readable name of this node. The name shall be stable, since it will be cached
     * by the caller. The name typically contains {@linkplain #getIdentifier() identifier} and
     * {@linkplain #getIndex() index} information, eventually localized.
     *
     * <p>The default implementation is suitable only for the root node - subclasses must override.</p>
     */
    CharSequence getName() {
        return CharSequences.camelCaseToSentence(Classes.getShortName(
                table.standard.getInterface(metadata.getClass()))).toString();
    }

    /**
     * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
     * The appended value is similar to the value returned by {@link #getIdentifier()} (except for the
     * root node), but may contains additional information like the index in a collection.
     *
     * <p>The default implementation is suitable only for the root node - subclasses must override.</p>
     */
    void appendIdentifier(final StringBuilder buffer) {
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
        throw new UnsupportedOperationException(unmodifiableCellValue(TableColumn.VALUE));
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
     * objects still the metadata object, which allow {@link TreeTableView} to be a dynamic view.
     *
     * <p>Instances of this class shall be instantiated only for metadata singletons. If a metadata
     * property is a collection, then the {@link CollectionElement} subclass shall be instantiated
     * instead.</p>
     */
    static class Element extends TreeNode {
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
         * Callers must invoke {@link #init()} after construction.
         *
         * @param  parent      The parent of this node.
         * @param  metadata    The metadata object for which this node will be a value.
         * @param  accessor    Accessor to use for fetching the name, type and value.
         * @param  indexInData Index to be given to the accessor of fetching the value.
         */
        Element(final TreeNode parent, final Object metadata,
                final PropertyAccessor accessor, final int indexInData)
        {
            super(parent, metadata);
            this.accessor = accessor;
            this.indexInData = indexInData;
        }

        /**
         * The property identifier to be returned in the {@link TableColumn#IDENTIFIER} cells.
         */
        @Override
        final String getIdentifier() {
            return accessor.name(indexInData, KeyNamePolicy.UML_IDENTIFIER);
        }

        /**
         * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
         */
        @Override
        void appendIdentifier(final StringBuilder buffer) {
            super.appendIdentifier(buffer);
            buffer.append('.').append(accessor.name(indexInData, KeyNamePolicy.JAVABEANS_PROPERTY));
        }

        /**
         * Gets the name of this node. Current implementation derives the name from the
         * {@link KeyNamePolicy#UML_IDENTIFIER} instead than {@link KeyNamePolicy#JAVABEANS_PROPERTY}
         * in order to get the singular form instead of the plural one, because we will create one
         * node for each element in a collection.
         */
        @Override
        CharSequence getName() {
            return CharSequences.camelCaseToSentence(getIdentifier()).toString();
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
         * Sets the property value for this node.
         */
        @Override
        void setUserObject(final Object value) {
            accessor.set(indexInData, metadata, value, PropertyAccessor.RETURN_NULL);
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
         * Index of the element in the collection, in iteration order.
         */
        final int indexInList;

        /**
         * Creates a new node for the given collection element.
         * Callers must invoke {@link #init()} after construction.
         *
         * @param  parent      The parent of this node.
         * @param  metadata    The metadata object for which this node will be a value.
         * @param  accessor    Accessor to use for fetching the name, type and collection.
         * @param  indexInData Index to be given to the accessor of fetching the collection.
         * @param  indexInList Index of the element in the collection, in iteration order.
         */
        CollectionElement(final TreeNode parent, final Object metadata,
                final PropertyAccessor accessor, final int indexInData, final int indexInList)
        {
            super(parent, metadata, accessor, indexInData);
            this.indexInList = indexInList;
        }

        /**
         * Appends an identifier for this node in the given buffer, for {@link #toString()} implementation.
         */
        @Override
        void appendIdentifier(final StringBuilder buffer) {
            super.appendIdentifier(buffer);
            buffer.append('[').append(indexInList).append(']');
        }

        /**
         * Returns the zero-based index of this node in the metadata property.
         */
        @Override
        Integer getIndex() {
            return indexInList;
        }

        /**
         * Appends the index of this property, if there is more than one.
         * Index numbering begins at 1, since this name if for human reading.
         */
        @Override
        CharSequence getName() {
            CharSequence name = super.getName();
            final Collection<?> values = (Collection<?>) super.getUserObject();
            if (values != null) {
                final int size = values.size();
                if (size >= 2) {
                    name = Vocabulary.formatInternational(Vocabulary.Keys.Of_3, name, indexInList+1, size);
                }
            }
            return name;
        }

        /**
         * Fetches the property value from the metadata object, which is expected to be a collection,
         * then fetch the element at the index represented by this node.
         */
        @Override
        public Object getUserObject() {
            final Collection<?> values = (Collection<?>) super.getUserObject();
            /*
             * If the collection is null or empty but the value existence policy tells
             * us that such elements shall be shown, behave as if the collection was a
             * singleton containing a null element, in order to make the property
             * visible in the tree.
             */
            if (indexInList == 0 && table.valuePolicy.substituteByNullElement(values)) {
                return null;
            }
            try {
                if (values instanceof List<?>) {
                    return ((List<?>) values).get(indexInList);
                }
                final Iterator<?> it = values.iterator();
                for (int i=0; i<indexInList; i++) {
                    it.next(); // Inefficient way to move at the desired index, but hopefully rare.
                }
                return it.next();
            } catch (RuntimeException e) { // (NullPointerException | IndexOutOfBoundsException | NoSuchElementException) on JDK7.
                /*
                 * May happen if the collection for this metadata property changed after the iteration
                 * in the TreeNodeChildren. Users should not keep TreeNode references instances for a
                 * long time, but instead iterate again over TreeNodeChildren when needed.
                 */
                throw (ConcurrentModificationException) new ConcurrentModificationException().initCause(e);
            }
        }

        /**
         * Sets the property value for this node.
         */
        @Override
        void setUserObject(Object value) {
            final Collection<?> values = (Collection<?>) super.getUserObject();
            if (!(values instanceof List<?>)) {
                // 'setValue' is the public method which invoked this one.
                throw new UnsupportedOperationException(Errors.format(
                        Errors.Keys.UnsupportedOperation_1, "setValue"));
            }
            final Class<?> targetType;
            if (values instanceof CheckedContainer<?>) {
                // Typically the same than getElementType(), but let be safe
                // in case some implementations have stricter requirements.
                targetType = ((CheckedContainer<?>) values).getElementType();
            } else {
                targetType = getElementType();
            }
            value = ObjectConverters.convert(value, targetType);
            try {
                /*
                 * Unsafe addition into a collection. In SIS implementation, the collection is
                 * actually an instance of CheckedCollection, so the check will be performed at
                 * runtime. However other implementations could use unchecked collection. We have
                 * done our best for converting the type above, there is not much more we can do...
                 */
                // No @SuppressWarnings because this is a real hole.
                ((List) values).set(indexInList, value);
            } catch (IndexOutOfBoundsException e) {
                // Same rational than in the getUserObject() method.
                throw (ConcurrentModificationException) new ConcurrentModificationException().initCause(e);
            }
        }
    }


    // -------- Final methods (defined in terms of above methods only) ----------------------------


    /**
     * Returns the parent node, or {@code null} if this node is the root of the tree.
     */
    @Override
    public final Node getParent() {
        return parent;
    }

    /**
     * Returns {@code false} if the value is a metadata object (and consequently can have children),
     * or {@code true} if the value is not a metadata object.
     */
    @Override
    public final boolean isLeaf() {
        return (children == LEAF);
    }

    /**
     * Returns the children of this node, or an empty set if none.
     * Only metadata object can have children.
     */
    @Override
    public final Collection<Node> getChildren() {
        /*
         * 'children' is set to LEAF if an only if the node *can not* have children,
         * in which case we do not need to check for changes in the underlying metadata.
         */
        if (!isLeaf()) {
            Object value = cachedValue;
            if (value == null) {
                value = getUserObject();
                if (value == null) {
                    /*
                     * If there is no value, returns an empty set but *do not* set 'children'
                     * to that set, in order to allow this method to check again the next time
                     * that this method is invoked.
                     */
                    children = null; // Let GC do its work.
                    return LEAF;
                }
            }
            cachedValue = null; // Use the cached value only once after iteration.
            /*
             * If there is a value, check if the cached collection is still applicable.
             */
            if (children instanceof TreeNodeChildren) {
                final TreeNodeChildren candidate = (TreeNodeChildren) children;
                if (candidate.metadata == value) {
                    return candidate;
                }
            }
            /*
             * At this point, we need to create a new collection. The property accessor shall
             * exist, otherwise the call to 'isLeaf()' above would have returned 'true'.
             */
            children = new TreeNodeChildren(this, value, table.standard.getAccessor(value.getClass(), true));
        }
        return children;
    }

    /**
     * Returns a proxy for a new property to be defined in the metadata object.
     * The user shall set the identifier and the value, in that order, before
     * any other operation on the new child. Example:
     *
     * {@preformat java
     *     TreeTable.Node node = ...;
     *     TreeTable.Node child = node.newChild();
     *     child.setValue(TableColumn.IDENTIFIER, "title");
     *     child.setValue(TableColumn.VALUE, "Le petit prince");
     *     // Nothing else to do - node has been added.
     * }
     *
     * Do not keep a reference to the returned node for a long time, since it is only
     * a proxy toward the real node to be created once the identifier is known.
     *
     * @throws UnsupportedOperationException If this node {@linkplain #isLeaf() is a leaf}.
     */
    @Override
    public final Node newChild() throws UnsupportedOperationException {
        if (isLeaf()) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.NodeIsLeaf_1, this));
        }
        return new NewChild();
    }

    /**
     * The proxy to be returned by {@link TreeNode#newChild()}.
     * User shall not keep a reference to this proxy for a long time.
     */
    private final class NewChild implements Node {
        /**
         * Index in the {@link PropertyAccessor} for the property to be set.
         * This index is known only after a value has been specified for the
         * {@link TableColumn#IDENTIFIER}.
         */
        private int indexInData = -1;

        /**
         * The real node created after the identifier and the value have been specified.
         * All operations will be delegated to that node after it has been determined.
         */
        private TreeNode delegate;

        /**
         * Returns the {@link #delegate} node if non-null, or throw an exception otherwise.
         *
         * @throws IllegalStateException if the identifier and value columns have not yet been defined.
         */
        private TreeNode delegate() throws IllegalStateException {
            if (delegate != null) {
                return delegate;
            }
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueInColumn_1,
                    (indexInData < 0 ? TableColumn.IDENTIFIER : TableColumn.VALUE).getHeader()));
        }

        /**
         * Returns all children of the parent node. The new child will be added to that list.
         */
        private TreeNodeChildren getSiblings() {
            return (TreeNodeChildren) TreeNode.this.getChildren();
        }

        /**
         * If the {@link #delegate} is not yet known, set the identifier or the value.
         * After the identifier and value have been specified, delegates to the real node.
         */
        @Override
        public <V> void setValue(final TableColumn<V> column, final V value) {
            if (delegate == null) {
                /*
                 * For the given identifier, get the index in the property accessor.
                 * This can be done only before the 'delegate' is found - after that
                 * point, the identifier will become unmodifiable.
                 */
                if (column == TableColumn.IDENTIFIER) {
                    ArgumentChecks.ensureNonNull("value", value);
                    indexInData = getSiblings().accessor.indexOf((String) value, true);
                    return;
                }
                /*
                 * Set the value for the property specified by the above identifier,
                 * then get the 'delegate' on the assumption that the new value will
                 * be added at the end of collection (if the property is a collection).
                 */
                if (column == TableColumn.VALUE) {
                    ArgumentChecks.ensureNonNull("value", value);
                    if (indexInData < 0) {
                        throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueInColumn_1,
                                TableColumn.IDENTIFIER.getHeader()));
                    }
                    final TreeNodeChildren siblings = getSiblings();
                    final int indexInList;
                    if (siblings.isCollection(indexInData)) {
                        indexInList = ((Collection<?>) siblings.valueAt(indexInData)).size();
                    } else {
                        indexInList = -1;
                    }
                    if (!siblings.add(indexInData, value)) {
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, value));
                    }
                    delegate = siblings.childAt(indexInData, indexInList);
                    // Do not set 'delegate.cachedValue = value', since 'value' may
                    // have been converted by the setter method to an other value.
                    return;
                }
            }
            delegate().setValue(column, value);
        }

        /**
         * For all operations other than {@code setValue(…)}, delegates to the {@link #delegate} node
         * or to some code functionally equivalent.
         *
         * @throws IllegalStateException if the identifier and value columns have not yet been defined.
         */
        @Override public Node             getParent()                       {return TreeNode.this;}
        @Override public boolean          isLeaf()                          {return delegate().isLeaf();}
        @Override public Collection<Node> getChildren()                     {return delegate().getChildren();}
        @Override public Node             newChild()                        {return delegate().newChild();}
        @Override public <V> V            getValue(TableColumn<V> column)   {return delegate().getValue(column);}
        @Override public boolean          isEditable(TableColumn<?> column) {return delegate().isEditable(column);}
        @Override public Object           getUserObject()                   {return delegate().getUserObject();}
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

        // Check the columns in what we think may be the most frequently
        // asked columns first, and less frequently asked columns last.
        if (column == TableColumn.VALUE) {
            if (isLeaf()) {
                value = cachedValue;
                cachedValue = null; // Use the cached value only once after iteration.
                if (value == null) {
                    value = getUserObject();
                }
            }
        } else if (column == TableColumn.NAME) {
            if (name == null) {
                name = getName();
            }
            value = name;
        } else if (column == TableColumn.IDENTIFIER) {
            value = getIdentifier();
        } else if (column == TableColumn.INDEX) {
            value = getIndex();
        } else if (column == TableColumn.TYPE) {
            value = getElementType();
        }
        return column.getElementType().cast(value);
    }

    /**
     * Sets the value if the given column is {@link TableColumn#VALUE}. This method verifies
     * the {@code column} argument, then delegates to {@link #setUserObject(Object)}.
     *
     * <p>This method does not accept null value, because setting a singleton property to null
     * with {@link ValueExistencePolicy#NON_EMPTY} is equivalent to removing the property, and
     * setting a collection element to null is not allowed. Those various behavior are at risk
     * of causing confusion, so we are better to never allow null.</p>
     */
    @Override
    public final <V> void setValue(final TableColumn<V> column, final V value) throws UnsupportedOperationException {
        ArgumentChecks.ensureNonNull("column", column);
        if (column == TableColumn.VALUE) {
            ArgumentChecks.ensureNonNull("value", value);
            cachedValue = null;
            setUserObject(value);
        } else if (TreeTableView.COLUMNS.contains(column)) {
            throw new UnsupportedOperationException(unmodifiableCellValue(column));
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "column", column));
        }
    }

    /**
     * Returns the error message for an unmodifiable cell value in the given column.
     */
    private String unmodifiableCellValue(final TableColumn<?> column) {
        return Errors.format(Errors.Keys.UnmodifiableCellValue_2, getValue(TableColumn.NAME), column.getHeader());
    }

    /**
     * Returns {@code true} if the given column is {@link TableColumn#VALUE} and the property is writable,
     * or {@code false} in all other cases. This method verifies the {@code column} argument, then delegates
     * to {@link #isWritable()}.
     */
    @Override
    public final boolean isEditable(final TableColumn<?> column) {
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
        appendIdentifier(buffer.append("Node["));
        buffer.append(" : ").append(Classes.getShortName(getElementType())).append(']');
    }
}
