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

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedSet(Set, Class) checked} and
 * {@linkplain Collections#synchronizedSet(Set) synchronized} {@link LinkedHashSet}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping a {@link LinkedHashSet} using the methods provided
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
 * to the set in case of concurrent access. It does <strong>not</strong> prevent
 * {@link java.util.ConcurrentModificationException} to be thrown during iterations,
 * unless the whole iteration is synchronized on this set {@linkplain #getLock() lock}.
 * For real concurrency, see the {@link java.util.concurrent} package instead.
 *
 * {@note The above is the reason why the name of this class emphases the <cite>checked</cite>
 *        aspect rather than the <cite>synchronized</cite> aspect of the set.}
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see Collections#checkedSet(Set, Class)
 * @see Collections#synchronizedSet(Set)
 */
@ThreadSafe
public class CheckedHashSet<E> extends LinkedHashSet<E> implements CheckedContainer<E>, Cloneable {
    /**
     * Serial version UID for compatibility with different versions.
     */
    private static final long serialVersionUID = 1999408533884863599L;

    /**
     * The element type.
     */
    private final Class<E> type;

    /**
     * Constructs a set of the specified type.
     *
     * @param type The element type (can not be null).
     */
    public CheckedHashSet(final Class<E> type) {
        super();
        this.type = type;
        ensureNonNull("type", type);
    }

    /**
     * Constructs a set of the specified type and initial capacity.
     *
     * @param type The element type (should not be null).
     * @param capacity The initial capacity.
     */
    public CheckedHashSet(final Class<E> type, final int capacity) {
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
     * Ensures that the given element can be added to this set.
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
     * @throws IllegalArgumentException if the specified element can not be added to this set.
     */
    protected void ensureValid(final E element) throws IllegalArgumentException {
        if (element != null && !type.isInstance(element)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, "element", type, element.getClass()));
        }
    }

    /**
     * Ensures that all elements of the given collection can be added to this set.
     *
     * @param  collection the collection to check, or {@code null}.
     * @throws IllegalArgumentException if at least one element can not be added to this set.
     */
    private void ensureValidCollection(final Collection<? extends E> collection) throws IllegalArgumentException {
        for (final E element : collection) {
            ensureValid(element);
        }
    }

    /**
     * Checks if changes in this set are allowed. This method is automatically invoked
     * after this set got the {@linkplain #getLock() lock} and before any operation that
     * may change the content. If the write operation is allowed, then this method shall
     * returns normally. Otherwise an {@link UnsupportedOperationException} is thrown.
     *
     * <p>The default implementation does nothing significant (see below), thus allowing this set to
     * be modified. Subclasses can override this method if they want to control write permissions.</p>
     *
     * {@note Actually the current implementation contains an <code>assert</code> statement
     *        ensuring that the thread holds the lock. This is an implementation details that may
     *        change in any future version of the SIS library. Nevertheless methods that override
     *        this one are encouraged to invoke <code>super.checkWritePermission()</code>.}
     *
     * @throws UnsupportedOperationException if this set is unmodifiable.
     */
    protected void checkWritePermission() throws UnsupportedOperationException {
        assert Thread.holdsLock(getLock());
    }

    /**
     * Returns the synchronization lock. The default implementation returns {@code this}.
     *
     * {@section Note for subclass implementors}
     * Subclasses that override this method must be careful to update the lock reference
     * (if needed) when this set is {@linkplain #clone() cloned}.
     *
     * @return The synchronization lock.
     */
    protected Object getLock() {
        return this;
    }

    /**
     * A synchronized iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by {@link LinkedHashSet#iterator()}.
     *
     * @see CheckedHashSet#iterator()
     */
    @ThreadSafe
    @Decorator(Iterator.class)
    private final class Iter implements Iterator<E> {
        /** The {@link LinkedHashSet} iterator. */
        private final Iterator<E> iterator;

        /** Creates a new wrapper for the given {@link LinkedHashSet} iterator. */
        Iter(final Iterator<E> iterator) {
            this.iterator = iterator;
        }

        /** Returns {@code true} if there is more elements in the iteration. */
        @Override
        public boolean hasNext() {
            synchronized (getLock()) {
                return iterator.hasNext();
            }
        }

        /** Returns the next element in the iteration. */
        @Override
        public E next() throws NoSuchElementException {
            synchronized (getLock()) {
                return iterator.next();
            }
        }

        /** Removes the previous element if the enclosing {@link CheckedHashSet} allows write operations. */
        @Override
        public void remove() throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                iterator.remove();
            }
        }
    }

    /**
     * Returns an iterator over the elements in this set.
     * The returned iterator will support {@linkplain Iterator#remove() element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public Iterator<E> iterator() {
        synchronized (getLock()) {
            return new Iter(super.iterator());
        }
    }

    /**
     * Returns the number of elements in this set.
     */
    @Override
    public int size() {
        synchronized (getLock()) {
            return super.size();
        }
    }

    /**
     * Returns {@code true} if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        synchronized (getLock()) {
            return super.isEmpty();
        }
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     */
    @Override
    public boolean contains(final Object o) {
        synchronized (getLock()) {
            return super.contains(o);
        }
    }

    /**
     * Adds the specified element to this set if it is not already present.
     *
     * @param  element element to be added to this set.
     * @return {@code true} if the set did not already contain the specified element.
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
     * Appends all of the elements in the specified collection to this set.
     *
     * @param  collection the elements to be inserted into this set.
     * @return {@code true} if this set changed as a result of the call.
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
     * Removes the specified element from this set.
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
     * Removes all of this set's elements that are also contained in the specified collection.
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
     * Retains only the elements in this set that are contained in the specified collection.
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
     * Removes all of the elements from this set.
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
     * Returns an array containing all of the elements in this set.
     */
    @Override
    public Object[] toArray() {
        synchronized (getLock()) {
            return super.toArray();
        }
    }

    /**
     * Returns an array containing all of the elements in this set.
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
     * Returns a string representation of this set.
     */
    @Override
    public String toString() {
        synchronized (getLock()) {
            return super.toString();
        }
    }

    /**
     * Compares the specified object with this set for equality.
     */
    @Override
    public boolean equals(Object o) {
        synchronized (getLock()) {
            return super.equals(o);
        }
    }

    /**
     * Returns the hash code value for this set.
     */
    @Override
    public int hashCode() {
        synchronized (getLock()) {
            return super.hashCode();
        }
    }

    /**
     * Returns a shallow copy of this set.
     *
     * @return A shallow copy of this set.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CheckedHashSet<E> clone() {
        synchronized (getLock()) {
            return (CheckedHashSet<E>) super.clone();
        }
    }
}
