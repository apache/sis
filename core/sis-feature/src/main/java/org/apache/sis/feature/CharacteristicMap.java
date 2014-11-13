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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.AbstractMapEntry;

// Branch-dependent imports
import org.opengis.feature.Attribute;


/**
 * Implementation of {@link AbstractAttribute#characteristics()} map.
 * This map holds only the attribute characteristics which have been explicitely set or requested.
 *
 * <p>This implementation has one behavioral difference compared to the familiar {@code Map} invariants:
 * a call to the {@link #get(Object)} method creates a new {@code Attribute} instance if the given key
 * is valid and no instance existed previously for that key, thus increasing this {@code Map} size by one.
 * If this behavior is not desired, then caller should check {@link #containsKey(Object)} first.</p>
 *
 * <div class="note"><b>Note:</b>
 * Such departures are unusual, but exists also in the JDK. For example {@link java.util.WeakHashMap} too
 * may change its size as a result of read-only methods. Those maps behave as though an unknown thread is
 * silently adding or removing entries.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class CharacteristicMap extends AbstractMap<String,Attribute<?>> {
    /**
     * The attribute source for which to provide characteristics.
     */
    private final Attribute<?> source;

    /**
     * Characteristics of the {@code source} attribute, created when first needed.
     */
    Attribute<?>[] characterizedBy;

    /**
     * Description of the attribute characteristics.
     */
    final CharacteristicTypeMap types;

    /**
     * Creates an initially empty map of attribute characteristics.
     *
     * @param source The attribute which is characterized by {@code characterizedBy}.
     * @param characterizedBy Description of the characteristics of {@code source}.
     */
    CharacteristicMap(final Attribute<?> source, final CharacteristicTypeMap types) {
        this.source = source;
        this.types  = types;
    }

    /**
     * Returns {@code false} if this map contains at least one characteristic.
     */
    @Override
    public boolean isEmpty() {
        if (characterizedBy != null) {
            for (final Attribute<?> attribute : characterizedBy) {
                if (attribute != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the number of attribute characteristics.
     */
    @Override
    public int size() {
        int n = 0;
        if (characterizedBy != null) {
            for (final Attribute<?> attribute : characterizedBy) {
                if (attribute != null) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns {@code true} if this map contains an attribute characteristic for the given name.
     */
    @Override
    public boolean containsKey(final Object key) {
        if (characterizedBy != null) {
            final Integer index = types.indices.get(key);
            if (index != null) {
                return characterizedBy[index] != null;
            }
        }
        return false;
    }

    /**
     * Returns the attribute characteristic for the given name, or {@code null} if none. If no characteristic
     * exist for the given key and that key is valid, then this method will silently creates a new instance.
     * The intend is to allow callers to set a characteristic values using the following pattern:
     *
     * {@preformat java
     *     Attribute<?> characteristic = attribute.characteristics().get("accuracy");
     *     Features.cast(characteristic, Double.class).setValue(0.1);
     * }
     */
    @Override
    public Attribute<?> get(final Object key) {
        final Integer index = types.indices.get(key);
        if (index == null) {
            return null;
        }
        if (characterizedBy == null) {
            characterizedBy = new Attribute<?>[types.characterizedBy.length];
        }
        Attribute<?> attribute = characterizedBy[index];
        if (attribute == null) {
            attribute = AbstractAttribute.create(types.characterizedBy[index]);
            characterizedBy[index] = attribute;
        }
        return attribute;
    }

    /**
     * Removes the attribute characteristic for the given name.
     */
    @Override
    public Attribute<?> remove(final Object key) {
        if (characterizedBy != null) {
            final Integer index = types.indices.get(key);
            if (index != null) {
                final Attribute<?> previous = characterizedBy[index];
                characterizedBy[index] = null;
                return previous;
            }
        }
        return null;
    }

    /**
     * Sets the attribute characteristic for the given name.
     */
    @Override
    public Attribute<?> put(final String key, final Attribute<?> value) {
        ArgumentChecks.ensureNonNull("value", value);
        final Integer index = types.indices.get(key);
        if (index == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, source.getName(), key));
        }
        if (!types.characterizedBy[index].equals(value.getType())) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedPropertyType_1, key));
        }
        if (characterizedBy == null) {
            characterizedBy = new Attribute<?>[types.characterizedBy.length];
        }
        final Attribute<?> previous = characterizedBy[index];
        characterizedBy[index] = value;
        return previous;
    }

    /**
     * Returns the set of entries in this map.
     */
    @Override
    public Set<Map.Entry<String, Attribute<?>>> entrySet() {
        return (characterizedBy != null) ? new Entries() : Collections.emptySet();
    }

    /**
     * The set of entries in the {@link CharacteristicMap}.
     */
    private final class Entries extends AbstractSet<Map.Entry<String,Attribute<?>>> {
        /** Creates a new set of entries. */
        Entries() {
        }

        /** Returns the number of entries. */
        @Override
        public int size() {
            return CharacteristicMap.this.size();
        }

        /** Returns an iterator over the entries. */
        @Override
        public Iterator<Map.Entry<String, Attribute<?>>> iterator() {
            return new Iter();
        }
    }

    /**
     * Iterator over the {@link CharacteristicMap} entries.
     */
    private final class Iter implements Iterator<Map.Entry<String, Attribute<?>>> {
        /** Index of the next element to return in the iteration. */
        private int index;

        /** The next element to return, or {@code null} if we reached the end of iteration. */
        private Attribute<?> next;

        /** Index of the element returned by the last call to {@link #next()}, or -1 if none. */
        private int previous;

        /** Creates a new iterator. */
        Iter() {
            move();
        }

        /** Advances {@link #index} to the next attribute to return. */
        private void move() {
            previous = -1;
            while (index < characterizedBy.length) {
                next = characterizedBy[index];
                if (next != null) return;
                index++;
            }
            next = null;
        }

        /** Returns {@code true} if there is more entries in the iteration. */
        @Override
        public boolean hasNext() {
            return next != null;
        }

        /** Creates and return the next entry. */
        @Override
        public Map.Entry<String, Attribute<?>> next() {
            if (hasNext()) {
                return new Entry(previous = index++, next);
            } else {
                throw new NoSuchElementException();
            }
        }

        /** Removes the last element returned by {@link #next()}. */
        @Override
        public void remove() {
            if (previous >= 0) {
                characterizedBy[previous] = null;
                previous = -1;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * An entry returned by the {@link CharacteristicMap#entrySet()} iterator.
     * The key and value are never null, even in case of concurrent modification.
     * This entry supports the {@link #setValue(Attribute)} operation.
     */
    private final class Entry extends AbstractMapEntry<String, Attribute<?>> {
        /** Index of the attribute characteristics represented by this entry. */
        private final int index;

        /** The current attribute value, which is guaranteed to be non-null. */
        private Attribute<?> value;

        /** Creates a new entry for the characteristic at the given index. */
        Entry(final int index, final Attribute<?> value) {
            this.index = index;
            this.value = value;
        }

        /** Returns the name of the attribute characteristic. */
        @Override
        public String getKey() {
            return types.names[index];
        }

        /** Returns the attribute characteristic (never {@code null}). */
        @Override
        public Attribute<?> getValue() {
            return value;
        }

        /** Sets the attribute characteristic. */
        @Override
        public Attribute<?> setValue(final Attribute<?> value) {
            ArgumentChecks.ensureNonNull("value", value);
            if (!types.characterizedBy[index].equals(value.getType())) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedPropertyType_1, getKey()));
            }
            final Attribute<?> previous = this.value;
            characterizedBy[index] = value;
            this.value = value;
            return previous;
        }
    }
}
