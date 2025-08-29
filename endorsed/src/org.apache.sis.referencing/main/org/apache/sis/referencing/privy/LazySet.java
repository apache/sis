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
package org.apache.sis.referencing.privy;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.util.privy.SetOfUnknownSize;


/**
 * An unmodifiable set built from an iterator, which will be filled only when needed.
 * This implementation does <strong>not</strong> check if all elements in the iterator
 * are really unique. We assume that this condition was already verified by the caller.
 *
 * <p>Some usages for this class are to prepend some values before the elements given by the source {@code Iterable},
 * or to replace some values when they are loaded.</p>
 *
 * <h2>Thread-safety</h2>
 * This class is thread safe. The synchronization lock is {@code this}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see FilteredIterator
 *
 * @param <E>  the type of elements in the set.
 */
public abstract class LazySet<E> extends SetOfUnknownSize<E> {
    /**
     * The iterator to use for filling this set, or {@code null} if the iteration did not started yet or is finished.
     * Those two cases can be distinguished by looking whether the {@link #cachedElements} array is null or not.
     */
    private Iterator<? extends E> sourceIterator;

    /**
     * The elements that we cached so far, or {@code null} if the iteration did not started yet.
     * After the iteration started, this array will grow as needed.
     *
     * @see #createCache()
     * @see #cache(Object)
     */
    private E[] cachedElements;

    /**
     * The number of valid elements in the {@link #cachedElements} array.
     * This counter will be incremented as long as there is more elements returned by {@link #sourceIterator}.
     */
    private int numCached;

    /**
     * Creates a new set.
     */
    protected LazySet() {
    }

    /**
     * Creates the iterator which will provide the elements of this set.
     * This method will be invoked only when first needed and at most once, unless {@link #reload()} is invoked.
     * After creation, calls to {@link Iterator#next()} will also be done only when first needed.
     *
     * @return iterator over the elements of this set.
     */
    protected abstract Iterator<? extends E> createSourceIterator();

    /**
     * Hook for subclasses that want to prepend some values before the source {@code Iterable}.
     * This method is invoked only when first needed. It is safe to return a shared array since
     * {@code LazySet} will not write in that array ({@code LazySet} will create a new array if
     * it needs to add more values).
     *
     * @return values to prepend before the source {@code Iterable}, or {@code null} if none.
     */
    protected E[] initialValues() {
        return null;
    }

    /**
     * Creates the {@link #cachedElements} array. This array will contain the elements
     * given by {@link #initialValues()} if that method returned a non-null and non-empty array.
     *
     * @return {@code true} if {@link #initialValues()} initialized the set with at least one value.
     */
    @SuppressWarnings("unchecked")
    private boolean createCache() {
        cachedElements = initialValues();                   // No need to clone.
        if (cachedElements != null) {
            numCached = cachedElements.length;
            if (numCached != 0) {
                return true;
            }
        }
        cachedElements = (E[]) new Object[4];
        return false;
    }

    /**
     * Returns {@code true} if the {@link #sourceIterator} is non-null and have more elements to return,
     * or if we initialized the cache with some elements declared by {@link #initialValues()}.
     */
    private boolean canPullMore() {
        if (sourceIterator == null && cachedElements == null) {
            sourceIterator = createSourceIterator();
            if (createCache()) {
                return true;
            }
        }
        if (sourceIterator != null) {
            if (sourceIterator.hasNext()) {
                return true;
            }
            sourceIterator = null;
        }
        return false;
    }

    /**
     * Tests if this set has no element.
     *
     * @return {@code true} if this set has no element.
     */
    @Override
    public final synchronized boolean isEmpty() {
        return (numCached == 0) && !canPullMore();
    }

    /**
     * Returns the number of elements in this set. Invoking this method
     * forces the set to immediately iterates through all remaining elements.
     *
     * @return number of elements in the iterator.
     */
    @Override
    public final synchronized int size() {
        if (canPullMore()) {
            while (sourceIterator.hasNext()) {
                cache(sourceIterator.next());
            }
            sourceIterator = null;
        }
        return numCached;
    }

    /**
     * Caches a new element. This method is invoked by {@code LazySet} inside a synchronized block.
     * Subclasses could override this method if they want to substitute the given value by another value.
     *
     * @param  element  the element to add to the cache.
     */
    private void cache(final E element) {
        if (cachedElements == null) {
            createCache();
        }
        if (numCached >= cachedElements.length) {
            cachedElements = Arrays.copyOf(cachedElements, numCached << 1);
        }
        cachedElements[numCached++] = element;
    }

    /**
     * Returns {@code true} if an element exists at the given index.
     * The element is not loaded immediately.
     *
     * <p><strong>NOTE: This method is for use by iterators only.</strong>
     * It is not suited for more general usage since it does not check for
     * negative index and for skipped elements.</p>
     */
    private synchronized boolean exists(final int index) {
        assert index <= numCached : index;
        return (index < numCached) || canPullMore();
    }

    /**
     * Returns the element at the specified position in this set.
     *
     * @param  index  the index at which to get an element.
     * @return the element at the requested index.
     */
    private synchronized E get(final int index) {
        assert numCached <= cachedElements.length : numCached;
        assert index <= numCached : index;
        if (index >= numCached) {
            if (canPullMore()) {
                cache(sourceIterator.next());
            } else {
                throw new NoSuchElementException();
            }
        }
        return cachedElements[index];
    }

    /**
     * Returns an iterator over the elements contained in this set.
     * This is not the same iterator as the one given to the constructor.
     *
     * @return an iterator over the elements in this set.
     */
    @Override
    public final Iterator<E> iterator() {
        return new Iterator<E>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return exists(cursor);
            }

            @Override
            public E next() {
                return get(cursor++);
            }
        };
    }

    /**
     * Notifies this {@code LazySet} that it should re-fetch the elements from the source.
     */
    public synchronized void reload() {
        sourceIterator = null;
        cachedElements = null;
        numCached = 0;
    }
}
