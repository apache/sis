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
package org.apache.sis.util.collection;

import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.AbstractMap;
import java.io.Serializable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.FunctionProperty;


/**
 * A map whose keys and values are derived <cite>on-the-fly</cite> from an other map.
 * Conversions are performed when needed by the following methods:
 *
 * <ul>
 *   <li>The iterators over the {@linkplain #keySet() key set} or {@linkplain #entrySet() entry set}
 *       obtain the derived keys using the {@link #keyConverter}.</li>
 *   <li>The iterators over the {@linkplain #values() values} or {@linkplain #entrySet() entry set}
 *       obtain the derived values using the {@link #valueConverter}.</li>
 *   <li>Queries ({@link #get get}, {@link #containsKey containsKey}) and write operations
 *       ({@link #put put}, {@link #remove remove}) obtain the storage values using the
 *       inverse of the above converters.</li>
 * </ul>
 *
 * <div class="section">Constraints</div>
 * <ul>
 *   <li>This map does not support {@code null} keys, since {@code null} is used as a
 *       sentinel value when no mapping from {@linkplain #storage} to {@code this} exists.</li>
 *   <li>Instances of this class are serializable if their underlying {@linkplain #storage} map
 *       is serializable.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 *
 * <div class="section">Performance considerations</div>
 * This class does not cache any value, since the {@linkplain #storage} map is presumed modifiable.
 * If the storage map is known to be immutable, then sub-classes may consider to cache some values,
 * especially the result of the {@link #size()} method.
 *
 * @param <SK> The type of keys in the storage map.
 * @param <SV> The type of values in the storage map.
 * @param <K>  The type of keys in this map.
 * @param <V>  The type of values in this map.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class DerivedMap<SK,SV,K,V> extends AbstractMap<K,V> implements
        ObjectConverter<Map.Entry<SK,SV>, Map.Entry<K,V>>, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4760466188643114727L;

    /**
     * The storage map whose keys are derived from.
     */
    protected final Map<SK,SV> storage;

    /**
     * The converter from the storage to the derived keys.
     */
    protected final ObjectConverter<SK,K> keyConverter;

    /**
     * The converter from the storage to the derived values.
     */
    protected final ObjectConverter<SV,V> valueConverter;

    /**
     * Key set. Will be constructed only when first needed.
     *
     * @see #keySet()
     */
    private transient Set<K> keySet;

    /**
     * Entry set. Will be constructed only when first needed.
     *
     * @see #entrySet()
     */
    private transient Set<Map.Entry<K,V>> entrySet;

    /**
     * Creates a new derived map from the specified storage map.
     *
     * @param storage        The map which actually store the entries.
     * @param keyConverter   The converter for the keys.
     * @param valueConverter The converter for the values.
     */
    static <SK,SV,K,V> Map<K,V> create(final Map<SK,SV> storage,
                                       final ObjectConverter<SK,K> keyConverter,
                                       final ObjectConverter<SV,V> valueConverter)
    {
        final Set<FunctionProperty> kp =   keyConverter.properties();
        final Set<FunctionProperty> vp = valueConverter.properties();
        if (kp.contains(FunctionProperty.INVERTIBLE)) {
            if (vp.contains(FunctionProperty.INVERTIBLE)) {
                return new Invertible<SK,SV,K,V>(storage, keyConverter, valueConverter);
            }
            return new InvertibleKey<SK,SV,K,V>(storage, keyConverter, valueConverter);
        }
        if (vp.contains(FunctionProperty.INVERTIBLE)) {
            return new InvertibleValue<SK,SV,K,V>(storage, keyConverter, valueConverter);
        }
        return new DerivedMap<SK,SV,K,V>(storage, keyConverter, valueConverter);
    }

    /**
     * Creates a new derived map from the specified storage map.
     *
     * @param storage        The map which actually store the entries.
     * @param keyConverter   The converter for the keys.
     * @param valueConverter The converter for the values.
     */
    private DerivedMap(final Map<SK,SV> storage,
                       final ObjectConverter<SK,K> keyConverter,
                       final ObjectConverter<SV,V> valueConverter)
    {
        this.storage        = storage;
        this.keyConverter   = keyConverter;
        this.valueConverter = valueConverter;
    }

    /**
     * Returns the number of entries in this map.
     *
     * @return The number of entries in this map.
     */
    @Override
    public int size() {
        return keySet().size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return storage.isEmpty() || keySet().isEmpty();
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param  key key with which the specified value is to be associated.
     * @param  value value to be associated with the specified key.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.
     * @throws UnsupportedOperationException if the converters are not invertible,
     *         or the {@linkplain #storage} map doesn't supports the {@code put} operation.
     */
    @Override
    public V put(final K key, final V value) throws UnsupportedOperationException {
        return put(key, keyConverter.inverse().apply(key),
                      valueConverter.inverse().apply(value));
    }

    /**
     * Implementation of the {@link #put(Object,Object)} method storing the given converted entry
     * to the storage map. The {@code original} key is used only for formatting an error message
     * in case of failure.
     */
    final V put(final K original, final SK key, final SV value) {
        if (key == null) {
            throw new UnconvertibleObjectException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "key", original));
        }
        return valueConverter.apply(storage.put(key, value));
    }

    /**
     * A {@link DerivedMap} used when the {@link #keyConverter} is invertible.
     * Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #storage} map instead than iterating over all entries.
     */
    private static class InvertibleKey<SK,SV,K,V> extends DerivedMap<SK,SV,K,V> {
        private static final long serialVersionUID = 3499911507293121425L;

        /** The inverse of {@link #keyConverter}. */
        protected final ObjectConverter<K,SK> keyInverse;

        InvertibleKey(final Map<SK,SV> storage,
                      final ObjectConverter<SK,K> keyConverter,
                      final ObjectConverter<SV,V> valueConverter)
        {
            super(storage, keyConverter, valueConverter);
            keyInverse = keyConverter.inverse();
        }

        @Override
        public final V get(final Object key) {
            final Class<K> type = keyConverter.getTargetClass();
            return type.isInstance(key) ? valueConverter.apply(storage.get(keyInverse.apply(type.cast(key)))) : null;
        }

        @Override
        public final V remove(final Object key) throws UnsupportedOperationException {
            final Class<K> type = keyConverter.getTargetClass();
            return type.isInstance(key) ? valueConverter.apply(storage.remove(keyInverse.apply(type.cast(key)))) : null;
        }

        @Override
        public final boolean containsKey(final Object key) {
            final Class<K> type = keyConverter.getTargetClass();
            return type.isInstance(key) && storage.containsKey(keyInverse.apply(type.cast(key)));
        }
    }

    /**
     * A {@link DerivedMap} used when the {@link #valueConverter} is invertible.
     * Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #storage} map instead than iterating over all entries.
     */
    private static final class InvertibleValue<SK,SV,K,V> extends DerivedMap<SK,SV,K,V> {
        private static final long serialVersionUID = -8290698486357636366L;

        /** The inverse of {@link #valueConverter}. */
        private final ObjectConverter<V,SV> valueInverse;

        InvertibleValue(final Map<SK,SV> storage,
                        final ObjectConverter<SK,K> keyConverter,
                        final ObjectConverter<SV,V> valueConverter)
        {
            super(storage, keyConverter, valueConverter);
            valueInverse = valueConverter.inverse();
        }

        @Override
        public boolean containsValue(final Object value) {
            final Class<V> type = valueConverter.getTargetClass();
            return type.isInstance(value) && storage.containsValue(valueInverse.apply(type.cast(value)));
        }
    }

    /**
     * A {@link DerivedMap} used when both the {@link #keyConverter} and {@link #valueConverter}
     * are invertible. Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #storage} map instead than iterating over all entries.
     */
    private static final class Invertible<SK,SV,K,V> extends InvertibleKey<SK,SV,K,V> {
        private static final long serialVersionUID = -6625938922337246124L;

        /** The inverse of {@link #valueConverter}. */
        private final ObjectConverter<V,SV> valueInverse;

        /** The inverse of this entry converter. */
        private transient ObjectConverter<Entry<K,V>, Entry<SK,SV>> inverse;

        Invertible(final Map<SK,SV> storage,
                   final ObjectConverter<SK,K> keyConverter,
                   final ObjectConverter<SV,V> valueConverter)
        {
            super(storage, keyConverter, valueConverter);
            valueInverse = valueConverter.inverse();
        }

        @Override
        public boolean containsValue(final Object value) {
            final Class<V> type = valueConverter.getTargetClass();
            return type.isInstance(value) && storage.containsValue(valueInverse.apply(type.cast(value)));
        }

        @Override
        public V put(final K key, final V value) {
            return put(key, keyInverse.apply(key),
                          valueInverse.apply(value));
        }

        @Override
        public ObjectConverter<Entry<K,V>, Entry<SK,SV>> inverse() {
            if (inverse == null) {
                inverse = new DerivedMap<K,V,SK,SV>(null, keyInverse, valueInverse);
            }
            return inverse;
        }
    }

    /**
     * Returns a set view of the keys contained in this map.
     */
    @Override
    public final Set<K> keySet() {
        if (keySet == null) {
            keySet = DerivedSet.create(storage.keySet(), keyConverter);
        }
        return keySet;
    }

    /**
     * Returns a set view of the mappings contained in this map.
     */
    @Override
    public final Set<Map.Entry<K,V>> entrySet() {
        if (entrySet == null) {
            entrySet = DerivedSet.create(storage.entrySet(), this);
        }
        return entrySet;
    }

    /**
     * Returns the properties of the entry converter, as the union of some properties
     * of the key and value converters.
     */
    @Override
    public final Set<FunctionProperty> properties() {
        final EnumSet<FunctionProperty> properties = EnumSet.of(
                FunctionProperty.INVERTIBLE,
                FunctionProperty.INJECTIVE,
                FunctionProperty.SURJECTIVE);
        properties.retainAll(  keyConverter.properties());
        properties.retainAll(valueConverter.properties());
        return properties;
    }

    /**
     * Returns the source class of the map entry converter.
     * Defined because the interface requires so but not used.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public final Class<Entry<SK,SV>> getSourceClass() {
        return (Class) Entry.class;
    }

    /**
     * Returns the target class of the map entry converter.
     * Defined because the interface requires so but not used.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public final Class<Entry<K,V>> getTargetClass() {
        return (Class) Entry.class;
    }

    /**
     * Converts the given entry.
     */
    @Override
    public final Entry<K,V> apply(final Entry<SK,SV> entry) {
        final K key   =   keyConverter.apply(entry.getKey());
        final V value = valueConverter.apply(entry.getValue());
        return (key != null) ? new SimpleEntry<K,V>(key, value) : null;
    }

    /**
     * To be defined in the {@link Invertible} sub-class only.
     */
    @Override
    public ObjectConverter<Entry<K,V>, Entry<SK,SV>> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
