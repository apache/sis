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
package org.apache.sis.util.collection;

import java.util.List;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntFunction;
import static org.apache.sis.util.collection.SetOfUnknownSize.DEFAULT_INITIAL_SIZE;


/**
 * An alternative to {@code AbstractList} for implementations having a costly {@code size()} method.
 * The size of the set is not known in advance, but may become known after one complete iteration.
 * This class overrides methods in a way that reduces or eliminates the need to invoke {@link #size()}.
 *
 * <h4>Note for implementers</h4>
 * Despite extending {@link AbstractSequentialList}, subclasses are not expected to overwrite the
 * {@link #listIterator(int)} method. Instead, subclasses should implement the following methods:
 *
 * <ul>
 *   <li>{@link #get(int)}</li>
 *   <li>{@link #isValidIndex(int)}</li>
 *   <li>{@link #sizeIfKnown()} — optional but recommended for efficiency</li>
 *   <li>{@link #characteristics()} — if the implementation disallows null elements.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param <E>  the type of elements in the list.
 *
 * @since 1.6
 */
@SuppressWarnings("EqualsAndHashcode")
public abstract class ListOfUnknownSize<E> extends AbstractSequentialList<E> {
    /**
     * Creates a new list of initially unknown size.
     */
    protected ListOfUnknownSize() {
    }

    /**
     * Returns {@code true} if this list is empty.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @return {@code true} if this list is empty.
     */
    @Override
    public boolean isEmpty() {
        return sizeIfKnown().orElseGet(() -> isValidIndex(0) ? 1 : 0) == 0;
    }

    /**
     * Returns the number of elements in this list. The default implementation returns the
     * {@linkplain #sizeIfKnown() size if known}, or otherwise searches the smallest value
     * for which {@link #isValidIndex(int)} returns {@code false}.
     *
     * @return the number of elements in this list.
     */
    @Override
    public int size() {
        OptionalInt size;
        int i = 1;
        do {
            size = sizeIfKnown();
            if (size.isPresent()) {
                return size.getAsInt();
            }
        } while (isValidIndex(i) && (i <<= 1) >= 0);
        // Following is inefficient, but this fallback should not happen with Apache SIS subclasses.
        i >>>= 1;
        while (isValidIndex(++i)) {
            if (i == Integer.MAX_VALUE) break;
        }
        return i;
    }

    /**
     * Returns the {@link #size()} value if it is already known, or empty if the size is still unknown.
     * The default implementation always returns an empty value. Subclasses can overwrite this method
     * if they have already computed and cached the number of elements. When this information is known,
     * some {@code ListOfUnknownSize} methods will take a more efficient path.
     *
     * @return {@link #size()} if its value is already known, or empty if this value is still costly to compute.
     */
    protected OptionalInt sizeIfKnown() {
        return OptionalInt.empty();
    }

    /**
     * Returns {@code true} if the given index is valid for this list. If the index <var>i</var> is valid,
     * then all indices from 0 to <var>i</var> - 1 inclusive are presumed also valid. Invoking this method
     * is usually less expansive than testing {@code index < size()} when the size is costly to compute.
     *
     * @param  index  the index where to verify if an element exists.
     * @return {@code true} if an element exists at the given index.
     */
    protected abstract boolean isValidIndex(int index);

    /**
     * Returns the element at the specified index.
     * Invoking this method may trig computation of the element if these computations are deferred.
     *
     * @param  index  position of the element to get in this list.
     * @return the element at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public abstract E get(int index);

    /**
     * Removes elements of the given collection from this list.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  c  the collection containing elements to remove.
     * @return {@code true} if at least one element has been removed.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
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
     * The default implementation delegates to {@link #isValidIndex(int)} and {@link #get(int)}.
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
     * of an anonymous class for more readable stack traces. This is especially useful since
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
            return isValidIndex(cursor);
        }

        /** Move forward by one element. */
        @Override public E next() {
            final E element;
            try {
                element = get(cursor);
            } catch (IndexOutOfBoundsException e) {
                // TODO: simplify when we are allowed to compile for JDK15.
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
     * Returns characteristics of this collection as a combination of {@code Spliterator} bits.
     * The default implementation returns {@link Spliterator#ORDERED}. Subclasses should add the
     * {@link Spliterator#NONNULL} bit if they guarantee that this list does not contain any {@code null} element.
     *
     * @return the characteristics of this collection.
     *
     * @see Spliterator#ORDERED
     * @see Spliterator#NONNULL
     * @see Spliterator#IMMUTABLE
     * @see Spliterator#characteristics()
     */
    protected int characteristics() {
        return Spliterator.ORDERED;
    }

    /**
     * Creates a {@code Spliterator} which may be of unknown size.
     *
     * @return a {@code Spliterator} over the elements in this collection.
     */
    @Override
    public Spliterator<E> spliterator() {
        final int characteristics = characteristics();
        final OptionalInt size = sizeIfKnown();
        return size.isPresent()
                ? Spliterators.spliterator(iterator(), size.getAsInt(), characteristics)
                : Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }

    /**
     * Returns all elements in an array.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @return an array containing all list elements.
     */
    @Override
    public Object[] toArray() {
        return SetOfUnknownSize.toArray(iterator(), new Object[sizeIfKnown().orElse(DEFAULT_INITIAL_SIZE)], true);
    }

    /**
     * Returns the elements in the given array, or in a new array if it was necessary to allocate more space.
     * If the given array is larger than necessary, the remaining array elements are set to {@code null}.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  <T>    the type array elements.
     * @param  array  where to store the elements.
     * @return an array containing all list elements.
     */
    @Override
    public <T> T[] toArray(final T[] array) {
        return SetOfUnknownSize.toArray(iterator(), array, false);
    }

    /**
     * Returns the elements in an array generated by the given function.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  <T>        the type array elements.
     * @param  generator  the function for allocating a new array.
     * @return an array containing all list elements.
     */
    @Override
    public <T> T[] toArray(final IntFunction<T[]> generator) {
        return SetOfUnknownSize.toArray(iterator(), generator.apply(sizeIfKnown().orElse(DEFAULT_INITIAL_SIZE)), true);
    }

    /**
     * Returns {@code true} if the given object is also a list and the two lists have the same content.
     * This method avoids to invoke {@link #size()} on this instance.
     *
     * @param  object  the object to compare with this list.
     * @return {@code true} if the two list have the same content.
     * @hidden because nothing new to said compared to general contract.
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
