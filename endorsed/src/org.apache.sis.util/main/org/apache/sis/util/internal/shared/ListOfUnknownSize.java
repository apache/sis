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
package org.apache.sis.util.internal.shared;

import java.util.List;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Objects;


/**
 * An alternative to {@code AbstractList} for implementations having a costly {@link #size()} method.
 * This class overrides some methods in a way that avoid or reduce calls to {@link #size()}.
 *
 * <p>Despite extending {@link AbstractSequentialList}, this class expects implementations to override
 * the random access method {@link #get(int)} instead of {@link #listIterator(int)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <E>  the type of elements in the list.
 */
public abstract class ListOfUnknownSize<E> extends AbstractSequentialList<E> {
    /**
     * For subclass constructors.
     */
    protected ListOfUnknownSize() {
    }

    /**
     * Returns {@link #size()} if its value is already known, or a negative value if the size is still unknown.
     * The size may become known for example if it has been cached by the subclass. In such case,
     * some {@code ListOfUnknownSize} methods will take a more efficient path.
     *
     * @return {@link #size()} if its value is already known, or any negative value if it still costly to compute.
     */
    protected int sizeIfKnown() {
        return -1;
    }

    /**
     * Returns the number of elements in this list. The default implementation counts the number of elements
     * for which {@link #exists(int)} returns {@code true}. Subclasses are encouraged to cache this value if
     * they know that the underlying storage is immutable.
     *
     * @return the number of elements in this list.
     */
    @Override
    public int size() {
        int size = sizeIfKnown();
        if (size < 0) {
            size = 0;
            while (exists(size)) {
                if (++size == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
        return size;
    }

    /**
     * Returns {@code true} if this list is empty.
     * This method avoids to invoke {@link #size()} unless it is cheap.
     *
     * @return {@code true} if this list is empty.
     */
    @Override
    public boolean isEmpty() {
        final int size = sizeIfKnown();
        return (size == 0) || (size < 0 && !exists(0));
    }

    /**
     * Returns {@code true} if an element exists at the given index. If an element at index <var>i</var> exists,
     * then all elements at index 0 to <var>i</var> - 1 also exist. Those elements do not need to be computed
     * immediately if their computation is deferred.
     *
     * @param  index  the index where to verify if an element exists.
     * @return {@code true} if an element exists at the given index.
     */
    protected abstract boolean exists(int index);

    /**
     * Returns the element at the specified index.
     * Invoking this method may trig computation of the element if their computation is deferred.
     *
     * @param  index  position of the element to get in this list.
     * @return the element at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public abstract E get(int index);

    /**
     * Removes elements of the given collection from this list.
     * This method avoids to invoke {@link #size()}.
     *
     * @param  c  the collection containing elements to remove.
     * @return {@code true} if at least one element has been removed.
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        // See comment in SetOfUnknownSize.removeAll(…).
        boolean modified = false;
        for (final java.util.Iterator<?> it = c.iterator(); it.hasNext();) {
            modified |= remove(it.next());
        }
        return modified;
    }

    /**
     * Returns a list iterator over the elements in this list.
     * The default implementation invokes {@link #exists(int)} and {@link #get(int)}.
     * Write operations are not supported.
     *
     * @param  index  index of first element to be returned from the list.
     * @return a list iterator over the elements in this list.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public ListIterator<E> listIterator(final int index) {
        return new Iterator(index);
    }

    /**
     * The iterator returned by {@link #listIterator()}.  Provided as a named class instead
     * than anonymous class for more readable stack traces. This is especially useful since
     * elements may be loaded or computed when first needed, and those operations may fail.
     */
    private final class Iterator implements ListIterator<E> {
        /** Index of the next element to be returned. */
        private int cursor;

        /** Creates a new iterator starting at the given index. */
        Iterator(final int index) {
            cursor = index;
        }

        /** Index of element to be returned by {@link #next()}. */
        @Override public int nextIndex() {
            return cursor;
        }

        /** Whether {@link #next()} can succeed. */
        @Override public boolean hasNext() {
            final int size = sizeIfKnown();
            return (size >= 0) ? cursor < size : exists(cursor);
        }

        /** Move forward by one element. */
        @Override public E next() {
            final E element;
            try {
                element = get(cursor);
            } catch (IndexOutOfBoundsException e) {
                throw (NoSuchElementException) new NoSuchElementException().initCause(e);
            }
            cursor++;           // Set only on success.
            return element;
        }

        /** Index of element to be returned by {@link #previous()}. */
        @Override public int previousIndex() {
            return cursor - 1;
        }

        /** Whether {@link #previous()} can succeed. */
        @Override public boolean hasPrevious() {
            return cursor != 0;
        }

        /** Move backward by one element. */
        @Override public E previous() {
            if (cursor != 0) return get(--cursor);
            throw new NoSuchElementException();
        }

        @Override public void set(E e) {throw new UnsupportedOperationException();}
        @Override public void add(E e) {throw new UnsupportedOperationException();}
        @Override public void remove() {throw new UnsupportedOperationException();}
    }

    /**
     * Creates a {@code Spliterator} without knowledge of collection size.
     *
     * @return a {@code Spliterator} over the elements in this collection.
     */
    @Override
    public Spliterator<E> spliterator() {
        return sizeIfKnown() >= 0 ? super.spliterator() : Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
    }

    /**
     * Returns the elements in an array.
     *
     * @return an array containing all list elements.
     */
    @Override
    public Object[] toArray() {
        return sizeIfKnown() >= 0 ? super.toArray() : SetOfUnknownSize.toArray(iterator(), new Object[32], true);
    }

    /**
     * Returns the elements in the given array, or in a new array of the same type
     * if it was necessary to allocate more space.
     *
     * @param  <T>    the type array elements.
     * @param  array  where to store the elements.
     * @return an array containing all list elements.
     */
    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(final T[] array) {
        return sizeIfKnown() >= 0 ? super.toArray(array) : SetOfUnknownSize.toArray(iterator(), array, false);
    }

    /**
     * Returns {@code true} if the given object is also a list and the two lists have the same content.
     * This method avoids to invoke {@link #size()} on this instance.
     *
     * @param  object  the object to compare with this list.
     * @return {@code true} if the two list have the same content.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof List<?>)) {
            return false;
        }
        final java.util.Iterator<E> it = iterator();
        final java.util.Iterator<?> ot = ((List<?>) object).iterator();
        while (it.hasNext()) {
            if (!ot.hasNext() || !Objects.equals(it.next(), ot.next())) {
                return false;
            }
        }
        return !ot.hasNext();
    }
}
