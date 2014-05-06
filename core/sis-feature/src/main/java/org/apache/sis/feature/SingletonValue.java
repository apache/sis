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

import java.util.Map;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A list containing 0 or 1 attribute. This implementation is used in the very common case where a
 * {@link DefaultFeature} accepts at most one attribute for a given name. Its main purpose is to
 * reduce the amount of objects in memory, compared to using an {@link java.util.ArrayList}.
 *
 * <p>There is no need to keep long-lived references to instances of this class.
 * Instances can be recreated when needed.</p>
 *
 * {@section Non serialization}
 * This class is intentionally not serializable, since serializing this instance would imply serializing the whole
 * map of attributes if we want to keep the <cite>change in this list are reflected in the feature</cite> contract.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class SingletonValue extends AbstractList<DefaultAttribute<?>> {
    /**
     * An empty list of attributes.
     */
    private static final DefaultAttribute<?>[] EMPTY = new DefaultAttribute<?>[0];

    /**
     * The type of property elements in the list.
     */
    private final AbstractIdentifiedType type;

    /**
     * The map of properties in which to look for the attributes.
     * This is the same reference than {@link DefaultFeature#properties}.
     */
    private final Map<String, Object> properties;

    /**
     * The key for the attribute in the {@link #properties} map.
     */
    private final String key;

    /**
     * Creates a new list for the attribute associated to the given key in the given map.
     */
    SingletonValue(final AbstractIdentifiedType type, final Map<String, Object> properties, final String key) {
        this.type       = type;
        this.properties = properties;
        this.key        = key;
    }

    /**
     * Returns 1 or 0, depending on whether or not an attribute is associated to the key.
     */
    @Override
    public int size() {
        return properties.get(key) != null ? 1 : 0;
    }

    /**
     * Returns the index of the given element (which can only be 0), or -1 if not present.
     */
    @Override
    public int indexOf(final Object element) {
        return (element != null) && element.equals(properties.get(key)) ? 0 : -1;
    }

    /**
     * Returns the index of the given element (which can only be 0), or -1 if not present.
     */
    @Override
    public int lastIndexOf(final Object element) {
        return indexOf(element);
    }

    /**
     * Returns the attribute associated to the key, if present.
     */
    @Override
    public DefaultAttribute<?> get(final int index) {
        if (index == 0) {
            final Object element = properties.get(key);
            if (element != null) {
                return (DefaultAttribute<?>) element;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Sets the attribute associated to the key, if an instance already exists.
     */
    @Override
    public DefaultAttribute<?> set(final int index, final DefaultAttribute<?> element) {
        ensureNonNull("element", element);
        Validator.ensureValidType(type, element);
        if (index == 0) {
            modCount++;
            final Object previous = properties.put(key, element);
            if (previous != null) {
                return (DefaultAttribute<?>) previous;
            }
            if (properties.remove(key) != element) {
                throw new ConcurrentModificationException();
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Sets the attribute associated to the key, if no instance existed prior this method call.
     */
    @Override
    public void add(final int index, final DefaultAttribute<?> element) {
        ensureNonNull("element", element);
        Validator.ensureValidType(type, element);
        if (index == 0) {
            if (properties.putIfAbsent(key, element) == null) {
                modCount++;
                return;
            }
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, key));
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Sets the attribute associated to the key, if no instance existed prior this method call.
     */
    @Override
    public boolean add(final DefaultAttribute<?> element) {
        add(0, element);
        return true;
    }

    /**
     * Removes the attribute associated to the key.
     *
     * This method does not checks if the removal is allowed by the
     * {@linkplain DefaultAttributeType#getCardinality() cardinality}.
     * Such check can be performed by {@link DefaultFeature#validate()}.
     */
    @Override
    public DefaultAttribute<?> remove(final int index) {
        if (index == 0) {
            final Object previous = properties.remove(key);
            if (previous != null) {
                modCount++;
                return (DefaultAttribute<?>) previous;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Removes the singleton value, if presents.
     * This method is for {@link Iter#remove()} implementation only.
     *
     * @param  c The expected {@link #modCount} value, for check against concurrent modification.
     * @return {@code true} if the value has been removed.
     */
    final boolean clear(final int c) {
        if (c != modCount) {
            throw new ConcurrentModificationException();
        }
        return properties.remove(key) != null;
    }

    /**
     * Removes the attribute associated to the key.
     *
     * This method does not checks if the removal is allowed by the
     * {@linkplain DefaultAttributeType#getCardinality() cardinality}.
     * Such check can be performed by {@link DefaultFeature#validate()}.
     */
    @Override
    public void clear() {
        modCount++;
        properties.remove(key);
    }

    /**
     * Returns an array wrapping the singleton value, or an empty array if none.
     */
    @Override
    public Object[] toArray() {
        final Object element = properties.get(key);
        return (element == null) ? EMPTY : new DefaultAttribute<?>[] {(DefaultAttribute<?>) element};
    }

    /**
     * Returns an iterator over the unique element in this list.
     */
    @Override
    public Iterator<DefaultAttribute<?>> iterator() {
        return new Iter((DefaultAttribute<?>) properties.get(key), modCount);
    }

    /**
     * Implementation of the iterator returned by {@link SingletonValue#iterator()}.
     */
    private final class Iter implements Iterator<DefaultAttribute<?>> {
        /**
         * The attribute to return, or {@code null} if we reached the iteration end.
         */
        private DefaultAttribute<?> element;

        /**
         * Initial {@link SingletonValue#modCount} value, for checks against concurrent modifications.
         */
        private final int c;

        /**
         * Creates a new iterator which will return the given attribute.
         */
        Iter(final DefaultAttribute<?> element, final int c) {
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
         * Returns the singleton attribute, if present.
         */
        @Override
        public DefaultAttribute<?> next() {
            final DefaultAttribute<?> v = element;
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
            if (element != null || !clear(c)) {
                throw new IllegalStateException();
            }
        }
    }
}
