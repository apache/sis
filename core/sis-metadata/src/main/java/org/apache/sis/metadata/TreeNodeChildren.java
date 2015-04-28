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

import java.util.Iterator;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * The collection of children to be returned by {@link TreeNode#getChildren()}.
 * This collection holds a reference to the metadata object at creation time;
 * it does not track changes in {@code parent.getUserObject()}.
 *
 * <div class="section">Note on value existence policy</div>
 * It is better to use this class with {@link ValueExistencePolicy#NON_EMPTY} in order
 * to avoid code complication and surprising behavior of {@link Iter#remove()} operation.
 * If the policy is set to another value, we need to keep the following aspects in mind:
 *
 * <ul>
 *   <li>When {@link Iter#hasNext()} finds a null or empty collection,
 *       it may needs to simulate a singleton with a null value.</li>
 *   <li>In {@link TreeNode#getUserObject()}, we need the same check than above
 *       for simulating a singleton collection with a null value if the node is
 *       for the element at index 0.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class TreeNodeChildren extends AbstractCollection<TreeTable.Node> {
    /**
     * The parent of the children to be returned by the iterator.
     * Some useful information are available indirectly through this parent:
     *
     * <ul>
     *   <li>{@link ValueExistencePolicy}: {@code parent.table.valuePolicy}</li>
     * </ul>
     *
     * @see #childAt(int)
     */
    private final TreeNode parent;

    /**
     * The metadata object for which property values will be the elements of this collection.
     * This is typically an {@link AbstractMetadata} instance, but not necessarily.
     * Any type for which {@link MetadataStandard#isMetadata(Class)} returns {@code true} is okay.
     *
     * <p>This field is a snapshot of the {@linkplain #parent} {@link TreeNode#getUserObject()} at
     * creation time. This collection does not track changes in the reference returned by the above-cited
     * {@code getUserObject()}. In other words, changes in the {@code metadata} object will be reflected
     * in this collection, but if {@code parent.getUserObject()} returns a reference to another object,
     * this change will not be reflected in this collection.
     */
    final Object metadata;

    /**
     * The accessor to use for accessing the property names, types and values of the
     * {@link #metadata} object. This is given at construction time and shall be the
     * same than the following code:
     *
     * {@preformat java
     *     accessor = parent.table.standard.getAccessor(metadata.getClass(), true);
     * }
     */
    final PropertyAccessor accessor;

    /**
     * The children to be returned by this collection. All elements in this collection are
     * initially {@code null}, then created by {@link #childAt(int)} when first needed.
     *
     * <p>Not all elements in this array will be returned by the iterator.
     * The value needs to be verified for the {@link ValueExistencePolicy}.</p>
     */
    private final TreeNode[] children;

    /**
     * Modification count, incremented when the content of this collection is modified. This check
     * is done on a <cite>best effort basis</cite> only, since we can't not track the changes which
     * are done independently in the {@linkplain #metadata} object.
     */
    int modCount;

    /**
     * Creates a collection of children for the specified metadata.
     *
     * @param parent   The parent for which this node is an element.
     * @param metadata The metadata object for which property values will be the elements of this collection.
     * @param accessor The accessor to use for accessing the property names, types and values of the metadata object.
     */
    TreeNodeChildren(final TreeNode parent, final Object metadata, final PropertyAccessor accessor) {
        this.parent   = parent;
        this.metadata = metadata;
        this.accessor = accessor;
        children = new TreeNode[accessor.count()];
    }

    /**
     * Clears the value at the given index. The given {@code index} is relative to
     * the {@link #accessor} indexing, <strong>not</strong> to this collection.
     *
     * <p>The cleared elements may or may not be considered as removed, depending on the
     * value policy. To check if the element shall be considered as removed (for example
     * in order to update index), invoke {@code isSkipped(value)} after this method.</p>
     *
     * <div class="section">Implementation note</div>
     * This method sets the property to {@code null}. This is not strictly correct for collections,
     * since we should rather set the property to an empty collection. However this approach would
     * force us to check if the expected collection type is actually a list, a set or any other type.
     * Passing null avoid the type check and is safe at least with SIS implementation. We may revisit
     * later if this appears to be a problem with other implementations.
     *
     * @param index The index in the accessor (<em>not</em> the index in this collection).
     */
    final void clearAt(final int index) {
        accessor.set(index, metadata, null, PropertyAccessor.RETURN_NULL);
    }

    /**
     * Returns the value at the given index. The given {@code index} is relative to
     * the {@link #accessor} indexing, <strong>not</strong> to this collection.
     *
     * @param  index The index in the accessor (<em>not</em> the index in this collection).
     * @return The value at the given index. May be {@code null} or a collection.
     */
    final Object valueAt(final int index) {
        return accessor.get(index, metadata);
    }

    /**
     * Returns {@code true} if the type at the given index is a collection. The given
     * {@code index} is relative to the {@link #accessor} indexing, <strong>not</strong>
     * to this collection.
     *
     * <div class="note"><b>Implementation note:</b>
     * We do not test {@code (value instanceof Collection)} because the value could be any user's implementation.
     * Nothing prevent users from implementing the collection interface even for singleton elements if they wish.</div>
     *
     * @param  index The index in the accessor (<em>not</em> the index in this collection).
     * @return {@code true} if the value at the given index is a collection.
     */
    final boolean isCollection(final int index) {
        return accessor.isCollection(index);
    }

    /**
     * Returns {@code true} if the give value shall be skipped by the iterators,
     * according the value policy.
     *
     * @param  value The value to test.
     * @return {@code true} if the given value shall be skipped by the iterators.
     */
    final boolean isSkipped(final Object value) {
        return parent.table.valuePolicy.isSkipped(value);
    }

    /**
     * Returns the child at the given index, creating it if needed. The given {@code index}
     * is relative to the {@link #accessor} indexing, <strong>not</strong> to this collection.
     *
     * <p>This method does not check if the child at the given index should be skipped.
     * It is caller responsibility to do such verification before this method call.</p>
     *
     * @param  index The index in the accessor (<em>not</em> the index in this collection).
     * @param  subIndex If the property at {@link #index} is a collection, the index in that
     *         collection (<em>not</em> the index in <em>this</em> collection). Otherwise -1.
     * @return The node to be returned by public API.
     */
    final TreeNode childAt(final int index, final int subIndex) {
        TreeNode node = children[index];
        if (subIndex >= 0) {
            /*
             * If the value is an element of a collection, we will cache only the last used value.
             * We don't cache all elements in order to avoid yet more complex code, and this cover
             * the majority of cases where the collection has only one element anyway.
             */
            if (node == null || ((TreeNode.CollectionElement) node).indexInList != subIndex) {
                node = new TreeNode.CollectionElement(parent, metadata, accessor, index, subIndex);
                node.init();
            }
        } else {
            /*
             * If the property is a singleton (not an element of a collection), returns a more
             * dynamic node which will fetch the value from the metadata object. This allows
             * the node to reflect changes in the metadata object, and conversely.
             */
            if (node == null) {
                node = new TreeNode.Element(parent, metadata, accessor, index);
                node.init();
            }
        }
        children[index] = node;
        return node;
    }

    /**
     * Returns the maximal number of children. This is the number of all possible elements
     * according the {@link #accessor}, including {@linkplain #isSkipped(Object) skipped}
     * ones. This is <strong>not</strong> the collection size.
     */
    final int childCount() {
        return children.length;
    }

    /**
     * Returns the number of elements in this collection,
     * ignoring the {@link #isSkipped(Object) skipped} ones.
     */
    @Override
    public int size() {
        return accessor.count(metadata, parent.table.valuePolicy, PropertyAccessor.COUNT_DEEP);
    }

    /**
     * Returns {@code true} if this collection contains no elements. Invoking this method is more efficient
     * than testing {@code size() == 0} because this method does not iterate over all properties.
     */
    @Override
    public boolean isEmpty() {
        return accessor.count(metadata, parent.table.valuePolicy, PropertyAccessor.COUNT_FIRST) == 0;
    }

    /**
     * Clears all properties in the metadata object. Note that this collection will effectively
     * by empty after this method call only if the value existence policy is {@code NON_EMPTY},
     * which is the default.
     */
    @Override
    public void clear() {
        for (int i=childCount(); --i>=0;) {
            clearAt(i);
        }
    }

    /**
     * Returns an iterator over the nodes in the collection of children.
     */
    @Override
    public Iterator<TreeTable.Node> iterator() {
        return new Iter();
    }

    /**
     * The iterator over the elements in the enclosing {@link TreeNodeChildren} collection.
     * Each element is identified by its index in the {@link PropertyAccessor}, together with
     * its position in its sub-iterator when the metadata property is a collection.
     *
     * <div class="section">Implementation note</div>
     * It could be cheaper to not take an iterator for the properties that are collections,
     * and instead just increment a "sub-index" from 0 to the collection size.  It would be
     * cheaper because we don't really need to extract the values of those collections (i.e.
     * the {@link #nextValue} field is not really needed). Nevertheless we prefer (for now)
     * the iterator approach anyway because it makes easier to implement the {@link #remove()}
     * method and has the desired side-effect to check for concurrent modifications. It also
     * keeps the {@link #nextValue} field up-to-date in case we would like to use it in a
     * future SIS version. We do that on the assumption that sub-iterators are cheap since
     * they are {@code ArrayList} iterators in the majority of cases.
     */
    private final class Iter implements Iterator<TreeTable.Node> {
        /**
         * Index in {@link TreeNodeChildren#accessor} of the next element to be
         * returned by {@link #next()}, or {@link PropertyAccessor#count()} if
         * we have reached the end of the list.
         */
        private int nextInAccessor;

        /**
         * Index in {@link TreeNodeChildren#accessor} of the element returned by
         * the last call to {@link #next()}, or -1 if none.
         */
        private int previousInAccessor = -1;

        /**
         * {@code true} if we have verified the value at {@link #nextInAccessor} index
         * for non-null or non-empty element.
         */
        private boolean isNextVerified;

        /**
         * The value of the node to be returned by the {@link #next()} method. This value is computed
         * ahead of time by {@link #hasNext()} since we need that information in order to determine
         * if the value needs to be skipped or not.
         *
         * <div class="note"><b>Note:</b>
         * Actually we don't really need to keep this value, since it is not used outside the {@link #hasNext()}
         * method. But we keep it for allowing the {@link #next()} method to opportunistically update the
         * {@link TreeNode#cachedValue} field.</div>
         */
        private Object nextValue;

        /**
         * If the call to {@link #next()} found a collection, the iterator over the elements
         * in that collection. Otherwise {@code null}.
         *
         * <p>A non-null value (even if that sub-iterator has no next elements)
         * means that {@link #nextValue} is an element of that sub-iteration.</p>
         */
        private Iterator<?> subIterator;

        /**
         * Position of the {@link #nextValue} in the {@link #subIterator},
         * or -1 if the sub-iterator is null.
         */
        private int subIndex = -1;

        /**
         * The value of {@link TreeNodeChildren#modCount} at construction time or after
         * the last change done by this iterator. Used for concurrent modification checks.
         *
         * <div class="note"><b>Note:</b>
         * Actually this iterator should be robust to most concurrent modifications.
         * But we check anyway in order to prevent concurrent modifications in user
         * code, in case a future SIS version become more sensitive to such changes.</div>
         */
        private int modCountCheck;

        /**
         * Creates a new iterator.
         */
        Iter() {
            modCountCheck = modCount;
        }

        /**
         * Throws {@link ConcurrentModificationException} if an unexpected change has been detected.
         */
        final void checkConcurrentModification() {
            if (modCountCheck != modCount) {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * Ensures that {@link #nextInAccessor} is valid. If the index has not been validated, then this method
         * moves the iterator to the next valid element, starting at the current {@link #nextInAccessor} value.
         *
         * @return {@code true} on success, or {@code false} if we reached the end of the iteration.
         */
        @Override
        public boolean hasNext() {
            checkConcurrentModification();
            if (isNextVerified) {
                return true;
            }
            /*
             * If we were iterating over the elements of a sub-collection, move to the next element
             * in that iteration. We do not check for 'isSkipped(value)' here because null or empty
             * elements in collections are probably mistakes, and we want to see them.
             */
            if (subIterator != null) {
                if (subIterator.hasNext()) {
                    nextValue = subIterator.next();
                    subIndex++;
                    isNextVerified = true;
                    return true;
                }
                subIterator = null;
                subIndex = -1;
                nextInAccessor++; // See the comment before nextInAccessor++ in the next() method.
            }
            /*
             * Search for the next property, which may be either a singleton or the first element
             * of a sub-collection. In the later case, we will create a sub-iterator.
             */
            final int count = childCount();
            while (nextInAccessor < count) {
                nextValue = valueAt(nextInAccessor);
                if (!isSkipped(nextValue)) {
                    if (isCollection(nextInAccessor)) {
                        /*
                         * If the property is a collection, unconditionally get the first element
                         * even if absent (null) in order to comply with the ValueExistencePolicy.
                         * if we were expected to ignore empty collections, 'isSkipped(nextValue)'
                         * would have returned 'true'.
                         */
                        if (nextValue != null) {
                            subIterator = ((Iterable<?>) nextValue).iterator();
                        } else {
                            subIterator = Collections.emptySet().iterator();
                            // Null collections are illegal (it shall be empty collections instead),
                            // but we try to keep the iterator robut to ill-formed metadata, because
                            // we want AbstractMetadata.toString() to work so we can spot problems.
                        }
                        subIndex = 0;
                        if (subIterator.hasNext()) {
                            nextValue = subIterator.next();
                        } else {
                            nextValue = null;
                            // Do not set 'childIterator' to null, since the above 'nextValue'
                            // is considered as part of the child iteration.
                        }
                    }
                    isNextVerified = true;
                    return true;
                }
                nextInAccessor++;
            }
            return false;
        }

        /**
         * Returns the node for the metadata property at the current {@link #nextInAccessor}.
         * The value of this property is initially {@link #nextValue}, but this may change at
         * any time if the user modify the underlying metadata object.
         */
        @Override
        public TreeTable.Node next() {
            if (hasNext()) {
                final TreeNode node = childAt(nextInAccessor, subIndex);
                node.cachedValue = nextValue;
                previousInAccessor = nextInAccessor;
                if (subIterator == null) {
                    /*
                     * If we are iterating over the elements in a collection, the PropertyAccessor index
                     * still the same and will be incremented by 'hasNext()' only when the iteration is
                     * over. Otherwise (not iterating in a collection), move to the next property. The
                     * 'hasNext()' method will determine later if this property is non-empty, or if we
                     * need to move forward again.
                     */
                    nextInAccessor++;
                }
                isNextVerified = false;
                return node;
            }
            throw new NoSuchElementException();
        }

        /**
         * Clears the element returned by the last call to {@link #next()}.
         * Whether the cleared element is considered removed or not depends
         * on the value policy and on the element type. With the default
         * {@code NON_EMPTY} policy, the effect is a removal.
         */
        @Override
        public void remove() {
            if (previousInAccessor < 0) {
                throw new IllegalStateException();
            }
            checkConcurrentModification();
            if (subIterator != null) {
                subIterator.remove();
            } else {
                clearAt(previousInAccessor);
                previousInAccessor = -1;
            }
            modCountCheck = ++modCount;
        }
    }

    /**
     * Adds the given node to this list. This method fetches the object from {@link TableColumn#VALUE}
     * and assigns it to the property identified by {@link TableColumn#IDENTIFIER}. All other columns
     * are ignored.
     *
     * <p>If the identified property is a collection, then this method adds the value to that collection.
     * Otherwise the new value will be set only if the previous value is null,
     * {@linkplain org.apache.sis.xml.NilObject nil} or empty.</p>
     *
     * <p>This method does not iterate explicitly through the children list, because adding a metadata
     * object implicitly adds all its children.</p>
     *
     * @param  node The node from which to get the values.
     * @return {@code true} if the metadata changed as a result of this method call.
     * @throws NullPointerException if the given node is null.
     * @throws IllegalArgumentException if this list does not have a property for the node identifier.
     * @throws IllegalStateException if a value already exists and no more value can be added for the node identifier.
     * @throws UnmodifiableMetadataException if the property for the node identifier is read-only.
     * @throws ClassCastException if the node value can not be converted to the expected type.
     * @throws BackingStoreException if the metadata implementation threw a checked exception.
     */
    @Override
    public boolean add(final TreeTable.Node node) throws IllegalStateException {
        final String identifier = node.getValue(TableColumn.IDENTIFIER);
        if (identifier == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.MissingValueInColumn_1, TableColumn.IDENTIFIER.getHeader()));
        }
        return add(accessor.indexOf(identifier, true), node.getValue(TableColumn.VALUE));
    }

    /**
     * Implementation of {@link #add(TreeTable.Node)}, also invoked by {@link TreeNode.NewChild}.
     * This method will attempt to convert the given {@code value} to the expected type.
     *
     * @param  index The index in the accessor (<em>not</em> the index in this collection).
     * @param  value The property value to add.
     * @return {@code true} if the metadata changed as a result of this method call.
     */
    final boolean add(final int index, final Object value) throws IllegalStateException {
        if (ValueExistencePolicy.isNullOrEmpty(value)) {
            return false;
        }
        // Conversion attempt happen in the PropertyAccessor.set(…) method.
        final Boolean changed = (Boolean) accessor.set(index, metadata, value, PropertyAccessor.APPEND);
        if (changed == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1,
                    accessor.name(index, KeyNamePolicy.UML_IDENTIFIER)));
        }
        if (changed) {
            modCount++;
        }
        return changed;
    }

    /**
     * Returns a string representation of this collection for debugging purpose.
     * This string representation uses one line per element instead of formatting
     * everything on a single line.
     */
    @Debug
    @Override
    public String toString() {
        final String lineSeparator = JDK7.lineSeparator();
        final StringBuilder buffer = new StringBuilder(512);
        parent.toString(buffer);
        buffer.append(lineSeparator);
        for (final TreeTable.Node node : this) {
            buffer.append("  ");
            ((TreeNode) node).toString(buffer);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
