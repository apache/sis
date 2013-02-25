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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Serializable;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A specialized {@code Set} implementation for use with {@link CodeList} values.
 * This implementation uses a bit mask for efficient storage.
 *
 * {@section Current limitation}
 * The current implementation is restricted to code list having a maximum of 64 values.
 * This restriction may be removed in a future Apache SIS version.
 *
 * @note
 *
 * @param <E> The type of code list elements in the set.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class CodeListSet<E extends CodeList<E>> extends AbstractSet<E>
        implements CheckedContainer<E>, Serializable
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3648460713432430695L;

    /**
     * The type of code list elements.
     */
    final Class<E>  elementType;

    /**
     * A bitmask of code list values present in this map.
     */
    long values;

    /**
     * All possible code list elements, fetched when first needed.
     * Note that this array may need to be fetched more than once,
     * because code list elements can be dynamically added.
     */
    transient E[] codes;

    /**
     * Creates an initially empty set for code lists of the given type.
     *
     * @param elementType The type of code list elements to be included in this set.
     */
    public CodeListSet(final Class<E> elementType) {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        this.elementType = elementType;
    }

    /**
     * Returns the type of code list elements in this set.
     *
     * @return The type of code list elements in this set.
     */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        values = 0;
    }

    /**
     * Returns {@code true} if this set does not contains any element.
     */
    @Override
    public boolean isEmpty() {
        return values == 0;
    }

    /**
     * Returns the number of elements in this set.
     *
     * @return The number of elements in this set.
     */
    @Override
    public int size() {
        return Long.bitCount(values);
    }

    /**
     * Adds the specified code list element in this set.
     *
     * @param  e The code list element to add in this set.
     * @return {@code true} if this set has been modified as a consequence of this method call.
     */
    @Override
    public boolean add(final E e) {
        final long mask = 1L << e.ordinal();
        if (mask == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IndexOutOfBounds_1, "ordinal"));
        }
        return values != (values |= mask);
    }

    /**
     * Removes the specified code list element from this set.
     * This methods does nothing if the given argument is {@code null} or is
     * not an instance of the code list class specified at construction time.
     *
     * @param  object The code list element to remove from this set.
     * @return {@code true} if this set has been modified as a consequence of this method call.
     */
    @Override
    public boolean remove(final Object object) {
        if (elementType.isInstance(object)) {
            return values != (values &= ~(1L << ((CodeList) object).ordinal()));
        }
        return false;
    }

    /**
     * Returns {@code true} if this set contains the given element.
     * This methods returns {@code false} if the given argument is {@code null} or
     * is not an instance of the code list class specified at construction time.
     *
     * @param  object The element to test for presence in this set.
     * @return {@code true} if the given object is contained in this set.
     */
    @Override
    public boolean contains(final Object object) {
        if (elementType.isInstance(object)) {
            return (values & (1L << ((CodeList) object).ordinal())) != 0;
        }
        return false;
    }

    /**
     * Returns {@code true} if this set contains all the elements of the given collection.
     *
     * @param  c The collection to be checked for containment in this set.
     * @return {@code true} if this set contains all elements of the given collection.
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            final CodeListSet<?> o = (CodeListSet) c;
            if (elementType == o.elementType) {
                return values == (values | o.values);
            }
        }
        return super.containsAll(c);
    }

    /**
     * Adds all elements of the given collection to this set.
     *
     * @param  c The collection containing elements to be added to this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean addAll(final Collection<? extends E> c) throws IllegalArgumentException {
        if (c instanceof CodeListSet) {
            // Following assertion should be ensured by parameterized types.
            assert elementType.isAssignableFrom(((CodeListSet) c).elementType);
            return values != (values |= ((CodeListSet) c).values);
        }
        return super.addAll(c);
    }

    /**
     * Adds all elements of the given collection from this set.
     *
     * @param  c The collection containing elements to be removed from this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            final CodeListSet<?> o = (CodeListSet) c;
            if (elementType == o.elementType) {
                return values != (values &= ~o.values);
            }
        }
        return super.removeAll(c);
    }

    /**
     * Retains only the elements of the given collection in this set.
     *
     * @param  c The collection containing elements to retain in this set.
     * @return {@code true} if this set changed as a result of this method call.
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (c instanceof CodeListSet) {
            final CodeListSet<?> o = (CodeListSet) c;
            if (elementType == o.elementType) {
                return values != (values &= o.values);
            }
        }
        return super.retainAll(c);
    }

    /**
     * Returns an iterator over the elements in this set.
     *
     * @return An iterator over the elements in this set.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iter();
    }

    /**
     * The iterator returned by {@link CodeListSet#iterator()}.
     */
    private final class Iter implements Iterator<E> {
        /**
         * Initialized to {@link CodeListSet#values}, then the bits are cleared as we
         * progress in the iteration. This value become 0 when the iteration is done.
         */
        private long remaining;

        /**
         * Mask with all bits set to 1 except the bit for the last value returned by {@link #next()}.
         * This mask is 0 if {@code next()} has not yet been invoked.
         */
        private long mask;

        /**
         * Creates a new iterator.
         */
        Iter() {
            remaining = values;
        }

        /**
         * Returns {@code true} if there is more elements to return.
         */
        @Override
        public boolean hasNext() {
            return remaining != 0;
        }

        /**
         * Returns the next element.
         */
        @Override
        public E next() {
            final int index = Long.numberOfLeadingZeros(remaining);
            if (index >= Long.SIZE) {
                throw new NoSuchElementException();
            }
            mask = ~(1L << index);
            remaining &= mask;
            E[] array = codes;
            if (array == null || index >= array.length) {
                codes = array = Types.getCodeValues(elementType);
            }
            return array[index];
        }

        /**
         * Removes the last element returned by this iterator.
         */
        @Override
        public void remove() {
            if (mask == 0) {
                throw new IllegalStateException();
            }
            values &= mask;
            mask = 0;
        }
    }
}
