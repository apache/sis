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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.util.internal.shared.AbstractMapEntry;
import org.apache.sis.util.resources.Errors;


/**
 * The base class of {@link Map} views of metadata properties.
 * The map keys are fixed to the {@link String} type and will be the property names.
 * The map values depend on the actual {@code PropertyMap} subclasses; they may be
 * property values, property classes or property information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <V>  the type of values in the map.
 *
 * @see ValueMap
 * @see NameMap
 * @see TypeMap
 * @see InformationMap
 * @see NilReasonMap
 */
abstract class PropertyMap<V> extends AbstractMap<String,V> {
    /**
     * The accessor to use for accessing the property names, types or values.
     */
    final PropertyAccessor accessor;

    /**
     * Determines the string representation of keys in the map.
     */
    protected final KeyNamePolicy keyPolicy;

    /**
     * A view of the mappings contained in this map.
     */
    private transient Set<Map.Entry<String,V>> entrySet;

    /**
     * Creates a new map backed by the given accessor.
     */
    PropertyMap(final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        this.accessor  = accessor;
        this.keyPolicy = keyPolicy;
    }

    /**
     * Returns the number of elements in this map.
     * The default implementation returns {@link PropertyAccessor#count()}, which is okay only if
     * all metadata defined by the standard are included in the map. Subclasses shall override
     * this method if their map contain only a subset of all possible metadata elements.
     */
    @Override
    public int size() {
        return accessor.count();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the property at the specified index.
     * The default implementation is okay only if all metadata defined by the standard are included in the map.
     * Subclasses shall override this method if their map contain only a subset of all possible metadata elements.
     *
     * @param  index  index of the property to test, possibly negative.
     * @return whether this map contains a property for the specified index.
     */
    boolean contains(int index) {
        return index >= 0;
    }

    /**
     * Returns the value for the property at the specified index.
     *
     * @param  index  index of the property to get, possibly negative.
     * @return value at the given index, or {@code null} if none.
     */
    abstract V getReflectively(int index);

    /**
     * Sets the value for the property at the specified index.
     *
     * @param  index  index of the property to set.
     * @param  value  new property value to set.
     * @return old property value, or {@code null} if none.
     * @throws UnsupportedOperationException if this map is read-only.
     */
    V setReflectively(int index, V value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, Map.class));
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * Note that the associated value may be null.
     */
    @Override
    public final boolean containsKey(final Object key) {
        return (key instanceof String) && contains(accessor.indexOf((String) key, false));
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if none.
     */
    @Override
    public final V get(final Object key) {
        if (key instanceof String) {
            return getReflectively(accessor.indexOf((String) key, false));
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
    public final V put(final String key, final V value) {
        return setReflectively(accessor.indexOf(key, true), value);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    public final V remove(final Object key) throws UnsupportedOperationException {
        if (key instanceof String) {
            return setReflectively(accessor.indexOf((String) key, false), null);
        }
        return null;
    }

    /**
     * Returns a view of the mappings contained in this map.
     * The entries are provided by {@link #iterator()}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")         // Intentionally modifiable set.
    public final Set<Map.Entry<String,V>> entrySet() {
        if (entrySet == null) {
            entrySet = new Entries();
        }
        return entrySet;
    }

    /**
     * Views of the entries contained in the map.
     * The entries are provided by {@link PropertyMap#iterator()}.
     */
    private final class Entries extends AbstractSet<Map.Entry<String,V>> {
        /** Creates a new entries set. */
        Entries() {
        }

        /** Returns true if this collection contains no elements. */
        @Override public final boolean isEmpty() {
            return PropertyMap.this.isEmpty();
        }

        /** Returns the number of elements in this collection. */
        @Override public final int size() {
            return PropertyMap.this.size();
        }

        /** Returns an iterator over the elements contained in this collection. */
        @Override public final Iterator<Map.Entry<String,V>> iterator() {
            return PropertyMap.this.iterator();
        }

        /** Returns {@code true} if this collection contains the specified element. */
        @Override public boolean contains(final Object object) {
            if (object instanceof Map.Entry<?,?>) {
                final Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
                final Object key   = entry.getKey();
                final Object value = entry.getValue();
                if (value != null) {
                    return value.equals(get(key));
                } else {
                    return containsKey(key);
                }
            }
            return false;
        }
    }

    /**
     * Returns an iterator over the entries in this map.
     * The default implementation is okay only if all metadata defined by the standard are included in the map.
     * Subclasses shall override this method if their map contain only a subset of all possible metadata elements.
     * The {@link ReflectiveIterator} may be used for those maps.
     */
    Iterator<Map.Entry<String,V>> iterator() {
        return new Iterator<>() {
            /** Index of the next property to return. */
            private int index;

            /** Returns true if there is more entries to return. */
            @Override public boolean hasNext() {
                return index < accessor.count();
            }

            /** Returns the next entry. */
            @Override public Map.Entry<String,V> next() {
                final int i = index++;
                final String name = accessor.name(i, keyPolicy);
                if (name != null) {
                    return new SimpleImmutableEntry<>(name, getReflectively(i));
                }
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * The iterator over the {@link ReflectiveEntry} elements contained in an {@link Entries} set.
     * Can be used as an alternative to the default {@link PropertyMap#iterator()} when the map
     * contains only a subset of all possible properties.
     */
    final class ReflectiveIterator implements Iterator<Map.Entry<String,V>> {
        /** The current and the next property, or {@code null} if the iteration is over. */
        private ReflectiveEntry current, next;

        /** Creates en iterator. */
        ReflectiveIterator() {
            move(0);
        }

        /** Moves to the first property with a valid value, starting at the specified index. */
        private void move(int index) {
            final int count = accessor.count();
            while (index < count) {
                if (contains(index)) {
                    next = new ReflectiveEntry(index);
                    return;
                }
                index++;
            }
            next = null;
        }

        /** Returns {@code true} if the iteration has more elements. */
        @Override public boolean hasNext() {
            return next != null;
        }

        /** Returns the next element in the iteration. */
        @Override public Map.Entry<String,V> next() {
            if (next != null) {
                current = next;
                move(next.index + 1);
                return current;
            } else {
                throw new NoSuchElementException();
            }
        }

        /** Removes from the underlying collection the last element returned by the iterator. */
        @Override public void remove() {
            if (current != null) {
                current.setValue(null);
                current = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * A map entry for a property whose key and value are fetched by reflection.
     */
    private final class ReflectiveEntry extends AbstractMapEntry<String,V> {
        /** The property index. */
        final int index;

        /** Creates an entry for the property at the given index. */
        ReflectiveEntry(final int index) {
            this.index = index;
        }

        /** Returns the key corresponding to this entry. */
        @Override public String getKey() {
            return accessor.name(index, keyPolicy);
        }

        /** Returns the value corresponding to this entry. */
        @Override public V getValue() {
            return getReflectively(index);
        }

        /** Replaces the value corresponding to this entry with the specified value. */
        @Override public V setValue(final V value) {
            return setReflectively(index, value);
        }
    }
}
