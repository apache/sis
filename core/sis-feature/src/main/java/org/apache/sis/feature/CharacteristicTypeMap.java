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
import java.util.Set;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;

// Branch-dependent imports
import org.opengis.feature.AttributeType;


/**
 * Implementation of the map returned by {@link DefaultAttributeType#characteristics()}.
 * Information provided by this implementation are also used by {@link CharacteristicMap}.
 *
 * <p><b>Comparison with standard hash map:</b>
 * The straightforward approach would be to store the attributes directly as values in a standard {@code HashMap}.
 * But instead of that, we store attributes in an array and the array indices in a {@code HashMap}. This level of
 * indirection is useless if we consider only the {@link DefaultAttributeType#characteristics()} method, since a
 * standard {@code HashMap<String,AttributeType>} would work as well or better. However this level of indirection
 * become useful for {@link CharacteristicMap} (the map returned by {@link DefaultAttribute#characteristics()}),
 * since it allows a more efficient storage. We do this effort because some applications may create a very large
 * amount of attribute instances.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class CharacteristicTypeMap extends AbstractMap<String,AttributeType<?>> {
    /**
     * Characteristics of an other attribute type (the {@code source} attribute given to the constructor).
     * This array shall not be modified.
     */
    final AttributeType<?>[] characterizedBy;

    /**
     * Name of the {@code characterizedBy} attribute types, used only during iteration over map entries.
     * This array shall not be modified.
     */
    final String[] names;

    /**
     * The names of attribute types listed in the {@link #characterizedBy} array,
     * together where their index in the array. This map shall not be modified.
     */
    final Map<String,Integer> indices;

    /**
     * Creates a new map for the given attribute characteristics.
     *
     * @param  source The attribute which is characterized by {@code characterizedBy}.
     * @param  characterizedBy Characteristics of {@code source}. Should not be empty.
     * @throws IllegalArgumentException if two characteristics have the same name.
     */
    CharacteristicTypeMap(final AttributeType<?> source, final AttributeType<?>[] characterizedBy) {
        this.characterizedBy = characterizedBy.clone();
        names = new String[characterizedBy.length];
        int index = 0;
        final Map<String,Integer> indices = new HashMap<>(Containers.hashMapCapacity(characterizedBy.length));
        for (int i=0; i<characterizedBy.length; i++) {
            final AttributeType<?> attribute = characterizedBy[i];
            ensureNonNullElement("characterizedBy", i, attribute);
            final String name = AbstractIdentifiedType.toString(attribute.getName(), source, "characterizedBy", i);
            names[index] = name;
            if (indices.put(name, index++) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, name));
            }
        }
        this.indices = CollectionsExt.compact(indices);
    }

    /**
     * Returns the number of attribute characteristics.
     */
    @Override
    public int size() {
        return characterizedBy.length;
    }

    /**
     * Returns {@code true} if this map contains an attribute characteristic of the given name.
     */
    @Override
    public boolean containsKey(final Object key) {
        return indices.containsKey(key);
    }

    /**
     * Returns {@code true} if this map contains the given attribute characteristic.
     */
    @Override
    public boolean containsValue(final Object key) {
        for (final AttributeType<?> type : characterizedBy) {
            if (type.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the attribute characteristic for the given name, or {@code null} if none.
     */
    @Override
    public AttributeType<?> get(final Object key) {
        final Integer index = indices.get(key);
        return (index != null) ? characterizedBy[index] : null;
    }

    /**
     * Returns the set of entries in this map.
     */
    @Override
    public Set<Entry<String, AttributeType<?>>> entrySet() {
        return new Entries();
    }

    /**
     * The set of entries in the {@link CharacteristicTypeMap}.
     */
    private final class Entries extends AbstractSet<Entry<String,AttributeType<?>>> {
        /** Creates a new set of entries. */
        Entries() {
        }

        /** Returns the number of entries. */
        @Override
        public int size() {
            return CharacteristicTypeMap.this.size();
        }

        /** Returns an iterator over the entries. */
        @Override
        public Iterator<Entry<String, AttributeType<?>>> iterator() {
            return new Iter();
        }
    }

    /**
     * Iterator over the {@link CharacteristicTypeMap} entries.
     */
    private final class Iter implements Iterator<Entry<String, AttributeType<?>>> {
        /** Index of the next element to return in the iteration. */
        private int index;

        /** Creates a new iterator. */
        Iter() {
        }

        /** Returns {@code true} if there is more entries in the iteration. */
        @Override
        public boolean hasNext() {
            return index < characterizedBy.length;
        }

        /** Creates and return the next entry. */
        @Override
        public Entry<String, AttributeType<?>> next() {
            if (hasNext()) {
                return new SimpleImmutableEntry<>(names[index], characterizedBy[index++]);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
