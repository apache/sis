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
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class SingletonValue extends AbstractList<DefaultAttribute<?>> {
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
    SingletonValue(final Map<String, Object> properties, final String key) {
        this.properties = properties;
        this.key = key;
    }

    /**
     * Returns 1 or 0, depending on whether or not an attribute is associated to the key.
     */
    @Override
    public int size() {
        return properties.get(key) != null ? 1 : 0;
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
        if (index == 0) {
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
        if (index == 0) {
            if (properties.putIfAbsent(key, element) == null) {
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
     */
    @Override
    public  DefaultAttribute<?> remove(final int index) {
        if (index == 0) {
            final Object previous = properties.remove(key);
            if (previous != null) {
                return (DefaultAttribute<?>) previous;
            }
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Removes the attribute associated to the key.
     */
    @Override
    public void clear() {
        properties.remove(key);
    }

    /**
     * Returns an iterator over the unique element in this list.
     */
    @Override
    public Iterator<DefaultAttribute<?>> iterator() {
        return new Iter((DefaultAttribute<?>) properties.get(key));
    }

    /**
     * Implementation of the iterator returned by {@link SingletonValue#iterator()}.
     */
    private static final class Iter implements Iterator<DefaultAttribute<?>> {
        /**
         * The attribute to return, or {@code null} if we reached the iteration end.
         */
        private DefaultAttribute<?> element;

        /**
         * Creates a new iterator which will return the given attribute.
         */
        Iter(final DefaultAttribute<?> element) {
            this.element = element;
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
    }
}
