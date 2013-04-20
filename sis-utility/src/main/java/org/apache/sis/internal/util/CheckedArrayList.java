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
package org.apache.sis.internal.util;

import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedList(List, Class) checked} {@link ArrayList}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping an {@link ArrayList} using the methods provided
 * in the standard {@link Collections} class, except for the following advantages:</p>
 *
 * <ul>
 *   <li>Avoid one level of indirection.</li>
 *   <li>Checks for write permission.</li>
 *   <li>Overrideable methods for controlling the type checks and write permission checks.</li>
 * </ul>
 *
 * @param <E> The type of elements in the list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see Collections#checkedList(List, Class)
 */
public class CheckedArrayList<E> extends ArrayList<E> implements CheckedContainer<E>, Cloneable {
    /**
     * Serial version UID for compatibility with different versions.
     */
    private static final long serialVersionUID = -8265578982723471814L;

    /**
     * The element type.
     */
    private final Class<E> type;

    /**
     * Constructs a list of the specified type.
     *
     * @param type The element type (can not be null).
     */
    public CheckedArrayList(final Class<E> type) {
        super();
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Constructs a list of the specified type and initial capacity.
     *
     * @param type The element type (should not be null).
     * @param capacity The initial capacity.
     */
    public CheckedArrayList(final Class<E> type, final int capacity) {
        super(capacity);
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Returns the element type given at construction time.
     */
    @Override
    public Class<E> getElementType() {
        return type;
    }

    /**
     * Ensures that the given element can be added to this list.
     * The default implementation ensures that the object is {@code null} or assignable
     * to the type specified at construction time. Subclasses can override this method
     * if they need to perform additional checks.
     *
     * @param  element the object to check, or {@code null}.
     * @throws IllegalArgumentException if the specified element can not be added to this list.
     */
    protected void ensureValid(final E element) throws IllegalArgumentException {
        if (element != null && !type.isInstance(element)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, "element", type, element.getClass()));
        }
    }

    /**
     * Ensures that all elements of the given collection can be added to this list.
     *
     * @param  collection the collection to check, or {@code null}.
     * @throws IllegalArgumentException if at least one element can not be added to this list.
     */
    private void ensureValidCollection(final Collection<? extends E> collection) throws IllegalArgumentException {
        for (final E element : collection) {
            ensureValid(element);
        }
    }

    /**
     * Checks if changes in this list are allowed. This method is automatically invoked before any
     * operation that may change the content. If the write operation is allowed, then this method
     * shall returns normally. Otherwise an {@link UnsupportedOperationException} is thrown.
     *
     * <p>The default implementation does nothing, thus allowing this list to be modified.
     * Subclasses can override this method if they want to control write permissions.</p>
     *
     * @throws UnsupportedOperationException if this list is unmodifiable.
     */
    protected void checkWritePermission() throws UnsupportedOperationException {
    }

    /**
     * An iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by {@link ArrayList#iterator()}, and is
     * also the base class for the wrapper around {@link ArrayList#listIterator()}.
     *
     * @see CheckedArrayList#iterator()
     */
    @Decorator(Iterator.class)
    private class Iter<I extends Iterator<E>> implements Iterator<E> {
        /** The {@link ArrayList} iterator. */
        protected final I iterator;

        /** Creates a new wrapper for the given {@link ArrayList} iterator. */
        Iter(final I iterator) {
            this.iterator = iterator;
        }

        /** Returns {@code true} if there is more elements in the iteration. */
        @Override
        public final boolean hasNext() {
            return iterator.hasNext();
        }

        /** Returns the next element in the iteration. */
        @Override
        public final E next() throws NoSuchElementException {
            return iterator.next();
        }

        /** Removes the previous element if the enclosing {@link CheckedArrayList} allows write operations. */
        @Override
        public final void remove() throws UnsupportedOperationException {
            checkWritePermission();
            iterator.remove();
        }
    }

    /**
     * A list iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by {@link ArrayList#listIterator()}.
     *
     * @see CheckedArrayList#listIterator()
     * @see CheckedArrayList#listIterator(int)
     */
    @Decorator(ListIterator.class)
    private class ListIter extends Iter<ListIterator<E>> implements ListIterator<E> {
        /** Creates a new wrapper for the given {@link ArrayList} list iterator. */
        ListIter(final ListIterator<E> iterator) {
            super(iterator);
        }

        /** Returns the index of the element to be returned by {@link #next()}. */
        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        /** Returns the index of the element to be returned by {@link #previous()}. */
        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        /** Returns {@code true} if there is elements before current position. */
        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        /** Returns the previous element in the iteration. */
        @Override
        public E previous() throws NoSuchElementException {
            return iterator.previous();
        }

        /** See the {@link CheckedArrayList#set(int, Object)} method contract. */
        @Override
        public void set(final E element) throws IllegalArgumentException, UnsupportedOperationException {
            ensureValid(element);
            checkWritePermission();
            iterator.set(element);
        }

        /** See the {@link CheckedArrayList#add(Object)} method contract. */
        @Override
        public void add(final E element) throws IllegalArgumentException, UnsupportedOperationException {
            ensureValid(element);
            checkWritePermission();
            iterator.add(element);
        }
    }

    /**
     * Returns an iterator over the elements in this list.
     * The returned iterator will support {@linkplain Iterator#remove() element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter<>(super.iterator());
    }

    /**
     * Returns an iterator over the elements in this list.
     * The returned iterator will support {@linkplain ListIterator#remove() element removal},
     * {@linkplain ListIterator#add(Object) addition} or {@linkplain ListIterator#set(Object)
     * modification} only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public ListIterator<E> listIterator() {
        return new ListIter(super.listIterator());
    }

    /**
     * Returns an iterator over the elements in this list, starting at the given index.
     * The returned iterator will support {@linkplain ListIterator#remove() element removal},
     * {@linkplain ListIterator#add(Object) addition} or {@linkplain ListIterator#set(Object)
     * modification} only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ListIter(super.listIterator(index));
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param  index   index of element to replace.
     * @param  element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public E set(final int index, final E element)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValid(element);
        checkWritePermission();
        return super.set(index, element);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param  element element to be appended to this list.
     * @return always {@code true}.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean add(final E element)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValid(element);
        checkWritePermission();
        return super.add(element);
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param  index index at which the specified element is to be inserted.
     * @param  element element to be inserted.
     * @throws IndexOutOfBoundsException if index out of range.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void add(final int index, final E element)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValid(element);
        checkWritePermission();
        super.add(index, element);
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified Collection's Iterator.
     *
     * @param  collection the elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws IllegalArgumentException if at least one element is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean addAll(final Collection<? extends E> collection)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValidCollection(collection);
        checkWritePermission();
        return super.addAll(collection);
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position.
     *
     * @param  index index at which to insert first element fromm the specified collection.
     * @param  collection elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @throws IllegalArgumentException if at least one element is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends E> collection)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValidCollection(collection);
        checkWritePermission();
        return super.addAll(index, collection);
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public E remove(int index) throws UnsupportedOperationException {
        checkWritePermission();
        return super.remove(index);
    }

    /**
     * Removes the first occurrence of the specified element from this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean remove(Object o) throws UnsupportedOperationException {
        checkWritePermission();
        return super.remove(o);
    }

    /**
     * Removes all of this list's elements that are also contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean removeAll(Collection<?> c) throws UnsupportedOperationException {
        checkWritePermission();
        return super.removeAll(c);
    }

    /**
     * Retains only the elements in this list that are contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean retainAll(Collection<?> c) throws UnsupportedOperationException {
        checkWritePermission();
        return super.retainAll(c);
    }

    /**
     * Removes all of the elements from this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        checkWritePermission();
        super.clear();
    }
}
