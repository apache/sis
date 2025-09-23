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
import java.util.HashMap;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.pending.jdk.JDK19;


/**
 * Implementation of the map returned by {@link DefaultAttributeType#characteristics()}.
 * Information provided by this implementation are also used by {@link CharacteristicMap}.
 *
 * <h2>Comparison with standard hash map</h2>
 * The straightforward approach would be to store the attributes directly as values in a standard {@code HashMap}.
 * But instead of that, we store attributes in an array and the array indices in a {@code HashMap}. This level of
 * indirection is useless if we consider only the {@link DefaultAttributeType#characteristics()} method, since a
 * standard {@code HashMap<String,AttributeType>} would work as well or better. However, this level of indirection
 * become useful for {@link CharacteristicMap} (the map returned by {@link AbstractAttribute#characteristics()}),
 * since it allows a more efficient storage. We do this effort because some applications may create a very large
 * number of attribute instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CharacteristicTypeMap extends AbstractMap<String,DefaultAttributeType<?>> {
    /**
     * For sharing the same {@code CharacteristicTypeMap} instances among the attribute types
     * having the same characteristics.
     */
    @SuppressWarnings("unchecked")
    private static final WeakValueHashMap<DefaultAttributeType<?>[],CharacteristicTypeMap> SHARED =
            new WeakValueHashMap<>((Class) DefaultAttributeType[].class);

    /*
     * This class has intentionally no reference to the DefaultAttributeType for which we are providing characteristics.
     * This allows us to use the same CharacteristicTypeMap instance for various attribute types having the same
     * characteristic (e.g. many measurements may have an "accuracy" characteristic).
     */

    /**
     * Characteristics of another attribute type (the {@code source} attribute given to the constructor).
     * This array shall not be modified.
     */
    final DefaultAttributeType<?>[] characterizedBy;

    /**
     * The names of attribute types listed in the {@link #characterizedBy} array,
     * together where their index in the array. This map shall not be modified.
     */
    final Map<String,Integer> indices;

    /**
     * Creates a new map or return an existing map for the given attribute characteristics.
     *
     * <p>This method does not clone the {@code characterizedBy} array. If that array
     * is a user-provided argument, then cloning that array is caller responsibility.</p>
     *
     * @param  source           the attribute which is characterized by {@code characterizedBy}.
     * @param  characterizedBy  characteristics of {@code source}. Should not be empty.
     * @return a map for this given characteristics.
     * @throws IllegalArgumentException if two characteristics have the same name.
     */
    static CharacteristicTypeMap create(final DefaultAttributeType<?> source, final DefaultAttributeType<?>[] characterizedBy) {
        CharacteristicTypeMap map;
        synchronized (SHARED) {
            map = SHARED.get(characterizedBy);
            if (map == null) {
                map = new CharacteristicTypeMap(source, characterizedBy);
                SHARED.put(characterizedBy, map);
            }
        }
        return map;
    }

    /**
     * Creates a new map for the given attribute characteristics.
     *
     * <p>This constructor does not clone the {@code characterizedBy} array. If that array
     * is a user-provided argument, then cloning that array is caller responsibility.</p>
     *
     * @param  source           the attribute which is characterized by {@code characterizedBy}.
     * @param  characterizedBy  characteristics of {@code source}. Should not be empty.
     * @throws IllegalArgumentException if two characteristics have the same name.
     */
    private CharacteristicTypeMap(final DefaultAttributeType<?> source, final DefaultAttributeType<?>[] characterizedBy) {
        this.characterizedBy = characterizedBy;
        int index = 0;
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Map<String,Integer> indices = JDK19.newHashMap(characterizedBy.length);
        final Map<String,Integer> aliases = new HashMap<>();
        for (int i=0; i<characterizedBy.length; i++) {
            final DefaultAttributeType<?> attribute = characterizedBy[i];
            ArgumentChecks.ensureNonNullElement("characterizedBy", i, attribute);
            GenericName name = attribute.getName();
            String key = AbstractIdentifiedType.toString(name, source, "characterizedBy", i);
            final Integer value = index++;
            if (indices.put(key, value) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, key));
            }
            /*
             * If some characteristics use long name of the form "head:tip", creates short aliases containing
             * only the "tip" name for convenience, provided that it does not create ambiguity. If an alias
             * could map to two or more characteristics, then that alias is not added. Those ambiguous aliases
             * are identified by the -1 value in the 'aliases' map.
             */
            while (name instanceof ScopedName) {
                if (name == (name = ((ScopedName) name).tail())) break;   // Safety against broken implementations.
                key = name.toString();
                if (key == null || (key = key.trim()).isEmpty()) break;   // Safety against broken implementations.
                if (aliases.put(key, value) != null) {
                    aliases.put(key, -1);
                }
            }
        }
        /*
         * Copy the aliases only after we finished to create the list of all fully-qualified names.
         * The copy operation shall exclude all ambiguous names.
         */
        for (final Map.Entry<String,Integer> entry : aliases.entrySet()) {
            final Integer value = entry.getValue();
            if (value >= 0) {
                indices.putIfAbsent(entry.getKey(), value);
            }
        }
        this.indices = CollectionsExt.compact(indices);
    }

    /**
     * Returns {@code true} if there are no attribute characteristics.
     */
    @Override
    public boolean isEmpty() {
        return characterizedBy.length == 0;
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
        for (final DefaultAttributeType<?> type : characterizedBy) {
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
    public DefaultAttributeType<?> get(final Object key) {
        final Integer index = indices.get(key);
        return (index != null) ? characterizedBy[index] : null;
    }

    /**
     * Returns an iterator over the entries.
     * This is not the iterator returned by public API like {@code Map.entrySet().iterator()}.
     */
    @Override
    protected EntryIterator<String, DefaultAttributeType<?>> entryIterator() {
        return new EntryIterator<String, DefaultAttributeType<?>>() {
            /** Index of the next element to return in the iteration. */
            private int index;

            /** Value of current entry. */
            private DefaultAttributeType<?> value;

            /**
             * Returns {@code true} if there is more entries in the iteration.
             */
            @Override
            protected boolean next() {
                if (index < characterizedBy.length) {
                    value = characterizedBy[index++];
                    return true;
                }
                return false;
            }

            /**
             * Returns the attribute characteristic name.
             */
            @Override
            protected String getKey() {
                return value.getName().toString();
            }

            /**
             * Returns the attribute characteristic contained in this entry.
             */
            @Override
            protected DefaultAttributeType<?> getValue() {
                return value;
            }
        };
    }
}
