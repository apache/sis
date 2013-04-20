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
import org.apache.sis.util.Decorator;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedSet(Set, Class) checked} {@link LinkedHashSet}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping a {@link LinkedHashSet} using the methods provided
 * in the standard {@link Collections} class, except for the following advantages:</p>
 *
 * <ul>
 *   <li>Avoid one level of indirection.</li>
 *   <li>Checks for write permission.</li>
 *   <li>Overrideable methods for controlling the type checks and write permission checks.</li>
 * </ul>
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see Collections#checkedSet(Set, Class)
 */
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
     * Checks if changes in this set are allowed. This method is automatically invoked before any
     * operation that may change the content. If the write operation is allowed, then this method
     * shall returns normally. Otherwise an {@link UnsupportedOperationException} is thrown.
     *
     * <p>The default implementation does nothing, thus allowing this set to be modified.
     * Subclasses can override this method if they want to control write permissions.</p>
     *
     * @throws UnsupportedOperationException if this set is unmodifiable.
     */
    protected void checkWritePermission() throws UnsupportedOperationException {
    }

    /**
     * An iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by {@link LinkedHashSet#iterator()}.
     *
     * @see CheckedHashSet#iterator()
     */
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
            return iterator.hasNext();
        }

        /** Returns the next element in the iteration. */
        @Override
        public E next() throws NoSuchElementException {
            return iterator.next();
        }

        /** Removes the previous element if the enclosing {@link CheckedHashSet} allows write operations. */
        @Override
        public void remove() throws UnsupportedOperationException {
            checkWritePermission();
            iterator.remove();
        }
    }

    /**
     * Returns an iterator over the elements in this set.
     * The returned iterator will support {@linkplain Iterator#remove() element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter(super.iterator());
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
        checkWritePermission();
        return super.add(element);
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
        checkWritePermission();
        return super.addAll(collection);
    }

    /**
     * Removes the specified element from this set.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean remove(Object o) throws UnsupportedOperationException {
        checkWritePermission();
        return super.remove(o);
    }

    /**
     * Removes all of this set's elements that are also contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean removeAll(Collection<?> c) throws UnsupportedOperationException {
        checkWritePermission();
        return super.removeAll(c);
    }

    /**
     * Retains only the elements in this set that are contained in the specified collection.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public boolean retainAll(Collection<?> c) throws UnsupportedOperationException {
        checkWritePermission();
        return super.retainAll(c);
    }

    /**
     * Removes all of the elements from this set.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        checkWritePermission();
        super.clear();
    }
}
