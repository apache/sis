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

import java.util.Set;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntFunction;
import org.apache.sis.util.ArraysExt;


/**
 * An alternative to {@code AbstractSet} for implementations having a costly {@code size()} method.
 * The size of the set is not known in advance, but may become known after one complete iteration.
 * This class overrides methods in a way that reduces or eliminates the need to invoke {@link #size()}.
 *
 * <h4>Note for implementers</h4>
 * Subclasses should implement the following methods:
 *
 * <ul>
 *   <li>{@link #iterator()}</li>
 *   <li>{@link #sizeIfKnown()} — optional but recommended for efficiency</li>
 *   <li>{@link #characteristics()} — if the implementation disallows null elements.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param <E>  the type of elements in the set.
 *
 * @since 1.6
 */
@SuppressWarnings("EqualsAndHashcode")
public abstract class SetOfUnknownSize<E> extends AbstractSet<E> {
    /**
     * Default initial size for {@code toArray(…)} methods.
     */
    static final int DEFAULT_INITIAL_SIZE = 16;

    /**
     * Creates a new set of initially unknown size.
     */
    protected SetOfUnknownSize() {
    }

    /**
     * Returns {@code true} if this set is empty.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @return {@code true} if this set is empty.
     */
    @Override
    public boolean isEmpty() {
        return sizeIfKnown().orElseGet(() -> iterator().hasNext() ? 1 : 0) == 0;
    }

    /**
     * Returns the number of elements in this set. The default implementation returns the
     * {@linkplain #sizeIfKnown() size if known}, or otherwise counts the number of elements
     * returned by the {@linkplain #iterator() iterator}.
     *
     * @return the number of elements in this set.
     */
    @Override
    public int size() {
        return sizeIfKnown().orElseGet(() -> {
            int count = 0;
            for (final Iterator<E> it = iterator(); it.hasNext();) {
                it.next();
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
            return count;
        });
    }

    /**
     * Returns the {@link #size()} value if it is already known, or empty if the size is still unknown.
     * The default implementation always returns an empty value. Subclasses can overwrite this method
     * if they have already computed and cached the number of elements. When this information is known,
     * some {@code SetOfUnknownSize} methods will take a more efficient path.
     *
     * @return {@link #size()} if its value is already known, or empty if this value is still costly to compute.
     */
    protected OptionalInt sizeIfKnown() {
        return OptionalInt.empty();
    }

    /**
     * Removes elements of the given collection from this set.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  c  the collection containing elements to remove.
     * @return {@code true} if at least one element has been removed.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean removeAll(final Collection<?> c) {
        /*
         * Do not invoke `super.removeAll(c)` even if `sizeIfKnown()` is present because we want to unconditionally
         * iterate over the elements of the given collection. The reason is that this Set may compute the values in
         * a dynamic way and it is sometimes difficult to ensure that the values returned by this Set's iterator are
         * fully consistent with the values recognized by contains(Object) and remove(Object) methods. Furthermore,
         * we want the operation to fail fast in the common case where the remove(Object) method is unsupported.
         */
        boolean modified = false;
        for (final Iterator<?> it = c.iterator(); it.hasNext();) {
            modified |= remove(it.next());
        }
        return modified;
    }

    /**
     * Returns characteristics of this collection as a combination of {@code Spliterator} bits.
     * The default implementation returns {@link Spliterator#DISTINCT}. Subclasses should add the
     * {@link Spliterator#NONNULL} bit if they guarantee that this set does not contain the {@code null} element.
     *
     * @return the characteristics of this collection.
     *
     * @see Spliterator#DISTINCT
     * @see Spliterator#NONNULL
     * @see Spliterator#IMMUTABLE
     * @see Spliterator#characteristics()
     */
    protected int characteristics() {
        return Spliterator.DISTINCT;
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
     * @return an array containing all set elements.
     */
    @Override
    public Object[] toArray() {
        return toArray(iterator(), new Object[sizeIfKnown().orElse(DEFAULT_INITIAL_SIZE)], true);
    }

    /**
     * Returns the elements in the given array, or in a new array if it was necessary to allocate more space.
     * If the given array is larger than necessary, the remaining array elements are set to {@code null}.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  <T>    the type array elements.
     * @param  array  where to store the elements.
     * @return an array containing all set elements.
     */
    @Override
    public <T> T[] toArray(final T[] array) {
        return toArray(iterator(), array, false);
    }

    /**
     * Returns the elements in an array generated by the given function.
     * The implementation of this method avoids to invoke {@link #size()}.
     *
     * @param  <T>        the type array elements.
     * @param  generator  the function for allocating a new array.
     * @return an array containing all set elements.
     */
    @Override
    public <T> T[] toArray(final IntFunction<T[]> generator) {
        return toArray(iterator(), generator.apply(sizeIfKnown().orElse(DEFAULT_INITIAL_SIZE)), true);
    }

    /**
     * Implementation of the public {@code toArray()} methods without call to {@link #size()}.
     *
     * @param  <E>         type of elements in the collection.
     * @param  it          iterator providing the elements.
     * @param  array       where to store the elements.
     * @param  trimToSize  whether to trim the array to the actual number of elements.
     * @return the elements.
     */
    @SuppressWarnings("unchecked")
    static <E> E[] toArray(final Iterator<?> it, E[] array, boolean trimToSize) {
        int i = 0;
        while (it.hasNext()) {
            if (i >= array.length) {
                int length = array.length << 1;
                if (length < 0) {   // Overflow.
                    length = Integer.MAX_VALUE;
                    if (length == array.length) break;  // Stop after we reached the maximal capacity of an array.
                }
                array = Arrays.copyOf(array, Math.max(DEFAULT_INITIAL_SIZE, length));
                trimToSize = true;
            }
            array[i++] = (E) it.next();     // Will throw an ArrayStoreException if the type is incorrect.
        }
        if (trimToSize) {
            array = ArraysExt.resize(array, i);
        } else {
            Arrays.fill(array, i, array.length, null);
        }
        return array;
    }

    /**
     * Returns {@code true} if the given object is also a set and the two sets have the same content.
     * The implementation of this method avoids to invoke {@link #size()} on this instance.
     * However, it still call {@code other.size()}.
     *
     * @param  other  the object to compare with this set.
     * @return {@code true} if the two set have the same content.
     */
    @Override
    public boolean equals(final Object other) {
        /*
         * Do not invoke `super.equals(object)` even if `sizeIfKnown()` is present because we want to unconditionally
         * iterate over the elements of this Set. The reason is that this Set may compute the values dynamically and
         * it is sometimes difficult to ensure that this Set's iterator is fully consistent with the values recognized
         * by the contains(Object) method. For example, the iterator may return "EPSG:4326" while the contains(Object)
         * method may accept both "EPSG:4326" and "EPSG:4326". For this equal(Object) method, we consider the
         * contains(Object) method of the other Set as more reliable.
         */
        if (other == this) {
            return true;
        }
        if (!(other instanceof Set<?>)) {
            return false;
        }
        final var that = (Set<?>) other;
        int size = 0;
        for (final Iterator<E> it = iterator(); it.hasNext();) {
            if (!that.contains(it.next())) {
                return false;
            }
            size++;
        }
        return size == that.size();
    }
}
