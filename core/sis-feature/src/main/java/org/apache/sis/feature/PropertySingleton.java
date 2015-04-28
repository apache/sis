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
package org.apache.sis.feature;

import java.util.List;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A list containing 0 or 1 value. This implementation is used in the very common case where a
 * {@link AbstractAttribute} accepts at most one value. Its main purpose is to reduce the amount
 * of objects in memory compared to {@link java.util.ArrayList}.
 *
 * <p>There is no need to keep long-lived references to instances of this class.
 * Instances can be recreated when needed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class PropertySingleton<V> extends AbstractList<V> {
    /**
     * The property where to read and write the value.
     */
    private final Field<V> property;

    /**
     * Creates a new list for the value of the given property.
     */
    PropertySingleton(final Field<V> property) {
        this.property = property;
    }

    /**
     * Returns 1 or 0, depending on whether or not a value exists.
     */
    @Override
    public int size() {
        return property.getValue() == null ? 0 : 1;
    }

    /**
     * Returns the index of the given element (which can only be 0), or -1 if not present.
     */
    @Override
    public int indexOf(final Object element) {
        return (element != null) && element.equals(property.getValue()) ? 0 : -1;
    }

    /**
     * Returns the index of the given element (which can only be 0), or -1 if not present.
     */
    @Override
    public int lastIndexOf(final Object element) {
        return indexOf(element);
    }

    /**
     * Returns the property value, if present.
     */
    @Override
    public V get(final int index) {
        if (index == 0) {
            final V element = property.getValue();
            if (element != null) {
                return element;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Sets the property value, if an instance already exists.
     */
    @Override
    public V set(final int index, final V element) {
        ensureNonNull("element", element);
        if (index == 0) {
            final V previous = property.getValue();
            if (previous != null) {
                property.setValue(element);
                modCount++;
                return previous;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Sets the property value, if no instance existed prior this method call.
     */
    @Override
    public void add(final int index, final V element) {
        if (index != 0) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
        }
        add(element);
    }

    /**
     * Sets the property value, if no instance existed prior this method call.
     */
    @Override
    public boolean add(final V element) {
        ensureNonNull("element", element);
        if (property.getValue() == null) {
            property.setValue(element);
            modCount++;
            return true;
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, property.getName()));
    }

    /**
     * Removes the property value.
     *
     * This method does not checks if the removal is allowed by the
     * {@linkplain DefaultAttributeType#getMinimumOccurs() cardinality}.
     * Such check can be performed by {@link AbstractFeature#quality()}.
     */
    @Override
    public V remove(final int index) {
        if (index == 0) {
            final V previous = property.getValue();
            if (previous != null) {
                clear();
                return previous;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Removes the singleton value, if presents.
     * This method is for {@link Iter#remove()} implementation only.
     *
     * @param  c The expected {@link #modCount} value, for check against concurrent modification.
     */
    final void clear(final int c) {
        if (c != modCount) {
            throw new ConcurrentModificationException(String.valueOf(property.getName()));
        }
        property.setValue(null);
    }

    /**
     * Removes the property value.
     *
     * This method does not checks if the removal is allowed by the
     * {@linkplain DefaultAttributeType#getMinimumOccurs() cardinality}.
     * Such check can be performed by {@link AbstractFeature#quality()}.
     */
    @Override
    public void clear() {
        property.setValue(null);
        modCount++;
    }

    /**
     * Returns an array wrapping the singleton value, or an empty array if none.
     */
    @Override
    public Object[] toArray() {
        final V element = property.getValue();
        return (element == null) ? new Object[0] : new Object[] {element};
    }

    /**
     * Same contract than {@link AbstractList}, just slightly more efficient for this particular class.
     */
    @Override
    public int hashCode() {
        final V element = property.getValue();
        final int hashCode = (element != null) ? 31 + element.hashCode() : 1;
        assert hashCode == super.hashCode() : hashCode;
        return hashCode;
    }

    /**
     * Same contract than {@link AbstractList}, just slightly more efficient for this particular class.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof List<?>) {
            final V element = property.getValue();
            if (element == null) {
                return ((List<?>) other).isEmpty();
            } else {
                final Iterator<?> it = ((List<?>) other).iterator();
                return it.hasNext() && element.equals(it.next()) && !it.hasNext();
            }
        }
        return false;
    }

    /**
     * Returns an iterator over the unique element in this list.
     */
    @Override
    public Iterator<V> iterator() {
        return new Iter(property.getValue(), modCount);
    }

    /**
     * Implementation of the iterator returned by {@link PropertySingleton#iterator()}.
     */
    private final class Iter implements Iterator<V> {
        /**
         * The property value to return, or {@code null} if we reached the iteration end.
         */
        private V element;

        /**
         * Initial {@link PropertySingleton#modCount} value, for checks against concurrent modifications.
         */
        private final int c;

        /**
         * Creates a new iterator which will return the given attribute.
         */
        Iter(final V element, final int c) {
            this.element = element;
            this.c = c;
        }

        /**
         * Returns {@code true} if the singleton attribute has not yet been returned.
         */
        @Override
        public boolean hasNext() {
            return element != null;
        }

        /**
         * Returns the singleton value, if present.
         */
        @Override
        public V next() {
            final V v = element;
            if (v == null) {
                throw new NoSuchElementException();
            }
            element = null;
            return v;
        }

        /**
         * Removes the value returned by the last call to {@link #next()}.
         */
        @Override
        public void remove() {
            if (element != null) {
                throw new IllegalStateException();
            }
            clear(c);
        }
    }
}
