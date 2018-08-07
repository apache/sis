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
package org.apache.sis.openoffice;

import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.util.collection.Cache;


/**
 * Key of cached results.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @param <T>  the type of cached values.
 *
 * @since 0.8
 * @module
 */
final class CacheKey<T> {
    /**
     * The cache shared by all formulas.
     */
    private static final Cache<CacheKey<?>, Object> cache = new Cache<>(32, 10, true);

    /**
     * The type of cached value.
     */
    private final Class<T> type;

    /**
     * The key for fetching a cached value.
     * This is for example an EPSG code or a format pattern.
     */
    private String key;

    /**
     * Optional object to use in addition of the key, or {@code null} if none.
     */
    private Object ext;

    /**
     * The area of interest, or {@code null} if none or not relevant.
     */
    private final double[] area;

    /**
     * Creates a new key for a value to cache.
     *
     * @param type  the type of the value to cache.
     * @param key   the key for fetching a cached value. This is for example an EPSG code or a format pattern.
     * @param ext   optional object to use in addition of the key, or {@code null} if none.
     * @param area  the area of interest, or {@code null} if none or not relevant.
     */
    CacheKey(final Class<T> type, final String key, final Object ext, final double[] area) {
        this.type = type;
        this.key  = key.trim();
        this.ext  = ext;
        this.area = area;
    }

    /**
     * Returns the cached value for this key, or {@code null} if none.
     */
    final T peek() {
        return type.cast(cache.peek(this));
    }

    /**
     * Notifies the cache that a value will be computed for this key.
     * This method must be followed by a {@code try} … {@code finally} block as below:
     *
     * {@preformat java
     *     T value = key.peek();
     *     if (value == null) {
     *         final Cache.Handler<T> handler = key.lock();
     *         try {
     *             value = handler.peek();
     *             if (value == null) {
     *                 value = createMyObject(key);
     *             }
     *         } finally {
     *             handler.putAndUnlock(value);
     *         }
     *     }
     * }
     */
    @SuppressWarnings("unchecked")
    final Cache.Handler<T> lock() {
        key = key.intern();           // Because the same authority code is often used many time.
        if (ext instanceof String) {
            ext = ((String) ext).intern();
        }
        return (Cache.Handler<T>) cache.lock(this);
    }

    /**
     * Returns a hash code value for this key.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, key, ext) + Arrays.hashCode(area);
    }

    /**
     * Compares the given object with this key for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CacheKey<?>) {
            final CacheKey<?> that = (CacheKey<?>) obj;
            return type.equals(that.type) &&
                   key .equals(that.key)  &&
                   Objects.equals(ext,  that.ext)  &&
                    Arrays.equals(area, that.area);
        }
        return false;
    }
}
