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

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractSequentialList;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Debug;


/**
 * The list of children to be returned by {@link MetadataTreeNode#getChildren()}.
 * This list holds a reference to the metadata object at creation time; it does
 * not track changes in {@code parent.getUserObject()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class MetadataTreeChildren extends AbstractSequentialList<TreeTable.Node> {
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
    private final MetadataTreeNode parent;

    /**
     * The metadata object for which property values will be the elements of this list.
     * This is typically an {@link AbstractMetadata} instance, but not necessarily.
     * Any type for which {@link MetadataStandard#isMetadata(Class)} returns {@code true} is okay.
     *
     * <p>This field is a snapshot of the {@linkplain #parent} {@link MetadataTreeNode#getUserObject()}
     * at creation time. This list does not track changes in the reference returned by the above-cited
     * {@code getUserObject()}. In other words, changes in the {@code metadata} object will be reflected
     * in this list, but if {@code parent.getUserObject()} returns a reference to another object, this
     * change will not be reflected in this list.
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
    private final PropertyAccessor accessor;

    /**
     * The children to be returned by this list. All elements in this list are initially
     * {@code null}, then created by {@link #childAt(int)} when first needed.
     *
     * <p>Not all elements in this array will be returned by the list iterator.
     * The value needs to be verified for the {@link ValueExistencePolicy}.</p>
     */
    private final MetadataTreeNode[] children;

    /**
     * Creates a list of children for the specified metadata.
     *
     * @param parent   The parent for which this node is an element.
     * @param metadata The metadata object for which property values will be the elements of this list.
     * @param accessor The accessor to use for accessing the property names, types and values of the metadata object.
     */
    MetadataTreeChildren(final MetadataTreeNode parent, final Object metadata, final PropertyAccessor accessor) {
        this.parent   = parent;
        this.metadata = metadata;
        this.accessor = accessor;
        children = new MetadataTreeNode[accessor.count()];
    }

    /**
     * Clears the value at the given index. The given {@code index} is relative to
     * the {@link #accessor} indexing, <strong>not</strong> to this list index.
     *
     * <p>The cleared elements may or may not be considered as removed, depending on the
     * value policy. To check if the element shall be considered as removed (for example
     * in order to update index), invoke {@code isSkipped(value)} after this method.</p>
     *
     * {@note We do not provide setter method because this <code>List</code> contract
     *        requires the values to be instances of {@code TreeTable.Node}, which is
     *        not very convenient in the case of our list view.}
     *
     * @param index The index in the accessor (<em>not</em> the index in this list).
     */
    final void clearAt(final int index) {
        accessor.set(index, metadata, null, false);
    }

    /**
     * Returns the value at the given index. The given {@code index} is relative to
     * the {@link #accessor} indexing, <strong>not</strong> to this list index.
     *
     * @param  index The index in the accessor (<em>not</em> the index in this list).
     * @return The value at the given index. May be {@code null} or a collection.
     */
    final Object valueAt(final int index) {
        return accessor.get(index, metadata);
    }

    /**
     * Returns {@code true} if the type at the given index is a collection. The given
     * {@code index} is relative to the {@link #accessor} indexing, <strong>not</strong>
     * to this list index.
     *
     * {@note We do not test <code>(value instanceof Collection)</code> because the value
     *        could be any user's implementation. Nothing prevent users from implementing
     *        the collection interface even for singleton elements if they wish.}
     *
     * @param  index The index in the accessor (<em>not</em> the index in this list).
     * @return {@code true} if the value at the given index is a collection.
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
     * is relative to the {@link #accessor} indexing, <strong>not</strong> to this list index.
     *
     * <p>This method does not check if the child at the given index should be skipped.
     * It is caller responsibility to do such verification before this method call.</p>
     *
     * @param  index The index in the accessor (<em>not</em> the index in this list).
     * @param  childIndex If the property at {@link #index} is a collection, the index
     *         in that collection (<em>not</em> the index in this list). Otherwise -1.
     * @return The node to be returned by pulic API.
     */
    final MetadataTreeNode childAt(final int index, final int childIndex) {
        MetadataTreeNode node = children[index];
        if (childIndex >= 0) {
            /*
             * If the value is an element of a collection, we will cache only the last used value.
             * We don't cache all elements in order to avoid yet more complex code, and this cover
             * the majority of cases where the collection has only one element anyway.
             */
            if (node == null || ((MetadataTreeNode.CollectionElement) node).indexInList != childIndex) {
                node = new MetadataTreeNode.CollectionElement(parent, metadata, accessor, index, childIndex);
            }
        } else {
            /*
             * If the property is a singleton (not an element of a collection), returns a more
             * dynamic node which will fetch the value from the metadata object. This allows
             * the node to reflect changes in the metadata object, and conversely.
             */
            if (node == null) {
                node = new MetadataTreeNode.Element(parent, metadata, accessor, index);
            }
        }
        children[index] = node;
        return node;
    }

    /**
     * Returns the maximal number of children. This is the number of all possible elements
     * according the {@link #accessor}, including {@linkplain #isSkipped(Object) skipped}
     * ones. This is <strong>not</strong> the list size.
     */
    final int childCount() {
        return children.length;
    }

    /**
     * Returns the number of elements in this list, ignoring the {@link #isSkipped(Object)
     * skipped} ones.
     */
    @Override
    public int size() {
        return accessor.count(metadata, parent.table.valuePolicy, PropertyAccessor.COUNT_DEEP);
    }

    /**
     * Returns {@code true} if this list contains no elements. Invoking this method is more efficient
     * than testing {@code size() == 0} because this method does not iterate over all properties.
     */
    @Override
    public boolean isEmpty() {
        return accessor.count(metadata, parent.table.valuePolicy, PropertyAccessor.COUNT_FIRST) == 0;
    }

    /**
     * Returns an iterator over the nodes in the list of children.
     */
    @Override
    public Iterator<TreeTable.Node> iterator() {
        return new Iter();
    }

    /**
     * Returns a bidirectional iterator over the nodes in the list of children.
     */
    @Override
    public ListIterator<TreeTable.Node> listIterator() {
        return new BiIter();
    }

    /**
     * Returns an iterator over the nodes in the list of children, starting at the given index.
     */
    @Override
    public ListIterator<TreeTable.Node> listIterator(final int index) {
        if (index >= 0) {
            final BiIter it = new BiIter();
            if (it.skip(index)) {
                return it;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * The iterator returned by {@link #iterator()}. This iterator fetches metadata property
     * values and creates the nodes only when first needed.
     */
    private class Iter implements Iterator<TreeTable.Node> {
        /**
         * Index in {@link MetadataTreeChildren#accessor} of the next element to be
         * returned by {@link #next()}, or {@link PropertyAccessor#count()} if we
         * have reached the end of the list.
         */
        private int nextInAccessor;

        /**
         * Index in {@link MetadataTreeChildren#accessor} of the element returned by
         * the last call to {@link #next()}, or -1 if none.
         */
        private int previousInAccessor = -1;

        /**
         * {@code true} if we have verified the value at {@link #nextInAccessor} index
         * for non-null or non-empty element.
         */
        private boolean isNextVerified;

        /**
         * The value to be returned by {@link #next()} method. This value is computed ahead
         * of time by {@link #hasNext()} since we need that information in order to determine
         * if the value needs to be skipped or not.
         */
        private Object nextValue;

        /**
         * If the call to {@link #next()} found a collection, the iterator over the elements
         * in that collection. Otherwise {@code null}.
         *
         * <p>A non-null value (even if that child iterator has no next elements) means that
         * {@link #nextValue} is an element of that child iteration.</p>
         */
        private Iterator<?> childIterator;

        /**
         * Position of the {@link #nextValue} in the {@link #childIterator}.
         * This field has no meaning if the child iterator is null.
         */
        private int childIndex;

        /**
         * The value of {@link AbstractSequentialList#modCount} at construction time or
         * after the last change done by this iterator. Used for concurrent modification
         * checks.
         *
         * {@note Actually this iterator should be robust to most concurrent modifications.
         *        But we check anyway in order to prevent concurrent modifications in user
         *        code, in case a future SIS version become more sensitive to such changes.}
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
         * @return {@code true} on success, or {@code false} if we reached the end of the list.
         */
        @Override
        public boolean hasNext() {
            checkConcurrentModification();
            if (isNextVerified) {
                return true;
            }
            /*
             * If an iteration was under progress, move to the next element from that iteration.
             * We do not check for 'isSkipped(value)' here because empty elements in collections
             * are probably mistakes, and we want to see them.
             */
            if (childIterator != null) {
                if (childIterator.hasNext()) {
                    childIndex++;
                    nextValue = childIterator.next();
                    isNextVerified = true;
                    return true;
                }
                childIterator = null;
                nextInAccessor++; // See the comment before nextInAccessor++ in the next() method.
            }
            /*
             * Search for the next property, which may be either a singleton or the first element
             * of a collection. In the later case, we will create a child iterator.
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
                            childIterator = ((Collection<?>) nextValue).iterator();
                        } else {
                            childIterator = Collections.emptyIterator();
                            // Null collections are illegal (it shall be empty collections instead),
                            // but we try to keep the iterator robut to ill-formed metadata, because
                            // we want AbstractMetadata.toString() to work so we can spot problems.
                        }
                        childIndex = 0;
                        if (childIterator.hasNext()) {
                            nextValue = childIterator.next();
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
                final boolean isElementOfCollection = (childIterator != null);
                final MetadataTreeNode node = childAt(nextInAccessor, isElementOfCollection ? childIndex : -1);
                previousInAccessor = nextInAccessor;
                if (!isElementOfCollection) {
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
         * on the value policy and on the element type.
         */
        @Override
        public void remove() {
            if (previousInAccessor < 0) {
                throw new IllegalStateException();
            }
            checkConcurrentModification();
            if (childIterator != null) {
                childIterator.remove();
            } else {
                clearAt(previousInAccessor);
                previousInAccessor = -1;
            }
            modCountCheck = ++modCount;
        }
    }

    /**
     * The bidirectional iterator returned by {@link #listIterator(int)}.
     */
    private final class BiIter extends Iter implements ListIterator<TreeTable.Node> {
        /**
         * The previous elements returned by this iterator.
         */
        private TreeTable.Node[] previous;

        /**
         * Number of valid elements in the {@link #previous} array.
         * This is the position of the {@link Iter} super-class.
         */
        private int position;

        /**
         * Index to be returned by {@link #nextIndex()}.
         *
         * @see #nextIndex()
         * @see #previousIndex()
         */
        private int nextIndex;

        /**
         * Creates a new iterator.
         */
        BiIter() {
            previous = new TreeTable.Node[childCount()];
        }

        /**
         * Skips the given amount of elements. This is a convenience method
         * for implementation of {@link MetadataTreeChildren#listIterator(int)}.
         *
         * @param  n Number of elements to skip.
         * @return {@code true} on success, or {@code false} if the list doesn't contain enough elements.
         */
        boolean skip(int n) {
            while (--n >= 0) {
                if (!super.hasNext()) {
                    return false;
                }
                next();
            }
            return true;
        }

        /**
         * Returns the index of the element to be returned by {@link #next()},
         * or the list size if the iterator is at the end of the list.
         */
        @Override
        public int nextIndex() {
            return nextIndex;
        }

        /**
         * Returns the index of the element to be returned by {@link #previous()},
         * or -1 if the iterator is at the beginning of the list.
         */
        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        /**
         * Returns {@code true} if {@link #next()} can return an element.
         */
        @Override
        public boolean hasNext() {
            return (nextIndex < position) || super.hasNext();
        }

        /**
         * Returns {@code true} if {@link #previous()} can return an element.
         */
        @Override
        public boolean hasPrevious() {
            checkConcurrentModification();
            return nextIndex != 0;
        }

        /**
         * Returns the next element.
         */
        @Override
        public TreeTable.Node next() {
            if (nextIndex < position) {
                checkConcurrentModification();
                return previous[nextIndex++];
            }
            final TreeTable.Node node = super.next();
            if (nextIndex == previous.length) {
                previous = Arrays.copyOf(previous, nextIndex*2);
            }
            previous[nextIndex++] = node;
            position = nextIndex;
            return node;
        }

        /**
         * Returns the previous element.
         */
        @Override
        public TreeTable.Node previous() {
            if (hasPrevious()) {
                return previous[--nextIndex];
            }
            throw new NoSuchElementException();
        }

        /**
         * Current implementation does not support removal after {@link #previous()}.
         */
        @Override
        public void remove() {
            if (nextIndex != position) {
                throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "remove"));
            }
            super.remove();
        }

        /**
         * Unsupported operation.
         */
        @Override
        public void set(TreeTable.Node e) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "set"));
        }

        /**
         * Unsupported operation.
         */
        @Override
        public void add(TreeTable.Node e) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "add"));
        }
    }

    /**
     * Returns a string representation of this list for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final String lineSeparator = System.lineSeparator();
        final StringBuilder buffer = new StringBuilder(512);
        parent.toString(buffer);
        buffer.append(lineSeparator);
        for (final TreeTable.Node node : this) {
            buffer.append("  ");
            ((MetadataTreeNode) node).toString(buffer);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
