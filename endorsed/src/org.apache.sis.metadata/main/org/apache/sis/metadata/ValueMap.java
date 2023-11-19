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
import java.util.Iterator;

import static org.apache.sis.metadata.PropertyAccessor.RETURN_NULL;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_PREVIOUS;


/**
 * A view of a metadata object as a map. Keys are property names and values
 * are the value returned by the {@code getFoo()} method using reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MetadataStandard#asValueMap(Object, Class, KeyNamePolicy, ValueExistencePolicy)
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
     * @param metadata     the metadata object to wrap.
     * @param accessor     the accessor to use for the metadata.
     * @param keyPolicy    determines the string representation of keys in the map.
     * @param valuePolicy  the behavior of this map toward null or empty values.
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
     * Returns {@code true} if this map contains a mapping for the property at the specified index.
     */
    @Override
    final boolean contains(final int index) {
        if (valuePolicy == ValueExistencePolicy.ALL) {
            return super.contains(index);
        }
        return !valuePolicy.isSkipped(accessor.get(index, metadata));
    }

    /**
     * Returns the value for the property at the specified index.
     */
    @Override
    final Object getReflectively(final int index) {
        final Object value = accessor.get(index, metadata);
        return valuePolicy.isSkipped(value) ? null : value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @throws IllegalArgumentException if the given key is not the name of a property in the metadata.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    final Object setReflectively(final int index, final Object value) {
        final Object old = accessor.set(index, metadata, value, RETURN_PREVIOUS);
        return valuePolicy.isSkipped(old) ? null : old;
    }

    /**
     * Associates the specified value with the specified key in this map if no value is currently associated.
     *
     * @throws IllegalArgumentException if the given key is not the name of a property in the metadata.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    public Object putIfAbsent(final String key, final Object value) {
        final int index = accessor.indexOf(key, true);
        final Object old = accessor.get(index, metadata);
        if (old == null || valuePolicy.isSkipped(old)) {
            return accessor.set(index, metadata, value, RETURN_NULL);
        } else {
            return old;
        }
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
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,Object>> iterator() {
        return new ReflectiveIterator();
    }
}
