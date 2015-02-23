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


/**
 * An immutable set built from an iterator, which will be filled only when needed.
 * This implementation do <strong>not</strong> check if all elements in the iterator
 * are really unique; we assume that it was already verified by the caller.
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class LazySet<E> extends AbstractSet<E> {
    /**
     * The iterator to use for filling this set, or {@code null} if the iteration is over.
     */
    private Iterator<? extends E> iterator;

    /**
     * The elements in this set. This array will grown as needed.
     */
    private E[] elements;

    /**
     * The current position in the iteration. This position will be incremented as long as
     * there is some elements remaining in the iterator.
     */
    private int position;

    /**
     * Constructs a set to be filled using the specified iterator.
     * Iteration in the given iterator will occurs only when needed.
     *
     * @param iterator The iterator to use for filling the set.
     */
    @SuppressWarnings("unchecked")
    public LazySet(final Iterator<? extends E> iterator) {
        this.iterator = iterator;
        elements = (E[]) new Object[4];
    }

    /**
     * Returns {@code true} if the {@link #iterator} is non-null and have more elements to return.
     */
    private boolean hasNext() {
        if (iterator != null) {
            if (iterator.hasNext()) {
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
        if (iterator != null) {
            while (iterator.hasNext()) {
                addNext();
            }
            iterator = null;
        }
        return position;
    }

    /**
     * Adds the next element from the iterator to this set. This method does not check if more
     * element were available; the check must have been done before to invoke this method.
     */
    private void addNext() {
        if (position >= elements.length) {
            elements = Arrays.copyOf(elements, position*2);
        }
        elements[position++] = iterator.next();
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
                addNext();
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
