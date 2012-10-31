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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ArgumentChecks;


/**
 * A map whose keys are derived <cite>on-the-fly</cite> from an other map.
 * Conversions are performed when needed by two methods:
 *
 * <ul>
 *   <li>The iterators over the {@linkplain #keySet() key set} or {@linkplain #entrySet() entry set}
 *       obtain the derived values by calls to the {@link #baseToDerived(Object)} method.</li>
 *   <li>Queries ({@link #get get}, {@link #containsKey containsKey}) and write operations
 *       ({@link #put put}, {@link #remove remove}) obtain the storage values by calls to the
 *       {@link #derivedToBase(Object)} method.</li>
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
 * @param <K>  The type of keys in this map.
 * @param <V>  The type of values in both this map and the underlying map.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@Decorator(Map.class)
public abstract class DerivedMap<BK,K,V> extends AbstractMap<K,V> implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6994867383669885934L;

    /**
     * The base map whose keys are derived from.
     *
     * @see #baseToDerived(Object)
     * @see #derivedToBase(Object)
     */
    protected final Map<BK,V> base;

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
    private transient Set<? extends Map.Entry<K,V>> entrySet;

    /**
     * The derived key type.
     */
    private final Class<K> keyType;

    /**
     * Creates a new derived map from the specified base map.
     *
     * @param base The base map.
     * @param keyType the type of keys in the derived map.
     */
    public DerivedMap(final Map<BK,V> base, final Class<K> keyType) {
        ArgumentChecks.ensureNonNull("base",    this.base    = base);
        ArgumentChecks.ensureNonNull("keyType", this.keyType = keyType);
    }

    /**
     * Transforms a key from the {@linkplain #base} map to a key in this map.
     * If there is no key in the derived map for the specified base key,
     * then this method returns {@code null}.
     *
     * @param  key A ley from the {@linkplain #base} map.
     * @return The key that this view should contains instead of {@code key}, or {@code null}.
     */
    protected abstract K baseToDerived(final BK key);

    /**
     * Transforms a key from this derived map to a key in the {@linkplain #base} map.
     *
     * @param  key A key in this map.
     * @return The key stored in the {@linkplain #base} map.
     */
    protected abstract BK derivedToBase(final K key);

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return base.isEmpty() || entrySet().isEmpty();
    }

    /**
     * Returns {@code true} if this map maps one or more keys to this value.
     * The default implementation delegates directly to the {@linkplain #base} map.
     *
     * @return {@code true} if this map maps one or more keys to this value.
     */
    @Override
    public boolean containsValue(final Object value) {
        return base.containsValue(value);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * This method first checks if the given element is an instance of {@link #keyType},
     * then delegates to the {@link #base} map like below:
     *
     * {@preformat java
     *     return base.containsKey(derivedToBase(element));
     * }
     *
     * @param  key key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(final Object key) {
        return keyType.isInstance(key) && base.containsKey(derivedToBase(keyType.cast(key)));
    }

    /**
     * Returns the value to which this map maps the specified key.
     * This method first checks if the given element is an instance of {@link #keyType},
     * then delegates to the {@link #base} map like below:
     *
     * {@preformat java
     *     return base.get(derivedToBase(element));
     * }
     *
     * @param  key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key.
     */
    @Override
    public V get(final Object key) {
        return keyType.isInstance(key) ? base.get(derivedToBase(keyType.cast(key))) : null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * This method first checks if the given element is non-null,
     * then delegates to the {@link #base} map like below:
     *
     * {@preformat java
     *     return base.put(derivedToBase(key), value);
     * }
     *
     * @param  key key with which the specified value is to be associated.
     * @param  value value to be associated with the specified key.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.
     * @throws UnsupportedOperationException if the {@linkplain #base} map doesn't
     *         supports the {@code put} operation.
     */
    @Override
    public V put(final K key, final V value) throws UnsupportedOperationException {
        return base.put(derivedToBase(key), value);
    }

    /**
     * Removes the mapping for this key from this map if present.
     * This method first checks if the given element is an instance of {@link #keyType},
     * then delegates to the {@link #base} map like below:
     *
     * {@preformat java
     *     return base.remove(derivedToBase(element));
     * }
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no entry for key.
     * @throws UnsupportedOperationException if the {@linkplain #base} map doesn't
     *         supports the {@code remove} operation.
     */
    @Override
    public V remove(final Object key) throws UnsupportedOperationException {
        return keyType.isInstance(key) ? base.remove(derivedToBase(keyType.cast(key))) : null;
    }

    /**
     * Returns a set view of the keys contained in this map.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new KeySet(base.keySet());
        }
        return keySet;
    }

    /**
     * Returns a collection view of the values contained in this map.
     *
     * @return a collection view of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        return base.values();
    }

    /**
     * Returns a set view of the mappings contained in this map.
     *
     * @return a set view of the mappings contained in this map.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<K,V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet(base.entrySet());
        }
        return (Set<Map.Entry<K,V>>) entrySet; // Safe because read-only.
    }

    /**
     * The key set.
     */
    @Decorator(Set.class)
    private final class KeySet extends DerivedSet<BK,K> {
        private static final long serialVersionUID = -2931806200277420177L;

        public KeySet(final Set<BK> base) {
            super(base, keyType);
        }

        @Override
        protected K baseToDerived(final BK element) {
            return DerivedMap.this.baseToDerived(element);
        }

        @Override
        protected BK derivedToBase(final K element) {
            return DerivedMap.this.derivedToBase(element);
        }
    }

    /**
     * The entry set.
     */
    @Decorator(Set.class)
    private final class EntrySet extends DerivedSet<Map.Entry<BK,V>, Entry<BK,K,V>> {
        private static final long serialVersionUID = -1328083271645313149L;

        @SuppressWarnings({"unchecked","rawtypes"})
        public EntrySet(final Set<Map.Entry<BK,V>> base) {
            super(base, (Class) Entry.class);
        }

        @Override
        protected Entry<BK,K,V> baseToDerived(final Map.Entry<BK,V> entry) {
            final K derived = DerivedMap.this.baseToDerived(entry.getKey());
            return (derived != null) ? new Entry<>(entry, derived) : null;
        }

        @Override
        protected Map.Entry<BK,V> derivedToBase(final Entry<BK,K,V> element) {
            return element.entry;
        }
    }

    /**
     * The entry element.
     */
    @Decorator(Map.Entry.class)
    private static final class Entry<BK,K,V> implements Map.Entry<K,V> {
        public final Map.Entry<BK,V> entry;
        private final K derived;

        public Entry(final Map.Entry<BK,V> entry, final K derived) {
            this.entry   = entry;
            this.derived = derived;
        }

        @Override
        public K getKey() {
            return derived;
        }

        @Override
        public V getValue() {
            return entry.getValue();
        }

        @Override
        public V setValue(V value) {
            return entry.setValue(value);
        }
    }
}
