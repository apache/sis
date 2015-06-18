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
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Cloner;
import org.apache.sis.internal.util.AbstractMap;
import org.apache.sis.internal.util.AbstractMapEntry;


/**
 * Implementation of {@link AbstractAttribute#characteristics()} map.
 * This map holds only the attribute characteristics which have been explicitely set or requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
final class CharacteristicMap extends AbstractMap<String,AbstractAttribute<?>> implements Cloneable {
    /**
     * The attribute source for which to provide characteristics.
     */
    private final AbstractAttribute<?> source;

    /**
     * Characteristics of the {@code source} attribute, created when first needed.
     */
    AbstractAttribute<?>[] characterizedBy;

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
    CharacteristicMap(final AbstractAttribute<?> source, final CharacteristicTypeMap types) {
        this.source = source;
        this.types  = types;
    }

    /**
     * Returns a copy of this map. Characteristics are also cloned.
     *
     * @return A copy of this map.
     */
    @Override
    public CharacteristicMap clone() throws CloneNotSupportedException {
        final CharacteristicMap clone = (CharacteristicMap) super.clone();
        AbstractAttribute<?>[] c = clone.characterizedBy;
        if (c != null) {
            clone.characterizedBy = c = c.clone();
            final Cloner cloner = new Cloner();
            for (int i=0; i<c.length; i++) {
                final AbstractAttribute<?> attribute = c[i];
                if (attribute instanceof Cloneable) {
                    c[i] = (AbstractAttribute<?>) cloner.clone(attribute);
                }
            }
        }
        return clone;
    }

    /**
     * Removes all entries in this map.
     */
    @Override
    public void clear() {
        /*
         * Implementation note: We could keep existing array and clear it with Arrays.fill(characterizedBy, null)
         * instead, but maybe the user does not plan to store characteristics anymore for the attribute. Setting
         * the array reference to null free more memory in such cases.
         */
        characterizedBy = null;
    }

    /**
     * Returns {@code false} if this map contains at least one characteristic.
     */
    @Override
    public boolean isEmpty() {
        if (characterizedBy != null) {
            for (final AbstractAttribute<?> attribute : characterizedBy) {
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
            for (final AbstractAttribute<?> attribute : characterizedBy) {
                if (attribute != null) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the attribute characteristic for the given name, or {@code null} if none.
     */
    @Override
    public AbstractAttribute<?> get(final Object key) {
        if (characterizedBy != null) {
            final Integer index = types.indices.get(key);
            if (index != null) {
                return characterizedBy[index];
            }
        }
        return null;
    }

    /**
     * Removes the attribute characteristic for the given name.
     */
    @Override
    public AbstractAttribute<?> remove(final Object key) {
        if (characterizedBy != null) {
            final Integer index = types.indices.get(key);
            if (index != null) {
                final AbstractAttribute<?> previous = characterizedBy[index];
                characterizedBy[index] = null;
                return previous;
            }
        }
        return null;
    }

    /**
     * Returns the index for the characteristic of the given name.
     *
     * @param  key The name for which to get the characteristic index.
     * @return The index for the characteristic of the given name.
     * @throws IllegalArgumentException if the given key is not the name of a characteristic in this map.
     */
    private int indexOf(final String key) {
        ArgumentChecks.ensureNonNull("key", key);
        final Integer index = types.indices.get(key);
        if (index == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, source.getName(), key));
        }
        return index;
    }

    /**
     * Ensures that the given attribute type is the instance that we expect at the given index.
     * If the given instance is not the expected one, then an {@link IllegalArgumentException}
     * will be thrown with an error message formatted using the name of expected and given types.
     *
     * @param index Index of the expected attribute type.
     * @param type  The actual attribute type.
     */
    final void verifyAttributeType(final int index, final DefaultAttributeType<?> type) {
        final DefaultAttributeType<?> expected = types.characterizedBy[index];
        if (!expected.equals(type)) {
            final GenericName en = expected.getName();
            final GenericName an = type.getName();
            throw new IllegalArgumentException(String.valueOf(en).equals(String.valueOf(an))
                    ? Errors.format(Errors.Keys.MismatchedPropertyType_1, en)
                    : Errors.format(Errors.Keys.CanNotAssign_2, en.push(source.getName()), an));
        }
    }

    /**
     * Sets the attribute characteristic for the given name.
     *
     * @param  key The name of the characteristic to set.
     * @throws IllegalArgumentException if the given key is not the name of a characteristic in this map.
     */
    @Override
    public AbstractAttribute<?> put(final String key, final AbstractAttribute<?> value) {
        final int index = indexOf(key);
        ArgumentChecks.ensureNonNull("value", value);
        verifyAttributeType(index, value.getType());
        if (characterizedBy == null) {
            characterizedBy = new AbstractAttribute<?>[types.characterizedBy.length];
        }
        final AbstractAttribute<?> previous = characterizedBy[index];
        characterizedBy[index] = value;
        return previous;
    }

    /**
     * If no characteristic exists for the given name and that name is valid,
     * creates a new map entry with a default {@code Attribute} characteristic.
     *
     * @param  name The name of the characteristic to create, if it does not already exist.
     * @return {@code true} if a new characteristic has been created for the given name.
     * @throws IllegalArgumentException if the given key is not the name of a characteristic in this map.
     */
    @Override
    protected boolean addKey(final String name) {
        final int index = indexOf(name);
        if (characterizedBy == null) {
            characterizedBy = new AbstractAttribute<?>[types.characterizedBy.length];
        }
        if (characterizedBy[index] == null) {
            characterizedBy[index] = types.characterizedBy[index].newInstance();
            return true;
        }
        return false;
    }

    /**
     * Adds the given characteristic if none is currently associated for the same characteristic name.
     *
     * @param  value The characteristic to add.
     * @return {@code true} if the characteristic has been added.
     * @throws IllegalArgumentException if given characteristic is not valid for this map.
     * @throws IllegalStateException if another characteristic already exists for the characteristic name.
     */
    @Override
    protected boolean addValue(final AbstractAttribute<?> value) {
        ArgumentChecks.ensureNonNull("value", value);
        final int index = indexOf(value.getName().toString());
        verifyAttributeType(index, value.getType());
        if (characterizedBy == null) {
            characterizedBy = new AbstractAttribute<?>[types.characterizedBy.length];
        }
        final AbstractAttribute<?> previous = characterizedBy[index];
        if (previous == null) {
            characterizedBy[index] = value;
            return true;
        } else if (previous.equals(value)) {
            return false;
        } else {
            throw new IllegalStateException(Errors.format(
                    Errors.Keys.PropertyAlreadyExists_2, source.getName(), value.getName()));
        }
    }

    /**
     * Returns an iterator over the entries.
     */
    @Override
    protected EntryIterator<String, AbstractAttribute<?>> entryIterator() {
        if (characterizedBy == null) {
            return null;
        }
        return new EntryIterator<String, AbstractAttribute<?>>() {
            /** Index of the current element to return in the iteration. */
            private int index = -1;

            /** The element to return, or {@code null} if we reached the end of iteration. */
            private AbstractAttribute<?> value;

            /** Returns {@code true} if there is more entries in the iteration. */
            @Override protected boolean next() {
                while (++index < characterizedBy.length) {
                    value = characterizedBy[index];
                    if (value != null) return true;
                }
                value = null;
                return false;
            }

            /** Returns the name of the attribute characteristic. */
            @Override protected String getKey() {
                return value.getType().getName().toString();
            }

            /** Returns the attribute characteristic (never {@code null}). */
            @Override protected AbstractAttribute<?> getValue() {
                return value;
            }

            /** Creates and return the next entry. */
            @Override protected Map.Entry<String, AbstractAttribute<?>> getEntry() {
                return new Entry(index, value);
            }

            /** Removes the last element returned by {@link #next()}. */
            @Override protected void remove() {
                characterizedBy[index] = null;
            }
        };
    }

    /**
     * An entry returned by the {@link CharacteristicMap#entrySet()} iterator.
     * The key and value are never null, even in case of concurrent modification.
     * This entry supports the {@link #setValue(Attribute)} operation.
     */
    private final class Entry extends AbstractMapEntry<String, AbstractAttribute<?>> {
        /** Index of the attribute characteristics represented by this entry. */
        private final int index;

        /** The current attribute value, which is guaranteed to be non-null. */
        private AbstractAttribute<?> value;

        /** Creates a new entry for the characteristic at the given index. */
        Entry(final int index, final AbstractAttribute<?> value) {
            this.index = index;
            this.value = value;
        }

        /** Returns the name of the attribute characteristic. */
        @Override public String getKey() {
            return value.getType().getName().toString();
        }

        /** Returns the attribute characteristic (never {@code null}). */
        @Override public AbstractAttribute<?> getValue() {
            return value;
        }

        /** Sets the attribute characteristic. */
        @Override public AbstractAttribute<?> setValue(final AbstractAttribute<?> value) {
            ArgumentChecks.ensureNonNull("value", value);
            verifyAttributeType(index, value.getType());
            final AbstractAttribute<?> previous = this.value;
            characterizedBy[index] = value;
            this.value = value;
            return previous;
        }
    }
}
