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

import java.util.Arrays;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import org.apache.sis.util.Workaround;


/**
 * An immutable set built from an iterator, which will be filled only when needed.
 * This implementation does <strong>not</strong> check if all elements in the iterator
 * are really unique; we assume that this condition was already verified by the caller.
 *
 * <p>One usage of {@code LazySet} is to workaround a {@link java.util.ServiceLoader} bug which block usage of two
 * {@link Iterator} instances together: the first iteration must be fully completed or abandoned before we can start
 * a new iteration. See
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#DefaultMathTransformFactory()}.</p>
 *
 * <p>This class is not thread-safe. Synchronization, if desired, shall be done by the caller.</p>
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.6
 * @version 0.6
 * @module
 */
@Workaround(library="JDK", version="1.8.0_31-b13")
public final class LazySet<E> extends AbstractSet<E> {
    /**
     * The original source of elements, or {@code null} if unknown.
     */
    public final Iterable<? extends E> source;

    /**
     * The iterator to use for filling this set, or {@code null} if the iteration did not started yet
     * or is finished. Those two cases can be distinguished by looking whether the {@link #elements}
     * array is null or not.
     */
    private Iterator<? extends E> iterator;

    /**
     * The elements in this set, or {@code null} if the iteration did not started yet.
     * After the iteration started, this array will grow as needed.
     */
    private E[] elements;

    /**
     * The current position in the iteration. This position will be incremented as long as
     * there is some elements remaining in the iterator.
     */
    private int position;

    /**
     * Constructs a set to be filled by the elements from the specified source. Iteration will starts
     * only when first needed, and at most one iteration will be performed (unless {@link #reload()}
     * is invoked).
     *
     * @param source The source of elements to use for filling the set.
     */
    @SuppressWarnings("unchecked")
    public LazySet(final Iterable<? extends E> source) {
        this.source = source;
    }

    /**
     * Constructs a set to be filled using the specified iterator.
     * Iteration in the given iterator will occurs only when needed.
     *
     * @param iterator The iterator to use for filling the set.
     */
    @SuppressWarnings("unchecked")
    public LazySet(final Iterator<? extends E> iterator) {
        source = null;
        this.iterator = iterator;
        elements = (E[]) new Object[4];
    }

    /**
     * Notify this {@code LazySet} that it should re-fetch the elements from the {@linkplain #source}.
     */
    public void reload() {
        if (source != null) {
            iterator = null;
            elements = null;
            position = 0;
        }
    }

    /**
     * Returns the iterator over the source elements, or {@code null} if the iteration is finished.
     */
    @SuppressWarnings("unchecked")
    private Iterator<? extends E> sourceElements() {
        if (iterator == null && elements == null && source != null) {
            iterator = source.iterator();
            elements = (E[]) new Object[4];
        }
        return iterator;
    }

    /**
     * Returns {@code true} if the {@link #iterator} is non-null and have more elements to return.
     */
    private boolean hasNext() {
        final Iterator<? extends E> it = sourceElements();
        if (it != null) {
            if (it.hasNext()) {
                return true;
            }
            iterator = null;
        }
        return false;
    }

    /**
     * Tests if this set has no element.
     *
     * @return {@code true} if this set has no element.
     */
    @Override
    public boolean isEmpty() {
        return (position == 0) && !hasNext();
    }

    /**
     * Returns the number of elements in this set. Invoking this method
     * forces the set to immediately iterates through all remaining elements.
     *
     * @return Number of elements in the iterator.
     */
    @Override
    public int size() {
        final Iterator<? extends E> it = sourceElements();
        if (it != null) {
            while (it.hasNext()) {
                cache(it.next());
            }
            iterator = null;
        }
        return position;
    }

    /**
     * Adds the given element to the {@link #elements} array.
     */
    private void cache(final E element) {
        if (position >= elements.length) {
            elements = Arrays.copyOf(elements, position*2);
        }
        elements[position++] = element;
    }

    /**
     * Returns {@code true} if an element exists at the given index.
     * The element is not loaded immediately.
     *
     * <p><strong>NOTE: This method is for use by iterators only.</strong>
     * It is not suited for more general usage since it does not check for
     * negative index and for skipped elements.</p>
     */
    final boolean exists(final int index) {
        return (index < position) || hasNext();
    }

    /**
     * Returns the element at the specified position in this set.
     *
     * @param index The index at which to get an element.
     * @return The element at the requested index.
     */
    final E get(final int index) {
        if (index >= position) {
            if (hasNext()) {
                cache(iterator.next());
            } else {
                throw new NoSuchElementException();
            }
        }
        return elements[index];
    }

    /**
     * Returns an iterator over the elements contained in this set.
     * This is not the same iterator than the one given to the constructor.
     *
     * @return An iterator over the elements in this set.
     */
    @Override
    public Iterator<E> iterator() {
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
}
