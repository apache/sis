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
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.NoSuchElementException;
import java.lang.ref.Reference;


/**
 * The set of entries in the {@link Cache#map}. On iteration, handlers will be skipped
 * and the values of weak references are returned instead of the {@link Reference} object.
 *
 * <p>This class is not needed for the normal working of {@link Cache}. it is used only if
 * the user wants to see the cache entries through the standard Java collection API.</p>
 *
 * <div class="section">Thread safety</div>
 * This class is thread-safe if and only if the {@code Set} given to the constructor is thread-safe.
 *
 * @param <K> The type of key objects.
 * @param <V> The type of value objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class CacheEntries<K,V> extends AbstractSet<Map.Entry<K,V>> {
    /**
     * The set of entries in the {@link Cache#map}.
     */
    private final Set<Map.Entry<K,Object>> entries;

    /**
     * Wraps the given set of entries of a {@link Cache#map}.
     *
     * @param entries The set of entries. Implementation shall support concurrency.
     */
    CacheEntries(final Set<Map.Entry<K,Object>> entries) {
        this.entries = entries;
    }

    /**
     * Returns {@code true} if the set is empty. Overloaded because {@code ConcurrentHashMap}
     * has a more efficient implementation of this method than testing {@code size() == 0}.
     */
    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the number of entries.
     */
    @Override
    public int size() {
        return entries.size();
    }

    /**
     * Returns an iterator over the entries.
     */
    @Override
    public Iterator<Map.Entry<K,V>> iterator() {
        return new Iter<K,V>(entries.iterator());
    }

    /**
     * An iterator over the entries in the {@link Cache#map}. Handlers will be skipped and the
     * values of weak references are returned instead of the {@link Reference} object.
     */
    private static final class Iter<K,V> implements Iterator<Map.Entry<K,V>> {
        /**
         * The iterator over the entries wrapped by {@link CacheEntries}.
         */
        private final Iterator<Map.Entry<K,Object>> it;

        /**
         * The next entry to returns, or {@code null} if we reached the end of iteration.
         */
        private Map.Entry<K,V> next;

        /**
         * Creates a new iterator wrapping the given iterator from {@link CacheEntries#entries}.
         */
        Iter(final Iterator<Map.Entry<K,Object>> it) {
            this.it = it;
            advance();
        }

        /**
         * Advances the iterator to the next entry to be returned.
         */
        @SuppressWarnings("unchecked")
        private void advance() {
            while (it.hasNext()) {
                final Map.Entry<K,Object> entry = it.next();
                Object value = entry.getValue();
                if (value == null || value instanceof Cache.Handler<?>) {
                    continue;
                }
                if (value instanceof Reference<?>) {
                    value = ((Reference<?>) value).get();
                    if (value == null) {
                        continue;
                    }
                }
                next = new SimpleEntry<K,V>(entry.getKey(), (V) value);
                return;
            }
            next = null;
        }

        /**
         * Returns {@code true} if there is more element to returns.
         */
        @Override
        public boolean hasNext() {
            return next != null;
        }

        /**
         * Returns the next element.
         */
        @Override
        public Map.Entry<K, V> next() {
            final Map.Entry<K,V> n = next;
            if (n != null) {
                advance();
                return n;
            }
            throw new NoSuchElementException();
        }

        /**
         * Unsupported operation, because the wrapped iterator is not after the proper element
         * (it is after the next one).
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
