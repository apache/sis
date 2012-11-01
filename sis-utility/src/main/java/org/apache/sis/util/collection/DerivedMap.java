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
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.FunctionProperty;


/**
 * A map whose keys are derived <cite>on-the-fly</cite> from an other map.
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
 * {@section Constraints}
 * <ul>
 *   <li>This map does not support {@code null} keys, since {@code null} is used as a
 *       sentinel value when no mapping from {@linkplain #base} to {@code this} exists.</li>
 *   <li>Instances of this class are serializable if their underlying {@linkplain #base} map
 *       is serializable.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 *
 * {@section Performance considerations}
 * This class does not cache any value, since the {@linkplain #base} map is presumed modifiable.
 * If the base map is known to be immutable, then sub-classes may consider to cache some values,
 * especially the result of the {@link #size()} method.
 *
 * @param <BK> The type of keys in the backing map.
 * @param <BV> The type of values in the backing map.
 * @param <K>  The type of keys in this map.
 * @param <V>  The type of values in both this map and the underlying map.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@Decorator(Map.class)
class DerivedMap<BK,BV,K,V> extends AbstractMap<K,V> implements
        ObjectConverter<Map.Entry<BK,BV>, Map.Entry<K,V>>, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6994867383669885934L;

    /**
     * The base map whose keys are derived from.
     */
    protected final Map<BK,BV> base;

    /**
     * The converter from the base to the derived keys.
     */
    protected final ObjectConverter<BK,K> keyConverter;

    /**
     * The converter from the base to the derived values.
     */
    protected final ObjectConverter<BV,V> valueConverter;

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
     * Creates a new derived map from the specified base map.
     *
     * @param base           The base map.
     * @param keyConverter   The converter for the keys.
     * @param valueConverter The converter for the values.
     */
    static <BK,BV,K,V> Map<K,V> create(final Map<BK,BV> base,
                                       final ObjectConverter<BK,K> keyConverter,
                                       final ObjectConverter<BV,V> valueConverter)
    {
        final Set<FunctionProperty> kp =   keyConverter.properties();
        final Set<FunctionProperty> vp = valueConverter.properties();
        if (kp.contains(FunctionProperty.INVERTIBLE)) {
            if (vp.contains(FunctionProperty.INVERTIBLE)) {
                return new Invertible<>(base, keyConverter, valueConverter);
            }
            return new InvertibleKey<>(base, keyConverter, valueConverter);
        }
        if (vp.contains(FunctionProperty.INVERTIBLE)) {
            return new InvertibleValue<>(base, keyConverter, valueConverter);
        }
        return new DerivedMap<>(base, keyConverter, valueConverter);
    }

    /**
     * Creates a new derived map from the specified base map.
     *
     * @param base           The base map.
     * @param keyConverter   The converter for the keys.
     * @param valueConverter The converter for the values.
     */
    private DerivedMap(final Map<BK,BV> base,
                       final ObjectConverter<BK,K> keyConverter,
                       final ObjectConverter<BV,V> valueConverter)
    {
        this.base           = base;
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
        return base.isEmpty() || keySet().isEmpty();
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param  key key with which the specified value is to be associated.
     * @param  value value to be associated with the specified key.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.
     * @throws UnsupportedOperationException if the converters are not invertible,
     *         or the {@linkplain #base} map doesn't supports the {@code put} operation.
     */
    @Override
    public V put(final K key, final V value) throws UnsupportedOperationException {
        ArgumentChecks.ensureNonNull("key", key);
        return valueConverter.convert(base.put(
                 keyConverter.inverse().convert(key),
               valueConverter.inverse().convert(value)));
    }

    /**
     * A {@link DerivedMap} used when the {@link #keyConverter} is invertible.
     * Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #base} map instead than iterating over all entries.
     */
    private static class InvertibleKey<BK,BV,K,V> extends DerivedMap<BK,BV,K,V> {
        private static final long serialVersionUID = -7770446176017835821L;

        /** The inverse of {@link #keyConverter}. */
        protected final ObjectConverter<K,BK> keyInverse;

        InvertibleKey(final Map<BK,BV> base,
                      final ObjectConverter<BK,K> keyConverter,
                      final ObjectConverter<BV,V> valueConverter)
        {
            super(base, keyConverter, valueConverter);
            keyInverse = keyConverter.inverse();
        }

        @Override
        public final V get(final Object key) {
            final Class<? extends K> type = keyConverter.getTargetClass();
            return type.isInstance(key) ? valueConverter.convert(base.get(keyInverse.convert(type.cast(key)))) : null;
        }

        @Override
        public final V remove(final Object key) throws UnsupportedOperationException {
            final Class<? extends K> type = keyConverter.getTargetClass();
            return type.isInstance(key) ? valueConverter.convert(base.remove(keyInverse.convert(type.cast(key)))) : null;
        }

        @Override
        public final boolean containsKey(final Object key) {
            final Class<? extends K> type = keyConverter.getTargetClass();
            return type.isInstance(key) && base.containsKey(keyInverse.convert(type.cast(key)));
        }
    }

    /**
     * A {@link DerivedMap} used when the {@link #valueConverter} is invertible.
     * Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #base} map instead than iterating over all entries.
     */
    private static final class InvertibleValue<BK,BV,K,V> extends DerivedMap<BK,BV,K,V> {
        private static final long serialVersionUID = 6249800498911409046L;

        /** The inverse of {@link #valueConverter}. */
        private final ObjectConverter<V,BV> valueInverse;

        InvertibleValue(final Map<BK,BV> base,
                        final ObjectConverter<BK,K> keyConverter,
                        final ObjectConverter<BV,V> valueConverter)
        {
            super(base, keyConverter, valueConverter);
            valueInverse = valueConverter.inverse();
        }

        @Override
        public boolean containsValue(final Object value) {
            final Class<? extends V> type = valueConverter.getTargetClass();
            return type.isInstance(value) && base.containsValue(valueInverse.convert(type.cast(value)));
        }
    }

    /**
     * A {@link DerivedMap} used when both the {@link #keyConverter} and {@link #valueConverter}
     * are invertible. Availability of the inverse conversion allows us to delegate some operations
     * to the {@linkplain #base} map instead than iterating over all entries.
     */
    private static final class Invertible<BK,BV,K,V> extends InvertibleKey<BK,BV,K,V> {
        private static final long serialVersionUID = 3830322680676020356L;

        /** The inverse of {@link #valueConverter}. */
        private final ObjectConverter<V,BV> valueInverse;

        /** The inverse of this entry converter. */
        private transient ObjectConverter<Entry<K,V>, Entry<BK,BV>> inverse;

        Invertible(final Map<BK,BV> base,
                   final ObjectConverter<BK,K> keyConverter,
                   final ObjectConverter<BV,V> valueConverter)
        {
            super(base, keyConverter, valueConverter);
            valueInverse = valueConverter.inverse();
        }

        @Override
        public boolean containsValue(final Object value) {
            final Class<? extends V> type = valueConverter.getTargetClass();
            return type.isInstance(value) && base.containsValue(valueInverse.convert(type.cast(value)));
        }

        @Override
        public V put(final K key, final V value) {
            ArgumentChecks.ensureNonNull("key", key);
            return valueConverter.convert(base.put(keyInverse.convert(key), valueInverse.convert(value)));
        }

        @Override
        public ObjectConverter<Entry<K,V>, Entry<BK,BV>> inverse() {
            if (inverse == null) {
                inverse = new DerivedMap<>(null, keyInverse, valueInverse);
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
            keySet = DerivedSet.create(base.keySet(), keyConverter);
        }
        return keySet;
    }

    /**
     * Returns a set view of the mappings contained in this map.
     */
    @Override
    public final Set<Map.Entry<K,V>> entrySet() {
        if (entrySet == null) {
            entrySet = DerivedSet.create(base.entrySet(), this);
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
    public final Class<? super Entry<BK,BV>> getSourceClass() {
        return Entry.class;
    }

    /**
     * Returns the target class of the map entry converter.
     * Defined because the interface requires so but not used.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public final Class<? extends Entry<K,V>> getTargetClass() {
        return (Class) Entry.class;
    }

    /**
     * Converts the given entry.
     */
    @Override
    public final Entry<K,V> convert(final Entry<BK,BV> entry) {
        final K key   =   keyConverter.convert(entry.getKey());
        final V value = valueConverter.convert(entry.getValue());
        return (key != null) ? new SimpleEntry<>(key, value) : null;
    }

    /**
     * To be defined in the {@link Invertible} sub-class only.
     */
    @Override
    public ObjectConverter<Entry<K,V>, Entry<BK,BV>> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
