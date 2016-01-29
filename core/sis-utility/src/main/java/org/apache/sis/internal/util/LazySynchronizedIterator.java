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
package org.apache.sis.internal.util;

import java.util.Iterator;


/**
 * An iterator over all elements given by an array of {@code Iterable<E>}, skipping null elements.
 * All uses of an {@code Iterable<E>} (including its iterator) is synchronized on that {@code Iterable} instance.
 *
 * <p>Note that despite the above-cited synchronization, this iterator is <strong>not</strong> thread-safe:
 * each thread needs to use its own iterator instance. However provided that the above condition is meet,
 * different threads can safely use their iterators concurrently even if the underlying {@code Iterable}s
 * were not thread-safe, because of the synchronization on {@code Iterable<E>} instances.</p>
 *
 * @param <E> The type of elements to be returned by the iterator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class LazySynchronizedIterator<E> extends AbstractIterator<E> {
    /**
     * The providers of iterators. This array shall not be modified by {@code LazySynchronizedSetIterator},
     * since this is a direct reference to the array given to the constructor (not a copy).
     */
    private final Iterable<? extends E>[] providers;

    /**
     * Index of the {@code Iterable<E>} instance that provides the {@link #it} value.
     * This is also the instance to use as a synchronization lock.
     */
    private int providerIndex;

    /**
     * The iterator on which to delegate calls to {@link #hasNext()} and {@link #next()}.
     * This iterator is provided by {@code providers[providerIndex].iterator()}.
     */
    private Iterator<? extends E> it;

    /**
     * Creates a new iterator over all elements returned by the given providers.
     * Null elements in the given array will be ignored.
     *
     * @param providers The providers of iterators. This array is <strong>not</strong> cloned.
     */
    public LazySynchronizedIterator(final Iterable<? extends E>[] providers) {
        this.providers = providers;
    }

    /**
     * Returns {@code true} if {@link #next()} can return a non-null element.
     * This method delegates to the iterators of all {@linkplain #providers}
     * until one is found that return a non-null element.
     *
     * @return {@code true} if there is more elements to return.
     */
    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        while (providerIndex < providers.length) {
            final Iterable<? extends E> provider = providers[providerIndex];
            if (provider != null) {
                synchronized (provider) {
                    if (it == null) {
                        it = provider.iterator();
                    }
                    while (it.hasNext()) {
                        next = it.next();
                        if (next != null) {
                            return true;
                        }
                    }
                    it = null;
                }
            }
            providerIndex++;
        }
        return false;
    }
}
