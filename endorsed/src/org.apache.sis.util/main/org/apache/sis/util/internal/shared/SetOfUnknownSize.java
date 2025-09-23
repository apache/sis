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

import java.util.Set;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import org.apache.sis.util.ArraysExt;


/**
 * An alternative to {@code AbstractSet} for implementations having a costly {@link #size()} method.
 * This class overrides some methods in a way that avoid or reduce calls to {@link #size()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <E>  the type of elements in the set.
 */
public abstract class SetOfUnknownSize<E> extends AbstractSet<E> {
    /**
     * For subclass constructors.
     */
    protected SetOfUnknownSize() {
    }

    /**
     * Returns {@code true} if the {@link #size()} method is cheap. This is sometimes the case
     * when {@code size()} has already been invoked and the subclasses cached the result.
     *
     * @return {@code true} if the {@link #size()} method is cheap.
     */
    protected boolean isSizeKnown() {
        return false;
    }

    /**
     * Returns {@code true} if this set is empty.
     * This method avoids to invoke {@link #size()} unless it is cheap.
     *
     * @return {@code true} if this set is empty.
     */
    @Override
    public boolean isEmpty() {
        return isSizeKnown() ? super.isEmpty() : !iterator().hasNext();
    }

    /**
     * Returns the number of elements in this set. The default implementation counts the number of elements
     * returned by the {@linkplain #iterator() iterator}. Subclasses are encouraged to cache this value
     * if they know that the underlying storage is immutable.
     *
     * @return the number of elements in this set.
     */
    @Override
    public int size() {
        int count = 0;
        for (final Iterator<E> it=iterator(); it.hasNext();) {
            it.next();
            if (++count == Integer.MAX_VALUE) {
                break;
            }
        }
        return count;
    }

    /**
     * Removes elements of the given collection from this set.
     * This method avoids to invoke {@link #size()}.
     *
     * @param  c  the collection containing elements to remove.
     * @return {@code true} if at least one element has been removed.
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        /*
         * Do not invoke super.removeAll(c) even if isSizeKnown() returns `true` because we want to unconditionally
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
     * Creates a {@code Spliterator} without knowledge of collection size.
     *
     * @return a {@code Spliterator} over the elements in this collection.
     */
    @Override
    public Spliterator<E> spliterator() {
        return isSizeKnown() ? super.spliterator() : Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    /**
     * Returns the elements in an array.
     *
     * @return an array containing all set elements.
     */
    @Override
    public Object[] toArray() {
        return isSizeKnown() ? super.toArray() : toArray(iterator(), new Object[32], true);
    }

    /**
     * Returns the elements in the given array, or in a new array of the same type
     * if it was necessary to allocate more space.
     *
     * @param  <T>    the type array elements.
     * @param  array  where to store the elements.
     * @return an array containing all set elements.
     */
    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(final T[] array) {
        return isSizeKnown() ? super.toArray(array) : toArray(iterator(), array, false);
    }

    /**
     * Implementation of the public {@code toArray()} methods without call to {@link #size()}.
     */
    @SuppressWarnings("unchecked")
    static <T> T[] toArray(final Iterator<?> it, T[] array, boolean trimToSize) {
        int i = 0;
        while (it.hasNext()) {
            if (i >= array.length) {
                if (i >= Integer.MAX_VALUE >>> 1) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                array = Arrays.copyOf(array, Math.max(16, array.length) << 1);
                trimToSize = true;
            }
            array[i++] = (T) it.next();     // Will throw an ArrayStoreException if the type is incorrect.
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
     * This method avoids to invoke {@link #size()} on this instance (but it still call that method
     * on the other instance).
     *
     * @param  object  the object to compare with this set.
     * @return {@code true} if the two set have the same content.
     */
    @Override
    public boolean equals(final Object object) {
        /*
         * Do not invoke super.equals(object) even if isSizeKnown() returns `true` because we want to unconditionally
         * iterate over the elements of this Set. The reason is that this Set may compute the values dynamically and
         * it is sometimes difficult to ensure that this Set's iterator is fully consistent with the values recognized
         * by the contains(Object) method. For example, the iterator may return "EPSG:4326" while the contains(Object)
         * method may accept both "EPSG:4326" and "EPSG:4326". For this equal(Object) method, we consider the
         * contains(Object) method of the other Set as more reliable.
         */
        if (object == this) {
            return true;
        }
        if (!(object instanceof Set<?>)) {
            return false;
        }
        final var that = (Set<?>) object;
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
