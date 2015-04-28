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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.internal.util.AbstractMapEntry;

import static org.apache.sis.metadata.PropertyAccessor.RETURN_NULL;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_PREVIOUS;


/**
 * A view of a metadata object as a map. Keys are property names and values
 * are the value returned by the {@code getFoo()} method using reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)
 */
final class ValueMap extends PropertyMap<Object> {
    /**
     * The metadata object to wrap.
     */
    final Object metadata;

    /**
     * The behavior of this map toward null or empty values.
     */
    final ValueExistencePolicy valuePolicy;

    /**
     * Creates a map of values for the specified metadata and accessor.
     *
     * @param metadata    The metadata object to wrap.
     * @param accessor    The accessor to use for the metadata.
     * @param keyPolicy   Determines the string representation of keys in the map..
     * @param valuePolicy The behavior of this map toward null or empty values.
     */
    ValueMap(final Object metadata, final PropertyAccessor accessor,
            final KeyNamePolicy keyPolicy, final ValueExistencePolicy valuePolicy)
    {
        super(accessor, keyPolicy);
        this.metadata    = metadata;
        this.valuePolicy = valuePolicy;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return accessor.count(metadata, valuePolicy, PropertyAccessor.COUNT_FIRST) == 0;
    }

    /**
     * Returns the number of elements in this map.
     */
    @Override
    public int size() {
        return accessor.count(metadata, valuePolicy, PropertyAccessor.COUNT_SHALLOW);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    @Override
    public Object get(final Object key) {
        if (key instanceof String) {
            final Object value = accessor.get(accessor.indexOf((String) key, false), metadata);
            if (!valuePolicy.isSkipped(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @throws IllegalArgumentException if the given key is not the name of a property in the metadata.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    public Object put(final String key, final Object value) {
        final Object old = accessor.set(accessor.indexOf(key, true), metadata, value, RETURN_PREVIOUS);
        return valuePolicy.isSkipped(old) ? null : old;
    }

    /**
     * Puts every entries from the given map. This method is overloaded for performance reasons
     * since we are not interested in the return value of the {@link #put(String, Object)} method.
     *
     * @throws IllegalArgumentException if at least one key is not the name of a property in the metadata.
     * @throws ClassCastException if at least one value is not of the expected type.
     * @throws UnmodifiableMetadataException if at least one property is read-only.
     */
    @Override
    public void putAll(final Map<? extends String, ?> map) {
        for (final Map.Entry<? extends String, ?> e : map.entrySet()) {
            accessor.set(accessor.indexOf(e.getKey(), true), metadata, e.getValue(), RETURN_NULL);
        }
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    public Object remove(final Object key) throws UnsupportedOperationException {
        if (key instanceof String) {
            final Object old = accessor.set(accessor.indexOf((String) key, false), metadata, null, RETURN_PREVIOUS);
            if (!valuePolicy.isSkipped(old)) {
                return old;
            }
        }
        return null;
    }

    /**
     * Returns a view of the mappings contained in this map.
     */
    @Override
    public Set<Map.Entry<String,Object>> entrySet() {
        if (entrySet == null) {
            entrySet = new Entries();
        }
        return entrySet;
    }

    /**
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,Object>> iterator() {
        return new Iter();
    }




    /**
     * A map entry for a given property.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    final class Property extends AbstractMapEntry<String,Object> {
        /**
         * The property index.
         */
        final int index;

        /**
         * Creates an entry for the property at the given index.
         */
        Property(final int index) {
            this.index = index;
        }

        /**
         * Returns the key corresponding to this entry.
         */
        @Override
        public String getKey() {
            return accessor.name(index, keyPolicy);
        }

        /**
         * Returns value type as declared in the interface method signature.
         * It may be a primitive type.
         */
        public Class<?> getValueType() {
            return accessor.type(index, TypeValuePolicy.PROPERTY_TYPE);
        }

        /**
         * Returns the value corresponding to this entry.
         */
        @Override
        public Object getValue() {
            final Object value = accessor.get(index, metadata);
            return valuePolicy.isSkipped(value) ? null : value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified value.
         *
         * @throws ClassCastException if the given value is not of the expected type.
         * @throws UnmodifiableMetadataException if the property for this entry is read-only.
         */
        @Override
        public Object setValue(final Object value) {
            return accessor.set(index, metadata, value, RETURN_PREVIOUS);
        }
    }




    /**
     * The iterator over the {@link Property} elements contained in an {@link Entries} set.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private final class Iter implements Iterator<Map.Entry<String,Object>> {
        /**
         * The current and the next property, or {@code null} if the iteration is over.
         */
        private Property current, next;

        /**
         * Creates en iterator.
         */
        Iter() {
            move(0);
        }

        /**
         * Moves {@link #next} to the first property with a valid value,
         * starting at the specified index.
         */
        private void move(int index) {
            final int count = accessor.count();
            while (index < count) {
                if (!valuePolicy.isSkipped(accessor.get(index, metadata))) {
                    next = new Property(index);
                    return;
                }
                index++;
            }
            next = null;
        }

        /**
         * Returns {@code true} if the iteration has more elements.
         */
        @Override
        public boolean hasNext() {
            return next != null;
        }

        /**
         * Returns the next element in the iteration.
         */
        @Override
        public Map.Entry<String,Object> next() {
            if (next != null) {
                current = next;
                move(next.index + 1);
                return current;
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * Removes from the underlying collection the last element returned by the iterator.
         *
         * @throws UnmodifiableMetadataException if the property for this entry is read-only.
         */
        @Override
        public void remove() {
            if (current != null) {
                current.setValue(null);
                current = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }




    /**
     * View of the entries contained in the map.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    private final class Entries extends PropertyMap<Object>.Entries {
        /**
         * Creates an entry set.
         */
        Entries() {
        }

        /**
         * Returns {@code true} if this collection contains the specified element.
         */
        @Override
        public boolean contains(final Object object) {
            if (object instanceof Map.Entry<?,?>) {
                final Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
                final Object key = entry.getKey();
                if (key instanceof String) {
                    final int index = accessor.indexOf((String) key, false);
                    if (index >= 0) {
                        return new Property(index).equals(entry);
                    }
                }
            }
            return false;
        }
    }
}
