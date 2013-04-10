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
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedList(List, Class) checked} and
 * {@linkplain Collections#synchronizedList(List) synchronized} {@link ArrayList}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping an {@link ArrayList} using the methods provided
 * in the standard {@link Collections} class, except for the following advantages:</p>
 *
 * <ul>
 *   <li>Avoid the two levels of indirection (for type check and synchronization).</li>
 *   <li>Checks for write permission.</li>
 *   <li>Overrideable methods for controlling the synchronization lock,
 *       type checks and write permission checks.</li>
 * </ul>
 *
 * The synchronization is provided mostly in order to prevent damages
 * to the list in case of concurrent access. It does <strong>not</strong> prevent
 * {@link java.util.ConcurrentModificationException} to be thrown during iterations,
 * unless the whole iteration is synchronized on this list {@linkplain #getLock() lock}.
 * For real concurrency, see the {@link java.util.concurrent} package instead.
 *
 * {@note The above is the reason why the name of this class emphases the <cite>checked</cite>
 *        aspect rather than the <cite>synchronized</cite> aspect of the list.}
 *
 * @param <E> The type of elements in the list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see Collections#checkedList(List, Class)
 * @see Collections#synchronizedList(List)
 */
@ThreadSafe
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
     * {@section Synchronization}
     * This method is invoked <em>before</em> to get the synchronization {@linkplain #getLock() lock}.
     * This is different than the {@link #checkWritePermission()} method, which is invoked inside the
     * synchronized block.
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
     * Checks if changes in this list are allowed. This method is automatically invoked
     * after this list got the {@linkplain #getLock() lock} and before any operation that
     * may change the content. If the write operation is allowed, then this method shall
     * returns normally. Otherwise an {@link UnsupportedOperationException} is thrown.
     *
     * <p>The default implementation does nothing significant (see below), thus allowing this list to
     * be modified. Subclasses can override this method if they want to control write permissions.</p>
     *
     * {@note Actually the current implementation contains an <code>assert</code> statement
     * ensuring that the thread holds the lock. This is an implementation details that may
     * change in any future version of the SIS library. Nevertheless methods that override
     * this one are encouraged to invoke <code>super.checkWritePermission()</code>.}
     *
     * @throws UnsupportedOperationException if this list is unmodifiable.
     */
    protected void checkWritePermission() throws UnsupportedOperationException {
        assert Thread.holdsLock(getLock());
    }

    /**
     * Returns the synchronization lock. The default implementation returns {@code this}.
     *
     * {@section Note for subclass implementors}
     * Subclasses that override this method must be careful to update the lock reference
     * (if needed) when this list is {@linkplain #clone() cloned}.
     *
     * @return The synchronization lock.
     */
    protected Object getLock() {
        return this;
    }

    /**
     * A synchronized iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by {@link ArrayList#iterator()}, and is
     * also the base class for the wrapper around {@link ArrayList#listIterator()}.
     *
     * @see CheckedArrayList#iterator()
     */
    @ThreadSafe
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
            synchronized (getLock()) {
                return iterator.hasNext();
            }
        }

        /** Returns the next element in the iteration. */
        @Override
        public final E next() throws NoSuchElementException {
            synchronized (getLock()) {
                return iterator.next();
            }
        }

        /** Removes the previous element if the enclosing {@link CheckedArrayList} allows write operations. */
        @Override
        public final void remove() throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                iterator.remove();
            }
        }
    }

    /**
     * A synchronized list iterator with a check for write permission prior element removal.
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
            synchronized (getLock()) {
                return iterator.nextIndex();
            }
        }

        /** Returns the index of the element to be returned by {@link #previous()}. */
        @Override
        public int previousIndex() {
            synchronized (getLock()) {
                return iterator.previousIndex();
            }
        }

        /** Returns {@code true} if there is elements before current position. */
        @Override
        public boolean hasPrevious() {
            synchronized (getLock()) {
                return iterator.hasPrevious();
            }
        }

        /** Returns the previous element in the iteration. */
        @Override
        public E previous() throws NoSuchElementException {
            synchronized (getLock()) {
                return iterator.previous();
            }
        }

        /** See the {@link CheckedArrayList#set(int, Object)} method contract. */
        @Override
        public void set(final E element) throws IllegalArgumentException, UnsupportedOperationException {
            ensureValid(element);
            synchronized (getLock()) {
                checkWritePermission();
                iterator.set(element);
            }
        }

        /** See the {@link CheckedArrayList#add(Object)} method contract. */
        @Override
        public void add(final E element) throws IllegalArgumentException, UnsupportedOperationException {
            ensureValid(element);
            synchronized (getLock()) {
                checkWritePermission();
                iterator.add(element);
            }
        }
    }

    /**
     * Returns an iterator over the elements in this list.
     * The returned iterator will support {@linkplain Iterator#remove() element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public Iterator<E> iterator() {
        synchronized (getLock()) {
            return new Iter<Iterator<E>>(super.iterator());
        }
    }

    /**
     * Returns an iterator over the elements in this list.
     * The returned iterator will support {@linkplain ListIterator#remove() element removal},
     * {@linkplain ListIterator#add(Object) addition} or {@linkplain ListIterator#set(Object)
     * modification} only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public ListIterator<E> listIterator() {
        synchronized (getLock()) {
            return new ListIter(super.listIterator());
        }
    }

    /**
     * Returns an iterator over the elements in this list, starting at the given index.
     * The returned iterator will support {@linkplain ListIterator#remove() element removal},
     * {@linkplain ListIterator#add(Object) addition} or {@linkplain ListIterator#set(Object)
     * modification} only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public ListIterator<E> listIterator(final int index) {
        synchronized (getLock()) {
            return new ListIter(super.listIterator(index));
        }
    }

    /**
     * Returns the number of elements in this list.
     */
    @Override
    public int size() {
        synchronized (getLock()) {
            return super.size();
        }
    }

    /**
     * Returns {@code true} if this list contains no elements.
     */
    @Override
    public boolean isEmpty() {
        synchronized (getLock()) {
            return super.isEmpty();
        }
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     */
    @Override
    public boolean contains(final Object o) {
        synchronized (getLock()) {
            return super.contains(o);
        }
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if none.
     */
    @Override
    public int indexOf(Object o) {
        synchronized (getLock()) {
            return super.indexOf(o);
        }
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if none.
     */
    @Override
    public int lastIndexOf(Object o) {
        synchronized (getLock()) {
            return super.lastIndexOf(o);
        }
    }

    /**
     * Returns the element at the specified position in this list.
     */
    @Override
    public E get(int index) {
        synchronized (getLock()) {
            return super.get(index);
        }
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
        synchronized (getLock()) {
            checkWritePermission();
            return super.set(index, element);
        }
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
        synchronized (getLock()) {
            checkWritePermission();
            return super.add(element);
        }
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
        synchronized (getLock()) {
            checkWritePermission();
            super.add(index, element);
        }
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
        synchronized (getLock()) {
            checkWritePermission();
            return super.addAll(collection);
        }
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
        synchronized (getLock()) {
            checkWritePermission();
            return super.addAll(index, collection);
        }
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public E remove(int index) throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            return super.remove(index);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean remove(Object o) throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            return super.remove(o);
        }
    }

    /**
     * Removes all of this list's elements that are also contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean removeAll(Collection<?> c) throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            return super.removeAll(c);
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean retainAll(Collection<?> c) throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            return super.retainAll(c);
        }
    }

    /**
     * Trims the capacity to the list's current size.
     */
    @Override
    public void trimToSize() {
        synchronized (getLock()) {
            super.trimToSize();
        }
    }

    /**
     * Increases the capacity, if necessary, to ensure that it can hold the given number
     * of elements.
     */
    @Override
    public void ensureCapacity(final int minCapacity) {
        synchronized (getLock()) {
            super.ensureCapacity(minCapacity);
        }
    }

    /**
     * Removes all of the elements from this list.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            super.clear();
        }
    }

    /**
     * Returns an array containing all of the elements in this list.
     */
    @Override
    public Object[] toArray() {
        synchronized (getLock()) {
            return super.toArray();
        }
    }

    /**
     * Returns an array containing all of the elements in this list in proper sequence.
     *
     * @param <T> The type of array elements.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (getLock()) {
            return super.toArray(a);
        }
    }

    /**
     * Returns a string representation of this list.
     */
    @Override
    public String toString() {
        synchronized (getLock()) {
            return super.toString();
        }
    }

    /**
     * Compares the specified object with this list for equality.
     */
    @Override
    public boolean equals(Object o) {
        synchronized (getLock()) {
            return super.equals(o);
        }
    }

    /**
     * Returns the hash code value for this list.
     */
    @Override
    public int hashCode() {
        synchronized (getLock()) {
            return super.hashCode();
        }
    }

    /**
     * Returns a shallow copy of this list.
     *
     * @return A shallow copy of this list.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CheckedArrayList<E> clone() {
        synchronized (getLock()) {
            return (CheckedArrayList<E>) super.clone();
        }
    }
}
